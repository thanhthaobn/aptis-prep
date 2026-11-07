# Quy chuẩn giao diện AptisPrep

## Thanh cấp độ và nút quay lại

- Thẻ lộ trình giữ ba thông tin ngắn: Hiện tại, Đích chặng, Mục tiêu; bên dưới luôn có thanh A1–C1. Dùng chung `path-stage :: stageScale(stage)` cho tổng quan và trang kỹ năng. Phần tô biểu thị trình độ đã đạt, ô viền biểu thị đích chặng, ngôi sao biểu thị mục tiêu cá nhân.
- Menu và tiêu đề trang Placement dùng nhãn **Kiểm tra đầu vào**, giữ biểu tượng clipboard. Không lặp hướng dẫn dài trên trang.
- Nút quay lại dùng `back-link :: backLink(url)` với nhãn **Quay lại**, biểu tượng chevron và kiểu dáng chung. Chỉ đặt ở trang chi tiết hoặc biểu mẫu; các trang chính trong menu không có nút về Tổng quan. Giữ đường dẫn về trang cha phù hợp; chi tiết đề Placement quay về trang đánh giá đúng kỹ năng. Các nút chuyển câu/thẻ và thoát bài thi vẫn có hành vi riêng.

## Cách viết dễ đọc cập nhật 27/10/2026

Template không dùng các helper bắt đầu bằng `#strings`, `#lists`, `#fields`,
`#numbers` hoặc `#temporals`. Dấu `#` trong màu CSS, selector và liên kết tới một
phần của trang vẫn giữ nguyên vì đó là cú pháp HTML/CSS cần thiết.

- Chuỗi có nội dung: `${text != null and !text.isBlank()}`. Giữ kiểm tra khoảng
  trắng vì helper `#strings.isEmpty` trước đây cũng coi chuỗi toàn khoảng trắng là rỗng.
- Lỗi tổng hợp: `${display.globalErrors('form')}` cho lỗi chung,
  `${display.allErrors('form')}` cho toàn bộ lỗi; thay `form` bằng tên Model của form.
  Dùng `th:if` để ẩn khi không có lỗi, `th:each` và `th:text` để hiển thị an toàn.
  Lỗi từng trường vẫn dùng `th:errors="*{title}"` với đúng tên thuộc tính DTO.
- Chuỗi ghép: dùng `${'Bài học ' + lesson.title}` thay cú pháp `|...|`.
  Danh sách cấp độ/kỹ năng lấy qua `display.learningLevels()`, `display.scoringLevels()`
  và `display.skillCodes()`, không viết danh sách lồng trực tiếp trong biểu thức.
- Ngày giờ: `${display.date(value)}`, `${display.dateTime(value)}`,
  `${display.dateTimeWithSeconds(value)}` hoặc `${display.timeAndDate(value)}`.
- Các số trang: `${display.pageNumbers(start, end)}`.
- Nhãn trạng thái: dùng `th:switch`/`th:case` trong fragment `statusBadge`, tránh
  lặp cùng phép chuyển chuỗi và một điều kiện fallback quá dài.

`DisplayFormatting` trong package `controller` là lớp hỗ trợ trình bày được
`NavigationModelAdvice` đưa vào Model dưới tên `display`. Lớp này không đọc DB,
không xử lý nghiệp vụ hoặc lưu dữ liệu. Các định dạng ngày giữ nguyên như trước.

`AccountProfileService.snapshot()` được chia thành các bước lấy học viên, lấy
Goal hiện tại, chuyển Goal và chuyển Profile sang DTO. Hai hàm riêng
`toGoalView()` và `loadSkillViews()` dùng vòng lặp rõ ràng; danh sách kỹ năng
Goal được lấy một lần rồi tạo cả map cấp độ và map điểm mục tiêu. Không đổi
cấu trúc `AccountSnapshot` hoặc dữ liệu lịch sử.

Tài liệu này là hợp đồng giao diện cho mọi Pull Request có Thymeleaf/CSS.

## Cách đọc template và format (26/10/2026)

HTML dùng Prettier, thụt lề 2 khoảng trắng. `htmlWhitespaceSensitivity: ignore`
giúp thẻ inline đóng/mở trên các dòng riêng, tránh kiểu `</label` rồi dòng sau
mới có `>`. Nội dung trong textarea/pre vẫn phải được bảo toàn.
Chạy `npm run format:web` để format và `npm run format:web:check` để kiểm tra.

| Cú pháp | Ý nghĩa |
| --- | --- |
| `${catalog.title}` | Lấy dữ liệu controller đã đưa vào model |
| `*{title}` | Trường của đối tượng form hiện tại, khai báo bằng `th:object` |
| `@{/admin/questions}` | Tạo URL, có xét context path của ứng dụng |
| `~{fragments/navbar :: navbar}` | Chọn fragment `navbar` trong file `fragments/navbar.html` |
| `th:replace` | Thay cả thẻ hiện tại bằng fragment; dùng chung navbar/sidebar/footer |
| `th:with="itemPath=${'items[' + itemStat.index + ']'}"` | Đặt tên đường dẫn của mục đang lặp, ví dụ `items[0]` |
| `th:name="${itemPath + '.content'}"` | Ghép đường dẫn với tên trường bằng phép nối chuỗi `+` |
| `${items != null and !items.isEmpty()}` | Danh sách có dữ liệu; kiểm tra null trước khi gọi phương thức |
| `${items == null or items.isEmpty()}` | Danh sách chưa có hoặc rỗng |
| `${items.size()}` | Đếm phần tử của danh sách đã được khởi tạo |

