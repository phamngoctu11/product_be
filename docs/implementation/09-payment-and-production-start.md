# Bàn giao PHẦN 09 — Payment, webhook và staff bắt đầu sản xuất (WF04)

Ngày cập nhật: 12/10/2026.

Trạng thái backend: `IMPLEMENTED`, frontend không được sửa trong lần bàn giao này. Lệnh giao việc cho Antigravity nằm tại [P9_FRONTEND_ANTIGRAVITY.md](../prompts/P9_FRONTEND_ANTIGRAVITY.md).

## Kết quả backend

- USER sở hữu Order `ORDER_ACCEPTED`, có active assignment và `paymentMethod=ONLINE` có thể mở đúng một cửa sổ thanh toán còn hiệu lực.
- `expectedAmount` luôn lấy từ `Order.finalPrice` trên server; request không nhận amount, số tài khoản hoặc nội dung chuyển khoản từ client.
- PaymentAttempt có mã thanh toán duy nhất, `openedAt`, `dueAt = openedAt + 60 phút`, trạng thái độc lập với `OrderStatus` và được bảo vệ bằng Order lock + idempotency key.
- Retry mở payment trả attempt `PENDING`/`PAID` hiện có và không kéo dài `dueAt`.
- COD không tạo PaymentAttempt và không đi qua webhook.
- Webhook SePay xác minh HMAC-SHA256 trên đúng raw body, kiểm tra timestamp, provider reference, tài khoản nhận, chiều tiền vào và số tiền chính xác.
- Provider transaction ID và trạng thái đã khóa bảo đảm callback lặp không phát `PAYMENT_CONFIRMED` lần hai.
- Webhook thành công chỉ ghi PaymentAttempt/Order payment là `PAID`; Order vẫn `ORDER_ACCEPTED`, chưa có `productionStartedAt`.
- Callback đến sau timeout/cancel không hồi sinh Order; attempt được đánh dấu `reconciliationRequired`.
- Worker timeout khóa PaymentAttempt rồi Order. Nếu attempt vẫn `PENDING` khi quá hạn, worker ghi `EXPIRED` và gọi `OrderCancellationService` với nguồn `PAYMENT_TIMEOUT`, không áp dụng reputation penalty.
- Chỉ active assigned STAFF được bắt đầu sản xuất. COD được bắt đầu trực tiếp; ONLINE bắt buộc `paymentStatus=PAID`.
- Start hợp lệ chuyển `ORDER_ACCEPTED -> ORDER_CREATING`, ghi `productionStartedAt`, lý do bắt đầu muộn nếu có và deadline riêng từng item. Ngày bắt đầu không được tính; Chủ nhật và ngày nghỉ cấu hình bị bỏ qua, thứ Bảy vẫn tính.
- Retry start trả lại timestamp/deadline cũ, không chuyển state, ghi history hoặc phát event lần hai.

## REST contract

Mọi API nghiệp vụ dùng wrapper `ApiResponse<T>` hiện có.

### USER mở/đọc thanh toán

```http
POST /api/payments/orders/{orderId}/attempts
Authorization: Bearer <USER JWT>
Idempotency-Key: <stable-key-for-one-logical-open>
```

POST không có request body. Response `data`:

```json
{
  "attemptId": 70,
  "orderId": 10,
  "orderVersion": 8,
  "provider": "SEPAY",
  "paymentCode": "DH...",
  "expectedAmount": 120000,
  "status": "PENDING",
  "openedAt": "2026-10-12T15:00:00",
  "dueAt": "2026-10-12T16:00:00",
  "paidAt": null,
  "bankCode": "VietinBank",
  "accountNumber": "104888199452",
  "accountName": "PHAM NGOC TU",
  "transferContent": "SEVQR DH...",
  "qrUrl": "https://vietqr.app/img?..."
}
```

```http
GET /api/payments/orders/{orderId}/status
Authorization: Bearer <USER JWT>
```

Chỉ USER owner được đọc. Khi chưa từng mở attempt, API trả `PAYMENT_ATTEMPT_NOT_FOUND` thay vì tạo attempt ngầm.

### Webhook SePay

```http
POST /api/webhooks/sepay
Content-Type: application/json
X-SePay-Timestamp: <epoch-seconds>
X-SePay-Signature: sha256=<hex-hmac>
```

