# APTISPREP — QUYẾT ĐỊNH TRIỂN KHAI THEO V2

## 1. Tài liệu chuẩn và phạm vi

[docs/SPEC_V2.md](docs/SPEC_V2.md) là bản yêu cầu nghiệp vụ chuẩn, được đưa vào repository từ v2.md do nhóm cung cấp. Tài liệu này thay thế các quyết định trước đây khi có khác biệt về schema và nghiệp vụ.

Project đã tích hợp các service nghiệp vụ và DB thật; không còn chỉ là nền/GET preview. Có code và test không đồng nghĩa đã nghiệm thu mọi use case/UI/AI thật. Trạng thái hiện tại: [docs/V2_IMPLEMENTATION.md](docs/V2_IMPLEMENTATION.md); kiểm chứng đợt sửa V2: [docs/V2_CLEANUP_VERIFICATION_2026-10-23.md](docs/V2_CLEANUP_VERIFICATION_2026-10-23.md).

## 2. Công nghệ và cấu trúc

- Java 21, Spring Boot 4.1.1, SQL Server, Maven Wrapper.
- Thymeleaf và Spring MVC: Controller → Service → Repository.
- Session, Spring Security form login, CSRF; không xây REST API cho giao diện.
- Base package: com.aptisprep.
- Từ 24/10/2026 dùng một application.properties, đọc DB_PASSWORD/API key từ môi trường; không commit secret.
- Theo quyết định mới của chủ project, bỏ Flyway và dùng database/schema-current.sql cho DB mới/rỗng, Hibernate validate. Không tự nâng hoặc xóa DB hiện có. Các tên Vxxx bên dưới chỉ giải thích nguồn gốc tương thích dữ liệu cũ, không còn là cơ chế chạy schema.
- Không dùng cloud storage. File lưu local trong uploads/, database lưu đường dẫn.

## 3. Các thay đổi theo V2

V004__align_v2_contract.sql là migration chuyển tiếp, không xóa bảng hoặc cột có dữ liệu:

V005 bỏ unique theo section cũ vì NULL trong unique của SQL Server có thể chặn tái sử dụng câu hỏi ở các đề không có section. Unique theo toàn đề của V004 vẫn được giữ.

- QUESTION → QUESTION_ITEM → QUESTION_OPTION; đáp án đúng ở option.is_correct hoặc item.correct_position.
- Các bảng item/option/media/câu hỏi trong đề/lộ trình dùng position theo V2.
- QUESTION_MEDIA dùng media_url.
- MOCK_EXAM có skill và duration_minutes; MOCK_EXAM_QUESTION có mock_exam_id và vị trí duy nhất toàn đề.
- mock_exam_sections được giữ như phần mở rộng tùy chọn cho thời gian từng kỹ năng. Nếu gán section cho câu hỏi, section phải thuộc đúng đề; mock_exam_id là quan hệ đề chuẩn V2.
- Writing dùng WRITING_TEXT; Speaking dùng SPEAKING_AUDIO.
- USER_ANSWER.text_answer/audio_url là nguồn bài tự luận/nói chuẩn V2. USER_ANSWER_DETAIL phục vụ các câu khách quan.
- USER_ANSWER có ai_score, ai_feedback, graded_at và thông tin truy vết chấm.
- USER_ANSWER_DETAIL vẫn lưu item tự luận để sửa/khôi phục form nhiều item và tương thích dữ liệu cũ. Code đồng bộ text_answer/audio_manifest cấp USER_ANSWER trong cùng transaction; khóa/chấm dùng bản cấp answer. Không xóa detail hay file ghi âm cũ.
- LEARNING_PATH_ITEM có status/completed_at theo V2. Luồng học phải đồng bộ với LESSON_PROGRESS trong cùng transaction.
- Target level của Learning Path có thể NULL khi là lộ trình định hướng.
- Certificate, Scoring Scheme và Score Band phải có chức năng quản lý theo UC-A03–A05; seed cố định chỉ là dữ liệu khởi tạo, không thay thế CRUD.

Migration từ chối chuyển tiếp nếu câu hỏi/vị trí bị trùng trong cùng đề hoặc nhiều bài tự luận ở cấp item không thể gộp an toàn. Khi đó giải quyết dữ liệu cụ thể trước, không reset database của người khác.

## 4. Quy tắc nghiệp vụ phải triển khai

Đã chốt làm lại Placement không giới hạn tổng lượt, một lượt chưa kết thúc mỗi user/skill; resume lượt cũ, chặn tạo lượt mới khi chờ chấm, retry lỗi trên cùng bài. Dùng Placement hợp lệ gần nhất, không lấy điểm cao nhất; không tự hạ dưới trình độ đã đạt qua checkpoint và không xóa tiến độ. Chi tiết và phân công: [PLACEMENT_RETAKE_POLICY.md](docs/PLACEMENT_RETAKE_POLICY.md).

