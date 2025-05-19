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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;

/**
 * Utility class for working with classpath resources.
 */
public class ClasspathResourceUtil {

    /**
     * Converts a classpath resource to a Path.
     *
     * @param name The resource name (e.g., "/nano/jnanorc")
     * @return The Path to the resource
     * @throws IOException If an I/O error occurs
     * @throws URISyntaxException If the resource URI is invalid
     */
    public static Path getResourcePath(String name) throws IOException, URISyntaxException {
        return getResourcePath(name, ClasspathResourceUtil.class.getClassLoader());
    }

    /**
     * Converts a classpath resource to a Path.
     *
     * @param name The resource name (e.g., "/nano/jnanorc")
     * @param clazz The class to use for resource loading
     * @return The Path to the resource
     * @throws IOException If an I/O error occurs
     * @throws URISyntaxException If the resource URI is invalid
     */
    public static Path getResourcePath(String name, Class<?> clazz) throws IOException, URISyntaxException {
        URL resource = clazz.getResource(name);
        if (resource == null) {
            throw new IOException("Resource not found: " + name);
        }
        return getResourcePath(resource);
    }

    /**
     * Converts a classpath resource to a Path.
     *
     * @param name The resource name (e.g., "/nano/jnanorc")
     * @param classLoader The ClassLoader to use for resource loading
     * @return The Path to the resource
     * @throws IOException If an I/O error occurs
     * @throws URISyntaxException If the resource URI is invalid
     */
    public static Path getResourcePath(String name, ClassLoader classLoader) throws IOException, URISyntaxException {
        URL resource = classLoader.getResource(name);
        if (resource == null) {
            throw new IOException("Resource not found: " + name);
        }
        return getResourcePath(resource);
    }

    /**
     * Converts a URL to a Path.
     *
     * @param resource The URL to convert
     * @return The Path to the resource
     * @throws IOException If an I/O error occurs
     * @throws URISyntaxException If the resource URI is invalid
     */
    public static Path getResourcePath(URL resource) throws IOException, URISyntaxException {
        URI uri = resource.toURI();
        String scheme = uri.getScheme();

        if (scheme.equals("file")) {
            return Paths.get(uri);
        }

        if (!scheme.equals("jar")) {
            throw new IllegalArgumentException("Cannot convert to Path: " + uri);
        }

        String s = uri.toString();
        int separator = s.indexOf("!/");
        String entryName = s.substring(separator + 2);
        URI fileURI = URI.create(s.substring(0, separator));

        FileSystem fs = FileSystems.newFileSystem(fileURI, new HashMap<>());
        return fs.getPath(entryName);
    }
}
