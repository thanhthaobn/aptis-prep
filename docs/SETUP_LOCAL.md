# Thiết lập môi trường local

## 1. Yêu cầu

- JDK 21
- SQL Server Developer hoặc Express
- Git
- IntelliJ IDEA

Kiểm tra Java:

```powershell
java -version
```

## 2. Tạo database

Kết nối SQL Server bằng IntelliJ Database Tool, SSMS hoặc DBeaver rồi chạy:

```sql
IF DB_ID('aptisprep_db') IS NULL
BEGIN
    CREATE DATABASE aptisprep_db;
END;
GO
```

Không tự tạo các bảng. Flyway sẽ tạo schema khi ứng dụng khởi động.

## 3. Cấu hình kết nối riêng

Copy:

```text
src/main/resources/application-dev.example.properties
```

thành:

```text
src/main/resources/application-dev.properties
```

Sau đó điền username/password SQL Server local. File này đã được `.gitignore` và không được commit.

## 4. Chạy ứng dụng

```powershell
.\mvnw.cmd clean spring-boot:run
```

Mở `http://localhost:8080`.

## 5. Xác nhận setup

- Flyway chạy V001 đến V005 thành công (máy đã có V003 chỉ chạy phần còn thiếu).
- Bảng `flyway_schema_history` tồn tại.
- Hibernate không báo schema validation error.
- Đăng nhập được bằng tài khoản demo.
- Admin vào `/admin/dashboard`.
- Student vào `/student/dashboard`.

## 6. SQL Server TCP/IP và tài khoản

- SQL Server Configuration Manager → Network Configuration → Protocols của đúng instance → bật TCP/IP. Mẫu dùng localhost:1433; Express/named instance có thể dùng cổng khác, xác định đúng cổng hoặc đặt cổng cố định và restart đúng service instance.
- SQL login/password cần mixed mode và login được bật. Login cần quyền đọc/ghi và DDL trong database local để chạy Flyway. Không cần mở cổng public hoặc tắt firewall.
- `Test-NetConnection localhost -Port 1433` kiểm tra cổng, không xác minh password.
- IntelliJ Database Tool kết nối được không có nghĩa Spring đã có datasource. App vẫn cần file dev riêng. `trustServerCertificate=true` chỉ là lựa chọn local; không dùng làm cấu hình production.
- Không sửa V001–V005 đã chạy, tự repair history hoặc drop DB để vượt checksum. Cần kiểm tra từ đầu thì tạo DB local tên mới; không xóa dữ liệu đang cần giữ.

## 7. IntelliJ và Java 21

- Mở thư mục chứa pom.xml; Project Structure → Project SDK/Language level: 21.
- Settings → Maven: Maven Wrapper, Runner JRE và Importer JDK: 21; reload Maven.
- Chạy com.aptisprep.AptisPrepApplication; working directory là thư mục repo. Profile mặc định dev, kiểm tra Run Configuration không override sang profile khác.
- So sánh `java -version` và `.\mvnw.cmd -version`, cả hai phải là 21. Lần đầu cần mạng tải dependency.
- Chỉ mở localhost khi log có Started AptisPrepApplication và server còn chạy. Dừng bản IntelliJ trước khi chạy bản terminal cùng cổng.
- Sau sửa UI: Build Project hoặc chạy lại và Ctrl+F5, không sửa file trong target.

## 8. Kiểm thử, JAR và file riêng

```powershell
.\mvnw.cmd verify
```

Test thường dùng H2/Flyway tắt/OpenAI giả lập, không chứng minh SQL Server migration chạy đúng. GitHub Actions chạy Java 21 và verify, không cần key hay DB secret.

SqlServerMigrationTests là opt-in, gọi Flyway migrate nên có thay đổi schema. Chỉ chạy trên DB kiểm thử riêng: cấu hình datasource local trỏ đúng DB đó, đặt APTIS_SQLSERVER_TESTS=true, chạy `.\mvnw.cmd -Dtest=SqlServerMigrationTests test`, sau đó bỏ biến và khôi phục datasource. Không chạy trên DB có dữ liệu cần giữ.

JAR đóng gói không chứa application-dev.properties. Chạy JAR cần datasource qua SPRING_DATASOURCE_URL/USERNAME/PASSWORD hoặc file config ngoài. Khi phát triển, vẫn chạy từ source bằng IntelliJ/Maven như trên. Không chia sẻ JAR/config kèm mật khẩu, API key hoặc dữ liệu upload.

Mỗi người có DB local riêng. Git chia sẻ schema/seed và giao diện, không tự đồng bộ dữ liệu phát sinh. Tài khoản demo chỉ dùng local; AI tắt vẫn chạy bình thường. `/uploads/**` hiện bị chặn, chưa có endpoint media riêng kiểm quyền; không bật upload/audio thật trước khi hoàn thiện theo SERVICE_CONTRACTS.md.

## 9. Lỗi thường gặp và checklist máy thứ hai

| Lỗi | Kiểm tra |
|---|---|
| Thiếu datasource URL | Tạo đúng application-dev.properties và profile dev |
| SQL timeout/refused | Instance, service, TCP/IP và cổng thực |
| Login failed | Mixed mode, login bật, password và quyền DB |
| Cannot open database | DB trên đúng instance, login có quyền |
| Port 8080 in use | Dừng app cũ hoặc đổi cổng local |
| Web chưa đổi | Đúng project/working directory, build resources, Ctrl+F5 |
| Migration checksum lỗi | Không sửa migration cũ; báo người giữ nền |

- [ ] Clone đúng commit trên máy khác, dùng JDK 21 và SQL Server riêng.
- [ ] Verify thành công, DB rỗng chạy V001–V005, đăng nhập được hai role.
- [ ] Student mở Bài học/Đề thi/Kết quả/Placement/Hồ sơ; Admin mở Cấu hình.
- [ ] git status không có secret, uploads, target hay .idea.
- [ ] Ghi commit và phiên bản SQL Server/kết quả trong PR, không chụp secret.

Chưa chạy máy thứ hai thì không đánh dấu hoàn tất checklist đó. Hibernate hiện chỉ validate Entity đã có (User), không xác nhận toàn bộ bảng nghiệp vụ đã map.
