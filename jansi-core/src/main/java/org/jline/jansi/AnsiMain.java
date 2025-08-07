/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Properties;

import org.jline.terminal.impl.Diag;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jline.jansi.Ansi.ansi;

/**
 * Main class for the library, providing executable jar to diagnose Jansi setup.
 * <p>If no system property is set and output is sent to a terminal (no redirect to a file):
 * <ul>
 * <li>any terminal on any Unix should get <code>RESET_ANSI_AT_CLOSE</code> mode,</li>
 * <li>on Windows, Git-bash or Cygwin terminals should get <code>RESET_ANSI_AT_CLOSE</code> mode also, since they
 * support natively ANSI escape sequences like any Unix terminal,</li>
 * <li>on Windows, cmd.exe, PowerShell or Git-cmd terminals should get <code>WINDOWS</code> mode.</li>
 * </ul>
 * If stdout is redirected to a file (<code>&gt; out.txt</code>), System.out should switch to <code>STRIP_ANSI</code>.
 * Same for stderr redirection (<code>2&gt; err.txt</code>) which should affect System.err mode.
 * <p>The results will vary if you play with <code>jansi.passthrough</code>, <code>jansi.strip</code> or
 * <code>jansi.force</code> system property, or if you redirect output to a file.
 * <p>If you have a specific situation that is not covered, please report precise conditions to reproduce
 * the issue and ideas on how to detect precisely the affected situation.
 * @see AnsiConsole
 */
@SuppressWarnings("deprecation")
public class AnsiMain {
    /**
     * Private constructor to prevent instantiation.
     */
    private AnsiMain() {
        // Utility class
    }

