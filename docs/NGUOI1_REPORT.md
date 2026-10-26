# Báo cáo phần việc Người 1

Ngày cập nhật: 18/10/2026
Repository: `aptis-prep`
Branch: `feature/thong`

## 1. Phạm vi đã hoàn thành

### A. Chính sách chấm điểm

- Thêm Entity/Repository cho `Certificate`, `ScoringScheme` và `ScoreBand`.
- Thêm CRUD MVC cho chứng chỉ, bộ chấm điểm và khoảng điểm CEFR.
- Kiểm tra mã chứng chỉ và version scheme bị trùng.
- Kiểm tra `min <= max`, điểm trong khoảng `0..50`, band chồng lấn hoặc chung biên.
- Không tự gán CEFR khi skill chưa có band, đặc biệt Grammar & Vocabulary.
- Thêm `ScoringPolicyContract` cho raw score → scale score → CEFR.
- Thêm `ScoringSchemeUsageContract` để Owner 3 cung cấp usage signal và bảo toàn lịch sử scheme đã sử dụng.

### B. Tài khoản và quản lý học viên

- Thêm Goal và Skill Profile thật, không dùng fixture hoặc mặc định B2.
- Thêm `AccountAccessContract` với kiểm tra tài khoản Student đang ACTIVE.
- Thêm form Profile/Goal dùng POST MVC, validation, CSRF và redirect sau khi lưu.
- Phát `GoalChangedEvent` sau commit để Owner 2 cập nhật path qua contract.
- Thay Student Preview bằng dữ liệu thật:
  - Danh sách học viên, tìm kiếm, lọc trạng thái và phân trang.
  - Chi tiết hồ sơ, mục tiêu, CEFR, tiến độ và lịch sử thi qua contract.
  - Lock/Unlock riêng biệt, có pessimistic lock và kiểm tra quyền.
- Thêm filter hủy session ở request kế tiếp khi tài khoản bị khóa hoặc inactive.

### C. Certificate và Scoring Admin

- Thêm route GET/POST cho certificate, scheme và score band.
- Chỉ ADMIN được xem và thay đổi cấu hình.
- Disable bằng trạng thái INACTIVE thay vì xóa certificate/scheme.
- Scheme đã được sử dụng không sửa trực tiếp; phải tạo version mới khi có usage signal.
- Tất cả form POST có CSRF token và xử lý lỗi validation/business bằng thông báo tiếng Việt.
- `/admin/settings` redirect về `/admin/certificates`.

### D. AI assessment

- Thêm bảng và Entity riêng cho AI job/assessment, không tạo Repository cho `user_answers`.
- Thêm `AiAssessmentContract` với assessment key, answer version, rubric/policy version, transcript và kết quả.
- Idempotency bằng unique key trong database, có pessimistic claim.
- Gọi provider ngoài transaction DB; lưu kết quả thành transaction riêng.
- Writing dùng Responses API với JSON Schema.
- Speaking hỗ trợ transcription rồi chấm transcript; giới hạn file 24 MB và định dạng âm thanh.
- Retry có giới hạn, không gọi lại bài đã hoàn thành.
- Điểm lỗi/pending giữ `NULL`; lỗi provider được làm sạch trước khi hiển thị.
- Thêm fragment phản hồi và form retry riêng cho Student/Admin.
- Phát event AI sau commit, bao gồm cả trạng thái thành công và lỗi.

### E. Ghi Skill Profile

- Thêm `applyAssessment` cho Placement và Checkpoint.
- Claim bền bằng bảng `user_profile_applications`, mỗi attempt/skill chỉ áp dụng một lần.
- Kiểm tra owner, loại bài, trạng thái hoàn tất, điểm và CEFR từ server.
- Placement mới nhất được xác định theo `submittedAt`, sau đó `attemptId`.
- Checkpoint floor tăng đơn điệu và không bị Placement thấp hơn ghi đè.
- Kết quả failed/pending/error, Practice và Full Mock không nâng profile.
- Attempt cũ và retry cũ không ghi đè kết quả mới.

## 2. Database và tài liệu

- Không sửa các migration V001–V005.
- Thêm migration `V006__add_profile_application_and_ai_assessment.sql` cho:
  - checkpoint/placement metadata của Skill Profile;
  - `user_profile_applications`;
  - `ai_assessment_jobs`;
  - `ai_assessments`.
- Ghi assumptions, giới hạn và điểm cần phối hợp vào [NGUOI1_NOTES.md](NGUOI1_NOTES.md).

## 3. Kiểm thử và verify

Đã chạy:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Kết quả cuối:

- 58 tests chạy.
- 0 failure.
- 0 error.
- Bộ test H2 không phụ thuộc SQL Server; test SQL Server là opt-in.
- Build thành công bằng JDK 21.
- Có test cho scoring boundary, band overlap, account/profile, AI idempotency/retry, Speaking pipeline, CSRF và phân quyền.
- Đã xác nhận kết nối SQL Server local và Flyway chạy V001–V006 trên `aptisprep_db`.
- Lần chạy Spring Boot đầu tiên dừng ở schema validation do Hibernate suy diễn `@Lob` thành `varchar(max)` trong khi SQL Server dùng `NVARCHAR(MAX)`; mapping đã được chỉ rõ `NVARCHAR(MAX)`, nhưng cần chạy lại app để xác nhận startup cuối cùng.

## 4. Git

- Commit `b3415a6`: `feat: implement owner1 account scoring and ai flows`.
- Commit `ee9201f`: `fix: preserve scoring and assessment history`.
- Đã push lên đúng `origin/feature/thong`.
- Không force push, không push main/develop, không tạo PR.

## 5. Giới hạn và điểm cần phối hợp

- Chưa dùng OpenAI API key thật; các test dùng provider mock.
- Raw → scale hiện dùng thang nội bộ 0–50 và cần Owner 4 review mapping Aptis chính thức.
- Owner 3 cần triển khai `ScoringSchemeUsageContract` để cung cấp usage signal từ MockExam.
- Owner 2 cần triển khai `LearningPathContract` để cập nhật path sau GoalChangedEvent.
- Owner 4 cần hoàn thiện `ExamAttemptContract` với `submittedAt`, `passed`, `answerVersion` và flow retry áp kết quả.
- Speaking hiện chỉ đánh giá transcript, chưa đánh giá phát âm, ngữ điệu hoặc acoustic fluency.
- Chưa kiểm thử trên máy clone thứ hai.

## 6. Cấu hình máy

Đã khôi phục Windows về power scheme mặc định Balanced. Máy hiện dùng thời gian sleep/display mặc định của Balanced; không giữ chế độ luôn bật.
