# WF06 — Yêu cầu thay đổi chi tiết (`change_request`)

Ngày lập: 06/10/2026. Đặc tả hệ thống mục tiêu, không phải xác nhận hành vi runtime hiện tại.

Nguồn: [target.txt](../../target.txt), mục B–C, E/WF06, E1, F14, I–J. Liên quan đến thỏa thuận tại [WF03](../WF03-agree-order/README.md) và sản xuất tại [WF05](../WF05-production-checkpoint/README.md).

## 1. Phạm vi và điểm bàn giao

WF06 dùng khi USER muốn sửa một chi tiết sau khi thỏa thuận đã được chốt. Yêu cầu chỉ có thể được áp dụng nếu:

- Chi tiết bị tác động **chưa được triển khai** tại thời điểm staff quyết định.
- Staff phụ trách xác nhận thay đổi vẫn thực hiện được trong giá và deadline đã chốt.
- Thay đổi không sửa `unitPrice`, `subtotal`, `discount`, `finalPrice`, voucher, payment hoặc deadline.

Luồng bắt đầu khi USER gửi ChangeRequest có cấu trúc và kết thúc với:

- `APPLIED`: tạo phiên bản spec mới cho phần chưa làm, giữ lịch sử phiên bản cũ.
- `REJECTED`: lưu lý do, sản phẩm tiếp tục theo spec đang áp dụng.

WF06 không đổi trạng thái Order, không reset `productionStartedAt`, không dừng toàn bộ quy trình sản xuất và không release staff. Rework để sản phẩm đạt spec đã chốt thuộc WF05, không phải ChangeRequest.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân chính | Kết quả |
|---|---|---|---|
| UC06.1 | User tạo yêu cầu thay đổi | USER sở hữu Order | ChangeRequest chờ staff quyết định |
| UC06.2 | Staff quyết định yêu cầu thay đổi | STAFF phụ trách | APPLIED với spec version mới hoặc REJECTED có lý do |

Guest không có ChangeRequest. MANAGER không tham gia, không duyệt và không quyết định thay staff. Camunda/backend/database/notification consumer là thành phần nội bộ.

## 3. Quy tắc chung

### 3.1 Order và thời điểm được yêu cầu

- Chỉ USER sở hữu Order đã có thỏa thuận/spec chính thức mới được tạo ChangeRequest.
- Có thể tạo sau khi `ORDER_ACCEPTED`, bao gồm lúc Order đang `ORDER_CREATING`, miễn phần bị tác động chưa được triển khai.
- Không áp dụng khi Order `READY_TO_SHIP`, `SHIPPING`, `DELIVERED` hoặc `CANCELLED`.
- Khi Order đang `ORDER_CREATING`, tiến độ của chi tiết liên quan phải được kiểm tra lại tại lúc submit và lúc staff quyết định.
- Target chưa giới hạn ChangeRequest chỉ cho CUSTOM hay cho cả USER CATALOG. Thiết kế này cho phép backend đánh giá theo `orderType` và policy trường được phép, nhưng CATALOG tuyệt đối không được sửa giá snapshot hoặc biến thành một đơn custom ngoài quy trình.

### 3.2 Dữ liệu bất biến

ChangeRequest không được thay đổi trực tiếp hoặc gián tiếp:

- `unitPrice`, `subtotal`, `discount`, `finalPrice`.
- Voucher đã áp dụng, `paymentMethod`, PaymentAttempt hoặc payment status.
- `productionStartedAt`, `productionDurationDays`, `computedCompletionAt` hoặc deadline đã cam kết.
- Order status, assignedStaffId hoặc active assignment.
- Checkpoint/ảnh/decision đã có trong lịch sử.

Nếu thay đổi mong muốn cần thêm tiền hoặc đổi cam kết thời gian, staff phải từ chối trong WF06. Dự án hiện chưa có luồng báo giá bổ sung hoặc đổi deadline.

### 3.3 Version và audit

