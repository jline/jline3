/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi.win;

import java.nio.charset.StandardCharsets;

import static org.jline.nativ.Kernel32.FORMAT_MESSAGE_FROM_SYSTEM;
import static org.jline.nativ.Kernel32.FormatMessageW;
import static org.jline.nativ.Kernel32.GetLastError;

class WindowsSupport {

    public static String getLastErrorMessage() {
        int errorCode = GetLastError();
        int bufferSize = 160;
        byte[] data = new byte[bufferSize];
        FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM, 0, errorCode, 0, data, bufferSize, null);
        return new String(data, StandardCharsets.UTF_16LE).trim();
    }
}
