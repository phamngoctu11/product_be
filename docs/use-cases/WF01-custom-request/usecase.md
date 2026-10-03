# WF01 — Quản lý bản nháp và gửi yêu cầu đặt sản phẩm custom

Ngày lập: 03/10/2026.

Nguồn nghiệp vụ: [target.txt](../../target.txt), mục E/WF01, F05, F18 và các quy tắc Order CUSTOM.

Đây là đặc tả hệ thống mục tiêu. Luồng đầu tiên được xác định theo danh mục workflow tại mục E, không phải danh sách chức năng F01 (xem catalog). Các tên trường kỹ thuật bên dưới là đề xuất để triển khai.

## 1. Phạm vi workflow

User có thể lưu nhiều yêu cầu custom chưa đặt, chọn một bản để gửi thành đơn. Workflow kết thúc khi bản nháp được lưu hoặc một Order CUSTOM được tạo ở `PENDING_APPROVAL` và được đưa vào hàng chờ manager review.

Manager duyệt khả năng làm thuộc WF02. Phân staff, mở chat, chốt giá/voucher/thời gian thuộc các workflow tiếp theo. WF01 không khởi động timer xác nhận custom 24 giờ, timer thanh toán 1 giờ hoặc sản xuất.

Workflow gồm ba use case để dễ vẽ và đặc tả:

| Mã | Use case | Kết quả |
|---|---|---|
| UC01.1 | Xem danh sách và chi tiết bản nháp custom | User đọc được các bản nháp thuộc mình |
| UC01.2 | Tạo/cập nhật bản nháp custom | Lưu nội dung và trả `draftId` |
| UC01.3 | Gửi bản nháp để tạo đơn custom | Tạo `Order CUSTOM / PENDING_APPROVAL`, trả `orderId` |

## 2. Tác nhân và quyền

| Tác nhân | Vai trò |
|---|---|
| USER | Tạo, sửa, đọc bản nháp của mình và xác nhận gửi yêu cầu tạo đơn |
| Guest | Không được thực hiện; giao diện hướng dẫn đăng nhập/đăng ký |
| Backend | Xác thực chủ sở hữu, kiểm tra dữ liệu, lưu snapshot và tạo đơn |
| Camunda | Tiếp nhận yêu cầu khởi chạy bước manager review sau khi đơn được lưu |
| Email/notification consumer | Gửi email và notification xác nhận tạo đơn cho user |
| MANAGER | Nhận đơn trong danh sách chờ duyệt; quyết định duyệt không nằm trong WF01 |

Trong Use Case Diagram toàn hệ thống, backend, Camunda và consumer là thành phần nội bộ, không cần vẽ thành actor bên ngoài. Dịch vụ email có thể là actor phụ nếu nằm ngoài ranh giới hệ thống.

## 3. UC01.1 — Xem bản nháp custom

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Tìm và xem lại yêu cầu custom chưa gửi |
| Tác nhân chính | USER |
| Kích hoạt | User mở mục “Yêu cầu custom của tôi” |
| Tiền điều kiện | User đã đăng nhập với quyền USER |
| Hậu điều kiện thành công | Trả danh sách/chi tiết thuộc đúng user; không thay đổi dữ liệu |
| Hậu điều kiện thất bại | Không lộ bản nháp của tài khoản khác |

| Bước | User | Hệ thống |
|---|---|---|
| 1 | Mở danh sách bản nháp | Lấy userId từ JWT, truy vấn các bản nháp thuộc user |
| 2 | Xem danh sách | Hiển thị định danh, mô tả tóm tắt, số lượng và thời điểm cập nhật |
| 3 | Chọn một bản nháp | Kiểm tra ownership và trả spec, ghi chú, ảnh tham khảo |
| 4 | Chọn sửa hoặc gửi | Chuyển tới UC01.2 hoặc UC01.3 |

Luồng thay thế: chưa có bản nháp thì trả danh sách rỗng và hiển thị hành động tạo mới. Bản nháp không tồn tại hoặc không thuộc user không được trả nội dung.

