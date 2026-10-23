# Đối chiếu V2, ERD và code hiện tại — 23/10/2026

Commit được rà soát: `93b2fbd`, nhánh `develop`.

Đây là báo cáo đọc source và tài liệu, không phải biên bản nghiệm thu UI, AI thật hoặc database đang chạy. Không sửa nghiệp vụ, không xóa file và không sửa migration trong lần rà soát này. Số bảng bên dưới được đếm từ khai báo CREATE TABLE trong migration, không phải truy vấn SQL Server trực tiếp.

## 1. Nguồn đối chiếu và giới hạn của tài liệu

- Đã đọc `docs/SPEC_V2.md`: mục tiêu, ERD, danh sách entity, UC-S01–S14, UC-A01–A10, flowchart, business rules và mục 9 về triển khai.
- Đối chiếu bản SPEC đầu tiên trong Git tại `88088aa` với bản hiện tại. Commit `aa53a92` sửa SPEC để bổ sung FLASHCARD_SET, bộ cá nhân và quan hệ Set → Card. Vì vậy SPEC hiện tại không còn hoàn toàn giống bản ban đầu.
- Đọc thêm `PROJECT_DECISIONS.md`, `V2_IMPLEMENTATION.md`, `SERVICE_CONTRACTS.md`, `PLACEMENT_RETAKE_POLICY.md`, `REAL_PROGRESS_CONTRACT.md`, `STUDENT_MANAGEMENT.md`, `SKILL_LESSON_AUTHORING.md`, `LESSON_WORK_AND_MEDIA.md`, `AI_SETUP.md` và báo cáo ERD ngày 22/10.
- Không xem nội dung mô tả "đã có" trong docs như bằng chứng code hoạt động; kiểm tra lại qua implementation và call site.
- Docs hiện mâu thuẫn với nhau: V2_IMPLEMENTATION và phần đầu SERVICE_CONTRACTS vẫn nói chưa có implementation/DB thật; PROJECT_DECISIONS vẫn mô tả OpenAI; code đang dùng Gemini và có các service triển khai thực tế. SKILL_LESSON_AUTHORING nói knowledge check không chặn hoàn thành, trong khi LESSON_WORK_AND_MEDIA mới hơn quy định phải đạt 60%.
- REAL_PROGRESS_CONTRACT tính trên toàn bộ bài được gán; STUDENT_MANAGEMENT lại mô tả tỷ lệ theo bài bắt buộc. Cần thống nhất ý nghĩa chỉ số; hiện LearningPathService có cả bộ đếm toàn bộ và bộ đếm bắt buộc.

## 2. Kết luận

Hệ thống vẫn giữ trục nghiệp vụ V2: tài khoản → mục tiêu/đầu vào → profile → lộ trình/bài học → bài thi → chấm/kết quả. Không có căn cứ để kết luận toàn bộ thiết kế sai chỉ vì số file tăng.

Tuy nhiên, phạm vi đã mở rộng đáng kể, có code mẫu còn nằm trong service chạy thật, có phần mở rộng chưa được nối đầy đủ, và có chỗ thực sự khác contract V2. Không thể gọi hiện trạng là "đã sạch, đầy đủ và khớp ERD ban đầu".

Ưu tiên sửa tính đúng của scoring, nguồn bài Writing và ranh giới AI Lesson/Exam trước khi giảm số file.

## 3. SINGLE_CHOICE: nghiệp vụ đang dùng, enum Java chưa được dùng

- V2 mục 1, UC-A06 và mục 8 yêu cầu SINGLE_CHOICE, MATCHING, ORDERING, WRITING_TEXT, SPEAKING_AUDIO.
- `enums/QuestionType.java:4` khai báo enum, nhưng chưa tìm thấy call site dùng kiểu enum đó trong source/test/template.
- `service/admin/CatalogService.java:32` có danh sách chuỗi TYPES; saveQuestion tạo Item/Option và lưu is_correct.
- `service/exam/ExamAttemptService.java:221` trở đi xử lý ORDERING, lựa chọn option và Writing theo chuỗi question_type.
- `service/grading/ObjectiveGradingService.java:121` chấm SINGLE_CHOICE/MATCHING bằng selectedOptionId và correctOptionId trong snapshot.
- `V004__align_v2_contract.sql:74` giới hạn loại bằng CHECK của DB.
- `templates/student/attempt.html` render lựa chọn; `static/js/exam-runner.js` có nhánh SINGLE_CHOICE/MATCHING/ORDERING.

