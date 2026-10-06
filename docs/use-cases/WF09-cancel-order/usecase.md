# WF09 — Hủy đơn (`cancel_order`)

Ngày lập: 06/10/2026. Đặc tả hệ thống mục tiêu, không phải xác nhận hành vi runtime hiện tại.

Nguồn: [target.txt](../../target.txt), mục B2–B3, C3–C4, D1–D3, E/WF09, E1, F09b–F11, F16, F18 và G–K.

## 1. Phạm vi và kết quả

WF09 cung cấp một nghiệp vụ hủy dùng chung nhưng không mở quyền hủy tùy ý. Luồng nhận ba nhóm nguồn hợp lệ:

- Khách chủ động: USER hoặc Guest sở hữu Order, chỉ trong các trạng thái trước `ORDER_ACCEPTED`.
- Manager từ chối: quyết định review tại `PENDING_APPROVAL`.
- Hệ thống: timeout xác nhận CUSTOM 24 giờ, payment ONLINE hết hạn một giờ hoặc payment thất bại hợp lệ.

Khi hủy thành công, hệ thống chuyển Order sang `CANCELLED`, ghi `endOrderTime`, source/reason/history, dừng bước workflow còn chờ, release đúng active assignment nếu có, xử lý voucher và phát thông báo phù hợp. Mọi tác động chỉ được xảy ra một lần.

WF09 không hoàn tồn kho, không reassign staff, không xử lý nghỉ việc và không tự công bố hoàn tiền. Order `CANCELLED` có thể là đầu vào của nghiệp vụ reorder tại mục F17 trong tài liệu mục tiêu, nhưng reorder không nằm trong WF09.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân/nguồn chính | Kết quả |
|---|---|---|---|
| UC09.1 | Khách chủ động hủy Order | USER owner hoặc Guest có cancel token | `CANCELLED` hoặc lỗi quyền/trạng thái/reputation/rate limit |
| UC09.2 | Manager từ chối Order | MANAGER đang xử lý review | `CANCELLED` với nguồn `MANAGER_REJECTED` |
| UC09.3 | Hệ thống hủy do timeout/payment fail | Timer/sự kiện payment nội bộ đã được xác thực | `CANCELLED` với lý do hệ thống, không trừ reputation |

Timer, backend và Camunda là thành phần nội bộ nên UC09.3 không có actor người bên ngoài trong Use Case Diagram. Cổng thanh toán chỉ cung cấp kết quả payment tại WF04; backend phải xác minh trước khi dùng làm nguồn hủy.

## 3. Ma trận quyền và trạng thái

| Nguồn hủy | Trạng thái/điểm chờ được phép | Reputation | Ghi chú |
|---|---|---|---|
| USER tự hủy | `PENDING_APPROVAL`, `PENDING_ASSIGNMENT`, `DISCUSSING`, `WAITING_STAFF_CONFIRMATION` | Trừ theo finalPrice chính thức, nếu chưa có giá chính thức thì không trừ | Phải là owner từ JWT |
| Guest tự hủy | Trạng thái trước `ORDER_ACCEPTED` mà Guest thực tế có thể đi qua | Không có | Bắt buộc token đúng Order và scope cancel |
| MANAGER từ chối | `PENDING_APPROVAL` tại manager review | Không trừ | Không phải quyền hủy tùy ý sau review |
| CUSTOM confirmation timeout | Wait state chưa có form hợp lệ sau PT24H từ `managerApprovedAt` | Không trừ | Form hợp lệ đã lưu phải thắng timer |
| ONLINE payment timeout/fail | `ORDER_ACCEPTED`, PaymentAttempt đang chờ và chưa `PAID` | Không trừ | Webhook thành công và timeout phải có một kết quả |

USER/Guest không được tự hủy từ `ORDER_ACCEPTED`, `ORDER_CREATING`, `READY_TO_SHIP`, `SHIPPING` hoặc `DELIVERED`. Điều này vẫn áp dụng khi link/token còn hạn và khi ONLINE đang chờ thanh toán. Email `ORDER_ACCEPTED` không có nút hủy.

