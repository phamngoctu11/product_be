# Bàn giao PHẦN 12 — Shipping, receipt và reorder (WF07/WF08/F17)

Trạng thái hiện tại: `VERIFIED` theo review và chấp nhận của chủ dự án ngày 28/10/2026 cho backend và frontend.

## Phạm vi đã triển khai

- STAFF lịch sử của Order ghi `exportedQuantity` đầy đủ theo `orderItemId`, chuyển `READY_TO_SHIP → SHIPPING` đúng một lần.
- WF07 dùng assignment history sau khi WF05 đã release staff; không yêu cầu assignment còn active và không release lần hai.
- Lưu actor, thời điểm bàn giao, ghi chú, `expectedShippingDays = 2`, history, outbox và cache invalidation.
- USER owner hoặc Guest có token đúng scope đọc receipt view và xác nhận đủ `receivedQuantity` theo `orderItemId`.
- Chênh lệch chưa được chấp nhận chỉ trả mismatches, không ghi một phần và giữ `SHIPPING`.
- Khớp hoặc chấp nhận lệch tạo một `OrderReceipt`, chốt quantity, chuyển `DELIVERED`, ghi history/outbox; USER +2 reputation, Guest không có reputation.
- CATALOG mở lời mời đánh giá sau `ORDER_DELIVERED`; CUSTOM không phát lời mời đánh giá.
- CATALOG reorder chỉ thêm variant còn `ACCEPTING_ORDERS` vào cart và trả `addedItems/skippedItems`.
- CUSTOM reorder tạo `CustomRequest DRAFT` mới từ snapshot và trả `customDraftId`; không sao chép giá, payment, voucher, assignment, deadline, checkpoint hay approval.
- Đã loại bỏ endpoint/DTO/event/email complaint legacy. P12 chỉ hiển thị chênh lệch và cho khách quyết định `acceptMismatch`.

## API runtime

### STAFF bàn giao

```http
POST /api/orders/staff/{orderId}/shipping-handover
Authorization: Bearer <STAFF JWT>
Idempotency-Key: <stable operation key>
Content-Type: application/json

{
  "orderVersion": 12,
  "items": [
    { "orderItemId": 501, "exportedQuantity": 2 }
  ],
  "handoverNote": "Đã bàn giao kiện hàng"
}
```

### USER receipt

```http
GET  /api/orders/{orderId}/receipt
POST /api/orders/{orderId}/receipt
Authorization: Bearer <USER JWT>
Idempotency-Key: <stable operation key>   # POST
```

```json
{
  "orderVersion": 13,
  "receivedItems": [
    { "orderItemId": 501, "receivedQuantity": 2 }
  ],
  "acceptMismatch": false,
  "note": null
}
```

### Guest receipt

```http
GET  /api/guest/orders/{orderId}/receipt?token=<order-token>
POST /api/guest/orders/{orderId}/receipt?token=<order-token>
Idempotency-Key: <stable operation key>   # POST
```

Guest token được rate-limit và kiểm tra đúng Order/scope. Raw token không lưu trong receipt, log hoặc event.

### Reorder

```http
POST /api/orders/{orderId}/reorder
Authorization: Bearer <USER JWT>
Idempotency-Key: <stable operation key>
```

CATALOG trả danh sách thêm/bỏ qua. CUSTOM trả `orderType=CUSTOM` và `customDraftId` để mở `/store/custom-requests/{id}`.

## Consistency và migration

- Mọi mutation dùng `DurableRequestExecutor`; cùng key/payload trả kết quả đã commit, cùng key/khác payload trả conflict.
- Order được khóa pessimistic và kiểm tra `orderVersion`; hai receipt cạnh tranh chỉ một lần chuyển `DELIVERED`.
- `OrderReceipt.order_id` unique cùng Order lock bảo vệ một receipt cho mỗi Order.
- Migration `11-delivery-receipt.xml` thêm metadata handover và `delivery_contract_version`.
- Order SHIPPING legacy giữ version null nên có thể dùng ordered quantity làm fallback có cảnh báo; mọi handover P12 mới ghi version 1 và bắt buộc explicit `exportedQuantity`.
- Không tích hợp shipper, tracking webhook, COD reconciliation, timer auto-deliver hoặc email trạng thái SHIPPING/DELIVERED.

## Kiểm thử

- Unit: quyền staff lịch sử, danh sách item đầy đủ, mismatch, USER/Guest reputation, legacy fallback, catalog/custom reorder.
- Integration: replay idempotency và hai receipt đồng thời chỉ một transition/event/reputation.
- Migration: metadata mới không biến Order SHIPPING legacy thành handover contract mới.
- Targeted P12: 10 test service/atomicity và 1 test migration đều xanh.
- Full suite: `.\mvnw.cmd test` chạy 341 test, 0 failure, 0 error, 11 test opt-in được skip.
