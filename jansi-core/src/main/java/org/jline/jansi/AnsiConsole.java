/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.jline.jansi.io.AnsiOutputStream;
import org.jline.jansi.io.AnsiProcessor;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.spi.TerminalExt;
import org.jline.utils.OSUtils;

/**
 * Provides consistent access to an ANSI aware console PrintStream or an ANSI codes stripping PrintStream
 * if not on a terminal (see
 * <a href="http://fusesource.github.io/jansi/documentation/native-api/index.html?org/fusesource/jansi/internal/CLibrary.html">Jansi native
 * CLibrary isatty(int)</a>).
 * <p>The native library used is named <code>jansi</code> and is loaded using <a href="http://fusesource.github.io/hawtjni/">HawtJNI</a> Runtime
 * <a href="http://fusesource.github.io/hawtjni/documentation/api/index.html?org/fusesource/hawtjni/runtime/Library.html"><code>Library</code></a>
 *
 * @since 1.0
 * @see #systemInstall()
 * @see #out()
 * @see #err()
 * @see #ansiStream(boolean) for more details on ANSI mode selection
 */
public class AnsiConsole {

    /**
     * The default mode which Jansi will use, can be either <code>force</code>, <code>strip</code>
     * or <code>default</code> (the default).
     * If this property is set, it will override <code>jansi.passthrough</code>,
     * <code>jansi.strip</code> and <code>jansi.force</code> properties.
     */
    public static final String JANSI_MODE = "jansi.mode";
    /**
     * Jansi mode specific to the standard output stream.
     */
    public static final String JANSI_OUT_MODE = "jansi.out.mode";
    /**
     * Jansi mode specific to the standard error stream.
     */
    public static final String JANSI_ERR_MODE = "jansi.err.mode";

    /**
     * Jansi mode value to strip all ansi sequences.
     */
    public static final String JANSI_MODE_STRIP = "strip";
    /**
     * Jansi mode value to force ansi sequences to the stream even if it's not a terminal.
     */
    public static final String JANSI_MODE_FORCE = "force";
    /**
     * Jansi mode value that output sequences if on a terminal, else strip them.
     */
    public static final String JANSI_MODE_DEFAULT = "default";

    /**
     * The default color support that Jansi will use, can be either <code>16</code>,
     * <code>256</code> or <code>truecolor</code>.  If not set, JANSI will try to
     * autodetect the number of colors supported by the terminal by checking the
     * <code>COLORTERM</code> and <code>TERM</code> variables.
     */
    public static final String JANSI_COLORS = "jansi.colors";
    /**
     * Jansi colors specific to the standard output stream.
     */
    public static final String JANSI_OUT_COLORS = "jansi.out.colors";
    /**
     * Jansi colors specific to the standard error stream.
     */
    public static final String JANSI_ERR_COLORS = "jansi.err.colors";

    /**
     * Force the use of 16 colors. When using a 256-indexed color, or an RGB
     * color, the color will be rounded to the nearest one from the 16 palette.
     */
    public static final String JANSI_COLORS_16 = "16";
    /**
     * Force the use of 256 colors. When using an RGB color, the color will be
     * rounded to the nearest one from the standard 256 palette.
     */
    public static final String JANSI_COLORS_256 = "256";
    /**
     * Force the use of 24-bit colors.
     */
    public static final String JANSI_COLORS_TRUECOLOR = "truecolor";

    /**
     * If the <code>jansi.noreset</code> system property is set to true, the attributes won't be
     * reset when the streams are uninstalled.
     */
    public static final String JANSI_NORESET = "jansi.noreset";
    /**
     * If the <code>jansi.graceful</code> system property is set to false, any exception that occurs
     * during the initialization will cause the library to report this exception and fail. The default
     * behavior is to behave gracefully and fall back to pure emulation on posix systems.
     */
    public static final String JANSI_GRACEFUL = "jansi.graceful";

