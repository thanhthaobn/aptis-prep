# Bàn giao giao diện quản trị nội dung

> Phân công 20/10/2026: Thông phụ trách Học viên, Chứng chỉ/Chấm điểm và grading; Thảo phụ trách Bài học/Flashcard cùng lịch sử/kết quả; Trâm phụ trách Câu hỏi/Đề thi, Placement và engine attempt. Xem [TEAM_ASSIGNMENT_V2.md](TEAM_ASSIGNMENT_V2.md).

Mục Cấu hình cũ đã đổi thành **Chứng chỉ & Chấm điểm**, không còn trang trạng thái OpenAI. UI UC-A03–A05 và phân công Người 1 xem [SCORING_UI_HANDOFF.md](SCORING_UI_HANDOFF.md). Các mô tả Cấu hình/settings cũ bên dưới được thay bằng luồng mới này; settings chỉ còn redirect.

> Quyết định mới: đã bỏ nhãn “minh họa”/“mẫu” trên FE theo yêu cầu chủ project; hướng dẫn giữ nhãn cũ bên dưới không còn áp dụng. Fixture chưa được thay bằng DB, POST vẫn chưa triển khai, không coi giao diện không nhãn là nghiệp vụ hoàn tất.

## Phạm vi hiện có

Trang `/admin/settings` redirect sang Chứng chỉ & Chấm điểm (AdminSettingsPreviewController, `admin/settings/*`). Chỉ UI fixture, không gọi AI, đọc key/datasource hoặc lưu properties. Người 1 nối quản lý chứng chỉ/scoring theo V2; cấu hình kỹ thuật vẫn qua file local/biến môi trường và người giữ nền review.

Đã dựng giao diện GET dùng dữ liệu minh họa, theo theme teal của Admin. Không đọc/ghi bảng nghiệp vụ, không gọi OpenAI, không upload file lên server và không lưu chỉnh sửa. Nhãn “Dữ liệu minh họa” phải giữ đến khi thay bằng dữ liệu thật.

Đã đồng bộ danh sách, chi tiết và form với `learning-theme.css`: chữ gốc 18px, nhãn form/bảng 1rem, dữ kiện chính 1.15rem; mô tả dài không lặp lại ở danh sách. Mô tả bài học/đề thi nằm trong mục bấm mở ở chi tiết. Hàng thao tác xuống dòng riêng để không tràn tiêu đề. Giữ email học viên, cảnh báo khóa và giới hạn chấm Speaking. Bảng cuộn ngang khi cần; không thu nhỏ chữ để ép cột.

| Khu vực | Route danh sách | Chi tiết mẫu | Tạo / sửa | Người tiếp nhận |
|---|---|---|---|---|
| Bài học / Flashcard | `/admin/lessons` | `/admin/lessons/2001` | `/admin/lessons/create`, `/admin/lessons/2001/edit` | 2 |
| Ngân hàng câu hỏi | `/admin/questions` | `/admin/questions/3001` | `/admin/questions/create`, `/admin/questions/3001/edit` | 3 |
| Đề thi | `/admin/exams` | `/admin/exams/4001` | `/admin/exams/create`, `/admin/exams/4001/edit` | 3 |
| Kết quả | `/admin/results` | `/admin/results/5001` | Chỉ xem, không sửa điểm hoặc tạo kết quả | 4 |

Học viên tiếp tục dùng AdminStudentController và StudentPreviewService do Người 1 tiếp nhận theo STUDENT_MANAGEMENT.md.

## Danh sách

- Tìm tên/nội dung; riêng kết quả tìm học viên/email/bài thi.
- Lọc kỹ năng, loại nội dung/đề và trạng thái; chạy trên fixture bằng form GET, không REST.
- Có trường hợp không có kết quả và nút đặt lại.
- Fixture có 3 bản ghi mỗi khu vực, chưa có phân trang dữ liệu thật.
- Sidebar active tương ứng lessons/questions/exams/results; dashboard đã liên kết sang trang thực.

