/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * Animation timer that ticks registered {@link Animatable} components
 * and triggers re-rendering via a callback.
 *
 * <p>Uses a single daemon thread, following the pattern from Status class.</p>
 */
public class AnimationTimer {

    private final List<Animatable> animatables = new ArrayList<>();
    private final Runnable onDirty;
    private volatile Thread timerThread;
    private volatile boolean running;
    private long startTime;

    public AnimationTimer(Runnable onDirty) {
        this.onDirty = onDirty;
    }

    public synchronized void register(Animatable animatable) {
        animatables.add(animatable);
    }

    public synchronized void unregister(Animatable animatable) {
        animatables.remove(animatable);
    }

    public synchronized void clear() {
        animatables.clear();
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        startTime = System.currentTimeMillis();
        timerThread = new Thread(this::run, "JLine-AnimationTimer");
        timerThread.setDaemon(true);
        timerThread.start();
    }

    public void stop() {
        Thread t;
        synchronized (this) {
            running = false;
            t = timerThread;
            timerThread = null;
        }
        if (t != null) {
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void run() {
        while (running) {
            long elapsed = System.currentTimeMillis() - startTime;
            boolean anyDirty = false;

            synchronized (this) {
                for (Animatable a : animatables) {
                    if (a.onTick(elapsed)) {
                        anyDirty = true;
                    }
                }
            }

            if (anyDirty && onDirty != null) {
                onDirty.run();
            }

            // Find the minimum interval
            long minInterval;
            synchronized (this) {
                minInterval = Long.MAX_VALUE;
                for (Animatable a : animatables) {
                    minInterval = Math.min(minInterval, a.getIntervalMs());
                }
            }
            if (minInterval == Long.MAX_VALUE) {
                minInterval = 100;
            }

            try {
                Thread.sleep(minInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
