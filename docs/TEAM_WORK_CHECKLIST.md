# Checklist chi tiết sau khi chia lại luồng thi — Thông, Thảo, Trâm

> Cập nhật 20/10/2026. Phạm vi engine thi, chấm và kết quả được chia cho ba đầu mối. Dấu `[x]` chỉ có nghĩa code nền đã tồn tại; vẫn phải kiểm thử tích hợp trước khi nghiệm thu.

## 1. Hiện trạng code vừa nhận

- [x] Có entity/repository cho `exam_attempts`, `user_answers`, `user_answer_details`, `exam_skill_results`.
- [x] Có start attempt, tạo answer rỗng, lưu các loại đáp án, submit, chấm objective cơ bản và trang attempt/result tối thiểu.
- [x] Có event `AttemptSubmittedEvent`, `AttemptCompletedEvent` và orchestration Placement/Checkpoint ban đầu.
- [x] Có gọi AI cho Writing/Speaking khi AI bật.
- [ ] Chưa có audio upload thật, bộ đếm Listening, autosave, màn lịch sử hoàn chỉnh, admin result, retry AI thật hoặc test concurrent cho engine.
- [ ] Chưa tách quyền ghi đúng ba đầu mối: `ExamAttemptService` hiện vừa quản lý attempt, vừa chấm, ghi skill result và tổng hợp. Phải tách dần theo mục 2–4.
- [ ] Trạng thái hết giờ hiện có thể vẫn gọi grading nhưng đặt `EXPIRED`; cần chốt rõ có lưu/chấm/hiển thị kết quả hay không.
- [ ] Công thức objective hiện lấy `correct * 50 / total`; chỉ là tạm thời, chưa được coi là điểm Aptis chính thức cho mọi kỹ năng.
- [ ] `retryFailedGrading()` hiện luôn báo chưa hỗ trợ; Speaking đang nhận `audioUrl` từ form thay vì file upload an toàn.
- [ ] Event sau commit là event trong tiến trình; chưa có outbox/job bền nên crash có thể bỏ mất bước AI/Profile/Path.

## 2. Trâm — nhận engine attempt và nội dung đề

### 2.1. Phần code sở hữu

- Controller: `StudentExamController`, controller Placement và endpoint media của attempt.
- Service: phần start/resume/page/save/submit-lock trong `ExamAttemptService`; sau khi tách có thể đổi tên thành `ExamAttemptLifecycleService`.
- Dữ liệu ghi: `exam_attempts`, `user_answers`, `user_answer_details`, `user_media_activities`.
- Tiếp tục sở hữu Question/Item/Option/Media, MockExam/Section/Question và Placement orchestration.
- Không ghi `exam_skill_results`, điểm, CEFR, Profile hoặc Learning Path.

### 2.2. Việc bắt buộc làm thêm

- [ ] Tạo DTO `LockedAttempt` chứa attempt/user/exam/type, answerVersion, submittedAt, scheme/policy version và grading context backend; không đưa đáp án đúng ra frontend.
- [ ] Tách `gradeObjective`, `complete`, `applyAiAssessment` khỏi service vòng đời attempt và bàn giao cho Thông.
- [ ] Chốt state machine: `IN_PROGRESS → SUBMITTED → WAITING_GRADING → COMPLETED|GRADING_ERROR`; `EXPIRED` và `CANCELLED` không được chuyển tùy ý.
- [ ] `start`: khóa/constraint chống hai tab tạo hai Placement cùng user/skill; requestKey trùng phải trả đúng attempt cũ, không nối UUID để che trùng nghiệp vụ.
- [ ] `resume`: khi còn `IN_PROGRESS` trả attempt cũ; không tạo lại answer hoặc reset `startedAt/deadline`.
- [ ] `saveAnswer`: kiểm question thuộc đúng exam, item thuộc question, option thuộc item; hỗ trợ xóa lựa chọn và lưu nhiều item trong Matching/Ordering.
- [ ] Thêm autosave hoặc form save rõ ràng; lưu xong reload phải khôi phục mọi đáp án.
- [ ] `submit`: khóa row, kiểm deadline, khóa toàn bộ answer, tăng `answerVersion`, chống submit đồng thời và phát đúng một yêu cầu grading sau commit.
- [ ] Chốt cách xử lý hết giờ: backend tự nộp hoặc đánh dấu hết hạn; frontend countdown chỉ để hiển thị.
- [ ] Listening: endpoint phát media có owner/attempt/deadline guard, lưu số lượt nghe trong DB, tối đa hai lượt kể cả reload/Range/retry.
- [ ] Speaking: nhận multipart upload thật; kiểm MIME thực, dung lượng, thời lượng nếu có, tên ngẫu nhiên/path traversal; gắn file với user/attempt/answer; không nhận `audioUrl` tùy ý từ form.
- [ ] Không cho sửa/nghe/ghi thêm sau khi bài đã khóa.
- [ ] Placement: chọn đề PUBLISHED đúng skill/scheme, resume một lượt chưa kết thúc, pending grading chặn lượt mới; sau result cuối gọi Profile Thông rồi Path Thảo.
- [ ] Lưu dấu xử lý Placement bền để crash giữa Profile và Path có thể retry mà không nâng cấp hai lần.

