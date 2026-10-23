# Báo cáo kiểm thử hệ thống theo `docs/target.txt`

- Ngày kiểm thử: 21/10/2026 (Asia/Saigon)
- Phạm vi: backend `product_be`, frontend `product_fe`, MySQL, MongoDB, Redis, Keycloak, Camunda, WebSocket và mail service local
- Tài khoản: `admin` (manager), `user04` (user), `staff04` (staff), guest không đăng nhập
- Thanh toán online: không chuyển tiền thật theo yêu cầu; chỉ đối chiếu regression/contract hiện có

## 1. Kết luận nhanh

Hệ thống đã chạy được hai luồng nghiệp vụ cốt lõi qua giao diện thật:

1. Guest chọn mẫu catalog → thêm giỏ → đặt COD → manager duyệt → staff claim → bắt đầu sản xuất → checkpoint 1 → checkpoint 2 → manager KCS → bàn giao vận chuyển → guest xác nhận nhận hàng (`DELIVERED`).
2. User tạo custom request → manager duyệt → staff claim → chat WebSocket → user gửi thỏa thuận COD → staff chốt → bắt đầu sản xuất.

Chat sau tối ưu đạt khoảng **90–368 ms** từ lúc bấm gửi đến khi tin nhắn xuất hiện ở trình duyệt nhận/echo trong môi trường local. Kết quả này tốt hơn mức 400–500 ms đã phản ánh trước đó.

Chưa nên coi bản hiện tại là “đạt toàn bộ target để release” vì còn các lỗi/mismatch quan trọng:

- Manager có thể nhận `409 optimistic lock` ngay lần duyệt đầu sau guest checkout và phải tải lại.
- Form custom mới tạo không gửi được ngay ở bước 2 cho đến khi tải lại URL draft.
- Frontend còn dùng trạng thái legacy `COMPLETED`, trái với target chỉ dùng `DELIVERED` cho hoàn tất giao nhận.
- API quản lý catalog còn nhận `userId` do client truyền, trái C3 (actor phải lấy từ JWT).
- Tiền vẫn dùng `Double`/`double`, chưa phải biểu diễn chính xác theo C3.
- Form thỏa thuận custom tự đặt giới hạn 10 ảnh dù target yêu cầu chưa cố định chính sách số ảnh.

## 2. Môi trường và regression

Các service local tại thời điểm kiểm thử:

| Service | Kết quả |
|---|---|
| Frontend `localhost:4200` | Hoạt động |
| Backend `localhost:8080` | Hoạt động |
| Keycloak `localhost:8180` | Hoạt động |
| Redis `localhost:6379` | Hoạt động |
| MongoDB `localhost:27017` | Hoạt động |
| MySQL theo cấu hình local | Hoạt động qua toàn bộ luồng E2E |
| Mail service | User xác nhận đã gửi thành công; E2E không đăng nhập inbox để kiểm chứng nội dung từng mail |

Regression chạy lại từ source hiện tại:

| Bộ kiểm thử | Kết quả |
|---|---|
| Backend `mvnw test` | **317 test**, 0 failure, 0 error, **11 skipped**, BUILD SUCCESS |
| Frontend Karma/ChromeHeadless | **123/123 SUCCESS** |
| Frontend development build | Thành công |

11 test bị skip là các test MySQL opt-in (`MySqlConsistencyIntegrationTest`: 3, `MySqlDomainExpandMigrationTest`: 8). Vì vậy các race/migration đặc thù MySQL chưa được tính là đã chạy trong regression mặc định.

## 3. Kết quả sử dụng thực tế theo vai trò

### Guest/catalog — đơn kiểm thử #18

| Thao tác | Kết quả | Thời gian quan sát |
|---|---|---:|
| Xem catalog công khai, nhận biết made-to-order | PASS | — |
| Chọn variant và thêm giỏ | PASS | 687 ms |
| Đặt COD, nhận order ID và lookup token | PASS | 726 ms |
| Manager duyệt sau khi tải lại | PASS | 2.758 ms |
| Staff tự claim | PASS | 1.932 ms |
| Staff bắt đầu sản xuất | PASS | 1.551 ms |
| Nộp INITIAL_SHAPE kèm ảnh | PASS | 3.542 ms |
| Nộp FINAL_PRODUCT kèm ảnh | PASS | 5.469 ms |
| Manager mở KCS | PASS | 453 ms |
| Manager duyệt đạt KCS | PASS | 1 item; đơn chuyển `READY_TO_SHIP` |
| Staff bàn giao vận chuyển | PASS | 1.510 ms |
| Guest mở link token và xác nhận số lượng nhận | PASS | 1.230 ms; đơn chuyển `DELIVERED` |

