package org.pdfparse;

import org.junit.Assert;
import org.junit.Test;
import org.pdfparse.cos.COSName;
import org.pdfparse.exception.EParseError;
import org.pdfparse.model.PDFDocument;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ComplexTest extends Assert {
    @Test
    public void checkSingleFileParse_v12() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/24c16.pdf").toURI();
        File file = new File(uri);

        PDFFile pp = new PDFFile();
        PDFDocument doc = pp.open(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "SERIAL 16K (2K X 8) EEPROM", doc.getInfo().getTitle());
        Assert.assertEquals("Author", "SGS-THOMSON Microelectronics", doc.getInfo().getAuthor());
        Assert.assertEquals("Subject", "ST24C16 ST25W16 ST25C16 ST24W16", doc.getInfo().getSubject());
        Assert.assertEquals("Keywords", "Datasheet", doc.getInfo().getKeywords());

        Assert.assertEquals("Creator", "", doc.getInfo().getCreator());
        Assert.assertEquals("Producer", "Acrobat Distiller Command 3.0 for Solaris 2.3 and later (SPARC)", doc.getInfo().getProducer());
        //Assert.assertEquals("Created", doc.getInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.UNKNOWN, doc.getInfo().getTrapped());

        Assert.assertEquals("MajorVersion", 1, pp.getMajorVersion());
        Assert.assertEquals("MinorVersion", 2, pp.getMinorVersion());
        //Assert.assertEquals("Version2",  1.4, doc.getCatalog().getVersion(), 0.01);
        Assert.assertEquals("Page count", 17, doc.getCatalog().getPagesCount());

        Assert.assertEquals("Page mode", COSName.PM_THUMBS, doc.getCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, doc.getCatalog().getPageLayout());

        pp.parseEverything();
    }

    @Test
    public void checkSingleFileParse_v13() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/vrml.pdf").toURI();
        File file = new File(uri);

        PDFFile pp = new PDFFile();
        PDFDocument doc = pp.open(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "PDF", doc.getInfo().getTitle());
        Assert.assertEquals("Author", "Software 995", doc.getInfo().getAuthor());
        Assert.assertEquals("Subject", "Create PDF with Pdf 995", doc.getInfo().getSubject());
        Assert.assertEquals("Keywords", "pdf, create pdf, software, acrobat, adobe", doc.getInfo().getKeywords());

        Assert.assertEquals("Creator", "Pdf995", doc.getInfo().getCreator());
        Assert.assertEquals("Producer", "GNU Ghostscript 7.05", doc.getInfo().getProducer());
        //Assert.assertEquals("Created", doc.getInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.UNKNOWN, doc.getInfo().getTrapped());

        Assert.assertEquals("MajorVersion", 1, pp.getMajorVersion());
        Assert.assertEquals("MinorVersion", 3, pp.getMinorVersion());
        //Assert.assertEquals("Version2",  1.4, doc.getCatalog().getVersion(), 0.01);
        Assert.assertEquals("Page count", 5, doc.getCatalog().getPagesCount());

        Assert.assertEquals("Page mode", COSName.PM_OUTLINES, doc.getCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, doc.getCatalog().getPageLayout());

        pp.parseEverything();
    }

    @Test
    public void checkSingleFileParse_v14() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/Creativecommons-what-is-creative-commons_eng.pdf").toURI();
        File file = new File(uri);

        PDFFile pp = new PDFFile();
        PDFDocument doc = pp.open(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "", doc.getInfo().getTitle());
        Assert.assertEquals("Author", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Subject", "", doc.getInfo().getSubject());
        Assert.assertEquals("Keywords", "", doc.getInfo().getKeywords());

        Assert.assertEquals("Creator", "Adobe InDesign CS3 (5.0.1)", doc.getInfo().getCreator());
        Assert.assertEquals("Producer", "Adobe PDF Library 8.0", doc.getInfo().getProducer());
        //Assert.assertEquals("Created", doc.getInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.FALSE, doc.getInfo().getTrapped());

        Assert.assertEquals("MajorVersion", 1, pp.getMajorVersion());
        Assert.assertEquals("MinorVersion", 4, pp.getMinorVersion());
        //Assert.assertEquals("Version2",  1.4, doc.getCatalog().getVersion(), 0.01);
        Assert.assertEquals("Page count", 1, doc.getCatalog().getPagesCount());

        Assert.assertEquals("Page mode", COSName.PM_NONE, doc.getCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, doc.getCatalog().getPageLayout());

        pp.parseEverything();
    }

    @Test
    public void checkSingleFileParse_v15() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/loremipsum2.pdf").toURI();
        File file = new File(uri);

        PDFFile pp = new PDFFile();
        PDFDocument doc = pp.open(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "Lorem Ipsum", doc.getInfo().getTitle());
        Assert.assertEquals("Author", "Anton Golinko", doc.getInfo().getAuthor());
        Assert.assertEquals("Subject", "", doc.getInfo().getSubject());
        Assert.assertEquals("Keywords", "tag1, tag2, tag3", doc.getInfo().getKeywords());

        Assert.assertEquals("Creator", "Microsoft® Word 2013", doc.getInfo().getCreator());
        Assert.assertEquals("Producer", "Microsoft® Word 2013", doc.getInfo().getProducer());
        //Assert.assertEquals("Created", doc.getInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.UNKNOWN, doc.getInfo().getTrapped());

        Assert.assertEquals("MajorVersion", 1, pp.getMajorVersion());
        Assert.assertEquals("MinorVersion", 5, pp.getMinorVersion());
        //Assert.assertEquals("Version2",  1.4, doc.getCatalog().getVersion(), 0.01);
        Assert.assertEquals("Page count", 3, doc.getCatalog().getPagesCount());

        Assert.assertEquals("Page mode", COSName.PM_NONE, doc.getCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, doc.getCatalog().getPageLayout());

        pp.parseEverything();
    }

    @Test
    public void checkSingleFileParse_v16() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/made-with-cc.pdf").toURI();
        File file = new File(uri);

        PDFFile pp = new PDFFile();
        PDFDocument doc = pp.open(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "", doc.getInfo().getTitle());
        Assert.assertEquals("Author", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Subject", "", doc.getInfo().getSubject());
        Assert.assertEquals("Keywords", "", doc.getInfo().getKeywords());

        Assert.assertEquals("Creator", "Adobe InDesign CC 2017 (Windows)", doc.getInfo().getCreator());
        Assert.assertEquals("Producer", "Adobe PDF Library 15.0", doc.getInfo().getProducer());
        //Assert.assertEquals("Created", doc.getInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.FALSE, doc.getInfo().getTrapped());

        Assert.assertEquals("MajorVersion", 1, pp.getMajorVersion());
        Assert.assertEquals("MinorVersion", 6, pp.getMinorVersion());
        Assert.assertEquals("MinorVersion", 6, pp.getMinorVersion());


        Assert.assertEquals("Page count", 176, doc.getCatalog().getPagesCount());
        Assert.assertEquals("Page mode", COSName.PM_NONE, doc.getCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, doc.getCatalog().getPageLayout());
        Assert.assertEquals("Page layout", "en-US", doc.getCatalog().getLanguage());

        pp.parseEverything();
    }

    @Test
    public void checkSingleFileParse_v17() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/6licenses-flat.pdf").toURI();
        File file = new File(uri);

        PDFFile pp = new PDFFile();
        PDFDocument doc = pp.open(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "", doc.getInfo().getTitle());
        Assert.assertEquals("Author", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Subject", "", doc.getInfo().getSubject());
        Assert.assertEquals("Keywords", "", doc.getInfo().getKeywords());

        Assert.assertEquals("Creator", "Adobe InDesign CS6 (Macintosh)", doc.getInfo().getCreator());
        Assert.assertEquals("Producer", "Adobe PDF Library 10.0.1", doc.getInfo().getProducer());
        //Assert.assertEquals("Created", doc.getInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", doc.getInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.FALSE, doc.getInfo().getTrapped());

        Assert.assertEquals("MajorVersion", 1, pp.getMajorVersion());
        Assert.assertEquals("MinorVersion", 7, pp.getMinorVersion());

        Assert.assertEquals("Page count", 1, doc.getCatalog().getPagesCount());

        Assert.assertEquals("Page mode", COSName.PM_NONE, doc.getCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, doc.getCatalog().getPageLayout());

        pp.parseEverything();
    }
}
