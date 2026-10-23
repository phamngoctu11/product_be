/**
 * Google Apps Script cập nhật đề cương tốt nghiệp theo dự án product_be/product_fe.
 *
 * Cách dùng an toàn:
 * 1. Mở https://script.google.com hoặc Extensions > Apps Script trong Google Docs.
 * 2. Dán toàn bộ file này vào Code.gs.
 * 3. Chạy previewGraduationProposal() để xem bản nháp trong một tài liệu mới.
 * 4. Khi nội dung đã ổn, chạy updateGraduationProposal(). Hàm này luôn tạo bản sao
 *    dự phòng trên Google Drive trước khi ghi đè tài liệu đích.
 */

const DOCUMENT_ID = '1uiF0OuORGhqbVRt4tlbbEqHWg-X3SFyg';
const FONT_FAMILY = 'Times New Roman';
const BODY_FONT_SIZE = 13;

function previewGraduationProposal() {
  const preview = DocumentApp.create('[PREVIEW] Đề cương website handmade made-to-order');
  buildProposal_(preview);
  Logger.log('Bản xem trước: ' + preview.getUrl());
}

function updateGraduationProposal() {
  const target = DocumentApp.openById(DOCUMENT_ID);
  const timestamp = Utilities.formatDate(
    new Date(),
    Session.getScriptTimeZone() || 'Asia/Ho_Chi_Minh',
    'yyyyMMdd-HHmmss'
  );
  const backup = DriveApp.getFileById(DOCUMENT_ID).makeCopy(
    '[BACKUP ' + timestamp + '] ' + target.getName()
  );

  buildProposal_(target);

  Logger.log('Đã cập nhật: ' + target.getUrl());
  Logger.log('Bản sao dự phòng: ' + backup.getUrl());
}

