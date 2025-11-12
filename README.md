# QueueCTL
queuectl is a robust job queue system built in Java. It is designed to run shell commands as background jobs, manage parallel workers, and gracefully handle failures with retries and a Dead Letter Queue (DLQ).
It uses SQLite for persistence, picocli for the CLI, and Javalin for a live web dashboard.
