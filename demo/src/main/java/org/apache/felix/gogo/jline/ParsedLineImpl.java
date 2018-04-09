/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.jline;

import org.apache.felix.gogo.runtime.Parser.Program;
import org.apache.felix.gogo.runtime.Token;
import org.jline.reader.CompletingParsedLine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

//
// TODO: remove when implemented in Felix Gogo JLine
//
public class ParsedLineImpl implements CompletingParsedLine {

    private final Program program;
    private final String source;
    private final int cursor;
    private final List<String> tokens;
    private final int wordIndex;
    private final int wordCursor;
    private final CharSequence rawWord;
    private final int rawWordCursor;

    public ParsedLineImpl(Program program, Token line, int cursor, List<Token> tokens) {
        this.program = program;
        this.source = line.toString();
        this.cursor = cursor - line.start();
        this.tokens = new ArrayList<>();
        for (Token token : tokens) {
            this.tokens.add(unquote(token, null).toString());
        }
        int wi = tokens.size();
        int wc = 0;
        if (cursor >= 0) {
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                if (t.start() > cursor) {
                    wi = i;
                    wc = 0;
                    this.tokens.add(i, "");
                    break;
                }
                if (t.start() + t.length() >= cursor) {
                    wi = i;
                    wc = cursor - t.start();
                    break;
                }
            }
        }
        if (wi == tokens.size()) {
            this.tokens.add("");
            rawWord = "";
            wordCursor = 0;
        } else {
            rawWord = tokens.get(wi);
            int[] c = new int[] { wc };
            unquote(rawWord, c);
            wordCursor = c[0];
        }
        wordIndex = wi;
        rawWordCursor = wc;
    }

    public String word() {
        return tokens.get(wordIndex());
    }

    public int wordCursor() {
        return wordCursor;
    }

    public int wordIndex() {
        return wordIndex;
    }

    public List<String> words() {
        return tokens;
    }

    public String line() {
        return source;
    }

    public int cursor() {
        return cursor;
    }

    public Program program() {
        return program;
    }

    public int rawWordCursor() {
        return rawWordCursor;
    }

    public int rawWordLength() {
        return rawWord.length();
    }

    public CharSequence escape(CharSequence str, boolean complete) {
        StringBuilder sb = new StringBuilder(str);
        Predicate<Character> needToBeEscaped;
        char quote = 0;
        char first = rawWord.length() > 0 ? rawWord.charAt(0) : 0;
        if (first == '\'') {
            quote = '\'';
            needToBeEscaped = i -> i == '\'';
        } else if (first == '"') {
            quote = '"';
            needToBeEscaped = i -> i == '"';
        } else {
            needToBeEscaped = i -> i == ' ' || i == '\t';
        }
        for (int i = 0; i < sb.length(); i++) {
            if (needToBeEscaped.test(str.charAt(i))) {
                sb.insert(i++, '\\');
            }
        }
        if (quote != 0) {
            sb.insert(0, quote);
            if (complete) {
                sb.append(quote);
            }
        }
        return sb;
    }

    private CharSequence unquote(CharSequence arg, int[] cursor) {
        boolean hasEscape = false;
        for (int i = 0; i < arg.length(); i++) {
            int c = arg.charAt(i);
            if (c == '\\' || c == '"' || c == '\'') {
                hasEscape = true;
                break;
            }
        }
        if (!hasEscape) {
            return arg;
        }
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;
        StringBuilder buf = new StringBuilder(arg.length());
        for (int i = 0; i < arg.length(); i++) {
            if (cursor != null && cursor[0] == i) {
                cursor[0] = buf.length();
                cursor = null;
            }
            char c = arg.charAt(i);
            if (doubleQuoted && escaped) {
                if (c != '"' && c != '\\' && c != '$' && c != '%') {
                    buf.append('\\');
                }
                buf.append(c);
                escaped = false;
            } else if (escaped) {
                buf.append(c);
                escaped = false;
            } else if (singleQuoted) {
                if (c == '\'') {
                    singleQuoted = false;
                } else {
                    buf.append(c);
                }
            } else if (doubleQuoted) {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '\"') {
                    doubleQuoted = false;
                } else {
                    buf.append(c);
                }
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '\'') {
                singleQuoted = true;
            } else if (c == '"') {
                doubleQuoted = true;
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

}
