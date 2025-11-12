package com.queuectl.cli;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.queuectl.model.Job;
import com.queuectl.service.JobStore;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "info", description = "Get all details for a specific job")
public class InfoCommand implements Runnable {

    @Parameters(index = "0", description = "The ID of the job to inspect")
    private String jobId;

    @Override
    public void run() {
        Job job = JobStore.getInstance().findJobById(jobId);

        if (job == null) {
            System.err.println("Error: Job not found: " + jobId);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("--- Job Details ---");
        System.out.printf("ID:        %s%n", job.getId());
        System.out.printf("State:     %s%n", job.getState());
        System.out.printf("Command:   %s%n", job.getCommand());
        System.out.printf("Priority:  %d%n", job.getPriority());
        System.out.printf("Attempts:  %d / %d%n", job.getAttempts(), job.getMaxRetries());
        System.out.printf("Timeout:   %d seconds%n", job.getTimeout());
        System.out.printf("Created:   %s%n", sdf.format(new Date(job.getCreatedAt())));
        System.out.printf("Updated:   %s%n", sdf.format(new Date(job.getUpdatedAt())));
        
        if (job.getState() == com.queuectl.model.JobState.FAILED) {
             System.out.printf("Next Try:  %s%n", sdf.format(new Date(job.getNextExecutionTime())));
        }

        System.out.println("--- Last Output ---");
        if (job.getOutput() != null && !job.getOutput().isEmpty()) {
            System.out.println(job.getOutput());
        } else {
            System.out.println("(No output recorded)");
        }
    }
}