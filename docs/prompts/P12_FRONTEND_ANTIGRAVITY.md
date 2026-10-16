# Lệnh giao việc Antigravity — hoàn thiện frontend PHẦN 12

Bạn đang làm việc trong repository frontend `D:\me\product_fe`. Hãy audit và hoàn thiện PHẦN 12 — WF07 bàn giao vận chuyển, WF08 xác nhận nhận hàng và F17 reorder theo backend runtime hiện tại. Không sửa backend, không commit và giữ nguyên mọi thay đổi chưa commit không liên quan.

## Nguồn bắt buộc phải đọc

Đọc kỹ các file backend sau trước khi sửa:

- `D:\me\product_be\docs\target.txt`, mục F15, F17, F18 và AC21.
- `docs/use-cases/WF07-ship-order/usecase.md` và `sequence.md`.
- `docs/use-cases/WF08-complete-order/usecase.md` và `sequence.md`.
- `docs/implementation/12-delivery-receipt-reorder.md`.
- `src/main/java/com/example/workflow/controller/StaffOrderController.java`.
- `src/main/java/com/example/workflow/controller/OrderController.java`.
- `src/main/java/com/example/workflow/controller/GuestOrderController.java`.
- Các DTO: `ShippingHandover*`, `Receipt*`, `Reorder*`, `OrderDTO`, `OrderItemDTO`.

Ưu tiên controller/DTO runtime hơn code FE legacy hoặc tài liệu cũ nếu có khác biệt.

## Audit code FE hiện có, không tạo trùng

Các phần P12 legacy đã tồn tại và cần sửa tại chỗ:

- `src/app/model/order.model.ts` hiện còn `variantId/quantity`, `confirmed` và `ReceiptComplaintRequest`.
- `src/app/service/order.service.ts` đang gọi URL receipt/complaint cũ, chưa gửi idempotency header cho receipt/reorder.
- `src/app/features/storefront/orders/orders.*` đang map receipt theo variant và có action complaint.
- `src/app/features/storefront/guest-order/guest-order.component.ts` đang dùng URL/model cũ và fallback variant.
- `src/app/features/shared/order-detail-popup/order-detail-popup.component.*` đang kiểm tra `response.confirmed` và phát `sendComplaint`.
- `src/app/features/management/staff-orders/staff-orders.component.*` cần bổ sung action bàn giao cho Order `READY_TO_SHIP`.
- Dùng lại route custom draft `/store/custom-requests/:id`; không tạo màn custom request mới.

## API runtime chính xác

### 1. STAFF bàn giao vận chuyển

```http
POST /api/orders/staff/{orderId}/shipping-handover
Authorization: Bearer <STAFF JWT>
Idempotency-Key: <stable UUID của đúng operation>
Content-Type: application/json
```

```json
{
  "orderVersion": 12,
  "items": [
    { "orderItemId": 501, "exportedQuantity": 2 }
  ],
  "handoverNote": "Đã bàn giao kiện hàng"
}
```

Response data:

```text
orderId, orderVersion, status=SHIPPING,
shippingStartedAt, expectedShippingDays=2, expectedDeliveryAt,
items[{orderItemId, orderedQuantity, exportedQuantity}]
```

Chỉ hiển thị action cho STAFF lịch sử phụ trách Order `READY_TO_SHIP`. Backend mới là nơi quyết định quyền. Form phải có đúng một dòng cho mọi `OrderItem`; mặc định có thể dùng ordered quantity nhưng người dùng được nhập trong `0..orderedQuantity`.

### 2. USER đọc/xác nhận receipt

```http
GET  /api/orders/{orderId}/receipt
POST /api/orders/{orderId}/receipt
Authorization: Bearer <USER JWT>
Idempotency-Key: <stable UUID>   # bắt buộc với POST
```

### 3. Guest đọc/xác nhận receipt

```http
GET  /api/guest/orders/{orderId}/receipt?token=<token>
POST /api/guest/orders/{orderId}/receipt?token=<token>
Idempotency-Key: <stable UUID>   # bắt buộc với POST
```

Không đưa token vào path, body, log, toast hoặc local analytics.

Receipt view:

```text
orderId, orderVersion, orderType, status,
shippingStartedAt, expectedShippingDays, expectedDeliveryAt, deliveredAt,
canConfirmReceipt, dataWarning,
items[{orderItemId, variantId?, productName, variantName,
       orderedQuantity, exportedQuantity, receivedQuantity}]
```

Receipt request:

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

Receipt response:

