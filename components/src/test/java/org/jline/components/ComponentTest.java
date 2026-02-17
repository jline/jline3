/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.util.List;

import org.jline.components.animation.SpinnerFrames;
import org.jline.components.layout.FlexAlign;
import org.jline.components.layout.FlexDirection;
import org.jline.components.layout.Insets;
import org.jline.components.layout.Size;
import org.jline.components.ui.Box;
import org.jline.components.ui.Gradient;
import org.jline.components.ui.ProgressBar;
import org.jline.components.ui.Separator;
import org.jline.components.ui.Spinner;
import org.jline.components.ui.StatusMessage;
import org.jline.components.ui.Text;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {

    @Test
    void testTextComponent() {
        Text text = Text.builder().text("Hello World").build();
        assertEquals(new Size(11, 1), text.getPreferredSize());

        Canvas canvas = Canvas.create(20, 1);
        text.render(canvas, 20, 1);
        assertTrue(canvas.toLines().get(0).toString().startsWith("Hello World"));
        assertFalse(text.isDirty());
    }

    @Test
    void testTextWrapping() {
        Text text =
                Text.builder().text("Hello World Foo").wrap(true).maxWidth(8).build();
        Size pref = text.getPreferredSize();
        assertEquals(3, pref.height()); // "Hello" + "World" + "Foo" wrapped at 8

        Canvas canvas = Canvas.create(8, 3);
        text.render(canvas, 8, 3);
        List<AttributedString> lines = canvas.toLines();
        assertTrue(lines.get(0).toString().startsWith("Hello"));
        assertTrue(lines.get(1).toString().startsWith("World"));
    }

    @Test
    void testTextCenterAlignment() {
        Text text = Text.builder().text("Hi").alignment(FlexAlign.CENTER).build();
        Canvas canvas = Canvas.create(10, 1);
        text.render(canvas, 10, 1);
        String line = canvas.toLines().get(0).toString();
        // "Hi" centered in 10 chars: 4 spaces + Hi + 4 spaces
        assertEquals('H', line.charAt(4));
    }

    @Test
    void testSeparator() {
        Separator sep = Separator.builder().build();
        assertEquals(1, sep.getPreferredSize().height());

        Canvas canvas = Canvas.create(10, 1);
        sep.render(canvas, 10, 1);
        String line = canvas.toLines().get(0).toString();
        // Should be all horizontal line chars
        for (int i = 0; i < 10; i++) {
            assertEquals('\u2500', line.charAt(i));
        }
    }

    @Test
    void testSeparatorWithTitle() {
        Separator sep = Separator.builder().title("Test").build();
        Canvas canvas = Canvas.create(20, 1);
        sep.render(canvas, 20, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("Test"));
    }

    @Test
    void testBoxWithBorder() {
        Box box = Box.builder()
                .direction(FlexDirection.COLUMN)
                .borderStyle(Box.BorderStyle.SINGLE)
                .child(Text.builder().text("Hi").build())
                .build();

        Size pref = box.getPreferredSize();
        assertTrue(pref.width() >= 4); // 2 border + text
        assertTrue(pref.height() >= 3); // 2 border + text

        Canvas canvas = Canvas.create(10, 5);
        box.render(canvas, 10, 5);
        List<AttributedString> lines = canvas.toLines();

        // Top-left corner
        assertEquals('\u250c', lines.get(0).charAt(0));
        // Top-right corner
        assertEquals('\u2510', lines.get(0).charAt(9));
        // Bottom-left corner
        assertEquals('\u2514', lines.get(4).charAt(0));
        // Side
        assertEquals('\u2502', lines.get(1).charAt(0));
    }

    @Test
    void testBoxRoundedBorder() {
        Box box = Box.builder()
                .borderStyle(Box.BorderStyle.ROUNDED)
                .child(Text.builder().text("X").build())
                .build();

        Canvas canvas = Canvas.create(5, 3);
        box.render(canvas, 5, 3);
        List<AttributedString> lines = canvas.toLines();
        assertEquals('\u256d', lines.get(0).charAt(0));
        assertEquals('\u256e', lines.get(0).charAt(4));
        assertEquals('\u2570', lines.get(2).charAt(0));
        assertEquals('\u256f', lines.get(2).charAt(4));
    }

    @Test
    void testBoxIsDirtyPropagation() {
        Text text = Text.builder().text("A").build();
        Box box = Box.builder().child(text).build();

        // Initially dirty
        assertTrue(box.isDirty());

        Canvas canvas = Canvas.create(10, 5);
        box.render(canvas, 10, 5);
        // After render, box is clean but child may be clean too
        assertFalse(box.isDirty());

        // Invalidate child should make box dirty
        text.invalidate();
        assertTrue(box.isDirty());
    }

    @Test
    void testSpinner() {
        Spinner spinner = Spinner.builder().label("Loading...").build();
        Size pref = spinner.getPreferredSize();
        assertTrue(pref.width() > 0);
        assertEquals(1, pref.height());

        Canvas canvas = Canvas.create(30, 1);
        spinner.render(canvas, 30, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("Loading..."));
    }

    @Test
    void testSpinnerAnimation() {
        Spinner spinner = Spinner.builder().build();
        // First tick should not change (frame 0)
        assertFalse(spinner.onTick(0));
        // Tick past the interval should advance frame
        assertTrue(spinner.onTick(spinner.getIntervalMs()));
    }

    @Test
    void testProgressBar() {
        ProgressBar bar = ProgressBar.builder().progress(0.5).width(20).build();
        Size pref = bar.getPreferredSize();
        assertEquals(25, pref.width()); // 20 bar + 5 percentage

        Canvas canvas = Canvas.create(30, 1);
        bar.render(canvas, 30, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("50%"));
    }

    @Test
    void testProgressBarBounds() {
        ProgressBar bar = ProgressBar.builder().build();
        bar.setProgress(-0.5);
        assertEquals(0.0, bar.getProgress());
        bar.setProgress(1.5);
        assertEquals(1.0, bar.getProgress());
    }

    @Test
    void testStatusMessage() {
        StatusMessage msg = StatusMessage.success("Done!");
        Size pref = msg.getPreferredSize();
        assertTrue(pref.width() > 5);
        assertEquals(1, pref.height());

        Canvas canvas = Canvas.create(30, 1);
        msg.render(canvas, 30, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("Done!"));
    }

    @Test
    void testGradient() {
        Gradient gradient = Gradient.builder()
                .text("Rainbow")
                .colors(new int[] {255, 0, 0}, new int[] {0, 0, 255})
                .build();

        assertEquals(new Size(7, 1), gradient.getPreferredSize());

        Canvas canvas = Canvas.create(10, 1);
        gradient.render(canvas, 10, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.startsWith("Rainbow"));
    }

    @Test
    void testComposition() {
        // Build a composed layout: bordered box with text, separator, progress bar
        Box root = Box.builder()
                .direction(FlexDirection.COLUMN)
                .borderStyle(Box.BorderStyle.ROUNDED)
                .padding(Insets.of(0, 1))
                .child(Text.builder().text("Status").style(AttributedStyle.BOLD).build())
                .child(Separator.builder().build())
                .child(ProgressBar.builder().progress(0.75).width(15).build())
                .build();

        Canvas canvas = Canvas.create(25, 6);
        root.render(canvas, 25, 6);
        List<AttributedString> lines = canvas.toLines();

        // Should have rendered without exceptions
        assertEquals(6, lines.size());
        // Top border
        assertEquals('\u256d', lines.get(0).charAt(0));
        // Content should be within the box
        assertTrue(lines.get(1).toString().contains("Status"));
    }

    @Test
    void testBoxBorderWidth1() {
        // Box with border but only 1 char wide — only left border char fits
        Box box = Box.builder()
                .borderStyle(Box.BorderStyle.SINGLE)
                .child(Text.builder().text("X").build())
                .build();

        Canvas canvas = Canvas.create(1, 3);
        box.render(canvas, 1, 3);
        // Should not throw, border partially renders
        List<AttributedString> lines = canvas.toLines();
        assertEquals(3, lines.size());
    }

    @Test
    void testBoxBorderWidth2Height2() {
        // Box 2x2 with border — border fills entire space, no room for content
        Box box = Box.builder()
                .borderStyle(Box.BorderStyle.SINGLE)
                .child(Text.builder().text("X").build())
                .build();

        Canvas canvas = Canvas.create(2, 2);
        box.render(canvas, 2, 2);
        List<AttributedString> lines = canvas.toLines();
        assertEquals(2, lines.size());
        // Top-left and top-right corners
        assertEquals('\u250c', lines.get(0).charAt(0));
        assertEquals('\u2510', lines.get(0).charAt(1));
    }

    @Test
    void testBoxBorderPaddingExceedsSpace() {
        // Border + padding larger than available space
        Box box = Box.builder()
                .borderStyle(Box.BorderStyle.SINGLE)
                .padding(Insets.of(5))
                .child(Text.builder().text("X").build())
                .build();

        Canvas canvas = Canvas.create(4, 4);
        box.render(canvas, 4, 4);
        // Should not throw — inner area is 0 after border+padding
        List<AttributedString> lines = canvas.toLines();
        assertEquals(4, lines.size());
    }

    @Test
    void testProgressBarSmallWidthWithPercentage() {
        ProgressBar bar = ProgressBar.builder()
                .progress(0.5)
                .width(3)
                .showPercentage(true)
                .build();

        Canvas canvas = Canvas.create(3, 1);
        bar.render(canvas, 3, 1);
        // With width=3, no room for percentage — should render bar only
        List<AttributedString> lines = canvas.toLines();
        assertEquals(1, lines.size());
    }

    @Test
    void testStatusMessagePreferredSizeWithIcon() {
        StatusMessage msg = StatusMessage.success("OK");
        Size pref = msg.getPreferredSize();
        // Icon width should be calculated via WCWidth, not String.length()
        // ✔ (U+2714) is typically 1 cell wide
        assertTrue(pref.width() >= 4); // icon + space + "OK"
    }

    @Test
    void testSpinnerLabelWidth() {
        Spinner spinner =
                Spinner.builder().frames(SpinnerFrames.DOTS).label("Loading").build();
        Size pref = spinner.getPreferredSize();
        // frames.maxWidth() + 1 (space) + label display width
        assertTrue(pref.width() >= 8); // at least spinner + space + "Loading"
    }

    @Test
    void testSpinnerFramesMaxWidthConsistency() {
        // Verify all spinner frame sets report positive maxWidth
        for (SpinnerFrames sf : SpinnerFrames.values()) {
            int maxWidth = sf.maxWidth();
            assertTrue(maxWidth > 0, sf.name() + " should have maxWidth > 0");
        }
    }

    @Test
    void testConvenienceFactory() {
        Text t = Components.text("hello");
        assertEquals("hello", t.getText());

        Separator sep = Components.separator("title");
        assertNotNull(sep);

        StatusMessage msg = Components.success("ok");
        assertNotNull(msg);

        Box vbox = Components.vbox(t, sep);
        assertEquals(2, vbox.getChildren().size());
    }
}
