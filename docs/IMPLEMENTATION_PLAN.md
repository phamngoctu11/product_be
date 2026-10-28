# Kế hoạch và nhật ký tái triển khai hệ thống

Ngày tạo: 06/10/2026.

File này là **điểm tiếp tục công việc giữa các phiên làm việc**. Trước khi sửa code, người triển khai phải đọc file này, [target.txt](target.txt) và tài liệu workflow liên quan trong [use-cases](use-cases/README.md).

## 1. Trạng thái hiện tại

```text
Tài liệu nghiệp vụ:       ĐÃ CHỐT CATALOG/CUSTOM ĐỀU HANDMADE MADE-TO-ORDER; CUSTOM 1 ITEM NHIỀU QUANTITY; CATALOG NHIỀU ITEM
Tái triển khai code:      BACKEND VÀ FRONTEND PHẦN 01–13 ĐÃ VERIFIED THEO REVIEW CỦA CHỦ DỰ ÁN
Phần đang thực hiện:      KHÔNG CÒN PHẦN CHỨC NĂNG; DEPLOY LIVE NẰM NGOÀI PHẠM VI
Phần hoàn thành gần nhất: REVIEW XUYÊN LUỒNG; ĐƠN THANH TOÁN ONLINE ĐÃ ĐƯỢC TEST THÀNH CÔNG
Ngày cập nhật gần nhất:   28/10/2026
```

Ngày 28/10/2026, chủ dự án xác nhận đã review và chấp nhận các phần còn lại; đồng thời xác nhận đã test hoàn tất một đơn thanh toán online. Trạng thái `VERIFIED` dưới đây ghi nhận xác nhận đó. Các kết quả test cũ vẫn được giữ đúng như đã ghi; lần cập nhật này không khẳng định có thêm lượt chạy full suite, E2E, MySQL rehearsal hoặc deploy live. Các ghi chú bàn giao bên dưới là ảnh chụp tại thời điểm triển khai từng phần, không phủ định trạng thái review mới nhất.

## 2. Thứ tự nguồn quyết định

Khi có khác biệt, áp dụng theo thứ tự:

1. Quyết định mới nhất được người dùng xác nhận.
2. [target.txt](target.txt) — nguồn nghiệp vụ chính.
3. Đặc tả WF01–WF09 trong [docs/use-cases](use-cases/README.md).
4. File kế hoạch này — nguồn tiến độ và thứ tự triển khai, không thay thế nghiệp vụ.
5. Code/BPMN hiện tại — hiện trạng cần migrate, không được dùng để phủ định target.

Nếu phát hiện target và use case lệch nhau, dừng phần bị ảnh hưởng, ghi vào mục “Quyết định/chặn cần xử lý” và làm rõ trước khi chọn hành vi.

## 3. Quy ước trạng thái

| Trạng thái | Ý nghĩa |
|---|---|
| `NOT_STARTED` | Chưa bắt đầu sửa code của phần này |
| `IN_PROGRESS` | Đang triển khai, chưa đủ điều kiện bàn giao |
| `BLOCKED` | Không thể tiếp tục vì thiếu quyết định hoặc phụ thuộc cụ thể |
| `IMPLEMENTED` | Đã viết code/migration nhưng chưa vượt toàn bộ kiểm tra |
| `VERIFIED` | Chủ dự án đã review và chấp nhận phạm vi; bằng chứng test ghi riêng theo đúng các lượt đã chạy |

Không ghi nhận test hoặc migration rehearsal chưa chạy như đã hoàn tất. Trạng thái tổng thể ngày 28/10 dựa trên xác nhận review của chủ dự án; các bước deploy live không thuộc phạm vi xác nhận này.

## 4. Bản đồ 13 phần triển khai

| Phần | Phạm vi | Workflow/chức năng | Phụ thuộc | Trạng thái |
|---:|---|---|---|---|
| 01 | Baseline và bảng chênh lệch | Toàn hệ thống hiện tại | Không | `VERIFIED` |
| 02 | Domain contract và migration expand | State, entity, tiền, thời gian, dữ liệu mới | 01 | `VERIFIED` |
| 03 | Hạ tầng nhất quán dùng chung | Transition, lock/version, idempotency, outbox, token, audit | 02 | `VERIFIED` |
| 04 | Catalog, cart và checkout | Catalog, cart USER/Guest, đầu vào WF02 | 02–03 | `VERIFIED` |
| 05 | Custom request | WF01 | 02–04 | `VERIFIED` |
| 06 | Nghiệp vụ hủy dùng chung | WF09 kernel và API khách hủy | 02–03 | `VERIFIED` |
| 07 | Manager review và assignment | WF02, manager reject gọi WF09 | 04–06 | `VERIFIED` |
| 08 | Chat, agreement và timer custom | WF03, PT24H | 05–07 | `VERIFIED` |
| 09 | Payment và staff bắt đầu | WF04, PT1H, webhook, ORDER_CREATING | 03, 06–08 | `VERIFIED` |
| 10 | Production, checkpoint và KCS | WF05 | 07–09 | `VERIFIED` |
| 11 | ChangeRequest | WF06 | 08–10 | `VERIFIED` |
| 12 | Bàn giao, nhận hàng và reorder | WF07, WF08, F17 | 06, 10–11 | `VERIFIED` |
| 13 | Tích hợp xuyên luồng và rollout | BPMN, event, email, cache, FE, E2E, migration rollout | 01–12 | `VERIFIED` |

Thứ tự trên là thứ tự dependency, không phải thứ tự số workflow. WF09 được xây sớm vì manager reject, timer custom và payment timeout đều cần gọi cùng nghiệp vụ hủy.

## 5. Chi tiết từng phần

### PHẦN 01 — Baseline và bảng chênh lệch

Trạng thái: `VERIFIED`.

Mục tiêu:

- Ghi lại trạng thái build/test trước khi thay đổi nghiệp vụ.
- Lập bảng ánh xạ target với entity, enum, repository, service, controller, BPMN và frontend hiện tại.
- Phân loại thành `KEEP`, `MODIFY`, `ADD`, `MIGRATE`, `REMOVE_AFTER_MIGRATION`.
- Xác định dữ liệu hiện có cần migrate và những endpoint cần tương thích tạm thời.

Việc cần làm:

- Chạy toàn bộ test hiện tại và ghi command/kết quả.
- Kiểm tra schema được tạo bằng JPA hay migration tool; chọn Flyway/Liquibase hoặc phương án migration được kiểm soát.
- Thống kê endpoint và service đang phụ thuộc các state cũ.
- Kiểm tra cả ba BPMN hiện có và process instance/business key.
- Kiểm tra frontend đang dùng enum/API nào.
- Lập gap report, không bắt đầu đổi enum trong phần này.

