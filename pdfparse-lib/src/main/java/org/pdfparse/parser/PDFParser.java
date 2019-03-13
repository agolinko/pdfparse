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

import java.util.Arrays;
import java.util.StringTokenizer;

public class PDFParser implements ParsingGetObject {
    private PDFRawData pdfData;
    private ParsingEvent parsingEvent;

    private COSReference rootID = null;
    private COSReference infoID = null;
    private COSReference encryptID = null;
    private byte[][] documentId = {null,null};

    private XRefTable xref;

    public ParseSettings settings;

    public PDFParser(PDFRawData pData) {
        this.settings = new ParseSettings();
        this.pdfData = pData;
        this.xref = new XRefTable(this.settings);
    }
    public PDFParser(PDFRawData pData, ParsingEvent evt) {
        this.settings = new ParseSettings();
        this.pdfData = pData;
        this.parsingEvent = evt;
        this.xref = new XRefTable(this.settings);

        parse();
        evt.onDocumentLoaded(rootID, infoID, encryptID);
    }

    private void parse() {
        PDFRawData src = pdfData;

        if (src.length < settings.MIN_PDF_RAW_CONTENT_LENGTH) {
            throw new EParseError("This is not a valid PDF file");
        }

        // Check the PDF header & version -----------------------
        src.pos = 0;
        if (  !( src.checkSignature(Token.PDF_HEADER) || src.checkSignature(Token.FDF_HEADER) ) ) {
            if (!settings.allowScan)
                throw new EParseError("This is not a PDF file");

            // Scan until found PDF header signature
            while ( !(src.checkSignature(Token.PDF_HEADER) || src.checkSignature(Token.FDF_HEADER))
                    && (src.pos < settings.headerLookupRange) && (src.pos < src.length) ) src.pos++;

            if (  !(src.checkSignature(Token.PDF_HEADER) || src.checkSignature(Token.FDF_HEADER)) )
                throw new EParseError("This is not a PDF file (PDF header not found)");
        }

        if (src.length - src.pos < settings.MIN_PDF_RAW_CONTENT_LENGTH)
            throw new EParseError("This is not a valid PDF file");

        String versionLine = src.readLine();
        processVersion(versionLine.substring(Token.PDF_HEADER.length));

        // Scan for EOF -----------------------------------------
        if (src.reverseScan(src.length, Token.EOF, settings.eofLookupRange) < 0)
            throw new EParseError("Missing end of file marker");

        // Scan for 'startxref' marker --------------------------
        if (src.reverseScan(src.pos, Token.STARTXREF, 100) < 0)
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
            parseTableAndTrailer(src, parsingEvent);
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
            settings.softAssertSyntaxComliance(false, "Failed to parse PDF version");
        }

        settings.softAssertSupportedFeatures(
                majorVersion == 1 && (minorVersion >= 1 && minorVersion <= 8),
                "PDF version is not supported");

