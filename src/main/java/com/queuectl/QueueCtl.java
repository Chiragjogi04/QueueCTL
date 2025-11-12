package com.queuectl;

import com.queuectl.cli.ConfigCommand;
import com.queuectl.cli.DashboardCommand;
import com.queuectl.cli.DlqCommand;
import com.queuectl.cli.EnqueueCommand;
import com.queuectl.cli.InfoCommand;
import com.queuectl.cli.ListCommand;
import com.queuectl.cli.LogsCommand;
import com.queuectl.cli.StatusCommand;
import com.queuectl.cli.WorkerCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "queuectl",
    mixinStandardHelpOptions = true,
    version = "queuectl 1.0.0",
    description = "A CLI-based background job queue system.",
    subcommands = {
        EnqueueCommand.class,
        WorkerCommand.class,
        StatusCommand.class,
        ListCommand.class,
        DlqCommand.class,
        ConfigCommand.class,
        LogsCommand.class,
        DashboardCommand.class,
        InfoCommand.class
    }
)
public class QueueCtl implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        try {
            com.queuectl.service.JobStore.getInstance().initializeDatabase();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        int exitCode = new CommandLine(new QueueCtl()).execute(args);
        System.exit(exitCode);
    }
}