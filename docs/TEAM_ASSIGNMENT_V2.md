# Phân công 4 thành viên — AptisPrep theo V2

## 1. Phương án chính thức

Cập nhật 17/10/2026, **phương án mới nhất thay các phương án trước**: chuyển trọn Chứng chỉ & Chấm điểm (giao diện, bảng dữ liệu, service quy đổi) từ Người 3 sang Người 1. Người 1 vẫn nhận AI, Người 3 vẫn nhận Placement, Người 4 vẫn giữ lịch sử/kết quả toàn bài.

Nhân sự đã chốt: **Người 1 = Thông; Người 2 = Thảo; Người 3 = Trâm; Người 4 = Dương**. Các số thứ tự trong tài liệu này và tài liệu liên quan luôn đối chiếu theo tên này; không thay đổi phạm vi công việc khi bổ sung tên.

Checklist từng người theo thứ tự, bàn giao sớm và các mốc tích hợp: [TEAM_WORK_CHECKLIST.md](TEAM_WORK_CHECKLIST.md). Dùng file đó theo dõi thực hiện; tài liệu này giữ phạm vi và quyền sở hữu chính thức.

Mỗi người làm xuyên các lớp Controller → Service → Repository của phần mình, DTO/validation → Thymeleaf → test → dữ liệu kiểm tra → tài liệu. Trọn luồng không có nghĩa là tự viết lại service của người khác. Mỗi bảng và mỗi trạng thái nghiệp vụ chỉ có một đầu mối ghi.

| Người | Luồng sở hữu đầu-cuối | Đầu ra chính |
|---|---|---|
| 1 — Thông | Tài khoản, mục tiêu, học viên, Chứng chỉ & Chấm điểm và AI | Hồ sơ/Goal/Profile; quản lý học viên; chứng chỉ/scheme/band → quy đổi; tự luận đã nộp → OpenAI → đánh giá/retry/phản hồi |
| 2 — Thảo | Nội dung và hành trình học tập | Admin Lesson/Flashcard → học/ôn → tiến độ/lộ trình → checkpoint → chặng tiếp và tổng quan |
| 3 — Trâm | Ngân hàng câu hỏi, media, đề và kiểm tra đầu vào | Câu hỏi/media/đề công bố có tham chiếu policy Người 1; Placement → engine chung → kết quả → gọi cập nhật Profile/path |
| 4 — Dương | Thi, chấm khách quan và kết quả | Danh sách đề → bắt đầu/làm/nộp → chấm khách quan/tổng hợp → lịch sử và kết quả Student/Admin |

Người 3 giảm ba nhóm bảng/form/validation và engine quy đổi. Người 1 tận dụng auth đã có và gom scoring với AI; cần bàn giao policy nhỏ sớm, không đợi hoàn thiện CRUD. Người 4 vẫn khó về backend nhưng không tích hợp provider/retry AI. Không coi tải bốn người bằng nhau tuyệt đối.

Nền hiện có: Java 21, Spring Boot, SQL Server, Thymeleaf MVC/session, migration V001–V005, auth, UI preview và service OpenAI. Tận dụng nền, không viết lại. Nhiều trang chỉ có GET/fixture, nút ghi disabled, chưa có nghiệp vụ thật. Việc bỏ nhãn mẫu/minh họa không có nghĩa dữ liệu đã thật.

Đọc kèm [V2](SPEC_V2.md), [quyết định](../PROJECT_DECISIONS.md), [trạng thái](V2_IMPLEMENTATION.md), [setup](SETUP_LOCAL.md), [UI](UI_GUIDE.md), [route](ROUTES.md), [contract](SERVICE_CONTRACTS.md), [Git](GIT_WORKFLOW.md). Phân công trong tài liệu này thay mô tả chủ sở hữu cũ trong tài liệu bàn giao.

## 2. Thông (Người 1) — Tài khoản, học viên, Chứng chỉ & Chấm điểm và AI

Branch đề xuất: `feature/account-ai-assessment`; PR riêng: contract/policy tối thiểu trước → tài khoản/Goal và quản lý học viên → CRUD chứng chỉ/scoring → AI → tích hợp. Có thể dùng nhánh con theo PR, không gom hết một PR lớn.

