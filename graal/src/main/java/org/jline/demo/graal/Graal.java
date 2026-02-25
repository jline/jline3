/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.graal;

import java.nio.file.Paths;

import org.jline.reader.LineReader;
import org.jline.shell.Shell;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.spi.TerminalExt;
import org.jline.utils.OSUtils;

public class Graal {

    public static void main(String[] args) {
        try {
            Terminal terminal = TerminalBuilder.builder().provider("ffm").build();

            System.out.println(terminal.getName() + ": " + terminal.getType() + ", provider="
                    + ((TerminalExt) terminal).getProvider().name());

            Shell shell = Shell.builder()
                    .terminal(terminal)
                    .prompt("graal> ")
                    .helpCommands(true)
                    .historyCommands(true)
                    .optionCommands(true)
                    .variableCommands(true)
                    .commandHighlighter(true)
                    .historyFile(Paths.get(System.getProperty("user.home"), ".jline-graal-history"))
                    .option(LineReader.Option.INSERT_BRACKET, true)
                    .option(LineReader.Option.EMPTY_WORD_OPTIONS, false)
                    .option(LineReader.Option.USE_FORWARD_SLASH, true)
                    .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                    .variable(LineReader.INDENTATION, 2)
                    .variable(LineReader.LIST_MAX, 100)
                    .onReaderReady(reader -> {
                        if (OSUtils.IS_WINDOWS) {
                            reader.setVariable(LineReader.BLINK_MATCHING_PAREN, 0);
                        }
                    })
                    .build();

            shell.run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
