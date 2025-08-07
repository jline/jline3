/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jline.prompt.PrompterConfig;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.OSUtils;
import org.jline.utils.StyleResolver;

/**
 * Default implementation of PrompterConfig interface.
 */
public class DefaultPrompterConfig implements PrompterConfig {

    private final String indicator;
    private final String uncheckedBox;
    private final String checkedBox;
    private final String unavailable;
    private final StyleResolver styleResolver;
    private final boolean cancellableFirstPrompt;

    public static DefaultPrompterConfig defaults() {
        return OSUtils.IS_WINDOWS ? windows() : unix();
    }

    public static DefaultPrompterConfig windows() {
        return windows(null);
    }

    public static DefaultPrompterConfig unix() {
        return unix(null);
    }

    public static DefaultPrompterConfig custom(StyleResolver styleResolver) {
        return OSUtils.IS_WINDOWS ? windows(styleResolver) : unix(styleResolver);
    }

    public static DefaultPrompterConfig windows(StyleResolver styleResolver) {
        return custom(">", "o", "x", "-", styleResolver, false);
    }

    public static DefaultPrompterConfig unix(StyleResolver styleResolver) {
        return custom("❯", "◯", "◉", "⊝", styleResolver, false);
    }

    /**
     * Create a configuration with default styling support.
     * <p>
     * This method creates a configuration similar to UiConfig, with default color styling
     * based on environment variables or built-in defaults. The style keys used are:
     * </p>
     * <ul>
     *   <li><code>.cu</code> - cursor/indicator style</li>
     *   <li><code>.be</code> - box element style (checked/unchecked boxes)</li>
     *   <li><code>.bd</code> - disabled/unavailable item style</li>
     * </ul>
     *
     * @param indicator the indicator character/string
     * @param uncheckedBox the unchecked box character/string
     * @param checkedBox the checked box character/string
     * @param unavailable the unavailable item character/string
     * @return a configuration with default styling support
     */
    public static DefaultPrompterConfig custom(
            String indicator, String uncheckedBox, String checkedBox, String unavailable) {
        return custom(indicator, uncheckedBox, checkedBox, unavailable, null, false);
    }

    public static DefaultPrompterConfig custom(
            String indicator,
            String uncheckedBox,
            String checkedBox,
            String unavailable,
            StyleResolver resolver,
            boolean cancellableFirstPrompt) {
        if (resolver == null) {
            String defaultColors = "cu=36:be=32:bd=37:pr=32:me=1:an=36:se=36:cb=100:er=31";
            String envColors = System.getenv("PROMPTER_COLORS");
            String colors = envColors != null ? envColors : defaultColors;

            Map<String, String> colorMap = Arrays.stream(colors.split(":"))
                    .collect(Collectors.toMap(
                            s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1)));

            resolver = new StyleResolver(colorMap::get);
        }
        return new DefaultPrompterConfig(
                indicator, uncheckedBox, checkedBox, unavailable, resolver, cancellableFirstPrompt);
    }

    private static AttributedString toAttributedString(StyleResolver resolver, String string, String styleKey) {
        if (resolver == null) {
            return new AttributedString(string);
        }
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(resolver.resolve(styleKey));
        asb.append(string);
        return asb.toAttributedString();
    }

    /**
     * Create a configuration with specific values.
     */
    DefaultPrompterConfig(
            String indicator,
            String uncheckedBox,
            String checkedBox,
            String unavailable,
            StyleResolver styleResolver,
            boolean cancellableFirstPrompt) {
        this.indicator = indicator;
        this.uncheckedBox = uncheckedBox;
        this.checkedBox = checkedBox;
        this.unavailable = unavailable;
        this.styleResolver = styleResolver;
        this.cancellableFirstPrompt = cancellableFirstPrompt;
    }

    @Override
    public boolean cancellableFirstPrompt() {
        return cancellableFirstPrompt;
    }

    @Override
    public StyleResolver styleResolver() {
        return styleResolver;
    }

    @Override
    public String indicator() {
        return indicator;
    }

    @Override
    public String uncheckedBox() {
        return uncheckedBox;
    }

    @Override
    public String checkedBox() {
        return checkedBox;
    }

    @Override
    public String unavailable() {
        return unavailable;
    }
}
