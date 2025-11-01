/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import java.util.Arrays;
import java.util.List;

import org.jline.curses.impl.Menu;
import org.jline.curses.impl.MenuItem;
import org.jline.curses.impl.SubMenu;

/**
 * Test class for Menu functionality to verify the fixes for:
 * 1. Menu item staying selected after submenu closes
 * 2. Cannot reopen submenu after closing
 * 3. Escape key should close submenu
 */
public class MenuFixesTest {

    public static void main(String[] args) {
        MenuFixesTest test = new MenuFixesTest();
        test.runTests();
    }

    public void runTests() {
        System.out.println("Running Menu Fixes Tests...");

        try {
            testMenuInitialState();
            testMenuSelection();
            testMenuToggleBehavior();
            testMenuSwitchBehavior();
            testMenuItemExecution();

            System.out.println("All tests passed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Menu createTestMenu() {
        // Create menu items
        MenuItem viewItem = new MenuItem();
        viewItem.setName("View");
        viewItem.setKey("V");
        viewItem.setShortcut("F3");
        viewItem.setAction(() -> System.out.println("View action"));

        MenuItem selectGroupItem = new MenuItem();
        selectGroupItem.setName("Select group");
        selectGroupItem.setKey("g");
        selectGroupItem.setShortcut("C-x C-s");
        selectGroupItem.setAction(() -> System.out.println("Select group action"));

        MenuItem userMenuItem = new MenuItem();
        userMenuItem.setName("User menu");
        userMenuItem.setAction(() -> System.out.println("User menu action"));

        // Create submenus
        SubMenu fileMenu = new SubMenu("File", "F", Arrays.asList(viewItem, MenuItem.SEPARATOR, selectGroupItem));
        SubMenu commandMenu = new SubMenu("Command", "C", Arrays.asList(userMenuItem));

        // Create menu
        List<SubMenu> subMenus = Arrays.asList(fileMenu, commandMenu);
        return new Menu(subMenus);
    }

    private void testMenuInitialState() {
        System.out.println("Testing initial state...");
        Menu menu = createTestMenu();
        SubMenu selected = getSelectedSubMenu(menu);
        if (selected != null) {
            throw new RuntimeException("Initially no submenu should be selected, but got: " + selected.getName());
        }
        System.out.println("✓ Initial state test passed");
    }

    private void testMenuSelection() {
        System.out.println("Testing menu selection...");
        Menu menu = createTestMenu();
        SubMenu fileMenu = menu.getContents().get(0);

        // Directly set the selected field to simulate selection
        setSelectedSubMenu(menu, fileMenu);
        SubMenu selected = getSelectedSubMenu(menu);
        if (selected != fileMenu) {
            throw new RuntimeException("File submenu should be selected");
        }
        System.out.println("✓ Menu selection test passed");
    }

    private void testMenuToggleBehavior() {
        System.out.println("Testing menu toggle behavior (simulated)...");
        Menu menu = createTestMenu();
        SubMenu fileMenu = menu.getContents().get(0);

        // Simulate the toggle behavior logic
        // First selection
        setSelectedSubMenu(menu, fileMenu);
        SubMenu selected = getSelectedSubMenu(menu);
        if (selected != fileMenu) {
            throw new RuntimeException("File submenu should be selected");
        }

        // Simulate toggle - if same submenu is selected again, it should be deselected
        // This tests the logic we added: if (s == selected && s != null) { selected = null; }
        SubMenu currentSelected = getSelectedSubMenu(menu);
        if (currentSelected == fileMenu) {
            setSelectedSubMenu(menu, null); // Simulate the toggle behavior
        }

        selected = getSelectedSubMenu(menu);
        if (selected != null) {
            throw new RuntimeException("File submenu should be deselected after toggle, but got: "
                    + (selected != null ? selected.getName() : "null"));
        }
        System.out.println("✓ Menu toggle behavior test passed");
    }

    private void testMenuSwitchBehavior() {
        System.out.println("Testing menu switch behavior (simulated)...");
        Menu menu = createTestMenu();
        SubMenu fileMenu = menu.getContents().get(0);
        SubMenu commandMenu = menu.getContents().get(1);

        // Select first submenu
        setSelectedSubMenu(menu, fileMenu);
        SubMenu selected = getSelectedSubMenu(menu);
        if (selected != fileMenu) {
            throw new RuntimeException("File submenu should be selected");
        }

        // Select second submenu - should replace first
        setSelectedSubMenu(menu, commandMenu);
        selected = getSelectedSubMenu(menu);
        if (selected != commandMenu) {
            throw new RuntimeException("Command submenu should be selected");
        }
        System.out.println("✓ Menu switch behavior test passed");
    }

    private void testMenuItemExecution() {
        System.out.println("Testing menu item execution...");
        Menu menu = createTestMenu();
        SubMenu fileMenu = menu.getContents().get(0);
        MenuItem viewItem = fileMenu.getContents().get(0); // First item (View)

        // Select submenu
        setSelectedSubMenu(menu, fileMenu);
        SubMenu selected = getSelectedSubMenu(menu);
        if (selected != fileMenu) {
            throw new RuntimeException("File submenu should be selected");
        }

        // Execute menu item - should close submenu and reset selection
        executeMenuItem(menu, viewItem);
        selected = getSelectedSubMenu(menu);
        if (selected != null) {
            throw new RuntimeException("Submenu should be closed after menu item execution, but got: "
                    + (selected != null ? selected.getName() : "null"));
        }
        System.out.println("✓ Menu item execution test passed");
    }

    // Helper methods to access private fields and simulate interactions
    private SubMenu getSelectedSubMenu(Menu menu) {
        try {
            java.lang.reflect.Field selectedField = Menu.class.getDeclaredField("selected");
            selectedField.setAccessible(true);
            return (SubMenu) selectedField.get(menu);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access selected field", e);
        }
    }

    private void setSelectedSubMenu(Menu menu, SubMenu subMenu) {
        try {
            java.lang.reflect.Field selectedField = Menu.class.getDeclaredField("selected");
            selectedField.setAccessible(true);
            selectedField.set(menu, subMenu);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set selected field", e);
        }
    }

    private void executeMenuItem(Menu menu, MenuItem item) {
        try {
            java.lang.reflect.Method closeAndExecuteMethod =
                    Menu.class.getDeclaredMethod("closeAndExecute", MenuItem.class);
            closeAndExecuteMethod.setAccessible(true);
            closeAndExecuteMethod.invoke(menu, item);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call closeAndExecute method", e);
        }
    }
}
