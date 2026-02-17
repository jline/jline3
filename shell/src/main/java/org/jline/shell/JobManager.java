/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.List;

/**
 * Manages jobs in the shell's job control system.
 * <p>
 * The job manager tracks running, suspended, and completed jobs and provides
 * methods for querying and listening to job state changes.
 *
 * @since 4.0
 */
public interface JobManager {

    /**
     * Returns the current foreground job, or {@code null} if no job is in the foreground.
     *
     * @return the foreground job, or null
     */
    Job foregroundJob();

    /**
     * Returns all tracked jobs.
     *
     * @return list of all jobs
     */
    List<Job> jobs();

    /**
     * Returns the job with the specified id, or {@code null} if not found.
     *
     * @param id the job id
     * @return the job, or null
     */
    Job get(long id);

    /**
     * Adds a job listener.
     *
     * @param listener the listener to add
     */
    void addJobListener(JobListener listener);

    /**
     * Removes a job listener.
     *
     * @param listener the listener to remove
     */
    void removeJobListener(JobListener listener);
}
