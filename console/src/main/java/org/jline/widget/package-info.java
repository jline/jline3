/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Widget package provides a framework for creating and managing widgets for JLine's LineReader.
 * <p>
 * This package contains classes for:
 * <ul>
 *   <li>Creating custom widgets that can be bound to key sequences</li>
 *   <li>Managing widget state and behavior</li>
 *   <li>Interacting with the LineReader's buffer and terminal</li>
 *   <li>Implementing specialized widgets for auto-completion, auto-suggestion, and more</li>
 * </ul>
 * <p>
 * Key components include:
 * <ul>
 *   <li>{@link org.jline.widget.Widgets} - Base class for creating custom widgets</li>
 *   <li>{@link org.jline.widget.AutopairWidgets} - Widgets for auto-pairing brackets and quotes</li>
 *   <li>{@link org.jline.widget.AutosuggestionWidgets} - Widgets for auto-suggestion functionality</li>
 *   <li>{@link org.jline.widget.TailTipWidgets} - Widgets for displaying command hints in the terminal</li>
 * </ul>
 * <p>
 * Widgets are reusable components that can be bound to key sequences and provide specific
 * functionality when invoked. They can be used to enhance the functionality of the LineReader
 * with features like auto-completion, auto-suggestion, and command hints.
 */
package org.jline.widget;
