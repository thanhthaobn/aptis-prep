# Bài làm và media bài học — 22/10/2026

## Cập nhật 23/10/2026 — V039

- Listening bắt buộc được server xác nhận đã nghe ít nhất 90% nội dung audio. Server đọc thời lượng từ file; không dùng duration/listened của học viên. Phiên nghe gắn user/lesson/file, có token, thứ tự request và hạn sử dụng; các đoạn được hợp nhất, không cộng trùng khi nghe lặp. Tua không được tính, thời gian được cộng không vượt thời gian server. Giao diện xác thực ở tốc độ 1x và chờ cấp phiên trước khi phát.
- Endpoint audio-played cũ từ chối đánh dấu trực tiếp. Lesson hoàn thành và transcript AFTER_LISTEN chỉ dùng verified_audio_path khớp audio hiện tại. Dấu nghe legacy được giữ nguyên trong DB nhưng không được coi là bằng chứng xác thực mới; Lesson Progress và lịch sử hoàn thành cũ không bị xóa.
- V039 bổ sung 10 cột playback/verification vào lesson_engagement, không thêm bảng và không xóa dữ liệu cũ. Đồng thời thay constraint correct_option để cho phép 0–5, phù hợp A–F; service vẫn kiểm số lựa chọn thực tế.
- Metadata MP3/WAV/M4A/OGG được đọc phía Java khi hỗ trợ; WEBM hoặc file không đọc được cần ffprobe trên PATH hoặc biến FFPROBE_PATH. Không đọc được thời lượng thì báo lỗi và không tự mở khóa. Tài nguyên trong JAR được đọc qua file tạm riêng rồi dọn, không sửa file media nguồn.
- Trình duyệt vẫn báo vị trí/trạng thái phát; server kiểm tính khả thi theo thời gian và coverage, không thể chứng minh người dùng thực sự chú ý/nghe bằng tai.