- Request lưu `baseAgreementVersion` hoặc base spec version mà USER đã xem.
- Request có nội dung cấu trúc chỉ rõ `orderItemId`, trường/chi tiết cũ, chi tiết đề nghị, lý do và attachment tham khảo nếu được hỗ trợ.
- Khi APPLIED, tạo một applied spec version mới liên kết ChangeRequest và base version, không ghi đè lịch sử thỏa thuận cũ.
- Mọi quyết định lưu staff actor, thời gian, decision và reason/audit.
- Nếu current spec đã đổi từ base do ChangeRequest khác, backend trả conflict để staff đánh giá trên phiên bản mới, không tự merge.

### 3.4 Vai trò Camunda

Camunda không bắt buộc cho CRUD ChangeRequest. Nếu tích hợp vào process vòng đời Order, dùng **non-interrupting event subprocess** hoặc cơ chế task phụ tương đương:

- Sau khi ChangeRequest commit, message `CHANGE_REQUEST_SUBMITTED` tạo user task cho assigned staff.
- Main flow payment/start/production không bị dừng toàn cục.
- Accept/reject hoàn tất task phụ và không đổi vị trí chính của Order trong WF04/WF05.
- Message/task retry dùng changeRequestId và eventId, không tạo bản ghi hoặc task quyết định thứ hai.

Không tạo một process vòng đời Order thứ hai cho ChangeRequest. Không dùng ChangeRequest để quay ngược trạng thái sản xuất.

## 4. UC06.1 — User tạo yêu cầu thay đổi

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Gửi đề nghị có cấu trúc cho chi tiết chưa triển khai |
| Kích hoạt | USER chọn “Yêu cầu thay đổi” từ Order của mình |
| Tiền điều kiện | USER là owner; Order có spec chính thức và staff phụ trách; trạng thái còn đủ điều kiện |
| Đầu vào | orderId, orderItemId, baseAgreement/spec version, chi tiết đề nghị, reason, attachment tham khảo tùy chọn, idempotency key |
| Thành công | ChangeRequest trạng thái đề xuất `PENDING_REVIEW`, notification/task cho assigned staff |
| Bảo đảm thất bại | Không thay spec, giá, deadline, payment hoặc Order status |

### Luồng chính

| Bước | USER | Hệ thống |
|---|---|---|
| 1 | Mở thông tin Order | Kiểm tra owner và trả spec version hiện hành cùng phạm vi có thể đề nghị |
| 2 | Chọn OrderItem/chi tiết, nhập nội dung mới và lý do | UI không hiển thị trường giá, voucher, deadline hoặc payment như trường được chỉnh |
| 3 | Bấm gửi | Backend lấy userId từ JWT, nhận base version và idempotency key |
| 4 | — | Khóa/kiểm tra Order, owner, assigned staff, trạng thái, OrderItem và base version |
| 5 | — | Kiểm tra field thuộc phạm vi được phép và chưa được triển khai theo dữ liệu tiến độ hiện có |
| 6 | — | Trong transaction lưu ChangeRequest, nội dung before/proposed, base version, actor/time và công việc thông báo |
| 7 | Nhận kết quả | Trả changeRequestId, trạng thái chờ, createdAt và spec version tham chiếu |
| 8 | — | Sau commit thông báo assigned staff và tạo task phụ nếu dùng Camunda |

Submit ChangeRequest không tự sửa Order/spec. USER không được gửi `approved=true`, staffId hoặc các trường tài chính để tự áp dụng thay đổi.

### Ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| A01 | Sai owner, Guest hoặc token không hợp lệ | 401/403, không lộ spec nội bộ |
| A02 | Order chưa chốt spec hoặc đã qua trạng thái cho phép | 409, không tạo request |
| A03 | OrderItem không thuộc Order | Từ chối |
| A04 | Base version cũ | 409, trả metadata version hiện hành theo quyền |
| A05 | Chi tiết đã triển khai | Từ chối, hướng dẫn không dùng ChangeRequest để yêu cầu làm lại |
| A06 | Nội dung sửa giá/voucher/payment/deadline | 400, không âm thầm bỏ các trường cấm rồi lưu phần còn lại |
| A07 | Cùng idempotency key và cùng nội dung | Trả ChangeRequest đã tạo, không tạo task/notification mới |
| A08 | Cùng key nhưng nội dung khác | 409 |
| A09 | DB commit nhưng task/notification lỗi | Giữ ChangeRequest, retry bàn giao, không yêu cầu USER gửi lại |

