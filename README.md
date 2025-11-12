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
```
This creates:
.jar file which is used to run the commands

### **2. Set Up (Recommended)**
To make queuectl behave like a native command, add an alias:
```bash
alias queuectl="java -jar /full/path/to/your/queuectl/target/queuectl-1.0.0.jar"
```

## **CLI Command Reference**
### 1.dashboard
Starts the live web dashboard.
```bash
queuectl dashboard
```
Default runs on: http://localhost:7070 but if you want to change the port you can use below command,
```bash
queuectl dashboard --port 8080
```

### 2.worker
Manages worker threads.
### Start 3 workers
```bash
queuectl worker start --count 3
```
### Stop workers
```bash
queuectl worker stop
```

### 3.enqueue
Adds a job to the queue (in JSON).
### Simple job
```bash
queuectl enqueue '{"id":"job1", "command":"echo Hello"}'
```
### Job with priority and timeout
```bash
queuectl enqueue '{"command":"sleep 10", "priority":10, "timeout":60}'
```

### 4.status
Displays system status summary.
```bash
queuectl status
```

### 5.list
Lists all jobs in a given state.
```bash
queuectl list --state PENDING
```

```bash
queuectl list --state FAILED
```

### 6.info
Shows details for a single job.
```bash
queuectl info <job-id>
```

### 7.logs
Prints the full log for a job.
```bash
queuectl logs <job-id>
```

### 8.dlq
Manages the Dead Letter Queue.
### List all DEAD jobs
```bash
queuectl dlq list
```

### Retry a job
```bash
queuectl dlq retry <job-id>
```

### 9.config
Configure global settings.
### Set max retries
```bash
queuectl config set max-retries 5
```

### Set backoff base
```bash
queuectl config set backoff-base 3
```

## 📄 Job Specification
| Field	Type |  Default	Description |
|:------|:-------------|
id - String | UUID	Unique job ID (auto-generated if omitted).
command	- String	| Required	Shell command to execute.
priority - Integer |	Higher = runs earlier.
timeout -	Integer |	300	Max seconds before killing the job.
max_retries	- Integer |	3	Overrides global retry limit.

## Tech Stack Used
This project was made possible using the following libraries:
- **picocli** — Type-safe CLI framework.
- **Javalin** — Lightweight web framework for the dashboard.
- **SQLite-JDBC** — File-based database engine.
- **Jackson** — Fast JSON parsing library.