Vì vậy nhận định chính xác là: bộ enum và bộ hằng chuỗi tồn tại song song, chưa thống nhất; không phải chức năng SINGLE_CHOICE bị bỏ. Nên dùng QuestionType làm nguồn định nghĩa chung ở Java và giữ tên DB bằng EnumType.STRING hoặc chuyển đổi rõ ràng ở biên. Không xóa loại SINGLE_CHOICE khỏi nghiệp vụ hay dữ liệu.

## 4. Những thiếu sót/rủi ro cần xử lý

### 4.1. Chưa có implementation bảo vệ scoring scheme đã dùng — ưu tiên cao

Bằng chứng: `ScoringAdminService.java:125` lấy `ObjectProvider<ScoringSchemeUsageContract>` và trả `contract != null && contract.isUsed(schemeId)`. Search production chỉ thấy interface và nơi sử dụng, không thấy implementation hoặc bean cung cấp contract.

Hệ quả: khi không có bean, schemeIsUsed luôn false; các nhánh chặn sửa scheme/band đã dùng không hoạt động như ý định ghi trong SERVICE_CONTRACTS. Test hiện mock usage contract, không chứng minh wiring production đầy đủ.

Rủi ro tăng vì `ExamAttemptService.createSnapshot` lưu scheme ID và chuỗi policy version, không lưu toàn bộ band/rule; ScoringPolicyService vẫn đọc bảng policy hiện hành. Chỉ có snapshot đề chưa đủ bảo đảm lịch sử chấm bất biến.

Đề xuất: triển khai usage signal thật hoặc luôn tạo version bất biến khi thay policy, kiểm tra cả đề/lượt thi liên quan, bổ sung integration test bằng bean thật. Không xóa interface để làm mất kiểm tra này.

### 4.2. Quy đổi raw-to-scale đã trở thành bắt buộc nhưng chưa có luồng cấu hình đầy đủ — ưu tiên cao

Bằng chứng: `ScoringPolicyService.java:108` bắt buộc tìm ScoringScaleRule theo scheme/skill. Không có rule thì báo "Chưa cấu hình raw-to-scale...". `V036__add_raw_to_scale_policy.sql` chỉ seed cho certificate APTIS_GENERAL, scheme version 1.0. Chưa tìm thấy route/form hoặc writer của ScoringScaleRule trong phần quản trị; tạo scheme mới chỉ lưu ScoringScheme và band riêng.

Tình huống: Admin tạo scheme version mới và cấu hình band → phần kiểm tính khả dụng có thể chấp nhận vì có band → khi chấm, policy thiếu raw-to-scale và thất bại. Có thể cấu hình bằng SQL thủ công, nhưng đó chưa phải quy trình quản trị hoàn chỉnh trong ứng dụng.

V2 mục 9.3 cho phép mở rộng bảng quy đổi; việc có bảng không sai. Phần thiếu là cách cấu hình và validation trước publish/start, đặc biệt khi tạo version mới.

Ngoài ra, với các rule seed hiện tại có scale 0–50, phép raw từ tỷ lệ đúng rồi scale tuyến tính về 0–50 gần như trở về `50 × correct/total` (có sai khác do làm tròn). Khác raw_max 40/50 chưa tự tạo ra policy chấm khác nhau thực chất. Cần chốt và mô tả đây là estimated policy, không khẳng định đã tái tạo thang Aptis.

### 4.3. AI của Lesson đang dùng ID của LessonResponse làm ID Exam — ranh giới sai, ưu tiên cao

Bằng chứng: `LessonTeachingService.java:116` tạo AssessmentRequest với attemptId và answerId đều là response.getId(), mặc dù đó không phải EXAM_ATTEMPT/USER_ANSWER. Key có prefix lesson, nhưng `AiAssessmentService.java:135,153` vẫn phát AiAssessmentCompletedEvent chung; `AttemptGradingOrchestrationService.java:89` chuyển mọi event vào AttemptFinalizationService; finalization bắt đầu tìm ExamAttempt theo các ID này tại dòng 118.

