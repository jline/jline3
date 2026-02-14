---
sidebar_position: 19
---

# Curses TUI Library

JLine's curses module provides a terminal-based user interface (TUI) library for building interactive applications. It includes a component model, layout system, keyboard/mouse event handling, theming, and a builder API for composing UIs declaratively.

## Getting Started

Add the `jline-curses` dependency to your project:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-curses</artifactId>
    <version>${jline.version}</version>
</dependency>
```

A minimal curses application:

```java
import org.jline.curses.*;
import org.jline.curses.impl.*;
import org.jline.terminal.*;
import static org.jline.curses.Curses.*;

Terminal terminal = TerminalBuilder.terminal();
GUI gui = gui(terminal);

Window window = window()
    .title("My Application")
    .component(new Label("Hello, TUI!"))
    .build();

gui.addWindow(window);
gui.run();
```

## Components

### Label

Displays static text with alignment and word wrap support.

```java
Label label = new Label("Hello World");
label.setAlignment(Label.Alignment.CENTER); // LEFT, CENTER, RIGHT
label.setWordWrap(true);
label.setStyle(AttributedStyle.DEFAULT.bold());
```

### Input

Single-line text input field with cursor navigation, selection, validation, and password mode.

```java
Input input = new Input("initial text");
input.setPlaceholder("Enter name...");
input.setPasswordMode(true);
input.setValidator(text -> text.length() >= 3);
input.addChangeListener(() -> System.out.println("Changed: " + input.getText()));
```

Key editing methods: `insertText()`, `deleteCharBefore()`, `deleteCharAfter()`, `moveCursorLeft()`, `moveCursorRight()`, `moveCursorToStart()`, `moveCursorToEnd()`, `selectAll()`, `clearSelection()`.

### Button

Clickable button with visual states (normal, focused, pressed).

```java
Button button = new Button("Submit");
button.addClickListener(() -> {
    // handle click
});
```

### TextArea

Multi-line text editor with scrolling, line wrapping, and cursor positioning.

```java
TextArea textArea = new TextArea();
textArea.setText("Line 1\nLine 2\nLine 3");
textArea.setEditable(true);
```

### List

Scrollable list with single or multiple selection, keyboard navigation, and custom item rendering.

```java
List<String> list = new List<>(Arrays.asList("Item 1", "Item 2", "Item 3"));
list.setSelectionMode(List.SelectionMode.MULTIPLE);
list.setItemRenderer(item -> ">> " + item);
list.addSelectionChangeListener(() -> {
    System.out.println("Selected: " + list.getSelectedItems());
});
```

Navigation: Up/Down arrows, Page Up/Down, Home/End. Toggle selection with Space.

### Table

Multi-column table with sorting, column resizing, and row selection.

```java
Table<Person> table = new Table<>();
table.addColumn("Name", Person::getName);
table.addColumn("Age", p -> String.valueOf(p.getAge()));
table.addColumn("Email", Person::getEmail, 30); // with width hint
table.setData(personList);
table.setSelectedRow(0);
```

Navigation: Up/Down arrows for row navigation, Left/Right for column focus.

### Tree

Hierarchical tree view with expand/collapse, keyboard navigation, and custom rendering.

```java
Tree<String> tree = new Tree<>();
Tree.TreeNode<String> root = new Tree.TreeNode<>("Root");
Tree.TreeNode<String> child1 = new Tree.TreeNode<>("Child 1");
child1.addChild(new Tree.TreeNode<>("Grandchild"));
root.addChild(child1);
root.addChild(new Tree.TreeNode<>("Child 2"));
tree.setRoot(root);
```

Navigation: Up/Down to move, Left to collapse/go to parent, Right to expand/go to first child, Space to toggle expand.

### Menu

Menu bar with submenus, keyboard shortcuts, and item actions.

```java
Menu menu = menu(
    submenu().name("File").key("F")
        .item("Open", "O", "C-o", () -> openFile())
        .separator()
        .item("Exit", "x", "C-q", () -> exit()),
    submenu().name("Edit").key("E")
        .item("Copy", "C", "C-c", () -> copy())
        .item("Paste", "P", "C-v", () -> paste())
).build();
```

### Box

Container with a titled border around another component. Supports shortcut keys for focus.

```java
Box box = new Box("Input", Curses.Border.Single, inputComponent, "I");
// Alt+I will focus this box's inner component
```

## Layout System

### BorderPanel

Five-region layout: Top, Bottom, Left, Right, and Center.

```java
Container layout = border()
    .add(menu, Location.Top)
    .add(statusBar, Location.Bottom)
    .add(sidebar, Location.Left)
    .add(mainContent, Location.Center)
    .build();
