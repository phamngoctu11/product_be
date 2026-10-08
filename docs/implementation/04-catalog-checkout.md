# PHẦN 04 — Catalog, cart và checkout

Ngày hoàn thành: 07/10/2026. Nguồn nghiệp vụ: `target.txt`, WF02 và báo cáo `THIET_KE_CODE_VA_ENDPOINT.xlsx`.

## Kết quả

- Thêm `CatalogDurationCalculator` với rule `CATALOG_V1`, tính độc lập cho từng `OrderItem`: q=1/5/6/10 và madeDay=3 cho 5/14/16/22 ngày.
- Thêm `ProductionCalendarService`: không tính ngày bắt đầu, bỏ Chủ nhật và các ngày ISO cấu hình tại `workflow.production.holidays`.
- Thêm `POST /api/checkout` cho USER và chuyển `POST /api/guest-checkout` sang `CheckoutService`.
- Checkout dùng `DurableRequestExecutor`: scope chứa actor/action, payload canonical, business data/result/outbox commit cùng transaction.
- Khóa pessimistic Cart và UserVoucher trong transaction; guest quota vẫn dùng atomic update và unique usage.
- Mọi checkout mới tạo `CATALOG` Order ở `PENDING_APPROVAL`, `paymentStatus=NOT_DUE`; ONLINE không gọi MoMo và không tạo URL thanh toán.
- Không khởi chạy `ApproveCartProcess` hoặc guest process legacy từ endpoint checkout mới. Các process này chờ cutover ở phần tích hợp.
- Backend lấy giá, tên product/variant, attributes, madeDay, duration và rule version để chụp snapshot. Client không gửi giá catalog.
- Chỉ xóa các `CartItem` đã chọn; variant lạ bị từ chối, không bị bỏ qua âm thầm.
- USER lưu contact snapshot từ hồ sơ và note request. Guest bắt buộc contact theo DTO hiện tại.
- COD của USER yêu cầu reputation >= 20; Guest luôn COD. USER reputation thấp vẫn có thể chọn ONLINE nhưng payment chỉ mở ở WF04.
- USER nhận email và notification tạo đơn. Guest chỉ nhận email; email có link token tra cứu và thời lượng sản xuất lớn nhất của các item.

## API đích

| Method | URL | Quyền | Idempotency | Kết quả |
|---|---|---|---|---|
| POST | `/api/checkout` | USER từ JWT | Bắt buộc `Idempotency-Key` | `CheckoutResponseDTO`, `PENDING_APPROVAL`, không payment URL |
| POST | `/api/guest-checkout` | Guest session | Bắt buộc `Idempotency-Key` | `CheckoutResponseDTO`, token trả một lần/replay cùng key, Guest COD |

Route `/api/cart/approve/{userId}` được đánh dấu deprecated và chuyển controller sang `CheckoutService`; service checkout legacy trong `CartService` không còn được controller mới gọi và chỉ giữ package-private để test/migration cũ chưa bị xóa đột ngột.

## Transaction và event

Trong operation của durable request: khóa cart → kiểm tra toàn bộ selection/catalog → snapshot item → consume voucher → lưu Order → xóa item đã chọn → ghi outbox email/notification/cache. Bất kỳ lỗi nào rollback cả request record, voucher, Order, cart và outbox.

- USER checkout chỉ ghi `ORDER_CREATED` trong transaction tạo đơn và xóa cache giỏ sau commit. Consumer của `ORDER_CREATED` mới route email, notification và cache read model Order/voucher; không invalidate catalog/dashboard.
- Guest: `GUEST_ORDER_CREATED` chứa dữ liệu cần giao email link; consumer có milestone dedup theo orderId. Event legacy thiếu token vẫn được đọc tương thích.

Không có migration schema mới ở PHẦN 04; các cột snapshot đã được tạo trong changelog PHẦN 02.

## Kiểm thử

- Targeted: 49 test, 0 failure/error/skipped trước khi bổ sung test email link.
- Full H2/application: 198 test, 0 failure/error, 6 MySQL opt-in skipped; `BUILD SUCCESS`.
- MySQL 8.0.36 Testcontainers opt-in: 6 test, 0 failure/error/skipped; `BUILD SUCCESS`.
- `git diff --check -- docs src pom.xml`: đạt, chỉ có cảnh báo CRLF.

Log cục bộ: `target/phase04-targeted.log`, `target/phase04-verification-h2.log`, `target/phase04-mysql.log`.

## Giới hạn và bàn giao

- Deadline chính thức chưa được ghi ở checkout. WF04 tính từ `productionStartedAt` khi staff bấm bắt đầu; `ProductionCalendarService` được chuẩn bị để dùng tại PHẦN 09.
- TTL/scope/cấp lại guest token vẫn là quyết định mở. Checkout hiện cấp token hash tương thích để gửi link; endpoint guest ở PHẦN 06/12 phải chốt TTL và chuyển sang overload có scope/validity trước khi dùng `GuestOrderAccessGuard`.
- Raw guest token phải đi qua outbox để email bền vững và replay checkout trả cùng kết quả. Trước production cần xác định retention/mã hóa payload outbox có secret.
- Code checkout/payment/process legacy chưa bị xóa; endpoint mới không gọi chúng. Việc loại bỏ hoàn toàn và migrate process instance thuộc PHẦN 13.

Hành động tiếp theo: PHẦN 05 xây CustomRequest draft/submit, tái sử dụng durable request, snapshot contact và event tạo Order; giá CUSTOM trước agreement phải giữ `null`.
