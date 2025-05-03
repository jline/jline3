/*
 * Copyright (c) 2022-2025, the original author(s).
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
import java.util.Properties;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

/**
 * Service provider interface for terminal implementations.
 *
 * <p>
 * The TerminalProvider interface defines the contract for classes that can create
 * and manage terminal instances on specific platforms. Each provider implements
 * platform-specific terminal functionality, allowing JLine to work across different
 * operating systems and environments.
 * </p>
 *
 * <p>
 * JLine includes several built-in terminal providers:
 * </p>
 * <ul>
 *   <li>FFM - Foreign Function Memory (Java 22+) based implementation</li>
 *   <li>JNI - Java Native Interface based implementation</li>
 *   <li>Jansi - Implementation based on the Jansi library</li>
 *   <li>JNA - Java Native Access based implementation</li>
 *   <li>Exec - Implementation using external commands</li>
 *   <li>Dumb - Fallback implementation with limited capabilities</li>
 * </ul>
 *
 * <p>
 * Terminal providers are loaded dynamically using the {@link #load(String)} method,
 * which looks up provider implementations in the classpath based on their name.
 * </p>
 *
 * @see Terminal
 * @see org.jline.terminal.TerminalBuilder
 */
public interface TerminalProvider {

    /**
     * Returns the name of this terminal provider.
     *
     * <p>
     * The provider name is a unique identifier that can be used to request this
     * specific provider when creating terminals. Common provider names include
     * "ffm", "jni", "jansi", "jna", "exec", and "dumb".
     * </p>
     *
     * @return the name of this terminal provider
     */
    String name();

    /**
     * Creates a terminal connected to a system stream.
     *
     * <p>
     * This method creates a terminal that is connected to one of the standard
     * system streams (standard input, standard output, or standard error). Such
     * terminals typically represent the actual terminal window or console that
     * the application is running in.
     * </p>
     *
     * @param name the name of the terminal
     * @param type the terminal type (e.g., "xterm", "dumb")
     * @param ansiPassThrough whether to pass through ANSI escape sequences
     * @param encoding the character encoding to use
     * @param nativeSignals whether to use native signal handling
     * @param signalHandler the signal handler to use
     * @param paused whether the terminal should start in a paused state
     * @param systemStream the system stream to connect to
     * @return a new terminal connected to the specified system stream
     * @throws IOException if an I/O error occurs
     */
    Terminal sysTerminal(
            String name,
            String type,
            boolean ansiPassThrough,
            Charset encoding,
            boolean nativeSignals,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            SystemStream systemStream)
            throws IOException;

    /**
     * Creates a new terminal with custom input and output streams.
     *
     * <p>
     * This method creates a terminal that is connected to the specified input and
     * output streams. Such terminals can be used for various purposes, such as
     * connecting to remote terminals over network connections or creating virtual
     * terminals for testing.
     * </p>
     *
     * @param name the name of the terminal
     * @param type the terminal type (e.g., "xterm", "dumb")
     * @param masterInput the input stream to read from
     * @param masterOutput the output stream to write to
     * @param encoding the character encoding to use
     * @param signalHandler the signal handler to use
     * @param paused whether the terminal should start in a paused state
     * @param attributes the initial terminal attributes
     * @param size the initial terminal size
     * @return a new terminal connected to the specified streams
     * @throws IOException if an I/O error occurs
     */
    Terminal newTerminal(
            String name,
            String type,
            InputStream masterInput,
            OutputStream masterOutput,
            Charset encoding,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            Attributes attributes,
            Size size)
            throws IOException;

    /**
     * Checks if the specified system stream is available on this platform.
     *
     * <p>
     * This method determines whether the specified system stream (standard input,
     * standard output, or standard error) is available for use on the current
     * platform. Some platforms or environments may restrict access to certain
     * system streams.
     * </p>
     *
     * @param stream the system stream to check
     * @return {@code true} if the system stream is available, {@code false} otherwise
     */
    boolean isSystemStream(SystemStream stream);

    /**
     * Returns the name of the specified system stream on this platform.
     *
     * <p>
     * This method returns a platform-specific name or identifier for the specified
     * system stream. The name may be used for display purposes or for accessing
     * the stream through platform-specific APIs.
     * </p>
     *
     * @param stream the system stream
     * @return the name of the system stream on this platform
     */
    String systemStreamName(SystemStream stream);

    /**
     * Returns the width (number of columns) of the specified system stream.
     *
     * <p>
     * This method determines the width of the terminal associated with the specified
     * system stream. The width is measured in character cells and represents the
     * number of columns available for display.
     * </p>
     *
     * @param stream the system stream
     * @return the width of the system stream in character columns
     */
    int systemStreamWidth(SystemStream stream);

    /**
     * Loads a terminal provider with the specified name.
     *
     * <p>
     * This method loads a terminal provider implementation based on its name.
     * Provider implementations are discovered through the Java ServiceLoader
     * mechanism, looking for resource files in the classpath at
     * {@code META-INF/services/org/jline/terminal/provider/[name]}.
     * </p>
     *
     * <p>
     * Each provider resource file should contain a {@code class} property that
     * specifies the fully qualified name of the provider implementation class.
     * </p>
     *
     * @param name the name of the provider to load
     * @return the loaded terminal provider
     * @throws IOException if the provider cannot be loaded
     */
    static TerminalProvider load(String name) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = TerminalProvider.class.getClassLoader();
        }
        InputStream is = cl.getResourceAsStream("META-INF/services/org/jline/terminal/provider/" + name);
        if (is != null) {
            Properties props = new Properties();
            try {
                props.load(is);
                String className = props.getProperty("class");
                if (className == null) {
                    throw new IOException("No class defined in terminal provider file " + name);
                }
                Class<?> clazz = cl.loadClass(className);
                return (TerminalProvider) clazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new IOException("Unable to load terminal provider " + name + ": " + e.getMessage(), e);
            }
        } else {
            throw new IOException("Unable to find terminal provider " + name);
        }
    }
}
