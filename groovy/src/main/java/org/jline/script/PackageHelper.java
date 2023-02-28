/*
 * Copyright (c) 2002-2021, the original author(s).
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
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import groovy.lang.GroovyClassLoader;

/**
 *
 * https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection/22462785#22462785
 *
 */
public class PackageHelper {
    private enum ClassesToScann {
        ALL,
        PACKAGE_ALL,
        PACKAGE_CLASS
    }

    private enum ClassOutput {
        NAME,
        CLASS,
        MIXED
    }
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
     * @param scann
     *            determinate which classes will be added
     */
    private static void checkDirectory(
            File directory,
            String pckgname,
            List<Object> classes,
            ClassesToScann scann,
            ClassOutput outType,
            Function<String, Class<?>> classResolver) {
        File tmpDirectory;

        if (directory.exists() && directory.isDirectory()) {
            final String[] files = directory.list();

            for (final String file : files != null ? files : new String[0]) {
                if (file.endsWith(".class")) {
                    String className = pckgname + '.' + file.substring(0, file.length() - 6);
                    if (outType != ClassOutput.CLASS) {
                        classes.add(className);
                    } else {
                        addClass(className, classes, classResolver);
                    }
                } else if (scann == ClassesToScann.ALL && (tmpDirectory = new File(directory, file)).isDirectory()) {
                    checkDirectory(
                            tmpDirectory,
                            pckgname + "." + file,
                            classes,
                            ClassesToScann.ALL,
                            ClassOutput.NAME,
                            classResolver);
                }
            }
        }
    }

    /**
     * Private helper method.
     *
     * @param jarFile
     *            the jar file
     * @param pckgname
     *            the package name to search for
     * @param classes
     *            the current ArrayList of all classes. This method will simply
     *            add new classes.
     * @param scann
     *            determinate which classes will be added
     * @throws IOException
     *             if it can't correctly read from the jar file.
     */
    private static void checkJarFile(
            final JarFile jarFile,
            String pckgname,
            List<Object> classes,
            ClassesToScann scann,
            ClassOutput outType,
            Function<String, Class<?>> classResolver)
            throws IOException {
        final Enumeration<JarEntry> entries = jarFile.entries();
        String name;

        for (JarEntry jarEntry; entries.hasMoreElements() && ((jarEntry = entries.nextElement()) != null); ) {
            name = jarEntry.getName();
            if (name.contains(".class")) {
                name = name.substring(0, name.length() - 6).replace('/', '.');
                if (scann != ClassesToScann.ALL) {
                    String namepckg = name.substring(0, name.lastIndexOf("."));
                    if (pckgname.equals(namepckg)
                            && ((scann == ClassesToScann.PACKAGE_CLASS && !name.contains("$"))
                                    || scann == ClassesToScann.PACKAGE_ALL)) {
                        if (outType == ClassOutput.CLASS) {
                            addClass(name, classes, classResolver);
                        } else {
                            classes.add(name);
                        }
                    }
                } else if (name.contains(pckgname)) {
                    if (outType == ClassOutput.CLASS
                            || (outType == ClassOutput.MIXED
                                    && Character.isUpperCase(name.charAt(pckgname.length() + 1)))) {
                        addClass(name, classes, classResolver);
                    } else {
                        classes.add(name);
                    }
                }
            }
        }
    }

    private static void addClass(String className, List<Object> classes, Function<String, Class<?>> classResolver) {
        if (classResolver != null) {
            Class<?> clazz = classResolver.apply(className);
            if (clazz != null) {
                classes.add(clazz);
            }
        } else {
            classes.add(className);
        }
    }

    private static Class<?> classResolver(String name) {
        Class<?> out = null;
        try {
            out = Class.forName(name);
        } catch (Exception | Error ignore) {

        }
        return out;
    }

    private static class PackageNameParser {
        private final String packageName;
        private final ClassesToScann classesToScann;
        private final ClassOutput outType;

        public PackageNameParser(String packageName) {
            if (packageName.endsWith(".*")) {
                classesToScann = ClassesToScann.PACKAGE_CLASS;
                outType = ClassOutput.CLASS;
                this.packageName = packageName.substring(0, packageName.length() - 2);
            } else if (packageName.endsWith(".**")) {
                this.packageName = packageName.substring(0, packageName.length() - 3);
                classesToScann = ClassesToScann.PACKAGE_ALL;
                outType = ClassOutput.CLASS;
            } else {
                classesToScann = ClassesToScann.ALL;
                this.packageName = packageName;
                outType = ClassOutput.MIXED;
            }
        }

        public String packageName() {
            return packageName;
        }

        public ClassesToScann classesToScann() {
            return classesToScann;
        }

