# Bàn giao refactor mapper và ranh giới API

Ngày cập nhật: 08/10/2026  
Trạng thái: `VERIFIED`

## Mục tiêu

Chuẩn hóa việc chuyển đổi Entity/DTO, loại bỏ các đoạn dựng DTO và aggregate bị lặp trong service, đồng thời không để Entity đi trực tiếp qua REST/WebSocket API.

## Kết quả triển khai

- Tất cả MapStruct mapper dùng `CentralMapperConfig`, constructor injection và `unmappedTargetPolicy = ERROR`.
- Bỏ `CartItemMapper` trùng chức năng; `CartMapper` chịu trách nhiệm toàn bộ ánh xạ cart/item và fallback ảnh variant sang ảnh product.
- Tách mapper cho checkout response, notification, reputation history, consultation, product review, chat message, chat thread và chi tiết hoa hồng.
- Tách `OrderDetailsAssembler` để ghép trạng thái review theo batch; `OrderService` không còn tự truy vấn rồi sửa DTO.
- Tách `CatalogOrderFactory`, `CustomOrderFactory`, `UserFactory`; service điều phối nghiệp vụ thay vì tự gán từng trường aggregate.
- `ProductMapper` có ba mục đích cập nhật có tên rõ ràng: catalog đầy đủ, thông tin cơ bản và variant. Các trường kỹ thuật/id/delete/quantity không còn bị ghi đè ngầm.
- `OrderMapper` ưu tiên contact snapshot và item snapshot, chỉ fallback sang dữ liệu quan hệ cho bản ghi legacy.
- Chat REST/WebSocket dùng request/response DTO; manager/admin không được đọc phòng chat. Topic quản trị chung đã bị loại bỏ.
- Voucher campaign nhận request DTO và trả response DTO, không nhận/trả `VoucherTemplate` entity.
- `GUEST_ORDER_CREATED` chỉ được phát bởi guest checkout sau khi lưu đơn. Generic Camunda publish handler từ chối phát event này để tránh hai producer cho cùng một milestone.
- Các DTO tổng hợp cần phép tính nghiệp vụ như dashboard, voucher option, reorder result và commission summary tiếp tục được dựng tại service; đây không phải ánh xạ Entity/DTO thuần.

## Kiểm chứng

- `.\mvnw.cmd -DskipTests compile`: thành công, không có cảnh báo unmapped target của MapStruct.
- `.\mvnw.cmd test`: 218 test chạy, 0 failure, 0 error, 6 test tích hợp MySQL opt-in được skip.
- `git diff --check`: đạt; chỉ có cảnh báo line-ending LF/CRLF của môi trường Windows.

## Quy tắc tiếp tục áp dụng

1. Mapper chỉ chuyển đổi dữ liệu, không gọi repository/service.
2. Assembler được dùng khi response cần dữ liệu từ nhiều repository hoặc cần enrichment theo batch.
3. Factory tạo aggregate mới và thiết lập invariant ban đầu.
4. Service giữ validation, transaction, authorization và phép tính nghiệp vụ.
5. Controller/WebSocket không nhận hoặc trả persistence entity.
6. Mỗi event milestone chỉ có một producer sở hữu rõ ràng.
