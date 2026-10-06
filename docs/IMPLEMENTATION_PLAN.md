# Kế hoạch và nhật ký tái triển khai hệ thống

Ngày tạo: 06/10/2026.

File này là **điểm tiếp tục công việc giữa các phiên làm việc**. Trước khi sửa code, người triển khai phải đọc file này, [target.txt](target.txt) và tài liệu workflow liên quan trong [use-cases](use-cases/README.md).

## 1. Trạng thái hiện tại

```text
Tài liệu nghiệp vụ:       HOÀN THÀNH WF01–WF09
Tái triển khai code:      CHƯA BẮT ĐẦU
Phần đang chờ thực hiện:  PHẦN 01 — Baseline và bảng chênh lệch
Phần hoàn thành gần nhất: Chưa có
Ngày cập nhật gần nhất:   06/10/2026
```

Không suy trạng thái hoàn thành chỉ từ đoạn code tồn tại. Trạng thái trong file này phải được cập nhật sau khi đã kiểm tra code, migration và test thực tế.

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
| `VERIFIED` | Đã đạt Definition of Done và kiểm tra liên quan |

Chỉ đánh dấu `VERIFIED` khi có bằng chứng test và tất cả đầu ra bắt buộc của phần đã hoàn thành.

## 4. Bản đồ 13 phần triển khai

| Phần | Phạm vi | Workflow/chức năng | Phụ thuộc | Trạng thái |
|---:|---|---|---|---|
| 01 | Baseline và bảng chênh lệch | Toàn hệ thống hiện tại | Không | `NOT_STARTED` |
| 02 | Domain contract và migration | State, entity, tiền, thời gian, dữ liệu mới | 01 | `NOT_STARTED` |
| 03 | Hạ tầng nhất quán dùng chung | Transition, lock/version, idempotency, outbox, token, audit | 02 | `NOT_STARTED` |
| 04 | Catalog, cart và checkout | Catalog, cart USER/Guest, đầu vào WF02 | 02–03 | `NOT_STARTED` |
| 05 | Custom request | WF01 | 02–04 | `NOT_STARTED` |
| 06 | Nghiệp vụ hủy dùng chung | WF09 kernel và API khách hủy | 02–03 | `NOT_STARTED` |
| 07 | Manager review và assignment | WF02, manager reject gọi WF09 | 04–06 | `NOT_STARTED` |
| 08 | Chat, agreement và timer custom | WF03, PT24H | 05–07 | `NOT_STARTED` |
| 09 | Payment và staff bắt đầu | WF04, PT1H, webhook, ORDER_CREATING | 03, 06–08 | `NOT_STARTED` |
| 10 | Production, checkpoint và KCS | WF05 | 07–09 | `NOT_STARTED` |
| 11 | ChangeRequest | WF06 | 08–10 | `NOT_STARTED` |
| 12 | Bàn giao, nhận hàng và reorder | WF07, WF08, F17 | 06, 10–11 | `NOT_STARTED` |
| 13 | Tích hợp xuyên luồng và rollout | BPMN, event, email, cache, FE, E2E, migration rollout | 01–12 | `NOT_STARTED` |

Thứ tự trên là thứ tự dependency, không phải thứ tự số workflow. WF09 được xây sớm vì manager reject, timer custom và payment timeout đều cần gọi cùng nghiệp vụ hủy.

## 5. Chi tiết từng phần

### PHẦN 01 — Baseline và bảng chênh lệch

Trạng thái: `NOT_STARTED`.

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

- Báo cáo baseline và gap matrix.
- Danh sách migration rủi ro cao.
- Kết quả test ban đầu.
- Phạm vi chính xác của PHẦN 02.

### PHẦN 02 — Domain contract và migration

Trạng thái: `NOT_STARTED`.

Mục tiêu:

- Chuẩn hóa model trước khi viết lại workflow.
- Có migration tiến/lùi hoặc chiến lược rollback rõ ràng, không làm mất dữ liệu cũ.

Phạm vi chính:

- `OrderStatus`: `PENDING_APPROVAL`, `PENDING_ASSIGNMENT`, `DISCUSSING`, `WAITING_STAFF_CONFIRMATION`, `ORDER_ACCEPTED`, `ORDER_CREATING`, `READY_TO_SHIP`, `SHIPPING`, `DELIVERED`, `CANCELLED`.
- Tách `paymentStatus` khỏi Order status và chuẩn hóa `paymentMethod`.
- Bổ sung `orderType`, version/optimistic locking, các timestamp và cancellation source/reference.
- Thay biểu diễn tiền `double` bằng kiểu chính xác đã chọn.
- Chuẩn hóa Order/OrderItem snapshot, `exportedQuantity`, `receivedQuantity`.
- Bổ sung/migrate Assignment, PaymentAttempt, CustomRequest, Agreement, ChangeRequest, receipt và lookup token theo thiết kế được chốt.
- Lập ánh xạ state cũ như `PENDING_WAREHOUSE`, `WAREHOUSE_ASSIGNED`, `PENDING_KCS`, `PENDING_PAYMENT` sang dữ liệu mới.
- Đánh dấu các trường stock/tracking/complaint cũ để loại bỏ sau migration, không xóa nóng khi chưa xác minh dữ liệu.

Đầu ra bắt buộc:

