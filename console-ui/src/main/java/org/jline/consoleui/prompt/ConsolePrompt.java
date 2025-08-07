/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

import java.io.IOError;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jline.builtins.Styles;
import org.jline.consoleui.elements.*;
import org.jline.consoleui.elements.items.ConsoleUIItemIF;
import org.jline.consoleui.elements.items.impl.ChoiceItem;
import org.jline.consoleui.prompt.AbstractPrompt.*;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.*;

/**
 * ConsolePrompt encapsulates the prompting of a list of input questions for the user.
 *
 * @deprecated This class is deprecated as of JLine 4.0.0. Please use the new
 *             {@code jline-prompt} module instead, which provides a cleaner,
 *             interface-based API. Use {@link org.jline.prompt.PrompterFactory#create(org.jline.terminal.Terminal)}
 *             to create a {@link org.jline.prompt.Prompter} instance.
 *
 *             <p>Migration example:</p>
 *             <pre>{@code
 *             // Old API
 *             ConsolePrompt prompt = new ConsolePrompt(terminal);
 *             PromptBuilder builder = prompt.getPromptBuilder();
 *
 *             // New API
 *             Prompter prompter = PrompterFactory.create(terminal);
 *             PromptBuilder builder = prompter.newBuilder();
 *             }</pre>
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class ConsolePrompt {
    protected final LineReader reader;
    protected final Terminal terminal;
    protected final Display display;
    protected final UiConfig config;
    protected Attributes attributes;
    protected List<AttributedString> header = new ArrayList<>();

    /**
     * @param terminal the terminal.
     * @deprecated Use {@link org.jline.prompt.PrompterFactory#create(org.jline.terminal.Terminal)} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public ConsolePrompt(Terminal terminal) {
        this(null, terminal, new UiConfig());
    }

    /**
     * @param terminal the terminal.
     * @param config ConsolePrompt cursor pointer and checkbox configuration
     * @deprecated Use {@link org.jline.prompt.PrompterFactory#create(org.jline.terminal.Terminal, org.jline.prompt.PrompterConfig)} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public ConsolePrompt(Terminal terminal, UiConfig config) {
        this(null, terminal, config);
    }

    /**
     * @param reader the lineReader.
     * @param terminal the terminal.
     * @param config ConsolePrompt cursor pointer and checkbox configuration
     * @deprecated Use {@link org.jline.prompt.PrompterFactory#create(org.jline.reader.LineReader, org.jline.terminal.Terminal, org.jline.prompt.PrompterConfig)} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public ConsolePrompt(LineReader reader, Terminal terminal, UiConfig config) {
        this.terminal = terminal;
        this.display = new Display(terminal, false);
        this.config = config;
        this.reader = reader;
        if (reader != null) {
            Map<LineReader.Option, Boolean> options = new HashMap<>();
            for (LineReader.Option option : LineReader.Option.values()) {
                options.put(option, reader.isSet(option));
            }
            config.setReaderOptions(options);
        }
    }

    protected void open() {
        if (!terminalInRawMode()) {
            attributes = terminal.enterRawMode();
            terminal.puts(InfoCmp.Capability.keypad_xmit);
            terminal.writer().flush();
        }
    }

    protected void close() {
        if (terminalInRawMode()) {
            int cursor = (terminal.getWidth() + 1) * header.size();
            display.update(header, cursor);
            terminal.setAttributes(attributes);
            terminal.puts(InfoCmp.Capability.keypad_local);
            terminal.writer().println();
            terminal.writer().flush();
            attributes = null;
        }
    }

    private boolean terminalInRawMode() {
        return attributes != null;
    }

    /**
     * Prompt a list of choices (questions). This method takes a list of promptable elements, typically
     * created with {@link PromptBuilder}. Each of the elements is processed and the user entries and
     * answers are filled in to the result map. The result map contains the key of each promptable element
     * and the user entry as an object implementing {@link PromptResultItemIF}.
     *
     * @param promptableElementList the list of questions / prompts to ask the user for.
     * @return a map containing a result for each element of promptableElementList
     * @throws IOException  may be thrown by terminal
     * @throws UserInterruptException if user interrupt handling is enabled and the user types the interrupt character (ctrl-C)
     * @deprecated Use {@link org.jline.prompt.Prompter#prompt(java.util.List, java.util.List)} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public Map<String, PromptResultItemIF> prompt(List<PromptableElementIF> promptableElementList)
            throws IOException, UserInterruptException {
        return prompt(new ArrayList<>(), promptableElementList);
    }

    /**
     * Prompt a list of choices (questions). This method takes a list of promptable elements, typically
     * created with {@link PromptBuilder}. Each of the elements is processed and the user entries and
     * answers are filled in to the result map. The result map contains the key of each promptable element
     * and the user entry as an object implementing {@link PromptResultItemIF}.
     *
     * @param header info to be displayed before first prompt.
     * @param promptableElementList the list of questions / prompts to ask the user for.
     * @return a map containing a result for each element of promptableElementList
     * @throws IOException  may be thrown by terminal
     * @throws UserInterruptException if user interrupt handling is enabled and the user types the interrupt character (ctrl-C)
     * @deprecated Use {@link org.jline.prompt.Prompter#prompt(java.util.List, java.util.List)} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public Map<String, PromptResultItemIF> prompt(
            List<AttributedString> header, List<PromptableElementIF> promptableElementList)
            throws IOException, UserInterruptException {
        try {
            open();
            Map<String, PromptResultItemIF> resultMap = new HashMap<>();
            prompt(header, promptableElementList, resultMap);
            return removeNoResults(resultMap);
        } finally {
            close();
        }
    }

    /**
     * Prompt a list of choices (questions). This method takes a function that given a map of interim results
     * returns a list of promptable elements (typically created with {@link PromptBuilder}). Each list is then
     * passed to {@link #prompt(List, List, Map)} and the result added to the map of interim results.
     * The function is then called again with the updated map of results until the function returns null.
     * The final result map contains the key of each promptable element and the user entry as an object
     * implementing {@link PromptResultItemIF}.
     *
     * @param promptableElementLists a function returning lists of questions / prompts to ask the user for.
     * @throws IOException  may be thrown by terminal
     * @deprecated Use {@link org.jline.prompt.Prompter#prompt(java.util.List, java.util.function.Function)} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public Map<String, PromptResultItemIF> prompt(
            Function<Map<String, PromptResultItemIF>, List<PromptableElementIF>> promptableElementLists)
            throws IOException {
        return prompt(new ArrayList<>(), promptableElementLists);
    }

    /**
     * Prompt a list of choices (questions). This method takes a function that given a map of interim results
     * returns a list of promptable elements (typically created with {@link PromptBuilder}). Each list is then
     * passed to {@link #prompt(List, List, Map)} and the result added to the map of interim results.
     * The function is then called again with the updated map of results until the function returns null.
     * The final result map contains the key of each promptable element and the user entry as an object
     * implementing {@link PromptResultItemIF}.
     *
     * @param headerIn info to be displayed before first prompt.
     * @param promptableElementLists a function returning lists of questions / prompts to ask the user for.
     * @throws IOException  may be thrown by terminal
     * @deprecated Use {@link org.jline.prompt.Prompter#prompt(java.util.List, java.util.function.Function)} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public Map<String, PromptResultItemIF> prompt(
            List<AttributedString> headerIn,
            Function<Map<String, PromptResultItemIF>, List<PromptableElementIF>> promptableElementLists)
            throws IOException {
        Map<String, PromptResultItemIF> resultMap = new HashMap<>();
        Deque<List<PromptableElementIF>> prevLists = new ArrayDeque<>();
        Deque<Map<String, PromptResultItemIF>> prevResults = new ArrayDeque<>();
        boolean cancellable = config.cancellableFirstPrompt();
        try {
            open();
            // Get our first list of prompts
            List<PromptableElementIF> peList = promptableElementLists.apply(new HashMap<>());
            Map<String, PromptResultItemIF> peResult = new HashMap<>();
            while (peList != null) {
                // Second and later prompts should always be cancellable
                config.setCancellableFirstPrompt(!prevLists.isEmpty() || cancellable);
                // Prompt the user
                prompt(headerIn, peList, peResult);
                if (peResult.isEmpty()) {
                    // The prompt was cancelled by the user, so let's go back to the
                    // previous list of prompts and its results (if any)
                    peList = prevLists.pollFirst();
                    peResult = prevResults.pollFirst();
                    if (peResult != null) {
                        // Remove the results of the previous prompt from the main result map
                        peResult.forEach((k, v) -> resultMap.remove(k));
                        headerIn.remove(headerIn.size() - 1);
                    }
                } else {
                    // We remember the list of prompts and their results
                    prevLists.push(peList);
                    prevResults.push(peResult);
                    // Add the results to the main result map
                    resultMap.putAll(peResult);
                    // And we get our next list of prompts (if any)
                    peList = promptableElementLists.apply(resultMap);
                    peResult = new HashMap<>();
                }
            }
            return removeNoResults(resultMap);
        } finally {
            close();
            // Restore the original state of cancellable
            config.setCancellableFirstPrompt(cancellable);
        }
    }

    /**
     * Prompt a list of choices (questions). This method takes a list of promptable elements, typically
     * created with {@link PromptBuilder}. Each of the elements is processed and the user entries and
     * answers are filled in to the result map. The result map contains the key of each promptable element
     * and the user entry as an object implementing {@link PromptResultItemIF}.
     *
     * @param headerIn info to be displayed before first prompt.
     * @param promptableElementList the list of questions / prompts to ask the user for.
     * @param resultMap a map containing a result for each element of promptableElementList
     * @throws IOException  may be thrown by terminal
     */
    protected void prompt(
            List<AttributedString> headerIn,
            List<PromptableElementIF> promptableElementList,
            Map<String, PromptResultItemIF> resultMap)
            throws IOException {
        if (!terminalInRawMode()) {
            throw new IllegalStateException("Terminal is not in raw mode! Maybe ConsolePrompt is closed?");
        }
        this.header = headerIn;

        boolean backward = false;
        for (int i = resultMap.isEmpty() ? 0 : resultMap.size() - 1; i < promptableElementList.size(); i++) {
            PromptableElementIF pe = promptableElementList.get(i);
            try {
                if (backward) {
                    removePreviousResult(pe);
                    backward = false;
                }
                PromptResultItemIF oldResult = resultMap.get(pe.getName());
                PromptResultItemIF result = promptElement(header, pe, oldResult);
                if (result == null) {
                    // Prompt was cancelled by the user
                    if (i > 0) {
                        // Go back to previous prompt
                        i -= 2;
                        backward = true;
                        continue;
                    } else {
                        if (config.cancellableFirstPrompt()) {
                            resultMap.clear();
                            return;
                        } else {
                            // Repeat current prompt
                            i -= 1;
                            continue;
                        }
                    }
                }
                AttributedStringBuilder message;
                if (pe instanceof Text) {
                    Text te = (Text) pe;
                    header.addAll(te.getLines());
                } else {
                    String resp = result.getDisplayResult();
                    if (result instanceof ConfirmResult) {
                        ConfirmResult cr = (ConfirmResult) result;
                        if (cr.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
                            resp = config.resourceBundle().getString("confirmation_yes_answer");
                        } else {
                            resp = config.resourceBundle().getString("confirmation_no_answer");
                        }
                    }
                    message = createMessage(pe.getMessage(), resp);
                    header.add(message.toAttributedString());
                }
                resultMap.put(pe.getName(), result);
            } catch (IOError e) {
                if (e.getCause() instanceof InterruptedIOException) {
                    throw new UserInterruptException(e.getCause());
                } else {
                    throw e;
                }
            }
        }
    }

    protected PromptResultItemIF promptElement(
            List<AttributedString> header, PromptableElementIF pe, PromptResultItemIF oldResult) {
        AttributedStringBuilder message = createMessage(pe.getMessage(), null);
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(message);
        asb.style(AttributedStyle.DEFAULT);
        PromptResultItemIF result;
        if (pe instanceof ListChoice) {
            ListChoice lc = (ListChoice) pe;
            result = ListChoicePrompt.getPrompt(
                            terminal,
                            display,
                            header,
                            asb.toAttributedString(),
                            lc.getListItemList(),
                            computePageSize(terminal, lc.getPageSize(), lc.getPageSizeType()),
                            config)
                    .execute();
        } else if (pe instanceof InputValue) {
            InputValue ip = (InputValue) pe;
            if (ip.getDefaultValue() != null) {
                asb.append("(").append(ip.getDefaultValue()).append(") ");
            }
            result = InputValuePrompt.getPrompt(reader, terminal, display, header, asb.toAttributedString(), ip, config)
                    .execute();
        } else if (pe instanceof ExpandableChoice) {
            ExpandableChoice ec = (ExpandableChoice) pe;
            asb.append("(");
            for (ConsoleUIItemIF item : ec.getChoiceItems()) {
                if (item instanceof ChoiceItem) {
                    ChoiceItem ci = (ChoiceItem) item;
                    if (ci.isSelectable()) {
                        asb.append(ci.isDefaultChoice() ? Character.toUpperCase(ci.getKey()) : ci.getKey());
                    }
                }
            }
            asb.append("h) ");
            try {
                result = ExpandableChoicePrompt.getPrompt(
                                terminal, display, header, asb.toAttributedString(), ec, config)
                        .execute();
            } catch (ExpandableChoiceException e) {
                result = ListChoicePrompt.getPrompt(
                                terminal,
                                display,
                                header,
                                message.toAttributedString(),
                                ec.getChoiceItems(),
                                10,
                                config)
                        .execute();
            }
        } else if (pe instanceof Checkbox) {
            Checkbox cb = (Checkbox) pe;
            result = CheckboxPrompt.getPrompt(
                            terminal,
                            display,
                            header,
                            message.toAttributedString(),
                            cb.getCheckboxItemList(),
                            computePageSize(terminal, cb.getPageSize(), cb.getPageSizeType()),
                            config)
                    .execute();
        } else if (pe instanceof ConfirmChoice) {
            ConfirmChoice cc = (ConfirmChoice) pe;
            if (cc.getDefaultConfirmation() == null) {
                asb.append(config.resourceBundle().getString("confirmation_without_default"));
            } else if (cc.getDefaultConfirmation() == ConfirmChoice.ConfirmationValue.YES) {
                asb.append(config.resourceBundle().getString("confirmation_yes_default"));
            } else {
                asb.append(config.resourceBundle().getString("confirmation_no_default"));
            }
            asb.append(" ");
            result = ConfirmPrompt.getPrompt(terminal, display, header, asb.toAttributedString(), cc, config)
                    .execute();
        } else if (pe instanceof Text) {
            Text te = (Text) pe;
            result = oldResult == null ? NoResult.INSTANCE : null;
        } else {
            throw new IllegalArgumentException("wrong type of promptable element");
        }
        return result;
    }

    protected AttributedStringBuilder createMessage(String message, String response) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(config.style(".pr")).append("? ");
        asb.style(config.style(".me")).append(message).append(" ");
        if (response != null) {
            asb.style(config.style(".an")).append(response);
        }
        return asb;
    }

    /**
     * @deprecated This method is deprecated along with the ConsolePrompt class. Use the new jline-prompt module instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public static int computePageSize(Terminal terminal, int pageSize, PageSizeType sizeType) {
        int rows = terminal.getHeight();
        return sizeType == PageSizeType.ABSOLUTE ? Math.min(rows, pageSize) : (rows * pageSize) / 100;
    }

    private void removePreviousResult(PromptableElementIF pe) {
        if (pe instanceof Text) {
            Text te = (Text) pe;
            for (int i = 0; i < te.getLines().size(); i++) {
                header.remove(header.size() - 1);
            }
        } else {
            header.remove(header.size() - 1);
        }
    }

    private Map<String, PromptResultItemIF> removeNoResults(Map<String, PromptResultItemIF> resultMap) {
        return resultMap.entrySet().stream()
                .filter(e -> !(e.getValue() instanceof NoResult))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Creates a {@link PromptBuilder}.
     *
     * @return a new prompt builder object.
     * @deprecated Use {@link org.jline.prompt.Prompter#newBuilder()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public PromptBuilder getPromptBuilder() {
        return new PromptBuilder();
    }

    /**
     * ConsoleUI configuration: colors, cursor pointer and selected/unselected/unavailable boxes.
     * ConsoleUI colors are configurable using UI_COLORS environment variable
     *
     * @deprecated This class is deprecated along with ConsolePrompt. Use {@link org.jline.prompt.PrompterConfig} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public static class UiConfig {
        static final String DEFAULT_UI_COLORS = "cu=36:be=32:bd=37:pr=32:me=1:an=36:se=36:cb=100";
        static final String UI_COLORS = "UI_COLORS";
        private final AttributedString indicator;
        private final AttributedString uncheckedBox;
        private final AttributedString checkedBox;
        private final AttributedString unavailable;
        private final StyleResolver resolver;
        private final ResourceBundle resourceBundle;
        private Map<LineReader.Option, Boolean> readerOptions = new HashMap<>();
        private boolean cancellableFirstPrompt;

        public UiConfig() {
            this(null, null, null, null);
        }

        public UiConfig(String indicator, String uncheckedBox, String checkedBox, String unavailable) {
            String uc = System.getenv(UI_COLORS);
            String uiColors = uc != null && Styles.isStylePattern(uc) ? uc : DEFAULT_UI_COLORS;
            this.resolver = resolver(uiColors);
            this.indicator = toAttributedString(resolver, (indicator != null ? indicator : ">"), ".cu");
            this.uncheckedBox = toAttributedString(resolver, (uncheckedBox != null ? uncheckedBox : " "), ".be");
            this.checkedBox = toAttributedString(resolver, (checkedBox != null ? checkedBox : "x "), ".be");
            this.unavailable = toAttributedString(resolver, (unavailable != null ? unavailable : "- "), ".bd");
            this.resourceBundle = ResourceBundle.getBundle("consoleui_messages");
        }

        private static AttributedString toAttributedString(StyleResolver resolver, String string, String styleKey) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.style(resolver.resolve(styleKey));
            asb.append(string);
            return asb.toAttributedString();
        }

        public AttributedString indicator() {
            return indicator;
        }

        public AttributedString uncheckedBox() {
            return uncheckedBox;
        }

        public AttributedString checkedBox() {
            return checkedBox;
        }

        public AttributedString unavailable() {
            return unavailable;
        }

        public AttributedStyle style(String key) {
            return resolver.resolve(key);
        }

        public ResourceBundle resourceBundle() {
            return resourceBundle;
        }

        public boolean cancellableFirstPrompt() {
            return cancellableFirstPrompt;
        }

        public void setCancellableFirstPrompt(boolean cancellableFirstPrompt) {
            this.cancellableFirstPrompt = cancellableFirstPrompt;
        }

        protected void setReaderOptions(Map<LineReader.Option, Boolean> readerOptions) {
            this.readerOptions = readerOptions;
        }

        public Map<LineReader.Option, Boolean> readerOptions() {
            return readerOptions;
        }

        public static StyleResolver resolver(String style) {
            Map<String, String> colors = Arrays.stream(style.split(":"))
                    .collect(Collectors.toMap(
                            s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1)));
            return new StyleResolver(colors::get);
        }
    }
}
