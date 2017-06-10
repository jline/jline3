/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.jline.utils.AttributedStyle;

import static java.util.Objects.requireNonNull;
import static org.jline.utils.AttributedStyle.*;

// TODO: document style specification

/**
 * Resolves named (or source-referenced) {@link AttributedStyle}.
 *
 * @since TBD
 */
public class StyleResolver
{
  private static final Logger log = Logger.getLogger(StyleResolver.class.getName());

  private final StyleSource source;

  private final String group;

  public StyleResolver(final StyleSource source, final String group) {
    this.source = requireNonNull(source);
    this.group = requireNonNull(group);
  }

  public StyleSource getSource() {
    return source;
  }

  public String getGroup() {
    return group;
  }

  // TODO: could consider a small cache to reduce style calculations?

  /**
   * Resolve the given style specification.
   *
   * If for some reason the specification is invalid, then {@link AttributedStyle#DEFAULT} will be used.
   */
  public AttributedStyle resolve(final String spec) {
    requireNonNull(spec);

    if (log.isLoggable(Level.FINEST)) {
      log.finest("Resolve: " + spec);
    }

    int i = spec.indexOf(":-");
    if (i != -1) {
      String[] parts = spec.split(":-");
      return resolve(parts[0].trim(), parts[1].trim());
    }

    return apply(DEFAULT, spec);
  }

  /**
   * Resolve the given style specification.
   *
   * If this resolves to {@link AttributedStyle#DEFAULT} then given default specification is used if non-null.
   */
  public AttributedStyle resolve(final String spec, @Nullable final String defaultSpec) {
    requireNonNull(spec);

    if (log.isLoggable(Level.FINEST)) {
      log.finest(String.format("Resolve: %s; default: %s", spec, defaultSpec));
    }

    AttributedStyle style = apply(DEFAULT, spec);
    if (style == DEFAULT && defaultSpec != null) {
      style = apply(style, defaultSpec);
    }
    return style;
  }

  /**
   * Apply style specification.
   */
  private AttributedStyle apply(AttributedStyle style, final String spec) {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("Apply: " + spec);
    }

    for (String item : spec.split(",")) {
      item = item.trim();
      if (item.isEmpty()) {
        continue;
      }

      if (item.startsWith(".")) {
        style = applyReference(style, item);
      }
      else if (item.contains(":")) {
        style = applyColor(style, item);
      }
      else {
        style = applyNamed(style, item);
      }
    }

    return style;
  }

  /**
   * Apply source-referenced named style.
   */
  private AttributedStyle applyReference(final AttributedStyle style, final String spec) {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("Apply-reference: " + spec);
    }

    if (spec.length() == 1) {
      log.warning("Invalid style-reference; missing discriminator: " + spec);
    }
    else {
      String name = spec.substring(1, spec.length());
      String resolvedSpec = source.get(group, name);
      if (resolvedSpec != null) {
        return apply(style, resolvedSpec);
      }
      // null is normal if source has not be configured with named style
    }

    return style;
  }

  /**
   * Apply default named styles.
   */
  private AttributedStyle applyNamed(final AttributedStyle style, final String name) {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("Apply-named: " + name);
    }

    // TODO: consider short aliases for named styles

    switch (name.toLowerCase(Locale.US)) {
      case "default":
        return DEFAULT;

      case "bold":
        return style.bold();

      case "faint":
        return style.faint();

      case "italic":
        return style.italic();

      case "underline":
        return style.underline();

      case "blink":
        return style.blink();

      case "inverse":
        return style.inverse();

      case "inverse-neg":
      case "inverseneg":
        return style.inverseNeg();

      case "conceal":
        return style.conceal();

      case "crossed-out":
      case "crossedout":
        return style.crossedOut();

      case "hidden":
        return style.hidden();

      default:
        log.warning("Unknown style: " + name);
        return style;
    }
  }

  /**
   * Apply color styles specification.
   *
   * @param spec Color specification: {@code <color-mode>:<color-name>}
   */
  private AttributedStyle applyColor(final AttributedStyle style, final String spec) {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("Apply-color: " + spec);
    }

    // extract color-mode:color-name
    String[] parts = spec.split(":", 2);
    String colorMode = parts[0].trim();
    String colorName = parts[1].trim();

    // resolve the color-name
    Integer color = color(colorName);
    if (color == null) {
      log.warning("Invalid color-name: " + colorName);
    }
    else {
      // resolve and apply color-mode
      switch (colorMode.toLowerCase(Locale.US)) {
        case "foreground":
        case "fg":
        case "f":
          return style.foreground(color);

        case "background":
        case "bg":
        case "b":
          return style.background(color);

        default:
          log.warning("Invalid color-mode: " +  colorMode);
      }
    }
    return style;
  }

  // TODO: consider simplify and always using StyleColor, for now for compat with other bits leaving syntax complexity

  /**
   * Returns the color identifier for the given name.
   *
   * Bright color can be specified with: {@code !<color>} or {@code bright-<color>}.
   *
   * Full xterm256 color can be specified with: {@code ~<color>}.
   *
   * @return color code, or {@code null} if unable to determine.
   */
  @Nullable
  private static Integer color(String name) {
    int flags = 0;
    name = name.toLowerCase(Locale.US);

    // extract bright flag from color name
    if (name.charAt(0) == '!') {
      name = name.substring(1, name.length());
      flags = BRIGHT;
    }
    else if (name.startsWith("bright-")) {
      name = name.substring(7, name.length());
      flags = BRIGHT;
    }
    else if (name.charAt(0) == '~') {
      try {
        name = name.substring(1, name.length());
        StyleColor color = StyleColor.valueOf(name);
        return color.code;
      }
      catch (IllegalArgumentException e) {
        log.warning("Invalid style-color name: " + name);
        return null;
      }
    }

    switch (name) {
      case "black":
      case "k":
        return flags + BLACK;

      case "red":
      case "r":
        return flags + RED;

      case "green":
      case "g":
        return flags + GREEN;

      case "yellow":
      case "y":
        return flags + YELLOW;

      case "blue":
      case "b":
        return flags + BLUE;

      case "magenta":
      case "m":
        return flags + MAGENTA;

      case "cyan":
      case "c":
        return flags + CYAN;

      case "white":
      case "w":
        return flags + WHITE;
    }

    return null;
  }
}
