/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for the suggestion box placement in Nano editor.
 */
public class NanoSuggestionBoxTest {

    /**
     * A custom Nano implementation that exposes the buildSuggestionBox method for testing.
     */
    static class TestableNano extends Nano {
        public TestableNano(Terminal terminal) {
            super(terminal, Paths.get("."));
        }

        /**
         * Expose the buildSuggestionBox method for testing.
         */
        public Nano.Box buildSuggestionBoxForTest(
                List<AttributedString> suggestions,
                List<AttributedString> screenLines,
                int cursorLine,
                int firstLineToDisplay) {
            // Set up the buffer with the cursor at the specified line
            buffer = new Buffer(null);
            buffer.lines = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                buffer.lines.add("Line " + (i + 1));
            }
            buffer.line = cursorLine;
            buffer.column = 3;
            buffer.firstLineToDisplay = firstLineToDisplay;
            buffer.offsetInLine = 0;
            buffer.offsetInLineToDisplay = 0;
            buffer.computeAllOffsets(); // Initialize offsets

            // Set the terminal size
            size.copy(terminal.getSize());

            // Call the protected method using reflection
            try {
                java.lang.reflect.Method method =
                        Nano.class.getDeclaredMethod("buildSuggestionBox", List.class, List.class);
                method.setAccessible(true);
                return (Nano.Box) method.invoke(this, suggestions, screenLines);
            } catch (Exception e) {
                throw new RuntimeException("Failed to call buildSuggestionBox", e);
            }
        }
    }

    @Test
    public void testSuggestionBoxPlacement() throws IOException {
        // Create a dumb terminal for testing
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = new DumbTerminal("name", "dumb", in, out, Charset.forName("UTF-8"));
        terminal.setSize(new Size(80, 24));

        TestableNano nano = new TestableNano(terminal);

        // Create suggestions
        List<AttributedString> suggestions = new ArrayList<>();
        suggestions.add(new AttributedString("Suggestion 1"));
        suggestions.add(new AttributedString("Suggestion 2"));
        suggestions.add(new AttributedString("Suggestion 3"));

        // Create screen lines
        List<AttributedString> screenLines = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            screenLines.add(new AttributedString("Screen line " + i));
        }

        // Test with cursor at different positions

        // 1. Test with cursor at the top of the screen
        Nano.Box box1 = nano.buildSuggestionBoxForTest(suggestions, screenLines, 0, 0);
        assertNotNull(box1, "Box should be created when cursor is at the top");

        // 2. Test with cursor in the middle of the screen
        Nano.Box box2 = nano.buildSuggestionBoxForTest(suggestions, screenLines, 10, 0);
        assertNotNull(box2, "Box should be created when cursor is in the middle");

        // 3. Test with cursor at the bottom of the screen
        Nano.Box box3 = nano.buildSuggestionBoxForTest(suggestions, screenLines, 19, 0);
        assertNotNull(box3, "Box should be created when cursor is at the bottom");

        // 4. Test with scrolled view (firstLineToDisplay > 0)
        Nano.Box box4 = nano.buildSuggestionBoxForTest(suggestions, screenLines, 15, 10);
        assertNotNull(box4, "Box should be created when view is scrolled");

        // 5. Test with cursor at the top of a scrolled view
        Nano.Box box5 = nano.buildSuggestionBoxForTest(suggestions, screenLines, 10, 10);
        assertNotNull(box5, "Box should be created when cursor is at the top of a scrolled view");

        // 6. Test with cursor at the bottom of a scrolled view
        Nano.Box box6 = nano.buildSuggestionBoxForTest(suggestions, screenLines, 29, 10);
        assertNotNull(box6, "Box should be created when cursor is at the bottom of a scrolled view");

        // The test passes if all boxes are created successfully
        // Visual verification would require manual testing
        System.out.println("All suggestion boxes were created successfully");
    }
}
