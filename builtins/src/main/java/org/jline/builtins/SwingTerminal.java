/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

/**
 * A Swing-based terminal implementation that extends ScreenTerminal.
 * <p>
 * This class provides a JComponent that can be embedded in Swing applications
 * to display a terminal interface. It renders terminal content using Java 2D
 * graphics and handles keyboard and mouse input.
 * </p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Custom painting for terminal characters and attributes</li>
 *   <li>ANSI color support with configurable color palette</li>
 *   <li>Font configuration with monospace font support</li>
 *   <li>Keyboard input handling with special key support</li>
 *   <li>Mouse support for cursor positioning</li>
 *   <li>Scrollback buffer support</li>
 *   <li>Cursor blinking</li>
 * </ul>
 */
public class SwingTerminal extends ScreenTerminal {

    /**
     * JComponent that renders the terminal display.
     */
    public static class TerminalComponent extends JComponent implements KeyListener {

        private static final long serialVersionUID = 1L;

        private final transient SwingTerminal terminal;
        private Font terminalFont;
        private FontMetrics fontMetrics;
        private int charWidth;
        private int charHeight;
        private int charAscent;

        // Color palette for ANSI colors
        private final Color[] ansiColors = {
            Color.BLACK, // 0: Black
            new Color(128, 0, 0), // 1: Dark Red
            new Color(0, 128, 0), // 2: Dark Green
            new Color(128, 128, 0), // 3: Dark Yellow
            new Color(0, 0, 128), // 4: Dark Blue
            new Color(128, 0, 128), // 5: Dark Magenta
            new Color(0, 128, 128), // 6: Dark Cyan
            new Color(192, 192, 192), // 7: Light Gray
            new Color(128, 128, 128), // 8: Dark Gray
            new Color(255, 0, 0), // 9: Bright Red
            new Color(0, 255, 0), // 10: Bright Green
            new Color(255, 255, 0), // 11: Bright Yellow
            new Color(0, 0, 255), // 12: Bright Blue
            new Color(255, 0, 255), // 13: Bright Magenta
            new Color(0, 255, 255), // 14: Bright Cyan
            Color.WHITE // 15: White
        };

        private final Color defaultForeground = Color.WHITE;
        private final Color defaultBackground = Color.BLACK;

        private final transient BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
        private final AtomicBoolean cursorVisible = new AtomicBoolean(true);
        private transient Timer cursorTimer;

        public TerminalComponent(SwingTerminal terminal) {
            this.terminal = terminal;

            // Set up font directly to avoid this-escape warning
            this.terminalFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);
            // All other initialization will be done in initialize() after construction
        }

