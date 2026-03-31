/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

import java.util.ArrayList;
import java.util.List;

import org.jline.consoleui.elements.items.CheckboxItemIF;
import org.jline.consoleui.elements.items.impl.CheckboxItem;
import org.jline.consoleui.elements.items.impl.Separator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckboxPromptTest {

    @Test
    void renderSimpleList() {
        List<CheckboxItemIF> list = new ArrayList<>();

        CheckboxItem one = new CheckboxItem("One");
        list.add(one);
        CheckboxItem two = new CheckboxItem(true, "Two");
        list.add(two);
        CheckboxItem three = new CheckboxItem("Three");
        three.setDisabled("not available");
        list.add(three);
        list.add(new Separator("some extra items"));
        list.add(new CheckboxItem("Four"));
        list.add(new CheckboxItem(true, "Five"));

        assertEquals(6, list.size());
        assertFalse(one.isChecked());
        assertTrue(two.isChecked());
        assertTrue(three.isDisabled());
        assertEquals("not available", three.getDisabledText());
    }
}
