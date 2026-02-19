/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.*;
import java.util.regex.Pattern;

import org.jline.utils.AttributedString;

/**
 * Describes a command for help display and widget integration.
 * <p>
 * Uses a builder pattern and record-style accessors. Instances are
 * immutable once built.
 * <p>
 * Example:
 * <pre>
 * CommandDescription desc = CommandDescription.builder()
 *     .mainDescription(List.of(new AttributedString("list files")))
 *     .argument(new ArgumentDescription("path"))
 *     .option("-l --long", List.of(new AttributedString("long format")))
 *     .build();
 * </pre>
 *
 * @see ArgumentDescription
 * @see Command#describe(List)
 * @since 4.0
 */
public class CommandDescription {

    private final List<AttributedString> mainDescription;
    private final List<ArgumentDescription> arguments;
    private final Map<String, List<AttributedString>> options;
    private final boolean valid;
    private final boolean command;
    private final boolean subcommand;
    private final boolean highlighted;
    private final Pattern errorPattern;
    private final int errorIndex;

    private CommandDescription(Builder builder) {
        this.mainDescription = Collections.unmodifiableList(new ArrayList<>(builder.mainDescription));
        this.arguments = Collections.unmodifiableList(new ArrayList<>(builder.arguments));
        this.options = Collections.unmodifiableMap(new TreeMap<>(builder.options));
        this.valid = builder.valid;
        this.command = builder.command;
        this.subcommand = builder.subcommand;
        this.highlighted = builder.highlighted;
        this.errorPattern = builder.errorPattern;
        this.errorIndex = builder.errorIndex;
    }

    /**
     * Returns the main description lines.
     *
     * @return the main description, never null
     */
    public List<AttributedString> mainDescription() {
        return mainDescription;
    }

    /**
     * Returns the argument descriptions.
     *
     * @return the arguments, never null
     */
    public List<ArgumentDescription> arguments() {
        return arguments;
    }

    /**
     * Returns the option descriptions, keyed by option syntax.
     *
     * @return the options map, never null
     */
    public Map<String, List<AttributedString>> options() {
        return options;
    }

    /**
     * Returns whether this description represents a valid command.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns whether this description represents a command (as opposed to a method or syntax).
     *
     * @return true if this is a command description
     */
    public boolean isCommand() {
        return command;
    }

    /**
     * Returns whether this description represents a subcommand.
     *
     * @return true if this is a subcommand
     */
    public boolean isSubcommand() {
        return subcommand;
    }

    /**
     * Returns whether the specified option takes a value.
     * <p>
     * An option is considered to take a value if its key in the options map
     * contains an {@code =} character (e.g., {@code "-o --output=FILE"}).
     *
     * @param option the option to check (e.g., {@code "-o"})
     * @return true if the option takes a value
     */
    public boolean optionWithValue(String option) {
        for (String key : options.keySet()) {
            if (key.matches("(^|.*\\s)" + Pattern.quote(option) + "($|=.*|\\s.*)")) {
                return key.contains("=");
            }
        }
        return false;
    }

    /**
     * Returns the first line of description for the specified option key.
     *
     * @param key the option key as it appears in the options map
     * @return the first description line, or an empty string if none
     */
    public AttributedString optionDescription(String key) {
        List<AttributedString> desc = options.get(key);
        return desc != null && !desc.isEmpty() ? desc.get(0) : new AttributedString("");
    }

    /**
     * Returns whether the command should be highlighted.
     *
     * @return true if highlighted
     */
    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * Returns the pattern for identifying errors, or null.
     *
     * @return the error pattern
     */
    public Pattern errorPattern() {
        return errorPattern;
    }

    /**
     * Returns the error index, or -1 if none.
     *
     * @return the error index
     */
    public int errorIndex() {
        return errorIndex;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CommandDescription} instances.
     *
     * @since 4.0
     */
    public static class Builder {
        private final List<AttributedString> mainDescription = new ArrayList<>();
        private final List<ArgumentDescription> arguments = new ArrayList<>();
        private final Map<String, List<AttributedString>> options = new LinkedHashMap<>();
        private boolean valid = true;
        private boolean command = false;
        private boolean subcommand = false;
        private boolean highlighted = true;
        private Pattern errorPattern;
        private int errorIndex = -1;

        Builder() {}

        /**
         * Sets the main description.
         *
         * @param mainDescription the description lines
         * @return this builder
         */
        public Builder mainDescription(List<AttributedString> mainDescription) {
            this.mainDescription.clear();
            this.mainDescription.addAll(mainDescription);
            return this;
        }

        /**
         * Adds an argument description.
         *
         * @param argument the argument description
         * @return this builder
         */
        public Builder argument(ArgumentDescription argument) {
            this.arguments.add(argument);
            return this;
        }

        /**
         * Sets all argument descriptions.
         *
         * @param arguments the argument descriptions
         * @return this builder
         */
        public Builder arguments(List<ArgumentDescription> arguments) {
            this.arguments.clear();
            this.arguments.addAll(arguments);
            return this;
        }

        /**
         * Adds an option with its description.
         *
         * @param key the option syntax (e.g., "-l --long")
         * @param description the option description
         * @return this builder
         */
        public Builder option(String key, List<AttributedString> description) {
            this.options.put(key, new ArrayList<>(description));
            return this;
        }

        /**
         * Sets all options.
         *
         * @param options the options map
         * @return this builder
         */
        public Builder options(Map<String, List<AttributedString>> options) {
            this.options.clear();
            this.options.putAll(options);
            return this;
        }

        /**
         * Sets whether this description is valid.
         *
         * @param valid true if valid
         * @return this builder
         */
        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        /**
         * Sets whether this description represents a command.
         *
         * @param command true if this is a command description
         * @return this builder
         */
        public Builder command(boolean command) {
            this.command = command;
            return this;
        }

        /**
         * Sets whether this description represents a subcommand.
         *
         * @param subcommand true if this is a subcommand
         * @return this builder
         */
        public Builder subcommand(boolean subcommand) {
            this.subcommand = subcommand;
            return this;
        }

        /**
         * Sets whether the command should be highlighted.
         *
         * @param highlighted true if highlighted
         * @return this builder
         */
        public Builder highlighted(boolean highlighted) {
            this.highlighted = highlighted;
            return this;
        }

        /**
         * Sets the error pattern.
         *
         * @param errorPattern the error pattern
         * @return this builder
         */
        public Builder errorPattern(Pattern errorPattern) {
            this.errorPattern = errorPattern;
            return this;
        }

        /**
         * Sets the error index.
         *
         * @param errorIndex the error index
         * @return this builder
         */
        public Builder errorIndex(int errorIndex) {
            this.errorIndex = errorIndex;
            return this;
        }

        /**
         * Builds the command description.
         *
         * @return the immutable command description
         */
        public CommandDescription build() {
            return new CommandDescription(this);
        }
    }
}
