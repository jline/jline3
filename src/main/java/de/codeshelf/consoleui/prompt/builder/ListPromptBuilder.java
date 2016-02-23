package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.ListChoice;
import de.codeshelf.consoleui.elements.items.ListItemIF;
import de.codeshelf.consoleui.elements.items.impl.ListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 22.01.16.
 */
public class ListPromptBuilder {
  private final PromptBuilder promptBuilder;
  private String name;
  private String message;
  private List<ListItemIF> itemList = new ArrayList<ListItemIF>();

  public ListPromptBuilder(PromptBuilder promptBuilder) {
    this.promptBuilder = promptBuilder;
  }

  public ListPromptBuilder name(String name) {
    this.name = name;
    this.message = name;
    return this;
  }

  public ListPromptBuilder message(String message) {
    this.message = message;
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
    ListChoice listChoice = new ListChoice(message, name, itemList);
    promptBuilder.addPrompt(listChoice);
    return promptBuilder;
  }

  void addItem(ListItem listItem) {
    this.itemList.add(listItem);
  }

}
