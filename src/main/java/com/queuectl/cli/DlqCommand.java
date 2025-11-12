package com.queuectl.cli;

import com.queuectl.model.Job;
import com.queuectl.model.JobState;
import com.queuectl.service.JobStore;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "dlq", description = "Manage the Dead Letter Queue (DLQ)")
public class DlqCommand {

    @Command(name = "list", description = "View all jobs in the DLQ")
    public void list() {
        new ListCommand() {
            @Override
            public void run() {
                super.state = JobState.DEAD;
                super.run();
            }
        }.run();
    }

    @Command(name = "retry", description = "Retry a specific job from the DLQ")
    public void retry(@CommandLine.Parameters(index = "0", description = "The ID of the job to retry") String jobId) {
        JobStore jobStore = JobStore.getInstance();
        Job job = jobStore.findJobById(jobId);

        if (job == null) {
            System.err.println("Error: Job not found: " + jobId);
            return;
        }

        if (job.getState() != JobState.DEAD) {
            System.err.println("Error: Job is not in the DLQ. Current state: " + job.getState());
            return;
        }

        job.setState(JobState.PENDING);
        job.setAttempts(0);
        job.setUpdatedAt(System.currentTimeMillis());
        job.setNextExecutionTime(System.currentTimeMillis());

        if (jobStore.updateJob(job)) {
            System.out.println("Job " + jobId + " has been requeued.");
        } else {
            System.err.println("Failed to retry job " + jobId);
        }
    }
}