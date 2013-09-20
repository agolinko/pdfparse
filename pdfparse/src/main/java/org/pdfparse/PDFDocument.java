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

package org.pdfparse;

import org.pdfparse.cos.*;
import org.pdfparse.exception.EParseError;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;


public class PDFDocument implements ParsingEvent {
    private String filename;
    private String filepath;

    private ObjectCache cache;
    private ParsingContext context;
    private XRef xref;
    private PDFRawData data;


    private COSReference rootID = null;
    private COSReference infoID = null;

    private COSDictionary encryption = null;
    private COSDictionary dictRoot = null;

    private PDFDocInfo documentInfo = null;
    private byte[][] documentId = {null,null};
    private boolean documentIsEncrypted = false;
    private float documentVersion = 0.0f;

    public PDFDocument() {
       data = new PDFRawData();
       context = new ParsingContext();
       xref = new XRef(context);
       cache = new ObjectCache(xref, data, context);

       context.findObject = cache;
    }

    public void done() {
        cache.done();
        xref.done();
        context.findObject = null;
    }

    public PDFDocument(String filename) throws EParseError, IOException {
        this();
        File file = new File(filename);
        open(file);
    }

    public PDFDocument(File file) throws EParseError, IOException {
        this();
        open(file);
    }

    public PDFDocument(byte[] buffer) throws EParseError {
        this();

        this.filename = "internal";
        this.filepath = "internal";

        data.src = buffer;
        data.pos = 0;
        data.length = buffer.length;

        open(data);
    }

    private void open(File file) throws EParseError, IOException {
        this.filename = file.getName();
        this.filepath = file.getParent();


        FileInputStream fin = new FileInputStream(file);
        FileChannel channel = fin.getChannel();

        data.src = new byte[(int) file.length()];
        data.pos = 0;
        data.length = (int) file.length();

        ByteBuffer bb = ByteBuffer.wrap(data.src);
        bb.order(ByteOrder.BIG_ENDIAN);
        channel.read(bb);


        open(data);

    }

    private void open(PDFRawData data) throws EParseError {


        if (data.length < 10) {
            throw new EParseError("This is not a valid PDF file");
        }

        // Check the PDF header & version -----------------------
        data.pos = 0;
        if (  !( data.checkSignature(PDFKeywords.PDF_HEADER) || data.checkSignature(PDFKeywords.FDF_HEADER) ) ) {
            if (!context.allowScan)
                throw new EParseError("This is not a PDF file");

            while ( !(data.checkSignature(PDFKeywords.PDF_HEADER) || data.checkSignature(PDFKeywords.FDF_HEADER))
                    && (data.pos < context.headerLookupRange) && (data.pos < data.length) ) data.pos++;

            if (  !(data.checkSignature(PDFKeywords.PDF_HEADER) || data.checkSignature(PDFKeywords.FDF_HEADER)) )
                throw new EParseError("This is not a PDF file (PDF header not found)");
        }

        if (data.length - data.pos < 10)
            throw new EParseError("This is not a valid PDF file");


        if ((data.src[data.pos + 5] != '1') || (data.src[data.pos + 7] < '1') || (data.src[data.pos + 7] > '8')) {
            throw new EParseError("PDF version is not supported");
        }

        documentVersion = (data.src[data.pos + 5] - '0') + (data.src[data.pos + 7] - '0')*0.1f;


        // Scan for EOF -----------------------------------------
        if (data.reverseScan(data.length, PDFKeywords.EOF, context.eofLookupRange) < 0)
            throw new EParseError("Missing end of file marker");

        // Scan for 'startxref' marker --------------------------
        if (data.reverseScan(data.pos, PDFKeywords.STARTXREF, 100) < 0)
            throw new EParseError("Missing 'startxref' marker");


        // Fetch XREF offset ------------------------------------
        data.pos += 10;
        data.skipWS();

        int xref_offset = COSNumber.readInteger(data);

        if ((xref_offset == 0) || (xref_offset >= data.length)) {
            throw new EParseError("Invalid xref offset");
        }

        data.pos = xref_offset;
        xref.parse(data, this);


    }

