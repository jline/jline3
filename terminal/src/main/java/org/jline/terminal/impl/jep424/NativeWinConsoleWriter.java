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

import java.io.IOException;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;

import org.jline.terminal.impl.AbstractWindowsConsoleWriter;

import static org.jline.terminal.impl.jep424.Kernel32.GetStdHandle;
import static org.jline.terminal.impl.jep424.Kernel32.STD_OUTPUT_HANDLE;
import static org.jline.terminal.impl.jep424.Kernel32.WriteConsoleW;
import static org.jline.terminal.impl.jep424.Kernel32.getLastErrorMessage;

class NativeWinConsoleWriter extends AbstractWindowsConsoleWriter
{

    private final MemoryAddress console = GetStdHandle( STD_OUTPUT_HANDLE );

    @Override
    protected void writeConsole( char[] text, int len ) throws IOException
    {
        MemorySegment txt = MemorySegment.ofArray( text );
        if ( WriteConsoleW( console, txt, len, null, null ) == 0 )
        {
            throw new IOException( "Failed to write to console: " + getLastErrorMessage() );
        }
    }
}
