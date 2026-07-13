/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for executing external commands and capturing their output.
 *
 * <p>
 * The ExecHelper class provides methods for executing external commands (primarily
 * on Unix-like systems) and capturing their output. It handles the complexities of
 * process creation, input/output redirection, and process termination.
 * </p>
 *
 * <p>
 * This class is used by various JLine components that need to interact with the
 * underlying operating system, such as terminal detection, capability querying,
 * and terminal size determination. It provides a simplified interface for executing
 * commands and capturing their output as strings.
 * </p>
 *
 * <p>
 * The methods in this class handle common error conditions, such as interrupted
 * execution and I/O errors, and provide appropriate logging for debugging purposes.
 * </p>
 *
 * <p>
 * Note that while this class is primarily designed for Unix-like systems, some
 * functionality may work on other platforms depending on the available commands.
 * </p>
 */
public final class ExecHelper {

    private ExecHelper() {}

    public static String exec(boolean redirectInput, final String... cmd) throws IOException {
        Objects.requireNonNull(cmd);
        try {
            Log.trace("Running: ", cmd);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (OSUtils.IS_AIX) {
                Map<String, String> env = pb.environment();
                env.put("PATH", "/opt/freeware/bin:" + env.get("PATH"));
                env.put("LANG", "C");
                env.put("LC_ALL", "C");
            }
            if (redirectInput) {
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            }
            Process p = pb.start();
            String result = waitAndCapture(p);
            Log.trace("Result: ", result);
            if (p.exitValue() != 0) {
                if (result.endsWith("\n")) {
                    result = result.substring(0, result.length() - 1);
                }
                throw new IOException("Error executing '" + String.join(" ", (CharSequence[]) cmd) + "': " + result);
            }
            return result;
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException("Command interrupted").initCause(e);
        }
    }

    public static String waitAndCapture(Process p) throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (InputStream in = p.getInputStream();
                InputStream err = p.getErrorStream();
                OutputStream out = p.getOutputStream()) {
            int c;
            while ((c = in.read()) != -1) {
                bout.write(c);
            }
            while ((c = err.read()) != -1) {
                bout.write(c);
            }
            p.waitFor();
        }
        return bout.toString();
    }
}
