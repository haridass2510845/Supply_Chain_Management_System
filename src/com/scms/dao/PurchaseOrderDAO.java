package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.PurchaseOrder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for the purchase_orders table.
 * Implements Module 3 (Procurement Management):
 * PO-01 Create, PO-02 Approve, PO-03 Cancel, PO-04 Track Status,
 * PO-05 Supplier Performance, PO-06 Reports.
 */
public class PurchaseOrderDAO {

    private static final String BASE_SELECT =
            "SELECT po.po_id, po.supplier_id, s.supplier_name, po.item_name, po.quantity, "
          + "po.unit_price, po.total_amount, po.status, po.created_by, po.created_at, "
          + "po.approved_by, po.approved_at, po.shipped_at, po.delivered_at, po.remarks "
          + "FROM purchase_orders po "
          + "JOIN suppliers s ON s.supplier_id = po.supplier_id ";

    /**
     * Returns every purchase order, most recently created first (PO-04: Track Status).
     */
    public List<PurchaseOrder> getAllOrders() {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY po.po_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Filters purchase orders by status (PENDING / APPROVED / CANCELLED / COMPLETED).
     * Pass null or "ALL" to get every order.
     */
    public List<PurchaseOrder> getOrdersByStatus(String status) {
        if (status == null || status.isEmpty() || "ALL".equalsIgnoreCase(status)) {
            return getAllOrders();
        }

        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE po.status = ? ORDER BY po.po_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public PurchaseOrder getOrderById(int poId) {
        String sql = BASE_SELECT + "WHERE po.po_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, poId);
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
     * Creates a new purchase order in PENDING status (PO-01: Create Purchase Order).
     * total_amount is computed server-side from quantity * unitPrice so it can
     * never drift from what the two source fields actually say.
     */
    public boolean createOrder(PurchaseOrder po) {
        String sql = "INSERT INTO purchase_orders "
                   + "(supplier_id, item_name, quantity, unit_price, total_amount, status, created_by) "
                   + "VALUES (?, ?, ?, ?, ?, 'PENDING', ?)";

        BigDecimal total = po.getUnitPrice().multiply(BigDecimal.valueOf(po.getQuantity()));

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, po.getSupplierId());
            ps.setString(2, po.getItemName());
            ps.setInt(3, po.getQuantity());
            ps.setBigDecimal(4, po.getUnitPrice());
            ps.setBigDecimal(5, total);
            ps.setString(6, po.getCreatedBy());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Approves a PENDING purchase order (PO-02: Approve Purchase Order).
     * Only orders currently PENDING can be approved, preventing an already
     * cancelled/completed order from being flipped back to approved.
     */
    public boolean approveOrder(int poId, String approvedBy) {
        String sql = "UPDATE purchase_orders "
                   + "SET status = 'APPROVED', approved_by = ?, approved_at = ? "
                   + "WHERE po_id = ? AND status = 'PENDING'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, approvedBy);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setInt(3, poId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cancels a purchase order (PO-03: Cancel Purchase Order).
     * A COMPLETED order cannot be cancelled; PENDING or APPROVED orders can.
     */
    public boolean cancelOrder(int poId, String remarks) {
        String sql = "UPDATE purchase_orders "
                   + "SET status = 'CANCELLED', remarks = ? "
                   + "WHERE po_id = ? AND status IN ('PENDING','APPROVED')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, remarks);
            ps.setInt(2, poId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * PO-05: Supplier Performance - per-supplier order counts and total value,
     * used to gauge how much business (and how reliably) each supplier handles.
     */
    public List<Map<String, Object>> getSupplierPerformance() {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT s.supplier_name, "
                   + "COUNT(po.po_id) AS total_orders, "
                   + "SUM(CASE WHEN po.status = 'APPROVED' OR po.status = 'COMPLETED' THEN 1 ELSE 0 END) AS fulfilled_orders, "
                   + "SUM(CASE WHEN po.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_orders, "
                   + "COALESCE(SUM(po.total_amount), 0) AS total_value "
                   + "FROM suppliers s "
                   + "LEFT JOIN purchase_orders po ON po.supplier_id = s.supplier_id "
                   + "GROUP BY s.supplier_name "
                   + "ORDER BY total_value DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("supplierName", rs.getString("supplier_name"));
                row.put("totalOrders", rs.getInt("total_orders"));
                row.put("fulfilledOrders", rs.getInt("fulfilled_orders"));
                row.put("cancelledOrders", rs.getInt("cancelled_orders"));
                row.put("totalValue", rs.getBigDecimal("total_value"));
                rows.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    /**
     * PO-06: Procurement Reports - a simple summary (counts per status and
     * total spend on approved/completed orders) for the dashboard cards.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalOrders", 0);
        summary.put("pendingCount", 0);
        summary.put("approvedCount", 0);
        summary.put("cancelledCount", 0);
        summary.put("totalSpend", BigDecimal.ZERO);

        String sql = "SELECT "
                   + "COUNT(*) AS total_orders, "
                   + "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count, "
                   + "SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count, "
                   + "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_count, "
                   + "COALESCE(SUM(CASE WHEN status IN ('APPROVED','COMPLETED') THEN total_amount ELSE 0 END), 0) AS total_spend "
                   + "FROM purchase_orders";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                summary.put("totalOrders", rs.getInt("total_orders"));
                summary.put("pendingCount", rs.getInt("pending_count"));
                summary.put("approvedCount", rs.getInt("approved_count"));
                summary.put("cancelledCount", rs.getInt("cancelled_count"));
                summary.put("totalSpend", rs.getBigDecimal("total_spend"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    /**
     * PO-07: Returns every purchase order raised against one specific
     * supplier -- what a logged-in SUPPLIER account is allowed to see.
     */
    public List<PurchaseOrder> getOrdersBySupplier(int supplierId) {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE po.supplier_id = ? ORDER BY po.po_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * PO-08: Marks an APPROVED order as SHIPPED. The supplierId check
     * ensures a supplier can only update their own orders, never someone
     * else's, even if they guess another order's ID in the URL.
     */
    public boolean markShipped(int poId, int supplierId) {
        String sql = "UPDATE purchase_orders "
                   + "SET status = 'SHIPPED', shipped_at = ? "
                   + "WHERE po_id = ? AND supplier_id = ? AND status = 'APPROVED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, poId);
            ps.setInt(3, supplierId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * PO-09: Marks a SHIPPED order as COMPLETED (delivery confirmed).
     * Same ownership guard as markShipped.
     */
    public boolean markDelivered(int poId, int supplierId) {
        String sql = "UPDATE purchase_orders "
                   + "SET status = 'COMPLETED', delivered_at = ? "
                   + "WHERE po_id = ? AND supplier_id = ? AND status = 'SHIPPED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, poId);
            ps.setInt(3, supplierId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * PO-10: A single supplier's own performance snapshot -- the same
     * shape as one row of getSupplierPerformance(), scoped to just them.
     */
    public Map<String, Object> getPerformanceForSupplier(int supplierId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("totalOrders", 0);
        row.put("fulfilledOrders", 0);
        row.put("cancelledOrders", 0);
        row.put("totalValue", BigDecimal.ZERO);

        String sql = "SELECT "
                   + "COUNT(po_id) AS total_orders, "
                   + "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS fulfilled_orders, "
                   + "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_orders, "
                   + "COALESCE(SUM(total_amount), 0) AS total_value "
                   + "FROM purchase_orders WHERE supplier_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    row.put("totalOrders", rs.getInt("total_orders"));
                    row.put("fulfilledOrders", rs.getInt("fulfilled_orders"));
                    row.put("cancelledOrders", rs.getInt("cancelled_orders"));
                    row.put("totalValue", rs.getBigDecimal("total_value"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return row;
    }

    private PurchaseOrder mapRow(ResultSet rs) throws SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(rs.getInt("po_id"));
        po.setSupplierId(rs.getInt("supplier_id"));
        po.setSupplierName(rs.getString("supplier_name"));
        po.setItemName(rs.getString("item_name"));
        po.setQuantity(rs.getInt("quantity"));
        po.setUnitPrice(rs.getBigDecimal("unit_price"));
        po.setTotalAmount(rs.getBigDecimal("total_amount"));
        po.setStatus(rs.getString("status"));
        po.setCreatedBy(rs.getString("created_by"));
        po.setCreatedAt(rs.getTimestamp("created_at"));
        po.setApprovedBy(rs.getString("approved_by"));
        po.setApprovedAt(rs.getTimestamp("approved_at"));
        po.setShippedAt(rs.getTimestamp("shipped_at"));
        po.setDeliveredAt(rs.getTimestamp("delivered_at"));
        po.setRemarks(rs.getString("remarks"));
        return po;
    }
}
