/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Interface for number prompts.
 * A number prompt is like an input prompt but validates that the input is a valid number.
 */
public interface NumberPrompt extends InputPrompt {

    /**
     * Get the minimum allowed value.
     *
     * @return the minimum value, or null for no minimum
     */
    default Double getMin() {
        return null;
    }

    /**
     * Get the maximum allowed value.
     *
     * @return the maximum value, or null for no maximum
     */
    default Double getMax() {
        return null;
    }

    /**
     * Whether to allow decimal numbers or only integers.
     *
     * @return true to allow decimals, false for integers only
     */
    default boolean allowDecimals() {
        return true;
    }

    /**
     * Get the default value as a number.
     *
     * @return the default number value, or null for no default
     */
    default Double getDefaultNumber() {
        String defaultValue = getDefaultValue();
        if (defaultValue == null) {
            return null;
        }
        try {
            return Double.parseDouble(defaultValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get the error message to show for invalid numbers.
     *
     * @return the error message
     */
    default String getInvalidNumberMessage() {
        return "Please enter a valid number";
    }

    /**
     * Get the error message to show for out-of-range numbers.
     *
     * @return the error message
     */
    default String getOutOfRangeMessage() {
        Double min = getMin();
        Double max = getMax();
        if (min != null && max != null) {
            return String.format("Please enter a number between %s and %s", min, max);
        } else if (min != null) {
            return String.format("Please enter a number greater than or equal to %s", min);
        } else if (max != null) {
            return String.format("Please enter a number less than or equal to %s", max);
        }
        return "Number is out of range";
    }
}
