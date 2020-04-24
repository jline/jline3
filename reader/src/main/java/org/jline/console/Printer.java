/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.HashMap;
import java.util.Map;

public interface Printer {
    //
    // option names
    //
    //   1) command options
    //
    final static String ALL = "all";
    final static String COLUMNS = "columns";
    final static String EXCLUDE = "exclude";
    final static String INCLUDE = "include";
    final static String INDENTION = "indention";
    final static String MAX_COLUMN_WIDTH = "maxColumnWidth";
    final static String MAX_DEPTH = "maxDepth";
    final static String MAXROWS = "maxrows";
    final static String ONE_ROW_TABLE = "oneRowTable";
    final static String ROWNUM = "rownum";
    final static String SKIP_DEFAULT_OPTIONS = "skipDefaultOptions";
    final static String STRUCT_ON_TABLE = "structsOnTable";
    final static String STYLE = "style";
    final static String TO_STRING = "toString";
    final static String WIDTH = "width";
    //
    //  2) additional PRNT_OPTIONS
    //
    final static String COLUMNS_IN = "columnsIn";
    final static String COLUMNS_OUT = "columnsOut";
    final static String HIGHLIGHT_VALUE = "highlightValue";
    final static String MAP_SIMILARITY = "mapSimilarity";
    final static String OBJECT_TO_MAP = "objectToMap";
    final static String OBJECT_TO_STRING = "objectToString";

    default void println(Object object) {
        println(new HashMap<>(), object);
    }

    void println(Map<String,Object> options, Object object);
}
