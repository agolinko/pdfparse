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

import java.util.*;


public class XRef {
    private static final byte[] SIGN_XREF = {0x78, 0x72, 0x65, 0x66};
    private static final byte[] SIGN_TRAILER = {0x74, 0x72, 0x61, 0x69, 0x6C, 0x65, 0x72};

    private ParsingContext pContext;
    private HashMap<Integer, XRefEntry> by_id;
    private int max_id = 0;
    private int max_gen = 0;
    private int max_offset = 0;

    private int compressed_max_stream_id = 0;
    private int compressed_max_stream_offs = 0;

    public XRef(ParsingContext pContext) {
        this.pContext = pContext;
        by_id = new HashMap<Integer, XRefEntry>();
    }

    public void done() {
        pContext = null;
        by_id.clear();
    }

    private void addXref(int id, int gen, int offs) throws EParseError {
        // Skip invalid or not-used objects (assumed that they are free objects)
        if (offs == 0) {
            if (PDFDefines.DEBUG)
                System.out.printf("XREF: Got object with zero offset. Assumed that this was a free object(%d %d R)\r\n", id, gen);
            return;
        }
        if (offs < 0)
            throw new EParseError(String.format("Negative offset for object id=%d", id));

        XRefEntry obj = new XRefEntry();
        obj.id = id;
        obj.gen = gen;
        obj.fileOffset = offs;
        obj.isCompressed = false;

        String name = String.format("%d %d R", id, gen);


        if (!by_id.containsKey(id)) {
            by_id.put(id, obj);
            //obj_order.push(id);
            //all_obj_by_id[id] = obj;
        } else if (by_id.get(id).gen < gen) {
            by_id.put(id, obj);
            //all_obj_by_id[id] = obj;
        }

        if (max_id < id) max_id = id;
        if (max_offset < offs) max_offset = offs;
        if (max_gen < gen) max_gen = gen;
    }

    private void addXrefCompressed(int id, int containerId, int indexWithinContainer) throws EParseError {
        // Skip invalid or not-used objects (assumed that they are free objects)
        if (containerId == 0) {
            if (PDFDefines.DEBUG)
                System.out.printf("XREF: Got containerId which is zero. Assumed that this was a free object (%d 0 R)\r\n", id);
            return;
        }
        if (indexWithinContainer < 0)
            throw new EParseError(String.format("Negative indexWithinContainer for compressed object id=%d in stream #%d", id, containerId));

        XRefEntry obj = new XRefEntry();
        obj.id = id;
        obj.gen = 0;
        obj.fileOffset = 0;
        obj.isCompressed = true;
        obj.containerObjId = containerId;
        obj.indexWithinContainer = indexWithinContainer;

        String name = String.format("%d 0 R", id);
        by_id.put(id, obj);

        //compressed_obj_by_id[id] = obj;
        //compressed_obj_order.push(id);
        //all_obj_by_id[id] = obj;

        if (compressed_max_stream_id<containerId) compressed_max_stream_id = containerId;
        if (compressed_max_stream_offs<indexWithinContainer) compressed_max_stream_offs = indexWithinContainer;
    }

    private void parseTableOnly(PDFRawData src, boolean override) throws EParseError {
        src.skipWS();
        int start;
        int count;
        int i, p;
        int obj_off;
        int obj_gen;
        boolean obj_use;

        while (true) {
        start = src.fetchUInt(); src.skipWS();
        count = src.fetchUInt(); src.skipWS();

        if (start == 1) { // fix incorrect start number
            p = src.pos;
            obj_off = src.fetchUInt();
            obj_gen = src.fetchUInt();
            if (obj_off == 0 && obj_gen == 65535)
                start--;
            src.pos = p;
        }

        for (i = 0; i < count; i++) {
            obj_off = src.fetchUInt();
            obj_gen = src.fetchUInt();
            src.skipWS();
            if (src.getByte(0) == 0x6E) obj_use = true; // 'n'
            else obj_use = false;
            src.pos++; // skip flag
            if (!obj_use) continue;

            if (!override) {
              if (by_id.containsKey(start+i)) continue; // TODO: Optimize this
            }
            addXref(start+i, obj_gen, obj_off);
        }
        src.skipWS();
        byte b = src.getByte(0);
        if ((b < 0x30)||(b > 0x39)) break; // not in [0..9] range
        }// while(1)...
    }

    private COSDictionary parseTableAndTrailer(PDFRawData src, ParsingEvent evt) throws EParseError {
        int prev = src.pos;
        int xrefstrm = 0;
        int res, trailer_ordering = 0;
        COSDictionary curr_trailer = null;
        COSDictionary dic_trailer = null;

        while (prev != 0) {
            src.pos = prev;
            // Parse XREF ---------------------
            if (!src.checkSignature(SIGN_XREF))
                throw new EParseError("This is not an 'xref' table");
            src.pos += SIGN_XREF.length;

            parseTableOnly(src, false);
            // Parse Trailer ------------------
            src.skipWS();
            if (!src.checkSignature(SIGN_TRAILER))
                throw new EParseError("Cannot find 'trailer' tag");
            src.pos += SIGN_TRAILER.length;
            src.skipWS();

            curr_trailer = new COSDictionary(src, pContext);
            prev = curr_trailer.getInt(COSName.PREV, 0);
            if (trailer_ordering == 0)
                dic_trailer = curr_trailer;

            res = evt.onTrailerFound(curr_trailer, trailer_ordering);
            if ((res & ParsingEvent.ABORT_PARSING) != 0)
                return dic_trailer;

            // TODO: mark encrypted objects for removing
            //-----------------------
            if (trailer_ordering == 0) {
                xrefstrm = curr_trailer.getInt(COSName.XREFSTM, 0);
                if (xrefstrm != 0) { // This is an a hybrid PDF-file
                    //res = evt.onNotSupported("Hybrid PDF-files not supported");
                    //if ((res&ParsingEvent.CONTINUE) == 0)
                    //    return dic_trailer;

                    src.pos = xrefstrm;
                    parseXRefStream(src, true, trailer_ordering+1, evt);
                }
            }
            trailer_ordering++;
        } // while
        return dic_trailer;
    }