## 4. Quy tắc nghiệp vụ chung

### 4.1 Nguồn hủy và dữ liệu tin cậy

- API công khai không cho client tự chọn source `MANAGER_REJECTED`, `TIMEOUT` hoặc `PAYMENT_FAILED`.
- USER identity lấy từ JWT. Guest phải có `orderId + token` gắn đúng Order và scope hủy; chỉ orderId, guestSessionId, email hoặc phone không đủ quyền.
- MANAGER identity lấy từ JWT và phải sở hữu/có quyền trên manager review task hiện hành.
- Lệnh hệ thống phải đến từ timer/worker nội bộ đã xác thực và kèm reference của wait state hoặc PaymentAttempt.
- `reason` của khách/manager phải được validate. Lý do hệ thống dùng mã ổn định như `CUSTOM_CONFIRMATION_TIMEOUT`, `PAYMENT_TIMEOUT`, `PAYMENT_FAILED` và metadata tham chiếu.
- Guest token chỉ lưu hash, có expiry/revocation theo cấu hình và không xuất hiện trong log/event.

### 4.2 Link hủy của Guest và an toàn HTTP

- Email xác nhận tạo Order ở `PENDING_APPROVAL` chứa link đến trang xác nhận hủy đúng Order.
- Mở link bằng GET chỉ đọc thông tin được phép và hiển thị trang xác nhận, không thay đổi trạng thái.
- Sau khi Guest bấm xác nhận, UI mới gọi endpoint mutation bằng POST/DELETE phù hợp kèm token, reason, version và idempotency key.
- Backend kiểm tra token, trạng thái và rate limit Redis theo `orderId`. Rate limit chỉ chống spam, không thay thế xác thực.
- Link mở sau khi Order đã `ORDER_ACCEPTED` phải bị từ chối dù token chưa hết hạn.

### 4.3 Reputation khi USER tự hủy

Chỉ USER chủ động hủy Order có `finalPrice` chính thức mới bị trừ điểm:

| Final price chính thức | Điểm trừ |
|---|---:|
| `< 1.000.000 VND` | 1 |
| `1.000.000–5.000.000 VND` | 2 |
| `> 5.000.000–10.000.000 VND` | 3 |
| `> 10.000.000 VND` | 5 |

- So sánh tiền bằng kiểu chính xác theo đơn vị VND, không dùng floating point.
- Nếu reputation hiện có thấp hơn mức trừ, trả lỗi và không hủy, không ghi số âm.
- CUSTOM trước khi staff xác nhận có thể chưa có `finalPrice` chính thức. Khi đó vẫn cho owner hủy trước `ORDER_ACCEPTED`, không trừ reputation, không kiểm tra số dư để phạt và không gán giá 0 giả làm finalPrice.
- Final price CUSTOM nếu đã hợp lệ được tính từ giá thỏa thuận sau voucher. CATALOG dùng snapshot/tổng tiền chính thức do server tính.
- Guest, manager reject, timeout và payment fail không bị trừ reputation.
- Việc trừ điểm phải có ledger/idempotency unique theo Order và loại tác động để retry không trừ lần hai.

### 4.4 Voucher

- `UserVoucher` đã dùng được trả về ví bằng `used=false`, `usedDate=null` đúng một lần.
- Không hoàn `pointCost` đã dùng để đổi voucher vì voucher đã được trả lại và có thể dùng lại theo policy.
- Với Guest voucher, hoàn quota tổng đúng một lần nhưng giữ dấu vết usage theo session/email/phone. Guest đó không được sử dụng lại cùng giới hạn, nhằm chống spam.
- Việc phục hồi voucher/quota nằm trong transaction hủy hoặc có ledger/ràng buộc bù trừ nguyên tử tương đương.
- Request lặp không phục hồi quota nhiều lần và không tạo thêm voucher.

### 4.5 Assignment, workflow và dữ liệu khác

