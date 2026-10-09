# PHẦN 05 — Custom request, WF01

Ngày cập nhật: 09/10/2026. Trạng thái: `IMPLEMENTED` — code, migration tương thích, API, snapshot, email và test H2 đã đồng bộ quyết định mới; còn kiểm tra migration trên MySQL có Docker và nối BPMN mục tiêu ở PHẦN 13.

Nguồn nghiệp vụ: `target.txt`, tài liệu `docs/use-cases/WF01-custom-request` và báo cáo thiết kế code/endpoint.

## Quyết định nghiệp vụ bổ sung ngày 09/10/2026

- Custom draft dùng `description` mô tả sản phẩm, bắt buộc sau trim và tối đa 4.000 từ. Đếm từ bằng cách tách theo một hoặc nhiều ký tự khoảng trắng Unicode; 4.000 từ hợp lệ, 4.001 từ bị từ chối.
- Ảnh minh họa dùng `imageUrls: string[]`. Danh sách có thể rỗng; mỗi phần tử nếu có phải là URL tuyệt đối HTTP/HTTPS. Backend lưu tham chiếu và không fetch URL trong transaction.
- Chưa có business cap số ảnh hoặc allowlist host; không tự hard-code con số/domain. Giới hạn request chung vẫn áp dụng.
- API ghi mới không nhận `attachments` dạng chuỗi làm contract chính. Description và toàn bộ danh sách URL phải được snapshot bất biến vào OrderItem khi submit.
- Database phải đủ chứa chắc chắn description 4.000 từ UTF-8 và danh sách URL. `TEXT` 64 KiB hiện tại không được coi là bảo đảm; migration cần dùng `LONGTEXT`/kiểu tương đương và có đường đọc dữ liệu legacy.

### Đối chiếu triển khai

| Hạng mục | Triển khai hiện tại | Contract mục tiêu | Kết quả |
|---|---|---|---|
| Mô tả | API/domain dùng `description`; service đếm từ theo khoảng trắng Unicode | `description: String`, tối đa 4.000 từ | Đạt; test boundary 4.000/4.001 |
| Ảnh | API/domain dùng `List<String> imageUrls`; kiểm tra URL tuyệt đối HTTP/HTTPS | `imageUrls: List<String>` với URL HTTP/HTTPS | Đạt; danh sách rỗng hợp lệ, giữ thứ tự |
| Lưu trữ | Giữ tên cột legacy `spec/attachments`, nâng thành `LONGTEXT`; converter JSON đọc được dữ liệu cũ | Description/snapshot đủ chứa 4.000 từ UTF-8 | Đạt trên H2 MySQL mode; MySQL thực tế còn opt-in |
| Snapshot | JSON mới có `description`, `imageUrls`; email fallback khóa `spec` legacy | JSON có `description`, `imageUrls` đúng kiểu | Đạt cho WF01; Agreement PHẦN 08 phải dùng cùng adapter đọc |

## Kết quả triển khai

- USER có thể tạo, xem danh sách, xem chi tiết, cập nhật và xóa nhiều bản nháp custom của chính mình.
- Owner luôn lấy từ JWT qua `CurrentUserService`; request/response không nhận hoặc cho phép thay đổi `ownerId`. Mọi thao tác còn kiểm tra user chưa bị soft-delete.
- Mọi endpoint custom được bảo vệ bằng `hasAuthority('USER')`; Guest, STAFF, MANAGER và ADMIN không có authority USER không được gọi luồng này.
- Draft dùng optimistic `version`; submit và delete kiểm tra version, còn submit khóa bản ghi bằng `PESSIMISTIC_WRITE`.
- Submit một draft tạo đúng một Order `CUSTOM / PENDING_APPROVAL` và đúng một loại OrderItem với quantity của draft.
- Description và `imageUrls` được chụp thành JSON đúng kiểu trong `OrderItem.specSnapshot`. Tên item snapshot được tạo ổn định từ draft; Order không phụ thuộc `ProductVariant` hoặc catalog giả.
- Giá item và `finalPrice` giữ `null` vì chưa thỏa thuận. Trường `totalPrice` primitive hiện phải lưu `0.0` để tương thích schema cũ; response submit custom chuyên biệt không trả trường giá, nên không thể diễn giải 0 thành giá đã chốt.
- Chưa chọn COD/ONLINE ở WF01: `paymentMethodType=null`, `paymentStatus=NOT_DUE`; không mở payment, voucher, timer, chat hoặc production.
- Contact snapshot ưu tiên dữ liệu submit và fallback hồ sơ user. Khi submit, họ tên, email, điện thoại và địa chỉ sau fallback phải đầy đủ.
- Draft chỉ chuyển sang `SUBMITTED`, lưu `linkedOrderId` và `submittedAt` sau khi Order cùng item đã được lưu thành công.
- Lưu/chỉnh sửa draft không phát email, notification hay `ORDER_CREATED`.

