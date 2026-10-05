# WF04 — Thanh toán và bắt đầu sản xuất (`payment_and_start`)

Ngày lập: 05/10/2026. Đặc tả hệ thống mục tiêu, không phải xác nhận hành vi runtime hiện tại.

Nguồn: [target.txt](../../target.txt), mục C5, D, E/WF04, E1, F10–F12, F16, F18, H–J. Đầu vào đến từ [WF03](../WF03-agree-order/README.md).

## 1. Phạm vi và điểm bàn giao

WF04 bắt đầu khi Order đã `ORDER_ACCEPTED`, có staff phụ trách và đã có giá/thông tin chính thức:

- **COD:** không tạo PaymentAttempt và không chờ webhook. Camunda đi thẳng tới user task chờ staff bấm bắt đầu. Tiền COD do shipper bên thứ ba thu, nằm ngoài hệ thống.
- **ONLINE:** chỉ USER có tài khoản sử dụng. Sau khi PaymentAttempt chính thức được mở, Camunda chờ một trong các kết quả payment thành công, payment thất bại hoặc timer `PT1H`.

Webhook thành công chỉ ghi `paymentStatus = PAID`, `paidAt` và thông báo staff. Order vẫn `ORDER_ACCEPTED`. Chỉ staff đang phụ trách bấm bắt đầu hợp lệ mới chuyển Order sang `ORDER_CREATING`.

WF04 kết thúc theo một trong hai kết quả:

- `ORDER_CREATING`, có `productionStartedAt` và deadline từng OrderItem, sau đó bàn giao WF05.
- `CANCELLED` do payment thất bại/hết hạn qua nghiệp vụ hủy hệ thống dùng chung.

WF04 không phân công staff, không chốt lại giá/spec, không xử lý ChangeRequest và không thực hiện checkpoint.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân chính | Kết quả |
|---|---|---|---|
| UC04.1 | Mở và thực hiện thanh toán online | USER sở hữu Order | PaymentAttempt PENDING, URL và hạn thanh toán |
| UC04.2 | Tiếp nhận kết quả thanh toán | Cổng thanh toán | PAID và chờ staff, hoặc FAILED/CANCELLED |
| UC04.3 | Staff bắt đầu sản xuất | STAFF phụ trách | ORDER_CREATING và productionStartedAt |

Camunda, backend, database, timer và notification consumer là thành phần nội bộ. Guest không thực hiện UC04.1 vì Guest chỉ COD. USER/COD cũng bỏ qua UC04.1–UC04.2. USER và Guest nhận thông báo `ORDER_CREATING`, nhưng không phải tác nhân thực hiện UC04.3.

## 3. Quy tắc chung của WF04

### 3.1 Thanh toán và Order là hai trục trạng thái

- `paymentMethod`: COD hoặc ONLINE.
- `paymentStatus` đề xuất: `NOT_DUE`, `PENDING`, `PAID`, `FAILED`, `EXPIRED`.
- `ORDER_ACCEPTED` không đồng nghĩa đã thanh toán hoặc đã bắt đầu sản xuất.
- ONLINE phải `PAID` mới được staff bắt đầu. COD không tạo bản ghi online giả và không tự đánh dấu `PAID`.
- Số tiền thanh toán lấy từ `finalPrice` chính thức trên server. Không nhận amount do UI quyết định.
- ChangeRequest trong phạm vi dự án không thay giá và không tạo payment bổ sung.

### 3.2 Camunda

- WF04 tiếp tục process instance của Order đã chạy từ WF02–WF03.
- Cổng rẽ nhánh đọc `paymentMethod` đã lưu. COD đi thẳng đến user task staff start.
- ONLINE chỉ vào event-based gateway sau khi PaymentAttempt đã mở, với các nhánh message thành công, message thất bại và timer tại `paymentDueAt` tương đương `PT1H` từ `openedAt`.
- Webhook phải được backend xác minh và commit trước khi correlate message vào Camunda.
- Success/failed callback và timer cạnh tranh phải có một kết quả nghiệp vụ duy nhất bằng khóa/version/trạng thái database.
- Lỗi correlate sau khi DB đã ghi PAID/FAILED phải được retry/reconciliation, không chỉ ghi log và không xử lý callback lại như giao dịch mới.

