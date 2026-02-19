/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.nio.file.Paths;
import java.util.Map;

import org.jline.shell.Pipeline;
import org.jline.shell.Pipeline.Operator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PipelineParser}.
 */
public class PipelineParserTest {

    private PipelineParser parser;

    @BeforeEach
    void setUp() {
        parser = new PipelineParser();
    }

    @Test
    void parseSingleCommand() {
        Pipeline pipeline = parser.parse("ls -la");
        assertEquals(1, pipeline.stages().size());
        assertEquals("ls -la", pipeline.stages().get(0).commandLine());
        assertNull(pipeline.stages().get(0).operator());
        assertFalse(pipeline.isBackground());
    }

    @Test
    void parsePipeOperator() {
        Pipeline pipeline = parser.parse("ls | grep foo");
        assertEquals(2, pipeline.stages().size());
        assertEquals("ls", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.PIPE, pipeline.stages().get(0).operator());
        assertEquals("grep foo", pipeline.stages().get(1).commandLine());
        assertNull(pipeline.stages().get(1).operator());
    }

    @Test
    void parseMultiplePipes() {
        Pipeline pipeline = parser.parse("cat file | grep pattern | sort");
        assertEquals(3, pipeline.stages().size());
        assertEquals("cat file", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.PIPE, pipeline.stages().get(0).operator());
        assertEquals("grep pattern", pipeline.stages().get(1).commandLine());
        assertEquals(Operator.PIPE, pipeline.stages().get(1).operator());
        assertEquals("sort", pipeline.stages().get(2).commandLine());
    }

    @Test
    void parseFlipPipe() {
        Pipeline pipeline = parser.parse("cmd1 |; cmd2");
        assertEquals(2, pipeline.stages().size());
        assertEquals(Operator.FLIP, pipeline.stages().get(0).operator());
    }

    @Test
    void parseAndOperator() {
        Pipeline pipeline = parser.parse("mkdir dir && cd dir");
        assertEquals(2, pipeline.stages().size());
        assertEquals("mkdir dir", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.AND, pipeline.stages().get(0).operator());
        assertEquals("cd dir", pipeline.stages().get(1).commandLine());
    }

    @Test
    void parseOrOperator() {
        Pipeline pipeline = parser.parse("test -f file || echo missing");
        assertEquals(2, pipeline.stages().size());
        assertEquals("test -f file", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.OR, pipeline.stages().get(0).operator());
        assertEquals("echo missing", pipeline.stages().get(1).commandLine());
    }

    @Test
    void parseRedirect() {
        Pipeline pipeline = parser.parse("ls > output.txt");
        assertEquals(1, pipeline.stages().size());
        assertEquals("ls", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.REDIRECT, pipeline.stages().get(0).operator());
        assertEquals(Paths.get("output.txt"), pipeline.stages().get(0).redirectTarget());
        assertFalse(pipeline.stages().get(0).isAppend());
    }

    @Test
    void parseAppend() {
        Pipeline pipeline = parser.parse("echo hello >> log.txt");
        assertEquals(1, pipeline.stages().size());
        assertEquals("echo hello", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.APPEND, pipeline.stages().get(0).operator());
        assertEquals(Paths.get("log.txt"), pipeline.stages().get(0).redirectTarget());
        assertTrue(pipeline.stages().get(0).isAppend());
    }

    @Test
    void parseBackground() {
        Pipeline pipeline = parser.parse("long-running-cmd &");
        assertTrue(pipeline.isBackground());
        assertEquals(1, pipeline.stages().size());
        assertEquals("long-running-cmd", pipeline.stages().get(0).commandLine());
    }

    @Test
    void parsePipeAndRedirect() {
        Pipeline pipeline = parser.parse("ls | grep foo > results.txt");
        assertEquals(2, pipeline.stages().size());
        assertEquals("ls", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.PIPE, pipeline.stages().get(0).operator());
        assertEquals("grep foo", pipeline.stages().get(1).commandLine());
        assertEquals(Operator.REDIRECT, pipeline.stages().get(1).operator());
        assertEquals(Paths.get("results.txt"), pipeline.stages().get(1).redirectTarget());
    }

