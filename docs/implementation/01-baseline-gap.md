# PHẦN 01 — Báo cáo baseline và bảng chênh lệch

Ngày kiểm tra: 06/10/2026  
Nguồn nghiệp vụ: [target.txt](../target.txt) và bộ tài liệu [WF01–WF09](../use-cases/README.md)  
Phạm vi: backend, schema do JPA quản lý, BPMN, event/notification và phần frontend có trong repository.

Ghi chú sau audit: người dùng đã chốt Liquibase và giữ `double/Double`. Các nhận xét hiện trạng ở mục 1–7 là ảnh chụp trước PHẦN 02; xem [báo cáo PHẦN 02](02-domain-migration.md) để biết code/schema hiện đã thay đổi.

## 1. Kết luận

Baseline hiện tại chạy ổn định nhưng domain và workflow chưa phù hợp với thiết kế mới. Có thể tiếp tục sang PHẦN 02, tuy nhiên không được chỉ đổi tên `OrderStatus`: dữ liệu đơn cũ, trạng thái thanh toán, assignment, tiền và process instance Camunda phải được migrate có kiểm soát.

Các vấn đề quan trọng nhất:

1. `OrderStatus` vẫn mô tả luồng kho cũ; một enum `OrderProductionStatus` song song chưa giải quyết được lifecycle mục tiêu.
2. Một số nhánh tạo payment trước khi manager duyệt; callback thành công lại đưa đơn về `PENDING_APPROVAL`.
3. Assignment hiện được lưu theo `OrderItem`, trong khi target yêu cầu một staff sản xuất cho một Order và staff chỉ giữ một assignment đang hoạt động.
4. Ba file BPMN không cùng một lifecycle. `ApproveCartProcess` dùng `userId` làm business key; guest dùng process khác với business key theo `orderId`.
5. Tiền đang dùng `double/Double` trên toàn chuỗi entity–DTO–service; custom order chưa có giá lại có thể bị biểu diễn thành `0.0`.
6. Event phát sau commit trực tiếp sang Redis Stream nhưng lỗi chỉ được ghi log, nên chưa có bảo đảm retry/durable delivery.
7. Nghiệp vụ stock, xuất kho, reconciliation và complaint vẫn tồn tại dù không còn thuộc target.
8. Repository này không chứa mã nguồn frontend, chỉ có `frontend/nginx.conf`; không thể audit enum/action ở giao diện trong PHẦN 01.

Không thay đổi entity, enum, API hay BPMN trong PHẦN 01.

## 2. Baseline kỹ thuật

### 2.1 Môi trường và kết quả test

| Hạng mục | Kết quả |
|---|---|
| Java | Temurin `21.0.12` |
| Maven Wrapper | `3.9.16` |
| Lệnh | `.\mvnw.cmd test` |
| Kết quả | `BUILD SUCCESS` |
| Test | 167 chạy, 0 failure, 0 error, 0 skipped |
| Thời gian Maven báo cáo | 19.447 giây |

Log test có các exception giả lập như Redis/Camunda không khả dụng và một lần vi phạm unique constraint được test chủ động. Đây không phải test fail.

### 2.2 Quy mô mã nguồn

| Loại | Số lượng |
|---|---:|
| Java production | 249 file |
| Java test / Surefire report | 37 file / 37 report |
| Entity | 28 |
| Controller | 20 |
| Repository | 27 |
| Service, gồm thư mục con | 44 |
| BPMN | 3 |

Stack chính: Java 21, Spring Boot 3.1.12, Camunda 7.21.0, MySQL, MongoDB, Redis, H2 cho test.

### 2.3 Schema và migration

- Không có Flyway, Liquibase, changelog hoặc script `db/migration`.
- Cả cấu hình chung, dev và prod đều dùng `spring.jpa.hibernate.ddl-auto: update`.
- `OrderRepository.findByIdForUpdate` đã có pessimistic lock cho một số thao tác.
- `Order` chưa có `@Version`; chỉ `OrderItemAssignment` và `ProductionCheckpoint` có optimistic version.

Kết luận: `ddl-auto=update` không đủ để đổi state, backfill dữ liệu và rollback an toàn. PHẦN 02 phải đưa migration có version vào trước khi đổi model.

