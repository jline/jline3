/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The edit-and-execute widget writes the current line buffer to a temp file for
 * the editor. The buffer can hold secrets typed on the command line, so the file
 * must not be readable by other users while it is being edited.
 */
class EditAndExecuteTempFileTest {

    @Test
    void tempFileIsOwnerOnly() throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));

        Terminal terminal = new DumbTerminal(
                "dumb",
                "dumb",
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);
        AtomicReference<Set<PosixFilePermission>> seen = new AtomicReference<>();
        AtomicReference<String> content = new AtomicReference<>();
        LineReaderImpl reader = new LineReaderImpl(terminal) {
            @Override
            public void editAndAddInBuffer(Path file) throws Exception {
                seen.set(Files.getPosixFilePermissions(file));
                content.set(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            }
        };
        reader.getBuffer().write("mysql -u root -psecret");

        assertTrue(reader.editAndExecute());
        assertEquals("mysql -u root -psecret", content.get());
        assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), seen.get());
    }
}
