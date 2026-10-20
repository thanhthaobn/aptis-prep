# Phân công 3 thành viên — AptisPrep

## 1. Phương án chính thức

Cập nhật ngày 20/10/2026: **Dương không còn tham gia dự án**. Nhóm còn ba thành viên: **Người 1 = Thông, Người 2 = Thảo, Người 3 = Trâm**. Phạm vi engine thi, chấm khách quan và kết quả trước đây của Dương được chia lại trong tài liệu này.

Mỗi bảng và mỗi trạng thái nghiệp vụ chỉ có một đầu mối ghi. Người làm giao diện phải gọi service của chủ dữ liệu, không tạo Entity/Repository thứ hai.

| Người | Luồng sở hữu đầu-cuối | Phần tiếp nhận từ Dương |
|---|---|---|
| 1 — Thông | Tài khoản, Goal/Profile, học viên, Chứng chỉ & Chấm điểm, AI | Chấm khách quan, quy đổi, tổng hợp `exam_skill_results`, hoàn tất kết quả sau AI |
| 2 — Thảo | Lesson/Flashcard, lộ trình, tiến độ, checkpoint, dashboard học tập | Lịch sử/kết quả Student/Admin, nối kết quả vào checkpoint và tiến độ |
| 3 — Trâm | Câu hỏi/media/đề, Placement | Engine attempt: danh sách đề, start/resume/save/timer/listening/speaking/submit và khóa đáp án |

## 2. Thông — tài khoản, scoring và grading

### Sở hữu

- Tài khoản, Goal, Skill Profile, quản lý học viên và trạng thái ACTIVE.
- CRUD chứng chỉ, scoring scheme, score band và `ScoringPolicyContract` có version.
- AI Writing/Speaking, retry, feedback và trạng thái tác vụ đánh giá.
- Chấm SINGLE_CHOICE/MATCHING/ORDERING từ đáp án đã khóa do Trâm bàn giao.
- Duy nhất ghi `exam_skill_results`, raw/scale/CEFR, tổng hợp kết quả và chuyển attempt từ `SUBMITTED/PENDING_GRADING` sang trạng thái kết thúc.
- Kiểm assessmentKey/answerVersion/policyVersion để sự kiện AI trùng hoặc cũ không ghi đè kết quả mới.

### Dữ liệu và trang

`users`, `user_goals`, `user_goal_skills`, `user_skill_profiles`, `certificates`, `scoring_schemes`, `score_bands`, `exam_skill_results` và dữ liệu đánh giá AI. Trang hồ sơ/Goal, admin học viên, Chứng chỉ & Chấm điểm, phản hồi AI.

### Hoàn thành khi

Chấm khách quan và AI đều idempotent; pending/error không thành 0; policy cũ vẫn tái hiện được kết quả; Profile chỉ đổi từ Placement/Checkpoint hợp lệ và không hạ dưới checkpoint đã đạt.

## 3. Thảo — hành trình học và trình bày kết quả

### Sở hữu

- Admin Lesson/Flashcard; học, ôn thẻ, hoàn thành bài, lộ trình và tiến độ.
- Sinh/refresh path không mất completion; checkpoint dùng đề Trâm, attempt Trâm và kết quả đã chấm từ Thông.
- Trang lịch sử/kết quả Student/Admin: danh sách, lọc, phân trang, chi tiết theo kỹ năng, pending/error và feedback.
- Service đọc kết quả phải kiểm owner/admin; không trả Entity, đáp án đúng hoặc grading context ra Thymeleaf.
- Khi nhận kết quả cuối: cập nhật checkpoint/path idempotent, không ghi trực tiếp Profile; gọi service Profile của Thông.

### Dữ liệu và trang

`lessons`, `flashcards`, `lesson_progress`, `user_flashcard_progress`, `learning_paths`, `learning_path_items`. Chỉ đọc attempt/answer/skill result qua service của Trâm/Thông. Trang Lesson/Flashcard/lộ trình/checkpoint/dashboard và lịch sử/kết quả.