- Entity/enum mới và migration chạy được trên dữ liệu thử.
- Test mapping state/data cũ sang mới.
- Không có số tiền chính thức mặc định bằng 0 khi giá chưa được xác định.

### PHẦN 03 — Hạ tầng nhất quán dùng chung

Trạng thái: `NOT_STARTED`.

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

Trạng thái: `NOT_STARTED`.

Nguồn: target F01–F04, WF02 phần tạo Order.

Phạm vi chính:

- Availability `ACCEPTING_ORDERS`, `PAUSED`, `DISCONTINUED`.
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

Trạng thái: `NOT_STARTED`.

Phạm vi chính:

- USER tạo/sửa/xem nhiều CustomRequest draft.
- Guest không được tạo custom.
- Submit một draft tạo một Order CUSTOM `PENDING_APPROVAL` và snapshot dữ liệu.
- Chưa có giá chính thức trước khi staff xác nhận agreement.
- Submit idempotent và liên kết draft–Order.
- Email + notification chỉ khi Order được tạo, không gửi khi chỉ lưu draft.

Đầu ra bắt buộc:

- API, authorization, migration và test theo UC01.
- Draft không bị biến thành Order nhiều lần.

### PHẦN 06 — Nghiệp vụ hủy dùng chung, WF09

Trạng thái: `NOT_STARTED`.

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

Trạng thái: `NOT_STARTED`.

Phạm vi chính:

- Manager xem `PENDING_APPROVAL`, approve/reject có reason/version.
- Reject gọi PHẦN 06 với source `MANAGER_REJECTED`.
- Approve chuyển `PENDING_ASSIGNMENT` hoặc gán staff hợp lệ.
- Manager assign và staff claim dùng cùng service/ràng buộc DB.
- Một staff chỉ có một active assignment, một Order chỉ có một staff sản xuất.
- Khởi/tiếp tục process với business key gắn `orderId`.

Đầu ra bắt buộc:

- Test hai manager, hai staff và manager/khách hủy cạnh tranh.
- Không có payment URL trước approve/agreement.

### PHẦN 08 — Chat, agreement và timer custom, WF03

Trạng thái: `NOT_STARTED`.

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

Trạng thái: `NOT_STARTED`.

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

Trạng thái: `NOT_STARTED`.

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

Trạng thái: `NOT_STARTED`.

Phạm vi chính:

- USER đề nghị thay đổi chi tiết chưa triển khai.
- Assigned staff accept/reject theo version và progress hiện hành.
- Không đổi giá, voucher, payment, duration hoặc deadline.
- Không reset production, không chặn main flow toàn cục và không release staff.
- Có thể nối bằng non-interrupting event subprocess nếu cần.

Đầu ra bắt buộc:

- Test stale version, phần đã làm, field bị cấm và quyết định cạnh tranh.

### PHẦN 12 — Bàn giao, nhận hàng và reorder

Trạng thái: `NOT_STARTED`.

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

Trạng thái: `NOT_STARTED`.

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

- Bộ test acceptance bám AC01–AC42 trong target.
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

Hiện chưa có blocker bắt đầu PHẦN 01. Các lựa chọn kỹ thuật cần xác nhận bằng audit trong PHẦN 01–02:

- Công cụ migration database sẽ dùng.
- Chiến lược tương thích/migrate process instance Camunda đang chạy.
- Kiểu tiền chính xác dùng cho VND và cách migrate các cột `double`.
- Policy ánh xạ dữ liệu Order cũ sang state mới.
- TTL/revoke/cấp lại guest token.
- Policy tài chính cho payment success đến sau khi Order đã timeout và `CANCELLED`.

Các mục trên không được giải quyết bằng cách giữ nguyên hành vi code cũ nếu trái target.

## 11. Hiện trạng cần đặc biệt lưu ý trước khi bắt đầu

- `OrderStatus` hiện vẫn mang mô hình kho cũ và thiếu state mục tiêu.
- `Order` còn dùng `double`, `finalPrice` mặc định 0 và các cờ stock.
- KCS hiện có đường chuyển trực tiếp sang `SHIPPING`, thiếu `READY_TO_SHIP` riêng.
- Code còn complaint receipt dù target đã loại bỏ complaint workflow.
- Checkout/payment hiện có thứ tự không còn phù hợp với manager-review-before-payment.
- Repo có nhiều BPMN cho user/guest; cần tránh duy trì hai lifecycle process cạnh tranh cho cùng Order.
- Trước mọi lần sửa, phải giữ nguyên các thay đổi chưa commit không thuộc phần đang làm. Tại thời điểm tạo file này, worktree đã có thay đổi ở tài liệu use case và file cấu hình ứng dụng; cần chạy lại `git status` vì trạng thái này có thể thay đổi ở phiên sau.

## 12. Hành động tiếp theo chính xác

Tiếp theo thực hiện **PHẦN 01 — Baseline và bảng chênh lệch**:

1. Chạy test suite hiện tại mà chưa sửa nghiệp vụ.
2. Ghi các test đang pass/fail và nguyên nhân đã biết.
3. Lập danh sách endpoint/entity/state/BPMN hiện tại.
4. Tạo gap matrix ánh xạ từng yêu cầu target sang code cần giữ/sửa/thêm/bỏ.
5. Cập nhật file này trước khi chuyển sang PHẦN 02.
