package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.User;
import com.scms.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for the users table.
 * Implements FR1 (User Login) from the SRS.
 */
public class UserDAO {

    /**
     * Validates the given username/password against the database.
     *
     * @return the matching User object if credentials are valid and the
     *         account is ACTIVE, or null if authentication fails.
     */
    public User authenticate(String username, String plainPassword) {
        String sql = "SELECT user_id, username, password, full_name, email, role, status "
                   + "FROM users WHERE username = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String status = rs.getString("status");

                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        return null; // account disabled
                    }

                    if (PasswordUtil.verifyPassword(plainPassword, storedHash)) {
                        User user = new User();
                        user.setUserId(rs.getInt("user_id"));
                        user.setUsername(rs.getString("username"));
                        user.setFullName(rs.getString("full_name"));
                        user.setEmail(rs.getString("email"));
                        user.setRole(rs.getString("role"));
                        user.setStatus(status);
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // invalid credentials
    }

    /**
     * Checks whether a username is already taken.
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true; // fail safe: treat as taken so we don't silently skip a duplicate check
        }
    }

    /**
     * Registers a new user with a hashed password. New accounts are
     * created with status ACTIVE so they can log in immediately.
     *
     * @return true if the account was created successfully.
     */
    public boolean registerUser(String username, String plainPassword, String fullName,
                                 String email, String role) {
        String sql = "INSERT INTO users (username, password, full_name, email, role, status) "
                   + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hashPassword(plainPassword));
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, role);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Changes a user's password (used by the "Change Password" function
     * listed in SRS 2.2 Product Functions).
     */
    public boolean changePassword(int userId, String newPlainPassword) {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, PasswordUtil.hashPassword(newPlainPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
