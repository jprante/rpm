package org.xbib.rpm.header;

import org.xbib.rpm.signature.SignatureTag;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Tags {

    private static final Map<Integer, EntryType> tags = new HashMap<>();

    static {
        for (HeaderTag tag : HeaderTag.values()) {
            tags.put(tag.getCode(), tag);
        }
        for (SignatureTag tag : SignatureTag.values()) {
            tags.put(tag.getCode(), tag);
        }
    }

    public static EntryType from(int code) {
        return tags.get(code);
    }

    public static Map<Integer, EntryType> tags() {
        return tags;
    }
}
