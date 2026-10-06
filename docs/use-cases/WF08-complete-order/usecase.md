# WF08 — Tra cứu và xác nhận nhận hàng (`complete_order`)

Ngày lập: 06/10/2026. Đặc tả hệ thống mục tiêu, không phải xác nhận hành vi runtime hiện tại.

Nguồn: [target.txt](../../target.txt), mục B2–B3, C2–C3, D1–D3, E/WF08, F15, F18, G, I–K, M và O. Đầu vào bàn giao đến từ [WF07](../WF07-ship-order/README.md).

## 1. Phạm vi và điểm kết thúc

WF08 gồm hai trách nhiệm:

1. Cho USER sở hữu Order hoặc Guest có token bảo mật mở snapshot đơn được phép xem.
2. Nhận đầy đủ số lượng thực nhận theo từng `OrderItem`, đối chiếu với số bàn giao và quyết định có hoàn tất Order hay chưa.

Luồng xác nhận chỉ bắt đầu khi Order đang `SHIPPING`. Kết quả có thể là:

- Số nhận khớp số bàn giao: lưu nhận hàng và chuyển `SHIPPING → DELIVERED`.
- Có chênh lệch và `acceptMismatch=false`: trả chi tiết chênh lệch, Order vẫn `SHIPPING`, chưa cộng reputation.
- Có chênh lệch và `acceptMismatch=true`: lưu số thực nhận cùng dấu vết chấp nhận, sau đó chuyển `DELIVERED`.

WF08 kết thúc tại `DELIVERED`; không mở hồ sơ complaint, không tự hoàn tiền/bồi thường và không tự hoàn tất bằng timer nếu khách im lặng.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân chính | Kết quả |
|---|---|---|---|
| UC08.1 | Tra cứu thông tin Order để nhận hàng | USER sở hữu Order hoặc Guest có token | Snapshot Order đúng phạm vi quyền và trạng thái có thể xác nhận |
| UC08.2 | Xác nhận số lượng thực nhận | USER sở hữu Order hoặc Guest có token | `DELIVERED` hoặc kết quả chênh lệch chưa hoàn tất |

STAFF và MANAGER không xác nhận thay khách trong WF08. Backend thực hiện xác thực, validation, đối chiếu, cập nhật transaction và xử lý sự kiện; backend không phải actor bên ngoài trong Use Case Diagram.

## 3. Quy tắc chung

### 3.1 Xác thực USER và Guest

- USER được xác định từ JWT và phải có `userId` đúng bằng owner của Order. Không nhận userId từ request làm căn cứ cấp quyền.
- Guest phải gửi `orderId` cùng token ngẫu nhiên/bảo mật đã cấp qua link email. Chỉ có orderId, email, phone hoặc guestSessionId không đủ quyền đọc hay xác nhận.
- Backend chỉ lưu hash của guest token, kiểm tra token gắn đúng Order, đúng scope, chưa hết hạn/chưa thu hồi và áp dụng rate limit độc lập.
- Token của Order này không được dùng cho Order khác. API danh sách không trả token hoặc hash token.
- Có thể đổi link token thành guest access session ngắn hạn, nhưng refresh session, TTL và quy tắc cấp lại link chưa được chốt. Dù chọn phương án nào vẫn phải giữ ràng buộc Order và scope.
- Không ghi token/secret vào application log, audit payload, event hoặc URL do backend phát lại cho nơi không cần thiết.

### 3.2 Quyền đọc và quyền xác nhận

- Link được gửi từ mốc `READY_TO_SHIP` có thể mở trang Order và xem snapshot cho phép.
- Nút/form xác nhận nhận hàng chỉ được bật khi Order đang `SHIPPING`.
- `READY_TO_SHIP` chưa được xác nhận nhận hàng vì staff chưa ghi bàn giao.
- `DELIVERED` chỉ hiển thị kết quả đã hoàn tất; request lặp không được ghi lại số lượng hoặc cộng điểm lần hai.
- `CANCELLED` và các trạng thái trước giao không cho xác nhận.
- Snapshot trả cho khách gồm thông tin Order, contact của chính đơn, dòng hàng/spec được phép xem, số đặt, số bàn giao khi đã có, trạng thái và thời gian liên quan. Không trả ghi chú nội bộ, token hash, payment secret, dữ liệu quản trị hoặc KCS ngoài phạm vi khách hàng.

