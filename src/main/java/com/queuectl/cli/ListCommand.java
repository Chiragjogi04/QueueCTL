package com.queuectl.cli;

import java.util.List;

import com.queuectl.model.Job;
import com.queuectl.model.JobState;
import com.queuectl.service.JobStore;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List jobs by state")
public class ListCommand implements Runnable {

    @Option(names = "--state", required = true, description = "Job state (PENDING, COMPLETED, DEAD)")
    protected JobState state;

    @Override
    public void run() {
        List<Job> jobs = JobStore.getInstance().listJobsByState(state);
        if (jobs.isEmpty()) {
            System.out.println("No jobs found in state " + state);
            return;
        }

        System.out.printf("--- Jobs in %s state ---%n", state);
        for (Job job : jobs) {
            System.out.printf("ID: %s%n", job.getId());
            System.out.printf("  Command: %s%n", job.getCommand());
            System.out.printf("  Attempts: %d%n", job.getAttempts());
            if (state == JobState.DEAD && job.getOutput() != null) {
                System.out.printf("  Last Output: %s%n", job.getOutput().substring(0, Math.min(80, job.getOutput().length())) + "...");
            }
            System.out.println("---------------------------------");
        }
    }
}