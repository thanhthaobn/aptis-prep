# Cấu hình chấm Writing/Speaking bằng Gemini

Phân công mới theo [TEAM_ASSIGNMENT_V2.md](TEAM_ASSIGNMENT_V2.md): Người 1 tiếp nhận service/provider, objective/AI grading, quy đổi, retry, tổng hợp và Profile; Người 3 lưu/khóa bài, gửi `LockedAttempt` sau commit và nhận trạng thái grading idempotent. Người 2 chỉ đọc DTO để hiển thị history/result. Không tự tạo trang chấm mẫu Admin. Ranh giới schema/writer và hợp đồng AI phải chốt trong PR đầu theo [SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md); đây chưa phải nghiệp vụ đã triển khai.

## 1. Bật AI trên máy cá nhân

Tạo API key riêng tại https://aistudio.google.com/apikey. Không dùng key công khai và không gửi key vào chat hoặc commit lên Git. Tài khoản API phải có quyền truy cập model và hạn mức phù hợp.

Mở file **src/main/resources/application-dev.properties** đang được gitignore, thêm:

```properties
app.ai.enabled=true
app.ai.api-key=DIEN_API_KEY_RIENG_CUA_BAN
app.ai.grading-model=gemini-3.5-flash-lite
```

Hoặc đặt biến môi trường trong IntelliJ Run Configuration:

```text
GEMINI_ENABLED=true
GEMINI_API_KEY=<key riêng>
```

Nếu cấu hình cùng một thuộc tính trong file dev, giá trị file đó được ưu tiên hơn placeholder ở application.properties. Chọn một cách để tránh nhầm. Không đưa key vào template, JavaScript hoặc ảnh chụp cấu hình.

Không có key thì để app.ai.enabled=false. Project vẫn chạy, chức năng chấm sẽ báo chưa khả dụng và không tạo điểm giả.

## 2. Trạng thái tích hợp

Hiện giữ service chấm Writing/Speaking, cấu hình Gemini và kiểm thử HTTP giả lập. Chưa có giao diện gọi chấm AI. Sau khi nhập key, khởi động lại ứng dụng để nạp cấu hình; việc nhập key chưa tự tạo chức năng chấm trên web.

Trâm, người phụ trách Exam Engine, gửi context bài đã khóa cho service của Thông sau khi học viên nộp bài. Thông lưu assessment/điểm/nhận xét và phát trạng thái để engine cập nhật lifecycle; Thảo hiển thị DTO trong trang kết quả. Writing gửi đề và bài làm; Speaking gửi file đã lưu, tối đa 24 MB.

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

## 5. Kiểm thử và giới hạn xác nhận

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
