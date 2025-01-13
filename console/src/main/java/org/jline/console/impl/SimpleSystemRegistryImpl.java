/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.jline.builtins.ConfigurationPath;
import org.jline.reader.LineReader;
import org.jline.reader.Parser;
import org.jline.terminal.Terminal;

/**
 * Simple SystemRegistry which stores variables in the LineReader.
 */
public class SimpleSystemRegistryImpl extends SystemRegistryImpl {
    private LineReader lineReader;

    public SimpleSystemRegistryImpl(
            Parser parser, Terminal terminal, Supplier<Path> workDir, ConfigurationPath configPath) {
        super(parser, terminal, workDir, configPath);
    }

    public void setLineReader(LineReader lineReader) {
        this.lineReader = lineReader;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T consoleOption(String name, T defVal) {
        return (T) lineReader.getVariables().getOrDefault(name, defVal);
    }

    @Override
    public void setConsoleOption(String name, Object value) {
        lineReader.setVariable(name, value);
    }
}
