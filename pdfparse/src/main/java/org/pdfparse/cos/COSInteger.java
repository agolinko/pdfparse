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
import org.pdfparse.exception.EParseError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class COSInteger implements COSObject {

    public int value;

    public COSInteger(int val) {
        value = val;
    }

    public COSInteger(PDFRawData src, ParsingContext context) throws EParseError {
        parse(src, context);
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        value = readDecimal(src);
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        writeDecimal(value, dst);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    static public void writeDecimal(int val, OutputStream dst) throws IOException {
        String str = Integer.toString(val);
        for (int i = 0; i < str.length(); i++) {
            dst.write(str.codePointAt(i));
        }
    }

    static public int readDecimal(PDFRawData src) throws EParseError {
        int prev = src.pos;
        int res = 0;
        while (src.pos <= src.length) {
            switch (src.src[src.pos]) {
                case 0x30:
                case 0x31:
                case 0x32:
                case 0x33:
                case 0x34: // 0..4
                case 0x35:
                case 0x36:
                case 0x37:
                case 0x38:
                case 0x39: // 5..9
                    res = res * 10 + (src.src[src.pos] - 0x30);
                    src.pos++;
                    break;
                default:
                    if (prev == src.pos) {
                        throw new EParseError("expected number, but got " + Integer.toHexString(src.src[src.pos]));
                    }
                    return res;
            } // switch
        } // while
        if (prev == src.pos) {
            throw new EParseError("expected number, but got " + Integer.toHexString(src.src[src.pos]));
        }
        return res;
    }
}
