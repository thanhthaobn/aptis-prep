# Phân công 3 thành viên — AptisPrep theo V2

## 1. Phương án chính thức

Cập nhật 20/10/2026, **phương án mới nhất thay các phương án trước**: Dương đã nghỉ. Phần chấm khách quan, chấm AI, quy đổi điểm, tổng hợp kết quả và cập nhật Profile do Thông nhận; phần engine thi còn lại chuyển cho Trâm, phối hợp với Thảo ở checkpoint.

Nhân sự hiện hành: **Người 1 = Thông; Người 2 = Thảo; Người 3 = Trâm**. Dương (Người 4) đã nghỉ; các tham chiếu Người 4 trong tài liệu cũ được chuyển theo bảng dưới đây, không tạo đầu mối mới cho Dương.

Checklist từng người theo thứ tự, bàn giao sớm và các mốc tích hợp: [TEAM_WORK_CHECKLIST.md](TEAM_WORK_CHECKLIST.md). Dùng file đó theo dõi thực hiện; tài liệu này giữ phạm vi và quyền sở hữu chính thức.

Mỗi người làm xuyên các lớp Controller → Service → Repository của phần mình, DTO/validation → Thymeleaf → test → dữ liệu kiểm tra → tài liệu. Trọn luồng không có nghĩa là tự viết lại service của người khác. Mỗi bảng và mỗi trạng thái nghiệp vụ chỉ có một đầu mối ghi.

| Người | Luồng sở hữu đầu-cuối | Đầu ra chính |
|---|---|---|
| 1 — Thông | Tài khoản, mục tiêu, học viên, Chấm điểm, AI, kết quả và Profile | Hồ sơ/Goal/Profile; quản lý học viên; chứng chỉ/scheme/band; chấm khách quan/AI → raw/scale/CEFR → tổng hợp kết quả và phản hồi/retry |
| 2 — Thảo | Nội dung và hành trình học tập | Admin Lesson/Flashcard → học/ôn → tiến độ/lộ trình → checkpoint → chặng tiếp và tổng quan |
| 3 — Trâm | Ngân hàng câu hỏi, media, đề, engine thi và kiểm tra đầu vào | Câu hỏi/media/đề công bố; start/save/submit/deadline/listening/speaking; Placement → engine → nhận result từ Thông → cập nhật path |

Người 3 nhận thêm engine thi, media và lifecycle attempt từ Dương. Người 1 gom scoring, objective/AI grading, tổng hợp kết quả và Profile; cần nhận policy/context sớm, không đợi hoàn thiện CRUD. Người 2 phối hợp checkpoint/lộ trình với engine Trâm và result Thông. Không coi tải ba người bằng nhau tuyệt đối.

Nền hiện có: Java 21, Spring Boot, SQL Server, Thymeleaf MVC/session, migration V001–V005, auth, UI preview và service Gemini. Tận dụng nền, không viết lại. Nhiều trang chỉ có GET/fixture, nút ghi disabled, chưa có nghiệp vụ thật. Việc bỏ nhãn mẫu/minh họa không có nghĩa dữ liệu đã thật.

Đọc kèm [V2](SPEC_V2.md), [quyết định](../PROJECT_DECISIONS.md), [trạng thái](V2_IMPLEMENTATION.md), [setup](SETUP_LOCAL.md), [UI](UI_GUIDE.md), [route](ROUTES.md), [contract](SERVICE_CONTRACTS.md), [Git](GIT_WORKFLOW.md). Phân công trong tài liệu này thay mô tả chủ sở hữu cũ trong tài liệu bàn giao.

## 2. Thông (Người 1) — Tài khoản, Chấm điểm, AI, kết quả và Profile

Branch đề xuất: `feature/thong`; PR riêng: contract/policy tối thiểu trước → tài khoản/Goal/Profile → CRUD chứng chỉ/scoring → objective/AI grading → quy đổi/tổng hợp kết quả. Có thể dùng nhánh con theo PR, không gom hết một PR lớn.

### Công việc

