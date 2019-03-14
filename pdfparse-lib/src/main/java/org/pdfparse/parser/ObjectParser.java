package org.pdfparse.parser;

import org.pdfparse.cos.COSObject;

public interface ObjectParser {
    COSObject getObject(XRefEntry xref);
}
