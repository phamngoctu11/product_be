# PHẦN 03 — Hạ tầng nhất quán dùng chung

Ngày triển khai: 06/10/2026. Kế thừa Liquibase và double/Double đã chốt ở PHẦN 02.

## Thành phần và trách nhiệm

| Thành phần | Hành vi |
|---|---|
| DurableRequestExecutor | Khóa request trong DB; cùng scope/key/payload trả kết quả đã lưu; khác payload trả 409; operation và kết quả commit cùng transaction |
| OutboxStore / DomainEventPublisher | Ghi event vào DB trong transaction nghiệp vụ; giữ API publish/publishAfterCommit cho các caller hiện tại |
| OutboxDispatcher | Khóa từng event, ghi Redis Stream, đánh dấu published hoặc lưu attempt/due date để retry; eventId không đổi |
| Durable consumer ledger | Consumer dùng DurableRequestExecutor theo group/eventId; bỏ qua event đã hoàn tất; guest-order-created có khóa milestone theo orderId |
| OutboxRecoveryService | Requeue theo eventId cũ để đối soát/sửa sự kiện chưa xử lý; đây là hàm nội bộ, chưa mở endpoint quản trị |
| OrderTransitionPolicy | Đồ thị state hợp lệ và quy tắc nguồn hủy; state CANCELLED không quay lại hoạt động; customer chỉ hủy trước accepted |
| OrderTransitionService | Bắt buộc transaction ngoài, khóa Order, kiểm tra version, ghi status/timestamp/audit và sự kiện invalidate cache cùng transaction |
| OrderLookupTokenService | Hash, scope, expiry, revoke và rotate; token mới thay hash nên token trước mất hiệu lực |
| GuestOrderAccessGuard | Rate limit theo orderId/action, fail-closed khi Redis lỗi, sau đó kiểm tra token/scope |

## Contract cho workflow ở các phần sau

Application service phải xác thực actor/ownership trước khi đọc kết quả idempotency. Scope phải được backend tạo từ danh tính + action, key là request key của client, payload phải là JSON canonical đã validate (cùng nội dung phải cho cùng chuỗi, không phụ thuộc thứ tự thuộc tính).

Gọi DurableRequestExecutor để mở transaction; trong operation thực hiện điều kiện nghiệp vụ, assignment/voucher/payment, rồi OrderTransitionService và ghi các event cần thiết. Lưu response JSON thành result. Khi retry cùng key, operation không chạy lại và timestamp/version không tăng thêm.

OrderTransitionService chỉ là hạ tầng chuyển trạng thái, không thay thế authorization và điều kiện chuyên biệt của WF02–WF09: manager được phép duyệt, staff available, agreement hợp lệ, ONLINE đã PAID, tất cả checkpoint PASS, release assignment, hoàn voucher… phải nằm trong cùng transaction tại application service tương ứng. Không expose một API nhận state tùy ý từ client. Legacy OrderService/BPMN chưa được chuyển toàn bộ sang service này; việc nối từng WF thuộc các phần tiếp theo.

Các state legacy không nằm trong đồ thị mới. Hủy SYSTEM chung bị từ chối; nguồn hủy mới phải thêm policy rõ. CUSTOM_CONFIRMATION_TIMEOUT không áp dụng sau WAITING_STAFF_CONFIRMATION vì form đã gửi sẽ dừng timer.

Audit mới lưu trong workflow_transition_audit: old/new state, actor, reason/reference, thời điểm. Read API history sẽ ghép/đọc bảng này khi chuyển workflow; bảng orderhistory legacy được giữ nguyên.

## Outbox, retry và consumer

DomainEventPublisher giờ lưu outbox trước commit. Nếu không có transaction ngoài, OutboxStore mở transaction riêng để lưu event. Nếu có transaction, rollback nghiệp vụ sẽ rollback event. Các caller đang phát sau khi transaction nghiệp vụ đã kết thúc chưa có tính nguyên tử với transaction trước đó; phải chuyển vào application service khi triển khai workflow tương ứng.

Dispatcher chạy batch tối đa 20 event, mặc định mỗi giây. Khi gửi lỗi, tăng attempts, lưu tên loại lỗi (không lưu secret/provider payload trong last_error), lùi due_at theo backoff và tiếp tục retry. Cờ `workflow.events.redis-stream.enabled=false` tắt transport nhưng vẫn giữ outbox để gửi khi bật lại.

Dispatcher giữ DB row lock trong lần gửi Redis; cần timeout Redis có giới hạn. Nếu Redis đã nhận nhưng DB chưa đánh dấu published, retry có thể phát lại cùng eventId. Consumer ledger trong DB ngăn chạy lại tác vụ đã commit. Request/result/outbox cùng DB, Redis và Camunda không phải nguồn dữ liệu nghiệp vụ chính.

