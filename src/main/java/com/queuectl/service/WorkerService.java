package com.queuectl.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.queuectl.worker.JobWorker;

public class WorkerService {
    private static final Path PID_FILE = Paths.get(System.getProperty("user.home"), ".queuectl", "worker.pid");
    private ExecutorService executor;
    private List<JobWorker> workers;

    public void start(int count) {
        if (isAlreadyRunning()) {
            System.err.println("Workers already running (PID: " + getRunningPid() + "). Stop them first.");
            return;
        }

        System.out.printf("Starting %d workers...%n", count);
        executor = Executors.newFixedThreadPool(count);
        workers = new ArrayList<>();

        try {
            writePidFile();
        } catch (IOException e) {
            System.err.println("Could not write PID file: " + e.getMessage());
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        for (int i = 0; i < count; i++) {
            JobWorker worker = new JobWorker();
            workers.add(worker);
            executor.submit(worker);
        }
        
        System.out.println("Workers started. Press Ctrl+C to stop.");
        
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        String pid = getRunningPid();
        if (pid == null) {
            System.out.println("No workers seem to be running.");
            return;
        }

        System.out.println("Sending stop signal to worker process (PID: " + pid + ")...");
        try {
            Runtime.getRuntime().exec("kill -SIGINT " + pid);
            
            for (int i = 0; i < 10; i++) {
                if (!Files.exists(PID_FILE)) {
                    System.out.println("Workers stopped successfully.");
                    return;
                }
                Thread.sleep(500);
            }
            System.err.println("Workers did not stop. You may need to kill the process manually.");
        } catch (Exception e) {
            System.err.println("Failed to stop workers: " + e.getMessage());
        }
    }

    private void shutdown() {
        System.out.println("\nGraceful shutdown initiated...");
        if (workers != null) {
            workers.forEach(JobWorker::stop);
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        deletePidFile();
        System.out.println("Workers shut down.");
    }

    private void writePidFile() throws IOException {
        String pid = String.valueOf(ProcessHandle.current().pid());
        Files.createDirectories(PID_FILE.getParent());
        Files.write(PID_FILE, pid.getBytes());
    }

    private void deletePidFile() {
        try {
            Files.deleteIfExists(PID_FILE);
        } catch (IOException e) {
            // ignore
        }
    }

    public static boolean isAlreadyRunning() {
        return Files.exists(PID_FILE);
    }

    public static String getRunningPid() {
        try {
            if (isAlreadyRunning()) {
                return new String(Files.readAllBytes(PID_FILE));
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }
}