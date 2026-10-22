# Tích hợp và hoàn thiện phần Thảo — 21/10/2026

## Phạm vi và merge

Theo yêu cầu: chỉ sửa hành trình học và trình bày kết quả của Thảo, theo `TEAM_ASSIGNMENT_V2.md`/`SERVICE_CONTRACTS.md`. Các lỗi thuộc Thông/Trâm được bàn giao bên dưới, không sửa engine, Placement, scoring, AI hoặc CRUD câu hỏi/đề trong đợt này.

Đã merge `origin/develop` tại `547fa99cecda9c89341f068b0d143c4889fffe45` vào `feature/thao`, merge commit local `4f79f9d`. Đã xử lý 4 conflict và bổ sung query tương thích của LearningPathItemRepository:

| File xung đột | Cách xử lý |
|---|---|
| LessonProgressRepository | Giữ query từ develop và method đọc hoạt động học của Thảo |
| LearningPathService | Giữ luồng Thảo, sau đó sửa theo nghiệp vụ bên dưới; bỏ nhánh xử lý checkpoint riêng chỉ dành cho test |
| application.properties | Giữ trigger restart của Thảo và giới hạn multipart 25/30MB từ develop |
| student/attempt.html | Nhận bản engine/media mới của Trâm, không tự sửa F10 của Trâm |

Thay đổi test local trước merge đã được bảo toàn; bản stash dự phòng vẫn còn. Không push lên remote trong đợt này.

## Phần Thảo đã sửa

### Lộ trình và tiến độ — F08

- Đọc trình độ hiệu lực qua AccountAccessContract; không ghi Profile trực tiếp. Source attempt NULL không gây NPE.
- Liên kết đúng `goal_skill_id`. Trình độ B1, Goal B2 chỉ được giao bài B2 trong chặng hiện tại, không lấy cả A1–C1.
- Chưa có đánh giá: chặng nền tảng A1. Không có Goal/Placement thì chặng định hướng chưa có target checkpoint; thư viện tất cả mức vẫn có thể tự học.
- Có Goal nhưng chưa có Placement: bắt đầu chặng A1 và tiến dần tới Goal, không mặc định đã biết toàn bộ các mức thấp hơn Goal.
- Goal đã đạt thì không mở chặng vượt Goal. Hoàn tất chặng cuối vẫn hiển thị trạng thái đã đạt checkpoint.
- Khóa user trong transaction khi tạo/refresh/hoàn thành để tránh hai request tạo hai path active hoặc tăng tiến độ lặp.
- Chỉ đọc dashboard không thay tổng bài. Refresh tường minh tạo phiên bản mới khi nội dung đổi, archive path cũ và kế thừa completion có thời điểm hợp lệ.
- Archive được flush trước insert path mới, phù hợp filtered unique active user/skill của SQL Server.
- Giữ thời điểm hoàn thành đầu tiên, đồng bộ LessonProgress và PathItem trong một transaction; bài không được công bố không được đánh dấu hoàn thành mới.
- Bài đã lưu trữ vẫn nằm trong lịch sử/mẫu số của path cũ nhưng không trở thành liên kết học tiếp bị lỗi.
- UI chặng hiện rõ from level, target của chặng, Goal cuối và danh sách bài thực sự được giao. Thư viện tự ôn được tách nhãn, không cộng vào tổng bài chặng.
- Dashboard đọc trình độ/Goal thật; không luôn hiển thị “Chưa đánh giá” khi đã có Profile.

### Checkpoint — F07 và phần Thảo của F08

- Chỉ mở khi có ít nhất một bài bắt buộc và tất cả bài đó hoàn thành có timestamp; không mở chỉ vì 0/0.
- Dùng ExamCatalogContract chọn checkpoint đúng skill/target; lưu `checkpoint_exam_id`; dùng ExamAttemptContract chạy bài.
- Start mới đưa tới trang làm bài. Resume lượt đang làm đưa về bài cũ; lượt đang chấm đưa tới kết quả, không tạo bài mới.
- Request key của Checkpoint ổn định theo user/path/lượt terminal trước đó; một lần start gửi lặp dùng cùng key, còn lượt trượt có thể làm lại.
- Kết quả phải đúng user, attempt, CHECKPOINT, đề đã gắn path, skill, trạng thái đã chấm và không cũ hơn path. Pending/lỗi/sai skill không được hoàn tất chặng.
- Gọi AccountAccessContract để Thông áp dụng Profile; chỉ Thảo ghi trạng thái path.
- Trượt giữ path và completion. Đạt đóng path, ghi completed_at, chỉ mở chặng kế nếu còn Goal/nội dung phù hợp; áp lại cùng kết quả không mở thêm chặng.
- `applyCheckpoint` dùng transaction mới khi được gọi từ listener AFTER_COMMIT, khắc phục lỗi transaction ở phần xử lý path của Thảo. Không sửa callback Placement của Trâm hoặc service Profile của Thông.
- UI phân biệt chưa cấu hình, chưa đủ bài, đang làm, chờ chấm, lỗi, trượt và đạt.

