# **queuectl: A Persistent CLI Job Queue**

`queuectl` is a robust, **job queue system** built in **Java**.  
It runs shell commands as background jobs, manages parallel workers, and gracefully handles failures with retries and a **Dead Letter Queue (DLQ)**.  

It uses **SQLite** for persistence, **picocli** for the CLI, and **Javalin** for a live web dashboard.

---

## **Features**

- **Persistent Storage** — Jobs are stored in an SQLite database (`queuectl.db`) and survive restarts.  
- **Parallel Workers** — Run multiple worker threads (`worker start --count 5`) to process jobs concurrently.  
- **Job Priority Queues** — Enqueue jobs with priority to ensure important tasks run first.  
- **Automatic Retries** — Failed jobs are retried automatically with configurable exponential backoff.  
- **Dead Letter Queue (DLQ)** — Jobs that exhaust all retries are moved to a DEAD state for manual inspection.  
- **Job Timeouts** — Prevent stalled jobs by setting a timeout (in seconds).  
- **Persistent Logging** — Each job’s `stdout` and `stderr` are saved at `~/.queuectl/logs/<job-id>.log`.  
- **Live Web Dashboard** — Built-in web UI to monitor job states and worker activity in real-time.  

---

## **Job Lifecycle**

Jobs move through a clearly defined lifecycle managed by the workers:

| State | Description |
|:------|:-------------|
| 🕓 **PENDING** | New jobs waiting for a worker. |
| ⚙️ **PROCESSING** | Worker is executing the command. |
| ❌ **FAILED** | Command failed, but retries remain. |
| ✅ **COMPLETED** | Job finished successfully (exit code 0). |
| 💀 **DEAD** | Job failed and has no retries left. |

**Workers pick up jobs that are:**
- `PENDING`
- `FAILED` and whose `next_execution_time` is in the past.

---

## **Build & Run**

### **Prerequisites**
- ☕ Java 11+
- 🧱 Maven

### **1. Build**

```bash
mvn clean package
This creates:
target/queuectl-1.0.0.jar
```
2. Set Up (Recommended)
To make queuectl behave like a native command, add an alias:
# Add this line to your .bashrc or .zshrc
alias queuectl="java -jar /full/path/to/your/queuectl/target/queuectl-1.0.0.jar"
🕹️ CLI Command Reference
dashboard
Starts the live web dashboard.
queuectl dashboard
# Default: http://localhost:7070
queuectl dashboard --port 8080
worker
Manages worker threads.
# Start 4 workers
queuectl worker start --count 4

# Stop workers
queuectl worker stop
enqueue
Adds a job to the queue (in JSON).
# Simple job
queuectl enqueue '{"id":"job1", "command":"echo Hello"}'

# Job with priority and timeout
queuectl enqueue '{"command":"sleep 10", "priority":10, "timeout":60}'
status
Displays system status summary.
queuectl status
Output Example:
--- Worker Status ---
Workers are RUNNING (PID: 12345)

--- Job Queue Status ---
PENDING   : 10
PROCESSING: 4
FAILED    : 2
COMPLETED : 52
DEAD      : 1
list
Lists all jobs in a given state.
queuectl list --state PENDING
queuectl list --state FAILED
info
Shows details for a single job.
queuectl info <job-id>
Example Output:
--- Job Details ---
ID:        test-timeout
State:     DEAD
Command:   sleep 20
Priority:  0
Attempts:  3 / 3
Timeout:   2 seconds
Created:   2025-11-12 11:30:00
Updated:   2025-11-12 11:30:08

--- Last Output ---
[ERROR] Job timed out after 2 seconds.
logs
Prints the full log for a job.
queuectl logs <job-id>
Example Output:
--- Logs for test-timeout ---

--- ATTEMPT 1 at 2025-11-12 11:30:02 ---
[ERROR] Job timed out after 2 seconds.

--- ATTEMPT 2 at 2025-11-12 11:30:04 ---
[ERROR] Job timed out after 2 seconds.

--- ATTEMPT 3 at 2025-11-12 11:30:08 ---
[ERROR] Job timed out after 2 seconds.
dlq
Manages the Dead Letter Queue.
# List all DEAD jobs
queuectl dlq list

# Retry a job
queuectl dlq retry <job-id>
config
Configure global settings.
# Set max retries
queuectl config set max-retries 5

# Set backoff base
queuectl config set backoff-base 3
📄 Job Specification
Field	Type	Default	Description
id	String	UUID	Unique job ID (auto-generated if omitted).
command	String	Required	Shell command to execute.
priority	Integer	0	Higher = runs earlier.
timeout	Integer	300	Max seconds before killing the job.
max_retries	Integer	3	Overrides global retry limit.
🙏 Acknowledgements
This project was made possible thanks to the following libraries:
⚡ picocli — Type-safe CLI framework.
🌐 Javalin — Lightweight web framework for the dashboard.
💾 SQLite-JDBC — File-based database engine.
📦 Jackson — Fast JSON parsing library.
📚 Further Reading
1️⃣ Conceptual Foundations (Queuing Theory)
Erlang, A. K. (1909). The theory of probabilities and telephone conversations.
Nyt Tidsskrift for Matematik B, 20, 33–39.
Harchol-Balter, M. (2013). Performance Modeling and Design of Computer Systems: Queueing Theory in Action. Cambridge University Press.
2️⃣ Operating System Scheduling
Silberschatz, A., Galvin, P. B., & Gagne, G. (2018). Operating System Concepts (10th ed.). Wiley.
3️⃣ Modern System Design Patterns
Hohpe, G., & Woolf, B. (2003). Enterprise Integration Patterns. Addison-Wesley.
Richardson, C. (2018). Microservices Patterns. Manning.
💡 Author
queuectl — A Modern Java CLI Job Queue System for Distributed Automation.
Maintained with ❤️ by [Your Name / Team].
