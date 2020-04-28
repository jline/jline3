/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jline.builtins.Completers.CompletionData;
import org.jline.builtins.Options;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.Source.StdInSource;
import org.jline.builtins.Source.URLSource;
import org.jline.console.ConfigurationPath;
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

public class Commands {

    public static void tmux(Terminal terminal, PrintStream out, PrintStream err,
                            Supplier<Object> getter,
                            Consumer<Object> setter,
                            Consumer<Terminal> runner,
                            String[] argv) throws Exception {
        final String[] usage = {
                "tmux -  terminal multiplexer",
                "Usage: tmux [command]",
                "  -? --help                    Show help",
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

    public static void nano(Terminal terminal, PrintStream out, PrintStream err,
            Path currentDir,
            String[] argv) throws Exception {
        nano(terminal, out, err, currentDir, argv, null);
    }

    public static void nano(Terminal terminal, PrintStream out, PrintStream err,
            Path currentDir,
            String[] argv,
            ConfigurationPath configPath) throws Exception {
        Options opt = Options.compile(Nano.usage()).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        Nano edit = new Nano(terminal, currentDir, opt, configPath);
        edit.open(opt.args());
        edit.run();
    }

    public static void less(Terminal terminal, InputStream in, PrintStream out, PrintStream err,
            Path currentDir,
            String[] argv) throws Exception {
        less(terminal, in, out, err, currentDir, argv, null);
    }

    public static void less(Terminal terminal, InputStream in, PrintStream out, PrintStream err,
                            Path currentDir,
                            String[] argv,
                            ConfigurationPath configPath) throws Exception {
        Options opt = Options.compile(Less.usage()).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        Less less = new Less(terminal, currentDir, opt, configPath);
        List<Source> sources = new ArrayList<>();
        if (opt.args().isEmpty()) {
            opt.args().add("-");
        }
        for (String arg : opt.args()) {
            arg = arg.startsWith("~") ? arg.replace("~", System.getProperty("user.home")) : arg;
            if ("-".equals(arg)) {
                sources.add(new StdInSource(in));
            } else if (arg.contains("*") || arg.contains("?")) {
                for (Path p: findFiles(currentDir, arg)) {
                    sources.add(new URLSource(p.toUri().toURL(), p.toString()));
                }
            } else {
                sources.add(new URLSource(currentDir.resolve(arg).toUri().toURL(), arg));
            }
        }
        less.run(sources);
    }

    protected static List<Path> findFiles(Path root, String files) throws IOException{
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
            regex = (root.toString().length() == 0 ? "" : root.toString() + "/") + files;
            regex = regex.replaceAll("\\\\", "/").replaceAll("//", "/");
            searchRoot = root;
        }
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+regex);
        return Files.find(searchRoot, Integer.MAX_VALUE, (path, f)->pathMatcher.matches(path)).collect(Collectors.toList());
    }

