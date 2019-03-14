package org.pdfparse.parser;

public class Token {
    public static final byte[] R = {0x52};
    public static final byte[] OBJ = {0x6F, 0x62, 0x6A};
    public static final byte[] ENDOBJ = {0x65, 0x6E, 0x64, 0x6F, 0x62, 0x6A};

    public static final byte[] STREAM = {0x73, 0x74, 0x72, 0x65, 0x61, 0x6D};
    public static final byte[] ENDSTREAM = {0x65, 0x6E, 0x64, 0x73, 0x74, 0x72, 0x65, 0x61, 0x6D};

    public static final byte[] PDF_HEADER = {0x25, 0x50, 0x44, 0x46, 0x2D}; // "%PDF-";
    public static final byte[] FDF_HEADER = {0x25, 0x46, 0x44, 0x46, 0x2D}; // "%FDF-";

    public static final byte[] EOF = {0x25, 0x25, 0x45, 0x4F, 0x46}; // "%%EOF"
    public static final byte[] STARTXREF = {0x73, 0x74, 0x61, 0x72, 0x74, 0x78, 0x72, 0x65, 0x66}; // "startxref"

    public static final byte[] XREF = {0x78, 0x72, 0x65, 0x66};
    public static final byte[] TRAILER = {0x74, 0x72, 0x61, 0x69, 0x6C, 0x65, 0x72};
}
