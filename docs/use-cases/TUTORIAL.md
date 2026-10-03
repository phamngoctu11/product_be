# Hướng dẫn tạo tài liệu và sơ đồ cho từng workflow

Đọc file này trước mỗi lần tạo hoặc cập nhật tài liệu workflow. Mục tiêu là mỗi luồng có một thư mục tự đầy đủ, dễ bàn giao cho dev và chèn vào báo cáo.

## 1. Nguồn nghiệp vụ

1. Đọc [target.txt](../target.txt), ưu tiên yêu cầu người dùng đã chốt mới nhất.
2. Xác định workflow theo mục E, không nhầm số thứ tự chức năng Fxx với WFxx.
3. Xác định điểm bắt đầu, kết thúc, tác nhân, các UC và những bước thuộc workflow kế tiếp.
4. Dùng code để đối chiếu hiện trạng; không lấy hành vi code chưa cập nhật để thay yêu cầu mục tiêu.
5. Ghi rõ điểm chưa quyết định. Không tự thêm quyền, thanh toán, timer, review hoặc trạng thái chưa được thống nhất.

## 2. Cấu trúc bắt buộc

```text
docs/
├── target.txt
└── use-cases/
    ├── TUTORIAL.md
    ├── README.md
    └── WF01-custom-request/
        ├── README.md
        ├── usecase.md
        ├── diagram.md
        ├── sequence.md
        ├── overview.svg
        └── overview.puml
```

- Mỗi workflow một folder: `WF<2 chữ số>-<tên-luồng-kebab-case>`; ví dụ `WF02-accept-order`.
- Giữ tên file bên trong giống nhau cho mọi workflow.
- `overview.svg` và `overview.puml` nằm trực tiếp trong folder luồng. Không đặt overview vào một folder diagrams chung ngoài luồng.
- Không tạo folder cho luồng chưa thực hiện chỉ để chứa file rỗng.
- Không để hai bản đặc tả cùng workflow ở hai đường dẫn gây nhầm nguồn chính.

## 3. Nội dung từng file

### README.md của luồng

Ghi tên, mục tiêu ngắn, nguồn `../../target.txt` và link mở tất cả file trong luồng. Nhúng `overview.svg` để xem ngay. Nêu cách mở SVG bằng trình duyệt và xem Markdown ở chế độ preview.

### usecase.md — Đặc tả nghiệp vụ

Phải có:

- Mã/tên workflow, phạm vi, điểm bắt đầu và kết thúc.
- Danh mục UC, tác nhân và quyền.
- Với từng UC: mục tiêu, kích hoạt, tiền điều kiện, hậu điều kiện thành công và bảo đảm khi thất bại.
- Bảng luồng chính: bước / hành động tác nhân / phản hồi hệ thống.
- Luồng thay thế và ngoại lệ, gắn với bước cụ thể.
- Đầu vào, đầu ra, validation và quy tắc nghiệp vụ.
- Trạng thái/event phát sinh, đối tượng nhận thông báo, thời điểm timer nếu có.
- Tiêu chí nghiệm thu, gồm quyền truy cập, yêu cầu lặp và lỗi bất đồng bộ nếu liên quan.
- Link đến diagram.md, sequence.md, overview.svg và overview.puml.

Không dùng “như cũ”, “giữ nguyên” thay cho nội dung cần dev hiểu. Viết hành vi cụ thể. Tên lớp/API chưa có trong code phải ghi là thiết kế đề xuất.

Đánh mã: WF01 dùng UC01.1, UC01.2...; WF02 dùng UC02.1... Mã giống nhau trong đặc tả và mọi sơ đồ.

### overview.svg — Use Case Diagram để xem/chèn báo cáo

Vẽ đúng kiểu mẫu người dùng yêu cầu:

- Actor hình người ở ngoài khung hệ thống.
- Use case hình oval bên trong khung.
- Actor nối với chức năng mà họ thực sự tham gia bằng đường liên kết.
- Tiêu đề có mã/tên workflow; nền trắng, chữ tiếng Việt dễ đọc.
- Không đưa database/service/Camunda thành actor bên ngoài khi chúng thuộc hệ thống.
- Không đưa manager vào workflow tạo bản nháp chỉ vì manager sẽ duyệt ở workflow tiếp theo.
- Không dùng include/extend để biểu diễn trình tự. Chỉ dùng khi có quan hệ UML đúng và được giải thích.
- Bố trí không chồng chữ, đường nối hoặc ghi chú. SVG phải mở được độc lập, không phụ thuộc font/ảnh tải từ dịch vụ ngoài.

### overview.puml — Mã nguồn UML

Lưu sơ đồ tương đương bằng PlantUML để dễ chỉnh sửa/tái xuất. Actor, use case, quan hệ và phạm vi phải khớp SVG; bố cục có thể khác.

Nếu có công cụ PlantUML, có thể xuất SVG từ nguồn. Nếu dựng SVG trực tiếp, vẫn cập nhật `.puml` tương ứng và kiểm tra hai bản cùng nội dung. Không nói SVG được sinh từ PlantUML nếu thực tế vẽ trực tiếp.

