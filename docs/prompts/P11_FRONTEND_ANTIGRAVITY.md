# Lệnh giao việc Antigravity — hoàn thiện frontend PHẦN 11 ChangeRequest

Bạn đang làm việc trong repository frontend `D:\me\product_fe`. Hãy triển khai/rá soát hoàn chỉnh PHẦN 11 — WF06 ChangeRequest theo backend runtime hiện tại. Không sửa backend, không commit và không triển khai Manager KCS trong phần này.

## Nguồn phải đọc trước khi sửa

Đọc đầy đủ các file sau ở repository backend `D:\me\product_be`:

- `docs/target.txt`, phần ChangeRequest/F14 và AC19–AC20.
- `docs/use-cases/WF06-change-request/usecase.md`.
- `docs/use-cases/WF06-change-request/sequence.md`.
- `docs/implementation/11-change-request.md`.
- `src/main/java/com/example/workflow/controller/OrderController.java`.
- `src/main/java/com/example/workflow/controller/StaffOrderController.java`.
- `src/main/java/com/example/workflow/dto/ChangeRequestDTO.java`.
- `src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java`.
- `src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java`.
- `src/main/java/com/example/workflow/nume/ChangeRequestStatus.java`.
- `src/main/java/com/example/workflow/nume/ChangeRequestDecision.java`.
- `src/main/java/com/example/workflow/nume/OrderItemProductionStatus.java`.

Ưu tiên endpoint/controller runtime hơn file Excel hoặc tài liệu thiết kế cũ nếu có khác biệt.

Frontend đã có khung P11 trong các file sau. Phải audit và hoàn thiện tại chỗ, không tạo component/service/model trùng chức năng:

- `src/app/model/order.model.ts`.
- `src/app/service/order.service.ts` và test tương ứng.
- `src/app/features/shared/change-request/change-request-panel.component.*`.
- `src/app/features/shared/order-detail-popup/order-detail-popup.component.*`.
- `src/app/features/management/staff-orders/staff-orders.component.*`.

Giữ nguyên mọi thay đổi chưa commit không liên quan trong worktree.

## Ranh giới nghiệp vụ bắt buộc

- P11 chỉ là ChangeRequest giữa USER sở hữu Order và assigned STAFF đang giữ active assignment.
- Guest không được tạo/xem luồng này. MANAGER không duyệt ChangeRequest.
- Manager KCS, `FINAL_PRODUCT`, PASS/rework và `READY_TO_SHIP` thuộc P10; không đưa các action đó vào panel P11.
- ChangeRequest không cho sửa giá, quantity, voucher, payment, production duration hoặc deadline.
- Không đổi Order status, không reset production, không release staff và không tạo checkpoint.
- USER chỉ gửi đề nghị; spec chính thức chỉ đổi khi STAFF chọn `ACCEPT`.

## API runtime chính xác

Mọi endpoint đọc wrapper `ApiResponse<T>` theo helper hiện có.

### Đọc lịch sử/trạng thái

```http
GET /api/orders/{orderId}/change-requests
Authorization: Bearer <JWT USER hoặc STAFF>
```

Response data là `ChangeRequestDTO[]`, sắp mới nhất trước.

### USER tạo yêu cầu

```http
POST /api/orders/{orderId}/change-requests
Authorization: Bearer <USER JWT>
Idempotency-Key: <stable UUID của đúng operation>
Content-Type: application/json

{
  "orderVersion": 3,
  "orderItemId": 21,
  "baseSpecVersion": 1,
  "proposedDescription": "Nội dung đặc tả mới",
  "proposedImageUrls": ["https://..."],
  "reason": "Lý do yêu cầu"
}
```

`proposedDescription` bắt buộc, tối đa 4.000 ký tự. `reason` bắt buộc, tối đa 2.000 ký tự. `proposedImageUrls` tối đa 10 URL HTTP/HTTPS; upload ảnh bằng cơ chế hiện có rồi chỉ gửi URL.

### STAFF quyết định

```http
POST /api/orders/staff/{orderId}/change-requests/{changeRequestId}/decision
Authorization: Bearer <STAFF JWT>
Idempotency-Key: <stable UUID của đúng operation>
Content-Type: application/json

{
  "orderVersion": 3,
  "changeRequestVersion": 0,
  "decision": "ACCEPT",
  "reason": null
}
```

- `decision`: chỉ `ACCEPT | REJECT`.
- `REJECT` bắt buộc reason sau khi trim, tối đa 2.000 ký tự.
- `ACCEPT` có thể không có reason.
- Không gọi URL manager và không dùng `/api/manager/...` cho P11.

## Typed model phải khớp backend

`ChangeRequestDTO` gồm:

```text
id, version, orderId, orderVersion, orderItemId,
productName, variantName, ownerId, assignedStaffId,
baseAgreementVersion, baseSpecVersion,
baseSpecSnapshot, proposedSpecSnapshot, requestReason,
status, createdAt, decidedBy, decidedAt, decisionReason,
appliedVersion, appliedSpecSnapshot,
currentSpecVersion, currentSpecSnapshot, itemProductionStatus
```

Enum chính xác:

- status: `PENDING_REVIEW | APPLIED | REJECTED`.
- decision: `ACCEPT | REJECT`.
- Không dùng status legacy `PENDING` hoặc `ACCEPTED`.

## Quy tắc ánh xạ ID và version

