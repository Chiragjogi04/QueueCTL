package com.queuectl.cli;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuectl.model.Job;
import com.queuectl.model.JobState;
import com.queuectl.service.JobStore;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "enqueue", description = "Add a new job to the queue")
public class EnqueueCommand implements Runnable {

    @Parameters(index = "0", description = "The job specification in JSON format")
    private String jobJson;

    @Override
    public void run() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Job job = mapper.readValue(jobJson, Job.class);

            if (job.getId() == null || job.getId().isEmpty()) {
                job.setId(UUID.randomUUID().toString());
            }
            job.setState(JobState.PENDING);
            job.setAttempts(0);
            long now = System.currentTimeMillis();
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            job.setNextExecutionTime(now);

            if (job.getCommand() == null || job.getCommand().isEmpty()) {
                System.err.println("Error: 'command' field is required.");
                return;
            }

            if (JobStore.getInstance().enqueueJob(job)) {
                System.out.println("Job enqueued with ID: " + job.getId());
            } else {
                System.err.println("Failed to enqueue job.");
            }

        } catch (Exception e) {
            System.err.println("Error parsing job JSON: " + e.getMessage());
        }
    }
}