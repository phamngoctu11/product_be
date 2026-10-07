# PHẦN 02 — Domain contract và Liquibase expand migration

Ngày thực hiện: 06/10/2026. Quyết định của người dùng: Liquibase và tiền `double/Double`.

## Phạm vi triển khai

- Thêm các state lifecycle mới, giữ bốn state legacy để code/process cũ còn đọc được dữ liệu.
- Order có optimistic version, type, payment status/method chuẩn hóa, assigned staff, mốc thời gian, cancellation source/reference và metadata guest token.
- Giữ `double/Double`; `Order.finalPrice`, `OrderItem.price` và DTO tương ứng cho phép `null`. Mapper giữ `null` cho giá CUSTOM chưa chốt và giữ `0.0` khi giá chính thức bằng không.
- Thêm CustomRequest, OrderAgreement, OrderAssignment, PaymentAttempt, ChangeRequest, OrderReceipt và repository. Các model này là contract cho những phần workflow tiếp theo, chưa có API tạo/sửa tương ứng.
- Assignment có lịch sử riêng và hai khóa active nullable, unique theo Order và staff. CHECK buộc khóa active khớp tham chiếu khi đang làm; release phải xóa khóa active và ghi lý do/thời điểm. Vì vậy staff có thể nhận đơn khác sau release mà không mất lịch sử.
- PaymentAttempt có unique provider/reference và provider/transaction ID; giữ cờ reconciliation cho kết quả cần đối soát. Không tự suy diễn payment success từ SHIPPING/DELIVERED.
- Catalog thêm madeDay. Manager tạo/cập nhật phải nhập số hữu hạn >= 2. Dữ liệu catalog cũ để null chờ manager bổ sung, không tự tạo cam kết 2 ngày. Staff basic-info không sửa madeDay.
- OrderItem có snapshot spec, catalog names, madeDay, duration/rule version và deadline. Công thức, lịch sản xuất và việc chụp snapshot vào checkout thuộc PHẦN 04/09.

## Cách áp dụng migration

Changelog `src/main/resources/db/changelog/db.changelog-master.xml` là migration **expand cho database legacy đã tồn tại**. Nó không phải bộ bootstrap database trống. Precondition thiếu bảng legacy sẽ dừng migration. Không dùng `changelogSync` hoặc MARK_RAN để bỏ qua xung đột schema.

Changelog cũng chuyển `orders.status`, `orderhistory.oldstatus/newstatus` từ ENUM legacy sang VARCHAR để chấp nhận state mới. Test MySQL khởi tạo đúng ENUM cũ và xác nhận có thể ghi ORDER_ACCEPTED/ORDER_CREATING sau migration.

Hibernate chuyển sang `ddl-auto=validate` trong cấu hình chung/dev/prod. Liquibase mặc định tắt; bật profile `migration` để chạy có chủ đích. Cấu hình datasource có sẵn của người dùng được giữ nguyên. Không khởi động phiên bản entity mới với Hibernate `update`, vì Hibernate có thể tạo cột trước changelog, khiến migration sau đó xung đột.

Quy trình trên bản sao database:

