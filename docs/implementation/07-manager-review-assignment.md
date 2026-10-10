# Bàn giao PHẦN 07 — Manager review và assignment (WF02)

Ngày cập nhật: 09/10/2026.

Trạng thái sau rà soát domain: `IN_PROGRESS`. Phần API/service/transaction bên dưới đã được triển khai, nhưng Guest BPMN còn gateway legacy `hasHandmadeItems` cho phép đi theo nhánh không cần assignment/sản xuất. Nhánh này trái target vì shop không bán hàng làm sẵn; phải sửa trước khi coi PHẦN 07 hoàn tất.

## Kết quả

PHẦN 07 đã thay đường manager review/assign và staff claim legacy bằng hai application service:

- `ManagerOrderReviewService`: danh sách/chi tiết chờ duyệt, approve/reject, metadata quyết định và hạn xác nhận CUSTOM.
- `OrderAssignmentService`: một kernel gán dùng chung cho manager assign, manager approve kèm staff và staff self-claim.

API chi tiết Order hiện trả thêm `description` và `imageUrls` đã parse trên từng `OrderItemDTO` CUSTOM. FE không phải tự parse `specSnapshot`; trường snapshot thô vẫn được giữ tạm để tương thích client cũ. Mapper đọc cả snapshot mới `{description,imageUrls}` và snapshot legacy `{spec,attachments}`.

Actor không còn lấy từ `changerId` do client gửi. Mọi thao tác lấy MANAGER/STAFF từ JWT qua `CurrentUserService`, bắt buộc `orderVersion` và `Idempotency-Key`.

MANAGER đã bị khóa (`isActive=false`) hoặc soft-delete bị từ chối ở cả review, danh sách staff khả dụng và assignment, kể cả khi JWT cũ vẫn còn hiệu lực.

Các endpoint cũ `/admin/pending`, `/manager/review-order/{orderId}` và `/manager/assign-staff/{orderId}` đã bị loại khỏi controller; ba hàm ghi legacy tương ứng cũng đã được xóa khỏi `OrderService`.

## API

### Manager review

```http
GET  /api/orders/manager/reviews
GET  /api/orders/manager/reviews/{orderId}
POST /api/orders/manager/reviews/{orderId}
Idempotency-Key: <key>

{
  "orderVersion": 0,
  "approved": true,
  "reason": null,
  "staffId": "staff-id-or-null"
}
```

Reject bắt buộc `reason`, không cho kèm `staffId`. Approve có thể để trống staff để Order chuyển `PENDING_ASSIGNMENT`, hoặc gán ngay trong cùng transaction.

### Assignment

```http
GET  /api/orders/manager/available-staff
POST /api/orders/manager/assignments/{orderId}
Idempotency-Key: <key>

{
  "orderVersion": 1,
  "staffId": "staff-id"
}
```

```http
GET  /api/orders/staff/claimable
POST /api/orders/staff/claim/{orderId}
Idempotency-Key: <key>

{
  "orderVersion": 1
}
```

Danh sách review và claimable có phân trang. Danh sách `PENDING_APPROVAL` và `PENDING_ASSIGNMENT` được cache theo trang; approve/assign/claim xóa cache liên quan sau commit.

## Quy tắc trạng thái

| Thao tác | Trạng thái trước | Kết quả |
|---|---|---|
| Manager approve, chưa chọn staff | `PENDING_APPROVAL` | `PENDING_ASSIGNMENT` |
| Manager approve kèm staff | `PENDING_APPROVAL` | Ghi assignment rồi áp quy tắc theo loại khách |
| Manager assign sau approve | `PENDING_ASSIGNMENT` | Ghi assignment rồi áp quy tắc theo loại khách |
| Staff self-claim | `PENDING_ASSIGNMENT` | Ghi assignment rồi áp quy tắc theo loại khách |
| Manager reject | `PENDING_APPROVAL` | Gọi PHẦN 06, kết quả `CANCELLED/MANAGER_REJECTED` |

Sau assignment:

