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
package org.jline.reader.impl.completer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.ArgParseCompleter.ArgParseNamespace;

/**
 * Completer implementation trying to mimic python's argparse standart lib, while also providing
 * command line parsing utilities. <br>
 * <br>
 * 1. define command, subcommands, and positional/named arguments for each. <br>
 * 2. define for arguments {@link ParsingAcceptor}s. <br>
 * 3. {@link #parse(Supplier, List)} results are stored into {@link ArgParseNamespace}
 * implementation. <br>
 * 4. {@link #getHelpText()} returns help text. <br>
 * @Note
 *
 * <pre>
 * 	- so far no default '-h' '--help' option, and no default handling of such.
 * 	- {@link #addNamedArgument(String, String, String, ParsingAcceptor, Completer...)} <br>
 * 	  Parses n args with same type => {@link ParsingAcceptor#parse(Object, int, String)} is called n
 *    times. <br>
 *    A parameter similar to 'nargs' may be implemented in the future. <br>
 * 	- Instead of storing the last parsed subcommand, similar to argparse a callable could be set or
 * 	  executed. But this can be implemented by the user himself by implementing {@link
 * 	  ArgParseNamespace}. <br>
 * 	- I chose users to implement {@link ArgParseNamespace} to enable better parsing error messages
 * 	  and leverage automatic type checking.
 * </pre>
 *
 * @param <T> {@link ArgParseNamespace} implementation passed to {@link ParsingAcceptor} during
 *     {@link #parse(Supplier, List)}
 * @author <a href="mailto:bschuess@yahoo.de">Balthasar Josef Schüss</a>
 */
public class ArgParseCompleter<T extends ArgParseNamespace> implements Completer {

  private final String command;
  private final String cmdDescription;

  private final List<PositionalArgument<T>> posArgs = new ArrayList<>();
  private final Set<NamedArgument<T>> namedArgs = new HashSet<>();
  private final Set<ArgParseCompleter<T>> subcommands = new HashSet<>();

  /**
   * Creates a new ArgParseCompleter similar to python's argparse standard lib. Supports parsing of
   * defined positional and named arguments as well.
   *
   * @param command The top level command to match.
   * @param description Description of the new top level command.
   */
  public ArgParseCompleter(String command, String description) {
    this.command = Objects.requireNonNull(command);
    this.cmdDescription = Objects.requireNonNull(description);
  }

  /**
   * Adds a new positional argument to the {@link ArgParseCompleter}. Positional arguments are in
   * order of addition to the {@link ArgParseCompleter}.
   *
   * @param completer the {@link Completer} to use for completing the positional argument.
   * @param valueConsumer @see {@link ParsingAcceptor}.
   * @param description the description of the positional argument.
   * @return
   */
  public ArgParseCompleter<T> addPosArgument(
      Completer completer, ParsingAcceptor<T> valueConsumer, String description) {
    String posArgName = String.format("%s:pos%s", command, posArgs.size());
    this.posArgs.add(new PositionalArgument<>(posArgName, description, valueConsumer, completer));
    return this;
  }

  /**
   * Adds a non optional argument.
   *
   * @see #addArgument(String, String, String, ParsingAcceptor, boolean, Completer...).
   */
  public ArgParseCompleter<T> addNamedArgument(
      String argName,
      String description,
      String argShorthand,
      ParsingAcceptor<T> valueConsumer,
      Completer... posArgs) {
    return addNamedArgument(argName, description, argShorthand, valueConsumer, false, posArgs);
  }
  /**
   * Adds a new named argument to the {@link ArgParseCompleter}.
   *
   * @param argName the full argument name, e.g. '--help'.
   * @param description the description of positional argument
   * @param argShorthand shorthand for the argument, e.g. '-h', is nullable.
   * @param valueConsumer @see {@link ParsingAcceptor}.
   * @param optional true if must not be present during parsing.
   * @param posArgs n {@link Completer}s for positional arguments. <br>
   *     parsed values must be of same type. <br>
   *     n=0 is allowed, but specifying won't result in an acceptor call during parsing. So it is
   *     ignored during parsing.
   * @return {@link ArgParseCompleter} - builder
   */
  public ArgParseCompleter<T> addNamedArgument(
      String argName,
      String description,
      String argShorthand,
      ParsingAcceptor<T> valueConsumer,
      boolean optional,
      Completer... posArgs) {
    this.namedArgs.add(
        new NamedArgument<>(argName, description, argShorthand, valueConsumer, optional, posArgs));
    return this;
  }

