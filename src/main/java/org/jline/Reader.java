package org.jline;

import java.io.IOException;
import java.io.InputStream;

public interface Reader {

    /**
     * Read the next line and return the contents of the buffer.
     *
     * Equivalent to <code>readLine(null, null, null)</code>
     */
    String readLine() throws IOException;

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     *
     * Equivalent to <code>readLine(null, mask, null)</code>
     */
    String readLine(final Character mask) throws IOException;

    /**
     * Read the next line with the specified prompt.
     * If null, then the default prompt will be used.
     *
     * Equivalent to <code>readLine(prompt, null, null)</code>
     */
    String readLine(final String prompt) throws IOException;

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * Equivalent to <code>readLine(prompt, mask, null)</code>
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @param mask      The character mask, may be null
     * @return          A line that is read from the console, or null if there was null input (e.g., <i>CTRL-D</i>
     *                  was pressed).
     */
    String readLine(String prompt, final Character mask) throws IOException;

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @param mask      The character mask, may be null.
     * @param buffer    The default value presented to the user to edit, may be null.
     * @return          A line that is read from the console, or null if there was null input (e.g., <i>CTRL-D</i>
     *                  was pressed).
     */
    String readLine(String prompt, final Character mask, String buffer) throws IOException;

}