## 4. UC01.2 — Tạo/cập nhật bản nháp

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Lưu yêu cầu sản phẩm riêng để tiếp tục chỉnh sửa hoặc gửi sau |
| Tác nhân chính | USER |
| Kích hoạt | User chọn “Tạo yêu cầu custom” hoặc sửa bản nháp |
| Tiền điều kiện | Đăng nhập USER; khi sửa, bản nháp thuộc user |
| Hậu điều kiện thành công | Lưu bản nháp và trả `draftId` cùng nội dung đã lưu |
| Hậu điều kiện thất bại | Không ghi đè nội dung hợp lệ bằng một cập nhật lỗi |

| Bước | User | Hệ thống |
|---|---|---|
| 1 | Chọn tạo mới/sửa | Mở form trống hoặc nạp bản nháp sau khi kiểm tra quyền |
| 2 | Nhập chất liệu, màu, hình dáng, kích thước, số lượng và ghi chú | Hiển thị dữ liệu đang nhập |
| 3 | Chọn ảnh tham khảo nếu có | Nhận/tải ảnh và gắn tham chiếu hợp lệ vào bản nháp |
| 4 | Bấm “Lưu bản nháp” | Validate định dạng, ownership và dữ liệu được gửi |
| 5 | — | Lưu bản nháp của user; trả định danh, nội dung, thời điểm cập nhật |
| 6 | Tiếp tục sửa hoặc rời màn hình | Bản nháp vẫn được lưu, chưa tạo Order |

Luồng thay thế và ngoại lệ:

- User có thể tạo bản nháp khác; không giới hạn một bản nháp cho mỗi tài khoản trong yêu cầu hiện tại.
- Dữ liệu sai định dạng/số lượng không hợp lệ: trả lỗi đúng trường, không lưu bản cập nhật lỗi.
- Upload ảnh thất bại: thông báo ảnh chưa tải thành công; không gắn ảnh lỗi như một attachment hợp lệ.
- Chưa gửi Order: không phát email/notification `PENDING_APPROVAL`, không tạo task manager review.
- Nếu hỗ trợ sửa nội dung nguồn sau khi đã gửi, cập nhật đó không được thay snapshot của Order đã tạo. Chính sách cho phép sửa bản nguồn hay tạo bản sao là lựa chọn giao diện cần xác định; không biến cập nhật draft thành sửa Order.

## 5. UC01.3 — Gửi yêu cầu tạo đơn custom

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Chuyển một bản nháp được chọn thành đơn để manager đánh giá khả năng làm |
| Tác nhân chính | USER |
| Tác nhân phụ | Dịch vụ email; manager là bên nhận công việc ở workflow kế tiếp |
| Kích hoạt | User bấm xác nhận gửi yêu cầu tạo đơn |
| Tiền điều kiện | Đăng nhập USER; sở hữu bản nháp; dữ liệu gửi đáp ứng validation; không phải yêu cầu gửi trùng đã tạo đơn |
| Hậu điều kiện thành công | Một Order CUSTOM và một loại OrderItem được lưu ở `PENDING_APPROVAL`, liên kết bản nháp nguồn |
| Bảo đảm tối thiểu | Gửi lỗi không tạo đơn thiếu item/snapshot; retry không tạo thêm đơn ngoài ý muốn |

### Luồng chính

| Bước | User | Hệ thống |
|---|---|---|
| 1 | Mở bản nháp muốn đặt | Kiểm tra quyền và hiển thị nội dung hiện tại |
| 2 | Kiểm tra thông tin sản phẩm, số lượng và thông tin liên hệ/giao hàng | Hiển thị thông tin cần dùng cho Order; giá custom chưa phải giá đã chốt |
| 3 | Bấm “Gửi yêu cầu tạo đơn” | Nhận `draftId`, thông tin xác nhận và khóa chống gửi trùng |
| 4 | — | Xác định user từ JWT; kiểm tra ownership, dữ liệu, attachment và yêu cầu lặp |
| 5 | — | Chụp spec/ảnh/số lượng và thông tin liên hệ vào dữ liệu đơn |
| 6 | — | Trong transaction: tạo Order loại CUSTOM, trạng thái PENDING_APPROVAL; tạo một loại OrderItem; liên kết draft với Order và lưu dấu vết tạo đơn |
| 7 | — | Commit transaction; đăng ký khởi chạy manager review và sự kiện ORDER_CREATED |
| 8 | Nhận kết quả | Trả orderId, orderType, status và thông tin để mở trang Order |
| 9 | Mở MyOrder | Hiển thị đơn đang chờ manager; manager thấy đơn trong danh sách chờ duyệt |
| 10 | Nhận email/notification | Consumer gửi email tạo đơn thành công và notification cho user theo sự kiện sau commit |