- Hoàn thiện đăng ký/đăng nhập/đăng xuất/phân quyền, hồ sơ và validation, chống trùng email; chốt session khi đổi email.
- Goal theo chứng chỉ/ngày/target từng skill; cho bỏ qua Goal/Placement. Đổi Goal gọi Người 2 cập nhật path, không tự ghi bảng path.
- Sở hữu ACTIVE/context và service ghi Skill Profile: xác minh kết quả thật, nguồn Placement/Checkpoint, quyền sở hữu và chống áp dụng lặp. Không hạ dưới checkpoint đã đạt; Practice/Full Mock không nâng Profile.
- Admin danh sách STUDENT, tìm tên/email, lọc trạng thái/phân trang; chi tiết hồ sơ, Goal, tiến độ/Flashcard qua Người 2, lịch sử/attempt qua Người 3 và result DTO của chính Thông.
- Khóa/mở khóa theo STUDENT_MANAGEMENT.md; không xóa học viên, sửa role/mật khẩu/điểm. Khóa có hiệu lực request tiếp theo của session cũ; không unlock nhầm INACTIVE. Bàn giao kiểm tra ACTIVE cho cả nhóm.
- Tiếp nhận trọn Chứng chỉ & Chấm điểm theo SCORING_UI_HANDOFF.md: Controller/form/template, Entity/Repository/Service/test cho certificates/scoring_schemes/score_bands; không chỉ làm backend.
- Certificate code duy nhất; scheme/version duy nhất theo chứng chỉ; band min/max/skill/CEFR không chồng lấn kể cả biên. Chứng chỉ/scheme đã được tham chiếu không hard delete; policy đã dùng giữ lịch sử, thay đổi bằng version mới.
- Triển khai ScoringPolicyContract raw → scale → CEFR theo skill/scheme/version, chốt đơn vị/thang/rounding/biên và rubric AI. Điểm là ước lượng nội bộ, không coi tỷ lệ đúng chung là điểm Aptis chính thức. Test duplicate, band thiếu/trùng/biên và version lịch sử.
- Sở hữu chấm khách quan theo item từ submitted-answer context của Trâm; không để engine thi tự viết công thức hoặc tự tạo kết quả.
- Sở hữu quy đổi raw → scale → CEFR cho cả objective và AI, tổng hợp theo skill/toàn bài và ghi `EXAM_SKILL_RESULTS` theo policy/version đã cố định.
- Cung cấp service đọc chứng chỉ/scheme khả dụng và kiểm scheme thuộc đúng certificate, trạng thái/phạm vi kỹ năng để Người 3 tạo/publish đề. Dữ liệu cũ đã dùng vẫn đọc/chấm theo version đã cố định, không tra bản mới nhất thay thế.
- Bàn giao sớm một Certificate + scheme/policy cố định, DTO và test quy đổi kỳ vọng cho Trâm; chưa cần hoàn thiện toàn bộ form. Không tự nhận engine, recorder hoặc Placement; phối hợp Trâm để hiển thị result DTO.
- Tiếp nhận WritingGradingService/SpeakingGradingService và cấu hình Gemini sẵn có. Không thêm trang chấm mẫu hoặc nhập key cho Admin.
- Nhận yêu cầu đánh giá **bài đã lưu và khóa sau nộp**: kiểm loại bài/payload/version, gọi Gemini ngoài transaction DB, validate response và lưu đánh giá AI bền vững.
- Sở hữu trạng thái tác vụ AI, timeout/lỗi/retry chỉ phần thất bại; request trùng không tạo công việc/đánh giá trùng. Không hứa provider chỉ bị gọi đúng một lần khi timeout không biết kết quả; không retry vô hạn.
- AI tắt/lỗi giữ bài và điểm NULL. Lưu provider/model, rubric/version, feedback, transcript và thời gian theo schema được duyệt; không log secret/nội dung nhạy cảm tùy tiện.
- Sau khi objective/AI hoàn tất, tổng hợp kết quả và phát trạng thái cho engine Trâm; chỉ áp dụng Profile khi nguồn là Placement/Checkpoint hợp lệ, không nâng từ Practice/Full Mock.
- Speaking hiện đánh giá qua transcript: ghi rõ giới hạn, không khẳng định chấm phát âm từ transcript. Không tự bổ sung dịch vụ nhận dạng giọng nói ngoài phạm vi đã chốt.
- Làm trang/fragment phản hồi tự luận chi tiết và form retry có CSRF. Truy cập qua service kiểm owner/admin của Trâm; không tin file path/transcript do client gửi.
- Cung cấp số học viên cho dashboard admin, Goal/Profile cho dashboard học tập.

