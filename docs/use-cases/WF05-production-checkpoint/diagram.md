# WF05 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Đây là Activity Diagram, khác với Use Case Diagram actor/oval.

## Use Case tổng quát

![WF05 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC05.1 — Staff nộp checkpoint hình ảnh đầu tiên

```mermaid
flowchart TD
    A([Staff gửi INITIAL_SHAPE]) --> B{Là assigned staff<br/>và assignment còn active?}
    B -- Không --> X[Từ chối]
    B -- Có --> C{Order đang ORDER_CREATING<br/>và item thuộc Order?}
    C -- Không --> X
    C -- Có --> D{Task INITIAL_SHAPE còn mở<br/>và ảnh hợp lệ?}
    D -- Không --> Y[Trả lỗi, task vẫn mở]
    D -- Có --> E{Request đã xử lý?}
    E -- Có --> F[Trả checkpoint đã lưu,<br/>không tạo bản thứ hai]
    E -- Chưa --> G[Transaction lưu checkpoint, ảnh,<br/>attempt, audit và công việc bàn giao]
    G --> H{Commit thành công?}
    H -- Không --> R[Rollback, không hoàn tất task]
    H -- Có --> I[Hoàn tất task INITIAL_SHAPE,<br/>nhánh item tiếp tục FINAL_PRODUCT]
    I --> J[Gửi email ảnh tiến trình cho USER hoặc Guest]
    J --> K{Chủ đơn là USER?}
    K -- Có --> L[Notification: Đã có hình ảnh đầu tiên<br/>của item #orderItemId]
    K -- Không --> M[Không tạo notification tài khoản]
    L --> N([Order vẫn ORDER_CREATING])
    M --> N
    X --> Z([Kết thúc không thay đổi])
    Y --> Z
    F --> Z
    R --> Z
```

Checkpoint 1 không có manager/customer approval. Lỗi email được retry sau commit và không chặn staff tiếp tục sản xuất.

## UC05.2 — Staff nộp sản phẩm hoàn thiện

```mermaid
flowchart TD
    A([Staff gửi FINAL_PRODUCT]) --> B{Đúng staff, Order và OrderItem?}
    B -- Không --> X[Từ chối]
    B -- Có --> C{INITIAL_SHAPE đã hoàn tất?}
    C -- Không --> Y[Chưa được nộp final]
    C -- Có --> D{Có attempt đang PENDING_REVIEW?}
    D -- Có --> E[Không tạo attempt mới,<br/>trả trạng thái đang chờ KCS]
    D -- Không --> F{Task FINAL_PRODUCT còn mở<br/>và ảnh hợp lệ?}
    F -- Không --> Y
    F -- Có --> G[Sinh attempt = previousMax + 1<br/>bằng cơ chế an toàn]
    G --> H[Transaction lưu checkpoint, ảnh,<br/>PENDING_REVIEW và công việc bàn giao]
    H --> I{Commit thành công?}
    I -- Không --> R[Rollback, task vẫn mở]
    I -- Có --> J[Hoàn tất staff task,<br/>tạo task manager KCS]
    J --> K[Thông báo nội bộ cho MANAGER]
    K --> L([Order vẫn ORDER_CREATING,<br/>staff chưa được release])
    X --> Z([Kết thúc])
    Y --> Z
    E --> Z
    R --> Z
```

Sau khi manager yêu cầu rework, UC05.2 được thực hiện lại cho đúng OrderItem và sinh attempt mới. Attempt cũ không bị ghi đè.

## UC05.3 — Manager KCS sản phẩm hoàn thiện

```mermaid
flowchart TD
    A([Manager gửi quyết định KCS]) --> B{Có quyền MANAGER?}
    B -- Không --> X[Từ chối]
    B -- Có --> C[Khóa Order, OrderItem,<br/>checkpoint và version]
    C --> D{Attempt mới nhất đang PENDING_REVIEW?}
    D -- Không --> Y[Conflict hoặc trả quyết định hiện hành]
    D -- Có --> E{Quyết định}
    E -- REWORK_REQUIRED --> F{Có reason?}
    F -- Không --> W[Validation lỗi, task vẫn chờ]
    F -- Có --> G[Transaction lưu decision, reason<br/>và trạng thái REWORK_REQUIRED]
    G --> H[Order giữ ORDER_CREATING,<br/>assignment vẫn active]
    H --> I[Tạo lại task FINAL_PRODUCT<br/>cho đúng OrderItem]
    I --> J[Thông báo staff kèm reason]
    E -- PASS --> K[Transaction lưu decision PASS<br/>và checkpoint PASSED]
    K --> L[Đọc kết quả checkpoint 2<br/>của toàn bộ OrderItem]
    L --> M{Tất cả loại đã PASS?}
    M -- Không --> N[Hoàn tất nhánh item hiện tại,<br/>Order vẫn ORDER_CREATING]
    M -- Có --> O[Trong cùng transaction chuyển READY_TO_SHIP,<br/>đóng đúng assignment và release staff]
    O --> P[Lưu history và event ORDER_READY_TO_SHIP]
    P --> Q[Email USER hoặc Guest,<br/>USER thêm notification]
    Q --> R([Kết thúc WF05])
    X --> Z([Kết thúc không thay đổi])
    Y --> Z
    W --> Z
    J --> V([Quay lại UC05.2])
    N --> U([Chờ các loại còn lại])
```

Hai nhánh cuối PASS gần đồng thời phải khóa Order và điều kiện hội tụ để chỉ một lần chuyển `READY_TO_SHIP`, release assignment và phát event.

## Điều phối toàn WF05 theo loại sản phẩm

```mermaid
flowchart TD
    A[WF04: ORDER_CREATING] --> B[Chụp collection orderItemId]
    B --> C{{Multi-instance theo từng OrderItem}}
    C --> D[UC05.1 INITIAL_SHAPE]
    D --> E[Gửi báo cáo tiến trình,<br/>không chờ duyệt]
    E --> F[UC05.2 FINAL_PRODUCT attempt]
    F --> G[UC05.3 Manager KCS]
    G -- Rework --> F
    G -- Pass --> H[Hoàn tất nhánh OrderItem]
    H --> I{Mọi nhánh đã pass?}
    I -- Chưa --> C
    I -- Rồi --> J[READY_TO_SHIP và release staff<br/>trong một transaction]
    J --> K[Email hoặc notification chủ đơn]
    K --> L[WF07: bàn giao vận chuyển]
```

Multi-instance có thể điều phối các loại độc lập nhưng không đồng nghĩa một staff vật lý làm song song. Quantity không được dùng làm loop cardinality.