## 3. Quy ước phân loại

| Nhãn | Ý nghĩa |
|---|---|
| `KEEP` | Có thể giữ hành vi/cấu trúc chính, chỉ bổ sung test hoặc tích hợp nhỏ |
| `MODIFY` | Còn dùng nhưng phải đổi contract hoặc business rule |
| `ADD` | Chưa có, cần tạo mới |
| `MIGRATE` | Có dữ liệu/cấu trúc cũ phải ánh xạ sang model mới |
| `REMOVE_AFTER_MIGRATION` | Không còn thuộc target; chỉ loại bỏ sau khi không còn consumer/dữ liệu phụ thuộc |

## 4. Bản đồ hiện trạng

### 4.1 Domain và persistence

| Thành phần hiện tại | Phân loại | Nhận xét / đích đến |
|---|---|---|
| `Order`, `OrderItem`, contact snapshot, history | `MODIFY` + `MIGRATE` | Giữ aggregate nhưng thêm type, payment state, version, timestamps, cancellation metadata và snapshot mục tiêu |
| `OrderStatus` | `MIGRATE` | Thay state kho bằng lifecycle mới; cần ánh xạ theo dữ liệu thực tế chứ không đổi enum trực tiếp |
| `OrderProductionStatus` | `MIGRATE` | Đang trùng một phần lifecycle; sau migration chỉ giữ nếu còn giá trị độc lập rõ ràng |
| `ProductAvailabilityStatus` | `KEEP` | Đã có đúng `ACCEPTING_ORDERS`, `PAUSED`, `DISCONTINUED` |
| `Product`, `ProductVariant` | `MODIFY` | Chưa có `madeDay`; còn `quantity` legacy và giá `double` |
| `OrderItemAssignment` | `MIGRATE` | Hiện one-to-one với item; target cần một active assignment trên Order và unique active staff |
| `ProductionCheckpoint`, image, decision | `MODIFY` | Có nền tảng attempt/version nhưng cần checkpoint theo OrderItem/type và quy tắc hội tụ mới |
| `ChatMessage`, `ConsultationRequest` | `MIGRATE` | Chat hiện theo consultation/user; cần `ChatThread` theo `orderId`, membership chỉ USER–STAFF |
| Voucher entities/services | `MODIFY` | Có user/guest quota; phải thống nhất thời điểm consume/restore và chống spam guest |
| Inventory entity/service/delegate | `REMOVE_AFTER_MIGRATION` | Không còn nghiệp vụ stock trong workflow mới |
| Complaint DTO/event/email/API | `REMOVE_AFTER_MIGRATION` | Target giữ xác nhận chênh lệch ngay tại nhận hàng, không có complaint workflow |
| `CustomRequest`, Agreement, `ChangeRequest`, `PaymentAttempt` | `ADD` | Chưa có domain model mục tiêu |
| Guest lookup token | `MODIFY` | Đã hash token trên Order nhưng chưa có scope, expiry, revoke/cấp lại hoàn chỉnh |
| Transactional outbox / delivery ledger | `ADD` | Chưa có nguồn phát event bền vững |

### 4.2 API liên quan trực tiếp lifecycle

