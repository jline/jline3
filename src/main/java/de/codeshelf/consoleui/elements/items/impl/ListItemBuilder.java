package de.codeshelf.consoleui.elements.items.impl;

import de.codeshelf.consoleui.util.BuilderIF;

/**
 * User: andy
 * Date: 04.01.16
 */
public class ListItemBuilder implements BuilderIF<ListItem> {
  ListItem item;
  private String text;
  private String name;

  private ListItemBuilder() {
    this.item = new ListItem(null);
  }

  static public ListItemBuilder create() {
    return new ListItemBuilder();
  }

  public ListItemBuilder text(String text) {
    this.text = text;
    return this;
  }

  public ListItem build() {
    ListItem listItem = new ListItem(text,name);
    this.name = null;
    return listItem;
  }

  public ListItemBuilder name(String name) {
    this.name = name;
    return this;
  }
}
