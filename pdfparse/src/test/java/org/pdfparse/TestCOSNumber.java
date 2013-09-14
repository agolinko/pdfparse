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
import org.junit.Test;
import org.pdfparse.cos.COSNumber;
import org.pdfparse.exception.EParseError;

public class TestCOSNumber {

    private void setData(PDFRawData data, String value) {
        data.src = value.getBytes();
        data.length = data.src.length;
        data.pos = 0;
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