    /**
     * The {@code jansi.providers} system property can be set to control which internal provider
     * will be used.  If this property is not set, the {@code ffm} provider will be used if available,
     * else the {@code jni} one will be used.  If set, this property is interpreted as a comma
     * separated list of provider names to try in order.
     */
    public static final String JANSI_PROVIDERS = "jansi.providers";
    /**
     * The name of the {@code jni} provider.
     */
    public static final String JANSI_PROVIDER_JNI = "jni";
    /**
     * The name of the {@code ffm} provider.
     */
    public static final String JANSI_PROVIDER_FFM = "ffm";
    /**
     * The name of the {@code native-image} provider.
     * <p>This provider uses the
     * <a href="https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/C-API/">Native Image C API</a>
     * to call native functions, so it is only available when building to native image.
     * Additionally, this provider currently does not support Windows.
     * <p>Note: This is not the only provider available on Native Image,
     * and it is usually recommended to use ffm or jni provider.
     * This provider is mainly used when building static native images linked to musl libc.
     */
    public static final String JANSI_PROVIDER_NATIVE_IMAGE = "native-image";

    private static final PrintStream system_out = System.out;
    private static PrintStream out;
    private static final PrintStream system_err = System.err;
    private static PrintStream err;

    /**
     * Try to find the width of the console for this process.
     * Both output and error streams will be checked to determine the width.
     * A value of 0 is returned if the width can not be determined.
     * @since 2.2
     */
    public static int getTerminalWidth() {
        int w = out().getTerminalWidth();
        if (w <= 0) {
            w = err().getTerminalWidth();
        }
        return w;
    }

    static final boolean IS_WINDOWS = OSUtils.IS_WINDOWS;

    static final boolean IS_CYGWIN =
            IS_WINDOWS && System.getenv("PWD") != null && System.getenv("PWD").startsWith("/");

    static final boolean IS_MSYSTEM = IS_WINDOWS
            && System.getenv("MSYSTEM") != null
            && (System.getenv("MSYSTEM").startsWith("MINGW")
                    || System.getenv("MSYSTEM").equals("MSYS"));

    static final boolean IS_CONEMU = IS_WINDOWS && System.getenv("ConEmuPID") != null;

    static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    static int STDOUT_FILENO = 1;

    static int STDERR_FILENO = 2;

    private static int installed;
    static Terminal terminal;

    private AnsiConsole() {}

    public static Terminal getTerminal() {
        return terminal;
    }

    public static void setTerminal(Terminal terminal) {
        AnsiConsole.terminal = terminal;
    }

