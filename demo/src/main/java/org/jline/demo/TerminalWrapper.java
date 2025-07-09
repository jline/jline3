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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;

import org.jline.builtins.SwingTerminal;
import org.jline.builtins.WebTerminal;

/**
 * Wrapper class that demonstrates WebTerminal and SwingTerminal usage.
 * This class shows how to create and use both terminal implementations
 * for displaying terminal content in web browsers or Swing applications.
 */
public class TerminalWrapper {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: TerminalWrapper --terminal=<web|swing> --demo=<MainClass> [demo-args...]");
            System.err.println("Examples:");
            System.err.println("  TerminalWrapper --terminal=web --demo=org.jline.demo.Repl");
            System.err.println("  TerminalWrapper --terminal=swing --demo=org.apache.felix.gogo.jline.Main");
            System.err.println("");
            System.err.println("Note: This wrapper demonstrates WebTerminal and SwingTerminal components.");
            System.err.println("      The actual demo will run in the current terminal, while the");
            System.err.println("      web/swing terminal shows a demonstration of the terminal component.");
            System.exit(1);
        }

        String terminalType = null;
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

        if (terminalType == null || demoClass == null) {
            System.err.println("Error: Both --terminal and --demo parameters are required");
            System.exit(1);
        }

        // Start the terminal demonstration
        switch (terminalType.toLowerCase()) {
            case "web":
                startWebTerminalDemo();
                break;
            case "swing":
                startSwingTerminalDemo();
                break;
            default:
                System.err.println("Error: Unknown terminal type: " + terminalType);
                System.err.println("Supported types: web, swing");
                System.exit(1);
                return;
        }

        // Run the actual demo in the current terminal
        System.out.println("Running demo: " + demoClass);
        runDemo(demoClass, demoArgs.toArray(new String[0]));
    }

    private static void startWebTerminalDemo() throws IOException {
        System.out.println("Starting WebTerminal demonstration...");
        WebTerminal webTerminal = new WebTerminal("localhost", 8080, 80, 24);

        // Add some demonstration content
        webTerminal.write("=== JLine WebTerminal Demonstration ===\n");
        webTerminal.write("This is a web-based terminal component.\n");
        webTerminal.write("Visit http://localhost:8080 to see it in action.\n");
        webTerminal.write("\n");
        webTerminal.write("Features:\n");
        webTerminal.write("- ANSI color support\n");
        webTerminal.write("- Real-time updates via AJAX\n");
        webTerminal.write("- Keyboard input handling\n");
        webTerminal.write("- HTML/CSS rendering\n");
        webTerminal.write("\n");
        webTerminal.write("The actual demo is running in your current terminal.\n");
        webTerminal.write("$ ");

        webTerminal.start();
        System.out.println("WebTerminal started at http://localhost:8080");
        System.out.println("The web terminal shows a demonstration while your demo runs here.");
    }

    private static void startSwingTerminalDemo() {
        System.out.println("Starting SwingTerminal demonstration...");
        SwingTerminal swingTerminal = new SwingTerminal(80, 24);

        // Add some demonstration content
        swingTerminal.write("=== JLine SwingTerminal Demonstration ===\n");
        swingTerminal.write("This is a Swing-based terminal component.\n");
        swingTerminal.write("\n");
        swingTerminal.write("Features:\n");
        swingTerminal.write("- Custom painting with Java 2D\n");
        swingTerminal.write("- ANSI color support\n");
        swingTerminal.write("- Font configuration\n");
        swingTerminal.write("- Keyboard and mouse input\n");
        swingTerminal.write("- Cursor blinking\n");
        swingTerminal.write("\n");
        swingTerminal.write("The actual demo is running in your current terminal.\n");
        swingTerminal.write("$ ");

        JFrame frame = swingTerminal.createFrame("JLine SwingTerminal Demo");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("SwingTerminal window closed.");
                frame.dispose();
            }
        });

        System.out.println("SwingTerminal window opened.");
        System.out.println("The Swing terminal shows a demonstration while your demo runs here.");
    }

    private static void runDemo(String demoClassName, String[] args) throws Exception {
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

        System.out.println("Starting demo: " + demoClassName);
        if (args.length > 0) {
            System.out.println("Demo arguments: " + Arrays.toString(args));
        }
        System.out.println("----------------------------------------");

        // Invoke the main method directly
        mainMethod.invoke(null, (Object) args);

        System.out.println("----------------------------------------");
        System.out.println("Demo finished.");
    }
}
