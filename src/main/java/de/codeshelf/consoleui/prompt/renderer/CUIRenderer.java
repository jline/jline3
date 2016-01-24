package de.codeshelf.consoleui.prompt.renderer;

import de.codeshelf.consoleui.elements.ConfirmChoice;
import de.codeshelf.consoleui.elements.InputValue;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;
import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;
import de.codeshelf.consoleui.elements.items.impl.ListItem;
import de.codeshelf.consoleui.elements.items.impl.Separator;
import org.fusesource.jansi.Ansi;

import java.util.ResourceBundle;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public class CUIRenderer {
  //private final String cursorSymbol = ansi().fg(Ansi.Color.CYAN).a("\uF078> ").toString();
  private final String cursorSymbol;
  private final String noCursorSpace;

  private final String uncheckedBox;
  private final String checkedBox;
  private final String line;

  private static CUIRenderer renderer = new CUIRenderer();
  private final ResourceBundle resourceBundle;

  public CUIRenderer() {
    checkedBox = "\u25C9 ";
    uncheckedBox = "\u25EF ";
    line = "\u2500─────────────";
    cursorSymbol = ansi().fg(Ansi.Color.CYAN).a("❯ ").toString();
    noCursorSpace = ansi().fg(Ansi.Color.DEFAULT).a("  ").toString();
    resourceBundle = ResourceBundle.getBundle("consoleui_messages");
  }

  public static CUIRenderer getRenderer() {
    return renderer;
  }

  public String render(ConsoleUIItemIF item, boolean withCursor) {
    if (item instanceof CheckboxItem) {
      CheckboxItem checkboxItem = (CheckboxItem) item;
      String prefix;
      if (withCursor) {
        prefix = cursorSymbol;
      } else {
        prefix = noCursorSpace;
      }
      return prefix + ansi()
              .fg(checkboxItem.isEnabled() ? Ansi.Color.GREEN : Ansi.Color.WHITE)
              .a(checkboxItem.isChecked() ? checkedBox : uncheckedBox)
              .reset().a(checkboxItem.getText() +
                      (checkboxItem.isDisabled() ? " (" + checkboxItem.getDisabledText() + ")" : "")
              ).reset().toString();
    }

    if (item instanceof ListItem) {
      ListItem listItem = (ListItem) item;
      if (withCursor) {
        return cursorSymbol + ansi()
                .fg(Ansi.Color.CYAN).a(listItem.getText()
                ).reset().toString();
      } else {
        return noCursorSpace + ansi()
                .fg(Ansi.Color.DEFAULT).a(listItem.getText()
                ).reset().toString();
      }
    }

    if (item instanceof ChoiceItem) {
      ChoiceItem checkboxItem = (ChoiceItem) item;
      if (withCursor) {
        return cursorSymbol + ansi()
                .fg(Ansi.Color.CYAN).a(checkboxItem.getKey() + " - " + checkboxItem.getMessage()).reset().toString();
      } else
        return noCursorSpace + ansi()
                .fg(Ansi.Color.DEFAULT).a(checkboxItem.getKey() + " - " + checkboxItem.getMessage()).reset().toString();
    }

    if (item instanceof Separator) {
      Separator separator = (Separator) item;
      return ansi().fg(Ansi.Color.WHITE).a(
              separator.getMessage() != null ? separator.getMessage() : line).reset().toString();
    }
    return "";
  }

  public String renderOptionalDefaultValue(InputValue inputElement) {
    if (inputElement.getDefaultValue() != null) {
      return " (" + inputElement.getDefaultValue() + ") ";
    }
    return " ";
  }

  public String renderValue(InputValue inputElement) {
    if (inputElement.getValue() != null) {
      return inputElement.getValue();
    }
    return "";
  }

  public String renderConfirmChoiceOptions(ConfirmChoice confirmChoice) {
    if (confirmChoice.getDefaultConfirmation() == ConfirmChoice.ConfirmationValue.YES) {
      return resourceBundle.getString("confirmation_yes_default");
    } else if (confirmChoice.getDefaultConfirmation() == ConfirmChoice.ConfirmationValue.NO) {
      return resourceBundle.getString("confirmation_no_default");
    }
    return resourceBundle.getString("confimation_without_default");
  }
}
