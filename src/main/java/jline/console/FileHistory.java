/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import jline.internal.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

/**
 * {@link History} using a file for persistent backing.
 *
 * Implementors should install shutdown hook to call {@link FileHistory#flush}
 * to save history to disk.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public class FileHistory
    extends MemoryHistory
    implements PersistentHistory
{
    private final File file;

    public FileHistory(final File file) throws IOException {
        assert file != null;
        this.file = file;
        load(file);
    }

    public File getFile() {
        return file;
    }

    public void load(final File file) throws IOException {
        assert file != null;
        if (file.exists()) {
            Log.trace("Loading history from: ", file);
            load(new FileInputStream(file));
        }
    }

    public void load(final InputStream input) throws IOException {
        assert input != null;
        load(new InputStreamReader(input));
    }

    public void load(final Reader reader) throws IOException {
        assert reader != null;
        BufferedReader input = new BufferedReader(reader);

        String item;
        while ((item = input.readLine()) != null) {
            add(item);
        }
    }

    public void flush() throws IOException {
        Log.trace("Flusing history");

        if (!file.exists()) {
            if (!file.getParentFile().mkdirs()) {
                Log.warn("Failed to create directory structure for: ", file);
            }
            if (!file.createNewFile()) {
                Log.warn("Failed to create file: ", file);
            }
        }

        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            for (String item : items()) {
                out.println(item);
            }
        }
        finally {
            out.close();
        }
    }

    public void purge() throws IOException {
        Log.trace("Purging history");

        clear();

        if (!file.delete()) {
            Log.warn("Failed to delete history file: ", file);
        }
    }
}