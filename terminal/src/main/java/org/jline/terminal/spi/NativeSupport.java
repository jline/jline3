/*
 * Copyright (c) 2022, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.spi;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

public interface NativeSupport
{

    enum Stream {
        Input,
        Output,
        Error
    }

    String name();

    Pty current(Stream consoleStream) throws IOException;

    Pty open(Attributes attributes, Size size) throws IOException;

    Terminal winSysTerminal(String name, String type, boolean ansiPassThrough,
                            Charset encoding, int codepage, boolean nativeSignals,
                            Terminal.SignalHandler signalHandler, boolean paused,
                            Stream consoleStream) throws IOException;

    boolean isWindowsSystemStream(Stream stream);

    boolean isPosixSystemStream(Stream stream);

    String posixSystemStreamName(Stream stream);

}
