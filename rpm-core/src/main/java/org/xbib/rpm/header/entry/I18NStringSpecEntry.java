package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

/**
 *
 */
public class I18NStringSpecEntry extends StringSpecEntry {

    @Override
    public int getType() {
        return EntryType.I18NSTRING_ENTRY;
    }
}