Hệ quả suy ra từ call chain: feedback Lesson có thể bị lỗi sau khi kết quả AI đã lưu vì không có exam attempt cùng ID/owner; ID tình cờ trùng cũng dẫn vào nhánh exam không đúng domain. Các kiểm tra version/rubric/policy phía sau giảm nguy cơ ghi nhầm nhưng không làm cách biểu diễn ID trở nên đúng.

Đề xuất: model nguồn assessment rõ ràng (LESSON_SUBMISSION/EXAM_ANSWER), hoặc tách sự kiện/đầu vào cho hai luồng. Chỉ finalization bài thi nhận event bài thi. Cần kiểm thử với attempt ID không tồn tại và ID trùng. Chưa chạy AI thật để tái hiện trong lần audit này.

### 4.4. Nguồn bài Writing khác quy tắc V2 — cần quyết định thiết kế

V2 mục 8 và PROJECT_DECISIONS quy định Writing/Speaking ở USER_ANSWER; USER_ANSWER_DETAIL phục vụ khách quan, cột tự luận detail cũ chỉ là legacy.

Hiện `ExamAttemptService.java:232–235` ghi rõ detail row là source of truth và lưu detail.answerText. `lockedAttempt` ghép các item thành nội dung chấm tại dòng 509–513. Chưa tìm thấy writer mới setTextAnswer cho USER_ANSWER trong service này. Speaking lưu theo detail và chỉ đặt answer.audioUrl nếu đang trống.

Đây không chỉ là thêm cột: nguồn dữ liệu chuẩn đã thay đổi để hỗ trợ nhiều item tự luận. Code và tài liệu phải thống nhất. Có thể giữ bài làm theo item bằng thiết kế mở rộng được ghi rõ; hoặc quay về nguồn answer-level theo V2, nhưng phải có cách bảo toàn nhiều item. Không xóa detail tự luận/legacy trước khi kiểm tra dữ liệu và chuyển đổi an toàn.

### 4.5. ORDERING chưa được validate đúng mô hình item của V2

`CatalogService.java:583` coi ORDERING là usesOptions. Validation kiểm correctPosition trong phạm vi số option của từng item, giống câu lựa chọn, thay vì xác nhận vị trí của toàn bộ item là một hoán vị 1..N.

V2 UC-A06/mục 8 dùng QUESTION_ITEM.correct_position cho ORDERING, không bắt buộc tạo các option để xác định vị trí. Hiện Admin có thể bị buộc nhập option dư; nhiều item có cùng correctPosition vẫn có thể qua bước validate Java nếu từng item có option hợp lệ. DB/snapshot khác có thể chặn thêm, nhưng không thay thế thông báo validate nghiệp vụ.

Đề xuất: tách validation ORDERING khỏi SINGLE_CHOICE/MATCHING; kiểm đủ, duy nhất, đúng miền 1..N; không bắt nhập option không phục vụ nghiệp vụ.

### 4.6. Chưa chặn đầy đủ loại câu hỏi theo skill

`CatalogService.validateQuestion` kiểm skill và questionType có nằm trong hai danh sách độc lập; chưa thấy rule kết hợp Writing↔WRITING_TEXT, Speaking↔SPEAKING_AUDIO, ba skill còn lại↔câu khách quan.

V2 mục 1 phân biệt rõ hai nhóm. Request gửi trực tiếp có thể tạo cấu hình không phù hợp dù dropdown UI có thể đã hạn chế. Cần validation backend theo ma trận skill/type và test publish.

### 4.7. Không có Goal nhưng dashboard hiển thị C2

`StudentDashboardController.java:62` dùng C2 khi không có goal hoặc không có target cho skill; template dashboard dòng 104 in skill.goal vào cột mục tiêu. `LearningPathService.java:87,180` cũng dùng C2 làm mốc cuối mặc định.

