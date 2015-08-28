package org.jline;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.jline.reader.UserInterruptException;

public interface Reader {

    /**
     * Read the next line and return the contents of the buffer.
     *
     * Equivalent to <code>readLine(null, null, null)</code>
     */
    String readLine() throws UserInterruptException, EOFException;

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     *
     * Equivalent to <code>readLine(null, mask, null)</code>
     */
    String readLine(Character mask) throws UserInterruptException, EOFException;

    /**
     * Read the next line with the specified prompt.
     * If null, then the default prompt will be used.
     *
     * Equivalent to <code>readLine(prompt, null, null)</code>
     */
    String readLine(String prompt) throws UserInterruptException, EOFException;

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * Equivalent to <code>readLine(prompt, mask, null)</code>
     */
    String readLine(String prompt, Character mask) throws UserInterruptException, EOFException;

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @param mask      The character mask, may be null.
     * @param buffer    The default value presented to the user to edit, may be null.
     * @return          A line that is read from the console, can never be null.
     *
     * @throws UserInterruptException if readLine was interrupted (using Ctrl-C for example)
     * @throws EOFException if an EOF has been found (using Ctrl-D for example)
     * @throws IOException in case of other i/o errors
     */
    String readLine(String prompt, Character mask, String buffer) throws UserInterruptException, EOFException;

}
