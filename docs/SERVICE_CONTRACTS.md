# Hợp đồng service cho 4 luồng

## 1. Chủ sở hữu theo phân công mới

Theo [TEAM_ASSIGNMENT_V2.md](TEAM_ASSIGNMENT_V2.md), phương án mới nhất: **1 làm Chứng chỉ & Chấm điểm và AI; 3 làm câu hỏi/đề/media và Placement; 4 làm engine/kết quả toàn bài; 2 giữ hành trình học**. Scoring không còn thuộc Người 3.

Interface/DTO tối thiểu tại `com.aptisprep.service.contract` đã có, chưa có implementation/bean hoặc nối preview. AiAssessmentContract, Placement orchestration và DTO mở rộng dưới đây là hợp đồng đề xuất cần làm trong PR đầu, không phải code đã tồn tại. Không bean giả trong cấu hình chạy thật, không gọi Controller/Repository xuyên luồng.

| Interface/service | Chủ sở hữu | Bên dùng |
|---|---|---|
| AccountAccessContract | 1 | 2/3/4: ACTIVE/context; 3 yêu cầu áp Placement, 2 yêu cầu áp checkpoint |
| LearningPathContract | 2 | 1 đổi Goal, 3 Placement; 1 đọc summary |
| ExamCatalogContract | 3 | 2/4: đề công bố; DTO an toàn và grading context backend tách biệt |
| ScoringPolicyContract | 1 | 4 raw → scale → CEFR theo version; 1 đồng bộ rubric/thang provider; không ghi điểm attempt |
| Service đọc Certificate/Scheme — cần bổ sung | 1 | 3 lấy lựa chọn/kiểm tương thích certificate-scheme khi tạo/publish đề; 2 đọc metadata nếu cần |
| ExamAttemptContract và service đọc | 4 | 3 Placement, 2 checkpoint, 1 lịch sử và ngữ cảnh AI có kiểm quyền |
| AiAssessmentContract — cần bổ sung | 1 | 4 yêu cầu chấm/retry, đọc assessment; 1 thông báo sau commit |
| Placement orchestration — cần bổ sung | 3 | Nhận kết quả 4 → gọi Profile 1 → path 2 |
| Media helper — cần hoàn thiện | 3 | 2/4 kiểm/đọc file; chủ luồng vẫn kiểm quyền đối tượng |

IntegrationModels là DTO không phải Entity/enum DB; ID Integer, deadline Instant. Mã skill/type/state theo V2, nhãn trình bày theo UI_GUIDE. QuestionView hiện tối thiểu; 3/4 phải chốt item/option/media trước render, không trả Entity chứa đáp án đúng lên HTML.

## 2. Quyền, NULL và lỗi

- Student controller lấy email từ Principal, gọi requireActiveStudent; không tin userId từ form. Mọi ghi phải kiểm ACTIVE/ownership lại ở service.
- Context/summary/result/history/assessment chỉ đúng owner hoặc ADMIN có quyền; worker nội bộ dùng ngữ cảnh hệ thống được kiểm, không mở endpoint công khai nhận ID tùy ý. Không trả password hash, secret hoặc grading context.
- Goal/Profile chưa có trả rỗng; không gán B2. progressPercent NULL khi N=0; nextLesson NULL nếu không có bài. C/N theo REAL_PROGRESS_CONTRACT.md.
- Điểm/CEFR/feedback chưa chấm hoặc lỗi giữ NULL, không thành 0. Deadline không đổi khi reload; NULL chỉ nếu policy cho phép.
- Page bắt đầu 0, size mặc định 20, giới hạn 1–100; rỗng trả items rỗng, lỗi DB không thay bằng fixture.
- Không tìm thấy → ResourceNotFoundException/404; sai nghiệp vụ → BusinessRuleException/400; sai quyền → AccessDeniedException/403. Không đưa SQL/provider/secret ra FE.
- Người 1 triển khai kiểm tài khoản khóa ở request tiếp theo của session cũ; Người 2/3/4 kiểm ACTIVE trước ghi. Interface chưa phải filter hoạt động.

## 3. Quyền ghi không chồng chéo

