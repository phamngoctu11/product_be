# Bàn giao PHẦN 10 — Production checkpoint và KCS (WF05)

Ngày cập nhật: 15/10/2026.

Trạng thái hiện tại: `VERIFIED` theo review và chấp nhận của chủ dự án ngày 28/10/2026 cho backend và frontend. Các kết quả kiểm thử đã ghi trong tài liệu giữ nguyên theo lần chạy thực tế.

## Kết quả nghiệp vụ

- Mọi OrderItem mới của cả CATALOG và CUSTOM đều đi qua sản xuất; quantity chỉ là số lượng của item, không sinh checkpoint theo từng chiếc.
- Khi staff bắt đầu sản xuất, `OrderProductionStatus` chuyển sang `IN_PRODUCTION`; deadline vẫn dùng snapshot `productionDurationDays` đã chốt ở checkout và mốc `productionStartedAt` của P9.
- Chỉ STAFF đang giữ active `OrderAssignment` của đúng Order mới được nộp checkpoint.
- `INITIAL_SHAPE` lưu 1–10 URL ảnh và ghi chú, tự `PASSED`, không có manager/customer review; item chuyển thẳng sang `FINAL_IN_PROGRESS` và Order vẫn `ORDER_CREATING`.
- `FINAL_PRODUCT` chỉ được nộp sau INITIAL_SHAPE, tạo attempt bất biến `PENDING_REVIEW`. Một item không thể có hai attempt cùng chờ KCS.
- MANAGER chỉ review FINAL_PRODUCT bằng `PASS` hoặc `REWORK_REQUIRED`. Rework bắt buộc có reason, giữ nguyên ảnh/decision cũ và lần nộp sau tăng `attemptNumber`.
- PASS một item chưa đủ không đổi Order và không release staff. PASS item cuối cùng chuyển đồng thời:
  - `OrderStatus: ORDER_CREATING -> READY_TO_SHIP`;
  - `OrderProductionStatus: IN_PRODUCTION -> READY_TO_SHIP`;
  - active assignment hiện tại sang `RELEASED/FINAL_KCS_PASSED`.
- `order.assignedStaff`/`warehouseStaff` lịch sử không bị xóa. Khóa active trên `OrderAssignment` mới là nguồn xác định staff còn bận.
- INITIAL_SHAPE gửi email cho USER/guest; USER còn nhận notification. FINAL_PRODUCT gửi notification nội bộ chờ KCS. Rework báo đúng assigned staff. READY_TO_SHIP có email và USER notification riêng.
- USER và guest chỉ đọc INITIAL_SHAPE đã công bố; FINAL_PRODUCT, KCS decision và reason không bị lộ ra owner view.
- Không có timer tự PASS, không có customer approval, không xuất kho/tồn kho thành phẩm và không chuyển `SHIPPING` trong P10.

## REST contract

Tất cả response dùng `ApiResponse<T>` hiện có.

### STAFF nộp checkpoint

```http
POST /api/orders/staff/{orderId}/items/{orderItemId}/checkpoints
Authorization: Bearer <STAFF JWT>
Idempotency-Key: <stable-key>
Content-Type: application/json

{
  "orderVersion": 3,
  "stage": "INITIAL_SHAPE",
  "imageUrls": ["https://cdn.example/item-501-initial.jpg"],
  "note": "Đã hoàn thành tạo dáng"
}
```

`stage` chỉ nhận `INITIAL_SHAPE | FINAL_PRODUCT`. Endpoint nhận URL/object URL sau khi FE upload ảnh; không nhận binary/base64. Response `data` là `ProductionCheckpointDTO` gồm `id`, `orderItemId`, `stage`, `status`, `attemptNumber`, `note`, `managerReviewRequired`, người/thời điểm gửi, `images`, `decision`, `version`.

### MANAGER KCS FINAL_PRODUCT

```http
POST /api/orders/manager/{orderId}/checkpoints/{checkpointId}/review
Authorization: Bearer <MANAGER JWT>
Idempotency-Key: <stable-key>
Content-Type: application/json

{
  "checkpointVersion": 0,
  "decision": "REWORK_REQUIRED",
  "reason": "Cần bổ sung đường may"
}
```

`decision` chỉ nhận `PASS | REWORK_REQUIRED`. Response `data`:

```json
{
  "checkpoint": {},
  "orderId": 10,
  "orderVersion": 4,
  "orderStatus": "ORDER_CREATING",
  "itemProductionStatus": "FINAL_REWORK_REQUIRED",
  "remainingOrderItemIds": [502],
  "assignmentReleased": false
}
```

Ở PASS cuối cùng, `orderStatus=READY_TO_SHIP`, danh sách remaining rỗng và `assignmentReleased=true`.

### Đọc tiến độ

```http
GET /api/orders/{orderId}/checkpoints
Authorization: Bearer <USER|STAFF|MANAGER JWT>
```

