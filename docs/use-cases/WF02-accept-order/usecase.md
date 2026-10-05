# WF02 — Tiếp nhận và duyệt đơn (`accept_order`)

Ngày lập: 05/10/2026. Đặc tả hệ thống mục tiêu, không phải xác nhận hành vi runtime hiện tại.

Nguồn: [target.txt](../../target.txt), mục E/WF02, F03–F07, F16, F18, C4–C5 và D. Liên quan: [WF01](../WF01-custom-request/README.md).

## 1. Phạm vi và điểm bàn giao

WF02 có hai đầu vào:

- **CATALOG:** user/guest chọn mục trong giỏ và checkout; WF02 tạo Order cùng snapshot ở `PENDING_APPROVAL`.
- **CUSTOM:** nhận `orderId` đã được tạo ở `PENDING_APPROVAL` tại WF01. Không tạo lại Order, không đưa custom vào cart giả và không phát lại email tạo đơn.

Manager xem toàn bộ yêu cầu/số lượng và quyết định cửa hàng có nhận làm được hay không. Manager có thể chọn staff khi duyệt hoặc để đơn chờ staff claim. Kiểm tra và lưu assignment phải dùng chung quy tắc của WF03, không xây một cơ chế phân công khác.

WF02 kết thúc khi quyết định được lưu và bước workflow được bàn giao nhất quán:

- Duyệt: ghi người/thời điểm duyệt; chưa gán staff thì `PENDING_ASSIGNMENT`. Nếu kèm staff hợp lệ, bàn giao WF03 với assignment đã ghi nhận.
- Từ chối: `CANCELLED`, lý do, lịch sử và các tác động hủy cần thiết.

WF02 không thực hiện chat, chốt giá custom, payment, checkpoint hoặc giao hàng. Không coi chữ “accept” trong tên workflow là lệnh luôn chuyển `ORDER_ACCEPTED`.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân chính | Kết quả |
|---|---|---|---|
| UC02.1 | Đặt hàng catalog từ giỏ | USER, Guest | Order CATALOG / PENDING_APPROVAL |
| UC02.2 | Xem danh sách và chi tiết đơn chờ duyệt | MANAGER | Dữ liệu đơn để đánh giá khả năng làm |
| UC02.3 | Chấp nhận hoặc từ chối tiếp nhận đơn | MANAGER | Quyết định duyệt và bàn giao WF03, hoặc CANCELLED |

**Tác nhân phụ:** dịch vụ email hỗ trợ xác nhận tạo đơn và thông báo từ chối. USER có notification trong hệ thống; Guest chỉ email.

Backend, database, Redis và Camunda là thành phần nội bộ. STAFF không thực hiện ba UC này: self-claim nằm ở WF03. Cổng thanh toán không tham gia WF02 vì chưa đến bước thanh toán. ADMIN không tự có quyền MANAGER nếu chưa được cấp theo chính sách.

## 3. UC02.1 — Đặt hàng catalog từ giỏ

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Tạo đơn theo mẫu catalog và gửi manager xét nhận làm |
| Kích hoạt | Khách chọn các mục trong giỏ và bấm xác nhận đặt hàng |
| Tiền điều kiện | Có giỏ đúng chủ; mục được chọn còn hợp lệ; có thông tin giao hàng và phương thức được phép |
| Xác thực USER | JWT; backend lấy userId từ token |
| Định danh Guest | X-Guest-Session-Id để xác định giỏ; không dùng nó làm quyền truy cập đơn sau tạo |
| Thành công | Lưu Order, OrderItem, contact/price/time snapshot, voucher usage và cập nhật giỏ nhất quán |
| Bảo đảm thất bại | Không tạo đơn thiếu item, không mất mục giỏ/tiêu thụ voucher do transaction tạo đơn thất bại |

### Luồng chính