### Giao diện lịch sử/kết quả Student/Admin — F01 và phần Thảo của F13

- Sửa fragment SpringEL khiến trang kết quả thật và Placement result không render được.
- Hiển thị rõ bài đang làm, chờ chấm, lỗi, hết hạn và hoàn tất; điểm NULL không được hiển thị thành 0.
- Trang/hàng lịch sử của bài đang làm có nút tiếp tục bài. Phần lỗi có nút chấm lại chỉ khi DTO báo retryable.
- POST retry kiểm owner qua contract, xác nhận trạng thái ERROR/retryable rồi gọi service chấm lại sẵn có. Không tự chấm hoặc chỉnh attempt/skill result.
- Bỏ câu khẳng định đã tự đồng bộ Goal/Profile khi hệ thống chưa chứng minh điều đó.
- Nhãn trạng thái/loại đề ở kết quả Admin được Việt hóa; ngày lọc đảo ngược bị từ chối.
- Luồng kết quả đọc DTO qua ExamAttemptContract, không đưa entity/đáp án đúng/snapshot nội bộ ra template.

## ERD và schema phần Thảo

Không thêm bảng hoặc sửa migration cũ trong đợt fix này. Giữ quan hệ đã chốt trong SPEC_V2 của nhánh Thảo:

- User → GoalSkill → LearningPath → PathItem → Lesson.
- LearningPath lưu from/target/source/checkpoint và trạng thái chặng; PathItem/ LessonProgress lưu completion riêng.
- Lesson → FlashcardSet → Flashcard; bộ cá nhân có owner, không ép gắn Lesson; progress thẻ thuộc đúng user/card.
- Skill Profile, Attempt, Answer và Skill Result vẫn do Thông/Trâm sở hữu; các test được phép tạo fixture, application code Thảo chỉ đi qua contract.

Migrations V021–V025/curriculum và quan hệ lesson–set đã có trên nhánh Thảo được giữ khi merge. Cần kiểm tra SQL Server/Flyway như mục bàn giao, không suy từ H2 rằng toàn bộ schema đã đạt.

## Note để gửi Trâm

Các mục dưới đây vẫn cần sửa, dù test hiện tại có thể xanh:

| Mã audit | Lỗi/cách tái hiện | Nguồn cần xem | Tiêu chí sửa |
|---|---|---|---|
| F02 | `/student/exams` vẫn hiện 7001–7003; đề vừa tạo trong DB không hiện, mở ID thật 404 | StudentContentPreviewController (route exams), StudentContentPreviewService, student/content/detail | Nối list/detail/start với đề published thật; không dùng fixture trong production |
| F03 | GET edit question/exam có thật ném `entry.id on null` | AdminCatalogPreviewController.edit; admin/catalog/form | Truyền đúng entry/ID cho cả dữ liệu thật, test GET render và POST lưu |
| F05 | Nộp xong, start Placement/Practice lại vẫn cùng attempt terminal | PlacementService.start; StudentExamController.start; ExamAttemptService.start | Key theo một lần start, resume đúng, tạo lượt mới sau terminal; không giới hạn vô tình một lượt suốt đời |
| F06 | Lượt quá deadline trả IN_PROGRESS/0 giây khi start lại | ExamAttemptService.start/page/editableAttempt | Kết thúc lượt quá hạn bền, không nhầm bài đang chấm AI thành hết hạn để tạo lượt mới |
| F09 | FULL_MOCK chỉ tạo một section/skill và chặn câu khác skill | CatalogService.saveExam/validateQuestionSelection; form Exam | Full Mock nhiều skill/section và kiểm cấu trúc đầy đủ |
| F10 | Trang làm bài không include runner/timer, không xác nhận nộp và không flush autosave cuối | student/attempt.html; exam-runner.js; StudentExamController.submit | Timer/deadline, confirm, save cuối, submit trùng, mạng chậm, fallback hoạt động nhất quán |
| F11 | Page/deadline còn đọc đề sống thay snapshot; Writing nhiều item ghi đè text_answer, Speaking ghi đè/xóa file cũ | ExamAttemptService.page/saveAnswer/saveSpeakingAudio; CatalogService.saveQuestion | Giữ version nội dung, tổng hợp bài nhiều ý hoặc giới hạn mô hình nhất quán, bảo toàn file và lịch sử |
| F13 | Admin list/detail fallback sang ID mẫu khi lọc không có kết quả; question media chưa có luồng quản lý đầy đủ | AdminCatalogPreviewController; CatalogService/media templates | Empty state thật; preview/media quyền đúng, thay/xóa/sắp xếp theo yêu cầu |

