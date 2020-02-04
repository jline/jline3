/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jline.reader.Completer;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Aggregate command registries and dispatch command executions.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public interface SystemRegistry extends CommandRegistry {

    /**
     * Set command registeries
     * @param commandRegistries command registeries used by the application
     */
    void setCommandRegistries(CommandRegistry... commandRegistries);

    /**
     * Initialize consoleEngine environment by executing console script
     * @param script initialization script
     */
    void initialize(File script);

    /**
     * Returns command completer that includes also console variable and script completion.
     * @return command completer
     */
    Completer completer();

    /**
     * Returns a command, method or syntax description for use in the JLine Widgets framework.
     * @param line command line whose description to return
     * @return command description for JLine TailTipWidgets to be displayed
     *         in the terminal status bar.
     */
    Widgets.CmdDesc commandDescription(Widgets.CmdLine line);

   /**
     * Execute a command, script or evaluate scriptEngine statement
     * @param line command line to be executed
     * @return execution result
     * @throws Exception in case of error
     */
    Object execute(String line) throws Exception;

    /**
     *
     * @return terminal
     */
    Terminal terminal();

    /**
     * Execute command with arguments
     * @param command command to be executed
     * @param args arguments of the command
     * @return command execution result
     * @throws Exception in case of error
     */
    Object execute(String command, String[] args) throws Exception;

    /**
     * Execute command with arguments
     * @param command command to be executed
     * @param args arguments of the command
     * @return command execution result
     * @throws Exception in case of error
     */
    Object invoke(String command, Object... args) throws Exception;

    /**
     * Print exception
     * @param stack print stack trace if stack true otherwise message
     * @param terminal JLine terminal
     * @param exception exception to be printed
     */
    static void println(boolean stack, Terminal terminal, Exception exception) {
        if (exception instanceof Options.HelpException) {
            Options.HelpException.highlight((exception).getMessage(), Options.HelpException.defaultStyle()).print(terminal);
        } else if (stack) {
            exception.printStackTrace();
        } else {
            String message = exception.getMessage();
            AttributedStringBuilder asb = new AttributedStringBuilder();
            if (message != null) {
                asb.append(message, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            } else {
                asb.append("Caught exception: ", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                asb.append(exception.getClass().getCanonicalName(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            }
            asb.toAttributedString().println(terminal);
        }
    }

    /**
     * @return systemRegistry of the current thread
     */
    static SystemRegistry get() {
        return Registeries.getInstance().getSystemRegistry();
    }

    /**
     * Add systemRegistry to the thread map
     * @param systemRegistry
     */
    static void add(SystemRegistry systemRegistry) {
        Registeries.getInstance().addRegistry(systemRegistry);
    }

    /**
     * Remove systemRegistry from the thread map
     */
    static void remove() {
        Registeries.getInstance().removeRegistry();
    }

    /**
     * Manage systemRegistry store
     */
    public class Registeries {
        private static Registeries instance = new Registeries();
        private Map<Long, SystemRegistry> systemRegisteries = new HashMap<>();

        private Registeries () {}

        public static Registeries getInstance() {
            return instance;
        }

        public void addRegistry(SystemRegistry systemRegistry) {
            systemRegisteries.put(Thread.currentThread().getId(), systemRegistry);
        }

        public SystemRegistry getSystemRegistry() {
            return systemRegisteries.getOrDefault(Thread.currentThread().getId(), null);
        }

        public void removeRegistry() {
            systemRegisteries.remove(Thread.currentThread().getId());
        }

    }

}