### 3.3 Thông báo

- Payment thành công gửi notification nội bộ cho `assignedStaffId` lấy từ database, không gửi email trạng thái mới cho chủ đơn vì Order vẫn `ORDER_ACCEPTED`.
- Payment thất bại/hết hạn gửi email hủy ngoại lệ và notification theo F16.
- Khi staff bắt đầu, phát mốc `ORDER_CREATING`: USER nhận email + notification, Guest chỉ email.
- Đọc notification không tự hoàn tất user task hoặc bắt đầu sản xuất.

## 4. UC04.1 — Mở và thực hiện thanh toán online

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Tạo một yêu cầu thanh toán đúng số tiền đã chốt và đưa USER tới cổng thanh toán |
| Kích hoạt | USER chọn “Thanh toán” trên Order `ORDER_ACCEPTED` có paymentMethod ONLINE |
| Tiền điều kiện | USER là owner; Order có active assignment, thông tin chính thức và finalPrice; chưa PAID/CANCELLED |
| Đầu vào | orderId, idempotency key; identity từ JWT |
| Thành công | PaymentAttempt PENDING có provider reference, expectedAmount, openedAt, dueAt và URL |
| Bảo đảm thất bại | Không tạo nhiều cửa sổ thanh toán hoặc kéo dài hạn do retry |

### Luồng chính

| Bước | USER | Hệ thống |
|---|---|---|
| 1 | Mở Order đã được chấp nhận | Hiển thị finalPrice và trạng thái thanh toán riêng với trạng thái Order |
| 2 | Bấm thanh toán | Xác thực owner từ JWT và nhận idempotency key |
| 3 | — | Khóa/kiểm tra Order `ORDER_ACCEPTED`, paymentMethod ONLINE, finalPrice chính thức và assignment |
| 4 | — | Kiểm tra chưa PAID, chưa có active PaymentAttempt khác và yêu cầu chưa được xử lý |
| 5 | — | Tạo yêu cầu với provider bằng order reference duy nhất và expectedAmount từ server |
| 6 | — | Lưu PaymentAttempt `PENDING`, `openedAt`, `dueAt = openedAt + 1 giờ`, providerTransactionId và công việc bàn giao Camunda |
| 7 | Nhận URL/QR | Trả attemptId, URL/thông tin thanh toán, expectedAmount và dueAt |
| 8 | Chuyển sang provider | Camunda vào event-based gateway chờ callback hoặc timer tại dueAt |

Không ghi secret/signature vào log. Nếu provider API và database không thể nằm trong một transaction, triển khai state/idempotency để retry không tạo hai giao dịch provider và có reconciliation cho attempt tạo dở dang.

### Luồng thay thế và ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| A01 | Guest hoặc USER không sở hữu Order | 403, không lộ URL/payment detail |
| A02 | paymentMethod COD | Từ chối tạo online payment, workflow đã ở bước chờ staff start |
| A03 | Order chưa ORDER_ACCEPTED, đã CANCELLED hoặc đã ORDER_CREATING | 409, không tạo attempt |
| A04 | finalPrice chưa chính thức/không hợp lệ | Từ chối, không dùng giá 0 mặc định |
| A05 | Đã PAID | Trả trạng thái đã thanh toán, không mở giao dịch mới |
| A06 | Cùng idempotency key và cùng yêu cầu | Trả PaymentAttempt/URL hiện có, không đổi dueAt |
| A07 | Cùng key nhưng nội dung khác | 409; lần tạo mới phải có key mới sau khi nghiệp vụ cho phép |
| A08 | Active attempt còn hạn | Trả attempt hiện có theo policy, không tạo timer mới |
| A09 | Provider hoặc lưu DB lỗi | Ghi trạng thái có thể retry/reconcile, không báo thành công giả |

### Điểm cần chốt khi triển khai UI

