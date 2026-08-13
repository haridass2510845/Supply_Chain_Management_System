package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.InventoryItem;
import com.scms.model.InventoryTransaction;
import com.scms.model.PurchaseOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for inventory and inventory_transactions.
 * Implements Module 4 (Warehouse Management):
 * WH-01 Receive Goods, WH-02 Store Inventory, WH-03 Update Stock,
 * WH-04 Dispatch Goods, WH-05 Warehouse Reports.
 */
public class InventoryDAO {

    /**
     * WH-02: The full current inventory, alphabetical by item.
     */
    public List<InventoryItem> getAllInventory() {
        List<InventoryItem> items = new ArrayList<>();
        String sql = "SELECT item_id, item_name, quantity_on_hand, reorder_level, location, updated_at "
                   + "FROM inventory ORDER BY item_name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(mapItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * WH-01: Completed purchase orders (delivery confirmed by the
     * supplier) that haven't been received into the warehouse yet --
     * i.e. no RECEIVE transaction references them so far.
     */
    public List<PurchaseOrder> getReceivablePOs() {
        List<PurchaseOrder> orders = new ArrayList<>();
        // Rewritten from NOT IN (subquery) to LEFT JOIN ... WHERE IS NULL:
        // functionally identical, but the optimizer can use an index on
        // (txn_type, po_id) for this instead of building/scanning the
        // subquery result for every outer row.
        String sql = "SELECT po.po_id, po.supplier_id, s.supplier_name, po.item_name, po.quantity, "
                   + "po.unit_price, po.total_amount, po.status, po.created_by, po.created_at, "
                   + "po.approved_by, po.approved_at, po.shipped_at, po.delivered_at, po.remarks "
                   + "FROM purchase_orders po "
                   + "JOIN suppliers s ON s.supplier_id = po.supplier_id "
                   + "LEFT JOIN inventory_transactions it "
                   + "    ON it.po_id = po.po_id AND it.txn_type = 'RECEIVE' "
                   + "WHERE po.status = 'COMPLETED' "
                   + "AND it.txn_id IS NULL "
                   + "ORDER BY po.po_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PurchaseOrder po = new PurchaseOrder();
                po.setPoId(rs.getInt("po_id"));
                po.setSupplierId(rs.getInt("supplier_id"));
                po.setSupplierName(rs.getString("supplier_name"));
                po.setItemName(rs.getString("item_name"));
                po.setQuantity(rs.getInt("quantity"));
                po.setStatus(rs.getString("status"));
                orders.add(po);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * WH-01: Receives a completed purchase order into the warehouse.
     * Finds the matching inventory item by name (creating it at zero
     * stock if this is the first time it's ever been received) then
     * adds the PO's quantity and logs a RECEIVE transaction.
     */
    public boolean receiveFromPO(int poId, String performedBy) {
        String findPo = "SELECT item_name, quantity FROM purchase_orders "
                       + "WHERE po_id = ? AND status = 'COMPLETED'";

        try (Connection con = DBConnection.getConnection()) {

            String itemName;
            int quantity;

            try (PreparedStatement ps = con.prepareStatement(findPo)) {
                ps.setInt(1, poId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false; // not a completed PO, or already handled
                    }
                    itemName = rs.getString("item_name");
                    quantity = rs.getInt("quantity");
                }
            }

            int itemId = findOrCreateItem(con, itemName);
            adjustQuantity(con, itemId, quantity);
            logTransaction(con, itemId, poId, "RECEIVE", quantity, performedBy,
                    "Received from PO-" + poId);

            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * WH-03 / WH-04: Adjusts stock for an existing item -- positive
     * quantity for a manual stock-in adjustment, negative for a dispatch
     * or correction. Refuses to let stock go negative.
     */
    public boolean adjustStock(int itemId, int deltaQuantity, String txnType,
                                String performedBy, String remarks) {
        String checkSql = "SELECT quantity_on_hand FROM inventory WHERE item_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            int current;
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    current = rs.getInt("quantity_on_hand");
                }
            }

            if (current + deltaQuantity < 0) {
                return false; // would take stock negative -- refuse
            }

            adjustQuantity(con, itemId, deltaQuantity);
            logTransaction(con, itemId, null, txnType, Math.abs(deltaQuantity), performedBy, remarks);

            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * WH-05: The most recent stock movements across all items, newest
     * first, for the warehouse activity feed.
     */
    public List<InventoryTransaction> getRecentTransactions(int limit) {
        List<InventoryTransaction> txns = new ArrayList<>();
        String sql = "SELECT t.txn_id, t.item_id, i.item_name, t.po_id, t.txn_type, t.quantity, "
                   + "t.performed_by, t.remarks, t.created_at "
                   + "FROM inventory_transactions t "
                   + "JOIN inventory i ON i.item_id = t.item_id "
                   + "ORDER BY t.txn_id DESC "
                   + "FETCH FIRST ? ROWS ONLY";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InventoryTransaction t = new InventoryTransaction();
                    t.setTxnId(rs.getInt("txn_id"));
                    t.setItemId(rs.getInt("item_id"));
                    t.setItemName(rs.getString("item_name"));
                    int poId = rs.getInt("po_id");
                    t.setPoId(rs.wasNull() ? null : poId);
                    t.setTxnType(rs.getString("txn_type"));
                    t.setQuantity(rs.getInt("quantity"));
                    t.setPerformedBy(rs.getString("performed_by"));
                    t.setRemarks(rs.getString("remarks"));
                    t.setCreatedAt(rs.getTimestamp("created_at"));
                    txns.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return txns;
    }

    /**
     * WH-05: Summary numbers for the Warehouse Reports cards.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalItems", 0);
        summary.put("totalUnits", 0);
        summary.put("lowStockCount", 0);

        String sql = "SELECT COUNT(*) AS total_items, "
                   + "COALESCE(SUM(quantity_on_hand), 0) AS total_units, "
                   + "SUM(CASE WHEN quantity_on_hand <= reorder_level THEN 1 ELSE 0 END) AS low_stock_count "
                   + "FROM inventory";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                summary.put("totalItems", rs.getInt("total_items"));
                summary.put("totalUnits", rs.getInt("total_units"));
                summary.put("lowStockCount", rs.getInt("low_stock_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    /**
     * Finds an inventory row by exact item name, or creates one at zero
     * stock if this is the first time this item name has ever appeared.
     * Item names are the natural key tying procurement items to warehouse
     * stock, so this keeps "Steel Rods (12mm)" as one row no matter how
     * many different POs eventually get received against it.
     */
    private int findOrCreateItem(Connection con, String itemName) throws SQLException {
        String findSql = "SELECT item_id FROM inventory WHERE item_name = ?";
        try (PreparedStatement ps = con.prepareStatement(findSql)) {
            ps.setString(1, itemName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("item_id");
                }
            }
        }

        String insertSql = "INSERT INTO inventory (item_name, quantity_on_hand, reorder_level) VALUES (?, 0, 0)";
        try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, itemName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void adjustQuantity(Connection con, int itemId, int delta) throws SQLException {
        String sql = "UPDATE inventory SET quantity_on_hand = quantity_on_hand + ?, updated_at = ? WHERE item_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setInt(3, itemId);
            ps.executeUpdate();
        }
    }

    private void logTransaction(Connection con, int itemId, Integer poId, String txnType,
                                 int quantity, String performedBy, String remarks) throws SQLException {
        String sql = "INSERT INTO inventory_transactions "
                   + "(item_id, po_id, txn_type, quantity, performed_by, remarks) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            if (poId != null) {
                ps.setInt(2, poId);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, txnType);
            ps.setInt(4, quantity);
            ps.setString(5, performedBy);
            ps.setString(6, remarks);
            ps.executeUpdate();
        }
    }

    private InventoryItem mapItem(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        item.setItemId(rs.getInt("item_id"));
        item.setItemName(rs.getString("item_name"));
        item.setQuantityOnHand(rs.getInt("quantity_on_hand"));
        item.setReorderLevel(rs.getInt("reorder_level"));
        item.setLocation(rs.getString("location"));
        item.setUpdatedAt(rs.getTimestamp("updated_at"));
        return item;
    }
}
