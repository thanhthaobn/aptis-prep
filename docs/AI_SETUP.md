# Cấu hình chấm Writing/Speaking bằng OpenAI

Phân công theo [TEAM_ASSIGNMENT_V2.md](TEAM_ASSIGNMENT_V2.md): Thông tiếp nhận provider, đánh giá AI, retry, chấm/tổng hợp skill result; Trâm lưu/khóa bài và gửi yêu cầu sau commit; Thảo hiển thị phản hồi/kết quả theo quyền. Không tự tạo trang chấm mẫu Admin. Ranh giới schema/writer theo [SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md).

## 1. Bật AI trên máy cá nhân

Tạo API key riêng tại https://platform.openai.com/api-keys. Không dùng key công khai và không gửi key vào chat hoặc commit lên Git. Tài khoản API phải có quyền truy cập model và hạn mức phù hợp; đăng nhập ChatGPT không tự cấu hình API cho ứng dụng.

Mở file **src/main/resources/application-dev.properties** đang được gitignore, thêm:

```properties
app.ai.enabled=true
app.ai.api-key=DIEN_API_KEY_RIENG_CUA_BAN
app.ai.grading-model=gpt-4.1-mini
app.ai.transcription-model=gpt-4o-mini-transcribe
```

Hoặc đặt biến môi trường trong IntelliJ Run Configuration:

```text
OPENAI_ENABLED=true
OPENAI_API_KEY=<key riêng>
```

Nếu cấu hình cùng một thuộc tính trong file dev, giá trị file đó được ưu tiên hơn placeholder ở application.properties. Chọn một cách để tránh nhầm. Không đưa key vào template, JavaScript hoặc ảnh chụp cấu hình.

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

Định dạng hỗ trợ: mp3, mp4, mpeg, mpga, m4a, wav, webm, flac, ogg. Dịch vụ OpenAI sẽ kiểm tra khả năng giải mã file; đổi đuôi file không biến file khác thành âm thanh hợp lệ.

Giao diện thi phải thông báo việc gửi bài làm/bản ghi sang OpenAI trước khi chấm. Bản ghi của lượt thi được lưu theo chính sách lưu trữ bài thi, không xóa ngay sau khi gọi service.

## 3. Cách hoạt động

Writing: đề + bài làm → Responses API → JSON Schema → kiểm tra điểm/CEFR/feedback → GradingResult.

Speaking: file → Transcriptions API → bản nhận diện → Responses API → GradingResult kèm transcript.

Speaking qua transcript chỉ đánh giá nội dung và ngôn ngữ; không phải chấm phát âm, nhịp nói hoặc ngữ điệu. Điểm là ước lượng để học tập, không phải điểm Aptis chính thức. Cần kiểm định rubric bằng bài mẫu và đánh giá của giáo viên trước khi dùng để quyết định nâng CEFR.

Model có thể thay qua cấu hình nếu hỗ trợ đúng API và Structured Outputs. Chỉ thêm key của dịch vụ khác vào đây sẽ không hoạt động: phần tích hợp này dành riêng cho OpenAI.

## 4. Hợp đồng cho người làm Exam Engine

```java
GradingResult writingResult = writingGradingService.grade(prompt, textAnswer);
GradingResult speakingResult = speakingGradingService.grade(prompt, savedAudioPath);
```

- Lưu USER_ANSWER.text_answer/audio_url trước khi gọi service.
- Catch AiGradingException; giữ điểm NULL và trạng thái chờ/lỗi khi không chấm được.
- Không gọi lại các bài đã chấm thành công khi retry.
- Ghi ai_score, ai_feedback, graded_at và provider/model; Speaking ghi thêm transcript.
- Điểm trả về là của một bài trả lời. Cần tổng hợp theo kỹ năng, rubric và cấu hình chấm trước khi tạo EXAM_SKILL_RESULT.
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

OpenAiGradingServiceTests dùng phản hồi HTTP giả lập: không gọi OpenAI thật, không mất phí. Kiểm tra key thiếu, AI tắt, hạn mức, từ chối trả lời, điểm ngoài khoảng và pipeline Speaking.

Chưa thể xác nhận quyền truy cập model và chấm thực tế của tài khoản cho đến khi có API key hợp lệ. Không có chế độ tự sinh điểm mẫu để che lỗi.

Tài liệu tham khảo đã dùng:

- https://developers.openai.com/api/docs/guides/structured-outputs
- https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create
- https://developers.openai.com/api/docs/models/gpt-4.1-mini
