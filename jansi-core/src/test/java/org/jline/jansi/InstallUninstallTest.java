/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

import org.jline.terminal.impl.DumbTerminalProvider;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InstallUninstallTest {

    @Test
    void testInstallUninstall() {
        AnsiConsole.systemInstall();
        try {
            assertNotNull(AnsiConsole.out());
            assertNotNull(AnsiConsole.err());
        } finally {
            AnsiConsole.systemUninstall();
        }
    }

    @Test
    void theTerminalsProviderDecidesWhetherTheOtherStreamIsATty() throws IOException {
        // A native Windows console gets ANSI only on the stream the terminal writes to.
        boolean ansiOnOtherTty =
                !AnsiConsole.IS_WINDOWS || AnsiConsole.IS_CYGWIN || AnsiConsole.IS_MSYSTEM || AnsiConsole.IS_CONEMU;
        assertEquals(ansiOnOtherTty ? AnsiType.Native : AnsiType.Redirected, stderrTypeWhenProviderSays(true));

        assertEquals(AnsiType.Redirected, stderrTypeWhenProviderSays(false));
    }

    private static AnsiType stderrTypeWhenProviderSays(boolean isTty) throws IOException {
        Set<SystemStream> asked = EnumSet.noneOf(SystemStream.class);
        TerminalProvider provider = new DumbTerminalProvider() {
            @Override
            public boolean isSystemStream(SystemStream stream) {
                asked.add(stream);
                return isTty;
            }
        };
        AnsiConsole.setTerminal(
                new LineDisciplineTerminal("test", "xterm", new ByteArrayOutputStream(), StandardCharsets.UTF_8) {
                    @Override
                    public TerminalProvider getProvider() {
                        return provider;
                    }

                    @Override
                    public SystemStream getSystemStream() {
                        return SystemStream.Output;
                    }
                });
        AnsiConsole.systemInstall();
        try {
            assertEquals(EnumSet.of(SystemStream.Error), asked);
            return AnsiConsole.err().getType();
        } finally {
            AnsiConsole.systemUninstall();
        }
    }
}