        parsingEvent.onDocumentVersionFound(majorVersion, minorVersion);
    }

    @Override
    public COSObject getObject(COSReference ref) {
        int savepos = pdfData.pos;
        try {
            return getCOSObject(ref.id, ref.gen);
        } finally {
            pdfData.pos = savepos;
        }
    }
    private COSObject getCOSObject(int id, int gen) throws EParseError {
        XRefEntry x = xref.get(id);

        if (x == null) {
            settings.debugMessage("No XRef entry for object %d %d R. Used COSNull instead", id, gen);
            return new COSNull();
        }

        if (x.cachedObject != null)
            return x.cachedObject;

        if (x.gen != gen) {
            settings.debugMessage("Object with generation %d not found. But there is %d generation number", gen, x.gen);
        }

        if (!x.isCompressed) {
            pdfData.pos = x.fileOffset;
            //-----


            if (!IdGenPair.tryReadId(pdfData, pdfData.tmpIdGenPair, Token.OBJ))
                throw new EParseError(String.format("Invalid indirect object header (expected '%d %d obj' @ %d)", id, gen, pdfData.pos));

            if ((pdfData.tmpIdGenPair.id != id)||(pdfData.tmpIdGenPair.gen != gen))
                throw new EParseError(String.format("Object header not correspond data specified in reference (expected '%d %d obj' @ %d)", id, gen, pdfData.pos));
            pdfData.skipWS();
            //-----
            x.cachedObject = this.parseObject(pdfData);
            return x.cachedObject;
        }

        // Compressed ----------------------------------------------------
        XRefEntry containerXRef = xref.get(x.containerObjId);
        if (containerXRef == null) {
            settings.debugMessage("No XRef entry for compressed stream %d 0 R referenced by %d %d R. Used COSNull instead", x.containerObjId, id, gen);
            return new COSNull();
        }

        if (containerXRef.cachedObject == null) { // Extract compressed block (stream object)
            pdfData.pos = containerXRef.fileOffset;
            //-----
            if (!IdGenPair.tryReadId(pdfData, pdfData.tmpIdGenPair, Token.OBJ))
                throw new EParseError("Invalid indirect object header");
            if ((pdfData.tmpIdGenPair.id != x.containerObjId)||(pdfData.tmpIdGenPair.gen != 0))
                throw new EParseError("Object header not correspond data specified in reference");
            pdfData.skipWS();
            //-----
            containerXRef.cachedObject = this.parseObject(pdfData);

            if (! (containerXRef.cachedObject instanceof COSStream))
                throw new EParseError("Referenced object-container is not stream object");
        }

        COSStream streamObject = (COSStream)containerXRef.cachedObject;

        // --- Ok, received streamObject
        // next, decompress its data, and put in cache
        if (containerXRef.decompressedStreamData == null) {
            containerXRef.decompressedStreamData = StreamDecoder.decodeStream(streamObject.getData(), streamObject, this.settings);
        }
        PDFRawData streamData = containerXRef.decompressedStreamData;

        // -- OK, retrieved from cache decompressed data
        // Parse stream index & content

        int n = streamObject.getInt(COSName.N, 0);
        int first = streamObject.getInt(COSName.FIRST, 0);
        int idxId, idxOffset, savepos;
        XRefEntry idxXRefEntry;
        COSObject obj = null;
        for (int i=0; i<n; i++) { // Extract all objects within stream
            idxId = streamData.fetchUInt();
            idxOffset = streamData.fetchUInt();

            // check if it is free object
            idxXRefEntry = xref.get(idxId);
            if (idxXRefEntry == null)
                continue; // this is a free object. skip it

            if (!idxXRefEntry.isCompressed)
                throw new EParseError(String.format("Something strange. Compressed object #%d marked as regular object in XRef", idxId));

            savepos = streamData.pos;

            streamData.pos = first + idxOffset;
            idxXRefEntry.cachedObject = this.parseObject(streamData);
            if (idxId == id)
                obj = idxXRefEntry.cachedObject; // found it

            streamData.pos = savepos;
        }

        return obj;
    }

    @Override
    public COSDictionary getDictionary(COSReference ref) {
        return getDictionary(ref.id, ref.gen, true);
    }
    private COSDictionary getDictionary(int id, int gen, boolean strict) throws EParseError {
        COSObject obj = this.getCOSObject(id, gen);
        if (obj instanceof COSDictionary)
            return (COSDictionary)obj;

        if (strict)
            throw new EParseError("Dictionary expected for %d %d R. But retrieved object is %s", id, gen, obj.getClass().getName());
        else return null;
    }

    @Override
    public COSStream getStream(COSReference ref) {
        return getStream(ref.id, ref.gen, true);
    }
    private COSStream getStream(int id, int gen, boolean strict) throws EParseError {
        COSObject obj = this.getCOSObject(id, gen);
        if (obj instanceof COSStream)
            return (COSStream)obj;

        if (strict)
            throw new EParseError("Stream expected for %d %d R. But retrieved object is %s", id, gen, obj.getClass().getName());
        else return null;
    }

    public COSObject parseObject(PDFRawData src) throws EParseError {
        byte ch;

        while(true) {
            // skip spaces if any
            int dlen = src.length;
            ch = src.src[src.pos];
            while ((src.pos < dlen)&&((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D))) {
                src.pos++;
                ch = src.src[src.pos];
            }
            //--------------
            ch = src.src[src.pos];
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
                    if (src.src[src.pos+1] == 0x3C) { // '<'
                        COSDictionary dict = new COSDictionary(src, this);
                        // check for stream object
                        src.skipWS();
                        if (!src.checkSignature(Token.STREAM))
                            return dict; // this is COSDictionary only
                        // this is stream object
                        COSStream stm = new COSStream(dict, src, this);
                        dict.clear();
                        dict = null;
                        return stm;
                    }
                    // this is only Hexadecimal string
                    return new COSString(src, this);
                case 0x5B: // '[' - array
                    return new COSArray(src, this);

                case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: // 0..4
                case 0x35: case 0x36: case 0x37: case 0x38: case 0x39: // 5..9
                case 0x2B: case 0x2D: case 0x2E: // '+', '-', '.'
                    if (IdGenPair.tryReadId(src, src.tmpIdGenPair, Token.R)) {
                        // this is a valid reference
                        return new COSReference(src.tmpIdGenPair);
                    }

                    return new COSNumber(src, this);
                default:
                    settings.debugMessage("Bytes before error occurs: %s", src.dbgPrintBytes());
                    throw new EParseError("Unknown value token at %d", src.pos);
            } // switch
        } // while
    }

    public static final byte[] fetchStream(PDFRawData src, int stream_len, boolean movePosBeyoundEndObj) throws EParseError {
        src.skipWS();
        if (!src.checkSignature(Token.STREAM))
            throw new EParseError("'stream' keyword not found");
        src.pos += Token.STREAM.length;
        src.skipCRLForLF();
        if (src.pos + stream_len > src.length)
            throw new EParseError("Unexpected end of file (stream object too large)");

        // TODO: Lazy parse (reference + start + len)
        byte[] res = Arrays.copyOfRange(src.src, src.pos, src.pos + stream_len);
        src.pos += stream_len;

        if (movePosBeyoundEndObj) {
            byte firstbyte = Token.ENDOBJ[0];
            int max_pos = src.length - Token.ENDOBJ.length;
            if (max_pos - src.pos > ParseSettings.MAX_SCAN_RANGE)
                max_pos = src.pos + ParseSettings.MAX_SCAN_RANGE;
            for (int i = src.pos; i < max_pos; i++)
                if ((src.src[i] == firstbyte)&&src.checkSignature(i, Token.ENDOBJ)) {
                    src.pos = i + Token.ENDOBJ.length;
                    return res;
                }

            throw new EParseError("'endobj' tag not found");
        }

        return res;
    }

    private void parseTableOnly(PDFRawData src) throws EParseError {
        src.skipWS();
        int start;
        int count;
        int n, p;
        int obj_off;
        int obj_gen;
        boolean obj_use;

        while (true) {
        start = src.fetchUInt(); src.skipWS();
        count = src.fetchUInt(); src.skipWS();

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
            obj_use = (src.src[src.pos] == 0x6E);   // 'n'
            src.pos++; // skip flag
            if (!obj_use) continue;

            xref.add(start+n, obj_gen, obj_off);
        }
        src.skipWS();
        byte b = src.src[src.pos];
        if ((b < 0x30)||(b > 0x39)) break; // not in [0..9] range
        }// while(1)...
    }

    //  Read the cross reference table from a PDF file.  When this method
    //  is called, the file pointer must point to the start of the word
    //  "xref" in the file.
    private void parseTableAndTrailer(PDFRawData src, ParsingEvent evt) throws EParseError {
        int prevOffset = src.pos;

        while (prevOffset != 0) {
            src.pos = prevOffset;
            // Parse XREF ---------------------
            if (!src.checkSignature(Token.XREF))
                throw new EParseError("This is not an 'xref' table");
            src.pos += Token.XREF.length;

            parseTableOnly(src);
            // Parse Trailer ------------------
            src.skipWS();
            if (!src.checkSignature(Token.TRAILER))
                throw new EParseError("Cannot find 'trailer' tag");
            src.pos += Token.TRAILER.length;
            src.skipWS();

            COSDictionary trailer = new COSDictionary(src, this);
            prevOffset = trailer.getInt(COSName.PREV, 0);

            if (rootID == null) {
                rootID = trailer.getReference(COSName.ROOT);
            }
            if (infoID == null) {
                infoID = trailer.getReference(COSName.INFO);
            }
            if (encryptID == null) {
                encryptID = trailer.getReference(COSName.ENCRYPT);

                if (encryptID != null) {
                    COSArray Ids = trailer.getArray(COSName.ID, null);
                    if ( (Ids == null) ||
                        (Ids.size() != 2) ||
                        !(Ids.get(0) instanceof COSString) ||
                        !(Ids.get(1) instanceof COSString)) {

                        throw new EParseError("Missing or invalid file identifier for encrypted document (required)");
                    }

                    documentId[0] = ((COSString) Ids.get(0)).getBinaryValue();
                    documentId[1] = ((COSString) Ids.get(1)).getBinaryValue();
                }
            }

            // Check for a hybrid PDF-file
            int xrefstrm = trailer.getInt(COSName.XREFSTM, 0);
            if (xrefstrm != 0) { // Yes, this is a hybrid
                src.pos = xrefstrm;
                parseXRefStream(src);
            }

        } // while
    }

    private void parseXRefStream(PDFRawData src) throws EParseError {
        COSDictionary curr_trailer;
        int prev;
        while (true) {
            src.skipWS();

            if (!IdGenPair.tryReadId(src, src.tmpIdGenPair, Token.OBJ))
                throw new EParseError("Invalid indirect object header");

            src.skipWS();


            //addXRef(65530, 0, trailerOffset);
            curr_trailer = new COSDictionary(src, this);

            // TODO: Mark 'encrypt' objects for removing

            if (!curr_trailer.getName(COSName.TYPE, null).equals(COSName.XREF))
                throw new EParseError("This is not a XRef stream");


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

            //int row_len = w[0] + w[1] + w[2];

            //byte[] bstream =  // TODO: implement max verbosity mode
            //    src.fetchStream(curr_trailer.getUInt(COSName.LENGTH, 0), false);

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
                    if (w[0] != 0) itype = bstream.fetchBinaryUInt(w[0]); else itype = 1; // default value (see specs)
                    if (w[1] != 0) i2 = bstream.fetchBinaryUInt(w[1]); else i2 = 0;
                    if (w[2] != 0) i3 = bstream.fetchBinaryUInt(w[2]); else i3 = 0;

                    switch(itype) {
                    case 0:  // linked list of free objects (corresponding to f entries in a cross-reference table).
                        i++; //TODO: mark as free (delete if exist)
                        continue;
                    case 1: // objects that are in use but are not compressed (corresponding to n entries in a cross-reference table).
                        xref.add((start+i), i3, i2);
                        i++;
                        continue;
                    case 2: // compressed objects.
                        xref.addCompressed(start+i, i2, i3);
                        i++;
                        continue;
                    default:
                        //throw new EParseError("Invalid iType entry in xref stream");
                        settings.debugMessage("Invalid iType entry in xref stream: %d", itype );
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

    public void dbgPrintAll() {
//        System.out.printf("Max id: %d\r\n", max_id);
//        System.out.printf("Max gen: %d\r\n", max_gen);
//        System.out.printf("Max offset: %d\r\n", max_offset);
//        System.out.printf("Compressed max stream id: %d\r\n", compressed_max_stream_id);
//        System.out.printf("Compressed max stream offs: %d\r\n", compressed_max_stream_offs);

//        XRefEntry xref;
//        int[] keys = by_id.getKeys();
//        for (int i = 0; i<keys.length; i++) {
//            xref = by_id.get(keys[i]);
//            System.out.printf("%d %s\r\n", keys[i], xref.toString());
//        }
//        for (Integer id : by_id.keySet()) {
//           xref = by_id.get(id);
//           System.out.printf("%d %s\r\n", id.intValue(), xref.toString());
//        }

    }

//    public void dbgSaveAllStreams(String dir) {
//        File path = new File (dir);
//        path.mkdirs();
//        path = null;
//
//        FileOutputStream f;
//        for (Integer stmId  : decompressedStreams.keySet()) {
//            try {
//                f = new FileOutputStream(dir + File.separator + "stream" + stmId.toString() + ".bin");
//                f.write(decompressedStreams.get(stmId).src);
//                f.flush();
//                f.close();
//            } catch ( IOException ex) {
//                Logger.getLogger(XRef.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//
//    public void dbgSaveAllObjects(String dir) {
//        final byte[] SEPARATOR = {0xD, 0xA, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0xD, 0xA};
//        File path = new File (dir);
//        path.mkdirs();
//        path = null;
//
//        COSObject obj;
//        FileOutputStream f;
//        ByteArrayOutputStream out;
//        PDFRawData decompressedStm;
//        for (Integer objId  : by_id.keySet()) {
//            try {
//                f = new FileOutputStream(dir + File.separator + "obj" + objId.toString() + ".bin");
//                obj = by_id.get(objId);
//                out = new ByteArrayOutputStream();
//                obj.produce(out, pContext);
//
//                if (obj instanceof COSStream) {
//                    decompressedStm = decompressedStreams.get(objId);
//
//                    if (decompressedStm == null) { // is not in cache?
//                        decompressedStm = StreamDecoder.decodeStream(((COSStream)obj).getData(), (COSStream)obj, pContext);
//                        decompressedStreams.put(objId.intValue(), decompressedStm); // put in cache
//                    }
//
//                    if (decompressedStm != null) {
//                        out.write(SEPARATOR);
//                        out.write(decompressedStm.src);
//                    }
//                }
//                f.write(out.toByteArray());
//                f.flush();
//                f.close();
//                out = null;
//            } catch ( EParseError ex) {
//                Logger.getLogger(XRef.class.getName()).log(Level.SEVERE, null, ex);
//            } catch ( IOException io) {
//                Logger.getLogger(XRef.class.getName()).log(Level.SEVERE, null, io);
//            }
//
//        }
//    }

    public void parseAndDecodeAllObjects() {
        for (int key : xref.getKeys()) {
            XRefEntry entry = xref.get(key);
            this.getCOSObject(entry.id, entry.gen);
        }
    }

}
