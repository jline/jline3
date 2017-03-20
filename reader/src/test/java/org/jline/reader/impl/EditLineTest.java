/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.junit.Test;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.reader.LineReader.BACKWARD_KILL_LINE;
import static org.jline.reader.LineReader.BACKWARD_KILL_WORD;
import static org.jline.reader.LineReader.BACKWARD_WORD;
import static org.jline.reader.LineReader.END_OF_LINE;
import static org.jline.reader.LineReader.FORWARD_WORD;
import static org.jline.reader.LineReader.KILL_WORD;

/**
 * Tests various features of editing lines.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class EditLineTest
    extends ReaderTestSupport
{
    @Test
    public void testIssue101() throws Exception {
        TestBuffer b = new TestBuffer("config:property-set --pid org.ops4j.pax.url.mvn org.ops4j.pax.url.mvn.globalChecksumPolicy crash")
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append("odsa odsa ")
                .op(BACKWARD_WORD)
                .op(BACKWARD_KILL_WORD)
                .op(FORWARD_WORD)
                .op(FORWARD_WORD)
                .op(BACKWARD_KILL_WORD);
        assertBuffer("config:property-set --pid org.ops4j.pax.url.mvn odsa crash", b);
    }

    @Test
    public void testDeletePreviousWord() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");

        assertBuffer("This is a ", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This is ", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This ", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("", b = b.op(BACKWARD_KILL_WORD));
    }

    @Test
    public void testDeleteNextWord() throws Exception {
        TestBuffer b = new TestBuffer("This is a test").op(END_OF_LINE);

        assertBuffer("This is a test", b = b.op(KILL_WORD));
        assertBuffer("This is a ", b = b.op(BACKWARD_WORD).op(KILL_WORD));
    }

    @Test
    public void testMoveToEnd() throws Exception {
        assertBuffer("This is a XtestX",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .append('X')
                .op(END_OF_LINE)
                .append('X'));

        assertBuffer("This is Xa testX",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append('X')
                .op(END_OF_LINE)
                .append('X'));

        assertBuffer("This Xis a testX",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append('X')
                .op(END_OF_LINE)
                .append('X'));
    }

    @Test
    public void testPreviousWord() throws Exception {
        assertBuffer("This is a Xtest",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .append('X'));
        assertBuffer("This is Xa test",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append('X'));
        assertBuffer("This Xis a test",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append('X'));
        assertBuffer("XThis is a test",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append('X'));
        assertBuffer("XThis is a test",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append('X'));
        assertBuffer("XThis is a test",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .op(BACKWARD_WORD)
                .append('X'));
    }

    @Test
    public void testLineStart() throws Exception {
        assertBuffer("XThis is a test",
            new TestBuffer("This is a test").ctrlA().append('X'));
        assertBuffer("TXhis is a test",
            new TestBuffer("This is a test").ctrlA().right().append('X'));
    }

    @Test
    public void testClearLine() throws Exception {
        reader.getKeys().bind(new Reference(BACKWARD_KILL_LINE), ctrl('U'));
        assertBuffer("", new TestBuffer("This is a test").ctrlU());
        assertBuffer("t", new TestBuffer("This is a test").left().ctrlU());
        assertBuffer("st", new TestBuffer("This is a test").left().left().ctrlU());
    }

    @Test
    public void testRight() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        b = b.left().right().back();
        assertBuffer("This is a tes", b);
        b = b.left().left().left().right().left().back();
        assertBuffer("This is ates", b);
        b.append('X');
        assertBuffer("This is aXtes", b);
    }

    @Test
    public void testLeft() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        b = b.left().left().left();
        assertBuffer("This is a est", b = b.back());
        assertBuffer("This is aest", b = b.back());
        assertBuffer("This is est", b = b.back());
        assertBuffer("This isest", b = b.back());
        assertBuffer("This iest", b = b.back());
        assertBuffer("This est", b = b.back());
        assertBuffer("Thisest", b = b.back());
        assertBuffer("Thiest", b = b.back());
        assertBuffer("Thest", b = b.back());
        assertBuffer("Test", b = b.back());
        assertBuffer("est", b = b.back());
        assertBuffer("est", b = b.back());
        assertBuffer("est", b = b.back());
        assertBuffer("est", b = b.back());
        assertBuffer("est", b = b.back());
    }

    @Test
    public void testBackspace() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        assertBuffer("This is a tes", b = b.back());
        assertBuffer("This is a te", b = b.back());
        assertBuffer("This is a t", b = b.back());
        assertBuffer("This is a ", b = b.back());
        assertBuffer("This is a", b = b.back());
        assertBuffer("This is ", b = b.back());
        assertBuffer("This is", b = b.back());
        assertBuffer("This i", b = b.back());
        assertBuffer("This ", b = b.back());
        assertBuffer("This", b = b.back());
        assertBuffer("Thi", b = b.back());
        assertBuffer("Th", b = b.back());
        assertBuffer("T", b = b.back());
        assertBuffer("", b = b.back());
        assertBuffer("", b = b.back());
        assertBuffer("", b = b.back());
        assertBuffer("", b = b.back());
        assertBuffer("", b = b.back());
    }

    @Test
    public void testBuffer() throws Exception {
        assertBuffer("This is a test", new TestBuffer("This is a test"));
    }

    @Test
    public void testAbortPartialBuffer() throws Exception {
        reader.setVariable(LineReader.BELL_STYLE, "audible");
        assertBuffer("", new TestBuffer("This is a test").ctrl('G'));
        assertConsoleOutputContains("\n");
        assertBeeped();

        out.reset();

        assertBuffer("",
            new TestBuffer("This is a test").op(BACKWARD_WORD)
                                        .op(BACKWARD_WORD)
                                        .ctrl('G'));
        assertConsoleOutputContains("\n");
        assertBeeped();
    }

    @Test
    public void testEscapeNewLine() throws Exception {
        boolean prev = ((DefaultParser) reader.getParser()).isEofOnEscapedNewLine();
        ((DefaultParser) reader.getParser()).setEofOnEscapedNewLine(true);
        try {
            assertLine("echo foobar", new TestBuffer("echo foo\\").enter().append("bar").enter());
        } finally {
            ((DefaultParser) reader.getParser()).setEofOnEscapedNewLine(prev);
        }
    }
}
