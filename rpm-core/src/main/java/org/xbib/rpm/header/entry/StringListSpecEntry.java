package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

/**
 *
 */
public class StringListSpecEntry extends StringSpecEntry {

    @Override
    public int getType() {
        return EntryType.STRING_LIST_ENTRY;
    }
}