### 3.3 Dữ liệu số lượng và đối chiếu

- Request dùng `orderItemId`, không dùng product/variant id làm khóa cập nhật.
- Phải gửi đủ mọi OrderItem đúng một lần, không trùng, không có dòng ngoài Order.
- `receivedQuantity` là số nguyên không âm. Không giới hạn bằng `exportedQuantity` vì giá trị cao/thấp hơn chính là dữ liệu cần đối chiếu.
- Với đơn mới, mọi OrderItem phải có `exportedQuantity` do WF07 ghi. Nếu thiếu thì backend trả lỗi dữ liệu/trạng thái và không tự hoàn tất.
- Với dữ liệu migrated cũ thiếu `exportedQuantity`, backend có thể dùng ordered quantity làm fallback để hiển thị/đối chiếu theo policy migration, đồng thời phải cho biết nguồn fallback trong audit hoặc dữ liệu nội bộ.
- `matched=true` khi mọi `receivedQuantity` bằng effective exported quantity tương ứng.
- `mismatches` ghi tối thiểu `orderItemId`, thông tin dòng/biến thể hiển thị, `orderedQuantity`, `exportedQuantity` hiệu lực và `receivedQuantity`.
- `exportedQuantity`/`receivedQuantity` không phải tồn kho và WF08 không reserve, deduct hoặc restore stock.

### 3.4 Quy tắc hoàn tất và chấp nhận chênh lệch

- Nếu `matched=true`, giá trị `acceptMismatch` không cần thiết để hoàn tất.
- Nếu `matched=false` và `acceptMismatch=false`, trả `completed=false`, danh sách chênh lệch và message; không ghi `receivedQuantity` chính thức, không chuyển trạng thái, không cộng reputation.
- Sau khi xem kết quả chênh lệch, nếu khách đổi quyết định sang chấp nhận thì gửi request mới với cùng danh sách số lượng hiện hành, `acceptMismatch=true`, version hiện hành và **idempotency key mới**. Không tái sử dụng key cũ cho payload đã thay đổi.
- Nếu `matched=false` và `acceptMismatch=true`, lưu `receivedQuantity`, `acceptedMismatch=true`, actor/time và note nếu có; sau đó hoàn tất Order.
- Việc chấp nhận chênh lệch là quyết định của chủ đơn đã xác thực, không phải cách hệ thống tự kết luận đã giải quyết khiếu nại.
- Không có endpoint complaint, ticket bồi thường, auto-refund hoặc workflow tranh chấp trong phạm vi hiện tại.
- Không có timer tự xác nhận. Timer custom 24 giờ ở WF03 không liên quan nhận hàng.

### 3.5 Hậu quả khi chuyển DELIVERED

Trong một kết quả nghiệp vụ duy nhất, hệ thống phải:

- Lưu `receivedQuantity` của toàn bộ OrderItem.
- Lưu `matched`, `acceptedMismatch`, note, actor và thời điểm xác nhận.
- Chuyển Order `SHIPPING → DELIVERED`, ghi `endOrderTime` và history.
- Với USER: cộng 2 reputation đúng một lần. Nên dùng reputation ledger có unique key theo `orderId + reason` hoặc ràng buộc tương đương.
- Với Guest: không tạo User, không có reputation và không cộng điểm.
- Phát `ORDER_DELIVERED` sau commit hoặc lưu outbox trong cùng transaction.
- Với Order CATALOG: cấp quyền/mời người mua đánh giá catalog. Guest chứng minh quyền đánh giá qua link/token; CUSTOM chưa có đánh giá.
- Không release staff lần nữa vì assignment đã đóng ở checkpoint 2 hoặc lúc hủy.

Nếu reputation nằm cùng database, cập nhật Order và ledger trong cùng transaction. Nếu là consumer/service tách rời, phải có outbox và khóa idempotency để retry không cộng thêm 2 điểm.

