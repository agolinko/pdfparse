package org.pdfparse;

import org.pdfparse.cos.COSDictionary;
import org.pdfparse.cos.COSReference;
import org.pdfparse.exception.EParseError;
import org.pdfparse.model.PDFDocument;
import org.pdfparse.parser.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PDFFile implements ParsingEvent {
    private XRefTable xref;
    private ParserSettings settings;

    private PDFParser pdfParser;
    private int majorVersion;
    private int minorVersion;

    private COSReference rootRef = null;
    private COSReference infoRef = null;
    private COSReference encryptionRef = null;

    private PDFDocument document;

    public PDFFile() {
        settings = new ParserSettings();
        xref = new XRefTable(settings);
    }

    public PDFFile(String filename) throws EParseError, IOException {
        this();
        open(filename);
    }

    public PDFFile(File file) throws EParseError, IOException {
        this();
        open(file);
    }

    public PDFFile(byte[] buffer) throws EParseError {
        this();
        open(buffer);
    }

    public PDFDocument open(String filename) throws EParseError, IOException {
        File file = new File(filename);
        return open(file);
    }

    public PDFDocument open(File file) throws EParseError, IOException {
        FileInputStream fin = new FileInputStream(file);
        byte[] contents = new byte[(int) file.length()];

        fin.read(contents);
        return open(contents);
    }

    private PDFDocument open(byte[] buffer) throws EParseError {
        xref.clear();
        PDFRawData data = new PDFRawData(buffer);
        pdfParser = new PDFParser(data, xref, settings, this);

        COSDictionary dictRoot = xref.getDictionary(rootRef);
        COSDictionary dictInfo = xref.getDictionary(infoRef);

        document = new PDFDocument(xref, settings, dictRoot, dictInfo);
        return document;
    }

    public void parseEverything() {
        pdfParser.parseAndDecodeAllObjects();
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

    public int getMajorVersion() {
        return majorVersion;
    }
    public int getMinorVersion() {
        return minorVersion;
    }
    public PDFDocument getDocument() {
        return document;
    }

    /**
     * Tell if this document is encrypted or not.
     *
     * @return true If this document is encrypted.
     */
    public boolean isEncrypted() {
        return encryptionRef != null;
    }
}
