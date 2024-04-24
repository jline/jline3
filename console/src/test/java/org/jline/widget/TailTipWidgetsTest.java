/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.widget;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Status;
import org.jline.widget.TailTipWidgets.TipType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class TailTipWidgetsTest {

    /** A simple extension of {@link DumbTerminal} doing the minimal amount of work so that a {@link Status} constructed
     * from it is "supported". */
    private static final class SupportedDumbTerminal extends DumbTerminal {
        private SupportedDumbTerminal() throws IOException {
            super(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
            strings.put(Capability.change_scroll_region, "");
            strings.put(Capability.save_cursor, "");
            strings.put(Capability.restore_cursor, "");
            strings.put(Capability.cursor_address, "");
        }
    }

    /** Subclass of {@link Status} exposing the {@code supported} field. For testing only. */
    private static final class TestStatus extends Status {
        private TestStatus(Terminal terminal) {
            super(terminal);
        }

        private boolean isSupported() {
            return supported;
        }
    }

    /** A dummy {@link LineReader} that's immediately resized to 0x0. */
    private static final class ZeroSizeLineReader extends LineReaderImpl {
        private ZeroSizeLineReader(Terminal terminal) throws IOException {
            super(terminal);
            display.resize(0, 0);
        }
    }

    @Test
    public void enableTest() throws Exception {
        Terminal terminal = new SupportedDumbTerminal();
        assertTrue(new TestStatus(terminal).isSupported());
        LineReader reader = new ZeroSizeLineReader(terminal);
        new TailTipWidgets(reader, __ -> null, 1, TipType.COMBINED).enable();
    }
}
