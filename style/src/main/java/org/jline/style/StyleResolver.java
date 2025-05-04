/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import org.jline.utils.AttributedStyle;

import static java.util.Objects.requireNonNull;

/**
 * Resolves named (or source-referenced) {@link AttributedStyle} for a specific style group.
 * <p>
 * This class extends {@link org.jline.utils.StyleResolver} to add support for style groups
 * and style sources. It allows resolving style specifications into {@link AttributedStyle}
 * objects, with the ability to look up named styles from a {@link StyleSource}.
 * </p>
 * <p>
 * Style specifications can be in the following formats:
 * </p>
 * <ul>
 *   <li>Direct style specifications: "bold,fg:red,bg:blue,underline"</li>
 *   <li>Named style references: ".error" (looks up "error" in the current style group)</li>
 *   <li>Named style references with defaults: ".error:-bold,fg:red" (uses default if named style is not found)</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * StyleSource source = new MemoryStyleSource();
 * source.set("mygroup", "error", "bold,fg:red");
 *
 * StyleResolver resolver = new StyleResolver(source, "mygroup");
 * AttributedStyle style1 = resolver.resolve("bold,fg:blue");       // Direct style
 * AttributedStyle style2 = resolver.resolve(".error");              // Named style
 * AttributedStyle style3 = resolver.resolve(".missing:-underline"); // Default style
 * </pre>
 *
 * @since 3.4
 * @see org.jline.utils.StyleResolver
 * @see StyleSource
 * @see org.jline.utils.AttributedStyle
 */
public class StyleResolver extends org.jline.utils.StyleResolver {

    private final StyleSource source;

    private final String group;

    /**
     * Constructs a new StyleResolver for the specified source and group.
     * <p>
     * This constructor creates a StyleResolver that will look up named styles
     * in the specified source and group. The resolver will use the source to
     * resolve style names when a style specification starts with a dot (e.g., ".error").
     * </p>
     *
     * @param source the style source to use for named style lookups (must not be null)
     * @param group the style group to use for named style lookups (must not be null)
     * @throws NullPointerException if source or group is null
     */
    public StyleResolver(final StyleSource source, final String group) {
        super(s -> source.get(group, s));
        this.source = requireNonNull(source);
        this.group = requireNonNull(group);
    }

    /**
     * Returns the style source used by this resolver.
     * <p>
     * The style source is used to look up named styles when resolving
     * style specifications that start with a dot (e.g., ".error").
     * </p>
     *
     * @return the style source (never null)
     */
    public StyleSource getSource() {
        return source;
    }

    /**
     * Returns the style group used by this resolver.
     * <p>
     * The style group is used to look up named styles in the style source
     * when resolving style specifications that start with a dot (e.g., ".error").
     * </p>
     *
     * @return the style group name (never null)
     */
    public String getGroup() {
        return group;
    }
}
