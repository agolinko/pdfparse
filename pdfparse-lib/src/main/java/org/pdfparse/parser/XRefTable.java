package org.pdfparse.parser;

import org.pdfparse.exception.EParseError;
import org.pdfparse.utils.IntObjHashtable;

public class XRefTable {
    private IntObjHashtable<XRefEntry> by_id;
    private ParseSettings settings;

    XRefTable(ParseSettings settings) {
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
            settings.debugMessage("XREF: Got object with zero offset. Assumed that this was a free object(%d %d R)", id, gen);
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
            settings.debugMessage("XREF: Got containerId which is zero. Assumed that this was a free object (%d 0 R)", id);
        }
    }

}
