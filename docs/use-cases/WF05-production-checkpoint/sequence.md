# WF05 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity Diagram](diagram.md) · [Use Case tổng quát](overview.svg).

Tên CheckpointService, ProductionCompletionService, storage và worker dưới đây là vai trò kỹ thuật đề xuất. Database giữ trạng thái checkpoint, attempt, KCS và assignment. Camunda điều phối user task, multi-instance và vòng lặp rework.

## UC05.1 — Staff nộp checkpoint hình ảnh đầu tiên

```mermaid
sequenceDiagram
    actor Staff as STAFF phụ trách
    participant UI as Màn hình checkpoint
    participant API as Checkpoint API
    participant S as CheckpointService
    participant Storage as Lưu trữ ảnh
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Email và Notification

    Staff->>UI: Mở task INITIAL_SHAPE của OrderItem
    UI->>API: Đọc task và dữ liệu item
    API->>S: Kiểm tra staffId từ JWT
    S->>DB: Đọc Order, item, active assignment và task reference
    alt Không phải staff phụ trách hoặc sai trạng thái
        DB-->>S: Không hợp lệ
        S-->>API: 403 hoặc 409
        API-->>UI: Không cho nộp
    else Có quyền
        DB-->>S: Dữ liệu item được phép
        S-->>API: Task INITIAL_SHAPE
        API-->>UI: Hiển thị form
        Staff->>UI: Chọn ảnh, ghi chú và bấm gửi
        UI->>Storage: Tải ảnh theo cơ chế có kiểm soát
        Storage-->>UI: Object key và metadata
        UI->>API: orderId, orderItemId, object keys,<br/>note và idempotency key
        API->>S: submitInitialShape với actor đã xác thực
        S->>Storage: Xác minh object thuộc request và đạt policy
        S->>DB: Khóa Order, item, assignment và stage
        alt Ảnh, quyền hoặc stage không hợp lệ
            S-->>API: Lỗi, task vẫn mở
            API-->>UI: Hiển thị lỗi
        else Request đã xử lý
            DB-->>S: Checkpoint hiện có
            S-->>API: Kết quả idempotent
            API-->>UI: Không tạo checkpoint thứ hai
        else Hợp lệ
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Tạo INITIAL_SHAPE, CheckpointImage,<br/>attempt, audit và công việc sau commit
            alt Ghi thất bại
                DB-->>S: Rollback
                S-->>API: Chưa hoàn tất task
                API-->>UI: Thử lại an toàn
            else Commit thành công
                DB-->>S: checkpointId và submittedAt
                S-->>API: Kết quả checkpoint
                API-->>UI: Đã gửi ảnh đầu tiên
                Jobs->>Engine: Hoàn tất task INITIAL_SHAPE theo eventId
                Jobs->>Notify: Gửi email ảnh tiến trình cho chủ đơn
                alt Chủ đơn là USER
                    Jobs->>Notify: Notification “Đã có hình ảnh đầu tiên<br/>của item #{orderItemId}”
                else Chủ đơn là Guest
                    Note over Notify: Chỉ email, không tạo notification tài khoản
                end
                Engine->>Engine: Mở task FINAL_PRODUCT của item
                opt Engine hoặc thông báo lỗi
                    Jobs->>DB: Ghi retry theo checkpointId và eventType
                end
            end
        end
    end
```

Upload object có thể xảy ra ngoài transaction database. Cần cơ chế dọn object mồ côi theo policy, nhưng không được hoàn tất task nếu Checkpoint và liên kết ảnh chưa commit.

## UC05.2 — Staff nộp sản phẩm hoàn thiện

```mermaid
sequenceDiagram
    actor Staff as STAFF phụ trách
    participant UI as Màn hình checkpoint
    participant API as Checkpoint API
    participant S as CheckpointService
    participant Storage as Lưu trữ ảnh
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Notification nội bộ

    Staff->>UI: Mở task FINAL_PRODUCT của OrderItem
    UI->>API: Đọc history checkpoint và reason rework
    API->>S: Kiểm tra actor và task
    S->>DB: Đọc active assignment, INITIAL_SHAPE,<br/>attempts và trạng thái KCS
    alt Không đủ quyền hoặc INITIAL_SHAPE chưa xong
        S-->>API: 403 hoặc 409
        API-->>UI: Không cho nộp final
    else Có attempt đang PENDING_REVIEW
        DB-->>S: Attempt hiện hành
        S-->>API: Đang chờ KCS
        API-->>UI: Không tạo attempt mới
    else Có task hợp lệ
        DB-->>S: History và next attempt context
        S-->>API: Dữ liệu form
        API-->>UI: Hiển thị ảnh/reason cũ
        Staff->>UI: Tải ảnh final và bấm gửi
        UI->>Storage: Upload ảnh
        Storage-->>UI: Object keys
        UI->>API: orderId, orderItemId, object keys,<br/>note và idempotency key
        API->>S: submitFinalProduct với staffId từ JWT
        S->>Storage: Xác minh object và metadata
        S->>DB: Khóa OrderItem và chuỗi attempt
        alt Request lặp
            DB-->>S: FINAL_PRODUCT attempt đã tạo
            S-->>API: Kết quả idempotent
            API-->>UI: Trạng thái PENDING_REVIEW
        else Hợp lệ
            S->>S: Sinh attempt = previousMax + 1 an toàn
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Tạo FINAL_PRODUCT PENDING_REVIEW,<br/>ảnh, audit và công việc bàn giao
            alt Unique conflict hoặc lưu lỗi
                DB-->>S: Rollback
                S-->>API: Conflict hoặc lỗi
                API-->>UI: Tải attempt hiện hành
            else Commit thành công
                DB-->>S: checkpointId và attempt
                S-->>API: PENDING_REVIEW
                API-->>UI: Đã gửi, chờ manager KCS
                Jobs->>Engine: Hoàn tất staff task,<br/>tạo manager KCS task cho attempt
                Jobs->>Notify: Thông báo nội bộ có checkpoint chờ KCS
                opt Bàn giao lỗi
                    Jobs->>DB: Ghi retry, không tạo attempt mới
                end
            end
        end
    end
```