        public ClassOutput outType() {
            return outType;
        }
    }

    private static Enumeration<URL> toEnumeration(final URL[] urls) {
        return (new Enumeration<URL>() {
            final int size = urls.length;

            int cursor;

            public boolean hasMoreElements() {
                return (cursor < size);
            }

            public URL nextElement() {
                return urls[cursor++];
            }
        });
    }

    private static Enumeration<URL> getResources(final ClassLoader classLoader, String packageName)
            throws ClassNotFoundException {
        try {
            return classLoader.getResources(packageName.replace('.', '/'));
        } catch (final NullPointerException ex) {
            throw new ClassNotFoundException(
                    packageName + " does not appear to be a valid package (Null pointer exception)", ex);
        } catch (final IOException ioex) {
            throw new ClassNotFoundException(
                    "IOException was thrown when trying to get all resources for " + packageName, ioex);
        }
    }

    /**
     * Attempts to list all the class names in the specified package as determined
     * by the Groovy class loader classpath
     *
     * @param pckgname
     *            the package name to search
     * @param classLoader class loader
     * @return a list of class names that exist within that package
     */
    @SuppressWarnings("unchecked")
    public static List<String> getClassNamesForPackage(String pckgname, ClassLoader classLoader) {
        try {
            PackageNameParser pnp = new PackageNameParser(pckgname);
            Enumeration<URL> resources = getResources(classLoader, pnp.packageName());
            return (List<String>) (Object)
                    getClassesForPackage(pnp.packageName(), resources, pnp.classesToScann(), ClassOutput.NAME, null);
        } catch (Exception ignore) {
        }
        return new ArrayList<>();
    }

    /**
     * Attempts to list all the classes in the specified package as determined
     * by the Groovy class loader classpath
     *
     * @param pckgname
     *            the package name to search
     * @param classLoader Groovy class loader
     * @param classResolver resolve class from class name
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException
     *             if something went wrong
     */
    public static List<Object> getClassesForPackage(
            String pckgname, GroovyClassLoader classLoader, Function<String, Class<?>> classResolver)
            throws ClassNotFoundException {
        PackageNameParser pnp = new PackageNameParser(pckgname);
        Enumeration<URL> resources = toEnumeration(classLoader.getURLs());
        return getClassesForPackage(pnp.packageName(), resources, pnp.classesToScann(), pnp.outType(), classResolver);
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
    public static List<Object> getClassesForPackage(String pckgname) throws ClassNotFoundException {
        final ClassLoader cld = Thread.currentThread().getContextClassLoader();
        if (cld == null) {
            throw new ClassNotFoundException("Can't get class loader.");
        }
        PackageNameParser pnp = new PackageNameParser(pckgname);
        Enumeration<URL> resources = getResources(cld, pnp.packageName());
        return getClassesForPackage(
                pnp.packageName(), resources, pnp.classesToScann(), pnp.outType(), PackageHelper::classResolver);
    }

    private static List<Object> getClassesForPackage(
            String pckgname,
            final Enumeration<URL> resources,
            ClassesToScann scann,
            ClassOutput outType,
            Function<String, Class<?>> classResolver)
            throws ClassNotFoundException {
        List<Object> classes = new ArrayList<>();
        URLConnection connection;

        for (URL url; resources.hasMoreElements() && ((url = resources.nextElement()) != null); ) {
            try {
                connection = url.openConnection();

                if (connection instanceof JarURLConnection) {
                    checkJarFile(
                            ((JarURLConnection) connection).getJarFile(),
                            pckgname,
                            classes,
                            scann,
                            outType,
                            classResolver);
                } else if (connection
                        .getClass()
                        .getCanonicalName()
                        .equals("sun.net.www.protocol.file.FileURLConnection")) {
                    try {
                        File file = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
                        if (file.exists()) {
                            if (file.isDirectory()) {
                                checkDirectory(file, pckgname, classes, scann, outType, classResolver);
                            } else if (file.getName().endsWith(".jar")) {
                                checkJarFile(new JarFile(file), pckgname, classes, scann, outType, classResolver);
                            }
                        }
                    } catch (final UnsupportedEncodingException ex) {
                        throw new ClassNotFoundException(
                                pckgname + " does not appear to be a valid package (Unsupported encoding)", ex);
                    }
                } else {
                    throw new ClassNotFoundException(
                            pckgname + " (" + url.getPath() + ") does not appear to be a valid package");
                }
            } catch (final IOException ioex) {
                throw new ClassNotFoundException(
                        "IOException was thrown when trying to get all resources for " + pckgname, ioex);
            }
        }
        return classes;
    }
}