| Bước | Khách | Hệ thống |
|---|---|---|
| 1 | Chọn các mục catalog cần đặt | Hiển thị mục, quantity và giá tham khảo |
| 2 | Xác nhận họ tên, email, điện thoại, địa chỉ, ghi chú | Kiểm tra contact dùng để chụp vào đơn; Guest bắt buộc đủ bốn trường liên hệ |
| 3 | Chọn payment method và voucher nếu có | USER có reputation dưới 20 không được chọn COD; từ 20 được COD/ONLINE. Guest dùng COD/public voucher |
| 4 | Bấm đặt hàng | Nhận selected items và idempotency key; xác định chủ giỏ từ JWT/header |
| 5 | — | Kiểm tra yêu cầu lặp, quyền giỏ, item/variant chưa xóa, quantity nguyên dương và mẫu ACCEPTING_ORDERS |
| 6 | — | Lấy giá và madeDay từ database, tính tiền và thời lượng từng loại; không nhận giá client làm giá chính thức |
| 7 | — | Kiểm tra voucher theo loại khách, tính discount/finalPrice |
| 8 | — | Trong transaction: tạo Order CATALOG / PENDING_APPROVAL, các OrderItem, snapshot và lịch sử tạo; Guest cấp lookup token, chỉ lưu hash |
| 9 | — | Cùng transaction: ghi usage voucher, xóa các mục đã checkout, lưu kết quả chống tạo trùng |
| 10 | — | Commit; đăng ký manager review và email/notification tạo đơn theo cơ chế retry |
| 11 | Nhận kết quả và mở đơn | Trả orderId, trạng thái, tổng tiền; Guest nhận thông tin truy cập đơn theo cơ chế token |
| 12 | Nhận thông báo | USER: email + notification PENDING_APPROVAL; Guest: email xác nhận kèm link bảo mật |

Email được xử lý sau commit, không chặn phản hồi checkout. Link Guest có quyền đọc/hủy trong phạm vi được phép; mở link không tự hủy đơn bằng GET. Hủy thuộc WF09 và backend luôn kiểm tra trạng thái hiện hành.

### Luồng thay thế và ngoại lệ

| Mã | Bước | Điều kiện | Kết quả |
|---|---|---|---|
| A01 | 4–5 | JWT không hợp lệ hoặc giỏ sai chủ/phiên thiếu | Từ chối, không tạo đơn |
| A02 | 5 | Giỏ rỗng/không có mục được chọn hoặc dòng không hợp lệ | Trả lỗi để khách chỉnh lại |
| A03 | 5 | Mẫu PAUSED/DISCONTINUED hoặc đã xóa | Không checkout mẫu đó; không tạo Order rồi mới báo lỗi |
| A04 | 3/7 | USER reputation <20 chọn COD; Guest chọn phương thức chưa hỗ trợ | Từ chối phương thức, không tự đổi sang ONLINE rồi thu tiền |
| A05 | 7 | Voucher không hợp lệ/hết hạn/hết quota/không đúng chủ/đã dùng | Trả lỗi, không âm thầm bỏ voucher và tăng tiền phải trả |
| A06 | 4–9 | Cùng lần checkout đã thành công | Trả lại kết quả đơn, không trừ quota/xóa giỏ/tạo workflow lần hai |
| A07 | 4–9 | Cùng key nhưng nội dung khác hoặc gửi đồng thời xung đột | Báo conflict; ràng buộc transaction/idempotency quyết định một lần tạo |
| A08 | 8–9 | Database lỗi | Rollback đơn, item, voucher và thay đổi giỏ |
| A09 | Sau commit | Lỗi khởi workflow/phát event/gửi email | Giữ Order, lưu trạng thái retry/reconciliation; không checkout lại để chữa lỗi |

### Quy tắc giá, thời lượng và voucher

- `subtotal = Σ(unitPriceSnapshot × quantity)`; `finalPrice = max(0, subtotal - discountAmount)`.
- CATALOG không có giá thương lượng. Thay giá/madeDay catalog sau tạo không thay snapshot của OrderItem.
- Với từng item, D = madeDay >= 2 và q = quantity > 0: nếu q > 5 thì S = D + 1 + (D − 1.5) × (q − 5) + (D − 1) × 4; nếu không thì S = D + 1 + (D − 1) × (q − 1). `durationDays = ceil(S) + ceil(S × 0.1)`.
- WF02 lưu thời lượng từng item, chưa có deadline ngày cụ thể vì chưa có productionStartedAt. Deadline được tính khi staff bắt đầu ở WF04.
- UserVoucher: đúng owner, chưa dùng, chưa hết hạn, đạt giá trị đơn tối thiểu; đánh dấu used/usedDate khi áp dụng vào đơn.
- Guest voucher: public/guest, active, chưa hết hạn, còn quota và đạt minOrderValue; chặn trùng template theo session hoặc email hash hoặc phone hash. Quota giảm nguyên tử và có usage gắn Order.
- Giảm phần trăm có trần theo cấu hình; giảm cố định không vượt subtotal. Kiểu tiền cần tính chính xác ở backend.
- Không giữ/trừ stock; quota voucher không phải tồn kho thành phẩm.

