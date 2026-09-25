/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.TermiosMapping;
import org.jline.utils.OSUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FfmTest {

    @Test
    @DisabledOnOs(OS.WINDOWS) // non system terminals are not supported on windows
    void testNewTerminalWithNull() throws IOException {
        try (Terminal terminal = new FfmTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        false,
                        null,
                        null)) {
            assertNotNull(terminal);
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // non system terminals are not supported on windows
    void testNewTerminalNoNull() throws IOException {
        try (Terminal terminal = new FfmTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        false,
                        new Attributes(),
                        Size.of(0, 0))) {
            assertNotNull(terminal);
            assertNotNull(terminal.getSize());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testWinsizeConstructorArgumentOrder() {
        try (Arena arena = Arena.ofConfined()) {
            short cols = 120;
            short rows = 40;
            CLibrary.winsize ws = new CLibrary.winsize(arena, cols, rows);
            assertEquals(cols, ws.ws_col());
            assertEquals(rows, ws.ws_row());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void applyAttributesPreservesBaudAndSpeed() {
        try (Arena arena = Arena.ofConfined()) {
            CLibrary.termios t = new CLibrary.termios(arena);
            Attributes seed = new Attributes();
            seed.setControlFlag(ControlFlag.CS8, true);
            seed.setControlFlag(ControlFlag.CREAD, true);
            seed.setLocalFlag(LocalFlag.ECHO, true);
            t.apply(seed);

            TermiosMapping mapping = TermiosMapping.forCurrentPlatform();
            Attributes allControl = new Attributes();
            for (ControlFlag flag : ControlFlag.values()) {
                allControl.setControlFlag(flag, true);
            }
            long mappedCflag = mapping.toTermios(allControl).cflag();
            long baudBits = 0x000DL & ~mappedCflag;
            if (baudBits == 0) {
                baudBits = Long.lowestOneBit(~mappedCflag);
            }
            t.c_cflag(t.c_cflag() | baudBits);

            // Speed fields only exist on non-AIX platforms (macOS, Linux).
            // On AIX the c_ispeed/c_ospeed VarHandles are null and the
            // getters return 0 regardless of what is written.
            if (!OSUtils.IS_AIX) {
                t.c_ispeed(9600);
                t.c_ospeed(9600);
            }

            Attributes attr = t.asAttributes();
            attr.setLocalFlag(LocalFlag.ECHO, false);
            t.apply(attr);

            assertEquals(baudBits, t.c_cflag() & baudBits);
            if (!OSUtils.IS_AIX) {
                assertEquals(9600, t.c_ispeed());
                assertEquals(9600, t.c_ospeed());
            }
            Attributes applied = t.asAttributes();
            assertFalse(applied.getLocalFlag(LocalFlag.ECHO));
            assertTrue(applied.getControlFlag(ControlFlag.CS8));
            assertTrue(applied.getControlFlag(ControlFlag.CREAD));
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSignalHandlerAvailable() {
        // Verify isAvailable() is callable and returns a stable result.
        // On supported POSIX platforms (Linux, macOS, FreeBSD + x86_64/aarch64)
        // the handler is expected to work, but native initialization can legitimately
        // fail for environmental reasons (SELinux, sandboxed mmap/mprotect).
        // Each signal handler test guards individually with if (!isAvailable()) return.
        assertEquals(
                FfmSignalHandler.isAvailable(),
                FfmSignalHandler.isAvailable(),
                "isAvailable() must return a stable result across calls");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSignalRegisterUnregister() {
        if (!FfmSignalHandler.isAvailable()) {
            return;
        }

        // Register a handler for WINCH (safe signal that won't kill the process)
        Object reg = FfmSignalHandler.register("WINCH", () -> {});
        assertNotNull(reg, "Registration should succeed for WINCH");
        assertTrue(reg instanceof FfmSignalHandler.Registration, "Should return a Registration record");

        // Unregister — should restore previous handler without error
        FfmSignalHandler.unregister("WINCH", reg);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSignalRegisterDefault() {
        if (!FfmSignalHandler.isAvailable()) {
            return;
        }

        // Register a handler then reset to default
        Object reg1 = FfmSignalHandler.register("WINCH", () -> {});
        assertNotNull(reg1);

        Object reg2 = FfmSignalHandler.registerDefault("WINCH");
        assertNotNull(reg2, "registerDefault should succeed");

        // Restore original handler
        FfmSignalHandler.unregister("WINCH", reg2);
        FfmSignalHandler.unregister("WINCH", reg1);
    }

    @SuppressWarnings("restricted")
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSignalDeliveryViaSelfPipe() throws Throwable {
        if (!FfmSignalHandler.isAvailable()) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Object reg = FfmSignalHandler.register("WINCH", latch::countDown);
        assertNotNull(reg, "Registration should succeed");

        try {
            // Send SIGWINCH to ourselves via libc kill(2) through FFM — no subprocess needed,
            // portable to all POSIX systems without requiring bash.
            Linker linker = Linker.nativeLinker();
            SymbolLookup lookup = SymbolLookup.loaderLookup().or(linker.defaultLookup());
            MethodHandle killMh = linker.downcallHandle(
                    lookup.find("kill").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            int pid = (int) ProcessHandle.current().pid();
            int sigwinch = 28; // SIGWINCH on Linux, macOS, and FreeBSD
            int result = (int) killMh.invoke(pid, sigwinch);
            assertEquals(0, result, "kill() should succeed");

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Signal handler should have been called within 5s");
        } finally {
            FfmSignalHandler.unregister("WINCH", reg);
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testSignalProviderRoundTrip() {
        // Test the full FfmTerminalProvider signal registration path
        FfmTerminalProvider provider = new FfmTerminalProvider();
        Object reg = provider.registerSignal("WINCH", () -> {});
        assertNotNull(reg, "Provider-level signal registration should succeed");
        provider.unregisterSignal("WINCH", reg);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testSigDflTerminalDoesNotClobberSigquit() throws Exception {
        // Regression test for https://github.com/jline/jline3/issues/2262.
        // Calling registerDefault("QUIT") must not clobber HotSpot's SIGQUIT handler.
        //
        // We verify by checking /proc/self/status: SIGQUIT (signal 3) must remain in
        // the SigCgt (caught) bitmask after registerDefault("QUIT").
        assumeTrue(
                FfmSignalHandler.isAvailable(), "FFM signal handling not available on this platform — skipping test");
        long sigquitBit = 1L << (3 - 1); // SIGQUIT = 3, bits are 0-indexed from signal 1

        long caughtBefore = readSigCgt();
        assumeTrue((caughtBefore & sigquitBit) != 0, "HotSpot didn't catch SIGQUIT in this JVM — skipping test");

        // This is the call that AbstractUnixSysTerminal.registerNativeSignals() makes for SIG_DFL.
        // It must return null (declining to install SIG_DFL) so HotSpot's handler is preserved.
        Object reg = FfmSignalHandler.registerDefault("QUIT");
        assertNull(
                reg,
                "registerDefault(QUIT) must return null — installing SIG_DFL would clobber HotSpot's thread-dump handler");

        long caughtAfter = readSigCgt();
        assertNotEquals(
                0L,
                caughtAfter & sigquitBit,
                "SIGQUIT must remain in SigCgt after registerDefault(QUIT) — "
                        + "HotSpot's thread-dump handler must not be clobbered");
    }

    private static long readSigCgt() throws Exception {
        for (String line : Files.readString(Path.of("/proc/self/status")).split("\n")) {
            if (line.startsWith("SigCgt:")) {
                return Long.parseUnsignedLong(line.split(":")[1].strip(), 16);
            }
        }
        throw new AssertionError("SigCgt line not found in /proc/self/status");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void checkStructLayout() {
        try (Arena arena = Arena.ofConfined()) {
            assertNotNull(new Kernel32.KEY_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.MOUSE_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.WINDOW_BUFFER_SIZE_RECORD(arena));
            assertNotNull(new Kernel32.MENU_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.FOCUS_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.INPUT_RECORD(arena));
            assertNotNull(new Kernel32.SMALL_RECT(arena));
        }
    }
}
