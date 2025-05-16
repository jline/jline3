/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Supplier;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for using Nano with configuration loaded from the classpath.
 */
public class NanoClasspathConfigTest {

    /**
     * Test class that simulates a command-line tool using Nano with classpath configuration.
     */
    static class NanoCommand {
        public Integer doCall() throws Exception {
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
            try (Terminal terminal = createTestTerminal()) {
                String[] argv = new String[] {"--ignorercfiles"}; // Ignore default config files
                Options opt = Options.compile(Nano.usage()).parse(argv);
                if (opt.isSet("help")) {
                    throw new Options.HelpException(opt.usage());
                } else {
                    Path currentDir = workDir.get();
                    Path appConfig = getResourcePath("/nano/jnanorc").getParent();
                    ConfigurationPath configPath = new ConfigurationPath(appConfig, null);
                    Nano nano = new Nano(terminal, currentDir, opt, configPath);
                    // We don't actually run the editor in the test
                    // nano.open();
                    // nano.run();
                }
            }
            return 0;
        }

        private Terminal createTestTerminal() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            LineDisciplineTerminal terminal =
                    new LineDisciplineTerminal("nano", "xterm", output, StandardCharsets.UTF_8);
            terminal.setSize(new Size(80, 25));
            // Simulate pressing Ctrl-X and 'n' to exit without saving
            terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
            terminal.processInputByte('n');
            return terminal;
        }
    }

    @Test
    @Timeout(1)
    public void testNanoWithClasspathConfig() throws Exception {
        NanoCommand command = new NanoCommand();
        Integer result = command.doCall();
        assertEquals(0, result, "Command should execute successfully");
    }

    /**
     * Helper method to get a Path from a classpath resource.
     */
    static Path getResourcePath(String name) throws IOException, URISyntaxException {
        return ClasspathResourceUtil.getResourcePath(name, NanoClasspathConfigTest.class);
    }
}
