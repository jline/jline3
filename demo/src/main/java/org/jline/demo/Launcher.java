/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

import org.jline.builtins.SwingTerminal;
import org.jline.builtins.WebTerminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Launcher that runs any JLine demo in a system, web, or Swing terminal.
 *
 * <p>Usage: {@code Launcher [--terminal=system|web|swing] <MainClass> [args...]}</p>
 *
 * <p>Both WebTerminal and SwingTerminal implement the full JLine Terminal interface,
 * so any demo that uses TerminalBuilder will automatically use the chosen terminal
 * via {@link TerminalBuilder#setTerminalOverride}.</p>
 */
public class Launcher {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: Launcher [--terminal=system|web|swing] <MainClass> [args...]");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  Launcher org.jline.demo.Repl");
            System.err.println("  Launcher --terminal=web org.jline.demo.Repl");
            System.err.println("  Launcher --terminal=swing org.jline.demo.Repl");
            System.exit(1);
        }

        String terminalType = "system";
        String demoClass = null;
        List<String> demoArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--terminal=")) {
                terminalType = arg.substring("--terminal=".length());
            } else if (demoClass == null) {
                demoClass = arg;
            } else {
                demoArgs.add(arg);
            }
        }

        if (demoClass == null) {
            System.err.println("Error: no demo class specified");
            System.exit(1);
        }

        switch (terminalType.toLowerCase()) {
            case "system":
                runDemo(demoClass, demoArgs.toArray(new String[0]));
                break;
            case "web":
                runInWebTerminal(demoClass, demoArgs.toArray(new String[0]));
                break;
            case "swing":
                runInSwingTerminal(demoClass, demoArgs.toArray(new String[0]));
                break;
            default:
                System.err.println("Unknown terminal type: " + terminalType);
                System.err.println("Supported: system, web, swing");
                System.exit(1);
        }
    }

    @SuppressWarnings("deprecation")
    private static void runInWebTerminal(String demoClass, String[] args) throws Exception {
        WebTerminal terminal = new WebTerminal("localhost", 8080, 80, 24);
        terminal.start();
        System.out.println("WebTerminal started at " + terminal.getUrl());
        System.out.println("Open the URL in your browser to interact with the terminal.");

        try {
            TerminalBuilder.setTerminalOverride(terminal);
            try {
                runDemo(demoClass, args);
            } finally {
                TerminalBuilder.setTerminalOverride(null);
            }
        } finally {
            terminal.stop();
            terminal.close();
        }
    }

    @SuppressWarnings("deprecation")
    private static void runInSwingTerminal(String demoClass, String[] args) throws Exception {
        String title = "JLine - " + demoClass.substring(demoClass.lastIndexOf('.') + 1);
        SwingTerminal terminal = new SwingTerminal(title, 80, 24);
        JFrame frame = terminal.createFrame(title);

        try {
            TerminalBuilder.setTerminalOverride(terminal);
            try {
                runDemo(demoClass, args);
            } finally {
                TerminalBuilder.setTerminalOverride(null);
            }
        } finally {
            terminal.close();
            terminal.dispose();
            frame.dispose();
        }
    }

    private static void runDemo(String demoClassName, String[] args) throws Exception {
        Class<?> demoClass = Class.forName(demoClassName);
        Method mainMethod = demoClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }
}
