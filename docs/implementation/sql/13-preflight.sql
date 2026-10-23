-- PHẦN 13 preflight: read-only. Chạy trên đúng database trước backup/cutover.

SELECT status, COUNT(*) AS order_count
FROM orders
GROUP BY status
ORDER BY status;

SELECT payment_status, COUNT(*) AS order_count
FROM orders
GROUP BY payment_status
ORDER BY payment_status;

SELECT status, reconciliation_required, COUNT(*) AS attempt_count
FROM payment_attempts
GROUP BY status, reconciliation_required
ORDER BY status, reconciliation_required;

SELECT COUNT(*) AS pending_outbox,
       SUM(CASE WHEN attempts > 0 THEN 1 ELSE 0 END) AS retrying_outbox,
       MIN(occurred_at) AS oldest_pending_at
FROM workflow_outbox
WHERE published_at IS NULL;

SELECT event_type, attempts, COUNT(*) AS failed_count
FROM workflow_outbox
WHERE published_at IS NULL AND attempts > 0
GROUP BY event_type, attempts
ORDER BY event_type, attempts;

SELECT table_name, table_rows AS approximate_rows_to_archive
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'inventory_transaction';

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'orders'
  AND column_name IN ('stock_reserved', 'stock_deducted')
ORDER BY column_name;

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'product_variants'
  AND column_name = 'quantity';

-- Kết quả mong đợi trước khi cho phép cutover:
-- 1. Không có Order mới bị ghi vào status legacy.
-- 2. Mọi payment reconciliation_required đã có owner xử lý.
-- 3. Outbox retrying/DLQ đã được đánh giá; không replay mù.
-- 4. inventory_transaction, hai stock flag và product_variants.quantity đã được backup/được business chấp thuận xóa.
