/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.FileInputStream;
import java.io.IOException;

import static org.jline.jansi.Ansi.*;

/**
 *
 */
public class AnsiConsoleExample2 {

    private AnsiConsoleExample2() {}

    public static void main(String[] args) throws IOException {
        String file = "src/test/resources/jansi.ans";
        if (args.length > 0) file = args[0];

        // Allows us to disable ANSI processing.
        if ("true".equals(System.getProperty("jansi", "true"))) {
            AnsiConsole.systemInstall();
        }

        System.out.print(ansi().reset().eraseScreen().cursor(1, 1));
        System.out.print("=======================================================================");
        FileInputStream f = new FileInputStream(file);
        int c;
        while ((c = f.read()) >= 0) {
            System.out.write(c);
        }
        f.close();
        System.out.println("=======================================================================");
    }
}
