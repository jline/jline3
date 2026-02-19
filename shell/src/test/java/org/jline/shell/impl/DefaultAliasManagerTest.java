/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jline.shell.AliasManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultAliasManager}.
 */
public class DefaultAliasManagerTest {

    @Test
    void setGetRemoveAlias() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("ll", "ls -la");
        assertEquals("ls -la", mgr.getAlias("ll"));
        assertTrue(mgr.removeAlias("ll"));
        assertNull(mgr.getAlias("ll"));
        assertFalse(mgr.removeAlias("nonexistent"));
    }

    @Test
    void aliasesMap() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("a", "alpha");
        mgr.setAlias("b", "beta");
        assertEquals(2, mgr.aliases().size());
        assertEquals("alpha", mgr.aliases().get("a"));
    }

    @Test
    void expandSimpleAlias() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("ll", "ls -la");
        assertEquals("ls -la", mgr.expand("ll"));
        assertEquals("ls -la /tmp", mgr.expand("ll /tmp"));
    }

    @Test
    void expandNoAlias() {
        AliasManager mgr = new DefaultAliasManager();
        assertEquals("ls -la", mgr.expand("ls -la"));
    }

    @Test
    void expandParameterSubstitution() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("grp", "grep $1 $2");
        assertEquals("grep pattern file.txt", mgr.expand("grp pattern file.txt"));
    }

    @Test
    void expandAllParameters() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("e", "echo $@");
        assertEquals("echo hello world", mgr.expand("e hello world"));
    }

    @Test
    void expandRecursionGuard() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("a", "b");
        mgr.setAlias("b", "a");
        // Should not loop forever
        String result = mgr.expand("a");
        assertNotNull(result);
    }

    @Test
    void expandNullAndEmpty() {
        AliasManager mgr = new DefaultAliasManager();
        assertNull(mgr.expand(null));
        assertEquals("", mgr.expand(""));
        assertEquals("   ", mgr.expand("   "));
    }

    @Test
    void persistenceRoundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("aliases.txt");

        DefaultAliasManager mgr1 = new DefaultAliasManager(file);
        mgr1.setAlias("ll", "ls -la");
        mgr1.setAlias("gs", "git status");
        mgr1.save();

        assertTrue(Files.exists(file));

        DefaultAliasManager mgr2 = new DefaultAliasManager(file);
        mgr2.load();
        assertEquals("ls -la", mgr2.getAlias("ll"));
        assertEquals("git status", mgr2.getAlias("gs"));
    }

    @Test
    void loadFromNonexistentFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("nonexistent.txt");
        DefaultAliasManager mgr = new DefaultAliasManager(file);
        mgr.load(); // should not throw
        assertTrue(mgr.aliases().isEmpty());
    }

    @Test
    void overwriteAlias() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("ll", "ls -la");
        mgr.setAlias("ll", "ls -lah");
        assertEquals("ls -lah", mgr.getAlias("ll"));
    }

    @Test
    void chainedAliasExpansion() {
        AliasManager mgr = new DefaultAliasManager();
        mgr.setAlias("ll", "ls -la");
        mgr.setAlias("lla", "ll --all");
        assertEquals("ls -la --all", mgr.expand("lla"));
    }
}