Upload checkpoint chậm hơn các thao tác trạng thái vì có thêm upload ảnh lên Cloudinary. Đây không phải độ trễ chat.

### User/custom — draft #8, đơn #19

| Thao tác | Kết quả | Thời gian quan sát |
|---|---|---:|
| Tạo/lưu custom draft | PASS | 0,6–0,9 giây |
| Submit draft sau khi tải lại URL | PASS | 2.069 ms |
| Manager duyệt custom | PASS | 2.084 ms |
| Staff claim custom | PASS | 1.927 ms |
| User gửi chat và nhận echo WebSocket | PASS | 111 ms (một lượt trước đó 368 ms) |
| User gửi thỏa thuận COD | PASS | 1.230 ms |
| Staff chốt thỏa thuận | PASS | 1.983 ms |
| Staff bắt đầu sản xuất custom | PASS | 1.823 ms |
| Staff gửi, user nhận chat ở browser khác | PASS | 90 ms |

Chat thread lúc mới claim trả `404` ngắn hạn vài lần rồi tự retry và hoạt động. UI có trạng thái “đang chuẩn bị kênh”, nên người dùng vẫn hiểu được tiến trình; sau khi thread sẵn sàng không còn lỗi gửi/nhận.

### Phân quyền và vận hành

- PASS: `user04` bị chặn khi truy cập trực tiếp route manager.
- PASS: guest bị chuyển sang đăng nhập khi mở lịch sử đơn của user.
- PASS: manager mở được dashboard.
- PASS: manager mở được màn giám sát workflow, outbox và DLQ; các request tải dữ liệu trả 200.
- PASS: staff bận không claim được đơn thứ hai (409 đúng nghiệp vụ); sau KCS cuối cùng, assignment được release và staff claim được đơn mới.
- Chưa xác nhận thao tác wishlist bằng E2E: trang/catalog hoạt động nhưng runner không tìm thấy control wishlist khả dụng trong phiên này. Unit test frontend vẫn pass.

## 4. Lỗi và mismatch cần xử lý

### P1 — Manager duyệt đơn mới có thể lỗi 409 ngay lần đầu

- Tái hiện ở đơn #17 và #18 ngay sau guest checkout.
- Request thật: `POST /api/orders/manager/reviews/{orderId}` trả 409 với thông báo dữ liệu bị cập nhật đồng thời.
- Tải lại màn rồi duyệt lại thì thành công.
- Trải nghiệm hiện tại khiến manager tưởng thao tác thất bại dù không có người khác sửa đơn.

Khuyến nghị:

- Kiểm tra consumer/outbox khởi tạo workflow có đang tăng `orders.version` sau khi hàng đợi review đã trả DTO cũ hay không.
- Không tăng optimistic version cho metadata không ảnh hưởng quyết định review, hoặc trả phiên bản mới nhất sau event.
- FE nên tự reload DTO và cho retry rõ ràng; không tự retry mutation bằng idempotency key mới nếu chưa biết request đầu đã commit hay chưa.

### P1 — Custom draft mới tạo không submit ngay nếu chưa reload

- Tái hiện liên tiếp với draft #4–#8: sau “Lưu bản nháp”, URL đổi sang `/store/custom-requests/{id}`, form bước 2 hiển thị hợp lệ và nút không disabled nhưng bấm không phát request submit.
- Reload đúng URL draft rồi bấm lại: confirm hiện ra và request thành công.
- Nghi ngờ vòng đời component dùng `route.snapshot.paramMap` trong `ngOnInit` trong khi điều hướng từ `new` sang `:id` giữ cùng component; cần kiểm tra lại state/form/subscription sau `router.navigate(..., replaceUrl: true)`.

### P2 — Còn trạng thái legacy `COMPLETED` ở frontend

Target D1 yêu cầu dùng `DELIVERED`, tránh thêm `COMPLETED` cùng nghĩa. Frontend vẫn lọc/hiển thị `COMPLETED` tại:

- `staff-orders.component.ts/html`
- `custom-orders.component.ts/html`
- `staff-custom-orders.component.html`
- các spec tương ứng

Backend enum/runtime hiện dùng `DELIVERED`; FE nên bỏ nhánh legacy sau khi migration dữ liệu đã hoàn tất.

### P2 — API catalog tin `userId` do client truyền

`ProductController` nhận `@RequestParam("userId")` ở create/update/add variant/delete và chuyển thẳng vào service. Điều này không khớp C3: actor phải lấy từ JWT. Dù endpoint đã có role guard, audit actor vẫn có thể bị giả mạo.

