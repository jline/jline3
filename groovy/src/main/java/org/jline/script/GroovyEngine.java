/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.script;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.groovy.Utils;
import org.jline.reader.ScriptEngine;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Implements Groovy ScriptEngine.
 * You must be very careful when using GroovyEngine in a multithreaded environment. The Binding instance is not
 * thread safe, and it is shared by all scripts.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class GroovyEngine implements ScriptEngine {
    private static final String REGEX_SYSTEM_VAR = "[A-Z]+[A-Z_]*";
    private static final String REGEX_VAR = "[a-zA-Z_]+[a-zA-Z0-9_]*";
    private static final Pattern PATTERN_FUNCTION_DEF=Pattern.compile("^def\\s+(" + REGEX_VAR + ")\\s*\\(([a-zA-Z0-9_ ,]*)\\)\\s*\\{(.*)?\\}(|\n)$"
                                                                     , Pattern.DOTALL);
    private static final Pattern PATTERN_CLASS_DEF=Pattern.compile("^class\\s+(" + REGEX_VAR + ")\\ .*?\\{.*?\\}(|\n)$"
                                                                  , Pattern.DOTALL);
    private GroovyShell shell;
    private Binding sharedData;
    private Map<String,String> imports = new HashMap<>();
    private Map<String,String> methods = new HashMap<>();
    private Map<String,String> classes = new HashMap<>();

    public GroovyEngine() {
        this.sharedData = new Binding();
        shell = new GroovyShell(sharedData);
    }

    @Override
    public boolean hasVariable(String name) {
        return sharedData.hasVariable(name);
    }

    @Override
    public void put(String name, Object value) {
        sharedData.setProperty(name, value);
    }

    @Override
    public Object get(String name) {
        return sharedData.hasVariable(name) ? sharedData.getVariable(name) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String,Object> find(String name) {
        Map<String, Object> out = new HashMap<>();
        if (name == null) {
            out = sharedData.getVariables();
        } else {
            for (String v : internalFind(name)) {
                out.put(v, get(v));
            }
        }
        return out;
    }

    @Override
    public Object expandParameter(String variable, String format) {
        Object out = variable;
        if (format.equalsIgnoreCase("TXT")) {
            // do nothing
        } else if (format.equalsIgnoreCase("JSON")) {
            out = Utils.toObject(variable);
        } else if (format.equalsIgnoreCase("GROOVY")) {
            try {
                out = execute(variable);
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            variable = variable.trim();
            boolean hasCurly = variable.contains("{") && variable.contains("}");
            try {
                if (variable.startsWith("[") && variable.endsWith("]")) {
                    try {
                        if (hasCurly) {
                            out = Utils.toObject(variable); // try json
                        } else {
                            out = execute(variable);
                        }
                    } catch (Exception e) {
                        if (hasCurly) {
                            try {
                                out = execute(variable);
                            } catch (Exception e2) {

                            }
                        } else {
                            out = Utils.toObject(variable); // try json
                        }
                    }
                } else if (variable.startsWith("{") && variable.endsWith("}")) {
                    out = Utils.toObject(variable);
                }
            } catch (Exception e) {
            }
        }
        return out;
    }

    @Override
    public void persist(Path file, Object object, String format) {
        if (!format.equalsIgnoreCase("JSON")) {
            throw new IllegalArgumentException();
        }
        Utils.persist(file, object);
    }

    @Override
    public Object execute(File script, Object[] args) throws Exception {
        sharedData.setProperty("_args", args);
        Script s = shell.parse(script);
        return s.run();
    }

    @Override
    public Object execute(String statement) throws Exception {
        Object out = null;
        if (statement.startsWith("import ")) {
            shell.evaluate(statement);
            String[] p = statement.split("\\s+", 2);
            imports.put(p[1].replaceAll(";", ""), statement);
        } else if (statement.equals("import")) {
            out = new ArrayList<>(imports.keySet());
        } else if (functionDef(statement)) {
            // do nothing
        } else if (statement.equals("def")) {
            out = methods;
        } else if (statement.matches("def\\s+" + REGEX_VAR)) {
            String name = statement.split("\\s+")[1];
            if (methods.containsKey(name)) {
                out = "def " + name + methods.get(name);
            }
        } else if (classDef(statement)) {
            // do nothing
        } else if (statement.equals("class")) {
            out = classes.values();
        } else if (statement.matches("class\\s+" + REGEX_VAR)) {
            String name = statement.split("\\s+")[1];
            if (classes.containsKey(name)) {
                out = classes.get(name);
            }
        } else {
            String e = "";
            for (Map.Entry<String, String> entry : imports.entrySet()) {
                e += entry.getValue()+"\n";
            }
            for (String c : classes.values()) {
                e += c;
            }
            e += statement;
            out = shell.evaluate(e);
        }
        return out;
    }

    @Override
    public String getEngineName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("groovy");
    }

    @SuppressWarnings("unchecked")
    private List<String> internalFind(String var) {
        List<String> out = new ArrayList<>();
        if(!var.contains(".") && var.contains("*")) {
            var = var.replaceAll("\\*", ".*");
        }
        for (String v :  (Set<String>)sharedData.getVariables().keySet()) {
            if (v.matches(var)) {
                out.add(v);
            }
        }
        return out;
    }

    private boolean functionDef (String statement) throws Exception{
        boolean out = false;
        Matcher m = PATTERN_FUNCTION_DEF.matcher(statement);
        if(m.matches()){
            out = true;
            put(m.group(1), execute("{" + m.group(2) + "->" + m.group(3) + "}"));
            methods.put(m.group(1), "(" + m.group(2) + ")" + "{" + m.group(3) + "}");
        }
        return out;
    }

    private boolean classDef (String statement) throws Exception{
        boolean out = false;
        Matcher m = PATTERN_CLASS_DEF.matcher(statement);
        if(m.matches()){
            out = true;
            classes.put(m.group(1), statement);
        }
        return out;
    }

    private void del(String var) {
        if (var == null) {
            return;
        }
        if (imports.containsKey(var)) {
            imports.remove(var);
        } else if (sharedData.hasVariable(var)) {
            sharedData.getVariables().remove(var);
            if (methods.containsKey(var)) {
                methods.remove(var);
            }
        } else if (!var.contains(".") && var.contains("*")) {
            for (String v : internalFind(var)){
                if (sharedData.hasVariable(v) && !v.equals("_") && !v.matches(REGEX_SYSTEM_VAR)) {
                    sharedData.getVariables().remove(v);
                    if (methods.containsKey(v)) {
                        methods.remove(v);
                    }
                }
                if (classes.containsKey(v)) {
                    classes.remove(v);
                }
            }
        }
        if (classes.containsKey(var)) {
            classes.remove(var);
        }
    }

    @Override
    public void del(String... vars) {
        if (vars == null) {
            return;
        }
        for (String s: vars) {
            del(s);
        }
    }

    @Override
    public String format(Map<String, Object> options, Object obj) {
        String out = obj instanceof String ? (String)obj : "";
        String style = (String)options.getOrDefault("style", "");
        if (style.equalsIgnoreCase("JSON")) {
            out = Utils.toJson(obj);
        } else if (!(obj instanceof String)) {
            throw new IllegalArgumentException("Bad or missing style option: " + style);
        }
        return out;
    }

    @Override
    public List<AttributedString> highlight(Map<String, Object> options, Object obj) {
        List<AttributedString> out = new ArrayList<>();
        String style = (String)options.getOrDefault("style", "");
        if (style.equalsIgnoreCase("JSON")) {
            throw new IllegalArgumentException("Bad style option: " + style);
        } else {
            try {
                out = internalHighlight(options, obj);
            } catch (Exception e) {
                out = internalHighlight(options, Utils.convert(obj));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<AttributedString> internalHighlight(Map<String, Object> options, Object obj) {
        List<AttributedString> out = new ArrayList<>();
        int width = (int)options.getOrDefault("width", Integer.MAX_VALUE);
        boolean rownum = options.containsKey("rownum");
        if (obj == null) {
            // do nothing
        } else if (obj instanceof Map) {
            out = highlightMap((Map<String, Object>)obj, width);
        } else if (obj instanceof Collection<?> || obj instanceof Object[]) {
            Collection<?> collection = obj instanceof Collection<?> ? (Collection<?>)obj
                                                                    : Arrays.asList((Object[])obj);
            if (!collection.isEmpty()) {
                if (collection.size() == 1) {
                    Object elem = collection.iterator().next();
                    if (elem instanceof Map) {
                        out = highlightMap((Map<String, Object>)elem, width);
                    } else {
                        out.add(new AttributedString(Utils.toString(obj)));
                    }
                } else {
                    Object elem = collection.iterator().next();
                    if (elem instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>)elem;
                        List<String> header = new ArrayList<>();
                        header.addAll(map.keySet());
                        List<Integer> columns = new ArrayList<>();
                        for (int i = 0; i < header.size(); i++) {
                            columns.add(header.get(i).length() + 1);
                        }
                        for (Object o : collection) {
                            for (int i = 0; i < header.size(); i++) {
                                Map<String, Object> m = (Map<String, Object>)o;
                                if (Utils.toString(m.get(header.get(i))).length() > columns.get(i) - 1) {
                                    columns.set(i, Utils.toString(m.get(header.get(i))).length() + 1);
                                }
                            }
                        }
                        columns.add(0, 0);
                        toTabStops(columns, rownum);
                        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(columns);
                        if (rownum) {
                            asb.append("\t");
                        }
                        for (int i = 0; i < header.size(); i++) {
                            asb.append(header.get(i), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE + AttributedStyle.BRIGHT));
                            asb.append("\t");
                        }
                        if (asb.columnLength() > width) {
                            asb.setLength(width);
                        }
                        out.add(asb.toAttributedString());
                        Integer row = 0;
                        for (Object o : collection) {
                            AttributedStringBuilder asb2 = new AttributedStringBuilder().tabs(columns);
                            if (rownum) {
                                asb2.append(row.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE + AttributedStyle.BRIGHT));
                                asb2.append("\t");
                                row++;
                            }
                            for (int i = 0; i < header.size(); i++) {
                                Map<String, Object> m = (Map<String, Object>)o;
                                asb2.append(Utils.toString(m.get(header.get(i))));
                                asb2.append("\t");
                            }
                            if (asb2.columnLength() > width) {
                                asb2.setLength(width);
                            }
                            out.add(asb2.toAttributedString());
                        }
                    } else if (elem instanceof Collection || elem instanceof Object[]) {
                        boolean isCollection = elem instanceof Collection;
                        List<Integer> columns = new ArrayList<>();
                        for (Object o : collection) {
                            List<Object> inner = new ArrayList<>();
                            inner.addAll(isCollection ? (Collection<?>)o : Arrays.asList((Object[])o));
                            for (int i = 0; i < inner.size(); i++) {
                                int len1 = Utils.toString(inner.get(i)).length() + 1;
                                if (columns.size() <= i) {
                                    columns.add(len1);
                                } else if (len1 > columns.get(i)) {
                                    columns.set(i, len1);
                                }
                            }
                        }
                        toTabStops(columns, rownum);
                        Integer row = 0;
                        for (Object o : collection) {
                            AttributedStringBuilder asb = new AttributedStringBuilder().tabs(columns);
                            if (rownum) {
                                asb.append(row.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE + AttributedStyle.BRIGHT));
                                asb.append("\t");
                                row++;
                            }
                            List<Object> inner = new ArrayList<>();
                            inner.addAll(isCollection ? (Collection<?>)o : Arrays.asList((Object[])o));
                            for (int i = 0; i < inner.size(); i++) {
                                asb.append(Utils.toString(inner.get(i)));
                                asb.append("\t");
                            }
                            if (asb.columnLength() > width) {
                                asb.setLength(width);
                            }
                            out.add(asb.toAttributedString());
                        }
                    } else {
                        Integer row = 0;
                        Integer tabsize = ((Integer)collection.size()).toString().length() + 1;
                        for (Object o: collection) {
                            AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabsize);
                            if (rownum) {
                                asb.append(row.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE + AttributedStyle.BRIGHT));
                                asb.append("\t");
                                row++;
                            }
                            asb.append(Utils.toString(o));
                            if (asb.columnLength() > width) {
                                asb.setLength(width);
                            }
                            out.add(asb.toAttributedString());
                        }
                    }
                }
            }
        } else {
            out.add(new AttributedString(Utils.toString(obj)));
        }
        return out;
    }

    private void toTabStops(List<Integer> columns, boolean rownum) {
        if (rownum) {
            columns.add(0, 5);
        }
        for (int i = 1; i < columns.size(); i++) {
            columns.set(i, columns.get(i - 1) + columns.get(i));
        }
    }

    private List<AttributedString> highlightMap(Map<String, Object> map, int width) {
        List<AttributedString> out = new ArrayList<>();
        int max = map.keySet().stream().map(String::length).max(Integer::compareTo).get();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            AttributedStringBuilder asb = new AttributedStringBuilder().tabs(Arrays.asList(0, max + 1));
            asb.append(entry.getKey(), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE + AttributedStyle.BRIGHT));
            asb.append("\t");
            asb.append(Utils.toString(entry.getValue()), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
            if (asb.columnLength() > width) {
                asb.setLength(width);
            }
            out.add(asb.toAttributedString());
        }
        return out;
    }
}
