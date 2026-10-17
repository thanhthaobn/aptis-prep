# Tiến độ học tập thật và điều kiện bỏ nhãn minh họa

> Cập nhật theo yêu cầu chủ project: đã bỏ nhãn “minh họa”/“mẫu” trên UI trước khi triển khai nghiệp vụ. Vì vậy các điều kiện “bỏ nhãn” dưới đây được hiểu là checklist **chuyển dữ liệu tĩnh sang dữ liệu thật**, không còn là điều kiện hiển thị chữ. Không coi việc bỏ nhãn là hoàn tất; B2/68%/kết quả fixture hiện chưa có giá trị đánh giá tài khoản thật. Trang chủ vẫn dùng nội dung giới thiệu cố định. Các công thức, quyền và tiêu chí kiểm thử dưới đây giữ nguyên.

## 1. Trạng thái và phạm vi

Đây là hợp đồng triển khai tiếp theo V2, **chưa phải nghiệp vụ đã có trong code**. Các trang hiện tại dùng fixture; B2, 68% và bài tiếp theo trên trang chủ là ví dụ. Không xóa nhãn chỉ vì đã có bảng DB hoặc đổi nội dung seed.

Thanh chính đặt tên **Tiến độ bài học**, luôn kèm `đã hoàn thành / tổng bài`. Nó không phải phần trăm đạt CEFR, điểm thi hoặc tỷ lệ đạt mục tiêu. Khi checkpoint chưa đạt, vẫn có thể hiển thị 100% bài học nhưng phải ghi trạng thái checkpoint riêng.

## 2. Công thức thống nhất

Tính cho **một learning_path_id cụ thể thuộc học viên đang đăng nhập**:

```text
S = tập lesson_id duy nhất được gán trong learning_path_items của lộ trình
N = số phần tử của S
C = số lesson_id trong S có item trạng thái COMPLETED và completed_at hợp lệ
    (đã được đồng bộ từ lesson_progress của đúng học viên)

Nếu N > 0: phần trăm = floor(100 × C / N)
Nếu N = 0: phần trăm = null, hiển thị “Chưa có bài học”
```

- `COMPLETED` là tên trạng thái đề xuất; Người 2 ánh xạ thống nhất với enum/constraint thực tế trong PR, không tạo hai cách hiểu “hoàn thành”.
- Đếm bài bắt buộc và tùy chọn đã được gán trong lộ trình; không đếm toàn bộ kho Lesson, đề thi, câu hỏi, số lượt truy cập hoặc từng Flashcard.
- Một lesson chỉ tính một lần trong cùng path. Service sinh path từ chối item trùng lesson; không dựa riêng vào position để chống trùng. Nếu cần constraint mới, dùng migration mới sau khi rà soát dữ liệu.
- Dùng phép chia thập phân/số nguyên an toàn ở backend, không chia nguyên trước khi nhân. Luôn kiểm tra `0 ≤ C ≤ N`; không che lỗi dữ liệu bằng cách chỉ ép số về 0–100.
- Làm tròn xuống để không hiện 100% khi còn bài chưa hoàn thành. Ví dụ: 8/12 → 66%; 2/3 → 66%; 0/12 → 0%; 12/12 → 100%; 199/200 → 99%.
- Quy tắc này áp dụng giống nhau cho dashboard học viên và tiến độ mà admin xem. Nếu muốn đổi cách làm tròn phải đổi một service chung và test, không tính riêng trên HTML.

## 3. Khi nào một bài được tính hoàn thành?

1. Student đăng nhập, tài khoản ACTIVE, lesson được phép truy cập, các điều kiện mở khóa nếu có đã đạt.
2. Mở trang chỉ cập nhật trạng thái đang học/lần truy cập, **không** tự hoàn thành.
3. Với phạm vi đơn giản của nhóm: Student chủ động bấm “Hoàn thành bài học”; backend xác nhận yêu cầu hợp lệ rồi lưu `lesson_progress.status/completed_at`. Không khẳng định người học đã xem hết video chỉ dựa vào mở trang hoặc timer frontend.
4. Người 2 đồng bộ item của các lộ trình active của học viên có chứa lesson đó trong cùng transaction, rồi đọc lại summary. Admin không được đánh dấu thay học viên.
5. Gửi lại form, refresh, hai tab đồng thời hoặc học lại không tăng C lần nữa. Không ghi đè thời điểm hoàn thành đầu tiên nếu không có thao tác reset nghiệp vụ được chốt riêng.
6. Flashcard lưu lượt ôn/mức ghi nhớ riêng ở `user_flashcard_progress`. Lật một thẻ hay ôn nhiều lần không cộng thêm bài; chỉ lesson chứa Flashcard được tính một lần khi được đánh dấu hoàn thành.

Không triển khai bằng nút tăng biến JavaScript/localStorage hay lưu trạng thái trong session. Sau đăng xuất, khởi động lại ứng dụng, DB vẫn là nguồn dữ liệu.