## API

Base path: `/api/custom-requests`. Tất cả endpoint yêu cầu JWT có authority `USER`.

| Method | URL | Input chính | Idempotency | Kết quả |
|---|---|---|---|---|
| POST | `/api/custom-requests` | `description`, `imageUrls[]`, `quantity` | `Idempotency-Key` tùy chọn | Tạo `DRAFT`, HTTP 201 |
| GET | `/api/custom-requests?status=&page=&size=` | status tùy chọn `DRAFT/SUBMITTED` | Không áp dụng | Danh sách của user hiện tại; tối đa 50 bản ghi/trang |
| GET | `/api/custom-requests/{id}` | id dương | Không áp dụng | Chi tiết thuộc user hiện tại |
| PUT | `/api/custom-requests/{id}` | `expectedVersion`, `description`, `imageUrls[]`, `quantity` | Optimistic version | Thay nội dung một draft còn editable |
| DELETE | `/api/custom-requests/{id}?expectedVersion=` | version hiện tại | Bắt buộc `Idempotency-Key` | Xóa draft, HTTP 204; retry cùng key vẫn thành công |
| POST | `/api/custom-requests/{id}/submit` | `expectedVersion`, contact/note tùy chọn | Bắt buộc `Idempotency-Key` | Tạo Order chờ duyệt, HTTP 201 |

Các DTO ghi được tách riêng theo tên nghiệp vụ: `CreateCustomRequest`, `UpdateCustomRequest`, `SubmitCustomRequest`. Response draft dùng `CustomRequestDTO`; response submit dùng `CustomRequestSubmissionDTO` gồm `draftId`, `orderId`, `version`, `orderType`, `status`, `message` và không có giá chưa được thỏa thuận.

Nếu id không tồn tại hoặc thuộc user khác, API cùng trả `CUSTOM_REQUEST_NOT_FOUND`, không tiết lộ tài nguyên của tài khoản khác. Draft đã submit không thể update/delete. Version cũ trả `CUSTOM_REQUEST_VERSION_CONFLICT`.

## Transaction submit và chống lặp

`CustomRequestService.submit` dùng `DurableRequestExecutor` với scope gồm user và draft, payload canonical và `Idempotency-Key`:

1. Khóa draft theo `id + ownerId`.
2. Nếu draft đã liên kết Order, trả lại Order hiện hữu, không tạo event mới.
3. Kiểm tra version, description tối đa 4.000 từ, từng image URL, quantity và contact snapshot.
4. Lưu Order CUSTOM, cascade một OrderItem snapshot.
5. Đánh dấu draft `SUBMITTED` và liên kết `linkedOrderId`.
6. Ghi kết quả durable request và một outbox event cha `ORDER_CREATED` trong cùng transaction. Consumer sau commit đọc snapshot custom rồi route email/notification cho đúng user.

Retry cùng key/payload trả lại cùng `orderId`. Cùng key nhưng payload khác bị `REQUEST_KEY_CONFLICT`. Một key khác gửi lại draft đã submit cũng chỉ trả Order đã liên kết. Unique `custom_requests.linked_order_id`, khóa pessimistic và durable request tạo ba lớp bảo vệ chống sinh Order lặp.

## Email và tương thích event

Mẫu `order-confirmation.html` chỉ hiển thị tổng tiền/phương thức khi các giá trị đó tồn tại. Email custom đọc description từ snapshot mới, fallback `spec` legacy, hiển thị quantity, giải thích giá/phương thức thanh toán sẽ được xác nhận sau khi quản lý duyệt và có link `/orders/{orderId}`; danh sách ảnh không được nhúng/fetch tự động trong consumer.

Payload `OrderConfirmationEmailRequestedEvent` được mở rộng thêm `customSpec` và `quantity`; các constructor cũ vẫn được giữ cho checkout catalog/guest. Consumer đọc được event cũ vì hai trường mới có thể null.

