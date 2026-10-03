# WF01 — Sơ đồ tổng quan và hoạt động

### 9.1. Use Case Diagram tổng quan WF01

![Biểu đồ Use Case tổng quát WF01](overview.svg)

Tệp hình vector để chèn báo cáo: [WF01-use-case-overview.svg](overview.svg). Mã nguồn chỉnh sửa bằng PlantUML: [WF01-use-case-overview.puml](overview.puml).

Sơ đồ UML bằng PlantUML. Ba use case là các mục tiêu độc lập của USER; không dùng include/extend để biểu diễn thứ tự thao tác. Đăng nhập là tiền điều kiện. Dịch vụ email là actor phụ bên ngoài; backend, database và Camunda nằm trong hệ thống.

```plantuml
@startuml
left to right direction
actor "Khách có tài khoản\nUSER" as User
actor "Dịch vụ email" as Email
rectangle "WF01 — Custom Request" {
  usecase "UC01.1\nXem danh sách và chi tiết\nbản nháp custom" as View
  usecase "UC01.2\nTạo và cập nhật\nbản nháp custom" as Draft
  usecase "UC01.3\nGửi yêu cầu tạo\nđơn custom" as Submit
}
User -- View
User -- Draft
User -- Submit
Submit -- Email
note bottom of Submit
  Kết quả: Order CUSTOM / PENDING_APPROVAL.
  Email và notification được xử lý bất đồng bộ.
  Manager duyệt thuộc WF02.
end note
@enduml
```

Các sơ đồ Mermaid dưới đây là sơ đồ hoạt động tổng quát, thể hiện xử lý và kết quả của từng UC; không phải ký pháp UML Use Case Diagram.

### 9.2. UC01.1 — Xem danh sách và chi tiết bản nháp

```mermaid
flowchart TD
    A([User mở mục yêu cầu custom]) --> B{Đăng nhập với quyền USER?}
    B -->|Không| X[Từ chối truy cập, hướng dẫn đăng nhập]
    B -->|Có| C[Hệ thống lấy danh sách theo userId từ JWT]
    C --> D{Có bản nháp?}
    D -->|Không| E[Hiển thị danh sách rỗng và nút tạo mới]
    D -->|Có| F[Hiển thị các bản nháp của user]
    F --> G[User chọn bản nháp]
    G --> H{Tồn tại và thuộc user?}
    H -->|Không| I[Trả lỗi, không tiết lộ nội dung]
    H -->|Có| J[Hiển thị spec, số lượng, ảnh và ghi chú]
    J --> K([Kết thúc: xem được chi tiết])
    E --> L([Kết thúc: chưa có bản nháp])
    X --> M([Kết thúc: không được truy cập])
    I --> M
```

Điểm chuyển tiếp: từ danh sách/chi tiết, user có thể chọn UC01.2 hoặc UC01.3. Việc xem không làm thay đổi draft, không tạo Order và không gửi email.

### 9.3. UC01.2 — Tạo/cập nhật bản nháp

```mermaid
flowchart TD
    A([User chọn tạo hoặc sửa bản nháp]) --> B{Đăng nhập với quyền USER?}
    B -->|Không| X[Từ chối, hướng dẫn đăng nhập]
    B -->|Có| C{Tạo mới hay sửa?}
    C -->|Tạo mới| D[Mở form trống]
    C -->|Sửa| E{Bản nháp tồn tại và thuộc user?}
    E -->|Không| Y[Trả lỗi, không thay đổi dữ liệu]
    E -->|Có| F[Nạp nội dung bản nháp]
    D --> G[User nhập spec, số lượng, ghi chú]
    F --> G
    G --> H{Có chọn ảnh tham khảo?}
    H -->|Không| I[User bấm Lưu bản nháp]
    H -->|Có| J[Tải ảnh và kiểm tra attachment]
    J --> K{Ảnh hợp lệ và tải thành công?}
    K -->|Không| L[Hiển thị lỗi ảnh để sửa hoặc bỏ ảnh lỗi]
    L --> G
    K -->|Có| I
    I --> M{Dữ liệu lưu hợp lệ?}
    M -->|Không| N[Hiển thị lỗi trường, chưa lưu thay đổi]
    N --> G
    M -->|Có| O[Lưu draft và attachment hợp lệ]
    O --> P{Lưu thành công?}
    P -->|Không| Q[Trả lỗi, không ghi đè dữ liệu bằng bản lỗi]
    P -->|Có| R[Trả draftId, nội dung và thời điểm cập nhật]
    R --> S([Kết thúc: bản nháp được lưu])
    X --> T([Kết thúc: thất bại])
    Y --> T
    Q --> T
```

