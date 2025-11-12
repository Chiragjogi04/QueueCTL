package com.queuectl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {
    private String id;
    private String command;
    private JobState state;
    private int attempts;
    
    @JsonProperty("max_retries")
    private int maxRetries = 3;
    
    @JsonProperty("created_at")
    private long createdAt;
    
    @JsonProperty("updated_at")
    private long updatedAt;
    
    private long nextExecutionTime;
    private String output;

    @JsonProperty("priority")
    private int priority = 0;

    @JsonProperty("timeout")
    private int timeout = 300;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public JobState getState() { return state; }
    public void setState(JobState state) { this.state = state; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public long getNextExecutionTime() { return nextExecutionTime; }
    public void setNextExecutionTime(long nextExecutionTime) { this.nextExecutionTime = nextExecutionTime; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    @Override
    public String toString() {
        return String.format("Job[ID=%s, State=%s, Prio=%d, Attempts=%d, Command=%s]",
            id, state, priority, attempts, command);
    }
}