### 2.3. Test Trâm phải bàn giao

- [ ] Hai request start đồng thời chỉ tạo một Placement active.
- [ ] Hai request submit đồng thời chỉ khóa/phát grading một lần.
- [ ] User B không xem/sửa/nộp/phát media attempt của user A; tài khoản khóa không ghi tiếp.
- [ ] Option/item/question ngoài đề bị từ chối; đáp án đúng không có trong HTML.
- [ ] Reload không reset deadline/answer/lượt nghe; lượt nghe thứ ba bị chặn ở backend.
- [ ] File giả đuôi audio, quá dung lượng và path lạ bị từ chối.
- [ ] Placement pending/error/retake và kết quả trả sai thứ tự không cập nhật Profile sai.

## 3. Thông — nhận grading, tổng hợp và Profile

### 3.1. Phần code sở hữu

- `ScoringPolicyContract`, Certificate/Scheme/Band và toàn bộ AI assessment.
- Sau khi tách: `ObjectiveGradingService`, `AttemptFinalizationService` và writer giới hạn cho `exam_skill_results`/trạng thái grading.
- Dữ liệu ghi: assessment/job AI, `exam_skill_results`, `final_scale_score`, `estimated_overall_cefr`, `grading_status` và trạng thái cuối của attempt.
- Duy nhất ghi Skill Profile sau Placement/Checkpoint hợp lệ.

### 3.2. Việc bắt buộc làm thêm

- [ ] Nhận `LockedAttempt` từ Trâm và từ chối bài chưa khóa, sai owner/exam/version hoặc grading request cũ.
- [ ] Chấm SINGLE_CHOICE theo option đúng; MATCHING theo từng item; ORDERING theo `correctPosition`; câu bỏ trống tính sai nhưng không gây lỗi.
- [ ] Không dùng một công thức `correct * 50 / total` cho mọi skill nếu policy chưa chốt; raw → scale → CEFR phải theo scheme/skill/version.
- [ ] Snapshot policy/rubric version lúc start/submit; policy sửa sau đó không làm đổi kết quả lịch sử.
- [ ] Upsert một `exam_skill_results` cho mỗi attempt/skill; request/event trùng không cộng hoặc ghi kết quả lần hai.
- [ ] Nếu có objective và subjective cùng skill, không được để kết quả PENDING xóa mất điểm objective; chốt quy tắc tổng hợp thành phần.
- [ ] AI request lấy bài đã khóa từ server, có assessmentKey + answerVersion; không tin nội dung/file path từ client.
- [ ] AI tắt/lỗi/timeout giữ score/CEFR NULL và trạng thái `WAITING_GRADING` hoặc `GRADING_ERROR`, không biến thành 0.
- [ ] Implement `retryFailedGrading`: chỉ retry phần lỗi, không nộp lại bài, không gọi lại phần đã thành công, giới hạn số lần và chống hai retry đồng thời.
- [ ] Response AI cũ hoặc sai version không được ghi đè bài mới; validate score range, provider/model/rubric metadata.
- [ ] `complete`: chỉ hoàn tất khi mọi skill cần chấm có kết quả hợp lệ; tránh chia trung bình cho danh sách rỗng.
- [ ] Chốt overall score/CEFR theo policy thay vì mặc định trung bình cộng và lấy CEFR thấp nhất nếu nghiệp vụ không yêu cầu vậy.
- [ ] Phát `AttemptCompletedEvent` đúng một lần sau commit.
- [ ] `applyAssessment`: xác minh type là Placement/Checkpoint, result COMPLETED, thuộc user, đúng skill/policy và chưa áp trước đó; không hạ dưới checkpoint đã đạt.
- [ ] Cung cấp DTO `ResultView`/`HistoryItem` đã làm sạch cho Thảo; không trả Entity hoặc marking key.

### 3.3. Test Thông phải bàn giao

- [ ] Chấm đúng/thiếu/sai cho từng SINGLE_CHOICE, MATCHING và ORDERING.
- [ ] Test biên raw/scale/band, band thiếu/chồng lấn, scheme sai skill và policy version lịch sử.
- [ ] Event grading trùng, concurrent finalize và AI callback trùng/cũ chỉ ghi một kết quả.
- [ ] AI disabled/timeout/response sai/retry thành công không làm mất bài hoặc thành điểm 0.
- [ ] Attempt chưa đủ skill không COMPLETED; attempt đủ skill phát completed một lần.
- [ ] Placement/Checkpoint áp Profile một lần; Practice/Full Mock không nâng Profile.

## 4. Thảo — nhận lịch sử/kết quả và nối hành trình học

### 4.1. Phần code sở hữu

- Controller/service/template lịch sử và kết quả Student/Admin.
- Lesson/Flashcard, dashboard, Learning Path, progress và Checkpoint.
- Chỉ đọc attempt metadata của Trâm và result DTO của Thông; không tự ghi answer/skill result hoặc tự tính lại điểm.

