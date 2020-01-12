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
import java.util.*;

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
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class GroovyEngine implements ScriptEngine {
    private GroovyShell shell;
    private Binding sharedData;
    private Map<String,String> imports = new HashMap<String,String>();

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

    @Override
    public Map<String,Object> get() {
        return sharedData.getVariables();
    }

    @Override
    public Object execute(File script, Object[] args) throws Exception {
        sharedData.setProperty("_args", args);
        Script s = shell.parse("import ");
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
        } else {
            String e = "";
            for (Map.Entry<String, String> entry : imports.entrySet()) {
                e += entry.getValue()+"\n";
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

    private void del(String var) {
        if (var == null) {
            return;
        }
        if (imports.containsKey(var)) {
            imports.remove(var);
        } else if (sharedData.hasVariable(var)) {
            sharedData.getVariables().remove(var);
        } else if (!var.contains(".") && var.contains("*")) {
            var = var.replaceAll("\\*", ".*");
            Map<String, Object> vars=sharedData.getVariables();
            List<String> todel = new ArrayList<String>();
            for (Map.Entry<String,Object> entry : vars.entrySet()){
                if (!entry.getKey().equals("_") && entry.getKey().matches(var)) {
                    todel.add(entry.getKey());
                }
            }
            for (String v : todel){
                if (sharedData.hasVariable(v)) {
                    sharedData.getVariables().remove(v);
                }
            }
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
    public List<AttributedString> format(Map<String, Object> options, Object obj) {
        List<AttributedString> out = new ArrayList<>();
        int width = (int)options.getOrDefault("width", 80);
        if (obj == null) {
            // do nothing
        } else if (obj instanceof Map) {
            out = formatMap((Map<String, Object>)obj, width);
        } else if (obj instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>)obj;
            if (!collection.isEmpty()) {
                if (collection.size() == 1) {
                    Object elem = collection.iterator().next();
                    if (elem instanceof Map) {
                        out = formatMap((Map<String, Object>)elem, width);
                    } else {
                        out.add(new AttributedString(Utils.toString(obj)));
                    }
                } else {

                }
            }
        } else {
            out.add(new AttributedString(Utils.toString(obj)));
        }
        return out;
    }

    private List<AttributedString> formatMap(Map<String, Object> map, int width) {
        List<AttributedString> out = new ArrayList<>();
        int max = map.keySet().stream().map(String::length).max(Integer::compareTo).get();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            AttributedStringBuilder asb = new AttributedStringBuilder().tabs(Arrays.asList(1, max + 2));
            asb.append("\t");
            asb.append(entry.getKey(), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
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
