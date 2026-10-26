# Kịch bản kiểm thử AptisPrep

## 1. Mục tiêu

Kiểm tra nhanh ứng dụng Spring MVC + Thymeleaf với SQL Server local, tập trung vào:

- Khởi động ứng dụng và kiểm tra Flyway/Hibernate.
- Đăng ký, đăng nhập, phân quyền Student/Admin.
- Hồ sơ, mục tiêu và các trang học tập của Student.
- Quản lý học viên, chứng chỉ và bộ chấm điểm của Admin.
- CSRF, khóa tài khoản và các trường hợp nhập sai.

## 2. Điều kiện trước khi test

- JDK 21 và SQL Server local đang chạy.
- Database `aptisprep_db` đã tồn tại.
- File `src/main/resources/application-dev.properties` có datasource đúng và không chứa giá trị giả.
- Dùng dữ liệu test riêng; không dùng mật khẩu/API key thật trong ảnh chụp hoặc báo cáo.
- Tài khoản Admin local đã được cấp sẵn. Tạo một tài khoản Student mới bằng chức năng đăng ký để tránh trùng dữ liệu.

## 3. Khởi động và kiểm tra kỹ thuật

Chạy trong PowerShell:

```powershell
.\mvnw.cmd -B -ntp spring-boot:run
```

| ID | Thao tác | Kết quả mong đợi | Kết quả thực tế |
|---|---|---|---|
| TECH-01 | Quan sát log kết nối datasource | Kết nối được `localhost:1433/aptisprep_db`, không có `Login failed` | [ ] |
| TECH-02 | Quan sát log Flyway | V001 đến V006 validate/migrate thành công; không có checksum error | [ ] |
| TECH-03 | Quan sát log Hibernate | Không có `Schema-validation ... wrong column type`; thấy `Started AptisPrepApplication` | [ ] |
| TECH-04 | Mở `http://localhost:8080` | Trang chủ hiển thị HTTP 200, không phải trang lỗi | [ ] |

Lưu ý: dòng `BUILD SUCCESS` của Maven không đủ để kết luận ứng dụng chạy thành công. Phải thấy `Started AptisPrepApplication`.

## 4. Smoke test trang công khai

| ID | Thao tác | Kết quả mong đợi | Kết quả thực tế |
|---|---|---|---|
| PUB-01 | Mở `/` | Trang landing hiển thị bình thường | [ ] |
| PUB-02 | Mở `/login` | Hiển thị form đăng nhập | [ ] |
| PUB-03 | Mở `/register` | Hiển thị form đăng ký | [ ] |
| PUB-04 | Mở `/student/dashboard` khi chưa đăng nhập | Redirect về `/login` | [ ] |
| PUB-05 | Mở `/admin/dashboard` khi chưa đăng nhập | Redirect về `/login` | [ ] |

## 5. Đăng ký và đăng nhập Student

### TC-AUTH-01 — Đăng ký hợp lệ

1. Mở `/register`.
2. Nhập họ tên, email mới, mật khẩu hợp lệ và xác nhận đúng mật khẩu.
3. Submit form.

Kỳ vọng: redirect về `/login`, hiển thị thông báo đăng ký thành công. Đăng nhập bằng email vừa tạo thì vào `/student/dashboard`.

### TC-AUTH-02 — Xác nhận mật khẩu sai

1. Mở `/register`.
2. Nhập mật khẩu và xác nhận khác nhau.
3. Submit form.

Kỳ vọng: không tạo tài khoản, vẫn ở trang đăng ký và hiển thị lỗi mật khẩu xác nhận.

### TC-AUTH-03 — Email bị trùng

1. Đăng ký lại bằng email đã tồn tại.

Kỳ vọng: không tạo bản ghi thứ hai, hiển thị lỗi đăng ký.

### TC-AUTH-04 — Đăng nhập sai

1. Mở `/login`.
2. Nhập email hoặc mật khẩu sai.

Kỳ vọng: vẫn ở trang login và hiển thị trạng thái đăng nhập thất bại; không vào được dashboard.

### TC-AUTH-05 — Đăng xuất

1. Đăng nhập Student.
2. Chọn Đăng xuất.
3. Mở lại `/student/dashboard`.

Kỳ vọng: redirect về `/login`; session cũ không còn quyền truy cập.

## 6. Student flow

Thực hiện bằng tài khoản Student vừa đăng ký.

| ID | Thao tác | Kết quả mong đợi | Kết quả thực tế |
|---|---|---|---|
| STU-01 | Mở `/student/dashboard` | Dashboard Student hiển thị; không lỗi 500 | [ ] |
| STU-02 | Mở `/student/orientation` | Nội dung định hướng hiển thị dù chưa đặt mục tiêu/placement | [ ] |
| STU-03 | Mở `/student/profile` | Hiển thị email, họ tên, mục tiêu và trình độ hiện tại | [ ] |
| STU-04 | Sửa họ tên rồi lưu | Redirect về profile, dữ liệu mới được giữ sau refresh | [ ] |
| STU-05 | Nhập họ tên rỗng rồi lưu | Không lưu, hiển thị validation error | [ ] |
| STU-06 | Mở `/student/goals` và lưu mục tiêu hợp lệ | Redirect về goals, mục tiêu được hiển thị lại | [ ] |
| STU-07 | Nhập ngày mục tiêu trong quá khứ, ví dụ `11/11/1111` | Không lưu, hiển thị lỗi “Ngày mục tiêu phải từ hôm nay trở đi” ngay dưới ô ngày | [ ] |
| STU-08 | Mở `/student/placement` | Trang giới thiệu placement hiển thị bình thường | [ ] |
| STU-09 | Mở `/student/lessons`, `/student/exams`, `/student/results` | Trang danh sách/empty state hiển thị, không lỗi quyền | [ ] |
| STU-10 | Mở `/admin/dashboard` bằng Student | Nhận HTTP 403 hoặc trang access denied | [ ] |

