# Bàn giao giao diện học viên

> Phân công mới nhất 17/10/2026: Người 4 sở hữu đề/engine và toàn bộ lịch sử/kết quả học viên; Người 1 sở hữu tác vụ/đánh giá AI và phản hồi tự luận chi tiết; Người 3 nhận Placement, dùng engine 4 và service ghi Profile 1; Người 2 giữ Bài học/Flashcard/lộ trình/checkpoint. Phương án chuyển màn kết quả sang Người 1 không còn áp dụng. Xem [TEAM_ASSIGNMENT_V2.md](TEAM_ASSIGNMENT_V2.md); tách mapping/file preview chung trước khi sửa.

> Quyết định mới: chủ project yêu cầu bỏ nhãn “minh họa”/“mẫu” khỏi FE. Các yêu cầu giữ nhãn cũ trong tài liệu được thay thế; dữ liệu Preview vẫn cố định và không phải tiến độ/lịch sử thật. Giữ nút chưa có nghiệp vụ disabled, kiểm checklist REAL_PROGRESS_CONTRACT trước khi nghiệm thu dữ liệu thật.

## Phạm vi đã dựng

Thymeleaf MVC, theme xanh lá, Segoe UI, chữ gốc 18px; dùng navbar, sidebar, breadcrumb và footer chung. Nhãn “Dữ liệu minh họa” luôn hiển thị. Không giả lập tiến độ thật, không yêu cầu mục tiêu hoặc Placement để xem trang.

| Trang | Danh sách | Chi tiết mẫu | Người tiếp nhận |
|---|---|---|---|
| Bài học | `/student/lessons` | `/student/lessons/6001` | 2 |
| Flashcard | Nằm trong Bài học | `/student/lessons/6002` | 2 |
| Đề thi | `/student/exams` | `/student/exams/7001` | 3 cung cấp đề; 4 tích hợp lượt làm |
| Kết quả | `/student/results` | `/student/attempts/8001/result` | 4 |

Lớp giao diện: `StudentContentPreviewController`, `StudentContentPreviewService`, `StudentContentPreview` và `templates/student/content/list.html`, `detail.html`. Đây là DTO/fixture riêng, không sử dụng dữ liệu admin. ID 6001–6005, 7001–7003, 8001–8003 chỉ dùng xem layout, không tạo bản ghi tương ứng trong DB.

## Những gì hoạt động hiện tại

Đề thi và Kết quả đã đồng bộ với Bài học qua `compact-catalog`: font/card/nút gọn và palette skill áp dụng cả danh sách/chi tiết. Đề Tổng hợp trung tính, từng phần dùng màu kỹ năng riêng. Không thay đổi fixture, quyền hoặc nghiệp vụ chấm/làm bài.

Cập nhật màu mới theo chủ project: Reading xanh, Listening vàng, Writing/Speaking tím, Flashcard đỏ. Áp dụng chung trang Khám phá, thẻ danh sách Bài học và chi tiết (data-skill/data-kind). Thẻ khám phá nhỏ gọn, chữ chính lớn hơn; phối màu cũ bên dưới đã được thay thế.

Đã bổ sung Writing `/student/lessons/6004` và Speaking `/student/lessons/6005`: nội dung hướng dẫn theo bước, ô Writing gõ thử không lưu, Speaking chỉ khung ghi âm disabled (không xin quyền micro). Các nút gửi/chấm/hoàn thành vẫn disabled. Người 2 tiếp nhận nội dung/UI; Người 4 tích hợp lưu bài/ghi âm/chấm khi có luồng hợp lệ. ID bài học hiện 6001–6005; không phải bản ghi DB.

Khám phá dùng thẻ màu theo kỹ năng (Reading tím, Listening xanh trời, Writing hồng, Speaking cam, Grammar & Vocabulary xanh lá), icon SVG rõ nét và liên kết lọc skill giữ nguyên. Không đổi dữ liệu để lấp sai kỹ năng, không tạo attempt từ GET.

- Menu Bài học, Đề thi, Kết quả mở được; trạng thái menu theo trang.
- Form GET tìm kiếm không phân biệt dấu; lọc skill/level cho bài học, đề thi; skill/state cho kết quả. Có trạng thái tìm không thấy và nút đặt lại.
- Bài học có nội dung mẫu; Flashcard bấm mở nghĩa bằng `details`, không lưu lượt ôn.
- Chi tiết đề chỉ có cấu trúc/thời gian và hướng dẫn; không lộ đáp án, không tạo attempt.
- Kết quả có ví dụ đã chấm, chờ chấm, lỗi chấm; không lấy thông tin người khác, không tính điểm hoặc gọi OpenAI.
- Nút hoàn thành, bắt đầu làm bài, chấm lại bị vô hiệu hóa. Không có POST mới, không tự lưu bất cứ dữ liệu nào. Không có audio, micro, timer hay nộp bài hoạt động.

