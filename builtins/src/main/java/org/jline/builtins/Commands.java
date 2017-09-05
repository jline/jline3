/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jline.builtins.Completers.CompletionData;
import org.jline.builtins.Source.StdInSource;
import org.jline.builtins.Source.URLSource;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
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
        // Simplified parsing
        if (argv.length == 1 && ("--help".equals(argv[0]) || "-?".equals(argv[0]))) {
            for (String s : usage) {
                err.println(s);
            }
            return;
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
        final String[] usage = {
                "nano -  edit files",
                "Usage: nano [FILES]",
                "  -? --help                    Show help",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(err);
            return;
        }
        Nano edit = new Nano(terminal, currentDir);
        edit.open(opt.args());
        edit.run();
    }

    public static void less(Terminal terminal, InputStream in, PrintStream out, PrintStream err,
                            Path currentDir,
                            String[] argv) throws IOException, InterruptedException {
        final String[] usage = {
                "less -  file pager",
                "Usage: less [OPTIONS] [FILES]",
                "  -? --help                    Show help",
                "  -e --quit-at-eof             Exit on second EOF",
                "  -E --QUIT-AT-EOF             Exit on EOF",
                "  -q --quiet --silent          Silent mode",
                "  -Q --QUIET --SILENT          Completely  silent",
                "  -S --chop-long-lines         Do not fold long lines",
                "  -i --ignore-case             Search ignores lowercase case",
                "  -I --IGNORE-CASE             Search ignores all case",
                "  -x --tabs                    Set tab stops",
                "  -N --LINE-NUMBERS            Display line number for each line"
        };

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            opt.usage(err);
            return;
        }

        Less less = new Less(terminal);
        less.quitAtFirstEof = opt.isSet("QUIT-AT-EOF");
        less.quitAtSecondEof = opt.isSet("quit-at-eof");
        less.quiet = opt.isSet("quiet");
        less.veryQuiet = opt.isSet("QUIET");
        less.chopLongLines = opt.isSet("chop-long-lines");
        less.ignoreCaseAlways = opt.isSet("IGNORE-CASE");
        less.ignoreCaseCond = opt.isSet("ignore-case");
        if (opt.isSet("tabs")) {
            less.tabs = opt.getNumber("tabs");
        }
        less.printLineNumbers = opt.isSet("LINE-NUMBERS");
        List<Source> sources = new ArrayList<>();
        if (opt.args().isEmpty()) {
            opt.args().add("-");
        }
        for (String arg : opt.args()) {
            if ("-".equals(arg)) {
                sources.add(new StdInSource(in));
            } else {
                sources.add(new URLSource(currentDir.resolve(arg).toUri().toURL(), arg));
            }
        }
        less.run(sources);
    }

    public static void history(LineReader reader, PrintStream out, PrintStream err,
                               String[] argv) throws IOException {
        final String[] usage = {
                "history -  list history of commands",
                "Usage: history [OPTIONS]",
                "  -? --help                       Displays command help",
                "     --clear                      Clear history",
                "     --save                       Save history",
                "  -d                              Print timestamps for each event"};

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            opt.usage(err);
            return;
        }
        if (!opt.args().isEmpty()) {
            err.println("usage: history [OPTIONS]");
            return;
        }

        History history = reader.getHistory();
        if (opt.isSet("clear")) {
            history.purge();
        }
        if (opt.isSet("save")) {
            history.save();
        }
        if (opt.isSet("clear") || opt.isSet("save")) {
            return;
        }
        for (History.Entry entry : history) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.append("  ");
            sb.styled(AttributedStyle::bold, String.format("%3d", entry.index() + 1));
            if (opt.isSet("d")) {
                sb.append("  ");
                LocalTime lt = LocalTime.from(entry.time().atZone(ZoneId.systemDefault()))
                        .truncatedTo(ChronoUnit.SECONDS);
                DateTimeFormatter.ISO_LOCAL_TIME.formatTo(lt, sb);
            }
            sb.append("  ");
            sb.append(entry.line());
            out.println(sb.toAnsi(reader.getTerminal()));
        }
    }

    public static void complete(LineReader reader, PrintStream out, PrintStream err,
                                Map<String, List<CompletionData>> completions,
                                String[] argv) {
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
            opt.usage(err);
            return;
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
                "Usage: widget [options] -N new-widget [function-name]",
                "       widget [options] -D widget ...",
                "       widget [options] -A old-widget new-widget",
                "       widget [options] -U string ...",
                "       widget [options] -l",
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
            opt.usage(err);
            return;
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
            Set<String> widgets = new TreeSet<>(reader.getWidgets().keySet());
            if (!opt.isSet("a")){
                widgets.removeAll(reader.getBuiltinWidgets().keySet());
            }
            widgets.forEach(out::println);
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
            Widget org = reader.getWidgets().get(opt.args().get(0));
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
                              String[] argv) {
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
                "  -r                              Unbind specified in-strings",
                "  -s                              Bind each in-string to each out-string",
                "  -v                              Select viins keymap and bind it to main",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(err);
            return;
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
                              String[] argv) {
        final String[] usage = {
                "setopt -  set options",
                "Usage: setopt [-m] option ...",
                "       setopt",
                "  -? --help                       Displays command help",
                "  -m                              Use pattern matching"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(err);
            return;
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
                                String[] argv) {
        final String[] usage = {
                "unsetopt -  unset options",
                "Usage: unsetopt [-m] option ...",
                "       unsetopt",
                "  -? --help                       Displays command help",
                "  -m                              Use pattern matching"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(err);
            return;
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

}
