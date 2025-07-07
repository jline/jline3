/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jline.prompt.*;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.OSUtils;
import org.jline.utils.Size;

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

    // Column layout support
    private int columns = 1;
    private int lines = 1;
    private boolean rowsFirst = true; // true = row-first layout, false = column-first
    private static final int MARGIN_BETWEEN_COLUMNS = 2;

    /**
     * Create a new DefaultPrompter with the given terminal.
     *
     * @param terminal the terminal to use
     */
    public DefaultPrompter(Terminal terminal) {
        this(null, terminal, new DefaultConfig());
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
        this.display = new Display(terminal, true);
        this.bindingReader = new BindingReader(terminal.reader());
    }

    // Operation enums for different prompt types
    private enum ListOperation {
        FORWARD_ONE_LINE,
        BACKWARD_ONE_LINE,
        FORWARD_ONE_COLUMN,
        BACKWARD_ONE_COLUMN,
        INSERT,
        EXIT,
        CANCEL
    }

    private enum CheckboxOperation {
        FORWARD_ONE_LINE,
        BACKWARD_ONE_LINE,
        FORWARD_ONE_COLUMN,
        BACKWARD_ONE_COLUMN,
        TOGGLE,
        EXIT,
        CANCEL
    }

    private enum ChoiceOperation {
        INSERT,
        EXIT,
        CANCEL
    }

    @Override
    public PromptBuilder newBuilder() {
        return new DefaultPromptBuilder();
    }

    @Override
    public Map<String, ? extends PromptResult<? extends Prompt>> prompt(
            List<AttributedString> header, List<? extends Prompt> prompts) throws IOException, UserInterruptException {
        return executePrompts(header, prompts);
    }

    @Override
    public Map<String, ? extends PromptResult<? extends Prompt>> prompt(
            List<AttributedString> header,
            Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> promptsProvider)
            throws IOException {

        Map<String, PromptResult<? extends Prompt>> allResults = new HashMap<>();

        try {
            // Iteratively prompt until no more prompts are provided
            while (true) {
                // Get the next set of prompts based on current results
                List<? extends Prompt> prompts = promptsProvider.apply(allResults);

                // If no prompts returned, we're done
                if (prompts == null || prompts.isEmpty()) {
                    break;
                }

                // Execute the current batch of prompts
                Map<String, ? extends PromptResult<? extends Prompt>> batchResults = executePrompts(header, prompts);

                // Add results to our accumulated results
                for (Map.Entry<String, ? extends PromptResult<? extends Prompt>> entry : batchResults.entrySet()) {
                    allResults.put(entry.getKey(), entry.getValue());
                }

                // Clear header after first iteration to avoid repeating it
                header = null;
            }

            return allResults;

        } catch (UserInterruptException e) {
            throw new IOException("User interrupted", e);
        }
    }

    @Override
    public Terminal getTerminal() {
        return terminal;
    }

    @Override
    public LineReader getLineReader() {
        return reader;
    }

    /**
     * Execute a list of prompts and return results.
     */
    private Map<String, ? extends PromptResult<? extends Prompt>> executePrompts(
            List<AttributedString> header, List<? extends Prompt> prompts) throws IOException, UserInterruptException {

        if (prompts == null || prompts.isEmpty()) {
            return new HashMap<>();
        }

        try {
            open();
            Map<String, PromptResult<? extends Prompt>> results = new HashMap<>();

            // Display header only once at the beginning
            displayHeader(header);

            for (Prompt prompt : prompts) {
                try {
                    PromptResult<? extends Prompt> result = executePrompt(null, prompt); // Don't repeat header
                    if (result != null) {
                        results.put(prompt.getName(), result);
                    }
                } catch (UserInterruptException e) {
                    // Re-throw user interruption
                    throw e;
                } catch (Exception e) {
                    // Log error and continue with next prompt
                    terminal.writer().println("Error executing prompt '" + prompt.getName() + "': " + e.getMessage());
                    terminal.flush();
                }
            }

            return results;
        } finally {
            close();
        }
    }

    /**
     * Execute a single prompt and return its result.
     */
    @SuppressWarnings("unchecked")
    private PromptResult<? extends Prompt> executePrompt(List<AttributedString> header, Prompt prompt)
            throws IOException, UserInterruptException {

        if (prompt instanceof InputPrompt) {
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

        // Display header and prompt message
        displayHeader(header);
        displayPromptMessage(prompt.getMessage());

        // Read input from user
        String input;
        if (prompt.getMask() != null) {
            input = reader.readLine("", prompt.getMask());
        } else {
            String defaultValue = prompt.getDefaultValue();
            if (defaultValue != null) {
                input = reader.readLine("", null, defaultValue);
            } else {
                input = reader.readLine("");
            }
        }

        // Validate input if validator is provided
        if (prompt.getValidator() != null) {
            while (!prompt.getValidator().apply(input)) {
                displayError("Invalid input. Please try again.");
                if (prompt.getMask() != null) {
                    input = reader.readLine("", prompt.getMask());
                } else {
                    input = reader.readLine("");
                }
            }
        }

        return new DefaultInputResult(input, input, prompt);
    }

    private ListResult executeListPrompt(List<AttributedString> header, ListPrompt prompt)
            throws IOException, UserInterruptException {

        List<ListItem> items = prompt.getItems();
        if (items.isEmpty()) {
            return new DefaultListResult("", prompt);
        }

        // Initialize display
        resetDisplay();
        firstItemRow = header.size() + 1; // Header + message line

        // Calculate column layout
        calculateColumnLayout(items);

        // Find first selectable item
        int selectRow = nextRow(firstItemRow - 1, firstItemRow, items);

        // Set up key bindings
        KeyMap<ListOperation> keyMap = new KeyMap<>();
        bindListKeys(keyMap);

        // Interactive selection loop
        while (true) {
            // Update display with current selection
            refreshListDisplay(header, prompt.getMessage(), items, selectRow);

            // Read user input using BindingReader
            ListOperation op = bindingReader.readBinding(keyMap);

            switch (op) {
                case FORWARD_ONE_LINE:
                    selectRow = nextRow(selectRow, firstItemRow, items);
                    break;
                case BACKWARD_ONE_LINE:
                    selectRow = prevRow(selectRow, firstItemRow, items);
                    break;
                case FORWARD_ONE_COLUMN:
                    selectRow = nextColumn(selectRow, firstItemRow, items, columns, lines, rowsFirst);
                    break;
                case BACKWARD_ONE_COLUMN:
                    selectRow = prevColumn(selectRow, firstItemRow, items, columns, lines, rowsFirst);
                    break;
                case INSERT:
                    // Handle character-based selection (if items have keys)
                    String ch = bindingReader.getLastBinding();
                    int id = 0;
                    for (ListItem item : items) {
                        if (item instanceof ChoiceItem) {
                            ChoiceItem choiceItem = (ChoiceItem) item;
                            if (choiceItem.isSelectable() && choiceItem.getKey() != null &&
                                choiceItem.getKey().toString().equals(ch)) {
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
                case CANCEL:
                    throw new UserInterruptException("User cancelled");
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
        firstItemRow = header.size() + 1; // Header + message line

        // Calculate column layout
        calculateColumnLayout(items);

        // Find first selectable item
        int selectRow = nextRow(firstItemRow - 1, firstItemRow, items);

        // Set up key bindings
        KeyMap<CheckboxOperation> keyMap = new KeyMap<>();
        bindCheckboxKeys(keyMap);

        // Interactive selection loop
        while (true) {
            // Update display with current selection and checkbox states
            refreshCheckboxDisplay(header, prompt.getMessage(), items, selectRow, selectedIds);

            // Read user input using BindingReader
            CheckboxOperation op = bindingReader.readBinding(keyMap);

            switch (op) {
                case FORWARD_ONE_LINE:
                    selectRow = nextRow(selectRow, firstItemRow, items);
                    break;
                case BACKWARD_ONE_LINE:
                    selectRow = prevRow(selectRow, firstItemRow, items);
                    break;
                case FORWARD_ONE_COLUMN:
                    selectRow = nextColumn(selectRow, firstItemRow, items, columns, lines, rowsFirst);
                    break;
                case BACKWARD_ONE_COLUMN:
                    selectRow = prevColumn(selectRow, firstItemRow, items, columns, lines, rowsFirst);
                    break;
                case TOGGLE:
                    CheckboxItem currentItem = items.get(selectRow - firstItemRow);
                    if (currentItem.isSelectable()) {
                        if (selectedIds.contains(currentItem.getName())) {
                            selectedIds.remove(currentItem.getName());
                        } else {
                            selectedIds.add(currentItem.getName());
                        }
                    }
                    break;
                case EXIT:
                    return new DefaultCheckboxResult(selectedIds, prompt);
                case CANCEL:
                    throw new UserInterruptException("User cancelled");
            }
        }
    }

    private ChoiceResult executeChoicePrompt(List<AttributedString> header, ChoicePrompt prompt)
            throws IOException, UserInterruptException {

        displayHeader(header);
        displayPromptMessage(prompt.getMessage());

        List<ChoiceItem> items = prompt.getItems();
        if (items.isEmpty()) {
            return new DefaultChoiceResult("", prompt);
        }

        // Display available choices with their keys
        displayChoiceItems(items);

        // Find default choice if any
        ChoiceItem defaultChoice = null;
        for (ChoiceItem item : items) {
            if (item.isDefaultChoice() && item.isSelectable()) {
                defaultChoice = item;
                break;
            }
        }

        terminal.writer().print("Choice: ");
        terminal.flush();

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
                        if (item.isSelectable() && item.getKey() != null &&
                            item.getKey().toString().equalsIgnoreCase(ch)) {
                            // Found matching choice
                            terminal.writer().print(ch);
                            terminal.writer().println();
                            terminal.flush();
                            return new DefaultChoiceResult(item.getName(), prompt);
                        }
                    }
                    // Invalid choice, continue waiting
                    break;
                case EXIT:
                    // Use default choice if available
                    if (defaultChoice != null) {
                        terminal.writer().println(defaultChoice.getKey() != null ? defaultChoice.getKey().toString() : "");
                        terminal.flush();
                        return new DefaultChoiceResult(defaultChoice.getName(), prompt);
                    }
                    // No default, continue waiting for input
                    break;
                case CANCEL:
                    terminal.writer().println();
                    terminal.flush();
                    throw new UserInterruptException("User cancelled");
            }
        }
    }

    private ConfirmResult executeConfirmPrompt(List<AttributedString> header, ConfirmPrompt prompt)
            throws IOException, UserInterruptException {

        displayHeader(header);
        displayPromptMessage(prompt.getMessage() + " (y/N)");

        String input = reader.readLine("").trim().toLowerCase();
        boolean confirmed = "y".equals(input) || "yes".equals(input);

        ConfirmResult.ConfirmationValue value =
                confirmed ? ConfirmResult.ConfirmationValue.YES : ConfirmResult.ConfirmationValue.NO;

        return new DefaultConfirmResult(value, prompt);
    }

    private PromptResult<TextPrompt> executeTextPrompt(List<AttributedString> header, TextPrompt prompt)
            throws IOException, UserInterruptException {

        displayHeader(header);
        displayText(prompt.getText());

        // Text prompts don't require user input, just display
        return new AbstractPromptResult<TextPrompt>(prompt) {
            @Override
            public String getResult() {
                return "TEXT_DISPLAYED";
            }
        };
    }

    private void displayHeader(List<AttributedString> header) {
        if (header != null && !header.isEmpty()) {
            for (AttributedString line : header) {
                terminal.writer().println(line.toAnsi(terminal));
            }
            terminal.writer().println();
            terminal.flush();
        }
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
        attributes = terminal.enterRawMode();
        display.clear();
    }

    private void close() throws IOException {
        if (attributes != null) {
            terminal.setAttributes(attributes);
        }
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
        map.bind(ListOperation.FORWARD_ONE_COLUMN, key(terminal, key_right));
        map.bind(ListOperation.BACKWARD_ONE_COLUMN, key(terminal, key_left));
        // Bind action keys
        map.bind(ListOperation.EXIT, "\r");
        map.bind(ListOperation.CANCEL, esc());
        map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
    }

    /**
     * Refresh the display for list prompts using JLine's Display class.
     */
    private void refreshListDisplay(List<AttributedString> header, String message, List<ListItem> items, int cursorRow) {
        display.resize(size.getRows(), size.getColumns());
        display.update(
                buildListDisplayLines(header, message, items, cursorRow),
                size.cursorPos(Math.min(size.getRows() - 1, firstItemRow + items.size()), 0));
    }

    /**
     * Build display lines for list prompts with column layout support.
     */
    private List<AttributedString> buildListDisplayLines(List<AttributedString> header, String message, List<ListItem> items, int cursorRow) {
        List<AttributedString> out = new ArrayList<>(header);

        // Add message line
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        out.add(asb.toAttributedString());

        if (columns == 1) {
            // Single column layout - use original logic with pagination
            computeListRange(cursorRow, items.size());
            for (int i = range.first; i < range.last - 1; i++) {
                if (items.isEmpty() || i > items.size() - 1) {
                    break;
                }
                out.add(buildSingleItemLine(items.get(i), i + firstItemRow == cursorRow));
            }
        } else {
            // Multi-column layout
            out.addAll(buildMultiColumnLines(items, cursorRow));
        }

        return out;
    }

    /**
     * Build a single item line for display.
     */
    private AttributedString buildSingleItemLine(ListItem item, boolean isSelected) {
        AttributedStringBuilder asb = new AttributedStringBuilder();

        // Add selection indicator and key if available
        String key = item instanceof ChoiceItem ? ((ChoiceItem) item).getKey() + " - " : "";
        if (isSelected) {
            asb.append(config.indicator())
                    .style(AttributedStyle.DEFAULT.inverse())
                    .append(" ")
                    .append(key)
                    .append(item.getText());
        } else if (item.isSelectable()) {
            fillIndicatorSpace(asb);
            asb.append(" ").append(key).append(item.getText());
        } else {
            // Disabled item
            fillIndicatorSpace(asb);
            asb.append(" ").append(key);
            if (item.isDisabled()) {
                asb.append(item.getDisabledText()).append(" (").append(item.getDisabledText()).append(")");
            } else {
                asb.append(item.getText());
            }
        }

        return asb.toAttributedString();
    }

    /**
     * Build multi-column layout lines.
     */
    private List<AttributedString> buildMultiColumnLines(List<ListItem> items, int cursorRow) {
        List<AttributedString> out = new ArrayList<>();

        // Calculate column width
        int terminalWidth = size.getColumns();
        int columnWidth = (terminalWidth - (columns - 1) * MARGIN_BETWEEN_COLUMNS) / columns;

        for (int row = 0; row < lines; row++) {
            AttributedStringBuilder lineBuilder = new AttributedStringBuilder();

            for (int col = 0; col < columns; col++) {
                int index = gridToIndex(row, col, items.size());
                if (index >= 0 && index < items.size()) {
                    ListItem item = items.get(index);
                    boolean isSelected = (index + firstItemRow) == cursorRow;

                    // Build item text
                    AttributedStringBuilder itemBuilder = new AttributedStringBuilder();
                    String key = item instanceof ChoiceItem ? ((ChoiceItem) item).getKey() + " - " : "";

                    if (isSelected) {
                        itemBuilder.append(config.indicator())
                                .style(AttributedStyle.DEFAULT.inverse())
                                .append(" ")
                                .append(key)
                                .append(item.getText());
                    } else if (item.isSelectable()) {
                        fillIndicatorSpace(itemBuilder);
                        itemBuilder.append(" ").append(key).append(item.getText());
                    } else {
                        // Disabled item
                        fillIndicatorSpace(itemBuilder);
                        itemBuilder.append(" ").append(key);
                        if (item.isDisabled()) {
                            itemBuilder.append(item.getDisabledText());
                        } else {
                            itemBuilder.append(item.getText());
                        }
                    }

                    // Pad to column width
                    String itemText = itemBuilder.toString();
                    int itemLength = display.wcwidth(itemText);
                    lineBuilder.append(itemText);

                    // Add padding to reach column width
                    for (int i = itemLength; i < columnWidth; i++) {
                        lineBuilder.append(' ');
                    }

                    // Add margin between columns (except for last column)
                    if (col < columns - 1) {
                        for (int i = 0; i < MARGIN_BETWEEN_COLUMNS; i++) {
                            lineBuilder.append(' ');
                        }
                    }
                }
            }

            out.add(lineBuilder.toAttributedString());
        }

        return out;
    }

    /**
     * Fill space for indicator alignment.
     */
    private AttributedStringBuilder fillIndicatorSpace(AttributedStringBuilder asb) {
        for (int i = 0; i < config.indicator().length(); i++) {
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
        map.bind(CheckboxOperation.FORWARD_ONE_COLUMN, key(terminal, key_right));
        map.bind(CheckboxOperation.BACKWARD_ONE_COLUMN, key(terminal, key_left));
        // Bind toggle key
        map.bind(CheckboxOperation.TOGGLE, " ");
        // Bind action keys
        map.bind(CheckboxOperation.EXIT, "\r");
        map.bind(CheckboxOperation.CANCEL, esc());
        map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
    }

    /**
     * Refresh the display for checkbox prompts using JLine's Display class.
     */
    private void refreshCheckboxDisplay(List<AttributedString> header, String message, List<CheckboxItem> items, int cursorRow, Set<String> selectedIds) {
        display.resize(size.getRows(), size.getColumns());
        display.update(
                buildCheckboxDisplayLines(header, message, items, cursorRow, selectedIds),
                size.cursorPos(Math.min(size.getRows() - 1, firstItemRow + items.size()), 0));
    }

    /**
     * Build display lines for checkbox prompts with column layout support.
     */
    private List<AttributedString> buildCheckboxDisplayLines(List<AttributedString> header, String message, List<CheckboxItem> items, int cursorRow, Set<String> selectedIds) {
        List<AttributedString> out = new ArrayList<>(header);

        // Add message line
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        out.add(asb.toAttributedString());

        if (columns == 1) {
            // Single column layout - use original logic with pagination
            computeListRange(cursorRow, items.size());
            for (int i = range.first; i < range.last - 1; i++) {
                if (items.isEmpty() || i > items.size() - 1) {
                    break;
                }
                out.add(buildSingleCheckboxLine(items.get(i), i + firstItemRow == cursorRow, selectedIds));
            }
        } else {
            // Multi-column layout
            out.addAll(buildMultiColumnCheckboxLines(items, cursorRow, selectedIds));
        }

        return out;
    }

    /**
     * Build a single checkbox item line for display.
     */
    private AttributedString buildSingleCheckboxLine(CheckboxItem item, boolean isSelected, Set<String> selectedIds) {
        AttributedStringBuilder asb = new AttributedStringBuilder();

        if (item.isSelectable()) {
            // Selection indicator
            if (isSelected) {
                asb.append(config.indicator()).style(AttributedStyle.DEFAULT).append(" ");
            } else {
                fillIndicatorSpace(asb).append(" ");
            }

            // Checkbox state
            if (selectedIds.contains(item.getName())) {
                asb.append(config.checkedBox());
            } else {
                asb.append(config.uncheckedBox());
            }
        } else {
            // Disabled item
            fillIndicatorSpace(asb);
            asb.append(" ");
            if (item.isDisabled()) {
                asb.append(config.unavailable());
            } else {
                fillCheckboxSpace(asb);
            }
        }

        // Item text
        asb.append(item.getText());
        if (item.isDisabled()) {
            asb.append(" (").append(item.getDisabledText()).append(")");
        }

        return asb.toAttributedString();
    }

    /**
     * Build multi-column checkbox layout lines.
     */
    private List<AttributedString> buildMultiColumnCheckboxLines(List<CheckboxItem> items, int cursorRow, Set<String> selectedIds) {
        List<AttributedString> out = new ArrayList<>();

        // Calculate column width
        int terminalWidth = size.getColumns();
        int columnWidth = (terminalWidth - (columns - 1) * MARGIN_BETWEEN_COLUMNS) / columns;

        for (int row = 0; row < lines; row++) {
            AttributedStringBuilder lineBuilder = new AttributedStringBuilder();

            for (int col = 0; col < columns; col++) {
                int index = gridToIndex(row, col, items.size());
                if (index >= 0 && index < items.size()) {
                    CheckboxItem item = items.get(index);
                    boolean isSelected = (index + firstItemRow) == cursorRow;

                    // Build item text
                    AttributedStringBuilder itemBuilder = new AttributedStringBuilder();

                    if (item.isSelectable()) {
                        // Selection indicator
                        if (isSelected) {
                            itemBuilder.append(config.indicator()).style(AttributedStyle.DEFAULT).append(" ");
                        } else {
                            fillIndicatorSpace(itemBuilder).append(" ");
                        }

                        // Checkbox state
                        if (selectedIds.contains(item.getName())) {
                            itemBuilder.append(config.checkedBox());
                        } else {
                            itemBuilder.append(config.uncheckedBox());
                        }
                    } else {
                        // Disabled item
                        fillIndicatorSpace(itemBuilder);
                        itemBuilder.append(" ");
                        if (item.isDisabled()) {
                            itemBuilder.append(config.unavailable());
                        } else {
                            fillCheckboxSpace(itemBuilder);
                        }
                    }

                    // Item text
                    itemBuilder.append(item.getText());
                    if (item.isDisabled()) {
                        itemBuilder.append(" (").append(item.getDisabledText()).append(")");
                    }

                    // Pad to column width
                    String itemText = itemBuilder.toString();
                    int itemLength = display.wcwidth(itemText);
                    lineBuilder.append(itemText);

                    // Add padding to reach column width
                    for (int i = itemLength; i < columnWidth; i++) {
                        lineBuilder.append(' ');
                    }

                    // Add margin between columns (except for last column)
                    if (col < columns - 1) {
                        for (int i = 0; i < MARGIN_BETWEEN_COLUMNS; i++) {
                            lineBuilder.append(' ');
                        }
                    }
                }
            }

            out.add(lineBuilder.toAttributedString());
        }

        return out;
    }

    /**
     * Fill space for checkbox alignment.
     */
    private void fillCheckboxSpace(AttributedStringBuilder asb) {
        for (int i = 0; i < config.checkedBox().length(); i++) {
            asb.append(" ");
        }
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
     * Display choice items with their keys.
     */
    private void displayChoiceItems(List<ChoiceItem> items) {
        terminal.writer().println();
        for (ChoiceItem item : items) {
            if (item.isSelectable()) {
                terminal.writer().print("  ");
                if (item.getKey() != null && item.getKey() != ' ') {
                    terminal.writer().print(item.getKey() + ") ");
                }
                terminal.writer().print(item.getText());
                if (item.isDefaultChoice()) {
                    terminal.writer().print(" (default)");
                }
                terminal.writer().println();
            }
        }
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
     * Compute the visible range of items based on cursor position and terminal size.
     */
    private void computeListRange(int cursorRow, int itemsSize) {
        if (range != null && range.first <= cursorRow - firstItemRow && range.last - 1 > cursorRow - firstItemRow) {
            return;
        }
        range = new ListRange(0, itemsSize + 1);
        if (size.getRows() < firstItemRow + itemsSize) {
            int itemId = cursorRow - firstItemRow;
            int forList = size.getRows() - firstItemRow;
            if (itemId < forList - 1) {
                range = new ListRange(0, forList);
            } else {
                range = new ListRange(itemId - forList + 2, itemId + 2);
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
                next - firstItemRow < itemsSize && !items.get(next - firstItemRow).isSelectable();
                next++) {}
        if (next - firstItemRow >= itemsSize) {
            for (next = firstItemRow;
                    next - firstItemRow < itemsSize && !items.get(next - firstItemRow).isSelectable();
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

    /**
     * Calculate column layout for items based on terminal width.
     */
    private void calculateColumnLayout(List<? extends PromptItem> items) {
        if (items.isEmpty()) {
            columns = 1;
            lines = 1;
            return;
        }

        // Calculate maximum item width
        int maxWidth = 0;
        for (PromptItem item : items) {
            String text = item.getText();
            if (item instanceof ChoiceItem) {
                ChoiceItem choice = (ChoiceItem) item;
                if (choice.getKey() != null) {
                    text = choice.getKey() + " - " + text;
                }
            }
            maxWidth = Math.max(maxWidth, display.wcwidth(text));
        }

        // Add space for indicator and checkbox symbols
        maxWidth += config.indicator().length() + 1; // indicator + space
        if (items.get(0) instanceof CheckboxItem) {
            maxWidth += Math.max(config.checkedBox().length(), config.uncheckedBox().length());
        }

        // Calculate how many columns fit
        int terminalWidth = size.getColumns();
        columns = Math.max(1, terminalWidth / (maxWidth + MARGIN_BETWEEN_COLUMNS));

        // Adjust if we have fewer items than columns
        columns = Math.min(columns, items.size());

        // Calculate lines needed
        lines = (items.size() + columns - 1) / columns;

        // Ensure we don't exceed available terminal height
        int availableRows = size.getRows() - firstItemRow;
        if (lines > availableRows && availableRows > 0) {
            lines = availableRows;
            columns = (items.size() + lines - 1) / lines;
        }
    }

    /**
     * Convert 2D grid position to linear item index.
     */
    private int gridToIndex(int row, int col, int totalItems) {
        int index;
        if (rowsFirst) {
            index = row * columns + col;
        } else {
            index = col * lines + row;
        }
        return index < totalItems ? index : -1;
    }

    /**
     * Convert linear item index to 2D grid position.
     */
    private int[] indexToGrid(int index) {
        if (rowsFirst) {
            return new int[]{index / columns, index % columns};
        } else {
            return new int[]{index % lines, index / lines};
        }
    }

    /**
     * Navigate to next column in grid layout.
     */
    private static <T extends PromptItem> int nextColumn(int currentRow, int firstItemRow, List<T> items,
                                                        int columns, int lines, boolean rowsFirst) {
        int currentIndex = currentRow - firstItemRow;
        int[] grid = indexToGrid(currentIndex, columns, lines, rowsFirst);
        int row = grid[0];
        int col = grid[1];

        // Move right
        col = (col + 1) % columns;

        int newIndex = gridToIndex(row, col, items.size(), columns, lines, rowsFirst);
        if (newIndex >= 0 && newIndex < items.size() && items.get(newIndex).isSelectable()) {
            return firstItemRow + newIndex;
        }

        // If target is not selectable, find next selectable item
        return nextRow(currentRow, firstItemRow, items);
    }

    /**
     * Navigate to previous column in grid layout.
     */
    private static <T extends PromptItem> int prevColumn(int currentRow, int firstItemRow, List<T> items,
                                                        int columns, int lines, boolean rowsFirst) {
        int currentIndex = currentRow - firstItemRow;
        int[] grid = indexToGrid(currentIndex, columns, lines, rowsFirst);
        int row = grid[0];
        int col = grid[1];

        // Move left
        col = (col - 1 + columns) % columns;

        int newIndex = gridToIndex(row, col, items.size(), columns, lines, rowsFirst);
        if (newIndex >= 0 && newIndex < items.size() && items.get(newIndex).isSelectable()) {
            return firstItemRow + newIndex;
        }

        // If target is not selectable, find previous selectable item
        return prevRow(currentRow, firstItemRow, items);
    }

    /**
     * Helper methods for grid calculations.
     */
    private static int gridToIndex(int row, int col, int totalItems, int columns, int lines, boolean rowsFirst) {
        int index;
        if (rowsFirst) {
            index = row * columns + col;
        } else {
            index = col * lines + row;
        }
        return index < totalItems ? index : -1;
    }

    private static int[] indexToGrid(int index, int columns, int lines, boolean rowsFirst) {
        if (rowsFirst) {
            return new int[]{index / columns, index % columns};
        } else {
            return new int[]{index % lines, index / lines};
        }
    }

    /**
     * Default implementation of the Prompter.Config interface.
     */
    public static class DefaultConfig implements PrompterConfig {
        private final String indicator;
        private final String uncheckedBox;
        private final String checkedBox;
        private final String unavailable;
        private final boolean cancellableFirstPrompt;

        /**
         * Create a new DefaultConfig with default values.
         */
        public DefaultConfig() {
            this(null, null, null, null, false);
        }

        /**
         * Create a new DefaultConfig with the given values.
         *
         * @param indicator the indicator character/string
         * @param uncheckedBox the unchecked box character/string
         * @param checkedBox the checked box character/string
         * @param unavailable the unavailable item character/string
         * @param cancellableFirstPrompt if the first prompt can be cancelled
         */
        public DefaultConfig(
                String indicator,
                String uncheckedBox,
                String checkedBox,
                String unavailable,
                boolean cancellableFirstPrompt) {
            // Set platform-specific defaults if not explicitly provided
            if (indicator == null) {
                if (OSUtils.IS_WINDOWS) {
                    this.indicator = ">";
                } else {
                    this.indicator = "\u276F"; // ❯
                }
            } else {
                this.indicator = indicator;
            }

            if (uncheckedBox == null) {
                if (OSUtils.IS_WINDOWS) {
                    this.uncheckedBox = "( )";
                } else {
                    this.uncheckedBox = "\u25EF "; // ◯
                }
            } else {
                this.uncheckedBox = uncheckedBox;
            }

            if (checkedBox == null) {
                if (OSUtils.IS_WINDOWS) {
                    this.checkedBox = "(x)";
                } else {
                    this.checkedBox = "\u25C9 "; // ◉
                }
            } else {
                this.checkedBox = checkedBox;
            }

            if (unavailable == null) {
                if (OSUtils.IS_WINDOWS) {
                    this.unavailable = "( )";
                } else {
                    this.unavailable = "\u25EF "; // ◯
                }
            } else {
                this.unavailable = unavailable;
            }

            this.cancellableFirstPrompt = cancellableFirstPrompt;
        }

        @Override
        public String indicator() {
            return indicator;
        }

        @Override
        public String uncheckedBox() {
            return uncheckedBox;
        }

        @Override
        public String checkedBox() {
            return checkedBox;
        }

        @Override
        public String unavailable() {
            return unavailable;
        }

        @Override
        public boolean cancellableFirstPrompt() {
            return cancellableFirstPrompt;
        }
    }
}