Endpoint không yêu cầu JWT nhưng bắt buộc chữ ký provider và trả acknowledgement chính xác:

```json
{"success":true}
```

Frontend không gọi endpoint này và không được biết webhook secret.

### Assigned STAFF bắt đầu sản xuất

```http
POST /api/orders/staff/{orderId}/start-production
Authorization: Bearer <STAFF JWT>
Idempotency-Key: <stable-key-for-one-logical-start>
Content-Type: application/json

{
  "orderVersion": 8,
  "lateStartReason": "Lý do tùy chọn, tối đa 1000 ký tự"
}
```

Response `data`:

```json
{
  "orderId": 10,
  "orderVersion": 10,
  "status": "ORDER_CREATING",
  "productionStartedAt": "2026-10-12T15:30:00",
  "itemDeadlines": [
    {
      "orderItemId": 501,
      "productionDurationDays": 2,
      "computedCompletionAt": "2026-10-14T15:30:00"
    }
  ]
}
```

Client phải dùng `orderVersion` mới từ response cho thao tác kế tiếp; không tự tính deadline.

## Trạng thái và quyền

| Nhánh | Trước start | Điều kiện start | Sau start |
|---|---|---|---|
| COD | `ORDER_ACCEPTED` + `NOT_DUE` | active assigned STAFF | `ORDER_CREATING`, không PaymentAttempt |
| ONLINE chưa trả | `ORDER_ACCEPTED` + `PENDING` | không được start | giữ nguyên |
| ONLINE đã trả | `ORDER_ACCEPTED` + `PAID` | active assigned STAFF | `ORDER_CREATING` |
| Timeout payment | `ORDER_ACCEPTED` + attempt `PENDING` quá hạn | worker hệ thống | `CANCELLED` + `EXPIRED` |

Payment success không tự start, không gửi email trạng thái mới cho USER và không đổi Order sang `ORDER_CREATING`. Consumer chỉ correlate wait-payment và thông báo assigned STAFF. Event `PRODUCTION_STARTED` mới hoàn tất task start, gửi email cho USER/Guest và notification cho USER đăng nhập.

## Cấu hình local hiện tại

- Bank: VietinBank.
- Số tài khoản: `104888199452`.
- Chủ tài khoản: `PHAM NGOC TU`.
- Nội dung QR: tiền tố `SEVQR` + payment code.
- Webhook public: `${APP_PUBLIC_BASE_URL}/api/webhooks/sepay`, hiện dùng URL ngrok local trong `application.yaml`/`application-dev.yaml`.
- Secret local được cấu hình theo thông tin test do chủ dự án cung cấp. Production mặc định tắt webhook và bắt buộc lấy toàn bộ dữ liệu nhạy cảm từ environment.

## Dữ liệu, event và vận hành

- Liquibase `08-payment-start.xml` thêm index `(order_id,status,opened_at)` và `(status,due_at)` cho lookup/status worker; không đổi quyết định chung dùng `Double` cho tiền.
- Event/outbox mới: `PAYMENT_OPENED`, `PAYMENT_CONFIRMED`, `PRODUCTION_STARTED`; đều nằm trong retry allow-list Redis Stream.
- Timeout worker mặc định quét mỗi 60 giây, batch 100; cấu hình qua `sepay.payment.timeout-scan-delay-ms` và `sepay.payment.timeout-batch-size`.
- Camunda hook đã có cho mở payment, payment success và staff start. Việc thay toàn bộ process definition legacy/cutover instance vẫn thuộc PHẦN 13; database, outbox và timeout worker là nguồn thực thi an toàn trong giai đoạn rollout.
- Payload webhook đã xác minh được lưu cho audit/reconciliation, không log JWT hoặc webhook secret và không trả provider payload về frontend.

## Kiểm tra

Test P9 bao phủ chữ ký đúng/sai/quá hạn, payment success giữ `ORDER_ACCEPTED`, callback lặp, callback muộn, payment–timer race, start khi ONLINE chưa trả, COD start, cancel–start, start lặp, deadline bỏ Chủ nhật, server-side amount và COD không tạo attempt.

Lệnh xác minh cuối:

```powershell
.\mvnw.cmd test
git diff --check
```

