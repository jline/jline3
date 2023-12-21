/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.PrintStream;

public class InstallUninstallTest {

    public static void main(String[] args) {
        print(System.out, "Lorem ipsum");
        print(System.err, "dolor sit amet");
        AnsiConsole.systemInstall();
        print(System.out, "consectetur adipiscing elit");
        print(System.err, "sed do eiusmod");
        AnsiConsole.out().setMode(AnsiMode.Strip);
        AnsiConsole.err().setMode(AnsiMode.Strip);
        print(System.out, "tempor incididunt ut");
        print(System.err, "labore et dolore");
        AnsiConsole.systemUninstall();
        print(System.out, "magna aliqua.");
        print(System.err, "Ut enim ad ");
    }

    private static void print(PrintStream stream, String text) {
        int half = text.length() / 2;
        stream.print(text.substring(0, half));
        stream.println(Ansi.ansi().fg(Ansi.Color.GREEN).a(text.substring(half)).reset());
    }
}
