/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.prompt.impl.*;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import static org.jline.prompt.DynamicPromptHelper.*;

/**
 * Improved example demonstrating dynamic prompting using the DynamicPromptHelper utility.
 * This shows cleaner, more readable conditional prompting patterns.
 */
public class ImprovedDynamicPromptExample {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ImprovedDynamicPromptExample() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        List<AttributedString> header = Arrays.asList(
                new AttributedString("=== Improved Dynamic Prompt Example ==="),
                new AttributedString("This example shows clean conditional prompting using helper utilities."));

        try {
            // Use the helper to create a clean conditional survey
            Map<String, ? extends PromptResult<? extends Prompt>> results =
                    prompter.prompt(header, createCleanConditionalSurvey());

            // Display final results
            System.out.println("\n=== Survey Results ===");
            for (Map.Entry<String, ? extends PromptResult<? extends Prompt>> entry : results.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue().getDisplayResult());
            }

        } catch (UserInterruptException e) {
            System.out.println("\nSurvey cancelled by user.");
        }
    }

    /**
     * Creates a clean conditional survey using the DynamicPromptHelper utilities.
     */
    private static java.util.function.Function<
                    Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>
            createCleanConditionalSurvey() {

        return chain(
                // Step 1: Ask for user type (only if no results yet)
                when(
                        Map::isEmpty,
                        results -> Arrays.asList(new DefaultListPrompt(
                                "user_type",
                                "What type of user are you?",
                                Arrays.asList(
                                        new DefaultListItem("developer", "Software Developer"),
                                        new DefaultListItem("designer", "UI/UX Designer"),
                                        new DefaultListItem("student", "Student"),
                                        new DefaultListItem("other", "Other"))))),

                // Step 2: Ask for experience level (after user type is selected)
                whenResultExists(
                        "user_type",
                        whenResultMissing(
                                "experience",
                                results -> Arrays.asList(new DefaultListPrompt(
                                        "experience",
                                        "What is your experience level?",
                                        Arrays.asList(
                                                new DefaultListItem("beginner", "Beginner (0-1 years)"),
                                                new DefaultListItem("intermediate", "Intermediate (2-4 years)"),
                                                new DefaultListItem("advanced", "Advanced (5+ years)")))))),

                // Step 3a: Developer-specific questions
                whenResultEquals(
                        "user_type",
                        "developer",
                        whenResultMissing(
                                "programming_languages",
                                results -> Arrays.asList(new DefaultCheckboxPrompt(
                                        "programming_languages",
                                        "Which programming languages do you use?",
                                        Arrays.asList(
                                                new DefaultCheckboxItem("java", "Java", false),
                                                new DefaultCheckboxItem("python", "Python", false),
                                                new DefaultCheckboxItem("javascript", "JavaScript", false),
                                                new DefaultCheckboxItem("typescript", "TypeScript", false),
                                                new DefaultCheckboxItem("go", "Go", false),
                                                new DefaultCheckboxItem("rust", "Rust", false)))))),

                // Step 3b: Designer-specific questions
                whenResultEquals(
                        "user_type",
                        "designer",
                        whenResultMissing(
                                "design_focus",
                                results -> Arrays.asList(new DefaultListPrompt(
                                        "design_focus",
                                        "What is your primary design focus?",
                                        Arrays.asList(
                                                new DefaultListItem("ui", "User Interface (UI)"),
                                                new DefaultListItem("ux", "User Experience (UX)"),
                                                new DefaultListItem("visual", "Visual Design"),
                                                new DefaultListItem("product", "Product Design")))))),

                // Step 3c: Student-specific questions
                whenResultEquals(
                        "user_type",
                        "student",
                        whenResultMissing(
                                "study_field",
                                results -> Arrays.asList(new DefaultInputPrompt(
                                        "study_field", "What field are you studying?", "", null, null, null, null)))),

                // Step 4: Ask about preferred tools (for developers and designers)
                when(
                        results -> resultEqualsAny(results, "user_type", "developer", "designer")
                                && (hasValue(results, "programming_languages") || hasValue(results, "design_focus"))
                                && !hasValue(results, "preferred_tools"),
                        results -> {
                            String userType = getResultValue(results, "user_type");
                            if ("developer".equals(userType)) {
                                return Arrays.asList(new DefaultInputPrompt(
                                        "preferred_tools",
                                        "What is your favorite development tool/IDE?",
                                        "",
                                        null,
                                        null,
                                        null,
                                        null));
                            } else {
                                return Arrays.asList(new DefaultInputPrompt(
                                        "preferred_tools",
                                        "What is your favorite design tool?",
                                        "",
                                        null,
                                        null,
                                        null,
                                        null));
                            }
                        }),

                // Step 5: Ask about learning goals (for everyone)
                when(
                        results -> hasValue(results, "experience")
                                && (anyResultExists(results, "programming_languages", "design_focus", "study_field")
                                        || resultEqualsAny(results, "user_type", "other"))
                                && !hasValue(results, "learning_goals"),
                        results -> Arrays.asList(new DefaultInputPrompt(
                                "learning_goals",
                                "What would you like to learn or improve this year?",
                                "",
                                null,
                                null,
                                null,
                                null))),

                // Step 6: Final satisfaction question (for experienced users)
                when(
                        results -> resultEqualsAny(results, "experience", "intermediate", "advanced")
                                && hasValue(results, "learning_goals")
                                && !results.containsKey("satisfaction"),
                        results -> Arrays.asList(new DefaultListPrompt(
                                "satisfaction",
                                "How satisfied are you with your current skill level?",
                                Arrays.asList(
                                        new DefaultListItem("very_satisfied", "Very Satisfied"),
                                        new DefaultListItem("satisfied", "Satisfied"),
                                        new DefaultListItem("neutral", "Neutral"),
                                        new DefaultListItem("want_to_improve", "Want to Improve"))))),

                // Step 7: Recommendation question (based on satisfaction)
                when(results -> results.containsKey("satisfaction") && !results.containsKey("recommend"), results -> {
                    String satisfaction = getResultValue(results, "satisfaction");
                    boolean defaultValue = resultEqualsAny(results, "satisfaction", "very_satisfied", "satisfied");
                    String message = defaultValue
                            ? "Would you recommend your field to others?"
                            : "Despite wanting to improve, would you still recommend your field to others?";

                    return Arrays.asList(new DefaultConfirmPrompt("recommend", message, defaultValue));
                }),

                // Step 8: Optional feedback (final step)
                when(
                        results -> allResultsExist(results, "user_type", "experience", "learning_goals")
                                && !hasValue(results, "feedback"),
                        results -> Arrays.asList(new DefaultInputPrompt(
                                "feedback",
                                "Any additional feedback or comments? (optional)",
                                "",
                                null,
                                null,
                                null,
                                null))));
    }
}