Đầu ra bắt buộc:

- [Báo cáo baseline và gap matrix](implementation/01-baseline-gap.md).
- Danh sách migration rủi ro cao nằm tại mục 7 của báo cáo.
- Baseline `.\mvnw.cmd test`: 167 test, 0 failure, 0 error, 0 skipped; `BUILD SUCCESS`.
- Phạm vi chính xác của PHẦN 02 nằm tại mục 9 của báo cáo.
- Frontend application source không có trong repository; báo cáo đã ghi rõ giới hạn audit và contract cần bàn giao.

### PHẦN 02 — Domain contract và migration

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10. Các ghi chú cutover workflow/state/assignment legacy bên dưới là lưu ý rollout, không còn là blocker của phạm vi chức năng được chấp nhận.

Bàn giao: [02-domain-migration.md](implementation/02-domain-migration.md), [preflight SQL](implementation/sql/02-preflight.sql).

- Liquibase master/expand changelog, 6 entity/repository mới, enum lifecycle và metadata/snapshot.
- Tiền giữ `double/Double`, giá CUSTOM chưa chốt giữ `null` qua mapper/DTO.
- Assignment active unique theo Order/staff, payment provider reference unique, version cho Order.
- Manager nhập madeDay hữu hạn >= 2; catalog legacy thiếu thời gian để null chờ bổ sung.
- LegacyOrderStateMapper chỉ đánh giá, không đổi state/process cũ khi chưa có đủ bằng chứng.
- Hibernate validate; bật profile migration trước khi khởi động với schema mới. Changelog yêu cầu database legacy, chưa bootstrap database trống.
- Kiểm thử ngày 06/10/2026: 178 test đạt, gồm H2 và MySQL 8.0.36 Docker; không lỗi/bỏ qua. Command: `.\mvnw.cmd test '-DmysqlMigrationTests=true' '-Dapi.version=1.44'`.

Mục tiêu:

- Chuẩn hóa model trước khi viết lại workflow.
- Có migration tiến/lùi hoặc chiến lược rollback rõ ràng, không làm mất dữ liệu cũ.

Phạm vi chính:

- `OrderStatus`: `PENDING_APPROVAL`, `PENDING_ASSIGNMENT`, `DISCUSSING`, `WAITING_STAFF_CONFIRMATION`, `ORDER_ACCEPTED`, `ORDER_CREATING`, `READY_TO_SHIP`, `SHIPPING`, `DELIVERED`, `CANCELLED`.
- Tách `paymentStatus` khỏi Order status và chuẩn hóa `paymentMethod`.
- Bổ sung `orderType`, version/optimistic locking, các timestamp và cancellation source/reference.
- Giữ `double/Double` cho tiền theo quyết định ngày 06/10/2026; giá CUSTOM chưa chốt dùng `null` và DTO phải giữ nguyên ý nghĩa này.
- Chuẩn hóa Order/OrderItem snapshot, `exportedQuantity`, `receivedQuantity`.
- Bổ sung/migrate Assignment, PaymentAttempt, CustomRequest, Agreement, ChangeRequest, receipt và lookup token theo thiết kế được chốt.
- Lập ánh xạ state cũ như `PENDING_WAREHOUSE`, `WAREHOUSE_ASSIGNED`, `PENDING_KCS`, `PENDING_PAYMENT` sang dữ liệu mới.
- Đánh dấu các trường stock/tracking/complaint cũ để loại bỏ sau migration, không xóa nóng khi chưa xác minh dữ liệu.

Đầu ra bắt buộc:

- Entity/enum mới và migration chạy được trên dữ liệu thử.
- Test mapping state/data cũ sang mới.
- Không có số tiền chính thức mặc định bằng 0 khi giá chưa được xác định.

### PHẦN 03 — Hạ tầng nhất quán dùng chung

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10; tích hợp xuyên luồng được chấp nhận ở PHẦN 13.

Bàn giao: [03-consistency.md](implementation/03-consistency.md).

- Liquibase 03 tạo durable request ledger, outbox và transition audit; publisher/consumer hiện tại đã dùng hạ tầng này.
- Request trùng trả response đã commit; khác payload trả 409. Consumer deduplicate theo group/eventId trong DB.
- Outbox retry giữ eventId, có recovery nội bộ; cache event lỗi còn được retry.
- Transition bắt buộc transaction ngoài, lock Order/version, audit và cache event nguyên tử. Workflow service phải kiểm tra authorization và điều kiện nghiệp vụ chuyên biệt.
- Guest token có scope/expiry/revoke/rotate và rate limit theo Order. TTL/budget truyền rõ từ policy của endpoint sau này, chưa đặt mặc định nghiệp vụ.
- Error code ổn định qua X-Error-Code và CORS expose; notification realtime chỉ sau commit.
- Test: `.\mvnw.cmd test '-DmysqlMigrationTests=true' '-Dapi.version=1.44'` — **187 tests, 0 failure/error/skipped, BUILD SUCCESS**, H2 + MySQL 8.0.36 + JPA integration.
- SMTP vẫn có cửa sổ gửi trùng nếu chết sau gửi nhưng trước commit ledger; giới hạn và bước rollout được ghi rõ trong bàn giao.

Mục tiêu:

- Mọi workflow dùng cùng cơ chế kiểm tra chuyển trạng thái và side effect.

Phạm vi chính:

- Application service/state transition policy duy nhất cho từng chuyển trạng thái.
- Optimistic/pessimistic locking theo race condition.
- Idempotency key và kết quả request đã xử lý.
- Outbox/job bền vững, eventId, consumer idempotency và reconciliation.
- Audit/history có old/new state, actor, timestamp, reason/reference.
- Guest token hash, scope, expiry/revocation và rate limit.
- Error code ổn định cho FE.
- Cache invalidation sau commit.

Đầu ra bắt buộc:

- Test request lặp, concurrent update và lỗi side effect sau commit.
- Không dùng cache/Camunda làm nguồn dữ liệu nghiệp vụ chính.

### PHẦN 04 — Catalog, cart và checkout

Trạng thái: `VERIFIED`.

Bàn giao: [04-catalog-checkout.md](implementation/04-catalog-checkout.md).