    /**
     * Initialize the out/err ansi-enabled streams
     */
    static synchronized void doInstall() {
        try {
            if (terminal == null) {
                TerminalBuilder builder = TerminalBuilder.builder()
                        .system(true)
                        .name("jansi")
                        .providers(System.getProperty(JANSI_PROVIDERS));
                String graceful = System.getProperty(JANSI_GRACEFUL);
                if (graceful != null) {
                    builder.dumb(Boolean.parseBoolean(graceful));
                }
                terminal = builder.build();
            }
            if (out == null) {
                out = ansiStream(true);
                err = ansiStream(false);
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    static synchronized void doUninstall() {
        try {
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            throw new IOError(e);
        } finally {
            terminal = null;
            out = null;
            err = null;
        }
    }

    private static AnsiPrintStream ansiStream(boolean stdout) throws IOException {
        final OutputStream out;
        final AnsiOutputStream.WidthSupplier width;
        final AnsiProcessor processor = null;
        final AnsiType type;
        final AnsiOutputStream.IoRunnable installer = null;
        final AnsiOutputStream.IoRunnable uninstaller = null;

        out = terminal.output();
        width = terminal::getWidth;
        type = terminal instanceof DumbTerminal
                ? AnsiType.Unsupported
                : ((TerminalExt) terminal).getSystemStream() != null ? AnsiType.Native : AnsiType.Redirected;

        AnsiMode mode;

        // If the jansi.mode property is set, use it
        String jansiMode = System.getProperty(stdout ? JANSI_OUT_MODE : JANSI_ERR_MODE, System.getProperty(JANSI_MODE));
        if (JANSI_MODE_FORCE.equals(jansiMode)) {
            mode = AnsiMode.Force;
        } else if (JANSI_MODE_STRIP.equals(jansiMode)) {
            mode = AnsiMode.Strip;
        } else {
            mode = type == AnsiType.Native ? AnsiMode.Default : AnsiMode.Strip;
        }

        AnsiColors colors;

        String colorterm, term;
        // If the jansi.colors property is set, use it
        String jansiColors =
                System.getProperty(stdout ? JANSI_OUT_COLORS : JANSI_ERR_COLORS, System.getProperty(JANSI_COLORS));
        if (JANSI_COLORS_TRUECOLOR.equals(jansiColors)) {
            colors = AnsiColors.TrueColor;
        } else if (JANSI_COLORS_256.equals(jansiColors)) {
            colors = AnsiColors.Colors256;
        } else if (jansiColors != null) {
            colors = AnsiColors.Colors16;
        }

        // If the COLORTERM env variable contains "truecolor" or "24bit", assume true color support
        // see https://gist.github.com/XVilka/8346728#true-color-detection
        else if ((colorterm = System.getenv("COLORTERM")) != null
                && (colorterm.contains("truecolor") || colorterm.contains("24bit"))) {
            colors = AnsiColors.TrueColor;
        }

        // check the if TERM contains -direct
        else if ((term = System.getenv("TERM")) != null && term.contains("-direct")) {
            colors = AnsiColors.TrueColor;
        }

        // check the if TERM contains -256color
        else if (term != null && term.contains("-256color")) {
            colors = AnsiColors.Colors256;
        }

        // else defaults to 16 colors
        else {
            colors = AnsiColors.Colors16;
        }

        // If the jansi.noreset property is not set, reset the attributes
        // when the stream is closed
        boolean resetAtUninstall = type != AnsiType.Unsupported && !getBoolean(JANSI_NORESET);

        return newPrintStream(
                new AnsiOutputStream(
                        out,
                        width,
                        mode,
                        processor,
                        type,
                        colors,
                        terminal.encoding(),
                        installer,
                        uninstaller,
                        resetAtUninstall),
                terminal.encoding().name());
    }

    private static AnsiPrintStream newPrintStream(AnsiOutputStream out, String enc) {
        if (enc != null) {
            try {
                return new AnsiPrintStream(out, true, enc);
            } catch (UnsupportedEncodingException e) {
            }
        }
        return new AnsiPrintStream(out, true);
    }

    static boolean getBoolean(String name) {
        boolean result = false;
        try {
            String val = System.getProperty(name);
            result = val.isEmpty() || Boolean.parseBoolean(val);
        } catch (IllegalArgumentException | NullPointerException ignored) {
        }
        return result;
    }

    /**
     * If the standard out natively supports ANSI escape codes, then this just
     * returns System.out, otherwise it will provide an ANSI aware PrintStream
     * which strips out the ANSI escape sequences or which implement the escape
     * sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    public static AnsiPrintStream out() {
        doInstall();
        return (AnsiPrintStream) out;
    }

    /**
     * Access to the original System.out stream before ansi streams were installed.
     *
     * @return the originial System.out print stream
     */
    public static PrintStream sysOut() {
        return system_out;
    }

    /**
     * If the standard out natively supports ANSI escape codes, then this just
     * returns System.err, otherwise it will provide an ANSI aware PrintStream
     * which strips out the ANSI escape sequences or which implement the escape
     * sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    public static AnsiPrintStream err() {
        doInstall();
        return (AnsiPrintStream) err;
    }

    /**
     * Access to the original System.err stream before ansi streams were installed.
     *
     * @return the originial System.err print stream
     */
    public static PrintStream sysErr() {
        return system_err;
    }

    /**
     * Install <code>AnsiConsole.out()</code> to <code>System.out</code> and
     * <code>AnsiConsole.err()</code> to <code>System.err</code>.
     * @see #systemUninstall()
     */
    public static synchronized void systemInstall() {
        if (installed == 0) {
            doInstall();
            System.setOut(out);
            System.setErr(err);
        }
        installed++;
    }

    /**
     * check if the streams have been installed or not
     */
    public static synchronized boolean isInstalled() {
        return installed > 0;
    }

    /**
     * undo a previous {@link #systemInstall()}.  If {@link #systemInstall()} was called
     * multiple times, {@link #systemUninstall()} must be called the same number of times before
     * it is actually uninstalled.
     */
    public static synchronized void systemUninstall() {
        installed--;
        if (installed == 0) {
            doUninstall();
            System.setOut(system_out);
            System.setErr(system_err);
        }
    }
}
