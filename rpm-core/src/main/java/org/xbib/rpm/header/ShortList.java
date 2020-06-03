package org.xbib.rpm.header;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("serial")
public class ShortList extends ArrayList<Short> {

    public static ShortList of(Short... values) {
        ShortList list = new ShortList();
        list.addAll(Arrays.asList(values));
        return list;
    }
}
