# WF02 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity](diagram.md) · [Use Case tổng quát](overview.svg).

Tên CheckoutService, ReviewService, AccessPolicy và bộ xử lý sau commit dưới đây là **vai trò kỹ thuật đề xuất**, cần ánh xạ với code khi triển khai. Event trong sơ đồ là tên nghiệp vụ, không khẳng định tên topic hiện hành. Các thao tác ghi được gom thành transaction, nếu Camunda không tham gia transaction đó thì dùng bàn giao bền vững/retry có chống trùng.

## UC02.1 — Đặt hàng catalog từ giỏ

```mermaid
sequenceDiagram
    actor Buyer as USER hoặc Guest
    participant UI as Giao diện checkout
    participant API as Checkout API
    participant Policy as AccessPolicy
    participant S as CheckoutService
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Email và Notification

    Buyer->>UI: Chọn mục, contact, phương thức và voucher
    UI->>API: Checkout + idempotency key + JWT hoặc guest session
    API->>Policy: Xác định danh tính và quyền giỏ
    alt Danh tính hoặc quyền không hợp lệ
        Policy-->>API: Từ chối
        API-->>UI: Lỗi truy cập, không tạo Order
    else Có quyền
        API->>S: Checkout theo chủ giỏ đã xác thực
        S->>DB: Đọc kết quả idempotency
        alt Cùng key, cùng nội dung và đã tạo đơn thành công
            DB-->>S: Order đã tạo
            S-->>API: Kết quả cũ, không ghi lại nghiệp vụ
            API-->>UI: Order hiện có
        else Key trùng nhưng nội dung khác
            S-->>API: Conflict
            API-->>UI: Yêu cầu không hợp lệ
        else Yêu cầu mới
            S->>DB: Đọc mục giỏ, catalog, reputation và voucher
            DB-->>S: Dữ liệu và giá server
            S->>S: Validate quantity, contact, catalog, phương thức, voucher
            alt Validation thất bại
                S-->>API: Lỗi cụ thể, không ghi đơn
                API-->>UI: Yêu cầu khách chỉnh lại
            else Hợp lệ
                S->>S: Tính tiền và durationDays riêng từng item
                Note over S,DB: Một transaction, kiểm tra lại dữ liệu cạnh tranh khi ghi
                S->>DB: Tạo Order PENDING_APPROVAL, items, snapshot, token hash cho Guest,<br/>ghi usage, cập nhật giỏ, idempotency và công việc sau commit
                alt Ghi thất bại hoặc xung đột
                    DB-->>S: Rollback hoặc unique conflict
                    S-->>API: Lỗi hoặc kết quả của yêu cầu trùng đã thắng
                    API-->>UI: Kết quả nhất quán, không mất giỏ/quota một phần
                else Commit thành công
                    DB-->>S: orderId và snapshot đã lưu
                    S-->>API: Order và thông tin truy cập đúng chủ
                    API-->>UI: PENDING_APPROVAL, tổng tiền, orderId
                    Jobs->>DB: Nhận công việc đã commit
                    Jobs->>Engine: Đăng ký review theo orderId, chống khởi trùng
                    Jobs->>Notify: ORDER_CREATED / PENDING_APPROVAL
                    Notify-->>Buyer: Email, chỉ USER có thêm notification
                    opt Engine hoặc consumer lỗi
                        Jobs->>DB: Ghi retry và lỗi, giữ Order hiện có
                    end
                end
            end
        end
    end
```

Email và khởi process không buộc API chờ. Guest link được kiểm tra token theo Order, truy cập GET không thực hiện hủy. Không tạo payment URL trong checkout.

## UC02.2 — Xem danh sách và chi tiết đơn chờ duyệt

```mermaid
sequenceDiagram
    actor M as MANAGER
    participant UI as Màn hình duyệt đơn
    participant API as Review API
    participant Policy as AccessPolicy
    participant S as ReviewService
    participant DB as Database

    M->>UI: Mở đơn chờ duyệt
    UI->>API: Danh sách PENDING_APPROVAL + phân trang
    API->>Policy: Kiểm tra JWT và quyền MANAGER
    alt Không đủ quyền
        Policy-->>API: Từ chối
        API-->>UI: Lỗi, không lộ dữ liệu
    else Đủ quyền
        API->>S: Tìm đơn chờ duyệt
        S->>DB: SELECT có phân trang
        DB-->>S: Danh sách hoặc rỗng
        S-->>API: Dữ liệu danh sách
        API-->>UI: Hiển thị kết quả
        opt Manager chọn một Order
            M->>UI: Xem chi tiết
            UI->>API: Đọc orderId
            API->>Policy: Kiểm tra quyền cho yêu cầu mới
            alt Quyền không còn hợp lệ
                API-->>UI: Từ chối truy cập
            else Đủ quyền
                API->>S: Đọc chi tiết hiện tại
                S->>DB: Order, items, snapshot, history và version
                DB-->>S: Dữ liệu hoặc không tìm thấy
                alt Không tìm thấy
                    S-->>API: Lỗi không tồn tại
                    API-->>UI: Không hiển thị nội dung đơn
                else Đơn đã chuyển trạng thái
                    S-->>API: Trạng thái mới nhất
                    API-->>UI: Hiển thị trạng thái, không dùng thao tác review cũ
                else Còn chờ duyệt
                    S-->>API: Chi tiết, giá CUSTOM chưa chốt được đánh dấu chưa xác định
                    API-->>UI: Thông tin đánh giá khả năng thực hiện
                    opt Manager muốn chọn staff khi duyệt
                        UI->>API: Đọc staff đủ điều kiện, có kiểm tra quyền
                        API->>DB: Staff AVAILABLE và không có active assignment
                        DB-->>API: Danh sách tại thời điểm đọc
                        API-->>UI: Danh sách tham khảo
                    end
                end
            end
        end
    end
    Note over UI,DB: UC chỉ đọc, UC02.3 phải kiểm tra lại trạng thái và staff khi ghi
```

