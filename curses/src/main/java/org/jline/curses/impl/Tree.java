/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.curses.Theme;
import org.jline.keymap.KeyMap;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

/**
 * A tree component for displaying hierarchical data.
 *
 * <p>Tree provides a hierarchical view with support for:
 * <ul>
 * <li>Expandable/collapsible nodes</li>
 * <li>Node selection</li>
 * <li>Keyboard navigation</li>
 * <li>Custom node rendering</li>
 * <li>Scrolling for large trees</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of data objects in the tree nodes
 */
public class Tree<T> extends AbstractComponent {

    /**
     * Actions for keyboard navigation.
     */
    enum Action {
        Up,
        Down,
        Left,
        Right,
        PageUp,
        PageDown,
        Home,
        End,
        Expand,
        Collapse,
        Select
    }

    /**
     * Represents a node in the tree.
     */
    public static class TreeNode<T> {
        private T data;
        private TreeNode<T> parent;
        private final java.util.List<TreeNode<T>> children = new ArrayList<>();
        private boolean expanded = false;

        public TreeNode(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public TreeNode<T> getParent() {
            return parent;
        }

        public java.util.List<TreeNode<T>> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public void addChild(TreeNode<T> child) {
            if (child != null && !children.contains(child)) {
                children.add(child);
                child.parent = this;
            }
        }

        public void removeChild(TreeNode<T> child) {
            if (children.remove(child)) {
                child.parent = null;
            }
        }

        public void clearChildren() {
            for (TreeNode<T> child : children) {
                child.parent = null;
            }
            children.clear();
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public int getLevel() {
            int level = 0;
            TreeNode<T> current = parent;
            while (current != null) {
                level++;
                current = current.parent;
            }
            return level;
        }

        public boolean isAncestorOf(TreeNode<T> node) {
            TreeNode<T> current = node.parent;
            while (current != null) {
                if (current == this) {
                    return true;
                }
                current = current.parent;
            }
            return false;
        }
    }

    private TreeNode<T> root;
    private final java.util.List<TreeNode<T>> visibleNodes = new ArrayList<>();
    private TreeNode<T> selectedNode;
    private TreeNode<T> focusedNode;
    private int scrollOffset = 0;
    private Function<T, String> nodeRenderer = Object::toString;

    // Event listeners
    private final java.util.List<Runnable> selectionChangeListeners = new ArrayList<>();
    private final java.util.List<Runnable> expansionChangeListeners = new ArrayList<>();

    // Input handling
    private KeyMap<Action> keyMap;

    // Styling - will be initialized from theme in setTheme()
    private AttributedStyle normalStyle = AttributedStyle.DEFAULT;
    private AttributedStyle selectedStyle = AttributedStyle.DEFAULT.background(AttributedStyle.BLUE);
    private AttributedStyle focusedStyle = AttributedStyle.DEFAULT.inverse();
    private AttributedStyle selectedFocusedStyle =
            AttributedStyle.DEFAULT.background(AttributedStyle.BLUE).inverse();

    // Tree drawing characters
    private String expandedIcon = "▼";
    private String collapsedIcon = "▶";
    private String leafIcon = "•";
    private String branchLine = "│";
    private String lastBranchLine = "└";
    private String middleBranchLine = "├";
    private String horizontalLine = "─";

    public Tree() {}

    public Tree(TreeNode<T> root) {
        setRoot(root);
    }

    @Override
    public void setTheme(Theme theme) {
        super.setTheme(theme);
        if (theme != null) {
            // Initialize styles from theme
            normalStyle = theme.getStyle(".tree.normal");
            selectedStyle = theme.getStyle(".tree.selected");
            focusedStyle = theme.getStyle(".tree.focused");
            selectedFocusedStyle = theme.getStyle(".tree.selected.focused");
        }
    }

    /**
     * Gets the root node.
     *
     * @return the root node
     */
    public TreeNode<T> getRoot() {
        return root;
    }

    /**
     * Sets the root node.
     *
     * @param root the root node to set
     */
    public void setRoot(TreeNode<T> root) {
        this.root = root;
        this.selectedNode = null;
        this.focusedNode = root;
        this.scrollOffset = 0;
        updateVisibleNodes();
    }

    /**
     * Gets the node renderer function.
     *
     * @return the node renderer
     */
    public Function<T, String> getNodeRenderer() {
        return nodeRenderer;
    }

    /**
     * Sets the node renderer function.
     *
     * @param nodeRenderer the node renderer to set
     */
    public void setNodeRenderer(Function<T, String> nodeRenderer) {
        this.nodeRenderer = nodeRenderer != null ? nodeRenderer : Object::toString;
    }

    /**
     * Gets the selected node.
     *
     * @return the selected node
     */
    public TreeNode<T> getSelectedNode() {
        return selectedNode;
    }

    /**
     * Sets the selected node.
     *
     * @param node the node to select
     */
    public void setSelectedNode(TreeNode<T> node) {
        if (node != selectedNode) {
            selectedNode = node;
            notifySelectionChange();
        }
    }

    /**
     * Gets the focused node.
     *
     * @return the focused node
     */
    public TreeNode<T> getFocusedNode() {
        return focusedNode;
    }

    /**
     * Sets the focused node.
     *
     * @param node the node to focus
     */
    public void setFocusedNode(TreeNode<T> node) {
        if (visibleNodes.contains(node)) {
            focusedNode = node;
            ensureFocusedVisible();
        }
    }

    /**
     * Expands a node.
     *
     * @param node the node to expand
     */
    public void expandNode(TreeNode<T> node) {
        if (node != null && node.hasChildren() && !node.isExpanded()) {
            node.setExpanded(true);
            updateVisibleNodes();
            notifyExpansionChange();
        }
    }

    /**
     * Collapses a node.
     *
     * @param node the node to collapse
     */
    public void collapseNode(TreeNode<T> node) {
        if (node != null && node.isExpanded()) {
            node.setExpanded(false);
            updateVisibleNodes();

            // If focused node is no longer visible, move focus to collapsed node
            if (!visibleNodes.contains(focusedNode)) {
                focusedNode = node;
            }

            notifyExpansionChange();
        }
    }

    /**
     * Toggles the expansion state of a node.
     *
     * @param node the node to toggle
     */
    public void toggleNode(TreeNode<T> node) {
        if (node != null && node.hasChildren()) {
            if (node.isExpanded()) {
                collapseNode(node);
            } else {
                expandNode(node);
            }
        }
    }

    /**
     * Expands all nodes in the tree.
     */
    public void expandAll() {
        if (root != null) {
            expandAllRecursive(root);
            updateVisibleNodes();
            notifyExpansionChange();
        }
    }

    /**
     * Collapses all nodes in the tree.
     */
    public void collapseAll() {
        if (root != null) {
            collapseAllRecursive(root);
            updateVisibleNodes();
            focusedNode = root;
            notifyExpansionChange();
        }
    }

    /**
     * Moves the focus up by one node.
     */
    public void moveFocusUp() {
        if (focusedNode != null) {
            int currentIndex = visibleNodes.indexOf(focusedNode);
            if (currentIndex > 0) {
                focusedNode = visibleNodes.get(currentIndex - 1);
                ensureFocusedVisible();
            }
        }
    }

    /**
     * Moves the focus down by one node.
     */
    public void moveFocusDown() {
        if (focusedNode != null) {
            int currentIndex = visibleNodes.indexOf(focusedNode);
            if (currentIndex >= 0 && currentIndex < visibleNodes.size() - 1) {
                focusedNode = visibleNodes.get(currentIndex + 1);
                ensureFocusedVisible();
            }
        }
    }

    /**
     * Scrolls the tree up by the specified number of lines.
     *
     * @param lines the number of lines to scroll up
     */
    public void scrollUp(int lines) {
        scrollOffset = Math.max(0, scrollOffset - lines);
    }

    /**
     * Scrolls the tree down by the specified number of lines.
     *
     * @param lines the number of lines to scroll down
     */
    public void scrollDown(int lines) {
        Size size = getSize();
        if (size != null) {
            int maxScroll = Math.max(0, visibleNodes.size() - size.h());
            scrollOffset = Math.min(maxScroll, scrollOffset + lines);
        }
    }

    /**
     * Adds a selection change listener.
     *
     * @param listener the listener to add
     */
    public void addSelectionChangeListener(Runnable listener) {
        if (listener != null) {
            selectionChangeListeners.add(listener);
        }
    }

    /**
     * Removes a selection change listener.
     *
     * @param listener the listener to remove
     */
    public void removeSelectionChangeListener(Runnable listener) {
        selectionChangeListeners.remove(listener);
    }

    /**
     * Adds an expansion change listener.
     *
     * @param listener the listener to add
     */
    public void addExpansionChangeListener(Runnable listener) {
        if (listener != null) {
            expansionChangeListeners.add(listener);
        }
    }

    /**
     * Removes an expansion change listener.
     *
     * @param listener the listener to remove
     */
    public void removeExpansionChangeListener(Runnable listener) {
        expansionChangeListeners.remove(listener);
    }

    /**
     * Recursively expands all nodes.
     */
    private void expandAllRecursive(TreeNode<T> node) {
        if (node.hasChildren()) {
            node.setExpanded(true);
            for (TreeNode<T> child : node.getChildren()) {
                expandAllRecursive(child);
            }
        }
    }

    /**
     * Recursively collapses all nodes.
     */
    private void collapseAllRecursive(TreeNode<T> node) {
        if (node.hasChildren()) {
            node.setExpanded(false);
            for (TreeNode<T> child : node.getChildren()) {
                collapseAllRecursive(child);
            }
        }
    }

    /**
     * Updates the list of visible nodes based on expansion states.
     */
    private void updateVisibleNodes() {
        visibleNodes.clear();
        if (root != null) {
            addVisibleNodesRecursive(root);
        }
    }

    /**
     * Recursively adds visible nodes to the visible nodes list.
     */
    private void addVisibleNodesRecursive(TreeNode<T> node) {
        visibleNodes.add(node);
        if (node.isExpanded()) {
            for (TreeNode<T> child : node.getChildren()) {
                addVisibleNodesRecursive(child);
            }
        }
    }

    /**
     * Ensures the focused node is visible by adjusting scroll offset.
     */
    private void ensureFocusedVisible() {
        if (focusedNode == null) {
            return;
        }

        int focusedIndex = visibleNodes.indexOf(focusedNode);
        if (focusedIndex < 0) {
            return;
        }

        Size size = getSize();
        if (size == null) {
            return;
        }

        int height = size.h();
        if (height <= 0) {
            return;
        }

        if (focusedIndex < scrollOffset) {
            scrollOffset = focusedIndex;
        } else if (focusedIndex >= scrollOffset + height) {
            scrollOffset = focusedIndex - height + 1;
        }
    }

    /**
     * Notifies all selection change listeners.
     */
    private void notifySelectionChange() {
        for (Runnable listener : selectionChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("Error in tree selection change listener: " + e.getMessage());
            }
        }
    }

