# WF06 — Yêu cầu thay đổi chi tiết

Ngày lập: 06/10/2026.

WF06 cho USER đề nghị thay đổi chi tiết chưa triển khai sau khi đơn đã được chốt. STAFF đang phụ trách là người duy nhất chấp nhận hoặc từ chối. Luồng không đổi giá, voucher, deadline, trạng thái Order hoặc tạo thanh toán bổ sung.

Nguồn: [target.txt](../../target.txt), mục B–C, E/WF06, E1, F14, I–J. Quy cách: [TUTORIAL.md](../TUTORIAL.md).

| File | Nội dung |
|---|---|
| [usecase.md](usecase.md) | UC06.1–UC06.2, quyền, phạm vi thay đổi, version, quyết định và tiêu chí nghiệm thu |
| [diagram.md](diagram.md) | Activity Diagram cho tạo và xử lý ChangeRequest |
| [sequence.md](sequence.md) | Sequence Diagram cho submit, staff quyết định và xử lý sau commit |
| [overview.svg](overview.svg) | Use Case Diagram tổng quát để xem/chèn báo cáo |
| [overview.puml](overview.puml) | Mã nguồn PlantUML tương ứng |

![Use Case tổng quát WF06](overview.svg)

Mở SVG bằng Chrome/Edge. Xem `diagram.md` và `sequence.md` bằng Markdown Preview hỗ trợ Mermaid.

Tên endpoint, service, event và trạng thái kỹ thuật trong tài liệu là thiết kế đề xuất cần ánh xạ khi triển khai. WF06 là luồng phụ, không thay thế rework KCS của WF05.
