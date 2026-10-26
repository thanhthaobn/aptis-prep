# Nâng schema DB local và kiểm chứng ứng dụng — 25/10/2026

> Cập nhật 26/10: thiếu band Grammar của policy 1.0 được xử lý bằng version mới
> 1.1-gv-c1, giữ nguyên policy/lịch sử cũ. Xem
> [biên bản kiểm chứng](GRAMMAR_SCORING_VERIFICATION_2026-10-26.md).
> Mô tả thiếu cấu hình bên dưới là hiện trạng tại thời điểm 25/10.

## Source và phạm vi

Runtime source local khớp `origin/develop` tại `0ec2019` (PR #36), gồm phần Trâm đã merge và phần refactor/format. Không sửa source nghiệp vụ trong đợt nâng DB này. Bổ sung báo cáo và cập nhật SETUP_LOCAL; chưa commit/push tài liệu mới.

Database được nâng: `aptisprep_db`, SQL Server local `localhost:1433` / SQLEXPRESS. Không chạy `schema-current.sql` lên DB này, không drop/recreate DB, không xóa bảng/cột/row cũ, không sửa mật khẩu hay nội dung người dùng.

## Sao lưu và rehearsal

- Full backup `COPY_ONLY,CHECKSUM`, `RESTORE VERIFYONLY WITH CHECKSUM` đạt.
- File local, bị Git ignore: `backups/aptisprep_before_schema_20261025_113631_b7e473b6463542b1b03d55f67633d52a.bak` (9.621.504 byte).
- SHA256: `C2D2178F1AA16B61AF09A2071C487271A9533A131789B5CB685917145DB3541E`.
- Đã restore chính backup này sang DB UUID riêng, nâng thử, validate mapping Hibernate và chạy 29 kiểm tra HTTP trước khi áp dụng lên DB thật.
- DB UUID thứ hai dựng schema chuẩn để so sánh metadata. Các DB thử được dọn sau khi kiểm chứng; giữ backup gốc, script và kết quả trong `backups/`.

Backup chứa dữ liệu thật, chỉ giữ riêng trên máy; không đưa lên Git hoặc chia sẻ công khai. Các script nâng DB có guard, transaction và báo cáo hash nằm trong `backups/`, không phải migration tự chạy cùng ứng dụng.

## Thay đổi đã áp dụng

- Thêm `scoring_scale_rules` và `flashcard_reviews` cùng FK, CHECK, unique/index tương ứng.
- Thêm tổng cộng 18 cột vào các bảng đã có: nguồn job AI, processing token, audio manifest, cờ đề demo, lịch ôn flashcard và phiên nghe được server xác minh.
- Cho phép NULL ở ba cột liên kết AI theo schema mới; giữ nguyên mọi giá trị ID legacy hiện có.
- Mở CHECK `lesson_checks.correct_option` từ 0–2 sang 0–5 bằng cách tìm constraint cũ theo metadata, giữ nguyên đáp án đã có.
- Khôi phục 5 raw-scale rule nội bộ đúng V036 cho scheme `1.0` theo xác nhận của chủ project. Thêm 5 rule tương tự cho `0.1-A1` theo xác nhận riêng, giữ nguyên band A1. Không coi đây là thang điểm Aptis chính thức.
- Không seed tài khoản/đề/nội dung demo. Không tạo band CEFR mặc định để lấp cấu hình thiếu.

Schema nghiệp vụ sau nâng khớp schema chuẩn: 39 bảng, 416 cột, 36 CHECK, 90 index; metadata của mọi cặp cột FK khớp. Bảng lịch sử Flyway cũ giữ nguyên, nên DB thực tế có tổng 40 bảng và 426 cột. Lịch sử V031 được giữ để tra cứu, không thêm bản ghi Flyway giả.

## Bảo toàn dữ liệu

Đối chiếu SHA256 của từng dòng (gồm tất cả cột cũ, có phân biệt NULL), sau đó gom hash theo bảng để kiểm số dòng và nội dung. Toàn bộ **38 bảng cũ, 1.530 dòng** giữ nguyên qua rehearsal, transaction nâng thật và sau khi app khởi động. Chỉ thêm 10 dòng cấu hình vào bảng raw-scale mới theo hai xác nhận của chủ project.

Giữ nguyên 2 user, 49 lesson, 80 knowledge check, 58 exam, 40 question, 5 attempt, 36 answer, lịch sử lesson/flashcard và tiến độ. Không chạy đăng ký, nộp bài hay sửa dữ liệu thử trên DB thật; tài khoản và attempt smoke chỉ tạo trên bản restore.

Kiểm tra các tham chiếu media hiện có: 12 media câu hỏi, 6 audio bài học, 9 ảnh và 4 video đều tìm thấy ở uploads hoặc tài nguyên curriculum trong classpath. Không xóa/thay file upload.

## Kiểm thử và chạy ứng dụng

- `mvn -o -q clean verify` với cả hai SQL opt-in: **248 test, 247 đạt, 1 skip, 0 failure/error**. Skip duy nhất là Gemini live; validate DB thật và kiểm schema trên DB tạm đều đã chạy.
- `npm run format:web:check` đạt; `npm run test:web`: **11/11 đạt**.
- 29 HTTP smoke trên DB restore: đăng ký, đăng nhập học viên/admin, kiểm quyền, dashboard, profile, goal, Placement, Lesson, Flashcard, Exam, kết quả, quản trị và bắt đầu/xem bài thi Reading.
- 12 HTTP kiểm tra chỉ đọc trên DB thật: trang chủ/login/register và 7 file JavaScript trả 200; trang học viên/admin chưa đăng nhập chuyển về login đúng.
- `DBCC CHECKDB` đạt; app khởi động với Hibernate validate, không có ERROR trong log tại thời điểm kiểm tra.
- Ứng dụng chạy local tại `http://127.0.0.1:8080`, dùng DB thật đã nâng, AI live tắt trong phiên smoke. PID/log riêng lưu trong manifest ở `backups/`.

Môi trường chạy process của Codex trên Windows gặp lỗi socket NIO ở thư mục temp mặc định. Phiên chạy thử đã dùng `jdk.net.unixdomain.tmpdir` trỏ vào thư mục tạm trong workspace và khởi động thành công. Không đưa workaround này vào source nghiệp vụ hay cấu hình chung. Khi chạy từ IntelliJ, vẫn dùng cấu hình local và biến môi trường riêng.

## Chức năng và giới hạn còn lại

Không phát hiện mất chức năng do phần tách service/format qua các test và smoke nêu trên. Các luồng nghiệp vụ chính vẫn có trong code. Điều này không chứng minh mọi tình huống ngoài bộ test đều không có lỗi; AI provider live và kiểm tra giao diện bằng mắt chưa được nghiệm thu trong đợt này.

**Thiếu cấu hình dữ liệu cũ còn tồn tại:** scheme `1.0` không có band CEFR cho `GRAMMAR_VOCABULARY`. Có **12 đề PUBLISHED** chứa kỹ năng này nên bị validation policy chặn bắt đầu: ID **20, 21, 30, 31, 32, 33, 34, 35, 60, 64, 69, 74**. Gồm Placement/Practice/Checkpoint Grammar & Vocabulary và hai Full Mock. Đây là dữ liệu cấu hình thiếu đã tồn tại trước đợt nâng schema; không phải đề bị xóa hoặc route bị mất.

Nhóm cần chốt các khoảng CEFR nội bộ cho G&V. Theo quy tắc không sửa policy đã sử dụng tại chỗ, hướng xử lý tiếp theo là tạo version policy đầy đủ, giữ version/snapshot/lịch sử cũ và gán version mới cho các đề cần dùng sau khi chốt cấu hình. Chưa tự sao chép band của Reading hay gán A1 cho mọi điểm.

Năm đề thuộc `0.1-A1` đã có đủ 5 raw-scale rule và band A1 phủ 0–50 sau xác nhận của chủ project. Gemini vẫn cần cấu hình key/enable và kiểm thử live riêng; không có điểm AI giả để thay provider.
