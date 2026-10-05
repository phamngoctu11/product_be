# WF02 — Sơ đồ hoạt động

Nguồn nghiệp vụ: [đặc tả](usecase.md). Sơ đồ dưới đây mô tả hoạt động, không thay thế Use Case Diagram.

## Use Case tổng quát

![WF02 — Use Case tổng quát](overview.svg)

[Mở SVG](overview.svg) · [Nguồn PlantUML](overview.puml)

## UC02.1 — Đặt hàng catalog từ giỏ

**Giải thích key chống tạo trùng (idempotency key):** đây là mã giao diện tạo cho một lần checkout, không phải mã Order hay mã sản phẩm. Khi gửi lại chính yêu cầu đó vì bấm lặp hoặc mất phản hồi mạng, giao diện dùng lại key. Backend đối chiếu trong phạm vi cùng user/guest và thao tác checkout.

- **Cùng key, cùng nội dung:** gửi lại cùng các mục đặt hàng, quantity, contact, phương thức thanh toán và voucher. Nếu lần trước đã tạo đơn thành công, trả lại đơn đó, không tạo đơn thứ hai.
- **Cùng key, khác nội dung:** dùng lại key nhưng đổi dữ liệu, ví dụ quantity từ 2 thành 3. Backend báo conflict vì một mã yêu cầu không được đại diện cho hai lần đặt khác nhau. Lần checkout mới dùng key mới, không tự đổi key khi chỉ retry do mất phản hồi.
- Nếu yêu cầu cùng key còn đang xử lý, không chạy thêm một lần tạo đơn song song, xử lý chờ hoặc báo đang xử lý theo hợp đồng API.

Ví dụ: checkout với key `checkout-abc`, quantity = 2 đã tạo Order #123. Gửi lại đúng yêu cầu trả Order #123. Gửi key `checkout-abc` với quantity = 3 bị từ chối do khác nội dung. Đây là thiết kế chống tạo trùng đề xuất, không phải xác nhận code hiện tại đã có cơ chế này.

```mermaid
flowchart TD
    A([Khách xác nhận checkout]) --> B{Danh tính và quyền giỏ hợp lệ?}
    B -- Không --> X[Trả lỗi; không tạo đơn]
    B -- Có --> C{Yêu cầu đã xử lý?}
    C -- Cùng key và nội dung --> R[Trả lại đơn đã tạo]
    C -- Cùng key khác nội dung --> X
    C -- Chưa --> D[Kiểm tra mục được chọn, quantity, contact và catalog nhận làm]
    D --> E{Dữ liệu hợp lệ?}
    E -- Không --> X
    E -- Có --> F{Loại khách}
    F -- Guest --> G[Chỉ COD; kiểm tra public voucher và giới hạn guest]
    F -- USER --> H[Kiểm tra reputation cho COD và quyền dùng voucher]
    G --> I{Phương thức và voucher hợp lệ?}
    H --> I
    I -- Không --> X
    I -- Có --> J[Lấy giá server; tính finalPrice và thời lượng riêng từng loại]
    J --> K[Transaction: Order PENDING_APPROVAL, items và snapshot;
    usage voucher, cập nhật giỏ, kết quả idempotency;
    lưu công việc bàn giao sau commit]
    K --> L{Commit thành công?}
    L -- Không --> M[Rollback toàn bộ; không mất giỏ hoặc quota]
    L -- Có --> N[Trả orderId và giá; guest có thông tin truy cập bảo mật]
    N --> O[Sau commit: đăng ký manager review;
    email tạo đơn; USER thêm notification]
    O --> P{Xử lý bất đồng bộ thành công?}
    P -- Không --> Q[Lưu lỗi để retry; không tạo Order lần hai]
    P -- Có --> Z([Đơn chờ manager])
    Q --> Z
    M --> Y([Kết thúc yêu cầu])
    X --> Y
    R --> Y
```

Guest chỉ nhận email; URL chứa token giới hạn theo Order, không chỉ orderId. Không tạo yêu cầu thanh toán hoặc trừ tồn kho trong UC này. Kết quả idempotency phải được kiểm tra trước khi xem giỏ đã checkout là giỏ rỗng.

## UC02.2 — Xem danh sách và chi tiết đơn chờ duyệt

