# Bàn giao refactor latency chat P8 cho Antigravity

Ngày cập nhật: 12/10/2026.

Trạng thái hiện tại: `VERIFIED` theo review và chấp nhận của chủ dự án ngày 28/10/2026. Kết quả kiểm thử được giữ theo đúng các lượt chạy ghi trong tài liệu.

## Backend đã thay đổi

- Không tạo entity/repository Mongo riêng cho order-chat. Backend tái sử dụng `ChatMessage` và `ChatMessageRepository` hiện có.
- `ChatMessage` chỉ còn `id`, `chatThreadId`, `senderId`, `content`, `timestamp`, `messageType`; toàn bộ field consultation legacy đã bị loại bỏ.
- `OrderChatThread` là room/security boundary, giữ `orderId`, owner, assigned staff và display name của hai participant.
- Server lấy `senderId` từ JWT Principal. Không nhận danh tính, role hoặc timestamp của sender từ payload.
- Mỗi lần gửi không còn gọi `OrderRepository.findById()` hoặc `UserRepository.findById()`; do đó không còn tải items/product/voucher/user graph trên hot path.
- `senderRole` và `senderName` trong response được suy ra từ room nên REST/WebSocket response contract hiện tại không đổi.
- Subscription `/topic/order-chat/{orderId}` được kiểm tra membership theo JWT. Các topic chat tư vấn legacy đã bị loại bỏ.
- `ChatMessage` khai báo Mongo index `chat_thread_timestamp_idx` trên `(chatThreadId, timestamp)`. Môi trường phải bật `MONGODB_AUTO_INDEX_CREATION=true` hoặc tạo index tương đương khi deploy.

## Cutover dữ liệu legacy

Liquibase changeset `07-order-agreement-chat.xml` tự kiểm tra tồn tại rồi xóa các bảng consultation, bảng SQL `chat_messages` cũ và cột `notifications.consultation_request_id`.

Sau khi backup và triển khai backend mới, chạy lệnh sau trên đúng MongoDB database để xóa document chat tư vấn cũ và index không còn dùng:

```javascript
db.chat_messages.deleteMany({ chatThreadId: null })
["chat_user_timestamp_idx", "chat_consultation_timestamp_idx"].forEach(name => {
  if (db.chat_messages.getIndexes().some(index => index.name === name)) {
    db.chat_messages.dropIndex(name)
  }
})
if (db.getCollectionNames().includes("staff_commission_daily_summaries")) {
  db.staff_commission_daily_summaries.drop()
}
```

Không xóa collection `chat_messages`, vì order-chat tiếp tục dùng collection này.

## Contract FE

Không bắt buộc sửa model hoặc endpoint FE. Các contract vẫn giữ nguyên:

```text
POST      /api/orders/{orderId}/chat/messages
SEND      /app/order-chat.send
SUBSCRIBE /topic/order-chat/{orderId}
```

Payload gửi vẫn là:

```json
{
  "orderId": 123,
  "content": "Nội dung"
}
```

Hiện hai màn hình order-chat vẫn gửi bằng HTTP POST rồi nhận broadcast bằng WebSocket. Nếu chuyển hoàn toàn sang WebSocket, dùng `WebsocketService.sendOrderChat()` và chỉ thêm message khi nhận broadcast từ `/topic/order-chat/{orderId}` để tránh render trùng.

## Cách xác minh latency

Gửi tối thiểu 20 message sau khi ứng dụng warm-up. Với message có tổng backend time từ 100 ms, log sẽ có dạng:

```text
Slow order-chat send orderId=123 authorizationMs=... persistenceMs=... publishMs=... totalMs=...
```

- `authorizationMs`: JWT extraction + SQL lookup room + participant check.
- `persistenceMs`: MongoDB save.
- `publishMs`: simple broker broadcast.
- Nếu backend `totalMs` thấp nhưng UI vẫn 400–500 ms, kiểm tra Network để xác nhận SockJS đã upgrade sang WebSocket thay vì XHR polling và tách thời gian HTTP POST khỏi thời gian receiver nhận broadcast.

## Kiểm thử

```text
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd '-Dtest=OrderChatServiceTest,WebSocketAuthInterceptorTest' test
.\mvnw.cmd test
```

Kết quả full suite sau khi loại bỏ module legacy: `281` test, `0` failure, `0` error, `7` skipped.
