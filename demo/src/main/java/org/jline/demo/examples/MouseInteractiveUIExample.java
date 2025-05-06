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
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;

/**
 * Example demonstrating interactive UI with mouse support in JLine.
 */
public class MouseInteractiveUIExample {

    // SNIPPET_START: MouseInteractiveUIExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Enable mouse tracking
            terminal.trackMouse(Terminal.MouseTracking.Normal);

            // Create a display for managing the screen
            Display display = new Display(terminal, true);

            // Define some buttons
            List<Button> buttons = new ArrayList<>();
            buttons.add(new Button(5, 3, "Button 1", () -> {
                terminal.writer().println("Button 1 clicked!");
                terminal.flush();
            }));
            buttons.add(new Button(5, 5, "Button 2", () -> {
                terminal.writer().println("Button 2 clicked!");
                terminal.flush();
            }));
            buttons.add(new Button(5, 7, "Exit", () -> {
                // This will be used to exit the application
            }));

            // Initial render
            display.clear();
            terminal.writer().println("Interactive UI Example");
            terminal.writer().println("Click on the buttons below:");
            terminal.writer().println();

            for (Button button : buttons) {
                button.render(terminal);
            }

            terminal.flush();

            // Event loop
            boolean running = true;
            StringBuilder buffer = new StringBuilder();
            boolean esc = false;
            boolean bracket = false;
            boolean mouse = false;

            while (running) {
                int c = terminal.reader().read();

                // Parse escape sequences for mouse events
                if (c == '\033') {
                    esc = true;
                    buffer.setLength(0);
                } else if (esc && c == '[') {
                    bracket = true;
                } else if (esc && bracket && c == 'M') {
                    mouse = true;
                    buffer.setLength(0);
                } else if (mouse && buffer.length() < 3) {
                    buffer.append((char) c);

                    if (buffer.length() == 3) {
                        int b = buffer.charAt(0) - 32;
                        int x = buffer.charAt(1) - 32;
                        int y = buffer.charAt(2) - 32;

                        // Check if this is a mouse press event
                        if ((b & 3) != 3 && (b & 64) == 0) {
                            // Check if any button was clicked
                            for (Button button : buttons) {
                                if (button.isInside(x, y)) {
                                    button.click();

                                    // Check if Exit button was clicked
                                    if (button.getText().equals("Exit")) {
                                        running = false;
                                    }

                                    break;
                                }
                            }
                        }

                        // Reset state
                        esc = false;
                        bracket = false;
                        mouse = false;
                    }
                } else {
                    // Not a mouse event or incomplete sequence
                    esc = false;
                    bracket = false;
                    mouse = false;
                }
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.trackMouse(Terminal.MouseTracking.Off);

            terminal.close();
        }
    }

    // Simple button class for demonstration
    static class Button {
        private final int x;
        private final int y;
        private final String text;
        private final Runnable action;

        public Button(int x, int y, String text, Runnable action) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.action = action;
        }

        public void render(Terminal terminal) {
            // Position cursor
            terminal.writer().write("\033[" + y + ";" + x + "H");

            // Draw button
            AttributedString buttonText = new AttributedString(
                    "[ " + text + " ]",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold());
            buttonText.print(terminal);
        }

        public boolean isInside(int mouseX, int mouseY) {
            return mouseY == y && mouseX >= x && mouseX < x + text.length() + 4;
        }

        public void click() {
            action.run();
        }

        public String getText() {
            return text;
        }
    }
    // SNIPPET_END: MouseInteractiveUIExample
}
