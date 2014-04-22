package org.pdfparse;

import org.junit.Test;
import org.junit.Assert;
import org.pdfparse.exception.EParseError;
import org.pdfparse.model.PDFDocument;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class FileHandlingTest extends Assert
{
    @Test(expected=java.io.FileNotFoundException.class)
    public void checkNonExistentFileParse() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/malformed_pdfs/").toURI();
        File dir = new File(uri);

        PDFDocument pp = new PDFDocument(dir.getAbsolutePath() + "\\nonexistent.pdf");
    }

    @Test(expected=org.pdfparse.exception.EParseError.class)
    public void checkMalformedFileParse() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/malformed_pdfs/noise.pdf").toURI();
        File file = new File(uri);

        PDFDocument pp = new PDFDocument(file);
    }

    @Test
    public void checkEvilPDFs() throws IOException, URISyntaxException {
        PDFDocument pp;

        URI uri = this.getClass().getResource("/malformed_pdfs/").toURI();
        File dir = new File(uri);

        FilenameFilter mask = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith( ".pdf" );
            }
        };

        for (String filename : dir.list(mask)) {
            System.out.printf("---- Parsing file: %s ... ", filename);

            try {
                pp = new PDFDocument(dir.getAbsolutePath() + "\\" + filename);
                pp.dbgDump();
                Assert.fail();
            } catch (EParseError e) {
                System.out.printf(" %s \r\n", e.getMessage());
            }
        }
    }

    //@Test
    public void checkSingleFileParse2() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/testfiles3/source11.pdf").toURI();
        File file = new File(uri);

        PDFDocument pp = new PDFDocument(file);
        pp.dbgDump();
    }

    //@Test
    public void checkMassiveOpeningForCrash() throws IOException, URISyntaxException {
        PDFDocument pp;

        URI uri = this.getClass().getResource("/testfiles/").toURI();
        File dir = new File(uri);

        FilenameFilter mask = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith( ".pdf" );
            }
        };

        for (String filename : dir.list(mask)) {
            System.out.printf("---- Parsing file: %s ...\r\n", filename);


            pp = new PDFDocument(dir.getAbsolutePath() + "\\" + filename);
            pp.dbgDump();
        }
    }

   // @Test
    public void checkMassiveOpeningForCrash2() throws EParseError, IOException, URISyntaxException {
        PDFDocument pp;

        URI uri = this.getClass().getResource("/testfiles2/").toURI();
        File dir = new File(uri);

        FilenameFilter mask = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith( ".pdf" );
            }
        };

        for (String filename : dir.list(mask)) {
            System.out.printf("---- Parsing file: %s ...\r\n", filename);


            pp = new PDFDocument(dir.getAbsolutePath() + "\\" + filename);
            pp.dbgDump();
        }
    }

    //@Test
    public void checkZeroPages() throws EParseError {
        //PDFDocument doc = new PDFDocument();
        //Assert.assertEquals(doc.getPagesCount(), 0);
    }
}
