# PHẦN 11 — ChangeRequest (WF06)

Ngày hoàn thiện: 16/10/2026.

Trạng thái hiện tại: `VERIFIED` theo review và chấp nhận của chủ dự án ngày 28/10/2026.

## Phạm vi đã triển khai

- USER sở hữu Order tạo ChangeRequest theo từng `OrderItem` khi Order ở `ORDER_ACCEPTED` hoặc `ORDER_CREATING` và item chưa đi tới bước KCS cuối.
- Assigned STAFF đang giữ active assignment là người duy nhất được `ACCEPT` hoặc `REJECT`.
- Guest và MANAGER không tham gia WF06; tài khoản inactive/soft-delete không được đọc hoặc mutation ChangeRequest.
- `ACCEPT` tạo `specVersion` kế tiếp và giữ base/proposed/applied snapshot để audit.
- `REJECT` bắt buộc reason và không thay đổi spec.
- Cả hai nhánh giữ nguyên giá, voucher, payment, duration, deadline, Order status, production timestamp, checkpoint và assignment.
- Idempotency bền vững dùng `workflow_requests`; domain event và notification được ghi qua outbox trong cùng transaction.
- Cache danh sách Order của USER/assigned STAFF được invalidation sau commit khi request được tạo hoặc quyết định.

## API runtime

| Actor | Method | URL | Body chính |
|---|---|---|---|
| USER/STAFF | GET | `/api/orders/{orderId}/change-requests` | Không có |
| USER | POST | `/api/orders/{orderId}/change-requests` | `orderVersion`, `orderItemId`, `baseSpecVersion`, `proposedDescription`, `proposedImageUrls`, `reason` |
| STAFF | POST | `/api/orders/staff/{orderId}/change-requests/{changeRequestId}/decision` | `orderVersion`, `changeRequestVersion`, `decision`, `reason` |

Hai POST bắt buộc header `Idempotency-Key`. Mọi response dùng `ApiResponse<T>`; `decision` chỉ nhận `ACCEPT` hoặc `REJECT`, trạng thái lưu chỉ gồm `PENDING_REVIEW`, `APPLIED`, `REJECTED`.

## Data và migration

- `OrderItem.specVersion` mặc định `1`.
- `ChangeRequest` lưu `orderItemId`, assigned staff snapshot, agreement/spec base version, base/proposed/applied snapshot, request/decision reason và actor/timestamp.
- Changelog `10-change-request.xml` mở rộng bảng cũ, chuẩn hóa `PENDING → PENDING_REVIEW` và `ACCEPTED → APPLIED`, thêm foreign key/index phục vụ list và decision.
- Changelog đã được thêm vào `db.changelog-master.xml` sau PHẦN 10.

## Đồng thời và tính nhất quán

- Order được khóa trước khi kiểm tra version, assignment, request và tiến độ item.
- Request được khóa pessimistic và vẫn có `@Version`; hai quyết định cạnh tranh chỉ có một winner.
- Retry cùng key và cùng payload trả đúng kết quả đã commit, không tạo notification/domain event lần hai.
- Cùng key nhưng payload khác trả conflict.
- Base spec stale hoặc item đã tới `FINAL_WAITING_QC`, `FINAL_REWORK_REQUIRED`, `READY` không thể được áp dụng.

## Ranh giới

- Manager KCS/rework là PHẦN 10, không phải ChangeRequest.
- P11 không có phí bổ sung, payment mới, đổi quantity/price/deadline, timer SLA hay auto accept/reject.
- P11 không release staff, không chuyển `READY_TO_SHIP` và không tạo checkpoint.
- Camunda non-interrupting subprocess là tùy chọn ở cutover PHẦN 13; database/outbox hiện là nguồn quyết định.

## Kiểm thử

- Unit service bao phủ create, accept/reject, stale spec, progress guard và payload chứa field cấm.
- Data integration bao phủ replay idempotency và hai quyết định trái ngược chạy đồng thời.
- Migration test bao phủ schema PHẦN 11, status legacy và chạy changelog lặp.
- `.\mvnw.cmd test`: 329 test, 0 failure, 0 error, 10 MySQL opt-in skipped.
- Test cache sau full suite: 4/4 đạt sau khi bổ sung case invalidation ChangeRequest.
