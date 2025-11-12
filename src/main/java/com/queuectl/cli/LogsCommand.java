package com.queuectl.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "logs", description = "View persistent logs for a job")
public class LogsCommand implements Runnable {

    @Parameters(index = "0", description = "The ID of the job to view logs for")
    private String jobId;

    @Override
    public void run() {
        try {
            Path logPath = Paths.get(System.getProperty("user.home"), ".queuectl", "logs", jobId + ".log");
            if (!Files.exists(logPath)) {
                System.err.println("No logs found for job: " + jobId);
                return;
            }
            String logs = new String(Files.readAllBytes(logPath));
            System.out.println("--- Logs for " + jobId + " ---");
            System.out.println(logs);
        } catch (Exception e) {
            System.err.println("Failed to read logs: " + e.getMessage());
        }
    }
}