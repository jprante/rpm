package org.xbib.rpm.header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("serial")
public class StringList extends ArrayList<String> {

    public StringList() {
        super();
    }

    public StringList(Collection<String> collection) {
        super(collection);
    }

    public static StringList of(String... values) {
        StringList list = new StringList();
        list.addAll(Arrays.asList(values));
        return list;
    }
}
