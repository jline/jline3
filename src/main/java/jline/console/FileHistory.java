/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple command history using a file for persistent backing.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public class FileHistory
    implements History
{
    public static final int DEFAULT_MAX_SIZE = 500;
    
    private final List<String> items = new LinkedList<String>();

    private PrintWriter output;

    private File file;

    private int maxSize = DEFAULT_MAX_SIZE;

    private int index = 0;

    /**
     * Construstor: initialize a blank history.
     */
    public FileHistory() {
        // nothing
    }

    /**
     * Construstor: initialize History object the the specified {@link java.io.File} for
     * storage.
     */
    public FileHistory(final File file) throws IOException {
        setFile(file);
    }

    /**
     * The output to which all history elements will be written (or null of
     * history is not saved to a buffer).
     */
    public void setOutput(final PrintWriter output) {
        this.output = output;
    }

    /**
     * Returns the PrintWriter that is used to store history elements.
     */
    public PrintWriter getOutput() {
        return output;
    }

    /**
     * Flush the entire history buffer to the output PrintWriter.
     */
    public void flush() throws IOException {
        PrintWriter out = getOutput();
        if (out != null) {
            for (String item : items) {
                out.println(item);
            }
            out.flush();
        }
    }

    public void setFile(final File file) throws IOException {
        assert file != null;

        if (file.isFile()) {
            load(new FileInputStream(file));
        }

        setOutput(new PrintWriter(new FileWriter(file), true));
        flush();

        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void load(final InputStream in) throws IOException {
        load(new InputStreamReader(in));
    }

    public void load(final Reader reader) throws IOException {
        BufferedReader breader = new BufferedReader(reader);
        List<String> lines = new ArrayList<String>();
        String line;

        while ((line = breader.readLine()) != null) {
            lines.add(line);
        }

        for (String line1 : lines) {
            add(line1);
        }
    }

    public int size() {
        return items.size();
    }

    /**
     * Clear the history buffer
     */
    public void clear() {
        items.clear();
        index = 0;
    }

    /**
     * Returns an immutable list of the history buffer.
     */
    public List<String> items() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Set the maximum size that the history buffer will store.
     */
    public void setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Get the maximum size that the history buffer will store.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Returns the current history index.
     */
    public int index() {
        return index;
    }
    
    /**
     * Add the specified buffer to the end of the history. The pointer is set to
     * the end of the history buffer.
     */
    public void add(final String item) {
        assert item != null;

        // don't append duplicates to the end of the buffer
        if (!items.isEmpty() && item.equals(items.get(items.size() - 1))) {
            return;
        }

        items.add(item);

        while (items.size() > getMaxSize()) {
            items.remove(0);
        }

        index = items.size();

        PrintWriter out = getOutput();
        if (out != null) {
            out.println(item);
            out.flush();
        }
    }

    /**
     * This moves the history to the last entry. This entry is one position
     * before the moveToEnd() position.
     *
     * @return Returns false if there were no history entries or the history
     *         index was already at the last entry.
     */
    public boolean moveToLast() {
        int lastEntry = items.size() - 1;
        if (lastEntry >= 0 && lastEntry != index) {
            index = items.size() - 1;
            return true;
        }

        return false;
    }

    /**
     * Moves the history index to the first entry.
     *
     * @return Return false if there are no entries in the history or if the
     *         history is already at the beginning.
     */
    public boolean moveToFirst() {
        if (items.size() > 0 && index != 0) {
            index = 0;
            return true;
        }

        return false;
    }

    /**
     * Move to the end of the history buffer. This will be a blank entry, after
     * all of the other entries.
     */
    public void moveToEnd() {
        index = items.size();
    }

    /**
     * Return the content of the current buffer.
     */
    public String current() {
        if (index >= items.size()) {
            return "";
        }

        return items.get(index);
    }

    /**
     * Move the pointer to the previous element in the buffer.
     *
     * @return true if we successfully went to the previous element
     */
    public boolean previous() {
        if (index <= 0) {
            return false;
        }

        index--;

        return true;
    }

    /**
     * Move the pointer to the next element in the buffer.
     *
     * @return true if we successfully went to the next element
     */
    public boolean next() {
        if (index >= items.size()) {
            return false;
        }

        index++;

        return true;
    }

    @Override
    public String toString() {
        return items.toString();
    }
}