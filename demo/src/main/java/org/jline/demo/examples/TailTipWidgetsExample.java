/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.console.CmdDesc;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.widget.TailTipWidgets;
import org.jline.widget.TailTipWidgets.TipType;

/**
 * Example demonstrating TailTip widgets in JLine.
 */
public class TailTipWidgetsExample {

    // SNIPPET_START: TailTipWidgetsExample
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a line reader with some completions for demonstration
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("help", "widget", "exit", "clear"))
                .build();

        // Create command descriptions for TailTipWidgets
        Map<String, CmdDesc> tailTips = new HashMap<>();

        // Description for 'help' command
        tailTips.put("help", new CmdDesc(Arrays.asList(new AttributedString("Display help information")), null, null));

        // Description for 'widget' command with options
        List<AttributedString> mainDesc = Arrays.asList(
                new AttributedString("widget -N new-widget [function-name]"),
                new AttributedString("widget -D widget ..."),
                new AttributedString("widget -A old-widget new-widget"),
                new AttributedString("widget -l [options]"));

        Map<String, List<AttributedString>> widgetOpts = new HashMap<>();
        widgetOpts.put("-N", Arrays.asList(new AttributedString("Create new widget")));
        widgetOpts.put("-D", Arrays.asList(new AttributedString("Delete widgets")));
        widgetOpts.put("-A", Arrays.asList(new AttributedString("Create alias to widget")));
        widgetOpts.put("-l", Arrays.asList(new AttributedString("List user-defined widgets")));

        tailTips.put("widget", new CmdDesc(mainDesc, null, widgetOpts));

        // Create tailtip widgets that uses description window size 5
        // and displays suggestions based on the completer
        TailTipWidgets tailtipWidgets = new TailTipWidgets(reader, tailTips, 5, TipType.COMPLETER);

        // Enable tailtip widgets
        tailtipWidgets.enable();

        // Display instructions
        terminal.writer().println("TailTip Widgets Example");
        terminal.writer().println("----------------------");
        terminal.writer().println("As you type commands, you'll see suggestions and descriptions.");
        terminal.writer().println("Try typing 'widget' to see command options and descriptions.");
        terminal.writer().println("- Type 'exit' to quit");
        terminal.writer().println();

        // Read input with tailtip widgets
        String line;
        while (true) {
            try {
                line = reader.readLine("tailtip> ");

                if (line.equalsIgnoreCase("exit")) {
                    break;
                } else if (line.equalsIgnoreCase("clear")) {
                    terminal.writer().print("\033[H\033[2J");
                    terminal.writer().flush();
                } else {
                    terminal.writer().println("You entered: " + line);
                    terminal.writer().flush();
                }
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
                terminal.writer().flush();
            }
        }

        terminal.close();
    }
    // SNIPPET_END: TailTipWidgetsExample
}
