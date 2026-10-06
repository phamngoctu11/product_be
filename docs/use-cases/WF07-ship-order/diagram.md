# WF07 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Đây là Activity Diagram, khác với Use Case Diagram actor/oval.

## Use Case tổng quát

![WF07 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC07.1 — Staff ghi nhận bàn giao vận chuyển

```mermaid
flowchart TD
    A([STAFF mở Order cần bàn giao]) --> B[Đọc Order, OrderItem,<br/>lịch sử assignment và KCS]
    B --> C{Là staff lịch sử<br/>đã phụ trách Order?}
    C -- Không --> X[Từ chối]
    C -- Có --> D{Order đang READY_TO_SHIP<br/>và version hiện hành?}
    D -- Không --> Y[Conflict hoặc trả kết quả đã có]
    D -- Có --> E[Nhập exportedQuantity<br/>cho đầy đủ từng OrderItem]
    E --> F{Đủ dòng, không trùng/lạ,<br/>số nguyên trong khoảng 0..quantity?}
    F -- Không --> Z[Validation lỗi,<br/>không lưu một phần]
    F -- Có --> G{Idempotency key đã xử lý?}
    G -- Cùng nội dung --> H[Trả kết quả SHIPPING đã lưu]
    G -- Khác nội dung --> Y
    G -- Chưa --> I[Khóa Order và OrderItem]
    I --> J[Transaction lưu exportedQuantity,<br/>actor, thời điểm và ghi chú]
    J --> K[Chuyển READY_TO_SHIP sang SHIPPING,<br/>ghi history và công việc sau commit]
    K --> L{Commit thành công?}
    L -- Không --> R[Rollback toàn bộ,<br/>Order vẫn READY_TO_SHIP]
    L -- Có --> M[Trả SHIPPING,<br/>shippingStartedAt và 2 ngày dự kiến]
    M --> N[Đồng bộ workflow và UI,<br/>không gửi email trạng thái SHIPPING]
    N --> O([Chờ WF08 xác nhận nhận hàng])
    X --> Q([Kết thúc không thay đổi])
    Y --> Q
    Z --> Q
    H --> Q
    R --> Q
```

Không kiểm tra assignment còn active vì staff đã được release ở WF05. Backend phải kiểm tra staff lịch sử đã phụ trách và không được release lần hai.

## Ranh giới hệ thống

```mermaid
flowchart LR
    A[WF05<br/>READY_TO_SHIP và release staff] --> B[WF07<br/>Ghi số lượng bàn giao]
    B --> C[SHIPPING<br/>ước tính vận chuyển 2 ngày]
    C --> D[WF08<br/>Khách ghi receivedQuantity]
    E[Đơn vị vận chuyển bên thứ ba] -. ngoài phạm vi<br/>không API, webhook, chat .- C
    F[Email trạng thái READY_TO_SHIP] --> A
    B -. không phát email<br/>trạng thái SHIPPING .-> G[Chỉ audit và cập nhật UI]
```

`SHIPPING` không chứng minh trạng thái thực tế ở hãng vận chuyển. Hệ thống chỉ ghi nhận rằng staff đã bàn giao theo dữ liệu đã nhập.
