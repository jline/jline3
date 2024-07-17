/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import static org.jline.curses.Curses.*;

public class CursesTest {

    Terminal terminal;
    Component menu, text;
    Window window;
    GUI gui;

    public static void main(String[] args) throws Exception {
        new CursesTest().run();
    }

    public void run() throws Exception {
        terminal = TerminalBuilder.terminal();

        window = window().title("mytitle")
                .component(border().add(
                                menu = menu(
                                                submenu()
                                                        .name("File")
                                                        .key("F")
                                                        .item("View", "V", "F3", this::view)
                                                        .separator()
                                                        .item("Select group", "g", "C-x C-s", this::selectGroup),
                                                submenu()
                                                        .name("Command")
                                                        .key("C")
                                                        .item("User menu", this::userMenu))
                                        .build(),
                                Location.Top)
                        .add(box("Text", Border.Double, text = textArea()), Location.Center))
                .build();

        gui = gui(terminal);
        gui.addWindow(window);
        gui.run();
    }

    private void view() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        System.out.println("view");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void userMenu() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        System.out.println("userMenu");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void selectGroup() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        System.out.println("selectGroup");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
