package com.scms.model;

import java.sql.Timestamp;

/**
 * A single stocked item in the warehouse.
 * Implements Module 4 (Warehouse Management).
 */
public class InventoryItem {

    private int itemId;
    private String itemName;
    private int quantityOnHand;
    private int reorderLevel;
    private String location;
    private Timestamp updatedAt;

    public InventoryItem() {
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

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * True once stock has fallen to or below the reorder threshold --
     * drives the low-stock warning badge in the JSP.
     */
    public boolean isLowStock() {
        return quantityOnHand <= reorderLevel;
    }
}
