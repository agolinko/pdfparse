
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

package org.pdfparse.filter;


import org.pdfparse.PDFRawData;
import org.pdfparse.ParsingContext;
import org.pdfparse.cos.*;
import org.pdfparse.exception.ENotSupported;
import org.pdfparse.exception.EParseError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class StreamDecoder {

    public static interface FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, final COSDictionary streamDictionary, ParsingContext context) throws EParseError;
    }

    private static final Map<COSName, FilterHandler> defaults;
    static {
        HashMap<COSName, FilterHandler> map = new HashMap<COSName, FilterHandler>();

        map.put(COSName.FLATEDECODE, new Filter_FLATEDECODE());
        map.put(COSName.FL, new Filter_FLATEDECODE());
        map.put(COSName.ASCIIHEXDECODE, new Filter_ASCIIHEXDECODE());
        map.put(COSName.AHX, new Filter_ASCIIHEXDECODE());
        map.put(COSName.ASCII85DECODE, new Filter_ASCII85DECODE());
        map.put(COSName.A85, new Filter_ASCII85DECODE());
        map.put(COSName.LZWDECODE, new Filter_LZWDECODE());
        //map.put(COSName.CCITTFAXDECODE, new Filter_CCITTFAXDECODE());
        map.put(COSName.CRYPT, new Filter_DoNothing());
        map.put(COSName.RUNLENGTHDECODE, new Filter_RUNLENGTHDECODE());

        // ignore this filters
        map.put(COSName.DCTDECODE, new Filter_DoNothing());
        map.put(COSName.JPXDECODE, new Filter_DoNothing());
        map.put(COSName.CCITTFAXDECODE, new Filter_DoNothing());
        map.put(COSName.JBIG2DECODE, new Filter_DoNothing());

        defaults = Collections.unmodifiableMap(map);
    }



    private static byte[] unpredictStream(byte[] stream, int predictor, int column_width) throws EParseError {
        //      2 - TIFF Predictor 2
        //      10 - PNG prediction (on encoding, PNG None on all rows)
        //      11 - PNG prediction (on encoding, PNG Sub on all rows)
        //      12 - PNG prediction (on encoding, PNG Up on all rows)
        //      13 - PNG prediction (on encoding, PNG Average on all rows)
        //      14 - PNG prediction (on encoding, PNG Paeth on all rows)
        //      15 - PNG prediction (on encoding, PNG optimum)

        // The fuckin manual is too complicated.
        // Better read this article: http://forums.adobe.com/thread/664902

        if (predictor == 1) return stream; // 1 - No prediction (the default value)
        if (predictor != 12)
            throw new ENotSupported("Predictor type " + String.valueOf(predictor) + " not supported yet");

        byte[] prev_row = new byte[column_width];

        int src_idx, dst_idx, j;

        // Strip off the last 10 characters of the string. This is the CRC and is unnecessary to
        // extract the raw data.

        //stream.length = stream.length - 10;

        // The first byte on the row will be the predictor type. You can actually change the predictor
        // line-by-line, though I haven't seen an example of this actually happening. For PNG Up prediction
        // (12, as above), the first byte should be 0x02. You should either strip this off (i.e. assume
        // all lines use the same prediction), or write code to change the algorithm based on this number.
        // The simpler solution, albeit potentially hazardous for your reader, is to strip it off

        if (stream.length % (column_width+1) != 0)
            throw new EParseError("Invalid stream length. Must be multiple of " + String.valueOf(column_width+1) + ".");

        dst_idx = 0;
        for (src_idx = 0; src_idx < stream.length; ) {
            src_idx++; // skip first byte of row
            for (j=0; j < column_width; j++) {
                prev_row[j] += stream[src_idx++];
                stream[dst_idx++] = prev_row[j];
            }
        }
        return Arrays.copyOf(stream, dst_idx);
    }

    public static byte[] FLATEDecode(final byte[] src) {
        byte[] buf = new byte[1024];

        Inflater decompressor = new Inflater();
        decompressor.setInput(src);

        // Create an expandable byte array to hold the decompressed data
        ByteArrayOutputStream bos = new ByteArrayOutputStream(src.length);

        try {
            while (!decompressor.finished()) {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            }
        } catch (DataFormatException e) {
          decompressor.end();
          throw new RuntimeException(e);
        }
        decompressor.end();

        return bos.toByteArray();
    }

    /** Decodes a stream that has the LZWDecode filter.
     * @param in the input data
     * @return the decoded data
     */
    public static byte[] LZWDecode(final byte in[]) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LZWDecoder lzw = new LZWDecoder();
        lzw.decode(in, out);
        return out.toByteArray();
    }

    /** Decodes a stream that has the ASCIIHexDecode filter.
     * @param in the input data
     * @return the decoded data
     */
    public static byte[] ASCIIHexDecode(final byte in[], ParsingContext context) throws EParseError {
        PDFRawData data = new PDFRawData();
        data.src = in;
        data.length = in.length;
        data.pos = 0;

        return COSHexString.parseHexStream(data, context);
    }

    /** Decodes a stream that has the ASCII85Decode filter.
     * @param in the input data
     * @return the decoded data
     */
    public static byte[] ASCII85Decode(final byte in[]) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int state = 0;
        int chn[] = new int[5];
        for (int k = 0; k < in.length; ++k) {
            int ch = in[k] & 0xff;
            if (ch == '~')
                break;

            if (PDFRawData.isWhitespace(ch))
                continue;
            if (ch == 'z' && state == 0) {
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                continue;
            }
            if (ch < '!' || ch > 'u')
                throw new RuntimeException("Illegal character in ascii85decode");
            chn[state] = ch - '!';
            ++state;
            if (state == 5) {
                state = 0;
                int r = 0;
                for (int j = 0; j < 5; ++j)
                    r = r * 85 + chn[j];
                out.write((byte)(r >> 24));
                out.write((byte)(r >> 16));
                out.write((byte)(r >> 8));
                out.write((byte)r);
            }
        }
        int r = 0;
        // We'll ignore the next two lines for the sake of perpetuating broken PDFs