function buildProposal_(doc) {
  const body = doc.getBody();
  body.clear();
  body.setMarginTop(54);
  body.setMarginBottom(54);
  body.setMarginLeft(72);
  body.setMarginRight(72);

  appendInstitutionHeader_(body);
  appendSpacer_(body, 1);
  appendCenteredTitle_(body, 'ĐỀ CƯƠNG CHI TIẾT', 15);
  appendCenteredTitle_(body, 'KHÓA LUẬN / CHUYÊN ĐỀ TỐT NGHIỆP', 15);
  appendSpacer_(body, 2);

  appendSectionTitle_(body, '1. Thông tin chung');
  appendLabeledBullet_(
    body,
    'Tên đề tài (Dự kiến): ',
    'Xây dựng website kinh doanh sản phẩm handmade theo yêu cầu, ứng dụng BPMN trong quản lý vòng đời đơn hàng.'
  );
  appendLabeledBullet_(body, 'Sinh viên thực hiện: ', 'Phạm Ngọc Tú — MSSV: A46764.');
  appendLabeledBullet_(body, 'Giảng viên hướng dẫn: ', 'ThS. Phạm Ngọc Bích.');
  appendLabeledBullet_(body, 'Hướng đề tài: ', 'Website làm và bán sản phẩm theo nhu cầu (made-to-order).');

  appendSectionTitle_(body, '2. Lý do chọn đề tài (Tính cấp thiết)');
  appendLabeledBullet_(
    body,
    'Vấn đề thực tế: ',
    'Sản phẩm handmade theo yêu cầu có vòng đời xử lý dài và phụ thuộc nhiều bên: khách hàng, quản lý và nhân viên sản xuất. Mỗi đơn có thể khác nhau về mẫu, số lượng, mô tả, hình ảnh, giá thỏa thuận, phương thức thanh toán và thời hạn hoàn thành. Nếu quản lý bằng tin nhắn hoặc bảng tính rời rạc, doanh nghiệp khó kiểm soát phiên bản yêu cầu, phân công nhân viên, tiến độ từng sản phẩm, các lần làm lại, kiểm tra chất lượng và trách nhiệm ở từng bước.'
  );
  appendLabeledBullet_(
    body,
    'Sự cần thiết của giải pháp CNTT: ',
    'Một hệ thống web thống nhất giúp số hóa từ lúc khách xem mẫu hoặc gửi yêu cầu tùy chỉnh đến khi quản lý duyệt, nhân viên nhận việc, hai bên chốt thỏa thuận, thanh toán, sản xuất, KCS, giao hàng và xác nhận nhận hàng. Luồng BPMN giữ các điểm chờ người dùng, timer và vòng lặp rework; cơ sở dữ liệu vẫn là nguồn trạng thái nghiệp vụ chính, giúp quá trình có thể theo dõi, khôi phục và kiểm toán.'
  );
  appendLabeledBullet_(
    body,
    'Tính mới/cải tiến của đề tài: ',
    'Hệ thống kết hợp thương mại điện tử made-to-order với một vòng đời đơn hàng BPMN duy nhất cho khách có tài khoản và khách vãng lai. Giải pháp hỗ trợ thỏa thuận có phiên bản, chat theo đúng thành viên đơn hàng, deadline tuyệt đối, thanh toán COD/SePay, checkpoint hình dáng ban đầu và sản phẩm hoàn thiện, KCS/rework theo từng OrderItem, yêu cầu thay đổi có kiểm soát, nhận hàng có đối soát số lượng và cơ chế Outbox/Redis Stream/DLQ để hạn chế mất hoặc lặp tác vụ sau giao dịch.'
  );

  appendSectionTitle_(body, '3. Mục tiêu và phạm vi nghiên cứu');
  appendLabeledBullet_(
    body,
    'Mục tiêu tổng quát: ',
    'Xây dựng ứng dụng web quản lý xuyên suốt hoạt động đặt và sản xuất sản phẩm handmade theo yêu cầu, bảo đảm đúng quyền, đúng trạng thái, có lịch sử kiểm toán và xử lý an toàn các yêu cầu lặp hoặc đồng thời.'
  );
  appendLabeledBullet_(
    body,
    'Mục tiêu cụ thể: ',
    'Xây dựng luồng Catalog và Custom; quản lý duyệt và phân công; nhân viên tự nhận việc khi AVAILABLE; chốt thỏa thuận và giá; thanh toán; bắt đầu sản xuất; checkpoint/KCS/rework; thay đổi yêu cầu; bàn giao, nhận hàng, đánh giá và đặt lại; đồng thời cung cấp email, notification, chat thời gian thực và màn hình theo dõi sự cố workflow.'
  );
  appendLabeledBullet_(
    body,
    'Đối tượng sử dụng: ',
    'Guest, User, Staff, Manager và Admin. Quyền thao tác được kiểm soát bằng JWT/RBAC và trạng thái nghiệp vụ do backend xác nhận.'
  );
  appendLabeledBullet_(
    body,
    'Phạm vi trong đề tài: ',
    'Danh mục mẫu handmade; giỏ hàng và checkout; yêu cầu tùy chỉnh; voucher; quản lý đơn; phân công; chat và thỏa thuận; thanh toán COD/ONLINE; tiến độ sản xuất; checkpoint ảnh; KCS; yêu cầu thay đổi; giao nhận; xác nhận số lượng; đánh giá, đặt lại; email/notification; báo cáo quản trị và quan sát Outbox/DLQ.'
  );
  appendLabeledBullet_(
    body,
    'Ngoài phạm vi: ',
    'Không thực hiện dự báo tồn kho, Safety Stock, ROP, quản lý nhà cung cấp hoặc ERP/kế toán. Hệ thống không reserve/deduct/restore tồn kho thành phẩm trong luồng đặt hàng; không xử lý hoa hồng tư vấn; không tối ưu tuyến vận chuyển và không thay thế hệ thống quyết toán của ngân hàng/cổng thanh toán.'
  );

  appendSectionTitle_(body, '4. Phương pháp nghiên cứu và công nghệ sử dụng');
  appendLabeledBullet_(
    body,
    'Quy trình thực hiện: ',
    'Khảo sát nghiệp vụ made-to-order → chuẩn hóa yêu cầu và quy tắc nghiệp vụ → mô hình hóa WF01–WF09 bằng Use Case, Activity, Sequence và BPMN → thiết kế miền dữ liệu, REST API và giao diện → triển khai theo 13 phần phụ thuộc → kiểm thử đơn vị, tích hợp, migration, đồng thời/idempotency và nghiệm thu theo AC01–AC47 → hoàn thiện tài liệu, demo và kế hoạch triển khai.'
  );
  appendLabeledBullet_(
    body,
    'Backend: ',
    'Java 21, Spring Boot 3.1.12, Spring Security/OAuth2 Resource Server, Spring Data JPA, Camunda Platform 7.21, WebSocket STOMP, Maven, MapStruct và Liquibase.'
  );
  appendLabeledBullet_(
    body,
    'Frontend: ',
    'Angular 20, Angular Material, Bootstrap, RxJS, STOMP/SockJS, QR Code và Chart.js.'
  );
  appendLabeledBullet_(
    body,
    'Dữ liệu và hạ tầng: ',
    'MySQL lưu dữ liệu nghiệp vụ; MongoDB lưu tin nhắn chat; Redis dùng cho cache, idempotency, rate limit và Redis Stream/DLQ; Keycloak cung cấp định danh và RBAC; Cloudinary lưu ảnh; SePay tiếp nhận thanh toán online qua webhook; ngrok hỗ trợ kiểm thử webhook tại môi trường local.'
  );
  appendLabeledBullet_(
    body,
    'Kiểm thử và công cụ: ',
    'JUnit 5, Mockito, Spring Boot Test, H2, Testcontainers MySQL, Jasmine/Karma, ChromeHeadless, Git, IntelliJ IDEA/VS Code, Postman và Draw.io/PlantUML.'
  );

  appendSectionTitle_(body, '5. Cấu trúc nội dung dự kiến (Các chương)');
  appendNumberedItem_(body, 'Chương 1 — Mở đầu: ', 'Tính cấp thiết, mục tiêu, phạm vi, đối tượng sử dụng, phương pháp tiếp cận và đóng góp của đề tài.');
  appendNumberedItem_(body, 'Chương 2 — Cơ sở lý thuyết và công nghệ: ', 'Thương mại điện tử made-to-order, BPMN/workflow, kiến trúc phân lớp, REST, WebSocket, JWT/RBAC, idempotency, Transactional Outbox, Redis Stream và các công nghệ sử dụng.');
  appendNumberedItem_(body, 'Chương 3 — Phân tích và thiết kế hệ thống: ', 'Tác nhân, yêu cầu chức năng/phi chức năng, quy tắc nghiệp vụ, WF01–WF09, AC01–AC47, Use Case, Activity/Sequence Diagram, mô hình trạng thái, kiến trúc, ERD, từ điển dữ liệu, API và thiết kế giao diện.');
  appendNumberedItem_(body, 'Chương 4 — Cài đặt và kiểm thử: ', 'Cấu hình môi trường; triển khai backend/frontend; Camunda OrderLifecycle; chat; SePay; checkpoint/KCS; giao nhận; Outbox/DLQ; migration; kiểm thử chức năng, phân quyền, đồng thời, idempotency, timer và các luồng end-to-end.');
  appendNumberedItem_(body, 'Chương 5 — Kết luận và hướng phát triển: ', 'Đánh giá kết quả, hạn chế, bài học triển khai và định hướng mở rộng vận chuyển, thanh toán, quan sát hệ thống và vận hành production.');

  appendSectionTitle_(body, '6. Kế hoạch thực hiện chi tiết (Timeline)');
  appendTimelineTable_(body);

  appendSectionTitle_(body, '7. Sản phẩm cuối cùng');
  appendBodyParagraph_(
    body,
    'Web Application kinh doanh sản phẩm handmade theo yêu cầu, gồm frontend Angular, backend Spring Boot, workflow Camunda, cơ sở dữ liệu và bộ tài liệu phân tích–thiết kế–kiểm thử.'
  );
  appendLabeledBullet_(body, 'Đăng nhập và phân quyền: ', 'Keycloak/JWT với các vai trò Guest, User, Staff, Manager và Admin; backend tiếp tục kiểm tra quyền trên từng API.');
  appendLabeledBullet_(body, 'Catalog và Custom: ', 'Quản lý mẫu handmade, biến thể, giỏ hàng, checkout User/Guest và custom draft có mô tả tối đa 4.000 từ cùng danh sách ảnh.');
  appendLabeledBullet_(body, 'Duyệt và phân công: ', 'Manager duyệt/từ chối, phân công Staff; Staff AVAILABLE có thể self-claim; khóa dữ liệu và ràng buộc unique ngăn nhận trùng hoặc vượt giới hạn.');
  appendLabeledBullet_(body, 'Chat và thỏa thuận: ', 'Một phòng chat theo Order, xác thực thành viên từ JWT, thỏa thuận có version, giá và phương thức COD/ONLINE; timeout custom 24 giờ.');
  appendLabeledBullet_(body, 'Thanh toán và sản xuất: ', 'SePay webhook có xác thực/idempotency, timer thanh toán một giờ, Staff chủ động bắt đầu sản xuất và hệ thống tính deadline theo lịch làm việc.');
  appendLabeledBullet_(body, 'Checkpoint và KCS: ', 'Theo dõi INITIAL_SHAPE và FINAL_PRODUCT theo từng OrderItem, lưu ảnh/lần thử/quyết định, hỗ trợ rework và chỉ READY_TO_SHIP khi toàn bộ item đạt.');
  appendLabeledBullet_(body, 'Thay đổi, giao nhận và sau bán: ', 'ChangeRequest có kiểm soát phiên bản; bàn giao vận chuyển; User/Guest xác nhận số lượng; hoàn tất đơn, đánh giá và reorder Catalog/Custom theo đúng contract.');
  appendLabeledBullet_(body, 'Độ tin cậy và vận hành: ', 'Transaction/audit, Idempotency-Key, Outbox, Redis Stream retry/DLQ, migration Liquibase, email/notification và màn hình workflow operations cho Manager/Admin.');
  appendLabeledBullet_(body, 'Hồ sơ bàn giao: ', 'Source code, cấu hình mẫu, BPMN, Use Case, Sequence/Activity Diagram, ERD, API contract, bộ test và hướng dẫn triển khai/rollback.');

  appendSectionTitle_(body, '8. Tài liệu tham khảo dự kiến');
  appendReference_(body, 'Object Management Group. (2014). Business Process Model and Notation (BPMN), Version 2.0.2.');
  appendReference_(body, 'Camunda. Camunda Platform 7 Documentation: Process Engine, BPMN, User Tasks, Messages and Timers.');
  appendReference_(body, 'Spring. Spring Boot, Spring Security, Spring Data JPA and Spring WebSocket Reference Documentation.');
  appendReference_(body, 'Redis Ltd. Redis Documentation: Streams, Consumer Groups and Reliability Patterns.');
  appendReference_(body, 'Oracle. MySQL 8.0 Reference Manual.');
  appendReference_(body, 'Jones, M., Bradley, J., & Sakimura, N. (2015). RFC 7519: JSON Web Token (JWT). IETF.');
  appendReference_(body, 'Richardson, C. (2018). Microservices Patterns. Manning Publications.');
  appendReference_(body, 'Angular Team. Angular Documentation: Components, Routing, Forms, HTTP Client and Testing.');

  appendSpacer_(body, 2);
  appendSignatureTable_(body);
  appendSpacer_(body, 2);
  const note = appendBodyParagraph_(
    body,
    'Ghi chú dành cho sinh viên: Đề cương cần được Giảng viên hướng dẫn thông qua trước khi hoàn thiện khóa luận. Phạm vi triển khai tập trung vào quản lý vòng đời đơn hàng handmade made-to-order; không mở rộng trở lại bài toán dự báo tồn kho hoặc quản trị chuỗi cung ứng.'
  );
  note.editAsText().setItalic(true).setFontSize(11);

  doc.saveAndClose();
}

