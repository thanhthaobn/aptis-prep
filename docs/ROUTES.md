# Route contract

Chứng chỉ & Chấm điểm đã có GET danh sách/tạo/xem/sửa tại `/admin/certificates` và `/admin/scoring-schemes`; `/admin/settings` redirect sang `/admin/certificates`. Người 3 tiếp nhận toàn bộ POST/validation/DB sau, xem [SCORING_UI_HANDOFF.md](SCORING_UI_HANDOFF.md).

| Method | URL | Quyền | Controller | Template |
|---|---|---|---|---|
| GET | `/` | Public | HomeController | `index` |
| GET | `/login` | Public | AuthController | `auth/login` |
| GET | `/register` | Public | AuthController | `auth/register` |
| POST | `/register` | Public | AuthController | Redirect `/login` |
| POST | `/logout` | Authenticated | Spring Security | Redirect `/login` |
| GET | `/student/dashboard` | Student | StudentDashboardController | `student/dashboard` |
| GET | `/student/orientation` | Student | StudentDashboardController, chỉ đọc minh họa | `student/learning/orientation` |
| GET | `/student/profile` | Student | StudentAccountPreviewController, chưa lưu | `student/account/profile` |
| GET | `/student/placement` | Student | StudentAccountPreviewController, chưa tạo attempt | `student/account/placement` |
| GET | `/admin/settings` | Admin | AdminSettingsPreviewController, đường dẫn cũ | Redirect `/admin/certificates` |
| GET | `/admin/dashboard` | Admin | AdminDashboardController | `admin/dashboard` |
| GET | `/admin/students` | Admin | AdminStudentController, dữ liệu minh họa | `admin/student/list` |
| GET | `/admin/students/{id}` | Admin | AdminStudentController, dữ liệu minh họa | `admin/student/detail` |

Route mới phải tuân theo:

- `GET` hiển thị trang.
- `POST` thay đổi dữ liệu và redirect sau khi thành công.
- Student dùng prefix `/student` khi route chỉ dành cho Student.
- Admin dùng prefix `/admin`.
- Không tạo prefix `/api` cho giao diện Thymeleaf.

## Route đã quy hoạch cho các luồng

Đã dựng GET preview cho `/student/lessons`, `/student/lessons/{id}`, `/student/exams`, `/student/exams/{id}`, `/student/results`, `/student/attempts/{id}/result`. Dùng `StudentContentPreviewController` và hai template `student/content/list`, `student/content/detail`. Chỉ dữ liệu minh họa; toàn bộ POST và trang làm bài vẫn chưa triển khai. Bàn giao chi tiết: [STUDENT_UI_HANDOFF.md](STUDENT_UI_HANDOFF.md).

Các route dưới đây là contract để tránh hai thành viên đặt URL khác nhau. Trừ các route GET preview được ghi rõ, các route còn lại chưa tồn tại cho đến khi module tương ứng được triển khai.

GET Hồ sơ/Placement đã dựng; POST cập nhật hồ sơ và bắt đầu Placement vẫn chưa có. `/uploads/**` bị chặn, chưa có route media kiểm quyền. Interface liên luồng theo [SERVICE_CONTRACTS.md](SERVICE_CONTRACTS.md).

### Account và Placement

| Method | URL | Mục đích |
|---|---|---|
| GET | `/student/profile` | Xem hồ sơ |
| GET/POST | `/student/profile/edit` | Cập nhật hồ sơ |
| GET/POST | `/student/goals` | Xem/cập nhật mục tiêu |
| GET | `/student/placement` | Giới thiệu Placement |
| POST | `/student/placement/start` | Tạo Placement Attempt |
| GET | `/student/placement/result` | Xem kết quả đầu vào |

### Learning

| Method | URL | Mục đích |
|---|---|---|
| GET | `/student/learning-path` | Xem lộ trình |
| GET | `/student/learning-path/{skill}` | Xem lộ trình theo kỹ năng |
| GET | `/student/lessons/{id}` | Học lesson |
| POST | `/student/lessons/{id}/complete` | Hoàn thành lesson |
| GET | `/student/flashcards` | Ôn flashcard |
| GET | `/student/checkpoints` | Danh sách checkpoint |

### Exam và Result

| Method | URL | Mục đích |
|---|---|---|
| GET | `/student/exams` | Danh sách đề |
| GET | `/student/exams/{id}` | Chi tiết đề |
| POST | `/student/exams/{id}/start` | Bắt đầu attempt |
| GET | `/student/attempts/{id}` | Làm bài |
| POST | `/student/attempts/{id}/answers` | Lưu câu trả lời bằng form |
| POST | `/student/attempts/{id}/submit` | Nộp bài |
| GET | `/student/attempts/{id}/result` | Xem kết quả |
| GET | `/student/results` | Lịch sử thi |

### Admin Content

Đã có GET danh sách/chi tiết/tạo/sửa cho lessons/questions/exams và GET danh sách/chi tiết results bằng dữ liệu minh họa. Tất cả POST và nghiệp vụ còn chờ thành viên triển khai. Xem [ADMIN_UI_HANDOFF.md](ADMIN_UI_HANDOFF.md).

| Method | URL | Mục đích |
|---|---|---|
| GET/POST | `/admin/lessons` | Danh sách/tạo lesson |
| GET | `/admin/lessons/create` | Form tạo bài học (preview) |
| GET | `/admin/lessons/{id}` | Chi tiết bài học (preview) |
| GET/POST | `/admin/lessons/{id}/edit` | Sửa lesson |
| GET/POST | `/admin/questions` | Danh sách/tạo question |
| GET | `/admin/questions/create` | Form tạo câu hỏi (preview) |
| GET | `/admin/questions/{id}` | Chi tiết câu hỏi (preview) |
| GET/POST | `/admin/questions/{id}/edit` | Sửa question |
| GET/POST | `/admin/exams` | Danh sách/tạo đề |
| GET | `/admin/exams/create` | Form tạo đề (preview) |
| GET | `/admin/exams/{id}` | Chi tiết đề (preview) |
| GET/POST | `/admin/exams/{id}/edit` | Cấu hình đề |
| POST | `/admin/exams/{id}/publish` | Publish đề |
| GET | `/admin/results` | Danh sách/tìm/lọc kết quả (preview) |
| GET | `/admin/results/{id}` | Kết quả theo kỹ năng (preview) |

### Quản lý học viên — mở rộng V2, đã dựng giao diện GET

Chi tiết nghiệp vụ và tiêu chí nghiệm thu: [STUDENT_MANAGEMENT.md](STUDENT_MANAGEMENT.md). Tất cả route yêu cầu ADMIN.

| Method | URL | Mục đích |
|---|---|---|
| GET | `/admin/students` | Danh sách, tìm kiếm, lọc trạng thái và phân trang |
| GET | `/admin/students/{id}` | Hồ sơ, mục tiêu, tiến độ và lịch sử thi |
| POST | `/admin/students/{id}/lock` | Khóa tài khoản STUDENT |
| POST | `/admin/students/{id}/unlock` | Mở khóa tài khoản STUDENT |

Hiện GET danh sách/chi tiết đã có với fixture và tìm/lọc q/status; chưa hỗ trợ page/size/attemptPage và chưa có POST lock/unlock. Bản nghiệp vụ hoàn chỉnh phải bổ sung phân trang, CSRF và redirect theo contract trên. Không có route tạo hoặc xóa học viên.

Frontend chỉ submit form tới Spring MVC Controller. Không xây endpoint `/api` cho các trang này.
