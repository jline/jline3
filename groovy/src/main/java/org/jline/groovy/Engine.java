package org.jline.groovy;

import java.io.File;
import java.util.*;

import org.jline.reader.ScriptEngine;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class Engine implements ScriptEngine {
    private GroovyShell shell;
    private Binding sharedData;
    private Map<String,String> imports = new HashMap<String,String>();

    public Engine() {
        this.sharedData = new Binding();
        shell = new GroovyShell(sharedData);
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
        sharedData.setProperty("args", args);
        Script s = shell.parse(script);
        return s.run();
    }

    @Override
    public Object execute(String statement) throws Exception {
        Object out=null;
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
            out=shell.evaluate(e);
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

    private void del(String var){
        if (var==null) {
            return;
        }
        if (imports.containsKey(var)) {
            imports.remove(var);
        } else if(sharedData.hasVariable(var)){
            sharedData.getVariables().remove(var);
        } else if (!var.contains(".") && var.contains("*")) {
            var = var.replace("*", ".*");
            Map<String,Object> vars = sharedData.getVariables();
            for (String v : vars.keySet()){
                if (v.matches(var) && sharedData.hasVariable(v)) {
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
}