function appendInstitutionHeader_(body) {
  const table = body.appendTable([
    [
      'TRƯỜNG ĐẠI HỌC THĂNG LONG\nKHOA CÔNG NGHỆ THÔNG TIN\n________________',
      'CỘNG HOÀ XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n________________'
    ]
  ]);
  table.setBorderWidth(0);
  for (let column = 0; column < 2; column++) {
    const paragraph = table.getRow(0).getCell(column).getChild(0).asParagraph();
    paragraph.setAlignment(DocumentApp.HorizontalAlignment.CENTER);
    paragraph.setLineSpacing(1.15);
    paragraph.editAsText().setFontFamily(FONT_FAMILY).setFontSize(12).setBold(true);
  }
}

function appendCenteredTitle_(body, text, size) {
  const paragraph = body.appendParagraph(text);
  paragraph.setAlignment(DocumentApp.HorizontalAlignment.CENTER);
  paragraph.setSpacingAfter(4);
  paragraph.editAsText().setFontFamily(FONT_FAMILY).setFontSize(size).setBold(true);
  return paragraph;
}

function appendSectionTitle_(body, text) {
  const paragraph = body.appendParagraph(text);
  paragraph.setSpacingBefore(10);
  paragraph.setSpacingAfter(4);
  paragraph.setLineSpacing(1.15);
  paragraph.editAsText().setFontFamily(FONT_FAMILY).setFontSize(BODY_FONT_SIZE).setBold(true);
  return paragraph;
}

