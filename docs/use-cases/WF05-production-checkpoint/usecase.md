# WF05 — Sản xuất, checkpoint và KCS (`start_made_product`)

Ngày lập: 05/10/2026. Đặc tả hệ thống mục tiêu, không phải xác nhận hành vi runtime hiện tại.

Nguồn: [target.txt](../../target.txt), mục C4–C5, D, E/WF05, E1, F13, F18, H–J. Đầu vào đến từ [WF04](../WF04-payment-and-start/README.md).

## 1. Phạm vi và điểm bàn giao

WF05 bắt đầu khi staff phụ trách đã bấm bắt đầu tại WF04 và Order đã `ORDER_CREATING`, có `productionStartedAt`, thời lượng và deadline riêng từng OrderItem.

Với mỗi **loại sản phẩm/OrderItem**, quy trình gồm:

1. Staff nộp checkpoint 1 `INITIAL_SHAPE` kèm ảnh và ghi chú.
2. Hệ thống gửi thẳng báo cáo tiến trình cho chủ đơn, không tạo bước duyệt.
3. Staff nộp checkpoint 2 `FINAL_PRODUCT`.
4. Manager KCS checkpoint 2, chọn đạt hoặc yêu cầu làm lại có lý do.
5. Nếu chưa đạt, staff tạo attempt FINAL_PRODUCT mới, không ghi đè attempt cũ.

WF05 chỉ kết thúc khi checkpoint 2 của **mọi OrderItem** đều đạt. Hệ thống chuyển Order sang `READY_TO_SHIP`, đóng đúng active assignment và release staff trong cùng transaction nghiệp vụ.

WF05 không chia checkpoint theo từng chiếc. `quantity = 20` của cùng một loại vẫn là một nhánh checkpoint. CUSTOM có một loại custom item nên chỉ có một nhánh. CATALOG có nhiều loại thì mỗi OrderItem có một nhánh độc lập.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân chính | Kết quả |
|---|---|---|---|
| UC05.1 | Staff nộp checkpoint hình ảnh đầu tiên | STAFF phụ trách | INITIAL_SHAPE được lưu và chủ đơn nhận báo cáo tiến trình |
| UC05.2 | Staff nộp sản phẩm hoàn thiện | STAFF phụ trách | FINAL_PRODUCT attempt chờ manager KCS |
| UC05.3 | Manager KCS sản phẩm hoàn thiện | MANAGER | Loại sản phẩm đạt hoặc quay lại rework, toàn đơn có thể READY_TO_SHIP |

USER/Guest chỉ nhận báo cáo tiến trình và thông báo `READY_TO_SHIP`, không duyệt checkpoint. Camunda, backend, database và notification consumer là thành phần nội bộ, không phải actor người dùng trong Use Case Diagram.

## 3. Quy tắc chung của WF05

### 3.1 Đơn vị theo dõi

- Khóa nghiệp vụ của nhánh sản xuất là `orderItemId` hoặc định danh loại tương đương, không phải từng unit quantity.
- Collection OrderItem được chụp khi bắt đầu WF05. ChangeRequest không được tự thêm loại sản phẩm hoặc tạo nhánh checkpoint ngoài phạm vi đã xác định.
- Mỗi checkpoint liên kết Order, OrderItem, stage, attempt, người nộp, thời gian, ghi chú và ảnh.
- Ảnh và attempt cũ là dữ liệu audit bất biến, không ghi đè khi rework.
- Giới hạn số ảnh, kích thước, MIME và dung lượng là cấu hình triển khai cần thống nhất, nhưng backend luôn phải xác minh upload thuộc đúng checkpoint/Order.

### 3.2 Camunda

- WF05 tiếp tục process instance của Order từ WF04.
- Camunda tạo multi-instance subprocess theo danh sách `orderItemId`, có thể cho phép các loại tiến triển độc lập. Multi-instance theo OrderItem, tuyệt đối không theo quantity.
- Trong mỗi instance: user task staff INITIAL_SHAPE → service task đăng ký thông báo → user task staff FINAL_PRODUCT → user task manager KCS → gateway đạt/rework.
- Nhánh rework quay lại task FINAL_PRODUCT của đúng OrderItem và tạo attempt mới.
- Khi tất cả instance hoàn tất với kết quả KCS đạt, service task gọi nghiệp vụ `completeProduction` một lần để chuyển `READY_TO_SHIP` và release assignment.
- Database là nguồn kết quả checkpoint và điều kiện hội tụ. Retry workflow không được tự suy tất cả đã đạt chỉ từ process variable cũ.

