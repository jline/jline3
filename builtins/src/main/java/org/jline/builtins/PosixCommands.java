/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.builtins.Options.HelpException;
import org.jline.builtins.Source.StdInSource;
import org.jline.builtins.Source.URLSource;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.OSUtils;
import org.jline.utils.StyleResolver;

/**
 * POSIX-like command implementations for JLine applications.
 * <p>
 * This class provides implementations of common POSIX commands that can be used
 * in JLine-based applications. The commands are designed to be reusable and
 * independent of any specific command framework.
 * </p>
 * <p>
 * Available commands include:
 * </p>
 * <ul>
 *   <li>cat - concatenate and print files</li>
 *   <li>echo - display text</li>
 *   <li>grep - search text patterns</li>
 *   <li>ls - list directory contents</li>
 *   <li>pwd - print working directory</li>
 *   <li>head - display first lines of files</li>
 *   <li>tail - display last lines of files</li>
 *   <li>wc - word, line, character, and byte count</li>
 *   <li>date - display or set date</li>
 *   <li>sleep - suspend execution</li>
 *   <li>sort - sort lines of text</li>
 *   <li>clear - clear terminal screen</li>
 * </ul>
 *
 * @see <a href="http://www.opengroup.org/onlinepubs/009695399/utilities/contents.html">
 * POSIX Utilities</a>
 */
@SuppressWarnings("resource")
public class PosixCommands {

    /**
     * Context for command execution, providing I/O streams and current directory.
     */
    public static class Context {
        private final InputStream in;
        private final PrintStream out;
        private final PrintStream err;
        private final Path currentDir;
        private final Terminal terminal;
        private final Function<String, Object> variables;

        public Context(
                InputStream in,
                PrintStream out,
                PrintStream err,
                Path currentDir,
                Terminal terminal,
                Function<String, Object> variables) {
            this.in = in;
            this.out = out;
            this.err = err;
            this.currentDir = currentDir;
            this.terminal = terminal;
            this.variables = variables;
        }

        public InputStream in() {
            return in;
        }

        public PrintStream out() {
            return out;
        }

        public PrintStream err() {
            return err;
        }

        public Path currentDir() {
            return currentDir;
        }

        public Terminal terminal() {
            return terminal;
        }

        public boolean isTty() {
            return terminal != null;
        }

        public Object get(String name) {
            return variables.apply(name);
        }
    }

    /**
     * Change directory command.
     * <p>
     * Changes the current working directory to the specified directory.
     * This version provides validation only and does not actually change
     * the directory. Use the overloaded version with Consumer&lt;Path&gt; for
     * actual directory changing functionality.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments
     * @throws Exception if the command fails
     * @see #cd(Context, String[], Consumer)
     */
    public static void cd(Context context, String[] argv) throws Exception {
        cd(context, argv, null);
    }

    /**
     * Change directory command with directory changer.
     * <p>
     * Changes the current working directory to the specified directory.
     * Supports POSIX-compliant options and behaviors:
     * </p>
     * <ul>
     *   <li>No arguments: change to home directory</li>
     *   <li>"-": change to previous directory (placeholder)</li>
     *   <li>"-P": use physical directory structure (resolve symlinks)</li>
     *   <li>"-L": follow symbolic links (default)</li>
     * </ul>
     * <p>
     * The directoryChanger consumer is called with the resolved path if provided,
     * allowing the caller to actually perform the directory change operation.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments
     * @param directoryChanger consumer to perform the actual directory change
     * @throws Exception if the command fails
     */
    public static void cd(Context context, String[] argv, Consumer<Path> directoryChanger) throws Exception {
        final String[] usage = {
            "cd - change directory",
            "Usage: cd [OPTIONS] [DIRECTORY]",
            "  -? --help                show help",
            "  -P                       use physical directory structure",
            "  -L                       follow symbolic links (default)"
        };
        Options opt = parseOptions(context, usage, argv);
        if (opt.args().size() != 1) {
            throw new IllegalArgumentException("usage: cd DIRECTORY");
        }
        Path cwd = context.currentDir();
        Path newDir;

        if (opt.args().isEmpty()) {
            // No argument - go to home directory
            String home = System.getProperty("user.home");
            if (home != null) {
                newDir = Paths.get(home);
            } else {
                newDir = cwd; // Stay in current directory if no home
            }
        } else {
            String target = opt.args().get(0);
            if ("-".equals(target)) {
                // Go to previous directory (simplified - just stay in current)
                newDir = cwd;
            } else {
                newDir = cwd.resolve(target);
            }
        }

        // Resolve path based on options
        if (opt.isSet("P")) {
            // Physical path - resolve all symbolic links
            newDir = newDir.toRealPath();
        } else {
            // Logical path - normalize but keep symbolic links
            newDir = newDir.toAbsolutePath().normalize();
        }

        if (!Files.exists(newDir)) {
            throw new IOException("cd: no such file or directory: " + opt.args().get(0));
        } else if (!Files.isDirectory(newDir)) {
            throw new IOException("cd: not a directory: " + opt.args().get(0));
        }

        // Change directory if changer is provided
        if (directoryChanger != null) {
            directoryChanger.accept(newDir);
        }
    }

