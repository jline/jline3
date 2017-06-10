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
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.planet57.gossip.Log;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory {@link StyleSource}.
 *
 * @since TBD
 */
public class MemoryStyleSource
  implements StyleSource
{
  private static final Logger log = Log.getLogger(MemoryStyleSource.class);

  private final Map<String,Map<String,String>> backing = new ConcurrentHashMap<>();

  @Nullable
  @Override
  public String get(final String group, final String name) {
    String style = null;
    Map<String,String> styles = backing.get(group);
    if (styles != null) {
      style = styles.get(name);
    }
    log.trace("Get: [{}] {} -> {}", group, name, style);
    return style;
  }

  @Override
  public void set(final String group, final String name, final String style) {
    checkNotNull(group);
    checkNotNull(name);
    checkNotNull(style);
    backing.computeIfAbsent(group, k -> new ConcurrentHashMap<>()).put(name, style);
    log.trace("Set: [{}] {} -> {}", group, name, style);
  }

  @Override
  public void remove(final String group) {
    checkNotNull(group);
    if (backing.remove(group) != null) {
      log.trace("Removed: [{}]");
    }
  }

  @Override
  public void remove(final String group, final String name) {
    checkNotNull(group);
    checkNotNull(name);
    Map<String,String> styles = backing.get(group);
    if (styles != null) {
      styles.remove(name);
      log.trace("Removed: [{}] {}", group, name);
    }
  }

  @Override
  public void clear() {
    backing.clear();
    log.trace("Cleared");
  }

  @Override
  public Iterable<String> groups() {
    return ImmutableSet.copyOf(backing.keySet());
  }

  @Override
  public Map<String,String> styles(final String group) {
    checkNotNull(group);
    Map<String,String> result = backing.get(group);
    if (result == null) {
      result = Collections.emptyMap();
    }
    return ImmutableMap.copyOf(result);
  }
}
