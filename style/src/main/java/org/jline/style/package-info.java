/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Style package provides a comprehensive styling system for terminal output.
 *
 * <p>
 * This package contains classes and interfaces for defining, managing, and applying
 * styles to text displayed in the terminal. It supports:
 * </p>
 *
 * <ul>
 *   <li>Style specifications using a simple syntax (e.g., "bold,fg:red")</li>
 *   <li>Named styles that can be referenced and reused</li>
 *   <li>Style expressions with the format {@code @{style text}}</li>
 *   <li>Style bundles using Java interfaces and proxies</li>
 *   <li>Style sources for storing and retrieving style definitions</li>
 * </ul>
 *
 * <p>
 * The styling system integrates with JLine's {@link org.jline.utils.AttributedString} and
 * {@link org.jline.utils.AttributedStyle} classes to provide rich text formatting capabilities.
 * </p>
 *
 * <p>
 * Key components of this package include:
 * </p>
 *
 * <ul>
 *   <li>{@link org.jline.style.StyleBundle} - Interface for proxy-based style bundles</li>
 *   <li>{@link org.jline.style.StyleExpression} - Evaluates style expressions</li>
 *   <li>{@link org.jline.style.StyleFactory} - Creates styled strings</li>
 *   <li>{@link org.jline.style.StyleResolver} - Resolves named styles</li>
 *   <li>{@link org.jline.style.StyleSource} - Interface for style storage</li>
 *   <li>{@link org.jline.style.Styler} - Facade for style operations</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * // Using StyleFactory
 * StyleFactory factory = Styler.factory("mygroup");
 * AttributedString text = factory.style("bold,fg:red", "Important message");
 *
 * // Using StyleExpression
 * StyleExpression expr = new StyleExpression(Styler.resolver("mygroup"));
 * AttributedString text = expr.evaluate("Normal text with @{bold,fg:red important} parts");
 *
 * // Using StyleBundle
 * &#64;StyleBundle.StyleGroup("mygroup")
 * interface MyStyles extends StyleBundle {
 *     &#64;DefaultStyle("bold,fg:red")
 *     AttributedString important(String text);
 * }
 * MyStyles styles = Styler.bundle(MyStyles.class);
 * AttributedString text = styles.important("Important message");
 * </pre>
 *
 * @since 3.4
 */
package org.jline.style;