### Công việc

- Hoàn thiện đăng ký/đăng nhập/đăng xuất/phân quyền, hồ sơ và validation, chống trùng email; chốt session khi đổi email.
- Goal theo chứng chỉ/ngày/target từng skill; cho bỏ qua Goal/Placement. Đổi Goal gọi Người 2 cập nhật path, không tự ghi bảng path.
- Sở hữu ACTIVE/context và service ghi Skill Profile: xác minh kết quả thật, nguồn Placement/Checkpoint, quyền sở hữu và chống áp dụng lặp. Không hạ dưới checkpoint đã đạt; Practice/Full Mock không nâng Profile.
- Admin danh sách STUDENT, tìm tên/email, lọc trạng thái/phân trang; chi tiết hồ sơ, Goal, tiến độ/Flashcard qua Người 2, lịch sử qua Người 4.
- Khóa/mở khóa theo STUDENT_MANAGEMENT.md; không xóa học viên, sửa role/mật khẩu/điểm. Khóa có hiệu lực request tiếp theo của session cũ; không unlock nhầm INACTIVE. Bàn giao kiểm tra ACTIVE cho cả nhóm.
- Tiếp nhận trọn Chứng chỉ & Chấm điểm theo SCORING_UI_HANDOFF.md: Controller/form/template, Entity/Repository/Service/test cho certificates/scoring_schemes/score_bands; không chỉ làm backend.
- Certificate code duy nhất; scheme/version duy nhất theo chứng chỉ; band min/max/skill/CEFR không chồng lấn kể cả biên. Chứng chỉ/scheme đã được tham chiếu không hard delete; policy đã dùng giữ lịch sử, thay đổi bằng version mới.
- Triển khai ScoringPolicyContract raw → scale → CEFR theo skill/scheme/version, chốt đơn vị/thang/rounding/biên và rubric AI. Điểm là ước lượng nội bộ, không coi tỷ lệ đúng chung là điểm Aptis chính thức. Test duplicate, band thiếu/trùng/biên và version lịch sử.
- Cung cấp service đọc chứng chỉ/scheme khả dụng và kiểm scheme thuộc đúng certificate, trạng thái/phạm vi kỹ năng để Người 3 tạo/publish đề. Dữ liệu cũ đã dùng vẫn đọc/chấm theo version đã cố định, không tra bản mới nhất thay thế.
- Bàn giao sớm một Certificate + scheme/policy cố định, DTO và test quy đổi kỳ vọng cho 3/4; chưa cần hoàn thiện toàn bộ form. Không tự nhận thêm engine, Placement hoặc trang kết quả tổng quát.
- Tiếp nhận WritingGradingService/SpeakingGradingService và cấu hình OpenAI sẵn có. Không thêm trang chấm mẫu hoặc nhập key cho Admin.
- Nhận yêu cầu đánh giá **bài đã lưu và khóa sau nộp**: kiểm loại bài/payload/version, gọi OpenAI ngoài transaction DB, validate response và lưu đánh giá AI bền vững.
- Sở hữu trạng thái tác vụ AI, timeout/lỗi/retry chỉ phần thất bại; request trùng không tạo công việc/đánh giá trùng. Không hứa provider chỉ bị gọi đúng một lần khi timeout không biết kết quả; không retry vô hạn.
- AI tắt/lỗi giữ bài và điểm NULL. Lưu provider/model, rubric/version, feedback, transcript và thời gian theo schema được duyệt; không log secret/nội dung nhạy cảm tùy tiện.
- Speaking hiện đánh giá qua transcript: ghi rõ giới hạn, không khẳng định chấm phát âm từ transcript. Không tự bổ sung dịch vụ nhận dạng giọng nói ngoài phạm vi đã chốt.
- Làm trang/fragment phản hồi tự luận chi tiết và form retry có CSRF. Truy cập qua service kiểm owner/admin của Người 4; không tin file path/transcript do client gửi.
- Cung cấp số học viên cho dashboard admin, Goal/Profile cho dashboard học tập.

