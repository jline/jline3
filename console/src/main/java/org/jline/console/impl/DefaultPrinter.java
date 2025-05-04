/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jline.builtins.ConfigurationPath;
import org.jline.builtins.Options;
import org.jline.builtins.Styles;
import org.jline.builtins.SyntaxHighlighter;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.Printer;
import org.jline.console.ScriptEngine;
import org.jline.console.SystemRegistry;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Log;
import org.jline.utils.StyleResolver;

import static org.jline.builtins.SyntaxHighlighter.DEFAULT_NANORC_FILE;
import static org.jline.console.ConsoleEngine.VAR_NANORC;

/**
 * Default implementation of the Printer interface that provides syntax highlighting and formatting.
 * <p>
 * DefaultPrinter provides functionality for printing various types of objects to the console
 * with syntax highlighting and formatting. It supports printing:
 * <ul>
 *   <li>Simple objects (strings, numbers, etc.)</li>
 *   <li>Collections and maps</li>
 *   <li>Tables with row highlighting</li>
 *   <li>JSON and other structured data</li>
 *   <li>Source code with syntax highlighting</li>
 * </ul>
 * <p>
 * The printer can be configured with various options to control the formatting and highlighting
 * of the output, such as maximum number of rows, indentation, and highlighting styles.
 *
 */
public class DefaultPrinter extends JlineCommandRegistry implements Printer {
    protected static final String VAR_PRNT_OPTIONS = "PRNT_OPTIONS";
    protected static final int PRNT_MAX_ROWS = 100000;
    protected static final int PRNT_MAX_DEPTH = 1;
    protected static final int PRNT_INDENTION = 4;
    private static final int NANORC_MAX_STRING_LENGTH = 400;
    private static final int HIGHLIGHTER_CACHE_SIZE = 5;

    private Map<Class<?>, Function<Object, Map<String, Object>>> objectToMap = new HashMap<>();
    private Map<Class<?>, Function<Object, String>> objectToString = new HashMap<>();
    private Map<String, Function<Object, AttributedString>> highlightValue = new HashMap<>();
    private int totLines;

    private final ScriptEngine engine;
    private final ConfigurationPath configPath;
    private StyleResolver prntStyle;

    @SuppressWarnings("serial")
    private final LinkedHashMap<String, SyntaxHighlighter> highlighters =
            new LinkedHashMap<String, SyntaxHighlighter>(HIGHLIGHTER_CACHE_SIZE + 1, .75F, false) {
                protected boolean removeEldestEntry(Map.Entry<String, SyntaxHighlighter> eldest) {
                    return size() > HIGHLIGHTER_CACHE_SIZE;
                }
            };

    public DefaultPrinter(ConfigurationPath configPath) {
        this(null, configPath);
    }

    public DefaultPrinter(ScriptEngine engine, ConfigurationPath configPath) {
        this.engine = engine;
        this.configPath = configPath;
    }

    @Override
    public void println(Object object) {
        internalPrintln(defaultPrntOptions(false), object);
    }

    @Override
    public void println(Map<String, Object> optionsIn, Object object) {
        Map<String, Object> options = new HashMap<>(optionsIn);
        for (Map.Entry<String, Object> entry : defaultPrntOptions(options.containsKey(Printer.SKIP_DEFAULT_OPTIONS))
                .entrySet()) {
            options.putIfAbsent(entry.getKey(), entry.getValue());
        }
        manageBooleanOptions(options);
        internalPrintln(options, object);
    }

    @Override
    public boolean refresh() {
        highlighters.clear();
        return true;
    }

    public String[] appendUsage(String[] customUsage) {
        final String[] usage = {
            "prnt -  print object",
            "Usage: prnt [OPTIONS] object",
            "  -? --help                       Displays command help",
            "  -a --all                        Ignore columnsOut configuration",
            "  -b --border=CHAR                Table cell vertical border character",
            "  -c --columns=COLUMNS,...        Display given columns on map/table",
            "  -e --exclude=COLUMNS,...        Exclude given columns on table",
            "  -i --include=COLUMNS,...        Include given columns on table",
            "     --indention=INDENTION        Indention size",
            "     --maxColumnWidth=WIDTH       Maximum column width",
            "  -d --maxDepth=DEPTH             Maximum depth objects are resolved",
            "  -n --maxrows=ROWS               Maximum number of lines to display",
            "  -m --multiColumns               Display the collection of simple data in multiple columns",
            "     --oneRowTable                Display one row data on table",
            "  -h --rowHighlight=ROW           Highlight table rows. ROW = EVEN, ODD, ALL",
            "  -r --rownum                     Display table row numbers",
            "     --shortNames                 Truncate table column names (property.field -> field)",
            "     --skipDefaultOptions         Ignore all options defined in PRNT_OPTIONS",
            "     --structsOnTable             Display structs and lists on table",
            "  -s --style=STYLE                Use nanorc STYLE to highlight Object.",
            "                                  STYLE = JSON serialize object to JSON string before printing",
            "     --toString                   Use object's toString() method to get print value",
            "                                  DEFAULT: object's fields are put to property map before printing",
            "     --valueStyle=STYLE           Use nanorc style to highlight string and column/map values",
            "  -w --width=WIDTH                Display width (default terminal width)"
        };
        String[] out;
        if (customUsage == null || customUsage.length == 0) {
            out = usage;
        } else {
            out = new String[usage.length + customUsage.length];
            System.arraycopy(usage, 0, out, 0, usage.length);
            System.arraycopy(customUsage, 0, out, usage.length, customUsage.length);
        }
        return out;
    }

