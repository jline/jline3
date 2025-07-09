/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.*;

import org.jline.builtins.SwingTerminal;
import org.jline.builtins.WebTerminal;

/**
 * Demonstration class showing how to use WebTerminal and SwingTerminal.
 * <p>
 * This class provides examples of creating and using both terminal implementations.
 * It includes methods to start a web terminal server and create a Swing terminal window.
 * </p>
 */
public class TerminalDemo {

    /**
     * Demonstrates the WebTerminal by starting an HTTP server.
     *
     * @throws IOException if the server cannot be started
     */
    public static void demoWebTerminal() throws IOException {
        System.out.println("Starting WebTerminal demo...");

        WebTerminal webTerminal = new WebTerminal("localhost", 8080, 80, 24);

        // Add some initial content
        webTerminal.write("Welcome to JLine WebTerminal!\n");
        webTerminal.write("This is a web-based terminal implementation.\n");
        webTerminal.write("Type commands and see them echoed back.\n");
        webTerminal.write("Press Ctrl+C to exit.\n");
        webTerminal.write("$ ");

        // Start the server
        webTerminal.start();

        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down WebTerminal...");
            webTerminal.stop();
        }));

        System.out.println("WebTerminal is running at: " + webTerminal.getUrl());
        System.out.println("Press Ctrl+C to stop the server.");

        // Simple input processing loop
        new Thread(() -> {
                    try {
                        while (webTerminal.isRunning()) {
                            Thread.sleep(100);

                            // Read any output from the terminal
                            String output = webTerminal.read();
                            if (output != null && !output.isEmpty()) {
                                System.out.print("Terminal output: " + output);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .start();
    }

    /**
     * Demonstrates the SwingTerminal by creating a window.
     */
    public static void demoSwingTerminal() {
        System.out.println("Starting SwingTerminal demo...");

        SwingUtilities.invokeLater(() -> {
            SwingTerminal swingTerminal = new SwingTerminal(80, 24);

            // Add some initial content
            swingTerminal.write("Welcome to JLine SwingTerminal!\n");
            swingTerminal.write("This is a Swing-based terminal implementation.\n");
            swingTerminal.write("Type characters and see them echoed back.\n");
            swingTerminal.write("Use arrow keys, function keys, etc.\n");
            swingTerminal.write("$ ");

            // Create and show the frame
            JFrame frame = swingTerminal.createFrame("JLine Swing Terminal Demo");

            // Set up window closing
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    swingTerminal.dispose();
                    System.exit(0);
                }
            });

            frame.setVisible(true);

            // Start input processing thread
            new Thread(() -> {
                        try {
                            while (true) {
                                String input = swingTerminal.takeInput();
                                if (input != null) {
                                    // Echo the input back
                                    swingTerminal.processInput(input);

                                    // Simple command processing
                                    if ("\r".equals(input)) {
                                        swingTerminal.write("\n$ ");
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    })
                    .start();
        });
    }

    /**
     * Demonstrates both terminals side by side.
     */
    public static void demoBothTerminals() {
        System.out.println("Starting both terminal demos...");

        // Start web terminal
        try {
            demoWebTerminal();
        } catch (IOException e) {
            System.err.println("Failed to start WebTerminal: " + e.getMessage());
        }

        // Start Swing terminal
        demoSwingTerminal();
    }

    /**
     * Creates a more advanced Swing terminal with a menu bar and additional features.
     */
    public static void demoAdvancedSwingTerminal() {
        SwingUtilities.invokeLater(() -> {
            SwingTerminal terminal = new SwingTerminal(80, 24);

            // Create frame with menu
            JFrame frame = new JFrame("Advanced JLine Swing Terminal");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create menu bar
            JMenuBar menuBar = new JMenuBar();

            // File menu
            JMenu fileMenu = new JMenu("File");
            JMenuItem clearItem = new JMenuItem("Clear");
            clearItem.addActionListener(e -> {
                // Clear the terminal
                for (int i = 0; i < terminal.getHeight(); i++) {
                    terminal.write("\n");
                }
                terminal.write("Terminal cleared.\n$ ");
            });
            fileMenu.add(clearItem);

            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.addActionListener(e -> {
                terminal.dispose();
                System.exit(0);
            });
            fileMenu.add(exitItem);
            menuBar.add(fileMenu);

            // View menu
            JMenu viewMenu = new JMenu("View");
            JMenuItem fontItem = new JMenuItem("Change Font...");
            fontItem.addActionListener(e -> {
                Font currentFont = terminal.getComponent().getTerminalFont();
                Font newFont = JFontChooser.showDialog(frame, "Choose Terminal Font", currentFont);
                if (newFont != null) {
                    terminal.getComponent().setTerminalFont(newFont);
                }
            });
            viewMenu.add(fontItem);
            menuBar.add(viewMenu);

            frame.setJMenuBar(menuBar);

            // Add terminal component
            frame.add(terminal.getComponent(), BorderLayout.CENTER);

            // Add status bar
            JLabel statusBar = new JLabel("Ready");
            statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
            frame.add(statusBar, BorderLayout.SOUTH);

            // Initial content
            terminal.write("Advanced JLine Swing Terminal\n");
            terminal.write("Features:\n");
            terminal.write("- Menu bar with options\n");
            terminal.write("- Font selection\n");
            terminal.write("- Status bar\n");
            terminal.write("- Full keyboard support\n");
            terminal.write("$ ");

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Input processing
            new Thread(() -> {
                        try {
                            while (true) {
                                String input = terminal.takeInput();
                                if (input != null) {
                                    terminal.processInput(input);

                                    if ("\r".equals(input)) {
                                        terminal.write("\n$ ");
                                    }

                                    // Update status bar
                                    SwingUtilities.invokeLater(() -> statusBar.setText("Last input: "
                                            + input.replace("\r", "\\r").replace("\n", "\\n")));
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    })
                    .start();
        });
    }

    /**
     * Simple font chooser dialog.
     */
    private static class JFontChooser {
        public static Font showDialog(Component parent, String title, Font initialFont) {
            String[] fontNames =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            String selectedFont = (String) JOptionPane.showInputDialog(
                    parent, "Choose a font:", title, JOptionPane.PLAIN_MESSAGE, null, fontNames, initialFont.getName());

            if (selectedFont != null) {
                String[] sizes = {"8", "9", "10", "11", "12", "14", "16", "18", "20", "24", "28", "32"};
                String selectedSize = (String) JOptionPane.showInputDialog(
                        parent,
                        "Choose font size:",
                        title,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        sizes,
                        String.valueOf(initialFont.getSize()));

                if (selectedSize != null) {
                    try {
                        int size = Integer.parseInt(selectedSize);
                        return new Font(selectedFont, Font.PLAIN, size);
                    } catch (NumberFormatException e) {
                        // Use default size
                        return new Font(selectedFont, Font.PLAIN, initialFont.getSize());
                    }
                }
            }

            return null;
        }
    }

    /**
     * Main method to run the demos.
     *
     * @param args command line arguments
     *             - "web" for WebTerminal demo only
     *             - "swing" for SwingTerminal demo only
     *             - "advanced" for advanced SwingTerminal demo
     *             - "both" or no args for both demos
     */
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "both";

        switch (mode) {
            case "web":
                try {
                    demoWebTerminal();
                } catch (IOException e) {
                    System.err.println("Failed to start WebTerminal: " + e.getMessage());
                    System.exit(1);
                }
                break;
            case "swing":
                demoSwingTerminal();
                break;
            case "advanced":
                demoAdvancedSwingTerminal();
                break;
            case "both":
            default:
                demoBothTerminals();
                break;
        }

        // Keep the main thread alive for web terminal
        if ("web".equals(mode) || "both".equals(mode)) {
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
