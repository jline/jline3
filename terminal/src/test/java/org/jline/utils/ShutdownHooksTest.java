/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ShutdownHooksTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(ShutdownHooks.PROP_DISABLE_SHUTDOWN_HOOK);
    }

    @Test
    void hookIsRegisteredByDefault() throws Exception {
        ShutdownHooks.Task task = ShutdownHooks.add(() -> {});
        try {
            assertNotNull(hook());
        } finally {
            ShutdownHooks.remove(task);
        }
    }

    @Test
    void disabledHookIsNotRegisteredAndTasksRunOnDemand() throws Exception {
        System.setProperty(ShutdownHooks.PROP_DISABLE_SHUTDOWN_HOOK, "true");
        AtomicInteger runs = new AtomicInteger();

        ShutdownHooks.Task task = ShutdownHooks.add(runs::incrementAndGet);
        assertNull(hook());

        ShutdownHooks.runTasks();
        assertEquals(1, runs.get());

        // the tasks are forgotten once they ran
        ShutdownHooks.runTasks();
        assertEquals(1, runs.get());
        ShutdownHooks.remove(task);
    }

    @Test
    void disabledHookStillRemovesTasks() throws Exception {
        System.setProperty(ShutdownHooks.PROP_DISABLE_SHUTDOWN_HOOK, "true");
        AtomicInteger runs = new AtomicInteger();

        ShutdownHooks.Task task = ShutdownHooks.add(runs::incrementAndGet);
        ShutdownHooks.remove(task);
        ShutdownHooks.runTasks();

        assertEquals(0, runs.get());
        assertNull(hook());
    }

    private static Thread hook() throws Exception {
        Field field = ShutdownHooks.class.getDeclaredField("hook");
        field.setAccessible(true);
        return (Thread) field.get(null);
    }
}
