package com.queuectl.cli;

import java.util.HashMap;

import com.queuectl.model.JobState;
import com.queuectl.service.JobStore;
import com.queuectl.service.WorkerService;

import io.javalin.Javalin;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "dashboard", description = "Start a minimal web dashboard")
public class DashboardCommand implements Runnable {

    @Option(names = {"-p", "--port"}, defaultValue = "7070", description = "Port to run the dashboard on")
    private int port;

    @Override
    public void run() {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public"); 
        }).start(port);

        System.out.println("Dashboard running at http://localhost:" + port);

        app.get("/api/status", ctx -> {
            var summary = JobStore.getInstance().getStatusSummary();
            var statusMap = new HashMap<String, Integer>();
            for (var entry : summary.entrySet()) {
                statusMap.put(entry.getKey().name(), entry.getValue());
            }
            statusMap.put("WORKERS_RUNNING", WorkerService.isAlreadyRunning() ? 1 : 0);
            ctx.json(statusMap);
        });

        app.get("/api/jobs/pending", ctx -> {
            ctx.json(JobStore.getInstance().listJobsByState(JobState.PENDING));
        });
        app.get("/api/jobs/completed", ctx -> {
            ctx.json(JobStore.getInstance().listJobsByState(JobState.COMPLETED));
        });
        app.get("/api/jobs/dead", ctx -> {
            ctx.json(JobStore.getInstance().listJobsByState(JobState.DEAD));
        });
        app.get("/api/jobs/processing", ctx -> {
            ctx.json(JobStore.getInstance().listJobsByState(JobState.PROCESSING));
        });
        
        app.get("/api/jobs/failed", ctx -> {
            ctx.json(JobStore.getInstance().listJobsByState(JobState.FAILED));
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping dashboard...");
            app.stop();
            System.out.println("Dashboard stopped.");
        }));

        try {
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Dashboard interrupted");
        }
    }
}