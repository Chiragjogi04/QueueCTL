package com.queuectl.worker;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.queuectl.model.Job;
import com.queuectl.model.JobState;
import com.queuectl.service.ConfigService;
import com.queuectl.service.JobStore;

public class JobWorker implements Runnable {
    private final JobStore jobStore;
    private final ConfigService configService;
    private volatile boolean running = true;

    public JobWorker() {
        this.jobStore = JobStore.getInstance();
        this.configService = new ConfigService();
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        System.out.printf("[Worker %s] Starting...%n", Thread.currentThread().getName());
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Job job = jobStore.findAndLockNextJob();
                if (job != null) {
                    executeJob(job);
                } else {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                System.err.printf("[Worker %s] Error in loop: %s%n",
                        Thread.currentThread().getName(), e.getMessage());
            }
        }
        System.out.printf("[Worker %s] Stopping...%n", Thread.currentThread().getName());
    }

    private void executeJob(Job job) {
        System.out.printf("[Worker %s] Processing Job %s: %s%n",
                Thread.currentThread().getName(), job.getId(), job.getCommand());

        Process process = null;
        StringBuilder output = new StringBuilder();
        Path logPath = Paths.get(System.getProperty("user.home"), ".queuectl", "logs", job.getId() + ".log");

        try {
            Files.createDirectories(logPath.getParent());
            try (FileWriter logWriter = new FileWriter(logPath.toFile(), true)) {

                logWriter.write(String.format("%n--- ATTEMPT %d at %s ---%n",
                        job.getAttempts(), new Date()));

                ProcessBuilder pb = new ProcessBuilder("sh", "-c", job.getCommand());
                process = pb.start();

                StreamGobbler stdOutGobbler = new StreamGobbler(process.getInputStream(), logWriter, output, "");
                StreamGobbler stdErrGobbler = new StreamGobbler(process.getErrorStream(), logWriter, output, "[ERROR] ");

                Thread outThread = new Thread(stdOutGobbler);
                Thread errThread = new Thread(stdErrGobbler);
                outThread.start();
                errThread.start();
                
                boolean finished = process.waitFor(job.getTimeout(), TimeUnit.SECONDS);

                outThread.join(1000); 
                errThread.join(1000);

                job.setOutput(output.toString().trim());

                if (!finished) {
                    process.destroyForcibly();
                    job.setOutput(output.append(String.format("%n[ERROR] Job timed out after %d seconds.", job.getTimeout())).toString());
                    handleFailure(job);
                } else if (process.exitValue() == 0) {
                    job.setState(JobState.COMPLETED);
                    System.out.printf("[Worker %s] Job %s COMPLETED%n", Thread.currentThread().getName(), job.getId());
                } else {
                    job.setOutput(output.append(String.format("%n[ERROR] Exited with code %d.", process.exitValue())).toString());
                    handleFailure(job);
                }
            }
        } catch (Exception e) {
            job.setOutput(output.append("\n[ERROR] Execution failed: ").append(e.getMessage()).toString());
            handleFailure(job);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            job.setUpdatedAt(System.currentTimeMillis());
            jobStore.updateJob(job);
        }
    }

    private void handleFailure(Job job) {
        int maxRetries = configService.getConfigAsInt("max-retries", 3);
        
        if (job.getMaxRetries() > 0) {
            maxRetries = job.getMaxRetries();
        }

        if (job.getAttempts() >= maxRetries) {
            job.setState(JobState.DEAD);
            System.out.printf("[Worker %s] Job %s FAILED (Max retries). Moved to DLQ.%n",
                    Thread.currentThread().getName(), job.getId());
        } else {
            job.setState(JobState.FAILED);
            long backoffBase = configService.getConfigAsInt("backoff-base", 2);
            long delayMillis = (long) Math.pow(backoffBase, job.getAttempts()) * 1000;
            job.setNextExecutionTime(System.currentTimeMillis() + delayMillis);
            System.out.printf("[Worker %s] Job %s FAILED. Retrying in %dms.%n",
                    Thread.currentThread().getName(), job.getId(), delayMillis);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private FileWriter logWriter;
        private StringBuilder output;
        private String prefix;

        public StreamGobbler(InputStream inputStream, FileWriter logWriter, StringBuilder output, String prefix) {
            this.inputStream = inputStream;
            this.logWriter = logWriter;
            this.output = output;
            this.prefix = prefix;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String logLine = prefix + line;
                    synchronized (output) {
                        output.append(logLine).append("\n");
                    }
                    synchronized (logWriter) {
                        logWriter.write(logLine + "\n");
                    }
                }
            } catch (Exception e) {
                System.err.println("LogGobbler error: " + e.getMessage());
            }
        }
    }
}