- Người 1 duy nhất ghi User/Goal/Profile, Certificate/ScoringScheme/ScoreBand và sở hữu tác vụ/đánh giá AI; triển khai phép quy đổi; không ghi attempt status hoặc EXAM_SKILL_RESULT.
- Người 2 ghi Lesson/Flashcard/progress/path; không ghi Profile.
- Người 3 ghi question/media/đề và điều phối Placement; chỉ tham chiếu certificate/scheme qua service 1, không Repository/Entity trùng hoặc ghi band/Profile/path/result.
- Người 4 ghi attempt/answer/media activity/skill result, duy nhất tổng hợp và đổi trạng thái lượt thi; không gọi provider hoặc tự ghi Profile/path.

### AI schema/writer — chốt trong PR đầu với người giữ nền

Ưu tiên migration mới cho job/assessment riêng do Người 1 sở hữu khi được duyệt phù hợp V2. Nếu giữ các trường AI trong user_answers theo schema hiện tại: Người 4 giữ Entity/Repository và cung cấp writer nội bộ giới hạn trường AI, kiểm answer/version; Người 1 gọi writer, không tạo Entity/Repository thứ hai. Không để cả hai phương án cùng ghi. Không sửa V001–V005.

DTO yêu cầu AI tối thiểu: assessmentKey, user/attempt/answerId, answerVersion, skill/task, nội dung đã khóa, rubricVersion/policyVersion, thang điểm và transcript nếu có. Tra ngữ cảnh/bản ghi từ server qua 4; không tin đường dẫn hoặc nội dung client làm căn cứ bài đã nộp.

DTO assessment tối thiểu: assessmentId/key, answer/version, jobState (cần ánh xạ trạng thái thật), score đúng đơn vị hoặc NULL, feedback/transcript, rubric/policyVersion, provider/model, thời gian và lỗi đã làm sạch. Chốt enum/đơn vị/độ chính xác/biên với 3/4; không nhận CEFR tùy ý từ FE.

## 4. Chuỗi nộp bài → AI → kết quả

1. **4:** lưu/khóa đáp án trong transaction, tạo yêu cầu bền; chấm khách quan và gọi ScoringPolicyContract của 1 theo version gắn với attempt.
2. **1:** nhận/claim task idempotent sau commit, gọi OpenAI ngoài transaction DB, validate response, lưu assessment trong transaction riêng. AI tắt/lỗi giữ NULL và retry phần lỗi.
3. **1 → 4:** thông báo sau commit; 4 đọc assessment lưu bền, kiểm version/source, áp một lần vào skill result và tổng hợp. Nếu áp kết quả lỗi chỉ retry áp, không gọi provider lại.
4. **4:** hoàn tất attempt khi đủ kết quả theo policy; pending/error không thành điểm 0; thông báo cho orchestration Placement 3 hoặc checkpoint 2.
5. **4 + 1:** trang kết quả tổng quát do 4; phản hồi AI chi tiết/fragment và form retry do 1. Chốt route riêng, CSRF/owner/admin, không có trang chấm mẫu Admin.

requestKey/assessmentKey lưu bền, chống đồng thời bằng transaction/constraint/claim, không Set RAM. Duplicate notification không chấm/tổng hợp hai lần; response cũ không ghi đè version mới. Provider timeout có thể đã xử lý: không cam kết exactly-once phía mạng; retry có kiểm soát, không vô hạn, lưu đủ metadata.

ExamAttemptContract.retryFailedGrading hiện vẫn là entry point engine của 4: kiểm quyền/trạng thái rồi điều phối AI service 1; Người 4 không triển khai provider retry trùng Người 1.

## 5. Placement, checkpoint và chống vòng phụ thuộc

Certificate/Scheme: Người 1 trả DTO id/code/status/version/skill phạm vi và phép kiểm certificate-scheme; Người 3 kiểm lại khi ghi/publish đề, không chỉ dropdown. Người 4 kiểm policy phù hợp khi start, giữ version/snapshot cho lịch sử. Scheme đã dùng không hard delete/đổi ngưỡng tại chỗ; trạng thái ngừng dùng không xóa căn cứ chấm bài cũ. Schema chưa có raw→scale riêng thì 1/4 chốt công thức/mapping theo skill/version và migration mới nếu cần. Không dùng band scale→CEFR để mặc định raw→scale đã hoàn tất. Tách scoring read/evaluation khỏi Profile/AI orchestration của 1 để 3/4 gọi không tạo vòng bean.