    private COSDictionary parseXRefStream(PDFRawData src, boolean override, int trailer_ordering, ParsingEvent evt) throws EParseError {
        COSDictionary curr_trailer, dic_trailer = null;
        int res, prev;
        while (true) {
            src.skipWS();

            COSReference x = ObjectCache.tryFetchIndirectObjHeader(src);
            if (x == null)
                throw new EParseError("Invalid indirect object header");

            src.skipWS();


            //addXRef(65530, 0, trailerOffset);
            curr_trailer = new COSDictionary(src, pContext);
            if (trailer_ordering == 0)
                dic_trailer = curr_trailer;

            res = evt.onTrailerFound(curr_trailer, trailer_ordering);
            if ((res & ParsingEvent.ABORT_PARSING) != 0)
                return dic_trailer;

            // TODO: Mark 'encrypt' objects for removing

            if (!curr_trailer.getName(COSName.TYPE, null).equals(COSName.XREF))
                throw new EParseError("This is not a XRef stream");


            COSArray oW = curr_trailer.getArray(COSName.W, null);
            if ((oW == null) || (oW.size() != 3))
                throw new EParseError("Invalid PDF file");
            int[] w = {oW.getInt(0), oW.getInt(1), oW.getInt(2)};

            int size = curr_trailer.getUInt(COSName.SIZE, 0);
            COSArray index = curr_trailer.getArray(COSName.INDEX, null);
            if (index == null) {
                index = new COSArray();
                index.add(new COSInteger(0));
                index.add(new COSInteger(size));
            }

            int row_len = w[0] + w[1] + w[2];

            //byte[] bstream =  // TODO: implement max verbosity mode
            //    src.fetchStream(curr_trailer.getUInt(COSName.LENGTH, 0), false);

            PDFRawData bstream;
            bstream = StreamDecoder.decodeStream(src, curr_trailer, pContext);

            int start;
            int count;
            int index_idx = 0;

            int itype, i2, i3;

            while (index_idx < index.size()) {
                start = index.getInt(index_idx++);
                count = index.getInt(index_idx++);

                for (int i = 0; i < count;) {
                    if (w[0] != 0) itype = bstream.fetchBinaryUInt(w[0]); else itype = 1; // default value (see specs)
                    if (w[1] != 0) i2 = bstream.fetchBinaryUInt(w[1]); else i2 = 0;
                    if (w[2] != 0) i3 = bstream.fetchBinaryUInt(w[2]); else i3 = 0;

                    switch(itype) {
                    case 0:  // linked list of free objects (corresponding to f entries in a cross-reference table).
                        i++; //TODO: mark as free (delete if exist)
                        continue;
                    case 1: // objects that are in use but are not compressed (corresponding to n entries in a cross-reference table).
                        addXref((start+i), i3, i2);
                        i++;
                        continue;
                    case 2: // compressed objects.
                        addXrefCompressed(start+i, i2, i3);
                        i++;
                        continue;
                    default:
                        //throw new EParseError("Invalid iType entry in xref stream");
                        if (PDFDefines.DEBUG)
                            System.out.println("Invalid iType entry in xref stream: " + String.valueOf(itype) );
                        continue;
                    }// switch
                }// for
            } // while

            prev = curr_trailer.getInt(COSName.PREV, 0);
            if (prev != 0) {
                if ((prev < 0) || (prev > src.length))
                    throw new EParseError("Invalid trailer offset");
                src.pos = prev;
                trailer_ordering++;
                continue;
            } else break;
        } // while (true)

        return dic_trailer;

    }

    public COSDictionary parse(PDFRawData src, ParsingEvent evt) throws EParseError {
        src.skipWS();
        if (src.checkSignature(SIGN_XREF))
            return parseTableAndTrailer(src, evt);

        //throw new EParseError("This is not an 'xref' table");
        return parseXRefStream(src, false, 0, evt);
    }

    public XRefEntry getEntry(int id, int gen) {
        return by_id.get((Integer)id);
    }

    public Set<Integer> getIdSet() {
        return by_id.keySet();
    }


    public void dbgPrintAll() {
        System.out.printf("Max id: %d\r\n", max_id);
        System.out.printf("Max gen: %d\r\n", max_gen);
        System.out.printf("Max offset: %d\r\n", max_offset);
        System.out.printf("Compressed max stream id: %d\r\n", compressed_max_stream_id);
        System.out.printf("Compressed max stream offs: %d\r\n", compressed_max_stream_offs);

        XRefEntry xref;
        for (Integer id : by_id.keySet()) {
           xref = by_id.get(id);
           System.out.printf("%d %s\r\n", id.intValue(), xref.toString());
        }

    }

}
