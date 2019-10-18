/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jline.builtins.Commands;
import org.jline.builtins.Completers;
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.TTop;
import org.jline.builtins.Widgets.AutopairWidgets;
import org.jline.builtins.Widgets.AutosuggestionWidgets;
import org.jline.builtins.Widgets.TailTipWidgets;
import org.jline.builtins.Widgets.TailTipWidgets.TipType;
import org.jline.builtins.Widgets.ArgDesc;
import org.jline.builtins.Widgets.CmdDesc;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Status;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static org.jline.builtins.Completers.TreeCompleter.node;


public class Example
{
    public static void usage() {
        String[] usage = {
                "Usage: java " + Example.class.getName() + " [cases... [trigger mask]]"
              , "  Terminal:"
              , "    -system          terminalBuilder.system(false)"
              , "    +system          terminalBuilder.system(true)"
              , "  Completors:"
              , "    argumet          an argument completor & autosuggestion"
              , "    files            a completor that completes file names"
              , "    none             no completors"
              , "    param            a paramenter completer using Java functional interface"
              , "    regexp           a regex completer"
              , "    simple           a string completor that completes \"foo\", \"bar\" and \"baz\""
              , "    tree             a tree completer"
              , "  Multiline:"
              , "    brackets         eof on unclosed bracket"
              , "    quotes           eof on unclosed quotes"
              , "  Mouse:"
              , "    mouse            enable mouse"
              , "    mousetrack       enable tracking mouse"
              , "  Miscellaneous:"
              , "    color            colored left and right prompts"
              , "    status           multi-thread test of jline status line"
              , "    timer            widget 'Hello world'"
              , "    <trigger> <mask> password mask"
              , "  Example:"
              , "    java " + Example.class.getName() + " simple su '*'"};
        for (String u: usage) {
            System.out.println(u);
        }
    }

    public static void help() {
        String[] help = {
            "List of available commands:"
          , "  Builtin:"
          , "    complete        UNAVAILABLE"
          , "    history         list history of commands"
          , "    keymap          manipulate keymaps"
          , "    less            file pager"
          , "    nano            nano editor"
          , "    setopt          set options"
          , "    tmux            UNAVAILABLE"
          , "    ttop            display and update sorted information about threads"
          , "    unsetopt        unset options"
          , "    widget          ~UNAVAILABLE"
          , "  Example:"
          , "    autopair        toggle brackets/quotes autopair key bindings"
          , "    autosuggestion  history, completer, tailtip [tailtip|completer|combined] or none"
          , "    cls             clear screen"
          , "    help            list available commands"
          , "    exit            exit from example app"
          , "    set             set lineReader variable"
          , "    sleep           sleep 3 seconds"
          , "    testkey         display key events"
          , "    tput            set terminal capability"
          , "  Additional help:"
          , "    <command> --help"};
        for (String u: help) {
            System.out.println(u);
        }

    }

    private static Map<String,CmdDesc> compileTailTips() {
        Map<String, CmdDesc> tailTips = new HashMap<>();
        Map<String, List<AttributedString>> optDesc = new HashMap<>();
        optDesc.put("--optionA", Arrays.asList(new AttributedString("optionA description...")));
        optDesc.put("--noitpoB", Arrays.asList(new AttributedString("noitpoB description...")));
        optDesc.put("--optionC", Arrays.asList(new AttributedString("optionC description...")
                                             , new AttributedString("line2")));
        Map<String, List<AttributedString>> widgetOpts = new HashMap<>();
        List<AttributedString> mainDesc = Arrays.asList(new AttributedString("widget -N new-widget [function-name]")
                                        , new AttributedString("widget -D widget ...")
                                        , new AttributedString("widget -A old-widget new-widget")
                                        , new AttributedString("widget -U string ...")
                                        , new AttributedString("widget -l [options]")
                       );
        widgetOpts.put("-N", Arrays.asList(new AttributedString("Create new widget")));
        widgetOpts.put("-D", Arrays.asList(new AttributedString("Delete widgets")));
        widgetOpts.put("-A", Arrays.asList(new AttributedString("Create alias to widget")));
        widgetOpts.put("-U", Arrays.asList(new AttributedString("Push characters to the stack")));
        widgetOpts.put("-l", Arrays.asList(new AttributedString("List user-defined widgets")));

        tailTips.put("widget", new CmdDesc(mainDesc, ArgDesc.doArgNames(Arrays.asList("[pN...]")), widgetOpts));
        tailTips.put("foo12", new CmdDesc(ArgDesc.doArgNames(Arrays.asList("param1", "param2", "[paramN...]"))));
        tailTips.put("foo11", new CmdDesc(Arrays.asList(
                new ArgDesc("param1",Arrays.asList(new AttributedString("Param1 description...")
                                                , new AttributedString("line 2: This is a very long line that does exceed the terminal width."
                                                      +" The line will be truncated automatically (by Status class) be before printing out.")
                                                , new AttributedString("line 3")
                                                , new AttributedString("line 4")
                                                , new AttributedString("line 5")
                                                , new AttributedString("line 6")
                                                  ))
              , new ArgDesc("param2",Arrays.asList(new AttributedString("Param2 description...")
                                                , new AttributedString("line 2")
                                                  ))
              , new ArgDesc("param3", new ArrayList<>())
              ), optDesc));
        return tailTips;
    }

