# WF07 — Bàn giao vận chuyển (`ship_order`)

Ngày lập: 06/10/2026. Đặc tả hệ thống mục tiêu, không phải xác nhận hành vi runtime hiện tại.

Nguồn: [target.txt](../../target.txt), mục B2–B3, C2, D1–D3, E/WF07, F15, F18, H–J. Đầu vào trực tiếp đến từ [WF05](../WF05-production-checkpoint/README.md); bước khách xác nhận nhận hàng thuộc WF08.

## 1. Phạm vi và điểm bàn giao

WF07 bắt đầu khi toàn bộ checkpoint cuối đã qua KCS và Order đang `READY_TO_SHIP`. Staff đã phụ trách Order ghi số lượng thực tế được bàn giao theo từng `OrderItem`. Khi dữ liệu hợp lệ được lưu thành công, hệ thống:

- Chuyển Order `READY_TO_SHIP → SHIPPING` đúng một lần.
- Lưu `exportedQuantity` cho đầy đủ các dòng hàng, thời điểm bàn giao, người thao tác và lịch sử.
- Ghi nhận thời gian giao dự kiến là 2 ngày, tách khỏi thời gian sản xuất.
- Cho phép WF08 dùng `exportedQuantity` làm mốc đối chiếu với số lượng khách khai nhận.

WF07 không điều phối hoạt động của đơn vị vận chuyển. Không có actor shipper, tài khoản shipper, phòng chat, API hãng vận chuyển, webhook tracking, đối soát COD hay bằng chứng rằng kiện hàng đang ở đâu. `SHIPPING` chỉ cho biết hệ thống đã ghi nhận việc bàn giao.

## 2. Danh mục use case và tác nhân

| Mã | Use case | Tác nhân chính | Kết quả |
|---|---|---|---|
| UC07.1 | Ghi nhận bàn giao vận chuyển | STAFF đã phụ trách Order | Lưu số lượng bàn giao và chuyển Order sang `SHIPPING` |

USER và Guest không thực hiện WF07; họ chỉ tra cứu/xác nhận ở WF08. MANAGER không duyệt lại việc bàn giao. Đơn vị vận chuyển là bên thứ ba ngoài phạm vi nên không được vẽ thành actor của hệ thống.

## 3. Quy tắc chung

### 3.1 Quyền thao tác sau khi staff đã được release

- WF05 đã đóng assignment và release staff khi checkpoint 2 của mọi loại đạt. WF07 không được yêu cầu assignment còn `ACTIVE`.
- Người được thao tác là staff lịch sử đã phụ trách Order, đối chiếu bằng `assignedStaffId`/assignment history được giữ lại sau release và danh tính từ JWT.
- Client không được gửi staffId để tự nhận quyền. Backend lấy actor từ access token và kiểm tra role cùng quan hệ lịch sử trong database.
- WF07 không release staff lần nữa và không xóa `assignedStaffId` hoặc lịch sử phân công.
- Nếu sau này cho phép kho hoặc vai trò giao nhận riêng thao tác, đó là thay đổi policy quyền phải được đặc tả; không tự mở quyền cho mọi STAFF hoặc MANAGER.

### 3.2 Dữ liệu bàn giao

- Request gửi danh sách theo `orderItemId`; không dùng product/variant id làm khóa cập nhật.
- Mỗi OrderItem của Order phải xuất hiện đúng một lần, không có dòng lạ và không trùng dòng.
- `exportedQuantity` là số nguyên, không âm và không lớn hơn `quantity` đã đặt.
- Danh sách là snapshot bàn giao cuối cùng của một lần giao. Thiết kế hiện tại không có nhiều kiện, nhiều chuyến hoặc cộng dồn nhiều lần bàn giao.
- Có thể lưu `handoverNote` và thông tin giao hàng nội bộ tối thiểu nếu cần audit. Không biến các trường này thành tích hợp tracking với hãng vận chuyển.
- `exportedQuantity` và `receivedQuantity` không phải dữ liệu tồn kho.

Target yêu cầu ghi số lượng thực tế bàn giao nhưng chưa quy định có bắt buộc mọi `exportedQuantity` phải bằng số lượng đặt hay không. Đặc tả này cho phép ghi số thực tế trong khoảng `0..quantity` để WF08 đối chiếu minh bạch, nhưng chỉ có **một lần bàn giao cuối**. Nếu nghiệp vụ muốn cấm bàn giao thiếu, cần siết thêm điều kiện tất cả `exportedQuantity = quantity` trước khi triển khai.

### 3.3 Trạng thái, thời gian và thông báo

