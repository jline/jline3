/*
 * Copyright (C) 2009 the original author(s).
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

package jline;

import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.WindowsAnsiOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * ANSI-supported {@link WindowsTerminal}.
 *
 * @since 2.0
 */
public class AnsiWindowsTerminal
    extends WindowsTerminal
{
    private final boolean ansiSupported = detectAnsiSupport();

    private static boolean detectAnsiSupport() {
        OutputStream out = AnsiConsole.wrapOutputStream(new ByteArrayOutputStream());
        try {
            out.close();
        }
        catch (Exception e) {
            // ignore;
        }
        return out instanceof WindowsAnsiOutputStream;
    }

    public AnsiWindowsTerminal() throws Exception {
        super();
    }

    @Override
    public boolean isAnsiSupported() {
        return ansiSupported;
    }
}
