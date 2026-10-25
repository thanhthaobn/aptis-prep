# Thiết lập local — một cấu hình, một schema hiện tại

## 1. Công cụ

JDK 21, SQL Server Developer/Express, Maven Wrapper và IntelliJ. Mở thư mục chứa pom.xml, chọn Project SDK và Maven Runner JDK 21, reload Maven.

## 2. Database mới

1. Tạo một database SQL Server **mới và rỗng** bằng SSMS.
2. Chọn đúng database mới trong cửa sổ query.
3. Chạy toàn bộ [schema-current.sql](../database/schema-current.sql), bật chế độ dừng khi có lỗi (sqlcmd: `-b`).
4. Script dựng 39 bảng cùng FK, CHECK, default và index. Không có tài khoản, đề thi hay dữ liệu mẫu.
5. Script từ chối database đã có bảng; không dùng nó để nâng cấp DB đang có dữ liệu.

Ứng dụng chỉ dùng `ddl-auto=validate`, `spring.sql.init.mode=never`. Không còn Flyway, không tạo/cập nhật schema khi khởi động.

## 3. Database đang dùng

**Không xóa/recreate database để vượt lỗi validate.** Sao lưu DB bằng SQL Server Backup và đối chiếu schema trước khi nâng cấp thủ công có kiểm chứng.

Kiểm tra chỉ đọc ngày 24/10/2026: `aptisprep_db` đang có 37 bảng nghiệp vụ, lịch sử cũ ở V031; chưa có `mock_exams.demo_only`, `ai_assessment_jobs.processing_token` và các cột playback mới. Vì vậy **không thể coi DB này đã sẵn sàng chạy source mới**. Đợt dọn source không sửa DB đó.

Các file SQL/config nguồn cũ được lưu trong thư mục `backups/` bị Git ignore. Đây là bản sao file nguồn, **không phải backup database**. Cấu hình riêng cũ nằm ở `backups/local-config.properties`, không được Spring nạp và không đưa lên Git.

## 4. Cấu hình duy nhất

Ứng dụng dùng `src/main/resources/application.properties`. Không cần profile dev. Đặt biến môi trường trong IntelliJ → Edit Configurations → Environment variables:

- `DB_PASSWORD`: bắt buộc, mật khẩu SQL Server riêng.
- `DB_USERNAME`: tùy chọn, mặc định sa; nên dùng tài khoản local có quyền phù hợp.
- `DB_URL`: tùy chọn; mặc định trỏ `localhost:1433`, database `aptisprep_db`. Nếu dựng DB mới tên khác phải đặt lại URL.
- `GEMINI_ENABLED`, `GEMINI_API_KEY`, `GEMINI_GRADING_MODEL`: tùy chọn.
- `FFPROBE_PATH`: tùy chọn nếu ffprobe không có trên PATH.

Không ghi mật khẩu/API key vào file được track, lệnh chia sẻ công khai hay ảnh chụp. Không có key thì để AI tắt; không tạo điểm giả.

Chọn Run `AptisPrep - local`; đặt working directory ở gốc project. Nếu dùng cấu hình cũ, xóa Active profiles/VM option dev và thêm DB_PASSWORD. Cần nạp lại cấu hình nếu IntelliJ đang mở trong lúc file được sửa.

Hoặc mở PowerShell ở gốc project, đặt các biến môi trường trên **trong phiên terminal của bạn**, rồi chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Chỉ mở localhost:8080 sau khi log có Started AptisPrepApplication. Dừng ứng dụng cũ trước khi chạy bản khác cùng cổng. Không khởi động source mới trên DB V031 trước khi đối chiếu/nâng schema.

## 5. Tài khoản và nội dung

Không có runner tự tạo admin/student hoặc seed đề demo. Đăng ký tài khoản thật qua /register. Trên DB mới do bạn quản lý, người quản trị DB cấp role ADMIN cho đúng tài khoản đã đăng ký; xác minh email và chỉ cập nhật đúng một bản ghi. Mật khẩu vẫn được mã hóa bởi ứng dụng, không nhập mật khẩu thô vào cột password_hash.

Sau đó dùng chức năng Admin để tạo Certificate, Scoring Scheme/Band, Lesson, Question và Exam. Các chức năng cần dữ liệu tham chiếu phải hiện trạng thái rỗng, không tự bịa dữ liệu thay thế.

## 6. Kiểm thử

```powershell
.\mvnw.cmd clean verify
node --test src/test/js/*.test.cjs
```

Test thường chạy H2 trong bộ nhớ, không cần DB_PASSWORD và không truy cập database ứng dụng.

SQL Server tạm: đặt DB_PASSWORD/DB_USERNAME của tài khoản local có quyền tạo/xóa database, rồi:

```powershell
$env:APTIS_SQLSERVER_ISOLATED_TESTS = 'true'
.\mvnw.cmd '-Dtest=V2SqlServerPreservationTests' test
Remove-Item Env:APTIS_SQLSERVER_ISOLATED_TESTS
```

Test chỉ tạo/xóa DB UUID của chính nó, kiểm schema, đáp án A-F, query AI và từ chối ghi đè DB có dữ liệu.

`SqlServerMigrationTests` giữ tên cũ để tương thích lệnh, nhưng nay chỉ **validate**, không migrate. Bật riêng bằng APTIS_SQLSERVER_TESTS=true và DB_URL rõ ràng; DB V031 chưa được nâng sẽ thất bại đúng theo thiết kế.

## 7. Thay đổi schema về sau

Cập nhật DDL hiện tại và entity cùng nhau, kiểm trên DB tạm. Đối với DB đang dùng, chuẩn bị bước nâng cấp riêng, sao lưu và nghiệm thu bảo toàn dữ liệu trước khi thực thi. Không dùng Hibernate update/create-drop trên dữ liệu thật. Schema hiện tại không lưu lịch sử thay đổi; Git vẫn giữ lịch sử source để khôi phục khi cần.

## 8. Format source

Java dùng Google Java Format theo style AOSP (4 dấu cách), quản lý qua Spotless. `verify` kiểm format nhưng không tự sửa source:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
```

HTML/JavaScript dùng Prettier; Node/npm chỉ phục vụ phát triển và CI, không phải dependency chạy ứng dụng Java:

```powershell
npm ci --ignore-scripts --no-audit --no-fund
npm run format:web
npm run format:web:check
npm run test:web
```

Prettier giữ whitespace HTML ở chế độ strict để hạn chế ảnh hưởng Thymeleaf, đồng thời format cả JavaScript trong các thẻ script hiện có. CI kiểm cả Java, HTML/JavaScript và test JavaScript. `.editorconfig` và `.gitattributes` thống nhất encoding, thụt lề và xuống dòng giữa các máy.
