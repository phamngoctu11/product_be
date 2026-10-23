# Lệnh giao việc Antigravity — frontend PHẦN 13 integration/cutover

Bạn đang làm việc trong repository frontend `D:\me\product_fe`. Hãy audit và hoàn thiện PHẦN 13 trên code hiện có. Không sửa backend, không commit, không tạo module/page trùng và phải giữ nguyên mọi thay đổi chưa commit không liên quan.

## Nguồn bắt buộc đọc trước khi sửa

- `D:\me\product_be\docs\target.txt`, toàn bộ state table, F02–F18 và AC01–AC47.
- `D:\me\product_be\docs\implementation\13-integration-cutover-rollout.md`.
- `D:\me\product_be\docs\implementation\12-delivery-receipt-reorder.md`.
- Backend controllers/DTO runtime: `CheckoutController`, `GuestCheckoutController`, `ManagerOrderController`, `OrderAgreementController`, `PaymentController`, `StaffOrderController`, `OrderController`, `GuestOrderController`, `WorkflowOperationsController`.
- Enum runtime: `OrderStatus`, `PaymentStatus`, `ProductionStatus`, `ProductionCheckpointStatus`.

Controller/DTO runtime có ưu tiên cao hơn model hoặc URL legacy trong FE.

## Mục tiêu

1. Một UI lifecycle thống nhất cho USER/Guest/MANAGER/STAFF; action được quyết định bởi role + trạng thái backend, không tự nhảy state ở FE.
2. Nối đầy đủ P12 còn thiếu, sau đó audit xuyên P7–P12 để mọi màn hình đọc cùng status/action contract.
3. Gỡ toàn bộ route/model/service MoMo, inventory/restock, complaint, export/KCS cũ và checkout `/api/cart/approve/{userId}`.
4. Thêm màn quan sát workflow cho MANAGER/ADMIN vào khu vực quản trị hiện có; không tạo dashboard quản trị thứ hai.

## Mapping lifecycle bắt buộc

| Order status | USER/Guest | MANAGER | STAFF |
|---|---|---|---|
| `PENDING_APPROVAL` | xem/hủy nếu backend cho phép | review approve/reject | không action |
| `PENDING_ASSIGNMENT` | xem tiến độ | assign staff | self-claim nếu claimable |
| `WAITING_CUSTOMER_CONFIRMATION` | submit/re-submit agreement | xem | xem chat/agreement |
| `WAITING_STAFF_CONFIRMATION` | chờ | xem | confirm/request changes |
| `ORDER_ACCEPTED` | mở payment nếu ONLINE; chờ start | xem | start production khi backend cho phép |
| `ORDER_CREATING` | xem INITIAL progress; gửi change request hợp lệ | KCS FINAL_PRODUCT | submit INITIAL/FINAL, xử lý rework/change request |
| `READY_TO_SHIP` | xem, chưa confirm receipt | xem | shipping handover nếu là staff lịch sử |
| `SHIPPING` | confirm receipt theo ReceiptView | xem | read-only |
| `DELIVERED` | review/reorder | read-only | read-only |
| `CANCELLED` | xem/reorder theo contract | read-only | read-only |

Không dùng status legacy `PENDING_PAYMENT`, `PENDING_WAREHOUSE`, `WAREHOUSE_ASSIGNED`, `PENDING_KCS` cho Order mới và không tự map chúng thành action mutation mới.

## Contract cần sửa chính xác

- USER checkout: `POST /api/checkout`, body `CheckoutRequest`, header `Idempotency-Key`.
- Guest checkout: `POST /api/guest-checkout`, session header và idempotency theo backend hiện có.
- Xóa mọi call `/api/cart/approve/{userId}`.
- Xóa toàn bộ `/api/payment/momo-*`, MoMo callback/mock/status model. Chỉ dùng:
  - `POST /api/payments/orders/{orderId}/attempts`
  - `GET /api/payments/orders/{orderId}/status`
  - SePay QR/attempt DTO backend trả về.
