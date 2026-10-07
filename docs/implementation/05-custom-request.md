# PHẦN 05 — Custom request, WF01

Ngày cập nhật: 07/10/2026. Trạng thái: `IMPLEMENTED` — mã nguồn và kiểm thử H2 đã hoàn thành; còn kiểm tra migration trên MySQL có Docker và nối BPMN mục tiêu ở PHẦN 13.

Nguồn nghiệp vụ: `target.txt`, tài liệu `docs/use-cases/WF01-custom-request` và báo cáo thiết kế code/endpoint.

## Kết quả triển khai

- USER có thể tạo, xem danh sách, xem chi tiết, cập nhật và xóa nhiều bản nháp custom của chính mình.
- Owner luôn lấy từ JWT qua `AuthService`; request/response không nhận hoặc cho phép thay đổi `ownerId`.
- Mọi endpoint custom được bảo vệ bằng `hasAuthority('USER')`; Guest, STAFF, MANAGER và ADMIN không có authority USER không được gọi luồng này.
- Draft dùng optimistic `version`; submit và delete kiểm tra version, còn submit khóa bản ghi bằng `PESSIMISTIC_WRITE`.
- Submit một draft tạo đúng một Order `CUSTOM / PENDING_APPROVAL` và đúng một loại OrderItem với quantity của draft.
- Spec và attachment được chụp thành JSON trong `OrderItem.specSnapshot`. Tên item snapshot được tạo ổn định từ draft; Order không phụ thuộc `ProductVariant` hoặc catalog giả.
- Giá item và `finalPrice` giữ `null` vì chưa thỏa thuận. Trường `totalPrice` primitive hiện phải lưu `0.0` để tương thích schema cũ, nhưng API submit cố ý trả `totalPrice`, `discountAmount`, `finalPrice` là `null`, không coi 0 là giá đã chốt.
- Chưa chọn COD/ONLINE ở WF01: `paymentMethodType=null`, `paymentStatus=NOT_DUE`; không mở payment, voucher, timer, chat hoặc production.
- Contact snapshot ưu tiên dữ liệu submit và fallback hồ sơ user. Khi submit, họ tên, email, điện thoại và địa chỉ sau fallback phải đầy đủ.
- Draft chỉ chuyển sang `SUBMITTED`, lưu `linkedOrderId` và `submittedAt` sau khi Order cùng item đã được lưu thành công.
- Lưu/chỉnh sửa draft không phát email, notification hay `ORDER_CREATED`.

## API

Base path: `/api/custom-requests`. Tất cả endpoint yêu cầu JWT có authority `USER`.

| Method | URL | Input chính | Idempotency | Kết quả |
|---|---|---|---|---|
| POST | `/api/custom-requests` | `spec`, `attachments`, `quantity` | `Idempotency-Key` tùy chọn | Tạo `DRAFT`, HTTP 201 |
| GET | `/api/custom-requests?status=&page=&size=` | status tùy chọn `DRAFT/SUBMITTED` | Không áp dụng | Danh sách của user hiện tại; tối đa 50 bản ghi/trang |
| GET | `/api/custom-requests/{id}` | id dương | Không áp dụng | Chi tiết thuộc user hiện tại |
| PUT | `/api/custom-requests/{id}` | `expectedVersion`, spec, attachments, quantity | Optimistic version | Thay nội dung một draft còn editable |
| DELETE | `/api/custom-requests/{id}?expectedVersion=` | version hiện tại | Bắt buộc `Idempotency-Key` | Xóa draft, HTTP 204; retry cùng key vẫn thành công |
| POST | `/api/custom-requests/{id}/submit` | `expectedVersion`, contact/note tùy chọn | Bắt buộc `Idempotency-Key` | Tạo Order chờ duyệt, HTTP 201 |

Các DTO ghi được tách riêng theo tên nghiệp vụ: `CreateCustomRequest`, `UpdateCustomRequest`, `SubmitCustomRequest`. Response draft dùng `CustomRequestDTO`; response submit dùng `CheckoutResponseDTO` để thống nhất contract tạo Order hiện có.

Nếu id không tồn tại hoặc thuộc user khác, API cùng trả `CUSTOM_REQUEST_NOT_FOUND`, không tiết lộ tài nguyên của tài khoản khác. Draft đã submit không thể update/delete. Version cũ trả `CUSTOM_REQUEST_VERSION_CONFLICT`.

## Transaction submit và chống lặp

`CustomRequestService.submit` dùng `DurableRequestExecutor` với scope gồm user và draft, payload canonical và `Idempotency-Key`:

1. Khóa draft theo `id + ownerId`.
2. Nếu draft đã liên kết Order, trả lại Order hiện hữu, không tạo event mới.
3. Kiểm tra version, spec, quantity và contact snapshot.
4. Lưu Order CUSTOM, cascade một OrderItem snapshot.
5. Đánh dấu draft `SUBMITTED` và liên kết `linkedOrderId`.
6. Ghi kết quả durable request và ba outbox event trong cùng transaction.

