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
import java.nio.file.*;

import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for using ConfigurationPath with classpath resources.
 */
public class ClasspathConfigurationPathTest {

    @Test
    public void testClasspathConfigurationPath() throws Exception {
        // Get the resource path for the nanorc file
        Path appConfig = getResourcePath("/nano/jnanorc").getParent();

        // Create a ConfigurationPath with the classpath resource
        ConfigurationPath configPath = new ConfigurationPath(appConfig, null);

        // Verify we can get the config file
        Path nanorcPath = configPath.getConfig("jnanorc");
        assertNotNull(nanorcPath, "Should find jnanorc file");
        assertTrue(Files.exists(nanorcPath), "jnanorc file should exist");

        // Test that we can use the config file with SyntaxHighlighter
        SyntaxHighlighter highlighter = SyntaxHighlighter.build(nanorcPath, "java");
        assertNotNull(highlighter, "Highlighter should not be null");

        // Test highlighting some Java code with keywords that should be highlighted
        String javaCode = "public class Test { private static final int x = 42; }";
        AttributedString highlighted = highlighter.highlight(javaCode);
        assertNotNull(highlighted, "Highlighted text should not be null");

        // The length of the text remains the same
        assertEquals(
                javaCode.length(),
                highlighted.length(),
                "Highlighted text should have the same length as original text");

        // Just verify the highlighter was created successfully
        // We can't reliably test the actual highlighting in a unit test
        // since it depends on the terminal capabilities
    }

    /**
     * Helper method to get a Path from a classpath resource.
     */
    static Path getResourcePath(String name) throws IOException, URISyntaxException {
        return ClasspathResourceUtil.getResourcePath(name, ClasspathConfigurationPathTest.class);
    }
}