    public Map<String, Object> compileOptions(Options opt) {
        Map<String, Object> options = new HashMap<>();
        if (opt.isSet(Printer.SKIP_DEFAULT_OPTIONS)) {
            options.put(Printer.SKIP_DEFAULT_OPTIONS, true);
        } else if (opt.isSet(Printer.STYLE)) {
            options.put(Printer.STYLE, opt.get(Printer.STYLE));
        }
        if (opt.isSet(Printer.TO_STRING)) {
            options.put(Printer.TO_STRING, true);
        }
        if (opt.isSet(Printer.WIDTH)) {
            options.put(Printer.WIDTH, opt.getNumber(Printer.WIDTH));
        }
        if (opt.isSet(Printer.ROWNUM)) {
            options.put(Printer.ROWNUM, true);
        }
        if (opt.isSet(Printer.ONE_ROW_TABLE)) {
            options.put(Printer.ONE_ROW_TABLE, true);
        }
        if (opt.isSet(Printer.SHORT_NAMES)) {
            options.put(Printer.SHORT_NAMES, true);
        }
        if (opt.isSet(Printer.STRUCT_ON_TABLE)) {
            options.put(Printer.STRUCT_ON_TABLE, true);
        }
        if (opt.isSet(Printer.COLUMNS)) {
            options.put(Printer.COLUMNS, Arrays.asList(opt.get(Printer.COLUMNS).split(",")));
        }
        if (opt.isSet(Printer.EXCLUDE)) {
            options.put(Printer.EXCLUDE, Arrays.asList(opt.get(Printer.EXCLUDE).split(",")));
        }
        if (opt.isSet(Printer.INCLUDE)) {
            options.put(Printer.INCLUDE, Arrays.asList(opt.get(Printer.INCLUDE).split(",")));
        }
        if (opt.isSet(Printer.ALL)) {
            options.put(Printer.ALL, true);
        }
        if (opt.isSet(Printer.MAXROWS)) {
            options.put(Printer.MAXROWS, opt.getNumber(Printer.MAXROWS));
        }
        if (opt.isSet(Printer.MAX_COLUMN_WIDTH)) {
            options.put(Printer.MAX_COLUMN_WIDTH, opt.getNumber(Printer.MAX_COLUMN_WIDTH));
        }
        if (opt.isSet(Printer.MAX_DEPTH)) {
            options.put(Printer.MAX_DEPTH, opt.getNumber(Printer.MAX_DEPTH));
        }
        if (opt.isSet(Printer.INDENTION)) {
            options.put(Printer.INDENTION, opt.getNumber(Printer.INDENTION));
        }
        if (opt.isSet(Printer.VALUE_STYLE)) {
            options.put(Printer.VALUE_STYLE, opt.get(Printer.VALUE_STYLE));
        }
        if (opt.isSet(Printer.BORDER)) {
            options.put(Printer.BORDER, opt.get(Printer.BORDER));
        }
        if (opt.isSet(Printer.ROW_HIGHLIGHT)) {
            try {
                options.put(Printer.ROW_HIGHLIGHT, optionRowHighlight(opt.get(Printer.ROW_HIGHLIGHT)));
            } catch (Exception e) {
                RuntimeException exception = new BadOptionValueException(
                        Printer.ROW_HIGHLIGHT + " has a bad value: " + opt.get(Printer.ROW_HIGHLIGHT));
                exception.addSuppressed(e);
                throw exception;
            }
        }
        if (opt.isSet(Printer.MULTI_COLUMNS)) {
            options.put(Printer.MULTI_COLUMNS, true);
        }
        options.put("exception", "stack");
        return options;
    }

    private TableRows optionRowHighlight(Object value) {
        if (value instanceof TableRows || value == null) {
            return (TableRows) value;
        } else if (value instanceof String) {
            String val = ((String) value).trim().toUpperCase();
            if (!val.isEmpty() && !val.equals("NULL")) {
                return TableRows.valueOf(val);
            } else {
                return null;
            }
        }
        throw new IllegalArgumentException("rowHighlight has a bad option value type: " + value.getClass());
    }

    @Override
    public Exception prntCommand(CommandInput input) {
        Exception out = null;
        String[] usage = appendUsage(null);
        try {
            Options opt = parseOptions(usage, input.xargs());
            Map<String, Object> options = compileOptions(opt);
            List<Object> args = opt.argObjects();
            if (!args.isEmpty()) {
                println(options, args.get(0));
            }
        } catch (Exception e) {
            out = e;
        }
        return out;
    }

    /**
     * Override ScriptEngine toMap() method
     * @param objectToMap key: object class, value: toMap function
     */
    public void setObjectToMap(Map<Class<?>, Function<Object, Map<String, Object>>> objectToMap) {
        this.objectToMap = objectToMap;
    }

    /**
     * Override ScriptEngine toString() method
     * @param objectToString key: object class, value: toString function
     */
    public void setObjectToString(Map<Class<?>, Function<Object, String>> objectToString) {
        this.objectToString = objectToString;
    }

    /**
     * Highlight column value
     * @param highlightValue key: regex for column name, value: highlight function
     */
    public void setHighlightValue(Map<String, Function<Object, AttributedString>> highlightValue) {
        this.highlightValue = highlightValue;
    }

    /**
     *
     * @return terminal to which will be printed
     */
    protected Terminal terminal() {
        return SystemRegistry.get().terminal();
    }

    /**
     * Boolean printing options Printer checks only if key is present.
     * Boolean options that have false value are removed from the options Map.
     * @param options printing options
     */
    protected void manageBooleanOptions(Map<String, Object> options) {
        for (String key : Printer.BOOLEAN_KEYS) {
            Object option = options.get(key);
            boolean value = option instanceof Boolean && (boolean) option;
            if (!value) {
                options.remove(key);
            }
        }
    }

