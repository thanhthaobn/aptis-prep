# Luồng và nghiệp vụ của Trâm

## 1. Phạm vi sở hữu

Trâm phụ trách nội dung đề thi và engine làm bài. Phạm vi này bao gồm:

- Ngân hàng `Question`, `Item`, `Option`, `Media`.
- Đề `MockExam`, section và thứ tự câu hỏi trong đề.
- Một engine chung cho Placement, Practice, Checkpoint và Full Mock.
- Start/resume, hiển thị snapshot, lưu đáp án, autosave, timer/deadline, Listening và Speaking.
- Submit, khóa attempt và phát yêu cầu grading cho Thông.
- Kiểm tra owner và trạng thái trước mọi thao tác của học viên.

Trâm không ghi `exam_skill_results`, điểm/CEFR, Profile hoặc Learning Path. Thông chịu grading và Profile; Thảo chịu Learning Path, checkpoint condition, lịch sử và kết quả hiển thị.

## 2. Dữ liệu chính

| Bảng | Vai trò |
|---|---|
| `questions` | Nội dung câu hỏi, skill, loại câu, level và trạng thái Draft/Published |
| `question_items` | Các ý hỏi bên trong một câu; hỗ trợ nhiều item cho Writing/Speaking, Matching và Ordering |
| `question_options` | Các phương án của item và đáp án đúng dùng ở backend grading |
| `question_media` | Audio/media gắn với câu hỏi; Listening có `max_play_count` |
| `mock_exams` | Loại đề, certificate, scoring scheme, skill, level, thời lượng và trạng thái |
| `mock_exam_sections` | Section và thời lượng từng skill; Full Mock có nhiều section |
| `mock_exam_questions` | Liên kết câu hỏi với đề/section và vị trí toàn đề |
| `exam_attempts` | Một lượt làm bài của một học viên |
| `user_answers` / `user_answer_details` | Đáp án theo câu và theo từng item |
| `exam_attempt_snapshots` | Nội dung đề, đáp án đúng, policy/grading context được khóa tại thời điểm start |
| `user_media_activities` | Số lượt phát audio và token phát tạm thời |
| `attempt_outbox` | Yêu cầu submit/grading cần được xử lý lại khi tiến trình bị gián đoạn |

Migration seed mẫu: [V026__seed_tram_exam_catalog.sql](../src/main/resources/db/migration/V026__seed_tram_exam_catalog.sql). Migration này tạo câu hỏi đủ năm kỹ năng, audio Listening, Placement/Practice, Checkpoint A1–C2, Full Mock nhiều section và bản ghi Draft cho Admin.

## 3. Luồng Admin quản lý câu hỏi và đề

```text
Admin tạo Question
  → thêm Item
  → thêm Option hoặc Media nếu loại câu yêu cầu
  → validate skill/level/type/item/đáp án
  → lưu Draft
  → publish Question

Admin tạo Mock Exam
  → chọn Certificate + Scoring Scheme
  → chọn exam type, level, thời lượng
  → tạo một hoặc nhiều Section
  → chọn và sắp xếp câu hỏi
  → kiểm tra skill của câu khớp Section
  → lưu Draft
  → publish Exam
```

Các route chính:

| Nghiệp vụ | Route |
|---|---|
| Danh sách câu hỏi | `GET /admin/questions` |
| Tạo/sửa câu hỏi | `GET /admin/questions/create`, `GET /admin/questions/{id}/edit` |
| Lưu câu hỏi | `POST /admin/questions`, `POST /admin/questions/{id}/edit` |
| Publish câu hỏi | `POST /admin/questions/{id}/publish` |
| Thêm/xóa media | `POST /admin/questions/{id}/media`, `POST /admin/questions/{id}/media/{mediaId}/delete` |
| Danh sách đề | `GET /admin/exams` |
| Tạo/sửa đề | `GET /admin/exams/create`, `GET /admin/exams/{id}/edit` |
| Lưu đề | `POST /admin/exams`, `POST /admin/exams/{id}/edit` |
| Publish đề | `POST /admin/exams/{id}/publish` |

Một đề Published chỉ được dùng khi có câu hỏi Published, section hợp lệ, skill khớp và không lặp câu hỏi/vị trí trong cùng đề.

## 4. Các loại đề và điểm bắt đầu

### Placement

Học viên chọn skill ở `/student/placement`. Backend chỉ trả các đề `PUBLISHED` có `exam_type = PLACEMENT` và đúng skill. Khi start, PlacementService kiểm tra đề qua catalog rồi gọi cùng `ExamAttemptService.start`; không có engine Placement riêng.

```text
GET  /student/placement?skill=READING
POST /student/placement/start
  → ExamCatalogContract.publishedExam
  → ExamAttemptContract.start
  → redirect /student/attempts/{attemptId}
```

Attempt Placement đang chạy được resume; attempt terminal không được dùng lại. Kết quả hoàn tất mới được bàn giao cho Thông cập nhật Profile và sau đó Thảo cập nhật Learning Path.

### Practice và Full Mock

Practice có thể bắt đầu từ danh sách/chi tiết đề. Full Mock dùng cùng engine nhưng có nhiều section/skill; toàn bộ câu hỏi vẫn được khóa trong một snapshot và dùng chung deadline của attempt.

```text
POST /student/exams/{examId}/start
  → kiểm tra đề Published
  → tạo snapshot và answer rỗng
  → tạo attempt IN_PROGRESS
  → redirect /student/attempts/{attemptId}
```

### Checkpoint

Checkpoint không được bắt đầu bằng route generic của danh sách đề. Thảo kiểm tra bài bắt buộc và chặng hiện tại trước, sau đó gọi route:

