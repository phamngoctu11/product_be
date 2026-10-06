# WF06 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity Diagram](diagram.md) · [Use Case tổng quát](overview.svg).

Tên ChangeRequestService, spec version service, event và task dưới đây là vai trò kỹ thuật đề xuất. WF06 có thể chạy bằng service + notification. Nếu nối Camunda, task được tạo trong non-interrupting event subprocess và không chặn main flow WF04/WF05.

## UC06.1 — User tạo yêu cầu thay đổi

```mermaid
sequenceDiagram
    actor U as USER sở hữu Order
    participant UI as Màn hình ChangeRequest
    participant API as ChangeRequest API
    participant S as ChangeRequestService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda tùy chọn
    participant Notify as Notification

    U->>UI: Mở chức năng yêu cầu thay đổi
    UI->>API: Đọc Order, item và spec version
    API->>S: Lấy dữ liệu với userId từ JWT
    S->>DB: Kiểm tra owner, Order status,<br/>assigned staff, item và current spec
    alt Sai owner, Guest hoặc trạng thái không cho phép
        DB-->>S: Không hợp lệ
        S-->>API: 403 hoặc 409
        API-->>UI: Không mở form
    else Hợp lệ
        DB-->>S: Spec hiện hành và phạm vi đề nghị
        S-->>API: Dữ liệu theo quyền
        API-->>UI: Hiển thị form không có field tài chính/deadline
        U->>UI: Chọn chi tiết, nhập đề nghị và reason
        UI->>API: orderId, orderItemId, baseVersion,<br/>proposed details và idempotency key
        API->>S: create với actor đã xác thực
        S->>DB: Khóa hoặc kiểm tra Order, item,<br/>base version và tiến độ chi tiết
        alt Base version cũ hoặc chi tiết đã triển khai
            DB-->>S: Conflict
            S-->>API: Không tạo request
            API-->>UI: Tải spec hoặc tiến độ mới
        else Đề nghị chứa giá, voucher, payment hoặc deadline
            S-->>API: Validation lỗi
            API-->>UI: Không âm thầm bỏ field cấm
        else Cùng key và cùng nội dung đã xử lý
            DB-->>S: ChangeRequest hiện có
            S-->>API: Kết quả idempotent
            API-->>UI: Request hiện có
        else Hợp lệ
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Tạo ChangeRequest PENDING_REVIEW,<br/>base/proposed snapshot, owner, time<br/>và công việc sau commit
            alt Ghi thất bại
                DB-->>S: Rollback
                S-->>API: Chưa tạo request
                API-->>UI: Thử lại an toàn
            else Commit thành công
                DB-->>S: changeRequestId
                S-->>API: PENDING_REVIEW và createdAt
                API-->>UI: Đang chờ staff phụ trách
                Jobs->>Notify: Notification assigned staff lấy từ DB
                opt Dùng Camunda task phụ
                    Jobs->>Engine: Correlate CHANGE_REQUEST_SUBMITTED<br/>theo orderId, changeRequestId và eventId
                    Engine->>Engine: Mở non-interrupting staff task
                end
                opt Bàn giao hoặc notification lỗi
                    Jobs->>DB: Ghi retry, không tạo ChangeRequest mới
                end
            end
        end
    end
```

USER submit chỉ tạo đề nghị. Spec, Order status, giá, payment, deadline và checkpoint chưa thay đổi ở UC06.1.

## UC06.2 — Staff quyết định yêu cầu thay đổi

```mermaid
sequenceDiagram
    actor Staff as STAFF phụ trách
    participant UI as Task ChangeRequest
    participant API as ChangeRequest Decision API
    participant S as ChangeRequestService
    participant Spec as SpecVersionService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda tùy chọn
    participant Notify as Notification

    Staff->>UI: Mở ChangeRequest đang chờ
    UI->>API: Đọc changeRequestId
    API->>S: Lấy dữ liệu với staffId từ JWT
    S->>DB: Kiểm tra active assignment, request,<br/>base/current spec và progress
    alt Không phải assigned staff hoặc assignment đã release
        DB-->>S: Không có quyền
        S-->>API: 403 hoặc 409
        API-->>UI: Không xử lý request
    else Request đã có quyết định
        DB-->>S: APPLIED hoặc REJECTED
        S-->>API: Kết quả hiện hành
        API-->>UI: Không quyết định lần hai
    else Có quyền
        DB-->>S: Request, history và progress hiện hành
        S-->>API: Dữ liệu đánh giá
        API-->>UI: Hiển thị before/proposed và phạm vi cấm
        Staff->>UI: ACCEPT hoặc REJECT với reason
        UI->>API: decision, reason và request version
        API->>S: decide với actor đã xác thực
        S->>DB: Khóa ChangeRequest, Order,<br/>current spec, progress và assignment
        alt Version cũ hoặc request không còn PENDING_REVIEW
            DB-->>S: Conflict
            S-->>API: Kết quả hiện hành
            API-->>UI: Tải lại
        else REJECT thiếu reason
            S-->>API: Validation lỗi
            API-->>UI: Yêu cầu nhập reason
        else REJECT hợp lệ
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Ghi REJECTED, reason, actor/time và audit
            DB-->>S: Commit, spec và Order không đổi
            S-->>API: REJECTED
            API-->>UI: Đã từ chối
            Jobs->>Notify: Notification kết quả cho USER
            opt Dùng Camunda
                Jobs->>Engine: Hoàn tất task phụ theo nhánh REJECTED
            end
        else ACCEPT
            S->>S: Kiểm tra lại chi tiết chưa triển khai,<br/>base spec hiện hành và field được phép
            alt Chi tiết đã làm, base stale hoặc ảnh hưởng giá/deadline
                S-->>API: Không được áp dụng
                API-->>UI: Chọn REJECT với reason hoặc tải dữ liệu mới
            else Có thể áp dụng
                Note over S,DB: Một transaction nghiệp vụ
                S->>Spec: Tạo applied spec version từ current version<br/>chỉ cho phần chưa triển khai
                Spec->>DB: Lưu version mới và liên kết ChangeRequest
                S->>DB: Ghi APPLIED, appliedVersion,<br/>decision actor/time, audit và công việc sau commit
                DB-->>S: Commit
                S-->>API: APPLIED và appliedVersion
                API-->>UI: Đã áp dụng
                Jobs->>Notify: Notification kết quả cho USER
                opt Dùng Camunda
                    Jobs->>Engine: Hoàn tất task phụ theo nhánh APPLIED
                end
                Note over S,DB: Order status, giá, voucher, payment,<br/>productionStartedAt, deadline và assignment không đổi
            end
        end
        opt Workflow hoặc notification lỗi sau commit
            Jobs->>DB: Ghi retry theo changeRequestId và decision
            Note over Jobs,Engine: Không áp dụng hoặc quyết định lần hai
        end
    end
```

Main process không chờ task phụ WF06 để tiếp tục toàn bộ production. Backend vẫn phải kiểm tra progress tại lúc ACCEPT vì chi tiết có thể đã được triển khai trong thời gian staff chưa quyết định.
