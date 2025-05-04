/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Non-deterministic Finite Automaton (NFA) implementation for pattern matching.
 * <p>
 * This class implements a Thompson NFA for regular expression matching. It converts
 * a regular expression to postfix notation, builds an NFA, and uses it to match
 * sequences of objects against the pattern.
 * </p>
 * <p>
 * The implementation is based on the algorithm described in Russ Cox's article:
 * <a href="https://swtch.com/~rsc/regexp/regexp1.html">https://swtch.com/~rsc/regexp/regexp1.html</a>
 * </p>
 *
 * @param <T> the type of objects to match against the pattern
 */
public class NfaMatcher<T> {

    /** The regular expression pattern */
    private final String regexp;
    /** Function to match an input against a pattern */
    private final BiFunction<T, String, Boolean> matcher;
    /** The start state of the NFA */
    private volatile State start;

    /**
     * Creates a new NfaMatcher with the specified regular expression and matcher function.
     *
     * @param regexp the regular expression pattern
     * @param matcher the function to match an input against a pattern
     */
    public NfaMatcher(String regexp, BiFunction<T, String, Boolean> matcher) {
        this.regexp = regexp;
        this.matcher = matcher;
    }

    /**
     * Compiles the regular expression into an NFA.
     * <p>
     * This method is called automatically when needed, but can be called explicitly
     * to precompile the pattern.
     * </p>
     */
    public void compile() {
        if (start == null) {
            start = toNfa(toPostFix(regexp));
        }
    }

    /**
     * Matches a list of arguments against the pattern.
     * <p>
     * This method uses the NFA to determine if the sequence of arguments matches
     * the regular expression pattern.
     * </p>
     *
     * @param args the list of arguments to match
     * @return true if the arguments match the pattern, false otherwise
     */
    public boolean match(List<T> args) {
        Set<State> clist = new HashSet<>();
        compile();
        addState(clist, start);
        for (T arg : args) {
            Set<State> nlist = new HashSet<>();
            clist.stream()
                    .filter(s -> !Objects.equals(State.Match, s.c) && !Objects.equals(State.Split, s.c))
                    .filter(s -> matcher.apply(arg, s.c))
                    .forEach(s -> addState(nlist, s.out));
            clist = nlist;
        }
        return clist.stream().anyMatch(s -> Objects.equals(State.Match, s.c));
    }

    /**
     * Returns the list of possible matcher names for the next object
     *
     * @param args input list
     * @return the list of possible matcher names for the next object
     */
    public Set<String> matchPartial(List<T> args) {
        Set<State> clist = new HashSet<>();
        compile();
        addState(clist, start);
        for (T arg : args) {
            Set<State> nlist = new HashSet<>();
            clist.stream()
                    .filter(s -> !Objects.equals(State.Match, s.c) && !Objects.equals(State.Split, s.c))
                    .filter(s -> matcher.apply(arg, s.c))
                    .forEach(s -> addState(nlist, s.out));
            clist = nlist;
        }
        return clist.stream()
                .filter(s -> !Objects.equals(State.Match, s.c) && !Objects.equals(State.Split, s.c))
                .map(s -> s.c)
                .collect(Collectors.toSet());
    }

    /**
     * Adds a state to the set of states, handling split states recursively.
     *
     * @param l the set of states to add to
     * @param s the state to add
     */
    void addState(Set<State> l, State s) {
        if (s != null && l.add(s)) {
            if (Objects.equals(State.Split, s.c)) {
                addState(l, s.out);
                addState(l, s.out1);
            }
        }
    }

    /**
     * Converts a postfix expression to an NFA.
     *
     * @param postfix the postfix expression
     * @return the start state of the NFA
     */
    static State toNfa(List<String> postfix) {
        Deque<Frag> stack = new ArrayDeque<>();
        Frag e1, e2, e;
        State s;
        for (String p : postfix) {
            switch (p) {
                case ".":
                    e2 = stack.pollLast();
                    e1 = stack.pollLast();
                    e1.patch(e2.start);
                    stack.offerLast(new Frag(e1.start, e2.out));
                    break;
                case "|":
                    e2 = stack.pollLast();
                    e1 = stack.pollLast();
                    s = new State(State.Split, e1.start, e2.start);
                    stack.offerLast(new Frag(s, e1.out, e2.out));
                    break;
                case "?":
                    e = stack.pollLast();
                    s = new State(State.Split, e.start, null);
                    stack.offerLast(new Frag(s, e.out, s::setOut1));
                    break;
                case "*":
                    e = stack.pollLast();
                    s = new State(State.Split, e.start, null);
                    e.patch(s);
                    stack.offerLast(new Frag(s, s::setOut1));
                    break;
                case "+":
                    e = stack.pollLast();
                    s = new State(State.Split, e.start, null);
                    e.patch(s);
                    stack.offerLast(new Frag(e.start, s::setOut1));
                    break;
                default:
                    s = new State(p, null, null);
                    stack.offerLast(new Frag(s, s::setOut));
                    break;
            }
        }
        e = stack.pollLast();
        if (!stack.isEmpty()) {
            throw new IllegalStateException("Wrong postfix expression, " + stack.size() + " elements remaining");
        }
        e.patch(new State(State.Match, null, null));
        return e.start;
    }

