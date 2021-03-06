pdfparse
========

The PDFParse library is a free, an open source, lightweight and stand-alone Java tool for working with PDF documents.
PDFParse currently not supports encrypted files. Yet.

Example:

```java
package org.pdfparse.examples;

import org.pdfparse.PDFFile;
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
            PDFFile pdf = new PDFFile(args[0]);
            PDFDocument doc = pdf.getDocument();

            // Get document structure elements
            PDFDocInfo info = doc.getInfo();
            PDFDocCatalog cat = doc.getCatalog();


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

    private static void usage() {
        System.err.println( "Usage: java org.pdfparse.examples.PDFInfo <pdf-file-name>" );
    }
}

```