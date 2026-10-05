# WF03 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity Diagram](diagram.md) · [Use Case tổng quát](overview.svg).

Tên API, AssignmentService, AgreementService, ChatService và worker dưới đây là vai trò kỹ thuật đề xuất, cần ánh xạ với code khi triển khai. Database là nguồn trạng thái nghiệp vụ, Camunda giữ task, timer và vị trí điều phối. Các message chỉ được correlate sau khi backend đã xác thực và lưu sự kiện nghiệp vụ.

## UC03.1 — Manager phân công staff

```mermaid
sequenceDiagram
    actor M as MANAGER
    participant UI as Màn hình quản lý đơn
    participant API as Assignment API
    participant S as AssignmentService
    participant DB as Database
    participant Jobs as Bộ xử lý bàn giao
    participant Engine as Camunda
    participant Notify as Notification và Email

    M->>UI: Chọn staff cho Order đã duyệt
    UI->>API: orderId, staffId và version
    API->>API: Xác thực MANAGER từ JWT
    alt Không đủ quyền
        API-->>UI: 403, không thay đổi Order
    else Đủ quyền
        API->>S: assign(orderId, staffId, managerId, version)
        S->>DB: Khóa hoặc kiểm tra Order, staff và active assignments
        alt Order không hợp lệ hoặc đã có staff
            DB-->>S: Trạng thái hiện tại
            S-->>API: Conflict hoặc kết quả idempotent
            API-->>UI: Tải trạng thái mới
        else Staff không AVAILABLE hoặc đã có active assignment
            DB-->>S: Không đủ điều kiện
            S-->>API: Conflict
            API-->>UI: Chọn staff khác
        else Có thể phân công
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Tạo Assignment MANAGER_ASSIGN,<br/>gắn assignedStaff, chiếm suất,<br/>ghi history và công việc bàn giao
            alt Ghi thất bại hoặc race
                DB-->>S: Rollback hoặc unique conflict
                S-->>API: Conflict, không lưu một phần
                API-->>UI: Không báo gán thành công
            else Commit thành công
                DB-->>S: Assignment đã lưu
                S-->>API: assignmentId và bước tiếp theo
                API-->>UI: Hiển thị staff phụ trách
                Jobs->>Engine: Correlate assignment theo orderId và eventId
                Jobs->>Notify: Notification cho staff
                alt USER Order
                    Engine->>Engine: Chuyển DISCUSSING và gọi service task chat
                else Guest CATALOG
                    Engine->>S: Áp dụng nhánh catalog snapshot
                    S->>DB: Chuyển ORDER_ACCEPTED một lần
                    Jobs->>Notify: Email ORDER_ACCEPTED cho Guest
                end
                opt Engine hoặc notification lỗi
                    Jobs->>DB: Ghi retry theo khóa nghiệp vụ
                end
            end
        end
    end
```

Khi manager chọn staff trong cùng request duyệt WF02, `ReviewService` và `AssignmentService` phải phối hợp theo một transaction hoặc hợp đồng bàn giao bền vững đã định nghĩa, không commit quyết định “đã gán” nếu assignment conflict.

## UC03.2 — Staff tự nhận đơn

```mermaid
sequenceDiagram
    actor Staff as STAFF
    participant UI as Danh sách đơn có thể nhận
    participant API as Claim API
    participant S as AssignmentService
    participant DB as Database
    participant Jobs as Bộ xử lý bàn giao
    participant Engine as Camunda
    participant Notify as Notification và Email

    Staff->>UI: Mở danh sách đơn chờ nhận
    UI->>API: Đọc danh sách PENDING_ASSIGNMENT
    API->>API: Xác thực STAFF
    API->>DB: Truy vấn dữ liệu được phép xem
    DB-->>UI: Danh sách tham khảo
    Staff->>UI: Chọn nhận một Order
    UI->>API: claim(orderId, version)
    API->>S: Claim với staffId lấy từ JWT
    S->>DB: Khóa hoặc kiểm tra Order và active assignment của staff
    alt CUSTOM đã timeout hoặc Order không còn chờ
        DB-->>S: Không còn hợp lệ
        S-->>API: Conflict và trạng thái hiện tại
        API-->>UI: Không nhận đơn
    else Staff đã bận
        DB-->>S: Có active assignment
        S-->>API: Conflict
        API-->>UI: Staff phải hoàn tất hoặc hủy assignment hiện tại
    else Hợp lệ
        Note over S,DB: Một transaction nghiệp vụ
        S->>DB: Tạo Assignment SELF_CLAIM,<br/>gắn staff, chiếm suất và lưu công việc bàn giao
        alt Một request cạnh tranh thắng trước
            DB-->>S: Unique conflict và rollback
            S-->>API: Conflict
            API-->>UI: Order hoặc staff đã được nhận
        else Commit thành công
            DB-->>S: Assignment đã lưu
            S-->>API: Kết quả claim
            API-->>UI: Staff trở thành người phụ trách
            Jobs->>Engine: Correlate assignment theo orderId và eventId
            Jobs->>Notify: Notification phân công
            alt USER Order
                Engine->>Engine: Tiếp tục nhánh tạo chat
            else Guest CATALOG
                Engine->>S: Xác nhận theo snapshot catalog
                S->>DB: ORDER_ACCEPTED và history
                Jobs->>Notify: Email ORDER_ACCEPTED
            end
        end
    end
```

