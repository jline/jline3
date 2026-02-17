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
 * Listener for job status changes.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface JobListener {

    /**
     * Called when a job's status changes.
     *
     * @param job the job whose status changed
     * @param previous the previous status
     * @param current the new status
     */
    void onChange(Job job, Job.Status previous, Job.Status current);
}