F04 cần Trâm phối hợp Thông: PlacementOrchestrationService.onCompleted chạy AFTER_COMMIT nhưng gọi Profile dùng REQUIRED/khóa user, đã tái hiện `No active transaction`; outbox vẫn DISPATCHED dù Profile không được tạo. Cần ranh giới transaction mới và marker chỉ xác nhận thành công khi handler thực sự xong. Thảo đã sửa transaction cho `applyCheckpoint`; callback Placement vẫn giữ nguyên để Trâm/Thông sửa.

## Note để gửi Thông

1. **F04 — transaction và event delivery Profile/AI.** Cùng Trâm sửa hậu xử lý Placement và outbox. Kiểm tra tương tự GoalPathListener sau commit; Thảo có lazy sync khi mở path/dashboard nhưng không thay thế giao hàng event đúng transaction.
2. **F12 — policy raw→scale.** ObjectiveGradingService vẫn `correct*50/total` cho mọi skill, ScoringPolicyService.scale giữ nguyên raw. Band theo skill chỉ là scale→CEFR; cần chốt/triển khai policy theo skill/version như PROJECT_DECISIONS.
3. **Checkpoint trượt khi chưa có Profile.** AccountAccessService.applySkillResult tạo UserSkillProfile mới, nhánh CHECKPOINT fail gọi save trước khi có current_level. Entity/schema hiện non-null. Cần xác định cách lưu dấu vết fail mà không tạo Profile với current NULL hoặc bịa A1; test trường hợp Goal trước Placement, checkpoint đầu tiên trượt. Đây là điểm phát hiện khi đọc code, chưa được kiểm thử SQL Server.
4. **AI nhiều câu tự luận và retry.** Kết quả retry UI Thảo đã nối đúng contract, nhưng cần Thông/Trâm nghiệm thu job thật, answerVersion, tổng hợp tất cả câu cùng skill, xử lý không nộp nội dung và AI tắt/lỗi. Không coi test mock là đã gọi provider thành công.
5. **Profile/Goal context.** `AccountAccessService.context` đang lấy `goalSkills.findByGoalUserId` sau khi tìm một Goal active, có thể gom cả skill của Goal archived. Phần Thảo dùng đúng Goal active để gắn goal_skill_id, nhưng contract context vẫn cần Thông lọc theo goal hiện tại.

## Kiểm thử và giới hạn

- Đã biên dịch bản merge trước khi hoàn tất merge commit.
- Test path mới dùng Spring/JPA/Profile service thật, stub rõ ExamAttemptService ở ranh giới engine, chạy H2 riêng. Đã kiểm callback AFTER_COMMIT thật của Checkpoint, pass/fail/pending/sai skill/idempotency/đạt Goal, start/resume/chờ chấm và tạo path đồng thời.
- Test kết quả tạo bản ghi thật rồi render Student result và Placement result ở COMPLETED/WAITING_GRADING/ERROR/EXPIRED/IN_PROGRESS, quyền owner/Admin, CSRF và UI không gán điểm NULL thành 0. Test retry xác nhận delegation và precondition.
- Test Lesson/Flashcard/refresh cũ tiếp tục được giữ; bổ sung kiểm chỉ xem dashboard không thay mẫu số trước explicit refresh.
- Lệnh cuối: `.\mvnw.cmd -B -ntp verify` — **BUILD SUCCESS, 104 test, 0 failure, 0 error, 1 skipped** (103 test thực sự chạy thành công; SQL Server opt-in bị skip). Hoàn tất lúc 21:35 ngày 21/10/2026, múi giờ Asia/Saigon.
- `git diff --check` đạt và không còn conflict marker. Test riêng `LearningPathCheckpointTests`: 9 ca đạt; `StudentResultFlowTests`: 10 ca đạt; `StudentResultRetryTests`: 1 ca đạt.
- Chưa chạy SQL Server migration trên DB riêng, Gemini thật hoặc nghiệm thu trình duyệt/micro/audio desktop/mobile. Không chạy thao tác phá dữ liệu DB local để kiểm thử.

## Trước khi nghiệm thu toàn hệ thống

Kiểm tra DB test rỗng và DB nâng cấp có Flyway history tương tự máy đang dùng. V019 vừa được đưa về từ develop nhưng Thảo có V020–V025; DB nào đã chạy đến V025 mà chưa có V019 có thể cần kế hoạch out-of-order được nhóm review. Không đổi số/xóa/sửa migration đã chạy, không reset DB để né lỗi. Trâm/Thông phụ trách phần V019/engine; Thảo giữ V021–V025 và kiểm quan hệ lesson–flashcard sau nâng cấp.

Phần Thảo có regression riêng không đồng nghĩa toàn bộ luồng Placement/Practice/Full Mock/AI đã hoạt động đầy đủ; các lỗi bàn giao ở trên vẫn cần hai bạn sửa và chạy tích hợp lại.
