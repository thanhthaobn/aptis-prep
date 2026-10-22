# Kiểm thử mẫu bài học — 22/10/2026

## Phạm vi và dữ liệu thật

Nhập bằng giao diện quản trị; kiểm tra bằng tài khoản học viên demo trên
localhost:8080, kết nối SQL Server `aptisprep_db`. Không nhập dữ liệu trực tiếp
vào DB. Các bài thử vẫn được công bố và có tiền tố `[QA 22-10]`:

| ID | Kỹ năng | Kết quả kiểm thử trực tiếp |
| --- | --- | --- |
| 35 | Reading | Ảnh và video tải lên được. Nộp 1/1 đúng, lịch sử lưu lựa chọn và giải thích. |
| 36 | Grammar & Vocabulary | Ảnh và video hợp lệ. Nộp 1/1 đúng, lịch sử xem lại được. |
| 37 | Listening | WAV 10,20 giây phát được; transcript khóa trước khi nghe và mở sau khi nghe đủ. Nộp 1/1 đúng. |
| 38 | Writing | Ảnh/video xem được. Nộp bài 32 từ trong khoảng 20–60; nội dung được lưu và xem lại sau khi tải lại. |
| 39 | Speaking | Đề, ảnh/video và bộ ghi âm hiển thị. Chưa xác nhận ghi/lưu bản ghi thực tế: trình duyệt kiểm thử chờ quyền micro, không có hộp cấp quyền để thao tác. |

Media QA tự tạo: PNG 960×540, video VP8 WEBM 6 giây, WAV giọng tổng hợp.
Các tệp và ảnh chụp bằng chứng nằm trong `target/qa-lesson-smoke/` (không bền
qua `mvn clean`). Video được trình duyệt giải mã 960×540, readyState=4.

## Lịch sử sau chỉnh sửa

- Đổi nội dung các bài 35–38 từ marker V1 sang V2 qua giao diện quản trị.
- Reading còn đổi đáp án đúng từ 9:00 sang 10:00 và bỏ video khỏi bài hiện tại.
- Mở lại lịch sử phía học viên: cả bốn vẫn giữ đề V1, yêu cầu lúc nộp và câu trả lời cũ.
- Video Reading trong lịch sử vẫn phát được, dù bài hiện tại không còn video.
- Truy vấn chỉ đọc SQL Server xác nhận attempts 5/6/7 của các bài 35/36/37
  đều 1/1 và có content_snapshot V1; submission 1 của Writing 38 lưu 173 ký tự
  và content_snapshot V1. Không có submission Speaking của bài 39 tại lúc kiểm tra.

## Lỗi đã sửa khi kiểm thử

1. Form quản trị thiếu model `profile`, khiến trang tạo bài lỗi khi đọc `mediaHint`.
2. Khôi phục minWords/maxWords trong bản chụp đề, giữ đúng yêu cầu Writing lúc nộp.
3. Listening tự reload sau khi nghe hết làm mất đáp án đang chọn. Nay chờ lưu trạng
   thái nghe thành công và không reload khi đang chọn đáp án; lần nộp sẽ mở transcript.
   Đã nghe lại đủ audio và xác nhận lựa chọn vẫn còn sau sự kiện ended.
4. Speaking không hiện thanh audio rỗng trước khi có bản ghi; có trạng thái rõ khi
   đang chờ quyền micro, chặn yêu cầu lặp và giải phóng stream nếu khởi tạo ghi thất bại.

## Bố cục và kiểm thử hồi quy

- Kiểm tra ô nhập, giới hạn từ và hàng đáp án ở viewport 1265px; hàng đáp án
  chuyển về một cột khi kiểm tra viewport 390px, không chồng nhau.
- Kiểm tra ảnh/media phía học viên trên cả 5 bài; không phát hiện tràn ngang.
- 59 kiểm thử Java qua (0 failures/errors); sau sửa template Speaking chạy lại
  11 SimpleLessonRulesTests cũng qua. Speaking lưu DB và phân quyền media đã được
  kiểm thử tích hợp tự động trên H2, không thay thế xác nhận micro thực tế.
- 4 kiểm thử JavaScript hồi quy Listening qua:
  `node --test src/test/js/lesson-player.test.cjs`.

## Còn cần xác nhận

Học viên mở `/student/lessons/39`, cho phép micro, ghi khoảng 5 giây, dừng và lưu.
Sau đó cần xác nhận bản ghi nghe được trong lịch sử và hàng `lesson_work_submissions`
trong SQL Server; chưa kết luận luồng này đã đạt kiểm thử trực tiếp.

Ứng dụng kiểm thử dùng JVM argument `-Djdk.net.unixdomain.tmpdir=D:\HSF302_FPT\PROJECT\target`
để tránh lỗi đường dẫn AF_UNIX quá dài của môi trường hiện tại. Không thay đổi cấu hình
hệ thống hay JDK của người dùng.