- Tiền trạng thái duy nhất: `READY_TO_SHIP`. Thành công chuyển sang `SHIPPING`.
- Lưu `handedOverAt`/`shippingStartedAt` theo thời điểm server commit. Client không quyết định timestamp chính thức.
- `expectedShippingDays = 2` cho mọi Order và hiển thị tách khỏi `productionDurationDays`/deadline sản xuất.
- Thiết kế đề xuất có thể hiển thị `expectedDeliveryAt = shippingStartedAt + 2 ngày lịch`. Không áp dụng lịch nghỉ sản xuất cho quãng vận chuyển; đây chỉ là ước tính nội bộ, không phải cam kết hay tracking của shipper.
- `SHIPPING` không thuộc bốn mốc email trạng thái. Không gửi thêm email/notification trạng thái cho USER hoặc Guest chỉ vì chuyển sang `SHIPPING`.
- Email/notification `READY_TO_SHIP` đã được WF05 phát. Link bảo mật của Guest tiếp tục được dùng để mở Order ở WF08.
- Audit, lịch sử trạng thái và cập nhật UI vẫn được ghi dù không gửi email trạng thái.

### 3.4 COD, ONLINE và bên vận chuyển

- WF07 áp dụng giống nhau cho COD và ONLINE sau khi Order đạt `READY_TO_SHIP`.
- COD được shipper bên thứ ba thu ngoài hệ thống. Chuyển `SHIPPING` không tạo PaymentAttempt, không chờ webhook COD và không tự đổi payment thành `PAID`.
- ONLINE đã đi qua luồng thanh toán/bắt đầu phù hợp trước sản xuất; WF07 không kiểm tra lại hoặc thu thêm tiền.
- Shipper tự liên hệ qua kênh ngoài hệ thống. Không lưu diễn biến chat ngoài hệ thống như một workflow nội bộ.

### 3.5 Vai trò Camunda

WF07 không thuộc phạm vi điều phối chính WF02–WF05. Có thể triển khai bằng endpoint/service nghiệp vụ và sau commit đồng bộ bước tiếp theo của process đang có:

- Nếu process WF02–WF05 kết thúc ở `READY_TO_SHIP`, WF07 chỉ là nghiệp vụ độc lập và phát sự kiện `ORDER_SHIPPING` cho WF08/UI.
- Nếu process vòng đời được kéo dài, dùng một user task “Ghi nhận bàn giao” giao cho staff lịch sử, sau đó đi vào wait state nhận hàng của WF08.
- Không tạo process vòng đời thứ hai cho cùng Order và không lưu toàn bộ danh sách OrderItem vào process variables.
- Database là nguồn quyết định quyền, trạng thái và số lượng. Engine chỉ giữ khóa tham chiếu và vị trí điều phối.
- Đồng bộ sau commit phải retry/idempotent; lỗi correlate không rollback việc bàn giao đã commit và không được chuyển trạng thái lần hai.

## 4. UC07.1 — Staff ghi nhận bàn giao vận chuyển

| Thuộc tính | Đặc tả |
|---|---|
| Mục tiêu | Lưu số lượng thực tế đã bàn giao và đánh dấu Order đang vận chuyển |
| Kích hoạt | Staff đã phụ trách mở Order đang `READY_TO_SHIP` và chọn “Bàn giao vận chuyển” |
| Tiền điều kiện | Order `READY_TO_SHIP`; mọi checkpoint cuối đã qua KCS; actor là staff lịch sử của Order |
| Đầu vào | orderId, danh sách `{orderItemId, exportedQuantity}`, handoverNote/thông tin nội bộ tùy chọn, order version, idempotency key |
| Thành công | Toàn bộ số lượng bàn giao được lưu; Order `SHIPPING`; có thời điểm, actor, history và kết quả idempotent |
| Bảo đảm thất bại | Không lưu một phần danh sách, không đổi trạng thái và không phát bước workflow thành công |

### Luồng chính

| Bước | STAFF | Hệ thống |
|---|---|---|
| 1 | Mở Order cần bàn giao | Xác thực JWT, kiểm tra role và staff lịch sử đã phụ trách Order |
| 2 | Xem danh sách dòng hàng | Trả OrderItem, số lượng đặt, trạng thái KCS và trạng thái Order theo quyền |
| 3 | Nhập số lượng thực tế cho từng dòng và ghi chú nếu có | UI kiểm tra định dạng sơ bộ nhưng backend vẫn là nơi quyết định |
| 4 | Bấm xác nhận bàn giao | Gửi order version và idempotency key cùng toàn bộ danh sách |
| 5 | — | Khóa Order và OrderItem; kiểm tra quyền lịch sử, `READY_TO_SHIP`, version và request chưa được xử lý |
| 6 | — | Kiểm tra đủ dòng, không trùng/lạ, số nguyên và trong khoảng `0..quantity` |
| 7 | — | Trong một transaction lưu `exportedQuantity`, actor, thời điểm, ghi chú, `SHIPPING`, history và công việc sau commit |
| 8 | Nhận kết quả | Trả Order `SHIPPING`, số lượng đã lưu, `shippingStartedAt`, `expectedShippingDays = 2` và thời gian giao dự kiến nếu được tính |
| 9 | — | Sau commit đồng bộ workflow/UI theo event idempotent; không gửi email trạng thái SHIPPING |

