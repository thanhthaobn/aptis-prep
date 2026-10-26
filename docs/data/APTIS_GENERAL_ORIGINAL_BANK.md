# Ngân hàng Aptis General tự biên soạn

`aptis-general-original-practice.sql` tạo một bộ đề luyện tập theo cấu trúc Aptis General. Nội dung trong file là nội dung mới do dự án tự biên soạn, không phải đề thi chính thức của British Council.

## Phạm vi nội dung

| Phần | Dạng bài trong seed | Thời lượng tham chiếu |
| --- | --- | --- |
| Grammar & Vocabulary | Hoàn thành câu và dùng từ trong ngữ cảnh | 25 phút |
| Reading | Sentence comprehension, text cohesion, opinion matching, long text comprehension | 35 phút |
| Listening | Information recognition, information matching, discussion inference, longer monologue inference | 40 phút |
| Writing | Word-level, short text, ba phản hồi ngắn, email thân mật và trang trọng | 50 phút |
| Speaking | Thông tin cá nhân, mô tả ảnh, so sánh ảnh, thảo luận chủ đề trừu tượng | 12 phút |

Script tạo một Full Mock và năm đề luyện theo kỹ năng. Script có thể chạy lại mà không tạo bản ghi trùng theo nội dung, kỹ năng, phần thi và dạng câu hỏi.

## Media cần bổ sung

Audio và ảnh không được tạo giả trong script. Bốn transcript Listening được lưu trong bảng tạm `@mediaDefinitions` cùng các đường dẫn:

- `uploads/curriculum/audio/aptis-original-listening-part-1.wav`
- `uploads/curriculum/audio/aptis-original-listening-part-2.wav`
- `uploads/curriculum/audio/aptis-original-listening-part-3.wav`
- `uploads/curriculum/audio/aptis-original-listening-part-4.wav`

Mặc định `@mediaReady = 0`, script tạo media ảo dùng transcript và giọng đọc tiếng Anh của trình duyệt nên Listening vẫn làm được. Chỉ đổi thành `1` sau khi đã upload đúng các file WAV để dùng player audio thật. Phần Speaking Part 2 và Part 3 hiện có prompt gốc nhưng chưa gắn ảnh vì repository chưa có ảnh được cấp phép.

## Cách nhập dữ liệu

1. Đảm bảo database đã chạy schema hiện tại và có certificate cùng scoring scheme ở trạng thái `ACTIVE`.
2. Chạy file SQL bằng tài khoản có quyền ghi vào các bảng `questions`, `question_items`, `question_options`, `question_media`, `mock_exams`, `mock_exam_sections` và `mock_exam_questions`.
3. Kiểm tra các đề mới ở khu vực quản trị trước khi cho học viên làm.
4. Khi đã có audio và ảnh, bật `@mediaReady` rồi chạy lại script để tạo media row còn thiếu.

## Đối chiếu format

Format và thời lượng tham chiếu được đối chiếu từ trang chuẩn bị Aptis ESOL General và tài liệu hướng dẫn giáo viên của British Council. Bộ seed này mô phỏng dạng nhiệm vụ và ngữ cảnh, không tuyên bố là đề chính thức hay dùng cho kỳ thi thật.
