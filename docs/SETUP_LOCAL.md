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

Chạy [upgrade-scoring-scheme-scope.sql](../database/upgrade-scoring-scheme-scope.sql) sau khi sao lưu để bổ sung loại đề và kỹ năng vào bộ chấm điểm. Các bộ đã lưu tiếp tục dùng được trên đề đã gán.

Để dùng thang điểm quy đổi trên 50, chạy [upgrade-custom-scoring-scales.sql](../database/upgrade-custom-scoring-scales.sql) trên database đang dùng sau khi sao lưu. Script cập nhật các ràng buộc kiểm tra điểm và giữ nguyên dữ liệu hiện có.

**Không xóa/recreate database để vượt lỗi validate.** Sao lưu DB bằng SQL Server Backup và đối chiếu schema trước khi nâng cấp thủ công có kiểm chứng.

Nếu database đã có dữ liệu nhưng còn các cột JSON legacy của snapshot, chạy
[repair-current-runtime-schema.sql](../database/repair-current-runtime-schema.sql)
sau khi backup. Script chỉ đổi ba cột legacy sang `NULL`, không xóa hoặc ghi đè
dữ liệu. Nếu các bảng snapshot quan hệ chưa tồn tại, chạy các script
`migration-no-json-*` được liệt kê ở đầu file repair trước, rồi chạy các script
`backfill-no-json-*` để phục hồi snapshot cũ. Khởi động lại ứng dụng sau khi
hoàn tất bước này.

Ngày 25/10/2026, DB local `aptisprep_db` đã được backup COPY_ONLY/CHECKSUM, restore thử và nâng schema có kiểm chứng. Schema nghiệp vụ hiện khớp source tại commit `0ec2019`; Hibernate validate và DBCC CHECKDB đều đạt. Toàn bộ 1.530 dòng trong 38 bảng cũ được đối chiếu hash trước/sau và giữ nguyên. Xem [báo cáo nâng schema](DATABASE_UPGRADE_2026-10-25.md).

Thông tin này chỉ xác nhận DB local đã kiểm tra trên máy chủ project; các máy khác vẫn phải backup và đối chiếu riêng. Bảng `flyway_schema_history` cũ giữ nguyên để tra cứu, không phải phiên bản schema hiện tại. Ứng dụng không còn dùng Flyway.

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

Chỉ mở localhost:8080 sau khi log có Started AptisPrepApplication. Dừng ứng dụng cũ trước khi chạy bản khác cùng cổng. DB local đã nâng như mục 3; không khởi động source mới trên DB ở máy khác còn schema cũ trước khi đối chiếu/nâng cấp.

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

`SqlServerMigrationTests` giữ tên cũ để tương thích lệnh, nhưng nay chỉ **validate**, không migrate. Bật riêng bằng APTIS_SQLSERVER_TESTS=true và DB_URL rõ ràng. DB local sau nâng đã qua test này; DB cũ ở máy khác chưa được nâng sẽ thất bại đúng theo thiết kế.

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

Prettier dùng `htmlWhitespaceSensitivity: css` để thẻ block dễ đọc, kiểm tra lại phần chữ inline khi sửa template. Script dài đã tách sang `static/js`; formatter vẫn hỗ trợ script nhúng nếu có. CI kiểm cả Java, HTML/JavaScript và test JavaScript. `.editorconfig` và `.gitattributes` thống nhất encoding, thụt lề và xuống dòng giữa các máy.

## 9. Media local và chuyển máy

Ứng dụng dùng một nơi lưu file: thư mục `uploads/` tại working directory của
process Java (`app.storage.upload-dir`). Các thư mục con là cách phân loại,
không phải nhiều hệ thống lưu trữ:

```text
uploads/
├── curriculum/          # Media giáo trình đã chuyển ra khỏi JAR
│   ├── audio/
│   └── images/
├── lesson-images/       # Ảnh Admin tải lên, chia theo lesson ID
├── lesson-video/        # Video Admin tải lên, chia theo lesson ID
├── lesson-audio/        # Audio Admin tải lên, chia theo lesson ID
├── questions/           # Media câu hỏi
├── lesson-recordings/   # Bản ghi bài học
└── attempts/            # Bản ghi bài thi
```

Không cần đổi DB hoặc snapshot khi gộp hai nơi lưu cũ: ví dụ đường dẫn trong DB
`curriculum/audio/listening-a2.wav` nay được đọc từ
`uploads/curriculum/audio/listening-a2.wav`. Không thêm tiền tố `uploads/` vào
đường dẫn tương đối đã lưu. Không còn fallback sang classpath/JAR.

Khi chuyển máy/deploy, sao lưu và khôi phục đồng bộ database với **toàn bộ uploads**,
giữ cấu trúc con và tên file. Không dùng riêng Git clone/JAR như bản sao lưu media.
`mvn clean` chỉ dọn output build, không dọn thư mục uploads ở gốc project.
Không xóa file chỉ vì bài hiện tại không tham chiếu: lịch sử/snapshot có thể vẫn
đang dùng file đó. Backend tiếp tục kiểm quyền qua các route media; `/uploads/**`
không phải đường dẫn công khai cho trình duyệt truy cập trực tiếp.

Đợt gộp ngày 25/10/2026 đã sao lưu cả media cũ và uploads trong file
`backups/media-before-consolidation-7afb6371a9464749a7d6b9772eacebb7.zip`, đối chiếu
hash 7 file được chuyển và 13 file upload có sẵn. File ZIP này chỉ ở máy hiện tại,
bị Git ignore. Khi khôi phục từ ZIP, thư mục `curriculum` của bản sao lưu đặt
bên trong uploads; không đưa trở lại src/main/resources.

Script `scripts/build-curriculum-audio.ps1` có thể tạo audio còn thiếu vào
`uploads/curriculum/audio`, giữ nguyên file đã tồn tại. Không tự chạy script này
khi khởi động ứng dụng. Test dùng audio tự tạo trong `target/test-uploads` hoặc
thư mục tạm, không cần và không sửa media thật.
