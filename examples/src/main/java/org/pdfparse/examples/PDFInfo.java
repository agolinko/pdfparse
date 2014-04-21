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

package org.pdfparse.examples;

import org.pdfparse.model.PDFDocCatalog;
import org.pdfparse.model.PDFDocInfo;
import org.pdfparse.model.PDFDocument;

public class PDFInfo {
    public static void main(String[] args) {
        if( args.length != 1 ) {
            usage();
            return;
        }

        try {
            // Create document object. Open file
            PDFDocument doc = new PDFDocument(args[0]);

            // Get document structure elements
            PDFDocInfo info = doc.getDocumentInfo();
            PDFDocCatalog cat = doc.getDocumentCatalog();


            System.out.printf("File: %s\r\n", args[0]);
            System.out.println("--- Document info:");
            System.out.printf("Subject: %s\r\n", info.getSubject());
            System.out.printf("Title: %s\r\n", info.getTitle());
            System.out.printf("Author: %s\r\n", info.getAuthor());
            System.out.printf("Creator: %s\r\n", info.getCreator());
            System.out.printf("Producer: %s\r\n", info.getProducer());
            System.out.printf("Creation date: %s\r\n", info.getCreationDate().getTime());
            System.out.printf("Keywords: %s\r\n", info.getKeywords());
            System.out.println("--- Document catalog:");
            System.out.printf("Pages count: %d\r\n", cat.getPagesCount());
            System.out.printf("Version: %s\r\n", cat.getVersion());
            System.out.printf("Language: %s\r\n", cat.getLanguage());
            System.out.printf("PageLayout: %s\r\n", cat.getPageLayout().toString());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    public static void usage() {
        System.err.println( "Usage: java org.pdfparse.examples.PDFInfo <pdf-file-name>" );
    }
}
