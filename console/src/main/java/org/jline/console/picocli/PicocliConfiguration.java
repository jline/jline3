/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.picocli;

/**
 * Configuration options for Picocli integration.
 * <p>
 * This class provides various configuration options to customize the behavior
 * of the Picocli integration with JLine.
 */
public class PicocliConfiguration {

    private boolean enableContextInjection = true;
    private boolean enableEnhancedCompletion = true;
    private boolean enableRichHelp = true;
    private boolean enableFileCompletion = true;
    private boolean enableEnumCompletion = true;
    private boolean enableColoredOutput = true;
    private boolean enableProgressIndicators = true;
    private boolean enableInteractivePrompts = false;
    private boolean caseSensitiveCompletion = false;
    private boolean showParameterTypes = true;
    private boolean showDefaultValues = true;
    private boolean showRequiredMarkers = true;
    private int maxCompletionCandidates = 100;
    private String promptStyle = "default";

    /**
     * Creates a default configuration.
     */
    public PicocliConfiguration() {
    }

    /**
     * Creates a configuration with all features enabled.
     * @return a fully-featured configuration
     */
    public static PicocliConfiguration fullFeatured() {
        return new PicocliConfiguration()
                .enableContextInjection(true)
                .enableEnhancedCompletion(true)
                .enableRichHelp(true)
                .enableFileCompletion(true)
                .enableEnumCompletion(true)
                .enableColoredOutput(true)
                .enableProgressIndicators(true)
                .enableInteractivePrompts(true);
    }

    /**
     * Creates a minimal configuration with basic features only.
     * @return a minimal configuration
     */
    public static PicocliConfiguration minimal() {
        return new PicocliConfiguration()
                .enableContextInjection(true)
                .enableEnhancedCompletion(false)
                .enableRichHelp(false)
                .enableColoredOutput(false)
                .enableProgressIndicators(false)
                .enableInteractivePrompts(false);
    }

    // Fluent setters
    public PicocliConfiguration enableContextInjection(boolean enable) {
        this.enableContextInjection = enable;
        return this;
    }

    public PicocliConfiguration enableEnhancedCompletion(boolean enable) {
        this.enableEnhancedCompletion = enable;
        return this;
    }

    public PicocliConfiguration enableRichHelp(boolean enable) {
        this.enableRichHelp = enable;
        return this;
    }

    public PicocliConfiguration enableFileCompletion(boolean enable) {
        this.enableFileCompletion = enable;
        return this;
    }

    public PicocliConfiguration enableEnumCompletion(boolean enable) {
        this.enableEnumCompletion = enable;
        return this;
    }

    public PicocliConfiguration enableColoredOutput(boolean enable) {
        this.enableColoredOutput = enable;
        return this;
    }

    public PicocliConfiguration enableProgressIndicators(boolean enable) {
        this.enableProgressIndicators = enable;
        return this;
    }

    public PicocliConfiguration enableInteractivePrompts(boolean enable) {
        this.enableInteractivePrompts = enable;
        return this;
    }

    public PicocliConfiguration caseSensitiveCompletion(boolean caseSensitive) {
        this.caseSensitiveCompletion = caseSensitive;
        return this;
    }

    public PicocliConfiguration showParameterTypes(boolean show) {
        this.showParameterTypes = show;
        return this;
    }

    public PicocliConfiguration showDefaultValues(boolean show) {
        this.showDefaultValues = show;
        return this;
    }

    public PicocliConfiguration showRequiredMarkers(boolean show) {
        this.showRequiredMarkers = show;
        return this;
    }

    public PicocliConfiguration maxCompletionCandidates(int max) {
        this.maxCompletionCandidates = max;
        return this;
    }

    public PicocliConfiguration promptStyle(String style) {
        this.promptStyle = style;
        return this;
    }

    // Getters
    public boolean isContextInjectionEnabled() {
        return enableContextInjection;
    }

    public boolean isEnhancedCompletionEnabled() {
        return enableEnhancedCompletion;
    }

    public boolean isRichHelpEnabled() {
        return enableRichHelp;
    }

    public boolean isFileCompletionEnabled() {
        return enableFileCompletion;
    }

    public boolean isEnumCompletionEnabled() {
        return enableEnumCompletion;
    }

    public boolean isColoredOutputEnabled() {
        return enableColoredOutput;
    }

    public boolean isProgressIndicatorsEnabled() {
        return enableProgressIndicators;
    }

    public boolean isInteractivePromptsEnabled() {
        return enableInteractivePrompts;
    }

    public boolean isCaseSensitiveCompletion() {
        return caseSensitiveCompletion;
    }

    public boolean isShowParameterTypes() {
        return showParameterTypes;
    }

    public boolean isShowDefaultValues() {
        return showDefaultValues;
    }

    public boolean isShowRequiredMarkers() {
        return showRequiredMarkers;
    }

    public int getMaxCompletionCandidates() {
        return maxCompletionCandidates;
    }

    public String getPromptStyle() {
        return promptStyle;
    }
}
