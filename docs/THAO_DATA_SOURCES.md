# Nguồn nội dung và cơ sở chọn bài trên dashboard

## Vì sao xuất hiện bài Grammar số 14?

**Cập nhật:** phần mô tả dưới đây là cách chọn cũ. Dashboard hiện dùng `LessonRecommendationService`: ưu tiên bài PUBLISHED có tiến độ IN_PROGRESS, chưa hoàn thành, được truy cập gần nhất (kể cả bài tự học ngoài chặng). Khi không có, chỉ xét bài tiếp theo của chặng ACTIVE: kỹ năng có mục tiêu đang hoạt động trước; khoảng cách giữa mục tiêu và trình độ đã đánh giá lớn hơn trước; tỷ lệ hoàn thành chặng thấp hơn trước; rồi kỹ năng lâu chưa truy cập. Nếu mọi tiêu chí bằng nhau, dùng ID chặng để kết quả ổn định, không sắp theo tên kỹ năng. Chưa có đánh giá thì không tự gán trình độ hoặc suy đoán kỹ năng yếu. Dashboard hiển thị lý do và tiến độ chặng nếu bài trùng bài tiếp theo của chặng. Không tạo dữ liệu học tập mới trong bước gợi ý.

Trong `StudentDashboardController.dashboard`, hệ thống lấy danh sách chặng từ `LearningPathService.ensureAndSummaries`. `summaries` sắp xếp theo tên kỹ năng. Dashboard dùng `findFirst` để lấy kỹ năng đầu tiên có `nextLessonId`.

Trong mỗi chặng, `summaryOf` đọc `learning_path_items` theo `position`, rồi chọn bài chưa hoàn thành đầu tiên còn ở trạng thái PUBLISHED. Vì tên Grammar & Vocabulary đứng trước Listening, Reading, Speaking, Writing nên nó được chọn khi còn bài.

Khi kiểm tra database local, chặng số 6 của `student@aptis.local` có hai bài: bài số 9 đã COMPLETED; bài số 14 NOT_STARTED. Bởi vậy trang hiển thị bài 14 và 1/2 = 50%. Tỷ lệ này nói về số bài trong chặng, không phải phần trăm năng lực ngữ pháp.

Đây là quy tắc điều hướng đơn giản. Nó không đánh giá bài 14 quan trọng hơn các kỹ năng khác, không so sánh kỹ năng yếu nhất và chưa chọn theo lần học gần nhất. Mục tiêu/Placement được dùng khi tạo chặng; thứ tự chọn giữa các kỹ năng trên dashboard vẫn là thứ tự tên. Không nên mô tả riêng ô này là một đề xuất AI.

## Nguồn các bài đã thêm

- Các bộ nội dung ở V013, V022 và 20 bài ở V024 được trợ lý AI tự biên soạn: mục tiêu, cách làm, ví dụ, phân tích, lỗi thường gặp, bài thực hành, đáp án và từ vựng. Đây không phải bộ đề chính thức lấy từ British Council, PREP hoặc Edulife.
- Nhãn A1–C1 do người biên soạn phân loại theo độ khó dự kiến; chưa có chuyên gia thẩm định mức của từng bài.
- `source_url` là trang tham khảo cấu trúc kỹ năng. Việc có liên kết đó không có nghĩa tác giả nguồn viết hoặc xác nhận toàn bộ bài giảng.
- Các video là video tổng quan từng kỹ năng từ British Council, được nhúng bằng URL YouTube. Cùng kỹ năng có thể dùng lại cùng video; đây chưa phải thư viện video giảng riêng cho từng chủ đề và mức.
- Bốn tệp nghe A2–C1 được tạo từ lời thoại tự biên soạn bằng giọng tổng hợp Microsoft David Desktop trên Windows. Không phải bản ghi người bản ngữ hay âm thanh trích từ đề Aptis chính thức.
- Bốn bài cũ khác được giữ từ database; chỉ thông tin tên nguồn trong database không đủ chứng minh nguồn gốc toàn bộ nội dung.

## Dữ liệu được thêm và lưu ở đâu?