## 4. UC02.2 — Manager xem đơn chờ duyệt

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Cung cấp thông tin để manager đánh giá khả năng làm toàn đơn |
| Kích hoạt | Manager mở danh sách chờ duyệt và chọn đơn |
| Tiền điều kiện | Đăng nhập và có quyền MANAGER |
| Thành công | Trả danh sách/chi tiết đúng dữ liệu đã lưu; không đổi trạng thái |
| Bảo đảm thất bại | Không lộ thông tin đơn cho người không có quyền |

| Bước | Manager | Hệ thống |
|---|---|---|
| 1 | Mở danh sách chờ duyệt | Kiểm tra quyền và truy vấn PENDING_APPROVAL, có phân trang |
| 2 | Chọn Order | Tải snapshot và trạng thái mới nhất |
| 3 | Xem loại đơn, spec/mẫu, quantity, ảnh, contact và tổng tiền | CATALOG hiển thị giá niêm yết snapshot; CUSTOM chưa chốt giá phải hiện rõ chưa xác định |
| 4 | Đánh giá khả năng làm và khối lượng toàn đơn | Cho chuyển sang UC02.3; có thể đọc danh sách staff đủ điều kiện nếu manager muốn giao ngay |

Luồng thay thế:

- Không có đơn chờ: danh sách rỗng, không báo lỗi nghiệp vụ.
- Order không tồn tại/không được truy cập: trả lỗi, không trả nội dung.
- Đơn đã bị khách hủy hoặc manager khác xử lý: hiển thị trạng thái mới, không cho UI mặc định tiếp tục quyết định cũ; UC02.3 vẫn kiểm tra lại phía server.
- Staff hiển thị AVAILABLE là dữ liệu tham khảo tại thời điểm đọc; điều kiện phải kiểm tra lại khi ghi assignment.

## 5. UC02.3 — Manager quyết định tiếp nhận

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Chấp nhận hoặc từ chối khả năng thực hiện đơn |
| Kích hoạt | Manager gửi quyết định từ trang đơn |
| Tiền điều kiện | MANAGER; Order ở PENDING_APPROVAL; có bước review hợp lệ trong workflow |
| Đầu vào | orderId, approve/reject, reason khi từ chối, staffId tùy chọn khi duyệt, version/khóa chống xử lý lặp theo thiết kế |
| Thành công khi duyệt | Lưu quyết định/actor/time, bàn giao trạng thái/assignment đúng nhánh, hoàn tất bước review |
| Thành công khi từ chối | CANCELLED, lý do, lịch sử, phục hồi voucher đã dùng và email ngoại lệ |
| Bảo đảm thất bại | Không một phần duyệt thành công nhưng assignment sai hoặc process chuyển sai bước |

### Luồng chính — chấp nhận

| Bước | Manager | Hệ thống |
|---|---|---|
| 1 | Chọn chấp nhận; có thể chọn staff | Nhận quyết định và staffId tùy chọn |
| 2 | — | Lấy manager hiện hành từ JWT, kiểm tra quyền và khóa/version Order |
| 3 | — | Kiểm tra PENDING_APPROVAL, chưa hủy/duyệt và task review tương ứng |
| 4 | — | Nếu có staffId: kiểm tra staff hợp lệ, AVAILABLE, chưa có active assignment; ghi assignment qua trách nhiệm chung của WF03 |
| 5 | — | Lưu managerApprovedAt, người duyệt, quyết định và history; không sửa giá CATALOG hoặc tự chốt giá CUSTOM |
| 6 | — | Không chọn staff: PENDING_ASSIGNMENT. Có staff: bàn giao WF03 để xử lý đúng loại đơn |
| 7 | — | CUSTOM: ghi confirmationDueAt = managerApprovedAt + 24 giờ và đăng ký bước chờ user gửi form; không reset hạn khi retry |
| 8 | Nhận kết quả | Hoàn tất/bàn giao task review nhất quán; trả trạng thái, quyết định, assignment nếu có |

**Chọn staff không hợp lệ:** thiết kế transaction đề xuất trả conflict và không lưu quyết định một phần. Manager chọn người khác hoặc chủ động gửi lại quyết định không kèm staff. Không tự bỏ staffId rồi báo đã giao thành công.