- Placement theo [PLACEMENT_RETAKE_POLICY.md](PLACEMENT_RETAKE_POLICY.md): **3 điều phối**, 4 đảm bảo một lượt chưa kết thúc/user/skill/start concurrent/resume/deadline, 1 bảo vệ Profile/floor, 2 cung cấp căn cứ checkpoint/path. Kết quả hợp lệ mới nhất theo submittedAt/id, không max score hoặc thứ tự AI trả về.
- 3 nhận kết quả → gọi AccountAccessContract.applyAssessment của 1 → gọi ensurePaths của 2; lưu dấu bước đã xử lý để retry/crash không bỏ mất path. 1 tự kiểm lại ownership/type/policy/newest/floor, không nhận CEFR tùy ý.
- 2 kiểm điều kiện checkpoint/skill/target, gọi engine 4; applyCheckpoint xác nhận kết quả rồi gọi 1 ghi Profile một lần và đóng/mở chặng. Practice/Full Mock không nâng Profile.
- ensurePaths idempotent; Goal thay đổi archive path/tạo mới và kế thừa completion hợp lệ. completeLesson kiểm quyền/mở khóa, ghi progress/item nhất quán; submit lặp không tăng C.
- Tách read service khỏi orchestration, thông báo sau commit và dấu xử lý lưu bền. Không tạo vòng bean 1↔4 hoặc 1↔2 rồi che bằng lazy.
- Không bắt buộc broker/cloud: cơ chế phục hồi local chốt trong PR contract. Không coi event trong RAM là đủ chống crash.
- Không REST frontend; Thymeleaf/form/session/CSRF. Backend gọi OpenAI theo tích hợp đã được chọn.

## 6. Media và cấu hình

/uploads/** đã denyAll, không static handler; StorageService mới thao tác file. Guard extension/50MB/tên ngẫu nhiên/path traversal hiện có không thay MIME thực/kiểm quyền/giới hạn từng mục đích.

- 3 làm helper MIME/content/size và media đề; 2/4 dùng helper và vẫn kiểm ownership đối tượng.
- 4 sở hữu Speaking recorder/upload, gắn user/attempt/answer, endpoint nghe/phát; Listening kiểm media thuộc đề, trạng thái/giờ và hai lượt, chốt Range/retry.
- 1 lấy transcript/ngữ cảnh đã được cấp quyền, không nhận file path client để grade/load/delete. Speaking qua transcript không chứng minh phát âm; chưa tự bổ sung speech-to-text.
- Không mở uploads để tránh kiểm quyền. Không dùng demo audio thay media đề thật.
- Admin Chứng chỉ & Chấm điểm thuộc 1, không hiển thị/sửa key/password/datasource/path/properties. Key local/biến môi trường; test provider mock không cần key thật.

## 7. Thứ tự bàn giao không phải chờ nhau

1. Cả nhóm chốt DTO/state/ownership/routes; 1+3+4 chốt Certificate/Scheme và raw→scale→CEFR/version; 1+4 chốt schema AI/writer; 1+2+3 chốt Placement/Profile/checkpoint.
2. Làm đồng thời: 1 policy nhỏ trước, account/CRUD scoring/AI bằng mock submitted-answer/provider; 2 lesson/progress/path với context mock; 3 đề/Placement với mock policy/engine; 4 engine/result với mock catalog/scoring/AI.
3. Bàn giao sớm: 1 → 3 Certificate/scheme khả dụng và validator; 1 → 4 ScoringPolicy + test biên/điểm dự kiến; 3 → 4 đề nhỏ tham chiếu version; 4 → 1 submitted-answer/writer; 1 → 4 AI DTO; 1 → cả nhóm ACTIVE/context; 2 → 1 summary. Không đợi CRUD hoàn chỉnh.
4. Nối policy 1 → đề 3 → engine/kết quả 4 trước; mạch học 2↔1; AI 4→1→4; Placement 3→1→2 sau kết quả 4; checkpoint 2→4→2→1. Merge PR nhỏ sau từng mạch.
5. Test tích hợp thật pending/error/retake/khóa session/concurrency/SQL Server. Mock chỉ giúp phát triển song song, không thay nghiệm thu hoặc làm bean giả ở cấu hình chạy thật.

Người giữ nền review contract/config/Security/CSS/fragment/migration. Đây là tài liệu phân công, không tự triển khai nghiệp vụ hoặc đổi interface Java trong lần cập nhật này.
