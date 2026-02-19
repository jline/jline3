/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.jline.shell.AliasManager;

/**
 * Default in-memory implementation of {@link AliasManager} with optional file persistence.
 * <p>
 * Aliases are stored in a {@link LinkedHashMap} to preserve insertion order.
 * When a {@code persistFile} is provided, aliases can be loaded from and saved to
 * a simple {@code name=expansion} format (one alias per line).
 * <p>
 * The {@link #expand(String)} method supports parameter substitution in alias
 * expansions using {@code $1}, {@code $2}, etc. for positional parameters,
 * and {@code $@} for all remaining arguments. A recursion guard prevents
 * infinite alias expansion loops.
 *
 * @since 4.0
 */
public class DefaultAliasManager implements AliasManager {

    private static final int MAX_EXPANSION_DEPTH = 10;

    private final Map<String, String> aliases = new LinkedHashMap<>();
    private final Path persistFile;

    /**
     * Creates an in-memory alias manager with no persistence.
     */
    public DefaultAliasManager() {
        this(null);
    }

    /**
     * Creates an alias manager with optional file persistence.
     * <p>
     * The persistence file uses a simple {@code name=expansion} format, one alias per line.
     * Lines starting with {@code #} are treated as comments.
     *
     * @param persistFile the file to persist aliases to, or null for in-memory only
     */
    public DefaultAliasManager(Path persistFile) {
        this.persistFile = persistFile;
    }

    @Override
    public void setAlias(String name, String expansion) {
        aliases.put(name, expansion);
    }

    @Override
    public boolean removeAlias(String name) {
        return aliases.remove(name) != null;
    }

    @Override
    public String getAlias(String name) {
        return aliases.get(name);
    }

    @Override
    public Map<String, String> aliases() {
        return Collections.unmodifiableMap(aliases);
    }

    @Override
    public String expand(String line) {
        if (line == null || line.trim().isEmpty()) {
            return line;
        }
        return expand(line, new HashSet<>(), 0);
    }

    private String expand(String line, Set<String> visited, int depth) {
        if (depth >= MAX_EXPANSION_DEPTH) {
            return line;
        }

        String trimmed = line.trim();
        // Split into first word and rest
        int spaceIdx = trimmed.indexOf(' ');
        String firstWord = spaceIdx >= 0 ? trimmed.substring(0, spaceIdx) : trimmed;
        String rest = spaceIdx >= 0 ? trimmed.substring(spaceIdx + 1) : "";

        String expansion = aliases.get(firstWord);
        if (expansion == null || visited.contains(firstWord)) {
            return line;
        }

        visited.add(firstWord);

        // Check if the expansion uses parameter markers
        if (expansion.contains("$")) {
            String[] restArgs = rest.isEmpty() ? new String[0] : rest.split("\\s+");
            StringBuilder result = new StringBuilder();
            int i = 0;
            while (i < expansion.length()) {
                if (expansion.charAt(i) == '$' && i + 1 < expansion.length()) {
                    char next = expansion.charAt(i + 1);
                    if (next == '@') {
                        // $@ — all arguments
                        result.append(rest);
                        i += 2;
                    } else if (Character.isDigit(next)) {
                        // $1, $2, etc.
                        int argIdx = Character.digit(next, 10) - 1;
                        if (argIdx >= 0 && argIdx < restArgs.length) {
                            result.append(restArgs[argIdx]);
                        }
                        i += 2;
                    } else {
                        result.append(expansion.charAt(i));
                        i++;
                    }
                } else {
                    result.append(expansion.charAt(i));
                    i++;
                }
            }
            return expand(result.toString(), visited, depth + 1);
        }

        // Simple substitution: replace first word with expansion
        String expanded = rest.isEmpty() ? expansion : expansion + " " + rest;
        return expand(expanded, visited, depth + 1);
    }

    @Override
    public void load() throws IOException {
        if (persistFile == null || !Files.exists(persistFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(persistFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String name = line.substring(0, eq).trim();
                    String expansion = line.substring(eq + 1).trim();
                    aliases.put(name, expansion);
                }
            }
        }
    }

    @Override
    public void save() throws IOException {
        if (persistFile == null) {
            return;
        }
        Files.createDirectories(persistFile.getParent());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(persistFile))) {
            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }
}
