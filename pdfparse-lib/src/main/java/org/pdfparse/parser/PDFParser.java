/*
 * Copyright (c) 2013 Anton Golinko
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 */

package org.pdfparse.parser;

import org.pdfparse.cos.*;
import org.pdfparse.exception.EParseError;
import org.pdfparse.filter.StreamDecoder;

import java.util.StringTokenizer;

public class PDFParser implements ObjectParser {
    private PDFRawData pdfData;
    private ParsingEvent parsingEvent;

    private COSReference rootId = null;
    private COSReference infoId = null;
    private COSReference encryptId = null;
    private byte[][] documentId = {null, null};

    private XRefTable xref;

    public ParserSettings settings;
    public Diagnostics diagnostics;

    public PDFParser(PDFRawData pData) {
        this.settings = new ParserSettings();
        this.diagnostics = new Diagnostics(settings);
        this.pdfData = pData;
        this.xref = new XRefTable(this.settings);
        this.xref.setParser(this);
    }

    public PDFParser(PDFRawData pData, XRefTable xref, ParserSettings settings, ParsingEvent evt) {
        this.settings = settings;
        this.diagnostics = new Diagnostics(settings);
        this.pdfData = pData;
        this.parsingEvent = evt;
        this.xref = xref;
        this.xref.setParser(this);

        parse();
        evt.onDocumentLoaded(rootId, infoId, encryptId);
    }

    private void parse() {
        PDFRawData src = pdfData;

        if (src.length < ParserSettings.MIN_PDF_RAW_CONTENT_LENGTH) {
            throw new EParseError("This is not a valid PDF file");
        }

        // Check the PDF header & version -----------------------
        src.pos = 0;
        if (!(src.checkSignature(Token.PDF_HEADER) || src.checkSignature(Token.FDF_HEADER))) {
            if (!settings.allowScan)
                throw new EParseError("This is not a PDF file");

            // Scan until found PDF header signature
            while (!(src.checkSignature(Token.PDF_HEADER) || src.checkSignature(Token.FDF_HEADER))
                    && (src.pos < settings.headerLookupRange) && (src.pos < src.length)) src.pos++;

            if (!(src.checkSignature(Token.PDF_HEADER) || src.checkSignature(Token.FDF_HEADER)))
                throw new EParseError("This is not a PDF file (PDF header not found)");
        }

        if (src.length - src.pos < ParserSettings.MIN_PDF_RAW_CONTENT_LENGTH)
            throw new EParseError("This is not a valid PDF file");

        String versionLine = src.readLine();
        processVersion(versionLine.substring(Token.PDF_HEADER.length));

        // Scan for EOF -----------------------------------------
        int eofPosition = src.reverseScan(src.length, Token.EOF, settings.eofLookupRange);
        Diagnostics.softAssertSyntaxComliance(settings, eofPosition > 0, "Missing EOF marker");
        if (eofPosition <= 0) {
            eofPosition = src.length;
        }

        // Scan for 'startxref' marker --------------------------
        if (src.reverseScan(eofPosition, Token.STARTXREF, 100) < 0)
            throw new EParseError("Missing 'startxref' marker");


        // Fetch XREF offset ------------------------------------
        src.pos += 10; // skip over "startxref" and first EOL char
        src.skipWS();

        int xref_offset = COSNumber.readInteger(src);

        if ((xref_offset == 0) || (xref_offset >= src.length)) {
            throw new EParseError("Invalid xref offset");
        }

        src.pos = xref_offset;

        src.skipWS();
        if (src.checkSignature(Token.XREF)) {
            parseTrailer(src);
        } else {
            parseXRefStream(src);
        }
    }

    private void processVersion(String versionString) {
        int majorVersion = 0;
        int minorVersion = 0;
        try {
            StringTokenizer tokens = new StringTokenizer(versionString, ".");
            majorVersion = Integer.parseInt(tokens.nextToken());
            minorVersion = Integer.parseInt(tokens.nextToken());
        } catch (Exception e) {
            Diagnostics.softAssertSyntaxComliance(settings, false, "Failed to parse PDF version");
        }

        Diagnostics.softAssertSupportedFeatures(settings,
                majorVersion == 1 && (minorVersion >= 0 && minorVersion <= 8),
                "PDF version is not supported");

        parsingEvent.onDocumentVersionFound(majorVersion, minorVersion);
    }

