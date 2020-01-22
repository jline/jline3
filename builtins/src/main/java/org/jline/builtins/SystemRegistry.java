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

import org.jline.reader.ParsedLine;
import org.jline.builtins.Widgets;

/**
 * Aggregate command registries and dispatch command executions.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public interface SystemRegistry extends CommandRegistry {

    /**
     * Initialize consoleEngine environment by executing console script
     * @param script
     */
    public void initialize(File script);

    /**
     * Returns a command, method or syntax description for use in the JLine Widgets framework.
     * @param command line whose description to return
     * @return command description for JLine TailTipWidgets to be displayed
     *         in the terminal status bar.
     */
    public Widgets.CmdDesc commandDescription(Widgets.CmdLine line);
    
   /**
     * Execute a command, script or evaluate scriptEngine statement
     * @param parsedLine
     * @return result
     * @throws Exception
     */
    Object execute(ParsedLine parsedLine) throws Exception;

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
