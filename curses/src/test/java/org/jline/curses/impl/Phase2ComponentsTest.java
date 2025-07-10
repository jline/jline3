/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.Arrays;

import org.jline.curses.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Phase 2 components: Label, Input, List, Table, and Tree.
 */
public class Phase2ComponentsTest {

    // Label Tests
    @Test
    public void testLabelBasicFunctionality() {
        Label label = new Label("Hello World");
        assertEquals("Hello World", label.getText());
        assertEquals(Label.Alignment.LEFT, label.getAlignment());
        assertFalse(label.isWordWrap());

        Size preferredSize = label.doGetPreferredSize();
        assertEquals(11, preferredSize.w()); // "Hello World" length
        assertEquals(1, preferredSize.h());
    }

    @Test
    public void testLabelMultiLine() {
        Label label = new Label("Line 1\nLine 2\nLine 3");
        Size preferredSize = label.doGetPreferredSize();
        assertEquals(6, preferredSize.w()); // "Line 1" length
        assertEquals(3, preferredSize.h());
    }

    @Test
    public void testLabelAlignment() {
        Label label = new Label("Test");

        label.setAlignment(Label.Alignment.CENTER);
        assertEquals(Label.Alignment.CENTER, label.getAlignment());

        label.setAlignment(Label.Alignment.RIGHT);
        assertEquals(Label.Alignment.RIGHT, label.getAlignment());
    }

    // Input Tests
    @Test
    public void testInputBasicFunctionality() {
        Input input = new Input();
        assertEquals("", input.getText());
        assertEquals(0, input.getCursorPosition());
        assertTrue(input.isEditable());
        assertFalse(input.isPasswordMode());
    }

    @Test
    public void testInputTextManipulation() {
        Input input = new Input("Hello");
        assertEquals("Hello", input.getText());
        assertEquals(5, input.getCursorPosition());

        input.setCursorPosition(2);
        input.insertText(" World");
        assertEquals("He Worldllo", input.getText());
        assertEquals(8, input.getCursorPosition());
    }

    @Test
    public void testInputCursorMovement() {
        Input input = new Input("Hello World");

        input.moveCursorToStart();
        assertEquals(0, input.getCursorPosition());

        input.moveCursorRight();
        assertEquals(1, input.getCursorPosition());

        input.moveCursorToEnd();
        assertEquals(11, input.getCursorPosition());

        input.moveCursorLeft();
        assertEquals(10, input.getCursorPosition());
    }

    @Test
    public void testInputSelection() {
        Input input = new Input("Hello World");
        input.setCursorPosition(0);

        input.startSelection();
        input.setCursorPosition(5);
        input.extendSelection();

        assertTrue(input.hasSelection());
        assertEquals("Hello", input.getSelectedText());

        input.deleteSelection();
        assertEquals(" World", input.getText());
        assertEquals(0, input.getCursorPosition());
    }

    @Test
    public void testInputPasswordMode() {
        Input input = new Input("secret");
        input.setPasswordMode(true);
        assertTrue(input.isPasswordMode());
        assertEquals('*', input.getPasswordChar());

        input.setPasswordChar('#');
        assertEquals('#', input.getPasswordChar());
    }

    // List Tests
    @Test
    public void testListBasicFunctionality() {
        org.jline.curses.impl.List<String> list = new org.jline.curses.impl.List<>();
        assertTrue(list.getItems().isEmpty());
        assertEquals(-1, list.getFocusedIndex());

        list.addItem("Item 1");
        list.addItem("Item 2");
        list.addItem("Item 3");

        assertEquals(3, list.getItems().size());
        assertEquals(0, list.getFocusedIndex());
        assertEquals("Item 1", list.getFocusedItem());
    }

    @Test
    public void testListSelection() {
        org.jline.curses.impl.List<String> list = new org.jline.curses.impl.List<>(Arrays.asList("A", "B", "C", "D"));

        // Single selection mode (default)
        assertEquals(org.jline.curses.impl.List.SelectionMode.SINGLE, list.getSelectionMode());

        list.setSelectedIndex(1);
        assertEquals(1, list.getSelectedIndices().size());
        assertTrue(list.getSelectedIndices().contains(1));
        assertEquals("B", list.getSelectedItem());

        // Multiple selection mode
        list.setSelectionMode(org.jline.curses.impl.List.SelectionMode.MULTIPLE);
        list.addToSelection(2);
        assertEquals(2, list.getSelectedIndices().size());
        assertTrue(list.getSelectedIndices().contains(1));
        assertTrue(list.getSelectedIndices().contains(2));
    }

    @Test
    public void testListNavigation() {
        org.jline.curses.impl.List<String> list = new org.jline.curses.impl.List<>(Arrays.asList("A", "B", "C"));

        assertEquals(0, list.getFocusedIndex());

        list.moveFocusDown();
        assertEquals(1, list.getFocusedIndex());

        list.moveFocusUp();
        assertEquals(0, list.getFocusedIndex());

        list.moveFocusToLast();
        assertEquals(2, list.getFocusedIndex());

        list.moveFocusToFirst();
        assertEquals(0, list.getFocusedIndex());
    }

