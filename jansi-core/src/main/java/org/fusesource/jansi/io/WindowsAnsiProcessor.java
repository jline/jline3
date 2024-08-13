/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.fusesource.jansi.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A Windows ANSI escape processor, that uses JNA to access native platform
 * API's to change the console attributes (see
 * <a href="http://fusesource.github.io/jansi/documentation/native-api/index.html?org/fusesource/jansi/internal/Kernel32.html">Jansi native Kernel32</a>).
 * <p>The native library used is named <code>jansi</code> and is loaded using <a href="http://fusesource.github.io/hawtjni/">HawtJNI</a> Runtime
 * <a href="http://fusesource.github.io/hawtjni/documentation/api/index.html?org/fusesource/hawtjni/runtime/Library.html"><code>Library</code></a>
 *
 * @since 1.19
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author Joris Kuipers
 * @deprecated Use {@link org.jline.jansi.io.WindowsAnsiProcessor} instead.
 */
@Deprecated
public final class WindowsAnsiProcessor extends AnsiProcessor {

    public WindowsAnsiProcessor(OutputStream ps, long console) throws IOException {
        super(ps);
    }

    public WindowsAnsiProcessor(OutputStream ps, boolean stdout) throws IOException {
        super(ps);
    }

    public WindowsAnsiProcessor(OutputStream ps) throws IOException {
        super(ps);
    }
}
