/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DoubleSizeCharacters;
import org.jline.terminal.impl.SixelGraphics;
import org.jline.terminal.impl.TerminalGraphics;
import org.jline.terminal.impl.TerminalGraphicsManager;

/**
 * Example demonstrating how to use terminal graphics in JLine.
 *
 * <p>This example shows how to display images in the terminal using various graphics
 * protocols. JLine supports multiple terminal graphics protocols and automatically
 * selects the best available option for your terminal.</p>
 *
 * <p>Supported graphics protocols:</p>
 * <ul>
 *   <li><strong>Kitty Graphics Protocol</strong> - Modern, feature-rich protocol (Kitty, WezTerm)</li>
 *   <li><strong>iTerm2 Inline Images</strong> - iTerm2's proprietary protocol (iTerm2)</li>
 *   <li><strong>Sixel</strong> - Widely supported legacy protocol (xterm, iTerm2, foot, WezTerm, Konsole, VS Code)</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>
 * # Using mvx (use '--' to separate mvx flags from application arguments):
 * ./mvx demo TerminalGraphicsExample
 * ./mvx demo TerminalGraphicsExample -- --test-image
 * ./mvx demo TerminalGraphicsExample -- --double-size
 * ./mvx demo TerminalGraphicsExample -- --banner "Hello World"
 * ./mvx demo TerminalGraphicsExample -- --protocol kitty
 * ./mvx demo TerminalGraphicsExample -- --list-protocols
 *
 * # Or using java directly:
 * java org.jline.demo.examples.TerminalGraphicsExample
 * java org.jline.demo.examples.TerminalGraphicsExample --force-enable
 * java org.jline.demo.examples.TerminalGraphicsExample --test-image
 * java org.jline.demo.examples.TerminalGraphicsExample --double-size
 * java org.jline.demo.examples.TerminalGraphicsExample --banner "Hello World"
 * java org.jline.demo.examples.TerminalGraphicsExample --protocol kitty
 * java org.jline.demo.examples.TerminalGraphicsExample --protocol iterm2
 * java org.jline.demo.examples.TerminalGraphicsExample --protocol sixel
 * java org.jline.demo.examples.TerminalGraphicsExample --list-protocols
 * </pre>
 */
public class TerminalGraphicsExample {

    // SNIPPET_START: TerminalGraphicsExample
    /**
     * Force enable or disable Sixel support, overriding automatic detection.
     * This is useful for testing or when automatic detection fails.
     *
     * @param enabled true to force enable, false to force disable, null for automatic detection
     */
    public static void forceSixelSupport(Boolean enabled) {
        SixelGraphics.setSixelSupportOverride(enabled);
    }