    /**
     * Set default and mandatory printing options.
     * Also unsupported options will be removed when Printer is used without scriptEngine
     * @param skipDefault when true does not set default options
     * @return default, mandatory and supported options
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> defaultPrntOptions(boolean skipDefault) {
        Map<String, Object> out = new HashMap<>();
        if (engine != null && !skipDefault && engine.hasVariable(VAR_PRNT_OPTIONS)) {
            out.putAll((Map<String, Object>) engine.get(VAR_PRNT_OPTIONS));
            out.remove(Printer.SKIP_DEFAULT_OPTIONS);
            manageBooleanOptions(out);
        }
        out.putIfAbsent(Printer.MAXROWS, PRNT_MAX_ROWS);
        out.putIfAbsent(Printer.MAX_DEPTH, PRNT_MAX_DEPTH);
        out.putIfAbsent(Printer.INDENTION, PRNT_INDENTION);
        out.putIfAbsent(Printer.COLUMNS_OUT, new ArrayList<String>());
        out.putIfAbsent(Printer.COLUMNS_IN, new ArrayList<String>());
        if (engine == null) {
            out.remove(Printer.OBJECT_TO_MAP);
            out.remove(Printer.OBJECT_TO_STRING);
            out.remove(Printer.HIGHLIGHT_VALUE);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private void internalPrintln(Map<String, Object> options, Object object) {
        if (object == null) {
            return;
        }
        long start = new Date().getTime();
        if (options.containsKey(Printer.EXCLUDE)) {
            List<String> colOut = optionList(Printer.EXCLUDE, options);
            List<String> colIn = optionList(Printer.COLUMNS_IN, options);
            colIn.removeAll(colOut);
            colOut.addAll((List<String>) options.get(Printer.COLUMNS_OUT));
            options.put(Printer.COLUMNS_IN, colIn);
            options.put(Printer.COLUMNS_OUT, colOut);
        }
        if (options.containsKey(Printer.INCLUDE)) {
            List<String> colIn = optionList(Printer.INCLUDE, options);
            colIn.addAll((List<String>) options.get(Printer.COLUMNS_IN));
            options.put(Printer.COLUMNS_IN, colIn);
        }
        options.put(Printer.VALUE_STYLE, valueHighlighter((String) options.getOrDefault(Printer.VALUE_STYLE, null)));
        prntStyle = Styles.prntStyle();
        options.putIfAbsent(Printer.WIDTH, terminal().getSize().getColumns());
        String style = (String) options.getOrDefault(Printer.STYLE, "");
        options.put(Printer.STYLE, valueHighlighter(style));
        int width = (int) options.get(Printer.WIDTH);
        int maxrows = (int) options.get(Printer.MAXROWS);
        if (!style.isEmpty() && object instanceof String) {
            highlightAndPrint(width, (SyntaxHighlighter) options.get(Printer.STYLE), (String) object, true, maxrows);
        } else if (style.equalsIgnoreCase("JSON")) {
            if (engine == null) {
                throw new IllegalArgumentException("JSON style not supported!");
            }
            String json = engine.toJson(object);
            highlightAndPrint(width, (SyntaxHighlighter) options.get(Printer.STYLE), json, true, maxrows);
        } else if (options.containsKey(Printer.SKIP_DEFAULT_OPTIONS)) {
            highlightAndPrint(options, object);
        } else if (object instanceof Exception) {
            highlightAndPrint(options, (Exception) object);
        } else if (object instanceof CmdDesc) {
            highlight((CmdDesc) object).println(terminal());
        } else if (object instanceof String || object instanceof Number) {
            String str = object.toString();
            SyntaxHighlighter highlighter = (SyntaxHighlighter) options.getOrDefault(Printer.VALUE_STYLE, null);
            highlightAndPrint(width, highlighter, str, doValueHighlight(options, str), maxrows);
        } else {
            highlightAndPrint(options, object);
        }
        terminal().flush();
        Log.debug("println: ", new Date().getTime() - start, " msec");
    }

    /**
     * Highlight and print an exception
     * @param options Printing options
     * @param exception Exception to be printed
     */
    protected void highlightAndPrint(Map<String, Object> options, Throwable exception) {
        SystemRegistry.get().trace(options.getOrDefault("exception", "stack").equals("stack"), exception);
    }

