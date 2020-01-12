/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.Map;
import java.util.HashMap;

import org.jline.builtins.CommandRegistry;
import org.jline.reader.ParsedLine;

public interface ConsoleEngine extends CommandRegistry {
    
    void setSystemRegistry(SystemRegistry systemRegistry);
    
    Object execute(ParsedLine parsedLine) throws Exception;
    
    default void println(Object object) {
        println(new HashMap<>(), object);    
    }
    
    void println(Map<String, Object> options, Object object);

}
