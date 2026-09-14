package com.crystalville.crystalcore.database;

import java.sql.*;

public class DatabaseManager {
    private final String url;

    public DatabaseManager(String path) {
        this.url = "jdbc:sqlite:" + path;
    }

    public String verifyCodeAndGetDiscordId(String code) {
        String query = "SELECT discord_id FROM link_codes WHERE code = ? AND expiry > ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, code.toUpperCase());
            pstmt.setLong(2, System.currentTimeMillis() / 1000);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveLink(String uuid, String discordId) {
        String query = "INSERT OR REPLACE INTO linked_users (uuid, discord_id) VALUES (?, ?)";
        String deleteCode = "DELETE FROM link_codes WHERE discord_id = ?";
        try (Connection conn = DriverManager.getConnection(url)) {
            // Save link
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, uuid);
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();

            // Cleanup code
            PreparedStatement delStmt = conn.prepareStatement(deleteCode);
            delStmt.setString(1, discordId);
            delStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
      }
