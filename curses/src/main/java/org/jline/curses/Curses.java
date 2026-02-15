/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jline.curses.impl.*;
import org.jline.terminal.Terminal;

public class Curses {

    public enum Border {
        Single,
        SingleBevel,
        Double,
        DoubleBevel
    }

    public enum Alignment {
        Beginning,
        Center,
        End,
        Fill;
    }

    public enum Location implements Constraint {
        Center,
        Top,
        Bottom,
        Left,
        Right
    }

    public static class GridConstraint implements Constraint {
        private final int row;
        private final int col;
        private final int rowSpan;
        private final int colSpan;

        public GridConstraint() {
            this(0, 0, 1, 1);
        }

        public GridConstraint(int row, int col) {
            this(row, col, 1, 1);
        }

        public GridConstraint(int row, int col, int rowSpan, int colSpan) {
            this.row = row;
            this.col = col;
            this.rowSpan = Math.max(1, rowSpan);
            this.colSpan = Math.max(1, colSpan);
        }

        public int row() {
            return row;
        }

        public int col() {
            return col;
        }

        public int rowSpan() {
            return rowSpan;
        }

        public int colSpan() {
            return colSpan;
        }
    }

    public static GridConstraint cell(int row, int col) {
        return new GridConstraint(row, col);
    }

    public static GridConstraint cell(int row, int col, int rowSpan, int colSpan) {
        return new GridConstraint(row, col, rowSpan, colSpan);
    }

    public static GUI gui(Terminal terminal) {
        return new GUIImpl(terminal);
    }

    public static WindowBuilder window() {
        return new WindowBuilder();
    }

    public static Button button() {
        return new Button();
    }

    public static TextArea textArea() {
        return new TextArea();
    }

    public static ContainerBuilder<Location> border() {
        return new ContainerBuilder<>(BorderPanel::new);
    }

    public static ContainerBuilder<GridConstraint> grid() {
        return new ContainerBuilder<>(GridPanel::new);
    }

    public static MenuBuilder menu() {
        return new MenuBuilder();
    }

    public static MenuBuilder menu(SubMenu... subMenus) {
        MenuBuilder builder = new MenuBuilder();
        builder.contents.addAll(Arrays.asList(subMenus));
        return builder;
    }

    public static MenuBuilder menu(SubMenuBuilder... subMenus) {
        MenuBuilder builder = new MenuBuilder();
        for (SubMenuBuilder subMenu : subMenus) {
            builder.contents.add(subMenu.build());
        }
        return builder;
    }

    public static SubMenuBuilder submenu() {
        return new SubMenuBuilder();
    }

    public static Box box(String title, Border border, ComponentBuilder<?> component) {
        return box(title, border, component.build());
    }

    public static Box box(String title, Border border, Component component) {
        return new Box(title, border, component);
    }

    public static BoxBuilder box(String title, Border border) {
        return new BoxBuilder(title, border);
    }

    // New widget factory methods

    public static Separator separator() {
        return new Separator();
    }

    public static Separator separator(Separator.Orientation orientation) {
        return new Separator(orientation);
    }

    public static Checkbox checkbox(String text) {
        return new Checkbox(text);
    }

    public static RadioButton radioButton(String text) {
        return new RadioButton(text);
    }

    public static RadioGroup radioGroup(RadioButton... buttons) {
        return new RadioGroup(buttons);
    }

    public static ProgressBar progressBar() {
        return new ProgressBar();
    }

    public static <T> ComboBox<T> comboBox() {
        return new ComboBox<>();
    }

    // Dialog factory methods

    public static void showMessage(GUI gui, String title, String message) {
        Label label = new Label(message);
        label.setWordWrap(true);
        Button okButton = new Button("OK");

        Container content = border().add(label, Location.Center)
                .add(okButton, Location.Bottom)
                .build();

        Dialog dialog = new Dialog(title, content);
        okButton.addClickListener(dialog::close);
        gui.addWindow(dialog);
    }

    public static void showConfirm(GUI gui, String title, String message, Runnable onOk) {
        Label label = new Label(message);
        label.setWordWrap(true);
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");

        Container buttons =
                grid().add(okButton, cell(0, 0)).add(cancelButton, cell(0, 1)).build();

        Container content = border().add(label, Location.Center)
                .add(buttons, Location.Bottom)
                .build();

        Dialog dialog = new Dialog(title, content);
        okButton.addClickListener(() -> {
            dialog.close();
            if (onOk != null) {
                onOk.run();
            }
        });
        cancelButton.addClickListener(dialog::close);
        gui.addWindow(dialog);
    }

