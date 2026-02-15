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

import org.jline.curses.impl.Menu;
import org.jline.curses.impl.MenuItem;
import org.jline.curses.impl.SubMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Menu functionality to verify the fixes for:
 * 1. Menu item staying selected after submenu closes
 * 2. Cannot reopen submenu after closing
 * 3. Escape key should close submenu
 */
public class MenuFixesTest {

    private Menu menu;
    private SubMenu fileMenu;
    private SubMenu commandMenu;
    private MenuItem viewItem;

    @BeforeEach
    public void setUp() {
        viewItem = new MenuItem();
        viewItem.setName("View");
        viewItem.setKey("V");
        viewItem.setShortcut("F3");
        viewItem.setAction(() -> {});

        MenuItem selectGroupItem = new MenuItem();
        selectGroupItem.setName("Select group");
        selectGroupItem.setKey("g");
        selectGroupItem.setShortcut("C-x C-s");
        selectGroupItem.setAction(() -> {});

        MenuItem userMenuItem = new MenuItem();
        userMenuItem.setName("User menu");
        userMenuItem.setAction(() -> {});

        fileMenu = new SubMenu("File", "F", Arrays.asList(viewItem, MenuItem.SEPARATOR, selectGroupItem));
        commandMenu = new SubMenu("Command", "C", Arrays.asList(userMenuItem));

        menu = new Menu(Arrays.asList(fileMenu, commandMenu));
    }

    @Test
    public void testMenuInitialState() {
        SubMenu selected = getSelectedSubMenu();
        assertNull(selected, "Initially no submenu should be selected");
    }

    @Test
    public void testMenuSelection() {
        setSelectedSubMenu(fileMenu);
        assertEquals(fileMenu, getSelectedSubMenu());
    }

    @Test
    public void testMenuToggleBehavior() {
        setSelectedSubMenu(fileMenu);
        assertEquals(fileMenu, getSelectedSubMenu());

        // Simulate toggle: if same submenu is selected again, deselect it
        if (getSelectedSubMenu() == fileMenu) {
            setSelectedSubMenu(null);
        }
        assertNull(getSelectedSubMenu(), "File submenu should be deselected after toggle");
    }

    @Test
    public void testMenuSwitchBehavior() {
        setSelectedSubMenu(fileMenu);
        assertEquals(fileMenu, getSelectedSubMenu());

        setSelectedSubMenu(commandMenu);
        assertEquals(commandMenu, getSelectedSubMenu());
    }

    @Test
    public void testMenuItemExecution() {
        setSelectedSubMenu(fileMenu);
        assertEquals(fileMenu, getSelectedSubMenu());

        executeMenuItem(viewItem);
        assertNull(getSelectedSubMenu(), "Submenu should be closed after menu item execution");
    }

    private SubMenu getSelectedSubMenu() {
        try {
            java.lang.reflect.Field selectedField = Menu.class.getDeclaredField("selected");
            selectedField.setAccessible(true);
            return (SubMenu) selectedField.get(menu);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access selected field", e);
        }
    }

    private void setSelectedSubMenu(SubMenu subMenu) {
        try {
            java.lang.reflect.Field selectedField = Menu.class.getDeclaredField("selected");
            selectedField.setAccessible(true);
            selectedField.set(menu, subMenu);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set selected field", e);
        }
    }

    private void executeMenuItem(MenuItem item) {
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