V2 cho phép bỏ Goal, và REAL_PROGRESS_CONTRACT yêu cầu hiển thị "Chưa đặt mục tiêu". C2 có thể là giới hạn giáo trình nội bộ, nhưng không nên trình bày như mục tiêu do học viên chọn. Cần tách trần CEFR của hệ thống khỏi goal nullable.

### 4.8. Một số phần V2 chưa được cung cấp đầy đủ

- Target score: V2 UC-S04 và USER_GOAL_SKILL có target_score; entity có field nhưng GoalForm/AccountProfileService chưa nhận và lưu target score.
- Full Mock: `CatalogService.validateExamBasics` chỉ yêu cầu ít nhất 2 skill, trong khi V2 mô tả bài thi đầy đủ. Đây là mức ràng buộc yếu; cần chốt rõ có bắt đủ 5 skill hay không, không mặc định đề 2 skill là mô phỏng đầy đủ Aptis.
- Section và scheme: V2 coi section là mở rộng và scoring scheme là tùy chọn; luồng saveExam hiện normalize/tạo section và validate bắt scheme. Đây là thu hẹp tính tùy chọn của V2, cần ghi quyết định rõ.
- Flashcard review_count: mark chỉ tăng khi status đổi, nên ôn lại cùng status không tăng. V2 UC-S09 mô tả đếm lượt ôn. Cần định nghĩa riêng action review và chống gửi lặp, không đồng nhất lượt ôn với số lần đổi LEARNING/MASTERED.
- PASS_SCORE checkpoint cố định 30/50 trong CheckpointScorePolicy. V2 không quy định ngưỡng này. Có thể là policy nhóm, nhưng cần ghi và/hoặc cấu hình theo scheme/skill; không lẫn với ngưỡng knowledge check bài học.

## 5. Hard-code: phân biệt đúng loại

| Loại | Bằng chứng | Đánh giá |
|---|---|---|
| Dữ liệu nghiệp vụ mẫu trong Java | AdminCatalogPreviewService: Entry 2001..5003; StudentContentPreviewService.fallbackExams: 7001..7003 | Nên tách khỏi production hoặc bỏ fallback nếu đã dùng DB thật |
| Cấu hình bật fallback | application.properties:28 = false; test properties = true | Chưa có căn cứ nói mọi trang production hiện đang hiển thị dữ liệu mẫu; fallback mặc định tắt, nhưng object mẫu vẫn được dựng trong bean |
| Mục tiêu giả thay dữ liệu thiếu | StudentDashboardController:62 = C2 | Cần sửa vì làm sai ý nghĩa nghiệp vụ theo học viên |
| Nội dung quảng bá cố định | index.html:34–35,65 có B2, 68%, 7 ngày | Không phải dữ liệu dashboard cá nhân, nhưng nên ghi rõ hình minh họa để tránh hiểu nhầm |
| DTO dựng từ DB | CatalogService.questionEntry/examEntry và StudentContentPreviewService.realExamEntry dùng new Entry/Row | Giữ; new DTO không đồng nghĩa tạo dữ liệu giả |
| Hằng miền/nhãn | enum, list skill/CEFR, LessonAuthorProfile, thông báo lỗi | Bình thường; nên gom nguồn định nghĩa trùng, không biến mọi nhãn thành bảng DB |
| Seed SQL | V003, các migration curriculum/catalogue và V037 demo grading | Là dữ liệu DB khởi tạo, không phải kết quả giả gán cho user. Nên tách demo khỏi migration schema nếu muốn môi trường sạch; không xóa migration đã chạy |
| DemoDataConfig | profile dev + app.demo-data.enabled=true và mật khẩu cấu hình ngoài | Có điều kiện rõ; không phải dữ liệu bắt buộc của ứng dụng. Có thể giữ riêng phục vụ demo |

Các bộ lesson/results trong map của AdminCatalogPreviewService là tàn dư đặc biệt rõ: route AdminCatalogPreviewController đã giới hạn questions|exams, nên dữ liệu mẫu lesson/results không thuộc route hiện tại. isReal luôn true trong các handler này, để lại các nhánh không thể đi tới từ route đã khai báo.

## 6. ERD ban đầu và hiện tại khác nhau thế nào?