    public static void main(String... args) throws IOException {
        Diag.diag(System.out);

        System.out.println("Jansi " + getJansiVersion());

        System.out.println();

        System.out.println("jansi.providers= " + System.getProperty(AnsiConsole.JANSI_PROVIDERS, ""));
        //        String provider = ((TerminalExt) AnsiConsole.terminal).getProvider().name();
        //        System.out.println("Selected provider: " + provider);
        //
        //        if (AnsiConsole.JANSI_PROVIDER_JNI.equals(provider)) {
        //            // info on native library
        //            System.out.println("library.jansi.path= " + System.getProperty("library.jansi.path", ""));
        //            System.out.println("library.jansi.version= " + System.getProperty("library.jansi.version", ""));
        //            boolean loaded = JansiLoader.initialize();
        //            if (loaded) {
        //                System.out.println("Jansi native library loaded from " + JansiLoader.getNativeLibraryPath());
        //                if (JansiLoader.getNativeLibrarySourceUrl() != null) {
        //                    System.out.println("   which was auto-extracted from " +
        // JansiLoader.getNativeLibrarySourceUrl());
        //                }
        //            } else {
        //                String prev = System.getProperty(AnsiConsole.JANSI_GRACEFUL);
        //                try {
        //                    System.setProperty(AnsiConsole.JANSI_GRACEFUL, "false");
        //                    JansiLoader.initialize();
        //                } catch (Throwable e) {
        //                    e.printStackTrace(System.out);
        //                } finally {
        //                    if (prev != null) {
        //                        System.setProperty(AnsiConsole.JANSI_GRACEFUL, prev);
        //                    } else {
        //                        System.clearProperty(AnsiConsole.JANSI_GRACEFUL);
        //                    }
        //                }
        //            }
        //        }

        System.out.println();

        System.out.println("os.name= " + System.getProperty("os.name") + ", "
                + "os.version= " + System.getProperty("os.version") + ", "
                + "os.arch= " + System.getProperty("os.arch"));
        System.out.println("file.encoding= " + System.getProperty("file.encoding"));
        System.out.println("sun.stdout.encoding= " + System.getProperty("sun.stdout.encoding") + ", "
                + "sun.stderr.encoding= " + System.getProperty("sun.stderr.encoding"));
        System.out.println("stdout.encoding= " + System.getProperty("stdout.encoding") + ", " + "stderr.encoding= "
                + System.getProperty("stderr.encoding"));
        System.out.println("java.version= " + System.getProperty("java.version") + ", "
                + "java.vendor= " + System.getProperty("java.vendor") + ","
                + " java.home= " + System.getProperty("java.home"));
        System.out.println("Console: " + System.console());

        System.out.println();

        System.out.println(AnsiConsole.JANSI_GRACEFUL + "= " + System.getProperty(AnsiConsole.JANSI_GRACEFUL, ""));
        System.out.println(AnsiConsole.JANSI_MODE + "= " + System.getProperty(AnsiConsole.JANSI_MODE, ""));
        System.out.println(AnsiConsole.JANSI_OUT_MODE + "= " + System.getProperty(AnsiConsole.JANSI_OUT_MODE, ""));
        System.out.println(AnsiConsole.JANSI_ERR_MODE + "= " + System.getProperty(AnsiConsole.JANSI_ERR_MODE, ""));
        System.out.println(AnsiConsole.JANSI_COLORS + "= " + System.getProperty(AnsiConsole.JANSI_COLORS, ""));
        System.out.println(AnsiConsole.JANSI_OUT_COLORS + "= " + System.getProperty(AnsiConsole.JANSI_OUT_COLORS, ""));
        System.out.println(AnsiConsole.JANSI_ERR_COLORS + "= " + System.getProperty(AnsiConsole.JANSI_ERR_COLORS, ""));
        System.out.println(AnsiConsole.JANSI_NORESET + "= " + AnsiConsole.getBoolean(AnsiConsole.JANSI_NORESET));
        System.out.println(Ansi.DISABLE + "= " + AnsiConsole.getBoolean(Ansi.DISABLE));

        System.out.println();

        System.out.println("IS_WINDOWS: " + AnsiConsole.IS_WINDOWS);
        if (AnsiConsole.IS_WINDOWS) {
            System.out.println("IS_CONEMU: " + AnsiConsole.IS_CONEMU);
            System.out.println("IS_CYGWIN: " + AnsiConsole.IS_CYGWIN);
            System.out.println("IS_MSYSTEM: " + AnsiConsole.IS_MSYSTEM);
        }

        System.out.println();

        diagnoseTty(false); // System.out
        diagnoseTty(true); // System.err

        AnsiConsole.systemInstall();

        System.out.println();

        System.out.println("Resulting Jansi modes for stout/stderr streams:");
        System.out.println("  - System.out: " + AnsiConsole.out().toString());
        System.out.println("  - System.err: " + AnsiConsole.err().toString());
        System.out.println("Processor types description:");
        for (AnsiType type : AnsiType.values()) {
            System.out.println("  - " + type + ": " + type.getDescription());
        }
        System.out.println("Colors support description:");
        for (AnsiColors colors : AnsiColors.values()) {
            System.out.println("  - " + colors + ": " + colors.getDescription());
        }
        System.out.println("Modes description:");
        for (AnsiMode mode : AnsiMode.values()) {
            System.out.println("  - " + mode + ": " + mode.getDescription());
        }

        try {
            System.out.println();

            testAnsi(false);
            testAnsi(true);

            if (args.length == 0) {
                printJansiLogoDemo();
                return;
            }

            System.out.println();

            if (args.length == 1) {
                File f = new File(args[0]);
                if (f.exists()) {
                    // write file content
                    System.out.println(
                            Ansi.ansi().bold().a("\"" + args[0] + "\" content:").reset());
                    writeFileContent(f);
                    return;
                }
            }

            // write args without Jansi then with Jansi AnsiConsole
            System.out.println(Ansi.ansi().bold().a("original args:").reset());
            int i = 1;
            for (String arg : args) {
                AnsiConsole.sysOut().print(i++ + ": ");
                AnsiConsole.sysOut().println(arg);
            }

            System.out.println(Ansi.ansi().bold().a("Jansi filtered args:").reset());
            i = 1;
            for (String arg : args) {
                System.out.print(i++ + ": ");
                System.out.println(arg);
            }
        } finally {
            AnsiConsole.systemUninstall();
        }
    }

    private static String getJansiVersion() {
        Package p = AnsiMain.class.getPackage();
        return (p == null) ? null : p.getImplementationVersion();
    }

