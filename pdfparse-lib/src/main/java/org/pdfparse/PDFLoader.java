/*
 * Copyright (c) 2019 Anton Golinko
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

import org.pdfparse.cos.COSDictionary;
import org.pdfparse.cos.COSReference;
import org.pdfparse.exception.EParseError;
import org.pdfparse.model.PDFDocCatalog;
import org.pdfparse.model.PDFDocInfo;
import org.pdfparse.parser.PDFParser;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PDFLoader implements ParsingEvent {
    private PDFParser pdfParser;

    private COSReference rootRef = null;
    private COSReference infoRef = null;
    private COSReference encryptionRef = null;

    private PDFDocInfo documentInfo = null;
    private PDFDocCatalog documentCatalog = null;

    private byte[][] documentId = {null,null};
    private boolean documentIsEncrypted = false;
    private int majorVersion;
    private int minorVersion;

    public PDFLoader(String filename) throws EParseError, IOException {
        File file = new File(filename);
        open(file);
    }

    public PDFLoader(File file) throws EParseError, IOException {
        open(file);
    }

    public PDFLoader(byte[] buffer) throws EParseError {
        open(buffer);
    }

    private void open(File file) throws EParseError, IOException {
        FileInputStream fin = new FileInputStream(file);
        byte[] contents = new byte[(int) file.length()];

        fin.read(contents);
        open(contents);
    }

    private void open(byte[] buffer) throws EParseError {
        PDFRawData data = new PDFRawData(buffer);
        pdfParser = new PDFParser(data, this);
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

        COSDictionary dictInfo = null;
        if (infoRef != null)
            dictInfo = pdfParser.getDictionary(infoRef);

        documentInfo = new PDFDocInfo(dictInfo, pdfParser);
        return documentInfo;
    }

    /**
     * This will get the document CATALOG. This is guaranteed to not return null.
     *
     * @return The documents /Root dictionary
     */
    public PDFDocCatalog getDocumentCatalog() throws EParseError {
        if (documentCatalog == null)
        {
            COSDictionary dictRoot;
            dictRoot = pdfParser.getDictionary(rootRef);

            documentCatalog = new PDFDocCatalog(pdfParser, dictRoot);
        }
        return documentCatalog;
    }

    public int getDocumentMajorVersion() {
        return majorVersion;
    }
    public int getDocumentMinorVersion() {
        return minorVersion;
    }

    @Override
    public void onDocumentVersionFound(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    @Override
    public void onDocumentLoaded(COSReference rootId, COSReference infoId, COSReference encryptionId) {
        this.rootRef = rootId;
        this.infoRef = infoId;
        this.encryptionRef = encryptionId;
    }

    public void dbgDump() {
        //xref.dbgPrintAll();
        pdfParser.parseAndDecodeAllObjects();
        //cache.dbgSaveAllStreams(filepath + File.separator + "[" + filename + "]" );
        //cache.dbgSaveAllObjects(filepath + File.separator + "[" + filename + "]" );

    }
}
