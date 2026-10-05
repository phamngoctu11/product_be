# Đặc tả use case theo workflow

Trước khi tạo hoặc sửa tài liệu, đọc [TUTORIAL.md](TUTORIAL.md). Nguồn nghiệp vụ: [target.txt](../target.txt).

| Workflow | Nội dung | Mở tài liệu |
|---|---|---|
| WF01 — Custom Request | Bản nháp và gửi yêu cầu tạo đơn custom | [Folder tài liệu](WF01-custom-request/README.md) |
| WF02 — Tiếp nhận và duyệt đơn | Checkout catalog, xem đơn chờ và manager quyết định tiếp nhận | [Folder tài liệu](WF02-accept-order/README.md) |
| WF03 — Phân công và xác nhận đơn | Assign/claim staff, chat, form thỏa thuận, timer custom và xác nhận ORDER_ACCEPTED | [Folder tài liệu](WF03-agree-order/README.md) |
| WF04 — Thanh toán và bắt đầu sản xuất | COD/ONLINE, payment timer 1 giờ, webhook và staff chuyển ORDER_CREATING | [Folder tài liệu](WF04-payment-and-start/README.md) |

Mỗi workflow có đặc tả, sơ đồ hoạt động, biểu đồ tuần tự và hình Use Case tổng quan trong cùng một folder.
