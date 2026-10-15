# Lệnh giao việc cho Antigravity — triển khai frontend PHẦN 09

Từ PowerShell, copy toàn bộ lệnh giao việc này vào clipboard rồi dán vào cửa sổ chat Antigravity trong VS Code:

```powershell
Get-Content "D:\me\product_be\docs\prompts\P9_FRONTEND_ANTIGRAVITY.md" -Raw | Set-Clipboard
```

Bạn đang làm trong frontend Angular tại `D:\me\product_fe`. Chỉ sửa frontend; không sửa backend `D:\me\product_be`.

Trước khi code, đọc đầy đủ:

1. `D:\me\product_be\docs\implementation\09-payment-and-production-start.md`
2. `D:\me\product_be\docs\use-cases\WF04-payment-and-start\usecase.md`
3. `D:\me\product_be\docs\target.txt`, riêng quy tắc WF04/payment/deadline.
4. Các model/service/trang Order USER, staff assigned orders, notification và shared detail hiện có trong FE.

Mục tiêu: triển khai UI P9 hoàn chỉnh cho USER thanh toán ONLINE và assigned STAFF chủ động bắt đầu sản xuất. Không gọi webhook từ FE, không tạo payment COD, không nhận/tự sửa amount và không tự chuyển trạng thái sản xuất.

## Model và API typed

- Thêm type `PaymentAttemptStatus = 'PENDING' | 'PAID' | 'FAILED' | 'EXPIRED'` và interface `PaymentAttempt` đúng response trong báo cáo backend.
- Thêm `StartProductionRequest` và `StartProductionResult`, trong đó có `itemDeadlines`.
- Map `paymentMethod`, `paymentStatus`, `productionStartedAt`, `plannedStartAt`, `version` và `computedCompletionAt` từ Order/OrderItem hiện có; không gộp `paymentStatus` vào `OrderStatus`.
- Service USER:
  - `POST /api/payments/orders/{orderId}/attempts`, header `Idempotency-Key`, không body.
  - `GET /api/payments/orders/{orderId}/status`.
- Service STAFF:
  - `POST /api/orders/staff/{orderId}/start-production`, header `Idempotency-Key`, body `{orderVersion, lateStartReason}`.
- Đọc đúng wrapper `ApiResponse<T>` hiện có. Không hard-code số tài khoản, tên ngân hàng, chủ tài khoản, amount, payment code hoặc QR URL; luôn dùng response backend.

## Idempotency và chống double action

- Tạo một UUID cho mỗi logical action “mở thanh toán” hoặc “bắt đầu sản xuất”. Retry do timeout/network/double click phải tái sử dụng key đó.
- Chỉ tạo key mới khi người dùng chủ động bắt đầu một logical action mới sau khi action cũ đã kết thúc/thất bại hợp lệ.
- Disable button và hiển thị trạng thái đang xử lý trong lúc request chạy.
- HTTP 409 stale version: reload Order/payment data trước khi cho thao tác lại; không tự tăng version phía client.

## UI USER — ONLINE payment

- Chỉ hiện CTA thanh toán khi USER là owner, Order `ORDER_ACCEPTED`, `paymentMethod=ONLINE` và chưa `PAID`/`EXPIRED`/`CANCELLED`.
- Lần đầu bấm CTA gọi POST mở payment. Retry hoặc mở lại detail phải ưu tiên GET status; nếu GET trả `PAYMENT_ATTEMPT_NOT_FOUND` mới hiển thị CTA mở payment.
- Modal/panel thanh toán hiển thị QR từ `qrUrl`, bank, account number, account name, `expectedAmount`, `transferContent`, nút copy và `dueAt`.
- Nội dung chuyển khoản phải hiển thị nguyên văn backend trả; cảnh báo USER không sửa nội dung hoặc số tiền.
- Countdown chỉ dựa trên `dueAt` tuyệt đối. Không tự cộng thêm 60 phút, không kéo dài khi refresh/retry.
- Khi attempt `PENDING`, poll GET status khoảng 3–5 giây/lần; dừng khi `PAID`, `FAILED`, `EXPIRED`, Order không còn phù hợp, modal/component bị destroy hoặc hết `dueAt`. Không tạo nhiều timer/subscription khi mở modal lại.
- Khi `PAID`, hiển thị “Đã thanh toán, đang chờ nhân viên bắt đầu sản xuất”; Order vẫn phải là `ORDER_ACCEPTED`. Không điều hướng/hiển thị như đã sản xuất.
- Khi `EXPIRED`/`FAILED`, dừng poll, reload Order và hiển thị trạng thái hủy phù hợp. Không tự mở attempt mới.
- COD và Guest không gọi hai endpoint payment, không hiển thị QR và không giả lập `PAID`.

## UI assigned STAFF — bắt đầu sản xuất

