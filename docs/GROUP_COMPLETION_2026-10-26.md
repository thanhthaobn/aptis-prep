# Hoàn thiện tích hợp Thảo, Trâm và Thông — 26/10/2026

## Phần đã sửa

| Phạm vi | Thay đổi |
| --- | --- |
| Trâm — bắt đầu đề | `ExamStartValidator` được dùng chung bởi danh sách đề, danh sách Placement và engine. Kiểm câu hỏi tồn tại/đã công bố/không trùng, kỹ năng Aptis, section thuộc đề, câu hỏi khớp section, kỹ năng đề đơn, đủ năm kỹ năng Full Mock, thời lượng dương và file audio đọc được. Đề legacy không có section vẫn được hỗ trợ; đề có section phải gán câu hỏi đúng section. |
| Trâm — Listening | Xóa giọng đọc trình duyệt khỏi trang thi và JavaScript. DTO `MediaPage` chỉ có ID, loại, tên file và content type; không có transcript hoặc answer key. Snapshot có thể giữ transcript phía backend nhưng không truy vấn bổ sung từ Lesson khi mở lại. File audio đi qua endpoint kiểm chủ sở hữu, deadline, token và giới hạn hai lượt nghe. |
| Trâm + Thông — Placement | Bắt buộc bộ điểm thuộc đúng chứng chỉ, đang hoạt động, có raw→scale và band phủ đủ miền điểm. Kiểm trước khi lưu/công bố và trước khi start; đề legacy thiếu cấu hình được báo chưa sẵn sàng. Practice vẫn được chấm raw nếu không có policy. |
| Trâm — chọn Placement | So sánh ID bằng `Objects.equals`, hỗ trợ ID lớn hơn 127. Trang chi tiết chỉ dẫn tới Placement; POST start đề chung cũng chặn Placement. `PlacementService` dùng `startPlacement`; lượt đang làm cùng kỹ năng được dùng lại. |
| Thảo + Trâm — Checkpoint | `LearningPathService` truyền `pathId` qua `startCheckpoint`. Engine kiểm lại DB: đúng chủ sở hữu, ACTIVE, đúng đề/skill/cấp độ A1–C1, có bài bắt buộc và tất cả có COMPLETED + completedAt. Tiền tố request key không cấp quyền. Request key ổn định vẫn chống tạo lượt trùng. Bài không bắt buộc không chặn Checkpoint. |
| Thông + Thảo — Goal | DTO, service và dropdown chặn Goal C2 mới. Goal/Profile/lịch sử C2 cũ được giữ để tra cứu; không đổi điểm hoặc tự hạ trình độ. Khi sửa mục tiêu, người học chọn A1–C1. |
| Thông — policy và trang danh sách | Kiểm tra policy chỉ đọc không đánh dấu rollback khi trả lỗi nghiệp vụ cho trang danh sách. Một đề cấu hình sai không làm hỏng toàn bộ trang. Lưu/công bố/start sai vẫn bị từ chối bởi transaction của thao tác tương ứng. |
| SQL | Chuyển V038 cũ ra `database/legacy/`, đánh dấu không thực thi vì sửa policy đã dùng. Quy trình hiện tại là schema mới nhất + validate; sửa Grammar dùng `database/complete-grammar-scoring.sql` tạo phiên bản riêng. Lượt công việc này không chạy script thay dữ liệu DB thật. |

## Điều kiện nghiệm thu và ảnh hưởng với dữ liệu cũ

- Đề Listening chỉ có `speech://` hoặc mất file audio không còn được bắt đầu. Admin phải tải file âm thanh hợp lệ và tạo phiên bản đề mới nếu đề đã có lịch sử. Không tự sinh audio mẫu hoặc gửi transcript cho trình duyệt để che việc thiếu file.
- Snapshot, câu trả lời, bản ghi âm, attempt, kết quả, Profile và tiến độ cũ không được cập nhật hàng loạt hoặc xóa. Với lượt cũ thiếu audio, hệ thống trả thông báo chưa sẵn sàng; không đổi snapshot để lấy nội dung Lesson đang sửa.
- Placement cũ thiếu bộ điểm không thể dùng để đánh giá trình độ mới cho tới khi có phiên bản đề/policy hợp lệ. Không gán CEFR mặc định.
- Profile vẫn chỉ cập nhật qua nghiệp vụ Placement/Checkpoint đã hoàn tất. Practice/Full Mock không tự nâng Profile.
- Không sửa giới hạn payload Gemini, công thức chấm, cơ chế G01/G02 hoặc quy tắc nghe 90% bài học trong lượt này; chạy lại các test hiện có cho các luồng đó.

## Kiểm thử

Test mới/mở rộng kiểm POST start trực tiếp với thời lượng null/0/âm, section của đề khác, lệch kỹ năng section/đề, Placement thiếu policy/raw→scale/band hoặc policy ngừng hoạt động, publish Placement, đường dẫn bắt đầu riêng, chọn ID 1000, dùng lại lượt đang làm, kết quả Placement có scale/CEFR và áp dụng Profile lặp, Goal C2 mới bị chặn trong khi lịch sử C2 còn nguyên, transcript không xuất hiện trong DTO/HTML, snapshot không đổi theo transcript live, lượt nghe thứ ba bị chặn, file audio thiếu và quyền bắt đầu Checkpoint.

`V2BusinessRegressionTests` và các test mới viết DB đều dùng transaction rollback; kiểm thử web/service thông thường dùng H2 theo cấu hình test. Test SQL Server ghi dữ liệu chỉ chạy trên database tạm có tên ngẫu nhiên; kiểm mapping DB hiện tại chỉ đọc. Khi chạy full build, dùng bản sao source với cấu hình Git để không sửa mật khẩu/config local của chủ project.

Gemini thật là smoke test opt-in qua `GEMINI_API_KEY` và `APTIS_GEMINI_LIVE_TESTS=true`. Test mock/recovery không chứng minh chất lượng chấm thực tế. Không ghi key vào source, tài liệu, log hoặc commit.

### Kết quả đã chạy

- `mvn clean verify`: **284 test, 283 đạt, 1 bỏ qua**, không lỗi; đóng gói JAR và Spotless đều đạt.
- Năm test SQL Server thực sự chạy: `GrammarScoringRepairSqlServerTests` (3), `SqlServerMigrationTests` (1, mapping chỉ đọc) và `V2SqlServerPreservationTests` (1, DB tạm).
- `npm run test:web`: **24/24 đạt**. `npm run format:web:check` và `git diff --check` đạt.
- Test bỏ qua là `GeminiLiveGradingServiceTests`: chưa tìm thấy key trong cấu hình local hoặc biến môi trường khi kiểm tra. Chủ project đang cấu hình key; chưa xác nhận provider thật.
- Full build thực hiện trên bản sao source hiện tại trong `target/group-verification-<UUID>/`, thay riêng cấu hình trong bản sao bằng bản cấu hình Git. Không sửa `application.properties` local và không đưa cấu hình riêng/backups/uploads lên Git.

Sau khi key đã sẵn sàng trong môi trường chạy Maven, smoke test riêng dùng:

```powershell
$env:APTIS_GEMINI_LIVE_TESTS = 'true'
mvn '-Dtest=GeminiLiveGradingServiceTests' test
```

Lượt smoke test này gọi provider thật một lần với đoạn văn kiểm thử, không ghi điểm hoặc lịch sử vào DB. Không chạy nếu chưa cấu hình key, và không coi test bị skip là kết quả nghiệm thu Gemini.
