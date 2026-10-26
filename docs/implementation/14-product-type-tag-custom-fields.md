# Loại sản phẩm, từ khóa và trường custom động

Ngày cập nhật: 24/10/2026

## Quy tắc đã chốt

- Manager tạo `ProductType` và định nghĩa các trường cần thiết để mô tả sản phẩm thuộc loại đó. Mọi trường trong schema đều bắt buộc; không còn cờ `required`. Hướng dẫn hoặc ngoại lệ khác được ghi trong mô tả tự do.
- Khi tạo draft custom, khách chọn một loại sản phẩm. Sau khi lưu draft, không thể đổi loại: yêu cầu làm đồ gốm không thể biến thành yêu cầu làm đồ đan. Draft giữ ảnh chụp schema để các thay đổi sau này ở cấu hình manager không làm đổi nội dung đã chọn.
- Khi cập nhật draft, khách chỉ sửa giá trị trường của loại đã chọn, mô tả tự do (tối đa 4.000 từ), số lượng và ảnh. Các giá trị trường được validate theo kiểu dữ liệu, giới hạn và danh sách lựa chọn.
- Từ khóa sản phẩm là `List<String>` nằm ngay trên Product. Không có entity/tagId/taxonomy riêng; bộ tìm kiếm `keyword` đối chiếu tên sản phẩm và từ khóa sản phẩm.
- Chỉ manager/admin tạo hoặc sửa sản phẩm đầy đủ. Các endpoint staff sửa thông tin cơ bản và trạng thái sản phẩm đã bị gỡ.
- Cờ `handmade` đã bị xóa khỏi sản phẩm, dòng đơn, DTO và điều kiện Change Request. Mọi sản phẩm thuộc shop handmade; khả năng gửi Change Request tiếp tục được giới hạn theo trạng thái sản xuất.
- Đã bỏ dữ liệu thời gian/phiên bản ProductType, cùng thời điểm lên kế hoạch và lý do staff bắt đầu muộn. Thời hạn vận hành được dùng bởi các job hủy tự động vẫn cần được giữ lại.

## Triển khai

- API ProductType: `GET /api/product-types`, `GET /api/product-types/management`, `POST/PUT /api/product-types`.
- API sản phẩm: `GET /api/products?keyword=...`; tìm kiếm từ khóa bao gồm tag trong danh sách sản phẩm.
- `UpdateCustomRequest` không còn nhận `productTypeId`. Giao diện khóa bộ chọn loại khi đang sửa draft.
- Changelog mới: `15-product-model-cleanup.xml`; các changelog cũ không bị sửa. Changelog xóa cờ handmade, thông tin kế hoạch/lý do bắt đầu muộn, metadata ProductType và các bảng tag độc lập.

## Kiểm tra

- Backend `./mvnw -q -DskipTests compile`: thành công.
- Frontend `npm.cmd run build`: thành công.
- Chưa xác minh changelog trên MySQL runtime. Bộ `test-compile` backend hiện còn lỗi độc lập ở các test cũ truyền `double` vào API tiền `BigDecimal`; cần sửa các test đó trước khi chạy toàn bộ suite.

## Việc còn lại

- Hoàn tất việc loại bỏ optimistic `expectedVersion` khỏi các luồng workflow còn lại; hiện các trường ProductType đã bỏ nhưng các luồng Order/Change Request vẫn giữ kiểm tra phiên bản để tránh mất cập nhật đồng thời.
- Rà soát toàn bộ timestamp theo từng nghiệp vụ và chỉ xóa sau khi chứng minh không còn phụ thuộc; hiện các timestamp phục vụ deadline/hủy tự động, hội thoại và tiến độ vẫn được giữ.
- Chạy migration và integration test trên MySQL/Redis khi các dịch vụ khả dụng.