```mermaid
flowchart TD
    A([Mở danh sách chờ duyệt]) --> B{Có quyền MANAGER?}
    B -- Không --> X[Từ chối truy cập]
    B -- Có --> C[Truy vấn PENDING_APPROVAL có phân trang]
    C --> D{Có kết quả?}
    D -- Không --> E[Hiển thị danh sách rỗng]
    D -- Có --> F[Manager chọn Order]
    F --> G{Order tồn tại và được phép đọc?}
    G -- Không --> H[Trả lỗi; không trả dữ liệu đơn]
    G -- Có --> I[Đọc snapshot và trạng thái mới nhất]
    I --> J{Còn PENDING_APPROVAL?}
    J -- Không --> K[Hiển thị trạng thái hiện tại; không dùng quyết định cũ]
    J -- Có --> L[Hiển thị loại, spec, ảnh, quantity, contact;
    giá catalog snapshot hoặc custom chưa xác định]
    L --> M[Manager đánh giá khả năng làm toàn đơn]
    M --> N[Tùy chọn xem staff AVAILABLE;
    chuyển UC02.3 khi quyết định]
    X --> Z([Kết thúc; không đổi trạng thái])
    E --> Z
    H --> Z
    K --> Z
    N --> Z
```

Danh sách staff chỉ phản ánh thời điểm đọc; thao tác gán phải kiểm tra lại khi ghi.

## UC02.3 — Chấp nhận hoặc từ chối tiếp nhận đơn

```mermaid
flowchart TD
    A([Manager gửi quyết định]) --> B{Đúng quyền và dữ liệu?}
    B -- Không --> X[Trả lỗi; không đổi đơn]
    B -- Có --> C[Kiểm tra khóa hoặc version; Order và task review]
    C --> D{Còn PENDING_APPROVAL và task hợp lệ?}
    D -- Không --> Y[Trả kết quả đã xử lý hoặc conflict;
    thiếu task thì xử lý reconciliation]
    D -- Có --> E{Quyết định}
    E -- Từ chối --> F{Có lý do?}
    F -- Không --> X
    F -- Có --> G[Transaction: CANCELLED, lý do, audit;
    hoàn voucher đúng quy tắc; không trừ reputation;
    lưu công việc kết thúc workflow và email]
    G --> H{Commit thành công?}
    H -- Không --> RB[Rollback; không phát kết quả thành công]
    H -- Có --> I[Sau commit: kết thúc nhánh review;
    email hủy; USER thêm notification]
    I --> J([Trả CANCELLED và lý do])
    E -- Chấp nhận --> K{Manager chọn staff?}
    K -- Có --> L{Staff AVAILABLE và chưa có active assignment?}
    L -- Không --> V[Conflict; không lưu một phần quyết định;
    manager chọn lại hoặc gửi không kèm staff]
    L -- Có --> M[Transaction: ghi assignment qua nghiệp vụ chung WF03]
    K -- Không --> N[Transaction: chuẩn bị PENDING_ASSIGNMENT]
    M --> O[Lưu người duyệt, managerApprovedAt và history;
    không sửa giá catalog hoặc chốt giá custom]
    N --> O
    O --> P{CUSTOM?}
    P -- Có --> Q[Lưu hạn gửi form bằng managerApprovedAt + 24h;
    lưu công việc đăng ký timer, không gia hạn khi retry]
    P -- Không --> S[Lưu công việc bàn giao WF03]
    Q --> S
    S --> T{Commit thành công?}
    T -- Không --> RB
    T -- Có --> U[Hoàn tất bước review nhất quán và bàn giao WF03;
    trả quyết định, trạng thái và assignment nếu có]
    U --> W([Kết thúc tiếp nhận; chưa bắt đầu sản xuất])
```

Các nhánh ghi assignment và quyết định nằm trong **một transaction nghiệp vụ**, không commit giữa các ô. Nếu engine không cùng transaction DB, công việc bàn giao phải bền vững và retry được. Email/engine lỗi sau commit không làm phát sinh quyết định, quota hay assignment thứ hai.

WF03 xử lý chat và xác nhận thông tin: manager duyệt không tự chốt CUSTOM. Guest CATALOG đã đồng ý mẫu, khi được duyệt và có staff sẽ đi tiếp nhánh xác nhận catalog đến ORDER_ACCEPTED. Không phát email trạng thái trung gian chỉ vì manager vừa duyệt.

## Liên kết giữa các workflow

```mermaid
flowchart LR
    Cart[Giỏ catalog] --> UC1[UC02.1: tạo Order PENDING_APPROVAL]
    WF1[WF01: Order CUSTOM đã tạo] --> UC2[UC02.2: manager xem đơn]
    UC1 --> UC2
    UC2 --> UC3[UC02.3: quyết định tiếp nhận]
    UC3 -- Duyệt --> WF3[WF03: phân công và xác nhận thông tin]
    UC3 -- Từ chối --> Cancel[CANCELLED; dùng tác động hủy chung WF09]
    WF3 --> WF4[WF04: thanh toán nếu ONLINE và staff bấm bắt đầu]
```

CUSTOM từ WF01 không đi lại bước tạo đơn UC02.1. Timer gửi form 24h dừng khi user gửi form hợp lệ ở WF03; timer payment 1h thuộc WF04.