API đọc thời lượng chính xác của thư viện: [AudioHeader.getPreciseTrackLength](https://javadoc.io/static/net.jthink/jaudiotagger/3.0.1/org/jaudiotagger/audio/AudioHeader.html).

## Mẫu nhập đơn giản theo kỹ năng (V028)

Không có trình chia khối. Giữ các trường cố định và đổi nhãn/gợi ý theo kỹ năng; hướng dẫn, phân tích, mục tiêu và tiêu chí bổ sung không bị bắt buộc nhập đủ. Writing/Speaking phải có đề thực hành trước khi công bố. Trắc nghiệm cần 2–6 lựa chọn liên tiếp, đáp án đúng và giải thích; tối đa 20 câu.

| Kỹ năng | Ảnh (mỗi bài 1 ảnh, tối đa 8 MB) | Audio (1 tệp, tối đa 20 MB) | Video (1 tệp, tối đa 50 MB) |
| --- | --- | --- | --- |
| Grammar & Vocabulary | Cạnh ví dụ / cấu trúc | Không dùng | Hướng dẫn trước ví dụ |
| Reading | Cạnh bài đọc | Không dùng | Hướng dẫn trước bài đọc |
| Listening | Ngữ cảnh ngay trước câu hỏi | Bài nghe ngay trước câu hỏi | Hướng dẫn trước phần nghe |
| Writing | Đề bài trên ô viết | Không dùng | Hướng dẫn trước phần viết |
| Speaking | Đề / ảnh mô tả trên bộ ghi âm | Không dùng cho đề; học viên ghi âm riêng | Hướng dẫn trước phần nói |

Video tải trực tiếp ưu tiên hơn liên kết YouTube khi cả hai cùng có. Muốn so sánh nhiều ảnh, hiện dùng một ảnh ghép; không có danh sách nhiều ảnh. Mỗi lần lưu tổng multipart tối đa 80 MB. Tệp lưu trong storage, DB lưu đường dẫn; học viên chỉ xem tệp bài đã công bố.

- Quản trị chọn mở transcript/bài mẫu ngay, sau nộp bài, sau hoàn thành, hoặc sau nghe 90% (Listening). Kiểm tra trên server: nội dung đang khóa không gửi trong HTML. Mặc định transcript sau nghe 90%; bài mẫu sau nộp, hoặc sau hoàn thành nếu không có phần nộp. Bản nháp không mở khóa; trắc nghiệm ôn phương pháp không thay thế bài nộp Writing/Speaking.
- V028 thêm thời điểm mở vào `lessons`. Snapshot nội dung đề dùng cột `content_snapshot` đã có từ V027; không kích hoạt mô hình khối cũ.
- Lượt nộp mới lưu đề, bài đọc/ví dụ, hướng dẫn, giới hạn từ và đường dẫn media; trắc nghiệm giữ cả lựa chọn gốc. Tệp cũ không bị xóa khi thay/xóa khỏi bài để lịch sử vẫn xem được, endpoint lịch sử kiểm tra chủ sở hữu. Chưa có tác vụ dọn tệp không còn tham chiếu.
- Lượt cũ không có snapshot đầy đủ không thể khôi phục bài đọc/media gốc; không lấy đề hiện tại gán ngược thành lịch sử.
- Nút yêu cầu AI chỉ xuất hiện khi AI được cấu hình và có bài làm đã xác nhận lưu. Khi chấm, dùng đề đã lưu ở lượt nộp, không dùng đề vừa sửa. Ghi âm luôn được lưu; AI vẫn chỉ nhận xét văn bản/transcript, chưa chấm trực tiếp phát âm.

- Grammar/Reading: câu hỏi kiến thức, không nhập ghi chú.
- Listening: audio và câu hỏi, không nhập ghi chú. Trình duyệt cộng thời gian đã phát (không cộng khoảng chỉ tua qua); gửi mốc 90% để ghi nhận nghe. Đây là theo dõi học tập dựa trên trình duyệt, không phải bằng chứng chống gian lận.
- Phần kiểm tra kiến thức cần một lượt đạt ít nhất 60% để hoàn thành. Không có câu hỏi thì bỏ qua điều kiện này.
- Writing: tự lưu là bản nháp (`lesson_responses`); nút lưu xác nhận một bản bài làm trong `lesson_work_submissions`, kiểm tra số từ nếu cấu hình.
- Speaking: phải lưu audio mới hoàn thành. Transcript chỉ phục vụ phản hồi AI; phải gắn với bản ghi hiện tại, không thay thế audio. Ghi âm mới xóa transcript hiện hành để không chấm nhầm lời nói cũ; transcript trong lịch sử vẫn giữ.
- AI chỉ chấm văn bản/transcript đã xác nhận lưu. Chưa tự phiên âm hay chấm phát âm trực tiếp từ audio.

## Dữ liệu lưu bền

- `lesson_check_attempts`: điểm, lựa chọn và `review_json` chứa snapshot câu hỏi, đáp án chọn/đúng, giải thích. Lịch sử mới đọc snapshot, không suy diễn từ câu hỏi đã sửa.
- `lesson_work_submissions`: các lần lưu bài viết, audio/transcript, đề bài tại thời điểm lưu, thời gian và chủ sở hữu. Autosave không sinh lượt lịch sử.
- `lesson_responses`: bản nháp/bài hiện hành và liên kết AI.
- `lesson_engagement`: mốc nghe và vị trí đọc gần nhất.
- `lesson_progress`, `learning_path_items`: tiến độ hoàn thành.
- Video/audio/ảnh lưu trong storage; DB lưu đường dẫn. Giữ tệp ghi âm cũ vì lịch sử còn tham chiếu. Đọc bản ghi lịch sử phải khớp user và lesson, tài khoản khác nhận 404.

## Media quản trị

Ảnh tối đa 8 MB; audio Listening tối đa 20 MB; video trực tiếp MP4/WEBM tối đa 50 MB, kiểm MIME/đuôi/signature cơ bản. Video lưu vào `lessons.video_path`, ưu tiên phát tệp trực tiếp khi đồng thời có YouTube. Endpoint media cho học viên chỉ phục vụ bài đã công bố, hỗ trợ Range.

Migration V026 tạo lịch sử và giữ lại bài viết/bản ghi hiện hành đã có. Các phiên bản bị ghi đè hoặc tệp bị xóa trước migration không thể khôi phục. Lượt kiểm tra cũ chưa có snapshot được ghi rõ là chỉ có điểm/mã đáp án, không gán nội dung câu hỏi hiện tại làm lịch sử cũ.
