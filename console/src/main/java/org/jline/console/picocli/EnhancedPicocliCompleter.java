/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.picocli;

import java.nio.file.Path;
import java.util.List;

import org.jline.console.CommandContext;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.FileNameCompleter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.shell.jline3.PicocliJLineCompleter;

/**
 * Enhanced completer for Picocli commands that provides context-aware completion.
 * <p>
 * This completer extends the basic PicocliJLineCompleter with additional features:
 * <ul>
 *   <li>File path completion for Path parameters</li>
 *   <li>Enum value completion for enum parameters</li>
 *   <li>Context-aware completion based on current directory</li>
 *   <li>Rich candidate descriptions with styling</li>
 * </ul>
 */
public class EnhancedPicocliCompleter implements Completer {

    private final PicocliJLineCompleter delegate;
    private final CommandContext context;
    private final FileNameCompleter fileCompleter;

    /**
     * Creates a new enhanced Picocli completer.
     * @param commandSpec the Picocli command specification
     * @param context the command context for enhanced completion
     */
    public EnhancedPicocliCompleter(CommandSpec commandSpec, CommandContext context) {
        this.delegate = new PicocliJLineCompleter(commandSpec);
        this.context = context;
        this.fileCompleter = new FileNameCompleter();
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // First, get basic Picocli completion
        delegate.complete(reader, line, candidates);
        
        // Then enhance with context-aware completion
        enhanceCompletion(reader, line, candidates);
    }

    private void enhanceCompletion(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return;
        }

        // Check if we're completing a file path parameter
        if (isFilePathCompletion(line)) {
            candidates.clear();
            addFilePathCandidates(reader, line, candidates);
        }
        
        // Check if we're completing an enum parameter
        else if (isEnumCompletion(line)) {
            enhanceEnumCandidates(candidates);
        }
        
        // Enhance all candidates with better descriptions and styling
        enhanceCandidateDescriptions(candidates);
    }

    private boolean isFilePathCompletion(ParsedLine line) {
        // Simple heuristic: if the current word looks like a path
        String word = line.word();
        return word.contains("/") || word.contains("\\") || word.equals(".") || word.equals("..");
    }

    private boolean isEnumCompletion(ParsedLine line) {
        // This would need more sophisticated parsing to determine if we're
        // completing an enum parameter - simplified for now
        return false;
    }

    private void addFilePathCandidates(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Use JLine's built-in file completer but with our context
        if (context.currentDir() != null) {
            // Set the current directory for file completion
            System.setProperty("user.dir", context.currentDir().toString());
        }
        
        fileCompleter.complete(reader, line, candidates);
        
        // Enhance file candidates with type information
        for (int i = 0; i < candidates.size(); i++) {
            Candidate candidate = candidates.get(i);
            Path path = context.currentDir().resolve(candidate.value());
            
            String description = candidate.descr();
            String suffix = candidate.suffix();
            
            if (java.nio.file.Files.isDirectory(path)) {
                description = description != null ? description + " (directory)" : "directory";
                if (suffix == null || suffix.isEmpty()) {
                    suffix = "/";
                }
            } else if (java.nio.file.Files.isRegularFile(path)) {
                description = description != null ? description + " (file)" : "file";
            }
            
            candidates.set(i, new Candidate(
                candidate.value(),
                candidate.displ(),
                candidate.group(),
                description,
                suffix,
                candidate.key(),
                candidate.complete()
            ));
        }
    }

    private void enhanceEnumCandidates(List<Candidate> candidates) {
        // Add styling and better descriptions for enum values
        for (int i = 0; i < candidates.size(); i++) {
            Candidate candidate = candidates.get(i);
            String styledDisplay = new AttributedString(candidate.displ(),
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)).toAnsi();
            
            candidates.set(i, new Candidate(
                candidate.value(),
                styledDisplay,
                candidate.group(),
                candidate.descr(),
                candidate.suffix(),
                candidate.key(),
                candidate.complete()
            ));
        }
    }

    private void enhanceCandidateDescriptions(List<Candidate> candidates) {
        // Add better styling and descriptions to all candidates
        for (int i = 0; i < candidates.size(); i++) {
            Candidate candidate = candidates.get(i);
            
            // Skip if already enhanced
            if (candidate.descr() != null && candidate.descr().contains("(")) {
                continue;
            }
            
            String description = candidate.descr();
            String group = candidate.group();
            
            // Add group information to description if available
            if (group != null && !group.isEmpty() && (description == null || !description.contains(group))) {
                description = description != null ? description + " (" + group + ")" : group;
            }
            
            // Style option candidates differently
            String display = candidate.displ();
            if (candidate.value().startsWith("-")) {
                display = new AttributedString(candidate.displ(),
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)).toAnsi();
            }
            
            candidates.set(i, new Candidate(
                candidate.value(),
                display,
                candidate.group(),
                description,
                candidate.suffix(),
                candidate.key(),
                candidate.complete()
            ));
        }
    }
}