## 4. Lộ trình mới, thay đổi nội dung và dữ liệu thiếu

- Bỏ qua Goal/Placement: vẫn tạo và lưu path định hướng thật nếu có nội dung phù hợp. Có thể hiện tiến độ thật mà không cần biết CEFR hoặc mục tiêu; hai trường này hiển thị “Chưa đánh giá”/“Chưa đặt mục tiêu”.
- Có nhiều path theo kỹ năng: chọn rõ path đang xem (id/skill), hiển thị phần trăm riêng. Không lấy trung bình các phần trăm hay gộp các path cũ thành một thanh. Chưa làm bộ chọn thì hiển thị từng path bằng thẻ riêng.
- Chưa có path: “Chưa có lộ trình”. Có path nhưng không có bài: “Chưa có bài học”; phần trăm null, chưa mở checkpoint. Không coi 0/0 là 100% hoặc 0%.
- Chốt tập bài khi sinh path; không tự thay mẫu số mỗi lần dashboard đọc dữ liệu. Không xóa bài đang được tham chiếu hoặc âm thầm bỏ item vì lesson vừa bị lưu trữ. Người 2 xử lý nội dung không còn khả dụng bằng policy rõ ràng và thông báo, không đánh dấu hoàn thành hộ.
- Đổi mục tiêu/sinh lại path: lưu trữ path cũ, tạo path mới trong transaction; không reset/xóa lesson_progress. Lesson đã hoàn thành trong path mới được kế thừa từ tiến độ hợp lệ và đồng bộ item trước khi tính. Path mới có thể có phần trăm khác vì tập bài khác; không giả giữ nguyên phần trăm cũ.
- Nếu nâng phiên bản nội dung cần học lại, Người 2 phải đề xuất version/migration và policy riêng; không tự xóa completion của học viên.
- Lỗi truy vấn: hiển thị không tải được và cho tải lại; không hiển thị fixture hoặc 0% để thay thế lỗi DB.

## 5. Bài tiếp theo và checkpoint/CEFR

- Bài tiếp theo lấy từ path đang xem, sắp theo position, chọn bài chưa COMPLETED và được phép học. Không tự chọn bài ngoài path. Nếu chưa còn bài được mở khóa, hiển thị lý do thay vì liên kết giả.
- Khi hết bài: không dựng bài tiếp theo mẫu. Hiển thị đã hoàn thành phần bài học và trạng thái checkpoint tương ứng.
- Điều kiện mở checkpoint: path có ít nhất một bài bắt buộc, tất cả bài bắt buộc đã hoàn thành, có checkpoint đúng skill/target đã công bố và các điều kiện tài khoản/quyền hợp lệ. Path không có bài bắt buộc không được tự mở checkpoint; Người 2 xử lý cấu hình thiếu. Bài tùy chọn chưa học có thể khiến tiến độ toàn bộ <100% nhưng không chặn checkpoint.
- Học xong, đạt 100% hoặc đạt Practice/Full Mock không tự nâng Skill Profile. Chỉ hậu xử lý Placement/Checkpoint hợp lệ theo V2; kết quả chưa chấm, lỗi hoặc thiếu skill cần thiết không coi là đạt/trượt và không gán 0.
- Người 4 cung cấp kết quả hoàn chỉnh theo attempt/user/type/skill/scheme. Người 2 kiểm điều kiện checkpoint rồi gọi Người 1 ghi Skill Profile; một attempt không áp dụng hai lần. Người 3 điều phối Placement, gọi Người 1 xác minh/ghi Profile rồi Người 2 cập nhật path.
- Không đánh dấu các mốc A1/A2/B1 là đã đạt dựa vào số bài. Nếu chưa có đánh giá, không dùng hình dấu tick gây hiểu nhầm. Tách mục tiêu CEFR, trình độ ước lượng và tiến độ bài học trên UI.
- Màn hình đang làm đề dùng số câu đã trả lời/tổng câu và thời gian còn lại. Không thay bằng thanh tiến độ lộ trình.

## 6. Hợp đồng dữ liệu dùng chung

Người 2 đề xuất một DTO/service đọc summary dùng cho dashboard học viên và admin. Tên dưới đây là contract **cần triển khai**, không phải class đang có:

```text
LearningProgressSummary:
  pathId, skill, pathSource, pathStatus
  completedLessons, totalLessons, progressPercent (nullable)
  completedRequiredLessons, totalRequiredLessons
  nextLessonId (nullable), nextLessonTitle (nullable)
  checkpointState (not_configured / locked / available / grading / passed / failed)
```

Nguồn: Path/Item + LessonProgress do Người 2 quản lý; Goal/SkillProfile do Người 1 cung cấp; đề khả dụng do Người 3 cung cấp; Attempt/Result do Người 4 cung cấp. Trạng thái DTO dùng nhãn tiếng Việt qua mapping khi render, không in tên thuộc tính/enum lên FE.

