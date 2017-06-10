/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link StyleSource} which always returns {@code null}.
 *
 * @since TBD
 */
public class NopStyleSource
  implements StyleSource
{
  // NOTE: preconditions here to help validate usage when this impl is used

  /**
   * Always returns {@code null}.
   */
  @Nullable
  @Override
  public String get(final String group, final String name) {
    checkNotNull(group);
    checkNotNull(name);
    return null;
  }

  /**
   * Non-operation.
   */
  @Override
  public void set(final String group, final String name, final String style) {
    checkNotNull(group);
    checkNotNull(name);
    checkNotNull(style);
  }

  /**
   * Non-operation.
   */
  @Override
  public void remove(final String group) {
    checkNotNull(group);
  }

  /**
   * Non-operation.
   */
  @Override
  public void remove(final String group, final String name) {
    checkNotNull(group);
    checkNotNull(name);
  }

  /**
   * Non-operation.
   */
  @Override
  public void clear() {
    // empty
  }

  /**
   * Always returns empty list.
   */
  @Override
  public Iterable<String> groups() {
    return ImmutableList.copyOf(Collections.emptyList());
  }

  /**
   * Always returns empty map.
   */
  @Override
  public Map<String, String> styles(final String group) {
    return ImmutableMap.copyOf(Collections.emptyMap());
  }
}