**Ranh giới với WF03:** nếu ghi assignment kèm duyệt, WF02 gọi nghiệp vụ assign dùng chung; service task mở chat/chuẩn bị thỏa thuận thuộc WF03. CUSTOM/user cần trao đổi có thể chuyển DISCUSSING; Guest CATALOG có staff và manager duyệt sẽ đi qua nhánh tự động xác nhận theo catalog đến ORDER_ACCEPTED, không có chat. Chuyển ORDER_ACCEPTED chỉ phát một email mốc đó. Không coi manager duyệt một mình là đủ để chốt custom.

### Luồng thay thế — từ chối

1. Manager chọn từ chối và nhập lý do.
2. Backend kiểm tra quyền, Order/task còn chờ duyệt và reason không rỗng.
3. Ghi quyết định REJECTED, người xử lý/thời gian; chuyển CANCELLED, cancelReason và endOrderTime.
4. Thực hiện tác động hủy trong cùng giao dịch nghiệp vụ: UserVoucher đã dùng được trả về used=false/usedDate=null; guest voucher hoàn quota một lần nhưng giữ usage theo session/email/phone để chống dùng lại. CUSTOM chưa dùng voucher không phát sinh hoàn voucher.
5. Không trừ reputation vì đây là manager từ chối, không phải khách chủ động hủy. Không hoàn stock và không báo hoàn tiền khi không có giao dịch hoàn tiền.
6. Hoàn tất nhánh từ chối của workflow, không tiếp tục phân công/chat/payment. Nếu có active assignment trong dữ liệu liên quan, xử lý release đúng assignment một lần theo WF09; không xóa lịch sử staff.
7. Sau commit, phát ORDER_CANCELLED và yêu cầu email lý do cho chủ đơn; USER có notification, Guest chỉ email.
8. Trả trạng thái CANCELLED và lý do.

Đây là nhánh gọi nghiệp vụ hủy dùng chung của WF09, không thêm một quy trình hủy khác. Ở luồng bình thường, đơn còn chờ manager chưa được thanh toán nên từ chối không tạo một nghiệp vụ hoàn tiền tự động.

### Ngoại lệ tại quyết định

| Mã | Điều kiện | Kết quả |
|---|---|---|
| B01 | Sai quyền hoặc giả changerId của manager khác | Từ chối; actor audit lấy từ danh tính xác thực |
| B02 | Đơn đã bị hủy/duyệt hoặc version cũ | Không ghi đè; trả kết quả đã xử lý hoặc conflict phù hợp |
| B03 | Hai manager hoặc manager/khách hủy cùng lúc | Khóa/version đảm bảo chỉ một chuyển tiếp có hiệu lực |
| B04 | Staff không AVAILABLE hoặc vừa nhận đơn khác | Không giao vượt giới hạn; trả conflict theo hợp đồng transaction |
| B05 | Từ chối thiếu lý do | Trả lỗi validation; Order còn chờ xử lý |
| B06 | Không có task review do lỗi khởi process | Không chỉ đổi DB rồi báo duyệt xong; trả trạng thái chưa sẵn sàng và xử lý retry/reconciliation |
| B07 | Lưu quyết định/assignment thất bại | Rollback transaction nghiệp vụ; không phát thông báo thành công |
| B08 | Commit rồi truyền kết quả workflow/email lỗi | Lưu trạng thái pending retry; không duyệt lại, hoàn quota lại hoặc tạo assignment khác |

## 6. Trạng thái, timer và thông báo

| Mốc | Order / dữ liệu | Thông báo chủ đơn |
|---|---|---|
| Checkout CATALOG thành công | PENDING_APPROVAL | USER email + notification; Guest email |
| CUSTOM từ WF01 đi vào review | Order hiện có PENDING_APPROVAL | Không gửi lại email tạo đơn từ WF01 |
| Manager duyệt chưa gán | PENDING_ASSIGNMENT, managerApprovedAt | Không thêm email trạng thái trung gian |
| Duyệt có gán | Kết quả bàn giao WF03 theo loại khách/đơn | Assignment notification nội bộ cho staff, không tự thêm email trạng thái trung gian cho khách |
| CUSTOM được manager duyệt | confirmationDueAt = approvedAt + 24h | Timer chờ user gửi form ở WF03; chưa phải timer payment |
| Manager từ chối | CANCELLED + reason + endOrderTime | Email ngoại lệ hủy; USER thêm notification |