Ba event outbox là:

- `ORDER_CREATED` để các consumer chung biết Order đã tồn tại.
- `ORDER_CONFIRMATION_EMAIL_REQUESTED` với nội dung custom, quantity và trạng thái đang chờ quản lý duyệt.
- `NOTIFICATION_REQUESTED` cho đúng user sở hữu Order.

Retry cùng key/payload trả lại cùng `orderId`. Cùng key nhưng payload khác bị `REQUEST_KEY_CONFLICT`. Một key khác gửi lại draft đã submit cũng chỉ trả Order đã liên kết. Unique `custom_requests.linked_order_id`, khóa pessimistic và durable request tạo ba lớp bảo vệ chống sinh Order lặp.

## Email và tương thích event

Mẫu `order-confirmation.html` chỉ hiển thị tổng tiền/phương thức khi các giá trị đó tồn tại. Với custom chưa thỏa thuận, email hiển thị spec, quantity và giải thích giá/phương thức thanh toán sẽ được xác nhận sau khi quản lý duyệt.

Payload `OrderConfirmationEmailRequestedEvent` được mở rộng thêm `customSpec` và `quantity`; các constructor cũ vẫn được giữ cho checkout catalog/guest. Consumer đọc được event cũ vì hai trường mới có thể null.

`ORDER_CREATED` trước đây chạy attribution cho catalog. Phép tính attribution đã được sửa để item có `price=null` hoặc không có `ProductVariant` được bỏ qua an toàn; custom Order không làm consumer retry/DLQ.

## Entity, repository, mapper và migration

- `CustomRequest` quản lý `DRAFT/SUBMITTED`, version, timestamps, owner, snapshot nguồn và Order liên kết.
- `CustomRequestRepository` có query phân trang theo owner, lọc status, ownership lookup và `FOR UPDATE` khi submit/delete.
- `CustomRequestMapper` chỉ map entity sang DTO, không cho map ngược để tránh mass assignment.
- `OrderItemMapper` ưu tiên snapshot, nên MyOrder vẫn đọc được custom item không có catalog relation.
- Liquibase `04-custom-request.xml` thêm `submitted_at` và index `(owner_id, status, updated_at, id)`; master changelog đã include file này sau `03-consistency.xml`.

Không thay đổi checksum changeset cũ. Rollback cột `submitted_at` sau khi đã có dữ liệu cần được sao lưu/audit trước.

## Kiểm thử

Full suite:

```text
.\mvnw.cmd test
```

Kết quả: **215 test**, 0 failure, 0 error, 6 MySQL opt-in test skipped, `BUILD SUCCESS`.

Các tình huống mới đã được kiểm tra:

- Mapper draft/submitted, editable và custom Order giữ giá null.
- Nhiều draft, thứ tự phân trang, ownership query, `FOR UPDATE`, version/timestamps và unique `linkedOrderId`.
- Tạo draft không phát side effect; update version cũ bị từ chối; owner khác nhận 404.
- Submit tạo đúng Order CUSTOM/PENDING_APPROVAL, một item custom, snapshot spec/attachments, contact và production state ban đầu.
- Draft đã submit trả Order cũ, không phát lại email/notification/event.
- Delete dùng khóa owner + version và durable idempotency.
- Consumer attribution bỏ qua custom item có giá null.
- Kiểm thử transaction thật `CustomRequestSubmissionAtomicityTest`: retry cùng key chỉ có 1 Order, 1 OrderItem, 1 durable request và 3 outbox event.
- Changelog chạy lặp an toàn trên H2 MySQL mode.

MySQL Testcontainers chưa chạy được trong checkpoint trước do Docker daemon trên máy không khả dụng. Vì vậy trạng thái là `IMPLEMENTED`, chưa nâng thành `VERIFIED` trên MySQL thực tế.

## Ranh giới bàn giao

- Manager đã thấy Order qua danh sách `PENDING_APPROVAL`, nhưng PHẦN 05 không khởi động `ApproveCartProcess` legacy vì process đó dùng thứ tự nghiệp vụ cũ. Việc nối event tạo Order sang process manager-review mới thuộc PHẦN 13/cutover Camunda.
- Timer xác nhận custom PT24H chỉ bắt đầu sau manager duyệt, không nằm trong submit WF01.
- Schema chi tiết cho từng loại custom và chính sách ownership/giới hạn attachment chưa được chốt; hiện `spec` và `attachments` là text có cấu trúc. Upload vẫn là dịch vụ chung hiện hữu.
- `sourceOrderId` được giữ cho reorder custom ở PHẦN 12; create/submit WF01 không tự sao chép Order cũ.
- Hủy draft khác với hủy Order. Endpoint DELETE ở đây chỉ xóa `DRAFT`; Order đã tạo phải đi qua nghiệp vụ hủy chung ở PHẦN 06.

Hành động tiếp theo theo kế hoạch: PHẦN 06 xây `CancelOrderService` dùng chung cho USER, Guest, manager reject và timeout; không nhét logic hủy Order vào `CustomRequestService`.
