/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Print object to the console.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public interface Printer {
    //
    // option names
    //
    //   1) command options
    //
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Ignore columnsOut configuration.
     */
    final static String ALL = "all";
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * Display given columns on table.
     */
    final static String COLUMNS = "columns";
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * Exclude given columns on table.
     */
    final static String EXCLUDE = "exclude";
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * Include given columns on table.
     */
    final static String INCLUDE = "include";
    /**
     * Value: Integer<br>
     * Applies: MAP<br>
     * Indention size.
     */
    final static String INDENTION = "indention";
    /**
     * Value: Integer<br>
     * Applies: MAP and TABLE<br>
     * Maximum column width.
     */
    final static String MAX_COLUMN_WIDTH = "maxColumnWidth";
    /**
     * Value: Integer<br>
     * Applies: MAP<br>
     * Maximum depth objects are resolved.
     */
    final static String MAX_DEPTH = "maxDepth";
    /**
     * Value: Integer<br>
     * Applies: MAP and TABLE<br>
     * Maximum number of lines to display.
     */
    final static String MAXROWS = "maxrows";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Display one row data on table.
     */
    final static String ONE_ROW_TABLE = "oneRowTable";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Display table row numbers.
     */
    final static String ROWNUM = "rownum";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Truncate table column names: property.field to field.
     */
    final static String SHORT_NAMES = "shortNames";
    /**
     * Value: Boolean<br>
     * Applies: MAP and TABLE<br>
     * Ignore all options defined in PRNT_OPTIONS.
     */
    final static String SKIP_DEFAULT_OPTIONS = "skipDefaultOptions";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Display object structures and lists on table.
     */
    final static String STRUCT_ON_TABLE = "structsOnTable";
    /**
     * Value: String<br>
     * Use nanorc STYLE<br>
     */
    final static String STYLE = "style";
    /**
     * Value: Boolean<br>
     * Applies: MAP and TABLE<br>
     * Use object's toString() method to get print value
     * DEFAULT: object's fields are put to property map before printing
     */
    final static String TO_STRING = "toString";
    /**
     * Value: String<br>
     * Applies: MAP and TABLE<br>
     * Nanorc syntax style used to highlight values.
     */
    final static String VALUE_STYLE = "valueStyle";
    /**
     * Value: Integer<br>
     * Applies: MAP and TABLE<br>
     * Display width (default terminal width).
     */
    final static String WIDTH = "width";
    //
    //  2) additional PRNT_OPTIONS
    //
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * These map values will be added to the table before all the other keys.
     */
    final static String COLUMNS_IN = "columnsIn";
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * These map values will not be inserted to the table.
     */
    final static String COLUMNS_OUT = "columnsOut";
    /**
     * Value: {@code Map<regex, function>}.<br>
     * Applies: TABLE<br>
     * If command result map key matches with regex the highlight function is applied
     * to the corresponding map value. The regex = * is processed after all the other regexes and the highlight
     * function will be applied to all map values that have not been already highlighted.
     */
    final static String HIGHLIGHT_VALUE = "highlightValue";
    /**
     * Value: Double<br>
     * Applies: MAP and TABLE<br>
     * default value 0.8 i.e. if at least of 4 of the 5 results map keys match with reference key set the
     * result will be printed out as a table.
     */
    final static String MAP_SIMILARITY = "mapSimilarity";
    /**
     * Value: {@code Map<class, function>}<br>
     * Applies: MAP and TABLE<br>
     * Overrides the ScriptEngine toMap() method.
     */
    final static String OBJECT_TO_MAP = "objectToMap";
    /**
     * Value: {@code Map<class, function>}<br>
     * Applies: MAP and TABLE<br>
     * Overrides the ScriptEngine toString() method.
     */
    final static String OBJECT_TO_STRING = "objectToString";

    final static List<String> BOOLEAN_KEYS = Arrays.asList(ALL, ONE_ROW_TABLE, ROWNUM, SHORT_NAMES, SKIP_DEFAULT_OPTIONS
            , STRUCT_ON_TABLE, TO_STRING);

    default void println(Object object) {
        println(new HashMap<>(), object);
    }

    void println(Map<String,Object> options, Object object);

    default Exception prntCommand(CommandInput input) {
        return null;
    }

}
