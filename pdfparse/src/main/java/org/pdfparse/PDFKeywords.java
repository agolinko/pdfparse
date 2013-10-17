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


public class PDFKeywords {
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