function appendBodyParagraph_(body, text) {
  const paragraph = body.appendParagraph(text);
  paragraph.setAlignment(DocumentApp.HorizontalAlignment.JUSTIFY);
  paragraph.setLineSpacing(1.15);
  paragraph.setSpacingAfter(4);
  paragraph.setIndentFirstLine(28);
  paragraph.editAsText().setFontFamily(FONT_FAMILY).setFontSize(BODY_FONT_SIZE);
  return paragraph;
}

function appendLabeledBullet_(body, label, content) {
  const item = body.appendListItem(label + content);
  item.setGlyphType(DocumentApp.GlyphType.BULLET);
  item.setAlignment(DocumentApp.HorizontalAlignment.JUSTIFY);
  item.setLineSpacing(1.15);
  item.setSpacingAfter(4);
  const text = item.editAsText();
  text.setFontFamily(FONT_FAMILY).setFontSize(BODY_FONT_SIZE);
  text.setBold(0, label.length - 1, true);
  return item;
}

function appendNumberedItem_(body, label, content) {
  const item = body.appendListItem(label + content);
  item.setGlyphType(DocumentApp.GlyphType.NUMBER);
  item.setAlignment(DocumentApp.HorizontalAlignment.JUSTIFY);
  item.setLineSpacing(1.15);
  item.setSpacingAfter(4);
  const text = item.editAsText();
  text.setFontFamily(FONT_FAMILY).setFontSize(BODY_FONT_SIZE);
  text.setBold(0, label.length - 1, true);
  return item;
}

