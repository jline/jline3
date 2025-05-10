/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages configuration file paths for JLine applications.
 * <p>
 * This class provides a way to manage application and user configuration directories
 * and locate configuration files within them. It follows the common pattern of looking
 * for configuration files first in the user's configuration directory, then falling back
 * to the application's configuration directory.
 * </p>
 */
public class ConfigurationPath {
    private final Path appConfig;
    private final Path userConfig;

    /**
     * Configuration class constructor.
     * @param appConfig   Application configuration directory
     * @param userConfig  User private configuration directory
     */
    public ConfigurationPath(Path appConfig, Path userConfig) {
        this.appConfig = appConfig;
        this.userConfig = userConfig;
    }

    /**
     * Search configuration file first from userConfig and then appConfig directory. Returns null if file is not found.
     * @param  name    Configuration file name.
     * @return         Configuration file.
     *
     */
    public Path getConfig(String name) {
        Path out = null;
        if (userConfig != null && Files.exists(userConfig.resolve(name))) {
            out = userConfig.resolve(name);
        } else if (appConfig != null && Files.exists(appConfig.resolve(name))) {
            out = appConfig.resolve(name);
        }
        return out;
    }

    /**
     * Search configuration file from userConfig directory. Returns null if file is not found.
     * @param  name    Configuration file name.
     * @return         Configuration file.
     * @throws         IOException   When we do not have read access to the file or directory.
     *
     */
    public Path getUserConfig(String name) throws IOException {
        return getUserConfig(name, false);
    }

    /**
     * Search configuration file from userConfig directory. Returns null if file is not found.
     * @param  name    Configuration file name
     * @param  create  When true configuration file is created if not found.
     * @return         Configuration file.
     * @throws         IOException   When we do not have read/write access to the file or directory.
     */
    public Path getUserConfig(String name, boolean create) throws IOException {
        Path out = null;
        if (userConfig != null) {
            if (!Files.exists(userConfig.resolve(name)) && create) {
                Files.createFile(userConfig.resolve(name));
            }
            if (Files.exists(userConfig.resolve(name))) {
                out = userConfig.resolve(name);
            }
        }
        return out;
    }
}