- Guest CATALOG chuyển thẳng `ORDER_ACCEPTED`, không tạo chat hoặc chờ guest xác nhận design.
- Order có USER chuyển `DISCUSSING`; WF03 quyết định việc tạo chat và thỏa thuận theo loại Order.
- CUSTOM được manager approve có `confirmationDueAt = managerApprovedAt + 24 giờ`. Retry trả kết quả đã commit, không kéo dài deadline.
- Guest checkout sau khi outbox `GUEST_ORDER_CREATED` được consume sẽ khởi chạy `GuestPurchaseFlow`; retry tra business key `guest-order-{orderId}` để không tạo process trùng.
- Manager review hoàn thành Camunda task `manager_approve_order` nếu Order đã có process. Assignment hoàn thành task Guest hiện hành hoặc correlate message `ORDER_ASSIGNED` của BPMN mục tiêu.

Quy tắc bắt buộc khi hiệu chỉnh BPMN: CATALOG chỉ là nguồn thiết kế do shop cung cấp, không phải hàng có sẵn. Guest CATALOG được manager duyệt phải luôn chờ một staff nhận Order rồi mới đạt `ORDER_ACCEPTED`; không dùng `hasHandmadeItems`, `OrderItem.handmade` hoặc `productionStatus=NOT_REQUIRED` để bỏ qua bước này.

Không tạo payment URL trong PHẦN 07.

## Điều kiện staff và chống cạnh tranh

Trong model hiện tại, `AVAILABLE` được suy ra từ ba điều kiện:

- User có role `STAFF`, chưa bị xóa và `isActive=true`.
- Không tồn tại `OrderAssignment.activeStaffId` cho staff.
- Order chưa có `OrderAssignment.activeOrderId` và chưa có assigned staff.
- Order cũng không được còn `warehouseStaff` legacy; endpoint mới từ chối thay vì ghi đè người đã phụ trách trong dữ liệu cũ.

Transaction luôn khóa Order trước, sau đó khóa hàng User của staff. Nó kiểm tra lại active assignment dưới khóa rồi mới chuyển trạng thái và `saveAndFlush` assignment. Hai unique constraint `active_order_id` và `active_staff_id` là lớp bảo vệ cuối ở database.

Hệ quả:

- Hai manager/staff cạnh tranh một Order: version/Order lock chọn một kết quả.
- Hai Order cạnh tranh cùng staff: staff row lock và unique active staff chỉ cho một assignment.
- Review cạnh tranh với customer cancellation: cả hai dùng cùng Order lock/version; không thể cùng ghi hai trạng thái cuối.
- Cùng idempotency key/cùng payload trả kết quả đã lưu; cùng key/khác payload trả conflict.

## Dữ liệu và migration

Liquibase `05-manager-review.xml` bổ sung:

- `orders.manager_review_decision`: `APPROVED` hoặc `REJECTED`.
- `orders.manager_rejected_at`.
- index review queue theo `status,id`, tương thích cả schema legacy chưa có tên cột thời gian chuẩn hóa.

Các trường `managerApprovedAt`, `confirmationDueAt`, `assignedStaffId` và bảng `order_assignments` đã có từ migration expand trước đó.

`assignedStaff` là quan hệ mục tiêu. `warehouseStaff` hiện được mirror tạm khi gán để các endpoint production legacy chưa được thay ở PHẦN 10 không bị gãy; không có assignment legacy thứ hai được tạo.

## Event, email và notification

- Gán thành công tạo notification cho đúng staff thông qua outbox của `NotificationService`.
- Mọi assignment phát parent event `ORDER_ASSIGNED`, chứa Order, staff, nguồn assign, trạng thái mới và cờ cần trao đổi. Consumer dùng event này để bàn giao workflow; PHẦN 08 có thể bổ sung tạo ChatThread mà không dò trạng thái DB.
- Guest CATALOG đạt `ORDER_ACCEPTED` phát một parent event `ORDER_ACCEPTED`.
- Handler của event gửi email trạng thái cho chủ đơn; USER nhận thêm notification hệ thống, Guest không có notification tài khoản. Email Guest CATALOG gồm mẫu snapshot, giá, tổng số lượng, thời lượng và signed READ-only link hết hạn sau 30 ngày.
- Không gửi email trung gian ở `PENDING_ASSIGNMENT` hoặc `DISCUSSING`.
- Manager reject tái sử dụng `ORDER_CANCELLED` của PHẦN 06 nên không nhân đôi email, voucher restore hoặc assignment release.