        /**
         * Initializes the component after construction to avoid this-escape warnings.
         */
        void initialize() {
            // Initialize font metrics
            initializeFontMetrics();

            // Set up component properties
            setFocusable(true);
            setBackground(defaultBackground);
            setForeground(defaultForeground);

            // Add listeners
            addKeyListener(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                }
            });

            // Set up cursor blinking timer
            cursorTimer = new Timer(500, e -> {
                cursorVisible.set(!cursorVisible.get());
                repaint();
            });
            cursorTimer.start();
        }

        /**
         * Initializes font metrics for the current font.
         */
        void initializeFontMetrics() {
            if (terminalFont != null) {
                this.fontMetrics = getFontMetrics(terminalFont);
                this.charWidth = fontMetrics.charWidth('M'); // Use 'M' for width calculation
                this.charHeight = fontMetrics.getHeight();
                this.charAscent = fontMetrics.getAscent();
                updatePreferredSize();
            }
        }

        /**
         * Sets the font used for terminal display.
         *
         * @param font the font to use (should be monospace)
         */
        public void setTerminalFont(Font font) {
            this.terminalFont = font;
            initializeFontMetrics();
            revalidate();
            repaint();
        }

        /**
         * Gets the current terminal font.
         *
         * @return the current font
         */
        public Font getTerminalFont() {
            return terminalFont;
        }

        private void updatePreferredSize() {
            int width = terminal.getWidth() * charWidth;
            int height = terminal.getHeight() * charHeight;
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            try {
                // Enable antialiasing for better text rendering
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Set font
                g2d.setFont(terminalFont);

                // Paint terminal content
                paintTerminalContent(g2d);

            } finally {
                g2d.dispose();
            }
        }

        private void paintTerminalContent(Graphics2D g2d) {
            int termWidth = terminal.getWidth();
            int termHeight = terminal.getHeight();

            // Get terminal screen data
            long[] screenData = new long[termWidth * termHeight];
            int[] cursor = new int[2];
            terminal.dump(screenData, 0, 0, termHeight, termWidth, cursor);

            // Paint each character
            for (int y = 0; y < termHeight; y++) {
                for (int x = 0; x < termWidth; x++) {
                    int index = y * termWidth + x;
                    if (index < screenData.length) {
                        long cell = screenData[index];
                        paintCell(g2d, x, y, cell, cursor[0] == x && cursor[1] == y);
                    }
                }
            }
        }

        private void paintCell(Graphics2D g2d, int x, int y, long cell, boolean isCursor) {
            // Extract character and attributes from cell
            char ch = (char) (cell & 0xffffffffL);
            long attr = cell >>> 32;

            // Extract colors and attributes
            int fg = (int) ((attr >>> 20) & 0x0f);
            int bg = (int) ((attr >>> 16) & 0x0f);
            boolean bold = (attr & 0x08000000L) != 0;
            boolean underline = (attr & 0x01000000L) != 0;
            boolean inverse = (attr & 0x02000000L) != 0;

            // Handle inverse
            if (inverse) {
                int temp = fg;
                fg = bg;
                bg = temp;
            }

            // Handle cursor
            if (isCursor && cursorVisible.get()) {
                bg = 15; // White background for cursor
                fg = 0; // Black foreground for cursor
            }

            // Calculate position
            int cellX = x * charWidth;
            int cellY = y * charHeight;

            // Paint background
            g2d.setColor(getAnsiColor(bg));
            g2d.fillRect(cellX, cellY, charWidth, charHeight);

            // Paint character if not space
            if (ch != ' ' && ch != 0) {
                g2d.setColor(getAnsiColor(fg));

                // Set font style
                Font font = terminalFont;
                if (bold) {
                    font = font.deriveFont(Font.BOLD);
                }
                g2d.setFont(font);

                // Draw character
                g2d.drawString(String.valueOf(ch), cellX, cellY + charAscent);

                // Draw underline if needed
                if (underline) {
                    int underlineY = cellY + charAscent + 1;
                    g2d.drawLine(cellX, underlineY, cellX + charWidth - 1, underlineY);
                }
            }
        }

        private Color getAnsiColor(int colorIndex) {
            if (colorIndex >= 0 && colorIndex < ansiColors.length) {
                return ansiColors[colorIndex];
            }
            return defaultForeground;
        }

        /**
         * Gets the next input from the input queue.
         *
         * @return the next input string, or null if none available
         */
        public String pollInput() {
            return inputQueue.poll();
        }

        /**
         * Gets the next input from the input queue, blocking if necessary.
         *
         * @return the next input string
         * @throws InterruptedException if interrupted while waiting
         */
        public String takeInput() throws InterruptedException {
            return inputQueue.take();
        }

        // KeyListener implementation
        @Override
        public void keyTyped(KeyEvent e) {
            char ch = e.getKeyChar();
            if (ch != KeyEvent.CHAR_UNDEFINED && !e.isControlDown()) {
                inputQueue.offer(String.valueOf(ch));
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            String input = null;

            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER:
                    input = "\r";
                    break;
                case KeyEvent.VK_BACK_SPACE:
                    input = "\u007f";
                    break;
                case KeyEvent.VK_TAB:
                    input = "\t";
                    break;
                case KeyEvent.VK_UP:
                    input = "~A";
                    break;
                case KeyEvent.VK_DOWN:
                    input = "~B";
                    break;
                case KeyEvent.VK_RIGHT:
                    input = "~C";
                    break;
                case KeyEvent.VK_LEFT:
                    input = "~D";
                    break;
                case KeyEvent.VK_HOME:
                    input = "~H";
                    break;
                case KeyEvent.VK_END:
                    input = "~F";
                    break;
                case KeyEvent.VK_PAGE_UP:
                    input = "~1";
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    input = "~2";
                    break;
                case KeyEvent.VK_INSERT:
                    input = "~3";
                    break;
                case KeyEvent.VK_DELETE:
                    input = "~4";
                    break;
                case KeyEvent.VK_F1:
                    input = "~a";
                    break;
                case KeyEvent.VK_F2:
                    input = "~b";
                    break;
                case KeyEvent.VK_F3:
                    input = "~c";
                    break;
                case KeyEvent.VK_F4:
                    input = "~d";
                    break;
                case KeyEvent.VK_F5:
                    input = "~e";
                    break;
                case KeyEvent.VK_F6:
                    input = "~f";
                    break;
                case KeyEvent.VK_F7:
                    input = "~g";
                    break;
                case KeyEvent.VK_F8:
                    input = "~h";
                    break;
                case KeyEvent.VK_F9:
                    input = "~i";
                    break;
                case KeyEvent.VK_F10:
                    input = "~j";
                    break;
                case KeyEvent.VK_F11:
                    input = "~k";
                    break;
                case KeyEvent.VK_F12:
                    input = "~l";
                    break;
                default:
                    // Handle Ctrl+key combinations
                    if (e.isControlDown() && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                        char ch = e.getKeyChar();
                        if (ch >= 'a' && ch <= 'z') {
                            input = String.valueOf((char) (ch - 'a' + 1));
                        } else if (ch >= 'A' && ch <= 'Z') {
                            input = String.valueOf((char) (ch - 'A' + 1));
                        }
                    }
                    break;
            }

            if (input != null) {
                inputQueue.offer(input);
                e.consume();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Not used
        }

        /**
         * Stops the cursor timer and cleans up resources.
         */
        public void dispose() {
            if (cursorTimer != null) {
                cursorTimer.stop();
            }
        }
    }

    private final TerminalComponent component;

    /**
     * Creates a new SwingTerminal with default size (80x24).
     */
    public SwingTerminal() {
        this(80, 24);
    }

    /**
     * Creates a new SwingTerminal with specified size.
     *
     * @param width terminal width in characters
     * @param height terminal height in characters
     */
    public SwingTerminal(int width, int height) {
        super(width, height);
        this.component = new TerminalComponent(this);
        // Initialize component after creation to avoid this-escape warning
        this.component.initialize();
    }

    /**
     * Gets the Swing component for this terminal.
     *
     * @return the terminal component
     */
    public TerminalComponent getComponent() {
        return component;
    }

    /**
     * Processes input from the terminal component.
     *
     * @param input the input string to process
     */
    public void processInput(String input) {
        if (input != null) {
            String processed = pipe(input);
            write(processed);
            component.repaint();
        }
    }

    /**
     * Gets input from the terminal component (non-blocking).
     *
     * @return the next input string, or null if none available
     */
    public String pollInput() {
        return component.pollInput();
    }

    /**
     * Gets input from the terminal component (blocking).
     *
     * @return the next input string
     * @throws InterruptedException if interrupted while waiting
     */
    public String takeInput() throws InterruptedException {
        return component.takeInput();
    }

    @Override
    protected void setDirty() {
        super.setDirty();
        if (component != null) {
            SwingUtilities.invokeLater(() -> component.repaint());
        }
    }

    /**
     * Creates a JFrame containing this terminal.
     *
     * @param title the frame title
     * @return the created frame
     */
    public JFrame createFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(component);
        frame.pack();
        frame.setLocationRelativeTo(null);
        return frame;
    }

    /**
     * Disposes of resources used by this terminal.
     */
    public void dispose() {
        component.dispose();
    }
}