## UC02.3 — Chấp nhận hoặc từ chối tiếp nhận đơn

```mermaid
sequenceDiagram
    actor M as MANAGER
    participant UI as Màn hình duyệt đơn
    participant API as Review API
    participant S as ReviewService
    participant Shared as Nghiệp vụ chung WF03 và WF09
    participant DB as Database
    participant Jobs as Bộ xử lý sau commit
    participant Engine as Camunda
    participant Notify as Email và Notification

    M->>UI: Approve hoặc reject, reason, staffId tùy chọn
    UI->>API: Quyết định + orderId + version
    API->>API: Xác thực MANAGER, actor lấy từ JWT
    alt Không đủ quyền hoặc dữ liệu sai
        API-->>UI: Lỗi, không xử lý quyết định
    else Hợp lệ
        API->>S: Xử lý quyết định với manager đã xác thực
        S->>DB: Đọc và kiểm soát cạnh tranh Order/version
        S->>Engine: Kiểm tra bước review của Order
        alt Đơn không còn chờ hoặc task chưa sẵn sàng
            S-->>API: Kết quả đã xử lý hoặc conflict/chưa sẵn sàng
            API-->>UI: Không ghi đè, tải trạng thái mới
            Note over S,Engine: Thiếu task do lỗi khởi process cần reconciliation, không đổi DB đơn lẻ
        else Đơn và task hợp lệ
            Note over S,DB: Bắt đầu một transaction nghiệp vụ, kiểm tra lại trạng thái/version lúc ghi
            alt Chấp nhận
                opt Manager chọn staff
                    S->>Shared: Kiểm tra và ghi assignment theo WF03, cùng transaction
                    Shared->>DB: Ràng buộc một active assignment/staff, kiểm tra AVAILABLE
                    DB-->>Shared: Assignment hợp lệ hoặc conflict
                    Shared-->>S: Kết quả gán hoặc lỗi
                end
                S->>DB: Nếu không có lỗi: ghi managerApprovedAt, actor, history,<br/>PENDING_ASSIGNMENT nếu chưa gán, lưu bàn giao WF03
                opt Là CUSTOM và các bước trước hợp lệ
                    S->>DB: Lưu confirmationDueAt = approvedAt + 24h,<br/>đăng ký chờ form, không reset hạn khi retry
                end
            else Từ chối với reason bắt buộc
                S->>Shared: Tác động hủy dùng chung WF09, cùng transaction
                Shared->>DB: Hoàn voucher một lần, guest giữ giới hạn usage,<br/>release assignment nếu có, không trừ reputation
                S->>DB: CANCELLED, reason, endOrderTime, actor và history,<br/>lưu công việc kết thúc process và thông báo hủy
            end
            alt Staff conflict hoặc bất kỳ lỗi ghi nào
                S->>DB: Rollback toàn bộ
                S-->>API: Conflict hoặc lỗi, không lưu một phần quyết định
                API-->>UI: Chọn lại hoặc gửi lại theo trạng thái hiện tại
            else Ghi hợp lệ
                S->>DB: Commit nghiệp vụ và công việc bàn giao
                DB-->>S: Kết quả đã lưu
                S-->>API: Quyết định, trạng thái, assignment nếu có
                API-->>UI: Kết quả và tình trạng đồng bộ workflow
                Jobs->>DB: Nhận công việc đã commit
                Jobs->>Engine: Hoàn tất review một lần theo quyết định
                alt Quyết định duyệt
                    Engine->>Engine: Bàn giao WF03, đăng ký timer CUSTOM đúng hạn đã lưu
                    opt Có assignment
                        Jobs->>Notify: Notification cho staff phụ trách
                    end
                    Note over Engine,Notify: Không email trạng thái trung gian, ORDER_ACCEPTED do nhánh xác nhận WF03
                else Quyết định từ chối
                    Jobs->>Notify: ORDER_CANCELLED với lý do
                    Notify->>Notify: Email chủ đơn, USER thêm notification, Guest chỉ email
                end
                opt Lỗi engine hoặc gửi thông báo
                    Jobs->>DB: Ghi lỗi và retry theo khóa nghiệp vụ
                    Note over Jobs,Engine: Không duyệt lại, không hoàn quota hay gán staff lần hai
                end
            end
        end
    end
```

Sơ đồ minh họa phương án engine tách transaction DB. Worker phải bàn giao đúng thứ tự, chống trùng và không để thao tác kế tiếp chạy khi bước trước chưa đồng bộ. Nếu engine dùng chung transaction thì hoàn tất task trong transaction chung, không vừa hoàn tất ở đó vừa phát lệnh hoàn tất lần nữa.

Timer 24h chỉ dành cho CUSTOM đã được manager duyệt, user gửi form hợp lệ sẽ dừng timer ở WF03. Thanh toán ONLINE và thao tác staff bắt đầu sản xuất thuộc WF04, không được suy ra từ quyết định approve ở đây.
