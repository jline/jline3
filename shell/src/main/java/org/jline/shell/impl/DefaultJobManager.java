/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.jline.shell.Job;
import org.jline.shell.JobListener;
import org.jline.shell.JobManager;

/**
 * Default implementation of {@link JobManager}.
 * <p>
 * Tracks jobs using an internal list and notifies registered listeners on status changes.
 *
 * @since 4.0
 */
public class DefaultJobManager implements JobManager {

    /**
     * Creates a new DefaultJobManager.
     */
    public DefaultJobManager() {}

    private final AtomicLong nextId = new AtomicLong(1);
    private final List<DefaultJob> managedJobs = new ArrayList<>();
    private final CopyOnWriteArrayList<JobListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public Job foregroundJob() {
        synchronized (managedJobs) {
            for (DefaultJob job : managedJobs) {
                if (job.status() == Job.Status.Foreground) {
                    return job;
                }
            }
        }
        return null;
    }

    @Override
    public List<Job> jobs() {
        synchronized (managedJobs) {
            return Collections.unmodifiableList(new ArrayList<>(managedJobs));
        }
    }

    @Override
    public Job get(long id) {
        synchronized (managedJobs) {
            for (DefaultJob job : managedJobs) {
                if (job.id() == id) {
                    return job;
                }
            }
        }
        return null;
    }

    @Override
    public void addJobListener(JobListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeJobListener(JobListener listener) {
        listeners.remove(listener);
    }

    /**
     * Creates a new foreground job for the given command and thread.
     *
     * @param command the command string
     * @param thread the thread executing the command
     * @return the new job
     */
    public DefaultJob createJob(String command, Thread thread) {
        DefaultJob job = new DefaultJob(nextId.getAndIncrement(), command, thread);
        synchronized (managedJobs) {
            managedJobs.add(job);
        }
        fireChange(job, null, Job.Status.Foreground);
        return job;
    }

    /**
     * Marks a job as done and fires listeners.
     *
     * @param job the job to complete
     */
    public void completeJob(DefaultJob job) {
        Job.Status previous = job.status();
        job.setStatus(Job.Status.Done);
        fireChange(job, previous, Job.Status.Done);
    }

    /**
     * Removes a job from the managed list.
     *
     * @param job the job to remove
     */
    public void removeJob(Job job) {
        synchronized (managedJobs) {
            managedJobs.remove(job);
        }
    }

    private void fireChange(Job job, Job.Status previous, Job.Status current) {
        for (JobListener listener : listeners) {
            listener.onChange(job, previous, current);
        }
    }
}