- Nếu Order có active assignment, đóng đúng assignment đó và release staff một lần.
- Nếu assignment đã đóng hoặc không tồn tại, không release lần nữa và không sửa lịch sử.
- Dừng/hủy các user task, timer và wait state liên quan. Không xóa process trực tiếp để thay thế nghiệp vụ `CANCELLED` đã được audit.
- Không reassign Order sau hủy và không mở luồng staff nghỉ việc/thay staff.
- Không reserve, deduct hoặc restore tồn kho thành phẩm.
- Không xóa Order, OrderItem, agreement, chat, checkpoint, PaymentAttempt hoặc lịch sử. Các dữ liệu này được giữ để audit theo quyền.
- Cache Order, danh sách, voucher và reputation liên quan phải được invalidate sau thay đổi.

### 4.6 Payment và hoàn tiền

- Customer self-cancel, manager reject và CUSTOM timeout xảy ra trước khi có payment hợp lệ theo luồng mục tiêu.
- Payment timeout/fail chỉ hủy khi PaymentAttempt chưa `PAID` và Order còn ở điểm chờ phù hợp.
- Webhook thành công và timer timeout phải khóa/kiểm tra cùng trạng thái để chỉ một kết quả nghiệp vụ thắng.
- Callback thành công đến sau khi Order đã bị hủy không tự khôi phục Order, không tự đánh dấu quy trình hoàn tất và không tự công bố đã hoàn tiền. Ghi ngoại lệ đối soát để xử lý theo policy tài chính riêng.
- WF09 không có API/refund workflow và không được trả message “đã hoàn tiền” nếu chưa có giao dịch hoàn thực tế.
- COD thu qua shipper ngoài hệ thống; hủy trước `ORDER_ACCEPTED` không tạo PaymentAttempt COD.

### 4.7 Transaction và idempotency

Một transaction hủy tối thiểu phải khóa Order/version và lưu đồng bộ:

- Cancellation source/reason/reference và actor.
- `CANCELLED`, `endOrderTime`, aggregate version và history.
- Reputation ledger/giá trị mới nếu USER tự hủy có penalty.
- Voucher restoration/quota reversal marker.
- Đóng active assignment nếu có.
- Outbox/job để dừng workflow, thông báo và invalidate cache.
- Idempotency result cho request có key.

Hai nguồn hủy hoặc một nguồn hủy cạnh tranh với submit form, webhook hay staff start phải có đúng một kết quả. Request cùng key/cùng nội dung trả kết quả đã lưu; cùng key/khác nội dung trả conflict. Nếu đã `CANCELLED`, không chạy lại penalty, voucher, release hoặc event.

### 4.8 Email và notification

Hủy là nhánh ngoại lệ, được gửi thông báo riêng và không được tính thành email trạng thái thứ năm của luồng bình thường:

| Nguồn | Chủ đơn | Staff | Manager |
|---|---|---|---|
| USER/Guest tự hủy | Email, USER có thêm notification | Thông báo nếu đang phụ trách | Cập nhật hàng công việc/audit nếu liên quan |
| MANAGER từ chối | Email, USER có thêm notification | Thông báo nếu đã có assignment bất thường | Người quyết định thấy kết quả task/audit |
| CUSTOM timeout | Email, USER có thêm notification | Thông báo nếu đã được gán | Cập nhật vận hành nếu được cấu hình |
| Payment timeout/fail | Email, USER có thêm notification | Thông báo staff đang phụ trách | Cập nhật vận hành nếu được cấu hình |

Guest chỉ nhận email, không tạo notification cho userId rỗng. Nội dung phải gồm Order, lý do/source, thời điểm và hậu quả cần biết, nhưng không tuyên bố hoàn tiền ngoài thực tế.

Consumer chống trùng theo `orderId + cancellation milestone + recipient/channel`. Lỗi gửi không rollback hủy và được retry sau commit.

### 4.9 Vai trò Camunda

WF09 phù hợp làm interrupting event subprocess/call activity dùng chung trong process vòng đời hiện có:

- Nhánh từ chối ở manager review gọi nghiệp vụ hủy với source cố định.
- Timer PT24H của CUSTOM và PT1H payment gọi cùng application service với expected wait-state/reference.
- Customer cancel sau khi backend xác thực/commit phát `ORDER_CANCELLED` để kết thúc các task/timer còn mở.
- Engine không tự cập nhật trực tiếp các bảng Order/voucher/reputation bằng logic khác với application service.
- Nếu engine và DB không chung transaction, dùng outbox/job idempotent và reconciliation. Lỗi correlate không được để process tiếp tục sang bước sản xuất sau khi DB đã `CANCELLED`.
- Không tạo process vòng đời thứ hai và không xóa process thô để né audit hủy.

## 5. UC09.1 — Khách chủ động hủy Order

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Cho chủ đơn hủy an toàn trước khi Order được chấp nhận chính thức |
| Kích hoạt | USER chọn hủy trong MyOrder hoặc Guest xác nhận trên trang mở từ email |
| Tiền điều kiện | Owner/token hợp lệ; trạng thái trước `ORDER_ACCEPTED`; chưa có kết quả hủy cạnh tranh |
| Đầu vào | orderId, reason, order version, idempotency key; JWT hoặc guest cancel token |
| Thành công | Order `CANCELLED`, tác động reputation/voucher/assignment đúng nguồn và có lịch sử |
| Bảo đảm thất bại | Không hủy, không trừ điểm, không phục hồi voucher/quota và không release staff |

### Luồng chính

| Bước | USER/Guest | Hệ thống |
|---|---|---|
| 1 | Mở chức năng hủy | USER từ MyOrder; Guest GET link chỉ mở trang xác nhận |
| 2 | Nhập/chọn lý do và bấm xác nhận | Gửi mutation cùng version và idempotency key |
| 3 | — | Xác thực owner JWT hoặc guest token/scope, sau đó kiểm tra rate limit theo orderId |
| 4 | — | Khóa Order, version, voucher usage, reputation và active assignment liên quan |
| 5 | — | Kiểm tra trạng thái thuộc tập trước `ORDER_ACCEPTED` và request chưa được xử lý |
| 6 | — | USER có finalPrice chính thức: tính penalty, kiểm tra số dư; CUSTOM chưa có giá/Guest: penalty bằng 0 |
| 7 | — | Trong transaction ghi CANCELLED/history/end time, penalty nếu có, phục hồi voucher/quota, đóng assignment và tạo outbox |
| 8 | Nhận kết quả | Trả status, source, reason, penalty, voucher result và cancelledAt theo quyền |
| 9 | — | Sau commit dừng task/timer, gửi thông báo, invalidate cache; lỗi side effect được retry |

### Ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| A01 | USER không phải owner hoặc Guest thiếu/sai token | 401/403/404 theo policy, không lộ Order |
| A02 | Guest chỉ có orderId/email/phone/session | Từ chối, không hủy |
| A03 | GET từ email/link scanner | Chỉ hiển thị trang, không mutation |
| A04 | Trạng thái đã `ORDER_ACCEPTED` hoặc muộn hơn | 409, không hủy dù token còn hạn |
| A05 | USER có penalty nhưng reputation không đủ | Lỗi nghiệp vụ, Order giữ nguyên |
| A06 | CUSTOM chưa có finalPrice chính thức | Cho hủy không penalty, không dùng giá 0 giả |
| A07 | Reason thiếu/không hợp lệ | 400 |
| A08 | Version cũ hoặc nguồn khác vừa thắng | 409 hoặc trả trạng thái hiện hành |
| A09 | Cùng key và cùng nội dung đã hủy | Trả kết quả cũ, không lặp side effect |
| A10 | Cùng key nhưng nội dung khác | 409 |
| A11 | Vượt rate limit | Từ chối tạm thời nhưng vẫn giữ kiểm tra quyền/token |
| A12 | Commit thành công nhưng workflow/email lỗi | Order vẫn CANCELLED, retry sau commit |

## 6. UC09.2 — Manager từ chối Order

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Kết thúc Order mà manager xác định không thể nhận làm |
| Kích hoạt | MANAGER chọn REJECT tại user task manager review |
| Tiền điều kiện | Actor có quyền MANAGER; Order `PENDING_APPROVAL`; task/version hiện hành |
| Đầu vào | orderId, decision REJECT, reason, task/order version, idempotency key hoặc task completion key |
| Thành công | `CANCELLED` với source `MANAGER_REJECTED`, không trừ reputation |
| Bảo đảm thất bại | Review task chưa hoàn tất, Order/voucher/assignment không thay đổi |

