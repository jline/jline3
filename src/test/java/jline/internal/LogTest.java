/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.internal;

import jline.internal.Log.Level;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link Log}.
 */
public class LogTest
{
    private ByteArrayOutputStream buff;

    private PrintStream out;

    @Before
    public void setUp() throws Exception {
        buff = new ByteArrayOutputStream();
        out = new PrintStream(buff);
        Log.setOutput(out);
    }

    @Test
    public void renderArray() {
        Log.render(out, new Object[]{"a", 1, "2", "b"});
        assertEquals("[a,1,2,b]", buff.toString());
    }

    @Test
    public void renderArrayWithThrowable() {
        Log.render(out, new Object[]{"a", 1, "2", "b", new Throwable("TEST")});
        assertEquals("[a,1,2,b,java.lang.Throwable: TEST]", buff.toString());
    }

    @Test
    public void renderThrowable() {
        Log.render(out, new Throwable("TEST"));
        System.out.println(buff);
    }

    @Test
    public void logSimple() {
        Log.log(Level.DEBUG, "a", 1, "2", "b");
        System.out.println(buff);
    }

    @Test
    public void logWithThrowable() {
        Log.log(Level.DEBUG, "a", 1, "2", "b", new Throwable("TEST"));
        System.out.println(buff);
    }
}