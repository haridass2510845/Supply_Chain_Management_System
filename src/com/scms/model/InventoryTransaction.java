package com.scms.model;

import java.sql.Timestamp;

/**
 * A single stock movement (receive, dispatch, or manual adjustment)
 * against one inventory item. Implements Module 4 (Warehouse Management).
 *
 * itemName is a convenience field populated by a join in
 * InventoryDAO.getRecentTransactions() so the JSP doesn't need a second
 * lookup just to show which item a transaction was for.
 */
public class InventoryTransaction {

    private int txnId;
    private int itemId;
    private String itemName;
    private Integer poId;
    private String txnType; // RECEIVE, DISPATCH, ADJUSTMENT
    private int quantity;
    private String performedBy;
    private String remarks;
    private Timestamp createdAt;

    public InventoryTransaction() {
    }

    public int getTxnId() {
        return txnId;
    }

    public void setTxnId(int txnId) {
        this.txnId = txnId;
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

    public Integer getPoId() {
        return poId;
    }

    public void setPoId(Integer poId) {
        this.poId = poId;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