## 5. UC06.2 — Staff quyết định yêu cầu thay đổi

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Chấp nhận thay đổi khả thi hoặc từ chối có lý do |
| Kích hoạt | Assigned staff mở ChangeRequest đang chờ |
| Tiền điều kiện | Staff giữ active assignment của Order; request còn chờ; Order/phần liên quan còn đủ điều kiện |
| Đầu vào | changeRequestId, request version, decision ACCEPT/REJECT, reason khi reject, staff identity từ JWT |
| Thành công ACCEPT | ChangeRequest APPLIED và một spec version mới cho phần chưa triển khai |
| Thành công REJECT | ChangeRequest REJECTED có reason, spec hiện hành không đổi |
| Bảo đảm thất bại | Không áp dụng một phần, không đổi giá/deadline/payment hoặc quyết định hai lần |

### Luồng ACCEPT

| Bước | STAFF | Hệ thống |
|---|---|---|
| 1 | Mở ChangeRequest | Kiểm tra active assignment, trả nội dung đề nghị, base/current spec và tiến độ liên quan |
| 2 | Đánh giá khả năng thực hiện trong giá/deadline đã chốt | Hệ thống không cung cấp thao tác tăng giá hoặc đổi deadline trong quyết định |
| 3 | Chọn chấp nhận | Khóa/kiểm tra request, Order, current spec version, progress và assignment |
| 4 | — | Xác minh lần cuối chi tiết vẫn chưa triển khai và nội dung không tác động trường cấm |
| 5 | — | Trong transaction tạo applied spec version, liên kết request, ghi decision/actor/time và chuyển request APPLIED |
| 6 | — | Giữ nguyên Order status, productionStartedAt, deadline, giá, voucher, payment và assignment |
| 7 | Nhận kết quả | Trả APPLIED, appliedVersion và spec hiện hành |
| 8 | — | Sau commit hoàn tất task phụ và gửi notification kết quả cho USER |

### Luồng REJECT

1. Staff chọn từ chối và nhập reason.
2. Backend kiểm tra assigned staff, request version và trạng thái chờ.
3. Trong transaction ghi REJECTED, reason, actor/time và audit, không tạo spec version mới.
4. Order tiếp tục theo spec đang áp dụng, trạng thái và deadline không đổi.
5. Sau commit hoàn tất task phụ và thông báo USER.

### Ngoại lệ và cạnh tranh

| Mã | Điều kiện | Kết quả |
|---|---|---|
| B01 | Staff khác hoặc assignment đã release | 403/409, không quyết định |
| B02 | Request đã APPLIED/REJECTED | Trả kết quả idempotent hoặc conflict, không ghi đè |
| B03 | REJECT thiếu reason | 400, request vẫn chờ |
| B04 | Base/current spec thay đổi | 409, không tự merge thay đổi |
| B05 | Chi tiết đã được triển khai trước lúc quyết định | Không ACCEPT, staff phải từ chối với lý do phù hợp |
| B06 | Thay đổi vượt giá/deadline đã chốt | Không ACCEPT trong WF06 |
| B07 | Hai quyết định đồng thời | Khóa/version cho phép một quyết định có hiệu lực |
| B08 | Commit thành công nhưng workflow/notification lỗi | Giữ quyết định, retry bàn giao, không quyết định lại |

## 6. Ảnh hưởng tới tiến trình chính

| Kết quả WF06 | Order status | Giá/payment | Deadline | WF05/checkpoint |
|---|---|---|---|---|
| PENDING_REVIEW | Không đổi | Không đổi | Không đổi | Main flow không dừng toàn cục |
| REJECTED | Không đổi | Không đổi | Không đổi | Tiếp tục spec hiện hành |
| APPLIED | Không đổi | Không đổi | Không đổi | Các bước chưa làm dùng applied spec version mới |

Checkpoint/rework trong WF05 nhằm làm đúng spec hiện hành. Không tạo ChangeRequest ngược để che một sản phẩm làm sai. Khi ChangeRequest APPLIED, checkpoint tạo sau đó phải liên kết/hiển thị spec version đang áp dụng để manager KCS đúng phiên bản.

## 7. Thông báo và lịch sử

