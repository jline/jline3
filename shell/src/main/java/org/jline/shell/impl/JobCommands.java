/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.List;

import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.jline.shell.Job;
import org.jline.shell.JobManager;

/**
 * Built-in job control commands: {@code jobs}, {@code fg}, and {@code bg}.
 * <p>
 * This command group is automatically added to {@link DefaultCommandDispatcher}
 * when a {@link JobManager} is configured.
 *
 * @since 4.0
 */
public class JobCommands extends SimpleCommandGroup {

    /**
     * Creates job control commands backed by the given job manager.
     *
     * @param jobManager the job manager
     */
    public JobCommands(JobManager jobManager) {
        super("jobs", createCommands(jobManager));
    }

    private static List<Command> createCommands(JobManager jobManager) {
        return List.of(new JobsCommand(jobManager), new FgCommand(jobManager), new BgCommand(jobManager));
    }

    private static class JobsCommand extends AbstractCommand {
        private final JobManager jobManager;

        JobsCommand(JobManager jobManager) {
            super("jobs");
            this.jobManager = jobManager;
        }

        @Override
        public String description() {
            return "List all tracked jobs";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            List<Job> jobs = jobManager.jobs();
            if (jobs.isEmpty()) {
                session.out().println("No jobs.");
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Job job : jobs) {
                if (job.status() != Job.Status.Done) {
                    sb.append(String.format("[%d]  %-12s %s%n", job.id(), job.status(), job.command()));
                }
            }
            if (sb.length() == 0) {
                session.out().println("No active jobs.");
            } else {
                session.out().print(sb);
            }
            return null;
        }
    }

    private static class FgCommand extends AbstractCommand {
        private final JobManager jobManager;

        FgCommand(JobManager jobManager) {
            super("fg");
            this.jobManager = jobManager;
        }

        @Override
        public String description() {
            return "Bring a job to the foreground";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            Job job = resolveJob(jobManager, session, args);
            if (job == null) {
                return null;
            }
            if (job.status() == Job.Status.Done) {
                session.err().println("fg: job " + job.id() + " has already completed");
                return null;
            }
            if (job.status() == Job.Status.Foreground) {
                session.err().println("fg: job " + job.id() + " is already in the foreground");
                return null;
            }
            session.out().println("[" + job.id() + "]  " + job.command());
            job.resume(true);
            // Wait for the job thread to finish
            if (job instanceof DefaultJob) {
                ((DefaultJob) job).thread().join();
            }
            return null;
        }
    }

    private static class BgCommand extends AbstractCommand {
        private final JobManager jobManager;

        BgCommand(JobManager jobManager) {
            super("bg");
            this.jobManager = jobManager;
        }

        @Override
        public String description() {
            return "Resume a job in the background";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            Job job = resolveJob(jobManager, session, args);
            if (job == null) {
                return null;
            }
            if (job.status() == Job.Status.Done) {
                session.err().println("bg: job " + job.id() + " has already completed");
                return null;
            }
            if (job.status() == Job.Status.Background) {
                session.err().println("bg: job " + job.id() + " is already in the background");
                return null;
            }
            job.resume(false);
            session.out().println("[" + job.id() + "]  " + job.command() + " &");
            return null;
        }
    }

    /**
     * Resolves a job from arguments. If an id is given, looks it up; otherwise uses the most recent
     * non-done job.
     */
    private static Job resolveJob(JobManager jobManager, CommandSession session, String[] args) {
        if (args.length > 0) {
            try {
                long id = Long.parseLong(args[0]);
                Job job = jobManager.get(id);
                if (job == null) {
                    session.err().println("No such job: " + id);
                }
                return job;
            } catch (NumberFormatException e) {
                session.err().println("Invalid job id: " + args[0]);
                return null;
            }
        }
        // Find the most recent non-done job
        List<Job> jobs = jobManager.jobs();
        for (int i = jobs.size() - 1; i >= 0; i--) {
            Job job = jobs.get(i);
            if (job.status() != Job.Status.Done) {
                return job;
            }
        }
        session.err().println("No current job");
        return null;
    }
}
