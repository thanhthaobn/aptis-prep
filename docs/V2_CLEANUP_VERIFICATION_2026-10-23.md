# Sửa lệch V2 và kiểm chứng — 23/10/2026

Nhánh làm việc: `codex/v2-business-cleanup`, nền `develop` tại `93b2fbd`. Không push hoặc merge trong đợt sửa này.

## Những thay đổi

1. Bổ sung bean ScoringSchemeUsageContract thật, kiểm tra cả đề và attempt; thiếu bean thì dừng an toàn. Khóa scheme khi ghi/cấp cho đề để bảo vệ lịch sử. Tạo version mới sao chép band và raw-scale rule.
2. Thêm form/POST cấu hình raw→scale theo skill; kiểm khoảng hợp lệ, band không ra ngoài scale. Scheme thiếu rule bị chặn trước khi dùng. Snapshot lấy version policy thật.
3. Phân biệt nguồn AI Lesson và Exam. Lesson không mượn attempt/answer ID và không phát event hoàn tất Exam. Job cũ vẫn đọc/retry được theo nguồn thật, kể cả ID trùng.
4. Đồng bộ Writing/Speaking cấp answer khi lưu từng item, giữ toàn bộ item/recording; khi khóa/chấm đọc canonical answer trước và fallback dữ liệu cũ nếu cần.
5. Dùng QuestionType cho danh sách loại, skill/type và usesOptions. ORDERING không cần option; đúng vị trí là hoán vị 1..N. Full Mock mới đủ 5 kỹ năng; chặn trùng câu giữa section, kể cả chỉ trùng một phần.
6. Bỏ mục tiêu C2 tự gán; bổ sung target score trong Goal. Cho phép đề một skill không section/không target; scheme tùy chọn với bài chỉ cần raw, nhưng Checkpoint vẫn cần policy CEFR. Bỏ ngưỡng checkpoint 30/50 không có trong V2.
7. Đếm action ôn flashcard bằng reviewKey, gửi lại cùng key không tăng lần nữa. Sử dụng các cột SRS sẵn có để lưu/lấy lịch ôn; không xóa dữ liệu ôn cũ.
8. Xóa AdminCatalogPreviewService và fallback đề mẫu của StudentContentPreviewService. Controller/view DTO đang nối DB được đổi tên bỏ Preview; xóa nhánh isReal/hasRealEntry/hasData chết, constructor event cũ và dependency/wrapper thừa. Bỏ map khóa JVM tăng vô hạn vì start đã khóa row User đến hết transaction.
9. Gắn nhãn hình minh họa ở landing page. Cập nhật tài liệu triển khai, nguồn AI và nguồn bài làm; không xóa exception/event đang có publisher/consumer.

## Bảo toàn dữ liệu

- Chỉ thêm V038; không sửa migration cũ, không reset DB, không xóa upload hoặc bài thi/kết quả/goal/progress hiện tại.
- Test SQL Server tạo database tên ngẫu nhiên `codex_v2_test_<UUID>`, áp dụng đến V037, bổ sung bài Writing/ghi âm/job Lesson kiểu cũ, chụp toàn bộ cột cũ của các bảng nghiệp vụ rồi nâng V038.
- Test so sánh dữ liệu trước/sau (trừ text_answer được chủ đích backfill), xác nhận text/manifest/source mới và Flyway validate. Chỉ database tạm do test tạo được xóa ở finally; database ứng dụng không được migrate hay clean bởi bài test.
- Đây là kiểm chứng migration trên bộ dữ liệu thử có seed và trường hợp legacy, không phải khẳng định đã kiểm mọi dữ liệu riêng của database ứng dụng. Cần backup database ứng dụng trước lần chạy code mới đầu tiên.

## Cách chạy lại

Kết quả lượt cuối: `mvn clean verify` thành công, 179 test Java được phát hiện: 177 đạt, 0 lỗi/thất bại, 2 skip có chủ đích (Gemini thật và bài migration cũ tác động database ứng dụng). Test migration SQL Server **cách ly** mới đã chạy và đạt, gồm Flyway V037→V038, đối chiếu dữ liệu và Hibernate validate toàn bộ entity. JAR đã được đóng gói thành công. JavaScript: 6/6 test đạt; kiểm tra cú pháp flashcard-study.js đạt. `git diff --check` không có lỗi khoảng trắng.

PowerShell tại thư mục project, Java 21/Maven và SQL Server đang chạy:

```powershell
$env:APTIS_SQLSERVER_ISOLATED_TESTS = 'true'
mvn clean verify
node --test src/test/js/lesson-player.test.cjs src/test/js/flashcard-study.test.cjs
node --check src/main/resources/static/js/flashcard-study.js
```

Test SQL Server đọc cấu hình local không commit trong application-dev.properties; tài khoản cần quyền tạo/xóa database tạm. Không bật biến môi trường này thì test SQL được skip rõ ràng.

## Giới hạn và phần giữ nguyên

- AI thật cần key/quota/provider; test mặc định mock dịch vụ, không tạo kết quả mẫu cho người dùng. Không xem test mock là nghiệm thu Gemini/STT thật.
- Không xóa dữ liệu demo đã tồn tại trong DB hoặc sửa seed migration đã chạy. Không tự xóa bảng legacy có thể chứa dữ liệu, đặc biệt lesson_block_listens.
- Giữ phần mở rộng đã có tài liệu: bộ flashcard cá nhân, quản lý học viên, lesson check/submission, snapshot/outbox/AI job. Event/exception có vai trò transaction, retry và trả lỗi nghiệp vụ, không phải code dư chỉ vì ngoài nội dung đã học.
- CatalogService vẫn là facade cho quản trị câu hỏi/đề/media; chưa chia toàn bộ thành các module riêng. Không có cam kết code hoàn toàn không còn cơ hội refactor hoặc mọi UC đã được kiểm thử thủ công trên trình duyệt.