    @Test
    void parseQuotedStringsNotSplit() {
        Pipeline pipeline = parser.parse("echo \"hello | world\"");
        assertEquals(1, pipeline.stages().size());
        assertEquals("echo \"hello | world\"", pipeline.stages().get(0).commandLine());
    }

    @Test
    void parseSingleQuotedStringsNotSplit() {
        Pipeline pipeline = parser.parse("echo 'hello && world'");
        assertEquals(1, pipeline.stages().size());
        assertEquals("echo 'hello && world'", pipeline.stages().get(0).commandLine());
    }

    @Test
    void parseEscapedOperatorsNotSplit() {
        Pipeline pipeline = parser.parse("echo hello \\| world");
        assertEquals(1, pipeline.stages().size());
    }

    @Test
    void parseBracketedContentNotSplit() {
        Pipeline pipeline = parser.parse("echo (a | b)");
        assertEquals(1, pipeline.stages().size());
        assertEquals("echo (a | b)", pipeline.stages().get(0).commandLine());
    }

    @Test
    void parseEmptyLine() {
        Pipeline pipeline = parser.parse("");
        assertEquals(1, pipeline.stages().size());
        assertFalse(pipeline.isBackground());
    }

    @Test
    void parseNullLine() {
        Pipeline pipeline = parser.parse(null);
        assertEquals(1, pipeline.stages().size());
    }

    @Test
    void sourcePreservesOriginalLine() {
        String line = "ls -la | grep foo > out.txt";
        Pipeline pipeline = parser.parse(line);
        assertEquals(line, pipeline.source());
    }

    @Test
    void builderCreatesPipeline() {
        Pipeline pipeline = Pipeline.of("ls -la")
                .pipe("grep pattern")
                .redirect(Paths.get("output.txt"))
                .build();
        assertEquals(2, pipeline.stages().size());
        assertEquals("ls -la", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.PIPE, pipeline.stages().get(0).operator());
        assertEquals("grep pattern", pipeline.stages().get(1).commandLine());
        assertEquals(Operator.REDIRECT, pipeline.stages().get(1).operator());
        assertEquals(Paths.get("output.txt"), pipeline.stages().get(1).redirectTarget());
    }

    @Test
    void builderAndOr() {
        Pipeline pipeline = Pipeline.of("cmd1").and("cmd2").or("cmd3").build();
        assertEquals(3, pipeline.stages().size());
        assertEquals(Operator.AND, pipeline.stages().get(0).operator());
        assertEquals(Operator.OR, pipeline.stages().get(1).operator());
    }

    @Test
    void builderBackground() {
        Pipeline pipeline = Pipeline.of("cmd").background().build();
        assertTrue(pipeline.isBackground());
    }

    @Test
    void operatorFromSymbol() {
        assertEquals(Operator.PIPE, Operator.fromSymbol("|"));
        assertEquals(Operator.FLIP, Operator.fromSymbol("|;"));
        assertEquals(Operator.AND, Operator.fromSymbol("&&"));
        assertEquals(Operator.OR, Operator.fromSymbol("||"));
        assertEquals(Operator.REDIRECT, Operator.fromSymbol(">"));
        assertEquals(Operator.APPEND, Operator.fromSymbol(">>"));
        assertEquals(Operator.SEQUENCE, Operator.fromSymbol(";"));
        assertNull(Operator.fromSymbol("???"));
    }

    @Test
    void parseSequenceOperator() {
        Pipeline pipeline = parser.parse("cmd1 ; cmd2");
        assertEquals(2, pipeline.stages().size());
        assertEquals("cmd1", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.SEQUENCE, pipeline.stages().get(0).operator());
        assertEquals("cmd2", pipeline.stages().get(1).commandLine());
        assertNull(pipeline.stages().get(1).operator());
    }

