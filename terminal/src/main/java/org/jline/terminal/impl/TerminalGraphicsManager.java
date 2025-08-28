/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.jline.terminal.Terminal;

/**
 * Manager for terminal graphics protocols.
 *
 * <p>This class provides a unified interface for displaying images in terminals
 * using the best available graphics protocol. It automatically detects which
 * protocols are supported by the terminal and selects the most appropriate one.</p>
 *
 * <p>The manager supports multiple graphics protocols:</p>
 * <ul>
 *   <li><strong>Kitty Graphics Protocol</strong> - Modern, feature-rich protocol</li>
 *   <li><strong>iTerm2 Inline Images</strong> - iTerm2's proprietary protocol</li>
 *   <li><strong>Sixel</strong> - Widely supported legacy protocol</li>
 * </ul>
 *
 * <p>Protocols are selected based on priority and terminal support. Higher priority
 * protocols are preferred when multiple protocols are available.</p>
 *
 * @since 3.30.0
 */
public class TerminalGraphicsManager {

    /**
     * Creates a new TerminalGraphicsManager instance.
     */
    public TerminalGraphicsManager() {
        // Default constructor
    }

    private static final List<TerminalGraphics> AVAILABLE_PROTOCOLS = new ArrayList<>();
    private static TerminalGraphics.Protocol forcedProtocol = null;

    static {
        // Register built-in protocols
        registerProtocol(new KittyGraphics());
        registerProtocol(new ITerm2Graphics());
        registerProtocol(new SixelGraphics());

        // Load additional protocols via ServiceLoader
        ServiceLoader<TerminalGraphics> loader = ServiceLoader.load(TerminalGraphics.class);
        for (TerminalGraphics protocol : loader) {
            registerProtocol(protocol);
        }

        // Sort by priority (highest first)
        AVAILABLE_PROTOCOLS.sort(
                Comparator.comparingInt(TerminalGraphics::getPriority).reversed());
    }

    /**
     * Registers a graphics protocol implementation.
     *
     * @param protocol the protocol implementation to register
     */
    public static void registerProtocol(TerminalGraphics protocol) {
        if (!AVAILABLE_PROTOCOLS.contains(protocol)) {
            AVAILABLE_PROTOCOLS.add(protocol);
        }
    }

    /**
     * Forces the use of a specific graphics protocol, overriding automatic detection.
     * This is useful for testing or when automatic detection fails.
     *
     * @param protocol the protocol to force, or null to enable automatic detection
     */
    public static void forceProtocol(TerminalGraphics.Protocol protocol) {
        forcedProtocol = protocol;
    }

    /**
     * Gets the forced protocol, if any.
     *
     * @return the forced protocol, or null if automatic detection is enabled
     */
    public static TerminalGraphics.Protocol getForcedProtocol() {
        return forcedProtocol;
    }

    /**
     * Finds the best graphics protocol for the given terminal.
     *
     * @param terminal the terminal to check
     * @return the best available graphics protocol, or empty if none are supported
     */
    public static Optional<TerminalGraphics> getBestProtocol(Terminal terminal) {
        // If a protocol is forced, try to find and return it
        if (forcedProtocol != null) {
            return AVAILABLE_PROTOCOLS.stream()
                    .filter(p -> p.getProtocol() == forcedProtocol)
                    .findFirst();
        }

        // Find the highest priority protocol that is supported
        return AVAILABLE_PROTOCOLS.stream()
                .filter(protocol -> protocol.isSupported(terminal))
                .findFirst();
    }

    /**
     * Gets all available graphics protocols.
     *
     * @return a list of all registered graphics protocols
     */
    public static List<TerminalGraphics> getAvailableProtocols() {
        return new ArrayList<>(AVAILABLE_PROTOCOLS);
    }

    /**
     * Gets all graphics protocols supported by the given terminal.
     *
     * @param terminal the terminal to check
     * @return a list of supported graphics protocols, sorted by priority
     */
    public static List<TerminalGraphics> getSupportedProtocols(Terminal terminal) {
        return AVAILABLE_PROTOCOLS.stream()
                .filter(protocol -> protocol.isSupported(terminal))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Checks if any graphics protocol is supported by the given terminal.
     *
     * @param terminal the terminal to check
     * @return true if at least one graphics protocol is supported
     */
    public static boolean isGraphicsSupported(Terminal terminal) {
        return getBestProtocol(terminal).isPresent();
    }

    /**
     * Displays a BufferedImage on the terminal using the best available protocol.
     *
     * @param terminal the terminal to display the image on
     * @param image the image to display
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if no graphics protocol is supported
     */
    public static void displayImage(Terminal terminal, BufferedImage image) throws IOException {
        TerminalGraphics protocol = getBestProtocol(terminal)
                .orElseThrow(
                        () -> new UnsupportedOperationException("No graphics protocol supported by this terminal"));
        protocol.displayImage(terminal, image);
    }

    /**
     * Displays a BufferedImage on the terminal with custom options.
     *
     * @param terminal the terminal to display the image on
     * @param image the image to display
     * @param options display options for the image
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if no graphics protocol is supported
     */
    public static void displayImage(Terminal terminal, BufferedImage image, TerminalGraphics.ImageOptions options)
            throws IOException {
        TerminalGraphics protocol = getBestProtocol(terminal)
                .orElseThrow(
                        () -> new UnsupportedOperationException("No graphics protocol supported by this terminal"));
        protocol.displayImage(terminal, image, options);
    }

    /**
     * Displays an image file on the terminal using the best available protocol.
     *
     * @param terminal the terminal to display the image on
     * @param file the image file to display
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if no graphics protocol is supported
     */
    public static void displayImage(Terminal terminal, File file) throws IOException {
        TerminalGraphics protocol = getBestProtocol(terminal)
                .orElseThrow(
                        () -> new UnsupportedOperationException("No graphics protocol supported by this terminal"));
        protocol.displayImage(terminal, file);
    }

    /**
     * Displays an image file on the terminal with custom options.
     *
     * @param terminal the terminal to display the image on
     * @param file the image file to display
     * @param options display options for the image
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if no graphics protocol is supported
     */
    public static void displayImage(Terminal terminal, File file, TerminalGraphics.ImageOptions options)
            throws IOException {
        TerminalGraphics protocol = getBestProtocol(terminal)
                .orElseThrow(
                        () -> new UnsupportedOperationException("No graphics protocol supported by this terminal"));
        protocol.displayImage(terminal, file, options);
    }

    /**
     * Displays an image from an input stream on the terminal using the best available protocol.
     *
     * @param terminal the terminal to display the image on
     * @param inputStream the input stream containing the image data
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if no graphics protocol is supported
     */
    public static void displayImage(Terminal terminal, InputStream inputStream) throws IOException {
        TerminalGraphics protocol = getBestProtocol(terminal)
                .orElseThrow(
                        () -> new UnsupportedOperationException("No graphics protocol supported by this terminal"));
        protocol.displayImage(terminal, inputStream);
    }

    /**
     * Displays an image from an input stream on the terminal with custom options.
     *
     * @param terminal the terminal to display the image on
     * @param inputStream the input stream containing the image data
     * @param options display options for the image
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if no graphics protocol is supported
     */
    public static void displayImage(Terminal terminal, InputStream inputStream, TerminalGraphics.ImageOptions options)
            throws IOException {
        TerminalGraphics protocol = getBestProtocol(terminal)
                .orElseThrow(
                        () -> new UnsupportedOperationException("No graphics protocol supported by this terminal"));
        protocol.displayImage(terminal, inputStream, options);
    }
}
