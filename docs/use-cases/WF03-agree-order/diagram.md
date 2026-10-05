# WF03 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Đây là Activity Diagram, khác với Use Case Diagram actor/oval.

## Use Case tổng quát

![WF03 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC03.1 — Manager phân công staff

```mermaid
flowchart TD
    A([Manager chọn staff]) --> B{Có quyền MANAGER?}
    B -- Không --> X[Từ chối truy cập]
    B -- Có --> C[Đọc Order, version và staff mục tiêu]
    C --> D{Order đã được duyệt<br/>và chưa có assignment?}
    D -- Không --> Y[Trả trạng thái hiện tại hoặc conflict]
    D -- Có --> E{Staff đúng role, AVAILABLE<br/>và chưa có active assignment?}
    E -- Không --> Y
    E -- Có --> F[Transaction tạo Assignment MANAGER_ASSIGN,<br/>gắn staff vào Order và chiếm suất]
    F --> G{Commit thành công?}
    G -- Không --> R[Rollback toàn bộ]
    G -- Có --> H[Đăng ký bàn giao Camunda và notification staff]
    H --> I{Loại chủ đơn}
    I -- Guest CATALOG --> J[Nhánh tự động dùng snapshot catalog,<br/>chuyển ORDER_ACCEPTED và gửi email]
    I -- USER --> K[Chuyển DISCUSSING,<br/>tiếp tục UC03.3]
    X --> Z([Kết thúc không thay đổi])
    Y --> Z
    R --> Z
    J --> Q([Bàn giao WF04])
    K --> Q2([Tiếp tục WF03])
```

Nếu manager chọn staff ngay trong quyết định duyệt WF02, review và assignment phải dùng cùng hợp đồng nhất quán. Không được lưu “đã duyệt và đã gán” một phần khi staff conflict.

## UC03.2 — Staff tự nhận đơn

```mermaid
flowchart TD
    A([Staff chọn nhận đơn]) --> B{JWT có role STAFF?}
    B -- Không --> X[Từ chối]
    B -- Có --> C[Khóa hoặc kiểm tra version Order và staff]
    C --> D{Order còn PENDING_ASSIGNMENT?}
    D -- Không --> Y[Trả trạng thái hiện tại hoặc conflict]
    D -- Có --> E{Staff AVAILABLE<br/>và chưa giữ active assignment?}
    E -- Không --> Y
    E -- Có --> F{CUSTOM đã hết hạn<br/>mà chưa có form hợp lệ?}
    F -- Có --> T[Không claim,<br/>để nhánh timeout hủy quyết định kết quả]
    F -- Không --> G[Transaction tạo Assignment SELF_CLAIM,<br/>gắn staff và chiếm suất]
    G --> H{Commit thành công?}
    H -- Không --> R[Rollback hoặc conflict do race]
    H -- Có --> I[Phát bàn giao Camunda và notification]
    I --> J{Guest CATALOG?}
    J -- Có --> K[Tự chuyển ORDER_ACCEPTED,<br/>không chat và không form]
    J -- Không --> L[Chuyển DISCUSSING,<br/>tạo chat cho USER]
    X --> Z([Kết thúc])
    Y --> Z
    T --> Z
    R --> Z
    K --> W([Bàn giao WF04])
    L --> V([Tiếp tục UC03.3])
```

Hai staff claim cùng Order và một staff claim hai Order đồng thời đều phải được chặn bằng ràng buộc database hoặc khóa tương đương, không chỉ dựa vào dữ liệu hiển thị trên UI.

## UC03.3 — Truy cập và trao đổi trong phòng chat

```mermaid
flowchart TD
    A([Assignment của Order USER được commit]) --> B[Camunda gọi service task tìm hoặc tạo ChatThread theo orderId]
    B --> C{Đã có phòng?}
    C -- Có --> D[Dùng lại phòng hiện có]
    C -- Không --> E[Tạo một phòng chỉ với membership<br/>owner USER và assigned STAFF]
    D --> F[Hoàn tất service task]
    E --> F
    F --> G[USER hoặc STAFF mở chat]
    G --> H{Có membership và quyền hành động?}
    H -- Không --> X[Từ chối đọc, gửi hoặc subscribe]
    H -- Có --> I[Trả lịch sử theo cursor và trạng thái Order]
    I --> J{Tác vụ}
    J -- Gửi tin --> K[Validate, lưu message,<br/>phát realtime đúng phòng]
    J -- Xem form --> L[Hiển thị thông tin Order,<br/>voucher và trường được phép]
    K --> M[Tiếp tục trao đổi,<br/>không tự đổi Order status]
    L --> N[Chuyển UC03.4 khi USER bấm gửi]
    X --> Z([Kết thúc yêu cầu])
    M --> Z
    N --> Z
```

Guest CATALOG không vào Activity này. MANAGER không có quyền chat dù là người duyệt hoặc phân công staff. Lỗi WebSocket không phải lý do tạo ChatThread mới hoặc rollback assignment.

## UC03.4 — User gửi form thông tin thỏa thuận

```mermaid
flowchart TD
    A([USER bấm gửi form]) --> B{Đúng owner, có staff<br/>và trạng thái DISCUSSING?}
    B -- Không --> X[Từ chối]
    B -- Có --> C{CUSTOM lần gửi hợp lệ đầu tiên<br/>đã quá confirmationDueAt?}
    C -- Có --> T[Nhánh timeout hủy đơn,<br/>không nhận form muộn]
    C -- Không --> D[Kiểm tra version và dữ liệu theo loại Order]
    D --> E{CATALOG có sửa giá<br/>hoặc thời lượng snapshot?}
    E -- Có --> X
    E -- Không --> F{CUSTOM fields và voucher<br/>hợp lệ để xem trước?}
    F -- Không --> X
    F -- Có --> G[Tính preview subtotal, discount và finalPrice,<br/>chưa tiêu thụ voucher]
    G --> H[Transaction lưu agreement version,<br/>userSubmittedAt và WAITING_STAFF_CONFIRMATION]
    H --> I{Commit thành công?}
    I -- Không --> R[Rollback,<br/>timer vẫn chạy nếu chưa từng submit hợp lệ]
    I -- Có --> J{CUSTOM lần gửi hợp lệ đầu tiên?}
    J -- Có --> K[Đánh dấu bước chờ form hoàn tất,<br/>correlate message để hủy timer PT24H]
    J -- Không --> L[Không tạo hoặc reset timer]
    K --> M[Thông báo staff có version cần xác nhận]
    L --> M
    M --> N([Chờ UC03.5])
    X --> Z([Kết thúc không lưu])
    T --> Z
    R --> Z
```

Nếu DB commit form trước nhưng correlation lỗi, trạng thái DB ngăn timeout hủy và reconciliation hoàn tất process sau. Nếu timeout giành quyền cập nhật trước, form gửi muộn bị từ chối.

## UC03.5 — Staff xác nhận hoặc yêu cầu sửa

```mermaid
flowchart TD
    A([Staff mở agreement đang chờ]) --> B{Là assigned staff<br/>và assignment còn active?}
    B -- Không --> X[Từ chối]
    B -- Có --> C{Agreement là version mới nhất<br/>và Order đang chờ xác nhận?}
    C -- Không --> Y[Conflict, tải version hiện hành]
    C -- Có --> D{Quyết định}
    D -- Yêu cầu sửa --> E{Có lý do?}
    E -- Không --> X
    E -- Có --> F[Transaction lưu decision và reason,<br/>chuyển DISCUSSING]
    F --> G[Thông báo USER sửa form,<br/>không khởi động lại timer]
    D -- Xác nhận --> H[Kiểm tra lại spec, giá, duration và voucher]
    H --> I{Voucher và dữ liệu còn hợp lệ?}
    I -- Không --> J[Từ chối confirm,<br/>yêu cầu USER chọn lại]
    I -- Có --> K[Transaction áp dụng snapshot chính thức,<br/>tiêu thụ voucher và tính finalPrice]
    K --> L[Chuyển ORDER_ACCEPTED,<br/>ghi history và bàn giao WF04]
    L --> M[Sau commit gửi email và notification ORDER_ACCEPTED]
    X --> Z([Kết thúc không thay đổi])
    Y --> Z
    J --> Z
    G --> V([Chờ UC03.4 version mới])
    M --> W([Kết thúc WF03])
```

CATALOG của USER dùng giá và thời lượng snapshot, không nhận giá từ form. Guest CATALOG không có task staff xác nhận trong UC này.

## Liên kết toàn WF03

```mermaid
flowchart LR
    WF2[WF02 manager đã duyệt] --> A{Đã có staff?}
    A -- Chưa --> B[UC03.1 assign hoặc UC03.2 claim]
    A -- Có --> C{Loại chủ đơn}
    B --> C
    C -- Guest CATALOG --> D[ORDER_ACCEPTED]
    C -- USER --> E[UC03.3 tạo và dùng chat]
    E --> F[UC03.4 gửi form hợp lệ]
    F --> G[UC03.5 staff quyết định]
    G -- Yêu cầu sửa --> E
    G -- Xác nhận --> D
    H[Timer CUSTOM tuyệt đối từ managerApprovedAt] -- Hết hạn trước form hợp lệ --> I[CANCELLED qua WF09]
    F -. Form hợp lệ dừng timer .-> H
    D --> WF4[WF04 payment và staff bắt đầu]
```

Timer CUSTOM chỉ bao quanh giai đoạn trước lần gửi form hợp lệ đầu tiên. Timer payment không nằm trong WF03.