- CUSTOM agreement submission phải gửi `paymentMethod: 'COD' | 'ONLINE'`; CATALOG không cho đổi payment method lúc agreement.
- Giữ các exact API P10–P12 đã bàn giao; mọi checkpoint/shipping/receipt dùng `orderItemId`, không fallback `variantId` hoặc array index.

## Workflow operations UI

Chỉ route cho MANAGER/ADMIN và vẫn tin 403 từ backend:

```text
GET  /api/manager/workflow-operations/summary
GET  /api/manager/workflow-operations/outbox/incidents?limit=50
POST /api/manager/workflow-operations/outbox/{eventId}/requeue
GET  /api/manager/workflow-operations/dlq/count
GET  /api/manager/workflow-operations/dlq/incidents?limit=50
POST /api/manager/workflow-operations/dlq/{recordId}/replay
```

- Hiển thị summary cards, bảng outbox incident và bảng DLQ, phân trang/limit tối đa 200.
- Không hiển thị hoặc cố đọc event payload; API cố ý không trả payload.
- Requeue/replay phải có confirm dialog, disable khi pending, chống double click, rồi reload summary/list.
- Event not found trả `data=false`: báo record đã được xử lý, reload; không coi là thành công giả.
- Redis unavailable/5xx hiển thị trạng thái unavailable riêng, không biến thành DLQ count 0.

## Email/notification và navigation

- UI không giả định có email cho SHIPPING/DELIVERED. Bốn email state bình thường chỉ là PENDING_APPROVAL, ORDER_ACCEPTED, ORDER_CREATING, READY_TO_SHIP.
- Cancellation và INITIAL_SHAPE là notification/email riêng.
- Notification route phải mở đúng orderId và đúng view theo role; không tự gọi mutation khi mở notification.
- Sau mutation thành công hoặc 409 phải reload Order detail/list/read model trước khi cho action tiếp.

## Idempotency và retry

- Một key ổn định cho một intent; network retry giữ cùng key và payload.
- Sửa form/payload tạo intent/key mới.
- 409 reload state, không auto-submit.
- Không retry 400/401/403/409. Tôn trọng `Retry-After` cho 429.
- Không lưu guest order token, webhook secret, payment content hoặc event payload vào log/analytics/localStorage ngoài cơ chế token hiện có đã được duyệt.

## Xóa legacy

Search toàn repo và loại code reachable liên quan:

```text
momo, momo-pay, momo-callback, momo-mock-success,
/api/cart/approve, inventory, stockReserved, stockDeducted, importStock, restock,
receipt-complaint, ReceiptComplaint, customer/confirm-receipt,
PENDING_WAREHOUSE, WAREHOUSE_ASSIGNED, PENDING_KCS,
approve-cart, guest-purchase-runtime
```

Chỉ giữ mapper/read-only compatibility nếu dữ liệu Order cũ thật sự cần hiển thị; ghi rõ file và lý do, tuyệt đối không để mutation legacy reachable.

## Test bắt buộc

- Service contract exact method/URL/header/body và unwrap `ApiResponse<T>`.
- Role/status matrix: action đúng, action sai không render; backend 403 vẫn được xử lý.
- Checkout chỉ dùng endpoint mới; CUSTOM agreement gửi paymentMethod.
- ONLINE render QR/payment attempt; COD không mở attempt; callback muộn không làm FE tự đổi state.
- Multi-item checkpoint theo item, rework đúng item, READY_TO_SHIP chỉ theo response backend.
- P12 shipping/receipt/reorder theo prompt P12.
- Operations: no payload, confirm requeue/replay, double-click guard, data=false reload, Redis unavailable.
- Search test xác nhận không còn service call/route/model MoMo, inventory, complaint và checkout legacy.

Chạy:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build:prod
git diff --check
git status --short
```

Không commit. Báo cáo file sửa, contract legacy đã xóa, số test/build, warning còn lại và mọi compatibility fallback được giữ.