| API hiện tại | Xử lý |
|---|---|
| `/api/cart/**`, `/api/guest-checkout` | Giữ route nếu phù hợp; sửa checkout để luôn tạo `PENDING_APPROVAL`, không reserve stock và không payment trước duyệt |
| `/api/cart/approve/{userId}` | Deprecate sau khi client migrate; route và business key theo user không phù hợp một user nhiều Order |
| `/api/orders/manager/review-order/{orderId}` | Giữ tương thích tạm thời; chuyển sang transition service và version/reason rõ ràng |
| `/api/orders/manager/assign-staff/{orderId}` | Sửa sang assignment theo Order và kiểm tra staff available nguyên tử |
| `/api/orders/staff/claim/{orderId}` | Giữ ý nghĩa claim nhưng dùng chung assignment service với manager |
| `/api/orders/staff/export/{orderId}` | Migrate sang bàn giao theo `orderItemId`; bỏ nghĩa xuất kho |
| `/api/orders/manager/kcs-check/{orderId}` | Migrate sang KCS checkpoint 2 theo loại/attempt; chỉ hội tụ mới `READY_TO_SHIP` |
| `/api/payment/momo-pay`, `/momo-callback` | Sửa để dùng `PaymentAttempt`, mở sau `ORDER_ACCEPTED`, webhook idempotent và không hồi sinh đơn đã hủy |
| `/api/orders/{id}`, `/me`, history | Giữ route/read use case, cập nhật DTO/state/giá nullable |
| `/api/orders/{id}/cancel` | Migrate sang `CancelOrderService`; thêm guest token flow và idempotency |
| `/api/orders/customer/confirm-receipt/{id}` | Giữ mục đích; request phải theo `orderItemId`, hỗ trợ guest token và mismatch |
| `/api/orders/customer/receipt-complaint/{id}` | `REMOVE_AFTER_MIGRATION` |
| `/api/orders/{id}/reorder` | Sửa: catalog trả added/skipped; custom tạo draft mới |
| `/api/consultations/**`, `/api/chat/**` | Không dùng trực tiếp làm order chat; tái sử dụng hạ tầng Mongo/WebSocket sau khi thêm thread/membership theo Order |
| `/api/products/variants/{id}/restock` | `REMOVE_AFTER_MIGRATION` |

API mới tối thiểu cần có ở các phần sau: custom draft/submit, order agreement, order chat, payment attempt/read, staff start production, checkpoint submit/review, change request, guest order lookup/cancel/receipt và các endpoint action có version/idempotency.

### 4.3 Event, email và notification

`DomainEventPublisher` đăng ký callback sau commit rồi ghi trực tiếp Redis Stream. Khi Redis lỗi, code chỉ log và trả về; event có thể mất vĩnh viễn. `RedisEventIdempotencyService` cũng có nhánh fail-open. Đây là khoảng trống so với yêu cầu retry/reconciliation.

`EventTypes` hiện có `ORDER_CREATED`, `GUEST_ORDER_CREATED`, `ORDER_DELIVERED`, `ORDER_CANCELLED` và các request email chung, nhưng chưa biểu diễn đầy đủ bốn milestone trạng thái, checkpoint, payment-paid-to-staff và production start.

Đích đến:

- DB transaction lưu domain change và outbox cùng lúc.
- Worker publish Redis Stream; retry và trạng thái delivery có thể quan sát.
- Consumer idempotent theo `eventId` và milestone/order.
- Chỉ bốn email trạng thái bình thường: `PENDING_APPROVAL`, `ORDER_ACCEPTED`, `ORDER_CREATING`, `READY_TO_SHIP`.
- Checkpoint và cancel là event riêng, không bị tính thành email chuyển trạng thái bình thường.

### 4.4 BPMN và business key

| File/process | Hiện trạng | Chênh lệch |
|---|---|---|
| `approve-cart.bpmn` / `ApproveCartProcess` | Executable; payment + timer PT15M trước manager review; sau đó xuất kho, KCS, deduct inventory, nhận hàng, reconciliation | Phải thiết kế lại thứ tự manager review → assignment/agreement → payment PT1H → staff start → production; bỏ stock/reconciliation. CartService hiện start bằng business key `userId`, gây xung đột khi một user có nhiều đơn |
| `guest-purchase-runtime.bpmn` / `GuestPurchaseFlow` | Executable; business key `guest-order-{orderId}`; riêng luồng guest, manager review rồi assign handmade | Không nên là lifecycle cạnh tranh; guest catalog phải dùng cùng quy tắc order lifecycle, không custom/chat |
| `guest-purchase-flow.bpmn` / `GuestPurchaseFlowBlueprint` | Blueprint không executable; còn reserve stock, online guest payment PT15M, manager duyệt checkpoint 1 | Trái target ở stock, Guest chỉ COD và checkpoint 1 gửi thẳng; chỉ dùng làm tài liệu tham khảo, không triển khai |

Camunda phù hợp cho WF02–WF05 vì có human task, service task, timer và message correlation. Database vẫn là source of truth; một Order dùng một lifecycle process với business key chuẩn hóa theo `orderId`. WF06–WF09 nên nối bằng event/subprocess/call activity khi có lợi, không tạo process lifecycle thứ hai cho cùng Order.

## 5. Gap matrix theo workflow