1. Dừng ghi ứng dụng/worker trên bản sao; lưu schema và backup trước migration.
2. Chạy [02-preflight.sql](sql/02-preflight.sql); kiểm kê process instance Camunda đang chờ và assignment xung đột.
3. Cấp `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trỏ tới bản sao; không dùng mặc định cấu hình khi kiểm thử migration.
4. Khởi động với profile `dev,migration` (đặt migration sau dev để cấu hình có hiệu lực): `SPRING_PROFILES_ACTIVE=dev,migration`. Liquibase chạy trước JPA validate.
5. Đối chiếu counts, giá, state, history và schema; bổ sung madeDay thật cho catalog cũ qua manager.
6. Sau khi schema đã cập nhật, ứng dụng có thể dùng profile dev/prod với Hibernate validate. Liquibase có thể bật lại: changeset đã thành công sẽ không chạy lại.

Test tự động chỉ kết nối H2 hoặc container MySQL riêng; phiên triển khai này không chạy migration lên datasource cấu hình của người dùng.

## Backfill và tương thích

Backfill tự động chỉ thực hiện những dữ liệu xác định được:

- Order legacy là CATALOG; item có variant là CATALOG.
- Chuẩn hóa chuỗi COD/ONLINE sang trường method mới; chuỗi không hợp lệ để null.
- COD → NOT_DUE. Legacy PENDING_PAYMENT + ONLINE → PENDING.
- ONLINE ở trạng thái khác để paymentStatus null cho đến khi đối soát; không suy ra PAID.
- Version = 0, expectedShippingDays = 2; giữ nguyên giá, số lượng, state, staff cũ và process instance.

`LegacyOrderStateMapper` là công cụ đánh giá, không ghi DB. PENDING_WAREHOUSE gợi ý PENDING_ASSIGNMENT nhưng vẫn yêu cầu kiểm tra approval/process. WAREHOUSE_ASSIGNED/PENDING_KCS/PENDING_PAYMENT cần kiểm tra bổ sung; không tự chuyển sang sản xuất hoặc ORDER_ACCEPTED.

Assignment cũ theo item chưa được chuyển tự động thành active assignment theo Order. Trước khi bật WF02 mới phải giải quyết dữ liệu một Order nhiều staff và một staff nhiều Order. Process cũ tiếp tục giữ definition cũ; chuyển instance là công việc rollout có kiểm thử riêng.

Các trường stock/tracking, OrderProductionStatus, OrderItemAssignment và API complaint vẫn là legacy chờ loại bỏ tại bước rollout. Changelog không drop dữ liệu này.

## Rollback

Changelog chỉ mở rộng và backfill dữ liệu xác định; không cung cấp rollback tự động drop bảng nghiệp vụ mới. MySQL DDL có implicit commit, nên lỗi giữa migration có thể để lại một số changeset đã áp dụng.

- Trước cutover: khôi phục backup bản sao/production và binary tương ứng nếu migration thất bại; hoặc sửa bằng changeset bổ sung đã kiểm thử khi xác định được trạng thái.
- Khi chỉ rollback binary và chưa có workflow mới ghi dữ liệu: giữ cột/bảng expand, kiểm tra bản cũ còn đọc được legacy state; không chạy bản cũ nếu đã có state mới mà enum cũ không hỗ trợ.
- Sau khi có dữ liệu workflow mới: không rollback bằng cách xóa bảng/cột. Cần restore có đối soát hoặc forward-fix để giữ payment/audit/assignment.

## Kiểm thử và giới hạn

Kết quả thực tế ngày 06/10/2026: **178 test, 0 failure, 0 error, 0 skipped — BUILD SUCCESS**, bao gồm 3 case migration kế thừa chạy trên MySQL 8.0.36 và 3 case trên H2. Lệnh toàn bộ: `.\mvnw.cmd test '-DmysqlMigrationTests=true' '-Dapi.version=1.44'`. Thời gian lần chạy cuối: 1 phút 34 giây. Lần chạy đầu gặp lỗi tương thích Docker API; tham số API ở lệnh trên đã khắc phục và toàn bộ test được chạy lại thành công.

Các test migration dùng schema legacy tối thiểu có dữ liệu mẫu, kiểm tra giữ giá/state, null duration, nullable custom price, chạy lặp, unique active staff và unique transaction nhà cung cấp. H2 test luôn chạy; MySQL test bật bằng:

```powershell
.\mvnw.cmd test '-Dtest=MySqlDomainExpandMigrationTest' '-DmysqlMigrationTests=true' '-Dapi.version=1.44'
```

MySQL test cần Docker, sử dụng image mysql:8.0.36 và database container dùng một lần. `api.version=1.44` là tham số tương thích Docker 29 trên môi trường phát triển này.

Test schema tối thiểu không thay thế diễn tập migration trên bản sao toàn bộ dữ liệu vận hành. Changelog này chưa quản lý tạo mới toàn bộ schema legacy hay Camunda engine tables. Chưa bật workflow mới, tự migrate process đang chạy hoặc quyết định chính sách hoàn tiền callback muộn.

## Bàn giao cho các phần tiếp theo

- PHẦN 03: transition/lock, outbox/idempotency, token scope/expiry/revoke và audit. Đọc paymentStatus null như dữ liệu legacy chưa đối soát.
- PHẦN 04: checkout mới phải điền NOT_DUE, paymentMethodType, sourceType và snapshots; từ chối catalog chưa có madeDay. Không dùng model giá null làm giá payment.
- PHẦN 07: trước khi bật assignment mới trên dữ liệu cũ, kiểm tra báo cáo staff conflict, chuyển assignment đã xác minh; không coi absence trong bảng mới là staff legacy rảnh.
- PHẦN 13: diễn tập trên clone đầy đủ, quyết định cutover/process migration và chính sách tài chính callback muộn. Bootstrap schema trống là công việc bổ sung nếu môi trường rollout cần.
