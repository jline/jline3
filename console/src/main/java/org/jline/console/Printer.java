/*
 * Copyright (c) 2002-2025, the original author(s).
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
 * Interface for printing objects to the console with various formatting options.
 * The Printer interface provides methods for displaying objects in different formats,
 * such as tables or structured output, with customizable styling and formatting options.
 *
 * Implementations of this interface handle the conversion of various object types to
 * formatted text output, with support for syntax highlighting and structured display.
 */
public interface Printer {
    /**
     * Enumeration specifying which rows in a table should be highlighted.
     */
    enum TableRows {
        /**
         * Highlight only even-numbered rows in the table.
         */
        EVEN,

        /**
         * Highlight only odd-numbered rows in the table.
         */
        ODD,

        /**
         * Highlight all rows in the table.
         */
        ALL
    }
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
    String ALL = "all";
    /**
     * Value: {@code List<String>}<br>
     * Applies: MAP and TABLE<br>
     * Display given keys/columns on map/table.
     */
    String COLUMNS = "columns";
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * Exclude given columns on table.
     */
    String EXCLUDE = "exclude";
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * Include given columns on table.
     */
    String INCLUDE = "include";
    /**
     * Value: Integer<br>
     * Applies: MAP<br>
     * Indention size.
     */
    String INDENTION = "indention";
    /**
     * Value: Integer<br>
     * Applies: MAP and TABLE<br>
     * Maximum column width.
     */
    String MAX_COLUMN_WIDTH = "maxColumnWidth";
    /**
     * Value: Integer<br>
     * Applies: MAP<br>
     * Maximum depth objects are resolved.
     */
    String MAX_DEPTH = "maxDepth";
    /**
     * Value: Integer<br>
     * Applies: MAP and TABLE<br>
     * Maximum number of lines to display.
     */
    String MAXROWS = "maxrows";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Display one row data on table.
     */
    String ONE_ROW_TABLE = "oneRowTable";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Display table row numbers.
     */
    String ROWNUM = "rownum";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Truncate table column names: property.field to field.
     */
    String SHORT_NAMES = "shortNames";
    /**
     * Value: Boolean<br>
     * Applies: MAP and TABLE<br>
     * Ignore all options defined in PRNT_OPTIONS.
     */
    String SKIP_DEFAULT_OPTIONS = "skipDefaultOptions";
    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * Display object structures and lists on table.
     */
    String STRUCT_ON_TABLE = "structsOnTable";
    /**
     * Value: String<br>
     * Use nanorc STYLE<br>
     */
    String STYLE = "style";
    /**
     * Value: Boolean<br>
     * Applies: MAP and TABLE<br>
     * Use object's toString() method to get print value
     * DEFAULT: object's fields are put to property map before printing
     */
    String TO_STRING = "toString";
    /**
     * Value: String<br>
     * Applies: MAP and TABLE<br>
     * Nanorc syntax style used to highlight values.
     */
    String VALUE_STYLE = "valueStyle";
    /**
     * Value: Integer<br>
     * Applies: MAP and TABLE<br>
     * Display width (default terminal width).
     */
    String WIDTH = "width";
    /**
     * Value: String<br>
     * Applies: TABLE<br>
     * Table cell vertical border character.
     */
    String BORDER = "border";
    /**
     * Value: TableRows<br>
     * Applies: TABLE<br>
     * Highlight table rows.
     */
    String ROW_HIGHLIGHT = "rowHighlight";
    //
    //  2) additional PRNT_OPTIONS
    //
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * These map values will be added to the table before all the other keys.
     */
    String COLUMNS_IN = "columnsIn";
    /**
     * Value: {@code List<String>}<br>
     * Applies: TABLE<br>
     * These map values will not be inserted to the table.
     */
    String COLUMNS_OUT = "columnsOut";
    /**
     * Value: {@code Map<regex, function>}.<br>
     * Applies: TABLE<br>
     * If command result map key matches with regex the highlight function is applied
     * to the corresponding map value. The regex = * is processed after all the other regexes and the highlight
     * function will be applied to all map values that have not been already highlighted.
     */
    String HIGHLIGHT_VALUE = "highlightValue";
    /**
     * Value: Double<br>
     * Applies: MAP and TABLE<br>
     * default value 0.8 i.e. if at least of 4 of the 5 results map keys match with reference key set the
     * result will be printed out as a table.
     */
    String MAP_SIMILARITY = "mapSimilarity";
    /**
     * Value: {@code Map<class, function>}<br>
     * Applies: MAP and TABLE<br>
     * Overrides the ScriptEngine toMap() method.
     */
    String OBJECT_TO_MAP = "objectToMap";
    /**
     * Value: {@code Map<class, function>}<br>
     * Applies: MAP and TABLE<br>
     * Overrides the ScriptEngine toString() method.
     */
    String OBJECT_TO_STRING = "objectToString";
    /**
     * Value: Boolean<br>
     * Applies: MAP and TABLE<br>
     * Highlight everything also strings with spaces
     * DEFAULT: highlight only strings without spaces or enclosed by quotes or brackets
     */
    String VALUE_STYLE_ALL = "valueStyleAll";

    /**
     * Value: Boolean<br>
     * Applies: TABLE<br>
     * List the collection of simple values in multiple columns
     * DEFAULT: list values in one column
     */
    String MULTI_COLUMNS = "multiColumns";

    List<String> BOOLEAN_KEYS = Arrays.asList(
            ALL,
            ONE_ROW_TABLE,
            ROWNUM,
            SHORT_NAMES,
            SKIP_DEFAULT_OPTIONS,
            STRUCT_ON_TABLE,
            TO_STRING,
            VALUE_STYLE_ALL,
            MULTI_COLUMNS);

    /**
     * Prints an object to the console using default formatting options.
     * This is a convenience method that calls {@link #println(Map, Object)} with an empty options map.
     *
     * @param object the object to print
     */
    default void println(Object object) {
        println(new HashMap<>(), object);
    }

    /**
     * Prints an object to the console with the specified formatting options.
     * The method handles different object types and formats them according to the provided options.
     *
     * @param options a map of formatting options that control how the object is displayed
     * @param object the object to print
     */
    void println(Map<String, Object> options, Object object);

    /**
     * Executes a print command with the given input.
     * This method can be implemented by printer implementations to handle specific print commands.
     * The default implementation returns null, indicating no error occurred.
     *
     * @param input the command input containing the command and its arguments
     * @return an Exception if an error occurred during command execution, or null if successful
     */
    default Exception prntCommand(CommandInput input) {
        return null;
    }

    /**
     * Clears the printer's syntax highlighter cache and refreshes internal state.
     * This method should be called when the highlighting rules or styles have changed
     * and need to be reapplied.
     *
     * @return true if the refresh operation was successful, false otherwise
     */
    boolean refresh();
}
