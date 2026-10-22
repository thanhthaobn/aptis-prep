# Kiểm tra sau tích hợp develop — 22/10/2026

## SQL Server và an toàn dữ liệu

- Kiểm tra trên SQL Server cục bộ, database `aptisprep_db`.
- Trước nâng cấp đã backup COPY_ONLY/CHECKSUM và RESTORE VERIFYONLY thành công:
  `backups/aptisprep_before_startup_20261022.bak`.
- Backup media: `backups/uploads_before_startup_20261022.zip`.
- Bộ migration được giữ tương thích với cả database đã chạy seed Trâm ở V026–V028 và database mới.
- V026–V028 giữ nguyên seed/catalogue/reading passage của Trâm; các migration lesson sau merge chạy ở V032–V034.
- Không sửa checksum hoặc repair lịch sử migration; không clean/drop database.
- Đối chiếu toàn bộ bản ghi cũ ở 37 bảng bằng số lượng và SHA-256 của JSON theo id:
  không có bản ghi cũ bị mất hoặc thay đổi. Dữ liệu catalogue mới được thêm bởi bộ V026–V028.
- Đối chiếu 13 file uploads với backup: không file nào bị thiếu/thay đổi.
- DBCC CHECKDB hoàn tất không báo lỗi.

Backup nằm trong thư mục Git đã ignore, không đưa DB/media hoặc credentials lên remote.
Máy đã chạy seed Trâm dưới V026–V028 có thể tiếp tục migrate mà không cần đổi checksum hoặc chạy repair.

## Dọn code

Định dạng 12 file của phần form/section, preview, snapshot bài học, nhãn theo kỹ năng,
quy tắc checkpoint và test hồi quy bằng google-java-format 1.24.0, chế độ AOSP.
Chỉ làm sạch cách trình bày/import, không đổi quy tắc chấm, lưu bài làm, quyền truy cập
hay cấu trúc dữ liệu. Các thay đổi cục bộ trước đó vẫn được giữ nguyên.

## Kiểm thử và khởi động

- `APTIS_SQLSERVER_TESTS=true` và `.\mvnw.cmd "-DforkCount=0" test`:
  152 test đạt, không lỗi, không bỏ qua; bao gồm kiểm thử SQL Server/Flyway thực tế.
- `node --test src/test/js/lesson-player.test.cjs`: 4 test đạt.
- `git diff --check`: đạt.
- Ứng dụng kết nối DB thật với Hibernate `ddl-auto=validate` và chạy cổng 8080.
- Trang `/` và `/login`: HTTP 200.

Lần chạy qua môi trường công cụ gặp lỗi JDK Windows `UnixDomainSockets.connect`
khi Tomcat mở selector. Chạy với thư mục socket tạm ngắn đã khởi động thành công:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=D:\HSF302_FPT\PROJECT\target"
```

Không cần đổi port hoặc cấu hình nghiệp vụ. Không bật hai instance cùng cổng 8080.
Kết quả trên là phạm vi đã kiểm chứng, không thay thế kiểm thử thủ công toàn bộ luồng UI.
