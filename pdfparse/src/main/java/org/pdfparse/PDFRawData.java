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

import org.pdfparse.exception.EParseError;

import java.util.Arrays;

public class PDFRawData {

    public static final int[] HEX = { // '0'..'f'
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15};
    public byte[] src;
    public int pos;
    public int length;

    public void skipWS() {
        byte ch;
        while (pos < length) {
            ch = src[pos];
            if ((ch != 0x00) && (ch != 0x09) && (ch != 0x0A) && (ch != 0x0D) && (ch != 0x20)) {
                break;
            }
            pos++;
        }
    }
    public static final boolean isWhitespace(int ch) {
        return ((ch == 0x00) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D) || (ch == 0x20));
    }

    public void skipLine() {
        int ch;
        while (pos < length) {
            ch = src[pos];
            if ((ch == 10) || (ch == 13)) {
                break;
            }
            pos++;
        }
        while (pos < length) {
            ch = src[pos];
            if ((ch == 10) || (ch == 13)) {
                pos++;
            } else {
                break;
            }
        }
    }

    public void skipCRLForLF() throws EParseError {
        byte ch;
        ch = src[pos];
        if (ch == 0x0D) {
            pos++;
            if (src[pos] != 0x0A) {
                java.lang.System.out.println("Expected CRLF but got CR alone");
                //throw new ParseError("Expected CRLF but got CR alone");
                return;
            }
            pos++;
            return;
        }
        if (ch == 0x0A) {
            pos++;
            return;
        }
        throw new EParseError("Expected CRLF or LF but got 0x" + java.lang.Integer.toHexString(ch));
    }



    public int fetchUInt() throws EParseError {
        int prev = pos;
        int res = 0;
        this.skipWS();
        while (pos < length) {
            switch (src[pos]) {
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
                    res = res * 10 + (src[pos] - 0x30);
                    pos++;
                    break;
                default:
                    if (prev == pos) {
                        throw new EParseError("expected number, but got #" + Integer.toHexString(src[pos]));
                    }
                    return res;
            } // switch
        } // while
        if (prev == pos) {
            throw new EParseError("expected number, but got " + Integer.toHexString(src[pos]));
        }
        return res;
    }

    // result: -1 -on of stream. -2 -illegal character
    public int fetchHexHalfValue() {
        int ch;
        while (pos < length) {
            ch = src[pos];
            // skip spaces
            if ((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0C) || (ch == 0x0D)) {
                pos++;
                continue;
            }
            if (ch == 0x3E) {
                return -1;
            }
            if ((ch < 0x30) || (ch > 0x66)) {
                return -2;
            }
            ch = HEX[ch - 0x30];
            if (ch < 0) {
                return -2;
            }
            pos++;
            return ch;
        }
        return -1;
    }

    public void writeDecimal(int val) {
    }

    public byte getByte(int relOffs) {
        return src[pos + relOffs];
    }

    // high-order byte first.
    public int fetchBinaryUInt(int size) throws EParseError {
        if ((size == 0) || (pos + size > length))
            throw new EParseError("Out of range"); // TODO: special exception

        int r = 0;
        int b;

        b = src[pos++];
        r = (b & 0xFF);
        if (size == 1) return r;

        b = src[pos++] & 0xFF;
        r = (r<<8) | b;
        if (size == 2) return r;

        b = src[pos++] & 0xFF;
        r = (r<<8) | b;
        if (size == 3) return r;

        b = src[pos++] & 0xFF;
        r = (r<<8) | b;
        if (size == 4) return r;

        throw
            new EParseError("Invalid bytes length");

    }

    public boolean checkSignature(byte[] sign) {
        int _to = this.pos + sign.length;
        if (_to > this.length) return false;
        for (int i = this.pos, j=0; i<_to; i++, j++)
            if (this.src[i] != sign[j])
                return false;
        return true;
    }

    public boolean checkSignature(int from, byte[] sign) {
        int _to = from + sign.length;
        if (_to > this.length) return false;
        for (int i = from, j=0; i<_to; i++, j++)
            if (this.src[i] != sign[j])
                return false;
        return true;
    }

    public byte[] fetchStream(int stream_len, boolean movePosBeyoundEndObj) throws EParseError {
        skipWS();
        if (!checkSignature(PDFKeywords.STREAM))
            throw new EParseError("'stream' keyword not found");
        pos += PDFKeywords.STREAM.length;
        skipCRLForLF();
        if (pos + stream_len > length)
            throw new EParseError("Unexpected end of file (stream object too large)");

        byte[] res = Arrays.copyOfRange(src, pos, pos + stream_len);
        pos += stream_len;

        if (movePosBeyoundEndObj) {
            byte firstbyte = PDFKeywords.ENDOBJ[0];
            int max_pos = length - PDFKeywords.ENDOBJ.length;
            if (max_pos - pos > PDFDefines.MAX_SCAN_RANGE)
                max_pos = pos + PDFDefines.MAX_SCAN_RANGE;
            for (int i = pos; i < max_pos; i++)
                if ((src[i] == firstbyte)&&checkSignature(i, PDFKeywords.ENDOBJ)) {
                    pos = i + PDFKeywords.ENDOBJ.length;
                    return res;
                }

            throw new EParseError("'endobj' tag not found");
        }

        return res;
    }

    public String dbgPrintBytes() {
        int len = 90;

        if (this.pos+len > this.length)
            len = this.length - this.pos;

        byte[] chunk = new byte[len];

        System.arraycopy(src, pos, chunk, 0, len);
        String s = "";

        for (int i=0; i<chunk.length; i++)
            if (chunk[i] > 0x19)
                s += (char)chunk[i];
            else
                s += "x"+ String.format("%02X", chunk[i]&0xFF);

        return s + " @ " + String.valueOf(pos);
    }

    public String dbgPrintBytesBefore() {
        int l = 20;
        int r = 20;

        if (this.pos+r > this.length)
            r = this.length - this.pos;
        if (this.pos-l < 0)
            l = this.pos;

        int len = r + l;
        byte[] chunk = new byte[len];

        System.arraycopy(src, pos - l, chunk, 0, len);
        String s = "";

        for (int i=0; i<chunk.length; i++) {
            if (i == l) s += "  [";
            if (chunk[i] > 0x19)
                s += (char)chunk[i];
            else
                s += "x"+ String.format("%02X", chunk[i]&0xFF);
            if (i == l) s += "]  ";
        }

        return s + " @ " + String.valueOf(pos);
    }
}
