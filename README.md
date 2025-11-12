🚀 queuectl: A Persistent CLI Job Queue

queuectl is a robust, file-based job queue system built in Java. It is designed to run shell commands as background jobs, manage parallel workers, and gracefully handle failures with retries and a Dead Letter Queue (DLQ).

It uses SQLite for persistence, picocli for the CLI, and Javalin for a live web dashboard.

✨ Features

Persistent Storage: Jobs are stored in an SQLite database (queuectl.db) and survive application restarts.

Parallel Workers: Run multiple worker threads (worker start --count 5) to process jobs concurrently.

Job Priority Queues: Enqueue jobs with a priority to ensure important tasks run first.

Automatic Retries: Failed jobs are automatically retried with configurable exponential backoff.

Job Lifecycle: A clear 5-state lifecycle (PENDING, PROCESSING, FAILED, COMPLETED, DEAD).

Dead Letter Queue (DLQ): Jobs that exhaust all retries are moved to a DEAD state for manual inspection and retry.

Job Timeouts: Protect workers from stalled jobs by setting a timeout (in seconds) on any job.

Persistent Logging: The full stdout and stderr output of every job attempt is saved to ~/.queuectl/logs/<job-id>.log.

Live Web Dashboard: A built-in web UI to monitor all job states and worker status in real-time.

🔄 Job Lifecycle

Jobs move through a clearly defined lifecycle, managed by the workers.

PENDING: The default state for new jobs waiting for a worker.

PROCESSING: A worker has locked the job and is actively executing its command.

FAILED: The job's command failed (non-zero exit or timeout) but it still has retries left. It will wait for its backoff period to end.

COMPLETED: The job's command finished successfully (exit code 0).

DEAD: The job failed and has no retries left. It will remain here until manually retried.

A worker will pick up any job that is:

PENDING

FAILED and its next_execution_time is in the past.

🛠️ Build & Run

Prerequisites

Java 11+

Maven

1. Build

Clone the repository and use Maven to create the executable "fat jar":

# Compile and package all dependencies into one jar
mvn clean package


This creates target/queuectl-1.0.0.jar.

2. Set Up (Recommended)

To make queuectl feel like a real command, create an alias in your .bashrc or .zshrc:

# Add this to your shell profile
alias queuectl="java -jar /full/path/to/your/queuectl/target/queuectl-1.0.0.jar"


🕹️ CLI Command Reference

dashboard

Starts the live web dashboard.

queuectl dashboard
# By default, runs on http://localhost:7070
queuectl dashboard --port 8080


worker

Manages the worker processes. This command runs in the foreground.

# Start 4 worker threads in the current terminal
queuectl worker start --count 4

# Stop workers from another terminal
queuectl worker stop


enqueue

Adds a new job to the queue. Takes a single JSON string.

# Simple job
queuectl enqueue '{"id":"job1", "command":"echo Hello"}'

# Job with priority and a 60s timeout
queuectl enqueue '{"command":"sleep 10", "priority": 10, "timeout": 60}'


status

Shows a high-level summary of the system.

queuectl status


Output:

--- Worker Status ---
Workers are RUNNING (PID: 12345)

--- Job Queue Status ---
PENDING   : 10
PROCESSING: 4
FAILED    : 2
COMPLETED : 52
DEAD      : 1


list

Lists all job IDs for a given state.

queuectl list --state PENDING
queuectl list --state FAILED


info

Gets all details for a single job.

queuectl info <job-id>


Output:

--- Job Details ---
ID:        test-timeout
State:     FAILED
Command:   sleep 20
Priority:  0
Attempts:  1 / 3
Timeout:   2 seconds
Created:   2025-11-12 11:30:00
Updated:   2025-11-12 11:30:02
Next Try:  2025-11-12 11:30:04
--- Last Output ---
[ERROR] Job timed out after 2 seconds.


logs

Prints the full, persistent log file for a job.

queuectl logs <job-id>


Output:

--- Logs for test-timeout ---

--- ATTEMPT 1 at 2025-11-12 11:30:02 ---
[ERROR] Job timed out after 2 seconds.


dlq

Manages the Dead Letter Queue (DLQ).

# List all jobs in the DEAD state
queuectl dlq list

# Move a job from DEAD back to PENDING to be retried
queuectl dlq retry <job-id>


config

Sets system-wide configuration.

# Set max retries to 5 (default 3)
queuectl config set max-retries 5

# Set backoff base to 3 (delay = 3^attempts seconds)
queuectl config set backoff-base 3


📄 Job Specification

The JSON for the enqueue command. Only command is required.

Field

Type

Default

Description

id

String

(UUID)

A unique ID for the job. Auto-generated if omitted.

command

String

Required

The shell command to execute (e.g., echo 'hi').

priority

Integer

0

Higher numbers run first.

timeout

Integer

300

Max seconds the job can run before being killed.

max_retries

Integer

3

Overrides the global config for this job.

🚀 Quick Start (3-Terminal Test)

Terminal 1: Start the Dashboard

queuectl dashboard


Open http://localhost:7070 in your browser.

Terminal 2: Start the Workers

queuectl worker start --count 2


You will see the "Workers" card turn RUNNING on the dashboard.

Terminal 3: Enqueue Jobs

# Enqueue a job that works
queuectl enqueue '{"command":"echo Hello World", "priority": 1}'

# Enqueue a job that fails
queuectl enqueue '{"command":"this-will-fail", "priority": 10}'


Watch the dashboard. You will see the high-priority "this-will-fail" job get processed first. It will move to FAILED and then DEAD. The "Hello World" job will then run and move to COMPLETED.
