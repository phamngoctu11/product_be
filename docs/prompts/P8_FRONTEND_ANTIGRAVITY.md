# Lệnh giao việc cho Antigravity — triển khai frontend PHẦN 08

Từ PowerShell, copy toàn bộ lệnh giao việc này vào clipboard rồi dán vào cửa sổ chat Antigravity trong VS Code:

```powershell
Get-Content "D:\me\product_be\docs\prompts\P8_FRONTEND_ANTIGRAVITY.md" -Raw | Set-Clipboard
```

Bạn đang làm trong frontend Angular tại `D:\me\product_fe`. Chỉ sửa frontend; không sửa backend `D:\me\product_be`.

Trước khi code, đọc đầy đủ:

1. `D:\me\product_be\docs\implementation\08-chat-agreement-timeout.md`
2. `D:\me\product_be\docs\use-cases\WF03-agree-order\usecase.md`
3. Các file FE hiện có: `src/app/model/order.model.ts`, `src/app/service/order.service.ts`, `src/app/service/websocket.service.ts`, trang order USER, trang staff orders và shared order detail popup.

Mục tiêu: triển khai UI P8 hoàn chỉnh cho USER và assigned STAFF theo đúng API backend đã bàn giao. Không đưa MANAGER/ADMIN vào order chat, không tạo UI cho Guest, không triển khai payment hoặc bắt đầu sản xuất. Scope mới đã loại bỏ hoàn toàn chat tư vấn trước bán, attribution và hoa hồng; frontend chỉ được giữ chat theo Order.

## Việc phải làm

### Dọn scope legacy bắt buộc

- Tìm và xóa route/menu/component/service/model/store còn dùng consultation, consultation attribution, consultation commission hoặc staff commission.
- Xóa mọi call tới `/api/consultations`, `/api/consultation-attributions`, `/api/consultation-commissions`, `/api/chat` và mọi subscription `/topic/chat/user/**`, `/topic/chat/staff/**`.
- Notification model không còn `consultationRequestId`; giữ notification theo `orderId` và notification chung hiện tại.
- Không xóa order-chat, product review, wishlist hoặc thống kê quản lý vẫn được nêu trong `target.txt`.
- Trước khi báo hoàn tất, tìm lại toàn bộ `src` bằng `rg -ni "consultation|commission|/api/chat|/topic/chat" src`; chỉ chấp nhận kết quả thuộc comment/migration tương thích thật sự cần giữ và phải giải thích rõ.

### Model và API service typed

- Thêm model typed cho `OrderAgreement`, submit/decision request, `OrderChatThread`, `OrderChatMessage` theo contract trong báo cáo backend.
- Bổ sung vào `Order`: `confirmationDueAt`, `userSubmittedAt`, `currentAgreementVersion`; sửa `staffId`/`managerId` sang `string | null` vì ID backend là UUID string.
- Thêm REST method đúng URL thực tế nêu trong báo cáo.
- Mọi submit/decision phải gửi `Idempotency-Key`. Một logical action/retry phải tái sử dụng cùng key; chỉ tạo key mới khi user chủ động tạo action mới.
- Với HTTP 409 stale version, hiển thị thông báo rõ ràng và reload Order + agreement trước khi cho thao tác lại.

### WebSocket order chat

- CONNECT `/ws` bằng header `Authorization: Bearer <JWT>` như cơ chế hiện có.
- Subscribe động `/topic/order-chat/{orderId}`; unsubscribe khi đóng/chuyển order và chống subscribe trùng sau reconnect.
- Có thể gửi bằng REST hoặc SEND `/app/order-chat.send` body `{orderId, content}`; không gửi/trust `senderId`, `userId`, `senderRole`.
- Mở rộng `WebsocketService` mà không làm mất subscription notification/admin hiện tại. Reconnect phải khôi phục cả notification và order chat đang mở.
- Dedupe message theo `id` khi response REST và frame WebSocket cùng đến.

