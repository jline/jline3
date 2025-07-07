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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

/**
 * Example demonstrating best practices for prompt usage.
 */
public class PromptBestPracticesExample {

    // SNIPPET_START: PromptBestPracticesExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        // Best Practice 1: Use descriptive but concise messages
        PromptBuilder builder = prompter.newBuilder();

        builder.createListPrompt()
                .name("deployment")
                .message("Select deployment target:") // Clear and concise
                .newItem("local")
                .text("Local Development")
                .add()
                .newItem("staging")
                .text("Staging Environment")
                .add()
                .newItem("production")
                .text("Production Environment")
                .add()
                .addPrompt();

        // Best Practice 2: Provide sensible defaults
        builder.createInputPrompt()
                .name("port")
                .message("Enter server port:")
                .defaultValue("8080") // Common default
                .addPrompt();

        // Best Practice 3: Group related options logically
        builder.createCheckboxPrompt()
                .name("security")
                .message("Security features:")
                .newItem("https")
                .text("HTTPS/TLS")
                .checked(true)
                .add() // Security defaults enabled
                .newItem("auth")
                .text("Authentication")
                .checked(true)
                .add()
                .newItem("cors")
                .text("CORS Protection")
                .add()
                .newItem("rate_limit")
                .text("Rate Limiting")
                .add()
                .addPrompt();

        // Best Practice 4: Use choice prompts for quick decisions
        builder.createChoicePrompt()
                .name("log_level")
                .message("Set log level:")
                .newChoice("debug")
                .text("Debug")
                .key('d')
                .add()
                .newChoice("info")
                .text("Info")
                .key('i')
                .defaultChoice(true)
                .add() // Sensible default
                .newChoice("warn")
                .text("Warning")
                .key('w')
                .add()
                .newChoice("error")
                .text("Error")
                .key('e')
                .add()
                .addPrompt();

        try {
            // Best Practice 5: Provide helpful headers
            List<AttributedString> header = Arrays.asList(
                    new AttributedString("Application Configuration"),
                    new AttributedString("Configure your application settings below"));

            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

            // Best Practice 6: Validate and provide feedback
            ListResult deployment = (ListResult) results.get("deployment");
            InputResult port = (InputResult) results.get("port");
            CheckboxResult security = (CheckboxResult) results.get("security");
            ChoiceResult logLevel = (ChoiceResult) results.get("log_level");

            // Validate port number
            try {
                int portNum = Integer.parseInt(port.getInput());
                if (portNum < 1 || portNum > 65535) {
                    System.err.println("Warning: Port number should be between 1 and 65535");
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid port number format");
                return;
            }

            // Provide clear summary
            System.out.println("\n✓ Configuration Summary:");
            System.out.println("  Deployment: " + deployment.getSelectedId());
            System.out.println("  Port: " + port.getInput());
            System.out.println("  Security: " + security.getSelectedIds());
            System.out.println("  Log Level: " + logLevel.getSelectedId());

        } catch (Exception e) {
            // Best Practice 7: Handle cancellation gracefully
            System.out.println("Configuration cancelled. Using default settings.");
        }
    }
    // SNIPPET_END: PromptBestPracticesExample
}
