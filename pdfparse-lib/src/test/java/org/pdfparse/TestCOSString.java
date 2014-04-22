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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import org.pdfparse.cos.COSString;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;
import org.pdfparse.utils.ByteBuffer;

public class TestCOSString extends Assert {

    private final static String ESC_CHAR_STRING =
            "( test#some) escaped< \\chars>!~1239857 ";
    private final static String ESC_CHAR_STRING_PDF_FORMAT =
            "\\( test#some\\) escaped< \\\\chars>!~1239857 ";


    ParsingContext context;
    PDFRawData data;
    Random random;

    @Before
    public void setup() {
        context = new ParsingContext();
        data = new PDFRawData();
        random = new Random();
        random.setSeed(100);
    }

    private void setData(PDFRawData data, String value) {
        data.src = value.getBytes();
        data.length = data.src.length;
        data.pos = 0;
    }

    private void setString(COSString str, String value) {
        setData(data, value);
        str.parse(data, context);
    }

    @Test
    public void checkParsing() {
        COSString str = new COSString("");
        String s;

        s = " test string <>[]{}}}} ~-=,./ (())";
        setData(data, "(" + s + ")");
        str.parse(data, context);
        Assert.assertEquals(s, str.getValue());
        //----
        setData(data,  "(" + ESC_CHAR_STRING_PDF_FORMAT + ")");
        str.parse(data, context);
        assertEquals(ESC_CHAR_STRING, str.getValue());
    }

    @Test
    public void checkUnicode() {
        String theString = "\u4e16";
        COSString string = new COSString(theString);
        assertTrue(string.getValue().equals(theString));
    }

    @Test
    public void checkOctal() {
        COSString str = new COSString("");

        setString(str, "(\\0053)");
        assertEquals(2, str.getValue().length());
        assertEquals('3', str.getValue().charAt(1));

        setString(str, "(\\053)");
        assertEquals("+", str.getValue());

        setString(str, "(\\053++)");
        assertEquals("+++", str.getValue());
    }
    /**
     * Tests equals(Object) - ensure that the Object.equals() contract is obeyed.
     */
    @Test
    public void testEquals()
    {
        // Check all these several times for consistency
        for (int i = 0; i < 10; i++)
        {
            // Reflexive
            COSString x1 = new COSString("Test");
            assertTrue(x1.equals(x1));

            // Symmetry i.e. if x == y then y == x
            COSString y1 = new COSString("Test");
            assertTrue(x1.equals(y1));
            assertTrue(y1.equals(x1));
            COSString x2 = new COSString("Test");

            x2.setForceHexForm(true);
            // also if x != y then y != x
            assertFalse(x1.equals(x2));
            assertFalse(x2.equals(x1));

            // Transitive if x == y && y == z then x == z
            COSString z1 = new COSString("Test");
            assertTrue(x1.equals(y1));
            assertTrue(y1.equals(z1));
            assertTrue(x1.equals(z1));
            // Test the negative as well if x1 == y1 && y1 != x2 then x1 != x2
            assertTrue(x1.equals(y1));
            assertFalse(y1.equals(x2));
            assertFalse(x1.equals(x2));

            // Non-nullity
            assertFalse(x1.equals(null));
            assertFalse(y1.equals(null));
            assertFalse(z1.equals(null));
            assertFalse(x2.equals(null));

            // Also check other state
            COSString y2 = new COSString("Test");
            y2.setForceLiteralForm(true);
            assertFalse(y2.equals(x2));
            assertTrue(y2.equals(x1));
        }
    }

    @Test
    public void checkProduceParsePair() throws IOException {
        byte[] bytearray = new byte[100];
        COSString str = new COSString("");
        ByteBuffer outbuffer = new ByteBuffer(1024);
        PDFRawData parsebuffer = new PDFRawData();

        for (int i=0; i < 99; i++) {
            random.nextBytes(bytearray);
            str.setBinaryValue(bytearray);

            // === Check HEX form
            str.setForceHexForm(true);

            outbuffer.reset();
            str.produce(outbuffer, context);
            parsebuffer.fromByteBuffer(outbuffer);
            str.clear();
            str.parse(parsebuffer, context);

            assertArrayEquals(bytearray, str.getBinaryValue());

            // === Check Literal form
            str.setForceLiteralForm(true);

            outbuffer.reset();
            str.produce(outbuffer, context);
            parsebuffer.fromByteBuffer(outbuffer);
            str.clear();
            str.parse(parsebuffer, context);

            assertArrayEquals(bytearray, str.getBinaryValue());

        }
    }
}