    @Override
    public COSObject getObject(XRefEntry x) throws EParseError {
        if (x.cachedObject != null) {
            return x.cachedObject;
        }

        int savedPos = pdfData.pos;
        try {
            if (!x.isCompressed) {
                x.cachedObject = parseIndirectObject(x);
                return x.cachedObject;
            }

            // -------- This is compressed object. Needed to do some actions
            XRefEntry containerXRef = xref.get(x.containerObjId);
            if (containerXRef == null) {
                Diagnostics.debugMessage(settings, "No XRef entry for compressed stream %d 0 R referenced by %d %d R. Used COSNull instead", x.containerObjId, x.id, x.gen);
                return new COSNull();
            }
            if (containerXRef.isCompressed) {
                throw new EParseError("Referenced container for compressed object should not be compressed itself (%d %d R)", containerXRef.id, containerXRef.gen);
            }

            if (containerXRef.cachedObject == null) { // Extract compressed block (stream object)
                containerXRef.cachedObject = parseIndirectObject(containerXRef);

                if (!(containerXRef.cachedObject instanceof COSStream))
                    throw new EParseError("Referenced object-container is not stream object (%d %d R)", containerXRef.id, containerXRef.gen);
            }

            // -------- Now got compressed stream
            // -------- decompress its data, and put in cache
            COSStream streamObject = (COSStream) containerXRef.cachedObject;

            if (containerXRef.decompressedStreamData == null) {
                containerXRef.decompressedStreamData = StreamDecoder.decodeStream(streamObject.getData(), streamObject, this.settings);
            }
            PDFRawData streamData = containerXRef.decompressedStreamData;

            // -------- Got decompressed data
            // -------- Parse stream index & content
            int n = streamObject.getInt(COSName.N, 0);
            int first = streamObject.getInt(COSName.FIRST, 0);
            int idxId, idxOffset, savepos;
            XRefEntry idxXRefEntry;
            COSObject result = null;
            for (int i = 0; i < n; i++) { // Extract all objects within stream
                idxId = streamData.fetchUInt();
                idxOffset = streamData.fetchUInt();

                // Update all corresponding entries in XRef table with decompressed objects
                idxXRefEntry = xref.get(idxId);
                if (idxXRefEntry == null)
                    continue; // This object marked in XRef as unused. Skip it

                if (!idxXRefEntry.isCompressed)
                    throw new EParseError(String.format("Something wrong. Compressed object #%d marked as regular object in XRef", idxId));

                savepos = streamData.pos;

                streamData.pos = first + idxOffset;
                idxXRefEntry.cachedObject = this.parseObject(streamData);
                if (idxId == x.id)
                    result = idxXRefEntry.cachedObject; // found it

                streamData.pos = savepos;
            }

            return result;
        } finally {
            pdfData.pos = savedPos;
        }
    }

    private COSObject parseIndirectObject(XRefEntry xref) throws EParseError {
        pdfData.pos = xref.fileOffset;
        //----- Do extra checks
        if (!IdGenPair.tryReadId(pdfData, pdfData.tmpIdGenPair, Token.OBJ))
            throw new EParseError(String.format("Invalid indirect object header (expected '%d %d obj' @ %d)", xref.id, xref.gen, pdfData.pos));

        if ((pdfData.tmpIdGenPair.id != xref.id) || (pdfData.tmpIdGenPair.gen != xref.gen))
            throw new EParseError(String.format("Object header not correspond data specified in reference (expected '%d %d obj' @ %d)", xref.id, xref.gen, pdfData.pos));

        //----- Parse object itself
        return this.parseObject(pdfData);
    }

