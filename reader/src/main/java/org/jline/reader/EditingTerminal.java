/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

/**
 * Composite interface that combines all provider interfaces needed by the line editing engine.
 *
 * <p>This interface extends {@link InputProvider}, {@link OutputProvider},
 * {@link SizeProvider}, {@link CapabilityProvider}, and {@link SignalProvider},
 * providing a single type that supplies everything the editing engine requires.</p>
 *
 * <p>There are two primary usage patterns:</p>
 * <ul>
 *   <li><b>Terminal-backed</b> — Use
 *       {@link org.jline.reader.impl.TerminalAdapter TerminalAdapter} to wrap a JLine
 *       {@link org.jline.terminal.Terminal Terminal}. This is the default for existing
 *       applications and is fully backward compatible.</li>
 *   <li><b>Embedded</b> — TUI frameworks implement this interface directly to
 *       embed JLine's editing engine in their own event loop and rendering pipeline.</li>
 * </ul>
 *
 * @since 4.1
 * @see InputProvider
 * @see OutputProvider
 * @see SizeProvider
 * @see CapabilityProvider
 * @see SignalProvider
 * @see org.jline.reader.impl.TerminalAdapter
 */
public interface EditingTerminal
        extends InputProvider, OutputProvider, SizeProvider, CapabilityProvider, SignalProvider {}
