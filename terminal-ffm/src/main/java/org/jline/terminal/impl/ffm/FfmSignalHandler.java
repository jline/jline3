/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native signal handling via POSIX {@code sigaction()} using the Foreign Function &amp; Memory API,
 * with the classic <em>self-pipe trick</em> to avoid running Java code in signal context.
 *
 * <h2>Problem</h2>
 * <p>A {@link java.lang.foreign.Linker#upcallStub upcall stub} used as {@code sa_handler} crashes
 * HotSpot with {@code guarantee(thread->thread_state() == _thread_in_native)} because the kernel
 * delivers the signal on whichever thread it interrupts — and that thread may not be in native
 * state when the stub prologue fires.</p>
 *
 * <h2>Solution: self-pipe trick</h2>
 * <p>Instead of an upcall stub, this class installs a tiny platform-specific <em>machine-code</em>
 * handler that performs a single async-signal-safe {@code write()} to a pipe via a raw syscall
 * instruction. No Java code runs in signal context whatsoever.</p>
 * <ol>
 *   <li>{@code pipe(2)} creates a notification channel.</li>
 *   <li>The signal handler (raw machine code allocated via {@code mmap}) writes the signal number
 *       as a single byte to the pipe's write end — this is async-signal-safe per POSIX.</li>
 *   <li>A daemon thread blocks on {@code read(pipe_read_fd)} and dispatches the registered Java
 *       handler in safe context.</li>
 * </ol>
 *
 * <p>This preserves all benefits of direct {@code sigaction()} without depending on
 * {@code sun.misc.Signal}:</p>
 * <ul>
 *   <li>No dependency on internal JVM APIs</li>
 *   <li>{@code SA_RESTART} is set on all handlers</li>
 *   <li>Full control over signal masks via {@code sigaction()}</li>
 *   <li>No upcall stubs — no JVM crash under any signal delivery scenario</li>
 * </ul>
 *
 * <p>Supported platforms: x86_64 and aarch64 on Linux, macOS, and FreeBSD.
 * Unsupported platforms (AIX/PPC64, etc.) fall back to {@code sun.misc.Signal} transparently.</p>
 *
 * @see <a href="https://github.com/jline/jline3/issues/2139">#2139 — JVM crash in upcall stub</a>
 * @see <a href="https://github.com/jline/jline3/issues/1747">#1747 — FFM signal handling motivation</a>
 */
@SuppressWarnings("restricted")
class FfmSignalHandler {

    private static final Logger logger = System.getLogger("org.jline");

    // --- Shared string constants ---
    private static final String SA_HANDLER = "sa_handler";
    private static final String SA_MASK = "sa_mask";
    private static final String SA_FLAGS = "sa_flags";
    private static final String EXCEPTION_DETAILS = "Exception details";

    // --- OS name prefixes ---
    private static final String OS_LINUX = "Linux";
    private static final String OS_DARWIN = "Darwin";
    private static final String OS_MAC = "Mac";
    private static final String OS_FREEBSD = "FreeBSD";
    private static final String OS_AIX = "AIX";

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

    // --- FFM method handles ---
    private static final MethodHandle sigaction_mh;
    private static final MethodHandle read_mh;
    private static final MethodHandle write_mh;
    private static final MethodHandle close_mh;

    // --- Self-pipe file descriptors (-1 if not created) ---
    private static final int PIPE_READ_FD;
    private static final int PIPE_WRITE_FD;

    // --- Native signal handler: executable machine code allocated via mmap ---
    private static final MemorySegment nativeHandlerCode;

    // --- Whether FFM signal handling is AVAILABLE on this platform ---
    private static final boolean AVAILABLE;

    // --- mmap constants ---
    private static final int PROT_READ = 0x01;
    private static final int PROT_WRITE = 0x02;
    private static final int PROT_EXEC = 0x04;
    private static final int MAP_PRIVATE = 0x02;

    // --- fcntl constants (same values on Linux, macOS, FreeBSD) ---
    private static final int F_SETFD = 2;
    private static final int FD_CLOEXEC = 1;
    private static final int F_SETFL = 4;

    static {
        boolean avail = false;
        GroupLayout layout = null;
        VarHandle saHandler = null;
        VarHandle saFlags = null;
        MethodHandle sigaction = null;
        MethodHandle readMh = null;
        MethodHandle writeMh = null;
        MethodHandle closeMh = null;
        int pipeFdRead = -1;
        int pipeFdWrite = -1;
        MemorySegment handlerCode = null;

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

            if (osName.startsWith(OS_MAC) || osName.startsWith(OS_DARWIN)) {
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
            } else if (osName.startsWith(OS_LINUX)) {
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
            } else if (osName.startsWith(OS_FREEBSD)) {
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
            } else if (osName.contains(OS_AIX)) {
                sighup = 1;
                sigint = 2;
                sigquit = 3;
                sigterm = 15;
                sigtstp = 18;
                sigcont = 19;
                siginfo = -1;
                sigwinch = 28;
                saRestart = 0x0008;

                layout = MemoryLayout.structLayout(
                        ValueLayout.ADDRESS.withName(SA_HANDLER),
                        MemoryLayout.sequenceLayout(8, ValueLayout.JAVA_BYTE).withName(SA_MASK),
                        ValueLayout.JAVA_INT.withName(SA_FLAGS),
                        MemoryLayout.paddingLayout(4));
            }

            if (layout != null) {
                saHandler =
                        FfmTerminalProvider.lookupVarHandle(layout, MemoryLayout.PathElement.groupElement(SA_HANDLER));
                saFlags = FfmTerminalProvider.lookupVarHandle(layout, MemoryLayout.PathElement.groupElement(SA_FLAGS));

                Linker linker = Linker.nativeLinker();
                SymbolLookup lookup = SymbolLookup.loaderLookup().or(linker.defaultLookup());

                Optional<MemorySegment> sigactionAddr = lookup.find("sigaction");
                Optional<MemorySegment> pipeAddr = lookup.find("pipe");
                Optional<MemorySegment> readAddr = lookup.find("read");
                Optional<MemorySegment> writeAddr = lookup.find("write");
                Optional<MemorySegment> closeAddr = lookup.find("close");
                Optional<MemorySegment> mmapAddr = lookup.find("mmap");
                Optional<MemorySegment> mprotectAddr = lookup.find("mprotect");
                Optional<MemorySegment> munmapAddr = lookup.find("munmap");
                Optional<MemorySegment> fcntlAddr = lookup.find("fcntl");

                if (sigactionAddr.isPresent()
                        && pipeAddr.isPresent()
                        && readAddr.isPresent()
                        && writeAddr.isPresent()
                        && closeAddr.isPresent()
                        && mmapAddr.isPresent()
                        && mprotectAddr.isPresent()
                        && munmapAddr.isPresent()
                        && fcntlAddr.isPresent()) {

                    sigaction = linker.downcallHandle(
                            sigactionAddr.get(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS));

                    MethodHandle pipeMh = linker.downcallHandle(
                            pipeAddr.get(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

                    readMh = linker.downcallHandle(
                            readAddr.get(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_LONG,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_LONG));

                    writeMh = linker.downcallHandle(
                            writeAddr.get(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_LONG,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_LONG));

                    closeMh = linker.downcallHandle(
                            closeAddr.get(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

                    MethodHandle mmapMh = linker.downcallHandle(
                            mmapAddr.get(),
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_LONG,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_LONG));

                    MethodHandle mprotectMh = linker.downcallHandle(
                            mprotectAddr.get(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_LONG,
                                    ValueLayout.JAVA_INT));

                    MethodHandle munmapMh = linker.downcallHandle(
                            munmapAddr.get(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

                    // fcntl(int fd, int cmd, int arg) — variadic, third arg is the first variadic
                    MethodHandle fcntlMh = linker.downcallHandle(
                            fcntlAddr.get(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT),
                            Linker.Option.firstVariadicArg(2));

                    // --- Step 1: Create pipe and set O_NONBLOCK on write end ---
                    try (Arena pipeArena = Arena.ofConfined()) {
                        MemorySegment pipeFds = pipeArena.allocate(ValueLayout.JAVA_INT, 2);
                        int pipeResult = (int) pipeMh.invoke(pipeFds);
                        if (pipeResult != 0) {
                            throw new IllegalStateException("pipe() failed with return code " + pipeResult);
                        }
                        pipeFdRead = pipeFds.getAtIndex(ValueLayout.JAVA_INT, 0);
                        pipeFdWrite = pipeFds.getAtIndex(ValueLayout.JAVA_INT, 1);
                    }

                    // Mark both ends close-on-exec so child processes do not inherit them.
                    // Failure is tolerated — the pipe still works, it just leaks fds into children
                    // spawned through non-JDK exec paths.
                    fcntlMh.invoke(pipeFdRead, F_SETFD, FD_CLOEXEC);
                    fcntlMh.invoke(pipeFdWrite, F_SETFD, FD_CLOEXEC);

                    // Make the write end non-blocking so the signal handler never stalls
                    // an interrupted thread when the pipe buffer is full.  Dropping a byte
                    // is harmless — signals coalesce, and the dispatcher only needs to know
                    // that *a* signal arrived, not how many.
                    // O_NONBLOCK: 0x0800 on Linux, 0x0004 on macOS/FreeBSD
                    int oNonblock = osName.startsWith(OS_LINUX) ? 0x0800 : 0x0004;
                    int fcntlResult = (int) fcntlMh.invoke(pipeFdWrite, F_SETFL, oNonblock);
                    if (fcntlResult != 0) {
                        // A blocking write end is unsafe for signal context — the handler
                        // could hang an interrupted thread once the pipe buffer fills.
                        closeMh.invoke(pipeFdRead);
                        closeMh.invoke(pipeFdWrite);
                        pipeFdRead = -1;
                        pipeFdWrite = -1;
                        throw new IllegalStateException(
                                "fcntl(F_SETFL, O_NONBLOCK) failed — blocking write end unsafe for signal handler");
                    }

                    // --- Step 2: Generate platform-specific signal handler machine code ---
                    byte[] codeBytes = generateSignalHandlerCode(pipeFdWrite, osName);
                    if (codeBytes == null) {
                        // Unsupported architecture — close pipe and bail out
                        closeMh.invoke(pipeFdRead);
                        closeMh.invoke(pipeFdWrite);
                        pipeFdRead = -1;
                        pipeFdWrite = -1;
                        throw new UnsupportedOperationException(
                                "No machine-code signal handler for " + System.getProperty("os.arch") + "/" + osName);
                    }

                    // --- Step 3: Allocate executable memory via mmap + mprotect ---
                    int mapAnonymous = osName.startsWith(OS_LINUX) ? 0x20 : 0x1000;
                    long codeSize = codeBytes.length;

                    MemorySegment mmapResult = (MemorySegment) mmapMh.invoke(
                            MemorySegment.NULL, codeSize, PROT_READ | PROT_WRITE, MAP_PRIVATE | mapAnonymous, -1, 0L);

                    // mmap returns MAP_FAILED ((void*)-1) on error
                    if (mmapResult.address() == -1L) {
                        closeMh.invoke(pipeFdRead);
                        closeMh.invoke(pipeFdWrite);
                        pipeFdRead = -1;
                        pipeFdWrite = -1;
                        throw new IllegalStateException("mmap() failed for executable signal handler");
                    }

                    // Reinterpret to a writable segment and copy machine code
                    MemorySegment codeSegment = mmapResult.reinterpret(codeSize);
                    MemorySegment.copy(
                            MemorySegment.ofArray(codeBytes),
                            ValueLayout.JAVA_BYTE,
                            0,
                            codeSegment,
                            ValueLayout.JAVA_BYTE,
                            0,
                            codeBytes.length);

                    // Make executable (remove write, add exec) — satisfies W^X on Apple Silicon
                    int mprotectResult = (int) mprotectMh.invoke(mmapResult, codeSize, PROT_READ | PROT_EXEC);
                    if (mprotectResult != 0) {
                        munmapMh.invoke(mmapResult, codeSize);
                        closeMh.invoke(pipeFdRead);
                        closeMh.invoke(pipeFdWrite);
                        pipeFdRead = -1;
                        pipeFdWrite = -1;
                        throw new IllegalStateException("mprotect(PROT_READ|PROT_EXEC) failed");
                    }

                    handlerCode = codeSegment;
                    avail = true;
                }
            }
        } catch (Throwable t) {
            logger.log(Level.DEBUG, "FFM signal handler not available", t);
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
        read_mh = readMh;
        write_mh = writeMh;
        close_mh = closeMh;
        PIPE_READ_FD = pipeFdRead;
        PIPE_WRITE_FD = pipeFdWrite;
        nativeHandlerCode = handlerCode;
        AVAILABLE = avail;
    }

    // ===================================================================================
    // Machine-code generation
    // ===================================================================================

    /**
     * Generates a tiny native signal handler in raw machine code for the current platform.
     *
     * <p>The generated handler performs a single async-signal-safe operation:
     * {@code write(writeFd, &signum_byte, 1)} via a raw syscall instruction. No Java code,
     * no libc function calls, no errno side-effects. This is the minimum required to
     * implement the self-pipe trick safely from signal context.</p>
     *
     * <p>Supported: x86_64 and aarch64 on Linux, macOS, and FreeBSD.</p>
     *
     * @param writeFd the write end of the self-pipe (must fit in 16 bits for aarch64, 32 bits for x86_64)
     * @param osName  the value of {@code os.name}
     * @return the machine code bytes, or {@code null} if the current architecture is unsupported
     */
    @SuppressWarnings("java:S1168") // null signals "unsupported platform" — empty array would be invalid machine code
    private static byte[] generateSignalHandlerCode(int writeFd, String osName) {
        if (Boolean.getBoolean("jline.ffm.forceUnsupportedArch")) {
            return null;
        }
        String arch = System.getProperty("os.arch");
        if ("amd64".equals(arch) || "x86_64".equals(arch)) {
            return generateX86_64Code(writeFd, osName);
        } else if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            return generateAarch64Code(writeFd, osName);
        }
        return null;
    }

    /**
     * x86_64 signal handler machine code.
     *
     * <pre>
     *   sub rsp, 8          ; allocate stack space
     *   mov [rsp], dil      ; store signum byte (1st arg, SysV ABI) on stack
     *   mov eax, SYS_write  ; syscall number
     *   mov edi, writeFd    ; pipe fd
     *   mov rsi, rsp        ; buf = &signum_byte on stack
     *   mov edx, 1          ; count = 1
     *   syscall
     *   add rsp, 8          ; restore stack
     *   ret
     * </pre>
     */
    @SuppressWarnings({
        "java:S100", // x86_64 is the canonical architecture name
        "java:S1168" // null signals "unsupported OS" — empty array would be invalid machine code
    })
    private static byte[] generateX86_64Code(int writeFd, String osName) {
        int sysWrite;
        if (osName.startsWith(OS_LINUX)) {
            sysWrite = 1;
        } else if (osName.startsWith(OS_MAC) || osName.startsWith(OS_DARWIN)) {
            sysWrite = 0x02000004;
        } else if (osName.startsWith(OS_FREEBSD)) {
            sysWrite = 4;
        } else {
            return null;
        }

        byte[] code = new byte[] {
            // sub rsp, 8
            0x48,
            (byte) 0x83,
            (byte) 0xEC,
            0x08,
            // mov [rsp], dil
            0x40,
            (byte) 0x88,
            0x3C,
            0x24,
            // mov eax, <sysWrite>           (patched at offset 9..12)
            (byte) 0xB8,
            0x00,
            0x00,
            0x00,
            0x00,
            // mov edi, <writeFd>            (patched at offset 14..17)
            (byte) 0xBF,
            0x00,
            0x00,
            0x00,
            0x00,
            // mov rsi, rsp
            0x48,
            (byte) 0x89,
            (byte) 0xE6,
            // mov edx, 1
            (byte) 0xBA,
            0x01,
            0x00,
            0x00,
            0x00,
            // syscall
            0x0F,
            0x05,
            // add rsp, 8
            0x48,
            (byte) 0x83,
            (byte) 0xC4,
            0x08,
            // ret
            (byte) 0xC3,
        };

        // Patch syscall number at offset 9 (little-endian int32)
        patchInt32LE(code, 9, sysWrite);
        // Patch pipe fd at offset 14 (little-endian int32)
        patchInt32LE(code, 14, writeFd);
        return code;
    }

    /**
     * AArch64 signal handler machine code.
     *
     * <p>Linux and FreeBSD use x8 for the syscall number and {@code svc #0}.
     * macOS uses x16 and {@code svc #0x80}.</p>
     *
     * <pre>
     *   sub sp, sp, #16     ; allocate 16 bytes (AArch64 requires 16-byte SP alignment)
     *   strb w0, [sp]       ; store signum byte (1st arg, AAPCS64) on stack
     *   mov xN, #SYS_write  ; syscall number (x8 Linux/FreeBSD, x16 macOS)
     *   mov w0, #writeFd    ; pipe fd
     *   mov x1, sp          ; buf = &signum_byte on stack
     *   mov x2, #1          ; count = 1
     *   svc #IMM            ; syscall (#0 Linux/FreeBSD, #0x80 macOS)
     *   add sp, sp, #16     ; restore stack
     *   ret
     * </pre>
     */
    @SuppressWarnings("java:S1168") // null signals "unsupported platform" — empty array would be invalid machine code
    private static byte[] generateAarch64Code(int writeFd, String osName) {
        if (writeFd < 0 || writeFd > 65535) {
            // fd must fit in MOVZ imm16 field
            return null;
        }

        boolean isMacOS = osName.startsWith(OS_MAC) || osName.startsWith(OS_DARWIN);
        boolean isLinux = osName.startsWith(OS_LINUX);
        boolean isFreeBSD = osName.startsWith(OS_FREEBSD);
        if (!isMacOS && !isLinux && !isFreeBSD) {
            return null;
        }

        // MOVZ encoding: instruction = base | (imm16 << 5) | Rd
        // SUB SP, SP, #16
        int insnSubSp = 0xD10043FF;
        // STRB W0, [SP]
        int insnStrbW0 = 0x390003E0;
        // Syscall number register + value
        int insnSyscallNr;
        if (isMacOS) {
            // MOV X16, #4 (SYS_write on macOS, uses x16)
            insnSyscallNr = 0xD2800000 | (4 << 5) | 16;
        } else if (isLinux) {
            // MOV X8, #64 (SYS_write on Linux)
            insnSyscallNr = 0xD2800000 | (64 << 5) | 8;
        } else {
            // MOV X8, #4 (SYS_write on FreeBSD)
            insnSyscallNr = 0xD2800000 | (4 << 5) | 8;
        }
        // MOV W0, #writeFd (MOVZ W0, #imm16)
        int insnMovFd = 0x52800000 | (writeFd << 5);
        // MOV X1, SP (ADD X1, SP, #0)
        int insnMovX1Sp = 0x910003E1;
        // MOV X2, #1 (MOVZ X2, #1)
        int insnMovX2One = 0xD2800000 | (1 << 5) | 2;
        // SVC instruction
        int insnSvc = isMacOS ? 0xD4001001 : 0xD4000001;
        // ADD SP, SP, #16
        int insnAddSp = 0x910043FF;
        // RET
        int insnRet = 0xD65F03C0;

        int[] instructions = {
            insnSubSp, insnStrbW0, insnSyscallNr, insnMovFd, insnMovX1Sp, insnMovX2One, insnSvc, insnAddSp, insnRet
        };

        byte[] code = new byte[instructions.length * 4];
        for (int i = 0; i < instructions.length; i++) {
            patchInt32LE(code, i * 4, instructions[i]);
        }
        return code;
    }

    /** Write a 32-bit integer in little-endian byte order at the given offset. */
    private static void patchInt32LE(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    // ===================================================================================
    // Signal dispatch infrastructure
    // ===================================================================================

    /** Registered Java handlers, keyed by signal number. */
    private static final Map<Integer, Runnable> handlers = new ConcurrentHashMap<>();

    /** Dispatcher thread (started lazily on first registration). */
    private static volatile Thread dispatcherThread;

    /** Flag to control dispatcher thread lifecycle. */
    private static volatile boolean dispatcherRunning;

    /**
     * Generation counter for dispatcher threads. Incremented each time a new dispatcher is
     * started. Each dispatcher loop captures its generation at start and exits if the global
     * generation has moved on — this prevents a stale dispatcher from continuing to run
     * after {@code stopDispatcherIfIdle()} retires it and {@code ensureDispatcherStarted()}
     * launches a replacement before the old thread has consumed the sentinel byte.
     */
    private static volatile int dispatcherGeneration;

    /**
     * Token returned by {@link #register} for later use with {@link #unregister}.
     */
    record Registration(int signum, MemorySegment oldAction, Runnable previousHandler) {}

    // ===================================================================================
    // Public API
    // ===================================================================================

    /**
     * Indicates whether native POSIX signal handling via the self-pipe FFM approach is
     * supported and fully initialized on this platform.
     *
     * @return {@code true} if FFM-based signal handling is available, {@code false} otherwise
     */
    static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Installs the given Java Runnable as the handler for the named POSIX signal using
     * {@code sigaction} with {@code SA_RESTART}. The native handler is a minimal machine-code
     * stub that writes the signal number to a pipe; a daemon thread dispatches it to the
     * registered Java handler.
     *
     * @param name    signal name (e.g. "WINCH", "INT")
     * @param handler the Java callback to invoke when the signal is dispatched
     * @return a {@link Registration} token for later restoration, or {@code null} if FFM
     *         support is unavailable or the signal name is unsupported
     */
    static Object register(String name, Runnable handler) {
        if (!AVAILABLE) {
            return null;
        }
        int signum = signalNumber(name);
        if (signum < 0) {
            return null;
        }

        // Install the Java handler before sigaction() so that signals arriving
        // immediately after the native install are not lost.
        Runnable previousHandler = handlers.put(signum, handler);

        Arena arena = Arena.ofAuto();
        MemorySegment oldAct = null;
        boolean nativeInstalled = false;
        try {
            oldAct = arena.allocate(sigactionLayout);
            MemorySegment newAct = arena.allocate(sigactionLayout);
            sa_handler_vh.set(newAct, nativeHandlerCode);
            sa_flags_vh.set(newAct, SA_RESTART);

            int res = (int) sigaction_mh.invoke(signum, newAct, oldAct);
            if (res != 0) {
                logger.log(Level.DEBUG, "sigaction() failed for signal {0} (signum={1})", new Object[] {name, signum});
                restoreHandler(signum, previousHandler);
                return null;
            }
            nativeInstalled = true;
            ensureDispatcherStarted();
            // Only preserve previousHandler in Registration when the old native
            // disposition was our handler code (otherwise it belongs to an external handler)
            Runnable saved = isOurHandler(oldAct) ? previousHandler : null;
            return new Registration(signum, oldAct, saved);
        } catch (Throwable t) {
            logger.log(Level.DEBUG, "Error registering FFM signal handler for {0}", name);
            logger.log(Level.DEBUG, EXCEPTION_DETAILS, t);
            if (nativeInstalled) {
                bestEffortRestore(signum, oldAct);
            }
            restoreHandler(signum, previousHandler);
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
        if (!AVAILABLE) {
            return null;
        }
        int signum = signalNumber(name);
        if (signum < 0) {
            return null;
        }

        Arena arena = Arena.ofAuto();
        try {
            MemorySegment oldAct = arena.allocate(sigactionLayout);
            MemorySegment newAct = arena.allocate(sigactionLayout);
            // sa_handler = SIG_DFL (0) — already zero from allocate()
            // sa_flags and sa_mask also zero

            int res = (int) sigaction_mh.invoke(signum, newAct, oldAct);
            if (res != 0) {
                logger.log(Level.DEBUG, "sigaction(SIG_DFL) failed for signal {0}", name);
                return null;
            }
            // Save the previous Java handler in case unregister() needs to restore it
            Runnable previousHandler = handlers.remove(signum);
            stopDispatcherIfIdle();
            return new Registration(signum, oldAct, previousHandler);
        } catch (Throwable t) {
            logger.log(Level.DEBUG, "Error registering default handler for {0}", name);
            logger.log(Level.DEBUG, EXCEPTION_DETAILS, t);
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
                logger.log(Level.DEBUG, "sigaction() restore failed for signal {0}", name);
                return;
            }
            // Preserve the previous Java Runnable when the restored native disposition is our handler
            if (isOurHandler(reg.oldAction()) && reg.previousHandler() != null) {
                handlers.put(reg.signum(), reg.previousHandler());
                ensureDispatcherStarted();
            } else {
                handlers.remove(reg.signum());
                stopDispatcherIfIdle();
            }
        } catch (Throwable t) {
            logger.log(Level.DEBUG, "Error unregistering FFM signal handler for {0}", name);
            logger.log(Level.DEBUG, EXCEPTION_DETAILS, t);
        }
    }

    // ===================================================================================
    // Dispatcher thread — reads signal bytes from the pipe in safe Java context
    // ===================================================================================

    /**
     * Starts the signal dispatcher daemon thread if one is not already running.
     *
     * <p>The dispatcher blocks on {@code read(pipe_read_fd)} waiting for signal bytes
     * written by the native machine-code handler.</p>
     */
    private static synchronized void ensureDispatcherStarted() {
        if (dispatcherThread != null && dispatcherThread.isAlive()) {
            return;
        }
        dispatcherRunning = true;
        int gen = ++dispatcherGeneration;
        Thread t = new Thread(() -> dispatchLoop(gen), "JLine-signal-dispatcher");
        t.setDaemon(true);
        t.start();
        dispatcherThread = t;
    }

    /**
     * Stops the dispatcher thread when there are no registered signal handlers.
     *
     * <p>Writes a sentinel byte (0) to the pipe to wake up the blocked {@code read()} call.</p>
     */
    private static synchronized void stopDispatcherIfIdle() {
        if (handlers.isEmpty() && dispatcherThread != null) {
            dispatcherRunning = false;
            wakeUpDispatcher();
            dispatcherThread = null;
        }
    }

    /**
     * Writes a zero byte (sentinel) to the pipe to wake up the dispatcher thread.
     * Signal number 0 is not a deliverable POSIX signal, so the dispatcher ignores it.
     */
    private static void wakeUpDispatcher() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(ValueLayout.JAVA_BYTE);
            buf.set(ValueLayout.JAVA_BYTE, 0, (byte) 0);
            write_mh.invoke(PIPE_WRITE_FD, buf, 1L);
        } catch (Throwable t) {
            logger.log(Level.DEBUG, "Failed to write sentinel to signal pipe", t);
        }
    }

    /**
     * Dispatcher loop: blocks on reading single bytes from the pipe. Each byte is a signal
     * number written by the native machine-code handler. Dispatches to the registered Java
     * handler. Exits when {@link #dispatcherRunning} is set to {@code false} and a sentinel
     * byte wakes up the read, or when the generation has moved on (meaning this dispatcher
     * was retired and replaced).
     *
     * @param myGeneration the generation counter captured at thread creation
     */
    private static void dispatchLoop(int myGeneration) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(ValueLayout.JAVA_BYTE);
            while (dispatcherRunning && dispatcherGeneration == myGeneration) {
                try {
                    if (!readAndDispatch(buf)) {
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Reads one byte from the signal pipe and dispatches it if it is a valid signal number.
     *
     * <p>Returns {@code true} if the loop should continue, {@code false} to exit (EOF, shutdown,
     * or persistent error while stopping).</p>
     *
     * @param buf a single-byte buffer owned by the caller's arena
     * @return {@code true} to keep reading, {@code false} to exit the dispatch loop
     * @throws InterruptedException if the backoff sleep is interrupted
     */
    @SuppressWarnings("java:S1181") // MethodHandle.invoke throws Throwable
    private static boolean readAndDispatch(MemorySegment buf) throws InterruptedException {
        try {
            long n = (long) read_mh.invoke(PIPE_READ_FD, buf, 1L);
            if (n == 0) {
                // EOF — pipe write end closed; nothing more to read
                return false;
            }
            if (n < 0) {
                // EINTR or persistent error — back off briefly to avoid CPU spin,
                // then retry (EINTR is the common case; a genuine error eventually
                // resolves or the dispatcher is stopped externally)
                if (dispatcherRunning) {
                    Thread.sleep(10);
                }
                return true;
            }
            int signum = buf.get(ValueLayout.JAVA_BYTE, 0) & 0xFF;
            if (signum > 0) {
                dispatchSignal(signum);
            }
            // signum == 0 is the wakeup sentinel — caller re-checks dispatcherRunning
            return true;
        } catch (InterruptedException e) {
            throw e; // propagate to caller for clean shutdown
        } catch (Throwable t) {
            if (!dispatcherRunning) {
                return false;
            }
            logger.log(Level.DEBUG, "Error reading from signal pipe", t);
            return true;
        }
    }

    /**
     * Invoke the registered Java handler for the given signal, if one exists.
     *
     * @param signum the POSIX signal number to dispatch
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

    // ===================================================================================
    // Internal helpers
    // ===================================================================================

    /**
     * Best-effort restore of the native signal handler via sigaction.
     */
    @SuppressWarnings("java:S1181") // MethodHandle.invoke throws Throwable
    private static void bestEffortRestore(int signum, MemorySegment oldAct) {
        try {
            sigaction_mh.invoke(signum, oldAct, MemorySegment.NULL);
        } catch (Throwable ignore) {
            // best-effort native handler restore
        }
    }

    /**
     * Restore or remove the Java handler mapping for the given signal number.
     */
    private static void restoreHandler(int signum, Runnable previousHandler) {
        if (previousHandler != null) {
            handlers.put(signum, previousHandler);
        } else {
            handlers.remove(signum);
        }
    }

    /**
     * Determine whether the native sigaction struct's {@code sa_handler} field points to this
     * class's machine-code signal handler.
     *
     * @param sigactionStruct a native {@code struct sigaction} memory segment
     * @return {@code true} if {@code sa_handler} matches our handler code address
     */
    private static boolean isOurHandler(MemorySegment sigactionStruct) {
        MemorySegment handler = (MemorySegment) sa_handler_vh.get(sigactionStruct);
        return handler.address() == nativeHandlerCode.address();
    }

    /**
     * Map a POSIX signal short name to its platform-specific signal number.
     *
     * @param name the short signal name (e.g., "INT", "HUP", "WINCH")
     * @return the platform signal number, or {@code -1} if not recognized
     */
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