- Endpoint mới `/api/checkout` và endpoint Guest dùng `CheckoutService`, durable idempotency, DB cart lock và outbox trong cùng transaction.
- Order CATALOG luôn bắt đầu `PENDING_APPROVAL`/`NOT_DUE`; không mở MoMo, không chạy BPMN checkout legacy.
- Snapshot server gồm giá, tên, attributes, madeDay, duration và rule version; partial checkout chỉ xóa item được chọn.
- USER email + notification; Guest email có link token và thời lượng catalog, không tạo notification hệ thống.
- 198 test ứng dụng/H2 đạt (6 test MySQL opt-in skipped); 6/6 test MySQL 8.0.36 opt-in đạt riêng.

Nguồn: target F01–F04, WF02 phần tạo Order.

Phạm vi chính:

- Catalog là mẫu thiết kế của shop, không phải hàng có sẵn; mọi item checkout đều made-to-order và cần sản xuất.
- Một Order CATALOG có thể có nhiều OrderItem, mỗi item có quantity riêng lớn hơn hoặc bằng một.
- Availability `ACCEPTING_ORDERS`, `PAUSED`, `DISCONTINUED`.
- Availability chỉ bật/tắt nhận Order mới, không phải stock và không được dùng để bỏ qua sản xuất; Order đã tạo dùng snapshot và tiếp tục độc lập với thay đổi availability sau đó.
- Manager nhập `madeDay >= 2`; công thức thời lượng catalog và ngày hoàn thành theo từng OrderItem.
- Bỏ reserve/deduct/restore stock khỏi cart/checkout/order workflow.
- Cart USER/Guest và checkout một phần.
- Giá, contact, catalog spec và duration snapshot do server tạo.
- COD theo reputation; Guest chỉ COD.
- Voucher validation/quota nguyên tử.
- Checkout idempotent tạo Order `PENDING_APPROVAL`.
- Guest lookup token và email tạo đơn sau commit.

Đầu ra bắt buộc:

- Không tạo payment trước manager review.
- Không nhận giá catalog từ client.
- Không xóa item không được checkout.
- Test công thức catalog q=1/5/6/10 và lịch ngày nghỉ.

### PHẦN 05 — Custom request, WF01

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10. Code, API, migration, snapshot, email và tích hợp xuyên luồng đã được chấp nhận; ghi chú kiểm tra MySQL phía dưới phản ánh giới hạn ở thời điểm bàn giao ban đầu.

Phạm vi chính:

- USER tạo/sửa/xem nhiều CustomRequest draft.
- Guest không được tạo custom.
- Một Order CUSTOM có đúng một OrderItem/loại custom; quantity của item có thể lớn hơn một và không tạo thêm item/workflow theo từng chiếc.
- Draft ghi mới dùng `description` bắt buộc tối đa 4.000 từ và `imageUrls` là danh sách URL HTTP/HTTPS có thể rỗng; không dùng `attachments` dạng chuỗi làm API contract chính.
- Database và OrderItem snapshot dùng `LONGTEXT` để chứa description 4.000 từ UTF-8 cùng danh sách URL; migration giữ tên cột vật lý và converter hỗ trợ đọc dữ liệu legacy `spec/attachments`.
- Submit một draft tạo một Order CUSTOM `PENDING_APPROVAL` và snapshot dữ liệu.
- Chưa có giá chính thức trước khi staff xác nhận agreement.
- Submit idempotent và liên kết draft–Order.
- Email + notification chỉ khi Order được tạo, không gửi khi chỉ lưu draft.
- API `/api/custom-requests` chỉ nhận JWT có authority `USER`; owner luôn lấy từ JWT.
- Submit ghi Order, OrderItem, liên kết draft, kết quả durable request và một parent outbox `ORDER_CREATED` trong cùng transaction; consumer fan-out email/notification sau commit.
- Không gọi process `ApproveCartProcess` legacy. `ORDER_CREATED` đã được ghi bền vững; điểm bắt đầu manager-review của BPMN mục tiêu sẽ được nối khi cutover workflow ở PHẦN 13.

Đầu ra bắt buộc:

- API, authorization, migration và test theo UC01.
- Draft không bị biến thành Order nhiều lần.
- Boundary test description 4.000/4.001 từ, list URL rỗng/nhiều phần tử/URL sai, snapshot bất biến và tương thích dữ liệu legacy.

### PHẦN 06 — Nghiệp vụ hủy dùng chung, WF09

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10; các nhánh USER/Guest và tích hợp xuyên luồng đã được chấp nhận.

Bàn giao: [06-order-cancellation.md](implementation/06-order-cancellation.md).

Phạm vi chính:

- Một `CancelOrderService` dùng chung cho customer, manager reject và system source.
- USER/Guest chỉ tự hủy trước `ORDER_ACCEPTED`.
- Guest GET link chỉ mở trang xác nhận; mutation mới hủy.
- Penalty USER theo finalPrice, CUSTOM chưa có giá không bị trừ, Guest không có reputation.
- UserVoucher trả về ví không hoàn point cost.
- Guest voucher hoàn global quota nhưng giữ anti-spam usage.
- Release đúng active assignment một lần.
- Không hoàn stock, không tự refund và không hồi sinh Order bởi callback muộn.
- `ORDER_CANCELLED`, email/notification ngoại lệ và workflow cleanup qua outbox.

Đầu ra bắt buộc:

- Test mọi ngưỡng penalty, insufficient reputation, token, request lặp và các race chính.
- API/service có thể được WF02–WF04 gọi lại, không copy logic hủy.

### PHẦN 07 — Manager review và assignment, WF02

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10; API, assignment, quyền truy cập và tích hợp guest đã được chấp nhận.

Bàn giao: [07-manager-review-assignment.md](implementation/07-manager-review-assignment.md).

Phạm vi chính:

- Manager xem `PENDING_APPROVAL`, approve/reject có reason/version.
- Reject gọi PHẦN 06 với source `MANAGER_REJECTED`.
- Approve chuyển `PENDING_ASSIGNMENT` hoặc gán staff hợp lệ.
- Manager assign và staff claim dùng cùng service/ràng buộc DB.
- Một staff chỉ có một active assignment, một Order chỉ có một staff sản xuất.
- Khởi/tiếp tục process với business key gắn `orderId`.
- Guest CATALOG sau manager approval luôn phải qua assignment; không phân nhánh theo `handmade`, vì mọi catalog item đều handmade/made-to-order.

Đầu ra bắt buộc:

- Test hai manager, hai staff và manager/khách hủy cạnh tranh.
- Không có payment URL trước approve/agreement.

### PHẦN 08 — Chat, agreement và timer custom, WF03

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10 (backend và frontend).

Phạm vi chính:

- Service task tạo đúng một ChatThread theo Order sau assignment.
- Chỉ USER owner và assigned STAFF được đọc/gửi/subscribe; MANAGER không có quyền chat.
- Agreement version, form USER, staff confirm/request change.
- CATALOG giữ nguyên giá/duration snapshot.
- CUSTOM chốt spec, quantity, price, duration và voucher.
- Timer PT24H tuyệt đối từ `managerApprovedAt`; form hợp lệ dừng timer ngay.
- Timeout gọi CancelOrderService với `CUSTOM_CONFIRMATION_TIMEOUT`.

Đầu ra bắt buộc:

- Test chat retry, membership REST/WebSocket, version stale và submit–timeout race.
- Kết thúc bằng `ORDER_ACCEPTED` hoặc `CANCELLED` đúng nguồn.

### PHẦN 09 — Payment, webhook và staff bắt đầu, WF04

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10. Chủ dự án đã xác nhận test hoàn tất một đơn thanh toán online; các test tự động trước đó được liệt kê trong bàn giao PHẦN 09.

Phạm vi chính:

- Rẽ COD/ONLINE tại `ORDER_ACCEPTED`.
- ONLINE tạo PaymentAttempt từ finalPrice và timer PT1H từ `openedAt`.
- Webhook xác minh signature/reference/amount, xử lý lặp an toàn.
- Payment success chỉ ghi `PAID`, thông báo assigned staff và kết thúc wait payment.
- Timeout/fail gọi CancelOrderService, không trừ reputation.
- Staff chủ động bấm bắt đầu mới chuyển `ORDER_CREATING` và ghi `productionStartedAt`.
- Tính deadline riêng từng OrderItem; gửi lý do bắt đầu muộn nếu có.

Đầu ra bắt buộc:

- Test webhook–timer, callback muộn, start–cancel và start lặp.
- COD không có PaymentAttempt giả.

### PHẦN 10 — Production, checkpoint và KCS, WF05

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10 (backend, frontend và tích hợp xuyên luồng).

Phạm vi chính:

- Multi-instance theo OrderItem/loại, không theo quantity.
- `INITIAL_SHAPE` gửi thẳng báo cáo tiến trình, không chờ duyệt.
- `FINAL_PRODUCT` tạo attempt và manager KCS.
- Rework giữ toàn bộ ảnh/decision/history cũ.
- Chỉ khi mọi loại PASS mới chuyển `READY_TO_SHIP` và release staff.
- Email/notification checkpoint tách khỏi email trạng thái.

Đầu ra bắt buộc:

- Test nhiều loại, attempt, rework, hai KCS cuối cạnh tranh và release đúng một lần.

### PHẦN 11 — ChangeRequest, WF06

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10.

Phạm vi chính:

- USER đề nghị thay đổi chi tiết chưa triển khai.
- Assigned staff accept/reject theo version và progress hiện hành.
- Không đổi giá, voucher, payment, duration hoặc deadline.
- Không reset production, không chặn main flow toàn cục và không release staff.
- Có thể nối bằng non-interrupting event subprocess nếu cần.

Đầu ra bắt buộc:

- Test stale version, phần đã làm, field bị cấm và quyết định cạnh tranh.

### PHẦN 12 — Bàn giao, nhận hàng và reorder

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10 (backend và frontend).

Phạm vi WF07:

- Staff lịch sử phụ trách ghi `exportedQuantity` theo OrderItem.
- `READY_TO_SHIP → SHIPPING`, không release staff lần hai.
- Không tích hợp shipper, không email trạng thái SHIPPING.

Phạm vi WF08:

- USER dùng JWT/ownership; Guest dùng token đúng Order/scope.
- Ghi đủ `receivedQuantity`, so với `exportedQuantity`.
- Chênh lệch chưa chấp nhận giữ `SHIPPING`; khớp/chấp nhận lệch mới `DELIVERED`.
- USER cộng 2 reputation một lần; Guest không có reputation.
- CATALOG mở quyền/mời đánh giá; CUSTOM chưa có đánh giá.
- Không complaint workflow và không tự xác nhận bằng timer.

Phạm vi reorder F17:

- CATALOG thêm lại các item còn `ACCEPTING_ORDERS`, trả `addedItems/skippedItems`.
- CUSTOM tạo draft mới, không sao chép approval, payment hoặc quyền cũ.

Đầu ra bắt buộc:

- Test guest token, mismatch, idempotency nhận hàng, reputation và dữ liệu migrated.

### PHẦN 13 — Tích hợp xuyên luồng và rollout

Trạng thái: `VERIFIED` theo review chủ dự án ngày 28/10. Không bao gồm triển khai live; rehearsal database và rollout vẫn là checklist vận hành khi bắt đầu deploy.

Bàn giao: [13-integration-cutover-rollout.md](implementation/13-integration-cutover-rollout.md), [preflight SQL](implementation/sql/13-preflight.sql), [prompt frontend P13](prompts/P13_FRONTEND_ANTIGRAVITY.md).

Phạm vi chính:

- BPMN mục tiêu WF02–WF05 trên một lifecycle process/business key cho mỗi Order.
- Kết nối WF06–WF09 bằng event/subprocess/call activity khi cần, không tạo process cạnh tranh.
- Routing Redis Stream/outbox, retry, dead-letter/reconciliation và quan sát incident.
- Chỉ bốn email trạng thái bình thường: `PENDING_APPROVAL`, `ORDER_ACCEPTED`, `ORDER_CREATING`, `READY_TO_SHIP`.
- Email checkpoint và hủy là loại riêng; chống gửi trùng theo milestone.
- Cache invalidation và read model.
- Frontend state/action theo quyền và trạng thái mới.
- Xóa/vô hiệu hóa code inventory/complaint/workflow cũ sau khi migration đã an toàn.
- Test E2E toàn bộ nhánh chính, timeout, retry và race condition.
- Kế hoạch deploy, migrate, rollback và theo dõi sau release.

Đầu ra bắt buộc:

- Bộ test acceptance bám AC01–AC47 trong target.
- Không còn endpoint/BPMN cũ có thể đi vòng qua state transition mới.
- Migration thử thành công trên bản sao dữ liệu phù hợp trước production.

## 6. Definition of Done áp dụng cho mọi phần

Một phần chỉ được đánh dấu `VERIFIED` khi các mục liên quan đều hoàn thành:

- [ ] Đã xác định rõ phạm vi và file bị tác động.
- [ ] Có migration/schema nếu model thay đổi.
- [ ] Entity/enum/repository nhất quán.
- [ ] Application service giữ transaction và business rule.
- [ ] DTO/API validation và authorization hoàn chỉnh.
- [ ] BPMN/task/event được cập nhật nếu phần đó sử dụng workflow.
- [ ] Side effect sau commit có idempotency/retry phù hợp.
- [ ] Unit test và integration test liên quan đã chạy.
- [ ] Đã kiểm tra race/retry quan trọng của phần.
- [ ] Frontend contract/state được cập nhật hoặc bàn giao rõ.
- [ ] Tài liệu use case/target được cập nhật nếu quyết định nghiệp vụ thay đổi.
- [ ] Không làm mất hoặc ghi đè thay đổi ngoài phạm vi của người dùng.

## 7. Quy trình cập nhật file sau mỗi phiên làm việc

### Khi bắt đầu phiên

1. Đọc mục “Trạng thái hiện tại”.
2. Đọc phần đang `IN_PROGRESS` hoặc phần đầu tiên `NOT_STARTED` có dependency đã hoàn thành.
3. Chạy `git status --short` và phân biệt thay đổi có sẵn với thay đổi của phiên mới.
4. Kiểm tra test/bằng chứng của phiên trước, không tin trạng thái chỉ vì có file code.
5. Cập nhật phần đang làm sang `IN_PROGRESS` và ghi nhật ký bắt đầu.

### Trong phiên

- Chỉ sửa trong phạm vi phần hiện tại, trừ khi một thay đổi nền tảng đã được ghi rõ.
- Ghi ngay quyết định mới hoặc chặn phát hiện được.
- Không tự đặt policy còn thiếu; thêm vào mục cần làm rõ.
- Sau mỗi migration hoặc thay đổi API, ghi tương thích và rollback.

### Khi kết thúc phiên

1. Chạy test phù hợp và ghi chính xác command/kết quả.
2. Cập nhật checklist/đầu ra của phần.
3. Đặt trạng thái `IMPLEMENTED`, `VERIFIED` hoặc `BLOCKED` đúng thực tế.
4. Cập nhật “Trạng thái hiện tại” và “Hành động tiếp theo chính xác”.
5. Thêm một dòng vào nhật ký triển khai.
6. Ghi danh sách file đã thay đổi và phần việc còn dang dở.

## 8. Nhật ký triển khai

