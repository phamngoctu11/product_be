# WF03 — Phân công và xác nhận đơn (`agree_order`)

Ngày lập: 05/10/2026. Đây là đặc tả hệ thống mục tiêu, không khẳng định code hiện tại đã triển khai đủ.

Nguồn: [target.txt](../../target.txt), mục C4–C5, D, E/WF03, E1, F07–F09b, I và J. Đầu vào đến từ [WF02](../WF02-accept-order/README.md).

## 1. Phạm vi và điểm bàn giao

WF03 bắt đầu sau khi manager chấp nhận khả năng thực hiện Order tại WF02:

- Nếu WF02 chưa chọn staff, Order ở `PENDING_ASSIGNMENT` và chờ manager phân công hoặc staff tự claim.
- Nếu WF02 đã chọn staff hợp lệ, assignment đã được ghi qua nghiệp vụ dùng chung của WF03; quy trình tiếp tục từ bước xử lý sau phân công, không tạo assignment thứ hai.
- Với CUSTOM, `managerApprovedAt` và `confirmationDueAt = managerApprovedAt + 24 giờ` đã được ghi khi manager duyệt. Cửa sổ này bao gồm cả thời gian chờ staff.

WF03 kết thúc khi:

- Đơn của USER được staff xác nhận phiên bản thỏa thuận hợp lệ và chuyển `ORDER_ACCEPTED`.
- Guest CATALOG có staff nhận, dùng nguyên snapshot catalog và tự chuyển `ORDER_ACCEPTED`, không tạo chat hoặc yêu cầu guest xác nhận lại.
- CUSTOM hết 24 giờ mà chưa có form hợp lệ thì chuyển `CANCELLED` qua nghiệp vụ hủy dùng chung.

WF03 không tạo payment, không đánh dấu PAID, không chuyển `ORDER_CREATING` và không thực hiện checkpoint. Các bước đó thuộc WF04–WF05.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân chính | Kết quả |
|---|---|---|---|
| UC03.1 | Manager phân công staff | MANAGER | Một assignment hợp lệ hoặc lỗi conflict |
| UC03.2 | Staff tự nhận đơn | STAFF | Staff trở thành người phụ trách duy nhất |
| UC03.3 | Truy cập và trao đổi trong phòng chat | USER sở hữu Order, STAFF phụ trách | ChatThread riêng tư đúng Order và lịch sử trao đổi |
| UC03.4 | User gửi form thông tin thỏa thuận | USER | Một agreement version hợp lệ chờ staff xác nhận; timer CUSTOM dừng |
| UC03.5 | Staff xác nhận hoặc yêu cầu sửa | STAFF phụ trách | ORDER_ACCEPTED hoặc quay lại trao đổi |

Camunda, database, service task và event consumer là thành phần nội bộ, không phải actor người dùng trong Use Case Diagram. Guest không thực hiện UC03.3–UC03.5. Guest chỉ nhận email/trang Order bảo mật sau nhánh tự động theo snapshot catalog.

## 3. Quy tắc chung của WF03

### 3.1 Assignment

- Một Order chỉ có một active assignment sản xuất; một staff chỉ có một active assignment.
- Staff phải có tài khoản STAFF hợp lệ, `AVAILABLE` và chưa giữ active assignment khác tại thời điểm ghi.
- Manager assign và staff self-claim dùng cùng một application service, ràng buộc database và kiểm soát cạnh tranh.
- Staff bận ngay khi assignment được tạo, kể cả đang chờ form, trao đổi hoặc chờ thanh toán.
- WF03 không hỗ trợ đổi staff, reassign hoặc ghi đè assignment hiện có.
- Assignment chỉ được release khi toàn đơn qua KCS checkpoint 2 hoặc đơn bị hủy; giữ `assignedStaffId` và lịch sử sau release.

### 3.2 Camunda và timer CUSTOM

