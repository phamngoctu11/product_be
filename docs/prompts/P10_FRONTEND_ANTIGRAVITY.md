# Lệnh giao việc cho Antigravity — triển khai frontend PHẦN 10

Từ PowerShell, copy toàn bộ lệnh giao việc này vào clipboard rồi dán vào cửa sổ chat Antigravity trong VS Code:

```powershell
Get-Content "D:\me\product_be\docs\prompts\P10_FRONTEND_ANTIGRAVITY.md" -Raw | Set-Clipboard
```

Bạn đang làm trong frontend Angular tại `D:\me\product_fe`. Chỉ sửa frontend; không sửa backend `D:\me\product_be`.

Trước khi code, đọc đầy đủ:

1. `D:\me\product_be\docs\implementation\10-production-checkpoint-kcs.md`
2. `D:\me\product_be\docs\use-cases\WF05-production-checkpoint\usecase.md`
3. `D:\me\product_be\docs\use-cases\WF05-production-checkpoint\sequence.md`
4. `D:\me\product_be\docs\target.txt`, riêng WF05.
5. Các service/model/trang P9 start-production, staff assigned orders, manager order detail, USER/guest order detail, upload ảnh và notification hiện có trong FE.

Mục tiêu: triển khai đầy đủ UI P10 theo từng `OrderItem`/loại sản phẩm, không theo quantity. Không tạo logic xuất kho/tồn kho, không customer review checkpoint, không tự PASS, không tự chuyển SHIPPING và không sửa deadline ở FE.

## API và typed model bắt buộc

- STAFF submit:
  - `POST /api/orders/staff/{orderId}/items/{orderItemId}/checkpoints`
  - header `Idempotency-Key`
  - body `{ orderVersion, stage, imageUrls, note }`
  - `stage: 'INITIAL_SHAPE' | 'FINAL_PRODUCT'`.
- MANAGER review:
  - `POST /api/orders/manager/{orderId}/checkpoints/{checkpointId}/review`
  - header `Idempotency-Key`
  - body `{ checkpointVersion, decision, reason }`
  - `decision: 'PASS' | 'REWORK_REQUIRED'`.
- Authenticated progress:
  - `GET /api/orders/{orderId}/checkpoints`.
- Guest progress:
  - `GET /api/guest/orders/{orderId}/checkpoints?token=<signed-read-token>`.
- Đọc đúng wrapper `ApiResponse<T>`.
- Tạo type chính xác cho `OrderItemProductionDTO`, `ProductionCheckpointDTO`, `ProductionCheckpointImageDTO`, `ProductionDecisionDTO`, `ReviewProductionCheckpointResultDTO` theo báo cáo backend.

Không gửi File/base64 vào endpoint checkpoint. Dùng cơ chế upload ảnh hiện có của FE, lấy URL HTTPS xong mới submit `imageUrls` (1–10 URL). Không hard-code CDN/object key.

## UI STAFF

- Chỉ hiện action P10 khi Order là `ORDER_CREATING` và staff đang phụ trách.
- Render một production card/stepper cho mỗi `OrderItem`; quantity 20 vẫn chỉ có một nhánh checkpoint cho item đó.
- Hiển thị tên product/variant, quantity, production duration, computed completion deadline và trạng thái item.
- INITIAL_SHAPE:
  - upload ảnh, note tùy chọn, submit một lần;
  - sau thành công hiển thị `PASSED`/đã gửi báo cáo; không hiển thị nút chờ manager duyệt;
  - mở ngay bước FINAL_PRODUCT.
- FINAL_PRODUCT:
  - hiển thị toàn bộ attempt cũ, ảnh, KCS decision và reason theo thời gian;
  - nếu chưa có attempt hoặc latest là `REWORK_REQUIRED`, cho nộp attempt mới;
  - nếu `PENDING_REVIEW`, khóa submit và hiển thị đang chờ KCS;
  - nếu `PASSED`, khóa toàn bộ action của item.
- Khi submit thành công, dùng response backend rồi reload progress/order để lấy `orderVersion` mới; không tự đoán version hoặc attempt.

## UI MANAGER

- Từ order detail/notification KCS, gọi progress API và hiển thị FINAL_PRODUCT `PENDING_REVIEW` theo từng item.
- Review modal bắt buộc hiển thị ảnh, note, attempt, submittedBy/submittedAt và checkpoint version.
- Hai action duy nhất: `PASS` và `REWORK_REQUIRED`.
- REWORK_REQUIRED bắt buộc reason phía UI, tối đa 1000 ký tự. PASS không bắt buộc reason.
- Sau review dùng `remainingOrderItemIds`, `itemProductionStatus`, `orderStatus`, `assignmentReleased` từ response:
  - còn item: Order vẫn `ORDER_CREATING`;
  - item cuối: hiển thị `READY_TO_SHIP`, staff đã được release;
  - tuyệt đối không gọi endpoint chuyển SHIPPING.

## UI USER và guest

- USER order detail dùng authenticated progress API; guest detail dùng token hiện có với guest progress API.
- Chỉ render dữ liệu INITIAL_SHAPE backend trả về: ảnh, note, submittedAt và deadline item.
- Không dựng UI review/approve/reject cho USER hoặc guest.
- Không cố lấy/hiển thị FINAL_PRODUCT, KCS decision hay reason nếu backend không trả.
- Notification “Đã có hình ảnh đầu tiên...” và READY_TO_SHIP phải điều hướng về đúng order detail/progress section.

## Idempotency, lỗi và đồng bộ

- Sinh một UUID cho mỗi logical submit/review; retry network/double click phải dùng lại cùng key.
- Disable action khi request đang chạy. Không tạo key mới chỉ vì timeout chưa rõ kết quả; reload progress trước.
- HTTP 409 version conflict/pending/duplicate: reload Order + progress, hiển thị thông báo nghiệp vụ, không tự tăng version.
- HTTP 403: quay về danh sách phù hợp và không giữ form action của staff/manager khác.
- Realtime notification chỉ là tín hiệu reload; dữ liệu REST là nguồn hiển thị.

## Loại bỏ frontend legacy

- Xóa mọi lời gọi/nút/form đến:
  - `/api/orders/staff/export/{orderId}`;
  - `/api/orders/manager/kcs-check/{orderId}`.
- Không dùng các khái niệm exported quantity, warehouse KCS hoặc customer final approval cho P10.
- Không map enum checkpoint cũ `SUBMITTED/APPROVED/REJECTED` và không dùng `INITIAL_WAITING_REVIEW/INITIAL_REWORK_REQUIRED`.

## Test và tiêu chí hoàn tất

- Unit/component test: quantity lớn vẫn một card; INITIAL không có review; FINAL pending/rework/pass; reason bắt buộc; stale 409 reload; permission theo role.
- Service test: đúng URL, body, wrapper, query token và `Idempotency-Key` reuse.
- UI test tối thiểu:
  - STAFF submit INITIAL rồi FINAL;
  - MANAGER rework, STAFF thấy reason và submit attempt tiếp;
  - PASS một trong nhiều item chưa READY_TO_SHIP;
  - PASS item cuối hiển thị READY_TO_SHIP;
  - USER và guest chỉ thấy INITIAL_SHAPE;
  - không còn request đến hai endpoint legacy.
- Chạy lint/test/build của frontend, sửa hết lỗi do thay đổi này.

Cuối cùng báo cáo: file đã sửa, route/component/service/model đã thêm hoặc dọn, test/build đã chạy và mọi giả định còn lại. Không báo hoàn tất nếu vẫn còn lời gọi API legacy hoặc UI cho phép review INITIAL_SHAPE.

