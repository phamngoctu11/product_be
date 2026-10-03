# WF01 — Biểu đồ tuần tự

Nguồn: [usecase.md](usecase.md) và [target.txt](../../target.txt). [Sơ đồ hoạt động](diagram.md) · [Use Case tổng quan](overview.svg).

Các participant Controller/Service và tên phương thức là phân chia trách nhiệm đề xuất, không khẳng định đã được triển khai đầy đủ. Database bao gồm dữ liệu nghiệp vụ cần lưu; chi tiết từng repository được lược để sơ đồ dễ đọc.

## UC01.1 — Xem danh sách và chi tiết bản nháp

```mermaid
sequenceDiagram
    autonumber
    actor U as USER
    participant UI as Màn hình Custom Request
    participant C as CustomRequestController
    participant A as Xác thực và phân quyền
    participant S as CustomRequestService
    participant DB as Database
    U->>UI: Mở bản nháp của tôi
    UI->>C: Lấy danh sách bản nháp
    C->>A: Kiểm tra JWT và role USER
    alt Không đủ quyền
        A-->>C: Từ chối
        C-->>UI: Lỗi xác thực hoặc phân quyền
        UI-->>U: Hướng dẫn đăng nhập hoặc báo lỗi
    else Hợp lệ
        A-->>C: userId
        C->>S: listMyDrafts(userId)
        S->>DB: Truy vấn theo ownerId
        DB-->>S: Danh sách, có thể rỗng
        S-->>C: Draft summaries
        C-->>UI: Kết quả
        UI-->>U: Hiển thị danh sách hoặc trạng thái rỗng
        opt User chọn một bản nháp
            U->>UI: Mở chi tiết draftId
            UI->>C: Lấy chi tiết với JWT
            C->>A: Xác thực lại yêu cầu
            A-->>C: userId hợp lệ
            C->>S: getOwnedDraft(userId, draftId)
            S->>DB: Tìm draft theo id và owner
            DB-->>S: Kết quả
            alt Không tồn tại hoặc không thuộc user
                S-->>C: Lỗi truy cập, không trả nội dung
                C-->>UI: Lỗi
            else Đúng chủ sở hữu
                S-->>C: Spec, quantity, note, attachments
                C-->>UI: Chi tiết bản nháp
                UI-->>U: Hiển thị nội dung
            end
        end
    end
```

Mọi request xem chi tiết đều phải xác thực, không dựa vào việc đã mở danh sách. Bước xác thực thất bại của request chi tiết được xử lý như request đầu, lược khỏi nhánh để tránh lặp sơ đồ.

## UC01.2 — Tạo/cập nhật bản nháp

```mermaid
sequenceDiagram
    autonumber
    actor U as USER
    participant UI as Form Custom Request
    participant C as CustomRequestController
    participant A as Xác thực và phân quyền
    participant S as CustomRequestService
    participant DB as Database
    U->>UI: Nhập hoặc chỉnh spec, quantity, note và ảnh
    Note over UI,S: Upload lỗi phải được xử lý trước khi gắn attachment hợp lệ
    U->>UI: Lưu bản nháp
    UI->>C: saveDraft(draftId nếu sửa, payload, JWT)
    C->>A: Kiểm tra JWT và role USER
    alt Không đủ quyền
        C-->>UI: Lỗi xác thực hoặc phân quyền
        UI-->>U: Không thể lưu
    else Hợp lệ
        A-->>C: userId
        C->>S: saveOwnedDraft(userId, payload)
        opt Cập nhật bản nháp đã có
            S->>DB: Tìm draft thuộc user
            DB-->>S: Draft hoặc không có quyền sở hữu
        end
        S->>S: Kiểm tra ownership, dữ liệu và attachment
        alt Không hợp lệ hoặc không đúng chủ
            S-->>C: Lỗi cụ thể, không lưu
            C-->>UI: Lỗi trường hoặc quyền
            UI-->>U: Chỉnh sửa thông tin
        else Hợp lệ
            S->>DB: Transaction lưu draft và liên kết attachment
            alt Lưu thất bại
                DB-->>S: Rollback, lỗi
                S-->>C: Không lưu được
                C-->>UI: Thông báo lỗi
            else Commit thành công
                DB-->>S: draftId và dữ liệu đã lưu
                S-->>C: Kết quả
                C-->>UI: draftId, nội dung, updatedAt
                UI-->>U: Đã lưu bản nháp
            end
        end
    end
    Note over S,DB: Không tạo Order, payment, email tạo đơn hoặc task manager ở UC này
```

