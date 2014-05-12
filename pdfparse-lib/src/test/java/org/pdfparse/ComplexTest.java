package org.pdfparse;

import org.junit.Assert;
import org.junit.Test;
import org.pdfparse.cos.COSName;
import org.pdfparse.exception.EParseError;
import org.pdfparse.model.PDFDocument;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

public class ComplexTest extends Assert
{
    @Test
    public void checkSingleFileParse1() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/Creativecommons-what-is-creative-commons_eng.pdf").toURI();
        File file = new File(uri);

        PDFDocument pp = new PDFDocument(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "", pp.getDocumentInfo().getTitle());
        Assert.assertEquals("Author", "", pp.getDocumentInfo().getAuthor());
        Assert.assertEquals("Subject", "", pp.getDocumentInfo().getSubject());
        Assert.assertEquals("Keywords", "", pp.getDocumentInfo().getKeywords());

        Assert.assertEquals("Creator", "Adobe InDesign CS3 (5.0.1)", pp.getDocumentInfo().getCreator());
        Assert.assertEquals("Producer", "Adobe PDF Library 8.0", pp.getDocumentInfo().getProducer());
        //Assert.assertEquals("Created", pp.getDocumentInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", pp.getDocumentInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.FALSE, pp.getDocumentInfo().getTrapped());

        Assert.assertEquals("Version",  1.4, pp.getDocumentVersion(), 0.01);
        //Assert.assertEquals("Version2",  1.4, pp.getDocumentCatalog().getVersion(), 0.01);
        Assert.assertEquals("Page count", 1, pp.getDocumentCatalog().getPagesCount());

        Assert.assertEquals("Page mode", COSName.PM_NONE, pp.getDocumentCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, pp.getDocumentCatalog().getPageLayout());

        pp.dbgDump();
    }

    @Test
    public void checkSingleFileParse2() throws EParseError, IOException, URISyntaxException {
        URI uri = this.getClass().getResource("/loremipsum2.pdf").toURI();
        File file = new File(uri);

        PDFDocument pp = new PDFDocument(file);

        Assert.assertFalse("Is encrypted", pp.isEncrypted());

        Assert.assertEquals("Title", "Lorem Ipsum", pp.getDocumentInfo().getTitle());
        Assert.assertEquals("Author", "Anton Golinko", pp.getDocumentInfo().getAuthor());
        Assert.assertEquals("Subject", "", pp.getDocumentInfo().getSubject());
        Assert.assertEquals("Keywords", "tag1, tag2, tag3", pp.getDocumentInfo().getKeywords());

        Assert.assertEquals("Creator", "Microsoft® Word 2013", pp.getDocumentInfo().getCreator());
        Assert.assertEquals("Producer", "Microsoft® Word 2013", pp.getDocumentInfo().getProducer());
        //Assert.assertEquals("Created", pp.getDocumentInfo().getCreationDate(),);
        //Assert.assertEquals("Modified", "", pp.getDocumentInfo().getAuthor());
        Assert.assertEquals("Trapped", COSName.UNKNOWN, pp.getDocumentInfo().getTrapped());

        Assert.assertEquals("Version",  1.5, pp.getDocumentVersion(), 0.01);
        //Assert.assertEquals("Version2",  1.4, pp.getDocumentCatalog().getVersion(), 0.01);
        Assert.assertEquals("Page count", 3, pp.getDocumentCatalog().getPagesCount());

        Assert.assertEquals("Page mode", COSName.PM_NONE, pp.getDocumentCatalog().getPageMode());
        Assert.assertEquals("Page layout", COSName.PL_SINGLE_PAGE, pp.getDocumentCatalog().getPageLayout());

        pp.dbgDump();
    }
}
