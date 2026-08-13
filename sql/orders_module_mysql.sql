-- ============================================================
-- Module 6 (Order Fulfillment) - SRS FR7
-- OF-01 Receive Customer Order, OF-02 Verify Inventory,
-- OF-03 Process Order, OF-04 Deliver Order
--
-- A customer order is fulfilled against a single inventory item.
-- Placing an order does NOT move stock by itself -- it is only
-- reserved once the order is VERIFIED (checked against inventory)
-- and stock is only actually deducted once it is PROCESSED
-- (dispatched from the warehouse), mirroring the same
-- check-before-you-commit pattern used by Warehouse dispatch.
-- ============================================================

CREATE TABLE IF NOT EXISTS customer_orders (
    order_id       INT AUTO_INCREMENT PRIMARY KEY,
    customer_name  VARCHAR(150) NOT NULL,
    item_id        INT NOT NULL,
    quantity       INT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at    TIMESTAMP NULL,
    processed_at   TIMESTAMP NULL,
    delivered_at   TIMESTAMP NULL,
    handled_by     VARCHAR(50),
    remarks        VARCHAR(255),
    FOREIGN KEY (item_id) REFERENCES inventory(item_id)
);

-- Sample customer order so Order Fulfillment has something to show immediately
INSERT INTO customer_orders (customer_name, item_id, quantity, status)
SELECT 'Chennai Retail Traders', item_id, 25, 'PENDING'
FROM inventory WHERE item_name = 'Steel Rods (12mm)' LIMIT 1;
