# Báo cáo kiểm thử — 24/10/2026

## Tóm tắt

Đã đăng nhập thành công bằng ba tài khoản được cung cấp: `admin` (MANAGER), `staff04` (STAFF), `user04` (USER). Backend `localhost:8080` và frontend `localhost:4200` hoạt động.

Luồng custom chính đã đi đến `DELIVERED`; nhánh khách tự hủy đơn trước khi duyệt đã đi đến `CANCELLED`. Tuy vậy, luồng tạo đơn custom trên giao diện đang bị chặn bởi lỗi chọn loại sản phẩm; để tiếp tục kiểm tra các bước sau, draft và đơn thử đã được tạo qua API có xác thực. Có ba lỗi mức P1 liên quan trực tiếp đến loại sản phẩm/thuộc tính custom cần xử lý trước khi xem chức năng này là sẵn sàng.

## Lỗi phát hiện

### P1 — Chọn loại sản phẩm không hiện các trường bắt buộc trên form custom

- Cách tái hiện: manager tạo một loại sản phẩm có trường bắt buộc, đăng nhập user04, mở form “Đặt làm riêng” và chọn loại vừa tạo.
- Thực tế: loại sản phẩm hiện trong danh sách và có thể được chọn, nhưng không có trường động nào xuất hiện. Form không thể gửi hợp lệ.
- Kỳ vọng: sau khi chọn loại, form hiển thị các trường do manager khai báo và cho nhập giá trị trước khi lưu/gửi.
- Dấu hiệu kỹ thuật: option dùng `[ngValue]` nhưng handler lấy giá trị DOM rồi so sánh bằng `Number(value)`. Giá trị native của option Angular có dạng `1: 1`, nên phép chuyển thành số không tìm thấy loại tương ứng. Tham chiếu: `D:\me\product_fe\src\app\features\storefront\custom-request\custom-request-form.component.html:46-58`; `...custom-request-form.component.ts:200-201`.
- Mức ảnh hưởng: P1 — người dùng không thể tạo yêu cầu custom từ giao diện trong khi tính năng mới phụ thuộc vào trường của `productType`.

### P1 — Thỏa thuận custom làm mất `productType` và giá trị thuộc tính khỏi snapshot đơn

- Tái hiện trên đơn #22: draft #11 được tạo với loại “Loại custom E2E 24-10”, chiều cao 18 cm, hình dáng “Tròn”; sau khi user và staff chốt thỏa thuận, gọi `GET /api/orders/22`.
- Thực tế: item trả `productTypeCode: null`, `productTypeName: null`, `productTypeFields: []`, `fieldValues: {}`. Form trao đổi/thỏa thuận của user và staff cũng không hiển thị loại sản phẩm hay các thuộc tính manager đã khai báo.
- Trước khi chốt thỏa thuận, draft #12/đơn #23 vẫn trả đầy đủ `productType`, schema trường và `fieldValues`; do đó dữ liệu ban đầu đã được lưu.
- Dấu hiệu kỹ thuật: snapshot thỏa thuận chỉ gồm mô tả, ảnh, số lượng, giá, thời lượng, phương thức thanh toán và ghi chú; sau đó snapshot này được gán đè lên `OrderItem.specSnapshot`. Tham chiếu: `src/main/java/com/example/workflow/service/OrderAgreementService.java:227-236,296`; mapper chỉ có thể trả các trường type nếu chúng còn trong snapshot: `src/main/java/com/example/workflow/mapper/OrderItemMapper.java:125-139`.
- Mức ảnh hưởng: P1 — khách và người chế tác không còn thấy ràng buộc cấu hình của loại sản phẩm sau khi thỏa thuận; có nguy cơ sản xuất sai yêu cầu.

### P1 — Yêu cầu chỉnh sửa không hỗ trợ thay đổi thuộc tính theo loại sản phẩm

- Tái hiện trên đơn #22 khi đang sản xuất: gửi yêu cầu có `fieldValues` (ví dụ thay chiều cao/hình dáng).
- Thực tế: backend trả HTTP 400: `Change request is invalid: Unsupported fields: fieldValues.` Giao diện chỉ có mô tả, lý do và ảnh tham chiếu; không có input cho `fieldValues`.
- Kết quả đối chứng: yêu cầu chỉ đổi mô tả được tạo và staff chấp nhận; `specVersion` tăng từ 1 lên 2, giá/số lượng/thời lượng/deadline giữ nguyên. Như vậy phần đổi mô tả chạy được, phần thuộc tính động chưa được triển khai.
- Tham chiếu: form FE `D:\me\product_fe\src\app\features\shared\change-request\change-request-panel.component.html:40-58`; request DTO chỉ khai báo mô tả/ảnh/lý do tại `src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java:17-24`; service từ chối trường ngoài DTO tại `src/main/java/com/example/workflow/service/ChangeRequestService.java:358-367`.
- Mức ảnh hưởng: P1 — không đáp ứng quy tắc đã thống nhất rằng change request có thể cập nhật thuộc tính của `productType` và mô tả.

### P2 — Đơn custom chưa thỏa thuận giá hiển thị 0 đồng và dự báo điểm hủy không khớp kết quả

