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

import org.apache.felix.gogo.runtime.EOFError;
import org.apache.felix.gogo.runtime.Parser.Program;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.gogo.runtime.SyntaxError;
import org.apache.felix.gogo.runtime.Token;
import org.jline.reader.ParsedLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//
// TODO: remove when implemented in Felix Gogo JLine
//
public class Parser implements org.jline.reader.Parser {

    public ParsedLine parse(String line, int cursor, ParseContext context) throws org.jline.reader.SyntaxError {
        try {
            return doParse(line, cursor, context);
        } catch (EOFError e) {
            throw new org.jline.reader.EOFError(e.line(), e.column(), e.getMessage(), e.missing());
        } catch (SyntaxError e) {
            throw new org.jline.reader.SyntaxError(e.line(), e.column(), e.getMessage());
        }
    }

    private ParsedLine doParse(String line, int cursor, ParseContext parseContext) throws SyntaxError {
        Program program = null;
        List<Statement> statements = null;
        String repaired = line;
        while (program == null) {
            try {
                org.apache.felix.gogo.runtime.Parser parser = new org.apache.felix.gogo.runtime.Parser(repaired);
                program = parser.program();
                statements = parser.statements();
            } catch (EOFError e) {
                // Make sure we don't loop forever
                if (parseContext == ParseContext.COMPLETE && repaired.length() < line.length() + 1024) {
                    repaired = repaired + " " + e.repair();
                } else {
                    throw e;
                }
            }
        }
        // Find corresponding statement
        Statement statement = null;
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement s = statements.get(i);
            if (s.start() <= cursor) {
                boolean isOk = true;
                // check if there are only spaces after the previous statement
                if (s.start() + s.length() < cursor) {
                    for (int j = s.start() + s.length(); isOk && j < cursor; j++) {
                        isOk = Character.isWhitespace(line.charAt(j));
                    }
                }
                statement = s;
                break;
            }
        }
        if (statement != null && statement.tokens() != null && !statement.tokens().isEmpty()) {
            if (repaired != line) {
                Token stmt = statement.subSequence(0, line.length() - statement.start());
                List<Token> tokens = new ArrayList<>(statement.tokens());
                Token last = tokens.get(tokens.size() - 1);
                tokens.set(tokens.size() - 1, last.subSequence(0, line.length() - last.start()));
                return new ParsedLineImpl(program, stmt, cursor, tokens);
            }
            return new ParsedLineImpl(program, statement, cursor, statement.tokens());
        } else {
            // TODO:
            return new ParsedLineImpl(program, program, cursor, Collections.<Token>singletonList(program));
        }
    }

}
