# WF09 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity Diagram](diagram.md) · [Use Case tổng quát](overview.svg).

Tên CancelOrderService, ReputationLedger, VoucherRestoration và worker dưới đây là vai trò kỹ thuật đề xuất. Mọi nguồn phải gọi cùng application service để không tạo nhiều cách cập nhật Order.

## UC09.1 — Khách chủ động hủy Order

```mermaid
sequenceDiagram
    actor C as USER hoặc Guest
    participant UI as Trang xác nhận hủy
    participant API as Cancel Order API
    participant Auth as Owner hoặc Guest Token Auth
    participant S as CancelOrderService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Email và Notification
    participant Cache as Cache

    C->>UI: Mở hủy từ MyOrder hoặc email
    alt Guest mở link bằng GET
        UI->>API: Đọc trang xác nhận với orderId và token
        API->>Auth: Kiểm tra token hash, Order, scope và rate limit
        Auth-->>API: Access context chỉ đọc
        API-->>UI: Hiển thị thông tin và nút xác nhận
        Note over UI,API: GET không thay đổi trạng thái
    end
    C->>UI: Nhập reason và bấm xác nhận
    UI->>API: Mutation với reason, version,<br/>idempotency key và credential
    API->>Auth: Xác thực USER owner hoặc Guest cancel token
    alt Không có quyền hoặc vượt rate limit
        Auth-->>API: 401, 403, 404 hoặc rate-limit error
        API-->>UI: Không hủy
    else Có quyền
        Auth-->>API: Actor context gắn đúng Order
        API->>S: cancelByCustomer với actor đã xác thực
        S->>DB: Khóa Order, voucher usage,<br/>reputation và active assignment
        alt Order không còn trước ORDER_ACCEPTED
            DB-->>S: Trạng thái hiện hành
            S-->>API: 409
            API-->>UI: Không còn quyền tự hủy
        else Cùng key và cùng nội dung đã xử lý
            DB-->>S: Cancellation result hiện có
            S-->>API: Kết quả idempotent
            API-->>UI: Không lặp penalty hoặc voucher
        else Cùng key nhưng nội dung khác
            S-->>API: 409
            API-->>UI: Không ghi đè request cũ
        else USER có finalPrice và reputation không đủ
            S-->>API: Lỗi nghiệp vụ
            API-->>UI: Order giữ nguyên
        else Đủ điều kiện
            S->>S: Tính penalty theo source và finalPrice<br/>CUSTOM chưa có giá hoặc Guest bằng 0
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Ghi CANCELLED, endOrderTime,<br/>source, reason, actor và history
            S->>DB: Ghi penalty ledger nếu có,<br/>phục hồi voucher/quota đúng một lần
            S->>DB: Đóng active assignment nếu có,<br/>lưu outbox và idempotency result
            alt Ghi thất bại
                DB-->>S: Rollback toàn bộ
                S-->>API: Chưa hủy
                API-->>UI: Thử lại an toàn
            else Commit thành công
                DB-->>S: Cancellation result
                S-->>API: CANCELLED và tác động đã áp dụng
                API-->>UI: Hiển thị kết quả hủy
                Jobs->>Engine: Kết thúc task/timer theo orderId và eventId
                Jobs->>Notify: Email chủ đơn và notification theo recipient
                Jobs->>Cache: Invalidate Order, voucher và reputation liên quan
                opt Một side effect lỗi
                    Jobs->>DB: Ghi retry theo eventId và consumer
                end
            end
        end
    end
```

Không participant nào thực hiện hoàn stock hoặc refund. Guest token không được đưa vào outbox, log hay notification payload.

## UC09.2 — Manager từ chối Order

