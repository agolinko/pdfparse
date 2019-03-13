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

import org.pdfparse.exception.EParseError;
import org.pdfparse.parser.PDFParser;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.Token;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;


public class COSReference extends COSId implements COSObject {
    public COSReference(int id, int gen) {
        super(id, gen);
    }

    public COSReference(IdGenPair from) {
        super(from.id, from.gen);
    }

    @Override
    public void parse(PDFRawData src, PDFParser pdfFile) throws EParseError {
        if (!tryReadId(src, this, Token.R)) {
            throw new EParseError("Failed to read object reference");
        }
    }

    @Override
    public void produce(OutputStream dst, PDFParser pdfFile) throws IOException {
        String s = String.format("%d %d R", id, gen);
        dst.write(s.getBytes(Charset.defaultCharset()));
    }

    @Override
    public String toString() {
        return String.format("%d %d R", id, gen);
    }

}
