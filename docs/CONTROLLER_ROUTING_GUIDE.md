# Đọc URL và handler trong controller — 27/10/2026

## Cách hiểu đúng

Endpoint được xác định bởi HTTP method và URL đầy đủ, có thể kèm các điều kiện
request khác. `GET /admin/flashcards` và `POST /admin/flashcards` là hai endpoint
khác nhau. Code hiện ghi URL đầy đủ ngay trong từng `@GetMapping` và `@PostMapping`,
không cần ghép thêm tiền tố trên class.

Ví dụ: `@GetMapping("/admin/flashcards")` hiển thị danh sách,
`@PostMapping("/admin/flashcards")` nhận dữ liệu tạo mới.
Hàm `private` không có mapping là hàm phụ được Java gọi nội bộ,
không phải endpoint bị thiếu URL.

Một handler có thể render HTML, redirect, trả response body hoặc trả file.
POST hợp lệ thường redirect; form có lỗi phải được render lại với dữ liệu và lỗi
validation. Không ép autosave, heartbeat, ghi nhận lượt ôn hoặc media trả một trang HTML.

## Ví dụ Admin Flashcard sau khi làm rõ tên hàm

| HTTP | URL | Handler | Kết quả |
| --- | --- | --- | --- |
| GET | `/admin/flashcards` | `showFlashcardList()` | `flashcard/list.html` |
| GET | `/admin/flashcards/create` | `showCreateForm()` | Form trống |
| GET | `/admin/flashcards/{id}/edit` | `showEditForm()` | Form có dữ liệu cũ |
| POST | `/admin/flashcards` | `createFlashcardSet()` | Tạo mới; redirect hoặc hiển thị lỗi form |
| POST | `/admin/flashcards/{id}` | `updateFlashcardSet()` | Cập nhật; redirect hoặc hiển thị lỗi form |

`saveFlashcardSet()` và `renderForm()` là hàm private dùng chung. Chúng không có
mapping và không được truy cập trực tiếp từ trình duyệt.

## Phạm vi đã rà

Đã khởi tạo ứng dụng trên H2 riêng và đọc `RequestMappingHandlerMapping` thực tế:
**18 controller, 123 endpoint**. Mỗi handler hiện có một path và một HTTP method;
không trùng cặp HTTP method + URL, không map hàm private và không dùng trùng tên
handler trong cùng controller. Spring cũng khởi tạo toàn bộ mapping thành công.

- Admin: `AdminCatalogController`, `AdminDashboardController`, `AdminFlashcardController`,
  `AdminLessonController`, `AdminResultController`, `AdminScoringController`, `AdminStudentController`.
- Student: `StudentAccountController`, `StudentContentController`, `StudentDashboardController`,
  `StudentExamController`, `StudentFlashcardController`, `StudentLearningPathController`, `StudentLessonController`.
- Chung và xác thực: `HomeController`, `AuthController`, `AiAssessmentController`, `LessonAudioController`.

## Thay đổi source

- Admin Flashcard, Admin Lesson và Student Flashcard: tách tên handler mở form
  khỏi handler ghi dữ liệu, tránh overload `create()`/`edit()` khó theo dõi.
- Admin Flashcard: dùng tên `flashcardSetService`, `form`, `bindingResult`, `model`
  rõ ràng thay cho các tên viết tắt.
- AI Assessment: tách URL Student/Admin cho trang chi tiết và retry thành bốn handler;
  dùng hàm private chung và giữ nguyên kiểm tra quyền.
- Admin Scoring: tách `/admin/certificates` và `/admin/scoring-schemes` thành hai
  handler; cả hai vẫn dùng cùng trang tổng quan và phần chuẩn bị dữ liệu.

Giữ nguyên URL công khai, HTTP method, DTO form, Model attribute, trang HTML,
redirect và response body/file. Không cần sửa action form hoặc URL trong JavaScript.
Không thay nghiệp vụ, cấu hình riêng hoặc dữ liệu DB.