### Quyền ghi và trang

- Entity/Repository: `users`, `user_goals`, `user_goal_skills`, `user_skill_profiles`, `certificates`, `scoring_schemes`, `score_bands`.
- Sở hữu dữ liệu đánh giá AI, nhưng một số trường hiện nằm trong answer: phải chốt cách ghi ở mục 6 với Người 4 trước khi code. Không tạo Repository thứ hai cho cùng bảng.
- Trang: hồ sơ/Goal, admin Học viên, Chứng chỉ & Chấm điểm (`/admin/certificates`, `/admin/scoring-schemes`, create/detail/edit và `/admin/settings` redirect hiện có), phản hồi tự luận chi tiết. Tiếp nhận AdminSettingsPreviewController/ScoringPreviewService và template admin/settings/*; không tạo mapping trùng. Không sở hữu Placement/kết quả toàn bài.

### Hoàn thành khi

Tài khoản/Goal/khóa session chạy thật; Profile đúng nguồn, không áp dụng lặp; CRUD chứng chỉ/scheme/band và quy đổi có validation, version/biên được test; 3 chọn/publish đề bằng policy và 4 quy đổi được qua service 1. AI thành công/lỗi/tắt/retry đúng, phản hồi chỉ owner/admin. Test provider mock, response sai, timeout, retry đồng thời/kết quả cũ trả chậm, không cần key thật.

## 3. Thảo (Người 2) — Lesson, Flashcard, lộ trình và checkpoint

Branch đề xuất: `feature/learning-checkpoint`.

### Công việc

- Admin CRUD/vô hiệu hóa Lesson theo skill/CEFR, Flashcard thuộc Lesson; bảo toàn lịch sử nội dung đã học.
- Học viên xem bài/tài nguyên qua media service an toàn, học/hoàn thành và ôn thẻ; refresh không tăng tiến độ/lượt ôn trùng.
- Sinh path bằng rule, không cần AI: định hướng khi chưa Goal/Placement và cá nhân khi có context; thiếu nội dung hiển thị đúng trạng thái.
- Đồng bộ lesson_progress/path_items trong transaction; C/N và bài tiếp theo theo REAL_PROGRESS_CONTRACT.md, N=0 trả phần trăm NULL. Không tính công thức khác cho admin.
- Đổi mục tiêu archive path cũ, tạo path mới và bảo toàn completion hợp lệ; khóa thành phần path theo quy tắc đã chốt.
- Checkpoint: kiểm bài bắt buộc/skill/target, chọn đề công bố Người 3, gọi engine Người 4. Kết quả hợp lệ → đạt/trượt → gọi service Người 1 ghi Profile → đóng/mở chặng idempotent.
- Trượt giữ trình độ; học đủ bài không tự tăng CEFR. Không tự ghi user_skill_profiles.
- Sở hữu tổng quan học tập động, lộ trình/bài tiếp theo; nhận Goal/Profile từ 1, nội dung từ 3, kết quả từ 4.
- Summary tiến độ cho admin Học viên và số Lesson cho dashboard admin.

### Quyền ghi và trang

`lessons`, `flashcards`, `lesson_progress`, `user_flashcard_progress`, `learning_paths`, `learning_path_items`. Trang admin Lesson/Flashcard, Student Bài học/ôn thẻ/lộ trình/checkpoint/dashboard. Không dựng engine thi checkpoint riêng.

### Hoàn thành khi

Học/ôn lưu bền; có path định hướng/cá nhân; C/N không trùng/không chia 0; checkpoint và chặng tiếp đúng, không nâng lặp. Có test quyền, đổi Goal, thiếu nội dung, kết quả trả chậm.

## 4. Trâm (Người 3) — Câu hỏi/đề, media và Placement

Branch đề xuất: `feature/catalog-placement`; PR DTO/đề nhỏ → CRUD câu hỏi/đề/media → Placement. Không nhận CRUD chứng chỉ/scoring hoặc ScoringPolicyContract.

### Công việc

- Khi tạo/sửa/publish đề, lấy danh sách chứng chỉ/scheme qua service Người 1; kiểm lại certificate/scheme phù hợp và khả dụng khi ghi, không chỉ tin dropdown. Lưu tham chiếu phiên bản policy theo schema; không tạo Entity/Repository certificate/scheme/band thứ hai hoặc sửa band từ màn đề.
- CRUD Question → Item → Option: SINGLE_CHOICE/MATCHING/ORDERING/WRITING_TEXT/SPEAKING_AUDIO; option đúng item, ordering hợp lệ, tự luận không ép option.
- Media đề và helper kiểm MIME thực/dung lượng/đọc file an toàn dùng chung. Extension không đủ; không nhận path tùy ý. Người 2/4 vẫn kiểm quyền đối tượng của mình.
- CRUD/publish bốn loại đề; kiểm vị trí/section/skill/target, không publish rỗng. DTO FE không lộ đáp án/transcript Listening; grading context chỉ backend.
- Version/snapshot nội dung đã dùng cùng Người 4, tránh sửa câu làm đổi lịch sử. Bàn giao đề nhỏ sớm, gắn scheme/version Người 1 cung cấp; khi policy thật chưa có, dùng mock contract trong test, không tạo policy giả ở cấu hình chạy thật.
- **Placement đầu-cuối:** giới thiệu/chọn/bỏ qua → chọn đề hợp lệ → gọi start/resume engine Người 4 → nhận kết quả → gọi service Profile Người 1 → cập nhật path Người 2.
- Theo PLACEMENT_RETAKE_POLICY.md: không giới hạn tổng lượt; một lượt chưa kết thúc/user/skill; chờ chấm chặn lượt mới cùng skill. Người 4 thực thi concurrency/deadline, Người 3 làm điều hướng/nghiệp vụ đầu vào.
- Chọn kết quả hợp lệ mới nhất theo submittedAt rồi id, không chọn điểm cao nhất hoặc thứ tự AI trả về. Chưa chấm không là 0; Người 1 kiểm lại floor checkpoint khi ghi Profile.
- Sở hữu orchestration bền/idempotent Placement → Profile → Path, dấu bước xử lý để crash/retry không mất cập nhật; cùng 1/2 chốt migration nếu cần. Không reset lịch sử/progress.
- Đầu mối dashboard admin: câu/đề công bố/nội dung mới, nhận số học viên/Lesson/lượt thi qua service 1/2/4.

### Quyền ghi và trang

`questions`, `question_items`, `question_options`, `question_media`, `mock_exams`, `mock_exam_questions`, `mock_exam_sections`. Trang admin câu hỏi/đề, Student Placement và dashboard admin. Không sở hữu certificate/scheme/band, attempt/result/Profile/path.

### Hoàn thành khi

Đề/câu hỏi/media validate/publish thật, từ chối scheme sai certificate/không khả dụng theo service 1. Placement dùng cùng engine, skip/resume/retake đúng, bảo vệ checkpoint. Không áp dụng kết quả cũ/hai lần; test orchestration mock 1/2/4 và tích hợp thật. Không cần tự triển khai phép quy đổi để nghiệm thu luồng mình.

## 5. Dương (Người 4) — Engine thi, chấm khách quan và kết quả

Branch đề xuất: `feature/exam-result`; PR start/save/timer → submit/objective/result → media → tích hợp AI.

### Công việc

- Danh sách/chi tiết đề; một engine cho Placement/Practice/Checkpoint/Full Mock, không tạo engine riêng cho 2/3.
- Render năm dạng câu; lưu/khôi phục đáp án theo item, Writing/Speaking theo V2. Kiểm option đúng item/câu trong đề và chủ sở hữu.
- Deadline backend, reload không reset; chặn sửa sau nộp/hết giờ. Chống start/submit đồng thời bằng transaction/constraint/requestKey lưu bền.
- Listening tối đa hai lượt theo attempt/media qua reload; endpoint có quyền, chốt Range/retry không đếm sai. Không lộ URL tĩnh vượt giới hạn.
- Ghi âm/upload Speaking gắn user/attempt/answer, kiểm quyền phát lại, dùng helper file Người 3. Người 1 không làm recorder/endpoint nhận file thi.
- Nộp xác nhận, lưu/khóa đáp án trước; chấm SINGLE_CHOICE/MATCHING/ORDERING; gọi ScoringPolicyContract Người 1, không viết lại quy đổi. Nhận scheme/version qua đề Người 3; cố định căn cứ chấm, không đổi theo policy mới nhất.
- Tự luận: tạo yêu cầu AI từ bài đã lưu/khóa, chuyển Người 1 sau commit; nhận đánh giá hợp lệ, quy đổi theo policy và lưu điểm kỹ năng/tổng hợp. Không gọi provider AI trực tiếp.
- **Duy nhất Người 4 cập nhật trạng thái attempt và EXAM_SKILL_RESULT**: chỉ hoàn tất khi các phần cần chấm hợp lệ; chờ/lỗi không thành 0. Chặn sự kiện AI trùng/cũ làm thay kết quả mới.
- Trang lịch sử/kết quả Student/Admin: tìm/lọc/phân trang, chi tiết skill, pending/error, feedback, xem lại theo policy; gắn trang/fragment AI của Người 1. Không có trang chấm mẫu admin.
- Service result/history kiểm owner/admin, không trả Entity/grading context cho Thymeleaf. Cung cấp lịch sử cho Học viên (1), Placement (3), checkpoint (2), số lượt thi cho dashboard.
- Thông báo kết quả sau commit cho 2/3; không ghi Profile/path. Nút retry gọi orchestration/service AI của 1 có kiểm quyền, không tùy ý sửa trạng thái DB ở Controller.

### Quyền ghi và trang

`exam_attempts`, `user_answers`, `user_answer_details`, `user_media_activities`, `exam_skill_results`. Giữ Entity/Repository khi trường AI còn ở user_answers. Trang đề/làm/nộp/lịch sử/kết quả Student và kết quả Admin; phản hồi AI chuyên biệt thuộc 1, Placement thuộc 3.

### Hoàn thành khi

Thi/nộp bốn loại cùng engine; save/resume/timer/listen-limit/quyền đúng; chấm/tổng hợp đúng; AI qua 1, retry/notification không trùng; kết quả thật có pending/error/phân quyền. Test đồng thời, hết giờ, submit lặp, DTO an toàn và AI trả chậm.

## 6. Ranh giới service và quyền ghi — bắt buộc

Tên mới bên dưới là hợp đồng **cần bổ sung trong PR contract**, không phải tất cả interface/bean đã có. Xem SERVICE_CONTRACTS.md và `service.contract` hiện tại.

| Service/luồng | Chủ sở hữu | Bên dùng và giới hạn |
|---|---|---|
| AccountAccessContract / Profile apply | 1 | 2/3/4 lấy ACTIVE/context; 3 yêu cầu áp Placement, 2 yêu cầu áp checkpoint; 1 xác minh/ghi Profile, không điều phối Placement |
| LearningPathContract | 2 | 1 đổi Goal, 3 Placement yêu cầu cập nhật path; 1 đọc summary; không ghi Profile trực tiếp |
| ExamCatalogContract | 3 | 2/4 lấy đề công bố; FE chỉ có DTO an toàn |
| ScoringPolicyContract | 1 | 4 gọi quy đổi khách quan/AI theo version; 1 đồng bộ rubric/provider; 4 vẫn ghi điểm/tổng hợp và quyết định trạng thái attempt |
| Service đọc Certificate/Scheme — cần bổ sung | 1 | 3 lấy lựa chọn/kiểm tương thích khi tạo/publish đề; 2 đọc metadata nếu cần; không repository xuyên luồng |
| ExamAttemptContract / service đọc | 4 | 3 Placement, 2 checkpoint, 1 history/ngữ cảnh AI được cấp quyền; 4 giữ attempt/result |
| AiAssessmentContract — đề xuất mới | 1 | 4 yêu cầu chấm/retry sau commit; 1 giữ job/status/provider/feedback, báo đánh giá hoàn tất |
| Placement orchestration — đề xuất mới | 3 | Nhận kết quả 4, gọi 1 ghi Profile rồi 2 cập nhật path; có dấu xử lý bền |
| Media helper | 3 | 2/4 kiểm/đọc file; quyền Lesson/attempt/hai lượt nghe do chủ luồng kiểm |

Người 1 sở hữu cấu hình và phép quy đổi, Người 3 sở hữu đề tham chiếu policy, Người 4 sở hữu điểm thực tế của bài làm. Mỗi bên giữ Entity/Repository bảng mình. Schema hiện thiếu raw→scale chuyên biệt thì 1 chốt công thức/mapping có version với 4, migration mới nếu cần; không mặc định UI band đã có phép quy đổi. Vô hiệu hóa policy chặn dùng mới theo luật đã chốt, không làm mất căn cứ lịch sử. Kiểm lại policy khi publish/start, giữ version/snapshot từ lúc attempt bắt đầu; không âm thầm dùng policy mới khi AI trả chậm.

### 6.1. AI và engine không cùng ghi một trạng thái

1. Người 4 lưu/khóa bài; yêu cầu có assessmentKey, answerId, attemptId, answerVersion, rubric/policyVersion. Payload từ server, không nhận điểm/path client.
2. Người 1 claim tác vụ lưu bền, gọi provider ngoài DB transaction, lưu assessment status/điểm đúng thang/feedback/transcript/provider/model/version/thời gian. Retry phần lỗi, bản cũ không đè bản mới.
3. Người 1 thông báo sau commit. Người 4 đọc assessment đã lưu, kiểm cùng answer/version/policy, áp một lần vào skill result/attempt. Lỗi áp kết quả chỉ retry bước áp, không gọi AI lại.
4. Người 4 thông báo kết quả hợp lệ cho Placement (3)/checkpoint (2). Người 1 không hoàn tất attempt; 2/3 không cập nhật điểm.

**Schema AI chốt trước khi code:** ưu tiên migration mới lưu job/assessment riêng (Người 1 sở hữu) khi được duyệt phù hợp V2. Nếu giữ trường AI trong user_answers, Người 4 giữ Entity/Repository và cung cấp writer nội bộ kiểm answerVersion, chỉ ghi trường AI; Người 1 gọi writer, không tạo Entity/Repository thứ hai hoặc sửa attempt/điểm tổng hợp. Chọn một phương án trong PR schema, không cùng dùng hai đường ghi. Người giữ nền review; không sửa V001–V005.

### 6.2. Placement/Checkpoint và vòng phụ thuộc

- Người 3 điều phối Placement; 2 điều phối checkpoint; 1 duy nhất ghi Profile. Policy newest/floor dùng chung/test chung, không chép thuật toán khác nhau.
- Tách service đọc khỏi orchestration; thông báo sau commit có dấu xử lý bền/phục hồi. Không tạo vòng bean 1 → 4 → 1 hoặc 1 → 2 → 1 rồi dùng lazy để che.
- Không cần broker/cloud: orchestration/tác vụ bền trong ứng dụng local phù hợp; cơ chế/migration cụ thể chốt ở PR contract.
- Controller không gọi Controller, không repository xuyên luồng. Thymeleaf form/session/CSRF, không REST frontend; backend OpenAI là tích hợp ngoài đã chốt.

## 7. File, route và phần chung

| Vùng | Đầu mối |
|---|---|
| Hồ sơ/Goal, admin Học viên, Chứng chỉ & Chấm điểm, phản hồi AI chi tiết | 1 |
| Student Bài học/Flashcard/lộ trình/checkpoint/dashboard; admin Lesson | 2 |
| Student /student/placement; admin câu hỏi/đề; dashboard admin | 3 |
| Student đề/làm bài, /student/results, /student/attempts/{id}/result; /admin/results và chi tiết | 4 |

Tiếp nhận preview theo ADMIN_UI_HANDOFF.md, STUDENT_UI_HANDOFF.md, SCORING_UI_HANDOFF.md, không tạo route trùng. Tách Controller/service/template preview chung trong PR nền nhỏ trước khi nhiều người sửa. Người 1/4 chốt route phản hồi AI khác kết quả toàn bài; đăng ký đường dẫn mới trong ROUTES.md trước khi dùng.

Bạn giữ nền review pom/config/SecurityConfig/WebMvcConfig/CSS/fragment/contract/migration; vẫn có thể là một trong bốn người, không phải người thứ năm. CSS/nav dùng chung không sửa riêng từng trang. Migration mới lấy số theo thứ tự merge, không sửa V001–V005 hoặc chia khoảng số theo người. Không commit password/key/upload runtime.

## 8. Triển khai song song — không đợi CRUD hoặc AI xong

### Giai đoạn 0: chốt nền và contract

- Mỗi máy chạy Java 21/SQL Server, migration/seed cố định và test được; cùng Git workflow.
- Chốt ID/enum/state, requestKey/quyền/NULL/lỗi, DTO đề/item/media, raw/scale/rubric, version/snapshot và result/history.
- 1 + 4 chốt AI job/result/writer; 1 + 2 + 3 chốt Profile/Placement/checkpoint và dấu áp dụng bền. Không để khi tích hợp mới quyết định.
- 1 + 3 + 4 chốt DTO certificate/scheme, quy tắc tương thích/publish/start, policy version và test raw→scale→CEFR. Người 1 bàn giao policy tối thiểu trước CRUD/AI để không trở thành điểm chờ.
- Chốt route/file ownership, test fixture chung; interface/mock trong test, không bean giả trong cấu hình thật.

### Giai đoạn 1: cả bốn bắt đầu cùng lúc

| Người | Đầu việc độc lập | Bàn giao sớm |
|---|---|---|
| 1 | Policy tối thiểu trước, Account/Goal/khóa, CRUD chứng chỉ/scoring; AI với submitted-answer/provider mock | Một Certificate/scheme cố định + DTO chọn/kiểm policy + test quy đổi cho 3/4; ACTIVE/context; AI DTO/writer cùng 4 |
| 2 | CRUD Lesson/Flashcard, học/lưu tiến độ; rule path với context mock | Summary C/N, path states, checkpoint input/output |
| 3 | Đề nhỏ dùng scheme do 1 cấp hoặc mock contract trong test; CRUD câu hỏi/media; Placement với mock engine/Profile | Đề/câu hỏi/đáp án backend và tham chiếu version cho 4 trước CRUD hoàn chỉnh; policy Placement newest/floor cùng 1 |
| 4 | Start/save/timer/submit với catalog mock; result/history/UI | Submitted-answer context/writer; result DTO/trạng thái/thông báo cho 1/2/3 |

Người 1 bàn giao policy tối thiểu và Người 3 bàn giao đề nhỏ ngay đầu đợt, không ai cần xong toàn bộ CRUD trước. Người 1 test scoring/AI không cần UI thi; 4 test engine bằng mock catalog/scoring không cần AI; 2 test path bằng context mock; 3 test đề/Placement bằng mock policy/engine/Profile. Không đưa mock vào cấu hình chạy thật.

### Giai đoạn 2: nối các mạch nhỏ

1. **1 + 3 + 4:** Certificate/scheme và policy nhỏ (1) → đề công bố (3) → engine/lưu/nộp/chấm khách quan/quy đổi → kết quả thật (4), chưa cần AI. Thử thêm scheme không hợp lệ và giữ version lịch sử.
2. **2 + 1:** học/hoàn thành → summary → admin xem; Goal → path, có trường hợp bỏ qua.
3. **4 + 1:** nộp tự luận → AI task bền → assessment → 4 tổng hợp → phản hồi 1 gắn kết quả 4; test bằng provider mock.
4. **3 + 4 + 1 + 2:** Placement → kết quả → Profile → path; làm kỹ năng khách quan trước để không chờ AI, sau test pending/AI trả chậm.
5. **2 + 4 + 1 + 3:** checkpoint → điều kiện/đề → kết quả → nâng/giữ Profile → chặng tiếp.

Merge PR nhỏ sau từng mạch; nhánh khác cập nhật theo Git workflow. Không đợi hết bốn luồng mới ghép. Mock giúp code song song, không thay nghiệm thu thật.

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

