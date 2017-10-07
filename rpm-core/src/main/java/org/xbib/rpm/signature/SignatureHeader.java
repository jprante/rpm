package org.xbib.rpm.signature;

import org.xbib.rpm.header.AbstractHeader;

/**
 *
 */
public class SignatureHeader extends AbstractHeader {

    public SignatureHeader() {
        for (SignatureTag tag : SignatureTag.values()) {
            tags.put(tag.getCode(), tag);
        }
    }

    @Override
    protected boolean pad() {
        return true;
    }

}
