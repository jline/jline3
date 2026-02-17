/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;

import org.jline.components.ComponentRenderer;
import org.jline.components.animation.SpinnerFrames;
import org.jline.components.layout.FlexDirection;
import org.jline.components.layout.Insets;
import org.jline.components.ui.Box;
import org.jline.components.ui.Gradient;
import org.jline.components.ui.Hyperlink;
import org.jline.components.ui.IndeterminateProgressBar;
import org.jline.components.ui.ProgressBar;
import org.jline.components.ui.Separator;
import org.jline.components.ui.Spinner;
import org.jline.components.ui.StatusMessage;
import org.jline.components.ui.Text;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Demonstrates the jline-components module with composable UI components.
 *
 * <p>This example showcases all available components across multiple phases:
 * <ol>
 *   <li>Indeterminate loading with gradient sweep bar</li>
 *   <li>Multiple spinner styles running in parallel</li>
 *   <li>Determinate progress bars with gradient fills</li>
 *   <li>Final results with status messages and hyperlinks</li>
 * </ol>
 *
 * <p>Run with: {@code java org.jline.demo.examples.ComponentsExample}
 */
public class ComponentsExample {

    @SuppressWarnings("BusyWait")
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            ComponentRenderer renderer = ComponentRenderer.create(terminal);

            // --- Phase 1: Indeterminate loading ---
            Gradient title = Gradient.builder()
                    .text("JLine Widgets Demo")
                    .baseColor(59, 130, 246)
                    .highlightColor(219, 234, 254)
                    .glowWidth(3)
                    .animate(true)
                    .cycleDuration(3000)
                    .build();

            IndeterminateProgressBar sweepBar = IndeterminateProgressBar.builder()
                    .width(50)
                    .glowRadius(12)
                    .cycleDuration(2000)
                    .trackColor(15, 23, 42)
                    .glowColor(96, 165, 250)
                    .build();

            Text loadingText = Text.builder()
                    .text("Initializing build system...")
                    .style(AttributedStyle.DEFAULT.foreground(148, 163, 184))
                    .build();

            Box phase1 = Box.builder()
                    .direction(FlexDirection.COLUMN)
                    .borderStyle(Box.BorderStyle.ROUNDED)
                    .padding(Insets.of(0, 1))
                    .gap(0)
                    .child(sweepBar)
                    .child(title)
                    .child(Separator.builder().build())
                    .child(loadingText)
                    .build();

            renderer.setRoot(phase1);
            renderer.startAnimations();

            for (int i = 0; i < 40; i++) {
                renderer.render();
                Thread.sleep(50);
            }

            // --- Phase 2: Multiple spinners with different styles ---
            Spinner dotsSpinner = Spinner.builder()
                    .frames(SpinnerFrames.DOTS)
                    .label("Scanning source files...")
                    .build();
            Spinner arcSpinner = Spinner.builder()
                    .frames(SpinnerFrames.ARC)
                    .label("Resolving dependencies...")
                    .build();
            Spinner triangleSpinner = Spinner.builder()
                    .frames(SpinnerFrames.TRIANGLE)
                    .label("Checking cache...")
                    .build();

            Box phase2 = Box.builder()
                    .direction(FlexDirection.COLUMN)
                    .borderStyle(Box.BorderStyle.ROUNDED)
                    .padding(Insets.of(0, 1))
                    .gap(0)
                    .child(sweepBar)
                    .child(title)
                    .child(Separator.builder().title("Tasks").build())
                    .child(dotsSpinner)
                    .child(arcSpinner)
                    .child(triangleSpinner)
                    .build();

            renderer.setRoot(phase2);

            for (int i = 0; i < 30; i++) {
                renderer.render();
                Thread.sleep(50);
            }

            // Update spinner labels as tasks complete
            dotsSpinner.setLabel("Scanning source files... 127 files found");
            renderer.render();
            Thread.sleep(300);

            arcSpinner.setLabel("Resolving dependencies... 34 resolved");
            renderer.render();
            Thread.sleep(300);

            triangleSpinner.setLabel("Checking cache... 18 hits, 3 misses");
            renderer.render();
            Thread.sleep(300);

            // --- Phase 3: Progress bars ---
            ProgressBar compileBar = ProgressBar.builder()
                    .progress(0.0)
                    .width(40)
                    .filledGradient(15, 23, 42, 96, 165, 250)
                    .build();

            ProgressBar testBar = ProgressBar.builder()
                    .progress(0.0)
                    .width(40)
                    .filledGradient(15, 23, 42, 34, 197, 94)
                    .build();

            ProgressBar lintBar = ProgressBar.builder()
                    .progress(0.0)
                    .width(40)
                    .filledGradient(15, 23, 42, 168, 85, 247)
                    .build();

