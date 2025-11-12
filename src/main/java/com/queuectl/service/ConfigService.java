package com.queuectl.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigService {
    private final JobStore jobStore = JobStore.getInstance();

    public void setConfig(String key, String value) {
        String sql = "REPLACE INTO config (key, value) VALUES (?, ?)";
        try (Connection conn = jobStore.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Config Error: " + e.getMessage());
        }
    }

    public String getConfig(String key) {
        String sql = "SELECT value FROM config WHERE key = ?";
        try (Connection conn = jobStore.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            System.err.println("Config Error: " + e.getMessage());
        }
        return null;
    }

    public int getConfigAsInt(String key, int defaultValue) {
        String value = getConfig(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return defaultValue;
    }
}