/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.util.Collections;
import java.util.List;

import org.jline.components.layout.Size;

/**
 * A purely rendering component with no input handling.
 * Components render themselves into a {@link Canvas} at a given size.
 *
 * <h2>Dirty Tracking</h2>
 * <p>Components track whether they need re-rendering via {@link #isDirty()} and
 * {@link #invalidate()}. When a component's state changes (e.g., text update,
 * progress change), it should call {@code invalidate()} to signal that it needs
 * re-rendering. The {@code render()} implementation should clear the dirty flag
 * after rendering (typically by calling {@code markClean()} in
 * {@link org.jline.components.ui.AbstractComponent}).</p>
 *
 * <p>Composite components (like {@link org.jline.components.ui.Box}) should
 * override {@code isDirty()} to also check their children's dirty state, and
 * should call {@code markClean()} <em>after</em> rendering children to avoid
 * missing child invalidations that occur during rendering.</p>
 *
 * <h2>Component Tree</h2>
 * <p>Composite components should override {@link #getChildren()} to return their
 * child components. This is used by {@link ComponentRenderer#startAnimations()}
 * to discover nested {@link org.jline.components.animation.Animatable} components
 * in the tree. Failing to override this method in a composite component will
 * cause nested animations to be silently ignored.</p>
 */
public interface Component {

    /**
     * Returns the preferred size of this component.
     * The layout engine queries this to determine space allocation.
     *
     * @return the preferred size, never null
     */
    Size getPreferredSize();

    /**
     * Render this component into the given canvas region.
     *
     * @param canvas the canvas to draw into
     * @param width  the allocated width in character cells
     * @param height the allocated height in character cells
     */
    void render(Canvas canvas, int width, int height);

    /**
     * Returns whether this component needs re-rendering.
     * Composite components should also check their children's dirty state.
     */
    boolean isDirty();

    /**
     * Marks this component as needing re-rendering.
     */
    void invalidate();

    /**
     * Returns the direct child components of this component.
     *
     * <p>Used by the animation framework to recursively discover
     * {@link org.jline.components.animation.Animatable} components in the tree.
     * Composite components (e.g., containers, wrappers) must override this
     * method to return their children; otherwise nested animations will not
     * be discovered by {@link ComponentRenderer#startAnimations()}.</p>
     *
     * @return an unmodifiable list of child components, never null
     */
    default List<Component> getChildren() {
        return Collections.emptyList();
    }
}
