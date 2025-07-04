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
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
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
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;

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

        public Context(InputStream in, PrintStream out, PrintStream err, Path currentDir, Terminal terminal) {
            this.in = in;
            this.out = out;
            this.err = err;
            this.currentDir = currentDir;
            this.terminal = terminal;
        }

        public InputStream in() { return in; }
        public PrintStream out() { return out; }
        public PrintStream err() { return err; }
        public Path currentDir() { return currentDir; }
        public Terminal terminal() { return terminal; }
        public boolean isTty() { return terminal != null; }
    }

    /**
     * Print working directory command.
     */
    public static void pwd(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "pwd - print working directory", 
            "Usage: pwd [OPTIONS]", 
            "  -? --help                show help"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        if (!opt.args().isEmpty()) {
            throw new IllegalArgumentException("usage: pwd");
        }
        context.out().println(context.currentDir());
    }

    /**
     * Echo command - display text.
     */
    public static void echo(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "echo - display text",
            "Usage: echo [OPTIONS] [ARGUMENTS]",
            "  -? --help                show help",
            "  -n                       no trailing new line"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        
        List<String> args = opt.args();
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            for (String arg : args) {
                if (buf.length() > 0) buf.append(' ');
                buf.append(arg);
            }
        }
        if (opt.isSet("n")) {
            context.out().print(buf.toString());
        } else {
            context.out().println(buf.toString());
        }
    }

    /**
     * Cat command - concatenate and print files.
     */
    public static void cat(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "cat - concatenate and print FILES",
            "Usage: cat [OPTIONS] [FILES]",
            "  -? --help                show help",
            "  -n                       number the output lines, starting at 1"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        
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
     */
    public static void date(Context context, String[] argv) throws Exception {
        String[] usage = {
            "date - display date",
            "Usage: date [-u] [+format]",
            "  -? --help                    Show help",
            "  -u                           Use UTC"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        ZoneId zone = opt.isSet("u") ? ZoneId.of("UTC") : ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        
        String format = null;
        if (!opt.args().isEmpty()) {
            String arg = opt.args().get(0);
            if (arg.startsWith("+")) {
                format = arg.substring(1);
            }
        }
        
        if (format != null) {
            // Simple format support - just a few common patterns
            format = format.replace("%Y", "yyyy")
                          .replace("%m", "MM")
                          .replace("%d", "dd")
                          .replace("%H", "HH")
                          .replace("%M", "mm")
                          .replace("%S", "ss");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            context.out().println(now.format(formatter));
        } else {
            // Default format
            context.out().println(now.format(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy")));
        }
    }

    /**
     * Sleep command - suspend execution.
     */
    public static void sleep(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "sleep - suspend execution for an interval of time",
            "Usage: sleep seconds",
            "  -? --help                    show help"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        
        List<String> args = opt.args();
        if (args.size() != 1) {
            throw new IllegalArgumentException("usage: sleep seconds");
        } else {
            int s = Integer.parseInt(args.get(0));
            Thread.sleep(s * 1000L);
        }
    }

    /**
     * Clear command - clear terminal screen.
     */
    public static void clear(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "clear - clear screen",
            "Usage: clear [OPTIONS]",
            "  -? --help                    Show help",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

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
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        List<Source> sources = new ArrayList<>();
        if (opt.args().isEmpty()) {
            opt.args().add("-");
        }
        for (String arg : opt.args()) {
            if ("-".equals(arg)) {
                sources.add(new StdInSource(context.in()));
            } else {
                sources.add(new URLSource(context.currentDir().resolve(arg).toUri().toURL(), arg));
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

            context.out().println(result.toString());
        }

        // Print totals if multiple files
        if (sources.size() > 1) {
            StringBuilder result = new StringBuilder();
            if (showLines) result.append(String.format("%8d", totalLines));
            if (showWords) result.append(String.format("%8d", totalWords));
            if (showChars) result.append(String.format("%8d", totalChars));
            if (showBytes) result.append(String.format("%8d", totalBytes));
            result.append(" total");

            context.out().println(result.toString());
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
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

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
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

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
            "grep - search text patterns",
            "Usage: grep [OPTIONS] PATTERN [FILES]",
            "  -? --help                    Show help",
            "  -i --ignore-case             Ignore case distinctions",
            "  -n --line-number             Print line numbers with output lines",
            "  -v --invert-match            Invert the sense of matching",
            "  -c --count                   Print only a count of matching lines",
            "  -l --files-with-matches      Print only names of files with matches",
            "  -L --files-without-match     Print only names of files without matches",
            "  -H --with-filename           Print the file name for each match",
            "  -h --no-filename             Suppress the file name prefix on output",
            "  -q --quiet                   Suppress all normal output",
            "  -r --recursive               Read all files under each directory recursively",
            "  -E --extended-regexp         Interpret PATTERN as an extended regular expression",
            "  -F --fixed-strings           Interpret PATTERN as a list of fixed strings",
            "  -w --word-regexp             Force PATTERN to match only whole words",
            "  -x --line-regexp             Force PATTERN to match only whole lines",
            "  -B --before-context=NUM      Print NUM lines of leading context before matching lines",
            "  -A --after-context=NUM       Print NUM lines of trailing context after matching lines",
            "  -C --context=NUM             Print NUM lines of output context",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        List<String> args = opt.args();
        if (args.isEmpty()) {
            throw new IllegalArgumentException("no pattern supplied");
        }

        String patternStr = args.get(0);
        List<String> files = args.subList(1, args.size());

        if (files.isEmpty()) {
            files = Collections.singletonList("-");
        }

        // Build pattern
        int flags = 0;
        if (opt.isSet("ignore-case")) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        Pattern pattern;
        if (opt.isSet("fixed-strings")) {
            pattern = Pattern.compile(Pattern.quote(patternStr), flags);
        } else if (opt.isSet("word-regexp")) {
            pattern = Pattern.compile("\\b" + patternStr + "\\b", flags);
        } else if (opt.isSet("line-regexp")) {
            pattern = Pattern.compile("^" + patternStr + "$", flags);
        } else {
            pattern = Pattern.compile(patternStr, flags);
        }

        boolean showLineNumbers = opt.isSet("line-number");
        boolean invertMatch = opt.isSet("invert-match");
        boolean countOnly = opt.isSet("count");
        boolean filesWithMatches = opt.isSet("files-with-matches");
        boolean filesWithoutMatch = opt.isSet("files-without-match");
        boolean showFilename = opt.isSet("with-filename") || files.size() > 1;
        boolean hideFilename = opt.isSet("no-filename");
        boolean quiet = opt.isSet("quiet");

        if (hideFilename) showFilename = false;

        for (String file : files) {
            grepFile(context, file, pattern, showLineNumbers, invertMatch, countOnly,
                    filesWithMatches, filesWithoutMatch, showFilename, quiet);
        }
    }

    private static void grepFile(Context context, String filename, Pattern pattern,
                                boolean showLineNumbers, boolean invertMatch, boolean countOnly,
                                boolean filesWithMatches, boolean filesWithoutMatch,
                                boolean showFilename, boolean quiet) throws IOException {
        InputStream is;
        if ("-".equals(filename)) {
            is = context.in();
            filename = "(standard input)";
        } else {
            try {
                is = context.currentDir().resolve(filename).toUri().toURL().openStream();
            } catch (MalformedURLException e) {
                context.err().println("grep: " + filename + ": " + e.getMessage());
                return;
            }
        }

        int matchCount = 0;
        int lineNumber = 0;
        boolean hasMatches = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                boolean matches = pattern.matcher(line).find();
                if (invertMatch) matches = !matches;

                if (matches) {
                    matchCount++;
                    hasMatches = true;

                    if (!quiet && !countOnly && !filesWithMatches && !filesWithoutMatch) {
                        StringBuilder output = new StringBuilder();
                        if (showFilename) {
                            output.append(filename).append(":");
                        }
                        if (showLineNumbers) {
                            output.append(lineNumber).append(":");
                        }
                        output.append(line);
                        context.out().println(output.toString());
                    }
                }
            }
        }

        if (countOnly && !quiet) {
            if (showFilename) {
                context.out().println(filename + ":" + matchCount);
            } else {
                context.out().println(matchCount);
            }
        } else if (filesWithMatches && hasMatches && !quiet) {
            context.out().println(filename);
        } else if (filesWithoutMatch && !hasMatches && !quiet) {
            context.out().println(filename);
        }
    }

    /**
     * Sort command - sort lines of text.
     */
    public static void sort(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "sort - sort lines of text",
            "Usage: sort [OPTIONS] [FILES]",
            "  -? --help                    Show help",
            "  -r --reverse                 Reverse the result of comparisons",
            "  -n --numeric-sort            Compare according to string numerical value",
            "  -f --ignore-case             Fold lower case to upper case characters",
            "  -u --unique                  Output only the first of an equal run",
            "  -b --ignore-leading-blanks   Ignore leading blanks",
            "  -k --key=KEY                 Fields to use for sorting separated by whitespaces"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        List<String> args = opt.args();
        List<String> lines = new ArrayList<>();

        if (!args.isEmpty()) {
            for (String arg : args) {
                InputStream is;
                if ("-".equals(arg)) {
                    is = context.in();
                } else {
                    is = context.currentDir().resolve(arg).toUri().toURL().openStream();
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
            }
        } else {
            // Read from stdin
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.in()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        }

        // Sort the lines
        Comparator<String> comparator = String::compareTo;

        if (opt.isSet("ignore-case")) {
            comparator = String.CASE_INSENSITIVE_ORDER;
        }
        if (opt.isSet("numeric-sort")) {
            comparator = (a, b) -> {
                try {
                    Double da = Double.parseDouble(a.trim());
                    Double db = Double.parseDouble(b.trim());
                    return da.compareTo(db);
                } catch (NumberFormatException e) {
                    return a.compareTo(b);
                }
            };
        }
        if (opt.isSet("reverse")) {
            comparator = comparator.reversed();
        }
        if (opt.isSet("ignore-leading-blanks")) {
            comparator = (a, b) -> comparator.compare(a.trim(), b.trim());
        }

        lines.sort(comparator);

        // Remove duplicates if unique option is set
        if (opt.isSet("unique")) {
            lines = lines.stream().distinct().collect(Collectors.toList());
        }

        // Output sorted lines
        for (String line : lines) {
            context.out().println(line);
        }
    }

    /**
     * List directory contents command.
     */
    public static void ls(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "ls - list directory contents",
            "Usage: ls [OPTIONS] [FILES]",
            "  -? --help                    Show help",
            "  -a --all                     Do not ignore entries starting with .",
            "  -l                           Use a long listing format",
            "  -1                           List one file per line",
            "  -C                           List entries by columns",
            "  -m                           Fill width with a comma separated list of entries",
            "  -r --reverse                 Reverse order while sorting",
            "  -t                           Sort by modification time",
            "  -S                           Sort by file size",
            "  -h --human-readable          Print sizes in human readable form",
            "  -d --directory               List directories themselves, not their contents",
            "     --color=WHEN              Colorize output (always, never, auto)"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        List<String> args = opt.args();
        if (args.isEmpty()) {
            args = Collections.singletonList(".");
        }

        boolean showAll = opt.isSet("all");
        boolean longFormat = opt.isSet("l");
        boolean onePerLine = opt.isSet("1");
        boolean columnFormat = opt.isSet("C");
        boolean commaFormat = opt.isSet("m");
        boolean reverse = opt.isSet("reverse");
        boolean sortByTime = opt.isSet("t");
        boolean sortBySize = opt.isSet("S");
        boolean humanReadable = opt.isSet("human-readable");
        boolean directoryOnly = opt.isSet("directory");

        String color = opt.isSet("color") ? opt.get("color") : "auto";
        boolean colored = "always".equals(color) || ("auto".equals(color) && context.isTty());

        for (String arg : args) {
            Path path = context.currentDir().resolve(arg);

            if (directoryOnly || !Files.isDirectory(path)) {
                // List the file/directory itself
                listPath(context, path, longFormat, humanReadable, colored);
            } else {
                // List directory contents
                if (args.size() > 1) {
                    context.out().println(arg + ":");
                }

                try (Stream<Path> stream = Files.list(path)) {
                    List<Path> entries = stream.collect(Collectors.toList());

                    // Filter hidden files
                    if (!showAll) {
                        entries = entries.stream()
                                .filter(p -> !p.getFileName().toString().startsWith("."))
                                .collect(Collectors.toList());
                    }

                    // Sort entries
                    Comparator<Path> comparator = Comparator.comparing(p -> p.getFileName().toString());
                    if (sortByTime) {
                        comparator = Comparator.comparing(p -> {
                            try {
                                return Files.getLastModifiedTime(p);
                            } catch (IOException e) {
                                return null;
                            }
                        });
                    } else if (sortBySize) {
                        comparator = Comparator.comparing(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0L;
                            }
                        });
                    }
                    if (reverse) {
                        comparator = comparator.reversed();
                    }

                    entries.sort(comparator);

                    // Display entries
                    if (longFormat) {
                        for (Path entry : entries) {
                            listPathLong(context, entry, humanReadable, colored);
                        }
                    } else if (onePerLine) {
                        for (Path entry : entries) {
                            context.out().println(formatFileName(entry, colored));
                        }
                    } else {
                        // Simple format - just names
                        for (Path entry : entries) {
                            context.out().print(formatFileName(entry, colored) + "  ");
                        }
                        context.out().println();
                    }
                }

                if (args.size() > 1) {
                    context.out().println();
                }
            }
        }
    }

    private static void listPath(Context context, Path path, boolean longFormat, boolean humanReadable, boolean colored) throws IOException {
        if (longFormat) {
            listPathLong(context, path, humanReadable, colored);
        } else {
            context.out().println(formatFileName(path, colored));
        }
    }

    private static void listPathLong(Context context, Path path, boolean humanReadable, boolean colored) throws IOException {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            // Permissions (simplified)
            String perms = Files.isDirectory(path) ? "d" : "-";
            perms += Files.isReadable(path) ? "r" : "-";
            perms += Files.isWritable(path) ? "w" : "-";
            perms += Files.isExecutable(path) ? "x" : "-";
            perms += "------"; // Simplified - not showing group/other permissions

            // Size
            String size;
            if (humanReadable) {
                size = formatHumanReadable(attrs.size());
            } else {
                size = String.valueOf(attrs.size());
            }

            // Date
            String date = DateTimeFormatter.ofPattern("MMM dd HH:mm")
                    .format(LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));

            context.out().printf("%s %8s %s %s%n",
                    perms, size, date, formatFileName(path, colored));

        } catch (IOException e) {
            context.out().println("? " + formatFileName(path, colored));
        }
    }

    private static String formatFileName(Path path, boolean colored) {
        String name = path.getFileName().toString();
        if (!colored) {
            return name;
        }

        // Simple coloring
        if (Files.isDirectory(path)) {
            return "\033[34m" + name + "\033[0m"; // Blue for directories
        } else if (Files.isExecutable(path)) {
            return "\033[32m" + name + "\033[0m"; // Green for executables
        } else {
            return name;
        }
    }

    private static String formatHumanReadable(long bytes) {
        if (bytes < 1024) return bytes + "B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f%s", bytes / Math.pow(1024, exp), pre);
    }
}
