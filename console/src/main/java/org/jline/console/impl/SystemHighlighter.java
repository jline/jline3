/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import org.jline.builtins.Nano.SyntaxHighlighter;
import org.jline.console.SystemRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.regex.Pattern;

/**
 * Highlight command and language syntax using nanorc highlighter.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SystemHighlighter extends DefaultHighlighter {
    private final SyntaxHighlighter commandHighlighter;
    private final SyntaxHighlighter argsHighlighter;
    private final SyntaxHighlighter langHighlighter;
    private final SystemRegistry systemRegistry;
    private Pattern errorPattern;
    private int errorIndex = -1;

    public SystemHighlighter(SyntaxHighlighter commandHighlighter, SyntaxHighlighter argsHighlighter
            , SyntaxHighlighter langHighlighter) {
        this.commandHighlighter = commandHighlighter;
        this.argsHighlighter = argsHighlighter;
        this.langHighlighter = langHighlighter;
        this.systemRegistry = SystemRegistry.get();
    }

    @Override
    public void setErrorPattern(Pattern errorPattern) {
        this.errorPattern = errorPattern;
        super.setErrorPattern(errorPattern);
    }

    @Override
    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
        super.setErrorIndex(errorIndex);
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        return doDefaultHighlight(reader) ? super.highlight(reader, buffer) : systemHighlight(reader.getParser(), buffer);
    }

    private boolean doDefaultHighlight(LineReader reader) {
        String search = reader.getSearchTerm();
        return ((search != null && search.length() > 0) || reader.getRegionActive() != LineReader.RegionType.NONE
                || errorIndex > -1 || errorPattern != null);
    }

    private AttributedString systemHighlight(Parser parser, String buffer) {
        AttributedString out;
        String command = parser.getCommand(buffer.trim().split("\\s+")[0]);
        if (buffer.trim().isEmpty()) {
            out = new AttributedStringBuilder().append(buffer).toAttributedString();
        } else if (systemRegistry.isCommandOrScript(command) || systemRegistry.isCommandAlias(command)) {
            if (commandHighlighter != null || argsHighlighter != null) {
                int idx = -1;
                boolean cmdFound = false;
                for (int i = 0; i < buffer.length(); i++) {
                    char c = buffer.charAt(i);
                    if (c != ' ') {
                        cmdFound = true;
                    } else if (cmdFound) {
                        idx = i;
                        break;
                    }
                }
                AttributedStringBuilder asb = new AttributedStringBuilder();
                if (idx < 0) {
                    highlightCommand(buffer, asb);
                } else {
                    highlightCommand(buffer.substring(0, idx), asb);
                    if (argsHighlighter != null) {
                        asb.append(argsHighlighter.highlight(buffer.substring(idx)));
                    } else {
                        asb.append(buffer.substring(idx));
                    }
                }
                out = asb.toAttributedString();
            } else {
                out = new AttributedStringBuilder().append(buffer).toAttributedString();
            }
        } else if (langHighlighter != null) {
            out = langHighlighter.highlight(buffer);
        } else {
            out = new AttributedStringBuilder().append(buffer).toAttributedString();
        }
        return out;
    }

    private void highlightCommand(String command, AttributedStringBuilder asb) {
        if (commandHighlighter != null) {
            asb.append(commandHighlighter.highlight(command));
        } else {
            asb.append(command);
        }
    }
}
