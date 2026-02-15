/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.jline.curses.*;
import org.jline.curses.impl.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import static org.jline.curses.Curses.*;

/**
 * Interactive Curses Demo showcasing all TUI components.
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
    ProgressBar progressBar;

    public static void main(String[] args) throws Exception {
        new CursesDemo().run();
    }

    public void run() throws Exception {
        terminal = TerminalBuilder.terminal();

        // Create the main window with all components
        createMainWindow();

        // Initialize the GUI and run it
        gui = gui(terminal);
        gui.addWindow(window);
        gui.run();
    }

    private void createMainWindow() {
        // Create components
        createComponents();

        window = window().title("JLine Curses Demo - Press 'q' to quit")
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
                                                        .name("Widgets")
                                                        .key("W")
                                                        .item("Checkbox Demo", "C", null, this::showCheckboxDemo)
                                                        .item("Radio Demo", "R", null, this::showRadioDemo)
                                                        .item("ComboBox Demo", "O", null, this::showComboBoxDemo)
                                                        .item(
                                                                "Progress Bar Demo",
                                                                "P",
                                                                null,
                                                                this::showProgressBarDemo),
                                                submenu()
                                                        .name("Dialogs")
                                                        .key("A")
                                                        .item("Message", "M", null, this::showMessageDialog)
                                                        .item("Confirm", "C", null, this::showConfirmDialog)
                                                        .item("Input", "I", null, this::showInputDialog)
                                                        .item("Image", "G", null, this::showImageDialog),
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
        // Create a layout with components using border layout
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
                        box("Text Editor", Border.Single)
                                .component(textArea)
                                .key("E")
                                .build(),
                        Location.Center)
                .add(
                        box("Tree Component", Border.Single)
                                .component(fileTree)
                                .key("R")
                                .build(),
                        Location.Right)
                .add(
                        grid().add(progressBar, cell(0, 0))
                                .add(statusLabel, cell(1, 0))
                                .build(),
                        Location.Bottom)
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

        // Create TextArea component with line numbers
        textArea = new TextArea();
        textArea.setShowLineNumbers(true);
        textArea.setText("Welcome to JLine Curses!\n\n" + "This interactive demo showcases:\n"
                + "- Input fields with placeholders\n"
                + "- Lists with selection\n"
                + "- Tables with sortable columns\n"
                + "- Trees with hierarchical data\n"
                + "- Text areas with line numbers\n"
                + "- Checkboxes and radio buttons\n"
                + "- ComboBox dropdowns\n"
                + "- Progress bars\n"
                + "- Modal dialogs\n"
                + "- Grid layout panels\n\n"
                + "Use the menu to explore features!");

        // Create ProgressBar
        progressBar = progressBar();
        progressBar.setValue(0.0);

        // Create Status label
        statusLabel = new Label("Ready - Use menus to explore demos, 'q' to quit");
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

    private void showCheckboxDemo() {
        Checkbox cb1 = checkbox("Enable notifications");
        Checkbox cb2 = checkbox("Dark mode");
        Checkbox cb3 = checkbox("Auto-save");
        cb3.setChecked(true);

        Label resultLabel = new Label("Select options above");
        Runnable updateLabel = () -> {
            StringBuilder sb = new StringBuilder("Selected: ");
            if (cb1.isChecked()) sb.append("Notifications ");
            if (cb2.isChecked()) sb.append("DarkMode ");
            if (cb3.isChecked()) sb.append("AutoSave ");
            resultLabel.setText(sb.toString());
        };
        cb1.addChangeListener(updateLabel);
        cb2.addChangeListener(updateLabel);
        cb3.addChangeListener(updateLabel);

        Container content = grid().add(cb1, cell(0, 0))
                .add(cb2, cell(1, 0))
                .add(cb3, cell(2, 0))
                .add(separator(), cell(3, 0))
                .add(resultLabel, cell(4, 0))
                .build();

        Dialog dialog = new Dialog("Checkbox Demo", content);
        gui.addWindow(dialog);
    }

    private void showRadioDemo() {
        RadioButton rb1 = radioButton("Small");
        RadioButton rb2 = radioButton("Medium");
        RadioButton rb3 = radioButton("Large");
        rb2.setSelected(true);
        radioGroup(rb1, rb2, rb3);

        Label resultLabel = new Label("Selected: Medium");
        Runnable updateLabel = () -> {
            if (rb1.isSelected()) resultLabel.setText("Selected: Small");
            if (rb2.isSelected()) resultLabel.setText("Selected: Medium");
            if (rb3.isSelected()) resultLabel.setText("Selected: Large");
        };
        rb1.addChangeListener(updateLabel);
        rb2.addChangeListener(updateLabel);
        rb3.addChangeListener(updateLabel);

        Container content = grid().add(rb1, cell(0, 0))
                .add(rb2, cell(1, 0))
                .add(rb3, cell(2, 0))
                .add(separator(), cell(3, 0))
                .add(resultLabel, cell(4, 0))
                .build();

        Dialog dialog = new Dialog("Radio Button Demo", content);
        gui.addWindow(dialog);
    }

    private void showComboBoxDemo() {
        ComboBox<String> combo = comboBox();
        combo.setItems(Arrays.asList("Red", "Green", "Blue", "Yellow", "Purple", "Orange"));
        combo.setSelectedIndex(0);

        Label resultLabel = new Label("Selected: Red");
        combo.addChangeListener(() -> {
            String item = combo.getSelectedItem();
            resultLabel.setText("Selected: " + (item != null ? item : "none"));
        });

        Container content = grid().add(new Label("Choose a color:"), cell(0, 0))
                .add(combo, cell(1, 0))
                .add(separator(), cell(2, 0))
                .add(resultLabel, cell(3, 0))
                .build();

        Dialog dialog = new Dialog("ComboBox Demo", content);
        gui.addWindow(dialog);
    }

    private void showProgressBarDemo() {
        ProgressBar pb = progressBar();
        pb.setValue(0.4);

        Button advanceButton = new Button("Advance +10%");
        Button resetButton = new Button("Reset");

        Label valueLabel = new Label("Progress: 40%");
        advanceButton.addClickListener(() -> {
            double newVal = Math.min(1.0, pb.getValue() + 0.1);
            pb.setValue(newVal);
            valueLabel.setText("Progress: " + (int) (newVal * 100) + "%");
        });
        resetButton.addClickListener(() -> {
            pb.setValue(0.0);
            valueLabel.setText("Progress: 0%");
        });

        Container buttons = grid().add(advanceButton, cell(0, 0))
                .add(resetButton, cell(0, 1))
                .build();

        Container content = grid().add(pb, cell(0, 0))
                .add(valueLabel, cell(1, 0))
                .add(buttons, cell(2, 0))
                .build();

        Dialog dialog = new Dialog("Progress Bar Demo", content);
        gui.addWindow(dialog);
    }

    private void showMessageDialog() {
        showMessage(gui, "Information", "This is a message dialog.\nPress OK to close.");
    }

    private void showConfirmDialog() {
        showConfirm(gui, "Confirm", "Do you want to proceed?", () -> {
            statusLabel.setText("Confirmed!");
        });
    }

    private void showInputDialog() {
        showInput(gui, "Input", "Enter your name:", (name) -> {
            statusLabel.setText("Hello, " + name + "!");
        });
    }

    private void showImageDialog() {
        BufferedImage testImage = createTestImage();
        ImageComponent imageComponent = new ImageComponent(testImage, "Terminal graphics not supported");
        Dialog dialog = new Dialog("Image Demo", imageComponent);
        gui.addWindow(dialog);
    }

    private static BufferedImage createTestImage() {
        int width = 400;
        int height = 150;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Gradient background
        for (int y = 0; y < height; y++) {
            Color color = new Color(
                    Math.max(0, Math.min(255, (int) (255 * y / (double) height))),
                    Math.max(0, Math.min(255, (int) (255 * (1 - y / (double) height)))),
                    Math.max(0, Math.min(255, (int) (128 + 127 * Math.sin(y * Math.PI / height)))));
            g2d.setColor(color);
            g2d.drawLine(0, y, width, y);
        }

        // Draw centered text
        g2d.setColor(Color.WHITE);
        Font titleFont = new Font("SansSerif", Font.BOLD, 24);
        g2d.setFont(titleFont);
        String titleText = "JLine Graphics Test";
        FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
        int titleX = (width - titleMetrics.stringWidth(titleText)) / 2;

        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 16);
        g2d.setFont(subtitleFont);
        String subtitleText = "Curses Image Component";
        FontMetrics subtitleMetrics = g2d.getFontMetrics(subtitleFont);
        int subtitleX = (width - subtitleMetrics.stringWidth(subtitleText)) / 2;

        int totalTextHeight = titleMetrics.getHeight() + subtitleMetrics.getHeight() + 10;
        int startY = (height - totalTextHeight) / 2;
        int titleY = startY + titleMetrics.getAscent();
        int subtitleY = titleY + titleMetrics.getDescent() + 10 + subtitleMetrics.getAscent();

        g2d.setFont(titleFont);
        g2d.drawString(titleText, titleX, titleY);
        g2d.setFont(subtitleFont);
        g2d.drawString(subtitleText, subtitleX, subtitleY);

        // Border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(10, 10, width - 20, height - 20);

        g2d.dispose();
        return image;
    }

    private void resetComponents() {
        statusLabel.setText("All components reset to initial state.");
        nameInput.setText("John Doe");
        nameInput.setPlaceholder("Enter your name...");
        itemList.setSelectedIndex(0);
        personTable.setSelectedRow(0);
        fileTree.setSelectedNode(fileTree.getRoot());
        progressBar.setValue(0.0);
        textArea.setText("Welcome to JLine Curses!\n\n" + "This interactive demo showcases:\n"
                + "- Input fields with placeholders\n"
                + "- Lists with selection\n"
                + "- Tables with sortable columns\n"
                + "- Trees with hierarchical data\n"
                + "- Text areas with line numbers\n"
                + "- Checkboxes and radio buttons\n"
                + "- ComboBox dropdowns\n"
                + "- Progress bars\n"
                + "- Modal dialogs\n"
                + "- Grid layout panels\n\n"
                + "Use the menu to explore features!");
    }

    private void showAbout() {
        showMessage(gui, "About", "JLine Curses Demo\nShowcasing TUI components:\nWidgets, Dialogs, and Layouts");
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