Học viên được đọc summary của chính mình. Endpoint admin đọc cùng phép tính nhưng phải kiểm tra ADMIN và học viên đích. ID lấy từ principal/quyền đã kiểm tra; không tin user_id/path_id từ form. Không Controller gọi Controller; không frontend REST. Người 2 là đầu mối nối dashboard, Người 1 tích hợp summary vào quản lý học viên.

## 7. Điều kiện bỏ chữ “minh họa”

Chỉ bỏ nhãn ở **phạm vi đã hoàn thành tất cả điều kiện dưới đây**:

- [ ] Path và danh sách lesson được lưu DB theo đúng tài khoản, không phải fixture/hardcode.
- [ ] Hoàn thành bài qua POST + CSRF, kiểm tra ACTIVE/quyền/mở khóa; lưu transaction và chống ghi trùng.
- [ ] C/N/phần trăm/bài tiếp theo đều lấy summary thật; mục tiêu và CEFR lấy nguồn thật hoặc trạng thái chưa có.
- [ ] Không còn B2, 68%, tên bài, streak hoặc số lượt học mẫu trong vùng đã bỏ nhãn. Chưa làm streak thì bỏ chỉ số đó, không giả số.
- [ ] Dashboard Student và Admin cho cùng C/N trên cùng path; refresh, đăng nhập lại và khởi động lại giữ kết quả.
- [ ] Hai tài khoản có path/tiến độ khác nhau; sửa URL không xem hay ghi dữ liệu người khác.
- [ ] Xử lý đúng chưa path, 0 bài, chưa học, học hết, thiếu nội dung, đổi mục tiêu, chờ/lỗi chấm và lỗi tải dữ liệu.
- [ ] Có test tự động và ảnh kiểm tra desktop/mobile; người giữ foundation review, Người 2 xác nhận tiến độ, Người 1/4 xác nhận các dữ kiện tích hợp.

Seed cố định đã đi qua nghiệp vụ/DB có thể là dữ liệu demo thật của tài khoản demo; không đồng nghĩa seed luôn là fixture. Nhưng không được hiển thị tiến độ tài khoản demo cho học viên khác. Trang chủ public vẫn là ví dụ giới thiệu: **giữ nhãn minh họa**, kể cả khi dashboard học viên đã chạy thật. Không xóa nhãn hàng loạt trong fragment/template. Vùng kết quả chỉ bỏ nhãn khi điểm/nhận xét của đúng người đã nối thật; không cần chờ AI mới được dùng tiến độ bài học thật.

## 8. Phân công và thứ tự bàn giao

| Thành viên | Việc bắt buộc | Bàn giao/kiểm thử |
|---|---|---|
| Người 1 | Goal/Profile có thể thiếu; ACTIVE; tiến độ thật vào admin; service xác minh/ghi Profile từ Placement/Checkpoint; cung cấp đánh giá AI cho 4 | Snapshot theo user/skill, không mặc định B2; tài khoản mới/khóa/quyền; không điều phối Placement |
| Người 2 — đầu mối | Sinh/lưu path định hướng và cá nhân; complete lesson; đồng bộ item; công thức/next lesson/checkpoint; dashboard thật | Summary duy nhất, test C/N/idempotency/đổi mục tiêu/rỗng; PR bỏ nhãn đúng phạm vi |
| Người 3 | Đề công bố/checkpoint đúng skill/target, media và tham chiếu scoring version do 1 cung cấp; điều phối Placement → Profile 1 → path 2 | Service đề cho 2/4; test draft/thiếu đề/scheme sai/retake/kết quả trả chậm; không ghi progress/profile hoặc band |
| Người 4 | Attempt/result đúng owner, trạng thái chờ/lỗi/hoàn tất; nhận AI 1 rồi tổng hợp; lịch sử/kết quả UI | Kết quả hợp lệ cho Placement 3/checkpoint 2; test submit/retry; không tăng C/ghi Profile |

1. Chốt DTO, enum trạng thái, policy tập bài và hoàn thành trong PR contract (1/2/3/4 cùng xác nhận).
2. Người 2 làm complete + summary trên path DB nhỏ; Người 1 cung cấp user/Goal/Profile; Người 3 cung cấp nội dung đã công bố. Nghiệm thu thanh tiến độ bài học trước, không chờ AI.
3. Người 2 nối dashboard và Người 1 nối admin từ cùng summary. Kiểm thử hai tài khoản, lưu bền, đổi mục tiêu.
4. Người 3/4 nối đề/checkpoint/result; Người 1/2 tích hợp CEFR theo V2. Chỉ bật chức năng khi đủ điều kiện, không dùng kết quả mẫu để mở checkpoint.
5. Người 2 gửi PR bỏ nhãn tại dashboard đã nối thật; người giữ foundation review ảnh + test + checklist. Mục nào còn preview vẫn giữ nhãn và nút chưa triển khai vẫn disabled.

Không sửa migration V001–V005 đã chạy; schema mới phải có migration mới được review.