    public COSObject parseObject(PDFRawData src) throws EParseError {
        byte ch;

        while (true) {
            // skip spaces if any
            int dlen = src.length;
            ch = src.data[src.pos];
            while ((src.pos < dlen) && ((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D))) {
                src.pos++;
                ch = src.data[src.pos];
            }
            //--------------
            ch = src.data[src.pos];
            switch (ch) {
                case 0x25: // '%' - comment
                    src.skipLine();
                    break;
                case 0x2F: // '/' - name
                    return new COSName(src, this);
                case 0x74: // 't' - true
                    //assert(StrLComp(pCurr, 'true', 4) = 0, 'It is not a "true"');
                    src.pos += 4;
                    return new COSBool(true);
                case 0x66: // 'f' - false
                    // Assert(StrLComp(pCurr, 'false', 5) = 0, 'It is not a "false"');
                    src.pos += 5;
                    return new COSBool(false);
                case 0x6E: // 'n' - null
                    // Assert(StrLComp(pCurr, 'false', 5) = 0, 'It is not a "false"');
                    src.pos += 4;
                    return new COSNull();
                case 0x28: // '(' - raw string
                    return new COSString(src, this);
                case 0x3C: // '<' - hexadecimal string
                    if (src.data[src.pos + 1] == 0x3C) { // '<'
                        COSDictionary dict = new COSDictionary(src, this);
                        // check for stream object
                        src.skipWS();
                        if (!src.checkSignature(Token.STREAM))
                            return dict; // this is COSDictionary only
                        // this is stream object
                        COSStream stm = new COSStream(dict, src, this.xref);
                        dict.clear();
                        return stm;
                    }
                    // this is only Hexadecimal string
                    return new COSString(src, this);
                case 0x5B: // '[' - array
                    return new COSArray(src, this);

                case 0x30:
                case 0x31:
                case 0x32:
                case 0x33:
                case 0x34: // 0..4
                case 0x35:
                case 0x36:
                case 0x37:
                case 0x38:
                case 0x39: // 5..9
                case 0x2B:
                case 0x2D:
                case 0x2E: // '+', '-', '.'
                    if (IdGenPair.tryReadId(src, src.tmpIdGenPair, Token.R)) {
                        // this is a valid reference
                        return new COSReference(src.tmpIdGenPair);
                    }

                    return new COSNumber(src, this);
                default:
                    Diagnostics.debugMessage(settings, "Bytes before error occurs: %s", src.dbgPrintBytes());
                    throw new EParseError("Unknown value token at %d", src.pos);
            } // switch
        } // while
    }

    //  Read the cross reference table from a PDF file.  When this method
    //  is called, the file pointer must point to the start of the word
    //  "xref" in the file.
    private void parseTrailer(PDFRawData src) throws EParseError {
        int prevOffset = src.pos;

        while (prevOffset != 0) {
            src.pos = prevOffset;
            // Parse XREF ---------------------
            if (!src.checkSignature(Token.XREF))
                throw new EParseError("This is not an 'xref' table");
            src.pos += Token.XREF.length;

            parseXRefTable(src);
            // Parse Trailer ------------------
            src.skipWS();
            if (!src.checkSignature(Token.TRAILER))
                throw new EParseError("Cannot find 'trailer' tag");
            src.pos += Token.TRAILER.length;
            src.skipWS();

            COSDictionary trailer = new COSDictionary(src, this);
            prevOffset = trailer.getInt(COSName.PREV, 0);
            updateDocumentRoots(trailer);

            // Check for a hybrid PDF-file
            int xrefstrm = trailer.getInt(COSName.XREFSTM, 0);
            if (xrefstrm != 0) { // Yes, this is a hybrid
                src.pos = xrefstrm;
                parseXRefStream(src);
            }

        } // while
    }

    private void parseXRefTable(PDFRawData src) throws EParseError {
        src.skipWS();
        int start;
        int count;
        int n, p;
        int obj_off;
        int obj_gen;
        boolean obj_use;

        while (true) {
            start = src.fetchUInt();
            src.skipWS();
            count = src.fetchUInt();
            src.skipWS();

            if (start == 1) { // fix incorrect start number
                p = src.pos;
                obj_off = src.fetchUInt();
                obj_gen = src.fetchUInt();
                if (obj_off == 0 && obj_gen == 65535)
                    start--;
                src.pos = p;
            }

            for (n = 0; n < count; n++) {
                obj_off = src.fetchUInt();
                obj_gen = src.fetchUInt();
                src.skipWS();
                obj_use = (src.data[src.pos] == 0x6E);   // 'n'
                src.pos++; // skip flag
                if (!obj_use) continue;

                xref.add(start + n, obj_gen, obj_off);
            }
            src.skipWS();
            byte b = src.data[src.pos];
            if ((b < 0x30) || (b > 0x39)) break; // not in [0..9] range
        }// while(1)...
    }

