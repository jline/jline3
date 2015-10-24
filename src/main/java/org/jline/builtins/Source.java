/*
 * Copyright (c) 2002-2015, the original author or authors.
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

public interface Source {

    String getName();

    InputStream read() throws IOException;

    class URLSource implements Source {
        final URL url;
        final String name;

        public URLSource(URL url, String name) {
            this.url = url;
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
            this.path = file.toPath();
            this.name = name;
        }

        public PathSource(Path path, String name) {
            this.path = path;
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

    class StdInSource implements Source {
        @Override
        public String getName() {
            return null;
        }

        @Override
        public InputStream read() throws IOException {
            return new FilterInputStream(System.in) {
                @Override
                public void close() throws IOException {
                }
            };
        }

    }
}
