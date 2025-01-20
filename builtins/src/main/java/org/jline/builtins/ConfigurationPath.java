/*
 * Copyright (c) 2002-2019, the original author(s).
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

public class ConfigurationPath {
    private Path appConfig;
    private Path userConfig;

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