    /**
     * Converts a regular expression to postfix notation.
     *
     * @param regexp the regular expression
     * @return the postfix expression as a list of tokens
     */
    static List<String> toPostFix(String regexp) {
        List<String> postfix = new ArrayList<>();
        int s = -1;
        int natom = 0;
        int nalt = 0;
        Deque<Integer> natoms = new ArrayDeque<>();
        Deque<Integer> nalts = new ArrayDeque<>();
        for (int i = 0; i < regexp.length(); i++) {
            char c = regexp.charAt(i);
            // Scan identifiers
            if (Character.isJavaIdentifierPart(c)) {
                if (s < 0) {
                    s = i;
                }
                continue;
            }
            // End of identifier
            if (s >= 0) {
                if (natom > 1) {
                    --natom;
                    postfix.add(".");
                }
                postfix.add(regexp.substring(s, i));
                natom++;
                s = -1;
            }
            // Ignore space
            if (Character.isWhitespace(c)) {
                continue;
            }
            // Special characters
            switch (c) {
                case '(':
                    if (natom > 1) {
                        --natom;
                        postfix.add(".");
                    }
                    nalts.offerLast(nalt);
                    natoms.offerLast(natom);
                    nalt = 0;
                    natom = 0;
                    break;
                case '|':
                    if (natom == 0) {
                        throw new IllegalStateException("unexpected '" + c + "' at pos " + i);
                    }
                    while (--natom > 0) {
                        postfix.add(".");
                    }
                    nalt++;
                    break;
                case ')':
                    if (nalts.isEmpty() || natom == 0) {
                        throw new IllegalStateException("unexpected '" + c + "' at pos " + i);
                    }
                    while (--natom > 0) {
                        postfix.add(".");
                    }
                    for (; nalt > 0; nalt--) {
                        postfix.add("|");
                    }
                    nalt = nalts.pollLast();
                    natom = natoms.pollLast();
                    natom++;
                    break;
                case '*':
                case '+':
                case '?':
                    if (natom == 0) {
                        throw new IllegalStateException("unexpected '" + c + "' at pos " + i);
                    }
                    postfix.add(String.valueOf(c));
                    break;
                default:
                    throw new IllegalStateException("unexpected '" + c + "' at pos " + i);
            }
        }
        // End of identifier
        if (s >= 0) {
            if (natom > 1) {
                --natom;
                postfix.add(".");
            }
            postfix.add(regexp.substring(s));
            natom++;
        }
        // Append
        while (--natom > 0) {
            postfix.add(".");
        }
        // Alternatives
        for (; nalt > 0; nalt--) {
            postfix.add("|");
        }
        return postfix;
    }

    /**
     * Represents a state in the NFA.
     */
    static class State {

        static final String Match = "++MATCH++";
        static final String Split = "++SPLIT++";

        final String c;
        State out;
        State out1;

        public State(String c, State out, State out1) {
            this.c = c;
            this.out = out;
            this.out1 = out1;
        }

        public void setOut(State out) {
            this.out = out;
        }

        public void setOut1(State out1) {
            this.out1 = out1;
        }
    }

    /**
     * Represents a fragment of the NFA during construction.
     */
    private static class Frag {
        final State start;
        final List<Consumer<State>> out = new ArrayList<>();

        public Frag(State start, Collection<Consumer<State>> l) {
            this.start = start;
            this.out.addAll(l);
        }

        public Frag(State start, Collection<Consumer<State>> l1, Collection<Consumer<State>> l2) {
            this.start = start;
            this.out.addAll(l1);
            this.out.addAll(l2);
        }

        public Frag(State start, Consumer<State> c) {
            this.start = start;
            this.out.add(c);
        }

        public Frag(State start, Collection<Consumer<State>> l, Consumer<State> c) {
            this.start = start;
            this.out.addAll(l);
            this.out.add(c);
        }

        public void patch(State s) {
            out.forEach(c -> c.accept(s));
        }
    }
}
