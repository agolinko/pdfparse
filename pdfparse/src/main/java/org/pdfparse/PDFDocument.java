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
import java.io.FileNotFoundException;
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

    private COSDictionary encryption = null;
    private COSReference rootID = null;
    private COSDictionary dictRoot = null;
    private COSReference infoID = null;
    private COSDictionary dictInfo = null;

    private PDFDocInfo docInfo = null;

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


        if (data.length < 8) {
            throw new EParseError("This is not a valid PDF file");
        }

        // Check the PDF version
        data.pos = 0;
        if (!data.checkSignature(PDFKeywords.PDF)) {
            throw new EParseError("This is not a PDF file");
        }

        if ((data.src[5] != '1') || (data.src[7] < '1') || (data.src[7] > '8')) {
            throw new EParseError("PDF version is not supported");
        }

        int pos, beg, len;
        len = data.length - 10;
        beg = len - 100;
        if (beg < 0) {
            beg = 0;
        }
        pos = len;


        // Searching 'startxref' tag
        while (pos > beg) { // startxref 73 74 61 72 74 78 72 65 66
            if ((data.src[pos + 0] == 0x73) && (data.src[pos + 1] == 0x74) && (data.src[pos + 2] == 0x61)
                    && (data.src[pos + 3] == 0x72) && (data.src[pos + 4] == 0x74) && (data.src[pos + 5] == 0x78)
                    && (data.src[pos + 6] == 0x72) && (data.src[pos + 7] == 0x65) && (data.src[pos + 8] == 0x66)) {
                pos += 10;
                break;
            }
            pos--;
        }

        if (pos == 0) {
            throw new EParseError("Cannot find 'startxref' tag");
        }

        // Fetch XREF OFFSET
        data.pos = pos;
        data.skipWS();

        int xref_offset = COSInteger.readDecimal(data);

        if ((xref_offset == 0) || (xref_offset >= data.length)) {
            throw new EParseError("Invalid xref offset");
        }

        data.pos = xref_offset;
        xref.parse(data, this);
    }

    public void setErrorHandlingPolicy(int policy) {
        if ((policy < 0) || (policy > 3))
            throw new IllegalArgumentException("policy should be between 0 and 3");

        context.errorHandlingPolicy = policy;
    }
    public int getErrorHandlingPolicy() {
        return context.errorHandlingPolicy;
    }
    public PDFDocInfo getDocumentInfo() throws EParseError {
        if (docInfo != null)
            return docInfo;

        COSDictionary dictInfo;
        try {
            dictInfo = cache.getDictionary(infoID.id, infoID.gen, false);
        } catch (EParseError e) {
            if (context.errorHandlingPolicy == ParsingContext.EP_THROW_EXCEPTION)
                throw e;
            dictInfo = null;
        }

        docInfo = new PDFDocInfo(dictInfo, cache);
        return docInfo;
    }

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
/*        if (PDFDefines.DEBUG) {
            PDFOutputStream stm = new PDFOutputStream();
            try {
                trailer.produce(stm);
                System.out.println(stm.toString());
            } catch (IOException ex) {
                Logger.getLogger(PDFDocument.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
*/
        if (ordering == 0) {
            rootID = trailer.getReference(COSName.ROOT);
            infoID = trailer.getReference(COSName.INFO);
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
