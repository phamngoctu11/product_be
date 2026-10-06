# WF08 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Đây là Activity Diagram, khác với Use Case Diagram actor/oval.

## Use Case tổng quát

![WF08 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC08.1 — Tra cứu thông tin Order để nhận hàng

```mermaid
flowchart TD
    A([USER hoặc Guest mở Order]) --> B{Loại người truy cập}
    B -- USER --> C[Đọc userId từ JWT]
    B -- Guest --> D[Nhận orderId và guest token]
    C --> E{USER là owner?}
    D --> F{Token đúng Order, scope,<br/>expiry, revocation và rate limit?}
    E -- Không --> X[Từ chối]
    F -- Không --> X
    E -- Có --> G[Đọc Order, OrderItem<br/>và dữ liệu bàn giao]
    F -- Có --> G
    G --> H[Lọc dữ liệu nội bộ,<br/>tạo snapshot theo quyền]
    H --> I{Order đang SHIPPING<br/>và exportedQuantity hợp lệ?}
    I -- Có --> J[Trả canConfirmReceipt=true<br/>và form số lượng]
    I -- Không --> K[Trả canConfirmReceipt=false<br/>và trạng thái hiện hành]
    J --> L([Có thể sang UC08.2])
    K --> M([Chỉ xem thông tin])
    X --> N([Không lộ snapshot Order])
```

Link Guest được gửi ở `READY_TO_SHIP` có thể mở trang nhưng chưa được xác nhận. Chỉ trạng thái `SHIPPING` mới bật thao tác UC08.2.

## UC08.2 — Xác nhận số lượng thực nhận

```mermaid
flowchart TD
    A([USER hoặc Guest gửi receipt]) --> B{Đúng owner hoặc<br/>guest token có scope xác nhận?}
    B -- Không --> X[Từ chối]
    B -- Có --> C[Khóa Order, OrderItem<br/>và idempotency key]
    C --> D{Order đang SHIPPING<br/>và version hiện hành?}
    D -- Không --> Y[Conflict hoặc trả kết quả đã hoàn tất]
    D -- Có --> E{Đủ item, không trùng/lạ,<br/>receivedQuantity là số nguyên không âm?}
    E -- Không --> Z[Validation lỗi,<br/>không lưu một phần]
    E -- Có --> F{Có exportedQuantity hiệu lực<br/>cho mọi item?}
    F -- Không --> R[Lỗi dữ liệu,<br/>không hoàn tất]
    F -- Có --> G[So sánh receivedQuantity<br/>với exportedQuantity]
    G --> H{Mọi dòng khớp?}
    H -- Có --> M[Chuẩn bị receipt matched=true]
    H -- Không --> I[Tạo danh sách mismatches]
    I --> J{acceptMismatch=true?}
    J -- Không --> K[Trả matched=false, completed=false,<br/>Order vẫn SHIPPING]
    J -- Có --> N[Chuẩn bị receipt acceptedMismatch=true<br/>và lưu note nếu có]
    M --> O[Transaction lưu mọi receivedQuantity,<br/>receipt, actor và endOrderTime]
    N --> O
    O --> P[Chuyển DELIVERED, ghi history,<br/>outbox và idempotency result]
    P --> Q{Commit thành công?}
    Q -- Không --> S[Rollback toàn bộ,<br/>Order vẫn SHIPPING]
    Q -- Có --> T[Trả matched, completed=true,<br/>mismatches và DELIVERED]
    T --> U[Đồng bộ workflow/read model,<br/>USER cộng 2 reputation một lần]
    U --> V{Order CATALOG?}
    V -- Có --> W[Mở quyền và gửi lời mời đánh giá]
    V -- Không --> AA[Không có đánh giá CUSTOM]
    W --> AB([Kết thúc])
    AA --> AB
    X --> AC([Không thay đổi])
    Y --> AC
    Z --> AC
    R --> AC
    K --> AD([Khách xem lại hoặc chủ động chấp nhận lệch])
    S --> AC
```

`matched=false, completed=false` là kết quả nghiệp vụ hợp lệ, không phải lỗi hệ thống. Hệ thống không tự tạo complaint hoặc tự hoàn tất khi khách im lặng.

## Luồng tổng thể WF08

```mermaid
flowchart LR
    A[WF07<br/>SHIPPING và exportedQuantity] --> B[UC08.1<br/>Tra cứu theo quyền]
    B --> C[UC08.2<br/>Nhập receivedQuantity]
    C -- Khớp --> D[DELIVERED]
    C -- Lệch, chưa chấp nhận --> E[Giữ SHIPPING<br/>trả mismatches]
    E --> C
    C -- Lệch, đã chấp nhận --> D
    D --> F[USER +2 reputation một lần]
    D --> G[CATALOG mở quyền đánh giá]
    D -. không có email<br/>trạng thái DELIVERED .-> H[Audit và cập nhật UI]
```
