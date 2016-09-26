/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.history;

import java.io.File;
import java.io.IOException;

import org.jline.reader.impl.history.FileHistory;
import org.junit.Test;

/**
 * Tests file history.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class FileHistoryTest {

    @Test
    public void testFileHistory() throws IOException {
        File file = new File("test");
        try {
            FileHistory history = new FileHistory(file);
            history.flush();
        } finally {
            file.delete();
        }
    }
}
