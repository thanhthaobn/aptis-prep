# Ghi chú Người 1 — phạm vi tài khoản, scoring và AI

## Đã chọn

- Không sửa V001–V005. V006 chỉ bổ sung dấu vết áp dụng Profile và bảng job/assessment AI của Người 1.
- Raw score được hiểu là điểm kỹ năng nội bộ trong khoảng 0–50; version hiện tại giữ nguyên thang 0–50 rồi làm tròn HALF_UP đến 2 chữ số. Đây là estimated/mock result, không phải ngưỡng Aptis chính thức.
- Score Band là scale → CEFR. Grammar & Vocabulary không được tự sinh band hoặc tự gán CEFR khi chưa có band.
- Khoảng band là đóng ở hai đầu; chung endpoint bị từ chối. `min <= max`, tối đa 50.00.
- Profile giữ `checkpoint_floor_level` và `checkpoint_source_attempt_id`, nên Placement mới không thể hạ mức đã xác nhận bởi Checkpoint. `user_profile_applications` là claim bền để một attempt/skill chỉ áp dụng một lần.
- V006 còn lưu `placement_level`/`placement_submitted_at` và lần checkpoint gần nhất để kết quả cũ không ghi đè kết quả mới; `source_attempt_id` ưu tiên Placement gần nhất, còn floor checkpoint giữ căn cứ riêng.
- Goal chỉ ghi qua POST MVC có CSRF; sau commit phát `GoalChangedEvent` để LearningPathContract (nếu đã có bean) tự `ensurePaths`, không ghi bảng path từ module này.
- AI job/assessment tách bảng riêng để Người 1 sở hữu; không tạo Entity/Repository thứ hai cho `user_answers`. Provider được gọi ngoài transaction lưu assessment.
- Các form POST MVC render hidden CSRF token; event hoàn tất AI chỉ phát sau commit, còn claim Profile khóa pessimistic theo user trước khi ghi dấu áp dụng.

## Cần chốt với nhóm

- Người 4 cần cung cấp implementation `ExamAttemptContract.result/history` với `submittedAt`, `SkillResult.passed`, `answerVersion` và trạng thái hoàn tất. Profile service không tự đọc repository attempt/result.
- Người 2 cần cung cấp `LearningPathContract` để admin học viên hiển thị summary thật; khi chưa có bean, giao diện hiển thị `Chưa có dữ liệu lộ trình`, không dùng fixture.
- Raw → scale chính thức theo từng skill/scheme/version chưa có trong schema V001–V005. V006 chưa thêm bảng conversion; công thức 0–50 giữ nguyên là phương án an toàn tạm thời, cần Người 4 review trước khi dùng cho điểm Aptis.
- AI request cần payload bài đã khóa do Người 4 cấp qua `AiAssessmentContract`; service không nhận CEFR hoặc file path từ form frontend.
- Speaking hiện dùng transcript để đánh giá ngôn ngữ và task fulfilment; không đánh giá phát âm/ngữ điệu/acoustic fluency.
- Checkpoint không đạt vẫn được claim bền nhưng không thay đổi Profile; trạng thái chờ/lỗi hoặc thiếu dữ liệu hợp lệ không tạo mức CEFR.
- Contract Certificate/Scheme trả cả skill đã có band và từ chối scheme thiếu band cho skill được yêu cầu; Grammar & Vocabulary vì vậy không được tự suy ra CEFR.
- Việc khóa sửa scheme/version hoặc band đã được MockExam sử dụng cần Owner 3 cung cấp usage signal/contract; trong phạm vi Người 1 hiện chưa truy vấn bảng `mock_exams` chéo module.
- `ScoringSchemeUsageContract` là cổng tùy chọn của Owner 3; khi bean cung cấp usage signal, mọi sửa scheme cùng version và mọi thêm/sửa/xóa band của scheme đã dùng đều bị từ chối, buộc tạo version mới. Khi Owner 3 chưa có bean trong repo hiện tại, không có luồng MockExam nào đủ khả năng dùng scheme để phát sinh lịch sử.

## Giới hạn kiểm tra lần này

- JDK 21/Maven Wrapper đã kiểm tra.
- SQL Server localhost:1433 không truy cập được; Docker daemon không khả dụng trong môi trường hiện tại. Chưa xác nhận Flyway V001–V006 trên SQL Server thật.
- Không dùng API key OpenAI thật; chỉ provider mock/unit test khi môi trường cho phép.
