/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

import java.io.FileDescriptor;

/**
 * Native interface for JLine's low-level system operations.
 * <p>
 * This class provides access to native methods that are implemented in the JLine native library.
 * It automatically initializes the native library using {@link JLineNativeLoader#initialize()}
 * when the class is loaded.
 * <p>
 * The native methods in this class provide functionality that is not available through standard
 * Java APIs, such as creating file descriptors and process redirects directly from file descriptors.
 * <p>
 * This class is primarily used internally by JLine's terminal implementations, particularly
 * those that require direct access to native system calls. Users of JLine typically do not need
 * to interact with this class directly.
 * <p>
 * If the native library cannot be loaded, attempts to use methods in this class will result
 * in {@link UnsatisfiedLinkError} exceptions.
 *
 * @see JLineNativeLoader For details on how the native library is loaded and configured
 */
@SuppressWarnings("unused")
public class JLineLibrary {

    /**
     * Private constructor to prevent instantiation.
     */
    private JLineLibrary() {
        // Utility class
    }

    static {
        JLineNativeLoader.initialize();
    }

    public static native FileDescriptor newFileDescriptor(int fd);

    public static native ProcessBuilder.Redirect newRedirectPipe(FileDescriptor fd);
}
