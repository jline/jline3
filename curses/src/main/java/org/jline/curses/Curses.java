/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.curses;

import org.jline.curses.impl.*;
import org.jline.terminal.Terminal;

import java.util.*;
import java.util.function.Supplier;

public class Curses {

    public enum Border {
        Single, SingleBevel, Double, DoubleBevel
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
        List<MenuItem> contents = new ArrayList<>();

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

        List<SubMenu> contents = new ArrayList<>();

        public MenuBuilder submenu(String name, String key, List<MenuItem> menu) {
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

}
