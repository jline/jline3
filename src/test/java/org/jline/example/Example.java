/*
 * Copyright (c) 2002-2015, the original author or authors.
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
import java.util.Map;

import org.jline.Completer;
import org.jline.Console;
import org.jline.ConsoleReader;
import org.jline.JLine;
import org.jline.JLine.ConsoleBuilder;
import org.jline.keymap.Binding;
import org.jline.keymap.KeyMap;
import org.jline.keymap.Macro;
import org.jline.keymap.Reference;
import org.jline.reader.ConsoleReaderImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Operation;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.completer.ArgumentCompleter;
import org.jline.reader.completer.FileNameCompleter;
import org.jline.reader.completer.StringsCompleter;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Color;
import org.jline.utils.InfoCmp.Capability;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;


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
            + "will use the simple compleator with 'su' triggering\n"
            + "the use of '*' as a password mask.");
    }

    public static void main(String[] args) throws IOException {
        try {
            String prompt = "prompt> ";
            String rightPrompt = null;
            Character mask = null;
            String trigger = null;
            boolean color = false;

            ConsoleBuilder builder = JLine.builder();

            if ((args == null) || (args.length == 0)) {
                usage();

                return;
            }

            Completer completer = null;

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
                        completer = new FileNameCompleter();
                        break label;
                    case "simple":
                        completer = new StringsCompleter("foo", "bar", "baz");
                        break label;
                    case "foo":
                        completer = new ArgumentCompleter(
                                new StringsCompleter("foo11", "foo12", "foo13"),
                                new StringsCompleter("foo21", "foo22", "foo23"));
                        break label;
                    case "color":
                        color = true;
                        prompt = Ansi.ansi().bg(Color.GREEN).a("foo").reset()
                                .a("@bar")
                                .fg(Color.GREEN)
                                .a("\nbaz")
                                .reset()
                                .a("> ").toString();
                        rightPrompt = Ansi.ansi().fg(Color.RED)
                                .a(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                                .a("\n")
                                .fgBright(Color.RED)
                                .a(LocalTime.now().format(new DateTimeFormatterBuilder()
                                        .appendValue(HOUR_OF_DAY, 2)
                                        .appendLiteral(':')
                                        .appendValue(MINUTE_OF_HOUR, 2)
                                        .toFormatter()))
                                .reset()
                                .toString();
                        completer = new StringsCompleter("\u001B[1mfoo\u001B[0m", "bar", "\u001B[32mbaz\u001B[0m", "foobar");
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

            builder.completer(completer);

            Console console = builder.build();
            ConsoleReader reader = console.newConsoleReader();

            while (true) {
                String line = null;
                try {
                    line = reader.readLine(prompt, rightPrompt, null, null);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return;
                }
                if (line == null) {
                    continue;
                }

                line = line.trim();
                if (color){
                    console.writer().println("\u001B[33m======>\u001B[0m\"" + line + "\"");

                } else {
                    console.writer().println("======>\"" + line + "\"");
                }
                console.flush();

                // If we input the special word then we will mask
                // the next line.
                if ((trigger != null) && (line.compareTo(trigger) == 0)) {
                    line = console.readLine("password> ", mask);
                }
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                ParsedLine pl = ((ConsoleReaderImpl) reader).getParser().parse(line, 0);
                if ("set".equals(pl.word())) {
                    if (pl.words().size() == 3) {
                        builder.variable(pl.words().get(1), pl.words().get(2));
                    }
                }
                else if ("tput".equals(pl.word())) {
                    if (pl.words().size() == 2) {
                        Capability vcap = Capability.byName(pl.words().get(1));
                        if (vcap != null) {
                            console.puts(vcap);
                        } else {
                            console.writer().println("Unknown capability");
                        }
                    }
                }
                else if ("bindkey".equals(pl.word())) {
                    if (pl.words().size() == 1) {
                        StringBuilder sb = new StringBuilder();
                        Map<String, Binding> bound = ((ConsoleReaderImpl) reader).getKeys().getBoundKeys();
                        for (Map.Entry<String, Binding> entry : bound.entrySet()) {
                            if (entry.getValue() != Operation.SELF_INSERT) {
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
                                } else if (entry.getValue() instanceof Operation) {
                                    sb.append(((Operation) entry.getValue()).name().toLowerCase().replace('_', '-'));
                                } else {
                                    sb.append(entry.getValue().toString());
                                }
                                sb.append("\n");
                            }
                        }
                        console.writer().print(sb.toString());
                        console.flush();
                    } else if (pl.words().size() == 3) {
                        ((ConsoleReaderImpl) reader).getKeys().bind(
                                KeyMap.translate(pl.words().get(1)),
                                new Reference(pl.words().get(2)));
                    }
                }
                else if ("cls".equals(pl.word())) {
                    console.puts(Capability.clear_screen);
                    console.flush();
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
