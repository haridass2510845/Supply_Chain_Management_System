package com.scms.model;

import java.sql.Timestamp;

/**
 * A single outbound delivery, fulfilling one warehouse dispatch.
 * Implements Module 5 (Logistics Management).
 *
 * itemName/quantity are convenience fields populated by a join in
 * LogisticsDAO (through the dispatch's inventory_transactions row) so
 * the JSP doesn't need a second lookup to show what's actually being
 * delivered.
 */
public class Shipment {

    private int shipmentId;
    private int txnId;
    private String itemName;
    private int quantity;
    private String destination;
    private String carrierName;
    private String vehicleNo;
    private String status; // ASSIGNED, IN_TRANSIT, DELIVERED
    private String assignedBy;
    private Timestamp assignedAt;
    private Timestamp inTransitAt;
    private Timestamp deliveredAt;
    private String remarks;

    public Shipment() {
    }

    public int getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(int shipmentId) {
        this.shipmentId = shipmentId;
    }

    public int getTxnId() {
        return txnId;
    }

    public void setTxnId(int txnId) {
        this.txnId = txnId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Timestamp getInTransitAt() {
        return inTransitAt;
    }

    public void setInTransitAt(Timestamp inTransitAt) {
        this.inTransitAt = inTransitAt;
    }

    public Timestamp getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Timestamp deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public boolean isAssigned() {
        return "ASSIGNED".equals(status);
    }

    public boolean isInTransit() {
        return "IN_TRANSIT".equals(status);
    }

    public boolean isDelivered() {
        return "DELIVERED".equals(status);
    }
}
