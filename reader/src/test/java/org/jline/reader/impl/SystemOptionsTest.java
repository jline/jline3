/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemOptionsTest {
    @Test
    public void testSystemOptions() {
        LineReader reader1 = LineReaderBuilder.builder().build();
        assertFalse(reader1.isSet(LineReader.Option.DISABLE_EVENT_EXPANSION));

        System.setProperty("org.jline.reader.props.disable-event-expansion", "on");
        try {
            LineReader reader2 = LineReaderBuilder.builder().build();
            assertTrue(reader2.isSet(LineReader.Option.DISABLE_EVENT_EXPANSION));
        } finally {
            System.clearProperty("org.jline.reader.props.disable-event-expansion");
        }
    }
}
