/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

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
            Character mask = null;
            String trigger = null;
            boolean color = false;

            ConsoleReader reader = new ConsoleReader();

            reader.setPrompt("prompt> ");

            if ((args == null) || (args.length == 0)) {
                usage();

                return;
            }

            List<Completer> completors = new LinkedList<Completer>();

            if (args.length > 0) {
                if (args[0].equals("none")) {
                }
                else if (args[0].equals("files")) {
                    completors.add(new FileNameCompleter());
                }
                else if (args[0].equals("simple")) {
                    completors.add(new StringsCompleter("foo", "bar", "baz"));
                }
                else if (args[0].equals("color")) {
                    color = true;
                    reader.setPrompt("\u001B[1mfoo\u001B[0m@bar\u001B[32m@baz\u001B[0m> ");
                }
                else {
                    usage();

                    return;
                }
            }

            if (args.length == 3) {
                mask = args[2].charAt(0);
                trigger = args[1];
            }

            for (Completer c : completors) {
                reader.addCompleter(c);
            }

            String line;
            PrintWriter out = new PrintWriter(reader.getOutput());

            while ((line = reader.readLine()) != null) {
                if (color){
                    out.println("\u001B[33m======>\u001B[0m\"" + line + "\"");

                } else {
                    out.println("======>\"" + line + "\"");
                }
                out.flush();

                // If we input the special word then we will mask
                // the next line.
                if ((trigger != null) && (line.compareTo(trigger) == 0)) {
                    line = reader.readLine("password> ", mask);
                }
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
