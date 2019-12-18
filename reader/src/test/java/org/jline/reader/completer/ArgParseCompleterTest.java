package org.jline.reader.completer;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jline.reader.impl.ReaderTestSupport;
import org.jline.reader.impl.completer.ArgParseCompleter;
import org.jline.reader.impl.completer.ArgParseCompleter.ArgParseNamespace;
import org.jline.reader.impl.completer.ArgParseCompleter.CommandParseException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.Before;
import org.junit.Test;

/*
 * Copyright (C) Dirk Beyer.
 * All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * This software was also released under the Apache License, Version 2.0 (the "License"), elsewhere.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 */
public class ArgParseCompleterTest extends ReaderTestSupport {

  private Map<String, String> results = new HashMap<>();
  private ArgParseNamespace namespace;

  @Before
  public void setup() {
    results.clear();
    namespace =
        new ArgParseNamespace() {

          @Override
          public void setCommand(String subcommand) {
            results.put("cmd", subcommand);
          }
        };
  }

  public ArgParseCompleter<ArgParseNamespace> createCompleter() {
    ArgParseCompleter<ArgParseNamespace> completer = new ArgParseCompleter<>("test", "description");

    completer
        .addNamedArgument(
            "--named0",
            "named0d",
            "-n0",
            (ns, r) -> results.put("--named0", r),
            true, // optional
            new StringsCompleter("rnamed0"))
        .addNamedArgument(
            "--named1",
            "named1d",
            "-n1",
            (ns, r) -> results.merge("--named1", r, (a, b) -> a + b),
            new StringsCompleter("rnamed1"),
            new StringsCompleter("rnamed2"))
        .addPosArgument(
            new StringsCompleter("foo", "bar"), (ns, r) -> results.put("pos0", r), "pos0d")
        .addPosArgument(
            new StringsCompleter("foz", "baz"), (ns, r) -> results.put("pos1", r), "pos1d");
    return completer;
  }

  @Test
  public void testCompleteSimple() throws Exception {
    reader.setCompleter(createCompleter());

    // complete command
    assertBuffer("test ", new TestBuffer("t").tab());
    assertBuffer("test ", new TestBuffer("test").tab());
    assertBuffer("nah", new TestBuffer("nah").tab());

    // complete pos0
    assertBuffer("test foo ", new TestBuffer("test f").tab());
    assertBuffer("test bar ", new TestBuffer("test b").tab());
    assertBuffer("test k", new TestBuffer("test k").tab());
    // assertBuffer("test bar y", new TestBuffer("test b y").left().left());
    assertBuffer("test --named0 rnamed0 bar ", new TestBuffer("test --named0 rnamed0 b").tab());

    // complete pos1
    assertBuffer("test kek b", new TestBuffer("test kek b").tab());
    assertBuffer("test foo baz ", new TestBuffer("test foo b").tab());
    assertBuffer("test foo foz ", new TestBuffer("test foo f").tab());
    assertBuffer("test foo k", new TestBuffer("test foo k").tab());
    assertBuffer(
        "test --named0 rnamed0 bar baz ", new TestBuffer("test --named0 rnamed0 bar b").tab());

    // complete named0
    assertBuffer("test --named0 ", new TestBuffer("test --named0").tab());
    assertBuffer("test --named0 rnamed0 ", new TestBuffer("test --named0 ").tab());
    assertBuffer("test -n0 ", new TestBuffer("test -n0").tab());
    assertBuffer("test -n0 rnamed0 ", new TestBuffer("test -n0 ").tab());
    assertBuffer("test bar baz --named0 ", new TestBuffer("test bar baz --named0").tab());
    assertBuffer("test --named", new TestBuffer("test --na").tab());

    // complete named1
    assertBuffer("test --named1 ", new TestBuffer("test --named1").tab());
    assertBuffer("test --named1 rnamed1 ", new TestBuffer("test --named1 ").tab());
    assertBuffer("test --named1 rnamed1 rnamed2 ", new TestBuffer("test --named1 rnamed1 ").tab());
    assertBuffer("test --named1 --rnamed2", new TestBuffer("test --named1 --rnamed2").tab());
    assertBuffer("test --named0 --named1", new TestBuffer("test --named0 --named1").tab());
    assertBuffer(
        "test --named0 rnamed0 --named1 ", new TestBuffer("test --named0 rnamed0 --named1").tab());
  }

