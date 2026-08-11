package com.scms.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Represents a purchase order raised against a supplier.
 * Implements Module 3 (Procurement Management): PO-01 Create, PO-02 Approve,
 * PO-03 Cancel, PO-04 Track Status.
 *
 * supplierName is a convenience field populated by a join in PurchaseOrderDAO
 * so JSPs don't need a second lookup just to display who the PO is for.
 */
public class PurchaseOrder {

    private int poId;
    private int supplierId;
    private String supplierName;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String status;
    private String createdBy;
    private Timestamp createdAt;
    private String approvedBy;
    private Timestamp approvedAt;
    private Timestamp shippedAt;
    private Timestamp deliveredAt;
    private String remarks;

    public PurchaseOrder() {
    }

    public int getPoId() {
        return poId;
    }

    public void setPoId(int poId) {
        this.poId = poId;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Timestamp getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Timestamp approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Timestamp getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Timestamp shippedAt) {
        this.shippedAt = shippedAt;
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

    /**
     * True while this PO is still awaiting a decision (used by the JSP to
     * decide whether to show the Approve action).
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * True once approved and ready to be shipped by the supplier, but not
     * yet marked SHIPPED.
     */
    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    /**
     * True once the supplier has marked this order shipped, but delivery
     * hasn't been confirmed yet.
     */
    public boolean isShipped() {
        return "SHIPPED".equals(status);
    }
}
