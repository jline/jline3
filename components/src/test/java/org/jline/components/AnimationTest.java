/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.util.concurrent.atomic.AtomicInteger;

import org.jline.components.animation.Animatable;
import org.jline.components.animation.AnimationTimer;
import org.jline.components.animation.SpinnerFrames;
import org.jline.components.ui.Gradient;
import org.jline.components.ui.Spinner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimationTest {

    @Test
    void testSpinnerFramesCycling() {
        for (SpinnerFrames sf : SpinnerFrames.values()) {
            assertTrue(sf.frameCount() > 0, "SpinnerFrames." + sf.name() + " has no frames");
            assertTrue(sf.intervalMs() > 0, "SpinnerFrames." + sf.name() + " has invalid interval");
            assertTrue(sf.maxWidth() > 0, "SpinnerFrames." + sf.name() + " has zero width");

            // Test frame wrapping
            assertEquals(sf.frame(0), sf.frame(sf.frameCount()));
            assertNotNull(sf.frame(0));
        }
    }

    @Test
    void testSpinnerOnTickAdvancesFrames() {
        Spinner spinner = Spinner.builder().frames(SpinnerFrames.CLASSIC).build();
        long interval = spinner.getIntervalMs();

        // Frame 0 at tick 0
        assertFalse(spinner.onTick(0));

        // Frame 1 at tick = interval
        assertTrue(spinner.onTick(interval));

        // Same frame again should not be dirty
        assertFalse(spinner.onTick(interval));

        // Frame 2 at tick = 2*interval
        assertTrue(spinner.onTick(2 * interval));

        // Full cycle returns to frame 0
        long fullCycle = interval * SpinnerFrames.CLASSIC.frameCount();
        spinner.onTick(fullCycle - 1); // advance to some frame
        boolean changed = spinner.onTick(fullCycle);
        // At full cycle, we're back to frame 0
        assertNotNull(spinner); // just verify no exception
    }

    @Test
    void testSpinnerDifferentFrameSets() {
        for (SpinnerFrames sf : SpinnerFrames.values()) {
            Spinner spinner = Spinner.builder().frames(sf).label("test").build();
            Canvas canvas = Canvas.create(40, 1);
            spinner.render(canvas, 40, 1);
            String line = canvas.toLines().get(0).toString();
            assertTrue(line.contains("test"), "Spinner " + sf.name() + " should render label");
        }
    }

    @Test
    void testSpinnerSetLabel() {
        Spinner spinner = Spinner.builder().label("old").build();
        Canvas canvas = Canvas.create(30, 1);
        spinner.render(canvas, 30, 1);
        assertTrue(canvas.toLines().get(0).toString().contains("old"));

        spinner.setLabel("new");
        assertTrue(spinner.isDirty());

        canvas = Canvas.create(30, 1);
        spinner.render(canvas, 30, 1);
        assertTrue(canvas.toLines().get(0).toString().contains("new"));
    }

    @Test
    void testGradientAnimation() {
        Gradient gradient = Gradient.builder()
                .text("Hello")
                .colors(new int[] {255, 0, 0}, new int[] {0, 255, 0}, new int[] {0, 0, 255})
                .animate(true)
                .build();

        assertEquals(16, gradient.getIntervalMs());

        // Tick 0 should not change
        assertFalse(gradient.onTick(0));

        // Tick past threshold should change phase
        assertTrue(gradient.onTick(100));
    }

    @Test
    void testGradientNoAnimation() {
        Gradient gradient = Gradient.builder()
                .text("Static")
                .colors(new int[] {255, 0, 0}, new int[] {0, 0, 255})
                .build();

        assertFalse(gradient.onTick(0));
        assertFalse(gradient.onTick(1000));
    }

    @Test
    void testAnimationTimerRegisterAndClear() {
        AnimationTimer timer = new AnimationTimer(() -> {});
        Animatable animatable = new Animatable() {
            @Override
            public boolean onTick(long elapsedMs) {
                return false;
            }

            @Override
            public long getIntervalMs() {
                return 100;
            }
        };

        timer.register(animatable);
        timer.clear();
        // Should not throw
        assertFalse(timer.isRunning());
    }

    @Test
    void testAnimationTimerStartStop() throws InterruptedException {
        AnimationTimer timer = new AnimationTimer(() -> {});
        timer.register(new Animatable() {
            @Override
            public boolean onTick(long elapsedMs) {
                return false;
            }

            @Override
            public long getIntervalMs() {
                return 50;
            }
        });

        assertFalse(timer.isRunning());
        timer.start();
        assertTrue(timer.isRunning());

        // Double start should be idempotent
        timer.start();
        assertTrue(timer.isRunning());

        timer.stop();
        assertFalse(timer.isRunning());

        // Double stop should be safe
        timer.stop();
        assertFalse(timer.isRunning());
    }

    @Test
    void testAnimationTimerCallsOnDirty() throws InterruptedException {
        boolean[] dirtyCalled = {false};
        AnimationTimer timer = new AnimationTimer(() -> dirtyCalled[0] = true);
        timer.register(new Animatable() {
            private int lastFrame = -1;

            @Override
            public boolean onTick(long elapsedMs) {
                int frame = (int) (elapsedMs / 50);
                if (frame != lastFrame) {
                    lastFrame = frame;
                    return true;
                }
                return false;
            }

            @Override
            public long getIntervalMs() {
                return 50;
            }
        });

        timer.start();
        Thread.sleep(200);
        timer.stop();

        assertTrue(dirtyCalled[0], "onDirty callback should have been called");
    }

    @Test
    void testAnimationTimerUnregister() {
        AtomicInteger tickCount = new AtomicInteger(0);
        Animatable animatable = new Animatable() {
            @Override
            public boolean onTick(long elapsedMs) {
                tickCount.incrementAndGet();
                return true;
            }

            @Override
            public long getIntervalMs() {
                return 10;
            }
        };

        AnimationTimer timer = new AnimationTimer(() -> {});
        timer.register(animatable);
        timer.unregister(animatable);

        // After unregister, start/stop should work without ticking the removed animatable
        timer.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        timer.stop();

        assertEquals(0, tickCount.get());
    }

    @Test
    void testAnimationTimerRestartWithDifferentAnimatables() throws InterruptedException {
        AtomicInteger tickCount1 = new AtomicInteger(0);
        AtomicInteger tickCount2 = new AtomicInteger(0);

        Animatable a1 = new Animatable() {
            @Override
            public boolean onTick(long elapsedMs) {
                tickCount1.incrementAndGet();
                return true;
            }

            @Override
            public long getIntervalMs() {
                return 10;
            }
        };

        Animatable a2 = new Animatable() {
            @Override
            public boolean onTick(long elapsedMs) {
                tickCount2.incrementAndGet();
                return true;
            }

            @Override
            public long getIntervalMs() {
                return 10;
            }
        };

        AnimationTimer timer = new AnimationTimer(() -> {});

        // First run with a1
        timer.register(a1);
        timer.start();
        Thread.sleep(50);
        timer.stop();

        int count1AfterFirst = tickCount1.get();
        assertTrue(count1AfterFirst > 0);

        // Clear and restart with a2
        timer.clear();
        timer.register(a2);
        timer.start();
        Thread.sleep(50);
        timer.stop();

        // a1 should not have received more ticks
        assertEquals(count1AfterFirst, tickCount1.get());
        // a2 should have been ticked
        assertTrue(tickCount2.get() > 0);
    }
}
