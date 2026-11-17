/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.File;
import java.util.Locale;

/**
 * Utility class for operating system detection and OS-specific operations.
 *
 * <p>
 * The OSUtils class provides constants and methods for detecting the current operating
 * system and performing OS-specific operations. It helps JLine components adapt their
 * behavior based on the platform they're running on, which is particularly important
 * for terminal handling where different operating systems have different terminal
 * implementations and capabilities.
 * </p>
 *
 * <p>
 * This class includes constants for detecting common operating systems:
 * </p>
 * <ul>
 *   <li>Windows - Including detection of Cygwin and MSYS environments</li>
 *   <li>Linux</li>
 *   <li>macOS (OSX)</li>
 *   <li>AIX</li>
 *   <li>Other Unix-like systems</li>
 * </ul>
 *
 * <p>
 * It also provides utility methods for working with file paths, environment variables,
 * and other OS-specific features that affect terminal behavior.
 * </p>
 *
 * <p>
 * This class is used throughout JLine to ensure correct behavior across different
 * platforms, particularly for terminal detection, path handling, and native library
 * loading.
 * </p>
 */
public class OSUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private OSUtils() {
        // Utility class
    }

    public static final boolean IS_LINUX =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux");

    public static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

    public static final boolean IS_OSX =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");

    public static final boolean IS_AIX =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("aix");

    public static final boolean IS_CYGWIN =
            IS_WINDOWS && System.getenv("PWD") != null && System.getenv("PWD").startsWith("/");

    public static final boolean IS_MSYSTEM = IS_WINDOWS && System.getenv("MSYSTEM") != null;

    public static final boolean IS_WSL = System.getenv("WSL_DISTRO_NAME") != null;

    public static final boolean IS_WSL1 = IS_WSL && System.getenv("WSL_INTEROP") == null;

    public static final boolean IS_WSL2 = IS_WSL && !IS_WSL1;

    public static final boolean IS_CONEMU = IS_WINDOWS && System.getenv("ConEmuPID") != null;

    // On Windows, these are null when the command is not found on PATH (see findCommand).
    public static String TTY_COMMAND;
    public static String STTY_COMMAND;
    public static String STTY_F_OPTION;
    public static String INFOCMP_COMMAND;
    public static String TEST_COMMAND;

    private static boolean isExecutable(File f) {
        return f.canExecute() && !f.isDirectory();
    }

    /**
     * Looks {@code name} up in the directories of {@code path}.
     * <p>
     * On Windows the {@code .exe} file is searched for and the result is {@code null} when it is
     * not on the path: a bare name would be resolved by {@code CreateProcess}, which looks in the
     * current directory before {@code PATH}, so a {@code tty.exe} or {@code infocmp.exe} dropped
     * in the working directory would be run. Elsewhere the bare name is returned, which the JDK
     * resolves through {@code PATH} only.
     */
    static String findCommand(String name, String path, boolean windows) {
        String file = windows ? name + ".exe" : name;
        if (path != null) {
            for (String p : path.split(File.pathSeparator)) {
                File f = new File(p, file);
                if (isExecutable(f)) {
                    return f.getAbsolutePath();
                }
            }
        }
        return windows ? null : name;
    }

    static {
        String path = System.getenv("PATH");
        TTY_COMMAND = findCommand("tty", path, IS_WINDOWS);
        STTY_COMMAND = findCommand("stty", path, IS_WINDOWS);
        STTY_F_OPTION = IS_OSX ? "-f" : "-F";
        INFOCMP_COMMAND = findCommand("infocmp", path, IS_WINDOWS);
        TEST_COMMAND = findCommand("test", path, IS_WINDOWS);
    }
}