| Workflow | Hiện có có thể tận dụng | Chênh lệch bắt buộc | Phân loại chính |
|---|---|---|---|
| WF01 Custom request | Consultation và upload có thể tham khảo cho form/ảnh | Chưa có nhiều draft/user, submit idempotent, snapshot CUSTOM và liên kết draft–Order | `ADD` |
| WF02 Accept order | Checkout, Order, manager review, staff claim/assign đã có một phần | Thứ tự payment sai; state kho; assignment theo item; thiếu staff availability; process/business key phân mảnh; catalog duration chưa có | `MIGRATE` + `MODIFY` |
| WF03 Agree order | Mongo chat, WebSocket, consultation access control có nền tảng | Chưa có ChatThread theo Order, agreement version, custom form, voucher trong chat, PT24H từ manager approve; manager hiện còn được đọc chat consultation | `ADD` + `MIGRATE` |
| WF04 Payment & start | MoMo callback và Camunda correlation đã có | Chưa có PaymentAttempt; timer 15m; callback đưa về `PENDING_APPROVAL`; lỗi correlation chỉ log; thiếu user task staff start và deadline từ `productionStartedAt` | `MIGRATE` + `ADD` |
| WF05 Production/checkpoint | Entity checkpoint/image/decision và KCS có một phần | KCS hiện trực tiếp `SHIPPING`; checkpoint 1 từng có manager review; chưa hội tụ theo loại, release một lần, event tiến trình riêng | `MODIFY` |
| WF06 Change request | Chưa có domain chuyên biệt | Cần request/version/progress guard, staff accept/reject, whitelist field; không đổi giá/deadline và không chặn main flow | `ADD` |
| WF07 Ship order | Exported/received quantity đã có trong OrderItem | Hành vi hiện theo variant/xuất kho; cần input theo orderItem, `READY_TO_SHIP → SHIPPING`, không tích hợp shipper và không email SHIPPING | `MIGRATE` |
| WF08 Complete order | Confirm receipt, reputation +2, product review đã có một phần | USER-only; chưa có guest token; map theo variant dễ nhập nhằng; còn complaint; cần idempotency và accept mismatch trực tiếp | `MODIFY` + `REMOVE_AFTER_MIGRATION` |
| WF09 Cancel order | Có cancel trong service/delegate và event cancel | Logic phân tán; guest chưa tự hủy an toàn; penalty custom chưa giá sai; còn restore stock; xóa process trực tiếp; thiếu source/reference/idempotency/race policy | `MIGRATE` |

## 6. Gap xuyên luồng theo target

| Yêu cầu | Hiện trạng | Hướng xử lý |
|---|---|---|
| Lifecycle mới | Hai enum và state kho cũ | Một transition policy; migration old→new có điều kiện |
| Payment tách Order status | `PENDING_PAYMENT` nằm trong `OrderStatus` | Thêm `paymentStatus` và `PaymentAttempt` |
| Contract tiền | `double/Double` trên entity, DTO, event, service | Giữ kiểu tiền theo quyết định mới; giá chưa chốt phải giữ null xuyên entity/DTO |
| Giá CUSTOM chưa xác định | `finalPrice` có mặc định `0.0` | Cho phép chưa có giá bằng nullable/draft contract; không dùng 0 giả |
| Catalog duration | Không có `madeDay` | Thêm giá trị manager nhập `>= 2`, snapshot duration và hàm tính/test q=1/5/6/10 |
| Lịch hoàn thành | Chưa có calculator domain | Thêm calculator theo ngày nghỉ đã chốt trong target và chạy từ `ORDER_CREATING` |
| Một staff/một active Order | Chỉ unique theo OrderItem; User không có availability field | Assignment theo Order + unique/ràng buộc transaction cho active staff |
| Concurrency | Một số pessimistic lock; Order không có version | Thêm `@Version`, lock đúng aggregate và expected version ở action |
| Idempotency | Checkout dùng Redis; nhiều action chưa có | Idempotency record/result bền vững cho mutation quan trọng |
| Event bền vững | Redis publish sau commit, lỗi chỉ log | Transactional outbox + retry + reconciliation |
| Guest security | Có hash token đơn giản | Scope/expiry/revoke/rate limit; GET không mutation |
| Audit transition | Có history nhưng chưa bao phủ actor/reason/reference mục tiêu | Chuẩn hóa audit trên transition service |
| Frontend contract | Không có frontend source | Bàn giao OpenAPI/DTO/state-action matrix; cần repository frontend riêng để triển khai UI |