    /**
     * Print working directory command.
     * <p>
     * Prints the absolute pathname of the current working directory to standard output.
     * This command is equivalent to the POSIX pwd utility.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments (should be empty except for --help)
     * @throws Exception if the command fails
     */
    public static void pwd(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "pwd - print working directory", "Usage: pwd [OPTIONS]", "  -? --help                show help"
        };
        Options opt = parseOptions(context, usage, argv);
        if (!opt.args().isEmpty()) {
            throw new IllegalArgumentException("usage: pwd");
        }
        context.out().println(context.currentDir());
    }

    /**
     * Echo command - display text.
     * <p>
     * Writes its arguments to standard output, followed by a newline.
     * Supports the -n option to suppress the trailing newline.
     * This command is equivalent to the POSIX echo utility.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments
     * @throws Exception if the command fails
     */
    public static void echo(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "echo - display text",
            "Usage: echo [OPTIONS] [ARGUMENTS]",
            "  -? --help                show help",
            "  -n                       no trailing new line"
        };
        Options opt = parseOptions(context, usage, argv);

        List<String> args = opt.args();
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            for (String arg : args) {
                if (buf.length() > 0) buf.append(' ');
                // Process escape sequences
                for (int i = 0; i < arg.length(); i++) {
                    int c = arg.charAt(i);
                    int ch;
                    if (c == '\\') {
                        c = i < arg.length() - 1 ? arg.charAt(++i) : '\\';
                        switch (c) {
                            case 'a':
                                buf.append('\u0007');
                                break;
                            case 'n':
                                buf.append('\n');
                                break;
                            case 't':
                                buf.append('\t');
                                break;
                            case 'r':
                                buf.append('\r');
                                break;
                            case '\\':
                                buf.append('\\');
                                break;
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                ch = 0;
                                for (int j = 0; j < 3; j++) {
                                    c = i < arg.length() - 1 ? arg.charAt(++i) : -1;
                                    if (c >= 0) {
                                        ch = ch * 8 + (c - '0');
                                    }
                                }
                                buf.append((char) ch);
                                break;
                            case 'u':
                                ch = 0;
                                for (int j = 0; j < 4; j++) {
                                    c = i < arg.length() - 1 ? arg.charAt(++i) : -1;
                                    if (c >= 0) {
                                        if (c >= 'A' && c <= 'Z') {
                                            ch = ch * 16 + (c - 'A' + 10);
                                        } else if (c >= 'a' && c <= 'z') {
                                            ch = ch * 16 + (c - 'a' + 10);
                                        } else if (c >= '0' && c <= '9') {
                                            ch = ch * 16 + (c - '0');
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                buf.append((char) ch);
                                break;
                            default:
                                buf.append((char) c);
                                break;
                        }
                    } else {
                        buf.append((char) c);
                    }
                }
            }
        }
        if (opt.isSet("n")) {
            context.out().print(buf);
        } else {
            context.out().println(buf);
        }
    }

    /**
     * Echo command - display text (Object array version for compatibility).
     */
    public static void echo(Context context, Object[] argv) throws Exception {
        // Convert Object array to String array
        String[] stringArgv = new String[argv.length];
        for (int i = 0; i < argv.length; i++) {
            stringArgv[i] = argv[i] != null ? argv[i].toString() : "";
        }
        echo(context, stringArgv);
    }

    /**
     * Cat command - concatenate and print files.
     * <p>
     * Reads files sequentially and writes them to standard output.
     * If no files are specified, or if a file is "-", reads from standard input.
     * Supports the -n option to number output lines.
     * This command is equivalent to the POSIX cat utility.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments
     * @throws Exception if the command fails
     */
    public static void cat(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "cat - concatenate and print FILES",
            "Usage: cat [OPTIONS] [FILES]",
            "  -? --help                show help",
            "  -n                       number the output lines, starting at 1"
        };
        Options opt = parseOptions(context, usage, argv);

        List<String> args = opt.args();
        if (args.isEmpty()) {
            args = Collections.singletonList("-");
        }
        Path cwd = context.currentDir();
        for (String arg : args) {
            InputStream is;
            if ("-".equals(arg)) {
                is = context.in();
            } else {
                is = cwd.toUri().resolve(arg).toURL().openStream();
            }
            cat(context, new BufferedReader(new InputStreamReader(is)), opt.isSet("n"));
        }
    }

    private static void cat(Context context, BufferedReader reader, boolean numbered) throws IOException {
        String line;
        int lineno = 1;
        try {
            while ((line = reader.readLine()) != null) {
                if (numbered) {
                    context.out().printf("%6d\t%s%n", lineno++, line);
                } else {
                    context.out().println(line);
                }
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Date command - display current date and time.
     * <p>
     * Displays the current date and time, or a specified date/time.
     * Supports various output formats including ISO 8601, RFC 2822, and RFC 3339.
     * Can parse date strings and display dates from epoch seconds.
     * This command provides functionality similar to the POSIX date utility.
     * </p>
     * <p>
     * Supported options:
     * </p>
     * <ul>
     *   <li>-u, --utc: Display time in UTC</li>
     *   <li>-r, --reference: Display time from epoch seconds</li>
     *   <li>-d, --date: Parse and display specified date string</li>
     *   <li>-I, --iso-8601: Output in ISO 8601 format</li>
     *   <li>-R, --rfc-2822: Output in RFC 2822 format</li>
     *   <li>--rfc-3339: Output in RFC 3339 format</li>
     * </ul>
     *
     * @param context the execution context
     * @param argv command arguments
     * @throws Exception if the command fails
     */
    public static void date(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "date - display date",
            "Usage: date [OPTIONS] [+FORMAT]",
            "  -? --help                    Show help",
            "  -u --utc                     Use UTC timezone",
            "  -r --reference=SECONDS       Print the date represented by 'seconds' since January 1, 1970",
            "  -d --date=STRING             Display time described by STRING",
            "  -f --file=DATEFILE           Like --date once for each line of DATEFILE",
            "  -I --iso-8601[=TIMESPEC]     Output date/time in ISO 8601 format",
            "  -R --rfc-2822                Output date and time in RFC 2822 format",
            "     --rfc-3339=TIMESPEC       Output date and time in RFC 3339 format"
        };

        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        Date input = new Date();
        String output = null;
        boolean useUtc = opt.isSet("utc");

        // Handle reference time
        if (opt.isSet("reference")) {
            long seconds = Long.parseLong(opt.get("reference"));
            input = new Date(seconds * 1000L);
        }

        // Handle date string
        if (opt.isSet("date")) {
            String dateStr = opt.get("date");
            // Simple date parsing - could be enhanced
            try {
                // Try common date formats
                SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                    new SimpleDateFormat("yyyy-MM-dd"),
                    new SimpleDateFormat("MM/dd/yyyy"),
                    new SimpleDateFormat("dd-MM-yyyy"),
                    new SimpleDateFormat("yyyy/MM/dd")
                };

                boolean parsed = false;
                for (SimpleDateFormat format : formats) {
                    try {
                        input = format.parse(dateStr);
                        parsed = true;
                        break;
                    } catch (Exception ignored) {
                        // Try next format
                    }
                }

                if (!parsed) {
                    throw new IllegalArgumentException("Unable to parse date: " + dateStr);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date string: " + dateStr);
            }
        }

        // Handle ISO 8601 format
        if (opt.isSet("iso-8601")) {
            String timespec = opt.get("iso-8601");
            if (timespec == null || "date".equals(timespec)) {
                output = "%Y-%m-%d";
            } else if ("hours".equals(timespec)) {
                output = "%Y-%m-%dT%H%z";
            } else if ("minutes".equals(timespec)) {
                output = "%Y-%m-%dT%H:%M%z";
            } else if ("seconds".equals(timespec)) {
                output = "%Y-%m-%dT%H:%M:%S%z";
            } else if ("ns".equals(timespec)) {
                output = "%Y-%m-%dT%H:%M:%S,%N%z";
            }
        }

        // Handle RFC 2822 format
        if (opt.isSet("rfc-2822")) {
            output = "%a, %d %b %Y %H:%M:%S %z";
        }

        // Handle RFC 3339 format
        if (opt.isSet("rfc-3339")) {
            String timespec = opt.get("rfc-3339");
            if ("date".equals(timespec)) {
                output = "%Y-%m-%d";
            } else if ("seconds".equals(timespec)) {
                output = "%Y-%m-%d %H:%M:%S%z";
            } else if ("ns".equals(timespec)) {
                output = "%Y-%m-%d %H:%M:%S.%N%z";
            }
        }

        // Handle format from arguments
        List<String> args = opt.args();
        if (!args.isEmpty()) {
            String arg = args.get(0);
            if (arg.startsWith("+")) {
                output = arg.substring(1);
            }
        }

        // Default format
        if (output == null) {
            output = "%c";
        }

        // Create formatter with UTC if requested
        SimpleDateFormat formatter = new SimpleDateFormat(toJavaDateFormat(output));
        if (useUtc) {
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        // Print output
        context.out().println(formatter.format(input));
    }

    /**
     * Convert Unix date format to Java SimpleDateFormat.
     */
    private static String toJavaDateFormat(String format) {
        // transform Unix format to Java SimpleDateFormat (if required)
        StringBuilder sb = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '%') {
                if (i + 1 < format.length()) {
                    if (quote) {
                        sb.append('\'');
                        quote = false;
                    }
                    c = format.charAt(++i);
                    switch (c) {
                        case '+':
                        case 'A':
                            sb.append("MMM EEE d HH:mm:ss yyyy");
                            break;
                        case 'a':
                            sb.append("EEE");
                            break;
                        case 'B':
                            sb.append("MMMMMMM");
                            break;
                        case 'b':
                            sb.append("MMM");
                            break;
                        case 'C':
                            sb.append("yy");
                            break;
                        case 'c':
                            sb.append("MMM EEE d HH:mm:ss yyyy");
                            break;
                        case 'D':
                            sb.append("MM/dd/yy");
                            break;
                        case 'd':
                            sb.append("dd");
                            break;
                        case 'e':
                            sb.append("dd");
                            break;
                        case 'F':
                            sb.append("yyyy-MM-dd");
                            break;
                        case 'G':
                            sb.append("YYYY");
                            break;
                        case 'g':
                            sb.append("YY");
                            break;
                        case 'H':
                            sb.append("HH");
                            break;
                        case 'h':
                            sb.append("MMM");
                            break;
                        case 'I':
                            sb.append("hh");
                            break;
                        case 'j':
                            sb.append("DDD");
                            break;
                        case 'k':
                            sb.append("HH");
                            break;
                        case 'l':
                            sb.append("hh");
                            break;
                        case 'M':
                            sb.append("mm");
                            break;
                        case 'm':
                            sb.append("MM");
                            break;
                        case 'N':
                            sb.append("S");
                            break;
                        case 'n':
                            sb.append("\n");
                            break;
                        case 'P':
                            sb.append("aa");
                            break;
                        case 'p':
                            sb.append("aa");
                            break;
                        case 'r':
                            sb.append("hh:mm:ss aa");
                            break;
                        case 'R':
                            sb.append("HH:mm");
                            break;
                        case 'S':
                            sb.append("ss");
                            break;
                        case 's':
                            sb.append("S");
                            break;
                        case 'T':
                            sb.append("HH:mm:ss");
                            break;
                        case 't':
                            sb.append("\t");
                            break;
                        case 'U':
                            sb.append("w");
                            break;
                        case 'u':
                            sb.append("u");
                            break;
                        case 'V':
                            sb.append("W");
                            break;
                        case 'v':
                            sb.append("dd-MMM-yyyy");
                            break;
                        case 'W':
                            sb.append("w");
                            break;
                        case 'w':
                            sb.append("u");
                            break;
                        case 'X':
                            sb.append("HH:mm:ss");
                            break;
                        case 'x':
                            sb.append("MM/dd/yy");
                            break;
                        case 'Y':
                            sb.append("yyyy");
                            break;
                        case 'y':
                            sb.append("yy");
                            break;
                        case 'Z':
                            sb.append("z");
                            break;
                        case 'z':
                            sb.append("X");
                            break;
                        case '%':
                            sb.append("%");
                            break;
                    }
                } else {
                    if (!quote) {
                        sb.append('\'');
                    }
                    sb.append(c);
                    sb.append('\'');
                }
            } else {
                if ((c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') && !quote) {
                    sb.append('\'');
                    quote = true;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Sleep command - suspend execution.
     * <p>
     * Suspends execution for a specified number of seconds.
     * This command is equivalent to the POSIX sleep utility.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments (should contain the number of seconds)
     * @throws Exception if the command fails
     */
    public static void sleep(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "sleep - suspend execution for an interval of time",
            "Usage: sleep seconds",
            "  -? --help                    show help"
        };
        Options opt = parseOptions(context, usage, argv);

        List<String> args = opt.args();
        if (args.size() != 1) {
            throw new IllegalArgumentException("usage: sleep seconds");
        } else {
            int s = Integer.parseInt(args.get(0));
            Thread.sleep(s * 1000L);
        }
    }

    /**
     * Watch command - execute a command repeatedly and display output.
     * <p>
     * Executes a command repeatedly at specified intervals and displays the output.
     * This version uses a basic command executor. For full shell integration,
     * use the overloaded version with a CommandExecutor.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments
     * @throws Exception if the command fails
     * @see #watch(Context, String[], CommandExecutor)
     */
    public static void watch(Context context, String[] argv) throws Exception {
        watch(context, argv, null);
    }

    /**
     * Watch command with command executor - execute a command repeatedly and display output.
     * <p>
     * Executes a command repeatedly at specified intervals and displays the output.
     * The command is executed using the provided CommandExecutor, which allows for
     * full shell integration and complex command execution.
     * </p>
     * <p>
     * Supported options:
     * </p>
     * <ul>
     *   <li>-n, --interval: Specify the interval between executions (default: 1 second)</li>
     *   <li>-a, --append: Append output instead of clearing the screen</li>
     * </ul>
     *
     * @param context the execution context
     * @param argv command arguments
     * @param executor the command executor for running the watched command
     * @throws Exception if the command fails
     */
    public static void watch(Context context, String[] argv, CommandExecutor executor) throws Exception {
        final String[] usage = {
            "watch - watches & refreshes the output of a command",
            "Usage: watch [OPTIONS] COMMAND",
            "  -? --help                    Show help",
            "  -n --interval=SECONDS        Interval between executions of the command in seconds",
            "  -a --append                  The output should be appended but not clear the console"
        };

        Options opt = parseOptions(context, usage, argv);

        List<String> args = opt.args();
        if (args.isEmpty()) {
            throw new IllegalArgumentException("usage: watch COMMAND");
        }

        int intervalValue = 1;
        if (opt.isSet("interval")) {
            intervalValue = opt.getNumber("interval");
            if (intervalValue < 1) {
                intervalValue = 1;
            }
        }
        final int interval = intervalValue;

        final boolean append = opt.isSet("append");
        final String command = String.join(" ", args);
        final List<String> finalArgs = new ArrayList<>(args);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        try {
            Runnable task = () -> {
                try {
                    if (!append && context.isTty()) {
                        // Clear screen if possible
                        context.terminal().puts(Capability.clear_screen);
                        context.terminal().flush();
                    } else if (!append) {
                        context.out().println();
                    }

                    // Display header
                    context.out().println("Every " + interval + "s: " + command + "    " + LocalDateTime.now());
                    context.out().println();

                    // Execute command
                    if (executor != null) {
                        try {
                            String output = executor.execute(finalArgs);
                            context.out().print(output);
                        } catch (Exception e) {
                            context.err().println("Command execution failed: " + e.getMessage());
                        }
                    } else {
                        // Fallback: try to execute as a simple command
                        try {
                            Process process = new ProcessBuilder(finalArgs).start();
                            try (BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    context.out().println(line);
                                }
                            }
                            process.waitFor();
                        } catch (Exception e) {
                            context.out().println("Command: " + command);
                            context.out()
                                    .println(
                                            "(Command execution requires shell integration - use gogo implementation for full functionality)");
                        }
                    }
                    context.out().flush();
                } catch (Exception e) {
                    context.err().println("Error executing command: " + e.getMessage());
                }
            };

            executorService.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);

            if (context.isTty()) {
                // Wait for user input to stop
                context.out().println("Press any key to stop...");
                context.in().read();
            } else {
                // Non-interactive mode - run for a limited time
                Thread.sleep(10000); // 10 seconds
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * Thread monitoring command - display and update sorted information about threads.
     * <p>
     * Displays real-time information about Java threads in a top-like interface.
     * Shows thread names, states, CPU usage, and other thread-related information.
     * This command delegates to the TTop utility for full functionality.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments
     * @throws Exception if the command fails
     */
    public static void ttop(Context context, String[] argv) throws Exception {
        TTop.ttop(context.terminal(), context.out(), context.err(), argv);
    }

    /**
     * Text editor command - edit files with nano-like interface.
     * <p>
     * Opens a text editor with a nano-like interface for editing files.
     * Provides basic text editing functionality with keyboard shortcuts
     * similar to the nano editor. This command delegates to the Nano utility.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments (typically file names to edit)
     * @throws Exception if the command fails
     */
    public static void nano(Context context, String[] argv) throws Exception {
        Options opt = parseOptions(context, Nano.usage(), argv);
        Nano nano = new Nano(context.terminal(), context.currentDir(), opt);
        nano.open(opt.args());
        nano.run();
    }

    /**
     * Pager command - view files with less-like interface.
     * <p>
     * Opens a pager for viewing files with a less-like interface.
     * Supports navigation, searching, and other pager functionality.
     * Can handle multiple files and supports glob patterns.
     * This command delegates to the Less utility for full functionality.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments (typically file names to view)
     * @throws Exception if the command fails
     */
    public static void less(Context context, String[] argv) throws Exception {
        Options opt = parseOptions(context, Less.usage(), argv);

        List<Source> sources = new ArrayList<>();
        if (opt.args().isEmpty()) {
            opt.args().add("-");
        }
        for (String arg : opt.args()) {
            if ("-".equals(arg)) {
                sources.add(new Source.StdInSource(context.in()));
            } else if (arg.contains("*") || arg.contains("?")) {
                PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + arg);
                try (Stream<Path> pathStream = Files.walk(context.currentDir())) {
                    pathStream.filter(pathMatcher::matches).forEach(p -> {
                        try {
                            sources.add(new Source.URLSource(p.toUri().toURL(), p.toString()));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } else {
                try {
                    Path path = context.currentDir().resolve(arg);
                    sources.add(new Source.URLSource(path.toUri().toURL(), arg));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (!context.isTty()) {
            // Non-interactive mode - just cat the files
            for (Source source : sources) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.read()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        context.out().println(line);
                    }
                }
            }
            return;
        }

        Less less = new Less(context.terminal(), context.currentDir(), opt);
        less.run(sources);
    }

    /**
     * Clear command - clear terminal screen.
     * <p>
     * Clears the terminal screen by sending the appropriate escape sequence.
     * Only works when connected to a TTY terminal.
     * This command is equivalent to the POSIX clear utility.
     * </p>
     *
     * @param context the execution context
     * @param argv command arguments (should be empty except for --help)
     * @throws Exception if the command fails
     */
    public static void clear(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "clear - clear screen", "Usage: clear [OPTIONS]", "  -? --help                    Show help",
        };
        Options opt = parseOptions(context, usage, argv);

        if (context.isTty()) {
            context.terminal().puts(Capability.clear_screen);
            context.terminal().flush();
        }
    }

    /**
     * Word count command - count lines, words, characters, and bytes.
     */
    public static void wc(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "wc - word, line, character, and byte count",
            "Usage: wc [OPTIONS] [FILES]",
            "  -? --help                    Show help",
            "  -l --lines                   Print line counts",
            "  -c --bytes                   Print byte counts",
            "  -m --chars                   Print character counts",
            "  -w --words                   Print word counts",
        };
        Options opt = parseOptions(context, usage, argv);

        List<Source> sources = new ArrayList<>();
        if (opt.args().isEmpty()) {
            opt.args().add("-");
        }
        for (String arg : opt.args()) {
            if ("-".equals(arg)) {
                sources.add(new StdInSource(context.in()));
            } else {
                sources.add(
                        new URLSource(context.currentDir().resolve(arg).toUri().toURL(), arg));
            }
        }

        boolean showLines = opt.isSet("lines");
        boolean showWords = opt.isSet("words");
        boolean showChars = opt.isSet("chars");
        boolean showBytes = opt.isSet("bytes");

        // If no options specified, show all
        if (!showLines && !showWords && !showChars && !showBytes) {
            showLines = showWords = showBytes = true;
        }

        long totalLines = 0, totalWords = 0, totalChars = 0, totalBytes = 0;

        for (Source source : sources) {
            long lines = 0, words = 0, chars = 0, bytes = 0;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.read()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines++;
                    chars += line.length() + 1; // +1 for newline
                    bytes += line.getBytes().length + 1; // +1 for newline

                    // Count words
                    String[] wordArray = line.trim().split("\\s+");
                    if (wordArray.length == 1 && wordArray[0].isEmpty()) {
                        // Empty line
                    } else {
                        words += wordArray.length;
                    }
                }
            }

            totalLines += lines;
            totalWords += words;
            totalChars += chars;
            totalBytes += bytes;

            // Print results for this file
            StringBuilder result = new StringBuilder();
            if (showLines) result.append(String.format("%8d", lines));
            if (showWords) result.append(String.format("%8d", words));
            if (showChars) result.append(String.format("%8d", chars));
            if (showBytes) result.append(String.format("%8d", bytes));
            result.append(" ").append(source.getName());

            context.out().println(result);
        }

        // Print totals if multiple files
        if (sources.size() > 1) {
            StringBuilder result = new StringBuilder();
            if (showLines) result.append(String.format("%8d", totalLines));
            if (showWords) result.append(String.format("%8d", totalWords));
            if (showChars) result.append(String.format("%8d", totalChars));
            if (showBytes) result.append(String.format("%8d", totalBytes));
            result.append(" total");

            context.out().println(result);
        }
    }

    /**
     * Head command - display first lines of files.
     */
    public static void head(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "head - display first lines of files",
            "Usage: head [-n lines | -c bytes] [file ...]",
            "  -? --help                    Show help",
            "  -n --lines=LINES             Print line counts",
            "  -c --bytes=BYTES             Print byte counts",
        };
        Options opt = parseOptions(context, usage, argv);

        if (opt.isSet("lines") && opt.isSet("bytes")) {
            throw new IllegalArgumentException("usage: head [-n # | -c #] [file ...]");
        }

        int nbLines = Integer.MAX_VALUE;
        int nbBytes = Integer.MAX_VALUE;
        if (opt.isSet("lines")) {
            nbLines = opt.getNumber("lines");
        } else if (opt.isSet("bytes")) {
            nbBytes = opt.getNumber("bytes");
        } else {
            nbLines = 10; // default
        }

        List<String> args = opt.args();
        if (args.isEmpty()) {
            args = Collections.singletonList("-");
        }

        boolean first = true;
        for (String arg : args) {
            if (!first && args.size() > 1) {
                context.out().println();
            }
            if (args.size() > 1) {
                context.out().println("==> " + arg + " <==");
            }

            InputStream is;
            if ("-".equals(arg)) {
                is = context.in();
            } else {
                is = context.currentDir().resolve(arg).toUri().toURL().openStream();
            }

            if (nbLines != Integer.MAX_VALUE) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null && count < nbLines) {
                        context.out().println(line);
                        count++;
                    }
                }
            } else {
                byte[] buffer = new byte[nbBytes];
                int bytesRead = is.read(buffer);
                if (bytesRead > 0) {
                    context.out().write(buffer, 0, bytesRead);
                }
                is.close();
            }
            first = false;
        }
    }

    /**
     * Tail command - display last lines of files.
     */
    public static void tail(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "tail - display last lines of files",
            "Usage: tail [-f] [-q] [-c # | -n #] [file ...]",
            "  -? --help                    Show help",
            "  -f --follow                  Do not stop at end of file",
            "  -F --FOLLOW                  Follow and check for file renaming or rotation",
            "  -n --lines=LINES             Number of lines to print",
            "  -c --bytes=BYTES             Number of bytes to print",
        };
        Options opt = parseOptions(context, usage, argv);

        if (opt.isSet("lines") && opt.isSet("bytes")) {
            throw new IllegalArgumentException("usage: tail [-f] [-q] [-c # | -n #] [file ...]");
        }

        int lines = opt.isSet("lines") ? opt.getNumber("lines") : 10;
        int bytes = opt.isSet("bytes") ? opt.getNumber("bytes") : -1;
        boolean follow = opt.isSet("follow") || opt.isSet("FOLLOW");

        List<String> args = opt.args();
        if (args.isEmpty()) {
            args = Collections.singletonList("-");
        }

        for (String arg : args) {
            if (args.size() > 1) {
                context.out().println("==> " + arg + " <==");
            }

            if ("-".equals(arg)) {
                // For stdin, just read and buffer
                tailInputStream(context, context.in(), lines, bytes);
            } else {
                Path path = context.currentDir().resolve(arg);
                if (bytes > 0) {
                    tailFileBytes(context, path, bytes, follow);
                } else {
                    tailFileLines(context, path, lines, follow);
                }
            }
        }
    }

    private static void tailInputStream(Context context, InputStream is, int lines, int bytes) throws IOException {
        if (bytes > 0) {
            // Read all and keep last bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            byte[] data = baos.toByteArray();
            int start = Math.max(0, data.length - bytes);
            context.out().write(data, start, data.length - start);
        } else {
            // Read all and keep last lines
            List<String> allLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
            }
            int start = Math.max(0, allLines.size() - lines);
            for (int i = start; i < allLines.size(); i++) {
                context.out().println(allLines.get(i));
            }
        }
    }

    private static void tailFileLines(Context context, Path path, int lines, boolean follow) throws IOException {
        if (!Files.exists(path)) {
            context.err().println("tail: " + path + ": No such file or directory");
            return;
        }

        // Read last lines
        List<String> lastLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLines.add(line);
                if (lastLines.size() > lines) {
                    lastLines.remove(0);
                }
            }
        }

        for (String line : lastLines) {
            context.out().println(line);
        }

        // TODO: Implement follow mode with WatchService if needed
        if (follow) {
            context.err().println("tail: follow mode not yet implemented");
        }
    }

    private static void tailFileBytes(Context context, Path path, int bytes, boolean follow) throws IOException {
        if (!Files.exists(path)) {
            context.err().println("tail: " + path + ": No such file or directory");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = raf.length();
            long start = Math.max(0, fileLength - bytes);
            raf.seek(start);

            byte[] buffer = new byte[8192];
            int n;
            while ((n = raf.read(buffer)) != -1) {
                context.out().write(buffer, 0, n);
            }
        }

        if (follow) {
            context.err().println("tail: follow mode not yet implemented");
        }
    }

    /**
     * Grep command - search text patterns.
     */
    public static void grep(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "grep -  search for PATTERN in each FILE or standard input.",
            "Usage: grep [OPTIONS] PATTERN [FILES]",
            "  -? --help                Show help",
            "  -i --ignore-case         Ignore case distinctions",
            "  -n --line-number         Prefix each line with line number within its input file",
            "  -q --quiet, --silent     Suppress all normal output",
            "  -v --invert-match        Select non-matching lines",
            "  -w --word-regexp         Select only whole words",
            "  -x --line-regexp         Select only whole lines",
            "  -c --count               Only print a count of matching lines per file",
            "     --color=WHEN          Use markers to distinguish the matching string, may be `always', `never' or `auto'",
            "  -B --before-context=NUM  Print NUM lines of leading context before matching lines",
            "  -A --after-context=NUM   Print NUM lines of trailing context after matching lines",
            "  -C --context=NUM         Print NUM lines of output context",
            "     --pad-lines           Pad line numbers"
        };
        Options opt = parseOptions(context, usage, argv);

        Map<String, String> colorMap = getColorMap(context, "GREP", DEFAULT_GREP_COLORS);

        List<String> args = opt.args();
        if (args.isEmpty()) {
            throw new IllegalArgumentException("no pattern supplied");
        }

        String regex = args.remove(0);
        String regexp = regex;
        if (opt.isSet("word-regexp")) {
            regexp = "\\b" + regexp + "\\b";
        }
        if (opt.isSet("line-regexp")) {
            regexp = "^" + regexp + "$";
        } else {
            regexp = ".*" + regexp + ".*";
        }
        Pattern p;
        Pattern p2;
        if (opt.isSet("ignore-case")) {
            p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
            p2 = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } else {
            p = Pattern.compile(regexp);
            p2 = Pattern.compile(regex);
        }
        int after = opt.isSet("after-context") ? opt.getNumber("after-context") : -1;
        int before = opt.isSet("before-context") ? opt.getNumber("before-context") : -1;
        int contextLines = opt.isSet("context") ? opt.getNumber("context") : 0;
        String lineFmt = opt.isSet("pad-lines") ? "%6d" : "%d";
        if (after < 0) {
            after = contextLines;
        }
        if (before < 0) {
            before = contextLines;
        }
        boolean count = opt.isSet("count");
        boolean quiet = opt.isSet("quiet");
        boolean invert = opt.isSet("invert-match");
        boolean lineNumber = opt.isSet("line-number");
        String color = opt.isSet("color") ? opt.get("color") : "auto";
        boolean colored;
        switch (color) {
            case "always":
            case "yes":
            case "force":
                colored = true;
                break;
            case "never":
            case "no":
            case "none":
                colored = false;
                break;
            case "auto":
            case "tty":
            case "if-tty":
                colored = context.isTty();
                break;
            default:
                throw new IllegalArgumentException("invalid argument '" + color + "' for '--color'");
        }
        Map<String, String> colors =
                colored ? (colorMap != null ? colorMap : getColorMap(DEFAULT_GREP_COLORS)) : Collections.emptyMap();

        if (args.isEmpty()) {
            args.add("-");
        }
        List<GrepSource> sources = new ArrayList<>();
        for (String arg : args) {
            if ("-".equals(arg)) {
                sources.add(new GrepSource(context.in(), "(standard input)"));
            } else {
                Path path = context.currentDir().resolve(arg);
                sources.add(new GrepSource(path, arg));
            }
        }
        boolean match = false;
        for (GrepSource src : sources) {
            List<String> lines = new ArrayList<>();
            boolean firstPrint = true;
            int nb = 0;
            try (InputStream is = src.getInputStream()) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    int lineno = 1;
                    int lineMatch = 0;
                    while ((line = r.readLine()) != null) {
                        boolean matches = p.matcher(line).matches();
                        if (invert) {
                            matches = !matches;
                        }
                        AttributedStringBuilder sbl = new AttributedStringBuilder();
                        if (matches) {
                            nb++;
                            if (!count && !quiet) {
                                if (sources.size() > 1) {
                                    if (colored) {
                                        applyStyle(sbl, colors, "fn");
                                    }
                                    sbl.append(src.getName());
                                    if (colored) {
                                        applyStyle(sbl, colors, "se");
                                    }
                                    sbl.append(":");
                                }
                                if (lineNumber) {
                                    if (colored) {
                                        applyStyle(sbl, colors, "ln");
                                    }
                                    sbl.append(String.format(lineFmt, lineno));
                                    if (colored) {
                                        applyStyle(sbl, colors, "se");
                                    }
                                    sbl.append(":");
                                }
                                if (colored) {
                                    Matcher matcher2 = p2.matcher(line);
                                    int cur = 0;
                                    while (matcher2.find()) {
                                        sbl.append(line, cur, matcher2.start());
                                        applyStyle(sbl, colors, "ms");
                                        sbl.append(line, matcher2.start(), matcher2.end());
                                        applyStyle(sbl, colors, "se");
                                        cur = matcher2.end();
                                    }
                                    sbl.append(line, cur, line.length());
                                } else {
                                    sbl.append(line);
                                }
                                lineMatch = before + 1;
                            }
                        } else if (lineMatch > 0) {
                            lineMatch--;
                            if (sources.size() > 1) {
                                if (colored) {
                                    applyStyle(sbl, colors, "fn");
                                }
                                sbl.append(src.getName());
                                if (colored) {
                                    applyStyle(sbl, colors, "se");
                                }
                                sbl.append("-");
                            }
                            if (lineNumber) {
                                if (colored) {
                                    applyStyle(sbl, colors, "ln");
                                }
                                sbl.append(String.format(lineFmt, lineno));
                                if (colored) {
                                    applyStyle(sbl, colors, "se");
                                }
                                sbl.append("-");
                            }
                            sbl.append(line);
                        } else {
                            if (sources.size() > 1) {
                                if (colored) {
                                    applyStyle(sbl, colors, "fn");
                                }
                                sbl.append(src.getName());
                                if (colored) {
                                    applyStyle(sbl, colors, "se");
                                }
                                sbl.append("-");
                            }
                            if (lineNumber) {
                                if (colored) {
                                    applyStyle(sbl, colors, "ln");
                                }
                                sbl.append(String.format(lineFmt, lineno));
                                if (colored) {
                                    applyStyle(sbl, colors, "se");
                                }
                                sbl.append("-");
                            }
                            sbl.append(line);
                            while (lines.size() > before) {
                                lines.remove(0);
                            }
                            lineMatch = 0;
                        }
                        lines.add(sbl.toAnsi(context.terminal()));
                        while (lineMatch == 0 && lines.size() > before) {
                            lines.remove(0);
                        }
                        lineno++;
                    }
                    if (!count && lineMatch > 0) {
                        if (!firstPrint && before + after > 0) {
                            AttributedStringBuilder sbl2 = new AttributedStringBuilder();
                            if (colored) {
                                applyStyle(sbl2, colors, "se");
                            }
                            sbl2.append("--");
                            context.out().println(sbl2.toAnsi(context.terminal()));
                        } else {
                            firstPrint = false;
                        }
                        for (int i = 0; i < lineMatch + after && i < lines.size(); i++) {
                            context.out().println(lines.get(i));
                        }
                    }
                    if (count) {
                        context.out().println(nb);
                    }
                    match |= nb > 0;
                }
            }
        }
    }

    // Old grepFile method removed - functionality integrated into main grep method

    /**
     * Sort command - sort lines of text.
     */
    public static void sort(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "sort -  writes sorted standard input to standard output.",
            "Usage: sort [OPTIONS] [FILES]",
            "  -? --help                    show help",
            "  -f --ignore-case             fold lower case to upper case characters",
            "  -r --reverse                 reverse the result of comparisons",
            "  -u --unique                  output only the first of an equal run",
            "  -t --field-separator=SEP     use SEP instead of non-blank to blank transition",
            "  -b --ignore-leading-blanks   ignore leading blancks",
            "     --numeric-sort            compare according to string numerical value",
            "  -k --key=KEY                 fields to use for sorting separated by whitespaces"
        };

        Options opt = parseOptions(context, usage, argv);

        List<String> args = opt.args();

        List<String> lines = new ArrayList<>();
        if (!args.isEmpty()) {
            for (String filename : args) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        context.currentDir().toUri().resolve(filename).toURL().openStream()))) {
                    readLines(reader, lines);
                }
            }
        } else {
            BufferedReader r = new BufferedReader(new InputStreamReader(context.in()));
            readLines(r, lines);
        }

        String separator = opt.get("field-separator");
        boolean caseInsensitive = opt.isSet("ignore-case");
        boolean reverse = opt.isSet("reverse");
        boolean ignoreBlanks = opt.isSet("ignore-leading-blanks");
        boolean numeric = opt.isSet("numeric-sort");
        boolean unique = opt.isSet("unique");
        List<String> sortFields = opt.getList("key");

        char sep = (separator == null || separator.length() == 0) ? '\0' : separator.charAt(0);
        lines.sort(new SortComparator(caseInsensitive, reverse, ignoreBlanks, numeric, sep, sortFields));
        String last = null;
        for (String s : lines) {
            if (!unique || last == null || !s.equals(last)) {
                context.out().println(s);
            }
            last = s;
        }
    }

    /**
     * List directory contents command.
     */
    public static void ls(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "ls - list files",
            "Usage: ls [OPTIONS] [PATTERNS...]",
            "  -? --help                show help",
            "  -1                       list one entry per line",
            "  -C                       multi-column output",
            "     --color=WHEN          colorize the output, may be `always', `never' or `auto'",
            "  -a                       list entries starting with .",
            "  -F                       append file type indicators",
            "  -m                       comma separated",
            "  -l                       long listing",
            "  -S                       sort by size",
            "  -f                       output is not sorted",
            "  -r                       reverse sort order",
            "  -t                       sort by modification time",
            "  -x                       sort horizontally",
            "  -L                       list referenced file for links",
            "  -h                       print sizes in human readable form"
        };
        Options opt = parseOptions(context, usage, argv);

        Map<String, String> colorMap = getLsColorMap(context);

        String color = opt.isSet("color") ? opt.get("color") : "auto";
        boolean colored;
        switch (color) {
            case "always":
            case "yes":
            case "force":
                colored = true;
                break;
            case "never":
            case "no":
            case "none":
                colored = false;
                break;
            case "auto":
            case "tty":
            case "if-tty":
                colored = context.isTty();
                break;
            default:
                throw new IllegalArgumentException("invalid argument '" + color + "' for '--color'");
        }
        Map<String, String> colors =
                colored ? (colorMap != null ? colorMap : getLsColorMap(DEFAULT_LS_COLORS)) : Collections.emptyMap();

        class PathEntry implements Comparable<PathEntry> {
            final Path abs;
            final Path path;
            final Map<String, Object> attributes;

            public PathEntry(Path abs, Path root) {
                this.abs = abs;
                this.path = abs.startsWith(root) ? root.relativize(abs) : abs;
                this.attributes = readAttributes(abs);
            }

            @Override
            public int compareTo(PathEntry o) {
                int c = doCompare(o);
                return opt.isSet("r") ? -c : c;
            }

            private int doCompare(PathEntry o) {
                if (opt.isSet("f")) {
                    return -1;
                }
                if (opt.isSet("S")) {
                    long s0 = attributes.get("size") != null ? ((Number) attributes.get("size")).longValue() : 0L;
                    long s1 = o.attributes.get("size") != null ? ((Number) o.attributes.get("size")).longValue() : 0L;
                    return s0 > s1 ? -1 : s0 < s1 ? 1 : path.toString().compareTo(o.path.toString());
                }
                if (opt.isSet("t")) {
                    long t0 = attributes.get("lastModifiedTime") != null
                            ? ((FileTime) attributes.get("lastModifiedTime")).toMillis()
                            : 0L;
                    long t1 = o.attributes.get("lastModifiedTime") != null
                            ? ((FileTime) o.attributes.get("lastModifiedTime")).toMillis()
                            : 0L;
                    return t0 > t1 ? -1 : t0 < t1 ? 1 : path.toString().compareTo(o.path.toString());
                }
                return path.toString().compareTo(o.path.toString());
            }

            boolean isNotDirectory() {
                return is("isRegularFile") || is("isSymbolicLink") || is("isOther");
            }

            boolean isDirectory() {
                return is("isDirectory");
            }

            private boolean is(String attr) {
                Object d = attributes.get(attr);
                return d instanceof Boolean && (Boolean) d;
            }

            String display() {
                String type;
                String suffix;
                String link = "";
                if (is("isSymbolicLink")) {
                    type = "sl";
                    suffix = "@";
                    try {
                        Path l = Files.readSymbolicLink(abs);
                        link = " -> " + l.toString();
                    } catch (IOException e) {
                        // ignore
                    }
                } else if (is("isDirectory")) {
                    type = "dr";
                    suffix = "/";
                } else if (is("isExecutable")) {
                    type = "ex";
                    suffix = "*";
                } else if (is("isOther")) {
                    type = "ot";
                    suffix = "";
                } else {
                    type = "";
                    suffix = "";
                }
                boolean addSuffix = opt.isSet("F");
                return applyStyle(path.toString(), colors, type) + (addSuffix ? suffix : "") + link;
            }

            String longDisplay() {
                String username;
                if (attributes.containsKey("owner")) {
                    username = Objects.toString(attributes.get("owner"), null);
                } else {
                    username = "owner";
                }
                if (username.length() > 8) {
                    username = username.substring(0, 8);
                } else {
                    for (int i = username.length(); i < 8; i++) {
                        username = username + " ";
                    }
                }
                String group;
                if (attributes.containsKey("group")) {
                    group = Objects.toString(attributes.get("group"), null);
                } else {
                    group = "group";
                }
                if (group.length() > 8) {
                    group = group.substring(0, 8);
                } else {
                    for (int i = group.length(); i < 8; i++) {
                        group = group + " ";
                    }
                }
                Number length = (Number) attributes.get("size");
                if (length == null) {
                    length = 0L;
                }
                String lengthString;
                if (opt.isSet("h")) {
                    double l = length.longValue();
                    String unit = "B";
                    if (l >= 1000) {
                        l /= 1024;
                        unit = "K";
                        if (l >= 1000) {
                            l /= 1024;
                            unit = "M";
                            if (l >= 1000) {
                                l /= 1024;
                                unit = "T";
                            }
                        }
                    }
                    if (l < 10 && length.longValue() > 1000) {
                        lengthString = String.format("%.1f", l) + unit;
                    } else {
                        lengthString = String.format("%3.0f", l) + unit;
                    }
                } else {
                    lengthString = String.format("%1$8s", length);
                }
                @SuppressWarnings("unchecked")
                Set<PosixFilePermission> perms = (Set<PosixFilePermission>) attributes.get("permissions");
                if (perms == null) {
                    perms = EnumSet.noneOf(PosixFilePermission.class);
                }
                // TODO: all fields should be padded to align
                return (is("isDirectory") ? "d" : (is("isSymbolicLink") ? "l" : (is("isOther") ? "o" : "-")))
                        + PosixFilePermissions.toString(perms) + " "
                        + String.format(
                                "%3s",
                                (attributes.containsKey("nlink")
                                        ? attributes.get("nlink").toString()
                                        : "1"))
                        + " " + username + " " + group + " " + lengthString + " "
                        + toString((FileTime) attributes.get("lastModifiedTime"))
                        + " " + display();
            }

            protected String toString(FileTime time) {
                long millis = (time != null) ? time.toMillis() : -1L;
                if (millis < 0L) {
                    return "------------";
                }
                ZonedDateTime dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
                // Less than six months
                if (System.currentTimeMillis() - millis < 183L * 24L * 60L * 60L * 1000L) {
                    return DateTimeFormatter.ofPattern("MMM ppd HH:mm").format(dt);
                }
                // Older than six months
                else {
                    return DateTimeFormatter.ofPattern("MMM ppd  yyyy").format(dt);
                }
            }

            protected Map<String, Object> readAttributes(Path path) {
                Map<String, Object> attrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                for (String view : path.getFileSystem().supportedFileAttributeViews()) {
                    try {
                        Map<String, Object> ta =
                                Files.readAttributes(path, view + ":*", getLinkOptions(opt.isSet("L")));
                        ta.forEach(attrs::putIfAbsent);
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                attrs.computeIfAbsent("isExecutable", s -> Files.isExecutable(path));
                attrs.computeIfAbsent("permissions", s -> getPermissionsFromFile(path));
                return attrs;
            }
        }

        Path currentDir = context.currentDir();
        // Listing
        List<Path> expanded = new ArrayList<>();
        if (opt.args().isEmpty()) {
            expanded.add(currentDir);
        } else {
            opt.args().forEach(s -> expanded.add(currentDir.resolve(s)));
        }
        boolean listAll = opt.isSet("a");
        Predicate<Path> filter = p -> listAll
                || p.getFileName().toString().equals(".")
                || p.getFileName().toString().equals("..")
                || !p.getFileName().toString().startsWith(".");
        List<PathEntry> all = expanded.stream()
                .filter(filter)
                .map(p -> new PathEntry(p, currentDir))
                .sorted()
                .collect(Collectors.toList());
        // Print files first
        List<PathEntry> files = all.stream().filter(PathEntry::isNotDirectory).collect(Collectors.toList());
        PrintStream out = context.out();
        Consumer<Stream<PathEntry>> display = s -> {
            boolean optLine = opt.isSet("1");
            boolean optComma = opt.isSet("m");
            boolean optLong = opt.isSet("l");
            boolean optCol = opt.isSet("C");
            if (!optLine && !optComma && !optLong && !optCol) {
                if (context.isTty()) {
                    optCol = true;
                } else {
                    optLine = true;
                }
            }
            // One entry per line
            if (optLine) {
                s.map(PathEntry::display).forEach(out::println);
            }
            // Comma separated list
            else if (optComma) {
                out.println(s.map(PathEntry::display).collect(Collectors.joining(", ")));
            }
            // Long listing
            else if (optLong) {
                s.map(PathEntry::longDisplay).forEach(out::println);
            }
            // Column listing
            else if (optCol) {
                toColumn(context, out, s.map(PathEntry::display), opt.isSet("x"));
            }
        };
        boolean space = false;
        if (!files.isEmpty()) {
            display.accept(files.stream());
            space = true;
        }
        // Print directories
        List<PathEntry> directories =
                all.stream().filter(PathEntry::isDirectory).collect(Collectors.toList());
        for (PathEntry entry : directories) {
            if (space) {
                out.println();
            }
            space = true;
            Path path = currentDir.resolve(entry.path);
            if (expanded.size() > 1) {
                out.println(currentDir.relativize(path).toString() + ":");
            }
            try (Stream<Path> pathStream = Files.list(path)) {
                display.accept(Stream.concat(Stream.of(".", "..").map(path::resolve), pathStream)
                        .filter(filter)
                        .map(p -> new PathEntry(p, path))
                        .sorted());
            }
        }
    }

    private static void toColumn(Context context, PrintStream out, Stream<String> ansi, boolean horizontal) {
        Terminal terminal = context.terminal();
        int width = context.isTty() ? terminal.getWidth() : 80;
        List<AttributedString> strings = ansi.map(AttributedString::fromAnsi).collect(Collectors.toList());
        if (!strings.isEmpty()) {
            int max = strings.stream()
                    .mapToInt(AttributedString::columnLength)
                    .max()
                    .getAsInt();
            int c = Math.max(1, width / max);
            while (c > 1 && c * max + (c - 1) >= width) {
                c--;
            }
            int columns = c;
            int lines = (strings.size() + columns - 1) / columns;
            IntBinaryOperator index;
            if (horizontal) {
                index = (i, j) -> i * columns + j;
            } else {
                index = (i, j) -> j * lines + i;
            }
            AttributedStringBuilder sb = new AttributedStringBuilder();
            for (int i = 0; i < lines; i++) {
                for (int j = 0; j < columns; j++) {
                    int idx = index.applyAsInt(i, j);
                    if (idx < strings.size()) {
                        AttributedString str = strings.get(idx);
                        boolean hasRightItem = j < columns - 1 && index.applyAsInt(i, j + 1) < strings.size();
                        sb.append(str);
                        if (hasRightItem) {
                            for (int k = 0; k <= max - str.length(); k++) {
                                sb.append(' ');
                            }
                        }
                    }
                }
                sb.append('\n');
            }
            out.print(sb.toAnsi(terminal));
        }
    }

    private static String formatHumanReadable(long bytes) {
        if (bytes < 1024) return bytes + "B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f%s", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Read lines from a BufferedReader into a list.
     */
    private static void readLines(BufferedReader reader, List<String> lines) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
    }

    // Color constants and helper methods
    public static final String DEFAULT_LS_COLORS = "dr=1;91:ex=1;92:sl=1;96:ot=34;43";
    public static final String DEFAULT_GREP_COLORS = "mt=1;31:fn=35:ln=32:se=36";

    private static final LinkOption[] NO_FOLLOW_OPTIONS = new LinkOption[] {LinkOption.NOFOLLOW_LINKS};
    private static final List<String> WINDOWS_EXECUTABLE_EXTENSIONS =
            Collections.unmodifiableList(Arrays.asList(".bat", ".exe", ".cmd"));
    private static final LinkOption[] EMPTY_LINK_OPTIONS = new LinkOption[0];

    /**
     * Get color map for ls command.
     */
    public static Map<String, String> getLsColorMap(String colorString) {
        return getColorMap(colorString != null ? colorString : DEFAULT_LS_COLORS);
    }

    /**
     * Get color map from color string.
     */
    public static Map<String, String> getColorMap(String colorString) {
        String str = colorString != null ? colorString : "";
        if (str.isEmpty()) {
            return Collections.emptyMap();
        }
        String sep = str.matches("[a-z]{2}=[0-9]*(;[0-9]+)*(:[a-z]{2}=[0-9]*(;[0-9]+)*)*") ? ":" : " ";
        return Arrays.stream(str.split(sep))
                .collect(Collectors.toMap(s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1)));
    }

    public static Map<String, String> getLsColorMap(Context session) {
        return getColorMap(session, "LS", DEFAULT_LS_COLORS);
    }

    public static Map<String, String> getColorMap(Context session, String name, String def) {
        return getColorMap(session::get, name, def);
    }

    public static Map<String, String> getColorMap(Function<String, Object> variables, String name, String def) {
        Object obj = variables.apply(name + "_COLORS");
        String str = obj != null ? obj.toString() : null;
        if (str == null) {
            str = def;
        }
        String sep = str.matches("[a-z]{2}=[0-9]*(;[0-9]+)*(:[a-z]{2}=[0-9]*(;[0-9]+)*)*") ? ":" : " ";
        return Arrays.stream(str.split(sep))
                .collect(Collectors.toMap(s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1)));
    }

    /**
     * Apply style to text using color map.
     */
    public static String applyStyle(String text, Map<String, String> colors, String... types) {
        String t = null;
        for (String type : types) {
            if (colors.get(type) != null) {
                t = type;
                break;
            }
        }
        return new AttributedString(text, new StyleResolver(colors::get).resolve("." + t)).toAnsi();
    }

    /**
     * Apply style to AttributedStringBuilder using color map.
     */
    public static void applyStyle(AttributedStringBuilder sb, Map<String, String> colors, String... types) {
        String t = null;
        for (String type : types) {
            if (colors.get(type) != null) {
                t = type;
                break;
            }
        }
        sb.style(new StyleResolver(colors::get).resolve("." + t));
    }

    /**
     * Get link options based on whether to follow links.
     */
    private static LinkOption[] getLinkOptions(boolean followLinks) {
        if (followLinks) {
            return EMPTY_LINK_OPTIONS;
        } else { // return a clone that modifications to the array will not affect others
            return NO_FOLLOW_OPTIONS.clone();
        }
    }

    /**
     * Check if a file name is a Windows executable.
     */
    private static boolean isWindowsExecutable(String fileName) {
        if ((fileName == null) || (fileName.length() <= 0)) {
            return false;
        }
        for (String suffix : WINDOWS_EXECUTABLE_EXTENSIONS) {
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get POSIX file permissions from a file, with Windows executable handling.
     */
    private static Set<PosixFilePermission> getPermissionsFromFile(Path f) {
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(f);
            if (OSUtils.IS_WINDOWS && isWindowsExecutable(f.getFileName().toString())) {
                perms = new HashSet<>(perms);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
            }
            return perms;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Comparator for sorting strings with various options.
     */
    public static class SortComparator implements Comparator<String> {

        private static Pattern fpPattern;

        static {
            final String Digits = "(\\p{Digit}+)";
            final String HexDigits = "(\\p{XDigit}+)";
            final String Exp = "[eE][+-]?" + Digits;
            final String fpRegex = "([\\x00-\\x20]*[+-]?(NaN|Infinity|(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp
                    + ")?)|(\\.(" + Digits + ")(" + Exp + ")?)|(((0[xX]" + HexDigits + "(\\.)?)|(0[xX]" + HexDigits
                    + "?(\\.)" + HexDigits + "))[pP][+-]?" + Digits + "))" + "[fFdD]?))[\\x00-\\x20]*)(.*)";
            fpPattern = Pattern.compile(fpRegex);
        }

        private boolean caseInsensitive;
        private boolean reverse;
        private boolean ignoreBlanks;
        private boolean numeric;
        private char separator;
        private List<Key> sortKeys;

        @SuppressWarnings("this-escape")
        public SortComparator(
                boolean caseInsensitive,
                boolean reverse,
                boolean ignoreBlanks,
                boolean numeric,
                char separator,
                List<String> sortFields) {
            this.caseInsensitive = caseInsensitive;
            this.reverse = reverse;
            this.separator = separator;
            this.ignoreBlanks = ignoreBlanks;
            this.numeric = numeric;
            if (sortFields == null || sortFields.isEmpty()) {
                sortFields = new ArrayList<>();
                sortFields.add("1");
            }
            sortKeys = sortFields.stream().map(Key::new).collect(Collectors.toList());
        }

        public int compare(String o1, String o2) {
            int res = 0;

            List<Integer> fi1 = getFieldIndexes(o1);
            List<Integer> fi2 = getFieldIndexes(o2);
            for (Key key : sortKeys) {
                int[] k1 = getSortKey(o1, fi1, key);
                int[] k2 = getSortKey(o2, fi2, key);
                if (key.numeric) {
                    Double d1 = getDouble(o1, k1[0], k1[1]);
                    Double d2 = getDouble(o2, k2[0], k2[1]);
                    res = d1.compareTo(d2);
                } else {
                    res = compareRegion(o1, k1[0], k1[1], o2, k2[0], k2[1], key.caseInsensitive);
                }
                if (res != 0) {
                    if (key.reverse) {
                        res = -res;
                    }
                    break;
                }
            }
            return res;
        }

        protected Double getDouble(String s, int start, int end) {
            String field = s.substring(start, end);
            Matcher m = fpPattern.matcher(field);
            if (m.find()) {
                return Double.valueOf(field.substring(m.start(1), m.end(1)));
            }
            // If no valid number found, return 0.0
            return 0.0;
        }

        protected int compareRegion(
                String s1, int start1, int end1, String s2, int start2, int end2, boolean caseInsensitive) {
            for (int i1 = start1, i2 = start2; i1 < end1 && i2 < end2; i1++, i2++) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                if (c1 != c2) {
                    if (caseInsensitive) {
                        c1 = Character.toUpperCase(c1);
                        c2 = Character.toUpperCase(c2);
                        if (c1 != c2) {
                            c1 = Character.toLowerCase(c1);
                            c2 = Character.toLowerCase(c2);
                            if (c1 != c2) {
                                return c1 - c2;
                            }
                        }
                    } else {
                        return c1 - c2;
                    }
                }
            }
            return end1 - end2;
        }

        protected int[] getSortKey(String str, List<Integer> fields, Key key) {
            int start;
            int end;
            if (key.startField * 2 <= fields.size()) {
                start = fields.get((key.startField - 1) * 2);
                if (key.ignoreBlanksStart) {
                    while (start < fields.get((key.startField - 1) * 2 + 1)
                            && Character.isWhitespace(str.charAt(start))) {
                        start++;
                    }
                }
                if (key.startChar > 0) {
                    start = Math.min(start + key.startChar - 1, fields.get((key.startField - 1) * 2 + 1));
                }
            } else {
                start = 0;
            }
            if (key.endField > 0 && key.endField * 2 <= fields.size()) {
                end = fields.get((key.endField - 1) * 2);
                if (key.ignoreBlanksEnd) {
                    while (end < fields.get((key.endField - 1) * 2 + 1) && Character.isWhitespace(str.charAt(end))) {
                        end++;
                    }
                }
                if (key.endChar > 0) {
                    end = Math.min(end + key.endChar - 1, fields.get((key.endField - 1) * 2 + 1));
                }
            } else {
                end = str.length();
            }
            return new int[] {start, end};
        }

        protected List<Integer> getFieldIndexes(String o) {
            List<Integer> fields = new ArrayList<>();
            if (o.length() > 0) {
                if (separator == '\0') {
                    fields.add(0);
                    for (int idx = 1; idx < o.length(); idx++) {
                        if (Character.isWhitespace(o.charAt(idx)) && !Character.isWhitespace(o.charAt(idx - 1))) {
                            fields.add(idx - 1);
                            fields.add(idx);
                        }
                    }
                    fields.add(o.length() - 1);
                } else {
                    int last = -1;
                    for (int idx = o.indexOf(separator); idx >= 0; idx = o.indexOf(separator, idx + 1)) {
                        if (last >= 0) {
                            fields.add(last);
                            fields.add(idx - 1);
                        } else if (idx > 0) {
                            fields.add(0);
                            fields.add(idx - 1);
                        }
                        last = idx + 1;
                    }
                    if (last < o.length()) {
                        fields.add(Math.max(last, 0));
                        fields.add(o.length() - 1);
                    }
                }
            }
            return fields;
        }

        public class Key {
            int startField;
            int startChar;
            int endField;
            int endChar;
            boolean ignoreBlanksStart;
            boolean ignoreBlanksEnd;
            boolean caseInsensitive;
            boolean reverse;
            boolean numeric;

            public Key(String str) {
                boolean modifiers = false;
                boolean startPart = true;
                boolean inField = true;
                boolean inChar = false;
                for (char c : str.toCharArray()) {
                    switch (c) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            if (!inField && !inChar) {
                                throw new IllegalArgumentException("Bad field syntax: " + str);
                            }
                            if (startPart) {
                                if (inChar) {
                                    startChar = startChar * 10 + (c - '0');
                                } else {
                                    startField = startField * 10 + (c - '0');
                                }
                            } else {
                                if (inChar) {
                                    endChar = endChar * 10 + (c - '0');
                                } else {
                                    endField = endField * 10 + (c - '0');
                                }
                            }
                            break;
                        case '.':
                            if (!inField) {
                                throw new IllegalArgumentException("Bad field syntax: " + str);
                            }
                            inField = false;
                            inChar = true;
                            break;
                        case 'n':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            numeric = true;
                            break;
                        case 'f':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            caseInsensitive = true;
                            break;
                        case 'r':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            reverse = true;
                            break;
                        case 'b':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            if (startPart) {
                                ignoreBlanksStart = true;
                            } else {
                                ignoreBlanksEnd = true;
                            }
                            break;
                        case ',':
                            inField = true;
                            inChar = false;
                            startPart = false;
                            break;
                        default:
                            throw new IllegalArgumentException("Bad field syntax: " + str);
                    }
                }
                if (!modifiers) {
                    ignoreBlanksStart = ignoreBlanksEnd = SortComparator.this.ignoreBlanks;
                    reverse = SortComparator.this.reverse;
                    caseInsensitive = SortComparator.this.caseInsensitive;
                    numeric = SortComparator.this.numeric;
                }
                if (startField < 1) {
                    throw new IllegalArgumentException("Bad field syntax: " + str);
                }
            }
        }
    }

    /**
     * Simple source abstraction for grep command.
     */
    private static class GrepSource {
        private final InputStream inputStream;
        private final Path path;
        private final String name;

        public GrepSource(InputStream inputStream, String name) {
            this.inputStream = inputStream;
            this.path = null;
            this.name = name;
        }

        public GrepSource(Path path, String name) {
            this.inputStream = null;
            this.path = path;
            this.name = name;
        }

        public InputStream getInputStream() throws IOException {
            if (inputStream != null) {
                return inputStream;
            } else {
                return path.toUri().toURL().openStream();
            }
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Interface for executing shell commands in watch.
     * <p>
     * This interface allows the watch command to execute commands using
     * different execution strategies. Implementations can provide shell
     * integration, process execution, or other command execution mechanisms.
     * </p>
     * <p>
     * Example implementation for shell integration:
     * </p>
     * <pre>{@code
     * CommandExecutor executor = command -> {
     *     Object result = session.execute(String.join(" ", command));
     *     return result != null ? result.toString() : "";
     * };
     * }</pre>
     */
    public interface CommandExecutor {
        /**
         * Execute a command and return its output.
         * <p>
         * The command is provided as a list of strings where the first element
         * is the command name and subsequent elements are arguments.
         * </p>
         *
         * @param command the command to execute as a list of strings
         * @return the command output as a string
         * @throws Exception if execution fails
         */
        String execute(List<String> command) throws Exception;
    }

    protected static Options parseOptions(Context context, String[] usage, Object[] argv) throws Exception {
        Options opt = Options.compile(usage, s -> get(context, s)).parse(argv, true);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        return opt;
    }

    protected static String get(Context context, String name) {
        Object o = context.get(name);
        return o != null ? o.toString() : null;
    }
}