| Ngày giờ | Phần | Trạng thái sau phiên | Nội dung | Test/bằng chứng | Hành động tiếp theo |
|---|---:|---|---|---|---|
| 06/10/2026 | Chuẩn bị | — | Hoàn thành tài liệu WF01–WF09 và tạo kế hoạch 13 phần | Có đủ 6 file cho mỗi WF | Bắt đầu PHẦN 01 |
| 06/10/2026 | 01 | `IN_PROGRESS` | Bắt đầu chụp baseline, chưa thay đổi nghiệp vụ | Java 21.0.12, Maven 3.9.16; ghi nhận worktree có thay đổi cấu hình sẵn có | Chạy toàn bộ test hiện tại |
| 06/10/2026 | 01 | `VERIFIED` | Hoàn thành audit code/schema/API/BPMN/event và gap matrix WF01–WF09; không sửa nghiệp vụ | `.\mvnw.cmd test`: 167/167 đạt; [báo cáo PHẦN 01](implementation/01-baseline-gap.md) | Xác nhận quyết định domain/migration và bắt đầu PHẦN 02 |
| 06/10/2026 | 02 | `IN_PROGRESS` | Bắt đầu domain contract và migration theo chiến lược expand; chưa xóa dữ liệu legacy | Đề xuất ban đầu Flyway/VND nguyên, chưa triển khai | Kiểm kê schema/model và chờ quyết định kỹ thuật |
| 06/10/2026 | 02 | `IN_PROGRESS` | Người dùng chốt Liquibase và tiếp tục dùng `double/Double` cho tiền; giá custom chưa xác định dùng `null`, không dùng `0.0` | Quyết định mới nhất thay thế đề xuất Flyway/VND nguyên | Triển khai Liquibase changelog, domain skeleton và migration test |
| 06/10/2026 | 02 | `VERIFIED` | Hoàn thành domain contract và migration expand; giữ state/process/assignment legacy chờ cutover được kiểm thử | 178 tests, 0 failure/error/skipped, MySQL 8.0.36 + H2; [bàn giao](implementation/02-domain-migration.md) | PHẦN 03: transition, idempotency, outbox, token, audit |
| 06/10/2026 | 03 | `IN_PROGRESS` | Thêm durable request/outbox/consumer ledger, transition/audit, guest token guard; nối event publisher và consumer hiện tại | H2/MySQL concurrency và JPA transaction atomicity đang xác minh | Chạy bộ test đầy đủ và ghi bàn giao PHẦN 03 |
| 06/10/2026 | 03 | `VERIFIED` | Hoàn tất hạ tầng dùng chung và tích hợp event/cache consumer; cập nhật runbook và giới hạn SMTP/legacy | 187/187 test đạt; git diff --check đạt | PHẦN 04: catalog duration, cart/checkout, snapshots và bỏ stock |
| 07/10/2026 | 04 | `VERIFIED` | Checkout USER/Guest mới dùng DB idempotency/lock/outbox; snapshot catalog/duration, PENDING_APPROVAL và email milestone | 198 test H2/application đạt với 6 MySQL opt-in skipped; chạy riêng 6/6 MySQL 8.0.36 đạt; [bàn giao](implementation/04-catalog-checkout.md) | PHẦN 05: CustomRequest draft/submit |
| 07/10/2026 | 05 | `IN_PROGRESS` | Hoàn thiện checkpoint data layer: CustomRequest lifecycle/submittedAt, repository ownership + submit lock, DTO/mapper và mapper snapshot cho custom OrderItem | Targeted 12/12 đạt; full suite 206 test, 0 failure/error, 6 MySQL opt-in skipped; MySQL opt-in chưa chạy vì Docker daemon không sẵn sàng; [báo cáo review](implementation/05-custom-request.md) | Người dùng review data model; sau khi duyệt mới triển khai request DTO/service/controller/API |
| 07/10/2026 | 05 | `IMPLEMENTED` | Hoàn tất draft CRUD, JWT ownership, version conflict, submit CUSTOM Order PENDING_APPROVAL, snapshot/contact, durable idempotency và outbox email/notification/ORDER_CREATED | Full suite 215 test đạt, 6 MySQL opt-in skipped; transaction integration chứng minh retry chỉ có 1 Order/Item/request và 3 outbox event; [bàn giao](implementation/05-custom-request.md) | Review PHẦN 05; sau đó bắt đầu PHẦN 06 CancelOrderService dùng chung |
| 07/10/2026 | Nền tảng/refactor | `VERIFIED` | Tách việc xác định người dùng hiện tại khỏi `AuthService` sang một `CurrentUserService`; mọi service, cache key, rate limit và WebSocket dùng chung đầu mối này; giữ nguyên endpoint và nghiệp vụ | `.\mvnw.cmd test`: 220 test chạy, 0 failure/error, 6 MySQL opt-in skipped; `git diff --check` đạt | Tiếp tục tách các nghiệp vụ trùng lặp theo từng cụm service, bắt đầu với lifecycle/hủy Order khi PHẦN 05 được duyệt |
| 08/10/2026 | Nền tảng/refactor | `VERIFIED` | Hoàn tất gom toàn bộ nhóm hàm trùng mục đích đã audit: lookup user/order/consultation/product-variant; pageable/text/JSON/money/root-cause/session guest/commission key; checkout chỉ còn ở `CheckoutService`; bỏ service chuyển tiếp manager/staff; gom hoàn kho, hoàn voucher, dừng workflow, audit và event hủy vào `OrderCancellationService` cùng các service chuyên trách | `.\mvnw.cmd test`: 211 test chạy, 0 failure/error, 6 test opt-in skipped; rà tên hàm trùng chỉ còn overload hoặc cùng tên khác miền nghiệp vụ | Review refactor; sau đó tiếp tục PHẦN 06 để áp policy WF09 mới lên service hủy dùng chung |
| 08/10/2026 | Nền tảng/refactor | `VERIFIED` | Chuẩn hóa side effect vòng đời Order: checkout/custom submit chỉ phát parent event; handler tập trung tạo email/notification và cache read-model; hủy đơn dùng `ORDER_CANCELLED`; xác nhận thanh toán dùng `PAYMENT_CONFIRMED`; loại nguồn phát `GUEST_ORDER_CREATED` trùng trong BPMN/fallback. Cache cart được xóa trực tiếp sau checkout; Product/Products chỉ xóa khi quản trị catalog; Dashboard và BestSelling chỉ dùng TTL 20 phút | `.\mvnw.cmd test`: 216 test chạy, 0 failure/error, 6 test opt-in skipped; `git diff --check` đạt; rà mã xác nhận không còn invalidation Dashboard/BestSelling trong service nghiệp vụ | Review ranh giới handler/cache; sau đó tiếp tục PHẦN 06 theo kế hoạch |
| 08/10/2026 | Nền tảng/refactor mapper | `VERIFIED` | Chuẩn hóa toàn bộ MapStruct bằng strict config; tách mapper/assembler/factory; bỏ dựng DTO lặp trong service; đóng ranh giới Entity tại Chat/Voucher API; snapshot Order là nguồn đọc chính; loại producer Camunda trùng của `GUEST_ORDER_CREATED` | `.\mvnw.cmd test`: 218 test chạy, 0 failure/error, 6 test opt-in skipped; `.\mvnw.cmd -DskipTests compile` và `git diff --check` đạt | Review [bàn giao mapper](implementation/06-mapper-boundary-refactor.md); tiếp tục PHẦN 06 nghiệp vụ hủy dùng chung |
| 08/10/2026 | 06 | `IMPLEMENTED` | Hoàn tất kernel hủy Order dùng chung cho USER, Guest, manager reject và system timeout/failure; API mutation idempotent, Guest GET chỉ đọc, token scope/rate/version, penalty, hoàn voucher, release assignment và `ORDER_CANCELLED` outbox; loại hoàn stock/refund và xóa thô process khỏi đường hủy | `.\mvnw.cmd test`: 232 test chạy, 0 failure/error, 6 MySQL opt-in skipped; atomicity test chứng minh một transition audit và một `ORDER_CANCELLED`; [bàn giao](implementation/06-order-cancellation.md) | Review PHẦN 06; sau đó triển khai PHẦN 07 manager review và assignment dùng kernel hủy cho nhánh reject |
| 08/10/2026 | 07 | `IN_PROGRESS` | Bắt đầu chuẩn hóa manager review và assignment WF02; giữ database là nguồn nghiệp vụ, reject tái sử dụng kernel PHẦN 06 | Đang đối chiếu target/WF02 với API, service, repository và transition hiện tại | Triển khai contract review/assign/claim, khóa cạnh tranh và test nguyên tử |
| 08/10/2026 | 07 | `IMPLEMENTED` | Thay API review/assign/claim legacy bằng `ManagerOrderReviewService` và kernel `OrderAssignmentService`; actor từ JWT, version/idempotency, Order/staff lock, active unique, CUSTOM due +24h, Guest CATALOG sang ORDER_ACCEPTED và manager reject dùng PHẦN 06 | `.\mvnw.cmd test`: 244 test chạy, 0 failure/error, 6 MySQL opt-in skipped; migration legacy lặp đạt; atomicity test chứng minh một decision/assignment/transition; [bàn giao](implementation/07-manager-review-assignment.md) | Review PHẦN 07; sau đó triển khai PHẦN 08 chat, agreement và timer CUSTOM PT24H |
| 09/10/2026 | 05 | `IN_PROGRESS` | Người dùng chốt custom draft gồm description tối đa 4.000 từ và `imageUrls` dạng danh sách URL; rà soát xác nhận code hiện dùng `spec/attachments` String và schema `TEXT` nên chưa đạt contract mới | Cập nhật target, WF01 và báo cáo; chưa sửa code. Test review trước quyết định: 26 test PHẦN 05 đạt trên H2, Docker/MySQL không sẵn sàng | Sửa migration/entity/DTO/mapper/service/snapshot/test PHẦN 05 trước khi bắt đầu PHẦN 08 |
| 09/10/2026 | 05 | `IMPLEMENTED` | Hoàn tất API/domain `description` + `imageUrls`, giới hạn 4.000 từ, URL HTTP/HTTPS, migration LONGTEXT tương thích cột legacy, snapshot mới/fallback cũ, response `draftId/orderType`, link email và chặn user soft-delete | `.\mvnw.cmd test`: 254 test, 0 failure/error, 7 MySQL opt-in skipped; test đồng thời chỉ tạo 1 Order và fault injection xác nhận rollback toàn transaction; `git diff --check` đạt | Review PHẦN 05, chạy MySQL opt-in khi Docker sẵn sàng, sau đó bắt đầu PHẦN 08 |
| 09/10/2026 | 07 / F06 | `IMPLEMENTED` | Nghiệm thu lại manager review theo contract custom mới: chi tiết OrderItem trả typed `description/imageUrls` với fallback snapshot legacy; assignment mới chặn ghi đè cả `assignedStaff` lẫn `warehouseStaff` legacy | Targeted 14 test mapper/review/assignment/atomicity đạt; `.\mvnw.cmd test`: 256 test, 0 failure/error, 7 MySQL opt-in skipped | Review PHẦN 05 và PHẦN 07/F06, sau đó bắt đầu PHẦN 08 |
| 09/10/2026 | 07 | `IMPLEMENTED` | Hardening sau review: chặn MANAGER inactive/deleted; phát/consume `ORDER_ASSIGNED`; nối Guest `GUEST_ORDER_CREATED → manager task → assignment task`; email Guest ORDER_ACCEPTED có signed READ-only link và snapshot mẫu/giá/số lượng/thời lượng; signed token không xoay token checkout | `.\mvnw.cmd test`: 269 test, 0 failure/error, 7 MySQL opt-in skipped; concurrency thật xác nhận một winner cho cùng Order/cùng staff; BPMN engine test xác nhận review → assignment | PHẦN 08 consume `ORDER_ASSIGNED` để tạo ChatThread và timer CUSTOM; PHẦN 13 cutover BPMN USER/CUSTOM; production phải đặt `ORDER_LINK_SIGNING_SECRET` |
| 09/10/2026 | 07 | `IN_PROGRESS` | Rà soát domain xác nhận shop không có hàng làm sẵn: CATALOG và CUSTOM đều handmade/made-to-order; phát hiện Guest BPMN/handler còn dùng `hasHandmadeItems` để cho phép bỏ qua assignment. Kết luận trước đó về Guest BPMN hoàn tất chưa còn đủ | Đã cập nhật target/báo cáo; chưa sửa code ở checkpoint tài liệu này | Bỏ biến/gateway `hasHandmadeItems`, cho Guest CATALOG luôn chờ assignment; giữ tương thích process instance cũ khi đổi task key rồi chạy lại BPMN/full suite |
| 10/10/2026 | 07 | `IMPLEMENTED` | Đóng gap Guest made-to-order: bỏ tham số/process variable và gateway `hasHandmadeItems` khỏi service, runtime BPMN và blueprint; mọi Guest CATALOG được duyệt đi thẳng tới `Task_ManagerAssignOrder`. Bridge vẫn hoàn tất được task key legacy cho process instance đang chạy | Targeted 21 test đạt; `.\mvnw.cmd test`: 270 test, 0 failure/error, 7 MySQL opt-in skipped; test BPMN xác nhận biến legacy `hasHandmadeItems=false` không thể bỏ qua assignment; `git diff --check` đạt | Review PHẦN 07; sau đó bắt đầu PHẦN 08 chat, agreement và timer CUSTOM PT24H |
| 12/10/2026 | 07 / FE | `IMPLEMENTED` | Hoàn thiện UI Manager duyệt/phân công và Staff self-claim theo API P7; thêm read queue Manager, DTO queue có `orderVersion`, idempotency header, phân quyền MANAGER riêng và badge state mục tiêu | FE development build đạt; `.\mvnw.cmd test`: 273 test, 0 failure/error, 7 MySQL opt-in skipped; test queue xác nhận `PENDING_ASSIGNMENT` và version cho cả Manager/Staff | Review thủ công UI với dữ liệu tích hợp; sau đó bắt đầu PHẦN 08 |
| 12/10/2026 | 08 / scope cleanup | `IMPLEMENTED` | Theo quyết định scope mới, loại bỏ hoàn toàn chat tư vấn trước bán, attribution và hoa hồng; chỉ giữ order-chat. Thu gọn `ChatMessage`, gỡ API/service/entity/repository/Redis presence/event/cache/security hook legacy và thêm migration xóa schema SQL cũ có điều kiện | `.\mvnw.cmd test`: 281 test, 0 failure/error, 7 MySQL opt-in skipped; Liquibase H2 đạt; lệnh cleanup Mongo nằm trong `P8_CHAT_LATENCY_REFACTOR.md` | Backup dữ liệu, deploy Liquibase rồi chạy cleanup Mongo trên đúng database |
| 12/10/2026 | 09 | `IMPLEMENTED` | PaymentAttempt ONLINE, SePay webhook HMAC/idempotency, timeout PT1H, COD bypass và assigned staff chủ động bắt đầu sản xuất/deadline snapshot | Test webhook–timer, callback muộn, start–cancel/start lặp đạt; báo cáo `09-payment-and-production-start.md` | Antigravity triển khai frontend P9; E2E và BPMN cutover tổng ở P13 |
| 13/10/2026 | 10 | `IMPLEMENTED` | Checkpoint theo OrderItem, INITIAL_SHAPE publish thẳng, FINAL_PRODUCT attempt + manager KCS/rework, PASS toàn bộ mới READY_TO_SHIP/release; gỡ export/KCS legacy | `.\mvnw.cmd test`: 316 test, 0 failure/error, 9 opt-in skipped; test hai KCS cuối cạnh tranh xác nhận đúng một transition/release/event | Antigravity triển khai frontend P10; tiếp theo backend P11 ChangeRequest |
| 15/10/2026 | 10 / FE | `IMPLEMENTED` | Hoàn thiện UI Staff submit INITIAL/FINAL, Manager KCS/rework, USER/guest progress chỉ INITIAL; notification mở đúng order; retry giữ nguyên payload và idempotency key; gỡ lời gọi export/KCS legacy | `npm.cmd test -- --watch=false --browsers=ChromeHeadless`: 42/42; `npm.cmd run build:prod`: thành công, chỉ còn warning budget/CommonJS hiện hữu | Review/commit P9–P10; tiếp theo backend P11 ChangeRequest |
| 15/10/2026 | 11 / BE+FE | `IMPLEMENTED` | WF06 theo OrderItem: USER gửi proposed spec, assigned STAFF accept/reject, spec version/snapshot history, progress guard, whitelist payload, lock/version/idempotency, notification/outbox; UI dùng chung cho USER/STAFF | `./mvnw.cmd test`: 323 test, 0 failure/error, 9 opt-in skipped; `npm.cmd test -- --watch=false --browsers=ChromeHeadless`: 44/44; `npm.cmd run build:prod`: thành công (warning budget/CommonJS hiện hữu) | Review/commit P11; tiếp theo P12 bàn giao/nhận hàng/reorder |
| 16/10/2026 | 11 / BE hardening | `VERIFIED` | Chặn actor inactive/soft-delete, invalidation cache USER/assigned STAFF sau commit, thêm migration coverage cho status legacy và kiểm thử replay/race hai quyết định trái ngược | `.\mvnw.cmd test`: 329 test, 0 failure/error, 10 MySQL opt-in skipped; P11 service/cache targeted 12/12; `git diff --check` đạt | Dùng prompt P11 để Antigravity audit FE; sau đó review/commit và bắt đầu P12 |
| 16/10/2026 | 12 / BE | `IMPLEMENTED` | WF07 shipping handover theo OrderItem và staff lịch sử; WF08 receipt USER/Guest, mismatch/acceptMismatch, DELIVERED/reputation/review invitation; F17 catalog-to-cart/custom-to-draft; gỡ complaint legacy | `.\mvnw.cmd test`: 341 test, 0 failure/error, 11 opt-in skipped; targeted P12 11/11; `git diff --check` không có lỗi | Dùng prompt P12 triển khai FE; review/commit P12, sau đó bắt đầu P13 integration/cutover |
| 17/10/2026 | 13 / BE | `IMPLEMENTED` | Hợp nhất WF02–WF05 thành `OrderLifecycle` cho USER/Guest, nối event WF06–WF09, timer tuyệt đối, multi-instance theo OrderItem, Redis retry/DLQ + outbox operations; gỡ BPMN/checkout/MoMo/inventory workflow cũ và thêm migration 12/runbook/prompt FE | Full suite 317 test, 0 failure/error, 11 opt-in skipped; targeted P13 40/40; migration 12 lặp an toàn trên H2 MySQL mode; [bàn giao](implementation/13-integration-cutover-rollout.md) | Antigravity triển khai P13 FE, rehearsal backup/migration/restore trên bản sao MySQL rồi E2E AC01–AC47 |
| 28/10/2026 | Toàn dự án 01–13 | `VERIFIED` | Chủ dự án xác nhận review và chấp nhận toàn bộ phần còn lại; xác nhận đơn thanh toán online đã test thành công. Không tính deploy live | Bằng chứng test lịch sử giữ nguyên ở từng phần; lần cập nhật này không chạy thêm test | Không còn việc chức năng; deploy live thực hiện riêng khi chủ dự án sẵn sàng |

