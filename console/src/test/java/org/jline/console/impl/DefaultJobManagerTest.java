/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.console.Job;
import org.jline.console.JobListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultJobManager} and {@link DefaultJob}.
 */
public class DefaultJobManagerTest {

    private DefaultJobManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultJobManager();
    }

    @Test
    void createJobAssignsIncrementingIds() {
        DefaultJob job1 = manager.createJob("cmd1", Thread.currentThread());
        DefaultJob job2 = manager.createJob("cmd2", Thread.currentThread());
        assertEquals(1, job1.id());
        assertEquals(2, job2.id());
    }

    @Test
    void createJobSetsForegroundStatus() {
        DefaultJob job = manager.createJob("test", Thread.currentThread());
        assertEquals(Job.Status.Foreground, job.status());
    }

    @Test
    void createJobStoresCommand() {
        DefaultJob job = manager.createJob("echo hello", Thread.currentThread());
        assertEquals("echo hello", job.command());
    }

    @Test
    void foregroundJobReturnsCurrentForeground() {
        assertNull(manager.foregroundJob());
        DefaultJob job = manager.createJob("test", Thread.currentThread());
        assertSame(job, manager.foregroundJob());
    }

    @Test
    void completeJobSetsStatusToDone() {
        DefaultJob job = manager.createJob("test", Thread.currentThread());
        assertEquals(Job.Status.Foreground, job.status());
        manager.completeJob(job);
        assertEquals(Job.Status.Done, job.status());
    }

    @Test
    void completeJobMakesNoForeground() {
        DefaultJob job = manager.createJob("test", Thread.currentThread());
        manager.completeJob(job);
        assertNull(manager.foregroundJob());
    }

    @Test
    void jobsReturnsAllJobs() {
        assertTrue(manager.jobs().isEmpty());
        DefaultJob job1 = manager.createJob("cmd1", Thread.currentThread());
        DefaultJob job2 = manager.createJob("cmd2", Thread.currentThread());
        List<Job> jobs = manager.jobs();
        assertEquals(2, jobs.size());
        assertTrue(jobs.contains(job1));
        assertTrue(jobs.contains(job2));
    }

    @Test
    void getReturnsJobById() {
        DefaultJob job = manager.createJob("test", Thread.currentThread());
        assertSame(job, manager.get(job.id()));
        assertNull(manager.get(999));
    }

    @Test
    void listenerFiredOnCreate() {
        List<String> events = new ArrayList<>();
        manager.addJobListener((job, prev, curr) -> events.add(prev + "->" + curr));
        manager.createJob("test", Thread.currentThread());
        assertEquals(1, events.size());
        assertEquals("null->Foreground", events.get(0));
    }

    @Test
    void listenerFiredOnComplete() {
        List<String> events = new ArrayList<>();
        DefaultJob job = manager.createJob("test", Thread.currentThread());
        manager.addJobListener((j, prev, curr) -> events.add(prev + "->" + curr));
        manager.completeJob(job);
        assertEquals(1, events.size());
        assertEquals("Foreground->Done", events.get(0));
    }

    @Test
    void removeListenerStopsNotification() {
        List<String> events = new ArrayList<>();
        JobListener listener = (j, prev, curr) -> events.add("fired");
        manager.addJobListener(listener);
        manager.createJob("test1", Thread.currentThread());
        assertEquals(1, events.size());
        manager.removeJobListener(listener);
        manager.createJob("test2", Thread.currentThread());
        assertEquals(1, events.size());
    }

    @Test
    void jobSuspendAndResume() {
        DefaultJob job = manager.createJob("test", Thread.currentThread());
        assertEquals(Job.Status.Foreground, job.status());
        job.suspend();
        assertEquals(Job.Status.Suspended, job.status());
        job.resume(false);
        assertEquals(Job.Status.Background, job.status());
        job.resume(true);
        assertEquals(Job.Status.Foreground, job.status());
    }

    @Test
    void jobInterruptInterruptsThread() throws InterruptedException {
        Thread testThread = new Thread(() -> {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                // expected
            }
        });
        testThread.start();
        DefaultJob job = manager.createJob("test", testThread);
        job.interrupt();
        testThread.join(5000);
        assertFalse(testThread.isAlive());
    }
}
