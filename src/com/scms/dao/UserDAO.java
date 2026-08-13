package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.User;
import com.scms.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the users table.
 * Implements FR1 (User Login) and the Admin "Manage Users" function
 * (create, update, activate/deactivate, delete accounts across all roles).
 */
public class UserDAO {

    /**
     * Validates the given username/password against the database.
     *
     * @return the matching User object if credentials are valid and the
     *         account is ACTIVE, or null if authentication fails.
     */
    public User authenticate(String username, String plainPassword) {
        String sql = "SELECT user_id, username, password, full_name, email, role, status, supplier_id "
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
                        int supplierId = rs.getInt("supplier_id");
                        user.setSupplierId(rs.wasNull() ? null : supplierId);
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
     * Checks whether an email address is already registered to another account.
     */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true; // fail safe
        }
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
        return registerUser(username, plainPassword, fullName, email, role, null);
    }

    /**
     * Same as {@link #registerUser(String, String, String, String, String)}
     * but also links the new account to a supplier company record at
     * creation time -- lets an admin create a SUPPLIER-role account that's
     * immediately able to see its purchase orders, instead of having to
     * edit it a second time.
     */
    public boolean registerUser(String username, String plainPassword, String fullName,
                                 String email, String role, Integer supplierId) {
        String sql = "INSERT INTO users (username, password, full_name, email, role, status, supplier_id) "
                   + "VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hashPassword(plainPassword));
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, role);
            if (supplierId != null) {
                ps.setInt(6, supplierId);
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifies that the given plain-text password matches the stored hash
     * for a user. Used to confirm the "current password" before allowing
     * a change (SRS 2.2 Change Password function).
     */
    public boolean verifyPassword(int userId, String plainPassword) {
        String sql = "SELECT password FROM users WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    return storedHash.equals(PasswordUtil.hashPassword(plainPassword));
                }
                return false;
            }
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

    // ------------------------------------------------------------------
    // Admin "Manage Users" (create, view, update, activate/deactivate,
    // delete accounts across all roles) + Forgot Password lookup.
    // ------------------------------------------------------------------

    private static final String LIST_COLUMNS =
            "user_id, username, full_name, email, role, status, supplier_id ";

    /**
     * Returns every user account, most recently created first.
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT " + LIST_COLUMNS + "FROM users ORDER BY user_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Searches users by username, full name, or email.
     */
    public List<User> searchUsers(String keyword) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT " + LIST_COLUMNS
                   + "FROM users "
                   + "WHERE LOWER(username) LIKE ? OR LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? "
                   + "ORDER BY user_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + keyword.trim().toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Fetches a single user by ID (used to pre-fill the "Edit User" form).
     */
    public User getUserById(int userId) {
        String sql = "SELECT " + LIST_COLUMNS + "FROM users WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Looks a user up by either their username OR their email address,
     * whichever the "Forgot Password" form was given. Returns the full
     * account (including email) so the caller can send the reset OTP.
     */
    public User getUserByUsernameOrEmail(String identifier) {
        String sql = "SELECT " + LIST_COLUMNS
                   + "FROM users WHERE username = ? OR LOWER(email) = LOWER(?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates a user's editable profile fields (username and password are
     * changed through their own dedicated flows, not here). Kept for
     * callers that don't need to touch the supplier link.
     */
    public boolean updateUserDetails(int userId, String fullName, String email, String role) {
        return updateUserDetails(userId, fullName, email, role, null);
    }

    /**
     * Same as {@link #updateUserDetails(int, String, String, String)} but
     * also sets (or clears) the supplier_id link -- how a SUPPLIER-role
     * account gets tied to an actual supplier company record so the
     * Supplier Portal (my_orders.jsp) has something to show them.
     * Pass null to clear the link (e.g. when the role is no longer SUPPLIER).
     */
    public boolean updateUserDetails(int userId, String fullName, String email, String role, Integer supplierId) {
        String sql = "UPDATE users SET full_name = ?, email = ?, role = ?, supplier_id = ? WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, role);
            if (supplierId != null) {
                ps.setInt(4, supplierId);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Activates or deactivates a user account. A deactivated (INACTIVE)
     * account can no longer log in (see {@link #authenticate}).
     */
    public boolean updateStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Permanently deletes a user account.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Counts currently-active administrators. Used to stop the last remaining
     * admin from being deactivated or deleted (and locking everyone out).
     */
    public int countActiveAdmins() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND status = 'ACTIVE'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        int supplierId = rs.getInt("supplier_id");
        user.setSupplierId(rs.wasNull() ? null : supplierId);
        return user;
    }
}
