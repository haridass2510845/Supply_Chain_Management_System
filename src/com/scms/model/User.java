package com.scms.model;

/**
 * Represents a system user (Admin, Procurement Manager, Warehouse Manager,
 * Supplier, or Logistics Staff) as described in SRS section 2.3.
 */
public class User {

    private int userId;
    private String username;
    private String fullName;
    private String email;
    private String role;   // ADMIN, PROCUREMENT_MANAGER, WAREHOUSE_MANAGER, SUPPLIER, LOGISTICS_STAFF
    private String status; // ACTIVE, INACTIVE

    public User() {
    }

    public User(int userId, String username, String fullName, String email, String role, String status) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
