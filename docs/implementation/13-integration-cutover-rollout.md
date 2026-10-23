# Bàn giao PHẦN 13 — tích hợp xuyên luồng và rollout

Trạng thái backend: `IMPLEMENTED`; chưa đánh dấu `VERIFIED` cho production vì chưa chạy migration trên bản sao MySQL/Aiven và chưa nghiệm thu frontend/E2E ngoài repository này.

## Kết quả triển khai

- Một process `OrderLifecycle`, business key `order-{orderId}`, điều phối WF02–WF05 cho USER, Guest, CATALOG và CUSTOM.
- Database vẫn là nguồn trạng thái. Camunda chỉ giữ khóa tham chiếu, human task, message và timer tuyệt đối.
- Manager review/assignment, agreement, payment, start production, checkpoint/KCS, ready-to-ship, shipping, delivered và cancellation được nối qua outbox/Redis Stream sau commit.
- CUSTOM chờ form bằng timer tuyệt đối `confirmationDueAt`; ONLINE bắt đầu timer từ `PaymentAttempt.dueAt`. Retry không kéo dài deadline.
- Multi-instance chạy theo `orderItemIds`, không theo quantity. FINAL_PRODUCT rework quay lại đúng item; chỉ hội tụ khi mọi item đã PASS.
- `ORDER_CANCELLED` là interrupting event subprocess; timeout gọi lại các cancellation/payment service dùng chung, không tự sửa Order trong delegate.
- Event review/assignment chịu được thứ tự giao chéo: lifecycle start idempotent, event chưa đúng wait-state được retry, assignment đã áp dụng được nhận biết là thành công.
- Outbox có summary/incident/requeue. Redis DLQ có count/list/replay; response incident không trả payload.
- Camunda web chỉ dành cho ADMIN. Webhook SePay vẫn public; route MoMo cũ đã bị xóa.

## API vận hành

Tất cả endpoint sau yêu cầu `MANAGER` hoặc `ADMIN`:

```http
GET  /api/manager/workflow-operations/summary
GET  /api/manager/workflow-operations/outbox/incidents?limit=50
POST /api/manager/workflow-operations/outbox/{eventId}/requeue
GET  /api/manager/workflow-operations/dlq/count
GET  /api/manager/workflow-operations/dlq/incidents?limit=50
POST /api/manager/workflow-operations/dlq/{recordId}/replay
```

`requeue` chỉ reset một outbox event đang pending và đã lỗi; không phát lại event đã published. DLQ replay giữ nguyên `eventId` để consumer ledger chống side effect trùng, append lại main stream trước rồi mới xóa DLQ record.

## Contract thay đổi cần FE lưu ý

- Chỉ dùng `POST /api/checkout` và `POST /api/guest-checkout`; `/api/cart/approve/{userId}` đã bị xóa.
- Chỉ dùng SePay tại `/api/payments/orders/{orderId}/attempts`, `/status` và `/api/webhooks/sepay`; toàn bộ MoMo API/config đã bị loại.
- CUSTOM agreement submission bắt buộc `paymentMethod: COD | ONLINE`. COD vẫn chịu policy reputation backend.
- Không còn import/restock inventory, receipt complaint, export/KCS legacy hoặc các BPMN guest/user cũ.
- Bốn email trạng thái bình thường là PENDING_APPROVAL, ORDER_ACCEPTED, ORDER_CREATING và READY_TO_SHIP. Cancellation và INITIAL_SHAPE là email nghiệp vụ riêng; SHIPPING/DELIVERED không gửi email trạng thái.

## Migration 12

`12-integration-cutover.xml`:

- thêm index phục vụ quan sát outbox;
- xóa `inventory_transaction`;
- xóa `orders.stock_reserved` và `orders.stock_deducted` nếu tồn tại.
- xóa stock `product_variants.quantity`; quantity nghiệp vụ chỉ còn ở CartItem/OrderItem/voucher quota.

Đây là migration contract có phần không thể rollback dữ liệu tự động. Phải chạy [preflight P13](sql/13-preflight.sql), backup database và diễn tập restore trước khi áp dụng trên production.

## Thứ tự rollout

1. Dừng mutation hoặc đưa hệ thống vào maintenance; giữ consumer/outbox ổn định.
2. Chạy preflight, chụp backup có kiểm tra restore, lưu số lượng outbox/DLQ/payment reconciliation.
3. Deploy migration job với Liquibase bật; xác nhận changelog 12 và schema validate thành công.
4. Deploy backend mới nhưng chưa mở FE mới; xác nhận chỉ `OrderLifecycle` được auto-deploy cho Order mới.
5. Smoke test USER CATALOG COD, USER CUSTOM ONLINE, Guest CATALOG COD, reject/cancel, checkpoint rework, shipping/receipt.
6. Deploy FE P13; xóa cache/CDN cũ, theo dõi outbox pending, DLQ, Camunda incidents và payment reconciliation.
7. Chỉ kết thúc maintenance khi event backlog về ngưỡng bình thường và không có Order mới thiếu process.

## Rollback

- Nếu chưa chạy migration 12: rollback artifact/FE như deployment thông thường.
- Nếu đã chạy migration 12: dừng write, restore DB backup trước migration rồi deploy lại artifact cũ. Không tự tạo lại bảng/cột rỗng vì mất lịch sử inventory.
- Process instance đã start bằng `OrderLifecycle` không được tự động chuyển về definition cũ. Khi rollback sau khi có Order mới, giữ backend P13 để drain hoặc xử lý migration process có kiểm thử riêng.
- Không replay hàng loạt DLQ/outbox trong lúc rollback. Chụp danh sách eventId/recordId và replay từng nhóm sau khi nguồn lỗi đã được sửa.

## Kiểm thử hiện tại

- BPMN engine: Guest dùng cùng lifecycle; CUSTOM COD qua agreement/KCS rework/delivery; ONLINE chờ PaymentAttempt + success; multi-item không nhân theo quantity; cancellation interrupt; assignment retry idempotent.
- Migration: changelog 12 lặp an toàn trên H2 MySQL mode, xóa đúng schema inventory và tạo index.
- Operations: summary/incident không trả payload, requeue chỉ áp dụng failed-pending event.
- Targeted integration P13: 40/40 test đạt; full suite: 317 test chạy, 0 failure, 0 error, 11 test opt-in skip.
- MySQL opt-in và migration trên bản sao dữ liệu production vẫn là gate vận hành, không được suy ra từ H2.