  /**
   * Creates a subcommand for the {@link ArgParseCompleter}. <br>
   * Commands and subcommands build a hierarchical tree structure <br>
   * Subcommands inherit all positional and named arguments from parent commands. <br>
   * E.g. 'command --arg0 pos0 subcommand --arg1 ...'
   *
   * @param command name of the subcommand.
   * @param description description of the subcommand.
   * @return {@link ArgParseCompleter} of subcommand.
   */
  public ArgParseCompleter<T> subCommand(String command, String description) {
    ArgParseCompleter<T> comp = new ArgParseCompleter<>(command, description);
    this.subcommands.add(comp);
    return comp;
  }

  private void parse(
      T config,
      Iterator<String> tokens,
      List<PositionalArgument<T>> unconsumedPosArgs,
      List<NamedArgument<T>> unconsumedNamedArgs)
      throws CommandParseException {

    unconsumedPosArgs.addAll(posArgs);
    unconsumedNamedArgs.addAll(namedArgs);

    while (tokens.hasNext()) {
      String token = tokens.next();
      Optional<NamedArgument<T>> matchedNamedArg =
          unconsumedNamedArgs
              .stream()
              .filter(
                  arg ->
                      arg.argName.equals(token)
                          || arg.argShorthand.filter(token::equals).isPresent())
              .findFirst();
      if (matchedNamedArg.isPresent()) {
        unconsumedNamedArgs.remove(matchedNamedArg.get());
        matchedNamedArg.get().parse(config, tokens);
        continue;
      }

      Optional<ArgParseCompleter<T>> matchedSubCommand =
          subcommands.stream().filter(cmd -> cmd.command.equals(token)).findFirst();
      if (matchedSubCommand.isPresent()) {
        config.setCommand(matchedSubCommand.get().command);
        matchedSubCommand.get().parse(config, tokens, unconsumedPosArgs, unconsumedNamedArgs);
        return;
      }

      // values starting with - are disallowed for positional args to yield better failure message.
      // otherwise positional argument may consume malformed named argument without failure.
      if (!unconsumedPosArgs.isEmpty() && !token.startsWith("-")) {
        unconsumedPosArgs.get(0).parse(config, token);
        unconsumedPosArgs.remove(0);
        continue;
      }

      // Throw exception if neither a subcommand, nor named argument and not either a positional
      // argument was matched.
      StringJoiner unrecognizedArgs = new StringJoiner("\n\t");
      unrecognizedArgs.add(token);
      tokens.forEachRemaining(unrecognizedArgs::add);
      throw new CommandParseException("Unexpected arguments:%n\t%s", unrecognizedArgs);
    }
  }

  /**
   * @param configFactory factory for the parse result.
   * @param tokens {@link List} of command line tokens.
   * @return the implementation of {@link ArgParseNamespace}, after accepting all values.
   * @throws CommandParseException
   */
  public T parse(Supplier<T> configFactory, List<String> tokens) throws CommandParseException {
    T config = configFactory.get();
    if (tokens.isEmpty()) {
      throw new CommandParseException("Args cannot be empty for command '%s'.", command);
    }
    String cmd = tokens.get(0);
    if (!cmd.equals(command)) {
      throw new CommandParseException("Arg0 must be '%s', but was '%s'", command, cmd);
    }
    config.setCommand(cmd);

    List<NamedArgument<T>> namedArgs = new ArrayList<>();
    List<PositionalArgument<T>> posArgs = new ArrayList<>();
    parse(config, tokens.listIterator(1), posArgs, namedArgs);

    if (!posArgs.isEmpty()) {
      StringJoiner posArgsJoiner = new StringJoiner("\n\t");
      posArgs.stream().map(PositionalArgument::toString).forEach(posArgsJoiner::add);
      throw new CommandParseException("Missing positional arguments:\n\t%s", posArgsJoiner);
    }
    List<NamedArgument<T>> remainingNonOptionalNamedArgs =
        namedArgs.stream().filter(namedArg -> !namedArg.optional).collect(Collectors.toList());
    if (!remainingNonOptionalNamedArgs.isEmpty()) {
      StringJoiner nonOptArgsJoiner = new StringJoiner("\n\t");
      remainingNonOptionalNamedArgs
          .stream()
          .map(NamedArgument::toString)
          .forEach(nonOptArgsJoiner::add);

      throw new CommandParseException(
          "Missing named non optional arguments:\\n\\t%s", nonOptArgsJoiner);
    }
    return config;
  }

