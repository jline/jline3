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
import java.util.LinkedList;
import java.util.List;

import org.jline.Completer;
import org.jline.Console;
import org.jline.JLine;
import org.jline.JLine.ConsoleBuilder;
import org.jline.reader.CandidateListCompletionHandler;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.reader.completer.AnsiStringsCompleter;
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

            List<Completer> completers = new LinkedList<>();

            int index = 0;
            while (args.length > index) {
                if (args[index].equals("-posix")) {
                    builder.posix(false);
                    index++;
                }
                else if (args[index].equals("+posix")) {
                    builder.posix(true);
                    index++;
                }
                else if (args[index].equals("-system")) {
                    builder.system(false);
                    index++;
                }
                else if (args[index].equals("+system")) {
                    builder.system(true);
                    index++;
                }
                else if (args[index].equals("-native-pty")) {
                    builder.nativePty(false);
                    index++;
                }
                else if (args[index].equals("+native-pty")) {
                    builder.nativePty(true);
                    index++;
                }
                else if (args[index].equals("none")) {
                    break;
                } else if (args[index].equals("files")) {
                    completers.add(new FileNameCompleter());
                    break;
                }
                else if (args[index].equals("simple")) {
                    completers.add(new StringsCompleter("foo", "bar", "baz"));
                    break;
                }
                else if (args[index].equals("foo")) {
                    completers.add(new ArgumentCompleter(
                            new StringsCompleter("foo11", "foo12", "foo13"),
                            new StringsCompleter("foo21", "foo22", "foo23")));
                    break;
                }
                else if (args[index].equals("color")) {
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
                    completers.add(new AnsiStringsCompleter("\u001B[1mfoo\u001B[0m", "bar", "\u001B[32mbaz\u001B[0m"));
                    CandidateListCompletionHandler handler = new CandidateListCompletionHandler();
                    handler.setStripAnsi(true);
                    builder.completionHandler(handler);
                    break;
                }
                else {
                    usage();

                    return;
                }
            }

            if (args.length == index + 2) {
                mask = args[index+1].charAt(0);
                trigger = args[index];
            }

            builder.completers(completers);

            Console console = builder.build();

            while (true) {
                String line = null;
                try {
                    line = console.readLine(prompt, rightPrompt, null, null);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return;
                }
                if (line == null) {
                    continue;
                }

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
                if (line.equalsIgnoreCase("cls")) {
                    console.puts(Capability.clear_screen);
                    console.flush();
                }
                if (line.equalsIgnoreCase("sleep")) {
                    Thread.sleep(3000);
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