- Tái hiện trên đơn #23 ở trạng thái `PENDING_APPROVAL`, chưa có thỏa thuận giá.
- Thực tế: lịch sử/chi tiết đơn và hộp thoại hủy hiển thị giá trị `0 đ`; hộp thoại báo tài khoản có 56 điểm và dự kiến bị trừ 1 điểm (còn 55). API hủy đơn trả `reputationPenalty: 0`.
- Kỳ vọng: trước khi chốt giá, giao diện thể hiện “Chưa thỏa thuận giá” (không tạo cảm giác đơn miễn phí) và dự báo điểm phải khớp với mức backend thực sự áp dụng.
- Tham chiếu tính dự báo FE: `D:\me\product_fe\src\app\features\storefront\orders\orders.ts:413-419,510`; mức backend: `src/main/java/com/example/workflow/service/OrderCancellationService.java:208,245-253`.

## Các luồng đã chạy

| Luồng | Kết quả |
|---|---|
| Tạo loại sản phẩm bằng admin | Đạt. Tạo type ID 1, mã `e2e-clay-20261024-018150`, gồm `chieu_cao` (NUMBER, 1–50 cm) và `hinh_dang` (SINGLE_SELECT: Tròn/Vuông). |
| Kiểm tra trường bắt buộc phía backend | Đạt. Tạo draft thiếu `fieldValues` bắt buộc bị trả 400; draft hợp lệ lưu đúng type và giá trị trường. |
| Đơn custom #22 | Draft #11 → `PENDING_APPROVAL` → manager duyệt → staff04 được phân công → user gửi thỏa thuận → staff xác nhận → `ORDER_ACCEPTED` → staff bắt đầu sản xuất (`ORDER_CREATING`). |
| Change request mô tả | Đạt ở service. Yêu cầu #4 được staff chấp nhận; `specVersion` 1 → 2, giá/số lượng/thời hạn giữ nguyên. Thay `fieldValues` bị lỗi như mô tả ở trên. |
| Checkpoint/KCS | Đạt. Checkpoint #17 tạo dáng tự đạt; checkpoint #18 bị yêu cầu làm lại; staff nộp lần 2 thành checkpoint #19; manager duyệt đạt. Đơn chuyển `READY_TO_SHIP`. |
| Bàn giao và nhận hàng | Đạt. Staff bàn giao 2/2 sản phẩm, đơn sang `SHIPPING`; user xác nhận nhận 2/2, đơn #22 sang `DELIVERED` (version 14). |
| Nhánh hủy đơn #23 | Backend đạt. Draft #12 được gửi thành đơn `PENDING_APPROVAL`; user hủy với lý do, API trả `CANCELLED`, nguồn `USER`, lưu lý do. Dialog hủy đã mở và hiển thị tác động điểm; thao tác xác nhận được gửi qua API có xác thực do tự động hóa trình duyệt không nhập được nội dung vào hộp thoại Angular. |
| Bộ lọc tag trên cửa hàng | Đạt ở mức khói. Chip được nạp từ `tags`; chọn chip gửi `keyword=com` và trả sản phẩm ID 1. Dữ liệu hiện chỉ có một sản phẩm với tên “Commision #01” và tag `com`, nên chưa đủ dữ liệu để kiểm tra nhiều tag/loại sản phẩm; nội dung này cũng chưa đáp ứng mục tiêu hiển thị hoàn toàn bằng tiếng Việt. |
| Chat custom | Socket hiển thị trực tuyến; tin nhắn thử được lưu. Chuỗi tiếng Việt trong một thao tác tự động bị môi trường PowerShell đổi thành dấu `?`, đây là lỗi dữ liệu kiểm thử, không tính là lỗi ứng dụng. |

## Giới hạn và dữ liệu kiểm thử

- Do lỗi P1 ở form chọn `productType`, draft #11 và #12 được tạo/gửi qua API có xác thực để tiếp tục kiểm thử các bước sau. Một số quyết định KCS/change request cũng được gọi qua API; giao diện manager đã được dùng để tra cứu và xem checkpoint.
- Chưa kiểm tra nhánh staff từ chối change request, manager từ chối đơn, hoặc hủy đơn sau khi đã thanh toán online/phân công staff.
- Các dữ liệu thử được giữ lại: type ID 1, draft #11/#12, đơn #22 `DELIVERED`, đơn #23 `CANCELLED`, change request #4 và checkpoint #17–#19. Không thay đổi các đơn cũ khác trong hàng chờ admin/staff.
- Mô tả/ghi chú thỏa thuận trên đơn #22 có ký tự `?` do encoding của script tự động; sau đó mô tả đã được thay bằng nội dung ASCII qua change request. Không dùng chuỗi này làm dữ liệu demo.

## Kết luận

Các service phía sau xử lý được luồng duyệt, phân công, thỏa thuận, sản xuất/KCS (kể cả làm lại), giao hàng, xác nhận nhận hàng và hủy đơn. Tuy nhiên, luồng custom chưa sẵn sàng cho người dùng thật cho đến khi sửa ba lỗi P1: form chọn type, bảo toàn snapshot type/field qua thỏa thuận, và hỗ trợ cập nhật thuộc tính động trong change request.