- Mỗi Order dùng process instance đã bắt đầu ở WF02, business key duy nhất gắn `orderId`.
- Chặng CUSTOM trước khi user gửi form được bao bởi một subprocess có interrupting boundary timer đặt theo `confirmationDueAt` tuyệt đối. Vì vậy timer vẫn có thể hết hạn khi Order còn chờ staff.
- Sau assignment, Camunda gọi service task tìm/tạo chat theo `orderId`. Service task phải idempotent.
- Form hợp lệ được lưu thành công và correlate vào đúng process sẽ kết thúc phạm vi chờ form, hủy timer ngay; không đợi staff xác nhận.
- Nếu user đã gửi form hợp lệ rồi staff yêu cầu sửa, timer 24 giờ không khởi động lại.
- Khi submit và timeout cạnh tranh, kiểm tra trạng thái/version trong database phải bảo đảm chỉ một kết quả có hiệu lực.

### 3.3 Trạng thái theo loại khách/đơn

| Trường hợp | Sau assignment | Bước tiếp theo |
|---|---|---|
| USER CUSTOM | `DISCUSSING` | Tạo chat, user gửi form, staff xác nhận |
| USER CATALOG | `DISCUSSING` | Tạo chat/form trong phạm vi cho phép; giá và thời lượng catalog chỉ đọc |
| Guest CATALOG | `ORDER_ACCEPTED` | Không chat/form; gửi email mốc ORDER_ACCEPTED và bàn giao WF04 |

CUSTOM chỉ dành cho USER. Manager duyệt không phải là staff xác nhận thỏa thuận. CATALOG không được thay đơn giá snapshot qua form, API hoặc chat.

## 4. UC03.1 — Manager phân công staff

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Giao Order đã duyệt cho một staff đủ điều kiện |
| Kích hoạt | Manager chọn staff khi duyệt tại WF02 hoặc trên danh sách `PENDING_ASSIGNMENT` |
| Tiền điều kiện | MANAGER hợp lệ; Order đã được duyệt, chưa có active assignment; staff mục tiêu hợp lệ |
| Đầu vào | orderId, staffId, version; actor manager lấy từ JWT |
| Thành công | Tạo đúng một Assignment, staff bị chiếm suất và workflow đi tiếp |
| Bảo đảm thất bại | Không có assignment hoặc quyết định dở dang do staff không còn khả dụng |

### Luồng chính

| Bước | Manager | Hệ thống |
|---|---|---|
| 1 | Mở đơn đã duyệt/chờ phân công | Hiển thị trạng thái và danh sách staff AVAILABLE mang tính tham khảo |
| 2 | Chọn một staff và xác nhận | Lấy managerId từ JWT, không tin actor do client gửi |
| 3 | — | Khóa/kiểm tra version Order, staff và active assignment |
| 4 | — | Xác nhận Order chưa có staff; staff vẫn AVAILABLE và chưa có active assignment |
| 5 | — | Trong transaction tạo Assignment nguồn `MANAGER_ASSIGN`, ghi assignedStaff/assignedAt và đánh dấu staff bận |
| 6 | — | Đăng ký sự kiện/bàn giao Camunda theo orderId, chống xử lý trùng |
| 7 | Nhận kết quả | Trả assignment và bước tiếp theo; staff nhận notification phân công sau commit |

Nếu thao tác được gọi kèm quyết định duyệt của WF02, quyết định review và assignment phải nhất quán theo hợp đồng transaction đã nêu ở WF02; không báo duyệt kèm staff thành công khi assignment thất bại.

### Ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| A01 | Không có quyền MANAGER | 403; không lộ danh sách staff hoặc sửa Order |
| A02 | Order chưa duyệt, đã hủy hoặc đã qua bước này | 409 hoặc trả kết quả idempotent phù hợp; không ghi đè |
| A03 | Staff không tồn tại/không đúng role/không AVAILABLE | Từ chối assignment |
| A04 | Staff vừa nhận đơn khác | Unique constraint/khóa làm một giao dịch thắng, giao dịch còn lại conflict |
| A05 | Order vừa được staff khác claim | Không thay người đã nhận; trả trạng thái hiện tại |
| A06 | DB lỗi | Rollback Order, Assignment và trạng thái staff |
| A07 | Commit thành công nhưng Camunda/notification lỗi | Giữ assignment; retry bàn giao bằng khóa nghiệp vụ, không assign lại |

