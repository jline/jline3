package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.Checkbox;
import de.codeshelf.consoleui.elements.ConfirmChoice;
import de.codeshelf.consoleui.elements.ExpandableChoice;
import de.codeshelf.consoleui.elements.InputValue;
import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ChoiceItemIF;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;
import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;
import de.codeshelf.consoleui.elements.items.impl.Separator;
import de.codeshelf.consoleui.prompt.ConsolePrompt.UiConfig;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jline.keymap.KeyMap.*;

/**
 * Classes for all prompt implementations.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public abstract class AbstractPrompt <T extends ConsoleUIItemIF> {
  protected final Terminal terminal;
  protected final BindingReader bindingReader;
  private final List<AttributedString> header;
  private final AttributedString message;
  protected final List<T> items;
  protected final int firstItemRow;
  private final Size size = new Size();
  private final ConsolePrompt.UiConfig config;
  private Display display;

  public AbstractPrompt(Terminal terminal, List<AttributedString> header, AttributedString message, ConsolePrompt.UiConfig cfg) {
    this(terminal, header, message, new ArrayList<>(), cfg);
  }

  public  AbstractPrompt(Terminal terminal, List<AttributedString> header, AttributedString message, List<T> items
      , ConsolePrompt.UiConfig cfg) {
    this.terminal = terminal;
    this.bindingReader = new BindingReader(terminal.reader());
    this.header = header;
    this.message = message;
    this.items = items;
    this.firstItemRow = header.size() + 1;
    this.config = cfg;
  }

  protected void resetDisplay() {
    display = new Display(terminal, true);
    size.copy(terminal.getSize());
    display.clear();
    display.reset();
  }

  protected void refreshDisplay(int row) {
    refreshDisplay(row, 0, null);
  }

  protected void refreshDisplay(int row, Set<String> selected) {
    display.resize(size.getRows(), size.getColumns());
    display.reset();
    display.update(displayLines(row, selected), size.cursorPos(firstItemRow + items.size(), 0));
  }

  protected void refreshDisplay(int row, int column, String buffer, boolean newline) {
    display.resize(size.getRows(), size.getColumns());
    AttributedStringBuilder asb = new AttributedStringBuilder();
    int crow = column == 0 ? firstItemRow + items.size() : row;
    if (buffer != null) {
      if (newline && !buffer.isEmpty()) {
        asb.style(config.style(".pr")).append(">> ");
      }
      asb.style(AttributedStyle.DEFAULT).append(buffer);
    }
    display.update(displayLines(row, asb.toAttributedString(), newline), size.cursorPos(crow, column));
  }

  protected void refreshDisplay(int row, int column, String buffer) {
    refreshDisplay(row, column, buffer, false);
  }

  private List<AttributedString> displayLines(int cursorRow, Set<String> selected) {
    List<AttributedString> out = new ArrayList<>(header);
    int i = firstItemRow;
    AttributedStringBuilder asb = new AttributedStringBuilder();
    asb.append(message);
    out.add(asb.toAttributedString());
    for (ConsoleUIItemIF s : items) {
      asb = new AttributedStringBuilder();
      if (s.isSelectable()) {
        asb = i == cursorRow ? asb.append(config.indicator()).style(AttributedStyle.DEFAULT).append(" ")
            : fillIndicatorSpace(asb).append(" ");
        asb = selected.contains(s.getName()) ? asb.append(config.checkedBox())
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
      out.add(asb.toAttributedString());
      i++;
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

  private List<AttributedString> displayLines(int cursorRow, AttributedString buffer, boolean newline) {
    List<AttributedString> out = new ArrayList<>(header);
    int i = firstItemRow;
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
    for (ConsoleUIItemIF s : items) {
      asb = new AttributedStringBuilder();
      String key = s instanceof ChoiceItem ? ((ChoiceItem)s).getKey() + " - " : "";
      if (i == cursorRow) {
        out.add(asb.append(config.indicator()).style(config.style(".se")).append(" ").append(key)
            .append(s.getText()).toAttributedString());
      } else if (!(s instanceof Separator)) {
        fillIndicatorSpace(asb);
        out.add(asb.append(" ").append(key).append(s.getText()).toAttributedString());
      } else {
        out.add(asb.append(s.getText()).toAttributedString());
      }
      i++;
    }
    return out;
  }

  protected static class ExpandableChoicePrompt extends AbstractPrompt<ListItemIF> {
    private enum Operation {INSERT, EXIT}
    private final int startColumn;
    private final List<ChoiceItemIF> items;
    private final UiConfig config;

    private ExpandableChoicePrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , ExpandableChoice expandableChoice, UiConfig cfg) {
      super(terminal, header, message, cfg);
      startColumn = message.columnLength();
      items = expandableChoice.getChoiceItems();
      config = cfg;
    }

    public static ExpandableChoicePrompt getPrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , ExpandableChoice expandableChoice, UiConfig cfg) {
      return new ExpandableChoicePrompt(terminal, header, message, expandableChoice, cfg);
    }

    private void bindKeys(KeyMap<Operation> map) {
      for (char i = 32; i < KEYMAP_LENGTH; i++) {
        map.bind(Operation.INSERT, Character.toString(i));
      }
      map.bind(Operation.EXIT,"\r");
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
        }
      }
    }

  }

  @SuppressWarnings("serial")
  protected static class ExpandableChoiceException extends RuntimeException {
  }

  protected static class ConfirmPrompt extends AbstractPrompt<ListItemIF> {
    private enum Operation {NO, YES, EXIT}
    private final int startColumn;
    private final ConfirmChoice.ConfirmationValue defaultValue;
    private final UiConfig config;

    private ConfirmPrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , ConfirmChoice confirmChoice, UiConfig cfg) {
      super(terminal, header, message, cfg);
      startColumn = message.columnLength();
      defaultValue = confirmChoice.getDefaultConfirmation();
      config = cfg;
    }

    public static ConfirmPrompt getPrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , ConfirmChoice confirmChoice, UiConfig cfg) {
      return new ConfirmPrompt(terminal, header, message, confirmChoice, cfg);
    }

    private void bindKeys(KeyMap<Operation> map) {
      String yes = config.resourceBundle().getString("confirmation_yes_key");
      String no = config.resourceBundle().getString("confirmation_no_key");
      map.bind(Operation.YES, yes, yes.toUpperCase());
      map.bind(Operation.NO, no, no.toUpperCase());
      map.bind(Operation.EXIT,"\r");
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
        refreshDisplay(row, column, buffer.toString());
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
        }
      }
    }

  }

  protected static class InputValuePrompt extends AbstractPrompt<ListItemIF> {
    private enum Operation {INSERT, BACKSPACE, DELETE, RIGHT, LEFT, BEGINNING_OF_LINE, END_OF_LINE, EXIT}
    private final int startColumn;
    private final String defaultValue;
    private final Character mask;

    private InputValuePrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , InputValue inputValue, UiConfig cfg) {
      super(terminal, header, message, cfg);
      defaultValue = inputValue.getDefaultValue();
      startColumn = message.columnLength();
      mask = inputValue.getMask();
    }

    public static InputValuePrompt getPrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , InputValue inputValue, UiConfig cfg) {
      return new InputValuePrompt(terminal, header, message, inputValue, cfg);
    }

    private void bindKeys(KeyMap<Operation> map) {
      map.setUnicode(Operation.INSERT);
      for (char i = 32; i < KEYMAP_LENGTH; i++) {
        map.bind(Operation.INSERT, Character.toString(i));
      }
      map.bind(Operation.BACKSPACE, del());
      map.bind(Operation.DELETE, ctrl('D'), key(terminal, InfoCmp.Capability.key_dc));
      map.bind(Operation.BACKSPACE, ctrl('H'));
      map.bind(Operation.EXIT,"\r");
      map.bind(Operation.RIGHT, key(terminal, InfoCmp.Capability.key_right));
      map.bind(Operation.LEFT, key(terminal, InfoCmp.Capability.key_left));
      map.bind(Operation.BEGINNING_OF_LINE, ctrl('A'), key(terminal, InfoCmp.Capability.key_home));
      map.bind(Operation.END_OF_LINE, ctrl('E'), key(terminal, InfoCmp.Capability.key_end));
      map.bind(Operation.RIGHT, ctrl('F'));
      map.bind(Operation.LEFT, ctrl('B'));
    }

    public InputResult execute() {
      resetDisplay();
      int row = firstItemRow - 1;
      int column = startColumn;
      KeyMap<Operation> keyMap = new KeyMap<>();
      bindKeys(keyMap);
      StringBuilder buffer = new StringBuilder();
      while (true) {
        refreshDisplay(row, column, buffer.toString());
        Operation op = bindingReader.readBinding(keyMap);
        switch (op) {
          case LEFT:
            if (column > startColumn) {
              column--;
            }
            break;
          case RIGHT:
            if (column < startColumn + buffer.length()) {
              column++;
            }
            break;
          case INSERT:
            buffer.insert(column - startColumn, mask == null ? bindingReader.getLastBinding() : mask);
            column++;
            break;
          case BACKSPACE:
            if (column > startColumn) {
              buffer.deleteCharAt(column - startColumn - 1);
            }
            column--;
            break;
          case DELETE:
            if (column < startColumn + buffer.length() && column >= startColumn) {
              buffer.deleteCharAt(column - startColumn);
            }
            break;
          case BEGINNING_OF_LINE:
            column = startColumn;
            break;
          case END_OF_LINE:
            column = startColumn + buffer.length();
            break;
          case EXIT:
            if (buffer.toString().isEmpty()) {
              buffer.append(defaultValue);
            }
            return new InputResult(buffer.toString());
        }
      }
    }

  }

  private static <T extends ConsoleUIItemIF> int nextRow(int row, int firstItemRow, List<T> items) {
    int itemsSize = items.size();
    int next;
    for (next = row + 1; next - firstItemRow < itemsSize && !items.get(next - firstItemRow).isSelectable(); next++) {
    }
    if (next - firstItemRow >= itemsSize) {
      for (next = firstItemRow; next - firstItemRow < itemsSize && !items.get(next - firstItemRow).isSelectable(); next++) {
      }
    }
    return next;
  }
  private static <T extends ConsoleUIItemIF> int  prevRow(int row, int firstItemRow,  List<T> items) {
    int itemsSize = items.size();
    int prev;
    for (prev = row - 1; prev - firstItemRow >= 0 && !items.get(prev - firstItemRow).isSelectable(); prev--) {
    }
    if (prev - firstItemRow < 0) {
      for (prev = firstItemRow + itemsSize - 1; prev - firstItemRow >= 0 && !items.get(prev - firstItemRow).isSelectable(); prev--) {
      }
    }
    return prev;
  }

  protected static class ListChoicePrompt <T extends ListItemIF> extends AbstractPrompt<T> {
    private enum Operation {FORWARD_ONE_LINE, BACKWARD_ONE_LINE, INSERT, EXIT}
    private final List<T> items;

    private ListChoicePrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , List<T> listItems, UiConfig cfg) {
      super(terminal, header, message, listItems, cfg);
      items = listItems;
    }

    public static <T extends ListItemIF> ListChoicePrompt<T> getPrompt(Terminal terminal, List<AttributedString> header
        , AttributedString message, List<T> listItems, UiConfig cfg) {
      return new ListChoicePrompt<>(terminal, header, message, listItems, cfg);
    }

    private void bindKeys(KeyMap<Operation> map) {
      for (char i = 32; i < KEYMAP_LENGTH; i++) {
        map.bind(Operation.INSERT, Character.toString(i));
      }
      map.bind(Operation.FORWARD_ONE_LINE, "e", ctrl('E'), key(terminal, InfoCmp.Capability.key_down));
      map.bind(Operation.BACKWARD_ONE_LINE, "y", ctrl('Y'), key(terminal, InfoCmp.Capability.key_up));
      map.bind(Operation.EXIT,"\r");
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
        }
      }
    }
  }

  protected static class CheckboxPrompt extends AbstractPrompt<CheckboxItemIF> {
    private enum Operation {FORWARD_ONE_LINE, BACKWARD_ONE_LINE, TOGGLE, EXIT}

    private final List<CheckboxItemIF> items;

    private CheckboxPrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , Checkbox checkbox, UiConfig cfg) {
      super(terminal, header, message, checkbox.getCheckboxItemList(), cfg);
      items = checkbox.getCheckboxItemList();
    }

    public static CheckboxPrompt getPrompt(Terminal terminal, List<AttributedString> header, AttributedString message
        , Checkbox checkbox, UiConfig cfg) {
      return new CheckboxPrompt(terminal, header, message, checkbox, cfg);
    }

    private void bindKeys(KeyMap<Operation> map) {
      map.bind(Operation.FORWARD_ONE_LINE, "e", ctrl('E'), key(terminal, InfoCmp.Capability.key_down));
      map.bind(Operation.BACKWARD_ONE_LINE, "y", ctrl('Y'), key(terminal, InfoCmp.Capability.key_up));
      map.bind(Operation.TOGGLE," ");
      map.bind(Operation.EXIT,"\r");
    }

    public CheckboxResult execute() {
      resetDisplay();
      int selectRow = nextRow(firstItemRow - 1, firstItemRow, items);
      Set<String> selected = items.stream().filter(CheckboxItemIF::isChecked)
          .flatMap(it -> Stream.of(it.getName())).collect(Collectors.toSet());
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
            if (selected.contains(items.get(selectRow - firstItemRow).getName())) {
              selected.remove(items.get(selectRow - firstItemRow).getName());
            } else {
              selected.add(items.get(selectRow - firstItemRow).getName());
            }
            break;
          case EXIT:
            return new CheckboxResult(selected);
        }
      }
    }
  }

}