### Quyền ghi và trang

- Entity/Repository: `users`, `user_goals`, `user_goal_skills`, `user_skill_profiles`, `certificates`, `scoring_schemes`, `score_bands`, `exam_skill_results`.
- Sở hữu dữ liệu đánh giá AI, nhưng một số trường hiện nằm trong answer: phải chốt cách ghi ở mục 6 với Người 3 trước khi code. Không tạo Repository thứ hai cho cùng bảng.
- Trang: hồ sơ/Goal, admin Học viên, Chứng chỉ & Chấm điểm (`/admin/certificates`, `/admin/scoring-schemes`, create/detail/edit và `/admin/settings` redirect hiện có), phản hồi tự luận và dữ liệu kết quả tổng hợp. Tiếp nhận AdminSettingsPreviewController/ScoringPreviewService và template admin/settings/*; không tạo mapping trùng. Không sở hữu engine thi/Placement.

### Hoàn thành khi

Tài khoản/Goal/khóa session chạy thật; Profile đúng nguồn, không áp dụng lặp; CRUD chứng chỉ/scheme/band, objective/AI grading, quy đổi và tổng hợp có validation, version/biên được test; 3 chọn/publish đề và gửi `LockedAttempt`/grading context qua service 1. AI thành công/lỗi/tắt/retry đúng, phản hồi và kết quả chỉ owner/admin. Test provider mock, response sai, timeout, retry đồng thời/kết quả cũ trả chậm, không cần key thật.

## 3. Thảo (Người 2) — Lesson, Flashcard, lộ trình và checkpoint

Branch đề xuất: `feature/learning-checkpoint`.

### Công việc

- Admin CRUD/vô hiệu hóa Lesson theo skill/CEFR, Flashcard thuộc Lesson; bảo toàn lịch sử nội dung đã học.
- Học viên xem bài/tài nguyên qua media service an toàn, học/hoàn thành và ôn thẻ; refresh không tăng tiến độ/lượt ôn trùng.
- Sinh path bằng rule, không cần AI: định hướng khi chưa Goal/Placement và cá nhân khi có context; thiếu nội dung hiển thị đúng trạng thái.
- Đồng bộ lesson_progress/path_items trong transaction; C/N và bài tiếp theo theo REAL_PROGRESS_CONTRACT.md, N=0 trả phần trăm NULL. Không tính công thức khác cho admin.
- Đổi mục tiêu archive path cũ, tạo path mới và bảo toàn completion hợp lệ; khóa thành phần path theo quy tắc đã chốt.
- Checkpoint: kiểm bài bắt buộc/skill/target, chọn đề công bố Người 3, gọi engine Trâm. Kết quả hợp lệ từ Thông → đạt/trượt → gọi service Người 1 ghi Profile → đóng/mở chặng idempotent.
- Trượt giữ trình độ; học đủ bài không tự tăng CEFR. Không tự ghi user_skill_profiles.
- Sở hữu tổng quan học tập động, lộ trình/bài tiếp theo; nhận Goal/Profile và result DTO từ 1, nội dung/engine từ 3.
- Summary tiến độ cho admin Học viên và số Lesson cho dashboard admin.

### Quyền ghi và trang

`lessons`, `flashcards`, `lesson_progress`, `user_flashcard_progress`, `learning_paths`, `learning_path_items`. Trang admin Lesson/Flashcard, Student Bài học/ôn thẻ/lộ trình/checkpoint/dashboard. Không dựng engine thi checkpoint riêng.

### Hoàn thành khi

Học/ôn lưu bền; có path định hướng/cá nhân; C/N không trùng/không chia 0; checkpoint và chặng tiếp đúng, không nâng lặp. Có test quyền, đổi Goal, thiếu nội dung, kết quả trả chậm.

## 4. Trâm (Người 3) — Câu hỏi/đề, engine thi, media và Placement

Branch đề xuất: `feature/tram`; PR DTO/đề nhỏ → CRUD câu hỏi/đề/media → engine thi → Placement. Không nhận CRUD chứng chỉ/scoring hoặc ScoringPolicyContract.

### Công việc

- Khi tạo/sửa/publish đề, lấy danh sách chứng chỉ/scheme qua service Người 1; kiểm lại certificate/scheme phù hợp và khả dụng khi ghi, không chỉ tin dropdown. Lưu tham chiếu phiên bản policy theo schema; không tạo Entity/Repository certificate/scheme/band thứ hai hoặc sửa band từ màn đề.
- CRUD Question → Item → Option: SINGLE_CHOICE/MATCHING/ORDERING/WRITING_TEXT/SPEAKING_AUDIO; option đúng item, ordering hợp lệ, tự luận không ép option.
- Media đề và helper kiểm MIME thực/dung lượng/đọc file an toàn dùng chung. Extension không đủ; không nhận path tùy ý. Trâm kiểm quyền attempt/media của engine.
- CRUD/publish bốn loại đề; kiểm vị trí/section/skill/target, không publish rỗng. DTO FE không lộ đáp án/transcript Listening; grading context chỉ backend.
- Version/snapshot nội dung đã dùng trong engine của mình, tránh sửa câu làm đổi lịch sử. Bàn giao đề nhỏ sớm, gắn scheme/version Người 1 cung cấp; khi policy thật chưa có, dùng mock contract trong test, không tạo policy giả ở cấu hình chạy thật.
- Engine thi: một engine cho Placement/Practice/Checkpoint/Full Mock; start/resume, render năm dạng câu, save/submit/lock, deadline/requestKey, kiểm ownership và context đã khóa.
- Listening tối đa hai lượt theo attempt/media qua reload; Speaking upload/phát lại gắn user/attempt/answer; dùng helper media an toàn, không lộ URL tĩnh.
- **Placement đầu-cuối:** giới thiệu/chọn/bỏ qua → chọn đề hợp lệ → gọi engine Trâm → nhận result Thông → gọi service Profile Người 1 → cập nhật path Người 2.
- Theo PLACEMENT_RETAKE_POLICY.md: không giới hạn tổng lượt; một lượt chưa kết thúc/user/skill; chờ chấm chặn lượt mới cùng skill. Trâm thực thi concurrency/deadline, Người 3 làm điều hướng/nghiệp vụ đầu vào.
- Chọn kết quả hợp lệ mới nhất theo submittedAt rồi id, không chọn điểm cao nhất hoặc thứ tự AI trả về. Chưa chấm không là 0; Người 1 kiểm lại floor checkpoint khi ghi Profile.
- Sở hữu orchestration bền/idempotent Placement → Profile → Path, dấu bước xử lý để crash/retry không mất cập nhật; cùng 1/2 chốt migration nếu cần. Không reset lịch sử/progress.
- Đầu mối dashboard admin: câu/đề công bố/nội dung mới, nhận số học viên/Lesson/lượt thi qua service 1/2/3.

### Quyền ghi và trang

`questions`, `question_items`, `question_options`, `question_media`, `mock_exams`, `mock_exam_questions`, `mock_exam_sections`, `exam_attempts`, `user_answers`, `user_answer_details`, `user_media_activities`. Trang admin câu hỏi/đề, Student Placement/làm bài và dashboard admin. Không sở hữu certificate/scheme/band, `exam_skill_results`, Profile hoặc path.

### Hoàn thành khi

Đề/câu hỏi/media validate/publish thật, engine thi save/resume/submit/media đúng quyền, từ chối scheme sai certificate/không khả dụng theo service 1. Placement dùng cùng engine, skip/resume/retake đúng, nhận result Thông và bảo vệ checkpoint. Không áp dụng kết quả cũ/hai lần; test orchestration mock 1/2/3 và tích hợp thật. Không tự triển khai phép quy đổi hoặc Profile.

## 5. Bàn giao phần việc của Dương sau khi nghỉ

Dương đã nghỉ; phần code engine đã có trong `develop` được tiếp quản theo ranh giới mới, không tiếp tục giao việc hoặc mở PR trên `feature/duong`.

- Trâm tiếp quản `exam_attempts`, `user_answers`, `user_answer_details`, `user_media_activities`: danh sách đề, start/resume, render, save/submit/lock, deadline, Listening/Speaking, ownership và submitted-answer context.
- Thông tiếp quản chấm khách quan, AI/provider/retry, ScoringPolicy raw → scale → CEFR, `EXAM_SKILL_RESULTS`, tổng hợp kết quả, feedback và Profile.
- Thảo phối hợp checkpoint/lộ trình: gọi engine Trâm, nhận result Thông và chỉ cập nhật path/progress theo contract; không chấm hoặc ghi Profile.
- Trâm gửi context đã khóa cho Thông sau commit và nhận grading/result status để cập nhật lifecycle; không tự tính điểm hoặc gọi provider.

## 6. Ranh giới service và quyền ghi — bắt buộc

Tên mới bên dưới là hợp đồng **cần bổ sung trong PR contract**, không phải tất cả interface/bean đã có. Xem SERVICE_CONTRACTS.md và `service.contract` hiện tại.

| Service/luồng | Chủ sở hữu | Bên dùng và giới hạn |
|---|---|---|
| AccountAccessContract / Profile apply | 1 | 2/3 lấy ACTIVE/context; 3 yêu cầu áp Placement, 2 yêu cầu áp checkpoint; 1 xác minh/ghi Profile, không điều phối Placement |
| LearningPathContract | 2 | 1 đổi Goal, 3 Placement yêu cầu cập nhật path; 1 đọc summary; không ghi Profile trực tiếp |
| ExamCatalogContract | 3 | 2/3 lấy đề công bố; FE chỉ có DTO an toàn |
| ScoringPolicyContract | 1 | 1 chấm khách quan/AI và quy đổi raw→scale→CEFR theo version; 3 chỉ cung cấp context/policy version và nhận trạng thái, không ghi điểm |
| Service đọc Certificate/Scheme — cần bổ sung | 1 | 3 lấy lựa chọn/kiểm tương thích khi tạo/publish đề; 2 đọc metadata nếu cần; không repository xuyên luồng |
| ExamAttemptContract / service đọc | 3 | 3 giữ attempt lifecycle/answer/media; 2 Placement/checkpoint dùng engine qua contract; 1 nhận `LockedAttempt`/grading context đã kiểm quyền |
| ExamResult/Grading service — đề xuất mới | 1 | 2/3 đọc result DTO; 1 chấm, quy đổi, tổng hợp, ghi `EXAM_SKILL_RESULTS` và báo trạng thái cho 3 |
| AiAssessmentContract — đề xuất mới | 1 | 3 gửi context sau commit; 1 giữ job/status/provider/feedback, retry và báo grading hoàn tất |
| Placement orchestration — đề xuất mới | 3 | Nhận kết quả 3, gọi 1 ghi Profile rồi 2 cập nhật path; có dấu xử lý bền |
| Media helper | 3 | 2/3 kiểm/đọc file; quyền Lesson/attempt/hai lượt nghe do chủ luồng kiểm |

Người 1 sở hữu chấm, cấu hình, phép quy đổi, tổng hợp kết quả và Profile; Người 3 sở hữu đề, dữ liệu bài làm và lifecycle attempt. Mỗi bên giữ Entity/Repository bảng mình. Schema hiện thiếu raw→scale chuyên biệt thì 1 chốt công thức/mapping có version với 3, migration mới nếu cần; không mặc định UI band đã có phép quy đổi. Vô hiệu hóa policy chặn dùng mới theo luật đã chốt, không làm mất căn cứ lịch sử. Kiểm lại policy khi publish/start, giữ version/snapshot từ lúc attempt bắt đầu; không âm thầm dùng policy mới khi AI trả chậm.

### 6.1. AI và engine không cùng ghi một trạng thái

1. Người 3 lưu/khóa bài; sau commit gửi `LockedAttempt` có assessmentKey, answerId, attemptId, answerVersion, rubric/policyVersion và requestKey. Payload từ server, không nhận điểm/path client.
2. Người 1 chấm objective hoặc claim tác vụ AI; provider chạy ngoài DB transaction, lưu assessment status/điểm/feedback/transcript/provider/model/version/thời gian. Retry phần lỗi, bản cũ không đè bản mới.
3. Người 1 quy đổi theo policy, ghi `EXAM_SKILL_RESULTS`, tổng hợp skill/toàn bài và báo trạng thái sau commit. Lỗi ghi kết quả chỉ retry bước áp dụng, không gọi AI lại.
4. Người 3 nhận trạng thái grading đã chấp nhận/idempotent để cập nhật lifecycle attempt; Người 3/2 nhận result hợp lệ để điều phối Placement/Checkpoint. Người 1 là đầu mối Profile và không tự điều phối Placement.

**Schema AI chốt trước khi code:** ưu tiên migration mới lưu job/assessment riêng (Người 1 sở hữu) khi được duyệt phù hợp V2. Nếu giữ trường AI trong `user_answers`, Trâm chỉ giữ mapping/reader của bảng attempt và cung cấp writer boundary giới hạn; Thông sở hữu service ghi assessment, không tạo Entity/Repository thứ hai hoặc để Trâm ghi điểm tổng hợp. Chọn một phương án trong PR schema, không cùng dùng hai đường ghi. Người giữ nền review; không sửa V001–V005.

### 6.2. Placement/Checkpoint và vòng phụ thuộc

- Người 3 điều phối Placement; 2 điều phối checkpoint; 1 duy nhất ghi Profile. Policy newest/floor dùng chung/test chung, không chép thuật toán khác nhau.
- Tách service đọc khỏi orchestration; thông báo sau commit có dấu xử lý bền/phục hồi. Không tạo vòng bean 1 → 3 → 1 hoặc 1 → 2 → 1 rồi dùng lazy để che.
- Không cần broker/cloud: orchestration/tác vụ bền trong ứng dụng local phù hợp; cơ chế/migration cụ thể chốt ở PR contract.
- Controller không gọi Controller, không repository xuyên luồng. Thymeleaf form/session/CSRF, không REST frontend; backend Gemini là tích hợp ngoài đã chốt.

## 7. File, route và phần chung

| Vùng | Đầu mối |
|---|---|
| Hồ sơ/Goal, admin Học viên, Chứng chỉ & Chấm điểm, objective/AI grading, quy đổi, tổng hợp kết quả, Profile và phản hồi AI | 1 |
| Student Bài học/Flashcard/lộ trình/checkpoint/dashboard; admin Lesson | 2 |
| Student /student/placement; admin câu hỏi/đề; dashboard admin | 3 |
| Student history/result UI; `/student/results`, `/student/attempts/{id}/result`, `/admin/results` đọc `ResultView`/`HistoryPage` DTO của 1 | 2 phối hợp 1; không tự tính điểm hoặc đọc repository result |
| Student đề/làm bài, lifecycle attempt, media, `/student/placement` và attempt metadata | 3 |

Tiếp nhận preview theo ADMIN_UI_HANDOFF.md, STUDENT_UI_HANDOFF.md, SCORING_UI_HANDOFF.md, không tạo route trùng. Tách Controller/service/template preview chung trong PR nền nhỏ trước khi nhiều người sửa. Người 1 chốt dữ liệu phản hồi AI/kết quả; Người 2 nhận result/history UI; Người 3 chốt route engine/attempt. Đăng ký đường dẫn mới trong ROUTES.md trước khi dùng.

Bạn giữ nền review pom/config/SecurityConfig/WebMvcConfig/CSS/fragment/contract/migration; vẫn có thể là một trong bốn người, không phải người thứ năm. CSS/nav dùng chung không sửa riêng từng trang. Migration mới lấy số theo thứ tự merge, không sửa V001–V005 hoặc chia khoảng số theo người. Không commit password/key/upload runtime.

## 8. Triển khai song song — không đợi CRUD hoặc AI xong

### Giai đoạn 0: chốt nền và contract

- Mỗi máy chạy Java 21/SQL Server, migration/seed cố định và test được; cùng Git workflow.
- Chốt ID/enum/state, requestKey/quyền/NULL/lỗi, DTO đề/item/media, raw/scale/rubric, version/snapshot và result/history.
- 1 + 3 chốt grading context, AI job/result/writer; 1 + 2 + 3 chốt Profile/Placement/checkpoint và dấu áp dụng bền. Không để khi tích hợp mới quyết định.
- 1 + 3 chốt DTO certificate/scheme, quy tắc tương thích/publish/start, policy version và test raw→scale→CEFR. Người 1 bàn giao policy tối thiểu trước CRUD/AI để không trở thành điểm chờ.
- Chốt route/file ownership, test fixture chung; interface/mock trong test, không bean giả trong cấu hình thật.

### Giai đoạn 1: cả bốn bắt đầu cùng lúc

| Người | Đầu việc độc lập | Bàn giao sớm |
|---|---|---|
| 1 | Policy tối thiểu trước, Account/Goal/khóa, CRUD chứng chỉ/scoring; objective/AI grading, quy đổi và result với provider mock | Một Certificate/scheme cố định + DTO chọn/kiểm policy + test quy đổi cho 3; grading/result DTO và Profile contract |
| 2 | CRUD Lesson/Flashcard, học/lưu tiến độ; rule path với context mock | Summary C/N, path states, checkpoint input/output |
| 3 | Đề nhỏ dùng scheme do 1 cấp hoặc mock contract trong test; CRUD câu hỏi/media; Placement với mock engine/Profile | Đề/câu hỏi/đáp án backend và tham chiếu version cho 3 trước CRUD hoàn chỉnh; policy Placement newest/floor cùng 1 |
| 3 | Start/save/timer/submit/media với catalog mock; state machine và attempt metadata | `LockedAttempt`, answerVersion, policy/rubric version, requestKey; lifecycle/result status contract cho 1/2 |

Người 1 bàn giao policy tối thiểu và Người 3 bàn giao đề nhỏ ngay đầu đợt, không ai cần xong toàn bộ CRUD trước. Người 1 test scoring/AI không cần UI thi; 3 test engine bằng mock catalog/grading không cần AI; 2 test path bằng context mock. Không đưa mock vào cấu hình chạy thật.

### Giai đoạn 2: nối các mạch nhỏ

1. **1 + 3:** Certificate/scheme và policy nhỏ (1) → đề công bố (3) → engine/lưu/nộp (3) → chấm khách quan/quy đổi/tổng hợp kết quả (1), chưa cần AI. Thử thêm scheme không hợp lệ và giữ version lịch sử.
2. **2 + 1:** học/hoàn thành → summary → admin xem; Goal → path, có trường hợp bỏ qua.
3. **3 + 1:** Trâm khóa bài → Thông AI task bền/assessment → quy đổi/tổng hợp/result/Profile; test bằng provider mock.
4. **3 + 1 + 2:** Placement → engine Trâm → result/điểm/Profile Thông → path; làm kỹ năng khách quan trước để không chờ AI, sau test pending/AI trả chậm.
5. **2 + 3 + 1:** checkpoint → điều kiện/đề → engine Trâm → result/điểm/Profile Thông → chặng tiếp.

Merge PR nhỏ sau từng mạch; nhánh khác cập nhật theo Git workflow. Không đợi hết ba luồng mới ghép. Mock giúp code song song, không thay nghiệm thu thật.

### Giai đoạn 3: hoàn thiện và nghiệm thu

- Timer/hai lượt nghe qua reload, Speaking upload/phát đúng quyền, AI retry/crash/phản hồi cũ; không gọi AI lại khi chỉ lỗi áp assessment.
- Khóa session chặn ghi; Student không đọc bài người khác, ADMIN không lọt danh sách STUDENT.
- Placement nhiều tab/chờ chấm/làm lại: chọn kết quả mới nhất hợp lệ, không hạ checkpoint/reset progress.
- Path/checkpoint/progress idempotent; điểm chưa chấm NULL; lịch sử không đổi khi cập nhật nội dung.
- SQL Server thật kiểm migration/constraint/concurrency; mock/H2 không chứng minh mọi hành vi SQL Server.

## 9. Dữ liệu demo và tiêu chí PR

- Seed cố định: Admin, Student mới/có hoạt động; Lesson/Flashcard đủ chặng; bốn loại đề, năm dạng câu và media có quyền dùng; checkpoint đạt/trượt; test AI pending/lỗi.
- Không gọi AI sinh điểm lúc khởi động. Key riêng ở cấu hình local/biến môi trường; test không cần key thật.
- PR có hành trình kiểm tra được, validation/POST-redirect/CSRF/quyền; test rỗng/lỗi/đồng thời, cập nhật route/contract/tài liệu và ảnh UI khi cần.
- Chủ dữ liệu/service và bên dùng review contract; mỗi bảng một Entity/Repository; đổi schema báo đầu mối.
- Chưa triển khai nghiệp vụ trong lần sửa tài liệu này; không mô tả fixture/preview là đã hoàn tất. Chưa commit/push.
