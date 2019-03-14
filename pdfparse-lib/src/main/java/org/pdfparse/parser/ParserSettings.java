package org.pdfparse.parser;

public class ParserSettings {
    public static final boolean PRETTY_PRINT = true;
    public static final int MIN_PDF_RAW_CONTENT_LENGTH = 10;
    public static final int MAX_SCAN_RANGE = 100;

    public boolean debugMessages = true;
    public boolean ignoreSyntaxCompliance = true;
    public boolean ignoreStructureErrors = true;
    public boolean ignoreDataIntegrityErrors = false;
    public boolean ignoreNonSupportedFeatures = true;

    public boolean allowScan = true;
    public int headerLookupRange = 100;
    public int eofLookupRange = 1024; // Same as Acrobat implementation


    public void setSyntaxComplianceChecks(boolean value) {
        ignoreSyntaxCompliance = !value;
    }
}