    private AttributedString highlight(CmdDesc cmdDesc) {
        StringBuilder sb = new StringBuilder();
        for (AttributedString as : cmdDesc.getMainDesc()) {
            sb.append(as.toString());
            sb.append("\n");
        }
        List<Integer> tabs = Arrays.asList(0, 2, 33);
        for (Map.Entry<String, List<AttributedString>> entry :
                cmdDesc.getOptsDesc().entrySet()) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.tabs(tabs);
            asb.append("\t");
            asb.append(entry.getKey());
            asb.append("\t");
            boolean first = true;
            for (AttributedString as : entry.getValue()) {
                if (!first) {
                    asb.append("\t");
                    asb.append("\t");
                }
                asb.append(as);
                asb.append("\n");
                first = false;
            }
            sb.append(asb);
        }
        return Options.HelpException.highlight(sb.toString(), Styles.helpStyle());
    }

    private SyntaxHighlighter valueHighlighter(String style) {
        SyntaxHighlighter out;
        if (style == null || style.isEmpty()) {
            out = null;
        } else if (highlighters.containsKey(style)) {
            out = highlighters.get(style);
        } else if (style.matches("[a-z]+:.*")) {
            out = SyntaxHighlighter.build(style);
            highlighters.put(style, out);
        } else {
            Path nanorc = configPath != null ? configPath.getConfig(DEFAULT_NANORC_FILE) : null;
            if (engine != null && engine.hasVariable(VAR_NANORC)) {
                nanorc = Paths.get((String) engine.get(VAR_NANORC));
            }
            if (nanorc == null) {
                nanorc = Paths.get("/etc/nanorc");
            }
            out = SyntaxHighlighter.build(nanorc, style);
            highlighters.put(style, out);
        }
        return out;
    }

    private String truncate4nanorc(String obj) {
        String val = obj;
        if (val.length() > NANORC_MAX_STRING_LENGTH && !val.contains("\n")) {
            val = val.substring(0, NANORC_MAX_STRING_LENGTH - 1);
        }
        return val;
    }

    private AttributedString highlight(
            Integer width, SyntaxHighlighter highlighter, String object, boolean doValueHighlight) {
        AttributedString out;
        AttributedStringBuilder asb = new AttributedStringBuilder();
        String val = object;
        if (highlighter != null && doValueHighlight) {
            val = truncate4nanorc(object);
        }
        asb.append(val);
        if (highlighter != null && val.length() < NANORC_MAX_STRING_LENGTH && doValueHighlight) {
            out = highlighter.highlight(asb);
        } else {
            out = asb.toAttributedString();
        }
        if (width != null) {
            out = out.columnSubSequence(0, width);
        }
        return out;
    }

    private boolean doValueHighlight(Map<String, Object> options, String value) {
        if (options.containsKey(Printer.VALUE_STYLE_ALL)
                || value.matches("\"(\\.|[^\"])*\"|'(\\.|[^'])*'")
                || (value.startsWith("[") && value.endsWith("]"))
                || (value.startsWith("(") && value.endsWith(")"))
                || (value.startsWith("{") && value.endsWith("}"))
                || (value.startsWith("<") && value.endsWith(">"))) {
            return true;
        } else {
            return !value.contains(" ") && !value.contains("\t");
        }
    }

    private void highlightAndPrint(
            int width, SyntaxHighlighter highlighter, String object, boolean doValueHighlight, int maxRows) {
        String lineBreak = null;
        if (object.indexOf("\r\n") >= 0) {
            lineBreak = "\r\n";
        } else if (object.indexOf("\n") >= 0) {
            lineBreak = "\n";
        } else if (object.indexOf("\r") >= 0) {
            lineBreak = "\r";
        }
        if (lineBreak == null) {
            highlightAndPrint(width, highlighter, object, doValueHighlight);
        } else {
            int rows = 0;
            int i0 = 0;
            while (rows < maxRows) {
                rows++;
                int i1 = object.indexOf(lineBreak, i0);
                String line = i1 >= 0 ? object.substring(i0, i1) : object.substring(i0);
                highlightAndPrint(width, highlighter, line, doValueHighlight);
                if (i1 < 0) {
                    break;
                }
                i0 = i1 + lineBreak.length();
            }
            if (rows == maxRows) {
                throw new TruncatedOutputException("Truncated output: " + maxRows);
            }
        }
    }

    private void highlightAndPrint(int width, SyntaxHighlighter highlighter, String object, boolean doValueHighlight) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        List<AttributedString> sas = asb.append(object).columnSplitLength(width);
        for (AttributedString as : sas) {
            highlight(width, highlighter, as.toString(), doValueHighlight).println(terminal());
        }
    }

    private Map<String, Object> keysToString(Map<Object, Object> map) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (entry.getKey() instanceof String) {
                out.put((String) entry.getKey(), entry.getValue());
            } else if (entry.getKey() != null) {
                out.put(entry.getKey().toString(), entry.getValue());
            } else {
                out.put("null", entry.getValue());
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Object mapValue(Map<String, Object> options, String key, Map<String, Object> map) {
        Object out = null;
        if (map.containsKey(key)) {
            out = map.get(key);
        } else if (key.contains(".")) {
            String[] keys = key.split("\\.");
            out = map.get(keys[0]);
            for (int i = 1; i < keys.length; i++) {
                if (out instanceof Map) {
                    Map<String, Object> m = keysToString((Map<Object, Object>) out);
                    out = m.get(keys[i]);
                } else if (canConvert(out)) {
                    out = engine.toMap(out).get(keys[i]);
                } else {
                    break;
                }
            }
        }
        if (!(out instanceof Map) && canConvert(out)) {
            out = objectToMap(options, out);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<String> optionList(String key, Map<String, Object> options) {
        List<String> out = new ArrayList<>();
        Object option = options.get(key);
        if (option instanceof String) {
            out.addAll(Arrays.asList(((String) option).split(",")));
        } else if (option instanceof Collection) {
            out.addAll((Collection<String>) option);
        } else if (option != null) {
            throw new IllegalArgumentException(
                    "Unsupported option list: {key: " + key + ", type: " + option.getClass() + "}");
        }
        return out;
    }

    private boolean hasMatch(List<String> regexes, String value) {
        for (String r : regexes) {
            if (value.matches(r)) {
                return true;
            }
        }
        return false;
    }

    private AttributedString addPadding(AttributedString str, int width) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (int i = str.columnLength(); i < width; i++) {
            sb.append(" ");
        }
        sb.append(str);
        return sb.toAttributedString();
    }

    private String addPadding(String str, int width) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (int i = str.length(); i < width; i++) {
            sb.append(" ");
        }
        sb.append(str);
        return sb.toString();
    }

    private String columnValue(String value) {
        return value.replaceAll("\r", "CR").replaceAll("\n", "LF");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectToMap(Map<String, Object> options, Object obj) {
        if (obj != null) {
            Map<Class<?>, Object> toMap =
                    (Map<Class<?>, Object>) options.getOrDefault(Printer.OBJECT_TO_MAP, Collections.emptyMap());
            if (toMap.containsKey(obj.getClass())) {
                return (Map<String, Object>) engine.execute(toMap.get(obj.getClass()), obj);
            } else if (objectToMap.containsKey(obj.getClass())) {
                return objectToMap.get(obj.getClass()).apply(obj);
            }
        }
        return engine.toMap(obj);
    }

    @SuppressWarnings("unchecked")
    private String objectToString(Map<String, Object> options, Object obj) {
        String out = "null";
        if (obj != null) {
            Map<Class<?>, Object> toString = options.containsKey(Printer.OBJECT_TO_STRING)
                    ? (Map<Class<?>, Object>) options.get(Printer.OBJECT_TO_STRING)
                    : new HashMap<>();
            if (toString.containsKey(obj.getClass())) {
                out = (String) engine.execute(toString.get(obj.getClass()), obj);
            } else if (objectToString.containsKey(obj.getClass())) {
                out = objectToString.get(obj.getClass()).apply(obj);
            } else if (obj instanceof Class) {
                out = ((Class<?>) obj).getName();
            } else if (engine != null) {
                out = engine.toString(obj);
            } else {
                out = obj.toString();
            }
        }
        return out;
    }

    private AttributedString highlightMapValue(Map<String, Object> options, String key, Map<String, Object> map) {
        return highlightValue(options, key, mapValue(options, key, map));
    }

    private boolean isHighlighted(AttributedString value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.styleAt(i).getStyle() != AttributedStyle.DEFAULT.getStyle()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private AttributedString highlightValue(Map<String, Object> options, String column, Object obj) {
        AttributedString out = null;
        Object raw = options.containsKey(Printer.TO_STRING) && obj != null ? objectToString(options, obj) : obj;
        Map<String, Object> hv = options.containsKey(Printer.HIGHLIGHT_VALUE)
                ? (Map<String, Object>) options.get(Printer.HIGHLIGHT_VALUE)
                : new HashMap<>();
        if (column != null && simpleObject(raw)) {
            for (Map.Entry<String, Object> entry : hv.entrySet()) {
                if (!entry.getKey().equals("*") && column.matches(entry.getKey())) {
                    out = (AttributedString) engine.execute(hv.get(entry.getKey()), raw);
                    break;
                }
            }
            if (out == null) {
                for (Map.Entry<String, Function<Object, AttributedString>> entry : highlightValue.entrySet()) {
                    if (!entry.getKey().equals("*") && column.matches(entry.getKey())) {
                        out = highlightValue.get(entry.getKey()).apply(raw);
                        break;
                    }
                }
            }
        }
        if (out == null) {
            if (raw instanceof String) {
                out = new AttributedString(columnValue((String) raw));
            } else {
                out = new AttributedString(columnValue(objectToString(options, raw)));
            }
        }
        if ((simpleObject(raw) || raw == null)
                && (hv.containsKey("*") || highlightValue.containsKey("*"))
                && !isHighlighted(out)) {
            if (hv.containsKey("*")) {
                out = (AttributedString) engine.execute(hv.get("*"), out);
            }
            Function<Object, AttributedString> func = highlightValue.get("*");
            if (func != null) {
                out = func.apply(out);
            }
        }
        if (options.containsKey(Printer.VALUE_STYLE) && !isHighlighted(out)) {
            out = highlight(
                    null,
                    (SyntaxHighlighter) options.get(Printer.VALUE_STYLE),
                    out.toString(),
                    doValueHighlight(options, out.toString()));
        }
        return truncateValue(options, out);
    }

    private AttributedString truncateValue(Map<String, Object> options, AttributedString value) {
        if (value.columnLength() > (int) options.getOrDefault(Printer.MAX_COLUMN_WIDTH, Integer.MAX_VALUE)) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.append(value.columnSubSequence(0, (int) options.get(Printer.MAX_COLUMN_WIDTH) - 3));
            asb.append("...");
            return asb.toAttributedString();
        }
        return value;
    }

    private String truncateValue(int maxWidth, String value) {
        if (value.length() > maxWidth) {
            return value.subSequence(0, maxWidth - 3) + "...";
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> objectToList(Object obj) {
        List<Object> out = new ArrayList<>();
        if (obj instanceof List) {
            out = (List<Object>) obj;
        } else if (obj instanceof Collection) {
            out.addAll((Collection<Object>) obj);
        } else if (obj instanceof Object[]) {
            out.addAll(Arrays.asList((Object[]) obj));
        } else if (obj instanceof Iterator) {
            ((Iterator<?>) obj).forEachRemaining(out::add);
        } else if (obj instanceof Iterable) {
            ((Iterable<?>) obj).forEach(out::add);
        } else {
            out.add(obj);
        }
        return out;
    }

    private boolean similarSets(final List<String> ref, final Set<String> c2, final int matchLimit) {
        boolean out = false;
        int limit = matchLimit;
        for (String s : ref) {
            if (c2.contains(s)) {
                limit--;
                if (limit == 0) {
                    out = true;
                    break;
                }
            }
        }
        return out;
    }

    @SuppressWarnings("serial")
    private static class BadOptionValueException extends RuntimeException {
        public BadOptionValueException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    private static class TruncatedOutputException extends RuntimeException {
        public TruncatedOutputException(String message) {
            super(message);
        }
    }

    private void println(AttributedString line, int maxrows) {
        line.println(terminal());
        totLines++;
        if (totLines > maxrows) {
            totLines = 0;
            throw new TruncatedOutputException("Truncated output: " + maxrows);
        }
    }

    private String columnName(String name, boolean shortName) {
        String out = name;
        if (shortName) {
            String[] p = name.split("\\.");
            out = p[p.length - 1];
        }
        return out;
    }

    private boolean isNumber(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    @SuppressWarnings("unchecked")
    private void highlightAndPrint(Map<String, Object> options, Object obj) {
        int width = (int) options.get(Printer.WIDTH);
        int maxrows = (int) options.get(Printer.MAXROWS);
        totLines = 0;
        String message = null;
        RuntimeException runtimeException = null;
        if (obj == null) {
            // do nothing
        } else if (obj instanceof Map) {
            highlightMap(options, keysToString((Map<Object, Object>) obj), width);
        } else if (collectionObject(obj)) {
            List<Object> collection = objectToList(obj);
            if (collection.size() > maxrows) {
                message = "Truncated output: " + maxrows + "/" + collection.size();
                collection = collection.subList(collection.size() - maxrows, collection.size());
            }
            if (!collection.isEmpty()) {
                if (collection.size() == 1 && !options.containsKey(Printer.ONE_ROW_TABLE)) {
                    Object elem = collection.iterator().next();
                    if (elem instanceof Map) {
                        highlightMap(options, keysToString((Map<Object, Object>) elem), width);
                    } else if (canConvert(elem) && !options.containsKey(Printer.TO_STRING)) {
                        highlightMap(options, objectToMap(options, elem), width);
                    } else if (elem instanceof String && options.get(Printer.STYLE) != null) {
                        highlightAndPrint(
                                width, (SyntaxHighlighter) options.get(Printer.STYLE), (String) elem, true, maxrows);
                    } else {
                        highlightValue(options, null, objectToString(options, obj))
                                .println(terminal());
                    }
                } else {
                    String columnSep = "";
                    TableRows tableRows = null;
                    boolean rownum = options.containsKey(Printer.ROWNUM);
                    try {
                        columnSep = (String) options.getOrDefault(Printer.BORDER, "");
                        tableRows = optionRowHighlight(options.getOrDefault(Printer.ROW_HIGHLIGHT, null));
                    } catch (Exception e) {
                        runtimeException = new BadOptionValueException(
                                "Option " + Printer.BORDER + " or " + Printer.ROW_HIGHLIGHT + " has a bad value!");
                        runtimeException.addSuppressed(e);
                    }
                    try {
                        Object elem = collection.iterator().next();
                        boolean convert = canConvert(elem);
                        if ((elem instanceof Map || convert) && !options.containsKey(Printer.TO_STRING)) {
                            List<Map<String, Object>> convertedCollection = new ArrayList<>();
                            Set<String> keys = new HashSet<>();
                            for (Object o : collection) {
                                Map<String, Object> m =
                                        convert ? objectToMap(options, o) : keysToString((Map<Object, Object>) o);
                                convertedCollection.add(m);
                                keys.addAll(m.keySet());
                            }
                            List<String> _header;
                            List<String> columnsIn = optionList(Printer.COLUMNS_IN, options);
                            List<String> columnsOut = !options.containsKey("all")
                                    ? optionList(Printer.COLUMNS_OUT, options)
                                    : new ArrayList<>();
                            if (options.containsKey(Printer.COLUMNS)) {
                                _header = (List<String>) options.get(Printer.COLUMNS);
                            } else {
                                _header = columnsIn;
                                _header.addAll(keys.stream()
                                        .filter(k -> !columnsIn.contains(k) && !hasMatch(columnsOut, k))
                                        .collect(Collectors.toList()));
                            }
                            List<String> header = new ArrayList<>();
                            List<Integer> columns = new ArrayList<>();
                            int headerWidth = 0;
                            List<String> refKeys = new ArrayList<>();
                            for (String v : _header) {
                                String value = v.split("\\.")[0];
                                if (!keys.contains(value) && !keys.contains(v)) {
                                    continue;
                                }
                                boolean addKey = false;
                                for (Map<String, Object> m : convertedCollection) {
                                    Object val = mapValue(options, v, m);
                                    if (val != null) {
                                        addKey = simpleObject(val)
                                                || options.containsKey(Printer.COLUMNS)
                                                || options.containsKey(Printer.STRUCT_ON_TABLE);
                                        break;
                                    }
                                }
                                if (!addKey) {
                                    continue;
                                }
                                refKeys.add(value);
                                header.add(v);
                                String cn = columnName(v, options.containsKey(Printer.SHORT_NAMES));
                                columns.add(cn.length() + 1);
                                headerWidth += cn.length() + 1;
                                if (headerWidth > width) {
                                    break;
                                }
                            }
                            if (header.size() == 0) {
                                throw new Exception("No columns for table!");
                            }
                            double mapSimilarity = ((BigDecimal)
                                            options.getOrDefault(Printer.MAP_SIMILARITY, new BigDecimal("0.8")))
                                    .doubleValue();
                            int matchLimit = (int) Math.ceil(header.size() * mapSimilarity);
                            for (Map<String, Object> m : convertedCollection) {
                                if (!similarSets(refKeys, m.keySet(), matchLimit)) {
                                    throw new Exception("Not homogenous list!");
                                }
                                for (int i = 0; i < header.size(); i++) {
                                    int cw = highlightMapValue(options, header.get(i), m)
                                            .columnLength();
                                    if (cw > columns.get(i) - 1) {
                                        columns.set(i, cw + 1);
                                    }
                                }
                            }
                            toTabStops(columns, collection.size(), rownum, columnSep);
                            AttributedStringBuilder asb = new AttributedStringBuilder().tabs(columns);
                            asb.style(prntStyle.resolve(".th"));
                            int firstColumn = 0;
                            if (rownum) {
                                asb.append(addPadding("", columns.get(0) - columnSep.length() - 1));
                                asb.append(columnSep);
                                asb.append("\t");
                                firstColumn = 1;
                            }
                            boolean first = true;
                            for (String s : header) {
                                if (!first) {
                                    asb.append(columnSep);
                                }
                                asb.append(columnName(s, options.containsKey(Printer.SHORT_NAMES)));
                                asb.append("\t");
                                first = false;
                            }
                            asb.columnSubSequence(0, width).println(terminal());
                            int row = 0;
                            for (Map<String, Object> m : convertedCollection) {
                                AttributedStringBuilder asb2 = new AttributedStringBuilder().tabs(columns);
                                if (doRowHighlight(row, tableRows)) {
                                    asb2.style(prntStyle.resolve(".rs"));
                                }
                                if (rownum) {
                                    asb2.styled(
                                            prntStyle.resolve(".rn"),
                                            addPadding(Integer.toString(row), columns.get(0) - columnSep.length() - 1));
                                    asb2.append(columnSep);
                                    asb2.append("\t");
                                }
                                row++;
                                for (int i = 0; i < header.size(); i++) {
                                    if (i > 0) {
                                        asb2.append(columnSep);
                                    }
                                    AttributedString v = highlightMapValue(options, header.get(i), m);
                                    if (isNumber(v.toString())) {
                                        v = addPadding(v, cellWidth(firstColumn + i, columns, rownum, columnSep) - 1);
                                    }
                                    asb2.append(v);
                                    asb2.append("\t");
                                }
                                asb2.columnSubSequence(0, width).println(terminal());
                            }
                        } else if (collectionObject(elem) && !options.containsKey(Printer.TO_STRING)) {
                            List<Integer> columns = new ArrayList<>();
                            for (Object o : collection) {
                                List<Object> inner = objectToList(o);
                                for (int i = 0; i < inner.size(); i++) {
                                    int len1 = objectToString(options, inner.get(i))
                                                    .length()
                                            + 1;
                                    if (columns.size() <= i) {
                                        columns.add(len1);
                                    } else if (len1 > columns.get(i)) {
                                        columns.set(i, len1);
                                    }
                                }
                            }
                            toTabStops(columns, collection.size(), rownum, columnSep);
                            int row = 0;
                            int firstColumn = rownum ? 1 : 0;
                            for (Object o : collection) {
                                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(columns);
                                if (doRowHighlight(row, tableRows)) {
                                    asb.style(prntStyle.resolve(".rs"));
                                }
                                if (rownum) {
                                    asb.styled(
                                            prntStyle.resolve(".rn"),
                                            addPadding(Integer.toString(row), columns.get(0) - columnSep.length() - 1));
                                    asb.append(columnSep);
                                    asb.append("\t");
                                }
                                row++;
                                List<Object> inner = objectToList(o);
                                for (int i = 0; i < inner.size(); i++) {
                                    if (i > 0) {
                                        asb.append(columnSep);
                                    }
                                    AttributedString v = highlightValue(options, null, inner.get(i));
                                    if (isNumber(v.toString())) {
                                        v = addPadding(v, cellWidth(firstColumn + i, columns, rownum, columnSep) - 1);
                                    }
                                    asb.append(v);
                                    asb.append("\t");
                                }
                                asb.columnSubSequence(0, width).println(terminal());
                            }
                        } else {
                            highlightList(options, collection, width);
                        }
                    } catch (Exception e) {
                        Log.debug("Stack: ", e);
                        highlightList(options, collection, width);
                    }
                }
            } else {
                highlightValue(options, null, objectToString(options, obj)).println(terminal());
            }
        } else if (canConvert(obj) && !options.containsKey(Printer.TO_STRING)) {
            highlightMap(options, objectToMap(options, obj), width);
        } else {
            highlightValue(options, null, objectToString(options, obj)).println(terminal());
        }
        if (message != null) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.styled(prntStyle.resolve(".em"), message);
            asb.println(terminal());
        }
        if (runtimeException != null) {
            throw runtimeException;
        }
    }

    private boolean doRowHighlight(int row, TableRows tableRows) {
        if (tableRows == null) {
            return false;
        }
        switch (tableRows) {
            case EVEN:
                return row % 2 == 0;
            case ODD:
                return row % 2 == 1;
            case ALL:
                return true;
        }
        return false;
    }

    private void highlightList(Map<String, Object> options, List<Object> collection, int width) {
        highlightList(options, collection, width, 0);
    }

    private void highlightList(Map<String, Object> options, List<Object> collection, int width, int depth) {
        int row = 0;
        int maxrows = (int) options.get(Printer.MAXROWS);
        int indent = (int) options.get(Printer.INDENTION);
        List<Integer> tabs = new ArrayList<>();
        SyntaxHighlighter highlighter = depth == 0 ? (SyntaxHighlighter) options.get(Printer.STYLE) : null;
        if (!(boolean) options.getOrDefault(Printer.MULTI_COLUMNS, false)) {
            tabs.add(indent * depth);
            if (options.containsKey(Printer.ROWNUM)) {
                tabs.add(indent * depth + digits(collection.size()) + 2);
            }
            options.remove(Printer.MAX_COLUMN_WIDTH);
            for (Object o : collection) {
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                if (depth > 0) {
                    asb.append("\t");
                }
                if (options.containsKey(Printer.ROWNUM)) {
                    asb.styled(prntStyle.resolve(".rn"), Integer.toString(row)).append(":");
                    asb.append("\t");
                    row++;
                }
                if (highlighter != null && o instanceof String) {
                    asb.append(highlighter.highlight((String) o));
                } else {
                    asb.append(highlightValue(options, null, o));
                }
                println(asb.columnSubSequence(0, width), maxrows);
            }
        } else {
            int maxWidth = 0;
            for (Object o : collection) {
                AttributedString as;
                if (highlighter != null && o instanceof String) {
                    as = highlighter.highlight((String) o);
                } else {
                    as = highlightValue(options, null, o);
                }
                if (as.length() > maxWidth) {
                    maxWidth = as.length();
                }
            }
            int mcw = (int) options.getOrDefault(Printer.MAX_COLUMN_WIDTH, Integer.MAX_VALUE);
            maxWidth = mcw < maxWidth ? mcw : maxWidth;
            tabs.add(maxWidth + 1);
            AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
            for (Object o : collection) {
                if (asb.length() + maxWidth > width) {
                    println(asb.columnSubSequence(0, width), maxrows);
                    asb = new AttributedStringBuilder().tabs(tabs);
                }
                if (highlighter != null && o instanceof String) {
                    asb.append(highlighter.highlight((String) o));
                } else {
                    asb.append(highlightValue(options, null, o));
                }
                asb.append("\t");
            }
            println(asb.columnSubSequence(0, width), maxrows);
        }
    }

    private boolean collectionObject(Object obj) {
        return obj instanceof Iterator || obj instanceof Iterable || obj instanceof Object[];
    }

    private boolean simpleObject(Object obj) {
        return obj instanceof Number
                || obj instanceof String
                || obj instanceof Date
                || obj instanceof File
                || obj instanceof Boolean
                || obj instanceof Enum;
    }

    private boolean canConvert(Object obj) {
        return engine != null
                && obj != null
                && !(obj instanceof Class)
                && !(obj instanceof Map)
                && !simpleObject(obj)
                && !collectionObject(obj);
    }

    private int digits(int number) {
        if (number < 100) {
            return number < 10 ? 1 : 2;
        } else if (number < 1000) {
            return 3;
        } else {
            return number < 10000 ? 4 : 5;
        }
    }

    private int cellWidth(int pos, List<Integer> columns, boolean rownum, String columnSep) {
        if (pos == 0) {
            return columns.get(0);
        }
        return columns.get(pos) - columns.get(pos - 1) - (rownum && pos == 1 ? 0 : columnSep.length());
    }

    private void toTabStops(List<Integer> columns, int rows, boolean rownum, String columnSep) {
        if (rownum) {
            columns.add(0, digits(rows) + 2 + columnSep.length());
        }
        for (int i = 1; i < columns.size(); i++) {
            columns.set(i, columns.get(i - 1) + columns.get(i) + (i > 1 || !rownum ? columnSep.length() : 0));
        }
    }

    private void highlightMap(Map<String, Object> options, Map<String, Object> map, int width) {
        if (!map.isEmpty()) {
            highlightMap(options, map, width, 0);
        } else {
            highlightValue(options, null, objectToString(options, map)).println(terminal());
        }
    }

    @SuppressWarnings("unchecked")
    private void highlightMap(Map<String, Object> options, Map<String, Object> map, int width, int depth) {
        int maxrows = (int) options.get(Printer.MAXROWS);
        int max = map.keySet().stream()
                .map(String::length)
                .max(Integer::compareTo)
                .get();
        if (max > (int) options.getOrDefault(Printer.MAX_COLUMN_WIDTH, Integer.MAX_VALUE)) {
            max = (int) options.get(Printer.MAX_COLUMN_WIDTH);
        }
        Map<String, Object> mapOptions = new HashMap<>(options);
        mapOptions.remove(Printer.MAX_COLUMN_WIDTH);
        int indent = (int) options.get(Printer.INDENTION);
        int maxDepth = (int) options.get(Printer.MAX_DEPTH);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (depth == 0
                    && options.containsKey(Printer.COLUMNS)
                    && !((List<String>) options.get(Printer.COLUMNS)).contains(entry.getKey())) {
                continue;
            }
            AttributedStringBuilder asb =
                    new AttributedStringBuilder().tabs(Arrays.asList(0, depth * indent, depth * indent + max + 1));
            if (depth != 0) {
                asb.append("\t");
            }
            asb.styled(prntStyle.resolve(".mk"), truncateValue(max, entry.getKey()));
            Object elem = entry.getValue();
            boolean convert = canConvert(elem);
            boolean highlightValue = true;
            if (depth < maxDepth && !options.containsKey(Printer.TO_STRING)) {
                if (elem instanceof Map || convert) {
                    Map<String, Object> childMap =
                            convert ? objectToMap(options, elem) : keysToString((Map<Object, Object>) elem);
                    if (!childMap.isEmpty()) {
                        println(asb.columnSubSequence(0, width), maxrows);
                        highlightMap(options, childMap, width, depth + 1);
                        highlightValue = false;
                    }
                } else if (collectionObject(elem)) {
                    List<Object> collection = objectToList(elem);
                    if (!collection.isEmpty()) {
                        println(asb.columnSubSequence(0, width), maxrows);
                        Map<String, Object> listOptions = new HashMap<>(options);
                        listOptions.put(Printer.TO_STRING, true);
                        highlightList(listOptions, collection, width, depth + 1);
                        highlightValue = false;
                    }
                }
            }
            if (highlightValue) {
                AttributedString val = highlightMapValue(mapOptions, entry.getKey(), map);
                asb.append("\t");
                if (map.size() == 1) {
                    if (val.contains('\n')) {
                        for (String v : val.toString().split("\\r?\\n")) {
                            asb.append(highlightValue(options, entry.getKey(), v));
                            println(asb.columnSubSequence(0, width), maxrows);
                            asb = new AttributedStringBuilder().tabs(Arrays.asList(0, max + 1));
                        }
                    } else {
                        asb.append(val);
                        println(asb.columnSubSequence(0, width), maxrows);
                    }
                } else {
                    if (val.contains('\n')) {
                        val = new AttributedString(
                                Arrays.asList(val.toString().split("\\r?\\n")).toString());
                        asb.append(highlightValue(options, entry.getKey(), val.toString()));
                    } else {
                        asb.append(val);
                    }
                    println(asb.columnSubSequence(0, width), maxrows);
                }
            }
        }
    }
}