## 5. UC03.2 — Staff tự nhận đơn

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Cho staff AVAILABLE tự nhận một Order đã được manager duyệt |
| Kích hoạt | Staff chọn “Nhận đơn” từ danh sách có thể claim |
| Tiền điều kiện | STAFF hợp lệ, AVAILABLE, không có active assignment; Order `PENDING_ASSIGNMENT` |
| Đầu vào | orderId, version; staffId lấy từ JWT |
| Thành công | Assignment nguồn `SELF_CLAIM`, staff trở thành người phụ trách duy nhất |
| Bảo đảm thất bại | Không chiếm hai đơn và không ghi đè người phụ trách |

### Luồng chính

1. Staff mở danh sách Order đã duyệt nhưng chưa có người phụ trách.
2. Backend chỉ trả dữ liệu cần thiết theo quyền; CUSTOM chưa chốt giá hiển thị “chưa xác định”.
3. Staff chọn Order và gửi claim.
4. Backend xác thực staff từ JWT, khóa/kiểm tra Order và điều kiện active assignment.
5. Trong transaction tạo Assignment nguồn `SELF_CLAIM`, gắn staff vào Order và chiếm suất.
6. Sau commit phát notification/bàn giao Camunda; trả assignment và bước tiếp theo.

Nhánh cạnh tranh:

- Hai staff claim cùng Order: chỉ một người thành công.
- Một staff claim hai Order đồng thời: chỉ một active assignment thành công.
- CUSTOM đã quá `confirmationDueAt` và chưa có form hợp lệ: claim không được hồi sinh Order; nhánh timeout/hủy quyết định kết quả.
- Request claim lặp của chính người đã nhận cùng Order có thể trả lại assignment hiện có, nhưng không tạo record thứ hai.

## 6. UC03.3 — Truy cập và trao đổi trong phòng chat

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Cung cấp kênh trao đổi giữa USER và staff phụ trách trước khi chốt thông tin |
| Kích hoạt | Assignment của Order USER được ghi thành công và workflow đi qua service task tạo chat |
| Tiền điều kiện | Order đã được manager duyệt, có userId và assignedStaffId |
| Thành công | Có đúng một ChatThread theo Order; thành viên đúng quyền có thể đọc/gửi realtime |
| Bảo đảm thất bại | Không tạo trùng phòng và không mở quyền cho staff/user khác |

### Luồng chính

| Bước | Tác nhân | Hệ thống |
|---|---|---|
| 1 | — | Camunda service task gọi tìm/tạo ChatThread theo orderId |
| 2 | — | Chỉ gắn owner USER và staff phụ trách; không cấp membership cho MANAGER |
| 3 | USER/STAFF mở chat | REST kiểm tra membership trước khi trả lịch sử; WebSocket kiểm tra quyền subscribe |
| 4 | USER/STAFF gửi tin | Kiểm tra quyền gửi, lưu message rồi phát realtime cho đúng phòng |
| 5 | USER xem thông tin đơn | Hiển thị nút “Thông tin & chốt đơn”, trạng thái và voucher khả dụng |
| 6 | Hai bên trao đổi | Tin nhắn được lưu; việc gửi tin không tự đổi trạng thái hoặc xác nhận giá |

Phòng chat là riêng tư giữa USER sở hữu Order và STAFF đang phụ trách. MANAGER không được xem lịch sử, đọc/gửi tin hoặc subscribe, kể cả manager đã duyệt hay phân công Order. Staff khác cũng không được truy cập. Guest không có chat; link guest chỉ mở trang Order theo token giới hạn.

Lỗi tạo chat sau assignment được retry/reconciliation. Không rollback assignment chỉ vì WebSocket hoặc push realtime đang lỗi; UI phải thể hiện chat chưa sẵn sàng thay vì tạo phòng khác thủ công.

