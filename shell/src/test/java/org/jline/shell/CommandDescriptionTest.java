/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.List;
import java.util.regex.Pattern;

import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandDescriptionTest {

    @Test
    void builderCreatesImmutableDescription() {
        CommandDescription desc = CommandDescription.builder()
                .mainDescription(List.of(new AttributedString("test command")))
                .argument(new ArgumentDescription("file"))
                .option("-v --verbose", List.of(new AttributedString("verbose output")))
                .build();

        assertEquals(1, desc.mainDescription().size());
        assertEquals("test command", desc.mainDescription().get(0).toString());
        assertEquals(1, desc.arguments().size());
        assertEquals("file", desc.arguments().get(0).name());
        assertEquals(1, desc.options().size());
        assertTrue(desc.isValid());
        assertTrue(desc.isHighlighted());
        assertEquals(-1, desc.errorIndex());
        assertNull(desc.errorPattern());
    }

    @Test
    void builderDefaultsAreCorrect() {
        CommandDescription desc = CommandDescription.builder().build();
        assertTrue(desc.mainDescription().isEmpty());
        assertTrue(desc.arguments().isEmpty());
        assertTrue(desc.options().isEmpty());
        assertTrue(desc.isValid());
        assertTrue(desc.isHighlighted());
    }

    @Test
    void builderInvalidDescription() {
        CommandDescription desc = CommandDescription.builder().valid(false).build();
        assertFalse(desc.isValid());
    }

    @Test
    void builderErrorPatternAndIndex() {
        Pattern pattern = Pattern.compile("error.*");
        CommandDescription desc =
                CommandDescription.builder().errorPattern(pattern).errorIndex(3).build();
        assertSame(pattern, desc.errorPattern());
        assertEquals(3, desc.errorIndex());
    }

    @Test
    void descriptionIsImmutable() {
        CommandDescription desc = CommandDescription.builder()
                .mainDescription(List.of(new AttributedString("test")))
                .argument(new ArgumentDescription("arg1"))
                .option("-o", List.of(new AttributedString("opt")))
                .build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> desc.mainDescription().add(new AttributedString("x")));
        assertThrows(UnsupportedOperationException.class, () -> desc.arguments().add(new ArgumentDescription("x")));
        assertThrows(UnsupportedOperationException.class, () -> desc.options().put("x", List.of()));
    }
}
