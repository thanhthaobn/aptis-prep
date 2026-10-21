# Bàn giao giao diện học viên — 3 thành viên

Phân công 20/10/2026: Trâm sở hữu đề và engine attempt; Thông sở hữu grading/AI/Profile; Thảo sở hữu Lesson/Flashcard/lộ trình/checkpoint cùng lịch sử/kết quả.

## Thảo

- Lesson theo kỹ năng, Flashcard, dashboard/path/checkpoint.
- Danh sách và chi tiết lịch sử/kết quả, trạng thái rỗng/pending/error, feedback; mọi lượt thi phải kiểm owner/admin.
- Không đọc Repository attempt/result trực tiếp; dùng DTO từ Trâm/Thông và không đưa đáp án đúng/grading context ra HTML.

## Trâm

- Chỉ đưa đề đã công bố và câu hỏi an toàn cho học viên.
- `start/resume/save/deadline/listening/speaking/submit`; khóa đáp án trước khi phát yêu cầu grading.
- Server kiểm thời gian, hai lượt nghe và ownership; tắt nút ở frontend không thay kiểm tra backend.

## Thông

- Chấm khách quan và Writing/Speaking qua AI; retry có version/idempotency.
- Điểm chưa chấm/lỗi giữ NULL; Speaking qua transcript phải ghi rõ chưa đánh giá phát âm/ngữ điệu.
- Ghi skill result/final state và cấp DTO kết quả có quyền cho Thảo.

## Tích hợp

- Placement: Trâm → kết quả Thông → Profile Thông → path Thảo.
- Checkpoint: Thảo kiểm điều kiện → attempt Trâm → result/Profile Thông → Thảo mở chặng.
- Giao diện dùng Thymeleaf/form/session/CSRF, responsive; không dùng REST frontend hoặc fixture khi đã nối dữ liệu thật.
- Chi tiết trạng thái xem [DASHBOARD_STATES.md](DASHBOARD_STATES.md), tiến độ xem [REAL_PROGRESS_CONTRACT.md](REAL_PROGRESS_CONTRACT.md), ranh giới service xem [SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md).
