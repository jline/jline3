/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the loading of JLine's native libraries (*.dll, *.jnilib, *.so) according to the current
 * operating system (Windows, Linux, macOS) and architecture.
 * <p>
 * This class handles the discovery, extraction, and loading of the appropriate native library
 * for the current platform. The native libraries are essential for certain terminal operations
 * that require direct system calls.
 * <p>
 * <h2>Usage</h2>
 * Call {@link #initialize()} before using JLine features that require native support:
 * <pre>
 * JLineNativeLoader.initialize();
 * </pre>
 *
 * <h2>Library Loading Process</h2>
 * The loader attempts to find and load the native library in the following order:
 * <ol>
 *   <li>From the path specified by the {@code library.jline.path} system property</li>
 *   <li>From the JAR file's embedded native libraries</li>
 *   <li>From the Java library path ({@code java.library.path})</li>
 * </ol>
 *
 * <h2>Configuration Options</h2>
 * The following system properties can be used to configure the native library loading:
 * <ul>
 *   <li>{@code library.jline.path} - Custom directory path where native libraries are located.
 *       The loader will check both {@code [library.jline.path]/[os]/[arch]} and
 *       {@code [library.jline.path]} directly.</li>
 *   <li>{@code library.jline.name} - Custom name for the native library file. If not specified,
 *       the default name will be used (e.g., "jlinenative.dll" on Windows).</li>
 *   <li>{@code jline.tmpdir} - Custom temporary directory for extracting native libraries.
 *       If not specified, {@code java.io.tmpdir} will be used.</li>
 *   <li>{@code java.library.path} - Standard Java property for native library search paths,
 *       used as a last resort.</li>
 * </ul>
 *
 * <h2>Platform Detection</h2>
 * The loader automatically detects the current operating system and architecture using
 * {@link OSInfo} to determine which native library to load. Supported platforms include:
 * <ul>
 *   <li>Operating Systems: Windows, macOS (Darwin), Linux, AIX</li>
 *   <li>Architectures: x86, x86_64 (amd64), ARM (various versions), PowerPC, and others</li>
 * </ul>
 *
 * <h2>Temporary Files</h2>
 * When loading from the JAR file, the native library is extracted to a temporary location.
 * These temporary files:
 * <ul>
 *   <li>Include version and UUID in the filename to avoid conflicts</li>
 *   <li>Are automatically cleaned up on JVM exit</li>
 *   <li>Old unused libraries from previous runs are cleaned up</li>
 * </ul>
 *
 * <h2>Troubleshooting</h2>
 * If the library fails to load, an exception is thrown with details about:
 * <ul>
 *   <li>The detected OS and architecture</li>
 *   <li>All paths that were searched</li>
 *   <li>The specific error that occurred</li>
 * </ul>
 *
 * <h2>Java Module System (JPMS) Considerations</h2>
 * When using JLine with the Java Module System, you may need to add the
 * {@code --enable-native-access=ALL-UNNAMED} JVM option to allow the native library loading.
 *
 * @see OSInfo For details on platform detection
 */
public class JLineNativeLoader {

    private static final Logger logger = Logger.getLogger("org.jline");
    private static boolean loaded = false;
    private static String nativeLibraryPath;
    private static String nativeLibrarySourceUrl;

    /**
     * Loads the JLine native library for the current platform.
     * <p>
     * This method should be called before using any JLine features that require native support.
     * It handles the discovery, extraction, and loading of the appropriate native library.
     * <p>
     * The method is thread-safe and idempotent - calling it multiple times has no additional effect
     * after the first successful call. A background thread is started to clean up old native library
     * files from previous runs.
     * <p>
     * If the library cannot be loaded, a {@link RuntimeException} is thrown with detailed information
     * about the failure, including the OS, architecture, and paths that were searched.
     * <p>
     * Example usage:
     * <pre>
     * try {
     *     JLineNativeLoader.initialize();
     *     // JLine features that require native support can be used here
     * } catch (RuntimeException e) {
     *     // Handle the case where native library cannot be loaded
     *     System.err.println("JLine native support not available: " + e.getMessage());
     * }
     * </pre>
     *
     * @return True if the JLine native library is successfully loaded; this will always
     *         be true if the method returns normally, as it throws an exception on failure.
     * @throws RuntimeException If the native library cannot be loaded for any reason.
     */
    public static synchronized boolean initialize() {
        // only cleanup before the first extract
        if (!loaded) {
            Thread cleanup = new Thread(JLineNativeLoader::cleanup, "cleanup");
            cleanup.setPriority(Thread.MIN_PRIORITY);
            cleanup.setDaemon(true);
            cleanup.start();
        }
        try {
            loadJLineNativeLibrary();
        } catch (Exception e) {
            throw new RuntimeException("Unable to load jline native library: " + e.getMessage(), e);
        }
        return loaded;
    }

    /**
     * Returns the absolute path to the loaded native library file.
     * <p>
     * This method can be used to determine which specific native library file was successfully loaded.
     * It's particularly useful for debugging and logging purposes.
     * <p>
     * Note: This method will return null if called before {@link #initialize()} or if the library
     * failed to load.
     *
     * @return The absolute path to the loaded native library file, or null if the library hasn't been loaded.
     */
    public static String getNativeLibraryPath() {
        return nativeLibraryPath;
    }

    /**
     * Returns the source URL from which the native library was loaded.
     * <p>
     * This is typically a jar:file: URL pointing to the location within the JAR file
     * from which the native library was extracted. This information can be useful for
     * debugging and logging purposes.
     * <p>
     * Note: This method will return null if called before {@link #initialize()} or if the library
     * failed to load, or if the library was loaded from the filesystem rather than extracted from a JAR.
     *
     * @return The source URL of the loaded native library, or null if not applicable or if the library hasn't been loaded.
     */
    public static String getNativeLibrarySourceUrl() {
        return nativeLibrarySourceUrl;
    }

    /**
     * Returns the temporary directory used for extracting native libraries.
     * <p>
     * The directory is determined by checking the following system properties in order:
     * <ol>
     *   <li>{@code jline.tmpdir} - Custom JLine-specific temporary directory</li>
     *   <li>{@code java.io.tmpdir} - Standard Java temporary directory</li>
     * </ol>
     *
     * @return A File object representing the temporary directory.
     */
    private static File getTempDir() {
        return new File(System.getProperty("jline.tmpdir", System.getProperty("java.io.tmpdir")));
    }

    /**
     * Cleans up old native library files from previous runs.
     * <p>
     * This method is called automatically by {@link #initialize()} to remove old native library files
     * that might have been left behind, particularly on Windows where DLL files are not always
     * properly removed on JVM exit (see bug #80).
     * <p>
     * The cleanup process:
     * <ol>
     *   <li>Scans the temporary directory for files matching the pattern "jlinenative-[version]*"</li>
     *   <li>Checks if each file has an associated lock file (.lck extension)</li>
     *   <li>Deletes files that don't have an associated lock file, as they are from previous runs</li>
     * </ol>
     * <p>
     * This method is run in a low-priority daemon thread to avoid impacting application startup time.
     */
    static void cleanup() {
        String tempFolder = getTempDir().getAbsolutePath();
        File dir = new File(tempFolder);

        File[] nativeLibFiles = dir.listFiles(new FilenameFilter() {
            private final String searchPattern = "jlinenative-" + getVersion();

            public boolean accept(File dir, String name) {
                return name.startsWith(searchPattern) && !name.endsWith(".lck");
            }
        });
        if (nativeLibFiles != null) {
            for (File nativeLibFile : nativeLibFiles) {
                File lckFile = new File(nativeLibFile.getAbsolutePath() + ".lck");
                if (!lckFile.exists()) {
                    try {
                        nativeLibFile.delete();
                    } catch (SecurityException e) {
                        logger.log(Level.INFO, "Failed to delete old native lib" + e.getMessage(), e);
                    }
                }
            }
        }
    }

    private static int readNBytes(InputStream in, byte[] b) throws IOException {
        int n = 0;
        int len = b.length;
        while (n < len) {
            int count = in.read(b, n, len - n);
            if (count <= 0) break;
            n += count;
        }
        return n;
    }

    private static String contentsEquals(InputStream in1, InputStream in2) throws IOException {
        byte[] buffer1 = new byte[8192];
        byte[] buffer2 = new byte[8192];
        int numRead1;
        int numRead2;
        while (true) {
            numRead1 = readNBytes(in1, buffer1);
            numRead2 = readNBytes(in2, buffer2);
            if (numRead1 > 0) {
                if (numRead2 <= 0) {
                    return "EOF on second stream but not first";
                }
                if (numRead2 != numRead1) {
                    return "Read size different (" + numRead1 + " vs " + numRead2 + ")";
                }
                // Otherwise same number of bytes read
                if (!Arrays.equals(buffer1, buffer2)) {
                    return "Content differs";
                }
                // Otherwise same bytes read, so continue ...
            } else {
                // Nothing more in stream 1 ...
                if (numRead2 > 0) {
                    return "EOF on first stream but not second";
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Extracts a native library from the JAR file and loads it.
     * <p>
     * This method handles the process of extracting a native library from within the JAR file
     * to a temporary location on the filesystem, and then loading it using {@link System#load(String)}.
     * <p>
     * The extraction process includes several important steps:
     * <ol>
     *   <li>Creating a unique filename for the extracted library to avoid conflicts</li>
     *   <li>Creating a lock file to indicate that the library is in use</li>
     *   <li>Extracting the library content from the JAR to the temporary file</li>
     *   <li>Setting appropriate file permissions (readable, writable, executable)</li>
     *   <li>Verifying that the extraction was successful by comparing file contents</li>
     *   <li>Loading the extracted library using {@link System#load(String)}</li>
     * </ol>
     * <p>
     * The extracted files are marked for deletion on JVM exit using {@link File#deleteOnExit()},
     * but on some platforms (particularly Windows), this may not always work reliably.
     *
     * @param  libFolderForCurrentOS The path within the JAR file to the native library for the current OS/architecture.
     * @param  libraryFileName       The filename of the native library.
     * @param  targetFolder          The target folder where the library will be extracted.
     * @return                       True if the library was successfully extracted and loaded; false otherwise.
     */
    private static boolean extractAndLoadLibraryFile(
            String libFolderForCurrentOS, String libraryFileName, String targetFolder) {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
        // Include architecture name in temporary filename in order to avoid conflicts
        // when multiple JVMs with different architectures running at the same time
        String uuid = randomUUID();
        String extractedLibFileName = String.format("jlinenative-%s-%s-%s", getVersion(), uuid, libraryFileName);
        String extractedLckFileName = extractedLibFileName + ".lck";

        File extractedLibFile = new File(targetFolder, extractedLibFileName);
        File extractedLckFile = new File(targetFolder, extractedLckFileName);

        try {
            // Extract a native library file into the target directory
            try (InputStream in = JLineNativeLoader.class.getResourceAsStream(nativeLibraryFilePath)) {
                if (!extractedLckFile.exists()) {
                    new FileOutputStream(extractedLckFile).close();
                }
                try (OutputStream out = new FileOutputStream(extractedLibFile)) {
                    copy(in, out);
                }
            } finally {
                // Delete the extracted lib file on JVM exit.
                extractedLibFile.deleteOnExit();
                extractedLckFile.deleteOnExit();
            }

            // Set executable (x) flag to enable Java to load the native library
            extractedLibFile.setReadable(true);
            extractedLibFile.setWritable(true);
            extractedLibFile.setExecutable(true);

            // Check whether the contents are properly copied from the resource folder
            try (InputStream nativeIn = JLineNativeLoader.class.getResourceAsStream(nativeLibraryFilePath)) {
                try (InputStream extractedLibIn = new FileInputStream(extractedLibFile)) {
                    String eq = contentsEquals(nativeIn, extractedLibIn);
                    if (eq != null) {
                        throw new RuntimeException(String.format(
                                "Failed to write a native library file at %s because %s", extractedLibFile, eq));
                    }
                }
            }

            // Load library
            if (loadNativeLibrary(extractedLibFile)) {
                nativeLibrarySourceUrl = JLineNativeLoader.class
                        .getResource(nativeLibraryFilePath)
                        .toExternalForm();
                return true;
            }
        } catch (IOException e) {
            log(Level.WARNING, "Unable to load JLine's native library", e);
        }
        return false;
    }

    private static String randomUUID() {
        return Long.toHexString(new Random().nextLong());
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
    }

    /**
     * Loads a native library from a specific file path.
     * <p>
     * This method attempts to load a native library using {@link System#load(String)}, which
     * requires an absolute path to the library file. If successful, it sets the
     * {@link #nativeLibraryPath} field to the absolute path of the loaded library.
     * <p>
     * The method handles the following cases:
     * <ul>
     *   <li>If the file doesn't exist, it returns false without attempting to load</li>
     *   <li>If loading fails with an {@link UnsatisfiedLinkError}, it logs the error and returns false</li>
     *   <li>If loading succeeds, it sets the path and returns true</li>
     * </ul>
     *
     * @param  libPath A File object representing the absolute path to the native library file.
     * @return         True if the library was successfully loaded; false otherwise.
     */
    private static boolean loadNativeLibrary(File libPath) {
        if (libPath.exists()) {

            try {
                String path = libPath.getAbsolutePath();
                System.load(path);
                nativeLibraryPath = path;
                return true;
            } catch (UnsatisfiedLinkError e) {
                log(
                        Level.WARNING,
                        "Failed to load native library:" + libPath.getName() + ". osinfo: "
                                + OSInfo.getNativeLibFolderPathForCurrentOS(),
                        e);
                return false;
            }

        } else {
            return false;
        }
    }

    /**
     * Core method that handles the JLine native library loading process.
     * <p>
     * This method implements the library loading strategy, attempting to load the native library
     * from various locations in a specific order. It's called by {@link #initialize()} and should
     * not be called directly.
     * <p>
     * The loading process follows this sequence:
     * <ol>
     *   <li>If the library is already loaded, return immediately</li>
     *   <li>Try loading from the custom path specified by {@code library.jline.path} system property:
     *     <ul>
     *       <li>First check {@code [library.jline.path]/[os]/[arch]/[library.name]}</li>
     *       <li>Then check {@code [library.jline.path]/[library.name]}</li>
     *     </ul>
     *   </li>
     *   <li>Try extracting and loading from the JAR file's embedded native libraries</li>
     *   <li>Try loading from each path in the {@code java.library.path} system property</li>
     *   <li>If all attempts fail, throw an exception with detailed information</li>
     * </ol>
     *
     * @throws Exception If the native library cannot be loaded from any location.
     *                   The exception includes details about the OS, architecture, and paths searched.
     */
    private static void loadJLineNativeLibrary() throws Exception {
        if (loaded) {
            return;
        }

        List<String> triedPaths = new ArrayList<String>();

        // Try loading library from library.jline.path library path */
        String jlineNativeLibraryPath = System.getProperty("library.jline.path");
        String jlineNativeLibraryName = System.getProperty("library.jline.name");
        if (jlineNativeLibraryName == null) {
            jlineNativeLibraryName = System.mapLibraryName("jlinenative");
            assert jlineNativeLibraryName != null;
            if (jlineNativeLibraryName.endsWith(".dylib")) {
                jlineNativeLibraryName = jlineNativeLibraryName.replace(".dylib", ".jnilib");
            }
        }

        if (jlineNativeLibraryPath != null) {
            String withOs = jlineNativeLibraryPath + "/" + OSInfo.getNativeLibFolderPathForCurrentOS();
            if (loadNativeLibrary(new File(withOs, jlineNativeLibraryName))) {
                loaded = true;
                return;
            } else {
                triedPaths.add(withOs);
            }

            if (loadNativeLibrary(new File(jlineNativeLibraryPath, jlineNativeLibraryName))) {
                loaded = true;
                return;
            } else {
                triedPaths.add(jlineNativeLibraryPath);
            }
        }

        // Load the os-dependent library from the jar file
        String packagePath = JLineNativeLoader.class.getPackage().getName().replace('.', '/');
        jlineNativeLibraryPath = String.format("/%s/%s", packagePath, OSInfo.getNativeLibFolderPathForCurrentOS());
        boolean hasNativeLib = hasResource(jlineNativeLibraryPath + "/" + jlineNativeLibraryName);

        if (hasNativeLib) {
            // temporary library folder
            String tempFolder = getTempDir().getAbsolutePath();
            // Try extracting the library from jar
            if (extractAndLoadLibraryFile(jlineNativeLibraryPath, jlineNativeLibraryName, tempFolder)) {
                loaded = true;
                return;
            } else {
                triedPaths.add(jlineNativeLibraryPath);
            }
        }

        // As a last resort try from java.library.path
        String javaLibraryPath = System.getProperty("java.library.path", "");
        for (String ldPath : javaLibraryPath.split(File.pathSeparator)) {
            if (ldPath.isEmpty()) {
                continue;
            }
            if (loadNativeLibrary(new File(ldPath, jlineNativeLibraryName))) {
                loaded = true;
                return;
            } else {
                triedPaths.add(ldPath);
            }
        }

        throw new Exception(String.format(
                "No native library found for os.name=%s, os.arch=%s, paths=[%s]",
                OSInfo.getOSName(), OSInfo.getArchName(), join(triedPaths, File.pathSeparator)));
    }

    private static boolean hasResource(String path) {
        return JLineNativeLoader.class.getResource(path) != null;
    }

    /**
     * Returns the major version number of the JLine library.
     * <p>
     * This method extracts the major version number from the full version string.
     * For example, if the version is "3.21.0", this method returns 3.
     * <p>
     * The version information is read from the Maven POM properties file in the JAR.
     * If the version cannot be determined, 1 is returned as a default value.
     *
     * @return The major version number of the JLine library, or 1 if it cannot be determined.
     * @see #getVersion()
     */
    public static int getMajorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 0) ? Integer.parseInt(c[0]) : 1;
    }

    /**
     * Returns the minor version number of the JLine library.
     * <p>
     * This method extracts the minor version number from the full version string.
     * For example, if the version is "3.21.0", this method returns 21.
     * <p>
     * The version information is read from the Maven POM properties file in the JAR.
     * If the version cannot be determined or doesn't have a minor component, 0 is returned as a default value.
     *
     * @return The minor version number of the JLine library, or 0 if it cannot be determined.
     * @see #getVersion()
     */
    public static int getMinorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 1) ? Integer.parseInt(c[1]) : 0;
    }

    /**
     * Returns the full version string of the JLine library.
     * <p>
     * This method retrieves the version information from the Maven POM properties file
     * in the JAR at "/META-INF/maven/org.jline/jline-native/pom.properties".
     * <p>
     * The version string is cleaned to include only numeric values and dots (e.g., "3.21.0").
     * If the version information cannot be determined (e.g., the properties file is missing
     * or cannot be read), "unknown" is returned.
     * <p>
     * This version information is used in the naming of temporary native library files to
     * ensure proper versioning and avoid conflicts between different JLine versions.
     *
     * @return The version string of the JLine library, or "unknown" if it cannot be determined.
     */
    public static String getVersion() {
        URL versionFile = JLineNativeLoader.class.getResource("/META-INF/maven/org.jline/jline-native/pom.properties");

        String version = "unknown";
        try {
            if (versionFile != null) {
                Properties versionData = new Properties();
                versionData.load(versionFile.openStream());
                version = versionData.getProperty("version", version);
                version = version.trim().replaceAll("[^0-9.]", "");
            }
        } catch (IOException e) {
            log(Level.WARNING, "Unable to load jline-native version", e);
        }
        return version;
    }

    private static String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : list) {
            if (first) first = false;
            else sb.append(separator);

            sb.append(item);
        }
        return sb.toString();
    }

    private static void log(Level level, String message, Throwable t) {
        if (logger.isLoggable(level)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(level, message, t);
            } else {
                logger.log(level, message + " (caused by: " + t + ", enable debug logging for stacktrace)");
            }
        }
    }
}
