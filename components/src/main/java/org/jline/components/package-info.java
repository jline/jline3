/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * Core API for composable terminal UI components.
 *
 * <p>This package provides the foundational interfaces for building composable,
 * output-only UI components for terminal applications. Components render into a
 * {@link org.jline.components.Canvas} which produces {@link org.jline.utils.AttributedString}
 * lines compatible with JLine's {@link org.jline.utils.Display}.
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link org.jline.components.Component} — The core rendering interface. Components
 *       report their preferred size and render into a canvas region. They have no input
 *       handling or focus management — purely rendering.</li>
 *   <li>{@link org.jline.components.Canvas} — A character grid buffer. Supports styled text,
 *       character placement, rectangular fills, and sub-region clipping. Convert to {@code List<AttributedString>} via
 *       {@link org.jline.components.Canvas#toLines()}.</li>
 *   <li>{@link org.jline.components.ComponentRenderer} — Manages rendering a component tree
 *       to a terminal. Wraps {@link org.jline.utils.Display}, handles terminal resize signals,
 *       and drives the animation timer for animated components.</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Create components using the Components factory
 * Box root = Components.borderedBox(Box.BorderStyle.ROUNDED,
 *     Components.text("Hello, World!"),
 *     Components.separator(),
 *     Components.spinner("Loading...")
 * );
 *
 * // Render to terminal
 * ComponentRenderer renderer = ComponentRenderer.create(terminal);
 * renderer.setRoot(root);
 * renderer.startAnimations();
 * renderer.render();
 *
 * // ... later
 * renderer.close();
 * }</pre>
 *
 * <h2>Using Without ComponentRenderer</h2>
 * <p>Components can also be used standalone with any Display:
 * <pre>{@code
 * Canvas canvas = Canvas.create(width, height);
 * root.render(canvas, width, height);
 * display.update(canvas.toLines(), cursorPos);
 * }</pre>
 *
 * @see org.jline.components.ui
 * @see org.jline.components.layout
 * @see org.jline.components.animation
 */
package org.jline.components;
