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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClasspathResourceUtil.
 */
public class ClasspathResourceUtilTest {

    @Test
    void testGetResourcePathFromURL() throws IOException, URISyntaxException {
        // Test loading a resource from a URL
        URL resource = ClasspathResourceUtilTest.class.getResource("/nano/jnanorc");
        assertNotNull(resource, "Resource URL should not be null");

        Path resourcePath = ClasspathResourceUtil.getResourcePath(resource);
        assertNotNull(resourcePath, "Resource path should not be null");
        assertTrue(Files.exists(resourcePath), "Resource should exist");
    }

    @Test
    void testResourceNotFound() {
        // Test that we get an IOException for non-existent resources
        assertThrows(IOException.class, () -> {
            ClasspathResourceUtil.getResourcePath("/nonexistent/resource.txt");
        });
    }

    @Test
    void testInvalidScheme() throws IOException, URISyntaxException {
        // Test that we get an IllegalArgumentException for unsupported schemes
        URL invalidUrl = new URL("http://example.com/resource.txt");
        assertThrows(IllegalArgumentException.class, () -> {
            ClasspathResourceUtil.getResourcePath(invalidUrl);
        });
    }

    @Test
    void testGetResourcePathFromJarURL() throws IOException, URISyntaxException {
        // Test loading a resource from a JAR URL (like jar:file:/path/to/jar!/motd)
        // This simulates the gogo-jline use case where motd is loaded from the JAR
        Path tempJar = Files.createTempFile("test-", ".jar");
        try {
            // Create a JAR file with a motd entry
            try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tempJar))) {
                JarEntry entry = new JarEntry("motd");
                jos.putNextEntry(entry);
                jos.write("Welcome to the test JAR!\n".getBytes());
                jos.closeEntry();
            }

            // Create a jar: URL pointing to the motd file in the JAR
            String jarUrl = "jar:" + tempJar.toUri() + "!/motd";
            URL url = new URL(jarUrl);

            // This should work with the jar: URI
            Path resourcePath = ClasspathResourceUtil.getResourcePath(url);
            assertNotNull(resourcePath, "Resource path should not be null");
            assertTrue(Files.exists(resourcePath), "Resource should exist");

            // Verify we can read the content
            String content = Files.readString(resourcePath);
            assertTrue(content.contains("Welcome to the test JAR!"), "Content should match");
        } finally {
            Files.deleteIfExists(tempJar);
        }
    }
}