### 3.6 Email và notification

- `DELIVERED` không thuộc bốn mốc email trạng thái. Không gửi email/notification trạng thái thứ năm chỉ vì Order hoàn tất.
- `ORDER_DELIVERED` vẫn được dùng cho audit, workflow, reputation, quyền đánh giá và read model.
- Lời mời đánh giá CATALOG là thông điệp chức năng riêng, không được diễn đạt hoặc đếm như email trạng thái `DELIVERED`.
- Guest không có notification tài khoản. Nếu gửi lời mời đánh giá cho Guest, dùng email/link bảo mật; USER có thể nhận lời mời qua notification/kênh đánh giá đã triển khai.
- CUSTOM không tạo lời mời đánh giá vì chưa phát triển đánh giá custom.

### 3.7 Camunda

WF08 là nghiệp vụ phụ sau luồng điều phối chính WF02–WF05:

- UC08.1 chỉ là chức năng đọc, không phải Camunda user task.
- UC08.2 được backend xác thực và commit trước. Sau đó event/message `ORDER_DELIVERED` có thể hoàn tất wait state nhận hàng nếu process vòng đời được kéo dài qua WF08.
- Không dùng Camunda timer để tự xác nhận nhận hàng và không tạo process vòng đời thứ hai cho cùng Order.
- Database quyết định owner/token, trạng thái, số lượng, reputation và idempotency. Process variables chỉ giữ khóa tham chiếu tối thiểu.
- Lỗi correlate sau commit được retry/reconcile; không rollback Order `DELIVERED` và không yêu cầu khách xác nhận lại.

## 4. UC08.1 — Tra cứu thông tin Order để nhận hàng

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Mở snapshot Order an toàn và biết có thể xác nhận nhận hàng hay chưa |
| Kích hoạt | USER mở MyOrder hoặc Guest mở link Order trong email |
| Tiền điều kiện USER | JWT hợp lệ và userId là owner của Order |
| Tiền điều kiện Guest | orderId + token đúng Order/scope, chưa hết hạn hoặc thu hồi |
| Đầu vào | orderId; JWT của USER hoặc guest token/session có scope đọc |
| Thành công | Snapshot được lọc theo quyền, gồm trạng thái và danh sách item cần cho bước xác nhận |
| Bảo đảm thất bại | Không trả snapshot, token hoặc thông tin cho người không có quyền |

### Luồng chính

| Bước | USER/Guest | Hệ thống |
|---|---|---|
| 1 | Mở Order từ MyOrder hoặc link email | Phân loại cơ chế xác thực USER/Guest, không suy quyền từ orderId |
| 2 | Gửi request đọc Order | USER: lấy userId từ JWT; Guest: kiểm tra token hash, Order, scope, expiry/revocation và rate limit |
| 3 | — | Đọc Order, OrderItem và dữ liệu bàn giao từ database |
| 4 | — | Lọc bỏ dữ liệu nội bộ và tạo snapshot theo quyền |
| 5 | Xem thông tin đơn | Nhận trạng thái, dòng hàng, số đặt, số bàn giao nếu có và thời gian liên quan |
| 6 | Xem khả năng xác nhận | Chỉ trả `canConfirmReceipt=true` khi Order `SHIPPING` và dữ liệu bàn giao đủ điều kiện |

### Ngoại lệ

| Mã | Điều kiện | Kết quả |
|---|---|---|
| A01 | USER chưa đăng nhập hoặc JWT không hợp lệ | 401 |
| A02 | USER không sở hữu Order | 403/404 theo policy chống dò dữ liệu |
| A03 | Guest chỉ gửi orderId, email, phone hoặc guestSessionId | Từ chối, không trả dữ liệu |
| A04 | Token sai Order, sai scope, hết hạn hoặc thu hồi | 401/403, không lộ snapshot |
| A05 | Vượt rate limit tra cứu | Từ chối tạm thời theo cấu hình, không thay quyền |
| A06 | Order READY_TO_SHIP | Cho xem theo quyền nhưng `canConfirmReceipt=false` |
| A07 | Order SHIPPING | Cho xem và bật form xác nhận |
| A08 | Order DELIVERED | Hiển thị kết quả đã lưu, không mở form sửa lại |
| A09 | OrderItem đơn mới thiếu exportedQuantity | Hiển thị trạng thái cần xử lý dữ liệu, không cho xác nhận |

