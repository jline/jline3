/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.prompt.*;
import org.jline.reader.Binding;
import org.jline.reader.Candidate;
import org.jline.reader.CompletingParsedLine;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.Display;

import static org.jline.keymap.KeyMap.*;
import static org.jline.utils.InfoCmp.Capability.*;

/**
 * Default implementation of the Prompter interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultPrompter implements Prompter {

    private final Terminal terminal;
    private final LineReader reader;
    private final PrompterConfig config;
    private final Display display;
    private final BindingReader bindingReader;
    private Attributes attributes;
    private List<AttributedString> header = new ArrayList<>();

    // Default timeout for escape sequences
    public static final long DEFAULT_TIMEOUT_WITH_ESC = 150L;

    // Default page size for lists
    private static final int DEFAULT_PAGE_SIZE = 10;

    // Terminal size tracking
    private final Size size = new Size();

    // List range for pagination
    private ListRange range = null;

    // First row where items start (after header and message)
    private int firstItemRow;

    // Marker string to detect Escape key press in readLine
    // Using a Unicode private use area character that's unlikely to be typed by users
    private static final String ESCAPE_MARKER = "\uE000ESCAPE\uE000";

    /**
     * Create a new DefaultPrompter with the given terminal.
     *
     * @param terminal the terminal to use
     */
    public DefaultPrompter(Terminal terminal) {
        this(null, terminal, PrompterConfig.defaults());
    }

    /**
     * Create a new DefaultPrompter with the given terminal and configuration.
     *
     * @param terminal the terminal to use
     * @param config the configuration to use
     */
    public DefaultPrompter(Terminal terminal, PrompterConfig config) {
        this(null, terminal, config);
    }

    /**
     * Create a new DefaultPrompter with the given line reader, terminal, and configuration.
     *
     * @param reader the line reader to use
     * @param terminal the terminal to use
     * @param config the configuration to use
     */
    public DefaultPrompter(LineReader reader, Terminal terminal, PrompterConfig config) {
        this.terminal = terminal;
        this.config = config;
        if (reader == null) {
            reader = LineReaderBuilder.builder().terminal(terminal).build();
        }
        this.reader = reader;
        this.display = new Display(terminal, false); // Don't use full screen mode like console-ui
        this.bindingReader = new BindingReader(terminal.reader());
    }

    // Operation enums for different prompt types
    private enum ListOperation {
        FORWARD_ONE_LINE,
        BACKWARD_ONE_LINE,
        INSERT,
        EXIT,
        CANCEL,
        ESCAPE,
        IGNORE // For unmatched keys (like console-ui behavior)
    }

    private enum CheckboxOperation {
        FORWARD_ONE_LINE,
        BACKWARD_ONE_LINE,
        TOGGLE,
        EXIT,
        CANCEL,
        ESCAPE,
        IGNORE // For unmatched keys (like console-ui behavior)
    }

    private enum ChoiceOperation {
        INSERT,
        EXIT,
        CANCEL,
        ESCAPE
    }

    @Override
    public PromptBuilder newBuilder() {
        return new DefaultPromptBuilder();
    }

    @Override
    public Map<String, ? extends PromptResult<? extends Prompt>> prompt(
            List<AttributedString> header, List<? extends Prompt> prompts) throws IOException, UserInterruptException {
        // Handle empty prompt list directly
        if (prompts == null || prompts.isEmpty()) {
            return new HashMap<>();
        }

        // Simple implementation for static lists that follows ConsolePrompt patterns
        Map<String, PromptResult<? extends Prompt>> resultMap = new HashMap<>();

        try {
            open();

            promptInternal(header, prompts, resultMap, config.cancellableFirstPrompt());

            return removeNoResults(resultMap);
        } finally {
            close();
        }
    }

    @Override
    public Map<String, ? extends PromptResult<? extends Prompt>> prompt(
            List<AttributedString> header,
            Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> promptsProvider)
            throws IOException {

        Map<String, PromptResult<? extends Prompt>> resultMap = new HashMap<>();
        Deque<List<? extends Prompt>> prevLists = new ArrayDeque<>();
        Deque<Map<String, PromptResult<? extends Prompt>>> prevResults = new ArrayDeque<>();
        boolean cancellable = config.cancellableFirstPrompt();

        header = new ArrayList<>(header);
        try {
            open();
            // Get our first list of prompts
            List<? extends Prompt> promptList = promptsProvider.apply(new HashMap<>());
            Map<String, PromptResult<? extends Prompt>> promptResult = new HashMap<>();

            while (promptList != null) {
                // Second and later prompts should always be cancellable
                boolean cancellableFirstPrompt = !prevLists.isEmpty() || cancellable;

                // Prompt the user
                promptInternal(header, promptList, promptResult, cancellableFirstPrompt);

                if (promptResult.isEmpty()) {
                    // The prompt was cancelled by the user, so let's go back to the
                    // previous list of prompts and its results (if any)
                    promptList = prevLists.pollFirst();
                    promptResult = prevResults.pollFirst();
                    if (promptResult != null) {
                        // Remove the results of the previous prompt from the main result map
                        promptResult.forEach((k, v) -> resultMap.remove(k));
                        // Remove the previous result from header - need to handle TextPrompt specially
                        removePreviousResult(promptResult);
                    }
                } else {
                    // We remember the list of prompts and their results
                    prevLists.push(promptList);
                    prevResults.push(promptResult);
                    // Add the results to the main result map
                    resultMap.putAll(promptResult);
                    // And we get our next list of prompts (if any)
                    promptList = promptsProvider.apply(resultMap);
                    promptResult = new HashMap<>();
                }
            }

            return removeNoResults(resultMap);
        } finally {
            close();
        }
    }

    @Override
    public Terminal getTerminal() {
        return terminal;
    }

    /**
     * Internal prompt method that mirrors ConsolePrompt.prompt() logic.
     * Handles header accumulation and backward navigation.
     */
    protected void promptInternal(
            List<AttributedString> headerIn,
            List<? extends Prompt> promptList,
            Map<String, PromptResult<? extends Prompt>> resultMap,
            boolean cancellableFirstPrompt)
            throws IOException {

        if (!terminalInRawMode()) {
            throw new IllegalStateException("Terminal is not in raw mode! Maybe Prompter is closed?");
        }

        // Initialize header from input
        this.header = headerIn;

        boolean backward = false;
        for (int i = resultMap.isEmpty() ? 0 : resultMap.size() - 1; i < promptList.size(); i++) {
            Prompt prompt = promptList.get(i);
            try {
                if (backward) {
                    // Remove the previous result from resultMap
                    removePreviousResult(resultMap);
                    backward = false;
                }

                PromptResult<? extends Prompt> oldResult = resultMap.get(prompt.getName());
                PromptResult<? extends Prompt> result = promptElement(this.header, prompt, oldResult);

                if (result == null) {
                    // Prompt was cancelled by the user
                    if (i > 0) {
                        // Go back to previous prompt
                        i -= 2;
                        backward = true;
                        continue;
                    } else {
                        if (cancellableFirstPrompt) {
                            resultMap.clear();
                            return;
                        } else {
                            // Repeat current prompt
                            i -= 1;
                            continue;
                        }
                    }
                }

                // Add result to header for next prompt (like ConsolePrompt)
                if (prompt instanceof TextPrompt) {
                    // For text prompts, add all lines to header like console-ui Text.getLines()
                    TextPrompt textPrompt = (TextPrompt) prompt;
                    this.header.addAll(textPrompt.getLines());
                } else {
                    String resp = result.getResult();
                    if (result instanceof ConfirmResult) {
                        ConfirmResult cr = (ConfirmResult) result;
                        resp = cr.getConfirmed() == ConfirmResult.ConfirmationValue.YES ? "Yes" : "No";
                    }
                    AttributedStringBuilder message = createMessage(prompt.getMessage(), resp);
                    this.header.add(message.toAttributedString());
                }

                resultMap.put(prompt.getName(), result);
            } catch (UserInterruptException e) {
                throw e;
            } catch (Exception e) {
                // Log error and continue
                terminal.writer().println("Error executing prompt '" + prompt.getName() + "': " + e.getMessage());
                terminal.flush();
            }
        }
    }

    @Override
    public LineReader getLineReader() {
        return reader;
    }

    /**
     * Remove results that have no meaningful value (like ConsolePrompt).
     */
    private Map<String, PromptResult<? extends Prompt>> removeNoResults(
            Map<String, PromptResult<? extends Prompt>> resultMap) {
        Map<String, PromptResult<? extends Prompt>> filtered = new HashMap<>();
        for (Map.Entry<String, PromptResult<? extends Prompt>> entry : resultMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getResult() != null) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    /**
     * Remove previous result when going backward.
     */
    private void removePreviousResult(Map<String, PromptResult<? extends Prompt>> promptResult) {
        // Find the prompt that was executed to determine how many lines to remove
        for (PromptResult<? extends Prompt> result : promptResult.values()) {
            Prompt prompt = result.getPrompt();
            if (prompt instanceof TextPrompt) {
                // For text prompts, remove all lines that were added
                TextPrompt textPrompt = (TextPrompt) prompt;
                int linesToRemove = textPrompt.getLines().size();
                for (int i = 0; i < linesToRemove && !this.header.isEmpty(); i++) {
                    this.header.remove(this.header.size() - 1);
                }
            } else {
                // For other prompts, remove just one line
                if (!this.header.isEmpty()) {
                    this.header.remove(this.header.size() - 1);
                }
            }
            break; // Only process the first result since we're going back one step
        }
    }

    /**
     * Create a message with prompt and response (like ConsolePrompt).
     */
    private AttributedStringBuilder createMessage(String message, String response) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(config.style(PrompterConfig.PR)).append("? ");
        asb.style(config.style(PrompterConfig.ME)).append(message).append(" ");
        if (response != null) {
            asb.style(config.style(PrompterConfig.AN)).append(response);
        }
        return asb;
    }

    /**
     * Execute a single prompt element (like ConsolePrompt.promptElement).
     */
    protected PromptResult<? extends Prompt> promptElement(
            List<AttributedString> header, Prompt prompt, PromptResult<? extends Prompt> oldResult)
            throws UserInterruptException {
        try {
            // Header is managed by individual prompt methods
            return executePrompt(header, prompt);
        } catch (UserInterruptException e) {
            // Propagate Ctrl+C to exit the whole demo
            throw e;
        } catch (Exception e) {
            terminal.writer().println("Error: " + e.getMessage());
            terminal.flush();
            return null;
        }
    }

    /**
     * Execute a single prompt and return its result.
     */
    @SuppressWarnings("unchecked")
    private PromptResult<? extends Prompt> executePrompt(List<AttributedString> header, Prompt prompt)
            throws IOException, UserInterruptException {

        if (prompt instanceof PasswordPrompt) {
            return executePasswordPrompt(header, (PasswordPrompt) prompt);
        } else if (prompt instanceof NumberPrompt) {
            return executeNumberPrompt(header, (NumberPrompt) prompt);
        } else if (prompt instanceof SearchPrompt) {
            return executeSearchPrompt(header, (SearchPrompt<?>) prompt);
        } else if (prompt instanceof EditorPrompt) {
            return executeEditorPrompt(header, (EditorPrompt) prompt);
        } else if (prompt instanceof InputPrompt) {
            return executeInputPrompt(header, (InputPrompt) prompt);
        } else if (prompt instanceof ListPrompt) {
            return executeListPrompt(header, (ListPrompt) prompt);
        } else if (prompt instanceof CheckboxPrompt) {
            return executeCheckboxPrompt(header, (CheckboxPrompt) prompt);
        } else if (prompt instanceof ChoicePrompt) {
            return executeChoicePrompt(header, (ChoicePrompt) prompt);
        } else if (prompt instanceof ConfirmPrompt) {
            return executeConfirmPrompt(header, (ConfirmPrompt) prompt);
        } else if (prompt instanceof TextPrompt) {
            return executeTextPrompt(header, (TextPrompt) prompt);
        } else {
            throw new IllegalArgumentException("Unknown prompt type: " + prompt.getClass());
        }
    }

    private InputResult executeInputPrompt(List<AttributedString> header, InputPrompt prompt)
            throws IOException, UserInterruptException {

        // Display header using LineReader's printAbove method
        if (header != null && !header.isEmpty()) {
            for (AttributedString line : header) {
                reader.printAbove(line);
            }
        }

        // Create prompt message using proper styling
        AttributedStringBuilder asb = createMessage(prompt.getMessage(), null);
        String defaultValue = prompt.getDefaultValue();
        if (defaultValue != null) {
            asb.append("(").append(defaultValue).append(") ");
        }
        String promptString = asb.toAttributedString().toAnsi();

        // Save original Escape binding and set up escape handling
        KeyMap<Binding> mainKeyMap = reader.getKeyMaps().get(LineReader.MAIN);
        Binding originalEscapeBinding = mainKeyMap.getBound("\u001b");

        // Create escape widget that sets marker and accepts line
        Widget escapeWidget = () -> {
            try {
                reader.getBuffer().clear();
                reader.getBuffer().write(ESCAPE_MARKER, false);
                reader.callWidget(LineReader.ACCEPT_LINE);
                return true;
            } catch (Exception e) {
                return false;
            }
        };
        reader.getWidgets().put("prompter-escape", escapeWidget);
        mainKeyMap.bind(new Reference("prompter-escape"), "\u001b");

        try {
            // Use LineReader to read the input
            String input;
            Character mask = prompt.getMask();
            String buffer = defaultValue != null ? defaultValue : null;

            input = reader.readLine(promptString, null, mask, buffer);

            // Check if Escape was pressed (marked by escape marker)
            if (ESCAPE_MARKER.equals(input)) {
                return null; // Go back to previous prompt
            }

            // Handle empty input with default value
            if (input.trim().isEmpty() && defaultValue != null) {
                input = defaultValue;
            }

            return new DefaultInputResult(input, input, prompt);
        } catch (UserInterruptException e) {
            // Ctrl+C was pressed
            throw e;
        } finally {
            // Restore original Escape binding
            if (originalEscapeBinding != null) {
                mainKeyMap.bind(originalEscapeBinding, "\u001b");
            } else {
                // Remove our binding if there was no original
                mainKeyMap.unbind("\u001b");
            }
            // Remove the escape widget
            reader.getWidgets().remove("prompter-escape");
        }
    }

    private InputResult executePasswordPrompt(List<AttributedString> header, PasswordPrompt prompt)
            throws IOException, UserInterruptException {
        // Password prompts are just input prompts with masking
        return executeInputPrompt(header, prompt);
    }

    private InputResult executeNumberPrompt(List<AttributedString> header, NumberPrompt prompt)
            throws IOException, UserInterruptException {

        String errorMessage = null;

        while (true) {
            // Build header with error message if present
            List<AttributedString> currentHeader = new ArrayList<>(header);
            if (errorMessage != null) {
                // Add error message with ">> " prefix like Inquirer.js using style resolver
                AttributedString errorLine = new AttributedStringBuilder()
                        .style(config.style(PrompterConfig.ERROR))
                        .append(">> ")
                        .append(errorMessage)
                        .toAttributedString();
                currentHeader.add(errorLine);
            }

            // Execute as regular input prompt with updated header
            InputResult result = executeInputPrompt(currentHeader, prompt);
            if (result == null) {
                return null; // User cancelled
            }

            String input = result.getInput();
            if (input.trim().isEmpty() && prompt.getDefaultValue() != null) {
                input = prompt.getDefaultValue();
            }

            // Validate the number
            try {
                double value = Double.parseDouble(input);

                // Check if decimals are allowed
                if (!prompt.allowDecimals() && input.contains(".")) {
                    errorMessage = "Please enter a whole number";
                    continue;
                }

                // Check range
                Double min = prompt.getMin();
                Double max = prompt.getMax();
                if ((min != null && value < min) || (max != null && value > max)) {
                    errorMessage = prompt.getOutOfRangeMessage();
                    continue;
                }

                return new DefaultInputResult(input, input, prompt);

            } catch (NumberFormatException e) {
                errorMessage = prompt.getInvalidNumberMessage();
                // Continue the loop to ask again
            }
        }
    }

    private PromptResult<SearchPrompt<?>> executeSearchPrompt(List<AttributedString> header, SearchPrompt<?> prompt)
            throws IOException, UserInterruptException {

        // Create a dynamic input prompt that searches as the user types
        StringBuilder searchTerm = new StringBuilder();
        List<?> currentResults = new ArrayList<>();
        int selectedIndex = 0;

        // Create prompt message
        AttributedStringBuilder asb = createMessage(prompt.getMessage(), null);
        int startColumn = asb.columnLength();

        size.copy(terminal.getSize());
        KeyMap<InputOperation> keyMap = new KeyMap<>();
        bindInputKeys(keyMap);

        while (true) {
            // Perform search if term is long enough
            if (searchTerm.length() >= prompt.getMinSearchLength()) {
                currentResults = prompt.getSearchFunction().apply(searchTerm.toString());
                if (prompt.getMaxResults() > 0 && currentResults.size() > prompt.getMaxResults()) {
                    currentResults = currentResults.subList(0, prompt.getMaxResults());
                }
            } else {
                currentResults = new ArrayList<>();
            }

            // Ensure selected index is valid
            if (selectedIndex >= currentResults.size()) {
                selectedIndex = Math.max(0, currentResults.size() - 1);
            }

            // Build display
            List<AttributedString> out = buildSearchDisplay(
                    header, asb, searchTerm.toString(), currentResults, selectedIndex, prompt, startColumn);

            display.resize(size.getRows(), size.getColumns());
            int cursorRow = out.size() - 1;
            int column = startColumn + searchTerm.length();
            display.update(out, size.cursorPos(cursorRow, column));

            InputOperation op = bindingReader.readBinding(keyMap);
            switch (op) {
                case INSERT:
                    String ch = bindingReader.getLastBinding();
                    searchTerm.append(ch);
                    selectedIndex = 0; // Reset selection when typing
                    break;

                case BACKSPACE:
                    if (searchTerm.length() > 0) {
                        searchTerm.deleteCharAt(searchTerm.length() - 1);
                        selectedIndex = 0;
                    }
                    break;

                case DOWN:
                    if (!currentResults.isEmpty() && selectedIndex < currentResults.size() - 1) {
                        selectedIndex++;
                    }
                    break;

                case UP:
                    if (selectedIndex > 0) {
                        selectedIndex--;
                    }
                    break;

                case EXIT:
                    if (!currentResults.isEmpty() && selectedIndex < currentResults.size()) {
                        Object selected = currentResults.get(selectedIndex);
                        @SuppressWarnings("unchecked")
                        String value = ((Function<Object, String>) prompt.getValueFunction()).apply(selected);
                        return new AbstractPromptResult<SearchPrompt<?>>(prompt) {
                            @Override
                            public String getResult() {
                                return value;
                            }
                        };
                    }
                    break;

                case ESCAPE:
                    return null;

                case CANCEL:
                    throw new UserInterruptException("User cancelled");
            }
        }
    }

    private PromptResult<EditorPrompt> executeEditorPrompt(List<AttributedString> header, EditorPrompt prompt)
            throws IOException, UserInterruptException {

        // Display the prompt message
        List<AttributedString> displayLines = new ArrayList<>();
        if (header != null) {
            displayLines.addAll(header);
        }

        AttributedStringBuilder asb = createMessage(prompt.getMessage(), null);
        asb.append("Press Enter to open editor, Escape to cancel");
        displayLines.add(asb.toAttributedString());

        size.copy(terminal.getSize());
        display.resize(size.getRows(), size.getColumns());
        display.update(displayLines, -1);

        // Wait for user input
        KeyMap<InputOperation> keyMap = new KeyMap<>();
        keyMap.bind(InputOperation.EXIT, "\r", "\n");
        keyMap.bind(InputOperation.ESCAPE, "\u001b");
        keyMap.bind(InputOperation.CANCEL, "\u0003");

        InputOperation op = bindingReader.readBinding(keyMap);
        switch (op) {
            case EXIT:
                // Launch editor
                try {
                    String result = launchEditor(prompt);
                    return new AbstractPromptResult<EditorPrompt>(prompt) {
                        @Override
                        public String getResult() {
                            return result;
                        }
                    };
                } catch (Exception e) {
                    terminal.writer().println("Error launching editor: " + e.getMessage());
                    terminal.flush();
                    return null;
                }

            case ESCAPE:
                return null;

            case CANCEL:
                throw new UserInterruptException("User cancelled");

            default:
                return null;
        }
    }

    private String launchEditor(EditorPrompt prompt) throws IOException, InterruptedException {
        try {
            // Create temporary file
            File tempFile = File.createTempFile("jline_editor_", "." + prompt.getFileExtension());
            tempFile.deleteOnExit();

            // Write initial content if provided
            String initialText = prompt.getInitialText();
            if (initialText != null) {
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(initialText);
                }
            }

            // Use JLine's built-in Nano editor
            Class<?> nanoClass = Class.forName("org.jline.builtins.Nano");
            Constructor<?> constructor = nanoClass.getConstructor(Terminal.class, Path.class);

            Object nano =
                    constructor.newInstance(terminal, tempFile.getParentFile().toPath());

            // Configure nano options
            if (prompt.getTitle() != null) {
                Field titleField = nanoClass.getField("title");
                titleField.set(nano, prompt.getTitle());
            }

            Field lineNumbersField = nanoClass.getField("printLineNumbers");
            lineNumbersField.setBoolean(nano, prompt.showLineNumbers());

            Field wrappingField = nanoClass.getField("wrapping");
            wrappingField.setBoolean(nano, prompt.enableWrapping());

            // Get methods
            Method openMethod = nanoClass.getMethod("open", List.class);
            Method runMethod = nanoClass.getMethod("run");

            // Open the file and run the editor
            openMethod.invoke(nano, Collections.singletonList(tempFile.getName()));
            runMethod.invoke(nano);

            // Reset terminal state after editor
            terminal.flush();

            // Read the result
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (result.length() > 0) {
                        result.append("\n");
                    }
                    result.append(line);
                }
            }

            return result.toString();

        } catch (ClassNotFoundException e) {
            throw new IOException("JLine Nano editor not available. Please add jline-builtins to your classpath.", e);
        } catch (Exception e) {
            throw new IOException("Error launching JLine Nano editor: " + e.getMessage(), e);
        }
    }

    /**
     * Build search display with results.
     */
    private List<AttributedString> buildSearchDisplay(
            List<AttributedString> header,
            AttributedStringBuilder messageBuilder,
            String searchTerm,
            List<?> results,
            int selectedIndex,
            SearchPrompt<?> prompt,
            int startColumn) {

        List<AttributedString> out = new ArrayList<>();
        if (header != null) {
            out.addAll(header);
        }

        // Add search input line
        AttributedStringBuilder searchLine = new AttributedStringBuilder();
        searchLine.append(messageBuilder);
        if (searchTerm.isEmpty()) {
            searchLine.style(config.style(".me")).append(prompt.getPlaceholder());
        } else {
            searchLine.append(searchTerm);
        }
        out.add(searchLine.toAttributedString());

        // Add results
        if (results.isEmpty() && searchTerm.length() >= prompt.getMinSearchLength()) {
            out.add(new AttributedString("No results found"));
        } else {
            for (int i = 0; i < results.size(); i++) {
                Object item = results.get(i);
                @SuppressWarnings("unchecked")
                String display = ((Function<Object, String>) prompt.getDisplayFunction()).apply(item);

                AttributedStringBuilder itemLine = new AttributedStringBuilder();
                if (i == selectedIndex) {
                    itemLine.style(config.style(".se")).append("❯ ").append(display);
                } else {
                    itemLine.append("  ").append(display);
                }
                out.add(itemLine.toAttributedString());
            }
        }

        return out;
    }

    /**
     * Build input display with completion candidates like AbstractPrompt.
     */
    private List<AttributedString> buildInputDisplay(
            List<AttributedString> header,
            AttributedStringBuilder messageBuilder,
            StringBuilder buffer,
            StringBuilder displayBuffer,
            Character mask,
            List<Candidate> candidates,
            boolean displayCandidates,
            int startColumn) {

        List<AttributedString> out = new ArrayList<>();
        if (header != null) {
            out.addAll(header);
        }

        // Create message line with current input buffer
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(messageBuilder);
        if (mask != null) {
            asb.append(displayBuffer.toString());
        } else {
            asb.append(buffer.toString());
        }
        out.add(asb.toAttributedString());

        // Add completion candidates if available
        if (displayCandidates && !candidates.isEmpty()) {
            out.addAll(buildCandidatesDisplay(candidates, startColumn + asb.columnLength()));
        }

        return out;
    }

    /**
     * Build candidates display like AbstractPrompt.
     */
    private List<AttributedString> buildCandidatesDisplay(List<Candidate> candidates, int listStart) {
        List<AttributedString> out = new ArrayList<>();

        int width = Math.max(
                candidates.stream()
                        .map(Candidate::displ)
                        .mapToInt(display::wcwidth)
                        .max()
                        .orElse(20),
                20);

        for (Candidate c : candidates) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            AttributedStringBuilder tmp = new AttributedStringBuilder();
            tmp.ansiAppend(c.displ());
            asb.style(tmp.styleAt(0));
            asb.append(AttributedString.stripAnsi(c.displ()));
            int cl = asb.columnLength();
            for (int k = cl; k < width; k++) {
                asb.append(" ");
            }
            AttributedStringBuilder asb2 = new AttributedStringBuilder();
            asb2.tabs(listStart);
            asb2.append("\t");
            asb2.style(config.style(".cb"));
            asb2.append(asb).append(" ");
            out.add(asb2.toAttributedString());
        }
        return out;
    }

    /**
     * Select candidate like AbstractPrompt.
     */
    private String selectCandidate(String buffer, List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return buffer;
        } else if (candidates.size() == 1) {
            return candidates.get(0).value();
        }
        // For now, just return the first candidate
        // TODO: Implement interactive candidate selection
        return candidates.get(0).value();
    }

    /**
     * CompletingWord implementation like AbstractPrompt.
     */
    private static class CompletingWord implements CompletingParsedLine {
        private final String word;

        public CompletingWord(String word) {
            this.word = word;
        }

        @Override
        public CharSequence escape(CharSequence candidate, boolean complete) {
            return null;
        }

        @Override
        public int rawWordCursor() {
            return word.length();
        }

        @Override
        public int rawWordLength() {
            return word.length();
        }

        @Override
        public String word() {
            return word;
        }

        @Override
        public int wordCursor() {
            return word.length();
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
            return word.length();
        }
    }

    /**
     * Input operations for direct character input (copied from ConsolePrompt).
     */
    private enum InputOperation {
        INSERT,
        BACKSPACE,
        DELETE,
        RIGHT,
        LEFT,
        UP,
        DOWN,
        BEGINNING_OF_LINE,
        END_OF_LINE,
        SELECT_CANDIDATE,
        EXIT,
        CANCEL,
        ESCAPE
    }

    /**
     * Confirm operations for direct character input (copied from ConsolePrompt).
     */
    private enum ConfirmOperation {
        YES,
        NO,
        EXIT,
        CANCEL,
        ESCAPE
    }

    /**
     * Bind keys for input operations (copied from ConsolePrompt).
     */
    private void bindInputKeys(KeyMap<InputOperation> keyMap) {
        // Bind printable characters to INSERT
        keyMap.setUnicode(InputOperation.INSERT);
        for (char i = 32; i < 127; i++) {
            keyMap.bind(InputOperation.INSERT, Character.toString(i));
        }

        // Bind special keys like ConsolePrompt
        keyMap.bind(InputOperation.BACKSPACE, "\b", "\u007f");
        keyMap.bind(InputOperation.DELETE, "\u001b[3~");
        keyMap.bind(InputOperation.EXIT, "\r", "\n");
        keyMap.bind(InputOperation.CANCEL, "\u0003"); // Ctrl+C
        keyMap.bind(InputOperation.ESCAPE, "\u001b"); // Escape key
        keyMap.bind(InputOperation.LEFT, "\u001b[D");
        keyMap.bind(InputOperation.RIGHT, "\u001b[C");
        keyMap.bind(InputOperation.UP, "\u001b[A");
        keyMap.bind(InputOperation.DOWN, "\u001b[B");
        keyMap.bind(InputOperation.BEGINNING_OF_LINE, "\u0001"); // Ctrl+A
        keyMap.bind(InputOperation.END_OF_LINE, "\u0005"); // Ctrl+E
        keyMap.bind(InputOperation.SELECT_CANDIDATE, "\t"); // Tab for completion
    }

    /**
     * Bind keys for confirm operations (copied from ConsolePrompt).
     */
    private void bindConfirmKeys(KeyMap<ConfirmOperation> keyMap) {
        keyMap.bind(ConfirmOperation.YES, "y", "Y");
        keyMap.bind(ConfirmOperation.NO, "n", "N");
        keyMap.bind(ConfirmOperation.EXIT, "\r", "\n");
        keyMap.bind(ConfirmOperation.CANCEL, "\u0003"); // Ctrl+C
        keyMap.bind(ConfirmOperation.ESCAPE, "\u001b"); // Escape key
    }

    private ListResult executeListPrompt(List<AttributedString> header, ListPrompt prompt)
            throws IOException, UserInterruptException {

        List<ListItem> items = prompt.getItems();
        if (items.isEmpty()) {
            return new DefaultListResult("", prompt);
        }

        // Initialize display
        resetDisplay();
        firstItemRow = (header != null ? header.size() : 0) + 1; // Header + message line
        range = null;

        // Find first selectable item
        int selectRow = nextRow(firstItemRow - 1, firstItemRow, items);

        // Set up key bindings
        KeyMap<ListOperation> keyMap = new KeyMap<>();
        bindListKeys(keyMap);

        // Interactive selection loop
        while (true) {
            // Update display with current selection
            refreshListDisplay(header, prompt.getMessage(), items, selectRow, prompt);

            // Read user input using BindingReader
            ListOperation op = bindingReader.readBinding(keyMap);
            switch (op) {
                case FORWARD_ONE_LINE:
                    selectRow = nextRow(selectRow, firstItemRow, items);
                    break;
                case BACKWARD_ONE_LINE:
                    selectRow = prevRow(selectRow, firstItemRow, items);
                    break;

                case INSERT:
                    // Handle character-based selection (if items have keys)
                    String ch = bindingReader.getLastBinding();
                    int id = 0;
                    for (ListItem item : items) {
                        if (item instanceof ChoiceItem) {
                            ChoiceItem choiceItem = (ChoiceItem) item;
                            if (!choiceItem.isDisabled()
                                    && choiceItem.getKey() != null
                                    && choiceItem.getKey().toString().equals(ch)) {
                                selectRow = firstItemRow + id;
                                break;
                            }
                        }
                        id++;
                    }
                    break;
                case EXIT:
                    ListItem selectedItem = items.get(selectRow - firstItemRow);
                    return new DefaultListResult(selectedItem.getName(), prompt);
                case ESCAPE:
                    return null; // Go back to previous prompt
                case CANCEL:
                    throw new UserInterruptException("User cancelled");
                case IGNORE:
                    // Ignore unmatched keys (like console-ui behavior)
                    break;
            }
        }
    }

    private CheckboxResult executeCheckboxPrompt(List<AttributedString> header, CheckboxPrompt prompt)
            throws IOException, UserInterruptException {

        List<CheckboxItem> items = prompt.getItems();
        Set<String> selectedIds = new HashSet<>();

        // Initialize with initially checked items
        for (CheckboxItem item : items) {
            if (item.isInitiallyChecked()) {
                selectedIds.add(item.getName());
            }
        }

        if (items.isEmpty()) {
            return new DefaultCheckboxResult(selectedIds, prompt);
        }

        // Initialize display
        resetDisplay();
        firstItemRow = (header != null ? header.size() : 0) + 1; // Header + message line
        range = null;

        // Find first selectable item
        int selectRow = nextRow(firstItemRow - 1, firstItemRow, items);

        // Set up key bindings
        KeyMap<CheckboxOperation> keyMap = new KeyMap<>();
        bindCheckboxKeys(keyMap);

        // Interactive selection loop
        while (true) {
            // Update display with current selection and checkbox states
            refreshCheckboxDisplay(header, prompt.getMessage(), items, selectRow, selectedIds, prompt);

            // Read user input using BindingReader
            CheckboxOperation op = bindingReader.readBinding(keyMap);
            switch (op) {
                case FORWARD_ONE_LINE:
                    selectRow = nextRow(selectRow, firstItemRow, items);
                    break;
                case BACKWARD_ONE_LINE:
                    selectRow = prevRow(selectRow, firstItemRow, items);
                    break;

                case TOGGLE:
                    CheckboxItem currentItem = items.get(selectRow - firstItemRow);
                    if (!currentItem.isDisabled()) {
                        if (selectedIds.contains(currentItem.getName())) {
                            selectedIds.remove(currentItem.getName());
                        } else {
                            selectedIds.add(currentItem.getName());
                        }
                    }
                    break;
                case EXIT:
                    return new DefaultCheckboxResult(selectedIds, prompt);
                case ESCAPE:
                    return null; // Go back to previous prompt
                case CANCEL:
                    throw new UserInterruptException("User cancelled");
                case IGNORE:
                    // Ignore unmatched keys (like console-ui behavior)
                    break;
            }
        }
    }

    private ChoiceResult executeChoicePrompt(List<AttributedString> header, ChoicePrompt prompt)
            throws IOException, UserInterruptException {

        size.copy(terminal.getSize());
        display.resize(size.getRows(), size.getColumns());

        List<ChoiceItem> items = prompt.getItems();
        if (items.isEmpty()) {
            return new DefaultChoiceResult("", prompt);
        }

        // Find default choice if any
        ChoiceItem defaultChoice = null;
        for (ChoiceItem item : items) {
            if (item.isDefaultChoice() && !item.isDisabled()) {
                defaultChoice = item;
                break;
            }
        }

        // Build initial display with header, message, choices, and prompt
        List<AttributedString> out = new ArrayList<>();
        if (header != null) {
            out.addAll(header);
        }

        // Add message line
        AttributedStringBuilder messageBuilder = createMessage(prompt.getMessage(), null);
        out.add(messageBuilder.toAttributedString());

        // Add choice items
        out.addAll(buildChoiceItemsDisplay(items));

        // Add choice prompt line
        AttributedStringBuilder choiceBuilder = new AttributedStringBuilder();
        choiceBuilder.styled(config.style(PrompterConfig.PR), "Choice: ");
        out.add(choiceBuilder.toAttributedString());

        display.update(out, out.size() - 1);

        // Set up key bindings
        KeyMap<ChoiceOperation> keyMap = new KeyMap<>();
        bindChoiceKeys(keyMap);

        // Interactive selection loop
        while (true) {
            ChoiceOperation op = bindingReader.readBinding(keyMap);
            switch (op) {
                case INSERT:
                    // Check if the input character matches any choice key
                    String ch = bindingReader.getLastBinding();
                    for (ChoiceItem item : items) {
                        if (!item.isDisabled()
                                && item.getKey() != null
                                && item.getKey().toString().equalsIgnoreCase(ch)) {
                            // Found matching choice - update display with answer
                            updateChoiceDisplay(out, ch);
                            return new DefaultChoiceResult(item.getName(), prompt);
                        }
                    }
                    // Invalid choice, continue waiting
                    break;
                case EXIT:
                    // Use default choice if available
                    if (defaultChoice != null) {
                        String defaultKey = defaultChoice.getKey() != null
                                ? defaultChoice.getKey().toString()
                                : "";
                        updateChoiceDisplay(out, defaultKey);
                        return new DefaultChoiceResult(defaultChoice.getName(), prompt);
                    }
                    // No default, continue waiting for input
                    break;
                case ESCAPE:
                    return null; // Go back to previous prompt
                case CANCEL:
                    throw new UserInterruptException("User cancelled");
            }
        }
    }

    /**
     * Update choice display with the selected answer.
     */
    private void updateChoiceDisplay(List<AttributedString> out, String answer) {
        // Update the last line (choice prompt) with the answer
        AttributedStringBuilder choiceBuilder = new AttributedStringBuilder();
        choiceBuilder.styled(config.style(PrompterConfig.PR), "Choice: ");
        choiceBuilder.styled(config.style(PrompterConfig.AN), answer);

        // Replace the last line with the updated one
        out.set(out.size() - 1, choiceBuilder.toAttributedString());
        display.update(out, -1);
    }

    private ConfirmResult executeConfirmPrompt(List<AttributedString> header, ConfirmPrompt prompt)
            throws IOException, UserInterruptException {

        // Copy ConsolePrompt's exact behavior for confirm prompts
        size.copy(terminal.getSize());

        // Set up key bindings like ConsolePrompt
        KeyMap<ConfirmOperation> keyMap = new KeyMap<>();
        bindConfirmKeys(keyMap);

        // Create prompt message using proper styling like ConsolePrompt
        AttributedStringBuilder asb = createMessage(prompt.getMessage(), null);
        asb.append("(y/N) ");

        ConfirmResult.ConfirmationValue confirm = ConfirmResult.ConfirmationValue.NO; // Default
        StringBuilder buffer = new StringBuilder();

        while (true) {
            // Build display lines exactly like ConsolePrompt: header + message + buffer
            List<AttributedString> out = new ArrayList<>();
            if (header != null) {
                out.addAll(header);
            }

            // Create message line with current input buffer
            AttributedStringBuilder messageBuilder = new AttributedStringBuilder();
            messageBuilder.append(asb);
            messageBuilder.append(buffer.toString());
            out.add(messageBuilder.toAttributedString());

            // Update display exactly like ConsolePrompt
            display.resize(size.getRows(), size.getColumns());
            int cursorRow = out.size() - 1;
            int column = asb.columnLength() + buffer.length();
            display.update(out, size.cursorPos(cursorRow, column));

            // Read input like ConsolePrompt
            ConfirmOperation op = bindingReader.readBinding(keyMap);
            switch (op) {
                case YES:
                    buffer = new StringBuilder("y");
                    confirm = ConfirmResult.ConfirmationValue.YES;
                    break;

                case NO:
                    buffer = new StringBuilder("n");
                    confirm = ConfirmResult.ConfirmationValue.NO;
                    break;

                case EXIT:
                    return new DefaultConfirmResult(confirm, prompt);

                case ESCAPE:
                    return null; // Go back to previous prompt

                case CANCEL:
                    throw new UserInterruptException("User cancelled");
            }
        }
    }

    private PromptResult<TextPrompt> executeTextPrompt(List<AttributedString> header, TextPrompt prompt)
            throws IOException, UserInterruptException {

        // Build display lines including header + text lines
        List<AttributedString> displayLines = new ArrayList<>();
        if (header != null) {
            displayLines.addAll(header);
        }

        // Add text content lines like console-ui Text.getLines()
        displayLines.addAll(prompt.getLines());

        // Update size and display using Display system
        size.copy(terminal.getSize());
        display.resize(size.getRows(), size.getColumns());
        display.update(displayLines, -1);

        // Text prompts don't require user input, just display
        return new AbstractPromptResult<TextPrompt>(prompt) {
            @Override
            public String getResult() {
                return "TEXT_DISPLAYED";
            }
        };
    }

    private void displayPromptMessage(String message) {
        terminal.writer().print("? " + message + " ");
        terminal.flush();
    }

    private void displayText(String text) {
        terminal.writer().println(text);
        terminal.flush();
    }

    private void displayError(String error) {
        terminal.writer().println("Error: " + error);
        terminal.flush();
    }

    private void open() throws IOException {
        if (!terminalInRawMode()) {
            attributes = terminal.enterRawMode();
            terminal.puts(keypad_xmit);
            terminal.writer().flush();
        }
    }

    private void close() throws IOException {
        if (terminalInRawMode()) {
            // Update display with final header state
            int cursor = (terminal.getWidth() + 1) * header.size();
            display.update(header, cursor);
            terminal.setAttributes(attributes);
            terminal.puts(keypad_local);
            terminal.writer().println();
            terminal.writer().flush();
            attributes = null;
        }
    }

    private boolean terminalInRawMode() {
        return attributes != null;
    }

    /**
     * Bind keys for list prompt operations.
     */
    private void bindListKeys(KeyMap<ListOperation> map) {
        // Bind printable characters to INSERT operation
        for (char i = 32; i < KEYMAP_LENGTH; i++) {
            map.bind(ListOperation.INSERT, Character.toString(i));
        }
        // Bind navigation keys
        map.bind(ListOperation.FORWARD_ONE_LINE, "e", ctrl('E'), key(terminal, key_down));
        map.bind(ListOperation.BACKWARD_ONE_LINE, "y", ctrl('Y'), key(terminal, key_up));

        // Bind action keys
        map.bind(ListOperation.EXIT, "\r");
        map.bind(ListOperation.ESCAPE, esc()); // Escape goes back to previous prompt
        map.bind(ListOperation.CANCEL, ctrl('C')); // Ctrl+C cancels

        // Set up fallback for unmatched keys (like console-ui behavior)
        map.setNomatch(ListOperation.IGNORE);
        map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
    }

    /**
     * Refresh the display for list prompts using JLine's Display class.
     */
    private void refreshListDisplay(
            List<AttributedString> header, String message, List<ListItem> items, int cursorRow, ListPrompt prompt) {
        size.copy(terminal.getSize());
        display.resize(size.getRows(), size.getColumns());
        display.update(
                buildListDisplayLines(header, message, items, cursorRow, prompt),
                size.cursorPos(Math.min(size.getRows() - 1, firstItemRow + items.size()), 0));
    }

    /**
     * Build display lines for list prompts with column layout support.
     */
    private List<AttributedString> buildListDisplayLines(
            List<AttributedString> header, String message, List<ListItem> items, int cursorRow, ListPrompt prompt) {
        List<AttributedString> out = new ArrayList<>(header != null ? header : new ArrayList<>());

        // Add message line
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        out.add(asb.toAttributedString());

        // Single column layout with pagination
        computeListRange(cursorRow, items.size(), prompt.getPageSize());
        for (int i = range.first; i < range.last; i++) {
            if (items.isEmpty() || i > items.size() - 1) {
                break;
            }
            out.add(buildSingleItemLine(items.get(i), i + firstItemRow == cursorRow));
        }

        return out;
    }

    /**
     * Build a single item line for display.
     */
    private AttributedString buildSingleItemLine(ListItem item, boolean isSelected) {
        AttributedStringBuilder asb = new AttributedStringBuilder();

        // Check if this is a separator
        if (item instanceof SeparatorItem) {
            // Separators don't get indicators, just display their text
            asb.append(item.getText());
            return asb.toAttributedString();
        }

        // Add selection indicator and key if available
        String key = item instanceof ChoiceItem ? ((ChoiceItem) item).getKey() + " - " : "";
        if (isSelected && item.isSelectable()) {
            asb.styled(config.style(PrompterConfig.CURSOR), config.indicator())
                    .style(config.style(PrompterConfig.SE))
                    .append(" ")
                    .append(key)
                    .append(item.getText());
        } else if (!item.isDisabled() && item.isSelectable()) {
            fillIndicatorSpace(asb);
            asb.append(" ").append(key).append(item.getText());
        } else {
            // Disabled item - use proper styling
            fillIndicatorSpace(asb);
            asb.append(" ").append(key);
            if (item.isDisabled()) {
                asb.styled(config.style(PrompterConfig.BD), item.getDisabledText())
                        .append(" (")
                        .styled(config.style(PrompterConfig.BD), item.getDisabledText())
                        .append(")");
            } else {
                asb.styled(config.style(PrompterConfig.BD), item.getText());
            }
        }

        return asb.toAttributedString();
    }

    /**
     * Fill space for indicator alignment.
     */
    private AttributedStringBuilder fillIndicatorSpace(AttributedStringBuilder asb) {
        for (int i = 0; i < display.wcwidth(config.indicator()); i++) {
            asb.append(" ");
        }
        return asb;
    }

    /**
     * Fill space for checkbox alignment.
     */
    private AttributedStringBuilder fillCheckboxSpace(AttributedStringBuilder asb) {
        for (int i = 0; i < display.wcwidth(config.checkedBox()); i++) {
            asb.append(" ");
        }
        return asb;
    }

    /**
     * Bind keys for checkbox prompt operations.
     */
    private void bindCheckboxKeys(KeyMap<CheckboxOperation> map) {
        // Bind navigation keys
        map.bind(CheckboxOperation.FORWARD_ONE_LINE, "e", ctrl('E'), key(terminal, key_down));
        map.bind(CheckboxOperation.BACKWARD_ONE_LINE, "y", ctrl('Y'), key(terminal, key_up));

        // Bind toggle key
        map.bind(CheckboxOperation.TOGGLE, " ");
        // Bind action keys
        map.bind(CheckboxOperation.EXIT, "\r");
        map.bind(CheckboxOperation.ESCAPE, esc()); // Escape goes back to previous prompt
        map.bind(CheckboxOperation.CANCEL, ctrl('C')); // Ctrl+C cancels

        // Set up fallback for unmatched keys (like console-ui behavior)
        map.setNomatch(CheckboxOperation.IGNORE);
        map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
    }

    /**
     * Refresh the display for checkbox prompts using JLine's Display class.
     */
    private void refreshCheckboxDisplay(
            List<AttributedString> header,
            String message,
            List<CheckboxItem> items,
            int cursorRow,
            Set<String> selectedIds,
            CheckboxPrompt prompt) {
        size.copy(terminal.getSize());
        display.resize(size.getRows(), size.getColumns());
        display.update(
                buildCheckboxDisplayLines(header, message, items, cursorRow, selectedIds, prompt),
                size.cursorPos(Math.min(size.getRows() - 1, firstItemRow + items.size()), 0));
    }

    /**
     * Build display lines for checkbox prompts with column layout support.
     */
    private List<AttributedString> buildCheckboxDisplayLines(
            List<AttributedString> header,
            String message,
            List<CheckboxItem> items,
            int cursorRow,
            Set<String> selectedIds,
            CheckboxPrompt prompt) {
        List<AttributedString> out = new ArrayList<>(header != null ? header : new ArrayList<>());

        // Add message line
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        out.add(asb.toAttributedString());

        // Single column layout with pagination
        computeListRange(cursorRow, items.size(), prompt.getPageSize());
        for (int i = range.first; i < range.last; i++) {
            if (items.isEmpty() || i > items.size() - 1) {
                break;
            }
            out.add(buildSingleCheckboxLine(items.get(i), i + firstItemRow == cursorRow, selectedIds));
        }

        return out;
    }

    /**
     * Build a single checkbox item line for display.
     */
    private AttributedString buildSingleCheckboxLine(CheckboxItem item, boolean isSelected, Set<String> selectedIds) {
        AttributedStringBuilder asb = new AttributedStringBuilder();

        // Check if this is a separator
        if (item instanceof SeparatorItem) {
            // Separators don't get indicators or checkboxes, just display their text
            asb.append(item.getText());
            return asb.toAttributedString();
        }

        if (!item.isDisabled()) {
            // Selection indicator (only for selectable items)
            if (isSelected && item.isSelectable()) {
                asb.styled(config.style(PrompterConfig.CURSOR), config.indicator());
            } else {
                fillIndicatorSpace(asb);
            }
            asb.append(" ");

            // Checkbox state (only for selectable items)
            if (item.isSelectable()) {
                asb.styled(
                        config.style(PrompterConfig.BE),
                        selectedIds.contains(item.getName()) ? config.checkedBox() : config.uncheckedBox());
            } else {
                // Non-selectable items don't get checkboxes
                fillCheckboxSpace(asb);
            }

            // Item text
            asb.append(" ");
            asb.append(item.getText());
        } else {
            // Disabled item
            fillIndicatorSpace(asb);
            asb.append(" ");
            asb.styled(config.style(PrompterConfig.BD), config.unavailable());
            // Item text
            asb.append(" ");
            asb.append(item.getText());
            asb.append(" (").append(item.getDisabledText()).append(")");
        }

        return asb.toAttributedString();
    }

    /**
     * Bind keys for choice prompt operations.
     */
    private void bindChoiceKeys(KeyMap<ChoiceOperation> map) {
        // Bind printable characters to INSERT operation
        for (char i = 32; i < KEYMAP_LENGTH; i++) {
            map.bind(ChoiceOperation.INSERT, Character.toString(i));
        }
        // Bind action keys
        map.bind(ChoiceOperation.EXIT, "\r");
        map.bind(ChoiceOperation.CANCEL, esc());
        map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
    }

    /**
     * Build choice items display lines with proper styling.
     */
    private List<AttributedString> buildChoiceItemsDisplay(List<ChoiceItem> items) {
        List<AttributedString> out = new ArrayList<>();
        out.add(AttributedString.EMPTY); // Empty line before choices

        for (ChoiceItem item : items) {
            if (!item.isDisabled()) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.append("  ");
                if (item.getKey() != null && item.getKey() != ' ') {
                    asb.styled(config.style(PrompterConfig.CURSOR), item.getKey() + ") ");
                }
                asb.append(item.getText());
                if (item.isDefaultChoice()) {
                    asb.styled(config.style(PrompterConfig.AN), " (default)");
                }
                out.add(asb.toAttributedString());
            }
        }
        return out;
    }

    /**
     * Inner class for managing list pagination ranges.
     */
    private static class ListRange {
        final int first;
        final int last;

        public ListRange(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    /**
     * Compute the visible range of items based on cursor position, terminal size, and page size.
     */
    private void computeListRange(int cursorRow, int itemsSize, int pageSize) {
        if (range != null && range.first <= cursorRow - firstItemRow && range.last - 1 > cursorRow - firstItemRow) {
            return;
        }
        range = new ListRange(0, itemsSize);

        // Determine effective page size
        int effectivePageSize;
        if (pageSize > 0) {
            effectivePageSize = pageSize;
        } else {
            effectivePageSize = size.getRows() - firstItemRow;
        }

        if (effectivePageSize < itemsSize) {
            int itemId = cursorRow - firstItemRow;
            if (itemId < effectivePageSize - 1) {
                range = new ListRange(0, effectivePageSize);
            } else {
                range = new ListRange(itemId - effectivePageSize + 2, itemId + 2);
            }
        }
    }

    /**
     * Get the next selectable row in the list.
     */
    private static <T extends PromptItem> int nextRow(int row, int firstItemRow, List<T> items) {
        int itemsSize = items.size();
        int next;
        for (next = row + 1;
                next - firstItemRow < itemsSize
                        && !items.get(next - firstItemRow).isSelectable();
                next++) {}
        if (next - firstItemRow >= itemsSize) {
            for (next = firstItemRow;
                    next - firstItemRow < itemsSize
                            && !items.get(next - firstItemRow).isSelectable();
                    next++) {}
        }
        return next;
    }

    /**
     * Get the previous selectable row in the list.
     */
    private static <T extends PromptItem> int prevRow(int row, int firstItemRow, List<T> items) {
        int itemsSize = items.size();
        int prev;
        for (prev = row - 1;
                prev - firstItemRow >= 0 && !items.get(prev - firstItemRow).isSelectable();
                prev--) {}
        if (prev - firstItemRow < 0) {
            for (prev = firstItemRow + itemsSize - 1;
                    prev - firstItemRow >= 0 && !items.get(prev - firstItemRow).isSelectable();
                    prev--) {}
        }
        return prev;
    }

    /**
     * Reset display size tracking.
     */
    private void resetDisplay() {
        size.copy(terminal.getSize());
    }
}
