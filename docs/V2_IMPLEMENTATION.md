# Đối chiếu triển khai với V2

Nguồn chuẩn: [SPEC_V2.md](SPEC_V2.md). Phạm vi nền và lựa chọn tích hợp: [../PROJECT_DECISIONS.md](../PROJECT_DECISIONS.md).

Đầu mối và công việc chi tiết từng thành viên: [TEAM_ASSIGNMENT_V2.md](TEAM_ASSIGNMENT_V2.md). A09–A10 quản trị Lesson/Flashcard giao Người 2 cùng luồng học tập.

## Trạng thái

UC-A03–A05 hiện có GET UI danh sách/tạo/xem/sửa chứng chỉ, bộ chấm điểm và khung band CEFR theo skill, thay trang Cấu hình. Không có CRUD/validation nghiệp vụ/DB hoặc engine quy đổi thật. Người 1 tiếp nhận theo SCORING_UI_HANDOFF.md và phân công mới; Người 3 chỉ tham chiếu policy khi quản lý đề.

Đã chốt tài liệu [Placement làm lại](PLACEMENT_RETAKE_POLICY.md): nhiều lượt, một lượt chưa kết thúc/user/skill, dùng kết quả hợp lệ gần nhất và bảo vệ mức checkpoint. Đây là policy/phân công, chưa có code tạo lượt, resume, retry hoặc cập nhật Profile tương ứng.

Nền bổ sung: CI Java 21/verify, interface/DTO liên luồng (chưa implementation), GET Hồ sơ/Placement/Cấu hình admin, chặn static uploads và guard extension/dung lượng. Chưa có endpoint media theo ownership/listen-limit, chưa có chặn session khóa theo request; các phần này vẫn thuộc luồng 1/2/3. Xem SERVICE_CONTRACTS.md. Kiểm tra máy clone thứ hai và SQL Server mới vẫn cần chạy thực tế trước khi ghi hoàn tất.

| Use case | Phần đã có | Phần thành viên cần hoàn thiện |
|---|---|---|
| S01–S03, A01–A02 | Đăng ký, đăng nhập, đăng xuất, phân quyền | Bổ sung các ca lỗi nghiệp vụ khi phát triển |
| S04 Goal | Schema | Entity, service, form, trang mục tiêu |
| S05 Placement | Schema đề/attempt/result | Trâm làm engine/attempt; Thông chấm và cập nhật Profile; Thảo nối path |
| S06 Skill Profile | Schema và vùng hiển thị rỗng | Đọc dữ liệu thật theo tài khoản |
| S07 Learning Path | Schema | Sinh lộ trình theo rule, gồm định hướng khi bỏ Goal/Placement |
| S08 Lesson | Schema | Học bài và lưu tiến độ |
| S09 Flashcard | Schema | Ôn thẻ, cập nhật lượt ôn và ghi nhớ |
| S10 Practice | Schema | Trâm làm bài; Thông chấm/quy đổi, không nâng Profile |
| S11 Checkpoint | Schema | Kiểm tra điều kiện, nâng level một lần, tạo chặng tiếp |
| S12 Full Mock | Schema, mở rộng section tùy chọn | Trâm thi đủ kỹ năng; Thông tổng hợp kết quả |
| S13 Submit | Interface và tích hợp chấm AI | Trâm lưu/khóa attempt; Thông chấm khách quan, AI, persistence và retry |
| S14 Result | Schema | Thảo xem kết quả/lịch sử bằng DTO; Thông cung cấp điểm theo skill |
| A03 Certificate | Schema và dữ liệu khởi tạo | CRUD/disable theo V2 |
| A04–A05 Scoring | Schema và band mẫu | Quản lý version, chống chồng lấn band, chính sách chấm theo skill |
| A06 Question | Schema và QuestionType enum theo V2 | CRUD, validate từng loại câu hỏi |
| A07 Media | Service lưu file local | Upload/preview và metadata gắn câu hỏi |
| A08 Exam | Schema | Tạo đề, chọn câu, sắp xếp và công bố |
| A09–A10 Lesson/Flashcard | Schema | CRUD phía quản trị |
| AI mở rộng | Service Gemini Writing, Speaking qua transcript/audio và cấu hình local; chưa có giao diện gọi chấm | Thông nối assessment vào attempt/result, kiểm định rubric và quy đổi; Thảo hiển thị |

Dashboard hiện chưa đọc số liệu các nghiệp vụ trên. Không dùng số hiển thị tĩnh làm bằng chứng đã hoàn thành use case.

