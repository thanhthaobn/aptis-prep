# Checklist triển khai — Thông, Thảo, Trâm

> Cập nhật 20/10/2026: Dương đã rời dự án. Checklist này thay toàn bộ phân công bốn người trước đó.

## Việc chung trước khi code tiếp

- [ ] Chốt enum attempt/grading/result và trạng thái terminal/non-terminal giữa ba người.
- [ ] Chốt DTO `LockedAttempt`, `GradingResult`, `ResultView`; không truyền Entity qua module hoặc ra giao diện.
- [ ] Kiểm migration mới không trùng version; không sửa migration đã merge.
- [ ] Mỗi bảng chỉ một Entity/Repository và một chủ ghi như TEAM_ASSIGNMENT_V2.

## Thông

- [ ] Hoàn thiện tài khoản, Goal/Profile, khóa/mở học viên và kiểm ACTIVE ở request tiếp theo.
- [ ] Hoàn thiện Certificate/Scheme/Band và policy raw → scale → CEFR có version.
- [ ] Nhận bài đã khóa từ Trâm; chấm khách quan idempotent.
- [ ] Nối AI Writing/Speaking; pending/error giữ điểm NULL; retry không tạo assessment trùng.
- [ ] Ghi/tổng hợp `exam_skill_results`, hoàn tất attempt khi đủ phần chấm.
- [ ] Cung cấp result DTO có owner/admin guard cho Thảo.
- [ ] Test biên điểm, submit event trùng, AI trả chậm/cũ, grading retry và concurrent finalization.

## Thảo

- [ ] Hoàn thiện Lesson/Flashcard, tiến độ, refresh path và checkpoint.
- [ ] Hoàn thiện flashcard tổng kết: đã nhớ, cần học lại, ôn thẻ chưa thuộc, học lại tất cả.
- [ ] Tiếp nhận trang lịch sử/kết quả Student/Admin; tìm/lọc/phân trang và chi tiết kỹ năng.
- [ ] Hiển thị pending/error/feedback đúng; không lộ đáp án hoặc grading context.
- [ ] Nối kết quả checkpoint vào Profile Thông và path; xử lý notification lặp idempotent.
- [ ] Cung cấp summary tiến độ/kết quả cho dashboard học viên và admin.
- [ ] Test owner/admin, user khác, N=0, đổi Goal, kết quả trả chậm và checkpoint submit lặp.

## Trâm

- [ ] Hoàn thiện Question/Item/Option/media, đề và publish validation.
- [ ] Hoàn thiện một engine cho Placement/Practice/Checkpoint/Full Mock.
- [ ] Start/resume/save/timer; chống nhiều attempt chưa kết thúc và requestKey trùng.
- [ ] Listening tối đa hai lượt; Speaking upload có MIME/size/path/ownership an toàn.
- [ ] Submit khóa đáp án, phát yêu cầu grading sau commit; không tự chấm hoặc ghi skill result.
- [ ] Placement dùng result Thông, gọi Profile Thông rồi path Thảo có dấu xử lý bền.
- [ ] Test deadline/reload, option không thuộc item, submit đồng thời, media unauthorized và DTO không lộ đáp án.

## Mốc tích hợp

| Mốc | Đầu ra |
|---|---|
| M1 | Trâm start/save/submit bài objective → Thông chấm/tổng hợp → Thảo xem kết quả |
| M2 | Listening/Speaking có giới hạn/quyền, bài tự luận pending thay vì 0 |
| M3 | Trâm submit Writing/Speaking → Thông AI/retry/finalize → Thảo hiển thị feedback |
| M4 | Placement: Trâm → Thông Profile → Thảo path |
| M5 | Checkpoint: Thảo → Trâm attempt → Thông result/Profile → Thảo mở chặng |

Definition of Done: `mvnw verify` xanh, test quyền/NULL/concurrency liên quan, migration chạy trên SQL Server thử nghiệm, có hướng dẫn kiểm thử và PR chỉ chứa phạm vi của người làm.