### Luồng chính

| Bước | MANAGER | Hệ thống |
|---|---|---|
| 1 | Mở Order đang chờ review | Kiểm tra quyền và task/version hiện hành |
| 2 | Chọn từ chối và nhập reason | Gửi decision REJECT |
| 3 | — | Khóa Order/task và kiểm tra còn `PENDING_APPROVAL` |
| 4 | — | Gọi cùng nghiệp vụ cancel với source do server đặt là `MANAGER_REJECTED` |
| 5 | — | Transaction ghi CANCELLED/history/end time, phục hồi voucher/quota, đóng assignment nếu có và tạo outbox; penalty bằng 0 |
| 6 | Nhận kết quả | Hiển thị quyết định từ chối đã hoàn tất |
| 7 | — | Hoàn tất/rẽ nhánh manager task, gửi thông báo chủ đơn và đồng bộ hàng công việc |

MANAGER không được dùng UC09.2 như endpoint hủy tự do ở `ORDER_ACCEPTED` hoặc các trạng thái sản xuất/giao hàng.

### Ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| B01 | Không có quyền MANAGER | 403 |
| B02 | Order không còn PENDING_APPROVAL | 409, không ghi đè quyết định hiện hành |
| B03 | Reason thiếu | 400, task vẫn chờ |
| B04 | Hai manager quyết định đồng thời | Khóa/version chỉ cho một quyết định có hiệu lực |
| B05 | Order đã được khách hủy | Trả trạng thái hiện hành, không hoàn tất nhánh reject lần hai |
| B06 | DB commit nhưng task/event/email lỗi | Giữ CANCELLED, retry/reconcile task và thông báo |

## 7. UC09.3 — Hệ thống hủy do timeout/payment fail

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Kết thúc Order khi điều kiện chờ bắt buộc hết hạn hoặc payment thất bại |
| Kích hoạt | Timer CUSTOM PT24H, timer payment PT1H hoặc kết quả payment fail đã xác minh |
| Tiền điều kiện | Order và wait state/PaymentAttempt vẫn ở trạng thái chờ tương ứng |
| Đầu vào | orderId, system reason, waitState/paymentAttempt reference, dueAt/result, event/job id |
| Thành công | `CANCELLED`, không trừ reputation, side effect đúng một lần |
| Bảo đảm thất bại | Nếu form/payment thành công đã thắng, không hủy và không phục hồi/release |

### Nhánh CUSTOM confirmation timeout

1. Camunda timer PT24H đến hạn từ `managerApprovedAt`.
2. Worker gọi cancel service với `CUSTOM_CONFIRMATION_TIMEOUT` và reference của bước chờ.
3. Service khóa Order và trạng thái xác nhận.
4. Nếu form hợp lệ đã được lưu và bước chờ đã hoàn tất, timer thua, không hủy.
5. Nếu vẫn chưa có form hợp lệ, transaction chuyển `CANCELLED`, penalty bằng 0, xử lý voucher/assignment/history/outbox.
6. Sau commit kết thúc các task còn lại và thông báo USER, assigned staff nếu có.

Draft hoặc request lỗi validation không dừng timer. Form hợp lệ đã lưu phải dừng timer ngay, không chờ staff review; staff yêu cầu sửa không tự khởi động lại PT24H.

### Nhánh payment timeout/fail

1. Timer PT1H đến hạn từ `PaymentAttempt.openedAt` hoặc backend nhận kết quả payment fail đã xác minh.
2. Service khóa Order, PaymentAttempt và workflow step.
3. Nếu PaymentAttempt đã `PAID` hoặc payment-success đã thắng, không hủy.
4. Nếu attempt vẫn chờ/hết hạn/thất bại đúng reference và Order còn `ORDER_ACCEPTED`, transaction hủy với `PAYMENT_TIMEOUT` hoặc `PAYMENT_FAILED`, penalty bằng 0.
5. Sau commit dừng bước chờ, thông báo owner/staff và invalidate cache.
6. Callback thành công đến muộn không khôi phục Order; ghi ngoại lệ đối soát và không tự tuyên bố refund.

