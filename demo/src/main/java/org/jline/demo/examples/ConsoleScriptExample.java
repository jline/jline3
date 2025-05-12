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
 * Example demonstrating a console script in JLine.
 * This is not a runnable Java class but shows the structure of a console script.
 */
public class ConsoleScriptExample {

    // SNIPPET_START: ConsoleScriptExample
    /**
     * Example of a JLine console script (hello.jline)
     *
     * Console scripts are executed by the REPL console and can contain
     * any commands that you would type interactively.
     *
     * This is not actual Java code but a representation of what a console script looks like.
     */
    public static void consoleScriptExample() throws IOException {
        // This is what a console script (hello.jline) might look like:
        String consoleScript = "#!/usr/bin/env jline\n" + "\n"
                + "# This is a simple JLine console script\n"
                + "# It demonstrates how to use parameters and execute commands\n"
                + "\n"
                + "# Access script parameters using $1, $2, etc.\n"
                + "name=$1\n"
                + "if [ -z \"$name\" ]; then\n"
                + "    name=\"World\"\n"
                + "fi\n"
                + "\n"
                + "# Execute commands just like in interactive mode\n"
                + ":echo \"Hello, $name!\"\n"
                + "\n"
                + "# You can use variables\n"
                + "count=5\n"
                + ":echo \"Counting to $count:\"\n"
                + "\n"
                + "# Loop example\n"
                + "i=1\n"
                + "while [ $i -le $count ]; do\n"
                + "    :echo \"  $i\"\n"
                + "    i=$((i+1))\n"
                + "done\n"
                + "\n"
                + "# Return a value from the script\n"
                + "exit \"Script completed successfully!\"\n";

        // Write the example script to a file for demonstration purposes
        Path scriptPath = Paths.get("hello.jline");
        Files.write(scriptPath, consoleScript.getBytes());

        System.out.println("Created example console script: " + scriptPath.toAbsolutePath());
        System.out.println("You can run this script in a JLine REPL console with:");
        System.out.println("  ./hello.jline YourName");
    }
    // SNIPPET_END: ConsoleScriptExample

    public static void main(String[] args) throws IOException {
        consoleScriptExample();
    }
}