  private Candidate candidate(String name, String description) {
    return new Candidate(name, name, null, description, null, null, true);
  }

  /** @return false if preconditions for completion were not met */
  private boolean complete(
      LineReader lineReader,
      ParsedLine line,
      Iterator<String> tokens,
      List<PositionalArgument<T>> unconsumedPosArgs,
      List<NamedArgument<T>> unconsumedNamedArguments,
      List<Candidate> candidates) {

    unconsumedPosArgs.addAll(posArgs);
    unconsumedNamedArguments.addAll(namedArgs);

    // loop consuming all previous words
    int posArgIndex = 0;
    for (; tokens.hasNext(); ) {
      String token = tokens.next();

      // consuming matched arguments
      Optional<NamedArgument<T>> matchedArg =
          unconsumedNamedArguments
              .stream()
              .filter(
                  arg ->
                      arg.argName.equals(token)
                          || arg.argShorthand.filter(token::equals).isPresent())
              .findFirst();
      if (matchedArg.isPresent()) {
        NamedArgument<T> arg = matchedArg.get();
        List<Candidate> cands = new ArrayList<>();
        boolean success = arg.complete(lineReader, line, tokens, cands);
        if (success && cands.isEmpty()) {
          continue; // argument was consumed completely and parsing of words should continue
        } else {
          // stop early because either matched arg was not completed successfully -> no
          // candidates
          // suggesting it worked.
          // we want fulfill positional arguments of named argument first.
          candidates.addAll(cands);
          return success;
        }
      }

      // consuming matched sub-command
      // carry over not consumed positional and named arguments
      Optional<ArgParseCompleter<T>> matchedSubCommand =
          subcommands.stream().filter(cmd -> cmd.command.equals(token)).findFirst();
      if (matchedSubCommand.isPresent()) {
        ArgParseCompleter<T> cmd = matchedSubCommand.get();
        boolean success =
            cmd.complete(
                lineReader,
                line,
                tokens,
                unconsumedPosArgs.subList(posArgIndex, unconsumedPosArgs.size()),
                unconsumedNamedArguments,
                candidates);
        return success;
      }

      // consuming positional argument if possible
      if (posArgIndex < unconsumedPosArgs.size()) {
        List<Candidate> generatedCandidates = new ArrayList<>();
        unconsumedPosArgs
            .get(posArgIndex)
            .complete(lineReader, new ArgumentLine(token, token.length()), generatedCandidates);

        boolean matches =
            generatedCandidates
                .stream()
                .anyMatch(cand -> cand.displ().toLowerCase().equals(token.toLowerCase()));
        if (matches) {
          posArgIndex += 1;
          continue;
        }
      }
      // unhandled unexpected input word
      return false;
    }

    unconsumedNamedArguments.forEach(
        arg -> {
          candidates.add(
              new Candidate(
                  arg.argName,
                  arg.argName,
                  String.format("'%s' named arguments:", command),
                  arg.argDescription,
                  null,
                  arg.argName,
                  true));
          arg.argShorthand
              .map(
                  shorthand ->
                      new Candidate(
                          shorthand,
                          shorthand,
                          String.format("'%s' named arguments:", command),
                          arg.argDescription,
                          null,
                          arg.argName,
                          true))
              .ifPresent(candidates::add);
        });

    if (posArgIndex < unconsumedPosArgs.size()) {
      unconsumedPosArgs
          .get(posArgIndex)
          .complete(lineReader, new ArgumentLine(line.word(), line.wordCursor()), candidates);
    }

    subcommands.forEach(
        cmd ->
            candidates.add(
                new Candidate(
                    cmd.command,
                    cmd.command,
                    "Subcommands:",
                    cmd.cmdDescription,
                    null,
                    null,
                    true)));

    return true;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    // removing the last word from the list makes this completer incompatible with TreeCompleter
    List<String> tokens = line.words().subList(0, line.wordIndex());
    List<Candidate> cands = new ArrayList<>();
    if (tokens.isEmpty()) {
      candidates.add(candidate(command, cmdDescription));
    } else if (tokens.get(0).equals(command)
        && complete(
            reader, line, tokens.listIterator(1), new ArrayList<>(), new ArrayList<>(), cands)) {
      candidates.addAll(cands);
    }
  }

