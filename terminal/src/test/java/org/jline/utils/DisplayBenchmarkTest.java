/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Size;
import org.junit.jupiter.api.Test;

/**
 * Micro-benchmark for Display.update to measure throughput.
 * Run with: ./mvx mvn test -B -pl terminal -Dtest=DisplayBenchmarkTest
 */
class DisplayBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 2_000;
    private static final int MEASURED_ITERATIONS = 10_000;
    private static final int ROWS = 40;
    private static final int COLS = 120;

    @Test
    void benchmarkDisplayUpdate() throws IOException {
        try (DisplayTest.VirtualTerminal terminal =
                new DisplayTest.VirtualTerminal("bench", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            terminal.enterRawMode();
            Display display = new Display(terminal, true);
            display.resize(Size.of(COLS, ROWS));

            // Build alternating screen content
            List<AttributedString> screenA = buildScreen("A", ROWS, COLS);
            List<AttributedString> screenB = buildScreen("B", ROWS, COLS);
            // Screen with partial changes (more realistic)
            List<AttributedString> screenC = buildPartialChangeScreen(screenA, ROWS, COLS);

            // === Warmup ===
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                display.update(screenA, 0);
                display.update(screenB, 0);
            }

            // === Measure full-repaint scenario ===
            // Note: System.gc() is a hint, not a guarantee. Heap growth numbers
            // are approximate and should not be used for precise allocation claims.
            // Use JMH with -prof gc for accurate allocation measurement.
            System.gc();
            long memBefore =
                    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long start = System.nanoTime();

            for (int i = 0; i < MEASURED_ITERATIONS; i++) {
                display.update(screenA, 0);
                display.update(screenB, 0);
            }

            long elapsed = System.nanoTime() - start;
            long memAfter =
                    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            double msPerUpdate = (elapsed / 1_000_000.0) / (MEASURED_ITERATIONS * 2);

            System.out.printf("=== Full repaint (A↔B) ===%n");
            System.out.printf("  Iterations:    %,d update calls%n", MEASURED_ITERATIONS * 2);
            System.out.printf("  Total time:    %.1f ms%n", elapsed / 1_000_000.0);
            System.out.printf("  Per update:    %.3f ms%n", msPerUpdate);
            System.out.printf("  Updates/sec:   %,.0f%n", 1000.0 / msPerUpdate);
            System.out.printf("  Heap growth:   %,d bytes%n", memAfter - memBefore);

            // === Warmup partial changes ===
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                display.update(screenA, 0);
                display.update(screenC, 0);
            }

            // === Measure partial-change scenario ===
            System.gc();
            memBefore =
                    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            start = System.nanoTime();

            for (int i = 0; i < MEASURED_ITERATIONS; i++) {
                display.update(screenA, 0);
                display.update(screenC, 0);
            }

            elapsed = System.nanoTime() - start;
            memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            msPerUpdate = (elapsed / 1_000_000.0) / (MEASURED_ITERATIONS * 2);

            System.out.printf("%n=== Partial change (A↔C, ~25%% lines differ) ===%n");
            System.out.printf("  Iterations:    %,d update calls%n", MEASURED_ITERATIONS * 2);
            System.out.printf("  Total time:    %.1f ms%n", elapsed / 1_000_000.0);
            System.out.printf("  Per update:    %.3f ms%n", msPerUpdate);
            System.out.printf("  Updates/sec:   %,.0f%n", 1000.0 / msPerUpdate);
            System.out.printf("  Heap growth:   %,d bytes%n", memAfter - memBefore);

            // === Measure no-change scenario ===
            display.update(screenA, 0);

            System.gc();
            memBefore =
                    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            start = System.nanoTime();

            for (int i = 0; i < MEASURED_ITERATIONS; i++) {
                display.update(screenA, 0);
            }

            elapsed = System.nanoTime() - start;
            memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            msPerUpdate = (elapsed / 1_000_000.0) / MEASURED_ITERATIONS;

            System.out.printf("%n=== No change (A→A) ===%n");
            System.out.printf("  Iterations:    %,d update calls%n", MEASURED_ITERATIONS);
            System.out.printf("  Total time:    %.1f ms%n", elapsed / 1_000_000.0);
            System.out.printf("  Per update:    %.3f ms%n", msPerUpdate);
            System.out.printf("  Updates/sec:   %,.0f%n", 1000.0 / msPerUpdate);
            System.out.printf("  Heap growth:   %,d bytes%n", memAfter - memBefore);

            // Verify display is still in a valid state after benchmark
            org.junit.jupiter.api.Assertions.assertTrue(msPerUpdate < 10.0, "Update should complete in under 10ms");
        }
    }

    private List<AttributedString> buildScreen(String label, int rows, int cols) {
        List<AttributedString> lines = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.style(AttributedStyle.DEFAULT.foreground(i % 8));
            String text = String.format("%s-%03d: ", label, i);
            sb.append(text);
            sb.style(AttributedStyle.DEFAULT);
            // Fill rest of line with content
            StringBuilder filler = new StringBuilder();
            while (filler.length() + text.length() < cols) {
                filler.append("The quick brown fox jumps over the lazy dog. ");
            }
            sb.append(filler.substring(0, Math.max(0, cols - text.length())));
            sb.append('\n');
            lines.add(sb.toAttributedString());
        }
        return lines;
    }

    private List<AttributedString> buildPartialChangeScreen(List<AttributedString> base, int rows, int cols) {
        List<AttributedString> lines = new ArrayList<>(base);
        // Change ~25% of lines
        for (int i = 0; i < rows; i += 4) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.style(AttributedStyle.DEFAULT.foreground(2));
            String text = String.format("MOD-%03d: ", i);
            sb.append(text);
            sb.style(AttributedStyle.DEFAULT.bold());
            StringBuilder filler = new StringBuilder();
            while (filler.length() + text.length() < cols) {
                filler.append("Modified content here! ");
            }
            sb.append(filler.substring(0, Math.max(0, cols - text.length())));
            sb.append('\n');
            lines.set(i, sb.toAttributedString());
        }
        return lines;
    }
}
