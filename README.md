# AptisPrep

Ứng dụng học tập và thi thử Aptis ESOL General sử dụng Java 21, Spring Boot 4.1.1, Thymeleaf và SQL Server.

## Công nghệ

Setup chi tiết SQL Server TCP/IP, IntelliJ/JDK 21, kiểm thử và checklist máy mới: [SETUP_LOCAL.md](docs/SETUP_LOCAL.md). Hợp đồng service và phân quyền tích hợp: [SERVICE_CONTRACTS.md](docs/SERVICE_CONTRACTS.md). GitHub Actions đã có, chưa push hoặc cấu hình repository từ đây.

- Java 21
- Spring Boot 4.1.1
- Spring MVC và Thymeleaf
- Spring Security Session
- Spring Data JPA
- Microsoft SQL Server
- Flyway Migration
- Maven Wrapper

## Chạy local

1. Cài JDK 21 và SQL Server.
2. Tạo database `aptisprep_db`.
3. Copy `src/main/resources/application-dev.example.properties` thành `application-dev.properties`.
4. Điền username/password SQL Server local.
5. Chạy `mvnw.cmd clean spring-boot:run` trên Windows.
6. Mở `http://localhost:8080`.

Tài khoản demo được tạo tự động trong profile `dev`:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@aptis.local` | `Password@123` |
| Student | `student@aptis.local` | `Password@123` |

## Kiểm thử

```powershell
$env:JAVA_HOME = "C:\duong-dan-den-jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean test
```

Maven Wrapper kiểm tra `JAVA_HOME` và chỉ chạy với JDK 21, kể cả khi máy đang cài thêm Java phiên bản mới hơn.

Test thường dùng H2 in-memory và không cần SQL Server. V001–V003 đã được kiểm tra trên SQL Server 15 với database rỗng; migration V004–V005 chuyển nền cũ sang contract V2. `SqlServerMigrationTests` chỉ chạy khi đặt `APTIS_SQLSERVER_TESTS=true` và dùng cấu hình database local.

## Nguyên tắc kiến trúc

Giao diện học viên dùng bản đồ chặng và phong cách học tập vui. Sau đăng nhập mở `/student/orientation` để xem lộ trình định hướng minh họa không yêu cầu mục tiêu/bài đầu vào; sinh/lưu lộ trình và tiến độ thực tế còn chờ luồng học tập triển khai.

Phân công đầy đủ cho bốn người theo V2 và phần mở rộng quản lý học viên: [TEAM_ASSIGNMENT_V2.md](docs/TEAM_ASSIGNMENT_V2.md).

```text
Controller -> Service -> Repository
```

- Không dùng `@RestController` cho giao diện.
- Controller không gọi Repository trực tiếp.
- Schema thay đổi qua Flyway, không sửa bảng thủ công rồi bỏ qua migration.
- Hibernate dùng `ddl-auto=validate` khi chạy với SQL Server.

Đọc [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md) trước khi phát triển module.

Yêu cầu chuẩn: [V2](docs/SPEC_V2.md). Xem [đối chiếu triển khai](docs/V2_IMPLEMENTATION.md) để phân biệt nền tảng đã có và nghiệp vụ còn thiếu. Service và cấu hình OpenAI được giữ để tích hợp vào luồng nộp bài thi theo [AI_SETUP.md](docs/AI_SETUP.md); hiện chưa có giao diện gọi chấm AI.

Trước khi tạo hoặc sửa giao diện, đọc [docs/UI_GUIDE.md](docs/UI_GUIDE.md) và giữ đúng route trong [docs/ROUTES.md](docs/ROUTES.md).

Phần mở rộng [Quản lý học viên cho Admin](docs/STUDENT_MANAGEMENT.md) có giao diện tại `/admin/students`, hỗ trợ xem/tìm/lọc hồ sơ minh họa. Nút khóa/mở khóa chỉ mở xác nhận, chưa thay đổi tài khoản; thành viên tiếp tục nối database, phân trang và nghiệp vụ theo tài liệu.

Admin còn có khung Bài học (`/admin/lessons`), Ngân hàng câu hỏi (`/admin/questions`), Đề thi (`/admin/exams`) và Kết quả (`/admin/results`). Đây là dữ liệu minh họa; bàn giao và phần việc chưa nối backend xem [ADMIN_UI_HANDOFF.md](docs/ADMIN_UI_HANDOFF.md).

## Giao diện học viên mới

Admin có mục **Chứng chỉ & Chấm điểm** tại `/admin/certificates`: khung UC-A03–A05, không có ghi DB. Người 3 nhận theo [SCORING_UI_HANDOFF.md](docs/SCORING_UI_HANDOFF.md); `/admin/settings` chỉ còn redirect, không phải trang cấu hình key.

Sau khi đăng nhập Student, mở menu **Bài học**, **Đề thi**, **Kết quả** để xem danh sách và chi tiết minh họa. Chưa có lưu tiến độ, tạo lượt thi hoặc chấm AI. Hướng dẫn nhóm tiếp nhận: [docs/STUDENT_UI_HANDOFF.md](docs/STUDENT_UI_HANDOFF.md).

