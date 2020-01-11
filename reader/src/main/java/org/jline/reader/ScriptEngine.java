/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import java.io.File;
import java.util.*;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

public interface ScriptEngine {

    String getEngineName();

    Collection<String> getExtensions();

    void put(String name, Object value);

    Object get(String name);

    Map<String,Object> get();

    void del(String... vars);

    Object execute(String statement) throws Exception;

    default Object execute(File script) throws Exception {
        return execute(script, null);
    }

    Object execute(File script, Object[] args) throws Exception;

    static List<Map<String, Object>> listEngines() {
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