    /**
     * Notifies all expansion change listeners.
     */
    private void notifyExpansionChange() {
        for (Runnable listener : expansionChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("Error in tree expansion change listener: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        if (size == null || root == null) {
            return;
        }

        Position pos = getScreenPosition();
        if (pos == null) {
            return;
        }

        int width = size.w();
        int height = size.h();

        // Clear the tree area
        screen.fill(pos.x(), pos.y(), width, height, normalStyle);

        // Draw visible nodes
        for (int row = 0; row < height && (scrollOffset + row) < visibleNodes.size(); row++) {
            TreeNode<T> node = visibleNodes.get(scrollOffset + row);
            drawNode(screen, row, width, node, pos);
        }
    }

    /**
     * Draws a single tree node.
     */
    private void drawNode(Screen screen, int row, int width, TreeNode<T> node, Position pos) {
        boolean isSelected = (node == selectedNode);
        boolean isFocused = (node == focusedNode);

        AttributedStyle style = normalStyle;
        if (isSelected && isFocused) {
            style = selectedFocusedStyle;
        } else if (isSelected) {
            style = selectedStyle;
        } else if (isFocused) {
            style = focusedStyle;
        }

        // Fill row background
        screen.fill(pos.x(), pos.y() + row, width, 1, style);

        StringBuilder lineBuilder = new StringBuilder();

        // Draw tree structure
        int level = node.getLevel();
        TreeNode<T> current = node;

        // Build the tree structure prefix
        java.util.List<Boolean> ancestorLines = new ArrayList<>();
        while (current.getParent() != null) {
            TreeNode<T> parent = current.getParent();
            boolean isLastChild = parent.getChildren().indexOf(current)
                    == parent.getChildren().size() - 1;
            ancestorLines.add(0, !isLastChild);
            current = parent;
        }

        // Draw ancestor lines
        for (int i = 0; i < level - 1; i++) {
            if (i < ancestorLines.size() && ancestorLines.get(i)) {
                lineBuilder.append(branchLine).append(" ");
            } else {
                lineBuilder.append("  ");
            }
        }

        // Draw node connector
        if (level > 0) {
            TreeNode<T> parent = node.getParent();
            boolean isLastChild =
                    parent.getChildren().indexOf(node) == parent.getChildren().size() - 1;

            if (isLastChild) {
                lineBuilder.append(lastBranchLine);
            } else {
                lineBuilder.append(middleBranchLine);
            }
            lineBuilder.append(horizontalLine);
        }

        // Draw expansion icon
        if (node.hasChildren()) {
            if (node.isExpanded()) {
                lineBuilder.append(expandedIcon);
            } else {
                lineBuilder.append(collapsedIcon);
            }
        } else {
            lineBuilder.append(leafIcon);
        }

        lineBuilder.append(" ");

        // Draw node text
        String nodeText = nodeRenderer.apply(node.getData());
        lineBuilder.append(nodeText);

        // Truncate if too long
        String fullLine = lineBuilder.toString();
        if (fullLine.length() > width) {
            fullLine = fullLine.substring(0, Math.max(0, width - 3)) + "...";
        }

        if (!fullLine.isEmpty()) {
            AttributedString attributedLine = new AttributedString(fullLine, style);
            screen.text(pos.x(), pos.y() + row, attributedLine);
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        if (root == null) {
            return new Size(20, 5);
        }

        // Calculate preferred width based on node text and tree structure
        int maxWidth = 0;
        for (TreeNode<T> node : visibleNodes) {
            int nodeWidth = node.getLevel() * 2 + 3; // Tree structure width
            String nodeText = nodeRenderer.apply(node.getData());
            nodeWidth += nodeText.length();
            maxWidth = Math.max(maxWidth, nodeWidth);
        }

        // Preferred height is number of visible nodes, capped at reasonable maximum
        int preferredHeight = Math.min(visibleNodes.size(), 20);

        return new Size(Math.max(20, maxWidth), Math.max(5, preferredHeight));
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        Action action = null;

        // Handle key events directly based on KeyEvent type
        if (event.getType() == KeyEvent.Type.Arrow) {
            switch (event.getArrow()) {
                case Up:
                    action = Action.Up;
                    break;
                case Down:
                    action = Action.Down;
                    break;
                case Left:
                    action = Action.Left;
                    break;
                case Right:
                    action = Action.Right;
                    break;
            }
        } else if (event.getType() == KeyEvent.Type.Special) {
            switch (event.getSpecial()) {
                case Enter:
                    action = Action.Select;
                    break;
                case PageUp:
                    action = Action.PageUp;
                    break;
                case PageDown:
                    action = Action.PageDown;
                    break;
                case Home:
                    action = Action.Home;
                    break;
                case End:
                    action = Action.End;
                    break;
            }
        } else if (event.getType() == KeyEvent.Type.Character) {
            char ch = event.getCharacter();
            if (ch == ' ') {
                action = Action.Expand;
            } else if (ch == '+') {
                action = Action.Expand;
            } else if (ch == '-') {
                action = Action.Collapse;
            } else if (ch == '\n' || ch == '\r') {
                action = Action.Select;
            }
        }

        if (action != null) {
            handleAction(action);
            return true;
        }
        return false;
    }

    private void initializeKeyMap() {
        Terminal terminal = getWindow().getGUI().getTerminal();
        keyMap = new KeyMap<>();

        // Arrow keys
        keyMap.bind(Action.Up, KeyMap.key(terminal, InfoCmp.Capability.key_up));
        keyMap.bind(Action.Down, KeyMap.key(terminal, InfoCmp.Capability.key_down));
        keyMap.bind(Action.Left, KeyMap.key(terminal, InfoCmp.Capability.key_left));
        keyMap.bind(Action.Right, KeyMap.key(terminal, InfoCmp.Capability.key_right));

        // Page navigation
        keyMap.bind(Action.PageUp, KeyMap.key(terminal, InfoCmp.Capability.key_ppage));
        keyMap.bind(Action.PageDown, KeyMap.key(terminal, InfoCmp.Capability.key_npage));

        // Home/End
        keyMap.bind(Action.Home, KeyMap.key(terminal, InfoCmp.Capability.key_home));
        keyMap.bind(Action.End, KeyMap.key(terminal, InfoCmp.Capability.key_end));

        // Tree operations
        keyMap.bind(Action.Expand, "+", " ");
        keyMap.bind(Action.Collapse, "-");
        keyMap.bind(Action.Select, KeyMap.key(terminal, InfoCmp.Capability.key_enter), "\n", "\r");
    }

    private void handleAction(Action action) {
        switch (action) {
            case Up:
                moveFocusUp();
                break;
            case Down:
                moveFocusDown();
                break;
            case Left:
                if (focusedNode != null && focusedNode.isExpanded()) {
                    collapseNode(focusedNode);
                } else {
                    moveFocusToParent();
                }
                break;
            case Right:
                if (focusedNode != null && !focusedNode.getChildren().isEmpty()) {
                    if (!focusedNode.isExpanded()) {
                        expandNode(focusedNode);
                    } else {
                        moveFocusDown(); // Move to first child
                    }
                }
                break;
            case PageUp:
                pageUp();
                break;
            case PageDown:
                pageDown();
                break;
            case Home:
                moveFocusToFirst();
                break;
            case End:
                moveFocusToLast();
                break;
            case Expand:
                if (focusedNode != null && !focusedNode.getChildren().isEmpty()) {
                    expandNode(focusedNode);
                }
                break;
            case Collapse:
                if (focusedNode != null && focusedNode.isExpanded()) {
                    collapseNode(focusedNode);
                }
                break;
            case Select:
                if (focusedNode != null) {
                    setSelectedNode(focusedNode);
                }
                break;
        }
    }

    private void pageUp() {
        Size size = getSize();
        if (size != null) {
            int pageSize = Math.max(1, size.h() - 1);
            for (int i = 0; i < pageSize; i++) {
                moveFocusUp();
            }
        }
    }

    private void pageDown() {
        Size size = getSize();
        if (size != null) {
            int pageSize = Math.max(1, size.h() - 1);
            for (int i = 0; i < pageSize; i++) {
                moveFocusDown();
            }
        }
    }

    private void moveFocusToParent() {
        if (focusedNode != null && focusedNode.getParent() != null) {
            setFocusedNode(focusedNode.getParent());
        }
    }

    private void moveFocusToFirst() {
        if (!visibleNodes.isEmpty()) {
            setFocusedNode(visibleNodes.get(0));
        }
    }

    private void moveFocusToLast() {
        if (!visibleNodes.isEmpty()) {
            setFocusedNode(visibleNodes.get(visibleNodes.size() - 1));
        }
    }
}
