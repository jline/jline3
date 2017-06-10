/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.planet57.gossip.Log;
import org.jline.style.StyleBundle.DefaultStyle;
import org.jline.style.StyleBundle.StyleGroup;
import org.jline.style.StyleBundle.StyleName;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link StyleBundle} proxy invocation-handler to convert method calls into string styling.
 *
 * @since TBD
 * @see StyleBundle
 */
class StyleBundleInvocationHandler
    implements InvocationHandler
{
  private static final Logger log = Log.getLogger(StyleBundleInvocationHandler.class);

  private final Class<? extends StyleBundle> type;

  private final StyleResolver resolver;

  public StyleBundleInvocationHandler(final Class<? extends StyleBundle> type, final StyleResolver resolver) {
    this.type = checkNotNull(type);
    this.resolver = checkNotNull(resolver);
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    // Allow invocations to Object methods to pass-through
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(this, args);
    }

    // or validate StyleBundle method is valid
    validate(method);

    // resolve the style-name for method
    String styleName = getStyleName(method);

    // resolve the sourced-style, or use the default
    String style = resolver.getSource().get(resolver.getGroup(), styleName);
    log.trace("Sourced-style: {} -> {}", styleName, style);

    if (style == null) {
      style = getDefaultStyle(method);

      // if sourced-style was missing and default-style is missing barf
      if (style == null) {
        throw new StyleBundleMethodMissingDefaultStyleException(method);
      }
    }

    String value  = String.valueOf(args[0]);
    log.trace("Applying style: {} -> {} to: {}", styleName, style, value);

    AttributedStyle astyle = resolver.resolve(style);
    return new AttributedString(value, astyle);
  }

  /**
   * Throws {@link InvalidStyleBundleMethodException} if given method is not suitable.
   */
  private static void validate(final Method method) {
    // All StyleBundle methods must take exactly 1 parameter
    if (method.getParameterCount() != 1) {
      throw new InvalidStyleBundleMethodException(method, "Invalid parameters");
    }

    // All StyleBundle methods must return an AttributeString
    if (method.getReturnType() != AttributedString.class) {
      throw new InvalidStyleBundleMethodException(method, "Invalid return-type");
    }
  }

  /**
   * Slightly better logging for proxies.
   */
  @Override
  public String toString() {
    return type.getName();
  }

  //
  // Helpers
  //

  /**
   * Returns the style group-name for given type, or {@code null} if unable to determine.
   */
  @Nullable
  private static String getStyleGroup(final Class<?> type) {
    StyleGroup styleGroup = type.getAnnotation(StyleGroup.class);
    return styleGroup != null ? Strings.emptyToNull(styleGroup.value().trim()) : null;
  }

  /**
   * Returns the style-name for given method, or {@code null} if unable to determine.
   */
  private static String getStyleName(final Method method) {
    StyleName styleName = method.getAnnotation(StyleName.class);
    return styleName != null ? Strings.emptyToNull(styleName.value().trim()) : method.getName();
  }

  /**
   * Returns the default-style for given method, or {@code null} if unable to determine.
   */
  @Nullable
  private static String getDefaultStyle(final Method method) {
    DefaultStyle defaultStyle = method.getAnnotation(DefaultStyle.class);
    // allow whitespace in default-style.value, but disallow empty-string
    return defaultStyle != null ? Strings.emptyToNull(defaultStyle.value()) : null;
  }

  //
  // Factory access
  //

  /**
   * Internal factory-method.
   *
   * @see Styler#bundle(Class)
   */
  @SuppressWarnings("unchecked")
  static <T extends StyleBundle> T create(final StyleResolver resolver, final Class<T> type) {
    checkNotNull(resolver);
    checkNotNull(type);

    log.trace("Using style-group: {} for type: {}", resolver.getGroup(), type.getName());

    StyleBundleInvocationHandler handler = new StyleBundleInvocationHandler(type, resolver);
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler);
  }

  /**
   * Internal factory-method.
   *
   * @see Styler#bundle(String,Class)
   */
  static <T extends StyleBundle> T create(final StyleSource source, final Class<T> type) {
    checkNotNull(type);

    String group = getStyleGroup(type);
    if (group == null) {
      throw new InvalidStyleGroupException(type);
    }

    return create(new StyleResolver(source, group), type);
  }

  //
  // Exceptions
  //

  /**
   * Thrown when {@link StyleBundle} method has missing {@link DefaultStyle}.
   */
  @VisibleForTesting
  static class StyleBundleMethodMissingDefaultStyleException
      extends RuntimeException
  {
    public StyleBundleMethodMissingDefaultStyleException(final Method method) {
      super(String.format("%s method missing @%s: %s",
          StyleBundle.class.getSimpleName(),
          DefaultStyle.class.getSimpleName(),
          method
      ));
    }
  }

  /**
   * Thrown when processing {@link StyleBundle} method is found to be invalid.
   */
  @VisibleForTesting
  static class InvalidStyleBundleMethodException
      extends RuntimeException
  {
    public InvalidStyleBundleMethodException(final Method method, final String message) {
      super(message + ": " + method);
    }
  }

  /**
   * Thrown when looking up {@link StyleGroup} on a type found to be missing or invalid.
   */
  @VisibleForTesting
  static class InvalidStyleGroupException
      extends RuntimeException
  {
    public InvalidStyleGroupException(final Class type) {
      super(String.format("%s missing or invalid @%s: %s",
          StyleBundle.class.getSimpleName(),
          StyleGroup.class.getSimpleName(),
          type.getName()
      ));
    }
  }
}
