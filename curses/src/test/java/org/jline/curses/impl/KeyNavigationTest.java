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
 * Tests for keyboard navigation in List, Table, and Tree components.
 */
public class KeyNavigationTest {

    // List navigation tests

    @Test
    public void testListInitialFocusOnFirstItem() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        assertEquals(0, list.getFocusedIndex());
        assertEquals("A", list.getFocusedItem());
    }

    @Test
    public void testListEmptyHasNoFocus() {
        List<String> list = new List<>();
        assertEquals(-1, list.getFocusedIndex());
        assertNull(list.getFocusedItem());
    }

    @Test
    public void testListMoveFocusDown() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.setSize(new Size(10, 10));
        list.moveFocusDown();
        assertEquals(1, list.getFocusedIndex());
        assertEquals("B", list.getFocusedItem());
    }

    @Test
    public void testListMoveFocusUp() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.setSize(new Size(10, 10));
        list.setFocusedIndex(2);
        list.moveFocusUp();
        assertEquals(1, list.getFocusedIndex());
    }

    @Test
    public void testListMoveFocusUpAtTop() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.setSize(new Size(10, 10));
        list.moveFocusUp();
        assertEquals(0, list.getFocusedIndex());
    }

    @Test
    public void testListMoveFocusDownAtBottom() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.setSize(new Size(10, 10));
        list.setFocusedIndex(2);
        list.moveFocusDown();
        assertEquals(2, list.getFocusedIndex());
    }

    @Test
    public void testListSingleSelection() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.setSelectionMode(List.SelectionMode.SINGLE);
        list.setSelectedIndex(1);
        assertTrue(list.getSelectedIndices().contains(1));
        assertEquals(1, list.getSelectedIndices().size());
    }

    @Test
    public void testListMultipleSelection() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.setSelectionMode(List.SelectionMode.MULTIPLE);
        list.toggleSelection(0);
        list.toggleSelection(2);
        assertTrue(list.getSelectedIndices().contains(0));
        assertTrue(list.getSelectedIndices().contains(2));
        assertEquals(2, list.getSelectedIndices().size());
    }

    @Test
    public void testListSelectionChangeListener() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        boolean[] changed = {false};
        list.addSelectionChangeListener(() -> changed[0] = true);
        list.setSelectedIndex(1);
        assertTrue(changed[0]);
    }

    @Test
    public void testListAddItem() {
        List<String> list = new List<>();
        list.addItem("A");
        assertEquals(1, list.getItems().size());
        assertEquals(0, list.getFocusedIndex());
    }

    @Test
    public void testListRemoveItem() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.removeItem("B");
        assertEquals(2, list.getItems().size());
        assertEquals(Arrays.asList("A", "C"), list.getItems());
    }

    @Test
    public void testListClear() {
        List<String> list = new List<>(Arrays.asList("A", "B", "C"));
        list.clear();
        assertTrue(list.getItems().isEmpty());
        assertEquals(-1, list.getFocusedIndex());
    }

    @Test
    public void testListPreferredSize() {
        List<String> list = new List<>(Arrays.asList("Hello", "World!"));
        Size size = list.doGetPreferredSize();
        assertEquals(10, size.w()); // min width is 10
        assertEquals(3, size.h()); // min height is 3
    }

    // Table tests

    @Test
    public void testTableCreation() {
        Table<String> table = new Table<>();
        table.addColumn("Name", s -> s);
        table.addColumn("Length", s -> String.valueOf(s.length()));
        table.setData(Arrays.asList("Hello", "World"));

        assertEquals(2, table.getData().size());
        assertEquals(2, table.getColumns().size());
    }

    @Test
    public void testTableFocusNavigation() {
        Table<String> table = new Table<>();
        table.addColumn("Name", s -> s);
        table.setData(Arrays.asList("A", "B", "C"));
        table.setSize(new Size(20, 10));

        assertEquals(0, table.getFocusedRow());
        table.moveFocusDown();
        assertEquals(1, table.getFocusedRow());
        table.moveFocusUp();
        assertEquals(0, table.getFocusedRow());
    }

    @Test
    public void testTableSelection() {
        Table<String> table = new Table<>();
        table.addColumn("Name", s -> s);
        table.setData(Arrays.asList("A", "B", "C"));

        table.setSelectedRow(1);
        assertTrue(table.getSelectedRows().contains(1));
    }

    // Tree tests

    @Test
    public void testTreeCreation() {
        Tree<String> tree = new Tree<>();
        Tree.TreeNode<String> root = new Tree.TreeNode<>("Root");
        root.addChild(new Tree.TreeNode<>("Child 1"));
        root.addChild(new Tree.TreeNode<>("Child 2"));
        tree.setRoot(root);

        assertNotNull(tree.getRoot());
        assertEquals(2, tree.getRoot().getChildren().size());
    }

    @Test
    public void testTreeNodeExpansion() {
        Tree.TreeNode<String> root = new Tree.TreeNode<>("Root");
        root.addChild(new Tree.TreeNode<>("Child 1"));
        assertFalse(root.isExpanded()); // Nodes default to collapsed
        root.setExpanded(true);
        assertTrue(root.isExpanded());
    }

    @Test
    public void testTreeNodeIsLeaf() {
        Tree.TreeNode<String> leaf = new Tree.TreeNode<>("Leaf");
        assertTrue(leaf.isLeaf());

        Tree.TreeNode<String> parent = new Tree.TreeNode<>("Parent");
        parent.addChild(leaf);
        assertFalse(parent.isLeaf());
    }

    @Test
    public void testTreeNodeAddRemoveChild() {
        Tree.TreeNode<String> parent = new Tree.TreeNode<>("Parent");
        Tree.TreeNode<String> child = new Tree.TreeNode<>("Child");
        parent.addChild(child);
        assertEquals(1, parent.getChildren().size());
        assertEquals(parent, child.getParent());

        parent.removeChild(child);
        assertTrue(parent.getChildren().isEmpty());
    }
}