Ngày 17/10/2026 đã dựng lại dashboard học viên dạng bản đồ chặng và trang GET `/student/orientation` để khám phá mà không cần Goal/Placement. Trang chỉ hiển thị nội dung làm quen và Flashcard minh họa, không tạo LEARNING_PATH, LESSON_PROGRESS hoặc chấm bài. Nghiệp vụ sinh/lưu lộ trình định hướng vẫn thuộc Người 2. Dashboard không còn hiển thị Goal/Placement như điều kiện bắt buộc để khám phá.

Giao diện Admin cho Bài học, Ngân hàng câu hỏi, Đề thi và Kết quả đã có route GET danh sách/chi tiết; ba mục nội dung có thêm form tạo/sửa, tất cả dùng fixture. Chưa có ghi dữ liệu hoặc thực hiện nghiệp vụ; các trạng thái “chưa triển khai” ở bảng trên nói về luồng thực tế, không phải thiếu khung UI. Bàn giao tại [ADMIN_UI_HANDOFF.md](ADMIN_UI_HANDOFF.md).

## Phần mở rộng quản lý học viên

Nhóm bổ sung UC-A11–A14 ngoài bản V2 gốc. Đã có Controller GET, preview service chỉ đọc và trang danh sách/chi tiết dùng dữ liệu minh họa; tìm/lọc fixture hoạt động, dialog khóa/mở khóa chưa có POST hoặc thay đổi tài khoản. Người 1 thay preview bằng dữ liệu thật, thêm phân trang và nghiệp vụ khóa; phối hợp Người 2 lấy tiến độ/history/result DTO và Người 3 lấy attempt metadata. Không tạo hoặc xóa học viên. Chi tiết: [STUDENT_MANAGEMENT.md](STUDENT_MANAGEMENT.md).

## Migration chuyển tiếp V004–V005

- Không sửa checksum migration đã chạy. Thay đổi V2 được áp dụng qua V004.
- Dùng tên position, media_url, WRITING_TEXT, SPEAKING_AUDIO trong code mới.
- USER_ANSWER là nơi lưu Writing/Speaking theo V2. Cột tương tự cấp detail cũ chỉ giữ để bảo toàn dữ liệu.
- Đề liên kết trực tiếp với MOCK_EXAM_QUESTION qua mock_exam_id; section chỉ là metadata mở rộng tùy chọn.
- Nếu dùng section, vị trí câu hỏi vẫn duy nhất toàn đề; composite FK đảm bảo section thuộc đúng đề.
- Tiến độ LEARNING_PATH_ITEM và LESSON_PROGRESS phải được cập nhật nhất quán trong service.
- V004 dừng nếu dữ liệu cũ không thể ánh xạ an toàn; không xóa database để vượt lỗi.
- V005 bỏ unique theo section cũ để đề không có section vẫn tái sử dụng câu hỏi đúng V2; unique theo toàn đề vẫn còn.

## Điều kiện nghiệm thu sau khi các luồng được tích hợp

1. Tài khoản mới chọn hoặc bỏ qua mục tiêu/đầu vào và vẫn truy cập được lộ trình phù hợp.
2. Đầu vào tạo kết quả và hồ sơ kỹ năng đúng người dùng.
3. Hoàn thành lesson/ôn Flashcard được lưu và khôi phục sau đăng nhập lại.
4. Practice/Full Mock không tự nâng level; Checkpoint đạt mới nâng và không áp dụng hai lần.
5. Writing/Speaking chưa chấm giữ NULL; lỗi dịch vụ không mất bài; retry không tính trùng.
6. Không xem/sửa attempt, goal hoặc tiến độ của người khác.
7. Admin quản lý đủ các use case A03–A10, có validation và vô hiệu hóa nội dung đã dùng.
8. Database rỗng chạy đủ migration; Entity mapping và giao diện dùng đúng contract V2.
## Bổ sung giao diện học viên: chỉ preview

Đã chốt hướng dẫn triển khai tiến độ thật và điều kiện bỏ nhãn tại [REAL_PROGRESS_CONTRACT.md](REAL_PROGRESS_CONTRACT.md). Đây chỉ là hợp đồng/tài liệu phân công; chưa có service summary, ghi hoàn thành hay dashboard lấy dữ liệu DB.

Đã có danh sách/tìm/lọc và chi tiết Bài học, Đề thi, Kết quả, Flashcard bấm mở nghĩa, menu hoạt động. Dữ liệu riêng cố định, không liên quan tài khoản thật; không có hoàn thành bài học, tạo attempt, audio/ghi âm, timer, lưu/nộp/chấm hay lịch sử DB. Không đánh dấu các nghiệp vụ V2 này là hoàn tất. Xem [STUDENT_UI_HANDOFF.md](STUDENT_UI_HANDOFF.md) để phân công tiếp nhận.