    public static void history(LineReader reader, PrintStream out, PrintStream err, Path currentDir,
                               String[] argv) throws Exception {
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
                "  -s                              Re-executes the command without invoking an editor"};
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
            for (char c: opt.args().get(argId++).toCharArray()) {
                if (c == '*' && prev != '\\' && prev != '.') {
                    sb.append('.');
                }
                sb.append(c);
                prev = c;
            }
            pattern = Pattern.compile(sb.toString(), Pattern.DOTALL);
        }
        boolean reverse = opt.isSet("r") || (opt.isSet("s") && opt.args().size() <= argId);
        int firstId = opt.args().size() > argId ? retrieveHistoryId(history, opt.args().get(argId++)) : -17;
        int lastId  = opt.args().size() > argId ? retrieveHistoryId(history, opt.args().get(argId++)) : -1;
        firstId = historyId(firstId, history.first(), history.last());
        lastId  = historyId(lastId, history.first(), history.last());
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
            iter =  history.reverseIterator(lastId);
        } else {
            iter =  history.iterator(firstId);
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
                        LocalDateTime lt = LocalDateTime.from(entry.time().atZone(ZoneId.systemDefault())
                                .truncatedTo(ChronoUnit.MINUTES));
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
                } else if (opt.args().size() > 0 ) {
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

    public static void complete(LineReader reader, PrintStream out, PrintStream err,
                                Map<String, List<CompletionData>> completions,
                                String[] argv) throws HelpException {
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
                "                                  specified command has a zero exit status"};

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

    public static void widget(LineReader reader, PrintStream out, PrintStream err,
                              Function<String, Widget> widgetCreator,
                              String[] argv) throws Exception {
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
                for (String s: temp) {
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
        }
        else if (opt.isSet("N")) {
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
        }
        else if (opt.isSet("U")) {
            for (String arg : opt.args()) {
                reader.runMacro(KeyMap.translate(arg));
            }
        }
        else if (opt.args().size() == 1) {
            reader.callWidget(opt.args().get(0));
        }
    }

    public static void keymap(LineReader reader,
                              PrintStream out,
                              PrintStream err,
                              String[] argv) throws HelpException {
        final String[] usage = {
                "keymap -  manipulate keymaps",
                "Usage: keymap [options] -l [-L] [keymap ...]",
                "       keymap [options] -d",
                "       keymap [options] -D keymap ...",
                "       keymap [options] -A old-keymap new-keymap",
                "       keymap [options] -N new-keymap [old-keymap]",
                "       keymap [options] -m",
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
        }
        else if (opt.isSet("N")) {
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
        }
        else if (opt.isSet("A")) {
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
        }
        else if (opt.isSet("d")) {
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
        }
        else if (opt.isSet("D")) {
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
        }
        else if (opt.isSet("r")) {
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
                        if (prefix && k.startsWith(seq) && k.length() > seq.length()
                                || !prefix && k.equals(seq)) {
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
        }
        else if (opt.isSet("s") || opt.args().size() > 1) {
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
        }
        else {
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
                            String n = (last.length() > 1 ? last.substring(0, last.length() - 1) : "") + (char) (last.charAt(last.length() - 1) + 1);
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
                                    out.println(sb.toString());
                                } else {
                                    if (commands) {
                                        sb.append("-R ");
                                    }
                                    sb.append(KeyMap.display(begin.getKey()));
                                    sb.append("-");
                                    sb.append(KeyMap.display(last));
                                    sb.append(" ");
                                    displayValue(sb, begin.getValue());
                                    out.println(sb.toString());
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

    public static void setopt(LineReader reader,
                              PrintStream out,
                              PrintStream err,
                              String[] argv) throws HelpException {
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
                    out.println((option.isDef() ? "no-" : "") + option.toString().toLowerCase().replace('_', '-'));
                }
            }
        }
        else {
            boolean match = opt.isSet("m");
            doSetOpts(reader, out, err, opt.args(), match, true);
        }
    }

    public static void unsetopt(LineReader reader,
                                PrintStream out,
                                PrintStream err,
                                String[] argv) throws HelpException {
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
                    out.println((option.isDef() ? "no-" : "") + option.toString().toLowerCase().replace('_', '-'));
                }
            }
        }
        else {
            boolean match = opt.isSet("m");
            doSetOpts(reader, out, err, opt.args(), match, false);
        }
    }

    private static void doSetOpts(LineReader reader, PrintStream out, PrintStream err, List<String> options, boolean match, boolean set) {
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
            sb.append(value.toString());
        }
    }

    public static void setvar(LineReader lineReader, PrintStream out, PrintStream err, String[] argv) throws HelpException {
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
            for (Map.Entry<String, Object> entry: lineReader.getVariables().entrySet()) {
                out.println(entry.getKey() + ": " + entry.getValue());
            }
        } else if (opt.args().size() == 1) {
            out.println(lineReader.getVariable(opt.args().get(0)));
        } else {
            lineReader.setVariable(opt.args().get(0), opt.args().get(1));
        }
    }

}
