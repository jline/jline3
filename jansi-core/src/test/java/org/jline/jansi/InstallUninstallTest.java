/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class InstallUninstallTest {

    @Test
    void testInstallUninstall() {
        AnsiConsole.systemInstall();
        try {
            assertNotNull(AnsiConsole.out());
            assertNotNull(AnsiConsole.err());
        } finally {
            AnsiConsole.systemUninstall();
        }
    }
}