Request claim lặp của chính staff đã nhận cùng Order có thể trả assignment hiện có. Nó không tạo record, notification hoặc correlation thứ hai.

## UC03.3 — Truy cập và trao đổi trong phòng chat

```mermaid
sequenceDiagram
    actor U as USER
    actor Staff as STAFF
    participant Engine as Camunda
    participant Worker as Chat service task
    participant Chat as ChatService
    participant DB as Database
    participant API as Chat REST API
    participant WS as WebSocket gateway

    Engine->>Worker: Tạo hoặc lấy chat cho Order USER đã có staff
    Worker->>Chat: ensureThread(orderId)
    Chat->>DB: Tìm ChatThread unique theo orderId
    alt Đã tồn tại
        DB-->>Chat: Thread và membership hiện có
    else Chưa tồn tại
        Chat->>DB: Tạo thread chỉ với owner membership<br/>và assigned staff membership
        alt Request retry hoặc race tạo trùng
            DB-->>Chat: Unique conflict
            Chat->>DB: Đọc thread đã được tạo
        end
    end
    Chat-->>Worker: chatThreadId
    Worker-->>Engine: Hoàn tất service task

    U->>API: Mở lịch sử chat theo orderId
    API->>Chat: Kiểm tra owner membership
    Chat->>DB: Đọc thread và messages theo cursor
    DB-->>API: Dữ liệu được phép
    API-->>U: Lịch sử và trạng thái Order

    Staff->>WS: Subscribe phòng theo JWT
    WS->>Chat: Kiểm tra assigned staff hiện hành
    alt Không có quyền
        Chat-->>WS: Từ chối subscribe
        WS-->>Staff: Lỗi quyền
    else Có quyền
        Chat-->>WS: Cho phép subscribe
        Staff->>WS: Gửi message
        WS->>Chat: Validate và lưu message
        Chat->>DB: INSERT ChatMessage
        DB-->>Chat: Message đã lưu
        Chat-->>WS: Phát realtime đúng phòng
        WS-->>U: Message mới
    end
    Note over U,DB: Gửi hoặc đọc tin không tự thay đổi Order status, giá hay agreement
```

REST đọc lịch sử, REST gửi nếu có và WebSocket subscribe đều phải kiểm tra quyền riêng. Chỉ USER sở hữu Order và STAFF đang phụ trách có membership. MANAGER, Guest và staff không phụ trách không được đọc, gửi hoặc subscribe.

## UC03.4 — User gửi form thông tin thỏa thuận

```mermaid
sequenceDiagram
    actor U as USER
    participant UI as Form trong phòng chat
    participant API as Agreement API
    participant S as AgreementService
    participant DB as Database
    participant Jobs as Bộ xử lý bàn giao
    participant Engine as Camunda
    participant Cancel as CancelService
    participant Notify as Notification và Email

    Note over Engine: Với CUSTOM, subprocess chờ form có boundary timer<br/>timeDate = confirmationDueAt từ managerApprovedAt
    U->>UI: Nhập thông tin và bấm gửi
    UI->>API: fields, voucherId, version và idempotency key
    API->>S: Submit với userId lấy từ JWT
    S->>DB: Đọc Order, owner, assignment, version và thời hạn
    alt Sai owner, trạng thái hoặc đã timeout
        DB-->>S: Không hợp lệ
        S-->>API: 403 hoặc 409
        API-->>UI: Không lưu form
    else Dữ liệu không hợp lệ
        S-->>API: Lỗi validation
        API-->>UI: Hiển thị trường cần sửa
        Note over S,Engine: Draft hoặc lỗi không dừng timer
    else Hợp lệ
        S->>S: Tính preview từ dữ liệu server,<br/>CATALOG không nhận giá và duration sửa
        Note over S,DB: Một transaction nghiệp vụ
        S->>DB: Lưu OrderAgreement version, userSubmittedAt,<br/>WAITING_STAFF_CONFIRMATION và công việc correlation
        alt Ghi thất bại hoặc version conflict
            DB-->>S: Rollback
            S-->>API: Conflict hoặc lỗi
            API-->>UI: Tải version hiện hành
        else Commit thành công
            DB-->>S: agreementVersion đã lưu
            S-->>API: Version và preview totals
            API-->>UI: Đã gửi, đang chờ staff
            Jobs->>Engine: Correlate FORM_SUBMITTED theo orderId và eventId
            Engine->>Engine: Kết thúc subprocess chờ form,<br/>hủy boundary timer và tạo staff task
            Jobs->>Notify: Notification cho assigned staff
            opt Correlation lỗi sau commit
                Jobs->>DB: Ghi retry và reconciliation
                Note over DB,Engine: userSubmittedAt đã lưu ngăn timeout hủy nhầm
            end
        end
    end

    par Nhánh timer có thể chạy cạnh submit
        Engine->>Cancel: CUSTOM_CONFIRMATION_TIMEOUT(orderId)
        Cancel->>DB: Khóa Order và kiểm tra chưa có form hợp lệ
        alt Form đã commit trước
            DB-->>Cancel: Không được hủy
            Cancel-->>Engine: Hoàn tất timeout không tác động
        else Timeout thắng trước
            Cancel->>DB: CANCELLED, release đúng assignment,<br/>history và công việc thông báo
            Cancel-->>Engine: Kết thúc process theo nhánh hủy
            Jobs->>Notify: Email và notification USER,<br/>notification staff nếu đã gán
        end
    end
```

