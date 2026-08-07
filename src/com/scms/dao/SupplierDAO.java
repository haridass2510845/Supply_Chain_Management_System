package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the suppliers table.
 * Implements FR2 (Supplier Management) from the SRS:
 * Add Supplier, Update Supplier, Delete Supplier, Search Supplier.
 */
public class SupplierDAO {

    /**
     * Returns every supplier, most recently added first.
     */
    public List<Supplier> getAllSuppliers() {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT supplier_id, supplier_name, contact_no, email, address "
                   + "FROM suppliers ORDER BY supplier_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                suppliers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    /**
     * Fetches a single supplier by ID (used to pre-fill the edit form).
     */
    public Supplier getSupplierById(int supplierId) {
        String sql = "SELECT supplier_id, supplier_name, contact_no, email, address "
                   + "FROM suppliers WHERE supplier_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, supplierId);
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
     * Searches suppliers by name, contact number, or email (SRS FR2: Search Supplier).
     */
    public List<Supplier> searchSuppliers(String keyword) {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT supplier_id, supplier_name, contact_no, email, address "
                   + "FROM suppliers "
                   + "WHERE LOWER(supplier_name) LIKE ? OR LOWER(contact_no) LIKE ? OR LOWER(email) LIKE ? "
                   + "ORDER BY supplier_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + keyword.trim().toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    suppliers.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    /**
     * Adds a new supplier (SRS FR2: Add Supplier / Feature 1).
     * Supplier_ID is duplicate-checked implicitly via the DB's identity/auto-increment
     * primary key, matching the SRS constraint "Duplicate Supplier IDs are not allowed."
     */
    public boolean addSupplier(Supplier supplier) {
        String sql = "INSERT INTO suppliers (supplier_name, contact_no, email, address) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getContactNo());
            ps.setString(3, supplier.getEmail());
            ps.setString(4, supplier.getAddress());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing supplier's details (SRS FR2: Update Supplier).
     */
    public boolean updateSupplier(Supplier supplier) {
        String sql = "UPDATE suppliers SET supplier_name = ?, contact_no = ?, email = ?, address = ? "
                   + "WHERE supplier_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getContactNo());
            ps.setString(3, supplier.getEmail());
            ps.setString(4, supplier.getAddress());
            ps.setInt(5, supplier.getSupplierId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a supplier by ID (SRS FR2: Delete Supplier).
     */
    public boolean deleteSupplier(int supplierId) {
        String sql = "DELETE FROM suppliers WHERE supplier_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, supplierId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(rs.getInt("supplier_id"));
        supplier.setSupplierName(rs.getString("supplier_name"));
        supplier.setContactNo(rs.getString("contact_no"));
        supplier.setEmail(rs.getString("email"));
        supplier.setAddress(rs.getString("address"));
        return supplier;
    }
}
