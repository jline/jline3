/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Matchers {
  public interface MatcherType { String name(); }
  public static final class CustomMatcherType implements MatcherType{
    private String name;

    public CustomMatcherType(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }
  }
  public enum StandardMatcherType implements MatcherType {
    PREFIX_STARTS_WITH,
    PREFIX_CONTAINS,
    IN_WORD_STARTS_WITH,
    IN_WORD_CONTAINS,
    IN_WORD_TYPO,
    STARTS_WITH,
    CONTAINS,
    TYPO,
    EMPTY_WORD;
  }
  public Matchers(Predicate<String> exact, LinkedHashMap<MatcherType, Function<Map<String, List<Candidate>>, Map<String, List<Candidate>>>> matchers) {
    this.exact = exact;
    this.matchers = matchers;
  }

  public Predicate<String> getExact() {
    return exact;
  }

  public LinkedHashMap<MatcherType, Function<Map<String, List<Candidate>>, Map<String, List<Candidate>>>> getMatchers() {
    return matchers;
  }

  private Predicate<String> exact;
  private LinkedHashMap<MatcherType, Function<Map<String, List<Candidate>>, Map<String, List<Candidate>>>> matchers;
}
