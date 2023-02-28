/*
 * Copyright (c) 2002-2022, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
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

    Long lines();

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

        @Override
        public Long lines() {
            Long out = null;
            try {
                out = Files.lines(new File(url.toURI()).toPath()).count();
            } catch (Exception e) {
            }
            return out;
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

        @Override
        public Long lines() {
            Long out = null;
            try {
                out = Files.lines(path).count();
            } catch (Exception e) {
            }
            return out;
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
                    public void close() throws IOException {}
                };
            }
            if (this.in.markSupported()) {
                this.in.mark(Integer.MAX_VALUE);
            }
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream read() throws IOException {
            if (in.markSupported()) {
                in.reset();
            }
            return in;
        }

        @Override
        public Long lines() {
            return null;
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

    class ResourceSource implements Source {
        final String resource;
        final String name;

        public ResourceSource(String resource) {
            this(resource, resource);
        }

        public ResourceSource(String resource, String name) {
            this.resource = Objects.requireNonNull(resource);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream read() throws IOException {
            return getClass().getResourceAsStream(resource);
        }

        @Override
        public Long lines() {
            return null;
        }
    }
}
