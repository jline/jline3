/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.util.Arrays;

import org.jline.curses.*;
import org.jline.curses.impl.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import static org.jline.curses.Curses.*;

/**
 * Interactive Curses Demo showcasing Phase 2 components.
 * Creates a proper curses GUI with windows and interactive components.
 */
public class CursesDemo {

    Terminal terminal;
    Window window;
    GUI gui;

    // Components for the demo
    Input nameInput;
    List<String> itemList;
    Table<Person> personTable;
    Tree<String> fileTree;
    TextArea textArea;
    Label statusLabel;

    public static void main(String[] args) throws Exception {
        new CursesDemo().run();
    }

    public void run() throws Exception {
        terminal = TerminalBuilder.terminal();

        // Create the main window with all Phase 2 components
        createMainWindow();

        // Initialize the GUI and run it
        gui = gui(terminal);
        gui.addWindow(window);
        gui.run();
    }

    private void createMainWindow() {
        // Create Phase 2 components
        createComponents();

        window = window().title("JLine Curses Phase 2 Demo - Press 'q' to quit")
                .component(border().add(
                                menu(
                                                submenu()
                                                        .name("Demo")
                                                        .key("D")
                                                        .item("Input Demo", "I", "F1", this::showInputDemo)
                                                        .item("List Demo", "L", "F2", this::showListDemo)
                                                        .item("Table Demo", "T", "F3", this::showTableDemo)
                                                        .item("Tree Demo", "R", "F4", this::showTreeDemo)
                                                        .separator()
                                                        .item("Reset All", "R", "F5", this::resetComponents),
                                                submenu()
                                                        .name("Help")
                                                        .key("H")
                                                        .item("About", "A", "F12", this::showAbout))
                                        .build(),
                                Location.Top)
                        .add(createMainLayout(), Location.Center))
                .build();
    }

    private Component createMainLayout() {
        // Create a layout with Phase 2 components using border layout
        // Add keyboard shortcuts for direct navigation to components
        return border().add(
                        box("Input Component", Border.Single)
                                .component(nameInput)
                                .key("I")
                                .build(),
                        Location.Top)
                .add(
                        box("List Component", Border.Single)
                                .component(itemList)
                                .key("L")
                                .build(),
                        Location.Left)
                .add(
                        box("Table Component", Border.Single)
                                .component(personTable)
                                .key("T")
                                .build(),
                        Location.Center)
                .add(
                        box("Tree Component", Border.Single)
                                .component(fileTree)
                                .key("R")
                                .build(),
                        Location.Right)
                .add(box("Status", Border.Single).component(statusLabel).build(), Location.Bottom)
                .build();
    }

    private void createComponents() {
        // Create Input component
        nameInput = new Input();
        nameInput.setPlaceholder("Enter your name...");
        nameInput.setText("John Doe");

        // Create List component
        itemList = new List<>();
        itemList.setItems(Arrays.asList("Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grape"));
        itemList.setSelectionMode(List.SelectionMode.SINGLE);

        // Create Table component
        personTable = new Table<>();
        personTable.addColumn("Name", Person::getName);
        personTable.addColumn("Age", p -> String.valueOf(p.getAge()));
        personTable.addColumn("City", Person::getCity);

        // Add sample data
        personTable.addData(new Person("Alice Johnson", 28, "New York"));
        personTable.addData(new Person("Bob Smith", 35, "Los Angeles"));
        personTable.addData(new Person("Charlie Brown", 42, "Chicago"));
        personTable.addData(new Person("Diana Prince", 31, "Miami"));

        // Create Tree component
        fileTree = new Tree<>();
        Tree.TreeNode<String> root = new Tree.TreeNode<>("File System");
        fileTree.setRoot(root);

        Tree.TreeNode<String> documents = new Tree.TreeNode<>("Documents");
        Tree.TreeNode<String> pictures = new Tree.TreeNode<>("Pictures");
        Tree.TreeNode<String> music = new Tree.TreeNode<>("Music");

        documents.addChild(new Tree.TreeNode<>("Resume.pdf"));
        documents.addChild(new Tree.TreeNode<>("Letter.docx"));
        pictures.addChild(new Tree.TreeNode<>("Vacation.jpg"));
        pictures.addChild(new Tree.TreeNode<>("Family.png"));
        music.addChild(new Tree.TreeNode<>("Song1.mp3"));
        music.addChild(new Tree.TreeNode<>("Song2.mp3"));

        root.addChild(documents);
        root.addChild(pictures);
        root.addChild(music);

        // Create TextArea component
        textArea = new TextArea();
        textArea.setText("Welcome to JLine Curses Phase 2!\n\n" + "This interactive demo showcases:\n"
                + "• Input fields with placeholders\n"
                + "• Lists with selection\n"
                + "• Tables with sortable columns\n"
                + "• Trees with hierarchical data\n"
                + "• Text areas for editing\n\n"
                + "Use the menu to explore features!");

        // Create Status label
        statusLabel = new Label("Ready - Use F1-F5 for demos, 'q' to quit");
        statusLabel.setAlignment(Label.Alignment.CENTER);
    }

    // Demo action methods
    private void showInputDemo() {
        statusLabel.setText("Input Demo: Type in the input field above. Try different text!");
        nameInput.focus();
        nameInput.setText("");
        nameInput.setPlaceholder("Type something here...");
    }

    private void showListDemo() {
        statusLabel.setText("List Demo: Use arrow keys to navigate the list. Press Enter to select.");
        itemList.focus();
        itemList.setSelectedIndex(0);
    }

    private void showTableDemo() {
        statusLabel.setText("Table Demo: Navigate with arrow keys. Click column headers to sort.");
        personTable.focus();
        personTable.setSelectedRow(0);
    }

    private void showTreeDemo() {
        statusLabel.setText("Tree Demo: Use arrow keys to navigate. Press Enter to expand/collapse nodes.");
        fileTree.focus();
        fileTree.setSelectedNode(fileTree.getRoot());
    }

    private void resetComponents() {
        statusLabel.setText("All components reset to initial state.");
        nameInput.setText("John Doe");
        nameInput.setPlaceholder("Enter your name...");
        itemList.setSelectedIndex(0);
        personTable.setSelectedRow(0);
        fileTree.setSelectedNode(fileTree.getRoot());
        textArea.setText("Welcome to JLine Curses Phase 2!\n\n" + "This interactive demo showcases:\n"
                + "• Input fields with placeholders\n"
                + "• Lists with selection\n"
                + "• Tables with sortable columns\n"
                + "• Trees with hierarchical data\n"
                + "• Text areas for editing\n\n"
                + "Use the menu to explore features!");
    }

    private void showAbout() {
        statusLabel.setText("JLine Curses Phase 2 Demo - Showcasing enhanced TUI components!");
    }

    // Helper class for table demo
    public static class Person {
        private final String name;
        private final int age;
        private final String city;

        public Person(String name, int age, String city) {
            this.name = name;
            this.age = age;
            this.city = city;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public String getCity() {
            return city;
        }

        @Override
        public String toString() {
            return name + " (" + age + ", " + city + ")";
        }
    }
}