## 7. UC03.4 — User gửi form thông tin thỏa thuận

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Ghi một phiên bản thông tin chính thức để staff kiểm tra |
| Kích hoạt | USER bấm gửi tại “Thông tin & chốt đơn” |
| Tiền điều kiện | USER là owner; Order có staff phụ trách, đang `DISCUSSING`; với CUSTOM chưa hết hạn nếu chưa từng gửi form hợp lệ |
| Đầu vào CUSTOM | spec, quantity, unitPrice, productionDurationDays/số ngày đã trao đổi, userVoucherId tùy chọn, note, version/idempotency key |
| Đầu vào USER CATALOG | Thông tin/ghi chú được policy cho phép; giá snapshot và thời lượng catalog chỉ đọc |
| Thành công | Lưu agreement version `SUBMITTED`, `userSubmittedAt`; trạng thái `WAITING_STAFF_CONFIRMATION`; timer CUSTOM dừng |
| Bảo đảm thất bại | Không dừng timer, không tiêu thụ voucher và không ghi giá chính thức từ form lỗi |

### Luồng chính

1. USER mở form từ phòng chat và xem dữ liệu Order hiện tại.
2. Với CUSTOM, USER nhập đúng giá, số ngày và chi tiết đã trao đổi; chọn voucher khả dụng nếu muốn.
3. Backend xác thực owner, staff/Order hiện hành, version và dữ liệu. Tiền dùng kiểu chính xác; quantity nguyên dương; giá/thời lượng phải hợp lệ theo validation triển khai.
4. Backend tính bản xem trước subtotal, discount và finalPrice. Voucher chỉ được kiểm tra sơ bộ, chưa đánh dấu used.
5. Trong transaction lưu agreement version, snapshot nội dung, `userSubmittedAt` và chuyển `WAITING_STAFF_CONFIRMATION`.
6. Với CUSTOM lần gửi hợp lệ đầu tiên, đánh dấu bước chờ form đã hoàn tất. Sau commit correlate message vào đúng process; subprocess chờ form kết thúc và timer PT24H bị hủy.
7. Staff phụ trách nhận notification có phiên bản cần xem. USER nhận kết quả và không cần gửi lại do mạng chậm.

### Nhánh sửa và timeout

- Draft/chỉ xem preview không dừng timer. Request validation lỗi không dừng timer.
- Cùng idempotency key và cùng nội dung trả lại version đã lưu; cùng key khác nội dung trả conflict.
- Nếu staff yêu cầu sửa, Order quay lại `DISCUSSING`; USER gửi version mới. Timer 24 giờ không khởi động lại vì đã dừng ở lần gửi hợp lệ đầu tiên.
- Nếu timer thắng trước khi form hợp lệ được commit, hủy `CUSTOM_CONFIRMATION_TIMEOUT`, không trừ reputation, release assignment nếu có và gửi email + notification cho USER.
- Nếu form commit thắng, timeout không được hủy Order dù correlate bị chậm; reconciliation phải hoàn tất process theo trạng thái DB.

## 8. UC03.5 — Staff xác nhận hoặc yêu cầu sửa

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Chốt phiên bản đúng với trao đổi hoặc trả lại USER sửa |
| Kích hoạt | Staff phụ trách mở task phiên bản đang chờ |
| Tiền điều kiện | STAFF là người giữ active assignment; Order `WAITING_STAFF_CONFIRMATION`; agreement version còn là bản mới nhất |
| Đầu vào | orderId, agreementVersion, decision CONFIRM/REQUEST_CHANGES, reason khi yêu cầu sửa |
| Thành công CONFIRM | Lưu dữ liệu chính thức, voucher usage, finalPrice; Order `ORDER_ACCEPTED` |
| Thành công REQUEST_CHANGES | Lưu reason/history; Order `DISCUSSING`; chờ version mới, không khởi động lại timer |
| Bảo đảm thất bại | Không xác nhận bản cũ, không dùng voucher hoặc cập nhật giá một phần |

### Luồng xác nhận