## Người 2: bài học, Flashcard và tiến độ

Thay fixture bằng nội dung đã công bố. Hoàn thiện media, nội dung học và ôn Flashcard. Tích hợp `POST /student/lessons/{id}/complete` với CSRF, kiểm tra quyền học và cập nhật tiến độ theo V2. Chỉ ghi nhận hoàn thành khi đủ điều kiện. Tách tiến độ bài học khỏi CEFR ước lượng. Dữ liệu rỗng hiển thị trạng thái chưa bắt đầu, không thay bằng phần trăm mẫu.

## Người 3 và 4: đề và lượt thi

Người 3 cung cấp dữ liệu đề đã công bố, cấu trúc phần thi và câu hỏi hợp lệ; không cung cấp bản nháp/đáp án cho học viên. Người 4 nối `POST /student/exams/{id}/start`, `/student/attempts/{id}`, lưu đáp án, nộp bài, kết quả. Server kiểm soát thời gian và hai lượt nghe; tắt nút trên UI không thay thế kiểm tra server. Không triển khai bằng REST frontend; dùng form MVC.

## Người 4: kết quả toàn bài; Người 1: AI và phản hồi tự luận

Người 4 làm Controller/service/template lịch sử/kết quả; mọi attempt kiểm owner/admin, có phân trang/chờ/lỗi/rỗng, điểm chưa chấm không thành 0. Người 1 tích hợp provider OpenAI, giữ tác vụ/đánh giá/retry và trang/fragment phản hồi tự luận; không key ra HTML, không có trang chấm mẫu Admin. Speaking qua transcript phải ghi rõ chưa đánh giá phát âm/ngữ điệu. Người 4 gửi yêu cầu sau lưu/khóa bài, nhận assessment để quy đổi/tổng hợp và là người duy nhất đổi trạng thái attempt/skill result. Chấm lại kiểm quyền/trạng thái/version/idempotency theo SERVICE_CONTRACTS.md.

## Cách tiếp nhận

Người 1/2/3/4 tiếp nhận quy tắc làm lại đầu vào tại [PLACEMENT_RETAKE_POLICY.md](PLACEMENT_RETAKE_POLICY.md). Trang Placement sau này đổi CTA theo lượt đang làm/chờ chấm/lỗi/đã kết thúc; hiện mới có giao diện, chưa giới hạn hoặc tạo attempt. Kết quả đầu vào mới không tự hạ mức checkpoint đã đạt.

Đã nối các mục Khám phá lộ trình sang danh sách Bài học theo đúng skill; Writing và Speaking tách lựa chọn. Dashboard đổi theo Goal/path vẫn chưa có nghiệp vụ. Người 1/2/3/4 tiếp nhận theo [DASHBOARD_STATES.md](DASHBOARD_STATES.md), gồm bảng trạng thái/CTA và ca kiểm thử bắt buộc.

Hồ sơ `/student/profile` và đầu vào `/student/placement` đã có GET preview trong StudentAccountPreviewController và `student/account/*`: Người 1 nhận Hồ sơ, Người 3 nhận Placement. Tách mapping/file chung trước khi làm song song, không tạo route trùng. Hồ sơ chỉ đọc email principal, chưa lấy tên/Goal/Profile DB; form lưu/bắt đầu Placement disabled. Nút bỏ qua mở khám phá, chưa sinh path thật. Contract: [SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md).

Bắt buộc theo [REAL_PROGRESS_CONTRACT.md](REAL_PROGRESS_CONTRACT.md): công thức C/N, làm tròn xuống, bài bắt buộc/tùy chọn, điều kiện checkpoint, chuyển path, DTO dùng chung và checklist bỏ nhãn minh họa. Người 2 là đầu mối tiến độ; Người 1/3/4 cung cấp nguồn dữ liệu theo phân công trong tài liệu đó.

Thay controller/service preview khi nối nghiệp vụ; không thêm mapping trùng. Giữ class UI, menu và route contract. Thêm kiểm thử quyền sở hữu, bản nháp, truy cập trực tiếp, CSRF và các trạng thái rỗng/lỗi. Chỉ bỏ nhãn khi toàn bộ dữ liệu trong phạm vi nhãn đó đã là thật; trang trộn dữ liệu thật/mẫu phải gắn nhãn rõ từng vùng mẫu. Thanh CEFR/lộ trình nằm ở tổng quan; màn hình làm bài dùng tiến độ câu hỏi và thời gian, không dùng 68% mẫu.
