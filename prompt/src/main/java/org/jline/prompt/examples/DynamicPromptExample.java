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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.prompt.impl.*;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

/**
 * Example demonstrating dynamic prompting where subsequent prompts depend on previous answers.
 */
public class DynamicPromptExample {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DynamicPromptExample() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        List<AttributedString> header = Arrays.asList(
                new AttributedString("=== Dynamic Prompt Example ==="),
                new AttributedString("This example shows how prompts can be conditional based on previous answers."));

        try {
            // Use dynamic prompting to create a conditional survey
            Map<String, ? extends PromptResult<? extends Prompt>> results =
                    prompter.prompt(header, DynamicPromptExample::createConditionalSurvey);

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
     * Creates a conditional survey where questions depend on previous answers.
     */
    private static List<? extends Prompt> createConditionalSurvey(
            Map<String, ? extends PromptResult<? extends Prompt>> previousResults) {

        List<Prompt> prompts = new ArrayList<>();

        if (previousResults.isEmpty()) {
            // First question: What's your role?
            List<ListItem> roleItems = Arrays.asList(
                    new DefaultListItem("developer", "Software Developer"),
                    new DefaultListItem("designer", "UI/UX Designer"),
                    new DefaultListItem("manager", "Project Manager"),
                    new DefaultListItem("other", "Other"));
            prompts.add(new DefaultListPrompt("role", "What is your role?", roleItems));

        } else if (previousResults.containsKey("role") && !previousResults.containsKey("experience")) {
            // Second question: Experience level (always asked)
            List<ListItem> experienceItems = Arrays.asList(
                    new DefaultListItem("junior", "Junior (0-2 years)"),
                    new DefaultListItem("mid", "Mid-level (3-5 years)"),
                    new DefaultListItem("senior", "Senior (6+ years)"));
            prompts.add(new DefaultListPrompt("experience", "What is your experience level?", experienceItems));

        } else if (previousResults.containsKey("experience") && !previousResults.containsKey("tech_stack")) {
            // Third question: Technology stack (only for developers)
            String role = previousResults.get("role").getResult();
            if ("developer".equals(role)) {
                List<CheckboxItem> techItems = Arrays.asList(
                        new DefaultCheckboxItem("java", "Java", false),
                        new DefaultCheckboxItem("python", "Python", false),
                        new DefaultCheckboxItem("javascript", "JavaScript", false),
                        new DefaultCheckboxItem("csharp", "C#", false),
                        new DefaultCheckboxItem("go", "Go", false));
                prompts.add(new DefaultCheckboxPrompt(
                        "tech_stack", "Which technologies do you use? (Select all that apply)", techItems));
            }

        } else if (previousResults.containsKey("experience") && !previousResults.containsKey("design_tools")) {
            // Alternative third question: Design tools (only for designers)
            String role = previousResults.get("role").getResult();
            if ("designer".equals(role)) {
                List<CheckboxItem> designItems = Arrays.asList(
                        new DefaultCheckboxItem("figma", "Figma", false),
                        new DefaultCheckboxItem("sketch", "Sketch", false),
                        new DefaultCheckboxItem("adobe_xd", "Adobe XD", false),
                        new DefaultCheckboxItem("photoshop", "Photoshop", false),
                        new DefaultCheckboxItem("illustrator", "Illustrator", false));
                prompts.add(new DefaultCheckboxPrompt(
                        "design_tools", "Which design tools do you use? (Select all that apply)", designItems));
            }

        } else if (previousResults.containsKey("experience") && !previousResults.containsKey("team_size")) {
            // Alternative third question: Team size (only for managers)
            String role = previousResults.get("role").getResult();
            if ("manager".equals(role)) {
                List<ListItem> teamSizeItems = Arrays.asList(
                        new DefaultListItem("small", "Small (1-5 people)"),
                        new DefaultListItem("medium", "Medium (6-15 people)"),
                        new DefaultListItem("large", "Large (16+ people)"));
                prompts.add(new DefaultListPrompt("team_size", "What is your team size?", teamSizeItems));
            }

        } else if (hasRoleSpecificAnswer(previousResults) && !previousResults.containsKey("satisfaction")) {
            // Fourth question: Job satisfaction (asked to everyone after role-specific questions)
            List<ListItem> satisfactionItems = Arrays.asList(
                    new DefaultListItem("very_satisfied", "Very Satisfied"),
                    new DefaultListItem("satisfied", "Satisfied"),
                    new DefaultListItem("neutral", "Neutral"),
                    new DefaultListItem("dissatisfied", "Dissatisfied"),
                    new DefaultListItem("very_dissatisfied", "Very Dissatisfied"));
            prompts.add(new DefaultListPrompt(
                    "satisfaction", "How satisfied are you with your current job?", satisfactionItems));

        } else if (previousResults.containsKey("satisfaction") && !previousResults.containsKey("recommend")) {
            // Fifth question: Would you recommend your field? (conditional on satisfaction)
            String satisfaction = previousResults.get("satisfaction").getResult();
            if ("very_satisfied".equals(satisfaction) || "satisfied".equals(satisfaction)) {
                prompts.add(new DefaultConfirmPrompt("recommend", "Would you recommend your field to others?", true));
            } else {
                prompts.add(new DefaultConfirmPrompt(
                        "recommend",
                        "Despite your satisfaction level, would you still recommend your field to others?",
                        false));
            }

        } else if (previousResults.containsKey("recommend") && !previousResults.containsKey("comments")) {
            // Final question: Optional comments
            prompts.add(new DefaultInputPrompt(
                    "comments", "Any additional comments? (optional)", "", null, null, null, null));

        } else {
            // No more questions
            return null;
        }

        return prompts;
    }

    /**
     * Check if the user has answered their role-specific question.
     */
    private static boolean hasRoleSpecificAnswer(Map<String, ? extends PromptResult<? extends Prompt>> results) {
        if (!results.containsKey("role")) {
            return false;
        }

        String role = results.get("role").getResult();
        switch (role) {
            case "developer":
                return results.containsKey("tech_stack");
            case "designer":
                return results.containsKey("design_tools");
            case "manager":
                return results.containsKey("team_size");
            case "other":
                return true; // No role-specific question for "other"
            default:
                return false;
        }
    }
}