//        if (state == 1)
//            throw new RuntimeException("illegal.length.in.ascii85decode");
        if (state == 2) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85 + 85 * 85 * 85  + 85 * 85 + 85;
            out.write((byte)(r >> 24));
        }
        else if (state == 3) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85  + chn[2] * 85 * 85 + 85 * 85 + 85;
            out.write((byte)(r >> 24));
            out.write((byte)(r >> 16));
        }
        else if (state == 4) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85  + chn[2] * 85 * 85  + chn[3] * 85 + 85;
            out.write((byte)(r >> 24));
            out.write((byte)(r >> 16));
            out.write((byte)(r >> 8));
        }
        return out.toByteArray();
    }

    public static PDFRawData decodeStream(byte[] src, COSDictionary dic, ParsingContext context) throws EParseError {
        // Decompress stream
        COSObject objFilter = dic.get(COSName.FILTER);
        if (objFilter != null) {
            COSArray filters = new COSArray();
            if (objFilter instanceof COSName)
                filters.add((COSName)objFilter);
            else if (objFilter instanceof COSArray)
                filters.addAll((COSArray)objFilter);

            byte[] bytes = src;
            for (int i=0; i<filters.size(); i++) {
                COSName currFilterName = (COSName)filters.get(i);
                FilterHandler fhandler = defaults.get(currFilterName);
                if (fhandler == null)
                    throw new EParseError("Stream filter not supported: " + currFilterName.toString());

                bytes = fhandler.decode(bytes, currFilterName, dic.get(COSName.DECODEPARMS), dic, context);
            }
            PDFRawData pd = new PDFRawData();
            pd.length = bytes.length;
            pd.pos = 0;
            pd.src = bytes;
            return pd;
        }
        PDFRawData pd = new PDFRawData();
        pd.length = src.length;
        pd.pos = 0;
        pd.src = src;
        return pd;

//            if (filter.equals(COSName.FLATEDECODE)) {
//               src = FLATEDecode(src);
//            } else if (filter.equals(COSName.DCTDECODE)) {
//                // do nothing
//            } else
//            throw new EParseError("Stream filter not supported: " + filter.toString());
//        }
//        // ------------
//        COSDictionary decodeParams = dic.getDictionary(COSName.DECODEPARMS, null);
//        if (decodeParams != null)
//          src = unpredictStream(src, decodeParams.getInt(COSName.PREDICTOR, 1), decodeParams.getInt(COSName.COLUMNS, 0));
//
//        PDFRawData pd = new PDFRawData();
//        pd.length = src.length;
//        pd.pos = 0;
//        pd.src = src;
//        return pd;
    }

    public static PDFRawData decodeStream(PDFRawData src, COSDictionary dic, ParsingContext context) throws EParseError {
        byte[] bstream =  // TODO: implement max verbosity mode
            src.fetchStream(dic.getUInt(COSName.LENGTH, 0), false);
        return decodeStream(bstream, dic, context);

    }


    /**
     * @param in
     * @param dic
     * @return a byte array
     */
    public static byte[] decodePredictor(final byte in[], final COSDictionary dic) { // TODO: Optimize this
        int predictor = dic.getInt(COSName.PREDICTOR, -1);
        if (predictor < 0)
            return in;

        if (predictor < 10 && predictor != 2)
            return in;

        int width = dic.getInt(COSName.COLUMNS, 1);
        int colors = dic.getInt(COSName.COLORS, 1);
        int bpc = dic.getInt(COSName.BITSPERCOMPONENT, 8);

        int bytesPerPixel = colors * bpc / 8;
        int bytesPerRow = (colors*width*bpc + 7)/8;
        byte[] curr = new byte[bytesPerRow];
        byte[] prior = new byte[bytesPerRow];

        if (predictor == 2) {
			if (bpc == 8) {
				int numRows = in.length / bytesPerRow;
				for (int row = 0; row < numRows; row++) {
					int rowStart = row * bytesPerRow;
					for (int col = 0 + bytesPerPixel; col < bytesPerRow; col++) {
						in[rowStart + col] = (byte)(in[rowStart + col] + in[rowStart + col - bytesPerPixel]);
					}
				}
			}
			return in;
		}

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(in));
        ByteArrayOutputStream fout = new ByteArrayOutputStream(in.length);

        // Decode the (sub)image row-by-row
        while (true) {
            // Read the filter type byte and a row of data
            int filter = 0;
            try {
                filter = dataStream.read();
                if (filter < 0) {
                    return fout.toByteArray();
                }
                dataStream.readFully(curr, 0, bytesPerRow);
            } catch (Exception e) {
                return fout.toByteArray();
            }

            switch (filter) {
                case 0: //PNG_FILTER_NONE
                    break;
                case 1: //PNG_FILTER_SUB
                    for (int i = bytesPerPixel; i < bytesPerRow; i++) {
                        curr[i] += curr[i - bytesPerPixel];        // TODO: bug?
                    }
                    break;
                case 2: //PNG_FILTER_UP
                    for (int i = 0; i < bytesPerRow; i++) {
                        curr[i] += prior[i];
                    }
                    break;
                case 3: //PNG_FILTER_AVERAGE
                    for (int i = 0; i < bytesPerPixel; i++) {
                        curr[i] += prior[i] / 2;
                    }
                    for (int i = bytesPerPixel; i < bytesPerRow; i++) {
                        curr[i] += ((curr[i - bytesPerPixel] & 0xff) + (prior[i] & 0xff))/2;
                    }
                    break;
                case 4: //PNG_FILTER_PAETH
                    for (int i = 0; i < bytesPerPixel; i++) {
                        curr[i] += prior[i];
                    }

                    for (int i = bytesPerPixel; i < bytesPerRow; i++) {
                        int a = curr[i - bytesPerPixel] & 0xff;
                        int b = prior[i] & 0xff;
                        int c = prior[i - bytesPerPixel] & 0xff;

                        int p = a + b - c;
                        int pa = Math.abs(p - a);
                        int pb = Math.abs(p - b);
                        int pc = Math.abs(p - c);

                        int ret;

                        if (pa <= pb && pa <= pc) {
                            ret = a;
                        } else if (pb <= pc) {
                            ret = b;
                        } else {
                            ret = c;
                        }
                        curr[i] += (byte)ret;
                    }
                    break;
                default:
                    // Error -- unknown filter type
                    throw new RuntimeException("PNG filter unknown");
            }
            try {
                fout.write(curr);
            }
            catch (IOException ioe) {
                // Never happens
            }

            // Swap curr and prior
            byte[] tmp = prior;
            prior = curr;
            curr = tmp;
        }
    }

    /**
     * @param in_out
     * @param dic
     * @return a new length
     */
    public static byte[] decodePredictorFast (byte in_out[], final COSDictionary dic, ParsingContext context) {
        int predictor = dic.getInt(COSName.PREDICTOR, -1);
        if (predictor < 0)
            return in_out;

        if (predictor < 10 && predictor != 2)
            return in_out;

        int width = dic.getInt(COSName.COLUMNS, 1);
        int colors = dic.getInt(COSName.COLORS, 1);
        int bpc = dic.getInt(COSName.BITSPERCOMPONENT, 8);

        int bytesPerPixel = colors * bpc / 8;
        int bytesPerRow = (colors*width*bpc + 7)/8;

        if (predictor == 2) {
            if (bpc == 8) {
                int numRows = in_out.length / bytesPerRow;
                for (int row = 0; row < numRows; row++) {
                    int rowStart = row * bytesPerRow;
                    for (int col = 0 + bytesPerPixel; col < bytesPerRow; col++) {       // TODO: Check documentation (BUG ?)
                        int idx = rowStart + col;
                        in_out[idx] = (byte)(in_out[idx] + in_out[idx - bytesPerPixel]);
                    }
                }
            }
            return in_out;
        }

        if (in_out.length < bytesPerPixel + 1) {
            if (context.errorHandlingPolicy == ParsingContext.EP_THROW_EXCEPTION)
                throw new EParseError("Data to small for decoding PNG prediction");
            return in_out;
        }


        int filter = 0;
        int curr_in_idx = 0, curr_out_idx = 0, prior_idx = 0;
        // Decode the first line -------------------
        filter = in_out[curr_in_idx++];

        switch (filter) {
            case 0: //PNG_FILTER_NONE
            case 2: //PNG_FILTER_UP
                //curr[i] += prior[i];
                for (int i = 0; i < bytesPerRow; i++)
                    in_out[curr_out_idx + i] = in_out[curr_in_idx + i];
                break;
            case 1: //PNG_FILTER_SUB
            case 4: //PNG_FILTER_PAETH
                //curr[i] += curr[i - bytesPerPixel];
                for (int i = 0; i < bytesPerPixel; i++)
                    in_out[curr_out_idx + i] = in_out[curr_in_idx + i];

                for (int i = bytesPerPixel; i < bytesPerRow; i++)
                    in_out[curr_out_idx + i] = (byte)((in_out[curr_out_idx + i - bytesPerPixel]&0xff + in_out[curr_in_idx + i]&0xff)&0xff);
                break;
            case 3: //PNG_FILTER_AVERAGE
                for (int i = 0; i < bytesPerPixel; i++)
                    in_out[curr_out_idx + i] = in_out[curr_in_idx + i];

                for (int i = bytesPerPixel; i < bytesPerRow; i++)
                    in_out[curr_out_idx + i] = (byte) ((in_out[curr_in_idx + i - bytesPerPixel] & 0xff)/2);
                break;
            default:
                // Error -- unknown filter type
                throw new RuntimeException("PNG filter unknown");
        }
        curr_in_idx += bytesPerRow;
        curr_out_idx += bytesPerRow;

        //-------------------------


        // Decode the (sub)image row-by-row
        while (true) {
             if (curr_in_idx >= in_out.length)
                 break;

             filter = in_out[curr_in_idx++];

            switch (filter) {
                case 0: //PNG_FILTER_NONE
                    for (int i = 0; i < bytesPerPixel; i++)
                        in_out[curr_out_idx + i] = in_out[curr_in_idx + i];
                    break;
                case 1: //PNG_FILTER_SUB
                    //curr[i] += curr[i - bytesPerPixel];
                    for (int i = 0; i < bytesPerPixel; i++)
                        in_out[curr_out_idx + i] = in_out[curr_in_idx + i];

                    for (int i = bytesPerPixel; i < bytesPerRow; i++)
                        in_out[curr_out_idx + i] = (byte)(((in_out[curr_out_idx + i - bytesPerPixel]&0xff) + (in_out[curr_in_idx + i]&0xff))&0xff);
                    break;
                case 2: //PNG_FILTER_UP
                    for (int i = 0; i < bytesPerRow; i++) {
                        //curr[i] += prior[i];
                        in_out[curr_out_idx + i] = (byte) ((in_out[curr_in_idx + i]&0xff) + (in_out[prior_idx + i]&0xff)&0xff);
                    }
                    break;
                case 3: //PNG_FILTER_AVERAGE
                    for (int i = 0; i < bytesPerPixel; i++) {
                        //curr[i] += prior[i] / 2;
                        in_out[curr_out_idx + i] += (byte) (((in_out[curr_in_idx + i]&0xff) + (in_out[prior_idx + i]&0xff) / 2)&0xff);
                    }
                    for (int i = bytesPerPixel; i < bytesPerRow; i++) {
                        //curr[i] += ((curr[i - bytesPerPixel] & 0xff) + (prior[i] & 0xff))/2;
                        in_out[curr_out_idx + i] += (byte) ((
                                (in_out[curr_out_idx + i - bytesPerPixel] & 0xff)+(in_out[prior_idx + i] & 0xff))/2);
                    }
                    break;
                case 4: //PNG_FILTER_PAETH
                    for (int i = 0; i < bytesPerPixel; i++) {
                        //curr[i] += prior[i];
                        in_out[curr_out_idx + i] = (byte) (((in_out[curr_in_idx + i]&0xff) + (in_out[prior_idx + i]&0xff)&0xff));
                    }

                    for (int i = bytesPerPixel; i < bytesPerRow; i++) {
                        //int a = curr[i - bytesPerPixel] & 0xff;
                        //int b = prior[i] & 0xff;
                        //int c = prior[i - bytesPerPixel] & 0xff;
                        int a = in_out[curr_out_idx + i - bytesPerPixel] & 0xFF;
                        int b = in_out[prior_idx + i] & 0xFF;
                        int c = in_out[prior_idx + i - bytesPerPixel] & 0xFF;

                        int p = a + b - c;
                        int pa = Math.abs(p - a);
                        int pb = Math.abs(p - b);
                        int pc = Math.abs(p - c);

                        int ret;

                        if (pa <= pb && pa <= pc) {
                            ret = a;
                        } else if (pb <= pc) {
                            ret = b;
                        } else {
                            ret = c;
                        }
                        //curr[i] += (byte)ret;
                        in_out[curr_out_idx + i] += (byte)ret;
                    }
                    break;
                default:
                    // Error -- unknown filter type
                    throw new RuntimeException("PNG filter unknown");
            }

            // Swap curr and prior
            prior_idx = curr_out_idx;
            curr_in_idx += bytesPerRow;
            curr_out_idx += bytesPerRow;
        } // while (true) ...

        byte[] res = new byte[curr_out_idx];
        System.arraycopy(in_out,0, res, 0, res.length);
        return res;
    }

    /**
     * Handles FLATEDECODE filter
     */
    private static class Filter_FLATEDECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.FLATEDecode(b);
            if (decodeParams != null)
                b = StreamDecoder.decodePredictorFast(b, (COSDictionary)decodeParams, context);
                //b = StreamDecoder.decodePredictor(b, (COSDictionary)decodeParams);
            return b;
        }
    }

    /**
     * Handles ASCIIHEXDECODE filter
     */
    private static class Filter_ASCIIHEXDECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.ASCIIHexDecode(b, context);
            return b;
        }
    }

    /**
     * Handles ASCIIHEXDECODE filter
     */
    private static class Filter_ASCII85DECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.ASCII85Decode(b);
            return b;
        }
    }

    /**
     * Handles LZWDECODE filter
     */
    private static class Filter_LZWDECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.LZWDecode(b);
            if (decodeParams != null)
                b = StreamDecoder.decodePredictorFast(b, (COSDictionary)decodeParams, context);
                //b = StreamDecoder.decodePredictor(b, (COSDictionary)decodeParams);
            return b;
        }
    }


    /**
     * A filter that doesn't modify the stream at all
     */
    private static class Filter_DoNothing implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            return b;
        }
    }

    /**
     * Handles RUNLENGTHDECODE filter
     */
    private static class Filter_RUNLENGTHDECODE implements FilterHandler{

        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
         // allocate the output buffer
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte dupCount = -1;
            for(int i = 0; i < b.length; i++){
                dupCount = b[i];
                if (dupCount == -128) break; // this is implicit end of data

                if (dupCount >= 0 && dupCount <= 127){
                    int bytesToCopy = dupCount+1;
                    baos.write(b, i, bytesToCopy);
                    i+=bytesToCopy;
                } else {
                    // make dupcount copies of the next byte
                    i++;
                    for(int j = 0; j < 1-(int)(dupCount);j++){
                        baos.write(b[i]);
                    }
                }
            }

            return baos.toByteArray();
        }
    }

}
