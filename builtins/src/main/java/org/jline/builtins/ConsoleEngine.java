/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.File;
import java.util.Map;

import org.jline.builtins.CommandRegistry;
import org.jline.reader.ParsedLine;

public interface ConsoleEngine extends CommandRegistry {

    void setSystemRegistry(SystemRegistry systemRegistry);

    Object execute(ParsedLine parsedLine) throws Exception;

    default Object execute(File script) throws Exception {
        return execute(script, "", new String[0]);
    }

    Object execute(File script, String cmdLine, String[] args) throws Exception;

    Object postProcess(String line, Object result);

    void println(Object object);

    void println(Map<String, Object> options, Object object);

}
