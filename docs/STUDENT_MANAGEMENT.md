# Quản lý học viên dành cho quản trị viên

## 1. Phạm vi và người phụ trách

Đây là phần mở rộng V2 do nhóm bổ sung. Đã có giao diện danh sách/chi tiết và route GET, dùng dữ liệu minh họa chỉ đọc; chưa nối database hoặc nghiệp vụ khóa/mở khóa.

### Bản giao diện được dựng sẵn

- AdminStudentController: GET `/admin/students` và GET `/admin/students/{id}`.
- StudentPreviewService và DTO StudentPreview: bốn hồ sơ minh họa, không đọc/ghi UserRepository; ID mẫu 1001–1004 không phải ID tài khoản thật.
- Danh sách hỗ trợ q và status trên fixture, tìm tên/email không dấu; chưa có phân trang dữ liệu thật.
- Template `admin/student/list.html` và `admin/student/detail.html`, sidebar active `students`.
- Chi tiết có hồ sơ, mục tiêu, tiến độ, kỹ năng, lịch sử và dialog xác nhận khóa/mở khóa.
- Dialog dùng `admin-students.js` để mở/đóng; nút Xác nhận disabled. Không có POST lock/unlock, không thay đổi tài khoản hay giả lập trạng thái khóa.
- Nhãn “Dữ liệu minh họa” phải giữ trong bản preview. Khi nối dữ liệu thật, bỏ fixture khỏi luồng chạy rồi mới bỏ nhãn.
- Người 1 thay nguồn preview bằng service/database theo đặc tả dưới đây, thêm phân trang và POST khóa/mở khóa; giữ layout/component đã có. Cập nhật test từ dữ liệu mẫu sang dữ liệu kiểm thử thật.

Người 1 (tài khoản, chấm/kết quả/Profile) chịu trách nhiệm trọn luồng quản lý học viên: Repository → Service → Controller → Thymeleaf → Test. Người 2 cung cấp dữ liệu tiến độ và history/result DTO; Người 3 cung cấp attempt metadata. Các bên không ghi xuyên Entity/Repository của nhau.

Chức năng gồm xem danh sách, tìm kiếm/lọc, xem chi tiết và tiến độ, khóa/mở khóa học viên. Không tạo học viên thay người dùng, không sửa mật khẩu/role, không xóa tài khoản hoặc lịch sử thi.

## 2. Use case bổ sung

| Mã | Chức năng | Kết quả |
|---|---|---|
| UC-A11 | Xem danh sách và tìm kiếm học viên | Danh sách phân trang, chỉ có role STUDENT |
| UC-A12 | Xem hồ sơ và tiến độ học viên | Tổng quan mục tiêu, kỹ năng, học tập và kết quả thi |
| UC-A13 | Khóa học viên | ACTIVE → LOCKED, chặn đăng nhập và các phiên đang sử dụng |
| UC-A14 | Mở khóa học viên | LOCKED → ACTIVE, cho phép đăng nhập lại |

Mọi thao tác yêu cầu ADMIN và phải kiểm tra ở backend. Student/khách không được truy cập bằng cách nhập URL trực tiếp.

## 3. Danh sách và tìm kiếm

Route: GET `/admin/students`.

- Cột: họ tên, email, ngày đăng ký, trạng thái, thao tác “Xem chi tiết”.
- Tìm theo họ tên hoặc email, từ khóa tối đa 255 ký tự, trim khoảng trắng, không phân biệt hoa thường.
- Dùng query parameter `q`, lọc trạng thái `status` (ACTIVE/LOCKED/INACTIVE hoặc để trống).
- Phân trang `page` bắt đầu từ 0; mặc định `size=20`, tối đa 100, không chấp nhận số âm.
- Sắp xếp mặc định created_at giảm dần rồi id giảm dần để ổn định giữa các trang.
- Điều kiện role STUDENT luôn có trong truy vấn; bộ lọc không được làm lộ tài khoản ADMIN.
- Dùng tham số bind của JPA/Repository, không ghép SQL từ input. Khi dùng LIKE, escape ký tự wildcard nếu quy định tìm chuỗi literal.
- Giữ q/status/size khi chuyển trang hoặc quay từ chi tiết; xử lý query parameter bằng URL builder, không nối chuỗi input trực tiếp.
- Chưa có học viên: “Chưa có học viên”. Không khớp bộ lọc: “Không tìm thấy học viên phù hợp”.
- Số học viên trên dashboard phải lấy số liệu thật từ cùng phạm vi role STUDENT, không tiếp tục gán 0 cố định.