| Bước | Staff | Hệ thống |
|---|---|---|
| 1 | Mở task và xem agreement | Kiểm tra assignee/active assignment và trả phiên bản hiện hành |
| 2 | Chọn xác nhận | Khóa/kiểm tra Order, agreement version và trạng thái |
| 3 | — | Với CUSTOM, kiểm tra lại spec, giá, duration và voucher tại thời điểm xác nhận |
| 4 | — | Trong transaction ghi confirmedBy/At, áp dụng snapshot chính thức, tính subtotal/discount/finalPrice và tiêu thụ voucher hợp lệ |
| 5 | — | Chuyển `ORDER_ACCEPTED`, ghi history và công việc hoàn tất task/bàn giao WF04 |
| 6 | Nhận kết quả | Trả giá/thỏa thuận đã chốt; sau commit gửi email + notification mốc ORDER_ACCEPTED cho USER |

USER CATALOG không được thay `unitPrice`, giá snapshot hoặc `productionDurationDays` catalog. Xác nhận chỉ chấp nhận các thông tin nằm trong policy. Guest CATALOG không đi qua UC này; hệ thống dùng snapshot catalog khi assignment được tạo.

### Luồng yêu cầu sửa và ngoại lệ

1. Staff chọn yêu cầu sửa và nhập lý do.
2. Backend kiểm tra staff phụ trách và version mới nhất.
3. Ghi quyết định/reason, chuyển `DISCUSSING`, thông báo USER sửa form.
4. Không phát email mốc trạng thái `ORDER_ACCEPTED`, không tiêu thụ voucher và không reset timer 24 giờ.

Ngoại lệ:

- Staff khác hoặc assignment đã release: 403/409, không xử lý.
- Agreement version cũ: 409 và trả phiên bản hiện hành.
- Voucher hết hiệu lực/hết quota/không còn thuộc user tại lúc confirm: transaction bị từ chối; yêu cầu USER chọn lại, không âm thầm bỏ voucher làm tăng giá.
- Hai quyết định đồng thời: khóa/version bảo đảm một quyết định có hiệu lực.
- DB commit nhưng Camunda/email lỗi: giữ `ORDER_ACCEPTED`, retry bàn giao/thông báo; không confirm hoặc trừ voucher lần hai.

## 9. Nhánh tự động Guest CATALOG

Sau UC03.1 hoặc UC03.2 tạo assignment cho Guest CATALOG:

1. Backend/Camunda nhận biết `orderType = CATALOG` và `userId = null`.
2. Không tạo ChatThread, không tạo agreement form và không chờ guest xác nhận design/deadline.
3. Kiểm tra snapshot catalog/giá/thời lượng đã có và assignment còn hợp lệ.
4. Chuyển `ORDER_ACCEPTED` một lần và bàn giao WF04. COD bỏ bước payment online tại WF04.
5. Sau commit gửi email mốc ORDER_ACCEPTED kèm link token tới thông tin đơn, mẫu, giá, quantity và thời lượng. Guest không có notification trong hệ thống.

Nếu bước tự động lỗi sau assignment, giữ trạng thái có thể reconciliation; không tạo assignment mới hoặc gửi email chấp nhận trước khi `ORDER_ACCEPTED` đã commit.

## 10. Trạng thái, timer và thông báo

| Sự kiện | Trạng thái/kết quả | Thông báo |
|---|---|---|
| Assignment được tạo | USER → `DISCUSSING`; Guest CATALOG → nhánh tự động | Notification cho staff; không phải email trạng thái khách |
| Chat message | Không đổi Order status | Realtime trong phòng, không phải email trạng thái |
| USER gửi form | `WAITING_STAFF_CONFIRMATION` | Notification cho staff; không gửi email trạng thái khách |
| Staff yêu cầu sửa | `DISCUSSING` | Notification cho USER, không phải một trong bốn email trạng thái |
| Staff xác nhận / Guest CATALOG tự xác nhận | `ORDER_ACCEPTED` | USER: email + notification; Guest: email |
| CUSTOM hết 24 giờ trước form hợp lệ | `CANCELLED` | Email + notification cho USER; thông báo staff nếu đã gán |

