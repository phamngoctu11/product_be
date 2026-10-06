# WF07 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity Diagram](diagram.md) · [Use Case tổng quát](overview.svg).

Tên ShippingHandoverService, event và worker dưới đây là vai trò kỹ thuật đề xuất. Database giữ quyền, số lượng và trạng thái. Không có participant shipper vì bên vận chuyển nằm ngoài phạm vi hệ thống.

## UC07.1 — Staff ghi nhận bàn giao vận chuyển

```mermaid
sequenceDiagram
    actor Staff as STAFF đã phụ trách
    participant UI as Màn hình bàn giao
    participant API as Shipping Handover API
    participant S as ShippingHandoverService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda tùy chọn
    participant Read as Read model và cache

    Staff->>UI: Mở Order READY_TO_SHIP
    UI->>API: Đọc dữ liệu bàn giao theo orderId
    API->>S: Lấy dữ liệu với staffId từ JWT
    S->>DB: Đọc Order, OrderItems,<br/>assignment history và KCS
    alt Không phải staff lịch sử phụ trách
        DB-->>S: Không có quyền
        S-->>API: 403
        API-->>UI: Không cho bàn giao
    else Order không còn READY_TO_SHIP
        DB-->>S: Trạng thái hiện hành
        S-->>API: 409 hoặc kết quả đã commit
        API-->>UI: Tải lại Order
    else Đủ quyền
        DB-->>S: Danh sách item và version
        S-->>API: Dữ liệu được phép xem
        API-->>UI: Hiển thị từng dòng và số lượng đặt
        Staff->>UI: Nhập số lượng bàn giao và ghi chú
        UI->>API: orderVersion, danh sách item,<br/>handoverNote và idempotency key
        API->>S: handover với actor đã xác thực
        S->>DB: Khóa Order, OrderItems và request key
        alt Thiếu, trùng, lạ hoặc số lượng không hợp lệ
            S-->>API: 400 và không lưu một phần
            API-->>UI: Hiển thị lỗi từng dòng
        else Cùng key và cùng nội dung đã xử lý
            DB-->>S: Kết quả bàn giao hiện có
            S-->>API: Kết quả idempotent
            API-->>UI: SHIPPING và số lượng đã lưu
        else Cùng key nhưng nội dung khác
            S-->>API: 409
            API-->>UI: Không ghi đè kết quả cũ
        else Hợp lệ
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Lưu exportedQuantity cho mọi item,<br/>actor, shippingStartedAt và ghi chú
            S->>DB: Chuyển SHIPPING, ghi history,<br/>version và công việc sau commit
            alt Ghi thất bại
                DB-->>S: Rollback toàn bộ
                S-->>API: Order vẫn READY_TO_SHIP
                API-->>UI: Thử lại an toàn
            else Commit thành công
                DB-->>S: SHIPPING và dữ liệu bàn giao
                S-->>API: shippingStartedAt,<br/>expectedShippingDays bằng 2 và items
                API-->>UI: Đã ghi nhận bàn giao
                Jobs->>Read: Làm mới read model và cache theo eventId
                opt Process vòng đời kéo dài tới WF08
                    Jobs->>Engine: Correlate ORDER_SHIPPING<br/>theo orderId và eventId
                    Engine->>Engine: Chuyển sang bước chờ khách nhận hàng
                end
                Note over Jobs,Read: Không route ORDER_SHIPPING thành<br/>email hoặc notification trạng thái chủ đơn
                opt Đồng bộ sau commit lỗi
                    Jobs->>DB: Ghi retry theo orderId và eventId
                end
            end
        end
    end
```

Không có lời gọi carrier API, webhook tracking hoặc xử lý COD trong sequence này. Nếu job sau commit retry, `eventId` và trạng thái database phải ngăn hoàn tất workflow hoặc cập nhật read model theo nghĩa nghiệp vụ lần hai.