Client không được tự chọn attempt để ghi đè. Mỗi lần rework tạo một record mới, history cũ vẫn đọc được.

## UC05.3 — Manager KCS sản phẩm hoàn thiện

```mermaid
sequenceDiagram
    actor M as MANAGER
    participant UI as Màn hình KCS
    participant API as KCS API
    participant S as CheckpointReviewService
    participant Complete as ProductionCompletionService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Email và Notification

    M->>UI: Mở task KCS FINAL_PRODUCT
    UI->>API: checkpointId và attempt
    API->>S: Đọc task với managerId từ JWT
    S->>DB: Đọc checkpoint, ảnh, spec snapshot,<br/>OrderItem, Order và version
    alt Không có quyền hoặc checkpoint không còn chờ
        S-->>API: 403 hoặc 409
        API-->>UI: Không xử lý task
    else Task hợp lệ
        DB-->>S: Attempt PENDING_REVIEW hiện hành
        S-->>API: Dữ liệu KCS
        API-->>UI: Hiển thị ảnh và yêu cầu
        M->>UI: PASS hoặc REWORK_REQUIRED với reason
        UI->>API: decision, reason và version
        API->>S: review với actor đã xác thực
        S->>DB: Khóa checkpoint, OrderItem và Order
        alt Version cũ hoặc đã có decision
            DB-->>S: Quyết định hiện hành
            S-->>API: Conflict hoặc kết quả idempotent
            API-->>UI: Tải lại
        else REWORK_REQUIRED thiếu reason
            S-->>API: Validation lỗi
            API-->>UI: Yêu cầu nhập reason
        else REWORK_REQUIRED hợp lệ
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Ghi Decision, reason,<br/>checkpoint REWORK_REQUIRED và công việc bàn giao
            DB-->>S: Commit
            S-->>API: Rework đã ghi
            API-->>UI: Kết quả
            Jobs->>Engine: Hoàn tất KCS task theo nhánh rework,<br/>tạo task FINAL_PRODUCT mới cho đúng item
            Jobs->>Notify: Thông báo assigned staff kèm reason
        else PASS
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Ghi Decision PASS và checkpoint PASSED
            S->>Complete: Kiểm tra toàn bộ loại trong cùng transaction
            Complete->>DB: Đọc trạng thái FINAL_PRODUCT mọi OrderItem
            alt Còn loại chưa PASS
                DB-->>Complete: remainingItemIds
                Complete-->>S: Chưa hoàn tất toàn đơn
                S->>DB: Lưu công việc hoàn tất nhánh item
                DB-->>S: Commit PASS của item
                S-->>API: ORDER_CREATING và remaining items
                API-->>UI: Item đạt, chờ loại khác
                Jobs->>Engine: Hoàn tất KCS task của item
            else Tất cả loại đã PASS
                Complete->>DB: Kiểm tra Order còn ORDER_CREATING<br/>và active assignment đúng assignmentId/orderId
                Complete->>DB: Chuyển READY_TO_SHIP, ghi history,<br/>đóng assignment và release staff,<br/>lưu ORDER_READY_TO_SHIP
                DB-->>S: Commit toàn đơn
                S-->>API: READY_TO_SHIP
                API-->>UI: Toàn đơn đã qua KCS
                Jobs->>Engine: Hoàn tất multi-instance và WF05
                Jobs->>Notify: Email READY_TO_SHIP cho USER hoặc Guest
                alt Chủ đơn là USER
                    Jobs->>Notify: Notification READY_TO_SHIP
                else Chủ đơn là Guest
                    Note over Notify: Email chứa link Order bảo mật
                end
            end
            opt Engine hoặc gửi thông báo lỗi sau commit
                Jobs->>DB: Ghi retry theo milestone,<br/>không PASS hoặc release lần hai
            end
        end
    end
```

Hai manager hoặc hai nhánh KCS cuối có thể cạnh tranh. Version/khóa Order và ràng buộc assignment phải bảo đảm một lần chuyển `READY_TO_SHIP`, một lần release và một event trạng thái.