### 3.3 Trạng thái và quyền

- Order giữ `ORDER_CREATING` trong toàn bộ quá trình INITIAL_SHAPE, FINAL_PRODUCT, KCS và rework.
- Chỉ assigned STAFF có active assignment mới được nộp checkpoint.
- MANAGER được KCS checkpoint 2. Manager không duyệt checkpoint 1 và USER/Guest không duyệt checkpoint nào.
- Nộp FINAL_PRODUCT không tự release staff và không tự chuyển `READY_TO_SHIP`.
- Staff chỉ được release khi mọi loại đã KCS đạt hoặc khi nghiệp vụ hủy release assignment.
- Không có cổng duyệt production thứ ba sau KCS checkpoint 2 của tất cả loại.

## 4. UC05.1 — Staff nộp checkpoint hình ảnh đầu tiên

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Ghi nhận hình dạng ban đầu của một loại sản phẩm và báo tiến độ cho chủ đơn |
| Kích hoạt | Staff mở task INITIAL_SHAPE của một OrderItem và bấm gửi |
| Tiền điều kiện | Staff là người phụ trách active; Order `ORDER_CREATING`; OrderItem thuộc Order; task đúng stage còn mở |
| Đầu vào | orderId, orderItemId, ảnh, ghi chú tùy chọn, idempotency key/version |
| Thành công | Checkpoint INITIAL_SHAPE và ảnh được lưu, task hoàn tất, thông báo tiến trình được đăng ký |
| Bảo đảm thất bại | Không tạo checkpoint thiếu ảnh, sai item hoặc hoàn tất task khi dữ liệu chưa commit |

### Luồng chính

| Bước | STAFF | Hệ thống |
|---|---|---|
| 1 | Mở danh sách checkpoint của Order | Chỉ hiển thị task thuộc active assignment và từng OrderItem |
| 2 | Chọn OrderItem, tải ảnh và nhập ghi chú | Kiểm tra file theo policy và cấp/liên kết upload an toàn |
| 3 | Bấm gửi INITIAL_SHAPE | Lấy staffId từ JWT, kiểm tra ownership task và idempotency |
| 4 | — | Khóa/kiểm tra Order `ORDER_CREATING`, active assignment, OrderItem và stage |
| 5 | — | Trong transaction tạo Checkpoint INITIAL_SHAPE, ảnh, attempt/audit và công việc hoàn tất task/phát báo cáo |
| 6 | Nhận kết quả | Trả checkpointId, orderItemId, stage và submittedAt |
| 7 | — | Sau commit hoàn tất task Camunda và phát email tiến trình cho USER/Guest |
| 8 | — | Nếu chủ đơn là USER, tạo notification chính xác: “Đã có hình ảnh đầu tiên của item #{orderItemId}” |

Checkpoint 1 không chờ manager hoặc khách mở email. Sau khi dữ liệu commit, nhánh OrderItem được phép tiếp tục sang FINAL_PRODUCT dù email/notification đang retry. Order vẫn `ORDER_CREATING`.

### Ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| A01 | Staff khác hoặc assignment đã release | 403/409, không lưu checkpoint |
| A02 | Order/OrderItem sai quan hệ hoặc Order không ORDER_CREATING | Từ chối, không lộ dữ liệu ngoài quyền |
| A03 | Thiếu ảnh hoặc file không đạt policy | 400, task vẫn mở |
| A04 | INITIAL_SHAPE đã được nộp | Trả kết quả idempotent nếu cùng request, không tạo checkpoint thứ hai |
| A05 | DB lưu ảnh/checkpoint lỗi | Rollback dữ liệu nghiệp vụ, không hoàn tất Camunda task |
| A06 | Commit thành công nhưng Camunda/email lỗi | Giữ checkpoint, retry bàn giao/thông báo, không yêu cầu staff tải lại |