Việc lưu danh sách và chuyển trạng thái phải nguyên tử. Không được lưu vài dòng rồi để Order ở `READY_TO_SHIP`, cũng không được chuyển `SHIPPING` trước khi mọi dòng hợp lệ đã được lưu.

### Ngoại lệ và cạnh tranh

| Mã | Điều kiện | Kết quả |
|---|---|---|
| A01 | Token thiếu/sai role | 401/403, không lộ dữ liệu ngoài quyền |
| A02 | Staff không phải người lịch sử phụ trách Order | 403, không cho mọi STAFF bàn giao tùy ý |
| A03 | Backend kiểm tra assignment còn active | Đây là lỗi thiết kế; WF05 đã release staff, phải dùng quan hệ lịch sử |
| A04 | Order chưa `READY_TO_SHIP` | 409, không bàn giao sớm |
| A05 | Order đã `SHIPPING`, `DELIVERED` hoặc `CANCELLED` | Trả kết quả idempotent nếu đúng request cũ, nếu khác thì conflict; không ghi đè |
| A06 | Thiếu dòng, trùng `orderItemId` hoặc có dòng ngoài Order | 400, rollback toàn bộ |
| A07 | Số lượng không phải số nguyên, âm hoặc lớn hơn quantity | 400, rollback toàn bộ |
| A08 | Order version cũ | 409, yêu cầu tải lại dữ liệu |
| A09 | Cùng idempotency key và cùng nội dung | Trả kết quả bàn giao đã commit, không phát event lần hai |
| A10 | Cùng key nhưng nội dung khác | 409, không sửa kết quả cũ |
| A11 | Hai request cạnh tranh | Khóa/version chỉ cho một request chuyển trạng thái; request còn lại đọc kết quả hoặc conflict |
| A12 | DB commit nhưng workflow/UI handoff lỗi | Giữ `SHIPPING`, retry job/outbox; không yêu cầu staff bàn giao lại |

## 5. Hợp đồng dữ liệu đề xuất

### Request

```json
{
  "orderVersion": 12,
  "idempotencyKey": "7f58d54d-0747-4e4e-b75d-2b70b2eb60ef",
  "items": [
    {
      "orderItemId": 501,
      "exportedQuantity": 2
    }
  ],
  "handoverNote": "Đã bàn giao kiện hàng cho đơn vị vận chuyển"
}
```

Staff identity không nằm trong request. Endpoint lấy từ JWT. Không nhận `status`, `shippingStartedAt`, `paymentStatus` hoặc `expectedShippingDays` do client tự đặt.

### Response tối thiểu

```json
{
  "orderId": 1001,
  "status": "SHIPPING",
  "shippingStartedAt": "2026-10-06T15:30:00+07:00",
  "expectedShippingDays": 2,
  "items": [
    {
      "orderItemId": 501,
      "orderedQuantity": 2,
      "exportedQuantity": 2
    }
  ]
}
```

`expectedDeliveryAt` có thể được bổ sung nếu đội dự án chốt quy ước cộng 2 ngày lịch và múi giờ hiển thị. Không trả dữ liệu nội bộ của shipper vì hệ thống không có dữ liệu đó.

## 6. Dữ liệu cần lưu và lịch sử

| Dữ liệu | Yêu cầu |
|---|---|
| `OrderItem.exportedQuantity` | Số thực tế bàn giao, bắt buộc cho đơn mới trước khi sang SHIPPING |
| `Order.status` | `SHIPPING` sau khi toàn bộ request hợp lệ được commit |
| `shippingStartedAt`/`handedOverAt` | Thời điểm server ghi nhận bàn giao |
| `expectedShippingDays` | Snapshot giá trị 2 ngày hoặc giá trị cấu hình cố định tương đương |
| Actor | staffId từ JWT và quan hệ lịch sử với Order |
| Audit/history | Trạng thái cũ/mới, actor, timestamp, version và ghi chú |
| Idempotency/outbox | Khóa request và event để chống ghi/phát trùng |

Không dùng cache làm nguồn duy nhất cho quyền thao tác hoặc trạng thái. Không xóa lịch sử assignment sau release vì WF07 cần xác định đúng staff và toàn hệ thống cần audit.

## 7. Sự kiện và thông báo

Thiết kế đề xuất phát một sự kiện nội bộ sau commit, ví dụ `ORDER_SHIPPING`, chứa tối thiểu `eventId`, `orderId`, `occurredAt`, version và actor reference. Consumer có thể:

