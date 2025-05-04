/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.jline.reader.LineReader;

/**
 * Utility class for configuring a LineReader from an inputrc file.
 * <p>
 * This class provides methods to configure a LineReader using initialization
 * files in the same format as GNU Readline's inputrc files. These files can
 * define key bindings, variable settings, and other configuration options.
 * </p>
 */
public final class InputRC {

    /**
     * Configures a LineReader from an inputrc file at the specified URL.
     *
     * @param reader the LineReader to configure
     * @param url the URL of the inputrc file
     * @throws IOException if an I/O error occurs
     */
    public static void configure(LineReader reader, URL url) throws IOException {
        org.jline.reader.impl.InputRC.configure(reader, url);
    }

    /**
     * Configures a LineReader from an inputrc file provided as an InputStream.
     *
     * @param reader the LineReader to configure
     * @param is the InputStream containing the inputrc content
     * @throws IOException if an I/O error occurs
     */
    public static void configure(LineReader reader, InputStream is) throws IOException {
        org.jline.reader.impl.InputRC.configure(reader, is);
    }

    /**
     * Configures a LineReader from an inputrc file provided as a Reader.
     *
     * @param reader the LineReader to configure
     * @param r the Reader containing the inputrc content
     * @throws IOException if an I/O error occurs
     */
    public static void configure(LineReader reader, Reader r) throws IOException {
        org.jline.reader.impl.InputRC.configure(reader, r);
    }
}
