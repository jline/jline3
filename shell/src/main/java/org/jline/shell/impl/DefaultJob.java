/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import org.jline.shell.Job;

/**
 * Default implementation of {@link Job}.
 * <p>
 * Wraps a {@link Thread} reference and tracks the job's lifecycle status.
 *
 * @since 4.0
 */
public class DefaultJob implements Job {

    private final long id;
    private final String command;
    private final Thread thread;
    private volatile Status status;

    /**
     * Creates a new job.
     *
     * @param id the job id
     * @param command the command string
     * @param thread the thread executing the command
     */
    public DefaultJob(long id, String command, Thread thread) {
        this.id = id;
        this.command = command;
        this.thread = thread;
        this.status = Status.Foreground;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public String command() {
        return command;
    }

    @Override
    public Status status() {
        return status;
    }

    /**
     * Sets the status of this job.
     *
     * @param status the new status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns the thread executing this job.
     *
     * @return the thread
     */
    public Thread thread() {
        return thread;
    }

    @Override
    public void interrupt() {
        thread.interrupt();
    }

    @Override
    public void suspend() {
        this.status = Status.Suspended;
    }

    @Override
    public void resume(boolean foreground) {
        this.status = foreground ? Status.Foreground : Status.Background;
    }
}
