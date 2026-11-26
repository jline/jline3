/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.impl.DeferredTerminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.impl.DumbTerminalProvider;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InstallUninstallTest {

    @Test
    void stripsAnsiFromADeferredDumbTerminal() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DeferredTerminal terminal = new DeferredTerminal();
        terminal.complete(new DumbTerminal(
                new DumbTerminalProvider(),
                SystemStream.Output,
                "test",
                "dumb",
                new ByteArrayInputStream(new byte[0]),
                output,
                StandardCharsets.UTF_8,
                SignalHandler.SIG_DFL));
        AnsiConsole.setTerminal(terminal);
        try {
            AnsiConsole.systemInstall();
            assertEquals(AnsiType.Unsupported, AnsiConsole.out().getType());
            AnsiConsole.out().print("\033[31mdeferred output\033[0m");
            AnsiConsole.out().flush();
        } finally {
            if (AnsiConsole.isInstalled()) {
                AnsiConsole.systemUninstall();
            } else {
                AnsiConsole.setTerminal(null);
                terminal.close();
            }
        }
        assertEquals("deferred output", output.toString(StandardCharsets.UTF_8));
    }

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