## 5. UC08.2 — Xác nhận số lượng thực nhận

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Ghi nhận số thực nhận và hoàn tất Order khi điều kiện phù hợp |
| Kích hoạt | USER/Guest bấm xác nhận trên Order đang `SHIPPING` |
| Tiền điều kiện | Actor đã xác thực đúng owner/token; Order `SHIPPING`; có dữ liệu bàn giao hiệu lực |
| Đầu vào | orderId, receivedItems, acceptMismatch, note tùy chọn, order version, idempotency key |
| Thành công khớp | `matched=true`, `completed=true`, Order `DELIVERED` |
| Thành công chấp nhận lệch | `matched=false`, `completed=true`, Order `DELIVERED` và có audit chấp nhận |
| Kết quả chưa hoàn tất | `matched=false`, `completed=false`, Order vẫn `SHIPPING`, trả mismatches |
| Bảo đảm thất bại | Không lưu một phần, không cộng reputation và không hoàn tất workflow |

### Luồng chính

| Bước | USER/Guest | Hệ thống |
|---|---|---|
| 1 | Mở form từ UC08.1 | Trả danh sách OrderItem và số bàn giao theo quyền |
| 2 | Nhập `receivedQuantity` cho mọi dòng | UI kiểm tra định dạng sơ bộ; backend vẫn kiểm tra lại |
| 3 | Gửi form | Gửi danh sách, `acceptMismatch`, note, version và idempotency key |
| 4 | — | Xác thực owner hoặc guest token có scope xác nhận |
| 5 | — | Khóa Order/OrderItem; kiểm tra `SHIPPING`, version, request key và dữ liệu bàn giao |
| 6 | — | Kiểm tra danh sách đủ, không trùng/lạ và số nguyên không âm |
| 7 | — | So sánh từng `receivedQuantity` với `exportedQuantity` hiệu lực và tạo mismatches |
| 8a | Nhận kết quả chênh lệch | Nếu lệch và chưa chấp nhận: trả `matched=false`, `completed=false`, Order vẫn `SHIPPING` |
| 8b | Xác nhận chấp nhận chênh lệch nếu muốn | Gửi request mới với `acceptMismatch=true`, cùng số lượng hiện hành, version hiện hành và idempotency key mới |
| 9 | — | Nếu khớp hoặc chấp nhận lệch: transaction lưu receipt, chuyển `DELIVERED`, ghi end time/history và công việc sau commit |
| 10 | Nhận kết quả hoàn tất | Trả `matched`, `completed=true`, mismatches nếu có, message và `DELIVERED` |
| 11 | — | Sau commit đồng bộ workflow/read model, cộng reputation USER đúng một lần và mời đánh giá nếu CATALOG |

### Ngoại lệ và cạnh tranh

| Mã | Điều kiện | Kết quả |
|---|---|---|
| B01 | Actor không phải owner hoặc guest token thiếu scope xác nhận | 401/403 |
| B02 | Order chưa `SHIPPING`, đã `CANCELLED` hoặc trạng thái khác | 409, không lưu receipt |
| B03 | Đơn mới thiếu exportedQuantity | Lỗi dữ liệu có thể quan sát, không fallback âm thầm và không DELIVERED |
| B04 | Danh sách thiếu, trùng hoặc có orderItemId ngoài Order | 400, rollback toàn bộ |
| B05 | receivedQuantity âm, thập phân hoặc sai kiểu | 400, rollback toàn bộ |
| B06 | Có chênh lệch và acceptMismatch=false | Phản hồi nghiệp vụ `completed=false`; không phải lỗi server |
| B07 | Có chênh lệch và acceptMismatch=true | Hoàn tất có audit, không tạo complaint |
| B08 | Order version cũ | 409, tải lại snapshot trước khi quyết định |
| B09 | Cùng idempotency key và cùng nội dung đã hoàn tất | Trả kết quả hiện có, không cộng điểm/phát event lần hai |
| B10 | Cùng key nhưng nội dung khác | 409, không ghi đè receipt |
| B11 | Hai request hoàn tất đồng thời | Khóa/version chỉ cho một transition và một lần cộng reputation |
| B12 | Request khác gửi sau DELIVERED | Không sửa receivedQuantity; trả kết quả cũ hoặc conflict theo nội dung |
| B13 | DB commit nhưng event/workflow/invite lỗi | Giữ DELIVERED, retry sau commit, không yêu cầu khách xác nhận lại |