| Mốc | Số entity/bảng | Cách đếm |
|---|---|---|
| SPEC_V2 đầu tiên tại 88088aa | 23 | Đếm các mục entity trong phần 3.3 |
| SPEC_V2 hiện tại | 24 | Thêm FLASHCARD_SET trong aa53a92 |
| Schema được khai báo qua migration hiện tại | 38 bảng riêng biệt | Đếm CREATE TABLE, gộp tên trùng; không tính flyway_schema_history |

Chênh lệch là +15 bảng so với V2 ban đầu, hoặc +14 so với SPEC hiện tại. Không thể dùng số 37 trong báo cáo cũ làm con số hiện tại sau khi đã thêm scoring_scale_rules. Đây là số từ source, chưa xác nhận DB máy đã áp dụng tất cả migration.

15 bảng mở rộng so với bản đầu:

| Nhóm | Bảng | Vai trò/đánh giá |
|---|---|---|
| Flashcard | flashcard_sets | Đổi quan hệ Lesson→Card thành Lesson→Set→Card; thêm bộ cá nhân. Đã đưa vào SPEC hiện tại nhưng UC-A10 vẫn diễn đạt kiểu cũ |
| Đề thi | mock_exam_sections | Mở rộng cấu trúc/thời lượng từng skill; hiện UI bắt tạo section |
| Bảo vệ lượt thi | user_media_activities, exam_attempt_snapshots, user_profile_applications | Đếm nghe, khóa đề, tránh áp profile trùng; có lý do nghiệp vụ/kỹ thuật |
| Phục hồi sự kiện | attempt_outbox | Hỗ trợ khôi phục xử lý sau commit; không phải bảng có trong V2 gốc |
| AI tùy chọn | ai_assessment_jobs, ai_assessments | Trạng thái/retry/audit cho phần AI; cần sửa ranh giới Lesson/Exam |
| Học có kiểm tra/lịch sử | lesson_checks, lesson_check_attempts, lesson_responses, lesson_work_submissions, lesson_engagement | Ngoài bài Lesson tối giản của V2, được mô tả thêm trong docs bài học mới |
| Mô hình block | lesson_block_listens | Có migration nhưng không tìm thấy code đọc/ghi bảng; ứng viên schema dư của hướng block đã bỏ |
| Quy đổi điểm | scoring_scale_rules | Mở rộng mà V2 mục 9.3 cho phép; chưa có luồng cấu hình hoàn chỉnh |

Các quan hệ cốt lõi Question→Item→Option, Exam↔Question qua bảng nối, User→Attempt→Result, Goal→GoalSkill, Path→Item→Lesson vẫn giữ hướng ban đầu.

Những thay đổi cần cập nhật ERD/đặc tả thay vì gọi đơn giản là "implementation detail":

1. Flashcard có Set và owner cá nhân thay cho liên kết trực tiếp Lesson→Card.
2. Bài Writing/Speaking theo item, dùng cột detail mà docs vẫn gọi legacy.
3. AI có job/assessment và được dùng thêm cho Lesson, không chỉ Exam.
4. Điều kiện hoàn thành Lesson có knowledge check/nghe/nộp bài, lịch sử media và visibility policy.
5. Scheme raw-to-scale trở thành điều kiện runtime bắt buộc.
6. Target/scheme/section trong tạo đề bị bắt buộc hơn baseline.

## 7. Phần có thể bỏ, phần mở rộng và phần nên giữ

### 7.1. Ứng viên dọn thực sự

- Các fixture AdminCatalogPreviewService và fallbackExams sau khi bỏ các nhánh fallback, sửa test liên quan.
- Nhánh isReal=false của AdminCatalogPreviewController vì route chỉ questions|exams; hasRealEntry đang đọc entry rồi handler đọc lại.
- ExamAttemptService.ownsAttempt và method applyAiAssessment chuyển tiếp: chưa tìm thấy caller trong main/test.
- Enum QuestionType đang treo: ưu tiên đưa vào dùng để loại hằng chuỗi trùng, thay vì bỏ loại nghiệp vụ.
- Constructor event legacy không có LockedAttempt/answerVersion: production hiện tạo payload đầy đủ; cân nhắc bỏ sau rà soát test/compatibility.
- Các import trùng, tên Preview lỗi thời, comment mô tả fixture trong lớp đã dùng DB.
- Schema block cũ và cột SRS chưa dùng phải đánh giá dữ liệu trước khi lập migration dọn; không xóa trực tiếp file migration.