```mermaid
sequenceDiagram
    actor M as MANAGER
    participant UI as Màn hình manager review
    participant API as Manager Review API
    participant S as CancelOrderService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Email và Notification

    M->>UI: Mở Order PENDING_APPROVAL
    UI->>API: Đọc manager review task
    API->>DB: Kiểm tra quyền, task và version
    alt Không có quyền hoặc task không hiện hành
        DB-->>API: 403 hoặc 409
        API-->>UI: Không cho quyết định
    else Task hợp lệ
        DB-->>API: Dữ liệu review theo quyền
        API-->>UI: Hiển thị Order
        M->>UI: Chọn REJECT và nhập reason
        UI->>API: decision, reason, task/order version<br/>và idempotency key
        API->>S: cancelByManagerReject với managerId từ JWT
        S->>DB: Khóa Order và manager task reference
        alt Reason thiếu hoặc Order không PENDING_APPROVAL
            S-->>API: 400 hoặc 409
            API-->>UI: Task chưa hoàn tất hoặc tải lại
        else Quyết định đã xử lý
            DB-->>S: Kết quả hiện hành
            S-->>API: Idempotent result hoặc conflict
            API-->>UI: Không từ chối lần hai
        else Hợp lệ
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Ghi source MANAGER_REJECTED,<br/>CANCELLED, reason, actor, end time và history
            S->>DB: Penalty bằng 0, phục hồi voucher/quota,<br/>đóng active assignment nếu có và lưu outbox
            alt Ghi thất bại
                DB-->>S: Rollback
                S-->>API: Review task chưa hoàn tất
                API-->>UI: Thử lại an toàn
            else Commit thành công
                DB-->>S: Cancellation result
                S-->>API: CANCELLED
                API-->>UI: Đã từ chối Order
                Jobs->>Engine: Hoàn tất manager task theo nhánh reject
                Jobs->>Notify: Email chủ đơn,<br/>USER thêm notification
                opt Workflow hoặc notification lỗi
                    Jobs->>DB: Ghi retry theo eventId
                end
            end
        end
    end
```

Manager reject là nhánh của review `PENDING_APPROVAL`, không phải endpoint cho manager hủy bất kỳ Order nào.

## UC09.3 — Hệ thống hủy do timeout/payment fail

```mermaid
sequenceDiagram
    participant Trigger as Timer hoặc Payment Event
    participant Worker as Cancellation Worker
    participant S as CancelOrderService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Email và Notification
    participant Reconcile as Payment Reconciliation

    Trigger->>Worker: orderId, reason, wait/payment reference<br/>và eventId
    Worker->>S: cancelBySystem với nguồn đã xác thực
    S->>DB: Khóa Order, workflow step,<br/>PaymentAttempt và assignment liên quan
    alt Reference không hợp lệ hoặc không thuộc Order
        DB-->>S: Không khớp
        S-->>Worker: Từ chối và ghi lỗi quan sát
    else CUSTOM timer nhưng form hợp lệ đã lưu
        DB-->>S: Wait state đã hoàn tất
        S-->>Worker: Stale timer, không hủy
    else Payment đã PAID hoặc success đã thắng
        DB-->>S: Payment result hiện hành
        S-->>Worker: Không hủy
    else Order đã CANCELLED
        DB-->>S: Cancellation result hiện có
        S-->>Worker: Kết quả idempotent
    else Wait state và Order còn hợp lệ
        S->>S: Chọn reason code từ trigger tin cậy<br/>và đặt penalty bằng 0
        Note over S,DB: Một transaction nghiệp vụ
        S->>DB: Ghi CANCELLED, endOrderTime,<br/>source, reference và history
        S->>DB: Phục hồi voucher/quota,<br/>đóng active assignment và lưu outbox
        alt Ghi thất bại
            DB-->>S: Rollback
            S-->>Worker: Job có thể retry
        else Commit thành công
            DB-->>S: Cancellation result
            S-->>Worker: CANCELLED
            Jobs->>Engine: Kết thúc task/timer còn mở<br/>theo orderId và eventId
            Jobs->>Notify: Email chủ đơn và notification phù hợp
            opt Workflow hoặc notification lỗi
                Jobs->>DB: Ghi retry theo eventId và consumer
            end
        end
    end
    opt Payment success đến sau CANCELLED
        Trigger->>Reconcile: Ghi ngoại lệ callback muộn
        Note over Reconcile,DB: Không tự restore Order hoặc tuyên bố refund
    end
```

Timer, webhook, form hợp lệ và staff-start phải dùng version/khóa chung ở các application service. Không được để mỗi delegate tự cập nhật trạng thái theo một quy tắc khác.
