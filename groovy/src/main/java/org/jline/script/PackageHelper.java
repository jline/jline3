/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.script;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
/**
 *
 * https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection/22462785#22462785
 *
 */
public class PackageHelper {
    /**
     * Private helper method
     *
     * @param directory
     *            The directory to start with
     * @param pckgname
     *            The package name to search for. Will be needed for getting the
     *            Class object.
     * @param classes
     *            if a file isn't loaded but still is in the directory
     * @param classesOnly
     *            if true returns only classes of the pckgname
     * @throws ClassNotFoundException
     *             if something went wrong
     */
    private static void checkDirectory(File directory, String pckgname,
            List<Class<?>> classes, boolean classesOnly) throws ClassNotFoundException {
        File tmpDirectory;

        if (directory.exists() && directory.isDirectory()) {
            final String[] files = directory.list();

            for (final String file : files != null ? files : new String[0]) {
                if (file.endsWith(".class")) {
                    try {
                        classes.add(Class.forName(pckgname + '.'
                                + file.substring(0, file.length() - 6)));
                    } catch (final NoClassDefFoundError e) {
                        // e.printStackTrace();
                        // do nothing. this class hasn't been found by the
                        // loader, and we don't care.
                    }
                } else if (!classesOnly && (tmpDirectory = new File(directory, file)).isDirectory()) {
                    checkDirectory(tmpDirectory, pckgname + "." + file, classes, false);
                }
            }
        }
    }

    /**
     * Private helper method.
     *
     * @param connection
     *            the connection to the jar
     * @param pckgname
     *            the package name to search for
     * @param classes
     *            the current ArrayList of all classes. This method will simply
     *            add new classes.
     * @param classesOnly
     *            if true returns only classes of the pckgname
     * @throws ClassNotFoundException
     *             if a file isn't loaded but still is in the jar file
     * @throws IOException
     *             if it can't correctly read from the jar file.
     */
    private static void checkJarFile(JarURLConnection connection,
            String pckgname, List<Class<?>> classes, boolean classesOnly)
            throws IOException, ClassNotFoundException {
        final JarFile jarFile = connection.getJarFile();
        final Enumeration<JarEntry> entries = jarFile.entries();
        String name;

        for (JarEntry jarEntry; entries.hasMoreElements() && ((jarEntry = entries.nextElement()) != null);) {
            name = jarEntry.getName();
            if (name.contains(".class")) {
                name = name.substring(0, name.length() - 6).replace('/', '.');
                if (classesOnly) {
                    String namepckg = name.substring(0, name.lastIndexOf("."));
                    if (pckgname.equals(namepckg) && !name.contains("$")) {
                        classes.add(Class.forName(name));
                    }
                } else if (name.contains(pckgname)) {
                    classes.add(Class.forName(name));
                }
            }
        }
    }

    /**
     * Attempts to list all the classes in the specified package as determined
     * by the context class loader
     *
     * @param pckgname
     *            the package name to search
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException
     *             if something went wrong
     */
    public static List<Class<?>> getClassesForPackage(String pckgname) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        boolean classesOnly = pckgname.endsWith(".*");
        if (classesOnly) {
            pckgname = pckgname.substring(0, pckgname.length() - 2);
        }

        try {
            final ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            final Enumeration<URL> resources = cld.getResources(pckgname.replace('.', '/'));
            URLConnection connection;

            for (URL url; resources.hasMoreElements()
                    && ((url = resources.nextElement()) != null);) {
                try {
                    connection = url.openConnection();

                    if (connection instanceof JarURLConnection) {
                        checkJarFile((JarURLConnection) connection, pckgname, classes, classesOnly);
                    } else if (connection.getClass().getCanonicalName().equals("sun.net.www.protocol.file.FileURLConnection")) {
                        try {
                            checkDirectory(
                                    new File(URLDecoder.decode(url.getPath(), "UTF-8")), pckgname, classes, classesOnly);
                        } catch (final UnsupportedEncodingException ex) {
                            throw new ClassNotFoundException(
                                    pckgname
                                            + " does not appear to be a valid package (Unsupported encoding)",
                                    ex);
                        }
                    } else {
                        throw new ClassNotFoundException(pckgname + " ("
                                + url.getPath()
                                + ") does not appear to be a valid package");
                    }
                } catch (final IOException ioex) {
                    throw new ClassNotFoundException(
                            "IOException was thrown when trying to get all resources for "
                                    + pckgname, ioex);
                }
            }
        } catch (final NullPointerException ex) {
            throw new ClassNotFoundException(
                    pckgname
                            + " does not appear to be a valid package (Null pointer exception)",
                    ex);
        } catch (final IOException ioex) {
            throw new ClassNotFoundException(
                    "IOException was thrown when trying to get all resources for "
                            + pckgname, ioex);
        } catch (final NoClassDefFoundError e) {
            throw new ClassNotFoundException(
                    "NoClassDefFoundError was thrown when trying to get all resources for "
                            + pckgname, e);
        }
        return classes;
    }
}