function appendReference_(body, text) {
  const item = body.appendListItem(text);
  item.setGlyphType(DocumentApp.GlyphType.NUMBER);
  item.setAlignment(DocumentApp.HorizontalAlignment.JUSTIFY);
  item.setLineSpacing(1.15);
  item.setSpacingAfter(3);
  item.editAsText().setFontFamily(FONT_FAMILY).setFontSize(12);
}

function appendTimelineTable_(body) {
  const rows = [
    ['Tuần', 'Nội dung công việc', 'Kết quả đạt được'],
    ['1–2', 'Khảo sát nghiệp vụ handmade made-to-order; xác định tác nhân, phạm vi, yêu cầu chức năng/phi chức năng và tiêu chí nghiệm thu.', 'Đề cương; target nghiệp vụ; actor/stakeholder; quy tắc và AC01–AC47.'],
    ['3–4', 'Mô hình hóa WF01–WF09; thiết kế Use Case, Activity, Sequence, BPMN, trạng thái Order và kiến trúc tổng thể.', 'Bộ sơ đồ phân tích; đặc tả use case; BPMN và kiến trúc hệ thống.'],
    ['5–6', 'Thiết kế dữ liệu/API; triển khai nền tảng nhất quán, catalog, cart/checkout, custom request và nghiệp vụ hủy.', 'ERD, migration Liquibase, API contract và các luồng tạo/hủy Order.'],
    ['7–8', 'Triển khai manager review/assignment, chat, thỏa thuận, timeout custom, payment và Staff bắt đầu sản xuất.', 'WF02–WF04; JWT/RBAC; WebSocket; SePay và timer nghiệp vụ.'],
    ['9–10', 'Triển khai checkpoint/KCS/rework, yêu cầu thay đổi theo OrderItem và tính deadline sản xuất.', 'WF05–WF06; ảnh tiến độ; version spec; lịch sử quyết định và kiểm thử.'],
    ['11', 'Triển khai bàn giao, nhận hàng, hoàn tất, đánh giá/reorder; hợp nhất OrderLifecycle và cơ chế operations.', 'WF07–WF09; lifecycle BPMN; Outbox/Redis Stream/DLQ và migration cutover.'],
    ['12', 'Tích hợp frontend–backend; kiểm thử end-to-end, bảo mật, đồng thời, migration; hoàn thiện báo cáo và slide.', 'Bản demo; test report; tài liệu triển khai/rollback; báo cáo khóa luận và slide bảo vệ.']
  ];

  const table = body.appendTable(rows);
  table.setBorderWidth(1);
  table.setBorderColor('#000000');
  for (let rowIndex = 0; rowIndex < table.getNumRows(); rowIndex++) {
    const row = table.getRow(rowIndex);
    for (let columnIndex = 0; columnIndex < row.getNumCells(); columnIndex++) {
      const cell = row.getCell(columnIndex);
      cell.setVerticalAlignment(DocumentApp.VerticalAlignment.CENTER);
      if (rowIndex === 0) {
        cell.setBackgroundColor('#d9eaf7');
      }
      const text = cell.editAsText();
      text.setFontFamily(FONT_FAMILY).setFontSize(11);
      if (rowIndex === 0) {
        text.setBold(true);
      }
      const paragraph = cell.getChild(0).asParagraph();
      paragraph.setLineSpacing(1.0);
      paragraph.setSpacingAfter(0);
      paragraph.setAlignment(
        columnIndex === 0 || rowIndex === 0
          ? DocumentApp.HorizontalAlignment.CENTER
          : DocumentApp.HorizontalAlignment.JUSTIFY
      );
    }
  }
}

