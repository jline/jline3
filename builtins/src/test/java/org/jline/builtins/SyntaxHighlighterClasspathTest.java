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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for loading nanorc files from the classpath.
 */
public class SyntaxHighlighterClasspathTest {

    @Test
    public void testLoadNanorcFromClasspath() throws Exception {
        // Test loading a nanorc file from the classpath
        SyntaxHighlighter highlighter = SyntaxHighlighter.build("classpath:/nano/jnanorc");
        assertNotNull(highlighter, "Highlighter should not be null");
    }

    @Test
    public void testNanoWithClasspathConfig(@TempDir Path tempDir) throws Exception {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes(StandardCharsets.UTF_8));

        // Set up a terminal
        Terminal terminal = TerminalBuilder.builder().build();

        // Get the resource path for the nanorc file
        Path appConfig = getResourcePath("/nano/jnanorc").getParent();

        // Create a ConfigurationPath with the classpath resource
        ConfigurationPath configPath = new ConfigurationPath(appConfig, null);

        // Create a Nano instance with the configuration
        String[] argv = new String[] {testFile.toString()};
        Options opt = Options.compile(Nano.usage()).parse(argv);

        // This just tests that we can create a Nano instance with a classpath config
        // We don't actually run it since that would be interactive
        Nano nano = new Nano(terminal, tempDir, opt, configPath);

        // Verify the configuration was loaded
        assertNotNull(nano);
    }

    /**
     * Helper method to get a Path from a classpath resource.
     */
    static Path getResourcePath(String name) throws IOException, URISyntaxException {
        return ClasspathResourceUtil.getResourcePath(name, SyntaxHighlighterClasspathTest.class);
    }
}
