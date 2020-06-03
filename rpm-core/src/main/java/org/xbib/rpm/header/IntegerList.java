package org.xbib.rpm.header;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("serial")
public class IntegerList extends ArrayList<Integer> {

    public static IntegerList of(Integer... integers) {
        IntegerList list = new IntegerList();
        list.addAll(Arrays.asList(integers));
        return list;
    }
}