```text
POST /student/learning-path/{pathId}/checkpoint/start
  → kiểm tra path ACTIVE
  → kiểm tra các bài required đã COMPLETED
  → tìm đề CHECKPOINT Published đúng skill/target level
  → gọi ExamAttemptContract.start với request key checkpoint
  → redirect tới attempt hoặc result
```

Frontend hiển thị nút “Mở từ lộ trình học”; backend cũng chặn request generic để không bypass điều kiện của Thảo.

## 5. Luồng làm bài FE–BE

```text
Student mở attempt
  → backend kiểm owner + trạng thái + deadline
  → đọc câu hỏi từ exam_attempt_snapshots
  → frontend render option/item/media, không nhận đáp án đúng

Student chọn đáp án/nhập Writing
  → POST /answers hoặc POST /answers/autosave
  → backend kiểm question/item/option thuộc snapshot
  → lưu user_answers và user_answer_details

Student ghi Speaking
  → multipart POST /answers/audio
  → kiểm MIME, chữ ký file, dung lượng và owner/attempt
  → lưu file theo attempt, question và item

Student nghe Listening
  → POST /media/{mediaId}/play
  → backend tăng bộ đếm tối đa 2 lượt và cấp token ngắn hạn
  → GET /media/{mediaId}/content?token=...

Student nộp bài
  → POST /attempts/{id}/submit
  → khóa attempt/answer, cập nhật answerVersion
  → tạo LockedAttempt từ snapshot
  → gửi yêu cầu grading cho Thông qua outbox/event
```

Các route làm bài:

| Nghiệp vụ | Route |
|---|---|
| Mở attempt | `GET /student/attempts/{id}` |
| Lưu đáp án | `POST /student/attempts/{id}/answers` |
| Autosave | `POST /student/attempts/{id}/answers/autosave` |
| Upload Speaking | `POST /student/attempts/{id}/answers/audio` với `multipart/form-data` |
| Xin lượt phát audio | `POST /student/attempts/{id}/media/{mediaId}/play` |
| Đọc audio | `GET /student/attempts/{id}/media/{mediaId}/content?token=...` |
| Xem audio Speaking đã lưu | `GET /student/attempts/{id}/answers/{answerId}/audio` |
| Submit | `POST /student/attempts/{id}/submit` |
| Xem kết quả | `GET /student/attempts/{id}/result` |

## 6. State machine của attempt

```text
IN_PROGRESS
  ├─ save/autosave ───────────────→ IN_PROGRESS
  ├─ deadline đến ────────────────→ EXPIRED
  └─ submit ──────────────────────→ SUBMITTED hoặc WAITING_GRADING

WAITING_GRADING
  ├─ grading thành công đầy đủ ───→ COMPLETED
  └─ grading lỗi ─────────────────→ ERROR

EXPIRED
  └─ được giữ là hết giờ; không cho sửa/nghe/upload thêm
```

Frontend timer chỉ hiển thị thời gian còn lại. Backend luôn tính lại deadline khi page, save, media hoặc submit để reload, request trễ và nhiều tab không thể gia hạn attempt.

## 7. Quy tắc bảo vệ bắt buộc

- Mọi thao tác attempt phải kiểm tra `userId` sở hữu; không tin `userId` từ form.
- Question, item, option và media phải thuộc snapshot/attempt tương ứng.
- Đáp án đúng chỉ nằm ở snapshot backend và không được đưa vào HTML học viên.
- Snapshot giữ nội dung đề, thứ tự, đáp án đúng, certificate, scoring scheme, policy version và answer version.
- Attempt terminal không được sửa đáp án, upload Speaking hoặc phát lại Listening.
- Audio Listening có bộ đếm bền trong database và giới hạn hai lượt kể cả khi reload.
- Speaking nhận file multipart; client không được tự gửi `audioUrl` tùy ý.
- Request key phải ổn định theo nghiệp vụ để start lặp trả lại attempt đang chạy, nhưng sau attempt terminal phải cho phép retake theo policy.
- Submit và event grading phải idempotent; không tạo nhiều kết quả khi hai tab cùng submit.

## 8. Bàn giao giữa ba người

```text
Trâm khóa attempt
  → LockedAttempt + answerVersion + scheme/policy/rubric context
  → Thông chấm objective/AI và ghi exam_skill_results/Profile
  → Thảo hiển thị ResultView và cập nhật checkpoint/Learning Path
```

Trâm chỉ chịu trách nhiệm vòng đời attempt và nội dung đề. Khi cần thay đổi scoring, Profile hoặc trạng thái Learning Path, phải dùng contract/service của Thông hoặc Thảo thay vì ghi trực tiếp repository của phần đó.

## 9. Kiểm thử bàn giao

Các nhóm test cần có khi nghiệm thu:

- User khác không xem/sửa/nộp attempt hoặc phát media.
- Start đồng thời không tạo duplicate attempt.
- Submit đồng thời chỉ phát một grading request.
- Reload giữ nguyên đáp án, deadline, snapshot và số lượt nghe.
- Lượt nghe thứ ba bị từ chối ở backend.
- File Speaking giả đuôi, sai MIME, quá dung lượng và path bất thường bị từ chối.
- Writing/Speaking nhiều item không ghi đè dữ liệu item khác.
- Checkpoint không thể bypass điều kiện bài học bằng route start generic.
- Full Mock có đủ nhiều section/skill trong cùng một attempt.
- Outbox/event submit có thể retry mà không tạo kết quả hoặc cập nhật Profile trùng.