## Chi tiết và form

- Bài học: thông tin, các mục nội dung, tài nguyên; ví dụ Flashcard mặt trước/sau ở bản ghi 2002.
- Câu hỏi: kỹ năng/cấp độ, nội dung, ý hỏi, phương án và nhãn đáp án đúng cho Admin; vùng tài nguyên.
- Đề: chứng chỉ/bộ chấm, loại/target, cấu trúc kỹ năng/câu và thời gian.
- Kết quả: hồ sơ lượt thi, điểm ước lượng từng kỹ năng, nhận xét, ca chờ chấm và chấm lỗi. Chưa có dữ liệu thì không gán 0.
- Form tạo/sửa có input/select/textarea, vùng nội dung theo module, Flashcard/phương án trả lời/chọn câu và file picker.
- Nút Lưu/Công bố/Lưu trữ/Khôi phục disabled; không có POST. File picker chỉ chọn file trên trình duyệt, chưa gửi file.
- Form đang dùng onsubmit để ngăn gửi trong bản preview. Khi nối nghiệp vụ phải bỏ chặn, đặt th:action, method POST, Form DTO/BindingResult/CSRF, multipart khi upload và redirect sau thành công.
- Form câu hỏi là bố cục tham khảo với một nhóm phương án; còn phải làm editor nhiều item, matching/ordering, giới hạn từ/thời gian theo từng loại. Form đề còn phải làm tìm/chọn/sắp xếp câu hỏi thật. Không coi form preview là hoàn thành nghiệp vụ.
- Các ô form đặc thù chưa bind đầy đủ nội dung fixture; trạng thái và dữ liệu phải lấy từ DTO thật khi triển khai edit.

## File cần tiếp nhận

| File | Vai trò |
|---|---|
| `controller/admin/AdminCatalogPreviewController.java` | Route GET cho cả bốn khu vực |
| `service/admin/AdminCatalogPreviewService.java` | Fixture và tìm/lọc trong bộ nhớ |
| `dto/view/AdminCatalogPreview.java` | Record trình bày fixture, không phải Entity |
| `templates/admin/catalog/list.html` | Danh sách/bộ lọc chung |
| `templates/admin/catalog/detail.html` | Chi tiết, các khối theo module |
| `templates/admin/catalog/form.html` | Form giao diện theo module |
| `static/css/components.css` | Style prefix catalog-/admin-catalog |

Khi nhóm bắt đầu code song song, ưu tiên tách các template theo thư mục `admin/lesson`, `admin/question`, `admin/exam`, `admin/result` và Controller theo luồng, giữ fragment/component chung. Đăng ký chuyển route trong cùng PR để không trùng mapping với PreviewController. Không tạo Controller thứ hai cùng GET route rồi để cả hai chạy.

Người 2 và 3 cần cùng chốt form Flashcard: bản preview gộp trong khu vực Bài học để dễ xem, có thể tách route/form quản lý thẻ thuộc Lesson theo V2. Không tạo bảng hoặc Entity mới chỉ vì fixture có thuộc tính trình bày.

Sau khi thay fixture, bỏ service preview khỏi luồng chạy, bỏ nhãn minh họa tại trang đó và thay test bằng dữ liệu kiểm thử thật. Không chuyển DTO preview thành Entity hoặc lưu các điểm minh họa vào database.

## Kiểm tra bàn giao

- AdminCatalogPreviewTests kiểm tra render danh sách, các chi tiết mẫu và form tạo/sửa, tìm/lọc, trạng thái trống và chặn Student.
- Chưa xác nhận các nghiệp vụ tạo/sửa/publish/chấm điểm vì chưa có implementation.
- Tuân UI_GUIDE.md, V2 và phân công TEAM_ASSIGNMENT_V2.md khi nối nghiệp vụ.