    private static class Executor {
        LineReader reader;
        Terminal terminal;

        public Executor(LineReader reader) {
            this.reader = reader;
            this.terminal = reader.getTerminal();
        }

        public boolean execute(String command, String[] argv) throws Exception {
            boolean out = true;
            if ("tmux".equals(command)) {
                Commands.tmux(terminal, System.out, System.err,
                        null, //Supplier<Object> getter,
                        null, //Consumer<Object> setter,
                        null, //Consumer<Terminal> runner,
                        argv);
            }
            else if ("nano".equals(command)) {
                Commands.nano(terminal, System.out, System.err,
                        Paths.get(""),
                        argv);
            }
            else if ("less".equals(command)) {
                Commands.less(terminal, System.in, System.out, System.err,
                        Paths.get(""),
                        argv);
            }
            else if ("history".equals(command)) {
                Commands.history(reader, System.out, System.err, Paths.get(""),argv);
            }
            else if ("complete".equals(command)) {
                Commands.complete(reader, System.out, System.err,
                        null, // Map<String, List<CompletionData>> completions,
                        argv);
            }
            else if ("widget".equals(command)) {
                Commands.widget(reader, System.out, System.err,
                        null, //Function<String, Widget> widgetCreator,
                        argv);
            }
            else if ("keymap".equals(command)) {
                Commands.keymap(reader, System.out, System.err, argv);
            }
            else if ("setopt".equals(command)) {
                Commands.setopt(reader, System.out, System.err, argv);
            }
            else if ("unsetopt".equals(command)) {
                Commands.unsetopt(reader, System.out, System.err, argv);
            }
            else if ("ttop".equals(command)) {
                TTop.ttop(terminal, System.out, System.err, argv);
            }
            else {
                out = false;
            }
            return out;
        }

        CmdDesc commandDescription(String command) {
            CmdDesc out = null;
            String[] argv = {"--help"};
            try {
                execute(command, argv);
           } catch (HelpException e) {
                List<AttributedString> main = new ArrayList<>();
                Map<String, List<AttributedString>> options = new HashMap<>();
                String[] msg = e.getMessage().split("\\n");
                String prevOpt = null;
                boolean mainDone = false;
                boolean start = false;
                for (String s: msg) {
                    if (!start) {
                        if (s.trim().startsWith("Usage: ")) {
                            s = s.split("Usage:")[1];
                            start = true;
                        } else {
                            continue;
                        }
                    }
                    if (s.matches("^\\s+-.*$")) {
                        mainDone = true;
                        int ind = s.lastIndexOf("  ");
                        if (ind > 0) {
                            String o = s.substring(0, ind);
                            String d = s.substring(ind);
                            if (o.trim().length() > 0) {
                                prevOpt = o.trim();
                                options.put(prevOpt, new ArrayList<>(Arrays.asList(new AttributedString(d.trim()))));
                            }
                        }
                    } else if (s.matches("^[\\s]{20}.*$") && prevOpt != null && options.containsKey(prevOpt)) {
                        int ind = s.lastIndexOf("  ");
                        if (ind > 0) {
                            options.get(prevOpt).add(new AttributedString(s.substring(ind).trim()));
                        }
                    } else {
                        prevOpt = null;
                    }
                    if (!mainDone) {
                        main.add(new AttributedString(s.trim()));
                    }
                }
                out = new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("[pN...]")), options);
            } catch (Exception e) {

            }
            return out;
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            String prompt = "prompt> ";
            String rightPrompt = null;
            Character mask = null;
            String trigger = null;
            boolean color = false;
            boolean timer = false;

