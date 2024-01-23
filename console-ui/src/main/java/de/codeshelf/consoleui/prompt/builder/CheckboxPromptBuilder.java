package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.Checkbox;
import de.codeshelf.consoleui.elements.PageSizeType;
import de.codeshelf.consoleui.elements.items.CheckboxItemIF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 22.01.16.
 */
public class CheckboxPromptBuilder {
  private final PromptBuilder promptBuilder;
  private String name;
  private String message;
  private int pageSize;
  private PageSizeType pageSizeType;
  private List<CheckboxItemIF> itemList;

  public CheckboxPromptBuilder(PromptBuilder promptBuilder) {
    this.promptBuilder = promptBuilder;
    this.pageSize = 10;
    this.pageSizeType = PageSizeType.ABSOLUTE;
    itemList = new ArrayList<CheckboxItemIF>();
  }

  void addItem(CheckboxItemIF checkboxItem) {
    itemList.add(checkboxItem);
  }

  public CheckboxPromptBuilder name(String name) {
    this.name = name;
    if (message == null) {
      message = name;
    }
    return this;
  }

  public CheckboxPromptBuilder message(String message) {
    this.message = message;
    if (name == null) {
      name = message;
    }
    return this;
  }

  public CheckboxPromptBuilder pageSize(int absoluteSize) {
    this.pageSize = absoluteSize;
    this.pageSizeType = PageSizeType.ABSOLUTE;
    return this;
  }

  public CheckboxPromptBuilder relativePageSize(int relativePageSize) {
    this.pageSize = relativePageSize;
    this.pageSizeType = PageSizeType.RELATIVE;
    return this;
  }

  public CheckboxItemBuilder newItem() {
    return new CheckboxItemBuilder(this);
  }

  public CheckboxItemBuilder newItem(String name) {
    CheckboxItemBuilder checkboxItemBuilder = new CheckboxItemBuilder(this);
    return checkboxItemBuilder.name(name);
  }

  public PromptBuilder addPrompt() {
    Checkbox checkbox = new Checkbox(message, name, pageSize, pageSizeType, itemList);
    promptBuilder.addPrompt(checkbox);
    return promptBuilder;
  }

  public CheckboxSeperatorBuilder newSeparator() {
    return new CheckboxSeperatorBuilder(this);
  }

  public CheckboxSeperatorBuilder newSeparator(String text) {
    CheckboxSeperatorBuilder checkboxSeperatorBuilder = new CheckboxSeperatorBuilder(this);
    return checkboxSeperatorBuilder.text(text);
  }


}
