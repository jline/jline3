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

/**
 * Set the system properties, library.jline.path, library.jline.name,
 * appropriately so that jline can find *.dll, *.jnilib and
 * *.so files, according to the current OS (win, linux, mac).
 * <p>
 * The library files are automatically extracted from this project's package
 * (JAR).
 * <p>
 * usage: call {@link #initialize()} before using jline.
 */
public class JLineNativeLoader {

    private static boolean loaded = false;
    private static String nativeLibraryPath;
    private static String nativeLibrarySourceUrl;

    /**
     * Loads jline native library.
     *
     * @return True if jline native library is successfully loaded; false
     *         otherwise.
     */
    public static synchronized boolean initialize() {
        // only cleanup before the first extract
        if (!loaded) {
            cleanup();
        }
        try {
            loadJLineNativeLibrary();
        } catch (Exception e) {
            throw new RuntimeException("Unable to load jline native library", e);
        }
        return loaded;
    }

    public static String getNativeLibraryPath() {
        return nativeLibraryPath;
    }

    public static String getNativeLibrarySourceUrl() {
        return nativeLibrarySourceUrl;
    }

    private static File getTempDir() {
        return new File(System.getProperty("jline.tmpdir", System.getProperty("java.io.tmpdir")));
    }

    /**
     * Deleted old native libraries e.g. on Windows the DLL file is not removed
     * on VM-Exit (bug #80)
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
                        System.err.println("Failed to delete old native lib" + e.getMessage());
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
     * Extracts and loads the specified library file to the target folder
     *
     * @param  libFolderForCurrentOS Library path.
     * @param  libraryFileName       Library name.
     * @param  targetFolder          Target folder.
     * @return
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
            System.err.println(e.getMessage());
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
     * Loads native library using the given path and name of the library.
     *
     * @param  libPath Path of the native library.
     * @return         True for successfully loading; false otherwise.
     */
    private static boolean loadNativeLibrary(File libPath) {
        if (libPath.exists()) {

            try {
                String path = libPath.getAbsolutePath();
                System.load(path);
                nativeLibraryPath = path;
                return true;
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Failed to load native library:" + libPath.getName() + ". osinfo: "
                        + OSInfo.getNativeLibFolderPathForCurrentOS());
                System.err.println(e);
                return false;
            }

        } else {
            return false;
        }
    }

    /**
     * Loads jline library using given path and name of the library.
     *
     * @throws
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
     * @return The major version of the jline library.
     */
    public static int getMajorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 0) ? Integer.parseInt(c[0]) : 1;
    }

    /**
     * @return The minor version of the jline library.
     */
    public static int getMinorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 1) ? Integer.parseInt(c[1]) : 0;
    }

    /**
     * @return The version of the jline library.
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
            System.err.println(e);
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
}
