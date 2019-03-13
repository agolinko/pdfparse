package org.pdfparse.parser;

import org.pdfparse.exception.EParseError;

public class ParseSettings {
    public static final boolean PRETTY_PRINT = true;
    public static final int MIN_PDF_RAW_CONTENT_LENGTH = 10;
    public static final int MAX_SCAN_RANGE = 100;

    private boolean debugMessages = true;
    private boolean checkSyntaxCompliance = false;
    private boolean ignoreErrors = false;
    private boolean ignoreBasicSyntaxErrors = false;
    private boolean ignoreNonSupportedFeatures = true;

    public boolean allowScan = true;
    public int headerLookupRange = 100;
    public int eofLookupRange = 100;


    private void checkAndLog(boolean canContinue, String message) {
        if (canContinue)
            System.err.println(message);
        else
            throw new EParseError(message);
    }

    public boolean softAssertSyntaxComliance(boolean condition, String message) {
        if (!condition)
            checkAndLog(checkSyntaxCompliance, message);
        return condition;
    }

    public boolean softAssertSupportedFeatures(boolean condition, String message) {
        if (!condition)
            checkAndLog(ignoreNonSupportedFeatures, message);
        return condition;
    }

    public boolean softAssertFormatError(boolean condition, String message) {
        if (!condition)
            checkAndLog(ignoreBasicSyntaxErrors, message);
        return condition;
    }

    public boolean softAssertStructure(boolean condition, String message) {
        if (!condition)
            checkAndLog(ignoreErrors, message);
        return condition;
    }

    public void setSyntaxComplianceChecks(boolean value) {
        checkSyntaxCompliance = value;
    }

    public void debugMessage(String msg) {
        if (debugMessages) {
            System.out.println(msg);
        }
    }

    public void debugMessage(String msg, Object ... args) {
        if (debugMessages) {
            System.out.println(String.format(msg, args));
        }
    }
}