- Đề: PLACEMENT, PRACTICE, CHECKPOINT, FULL_MOCK.
- Kỹ năng: GRAMMAR_VOCABULARY, READING, LISTENING, WRITING, SPEAKING.
- Goal và Placement có thể bỏ qua; khi thiếu dữ liệu, cung cấp lộ trình định hướng theo V2.
- Placement có thể khởi tạo/cập nhật hồ sơ kỹ năng sau khi có kết quả hợp lệ.
- Practice và Full Mock không tự nâng trình độ.
- Checkpoint đạt mới xác nhận cấp độ mới; trượt giữ trình độ cũ.
- Không áp dụng nâng cấp hai lần từ cùng một lượt thi.
- Hoàn thành bài học không tự nâng CEFR.
- Chấm khách quan theo item; chuyển raw → scale bằng chính sách riêng theo skill/scoring scheme. Không dùng một công thức phần trăm cố định cho mọi skill.
- Score Band không được chồng lấn trong cùng scheme và skill.
- Kết quả chỉ là ước lượng khi chưa tái tạo được cách chấm chính thức.
- Nội dung đã được dùng trong lịch sử thi ưu tiên vô hiệu hóa/lưu trữ thay vì xóa.
- Kiểm tra quyền sở hữu attempt, goal và progress ở backend.
- Giới hạn Listening hai lượt là yêu cầu bổ sung nhóm đã chốt; bộ đếm phải ở backend.

## 5. AI cho Writing và Speaking

AI là phần mở rộng tùy chọn của V2. Implementation hiện dùng GeminiGradingService (Gemini), không còn implementation OpenAI cũ. Cấu hình thực tế và giới hạn tại [docs/AI_SETUP.md](docs/AI_SETUP.md).

- WritingGradingService và SpeakingGradingService có GeminiGradingService triển khai.
- Backend gọi provider được cấu hình, kiểm tra đầu ra có cấu trúc; không cung cấp API key cho trình duyệt.
- Speaking dùng transcript từ pipeline nhận diện được cấu hình rồi chấm nội dung. Phương án transcript không đánh giá phát âm/ngữ điệu; phải hiển thị giới hạn đó.
- Điểm từ 0–50, CEFR A1–C2 và phản hồi tiếng Việt; chưa chấm/AI lỗi phải giữ NULL, không gán 0 hay điểm mẫu.
- Chưa bật AI hoặc thiếu key thì vẫn khởi động project bình thường.
- Lỗi quyền truy cập, hạn mức, timeout hoặc kết quả sai không được làm mất bài thi.
- Người phụ trách lượt thi phải lưu bài làm trước khi gọi AI; transaction lưu đáp án không giữ mở trong khi chờ mạng.
- Lưu trạng thái chấm, thông tin provider/model; retry chỉ phần chưa chấm và không nộp lại bài.
- Chấm AI đã nối attempt/result và feedback Lesson. Assessment có source_type/source_id rõ ràng; chỉ EXAM_ANSWER phát event hoàn tất Exam, LESSON_SUBMISSION không dùng ID Exam giả.
- Hướng dẫn bật AI: [docs/AI_SETUP.md](docs/AI_SETUP.md).

## 6. Ba luồng chính và quyền sở hữu

| Người | Luồng | Phạm vi |
|---|---|---|
| 1 | Tài khoản, chấm và kết quả | Hồ sơ, Goal, Skill Profile; Certificate/Scoring, objective/AI grading, quy đổi và tổng hợp result; quản trị học viên |
| 2 | Nội dung học và cuối chặng | CRUD Lesson/Flashcard phía Admin; học tập, Progress, Learning Path và Checkpoint phía Student |
| 3 | Nội dung đề và engine thi | Question/Media, cấu hình đề, Attempt/Answer, Placement và media attempt |

Người 2 làm cả CRUD Lesson/Flashcard phía Admin và hành trình học phía Student để cân bằng khối lượng, giữ đầu mối Entity/Repository của hai bảng này; đồng thời nhận history/result UI và checkpoint. Placement và Checkpoint cùng dùng Exam Engine người 3, không tạo engine riêng. Công việc chi tiết, contract phối hợp và tiêu chí nghiệm thu ở [docs/TEAM_ASSIGNMENT_V2.md](docs/TEAM_ASSIGNMENT_V2.md).

Nhóm bổ sung UC-A11–A14 quản lý học viên ngoài V2 gốc: xem danh sách, tìm kiếm/lọc, xem tiến độ và khóa/mở khóa. Không tạo/xóa học viên hoặc sửa điểm. Đặc tả tại [docs/STUDENT_MANAGEMENT.md](docs/STUDENT_MANAGEMENT.md). StudentManagementService đọc DB thật; khóa phải chặn cả đăng nhập mới và yêu cầu tiếp theo của session đang hoạt động, không chỉ đổi users.status.

## 7. Giao diện và làm việc nhóm

- Đọc docs/UI_GUIDE.md trước khi tạo trang chi tiết.
- Thiết kế mới 17/10/2026: trang chủ/học viên xanh lá, nút/thẻ nổi và bản đồ chặng; quản trị teal riêng, nền sáng. CSS hoàn thiện `learning-theme.css`, chi tiết UI_GUIDE.md.
- Kỹ năng Reading, Listening, Writing, Speaking, Grammar & Vocabulary và Flashcard giữ tiếng Anh. Hành động/hướng dẫn/trạng thái dùng tiếng Việt.
- Không đưa tên thuộc tính, enum hay ghi chú đội phát triển lên giao diện.
- Route và trạng thái triển khai xem docs/ROUTES.md.
- Feature branch → Pull Request → review → merge; không sửa CSS/fragment chung tùy tiện.
- Khi sửa schema, cập nhật schema-current.sql, entity và test; chuẩn bị nâng cấp DB cũ riêng với backup, không tự ghi đè dữ liệu.
- Mỗi luồng phải có validation, phân quyền, trạng thái rỗng/lỗi, dữ liệu kiểm tra và test nghiệp vụ.
