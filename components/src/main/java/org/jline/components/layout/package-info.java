/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * Flexbox-inspired layout engine for character-cell grids.
 *
 * <p>Provides a simplified flexbox layout operating on integer character cells.
 * The layout algorithm follows these steps:
 * <ol>
 *   <li><b>Measure:</b> Query each child's preferred size</li>
 *   <li><b>Distribute main-axis:</b> Use preferred sizes, shrink proportionally if overflow</li>
 *   <li><b>Cross-axis:</b> Position based on {@link org.jline.components.layout.FlexAlign}</li>
 *   <li><b>Justify:</b> Position children based on {@link org.jline.components.layout.FlexJustify}</li>
 * </ol>
 *
 * <h2>Layout Types</h2>
 * <ul>
 *   <li>{@link org.jline.components.layout.FlexDirection} — ROW (horizontal) or COLUMN (vertical)</li>
 *   <li>{@link org.jline.components.layout.FlexAlign} — Cross-axis alignment: START, CENTER, END, STRETCH</li>
 *   <li>{@link org.jline.components.layout.FlexJustify} — Main-axis justification: START, CENTER, END,
 *       SPACE_BETWEEN, SPACE_AROUND</li>
 *   <li>{@link org.jline.components.layout.Insets} — Padding/margin in character cells</li>
 *   <li>{@link org.jline.components.layout.Size} — Immutable width×height pair</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>The layout engine is used internally by {@link org.jline.components.ui.Box} but
 * can also be called directly:
 * <pre>{@code
 * LayoutEngine.layout(canvas, children, width, height,
 *     FlexDirection.COLUMN, FlexJustify.START, FlexAlign.STRETCH,
 *     gap, padding);
 * }</pre>
 *
 * @see org.jline.components.layout.LayoutEngine
 */
package org.jline.components.layout;
