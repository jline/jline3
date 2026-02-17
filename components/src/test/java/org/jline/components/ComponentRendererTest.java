/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.io.IOException;

import org.jline.components.animation.SpinnerFrames;
import org.jline.components.layout.FlexDirection;
import org.jline.components.ui.Box;
import org.jline.components.ui.Hyperlink;
import org.jline.components.ui.Spinner;
import org.jline.components.ui.Text;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRendererTest {

    private Terminal terminal;
    private ComponentRenderer renderer;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        renderer = ComponentRenderer.create(terminal);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (renderer != null) renderer.close();
        if (terminal != null) terminal.close();
    }

    @Test
    void testRenderWithNullRoot() {
        // Should not throw
        renderer.render();
    }

    @Test
    void testSetRootAndRender() {
        Text text = Text.builder().text("Hello").build();
        renderer.setRoot(text);
        renderer.render();
        // Should not throw
    }

    @Test
    void testRenderToDisplayWithCursorPos() {
        renderer.setRoot(Text.builder().text("Test").build());
        renderer.renderToDisplay(0);
        renderer.renderToDisplay(-1);
        // Should not throw
    }

    @Test
    void testStartStopAnimations() {
        Spinner spinner =
                Spinner.builder().frames(SpinnerFrames.DOTS).label("Loading").build();
        renderer.setRoot(spinner);
        renderer.startAnimations();
        renderer.stopAnimations();
        // Should not throw
    }

    @Test
    void testSetRootDuringAnimation() throws InterruptedException {
        Spinner spinner1 =
                Spinner.builder().frames(SpinnerFrames.DOTS).label("First").build();
        renderer.setRoot(spinner1);
        renderer.startAnimations();

        // Replace root with a different animatable
        Spinner spinner2 =
                Spinner.builder().frames(SpinnerFrames.LINE).label("Second").build();
        renderer.setRoot(spinner2);

        Thread.sleep(50); // let animation tick

        renderer.stopAnimations();
    }

    @Test
    void testNestedAnimatableDiscovery() throws InterruptedException {
        // Spinner nested inside a Hyperlink inside a Box
        Spinner spinner =
                Spinner.builder().frames(SpinnerFrames.DOTS).label("Nested").build();
        Hyperlink link =
                Hyperlink.builder().url("http://example.com").inner(spinner).build();
        Box box = Box.builder().direction(FlexDirection.COLUMN).child(link).build();

        renderer.setRoot(box);
        renderer.startAnimations();

        Thread.sleep(200);

        renderer.stopAnimations();

        // Spinner should have been discovered and ticked by the animation timer.
        // After stopping, calling onTick(0) resets to frame 0; if the timer
        // already advanced the frame, this will return true.
        assertTrue(spinner.onTick(0));
    }

    @Test
    void testDoubleStartAnimationsIsIdempotent() {
        renderer.setRoot(Spinner.builder().build());
        renderer.startAnimations();
        renderer.startAnimations(); // should not throw or create duplicate threads
        renderer.stopAnimations();
    }

    @Test
    void testCloseStopsAnimations() {
        renderer.setRoot(Spinner.builder().build());
        renderer.startAnimations();
        renderer.close();
        renderer = null; // prevent double-close in tearDown
    }

    @Test
    void testSetRootToNull() {
        renderer.setRoot(Text.builder().text("Hello").build());
        renderer.render();
        renderer.setRoot(null);
        renderer.render(); // should not throw
    }

    @Test
    void testFullScreenRenderer() throws IOException {
        try (Terminal t = TerminalBuilder.builder().dumb(true).build();
                ComponentRenderer r = ComponentRenderer.fullScreen(t)) {
            r.setRoot(Text.builder().text("Full").build());
            r.render();
        }
    }
}