Sau lần submit hợp lệ đầu tiên, staff yêu cầu sửa không tạo lại timer. Form version mới vẫn đi qua kiểm tra version và idempotency nhưng không chịu một cửa sổ 24 giờ mới.

## UC03.5 — Staff xác nhận hoặc yêu cầu sửa

```mermaid
sequenceDiagram
    actor Staff as STAFF phụ trách
    participant UI as Task xác nhận thỏa thuận
    participant API as Agreement Decision API
    participant S as AgreementService
    participant DB as Database
    participant Jobs as Bộ xử lý bàn giao
    participant Engine as Camunda
    participant Notify as Notification và Email

    Staff->>UI: Mở agreement đang chờ
    UI->>API: Đọc orderId và agreementVersion
    API->>S: Lấy dữ liệu theo staffId từ JWT
    S->>DB: Kiểm tra active assignment và version hiện hành
    alt Không phải staff phụ trách
        S-->>API: 403 hoặc 409
        API-->>UI: Không hiển thị task được xử lý
    else Có quyền
        DB-->>S: Agreement và snapshot cần đối chiếu
        S-->>API: Dữ liệu hiện hành
        API-->>UI: Hiển thị thông tin
        Staff->>UI: CONFIRM hoặc REQUEST_CHANGES
        UI->>API: decision, agreementVersion và reason nếu sửa
        API->>S: Quyết định với actor đã xác thực
        S->>DB: Khóa Order, assignment, agreement và voucher liên quan
        alt Version cũ hoặc trạng thái đã đổi
            DB-->>S: Conflict
            S-->>API: Phiên bản hiện hành
            API-->>UI: Tải lại task
        else REQUEST_CHANGES hợp lệ
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Ghi decision và reason,<br/>chuyển DISCUSSING, lưu history và công việc bàn giao
            DB-->>S: Commit
            S-->>API: Yêu cầu sửa đã lưu
            API-->>UI: Kết quả
            Jobs->>Engine: Hoàn tất task theo nhánh sửa,<br/>quay lại chờ agreement version mới không có timer 24h mới
            Jobs->>Notify: Notification cho USER
        else CONFIRM
            S->>S: Kiểm tra lại dữ liệu và voucher tại thời điểm xác nhận
            alt Voucher hoặc dữ liệu không còn hợp lệ
                S-->>API: Lỗi, không âm thầm bỏ voucher
                API-->>UI: Yêu cầu USER chọn lại
            else Hợp lệ
                Note over S,DB: Một transaction nghiệp vụ
                S->>DB: Áp dụng spec, duration và giá chính thức,<br/>tiêu thụ voucher một lần, tính finalPrice,<br/>ORDER_ACCEPTED, history và công việc bàn giao WF04
                alt Ghi thất bại
                    DB-->>S: Rollback
                    S-->>API: Lỗi hoặc conflict
                    API-->>UI: Chưa xác nhận
                else Commit thành công
                    DB-->>S: Official agreement và totals
                    S-->>API: ORDER_ACCEPTED
                    API-->>UI: Đã xác nhận
                    Jobs->>Engine: Hoàn tất staff task và vào WF04
                    Jobs->>Notify: Email và notification ORDER_ACCEPTED cho USER
                    opt Engine hoặc notification lỗi
                        Jobs->>DB: Ghi retry theo orderId, eventType và milestone
                    end
                end
            end
        end
    end
```

UC03.5 không tạo payment. Khi Order đã `ORDER_ACCEPTED`, WF04 mới rẽ COD hoặc mở PaymentAttempt ONLINE. Lỗi gửi email không được rollback thỏa thuận, trừ voucher lần nữa hoặc yêu cầu staff xác nhận lại.
