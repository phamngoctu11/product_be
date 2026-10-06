# WF05 — Sản xuất, checkpoint và KCS

Ngày lập: 05/10/2026.

WF05 nhận Order ở `ORDER_CREATING`, điều phối hai checkpoint theo từng loại sản phẩm/OrderItem, lưu toàn bộ attempt và vòng lặp rework. Chỉ khi checkpoint 2 của tất cả loại đạt KCS, hệ thống mới chuyển `READY_TO_SHIP` và release active assignment của staff.

Nguồn: [target.txt](../../target.txt), mục C4–C5, D, E/WF05, E1, F13, F18, H–J. Quy cách: [TUTORIAL.md](../TUTORIAL.md).

| File | Nội dung |
|---|---|
| [usecase.md](usecase.md) | UC05.1–UC05.3, checkpoint theo loại, rework, KCS, thông báo và release staff |
| [diagram.md](diagram.md) | Activity Diagram theo từng UC và luồng multi-instance tổng thể |
| [sequence.md](sequence.md) | Sequence Diagram cho INITIAL_SHAPE, FINAL_PRODUCT và manager KCS |
| [overview.svg](overview.svg) | Use Case Diagram tổng quát để xem/chèn báo cáo |
| [overview.puml](overview.puml) | Mã nguồn PlantUML tương ứng |

![Use Case tổng quát WF05](overview.svg)

Mở SVG bằng Chrome/Edge. Xem `diagram.md` và `sequence.md` bằng Markdown Preview hỗ trợ Mermaid.

Tên endpoint, service, event và task kỹ thuật trong tài liệu là thiết kế đề xuất cần ánh xạ khi triển khai. WF05 kết thúc ở `READY_TO_SHIP`; bàn giao vận chuyển thuộc WF07.
