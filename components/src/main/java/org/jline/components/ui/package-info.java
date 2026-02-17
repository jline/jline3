/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * Built-in composable UI components.
 *
 * <p>All components extend {@link org.jline.components.ui.AbstractComponent} and use
 * the builder pattern for construction.
 *
 * <h2>Available Components</h2>
 * <table>
 *   <caption>Component catalog</caption>
 *   <tr><th>Component</th><th>Description</th></tr>
 *   <tr><td>{@link org.jline.components.ui.Box}</td>
 *       <td>Flexbox container with optional borders (SINGLE, DOUBLE, ROUNDED).
 *           Supports direction, justify, align, gap, and padding.</td></tr>
 *   <tr><td>{@link org.jline.components.ui.Text}</td>
 *       <td>Styled text with optional word wrapping, max width, and alignment.</td></tr>
 *   <tr><td>{@link org.jline.components.ui.Spinner}</td>
 *       <td>Animated loading indicator with 25+ frame sets and a label.
 *           Implements {@link org.jline.components.animation.Animatable}.</td></tr>
 *   <tr><td>{@link org.jline.components.ui.ProgressBar}</td>
 *       <td>Progress indicator (0.0–1.0) with configurable fill characters
 *           and optional percentage display.</td></tr>
 *   <tr><td>{@link org.jline.components.ui.StatusMessage}</td>
 *       <td>Colored status text with prefix icons: ✔ success, ⚠ warning,
 *           ✖ error, ℹ info.</td></tr>
 *   <tr><td>{@link org.jline.components.ui.Gradient}</td>
 *       <td>Gradient-colored text with RGB interpolation and optional
 *           animation sweep effect.</td></tr>
 *   <tr><td>{@link org.jline.components.ui.Hyperlink}</td>
 *       <td>Styled text with URL metadata for programmatic access.</td></tr>
 *   <tr><td>{@link org.jline.components.ui.Separator}</td>
 *       <td>Horizontal line with optional centered title text.</td></tr>
 * </table>
 *
 * <h2>Builder Pattern</h2>
 * <pre>{@code
 * Spinner spinner = Spinner.builder()
 *     .frames(SpinnerFrames.DOTS)
 *     .label("Loading...")
 *     .build();
 *
 * Box box = Box.builder()
 *     .direction(FlexDirection.COLUMN)
 *     .borderStyle(Box.BorderStyle.ROUNDED)
 *     .padding(Insets.of(1))
 *     .gap(1)
 *     .child(spinner)
 *     .child(Components.separator())
 *     .child(Components.progressBar(0.5))
 *     .build();
 * }</pre>
 *
 * @see org.jline.components.Components
 */
package org.jline.components.ui;
