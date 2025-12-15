/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Interface representing a source of data that can be read.
 * <p>
 * This interface provides a unified way to access data from different sources,
 * such as files, URLs, or standard input. It abstracts away the details of how
 * the data is accessed, allowing commands to work with different types of input
 * sources in a consistent way.
 * </p>
 */
public interface Source {

    /**
     * Gets the name of this source.
     *
     * @return the name of the source
     */
    String getName();

    /**
     * Opens a stream to read the content of this source.
     *
     * @return an input stream for reading the source content
     * @throws IOException if an I/O error occurs
     */
    InputStream read() throws IOException;

    /**
     * Opens a buffered reader to read the content of this source.
     *
     * @return an buffered reader for reading the source content
     * @throws IOException if an I/O error occurs
     */
    default BufferedReader reader() throws IOException {
        return new BufferedReader(new InputStreamReader(read()));
    }

    /**
     * Gets the number of lines in this source, if known.
     *
     * @return the number of lines, or null if unknown
     */
    Long lines();

    /**
     * A Source implementation that reads from a URL.
     */
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
            try (Stream<String> lines = Files.lines(new File(url.toURI()).toPath())) {
                out = lines.count();
            } catch (Exception ignore) {
            }
            return out;
        }
    }

    /**
     * A Source implementation that reads from a file system path.
     */
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

        public Path getPath() {
            return path;
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
            try (Stream<String> lines = Files.lines(path)) {
                out = lines.count();
            } catch (Exception ignore) {
            }
            return out;
        }
    }

    /**
     * A Source implementation that reads from an InputStream.
     */
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

    /**
     * A Source implementation that reads from standard input.
     */
    class StdInSource extends InputStreamSource {

        public StdInSource() {
            this(System.in);
        }

        public StdInSource(InputStream in) {
            super(in, false, "(standard input)");
        }
    }

    /**
     * A Source implementation that reads from a classpath resource.
     */
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
