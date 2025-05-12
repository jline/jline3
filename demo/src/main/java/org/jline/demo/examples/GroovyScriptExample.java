/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Example demonstrating a Groovy script in JLine.
 * This is not a runnable Java class but shows the structure of a Groovy script.
 */
public class GroovyScriptExample {

    // SNIPPET_START: GroovyScriptExample
    /**
     * Example of a JLine Groovy script (hello.groovy)
     *
     * Groovy scripts are executed by the REPL console and can contain
     * Groovy code that interacts with the console.
     *
     * This is not actual Java code but a representation of what a Groovy script looks like.
     */
    public static void groovyScriptExample() throws IOException {
        // This is what a Groovy script (hello.groovy) might look like:
        String groovyScript = "#!/usr/bin/env groovy\n" + "\n"
                + "// This is a simple JLine Groovy script\n"
                + "// It demonstrates how to use parameters and execute commands\n"
                + "\n"
                + "// Access script parameters using the _args list\n"
                + "def name = _args.size() > 0 ? _args[0] : \"World\"\n"
                + "\n"
                + "// Print directly using Groovy\n"
                + "println \"Hello, ${name}!\"\n"
                + "\n"
                + "// Execute console commands using SystemRegistry\n"
                + "import org.jline.console.SystemRegistry\n"
                + "\n"
                + "// Create a map to print\n"
                + "def user = [name: name, greeting: \"Hello\", count: 5]\n"
                + "\n"
                + "// Print the map using the prnt command\n"
                + "SystemRegistry.get().invoke(\"prnt\", \"-s\", \"JSON\", user)\n"
                + "\n"
                + "// Loop example\n"
                + "println \"Counting to ${user.count}:\"\n"
                + "for (i in 1..user.count) {\n"
                + "    println \"  ${i}\"\n"
                + "}\n"
                + "\n"
                + "// Return a value from the script\n"
                + "return \"Script completed successfully!\"\n";

        // Write the example script to a file for demonstration purposes
        Path scriptPath = Paths.get("hello.groovy");
        Files.write(scriptPath, groovyScript.getBytes());

        System.out.println("Created example Groovy script: " + scriptPath.toAbsolutePath());
        System.out.println("You can run this script in a JLine REPL console with:");
        System.out.println("  ./hello.groovy YourName");
    }
    // SNIPPET_END: GroovyScriptExample

    public static void main(String[] args) throws IOException {
        groovyScriptExample();
    }
}
