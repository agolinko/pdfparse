package org.pdfparse.cos;

import org.pdfparse.parser.PDFRawData;

public class IdGenPair {
    public int id;
    public int gen;

    public IdGenPair() {
        this.id = 0;
        this.gen = 0;
    }

    public IdGenPair(int id, int gen) {
        this.id = id;
        this.gen = gen;
    }

    public void set(int id, int gen) {
        this.id = id;
        this.gen = gen;
    }


    // if next token is not a reference/Object header, function return false (without position changes)
    // otherwise it fetches destRef and changes stream position
    public static boolean tryReadId(PDFRawData src, IdGenPair destPair, byte[] endToken) {
        int pos = src.pos;
        int len = src.length;
        int ch;
        int obj_id = 0, obj_gen = 0;

        if (pos >= len) return false;

        // parse int #1 --------------------------------------------
        ch = src.data[pos];
        while ((ch >= 0x30) && (ch <= 0x39)) {
            obj_id = obj_id * 10 + (ch - 0x30);
            pos++; // 0..9
            if (pos >= len) return false;
            ch = src.data[pos];
        }

        //check if not a whitespace or EOF
        if (!((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D) || (ch == 0x00)))
            return false;
        pos++; // skip this space
        if (pos >= len) return false;

        // skip succeeded spaces if any
        ch = src.data[pos];
        while ((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D)) {
            pos++;
            if (pos >= len) return false;
            ch = src.data[pos];
        }

        // parse int #2 --------------------------------------------
        while ((ch >= 0x30) && (ch <= 0x39)) {
            obj_gen = obj_gen * 10 + (ch - 0x30);
            pos++;
            if (pos >= len) return false;
            ch = src.data[pos];
        }

        //check if not a whitespace or EOF
        if (!((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D) || (ch == 0x00)))
            return false;
        pos++; // skip space
        if (pos >= len) return false;

        // skip succeeded spaces if any
        ch = src.data[pos];
        while ((ch == 0x20) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D)) {
            pos++;
            if (pos >= len) return false;
            ch = src.data[pos];
        }

        // check if next token is endToken ---------------------------------
        if (src.checkSignature(pos, endToken)) {
            src.pos = pos + endToken.length;
            destPair.id = obj_id;
            destPair.gen = obj_gen;
            return true;
        }
        return false;
    }
}
