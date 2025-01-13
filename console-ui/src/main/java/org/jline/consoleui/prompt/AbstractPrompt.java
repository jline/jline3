/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.elements.ExpandableChoice;
import org.jline.consoleui.elements.InputValue;
import org.jline.consoleui.elements.items.*;
import org.jline.consoleui.elements.items.impl.CheckboxItem;
import org.jline.consoleui.elements.items.impl.ChoiceItem;
import org.jline.consoleui.elements.items.impl.Separator;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.CompletionMatcherImpl;
import org.jline.reader.impl.ReaderUtils;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.*;

import static org.jline.keymap.KeyMap.*;

/**
 * Classes for all prompt implementations.
 */
public abstract class AbstractPrompt<T extends ConsoleUIItemIF> {
    protected final Terminal terminal;
    protected final BindingReader bindingReader;
    private final List<AttributedString> header;
    private final AttributedString message;
    protected final List<T> items;
    protected int firstItemRow;
    private final Size size = new Size();
    protected final ConsolePrompt.UiConfig config;
    private Display display;
    private ListRange range = null;

    public static final long DEFAULT_TIMEOUT_WITH_ESC = 150L;

    public AbstractPrompt(
            Terminal terminal, List<AttributedString> header, AttributedString message, ConsolePrompt.UiConfig cfg) {
        this(terminal, header, message, new ArrayList<>(), 0, cfg);
    }

    public AbstractPrompt(
            Terminal terminal,
            List<AttributedString> header,
            AttributedString message,
            List<T> items,
            int pageSize,
            ConsolePrompt.UiConfig cfg) {
        this.terminal = terminal;
        this.bindingReader = new BindingReader(terminal.reader());
        this.size.copy(terminal.getSize());
        int listSpace = Math.min(size.getRows(), Math.max(pageSize, 3));
        this.header = header.size() > size.getRows() - listSpace
                ? header.subList(header.size() - size.getRows() + listSpace, header.size())
                : header;
        this.message = message;
        this.items = items;
        this.firstItemRow = this.header.size() + 1;
        this.config = cfg;
    }

    protected void resetHeader() {
        this.firstItemRow = header.size() + 1;
    }

    protected void resetDisplay() {
        display = new Display(terminal, true);
        size.copy(terminal.getSize());
        display.clear();
        display.reset();
    }

    protected void refreshDisplay(int row) {
        refreshDisplay(row, 0, null, false);
    }

    protected void refreshDisplay(int row, Set<String> selected) {
        display.resize(size.getRows(), size.getColumns());
        display.reset();
        display.update(
                displayLines(row, selected),
                size.cursorPos(Math.min(size.getRows() - 1, firstItemRow + items.size()), 0));
    }

    protected void refreshDisplay(int row, int column, String buffer, boolean newline) {
        display.resize(size.getRows(), size.getColumns());
        AttributedStringBuilder asb = new AttributedStringBuilder();
        int crow = column == 0 ? Math.min(size.getRows() - 1, firstItemRow + items.size()) : row;
        if (buffer != null) {
            if (newline && !buffer.isEmpty()) {
                asb.style(config.style(".pr")).append(">> ");
            }
            asb.style(AttributedStyle.DEFAULT).append(buffer);
        }
        display.update(displayLines(row, asb.toAttributedString(), newline), size.cursorPos(crow, column));
    }

    protected void refreshDisplay(
            int buffRow, int buffCol, String buffer, int candRow, int candCol, List<Candidate> candidates) {
        display.resize(size.getRows(), size.getColumns());
        AttributedStringBuilder asb = new AttributedStringBuilder();
        if (buffer != null) {
            asb.style(AttributedStyle.DEFAULT).append(buffer);
        }
        display.update(
                displayLines(candRow, candCol, asb.toAttributedString(), candidates), size.cursorPos(buffRow, buffCol));
    }

