package org.xbib.rpm.header;

/**
 *
 */
public class Header extends AbstractHeader {

    public Header() {
        for (HeaderTag tag : HeaderTag.values()) {
            tags.put(tag.getCode(), tag);
        }
    }

    @Override
    protected boolean pad() {
        return false;
    }
}
