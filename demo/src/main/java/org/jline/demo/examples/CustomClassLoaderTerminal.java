/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating various classloader scenarios with TerminalBuilder.
 */
public class CustomClassLoaderTerminal {

    public static void main(String[] args) throws IOException {
        basicCustomClassLoader();
        pluginClassLoader();
        osgiClassLoader();
        moduleSystemClassLoader();
        testClassLoader();
        fallbackStrategy();
    }

    public static void basicCustomClassLoader() throws IOException {
        // SNIPPET_START: CustomClassLoaderTerminal
        // Get your application's custom classloader
        ClassLoader customClassLoader = CustomClassLoaderTerminal.class.getClassLoader();

        // Create terminal with custom classloader
        Terminal terminal = TerminalBuilder.builder()
                .classLoader(customClassLoader)
                .system(true)
                .build();
        // SNIPPET_END: CustomClassLoaderTerminal

        System.out.println("Terminal with custom classloader created: "
                + terminal.getClass().getSimpleName());
        terminal.close();
    }

    public static void pluginClassLoader() throws IOException {
        // SNIPPET_START: PluginClassLoaderTerminal
        // In a plugin or module context
        ClassLoader pluginClassLoader = CustomClassLoaderTerminal.class.getClassLoader();

        Terminal terminal = TerminalBuilder.builder()
                .classLoader(pluginClassLoader)
                .system(true)
                .build();
        // SNIPPET_END: PluginClassLoaderTerminal

        System.out.println("Plugin terminal created: " + terminal.getClass().getSimpleName());
        terminal.close();
    }

    public static void osgiClassLoader() throws IOException {
        // SNIPPET_START: OSGiClassLoaderTerminal
        // In an OSGi bundle context, you would use:
        // Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        // ClassLoader bundleClassLoader = bundle.adapt(BundleWiring.class).getClassLoader();

        // For this demo, we'll use the current classloader
        ClassLoader bundleClassLoader = CustomClassLoaderTerminal.class.getClassLoader();

        Terminal terminal = TerminalBuilder.builder()
                .classLoader(bundleClassLoader)
                .system(true)
                .build();
        // SNIPPET_END: OSGiClassLoaderTerminal

        System.out.println("OSGi terminal created: " + terminal.getClass().getSimpleName());
        terminal.close();
    }

    public static void moduleSystemClassLoader() throws IOException {
        // SNIPPET_START: ModuleSystemClassLoaderTerminal
        // Get the classloader from your module system
        // ClassLoader moduleClassLoader = MyModuleSystem.getClassLoader("jline-module");

        // For this demo, we'll use the current classloader
        ClassLoader moduleClassLoader = CustomClassLoaderTerminal.class.getClassLoader();

        Terminal terminal = TerminalBuilder.builder()
                .classLoader(moduleClassLoader)
                .system(true)
                .build();
        // SNIPPET_END: ModuleSystemClassLoaderTerminal

        System.out.println(
                "Module system terminal created: " + terminal.getClass().getSimpleName());
        terminal.close();
    }

    public static void testClassLoader() throws IOException {
        // SNIPPET_START: TestClassLoaderTerminal
        // Create a test classloader that can access JLine providers
        ClassLoader testClassLoader = new URLClassLoader(
                new URL[] {
                    /* paths to JLine jars */
                },
                CustomClassLoaderTerminal.class.getClassLoader());

        Terminal terminal = TerminalBuilder.builder()
                .classLoader(testClassLoader)
                .dumb(true) // Use dumb terminal for testing
                .build();
        // SNIPPET_END: TestClassLoaderTerminal

        System.out.println("Test terminal created: " + terminal.getClass().getSimpleName());
        terminal.close();
    }

    public static Terminal createTerminalWithFallback() throws IOException {
        // SNIPPET_START: FallbackClassLoaderStrategy
        ClassLoader[] classLoaders = {
            Thread.currentThread().getContextClassLoader(),
            CustomClassLoaderTerminal.class.getClassLoader(),
            ClassLoader.getSystemClassLoader()
        };

        for (ClassLoader cl : classLoaders) {
            try {
                return TerminalBuilder.builder().classLoader(cl).system(true).build();
            } catch (Exception e) {
                // Try next classloader
                System.out.println("Failed with classloader " + cl + ": " + e.getMessage());
            }
        }

        // Final fallback to dumb terminal
        return TerminalBuilder.builder().dumb(true).build();
        // SNIPPET_END: FallbackClassLoaderStrategy
    }

    public static void fallbackStrategy() throws IOException {
        Terminal terminal = createTerminalWithFallback();
        System.out.println("Terminal created with fallback strategy: "
                + terminal.getClass().getSimpleName());
        terminal.close();
    }
}
