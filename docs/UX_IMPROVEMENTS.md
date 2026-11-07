# Cải thiện trải nghiệm học và tài khoản

- Trước thi có bảng ngưỡng điểm/CEFR theo kỹ năng và phiên bản bộ chấm. Placement có liên kết cấu trúc/thang điểm trước khi bắt đầu.
- Sau thi đọc bộ chấm gốc từ snapshot, tô khoảng điểm đạt được. Không gắn cứng giao diện với thang 50. Nếu đề không có policy thì hiển thị chưa có quy đổi, không dựng ngưỡng giả.
- Lộ trình phân biệt trình độ đã đánh giá (Profile), đích chặng (targetLevel) và mục tiêu cá nhân (Goal). Chưa đặt Goal vẫn có đích chặng; phần tô trên thang chỉ biểu thị trình độ đã đánh giá.
- Speaking dùng một khung ổn định cho đồng hồ, thông báo và nghe lại. Ghi lại bắt đầu bản mới. Lưu bằng JSON tại chỗ, lỗi tải lên giữ bản ghi để thử lại; nộp thủ công nhắc lưu bản ghi còn dở. Lưu/nộp/quyền media vẫn qua service hiện có.
- Đăng ký chuẩn hóa tên/email trước validate; hỗ trợ tên tiếng Việt/một từ; kiểm tên, mật khẩu phổ biến, xác nhận và giới hạn UTF-8 của BCrypt. Email trùng, kể cả cạnh tranh khi ghi, được trả về trường email. Có hướng dẫn, hiện mật khẩu, lỗi khi nhập và liên kết hỗ trợ đọc màn hình.
- Dashboard chọn hành động theo trạng thái; hồ sơ đưa sửa thông tin/bảo mật lên trước lịch hoạt động; kết quả rỗng phân biệt chưa làm bài với không khớp bộ lọc. Placement nằm trong khu vực tài khoản/trình độ.

## Đổi và khôi phục mật khẩu

`GET/POST /student/password` yêu cầu đăng nhập và mật khẩu hiện tại. Sau đổi, phiên hiện tại đăng xuất và các phiên đăng nhập cũ bị từ chối ở request tiếp theo.

`GET/POST /forgot-password`, `GET/POST /reset-password` dùng token HMAC có hạn 15 phút, gắn với password hash hiện tại; token không dùng lại được sau khi đổi mật khẩu. POST có CSRF, trả thông báo chung cho email không tồn tại/không hoạt động và giới hạn một email mỗi phút cho mỗi tài khoản trên một tiến trình. Không cần đổi schema.

Để gửi email thật, cấu hình biến môi trường trong Run Configuration (không commit secret):

| Biến | Giá trị |
|---|---|
| `SPRING_MAIL_HOST` | Máy chủ SMTP |
| `SPRING_MAIL_PORT` | Cổng SMTP, thường 587 |
| `SPRING_MAIL_USERNAME` | Tài khoản SMTP |
| `SPRING_MAIL_PASSWORD` | Mật khẩu SMTP |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | `true` nếu SMTP yêu cầu |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | `true` cho SMTP STARTTLS |
| `PASSWORD_RESET_FROM` | Email người gửi đã được SMTP cho phép |
| `PASSWORD_RESET_BASE_URL` | URL public của ứng dụng, dùng HTTPS khi triển khai |
| `PASSWORD_RESET_SECRET` | Secret ngẫu nhiên riêng dài tối thiểu 32 ký tự, giữ ổn định giữa các instance |

Có thể dùng Spring properties tương ứng cho SMTP nếu nhà cung cấp yêu cầu SSL hoặc tùy chọn riêng. Khi thiếu cấu hình khôi phục, giao diện thông báo rõ chưa khả dụng; không tạo link giả hoặc ghi token ra log. Việc gửi email thực tế cần được kiểm tra với SMTP của môi trường triển khai.

## Kiểm chứng

Kiểm thử dùng H2/transaction rollback và mail giả; không gọi Gemini/SMTP thật, không nâng schema hoặc thay đổi dữ liệu SQL Server của project. Các test mới bao phủ đăng ký chuẩn hóa/lỗi, email trùng khi cạnh tranh, đổi mật khẩu/phiên cũ, token sai/hết hạn/dùng lại, Placement không có Goal, bộ chấm lịch sử/thang không phải 50 và lifecycle ghi âm/mất kết nối.

Kết quả: 311 kiểm thử Java đạt, 6 kiểm thử môi trường ngoài được bỏ qua; 32/32 kiểm thử JavaScript đạt. Các kiểm thử câu chữ giao diện đã được cập nhật và chạy lại. Riêng kiểm thử cấu hình chuẩn đạt trên bản cấu hình Git được sao chép vào `target/ux-config-check`; file cấu hình local đã khác Git từ trước và được giữ nguyên. Prettier, kiểm định dạng Java bằng Google Java Format 1.24.0 và `git diff --check` đạt. Plugin Spotless thiếu dependency trong cache offline nên dùng trực tiếp formatter cùng phiên bản/cờ cấu hình. Chưa kiểm tra trực quan bằng trình duyệt hoặc gửi SMTP thật.
