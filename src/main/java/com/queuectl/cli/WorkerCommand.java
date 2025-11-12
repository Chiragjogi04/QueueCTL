package com.queuectl.cli;

import com.queuectl.service.WorkerService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "worker", description = "Manage worker processes")
public class WorkerCommand {

    @Command(name = "start", description = "Start worker processes")
    public void start(@Option(names = "--count", defaultValue = "1", description = "Number of workers") int count) {
        new WorkerService().start(count);
    }

    @Command(name = "stop", description = "Stop running workers gracefully")
    public void stop() {
        new WorkerService().stop();
    }
}