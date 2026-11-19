/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OSUtilsTest {

    @TempDir
    Path dir;

    private static String path(Path... dirs) {
        StringBuilder sb = new StringBuilder();
        for (Path d : dirs) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(d.toAbsolutePath());
        }
        return sb.toString();
    }

    private static Path executable(Path dir, String name) throws IOException {
        Path f = Files.createFile(dir.resolve(name));
        f.toFile().setExecutable(true);
        return f;
    }

    @Test
    void windowsCommandMissingFromPathIsNotResolvedByBareName() throws IOException {
        Path empty = Files.createDirectory(dir.resolve("empty"));
        // A bare "infocmp" would let CreateProcess pick infocmp.exe from the working directory.
        assertNull(OSUtils.findCommand("infocmp", path(empty), true));
        assertNull(OSUtils.findCommand("tty", null, true));
    }

    @Test
    void windowsCommandOnPathResolvesToExe() throws IOException {
        Path empty = Files.createDirectory(dir.resolve("empty"));
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path infocmp = executable(bin, "infocmp.exe");
        assertEquals(infocmp.toFile().getAbsolutePath(), OSUtils.findCommand("infocmp", path(empty, bin), true));
    }

    @Test
    void unixCommandResolution() throws IOException {
        Path bin = Files.createDirectory(dir.resolve("bin"));
        Path stty = executable(bin, "stty");
        assertEquals(stty.toFile().getAbsolutePath(), OSUtils.findCommand("stty", path(bin), false));
        // Not on PATH: the bare name is left for the JDK, which only searches PATH on Unix.
        assertEquals("infocmp", OSUtils.findCommand("infocmp", path(bin), false));
    }
}