```http
GET /api/guest/orders/{orderId}/checkpoints?token=<signed-read-token>
```

Mỗi phần tử là `OrderItemProductionDTO`: thông tin loại sản phẩm, quantity, duration/deadline snapshot, `productionStatus` và danh sách attempt. USER/guest chỉ nhận INITIAL_SHAPE; STAFF/MANAGER nhận đầy đủ lịch sử.

## Trạng thái dùng ở frontend

- Checkpoint: `PENDING_REVIEW | PASSED | REWORK_REQUIRED`.
- Decision: `PASS | REWORK_REQUIRED`.
- Item: `WAITING_ASSIGNMENT | ASSIGNED | INITIAL_IN_PROGRESS | FINAL_IN_PROGRESS | FINAL_WAITING_QC | FINAL_REWORK_REQUIRED | READY` (`NOT_REQUIRED` chỉ còn để đọc dữ liệu legacy, không được tạo cho đơn mới).
- Order trong WF05 luôn `ORDER_CREATING` cho tới khi mọi item PASS; đầu ra duy nhất là `READY_TO_SHIP`.

## Tính nhất quán và retry

- Submit/review dùng `Idempotency-Key` qua `DurableRequestExecutor`; cùng key + cùng payload trả kết quả cũ, cùng key + payload khác bị conflict.
- Lock order trước, sau đó lock active assignment/checkpoint. Unique `(order_item_id, stage, attempt_number)` và optimistic version chặn double submit/review.
- Hai KCS cuối chạy đồng thời được tuần tự hóa bằng pessimistic order lock; chỉ một transaction tạo transition READY_TO_SHIP, release assignment và event hoàn tất.
- Side effect chạy qua transactional outbox và Redis stream với retry cho `PRODUCTION_CHECKPOINT_SUBMITTED`, `PRODUCTION_CHECKPOINT_REVIEWED`, `ORDER_READY_TO_SHIP` và email event.
- Hook Camunda dùng đúng `orderId + orderItemId` cho các task `staff_submit_initial_shape`, `staff_submit_final_product`, `manager_review_final_product`; message hoàn tất là `ORDER_READY_TO_SHIP`. Database vẫn là nguồn trạng thái trong giai đoạn staged cutover; triển khai BPMN tổng cuối nằm ở P13 như kế hoạch.

## Migration và dọn legacy

- Liquibase `09-production-checkpoint.xml` tạo `production_checkpoints`, `production_checkpoint_images`, `production_decisions`, foreign key, unique constraint và index.
- Cột MySQL ENUM legacy `production_checkpoints.status` và `production_decisions.decision_type` được đổi sang `VARCHAR` trước khi chuẩn hóa dữ liệu; nhờ đó database đã tồn tại chấp nhận `PASSED/PENDING_REVIEW/REWORK_REQUIRED` và `PASS/REWORK_REQUIRED`.
- Dữ liệu enum cũ được chuẩn hóa: `SUBMITTED/APPROVED/REJECTED` sang bộ trạng thái P10; code không còn giữ enum cũ.
- Hai trạng thái review checkpoint đầu cũ được đưa về `FINAL_IN_PROGRESS` vì INITIAL_SHAPE không còn vòng duyệt.
- Đã gỡ API và service legacy đi vòng WF05:
  - `POST /api/orders/staff/export/{orderId}`;
  - `POST /api/orders/manager/kcs-check/{orderId}`.

## Kiểm thử

- Submit INITIAL_SHAPE, điều kiện FINAL_PRODUCT, rework và tăng attempt, stale/ownership guard.
- Một item trong nhiều item PASS vẫn giữ Order/staff; item cuối PASS release đúng assignment.
- Hai FINAL KCS đồng thời bằng hai transaction thật: đúng một transition, một release và một `ORDER_READY_TO_SHIP` event.
- USER/guest visibility, signed token guest, email/notification và Redis consumer.
- Unique checkpoint/image/decision, migration tạo bảng và chuẩn hóa enum legacy.
- Catalog có `Product.handmade=false` vẫn snapshot item thành made-to-order.

Lệnh test mục tiêu đã chạy xanh:

```powershell
.\mvnw.cmd "-Dtest=ProductionCheckpointServiceTest,OrderWorkflowServiceTest,OrderLifecycleEventHandlerTest,OrderAssignmentAtomicityTest,DomainExpandMigrationTest,ProductionRepositoryTest,ProductionMapperTest,CheckoutServiceTest,RedisStreamEventConsumerTest,ManagerOrderControllerContractTest" test
```

Toàn bộ suite cũng đã chạy xanh: `316 tests`, `0 failures`, `0 errors`, `9 skipped` (các test tích hợp opt-in).

Frontend đã được nghiệm thu bằng:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build:prod
```

Kết quả: `42/42` unit/component test đạt; production build thành công. Các warning còn lại là CSS budget ở login/product detail và CommonJS của QRCode/STOMP/SockJS, không phát sinh từ P10.
