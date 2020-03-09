/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import org.jline.reader.impl.ReaderUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MatcherFactory {

  /**
   * @param prefix True if executing the complete-prefix command
   * @param line The parsed line within which completion has been requested
   * @param options The active options of the line reader
   * @param originalGroupName
   * @param matchers A map of matchers.
   *
   * Extension point to allow custom matches for filtering completion candidates. Add an entry to the provided map
   * and/or remove entries that are not wanted.
   */
  public void updateMatchers(boolean prefix, CompletingParsedLine line, Map<LineReader.Option, Boolean> options, int errors,
                            String originalGroupName, LinkedHashMap<Matchers.MatcherType,
                            Function<Map<String, List<Candidate>>, Map<String, List<Candidate>>>> matchers) {
  }

  public static Function<Map<String, List<Candidate>>,
      Map<String, List<Candidate>>> simpleMatcher(Predicate<String> pred) {
    return m -> m.entrySet().stream()
        .filter(e -> pred.test(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
  public static Function<Map<String, List<Candidate>>,
      Map<String, List<Candidate>>> typoMatcher(String word, int errors, boolean caseInsensitive,
                                                String originalGroupName) {
    return m -> {
      Map<String, List<Candidate>> map = m.entrySet().stream()
          .filter(e -> ReaderUtils.distance(word, caseInsensitive ? e.getKey() : e.getKey().toLowerCase()) < errors)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      if (map.size() > 1) {
        map.computeIfAbsent(word, w -> new ArrayList<>())
            .add(new Candidate(word, word, originalGroupName, null, null, null, false));
      }
      return map;
    };
  }
}
