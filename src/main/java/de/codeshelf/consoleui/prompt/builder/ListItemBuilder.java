package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.items.impl.ListItem;

/**
 * Created by andy on 22.01.16.
 */
public class ListItemBuilder {
  private final ListPromptBuilder listPromptBuilder;
  private String text;
  private String name;

  public ListItemBuilder(ListPromptBuilder listPromptBuilder) {
    this.listPromptBuilder = listPromptBuilder;
  }

  public ListItemBuilder text(String text) {
    this.text = text;
    return this;
  }

  public ListItemBuilder name(String name) {
    this.name = name;
    return this;
  }

  public ListPromptBuilder add() {
    listPromptBuilder.addItem(new ListItem(text, name));
    return listPromptBuilder;
  }
}
