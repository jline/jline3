/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

/**
 * Represents a job in the shell's job control system.
 * <p>
 * A job tracks the execution of a command, including its lifecycle status.
 * Jobs can be in foreground, background, suspended, or done states.
 *
 * @since 4.0
 */
public interface Job {

    /**
     * Job lifecycle status.
     */
    enum Status {
        /** Job is running in the foreground */
        Foreground,
        /** Job is running in the background */
        Background,
        /** Job has been suspended (e.g., via Ctrl-Z) */
        Suspended,
        /** Job has completed execution */
        Done
    }

    /**
     * Returns the unique identifier for this job.
     *
     * @return the job id
     */
    long id();

    /**
     * Returns the command string associated with this job.
     *
     * @return the command string
     */
    String command();

    /**
     * Returns the current status of this job.
     *
     * @return the job status
     */
    Status status();

    /**
     * Interrupts this job.
     */
    void interrupt();

    /**
     * Suspends this job.
     */
    void suspend();

    /**
     * Resumes this job.
     *
     * @param foreground if true, resume in foreground; otherwise resume in background
     */
    void resume(boolean foreground);
}
