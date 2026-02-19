/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;

/**
 * A Swing-based terminal implementation that extends LineDisciplineTerminal.
 * <p>
 * This class provides a proper JLine Terminal implementation that can be embedded in Swing applications
 * to display a terminal interface. It renders terminal content using Java 2D graphics and handles
 * keyboard and mouse input with proper terminal capabilities.
 * </p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Full JLine Terminal interface implementation</li>
 *   <li>Custom painting for terminal characters and attributes</li>
 *   <li>ANSI color support with configurable color palette</li>
 *   <li>Font configuration with monospace font support</li>
 *   <li>Keyboard input handling with proper terminal capabilities</li>
 *   <li>Mouse support for cursor positioning</li>
 *   <li>Scrollback buffer support</li>
 *   <li>Cursor blinking</li>
 * </ul>
 */
public class SwingTerminal extends LineDisciplineTerminal {

    private final TerminalComponent component;
    private Thread inputThread;

    /**
     * Creates a new SwingTerminal with the specified dimensions.
     *
     * @param width  the terminal width in columns
     * @param height the terminal height in rows
     * @throws IOException if an I/O error occurs during initialization
     */
    public SwingTerminal(int width, int height) throws IOException {
        this("SwingTerminal", width, height);
    }

    /**
     * Creates a new SwingTerminal with default dimensions (80x24).
     *
     * @throws IOException if an I/O error occurs during initialization
     */
    public SwingTerminal() throws IOException {
        this("SwingTerminal", 80, 24);
    }

