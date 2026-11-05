/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni.win;

import java.nio.charset.StandardCharsets;

import org.jline.nativ.Kernel32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NativeWinSysTerminalTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void errorMessageLongerThanEightyCharacters() {
        // ERROR_OPERATION_ABORTED: 93 characters in the en-US message table
        int errorCode = 995;
        int capacity = 1024;
        byte[] buffer = new byte[capacity * 2];
        int length =
                Kernel32.FormatMessageW(Kernel32.FORMAT_MESSAGE_FROM_SYSTEM, 0, errorCode, 0, buffer, capacity, null);
        String expected = new String(buffer, 0, length * 2, StandardCharsets.UTF_16LE).trim();
        // only meaningful when the message overran the old 160-byte buffer and fits in 160 WCHARs
        assumeTrue(expected.length() > 80 && length < 160);
        assertEquals(expected, NativeWinSysTerminal.getErrorMessage(errorCode));
    }
}
