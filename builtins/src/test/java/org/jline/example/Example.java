/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.example;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jline.builtins.Completers;
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
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

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static org.jline.builtins.Completers.TreeCompleter.node;


public class Example
{
    public static void usage() {
        System.out.println("Usage: java " + Example.class.getName()
            + " [none/simple/files/dictionary [trigger mask]]");
        System.out.println("  none - no completors");
        System.out.println("  simple - a simple completor that comples "
            + "\"foo\", \"bar\", and \"baz\"");
        System.out
            .println("  files - a completor that comples " + "file names");
        System.out.println("  classes - a completor that comples "
            + "java class names");
        System.out
            .println("  trigger - a special word which causes it to assume "
                + "the next line is a password");
        System.out.println("  mask - is the character to print in place of "
            + "the actual password character");
        System.out.println("  color - colored prompt and feedback");
        System.out.println("\n  E.g - java Example simple su '*'\n"
            + "will use the simple completor with 'su' triggering\n"
            + "the use of '*' as a password mask.");
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

            int index = 0;
            label:
            while (args.length > index) {
                switch (args[index]) {
                    /* SANDBOX JANSI
                    case "-posix":
                        builder.posix(false);
                        index++;
                        break;
                    case "+posix":
                        builder.posix(true);
                        index++;
                        break;
                    case "-native-pty":
                        builder.nativePty(false);
                        index++;
                        break;
                    case "+native-pty":
                        builder.nativePty(true);
                        index++;
                        break;
                    */
                    case "timer":
                        timer = true;
                        index++;
                        break;
                    case "-system":
                        builder.system(false);
                        index++;
                        break;
                    case "+system":
                        builder.system(true);
                        index++;
                        break;
                    case "none":
                        break label;
                    case "files":
                        completer = new Completers.FileNameCompleter();
                        break label;
                    case "simple":
                        completer = new StringsCompleter("foo", "bar", "baz");
                        break label;
                    case "quotes":
                        DefaultParser p = new DefaultParser();
                        p.setEofOnUnclosedQuote(true);
                        parser = p;
                        break label;
                    case "foo":
                        completer = new ArgumentCompleter(
                                new StringsCompleter("foo11", "foo12", "foo13"),
                                new StringsCompleter("foo21", "foo22", "foo23"),
                                new Completer() {
                                    @Override
                                    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                                        candidates.add(new Candidate("", "", null, "frequency in MHz", null, null, false));
                                    }
                                });
                        break label;
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
                        break label;
                    case "tree":
                        completer = new TreeCompleter(
                           node("Command1",
                                   node("Option1",
                                        node("Param1", "Param2")),
                                   node("Option2"),
                                   node("Option3")));
                        break label;
                    case "regexp":
                        Map<String, Completer> comp = new HashMap<>();
                        comp.put("C1", new StringsCompleter("cmd1"));
                        comp.put("C11", new StringsCompleter("--opt11", "--opt12"));
                        comp.put("C12", new StringsCompleter("arg11", "arg12", "arg13"));
                        comp.put("C2", new StringsCompleter("cmd2"));
                        comp.put("C21", new StringsCompleter("--opt21", "--opt22"));
                        comp.put("C22", new StringsCompleter("arg21", "arg22", "arg23"));
                        completer = new Completers.RegexCompleter("C1 C11* C12+ | C2 C21* C22+", comp::get);
                        break label;
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
                        break label;
                    case "mouse":
                        mouse = 1;
                        break label;
                    case "mousetrack":
                        mouse = 2;
                        break label;
                    default:
                        usage();
                        return;
                }
            }

            if (args.length == index + 2) {
                mask = args[index+1].charAt(0);
                trigger = args[index];
            }

            Terminal terminal = builder.build();

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .parser(parser)
                    .build();

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

            while (true) {
                String line = null;
                try {
                    line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return;
                }
                if (line == null) {
                    continue;
                }

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
                if ("set".equals(pl.word())) {
                    if (pl.words().size() == 3) {
                        reader.setVariable(pl.words().get(1), pl.words().get(2));
                    }
                }
                else if ("tput".equals(pl.word())) {
                    if (pl.words().size() == 2) {
                        Capability vcap = Capability.byName(pl.words().get(1));
                        if (vcap != null) {
                            terminal.puts(vcap);
                        } else {
                            terminal.writer().println("Unknown capability");
                        }
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
                else if ("bindkey".equals(pl.word())) {
                    if (pl.words().size() == 1) {
                        StringBuilder sb = new StringBuilder();
                        Map<String, Binding> bound = reader.getKeys().getBoundKeys();
                        for (Map.Entry<String, Binding> entry : bound.entrySet()) {
                            sb.append("\"");
                            entry.getKey().chars().forEachOrdered(c -> {
                                if (c < 32) {
                                    sb.append('^');
                                    sb.append((char) (c + 'A' - 1));
                                } else {
                                    sb.append((char) c);
                                }
                            });
                            sb.append("\" ");
                            if (entry.getValue() instanceof Macro) {
                                sb.append("\"");
                                ((Macro) entry.getValue()).getSequence().chars().forEachOrdered(c -> {
                                    if (c < 32) {
                                        sb.append('^');
                                        sb.append((char) (c + 'A' - 1));
                                    } else {
                                        sb.append((char) c);
                                    }
                                });
                                sb.append("\"");
                            } else if (entry.getValue() instanceof Reference) {
                                sb.append(((Reference) entry.getValue()).name().toLowerCase().replace('_', '-'));
                            } else {
                                sb.append(entry.getValue().toString());
                            }
                            sb.append("\n");
                        }
                        terminal.writer().print(sb.toString());
                        terminal.flush();
                    } else if (pl.words().size() == 3) {
                        reader.getKeys().bind(
                                new Reference(pl.words().get(2)), KeyMap.translate(pl.words().get(1))
                        );
                    }
                }
                else if ("cls".equals(pl.word())) {
                    terminal.puts(Capability.clear_screen);
                    terminal.flush();
                }
                else if ("sleep".equals(pl.word())) {
                    Thread.sleep(3000);
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
