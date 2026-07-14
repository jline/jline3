/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationPathTest {

    @Test
    public void getUserConfigStaysInsideUserConfig(@TempDir Path root) throws Exception {
        Path userConfig = root.resolve("config");
        Files.createDirectories(userConfig);
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        Path resolved = configPath.getUserConfig("history", true);
        assertNotNull(resolved);
        assertTrue(resolved.toAbsolutePath()
                .normalize()
                .startsWith(userConfig.toAbsolutePath().normalize()));
        assertTrue(Files.exists(userConfig.resolve("history")));
    }

    @Test
    public void getUserConfigRejectsParentTraversal(@TempDir Path root) throws Exception {
        Path userConfig = root.resolve("config");
        Files.createDirectories(userConfig);
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        // Mirrors a `set historylog ../pwned` directive read from a jlessrc/jnanorc file.
        assertNull(configPath.getUserConfig("../pwned", true));
        assertFalse(Files.exists(root.resolve("pwned")), "must not create a file outside the config directory");
    }

    @Test
    public void getUserConfigRejectsAbsolutePath(@TempDir Path root) throws Exception {
        Path userConfig = root.resolve("config");
        Files.createDirectories(userConfig);
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        Path outside = root.resolve("outside");
        assertNull(configPath.getUserConfig(outside.toAbsolutePath().toString(), true));
        assertFalse(Files.exists(outside));
    }

    @Test
    public void getConfigRejectsParentTraversal(@TempDir Path root) throws Exception {
        Path userConfig = root.resolve("config");
        Files.createDirectories(userConfig);
        // A real file sits one level above the config directory.
        Path secret = Files.write(root.resolve("secret"), new byte[] {1});
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        assertNull(configPath.getConfig("../secret"));
        assertTrue(Files.exists(secret));
    }

    @Test
    public void getConfigFallsBackToAppConfig(@TempDir Path root) throws Exception {
        Path userConfig = Files.createDirectories(root.resolve("user"));
        Path appConfig = Files.createDirectories(root.resolve("app"));
        Files.write(appConfig.resolve("jnanorc"), new byte[] {1});
        ConfigurationPath configPath = new ConfigurationPath(appConfig, userConfig);

        // Not in userConfig, so the lookup falls back to appConfig.
        Path resolved = configPath.getConfig("jnanorc");
        assertNotNull(resolved);
        assertEquals(appConfig.toRealPath().resolve("jnanorc"), resolved);
        // Traversal out of appConfig is rejected as well.
        Files.write(root.resolve("escape"), new byte[] {1});
        assertNull(configPath.getConfig("../escape"));
    }

    @Test
    public void getConfigRejectsSymlinkEscape(@TempDir Path root) throws Exception {
        Path userConfig = Files.createDirectories(root.resolve("config"));
        Path outside = Files.createDirectories(root.resolve("outside"));
        Files.write(outside.resolve("secret"), "sensitive".getBytes());
        // Create a symlink inside the config directory pointing outside
        Path link = userConfig.resolve("escape");
        Files.createSymbolicLink(link, outside);
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        // "escape/secret" passes a lexical startsWith check but the symlink
        // resolves to outside/secret which is not inside the config directory.
        assertNull(configPath.getConfig("escape/secret"));
        assertNull(configPath.getUserConfig("escape/secret", false));
    }

    @Test
    public void relativeBaseStillResolves() throws Exception {
        // Console constructs ConfigurationPath with Paths.get("."); an empty normalized
        // base must not make every lookup return null.
        Path file = Files.createTempFile(Paths.get("").toAbsolutePath(), "configpath", ".txt");
        try {
            ConfigurationPath configPath = new ConfigurationPath(Paths.get("."), Paths.get("."));
            String name = file.getFileName().toString();
            assertNotNull(configPath.getConfig(name));
            assertNotNull(configPath.getUserConfig(name, false));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
