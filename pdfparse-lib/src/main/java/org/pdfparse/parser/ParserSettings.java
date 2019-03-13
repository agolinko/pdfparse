package org.pdfparse.parser;

public class ParserSettings {
    public static final boolean PRETTY_PRINT = true;
    public static final int MIN_PDF_RAW_CONTENT_LENGTH = 10;
    public static final int MAX_SCAN_RANGE = 100;

    public boolean debugMessages = true;
    public boolean checkSyntaxCompliance = false;
    public boolean ignoreErrors = false;
    public boolean ignoreBasicSyntaxErrors = false;
    public boolean ignoreNonSupportedFeatures = true;

    public boolean allowScan = true;
    public int headerLookupRange = 100;
    public int eofLookupRange = 100;


    public void setSyntaxComplianceChecks(boolean value) {
        checkSyntaxCompliance = value;
    }
}