Chỉ bốn email trạng thái trên luồng bình thường: PENDING_APPROVAL, ORDER_ACCEPTED, ORDER_CREATING, READY_TO_SHIP. Từ chối/hủy là ngoại lệ đã quy định. Email gửi lại do retry phải chống trùng theo mốc/event.

Timer custom dừng khi user gửi form hợp lệ, không chờ staff review; đó là xử lý tiếp theo ở WF03. Timer ONLINE 1 giờ chỉ bắt đầu khi mở payment ở WF04, không nằm trong checkout hoặc manager review.

## 7. Hợp đồng dữ liệu và triển khai

| Thao tác | Đầu vào chính | Đầu ra tối thiểu |
|---|---|---|
| Checkout | selected item IDs, contact, paymentMethod, voucher reference, idempotency key; identity từ JWT/header | orderId, CATALOG, PENDING_APPROVAL, subtotal/discount/finalPrice; guest access information |
| List/detail review | paging/filter thích hợp, orderId; manager JWT | Dữ liệu đơn, owner type, snapshot, current status/version, quyết định nếu đã có |
| Review | orderId, decision, reason khi reject, staffId tùy chọn, version | orderId, decision, current status, managerApprovedAt/rejectedAt, assignment nếu có |

Tên endpoint/lớp là thiết kế kỹ thuật cần ánh xạ khi triển khai; bản đặc tả không yêu cầu sửa application.yaml. Phạm vi hiện tại là tài liệu và sơ đồ.

Ghi dữ liệu nghiệp vụ, voucher, assignment và chuyển task phải nhất quán. Nếu Camunda không tham gia cùng transaction thì cần trạng thái bàn giao/retry rõ, không coi hai thao tác riêng mặc nhiên nguyên tử. Checkout lưu đơn trước khi khởi process phải có cơ chế đăng ký công việc bền vững hoặc retry tương đương.

Chưa chốt giới hạn độ dài reason/contact hoặc chi tiết rate-limit trong target thì không tự ghi con số như quy tắc nghiệp vụ. Dữ liệu legacy đã thanh toán trước review cần xử lý migration/ngoại lệ riêng, không tự dùng WF02 để hoàn tiền.

## 8. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả |
|---|---|---|
| TC01 | USER checkout catalog hợp lệ | Một Order PENDING_APPROVAL, snapshot đúng giá/time server |
| TC02 | Guest checkout đủ contact | userId null, token hash, email tạo đơn; không notification tài khoản giả |
| TC03 | USER dưới 20 điểm chọn COD | Bị từ chối; không tạo Order/payment |
| TC04 | Mẫu ngừng nhận sau khi thêm giỏ | Checkout bị từ chối, không trừ quota/mất giỏ |
| TC05 | Checkout một phần | Chỉ xóa mục đã đặt |
| TC06 | Checkout gửi trùng | Một Order, một usage, không phát lại nghiệp vụ |
| TC07 | Custom từ WF01 | Dùng đúng Order đã có, không tạo/sent email tạo đơn lần hai |
| TC08 | Người không có quyền manager đọc/duyệt | Bị từ chối |
| TC09 | Manager xem custom chưa có giá | Hiện chưa xác định, không hiển thị giá 0 như giá đã chốt |
| TC10 | Duyệt không chọn staff | PENDING_ASSIGNMENT và audit; không có email intermediate |
| TC11 | Duyệt kèm staff hợp lệ | Một assignment, bàn giao WF03; không tự chốt giá custom |
| TC12 | Staff bị nhận đồng thời bởi đơn khác | Conflict, không vượt một active assignment |
| TC13 | Custom được duyệt | Một deadline 24 giờ từ managerApprovedAt; retry không gia hạn |
| TC14 | Manager reject có lý do | CANCELLED, email lý do, không trừ reputation |
| TC15 | Guest voucher của đơn bị reject | Hoàn quota một lần, giữ giới hạn usage |
| TC16 | Hai review hoặc review/hủy đồng thời | Một kết quả trạng thái có hiệu lực, không ghi đè |
| TC17 | Email/engine lỗi sau commit | Đơn vẫn tồn tại, lỗi có trạng thái retry, không tạo/duyệt lại |
| TC18 | Đơn bất kỳ chỉ mới checkout/manager review | Không charge, không ORDER_CREATING, không inventory reservation |

## 9. Sơ đồ

- [Use Case tổng quan](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity theo từng UC](diagram.md).
- [Sequence theo từng UC](sequence.md).