    // Table Tests
    @Test
    public void testTableBasicFunctionality() {
        Table<Person> table = new Table<>();

        table.addColumn("Name", Person::getName);
        table.addColumn("Age", p -> String.valueOf(p.getAge()));

        assertEquals(2, table.getColumns().size());
        assertTrue(table.getData().isEmpty());
    }

    @Test
    public void testTableDataManipulation() {
        Table<Person> table = new Table<>();
        table.addColumn("Name", Person::getName);
        table.addColumn("Age", p -> String.valueOf(p.getAge()));

        Person person1 = new Person("Alice", 30);
        Person person2 = new Person("Bob", 25);

        table.addData(person1);
        table.addData(person2);

        assertEquals(2, table.getData().size());
        assertEquals(0, table.getFocusedRow());
        assertEquals(person1, table.getFocusedRowData());

        table.removeData(person1);
        assertEquals(1, table.getData().size());
        assertEquals(person2, table.getFocusedRowData());
    }

    @Test
    public void testTableSelection() {
        Table<Person> table = new Table<>();
        table.addColumn("Name", Person::getName);

        table.setData(Arrays.asList(new Person("Alice", 30), new Person("Bob", 25), new Person("Charlie", 35)));

        table.setSelectedRow(1);
        assertEquals(1, table.getSelectedRows().size());
        assertTrue(table.getSelectedRows().contains(1));

        // Multiple selection
        table.setSelectionMode(Table.SelectionMode.MULTIPLE);
        table.addToSelection(2);
        assertEquals(2, table.getSelectedRows().size());
    }

    // Tree Tests
    @Test
    public void testTreeBasicFunctionality() {
        Tree<String> tree = new Tree<>();
        assertNull(tree.getRoot());

        Tree.TreeNode<String> root = new Tree.TreeNode<>("Root");
        tree.setRoot(root);

        assertEquals(root, tree.getRoot());
        assertEquals(root, tree.getFocusedNode());
    }

    @Test
    public void testTreeNodeHierarchy() {
        Tree.TreeNode<String> root = new Tree.TreeNode<>("Root");
        Tree.TreeNode<String> child1 = new Tree.TreeNode<>("Child 1");
        Tree.TreeNode<String> child2 = new Tree.TreeNode<>("Child 2");
        Tree.TreeNode<String> grandchild = new Tree.TreeNode<>("Grandchild");

        root.addChild(child1);
        root.addChild(child2);
        child1.addChild(grandchild);

        assertEquals(2, root.getChildren().size());
        assertEquals(1, child1.getChildren().size());
        assertEquals(0, child2.getChildren().size());

        assertEquals(root, child1.getParent());
        assertEquals(child1, grandchild.getParent());

        assertEquals(0, root.getLevel());
        assertEquals(1, child1.getLevel());
        assertEquals(2, grandchild.getLevel());

        assertTrue(root.isAncestorOf(grandchild));
        assertFalse(child2.isAncestorOf(grandchild));
    }

    @Test
    public void testTreeExpansion() {
        Tree<String> tree = new Tree<>();
        Tree.TreeNode<String> root = new Tree.TreeNode<>("Root");
        Tree.TreeNode<String> child = new Tree.TreeNode<>("Child");
        root.addChild(child);
        tree.setRoot(root);

        assertFalse(root.isExpanded());

        tree.expandNode(root);
        assertTrue(root.isExpanded());

        tree.collapseNode(root);
        assertFalse(root.isExpanded());

        tree.toggleNode(root);
        assertTrue(root.isExpanded());
    }

    @Test
    public void testTextAreaDemoScenarios() {
        // This test replicates the functionality from the old TextAreaDemo
        // as unit tests to ensure all demo scenarios work correctly

        TextArea textArea = new TextArea();

        // Initial text setup
        String initialText = "Welcome to JLine Curses TextArea!\n\n" + "This is a multi-line text editor component.\n"
                + "Features include:\n"
                + "• Text editing with cursor navigation\n"
                + "• Text selection and manipulation\n"
                + "• Scrolling for large content\n"
                + "• Configurable tab size and word wrap\n\n"
                + "Try editing this text!";

        textArea.setText(initialText);
        assertEquals(initialText, textArea.getText());
        assertEquals(10, textArea.getLineCount());

        // Text insertion
        textArea.insertText(" (INSERTED)");
        assertTrue(textArea.getText().contains("(INSERTED)"));

        // Configuration
        assertTrue(textArea.isEditable());
        assertEquals(4, textArea.getTabSize());
        assertFalse(textArea.isWordWrap());
    }

    // Helper class for table tests
    private static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
