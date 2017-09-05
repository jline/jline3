/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public interface Source {

    String getName();

    InputStream read() throws IOException;

    class URLSource implements Source {
        final URL url;
        final String name;

        public URLSource(URL url, String name) {
            this.url = Objects.requireNonNull(url);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream read() throws IOException {
            return url.openStream();
        }

    }

    class PathSource implements Source {
        final Path path;
        final String name;

        public PathSource(File file, String name) {
            this(Objects.requireNonNull(file).toPath(), name);
        }

        public PathSource(Path path, String name) {
            this.path = Objects.requireNonNull(path);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream read() throws IOException {
            return Files.newInputStream(path);
        }

    }

    class InputStreamSource implements Source {
        final InputStream in;
        final String name;

        public InputStreamSource(InputStream in, boolean close, String name) {
            Objects.requireNonNull(in);
            if (close) {
                this.in = in;
            } else {
                this.in = new FilterInputStream(in) {
                    @Override
                    public void close() throws IOException {
                    }
                };
            }
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream read() throws IOException {
            return in;
        }
    }

    class StdInSource extends InputStreamSource {

        public StdInSource() {
            this(System.in);
        }

        public StdInSource(InputStream in) {
            super(in, false, null);
        }

    }
}