            TerminalBuilder builder = TerminalBuilder.builder();

            if ((args == null) || (args.length == 0)) {
                usage();

                return;
            }

            int mouse = 0;
            Completer completer = null;
            Parser parser = null;
            List<Consumer<LineReader>> callbacks = new ArrayList<>();

            for (int index=0; index < args.length; index++) {
                switch (args[index]) {
                    /* SANDBOX JANSI
                    case "-posix":
                        builder.posix(false);
                        break;
                    case "+posix":
                        builder.posix(true);
                        break;
                    case "-native-pty":
                        builder.nativePty(false);
                        break;
                    case "+native-pty":
                        builder.nativePty(true);
                        break;
                    */
                    case "timer":
                        timer = true;
                        break;
                    case "-system":
                        builder.system(false).streams(System.in, System.out);
                        break;
                    case "+system":
                        builder.system(true);
                        break;
                    case "none":
                        break;
                    case "files":
                        completer = new Completers.FileNameCompleter();
                        break;
                    case "simple":
                        completer = new StringsCompleter("foo", "bar", "baz");
                        break;
                    case "quotes":
                        DefaultParser p = new DefaultParser();
                        p.setEofOnUnclosedQuote(true);
                        parser = p;
                        break;
                    case "brackets":
                        prompt = "long-prompt> ";
                        DefaultParser p2 = new DefaultParser();
                        p2.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE);
                        parser = p2;
                        break;
                    case "status":
                        completer = new StringsCompleter("foo", "bar", "baz");
                        callbacks.add(reader -> {
                            new Thread(() -> {
                                int counter = 0;
                                while (true) {
                                    try {
                                        Status status = Status.getStatus(reader.getTerminal());
                                        counter++;
                                        status.update(Arrays.asList(new AttributedStringBuilder().append("counter: " + counter).toAttributedString()));
                                        ((LineReaderImpl) reader).redisplay();
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        });
                        break;
                    case "argument":
                        completer = new ArgumentCompleter(
                                new Completer() {
                                    @Override
                                    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                                        candidates.add(new Candidate("foo11", "foo11", null, "complete cmdDesc", null, null, true));
                                        candidates.add(new Candidate("foo12", "foo12", null, "cmdDesc -names only", null, null, true));
                                        candidates.add(new Candidate("foo13", "foo13", null, "-", null, null, true));
                                        candidates.add(new Candidate("widget", "widget", null, "cmdDesc with short options", null, null, true));
                                    }
                                },
                                new StringsCompleter("foo21", "foo22", "foo23"),
                                new Completer() {
                                    @Override
                                    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                                        candidates.add(new Candidate("", "", null, "frequency in MHz", null, null, false));
                                    }
                                });
                        break;
                    case "param":
                        completer = (reader, line, candidates) -> {
                            if (line.wordIndex() == 0) {
                                candidates.add(new Candidate("Command1"));
                            } else if (line.words().get(0).equals("Command1")) {
                                if (line.words().get(line.wordIndex() - 1).equals("Option1")) {
                                    candidates.add(new Candidate("Param1"));
                                    candidates.add(new Candidate("Param2"));
                                } else {
                                    if (line.wordIndex() == 1) {
                                        candidates.add(new Candidate("Option1"));
                                    }
                                    if (!line.words().contains("Option2")) {
                                        candidates.add(new Candidate("Option2"));
                                    }
                                    if (!line.words().contains("Option3")) {
                                        candidates.add(new Candidate("Option3"));
                                    }
                                }
                            }
                        };
                        break;
                    case "tree":
                        completer = new TreeCompleter(
                           node("Command1",
                                   node("Option1",
                                        node("Param1", "Param2")),
                                   node("Option2"),
                                   node("Option3")));
                        break;
                    case "regexp":
                        Map<String, Completer> comp = new HashMap<>();
                        comp.put("C1", new StringsCompleter("cmd1"));
                        comp.put("C11", new StringsCompleter("--opt11", "--opt12"));
                        comp.put("C12", new StringsCompleter("arg11", "arg12", "arg13"));
                        comp.put("C2", new StringsCompleter("cmd2"));
                        comp.put("C21", new StringsCompleter("--opt21", "--opt22"));
                        comp.put("C22", new StringsCompleter("arg21", "arg22", "arg23"));
                        completer = new Completers.RegexCompleter("C1 C11* C12+ | C2 C21* C22+", comp::get);
                        break;
                    case "color":
                        color = true;
                        prompt = new AttributedStringBuilder()
                                .style(AttributedStyle.DEFAULT.background(AttributedStyle.GREEN))
                                .append("foo")
                                .style(AttributedStyle.DEFAULT)
                                .append("@bar")
                                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                .append("\nbaz")
                                .style(AttributedStyle.DEFAULT)
                                .append("> ").toAnsi();
                        rightPrompt = new AttributedStringBuilder()
                                .style(AttributedStyle.DEFAULT.background(AttributedStyle.RED))
                                .append(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                                .append("\n")
                                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED | AttributedStyle.BRIGHT))
                                .append(LocalTime.now().format(new DateTimeFormatterBuilder()
                                                .appendValue(HOUR_OF_DAY, 2)
                                                .appendLiteral(':')
                                                .appendValue(MINUTE_OF_HOUR, 2)
                                                .toFormatter()))
                                .toAnsi();
                        completer = new StringsCompleter("\u001B[1mfoo\u001B[0m", "bar", "\u001B[32mbaz\u001B[0m", "foobar");
                        break;
                    case "mouse":
                        mouse = 1;
                        break;
                    case "mousetrack":
                        mouse = 2;
                        break;
                    default:
                        if (index==0) {
                            usage();
                            return;
                        } else if (args.length == index + 2) {
                            mask = args[index+1].charAt(0);
                            trigger = args[index];
                            index = args.length;
                        } else {
                            System.out.println("Bad test case: " + args[index]);
                        }
                }
            }
            Terminal terminal = builder.build();
            System.out.println(terminal.getName()+": "+terminal.getType());
            System.out.println("\nhelp: list available commands");
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .parser(parser)
                    .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                    .variable(LineReader.INDENTATION, 2)
                    .option(Option.INSERT_BRACKET, true)
                    .build();

