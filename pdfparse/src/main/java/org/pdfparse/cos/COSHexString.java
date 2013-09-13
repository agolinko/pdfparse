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

package org.pdfparse.cos;

import org.pdfparse.*;
import org.pdfparse.utils.ByteBuffer;
import org.pdfparse.exception.EParseError;
import java.io.IOException;
import java.io.OutputStream;

public class COSHexString extends COSString implements COSObject {

    private static final int[] HEX2V = { // '0'..'f'
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15};
    private static final byte[] V2HEX = { // '0'..'f'
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x61, 0x62, 0x63, 0x64, 0x65, 0x66};

    public COSHexString(String val) {
        super(val);
    }

    public COSHexString(PDFRawData src, ParsingContext context) throws EParseError {
        super(src, context);
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        src.pos++; // Skip the opening bracket '<'
        byte[] bytes = parseHexStream(src, context);
        setBinaryValue(bytes);
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        int i, j, len;
        int b;

        len = binaryValue.length;
        byte[] hex = new byte[binaryValue.length * 2];
        for (i = 0, j = 0; i < len; i++, j += 2) {
            b = binaryValue[i] & 0xFF;
            hex[j] = V2HEX[b >> 4];
            hex[j + 1] = V2HEX[b & 0xF];
        }
        dst.write(0x3C); // "<"
        dst.write(hex);
        dst.write(0x3E); // ">"
    }

    @Override
    public String toString() {
        return "<" + value + ">";
    }

    // result: -1 -on of stream. -2 -illegal character
    private static int fetchHexHalfValue(PDFRawData src) {
        int ch;
        while (src.pos <= src.length) {
            ch = src.src[src.pos];
            // skip spaces
            if ((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0C) || (ch == 0x0D)) {
                src.pos++;
                continue;
            }
            if (ch == 0x3E) {
                return -1;
            }
            if ((ch < 0x30) || (ch > 0x66)) {
                return -2;
            }
            ch = HEX2V[ch - 0x30];
            if (ch < 0) {
                return -2;
            }
            src.pos++;
            return ch;
        }
        return -1;
    }

    public static final byte[] parseHexStream(PDFRawData src, ParsingContext context) throws EParseError {
        int ch, n, n1 = 0;
        boolean first = true;

        //src.pos++; // Skip the opening bracket '<'

        ByteBuffer out = context.tmpBuffer;
        out.reset();
        for (int i = src.pos; i < src.length; i++) {
            ch = src.src[i] & 0xFF;

            if (ch == 0x3E) { // '>' - EOD
                src.pos = i + 1;
                if (!first)
                    out.append((byte)(n1 << 4));
                return out.toByteArray();
            }
            // whitespace ?
            if ((ch == 0x00) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0C) || (ch == 0x0D) || (ch == 0x20))
                continue;

            if ((ch < 0x30) || (ch > 0x66))
                throw new EParseError("Illegal character in hex string");

            n = HEX2V[ch - 0x30];
            if (n < 0)
                throw new EParseError("Illegal character in hex string");

            if (first)
                n1 = n;
            else
                out.append((byte)((n1 << 4) + n));
            first = !first;
        }

        throw new EParseError("Unterminated hexadecimal string"); // ">"
    }
}