Consumer không ACK event không hỗ trợ; lỗi đi theo cơ chế retry/DLQ sẵn có. Sự kiện lỗi cache phải throw để được retry, không ghi ledger completed khi cache lỗi. Commission refresh thực hiện trong transaction consumer; realtime notification chỉ đẩy sau commit, notification vẫn được lưu để client đọc lại.

Giới hạn: SMTP/WebSocket là hệ thống ngoài transaction database. Nếu SMTP đã gửi nhưng process chết trước commit ledger, lần retry có thể gửi lại email. Không cam kết exactly-once SMTP; cần nhà cung cấp hỗ trợ idempotency key hoặc giải pháp đối soát delivery riêng để loại bỏ cửa sổ này. Realtime là best-effort sau commit, API notification là nguồn đọc bền vững.

## Guest token và lỗi API

- `issueFor(order, scopes, validity)` yêu cầu thời hạn dương được truyền rõ. Không đặt TTL nghiệp vụ mặc định trong PHẦN 03.
- `authorize` từ chối scope không khớp, expiry thiếu/quá hạn, token đã revoke, token cũ sau rotation, hash không khớp.
- `issueFor(order)` cũ giữ tương thích tạo token; token legacy thiếu scope/expiry **không được** authorize qua guard mới. Endpoint guest mới phải dùng overload đầy đủ và lưu Order trong transaction.
- Guard chỉ dùng cho guest Order và rate limit theo orderId/action; thay token/session không đổi khóa rate limit.
- Rate budget/window và TTL cần được chốt ở phần API guest. Scope token không thay điều kiện state: CANCEL token còn hạn vẫn bị từ chối nếu Order đã accepted.
- AppException trả `X-Error-Code`, CORS expose header này. Request key khác nội dung, stale version hoặc concurrent update trả 409; rate limit trả 429/Retry-After, Redis rate-limit lỗi trả 503.

## Migration và vận hành

Master changelog thêm 03-consistency.xml, tạo workflow_requests, workflow_outbox, workflow_transition_audit. Áp dụng cùng profile migration như PHẦN 02 trước khi chạy bản code này. Không sửa checksum changeset 02 đã triển khai.

Kiểm tra queue bằng SQL read-only:

```sql
SELECT event_id, event_type, attempts, due_at, last_error
FROM workflow_outbox WHERE published_at IS NULL ORDER BY due_at;
SELECT order_id, old_status, new_status, actor_id, reference_id, occurred_at
FROM workflow_transition_audit ORDER BY occurred_at DESC;
```

Không xóa workflow_requests để chữa retry: xóa ledger có thể chạy lại tác vụ đã hoàn thành. Không sửa payload/hash của key cũ. Với sự kiện cần phát lại, OutboxRecoveryService.requeue giữ nguyên eventId/payload. Consumer đã completed sẽ bỏ qua; event chưa hoàn tất được thử lại. Hệ thống chưa có API/admin UI cho thao tác recovery này.

Rollback binary giữ nguyên bảng mới; không rollback bằng drop bảng có event/result chưa đối soát. Phải dừng worker và đối soát queue/ledger trước khi quay lại consumer cũ vì consumer cũ không đọc durable ledger, có thể xử lý trùng. Dữ liệu retention, claim pending của consumer chết và diễn tập khôi phục Redis toàn cụm sẽ được hoàn thiện trong rollout PHẦN 13.

## Bằng chứng kiểm thử

Kết quả ngày 06/10/2026: **187 tests, 0 failure, 0 error, 0 skipped — BUILD SUCCESS**, thời gian 1 phút 55 giây. Lượt cuối đã chạy lại đầy đủ sau khi sửa test MySQL phụ thuộc thời điểm due_at; test nay đặt rõ mốc đến hạn để kiểm tra nhánh retry một cách xác định. `git diff --check` đạt.

- H2 và MySQL: request rollback, retry trả response cũ, same key khác payload, hai transaction đồng thời chỉ tạo một tác vụ, Redis lỗi còn event retry và eventId/payload giữ nguyên.
- JPA integration: trạng thái Order, optimistic version, audit, request result và cache outbox cùng commit/rollback; stale version bị từ chối.
- Token: scope, expiry, revoke, rotate và legacy thiếu metadata bị từ chối ở authorization mới.
- Transition: không hồi sinh CANCELLED, khách không hủy ORDER_ACCEPTED, timer custom không hủy sau khi form đã gửi.

Lệnh đầy đủ: `.\mvnw.cmd test '-DmysqlMigrationTests=true' '-Dapi.version=1.44'`.
