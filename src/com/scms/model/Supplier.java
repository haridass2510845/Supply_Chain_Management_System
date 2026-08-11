package com.scms.model;

/**
 * Represents a supplier record as described in SRS section 6
 * (Supplier Table: Supplier_ID, Supplier_Name, Contact_No, Email, Address).
 */
public class Supplier {

    private int supplierId;
    private String supplierName;
    private String contactNo;
    private String email;
    private String address;

    public Supplier() {
    }

    public Supplier(int supplierId, String supplierName, String contactNo, String email, String address) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.contactNo = contactNo;
        this.email = email;
        this.address = address;
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

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