```

### GridPanel

Grid-based layout for arranging components in rows and columns.

```java
Container grid = grid()
    .add(component1, new GridConstraint())
    .add(component2, new GridConstraint())
    .build();
```

## Event Handling

### KeyEvent

The `KeyEvent` class represents a parsed keyboard event with type, character, modifiers, and special key information.

```java
// KeyEvent types: Character, Arrow, Function, Special, Unknown
// Modifiers: Shift, Control, Alt

// Parse a raw terminal sequence into a KeyEvent
KeyEvent event = KeyParser.parse("\u001b[A"); // Up arrow
event.getType();    // KeyEvent.Type.Arrow
event.getArrow();   // KeyEvent.Arrow.Up

KeyEvent ctrl = KeyParser.parse("\u0001"); // Ctrl+A
ctrl.getCharacter();                        // 'a'
ctrl.hasModifier(KeyEvent.Modifier.Control); // true
```

### Mouse Events

Components can handle mouse events by overriding `handleMouse(MouseEvent event)`. The `AbstractPanel` automatically routes mouse events to the correct child component and handles focus-on-click.

### Shortcut Keys

Components can define shortcut keys that work with Alt+key combinations. When a component with a shortcut is added to a panel, the panel registers it in a `KeyMap` for Alt+key dispatch.

```java
// Via Box
Box box = new Box("Files", border, fileList, "F"); // Alt+F focuses

// Via Component interface
@Override
public String getShortcutKey() {
    return "F"; // Alt+F focuses this component
}
```

## Invalidation and Repaint

The curses module uses an invalidation-based repaint system:

- Components start as invalid (needing initial draw)
- After `draw()`, a component becomes valid
- Calling `invalidate()` marks a component for redraw
- Invalidation propagates upward to parent containers
- Focus changes automatically invalidate the component
- Only invalid components are redrawn on the next render cycle

This avoids unnecessary redrawing and enables efficient screen updates.

## Theming

The `Theme` interface provides named styles for components. The `DefaultTheme` supplies styles for all built-in components.

Style names follow a dot-separated naming convention:
- `.box.border` / `.box.border.focused` -- box border styles
- `.box.title` / `.box.title.focused` -- box title styles
- `.box.key` -- shortcut key highlight style
- `.input.normal` / `.input.focused` / `.input.placeholder` / `.input.selection`
- `.list.normal` / `.list.selected` / `.list.focused` / `.list.selected.focused`
- `.table.header` / `.table.cell` / `.table.cell.focused`
- `.tree.node` / `.tree.node.focused`

## Builder API

The `Curses` class provides static factory methods and builder classes for composing UIs:

```java
import static org.jline.curses.Curses.*;

Window window = window()
    .title("Demo Application")
    .component(
        border()
            .add(menu(
                submenu().name("File").key("F")
                    .item("Quit", "Q", "C-q", () -> System.exit(0))
            ).build(), Location.Top)
            .add(box("Editor", Border.Single)
                .component(new TextArea())
                .key("E")
                .build(), Location.Center)
            .add(new Label("Ready"), Location.Bottom)
    )
    .build();
```

## Screen and VirtualScreen

The `Screen` interface defines drawing operations: `text()`, `fill()`, `clear()`, `refresh()`, cursor management, and size queries. The `VirtualScreen` implementation maintains an in-memory character buffer, useful for testing and off-screen rendering.

```java
VirtualScreen screen = new VirtualScreen(80, 24);
screen.text(0, 0, new AttributedString("Hello", AttributedStyle.DEFAULT.bold()));
screen.fill(0, 1, 80, 1, AttributedStyle.DEFAULT.inverse());
char c = screen.getChar(0, 0); // 'H'
```