`::` và `~{...}` là cú pháp chuẩn của Thymeleaf, không phải lỗi HTML. Không bỏ
chúng bằng cách chép navbar/sidebar vào mọi trang hoặc chuyển sang cú pháp cũ.

Tên file dùng chữ thường, ngăn cách bằng dấu `-`; tên fragment dùng camelCase
theo cùng ý nghĩa. Giữ các thành phần cùng chức năng trong một file khi hợp lý:

| File trong `fragments/` | Fragment |
| --- | --- |
| `navbar.html` | `navbar` |
| `admin-sidebar.html` | `adminSidebar(activePage)` |
| `student-sidebar.html` | `studentSidebar(activePage)` |
| `footer.html` | `footer` |
| `status-badge.html` | `statusBadge(status)`; `statusText(status)` cho dạng chữ |
| `student-pagination.html` | `studentPagination(currentPage, totalPages, totalItems, baseUrl)` |
| `lesson-history-context.html` | `lessonHistoryContext(context, mediaPrefix)` |
| `path-stage.html` | `pathStage(skill)` và `pathLevelRange(skill)` |

Ví dụ: `th:replace="~{fragments/status-badge :: statusBadge(${attempt.status})}"`.
`pagination.html :: pagination(page, baseUrl)` nhận Spring Data Page, khác với
`studentPagination` nhận các số trang riêng; không hoán đổi hai contract này.

Điều kiện dài được dùng nhiều lần nên đặt tên bằng `th:with` tại phần tử cha.
Trang bài học dùng `productive`, `hasQuestions`, `hasAttempts`, `hasMethodReview`,
`hasMethod` và `hasVideo`; các phần con chỉ cần `th:if="${hasMethod}"` thay vì
lặp lại toàn bộ điều kiện. Đây là biến trình bày trong template, không truy vấn
DB hoặc thay đổi điều kiện hoàn thành bài học.

`admin/catalog/form.html` chỉ giữ khung trang và chọn đúng form. Nội dung từng
form nằm ở `admin/catalog/question-form.html` và `admin/catalog/exam-form.html`.
Form bài học nằm ở `admin/lesson/form.html`.

Các form như bài học vẫn dùng `th:field="*{title}"` cho trường đơn để giữ binding
tự động của Spring. Hai fragment catalog dùng đối tượng rõ ràng, ví dụ
`name="title" th:value="${examForm.title}"`, và khai báo kiểu model bằng comment
`@thymesVar` để IDE nhận diện các thuộc tính trong fragment. Đây là thông tin
kiểu cho IDE, không phải tắt kiểm tra lỗi hay thêm dữ liệu nghiệp vụ.
Các danh sách nhiều cấp khai báo đường dẫn một lần bằng `th:with`, rồi dùng
phép nối chuỗi `+` quen thuộc. Không lặp lại cả đường dẫn dài ở mỗi ô nhập:

```html
<div th:each="item, itemStat : ${questionForm.items}"
     th:with="itemPath=${'items[' + itemStat.index + ']'}">
  <div th:each="option, optionStat : ${item.options}"
       th:with="optionPath=${itemPath + '.options[' + optionStat.index + ']'}">
    <input th:name="${optionPath + '.content'}" th:value="${option.content}" />
  </div>
</div>
```

Sau render, tên là `items[0].options[1].content`; Spring vẫn gán vào DTO tương
ứng khi POST. Không thể dùng chung `name="content"` cho mọi đáp án vì sẽ mất
thông tin đáp án thuộc mục nào. Form đề thi dùng `sectionPath`, form bài học
dùng `checkPath` theo cùng cách. Textarea dùng `th:text`, radio/checkbox dùng `th:checked`, option
dùng `th:selected` để giữ dữ liệu khi sửa hoặc khi form có lỗi. Không ghép nội
dung người dùng vào HTML bằng `th:utext`.

`__${...}__` là preprocessing hợp lệ của `th:field` cho danh sách, không phải
code sai. Các form hiện tại dùng `th:name` tường minh để tránh lồng cú pháp này.
Không chỉ xóa dấu `__` rồi giữ nguyên `th:field`, vì có thể làm hỏng binding.
Khi thêm/xóa dòng, phải cập nhật đồng thời `name`, `id` và `label[for]`.