## 5. UC05.2 — Staff nộp sản phẩm hoàn thiện

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Gửi hình ảnh sản phẩm hoàn thiện của một OrderItem để manager KCS |
| Kích hoạt | Staff hoàn thiện loại sản phẩm và nộp task FINAL_PRODUCT |
| Tiền điều kiện | Assigned STAFF active; Order `ORDER_CREATING`; INITIAL_SHAPE của OrderItem đã hoàn tất; không có FINAL_PRODUCT attempt khác đang chờ KCS |
| Đầu vào | orderId, orderItemId, ảnh, ghi chú, idempotency key/version; attempt do backend xác định |
| Thành công | Tạo một FINAL_PRODUCT attempt `PENDING_REVIEW` và task manager KCS |
| Bảo đảm thất bại | Không ghi đè attempt cũ hoặc tạo hai attempt cùng vòng rework |

### Luồng chính

1. Staff mở task FINAL_PRODUCT của OrderItem.
2. UI hiển thị INITIAL_SHAPE và các attempt/reason cũ để staff đối chiếu.
3. Staff tải ảnh sản phẩm hoàn thiện, nhập ghi chú và bấm gửi.
4. Backend kiểm tra JWT, active assignment, Order/OrderItem, stage và trạng thái attempt.
5. Backend sinh `attempt = previousMax + 1` an toàn, không tin số attempt client gửi để ghi đè.
6. Trong transaction lưu Checkpoint FINAL_PRODUCT, ảnh, submittedBy/At, state `PENDING_REVIEW` và công việc bàn giao Camunda.
7. Sau commit hoàn tất staff task, tạo user task KCS cho MANAGER và gửi notification nội bộ cho nhóm/người xử lý phù hợp.
8. Trả checkpointId, attempt và trạng thái chờ KCS.

Nộp FINAL_PRODUCT chưa phát email trạng thái cho chủ đơn, chưa chuyển `READY_TO_SHIP` và chưa release staff. Order vẫn `ORDER_CREATING`.

### Rework và ngoại lệ

- Khi UC05.3 trả `REWORK_REQUIRED`, Camunda tạo lại task FINAL_PRODUCT cho đúng OrderItem. Staff nộp attempt mới theo cùng UC này.
- Attempt mới không xóa ảnh, ghi chú, decision hoặc reason của attempt trước.
- Hai request nộp cùng một vòng rework: unique/idempotency cho phép một attempt thắng.
- Không cho nộp attempt mới khi attempt hiện tại còn `PENDING_REVIEW`.
- Staff khác, item sai Order, thiếu ảnh hoặc trạng thái không phù hợp bị từ chối.
- Commit thành công nhưng tạo manager task/notification lỗi phải retry, không tạo attempt mới để chữa lỗi.

## 6. UC05.3 — Manager KCS sản phẩm hoàn thiện

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Xác nhận FINAL_PRODUCT của từng loại đạt yêu cầu hoặc yêu cầu staff làm lại |
| Kích hoạt | MANAGER mở task KCS của FINAL_PRODUCT attempt đang chờ |
| Tiền điều kiện | Có quyền MANAGER; Order `ORDER_CREATING`; checkpoint thuộc OrderItem và state `PENDING_REVIEW` |
| Đầu vào | checkpointId, orderId, orderItemId, attempt/version, decision PASS/REWORK_REQUIRED, reason bắt buộc khi rework |
| Thành công PASS | Attempt PASSED, nhánh OrderItem hoàn tất; nếu tất cả loại đạt thì Order READY_TO_SHIP và staff được release |
| Thành công REWORK | Attempt REWORK_REQUIRED có reason, tạo lại staff task cho đúng OrderItem |
| Bảo đảm thất bại | Không đổi quyết định cũ, không release staff hoặc chuyển Order một phần |

### Luồng PASS

| Bước | MANAGER | Hệ thống |
|---|---|---|
| 1 | Mở task KCS | Kiểm tra quyền và tải ảnh/attempt hiện hành cùng snapshot yêu cầu |
| 2 | Chọn đạt | Khóa/kiểm tra checkpoint, OrderItem, Order và version |
| 3 | — | Trong transaction ghi Decision PASS, reviewer/time và Checkpoint PASSED |
| 4 | — | Kiểm tra từ database checkpoint 2 của toàn bộ OrderItem |
| 5a | — | Nếu còn loại chưa đạt, hoàn tất nhánh item hiện tại, Order giữ ORDER_CREATING và staff vẫn bận |
| 5b | — | Nếu tất cả đạt, chuyển READY_TO_SHIP, ghi history, đóng đúng active assignment và release staff trong cùng transaction |
| 6 | Nhận kết quả | Trả quyết định, trạng thái Order và danh sách loại còn chờ nếu có |
| 7 | — | Khi toàn đơn READY_TO_SHIP, sau commit phát email trạng thái cho USER/Guest; USER thêm notification, Guest có link token |