### Ngoại lệ và cạnh tranh

| Mã | Điều kiện | Kết quả |
|---|---|---|
| C01 | Job/event nội bộ không hợp lệ hoặc thiếu reference | Từ chối và ghi lỗi quan sát được |
| C02 | CUSTOM form hợp lệ đã lưu | Không hủy, timer được coi là stale |
| C03 | PaymentAttempt đã PAID | Không hủy, timer/fail event được coi là stale hoặc cần đối soát |
| C04 | Attempt/reference không thuộc Order | Từ chối, không tác động Order khác |
| C05 | Order đã CANCELLED bởi nguồn khác | Trả kết quả idempotent, không lặp side effect |
| C06 | Staff start cạnh tranh với cancel | Khóa/version chỉ cho một transition, không cùng thành công |
| C07 | Webhook success cạnh tranh timer | Khóa PaymentAttempt/Order chỉ cho một kết quả nghiệp vụ |
| C08 | Callback success đến sau CANCELLED | Ghi ngoại lệ đối soát, không tự restore/refund |
| C09 | Workflow cleanup lỗi sau DB commit | Order vẫn CANCELLED, retry/reconcile cleanup |

## 8. Hợp đồng dữ liệu đề xuất

### Customer cancel request

```json
{
  "orderVersion": 4,
  "idempotencyKey": "c57a83db-25bd-41a9-990f-1e5903a781b5",
  "reason": "Tôi không còn nhu cầu đặt sản phẩm"
}
```

Source, userId, guestSessionId, penalty, finalPrice và cancellation timestamp không do client quyết định.

### Manager reject request

```json
{
  "decision": "REJECT",
  "reason": "Xưởng không đủ khả năng thực hiện yêu cầu này",
  "orderVersion": 2,
  "taskVersion": 1,
  "idempotencyKey": "de546095-abf0-4eb9-8935-a84ca35aa420"
}
```

### Response tối thiểu

```json
{
  "orderId": 1001,
  "status": "CANCELLED",
  "cancellationSource": "CUSTOMER_USER",
  "reason": "Tôi không còn nhu cầu đặt sản phẩm",
  "reputationPenalty": 2,
  "voucherRestored": true,
  "assignmentReleased": true,
  "cancelledAt": "2026-10-06T16:00:00+07:00"
}
```

Response không tuyên bố `refunded=true` nếu hệ thống chưa có giao dịch hoàn tiền tương ứng.

## 9. Trạng thái, tác động và sự kiện

| Trường hợp | Trạng thái đầu | Trạng thái cuối | Penalty | Voucher | Assignment |
|---|---|---|---:|---|---|
| USER tự hủy có finalPrice | Trước ORDER_ACCEPTED | CANCELLED | Theo bảng giá | Phục hồi một lần | Release nếu active |
| USER CUSTOM chưa có finalPrice | Trước ORDER_ACCEPTED | CANCELLED | 0 | Phục hồi nếu đã dùng | Release nếu active |
| Guest tự hủy | Trước ORDER_ACCEPTED | CANCELLED | Không có | Hoàn global quota, giữ anti-spam usage | Release nếu active |
| Manager reject | PENDING_APPROVAL | CANCELLED | 0 | Phục hồi | Thường chưa có, nếu có thì release |
| CUSTOM timeout | Wait xác nhận trước accepted | CANCELLED | 0 | Phục hồi | Release nếu active |
| Payment timeout/fail | ORDER_ACCEPTED đang chờ payment | CANCELLED | 0 | Phục hồi | Release active staff |

Sau commit phát `ORDER_CANCELLED` kèm `eventId`, `orderId`, source, reason code/reference, actor reference, aggregate version và occurredAt. Không đưa guest token hoặc secret vào payload. Routing cụ thể tạo `ORDER_CANCELLATION_EMAIL_REQUESTED`, notification và workflow cleanup, không mặc định chỉ tên event là đã gửi email.

