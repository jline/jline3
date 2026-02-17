/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import org.jline.terminal.Terminal;

/**
 * Manages rendering of a component tree to a terminal via {@link org.jline.utils.Display}.
 * Handles animation timer, terminal resize, and display lifecycle.
 */
public interface ComponentRenderer extends AutoCloseable {

    /**
     * Create a renderer for inline (partial-screen) rendering.
     */
    static ComponentRenderer create(Terminal terminal) {
        return new org.jline.components.impl.DefaultComponentRenderer(terminal, false);
    }

    /**
     * Create a renderer for full-screen rendering.
     */
    static ComponentRenderer fullScreen(Terminal terminal) {
        return new org.jline.components.impl.DefaultComponentRenderer(terminal, true);
    }

    /**
     * Set the root component to render.
     */
    void setRoot(Component root);

    /**
     * Render the component tree to the display.
     */
    void render();

    /**
     * Render the component tree with cursor position.
     *
     * @param cursorPos the desired cursor position, or -1 to hide
     */
    void renderToDisplay(int cursorPos);

    /**
     * Start the animation timer. Walks the component tree and
     * registers all {@link org.jline.components.animation.Animatable} components.
     */
    void startAnimations();

    /**
     * Stop the animation timer.
     */
    void stopAnimations();

    @Override
    void close();
}