## 6. Hợp đồng API đề xuất

### Đọc Order

USER gửi JWT; Guest gửi token bảo mật bằng cơ chế không làm lộ trong log. Response tối thiểu phục vụ nhận hàng:

```json
{
  "orderId": 1001,
  "orderType": "CATALOG",
  "status": "SHIPPING",
  "canConfirmReceipt": true,
  "items": [
    {
      "orderItemId": 501,
      "displayName": "Sản phẩm A",
      "orderedQuantity": 2,
      "exportedQuantity": 2
    }
  ]
}
```

### Xác nhận nhận hàng

```json
{
  "orderVersion": 13,
  "idempotencyKey": "bc6b5412-a2e4-4892-a1ef-ff647bfa38bb",
  "receivedItems": [
    {
      "orderItemId": 501,
      "receivedQuantity": 1
    }
  ],
  "acceptMismatch": false,
  "note": "Tôi chỉ nhận được 1 sản phẩm"
}
```

Response khi chưa chấp nhận chênh lệch:

```json
{
  "matched": false,
  "completed": false,
  "status": "SHIPPING",
  "mismatches": [
    {
      "orderItemId": 501,
      "orderedQuantity": 2,
      "exportedQuantity": 2,
      "receivedQuantity": 1
    }
  ],
  "message": "Số lượng thực nhận có chênh lệch. Hãy kiểm tra và xác nhận nếu bạn chấp nhận kết quả này."
}
```

Nếu khách xác nhận đúng dữ liệu trên với `acceptMismatch=true`, response giữ `matched=false`, chuyển `completed=true` và `status=DELIVERED`. Không được đổi `matched` thành true để che việc đã chấp nhận sai lệch.

Việc đổi `acceptMismatch` từ false sang true làm thay đổi nội dung request, vì vậy client phải tạo idempotency key mới. Gửi lại cùng key cũ với payload khác phải nhận conflict.

## 7. Transaction, idempotency và sự kiện

Transaction hoàn tất tối thiểu bao gồm receipt, toàn bộ `receivedQuantity`, trạng thái/version Order, `endOrderTime`, history, audit chấp nhận chênh lệch và outbox/job. Reputation ledger có thể nằm cùng transaction hoặc được consumer xử lý idempotent sau commit.

Sự kiện `ORDER_DELIVERED` tối thiểu có `eventId`, `orderId`, aggregate version, owner type/reference, orderType, `matched`, `acceptedMismatch`, occurredAt và actor reference. Không đưa guest token hoặc thông tin bí mật vào payload.

Các consumer phải chống trùng độc lập:

- Workflow correlation theo orderId + eventId.
- Reputation theo orderId + lý do hoàn tất, chỉ cho USER.
- Quyền/lời mời đánh giá theo orderId hoặc OrderItem + event type, chỉ CATALOG.
- Read model/cache theo aggregate version.

Lỗi consumer được retry và quan sát. Không chạy lại transaction nhận hàng để chữa lỗi event, reputation hoặc lời mời đánh giá.

## 8. Trạng thái, timer và thông báo

