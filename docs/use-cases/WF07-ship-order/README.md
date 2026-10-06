# WF07 — Bàn giao vận chuyển

Ngày lập: 06/10/2026.

WF07 cho staff đã phụ trách sản xuất ghi nhận một lần số lượng thực tế bàn giao cho đơn vị vận chuyển bên thứ ba. Kết quả là Order chuyển từ `READY_TO_SHIP` sang `SHIPPING`. Hệ thống không quản lý tài khoản, trao đổi, tracking, webhook hay đối soát của shipper.

Nguồn: [target.txt](../../target.txt), mục B2–B3, C2, D1–D3, E/WF07, F15, F18, H–J. Quy cách: [TUTORIAL.md](../TUTORIAL.md).

| File | Nội dung |
|---|---|
| [usecase.md](usecase.md) | UC07.1, quyền thao tác sau khi release staff, dữ liệu bàn giao, chuyển trạng thái và tiêu chí nghiệm thu |
| [diagram.md](diagram.md) | Activity Diagram ghi nhận bàn giao |
| [sequence.md](sequence.md) | Sequence Diagram kiểm tra quyền, commit và đồng bộ workflow |
| [overview.svg](overview.svg) | Use Case Diagram tổng quát để xem/chèn báo cáo |
| [overview.puml](overview.puml) | Mã nguồn PlantUML tương ứng |

![Use Case tổng quát WF07](overview.svg)

Mở SVG bằng Chrome/Edge. Xem `diagram.md` và `sequence.md` bằng Markdown Preview hỗ trợ Mermaid.

Tên endpoint, service, event và trạng thái kỹ thuật trong tài liệu là thiết kế đề xuất cần ánh xạ khi triển khai. Việc khách tra cứu và xác nhận số lượng thực nhận thuộc WF08.