Bước 10 là xử lý bất đồng bộ: phản hồi API không cần đợi email gửi xong. “Tạo đơn thành công” chỉ có nghĩa đã lưu đơn chờ duyệt, không có nghĩa manager đã nhận làm.

### Luồng thay thế và ngoại lệ

| Mã | Điểm phát sinh | Điều kiện | Xử lý và kết quả |
|---|---|---|---|
| A01 | Trước bước 1 | Guest/chưa đăng nhập | Không tạo custom; hướng dẫn đăng nhập/đăng ký |
| A02 | Bước 4 | Bản nháp không thuộc user | Từ chối, không trả dữ liệu riêng của người khác |
| A03 | Bước 4 | Thiếu/sai dữ liệu yêu cầu gửi | Trả lỗi cụ thể; user chỉnh bản nháp và gửi lại; chưa tạo Order |
| A04 | Bước 4 | Cùng lần gửi đã tạo Order | Trả kết quả Order đã tạo thay vì sinh thêm một đơn |
| A05 | Bước 4 | Cùng idempotency key nhưng payload khác | Trả lỗi xung đột; không áp nội dung mới lên đơn đã tạo |
| A06 | Bước 6 | Lưu database thất bại | Rollback các thay đổi transaction; không để Order thiếu item/liên kết nguồn |
| A07 | Sau bước 7 | Client mất kết nối trước khi nhận phản hồi | Retry cùng yêu cầu nhận lại orderId đã tạo |
| A08 | Sau bước 7 | Camunda chưa khởi chạy được | Order vẫn tồn tại, lưu trạng thái khởi chạy lỗi/chờ retry; không tạo lại Order để sửa process |
| A09 | Bước 10 | Gửi email/notification thất bại | Ghi nhận và retry; không rollback Order hoặc yêu cầu user đặt lại |

## 6. Dữ liệu đầu vào và đầu ra

| Dữ liệu | Ý nghĩa/điều kiện |
|---|---|
| draftId | Định danh bản nháp; bắt buộc khi đọc/sửa/gửi bản đã lưu |
| Spec | Chất liệu, màu, hình dáng, kích thước và ghi chú yêu cầu |
| quantity | Số nguyên dương khi gửi tạo Order; số lượng của cùng một loại custom |
| Attachments | Ảnh tham khảo hợp lệ thuộc yêu cầu, không tham chiếu tùy ý ảnh riêng của người khác |
| Contact snapshot | Họ tên, email, điện thoại, địa chỉ và ghi chú dùng cho đơn |
| Idempotency key | Khóa cho một lần gửi; server kiểm tra cùng user/bản nháp/nội dung |
| userId | Backend lấy từ JWT; không dùng userId client tự khai để cấp quyền |

`target.txt` liệt kê các trường spec/ảnh nhưng chưa chốt trường nào bắt buộc theo từng loại custom, số ảnh, dung lượng hoặc định dạng. Đội triển khai cần chốt schema validation trước nghiệm thu form; không tự ghi con số hoặc coi tất cả trường đều bắt buộc. Có thể tách validation lưu nháp và gửi đơn, nhưng mức cho phép thiếu ở bản nháp phải được thống nhất.

Đầu ra lưu nháp: `draftId`, dữ liệu đã lưu và thời điểm cập nhật.

Đầu ra gửi đơn tối thiểu:

```json
{
  "draftId": "<draft-id>",
  "orderId": "<order-id>",
  "orderType": "CUSTOM",
  "status": "PENDING_APPROVAL"
}
```

