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

import org.jline.console.ScriptEngine;
import org.jline.groovy.Utils;

import groovy.lang.Binding;
import groovy.lang.Closure;
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
    public enum Format {JSON, GROOVY, NONE};
    private static final String REGEX_SYSTEM_VAR = "[A-Z]+[A-Z_]*";
    private static final String REGEX_VAR = "[a-zA-Z_]+[a-zA-Z0-9_]*";
    private static final Pattern PATTERN_FUNCTION_DEF=Pattern.compile("^def\\s+(" + REGEX_VAR + ")\\s*\\(([a-zA-Z0-9_ ,]*)\\)\\s*\\{(.*)?\\}(|\n)$"
                                                                     , Pattern.DOTALL);
    private static final Pattern PATTERN_CLASS_DEF=Pattern.compile("^class\\s+(" + REGEX_VAR + ")\\ .*?\\{.*?\\}(|\n)$"
                                                                  , Pattern.DOTALL);
    private GroovyShell shell;
    protected Binding sharedData;
    private Map<String,String> imports = new HashMap<>();
    private Map<String,String> methods = new HashMap<>();

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
    public List<String> getSerializationFormats() {
        return Arrays.asList(Format.JSON.toString(), Format.NONE.toString());
    }

    @Override
    public List<String> getDeserializationFormats() {
        return Arrays.asList(Format.JSON.toString(), Format.GROOVY.toString(), Format.NONE.toString());
    }

    @Override
    public Object deserialize(String value, String formatStr) {
        Object out = value;
        Format format = formatStr != null && !formatStr.isEmpty() ? Format.valueOf(formatStr.toUpperCase()) : null;
        if (format == Format.NONE) {
            // do nothing
        } else if (format == Format.JSON) {
            out = Utils.toObject(value);
        } else if (format == Format.GROOVY) {
            try {
                out = execute(value);
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            value = value.trim();
            boolean hasCurly = value.contains("{") && value.contains("}");
            try {
                if (value.startsWith("[") && value.endsWith("]")) {
                    try {
                        if (hasCurly) {
                            out = Utils.toObject(value); // try json
                        } else {
                            out = execute(value);
                        }
                    } catch (Exception e) {
                        if (hasCurly) {
                            try {
                                out = execute(value);
                            } catch (Exception e2) {

                            }
                        } else {
                            out = Utils.toObject(value); // try json
                        }
                    }
                } else if (value.startsWith("{") && value.endsWith("}")) {
                    out = Utils.toObject(value);
                }
            } catch (Exception e) {
            }
        }
        return out;
    }

    @Override
    public void persist(Path file, Object object) {
        persist(file, object, getSerializationFormats().get(0));
    }

    @Override
    public void persist(Path file, Object object, String format) {
        Utils.persist(file, object, Format.valueOf(format.toUpperCase()));
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
        } else {
            String e = "";
            for (Map.Entry<String, String> entry : imports.entrySet()) {
                e += entry.getValue()+"\n";
            }
            e += statement;
            if (classDef(statement)) {
                e += "; null";
            }
            out = shell.evaluate(e);
        }
        return out;
    }

    @Override
    public Object execute(Object closure, Object... args) {
        if (!(closure instanceof Closure)) {
            throw new IllegalArgumentException();
        }
        return ((Closure<?>)closure).call(args);
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
        return PATTERN_CLASS_DEF.matcher(statement).matches();
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
    public String toJson(Object obj) {
        return Utils.toJson(obj);
    }

    @Override
    public String toString(Object obj) {
        return Utils.toString(obj);
    }

    @Override
    public Map<String,Object> toMap(Object obj) {
        return Utils.toMap(obj);
    }

}
