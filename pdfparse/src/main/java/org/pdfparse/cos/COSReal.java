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

public class COSReal implements COSObject {

    private float value;

    public COSReal(float val) {
        value = val;
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        dst.write(String.valueOf(value).getBytes()); // TODO: +E
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