### diagram.md — Sơ đồ hoạt động

- Nhúng overview.svg và dẫn nguồn overview.puml.
- Mỗi UC có một Activity/flowchart Mermaid: bắt đầu, điều kiện quyền, luồng chính, nhánh lỗi/thay thế, kết quả.
- Có thể thêm Activity nối các UC trong workflow.
- Phân biệt rõ Activity Diagram với Use Case Diagram; hình chữ nhật/mũi tên luồng không thay sơ đồ actor/oval.
- Không giữ một bản đầy đủ của cùng Activity trong usecase.md; dùng liên kết để tránh sửa lệch.

### sequence.md — Biểu đồ tuần tự

- Mỗi UC có một sequence Mermaid, cùng mã với đặc tả.
- Actor, giao diện, controller/service/repository, DB, workflow và event consumer tùy mức cần thiết.
- Tên participant kỹ thuật chưa có trong code phải ghi là đề xuất.
- Thể hiện kiểm tra quyền, đọc/ghi, transaction và kết quả trả về.
- Dùng alt/opt/loop khi có nhánh thực sự trong đặc tả.
- Phân biệt thao tác đồng bộ với xử lý sau commit; email không buộc API phải chờ.
- Có nhánh rollback, gửi trùng hoặc retry khi ảnh hưởng kết quả nghiệp vụ.
- Dùng một thông điệp “transaction ...” với ghi chú hoặc nhóm rõ nếu sơ đồ đã lược chi tiết; không khiến người đọc hiểu các bảng được commit rời rạc.

## 4. Các bước tạo một workflow

1. Đọc hướng dẫn này và phần target liên quan.
2. Tạo folder có mã/tên luồng đúng quy tắc.
3. Viết usecase.md trước để chốt UC, actor và ranh giới.
4. Vẽ overview.puml và overview.svg tương ứng, có actor hình người/oval như mẫu.
5. Viết diagram.md cho từng UC, thêm Activity tổng thể nếu hữu ích.
6. Viết sequence.md cho từng UC, dựa trên luồng chính/ngoại lệ đã đặc tả.
7. Tạo README.md của luồng và cập nhật chỉ mục ../README.md.
8. Kiểm tra link, mã UC, trạng thái, timer, sự kiện, quyền và các điều kiện trong cả sáu file.
9. Bàn giao link folder/README và overview.svg để người dùng xem ngay.

## 5. Quy tắc chỉnh sửa và kiểm tra

- Khi người dùng sửa nghiệp vụ, cập nhật tất cả file bị ảnh hưởng, không chỉ target hoặc một hình.
- Nếu chuyển đường dẫn, cập nhật link tương đối; chỉ chuyển file thuộc nhiệm vụ, không xóa tài liệu không liên quan.
- Đường dẫn nguồn trong folder luồng: `../../target.txt`.
- Link giữa các file cùng folder: `usecase.md`, `diagram.md`, `sequence.md`, `overview.svg`, `overview.puml`.
- Đặc tả: mọi UC phải có actor, bước xử lý, đầu ra và lỗi rõ.
- Activity và sequence: phải có đủ các UC trong bảng đặc tả.
- Overview: chỉ actor/chức năng trong phạm vi, không biến sơ đồ thành bản đồ lớp kỹ thuật.
- Kiểm tra cú pháp XML của SVG, số cặp code fence Markdown và đường dẫn file.
- Mở SVG bằng Chrome/Edge để xem bố cục; khi có công cụ render Mermaid/PlantUML, kiểm tra thêm sơ đồ được render. Nếu chưa render, nói đúng mức kiểm tra đã thực hiện.
- `git diff --check` để phát hiện lỗi khoảng trắng. Không cần chạy test backend khi chỉ sửa tài liệu.

## 6. Cách xem

- SVG: mở trực tiếp trong Chrome/Edge, có thể dùng Ctrl+O rồi chọn overview.svg.
- Markdown: dùng chế độ preview hỗ trợ Mermaid để xem diagram.md/sequence.md thành hình.
- PlantUML: mở overview.puml bằng công cụ hỗ trợ PlantUML; đây là nguồn chỉnh sửa, không phải ảnh có sẵn.
- Nếu cần PNG/PDF để chèn Word, xuất từ SVG hoặc công cụ render; không đổi đuôi `.svg` thành `.png`.

## 7. Checklist bàn giao

```text
[ ] Folder đúng mã và tên workflow.
[ ] Có README.md, usecase.md, diagram.md, sequence.md.
[ ] Có overview.svg và overview.puml ngay trong folder luồng.
[ ] Mọi UC có đặc tả, Activity và Sequence cùng mã.
[ ] Overview có tác nhân hình người, chức năng oval, khung hệ thống.
[ ] UML không dùng include/extend thay cho thứ tự xử lý.
[ ] Trạng thái/quyền/timer/event khớp target và yêu cầu đã chốt.
[ ] Các link tương đối trỏ tới file tồn tại.
[ ] Đã cập nhật chỉ mục docs/use-cases/README.md.
[ ] Ghi đúng những gì đã kiểm tra và điểm chưa quyết định.
```
