/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.impl;

import java.util.List;

import org.jline.components.Canvas;
import org.jline.components.Component;
import org.jline.components.ComponentRenderer;
import org.jline.components.animation.Animatable;
import org.jline.components.animation.AnimationTimer;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.utils.AttributedString;
import org.jline.utils.Display;

/**
 * Default implementation of {@link ComponentRenderer} that renders
 * a component tree to a terminal via {@link Display}.
 */
public class DefaultComponentRenderer implements ComponentRenderer {

    private final Terminal terminal;
    private final Display display;
    private final boolean fullScreen;
    private final Object lock = new Object();
    private Component root;
    private AnimationTimer animationTimer;
    private Terminal.SignalHandler prevWinchHandler;

    public DefaultComponentRenderer(Terminal terminal, boolean fullScreen) {
        this.terminal = terminal;
        this.fullScreen = fullScreen;
        this.display = new Display(terminal, fullScreen);
        this.animationTimer = new AnimationTimer(this::render);

        // Reset any stale OSC 8 hyperlink state left by a previous process
        terminal.writer().write("\033]8;;\033\\");
        terminal.writer().flush();

        // Handle terminal resize
        prevWinchHandler = terminal.handle(Signal.WINCH, s -> {
            synchronized (lock) {
                render();
            }
        });
    }

    @Override
    public void setRoot(Component root) {
        synchronized (lock) {
            this.root = root;
            // Re-register animatables if the animation timer is running
            if (animationTimer.isRunning()) {
                animationTimer.clear();
                if (root != null) {
                    collectAnimatables(root);
                }
            }
        }
    }

    @Override
    public void render() {
        renderToDisplay(-1);
    }

    @Override
    public void renderToDisplay(int cursorPos) {
        synchronized (lock) {
            if (root == null) return;

            int width = terminal.getWidth();
            int height =
                    fullScreen ? terminal.getHeight() : root.getPreferredSize().height();
            if (width <= 0) width = 80;
            if (height <= 0) height = 1;

            display.resize(height, width);

            Canvas canvas = Canvas.create(width, height);
            root.render(canvas, width, height);

            List<AttributedString> lines = canvas.toLines();
            display.update(lines, cursorPos);
        }
    }

    @Override
    public void startAnimations() {
        synchronized (lock) {
            animationTimer.clear();
            if (root != null) {
                collectAnimatables(root);
            }
            animationTimer.start();
        }
    }

    @Override
    public void stopAnimations() {
        animationTimer.stop();
    }

    @Override
    public void close() {
        stopAnimations();
        if (prevWinchHandler != null) {
            terminal.handle(Signal.WINCH, prevWinchHandler);
        }
    }

    private void collectAnimatables(Component component) {
        if (component instanceof Animatable) {
            animationTimer.register((Animatable) component);
        }
        for (Component child : component.getChildren()) {
            collectAnimatables(child);
        }
    }
}
