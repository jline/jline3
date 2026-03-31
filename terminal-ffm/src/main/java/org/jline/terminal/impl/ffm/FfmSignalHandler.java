/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native signal handling via POSIX {@code sigaction()} using the Foreign Function &amp; Memory API.
 *
 * <p>This class replaces the reflection-based {@code sun.misc.Signal} approach with direct
 * {@code sigaction()} calls, providing:</p>
 * <ul>
 *   <li>No dependency on internal JVM APIs ({@code sun.misc.Signal})</li>
 *   <li>{@code SA_RESTART} flag to automatically restart interrupted system calls</li>
 *   <li>Arena-scoped handler lifetime</li>
 * </ul>
 *
 * <p><b>Async-signal-safety caveat:</b> The upcall stub used as the native signal handler
 * is not formally documented as async-signal-safe by the FFM specification. However, the
 * callback ({@link #signalReceived(int)}) performs only a single atomic store, minimizing
 * time spent in signal context. This is analogous to what {@code sun.misc.Signal} does
 * internally. All substantive Java work happens on the daemon dispatcher thread.</p>
 */
@SuppressWarnings("restricted")
class FfmSignalHandler {

    private static final Logger logger = Logger.getLogger("org.jline");

    // --- Shared string constants ---
    private static final String SA_HANDLER = "sa_handler";
    private static final String SA_MASK = "sa_mask";
    private static final String SA_FLAGS = "sa_flags";
    private static final String EXCEPTION_DETAILS = "Exception details";

    // --- Platform-specific signal constants ---
    private static final int SIGHUP;
    private static final int SIGINT;
    private static final int SIGQUIT;
    private static final int SIGTERM;
    private static final int SIGTSTP;
    private static final int SIGCONT;
    private static final int SIGINFO;
    private static final int SIGWINCH;
    private static final int SA_RESTART;

    // --- sigaction struct layout and field accessors ---
    private static final GroupLayout sigactionLayout;
    private static final VarHandle sa_handler_vh;
    private static final VarHandle sa_flags_vh;

    // --- FFM method handle for sigaction() ---
    private static final MethodHandle sigaction_mh;

    // --- Shared upcall stub for all signals ---
    private static final MemorySegment upcallStub;

    // --- Whether FFM signal handling is available on this platform ---
    private static final boolean available;

    static {
        boolean avail = false;
        GroupLayout layout = null;
        VarHandle saHandler = null;
        VarHandle saFlags = null;
        MethodHandle sigaction = null;
        MemorySegment stub = null;

        int sighup = -1;
        int sigint = -1;
        int sigquit = -1;
        int sigterm = -1;
        int sigtstp = -1;
        int sigcont = -1;
        int siginfo = -1;
        int sigwinch = -1;
        int saRestart = 0;

        try {
            String osName = System.getProperty("os.name");

            if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
                sighup = 1;
                sigint = 2;
                sigquit = 3;
                sigterm = 15;
                sigtstp = 18;
                sigcont = 19;
                siginfo = 29;
                sigwinch = 28;
                saRestart = 0x0002;

                layout = MemoryLayout.structLayout(
                        ValueLayout.ADDRESS.withName(SA_HANDLER),
                        ValueLayout.JAVA_INT.withName(SA_MASK),
                        ValueLayout.JAVA_INT.withName(SA_FLAGS));
            } else if (osName.startsWith("Linux")) {
                sighup = 1;
                sigint = 2;
                sigquit = 3;
                sigterm = 15;
                sigtstp = 20;
                sigcont = 18;
                siginfo = -1;
                sigwinch = 28;
                saRestart = 0x10000000;

                layout = MemoryLayout.structLayout(
                        ValueLayout.ADDRESS.withName(SA_HANDLER),
                        MemoryLayout.sequenceLayout(128, ValueLayout.JAVA_BYTE).withName(SA_MASK),
                        ValueLayout.JAVA_INT.withName(SA_FLAGS),
                        MemoryLayout.paddingLayout(4),
                        ValueLayout.ADDRESS.withName("sa_restorer"));
            } else if (osName.startsWith("FreeBSD")) {
                sighup = 1;
                sigint = 2;
                sigquit = 3;
                sigterm = 15;
                sigtstp = 18;
                sigcont = 19;
                siginfo = 29;
                sigwinch = 28;
                saRestart = 0x0002;

                layout = MemoryLayout.structLayout(
                        ValueLayout.ADDRESS.withName(SA_HANDLER),
                        ValueLayout.JAVA_INT.withName(SA_FLAGS),
                        MemoryLayout.sequenceLayout(16, ValueLayout.JAVA_BYTE).withName(SA_MASK),
                        MemoryLayout.paddingLayout(4));
            }

            if (layout != null) {
                saHandler =
                        FfmTerminalProvider.lookupVarHandle(layout, MemoryLayout.PathElement.groupElement(SA_HANDLER));
                saFlags = FfmTerminalProvider.lookupVarHandle(layout, MemoryLayout.PathElement.groupElement(SA_FLAGS));

                Linker linker = Linker.nativeLinker();
                SymbolLookup lookup = SymbolLookup.loaderLookup().or(linker.defaultLookup());

                Optional<MemorySegment> sigactionAddr = lookup.find("sigaction");
                if (sigactionAddr.isPresent()) {
                    sigaction = linker.downcallHandle(
                            sigactionAddr.get(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS));

                    stub = linker.upcallStub(
                            MethodHandles.lookup()
                                    .findStatic(
                                            FfmSignalHandler.class,
                                            "signalReceived",
                                            MethodType.methodType(void.class, int.class)),
                            FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT),
                            Arena.global());

                    avail = true;
                }
            }
        } catch (Exception | LinkageError t) {
            logger.log(Level.FINE, "FFM signal handler not available", t);
        }

        SIGHUP = sighup;
        SIGINT = sigint;
        SIGQUIT = sigquit;
        SIGTERM = sigterm;
        SIGTSTP = sigtstp;
        SIGCONT = sigcont;
        SIGINFO = siginfo;
        SIGWINCH = sigwinch;
        SA_RESTART = saRestart;

        sigactionLayout = layout;
        sa_handler_vh = saHandler;
        sa_flags_vh = saFlags;
        sigaction_mh = sigaction;
        upcallStub = stub;
        available = avail;
    }

    // --- Signal dispatch infrastructure ---

    /** Atomic flags: pendingSignals[signum] == 1 means a signal is pending dispatch. */
    private static final AtomicIntegerArray pendingSignals = new AtomicIntegerArray(64);

    /** Registered Java handlers, keyed by signal number. */
    private static final Map<Integer, Runnable> handlers = new ConcurrentHashMap<>();

    /** Dispatcher thread (started lazily on first registration). */
    private static volatile Thread dispatcherThread;

    /**
     * Token returned by {@link #register} for later use with {@link #unregister}.
     */
    record Registration(int signum, Arena arena, MemorySegment oldAction) {}

    // --- Public API ---

    /**
     * Returns whether FFM-based signal handling is available on this platform.
     */
    static boolean isAvailable() {
        return available;
    }

    /**
     * Registers a signal handler via {@code sigaction()} with {@code SA_RESTART}.
     *
     * @param name    signal name (e.g. "WINCH", "INT")
     * @param handler the Java callback
     * @return a {@link Registration} token, or {@code null} if the signal is unsupported
     */
    static Object register(String name, Runnable handler) {
        if (!available) {
            return null;
        }
        int signum = signalNumber(name);
        if (signum < 0) {
            return null;
        }

        ensureDispatcherStarted();

        Arena arena = Arena.ofShared();
        try {
            MemorySegment oldAct = arena.allocate(sigactionLayout);
            MemorySegment newAct = arena.allocate(sigactionLayout);
            sa_handler_vh.set(newAct, upcallStub);
            sa_flags_vh.set(newAct, SA_RESTART);

            int res = (int) sigaction_mh.invoke(signum, newAct, oldAct);
            if (res != 0) {
                logger.log(Level.FINE, "sigaction() failed for signal {0} (signum={1})", new Object[] {name, signum});
                arena.close();
                return null;
            }
            handlers.put(signum, handler);
            return new Registration(signum, arena, oldAct);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Error registering FFM signal handler for {0}", name);
            logger.log(Level.FINE, EXCEPTION_DETAILS, t);
            arena.close();
            return null;
        }
    }

    /**
     * Registers the default (SIG_DFL) handler for the specified signal.
     *
     * @param name signal name
     * @return a {@link Registration} token, or {@code null} if the signal is unsupported
     */
    static Object registerDefault(String name) {
        if (!available) {
            return null;
        }
        int signum = signalNumber(name);
        if (signum < 0) {
            return null;
        }

        Arena arena = Arena.ofShared();
        try {
            MemorySegment oldAct = arena.allocate(sigactionLayout);
            MemorySegment newAct = arena.allocate(sigactionLayout);
            // sa_handler = SIG_DFL (0) — already zero from allocate()
            // sa_flags and sa_mask also zero

            int res = (int) sigaction_mh.invoke(signum, newAct, oldAct);
            if (res != 0) {
                logger.log(Level.FINE, "sigaction(SIG_DFL) failed for signal {0}", name);
                arena.close();
                return null;
            }
            handlers.remove(signum);
            return new Registration(signum, arena, oldAct);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Error registering default handler for {0}", name);
            logger.log(Level.FINE, EXCEPTION_DETAILS, t);
            arena.close();
            return null;
        }
    }

    /**
     * Restores the previous signal handler that was in place before registration.
     *
     * @param name     signal name
     * @param previous the token returned by {@link #register} or {@link #registerDefault}
     */
    static void unregister(String name, Object previous) {
        if (!(previous instanceof Registration reg)) {
            return;
        }

        try {
            int res = (int) sigaction_mh.invoke(reg.signum(), reg.oldAction(), MemorySegment.NULL);
            if (res != 0) {
                logger.log(Level.FINE, "sigaction() restore failed for signal {0}", name);
                return;
            }
            handlers.remove(reg.signum());
        } catch (Throwable t) {
            logger.log(Level.FINE, "Error unregistering FFM signal handler for {0}", name);
            logger.log(Level.FINE, EXCEPTION_DETAILS, t);
        } finally {
            reg.arena().close();
        }
    }

    // --- Signal upcall target (called from native signal context) ---

    /**
     * Called from the native signal handler via the upcall stub.
     * Sets an atomic flag; the dispatcher thread will invoke the Java handler.
     */
    static void signalReceived(int signum) {
        if (signum >= 0 && signum < pendingSignals.length()) {
            pendingSignals.set(signum, 1);
        }
    }

    // --- Dispatcher thread ---

    private static synchronized void ensureDispatcherStarted() {
        if (dispatcherThread != null) {
            return;
        }
        Thread t = new Thread(FfmSignalHandler::dispatchLoop, "JLine-signal-dispatcher");
        t.setDaemon(true);
        t.start();
        dispatcherThread = t;
    }

    /**
     * Polls pending signal flags and dispatches to registered Java handlers.
     * Runs on a daemon thread; exits when interrupted.
     */
    private static void dispatchLoop() {
        while (!Thread.interrupted()) {
            boolean anyPending = false;
            for (int i = 0; i < pendingSignals.length(); i++) {
                if (pendingSignals.compareAndSet(i, 1, 0)) {
                    anyPending = true;
                    dispatchSignal(i);
                }
            }
            if (!anyPending) {
                LockSupport.parkNanos(1_000_000L); // 1 ms
            }
        }
    }

    /**
     * Dispatches a single pending signal to its registered handler.
     */
    private static void dispatchSignal(int signum) {
        Runnable handler = handlers.get(signum);
        if (handler != null) {
            try {
                handler.run();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in signal handler for signal {0}", signum);
                logger.log(Level.WARNING, EXCEPTION_DETAILS, e);
            }
        }
    }

    // --- Signal name → number mapping ---

    private static int signalNumber(String name) {
        return switch (name) {
            case "HUP" -> SIGHUP;
            case "INT" -> SIGINT;
            case "QUIT" -> SIGQUIT;
            case "TERM" -> SIGTERM;
            case "TSTP" -> SIGTSTP;
            case "CONT" -> SIGCONT;
            case "INFO" -> SIGINFO;
            case "WINCH" -> SIGWINCH;
            default -> -1;
        };
    }
}
