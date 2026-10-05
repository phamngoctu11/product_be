# WF04 — Biểu đồ tuần tự

Nguồn: [đặc tả](usecase.md) · [Activity Diagram](diagram.md) · [Use Case tổng quát](overview.svg).

Tên PaymentService, ProductionService, worker và event dưới đây là vai trò kỹ thuật đề xuất. Database giữ trạng thái nghiệp vụ, Camunda điều phối message, timer và user task. Webhook được xác minh và commit trước khi correlate vào process.

## UC04.1 — Mở và thực hiện thanh toán online

```mermaid
sequenceDiagram
    actor U as USER
    participant UI as Trang chi tiết Order
    participant API as Payment API
    participant S as PaymentService
    participant DB as Database
    participant Provider as Cổng thanh toán
    participant Jobs as Bộ xử lý bàn giao
    participant Engine as Camunda

    U->>UI: Bấm Thanh toán
    UI->>API: orderId và idempotency key
    API->>S: openPayment với userId từ JWT
    S->>DB: Khóa hoặc đọc Order, owner,<br/>finalPrice, assignment và PaymentAttempt
    alt Sai owner hoặc paymentMethod không phải ONLINE
        DB-->>S: Không hợp lệ
        S-->>API: 403 hoặc 409
        API-->>UI: Không mở payment
    else Order không còn ORDER_ACCEPTED hoặc giá chưa chính thức
        DB-->>S: Trạng thái không phù hợp
        S-->>API: Conflict
        API-->>UI: Hiển thị trạng thái hiện tại
    else Đã PAID
        DB-->>S: Payment PAID
        S-->>API: Trạng thái đã thanh toán
        API-->>UI: Không tạo giao dịch mới
    else Có cùng idempotency result hoặc active attempt còn hạn
        DB-->>S: PaymentAttempt hiện có
        S-->>API: URL, amount và dueAt cũ
        API-->>UI: Tiếp tục attempt hiện có
    else Có thể mở payment
        S->>Provider: Tạo giao dịch với reference duy nhất<br/>và expectedAmount từ server
        alt Provider từ chối hoặc timeout
            Provider-->>S: Lỗi
            S->>DB: Ghi lỗi khởi tạo có thể retry hoặc reconcile
            S-->>API: Chưa mở payment thành công
            API-->>UI: Thử lại an toàn
        else Provider tạo giao dịch
            Provider-->>S: providerTransactionId và URL hoặc QR
            Note over S,DB: Transaction lưu PaymentAttempt chính thức
            S->>DB: Ghi PENDING, openedAt, dueAt = openedAt + 1 giờ,<br/>provider reference, idempotency và công việc bàn giao
            alt Lưu DB thất bại sau khi provider đã tạo
                DB-->>S: Rollback
                S->>DB: Đăng ký hoặc ghi nhận reconciliation theo thiết kế
                S-->>API: Không báo thành công giả
                API-->>UI: Trạng thái đang được đối soát
            else Commit thành công
                DB-->>S: PaymentAttempt đã mở
                S-->>API: attemptId, URL, amount và dueAt
                API-->>UI: Chuyển USER tới provider
                Jobs->>Engine: Correlate PAYMENT_OPENED theo orderId và eventId
                Engine->>Engine: Vào event-based gateway chờ<br/>success, failure hoặc timer dueAt
                opt Correlation lỗi
                    Jobs->>DB: Ghi retry, không tạo PaymentAttempt mới
                end
            end
        end
    end
```

Provider API là side effect ngoài transaction database. Triển khai cần reference và idempotency ổn định để phục hồi tình huống provider đã tạo giao dịch nhưng phản hồi hoặc lưu DB gặp lỗi.

## UC04.2 — Tiếp nhận kết quả thanh toán