            Spinner activeSpinner = Spinner.builder()
                    .frames(SpinnerFrames.DOTS)
                    .label("Compiling...")
                    .build();

            // Gradient labels for each bar
            Text compileLabel = Text.builder()
                    .text("  Compile")
                    .style(AttributedStyle.DEFAULT.foreground(96, 165, 250))
                    .build();
            Text testLabel = Text.builder()
                    .text("  Tests")
                    .style(AttributedStyle.DEFAULT.foreground(34, 197, 94))
                    .build();
            Text lintLabel = Text.builder()
                    .text("  Lint")
                    .style(AttributedStyle.DEFAULT.foreground(168, 85, 247))
                    .build();

            Box phase3 = Box.builder()
                    .direction(FlexDirection.COLUMN)
                    .borderStyle(Box.BorderStyle.ROUNDED)
                    .padding(Insets.of(0, 1))
                    .gap(0)
                    .child(sweepBar)
                    .child(title)
                    .child(Separator.builder().title("Build Progress").build())
                    .child(activeSpinner)
                    .child(Text.builder().text(" ").build())
                    .child(compileLabel)
                    .child(compileBar)
                    .child(testLabel)
                    .child(testBar)
                    .child(lintLabel)
                    .child(lintBar)
                    .build();

            renderer.setRoot(phase3);

            // Animate compile progress
            for (int i = 0; i <= 100; i += 3) {
                compileBar.setProgress(i / 100.0);
                if (i < 50) {
                    activeSpinner.setLabel("Compiling... " + i + "%");
                } else {
                    activeSpinner.setLabel("Compiling... " + i + "% (optimizing)");
                }
                renderer.render();
                Thread.sleep(30);
            }
            compileBar.setProgress(1.0);

            // Animate test progress
            activeSpinner.setLabel("Running tests...");
            for (int i = 0; i <= 100; i += 4) {
                testBar.setProgress(i / 100.0);
                activeSpinner.setLabel("Running tests... " + (i * 42 / 100) + "/42 passed");
                renderer.render();
                Thread.sleep(25);
            }
            testBar.setProgress(1.0);

            // Animate lint progress
            activeSpinner.setLabel("Linting...");
            for (int i = 0; i <= 100; i += 5) {
                lintBar.setProgress(i / 100.0);
                activeSpinner.setLabel("Linting... " + (i * 127 / 100) + "/127 files");
                renderer.render();
                Thread.sleep(20);
            }
            lintBar.setProgress(1.0);
            renderer.render();
            Thread.sleep(300);

            // --- Phase 4: Results ---
            renderer.stopAnimations();

            Gradient resultTitle = Gradient.builder()
                    .text("Build Complete")
                    .colors(new int[] {34, 197, 94}, new int[] {16, 185, 129})
                    .build();

            Text timingText = Text.builder()
                    .text("Completed in 4.2s")
                    .style(AttributedStyle.DEFAULT.foreground(148, 163, 184))
                    .build();

            Hyperlink docsLink = Hyperlink.builder()
                    .url("https://github.com/jline/jline3")
                    .text("https://github.com/jline/jline3")
                    .style(AttributedStyle.DEFAULT.foreground(96, 165, 250).underline())
                    .build();

            Box resultsBox = Box.builder()
                    .direction(FlexDirection.COLUMN)
                    .borderStyle(Box.BorderStyle.DOUBLE)
                    .borderColor(AttributedStyle.DEFAULT.foreground(34, 197, 94))
                    .padding(Insets.of(0, 1))
                    .gap(0)
                    .child(resultTitle)
                    .child(Separator.builder().title("Summary").build())
                    .child(StatusMessage.success("127 files compiled"))
                    .child(StatusMessage.success("42 tests passed"))
                    .child(StatusMessage.warning("2 deprecation warnings"))
                    .child(StatusMessage.info("Code coverage: 94.3%"))
                    .child(Text.builder().text(" ").build())
                    .child(Separator.builder().title("Output").build())
                    .child(Text.builder()
                            .text("  Artifacts written to target/")
                            .style(AttributedStyle.DEFAULT.foreground(148, 163, 184))
                            .build())
                    .child(Text.builder()
                            .text("  Report at target/reports/index.html")
                            .style(AttributedStyle.DEFAULT.foreground(148, 163, 184))
                            .build())
                    .child(Text.builder().text(" ").build())
                    .child(Separator.builder().build())
                    .child(Box.builder()
                            .direction(FlexDirection.ROW)
                            .gap(2)
                            .child(timingText)
                            .child(docsLink)
                            .build())
                    .build();

            renderer.setRoot(resultsBox);
            renderer.render();

            Thread.sleep(3000);

            renderer.close();

            terminal.writer().println();
            terminal.writer().println("Demo complete!");
            terminal.flush();
        } finally {
            terminal.close();
        }
    }
}