## 7. Admin flow

Đăng xuất Student rồi đăng nhập bằng tài khoản Admin local.

### TC-ADMIN-01 — Dashboard và danh sách học viên

1. Mở `/admin/dashboard`.
2. Mở `/admin/students`.
3. Tìm theo tên/email và lọc theo trạng thái.
4. Mở chi tiết một học viên.

Kỳ vọng: chỉ Admin truy cập được; kết quả tìm/lọc đúng; trang chi tiết có thông tin tài khoản, mục tiêu, trình độ và lịch sử.

### TC-ADMIN-02 — Khóa và mở khóa học viên

1. Trong chi tiết học viên đang `ACTIVE`, chọn nút **Khóa tài khoản** ngay cạnh trạng thái trong thẻ hồ sơ.
2. Kiểm tra trạng thái chuyển thành `LOCKED`.
3. Đăng nhập hoặc gửi request tiếp theo bằng tài khoản Student đó.
4. Mở khóa từ Admin.

Kỳ vọng: tài khoản bị khóa không tiếp tục dùng session; sau khi mở khóa, Student đăng nhập lại được.

### TC-ADMIN-03 — Certificate

1. Mở `/admin/certificates`.
2. Tạo certificate hợp lệ.
3. Thử tạo mã certificate trùng.
4. Sửa certificate hoặc disable bằng trạng thái.

Kỳ vọng: dữ liệu hợp lệ được lưu; mã trùng bị từ chối; disable không xóa cứng dữ liệu.

### TC-ADMIN-04 — Scoring scheme và score band

1. Mở `/admin/scoring-schemes`.
2. Tạo scheme hợp lệ.
3. Tạo band có `minScore <= maxScore` và điểm trong `0..50`.
4. Thử tạo band bị chồng lấn hoặc trùng biên.
5. Với scheme đã được sử dụng, thử sửa trực tiếp version cũ.

Kỳ vọng: band hợp lệ được lưu; band sai bị từ chối; scheme đã sử dụng phải tạo version mới, không sửa phá lịch sử.

### TC-ADMIN-05 — Student không được vào Admin

1. Đăng nhập bằng Student.
2. Mở `/admin/students`, `/admin/certificates` và `/admin/scoring-schemes`.

Kỳ vọng: tất cả bị từ chối quyền truy cập.

## 8. CSRF và bảo mật form

| ID | Thao tác | Kết quả mong đợi | Kết quả thực tế |
|---|---|---|---|
| SEC-01 | Submit một form POST hợp lệ từ giao diện | Request có CSRF token và được xử lý | [ ] |
| SEC-02 | Gửi lại POST nhưng bỏ CSRF token bằng DevTools/Postman | HTTP 403, không đổi dữ liệu | [ ] |
| SEC-03 | Mở `/uploads/**` trực tiếp | Bị từ chối, không lộ file | [ ] |
| SEC-04 | Student truy cập route Admin | HTTP 403/access denied | [ ] |
| SEC-05 | Admin truy cập route Student | HTTP 403/access denied | [ ] |

## 9. AI assessment

Mặc định `app.ai.enabled=false`, vì vậy không gọi Gemini thật trong smoke test.

| ID | Thao tác | Kết quả mong đợi | Kết quả thực tế |
|---|---|---|---|
| AI-01 | Chạy app khi AI tắt | App vẫn khởi động; không tự tạo điểm giả | [ ] |
| AI-02 | Gọi flow assessment khi chưa có provider/key | Hiển thị trạng thái chưa khả dụng hoặc lỗi đã được làm sạch; không lộ API key/lỗi thô | [ ] |
| AI-03 | Retry một assessment lỗi | Job retry theo giới hạn; không tạo bản ghi assessment trùng version | [ ] |
| AI-04 | Mở trang assessment bằng Student/Admin đúng quyền | Chỉ người có quyền xem được assessment tương ứng | [ ] |

Không bật AI thật trong bài test thường. Nếu cần kiểm thử Gemini thật, dùng API key riêng, giới hạn chi phí và ghi rõ đó là test có phát sinh phí.

## 10. Kết thúc và báo lỗi

Sau khi test:

1. Đăng xuất tất cả tài khoản.
2. Dừng ứng dụng bằng `Ctrl+C`.
3. Ghi lại ID test, bước tái hiện, URL, dữ liệu đầu vào và phần log lỗi cuối cùng.
4. Không gửi mật khẩu, API key hoặc toàn bộ file `application-dev.properties`.

Mẫu báo lỗi:

```text
Test ID: STU-04
Kết quả: FAIL
Bước tái hiện: ...
Kỳ vọng: ...
Thực tế: ...
URL: ...
Log cuối: ...
```

## 11. Kiểm thử tự động tùy chọn

Chạy bộ test H2 không đụng database SQL Server local:

```powershell
.\mvnw.cmd -B -ntp test
```

`SqlServerMigrationTests` là test opt-in. Chỉ chạy trên database kiểm thử riêng, không chạy trên `aptisprep_db` đang chứa dữ liệu cần giữ.
