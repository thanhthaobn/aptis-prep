# Đối chiếu triển khai với V2

Bản nghiệp vụ viết lại kèm ERD không thuộc tính và có thuộc tính: [BUSINESS_SPEC_CURRENT.md](BUSINESS_SPEC_CURRENT.md).

Cập nhật 23/10/2026. Yêu cầu gốc: [SPEC_V2.md](SPEC_V2.md). Báo cáo trước sửa: [V2_SCOPE_AND_CODE_REVIEW_2026-10-23.md](V2_SCOPE_AND_CODE_REVIEW_2026-10-23.md). Thay đổi và giới hạn kiểm chứng: [V2_CLEANUP_VERIFICATION_2026-10-23.md](V2_CLEANUP_VERIFICATION_2026-10-23.md).

## Trạng thái triển khai

Các service dưới đây đã có implementation đọc/ghi DB; không còn mô hình “chỉ interface/GET preview”. Có code và test không đồng nghĩa đã nghiệm thu mọi thao tác UI hoặc AI thật.

| Nghiệp vụ | Implementation chính | Quy tắc cần giữ |
|---|---|---|
| Tài khoản, Goal, Profile | AccountProfileService, AccountAccessService | Goal/Placement tùy chọn; target score tùy chọn 0–50, cần target CEFR tương ứng; không có Goal không tự gán C2 |
| Learning Path, Lesson | LearningPathService, LessonTeachingService | Hoàn thành lesson không tự nâng CEFR; tiến độ được lưu; lộ trình định hướng có target nullable |
| Flashcard | FlashcardSetService | Mỗi thao tác ôn mới tăng review_count; retry cùng reviewKey không tính thêm; lịch ôn là mở rộng nội bộ |
| Placement, Practice, Checkpoint, Full Mock | PlacementService, ExamAttemptService | Một Placement đang hoạt động/user/skill; Checkpoint đi từ path; Full Mock mới phải đủ 5 skill |
| Chấm khách quan, AI, kết quả | ObjectiveGradingService, AiAssessmentService, AttemptFinalizationService | Lưu/khóa bài trước khi gọi AI; lỗi giữ NULL; retry cùng phiên bản; Lesson AI không phát event hoàn tất Exam |
| Chứng chỉ, scheme, band, raw→scale | ScoringAdminService, ScoringPolicyService | Scheme đã được đề/attempt dùng không sửa nội dung; tạo version mới sao chép band/rule; có rule và band trước khi sử dụng |
| Question, Exam, Media | CatalogService | QuestionType dùng làm nguồn loại câu hỏi; kiểm skill/type; ORDERING là hoán vị item; câu hỏi trong đề không trùng |
| Quản lý học viên | StudentManagementService | Mở rộng A11–A14; không tạo/xóa học viên, không sửa điểm |

Danh sách quản trị và danh sách đề học viên đọc DB thật. DB rỗng hiển thị rỗng; ID mẫu không tồn tại trả 404. Không dùng fixture làm fallback trong production. `DemoDataConfig` vẫn là cấu hình dev bật có chủ đích; seed SQL đã áp dụng không bị xóa.

## Những lựa chọn đã làm rõ

- `SINGLE_CHOICE` vẫn là nghiệp vụ đang dùng; không xóa enum. `QuestionType` cung cấp danh sách loại và quy tắc skill/options cho phía Java.
- Scheme là tùy chọn cho bài chỉ cần raw score: kết quả không tự suy ra scale/CEFR. Checkpoint cần scheme vì phải xác nhận CEFR. Placement không có CEFR không được tự sinh Profile giả.
- Section là metadata tùy chọn: service hỗ trợ đề một skill liên kết trực tiếp với câu hỏi, không cần lưu section. UI quản trị có editor section để cấu hình Full Mock/thời gian từng skill.
- Checkpoint không dùng ngưỡng cố định 30/50. Kết quả quy đổi phải đạt target CEFR và có điểm dương; không nâng vượt level của checkpoint. Practice/Full Mock không nâng Profile.
- Điểm và CEFR là ước lượng theo policy nội bộ, không khẳng định là thang Aptis chính thức. Rule tuyến tính được cấu hình theo scheme/skill, không thay bằng điểm mẫu.

## Nguồn bài làm và dữ liệu tương thích

`USER_ANSWER.text_answer` là bản tổng hợp Writing, `audio_url`/`audio_manifest` là bản Speaking cấp answer dùng để khóa/chấm. Dữ liệu theo item vẫn được giữ và đồng bộ khi sửa để hỗ trợ nhiều item, khôi phục form và tương thích bài cũ. Đây là mở rộng có chủ đích, không có hai nguồn chấm song song.

V038 bổ sung nguồn AI `EXAM_ANSWER`/`LESSON_SUBMISSION`, manifest âm thanh và bảng chống tính trùng lượt ôn. Migration chỉ backfill text còn trống, không thay text đã có, không xóa item/file/attempt/result. Các ID giả của job Lesson cũ được giữ để truy vết; code dùng source_type/source_id thay vì coi chúng là ID Exam.

Không sửa checksum các migration V001–V037. Không drop các bảng mở rộng cũ chỉ để ERD ít bảng hơn. Cụ thể `lesson_block_listens` chưa có consumer nhưng được giữ vì chưa có quyết định xóa dữ liệu.

## Phạm vi so với ERD ban đầu

V2 đầu tiên có 23 entity; SPEC hiện tại có thêm FLASHCARD_SET. Schema trước đợt sửa có 38 bảng nghiệp vụ; V038 thêm `flashcard_reviews` thành 39. Các phần mở rộng gồm snapshot, media counter, outbox, AI job/history, lesson check/submission, section, SRS và raw-scale policy. Chúng không phải tất cả đều thuộc ERD V2 gốc; không sửa yêu cầu gốc để che sự khác biệt.

Tài liệu này thay mô tả nền/fixture cũ. Tài liệu handoff theo ngày vẫn là lịch sử phân công, không phải bằng chứng về trạng thái chạy hiện tại.