            Executor executor = new Executor(reader);

            AutopairWidgets autopairWidgets = new AutopairWidgets(reader);
            AutosuggestionWidgets autosuggestionWidgets = new AutosuggestionWidgets(reader);

//            TailTipWidgets tailtipWidgets = new TailTipWidgets(reader, compileTailTips(), 5, TipType.COMPLETER);
            TailTipWidgets tailtipWidgets = new TailTipWidgets(reader, executor::commandDescription, 5, TipType.COMPLETER);

            if (timer) {
                Executors.newScheduledThreadPool(1)
                        .scheduleAtFixedRate(() -> {
                            reader.callWidget(LineReader.CLEAR);
                            reader.getTerminal().writer().println("Hello world!");
                            reader.callWidget(LineReader.REDRAW_LINE);
                            reader.callWidget(LineReader.REDISPLAY);
                            reader.getTerminal().writer().flush();
                        }, 1, 1, TimeUnit.SECONDS);
            }
            if (mouse != 0) {
                reader.setOpt(LineReader.Option.MOUSE);
                if (mouse == 2) {
                    reader.getWidgets().put(LineReader.CALLBACK_INIT, () -> {
                        terminal.trackMouse(Terminal.MouseTracking.Any);
                        return true;
                    });
                    reader.getWidgets().put(LineReader.MOUSE, () -> {
                        MouseEvent event = reader.readMouseEvent();
                        StringBuilder tsb = new StringBuilder();
                        Cursor cursor = terminal.getCursorPosition(c -> tsb.append((char) c));
                        reader.runMacro(tsb.toString());
                        String msg = "          " + event.toString();
                        int w = terminal.getWidth();
                        terminal.puts(Capability.cursor_address, 0, Math.max(0, w - msg.length()));
                        terminal.writer().append(msg);
                        terminal.puts(Capability.cursor_address, cursor.getY(), cursor.getX());
                        terminal.flush();
                        return true;
                    });
                }
            }
            callbacks.forEach(c -> c.accept(reader));
            if (!callbacks.isEmpty()) {
                Thread.sleep(2000);
            }
            while (true) {
                try {
                    String line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                    line = line.trim();

                    if (color) {
                        terminal.writer().println(
                            AttributedString.fromAnsi("\u001B[33m======>\u001B[0m\"" + line + "\"")
                                .toAnsi(terminal));

                    } else {
                        terminal.writer().println("======>\"" + line + "\"");
                    }
                    terminal.flush();

                    // If we input the special word then we will mask
                    // the next line.
                    if ((trigger != null) && (line.compareTo(trigger) == 0)) {
                        line = reader.readLine("password> ", mask);
                    }
                    if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                        break;
                    }
                    ParsedLine pl = reader.getParser().parse(line, 0);
                    String[] argv = pl.words().subList(1, pl.words().size()).toArray(new String[0]);
                    if ("help".equals(pl.word()) || "?".equals(pl.word())) {
                        help();
                    }
                    else if (executor.execute(pl.word(), argv)) {
                        // built-in command has been executed
                    }
                    else if ("set".equals(pl.word())) {
                        if (argv.length == 2) {
                            reader.setVariable(argv[0], argv[1]);
                        } else {
                            terminal.writer().println("Usage: set <name> <value>");
                        }
                    }
                    else if ("tput".equals(pl.word())) {
                        if (argv.length == 1) {
                            Capability vcap = Capability.byName(argv[0]);
                            if (vcap != null) {
                                terminal.puts(vcap);
                            } else {
                                terminal.writer().println("Unknown capability");
                            }
                        } else {
                            terminal.writer().println("Usage: tput <capability>");
                        }
                    }
                    else if ("testkey".equals(pl.word())) {
                        terminal.writer().write("Input the key event(Enter to complete): ");
                        terminal.writer().flush();
                        StringBuilder sb = new StringBuilder();
                        while (true) {
                            int c = ((LineReaderImpl) reader).readCharacter();
                            if (c == 10 || c == 13) break;
                            sb.append(new String(Character.toChars(c)));
                        }
                        terminal.writer().println(KeyMap.display(sb.toString()));
                        terminal.writer().flush();
                    }
                    else if ("cls".equals(pl.word())) {
                        terminal.puts(Capability.clear_screen);
                        terminal.flush();
                    }
                    else if ("sleep".equals(pl.word())) {
                        Thread.sleep(3000);
                    }
                    else if ("autopair".equals(pl.word())) {
                        terminal.writer().print("Autopair widgets are ");
                        if (autopairWidgets.toggle()) {
                            terminal.writer().println("enabled.");
                        } else {
                            terminal.writer().println("disabled.");
                        }
                    }
                    else if ("autosuggestion".equals(pl.word())) {
                        if (argv.length > 0) {
                            String type = argv[0].toLowerCase();
                            if (type.startsWith("his")) {
                                tailtipWidgets.disable();
                                autosuggestionWidgets.enable();
                            } else if (type.startsWith("tai")) {
                                autosuggestionWidgets.disable();
                                tailtipWidgets.enable();
                                if (argv.length > 1) {
                                    String mode = argv[1].toLowerCase();
                                    if (mode.startsWith("tai")) {
                                        tailtipWidgets.setTipType(TipType.TAIL_TIP);
                                    } else if (mode.startsWith("comp")) {
                                        tailtipWidgets.setTipType(TipType.COMPLETER);
                                    } else if (mode.startsWith("comb")) {
                                        tailtipWidgets.setTipType(TipType.COMBINED);
                                    }
                                }
                            } else if (type.startsWith("com")) {
                                autosuggestionWidgets.disable();
                                tailtipWidgets.disable();
                                reader.setAutosuggestion(SuggestionType.COMPLETER);
                            } else if (type.startsWith("non")) {
                                autosuggestionWidgets.disable();
                                tailtipWidgets.disable();
                                reader.setAutosuggestion(SuggestionType.NONE);
                            } else {
                                terminal.writer().println("Usage: autosuggestion history|completer|tailtip|none");
                            }
                        } else {
                            if (tailtipWidgets.isEnabled()) {
                                terminal.writer().println("Autosuggestion: tailtip/" + tailtipWidgets.getTipType());
                            } else {
                                terminal.writer().println("Autosuggestion: " + reader.getAutosuggestion());
                            }
                        }
                    }
                }
                catch (HelpException e) {
                    HelpException.highlight(e.getMessage(), HelpException.defaultStyle()).print(terminal);
                }
                catch (IllegalArgumentException|FileNotFoundException e) {
                    System.out.println(e.getMessage());
                }
                catch (UserInterruptException e) {
                    // Ignore
                }
                catch (EndOfFileException e) {
                    return;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
