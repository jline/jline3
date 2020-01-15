/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.HashMap;
import java.util.Map;

import org.jline.reader.ParsedLine;

public interface SystemRegistry extends CommandRegistry {

    Object execute(ParsedLine parsedLine) throws Exception;

    static SystemRegistry get() {
        return Registeries.getInstance().getSystemRegistry();
    }

    static void add(SystemRegistry systemRegistry) {
        Registeries.getInstance().addRegistry(systemRegistry);
    }

    static void remove() {
        Registeries.getInstance().removeRegistry();
    }

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
