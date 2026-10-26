# Nghiệm thu bộ điểm Grammar & Vocabulary — 26/10/2026

Source nền: `d93cc94`. Script bàn giao:
[complete-grammar-scoring.sql](../database/complete-grammar-scoring.sql).
Quy tắc và cách sử dụng:
[GRAMMAR_SCORING_REPAIR.md](../database/GRAMMAR_SCORING_REPAIR.md).

## Kết quả trên database local

Đã áp dụng lên `aptisprep_db` sau khi sao lưu và chạy thử trên bản restore.
Không thay schema hoặc tự chạy migration khi khởi động.

- Thêm scheme ID **4**, version **1.1-gv-c1**; sao chép 5 raw-scale rule và
  24 band của 4 kỹ năng từ version 1.0, thêm 5 band Grammar A1–C1.
- Grammar scale 0–9.99 → A1; 10–19.99 → A2; 20–29.99 → B1;
  30–39.99 → B2; 40–50 → C1. Đây là policy ước lượng nội bộ.
- Chuyển scheme của 9 đề chưa có attempt: **20, 30, 31, 32, 33, 34, 60, 64, 69**.
- Đề **21** và **74** đã có attempt: giữ nguyên toàn bộ bản cũ, tạo bản tương
  ứng **75** và **76**, với hậu tố `[GV1.1 #21]` và `[GV1.1 #74]`.
- Đề Checkpoint C2 **35** giữ nguyên vì nằm ngoài giáo trình A1–C1 đã chốt.
- Không cập nhật điểm/kết quả cũ, không chấm lại bài, không sửa Profile,
  câu trả lời, snapshot, tiến độ hoặc nội dung câu hỏi.

Các bản 21/74 còn giữ trạng thái PUBLISHED và policy cũ nên có thể xuất hiện là
chưa sẵn sàng trong danh sách; dùng bản 75/76 cho lượt làm mới. Không đổi trạng
thái bản cũ để tránh ảnh hưởng lượt đang làm hoặc lịch sử. Policy 1.0 giữ nguyên,
bao gồm việc chưa có band Grammar; đây là bảo toàn lịch sử, không phải thiếu
cấu hình của version 1.1-gv-c1.

## Sao lưu và bảo toàn dữ liệu

Full backup `COPY_ONLY,CHECKSUM` và `RESTORE VERIFYONLY WITH CHECKSUM` đạt.
File riêng được Git ignore:
`backups/aptisprep_before_grammar_20261026_145935_7676ce8235f542d8a1ba4aefaa5877e4.bak`.

Đối chiếu SHA256 từng dòng gốc, gồm giá trị NULL, trên **40 bảng, 1.547 dòng**.
Mọi dữ liệu gốc được giữ nguyên ngoại trừ `scoring_scheme_id` của đúng 9 đề chưa
có lịch sử được liệt kê. Kiểm riêng các tham chiếu policy để không có đề khác
bị đổi; kiểm nội dung/section/câu hỏi của hai bản sao; metadata schema không đổi.
Mọi kiểm tra thực hiện trước commit trong transaction trên cả bản restore và
DB chính. Manifest/hash/báo cáo chi tiết được giữ trong `backups/`, không lên Git.

## Kiểm thử

- `clean verify`: **266 Java test, 265 đạt, 1 skip, 0 failure/error**.
- SQL Server opt-in đã bật: test schema/preservation, ba test repair mới và
  Hibernate validate trên bản restore đều chạy thật. Skip duy nhất là Gemini
  live; phần AI không cần gọi provider cho thay đổi này.
- Full build chạy từ bản sao source cùng thay đổi hiện tại, dùng
  `application.properties` chuẩn trong Git và mật khẩu qua môi trường. File
  cấu hình DB riêng ở workspace gốc được giữ nguyên, không sửa để vượt test.
- Java Spotless đạt; JAR build thành công. **23/23 JavaScript test đạt**.
- **24 kiểm tra HTTP** trên ứng dụng riêng ở cổng 8094, dùng DB restore:
  Practice Grammar nộp đúng toàn bộ đạt 50/C1 và không cập nhật Profile;
  Placement Grammar đạt 50/C1 và cập nhật Profile của tài khoản thử;
  Full Mock bắt đầu/xem được; Checkpoint vẫn chặn start trực tiếp ngoài lộ trình.
- Sau khi áp dụng DB chính, truy vấn chỉ đọc xác nhận version mới đủ band/rule
  cho cả 5 kỹ năng; Hibernate validate DB chính đạt. Không tạo tài khoản/lượt
  thi thử trên DB chính. Ứng dụng smoke đã được dừng.

Test mới: `GrammarScoringRepairSqlServerTests` kiểm policy/dữ liệu lịch sử gốc,
copy đề đã dùng, section/câu hỏi đúng bản sao, chạy lại sau khi bản sao đã có
attempt, biên Grammar 0/9.99/10/19.99/20/29.99/30/39.99/40/50 và điểm ngoài miền.
Hai ca lỗi kiểm rollback khi thiếu band kỹ năng khác và từ chối ghi đè version
đích đã bị sửa.

Chưa commit/push. Không đưa backup, cấu hình riêng hoặc uploads lên Git.