Yêu cầu hiện tại chỉ quy định timer 1 giờ bắt đầu khi PaymentAttempt chính thức được mở. Chưa có timeout từ `ORDER_ACCEPTED` đến lúc USER lần đầu bấm “Thanh toán”. Không tự lấy timer PT1H áp vào khoảng chờ này. Nếu sản phẩm muốn tự mở PaymentAttempt ngay khi `ORDER_ACCEPTED` hoặc muốn hủy khi USER không bấm thanh toán, cần xác nhận thành một chính sách riêng trước khi code.

## 5. UC04.2 — Tiếp nhận kết quả thanh toán

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Ghi nhận chính xác kết quả provider và đánh thức đúng process đang chờ |
| Kích hoạt | Provider gửi webhook/callback cho PaymentAttempt |
| Tiền điều kiện | Có provider reference/order reference có thể đối chiếu |
| Đầu vào | providerTransactionId, order reference, amount, result code, signature và metadata cần thiết |
| Thành công payment | PaymentAttempt PAID, paidAt; Order vẫn ORDER_ACCEPTED; staff được thông báo |
| Thất bại/hết hạn | Payment FAILED/EXPIRED và Order CANCELLED theo F16 |
| Bảo đảm thất bại | Callback giả, sai tiền, trùng hoặc đến muộn không thay đổi sai trạng thái |

### Luồng thành công

1. Provider gọi webhook.
2. Backend xác minh signature theo raw payload và cấu hình provider.
3. Tra PaymentAttempt bằng provider reference/order reference, không tin orderId hoặc staffId tùy ý trong payload.
4. Kiểm tra amount/currency nếu có, result code, trạng thái attempt, dueAt và Order hiện hành.
5. Khóa/kiểm soát cạnh tranh với timer và callback khác.
6. Trong transaction ghi PaymentAttempt `PAID`, `paidAt`, provider result và event/outbox.
7. Order giữ `ORDER_ACCEPTED`, không ghi `productionStartedAt`.
8. Sau commit correlate payment-success vào Camunda và gửi notification “Đơn đã thanh toán, có thể bắt đầu làm” cho assigned staff đọc từ database.
9. Trả acknowledgement đúng hợp đồng provider.

### Luồng thất bại và timeout

- Callback thất bại hợp lệ: ghi `FAILED`, gọi nghiệp vụ hủy hệ thống một lần, release đúng active assignment, khôi phục voucher theo F16 và kết thúc nhánh payment.
- Timer đến hạn: khóa PaymentAttempt/Order, nếu vẫn `PENDING` thì ghi `EXPIRED` và hủy với lý do `PAYMENT_TIMEOUT`.
- Hủy do payment fail/timeout không trừ reputation vì không phải USER chủ động hủy.
- Nếu PAID đã commit trước, timer không được hủy Order dù correlation đang retry.
- Nếu timeout/cancel đã commit trước, callback thành công đến muộn không khôi phục Order hoặc tự tuyên bố refund. Ghi ngoại lệ đối soát để xử lý theo chính sách ngoài phạm vi hiện tại.

### Webhook không hợp lệ hoặc lặp

| Điều kiện | Kết quả |
|---|---|
| Signature không hợp lệ | Từ chối, không ghi PAID/FAILED |
| Sai amount/reference/provider transaction | Từ chối và ghi audit an toàn, không lộ secret |
| Callback thành công hợp lệ gửi lặp sau PAID | Trả acknowledgement idempotent, không phát notification/correlation lần hai |
| Callback failure gửi sau PAID | Không hạ trạng thái từ PAID xuống FAILED |
| Callback success sau CANCELLED/EXPIRED | Không hồi sinh Order, ghi reconciliation exception |
| DB commit nhưng event/correlation lỗi | Retry từ outbox/job, không yêu cầu provider tạo giao dịch khác |