    @Test
    void parseMultipleSequences() {
        Pipeline pipeline = parser.parse("cmd1 ; cmd2 ; cmd3");
        assertEquals(3, pipeline.stages().size());
        assertEquals(Operator.SEQUENCE, pipeline.stages().get(0).operator());
        assertEquals(Operator.SEQUENCE, pipeline.stages().get(1).operator());
        assertNull(pipeline.stages().get(2).operator());
    }

    @Test
    void parseSequenceWithPipe() {
        Pipeline pipeline = parser.parse("cmd1 | cmd2 ; cmd3");
        assertEquals(3, pipeline.stages().size());
        assertEquals(Operator.PIPE, pipeline.stages().get(0).operator());
        assertEquals(Operator.SEQUENCE, pipeline.stages().get(1).operator());
    }

    @Test
    void customOperatorReplacesDefault() {
        // "==>" replaces "|" as the PIPE operator
        PipelineParser custom = new PipelineParser(Map.of("==>", Operator.PIPE));
        Pipeline pipeline = custom.parse("cmd1 ==> cmd2");
        assertEquals(2, pipeline.stages().size());
        assertEquals("cmd1", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.PIPE, pipeline.stages().get(0).operator());
        assertEquals("cmd2", pipeline.stages().get(1).commandLine());

        // The default "|" is no longer recognized as PIPE
        pipeline = custom.parse("cmd1 | cmd2");
        assertEquals(1, pipeline.stages().size());
        assertEquals("cmd1 | cmd2", pipeline.stages().get(0).commandLine());
    }

    @Test
    void customOperatorLongestMatch() {
        PipelineParser custom = new PipelineParser(Map.of("=>", Operator.PIPE, "==>", Operator.FLIP));
        Pipeline pipeline = custom.parse("cmd1 ==> cmd2");
        assertEquals(2, pipeline.stages().size());
        assertEquals(Operator.FLIP, pipeline.stages().get(0).operator());
    }

    @Test
    void customOperatorRenameRedirect() {
        // Groovy use case: replace > and >> with |> and |>>
        PipelineParser custom = new PipelineParser(Map.of("|>", Operator.REDIRECT, "|>>", Operator.APPEND));
        // |> works as redirect
        Pipeline pipeline = custom.parse("ls |> output.txt");
        assertEquals(1, pipeline.stages().size());
        assertEquals("ls", pipeline.stages().get(0).commandLine());
        assertEquals(Operator.REDIRECT, pipeline.stages().get(0).operator());
        assertEquals(Paths.get("output.txt"), pipeline.stages().get(0).redirectTarget());

        // |>> works as append
        pipeline = custom.parse("echo hello |>> log.txt");
        assertEquals(1, pipeline.stages().size());
        assertEquals(Operator.APPEND, pipeline.stages().get(0).operator());
        assertTrue(pipeline.stages().get(0).isAppend());

        // > is no longer an operator, treated as part of the command
        pipeline = custom.parse("if (a > b) { echo yes }");
        assertEquals(1, pipeline.stages().size());
        assertEquals("if (a > b) { echo yes }", pipeline.stages().get(0).commandLine());
    }

    @Test
    void subclassOverrideMatchOperator() {
        PipelineParser custom = new PipelineParser() {
            @Override
            protected String matchOperator(String line, int pos) {
                // Custom: treat "::" as a pipe
                if (pos + 1 < line.length() && line.substring(pos, pos + 2).equals("::")) {
                    return "::";
                }
                return super.matchOperator(line, pos);
            }
        };
        // "::" is matched as an operator token but doesn't map to any Operator
        // in the operator table, so it's treated as text
        Pipeline pipeline = custom.parse("cmd1 :: cmd2");
        assertEquals(1, pipeline.stages().size());
    }

    @Test
    void builderSequence() {
        Pipeline pipeline = Pipeline.of("cmd1").sequence("cmd2").build();
        assertEquals(2, pipeline.stages().size());
        assertEquals(Operator.SEQUENCE, pipeline.stages().get(0).operator());
    }
}
