# Cấu hình chấm Writing/Speaking bằng Gemini

Phân công theo [TEAM_ASSIGNMENT_V2.md](TEAM_ASSIGNMENT_V2.md): Thông phụ trách scoring/provider, Trâm phụ trách lưu/khóa bài và snapshot, Thảo phụ trách học tập/giao diện. AI đã có luồng lưu job, retry, recovery và cách ly nguồn Lesson/Exam; test mock không chứng minh chất lượng provider thật. Cấu hình hiện dùng biến môi trường và một application.properties; không có dữ liệu chấm mẫu thay kết quả thật.

## 1. Bật AI trên máy cá nhân

Tạo API key riêng tại https://aistudio.google.com/apikey. Không dùng key công khai và không gửi key vào chat hoặc commit lên Git. Tài khoản API phải có quyền truy cập model và hạn mức phù hợp.

Ứng dụng dùng một `application.properties`. Đặt biến môi trường trong IntelliJ Run Configuration hoặc phiên terminal chạy Maven:

```text
GEMINI_ENABLED=true
GEMINI_API_KEY=<key riêng>
GEMINI_GRADING_MODEL=gemini-3.5-flash-lite
```

Không còn file cấu hình dev được nạp tự động. Không đưa key vào template, JavaScript, Git hoặc ảnh chụp cấu hình. Biến môi trường DB_PASSWORD vẫn cần cho kết nối SQL Server; xem SETUP_LOCAL.md.

Không có key thì để app.ai.enabled=false. Project vẫn chạy, chức năng chấm sẽ báo chưa khả dụng và không tạo điểm giả.

## 2. Trạng thái tích hợp

Ứng dụng đã có giao diện yêu cầu phản hồi AI cho bài học Writing/Speaking và
trang xem hoặc thử chấm lại kết quả. Nút chỉ xuất hiện sau khi AI được bật,
key có giá trị, và học viên đã lưu đúng bài làm vào lịch sử. Sau khi thay cấu
hình phải khởi động lại ứng dụng để nạp key.

Với bài thi, sau khi nộp attempt, câu Writing/Speaking được tạo tác vụ chấm
sau khi giao dịch lưu bài đã commit. Trạng thái chờ/lỗi/hoàn tất được lưu trong
DB; lỗi nhà cung cấp không được biến thành điểm 0.

Writing gửi đề và bài làm. Luồng bài học Speaking hiện yêu cầu học viên lưu cả
bản ghi lẫn transcript, sau đó AI đánh giá transcript; bản ghi dùng để nghe lại.
Luồng engine thi có thể gửi file âm thanh để phiên âm khi answer được khóa đúng
cấu trúc. File gửi trực tiếp tới dịch vụ tối đa 24 MB.

Định dạng hỗ trợ: mp3, mp4, mpeg, mpga, m4a, wav, webm, flac, ogg. Dịch vụ Gemini nhận file audio inline và kiểm tra định dạng/kích thước; đổi đuôi file không biến file khác thành âm thanh hợp lệ.

Giao diện thi phải thông báo việc gửi bài làm/bản ghi sang Gemini trước khi chấm. Bản ghi của lượt thi được lưu theo chính sách lưu trữ bài thi, không xóa ngay sau khi gọi service.

## 3. Cách hoạt động

Writing: đề + bài làm → Gemini `generateContent` → JSON Schema → kiểm tra điểm/CEFR/feedback → GradingResult.

Speaking: file audio inline → Gemini `generateContent` → transcript + JSON Schema → GradingResult kèm transcript.

Speaking qua transcript chỉ đánh giá nội dung và ngôn ngữ; không phải chấm phát âm, nhịp nói hoặc ngữ điệu. Điểm là ước lượng để học tập, không phải điểm Aptis chính thức. Cần kiểm định rubric bằng bài mẫu và đánh giá của giáo viên trước khi dùng để quyết định nâng CEFR.

Model có thể thay qua `GEMINI_GRADING_MODEL` nếu model hỗ trợ JSON response schema và audio input.

## 4. Hợp đồng giữa Exam Engine và grading

```java
GradingResult writingResult = writingGradingService.grade(prompt, textAnswer);
GradingResult speakingResult = speakingGradingService.grade(prompt, savedAudioPath);
```

- Trâm lưu `USER_ANSWER.text_answer`/file audio trước khi gọi service.
- Catch AiGradingException; giữ điểm NULL và trạng thái chờ/lỗi khi không chấm được.
- Không gọi lại các bài đã chấm thành công khi retry.
- Thông ghi ai_score, ai_feedback, graded_at và provider/model; Speaking ghi thêm transcript.
- Điểm trả về là của một bài trả lời. Thông cần tổng hợp theo kỹ năng, rubric và cấu hình chấm trước khi tạo `EXAM_SKILL_RESULT`.
- Tách lệnh gọi mạng khỏi transaction lưu đáp án. Không giữ khóa database trong thời gian chờ API.
- Không cho học viên xem prompt nội bộ, API key hay phản hồi lỗi thô từ nhà cung cấp.
- UI form POST; không mở REST endpoint cho frontend. Redirect sau khi nộp bài và kiểm tra trạng thái attempt để refresh hoặc submit lặp không gây chấm nhiều lần.

## 5. Kiểm tra cấu hình trước khi demo

Không in key ra terminal. Chỉ kiểm tra hai điều kiện sau:

1. `app.ai.enabled=true` trong file dev bị gitignore.
2. `OPENAI_API_KEY` có giá trị trong Run Configuration hoặc
   `app.ai.api-key` có giá trị trong file dev.

Sau khi restart, mở bài Writing/ Speaking đã công bố. Lưu bài làm vào lịch sử;
nếu cấu hình đúng sẽ thấy nút **Yêu cầu AI nhận xét**. Nếu không thấy nút, kiểm
tra lại hai điều kiện trên và restart toàn bộ process Java.

Kịch bản demo chi tiết nằm trong [DEMO_RUNBOOK.md](DEMO_RUNBOOK.md).

## 6. Kiểm thử và giới hạn xác nhận

GeminiGradingServiceTests dùng phản hồi HTTP giả lập: không gọi Gemini thật, không mất phí. Kiểm tra key thiếu, AI tắt, hạn mức, phản hồi bị chặn, điểm ngoài khoảng và pipeline Speaking.

Nếu Trâm có key riêng và muốn nghiệm thu provider thật, chạy smoke test opt-in trong PowerShell (không ghi key vào file hoặc log):

```powershell
$env:APTIS_GEMINI_LIVE_TESTS='true'
$env:GEMINI_API_KEY='KEY_RIENG_CUA_TRAM'
.\mvnw.cmd '-Dtest=GeminiLiveGradingServiceTests' test
```

Test này gọi cùng `GeminiGradingService` đang dùng trong ứng dụng và mặc định bị skip khi chưa bật `APTIS_GEMINI_LIVE_TESTS`.

Chưa thể xác nhận quyền truy cập model và chấm thực tế của tài khoản cho đến khi có API key hợp lệ. Không có chế độ tự sinh điểm mẫu để che lỗi.

Tài liệu tham khảo đã dùng:

- https://ai.google.dev/api/generate-content
- https://ai.google.dev/gemini-api/docs/generate-content/structured-output
- https://ai.google.dev/gemini-api/docs/generate-content/working-with-audio
