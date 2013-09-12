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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;


public class COSName implements COSObject {
    public static final COSName PREV = new COSName("/Prev");
    public static final COSName XREFSTM = new COSName("/XRefStm");
    public static final COSName LENGTH = new COSName("/Length");
    public static final COSName TYPE = new COSName("/Type");
    public static final COSName XREF = new COSName("/XRef");
    public static final COSName W = new COSName("/W");
    public static final COSName SIZE = new COSName("/Size");
    public static final COSName INDEX = new COSName("/Index");
    public static final COSName FILTER = new COSName("/Filter");

    public static final COSName FLATEDECODE = new COSName("/FlateDecode");
    public static final COSName FL = new COSName("/Fl");
    public static final COSName ASCIIHEXDECODE = new COSName("/ASCIIHexDecode");
    public static final COSName AHX = new COSName("/AHx");
    public static final COSName ASCII85DECODE = new COSName("/ASCII85Decode");
    public static final COSName A85 = new COSName("/A85");
    public static final COSName LZWDECODE = new COSName("/LZWDecode");
    public static final COSName CRYPT = new COSName("/Crypt");
    public static final COSName RUNLENGTHDECODE = new COSName("/RunLengthDecode");
    public static final COSName JPXDECODE = new COSName("/JPXDecode");
    public static final COSName CCITTFAXDECODE = new COSName("/CCITTFaxDecode");
    public static final COSName JBIG2DECODE = new COSName("/JBIG2Decode");



    public static final COSName DCTDECODE = new COSName("/DCTDecode");
    public static final COSName ENCRYPT = new COSName("/Encrypt");
    public static final COSName DECODEPARMS = new COSName("/DecodeParms");
    public static final COSName PREDICTOR = new COSName("/Predictor");
    public static final COSName COLUMNS = new COSName("/Columns");
    public static final COSName COLORS = new COSName("/Colors");
    public static final COSName BITSPERCOMPONENT = new COSName("/BitsPerComponent");
    public static final COSName ROOT = new COSName("/Root");
    public static final COSName INFO = new COSName("/Info");
    public static final COSName ID = new COSName("/ID");

    public static final COSName TITLE = new COSName("/Title");
    public static final COSName KEYWORDS = new COSName("/Keywords");
    public static final COSName SUBJECT = new COSName("/Subject");
    public static final COSName AUTHOR = new COSName("/Author");
    public static final COSName CREATOR = new COSName("/Creator");
    public static final COSName PRODUCER = new COSName("/Producer");
    public static final COSName CREATIONDATE = new COSName("/CreationDate");
    public static final COSName MODDATE = new COSName("/ModDate");
    public static final COSName TRAPPED = new COSName("/Trapped");
    public static final COSName PAGES = new COSName("/Pages");
    public static final COSName METADATA = new COSName("/Metadata");
    public static final COSName COUNT = new COSName("/Count");

    public static final COSName FIRST = new COSName("/First");
    public static final COSName N = new COSName("/N");







    private byte[] value;

    public COSName(PDFRawData src, ParsingContext context) throws EParseError {
        parse(src, context);
    }

    public COSName(String val) {
        value = val.getBytes(Charset.defaultCharset());
    }

    @Override
    public int hashCode() {
        int hc = Arrays.hashCode(value);
        return hc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final COSName other = (COSName) obj;
        if (!Arrays.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new String(value, Charset.defaultCharset());
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        src.skipWS();
        int p = src.pos;
        int len = src.length;
        int i, cnt = 0;
        byte b, v1, v2;
        boolean stop = false;

        if (src.getByte(0) != 0x2F)
            throw new EParseError("Expected SOLIDUS sign #2F in name object, but got " + Integer.toHexString(src.src[p]));

        p++; // skip '/'

        while ((p <= len) && !stop) {
            b = src.src[p];

            if (b < 0) {
                if (context.errorHandlingPolicy == ParsingContext.EP_THROW_EXCEPTION)
                    throw new EParseError("Illegal character in name token");
            }

            switch (b) {
                // Whitespace
                case 0x00:
                case 0x09:
                case 0x0A:
                case 0x0D:
                case 0x20:
                    stop = true;
                    break;

                // Escape char
                case 0x23:
                    cnt++; // escape char. skip it
                    break;

                // Delimeters
                case 0x28: // ( - LEFT PARENTHESIS
                case 0x29: // ) - RIGHT PARENTHESIS
                case 0x3C: // < - LESS-THAN SIGN
                case 0x3E: // > - GREATER-THAN SIGN
                case 0x5B: // [ - LEFT SQUARE BRACKET
                case 0x5D: // ] - RIGHT SQUARE BRACKET
                case 0x7B: // { - LEFT CURLY BRACKET
                case 0x7D: // } - RIGHT CURLY BRACKET
                case 0x2F: // / - SOLIDUS
                case 0x25: // % - PERCENT SIGN
                    stop = true;
                    break;

                default:
                    if ((b >= 0) && (b < 0x20))
                        throw new EParseError("Illegal character in name token(2)");
                    break;
            } // switch ...

            if (stop) break;
            p++;
        } // while ...

        if (cnt == 0) {
            value = new byte[p - src.pos];
            System.arraycopy(src.src, src.pos, value, 0, value.length);
            src.pos = p;
            return;
        }

        value = new byte[p-src.pos - 2*cnt];
        cnt = 0;
        for (i=src.pos; i<p; i++) {
            if (src.src[i] == 0x23) {
                v1 = (byte)PDFRawData.HEX[src.src[i+1] - 0x30];
                v2 = (byte)PDFRawData.HEX[src.src[i+2] - 0x30];
                value[cnt++] = (byte) ((v1<<4)&(v2&0xF));
                i +=2; //agh!!!!!
            } else
                value[cnt++] = src.src[i];
        }

        src.pos = p;


    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        int cnt = 0;
        int i;
        for (i=0; i<value.length; i++)
            if (value[i] < 0x21) cnt++; // count characters that need escape
        if (cnt == 0) {
          dst.write(value);
          return;
        }

        for (i=0; i<value.length; i++) {
            if (value[i] < 0x21) {
                dst.write(0x23);
                dst.write(0x30 + (value[i]>>4));
                dst.write(0x30 + (value[i]&0xF));
            } else
                dst.write(value[i]);
        }
    }
}
