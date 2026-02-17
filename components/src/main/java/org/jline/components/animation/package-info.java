/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * Animation framework for terminal UI components.
 *
 * <p>Provides tick-based animation support for components that change over time,
 * such as spinners and gradient text effects.
 *
 * <h2>Key Types</h2>
 * <ul>
 *   <li>{@link org.jline.components.animation.Animatable} — Interface for components
 *       that support animation. Implement {@code onTick(long elapsedMs)} to update
 *       state and return {@code true} if the component needs re-rendering.</li>
 *   <li>{@link org.jline.components.animation.AnimationTimer} — Single daemon thread
 *       that ticks registered Animatable components and triggers re-rendering via a
 *       callback. Follows the pattern from JLine's Status class.</li>
 *   <li>{@link org.jline.components.animation.SpinnerFrames} — 25+ predefined spinner
 *       frame sets including DOTS, LINE, ARC, ARROW, BOUNCE, STAR, CLASSIC, and more.
 *       Each defines frame strings and a tick interval.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Components implementing {@link org.jline.components.animation.Animatable} are
 * automatically discovered when {@link org.jline.components.ComponentRenderer#startAnimations()}
 * is called. The animation timer handles the tick loop and triggers display updates.
 *
 * @see org.jline.components.ui.Spinner
 * @see org.jline.components.ui.Gradient
 */
package org.jline.components.animation;
