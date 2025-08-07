/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility class to help with dynamic prompting scenarios.
 * Provides common patterns and helper methods for conditional prompting.
 */
public class DynamicPromptHelper {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DynamicPromptHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a conditional prompt provider that only shows prompts when a condition is met.
     *
     * @param condition the condition to check against previous results
     * @param promptProvider the provider to call when condition is true
     * @return a conditional prompt provider
     */
    public static Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> when(
            Predicate<Map<String, ? extends PromptResult<? extends Prompt>>> condition,
            Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> promptProvider) {

        return results -> {
            if (condition.test(results)) {
                return promptProvider.apply(results);
            }
            return null;
        };
    }

    /**
     * Creates a prompt provider that only executes if a specific result exists.
     *
     * @param resultName the name of the result that must exist
     * @param promptProvider the provider to call when the result exists
     * @return a conditional prompt provider
     */
    public static Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>
            whenResultExists(
                    String resultName,
                    Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>
                            promptProvider) {

        return when(results -> results.containsKey(resultName), promptProvider);
    }

    /**
     * Creates a prompt provider that only executes if a specific result has a specific value.
     *
     * @param resultName the name of the result to check
     * @param expectedValue the expected value
     * @param promptProvider the provider to call when the condition is met
     * @return a conditional prompt provider
     */
    public static Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>
            whenResultEquals(
                    String resultName,
                    String expectedValue,
                    Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>
                            promptProvider) {

        return when(
                results -> {
                    PromptResult<? extends Prompt> result = results.get(resultName);
                    return result != null && expectedValue.equals(result.getResult());
                },
                promptProvider);
    }

    /**
     * Creates a prompt provider that only executes if a specific result does NOT exist.
     *
     * @param resultName the name of the result that must not exist
     * @param promptProvider the provider to call when the result doesn't exist
     * @return a conditional prompt provider
     */
    public static Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>
            whenResultMissing(
                    String resultName,
                    Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>
                            promptProvider) {

        return when(results -> !results.containsKey(resultName), promptProvider);
    }

    /**
     * Creates a prompt provider that chains multiple conditional providers.
     * Each provider is tried in order until one returns a non-null result.
     *
     * @param providers the providers to chain
     * @return a chained prompt provider
     */
    @SafeVarargs
    public static Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> chain(
            Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>>... providers) {

        return results -> {
            for (Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> provider :
                    providers) {
                List<? extends Prompt> prompts = provider.apply(results);
                if (prompts != null && !prompts.isEmpty()) {
                    return prompts;
                }
            }
            return null;
        };
    }

    /**
     * Gets the result value for a specific prompt, or null if not found.
     *
     * @param results the results map
     * @param resultName the name of the result
     * @return the result value or null
     */
    public static String getResultValue(
            Map<String, ? extends PromptResult<? extends Prompt>> results, String resultName) {
        PromptResult<? extends Prompt> result = results.get(resultName);
        return result != null ? result.getResult() : null;
    }

    /**
     * Checks if a result exists and has a non-empty value.
     *
     * @param results the results map
     * @param resultName the name of the result
     * @return true if the result exists and has a non-empty value
     */
    public static boolean hasValue(Map<String, ? extends PromptResult<? extends Prompt>> results, String resultName) {
        String value = getResultValue(results, resultName);
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Checks if a result equals any of the given values.
     *
     * @param results the results map
     * @param resultName the name of the result
     * @param values the values to check against
     * @return true if the result equals any of the values
     */
    public static boolean resultEqualsAny(
            Map<String, ? extends PromptResult<? extends Prompt>> results, String resultName, String... values) {
        String resultValue = getResultValue(results, resultName);
        if (resultValue == null) {
            return false;
        }

        for (String value : values) {
            if (resultValue.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if all specified results exist.
     *
     * @param results the results map
     * @param resultNames the names of the results to check
     * @return true if all results exist
     */
    public static boolean allResultsExist(
            Map<String, ? extends PromptResult<? extends Prompt>> results, String... resultNames) {
        for (String resultName : resultNames) {
            if (!results.containsKey(resultName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if any of the specified results exist.
     *
     * @param results the results map
     * @param resultNames the names of the results to check
     * @return true if any result exists
     */
    public static boolean anyResultExists(
            Map<String, ? extends PromptResult<? extends Prompt>> results, String... resultNames) {
        for (String resultName : resultNames) {
            if (results.containsKey(resultName)) {
                return true;
            }
        }
        return false;
    }
}