Timer CUSTOM là `PT24H` tính từ `managerApprovedAt`, không tính từ assignment/chat. Timer ONLINE `PT1H` chỉ bắt đầu ở WF04 sau `ORDER_ACCEPTED` và mở PaymentAttempt.

## 11. Hợp đồng dữ liệu và triển khai

| Thao tác | Đầu vào chính | Đầu ra tối thiểu |
|---|---|---|
| Assign | orderId, staffId, version; manager JWT | assignmentId, assignedStaffId, assignedAt, nextStep |
| Claim | orderId, version; staff JWT | assignmentId, assignedAt, nextStep |
| Chat | orderId, message/cursor; JWT | chatThreadId, membership, messages |
| Submit agreement | orderId, fields theo loại đơn, voucherId, version, idempotency key | agreementVersion, state, preview totals, userSubmittedAt |
| Decide agreement | orderId, agreementVersion, decision, reason | decision, Order status, official totals nếu confirm |

Thiết kế dữ liệu tối thiểu gồm `Assignment`, `ChatThread`, `ChatMessage`, `OrderAgreement` versioned, các timestamp timer và history. Ràng buộc active assignment phải bảo vệ đồng thời cả mỗi Order và mỗi staff. ChatThread có unique theo orderId. Agreement version không bị ghi đè để bảo toàn audit.

Process variable chỉ giữ khóa tham chiếu nhỏ như orderId/orderType/assignmentId/currentAgreementVersion. Ảnh, message, spec đầy đủ và entity không được nhét vào process variable. Tên endpoint/service/event là thiết kế đề xuất.

## 12. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | Manager assign staff AVAILABLE | Một assignment, staff bị chiếm suất, workflow đi tiếp |
| TC02 | Manager chọn staff vừa nhận đơn khác | Conflict, không ghi assignment hoặc duyệt dở dang |
| TC03 | Hai staff claim cùng Order | Chỉ một người phụ trách |
| TC04 | Một staff claim hai Order đồng thời | Chỉ một active assignment |
| TC05 | Assignment event gửi lặp | Không tạo assignment/chat thứ hai |
| TC06 | USER Order có assignment | Một ChatThread đúng owner/staff |
| TC07 | Guest CATALOG có assignment | Không chat/form; ORDER_ACCEPTED và một email |
| TC08 | MANAGER hoặc staff khác đọc/gửi/subscribe chat | Bị từ chối ở REST và WebSocket |
| TC09 | USER gửi form CUSTOM hợp lệ trước hạn | Version được lưu, timer dừng ngay, chờ staff |
| TC10 | Draft hoặc form lỗi validation | Timer vẫn chạy, voucher chưa dùng |
| TC11 | Hết 24h khi còn chờ staff, chưa có form | CANCELLED, không trừ reputation |
| TC12 | Submit và timeout đồng thời | Chỉ một kết quả; form commit trước không bị timeout hủy |
| TC13 | Staff yêu cầu sửa sau form hợp lệ | DISCUSSING, có reason, timer không khởi động lại |
| TC14 | Staff xác nhận version cũ | Conflict, không thay giá/trạng thái |
| TC15 | Voucher hết hiệu lực tại confirm | Không confirm, không âm thầm bỏ voucher |
| TC16 | Staff xác nhận CUSTOM hợp lệ | Giá/voucher/spec chính thức và ORDER_ACCEPTED nguyên tử |
| TC17 | USER CATALOG cố gửi giá khác | Backend từ chối/không áp dụng giá client |
| TC18 | Confirm/event/email bị gửi lặp | Một lần chuyển trạng thái, voucher và thông báo theo khóa mốc |
| TC19 | Order mới ORDER_ACCEPTED | Chưa PAID, chưa ORDER_CREATING; bàn giao WF04 |
| TC20 | Đơn bị hủy trong WF03 | Release đúng assignment, giữ lịch sử assignedStaffId |

## 13. Sơ đồ

- [Use Case tổng quát](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity Diagram theo từng UC](diagram.md).
- [Sequence Diagram theo từng UC](sequence.md).