```text
orderId, orderVersion, status,
matched, completed, mismatchAccepted, confirmedAt,
message,
mismatches[{orderItemId, variantId?, productName, variantName,
            orderedQuantity, exportedQuantity, receivedQuantity}]
```

Nếu `matched=false` và `completed=false`, hiển thị bảng chênh lệch và nút xác nhận lại với cùng quantities nhưng `acceptMismatch=true`. Đây không phải complaint. Không gọi endpoint complaint và không hiển thị nút “Gửi khiếu nại”.

### 4. Reorder

```http
POST /api/orders/{orderId}/reorder
Authorization: Bearer <USER JWT>
Idempotency-Key: <stable UUID>
Body: không có
```

```text
orderType, customDraftId?, addedItemCount, skippedItemCount,
addedItems[{orderItemId, variantId, variantName, quantity, skipReason}],
skippedItems[...]
```

- CATALOG: hiện added/skipped rõ ràng; chỉ chuyển tới cart khi người dùng chủ động chọn sau khi xem kết quả.
- CUSTOM: khi có `customDraftId`, điều hướng `/store/custom-requests/{customDraftId}` để người dùng chỉnh sửa draft mới.
- Chỉ cho USER reorder Order `DELIVERED` hoặc `CANCELLED`; backend vẫn xác thực ownership/status.

## Quy tắc mapping bắt buộc

- Mọi shipping/receipt mutation dùng duy nhất `OrderItem.orderItemId`; không dùng `variantId`, `productId`, array index hoặc fallback `item.id || 0`.
- `orderVersion` lấy từ response mới nhất (`ReceiptView.orderVersion` cho receipt, Order detail version cho handover). Không dùng `?? 0`.
- Không tự fallback `exportedQuantity` ở UI. Dùng giá trị backend ReceiptView trả; nếu `dataWarning=LEGACY_EXPORTED_QUANTITY_FALLBACK`, hiển thị cảnh báo dữ liệu legacy.
- Form receipt chỉ bật khi `canConfirmReceipt=true`; `READY_TO_SHIP` chỉ được xem, chưa xác nhận.
- Sau mutation thành công hoặc lỗi `409`, reload Order detail/list và receipt view trước khi cho thao tác lại.
- Đóng dialog/chuyển Order phải reset form, mismatch result và idempotency intent của Order cũ.

## Idempotency và lỗi

- Tạo một key ổn định cho đúng một intent submit. Retry do timeout/network phải dùng lại cùng key và payload byte-equivalent.
- Không sinh key mới trong mỗi lần RxJS retry/resubscribe.
- Người dùng sửa quantity/note/acceptMismatch tạo intent/key mới.
- `400`: hiện validation theo dòng.
- `401/403`: không tự retry; báo hết quyền/token.
- `409`: xóa intent stale, reload dữ liệu mới nhất; không tự gửi mutation lần nữa.
- Guest `429`: tôn trọng `Retry-After` nếu response có.

## Xóa contract legacy

- Xóa `ReceiptComplaintRequest`, `sendReceiptComplaint`, event/output `sendComplaint` và toàn bộ nút/form complaint.
- Không gọi `/api/orders/customer/confirm-receipt/{id}`.
- Không gọi `/api/orders/customer/receipt-complaint/{id}`.
- Không gọi `/api/guest/orders/{id}/confirm-receipt`.
- Đổi `confirmed` thành `completed`.
- Đổi receipt item `{variantId, quantity}` thành `{orderItemId, receivedQuantity}`.
- Không thêm shipper account, tracking API/webhook, COD reconciliation, email SHIPPING/DELIVERED hoặc timer auto-confirm.

## Test bắt buộc

- Service test exact URL/method/header/query/body và unwrap `ApiResponse<T>` cho toàn bộ API P12.
- STAFF handover gửi đủ mọi item theo `orderItemId`, stable key và orderVersion thật.
- USER/Guest chỉ mở receipt form khi backend trả `canConfirmReceipt=true`.
- Mismatch lần đầu không completed; accept mismatch gửi cùng quantities với intent/key mới.
- Không còn complaint action/API/model.
- Retry timeout giữ cùng idempotency key/payload; 409 reload và không auto-resubmit.
- Guest giữ token trong query param và gửi Idempotency-Key.
- CATALOG reorder hiển thị added/skipped; CUSTOM điều hướng đúng draftId.
- Reset state/key khi đổi Order hoặc đóng dialog.

Chạy và báo cáo đầy đủ:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build:prod
git diff --check
git status --short
```

Không commit. Báo cáo file đã sửa, contract legacy đã xóa, số test và warning build còn lại.
