package com.queuectl.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.queuectl.model.Job;
import com.queuectl.model.JobState;

public class JobStore {
    private static final String DB_URL = "jdbc:sqlite:queuectl.db";
    private static JobStore instance;

    private JobStore() {}

    public static synchronized JobStore getInstance() {
        if (instance == null) {
            instance = new JobStore();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    public void initializeDatabase() {
        String jobsTable = "CREATE TABLE IF NOT EXISTS jobs (" +
                "id TEXT PRIMARY KEY, " +
                "command TEXT NOT NULL, " +
                "state TEXT NOT NULL, " +
                "attempts INTEGER NOT NULL, " +
                "max_retries INTEGER NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "next_execution_time INTEGER NOT NULL, " +
                "priority INTEGER NOT NULL DEFAULT 0, " +
                "timeout INTEGER NOT NULL DEFAULT 300, " +
                "output TEXT" +
                ");";

        String configTable = "CREATE TABLE IF NOT EXISTS config (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT NOT NULL" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(jobsTable);
            stmt.execute(configTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public boolean enqueueJob(Job job) {
        String sql = "INSERT INTO jobs(id, command, state, attempts, max_retries, " +
                "created_at, updated_at, next_execution_time, " +
                "priority, timeout, output) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, job.getId());
            pstmt.setString(2, job.getCommand());
            pstmt.setString(3, job.getState().name());
            pstmt.setInt(4, job.getAttempts());
            pstmt.setInt(5, job.getMaxRetries());
            pstmt.setLong(6, job.getCreatedAt());
            pstmt.setLong(7, job.getUpdatedAt());
            pstmt.setLong(8, job.getNextExecutionTime());
            pstmt.setInt(9, job.getPriority());
            pstmt.setInt(10, job.getTimeout());
            pstmt.setString(11, job.getOutput());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Enqueue Error: " + e.getMessage());
            return false;
        }
    }

    public Job findJobById(String id) {
        String sql = "SELECT * FROM jobs WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToJob(rs);
            }
        } catch (SQLException e) {
            System.err.println("Find Error: " + e.getMessage());
        }
        return null;
    }

    public boolean updateJob(Job job) {
        String sql = "UPDATE jobs SET state = ?, attempts = ?, updated_at = ?, next_execution_time = ?, output = ? " +
                "WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, job.getState().name());
            pstmt.setInt(2, job.getAttempts());
            pstmt.setLong(3, job.getUpdatedAt());
            pstmt.setLong(4, job.getNextExecutionTime());
            pstmt.setString(5, job.getOutput());
            pstmt.setString(6, job.getId());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Update Error: " + e.getMessage());
            return false;
        }
    }

    public List<Job> listJobsByState(JobState state) {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT * FROM jobs WHERE state = ? ORDER BY created_at ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, state.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                jobs.add(mapRowToJob(rs));
            }
        } catch (SQLException e) {
            System.err.println("List Error: " + e.getMessage());
        }
        return jobs;
    }

    public Map<JobState, Integer> getStatusSummary() {
        Map<JobState, Integer> summary = new HashMap<>();
        String sql = "SELECT state, COUNT(*) as count FROM jobs GROUP BY state";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JobState state = JobState.valueOf(rs.getString("state"));
                int count = rs.getInt("count");
                summary.put(state, count);
            }
        } catch (SQLException e) {
            System.err.println("Status Error: " + e.getMessage());
        }
        return summary;
    }


    public synchronized Job findAndLockNextJob() {
        String selectSql = "SELECT * FROM jobs " +
                "WHERE (state = 'PENDING' OR (state = 'FAILED' AND next_execution_time <= ?)) " +
                "ORDER BY priority DESC, created_at ASC LIMIT 1";
        
        String updateSql = "UPDATE jobs SET state = 'PROCESSING', updated_at = ?, attempts = ? " +
                "WHERE id = ? AND (state = 'PENDING' OR state = 'FAILED')";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            Job job = null;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setLong(1, System.currentTimeMillis());
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    job = mapRowToJob(rs);
                }
            }

            if (job != null) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    long now = System.currentTimeMillis();
                    int newAttempts = job.getAttempts() + 1;
                    
                    updateStmt.setLong(1, now);
                    updateStmt.setInt(2, newAttempts);
                    updateStmt.setString(3, job.getId());

                    int rowsAffected = updateStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        conn.commit();
                        job.setState(JobState.PROCESSING);
                        job.setUpdatedAt(now);
                        job.setAttempts(newAttempts);
                        return job;
                    } else {
                        conn.rollback();
                        return null;
                    }
                }
            }
            conn.commit();
            return null;
        } catch (SQLException e) {
            System.err.println("Locking Error: " + e.getMessage());
            return null;
        }
    }

    private Job mapRowToJob(ResultSet rs) throws SQLException {
        Job job = new Job();
        job.setId(rs.getString("id"));
        job.setCommand(rs.getString("command"));
        job.setState(JobState.valueOf(rs.getString("state")));
        job.setAttempts(rs.getInt("attempts"));
        job.setMaxRetries(rs.getInt("max_retries"));
        job.setCreatedAt(rs.getLong("created_at"));
        job.setUpdatedAt(rs.getLong("updated_at"));
        job.setNextExecutionTime(rs.getLong("next_execution_time"));
        job.setPriority(rs.getInt("priority"));
        job.setTimeout(rs.getInt("timeout"));
        job.setOutput(rs.getString("output"));
        return job;
    }
}