- `{orderId}` chỉ lấy từ `Order.id` của order detail hiện hành.
- `{orderItemId}` chỉ lấy từ `OrderItemDTO.orderItemId`; không lấy `productId`, `variantId`, array index hoặc quantity. Nếu backend chưa trả `orderItemId`, khóa form và reload/report lỗi contract; không fallback sang `item.id` hay `0`.
- `orderVersion` chỉ lấy từ `OrderDTO.version` mới nhất của `GET /api/orders/{id}`. Không dùng `?? 0`, không hard-code `0`.
- `baseSpecVersion` chỉ lấy từ chính `OrderItemDTO.specVersion`. Không dùng `?? 1`, không tự suy đoán.
- `changeRequestVersion` lấy từ `ChangeRequestDTO.version`; không dùng order/spec version thay thế.
- Sau mutation thành công hoặc lỗi `409`, reload cả order detail và danh sách ChangeRequest trước khi mở lại action.

## UI USER

- Chỉ hiện panel/action khi role là USER, Order không phải Guest, status là `ORDER_ACCEPTED` hoặc `ORDER_CREATING`, và có item backend trả đủ `orderItemId`, `specVersion` với production status hợp lệ.
- Item có thể đề nghị ở các state backend hiện chấp nhận: `WAITING_ASSIGNMENT`, `ASSIGNED`, `INITIAL_IN_PROGRESS`, `FINAL_IN_PROGRESS`.
- Không cho chọn `NOT_REQUIRED`, `FINAL_WAITING_QC`, `FINAL_REWORK_REQUIRED`, `READY`.
- Form chỉ có item, proposed description, reference images và reason. Không dựng input giá, quantity, voucher, payment, duration/deadline.
- Hiển thị spec hiện hành và version để USER biết mình đang đề nghị trên base nào.
- Upload tối đa 10 ảnh; xử lý lỗi từng upload và không POST ChangeRequest nếu upload chưa hoàn tất.
- Sau thành công hiển thị `PENDING_REVIEW`, refresh dữ liệu cha và reset sạch draft/key/object URL.

## UI STAFF

- Tích hợp panel vào chi tiết đơn thuộc danh sách assigned orders hiện có; không tạo queue endpoint giả vì backend chưa có API queue ChangeRequest riêng.
- Chỉ render action cho request `PENDING_REVIEW`. Hiển thị item, base/current version, proposed description/images, request reason, production status và lịch sử quyết định.
- Trước `ACCEPT`, có confirm dialog nêu rõ spec mới sẽ áp dụng nhưng giá/deadline không đổi.
- `REJECT` bắt buộc reason đã trim. Không dùng browser `alert/confirm` nếu dự án đã có dialog dùng chung.
- Khi một request đang submit, disable trung thực toàn bộ action cạnh tranh trong panel.
- Dùng response backend để render `APPLIED`/`REJECTED`, `appliedVersion` và `decisionReason`; không tự cộng version phía client.

## Idempotency, upload và xử lý lỗi

- Mỗi operation create/decision có một idempotency key riêng.
- Retry do network/timeout/5xx phải giữ nguyên key và chính xác cùng payload cuối cùng, bao gồm URL ảnh đã upload; không upload lại để tạo URL khác nhưng tái sử dụng key cũ.
- Nếu người dùng thay đổi payload sau lỗi transient, coi đó là operation mới và sinh key mới; không gửi payload mới với key cũ.
- Thành công: xóa key/payload retry đã lưu.
- `409`: xóa intent/key stale, không auto-resend; reload order detail + ChangeRequest rồi yêu cầu người dùng đánh giá lại.
- `400`: hiển thị validation từ backend và không tự bỏ field để gửi lại.
- `403`: khóa/ẩn action và reload/đóng dữ liệu không còn quyền.
- `404`: thông báo dữ liệu không còn tồn tại và reload/đóng panel.
- Khi đổi Order hoặc đóng panel, reset toàn bộ draft, decision reason, request đang submit, idempotency map, uploaded payload và revoke mọi `URL.createObjectURL` đã tạo.

## Các lỗi hiện hữu phải audit và loại bỏ

- Không để lại `order.version ?? 0`.
- Không để lại `item.specVersion ?? 1`.
- Không fallback `orderItemId` sang `item.id` hoặc `0`.
- Không giữ idempotency key/decision reason của Order trước khi mở Order mới.
- Không để object URL preview bị leak.
- Không tạo component/service/model P11 thứ hai khi file dùng chung đã tồn tại.
- Không thêm Manager KCS, shipping, payment bổ sung, timer hoặc auto decision vào P11.

## Test bắt buộc

- Service test exact URL, method, `Idempotency-Key`, body và unwrap `ApiResponse<T>` cho cả ba API.
- USER/STAFF/Guest/Manager visibility đúng role và ownership context.
- Eligible/ineligible production statuses đúng backend.
- Thiếu `orderVersion`, `orderItemId` hoặc `specVersion` phải khóa submit, không gửi `0/1` mặc định.
- Create upload thành công/thất bại, giới hạn 10 ảnh và payload URL chính xác.
- ACCEPT có confirm và gửi đúng ba version/id: order, request, item base.
- REJECT thiếu reason bị chặn; reason được trim và giới hạn 2.000 ký tự.
- Replay transient giữ nguyên key + payload; thay payload phải dùng key mới.
- `409` reload order detail + request list và không tự retry.
- Success cập nhật lịch sử, applied version và phát refresh cho component cha.
- Đổi order/đóng panel reset toàn bộ local state và revoke preview URL.
- Không có lời gọi Manager KCS/legacy ChangeRequest endpoint.

## Lệnh kiểm tra và báo cáo

Chạy ít nhất:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build:prod
git diff --check
git status --short
```

Nếu môi trường không có ChromeHeadless, ghi rõ blocker và vẫn chạy build cùng test khả dụng. Kết thúc bằng báo cáo:

- file đã sửa;
- API/model/UI/idempotency đã hoàn thiện;
- test/build và số test pass/fail;
- warning còn lại;
- phần chưa làm hoặc blocker thật sự.

Không commit.
