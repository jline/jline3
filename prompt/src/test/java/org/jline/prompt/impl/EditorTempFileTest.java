/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.prompt.EditorPrompt;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The editor prompt writes its text to a temp file for Nano. The text can hold
 * secrets, so the file must not be readable by other users while it is being edited.
 */
class EditorTempFileTest {

    @Test
    void launchEditorTempFileIsOwnerOnly() throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));

        Terminal terminal = TerminalBuilder.builder()
                .type("dumb")
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build();
        AtomicReference<String> name = new AtomicReference<>();
        AtomicReference<Set<PosixFilePermission>> seen = new AtomicReference<>();
        DefaultPrompter prompter = new DefaultPrompter(terminal) {
            @Override
            protected File createEditorTempFile(String prefix, String suffix) throws IOException {
                File file = super.createEditorTempFile(prefix, suffix);
                name.set(file.getName());
                seen.set(Files.getPosixFilePermissions(file.toPath()));
                Files.delete(file.toPath());
                // stop before Nano is started
                throw new IOException("captured");
            }
        };
        EditorPrompt prompt = new DefaultEditorPrompt("editor", "Edit", "password=secret");

        Method launchEditor = DefaultPrompter.class.getDeclaredMethod("launchEditor", EditorPrompt.class);
        launchEditor.setAccessible(true);
        InvocationTargetException e =
                assertThrows(InvocationTargetException.class, () -> launchEditor.invoke(prompter, prompt));
        assertEquals("captured", e.getCause().getMessage());

        assertTrue(name.get().startsWith("jline_editor_"));
        assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), seen.get());
    }
}