### Luồng REWORK_REQUIRED

1. Manager chọn yêu cầu làm lại và nhập reason.
2. Backend kiểm tra quyền, checkpoint/attempt mới nhất và reason không rỗng.
3. Trong transaction ghi Decision, reviewer/time, reason và checkpoint `REWORK_REQUIRED`.
4. Order giữ `ORDER_CREATING`, active assignment không bị release.
5. Camunda quay lại user task FINAL_PRODUCT của đúng OrderItem. Staff nhận notification nội bộ kèm reason.
6. Staff sửa sản phẩm và đi lại UC05.2 để tạo attempt mới.

### Ngoại lệ và cạnh tranh

| Mã | Điều kiện | Kết quả |
|---|---|---|
| B01 | Không có quyền MANAGER | 403, không trả dữ liệu KCS ngoài quyền |
| B02 | Checkpoint không còn PENDING_REVIEW hoặc version cũ | 409 hoặc trả quyết định hiện hành, không ghi đè |
| B03 | REWORK_REQUIRED thiếu reason | 400, task vẫn chờ |
| B04 | Hai manager quyết định đồng thời | Khóa/version cho phép một quyết định có hiệu lực |
| B05 | PASS một loại khi loại khác chưa đạt | Order vẫn ORDER_CREATING, không release staff |
| B06 | Hai nhánh cuối hoàn tất gần đồng thời | Điều kiện hội tụ và khóa Order chỉ chuyển READY_TO_SHIP/release một lần |
| B07 | Retry quyết định sau staff đã nhận Order mới | Release gắn đúng assignmentId/orderId cũ, không release assignment mới |
| B08 | DB commit nhưng Camunda/email lỗi | Giữ kết quả KCS/READY_TO_SHIP, retry bàn giao và thông báo |

## 7. Hoàn tất toàn đơn và release staff

Điều kiện hoàn tất phải đọc dữ liệu chính thức:

- Mọi OrderItem thuộc Order có ít nhất một FINAL_PRODUCT attempt `PASSED` hiện hành.
- Không có OrderItem thiếu checkpoint 2 hoặc còn `PENDING_REVIEW/REWORK_REQUIRED` chưa có attempt đạt sau đó.
- Order vẫn `ORDER_CREATING` và active assignment đúng với `assignmentId/orderId` đang xử lý.

Trong một transaction nghiệp vụ:

1. Chuyển Order `ORDER_CREATING → READY_TO_SHIP`.
2. Ghi thời điểm, actor/nguyên nhân `FINAL_KCS_PASSED` và OrderStatusHistory.
3. Đóng active Assignment, ghi `releasedAt/releaseReason` và đưa staff về khả dụng theo mô hình triển khai.
4. Lưu event/outbox `ORDER_READY_TO_SHIP` và công việc kết thúc WF05.

Không xóa `assignedStaffId` khỏi Order. Sau commit, staff có thể nhận Order mới dù Order cũ đang chờ giao. Retry completion không được đóng assignment mới staff vừa nhận.

## 8. Thông báo và lịch sử

| Mốc | Người nhận | Kênh/nội dung | Order status |
|---|---|---|---|
| INITIAL_SHAPE commit | USER | Email ảnh + notification “Đã có hình ảnh đầu tiên của item #{orderItemId}” | ORDER_CREATING |
| INITIAL_SHAPE commit | Guest | Email ảnh tiến trình | ORDER_CREATING |
| FINAL_PRODUCT submit | MANAGER | Task/notification nội bộ chờ KCS | ORDER_CREATING |
| KCS rework | STAFF phụ trách | Task/notification nội bộ kèm reason | ORDER_CREATING |
| Tất cả KCS pass | USER | Email + notification READY_TO_SHIP | READY_TO_SHIP |
| Tất cả KCS pass | Guest | Email READY_TO_SHIP kèm link Order bảo mật | READY_TO_SHIP |

Checkpoint 1 là email báo cáo tiến trình, không phải một lần thay đổi status. `READY_TO_SHIP` là email trạng thái bình thường thứ tư. Retry dùng khóa theo orderId/orderItemId/stage/attempt/eventType để không gửi trùng.

## 9. Hợp đồng dữ liệu và triển khai

