# Bàn giao PHẦN 06 — Nghiệp vụ hủy Order dùng chung (WF09)

Ngày cập nhật: 08/10/2026.

## Kết quả

PHẦN 06 đã triển khai một ranh giới transaction duy nhất tại `OrderCancellationService` cho bốn nhóm caller:

- USER owner tự hủy.
- Guest có token đúng Order và scope `CANCEL`.
- Manager từ chối ở bước review.
- Worker nội bộ hủy do `CUSTOM_CONFIRMATION_TIMEOUT`, `PAYMENT_TIMEOUT` hoặc `PAYMENT_FAILED`.

Kernel khóa Order, kiểm tra version và policy transition, sau đó ghi đồng bộ `CANCELLED`, audit/history, reputation, voucher, active assignment, durable request và outbox. Không có nhánh hủy nào hoàn stock hoặc tự refund.

## API khách hàng

### USER

```http
POST /api/orders/{orderId}/cancellation
Authorization: Bearer <JWT>
Idempotency-Key: <key>
Content-Type: application/json

{
  "orderVersion": 0,
  "reason": "Tôi không còn nhu cầu"
}
```

Identity luôn lấy từ JWT. Client không được gửi cancellation source, userId, penalty hoặc finalPrice.

### Guest

```http
GET /api/guest/orders/{orderId}/cancellation?token=<raw-token>
```

GET chỉ trả dữ liệu xác nhận đã giới hạn và email đã mask; không thay đổi Order. Link không còn quyền hủy từ `ORDER_ACCEPTED` trở đi bị từ chối.

```http
POST /api/guest/orders/{orderId}/cancellation?token=<raw-token>
Idempotency-Key: <key>
Content-Type: application/json

{
  "orderVersion": 0,
  "reason": "Không nhận đơn nữa"
}
```

Guest checkout mới cấp token hash-only với scope `READ,CANCEL,CONFIRM_RECEIPT`, hạn mặc định hiện tại là 30 ngày. Read và cancel đều qua rate limit theo `orderId`; ngưỡng tạm thời lần lượt là 30 và 8 request/giờ. Đây là cấu hình cần đưa ra property trước production nếu product owner chọn ngưỡng khác.

## Policy trạng thái và tiền

USER/Guest chỉ tự hủy tại:

- `PENDING_APPROVAL`;
- `PENDING_ASSIGNMENT`;
- `DISCUSSING`;
- `WAITING_STAFF_CONFIRMATION`.

Manager reject chỉ hợp lệ tại `PENDING_APPROVAL`. Payment timeout/fail chỉ dùng `ORDER_ACCEPTED`; `PENDING_PAYMENT` vẫn được nhận tạm thời cho dữ liệu/worker legacy và không được tạo bởi code mới.

Tiền tiếp tục dùng `Double` theo quyết định dự án. Penalty USER dựa duy nhất vào `finalPrice` chính thức:

| finalPrice | Điểm trừ |
|---:|---:|
| `< 1.000.000` | 1 |
| `1.000.000` đến `5.000.000` | 2 |
| `> 5.000.000` đến `10.000.000` | 3 |
| `> 10.000.000` | 5 |

`finalPrice=null` có nghĩa chưa có giá chính thức và penalty bằng 0; service không suy giá từ `totalPrice` và không gán giá 0 giả. User được khóa riêng trước khi trừ điểm. Nếu không đủ reputation, toàn bộ transaction thất bại và Order giữ nguyên.

## Voucher và assignment

- `UserVoucher` được khóa bi quan, chuyển `used=false`, `usedDate=null`; không hoàn point cost.
- Guest voucher tăng lại global quota nhưng giữ nguyên `GuestVoucherUsage`, nên guest cũ không lấy lại lượt chống spam.
- Query `findActiveByOrderIdForUpdate` chỉ đóng assignment có `activeOrderId` đúng Order, với reason `ORDER_CANCELLED`.
- Request lặp hoặc Order đã `CANCELLED` không trừ điểm, hoàn voucher, release assignment hoặc phát event lần hai.

Không cần migration mới: version Order, cancellation metadata, assignment unique, transition audit, durable request và outbox đã có từ PHẦN 02–03.

## Idempotency, audit và event

Mutation bắt buộc có `Idempotency-Key`. Scope gồm actor/source và orderId; payload canonical gồm version, reason, actor, source và reference.

- Cùng key/cùng payload trả JSON kết quả đã lưu.
- Cùng key/khác payload trả `REQUEST_KEY_CONFLICT`.
- Khóa Order và optimistic version phân xử hai nguồn cạnh tranh.
- Mỗi cancellation thành công phát đúng một `ORDER_CANCELLED` trong cùng transaction.

Consumer `ORDER_CANCELLED` thực hiện sau commit:

- correlate message catch event `ORDER_CANCELLED` nếu process đang đăng ký;
- hủy attribution đang chờ;
- gửi email cho USER/Guest owner;
- gửi notification cho USER, assigned staff và kênh manager phù hợp;
- invalidate Order/voucher/reputation/list cache liên quan.

Không còn xóa thô Camunda process trong transaction. BPMN mục tiêu cần có message catch/interrupting event tương ứng ở PHẦN 13; process legacy không có subscription được giữ nguyên để reconciliation thay vì bị xóa.

## Tương thích tạm thời

- Manager review cũ đã gọi kernel với source `MANAGER_REJECTED`; contract review đầy đủ có order/task version và key từ client sẽ được làm ở PHẦN 07.
- Scheduler `PENDING_PAYMENT` và callback MoMo cũ đã gọi kernel system thay vì tự hoàn kho/xóa process. PaymentAttempt, webhook race và PT1H mục tiêu thuộc PHẦN 09.
- `CancelOrderDelegate` không còn quyền tự tạo source `SYSTEM`. Nó chỉ chấp nhận Order đã được application service hủy; nếu process legacy tới task khi DB chưa `CANCELLED`, delegate fail rõ ràng để tránh một đường hủy không audit.

## Kiểm thử

Đã kiểm tra:

- toàn bộ biên penalty, kể cả `finalPrice=null`;
- không đủ reputation, owner sai, version cũ và tự hủy sau `ORDER_ACCEPTED`;
- Guest token/scope và Guest không chạm reputation;
- voucher/assignment/history/outbox đúng một lần;
- cùng idempotency key trả cùng kết quả; payload khác conflict;
- transaction tích hợp thật chỉ có một transition audit và một `ORDER_CANCELLED`;
- handler hậu kỳ correlate workflow trước email/notification;
- scheduler payment legacy và delegate không mở đường `SYSTEM` tùy ý.

```text
.\mvnw.cmd test
232 test chạy, 0 failure, 0 error, 6 MySQL opt-in test skipped
BUILD SUCCESS
```

## Ranh giới chưa thuộc PHẦN 06

- Chưa triển khai manager review/assignment API mới (PHẦN 07).
- Chưa triển khai timer CUSTOM PT24H (PHẦN 08).
- Chưa triển khai PaymentAttempt, webhook reconciliation và PT1H (PHẦN 09).
- Chưa sửa BPMN lifecycle mục tiêu để bắt message `ORDER_CANCELLED` (PHẦN 13).
- Chưa chạy MySQL opt-in trong checkpoint này; H2 đã chứng minh atomicity/idempotency, còn xác minh engine/database production nằm ở rollout.

Hành động tiếp theo: review PHẦN 06, sau đó triển khai PHẦN 07 — manager review và assignment dùng kernel hủy này cho nhánh reject.