Khuyến nghị dùng `CurrentUserService`/JWT principal trong controller/service và bỏ `userId` khỏi public contract.

### P2 — Tiền dùng số dấu phẩy động

`Order`, `OrderItem`, `OrderAgreement`, `PaymentAttempt` và nhiều DTO/service vẫn dùng `Double`; `MoneyUtils` làm tròn trên `double`. Target C3 yêu cầu biểu diễn chính xác (số nguyên VND hoặc decimal). Cần chuẩn hóa trước production để tránh sai số ở voucher, webhook amount và đối soát.

### P2 — Tự đặt giới hạn 10 ảnh cho thỏa thuận custom

Target C1 nói số ảnh tối đa chưa được chốt và không được tự cố định trong code. Hiện:

- FE hiển thị `x/10` và chặn từ ảnh thứ 10.
- `OrderAgreementService` từ chối list lớn hơn 10.

Giới hạn 1–10 ảnh của checkpoint sản xuất là contract riêng, không phải vấn đề; mismatch nằm ở ảnh tham khảo custom/agreement.

### P3 — Quản lý secret/config local

`application.yaml` có default credential phục vụ local test. Trước khi commit/deploy cần chuyển hoàn toàn sang environment/secret manager và rotate các giá trị đã từng ghi vào source. Báo cáo không ghi lại bất kỳ secret/token nào.

## 5. Ma trận AC01–AC47

Quy ước:

- **E2E PASS**: đã đi qua UI + API thật trong lượt kiểm thử này.
- **TEST PASS**: regression/service/repository/BPMN có bằng chứng, nhưng chưa mô phỏng live đầy đủ trong lượt này.
- **PARTIAL/GAP**: có phần đạt nhưng còn thiếu hoặc sai target.
- **NOT LIVE**: chủ động không chạy ngoài đời vì cần thời gian thật, race chuyên dụng hoặc giao dịch online.

