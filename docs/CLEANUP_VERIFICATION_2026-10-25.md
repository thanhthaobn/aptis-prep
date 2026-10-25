# Kết quả dọn source 24–25/10/2026

## Phạm vi được chốt

- Chủ project chọn một file SQL schema mới nhất + Hibernate validate, không dùng Flyway.
- Chủ project xác nhận xóa/thay source đã sao lưu và **không thay đổi database thật**.
- Không commit/push trong đợt này. Giữ các thay đổi chưa commit đã có và thư mục clone PR riêng.

## Đã thực hiện

- Bỏ 38 SQL migration, dependency Flyway và runner tạo tài khoản demo; bỏ method chỉ phục vụ runner.
- `database/schema-current.sql` dựng 39 bảng/416 cột/62 FK cùng CHECK/default/index, không INSERT tài khoản/nội dung mẫu. DDL chạy trong một batch/transaction, có guard từ chối DB đã có bảng (SQL error 50001); không tự chạy khi ứng dụng khởi động.
- Một `application.properties` với `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và biến môi trường Gemini. Giữ `ddl-auto=validate`, `spring.sql.init.mode=never`.
- Cấu hình local IntelliJ không cần profile dev. Cấu hình riêng cũ chuyển vào `backups/local-config.properties`, không nằm trong classpath hoặc Git.
- Import ngắn thay tên package đầy đủ trong annotation/kiểu Java. Giữ FQN của enum trong JPQL và Spring Security User vì trùng tên entity User.
- Tách QuestionDefinitionValidator, ExamDefinitionValidator và ExamSnapshotService; không thêm interface trung gian. Các thao tác ghi snapshot vẫn ở transaction của ExamAttemptService.
- Dùng enum Skill/CefrLevel chung, giữ danh sách giáo trình A1–C1; gom giới hạn recovery/retry trong AiProcessingPolicy.
- DTO Flashcard chuyển sang dto/view. Metadata nhãn Lesson do backend render, JS không giữ bộ nội dung nhãn thứ hai.
- Năm service dùng JsonMapper do Spring cấu hình. Codec snapshot entity dùng một mapper immutable riêng cho record chỉ có chuỗi/số, không tạo mapper mỗi lần đọc.
- Format các file đã thay đổi, giữ query/lock/transaction, event/outbox, dữ liệu snapshot và các quy tắc V2.
- Cập nhật README, SETUP_LOCAL, PROJECT_DECISIONS, AI_SETUP và đánh dấu tài liệu handoff/demo cũ là lịch sử.

## Dữ liệu thật và legacy

Kiểm tra chỉ đọc: `aptisprep_db` vẫn có lịch sử V031, 37 bảng nghiệp vụ; chưa có cột demo_only/processing_token và playback mới. **DB này chưa tương thích source mới**; không được chạy schema-current.sql lên đó. Cần bước nâng schema riêng, backup DB thật và đối chiếu bảo toàn dữ liệu trước khi thực thi.

`lesson_block_listens` có 0 dòng; số Lesson có content_blocks_json và Lesson Check có block_key đều bằng 0. Không có Java runtime tham chiếu trực tiếp các tên này. Giữ chúng trong schema tương thích, không tự drop bảng/cột DB thật trong phạm vi hiện tại.

Đã lưu ZIP nguồn cũ và patch thay đổi vào `backups/` bị Git ignore. Đây là **backup source/config, không phải backup database**. Không chia sẻ ZIP/config vì có thể chứa thông tin kết nối riêng. Các file migration đã track còn khôi phục được qua Git.

## Kiểm chứng

- `mvn -o -q clean verify` không bật SQL opt-in: 248 Java test, 245 đạt, 3 skip, không failure/error.
- Lượt `mvn -o -q verify` cuối với `APTIS_SQLSERVER_ISOLATED_TESTS=true`: 248 Java test, 246 đạt, 2 skip, không failure/error. Hai skip là Gemini live và kiểm schema DB thật opt-in. Lượt trước từng lỗi do SQLEXPRESS dừng; sau khi chủ máy bật lại, SQL test đã chạy đạt.
- `node --test src/test/js/lesson-author.test.cjs src/test/js/lesson-player.test.cjs src/test/js/flashcard-study.test.cjs`: 11/11 đạt.
- Kiểm tra SQL độc lập trên DB UUID tạm: schema tạo đúng 39 bảng; chạy lại bị từ chối với error 50001.
- Test SQL tích hợp xác minh entity mapping, ERD, đáp án A–F, query AI, index lọc NULL và mọi dòng/cột đã có giữ nguyên khi từ chối cài lại schema.
- JAR sau clean không chứa Flyway, migration, DemoDataConfig hoặc application-dev.properties.

Các test dùng DB tạm chỉ xóa đúng DB UUID tự tạo. Không gọi provider live, không nâng DB thật và không thay đổi file upload của người dùng.

## Đợt tiếp theo: format toàn bộ và tách service

Phạm vi format: 202 file Java chính, 46 file Java test, 52 template HTML, 7 file JavaScript và 3 file test JavaScript. Các script nhúng trong HTML cũng được format. Không đổi bố cục giao diện; giữ whitespace HTML ở chế độ strict. Các thẻ void nằm trong `th:block` dùng dạng tự đóng để parser HTML nhận đúng phạm vi fragment.

Thêm Spotless với Google Java Format AOSP, Prettier phiên bản cố định, `.editorconfig`, quy tắc LF trong `.gitattributes` và lệnh npm. Maven `verify` kiểm format Java; CI kiểm thêm format web và chạy test JavaScript. Xem lệnh sử dụng trong SETUP_LOCAL.md.

Các phần được tách theo trách nhiệm, giữ facade/controller contract và quy tắc hiện tại:

| Service chính | Phần chuyển sang lớp riêng | Ranh giới giữ nguyên |
| --- | --- | --- |
| CatalogService | AdminExamService | Form, lưu và công bố đề; transaction lưu đề/section/question vẫn nguyên tử |
| ExamAttemptService | ExamGradingPayloadFactory | Dựng payload từ snapshot và đáp án đã lưu; giữ prompt từng item, thứ tự, version và giới hạn nội dung |
| ExamAttemptService | ExamAnswerWriter | Facade kiểm chủ sở hữu, tài khoản, trạng thái và khóa attempt trước khi lưu; thao tác ghi dùng transaction của facade |
| ExamAttemptService | AttemptResultReader | Lịch sử/kết quả, bộ lọc và quyền admin; luồng học viên vẫn kiểm quyền trước khi đọc kết quả |
| LessonTeachingService | LessonCheckService | Validation A–F, lưu câu hỏi, chấm Knowledge Check và snapshot; DTO public giữ nguyên |
| LearningPathService | CheckpointStatusReader | Đọc lịch sử và suy ra trạng thái checkpoint; khóa, cập nhật tiến độ và điều kiện mở chặng vẫn ở service chính |

Bỏ helper private không còn được gọi (`chooseItem`, `levelOrder`, `requireCertificate`) và wrapper `getQuestionId` chỉ trả lại getter. Giữ các API/bean/DTO được Spring, Thymeleaf và contract sử dụng; không suy luận code dư chỉ dựa vào việc không thấy lời gọi Java trực tiếp. Không tạo thêm interface chỉ để bọc service.

Test giới hạn Writing chuyển từ reflection vào method private đã tách sang bean dựng payload, giữ toàn bộ assertion về ngưỡng và khả năng sửa bài khi submit bị từ chối. Test danh sách bài còn thiếu chấp nhận whitespace HTML sau format, vẫn kiểm đúng tên/link và loại trừ bài hoàn thành/bài tùy chọn.

Đợt này không thay schema, không chạy DDL/DML lên database thật và không thay đổi file upload hiện có.

Kết quả cuối sau format/tách service:

- `mvn -o -q clean verify` với `APTIS_SQLSERVER_ISOLATED_TESTS=true`: 248 test trong 46 suite, 246 đạt, 2 skip, 0 failure/error; build JAR và Spotless check đều đạt. Hai skip vẫn là Gemini live và validate DB thật opt-in.
- Test SQL Server chạy trên DB UUID tạm `codex_v2_test_a286511a4b0742069dc619947c0950dd`, đã dọn theo cơ chế của test; không dùng DB ứng dụng làm DB test.
- `npm run format:web:check`: đạt toàn bộ HTML/JavaScript/test JavaScript, gồm script nhúng.
- `npm run test:web`: 11/11 đạt.
- `git diff --check`: đạt.

Lượt đầu có hai test hỏng do tên method private và định dạng HTML thay đổi; đã cập nhật cách gọi/nhận diện như mô tả ở trên rồi chạy lại đầy đủ. Không bỏ assertion nghiệp vụ để làm test đạt. Chưa commit/push trong phạm vi yêu cầu này.
