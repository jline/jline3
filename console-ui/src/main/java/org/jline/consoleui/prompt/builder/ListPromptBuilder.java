package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.ListChoice;
import org.jline.consoleui.elements.items.ListItemIF;
import org.jline.consoleui.elements.items.impl.ListItem;
import org.jline.consoleui.elements.PageSizeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 22.01.16.
 */
public class ListPromptBuilder {
  private final PromptBuilder promptBuilder;
  private String name;
  private String message;
  private int pageSize;
  private PageSizeType pageSizeType;
  private List<ListItemIF> itemList = new ArrayList<>();

  public ListPromptBuilder(PromptBuilder promptBuilder) {
    this.promptBuilder = promptBuilder;
    this.pageSize = 10;
    this.pageSizeType = PageSizeType.ABSOLUTE;
  }

  public ListPromptBuilder name(String name) {
    this.name = name;
    if (message != null) {
      this.message = name;
    }
    return this;
  }

  public ListPromptBuilder message(String message) {
    this.message = message;
    if (name == null) {
      name = message;
    }
    return this;
  }

  public ListPromptBuilder pageSize(int absoluteSize) {
    this.pageSize = absoluteSize;
    this.pageSizeType = PageSizeType.ABSOLUTE;
    return this;
  }

  public ListPromptBuilder relativePageSize(int relativePageSize) {
    this.pageSize = relativePageSize;
    this.pageSizeType = PageSizeType.RELATIVE;
    return this;
  }
  
  public ListItemBuilder newItem() {
    return new ListItemBuilder(this);
  }

  public ListItemBuilder newItem(String name) {
    ListItemBuilder listItemBuilder = new ListItemBuilder(this);
    return listItemBuilder.name(name).text(name);
  }

  public PromptBuilder addPrompt() {
    ListChoice listChoice = new ListChoice(message, name, pageSize, pageSizeType, itemList);
    promptBuilder.addPrompt(listChoice);
    return promptBuilder;
  }

  void addItem(ListItem listItem) {
    this.itemList.add(listItem);
  }

}
