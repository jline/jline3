/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedCharSequence;
import org.jline.utils.ColorPalette;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.OSUtils;

/**
 * A {@link Terminal} proxy that defers construction to a background thread.
 *
 * <p>Before the real terminal is ready, this proxy returns <em>heuristic defaults</em> —
 * not the result of real terminal detection — so callers can produce output without blocking:</p>
 * <ul>
 *   <li>{@link #writer()} — buffers output, replays through the real writer on completion</li>
 *   <li>{@link #getType()} — {@code TERM} env var; {@code "dumb"} when {@code NO_COLOR} is set
 *       or {@code TERM=dumb}; on Windows without {@code TERM}, checks {@code WT_SESSION},
 *       {@code ConEmuANSI=ON}, {@code ANSICON}, {@code TERM_PROGRAM=vscode} for ANSI support;
 *       {@code "ansi"} on non-Windows when {@code TERM} is unset</li>
 *   <li>{@link #getSize()}/{@link #getColumns()}/{@link #getRows()} — {@code COLUMNS}/{@code LINES}
 *       env vars, defaulting to 120×40</li>
 *   <li>{@link #getNumericCapability} for {@code max_colors} — detected from {@code COLORTERM}
 *       (truecolor/24bit → 16M) and {@code TERM} (256color → 256), defaulting to 256</li>
 *   <li>{@link #getPalette()} — default palette (no terminal query)</li>
 *   <li>{@link #encoding()} — {@link java.nio.charset.Charset#defaultCharset()}</li>
 *   <li>Other capabilities — {@code false}/{@code null}</li>
 * </ul>
 *
 * <p>Methods that require the real terminal (signal handling, raw mode, input, cursor queries)
 * block until construction completes.</p>
 *
 * <p>If the process exits before the terminal is ready, {@link #close()} drains buffered
 * output to {@code System.out}.</p>
 *
 * <p>Obtain via {@link org.jline.terminal.TerminalBuilder#buildDeferred()}.</p>
 */
public class DeferredTerminal implements Terminal {

    private final CompletableFuture<Terminal> future = new CompletableFuture<>();
    private final AtomicReference<Consumer<Terminal>> onComplete = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    // A single writer returned by writer() for the entire lifetime.
    // Before the terminal is ready, writes accumulate in a buffer.
    // On complete(), the buffer is replayed through the real terminal's writer,
    // and all subsequent writes go directly to it.
    private final StringWriter buffer = new StringWriter();
    private volatile Writer delegate;
    private final PrintWriter writer = new PrintWriter(
            new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    Writer d = delegate;
                    if (d != null) {
                        d.write(cbuf, off, len);
                    } else {
                        synchronized (buffer) {
                            if (delegate != null) {
                                delegate.write(cbuf, off, len);
                            } else {
                                buffer.write(cbuf, off, len);
                            }
                        }
                    }
                }

                @Override
                public void flush() throws IOException {
                    Writer d = delegate;
                    if (d != null) {
                        d.flush();
                    }
                }