    public static void showInput(GUI gui, String title, String prompt, Consumer<String> onOk) {
        Label label = new Label(prompt);
        Input input = new Input();
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");

        Container buttons =
                grid().add(okButton, cell(0, 0)).add(cancelButton, cell(0, 1)).build();

        Container content = border().add(label, Location.Top)
                .add(input, Location.Center)
                .add(buttons, Location.Bottom)
                .build();

        Dialog dialog = new Dialog(title, content);
        okButton.addClickListener(() -> {
            dialog.close();
            if (onOk != null) {
                onOk.accept(input.getText());
            }
        });
        cancelButton.addClickListener(dialog::close);
        gui.addWindow(dialog);
    }

    public interface ComponentBuilder<C extends Component> {

        C build();
    }

    public static class ContainerBuilder<C extends Constraint> implements ComponentBuilder<Container> {

        private final Map<Component, C> components = new LinkedHashMap<>();
        private final Supplier<AbstractPanel> supplier;

        ContainerBuilder(Supplier<AbstractPanel> supplier) {
            this.supplier = supplier;
        }

        public ContainerBuilder<C> add(Component component, C constraint) {
            components.put(component, constraint);
            return this;
        }

        public ContainerBuilder<C> add(ComponentBuilder<?> component, C constraint) {
            return add(component.build(), constraint);
        }

        public Container build() {
            AbstractPanel container = supplier.get();
            components.forEach(container::addComponent);
            return container;
        }
    }

    public static class SubMenuBuilder {
        private String name;
        private String key;
        java.util.List<MenuItem> contents = new ArrayList<>();

        public SubMenuBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SubMenuBuilder key(String key) {
            this.key = key;
            return this;
        }

        public SubMenuBuilder item(String name, Runnable action) {
            return item().name(name).action(action).add();
        }

        public SubMenuBuilder item(String name, String key, String shortcut, Runnable action) {
            return item().name(name).key(key).shortcut(shortcut).action(action).add();
        }

        public MenuItemBuilder item() {
            return new MenuItemBuilder();
        }

        public SubMenuBuilder separator() {
            contents.add(MenuItem.SEPARATOR);
            return this;
        }

        public SubMenu build() {
            return new SubMenu(name, key, contents);
        }

        public class MenuItemBuilder {

            private String name;
            private String key;
            private String shortcut;
            private Runnable action;

            public MenuItemBuilder name(String name) {
                this.name = name;
                return this;
            }

            public MenuItemBuilder key(String key) {
                this.key = key;
                return this;
            }

            public MenuItemBuilder shortcut(String shortcut) {
                this.shortcut = shortcut;
                return this;
            }

            public MenuItemBuilder action(Runnable action) {
                this.action = action;
                return this;
            }

            public MenuItem build() {
                MenuItem item = new MenuItem();
                item.setName(this.name);
                item.setAction(this.action);
                item.setKey(this.key);
                item.setShortcut(this.shortcut);
                return item;
            }

            public SubMenuBuilder add() {
                contents.add(build());
                return SubMenuBuilder.this;
            }
        }
    }

    public static class MenuBuilder implements ComponentBuilder<Menu> {

        java.util.List<SubMenu> contents = new ArrayList<>();

        public MenuBuilder submenu(String name, String key, java.util.List<MenuItem> menu) {
            return submenu(new SubMenu(name, key, menu));
        }

        public MenuBuilder submenu(SubMenuBuilder menu) {
            return submenu(menu.build());
        }

        public MenuBuilder submenu(SubMenu subMenu) {
            contents.add(subMenu);
            return this;
        }

        public Menu build() {
            return new Menu(contents);
        }
    }

    public static class WindowBuilder {

        private GUI gui;
        private String title;
        private Component component;

        public WindowBuilder gui(GUI gui) {
            this.gui = gui;
            return this;
        }

        public WindowBuilder title(String title) {
            this.title = title;
            return this;
        }

        public WindowBuilder component(ComponentBuilder<?> component) {
            this.component = component.build();
            return this;
        }

        public WindowBuilder component(Component component) {
            this.component = component;
            return this;
        }

        public Window build() {
            BasicWindow w = new BasicWindow();
            w.setGUI(gui);
            w.setTitle(title);
            w.setComponent(component);
            return w;
        }
    }

    public static class BoxBuilder implements ComponentBuilder<Box> {
        private final String title;
        private final Border border;
        private Component component;
        private String shortcutKey;

        BoxBuilder(String title, Border border) {
            this.title = title;
            this.border = border;
        }

        public BoxBuilder component(Component component) {
            this.component = component;
            return this;
        }

        public BoxBuilder component(ComponentBuilder<?> component) {
            return component(component.build());
        }

        public BoxBuilder key(String shortcutKey) {
            this.shortcutKey = shortcutKey;
            return this;
        }

        @Override
        public Box build() {
            if (component == null) {
                throw new IllegalStateException("Component must be set");
            }
            return new Box(title, border, component, shortcutKey);
        }
    }
}