## 6. UC04.3 — Staff bắt đầu sản xuất

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Ghi nhận thời điểm staff thực sự bắt đầu và chuyển sang sản xuất |
| Kích hoạt | Staff phụ trách bấm “Bắt đầu làm” từ task/danh sách công việc |
| Tiền điều kiện | Active assignment đúng staff; Order `ORDER_ACCEPTED`; COD được phép hoặc ONLINE đã PAID; dữ liệu chính thức hợp lệ |
| Đầu vào | orderId, version/idempotency key, lateStartReason tùy chọn khi có lý do bắt đầu muộn; staff identity từ JWT |
| Thành công | `ORDER_CREATING`, productionStartedAt và computedCompletionAt từng item được lưu một lần |
| Bảo đảm thất bại | Không ghi timestamp/deadline hoặc hoàn tất Camunda task một phần |

### Luồng chính

| Bước | STAFF | Hệ thống |
|---|---|---|
| 1 | Mở task chờ bắt đầu | Hiển thị Order, payment status và dữ liệu sản xuất cần thiết |
| 2 | Bấm “Bắt đầu làm”, nhập lý do muộn nếu có | Lấy staffId từ JWT, không tin staffId client gửi |
| 3 | — | Khóa/kiểm tra Order, active assignment, agreement/snapshot và payment condition |
| 4 | — | Ghi `productionStartedAt = now` theo timezone ứng dụng và chuyển `ORDER_CREATING` |
| 5 | — | Với từng OrderItem, dùng `productionDurationDays` đã chốt để tính `computedCompletionAt` từ ngày kế tiếp, bỏ Chủ nhật và ngày nghỉ cấu hình |
| 6 | — | Cùng transaction ghi history, lateStartReason và công việc hoàn tất Camunda task/phát event |
| 7 | Nhận kết quả | Trả productionStartedAt, deadline từng item và trạng thái `ORDER_CREATING` |
| 8 | — | Sau commit phát `PRODUCTION_STARTED`, bàn giao WF05, gửi email mốc ORDER_CREATING; USER thêm notification |

Thứ Bảy được tính nếu không nằm trong ngày nghỉ cấu hình. Không tính ngày bắt đầu là ngày sản xuất thứ nhất. Thời gian vận chuyển giả định 2 ngày được hiển thị tách riêng, không cộng vào `productionDurationDays`.

### Ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| B01 | Staff khác hoặc assignment đã release | 403/409, không bắt đầu |
| B02 | Order không còn ORDER_ACCEPTED | Trả trạng thái hiện tại hoặc conflict |
| B03 | ONLINE chưa PAID | Từ chối, user task vẫn chờ |
| B04 | COD không hợp lệ theo phương thức đã chốt | Từ chối, không tự đổi payment method |
| B05 | Thiếu agreement/snapshot/duration chính thức | Từ chối và yêu cầu sửa dữ liệu, không dùng giá/thời lượng giả |
| B06 | Request lặp sau khi đã bắt đầu | Trả productionStartedAt/deadline cũ, không ghi đè hoặc phát event lần hai |
| B07 | Start cạnh tranh với cancel/payment failure | Khóa/version cho phép đúng một chuyển trạng thái có hiệu lực |
| B08 | DB commit nhưng Camunda/email lỗi | Giữ ORDER_CREATING và timestamp, retry bàn giao/thông báo |

Staff không được bắt đầu chỉ bằng việc đọc notification. Webhook không được gọi thay UC này. Không release assignment khi bắt đầu, staff tiếp tục bận đến khi toàn đơn qua KCS checkpoint 2 hoặc bị hủy.

## 7. Trạng thái và các mốc thời gian

| Mốc | Order | Payment | Workflow |
|---|---|---|---|
| Vào WF04 | ORDER_ACCEPTED | COD: NOT_DUE, ONLINE: chưa mở/PENDING theo attempt | Rẽ paymentMethod |
| Mở online payment | ORDER_ACCEPTED | PENDING, openedAt, dueAt | Chờ success/failure/PT1H |
| Webhook thành công | ORDER_ACCEPTED | PAID, paidAt | Tạo/đi tới task staff start |
| COD | ORDER_ACCEPTED | Không có online PaymentAttempt | Đi thẳng task staff start |
| Staff bắt đầu | ORDER_CREATING | COD giữ ngoài hệ thống, ONLINE PAID | Bàn giao WF05 |
| Payment fail/timeout | CANCELLED | FAILED/EXPIRED | Kết thúc nhánh hủy |

