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

import org.pdfparse.model.PDFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class TestPerformance {
    private static void parseFile(byte[] data) throws IOException {
        PDFDocument pdf;
        pdf = new PDFDocument( data );
        pdf.dbgDump();
        pdf.close();
    }

    public static void main(String[] args)  throws URISyntaxException, IOException {
        URI uri = TestPerformance.class.getResource("/testfiles/").toURI();
        File dir = new File(uri);
        String fn = dir.getAbsolutePath() + "\\AMDC2011Poster.pdf";

        FileInputStream fin = new FileInputStream(fn);
        FileChannel channel = fin.getChannel();

        byte[] buffer = new byte[(int)channel.size()];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.BIG_ENDIAN);
        channel.read(bb);



        for (int i=0; i < 99; i++) {
            parseFile(buffer);
        }
    }
}
