package org.jline.script;

import java.io.File;
import java.util.*;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

public interface JLineEngine {

    public String getEngineName();

    public void put(String name, Object value);

    public Object get(String name);

    public Map<String,Object> get();

    public void del(String... vars);

    public Object execute(String statement) throws Exception;

    public Object execute(File script) throws Exception;

    public static List<Map<String, Object>> listEngines() {
        List<Map<String, Object>> out = new ArrayList<>();
        ScriptEngineManager f = new ScriptEngineManager();
        List<ScriptEngineFactory> engines = f.getEngineFactories();
        for (ScriptEngineFactory engine : engines) {
            Map<String,Object> e = new HashMap<>();
            e.put("name", engine.getEngineName());
            e.put("version", engine.getEngineVersion());
            e.put("language", engine.getLanguageName());
            e.put("extensions", engine.getExtensions());
            e.put("nick-names", engine.getNames());
            out.add(e);
        }
        return out;
    }
}
