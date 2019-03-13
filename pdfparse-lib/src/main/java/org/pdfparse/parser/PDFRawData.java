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

package org.pdfparse.parser;

import org.pdfparse.cos.IdGenPair;
import org.pdfparse.exception.EParseError;
import org.pdfparse.utils.ByteBuffer;

import java.util.Arrays;


public final class PDFRawData {
    public byte[] src;
    public int pos;
    public int length;

    // No needed to do it thread local, as PDFRawData is not thread-safe itself
    public IdGenPair tmpIdGenPair = new IdGenPair(0, 0);
    public ByteBuffer tmpBuffer  = new ByteBuffer(1024);

    public PDFRawData() {

    }

    public PDFRawData(byte[] bytearray) {
        src = bytearray;
        pos = 0;
        length = src.length;
    }

    public final void fromByteBuffer(ByteBuffer bb) {
        src = bb.getBuffer();
        pos = 0;
        length = bb.size();
    }

    public final void skipWS() {
        byte ch;
        while (pos < length) {
            ch = src[pos];
            if ((ch != 0x20) && (ch != 0x09) && (ch != 0x0A) && (ch != 0x0D) && (ch != 0x00)) {
                break;
            }
            pos++;
        }
    }
    public static final boolean isWhitespace(int ch) {
        return ((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D) || (ch == 0x00));
    }

    public final void skipLine() {
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

    /**
     * Read a line of text -- it reads character by character until a '\n' is
     * encountered.  If a '\r' is encountered, it is discarded.
     */
    public final String readLine() {
        StringBuilder sb = new StringBuilder();

        while (pos < length) {
            char ch = (char) src[pos++];

            if (ch == '\r') {
                if (pos < length && ((char) src[pos] == '\n')) {
                    pos++;
                }
                break;
            } else if (ch == '\n') {
                break;
            }

            sb.append(ch);
        }

        return sb.toString();
    }

    public final void skipCRLForLF() throws EParseError {
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



    public final int fetchUInt() throws EParseError {
        int prev = pos;
        int res = 0;
        this.skipWS();
        while (pos < length) {
            byte b = src[pos];
            switch (b) {
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
                    res = res * 10 + (b - 0x30);
                    pos++;
                    break;
                default:
                    if (prev == pos) {
                        throw new EParseError("Expected number, but got #" + Integer.toHexString(src[pos]));
                    }
                    return res;
            } // switch
        } // while
        if (prev == pos) {
            throw new EParseError("Expected number, but got " + Integer.toHexString(src[pos]));
        }
        return res;
    }

    //public final byte getByte(int relOffs) {
    //    return src[pos + relOffs];
    //}

    // high-order byte first.
    public final int fetchBinaryUInt(int size) throws EParseError {
        if ((size == 0) || (pos + size > length))
            throw new EParseError("Out of range"); // TODO: special exception

        int r;
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

    public final boolean checkSignature(byte[] sign) {
        int _to = this.pos + sign.length;
        if (_to > this.length) return false;
        for (int i = this.pos, j=0; i<_to; i++, j++)
            if (this.src[i] != sign[j])
                return false;
        return true;
    }

    public final boolean checkSignature(int from, byte[] sign) {
        int _to = from + sign.length;
        if (_to > this.length) return false;
        for (int i = from, j=0; i<_to; i++, j++)
            if (this.src[i] != sign[j])
                return false;
        return true;
    }

    public final int reverseScan(int from,  byte[] sign, int limit) {
        pos = from - sign.length;
        if (pos < 0) {
            pos = 0;
            return -1;
        }

        int scanto = pos - limit;
        if (scanto < 0) scanto = 0;

        boolean found;

        while (pos >= scanto) {
            found = true;
            for (int i = 0; i < sign.length; i++)
                if (this.src[pos + i] != sign[i]) {
                    found = false;
                    break;
                }
            if (found)
                return pos;
            pos--;
        }
        pos = scanto;
        return -1;
    }

    public byte[] readStream(int stream_len, boolean movePosBeyoundEndObj) throws EParseError {
        skipWS();
        if (!checkSignature(Token.STREAM))
            throw new EParseError("'stream' keyword not found");
        pos += Token.STREAM.length;
        skipCRLForLF();
        if (pos + stream_len > length)
            throw new EParseError("Unexpected end of file (stream object too large)");

        // TODO: Lazy parse (reference + start + len)
        byte[] res = Arrays.copyOfRange(src, pos, pos + stream_len);
        pos += stream_len;

        if (movePosBeyoundEndObj) {
            byte firstbyte = Token.ENDOBJ[0];
            int max_pos = length - Token.ENDOBJ.length;
            if (max_pos - pos > ParseSettings.MAX_SCAN_RANGE)
                max_pos = pos + ParseSettings.MAX_SCAN_RANGE;
            for (int i = pos; i < max_pos; i++)
                if ((src[i] == firstbyte) && checkSignature(i, Token.ENDOBJ)) {
                    pos = i + Token.ENDOBJ.length;
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
        int l = 30;
        int r = 30;

        if (this.pos+r > this.length)
            r = this.length - this.pos;
        if (this.pos-l < 0)
            l = this.pos;

        int len = r + l;
        byte[] chunk = new byte[len];

        System.arraycopy(src, pos - l, chunk, 0, len);
        String s = "";

        for (int i=0; i<chunk.length; i++) {
            if (i == l) s += "   [";
            if (chunk[i] > 0x19)
                s += (char)chunk[i];
            else
                s += "x"+ String.format("%02X", chunk[i]&0xFF);
            if (i == l) s += "]   ";
        }

        return s + " @ " + String.valueOf(pos);
    }
}