Đây là hợp đồng dữ liệu khái niệm, chưa quy định kiểu ID hay wrapper phản hồi. Không trả payment URL hoặc mô tả giá 0 như một giá đã chốt.

## 7. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR01 | Chỉ USER được tạo/gửi custom request |
| BR02 | Một user có nhiều bản nháp; chỉ bản được chọn được gửi |
| BR03 | Một Order custom có một loại item, quantity có thể lớn hơn một |
| BR04 | Có thể dùng chung logic tiếp nhận với đơn cart mà không đưa custom vào cart giả |
| BR05 | Snapshot Order không bị sửa theo bản nháp nguồn |
| BR06 | Order tạo thành công ở PENDING_APPROVAL, chưa ORDER_ACCEPTED |
| BR07 | Manager duyệt khả năng làm ở workflow tiếp theo; giá do user/staff chốt sau |
| BR08 | Không thu tiền hoặc sử dụng voucher custom tại bước gửi yêu cầu; chọn/áp voucher trong form thỏa thuận ở chat |
| BR09 | Không giữ/trừ/hoàn tồn kho thành phẩm |
| BR10 | Chỉ khởi timer custom PT24H sau manager duyệt; không khởi ở lưu nháp/gửi đơn |
| BR11 | Tạo Order phát email + notification cho user; lưu draft không phát thông báo tạo Order |
| BR12 | Retry không tạo thêm Order, task hay thông báo nghiệp vụ trùng |

## 8. Nội dung thông báo

Email tạo đơn có mã đơn, tóm tắt yêu cầu/số lượng, trạng thái chờ manager duyệt và link tới chi tiết. Không thông báo “đã nhận làm”, “đã thanh toán” hoặc ngày hoàn thành chính thức tại bước này.

Notification cho user có nội dung tương đương: “Yêu cầu custom cho đơn #{orderId} đã được tạo và đang chờ quản lý duyệt”. Câu chữ cụ thể có thể điều chỉnh, nhưng trạng thái phải đúng.

User có quyền hủy trước ORDER_ACCEPTED theo use case hủy; WF01 không thực hiện hủy. Link hoặc giao diện không thay kiểm tra quyền/trạng thái phía backend.

## 9. Sơ đồ liên quan

- [Use Case tổng quan (SVG)](overview.svg)
- [Sơ đồ hoạt động theo từng UC](diagram.md)
- [Biểu đồ tuần tự theo từng UC](sequence.md)
- [Mã nguồn Use Case tổng quan](overview.puml)

## 10. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | Guest gọi API custom | Bị từ chối, không tạo draft/Order |
| TC02 | User lưu hai bản nháp | Hai bản thuộc cùng user, chưa có Order từ việc lưu |
| TC03 | User đọc/sửa draft tài khoản khác | Không được truy cập hoặc thay đổi |
| TC04 | Gửi một draft hợp lệ | Một Order CUSTOM ở PENDING_APPROVAL, liên kết nguồn, một loại item |
| TC05 | Quantity bằng 0/âm/không nguyên khi gửi | Trả lỗi, chưa tạo Order |
| TC06 | Gửi cùng yêu cầu hai lần/đồng thời | Không tạo hai Order; trả lại kết quả hoặc xung đột có kiểm soát |
| TC07 | Sửa nội dung draft nguồn | Snapshot Order đã tạo không đổi |
| TC08 | Database lỗi lúc tạo item | Không để lại Order tạo dở |
| TC09 | Email/process lỗi sau commit | Order vẫn xem được; lỗi có trạng thái retry, không tạo lại đơn |
| TC10 | Gửi custom thành công | Email + notification có nội dung chờ duyệt, không phải đã nhận làm |
| TC11 | Chỉ lưu draft | Không gửi email PENDING_APPROVAL, không mở task duyệt |
| TC12 | Đơn vừa được gửi | Chưa payment, chưa chat/phân công, chưa production và chưa timer 24 giờ |

Các yêu cầu chi tiết validation spec/attachment tại mục 6 cần được chốt để bổ sung test dữ liệu bắt buộc. Những quyết định đó không thay đổi quyền sở hữu, trạng thái hoặc ranh giới workflow đã mô tả.
