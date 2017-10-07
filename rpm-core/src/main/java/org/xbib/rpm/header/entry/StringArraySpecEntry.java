package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

/**
 *
 */
public class StringArraySpecEntry extends StringSpecEntry {

    @Override
    public int getType() {
        return EntryType.STRING_ARRAY_ENTRY;
    }
}
