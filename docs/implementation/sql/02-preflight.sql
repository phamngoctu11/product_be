-- Read-only audit on a copy of the legacy database BEFORE enabling migration.
SELECT status, payment_method, COUNT(*) AS orders_count
FROM orders GROUP BY status, payment_method;

-- Unknown ONLINE payment result requires provider reconciliation, never guess PAID.
SELECT id, status, payment_method, final_price FROM orders
WHERE UPPER(TRIM(payment_method)) = 'ONLINE';

-- Orders with multiple item-level staff cannot be migrated automatically.
SELECT i.order_id, COUNT(DISTINCT a.assigned_staff_id) AS staff_count
FROM order_item i JOIN order_item_assignments a ON a.order_item_id = i.id
GROUP BY i.order_id HAVING COUNT(DISTINCT a.assigned_staff_id) > 1;

-- Potential staff occupancy conflicts must be resolved before new assignments are enabled.
SELECT warehouse_staff_id, COUNT(*) AS active_legacy_orders FROM orders
WHERE warehouse_staff_id IS NOT NULL
  AND status NOT IN ('SHIPPING', 'DELIVERED', 'CANCELLED')
GROUP BY warehouse_staff_id HAVING COUNT(*) > 1;

SELECT id, final_price, total_price, discount_amount FROM orders
WHERE final_price < 0 OR total_price < 0 OR discount_amount < 0;

-- Preserve original column types/defaults before rollout or restoring a backup.
SELECT table_name, column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('orders', 'order_item', 'orderhistory', 'products');
