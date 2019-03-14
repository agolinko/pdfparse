package org.pdfparse;

import org.junit.Assert;
import org.junit.Test;
import org.pdfparse.exception.EParseError;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class FileHandlingTest extends Assert {
    @Test(expected = java.io.FileNotFoundException.class)
    public void checkNonExistentFileParse() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/malformed_pdfs/").toURI();
        File dir = new File(uri);

        PDFFile pf = new PDFFile(dir.getAbsolutePath() + "\\nonexistent.pdf");
    }

    @Test(expected = org.pdfparse.exception.EParseError.class)
    public void checkMalformedFileParse() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/malformed_pdfs/noise.pdf").toURI();
        File file = new File(uri);

        PDFFile pp = new PDFFile(file);
    }

    @Test
    public void checkEvilPDFs() throws IOException, URISyntaxException {
        PDFFile pp;

        URI uri = this.getClass().getResource("/malformed_pdfs/").toURI();
        File dir = new File(uri);

        FilenameFilter mask = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".pdf");
            }
        };

        for (String filename : dir.list(mask)) {
            System.out.printf("---- Parsing file: %s ... ", filename);

            try {
                pp = new PDFFile(new File(uri.resolve(filename)));
                pp.parseEverything();
                Assert.fail();
            } catch (EParseError e) {
                System.out.printf(" %s \r\n", e.getMessage());
            }
        }
    }

    @Test
    public void checkMinimalFileParse() throws EParseError, IOException, URISyntaxException {
        String[] files = new String[] {
                "1 - minimal by adobe.pdf",
                "1 - minimal_crlf.pdf",
                "p1 - minimum.pdf"
        };

        for (String filename : files) {
            System.out.println(String.format("Checking '%s'...", filename));
            File file = new File(this.getClass().getResource("/minimal/" + filename).toURI());
            PDFFile pp = new PDFFile(file);
            pp.parseEverything();
        }
    }
}
