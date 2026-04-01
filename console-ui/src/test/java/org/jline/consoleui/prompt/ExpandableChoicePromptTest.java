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

import org.jline.consoleui.elements.ExpandableChoice;
import org.jline.consoleui.elements.items.ChoiceItemIF;
import org.jline.consoleui.elements.items.impl.ChoiceItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpandableChoicePromptTest {

    @Test
    void testExpandableChoiceCreation() {
        List<ChoiceItemIF> choices = new ArrayList<>();
        choices.add(new ChoiceItem('a', "actionA", "Action A", false));
        choices.add(new ChoiceItem('b', "actionB", "Action B", true));

        ExpandableChoice expandableChoice = new ExpandableChoice("Choose an action", "action", choices);

        assertEquals("Choose an action", expandableChoice.getMessage());
        assertEquals("action", expandableChoice.getName());
        assertEquals(2, expandableChoice.getChoiceItems().size());
    }
}
