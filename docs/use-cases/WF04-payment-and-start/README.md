# WF04 — Thanh toán và bắt đầu sản xuất

Ngày lập: 05/10/2026.

WF04 nhận Order ở `ORDER_ACCEPTED`, tách nhánh COD/ONLINE, quản lý PaymentAttempt và timer ONLINE 1 giờ, rồi chờ staff phụ trách chủ động bấm bắt đầu. Kết quả thành công là `ORDER_CREATING` và `productionStartedAt` được ghi đúng một lần.

Nguồn: [target.txt](../../target.txt), mục C5, D, E/WF04, E1, F10–F12, F16, F18, H, I và J. Quy cách: [TUTORIAL.md](../TUTORIAL.md).

| File | Nội dung |
|---|---|
| [usecase.md](usecase.md) | UC04.1–UC04.3, COD/ONLINE, webhook, timer, start production và tiêu chí nghiệm thu |
| [diagram.md](diagram.md) | Activity Diagram theo từng UC và luồng tổng thể |
| [sequence.md](sequence.md) | Sequence Diagram cho mở payment, webhook/timeout và staff bắt đầu |
| [overview.svg](overview.svg) | Use Case Diagram tổng quát để xem/chèn báo cáo |
| [overview.puml](overview.puml) | Mã nguồn PlantUML tương ứng |

![Use Case tổng quát WF04](overview.svg)

Mở SVG bằng Chrome/Edge. Xem `diagram.md` và `sequence.md` bằng Markdown Preview hỗ trợ Mermaid.

Tên endpoint, service, event và task kỹ thuật trong tài liệu là thiết kế đề xuất cần ánh xạ khi triển khai. WF04 không thực hiện checkpoint, KCS hoặc READY_TO_SHIP, các bước đó thuộc WF05.
