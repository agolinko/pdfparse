
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

import org.pdfparse.cos.*;
import org.pdfparse.exception.EParseError;
import org.pdfparse.filter.StreamDecoder;

import java.io.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ObjectCache implements ParsingGetObject {
    private XRef pXRef;
    private ParsingContext pContext;
    // null object object indicates that object has not found
    private HashMap<Integer, COSObject> by_id;
    private HashMap<Integer, PDFRawData> decompressedStreams;

    private PDFRawData pData = null;


    public ObjectCache (XRef pXRef, PDFRawData pData, ParsingContext pContext) {
       this.pXRef = pXRef;
       this.pData = pData;
       this.pContext = pContext;
       by_id = new HashMap<Integer, COSObject>();
       decompressedStreams = new HashMap<Integer, PDFRawData>();
    }

    public void done () {
        pXRef = null;
        pContext = null;
        pData = null;

        by_id.clear();
        decompressedStreams.clear();
    }

    public COSObject get(int id, int gen) throws EParseError {
        COSReference header;
        COSObject obj = by_id.get(id);

        if (obj != null)
            return obj;

        XRefEntry x = pXRef.getEntry(id, gen);
        if (x == null) {
            return new COSNull();
        }
        if (x.gen != gen) {
            if (PDFDefines.DEBUG)
                System.out.printf("Object with generation %d not found. But there is %d generation number", gen, x.gen);
        }

        if (!x.isCompressed) {
            pData.pos = x.fileOffset;
            //-----
            header = this.tryFetchIndirectObjHeader(pData);
            if (header == null)
                throw new EParseError(String.format("Invalid indirect object header (expected '%d %d obj' @ %d)", id, gen, pData.pos));
            if ((header.id != id)||(header.gen != gen))
                throw new EParseError(String.format("Object header not correspond data specified in reference (expected '%d %d obj' @ %d)", id, gen, pData.pos));
            pData.skipWS();
            //-----
            obj = this.parseObject(pData, pContext);
            by_id.put(id, obj);
            return obj;
        }

        // Compressed ----------------------------------------------------
        COSObject streamObj = by_id.get(x.containerObjId);
        if (streamObj == null) { // Extract compressed block (stream object)
            XRefEntry cx = pXRef.getEntry(x.containerObjId, 0);
            if (cx == null)
                return new COSNull();
            pData.pos = cx.fileOffset;
            //-----
            header = this.tryFetchIndirectObjHeader(pData);
            if (header == null)
                throw new EParseError("Invalid indirect object header");
            if ((header.id != x.containerObjId)||(header.gen != 0))
                throw new EParseError("Object header not correspond data specified in reference");
            pData.skipWS();
            //-----
            streamObj = this.parseObject(pData, pContext);
            by_id.put(x.containerObjId, streamObj);
        }
        if (! (streamObj instanceof COSStream))
            throw new EParseError("Referenced object-container is not stream object");
        COSStream streamObject = (COSStream)streamObj;


        // --- Ok, received streamObject
        // next, decompress its data, and put in cache
        PDFRawData streamData = decompressedStreams.get(x.containerObjId);
        if (streamData == null) { // is not in cache?

            streamData = StreamDecoder.decodeStream(streamObject.getData(), streamObject, pContext);

            decompressedStreams.put(x.containerObjId, streamData); // put in cache
        }
        // -- OK, retrieved from cache decompressed data
        // Parse stream index & content

        int n = streamObject.getInt(COSName.N, 0);
        int first = streamObject.getInt(COSName.FIRST, 0);
        int idxId, idxOffset, savepos;
        XRefEntry idxXRefEntry;
        COSObject idxObj;
        obj = null;
        for (int i=0; i<n; i++) { // Extract all objects within stream
            idxId = streamData.fetchUInt();
            idxOffset = streamData.fetchUInt();

            // check if it is free object
            idxXRefEntry = pXRef.getEntry(id, 0);
            if (idxXRefEntry == null)
                continue; // this is a free object. skip it

            if (!idxXRefEntry.isCompressed)
                throw new EParseError(String.format("Something strange. Compressed object #%d marked as regular object in XRef", idxId));

            savepos = streamData.pos;
            streamData.pos = first + idxOffset;
            idxObj = this.parseObject(streamData, pContext);
            by_id.put(idxId, idxObj);
            if (idxId == id)
                obj = idxObj; // found it

            streamData.pos = savepos;
        }

        return obj;
    }

    @Override
    public COSObject getObject(COSReference ref) {
        try {
            int savepos = pData.pos;
            COSObject obj = get(ref.id, ref.gen);
            pData.pos = savepos;
            return obj;
        } catch (EParseError ex) {
            return null;
        }
    }

    public COSDictionary getDictionary(int id, int gen, boolean strict) throws EParseError {
        COSObject obj = this.get(id, gen);
        if (obj instanceof COSDictionary) return (COSDictionary)obj;

        if (strict)
            throw new EParseError("Dictionary expected for " + String.valueOf(id) + " " + String.valueOf(gen) + " R. But retrieved object is " + obj.getClass().getName());
        else return null;
    }
    public COSDictionary getDictionary(COSReference ref, boolean strict) throws EParseError {
       return getDictionary(ref.id, ref.gen, strict);
    }

    public COSStream getStream(int id, int gen, boolean strict) throws EParseError {
        COSObject obj = this.get(id, gen);
        if (obj instanceof COSStream) return (COSStream)obj;

        if (strict)
            throw new EParseError("Dictionary expected for " + String.valueOf(id) + " " + String.valueOf(gen) + " R. But retrieved object is " + obj.getClass().getName());
        else return null;
    }
    public COSStream getStream(COSReference ref, boolean strict) throws EParseError {
       return getStream(ref.id, ref.gen, strict);
    }



    public static COSObject parseObject(PDFRawData src, ParsingContext context) throws EParseError {
        byte ch;

        while(true) {
            // skip spaces if any
            int dlen = src.length;
            ch = src.src[src.pos];
            while ((src.pos < dlen)&&((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D))) {
                src.pos++;
                ch = src.src[src.pos];
            }
            //--------------
            ch = src.src[src.pos];
            switch (ch) {
                case 0x25: // '%' - comment
                    src.skipLine();
                    break;
                case 0x2F: // '/' - name
                    return new COSName(src, context);
                case 0x74: // 't' - true
                    //assert(StrLComp(pCurr, 'true', 4) = 0, 'It is not a "true"');
                    src.pos += 4;
                    return new COSBool(true);
                case 0x66: // 'f' - false
                    // Assert(StrLComp(pCurr, 'false', 5) = 0, 'It is not a "false"');
                    src.pos += 5;
                    return new COSBool(false);
                case 0x6E: // 'n' - null
                    // Assert(StrLComp(pCurr, 'false', 5) = 0, 'It is not a "false"');
                    src.pos += 4;
                    return new COSNull();
                case 0x28: // '(' - raw string
                    return new COSString(src, context);
                case 0x3C: // '<' - hexadecimal string
                    if (src.src[src.pos+1] == 0x3C) { // '<'
                        COSDictionary dict = new COSDictionary(src, context);
                        // check for stream object
                        // TODO: Merge COSDictionary and COSStream into one object(class)
                        src.skipWS();
                        if (!src.checkSignature(PDFKeywords.STREAM))
                            return dict; // this is COSDictionary only
                        // this is stream object
                        COSStream stm = new COSStream(dict, src, context);
                        dict.clear();
                        dict = null;
                        return stm;
                    }
                    // this is only Hexadecimal string
                    return new COSString(src, context);
                case 0x5B: // '[' - array
                    return new COSArray(src, context);

                case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: // 0..4
                case 0x35: case 0x36: case 0x37: case 0x38: case 0x39: // 5..9
                case 0x2B: case 0x2D: case 0x2E: // '+', '-', '.'
                    COSReference ref = tryFetchReference(src);
                    if (ref != null)
                        return ref; // this is a valid reference
                    return new COSNumber(src, context);
                default:
                    if (PDFDefines.DEBUG)
                        System.out.println("Bytes before error occurs: " + src.dbgPrintBytes());
                    throw new EParseError("Unknown value token at " + String.valueOf(src.pos));
            } // switch
        } // while
    }

    // if next token is not a reference, function return null (without position changes)
    // else it fetches token and change stream position
    private static COSReference tryFetchReference(PDFRawData src) {
        int pos = src.pos;
        int len = src.length;
        int ch;
        int obj_id = 0, obj_gen = 0;

        // parse int #1 --------------------------------------------
        ch = src.src[pos];
        while ((pos < len)&&(ch >= 0x30)&&(ch <= 0x39)) {
            obj_id = obj_id*10 + (ch - 0x30);
            pos++; // 0..9
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if ((pos >= len)||(!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00))))
            return null;
        pos++; // skip this space

        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((pos < len)&&((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D))) {
            pos++;
            ch = src.src[pos];
        }

        // parse int #2 --------------------------------------------
        while ((pos < len)&&(ch >= 0x30)&&(ch <= 0x39)) {
            obj_gen = obj_gen*10 + (ch - 0x30);
            pos++;
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if ((pos >= len)||(!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00))))
            return null;
        pos++; // skip space


        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((pos < len)&&((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D))) {
            pos++;
            ch = src.src[pos];
        }

        // check if next char is R ---------------------------------
        if (src.src[pos] != 0x52) // 'R'
            return null;

        src.pos = ++pos; // beyond the 'R'

        return new COSReference(obj_id, obj_gen);
    }

    // if next token is not a object header, function return null (without position changes)
    // else it fetches token and change stream position
    public static COSReference tryFetchIndirectObjHeader(PDFRawData src) {
        int pos = src.pos;
        int len = src.length;
        int ch;
        String s = "";
        int obj_id = 0, obj_gen = 0;

        // parse int #1 --------------------------------------------
        ch = src.src[pos];
        while ((pos < len)&&(ch >= 0x30)&&(ch <= 0x39)) {
            obj_id = obj_id*10 + (ch - 0x30);
            pos++; // 0..9
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if ((pos >= len)||(!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00))))
            return null;
        pos++; // skip this space

        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((pos < len)&&((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D))) {
            pos++;
            ch = src.src[pos];
        }

        // parse int #2 --------------------------------------------
        while ((pos < len)&&(ch >= 0x30)&&(ch <= 0x39)) {
            obj_gen = obj_gen*10 + (ch - 0x30);
            pos++;
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if ((pos >= len)||(!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00))))
            return null;
        pos++; // skip space


        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((pos < len)&&((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D))) {
            pos++;
            ch = src.src[pos];
        }

        // check if next char is obj ---------------------------------
        if (!src.checkSignature(pos, PDFKeywords.OBJ)) // 'obj'
            return null;

        src.pos = pos + 3; // beyond the 'obj'

        return new COSReference(obj_id, obj_gen);
    }

    public void dbgSaveAllStreams(String dir) {
        File path = new File (dir);
        path.mkdirs();
        path = null;

        FileOutputStream f;
        for (Integer stmId  : decompressedStreams.keySet()) {
            try {
                f = new FileOutputStream(dir + File.separator + "stream" + stmId.toString() + ".bin");
                f.write(decompressedStreams.get(stmId).src);
                f.flush();
                f.close();
            } catch ( IOException ex) {
                Logger.getLogger(ObjectCache.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void dbgSaveAllObjects(String dir) {
        final byte[] SEPARATOR = {0xD, 0xA, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0xD, 0xA};
        File path = new File (dir);
        path.mkdirs();
        path = null;

        COSObject obj;
        FileOutputStream f;
        ByteArrayOutputStream out;
        PDFRawData decompressedStm;
        for (Integer objId  : by_id.keySet()) {
            try {
                f = new FileOutputStream(dir + File.separator + "obj" + objId.toString() + ".bin");
                obj = by_id.get(objId);
                out = new ByteArrayOutputStream();
                obj.produce(out, pContext);

                if (obj instanceof COSStream) {
                   decompressedStm = decompressedStreams.get(objId);

                if (decompressedStm == null) { // is not in cache?
                    decompressedStm = StreamDecoder.decodeStream(((COSStream)obj).getData(), (COSStream)obj, pContext);
                    decompressedStreams.put(objId.intValue(), decompressedStm); // put in cache
                }

                   if (decompressedStm != null) {
                       out.write(SEPARATOR);
                       out.write(decompressedStm.src);
                   }
                }
                f.write(out.toByteArray());
                f.flush();
                f.close();
                out = null;
            } catch ( EParseError ex) {
                Logger.getLogger(ObjectCache.class.getName()).log(Level.SEVERE, null, ex);
            } catch ( IOException io) {
                Logger.getLogger(ObjectCache.class.getName()).log(Level.SEVERE, null, io);
            }

        }
    }

    public void dbgCacheAll() {
        XRefEntry xre;

        for (Integer id : pXRef.getIdSet()) {
            try {
                xre = pXRef.getEntry(id, 0);
//                if (xre.isCompressed)
//                    continue;
                this.get(id, xre.gen);
            } catch (EParseError ex) {
                Logger.getLogger(ObjectCache.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