  private String getHelpText(List<String> supercommands) {
    StringBuilder result = new StringBuilder();

    List<String> newSupercommands = new ArrayList<String>(supercommands);
    newSupercommands.add(command);
    StringJoiner joinResult = new StringJoiner(" ");
    newSupercommands.forEach(joinResult::add);
    result.append(joinResult.toString()).append(":\n").append(cmdDescription);

    posArgs.forEach(
        posArg -> {
          String entry = String.format("%n\t%-25s %s", posArg.argName, posArg.argDescription);
          result.append(entry);
        });

    namedArgs.forEach(
        namedArg -> {
          String entry =
              String.format(
                  "%n\t%-25s %s",
                  namedArg.argShorthand.map(str -> str + ", ").orElse("") + namedArg.argName,
                  namedArg.argDescription);
          result.append(entry);
        });

    result.append("\n\n");
    subcommands.forEach(cmd -> result.append(cmd.getHelpText(newSupercommands)));
    return result.toString();
  }

  /** @return Formatted help text of {@link ArgParseCompleter}. */
  public String getHelpText() {
    String result = getHelpText(new ArrayList<String>());
    return result;
  }

  /**
   * Enables users to parse a specific data type from {@link String} and register the value to their
   * custom implementation the {@link ArgParseNamespace}.
   *
   * @author Balthasar Schüss
   */
  @FunctionalInterface
  public static interface ParsingAcceptor<T> {
    /**
     * Parses custom value of parameter and registers it in the {@link ArgParseNamespace}
     * implementation.
     *
     * @param parsingResult The {@link ArgParseNamespace} implementation result instance.
     * @param value The {@link String} value to parse.
     * @throws Exception thrown exceptions are Wrapped by the {@link ArgParseCompleter} into {@link
     *     CommandParseException}s to provide meaningful error messages during parsing.
     */
    public void parse(T parsingResult, String value) throws Exception;
  }

  /**
   * Implement this class to store parsed values in a specialized data structure.
   *
   * @author Balthasar Schüss
   */
  public static interface ArgParseNamespace {
    /**
     * Called at least once for the outermost command. Called each time a more specific subcommand
     * is encountered. Can be used to determine custom callbacks depending on subcommand.
     *
     * @param command the current most specific parsed command.
     */
    public void setCommand(String command);
  }

  /** @author balthasar */
  public static class CommandParseException extends Exception {

    private static final long serialVersionUID = 1L;

    public CommandParseException(Throwable e, String format, Object... args) {
      super(String.format(format, args), e);
    }

    public CommandParseException(String format, Object... args) {
      super(String.format(format, args));
    }
  }

  private static class PositionalArgument<T> implements Completer {

    private final String argName;
    private final String argDescription;
    private final Completer completer;

    private final ParsingAcceptor<T> valueConsumer;

    private PositionalArgument(
        String name, String argDescription, ParsingAcceptor<T> valueConsumer, Completer completer) {
      this.argName = Objects.requireNonNull(name);
      this.argDescription = Objects.requireNonNull(argDescription);
      this.completer = Objects.requireNonNull(completer);
      this.valueConsumer = Objects.requireNonNull(valueConsumer);
    }

    private void parse(T config, String token) throws CommandParseException {
      try {
        valueConsumer.parse(config, token);
      } catch (Exception e) {
        throw new CommandParseException(e, "Parsing error '%s': %s", argName, e.getMessage());
      }
    }