- Làm mới read model/cache và giao diện theo dõi Order.
- Đồng bộ wait state của process nếu WF08 được nối vào Camunda.
- Ghi telemetry/audit kỹ thuật.

Consumer **không** được route sự kiện này thành email/notification trạng thái cho chủ đơn. Bốn email trạng thái bình thường vẫn chỉ là `PENDING_APPROVAL`, `ORDER_ACCEPTED`, `ORDER_CREATING`, `READY_TO_SHIP`.

## 8. Quan hệ với WF05 và WF08

| Điểm | WF05 | WF07 | WF08 |
|---|---|---|---|
| Trạng thái chính | `ORDER_CREATING → READY_TO_SHIP` | `READY_TO_SHIP → SHIPPING` | `SHIPPING → DELIVERED` khi đủ điều kiện |
| Số lượng | Sản xuất/KCS theo OrderItem | Ghi `exportedQuantity` | Khách ghi `receivedQuantity` và backend so sánh |
| Staff assignment | Release sau checkpoint 2 cuối | Không release lại, dùng lịch sử quyền | Không release lại |
| Email trạng thái | Gửi `READY_TO_SHIP` | Không gửi cho `SHIPPING` | Không gửi chỉ vì `DELIVERED` |
| Guest link | Email READY_TO_SHIP chứa link bảo mật | Không tạo link mới | Dùng token để đọc/xác nhận |

## 9. Điểm cần chốt trước khi triển khai production

Target chưa xác định đầy đủ hai chi tiết sau:

- Có cấm tuyệt đối bàn giao thiếu hay cho phép `exportedQuantity < orderedQuantity` để phản ánh số thực tế. Đặc tả hiện cho phép số thực tế trong `0..quantity`, không hỗ trợ giao bù nhiều chuyến.
- Có cần lưu `expectedDeliveryAt` hay chỉ hiển thị `expectedShippingDays = 2`; nếu lưu ngày dự kiến, cần thống nhất cộng 2 ngày lịch từ timestamp nào và múi giờ hiển thị.

Những điểm này không mở rộng thành tích hợp vận chuyển. Cho đến khi có quyết định, không thêm tracking code bắt buộc, trạng thái hãng vận chuyển, nhiều kiện hoặc nhiều lần giao.

## 10. Tiêu chí nghiệm thu

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC01 | Staff lịch sử phụ trách bàn giao Order READY_TO_SHIP với đủ dòng | Lưu số lượng nguyên tử và chuyển SHIPPING |
| TC02 | Staff đã được release ở WF05 | Vẫn thao tác được nhờ quan hệ lịch sử, không yêu cầu active assignment |
| TC03 | Staff khác hoặc MANAGER thao tác | Bị từ chối theo policy hiện tại |
| TC04 | Order chưa READY_TO_SHIP | Không lưu bàn giao, không đổi trạng thái |
| TC05 | Danh sách thiếu, thừa hoặc trùng OrderItem | Validation lỗi và không lưu một phần |
| TC06 | exportedQuantity âm, thập phân hoặc lớn hơn quantity | Bị từ chối |
| TC07 | Request hợp lệ | Mỗi OrderItem có exportedQuantity, Order có actor/time/history/version |
| TC08 | Request lặp cùng key và payload | Trả cùng kết quả, không chuyển trạng thái/phát event lần hai |
| TC09 | Cùng key nhưng payload khác | Conflict, không ghi đè số lượng |
| TC10 | Hai request đồng thời | Chỉ một request thắng và một lần chuyển SHIPPING |
| TC11 | Job đồng bộ Camunda/UI lỗi sau commit | Order vẫn SHIPPING và job retry an toàn |
| TC12 | Chuyển SHIPPING | Không gửi email/notification trạng thái thứ năm cho USER/Guest |
| TC13 | Guest Order | Không có chat/tài khoản shipper; link READY_TO_SHIP vẫn dùng cho WF08 |
| TC14 | COD Order | Không tạo webhook/payment PAID nội bộ ở bước bàn giao |
| TC15 | ONLINE Order | Không thu lại hoặc thay đổi payment ở WF07 |
| TC16 | Hiển thị thời gian | 2 ngày vận chuyển tách khỏi deadline sản xuất |
| TC17 | Hoàn tất WF07 | Staff không bị release lần hai và lịch sử assigned staff vẫn còn |
| TC18 | Dữ liệu WF08 | exportedQuantity đã lưu sẵn để so với receivedQuantity |

## 11. Sơ đồ

- [Use Case tổng quát](overview.svg) và [nguồn PlantUML](overview.puml).
- [Activity Diagram](diagram.md).
- [Sequence Diagram](sequence.md).