## 7. Dữ liệu cần migrate và rủi ro cao

### 7.1 Rủi ro mức cao

1. **Ánh xạ state cũ:** `PENDING_WAREHOUSE`, `WAREHOUSE_ASSIGNED`, `PENDING_KCS`, `PENDING_PAYMENT` không có ánh xạ 1–1. Phải dựa thêm payment, assignment, production checkpoint và dữ liệu xuất kho.
2. **Process instance đang chạy:** deploy BPMN mới không tự chuyển instance cũ. Cần chọn tiếp tục definition cũ, migrate instance đã kiểm thử, hoặc đóng/mở lại theo policy có audit.
3. **Tiền `double`:** chuyển kiểu có thể thay đổi làm tròn và unique/idempotency của payment. Phải backfill, đối soát trước/sau và không tạo payment trong lúc dữ liệu nửa cũ nửa mới.
4. **Assignment item→order:** một Order cũ có thể có nhiều staff ở nhiều item. Cần báo cáo conflict và policy chọn/đóng assignment, không tự lấy staff đầu tiên.
5. **Payment callback muộn:** callback có thể đến sau timeout/cancel. Không được đưa Order `CANCELLED` sống lại; phải ghi attempt để đối soát thủ công/tài chính.
6. **Event bị mất/trùng:** chuyển từ publish trực tiếp sang outbox cần consumer idempotent và mốc cutover để không vừa bỏ sót vừa gửi hai email.
7. **Loại bỏ stock:** chỉ xóa cột/service sau khi mọi checkout, cancel, BPMN và endpoint restock đã ngừng gọi; dữ liệu lịch sử giữ hoặc archive theo quyết định migration.
8. **Guest token cũ:** token hiện không có scope/expiry. Việc đổi contract có thể làm link email cũ mất hiệu lực; cần policy tương thích hoặc cấp lại.
9. **Complaint cũ:** không xóa dữ liệu khi bỏ endpoint; xác định cách giữ lịch sử read-only.
10. **Một user nhiều order:** business key `userId` hiện tại có thể correlation nhầm. Mọi process mới phải dùng một business key gắn duy nhất với Order.

### 7.2 Tương thích tạm thời

- Giữ read API `/api/orders/{id}`, `/me` và history, nhưng version DTO để client phân biệt state mới.
- Route action cũ chỉ được làm adapter gọi application service mới; không duy trì hai bộ business rule.
- Enum/string mới phải được deploy theo trình tự expand → backfill → switch reads/writes → contract, tránh đổi enum trước DB.
- BPMN cũ và mới phải có deployment/version rõ; không sửa file rồi giả định instance cũ chạy graph mới.
- Endpoint stock và complaint trả deprecation rõ trước khi loại bỏ nếu còn client sử dụng.

## 8. Quyết định kỹ thuật đề xuất cho PHẦN 02

| Chủ đề | Đề xuất | Cần xác nhận trước khi khóa migration |
|---|---|---|
| Migration tool | **Đã chốt Liquibase**; migration expand bật bằng profile riêng trên schema legacy, Hibernate validate để tránh tự tạo cột trước changelog | Đã xác nhận ngày 06/10/2026 |
| Tiền | **Đã chốt tiếp tục dùng `double/Double`**; giá custom chưa xác định dùng `null`, không dùng `0.0` | Đã xác nhận ngày 06/10/2026 |
| Order concurrency | `@Version` + pessimistic lock ở transition/race quan trọng | Không có xung đột với target |
| Lifecycle source of truth | DB state/history; Camunda điều phối task/timer/message | Không có xung đột với target |
| Event delivery | Transactional outbox trong MySQL; Redis Stream là transport | Không có xung đột với target |
| Business key | Chuỗi chuẩn chứa duy nhất `orderId`, ví dụ `order-{id}` | Có thể chốt trong code convention |
| Instance cũ | Mặc định để instance cũ chạy definition cũ trong giai đoạn tương thích; chỉ migrate nếu có test case cho từng wait state | Cần kiểm kê dữ liệu môi trường thật |

