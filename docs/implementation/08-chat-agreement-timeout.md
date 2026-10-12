# Bàn giao PHẦN 08 — Order chat, agreement và timeout CUSTOM (WF03)

Ngày cập nhật: 12/10/2026.

Trạng thái backend: `IMPLEMENTED`, toàn bộ test hiện tại xanh. Frontend không được sửa trong lần bàn giao này; hướng dẫn riêng cho Antigravity nằm tại [P8_FRONTEND_ANTIGRAVITY.md](../prompts/P8_FRONTEND_ANTIGRAVITY.md).

## Kết quả backend

- Mỗi Order có USER sau assignment có đúng một `OrderChatThread`, unique theo `orderId`; Guest không có thread.
- Chỉ USER sở hữu và assigned STAFF hiện hành được đọc/gửi/subscribe order chat. MANAGER, ADMIN, staff khác và user khác bị từ chối.
- Order-chat tái sử dụng `ChatMessage`/collection `chat_messages`; mỗi document chỉ ghi `chatThreadId`, `senderId`, `content`, `timestamp`, `messageType`. Participant và display name nằm ở `OrderChatThread`; module chat tư vấn/hoa hồng legacy đã bị loại bỏ.
- USER submit agreement bằng `orderVersion` và `Idempotency-Key`; một lần submit tạo một `agreementVersion` mới và chuyển `DISCUSSING -> WAITING_STAFF_CONFIRMATION`.
- Staff đang giữ active assignment có thể `CONFIRM` hoặc `REQUEST_CHANGES` đúng phiên bản hiện hành.
- `REQUEST_CHANGES` chuyển về `DISCUSSING`, giữ nguyên `userSubmittedAt` và không khởi động lại timer.
- `CONFIRM` CUSTOM áp dụng snapshot chính thức, khóa/tiêu thụ voucher, cập nhật tổng tiền và chuyển nguyên tử sang `ORDER_ACCEPTED`.
- USER CATALOG không được ghi đè quantity, price hoặc production duration từ client.
- CUSTOM chưa từng submit hợp lệ bị worker hủy qua `OrderCancellationService` với nguồn `CUSTOM_CONFIRMATION_TIMEOUT` khi quá `confirmationDueAt`.
- Submit và timeout dùng chung Order lock/version. Khi form commit trước, `userSubmittedAt` và trạng thái mới khiến timeout thành no-op; khi timeout commit trước, submit nhận conflict.
- Outbox có thêm `ORDER_AGREEMENT_SUBMITTED` và `ORDER_AGREEMENT_REVIEWED`; handler cung cấp hook correlate/complete cho process WF03 mục tiêu.

## REST/WebSocket contract thực tế

Tất cả REST response dùng wrapper `ApiResponse<T>` hiện có.

```http
GET  /api/orders/{orderId}/chat
GET  /api/orders/{orderId}/chat/messages
POST /api/orders/{orderId}/chat/messages

{
  "orderId": 123,
  "content": "Nội dung trao đổi"
}
```

WebSocket:

```text
CONNECT Authorization: Bearer <JWT>
SUBSCRIBE /topic/order-chat/{orderId}
SEND      /app/order-chat.send
body      {"orderId":123,"content":"Nội dung trao đổi"}
```

Server luôn lấy actor từ JWT/Principal, không tin `userId`, `senderId` hoặc role do client gửi.

Agreement:

```http
GET  /api/orders/{orderId}/agreement
POST /api/orders/{orderId}/agreement/submissions
Idempotency-Key: <stable-key-for-one-logical-submit>
```

CUSTOM submit body:

```json
{
  "orderVersion": 4,
  "description": "Mô tả đã thống nhất",
  "imageUrls": ["https://..."],
  "quantity": 2,
  "unitPrice": 250000,
  "productionDurationDays": 5,
  "userVoucherId": null,
  "note": "Ghi chú tùy chọn"
}
```

CATALOG submit chỉ gửi `orderVersion` và có thể gửi đúng voucher hiện có; không gửi description/image/quantity/unitPrice/duration để tránh bị từ chối.

```http
POST /api/orders/staff/{orderId}/agreement/decisions
Idempotency-Key: <stable-key-for-one-logical-decision>

{
  "orderVersion": 5,
  "agreementVersion": 1,
  "decision": "CONFIRM | REQUEST_CHANGES",
  "reason": "Bắt buộc khi REQUEST_CHANGES"
}
```

`OrderAgreementDTO` trả `orderVersion`, `orderType`, `orderStatus`, `agreementVersion`, `agreementStatus`, JSON string `itemSnapshot`, totals, voucher, actor/timestamp, reason và `confirmationDueAt`.

`OrderDTO` chi tiết được bổ sung `confirmationDueAt`, `userSubmittedAt`, `currentAgreementVersion`, `staffId/staffName`, `managerId/managerName`.

## Dữ liệu và migration

Liquibase `07-order-agreement-chat.xml` tạo `order_chat_threads`, unique `order_id`, FK owner/staff/order, participant display name và index tra cứu agreement hiện hành. `ChatMessage` có compound index `(chatThreadId,timestamp)` để đọc lịch sử P8 mà không cần thêm entity/repository/collection Mongo riêng.

## Orchestration và vận hành

- Thread được tạo từ consumer của `ORDER_ASSIGNED`; event retry không tạo thread thứ hai.
- Timeout reconciliation mặc định quét mỗi 60 giây, batch 100. Có thể cấu hình `workflow.custom-agreement-timeout.scan-delay-ms` và `workflow.custom-agreement-timeout.batch-size`.
- Backend đã có Camunda hook `ORDER_AGREEMENT_SUBMITTED` và task key `staff_review_agreement`. BPMN lifecycle đích chưa được thay toàn bộ trong phần này; database + outbox + timeout worker là nguồn thực thi an toàn cho giai đoạn rollout. Việc deploy/cutover BPMN xuyên WF02–WF05 vẫn thuộc PHẦN 13.
- Vì tạo chat chạy sau commit qua outbox, REST chat có thể trả 404 rất ngắn ngay sau assignment; FE nên hiển thị “đang chuẩn bị hội thoại” và retry có giới hạn, không tự tạo phòng khác.
- API history message hiện trả danh sách tăng dần theo thời gian, chưa phân trang/cursor.
- Hot path gửi order-chat không tải lại full `Order` hoặc `User`: chỉ đọc thread, so khớp JWT subject với participant, ghi Mongo rồi publish. Khi tổng thời gian từ 100 ms trở lên, server log breakdown `authorizationMs`, `persistenceMs`, `publishMs`, `totalMs` mà không log JWT/nội dung chat.

## Kiểm tra

Đã chạy full suite sau refactor room/message, WebSocket guard và loại bỏ chat tư vấn/hoa hồng: `281` test, `0` failure, `0` error, `7` skipped.

Test P8 mới bao phủ retry thread, Guest bypass, membership, WebSocket subscription guard, CUSTOM submit, CATALOG chống ghi đè, request-changes không reset timer và timeout sau/trước first valid submission. Stress test concurrency submit–timeout thực tế nên tiếp tục bổ sung ở PHẦN 13/E2E; kernel hiện đã dùng cùng pessimistic Order lock/version.

## Không thuộc PHẦN 08

Không tạo payment, không đánh dấu `PAID`, không chuyển `ORDER_CREATING`, không bắt đầu sản xuất/checkpoint và không reassign staff.