Upload/attachment là dữ liệu đầu vào đã được xác minh; quy trình lưu trữ ảnh chi tiết không được áp đặt thêm vào nghiệp vụ WF01. Validation lưu nháp thực hiện theo schema đã thống nhất, không tự coi tất cả trường của đơn đều bắt buộc ngay khi lưu nháp.

## UC01.3 — Gửi bản nháp tạo đơn custom

```mermaid
sequenceDiagram
    autonumber
    actor U as USER
    participant UI as Màn hình gửi yêu cầu
    participant C as CustomRequestController
    participant A as Xác thực và phân quyền
    participant S as CustomOrderService
    participant DB as Database
    participant W as WorkflowStarter
    participant E as EventPublisher
    participant N as Email và Notification Consumer
    U->>UI: Xác nhận gửi bản nháp
    UI->>C: submit(draftId, payload, idempotencyKey, JWT)
    C->>A: Kiểm tra JWT và role USER
    alt Không đủ quyền
        C-->>UI: Từ chối, không tạo đơn
    else Hợp lệ
        A-->>C: userId
        C->>S: submitOwnedDraft(userId, request)
        S->>DB: Đọc draft thuộc user và kết quả lần gửi
        DB-->>S: Dữ liệu kiểm tra
        alt Yêu cầu trùng đã thành công
            S-->>C: Order đã tạo
            C-->>UI: orderId hiện có
        else Sai quyền, dữ liệu hoặc key xung đột
            S-->>C: Lỗi, không tạo đơn mới
            C-->>UI: Thông tin cần sửa hoặc lỗi quyền
        else Yêu cầu mới hợp lệ
            S->>S: Chuẩn bị spec và contact snapshot
            S->>DB: Transaction tạo Order, item, liên kết draft và kết quả idempotency
            Note over S,DB: CUSTOM, một loại item, status PENDING_APPROVAL; chống gửi đồng thời ở transaction
            alt Transaction thất bại
                DB-->>S: Rollback
                S-->>C: Lỗi tạo đơn
                C-->>UI: Không có đơn mới hợp lệ
            else Commit thành công
                DB-->>S: orderId
                S->>W: Đăng ký khởi chạy manager review theo orderId
                S->>E: Đăng ký ORDER_CREATED sau commit
                Note over S,E: Lỗi khởi workflow hoặc publish được ghi nhận để retry, không tạo lại Order
                S-->>C: orderId, CUSTOM, PENDING_APPROVAL
                C-->>UI: Tạo đơn thành công
                UI-->>U: Mở MyOrder chờ duyệt
                E-->>N: Event tạo đơn
                N-->>U: Email và notification PENDING_APPROVAL
                Note over W,N: Xử lý bất đồng bộ có idempotency, retry và trạng thái lỗi quan sát được
            end
        end
    end
```

Email/notification không phải điều kiện để trả thành công của transaction tạo đơn. WorkflowStarter/EventPublisher cần đăng ký công việc bền vững hoặc cơ chế retry tương đương; sơ đồ không ngụ ý hai lời gọi sau commit tự bảo đảm không mất tác vụ.

Manager xử lý đơn ở WF02. Timer custom 24 giờ bắt đầu sau manager duyệt; không được đặt vào sequence tạo draft hoặc gửi đơn này.
