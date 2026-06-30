/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JLineNativeLoaderTest {

    @Test
    public void testLoadLibrary() {
        JLineNativeLoader.initialize();
    }

    @Test
    void exclusiveStreamRefusesExistingTarget(@TempDir Path dir) throws Exception {
        // A file already sitting at the target path stands in for a symlink an attacker
        // planted in the shared temp directory: the open must fail instead of writing through it.
        File planted = dir.resolve("jlinenative-planted").toFile();
        assertTrue(planted.createNewFile());
        assertThrows(
                FileAlreadyExistsException.class,
                () -> JLineNativeLoader.newExclusiveStream(planted).close());

        // A fresh path is created normally.
        File fresh = dir.resolve("jlinenative-fresh").toFile();
        try (OutputStream out = JLineNativeLoader.newExclusiveStream(fresh)) {
            out.write(new byte[] {1, 2, 3});
        }
        assertTrue(Files.isRegularFile(fresh.toPath()));
    }
}
