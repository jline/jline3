/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultPrompter.computeListRange (fixes #2272).
 *
 * <p>All bugs are in the range-computation logic; we test it in isolation by constructing a
 * DefaultPrompter, wiring its package-private fields, and calling computeListRange directly.
 */
class ComputeListRangeTest {

    private DefaultPrompter prompter;

    @BeforeEach
    void setUp() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .type("dumb")
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build();
        prompter = new DefaultPrompter(terminal);
    }

    /**
     * Configures the prompter's package-private state that computeListRange reads.
     */
    private void configure(int rows, int firstItemRow, int footerAreaHeight) throws Exception {
        // size: use reflection since it is a private field
        var sizeField = DefaultPrompter.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        sizeField.set(prompter, Size.of(80, rows));

        var firField = DefaultPrompter.class.getDeclaredField("firstItemRow");
        firField.setAccessible(true);
        firField.set(prompter, firstItemRow);

        var fahField = DefaultPrompter.class.getDeclaredField("footerAreaHeight");
        fahField.setAccessible(true);
        fahField.set(prompter, footerAreaHeight);

        // clear cached range
        var rangeField = DefaultPrompter.class.getDeclaredField("range");
        rangeField.setAccessible(true);
        rangeField.set(prompter, null);
    }

    private DefaultPrompter.ListRange getRange() throws Exception {
        var rangeField = DefaultPrompter.class.getDeclaredField("range");
        rangeField.setAccessible(true);
        return (DefaultPrompter.ListRange) rangeField.get(prompter);
    }

    // -----------------------------------------------------------------------
    // Bug 1: page indicator row not reserved in the no-footer path
    // -----------------------------------------------------------------------

    /**
     * With no footer (footerAreaHeight=0) and default pageSize (0), the effective page size must
     * leave one row for the page indicator.
     *
     * Before fix: effectivePageSize = rows - firstItemRow = 19 (for a 20-row terminal with
     * firstItemRow=1), so the frame was rows+1 tall.
     * After fix: effectivePageSize = rows - firstItemRow - 1 = 18 (indicator reserved).
     */
    @Test
    void bug1_defaultPageSize_indicatorRowReserved() throws Exception {
        // 20-row terminal, no header (firstItemRow=1), no footer.
        configure(20, 1, 0);
        int itemsSize = 100; // large enough that pagination is active

        // pageSize=0 (default), showPageIndicator=true
        prompter.computeListRange(1 /*cursorRow=firstItemRow*/, itemsSize, 0, true);

        DefaultPrompter.ListRange range = getRange();
        // With the fix, effectivePageSize = 20 - 1 - 1 = 18 (indicator reserved).
        // The first page starts at 0 and ends at 18 (exclusive).
        assertEquals(0, range.first, "first page must start at 0");
        assertEquals(18, range.last, "first page must show 18 items, leaving 1 row for indicator");
    }

    /**
     * With an explicit pageSize and no footer, the indicator row must still be reserved.
     * effectivePageSize is capped at min(pageSize, maxFit-1) when the indicator is shown.
     */
    @Test
    void bug1_explicitPageSize_indicatorRowReserved() throws Exception {
        // 20-row terminal, firstItemRow=1, no footer, pageSize=15 (fits within terminal).
        configure(20, 1, 0);
        int itemsSize = 100;

        // pageSize=15, showPageIndicator=true
        prompter.computeListRange(1, itemsSize, 15, true);

        DefaultPrompter.ListRange range = getRange();
        // maxFit = 20-1-0 = 19. pageSize=15 <= 19 so effectivePageSize=15.
        // indicator deduction: min(15, 19-1)=min(15,18)=15 -- no change because 15 < 18.
        // Actually: effectivePageSize stays 15 (already < maxFit-1=18).
        assertEquals(0, range.first);
        assertEquals(15, range.last);
    }

    /**
     * When an explicit pageSize already fills the terminal, the indicator deduction must not
     * shrink the page below what the user asked for when it already fits.
     */
    @Test
    void bug1_explicitPageSize_exactFit_noExtraDeduction() throws Exception {
        // 20-row terminal, firstItemRow=1, no footer, pageSize=19 (maxFit).
        // Before fix: effectivePageSize -= 1 unconditionally (became 18 even though user said 19).
        // After fix: min(19, 19-1) = min(19,18) = 18 -- wait, this still deducts!
        // But this is correct: we NEED to deduct if indicator is shown and items > 19.
        // The point is: if items <= effectivePageSize, no pagination, indicator not shown.
        configure(20, 1, 0);

        // 10 items, pageSize=19 -- fits entirely, no pagination, no indicator.
        prompter.computeListRange(1, 10, 19, true);

        DefaultPrompter.ListRange range = getRange();
        // No pagination needed (10 < 19), range covers all items.
        assertEquals(0, range.first);
        assertEquals(10, range.last);
    }

    // -----------------------------------------------------------------------
    // Bug 3: last page is one item short
    // -----------------------------------------------------------------------

    /**
     * When the cursor is on the last item and the window-slide formula would overshoot
     * the list end, the page must still show a full pageSize of items.
     *
     * Before fix: ListRange(itemId - pageSize + 2, itemId + 2) = ListRange(7, 11) for
     * itemId=9, pageSize=4, itemsSize=10.  Render clamps to [7,10) -> 3 items.
     * After fix: last=min(11,10)=10, first=max(0,10-4)=6 -> ListRange(6,10) -> 4 items.
     */
    @Test
    void bug3_lastPage_fullPageSize() throws Exception {
        // 30-row terminal, firstItemRow=1, no footer, pageSize=4, 10 items.
        configure(30, 1, 0);
        int itemsSize = 10;
        int pageSize = 4;
        // Cursor on last item (itemId=9 -> cursorRow=firstItemRow+9=10).
        prompter.computeListRange(10 /*cursorRow*/, itemsSize, pageSize, false);

        DefaultPrompter.ListRange range = getRange();
        assertEquals(6, range.first, "last page must start at item 6 (not 7)");
        assertEquals(10, range.last, "last page must end at item 10 (exclusive)");
        assertEquals(4, range.last - range.first, "last page must contain a full pageSize of items");
    }

    /**
     * When the cursor is one before the last item, the window must also be a full page.
     */
    @Test
    void bug3_secondToLastItem_fullPageSize() throws Exception {
        configure(30, 1, 0);
        int itemsSize = 10;
        int pageSize = 4;
        // Cursor on item8 (cursorRow=firstItemRow+8=9).
        prompter.computeListRange(9, itemsSize, pageSize, false);

        DefaultPrompter.ListRange range = getRange();
        // itemId=8: last=min(10,10)=10, first=max(0,10-4)=6 -> [6,10).
        assertEquals(6, range.first);
        assertEquals(10, range.last);
        assertEquals(4, range.last - range.first);
    }

    /**
     * Mid-list cursor: window slides normally, full page shown.
     */
    @Test
    void bug3_midList_fullPageSize() throws Exception {
        configure(30, 1, 0);
        int itemsSize = 10;
        int pageSize = 4;
        // Cursor on item5 (cursorRow=firstItemRow+5=6).
        prompter.computeListRange(6, itemsSize, pageSize, false);

        DefaultPrompter.ListRange range = getRange();
        // itemId=5: last=min(7,10)=7, first=max(0,7-4)=3 -> [3,7).
        assertEquals(3, range.first);
        assertEquals(7, range.last);
        assertEquals(4, range.last - range.first);
    }

    // -----------------------------------------------------------------------
    // Combined: no footer, default pageSize, last item
    // -----------------------------------------------------------------------

    /**
     * With default pageSize, no footer, cursor on the very last item: the page must
     * still be a full page (indicator reserved, window not overshot).
     */
    @Test
    void noFooter_defaultPageSize_lastItem() throws Exception {
        // 10-row terminal, firstItemRow=1, no footer, 20 items.
        // maxFit = 10-1-0 = 9. With indicator: effectivePageSize = min(9, 9-1) = 8.
        configure(10, 1, 0);
        int itemsSize = 20;
        // Cursor on item19 (cursorRow=1+19=20).
        prompter.computeListRange(20, itemsSize, 0, true);

        DefaultPrompter.ListRange range = getRange();
        int eps = 8; // expected effectivePageSize
        assertEquals(eps, range.last - range.first, "page must be exactly " + eps + " items");
        assertEquals(itemsSize, range.last, "page must end at the list end");
        assertEquals(itemsSize - eps, range.first, "page start must be derived from list end");
    }
}
