/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages the JLine shutdown-hook thread and tasks to execute on shutdown.
 *
 * <p>
 * The ShutdownHooks class provides a centralized mechanism for registering tasks
 * that should be executed when the JVM shuts down. By default, it manages a single
 * shutdown hook thread that executes all registered tasks in the order of their
 * registration.
 * </p>
 *
 * <p>
 * This class is particularly useful for terminal applications that need to perform
 * cleanup operations when the application is terminated, such as restoring the
 * terminal to its original state, closing open files, or releasing other resources.
 * </p>
 *
 * <p>
 * Tasks are registered using the {@link #add(Task)} method and can be removed using
 * the {@link #remove(Task)} method. All tasks must implement the {@link Task} interface,
 * which defines a single {@link Task#run()} method that is called when the JVM shuts down.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Create a task to restore the terminal on shutdown
 * ShutdownHooks.Task task = ShutdownHooks.add(() -> {
 *     terminal.setAttributes(originalAttributes);
 *     terminal.close();
 * });
 *
 * // Later, if the task is no longer needed
 * ShutdownHooks.remove(task);
 * </pre>
 *
 * @since 2.7
 */
public final class ShutdownHooks {

    /**
     * Private constructor to prevent instantiation.
     */
    private ShutdownHooks() {
        // Utility class
    }

    /**
     * System property that, when set to {@code true}, prevents JLine from registering its shutdown
     * hook with the JVM. Tasks are still tracked, and the application is then responsible for calling
     * {@link #runTasks()} itself, for instance from its own shutdown hook.
     */
    public static final String PROP_DISABLE_SHUTDOWN_HOOK = "org.jline.shutdownhook.disable";

    private static final List<Task> tasks = new ArrayList<>();

    private static Thread hook;

    /**
     * Adds a task to be executed when the JVM shuts down.
     *
     * <p>
     * This method registers a task to be executed when the JVM shuts down. Tasks are
     * executed in the order of their registration, so the first added task will be
     * executed first.
     * </p>
     *
     * <p>
     * If this is the first task to be added, a shutdown hook thread will be created
     * and registered with the JVM. This thread will execute all registered tasks when
     * the JVM shuts down.
     * </p>
     *
     * @param <T> the type of the task
     * @param task the task to be executed on shutdown
     * @return the task that was added (for method chaining)
     * @throws NullPointerException if the task is null
     */
    public static synchronized <T extends Task> T add(final T task) {
        Objects.requireNonNull(task);

        // Install the hook thread if needed, unless the application runs the tasks itself
        if (hook == null && !Boolean.getBoolean(PROP_DISABLE_SHUTDOWN_HOOK)) {
            hook = addHook(new Thread("JLine Shutdown Hook") {
                @Override
                public void run() {
                    runTasks();
                }
            });
        }

        // Track the task
        Log.debug("Adding shutdown-hook task: ", task);
        tasks.add(task);

        return task;
    }

    /**
     * Runs all registered tasks, in the order they were registered, then forgets them.
     *
     * <p>
     * This is what the JLine shutdown hook does. It is meant for applications that set
     * {@link #PROP_DISABLE_SHUTDOWN_HOOK} and decide themselves when terminals get closed, for
     * instance after their logging has been flushed. A task that fails is logged and does not
     * prevent the other tasks from running.
     * </p>
     */
    public static synchronized void runTasks() {
        Log.debug("Running all shutdown-hook tasks");

        // Take the tasks out before running them, so a task added while they run is kept for the next run
        final Task[] toRun = tasks.toArray(new Task[tasks.size()]);
        tasks.clear();

        for (Task task : toRun) {
            Log.debug("Running task: ", task);
            try {
                task.run();
            } catch (Throwable e) {
                Log.warn("Task failed", e);
            }
        }
    }

    private static Thread addHook(final Thread thread) {
        Log.debug("Registering shutdown-hook: ", thread);
        Runtime.getRuntime().addShutdownHook(thread);
        return thread;
    }

    public static synchronized void remove(final Task task) {
        Objects.requireNonNull(task);

        // Drop the task, it is tracked even when the hook is disabled
        tasks.remove(task);

        // If there are no more tasks, then remove the hook thread
        if (hook != null && tasks.isEmpty()) {
            removeHook(hook);
            hook = null;
        }
    }

    private static void removeHook(final Thread thread) {
        Log.debug("Removing shutdown-hook: ", thread);

        try {
            Runtime.getRuntime().removeShutdownHook(thread);
        } catch (IllegalStateException e) {
            // The VM is shutting down, not a big deal; ignore
        }
    }

    /**
     * Essentially a {@link Runnable} which allows running to throw an exception.
     */
    public interface Task {
        void run() throws Exception;
    }
}
