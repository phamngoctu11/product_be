# WF09 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Đây là Activity Diagram, khác với Use Case Diagram actor/oval.

## Use Case tổng quát

![WF09 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC09.1 — Khách chủ động hủy Order

```mermaid
flowchart TD
    A([USER hoặc Guest yêu cầu hủy]) --> B{Xác thực}
    B -- USER --> C{JWT owner hợp lệ?}
    B -- Guest --> D{Token đúng Order<br/>và scope cancel?}
    C -- Không --> X[Từ chối]
    D -- Không --> X
    C -- Có --> E[Kiểm tra rate limit và khóa Order]
    D -- Có --> E
    E --> F{Trạng thái trước ORDER_ACCEPTED?}
    F -- Không --> Y[Conflict, không hủy]
    F -- Có --> G{Idempotency key đã xử lý?}
    G -- Cùng nội dung --> H[Trả kết quả CANCELLED cũ]
    G -- Khác nội dung --> Y
    G -- Chưa --> I{USER có finalPrice chính thức?}
    I -- Không hoặc Guest --> L[Penalty bằng 0]
    I -- Có --> J[Tính penalty theo finalPrice]
    J --> K{Reputation đủ?}
    K -- Không --> Z[Lỗi nghiệp vụ, không hủy]
    K -- Có --> M[Chuẩn bị reputation ledger]
    L --> N[Transaction hủy]
    M --> N
    N --> O[Ghi CANCELLED, endOrderTime,<br/>source, reason và history]
    O --> P[Phục hồi voucher/quota,<br/>release active assignment và tạo outbox]
    P --> Q{Commit thành công?}
    Q -- Không --> R[Rollback toàn bộ]
    Q -- Có --> S[Trả kết quả hủy]
    S --> T[Dừng task/timer, gửi thông báo<br/>và invalidate cache]
    T --> U([Kết thúc])
    X --> V([Không thay đổi])
    Y --> V
    H --> V
    Z --> V
    R --> V
```

GET từ link email Guest chỉ mở trang xác nhận. Chỉ mutation sau hành động xác nhận mới đi vào activity trên.

## UC09.2 — Manager từ chối Order

```mermaid
flowchart TD
    A([MANAGER chọn REJECT]) --> B{Có quyền và task<br/>manager review hiện hành?}
    B -- Không --> X[Từ chối]
    B -- Có --> C{Có reason hợp lệ?}
    C -- Không --> Y[Validation lỗi, task vẫn chờ]
    C -- Có --> D[Khóa Order và task version]
    D --> E{Order còn PENDING_APPROVAL?}
    E -- Không --> Z[Conflict hoặc trả quyết định hiện hành]
    E -- Có --> F[Server đặt source MANAGER_REJECTED<br/>và penalty bằng 0]
    F --> G[Transaction ghi CANCELLED, history,<br/>voucher/quota và assignment nếu có]
    G --> H[Tạo outbox và kết quả idempotent]
    H --> I{Commit thành công?}
    I -- Không --> R[Rollback, task chưa hoàn tất]
    I -- Có --> J[Hoàn tất nhánh reject của manager task]
    J --> K[Gửi email/notification chủ đơn<br/>và đồng bộ hàng công việc]
    K --> L([Kết thúc])
    X --> M([Không thay đổi])
    Y --> M
    Z --> M
    R --> M
```

UC09.2 không tạo quyền cho MANAGER hủy Order ở các trạng thái sau manager review.

## UC09.3 — Hệ thống hủy do timeout/payment fail

```mermaid
flowchart TD
    A([Timer hoặc payment fail kích hoạt]) --> B{Nguồn hệ thống hợp lệ<br/>và có reference?}
    B -- Không --> X[Từ chối, ghi lỗi quan sát]
    B -- Có --> C[Khóa Order, wait state<br/>và PaymentAttempt nếu liên quan]
    C --> D{Loại nguồn}
    D -- CUSTOM PT24H --> E{Form hợp lệ đã lưu?}
    E -- Có --> F[Timer stale, không hủy]
    E -- Không --> G{Vẫn ở bước chờ đúng reference?}
    D -- PAYMENT PT1H hoặc fail --> H{Payment đã PAID<br/>hoặc success đã thắng?}
    H -- Có --> I[Không hủy, ghi stale/đối soát nếu cần]
    H -- Không --> J{Order còn ORDER_ACCEPTED<br/>và attempt đúng reference?}
    G -- Không --> K[Không hủy]
    J -- Không --> K
    G -- Có --> L[Đặt reason CUSTOM_CONFIRMATION_TIMEOUT]
    J -- Có --> M[Đặt reason PAYMENT_TIMEOUT<br/>hoặc PAYMENT_FAILED]
    L --> N[Transaction CANCELLED, penalty 0,<br/>voucher, assignment, history và outbox]
    M --> N
    N --> O{Commit thành công?}
    O -- Không --> R[Rollback, job có thể retry]
    O -- Có --> P[Dừng task/timer còn mở,<br/>gửi thông báo và invalidate cache]
    P --> Q([Kết thúc])
    X --> S([Không đổi Order])
    F --> S
    I --> S
    K --> S
    R --> S
```

Timer/webhook/form/staff-start cạnh tranh phải dùng khóa/version để chỉ một kết quả nghiệp vụ có hiệu lực.

## Điều phối nhánh hủy dùng chung

```mermaid
flowchart LR
    A[UC09.1<br/>Khách tự hủy] --> D[CancelOrderService dùng chung]
    B[UC09.2<br/>Manager reject] --> D
    C[UC09.3<br/>Timeout hoặc payment fail] --> D
    D --> E[CANCELLED và history]
    E --> F[Voucher/reputation/assignment<br/>theo cancellation source]
    F --> G[ORDER_CANCELLED outbox]
    G --> H[Workflow cleanup]
    G --> I[Email/notification hủy]
    G --> J[Cache invalidation]
```
