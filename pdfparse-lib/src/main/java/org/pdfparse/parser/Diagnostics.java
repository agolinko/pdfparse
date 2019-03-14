package org.pdfparse.parser;

import org.pdfparse.exception.EParseError;

public class Diagnostics {
    ParserSettings settings;

    public Diagnostics(ParserSettings settings) {
        this.settings = settings;
    }


    private static void checkAndLog(boolean canContinue, String message) {
        if (canContinue)
            System.err.println(message);
        else
            throw new EParseError(message);
    }

    public static boolean softAssertSyntaxComliance(ParserSettings settings, boolean condition, String message) {
        if (!condition)
            checkAndLog(settings.ignoreSyntaxCompliance, message);
        return condition;
    }

    public static boolean softAssertSupportedFeatures(ParserSettings settings, boolean condition, String message) {
        if (!condition)
            checkAndLog(settings.ignoreNonSupportedFeatures, message);
        return condition;
    }

    public static boolean softAssertFormatError(ParserSettings settings, boolean condition, String message) {
        if (!condition)
            checkAndLog(settings.ignoreBasicSyntaxErrors, message);
        return condition;
    }

    public static boolean softAssertStructure(ParserSettings settings, boolean condition, String message) {
        if (!condition)
            checkAndLog(settings.ignoreErrors, message);
        return condition;
    }

    public static void debugMessage(ParserSettings settings, String msg) {
        if (settings.debugMessages) {
            System.out.println(msg);
        }
    }


    public static void debugMessage(ParserSettings settings, String msg, Object... args) {
        if (settings.debugMessages) {
            System.out.println(String.format(msg, args));
        }
    }
}
