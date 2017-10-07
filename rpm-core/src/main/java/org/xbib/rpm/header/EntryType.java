package org.xbib.rpm.header;

/**
 *
 */
public interface EntryType {

    int INT8_ENTRY = 2;

    int INT16_ENTRY = 3;

    int INT32_ENTRY = 4;

    int INT64_ENTRY = 5;

    int STRING_ENTRY = 6;

    int BIN_ENTRY = 7;

    int STRING_ARRAY_ENTRY = 8;

    int I18NSTRING_ENTRY = 9;

    int getCode();

    int getType();

    String getName();
}
