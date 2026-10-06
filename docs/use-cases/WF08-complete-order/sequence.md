# WF08 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity Diagram](diagram.md) · [Use Case tổng quát](overview.svg).

Tên OrderLookupService, ReceiptService, ReputationService và worker dưới đây là vai trò kỹ thuật đề xuất. Database là nguồn quyết định quyền, trạng thái và số lượng. Guest token phải được băm khi lưu và không xuất hiện trong log hoặc event.

## UC08.1 — Tra cứu thông tin Order để nhận hàng

```mermaid
sequenceDiagram
    actor C as USER hoặc Guest
    participant UI as Trang thông tin Order
    participant API as Order Lookup API
    participant S as OrderLookupService
    participant Auth as JWT hoặc Guest Token Auth
    participant DB as Database

    C->>UI: Mở MyOrder hoặc link email
    UI->>API: orderId cùng JWT hoặc guest credential
    API->>Auth: Xác thực theo loại credential
    alt USER
        Auth->>DB: Kiểm tra userId từ JWT là owner
    else Guest
        Auth->>DB: So token hash, orderId, scope,<br/>expiry, revocation và rate limit
    end
    alt Không hợp lệ hoặc không có quyền
        DB-->>Auth: Từ chối
        Auth-->>API: 401, 403 hoặc 404 theo policy
        API-->>UI: Không trả snapshot
    else Có quyền
        DB-->>Auth: Access context đã giới hạn theo Order
        Auth-->>API: Actor context
        API->>S: getReceiptView với actor context
        S->>DB: Đọc Order, OrderItems,<br/>exportedQuantity và receipt hiện có
        S->>S: Lọc dữ liệu nội bộ
        alt Order SHIPPING và dữ liệu bàn giao hợp lệ
            S-->>API: Snapshot với canConfirmReceipt=true
            API-->>UI: Hiển thị form số lượng thực nhận
        else Order READY_TO_SHIP hoặc trạng thái chưa cho xác nhận
            S-->>API: Snapshot với canConfirmReceipt=false
            API-->>UI: Chỉ hiển thị thông tin và trạng thái
        else Order DELIVERED
            S-->>API: Snapshot receipt đã chốt
            API-->>UI: Hiển thị kết quả, không cho sửa
        else Đơn mới thiếu exportedQuantity
            S-->>API: canConfirmReceipt=false và lỗi dữ liệu
            API-->>UI: Chưa thể xác nhận
        end
    end
```

UC08.1 không tạo task hoặc message Camunda. Đây là chức năng đọc có kiểm soát, `guestSessionId` của giỏ không thay thế token truy cập Order.

## UC08.2 — Xác nhận số lượng thực nhận

```mermaid
sequenceDiagram
    actor C as USER hoặc Guest
    participant UI as Form nhận hàng
    participant API as Receipt API
    participant Auth as JWT hoặc Guest Token Auth
    participant S as ReceiptService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda tùy chọn
    participant Rep as ReputationService
    participant Review as Catalog Review Invitation
    participant Read as Read model và cache

    C->>UI: Nhập số lượng cho mọi OrderItem
    UI->>API: receivedItems, acceptMismatch, note,<br/>version và idempotency key
    API->>Auth: Xác thực owner hoặc token scope nhận hàng
    alt Không có quyền
        Auth-->>API: 401 hoặc 403
        API-->>UI: Không xử lý receipt
    else Có quyền
        Auth-->>API: Actor context gắn đúng Order
        API->>S: confirmReceipt với actor đã xác thực
        S->>DB: Khóa Order, OrderItems và request key
        alt Order không SHIPPING hoặc version cũ
            DB-->>S: Trạng thái hoặc version hiện hành
            S-->>API: 409 hoặc kết quả đã hoàn tất
            API-->>UI: Tải lại Order
        else Cùng key và cùng nội dung đã hoàn tất
            DB-->>S: Receipt result hiện có
            S-->>API: Kết quả idempotent
            API-->>UI: Không cộng điểm hoặc phát event lần hai
        else Cùng key nhưng nội dung khác
            S-->>API: 409
            API-->>UI: Không ghi đè receipt
        else Danh sách hoặc số lượng không hợp lệ
            S-->>API: 400 và không lưu một phần
            API-->>UI: Hiển thị lỗi từng dòng
        else Đơn mới thiếu exportedQuantity
            S-->>API: Lỗi dữ liệu có thể quan sát
            API-->>UI: Chưa thể hoàn tất
        else Dữ liệu hợp lệ
            S->>S: So sánh receivedQuantity<br/>với exportedQuantity hiệu lực
            alt Có lệch và acceptMismatch=false
                S-->>API: matched=false, completed=false,<br/>mismatches và status SHIPPING
                API-->>UI: Hiển thị chênh lệch để khách quyết định
            else Khớp hoặc khách chấp nhận lệch
                Note over S,DB: Một transaction nghiệp vụ
                S->>DB: Lưu receipt, mọi receivedQuantity,<br/>matched, acceptedMismatch, note và actor
                S->>DB: Chuyển DELIVERED, ghi endOrderTime,<br/>history, version và công việc sau commit
                opt USER và reputation cùng database
                    S->>DB: Ghi reputation ledger cộng 2<br/>với unique key theo orderId và reason
                end
                alt Ghi thất bại
                    DB-->>S: Rollback toàn bộ
                    S-->>API: Order vẫn SHIPPING
                    API-->>UI: Thử lại an toàn
                else Commit thành công
                    DB-->>S: Receipt result và DELIVERED
                    S-->>API: matched, completed=true,<br/>mismatches, message và status
                    API-->>UI: Đơn đã hoàn tất
                    Jobs->>Read: Làm mới read model/cache theo version
                    opt Process có wait state nhận hàng
                        Jobs->>Engine: Correlate ORDER_DELIVERED<br/>theo orderId và eventId
                    end
                    alt Actor là USER và reputation xử lý tách rời
                        Jobs->>Rep: Cộng 2 theo orderId và reason idempotent
                    else Actor là Guest
                        Note over Jobs,Rep: Không có reputation
                    end
                    opt Order CATALOG
                        Jobs->>Review: Mở quyền và tạo lời mời đánh giá idempotent
                    end
                    Note over Jobs,Review: Không gửi email hoặc notification<br/>trạng thái DELIVERED
                    opt Một side effect lỗi
                        Jobs->>DB: Ghi retry theo eventId và consumer
                    end
                end
            end
        end
    end
```

Request chênh lệch chưa được chấp nhận không chuyển trạng thái và không kích hoạt side effect hoàn tất. Retry sau khi đã commit phải đọc kết quả đã lưu thay vì chạy lại cộng reputation hoặc phát lời mời đánh giá.
