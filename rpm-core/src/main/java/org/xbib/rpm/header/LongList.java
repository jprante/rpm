package org.xbib.rpm.header;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("serial")
public class LongList extends ArrayList<Long> {

    public static LongList of(Long... values) {
        LongList list = new LongList();
        list.addAll(Arrays.asList(values));
        return list;
    }
}