Khi thêm nhật ký, không xóa lịch sử cũ. Nếu một kết luận cũ không còn đúng, thêm dòng mới giải thích thay đổi.

## 9. Mẫu bàn giao cuối phiên

Sao chép và điền mẫu này vào cuối mục phần đang làm hoặc nhật ký chi tiết khi cần:

```text
PHẦN:
TRẠNG THÁI:
MỤC TIÊU ĐÃ ĐẠT:
FILE ĐÃ THAY ĐỔI:
MIGRATION:
API/BPMN/EVENT ĐÃ THAY ĐỔI:
TEST ĐÃ CHẠY:
KẾT QUẢ TEST:
QUYẾT ĐỊNH NGHIỆP VỤ MỚI:
RỦI RO/CÔNG VIỆC DANG DỞ:
HÀNH ĐỘNG TIẾP THEO CHÍNH XÁC:
```

## 10. Quyết định/chặn cần xử lý

Theo xác nhận review ngày 28/10 của chủ dự án, không còn blocker trong phạm vi chức năng được nghiệm thu. Các mục dưới đây được giữ làm lưu ý migration/rollout để đối chiếu khi deploy live:

- Công cụ migration database: **đã chốt Liquibase**. Hibernate dùng `validate` để không tự tạo cột trước changelog. Migration expand chỉ bật bằng profile `migration`, yêu cầu schema legacy có sẵn; bootstrap database trống chưa được hỗ trợ bởi changelog này.
- Chiến lược tương thích/migrate process instance Camunda đang chạy.
- Kiểu tiền: **đã chốt tiếp tục dùng `double/Double`**. Giá chính thức chưa xác định phải là `null`, không dùng `0.0` làm giá giả.
- Policy ánh xạ dữ liệu Order cũ sang state mới.
- TTL/revoke/cấp lại guest token.
- Policy tài chính cho payment success đến sau khi Order đã timeout và `CANCELLED`.