Flyway chạy các file SQL trong `src/main/resources/db/migration` theo phiên bản khi khởi động. V024 thêm bài, câu kiểm tra và bộ từ vựng nếu chưa có; V025 gắn tệp âm thanh vào đúng bài. Chúng không thêm điểm, lượt làm bài hoặc trạng thái hoàn thành cho học viên.

| Loại | Nơi lưu | Cách sử dụng |
|---|---|---|
| Nội dung bài giảng | SQL Server: bảng `lessons` | Controller đọc qua service và đưa sang Thymeleaf |
| Câu kiểm tra, lựa chọn, đáp án, giải thích | Bảng `lesson_checks`, gắn bằng `lesson_id` | Backend chấm và chỉ trả lời giải sau khi nộp |
| Bộ thẻ và thẻ từ | `flashcard_sets`, `flashcards` | Bộ thư viện có `lesson_id`; bộ riêng có `owner_user_id` |
| Video | URL trong `lessons.video_url` | Trình duyệt nhúng YouTube; không có tệp MP4 tải về trong project |
| Âm thanh mới được đóng gói | `src/main/resources/curriculum/audio/listening-a2.wav` đến `listening-c1.wav` | Build đưa vào classpath/JAR; DB lưu đường dẫn tương đối |
| Transcript | `lessons.transcript` | Mở sau khi tự nghe để đối chiếu |
| Hình bài học do Admin tải lên | Mặc định `uploads/lesson-images/<lessonId>/<UUID>.<đuôi>` | DB lưu `image_path`, `image_name` |
| Âm thanh do Admin tải lên | Mặc định `uploads/lesson-audio/<lessonId>/<UUID>.<đuôi>` | DB lưu `audio_path`, `audio_name` |
| Bản ghi Speaking của học viên | `uploads/lesson-recordings/<userId>/<lessonId>/<UUID>.<đuôi>` | `lesson_responses` lưu đường dẫn, gắn đúng người học |
| Tiến độ và kết quả học | `lesson_progress`, `learning_path_items`, `lesson_check_attempts`, `user_flashcard_progress` | Phát sinh từ thao tác học được ghi nhận, tách khỏi nội dung bài giảng |

Thư mục upload gốc có thể đổi bằng `app.storage.upload-dir`. `LocalStorageService` đặt tên UUID để tránh đè tệp trùng tên. File được phát qua endpoint kiểm quyền như `/lesson-media/{id}/audio`, `/lesson-media/{id}/image`; bản ghi cá nhân qua `/student/lessons/{id}/recording`.

Ở lần kiểm tra dữ liệu local cho yêu cầu này, không có bài nào có `image_path`: tôi chưa bổ sung ảnh thật cho các bài mới. Chức năng upload ảnh có sẵn không đồng nghĩa mỗi bài đã được thêm ảnh. Biểu tượng trên thẻ là chi tiết giao diện, không phải ảnh nội dung bài học.

Bản nội dung biên soạn ở `docs/content/curriculum-a1-c1.json`; script dựng âm thanh ở `scripts/build-curriculum-audio.ps1`. Các migration đã áp dụng không nên sửa trực tiếp; thay đổi dữ liệu tiếp theo cần qua Admin hoặc migration phiên bản mới.

## Bố trí luyện tập và bộ lọc mới

- Reading: câu hỏi đọc hiểu và ghi chú.
- Grammar & Vocabulary: bài tập vận dụng ngữ pháp, từ vựng.
- Listening: yêu cầu → máy phát → câu hỏi nghe hiểu → transcript và ghi chú.
- Writing: câu hỏi ôn phương pháp ở Cách làm; phần luyện tập chính là bài viết, bài mẫu và tự rà soát.
- Speaking: câu hỏi ôn phương pháp ở Cách làm; phần luyện tập chính là ghi âm, dàn ý, nghe lại và tự đánh giá.
- Flashcard: `source=all`, `source=mine`, `source=lesson` tương ứng Tất cả, Của tôi, Theo bài học. Bộ lọc thực hiện ở server trên các bộ mà tài khoản được phép xem.