                @Override
                public void close() {
                    // delegating writer is never closed independently
                }
            },
            true);

    public DeferredTerminal() {
        // public constructor for use by TerminalBuilder.buildDeferred()
    }

    /**
     * Registers a callback that runs once the real terminal is ready.
     * If the terminal is already ready when this method is called, the callback is invoked
     * immediately on the calling thread.
     * Useful for wiring up AnsiConsole, LineReader, etc.
     *
     * <p><b>Thread safety:</b> the callback may be invoked from the build thread or the calling
     * thread. Any mutable state captured by the callback must be safely published independently
     * of this field.</p>
     */
    public void onComplete(Consumer<Terminal> callback) {
        if (!onComplete.compareAndSet(null, callback)) {
            throw new IllegalStateException("onComplete callback already registered");
        }
        if (isReady()) {
            Consumer<Terminal> cb = onComplete.getAndSet(null);
            if (cb != null) {
                cb.accept(future.join());
            }
        }
    }

    public boolean complete(Terminal terminal) {
        // Complete the future first so that if the callback throws, waiters are not hung.
        boolean published = future.complete(terminal);
        if (published) {
            synchronized (buffer) {
                String buffered = buffer.toString();
                if (!buffered.isEmpty()) {
                    terminal.writer().print(buffered);
                    terminal.writer().flush();
                }
                delegate = terminal.writer();
            }
            Consumer<Terminal> cb = onComplete.getAndSet(null);
            if (cb != null) {
                cb.accept(terminal);
            }
        }
        return published;
    }

    @SuppressWarnings("java:S106") // intentional use of stdout as last-resort output
    public void completeExceptionally(Throwable t) {
        synchronized (buffer) {
            String buffered = buffer.toString();
            if (!buffered.isEmpty()) {
                System.out.print(buffered);
                System.out.flush();
            }
            delegate = new OutputStreamWriter(System.out);
        }
        future.completeExceptionally(t);
    }

    /**
     * Blocks until the real terminal is ready, then returns it.
     */
    public Terminal getTerminal() {
        return terminal();
    }

    public boolean isReady() {
        return future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally();
    }

    private Terminal terminal() {
        try {
            return future.join();
        } catch (CancellationException e) {
            throw new IllegalStateException("Terminal was closed before initialization completed", e);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof IOException) {
                throw new UncheckedIOException((IOException) cause);
            }
            throw new RuntimeException("Terminal build failed", cause);
        }
    }

    // -- Non-blocking methods --

    @Override
    public PrintWriter writer() {
        return writer;
    }

    @Override
    public String getType() {
        if (isReady()) {
            return terminal().getType();
        }
        if (System.getenv("NO_COLOR") != null) {
            return TYPE_DUMB;
        }
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) {
            return TYPE_DUMB;
        }
        if (term != null && !term.isEmpty()) {
            return term;
        }
        if (OSUtils.IS_WINDOWS) {
            if (System.getenv("WT_SESSION") != null
                    || "ON".equals(System.getenv("ConEmuANSI"))
                    || System.getenv("ANSICON") != null
                    || "vscode".equals(System.getenv("TERM_PROGRAM"))) {
                return "ansi";
            }
            return TYPE_DUMB;
        }
        return "ansi";
    }

    // -- Non-blocking methods with sensible defaults --

    @Override
    public String getName() {
        if (isReady()) {
            return terminal().getName();
        }
        return "JLine terminal";
    }

    @Override
    public Charset encoding() {
        if (isReady()) {
            return terminal().encoding();
        }
        return Charset.defaultCharset();
    }

    @Override
    public Size getSize() {
        if (isReady()) {
            return terminal().getSize();
        }
        return Size.of(parseEnvInt("COLUMNS", 120), parseEnvInt("LINES", 40));
    }

    @Override
    public int getColumns() {
        return getSize().getColumns();
    }

    @Override
    public int getRows() {
        return getSize().getRows();
    }

    private static int parseEnvInt(String name, int defaultValue) {
        try {
            String val = System.getenv(name);
            return (val != null && !val.isEmpty()) ? Integer.parseInt(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanCapability(Capability capability) {
        if (isReady()) {
            return terminal().getBooleanCapability(capability);
        }
        return false;
    }

    @Override
    public Integer getNumericCapability(Capability capability) {
        if (isReady()) {
            return terminal().getNumericCapability(capability);
        }
        if (capability == Capability.max_colors) {
            return detectMaxColors();
        }
        return null;
    }

    private static int detectMaxColors() {
        String colorterm = System.getenv("COLORTERM");
        if (colorterm != null) {
            colorterm = colorterm.toLowerCase(java.util.Locale.ROOT);
            if (colorterm.contains("truecolor") || colorterm.contains("24bit")) {
                return AttributedCharSequence.TRUE_COLORS;
            }
        }
        String term = System.getenv("TERM");
        if (term != null && term.contains("256color")) {
            return 256;
        }
        if (OSUtils.IS_WINDOWS) {
            // Windows Terminal and VS Code support true colors
            if (System.getenv("WT_SESSION") != null || "vscode".equals(System.getenv("TERM_PROGRAM"))) {
                return AttributedCharSequence.TRUE_COLORS;
            }
            // ConEmu supports 256 colors
            if ("ON".equals(System.getenv("ConEmuANSI"))) {
                return 256;
            }
            // ANSICON supports basic 16 colors
            if (System.getenv("ANSICON") != null) {
                return 16;
            }
        }
        return 256;
    }

    @Override
    public String getStringCapability(Capability capability) {
        if (isReady()) {
            return terminal().getStringCapability(capability);
        }
        return null;
    }

    @Override
    public ColorPalette getPalette() {
        if (isReady()) {
            return terminal().getPalette();
        }
        try {
            return new ColorPalette(this);
        } catch (IOException e) {
            return ColorPalette.DEFAULT;
        }
    }

    @Override
    public void flush() {
        writer.flush();
    }

    // -- Blocking methods: require the real terminal --

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        return terminal().handle(signal, handler);
    }

    @Override
    public void raise(Signal signal) {
        terminal().raise(signal);
    }

    @Override
    public NonBlockingReader reader() {
        return terminal().reader();
    }

    @Override
    public Charset inputEncoding() {
        return terminal().inputEncoding();
    }

    @Override
    public Charset outputEncoding() {
        return terminal().outputEncoding();
    }

    @Override
    public InputStream input() {
        return terminal().input();
    }

    @Override
    public OutputStream output() {
        return terminal().output();
    }

    @Override
    public boolean canPauseResume() {
        return terminal().canPauseResume();
    }

    @Override
    public void pause() {
        terminal().pause();
    }

    @Override
    public void pause(boolean wait) throws InterruptedException {
        terminal().pause(wait);
    }

    @Override
    public void resume() {
        terminal().resume();
    }

    @Override
    public boolean paused() {
        return terminal().paused();
    }

    @Override
    public Attributes enterRawMode() {
        return terminal().enterRawMode();
    }

    @Override
    public boolean echo() {
        return terminal().echo();
    }

    @Override
    public boolean echo(boolean echo) {
        return terminal().echo(echo);
    }

    @Override
    public Attributes getAttributes() {
        return terminal().getAttributes();
    }

    @Override
    public void setAttributes(Attributes attr) {
        terminal().setAttributes(attr);
    }

    @Override
    public void setSize(Sized size) {
        terminal().setSize(size);
    }

    @Override
    public Size getBufferSize() {
        return terminal().getBufferSize();
    }

    @Override
    public boolean puts(Capability capability, Object... params) {
        return terminal().puts(capability, params);
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return terminal().getCursorPosition(discarded);
    }

    @Override
    public boolean hasMouseSupport() {
        return terminal().hasMouseSupport();
    }

    @Override
    public boolean trackMouse(MouseTracking tracking) {
        return terminal().trackMouse(tracking);
    }

    @Override
    public MouseTracking getCurrentMouseTracking() {
        return terminal().getCurrentMouseTracking();
    }

    @Override
    public MouseEvent readMouseEvent() {
        return terminal().readMouseEvent();
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier reader) {
        return terminal().readMouseEvent(reader);
    }

    @Override
    public MouseEvent readMouseEvent(String prefix) {
        return terminal().readMouseEvent(prefix);
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier reader, String prefix) {
        return terminal().readMouseEvent(reader, prefix);
    }

    @Override
    public boolean hasFocusSupport() {
        return terminal().hasFocusSupport();
    }

    @Override
    public boolean trackFocus(boolean tracking) {
        return terminal().trackFocus(tracking);
    }

    @SuppressWarnings("java:S106") // intentional use of stdout as last-resort output
    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (isReady()) {
            future.join().close();
        } else if (future.cancel(true)) {
            synchronized (buffer) {
                String buffered = buffer.toString();
                if (!buffered.isEmpty()) {
                    System.out.print(buffered);
                    System.out.flush();
                }
            }
        } else {
            // cancel() failed — complete() may have raced us. Close if it succeeded.
            try {
                Terminal t = future.getNow(null);
                if (t != null) {
                    t.close();
                }
            } catch (Exception ignored) {
                // completed exceptionally — nothing to close
            }
        }
    }
}