    private int candidateStartPosition(int candidatesColumn, String buffer, List<Candidate> cands) {
        List<String> values = cands.stream()
                .map(c -> AttributedString.stripAnsi(c.displ()))
                .filter(c -> !c.matches("\\w+") && c.length() > 1)
                .collect(Collectors.toList());
        Set<String> notDelimiters = new HashSet<>();
        values.forEach(v -> v.substring(0, v.length() - 1)
                .chars()
                .filter(c -> !Character.isDigit(c) && !Character.isAlphabetic(c))
                .forEach(c -> notDelimiters.add(Character.toString((char) c))));
        int out = candidatesColumn;
        for (int i = buffer.length(); i > 0; i--) {
            if (buffer.substring(0, i).matches(".*\\W") && !notDelimiters.contains(buffer.substring(i - 1, i))) {
                out += i;
                break;
            }
        }
        return out;
    }

    private List<AttributedString> displayLines(
            int cursorRow, int candidatesColumn, AttributedString buffer, List<Candidate> candidates) {
        computeListRange(cursorRow, candidates.size());
        List<AttributedString> out = new ArrayList<>(header);
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        asb.append(buffer);
        out.add(asb.toAttributedString());
        int listStart;
        if (cursorRow - firstItemRow >= 0 && !candidates.isEmpty() && cursorRow - firstItemRow < candidates.size()) {
            String dc = candidates.get(cursorRow - firstItemRow).displ();
            listStart = candidatesColumn
                    + buffer.columnLength()
                    - display.wcwidth(dc)
                    + (AttributedString.stripAnsi(dc).endsWith("*") ? 1 : 0);
        } else {
            listStart = candidateStartPosition(candidatesColumn, buffer.toString(), candidates);
        }
        int width = Math.max(
                candidates.stream()
                        .map(Candidate::displ)
                        .mapToInt(display::wcwidth)
                        .max()
                        .orElse(20),
                20);
        for (int i = range.first; i < range.last - 1; i++) {
            if (candidates.isEmpty() || i > candidates.size() - 1) {
                break;
            }
            Candidate c = candidates.get(i);
            asb = new AttributedStringBuilder();
            AttributedStringBuilder tmp = new AttributedStringBuilder();
            tmp.ansiAppend(c.displ());
            asb.style(tmp.styleAt(0));
            if (i + firstItemRow == cursorRow) {
                asb.style(new AttributedStyle().inverse());
            }
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

    private List<AttributedString> displayLines(int cursorRow, Set<String> selected) {
        computeListRange(cursorRow, items.size());
        List<AttributedString> out = new ArrayList<>(header);
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        out.add(asb.toAttributedString());
        for (int i = range.first; i < range.last - 1; i++) {
            if (items.isEmpty() || i > items.size() - 1) {
                break;
            }
            ConsoleUIItemIF s = items.get(i);
            asb = new AttributedStringBuilder();
            if (s.isSelectable()) {
                asb = i + firstItemRow == cursorRow
                        ? asb.append(config.indicator())
                                .style(AttributedStyle.DEFAULT)
                                .append(" ")
                        : fillIndicatorSpace(asb).append(" ");
                asb = selected.contains(s.getName())
                        ? asb.append(config.checkedBox())
                        : asb.append(config.uncheckedBox());
            } else if (s instanceof CheckboxItem) {
                fillIndicatorSpace(asb);
                asb.append(" ");
                if (s.isDisabled()) {
                    asb.append(config.unavailable());
                } else {
                    fillCheckboxSpace(asb);
                }
            }
            asb.append(s.getText()).toAttributedString();
            if (s.isDisabled()) {
                asb.append(" (").append(s.getDisabledText()).append(")");
            }
            int textLength = asb.length();
            for (int j = 0; j < size.getColumns() - textLength; j++) {
                asb.append(' ');
            }
            out.add(asb.toAttributedString());
        }
        return out;
    }

    private AttributedStringBuilder fillIndicatorSpace(AttributedStringBuilder asb) {
        for (int i = 0; i < config.indicator().length(); i++) {
            asb.append(" ");
        }
        return asb;
    }

    private void fillCheckboxSpace(AttributedStringBuilder asb) {
        for (int i = 0; i < config.checkedBox().length(); i++) {
            asb.append(" ");
        }
    }

    private static class ListRange {
        final int first;
        final int last;

        public ListRange(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    private void computeListRange(int cursorRow, int itemsSize) {
        if (range != null && range.first <= cursorRow - firstItemRow && range.last - 1 > cursorRow - firstItemRow) {
            return;
        }
        range = new ListRange(0, itemsSize + 1);
        if (size.getRows() < header.size() + itemsSize + 1) {
            int itemId = cursorRow - firstItemRow;
            int forList = size.getRows() - header.size();
            if (itemId < forList - 1) {
                range = new ListRange(0, forList);
            } else {
                range = new ListRange(itemId - forList + 2, itemId + 2);
            }
        }
    }

    private List<AttributedString> displayLines(int cursorRow, AttributedString buffer, boolean newline) {
        computeListRange(cursorRow, items.size());
        List<AttributedString> out = new ArrayList<>(header);
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        if (buffer != null && !newline) {
            asb.append(buffer);
        }
        out.add(asb.toAttributedString());
        if (buffer != null && newline) {
            asb = new AttributedStringBuilder();
            asb.append(buffer);
            out.add(asb.toAttributedString());
        }
        for (int i = range.first; i < range.last - 1; i++) {
            ConsoleUIItemIF s = items.get(i);
            asb = new AttributedStringBuilder();
            String key = s instanceof ChoiceItem ? ((ChoiceItem) s).getKey() + " - " : "";
            if (i + firstItemRow == cursorRow) {
                out.add(asb.append(config.indicator())
                        .style(config.style(".se"))
                        .append(" ")
                        .append(key)
                        .append(s.getText())
                        .toAttributedString());
            } else if (!(s instanceof Separator)) {
                fillIndicatorSpace(asb);
                out.add(asb.append(" ").append(key).append(s.getText()).toAttributedString());
            } else {
                out.add(asb.append(s.getText()).toAttributedString());
            }
        }
        return out;
    }

    protected static class ExpandableChoicePrompt extends AbstractPrompt<ListItemIF> {
        private enum Operation {
            INSERT,
            EXIT,
            CANCEL
        }

        private final int startColumn;
        private final List<ChoiceItemIF> items;
        private final ConsolePrompt.UiConfig config;

        private ExpandableChoicePrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                ExpandableChoice expandableChoice,
                ConsolePrompt.UiConfig cfg) {
            super(terminal, header, message, cfg);
            startColumn = message.columnLength();
            items = expandableChoice.getChoiceItems();
            config = cfg;
        }

        public static ExpandableChoicePrompt getPrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                ExpandableChoice expandableChoice,
                ConsolePrompt.UiConfig cfg) {
            return new ExpandableChoicePrompt(terminal, header, message, expandableChoice, cfg);
        }

        private void bindKeys(KeyMap<Operation> map) {
            for (char i = 32; i < KEYMAP_LENGTH; i++) {
                map.bind(Operation.INSERT, Character.toString(i));
            }
            map.bind(Operation.EXIT, "\r");
            map.bind(Operation.CANCEL, KeyMap.esc());
            map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
        }

        public ExpandableChoiceResult execute() {
            resetDisplay();
            int row = firstItemRow - 1;
            KeyMap<Operation> keyMap = new KeyMap<>();
            bindKeys(keyMap);
            StringBuilder buffer = new StringBuilder();
            String selectedId = null;
            boolean expandChoiceList = false;
            for (ChoiceItemIF cu : items) {
                if (cu.isSelectable() && cu.isDefaultChoice()) {
                    selectedId = cu.getName();
                    break;
                }
            }
            while (true) {
                refreshDisplay(row, startColumn, buffer.toString(), true);
                Operation op = bindingReader.readBinding(keyMap);
                buffer = new StringBuilder();
                switch (op) {
                    case INSERT:
                        String ch = bindingReader.getLastBinding();
                        if (ch.equals("h")) {
                            expandChoiceList = true;
                            buffer.append(config.resourceBundle().getString("help.list.all.options"));
                        } else {
                            selectedId = null;
                            expandChoiceList = false;
                            boolean found = false;
                            for (ChoiceItemIF cu : items) {
                                if (cu.isSelectable() && cu.getKey().toString().equals(ch)) {
                                    selectedId = cu.getName();
                                    buffer.append(selectedId);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                buffer.append(config.resourceBundle().getString("please.enter.a.valid.command"));
                            }
                        }
                        break;
                    case EXIT:
                        if (selectedId == null || expandChoiceList) {
                            if (expandChoiceList) {
                                throw new ExpandableChoiceException();
                            }
                            break;
                        }
                        return new ExpandableChoiceResult(selectedId);
                    case CANCEL:
                        return null;
                }
            }
        }
    }

    @SuppressWarnings("serial")
    protected static class ExpandableChoiceException extends RuntimeException {}

    protected static class ConfirmPrompt extends AbstractPrompt<ListItemIF> {
        private enum Operation {
            NO,
            YES,
            EXIT,
            CANCEL
        }

        private final int startColumn;
        private final ConfirmChoice.ConfirmationValue defaultValue;
        private final ConsolePrompt.UiConfig config;

        private ConfirmPrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                ConfirmChoice confirmChoice,
                ConsolePrompt.UiConfig cfg) {
            super(terminal, header, message, cfg);
            startColumn = message.columnLength();
            defaultValue = confirmChoice.getDefaultConfirmation();
            config = cfg;
        }

        public static ConfirmPrompt getPrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                ConfirmChoice confirmChoice,
                ConsolePrompt.UiConfig cfg) {
            return new ConfirmPrompt(terminal, header, message, confirmChoice, cfg);
        }

        private void bindKeys(KeyMap<Operation> map) {
            String yes = config.resourceBundle().getString("confirmation_yes_key");
            String no = config.resourceBundle().getString("confirmation_no_key");
            map.bind(Operation.YES, yes, yes.toUpperCase());
            map.bind(Operation.NO, no, no.toUpperCase());
            map.bind(Operation.EXIT, "\r");
            map.bind(Operation.CANCEL, KeyMap.esc());
            map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
        }

        public ConfirmResult execute() {
            resetDisplay();
            int row = firstItemRow - 1;
            int column = startColumn;
            KeyMap<Operation> keyMap = new KeyMap<>();
            bindKeys(keyMap);
            StringBuilder buffer = new StringBuilder();
            ConfirmChoice.ConfirmationValue confirm = defaultValue;
            while (true) {
                refreshDisplay(row, column, buffer.toString(), false);
                Operation op = bindingReader.readBinding(keyMap);
                buffer = new StringBuilder();
                switch (op) {
                    case YES:
                        buffer.append(config.resourceBundle().getString("confirmation_yes_answer"));
                        confirm = ConfirmChoice.ConfirmationValue.YES;
                        column = startColumn + 3;
                        break;
                    case NO:
                        buffer.append(config.resourceBundle().getString("confirmation_no_answer"));
                        confirm = ConfirmChoice.ConfirmationValue.NO;
                        column = startColumn + 2;
                        break;
                    case EXIT:
                        if (confirm == null) {
                            break;
                        }
                        return new ConfirmResult(confirm);
                    case CANCEL:
                        return null;
                }
            }
        }
    }

    protected static class InputValuePrompt extends AbstractPrompt<ListItemIF> {
        private enum Operation {
            INSERT,
            BACKSPACE,
            DELETE,
            RIGHT,
            LEFT,
            BEGINNING_OF_LINE,
            END_OF_LINE,
            SELECT_CANDIDATE,
            EXIT,
            CANCEL
        }

        private enum SelectOp {
            FORWARD_ONE_LINE,
            BACKWARD_ONE_LINE,
            EXIT,
            CANCEL
        }

        private final int startColumn;
        private final String defaultValue;
        private final Character mask;
        private final LineReader reader;
        private final Completer completer;

        private InputValuePrompt(
                LineReader reader,
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                InputValue inputValue,
                ConsolePrompt.UiConfig cfg) {
            super(terminal, header, message, cfg);
            this.reader = reader;
            defaultValue = inputValue.getDefaultValue();
            startColumn = message.columnLength();
            mask = inputValue.getMask();
            this.completer = inputValue.getCompleter();
        }

        public static InputValuePrompt getPrompt(
                LineReader reader,
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                InputValue inputValue,
                ConsolePrompt.UiConfig cfg) {
            return new InputValuePrompt(reader, terminal, header, message, inputValue, cfg);
        }

        private void bindKeys(KeyMap<Operation> map) {
            map.setUnicode(Operation.INSERT);
            for (char i = 32; i < KEYMAP_LENGTH; i++) {
                map.bind(Operation.INSERT, Character.toString(i));
            }
            map.bind(Operation.BACKSPACE, del());
            map.bind(Operation.DELETE, ctrl('D'), key(terminal, InfoCmp.Capability.key_dc));
            map.bind(Operation.BACKSPACE, ctrl('H'));
            map.bind(Operation.EXIT, "\r");
            map.bind(Operation.RIGHT, key(terminal, InfoCmp.Capability.key_right));
            map.bind(Operation.LEFT, key(terminal, InfoCmp.Capability.key_left));
            map.bind(Operation.BEGINNING_OF_LINE, ctrl('A'), key(terminal, InfoCmp.Capability.key_home));
            map.bind(Operation.END_OF_LINE, ctrl('E'), key(terminal, InfoCmp.Capability.key_end));
            map.bind(Operation.RIGHT, ctrl('F'));
            map.bind(Operation.LEFT, ctrl('B'));
            map.bind(Operation.SELECT_CANDIDATE, "\t");
            map.bind(Operation.CANCEL, KeyMap.esc());
            map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
        }

        private void bindSelectKeys(KeyMap<SelectOp> map) {
            map.bind(SelectOp.FORWARD_ONE_LINE, "\t", "e", ctrl('E'), key(terminal, InfoCmp.Capability.key_down));
            map.bind(SelectOp.BACKWARD_ONE_LINE, "y", ctrl('Y'), key(terminal, InfoCmp.Capability.key_up));
            map.bind(SelectOp.EXIT, "\r");
            map.bind(SelectOp.CANCEL, KeyMap.esc());
            map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
        }

        public InputResult execute() {
            resetDisplay();
            int row = firstItemRow - 1;
            int column = startColumn;
            List<Candidate> matches = new ArrayList<>();
            KeyMap<Operation> keyMap = new KeyMap<>();
            bindKeys(keyMap);
            StringBuilder displayBuffer = new StringBuilder();
            StringBuilder buffer = new StringBuilder();
            CompletionMatcher completionMatcher = new CompletionMatcherImpl();
            boolean tabCompletion = completer != null && reader != null;
            while (true) {
                boolean displayCandidates = true;
                if (tabCompletion) {
                    List<Candidate> possible = new ArrayList<>();
                    CompletingWord completingWord = new CompletingWord(buffer.toString());
                    completer.complete(reader, completingWord, possible);
                    completionMatcher.compile(config.readerOptions(), false, completingWord, false, 0, null);
                    matches = completionMatcher.matches(possible).stream()
                            .sorted(Comparator.naturalOrder())
                            .collect(Collectors.toList());
                    if (matches.size() > ReaderUtils.getInt(reader, LineReader.MENU_LIST_MAX, 10)) {
                        displayCandidates = false;
                    }
                }
                refreshDisplay(
                        firstItemRow - 1,
                        column,
                        displayBuffer.toString(),
                        row,
                        startColumn,
                        displayCandidates ? matches : new ArrayList<>());
                Operation op = bindingReader.readBinding(keyMap);
                switch (op) {
                    case LEFT:
                        if (column > startColumn) {
                            column--;
                        }
                        break;
                    case RIGHT:
                        if (column < startColumn + displayBuffer.length()) {
                            column++;
                        }
                        break;
                    case INSERT:
                        displayBuffer.insert(
                                column - startColumn, mask == null ? bindingReader.getLastBinding() : mask);
                        buffer.insert(column - startColumn, bindingReader.getLastBinding());
                        column++;
                        break;
                    case BACKSPACE:
                        if (column > startColumn) {
                            displayBuffer.deleteCharAt(column - startColumn - 1);
                            buffer.deleteCharAt(column - startColumn - 1);
                            column--;
                        }
                        break;
                    case DELETE:
                        if (column < startColumn + displayBuffer.length() && column >= startColumn) {
                            displayBuffer.deleteCharAt(column - startColumn);
                            buffer.deleteCharAt(column - startColumn);
                        }
                        break;
                    case BEGINNING_OF_LINE:
                        column = startColumn;
                        break;
                    case END_OF_LINE:
                        column = startColumn + displayBuffer.length();
                        break;
                    case SELECT_CANDIDATE:
                        if (tabCompletion && matches.size() < ReaderUtils.getInt(reader, LineReader.LIST_MAX, 50)) {
                            String selected =
                                    selectCandidate(firstItemRow - 1, buffer.toString(), row + 1, startColumn, matches);
                            resetHeader();
                            if (selected != null) {
                                displayBuffer.delete(0, displayBuffer.length());
                                if (mask == null) {
                                    displayBuffer.append(selected);
                                } else {
                                    for (int i = 0; i < selected.length(); i++) {
                                        displayBuffer.append(mask);
                                    }
                                }
                                buffer.delete(0, displayBuffer.length());
                                buffer.append(selected);
                                column = startColumn + displayBuffer.length();
                            }
                        }
                        break;
                    case EXIT:
                        if (displayBuffer.toString().isEmpty()) {
                            if (mask == null) {
                                displayBuffer.append(defaultValue);
                            } else {
                                for (int i = 0; i < defaultValue.length(); i++) {
                                    displayBuffer.append(mask);
                                }
                            }
                            buffer.append(defaultValue);
                        }
                        return new InputResult(buffer.toString(), displayBuffer.toString());
                    case CANCEL:
                        return null;
                }
            }
        }

        String selectCandidate(int buffRow, String buffer, int row, int column, List<Candidate> candidates) {
            if (candidates.isEmpty()) {
                return buffer;
            } else if (candidates.size() == 1) {
                return candidates.get(0).value();
            }
            KeyMap<SelectOp> keyMap = new KeyMap<>();
            bindSelectKeys(keyMap);
            while (true) {
                String selected = candidates.get(row - buffRow - 1).value();
                refreshDisplay(buffRow, column + selected.length(), selected, row, column, candidates);
                SelectOp op = bindingReader.readBinding(keyMap);
                switch (op) {
                    case FORWARD_ONE_LINE:
                        if (row < buffRow + candidates.size()) {
                            row++;
                        } else {
                            row = buffRow + 1;
                        }
                        break;
                    case BACKWARD_ONE_LINE:
                        if (row > buffRow + 1) {
                            row--;
                        } else {
                            row = buffRow + candidates.size();
                        }
                        break;
                    case EXIT:
                        return selected;
                    case CANCEL:
                        return null;
                }
            }
        }
    }

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
            return new ArrayList<>(Collections.singletonList(word));
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

    private static <T extends ConsoleUIItemIF> int nextRow(int row, int firstItemRow, List<T> items) {
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

    private static <T extends ConsoleUIItemIF> int prevRow(int row, int firstItemRow, List<T> items) {
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

    protected static class ListChoicePrompt<T extends ListItemIF> extends AbstractPrompt<T> {
        private enum Operation {
            FORWARD_ONE_LINE,
            BACKWARD_ONE_LINE,
            INSERT,
            EXIT,
            CANCEL
        }

        private final List<T> items;

        private ListChoicePrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                List<T> listItems,
                int pageSize,
                ConsolePrompt.UiConfig cfg) {
            super(terminal, header, message, listItems, pageSize, cfg);
            items = listItems;
        }

        public static <T extends ListItemIF> ListChoicePrompt<T> getPrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                List<T> listItems,
                int pageSize,
                ConsolePrompt.UiConfig cfg) {
            return new ListChoicePrompt<>(terminal, header, message, listItems, pageSize, cfg);
        }

        private void bindKeys(KeyMap<Operation> map) {
            for (char i = 32; i < KEYMAP_LENGTH; i++) {
                map.bind(Operation.INSERT, Character.toString(i));
            }
            map.bind(Operation.FORWARD_ONE_LINE, "e", ctrl('E'), key(terminal, InfoCmp.Capability.key_down));
            map.bind(Operation.BACKWARD_ONE_LINE, "y", ctrl('Y'), key(terminal, InfoCmp.Capability.key_up));
            map.bind(Operation.EXIT, "\r");
            map.bind(Operation.CANCEL, KeyMap.esc());
            map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
        }

        public ListResult execute() {
            resetDisplay();
            int selectRow = nextRow(firstItemRow - 1, firstItemRow, items);
            KeyMap<Operation> keyMap = new KeyMap<>();
            bindKeys(keyMap);
            while (true) {
                refreshDisplay(selectRow);
                Operation op = bindingReader.readBinding(keyMap);
                switch (op) {
                    case FORWARD_ONE_LINE:
                        selectRow = nextRow(selectRow, firstItemRow, items);
                        break;
                    case BACKWARD_ONE_LINE:
                        selectRow = prevRow(selectRow, firstItemRow, items);
                        break;
                    case INSERT:
                        String ch = bindingReader.getLastBinding();
                        int id = 0;
                        for (ListItemIF cu : items) {
                            if (cu instanceof ChoiceItem) {
                                ChoiceItem ci = (ChoiceItem) cu;
                                if (ci.isSelectable() && ci.getKey().toString().equals(ch)) {
                                    selectRow = firstItemRow + id;
                                    break;
                                }
                            }
                            id++;
                        }
                        break;
                    case EXIT:
                        T listItem = items.get(selectRow - firstItemRow);
                        return new ListResult(listItem.getName());
                    case CANCEL:
                        return null;
                }
            }
        }
    }

    protected static class CheckboxPrompt extends AbstractPrompt<CheckboxItemIF> {
        private enum Operation {
            FORWARD_ONE_LINE,
            BACKWARD_ONE_LINE,
            TOGGLE,
            EXIT,
            CANCEL
        }

        private final List<CheckboxItemIF> items;

        private CheckboxPrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                List<CheckboxItemIF> checkboxItemList,
                int pageSize,
                ConsolePrompt.UiConfig cfg) {
            super(terminal, header, message, checkboxItemList, pageSize, cfg);
            items = checkboxItemList;
        }

        public static CheckboxPrompt getPrompt(
                Terminal terminal,
                List<AttributedString> header,
                AttributedString message,
                List<CheckboxItemIF> checkboxItemList,
                int pageSize,
                ConsolePrompt.UiConfig cfg) {
            return new CheckboxPrompt(terminal, header, message, checkboxItemList, pageSize, cfg);
        }

        private void bindKeys(KeyMap<Operation> map) {
            map.bind(Operation.FORWARD_ONE_LINE, "e", ctrl('E'), key(terminal, InfoCmp.Capability.key_down));
            map.bind(Operation.BACKWARD_ONE_LINE, "y", ctrl('Y'), key(terminal, InfoCmp.Capability.key_up));
            map.bind(Operation.TOGGLE, " ");
            map.bind(Operation.EXIT, "\r");
            map.bind(Operation.CANCEL, KeyMap.esc());
            map.setAmbiguousTimeout(DEFAULT_TIMEOUT_WITH_ESC);
        }

        public CheckboxResult execute() {
            resetDisplay();
            int selectRow = nextRow(firstItemRow - 1, firstItemRow, items);
            Set<String> selected = items.stream()
                    .filter(CheckboxItemIF::isChecked)
                    .flatMap(it -> Stream.of(it.getName()))
                    .collect(Collectors.toSet());
            KeyMap<Operation> keyMap = new KeyMap<>();
            bindKeys(keyMap);
            while (true) {
                refreshDisplay(selectRow, selected);
                Operation op = bindingReader.readBinding(keyMap);
                switch (op) {
                    case FORWARD_ONE_LINE:
                        selectRow = nextRow(selectRow, firstItemRow, items);
                        break;
                    case BACKWARD_ONE_LINE:
                        selectRow = prevRow(selectRow, firstItemRow, items);
                        break;
                    case TOGGLE:
                        if (selected.contains(
                                items.get(selectRow - firstItemRow).getName())) {
                            selected.remove(items.get(selectRow - firstItemRow).getName());
                        } else {
                            selected.add(items.get(selectRow - firstItemRow).getName());
                        }
                        break;
                    case EXIT:
                        return new CheckboxResult(selected);
                    case CANCEL:
                        return null;
                }
            }
        }
    }
}
