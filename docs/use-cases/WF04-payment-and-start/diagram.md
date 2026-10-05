# WF04 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Đây là Activity Diagram, khác với Use Case Diagram actor/oval.

## Use Case tổng quát

![WF04 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC04.1 — Mở và thực hiện thanh toán online

```mermaid
flowchart TD
    A([USER bấm thanh toán]) --> B{Đúng owner và<br/>paymentMethod ONLINE?}
    B -- Không --> X[Từ chối]
    B -- Có --> C[Kiểm tra Order, assignment,<br/>finalPrice và payment status]
    C --> D{Order còn ORDER_ACCEPTED<br/>và giá chính thức hợp lệ?}
    D -- Không --> Y[Trả conflict hoặc trạng thái hiện tại]
    D -- Có --> E{Đã PAID?}
    E -- Có --> P[Trả trạng thái PAID,<br/>không mở giao dịch mới]
    E -- Không --> F{Có active attempt còn hạn<br/>hoặc idempotency result?}
    F -- Có --> G[Trả PaymentAttempt hiện có,<br/>không đổi dueAt]
    F -- Không --> H[Tạo yêu cầu provider bằng reference duy nhất<br/>và expectedAmount từ server]
    H --> I[Lưu PaymentAttempt PENDING,<br/>openedAt và dueAt = openedAt + 1 giờ]
    I --> J{Commit và mở provider thành công?}
    J -- Không --> R[Ghi trạng thái retry hoặc reconciliation,<br/>không báo thành công giả]
    J -- Có --> K[Correlate PAYMENT_OPENED,<br/>Camunda vào event-based gateway]
    K --> L[Trả URL hoặc QR,<br/>attemptId, expectedAmount và dueAt]
    L --> M([USER thực hiện thanh toán tại provider])
    X --> Z([Kết thúc])
    Y --> Z
    P --> Z
    G --> Z
    R --> Z
```

Timer PT1H chỉ bắt đầu khi PaymentAttempt được mở. Tài liệu chưa đặt timeout cho khoảng từ `ORDER_ACCEPTED` đến lần đầu USER bấm thanh toán.

## UC04.2 — Tiếp nhận kết quả thanh toán

```mermaid
flowchart TD
    A([Webhook hoặc timer kích hoạt]) --> B{Nguồn kích hoạt}
    B -- Webhook --> C[Xác minh signature, reference,<br/>amount, result và provider transaction]
    C --> D{Webhook hợp lệ?}
    D -- Không --> X[Từ chối, không đổi trạng thái]
    D -- Có --> E[Khóa PaymentAttempt và Order,<br/>kiểm tra trạng thái hiện tại]
    B -- Timer dueAt --> F[Khóa PaymentAttempt và Order]
    F --> G{Attempt vẫn PENDING?}
    G -- Không --> H[Không tác động,<br/>đọc kết quả đã thắng]
    G -- Có --> I[Ghi EXPIRED và gọi hủy hệ thống PAYMENT_TIMEOUT]
    E --> J{Kết quả webhook}
    J -- Success --> K{Attempt còn PENDING,<br/>Order chưa CANCELLED và trong hạn?}
    K -- Không --> L[Ghi duplicate hoặc reconciliation exception,<br/>không hồi sinh Order]
    K -- Có --> M[Transaction ghi PAID, paidAt<br/>và công việc sau commit]
    M --> N[Correlate PAYMENT_SUCCESS,<br/>thông báo assigned staff]
    N --> O([Order vẫn ORDER_ACCEPTED,<br/>chờ staff bắt đầu])
    J -- Failure --> Q{Attempt còn PENDING?}
    Q -- Không --> H
    Q -- Có --> S[Ghi FAILED và gọi nghiệp vụ hủy hệ thống]
    I --> T[Release đúng assignment,<br/>khôi phục voucher, email hủy]
    S --> T
    T --> U([Order CANCELLED,<br/>không trừ reputation])
    X --> Z([Kết thúc])
    H --> Z
    L --> Z
```

Nếu PAID đã commit nhưng correlation lỗi, timer phải đọc trạng thái PAID và không hủy. Job bàn giao sẽ retry correlation theo cùng eventId.

## UC04.3 — Staff bắt đầu sản xuất

```mermaid
flowchart TD
    A([STAFF bấm Bắt đầu làm]) --> B{Là assigned staff<br/>và assignment còn active?}
    B -- Không --> X[Từ chối]
    B -- Có --> C[Khóa Order và đọc dữ liệu chính thức]
    C --> D{Order còn ORDER_ACCEPTED?}
    D -- Không --> E{Đã ORDER_CREATING bởi request trước?}
    E -- Có --> F[Trả productionStartedAt<br/>và deadline đã lưu]
    E -- Không --> Y[Conflict, không ghi đè trạng thái]
    D -- Có --> G{Điều kiện payment}
    G -- ONLINE chưa PAID --> H[Từ chối, task vẫn chờ]
    G -- COD hoặc ONLINE PAID --> I{Agreement hoặc catalog snapshot<br/>và duration hợp lệ?}
    I -- Không --> H
    I -- Có --> J[Transaction ghi productionStartedAt,<br/>lateStartReason và ORDER_CREATING]
    J --> K[Loop từng OrderItem,<br/>tính computedCompletionAt từ ngày kế tiếp]
    K --> L[Bỏ Chủ nhật và ngày nghỉ cấu hình,<br/>giữ shipping 2 ngày tách riêng]
    L --> M[Ghi history và công việc<br/>hoàn tất user task Camunda]
    M --> N{Commit thành công?}
    N -- Không --> R[Rollback, task vẫn chờ]
    N -- Có --> O[Phát PRODUCTION_STARTED<br/>và bàn giao WF05]
    O --> P[Email ORDER_CREATING cho chủ đơn,<br/>USER thêm notification]
    P --> Q([Kết thúc WF04])
    X --> Z([Kết thúc không thay đổi])
    F --> Z
    Y --> Z
    H --> Z
    R --> Z
```

Việc staff đọc notification payment không đi qua bước chuyển trạng thái. Staff phải thực hiện hành động UC04.3.

## Liên kết toàn WF04

```mermaid
flowchart LR
    A[WF03: ORDER_ACCEPTED] --> B{paymentMethod}
    B -- COD --> C[UC04.3: chờ staff bấm bắt đầu]
    B -- ONLINE --> D[UC04.1: mở PaymentAttempt]
    D --> E{UC04.2: success, failure<br/>hay PT1H}
    E -- PAID --> C
    E -- FAILED hoặc EXPIRED --> F[CANCELLED qua WF09]
    C --> G[ORDER_CREATING]
    G --> H[WF05: checkpoint và KCS]
```

COD do shipper thu ngoài hệ thống. WF04 không có webhook COD và không gán paymentStatus PAID giả cho COD.
