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
 * Example demonstrating multiple prompt types in a single session.
 */
public class PromptMixedTypesExample {

    // SNIPPET_START: PromptMixedTypesExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        // Create a comprehensive setup wizard
        PromptBuilder builder = prompter.newBuilder();

        // Step 1: Project type selection
        builder.createListPrompt()
                .name("project_type")
                .message("What type of project are you creating?")
                .newItem("web")
                .text("Web Application")
                .add()
                .newItem("api")
                .text("REST API")
                .add()
                .newItem("cli")
                .text("Command Line Tool")
                .add()
                .newItem("library")
                .text("Library/Framework")
                .add()
                .addPrompt();

        // Step 2: Project details
        builder.createInputPrompt()
                .name("project_name")
                .message("Enter project name:")
                .defaultValue("my-awesome-project")
                .addPrompt();

        // Step 3: Features selection
        builder.createCheckboxPrompt()
                .name("features")
                .message("Select features to include:")
                .newItem("database")
                .text("Database Integration")
                .checked(true)
                .add()
                .newItem("auth")
                .text("Authentication")
                .add()
                .newItem("testing")
                .text("Unit Testing")
                .checked(true)
                .add()
                .newItem("docker")
                .text("Docker Support")
                .add()
                .newItem("docs")
                .text("Documentation")
                .checked(true)
                .add()
                .addPrompt();

        // Step 4: Environment choice
        builder.createChoicePrompt()
                .name("environment")
                .message("Choose target environment:")
                .newChoice("dev")
                .text("Development")
                .key('d')
                .defaultChoice(true)
                .add()
                .newChoice("staging")
                .text("Staging")
                .key('s')
                .add()
                .newChoice("prod")
                .text("Production")
                .key('p')
                .add()
                .addPrompt();

        // Step 5: Final confirmation
        builder.createConfirmPrompt()
                .name("create")
                .message("Create project with these settings?")
                .defaultValue(true)
                .addPrompt();

        try {
            List<AttributedString> header = Arrays.asList(
                    new AttributedString("Project Setup Wizard"),
                    new AttributedString("Follow the prompts to configure your new project"));

            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

            // Process results
            ListResult projectType = (ListResult) results.get("project_type");
            InputResult projectName = (InputResult) results.get("project_name");
            CheckboxResult features = (CheckboxResult) results.get("features");
            ChoiceResult environment = (ChoiceResult) results.get("environment");
            ConfirmResult create = (ConfirmResult) results.get("create");

            if (create.isConfirmed()) {
                System.out.println("\n✓ Project Configuration:");
                System.out.println("  Type: " + projectType.getSelectedId());
                System.out.println("  Name: " + projectName.getInput());
                System.out.println("  Features: " + features.getSelectedIds());
                System.out.println("  Environment: " + environment.getSelectedId());
                System.out.println("\nProject created successfully!");
            } else {
                System.out.println("Project creation cancelled.");
            }
        } catch (Exception e) {
            System.out.println("Setup wizard cancelled");
        }
    }
    // SNIPPET_END: PromptMixedTypesExample
}