| Thao tác | Đầu vào chính | Đầu ra tối thiểu |
|---|---|---|
| Submit INITIAL_SHAPE | orderId, orderItemId, images, note, idempotency key, STAFF JWT | checkpointId, stage, submittedAt |
| Submit FINAL_PRODUCT | orderId, orderItemId, images, note, idempotency key, STAFF JWT | checkpointId, attempt, PENDING_REVIEW |
| Review FINAL_PRODUCT | checkpointId, attempt/version, decision, reason, MANAGER JWT | decision, item production state, Order status, remaining items |
| Read progress | orderId theo quyền | items, stages, immutable attempts, images, decisions và reasons |

Thiết kế dữ liệu tối thiểu gồm Checkpoint, CheckpointImage và Decision. Unique constraint bảo vệ stage/attempt/idempotency. Ảnh lưu URL/object key và metadata an toàn, không lưu binary lớn trong Camunda variable. Process variable chỉ giữ orderId, orderItemId, checkpointId, attempt và trạng thái điều phối cần thiết.

Tên endpoint, class, topic và task là thiết kế đề xuất. Database quyết định quyền, trạng thái và điều kiện toàn đơn, cache không được dùng làm nguồn duy nhất cho KCS/release.

## 10. Điều không được suy diễn

- Không có manager/customer review checkpoint 1.
- Không có customer approval checkpoint 2.
- Không có timer tự động PASS, tự động READY_TO_SHIP hoặc tự release staff khi quá deadline.
- Không có checkpoint theo từng chiếc và không tạo 20 task cho quantity 20.
- Không có bước xuất kho, reserve/deduct/restore tồn thành phẩm.
- Không cộng thêm một vòng QC thứ ba sau khi mọi checkpoint 2 đã đạt.
- Vận chuyển 2 ngày nằm ngoài thời gian sản xuất và ngoài WF05.

## 11. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | Custom quantity 20 vào WF05 | Một nhánh checkpoint cho custom OrderItem, không phải 20 |
| TC02 | Catalog có ba OrderItem | Ba nhánh theo loại, mỗi nhánh có stage riêng |
| TC03 | Staff không phụ trách nộp checkpoint | Bị từ chối |
| TC04 | Staff nộp INITIAL_SHAPE hợp lệ | Lưu ảnh, Order vẫn ORDER_CREATING, không tạo task duyệt |
| TC05 | INITIAL_SHAPE của USER | Email + notification đúng câu và đúng orderItemId |
| TC06 | INITIAL_SHAPE của Guest | Chỉ email, không notification tài khoản giả |
| TC07 | Email checkpoint 1 lỗi | Checkpoint vẫn lưu, workflow tiếp tục, event được retry |
| TC08 | Staff nộp FINAL_PRODUCT | Tạo attempt PENDING_REVIEW, chưa READY_TO_SHIP |
| TC09 | FINAL_PRODUCT request lặp | Không tạo hai attempt |
| TC10 | Manager rework thiếu reason | Bị từ chối, task vẫn chờ |
| TC11 | Manager yêu cầu rework | Giữ ảnh/decision cũ, tạo staff task cho đúng item |
| TC12 | Staff nộp lại sau rework | Attempt tăng, không ghi đè attempt cũ |
| TC13 | Manager PASS version cũ | Conflict, không đổi decision hiện hành |
| TC14 | Một trong nhiều loại PASS | Order vẫn ORDER_CREATING, staff vẫn bận |
| TC15 | Loại cuối cùng PASS | READY_TO_SHIP và release đúng assignment trong một transaction |
| TC16 | Hai nhánh cuối PASS đồng thời | Một status transition, một release và một event READY_TO_SHIP |
| TC17 | Retry completion sau staff nhận đơn mới | Không release assignment mới |
| TC18 | READY_TO_SHIP của USER | Một email + notification trạng thái |
| TC19 | READY_TO_SHIP của Guest | Một email có link token, không notification hệ thống |
| TC20 | KCS/checkpoint event gửi lặp | Không lặp decision, release hoặc thông báo |
| TC21 | Manager/khách cố duyệt checkpoint 1 | Không có chức năng/quyền tương ứng |
| TC22 | WF05 hoàn tất | Không SHIPPING/DELIVERED, bàn giao WF07 |

## 12. Sơ đồ

- [Use Case tổng quát](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity Diagram theo từng UC](diagram.md).
- [Sequence Diagram theo từng UC](sequence.md).