`ORDER_CREATED` trước đây chạy attribution cho catalog. Phép tính attribution đã được sửa để item có `price=null` hoặc không có `ProductVariant` được bỏ qua an toàn; custom Order không làm consumer retry/DLQ.

## Entity, repository, mapper và migration

- `CustomRequest` quản lý `DRAFT/SUBMITTED`, version, timestamps, owner, snapshot nguồn và Order liên kết.
- `CustomRequestRepository` có query phân trang theo owner, lọc status, ownership lookup và `FOR UPDATE` khi submit/delete.
- `CustomRequestMapper` chỉ map entity sang DTO, không cho map ngược để tránh mass assignment.
- `OrderItemMapper` ưu tiên snapshot, nên MyOrder vẫn đọc được custom item không có catalog relation.
- Liquibase `04-custom-request.xml` thêm `submitted_at` và index `(owner_id, status, updated_at, id)`. `06-custom-request-content.xml` nâng ba vùng nội dung lên `LONGTEXT`, backfill attachment null/rỗng thành `[]` rồi đặt NOT NULL; master changelog include changeset mới sau PHẦN 07 hiện có.
- Entity giữ tên cột vật lý `spec/attachments` để không rename phá dữ liệu, nhưng property/API là `description/imageUrls`. `StringListJsonConverter` đọc JSON array mới, null/rỗng và một attachment thô legacy.

Không thay đổi checksum changeset cũ. Rollback cột `submitted_at` sau khi đã có dữ liệu cần được sao lưu/audit trước.

## Kiểm thử

Full suite:

```text
.\mvnw.cmd test
```

Kết quả: **254 test**, 0 failure, 0 error, 7 MySQL opt-in test skipped, `BUILD SUCCESS`.

Các tình huống mới đã được kiểm tra:

- Mapper draft/submitted, editable và custom Order giữ giá null.
- Nhiều draft, thứ tự phân trang, ownership query, `FOR UPDATE`, version/timestamps và unique `linkedOrderId`.
- Tạo draft không phát side effect; update version cũ bị từ chối; owner khác nhận 404.
- Submit tạo đúng Order CUSTOM/PENDING_APPROVAL, một item custom, snapshot description/imageUrls, contact và production state ban đầu; test bao phủ boundary 4.000/4.001 từ, URL sai scheme/relative và converter legacy.
- Draft đã submit trả Order cũ, không phát lại email/notification/event.
- Delete dùng khóa owner + version và durable idempotency.
- Consumer attribution bỏ qua custom item có giá null.
- Kiểm thử transaction thật `CustomRequestSubmissionAtomicityTest`: retry cùng key chỉ có 1 Order, 1 OrderItem, 1 durable request và 1 outbox event cha `ORDER_CREATED`; hai submit đồng thời bằng key khác vẫn chỉ tạo một Order; lỗi ghi event rollback Order, draft và durable request.
- Changelog chạy lặp an toàn trên H2 MySQL mode.

MySQL Testcontainers chưa chạy được trong checkpoint trước do Docker daemon trên máy không khả dụng. Vì vậy trạng thái là `IMPLEMENTED`, chưa nâng thành `VERIFIED` trên MySQL thực tế.

## Ranh giới bàn giao

- Manager đã thấy Order qua danh sách `PENDING_APPROVAL`, nhưng PHẦN 05 không khởi động `ApproveCartProcess` legacy vì process đó dùng thứ tự nghiệp vụ cũ. Việc nối event tạo Order sang process manager-review mới thuộc PHẦN 13/cutover Camunda.
- Timer xác nhận custom PT24H chỉ bắt đầu sau manager duyệt, không nằm trong submit WF01.
- Description tối đa 4.000 từ và contract `imageUrls: string[]` đã chốt. Số ảnh tối đa/allowlist host chưa chốt; upload vẫn là dịch vụ chung hiện hữu và backend WF01 không fetch URL.
- `sourceOrderId` được giữ cho reorder custom ở PHẦN 12; create/submit WF01 không tự sao chép Order cũ.
- Hủy draft khác với hủy Order. Endpoint DELETE ở đây chỉ xóa `DRAFT`; Order đã tạo phải đi qua nghiệp vụ hủy chung ở PHẦN 06.

Hành động tiếp theo: review PHẦN 05 và chạy bộ MySQL opt-in khi Docker sẵn sàng. Sau đó triển khai PHẦN 08; Agreement phải đọc snapshot custom mới và fallback snapshot legacy một cách xác định.