Tham khảo chính thức: [Template fragments](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#template-layout)
và [Dynamic fields](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html#dynamic-fields).

## Đọc source và theo dõi form hiện tại (25/10/2026)

Phần này mô tả source đang chạy; những mô tả preview trong các quyết định cũ
bên dưới là lịch sử triển khai, không phải danh sách chức năng còn thiếu.

| Khi cần sửa | Đọc ở đâu |
| --- | --- |
| Trang và form | `src/main/resources/templates/`, chia `auth`, `admin`, `student`, `flashcard` |
| Header/sidebar/thông báo dùng lại | `templates/fragments/` |
| Menu mobile | `static/js/navigation.js` được navbar nạp bằng `defer` |
| Thêm ý hỏi/phần thi ở Admin | `static/js/catalog-editor.js` được `admin/catalog/form.html` nạp |
| Autosave, timer, audio, ghi âm khi thi | `static/js/exam-runner.js` được `student/attempt.html` nạp |
| Soạn bài học / học bài | `static/js/lesson-author.js` / `lesson-player.js` |
| Soạn và ôn Flashcard | `static/js/flashcard-editor.js` / `flashcard-study.js` |
| Biểu đồ hoạt động hồ sơ | `static/js/profile-activity.js` |

Luồng form cơ bản: GET vào controller → service đọc dữ liệu → controller đặt
model → Thymeleaf render HTML. Khi nhấn lưu: trình duyệt gửi POST + CSRF →
controller nhận DTO trong `dto/form` → service kiểm nghiệp vụ và lưu qua
repository → redirect về GET. Lỗi nhập liệu phải quay lại form và giữ thông tin
có thể sửa; JavaScript chỉ hỗ trợ hiển thị và lưu nháp, backend vẫn kiểm lại.

Ví dụ đăng ký: `auth/register.html` → POST `/register` →
`AuthController.register` + `RegisterForm` → `UserService.registerStudent` →
`UserRepository`. Backend trả lỗi từng trường nếu thiếu dữ liệu hoặc xác nhận
mật khẩu khác nhau; form có `required`, giới hạn độ dài tương ứng DTO.

Ví dụ bài học: `StudentLessonController` render `student/lesson/detail.html`;
`lesson-player.js` hỗ trợ phiên nghe/autosave. Các POST `/check`, `/response`,
`/assess` được chuyển vào `LessonTeachingService`; điều kiện nghe đủ được xử lý
riêng bởi `LessonListeningService`, không tin phần trăm do client khai báo.

Ví dụ bài thi: `StudentExamController` → `ExamAttemptService` tạo snapshot và
lượt thi → `student/attempt.html` + `exam-runner.js`. Lưu đáp án qua
`/answers/autosave`; nộp qua `/submit`. Sau khi lưu bài, outbox/orchestration
chuyển phần chấm sang grading; kết quả được đọc lại ở trang result. Không đưa
lệnh gọi Gemini hoặc đáp án đúng vào JavaScript của học viên.

### Package nên giữ và mức độ phức tạp

`controller`, `dto`, `service`, `repository`, `entity` vẫn là các tầng MVC quen
thuộc. Các nhánh `service/learning`, `exam`, `grading`, `scoring`, `account`,
`placement`, `storage`, `catalog`, `admin`, `auth`, `student` đều đang có lớp
được sử dụng; chưa có bằng chứng cần bỏ hoặc thêm package nghiệp vụ.

- `event` dùng cho thông báo sau giao dịch, liên kết cập nhật Goal/lộ trình
  và kết quả AI; không thay bằng việc gọi mạng trong transaction.
- `exception` giữ lỗi nghiệp vụ, quyền truy cập và không tìm thấy dữ liệu,
  cùng cách trả thông báo phù hợp.
- `service/contract` chứa giao tiếp giữa các luồng; không cần thêm interface
  cho mọi service chỉ để có cấu trúc `service/impl`.
- Bean `resultLabels` được Thymeleaf gọi dù có thể không thấy lời gọi từ Java;
  không xóa bean chỉ dựa trên Find Usages trong Java.

Phần khó chủ yếu nằm ở `AiAssessmentService`, `AttemptFinalizationService`,
`ExamAttemptService`, `LessonTeachingService`, `CatalogService` và
`LearningPathService`. Chúng vẫn lớn; khi sửa tiếp nên tách theo một trách nhiệm
cụ thể và giữ ranh giới transaction/lock. Việc di chuyển file sang package mới
không tự làm các luồng này dễ hiểu hơn.

HTML hiện đã tách các script dài ra file riêng. Formatter dùng
`htmlWhitespaceSensitivity: css` để thẻ block dễ đọc hơn; dùng
`npm run format:web` / `npm run format:web:check` và kiểm tra lại các vùng text
inline sau khi sửa template. Bootstrap vẫn phụ trách collapse của sidebar,
các trang vẫn dùng CSS chung trong head fragment.

Thanh tìm kiếm/bộ lọc dùng chung `.toolbar` của Student/Admin: padding 10px 12px, gap 8px, nhãn 13px, ô nhập/select/nút 14px cao 38px desktop, viền mảnh và không shadow. Ô tìm kiếm co giãn cùng hàng với bộ lọc nếu đủ chỗ, không ép chiếm cả hàng trên desktop. Mobile ≤760px xếp ô lọc theo hàng riêng và giữ vùng thao tác tối thiểu 44px. Không áp các kích thước này lên form tạo/sửa hoặc đăng nhập.

## Quyết định giao diện mới nhất

Navbar hiện có active state theo URL qua NavigationModelAdvice.navSection: `/` → Trang chủ; `/student/**` → Học tập; `/admin/**` → Quản trị; login/register → đúng mục. Không chọn theo role khi user đang ở trang chủ. Link active có nền màu nhẹ, chữ màu theme đậm và aria-current, không viền, không đổ bóng; vẫn hiện trên mobile. Menu bo góc 8px, cùng padding và độ đậm chữ ở cả trạng thái thường/active để không đổi kích thước khi chuyển trang. Nút Đăng ký active dùng nền đậm hơn, không thêm khung ngoài. Không sửa active riêng từng template; không loại bỏ viền focus dành cho bàn phím.

Menu “Chứng chỉ & Chấm điểm” thay “Cấu hình”, liên kết `/admin/certificates` và dùng activePage `settings`. Template admin/settings/index, certificate, scheme tuân theme teal/tiêu đề không khung. Không hiển thị cấu hình kỹ thuật hay trạng thái Gemini ở đây. Bàn giao UC-A03–A05: SCORING_UI_HANDOFF.md.

Tiêu đề học viên cũng bỏ toàn bộ nền/viền/bóng/bo góc bao quanh như admin, padding 0 và giữ khoảng dưới 18px. Áp dụng Placement, Khám phá, Bài học, Đề thi, Kết quả, Hồ sơ, các chi tiết và banner tiêu đề tổng quan. Chữ dùng font chung; tiêu đề chi tiết kế thừa màu đậm của skill, trang chung màu xanh lá. Các quy tắc khung tiêu đề Student cũ (padding 12px 16px/bo 14px) được thay thế. Không xóa thẻ header ngữ nghĩa hoặc thay màu/khung card nội dung, bản đồ kỹ năng bên dưới.

Tiêu đề admin (page-header và dashboard-header, cả danh sách/chi tiết/form/Cấu hình) bỏ nền/viền/bóng/bo góc bao quanh; không xóa thẻ header ngữ nghĩa. H1 21–26px dùng font chung, màu teal đậm; mô tả 15px, khoảng dưới 18px. Hàng nút gọn, xuống dòng trên mobile. Chỉ bỏ khung đầu trang, giữ card nội dung, trạng thái và xác nhận thao tác. Tiêu đề học viên giữ cách thu gọn đã chốt riêng.

Khung tiêu đề đầu trang Student (Placement, Khám phá, Bài học, Đề thi, Kết quả, Hồ sơ và chi tiết) dùng padding 12px 16px, bo 14px, h1 khoảng 21–26px, mô tả 15px. Không đặt min-height lớn; cho chữ xuống dòng trên mobile. Bỏ eyebrow “CỨ THỬ MỘT BƯỚC NHỎ” ở trang khám phá để tránh chiếm chiều cao. Quy tắc này không thay các card nội dung bên dưới, trang chủ hoặc header admin.

Navbar dùng kích thước px độc lập với root font trang chủ/app: nav-wrap cao 72px desktop, 64px mobile (header thêm viền dưới 2px), logo 24px/21px, chữ menu/nút 15px, khung rộng tối đa 1440px với lề 32px mỗi bên desktop/12px mobile. Không dùng rem để sửa kích thước navbar theo theme role. `scrollbar-gutter: stable` giữ vị trí khi trang ngắn/dài. Màu thương hiệu vẫn theo role; hình học không đổi giữa public, Student và Admin.

Bài học, Đề thi và Kết quả học viên dùng chung class `compact-catalog` (thay tên `lesson-catalog` cũ) trong hai template content/list và detail. Font Arial/Segoe UI, card gọn, ba cột ở desktop rộng, hai/một cột khi hẹp. Tất cả có data-skill/data-kind để dùng chung palette; Tổng hợp dùng xanh xám trung tính. Cấu trúc đề tổng hợp tô từng phần theo skill, không gán cả đề thành một kỹ năng. Trạng thái chờ/lỗi chấm vẫn có chữ rõ ràng, không chỉ phân biệt bằng màu.

Màu kỹ năng đã dùng chung cho trang chủ (`skill-*`), nút bản đồ (`node-*`), khám phá (`explore-*`) và Bài học (`data-skill/data-kind`); các selector cùng khai báo một palette, không thêm bảng màu riêng. Font chung qua `--font-ui: Arial, Segoe UI, sans-serif` để chữ Việt nhất quán; quy định font Segoe UI trước đây được thay thế. Bài học dùng class `lesson-catalog`: card gọn 1rem padding, tiêu đề 1.25rem/700, nút vừa nội dung, desktop rộng ba cột; không đổi cỡ chữ admin/trang chủ đã chốt.

Palette kỹ năng mới thay thế các phối màu trước: Reading xanh dương, Listening vàng (chữ nâu đậm trên nút vàng), Writing và Speaking cùng tím, Flashcard/Grammar & Vocabulary đỏ. Dùng chung modifier `explore-*` và `data-skill`/`data-kind` trên thẻ Bài học và main chi tiết, không tô màu sidebar/admin theo lesson. Thẻ khám phá thu gọn padding/biểu tượng, tiêu đề 2rem, mô tả 1.125rem và bỏ dòng chip phụ. Flashcard không kéo toàn chiều ngang desktop. Không quay lại phối màu hồng/cam/tím Reading cũ.

Trang Khám phá dùng `explore-grid`, `explore-card` và modifier `explore-reading/listening/writing/speaking/vocabulary`; mỗi card có màu nền nhạt, icon trên nền trắng, chữ/nút đậm cùng nhóm màu. Giữ liên kết tới Bài học theo skill. Writing/Speaking có `lesson-practice`, ô viết không lưu và khung ghi âm disabled; không hứa có AI hay lưu bài khi chưa tích hợp.

Theo yêu cầu chủ project: trang chủ dùng cỡ chữ gốc 20px, khu học viên/admin và đăng nhập 16px. Nhãn “minh họa”/“mẫu” đã bỏ khỏi UI, kể cả nhãn hỗ trợ đọc màn hình; các quy tắc cũ yêu cầu giữ nhãn bên dưới được thay thế bởi quyết định này. Đây chỉ là thay đổi trình bày, không phải xác nhận dữ liệu thật. Controller/service Preview và nút nghiệp vụ disabled vẫn giữ. Thành viên phải dùng STUDENT_UI_HANDOFF, ADMIN_UI_HANDOFF và REAL_PROGRESS_CONTRACT để biết phần chưa triển khai.

Trang chi tiết học viên dùng `student/content/list.html` và `detail.html`, class `student-content`, `study-card-grid`, `study-facts`, `study-lesson-blocks`, `study-flashcards`. Giữ nội dung gọn, chữ 18px, theme xanh lá và điều hướng chung. Nút có dữ liệu giả không được ghi tiến độ hay gọi AI. Hướng dẫn nghiệp vụ tiếp nối: [STUDENT_UI_HANDOFF.md](STUDENT_UI_HANDOFF.md).

## Thanh lộ trình: trang giới thiệu và dữ liệu thật

Khi nối dữ liệu, bắt buộc dùng [REAL_PROGRESS_CONTRACT.md](REAL_PROGRESS_CONTRACT.md): phần trăm hoàn thành bài học = floor(100 × C/N), N=0 hiển thị chưa có bài học, không suy ra CEFR. Nhãn minh họa chỉ bỏ theo checklist từng vùng, không xóa toàn cục.

- Trang chủ dùng số liệu cố định nên giữ nhãn “Minh họa lộ trình · Mục tiêu mẫu”; không suy ra trình độ người đang xem từ thẻ CEFR B2 hay 68%.
- Trang học tập sau khi có nghiệp vụ có thể hiện mục tiêu, tiến độ bài học và bài tiếp theo từ dữ liệu của học viên. Chưa có mục tiêu thì hiển thị “Chưa đặt mục tiêu”, chưa đánh giá thì “Chưa đánh giá”; không mặc định B2.
- Phần trăm hoàn thành lộ trình không phải phần trăm đạt CEFR. Không đánh dấu A1/A2/B1 là đã đạt chỉ vì hoàn thành bài học. CEFR ước lượng phải đến từ kết quả đánh giá hợp lệ theo nghiệp vụ V2.
- Thẻ bài tiếp theo gắn bài học thật trong lộ trình; không bắt buộc gắn đề thi. Chỉ gắn đề khi đó là checkpoint hoặc thi thử, có đề đã công bố và nút chuyển đúng luồng làm bài.
- Trong màn hình đang làm đề, dùng tiến độ câu hỏi và thời gian còn lại; không dùng thanh CEFR làm tiến độ bài thi. Có thể đưa bản tóm tắt lộ trình về trang tổng quan hoặc kết quả.
- Các trang admin giữ nhãn dữ liệu minh họa đến khi nối dữ liệu thật. Danh sách ưu tiên tên, trạng thái, thao tác; mô tả dài xem ở chi tiết. Không bỏ email nhận diện học viên, nội dung câu hỏi, cảnh báo khóa tài khoản hay giới hạn chấm AI.

Rà soát độ dễ đọc: cỡ chữ gốc 18px, bảng và nhãn form 1rem; sidebar desktop 220px để nhãn không bị ép. Ưu tiên tiêu đề và thao tác, rút gọn mô tả phụ, không ẩn nhãn minh họa hoặc trạng thái quan trọng. Icon kỹ năng dùng nét xanh đậm trên nền nhạt. Vòng tiến độ có tâm trắng để số phần trăm không trùng màu với cung tiến độ.

Đồng bộ bổ sung: trang chủ, đăng nhập, đăng ký, trang khám phá và các trang danh sách/chi tiết/biểu mẫu quản trị dùng chung `learning-theme.css`. Font toàn hệ thống là Segoe UI, dự phòng system-ui/Arial, hỗ trợ tiếng Việt và không tải font từ mạng. Không dùng font rounded đậm cho nội dung dài. Bỏ dấu gạch trang trí trước `.eyebrow` trên mọi trang; không tự thêm lại bằng pseudo-element. Admin giữ màu teal riêng, không chuyển sang xanh lá học viên. Biểu mẫu, bộ lọc, bảng và hộp thoại dùng đường viền 2px, bo góc và màu theo theme. Các trang xác thực dùng `auth-layout`, kèm linh vật Navi, tự xếp một cột trên màn hình nhỏ. Không thay đổi route hay nghiệp vụ khi đồng bộ giao diện.

Mốc đồng bộ: ba trang hiện tại `templates/index.html`, `templates/student/dashboard.html` và `templates/admin/dashboard.html`. Thiết kế mới ngày 17/10/2026 theo hướng học tập vui, nút nổi và bản đồ chặng; CSS hoàn thiện nằm trong `learning-theme.css`, được head fragment tải cuối sau forms.css. Các quy định dưới đây áp dụng cho trang chi tiết của cả bốn luồng.

## 1. Layout bắt buộc

- Trang public dùng `navbar` và `footer`.
- Trang Student dùng `navbar`, `student-sidebar` và `footer`.
- Trang Admin dùng `navbar`, `admin-sidebar` và `footer`.
- Bắt buộc đặt theme trên `body`: `landing-theme` cho trang giới thiệu, `student-theme` cho trang học viên, `admin-theme` cho trang quản trị.
- Không copy HTML của navbar, sidebar hoặc footer vào trang riêng.

```html
<div th:replace="~{fragments/navbar :: navbar}"></div>
<div class="container app-shell">
    <div th:replace="~{fragments/student-sidebar :: studentSidebar('learning')}"></div>
    <main class="app-main">
        <!-- Nội dung trang -->
    </main>
</div>
<div th:replace="~{fragments/footer :: footer}"></div>
```

Khi module hoàn thành, đổi mục tương ứng trong sidebar từ `span.disabled` thành thẻ `a` có route thật.

## 2. Component dùng chung

| Mục đích | Class |
|---|---|
| Nút chính | `button` |
| Nút phụ | `button button-secondary` |
| Card | `card` hoặc `dashboard-panel` |
| Trạng thái | `badge`, `badge-success` |
| Thông báo | `alert alert-success`, `alert alert-danger` |
| Form field | `field` |
| Thanh tìm kiếm/lọc | `toolbar`, `toolbar-search` |
| Bảng | `table-wrap`, `data-table` |
| Phân trang | fragment `pagination` |
| Không có dữ liệu | `empty-box` |
| Tiêu đề trang | `page-header`, `page-actions` |

## 3. Trang danh sách

Thứ tự chuẩn:

1. Breadcrumb.
2. Page header và nút tạo mới.
3. Flash message.
4. Search/filter toolbar.
5. Table hoặc empty state.
6. Pagination.

Danh sách phải xử lý cả hai trạng thái: có dữ liệu và chưa có dữ liệu.

## 4. Trang form

- Dùng Form DTO, không bind Entity trực tiếp.
- Mỗi input phải có `label`.
- Hiển thị lỗi bằng `th:errors` và class `field-error`.
- Nút hủy quay về trang danh sách.
- POST thành công phải redirect và hiển thị flash message.
- Không dùng CSS inline.

## 5. Quy tắc màu và CSS

- Chỉ dùng biến màu trong `variables.css`.
- Theme cuối theo vai trò trong `learning-theme.css` ghi đè nền cũ; dùng `var(--color-primary)` thay vì gán màu cố định cho component mới. Khi sửa màu phải kiểm tra giá trị cuối trong file này.
- Component dùng chung đặt trong `components.css` hoặc `layout.css`.
- CSS riêng của một module phải có prefix rõ ràng.
- Không thêm framework CSS khác khi chưa thống nhất cả nhóm.
- Mọi trang phải kiểm tra ở độ rộng 375px và desktop.

## 6. Trạng thái dữ liệu

- Không hiển thị số liệu giả như dữ liệu thật sau đăng nhập.
- Dữ liệu minh họa phải ghi rõ `Dữ liệu minh họa`.
- Chưa có dữ liệu dùng empty state, không để bảng trắng.
- Chức năng chưa có route dùng trạng thái disabled, không dùng link `#`.

## 7. Yêu cầu Pull Request giao diện

- Ghi URL và role dùng để kiểm tra.
- Có ảnh chụp desktop và mobile.
- Có trạng thái empty và trạng thái có dữ liệu.
- Kiểm tra validation lỗi.
- Tái sử dụng fragment và component chung.
- Chạy `./mvnw.cmd clean test` trước khi yêu cầu review.

## 8. Màu sắc theo khu vực

| Khu vực | Theme trên body | Màu chính | Màu đậm | Điểm nhấn |
|---|---|---|---|---|
| Trang chủ | `landing-theme` | Xanh lá `#4c9b18` | `#397713` | Xanh trời `#43b9d7` |
| Học viên | `student-theme` | Xanh lá `#4c9b18` | `#397713` | Xanh trời `#43b9d7` |
| Quản trị | `admin-theme` | Teal `#168e86` | `#106860` | `#45b6ad` |

Trang quản trị dùng nền gần trắng và header teal nhạt với viền nổi. Các trang chi tiết dùng `page-header` gọn; chỉ dùng `dashboard-header` cho khối tiêu đề nổi bật. Tránh gradient tím, bóng mờ lớn và nền tối của thiết kế cũ.

Thiết kế mới lấy cảm hứng từ trải nghiệm học theo chặng: nền trắng, chữ đậm dễ đọc, viền 2px, bóng đặc 3–5px phía dưới tạo nút/thẻ nổi, bo góc lớn và màu điểm nhấn xanh trời/vàng/tím/cam. Giữ nhân vật Navi riêng trong fragment `study-guide`, không dùng logo hoặc nhân vật Duolingo. Icon SVG nội bộ và font hệ thống hoạt động không cần CDN.

Học viên dùng `.learn-board`, `.learn-unit-banner`, `.learn-path`, `.learn-node`, `.learn-rail-card`. Màn hẹp chuyển rail xuống dưới. Không gán điểm thưởng, streak, CEFR hoặc dấu hoàn thành giả trên tài khoản thật. Trạng thái lộ trình chưa nối dữ liệu phải có nhãn minh họa.

Các biến dùng thường xuyên: `--color-primary`, `--color-primary-dark`, `--color-primary-soft`, `--color-muted`, `--color-border`, `--color-danger`, `--color-success`. Trạng thái lỗi/thành công phải có chữ giải thích, không chỉ dựa vào màu.

## 9. Chiều rộng, khoảng cách và chữ

| Thành phần | Quy định hiện tại |
|---|---|
| Trang chủ | Khung tối đa 1320px, trừ tổng 64px ở hai bên |
| Khung học viên/quản trị | `container app-shell`, tối đa 1440px gồm sidebar |
| Sidebar | 190px; cách nội dung 16px |
| Màn hình ≤1080px | Sidebar chuyển thành điều hướng ngang |
| Màn hình ≤680px | Khung nội dung trừ tổng 24px, tương đương lề tối thiểu 12px mỗi bên |
| Card | Bo góc theo `--radius`, nền trắng, viền nhẹ |
| Form | Input theo `field`; chiều cao hiện tại 48px |

Lề desktop tối thiểu 32px mỗi bên; trên màn hình lớn lề tăng do khung có chiều rộng tối đa. Không tự dùng `width: 100vw`, margin âm, hoặc tăng max-width để kéo nội dung ra sát mép. Không lồng thêm `container` trong `app-main` vì sẽ làm nội dung bị thu hẹp lần nữa.

Ưu tiên tiêu đề và từ khóa. Mỗi card chỉ nên có một tiêu đề, một câu mô tả ngắn và hành động cần thiết. Dùng `page-header`, `panel-heading`, `feature-card` để giữ cỡ chữ đồng bộ. Nội dung chính của card khoảng 1rem; không thu nhỏ chữ để nhét nhiều thông tin. Nhãn nhỏ chỉ dành cho thông tin phụ, không dùng cho hướng dẫn quan trọng. Với bảng có nhiều cột, giữ cuộn ngang trong `table-wrap` thay vì giảm toàn bộ font.

## 10. Ngôn ngữ hiển thị đã chốt

Giao diện dùng tiếng Việt, với các tên kỹ năng và học cụ sau giữ tiếng Anh:

| Dùng trên giao diện | Không dùng thay thế |
|---|---|
| Reading | Đọc khi đó là tên kỹ năng |
| Listening | Nghe khi đó là tên kỹ năng |
| Writing | Viết khi đó là tên kỹ năng |
| Speaking | Nói khi đó là tên kỹ năng |
| Grammar & Vocabulary | Ngữ pháp và từ vựng khi đó là tên kỹ năng |
| Flashcard | Thẻ ghi nhớ |

Trong HTML viết `Grammar &amp; Vocabulary`. Câu mô tả tiếng Việt vẫn dùng tự nhiên: “Luyện nghe tối đa hai lượt”, “Ôn từ vựng”. Nội dung bài thi tiếng Anh giữ nguyên. Giữ tên AptisPrep, Aptis, CEFR và các cấp A1–C1.

| Tên kỹ thuật trong code | Nhãn hiển thị |
|---|---|
| Placement Test | Kiểm tra đầu vào |
| Learning Path | Lộ trình học tập |
| Mock Exam | Thi thử |
| Checkpoint | Kiểm tra cuối chặng |
| Lesson | Bài học |
| Question Bank | Ngân hàng câu hỏi |
| Question / Item / Option | Câu hỏi / Ý hỏi / Phương án trả lời |
| Media | Tài nguyên đính kèm |
| Section | Phần thi |
| Publish / Draft / Archive | Công bố / Bản nháp / Lưu trữ |
| Estimated Score | Điểm ước lượng |

Không hiển thị trực tiếp enum, tên thuộc tính hoặc tên bảng bằng `th:text` mà chưa chuyển sang nhãn cho người dùng. Ví dụ `IN_PROGRESS` phải hiển thị “Đang làm bài”.

Không đưa thông báo dành cho lập trình viên lên giao diện: Foundation, migration, database, route, đã seed, sắp phát triển, checklist triển khai, trạng thái module. Chức năng chưa có route giữ disabled và không thêm nhãn tiến độ phát triển. Trạng thái nghiệp vụ như “Chưa đánh giá”, “Chưa có lượt thi”, “Không tìm thấy bài học” được phép hiển thị.

## 11. Khung trang chi tiết để thành viên tái sử dụng

Ví dụ dưới đây là cấu trúc cho trang học viên; các route mới phải được triển khai theo `ROUTES.md` trước khi bật liên kết.

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/head :: head('Chi tiết bài học')}"></head>
<body class="student-theme">
<div th:replace="~{fragments/navbar :: navbar}"></div>
<div class="container app-shell">
    <div th:replace="~{fragments/student-sidebar :: studentSidebar('lessons')}"></div>
    <main class="app-main">
        <div th:replace="~{fragments/breadcrumb :: breadcrumb('Tổng quan', @{/student/dashboard}, 'Chi tiết bài học')}"></div>
        <header class="page-header">
            <div><h1 th:text="${lesson.title}">Tên bài học</h1><p>Reading · B1</p></div>
        </header>
        <div th:replace="~{fragments/messages :: messages}"></div>
        <article class="dashboard-panel">
            <h2>Nội dung bài học</h2>
            <p th:text="${lesson.description}">Mô tả bài học.</p>
        </article>
    </main>
</div>
<div th:replace="~{fragments/footer :: footer}"></div>
</body>
</html>
```

Trang quản trị dùng cấu trúc tương tự, đổi `body` thành `admin-theme`, sidebar thành `adminSidebar(...)` và breadcrumb về `/admin/dashboard`. Model trong ví dụ phải được Controller cung cấp; không tạo trang mới chỉ bằng cách dán mẫu khi chưa có dữ liệu tương ứng.

Sidebar hiện xử lý active cho `dashboard` và mục Admin `students`. Khi bật trang mới, thêm điều kiện active vào mục tương ứng và truyền cùng khóa từ mọi trang thuộc mục đó. Danh sách/chi tiết học viên đều dùng `students`; trang sửa bài học vẫn dùng `lessons`.

Admin đã bật thêm các khóa `lessons`, `questions`, `exams`, `results` cho các trang preview. Có thể tham khảo bộ trang tại `templates/admin/catalog/` và hướng dẫn ADMIN_UI_HANDOFF.md khi tạo trang thật. Không đưa ghi chú triển khai trong tài liệu này lên UI; chỉ giữ nhãn dữ liệu minh họa trên các fixture.

## 12. Mẫu danh sách, form và trạng thái

### Danh sách

Tìm kiếm/lọc dùng form GET; thay đổi dữ liệu dùng form POST. Dùng bảng có tiêu đề cột rõ ràng và nhãn thao tác “Xem”, “Chỉnh sửa”, “Lưu trữ”. Nút tạo mới nằm trong `page-actions`. Giữ từ khóa tìm kiếm và bộ lọc sau khi submit.

```html
<div class="table-wrap">
    <table class="data-table">
        <thead><tr><th scope="col">Bài học</th><th scope="col">Kỹ năng</th><th scope="col">Thao tác</th></tr></thead>
        <tbody>
        <tr th:each="lesson : ${lessons}">
            <td th:text="${lesson.title}"></td>
            <td>Reading</td>
            <td><a th:href="@{/admin/lessons/{id}/edit(id=${lesson.id})}">Chỉnh sửa</a></td>
        </tr>
        </tbody>
    </table>
</div>
```

Thay nhãn Reading mẫu bằng giá trị kỹ năng đã chuyển đổi. Không gán cố định kỹ năng cho dữ liệu thật.

Fragment hiện có: `pagination(page, baseUrl)`, nhận Spring Data Page và URL danh sách qua `@{...}`. Fragment hiện chỉ hỗ trợ tham số `page`; khi danh sách có tìm kiếm/lọc, thành viên phải bổ sung bảo toàn các tham số đó và kích thước trang trước khi sử dụng, tránh mất bộ lọc khi đổi trang.

### Form

```html
<form class="form-card" th:action="@{/admin/lessons}" th:object="${lessonForm}" method="post">
    <div class="field">
        <label for="title">Tên bài học</label>
        <input id="title" th:field="*{title}" required>
        <span class="field-error" th:errors="*{title}"></span>
    </div>
    <div class="page-actions">
        <button class="button" type="submit">Lưu bài học</button>
        <a class="button button-secondary" th:href="@{/admin/lessons}">Hủy</a>
    </div>
</form>
```

Form tải file dùng `enctype="multipart/form-data"`, label rõ định dạng/dung lượng cho phép và kiểm tra file ở backend. Không hiển thị đường dẫn lưu file trên máy chủ cho người dùng.

### Chưa có dữ liệu

```html
<div class="empty-box">
    <h2>Chưa có bài học</h2>
    <p>Tạo bài học đầu tiên để bắt đầu.</p>
</div>
```

Phân biệt “Chưa có dữ liệu” với “Không tải được dữ liệu”. Không dùng số 0 khi chưa lấy được số liệu thật. Dữ liệu mẫu trong landing page cần nhãn minh họa; dữ liệu tài khoản sau đăng nhập phải lấy từ hệ thống hoặc hiển thị trạng thái chưa có dữ liệu.

### Xác nhận thao tác

`modal-backdrop`, `modal-card`, `modal-actions` hiện chỉ là CSS, chưa có fragment hay cơ chế mở/đóng modal dùng chung. Khi cần, triển khai hành vi bàn phím, trả focus, nút Hủy và xác nhận POST; không coi modal là đã hoạt động chỉ vì dùng các class này. Có thể dùng trang xác nhận Thymeleaf để đơn giản hóa. JavaScript hỗ trợ giao diện không được dùng để gọi REST API.

Trang chi tiết học viên đã có mẫu native `dialog` và `admin-students.js`: mở/đóng, Escape và trả focus; Xác nhận đang disabled vì chỉ là preview. Người 1 sẽ nối form POST/CSRF và nghiệp vụ khóa, không cập nhật trạng thái chỉ bằng JavaScript. CSS riêng module có prefix `student-` và được giới hạn trong khu vực quản lý học viên.

## 13. Bàn giao giao diện của mỗi luồng

1. Dùng đúng theme, layout và sidebar của vai trò.
2. Giữ chiều rộng và cỡ chữ theo component chung.
3. Dùng nhãn tiếng Việt cùng các tên kỹ năng/Flashcard đã chốt.
4. Cung cấp trạng thái có dữ liệu, trống, lỗi và validation.
5. Kiểm tra quyền truy cập, quyền sở hữu dữ liệu và form POST.
6. Chụp màn hình ở 375px, 768px và desktop; bảo đảm không tràn ngang toàn trang.
7. Ghi URL, dữ liệu kiểm tra và ảnh vào Pull Request; chạy bộ test phù hợp.

Thay đổi fragment hoặc CSS chung cần mô tả tác động tới cả ba trang và được người giữ foundation review trước khi merge. Không sửa component chung chỉ để chữa bố cục riêng của một trang.
