package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.LoginAttempt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for login_audit.
 * Powers the Monitor System (Admin) page's activity feed.
 */
public class AuditDAO {

    /**
     * Records one login attempt, successful or not. Called from
     * LoginServlet on every submission, before it even knows what the
     * outcome will look like on-screen.
     */
    public void logAttempt(String username, boolean success, String ipAddress) {
        String sql = "INSERT INTO login_audit (username, success, ip_address) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, success ? "Y" : "N");
            ps.setString(3, ipAddress);
            ps.executeUpdate();

        } catch (SQLException e) {
            // Audit logging should never break the actual login flow --
            // just log it and move on.
            e.printStackTrace();
        }
    }

    /**
     * The most recent login attempts, newest first, for the activity feed.
     */
    public List<LoginAttempt> getRecentAttempts(int limit) {
        List<LoginAttempt> attempts = new ArrayList<>();
        String sql = "SELECT audit_id, username, success, ip_address, attempted_at "
                   + "FROM login_audit ORDER BY audit_id DESC FETCH FIRST ? ROWS ONLY";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoginAttempt a = new LoginAttempt();
                    a.setAuditId(rs.getInt("audit_id"));
                    a.setUsername(rs.getString("username"));
                    a.setSuccess("Y".equals(rs.getString("success")));
                    a.setIpAddress(rs.getString("ip_address"));
                    a.setAttemptedAt(rs.getTimestamp("attempted_at"));
                    attempts.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attempts;
    }

    /**
     * Counts of successful vs failed attempts in the most recent window
     * (last `limit` attempts) -- a quick security signal for the Monitor
     * System dashboard.
     */
    public Map<String, Object> getRecentSummary(int limit) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("successCount", 0);
        summary.put("failureCount", 0);

        String sql = "SELECT success, COUNT(*) AS cnt FROM ("
                   + "  SELECT success FROM login_audit ORDER BY audit_id DESC FETCH FIRST ? ROWS ONLY"
                   + ") recent GROUP BY success";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if ("Y".equals(rs.getString("success"))) {
                        summary.put("successCount", rs.getInt("cnt"));
                    } else {
                        summary.put("failureCount", rs.getInt("cnt"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }
}
