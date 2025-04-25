/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements;

import java.util.List;

import org.jline.utils.AttributedString;

public class Text extends AbstractPromptableElement {
    private final List<AttributedString> lines;

    private static int num = 0;

    public Text(List<AttributedString> lines) {
        // We don't actually care about names, so we just generate a unique one
        super("", "_text_" + ++num);
        this.lines = lines;
    }

    public List<AttributedString> getLines() {
        return lines;
    }
}
