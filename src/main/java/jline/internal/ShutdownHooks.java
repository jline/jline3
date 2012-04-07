/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jline.internal;

import java.util.ArrayList;
import java.util.List;

import static jline.internal.Preconditions.checkNotNull;

/**
 * Manages the JLine shutdown-hook thread and tasks to execute on shutdown.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.7
 */
public class ShutdownHooks
{
    public static final String JLINE_SHUTDOWNHOOK = "jline.shutdownhook";

    private static final boolean enabled = Configuration.getBoolean(JLINE_SHUTDOWNHOOK, true);

    private static final List<Runnable> tasks = new ArrayList<Runnable>();

    private static Thread hook;

    public static synchronized <T extends Runnable> T add(final T task) {
        checkNotNull(task);

        // If not enabled ignore
        if (!enabled) {
            Log.debug("Shutdown-hook is disabled; not installing: ", task);
            return task;
        }

        // Install the hook thread if needed
        if (hook == null) {
            hook = addHook(new Thread("JLine Shutdown Hook")
            {
                @Override
                public void run() {
                    runTasks();
                }
            });
        }

        // Track the task
        Log.trace("Adding shutdown-hook task: ", task);
        tasks.add(task);

        return task;
    }

    private static synchronized void runTasks() {
        Log.trace("Running all shutdown-hook tasks");

        for (Runnable task : tasks) {
            Log.trace("Running task:", task);
            try {
                task.run();
            }
            catch (Throwable e) {
                Log.trace("Task failed:", e);
            }
        }

        tasks.clear();
    }

    private static Thread addHook(final Thread thread) {
        Log.trace("Registering shutdown-hook: ", thread);
        try {
            Runtime.getRuntime().addShutdownHook(thread);
        }
        catch (AbstractMethodError e) {
            // JDK 1.3+ only method. Bummer.
            Log.trace("Failed to register shutdown-hook: ", e);
        }
        return thread;
    }

    public static synchronized void remove(final Runnable task) {
        checkNotNull(task);

        // ignore if not enabled or hook never installed
        if (!enabled || hook == null) {
            return;
        }

        // Drop the task
        tasks.remove(task);

        // If there are no more tasks, then remove the hook thread
        if (tasks.isEmpty()) {
            removeHook(hook);
            hook = null;
        }
    }

    private static void removeHook(final Thread thread) {
        Log.trace("Removing shutdown-hook: ", thread);

        try {
            Runtime.getRuntime().removeShutdownHook(thread);
        }
        catch (AbstractMethodError e) {
            // JDK 1.3+ only method. Bummer.
            Log.trace("Failed to remove shutdown-hook: ", e);
        }
        catch (IllegalStateException e) {
            // The VM is shutting down, not a big deal; ignore
        }
    }
}