### Shared collaboration panel

- Tạo component standalone tái sử dụng cho order detail của USER và STAFF, nhận `order` + actor mode.
- Panel chỉ xuất hiện với Order có USER và trong phạm vi P8 (`DISCUSSING`, `WAITING_STAFF_CONFIRMATION`, có thể readonly sau `ORDER_ACCEPTED`). Guest không gọi chat/agreement API.
- Hiển thị hai vùng responsive: agreement và chat. Reuse Bootstrap/design system hiện có.
- Chat có loading/empty/error, lịch sử theo thời gian, tên/role/thời điểm, input giới hạn 4000 ký tự, disable khi đang gửi.
- Nếu thread chưa sẵn sàng ngay sau assignment (404), hiển thị “Đang chuẩn bị hội thoại” và retry có backoff/giới hạn; không tự tạo thread từ FE.

### Luồng USER

- Ở `DISCUSSING`, USER mở “Thông tin & chốt đơn”.
- CUSTOM: bắt buộc description, quantity nguyên dương, unitPrice dương, productionDurationDays dương; tối đa 10 image URL; note và voucher tùy chọn. Reuse upload ảnh hiện có để lấy URL nếu phù hợp.
- CATALOG: hiển thị snapshot sản phẩm/quantity/unitPrice/duration chỉ đọc; request submit không gửi các field override đó.
- Hiển thị preview subtotal/discount/finalPrice backend trả; không coi phép tính client là giá chính thức.
- Countdown dùng `confirmationDueAt` tuyệt đối, chỉ là UI. Dừng ngay khi first valid submit thành công, không chờ staff confirm và không chạy lại sau REQUEST_CHANGES.
- Sau submit cập nhật `orderVersion`, status, agreement version và khóa nút chống double submit.
- Khi staff REQUEST_CHANGES, hiển thị reason, prefill snapshot cũ để user gửi version mới.

### Luồng assigned STAFF

- Trong detail “Đơn của tôi”, hiển thị cùng chat/agreement panel.
- Khi `WAITING_STAFF_CONFIRMATION` và agreement hiện hành `USER_SUBMITTED`, cho `CONFIRM` hoặc `REQUEST_CHANGES`; reason bắt buộc cho request changes, tối đa 1000 ký tự.
- Gửi cả `orderVersion` và `agreementVersion` mới nhất. Staff không sửa snapshot USER trên màn review.
- Confirm thành công cập nhật `ORDER_ACCEPTED`; không thêm payment/start production ở P8.

### Tích hợp và quyền

- USER: tích hợp vào shared order detail popup hoặc modal/section mở từ `features/storefront/orders`.
- STAFF: tích hợp vào detail modal `features/management/staff-orders`.
- Không sửa màn Manager để xem chat. Không hiển thị chat cho claimable order trước khi claim.
- Map đúng `DISCUSSING`, `WAITING_STAFF_CONFIRMATION`, `ORDER_ACCEPTED`, `CANCELLED`.

## Kiểm thử bắt buộc

- USER/assigned STAFF chat realtime; reconnect không duplicate.
- Manager/staff khác không có entry point; backend 403 vẫn được xử lý an toàn.
- CUSTOM submit hợp lệ; double click chỉ một logical action.
- Form lỗi không dừng countdown; REQUEST_CHANGES không chạy lại countdown.
- CATALOG không gửi field giá/duration editable.
- 409 stale reload dữ liệu; Guest không gọi chat/agreement; thread 404 tạm thời retry hợp lý.

## Hoàn tất

- Chạy `npm.cmd run build` tại `D:\me\product_fe` và test liên quan nếu có.
- Báo cáo file đã sửa, hành vi, command/kết quả build/test và điểm còn thiếu.
- Không tuyên bố hoàn tất nếu còn lỗi TypeScript/build hoặc phá notification WebSocket cũ.
