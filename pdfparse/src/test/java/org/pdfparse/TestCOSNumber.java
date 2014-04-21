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
import org.pdfparse.cos.COSNumber;
import org.pdfparse.exception.EParseError;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;

import java.util.Random;

public class TestCOSNumber extends Assert{

    Random rnd;

    @Before
    public void setup() {
        rnd = new Random();
    }

    private void setData(PDFRawData data, String value) {
        data.src = value.getBytes();
        data.length = data.src.length;
        data.pos = 0;
    }

    /**
     * Tests equals() - ensures that the Object.equals() contract is obeyed. These are tested over
     * a range of arbitrary values to ensure Consistency, Reflexivity, Symmetry, Transitivity and
     * non-nullity.
     */
    @Test
    public void testEquals()
    {
        // Consistency
        for (int i = -1000; i < 3000; i += 200)
        {
            COSNumber test1 = new COSNumber(i);
            COSNumber test2 = new COSNumber(i);
            COSNumber test3 = new COSNumber(i);

            // Reflexive (x == x)
            assertTrue(test1.equals(test1));
            // Symmetric is preserved ( x==y then y===x)
            assertTrue(test2.equals(test1));
            assertTrue(test1.equals(test2));
            // Transitive (if x==y && y==z then x===z)
            assertTrue(test1.equals(test2));
            assertTrue(test2.equals(test3));
            assertTrue(test1.equals(test3));
            // Non-nullity
            assertFalse(test1.equals(null));
            assertFalse(test2.equals(null));
            assertFalse(test3.equals(null));

            COSNumber test4 = new COSNumber(i + 1);
            assertFalse(test4.equals(test1));
        }

        // Test float values

        // Consistency
        for (int i = -100000; i < 300000; i += 20000)
        {
            float num = i * rnd.nextFloat();
            COSNumber test1 = new COSNumber(num);
            COSNumber test2 = new COSNumber(num);
            COSNumber test3 = new COSNumber(num);
            // Reflexive (x == x)
            assertTrue(test1.equals(test1));
            // Symmetric is preserved ( x==y then y==x)
            assertTrue(test2.equals(test1));
            assertTrue(test1.equals(test2));
            // Transitive (if x==y && y==z then x==z)
            assertTrue(test1.equals(test2));
            assertTrue(test2.equals(test3));
            assertTrue(test1.equals(test3));
            // Non-nullity
            assertFalse(test1.equals(null));
            assertFalse(test2.equals(null));
            assertFalse(test3.equals(null));

            float nf = Float.intBitsToFloat(Float.floatToIntBits(num)+1);
            COSNumber test4 = new COSNumber(nf);
            assertFalse(String.format("%e and %e", num, nf), test4.equals(test1));
        }
    }

    /**
     * Tests hashCode() - ensures that the Object.hashCode() contract is obeyed over a range of
     * arbitrary values.
     */
    @Test
    public void testHashCode()
    {
        for (int i = -1000; i < 3000; i += 200)
        {
            COSNumber test1 = new COSNumber(i);
            COSNumber test2 = new COSNumber(i);
            assertEquals(test1.hashCode(), test2.hashCode());

            COSNumber test3 = new COSNumber(i + 1);
            assertFalse(test3.hashCode() == test1.hashCode());
        }
        // Float
        for (int i = -100000; i < 300000; i += 20000)
        {
            float num = i * rnd.nextFloat();
            COSNumber test1 = new COSNumber(num);
            COSNumber test2 = new COSNumber(num);
            assertEquals(test1.hashCode(), test2.hashCode());

            float nf = Float.intBitsToFloat(Float.floatToIntBits(num)+1);
            COSNumber test3 = new COSNumber(nf);
            assertFalse(test3.hashCode()==test1.hashCode());
        }
    }

    @Test
    public void testIntValue()
    {
        for (int i = -100000; i < 300000; i += 20000)
        {
            float num = i * rnd.nextFloat();
            COSNumber testFloat = new COSNumber(num);
            assertEquals((int) num, testFloat.intValue());
        }

    }

    @Test
    public void checkIntStringParse() throws EParseError {
        COSNumber i = new COSNumber(0);
        PDFRawData data = new PDFRawData();
        ParsingContext context = new ParsingContext();

        setData(data, "-1");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "0");
        i.parse(data, context);
        Assert.assertEquals(0, i.intValue());

        setData(data, "-0000");
        i.parse(data, context);
        Assert.assertEquals(0, i.intValue());

        setData(data, "+1");
        i.parse(data, context);
        Assert.assertEquals(+1, i.intValue());

        setData(data, "1234567890");
        i.parse(data, context);
        Assert.assertEquals(1234567890, i.intValue());

        setData(data, "-1234567890");
        i.parse(data, context);
        Assert.assertEquals(-1234567890, i.intValue());

        setData(data, "+1234567890/");
        i.parse(data, context);
        Assert.assertEquals(1234567890, i.intValue());

        setData(data, "-123/4567890");
        i.parse(data, context);
        Assert.assertEquals(-123, i.intValue());
    }

    @Test
    public void checkFloatStringParse() throws EParseError {
        COSNumber i = new COSNumber(0);
        PDFRawData data = new PDFRawData();
        ParsingContext context = new ParsingContext();

        setData(data, "34.5");
        i.parse(data, context);
        Assert.assertEquals(34.5, i.floatValue(), 0.01);

        setData(data, "-3.62");
        i.parse(data, context);
        Assert.assertEquals(-3.62, i.floatValue(), 0.01);

        setData(data, "+123.6");
        i.parse(data, context);
        Assert.assertEquals(+123.6, i.floatValue(), 0.01);

        setData(data, "4.");
        i.parse(data, context);
        Assert.assertEquals(4, i.floatValue(), 0.01);

        setData(data, "-.002");
        i.parse(data, context);
        Assert.assertEquals(-0.002, i.floatValue(), 0.001);

        setData(data, "0.0");
        i.parse(data, context);
        Assert.assertEquals(0, i.floatValue(), 0.01);

        setData(data, "+.002/");
        i.parse(data, context);
        Assert.assertEquals(+0.002, i.floatValue(), 0.001);

        setData(data, ".001");
        i.parse(data, context);
        Assert.assertEquals(0.001, i.floatValue(), 0.001);
    }

    @Test
    public void checkParseWithSeparators() throws EParseError {
        COSNumber i = new COSNumber(0);
        PDFRawData data = new PDFRawData();
        ParsingContext context = new ParsingContext();

        setData(data, "-1/");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1 [");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1]");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1{{{");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1}}}");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1()-33");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1))");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1<");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1>");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());

        setData(data, "-1%");
        i.parse(data, context);
        Assert.assertEquals(-1, i.intValue());
    }
}
