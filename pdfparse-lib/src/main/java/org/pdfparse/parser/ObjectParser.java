package org.pdfparse.parser;

import org.pdfparse.cos.COSObject;
import org.pdfparse.cos.COSReference;

public interface ObjectParser {
    COSObject getObject (COSReference ref);
}