## 4. Hồ sơ và tiến độ

Route: GET `/admin/students/{id}`. Không tìm thấy user hoặc user không có role STUDENT thì trả trang không tìm thấy, không hiển thị thông tin quản trị viên.

| Khu vực | Dữ liệu | Nguồn |
|---|---|---|
| Hồ sơ | Họ tên, email, ngày đăng ký, trạng thái | users |
| Mục tiêu | Mục tiêu đang áp dụng, thời hạn, mục tiêu từng kỹ năng | user_goals, user_goal_skills |
| Năng lực | Mức CEFR từng kỹ năng, thời điểm cập nhật | user_skill_profiles |
| Lộ trình | Kỹ năng, trình độ đầu/cuối, trạng thái, tiến độ | learning_paths, learning_path_items, lesson_progress |
| Học tập | Bài đã hoàn thành, hoạt động học gần nhất | lesson_progress |
| Flashcard | Số thẻ đã ôn, lượt ôn, mức ghi nhớ | user_flashcard_progress |
| Lượt thi | Loại đề, ngày làm, trạng thái, kết quả | exam_attempts, mock_exams, exam_skill_results |

- CEFR chưa có thì ghi “Chưa đánh giá”, không tự đặt A1/B1/B2.
- Không có Goal thì ghi “Chưa đặt mục tiêu”.
- Tiến độ lộ trình = số lesson bắt buộc đã hoàn thành / tổng lesson bắt buộc × 100; đếm lesson riêng biệt, không nhân bản vì join. Không có lesson bắt buộc thì ghi “Chưa có bài học bắt buộc”, không chia cho 0.
- Theo V2, service học tập đồng bộ lesson_progress và learning_path_items; không tự suy ra hoàn thành chỉ vì đã mở trang bài học.
- Chưa chấm/AI lỗi giữ điểm NULL và nhãn “Chưa chấm”/“Chấm chưa thành công”; không hiển thị 0 thay cho NULL.
- Lịch sử thi phân trang riêng với `attemptPage`; không tải toàn bộ bài làm/audio vào trang tổng quan.
- Không hiển thị password_hash, đường dẫn vật lý, API key hoặc thông tin hạ tầng.
- Trang quản trị chỉ đọc tiến độ; không sửa điểm, hoàn thành lesson hay nâng CEFR thay học viên.
- Không tự ước lượng “thời gian học” từ started_at của lượt thi hoặc last_accessed_at của lesson vì schema chưa lưu thời lượng học đáng tin cậy.

## 5. Khóa và mở khóa

Route POST `/admin/students/{id}/lock`, POST `/admin/students/{id}/unlock`.

| Trạng thái hiện tại | Khóa | Mở khóa |
|---|---|---|
| ACTIVE | Chuyển LOCKED | Giữ nguyên ACTIVE |
| LOCKED | Giữ nguyên LOCKED | Chuyển ACTIVE |
| INACTIVE | Từ chối thao tác | Từ chối thao tác |

INACTIVE là trạng thái vô hiệu hóa riêng, không được vô tình kích hoạt lại bằng nút mở khóa. Gửi lặp thao tác trên ACTIVE/LOCKED phải an toàn, không đảo trạng thái. Dùng lệnh khóa và mở khóa riêng, không dùng toggle.

1. Admin chọn học viên, xem xác nhận có họ tên/email và hành động.
2. Form POST có CSRF; service kiểm tra role ADMIN, đích là STUDENT và trạng thái hợp lệ.
3. Cập nhật status và updated_at trong transaction; tránh ghi đè thay đổi đồng thời bằng khóa bản ghi hoặc cơ chế optimistic locking thích hợp.
4. Redirect về chi tiết và hiển thị thông báo kết quả tiếng Việt.

Không khóa chính mình hoặc tài khoản ADMIN, kể cả khi sửa id trong form. Không lấy role, email, điểm hoặc status tùy ý từ form để ghi vào Entity; nhận id và lệnh cụ thể rồi đọc dữ liệu từ database.

### Hiệu lực với phiên đăng nhập

CustomUserDetailsService hiện chỉ đọc status lúc đăng nhập. Đổi users.status không tự làm mất quyền của session đã đăng nhập; thành viên phải bổ sung phần này để khóa có hiệu lực đầy đủ.

