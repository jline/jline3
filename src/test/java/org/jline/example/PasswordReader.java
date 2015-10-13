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

import org.jline.console.Console;
import org.jline.console.ConsoleBuilder;
import org.jline.reader.ConsoleReader;
import org.jline.reader.ConsoleReaderBuilder;

public class PasswordReader
{
    public static void usage() {
        System.out.println("Usage: java "
            + PasswordReader.class.getName() + " [mask]");
    }

    public static void main(String[] args) throws IOException {
        Console console = ConsoleBuilder.console();
        ConsoleReader reader = ConsoleReaderBuilder.builder()
                .console(console).build();

        Character mask = (args.length == 0)
            ? new Character((char) 0)
            : new Character(args[0].charAt(0));

        String line;
        do {
            line = reader.readLine("Enter password> ", mask);
            System.out.println("Got password: " + line);
        }
        while (line != null && line.length() > 0);
    }
}