### 4.2. Việc bắt buộc làm thêm

- [ ] Tạo `/student/results`: phân trang, lọc loại đề/kỹ năng/trạng thái, sắp xếp mới nhất; trạng thái rỗng rõ ràng.
- [ ] Tạo chi tiết result: tiêu đề đề, ngày nộp, trạng thái, raw/scale/CEFR theo skill, overall và feedback được phép xem.
- [ ] Hiển thị `Đang chấm`, `Chấm lỗi — thử lại`, `Hoàn thành`, `Hết hạn` bằng tiếng Việt; NULL không hiển thị thành 0.
- [ ] Nút retry chỉ xuất hiện khi Thông báo có phần retry được; POST CSRF và kiểm owner/admin ở service.
- [ ] Admin results: tìm học viên/email, lọc exam type/status/date, phân trang và xem chi tiết; không cho sửa điểm thủ công ngoài nghiệp vụ đã chốt.
- [ ] Trang admin học viên lấy history/result thật qua DTO; không trộn fixture với DB.
- [ ] Checkpoint: kiểm đủ bài bắt buộc, skill/target, lấy đề checkpoint của Trâm rồi start attempt; không dựng engine riêng.
- [ ] Khi nhận completed checkpoint, kiểm result Thông, gọi Profile Thông rồi đóng/mở chặng idempotent; submit/event lặp không mở hai lần.
- [ ] Checkpoint trượt giữ trình độ/path hiện tại; đạt mới xác nhận cấp và tạo chặng tiếp theo, bảo toàn bài đã hoàn thành.
- [ ] Dashboard cập nhật số bài, tiến độ, bài tiếp theo và kết quả gần nhất; N=0 trả trạng thái chưa có nội dung, không chia 0.
- [ ] Flashcard tiếp tục dùng state đã nhớ/cần học lại để tổng kết, ôn thẻ chưa thuộc và học lại tất cả.
- [ ] Responsive desktop/mobile, nút quay lại rõ và không in enum/tên thuộc tính lên giao diện.

### 4.3. Test Thảo phải bàn giao

- [ ] User chỉ xem kết quả của mình; admin xem được theo quyền; user khác nhận 403/404 phù hợp.
- [ ] Lịch sử rỗng, nhiều trang, filter và pending/error/NULL hiển thị đúng.
- [ ] Retry button đúng trạng thái, CSRF hoạt động và không retry attempt người khác.
- [ ] Checkpoint khóa/mở đúng khi thiếu/đủ bài; đạt/trượt; completed event lặp; result trả chậm.
- [ ] Đổi Goal/Placement refresh path vẫn giữ completion hợp lệ.
- [ ] Kiểm giao diện ở 360px, tablet và desktop lớn.

## 5. Hợp đồng bàn giao giữa ba người

| Bàn giao | Đầu ra bắt buộc | Người nhận không được làm |
|---|---|---|
| Trâm → Thông | `LockedAttempt`, answerVersion, scheme/policy version, grading context backend, requestKey | Thông không đọc controller/form hoặc tin payload client |
| Thông → Thảo | `ResultView`, `HistoryPage`, retry capability, trạng thái/feedback đã làm sạch | Thảo không đọc repository result hoặc tự tính điểm |
| Thông → Trâm | Grading accepted/completed event idempotent | Trâm không ghi `exam_skill_results` |
| Trâm → Thảo | Attempt handle/deadline/exam metadata và start checkpoint | Thảo không tạo engine thứ hai |
| Thảo → Thông | Checkpoint context/path target cần xác minh | Thông không tự ghi path |

## 6. Thứ tự PR đề xuất

1. **PR Trâm 1:** state machine + locked-attempt DTO + submit idempotent, chưa làm media.
2. **PR Thông 1:** tách objective grading/finalization khỏi `ExamAttemptService`, test grading trùng.
3. **PR Thảo 1:** lịch sử/result DTO UI dùng dữ liệu thật, đầy đủ pending/error/NULL.
4. **PR Trâm 2:** Listening counter và Speaking upload/ownership.
5. **PR Thông 2:** AI retry/version/outbox hoặc recovery job bền.
6. **PR Thảo 2:** checkpoint → result/Profile → path, dashboard và admin result.
7. **PR tích hợp:** Placement objective, Full Mock, AI pending/error, Checkpoint đạt/trượt trên SQL Server.

## 7. Definition of Done chung

- `mvnw -B -ntp clean verify` xanh; test SQL Server migration/concurrency chạy trên database thử nghiệm.
- Không sửa migration đã merge; migration mới có số duy nhất và được thử từ DB rỗng lẫn DB đã có dữ liệu.
- Không có hai Entity/Repository cho cùng bảng; không có controller gọi repository xuyên luồng.
- Có test quyền, trạng thái NULL/lỗi, submit/retry trùng và transaction/event sau commit.
- PR ghi rõ bảng được ghi, contract thay đổi, migration, cách kiểm thử và giới hạn còn lại.