| AC | Trạng thái | Bằng chứng/ghi chú |
|---|---|---|
| 01 | PARTIAL | E2E catalog/made-to-order pass; chưa đổi một mẫu sang PAUSED để thử checkout live |
| 02 | TEST PASS | Cart/checkout regression pass; tổng tiền lấy server-side |
| 03 | TEST PASS | Snapshot mapper/checkout test pass; chưa đổi giá catalog live sau khi tạo đơn |
| 04 | PARTIAL | Tạo nhiều draft và submit một draft thành Order pass; còn lỗi phải reload sau create |
| 05 | TEST PASS | Payment service chặn custom chưa đủ điều kiện; không mở thanh toán trong E2E |
| 06 | TEST PASS | Manager review độc lập; rule reputation/COD có test, chưa đổi reputation live |
| 07 | E2E PASS | Staff đang giữ #14 không claim được #15; trả 409 đúng nghiệp vụ |
| 08 | TEST PASS / NOT LIVE | Atomic assignment/claim test pass; MySQL concurrency opt-in chưa chạy |
| 09 | E2E PASS | KCS #14 release staff; staff sau đó claim #16/#18/#19 |
| 10 | E2E PASS | Thread được tạo qua retry; chat WebSocket 90–368 ms; auth interceptor test pass |
| 11 | E2E PASS | User submit version hiện hành, staff confirm; catalog không có form sửa giá |
| 12 | TEST PASS / NOT LIVE | Timer payment 1 giờ có service test; không chờ 1 giờ thật |
| 13 | TEST PASS / NOT LIVE | Signature/amount/reference webhook có regression; không gọi provider thật |
| 14 | TEST PASS / NOT LIVE | Idempotency webhook/event có regression; không phát webhook thật |
| 15 | TEST PASS / NOT LIVE | Payment không tự start production; không chuyển tiền thật |
| 16 | E2E PASS | Staff bấm start mới chuyển `ORDER_CREATING`; timestamp/idempotency có test |
| 17 | TEST PASS | Checkpoint theo OrderItem/attempt; chưa tạo quantity=20 live |
| 18 | TEST PASS | Rework/attempt history có service/component test; chưa thao tác rework live |
| 19 | TEST PASS | ChangeRequest reject test pass; chưa thao tác live |
| 20 | TEST PASS | Scope/value invariants ChangeRequest test pass; chưa thao tác live |
| 21 | E2E PASS | Guest lookup token đọc đơn và xác nhận receipt; token không bị ghi log/report |
| 22 | TEST PASS | Validation receipt thiếu/trùng/âm/ngoài đơn có service test |
| 23 | TEST PASS | Mismatch/acceptMismatch có service + FE test; live dùng trường hợp số lượng khớp |
| 24 | TEST PASS | Reputation idempotent cho user và không cộng guest có test; live guest hoàn tất thành công |
| 25 | TEST PASS | Cancellation atomicity/service test pass; chưa hủy đơn live |
| 26 | TEST PASS | Reorder catalog/custom service + FE contract test pass; chưa reorder live |
| 27 | TEST PASS | Retry/DLQ/outbox test pass; màn workflow operations tải thành công |
| 28 | TEST PASS | Migration cutover bỏ inventory transaction/stock fields; regression pass |
| 29 | PARTIAL | Guest flow/email event chạy; user xác nhận mail service gửi được, chưa đối chiếu inbox/nội dung mail |
| 30 | PARTIAL | Start/deadline runtime pass; chưa đối chiếu nội dung email ngày bắt đầu/lý do muộn |
| 31 | TEST PASS / NOT LIVE | PT24H và stop timer có test; không chờ 24 giờ thật |
| 32 | PARTIAL | Event handler/idempotency test pass; chưa đếm chính xác bốn email trong inbox thật |
| 33 | TEST PASS | Guest cancel/rate-limit/quota có service test; chưa bắn tải rate-limit live |
| 34 | TEST PASS | Logic multi-item KCS có test; E2E hiện chỉ có một item |
| 35 | E2E PASS | INITIAL_SHAPE upload pass và Order vẫn `ORDER_CREATING`; mail/notification được event handler test |
| 36 | E2E PASS | Guest và custom tạo Order ở `PENDING_APPROVAL`; draft chưa submit không tạo Order |
| 37 | TEST PASS | State guard cancel có test; chưa mở lại link cancel live sau accepted |
| 38 | TEST PASS | Custom cancel trước giá/reputation có test; chưa thao tác live |
| 39 | TEST PASS | `CatalogDurationCalculatorTest` chạy đủ các nhánh công thức |
| 40 | TEST PASS | Validation madeDay backend/frontend có test/build pass |
| 41 | TEST PASS | Duration theo từng item có service test; chưa tạo multi-item live |
| 42 | TEST PASS | `ProductionCalendarServiceTest` pass |
| 43 | TEST PASS | Validator 4.000/4.001 từ có service test; chưa nhập 4.001 từ qua UI |
| 44 | GAP | List URL/snapshot có test, nhưng agreement đang áp giới hạn 10 ảnh trái target |
| 45 | E2E PASS | Custom #19 có một OrderItem; catalog quantity/item model chạy đúng |
| 46 | E2E PASS | Cả catalog #18 và custom #19 đều bắt buộc vào production |
| 47 | TEST PASS / NOT LIVE | Availability repository/service test pass; chưa đổi PAUSED/DISCONTINUED live |

## 6. Dữ liệu test đã tạo

Kiểm thử thực tế tạo dữ liệu trong database local:

- #14: `READY_TO_SHIP` (dùng để kiểm tra checkpoint/KCS/release assignment).
- #15: `PENDING_ASSIGNMENT` (staff bị chặn khi đang bận).
- #16: `SHIPPING` (luồng thử trước khi sửa selector receipt của runner).
- #17: `PENDING_APPROVAL` (ghi nhận 409 manager review).
- #18: `DELIVERED` (luồng guest hoàn chỉnh).
- #19: `ORDER_CREATING` (luồng custom/chat/agreement hoàn chỉnh đến start production).
- Draft #4–#7 còn ở `DRAFT`; draft #8 đã submit thành đơn #19.

Không nên xóa trực tiếp bằng SQL nếu muốn giữ đúng audit. Có thể giữ làm dữ liệu UAT hoặc dọn bằng API/workflow phù hợp sau khi sửa lỗi.

## 7. Việc chưa kiểm thử trực tiếp

- Không chuyển tiền/thực hiện giao dịch SePay thật theo yêu cầu.
- Không đợi timer 1 giờ/24 giờ theo thời gian thực.
- Không chạy các test MySQL opt-in và bài tải concurrency nhiều process.
- Không đăng nhập Gmail để kiểm tra từng mail, số lượng mail và nội dung link.
- Không thay đổi catalog/voucher/reputation production-like vì có thể làm ảnh hưởng dữ liệu dùng chung.

Ưu tiên xử lý: (1) lỗi 409 manager review, (2) submit custom ngay sau create, (3) bỏ `COMPLETED`, (4) bỏ client `userId`, (5) chuẩn hóa money, (6) bỏ giới hạn ảnh custom chưa được chốt. Sau đó chạy lại E2E cho các đơn mới và bổ sung MySQL opt-in/concurrency test.