### 7.2. Có dữ liệu/mapping nhưng chưa có tính năng sử dụng tương ứng

- next_review_at, interval_days, ease_factor được thêm bởi V035; tìm kiếm Java chỉ thấy field/getter/setter trong UserFlashcardProgress. Chưa có scheduler/sắp thẻ/lịch review theo các trường đó. Không nên tuyên bố đã triển khai spaced repetition.
- lesson_block_listens cùng content_blocks_json/block_key từ V027 chưa tìm thấy consumer production; docs LESSON_WORK_AND_MEDIA nói không kích hoạt mô hình block cũ.
- target_score có mapping nhưng chưa có input/writer từ nghiệp vụ Goal.
- ScoringSchemeUsageContract có consumer nhưng thiếu implementation, nên đây là phần thiếu chức năng chứ không phải interface dư.

### 7.3. Ngoài V2 nhưng không tự động là code rác

- Quản lý học viên/khóa tài khoản: UC-A11–A14 được docs STUDENT_MANAGEMENT ghi rõ là bổ sung của nhóm.
- Bộ flashcard cá nhân: mở rộng đã sửa vào SPEC hiện tại.
- AI grading: V2 mục 9.4 ghi optional; không bắt buộc phải bỏ vì muốn code đơn giản.
- Knowledge check, lịch sử bài nộp Lesson, transcript/sample visibility: mở rộng trong docs bài học mới. Nếu phạm vi môn học cần thu nhỏ, phải chọn bỏ cả luồng và dữ liệu liên quan, không xóa lẻ entity.

### 7.4. Event và exception

- Exception đang được throw/catch và GlobalExceptionHandler ánh xạ trang lỗi; V2 có alternative/exception flows cần xử lý. Không dư chỉ vì chưa học tới custom exception.
- Bốn event đều có publisher/consumer. Chúng tách thay đổi Goal, nộp bài, kết quả AI và hậu xử lý Placement/Checkpoint. Bỏ phải thay bằng orchestration khác giữ nguyên transaction/retry.
- Event Spring không đồng nghĩa message broker hoặc chạy nền. Code hiện không có @Async trong chuỗi này; AFTER_COMMIT là thời điểm xử lý sau commit, không tự tạo worker nền.
- Lỗi AI Lesson đi vào finalization Exam là lỗi phân loại nguồn event cần sửa, không phải lý do xóa mọi event.

## 8. Đề xuất thứ tự xử lý

1. Chốt baseline: giữ V2 đầu tiên, ghi rõ các thay đổi nhóm đã chọn; cập nhật ERD hiện trạng và các docs mô tả triển khai đã lỗi thời.
2. Sửa usage guard/version bất biến và luồng cấu hình raw-to-scale; thêm test scheme mới, scheme đã dùng và retry lịch sử.
3. Tách domain AI Lesson/Exam; test cả trường hợp ID không tồn tại và trùng ID.
4. Quyết định nguồn bài tự luận, đồng bộ docs/schema/service/read model và kế hoạch chuyển dữ liệu nếu cần.
5. Sửa mục tiêu giả C2, validation skill/type, ORDERING và quy định Full Mock.
6. Chốt nghiệp vụ target score/review count/checkpoint threshold; hoàn thiện hoặc ghi rõ chưa hỗ trợ.
7. Dọn fixture và nhánh chết, đổi tên Preview, thống nhất enum/hằng miền, tách CatalogService thành các trách nhiệm nhỏ hơn.
8. Sau cùng mới đánh giá bỏ SRS/block schema không dùng bằng migration mới có kiểm dữ liệu. Không reset DB hay sửa checksum migration cũ.

Không cần viết lại toàn bộ hệ thống. Cần giảm các đường xử lý mẫu song song, làm rõ phần mở rộng và sửa các chỗ vi phạm contract trước khi coi dự án đã hoàn tất V2.
