/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AttributedCharSequenceTest {

    @Test
    public void testBoldOnWindows() throws IOException {
        String HIC = "\33[36;1m";
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.appendAnsi(HIC);
        sb.append("the buffer");
        //sb.append(NOR);
        AttributedString as = sb.toAttributedString();

        assertEquals("\33[1;36mthe buffer\33[0m", as.toAnsi(null));
    }

}
