package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.CustomerOrder;

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
 * Data Access Object for customer_orders.
 * Implements Module 6 (Order Fulfillment):
 * OF-01 Receive Customer Order, OF-02 Verify Inventory,
 * OF-03 Process Order, OF-04 Deliver Order.
 *
 * Stock is only actually deducted at the PROCESS step (via
 * InventoryDAO.adjustStock, same as a warehouse dispatch) --
 * placing or verifying an order never touches inventory by itself.
 */
public class OrderDAO {

    private final InventoryDAO inventoryDAO = new InventoryDAO();

    /**
     * OF-01: Records a new customer order against a stocked item.
     */
    public boolean createOrder(String customerName, int itemId, int quantity, String handledBy) {
        String sql = "INSERT INTO customer_orders (customer_name, item_id, quantity, status, handled_by) "
                   + "VALUES (?, ?, ?, 'PENDING', ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, customerName);
            ps.setInt(2, itemId);
            ps.setInt(3, quantity);
            ps.setString(4, handledBy);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * OF-02: Verifies a PENDING order against current stock. Only moves
     * the order to VERIFIED if enough stock is on hand right now --
     * this is a check, not a reservation, so stock can still be taken
     * by something else before PROCESS actually runs.
     */
    public boolean verifyOrder(int orderId) {
        String checkSql = "SELECT co.item_id, co.quantity, co.status, i.quantity_on_hand "
                         + "FROM customer_orders co "
                         + "JOIN inventory i ON i.item_id = co.item_id "
                         + "WHERE co.order_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            boolean enoughStock;
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"PENDING".equals(rs.getString("status"))) {
                        return false;
                    }
                    enoughStock = rs.getInt("quantity_on_hand") >= rs.getInt("quantity");
                }
            }

            if (!enoughStock) {
                return false;
            }

            String updateSql = "UPDATE customer_orders SET status = 'VERIFIED', verified_at = ? WHERE order_id = ?";
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setInt(2, orderId);
                return ps.executeUpdate() == 1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * OF-03: Processes a VERIFIED order -- actually deducts the stock
     * (logging a DISPATCH transaction via InventoryDAO, same as a
     * manual warehouse dispatch) and moves the order to PROCESSED.
     */
    public boolean processOrder(int orderId, String performedBy) {
        String checkSql = "SELECT item_id, quantity, status FROM customer_orders WHERE order_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            int itemId;
            int quantity;
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"VERIFIED".equals(rs.getString("status"))) {
                        return false;
                    }
                    itemId = rs.getInt("item_id");
                    quantity = rs.getInt("quantity");
                }
            }

            boolean dispatched = inventoryDAO.adjustStock(itemId, -quantity, "DISPATCH",
                    performedBy, "Order fulfillment for order #" + orderId);
            if (!dispatched) {
                return false; // stock ran out between VERIFY and PROCESS
            }

            String updateSql = "UPDATE customer_orders SET status = 'PROCESSED', processed_at = ? WHERE order_id = ?";
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setInt(2, orderId);
                return ps.executeUpdate() == 1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * OF-04: Marks a PROCESSED order as delivered to the customer --
     * the final step in the fulfillment pipeline.
     */
    public boolean markDelivered(int orderId) {
        String sql = "UPDATE customer_orders SET status = 'DELIVERED', delivered_at = ? "
                   + "WHERE order_id = ? AND status = 'PROCESSED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, orderId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cancels an order that hasn't been processed yet (no stock was
     * ever taken for it, so cancelling is a pure status change).
     */
    public boolean cancelOrder(int orderId) {
        String sql = "UPDATE customer_orders SET status = 'CANCELLED' "
                   + "WHERE order_id = ? AND status IN ('PENDING','VERIFIED')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * All customer orders, newest first, with the item name joined in.
     */
    public List<CustomerOrder> getAllOrders() {
        List<CustomerOrder> orders = new ArrayList<>();
        String sql = "SELECT co.order_id, co.customer_name, co.item_id, i.item_name, co.quantity, "
                   + "co.status, co.order_date, co.verified_at, co.processed_at, co.delivered_at, "
                   + "co.handled_by, co.remarks "
                   + "FROM customer_orders co "
                   + "JOIN inventory i ON i.item_id = co.item_id "
                   + "ORDER BY co.order_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * OF-05 (reporting): Quick counts for the Order Fulfillment summary
     * strip -- mirrors the same pattern as InventoryDAO.getSummary().
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pending", 0);
        summary.put("verified", 0);
        summary.put("processed", 0);
        summary.put("delivered", 0);

        String sql = "SELECT status, COUNT(*) AS cnt FROM customer_orders GROUP BY status";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("cnt");
                if ("PENDING".equals(status)) summary.put("pending", count);
                else if ("VERIFIED".equals(status)) summary.put("verified", count);
                else if ("PROCESSED".equals(status)) summary.put("processed", count);
                else if ("DELIVERED".equals(status)) summary.put("delivered", count);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    private CustomerOrder mapOrder(ResultSet rs) throws SQLException {
        CustomerOrder o = new CustomerOrder();
        o.setOrderId(rs.getInt("order_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setItemId(rs.getInt("item_id"));
        o.setItemName(rs.getString("item_name"));
        o.setQuantity(rs.getInt("quantity"));
        o.setStatus(rs.getString("status"));
        o.setOrderDate(rs.getTimestamp("order_date"));
        o.setVerifiedAt(rs.getTimestamp("verified_at"));
        o.setProcessedAt(rs.getTimestamp("processed_at"));
        o.setDeliveredAt(rs.getTimestamp("delivered_at"));
        o.setHandledBy(rs.getString("handled_by"));
        o.setRemarks(rs.getString("remarks"));
        return o;
    }
}