  @Test
  public void testParseSimplePermutations0() throws Exception {
    assertParseSimplePositions(
        "test", "--named0", "val0", "--named1", "val1", "val2", "pos0v", "pos1v");
  }

  @Test
  public void testParseSimplePermutations1() throws Exception {
    assertParseSimplePositions(
        "test", "--named1", "val1", "val2", "--named0", "val0", "pos0v", "pos1v");
  }

  @Test
  public void testParseSimplePermutations2() throws Exception {
    assertParseSimplePositions(
        "test", "pos0v", "--named0", "val0", "pos1v", "--named1", "val1", "val2");
  }

  @Test
  public void testParseSimplePermutations3() throws Exception {
    assertParseSimplePositions("test", "pos0v", "-n0", "val0", "pos1v", "-n1", "val1", "val2");
  }

  private void assertParseSimplePositions(String... tokens) throws CommandParseException {
    ArgParseCompleter<ArgParseNamespace> completer = createCompleter();
    completer.parse(() -> namespace, Arrays.asList(tokens));
    assertParseSimplePositions0("test");
  }

  private void assertParseSimplePositions0(String expectedCommand) {
    assertThat(results.get("cmd"), equalTo(expectedCommand));
    assertThat(results.get("pos0"), equalTo("pos0v"));
    assertThat(results.get("pos1"), equalTo("pos1v"));
    assertThat(results.get("--named0"), equalTo("val0"));
    assertThat(results.get("--named1"), equalTo("val1val2"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseSimpleMissingPos() throws Exception {
    createCompleter()
        .parse(
            () -> namespace,
            Arrays.asList("test", "pos0v", "--named0", "val0", "--named1", "val1", "val2"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseSimpleMissingNonOptArg() throws Exception {
    createCompleter()
        .parse(() -> namespace, Arrays.asList("test", "pos0v", "--named0", "val0", "pos1v"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseSimpleMissingPosArgInNamedArg() throws Exception {
    createCompleter()
        .parse(
            () -> namespace,
            Arrays.asList("test", "pos0v", "--named0", "val0", "pos1v", "--named1", "val1"));
  }

  public void testParseSimpleMissingOptionalArg() throws Exception {
    createCompleter()
        .parse(
            () -> namespace, Arrays.asList("test", "pos0v", "pos1v", "--named1", "val1", "val2"));
  }

  public ArgParseCompleter<ArgParseNamespace> createComplexCompleter() {
    ArgParseCompleter<ArgParseNamespace> completer = createCompleter();
    completer
        .subCommand("subA", "subAd")
        .addNamedArgument(
            "--namedA0",
            "namedA0d",
            "-nA0",
            (ns, r) -> results.put("--namedA0", r),
            new StringsCompleter("rnamedA0"))
        .addPosArgument(
            new StringsCompleter("fooA", "barA"), (ns, r) -> results.put("posA0", r), "posA0d");
    completer
        .subCommand("subB", "subBd")
        .addNamedArgument(
            "--namedB0",
            "namedB0d",
            "-nB0",
            (ns, r) -> results.put("--namedB0", r),
            new StringsCompleter("rnamedB0"))
        .addPosArgument(
            new StringsCompleter("fooB", "barB"), (ns, r) -> results.put("posB0", r), "posB0d");
    return completer;
  }

  @Test
  public void testCompleteComplex() throws IOException {
    reader.setCompleter(createComplexCompleter());

    // test subcommand
    assertBuffer("test sub", new TestBuffer("test s").tab());
    assertBuffer("test foo sub", new TestBuffer("test foo s").tab());
    assertBuffer("test foo baz sub", new TestBuffer("test foo baz s").tab());
    assertBuffer(
        "test foo --named0 rnamed0 baz sub",
        new TestBuffer("test foo --named0 rnamed0 baz s").tab());

    // test subcommand pos args
    assertBuffer("test subA foo ", new TestBuffer("test subA f").tab());
    assertBuffer("test subA foo baz fooA ", new TestBuffer("test subA foo baz f").tab());
    assertBuffer("test subB foo baz fooB ", new TestBuffer("test subB foo baz f").tab());
    assertBuffer("test foo subA baz fooA ", new TestBuffer("test foo subA baz f").tab());

    // test subcommand named args
    assertBuffer("test subA --named", new TestBuffer("test subA --na").tab());
    assertBuffer("test subA --namedA0 ", new TestBuffer("test subA --namedA").tab());
    assertBuffer("test subA --namedB", new TestBuffer("test subA --namedB").tab());
    assertBuffer(
        "test --namedA0 subA --named0", new TestBuffer("test --namedA0 subA --named0").tab());
  }

  @Test
  public void testParseComplexPermutationsA0() throws CommandParseException {
    assertParseComplexPositionsA(
        "test",
        "pos0v",
        "-n0",
        "val0",
        "pos1v",
        "-n1",
        "val1",
        "val2",
        "subA",
        "--namedA0",
        "nA0v",
        "posA0");
  }

  @Test
  public void testParseComplexPermutationsA1() throws CommandParseException {
    assertParseComplexPositionsA(
        "test",
        "pos0v",
        "-n0",
        "val0",
        "pos1v",
        "-n1",
        "val1",
        "val2",
        "subA",
        "posA0",
        "--namedA0",
        "nA0v");
  }

  @Test
  public void testParseComplexPermutationsA2() throws CommandParseException {
    assertParseComplexPositionsA(
        "test",
        "pos0v",
        "-n0",
        "val0",
        "-n1",
        "val1",
        "val2",
        "subA",
        "pos1v",
        "posA0",
        "--namedA0",
        "nA0v");
  }

  @Test
  public void testParseComplexPermutationsA3() throws CommandParseException {
    assertParseComplexPositionsA(
        "test",
        "pos0v",
        "-n1",
        "val1",
        "val2",
        "subA",
        "pos1v",
        "posA0",
        "--namedA0",
        "nA0v",
        "-n0",
        "val0");
  }

  private void assertParseComplexPositionsA(String... tokens) throws CommandParseException {
    createComplexCompleter().parse(() -> namespace, Arrays.asList(tokens));
    assertParseSimplePositions0("subA");
    assertThat(results.get("--namedA0"), equalTo("nA0v"));
    assertThat(results.get("posA0"), equalTo("posA0"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseComplexMissingPos() throws Exception {
    createComplexCompleter()
        .parse(
            () -> namespace,
            Arrays.asList(
                "test",
                "pos0v",
                "-n0",
                "val0",
                "pos1v",
                "-n1",
                "val1",
                "val2",
                "subA",
                "--namedA0",
                "nA0v"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseComplexMissingNonOptional() throws Exception {
    createComplexCompleter()
        .parse(
            () -> namespace,
            Arrays.asList(
                "test", "pos0v", "-n0", "val0", "pos1v", "-n1", "val1", "val2", "subA", "posA0"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseComplexInvalidPosOrder() throws Exception {
    createComplexCompleter()
        .parse(
            () -> namespace,
            Arrays.asList(
                "test",
                "pos0v",
                "-n0",
                "val0",
                "pos1v",
                "-n1",
                "val1",
                "val2",
                "subA",
                "--namedA0",
                "nA0v"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseComplexInvalidPos() throws Exception {
    createComplexCompleter()
        .parse(
            () -> namespace,
            Arrays.asList(
                "test",
                "pos0v",
                "-n0",
                "val0",
                "pos1v",
                "-n1",
                "val1",
                "val2",
                "posA0",
                "subA",
                "--namedA0",
                "nA0v"));
  }

  @Test(expected = CommandParseException.class)
  public void testParseComplexMissingInvalidPosNamed() throws Exception {
    createComplexCompleter()
        .parse(
            () -> namespace,
            Arrays.asList(
                "test",
                "pos0v",
                "-n0",
                "val0",
                "pos1v",
                "-n1",
                "val1",
                "val2",
                "--namedA0",
                "nA0v",
                "subA",
                "posA0"));
  }

  @Test
  public void testArgParseHelpText() {
    assertNotNull(createComplexCompleter().getHelpText());
  }
}
