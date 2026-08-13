-- ============================================================
-- Module 6 (Order Fulfillment) - SRS FR7 - Oracle XE 21.3
-- OF-01 Receive Customer Order, OF-02 Verify Inventory,
-- OF-03 Process Order, OF-04 Deliver Order
--
-- Run this connected as scms_user, same as the rest of
-- schema_oracle.sql (e.g. after: CONNECT scms_user/scms_pass@localhost:1521/XEPDB1)
-- ============================================================

CREATE TABLE customer_orders (
    order_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_name  VARCHAR2(150) NOT NULL,
    item_id        NUMBER NOT NULL,
    quantity       NUMBER NOT NULL,
    status         VARCHAR2(20) DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING','VERIFIED','PROCESSED','DELIVERED','CANCELLED')),
    order_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at    TIMESTAMP NULL,
    processed_at   TIMESTAMP NULL,
    delivered_at   TIMESTAMP NULL,
    handled_by     VARCHAR2(50),
    remarks        VARCHAR2(255),
    FOREIGN KEY (item_id) REFERENCES inventory(item_id)
);

-- Sample customer order so Order Fulfillment has something to show immediately.
-- Only inserts if a matching inventory item already exists.
INSERT INTO customer_orders (customer_name, item_id, quantity, status)
SELECT 'Chennai Retail Traders', item_id, 25, 'PENDING'
FROM inventory WHERE item_name = 'Steel Rods (12mm)' AND ROWNUM = 1;

COMMIT;

-- Index for the "orders awaiting action" queries the servlet runs constantly
CREATE INDEX idx_customer_orders_status ON customer_orders (status);