### Hoàn thành khi

Học/ôn lưu bền; kết quả chỉ đúng owner/admin; refresh/notification lặp không tăng tiến độ; checkpoint đạt/trượt và mở chặng đúng; dashboard không dùng fixture khi đã có dữ liệu thật.

## 4. Trâm — nội dung đề và engine attempt

### Sở hữu

- CRUD Question/Item/Option/media và bốn loại đề; publish có validation và tham chiếu policy Thông.
- Một engine chung cho Placement/Practice/Checkpoint/Full Mock.
- Danh sách/chi tiết đề; start/resume, lưu/khôi phục đáp án, deadline backend, giới hạn nghe, ghi âm/upload Speaking, submit và khóa đáp án.
- Duy nhất ghi `exam_attempts`, `user_answers`, `user_answer_details`, `user_media_activities` và đổi trạng thái trước giai đoạn grading.
- Chống start/submit đồng thời bằng transaction/constraint/requestKey; sau submit phát sự kiện bền để Thông chấm.
- Placement điều phối kết quả Thông → Profile Thông → path Thảo; không tự ghi Profile/path/result.

### Dữ liệu và trang

`questions`, `question_items`, `question_options`, `question_media`, `mock_exams`, `mock_exam_questions`, `mock_exam_sections`, `exam_attempts`, `user_answers`, `user_answer_details`, `user_media_activities`. Trang admin câu hỏi/đề, Student Placement và giao diện làm/nộp bài.

### Hoàn thành khi

Save/resume/timer/listen-limit/quyền đúng; đáp án khóa trước khi chấm; submit lặp không tạo kết quả trùng; DTO học viên không lộ đáp án; Placement resume/retake đúng policy.

## 5. Ranh giới bắt buộc

| Service/luồng | Chủ sở hữu | Bên dùng |
|---|---|---|
| Account/Profile | Thông | Thảo/Trâm lấy context; chỉ Thông ghi Profile |
| LearningPath | Thảo | Thông đổi Goal, Trâm hoàn tất Placement |
| ExamCatalog | Trâm | Thảo chọn đề checkpoint; Thông lấy grading context backend |
| ExamAttempt | Trâm | Thông nhận bài đã khóa; Thảo đọc lịch sử có phân quyền |
| Scoring/Grading | Thông | Trâm phát yêu cầu sau submit; Thảo đọc kết quả cuối |
| Result presentation | Thảo | Đọc DTO từ Thông/Trâm, không ghi attempt/result |
| Media helper | Trâm | Thảo dùng cho Lesson nhưng vẫn kiểm quyền Lesson |

Chuỗi chuẩn: **Trâm khóa bài → Thông chấm/tổng hợp → Thảo hiển thị và cập nhật hành trình**. Placement: **Trâm điều phối → Thông ghi Profile → Thảo cập nhật path**. Checkpoint: **Thảo kiểm điều kiện → Trâm chạy attempt → Thông chấm/Profile → Thảo đóng/mở chặng**.

## 6. Thứ tự bàn giao

1. Trâm bàn giao DTO bài đã khóa, requestKey, grading context và sự kiện submit cho Thông.
2. Thông bàn giao DTO kết quả cuối/pending/error và service đọc có quyền cho Thảo.
3. Thảo nối lịch sử, checkpoint và dashboard, kiểm notification trùng.
4. Cả ba chạy tích hợp objective, AI pending/error, Placement và Checkpoint trên SQL Server; CI H2 không thay kiểm concurrency/migration thật.

Theo dõi việc còn lại tại [TEAM_WORK_CHECKLIST.md](TEAM_WORK_CHECKLIST.md), hợp đồng tại [SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md), quy trình nhánh tại [GIT_WORKFLOW.md](GIT_WORKFLOW.md).
