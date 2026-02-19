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

import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentDescriptionTest {

    @Test
    void nameOnly() {
        ArgumentDescription arg = new ArgumentDescription("file");
        assertEquals("file", arg.name());
        assertTrue(arg.description().isEmpty());
    }

    @Test
    void nameWithDescription() {
        ArgumentDescription arg = new ArgumentDescription("file", List.of(new AttributedString("the file to process")));
        assertEquals("file", arg.name());
        assertEquals(1, arg.description().size());
    }

    @Test
    void nameWithSpacesThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ArgumentDescription("bad name"));
    }

    @Test
    void nameWithTabsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ArgumentDescription("bad\tname"));
    }

    @Test
    void ofCreatesMultiple() {
        List<ArgumentDescription> args = ArgumentDescription.of("file", "pattern", "output");
        assertEquals(3, args.size());
        assertEquals("file", args.get(0).name());
        assertEquals("pattern", args.get(1).name());
        assertEquals("output", args.get(2).name());
    }
}