    /**
     * Creates a new SwingTerminal with the specified name and dimensions.
     *
     * @param name   the terminal name
     * @param width  the terminal width in columns
     * @param height the terminal height in rows
     * @throws IOException if an I/O error occurs during initialization
     */
    @SuppressWarnings("this-escape")
    public SwingTerminal(String name, int width, int height) throws IOException {
        super(name, "screen-256color", new SwingTerminalOutputStream(), StandardCharsets.UTF_8);

        // Create the terminal component
        this.component = new TerminalComponent(width, height);

        // Initialize after construction to avoid this-escape warnings
        initializeTerminal(width, height);

        // Start a thread to read from SwingTerminal and process input
        inputThread = new Thread(
                () -> {
                    try {
                        while (!closed) {
                            String input = component.takeInput();
                            if (input != null && !closed) {
                                processInputBytes(input.getBytes(StandardCharsets.UTF_8));
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
    }

    /**
     * Initializes the terminal after construction to avoid this-escape issues.
     */
    private void initializeTerminal(int width, int height) {
        // Set initial size
        setSize(new Size(width, height));

        // Connect the component output to our master output and set the terminal reference
        SwingTerminalOutputStream outputStream = (SwingTerminalOutputStream) masterOutput;
        outputStream.setComponent(component);
        outputStream.setTerminal(this);
        component.setTerminal(this);
    }

    /**
     * Gets the Swing component that renders the terminal.
     *
     * @return the terminal component
     */
    public TerminalComponent getComponent() {
        return component;
    }

    /**
     * Creates a JFrame containing the terminal component.
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
        frame.setVisible(true);
        component.requestFocusInWindow();
        return frame;
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

    /**
     * Processes input bytes through the terminal's line discipline.
     *
     * @param input the input bytes to process
     * @throws IOException if an I/O error occurs
     */
    public void processInputBytes(byte[] input) throws IOException {
        super.processInputBytes(input);
    }

    /**
     * Writes text to the terminal component.
     *
     * @param text the text to write
     */
    public void write(String text) {
        component.write(text);
    }

    /**
     * Dumps the terminal screen data.
     *
     * @param screen the screen data array to fill
     * @param x the starting x coordinate
     * @param y the starting y coordinate
     * @param height the height to dump
     * @param width the width to dump
     * @param cursor the cursor position array to fill
     */
    public void dump(long[] screen, int x, int y, int height, int width, int[] cursor) {
        component.dump(screen, x, y, height, width, cursor);
    }

    /**
     * Dumps the terminal screen data with scrollback.
     *
     * @param timeout the number of scrollback lines
     * @param forceUpdate whether to include scrollback
     * @return the screen data
     * @throws InterruptedException if interrupted while waiting
     */
    public String dump(long timeout, boolean forceUpdate) throws InterruptedException {
        return component.dump(timeout, forceUpdate);
    }

    /**
     * Checks if the terminal is dirty (needs repainting).
     *
     * @return true if dirty
     */
    public boolean isDirty() {
        return component.isDirty();
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        dispose();
    }

    /**
     * Disposes of the terminal resources.
     */
    public void dispose() {
        if (inputThread != null) {
            inputThread.interrupt();
        }
        component.dispose();
    }

    /**
     * Custom OutputStream that writes to the TerminalComponent.
     * Uses proper ByteBuffer and CharsetDecoder to handle byte-to-char transformation.
     */
    private static class SwingTerminalOutputStream extends OutputStream {
        private TerminalComponent component;
        private SwingTerminal terminal;
        private CharsetDecoder decoder;
        private ByteBuffer inputBuffer;
        private CharBuffer outputBuffer;
        private final StringBuilder pendingOutput = new StringBuilder();

        public void setComponent(TerminalComponent component) {
            this.component = component;
        }

        public void setTerminal(SwingTerminal terminal) {
            this.terminal = terminal;
            initializeDecoder();
        }

        private void initializeDecoder() {
            if (terminal != null) {
                Charset charset = terminal.outputEncoding();
                this.decoder = charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE);

                // Allocate buffers - input buffer for incomplete byte sequences
                this.inputBuffer = ByteBuffer.allocate(16); // Enough for any multi-byte sequence
                this.outputBuffer = CharBuffer.allocate(16);
            }
        }

        @Override
        public void write(int b) throws IOException {
            if (component == null || terminal == null) {
                return;
            }

            // Ensure decoder is initialized
            if (decoder == null) {
                initializeDecoder();
            }

            // Add byte to input buffer
            if (!inputBuffer.hasRemaining()) {
                // Buffer is full, decode what we have first
                decodeAndOutput(false);
                inputBuffer.clear();
            }

            inputBuffer.put((byte) b);

            // Try to decode the current buffer contents
            inputBuffer.flip();
            outputBuffer.clear();

            CoderResult result = decoder.decode(inputBuffer, outputBuffer, false);

            // Handle the result
            if (result.isUnderflow()) {
                // Either successful decode or need more input
                if (outputBuffer.position() > 0) {
                    // We decoded some characters
                    outputBuffer.flip();
                    pendingOutput.append(outputBuffer);
                }
                // Compact input buffer to preserve any remaining bytes
                inputBuffer.compact();
            } else if (result.isOverflow()) {
                // Output buffer too small (shouldn't happen with our buffer size)
                outputBuffer.flip();
                pendingOutput.append(outputBuffer);
                inputBuffer.compact();
            } else {
                // Error occurred, decoder handled it with replacement chars
                outputBuffer.flip();
                pendingOutput.append(outputBuffer);
                inputBuffer.compact();
            }

            // Output any complete characters immediately
            if (pendingOutput.length() > 0) {
                component.write(pendingOutput.toString());
                pendingOutput.setLength(0);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (component == null || terminal == null) {
                return;
            }

            // Ensure decoder is initialized
            if (decoder == null) {
                initializeDecoder();
            }

            // Check if there are leftover bytes from a previous incomplete sequence
            if (inputBuffer.position() > 0) {
                // Feed bytes one at a time until the pending sequence is complete
                int i = 0;
                while (i < len && inputBuffer.position() > 0) {
                    write(b[off + i] & 0xFF);
                    i++;
                }
                if (i >= len) {
                    return;
                }
                off += i;
                len -= i;
            }

            // Decode the remaining byte array directly
            ByteBuffer input = ByteBuffer.wrap(b, off, len);
            CharBuffer output = CharBuffer.allocate(len);
            CoderResult result = decoder.decode(input, output, false);

            if (output.position() > 0) {
                output.flip();
                component.write(output.toString());
            }

            // If there are remaining bytes (incomplete multi-byte sequence), save them
            if (input.hasRemaining()) {
                inputBuffer.clear();
                inputBuffer.put(input);
            }
        }

        @Override
        public void flush() throws IOException {
            // Flush any remaining bytes in input buffer only if we're closing
            // For normal flush operations, just repaint the component
            if (component != null) {
                SwingUtilities.invokeLater(() -> component.repaint());
            }
        }

        /**
         * Forces decoding of any remaining bytes in the buffer.
         * This should only be called when the stream is being closed.
         */
        public void close() throws IOException {
            if (component != null && terminal != null && inputBuffer != null && inputBuffer.position() > 0) {
                decodeAndOutput(true);
            }
        }

        /**
         * Decode any remaining bytes in the input buffer and output them.
         *
         * @param endOfInput true if this is the end of input (flush)
         */
        private void decodeAndOutput(boolean endOfInput) {
            if (decoder == null || inputBuffer == null) {
                return;
            }

            inputBuffer.flip();
            outputBuffer.clear();

            decoder.decode(inputBuffer, outputBuffer, endOfInput);

            outputBuffer.flip();
            if (outputBuffer.hasRemaining()) {
                pendingOutput.append(outputBuffer);
            }

            if (endOfInput) {
                // Final flush - decode may leave state that flush() will emit
                outputBuffer.clear();
                decoder.flush(outputBuffer);
                outputBuffer.flip();
                if (outputBuffer.hasRemaining()) {
                    pendingOutput.append(outputBuffer);
                }
                // Reset decoder after flush to allow reuse
                decoder.reset();
            }

            if (pendingOutput.length() > 0) {
                component.write(pendingOutput.toString());
                pendingOutput.setLength(0);
            }

            inputBuffer.compact();
        }
    }

    /**
     * JComponent that renders the terminal display.
     * This is the inner class that contains the original ScreenTerminal-based implementation.
     */
    public static class TerminalComponent extends JComponent implements KeyListener {

        private static final long serialVersionUID = 1L;

        private transient SwingTerminal terminal;
        private final transient ScreenTerminal screenTerminal;
        private Font terminalFont;
        private FontMetrics fontMetrics;
        private int charWidth;
        private int charHeight;
        private int charAscent;

        private final Color defaultForeground = Color.WHITE;
        private final Color defaultBackground = Color.BLACK;

        private final transient BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
        private final AtomicBoolean cursorVisible = new AtomicBoolean(true);
        private transient Timer cursorTimer;

        @SuppressWarnings("this-escape")
        public TerminalComponent(int width, int height) {
            this.screenTerminal = new ScreenTerminal(width, height);

            // Set up font directly to avoid this-escape warning
            this.terminalFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);

            // Initialize the component after construction
            initializeComponent();
        }

        /**
         * Initializes the component after construction to avoid this-escape issues.
         */
        private void initializeComponent() {
            // Initialize font metrics
            initializeFontMetrics();

            // Set up component properties
            setFocusable(true);
            setBackground(defaultBackground);
            setForeground(defaultForeground);

            // Disable focus traversal for Tab key so we can handle it ourselves
            setFocusTraversalKeysEnabled(false);

            // Add listeners - this is done after construction to avoid this-escape
            addKeyListener(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                }
            });

            // Start cursor blinking timer
            startCursorTimer();

            // Update preferred size
            updatePreferredSize();
        }

        /**
         * Sets the terminal reference after construction to avoid this-escape issues.
         *
         * @param terminal the SwingTerminal instance
         */
        public void setTerminal(SwingTerminal terminal) {
            this.terminal = terminal;
        }

        /**
         * Starts the cursor blinking timer.
         */
        private void startCursorTimer() {
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
                // Use getMaxAdvance() for proper character cell width to prevent overlap
                // Fall back to 'M' width if getMaxAdvance() returns -1 (unknown)
                int maxAdvance = fontMetrics.getMaxAdvance();
                this.charWidth = (maxAdvance > 0) ? maxAdvance : fontMetrics.charWidth('M');
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
            int width = screenTerminal.getWidth() * charWidth;
            int height = screenTerminal.getHeight() * charHeight;
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
            int termWidth = screenTerminal.getWidth();
            int termHeight = screenTerminal.getHeight();

            // Get terminal screen data
            long[] screenData = new long[termWidth * termHeight];
            int[] cursor = new int[2];
            screenTerminal.dump(screenData, cursor);

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
            // Extract character (full Unicode code point) and attributes from cell
            int cp = (int) (cell & 0xffffffffL);
            long attr = cell >>> 32;

            // Continuation cells for wide characters (cp == 0):
            // Paint background to clear any stale content, but don't draw a glyph
            if (cp == 0) {
                int cbg = (int) (attr & 0x0fff);
                boolean cbgset = (attr & 0x20000000L) != 0;
                if (!cbgset) {
                    cbg = 0;
                }
                g2d.setColor(getAnsiColor(cbg));
                g2d.fillRect(x * charWidth, y * charHeight, charWidth, charHeight);
                return;
            }

            // Extract colors and attributes
            int bg = (int) ((attr) & 0x0fff);
            int fg = (int) ((attr >>> 12) & 0x0fff);
            boolean underline = (attr & 0x01000000L) != 0;
            boolean inverse = (attr & 0x02000000L) != 0;
            boolean conceal = (attr & 0x04000000L) != 0;
            boolean bold = (attr & 0x08000000L) != 0;
            boolean fgset = (attr & 0x10000000L) != 0;
            boolean bgset = (attr & 0x20000000L) != 0;

            if (!fgset) {
                fg = 0x0fff; // Default white foreground
            }
            if (!bgset) {
                bg = 0; // Default black background
            }

            // Handle inverse
            if (inverse) {
                int temp = fg;
                fg = bg;
                bg = temp;
            }

            // Handle conceal - hide text by making foreground match background
            if (conceal) {
                fg = bg;
            }

            // Handle cursor
            if (isCursor && cursorVisible.get()) {
                bg = 0x0fff; // White background for cursor
                fg = 0; // Black foreground for cursor
            }

            // Calculate position
            int cellX = x * charWidth;
            int cellY = y * charHeight;

            // Determine character width (wide chars span 2 cells)
            int cellSpan = isWideCharacter(cp) ? 2 : 1;

            // Paint background (spanning multiple cells for wide chars)
            g2d.setColor(getAnsiColor(bg));
            g2d.fillRect(cellX, cellY, charWidth * cellSpan, charHeight);

            // Paint character if not space
            if (cp != ' ') {
                g2d.setColor(getAnsiColor(fg));

                // Set font style
                Font font = terminalFont;
                if (bold) {
                    font = font.deriveFont(Font.BOLD);
                }
                g2d.setFont(font);

                // Draw character using code point for proper non-BMP support
                String str = new String(Character.toChars(cp));
                g2d.drawString(str, cellX, cellY + charAscent);

                // Draw underline if needed
                if (underline) {
                    int underlineY = cellY + charAscent + 1;
                    g2d.drawLine(cellX, underlineY, cellX + charWidth * cellSpan - 1, underlineY);
                }
            }
        }

        private static boolean isWideCharacter(int cp) {
            // CJK Unified Ideographs and related blocks
            return (cp >= 0x1100 && cp <= 0x115F)
                    || (cp >= 0x2E80 && cp <= 0xA4CF && cp != 0x303F)
                    || (cp >= 0xAC00 && cp <= 0xD7A3)
                    || (cp >= 0xF900 && cp <= 0xFAFF)
                    || (cp >= 0xFE10 && cp <= 0xFE6F)
                    || (cp >= 0xFF01 && cp <= 0xFF60)
                    || (cp >= 0xFFE0 && cp <= 0xFFE6)
                    || (cp >= 0x20000 && cp <= 0x2FFFD)
                    || (cp >= 0x30000 && cp <= 0x3FFFD);
        }

        private Color getAnsiColor(int color) {
            return new Color(((color >> 8) & 0x0f) << 4, ((color >> 4) & 0x0f) << 4, ((color >> 0) & 0x0f) << 4);
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

        /**
         * Writes text to the terminal component.
         *
         * @param text the text to write
         */
        public void write(String text) {
            screenTerminal.write(text);
            SwingUtilities.invokeLater(this::repaint);
        }

        /**
         * Dumps the terminal screen data.
         *
         * @param screen the screen data array to fill
         * @param x the starting x coordinate
         * @param y the starting y coordinate
         * @param height the height to dump
         * @param width the width to dump
         * @param cursor the cursor position array to fill
         */
        public void dump(long[] screen, int x, int y, int height, int width, int[] cursor) {
            screenTerminal.dump(screen, x, y, height, width, cursor);
        }

        /**
         * Dumps the terminal screen data with scrollback.
         *
         * @param timeout
         * @param forceUpdate
         * @return the screen data
         * @throws InterruptedException if interrupted while waiting
         */
        public String dump(long timeout, boolean forceUpdate) throws InterruptedException {
            return screenTerminal.dump(timeout, forceUpdate);
        }

        /**
         * Checks if the terminal is dirty (needs repainting).
         *
         * @return true if dirty
         */
        public boolean isDirty() {
            return screenTerminal.isDirty();
        }

        // KeyListener implementation
        @Override
        public void keyTyped(KeyEvent e) {
            char ch = e.getKeyChar();
            // Filter out characters already handled by keyPressed (Enter, Backspace, Escape, Tab)
            // to avoid duplicate input into the line discipline
            if (ch != '\t'
                    && ch != '\n'
                    && ch != '\r'
                    && ch != '\b'
                    && ch != '\u007f'
                    && ch != '\u001b'
                    && ch != KeyEvent.CHAR_UNDEFINED
                    && !e.isControlDown()) {
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
                    input = getCapabilitySequence(InfoCmp.Capability.key_backspace, "\u007f");
                    break;
                case KeyEvent.VK_TAB:
                    if (e.isShiftDown()) {
                        input = getCapabilitySequence(InfoCmp.Capability.key_btab, "\t");
                    } else {
                        input = "\t";
                    }
                    break;
                case KeyEvent.VK_UP:
                    input = getCapabilitySequence(InfoCmp.Capability.key_up, "\u001b[A");
                    break;
                case KeyEvent.VK_DOWN:
                    input = getCapabilitySequence(InfoCmp.Capability.key_down, "\u001b[B");
                    break;
                case KeyEvent.VK_RIGHT:
                    input = getCapabilitySequence(InfoCmp.Capability.key_right, "\u001b[C");
                    break;
                case KeyEvent.VK_LEFT:
                    input = getCapabilitySequence(InfoCmp.Capability.key_left, "\u001b[D");
                    break;
                case KeyEvent.VK_HOME:
                    input = getCapabilitySequence(InfoCmp.Capability.key_home, "\u001b[H");
                    break;
                case KeyEvent.VK_END:
                    input = getCapabilitySequence(InfoCmp.Capability.key_end, "\u001b[F");
                    break;
                case KeyEvent.VK_PAGE_UP:
                    input = getCapabilitySequence(InfoCmp.Capability.key_ppage, "\u001b[5~");
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    input = getCapabilitySequence(InfoCmp.Capability.key_npage, "\u001b[6~");
                    break;
                case KeyEvent.VK_INSERT:
                    input = getCapabilitySequence(InfoCmp.Capability.key_ic, "\u001b[2~");
                    break;
                case KeyEvent.VK_DELETE:
                    input = getCapabilitySequence(InfoCmp.Capability.key_dc, "\u001b[3~");
                    break;
                case KeyEvent.VK_F1:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f1, "\u001bOP");
                    break;
                case KeyEvent.VK_F2:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f2, "\u001bOQ");
                    break;
                case KeyEvent.VK_F3:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f3, "\u001bOR");
                    break;
                case KeyEvent.VK_F4:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f4, "\u001bOS");
                    break;
                case KeyEvent.VK_F5:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f5, "\u001b[15~");
                    break;
                case KeyEvent.VK_F6:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f6, "\u001b[17~");
                    break;
                case KeyEvent.VK_F7:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f7, "\u001b[18~");
                    break;
                case KeyEvent.VK_F8:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f8, "\u001b[19~");
                    break;
                case KeyEvent.VK_F9:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f9, "\u001b[20~");
                    break;
                case KeyEvent.VK_F10:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f10, "\u001b[21~");
                    break;
                case KeyEvent.VK_F11:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f11, "\u001b[23~");
                    break;
                case KeyEvent.VK_F12:
                    input = getCapabilitySequence(InfoCmp.Capability.key_f12, "\u001b[24~");
                    break;
                case KeyEvent.VK_ESCAPE:
                    input = "\u001b";
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

        /**
         * Gets the terminal capability sequence for the specified capability,
         * falling back to a default sequence if the capability is not available.
         *
         * @param capability the terminal capability
         * @param defaultSequence the default sequence to use if capability is not available
         * @return the capability sequence or default sequence
         */
        private String getCapabilitySequence(InfoCmp.Capability capability, String defaultSequence) {
            if (terminal != null) {
                String sequence = terminal.getStringCapability(capability);
                return sequence != null ? Curses.tputs(sequence) : defaultSequence;
            }
            return defaultSequence;
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
}
