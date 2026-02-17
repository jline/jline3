/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.animation;

/**
 * Interface for components that support animation via periodic ticks.
 */
public interface Animatable {

    /**
     * Called on each animation tick.
     *
     * @param elapsedMs milliseconds since animation started
     * @return true if the component was updated and needs re-rendering
     */
    boolean onTick(long elapsedMs);

    /**
     * Returns the desired tick interval in milliseconds.
     */
    long getIntervalMs();
}
