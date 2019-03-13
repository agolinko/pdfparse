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

package org.pdfparse.model;

import org.pdfparse.cos.COSDictionary;
import org.pdfparse.parser.ObjectRetriever;
import org.pdfparse.parser.ParserSettings;

public class PDFDocument  {
    private ParserSettings settings;
    private COSDictionary dictRoot;
    private COSDictionary dictInfo;
    private ObjectRetriever retriever;

    private PDFDocCatalog catalog;
    private PDFDocInfo info;

    public PDFDocument() {

    }

    public PDFDocument(ObjectRetriever retriever, ParserSettings settings, COSDictionary dictRoot, COSDictionary dictInfo) {
        this.retriever = retriever;
        this.settings = settings;
        catalog = new PDFDocCatalog(retriever, settings, dictRoot);
        info = new PDFDocInfo(dictInfo, retriever);
    }


    /**
     * This will get the document CATALOG. This is guaranteed to not return null.
     *
     * @return The documents /Root dictionary
     */
    public PDFDocCatalog getCatalog() {
        return catalog;
    }

    /**
     * Get the document info dictionary.  This is guaranteed to not return null.
     *
     * @return The documents /Info dictionary
     */
    public PDFDocInfo getInfo() {
        return info;
    }
}