function appendSignatureTable_(body) {
  const table = body.appendTable([
    ['SINH VIÊN 1\n(Ký và ghi rõ họ tên)', 'SINH VIÊN 2\n(Ký và ghi rõ họ tên)', 'GIẢNG VIÊN HƯỚNG DẪN\n(Ký và ghi rõ họ tên)'],
    ['\n\n\nPhạm Ngọc Tú', '\n\n\n', '\n\n\nThS. Phạm Ngọc Bích']
  ]);
  table.setBorderWidth(0);
  for (let rowIndex = 0; rowIndex < table.getNumRows(); rowIndex++) {
    const row = table.getRow(rowIndex);
    for (let columnIndex = 0; columnIndex < row.getNumCells(); columnIndex++) {
      const paragraph = row.getCell(columnIndex).getChild(0).asParagraph();
      paragraph.setAlignment(DocumentApp.HorizontalAlignment.CENTER);
      paragraph.editAsText().setFontFamily(FONT_FAMILY).setFontSize(12);
      if (rowIndex === 0) {
        paragraph.editAsText().setBold(true);
      }
    }
  }
}

function appendSpacer_(body, count) {
  for (let index = 0; index < count; index++) {
    const paragraph = body.appendParagraph('');
    paragraph.setSpacingAfter(0);
    paragraph.editAsText().setFontFamily(FONT_FAMILY).setFontSize(BODY_FONT_SIZE);
  }
}
