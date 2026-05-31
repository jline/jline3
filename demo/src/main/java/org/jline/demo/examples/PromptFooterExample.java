/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Demonstrates the per-item footer of list and checkbox prompts.
 *
 * <p>Each item may declare a {@code footer(...)}. While that item is focused, its footer is shown
 * dimmed in a fixed-height pane below the list. The pane is sized to the tallest footer among the
 * items so the list does not shift as the selection moves, and footers may span multiple lines
 * (explicit {@code \n} and/or automatic wrapping to the terminal width). The maximum pane height is
 * configurable via {@link PrompterConfig#footerMaxRows()}.
 */
public class PromptFooterExample {

    public static void main(String[] args) throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            PrintWriter writer = terminal.writer();
            if (Terminal.TYPE_DUMB.equals(terminal.getType()) || Terminal.TYPE_DUMB_COLOR.equals(terminal.getType())) {
                writer.println("Dumb terminal detected. This demo needs a real terminal.");
                writer.flush();
                return;
            }

            Prompter prompter = PrompterFactory.create(terminal);

            List<AttributedString> header = new ArrayList<>();
            header.add(new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.GREEN))
                    .append("JLine Prompt footer demo")
                    .toAttributedString());
            header.add(new AttributedString("Move up and down to see the dimmed footer change for the focused item."));

            PromptBuilder promptBuilder = prompter.newBuilder();

            promptBuilder
                    .createListPrompt()
                    .name("type")
                    .message("Select the installation type")
                    .newItem("full")
                    .text("Full installation")
                    .footer("Installs all available components, including the optional and extra data files.")
                    .add()
                    .newItem("standard")
                    .text("Standard installation")
                    .footer("Installs the recommended set of components for most users.")
                    .add()
                    .newItem("custom")
                    .text("Custom installation")
                    .footer("Lets you choose exactly which components are installed on the next screen.\n"
                            + "Pick this if you want to exclude optional parts or save disk space.")
                    .add()
                    .addPrompt();

            promptBuilder
                    .createCheckboxPrompt()
                    .name("components")
                    .message("Select components")
                    .newItem("core")
                    .text("Core files")
                    .checked(true)
                    .footer("Mandatory runtime files. Required by every installation type.")
                    .add()
                    .newItem("docs")
                    .text("Documentation")
                    .footer("Offline HTML manuals and API reference.")
                    .add()
                    .newItem("samples")
                    .text("Sample projects")
                    .footer("Example projects you can open and build to learn the tool.")
                    .add()
                    .addPrompt();

            Map<String, ? extends PromptResult<? extends Prompt>> result =
                    prompter.prompt(header, promptBuilder.build());

            writer.println("Installation type: " + ((ListResult) result.get("type")).getSelectedId());
            writer.println("Components: " + ((CheckboxResult) result.get("components")).getSelectedIds());
            writer.flush();
        } catch (UserInterruptException e) {
            // cancelled by the user, nothing to do
        }
    }
}
