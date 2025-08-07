/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.prompt.ConfirmResult;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;

@SuppressWarnings("removal")
public class Issue1025 {

    private static void addInHeader(List<AttributedString> header, String text) {
        addInHeader(header, AttributedStyle.DEFAULT, text);
    }

    private static void addInHeader(List<AttributedString> header, AttributedStyle style, String text) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(style).append(text);
        header.add(asb.toAttributedString());
    }

    @Test
    void testIssue1025() throws Exception {
        EofPipedInputStream in = new EofPipedInputStream();
        in.setIn(new ByteArrayInputStream("\r\r\r\r\r\r".getBytes()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalBuilder.builder()
                .attributes(new Attributes())
                .type("xterm")
                .streams(in, baos)
                .build()) {
            test(terminal);
        }
    }

    public static void main(String[] args) throws Exception {
        try (Terminal terminal = TerminalBuilder.terminal()) {
            test(terminal);
        }
    }

    private static void test(Terminal terminal) throws IOException {
        List<AttributedString> header = new ArrayList<>();
        AttributedStyle style = new AttributedStyle();
        addInHeader(header, style.italic().foreground(2), "Hello World!");
        addInHeader(
                header, "This is a demonstration of ConsoleUI java library. It provides a simple console interface");
        addInHeader(
                header,
                "for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written");
        addInHeader(header, "in JavaScript.");

        // If the header exceeds the height of the display, this will lead to a crash.
        for (int i = 0; i < 80; i++) {
            addInHeader(header, "Extra line: " + i);
        }

        ConsolePrompt.UiConfig config;
        config = new ConsolePrompt.UiConfig("\u276F", "\u25EF ", "\u25C9 ", "\u25EF ");
        //
        // LineReader is needed only if you are adding JLine Completers in your prompts.
        // If you are not using Completers you do not need to create LineReader.
        //
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        ConsolePrompt prompt = new ConsolePrompt(reader, terminal, config);
        PromptBuilder promptBuilder = prompt.getPromptBuilder();

        promptBuilder
                .createInputPrompt()
                .name("name")
                .message("Please enter your name")
                .defaultValue("John Doe")
                // .mask('*')
                .addCompleter(
                        // new Completers.FilesCompleter(() -> Paths.get(System.getProperty("user.dir"))))
                        new StringsCompleter("Jim", "Jack", "John", "Donald", "Dock"))
                .addPrompt();

        promptBuilder
                .createListPrompt()
                .name("pizzatype")
                .message("Which pizza do you want?")
                .newItem()
                .text("Margherita")
                .add() // without name (name defaults to text)
                .newItem("veneziana")
                .text("Veneziana")
                .add()
                .newItem("hawai")
                .text("Hawai")
                .add()
                .newItem("quattro")
                .text("Quattro Stagioni")
                .add()
                .addPrompt();

        promptBuilder
                .createCheckboxPrompt()
                .name("topping")
                .message("Please select additional toppings:")
                .newSeparator("standard toppings")
                .add()
                .newItem()
                .name("cheese")
                .text("Cheese")
                .add()
                .newItem("bacon")
                .text("Bacon")
                .add()
                .newItem("onions")
                .text("Onions")
                .disabledText("Sorry. Out of stock.")
                .add()
                .newSeparator()
                .text("special toppings")
                .add()
                .newItem("salami")
                .text("Very hot salami")
                .check()
                .add()
                .newItem("salmon")
                .text("Smoked Salmon")
                .add()
                .newSeparator("and our speciality...")
                .add()
                .newItem("special")
                .text("Anchovies, and olives")
                .checked(true)
                .add()
                .addPrompt();

        promptBuilder
                .createChoicePrompt()
                .name("payment")
                .message("How do you want to pay?")
                .newItem()
                .name("cash")
                .message("Cash")
                .key('c')
                .asDefault()
                .add()
                .newItem("visa")
                .message("Visa Card")
                .key('v')
                .add()
                .newItem("master")
                .message("Master Card")
                .key('m')
                .add()
                .newSeparator("online payment")
                .add()
                .newItem("paypal")
                .message("Paypal")
                .key('p')
                .add()
                .addPrompt();

        promptBuilder
                .createConfirmPromp()
                .name("delivery")
                .message("Is this pizza for delivery?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .addPrompt();

        Map<String, ? extends PromptResultItemIF> result = prompt.prompt(header, promptBuilder.build());
        System.out.println("result = " + result);

        ConfirmResult delivery = (ConfirmResult) result.get("delivery");
        if (delivery.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
            System.out.println("We will deliver the pizza in 5 minutes");
        }
    }

    public static class EofPipedInputStream extends InputStream {

        private InputStream in;

        public InputStream getIn() {
            return in;
        }

        public void setIn(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            return in != null ? in.read() : -1;
        }

        @Override
        public int available() throws IOException {
            return in != null ? in.available() : 0;
        }
    }
}