- Sau khi khóa commit thành công, yêu cầu được xác thực tiếp theo của học viên phải kiểm tra trạng thái và hủy phiên/đăng xuất nếu không ACTIVE. Có thể dùng filter/interceptor đọc trạng thái hiện tại hoặc cơ chế thu hồi session tương đương.
- Các thao tác lưu đáp án/nộp bài/hoàn thành lesson phải kiểm tra trạng thái tài khoản trong service trước khi thay đổi dữ liệu để hạn chế đua giữa khóa và POST đang chạy.
- Khóa không xóa đáp án đã lưu, lịch sử, goal hoặc tiến độ; không tự nộp bài thay học viên.
- Một yêu cầu đã hoàn tất trước thời điểm khóa không bị hoàn tác.
- Sau mở khóa, học viên đăng nhập lại; không phục hồi session cũ. Lượt thi còn tiếp tục được hay đã hết hạn phụ thuộc luật thời gian của Exam Engine.

## 6. Cấu trúc triển khai và database

Gợi ý: AdminStudentController, AdminStudentService, StudentSummaryView, StudentProgressView; mở rộng UserRepository hiện có, không tạo Entity User thứ hai. Controller gọi Service, không gọi Repository trực tiếp.

Service tổng hợp tiến độ dùng service đọc dữ liệu do Người 2/3 cung cấp; result/score đọc DTO do Người 1 phát hành. Không copy logic tính CEFR hay chấm điểm vào module quản lý học viên. Khi các service chưa xong, hiển thị trạng thái chưa có dữ liệu; không seed điểm giả cho học viên thật.

Schema hiện có đủ dữ liệu cốt lõi và status ACTIVE/INACTIVE/LOCKED. Chưa cần migration mới chỉ để thêm danh sách/tìm kiếm/khóa. Nếu cần bổ sung index hoặc version cho xử lý đồng thời, đăng ký migration tiếp theo, không sửa V001–V005.

## 7. Giao diện

- Dùng body.admin-theme, admin-sidebar, page-header, toolbar, data-table, pagination và field theo UI_GUIDE.md.
- Thêm mục “Học viên” sau “Tổng quan”; cả trang danh sách và chi tiết cùng active khóa `students`.
- Template dự kiến: `admin/student/list.html`, `admin/student/detail.html`.
- Trạng thái hiển thị: ACTIVE → “Đang hoạt động”, LOCKED → “Đã khóa”, INACTIVE → “Ngừng hoạt động”.
- Kỹ năng giữ tên Reading, Listening, Writing, Speaking, Grammar & Vocabulary; giữ Flashcard.
- Nút “Khóa tài khoản” ở ACTIVE; “Mở khóa tài khoản” ở LOCKED. INACTIVE chỉ hiển thị trạng thái, không cho mở khóa.
- Xác nhận bằng modal có hành vi bàn phím/focus đầy đủ hoặc trang Thymeleaf xác nhận; không dùng link GET để khóa.
- Không hiển thị ghi chú lập trình hay thông báo triển khai lên giao diện.

## 8. Tiêu chí nghiệm thu

- Admin xem được danh sách chỉ gồm STUDENT, tìm theo tên/email và lọc đúng trạng thái.
- Phân trang giữ đúng bộ lọc và thứ tự; không khớp dữ liệu có trạng thái rỗng phù hợp.
- Trang chi tiết tổng hợp dữ liệu đúng học viên, không tự tạo điểm hoặc CEFR khi thiếu dữ liệu.
- Progress không đếm trùng lesson, không chia cho 0; kết quả chưa chấm không thành điểm 0.
- Khóa/mở khóa cập nhật đúng trạng thái; lệnh gửi lặp không đảo trạng thái.
- User INACTIVE không được mở bằng unlock; sửa id để khóa ADMIN bị từ chối.
- User đã khóa không đăng nhập được; phiên đang mở bị chặn ở yêu cầu kế tiếp và không được ghi tiến độ/đáp án mới.
- Mở khóa cho đăng nhập mới; toàn bộ tiến độ/bài làm cũ còn nguyên.
- Student/khách không truy cập được các route quản trị; POST thiếu CSRF bị từ chối.
- Có test quyền, trạng thái, tìm kiếm/phân trang, dữ liệu tiến độ và hiệu lực khóa với phiên đang hoạt động.
- Có ảnh desktop/mobile và URL kiểm tra trong Pull Request.
