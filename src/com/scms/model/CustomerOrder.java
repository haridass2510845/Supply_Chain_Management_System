package com.scms.model;

import java.sql.Timestamp;

/**
 * A customer order to be fulfilled from warehouse stock.
 * Implements Module 6 (Order Fulfillment):
 * OF-01 Receive Customer Order, OF-02 Verify Inventory,
 * OF-03 Process Order, OF-04 Deliver Order.
 */
public class CustomerOrder {

    private int orderId;
    private String customerName;
    private int itemId;
    private String itemName;      // joined in from inventory for display
    private int quantity;
    private String status;        // PENDING, VERIFIED, PROCESSED, DELIVERED, CANCELLED
    private Timestamp orderDate;
    private Timestamp verifiedAt;
    private Timestamp processedAt;
    private Timestamp deliveredAt;
    private String handledBy;
    private String remarks;

    public CustomerOrder() {
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    public Timestamp getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Timestamp verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Timestamp getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Timestamp processedAt) {
        this.processedAt = processedAt;
    }

    public Timestamp getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Timestamp deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