    public void setErrorHandlingPolicy(int policy) {
        if ((policy < 0) || (policy > 3))
            throw new IllegalArgumentException("Policy should be between 0 and 3");

        context.errorHandlingPolicy = policy;
    }
    public int getErrorHandlingPolicy() {
        return context.errorHandlingPolicy;
    }

    /**
     * Tell if this document is encrypted or not.
     *
     * @return true If this document is encrypted.
     */
    public boolean isEncrypted() {
        return documentIsEncrypted;
    }

    public byte[][] getDocumentId() {
        return documentId;
    }

    /**
     * Get the document info dictionary.  This is guaranteed to not return null.
     *
     * @return The documents /Info dictionary
     */
    public PDFDocInfo getDocumentInfo() throws EParseError {
        if (documentInfo != null)
            return documentInfo;

        COSDictionary dictInfo;
        try {
            dictInfo = cache.getDictionary(infoID.id, infoID.gen, false);
        } catch (EParseError e) {
            if (context.errorHandlingPolicy == ParsingContext.EP_THROW_EXCEPTION)
                throw e;
            dictInfo = null;
        }

        documentInfo = new PDFDocInfo(dictInfo, cache);
        return documentInfo;
    }

    /**
     * Return the total page count of the PDF document.
     *
     * @return The total number of pages in the PDF document.
     */
    public int getPagesCount() throws EParseError {
        if (rootID == null)
            return 0;
        if (dictRoot == null)
          dictRoot = cache.getDictionary(rootID.id, rootID.gen, true);

        if (dictRoot == null) return 0;

        COSReference refRootPages = dictRoot.getReference(COSName.PAGES);
        COSDictionary dictRootPages = cache.getDictionary(refRootPages, true);
        return dictRootPages.getUInt(COSName.COUNT, cache, -1);
    }

    public byte[] getXMLMetadata() throws EParseError {
        if (dictRoot == null)
          dictRoot = cache.getDictionary(rootID.id, rootID.gen, true);

        COSReference refMetadata = dictRoot.getReference(COSName.METADATA);
        if (refMetadata == null)
            return null;
        COSStream dictMetadata = cache.getStream(refMetadata, false);
        if (dictMetadata == null)
            return null;
        return dictMetadata.getData();
    }

    @Override
    public int onTrailerFound(COSDictionary trailer, int ordering) {
        if (ordering == 0) {
            rootID = trailer.getReference(COSName.ROOT);
            infoID = trailer.getReference(COSName.INFO);

            documentIsEncrypted = trailer.containsKey(COSName.ENCRYPT);

            COSArray Ids = trailer.getArray(COSName.ID, null);
            if (((Ids == null) || (Ids.size()!=2)) && documentIsEncrypted)
                throw new EParseError("Missing (required) file identifier for encrypted document");

            if (Ids != null) {
                if (Ids.size() != 2) {
                    if ((context.errorHandlingPolicy == ParsingContext.EP_THROW_EXCEPTION) || documentIsEncrypted)
                        throw new EParseError("Invalid document ID array size (should be 2)");
                    Ids = null;
                } else {
                    if ((Ids.get(0) instanceof COSString) && (Ids.get(1) instanceof COSString)) {
                        documentId[0] = ((COSString)Ids.get(0)).getBinaryValue();
                        documentId[1] = ((COSString)Ids.get(1)).getBinaryValue();
                    } else if (context.errorHandlingPolicy == ParsingContext.EP_THROW_EXCEPTION)
                        throw new EParseError("Invalid document ID");
                }
            } // Ids != null
        }
        return ParsingEvent.CONTINUE;
    }

    @Override
    public int onEncryptionDictFound(COSDictionary enc, int ordering) {
        if (ordering == 0)
            encryption = enc;
        return ParsingEvent.CONTINUE;
    }

    @Override
    public int onNotSupported(String msg) {
        //throw new UnsupportedOperationException("Not supported yet.");
        return ParsingEvent.CONTINUE;
    }

    public void dbgDump() {
        //xref.dbgPrintAll();
        cache.dbgCacheAll();
        //cache.dbgSaveAllStreams(filepath + File.separator + "[" + filename + "]" );
        //cache.dbgSaveAllObjects(filepath + File.separator + "[" + filename + "]" );

    }
}
