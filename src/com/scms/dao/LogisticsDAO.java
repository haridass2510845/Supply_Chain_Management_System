package com.scms.dao;

import com.scms.db.DBConnection;
import com.scms.model.Shipment;

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
 * Data Access Object for shipments.
 * Implements Module 5 (Logistics Management):
 * LG-01 Assign Delivery, LG-02 Track Shipment,
 * LG-03 Update Delivery Status, LG-04 Confirm Delivery.
 */
public class LogisticsDAO {

    private static final String BASE_SELECT =
            "SELECT sh.shipment_id, sh.txn_id, i.item_name, t.quantity, sh.destination, "
          + "sh.carrier_name, sh.vehicle_no, sh.status, sh.assigned_by, sh.assigned_at, "
          + "sh.in_transit_at, sh.delivered_at, sh.remarks "
          + "FROM shipments sh "
          + "JOIN inventory_transactions t ON t.txn_id = sh.txn_id "
          + "JOIN inventory i ON i.item_id = t.item_id ";

    /**
     * LG-01: Warehouse dispatches that have gone out but haven't been
     * picked up by Logistics yet -- i.e. no shipment references them.
     */
    public List<Map<String, Object>> getUnassignedDispatches() {
        List<Map<String, Object>> dispatches = new ArrayList<>();
        String sql = "SELECT t.txn_id, i.item_name, t.quantity, t.created_at "
                   + "FROM inventory_transactions t "
                   + "JOIN inventory i ON i.item_id = t.item_id "
                   + "WHERE t.txn_type = 'DISPATCH' "
                   + "AND t.txn_id NOT IN (SELECT txn_id FROM shipments) "
                   + "ORDER BY t.txn_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("txnId", rs.getInt("txn_id"));
                row.put("itemName", rs.getString("item_name"));
                row.put("quantity", rs.getInt("quantity"));
                dispatches.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dispatches;
    }

    /**
     * LG-01: Creates a shipment for one warehouse dispatch, assigning a
     * destination and carrier. Starts in ASSIGNED status.
     */
    public boolean assignDelivery(int txnId, String destination, String carrierName,
                                   String vehicleNo, String assignedBy) {
        String sql = "INSERT INTO shipments (txn_id, destination, carrier_name, vehicle_no, assigned_by) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, txnId);
            ps.setString(2, destination);
            ps.setString(3, carrierName);
            ps.setString(4, vehicleNo);
            ps.setString(5, assignedBy);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * LG-02: Every shipment, most recently assigned first.
     */
    public List<Shipment> getAllShipments() {
        List<Shipment> shipments = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY sh.shipment_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                shipments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shipments;
    }

    /**
     * LG-02: Filters shipments by status. Pass null or "ALL" for everything.
     */
    public List<Shipment> getShipmentsByStatus(String status) {
        if (status == null || status.isEmpty() || "ALL".equalsIgnoreCase(status)) {
            return getAllShipments();
        }

        List<Shipment> shipments = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE sh.status = ? ORDER BY sh.shipment_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    shipments.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shipments;
    }

    /**
     * LG-03: Moves a shipment from ASSIGNED to IN_TRANSIT (picked up by
     * the carrier and on the way).
     */
    public boolean markInTransit(int shipmentId) {
        String sql = "UPDATE shipments SET status = 'IN_TRANSIT', in_transit_at = ? "
                   + "WHERE shipment_id = ? AND status = 'ASSIGNED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, shipmentId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * LG-04: Moves a shipment from IN_TRANSIT to DELIVERED (confirmed
     * received at the destination).
     */
    public boolean confirmDelivery(int shipmentId) {
        String sql = "UPDATE shipments SET status = 'DELIVERED', delivered_at = ? "
                   + "WHERE shipment_id = ? AND status = 'IN_TRANSIT'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, shipmentId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Summary numbers for the Logistics dashboard cards.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("assignedCount", 0);
        summary.put("inTransitCount", 0);
        summary.put("deliveredCount", 0);

        String sql = "SELECT "
                   + "SUM(CASE WHEN status = 'ASSIGNED' THEN 1 ELSE 0 END) AS assigned_count, "
                   + "SUM(CASE WHEN status = 'IN_TRANSIT' THEN 1 ELSE 0 END) AS in_transit_count, "
                   + "SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) AS delivered_count "
                   + "FROM shipments";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                summary.put("assignedCount", rs.getInt("assigned_count"));
                summary.put("inTransitCount", rs.getInt("in_transit_count"));
                summary.put("deliveredCount", rs.getInt("delivered_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    private Shipment mapRow(ResultSet rs) throws SQLException {
        Shipment s = new Shipment();
        s.setShipmentId(rs.getInt("shipment_id"));
        s.setTxnId(rs.getInt("txn_id"));
        s.setItemName(rs.getString("item_name"));
        s.setQuantity(rs.getInt("quantity"));
        s.setDestination(rs.getString("destination"));
        s.setCarrierName(rs.getString("carrier_name"));
        s.setVehicleNo(rs.getString("vehicle_no"));
        s.setStatus(rs.getString("status"));
        s.setAssignedBy(rs.getString("assigned_by"));
        s.setAssignedAt(rs.getTimestamp("assigned_at"));
        s.setInTransitAt(rs.getTimestamp("in_transit_at"));
        s.setDeliveredAt(rs.getTimestamp("delivered_at"));
        s.setRemarks(rs.getString("remarks"));
        return s;
    }
}