Kết quả chỉ là bản nháp. Không có Order, email tạo đơn, thanh toán hoặc task manager. Validation cho phép thiếu trường nào khi lưu nháp phải theo schema đã được chốt, không mặc định giống validation gửi đơn.

### 9.4. UC01.3 — Gửi bản nháp tạo Order custom

```mermaid
flowchart TD
    A([User chọn bản nháp và xác nhận gửi]) --> B{Đăng nhập, có quyền sở hữu?}
    B -->|Không| X[Từ chối, không tạo Order]
    B -->|Có| C{Yêu cầu đã được xử lý?}
    C -->|Cùng yêu cầu đã thành công| D[Trả orderId đã tạo]
    C -->|Key trùng nhưng nội dung khác| E[Trả lỗi xung đột]
    C -->|Yêu cầu mới| F{Spec, số lượng, ảnh và contact hợp lệ?}
    F -->|Không| G[Trả lỗi trường, cho user chỉnh sửa]
    F -->|Có| H[Transaction tạo Order CUSTOM và một loại OrderItem]
    H --> I[Lưu snapshot, liên kết draft, trạng thái PENDING_APPROVAL]
    I --> J{Commit thành công?}
    J -->|Không| K[Rollback, trả lỗi, không để đơn tạo dở]
    J -->|Có| L[Đăng ký manager review và sự kiện tạo đơn]
    L --> M[Trả orderId, hiển thị MyOrder chờ duyệt]
    L -. Xử lý sau commit .-> N[Khởi workflow manager review]
    L -. Xử lý sau commit .-> O[Gửi email và notification cho user]
    N --> P{Khởi workflow thành công?}
    O --> Q{Gửi thành công?}
    P -->|Không| R[Lưu lỗi và retry, không tạo lại Order]
    Q -->|Không| R
    P -->|Có| S[Manager thấy công việc chờ duyệt ở WF02]
    Q -->|Có| T[User được thông báo tạo đơn thành công]
    M --> U([Kết thúc yêu cầu: Order đã tạo])
    D --> U
    X --> V([Kết thúc: chưa tạo đơn mới])
    E --> V
    G --> V
    K --> V
```

Các cạnh nét đứt là phần xử lý bất đồng bộ sau commit, không yêu cầu API chờ email hoặc workflow chạy xong. Hai yêu cầu gửi đồng thời vẫn phải được bảo vệ bằng transaction/idempotency tại server, không chỉ bằng bước kiểm tra trước khi lưu.

Kết quả nghiệp vụ: một Order CUSTOM ở PENDING_APPROVAL; chưa manager duyệt, chưa chốt giá, chưa thanh toán, chưa tạo chat/phân công và chưa chạy timer custom 24 giờ. Timer bắt đầu sau quyết định manager ở workflow tiếp theo.

### 9.5. Activity liên kết ba use case

```mermaid
flowchart TD
    A[User mở yêu cầu custom] --> B[Tạo hoặc chọn bản nháp]
    B --> C[Nhập spec, số lượng và ảnh]
    C --> D[Lưu bản nháp]
    D --> E{User chọn gửi tạo đơn?}
    E -->|Chưa| F[Bản nháp được lưu]
    E -->|Có| G[Kiểm tra quyền, dữ liệu và yêu cầu trùng]
    G -->|Không hợp lệ| C
    G -->|Đã xử lý| H[Trả Order đã tạo]
    G -->|Hợp lệ, chưa xử lý| I[Transaction tạo Order CUSTOM và snapshot]
    I --> J[PENDING_APPROVAL]
    J --> K[Đăng ký manager review và sự kiện tạo đơn]
    K --> L[Trả orderId và hiển thị MyOrder]
    K --> M[Gửi email và notification bất đồng bộ]
```

Use Case Diagram: USER liên kết với UC01.1, UC01.2 và UC01.3. Manager liên kết use case duyệt ở WF02, không phải người thao tác tạo yêu cầu. Không dùng quan hệ include giữa lưu nháp và gửi đơn chỉ để biểu diễn thứ tự; bản nháp có thể được lưu mà không gửi.

Biểu đồ tuần tự chi tiết nằm trong [sequence.md](sequence.md).
