/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl.history;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jline.reader.History;
import org.jline.utils.Log;

/**
 * {@link History} using a file for persistent backing.
 * <p/>
 * Implementers should install shutdown hook to call {@link FileHistory#flush}
 * to save history to disk.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public class FileHistory extends MemoryHistory
{
    private final File file;
    private final boolean append;
    private final List<String> itemsToAppend;

    public FileHistory(File file) throws IOException {
        this(file, false);
    }

    public FileHistory(File file, boolean append) throws IOException {
        this.file = Objects.requireNonNull(file).getAbsoluteFile();
        load(file);

        this.append = append;
        this.itemsToAppend = new LinkedList<>();
    }

    public File getFile() {
        return file;
    }

    public void load(final File file) throws IOException {
        Objects.requireNonNull(file);
        if (file.exists()) {
            Log.trace("Loading history from: ", file);
            FileReader reader = null;
            try{
                reader = new FileReader(file);
                load(reader);
            } finally{
                if(reader != null){
                    reader.close();
                }
            }
        }
    }

    public void load(final InputStream input) throws IOException {
        Objects.requireNonNull(input);
        load(new InputStreamReader(input));
    }

    public void load(final Reader reader) throws IOException {
        Objects.requireNonNull(reader);
        BufferedReader input = new BufferedReader(reader);

        String item;
        while ((item = input.readLine()) != null) {
            internalAdd(item);
        }
    }

    @Override
    public void add(String item) {
        super.add(item);
        if (append) {
            itemsToAppend.add(item);
        }
    }

    public void flush() throws IOException {
        Log.trace("Flushing history");

        if (!file.exists()) {
            File dir = file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                Log.warn("Failed to create directory: ", dir);
            }
            if (!file.createNewFile()) {
                Log.warn("Failed to create file: ", file);
            }
        }

        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, append)))) {
            if (append) {
                itemsToAppend.stream().forEach(out::println);
                itemsToAppend.clear();
            } else {
                for (Entry entry : this) {
                    out.println(entry.value());
                }
            }
        }
    }

    public void clear() {
        Log.trace("Purging history");

        super.clear();
        if (append) {
            itemsToAppend.clear();
        }

        if (!file.delete()) {
            Log.warn("Failed to delete history file: ", file);
        }
    }
}
