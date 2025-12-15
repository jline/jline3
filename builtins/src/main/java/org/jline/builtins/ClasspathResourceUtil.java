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
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Utility class for working with classpath resources.
 * <p>
 * This utility provides methods to convert classpath resources to Path objects,
 * which can be used with JLine's configuration classes like ConfigurationPath.
 * </p>
 */
public class ClasspathResourceUtil {

    /**
     * Creates a new ClasspathResourceUtil.
     */
    public ClasspathResourceUtil() {
        // Default constructor
    }

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
     * <p>
     * For file:// URLs, returns a Path directly to the file.
     * For jar: URLs, opens the JAR FileSystem and returns a Path within it.
     * The returned Path is valid as long as the underlying FileSystem remains open.
     * </p>
     * <p>
     * Note: For jar: URLs, the FileSystem is created on first access and reused for
     * subsequent accesses to the same JAR. The FileSystem will remain open for the
     * lifetime of the application. Callers should not attempt to close the FileSystem
     * as it may be shared with other code.
     * </p>
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
        String jarPart = s.substring(0, separator);

        // Use the jar: URI directly with FileSystems.newFileSystem()
        // This is safer than stripping the jar: prefix, as it ensures the jar provider is used
        URI jarURI = URI.create(jarPart);

        FileSystem fs;
        try {
            fs = FileSystems.newFileSystem(jarURI, new HashMap<>());
        } catch (FileSystemAlreadyExistsException e) {
            // FileSystem already exists, use the existing one
            fs = FileSystems.getFileSystem(jarURI);
        }
        return fs.getPath(entryName);
    }
}
