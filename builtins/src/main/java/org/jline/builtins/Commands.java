/*
 * Copyright (c) 2002-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.builtins.Completers.CompletionData;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.Source.StdInSource;
import org.jline.builtins.Source.URLSource;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Highlighter;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.Macro;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

import static org.jline.builtins.SyntaxHighlighter.*;

public class Commands {

    public static void tmux(
            Terminal terminal,
            PrintStream out,
            PrintStream err,
            Supplier<Object> getter,
            Consumer<Object> setter,
            Consumer<Terminal> runner,
            String[] argv)
            throws Exception {
        final String[] usage = {
            "tmux -  terminal multiplexer", "Usage: tmux [command]", "  -? --help                    Show help",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        // Tmux with no args
        if (argv.length == 0) {
            Object instance = getter.get();
            if (instance != null) {
                err.println("tmux: can't run tmux inside itself");
            } else {
                Tmux tmux = new Tmux(terminal, err, runner);
                setter.accept(tmux);
                try {
                    tmux.run();
                } finally {
                    setter.accept(null);
                }
            }
        } else {
            Object instance = getter.get();
            if (instance != null) {
                ((Tmux) instance).execute(out, err, Arrays.asList(argv));
            } else {
                err.println("tmux: no instance running");
            }
        }
    }

    public static void nano(Terminal terminal, PrintStream out, PrintStream err, Path currentDir, String[] argv)
            throws Exception {
        nano(terminal, out, err, currentDir, argv, null);
    }

    public static void nano(
            Terminal terminal,
            PrintStream out,
            PrintStream err,
            Path currentDir,
            String[] argv,
            ConfigurationPath configPath)
            throws Exception {
        Options opt = Options.compile(Nano.usage()).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        Nano edit = new Nano(terminal, currentDir, opt, configPath);
        edit.open(opt.args());
        edit.run();
    }

    public static void less(
            Terminal terminal, InputStream in, PrintStream out, PrintStream err, Path currentDir, Object[] argv)
            throws Exception {
        less(terminal, in, out, err, currentDir, argv, null);
    }

    public static void less(
            Terminal terminal,
            InputStream in,
            PrintStream out,
            PrintStream err,
            Path currentDir,
            Object[] argv,
            ConfigurationPath configPath)
            throws Exception {
        Options opt = Options.compile(Less.usage()).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        Less less = new Less(terminal, currentDir, opt, configPath);
        List<Source> sources = new ArrayList<>();
        if (opt.argObjects().isEmpty()) {
            opt.argObjects().add("-");
        }

        for (Object o : opt.argObjects()) {
            if (o instanceof String) {
                String arg = (String) o;
                arg = arg.startsWith("~") ? arg.replace("~", System.getProperty("user.home")) : arg;
                if ("-".equals(arg)) {
                    sources.add(new StdInSource(in));
                } else if (arg.contains("*") || arg.contains("?")) {
                    for (Path p : findFiles(currentDir, arg)) {
                        sources.add(new URLSource(p.toUri().toURL(), p.toString()));
                    }
                } else {
                    sources.add(new URLSource(currentDir.resolve(arg).toUri().toURL(), arg));
                }
            } else if (o instanceof Source) {
                sources.add((Source) o);
            } else {
                ByteArrayInputStream bais = null;
                if (o instanceof String[]) {
                    bais = new ByteArrayInputStream(
                            String.join("\n", (String[]) o).getBytes());
                } else if (o instanceof ByteArrayInputStream) {
                    bais = (ByteArrayInputStream) o;
                } else if (o instanceof byte[]) {
                    bais = new ByteArrayInputStream((byte[]) o);
                }
                if (bais != null) {
                    sources.add(new Source.InputStreamSource(bais, true, "Less"));
                }
            }
        }
        less.run(sources);
    }

    protected static List<Path> findFiles(Path root, String files) throws IOException {
        files = files.startsWith("~") ? files.replace("~", System.getProperty("user.home")) : files;
        String regex = files;
        Path searchRoot = Paths.get("/");
        if (new File(files).isAbsolute()) {
            regex = regex.replaceAll("\\\\", "/").replaceAll("//", "/");
            if (regex.contains("/")) {
                String sr = regex.substring(0, regex.lastIndexOf("/") + 1);
                while (sr.contains("*") || sr.contains("?")) {
                    sr = sr.substring(0, sr.lastIndexOf("/"));
                }
                searchRoot = Paths.get(sr + "/");
            }
        } else {
            regex = (root.toString().length() == 0 ? "" : root + "/") + files;
            regex = regex.replaceAll("\\\\", "/").replaceAll("//", "/");
            searchRoot = root;
        }
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + regex);
        return Files.find(searchRoot, Integer.MAX_VALUE, (path, f) -> pathMatcher.matches(path))
                .collect(Collectors.toList());
    }

    public static void history(LineReader reader, PrintStream out, PrintStream err, Path currentDir, String[] argv)
            throws Exception {
        final String[] usage = {
            "history -  list history of commands",
            "Usage: history [-dnrfEie] [-m match] [first] [last]",
            "       history -ARWI [filename]",
            "       history -s [old=new] [command]",
            "       history --clear",
            "       history --save",
            "  -? --help                       Displays command help",
            "     --clear                      Clear history",
            "     --save                       Save history",
            "  -m match                        If option -m is present the first argument is taken as a pattern",
            "                                  and only the history events matching the pattern will be shown",
            "  -d                              Print timestamps for each event",
            "  -f                              Print full time date stamps in the US format",
            "  -E                              Print full time date stamps in the European format",
            "  -i                              Print full time date stamps in ISO8601 format",
            "  -n                              Suppresses command numbers",
            "  -r                              Reverses the order of the commands",
            "  -A                              Appends the history out to the given file",
            "  -R                              Reads the history from the given file",
            "  -W                              Writes the history out to the given file",
            "  -I                              If added to -R, only the events that are not contained within the internal list are added",
            "                                  If added to -W or -A, only the events that are new since the last incremental operation",
            "                                  to the file are added",
            "  [first] [last]                  These optional arguments may be specified as a number or as a string. A negative number",
            "                                  is used as an offset to the current history event number. A string specifies the most",
            "                                  recent event beginning with the given string.",
            "  -e                              Uses the nano editor to edit the commands before executing",
            "  -s                              Re-executes the command without invoking an editor"
        };
        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        History history = reader.getHistory();
        boolean done = true;
        boolean increment = opt.isSet("I");
        if (opt.isSet("clear")) {
            history.purge();
        } else if (opt.isSet("save")) {
            history.save();
        } else if (opt.isSet("A")) {
            Path file = opt.args().size() > 0 ? currentDir.resolve(opt.args().get(0)) : null;
            history.append(file, increment);
        } else if (opt.isSet("R")) {
            Path file = opt.args().size() > 0 ? currentDir.resolve(opt.args().get(0)) : null;
            history.read(file, increment);
        } else if (opt.isSet("W")) {
            Path file = opt.args().size() > 0 ? currentDir.resolve(opt.args().get(0)) : null;
            history.write(file, increment);
        } else {
            done = false;
        }
        if (done) {
            return;
        }
        ReExecute execute = new ReExecute(history, opt);
        int argId = execute.getArgId();

        Pattern pattern = null;
        if (opt.isSet("m") && opt.args().size() > argId) {
            StringBuilder sb = new StringBuilder();
            char prev = '0';
            for (char c : opt.args().get(argId++).toCharArray()) {
                if (c == '*' && prev != '\\' && prev != '.') {
                    sb.append('.');
                }
                sb.append(c);
                prev = c;
            }
            pattern = Pattern.compile(sb.toString(), Pattern.DOTALL);
        }
        boolean reverse = opt.isSet("r") || (opt.isSet("s") && opt.args().size() <= argId);
        int firstId = opt.args().size() > argId
                ? retrieveHistoryId(history, opt.args().get(argId++))
                : -17;
        int lastId = opt.args().size() > argId
                ? retrieveHistoryId(history, opt.args().get(argId++))
                : -1;
        firstId = historyId(firstId, history.first(), history.last());
        lastId = historyId(lastId, history.first(), history.last());
        if (firstId > lastId) {
            int tmpId = firstId;
            firstId = lastId;
            lastId = tmpId;
            reverse = !reverse;
        }
        int tot = lastId - firstId + 1;
        int listed = 0;
        final Highlighter highlighter = reader.getHighlighter();
        Iterator<History.Entry> iter = null;
        if (reverse) {
            iter = history.reverseIterator(lastId);
        } else {
            iter = history.iterator(firstId);
        }

        while (iter.hasNext() && listed < tot) {
            History.Entry entry = iter.next();
            listed++;
            if (pattern != null && !pattern.matcher(entry.line()).matches()) {
                continue;
            }
            if (execute.isExecute()) {
                if (execute.isEdit()) {
                    execute.addCommandInFile(entry.line());
                } else {
                    execute.addCommandInBuffer(reader, entry.line());
                    break;
                }
            } else {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                if (!opt.isSet("n")) {
                    sb.append("  ");
                    sb.styled(AttributedStyle::bold, String.format("%3d", entry.index()));
                }
                if (opt.isSet("d") || opt.isSet("f") || opt.isSet("E") || opt.isSet("i")) {
                    sb.append("  ");
                    if (opt.isSet("d")) {
                        LocalTime lt = LocalTime.from(entry.time().atZone(ZoneId.systemDefault()))
                                .truncatedTo(ChronoUnit.SECONDS);
                        DateTimeFormatter.ISO_LOCAL_TIME.formatTo(lt, sb);
                    } else {
                        LocalDateTime lt = LocalDateTime.from(
                                entry.time().atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.MINUTES));
                        String format = "yyyy-MM-dd hh:mm";
                        if (opt.isSet("f")) {
                            format = "MM/dd/yy hh:mm";
                        } else if (opt.isSet("E")) {
                            format = "dd.MM.yyyy hh:mm";
                        }
                        DateTimeFormatter.ofPattern(format).formatTo(lt, sb);
                    }
                }
                sb.append("  ");
                sb.append(highlighter.highlight(reader, entry.line()));
                out.println(sb.toAnsi(reader.getTerminal()));
            }
        }
        execute.editCommandsAndClose(reader);
    }

    private static class ReExecute {
        private final boolean execute;
        private final boolean edit;
        private String oldParam;
        private String newParam;
        private FileWriter cmdWriter;
        private File cmdFile;
        private int argId = 0;

        public ReExecute(History history, Options opt) throws IOException {
            execute = opt.isSet("e") || opt.isSet("s");
            edit = opt.isSet("e");
            if (execute) {
                Iterator<History.Entry> iter = history.reverseIterator(history.last());
                if (iter.hasNext()) {
                    iter.next();
                    iter.remove();
                }
                if (edit) {
                    cmdFile = File.createTempFile("jline-history-", null);
                    cmdWriter = new FileWriter(cmdFile);
                } else if (opt.args().size() > 0) {
                    String[] s = opt.args().get(argId).split("=");
                    if (s.length == 2) {
                        argId = argId + 1;
                        oldParam = s[0];
                        newParam = s[1];
                    }
                }
            }
        }

        public int getArgId() {
            return argId;
        }

        public boolean isEdit() {
            return edit;
        }

        public boolean isExecute() {
            return execute;
        }

        public void addCommandInFile(String command) throws IOException {
            cmdWriter.write(command + "\n");
        }

        public void addCommandInBuffer(LineReader reader, String command) {
            reader.addCommandsInBuffer(Arrays.asList(replaceParam(command)));
        }

        private String replaceParam(String command) {
            String out = command;
            if (oldParam != null && newParam != null) {
                out = command.replaceAll(oldParam, newParam);
            }
            return out;
        }

        public void editCommandsAndClose(LineReader reader) throws Exception {
            if (edit) {
                cmdWriter.close();
                try {
                    reader.editAndAddInBuffer(cmdFile);
                } finally {
                    cmdFile.delete();
                }
            }
        }
    }

    private static int historyId(int id, int minId, int maxId) {
        int out = id;
        if (id < 0) {
            out = maxId + id + 1;
        }
        if (out < minId) {
            out = minId;
        } else if (out > maxId) {
            out = maxId;
        }
        return out;
    }

    private static int retrieveHistoryId(History history, String s) throws IllegalArgumentException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            Iterator<History.Entry> iter = history.iterator();
            while (iter.hasNext()) {
                History.Entry entry = iter.next();
                if (entry.line().startsWith(s)) {
                    return entry.index();
                }
            }
            throw new IllegalArgumentException("history: event not found: " + s);
        }
    }

    public static void complete(
            LineReader reader,
            PrintStream out,
            PrintStream err,
            Map<String, List<CompletionData>> completions,
            String[] argv)
            throws HelpException {
        final String[] usage = {
            "complete -  edit command specific tab-completions",
            "Usage: complete",
            "  -? --help                       Displays command help",
            "  -c --command=COMMAND            Command to add completion to",
            "  -d --description=DESCRIPTION    Description of this completions",
            "  -e --erase                      Erase the completions",
            "  -s --short-option=SHORT_OPTION  Posix-style option to complete",
            "  -l --long-option=LONG_OPTION    GNU-style option to complete",
            "  -a --argument=ARGUMENTS         A list of possible arguments",
            "  -n --condition=CONDITION        The completion should only be used if the",
            "                                  specified command has a zero exit status"
        };

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        String command = opt.get("command");

        if (opt.isSet("erase")) {
            completions.remove(command);
            return;
        }

        List<CompletionData> cmdCompletions = completions.computeIfAbsent(command, s -> new ArrayList<>());
        List<String> options = null;
        if (opt.isSet("short-option")) {
            for (String op : opt.getList("short-option")) {
                if (options == null) {
                    options = new ArrayList<>();
                }
                options.add("-" + op);
            }
        }
        if (opt.isSet("long-option")) {
            for (String op : opt.getList("long-option")) {
                if (options == null) {
                    options = new ArrayList<>();
                }
                options.add("--" + op);
            }
        }
        String description = opt.isSet("description") ? opt.get("description") : null;
        String argument = opt.isSet("argument") ? opt.get("argument") : null;
        String condition = opt.isSet("condition") ? opt.get("condition") : null;
        cmdCompletions.add(new CompletionData(options, description, argument, condition));
    }

    public static void widget(
            LineReader reader, PrintStream out, PrintStream err, Function<String, Widget> widgetCreator, String[] argv)
            throws Exception {
        final String[] usage = {
            "widget -  manipulate widgets",
            "Usage: widget -N new-widget [function-name]",
            "       widget -D widget ...",
            "       widget -A old-widget new-widget",
            "       widget -U string ...",
            "       widget -l [options]",
            "  -? --help                       Displays command help",
            "  -A                              Create alias to widget",
            "  -N                              Create new widget",
            "  -D                              Delete widgets",
            "  -U                              Push characters to the stack",
            "  -l                              List user-defined widgets",
            "  -a                              With -l, list all widgets"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        int actions = (opt.isSet("N") ? 1 : 0)
                + (opt.isSet("D") ? 1 : 0)
                + (opt.isSet("U") ? 1 : 0)
                + (opt.isSet("l") ? 1 : 0)
                + (opt.isSet("A") ? 1 : 0);
        if (actions > 1) {
            err.println("widget: incompatible operation selection options");
            return;
        }
        if (opt.isSet("l")) {
            TreeSet<String> ws = new TreeSet<>(reader.getWidgets().keySet());
            if (opt.isSet("a")) {
                Set<String> temp = new HashSet<>(ws);
                for (String s : temp) {
                    ws.add(reader.getWidgets().get(s).toString());
                }
            }
            for (String s : ws) {
                if (opt.isSet("a")) {
                    out.println(s);
                } else if (!reader.getWidgets().get(s).toString().startsWith(".")) {
                    out.println(s + " (" + reader.getWidgets().get(s) + ")");
                }
            }
        } else if (opt.isSet("N")) {
            if (opt.args().size() < 1) {
                err.println("widget: not enough arguments for -N");
                return;
            }
            if (opt.args().size() > 2) {
                err.println("widget: too many arguments for -N");
                return;
            }
            final String name = opt.args().get(0);
            final String func = opt.args().size() == 2 ? opt.args().get(1) : name;
            reader.getWidgets().put(name, widgetCreator.apply(func));
        } else if (opt.isSet("D")) {
            for (String name : opt.args()) {
                reader.getWidgets().remove(name);
            }
        } else if (opt.isSet("A")) {
            if (opt.args().size() < 2) {
                err.println("widget: not enough arguments for -A");
                return;
            }
            if (opt.args().size() > 2) {
                err.println("widget: too many arguments for -A");
                return;
            }
            Widget org = null;
            if (opt.args().get(0).startsWith(".")) {
                org = reader.getBuiltinWidgets().get(opt.args().get(0).substring(1));
            } else {
                org = reader.getWidgets().get(opt.args().get(0));
            }
            if (org == null) {
                err.println("widget: no such widget `" + opt.args().get(0) + "'");
                return;
            }
            reader.getWidgets().put(opt.args().get(1), org);
        } else if (opt.isSet("U")) {
            for (String arg : opt.args()) {
                reader.runMacro(KeyMap.translate(arg));
            }
        } else if (opt.args().size() == 1) {
            reader.callWidget(opt.args().get(0));
        }
    }

    public static void keymap(LineReader reader, PrintStream out, PrintStream err, String[] argv) throws HelpException {
        final String[] usage = {
            "keymap -  manipulate keymaps",
            "Usage: keymap [options] -l [-L] [keymap ...]",
            "       keymap [options] -d",
            "       keymap [options] -D keymap ...",
            "       keymap [options] -A old-keymap new-keymap",
            "       keymap [options] -N new-keymap [old-keymap]",
            "       keymap [options] -M",
            "       keymap [options] -r in-string ...",
            "       keymap [options] -s in-string out-string ...",
            "       keymap [options] in-string command ...",
            "       keymap [options] [in-string]",
            "  -? --help                       Displays command help",
            "  -A                              Create alias to keymap",
            "  -D                              Delete named keymaps",
            "  -L                              Output in form of keymap commands",
            "  -M (default=main)               Specify keymap to select",
            "  -N                              Create new keymap",
            "  -R                              Interpret in-strings as ranges",
            "  -a                              Select vicmd keymap",
            "  -d                              Delete existing keymaps and reset to default state",
            "  -e                              Select emacs keymap and bind it to main",
            "  -l                              List existing keymap names",
            "  -p                              List bindings which have given key sequence as a a prefix",
            "  -r                              Unbind specified in-strings ",
            "  -s                              Bind each in-string to each out-string ",
            "  -v                              Select viins keymap and bind it to main",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }

        Map<String, KeyMap<Binding>> keyMaps = reader.getKeyMaps();

        int actions = (opt.isSet("N") ? 1 : 0)
                + (opt.isSet("d") ? 1 : 0)
                + (opt.isSet("D") ? 1 : 0)
                + (opt.isSet("l") ? 1 : 0)
                + (opt.isSet("r") ? 1 : 0)
                + (opt.isSet("s") ? 1 : 0)
                + (opt.isSet("A") ? 1 : 0);
        if (actions > 1) {
            err.println("keymap: incompatible operation selection options");
            return;
        }
        if (opt.isSet("l")) {
            boolean commands = opt.isSet("L");
            // TODO: handle commands
            if (opt.args().size() > 0) {
                for (String arg : opt.args()) {
                    KeyMap<Binding> map = keyMaps.get(arg);
                    if (map == null) {
                        err.println("keymap: no such keymap: `" + arg + "'");
                    } else {
                        out.println(arg);
                    }
                }
            } else {
                keyMaps.keySet().forEach(out::println);
            }
        } else if (opt.isSet("N")) {
            if (opt.isSet("e") || opt.isSet("v") || opt.isSet("a") || opt.isSet("M")) {
                err.println("keymap: keymap can not be selected with -N");
                return;
            }
            if (opt.args().size() < 1) {
                err.println("keymap: not enough arguments for -N");
                return;
            }
            if (opt.args().size() > 2) {
                err.println("keymap: too many arguments for -N");
                return;
            }
            KeyMap<Binding> org = null;
            if (opt.args().size() == 2) {
                org = keyMaps.get(opt.args().get(1));
                if (org == null) {
                    err.println("keymap: no such keymap `" + opt.args().get(1) + "'");
                    return;
                }
            }
            KeyMap<Binding> map = new KeyMap<>();
            if (org != null) {
                for (Map.Entry<String, Binding> bound : org.getBoundKeys().entrySet()) {
                    map.bind(bound.getValue(), bound.getKey());
                }
            }
            keyMaps.put(opt.args().get(0), map);
        } else if (opt.isSet("A")) {
            if (opt.isSet("e") || opt.isSet("v") || opt.isSet("a") || opt.isSet("M")) {
                err.println("keymap: keymap can not be selected with -N");
                return;
            }
            if (opt.args().size() < 2) {
                err.println("keymap: not enough arguments for -A");
                return;
            }
            if (opt.args().size() > 2) {
                err.println("keymap: too many arguments for -A");
                return;
            }
            KeyMap<Binding> org = keyMaps.get(opt.args().get(0));
            if (org == null) {
                err.println("keymap: no such keymap `" + opt.args().get(0) + "'");
                return;
            }
            keyMaps.put(opt.args().get(1), org);
        } else if (opt.isSet("d")) {
            if (opt.isSet("e") || opt.isSet("v") || opt.isSet("a") || opt.isSet("M")) {
                err.println("keymap: keymap can not be selected with -N");
                return;
            }
            if (opt.args().size() > 0) {
                err.println("keymap: too many arguments for -d");
                return;
            }
            keyMaps.clear();
            keyMaps.putAll(reader.defaultKeyMaps());
        } else if (opt.isSet("D")) {
            if (opt.isSet("e") || opt.isSet("v") || opt.isSet("a") || opt.isSet("M")) {
                err.println("keymap: keymap can not be selected with -N");
                return;
            }
            if (opt.args().size() < 1) {
                err.println("keymap: not enough arguments for -A");
                return;
            }
            for (String name : opt.args()) {
                if (keyMaps.remove(name) == null) {
                    err.println("keymap: no such keymap `" + name + "'");
                    return;
                }
            }
        } else if (opt.isSet("r")) {
            // Select keymap
            String keyMapName = LineReader.MAIN;
            int sel = (opt.isSet("a") ? 1 : 0)
                    + (opt.isSet("e") ? 1 : 0)
                    + (opt.isSet("v") ? 1 : 0)
                    + (opt.isSet("M") ? 1 : 0);
            if (sel > 1) {
                err.println("keymap: incompatible keymap selection options");
                return;
            } else if (opt.isSet("a")) {
                keyMapName = LineReader.VICMD;
            } else if (opt.isSet("e")) {
                keyMapName = LineReader.EMACS;
            } else if (opt.isSet("v")) {
                keyMapName = LineReader.VIINS;
            } else if (opt.isSet("M")) {
                if (opt.args().isEmpty()) {
                    err.println("keymap: argument expected: -M");
                    return;
                }
                keyMapName = opt.args().remove(0);
            }
            KeyMap<Binding> map = keyMaps.get(keyMapName);
            if (map == null) {
                err.println("keymap: no such keymap `" + keyMapName + "'");
                return;
            }
            // Unbind
            boolean range = opt.isSet("R");
            boolean prefix = opt.isSet("p");
            Set<String> toRemove = new HashSet<>();
            Map<String, Binding> bound = map.getBoundKeys();
            for (String arg : opt.args()) {
                if (range) {
                    Collection<String> r = KeyMap.range(opt.args().get(0));
                    if (r == null) {
                        err.println("keymap: malformed key range `" + opt.args().get(0) + "'");
                        return;
                    }
                    toRemove.addAll(r);
                } else {
                    String seq = KeyMap.translate(arg);
                    for (String k : bound.keySet()) {
                        if (prefix && k.startsWith(seq) && k.length() > seq.length() || !prefix && k.equals(seq)) {
                            toRemove.add(k);
                        }
                    }
                }
            }
            for (String seq : toRemove) {
                map.unbind(seq);
            }
            if (opt.isSet("e") || opt.isSet("v")) {
                keyMaps.put(LineReader.MAIN, map);
            }
        } else if (opt.isSet("s") || opt.args().size() > 1) {
            // Select keymap
            String keyMapName = LineReader.MAIN;
            int sel = (opt.isSet("a") ? 1 : 0)
                    + (opt.isSet("e") ? 1 : 0)
                    + (opt.isSet("v") ? 1 : 0)
                    + (opt.isSet("M") ? 1 : 0);
            if (sel > 1) {
                err.println("keymap: incompatible keymap selection options");
                return;
            } else if (opt.isSet("a")) {
                keyMapName = LineReader.VICMD;
            } else if (opt.isSet("e")) {
                keyMapName = LineReader.EMACS;
            } else if (opt.isSet("v")) {
                keyMapName = LineReader.VIINS;
            } else if (opt.isSet("M")) {
                if (opt.args().isEmpty()) {
                    err.println("keymap: argument expected: -M");
                    return;
                }
                keyMapName = opt.args().remove(0);
            }
            KeyMap<Binding> map = keyMaps.get(keyMapName);
            if (map == null) {
                err.println("keymap: no such keymap `" + keyMapName + "'");
                return;
            }
            // Bind
            boolean range = opt.isSet("R");
            if (opt.args().size() % 2 == 1) {
                err.println("keymap: even number of arguments required");
                return;
            }
            for (int i = 0; i < opt.args().size(); i += 2) {
                Binding bout = opt.isSet("s")
                        ? new Macro(KeyMap.translate(opt.args().get(i + 1)))
                        : new Reference(opt.args().get(i + 1));
                if (range) {
                    Collection<String> r = KeyMap.range(opt.args().get(i));
                    if (r == null) {
                        err.println("keymap: malformed key range `" + opt.args().get(i) + "'");
                        return;
                    }
                    map.bind(bout, r);
                } else {
                    String in = KeyMap.translate(opt.args().get(i));
                    map.bind(bout, in);
                }
            }
            if (opt.isSet("e") || opt.isSet("v")) {
                keyMaps.put(LineReader.MAIN, map);
            }
        } else {
            // Select keymap
            String keyMapName = LineReader.MAIN;
            int sel = (opt.isSet("a") ? 1 : 0)
                    + (opt.isSet("e") ? 1 : 0)
                    + (opt.isSet("v") ? 1 : 0)
                    + (opt.isSet("M") ? 1 : 0);
            if (sel > 1) {
                err.println("keymap: incompatible keymap selection options");
                return;
            } else if (opt.isSet("a")) {
                keyMapName = LineReader.VICMD;
            } else if (opt.isSet("e")) {
                keyMapName = LineReader.EMACS;
            } else if (opt.isSet("v")) {
                keyMapName = LineReader.VIINS;
            } else if (opt.isSet("M")) {
                if (opt.args().isEmpty()) {
                    err.println("keymap: argument expected: -M");
                    return;
                }
                keyMapName = opt.args().remove(0);
            }
            KeyMap<Binding> map = keyMaps.get(keyMapName);
            if (map == null) {
                err.println("keymap: no such keymap `" + keyMapName + "'");
                return;
            }
            // Display
            boolean prefix = opt.isSet("p");
            boolean commands = opt.isSet("L");
            if (prefix && opt.args().isEmpty()) {
                err.println("keymap: option -p requires a prefix string");
                return;
            }
            if (opt.args().size() > 0 || !opt.isSet("e") && !opt.isSet("v")) {
                Map<String, Binding> bound = map.getBoundKeys();
                String seq = opt.args().size() > 0 ? KeyMap.translate(opt.args().get(0)) : null;
                Map.Entry<String, Binding> begin = null;
                String last = null;
                Iterator<Entry<String, Binding>> iterator = bound.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Binding> entry = iterator.next();
                    String key = entry.getKey();
                    if (seq == null
                            || prefix && key.startsWith(seq) && !key.equals(seq)
                            || !prefix && key.equals(seq)) {
                        if (begin != null || !iterator.hasNext()) {
                            String n = (last.length() > 1 ? last.substring(0, last.length() - 1) : "")
                                    + (char) (last.charAt(last.length() - 1) + 1);
                            if (key.equals(n) && entry.getValue().equals(begin.getValue())) {
                                last = key;
                            } else {
                                // We're not in a range, so we need to close the previous range
                                StringBuilder sb = new StringBuilder();
                                if (commands) {
                                    sb.append("keymap -M ");
                                    sb.append(keyMapName);
                                    sb.append(" ");
                                }
                                if (begin.getKey().equals(last)) {
                                    sb.append(KeyMap.display(last));
                                    sb.append(" ");
                                    displayValue(sb, begin.getValue());
                                    out.println(sb);
                                } else {
                                    if (commands) {
                                        sb.append("-R ");
                                    }
                                    sb.append(KeyMap.display(begin.getKey()));
                                    sb.append("-");
                                    sb.append(KeyMap.display(last));
                                    sb.append(" ");
                                    displayValue(sb, begin.getValue());
                                    out.println(sb);
                                }
                                begin = entry;
                                last = key;
                            }
                        } else {
                            begin = entry;
                            last = key;
                        }
                    }
                }
            }
            if (opt.isSet("e") || opt.isSet("v")) {
                keyMaps.put(LineReader.MAIN, map);
            }
        }
    }

    public static void setopt(LineReader reader, PrintStream out, PrintStream err, String[] argv) throws HelpException {
        final String[] usage = {
            "setopt -  set options",
            "Usage: setopt [-m] option ...",
            "       setopt",
            "  -? --help                       Displays command help",
            "  -m                              Use pattern matching"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        if (opt.args().isEmpty()) {
            for (Option option : Option.values()) {
                if (reader.isSet(option) != option.isDef()) {
                    out.println((option.isDef() ? "no-" : "")
                            + option.toString().toLowerCase().replace('_', '-'));
                }
            }
        } else {
            boolean match = opt.isSet("m");
            doSetOpts(reader, out, err, opt.args(), match, true);
        }
    }

    public static void unsetopt(LineReader reader, PrintStream out, PrintStream err, String[] argv)
            throws HelpException {
        final String[] usage = {
            "unsetopt -  unset options",
            "Usage: unsetopt [-m] option ...",
            "       unsetopt",
            "  -? --help                       Displays command help",
            "  -m                              Use pattern matching"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        if (opt.args().isEmpty()) {
            for (Option option : Option.values()) {
                if (reader.isSet(option) == option.isDef()) {
                    out.println((option.isDef() ? "no-" : "")
                            + option.toString().toLowerCase().replace('_', '-'));
                }
            }
        } else {
            boolean match = opt.isSet("m");
            doSetOpts(reader, out, err, opt.args(), match, false);
        }
    }

    private static void doSetOpts(
            LineReader reader, PrintStream out, PrintStream err, List<String> options, boolean match, boolean set) {
        for (String name : options) {
            String tname = name.toLowerCase().replaceAll("[-_]", "");
            if (match) {
                tname = tname.replaceAll("\\*", "[a-z]*");
                tname = tname.replaceAll("\\?", "[a-z]");
            }
            boolean found = false;
            for (LineReader.Option option : LineReader.Option.values()) {
                String optName = option.name().toLowerCase().replaceAll("[-_]", "");
                if (match ? optName.matches(tname) : optName.equals(tname)) {
                    if (set) {
                        reader.setOpt(option);
                    } else {
                        reader.unsetOpt(option);
                    }
                    found = true;
                    if (!match) {
                        break;
                    }
                } else if (match ? ("no" + optName).matches(tname) : ("no" + optName).equals(tname)) {
                    if (set) {
                        reader.unsetOpt(option);
                    } else {
                        reader.setOpt(option);
                    }
                    if (!match) {
                        found = true;
                    }
                    break;
                }
            }
            if (!found) {
                err.println("No matching option: " + name);
            }
        }
    }

    private static void displayValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("undefined-key");
        } else if (value instanceof Macro) {
            sb.append(KeyMap.display(((Macro) value).getSequence()));
        } else if (value instanceof Reference) {
            sb.append(((Reference) value).name());
        } else {
            sb.append(value);
        }
    }

    public static void setvar(LineReader lineReader, PrintStream out, PrintStream err, String[] argv)
            throws HelpException {
        final String[] usage = {
            "setvar -  set lineReader variable value",
            "Usage: setvar [variable] [value]",
            "  -? --help                    Show help",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        if (opt.args().isEmpty()) {
            for (Map.Entry<String, Object> entry : lineReader.getVariables().entrySet()) {
                out.println(entry.getKey() + ": " + entry.getValue());
            }
        } else if (opt.args().size() == 1) {
            out.println(lineReader.getVariable(opt.args().get(0)));
        } else {
            lineReader.setVariable(opt.args().get(0), opt.args().get(1));
        }
    }

    public static void colors(Terminal terminal, PrintStream out, String[] argv) throws HelpException, IOException {
        String[] usage = {
            "colors -  view 256-color table and ANSI-styles",
            "Usage: colors [OPTIONS]",
            "  -? --help                     Displays command help",
            "  -a --ansistyles               List ANSI-styles",
            "  -c --columns=COLUMNS          Number of columns in name/rgb table",
            "                                COLUMNS = 1, display columns: color, style, ansi and HSL",
            "  -f --find=NAME                Find color names which contains NAME ",
            "  -l --lock=STYLE               Lock fore- or background color",
            "  -n --name                     Color name table (default number table)",
            "  -r --rgb                      Use and display rgb value",
            "  -s --small                    View 16-color table (default 256-color)",
            "  -v --view=COLOR               View 24bit color table of COLOR ",
            "                                COLOR = <colorName>, <color24bit> or hue<angle>"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new Options.HelpException(opt.usage());
        }
        Colors colors = new Colors(terminal, out);
        if (opt.isSet("ansistyles")) {
            colors.printStyles();
        } else {
            String style = null;
            if (opt.isSet("lock")) {
                style = opt.get("lock");
                if (style.length() - style.replace(":", "").length() > 1) {
                    style = null;
                }
            }
            if (!opt.isSet("view")) {
                boolean rgb = opt.isSet("rgb");
                int columns = terminal.getWidth() > (rgb ? 71 : 122) ? 6 : 5;
                String findName = null;
                boolean nameTable = opt.isSet("name");
                boolean table16 = opt.isSet("small");
                if (opt.isSet("find")) {
                    findName = opt.get("find").toLowerCase();
                    nameTable = true;
                    table16 = false;
                    columns = 4;
                }
                if (table16) {
                    columns = columns + 2;
                }
                if (opt.isSet("columns")) {
                    columns = opt.getNumber("columns");
                }
                colors.printColors(nameTable, rgb, table16, columns, findName, style);
            } else {
                colors.printColor(opt.get("view").toLowerCase(), style);
            }
        }
    }

    private static class Colors {
        private static final String COLORS_24BIT = "[0-9a-fA-F]{6}";
        private static final List<String> COLORS_16 = Arrays.asList(
                "black",
                "red",
                "green",
                "yellow",
                "blue",
                "magenta",
                "cyan",
                "white",
                "!black",
                "!red",
                "!green",
                "!yellow",
                "!blue",
                "!magenta",
                "!cyan",
                "!white");
        boolean name;
        boolean rgb;
        private final Terminal terminal;
        private final PrintStream out;
        private boolean fixedBg;
        private String fixedStyle;
        int r, g, b;

        public Colors(Terminal terminal, PrintStream out) {
            this.terminal = terminal;
            this.out = out;
        }

        private String getAnsiStyle(String style) {
            return style;
        }

        public void printStyles() {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.tabs(13);
            for (String s : Styles.ANSI_STYLES) {
                AttributedStyle as = new StyleResolver(this::getAnsiStyle).resolve("." + s);
                asb.style(as);
                asb.append(s);
                asb.style(AttributedStyle.DEFAULT);
                asb.append("\t");
                asb.append(getAnsiStyle(s));
                asb.append("\t");
                asb.append(as.toAnsi());
                asb.append("\n");
            }
            asb.toAttributedString().println(terminal);
        }

        private String getStyle(String color) {
            String out;
            char fg = ' ';
            if (name) {
                out = (fixedBg ? "fg:" : "bg:") + "~" + color.substring(1);
                fg = color.charAt(0);
            } else if (rgb) {
                out = (fixedBg ? "fg-rgb:" : "bg-rgb:") + "#" + color.substring(1);
                fg = color.charAt(0);
            } else if (color.substring(1).matches("\\d+")) {
                out = (fixedBg ? "38;5;" : "48;5;") + color.substring(1);
                fg = color.charAt(0);
            } else {
                out = (fixedBg ? "fg:" : "bg:") + color;
            }
            if (fixedStyle == null) {
                if (color.startsWith("!") || color.equals("white") || fg == 'b') {
                    out += ",fg:black";
                } else {
                    out += ",fg:!white";
                }
            } else {
                out += "," + fixedStyle;
            }
            return out;
        }

        private String foreground(int idx) {
            String fg = "w";
            if ((idx > 6 && idx < 16)
                    || (idx > 33 && idx < 52)
                    || (idx > 69 && idx < 88)
                    || (idx > 105 && idx < 124)
                    || (idx > 141 && idx < 160)
                    || (idx > 177 && idx < 196)
                    || (idx > 213 && idx < 232)
                    || idx > 243) {
                fg = "b";
            }
            return fg;
        }

        private String addPadding(int width, String field) {
            int s = width - field.length();
            int left = s / 2;
            StringBuilder lp = new StringBuilder();
            StringBuilder rp = new StringBuilder();
            for (int i = 0; i < left; i++) {
                lp.append(" ");
            }
            for (int i = 0; i < s - left; i++) {
                rp.append(" ");
            }
            return lp + field + rp;
        }

        private String addLeftPadding(int width, String field) {
            int s = width - field.length();
            StringBuilder lp = new StringBuilder();
            for (int i = 0; i < s; i++) {
                lp.append(" ");
            }
            return lp + field;
        }

        private void setFixedStyle(String style) {
            this.fixedStyle = style;
            if (style != null
                    && (style.contains("b:")
                            || style.contains("b-")
                            || style.contains("bg:")
                            || style.contains("bg-")
                            || style.contains("background"))) {
                fixedBg = true;
            }
        }

        private List<String> retrieveColorNames() throws IOException {
            List<String> out;
            try (InputStream is = new Source.ResourceSource("/org/jline/utils/colors.txt", null).read();
                    BufferedReader br = new BufferedReader(new java.io.InputStreamReader(is))) {
                out = br.lines()
                        .map(String::trim)
                        .filter(s -> !s.startsWith("#"))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
            return out;
        }

        public void printColors(boolean name, boolean rgb, boolean small, int columns, String findName, String style)
                throws IOException {
            this.name = !rgb && name;
            this.rgb = rgb;
            setFixedStyle(style);
            AttributedStringBuilder asb = new AttributedStringBuilder();
            int width = terminal.getWidth();
            String tableName = small ? " 16-color " : "256-color ";
            if (!name && !rgb) {
                out.print(tableName);
                out.print("table, fg:<name> ");
                if (!small) {
                    out.print("/ 38;5;<n>");
                }
                out.println();
                out.print("                 bg:<name> ");
                if (!small) {
                    out.print("/ 48;5;<n>");
                }
                out.println("\n");
                boolean narrow = width < 180;
                for (String c : COLORS_16) {
                    AttributedStyle ss = new StyleResolver(this::getStyle).resolve('.' + c, null);
                    asb.style(ss);
                    asb.append(addPadding(11, c));
                    asb.style(AttributedStyle.DEFAULT);
                    if (c.equals("white")) {
                        if (narrow || small) {
                            asb.append('\n');
                        } else {
                            asb.append("    ");
                        }
                    } else if (c.equals("!white")) {
                        asb.append('\n');
                    }
                }
                asb.append('\n');
                if (!small) {
                    for (int i = 16; i < 256; i++) {
                        String fg = foreground(i);
                        String code = Integer.toString(i);
                        AttributedStyle ss = new StyleResolver(this::getStyle).resolve("." + fg + code, null);
                        asb.style(ss);
                        String str = " ";
                        if (i < 100) {
                            str = "  ";
                        } else if (i > 231) {
                            str = i % 2 == 0 ? "    " : "   ";
                        }
                        asb.append(str).append(code).append(' ');
                        if (i == 51
                                || i == 87
                                || i == 123
                                || i == 159
                                || i == 195
                                || i == 231
                                || narrow
                                        && (i == 33 || i == 69 || i == 105 || i == 141 || i == 177 || i == 213
                                                || i == 243)) {
                            asb.style(AttributedStyle.DEFAULT);
                            asb.append('\n');
                            if (i == 231) {
                                asb.append('\n');
                            }
                        }
                    }
                }
            } else {
                out.print(tableName);
                if (name) {
                    asb.tabs(Arrays.asList(25, 60, 75));
                    out.println("table, fg:~<name> OR 38;5;<n>");
                    out.println("                 bg:~<name> OR 48;5;<n>");
                } else {
                    asb.tabs(Arrays.asList(15, 45, 70));
                    out.println("table, fg-rgb:<color24bit> OR 38;5;<n>");
                    out.println("                 bg-rgb:<color24bit> OR 48;5;<n>");
                }
                out.println();
                int col = 0;
                int idx = 0;
                int colWidth = rgb ? 12 : 21;
                int lb = 1;
                if (findName != null && (findName.startsWith("#") || findName.startsWith("x"))) {
                    findName = findName.substring(1);
                }
                for (String line : retrieveColorNames()) {
                    if (rgb) {
                        // do nothing
                    } else if (findName != null) {
                        if (!line.toLowerCase().contains(findName)) {
                            idx++;
                            continue;
                        }
                    } else if (small) {
                        colWidth = 15;
                        lb = 1;
                    } else if (columns > 4) {
                        if (idx > 15 && idx < 232) {
                            colWidth = columns != 6 || col == 1 || col == 2 || col == 3 ? 21 : 20;
                            lb = 1;
                        } else {
                            colWidth = columns != 6 || idx % 2 == 0 || col == 7 ? 15 : 16;
                            lb = -1;
                        }
                    }
                    String fg = foreground(idx);
                    if (rgb) {
                        line = Integer.toHexString(org.jline.utils.Colors.DEFAULT_COLORS_256[idx]);
                        for (int p = line.length(); p < 6; p++) {
                            line = "0" + line;
                        }
                        if (findName != null) {
                            if (!line.toLowerCase().matches(findName)) {
                                idx++;
                                continue;
                            }
                        }
                    }
                    AttributedStyle ss = new StyleResolver(this::getStyle).resolve("." + fg + line, null);
                    if (rgb) {
                        line = "#" + line;
                    }
                    asb.style(ss);
                    String idxstr = Integer.toString(idx);
                    if (rgb) {
                        if (idx < 10) {
                            idxstr = "  " + idxstr;
                        } else if (idx < 100) {
                            idxstr = " " + idxstr;
                        }
                    }
                    asb.append(idxstr).append(addPadding(colWidth - idxstr.length(), line));
                    if (columns == 1) {
                        asb.style(AttributedStyle.DEFAULT);
                        asb.append("\t").append(getStyle(fg + line.substring(rgb ? 1 : 0)));
                        asb.append("\t").append(ss.toAnsi());
                        int[] rgb1 = rgb(org.jline.utils.Colors.DEFAULT_COLORS_256[idx]);
                        int[] hsl = rgb2hsl(rgb1[0], rgb1[1], rgb1[2]);
                        asb.append("\t")
                                .append(addLeftPadding(6, hsl[0] + ", "))
                                .append(addLeftPadding(4, hsl[1] + "%"))
                                .append(", ")
                                .append(addLeftPadding(4, hsl[2] + "%"));
                    }
                    col++;
                    idx++;
                    if ((col + 1) * colWidth > width || col + lb > columns) {
                        col = 0;
                        asb.style(AttributedStyle.DEFAULT);
                        asb.append('\n');
                    }
                    if (findName == null) {
                        if (idx == 16) {
                            if (small) {
                                break;
                            } else if (col != 0) {
                                col = 0;
                                asb.style(AttributedStyle.DEFAULT);
                                asb.append('\n');
                            }
                        } else if (idx == 232 && col != 0) {
                            col = 0;
                            asb.style(AttributedStyle.DEFAULT);
                            asb.append('\n');
                        }
                    }
                }
            }
            asb.toAttributedString().println(terminal);
        }

        private int[] rgb(long color) {
            int[] rgb = {0, 0, 0};
            rgb[0] = (int) ((color >> 16) & 0xFF);
            rgb[1] = (int) ((color >> 8) & 0xFF);
            rgb[2] = (int) (color & 0xFF);
            return rgb;
        }

        private int[] hue2rgb(int degree) {
            int[] rgb = {0, 0, 0};
            double hue = degree / 60.0;
            double a = Math.tan((degree / 360.0) * 2 * Math.PI) / Math.sqrt(3);
            if (hue >= 0 && hue < 1) {
                rgb[0] = 0xff;
                rgb[1] = (int) (2 * a * 0xff / (1 + a));
            } else if (hue >= 1 && hue < 2) {
                rgb[0] = (int) (0xff * (1 + a) / (2 * a));
                rgb[1] = 0xff;
            } else if (hue >= 2 && hue < 3) {
                rgb[1] = 0xff;
                rgb[2] = (int) (0xff * (1 + a) / (1 - a));
            } else if (hue >= 3 && hue < 4) {
                rgb[1] = (int) (0xff * (1 - a) / (1 + a));
                rgb[2] = 0xff;
            } else if (hue >= 4 && hue <= 5) {
                rgb[0] = (int) (0xff * (a - 1) / (2 * a));
                rgb[2] = 0xff;
            } else if (hue > 5 && hue <= 6) {
                rgb[0] = 0xff;
                rgb[2] = (int) (0xff * 2 * a / (a - 1));
            }
            return rgb;
        }

        private int[] rgb2hsl(int r, int g, int b) {
            int[] hsl = {0, 0, 0};
            if (r != 0 || g != 0 || b != 0) {
                hsl[0] = (int) Math.round((180 / Math.PI) * Math.atan2(Math.sqrt(3) * (g - b), 2 * r - g - b));
                while (hsl[0] < 0) {
                    hsl[0] += 360;
                }
            }
            double mx = Math.max(Math.max(r, g), b) / 255.0;
            double mn = Math.min(Math.min(r, g), b) / 255.0;
            double l = (mx + mn) / 2;
            hsl[1] = l == 0 || l == 1 ? 0 : (int) Math.round(100.0 * (mx - mn) / (1 - Math.abs(2 * l - 1)));
            hsl[2] = (int) Math.round(100 * l);
            return hsl;
        }

        String getStyleRGB(String s) {
            if (fixedStyle == null) {
                double ry = Math.pow(r / 255.0, 2.2);
                double by = Math.pow(b / 255.0, 2.2);
                double gy = Math.pow(g / 255.0, 2.2);
                double y = 0.2126 * ry + 0.7151 * gy + 0.0721 * by;
                String fg = "black";
                if (1.05 / (y + 0.05) > (y + 0.05) / 0.05) {
                    fg = "white";
                }
                return "bg-rgb:" + String.format("#%02x%02x%02x", r, g, b) + ",fg:" + fg;
            } else {
                return (fixedBg ? "fg-rgb:" : "bg-rgb:") + String.format("#%02x%02x%02x", r, g, b) + "," + fixedStyle;
            }
        }

        public void printColor(String name, String style) throws IOException {
            setFixedStyle(style);
            int hueAngle;
            double zoom = 1;
            int[] rgb = {0, 0, 0};
            if (name.matches(COLORS_24BIT)) {
                rgb = rgb(Long.parseLong(name, 16));
                zoom = 2;
            } else if ((name.startsWith("#") || name.startsWith("x"))
                    && name.substring(1).matches(COLORS_24BIT)) {
                rgb = rgb(Long.parseLong(name.substring(1), 16));
                zoom = 2;
            } else if (COLORS_16.contains(name)) {
                for (int i = 0; i < 16; i++) {
                    if (COLORS_16.get(i).equals(name)) {
                        rgb = rgb(org.jline.utils.Colors.DEFAULT_COLORS_256[i]);
                        break;
                    }
                }
            } else if (name.matches("hue[1-3]?[0-9]{1,2}")) {
                hueAngle = Integer.parseInt(name.substring(3));
                if (hueAngle > 360) {
                    throw new IllegalArgumentException("Color not found: " + name);
                }
                rgb = hue2rgb(hueAngle);
            } else if (name.matches("[a-z0-9]+")) {
                List<String> colors = retrieveColorNames();
                if (colors.contains(name)) {
                    for (int i = 0; i < 256; i++) {
                        if (colors.get(i).equals(name)) {
                            rgb = rgb(org.jline.utils.Colors.DEFAULT_COLORS_256[i]);
                            break;
                        }
                    }
                } else {
                    boolean found = false;
                    for (int i = 0; i < 256; i++) {
                        if (colors.get(i).startsWith(name)) {
                            rgb = rgb(org.jline.utils.Colors.DEFAULT_COLORS_256[i]);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        for (int i = 0; i < 256; i++) {
                            if (colors.get(i).contains(name)) {
                                rgb = rgb(org.jline.utils.Colors.DEFAULT_COLORS_256[i]);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        throw new IllegalArgumentException("Color not found: " + name);
                    }
                }
            } else {
                throw new IllegalArgumentException("Color not found: " + name);
            }
            double step = 32;
            int barSize = 14;
            int width = terminal.getWidth();
            if (width > 287) {
                step = 8;
                barSize = 58;
            } else if (width > 143) {
                step = 16;
                barSize = 29;
            } else if (width > 98) {
                step = 24;
                barSize = 18;
            }
            r = rgb[0];
            g = rgb[1];
            b = rgb[2];
            int[] hsl = rgb2hsl(r, g, b);
            hueAngle = hsl[0];
            out.println("HSL: " + hsl[0] + "deg, " + hsl[1] + "%, " + hsl[2] + "%");
            if (hsl[2] > 85 || hsl[2] < 15 || hsl[1] < 15) {
                zoom = 1;
            }
            double div = zoom * 256.0 / step;
            int ndiv = (int) (div / zoom);
            double xrs = (0xFF - r) / div;
            double xgs = (0xFF - g) / div;
            double xbs = (0xFF - b) / div;
            double[] yrs = new double[ndiv], ygs = new double[ndiv], ybs = new double[ndiv];
            double[] ro = new double[ndiv], go = new double[ndiv], bo = new double[ndiv];
            AttributedStringBuilder asb = new AttributedStringBuilder();
            for (int y = 0; y < ndiv; y++) {
                for (int x = 0; x < ndiv; x++) {
                    if (y == 0) {
                        yrs[x] = (rgb[0] + x * xrs) / div;
                        ygs[x] = (rgb[1] + x * xgs) / div;
                        ybs[x] = (rgb[2] + x * xbs) / div;
                        ro[x] = rgb[0] + x * xrs;
                        go[x] = rgb[1] + x * xgs;
                        bo[x] = rgb[2] + x * xbs;
                        r = (int) ro[x];
                        g = (int) go[x];
                        b = (int) bo[x];
                    } else {
                        r = (int) (ro[x] - y * yrs[x]);
                        g = (int) (go[x] - y * ygs[x]);
                        b = (int) (bo[x] - y * ybs[x]);
                    }
                    String col = String.format("%02x%02x%02x", r, g, b);
                    AttributedStyle s = new StyleResolver(this::getStyleRGB).resolve(".rgb" + col);
                    asb.style(s);
                    asb.append(" ").append("#").append(col).append(" ");
                }
                asb.style(AttributedStyle.DEFAULT).append("\n");
            }
            asb.toAttributedString().println(terminal);
            if (hueAngle != -1) {
                int dAngle = 5;
                int zero = (int) (hueAngle - (dAngle / 2.0) * (barSize - 1));
                zero = zero - zero % 5;
                AttributedStringBuilder asb2 = new AttributedStringBuilder();
                for (int i = 0; i < barSize; i++) {
                    int angle = zero + dAngle * i;
                    while (angle < 0) {
                        angle += 360;
                    }
                    while (angle > 360) {
                        angle -= 360;
                    }
                    rgb = hue2rgb(angle);
                    r = rgb[0];
                    g = rgb[1];
                    b = rgb[2];
                    AttributedStyle s = new StyleResolver(this::getStyleRGB).resolve(".hue" + angle);
                    asb2.style(s);
                    asb2.append(" ").append(addPadding(3, "" + angle)).append(" ");
                }
                asb2.style(AttributedStyle.DEFAULT).append("\n");
                asb2.toAttributedString().println(terminal);
            }
        }
    }

    public static void highlighter(
            LineReader lineReader,
            Terminal terminal,
            PrintStream out,
            PrintStream err,
            String[] argv,
            ConfigurationPath configPath)
            throws HelpException {
        final String[] usage = {
            "highlighter -  manage nanorc theme system",
            "Usage: highlighter [OPTIONS]",
            "  -? --help                       Displays command help",
            "  -c --columns=COLUMNS            Number of columns in theme view",
            "  -l --list                       List available nanorc themes",
            "  -r --refresh                    Refresh highlighter config",
            "  -s --switch=THEME               Switch nanorc theme",
            "  -v --view=THEME                 View nanorc theme",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        try {
            if (opt.isSet("refresh")) {
                lineReader.getHighlighter().refresh(lineReader);
            } else if (opt.isSet("switch")) {
                Path userConfig = configPath.getUserConfig(DEFAULT_NANORC_FILE);
                if (userConfig != null) {
                    SyntaxHighlighter sh = SyntaxHighlighter.build(userConfig, null);
                    Path currentTheme = sh.getCurrentTheme();
                    String newTheme = replaceFileName(currentTheme, opt.get("switch"));
                    File themeFile = new File(newTheme);
                    if (themeFile.exists()) {
                        switchTheme(err, userConfig, newTheme);
                        Path lessConfig = configPath.getUserConfig(DEFAULT_LESSRC_FILE);
                        if (lessConfig != null) {
                            switchTheme(err, lessConfig, newTheme);
                        }
                        lineReader.getHighlighter().refresh(lineReader);
                    }
                }
            } else {
                Path config = configPath.getConfig(DEFAULT_NANORC_FILE);
                Path currentTheme =
                        config != null ? SyntaxHighlighter.build(config, null).getCurrentTheme() : null;
                if (currentTheme != null) {
                    if (opt.isSet("list")) {
                        String parameter = replaceFileName(currentTheme, "*" + TYPE_NANORCTHEME);
                        out.println(currentTheme.getParent() + ":");
                        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + parameter);
                        Files.find(
                                        Paths.get(new File(parameter).getParent()),
                                        Integer.MAX_VALUE,
                                        (path, f) -> pathMatcher.matches(path))
                                .forEach(p -> out.println(p.getFileName()));
                    } else {
                        File themeFile;
                        if (opt.isSet("view")) {
                            themeFile = new File(replaceFileName(currentTheme, opt.get("view")));
                        } else {
                            themeFile = currentTheme.toFile();
                        }
                        out.println(themeFile.getAbsolutePath());
                        try (BufferedReader reader = new BufferedReader(new FileReader(themeFile))) {
                            String line;
                            List<List<String>> tokens = new ArrayList<>();
                            int maxKeyLen = 0;
                            int maxValueLen = 0;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (line.length() > 0 && !line.startsWith("#")) {
                                    List<String> parts = Arrays.asList(line.split("\\s+", 2));
                                    if (parts.get(0).matches(REGEX_TOKEN_NAME)) {
                                        if (parts.get(0).length() > maxKeyLen) {
                                            maxKeyLen = parts.get(0).length();
                                        }
                                        if (parts.get(1).length() > maxValueLen) {
                                            maxValueLen = parts.get(1).length();
                                        }
                                        tokens.add(parts);
                                    }
                                }
                            }
                            AttributedStringBuilder asb = new AttributedStringBuilder();
                            maxKeyLen = maxKeyLen + 2;
                            maxValueLen = maxValueLen + 1;
                            int cols = opt.isSet("columns") ? opt.getNumber("columns") : 2;
                            List<Integer> tabstops = new ArrayList<>();
                            for (int c = 0; c < cols; c++) {
                                tabstops.add((c + 1) * maxKeyLen + c * maxValueLen);
                                tabstops.add((c + 1) * maxKeyLen + (c + 1) * maxValueLen);
                            }
                            asb.tabs(tabstops);
                            int ind = 0;
                            for (List<String> token : tokens) {
                                asb.style(AttributedStyle.DEFAULT).append(" ");
                                asb.style(compileStyle("token" + ind++, token.get(1)));
                                asb.append(token.get(0)).append("\t");
                                asb.append(token.get(1));
                                asb.style(AttributedStyle.DEFAULT).append("\t");
                                if ((ind % cols) == 0) {
                                    asb.style(AttributedStyle.DEFAULT).append("\n");
                                }
                            }
                            asb.toAttributedString().println(terminal);
                        }
                    }
                }
            }
        } catch (Exception e) {
            err.println(e.getMessage());
        }
    }

    private static void switchTheme(PrintStream err, Path config, String theme) {
        try (Stream<String> stream = Files.lines(config, StandardCharsets.UTF_8)) {
            List<String> list = stream.map(line ->
                            line.matches("\\s*" + COMMAND_THEME + "\\s+.*") ? COMMAND_THEME + " " + theme : line)
                    .collect(Collectors.toList());
            Files.write(config, list, StandardCharsets.UTF_8);
        } catch (IOException e) {
            err.println(e.getMessage());
        }
    }

    private static String replaceFileName(Path path, String name) {
        int nameLength = path.getFileName().toString().length();
        int pathLength = path.toString().length();
        return (path.toString().substring(0, pathLength - nameLength) + name).replace("\\", "\\\\");
    }

    private static AttributedStyle compileStyle(String reference, String colorDef) {
        Map<String, String> spec = new HashMap<>();
        spec.put(reference, colorDef);
        Styles.StyleCompiler sh = new Styles.StyleCompiler(spec, true);
        return new StyleResolver(sh::getStyle).resolve("." + reference);
    }
}