## 10. Điểm cần chốt trước khi triển khai production

- Danh mục reason người dùng/manager được chọn và độ dài/nội dung free-text.
- TTL, revoke/cấp lại Guest cancel token và ngưỡng rate limit cụ thể.
- Kênh/đối tượng MANAGER nhận thông tin ở từng nguồn hủy hệ thống.
- Policy tài chính cho trường hợp webhook thành công đến sau khi payment timeout đã hủy Order. WF09 chỉ ghi ngoại lệ, không tự refund hoặc khôi phục.

Các điểm này không làm thay đổi giới hạn tự hủy trước `ORDER_ACCEPTED`, mức penalty đã chốt hoặc yêu cầu idempotency.

## 11. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | USER owner hủy ở trạng thái cho phép | CANCELLED và audit đúng actor/source |
| TC02 | USER khác hủy | Bị từ chối |
| TC03 | Guest chỉ có orderId, không token | Bị từ chối |
| TC04 | Guest mở GET link email | Chỉ hiện trang xác nhận, chưa hủy |
| TC05 | Guest token hợp lệ xác nhận mutation | CANCELLED, không reputation |
| TC06 | Khách hủy từ ORDER_ACCEPTED trở đi | Bị từ chối dù link còn hạn |
| TC07 | USER có finalPrice dưới 1 triệu | Trừ 1 điểm một lần |
| TC08 | USER có finalPrice 1–5 triệu | Trừ 2 điểm một lần |
| TC09 | USER có finalPrice trên 5–10 triệu | Trừ 3 điểm một lần |
| TC10 | USER có finalPrice trên 10 triệu | Trừ 5 điểm một lần |
| TC11 | USER không đủ reputation | Không hủy và không ghi điểm âm |
| TC12 | CUSTOM chưa có finalPrice chính thức | Hủy được, penalty 0 và không gán giá 0 |
| TC13 | UserVoucher đã dùng | Trả voucher một lần, không hoàn pointCost |
| TC14 | Guest voucher | Hoàn global quota một lần, vẫn giữ anti-spam usage |
| TC15 | Manager reject PENDING_APPROVAL có reason | CANCELLED, penalty 0 |
| TC16 | Manager cố hủy tùy ý sau review | Bị từ chối |
| TC17 | CUSTOM timer và form hợp lệ cạnh tranh | Chỉ form hoặc cancel thắng, không cả hai |
| TC18 | Form hợp lệ đã lưu trước hạn | Timer không hủy dù staff chưa review |
| TC19 | Payment webhook success và timeout cạnh tranh | Chỉ PAID hoặc CANCELLED thắng |
| TC20 | Callback success đến sau CANCELLED | Không restore Order/refund tự động, ghi ngoại lệ |
| TC21 | Staff start cạnh tranh cancel | Chỉ một transition thắng |
| TC22 | Cancel có active assignment | Đóng đúng assignment và release một lần |
| TC23 | Cancel không có/đã đóng assignment | Không release sai hoặc lần hai |
| TC24 | Request/event lặp | Không trừ điểm, hoàn voucher/quota, release hoặc gửi nghiệp vụ lần hai |
| TC25 | DB commit nhưng workflow/email lỗi | CANCELLED được giữ và side effect retry |
| TC26 | Bất kỳ nguồn hủy | Không hoàn stock và không xóa dữ liệu lịch sử |
| TC27 | Email/notification hủy | Gửi theo loại chủ đơn/recipient, không tính vào bốn email trạng thái bình thường |
| TC28 | Payment/COD | Không tuyên bố hoàn tiền hoặc tạo payment COD |
| TC29 | Race/version cũ | Một kết quả nghiệp vụ, request thua nhận conflict/trạng thái hiện hành |
| TC30 | Cache sau hủy | Dữ liệu Order/voucher/reputation liên quan được invalidate |

## 12. Sơ đồ

- [Use Case tổng quát](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity Diagram theo từng UC](diagram.md).
- [Sequence Diagram theo từng UC](sequence.md).