    public static void displayImageWithSixel(Terminal terminal, String imagePath) throws IOException {
        // Check if the terminal supports Sixel graphics
        if (!SixelGraphics.isSixelSupported(terminal)) {
            terminal.writer().println("Terminal does not support Sixel graphics");
            terminal.writer().println("Use forceSixelSupport(true) to override detection if needed");
            return;
        }

        // Load and display the image
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            new SixelGraphics().displayImage(terminal, imageFile);
        } else {
            terminal.writer().println("Image file not found: " + imagePath);
        }
    }

    public static void displayResourceImageWithSixel(Terminal terminal, String resourcePath) throws IOException {
        // Check if the terminal supports Sixel graphics
        if (!SixelGraphics.isSixelSupported(terminal)) {
            terminal.writer().println("Terminal does not support Sixel graphics");
            return;
        }

        // Load and display the image from resources
        try (InputStream is = TerminalGraphicsExample.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                new SixelGraphics().displayImage(terminal, is);
            } else {
                terminal.writer().println("Resource not found: " + resourcePath);
            }
        }
    }

    public static void displayBufferedImageWithSixel(Terminal terminal, BufferedImage image) throws IOException {
        // Check if the terminal supports Sixel graphics
        if (!SixelGraphics.isSixelSupported(terminal)) {
            terminal.writer().println("Terminal does not support Sixel graphics");
            return;
        }

        // Display the BufferedImage
        new SixelGraphics().displayImage(terminal, image);
    }

    /**
     * Creates a simple test image with text and a gradient background.
     * This is useful when no external images are available.
     *
     * @return a BufferedImage containing a test pattern
     */
    public static BufferedImage createTestImage() {
        int width = 400; // Made wider
        int height = 150;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Create a gradient background
        for (int y = 0; y < height; y++) {
            Color color = new Color(
                    Math.max(0, Math.min(255, (int) (255 * y / (double) height))),
                    Math.max(0, Math.min(255, (int) (255 * (1 - y / (double) height)))),
                    Math.max(0, Math.min(255, (int) (128 + 127 * Math.sin(y * Math.PI / height)))));
            g2d.setColor(color);
            g2d.drawLine(0, y, width, y);
        }

        // Draw text with proper horizontal and vertical centering
        g2d.setColor(Color.WHITE);

        // Main title
        Font titleFont = new Font("SansSerif", Font.BOLD, 24);
        g2d.setFont(titleFont);
        String titleText = "JLine Graphics Test";
        FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
        int titleX = (width - titleMetrics.stringWidth(titleText)) / 2;

        // Subtitle
        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 16);
        g2d.setFont(subtitleFont);
        String subtitleText = "Terminal Graphics Support";
        FontMetrics subtitleMetrics = g2d.getFontMetrics(subtitleFont);
        int subtitleX = (width - subtitleMetrics.stringWidth(subtitleText)) / 2;

        // Calculate vertical centering for both texts together
        int totalTextHeight = titleMetrics.getHeight() + subtitleMetrics.getHeight() + 10; // 10px gap between texts
        int startY = (height - totalTextHeight) / 2;

        // Position title and subtitle with proper vertical centering
        int titleY = startY + titleMetrics.getAscent();
        int subtitleY = titleY + titleMetrics.getDescent() + 10 + subtitleMetrics.getAscent();

        // Draw the text
        g2d.setFont(titleFont);
        g2d.drawString(titleText, titleX, titleY);
        g2d.setFont(subtitleFont);
        g2d.drawString(subtitleText, subtitleX, subtitleY);

        // Draw a border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(10, 10, width - 20, height - 20);

        g2d.dispose();
        return image;
    }
    // SNIPPET_END: TerminalGraphicsExample

    /**
     * Demonstrates how to force enable or disable Sixel support.
     *
     * @param terminal the terminal to use
     * @param enable true to force enable, false to force disable
     */
    public static void demonstrateSixelOverride(Terminal terminal, boolean enable) {
        // Save the current detection state
        boolean originalSupport = SixelGraphics.isSixelSupported(terminal);

        terminal.writer().println("Original sixel support: " + originalSupport);

        // Override the detection
        SixelGraphics.setSixelSupportOverride(enable);
        terminal.writer().println("Sixel support after override: " + SixelGraphics.isSixelSupported(terminal));

        // Reset to automatic detection
        SixelGraphics.setSixelSupportOverride(null);
        terminal.writer().println("Sixel support after reset: " + SixelGraphics.isSixelSupported(terminal));
    }

    /**
     * Demonstrates double-size character functionality.
     *
     * @param terminal the terminal to use
     */
    public static void demonstrateDoubleSizeCharacters(Terminal terminal) {
        try {
            if (DoubleSizeCharacters.isDoubleSizeSupported(terminal)) {
                terminal.writer().println("Terminal supports double-size characters");
                terminal.writer().println();

                // Demonstrate different modes
                DoubleSizeCharacters.printNormal(terminal, "Normal text");
                DoubleSizeCharacters.printDoubleWidth(terminal, "Double width text");
                DoubleSizeCharacters.printDoubleHeight(terminal, "Double height text");

                terminal.writer().println();

                // Create a banner
                DoubleSizeCharacters.printBanner(terminal, "JLine Graphics Demo", '*');

                // Reset to normal
                DoubleSizeCharacters.reset(terminal);
                terminal.writer().println("Back to normal text");
            } else {
                terminal.writer().println("Terminal does not support double-size characters");
            }
        } catch (IOException e) {
            terminal.writer().println("Error demonstrating double-size characters: " + e.getMessage());
        }
    }

    /**
     * Displays an image using the best available graphics protocol.
     * This method automatically detects which graphics protocols are supported
     * and uses the highest priority one.
     *
     * @param terminal the terminal to display the image on
     * @param image the image to display
     * @throws IOException if an I/O error occurs
     */
    public static void displayImageWithBestProtocol(Terminal terminal, BufferedImage image) throws IOException {
        Optional<TerminalGraphics> protocol = TerminalGraphicsManager.getBestProtocol(terminal);
        if (protocol.isPresent()) {
            terminal.writer().println("Using " + protocol.get().getProtocol().getName() + " graphics protocol");
            protocol.get().displayImage(terminal, image);
        } else {
            terminal.writer().println("No graphics protocol supported by this terminal");
            listSupportedTerminals(terminal);
        }
    }

    /**
     * Lists all available graphics protocols and their support status.
     *
     * @param terminal the terminal to check
     */
    public static void listProtocolSupport(Terminal terminal) {
        terminal.writer().println("Graphics Protocol Support:");
        terminal.writer().println("=========================");

        List<TerminalGraphics> allProtocols = TerminalGraphicsManager.getAvailableProtocols();
        List<TerminalGraphics> supportedProtocols = TerminalGraphicsManager.getSupportedProtocols(terminal);

        for (TerminalGraphics protocol : allProtocols) {
            boolean supported = supportedProtocols.contains(protocol);
            String status = supported ? "✓ SUPPORTED" : "✗ Not supported";
            terminal.writer()
                    .printf(
                            "  %-15s (Priority: %2d) - %s%n",
                            protocol.getProtocol().getName(), protocol.getPriority(), status);
        }

        terminal.writer().println();
        Optional<TerminalGraphics> best = TerminalGraphicsManager.getBestProtocol(terminal);
        if (best.isPresent()) {
            terminal.writer()
                    .println("Best protocol for this terminal: "
                            + best.get().getProtocol().getName());
        } else {
            terminal.writer().println("No graphics protocols supported by this terminal");
        }
    }

    /**
     * Forces the use of a specific graphics protocol.
     *
     * @param protocolName the name of the protocol to force ("kitty", "iterm2", "sixel")
     * @param terminal the terminal to use
     * @param image the image to display
     * @throws IOException if an I/O error occurs
     */
    public static void displayImageWithProtocol(String protocolName, Terminal terminal, BufferedImage image)
            throws IOException {
        TerminalGraphics.Protocol protocol;
        try {
            protocol = TerminalGraphics.Protocol.valueOf(protocolName.toUpperCase());
        } catch (IllegalArgumentException e) {
            terminal.writer().println("Unknown protocol: " + protocolName);
            terminal.writer().println("Available protocols: kitty, iterm2, sixel");
            return;
        }

        TerminalGraphicsManager.forceProtocol(protocol);
        try {
            Optional<TerminalGraphics> graphics = TerminalGraphicsManager.getBestProtocol(terminal);
            if (graphics.isPresent()) {
                terminal.writer().println("Forced protocol: " + protocol.getName());
                graphics.get().displayImage(terminal, image);
            } else {
                terminal.writer().println("Protocol " + protocol.getName() + " is not available or supported");
            }
        } finally {
            // Reset to automatic detection
            TerminalGraphicsManager.forceProtocol(null);
        }
    }

    /**
     * Lists terminals that support graphics protocols.
     *
     * @param terminal the terminal to write to
     */
    public static void listSupportedTerminals(Terminal terminal) {
        terminal.writer().println("\nTerminals with graphics support:");
        terminal.writer().println("Kitty Graphics Protocol:");
        terminal.writer().println("  - Kitty terminal");
        terminal.writer().println("  - WezTerm");
        terminal.writer().println("  - Ghostty");
        terminal.writer().println();
        terminal.writer().println("iTerm2 Inline Images:");
        terminal.writer().println("  - iTerm2");
        terminal.writer().println();
        terminal.writer().println("Sixel Graphics:");
        terminal.writer().println("  - XTerm (with --enable-sixel-graphics)");
        terminal.writer().println("  - iTerm2 (>= 3.3.0)");
        terminal.writer().println("  - foot");
        terminal.writer().println("  - WezTerm");
        terminal.writer().println("  - Konsole (>= 22.04)");
        terminal.writer().println("  - VS Code (with terminal.integrated.enableImages: true)");
        terminal.writer().println("  - MLTerm");
        terminal.writer().println("  - Mintty (>= 2.6.0)");
    }

    /**
     * Main method to demonstrate terminal graphics and double-size characters.
     */
    public static void main(String[] args) {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            // Check for command line arguments
            if (args.length > 0) {
                if (args[0].equals("--force-enable")) {
                    // Force enable sixel support for testing
                    SixelGraphics.setSixelSupportOverride(true);
                    terminal.writer().println("Forced sixel support enabled");
                } else if (args[0].equals("--force-disable")) {
                    // Force disable sixel support for testing
                    SixelGraphics.setSixelSupportOverride(false);
                    terminal.writer().println("Forced sixel support disabled");
                } else if (args[0].equals("--demo-override")) {
                    // Demonstrate the override feature
                    demonstrateSixelOverride(terminal, true);
                    return;
                } else if (args[0].equals("--test-image")) {
                    // Use the test image directly with best available protocol
                    terminal.writer().println("Displaying test image with best available protocol");
                    displayImageWithBestProtocol(terminal, createTestImage());
                    return;
                } else if (args[0].equals("--double-size")) {
                    // Demonstrate double-size characters
                    demonstrateDoubleSizeCharacters(terminal);
                    return;
                } else if (args[0].equals("--banner")) {
                    // Create a banner with double-size characters
                    String bannerText = args.length > 1 ? args[1] : "JLine 3";
                    DoubleSizeCharacters.printBanner(terminal, bannerText, '=');
                    return;
                } else if (args[0].equals("--list-protocols")) {
                    // List all available protocols and their support status
                    listProtocolSupport(terminal);
                    return;
                } else if (args[0].equals("--protocol")) {
                    // Force use of a specific protocol
                    if (args.length < 2) {
                        terminal.writer().println("Usage: --protocol <protocol_name>");
                        terminal.writer().println("Available protocols: kitty, iterm2, sixel");
                        return;
                    }
                    terminal.writer().println("Testing " + args[1] + " protocol with test image");
                    displayImageWithProtocol(args[1], terminal, createTestImage());
                    return;
                }
            }

            // Check if any graphics protocol is supported
            TerminalGraphics bestProtocol =
                    TerminalGraphicsManager.getBestProtocol(terminal).orElse(null);
            if (bestProtocol != null) {
                terminal.writer()
                        .println("Terminal supports graphics using: "
                                + bestProtocol.getProtocol().getName());

                // If an image path is provided as an argument, display it
                if (args.length > 0 && !args[0].startsWith("--")) {
                    try {
                        BufferedImage image = ImageIO.read(new File(args[0]));
                        if (image != null) {
                            terminal.writer()
                                    .println("Using "
                                            + bestProtocol.getProtocol().getName() + " graphics protocol");
                            bestProtocol.displayImage(terminal, image);
                        } else {
                            terminal.writer().println("Unable to read image file: " + args[0]);
                        }
                    } catch (IOException e) {
                        terminal.writer().println("Error reading image file: " + e.getMessage());
                    }
                } else {
                    // Otherwise, try to display a sample image from resources
                    try {
                        InputStream is = TerminalGraphicsExample.class.getResourceAsStream("/images/jline-logo.png");
                        if (is != null) {
                            BufferedImage image = ImageIO.read(is);
                            if (image != null) {
                                terminal.writer().println("Displaying JLine logo from resources");
                                terminal.writer()
                                        .println("Using "
                                                + bestProtocol.getProtocol().getName() + " graphics protocol");
                                bestProtocol.displayImage(terminal, image);
                            } else {
                                throw new IOException("Unable to read resource image");
                            }
                        } else {
                            throw new IOException("Resource image not found");
                        }
                    } catch (IOException e) {
                        // If resource image fails, use a programmatically generated test image
                        terminal.writer().println("Resource image not found, using test image instead");
                        terminal.writer()
                                .println("Using " + bestProtocol.getProtocol().getName() + " graphics protocol");
                        bestProtocol.displayImage(terminal, createTestImage());
                    }
                }
            } else {
                terminal.writer().println("Terminal does not support any graphics protocols");
                listSupportedTerminals(terminal);
                terminal.writer().println("\nCommand line options:");
                terminal.writer().println("  --force-enable     Override detection and force enable sixel support");
                terminal.writer().println("  --force-disable    Override detection and force disable sixel support");
                terminal.writer().println("  --demo-override    Demonstrate the override feature");
                terminal.writer().println("  --test-image       Display a programmatically generated test image");
                terminal.writer().println("  --double-size      Demonstrate double-size character functionality");
                terminal.writer().println("  --banner [text]    Create a banner with double-size characters");
                terminal.writer()
                        .println("  --list-protocols   Show all available graphics protocols and their support");
                terminal.writer()
                        .println("  --protocol <name>  Force use of a specific protocol (kitty, iterm2, sixel)");
                terminal.writer().println("  <image-path>       Display the specified image file");
            }

            terminal.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
