package org.pdfparse.parser;

import org.pdfparse.cos.*;
import org.pdfparse.exception.EGenericException;
import org.pdfparse.exception.EParseError;
import org.pdfparse.utils.IntObjHashtable;

public class XRefTable implements ObjectRetriever {
    private IntObjHashtable<XRefEntry> by_id;
    private ParserSettings settings;

    private ObjectParser parser;

    public XRefTable(ParserSettings settings) {
        by_id = new IntObjHashtable<XRefEntry>();
        this.settings = settings;
    }

    public XRefEntry get(int id) {
        return by_id.get(id);
    }

    public int[] getKeys() {
        return by_id.getKeys();
    }

    public void add(int id, int gen, int offs) throws EParseError {
        // Skip invalid or not-used objects (assumed that they are free objects)
        if (offs == 0) {
            Diagnostics.debugMessage(settings, "XREF: Got object with zero offset. Assumed that this was a free object(%d %d R)", id, gen);
            return;
        }

        XRefEntry xref = new XRefEntry(id, gen, offs, false);
        XRefEntry old_obj = by_id.get(id);

        if (old_obj == null) {
            by_id.put(id, xref);
        } else if (old_obj.gen < gen) {
            // override only if greater Generation
            by_id.put(id, xref);
        }
    }

    public void addCompressed(int id, int containerId, int indexWithinContainer) throws EParseError {
        // Skip invalid or not-used objects (assumed that they are free objects)
        if (containerId > 0) {
            XRefEntry xref = new XRefEntry(id, containerId, indexWithinContainer, true);
            by_id.put(id, xref);
        } else {
            Diagnostics.debugMessage(settings, "XREF: Got containerId which is zero. Assumed that this was a free object (%d 0 R)", id);
        }
    }

    public void setParser(ObjectParser parser) {
        this.parser = parser;
    }

    @Override
    public COSObject getObject(COSReference ref) {
        XRefEntry x = this.get(ref.id);

        if (x == null) {
            Diagnostics.debugMessage(settings, "No XRef entry for object %d %d R. Used COSNull instead", ref.id, ref.gen);
            return new COSNull();
        }

        if (x.gen != ref.gen) {
            Diagnostics.debugMessage(settings, "Object %s not found. But there is object with %d generation number", ref, x.gen);
        }

        if (x.cachedObject != null) {
            return x.cachedObject;
        }

        if (parser != null) {
            return parser.getObject(x);
        }

        throw new EGenericException("Trying to access %s. Object is not loaded/parsed yet", ref);
    }

    @Override
    public COSDictionary getDictionary(COSReference ref) {
        COSObject obj = this.getObject(ref);
        if (obj instanceof COSDictionary)
            return (COSDictionary) obj;

        throw new EParseError("Dictionary expected for %s. But retrieved object is %s", ref, obj.getClass().getName());
    }

    @Override
    public COSStream getStream(COSReference ref) {
        COSObject obj = this.getObject(ref);
        if (obj instanceof COSStream)
            return (COSStream) obj;

        throw new EParseError("Stream expected for %s. But retrieved object is %s", ref, obj.getClass().getName());
    }

    public void clear() {
        by_id.clear();
    }

}
