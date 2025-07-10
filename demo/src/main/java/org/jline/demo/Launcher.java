/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;

import org.jline.builtins.SwingTerminal;
import org.jline.builtins.WebTerminal;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Wrapper class that runs demos and examples with output redirected to WebTerminal or SwingTerminal.
 * This allows any JLine demo or example to be displayed in a web browser or Swing GUI
 * while still using the standard Terminal interface.
 */
public class Launcher {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: Launcher [--terminal=<web|swing|system>] --demo=<MainClass> [demo-args...]");
            System.err.println("Examples:");
            System.err.println("  Launcher --demo=org.jline.demo.Repl");
            System.err.println("  Launcher --terminal=web --demo=org.jline.demo.Repl");
            System.err.println("  Launcher --terminal=swing --demo=org.apache.felix.gogo.jline.Main");
            System.err.println("");
            System.err.println("This wrapper runs the demo with output displayed in WebTerminal or SwingTerminal.");
            System.err.println("The demo will be visible in the web browser or Swing window.");
            System.exit(1);
        }

        String terminalType = "system";
        String demoClass = null;
        List<String> demoArgs = new ArrayList<>();

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--terminal=")) {
                terminalType = arg.substring("--terminal=".length());
            } else if (arg.startsWith("--demo=")) {
                demoClass = arg.substring("--demo=".length());
            } else {
                // Collect remaining arguments for the demo
                demoArgs.addAll(Arrays.asList(Arrays.copyOfRange(args, i, args.length)));
                break;
            }
        }

        if (demoClass == null) {
            System.err.println("Error: Both --terminal and --demo parameters are required");
            System.exit(1);
        }

        try {
            switch (terminalType.toLowerCase()) {
                case "web":
                    runDemoInWebTerminal(demoClass, demoArgs.toArray(new String[0]));
                    break;
                case "swing":
                    runDemoInSwingTerminal(demoClass, demoArgs.toArray(new String[0]));
                    break;
                case "system":
                    runDemo(demoClass, demoArgs.toArray(new String[0]), "system", null);
                    break;
                default:
                    System.err.println("Error: Unknown terminal type: " + terminalType);
                    System.err.println("Supported types: web, swing");
                    System.exit(1);
                    return;
            }
        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runDemoInWebTerminal(String demoClass, String[] args) throws Exception {
        System.out.println("Starting WebTerminal for demo: " + demoClass);

        // Create WebTerminal
        WebTerminal webTerminal = new WebTerminal("localhost", 8080, 80, 24);
        webTerminal.start();
        System.out.println("WebTerminal started at http://localhost:8080");

        // Create piped streams to redirect demo output to WebTerminal
        PipedOutputStream outputPipe = new PipedOutputStream();
        PipedInputStream inputPipe = new PipedInputStream(outputPipe);

        // Create a terminal that uses the piped streams
        Terminal terminal = TerminalBuilder.builder()
                .name("WebTerminal-Demo")
                .type("screen-256color")
                .streams(System.in, outputPipe)
                .build();

        // Start a thread to read from the pipe and write to WebTerminal
        Thread outputThread = new Thread(
                () -> {
                    try {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputPipe.read(buffer)) != -1) {
                            String output = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                            webTerminal.write(output);
                        }
                    } catch (IOException e) {
                        // Pipe closed, normal termination
                    }
                },
                "WebTerminal-Output");
        outputThread.setDaemon(true);
        outputThread.start();

        try {
            // Run the demo
            runDemo(demoClass, args, "web", terminal);
        } finally {
            terminal.close();
            webTerminal.stop();
        }
    }

    private static void runDemoInSwingTerminal(String demoClass, String[] args) throws Exception {
        System.out.println("Starting SwingTerminal for demo: " + demoClass);

        // Create SwingTerminal - it's now a proper Terminal implementation
        String title = "JLine Demo - " + demoClass.substring(demoClass.lastIndexOf('.') + 1);
        SwingTerminal swingTerminal = new SwingTerminal("SwingTerminal-Demo", 80, 24);
        JFrame frame = swingTerminal.createFrame(title);

        // Handle window closing
        final boolean[] closed = {false};
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closed[0] = true;
                frame.dispose();
            }
        });

        System.out.println("SwingTerminal window opened: " + title);

        // Start a thread to read from SwingTerminal and process input
        Thread inputThread = new Thread(
                () -> {
                    try {
                        while (!closed[0]) {
                            String input = swingTerminal.takeInput();
                            if (input != null && !closed[0]) {
                                swingTerminal.processInputBytes(input.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (IOException e) {
                        // Terminal closed, normal termination
                    }
                },
                "SwingTerminal-Input");
        inputThread.setDaemon(true);
        inputThread.start();

        try {
            // Run the demo directly with the SwingTerminal
            runDemo(demoClass, args, "swing", swingTerminal);
        } finally {
            closed[0] = true;
            swingTerminal.close();
            if (frame.isDisplayable()) {
                frame.dispose();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void runDemo(String demoClassName, String[] args, String terminalType, Terminal terminal)
            throws Exception {
        // Load the demo class
        Class<?> demoClass;
        try {
            demoClass = Class.forName(demoClassName);
        } catch (ClassNotFoundException e) {
            System.err.println("Demo class not found: " + demoClassName);
            throw e;
        }

        // Find the main method
        Method mainMethod;
        try {
            mainMethod = demoClass.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            System.err.println("Main method not found in class: " + demoClassName);
            throw e;
        }

        System.out.println("Starting demo: " + demoClassName + " in " + terminalType + " terminal");
        if (args.length > 0) {
            System.out.println("Demo arguments: " + Arrays.toString(args));
        }

        // Invoke the main method
        TerminalBuilder.setTerminalOverride(terminal);
        try {
            mainMethod.invoke(null, (Object) args);
        } finally {
            TerminalBuilder.setTerminalOverride(null);
        }

        System.out.println("Demo finished.");
    }
}
