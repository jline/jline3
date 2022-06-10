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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

public interface TerminalProvider
{

    enum Stream {
        Input,
        Output,
        Error
    }

    String name();

    Terminal winSysTerminal(String name, String type, boolean ansiPassThrough,
                            Charset encoding, int codepage, boolean nativeSignals,
                            Terminal.SignalHandler signalHandler, boolean paused,
                            Stream consoleStream) throws IOException;

    Terminal posixSysTerminal(String name, String type, Charset encoding,
                              boolean nativeSignals, Terminal.SignalHandler signalHandler,
                              Stream consoleStream) throws IOException;

    Terminal newTerminal(String name, String type,
                         InputStream masterInput, OutputStream masterOutput,
                         Charset encoding, Terminal.SignalHandler signalHandler,
                         boolean paused, Attributes attributes, Size size) throws IOException;

    boolean isWindowsSystemStream(Stream stream);

    boolean isPosixSystemStream(Stream stream);

    String posixSystemStreamName(Stream stream);

}