Signed milestone token dùng HMAC, bị ràng buộc với `orderId`, chỉ có scope READ và không xoay/thu hồi token checkout cũ. Production bắt buộc cấu hình `ORDER_LINK_SIGNING_SECRET` tối thiểu 32 ký tự; không dùng fallback development khi chạy profile `prod`.

## Kiểm thử

Đã kiểm tra:

- approve CUSTOM không staff và deadline đúng 24 giờ;
- approve kèm staff ghi decision, assignment và transition trong cùng transaction;
- manager reject gọi đúng kernel cancellation với actor từ JWT;
- USER order sang `DISCUSSING`, Guest CATALOG sang `ORDER_ACCEPTED`;
- staff bận không nhận Order thứ hai;
- version cũ thất bại trước khi khóa/gán staff;
- snapshot CUSTOM mới/legacy đều trả `description` và `imageUrls` có kiểu rõ ràng cho màn hình manager;
- `warehouseStaff` legacy không thể bị endpoint assign mới ghi đè;
- retry cùng key chỉ có một assignment, một transition audit và một notification;
- hai manager chạy đồng thời trên một Order chỉ tạo một assignment/audit;
- hai Order chạy đồng thời tranh cùng staff chỉ một Order chiếm được staff;
- bridge Camunda thật hoàn thành chuỗi Guest manager review → assignment và không tạo process trùng khi retry;
- manager inactive/soft-delete không thể review hoặc xem danh sách staff khả dụng;
- contract controller giữ `MANAGER` authority và `Idempotency-Key` bắt buộc trên hai write API;
- migration chạy lặp trên schema legacy;
- `ORDER_ACCEPTED` được consumer định tuyến đúng và không tạo notification hệ thống cho Guest.

```text
.\mvnw.cmd test
269 test chạy, 0 failure, 0 error, 7 MySQL opt-in test skipped
BUILD SUCCESS
```

## Ranh giới chưa thuộc PHẦN 07

- Chưa tạo ChatThread và chưa xử lý agreement/form/timer PT24H; `ORDER_ASSIGNED` đã là contract bàn giao cho PHẦN 08.
- Chưa tạo payment hoặc timer PT1H; thuộc PHẦN 09.
- Chưa thay production/KCS legacy dùng `warehouseStaff`; thuộc PHẦN 10.
- Guest flow đã start/complete manager task và assignment task thật. USER/CUSTOM chưa có process lifecycle mục tiêu; full cutover và thiết kế lại `approve-cart.bpmn` vẫn thuộc PHẦN 13. Với Order chưa có process, database/outbox tiếp tục là nguồn nghiệp vụ chính.
- MySQL opt-in chưa chạy trong checkpoint này; H2 và migration legacy đã đạt.

## Gap phải đóng trước khi bàn giao

- Bỏ tham số/process variable `hasHandmadeItems` khỏi đường start Guest workflow.
- Bỏ gateway “contains handmade items” và nhánh kết thúc dành cho hàng làm sẵn; sau manager approval, Guest CATALOG luôn đến assignment task.
- Không đổi tên helper thành `hasCustomItem`: Guest không tạo CUSTOM và `orderType` đã là nguồn phân biệt CATALOG/CUSTOM.
- Nếu đổi task key, bridge phải tiếp tục hoàn tất được task key cũ cho process instance đang chạy trong giai đoạn chuyển tiếp.
- Bổ sung BPMN/service test với catalog item có giá trị `handmade=false` legacy nhưng vẫn bắt buộc chờ assignment, chứng minh field legacy không còn điều khiển nghiệp vụ mới.

Hành động tiếp theo: review PHẦN 07, sau đó triển khai PHẦN 08 — chat, agreement và timer CUSTOM PT24H.
