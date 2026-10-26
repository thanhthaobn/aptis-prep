# Bổ sung Grammar & Vocabulary bằng phiên bản policy mới

`complete-grammar-scoring.sql` là script thao tác chủ động trên database đã có
dữ liệu, không được Spring Boot tự chạy và không thay thế `schema-current.sql`.
Sao lưu full database, kiểm chứng backup và chạy thử trên bản restore trước khi áp dụng.

Script cần certificate `APTIS_GENERAL` và scheme `1.0` đang ACTIVE, có đủ năm
raw-scale rule nhưng thiếu band `GRAMMAR_VOCABULARY`. Rule Grammar nguồn phải là
raw 0–50, scale 0–50. Thiếu hoặc khác điều kiện này thì script dừng để kiểm tra.
Không chạy file V038 cũ để chèn band trực tiếp vào policy `1.0` đã được dùng;
script này tạo version mới để giữ nguyên căn cứ của lịch sử.

Tạo scheme mới `1.1-gv-c1`, sao chép nguyên raw-scale rule và các band hiện có
của nguồn. Chỉ thêm band Grammar nội bộ theo phạm vi giáo trình A1–C1:

| CEFR | Điểm scale từ | Đến |
|---|---:|---:|
| A1 | 0.00 | 9.99 |
| A2 | 10.00 | 19.99 |
| B1 | 20.00 | 29.99 |
| B2 | 30.00 | 39.99 |
| C1 | 40.00 | 50.00 |

Đây là quy ước ước lượng nội bộ của ứng dụng, không phải bảng Aptis chính thức.
Các band C2 có sẵn của kỹ năng khác được sao chép nguyên để không thay đổi
quy tắc chấm của chúng; không suy ra giáo trình hỗ trợ C2.

Đề PUBLISHED, không demo, dùng scheme nguồn và có câu Grammar được xử lý như sau:

- Chưa có attempt: chỉ chuyển `scoring_scheme_id` sang phiên bản mới.
- Đã có attempt: giữ nguyên đề cũ; tạo bản đề mới với hậu tố `[GV1.1 #ID]`,
  sao chép section và liên kết câu hỏi, sử dụng scheme mới cho lượt thi tương lai.
- Checkpoint C2 cũ nằm ngoài giáo trình A1–C1 và được giữ nguyên.
- Đề cũ có lịch sử vẫn giữ trạng thái/nội dung/policy cũ. Danh sách có thể còn
  hiển thị chúng là chưa sẵn sàng; chọn bản mới có hậu tố cho lượt làm tiếp theo.

Không cập nhật attempt, answer, snapshot, result, Profile, lộ trình hoặc tiến độ.
Không xóa/vô hiệu hóa policy cũ và không chấm lại bài đã nộp.

Script dùng transaction và application lock. Chạy lại không tạo trùng policy
hoặc bản đề. Nếu version đích đã bị sửa khác cấu hình dự kiến, script từ chối
ghi đè. Nếu policy sao chép không phủ đủ miền điểm ở bất kỳ kỹ năng nào,
transaction bị rollback.

## Kiểm thử

`GrammarScoringRepairSqlServerTests` chạy trên SQL Server UUID tạm khi
`APTIS_SQLSERVER_ISOLATED_TESTS=true`. Test dùng `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD` từ môi trường; không ghi mật khẩu vào source.

Test thực thi đúng script, kiểm bảo toàn lịch sử/policy gốc, sao chép section,
câu hỏi, chạy lại sau khi bản đề mới đã có attempt, các biên Grammar và rollback
khi policy thiếu hoặc version đích bị sửa. Các test Java còn lại kiểm tra
start/submit/ownership, chấm điểm, Placement, Checkpoint và recovery.