    public void complete(LineReader lineReader, ParsedLine line, List<Candidate> candidates) {

      List<Candidate> cands = new ArrayList<>();
      completer.complete(lineReader, line, cands);
      cands.forEach(
          cand -> {
            Candidate extended =
                new Candidate(
                    cand.value(),
                    cand.displ(),
                    argDescription,
                    cand.descr(),
                    cand.suffix(),
                    cand.key(),
                    cand.complete());
            candidates.add(extended);
          });
    }

    @Override
    public String toString() {
      return argDescription;
    }
  }

  private static class NamedArgument<T> {

    private final String argName;
    private final String argDescription;
    private final Optional<String> argShorthand;
    private final List<PositionalArgument<T>> argumentPosArgs = new ArrayList<>();

    private final boolean optional;

    /**
     * see {@link ArgParseCompleter#addNamedArgument(String, String, String, ParsingAcceptor,
     * boolean, Completer...)}.
     */
    private NamedArgument(
        String argName,
        String description,
        String argShorthand,
        ParsingAcceptor<T> valueConsumer,
        boolean optional,
        Completer... posArgs) {

      Objects.requireNonNull(valueConsumer);
      this.argName = Objects.requireNonNull(argName);
      this.argShorthand = Optional.ofNullable(argShorthand);
      this.argDescription = Objects.requireNonNull(description);
      this.optional = optional;

      for (int i = 0; i < posArgs.length; i++) {
        String posArgName = String.format("%s:arg%s", argName, i);
        argumentPosArgs.add(new PositionalArgument<>(posArgName, "", valueConsumer, posArgs[i]));
      }
    }

    private void parse(T config, Iterator<String> tokens) throws CommandParseException {
      Iterator<PositionalArgument<T>> args = argumentPosArgs.iterator();

      while (tokens.hasNext() && args.hasNext()) {
        args.next().parse(config, tokens.next());
      }
      if (args.hasNext()) {
        StringJoiner remainingPosArgsJoiner = new StringJoiner("\t\n");
        args.forEachRemaining(posArg -> remainingPosArgsJoiner.add(posArg.toString()));
        throw new CommandParseException(
            "Missing positional arguments:%n%s", remainingPosArgsJoiner);
      }
    }

    /**
     * @see Completer#complete(LineReader, ParsedLine, List) No candidates are added, if matching of
     *     all positional arguments succeeded. Candidates are added, if all previous positional
     *     arguments matched and words yields no more items.
     * @return false if matching present positional arguments failed.
     */
    private boolean complete(
        LineReader lineReader,
        ParsedLine line,
        Iterator<String> words,
        List<Candidate> candidates) {
      int posArgIndex = 0;
      for (; words.hasNext(); ) {
        if (posArgIndex < argumentPosArgs.size()) {
          String word = words.next();

          List<Candidate> generatedCandidates = new ArrayList<>();
          argumentPosArgs
              .get(posArgIndex)
              .complete(lineReader, new ArgumentLine(word, word.length()), generatedCandidates);
          boolean matches =
              generatedCandidates
                  .stream()
                  .anyMatch(cand -> cand.displ().toLowerCase().equals(word.toLowerCase()));

          if (matches) {
            posArgIndex += 1;
            continue;
          }
          return false;
        } else {
          // break return prematurely without adding any candidates, because it was
          // consumed successfully.
          return true;
        }
      }

      if (posArgIndex < argumentPosArgs.size()) {
        argumentPosArgs
            .get(posArgIndex)
            .complete(lineReader, new ArgumentLine(line.word(), line.wordCursor()), candidates);
      }

      return true;
    }
  }

  private static class ArgumentLine implements ParsedLine {
    private final String word;
    private final int cursor;

    ArgumentLine(String word, int cursor) {
      this.word = word;
      this.cursor = cursor;
    }

    @Override
    public String word() {
      return word;
    }

    @Override
    public int wordCursor() {
      return cursor;
    }

    @Override
    public int wordIndex() {
      return 0;
    }

    @Override
    public List<String> words() {
      return Collections.singletonList(word);
    }

    @Override
    public String line() {
      return word;
    }

    @Override
    public int cursor() {
      return cursor;
    }
  }
}
