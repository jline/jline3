/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.jline.builtins.Styles;
import org.jline.consoleui.elements.*;
import org.jline.consoleui.elements.items.ConsoleUIItemIF;
import org.jline.consoleui.elements.items.impl.ChoiceItem;
import org.jline.consoleui.prompt.AbstractPrompt.*;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.reader.LineReader;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.*;

/**
 * ConsolePrompt encapsulates the prompting of a list of input questions for the user.
 */
public class ConsolePrompt {
    private final LineReader reader;
    private final Terminal terminal;
    private final UiConfig config;

    /**
     *
     * @param terminal the terminal.
     */
    public ConsolePrompt(Terminal terminal) {
        this(null, terminal, new UiConfig());
    }
    /**
     *
     * @param terminal the terminal.
     * @param config ConsolePrompt cursor pointer and checkbox configuration
     */
    public ConsolePrompt(Terminal terminal, UiConfig config) {
        this(null, terminal, config);
    }
    /**
     *
     * @param reader the lineReader.
     * @param terminal the terminal.
     * @param config ConsolePrompt cursor pointer and checkbox configuration
     */
    public ConsolePrompt(LineReader reader, Terminal terminal, UiConfig config) {
        this.terminal = terminal;
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

    /**
     * Prompt a list of choices (questions). This method takes a list of promptable elements, typically
     * created with {@link PromptBuilder}. Each of the elements is processed and the user entries and
     * answers are filled in to the result map. The result map contains the key of each promptable element
     * and the user entry as an object implementing {@link PromptResultItemIF}.
     *
     * @param promptableElementList the list of questions / prompts to ask the user for.
     * @return a map containing a result for each element of promptableElementList
     * @throws IOException  may be thrown by terminal
     */
    public Map<String, PromptResultItemIF> prompt(List<PromptableElementIF> promptableElementList) throws IOException {
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
     */
    public Map<String, PromptResultItemIF> prompt(
            List<AttributedString> header, List<PromptableElementIF> promptableElementList) throws IOException {
        Attributes attributes = terminal.enterRawMode();
        try {
            terminal.puts(InfoCmp.Capability.enter_ca_mode);
            terminal.puts(InfoCmp.Capability.keypad_xmit);
            terminal.writer().flush();

            Map<String, PromptResultItemIF> resultMap = new HashMap<>();

            for (PromptableElementIF pe : promptableElementList) {
                AttributedStringBuilder message = new AttributedStringBuilder();
                message.style(config.style(".pr")).append("? ");
                message.style(config.style(".me")).append(pe.getMessage()).append(" ");
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.append(message);
                asb.style(AttributedStyle.DEFAULT);
                PromptResultItemIF result;
                if (pe instanceof ListChoice) {
                    ListChoice lc = (ListChoice) pe;
                    result = ListChoicePrompt.getPrompt(
                                    terminal,
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
                    result = InputValuePrompt.getPrompt(reader, terminal, header, asb.toAttributedString(), ip, config)
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
                                        terminal, header, asb.toAttributedString(), ec, config)
                                .execute();
                    } catch (ExpandableChoiceException e) {
                        result = ListChoicePrompt.getPrompt(
                                        terminal, header, message.toAttributedString(), ec.getChoiceItems(), 10, config)
                                .execute();
                    }
                } else if (pe instanceof Checkbox) {
                    Checkbox cb = (Checkbox) pe;
                    result = CheckboxPrompt.getPrompt(
                                    terminal,
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
                    result = ConfirmPrompt.getPrompt(terminal, header, asb.toAttributedString(), cc, config)
                            .execute();
                } else {
                    throw new IllegalArgumentException("wrong type of promptable element");
                }
                String resp = result.getResult();
                if (result instanceof ConfirmResult) {
                    ConfirmResult cr = (ConfirmResult) result;
                    if (cr.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
                        resp = config.resourceBundle().getString("confirmation_yes_answer");
                    } else {
                        resp = config.resourceBundle().getString("confirmation_no_answer");
                    }
                }
                message.style(config.style(".an")).append(resp);
                header.add(message.toAttributedString());
                resultMap.put(pe.getName(), result);
            }
            return resultMap;
        } finally {
            terminal.setAttributes(attributes);
            terminal.puts(InfoCmp.Capability.exit_ca_mode);
            terminal.puts(InfoCmp.Capability.keypad_local);
            terminal.writer().flush();
            for (AttributedString as : header) {
                as.println(terminal);
            }
            terminal.writer().flush();
        }
    }

    private int computePageSize(Terminal terminal, int pageSize, PageSizeType sizeType) {
        int rows = terminal.getHeight();
        return sizeType == PageSizeType.ABSOLUTE ? Math.min(rows, pageSize) : (rows * pageSize) / 100;
    }

    /**
     * Creates a {@link PromptBuilder}.
     *
     * @return a new prompt builder object.
     */
    public PromptBuilder getPromptBuilder() {
        return new PromptBuilder();
    }

    /**
     * ConsoleUI configuration: colors, cursor pointer and selected/unselected/unavailable boxes.
     * ConsoleUI colors are configurable using UI_COLORS environment variable
     */
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

        protected void setReaderOptions(Map<LineReader.Option, Boolean> readerOptions) {
            this.readerOptions = readerOptions;
        }

        public Map<LineReader.Option, Boolean> readerOptions() {
            return readerOptions;
        }

        private static StyleResolver resolver(String style) {
            Map<String, String> colors = Arrays.stream(style.split(":"))
                    .collect(Collectors.toMap(
                            s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1)));
            return new StyleResolver(colors::get);
        }
    }
}