- Tích hợp vào detail trong “Đơn của tôi”, không hiển thị action ở claimable list trước khi claim và không cho staff khác có entry point.
- Nút “Bắt đầu sản xuất” chỉ hiện khi Order `ORDER_ACCEPTED` và:
  - COD: được phép ngay, không chờ payment.
  - ONLINE: chỉ enable khi `paymentStatus=PAID`.
- Khi ONLINE `PENDING`, hiển thị “Đang chờ khách thanh toán”; notification payment chỉ dùng để reload Order/payment status, không tự gọi start.
- Gửi `orderVersion` mới nhất và key ổn định. Không gửi `staffId`, payment status, timestamp hay deadline từ client.
- `lateStartReason` tùy chọn, tối đa 1000 ký tự. Nếu UI xác định `now > plannedStartAt`, có thể yêu cầu người dùng nhập lý do trước khi submit, nhưng backend response vẫn là nguồn sự thật.
- Thành công: cập nhật ngay `ORDER_CREATING`, `productionStartedAt`, `orderVersion` và deadline từng item từ response; khóa action start vĩnh viễn cho trạng thái đó.
- Retry sau response bị mất có thể nhận lại kết quả cũ; không hiển thị như một lần start mới và không thay timestamp/deadline.
- Staff không tự tính ngày hoàn thành, không cộng shipping days vào deadline sản xuất và không release assignment.

## Hiển thị trạng thái

- Tách badge Order và payment:
  - `ORDER_ACCEPTED + PENDING`: đã nhận đơn, chờ thanh toán.
  - `ORDER_ACCEPTED + PAID`: đã thanh toán, chờ staff bắt đầu.
  - `ORDER_ACCEPTED + NOT_DUE + COD`: COD, chờ staff bắt đầu.
  - `ORDER_CREATING`: đang sản xuất; hiển thị `productionStartedAt` và deadline item.
  - `CANCELLED + EXPIRED/FAILED`: đã hủy do thanh toán.
- Không map payment success trực tiếp thành `ORDER_CREATING`.
- Không dùng state legacy `PENDING_PAYMENT`, `PENDING_WAREHOUSE`, `WAREHOUSE_ASSIGNED`, `PENDING_KCS` cho UI P9 mới.

## Xử lý lỗi

- Map tối thiểu: `PAYMENT_NOT_AVAILABLE`, `PAYMENT_METHOD_NOT_ONLINE`, `PAYMENT_ATTEMPT_NOT_FOUND`, `PAYMENT_ATTEMPT_EXPIRED`, `PRODUCTION_START_FORBIDDEN`, `ORDER_NOT_READY_FOR_PRODUCTION`, `PRODUCTION_DATA_INCOMPLETE`, `ORDER_VERSION_CONFLICT`, `REQUEST_KEY_CONFLICT`.
- 401/403: không lộ bank/payment detail, điều hướng/xử lý theo auth guard hiện có.
- Lỗi QR image không được đổi sang QR do client tự dựng; vẫn hiển thị thông tin chuyển khoản backend trả và cho retry tải ảnh.
- Không ghi JWT, payment data thô hoặc bất kỳ webhook secret nào vào console/local storage.

## Không được triển khai

- Không gọi `/api/webhooks/sepay` từ browser.
- Không nhúng `quachhieuphuong` hoặc header chữ ký vào frontend.
- Không tiếp tục dùng MoMo route cũ cho flow P9 mới.
- Không tạo amount/account/content từ Order hoặc form client.
- Không thêm payment cho ChangeRequest, COD hoặc Guest.
- Không tự set `PAID`, `ORDER_CREATING`, `productionStartedAt` hay deadline ở optimistic UI trước response backend.

## Kiểm thử bắt buộc

- ONLINE mở payment một lần; double click/retry không tạo attempt hoặc kéo dài dueAt.
- Refresh modal dùng attempt cũ; countdown và polling được cleanup.
- Payment chuyển `PENDING -> PAID` nhưng Order vẫn `ORDER_ACCEPTED`.
- COD/Guest không gọi payment API.
- Assigned staff start COD; assigned staff start ONLINE đã PAID.
- ONLINE chưa PAID, staff khác và Order CANCELLED không thể start.
- Start retry giữ nguyên timestamp/deadline; 409 stale reload dữ liệu.
- Responsive/mobile, copy account/content, QR loading/error và accessibility cơ bản.

## Hoàn tất

- Chạy `npm.cmd run build` tại `D:\me\product_fe` và test liên quan nếu có.
- Chạy `rg -n "momo|PENDING_PAYMENT|/api/webhooks/sepay|quachhieuphuong" src` và giải thích/xóa mọi kết quả legacy không còn hợp lệ với P9.
- Báo cáo file đã sửa, hành vi, command/kết quả build/test và điểm còn thiếu.
- Không tuyên bố hoàn tất nếu còn lỗi TypeScript/build, polling leak, double action hoặc FE chứa webhook secret.

