/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

@Deprecated
public class WindowsSupport {

    /**
     * Creates a new WindowsSupport.
     */
    public WindowsSupport() {
        // Default constructor
    }

    @Deprecated
    public static String getLastErrorMessage() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static String getErrorMessage(int errorCode) {
        throw new UnsupportedOperationException();
    }
}