Các mục trên không được giải quyết bằng cách giữ nguyên hành vi code cũ nếu trái target.

## 11. Hiện trạng và lưu ý rollout

Các trạng thái và luồng nghiệp vụ dưới đây đã được chủ dự án review. Những lưu ý process/data legacy chỉ cần xử lý khi triển khai hoặc migrate hệ thống vận hành.

- `OrderStatus` đã có state mục tiêu nhưng vẫn giữ state kho/payment legacy trong giai đoạn expand; code mới không được tạo state legacy.
- `Order.totalPrice` còn là primitive `double`; các cờ stock đã bị loại trong PHẦN 13. Giá chính thức CUSTOM chưa chốt được biểu diễn bằng `finalPrice=null`; API không được diễn giải `totalPrice=0.0` tương thích schema thành giá thỏa thuận.
- Đường export/KCS legacy chuyển thẳng sang `SHIPPING` đã được gỡ ở PHẦN 10; PHẦN 12 đã bổ sung handover `READY_TO_SHIP → SHIPPING`. BPMN cutover tổng vẫn thuộc PHẦN 13.
- Complaint receipt legacy đã được gỡ ở PHẦN 12; receipt mới chỉ đối chiếu quantity và cho phép khách chủ động chấp nhận chênh lệch.
- Endpoint checkout mới tạo `PENDING_APPROVAL/NOT_DUE`; route `/api/cart/approve/{userId}` và MoMo legacy đã bị loại trong PHẦN 13.
- Order mới dùng duy nhất `OrderLifecycle`; process definition cũ đã bị gỡ khỏi deployment resource. Instance cũ cần kế hoạch drain/process migration riêng nếu production từng chạy chúng.
- PHẦN 05 đã dùng `description` và `List<String> imageUrls` ở API/domain, validate 4.000 từ và URL. Tên cột vật lý `spec/attachments` được giữ để tương thích nhưng đã nâng `LONGTEXT`; Agreement PHẦN 08 phải đọc snapshot mới và fallback snapshot legacy.
- Trước mọi lần sửa, phải giữ nguyên các thay đổi chưa commit không thuộc phần đang làm. Tại thời điểm tạo file này, worktree đã có thay đổi ở tài liệu use case và file cấu hình ứng dụng; cần chạy lại `git status` vì trạng thái này có thể thay đổi ở phiên sau.

## 12. Hành động tiếp theo chính xác

Không còn hạng mục chức năng đang mở theo xác nhận review của chủ dự án ngày 28/10. Các bước preflight, backup, migration rehearsal, rollout và theo dõi production thuộc phạm vi deploy live và chưa được tính trong trạng thái `VERIFIED` này.
