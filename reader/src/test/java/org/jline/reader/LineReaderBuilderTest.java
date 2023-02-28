/*
 * Copyright (c) 2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LineReaderBuilderTest {

    @Test
    public void testInheritAppNameFromTerminal() throws IOException {
        final String expectedAppName = "BOB";
        final Terminal terminal =
                TerminalBuilder.builder().name(expectedAppName).build();
        final LineReader lineReader =
                LineReaderBuilder.builder().terminal(terminal).build();

        assertEquals("Did not inherit appName from terminal", expectedAppName, lineReader.getAppName());
    }

    @Test
    public void testPreferAppNameFromBuilder() throws IOException {
        final String expectedAppName = "NANCY";
        final Terminal terminal =
                TerminalBuilder.builder().name(expectedAppName + "X").build();
        final LineReader lineReader = LineReaderBuilder.builder()
                .appName(expectedAppName)
                .terminal(terminal)
                .build();

        assertEquals("Did not prefer appName from builder", expectedAppName, lineReader.getAppName());
    }
}