## 9. Phạm vi chính xác của PHẦN 02

PHẦN 02 chỉ chuẩn hóa domain contract và migration; chưa triển khai đầy đủ endpoint/workflow của WF01–WF09.

### 9.1 Việc phải làm

1. Thêm Liquibase, master changelog cho schema hiện có và chuyển dần từ `ddl-auto=update` sang `validate` sau khi baseline đầy đủ theo profile an toàn.
2. Viết migration **expand** trước: cột/bảng/enum string mới vẫn cho code cũ chạy trong thời gian chuyển tiếp.
3. Chuẩn hóa `OrderStatus` mục tiêu; thêm `OrderType`, `PaymentStatus`, payment method, cancellation source/reference và timestamps.
4. Giữ kiểu tiền `double/Double`; chuẩn hóa tính nullable để giá custom chưa xác định không bị biểu diễn thành `0.0`, đồng thời giữ contract tiền nhất quán trên Order, OrderItem, Product/Variant, Voucher, DTO và event.
5. Thêm `@Version` cho Order và các index/ràng buộc cần thiết.
6. Thêm schema/domain skeleton cho `CustomRequest`, Order Agreement, order-level Assignment, `PaymentAttempt`, `ChangeRequest`, receipt data và guest token metadata.
7. Thêm `madeDay` trên catalog, validation database/model và duration snapshot trên OrderItem; chưa thay toàn bộ checkout ở phần này.
8. Viết state/data mapper và migration test cho state cũ, gồm báo cáo record không ánh xạ chắc chắn.
9. Đánh dấu field/table stock, tracking, complaint và assignment-item là legacy; chưa xóa vật lý.
10. Cập nhật repository/mapper tối thiểu để project compile và test được với model expand; không đưa business rule mới rải vào controller.

### 9.2 Ngoài phạm vi PHẦN 02

- Chưa viết lại BPMN.
- Chưa thay checkout/payment/cancel chạy thật sang flow mới.
- Chưa xóa inventory/complaint endpoint hoặc dữ liệu.
- Chưa triển khai chat agreement, checkpoint, shipping hay receipt mới.
- Chưa sửa frontend vì repository này không chứa frontend application source.

### 9.3 Điều kiện hoàn thành PHẦN 02

- Migration chạy được từ schema baseline và chạy lại không phá dữ liệu.
- Có test ánh xạ state/money/assignment cũ, gồm case không thể ánh xạ tự động.
- Entity/enum/schema nhất quán và toàn bộ test qua.
- Giá CUSTOM chưa chốt không bị lưu thành `0` như giá chính thức.
- Có rollback/cutover note và không xóa nóng dữ liệu legacy.

## 10. File/nhóm code chịu tác động ở các phần sau

- Domain: `entity/Order.java`, `OrderItem.java`, `Product.java`, `ProductVariant.java`, assignment/checkpoint/voucher entities và các enum trong `nume`.
- Persistence: `OrderRepository` và repository assignment/checkpoint/voucher; thêm repository cho các aggregate mới.
- Application: `CartService`, `OrderService`, `ManagerOrderService`, `GuestCheckoutService`, `MomoService`, chat/consultation service và các delegate.
- API: Cart, GuestCheckout, Order, ManagerOrder, StaffOrder, Momo, Chat, Product controllers và DTO tương ứng.
- Workflow: ba BPMN hiện tại, workflow start/correlation service và delegate.
- Integration: `DomainEventPublisher`, Redis Stream consumer/idempotency, EmailService và NotificationService.
- Test: checkout, order, payment, workflow, voucher, checkpoint, event và security tests.

## 11. Bằng chứng và trạng thái bàn giao

- Test baseline: đạt 167/167.
- Gap matrix: hoàn thành cho WF01–WF09 và các concern xuyên luồng.
- Migration rủi ro cao: đã liệt kê.
- PHẦN 02: đã khóa phạm vi; các lựa chọn cần xác nhận được tách riêng, không dùng code cũ làm mặc định.
- Frontend: chưa thể audit do thiếu source trong repository; đây là dependency bàn giao, không phải test fail của backend.
