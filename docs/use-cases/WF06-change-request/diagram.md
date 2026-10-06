# WF06 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Đây là Activity Diagram, khác với Use Case Diagram actor/oval.

## Use Case tổng quát

![WF06 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC06.1 — User tạo yêu cầu thay đổi

```mermaid
flowchart TD
    A([USER gửi ChangeRequest]) --> B{Đúng owner và là USER?}
    B -- Không --> X[Từ chối]
    B -- Có --> C[Đọc Order, OrderItem,<br/>spec hiện hành và assignment]
    C --> D{Order đã có spec chính thức<br/>và còn trạng thái được phép?}
    D -- Không --> Y[Conflict, không tạo request]
    D -- Có --> E{Base version còn hiện hành?}
    E -- Không --> Y
    E -- Có --> F{Chi tiết chưa triển khai<br/>và field nằm trong phạm vi?}
    F -- Không --> Z[Từ chối, không dùng ChangeRequest<br/>để sửa phần đã làm]
    F -- Có --> G{Có sửa giá, voucher, payment,<br/>duration hoặc deadline?}
    G -- Có --> Z
    G -- Không --> H{Idempotency key đã dùng?}
    H -- Cùng nội dung --> I[Trả ChangeRequest hiện có]
    H -- Khác nội dung --> Y
    H -- Chưa --> J[Transaction lưu PENDING_REVIEW,<br/>base/proposed data, actor và công việc thông báo]
    J --> K{Commit thành công?}
    K -- Không --> R[Rollback, không tạo task staff]
    K -- Có --> L[Thông báo assigned staff,<br/>tạo task phụ nếu dùng Camunda]
    L --> M([Order, spec, giá và deadline chưa đổi])
    X --> Q([Kết thúc])
    Y --> Q
    Z --> Q
    I --> Q
    R --> Q
```

ChangeRequest là một bản đề nghị, không phải bản cập nhật spec. Chỉ UC06.2 ACCEPT mới tạo applied spec version.

## UC06.2 — Staff quyết định yêu cầu thay đổi

```mermaid
flowchart TD
    A([STAFF gửi quyết định]) --> B{Là assigned staff<br/>và assignment còn active?}
    B -- Không --> X[Từ chối]
    B -- Có --> C[Khóa ChangeRequest, Order,<br/>current spec và progress]
    C --> D{Request còn PENDING_REVIEW<br/>và version hiện hành?}
    D -- Không --> Y[Conflict hoặc trả quyết định cũ]
    D -- Có --> E{Quyết định}
    E -- REJECT --> F{Có reason?}
    F -- Không --> W[Validation lỗi, request vẫn chờ]
    F -- Có --> G[Transaction ghi REJECTED,<br/>reason, actor và audit]
    G --> H[Giữ spec đang áp dụng,<br/>Order tiếp tục như trước]
    E -- ACCEPT --> I{Chi tiết vẫn chưa triển khai<br/>và base spec chưa thay đổi?}
    I -- Không --> J[Không được ACCEPT,<br/>yêu cầu xử lý conflict hoặc reject]
    I -- Có --> K{Thay đổi vẫn giữ nguyên<br/>giá và deadline?}
    K -- Không --> J
    K -- Có --> L[Transaction tạo applied spec version,<br/>liên kết ChangeRequest và ghi APPLIED]
    L --> M[Giữ Order status, payment,<br/>productionStartedAt, deadline và assignment]
    H --> N[Hoàn tất task phụ,<br/>notification kết quả cho USER]
    M --> N
    N --> O([Main flow tiếp tục, không bị ngắt])
    X --> Q([Kết thúc không thay đổi])
    Y --> Q
    W --> Q
    J --> Q
```

Staff không có lựa chọn tăng giá, tạo payment hoặc đổi deadline trong UC06.2. Nếu cần các thay đổi đó, hệ thống hiện chưa có workflow xử lý.

## Liên kết WF06 với workflow chính

```mermaid
flowchart LR
    A[Order đã chốt spec<br/>ORDER_ACCEPTED hoặc ORDER_CREATING] --> B[UC06.1 USER tạo ChangeRequest]
    B --> C[Task phụ PENDING_REVIEW]
    C --> D[UC06.2 STAFF quyết định]
    D -- REJECTED --> E[Giữ spec hiện hành]
    D -- APPLIED --> F[Tạo applied spec version mới]
    E --> G[Main flow tiếp tục]
    F --> G
    H[WF04 hoặc WF05] -. không bị ngắt toàn cục .-> G
```

Nếu dùng Camunda, ChangeRequest phù hợp với non-interrupting event subprocess. Không tạo process vòng đời Order thứ hai và không quay ngược trạng thái Order.
