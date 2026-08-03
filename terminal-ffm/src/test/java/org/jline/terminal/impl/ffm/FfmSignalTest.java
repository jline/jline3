/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.Signals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("restricted")
class FfmSignalTest {

    /**
     * SIGWINCH is 28 on Linux, macOS, FreeBSD and AIX, and its default action is to ignore,
     * so a stray delivery cannot take down the surefire fork.
     */
    private static final int SIGWINCH = 28;

    /**
     * {@code sa_handler} is the first member of {@code struct sigaction} on every POSIX
     * platform, so reading it back does not require modelling the rest of the layout.
     * The buffer is simply made larger than any platform's struct.
     */
    private static final long SIGACTION_MAX_SIZE = 256;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIBC = LINKER.defaultLookup();

    private static final MethodHandle SIGACTION = LINKER.downcallHandle(
            LIBC.find("sigaction").orElseThrow(),
            FunctionDescriptor.of(
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private static final MethodHandle KILL = LINKER.downcallHandle(
            LIBC.find("kill").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

    /**
     * The provider must leave signal registration to the JVM instead of calling {@code sigaction()}
     * itself. Installing an FFM upcall stub as {@code sa_handler} crashes HotSpot whenever the
     * signal interrupts a thread that is not {@code _thread_in_native}.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void doesNotInstallASignalHandlerOfItsOwn() throws Throwable {
        long saHandlerViaSignals;
        Object registration = Signals.register("WINCH", () -> {});
        try {
            saHandlerViaSignals = currentSaHandlerAddress();
        } finally {
            Signals.unregister("WINCH", registration);
        }

        long saHandlerViaProvider;
        TerminalProvider provider = new FfmTerminalProvider();
        Object providerRegistration = provider.registerSignal("WINCH", () -> {});
        try {
            saHandlerViaProvider = currentSaHandlerAddress();
        } finally {
            provider.unregisterSignal("WINCH", providerRegistration);
        }

        assertEquals(
                saHandlerViaSignals, saHandlerViaProvider, "the FFM provider installed a signal handler of its own");
    }

    /**
     * A regression guard rather than a demonstration of the bug: this passes both before and after
     * the change, and only pins down that signals are still delivered once the provider stops
     * registering them itself.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void registeredHandlerReceivesTheSignal() throws Throwable {
        CountDownLatch received = new CountDownLatch(1);
        TerminalProvider provider = new FfmTerminalProvider();
        Object registration = provider.registerSignal("WINCH", received::countDown);
        try {
            int rc = (int) KILL.invoke((int) ProcessHandle.current().pid(), SIGWINCH);
            assertEquals(0, rc, "kill() failed");
            assertTrue(received.await(10, TimeUnit.SECONDS), "signal handler was not invoked");
        } finally {
            provider.unregisterSignal("WINCH", registration);
        }
    }

    private static long currentSaHandlerAddress() throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment current = arena.allocate(SIGACTION_MAX_SIZE, 8);
            current.fill((byte) 0);
            int rc = (int) SIGACTION.invoke(SIGWINCH, MemorySegment.NULL, current);
            assertEquals(0, rc, "sigaction() query failed");
            return current.get(ValueLayout.ADDRESS, 0).address();
        }
    }
}