    private static void diagnoseTty(boolean stderr) {
        //        int isatty;
        //        int width;
        //        if (AnsiConsole.IS_WINDOWS) {
        //            long console = AnsiConsoleSupportHolder.getKernel32().getStdHandle(!stderr);
        //            isatty = AnsiConsoleSupportHolder.getKernel32().isTty(console);
        //            if ((AnsiConsole.IS_CONEMU || AnsiConsole.IS_CYGWIN || AnsiConsole.IS_MSYSTEM) && isatty == 0) {
        //                MingwSupport mingw = new MingwSupport();
        //                String name = mingw.getConsoleName(!stderr);
        //                if (name != null && !name.isEmpty()) {
        //                    isatty = 1;
        //                    width = mingw.getTerminalWidth(name);
        //                } else {
        //                    isatty = 0;
        //                    width = 0;
        //                }
        //            } else {
        //                width = AnsiConsoleSupportHolder.getKernel32().getTerminalWidth(console);
        //            }
        //        } else {
        //            int fd = stderr ? AnsiConsoleSupport.CLibrary.STDERR_FILENO :
        // AnsiConsoleSupport.CLibrary.STDOUT_FILENO;
        //            isatty = AnsiConsoleSupportHolder.getCLibrary().isTty(fd);
        //            width = AnsiConsoleSupportHolder.getCLibrary().getTerminalWidth(fd);
        //        }
        //
        //        System.out.println("isatty(STD" + (stderr ? "ERR" : "OUT") + "_FILENO): " + isatty + ", System."
        //                + (stderr ? "err" : "out") + " " + ((isatty == 0) ? "is *NOT*" : "is") + " a terminal");
        //        System.out.println("width(STD" + (stderr ? "ERR" : "OUT") + "_FILENO): " + width);
    }

    private static void testAnsi(boolean stderr) {
        @SuppressWarnings("resource")
        PrintStream s = stderr ? System.err : System.out;
        s.print("test on System." + (stderr ? "err" : "out") + ":");
        for (Ansi.Color c : Ansi.Color.values()) {
            s.print(" " + Ansi.ansi().fg(c) + c + Ansi.ansi().reset());
        }
        s.println();
        s.print("            bright:");
        for (Ansi.Color c : Ansi.Color.values()) {
            s.print(" " + Ansi.ansi().fgBright(c) + c + Ansi.ansi().reset());
        }
        s.println();
        s.print("              bold:");
        for (Ansi.Color c : Ansi.Color.values()) {
            s.print(" " + Ansi.ansi().bold().fg(c) + c + Ansi.ansi().reset());
        }
        s.println();
        s.print("             faint:");
        for (Ansi.Color c : Ansi.Color.values()) {
            s.print(" " + Ansi.ansi().a(Ansi.Attribute.INTENSITY_FAINT).fg(c) + c
                    + Ansi.ansi().reset());
        }
        s.println();
        s.print("        bold+faint:");
        for (Ansi.Color c : Ansi.Color.values()) {
            s.print(" " + Ansi.ansi().bold().a(Ansi.Attribute.INTENSITY_FAINT).fg(c) + c
                    + Ansi.ansi().reset());
        }
        s.println();
        Ansi ansi = Ansi.ansi();
        ansi.a("        256 colors: ");
        for (int i = 0; i < 6 * 6 * 6; i++) {
            if (i > 0 && i % 36 == 0) {
                ansi.reset();
                ansi.newline();
                ansi.a("                    ");
            } else if (i > 0 && i % 6 == 0) {
                ansi.reset();
                ansi.a("  ");
            }
            int a0 = i % 6;
            int a1 = (i / 6) % 6;
            int a2 = i / 36;
            ansi.bg(16 + a0 + a2 * 6 + a1 * 36).a(' ');
        }
        ansi.reset();
        s.println(ansi);
        ansi = Ansi.ansi();
        ansi.a("         truecolor: ");
        for (int i = 0; i < 256; i++) {
            if (i > 0 && i % 48 == 0) {
                ansi.reset();
                ansi.newline();
                ansi.a("                    ");
            }
            int r = 255 - i;
            int g = i * 2 > 255 ? 255 - 2 * i : 2 * i;
            int b = i;
            ansi.bgRgb(r, g, b).fgRgb(255 - r, 255 - g, 255 - b).a(i % 2 == 0 ? '/' : '\\');
        }
        ansi.reset();
        s.println(ansi);
    }

    private static String getPomPropertiesVersion(String path) throws IOException {
        InputStream in = AnsiMain.class.getResourceAsStream("/META-INF/maven/" + path + "/pom.properties");
        if (in == null) {
            return null;
        }
        try {
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("version");
        } finally {
            closeQuietly(in);
        }
    }

    private static void printJansiLogoDemo() throws IOException {
        BufferedReader in =
                new BufferedReader(new InputStreamReader(AnsiMain.class.getResourceAsStream("jansi.txt"), UTF_8));
        try {
            String l;
            while ((l = in.readLine()) != null) {
                System.out.println(l);
            }
        } finally {
            closeQuietly(in);
        }
    }

    private static void writeFileContent(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            byte[] buf = new byte[1024];
            int l = 0;
            while ((l = in.read(buf)) >= 0) {
                System.out.write(buf, 0, l);
            }
        } finally {
            closeQuietly(in);
        }
    }

    private static void closeQuietly(Closeable c) {
        try {
            c.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }
}
