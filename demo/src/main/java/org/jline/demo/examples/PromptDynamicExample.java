/*
 * Copyright (c) 2024-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jline.prompt.*;
import org.jline.prompt.impl.DefaultPromptBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Enhanced dynamic prompt example showcasing all new prompt types.
 * Demonstrates password, number, search, editor prompts alongside traditional ones.
 * This is an enhanced version of the BasicDynamic ConsoleUI demo.
 */
public class PromptDynamicExample {
    private static final AttributedStyle ITALIC_GREEN =
            AttributedStyle.DEFAULT.italic().foreground(2);
    private static final AttributedStyle BOLD_RED = AttributedStyle.BOLD.foreground(1);

    public static void main(String[] args) {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Thread executeThread = Thread.currentThread();
            terminal.handle(Terminal.Signal.INT, signal -> executeThread.interrupt());

            if (terminal.getType().equals(Terminal.TYPE_DUMB)
                    || terminal.getType().equals(Terminal.TYPE_DUMB_COLOR)) {
                System.out.println(terminal.getName() + ": " + terminal.getType());
                throw new IllegalStateException("Dumb terminal detected.\nPrompt requires real terminal to work!\n"
                        + "Note: On Windows Jansi or JNA library must be included in classpath.");
            }

            // Create prompter with appropriate config for the platform
            PrompterConfig config = PrompterConfig.defaults().withCancellableFirstPrompt(true);

            Prompter prompter = PrompterFactory.create(terminal, config);

            List<AttributedString> header = Arrays.asList(
                    new AttributedStringBuilder()
                            .style(ITALIC_GREEN)
                            .append("Enhanced JLine Prompt Demo!")
                            .toAttributedString(),
                    new AttributedString(
                            "This demonstration showcases all the new prompt types in the JLine Prompt API:"),
                    new AttributedString(
                            "password, number, search, editor, plus traditional list, checkbox, and input prompts."),
                    new AttributedString(
                            "The API is inspired by Inquirer.js and replaces the deprecated ConsoleUI module."));

            // Start the dynamic prompt flow using the proper dynamic prompting API
            Map<String, ? extends PromptResult<? extends Prompt>> results =
                    prompter.prompt(header, PromptDynamicExample::createDynamicPrompts);

            System.out.println("result = " + results);
            if (results.isEmpty()) {
                System.out.println("User cancelled order.");
            } else {
                ConfirmResult delivery = (ConfirmResult) results.get("delivery");
                if (delivery != null && delivery.isConfirmed()) {
                    System.out.println("We will deliver the order in 5 minutes");
                }
            }
        } catch (UserInterruptException e) {
            System.out.println("<ctrl>-c pressed");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Dynamic prompt provider that creates prompts based on previous results.
     * This enhanced version showcases all new prompt types.
     */
    private static List<? extends Prompt> createDynamicPrompts(
            Map<String, ? extends PromptResult<? extends Prompt>> previousResults) {

        // Step 1: Start with user registration (password, number prompts)
        if (previousResults.isEmpty()) {
            return createRegistrationPrompts();
        }

        // Step 2: After registration, show search and editor prompts
        if (previousResults.containsKey("username")
                && previousResults.containsKey("password")
                && !previousResults.containsKey("searchdemo")) {
            return createSearchAndEditorPrompts();
        }

        // Step 3: After search/editor, show traditional prompts (list, checkbox)
        if (previousResults.containsKey("searchdemo")
                && previousResults.containsKey("notes")
                && !previousResults.containsKey("category")) {
            return createTraditionalPrompts();
        }

        // Step 4: Final confirmation and summary
        if (previousResults.containsKey("category")
                && previousResults.containsKey("features")
                && !previousResults.containsKey("summary")) {
            return createSummaryPrompts();
        }

        // No more prompts needed
        return null;
    }

    /**
     * Step 1: Registration prompts showcasing password and number prompts
     */
    private static List<? extends Prompt> createRegistrationPrompts() {
        PromptBuilder builder = new DefaultPromptBuilder();

        // Text header
        builder.createText()
                .name("regheader")
                .text("=== User Registration ===\nLet's start by creating your account.")
                .addPrompt();

        // Input prompt for username
        builder.createInputPrompt()
                .name("username")
                .message("Enter your username")
                .defaultValue("demo_user")
                .addPrompt();

        // Password prompt (new feature)
        builder.createPasswordPrompt()
                .name("password")
                .message("Enter your password")
                .mask('*')
                .showMask(true)
                .addPrompt();

        // Number prompt for age (new feature)
        builder.createNumberPrompt()
                .name("age")
                .message("Enter your age")
                .min(13.0)
                .max(120.0)
                .allowDecimals(false)
                .addPrompt();

        return builder.build();
    }

    /**
     * Step 2: Search and Editor prompts showcasing new interactive features
     */
    private static List<? extends Prompt> createSearchAndEditorPrompts() {
        PromptBuilder builder = new DefaultPromptBuilder();

        // Text header
        builder.createText()
                .name("searchheader")
                .text("=== Search and Editor Demo ===\nNow let's try the new search and editor features.")
                .addPrompt();

        // Search prompt (new feature)
        List<String> technologies = Arrays.asList(
                "Java",
                "JavaScript",
                "Python",
                "TypeScript",
                "Kotlin",
                "Scala",
                "React",
                "Vue.js",
                "Angular",
                "Spring Boot",
                "Node.js",
                "Express",
                "Docker",
                "Kubernetes",
                "AWS",
                "Azure",
                "PostgreSQL",
                "MongoDB");

        Function<String, List<String>> searchFunction = term -> technologies.stream()
                .filter(tech -> tech.toLowerCase().contains(term.toLowerCase()))
                .collect(Collectors.toList());

        builder.<String>createSearchPrompt()
                .name("searchdemo")
                .message("Search for a technology")
                .searchFunction(searchFunction)
                .displayFunction(tech -> tech)
                .valueFunction(tech -> tech)
                .placeholder("Type to search technologies...")
                .minSearchLength(1)
                .maxResults(8)
                .addPrompt();

        // Editor prompt (new feature)
        builder.createEditorPrompt()
                .name("notes")
                .message("Edit your project notes")
                .initialText(
                        "Project Notes:\n\n1. Technology selected: [will be filled from search]\n2. Goals:\n   - Build a modern application\n   - Use best practices\n   - Focus on user experience\n\n3. Next steps:\n   - Set up development environment\n   - Create project structure\n   - Implement core features\n\nFeel free to edit these notes...")
                .fileExtension("md")
                .title("Project Notes Editor")
                .showLineNumbers(true)
                .enableWrapping(true)
                .addPrompt();

        return builder.build();
    }

    /**
     * Step 3: Traditional prompts showcasing list and checkbox with separators
     */
    private static List<? extends Prompt> createTraditionalPrompts() {
        PromptBuilder builder = new DefaultPromptBuilder();

        // Text header
        builder.createText()
                .name("tradheader")
                .text(
                        "=== Traditional Prompts ===\nNow let's see the enhanced list and checkbox prompts with separators.")
                .addPrompt();

        // List prompt with separators
        builder.createListPrompt()
                .name("category")
                .message("Select a project category")
                .newItem("web")
                .text("Web Application")
                .add()
                .newItem("mobile")
                .text("Mobile App")
                .add()
                .newItem("desktop")
                .text("Desktop Application")
                .add()
                .newSeparator("specialized")
                .add()
                .newItem("api")
                .text("REST API Service")
                .add()
                .newItem("microservice")
                .text("Microservice")
                .add()
                .addPrompt();

        // Checkbox prompt with separators and pagination
        builder.createCheckboxPrompt()
                .name("features")
                .message("Select features to include:")
                .newSeparator("core features")
                .add()
                .newItem("auth")
                .text("Authentication")
                .checked(true)
                .add()
                .newItem("db")
                .text("Database Integration")
                .add()
                .newItem("api_client")
                .text("API Client")
                .add()
                .newSeparator("advanced features")
                .add()
                .newItem("cache")
                .text("Caching Layer")
                .add()
                .newItem("search")
                .text("Full-text Search")
                .add()
                .newItem("analytics")
                .text("Analytics Dashboard")
                .disabled(true)
                .disabledText("Premium feature")
                .add()
                .newSeparator("experimental")
                .add()
                .newItem("ai")
                .text("AI Integration")
                .add()
                .newItem("ml")
                .text("Machine Learning")
                .add()
                .pageSize(6)
                .showPageIndicator(true)
                .addPrompt();

        return builder.build();
    }

    /**
     * Step 4: Summary and confirmation prompts
     */
    private static List<? extends Prompt> createSummaryPrompts() {
        PromptBuilder builder = new DefaultPromptBuilder();

        // Text summary
        builder.createText()
                .name("summary")
                .text("=== Demo Complete! ===\nYou've now experienced all the new prompt types:\n"
                        + "✓ Password prompt with masking\n"
                        + "✓ Number prompt with validation\n"
                        + "✓ Search prompt with real-time filtering\n"
                        + "✓ Editor prompt with JLine's nano\n"
                        + "✓ Enhanced list and checkbox prompts with separators\n"
                        + "✓ Pagination support for long lists")
                .addPrompt();

        // Final confirmation
        builder.createConfirmPrompt()
                .name("satisfied")
                .message("Are you satisfied with the new prompt features?")
                .defaultValue(true)
                .addPrompt();

        return builder.build();
    }
}
