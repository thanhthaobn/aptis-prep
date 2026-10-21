# Hợp đồng service cho 3 thành viên

## 1. Chủ sở hữu

| Contract/service | Chủ | Trách nhiệm |
|---|---|---|
| `AccountAccessContract` / Profile | Thông | ACTIVE/context, áp Placement/Checkpoint hợp lệ |
| `ScoringPolicyContract` | Thông | raw → scale → CEFR theo policy version |
| Grading/Result service | Thông | chấm objective/AI, ghi skill result, finalize |
| `LearningPathContract` | Thảo | complete Lesson, refresh/archive path, checkpoint transition |
| Result query/presentation | Thảo | lịch sử/chi tiết Student/Admin từ DTO an toàn |
| `ExamCatalogContract` | Trâm | đề công bố, question view và grading context backend |
| `ExamAttemptContract` | Trâm | start/resume/save/deadline/submit/locked attempt |
| Placement orchestration | Trâm | result → Profile Thông → path Thảo |
| Media helper | Trâm | MIME/content/size/path; chủ đối tượng vẫn kiểm quyền |

## 2. Quyền ghi

- Thông ghi User/Goal/Profile, Certificate/Scheme/Band, assessment và `exam_skill_results`; chỉ Thông finalize grading.
- Thảo ghi Lesson/Flashcard/progress/path; chỉ đọc attempt/result qua contract.
- Trâm ghi question/media/exam, `exam_attempts`, answer/detail/media activity; không ghi Profile/path/skill result.
- Trâm sở hữu trạng thái attempt đến khi bài đã khóa; Thông sở hữu chuyển trạng thái trong grading/finalization. Hai bên phải dùng method có precondition/version, không `setStatus` tùy ý ở Controller.

## 3. DTO bàn giao tối thiểu

`LockedAttempt`: attemptId, userId, exam/type, requestKey, submittedAt, deadline, certificate/scheme/policyVersion, answerVersion và grading context backend. Không lấy nội dung/điểm từ client.

`GradingResult`: attemptId, skill, state, raw/scale/CEFR hoặc NULL, policy/rubric/answerVersion, feedback đã làm sạch và thời gian. Sự kiện cũ/sai version bị từ chối.

`ResultView`: metadata bài thi, trạng thái, điểm theo skill, pending/error, feedback được phép xem. Không chứa đáp án đúng, secret, file path hoặc Entity.

## 4. Chuỗi nghiệp vụ

1. Trâm lưu và khóa đáp án trong transaction; phát sự kiện/request bền sau commit.
2. Thông claim idempotent, chấm objective; AI chạy ngoài transaction và lưu assessment riêng.
3. Thông kiểm answer/policy version, ghi skill result một lần và finalize khi đủ kết quả; pending/error không thành 0.
4. Thảo đọc kết quả qua guard owner/admin, hiển thị lịch sử và áp checkpoint một lần.
5. Placement do Trâm nhận kết quả cuối, gọi Thông ghi Profile rồi Thảo cập nhật path.

## 5. Quy tắc chung

- Student lấy email từ `Principal`; service kiểm ACTIVE/ownership lại, không tin userId từ form.
- Not found → 404, sai nghiệp vụ → 400, sai quyền → 403; không lộ SQL/provider/secret.
- Deadline không đổi khi reload; requestKey/assessmentKey lưu DB, không dùng `Set` RAM để chống trùng.
- Điểm/CEFR/feedback chưa chấm giữ NULL. Profile không nhận CEFR tùy ý từ frontend.
- `/uploads/**` không public. Range/retry Listening không được đếm lượt sai; Speaking gắn user/attempt/answer.
- Không tạo engine riêng cho Placement hoặc Checkpoint; cả hai dùng engine Trâm.
- Không tạo vòng bean: ưu tiên read contract và event sau commit. Test mock giúp phát triển nhưng không thay test tích hợp SQL Server.