- Khi tạo: notification cho assigned staff, không gửi email trạng thái Order cho chủ đơn.
- Khi quyết định: notification kết quả cho USER, gồm decision, reason và appliedVersion nếu có.
- WF06 không nằm trong bốn email trạng thái `PENDING_APPROVAL`, `ORDER_ACCEPTED`, `ORDER_CREATING`, `READY_TO_SHIP`.
- Retry notification chống trùng theo changeRequestId + eventType + decision/version.
- Lịch sử phải đọc được request gốc, base spec, proposed change, decision, actor, timestamp và applied spec nếu có.

## 8. Điểm chưa có chính sách nghiệp vụ

Target hiện chưa quy định:

- SLA/timer bắt buộc staff phải trả lời ChangeRequest.
- Số ChangeRequest đồng thời tối đa trên một Order hoặc cùng một chi tiết.
- Tự động xử lý request còn PENDING khi Order đi tới `READY_TO_SHIP`.
- Danh sách field cụ thể được phép thay đổi theo từng loại catalog/custom.

Do đó không tự thêm timer, phí, auto-accept hoặc auto-reject. Backend dùng version và kiểm tra tiến độ để ngăn áp dụng request không còn hợp lệ. Các chính sách trên cần được chốt trước khi triển khai production đầy đủ.

## 9. Hợp đồng dữ liệu và triển khai

| Thao tác | Đầu vào chính | Đầu ra tối thiểu |
|---|---|---|
| Create | orderId, orderItemId, baseVersion, structured changes, reason, attachments, idempotency key, USER JWT | changeRequestId, PENDING_REVIEW, createdAt |
| Detail/list | orderId/changeRequestId, JWT | base/proposed/applied data và history theo quyền |
| Decide | changeRequestId, request version, ACCEPT/REJECT, reason, STAFF JWT | APPLIED + appliedVersion hoặc REJECTED + reason |

`ChangeRequest` tối thiểu lưu orderId, orderItemId, ownerId, assignedStaffId snapshot nếu cần audit, baseAgreement/spec version, proposed details, state, decision actor/time/reason và appliedVersion. Không có newPrice, feeDelta hoặc payment reference.

Tên trạng thái `PENDING_REVIEW`, endpoint, service, event và cấu trúc diff là thiết kế đề xuất. Database quyết định quyền, version và progress, cache không phải nguồn duy nhất.

## 10. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | USER owner gửi thay đổi chi tiết chưa làm | Một ChangeRequest chờ staff, Order/spec chưa đổi |
| TC02 | Guest hoặc USER khác tạo request | Bị từ chối |
| TC03 | Request cố đổi unitPrice/finalPrice/voucher | Bị từ chối, không lưu một phần |
| TC04 | Request cố đổi deadline/duration | Bị từ chối trong WF06 |
| TC05 | Request cho chi tiết đã triển khai | Bị từ chối |
| TC06 | Submit lặp cùng idempotency key | Một ChangeRequest và một notification staff |
| TC07 | Staff khác quyết định | Bị từ chối |
| TC08 | Assigned staff reject có reason | REJECTED, spec/giá/deadline/trạng thái không đổi |
| TC09 | Reject thiếu reason | Validation lỗi, request vẫn chờ |
| TC10 | Assigned staff accept hợp lệ | APPLIED, spec version mới, history cũ được giữ |
| TC11 | Accept khi base version đã cũ | Conflict, không tự merge |
| TC12 | Accept khi chi tiết vừa được triển khai | Không áp dụng |
| TC13 | Hai quyết định đồng thời | Một decision có hiệu lực |
| TC14 | ChangeRequest APPLIED sau ONLINE đã PAID | Không payment bổ sung, paid amount/finalPrice không đổi |
| TC15 | ChangeRequest trong ORDER_CREATING | Không reset productionStartedAt hoặc computedCompletionAt |
| TC16 | Main production đang chạy | ChangeRequest không dừng toàn bộ Camunda flow |
| TC17 | Notification/task lỗi sau commit | Request/decision vẫn tồn tại, có retry, không gửi lại nghiệp vụ |
| TC18 | WF06 bất kỳ | Không release staff, không READY_TO_SHIP và không tạo checkpoint thay thế |

## 11. Sơ đồ

- [Use Case tổng quát](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity Diagram theo từng UC](diagram.md).
- [Sequence Diagram theo từng UC](sequence.md).