    private void parseXRefStream(PDFRawData src) throws EParseError {
        COSDictionary curr_trailer;
        int prev;
        while (true) {
            src.skipWS();

            if (!IdGenPair.tryReadId(src, src.tmpIdGenPair, Token.OBJ))
                throw new EParseError("Invalid indirect object header");

            src.skipWS();

            curr_trailer = new COSDictionary(src, this);

            if (!curr_trailer.getName(COSName.TYPE, null).equals(COSName.XREF))
                throw new EParseError("This is not a XRef stream");

            updateDocumentRoots(curr_trailer);

            COSArray oW = curr_trailer.getArray(COSName.W, null);
            if ((oW == null) || (oW.size() != 3))
                throw new EParseError("Invalid PDF file");
            int[] w = {oW.getInt(0), oW.getInt(1), oW.getInt(2)};

            int size = curr_trailer.getUInt(COSName.SIZE, 0);
            COSArray index = curr_trailer.getArray(COSName.INDEX, null);
            if (index == null) {
                index = new COSArray();
                index.add(new COSNumber(0));
                index.add(new COSNumber(size));
            }

            PDFRawData bstream;
            bstream = StreamDecoder.decodeStream(src, curr_trailer, this.settings);

            int start;
            int count;
            int index_idx = 0;

            int itype, i2, i3;

            while (index_idx < index.size()) {
                start = index.getInt(index_idx++);
                count = index.getInt(index_idx++);

                int i = 0;
                while (i < count) {
                    if (w[0] != 0) itype = bstream.fetchBinaryUInt(w[0]);
                    else itype = 1; // default value (see specs)
                    if (w[1] != 0) i2 = bstream.fetchBinaryUInt(w[1]);
                    else i2 = 0;
                    if (w[2] != 0) i3 = bstream.fetchBinaryUInt(w[2]);
                    else i3 = 0;

                    switch (itype) {
                        case 0:  // linked list of free objects (corresponding to f entries in a cross-reference table).
                            i++; //TODO: mark as free (delete if exist)
                            continue;
                        case 1: // objects that are in use but are not compressed (corresponding to n entries in a cross-reference table).
                            xref.add((start + i), i3, i2);
                            i++;
                            continue;
                        case 2: // compressed objects.
                            xref.addCompressed(start + i, i2, i3);
                            i++;
                            continue;
                        default:
                            //throw new EParseError("Invalid iType entry in xref stream");
                            Diagnostics.debugMessage(settings, "Invalid iType entry in xref stream: %d", itype);
                            continue;
                    }// switch
                }// for
            } // while

            prev = curr_trailer.getInt(COSName.PREV, 0);
            if (prev != 0) {
                if ((prev < 0) || (prev > src.length))
                    throw new EParseError("Invalid trailer offset (%d)", prev);
                src.pos = prev;
                continue;
            } else break;
        } // while (true)
    }

    private void updateDocumentId(COSDictionary trailer, boolean required) {
        if (documentId[0] != null) {
            // IDs already has been set. Ignore all subsequents
            return;
        }
        COSArray Ids = trailer.getArray(COSName.ID, null);
        if ((Ids == null) ||
                (Ids.size() != 2) ||
                !(Ids.get(0) instanceof COSString) ||
                !(Ids.get(1) instanceof COSString)) {

            if (required) {
                throw new EParseError("Missing or invalid file identifier for encrypted document (required)");
            }
            return;
        }

        documentId[0] = ((COSString) Ids.get(0)).getBinaryValue();
        documentId[1] = ((COSString) Ids.get(1)).getBinaryValue();
    }

    private void updateDocumentRoots(COSDictionary trailer) {
        updateDocumentId(trailer, false);

        if (rootId == null) {
            rootId = trailer.getReference(COSName.ROOT);
        }
        if (infoId == null) {
            infoId = trailer.getReference(COSName.INFO);
        }
        if (encryptId == null) {
            encryptId = trailer.getReference(COSName.ENCRYPT);

            if (encryptId != null) {
                if (documentId[0] != null) {
                    Diagnostics.debugMessage(settings, "WARNING: Contradictory document IDs. Decryption may not work");
                }
                updateDocumentId(trailer, true);
            }
        }
    }

    public XRefTable getXref() {
        return xref;
    }

    public void parseAndDecodeAllObjects() {
        for (int key : xref.getKeys()) {
            XRefEntry entry = xref.get(key);
            this.getObject(entry);
        }
    }

}