| Tình huống | Order status | Reputation | Email trạng thái | Hành động khác |
|---|---|---|---|---|
| Đọc link tại READY_TO_SHIP | Không đổi | Không đổi | Không gửi thêm | Cho xem, chưa cho xác nhận |
| SHIPPING, lệch và chưa chấp nhận | `SHIPPING` | Không đổi | Không gửi | Trả mismatches |
| SHIPPING, khớp | `DELIVERED` | USER +2 một lần | Không gửi DELIVERED | CATALOG mở/mời đánh giá |
| SHIPPING, chấp nhận lệch | `DELIVERED` | USER +2 một lần | Không gửi DELIVERED | Lưu audit, CATALOG mở/mời đánh giá |
| Guest hoàn tất | `DELIVERED` | Không có | Không gửi email trạng thái | CATALOG có thể nhận lời mời đánh giá bảo mật |

Không có timer tự chuyển `DELIVERED`. COD vẫn do shipper thu ngoài hệ thống; xác nhận nhận hàng không tự đánh dấu một PaymentAttempt COD là `PAID`.

## 9. Điểm cần chốt trước khi triển khai production

- TTL, cách cấp lại/revoke guest link và có đổi token thành guest access session/refresh session hay không.
- Kênh cụ thể cho lời mời đánh giá CATALOG của USER; Guest cần email/link bảo mật vì không có notification tài khoản.
- Policy migration xác định đơn nào được phép fallback `exportedQuantity = orderedQuantity`; đơn mới không được fallback.

Các điểm này không làm thay đổi điều kiện cốt lõi: Guest luôn cần credential gắn với Order, chỉ `SHIPPING` mới xác nhận và hoàn tất phải idempotent.

## 10. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | USER owner đọc Order | Trả snapshot đúng quyền |
| TC02 | USER khác đọc/xác nhận | Bị từ chối |
| TC03 | Guest có orderId nhưng không có token | Không đọc và không xác nhận |
| TC04 | Guest token sai Order/scope, hết hạn hoặc revoked | Bị từ chối |
| TC05 | Guest token hợp lệ mở READY_TO_SHIP | Xem được nhưng chưa xác nhận |
| TC06 | USER/Guest hợp lệ mở SHIPPING | `canConfirmReceipt=true` |
| TC07 | Receipt thiếu, trùng hoặc có item ngoài Order | Bị từ chối, không lưu một phần |
| TC08 | receivedQuantity âm, thập phân hoặc sai kiểu | Bị từ chối |
| TC09 | Đơn mới thiếu exportedQuantity | Không cho hoàn tất, báo lỗi dữ liệu |
| TC10 | Mọi receivedQuantity khớp exportedQuantity | `matched=true`, `completed=true`, DELIVERED |
| TC11 | Có lệch, acceptMismatch=false | `matched=false`, `completed=false`, Order vẫn SHIPPING |
| TC12 | Có lệch, acceptMismatch=true | DELIVERED và lưu dấu vết chấp nhận |
| TC13 | USER hoàn tất | Cộng đúng 2 reputation một lần |
| TC14 | Guest hoàn tất | Không tạo tài khoản và không cộng reputation |
| TC15 | Gửi lặp cùng key/payload sau DELIVERED | Trả kết quả cũ, không cộng điểm hoặc phát event nghiệp vụ lần hai |
| TC16 | Hai request hoàn tất đồng thời | Một transition DELIVERED và một reputation ledger |
| TC17 | Request khác payload sau DELIVERED | Không sửa receivedQuantity đã chốt |
| TC18 | Event/workflow/reputation consumer lỗi | Order vẫn DELIVERED, side effect retry idempotent |
| TC19 | Order CATALOG hoàn tất | Mở quyền/mời đánh giá catalog đúng một lần |
| TC20 | Order CUSTOM hoàn tất | Không tạo đánh giá custom |
| TC21 | Chuyển DELIVERED | Không gửi email/notification trạng thái thứ năm |
| TC22 | Khách không thao tác | Không có timer tự hoàn tất |
| TC23 | Hoàn tất COD | Không tạo PaymentAttempt hoặc tự đánh dấu COD PAID |
| TC24 | Hoàn tất Order | Không release staff lần nữa, không thay lịch sử assignment |
| TC25 | Chênh lệch số lượng | Không tự tạo complaint, hoàn tiền hoặc bồi thường |

## 11. Sơ đồ

- [Use Case tổng quát](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity Diagram theo từng UC](diagram.md).
- [Sequence Diagram theo từng UC](sequence.md).