`openedAt`, `dueAt`, `paidAt` và `productionStartedAt` là các thời điểm khác nhau. Không dùng một trường thay cho nhau.

## 8. Hợp đồng dữ liệu và triển khai

| Thao tác | Đầu vào chính | Đầu ra tối thiểu |
|---|---|---|
| Open payment | orderId, idempotency key, USER JWT | attemptId, provider reference, URL/QR, expectedAmount, openedAt, dueAt, status |
| Webhook | raw payload, signature, provider headers | acknowledgement và trạng thái xử lý idempotent |
| Payment status | orderId/attemptId, owner JWT | status, expectedAmount, openedAt, dueAt, paidAt phù hợp quyền |
| Start production | orderId, version/idempotency key, lateStartReason, STAFF JWT | ORDER_CREATING, productionStartedAt, deadline từng item |

PaymentAttempt cần unique/idempotency cho provider transaction và quan hệ tới Order. Event/outbox cần khóa chống trùng theo `orderId + eventType + milestone` hoặc tương đương. Process variable chỉ giữ khóa tham chiếu, không lưu secret, raw payment credential hoặc toàn bộ Order.

Tên endpoint/service/topic là thiết kế đề xuất. API webhook thường public về network nhưng phải xác thực bằng cơ chế provider, không coi “không cần JWT” là không cần xác thực.

## 9. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | Guest hoặc USER/COD vào WF04 | Không tạo online PaymentAttempt, chờ staff start |
| TC02 | USER/ONLINE mở payment hợp lệ | Một PENDING attempt, amount server, dueAt đúng openedAt + 1 giờ |
| TC03 | Retry mở payment | Trả attempt hiện có, không kéo dài dueAt hoặc tạo timer mới |
| TC04 | Order chưa được chốt mở payment | Bị từ chối, không có URL hợp lệ |
| TC05 | Webhook sai signature | Không đổi Payment/Order |
| TC06 | Webhook đúng signature nhưng sai amount/reference | Không PAID, có audit an toàn |
| TC07 | Webhook success hợp lệ | PAID, Order vẫn ORDER_ACCEPTED, staff được thông báo |
| TC08 | Webhook success gửi lặp | Một lần PAID, một notification và một correlation |
| TC09 | Timer thắng khi còn PENDING | EXPIRED, CANCELLED, release assignment, không trừ reputation |
| TC10 | PAID commit trước nhưng correlation chậm | Timer không hủy, job retry workflow |
| TC11 | Callback success đến sau timeout/cancel | Không hồi sinh Order, ghi ngoại lệ đối soát |
| TC12 | Staff đọc notification payment | Chưa ORDER_CREATING |
| TC13 | Staff khác bấm bắt đầu | Bị từ chối |
| TC14 | Assigned staff bắt đầu ONLINE chưa PAID | Bị từ chối, task vẫn chờ |
| TC15 | Assigned staff bắt đầu COD hợp lệ | ORDER_CREATING không cần webhook COD |
| TC16 | Assigned staff bắt đầu ONLINE đã PAID | ORDER_CREATING và productionStartedAt một lần |
| TC17 | Staff gửi start lặp | Không đổi timestamp/deadline, không gửi email lần hai |
| TC18 | Start và cancel chạy đồng thời | Chỉ một chuyển trạng thái thắng |
| TC19 | Bắt đầu thành công | Tính deadline riêng từng OrderItem, bỏ ngày không tính theo cấu hình |
| TC20 | Email/event lỗi sau start | Order vẫn ORDER_CREATING, có retry, không yêu cầu bấm lại |
| TC21 | Order ORDER_CREATING | USER nhận email + notification, Guest chỉ email, staff vẫn bận |
| TC22 | WF04 bất kỳ | Không tạo checkpoint, không READY_TO_SHIP và không release staff |

## 10. Sơ đồ

- [Use Case tổng quát](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity Diagram theo từng UC](diagram.md).
- [Sequence Diagram theo từng UC](sequence.md).
