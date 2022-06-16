/*
 * Copyright (C) 2022 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jline.terminal.impl.jep424;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;


public class Jep424TerminalProvider implements TerminalProvider
{
    @Override
    public String name()
    {
        return "jep424";
    }

    @Override
    public Terminal sysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding,
                                boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused,
                                Stream consoleStream) throws IOException {
        if ( OSUtils.IS_WINDOWS) {
            return NativeWinSysTerminal.createTerminal(name, type, ansiPassThrough, encoding,
                    nativeSignals, signalHandler, paused, consoleStream);
        } else {
            Pty pty = new NativePty(-1, null, 0, FileDescriptor.in,
                    consoleStream == Stream.Output ? 1 : 2,
                    consoleStream == Stream.Output ? FileDescriptor.out : FileDescriptor.err,
                    CLibrary.ttyName(0));
            return new PosixSysTerminal(name, type, pty, encoding, nativeSignals, signalHandler);
        }
    }

    @Override
    public Terminal newTerminal(String name, String type, InputStream in, OutputStream out,
                                Charset encoding, Terminal.SignalHandler signalHandler, boolean paused,
                                Attributes attributes, Size size) throws IOException
    {
        Pty pty = CLibrary.openpty(attributes, size);
        return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler, paused);
    }

    @Override
    public boolean isSystemStream(Stream stream) {
        if (OSUtils.IS_WINDOWS) {
            return isWindowsSystemStream(stream);
        } else {
            return isPosixSystemStream(stream);
        }
    }

    public boolean isWindowsSystemStream(Stream stream) {
        return NativeWinSysTerminal.isWindowsSystemStream(stream);
    }

    public boolean isPosixSystemStream(Stream stream) {
        return NativePty.isPosixSystemStream(stream);
    }

    @Override
    public String systemStreamName(Stream stream) {
        return NativePty.posixSystemStreamName(stream);
    }


}
