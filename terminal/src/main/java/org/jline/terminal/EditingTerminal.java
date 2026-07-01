/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

/**
 * Composite interface that combines all provider interfaces needed by the line editing engine.
 *
 * <p>This interface extends {@link InputProvider}, {@link OutputProvider},
 * {@link SizeProvider}, {@link CapabilityProvider}, and {@link SignalProvider},
 * providing a single type that supplies everything the editing engine requires.</p>
 *
 * <p>There are two primary usage patterns:</p>
 * <ul>
 *   <li><b>Terminal-backed</b> — Pass a JLine {@link Terminal} directly.
 *       Since {@code Terminal extends EditingTerminal}, no adapter is needed.</li>
 *   <li><b>Embedded</b> — TUI frameworks implement this interface directly to
 *       embed JLine's editing engine in their own event loop and rendering pipeline,
 *       without needing to implement the full {@link Terminal} interface.</li>
 * </ul>
 *
 * @since 4.1
 * @see InputProvider
 * @see OutputProvider
 * @see SizeProvider
 * @see CapabilityProvider
 * @see SignalProvider
 * @see Terminal
 */
public interface EditingTerminal
        extends InputProvider, OutputProvider, SizeProvider, CapabilityProvider, SignalProvider {}
