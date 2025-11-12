package com.queuectl.cli;

import java.util.Map;

import com.queuectl.model.JobState;
import com.queuectl.service.JobStore;
import com.queuectl.service.WorkerService;

import picocli.CommandLine.Command;

@Command(name = "status", description = "Show summary of all job states & active workers")
public class StatusCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("--- Worker Status ---");
        String pid = WorkerService.getRunningPid();
        if (pid != null) {
            System.out.println("Workers are RUNNING (PID: " + pid + ")");
        } else {
            System.out.println("Workers are STOPPED");
        }

        System.out.println("\n--- Job Queue Status ---");
        Map<JobState, Integer> summary = JobStore.getInstance().getStatusSummary();
        if (summary.isEmpty()) {
            System.out.println("Queue is empty.");
        } else {
            for (JobState state : JobState.values()) {
                System.out.printf("%-10s: %d%n", state.name(), summary.getOrDefault(state, 0));
            }
        }
    }
}