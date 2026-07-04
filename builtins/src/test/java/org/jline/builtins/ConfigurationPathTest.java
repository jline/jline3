/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPathTest {

    @Test
    void getUserConfigStaysInsideUserConfig(@TempDir Path root) throws Exception {
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
    void getUserConfigRejectsParentTraversal(@TempDir Path root) throws Exception {
        Path userConfig = root.resolve("config");
        Files.createDirectories(userConfig);
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        // Mirrors a `set historylog ../pwned` directive read from a jlessrc/jnanorc file.
        assertNull(configPath.getUserConfig("../pwned", true));
        assertFalse(Files.exists(root.resolve("pwned")), "must not create a file outside the config directory");
    }

    @Test
    void getUserConfigRejectsAbsolutePath(@TempDir Path root) throws Exception {
        Path userConfig = root.resolve("config");
        Files.createDirectories(userConfig);
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        Path outside = root.resolve("outside");
        assertNull(configPath.getUserConfig(outside.toAbsolutePath().toString(), true));
        assertFalse(Files.exists(outside));
    }

    @Test
    void getConfigRejectsParentTraversal(@TempDir Path root) throws Exception {
        Path userConfig = root.resolve("config");
        Files.createDirectories(userConfig);
        // A real file sits one level above the config directory.
        Path secret = Files.write(root.resolve("secret"), new byte[] {1});
        ConfigurationPath configPath = new ConfigurationPath((Path) null, userConfig);

        assertNull(configPath.getConfig("../secret"));
        assertTrue(Files.exists(secret));
    }
}