```mermaid
sequenceDiagram
    actor Provider as Cổng thanh toán
    participant API as Webhook API
    participant S as PaymentService
    participant DB as Database
    participant Jobs as Bộ xử lý bàn giao
    participant Engine as Camunda
    participant Cancel as CancelService
    participant Notify as Notification và Email

    par Webhook từ provider
        Provider->>API: Raw payload, signature và provider headers
        API->>S: Xử lý raw webhook
        S->>S: Xác minh signature và chuẩn hóa result
        alt Signature không hợp lệ
            S-->>API: Từ chối
            API-->>Provider: Phản hồi lỗi theo hợp đồng
        else Signature hợp lệ
            S->>DB: Khóa PaymentAttempt và Order,<br/>đối chiếu reference, amount, status và dueAt
            alt Sai amount hoặc reference
                DB-->>S: Không khớp
                S-->>API: Từ chối và audit an toàn
                API-->>Provider: Phản hồi theo hợp đồng
            else Callback success và attempt còn PENDING hợp lệ
                Note over S,DB: Một transaction nghiệp vụ
                S->>DB: Ghi PAID, paidAt, provider result<br/>và công việc sau commit
                DB-->>S: Commit
                S-->>API: Acknowledgement
                API-->>Provider: Đã nhận
                Jobs->>Engine: Correlate PAYMENT_SUCCESS theo eventId
                Jobs->>Notify: Notification assigned staff lấy từ DB
                Note over DB,Engine: Order vẫn ORDER_ACCEPTED
            else Callback failure và attempt còn PENDING
                S->>DB: Ghi FAILED và yêu cầu hủy hệ thống một lần
                DB-->>S: Commit kết quả failure
                Jobs->>Engine: Correlate PAYMENT_FAILED
                Jobs->>Cancel: Hủy theo F16
                Cancel->>DB: CANCELLED, release assignment,<br/>khôi phục voucher và lưu history
                Jobs->>Notify: Email hủy và notification phù hợp
                S-->>API: Acknowledgement
                API-->>Provider: Đã nhận
            else Callback lặp hoặc trạng thái đã kết thúc
                DB-->>S: Kết quả hiện hành
                S-->>API: Acknowledgement idempotent<br/>hoặc reconciliation exception nếu success đến muộn
                API-->>Provider: Không xử lý nghiệp vụ lần hai
            end
        end
    and Timer Camunda tại dueAt
        Engine->>S: PAYMENT_TIMEOUT(orderId, attemptId)
        S->>DB: Khóa PaymentAttempt và Order
        alt Attempt đã PAID hoặc không còn PENDING
            DB-->>S: Không được expire
            S-->>Engine: Timer không tác động
        else Attempt vẫn PENDING
            Note over S,DB: Một transaction nghiệp vụ
            S->>DB: Ghi EXPIRED và yêu cầu hủy PAYMENT_TIMEOUT
            DB-->>S: Commit
            S->>Cancel: Hủy hệ thống một lần
            Cancel->>DB: CANCELLED, release assignment,<br/>khôi phục voucher và history
            Jobs->>Notify: Email hủy, notification USER và staff
            S-->>Engine: Kết thúc nhánh timeout
        end
    end

    opt PAID commit nhưng correlation hoặc notification lỗi
        Jobs->>DB: Ghi retry theo eventId
        Note over Jobs,Engine: Timer đọc PAID nên không được hủy Order
    end
```

Webhook và timer có thể chạy đồng thời nhưng khóa/trạng thái database phải chọn một kết quả. Callback thành công đến sau khi `EXPIRED/CANCELLED` chỉ tạo ngoại lệ đối soát, không tự phục hồi Order hoặc tự công bố hoàn tiền.

## UC04.3 — Staff bắt đầu sản xuất

```mermaid
sequenceDiagram
    actor Staff as STAFF phụ trách
    participant UI as Danh sách công việc
    participant API as Production API
    participant S as ProductionService
    participant DB as Database
    participant Engine as Camunda
    participant Jobs as Bộ xử lý sau commit
    participant Notify as Email và Notification

    Staff->>UI: Mở task chờ bắt đầu
    UI->>API: Đọc Order và trạng thái payment
    API->>DB: Kiểm tra assigned staff và dữ liệu được phép
    DB-->>UI: Order, payment status và thông tin sản xuất
    Staff->>UI: Bấm Bắt đầu làm,<br/>nhập lateStartReason nếu có
    UI->>API: orderId, version, idempotency key và reason
    API->>S: startProduction với staffId từ JWT
    S->>DB: Khóa Order, active assignment,<br/>agreement hoặc snapshot và payment
    alt Không phải staff phụ trách hoặc assignment đã release
        DB-->>S: Không có quyền
        S-->>API: 403 hoặc 409
        API-->>UI: Không bắt đầu
    else Order đã ORDER_CREATING do request trước
        DB-->>S: productionStartedAt và deadlines hiện có
        S-->>API: Kết quả idempotent
        API-->>UI: Không ghi lại thời điểm
    else Order không còn ORDER_ACCEPTED
        DB-->>S: Trạng thái không phù hợp
        S-->>API: Conflict
        API-->>UI: Tải trạng thái hiện tại
    else ONLINE chưa PAID hoặc dữ liệu chính thức thiếu
        DB-->>S: Chưa đủ điều kiện
        S-->>API: Từ chối, user task vẫn chờ
        API-->>UI: Hiển thị điều kiện còn thiếu
    else COD hợp lệ hoặc ONLINE đã PAID
        Note over S,DB: Một transaction nghiệp vụ
        S->>DB: Ghi productionStartedAt một lần,<br/>ORDER_CREATING, lateStartReason và history
        loop Từng OrderItem
            S->>S: addProductionDays từ ngày kế tiếp,<br/>bỏ Chủ nhật và ngày nghỉ cấu hình
            S->>DB: Lưu computedCompletionAt của item
        end
        S->>DB: Lưu công việc hoàn tất user task<br/>và phát PRODUCTION_STARTED
        alt Ghi thất bại hoặc cạnh tranh với cancel
            DB-->>S: Rollback hoặc conflict
            S-->>API: Chưa bắt đầu
            API-->>UI: Tải trạng thái hiện tại
        else Commit thành công
            DB-->>S: ORDER_CREATING và deadlines
            S-->>API: productionStartedAt và kết quả từng item
            API-->>UI: Đã bắt đầu sản xuất
            Jobs->>Engine: Hoàn tất staff task một lần,<br/>bàn giao WF05
            Jobs->>Notify: PRODUCTION_STARTED
            Note over Notify: USER nhận email và notification,<br/>Guest chỉ email
            opt Engine hoặc gửi thông báo lỗi
                Jobs->>DB: Ghi retry, không yêu cầu staff bấm lại
            end
        end
    end
```

Staff vẫn giữ active assignment sau UC04.3. WF05 chỉ release khi mọi loại sản phẩm qua KCS checkpoint 2, hoặc nghiệp vụ hủy release trước đó.
