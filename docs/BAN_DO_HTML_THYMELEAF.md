# Bản đồ HTML và Thymeleaf của AptisPrep

Đối chiếu với mã nguồn trong workspace ngày 09/11/2026. Phạm vi là toàn bộ 45 file HTML trong `src/main/resources/templates`. Có 44 template trang hoặc lỗi có đường render từ Java và 1 file khai báo thành phần được kiểm thử gọi trực tiếp. Các bản sao trong `target`, `backups` và `node_modules` nằm ngoài danh sách.

## Cách tra cứu

1. Tìm URL hoặc tên file ở mục lục. Mỗi mục dẫn đến template, phương thức mở trang và các lớp xử lý chính.
2. Tìm **Biến đọc trong template** để biết biến nào được đọc từ Spring, biến nào phát sinh từ vòng lặp hoặc khối HTML. Các biến chỉ có ở một nhánh như trang sửa hoặc khi chấm lỗi có thể vắng mặt ở nhánh khác.
3. Xem **Form và tương tác** để biết đích gửi dữ liệu, rồi tìm `@PostMapping` hoặc `@GetMapping` trong controller đã dẫn. Tài liệu liệt kê các lớp gắn trực tiếp với chức năng, gồm service, DTO và form tiêu biểu.
4. Phần **Chi tiết template** có các biểu thức thuộc tính, tên input, CSS/JS trực tiếp và danh sách class CSS viết tĩnh. Class tạo qua `th:classappend` hoặc JavaScript được định nghĩa ở file nguồn tương ứng.

## Biến và lớp dùng chung

- `display` và `navSection` đến từ [NavigationModelAdvice](../src/main/java/com/aptisprep/controller/NavigationModelAdvice.java). [DisplayFormatting](../src/main/java/com/aptisprep/controller/DisplayFormatting.java) cung cấp nhãn, định dạng số/ngày và các hàm dùng trong biểu thức.
- `@resultLabels` là bean [ResultLabels](../src/main/java/com/aptisprep/service/student/ResultLabels.java) dùng để dịch trạng thái kết quả trong các template có tham chiếu này.
- `signedIn`, `isStudent` và `isAdmin` do [SessionAuthenticationFilter](../src/main/java/com/aptisprep/security/SessionAuthenticationFilter.java) gắn vào request. [LoginSession](../src/main/java/com/aptisprep/security/LoginSession.java) giữ thông tin phiên đăng nhập.
- `_csrf.token` dùng cho form và AJAX. [CsrfProtection](../src/main/java/com/aptisprep/security/CsrfProtection.java) kiểm tra token, còn [WebMvcConfig](../src/main/java/com/aptisprep/config/WebMvcConfig.java) hỗ trợ chèn field ẩn vào form thay đổi dữ liệu.
- `message`, `successMessage`, `error`, `errorMessage`, `warning` và `info` cùng các biến thông báo tương tự thường đến từ `RedirectAttributes` hoặc model. `param` là tham số request của Thymeleaf.
- `th:object` chọn đối tượng form. `*{...}` đọc field của đối tượng đó. `th:each` tạo biến lặp và biến trạng thái như `partStat`. `th:with` tạo biến cục bộ trong phạm vi thẻ.
- Các file CSS trong nhóm dùng chung gồm [css/variables.css](../src/main/resources/static/css/variables.css), [css/base.css](../src/main/resources/static/css/base.css), [css/layout.css](../src/main/resources/static/css/layout.css), [css/components.css](../src/main/resources/static/css/components.css), [css/forms.css](../src/main/resources/static/css/forms.css), [css/learning-theme.css](../src/main/resources/static/css/learning-theme.css), [css/navi.css](../src/main/resources/static/css/navi.css), [css/exam.css](../src/main/resources/static/css/exam.css), [css/result.css](../src/main/resources/static/css/result.css), [css/catalog-compact.css](../src/main/resources/static/css/catalog-compact.css), [css/admin-overview.css](../src/main/resources/static/css/admin-overview.css), [css/typography.css](../src/main/resources/static/css/typography.css). Mỗi trang ghi rõ file CSS được nạp ở phần chi tiết.

## Các luồng thao tác

Bảng này ghi các thao tác thay đổi dữ liệu và điều hướng chính. Chi tiết từng form, biến và tài nguyên nằm ở phần theo file bên dưới.

| Hoạt động | Route | Handler | Xử lý |
|---|---|---|---|
| Đăng nhập và đăng xuất | `POST /login`, `POST /logout` | [AuthController.login](../src/main/java/com/aptisprep/controller/auth/AuthController.java#L42), [AuthController.logout](../src/main/java/com/aptisprep/controller/auth/AuthController.java#L55) | Xác thực qua UserService và mở hoặc hủy LoginSession. |
| Đăng ký và mật khẩu | `POST /register`, `POST /forgot-password`, `POST /reset-password`, `POST /student/password` | [AuthController.register](../src/main/java/com/aptisprep/controller/auth/AuthController.java#L72), [PasswordController.forgotSubmit](../src/main/java/com/aptisprep/controller/auth/PasswordController.java#L51), [PasswordController.save](../src/main/java/com/aptisprep/controller/auth/PasswordController.java#L79) | UserService tạo tài khoản. PasswordService nhận yêu cầu khôi phục, đặt lại hoặc đổi mật khẩu. |
| Cập nhật hồ sơ và mục tiêu | `POST /student/profile/edit`, `POST /student/goals` | [StudentAccountController.updateProfile](../src/main/java/com/aptisprep/controller/student/StudentAccountController.java#L60), [StudentAccountController.saveGoal](../src/main/java/com/aptisprep/controller/student/StudentAccountController.java#L85) | AccountProfileService xác thực form và lưu thông tin hoặc mục tiêu. |
| Bắt đầu đề đầu vào | `POST /student/placement/start` | [StudentAccountController.start](../src/main/java/com/aptisprep/controller/student/StudentAccountController.java#L126) | PlacementService nhận skill và examId rồi mở hoặc tiếp tục lượt thi. |
| Bắt đầu đề thường và checkpoint | `POST /student/exams/{id}/start`, `POST /student/learning-path/{pathId}/checkpoint/start` | [StudentExamController.start](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L35), [StudentLearningPathController.startCheckpoint](../src/main/java/com/aptisprep/controller/student/StudentLearningPathController.java#L67) | ExamAttemptService khởi tạo lượt thường. Lộ trình kiểm tra điều kiện trước khi mở checkpoint. |
| Lưu đáp án và ghi âm | `POST /student/attempts/{id}/answers/autosave`, `POST /student/attempts/{id}/answers/audio/autosave` | [StudentExamController.autosave](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L127), [StudentExamController.audioAutosave](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L169) | exam-runner.js gửi đáp án hoặc file Speaking. ExamAttemptService lưu vào lượt thi đang làm. |
| Nghe, chuyển phần, thoát và nộp | `POST /student/attempts/{id}/media/{mediaId}/play`, `GET /student/attempts/{id}/media/{mediaId}/content`, `POST /student/attempts/{id}/next-section`, `POST /student/attempts/{id}/exit`, `POST /student/attempts/{id}/submit` | [StudentExamController.playMedia](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L191), [StudentExamController.mediaContent](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L199), [StudentExamController.nextSection](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L266), [StudentExamController.exitAttempt](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L90), [StudentExamController.submit](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L281) | Engine cấp lượt nghe và phục vụ audio theo token, đổi phần và lưu bài nộp. Nút Thoát trả về danh sách đề phù hợp với loại đề. |
| Xem lại, xóa lượt dở, chấm lại | `GET /student/attempts/{id}/review`, `POST /student/attempts/{id}/discard`, `POST /student/attempts/{id}/retry-grading`, `POST /student/attempts/{id}/regrade-empty` | [StudentExamController.review](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L52), [StudentExamController.discard](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L69), [StudentContentController.retryGrading](../src/main/java/com/aptisprep/controller/student/StudentContentController.java#L173), [StudentContentController.regradeEmpty](../src/main/java/com/aptisprep/controller/student/StudentContentController.java#L159) | Trang kết quả và lịch sử hiển thị các thao tác tùy trạng thái lượt thi. |
| Cập nhật lộ trình | `POST /student/learning-path/refresh` | [StudentLearningPathController.refresh](../src/main/java/com/aptisprep/controller/student/StudentLearningPathController.java#L28) | LearningPathService cập nhật các chặng học. |
| Thực hành bài học | `POST /student/lessons/{id}/check`, `POST /student/lessons/{id}/response`, `POST /student/lessons/{id}/response/autosave`, `POST /student/lessons/{id}/recording`, `POST /student/lessons/{id}/assess`, `POST /student/lessons/{id}/complete` | [StudentLessonController.check](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L103), [StudentLessonController.response](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L116), [StudentLessonController.autosave](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L147), [StudentLessonController.recording](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L198), [StudentLessonController.assess](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L131), [StudentLessonController.complete](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L153) | LessonTeachingService kiểm tra, lưu câu trả lời hoặc ghi âm và tạo yêu cầu chấm khi đủ điều kiện. |
| Theo dõi nghe bài học | `POST /student/lessons/{id}/listening/start`, `POST /student/lessons/{id}/listening/heartbeat` | [StudentLessonController.startListening](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L165), [StudentLessonController.listeningHeartbeat](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L172) | lesson-player.js báo tiến độ để máy chủ xác minh điều kiện mở câu hỏi. |
| Tạo, sửa và lưu trữ Flashcard cá nhân | `POST /student/flashcards`, `POST /student/flashcards/{id}`, `POST /student/flashcards/{id}/archive` | [StudentFlashcardController.createPersonalSet](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L123), [StudentFlashcardController.updatePersonalSet](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L132), [StudentFlashcardController.archive](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L142) | FlashcardSetService lưu bộ thẻ thuộc học viên. |
| Ôn Flashcard | `POST /student/flashcards/{setId}/cards/{cardId}/state` | [StudentFlashcardController.mark](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L182) | flashcard-study.js gửi trạng thái ôn theo mã bộ và mã thẻ. |
| Quản trị Flashcard | `POST /admin/flashcards`, `POST /admin/flashcards/{id}` | [AdminFlashcardController.createFlashcardSet](../src/main/java/com/aptisprep/controller/admin/AdminFlashcardController.java#L55), [AdminFlashcardController.updateFlashcardSet](../src/main/java/com/aptisprep/controller/admin/AdminFlashcardController.java#L63) | FlashcardSetService lưu bộ thẻ gắn với bài học. |
| Quản trị câu hỏi và đề thi | `POST /admin/questions`, `POST /admin/questions/{id}/edit`, `POST /admin/exams`, `POST /admin/exams/{id}/edit` | [AdminCatalogController.saveQuestion](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L132), [AdminCatalogController.updateQuestion](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L145), [AdminCatalogController.saveExam](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L225), [AdminCatalogController.updateExam](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L230) | CatalogService và AdminExamService lưu nội dung. Các route publish, archive, restore và media được khai báo cùng controller. |
| Quản trị bài học | `POST /admin/lessons`, `POST /admin/lessons/{id}`, `POST /admin/lessons/{id}/archive`, `POST /admin/lessons/{id}/duplicate`, `POST /admin/lessons/{id}/move` | [AdminLessonController.createLesson](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L57), [AdminLessonController.updateLesson](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L75), [AdminLessonController.archive](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L119), [AdminLessonController.duplicate](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L126), [AdminLessonController.move](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L133) | LessonService lưu nội dung, tạo bản sao hoặc đổi thứ tự. |
| Quản trị chứng chỉ và bộ chấm | `POST /admin/certificates`, `POST /admin/certificates/{id}`, `POST /admin/scoring-schemes`, `POST /admin/scoring-schemes/{id}`, `POST /admin/scoring-schemes/{schemeId}/bands`, `POST /admin/scoring-schemes/{schemeId}/scale-rule` | [AdminScoringController.saveCertificate](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L120), [AdminScoringController.updateCertificate](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L146), [AdminScoringController.saveScheme](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L221), [AdminScoringController.updateScheme](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L254), [AdminScoringController.saveBand](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L299), [AdminScoringController.saveScaleRule](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L374) | ScoringAdminService lưu chứng chỉ, bộ chấm, dải CEFR và quy đổi điểm. |
| Khóa hoặc mở khóa học viên | `POST /admin/students/{id}/lock`, `POST /admin/students/{id}/unlock` | [AdminStudentController.lock](../src/main/java/com/aptisprep/controller/admin/AdminStudentController.java#L48), [AdminStudentController.unlock](../src/main/java/com/aptisprep/controller/admin/AdminStudentController.java#L62) | StudentManagementService cập nhật trạng thái tài khoản. |
| Thử lại tác vụ AI | `POST /student/assessments/{key}/retry`, `POST /admin/assessments/{key}/retry` | [AiAssessmentController.retryStudentAssessment](../src/main/java/com/aptisprep/controller/AiAssessmentController.java#L45), [AiAssessmentController.retryAdminAssessment](../src/main/java/com/aptisprep/controller/AiAssessmentController.java#L51) | AiAssessmentService xử lý lại tác vụ lỗi theo quyền của người yêu cầu. |

## Mục lục trang

- [index.html](#html-1) · Trang chủ AptisPrep · `/`
- [auth/login.html](#html-2) · Đăng nhập · `/login`
- [auth/register.html](#html-3) · Đăng ký học viên · `/register`
- [auth/forgot-password.html](#html-4) · Yêu cầu đặt lại mật khẩu · `/forgot-password`
- [auth/password.html](#html-5) · Đặt lại hoặc đổi mật khẩu · `/reset-password`, `/student/password`
- [error/400.html](#html-6) · Yêu cầu không hợp lệ · `HTTP 400`
- [error/403.html](#html-7) · Truy cập bị từ chối · `HTTP 403`, `/access-denied`
- [error/404.html](#html-8) · Không tìm thấy tài nguyên · `HTTP 404`
- [student/dashboard.html](#html-9) · Tổng quan học viên · `/student/dashboard`
- [student/account/profile.html](#html-10) · Hồ sơ học viên · `/student/profile`
- [student/account/goals.html](#html-11) · Mục tiêu học tập · `/student/goals`
- [student/account/placement.html](#html-12) · Kiểm tra đầu vào · `/student/placement`
- [student/account/placement-result.html](#html-13) · Kết quả đầu vào chuyên biệt · `/student/placement/result?attemptId={id}`
- [student/learning/path.html](#html-14) · Lộ trình của tôi · `/student/learning-path`
- [student/learning/skill.html](#html-15) · Chi tiết lộ trình một kỹ năng · `/student/learning-path/{skill}`
- [student/lesson/list.html](#html-16) · Thư viện bài học · `/student/lessons`
- [student/lesson/detail.html](#html-17) · Học và làm bài thực hành · `/student/lessons/{id}`
- [student/content/list.html](#html-18) · Thư viện đề thi · `/student/exams`
- [student/content/detail.html](#html-19) · Chi tiết đề và thang điểm · `/student/exams/{id}`, `/student/exams/{id}?fromPlacement=true`
- [student/attempt.html](#html-20) · Màn hình làm đề · `/student/attempts/{id}`
- [student/attempt-result.html](#html-21) · Kết quả một lượt thi · `/student/attempts/{id}/result`
- [student/attempt-review.html](#html-22) · Xem lại câu hỏi và bài làm · `/student/attempts/{id}/review`
- [student/results/list.html](#html-23) · Lịch sử kết quả học viên · `/student/results`
- [student/assessment.html](#html-24) · Chi tiết tác vụ chấm AI · `/student/assessments/{key}`, `/admin/assessments/{key}`
- [student/flashcards/list.html](#html-25) · Danh sách Flashcard học viên · `/student/flashcards`
- [student/flashcards/form.html](#html-26) · Tạo hoặc sửa Flashcard cá nhân · `/student/flashcards/create`, `/student/flashcards/{id}/edit`
- [student/flashcards/study.html](#html-27) · Ôn bộ Flashcard · `/student/flashcards/{id}`
- [admin/flashcards/list.html](#html-43) · Danh sách Flashcard quản trị · `/admin/flashcards`
- [admin/flashcards/form.html](#html-44) · Tạo hoặc sửa Flashcard quản trị · `/admin/flashcards/create`, `/admin/flashcards/{id}/edit`
- [admin/dashboard.html](#html-28) · Tổng quan quản trị · `/admin/dashboard`
- [admin/catalog/list.html](#html-29) · Danh sách câu hỏi hoặc đề thi · `/admin/questions`, `/admin/exams`
- [admin/catalog/detail.html](#html-30) · Chi tiết câu hỏi hoặc đề thi quản trị · `/admin/questions/{id}`, `/admin/exams/{id}`
- [admin/catalog/form.html](#html-31) · Tạo hoặc sửa câu hỏi và đề thi · `/admin/questions/create`, `/admin/questions/{id}/edit`, `/admin/exams/create`, `/admin/exams/{id}/edit`
- [admin/lesson/list.html](#html-32) · Danh sách bài học quản trị · `/admin/lessons`
- [admin/lesson/detail.html](#html-33) · Chi tiết bài học quản trị · `/admin/lessons/{id}`
- [admin/lesson/form.html](#html-34) · Tạo hoặc sửa bài học · `/admin/lessons/create`, `/admin/lessons/{id}/edit`
- [admin/result/list.html](#html-35) · Danh sách kết quả học viên · `/admin/results`
- [admin/result/detail.html](#html-36) · Chi tiết kết quả quản trị · `/admin/results/{id}`
- [admin/settings/certificates.html](#html-37) · Danh sách chứng chỉ · `/admin/certificates`
- [admin/settings/certificate.html](#html-38) · Xem, tạo hoặc sửa chứng chỉ · `/admin/certificates/create`, `/admin/certificates/{id}`, `/admin/certificates/{id}/edit`
- [admin/settings/schemes.html](#html-39) · Danh sách bộ chấm điểm · `/admin/scoring-schemes`
- [admin/settings/scheme.html](#html-40) · Xem, tạo hoặc sửa bộ chấm điểm · `/admin/scoring-schemes/create`, `/admin/scoring-schemes/{id}`, `/admin/scoring-schemes/{id}/edit`
- [admin/student/list.html](#html-41) · Danh sách học viên · `/admin/students`
- [admin/student/detail.html](#html-42) · Chi tiết học viên · `/admin/students/{id}`

## Mục lục thành phần HTML

- [fragments/status-badge.html](#html-60) · Nhãn trạng thái

Fragment còn lại được DisplayFormattingTests render trực tiếp để kiểm tra nhãn trạng thái.

## Chi tiết các trang

<a id="html-1"></a>

### Trang chủ AptisPrep · `index.html`

- **Template** [index.html](../src/main/resources/templates/index.html)
- **Đường dẫn** `/`.
- **Phương thức render** [HomeController.home](../src/main/java/com/aptisprep/controller/HomeController.java#L10).
- **Lớp liên quan** [NavigationModelAdvice](../src/main/java/com/aptisprep/controller/NavigationModelAdvice.java), [SessionAuthenticationFilter](../src/main/java/com/aptisprep/security/SessionAuthenticationFilter.java).
- **Chức năng** Giới thiệu nền tảng và điều hướng theo phiên đăng nhập.
- **Biến dùng chung** `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Class CSS ít dùng chung hơn** `active`, `active-question`, `ap-grid`, `button-light`, `card`, `card-icon`, `card-link`, `confetti`, `cta-card`, `cta-login`, `cta-mascot`, `done`, `exam-bar`, `exam-body`, `exam-window`, `experience-card`, `experience-copy`, `experience-grid`, `experience-section`, `final-cta`, `floating-score`, `grid`, `grid-3`, `hero`, `hero-shell`, `hero-visual`, `journey-copy`, `journey-layout`, `journey-panel`, `journey-panel-head`, `journey-points`, `landing-theme`, `level-circle`, `navi-celebrate`, `navi-welcome`, `next-lesson`, `on-dark`, `path-done`, `path-levels`, `path-line`, `play-icon`, `preview-avatar`, `preview-card`, `preview-stat`, `preview-stats`, `preview-top`, `preview-user`, `progress-card`, `progress-fill`, `progress-track`, `section`, `section-heading`, `section-heading-center`, `section-white`, `selected`, `skill-card`, `skill-icon`, `skill-label`, `skill-listening`, `skill-reading`, `skill-speaking`, `skill-writing`, `skills-grid`, `skills-section`, `text-gradient`, `trust-dot`, `trust-item`, `trust-row`.
- **Class CSS thường dùng lại** `actions`, `ap-container`, `ap-skip-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `container`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`, `site-footer`, `site-header`.

</details>

<a id="html-2"></a>

### Đăng nhập · `auth/login.html`

- **Template** [auth/login.html](../src/main/resources/templates/auth/login.html)
- **Đường dẫn** `/login`.
- **Phương thức render** [AuthController.login](../src/main/java/com/aptisprep/controller/auth/AuthController.java#L34).
- **Lớp liên quan** [UserService](../src/main/java/com/aptisprep/service/auth/UserService.java), [PasswordHasher](../src/main/java/com/aptisprep/service/auth/PasswordHasher.java), [LoginSession](../src/main/java/com/aptisprep/security/LoginSession.java).
- **Chức năng** POST /login xác thực email và mật khẩu rồi chuyển đến dashboard theo vai trò.
- **Biến dùng chung** `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `param`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **JavaScript nạp trực tiếp** [js/account-forms.js](../src/main/resources/static/js/account-forms.js), [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/login}`.
- **Tên input, select hoặc button khai báo trực tiếp** `email`, `password`.
- **Class CSS ít dùng chung hơn** `auth-layout`, `auth-page`, `auth-welcome`, `button-block`, `forgot-password-link`, `form-card`, `form-logo`, `form-note`, `landing-theme`, `navi-welcome`, `password-input`, `password-input--inline-toggle`, `password-toggle`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `container`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `messages-container`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`, `site-footer`, `site-header`.

</details>

<a id="html-3"></a>

### Đăng ký học viên · `auth/register.html`

- **Template** [auth/register.html](../src/main/resources/templates/auth/register.html)
- **Đường dẫn** `/register`.
- **Phương thức render** [AuthController.registerForm](../src/main/java/com/aptisprep/controller/auth/AuthController.java#L61).
- **Lớp liên quan** [RegisterForm](../src/main/java/com/aptisprep/dto/form/RegisterForm.java), [AccountInput](../src/main/java/com/aptisprep/validation/AccountInput.java), [UserService](../src/main/java/com/aptisprep/service/auth/UserService.java), [PasswordHasher](../src/main/java/com/aptisprep/service/auth/PasswordHasher.java).
- **Chức năng** POST /register kiểm tra thông tin, tạo tài khoản và chuyển đến đăng nhập.
- **Biến đọc trong template từ controller hoặc luồng redirect** `registerForm`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `message`, `status`.
- **JavaScript nạp trực tiếp** [js/account-forms.js](../src/main/resources/static/js/account-forms.js), [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/register}`.
- **Đối tượng form từ th:object** `${registerForm}`.
- **Field gắn với đối tượng form** `confirmPassword`, `email`, `fullName`, `password`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `status.first`.
- **Tên trùng giữa model và phạm vi cục bộ** `message`.
- **Class CSS ít dùng chung hơn** `account-page`, `auth-layout`, `auth-page`, `auth-welcome`, `button-block`, `field-error`, `form-card`, `form-logo`, `form-note`, `landing-theme`, `navi-welcome`, `password-input`, `password-toggle`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `ap-container`, `ap-skip-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `container`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`, `site-footer`, `site-header`.

</details>

<a id="html-4"></a>

### Yêu cầu đặt lại mật khẩu · `auth/forgot-password.html`

- **Template** [auth/forgot-password.html](../src/main/resources/templates/auth/forgot-password.html)
- **Đường dẫn** `/forgot-password`.
- **Phương thức render** [PasswordController.forgot](../src/main/java/com/aptisprep/controller/auth/PasswordController.java#L43).
- **Lớp liên quan** [PasswordController.RecoveryForm](../src/main/java/com/aptisprep/controller/auth/PasswordController.java#L27), [PasswordService](../src/main/java/com/aptisprep/service/auth/PasswordService.java).
- **Chức năng** POST /forgot-password tiếp nhận email và tạo yêu cầu khôi phục khi tính năng khả dụng.
- **Biến đọc trong template từ controller hoặc luồng redirect** `recoveryAvailable`, `recoveryForm`, `requested`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `message`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/forgot-password}`.
- **Đối tượng form từ th:object** `${recoveryForm}`.
- **Field gắn với đối tượng form** `email`.
- **Tên trùng giữa model và phạm vi cục bộ** `message`.
- **Class CSS ít dùng chung hơn** `auth-page`, `field-error`, `form-card`, `landing-theme`.
- **Class CSS thường dùng lại** `alert`, `alert-info`, `ap-container`, `ap-skip-link`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `container`, `field`, `footer-wrap`, `hamburger-icon`, `nav-links`, `nav-toggle`, `nav-wrap`, `site-footer`, `site-header`.

</details>

<a id="html-5"></a>

### Đặt lại hoặc đổi mật khẩu · `auth/password.html`

- **Template** [auth/password.html](../src/main/resources/templates/auth/password.html)
- **Đường dẫn** `/reset-password`, `/student/password`.
- **Phương thức render** [PasswordController.form](../src/main/java/com/aptisprep/controller/auth/PasswordController.java#L65).
- **Lớp liên quan** [PasswordForm](../src/main/java/com/aptisprep/dto/form/PasswordForm.java), [AccountInput](../src/main/java/com/aptisprep/validation/AccountInput.java), [PasswordService](../src/main/java/com/aptisprep/service/auth/PasswordService.java).
- **Chức năng** POST cùng đường dẫn. Chế độ reset dùng token, chế độ đổi mật khẩu dùng phiên học viên.
- **Biến đọc trong template từ controller hoặc luồng redirect** `changing`, `passwordForm`, `token`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `message`.
- **JavaScript nạp trực tiếp** [js/account-forms.js](../src/main/resources/static/js/account-forms.js), [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${changing} ? @{/student/password} : @{/reset-password}`.
- **Đối tượng form từ th:object** `${passwordForm}`.
- **Field gắn với đối tượng form** `confirmPassword`, `currentPassword`, `password`.
- **Tên input, select hoặc button khai báo trực tiếp** `token`.
- **Tên trùng giữa model và phạm vi cục bộ** `message`.
- **Class CSS ít dùng chung hơn** `account-page`, `auth-page`, `field-error`, `form-card`, `landing-theme`, `password-input`, `password-toggle`.
- **Class CSS thường dùng lại** `ap-container`, `ap-skip-link`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `container`, `field`, `footer-wrap`, `hamburger-icon`, `nav-links`, `nav-toggle`, `nav-wrap`, `site-footer`, `site-header`.

</details>

<a id="html-6"></a>

### Yêu cầu không hợp lệ · `error/400.html`

- **Template** [error/400.html](../src/main/resources/templates/error/400.html)
- **Đường dẫn** `HTTP 400`.
- **Phương thức render** [GlobalExceptionHandler.handleBusinessRule](../src/main/java/com/aptisprep/exception/GlobalExceptionHandler.java#L21).
- **Lớp liên quan** [BusinessRuleException](../src/main/java/com/aptisprep/exception/BusinessRuleException.java).
- **Chức năng** Nhận message từ bộ xử lý lỗi nghiệp vụ.
- **Biến dùng chung** `message`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Class CSS ít dùng chung hơn** `error-page`, `form-card`, `narrow`, `navi-help`, `navi-question`, `page-content`.
- **Class CSS thường dùng lại** `ap-container`, `button`, `container`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`.

</details>

<a id="html-7"></a>

### Truy cập bị từ chối · `error/403.html`

- **Template** [error/403.html](../src/main/resources/templates/error/403.html)
- **Đường dẫn** `HTTP 403`, `/access-denied`.
- **Phương thức render** [GlobalExceptionHandler.handleAccessDenied](../src/main/java/com/aptisprep/exception/GlobalExceptionHandler.java#L28), [HomeController.accessDenied](../src/main/java/com/aptisprep/controller/HomeController.java#L15).
- **Lớp liên quan** [AccessDeniedException](../src/main/java/com/aptisprep/exception/AccessDeniedException.java), [SessionAuthenticationFilter](../src/main/java/com/aptisprep/security/SessionAuthenticationFilter.java).
- **Chức năng** Hiển thị lỗi quyền truy cập và lối quay lại.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Class CSS ít dùng chung hơn** `error-page`, `form-card`, `narrow`, `navi-help`, `navi-question`, `page-content`.
- **Class CSS thường dùng lại** `ap-container`, `button`, `container`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`.

</details>

<a id="html-8"></a>

### Không tìm thấy tài nguyên · `error/404.html`

- **Template** [error/404.html](../src/main/resources/templates/error/404.html)
- **Đường dẫn** `HTTP 404`.
- **Phương thức render** [GlobalExceptionHandler.handleNotFound](../src/main/java/com/aptisprep/exception/GlobalExceptionHandler.java#L14).
- **Lớp liên quan** [ResourceNotFoundException](../src/main/java/com/aptisprep/exception/ResourceNotFoundException.java).
- **Chức năng** Hiển thị lỗi khi không tìm thấy trang hoặc tài nguyên.
- **Biến dùng chung** `message`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Class CSS ít dùng chung hơn** `error-page`, `form-card`, `narrow`, `navi-help`, `navi-question`, `page-content`.
- **Class CSS thường dùng lại** `ap-container`, `button`, `container`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`.

</details>

<a id="html-9"></a>

### Tổng quan học viên · `student/dashboard.html`

- **Template** [student/dashboard.html](../src/main/resources/templates/student/dashboard.html)
- **Đường dẫn** `/student/dashboard`.
- **Phương thức render** [StudentDashboardController.dashboard](../src/main/java/com/aptisprep/controller/student/StudentDashboardController.java#L45).
- **Lớp liên quan** [AccountProfileService](../src/main/java/com/aptisprep/service/account/AccountProfileService.java), [LearningPathService](../src/main/java/com/aptisprep/service/learning/LearningPathService.java), [LearningActivityService](../src/main/java/com/aptisprep/service/account/LearningActivityService.java), [LessonRecommendationService](../src/main/java/com/aptisprep/service/learning/LessonRecommendationService.java), [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java), [StudentDashboardController.NextAction](../src/main/java/com/aptisprep/controller/student/StudentDashboardController.java#L142), [StudentDashboardController.SkillOverview](../src/main/java/com/aptisprep/controller/student/StudentDashboardController.java#L144).
- **Chức năng** Ghép hồ sơ, tiến độ, hoạt động và hành động tiếp theo. /student/orientation là redirect.
- **Biến đọc trong template từ controller hoặc luồng redirect** `account`, `activity`, `email`, `hasLearningContent`, `newLearner`, `nextAction`, `paths`, `skillOverview`.
- **Biến dùng chung** `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `path`, `skill`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `account.profiles`, `account.profiles.isEmpty`, `activity.currentStreak`, `nextAction.label`, `nextAction.title`, `nextAction.url`, `path.completedLessons`, `path.progressPercent`, `path.skill`, `path.totalLessons`, `skill.current`, `skill.goal`, `skill.label`.
- **Tên trùng giữa model và phạm vi cục bộ** `skill`.
- **Class CSS ít dùng chung hơn** `align-items-baseline`, `button-block`, `button-primary`, `d-flex`, `gap-2`, `learn-account`, `learn-board`, `learn-choice`, `learn-dashboard`, `learn-journey`, `learn-node`, `learn-path`, `learn-path-heading`, `learn-rail`, `learn-rail-card`, `learn-rail-heading`, `learn-stop`, `learn-tag`, `learn-unit-banner`, `learn-unit-icon`, `mb-4`, `mt-2`, `mt-3`, `navi-welcome`, `next-lesson-card`, `skill-goal-level`, `skill-overview-card`, `skill-overview-note`, `skill-overview-table`, `streak-card`, `streak-label`, `streak-value`, `supporting-text`.
- **Class CSS thường dùng lại** `actions`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-theme`.

</details>

<a id="html-10"></a>

### Hồ sơ học viên · `student/account/profile.html`

- **Template** [student/account/profile.html](../src/main/resources/templates/student/account/profile.html)
- **Đường dẫn** `/student/profile`.
- **Phương thức render** [StudentAccountController.profile](../src/main/java/com/aptisprep/controller/student/StudentAccountController.java#L42).
- **Lớp liên quan** [AccountProfileService](../src/main/java/com/aptisprep/service/account/AccountProfileService.java), [AccountSnapshot](../src/main/java/com/aptisprep/dto/view/AccountSnapshot.java), [ProfileForm](../src/main/java/com/aptisprep/dto/form/ProfileForm.java), [LearningActivityService](../src/main/java/com/aptisprep/service/account/LearningActivityService.java).
- **Chức năng** POST /student/profile/edit cập nhật hồ sơ. Trang có lịch hoạt động và lối sang mục tiêu hoặc đổi mật khẩu.
- **Biến đọc trong template từ controller hoặc luồng redirect** `account`, `activity`, `profileForm`.
- **Biến dùng chung** `display`, `errorMessage`, `isAdmin`, `isStudent`, `navSection`, `signedIn`, `successMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `code`, `day`, `daysRemaining`, `p`, `targetLvl`, `week`.
- **JavaScript nạp trực tiếp** [js/account-forms.js](../src/main/resources/static/js/account-forms.js), [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js), [js/profile-activity.js](../src/main/resources/static/js/profile-activity.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/profile/edit}`.
- **Đối tượng form từ th:object** `${profileForm}`.
- **Field gắn với đối tượng form** `fullName`.
- **Tên input, select hoặc button khai báo trực tiếp** `email`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `account.email`, `account.goal`, `account.goal.certificateName`, `account.goal.targetDate`, `account.goal.targets`, `account.goal.targets.get`, `account.profiles`, `account.profiles.isEmpty`, `activity.activeDays`, `activity.currentStreak`, `activity.longestStreak`, `activity.totalActivities`, `activity.weeks`, `day.future`, `day.label`, `day.level`, `p.currentLevel`, `p.currentLevel.toLowerCase`, `p.skill`, `targetLvl.toLowerCase`, `week.days`.
- **Class CSS ít dùng chung hơn** `activity-chart`, `activity-day`, `activity-heading`, `activity-legend`, `activity-level-0`, `activity-level-1`, `activity-level-2`, `activity-level-3`, `activity-level-4`, `activity-metrics`, `activity-scroll`, `activity-streak`, `activity-week`, `activity-weekdays`, `activity-weeks`, `cefr-chip`, `cefr-none`, `cefr-target-compare`, `compare-cols-right`, `compare-item`, `compare-list`, `compare-skill-name`, `compare-table-head`, `current-level-slot`, `field-error`, `field-hint-dirty`, `field-label-row`, `goal-cert-info`, `goal-eyebrow`, `goal-summary-banner`, `goal-target-date`, `learning-activity`, `panel-bottom-note`, `panel-heading-between`, `panel-heading-row`, `profile-comparison-actions`, `profile-form-body`, `profile-form-footer`, `profile-panel`, `readonly-badge`, `readonly-input-wrap`, `required-star`, `skill-compare-table`.
- **Class CSS thường dùng lại** `actions`, `alert`, `alert-danger`, `alert-success`, `ap-alert`, `ap-alert-danger`, `ap-alert-success`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-11"></a>

### Mục tiêu học tập · `student/account/goals.html`

- **Template** [student/account/goals.html](../src/main/resources/templates/student/account/goals.html)
- **Đường dẫn** `/student/goals`.
- **Phương thức render** [StudentAccountController.goals](../src/main/java/com/aptisprep/controller/student/StudentAccountController.java#L76).
- **Lớp liên quan** [GoalForm](../src/main/java/com/aptisprep/dto/form/GoalForm.java), [AccountProfileService](../src/main/java/com/aptisprep/service/account/AccountProfileService.java), [CertificateSchemeService](../src/main/java/com/aptisprep/service/scoring/CertificateSchemeService.java).
- **Chức năng** POST /student/goals lưu mục tiêu theo kỹ năng và chứng chỉ.
- **Biến đọc trong template từ controller hoặc luồng redirect** `certificates`, `goalForm`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `certificate`, `level`, `message`, `status`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/goals}`.
- **Đối tượng form từ th:object** `${goalForm}`.
- **Field gắn với đối tượng form** `certificateId`, `grammarVocabulary`, `listening`, `reading`, `speaking`, `targetDate`, `writing`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `certificate.code`, `certificate.id`, `certificate.name`, `status.first`.
- **Tên trùng giữa model và phạm vi cục bộ** `certificate`, `message`.
- **Class CSS ít dùng chung hơn** `catalog-form-grid`, `field-error`.
- **Class CSS thường dùng lại** `actions`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `messages-container`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-12"></a>

### Kiểm tra đầu vào · `student/account/placement.html`

- **Template** [student/account/placement.html](../src/main/resources/templates/student/account/placement.html)
- **Đường dẫn** `/student/placement`.
- **Phương thức render** [StudentAccountController.placement](../src/main/java/com/aptisprep/controller/student/StudentAccountController.java#L109).
- **Lớp liên quan** [PlacementService](../src/main/java/com/aptisprep/service/placement/PlacementService.java), [PlacementService.PlacementOption](../src/main/java/com/aptisprep/service/placement/PlacementService.java#L96), [PlacementExamCatalogService](../src/main/java/com/aptisprep/service/placement/PlacementExamCatalogService.java), [PlacementExamCatalogService.Section](../src/main/java/com/aptisprep/service/placement/PlacementExamCatalogService.java#L82), [ExamStartValidator](../src/main/java/com/aptisprep/service/exam/ExamStartValidator.java), [ListPagination](../src/main/java/com/aptisprep/controller/ListPagination.java), [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java).
- **Chức năng** Hiển thị đề tổng hợp mặc định, cấu trúc từng kỹ năng và nút Thang điểm. Bộ lọc skill vẫn có thể truyền qua URL. Trang chỉ hiện phân trang khi có nhiều trang.
- **Biến đọc trong template từ controller hoặc luồng redirect** `placementExams`, `placementPage`, `skill`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `baseUrl`, `currentPage`, `end`, `exam`, `index`, `pageParam`, `pageUrl`, `part`, `partStat`, `start`, `totalPages`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/placement/start}`.
- **Tên input, select hoặc button khai báo trực tiếp** `examId`, `skill`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `exam.displayTitle`, `exam.durationMinutes`, `exam.id`, `exam.resumable`, `exam.sections`, `part.durationMinutes`, `part.skill`, `partStat.count`, `placementExams.isEmpty`, `placementPage.number`, `placementPage.totalPages`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `totalPages`.
- **Class CSS ít dùng chung hơn** `active`, `disabled`, `page-link`, `page-title-with-icon`, `pagination`, `pagination-gap`, `pagination-pages`, `placement-exam-actions`, `placement-page`, `student-pagination`, `study-exam-section`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `empty-box`, `footer-wrap`, `hamburger-icon`, `messages-container`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-13"></a>

### Kết quả đầu vào chuyên biệt · `student/account/placement-result.html`

- **Template** [student/account/placement-result.html](../src/main/resources/templates/student/account/placement-result.html)
- **Đường dẫn** `/student/placement/result?attemptId={id}`.
- **Phương thức render** [StudentAccountController.result](../src/main/java/com/aptisprep/controller/student/StudentAccountController.java#L143).
- **Lớp liên quan** [PlacementService](../src/main/java/com/aptisprep/service/placement/PlacementService.java), [ScoringGuideService](../src/main/java/com/aptisprep/service/scoring/ScoringGuideService.java), [IntegrationModels.AttemptResult](../src/main/java/com/aptisprep/service/contract/IntegrationModels.java#L171).
- **Chức năng** Đọc kết quả đầu vào theo mã lượt thi. Lượt nộp qua engine cũng có trang kết quả chung.
- **Biến đọc trong template từ controller hoặc luồng redirect** `feedbackWithoutGuide`, `placementResult`, `scoreGuides`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `resultLabels`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `band`, `guide`, `isPlacement`, `res`, `score`, `skill`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/attempts/{id}/retry-grading(id=${res.attemptId})}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `band.attained`, `band.level`, `band.max`, `band.min`, `guide.bands`, `guide.bands.isEmpty`, `guide.max.stripTrailingZeros`, `guide.min.stripTrailingZeros`, `guide.skill`, `res.attemptId`, `res.examTitle`, `res.examType`, `res.hasFullMockTotal`, `res.isCombined`, `res.overallCefr`, `res.overallScaleScore`, `res.skills`, `res.skills.isEmpty`, `res.state`, `res.submittedAt`, `score.stripTrailingZeros`, `scoreGuides.isEmpty`, `skill.estimatedLevel`, `skill.feedback`, `skill.feedback.isBlank`, `skill.gradingState`, `skill.rawScore`, `skill.scaleScore`, `skill.skill`.
- **Tên trùng giữa model và phạm vi cục bộ** `skill`.
- **Class CSS ít dùng chung hơn** `data-table`, `result-actions-bar`, `result-container`, `result-detail-header`, `result-guide-card`, `result-guide-grid`, `result-guides`, `result-message`, `result-message-error`, `result-skill-feedback`, `result-skill-table`, `result-status`, `result-totals`, `table`, `table-responsive`, `table-wrap`, `visually-hidden`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-14"></a>

### Lộ trình của tôi · `student/learning/path.html`

- **Template** [student/learning/path.html](../src/main/resources/templates/student/learning/path.html)
- **Đường dẫn** `/student/learning-path`.
- **Phương thức render** [StudentLearningPathController.paths](../src/main/java/com/aptisprep/controller/student/StudentLearningPathController.java#L39).
- **Lớp liên quan** [LearningPathService](../src/main/java/com/aptisprep/service/learning/LearningPathService.java), [LearningPathService.Stage](../src/main/java/com/aptisprep/service/learning/LearningPathService.java#L106).
- **Chức năng** Hiển thị các kỹ năng và chặng học. POST /student/learning-path/refresh cập nhật lộ trình.
- **Biến đọc trong template từ controller hoặc luồng redirect** `hasAssessment`, `stages`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `current`, `goal`, `level`, `levelStat`, `progress`, `scale`, `skill`, `stage`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `levelStat.index`, `progress.label`, `progress.percent`, `scale.indexOf`, `stage.currentLevel`, `stage.goalLevel`, `stage.progress`, `stage.skill`, `stage.status`, `stage.targetLevel`.
- **Tên trùng giữa model và phạm vi cục bộ** `progress`, `skill`.
- **Class CSS ít dùng chung hơn** `badge-dot`, `button-primary`, `entry-banner-badge`, `entry-banner-cta`, `entry-start-btn`, `entry-test-banner`, `explore-card`, `explore-card-action`, `explore-card-top`, `explore-grid`, `explore-listening`, `explore-reading`, `explore-speaking`, `explore-vocabulary`, `explore-writing`, `orientation-page`, `path-overview`, `path-summary`, `sr-only`, `stage-goal-label`, `stage-progress-fill`, `stage-progress-info`, `stage-progress-track`, `stage-progress-wrap`, `stage-scale`, `tile-label-suffix`, `unassessed-action-wrap`, `unassessed-hint`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-theme`.

</details>

<a id="html-15"></a>

### Chi tiết lộ trình một kỹ năng · `student/learning/skill.html`

- **Template** [student/learning/skill.html](../src/main/resources/templates/student/learning/skill.html)
- **Đường dẫn** `/student/learning-path/{skill}`.
- **Phương thức render** [StudentLearningPathController.skill](../src/main/java/com/aptisprep/controller/student/StudentLearningPathController.java#L49).
- **Lớp liên quan** [LearningPathService](../src/main/java/com/aptisprep/service/learning/LearningPathService.java), [LearningPathService.Stage](../src/main/java/com/aptisprep/service/learning/LearningPathService.java#L106), [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java), [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java).
- **Chức năng** Hiển thị bài học, bộ thẻ và checkpoint. POST /student/learning-path/{pathId}/checkpoint/start mở lượt thi cuối chặng.
- **Biến đọc trong template từ controller hoặc luồng redirect** `lessonSets`, `paths`, `selectedSkill`, `stages`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `current`, `goal`, `lesson`, `level`, `levelStat`, `path`, `progress`, `scale`, `skill`, `stage`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/learning-path/{id}/checkpoint/start(id=${path.pathId()})}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `lesson.available`, `lesson.completed`, `lesson.id`, `lesson.title`, `lesson.topic`, `lessonSets.containsKey`, `lessonSets.get`, `levelStat.index`, `path.checkpointState`, `path.completedLessons`, `path.completedRequiredLessons`, `path.pathId`, `path.progressPercent`, `path.skill`, `path.totalLessons`, `path.totalRequiredLessons`, `paths.isEmpty`, `progress.label`, `progress.percent`, `scale.indexOf`, `stage.currentLevel`, `stage.goalLevel`, `stage.lessons`, `stage.missingRequiredLessons`, `stage.pathId`, `stage.progress`, `stage.skill`, `stage.status`, `stage.targetLevel`.
- **Tên trùng giữa model và phạm vi cục bộ** `lesson`, `progress`, `skill`.
- **Class CSS ít dùng chung hơn** `badge-success`, `checkpoint-missing`, `checkpoint-missing-list`, `checkpoint-requirements`, `curriculum-tree`, `final-test-section`, `lesson-leaves`, `path-lesson-actions`, `path-lesson-content`, `path-lesson-title`, `path-overview`, `path-set-link`, `path-summary`, `skill-branch`, `sr-only`, `stage-goal-action`, `stage-goal-label`, `stage-overview`, `stage-progress-fill`, `stage-progress-info`, `stage-progress-track`, `stage-progress-wrap`, `stage-scale`, `tile-label-suffix`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `empty-box`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-16"></a>

### Thư viện bài học · `student/lesson/list.html`

- **Template** [student/lesson/list.html](../src/main/resources/templates/student/lesson/list.html)
- **Đường dẫn** `/student/lessons`.
- **Phương thức render** [StudentLessonController.list](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L38).
- **Lớp liên quan** [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java), [Lesson](../src/main/java/com/aptisprep/entity/Lesson.java), [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java).
- **Chức năng** Tìm và lọc bài học theo kỹ năng hoặc cấp độ, rồi mở trang chi tiết.
- **Biến đọc trong template từ controller hoặc luồng redirect** `lessons`, `lessonSets`, `lessonStatuses`, `lessonTree`, `levels`, `q`, `selectedLevel`, `selectedSkill`, `skills`.
- **Biến dùng chung** `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `branch`, `group`, `learningStatus`, `lesson`, `lessonStat`, `tier`, `v`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{/student/lessons}`.
- **Tên input, select hoặc button khai báo trực tiếp** `level`, `q`, `skill`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `branch.count`, `branch.levels`, `branch.skill`, `group.lessons`, `group.lessons.size`, `group.name`, `lesson.id`, `lesson.title`, `lessons.isEmpty`, `lessons.size`, `lessonSets.containsKey`, `lessonSets.get`, `lessonStat.count`, `lessonStatuses.getOrDefault`, `tier.count`, `tier.level`, `tier.topics`.
- **Tên trùng giữa model và phạm vi cục bộ** `lesson`.
- **Class CSS ít dùng chung hơn** `badge-success`, `lesson-library-page`, `lesson-library-tree`, `library-count`, `library-flashcard-link`, `library-learning-status`, `library-lesson-actions`, `library-lesson-body`, `library-lesson-heading`, `library-lesson-number`, `library-lesson-title`, `library-lessons`, `library-level`, `library-level-badge`, `library-levels`, `library-open`, `library-skill`, `library-skill-name`, `library-topic`, `library-topics`, `toolbar`, `toolbar-search`.
- **Class CSS thường dùng lại** `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `empty-box`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-17"></a>

### Học và làm bài thực hành · `student/lesson/detail.html`

- **Template** [student/lesson/detail.html](../src/main/resources/templates/student/lesson/detail.html)
- **Đường dẫn** `/student/lessons/{id}`.
- **Phương thức render** [StudentLessonController.detail](../src/main/java/com/aptisprep/controller/student/StudentLessonController.java#L77).
- **Lớp liên quan** [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java), [LessonTeachingService](../src/main/java/com/aptisprep/service/learning/LessonTeachingService.java), [LessonListeningService](../src/main/java/com/aptisprep/service/learning/LessonListeningService.java), [LessonCheckService](../src/main/java/com/aptisprep/service/learning/LessonCheckService.java), [LessonAudioController](../src/main/java/com/aptisprep/controller/LessonAudioController.java), [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java), [Lesson](../src/main/java/com/aptisprep/entity/Lesson.java), [AiAssessmentContract](../src/main/java/com/aptisprep/service/contract/AiAssessmentContract.java).
- **Chức năng** Nộp câu trả lời, lưu tự động, ghi âm, theo dõi bài nghe, xem lịch sử, yêu cầu chấm AI và đánh dấu hoàn thành.
- **Biến đọc trong template từ controller hoặc luồng redirect** `aiAvailable`, `aiSubmissions`, `attempts`, `canAssess`, `completion`, `feedback`, `flashcardSet`, `lesson`, `neighbors`, `progress`, `questions`, `release`, `savedResponse`, `savedWork`, `submissions`, `videoId`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `answer`, `attempt`, `context`, `hasAttempts`, `hasMethod`, `hasMethodReview`, `hasQuestions`, `hasVideo`, `heading`, `mediaPrefix`, `opt`, `option`, `productive`, `question`, `stat`, `status`, `submission`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/lesson-player.js](../src/main/resources/static/js/lesson-player.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên, [css/lesson-detail.css](../src/main/resources/static/css/lesson-detail.css).
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/lessons/{id}/check(id=${lesson.id})}`, `POST @{/student/lessons/{id}/response(id=${lesson.id})}`, `POST @{/student/lessons/{id}/assess(id=${lesson.id})}`, `POST @{/student/lessons/{id}/complete(id=${lesson.id})}`.
- **Tên input, select hoặc button khai báo trực tiếp** `${'q' + (question.id())}`, `response`, `submissionId`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `aiSubmissions.isEmpty`, `answer.correct`, `answer.explanation`, `answer.options`, `answer.prompt`, `answer.right`, `answer.selected`, `attempt.answers`, `attempt.answers.isEmpty`, `attempt.context`, `attempt.id`, `attempt.score`, `attempt.submittedAt`, `attempt.total`, `attempts.isEmpty`, `attempts.size`, `completion.eligible`, `completion.message`, `context.analysis`, `context.audioPath`, `context.description`, `context.exampleText`, `context.imagePath`, `context.maxWords`, `context.minWords`, `context.objectives`, `context.practicePrompt`, `context.strategy`, `context.title`, `context.videoPath`, `context.videoUrl`, `context.videoUrl.isBlank`, `feedback.answers`, `feedback.score`, `feedback.total`, `flashcardSet.id`, `lesson.audioName`, `lesson.audioPath`, `lesson.cefrLevel`, `lesson.description`, `lesson.id`, `lesson.imagePath`, `lesson.material`, `lesson.material.analysis`, `lesson.material.analysis.isBlank`, `lesson.material.checklist`, `lesson.material.checklist.isBlank`, `lesson.material.commonMistakes`, `lesson.material.commonMistakes.isBlank`, `lesson.material.exampleText`, `lesson.material.exampleText.isBlank`, `lesson.material.objectives`, `lesson.material.objectives.isBlank`, `lesson.material.practicePrompt`, `lesson.material.sampleResponse`, `lesson.material.sampleResponse.isBlank`, `lesson.material.sourceName`, `lesson.material.sourceUrl`, `lesson.material.sourceUrl.isBlank`, `lesson.material.strategy`, `lesson.material.strategy.isBlank`, `lesson.material.taskType`, `lesson.material.topic`, `lesson.maxWordCount`, `lesson.minWordCount`, `lesson.skill`, `lesson.title`, `lesson.transcript`, `lesson.transcript.isBlank`, `lesson.videoPath`, `neighbors.next`, `neighbors.previous`, `opt.index`, `progress.status`, `question.id`, `question.options`, `question.prompt`, `questions.isEmpty`, `questions.size`, `release.sample`, `release.transcript`, `release.transcriptHint`, `savedWork.audioPath`, `stat.count`, `status.first`, `submission.audioPath`, `submission.context`, `submission.id`, `submission.prompt`, `submission.skill`, `submission.submittedAt`, `submission.text`, `submissions.isEmpty`, `submissions.size`.
- **Tên trùng giữa model và phạm vi cục bộ** `attempt`.
- **Class CSS ít dùng chung hơn** `ai-grading-panel`, `ai-grading-target`, `ai-grading-target-head`, `ai-submission-choice`, `ai-submission-choice-body`, `ai-submission-excerpt`, `completion-hint`, `lesson-attempts`, `lesson-classroom`, `lesson-complete`, `lesson-disclosure`, `lesson-feedback`, `lesson-finish`, `lesson-header`, `lesson-hero-image`, `lesson-history-context`, `lesson-intro`, `lesson-knowledge-check`, `lesson-locked-content`, `lesson-model`, `lesson-prose`, `lesson-question`, `lesson-source`, `lesson-step-nav`, `lesson-video`, `lesson-vocabulary-link`, `lesson-work-history`, `listening-player`, `listening-preparation`, `method-review`, `path-set-link`, `skill-practice`, `speaking-recorder`.
- **Class CSS thường dùng lại** `actions`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-theme`.

</details>

<a id="html-18"></a>

### Thư viện đề thi · `student/content/list.html`

- **Template** [student/content/list.html](../src/main/resources/templates/student/content/list.html)
- **Đường dẫn** `/student/exams`.
- **Phương thức render** [StudentContentController.list](../src/main/java/com/aptisprep/controller/student/StudentContentController.java#L46).
- **Lớp liên quan** [StudentExamCatalogService](../src/main/java/com/aptisprep/service/student/StudentExamCatalogService.java), [StudentCatalogView](../src/main/java/com/aptisprep/dto/view/StudentCatalogView.java), [AccountAccessService](../src/main/java/com/aptisprep/service/account/AccountAccessService.java).
- **Chức năng** Tìm, lọc, phân trang đề và hiển thị nhóm theo loại đề.
- **Biến đọc trong template từ controller hoặc luồng redirect** `catalogPage`, `categoryTitle`, `entries`, `examCategories`, `examGroups`, `selectedExamType`, `selectedLevel`, `selectedSkill`, `selectedSort`, `selectedState`, `size`, `skills`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `baseUrl`, `block`, `category`, `currentPage`, `end`, `entry`, `fact`, `group`, `index`, `level`, `pageParam`, `pageUrl`, `q`, `skill`, `start`, `totalItems`, `totalPages`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên, [css/exam-library.css](../src/main/resources/static/css/exam-library.css).
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{/student/exams}`, `POST @{/student/exams/{id}/start(id=${entry.id})}`.
- **Tên input, select hoặc button khai báo trực tiếp** `examType`, `level`, `q`, `size`, `skill`, `sort`, `state`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `block.title`, `catalogPage.number`, `catalogPage.size`, `catalogPage.totalElements`, `catalogPage.totalPages`, `category.code`, `category.count`, `category.label`, `entries.isEmpty`, `entry.blocks`, `entry.examType`, `entry.facts`, `entry.id`, `entry.kind`, `entry.level`, `entry.level.isBlank`, `entry.level.toLowerCase`, `entry.readinessMessage`, `entry.skill`, `entry.startable`, `entry.summary`, `entry.summary.isBlank`, `entry.title`, `fact.label`, `fact.value`, `group.key`, `group.value`, `group.value.size`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `entry`, `q`, `skill`, `totalPages`.
- **Class CSS ít dùng chung hơn** `active`, `badge-secondary`, `button-primary`, `compact-catalog`, `disabled`, `exam-card-actions`, `exam-card-facts`, `exam-card-readiness`, `exam-card-summary`, `exam-category-context`, `exam-category-nav`, `exam-group-heading`, `exam-library`, `exam-library-heading`, `exam-section-skills`, `exam-skill-group`, `page-link`, `pagination`, `pagination-gap`, `pagination-pages`, `student-pagination`, `study-card-grid`, `study-card-meta`, `study-content-card`, `study-filters`, `toolbar`, `toolbar-search`.
- **Class CSS thường dùng lại** `actions`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `empty-box`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-19"></a>

### Chi tiết đề và thang điểm · `student/content/detail.html`

- **Template** [student/content/detail.html](../src/main/resources/templates/student/content/detail.html)
- **Đường dẫn** `/student/exams/{id}`, `/student/exams/{id}?fromPlacement=true`.
- **Phương thức render** [StudentContentController.detail](../src/main/java/com/aptisprep/controller/student/StudentContentController.java#L128).
- **Lớp liên quan** [StudentExamCatalogService](../src/main/java/com/aptisprep/service/student/StudentExamCatalogService.java), [StudentCatalogView.Entry](../src/main/java/com/aptisprep/dto/view/StudentCatalogView.java#L13), [ScoringGuideService](../src/main/java/com/aptisprep/service/scoring/ScoringGuideService.java), [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java), [StudentExamController](../src/main/java/com/aptisprep/controller/student/StudentExamController.java).
- **Chức năng** Hiển thị phần thi và thang điểm. Đề thường bắt đầu qua POST /student/exams/{id}/start. Placement bắt đầu từ trang Kiểm tra đầu vào. Khối hướng dẫn trước khi làm bài được ẩn cho đề đầu vào.
- **Biến đọc trong template từ controller hoặc luồng redirect** `entry`, `fromPlacement`, `module`, `resumableAttemptId`, `scoreGuides`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `backUrl`, `band`, `block`, `fact`, `guide`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/exams/{id}/start(id=${entry.id})}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `band.attained`, `band.level`, `band.max`, `band.min`, `block.content`, `block.title`, `block.value`, `entry.blocks`, `entry.examType`, `entry.facts`, `entry.id`, `entry.kind`, `entry.level`, `entry.skill`, `entry.skillCode`, `entry.startable`, `entry.state`, `entry.status`, `entry.summary`, `entry.title`, `fact.label`, `fact.value`, `guide.bands`, `guide.bands.isEmpty`, `guide.max.stripTrailingZeros`, `guide.min.stripTrailingZeros`, `guide.skill`, `scoreGuides.isEmpty`.
- **Class CSS ít dùng chung hơn** `button-primary`, `compact-catalog`, `learn-soft-label`, `lesson-practice`, `navi-help`, `navi-question`, `orientation-flashcard`, `practice-speaking`, `practice-writing`, `recorder-symbol`, `scoring-guide`, `speaking-cues`, `speaking-recorder`, `study-card-meta`, `study-detail-header`, `study-exam-section`, `study-facts`, `study-flashcards`, `study-instructions`, `study-lesson-blocks`, `study-overview-card`, `study-result-block`, `study-summary`, `table`, `table-responsive`, `visually-hidden`.
- **Class CSS thường dùng lại** `actions`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `empty-box`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-20"></a>

### Màn hình làm đề · `student/attempt.html`

- **Template** [student/attempt.html](../src/main/resources/templates/student/attempt.html)
- **Đường dẫn** `/student/attempts/{id}`.
- **Phương thức render** [StudentExamController.attempt](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L82).
- **Lớp liên quan** [ExamAttemptService](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java), [ExamAttemptService.AttemptPage](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java#L772), [ExamSnapshotService](../src/main/java/com/aptisprep/service/exam/ExamSnapshotService.java), [AccountAccessService](../src/main/java/com/aptisprep/service/account/AccountAccessService.java), [AudioUploadService](../src/main/java/com/aptisprep/service/storage/AudioUploadService.java).
- **Chức năng** Lưu đáp án và ghi âm, cấp quyền phát audio, chuyển phần có giới hạn giờ và nộp bài. Form Thoát gửi POST đến /student/attempts/{id}/exit để controller chọn trang quay về.
- **Biến đọc trong template từ controller hoặc luồng redirect** `attempt`.
- **Biến dùng chung** `_csrf`, `display`, `error`, `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `item`, `media`, `option`, `question`, `questionStat`, `section`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/exam-runner.js](../src/main/resources/static/js/exam-runner.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **CSS trong template** Có khối `<style>` khai báo trực tiếp.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/attempts/{id}/next-section(id=${attempt.attemptId})}`, `POST @{/student/attempts/{id}/submit(id=${attempt.attemptId})}`, `POST @{/student/attempts/{id}/exit(id=${attempt.attemptId})}`, `POST @{/student/attempts/{id}/answers(id=${attempt.attemptId})}`.
- **URL chuyển cho JS qua data attribute** `data-audio-action → /student/attempts/{id}/answers/audio/autosave(id=${attempt.attemptId})`, `data-autosave → /student/attempts/{id}/answers/autosave(id=${attempt.attemptId})`, `data-content → /student/attempts/{id}/media/{mediaId}/content(id=${attempt.attemptId},mediaId=${media.id})`, `data-play → /student/attempts/{id}/media/{mediaId}/play(id=${attempt.attemptId},mediaId=${media.id})`.
- **Tên input, select hoặc button khai báo trực tiếp** `itemId`, `questionId`, `sectionEndMinute`, `selectedOptionId`, `textAnswer`, `userPosition`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `attempt.answeredItems`, `attempt.attemptId`, `attempt.deadline`, `attempt.examType`, `attempt.nextSection`, `attempt.nextSectionAfterMinute`, `attempt.questions`, `attempt.remainingSeconds`, `attempt.sections`, `attempt.sections.size`, `attempt.sectionSkill`, `attempt.title`, `attempt.totalItems`, `item.answered`, `item.audioSaved`, `item.content`, `item.itemId`, `item.options`, `item.options.isEmpty`, `item.position`, `item.responseSeconds`, `item.selectedOptionId`, `item.textAnswer`, `item.userPosition`, `media.filename`, `media.id`, `media.type`, `option.code`, `option.content`, `option.id`, `question.content`, `question.instruction`, `question.instruction.isBlank`, `question.items`, `question.items.size`, `question.media`, `question.questionId`, `question.readingPassage`, `question.readingPassage.isBlank`, `question.skill`, `question.type`, `questionStat.count`, `questionStat.index`, `section.durationMinutes`, `section.index`, `section.open`, `section.questionCount`, `section.skill`, `section.state`.
- **Class CSS ít dùng chung hơn** `alert-error`, `answer-form`, `attempt-media`, `autosave-status`, `catalog-checkbox`, `exam-control-actions`, `exam-control-heading`, `exam-control-panel`, `exam-exit-button`, `exam-page`, `exam-progress-label`, `exam-question-grid`, `exam-question-link`, `exam-question-navigation`, `exam-section-link`, `exam-section-list`, `exam-section-navigation`, `exam-stats`, `exam-timer-badge`, `exam-workspace`, `full-mock-pagination`, `full-mock-pagination-info`, `full-mock-question`, `is-empty`, `media-audio`, `media-audio-shell`, `media-play-trigger`, `media-player`, `media-status`, `question-instruction`, `question-item`, `question-item-header`, `question-item-number`, `question-item-prompt`, `reading-passage`, `reading-passage-text`, `recording-meter`, `recording-meter-heading`, `recording-preview`, `recording-progress`, `recording-reset`, `recording-save`, `recording-saved`, `recording-start`, `recording-status`, `recording-stop`, `recording-time`, `speaking-recorder`, `speaking-recorder-actions`, `speaking-recorder-label`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `ap-alert`, `ap-alert-danger`, `ap-container`, `ap-skip-link`, `app-main`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `container`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-21"></a>

### Kết quả một lượt thi · `student/attempt-result.html`

- **Template** [student/attempt-result.html](../src/main/resources/templates/student/attempt-result.html)
- **Đường dẫn** `/student/attempts/{id}/result`.
- **Phương thức render** [StudentContentController.result](../src/main/java/com/aptisprep/controller/student/StudentContentController.java#L147).
- **Lớp liên quan** [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java), [ExamAttemptService](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java), [AttemptResultReader](../src/main/java/com/aptisprep/service/exam/AttemptResultReader.java), [AttemptFinalizationService](../src/main/java/com/aptisprep/service/grading/AttemptFinalizationService.java), [ScoringGuideService](../src/main/java/com/aptisprep/service/scoring/ScoringGuideService.java), [IntegrationModels.AttemptResult](../src/main/java/com/aptisprep/service/contract/IntegrationModels.java#L171).
- **Chức năng** Hiển thị điểm, phản hồi, trạng thái chấm và nút xem lại hoặc chấm lại khi đủ điều kiện.
- **Biến đọc trong template từ controller hoặc luồng redirect** `attemptResult`, `canRegradeEmpty`, `feedbackWithoutGuide`, `scoreGuides`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `resultLabels`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `band`, `guide`, `isPlacement`, `res`, `score`, `skill`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/attempts/{id}/regrade-empty(id=${res.attemptId})}`, `POST @{/student/attempts/{id}/retry-grading(id=${res.attemptId})}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `band.attained`, `band.level`, `band.max`, `band.min`, `guide.bands`, `guide.bands.isEmpty`, `guide.max.stripTrailingZeros`, `guide.min.stripTrailingZeros`, `guide.skill`, `res.attemptId`, `res.examTitle`, `res.examType`, `res.hasFullMockTotal`, `res.isCombined`, `res.overallCefr`, `res.overallScaleScore`, `res.skills`, `res.skills.isEmpty`, `res.state`, `res.submittedAt`, `score.stripTrailingZeros`, `scoreGuides.isEmpty`, `skill.estimatedLevel`, `skill.feedback`, `skill.feedback.isBlank`, `skill.gradingState`, `skill.rawScore`, `skill.scaleScore`, `skill.skill`.
- **Tên trùng giữa model và phạm vi cục bộ** `skill`.
- **Class CSS ít dùng chung hơn** `data-table`, `result-actions-bar`, `result-container`, `result-detail-header`, `result-guide-card`, `result-guide-grid`, `result-guides`, `result-message`, `result-message-error`, `result-skill-feedback`, `result-skill-table`, `result-status`, `result-totals`, `table`, `table-responsive`, `table-wrap`, `visually-hidden`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-22"></a>

### Xem lại câu hỏi và bài làm · `student/attempt-review.html`

- **Template** [student/attempt-review.html](../src/main/resources/templates/student/attempt-review.html)
- **Đường dẫn** `/student/attempts/{id}/review`.
- **Phương thức render** [StudentExamController.review](../src/main/java/com/aptisprep/controller/student/StudentExamController.java#L52).
- **Lớp liên quan** [ExamAttemptService](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java), [ExamAttemptService.AttemptReview](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java#L647), [ExamSnapshotService](../src/main/java/com/aptisprep/service/exam/ExamSnapshotService.java), [AccountAccessService](../src/main/java/com/aptisprep/service/account/AccountAccessService.java).
- **Chức năng** Hiển thị đáp án, phần đã trả lời và media/ghi âm qua các route review có xác thực.
- **Biến đọc trong template từ controller hoặc luồng redirect** `attemptId`, `review`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `item`, `media`, `option`, `question`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `item.answered`, `item.audioSaved`, `item.content`, `item.itemId`, `item.options`, `item.options.isEmpty`, `item.position`, `item.selectedOptionId`, `item.textAnswer`, `item.userPosition`, `media.id`, `media.type`, `option.content`, `option.id`, `question.content`, `question.instruction`, `question.items`, `question.media`, `question.questionId`, `question.readingPassage`, `question.skill`, `review.questions`, `review.title`.
- **Class CSS ít dùng chung hơn** `attempt-media`, `img-fluid`, `reading-passage-text`.
- **Class CSS thường dùng lại** `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-23"></a>

### Lịch sử kết quả học viên · `student/results/list.html`

- **Template** [student/results/list.html](../src/main/resources/templates/student/results/list.html)
- **Đường dẫn** `/student/results`.
- **Phương thức render** [StudentContentController.results](../src/main/java/com/aptisprep/controller/student/StudentContentController.java#L91).
- **Lớp liên quan** [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java), [ExamAttemptService](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java), [AttemptResultReader](../src/main/java/com/aptisprep/service/exam/AttemptResultReader.java), [IntegrationModels.HistoryPage](../src/main/java/com/aptisprep/service/contract/IntegrationModels.java#L278).
- **Chức năng** Tìm và lọc các lượt thi, mở kết quả hoặc tiếp tục lượt chưa hoàn tất.
- **Biến đọc trong template từ controller hoặc luồng redirect** `completedCount`, `history`, `inProgressCount`, `selectedExamType`, `selectedSkill`, `selectedState`, `totalPages`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `attempt`, `baseUrl`, `currentPage`, `end`, `filtered`, `index`, `pageParam`, `pageUrl`, `q`, `skill`, `start`, `totalItems`, `value`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{/student/results}`, `POST @{/student/attempts/{id}/discard(id=${attempt.attemptId})}`.
- **Tên input, select hoặc button khai báo trực tiếp** `examType`, `q`, `size`, `skill`, `state`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `attempt.attemptId`, `attempt.canDiscard`, `attempt.examId`, `attempt.examTitle`, `attempt.examType`, `attempt.finalScaleScore`, `attempt.overallCefr`, `attempt.skills`, `attempt.skills.isEmpty`, `attempt.state`, `attempt.submittedAt`, `history.items`, `history.items.isEmpty`, `history.page`, `history.size`, `history.totalItems`, `q.isBlank`, `selectedExamType.isBlank`, `selectedSkill.isBlank`, `selectedState.isBlank`, `skill.estimatedLevel`, `skill.skill`, `value.replace`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `attempt`, `q`, `skill`.
- **Class CSS ít dùng chung hơn** `active`, `button-danger`, `compact-filter-bar`, `data-table`, `disabled`, `empty-result-state`, `field-search`, `field-select`, `filter-actions-right`, `in-progress-time`, `page-header-actions`, `page-link`, `pagination`, `pagination-gap`, `pagination-pages`, `result-compact-table`, `result-discard-button`, `result-filters`, `result-history-page`, `result-levels-mini`, `result-overall-score`, `result-score-cell`, `result-title-text`, `result-type-chip`, `results-tab-nav`, `results-table-container`, `skill-mini-pill`, `sr-only`, `student-pagination`, `tab-count-badge`, `tab-link`, `table-row-actions`, `text-nowrap`, `text-right`, `time-meta-text`, `toolbar`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `field`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-24"></a>

### Chi tiết tác vụ chấm AI · `student/assessment.html`

- **Template** [student/assessment.html](../src/main/resources/templates/student/assessment.html)
- **Đường dẫn** `/student/assessments/{key}`, `/admin/assessments/{key}`.
- **Phương thức render** [AiAssessmentController.showStudentAssessment](../src/main/java/com/aptisprep/controller/AiAssessmentController.java#L25), [AiAssessmentController.showAdminAssessment](../src/main/java/com/aptisprep/controller/AiAssessmentController.java#L31).
- **Lớp liên quan** [AiAssessmentContract](../src/main/java/com/aptisprep/service/contract/AiAssessmentContract.java), [AiAssessmentContract.AssessmentView](../src/main/java/com/aptisprep/service/contract/AiAssessmentContract.java#L68), [AiAssessmentService](../src/main/java/com/aptisprep/service/grading/AiAssessmentService.java), [GeminiGradingService](../src/main/java/com/aptisprep/service/grading/GeminiGradingService.java), [UserRepository](../src/main/java/com/aptisprep/repository/UserRepository.java).
- **Chức năng** Xem kết quả một tác vụ và gửi POST tới route retry tương ứng với vai trò.
- **Biến đọc trong template từ controller hoặc luồng redirect** `assessment`, `assessmentBasePath`.
- **Biến dùng chung** `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `status`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${assessmentBasePath + assessment.assessmentKey + '/retry'}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `assessment.assessmentKey`, `assessment.estimatedCefr`, `assessment.feedback`, `assessment.score`, `assessment.state`, `assessmentBasePath.startsWith`, `status.toString`.
- **Class CSS ít dùng chung hơn** `badge-danger`, `badge-success`, `badge-warning`, `feedback-text`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`, `student-theme`.

</details>

<a id="html-25"></a>

### Danh sách Flashcard học viên · `student/flashcards/list.html`

- **Template** [student/flashcards/list.html](../src/main/resources/templates/student/flashcards/list.html)
- **Đường dẫn** `/student/flashcards`.
- **Phương thức render** [StudentFlashcardController.list](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L32).
- **Lớp liên quan** [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java), [FlashcardViews](../src/main/java/com/aptisprep/dto/view/FlashcardViews.java), [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java).
- **Chức năng** Tìm bộ thẻ, xem bộ thẻ theo bài học, mở ôn tập và lưu trữ bộ cá nhân.
- **Biến đọc trong template từ controller hoặc luồng redirect** `lessonBranches`, `lessonResultCount`, `mineCount`, `mineResultCount`, `q`, `sets`.
- **Biến dùng chung** `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `branch`, `set`, `tier`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{/student/flashcards}`, `POST @{/student/flashcards/{id}/archive(id=${set.id()})}`.
- **Tên input, select hoặc button khai báo trực tiếp** `q`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `branch.count`, `branch.levels`, `branch.skill`, `set.count`, `set.id`, `set.lessonId`, `set.lessonSkill`, `set.lessonTitle`, `set.level`, `set.personal`, `set.title`, `sets.isEmpty`, `tier.level`, `tier.sets`, `tier.sets.size`.
- **Tên trùng giữa model và phạm vi cục bộ** `set`.
- **Class CSS ít dùng chung hơn** `button-quiet`, `flashcard-create-action`, `flashcard-function-group`, `flashcard-group-heading`, `flashcard-level-branch`, `flashcard-level-branches`, `flashcard-library`, `flashcard-library-header`, `flashcard-manage`, `flashcard-page`, `flashcard-personal-empty`, `flashcard-row`, `flashcard-search`, `flashcard-search-line`, `flashcard-search-simple`, `flashcard-set-actions`, `flashcard-set-card`, `flashcard-set-content`, `flashcard-set-grid`, `flashcard-set-mark`, `flashcard-set-meta`, `flashcard-skill-branch`, `flashcard-skill-label`, `flashcard-topic-tree`, `personal-badge`, `set-badges`, `set-lesson-source`, `toolbar-search`.
- **Class CSS thường dùng lại** `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `empty-box`, `footer-wrap`, `hamburger-icon`, `messages-container`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-theme`.

</details>

<a id="html-26"></a>

### Tạo hoặc sửa Flashcard cá nhân · `student/flashcards/form.html`

- **Template** [student/flashcards/form.html](../src/main/resources/templates/student/flashcards/form.html)
- **Đường dẫn** `/student/flashcards/create`, `/student/flashcards/{id}/edit`.
- **Phương thức render** [StudentFlashcardController.showCreateForm](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L111), [StudentFlashcardController.showEditForm](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L118).
- **Lớp liên quan** [FlashcardSetForm](../src/main/java/com/aptisprep/dto/form/FlashcardSetForm.java), [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java), [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java).
- **Chức năng** Nhập tiêu đề, cấp độ và danh sách thẻ. Gửi form tạo hoặc sửa về controller học viên.
- **Biến đọc trong template từ controller hoặc luồng redirect** `form`, `levels`, `setId`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `message`, `row`, `stat`, `status`, `v`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/flashcard-editor.js](../src/main/resources/static/js/flashcard-editor.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${setId==null?'/student/flashcards':'/student/flashcards/'+setId}`.
- **Đối tượng form từ th:object** `${form}`.
- **Field gắn với đối tượng form** `cefrLevel`, `description`, `status`, `title`.
- **Tên input, select hoặc button khai báo trực tiếp** `${'rows[' + (stat.index) + '].definition'}`, `${'rows[' + (stat.index) + '].id'}`, `${'rows[' + (stat.index) + '].term'}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `row.definition`, `row.id`, `row.term`, `stat.count`, `stat.index`, `status.first`.
- **Tên trùng giữa model và phạm vi cục bộ** `message`.
- **Class CSS ít dùng chung hơn** `bulk-import`, `bulk-import-body`, `card-editor-row`, `card-number`, `catalog-form-grid`, `char-count`, `editor-actions`, `flashcard-editor`, `flashcard-editor-heading`, `flashcard-page`, `remove-card`.
- **Class CSS thường dùng lại** `actions`, `alert`, `alert-danger`, `ap-alert`, `ap-alert-danger`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-theme`.

</details>

<a id="html-27"></a>

### Ôn bộ Flashcard · `student/flashcards/study.html`

- **Template** [student/flashcards/study.html](../src/main/resources/templates/student/flashcards/study.html)
- **Đường dẫn** `/student/flashcards/{id}`.
- **Phương thức render** [StudentFlashcardController.study](../src/main/java/com/aptisprep/controller/student/StudentFlashcardController.java#L174).
- **Lớp liên quan** [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java), [FlashcardViews](../src/main/java/com/aptisprep/dto/view/FlashcardViews.java).
- **Chức năng** Lật thẻ, đánh dấu đã thuộc hoặc cần ôn và gửi trạng thái qua flashcard-study.js.
- **Biến đọc trong template từ controller hoặc luồng redirect** `cards`, `set`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `card`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/flashcard-study.js](../src/main/resources/static/js/flashcard-study.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/student/flashcards/{id}/cards/0/state(id=${set.id})}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `card.definition`, `card.id`, `card.nextReviewAt`, `card.state`, `card.term`, `set.cefrLevel`, `set.cefrLevel.toLowerCase`, `set.description`, `set.id`, `set.lesson`, `set.lesson.id`, `set.lesson.skill`, `set.lesson.title`, `set.title`.
- **Class CSS ít dùng chung hơn** `align-items-center`, `completion-actions`, `confetti`, `d-flex`, `definition`, `face-content`, `face-label`, `flashcard-3d`, `flashcard-back`, `flashcard-controls`, `flashcard-front`, `flashcard-inner`, `flashcard-options`, `flashcard-page`, `flashcard-result`, `flashcard-result-grid`, `flashcard-side`, `flashcard-stage`, `flashcard-study`, `flashcard-toggle`, `flex-wrap`, `gap-3`, `is-learning`, `is-mastered`, `justify-content-between`, `justify-content-center`, `keyboard-hint`, `learning-review`, `mastery-controls`, `navi-celebrate`, `set-lesson-source`, `study-complete`, `study-lesson-source`, `study-summary`, `term`, `term-list`, `term-overview`, `toolbar`.
- **Class CSS thường dùng lại** `actions`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-theme`.

</details>

<a id="html-28"></a>

### Tổng quan quản trị · `admin/dashboard.html`

- **Template** [admin/dashboard.html](../src/main/resources/templates/admin/dashboard.html)
- **Đường dẫn** `/admin/dashboard`.
- **Phương thức render** [AdminDashboardController.dashboard](../src/main/java/com/aptisprep/controller/admin/AdminDashboardController.java#L41).
- **Lớp liên quan** [StudentManagementService](../src/main/java/com/aptisprep/service/admin/StudentManagementService.java), [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java), [LessonRepository](../src/main/java/com/aptisprep/repository/LessonRepository.java), [QuestionRepository](../src/main/java/com/aptisprep/repository/QuestionRepository.java), [MockExamRepository](../src/main/java/com/aptisprep/repository/MockExamRepository.java).
- **Chức năng** Hiển thị số liệu tổng quan và liên kết quản trị.
- **Biến đọc trong template từ controller hoặc luồng redirect** `email`, `lessonCount`, `publishedExamCount`, `questionCount`, `recentAttempts`, `recentContent`, `studentCount`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `resultLabels`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `item`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `item.kind`, `item.learnerName`, `item.result.attemptId`, `item.result.examTitle`, `item.result.state`, `item.title`, `item.updatedAt`, `item.url`, `recentAttempts.isEmpty`, `recentContent.isEmpty`, `recentContent.size`.
- **Class CSS ít dùng chung hơn** `admin-main-grid`, `admin-overview`, `admin-stat-grid`, `card-icon`, `dashboard-grid`, `dashboard-header`, `dashboard-page`, `dashboard-recent-list`, `empty-state`, `empty-state-icon`, `panel-heading`, `panel-kicker`, `quick-arrow`, `quick-card`, `quick-grid`.
- **Class CSS thường dùng lại** `admin-theme`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-29"></a>

### Danh sách câu hỏi hoặc đề thi · `admin/catalog/list.html`

- **Template** [admin/catalog/list.html](../src/main/resources/templates/admin/catalog/list.html)
- **Đường dẫn** `/admin/questions`, `/admin/exams`.
- **Phương thức render** [AdminCatalogController.questions](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L27), [AdminCatalogController.exams](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L39).
- **Lớp liên quan** [CatalogService](../src/main/java/com/aptisprep/service/admin/CatalogService.java), [AdminExamService](../src/main/java/com/aptisprep/service/admin/AdminExamService.java), [AdminCatalogView](../src/main/java/com/aptisprep/dto/view/AdminCatalogView.java), [ListPagination](../src/main/java/com/aptisprep/controller/ListPagination.java).
- **Chức năng** Dùng catalog.key để đổi mô-đun, hiển thị danh sách, bộ lọc và phân trang.
- **Biến đọc trong template từ controller hoặc luồng redirect** `catalog`, `pageData`, `selectedKind`, `selectedSkill`, `selectedStatus`, `skills`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `baseUrl`, `currentPage`, `end`, `entry`, `index`, `kind`, `pageParam`, `pageUrl`, `q`, `skill`, `start`, `totalItems`, `totalPages`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{${catalog.baseUrl()}}`.
- **Tên input, select hoặc button khai báo trực tiếp** `kind`, `q`, `skill`, `status`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `catalog.baseUrl`, `catalog.createLabel`, `catalog.editable`, `catalog.key`, `catalog.kinds`, `catalog.singular`, `catalog.title`, `catalog.typeLabel`, `entry.date`, `entry.id`, `entry.kind`, `entry.level`, `entry.skill`, `entry.status`, `entry.statusLabel`, `entry.subtitle`, `entry.title`, `pageData.items`, `pageData.items.isEmpty`, `pageData.page`, `pageData.size`, `pageData.totalItems`, `pageData.totalPages`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `entry`, `q`, `skill`, `totalPages`.
- **Class CSS ít dùng chung hơn** `active`, `admin-catalog`, `catalog-row-description`, `catalog-table`, `catalog-table-actions`, `catalog-toolbar`, `data-table`, `disabled`, `page-actions`, `page-link`, `pagination`, `pagination-gap`, `pagination-pages`, `student-pagination`, `table-wrap`, `toolbar`, `toolbar-search`.
- **Class CSS thường dùng lại** `admin-theme`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `empty-box`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-30"></a>

### Chi tiết câu hỏi hoặc đề thi quản trị · `admin/catalog/detail.html`

- **Template** [admin/catalog/detail.html](../src/main/resources/templates/admin/catalog/detail.html)
- **Đường dẫn** `/admin/questions/{id}`, `/admin/exams/{id}`.
- **Phương thức render** [AdminCatalogController.question](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L72), [AdminCatalogController.exam](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L77).
- **Lớp liên quan** [CatalogService](../src/main/java/com/aptisprep/service/admin/CatalogService.java), [AdminExamService](../src/main/java/com/aptisprep/service/admin/AdminExamService.java), [AdminCatalogView](../src/main/java/com/aptisprep/dto/view/AdminCatalogView.java), [StorageService](../src/main/java/com/aptisprep/service/storage/StorageService.java).
- **Chức năng** Xem nội dung, câu hỏi hoặc media. Các form quản lý trạng thái và media dùng cùng controller.
- **Biến đọc trong template từ controller hoặc luồng redirect** `catalog`, `entry`, `examPreview`, `mediaItems`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `fact`, `item`, `media`, `option`, `order`, `question`, `questionOrder`, `row`, `section`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${'/admin/questions/' + entry.id + '/media/' + media.id}`, `POST ${'/admin/questions/' + entry.id + '/media/' + media.id + '/move'}`, `POST ${'/admin/questions/' + entry.id + '/media/' + media.id + '/delete'}`, `POST ${'/admin/questions/' + entry.id + '/media'}`, `POST ${'/admin/' + catalog.key + '/' + entry.id + '/publish'}`, `POST @{/admin/exams/{id}/unpublish(id=${entry.id})}`, `POST ${'/admin/questions/' + entry.id + '/archive'}`, `POST ${'/admin/questions/' + entry.id + '/restore'}`.
- **Tên input, select hoặc button khai báo trực tiếp** `altText`, `direction`, `files`, `maxPlayCount`, `transcript`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `catalog.baseUrl`, `catalog.editable`, `catalog.key`, `catalog.title`, `catalog.typeLabel`, `entry.facts`, `entry.id`, `entry.kind`, `entry.level`, `entry.rows`, `entry.rows.isEmpty`, `entry.skill`, `entry.status`, `entry.statusLabel`, `entry.subtitle`, `entry.title`, `fact.label`, `fact.value`, `item.content`, `item.maxWordCount`, `item.minWordCount`, `item.options`, `item.options.isEmpty`, `item.position`, `item.responseSeconds`, `media.altText`, `media.altText.isBlank`, `media.browserSpeech`, `media.contentType`, `media.filename`, `media.fileSize`, `media.id`, `media.maxPlayCount`, `media.mediaType`, `media.mediaUrl`, `media.mediaUrl.startsWith`, `media.originalFilename`, `media.position`, `media.transcript`, `media.type`, `mediaItems.isEmpty`, `mediaItems.size`, `option.code`, `option.code.isBlank`, `option.content`, `order.index`, `question.content`, `question.id`, `question.instruction`, `question.instruction.isBlank`, `question.items`, `question.items.isEmpty`, `question.items.size`, `question.media`, `question.media.isEmpty`, `question.readingPassage`, `question.readingPassage.isBlank`, `question.skill`, `question.type`, `question.typeLabel`, `questionOrder.index`, `row.description`, `row.title`, `row.title.startsWith`, `row.value`, `row.value.isBlank`, `section.minutes`, `section.questions`, `section.questions.isEmpty`, `section.questions.size`, `section.skill`.
- **Class CSS ít dùng chung hơn** `admin-catalog`, `admin-description`, `admin-reading-passage`, `admin-reading-passage-text`, `catalog-actions-panel`, `catalog-answer`, `catalog-content-list`, `catalog-facts`, `catalog-media-actions`, `catalog-media-audio`, `catalog-media-card`, `catalog-media-empty`, `catalog-media-form`, `catalog-media-form-footer`, `catalog-media-heading`, `catalog-media-list`, `catalog-media-meta`, `catalog-media-name`, `catalog-media-panel`, `catalog-media-position`, `catalog-media-preview`, `catalog-media-preview-box`, `catalog-media-upload`, `catalog-speech-fallback`, `catalog-table`, `dashboard-grid`, `data-table`, `exam-preview-audio`, `exam-preview-item`, `exam-preview-item-heading`, `exam-preview-items`, `exam-preview-media`, `exam-preview-media-list`, `exam-preview-option`, `exam-preview-option-control`, `exam-preview-options`, `exam-preview-question`, `exam-preview-question-header`, `exam-preview-question-meta`, `exam-preview-questions`, `exam-preview-recording`, `exam-preview-response`, `exam-preview-response-box`, `exam-preview-section`, `exam-preview-section-summary`, `exam-preview-textarea`, `media-empty-icon`, `media-limit-field`, `media-type-badge`, `page-actions`, `panel-heading`, `panel-kicker`, `question-instruction`, `question-item-number`, `table-wrap`, `text-link`.
- **Class CSS thường dùng lại** `admin-theme`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `empty-box`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-31"></a>

### Tạo hoặc sửa câu hỏi và đề thi · `admin/catalog/form.html`

- **Template** [admin/catalog/form.html](../src/main/resources/templates/admin/catalog/form.html)
- **Đường dẫn** `/admin/questions/create`, `/admin/questions/{id}/edit`, `/admin/exams/create`, `/admin/exams/{id}/edit`.
- **Phương thức render** [AdminCatalogController.createQuestion](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L93), [AdminCatalogController.editQuestion](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L112), [AdminCatalogController.createExam](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L98), [AdminCatalogController.editExam](../src/main/java/com/aptisprep/controller/admin/AdminCatalogController.java#L117).
- **Lớp liên quan** [QuestionForm](../src/main/java/com/aptisprep/dto/form/QuestionForm.java), [ExamForm](../src/main/java/com/aptisprep/dto/form/ExamForm.java), [CatalogService](../src/main/java/com/aptisprep/service/admin/CatalogService.java), [AdminExamService](../src/main/java/com/aptisprep/service/admin/AdminExamService.java), [QuestionDefinitionValidator](../src/main/java/com/aptisprep/service/admin/QuestionDefinitionValidator.java), [ExamDefinitionValidator](../src/main/java/com/aptisprep/service/admin/ExamDefinitionValidator.java).
- **Chức năng** Hai biểu mẫu được khai báo trực tiếp trong file theo catalog.key. Controller nhận các POST tạo và sửa.
- **Biến đọc trong template từ controller hoặc luồng redirect** `catalog`, `certificates`, `editing`, `entry`, `examError`, `examForm`, `formError`, `levels`, `publishedQuestions`, `questionForm`, `schemes`, `skills`.
- **Biến dùng chung** `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `item`, `itemPath`, `itemStat`, `option`, `optionPath`, `optionStat`, `question`, `section`, `sectionPath`, `sectionStat`, `skillCode`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/catalog-editor.js](../src/main/resources/static/js/catalog-editor.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${editing ? '/admin/questions/' + entry.id + '/edit' : '/admin/questions'}`, `POST ${editing ? '/admin/exams/' + entry.id + '/edit' : '/admin/exams'}`.
- **Tên input, select hoặc button khai báo trực tiếp** `_demoOnly`, `${itemPath + '.content'}`, `${itemPath + '.correctPosition'}`, `${itemPath + '.maxWordCount'}`, `${itemPath + '.minWordCount'}`, `${itemPath + '.responseSeconds'}`, `${optionPath + '.code'}`, `${optionPath + '.content'}`, `${sectionPath + '.durationMinutes'}`, `${sectionPath + '.questionIds'}`, `${sectionPath + '.skill'}`, `cefrLevel`, `certificateId`, `content`, `demoOnly`, `description`, `difficulty`, `examType`, `file`, `instruction`, `partNo`, `questionType`, `readingPassage`, `scoringSchemeId`, `skill`, `status`, `targetLevel`, `title`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `catalog.baseUrl`, `catalog.createLabel`, `catalog.key`, `catalog.singular`, `catalog.title`, `entry.id`, `examForm.certificateId`, `examForm.demoOnly`, `examForm.description`, `examForm.examType`, `examForm.scoringSchemeId`, `examForm.sections`, `examForm.targetLevel`, `examForm.title`, `item.certificate.id`, `item.content`, `item.correctPosition`, `item.examType`, `item.id`, `item.maxWordCount`, `item.minWordCount`, `item.name`, `item.options`, `item.responseSeconds`, `item.scopeSkill`, `item.version`, `itemStat.index`, `option.code`, `option.content`, `optionStat.index`, `question.content`, `question.id`, `question.skill`, `questionForm.cefrLevel`, `questionForm.content`, `questionForm.difficulty`, `questionForm.instruction`, `questionForm.items`, `questionForm.partNo`, `questionForm.questionType`, `questionForm.readingPassage`, `questionForm.skill`, `questionForm.status`, `section.durationMinutes`, `section.questionIds`, `section.skill`, `sectionStat.index`.
- **Class CSS ít dùng chung hơn** `admin-catalog`, `alert-error`, `catalog-checkbox`, `catalog-form-grid`, `catalog-form-intro`, `catalog-form-intro-small`, `catalog-form-section`, `catalog-form-step`, `catalog-guided-help`, `catalog-option-fields`, `catalog-preview-form`, `conditional-field`, `correct-position-field`, `exam-section-editor`, `exam-section-number`, `exam-section-title`, `exam-total-summary`, `field-help`, `inline-checkbox`, `item-response-rule`, `item-rules-grid`, `item-writing-rule`, `option-code-label`, `option-correct-label`, `option-correct-text`, `page-actions`, `question-editor-form`, `question-item-editor`, `question-item-editor-heading`, `question-item-editor-number`, `question-item-editor-title`, `remove-item`, `section-add-action`, `section-question`, `section-question-ids`, `section-question-label`, `section-question-picker`, `section-skill`, `upload-field`.
- **Class CSS thường dùng lại** `admin-theme`, `alert`, `alert-danger`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `field`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-32"></a>

### Danh sách bài học quản trị · `admin/lesson/list.html`

- **Template** [admin/lesson/list.html](../src/main/resources/templates/admin/lesson/list.html)
- **Đường dẫn** `/admin/lessons`.
- **Phương thức render** [AdminLessonController.list](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L29).
- **Lớp liên quan** [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java), [LessonService.SkillBranch](../src/main/java/com/aptisprep/service/learning/LessonService.java#L67), [LessonService.LevelBranch](../src/main/java/com/aptisprep/service/learning/LessonService.java#L54).
- **Chức năng** Hiển thị cây bài học theo kỹ năng và cấp độ.
- **Biến đọc trong template từ controller hoặc luồng redirect** `tree`.
- **Biến dùng chung** `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `branch`, `lesson`, `level`, `topic`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `branch.count`, `branch.levels`, `branch.skill`, `lesson.displayOrder`, `lesson.id`, `lesson.status`, `lesson.title`, `level.lessons`, `level.level`, `level.topics`, `topic.key`, `topic.value`.
- **Tên trùng giữa model và phạm vi cục bộ** `lesson`.
- **Class CSS ít dùng chung hơn** `admin-catalog`, `admin-lesson-leaf`, `admin-lesson-tree`, `catalog-table-actions`, `curriculum-tree`, `lesson-leaf-actions`, `lesson-leaf-title`, `lesson-leaves`, `level-branch`, `skill-branch`, `topic-branch`.
- **Class CSS thường dùng lại** `admin-theme`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-33"></a>

### Chi tiết bài học quản trị · `admin/lesson/detail.html`

- **Template** [admin/lesson/detail.html](../src/main/resources/templates/admin/lesson/detail.html)
- **Đường dẫn** `/admin/lessons/{id}`.
- **Phương thức render** [AdminLessonController.detail](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L35).
- **Lớp liên quan** [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java), [Lesson](../src/main/java/com/aptisprep/entity/Lesson.java), [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java).
- **Chức năng** Xem nội dung, câu hỏi, thống kê và bộ thẻ liên kết.
- **Biến đọc trong template từ controller hoặc luồng redirect** `flashcardSet`, `lesson`, `questions`.
- **Biến dùng chung** `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `option`, `question`, `stat`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên, [css/lesson-detail.css](../src/main/resources/static/css/lesson-detail.css).
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/admin/lessons/{id}/archive(id=${lesson.id})}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `flashcardSet.id`, `flashcardSet.title`, `lesson.audioName`, `lesson.audioPath`, `lesson.cefrLevel`, `lesson.description`, `lesson.id`, `lesson.material`, `lesson.material.analysis`, `lesson.material.analysis.isBlank`, `lesson.material.checklist`, `lesson.material.checklist.isBlank`, `lesson.material.commonMistakes`, `lesson.material.commonMistakes.isBlank`, `lesson.material.exampleText`, `lesson.material.exampleText.isBlank`, `lesson.material.objectives`, `lesson.material.objectives.isBlank`, `lesson.material.practicePrompt`, `lesson.material.practicePrompt.isBlank`, `lesson.material.sampleResponse`, `lesson.material.sampleResponse.isBlank`, `lesson.material.strategy`, `lesson.material.strategy.isBlank`, `lesson.material.taskType`, `lesson.material.topic`, `lesson.skill`, `lesson.status`, `lesson.title`, `lesson.videoPath`, `question.options`, `question.prompt`, `questions.isEmpty`, `questions.size`, `stat.count`.
- **Class CSS ít dùng chung hơn** `lesson-classroom`, `lesson-complete`, `lesson-flashcard-callout`, `lesson-model`, `lesson-prose`, `lesson-question`, `lesson-video`, `page-actions`.
- **Class CSS thường dùng lại** `admin-theme`, `alert`, `alert-success`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-34"></a>

### Tạo hoặc sửa bài học · `admin/lesson/form.html`

- **Template** [admin/lesson/form.html](../src/main/resources/templates/admin/lesson/form.html)
- **Đường dẫn** `/admin/lessons/create`, `/admin/lessons/{id}/edit`.
- **Phương thức render** [AdminLessonController.showCreateForm](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L44), [AdminLessonController.showEditForm](../src/main/java/com/aptisprep/controller/admin/AdminLessonController.java#L69).
- **Lớp liên quan** [LessonForm](../src/main/java/com/aptisprep/dto/form/LessonForm.java), [LessonAuthorProfile](../src/main/java/com/aptisprep/service/learning/LessonAuthorProfile.java), [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java), [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java), [StorageService](../src/main/java/com/aptisprep/service/storage/StorageService.java).
- **Chức năng** Nhập bài học, phần nội dung, câu hỏi và media. Luồng POST kiểm tra dữ liệu theo kỹ năng.
- **Biến đọc trong template từ controller hoặc luồng redirect** `authorProfiles`, `editing`, `form`, `lesson`, `lessonId`, `levels`, `profile`, `skills`, `statuses`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `check`, `checkPath`, `entry`, `message`, `stat`, `status`, `task`, `v`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/lesson-author.js](../src/main/resources/static/js/lesson-author.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên, [css/lesson-detail.css](../src/main/resources/static/css/lesson-detail.css).
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${editing ? '/admin/lessons/'+lessonId : '/admin/lessons'}`.
- **Đối tượng form từ th:object** `${form}`.
- **Field gắn với đối tượng form** `analysis`, `cefrLevel`, `checklist`, `commonMistakes`, `description`, `displayOrder`, `exampleText`, `maxWordCount`, `minWordCount`, `objectives`, `practicePrompt`, `sampleResponse`, `sampleVisibility`, `skill`, `sourceName`, `sourceUrl`, `status`, `strategy`, `taskType`, `title`, `topic`, `transcript`, `transcriptVisibility`, `videoUrl`.
- **Tên input, select hoặc button khai báo trực tiếp** `${checkPath + '.correctOption'}`, `${checkPath + '.explanation'}`, `${checkPath + '.optionA'}`, `${checkPath + '.optionB'}`, `${checkPath + '.optionC'}`, `${checkPath + '.optionD'}`, `${checkPath + '.optionE'}`, `${checkPath + '.optionF'}`, `${checkPath + '.prompt'}`, `audio`, `image`, `removeAudio`, `removeImage`, `removeVideo`, `video`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `check.correctOption`, `check.explanation`, `check.optionA`, `check.optionB`, `check.optionC`, `check.optionD`, `check.optionE`, `check.optionF`, `check.prompt`, `entry.key`, `entry.value.analysisLabel`, `entry.value.checklistLabel`, `entry.value.exampleLabel`, `entry.value.mediaHint`, `entry.value.promptLabel`, `entry.value.sampleLabel`, `entry.value.strategyLabel`, `entry.value.tasks`, `form.checks`, `form.checks.isEmpty`, `form.skill`, `lesson.audioName`, `lesson.audioPath`, `lesson.id`, `lesson.imagePath`, `lesson.videoPath`, `profile.analysisLabel`, `profile.checklistLabel`, `profile.exampleLabel`, `profile.mediaHint`, `profile.promptLabel`, `profile.sampleLabel`, `profile.strategyLabel`, `profile.tasks`, `stat.count`, `stat.index`, `status.first`.
- **Tên trùng giữa model và phạm vi cục bộ** `entry`, `message`.
- **Class CSS ít dùng chung hơn** `add-option`, `catalog-form-grid`, `check-line`, `correct-option`, `editor-actions`, `empty-inline`, `lesson-author`, `lesson-author-extra`, `lesson-check-editor`, `lesson-core-fields`, `lesson-editor-form`, `lesson-form-grid`, `lesson-form-section`, `lesson-hero-image`, `lesson-media-placement`, `lesson-question-editor`, `lesson-video`, `option-limit`, `panel-heading`, `question-add-footer`, `question-option`, `question-option-grid`, `remove-option`, `remove-question`.
- **Class CSS thường dùng lại** `admin-theme`, `alert`, `alert-danger`, `ap-alert`, `ap-alert-danger`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `field`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-35"></a>

### Danh sách kết quả học viên · `admin/result/list.html`

- **Template** [admin/result/list.html](../src/main/resources/templates/admin/result/list.html)
- **Đường dẫn** `/admin/results`.
- **Phương thức render** [AdminResultController.list](../src/main/java/com/aptisprep/controller/admin/AdminResultController.java#L22).
- **Lớp liên quan** [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java), [ExamAttemptService](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java), [AttemptResultReader](../src/main/java/com/aptisprep/service/exam/AttemptResultReader.java), [IntegrationModels.AdminHistoryPage](../src/main/java/com/aptisprep/service/contract/IntegrationModels.java#L290).
- **Chức năng** Tìm và lọc kết quả theo học viên, đề, trạng thái hoặc ngày.
- **Biến đọc trong template từ controller hoặc luồng redirect** `history`, `selectedExamType`, `selectedSkill`, `selectedState`, `skills`, `totalPages`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `resultLabels`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `baseUrl`, `currentPage`, `end`, `from`, `index`, `item`, `pageParam`, `pageUrl`, `q`, `start`, `to`, `totalItems`, `v`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{/admin/results}`.
- **Tên input, select hoặc button khai báo trực tiếp** `examType`, `from`, `q`, `skill`, `state`, `to`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `history.items`, `history.items.isEmpty`, `history.page`, `history.size`, `history.totalItems`, `item.learnerEmail`, `item.learnerName`, `item.result.attemptId`, `item.result.examTitle`, `item.result.examType`, `item.result.state`, `item.result.submittedAt`, `v.replace`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `from`, `q`, `to`.
- **Class CSS ít dùng chung hơn** `active`, `admin-catalog`, `admin-responsive-table`, `data-table`, `disabled`, `page-link`, `pagination`, `pagination-gap`, `pagination-pages`, `result-filters`, `result-history-table`, `student-pagination`, `table-wrap`.
- **Class CSS thường dùng lại** `actions`, `admin-theme`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `empty-box`, `field`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-36"></a>

### Chi tiết kết quả quản trị · `admin/result/detail.html`

- **Template** [admin/result/detail.html](../src/main/resources/templates/admin/result/detail.html)
- **Đường dẫn** `/admin/results/{id}`.
- **Phương thức render** [AdminResultController.detail](../src/main/java/com/aptisprep/controller/admin/AdminResultController.java#L64).
- **Lớp liên quan** [ExamAttemptContract](../src/main/java/com/aptisprep/service/contract/ExamAttemptContract.java), [ExamAttemptService](../src/main/java/com/aptisprep/service/exam/ExamAttemptService.java), [AttemptResultReader](../src/main/java/com/aptisprep/service/exam/AttemptResultReader.java), [IntegrationModels.AttemptResult](../src/main/java/com/aptisprep/service/contract/IntegrationModels.java#L171).
- **Chức năng** Hiển thị điểm từng kỹ năng và nhận xét của lượt thi.
- **Biến đọc trong template từ controller hoặc luồng redirect** `attemptResult`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `resultLabels`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `score`, `skill`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `attemptResult.attemptId`, `attemptResult.examTitle`, `attemptResult.examType`, `attemptResult.finalScaleScore`, `attemptResult.isCombined`, `attemptResult.overallCefr`, `attemptResult.skills`, `attemptResult.state`, `score.stripTrailingZeros`, `skill.estimatedLevel`, `skill.feedback`, `skill.rawScore`, `skill.scaleScore`, `skill.skill`.
- **Tên trùng giữa model và phạm vi cục bộ** `skill`.
- **Class CSS ít dùng chung hơn** `attempt-result-page`, `result-feedback`, `result-hero`, `result-overall-panel`, `result-score-list`, `result-skill-card`, `result-skill-grid`, `result-skill-heading`, `result-status`.
- **Class CSS thường dùng lại** `admin-theme`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`, `student-content`.

</details>

<a id="html-37"></a>

### Danh sách chứng chỉ · `admin/settings/certificates.html`

- **Template** [admin/settings/certificates.html](../src/main/resources/templates/admin/settings/certificates.html)
- **Đường dẫn** `/admin/certificates`.
- **Phương thức render** [AdminScoringController.showCertificates](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L41).
- **Lớp liên quan** [ScoringAdminService](../src/main/java/com/aptisprep/service/scoring/ScoringAdminService.java), [ScoringViews](../src/main/java/com/aptisprep/service/scoring/ScoringViews.java), [ListPagination](../src/main/java/com/aptisprep/controller/ListPagination.java).
- **Chức năng** Danh sách và phân trang các chứng chỉ. /admin/settings chuyển hướng sang quản lý chứng chỉ.
- **Biến đọc trong template từ controller hoặc luồng redirect** `certificatePage`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `baseUrl`, `certificate`, `currentPage`, `end`, `index`, `pageParam`, `pageUrl`, `start`, `totalItems`, `totalPages`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `certificate.code`, `certificate.id`, `certificate.name`, `certificate.status`, `certificate.statusLabel`, `certificatePage.content`, `certificatePage.hasContent`, `certificatePage.number`, `certificatePage.totalElements`, `certificatePage.totalPages`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `certificate`, `totalPages`.
- **Class CSS ít dùng chung hơn** `active`, `admin-catalog`, `admin-responsive-table`, `admin-scoring-settings`, `catalog-table-actions`, `data-table`, `disabled`, `page-link`, `pagination`, `pagination-gap`, `pagination-pages`, `scoring-status`, `student-pagination`, `table-wrap`.
- **Class CSS thường dùng lại** `admin-theme`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `empty-box`, `footer-wrap`, `hamburger-icon`, `messages-container`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-38"></a>

### Xem, tạo hoặc sửa chứng chỉ · `admin/settings/certificate.html`

- **Template** [admin/settings/certificate.html](../src/main/resources/templates/admin/settings/certificate.html)
- **Đường dẫn** `/admin/certificates/create`, `/admin/certificates/{id}`, `/admin/certificates/{id}/edit`.
- **Phương thức render** [AdminScoringController.createCertificate](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L95), [AdminScoringController.certificate](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L103), [AdminScoringController.editCertificate](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L113).
- **Lớp liên quan** [CertificateForm](../src/main/java/com/aptisprep/dto/form/CertificateForm.java), [ScoringAdminService](../src/main/java/com/aptisprep/service/scoring/ScoringAdminService.java), [ScoringViews](../src/main/java/com/aptisprep/service/scoring/ScoringViews.java).
- **Chức năng** Cờ readOnly và editing chọn chế độ. Các form lưu, bật hoặc ngừng sử dụng chứng chỉ.
- **Biến đọc trong template từ controller hoặc luồng redirect** `certificate`, `certificateForm`, `editing`, `readOnly`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `message`, `status`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${editing ? '/admin/certificates/' + certificate.id : '/admin/certificates'}`, `POST @{/admin/certificates/{id}/disable(id=${certificate.id})}`, `POST @{/admin/certificates/{id}/activate(id=${certificate.id})}`.
- **Đối tượng form từ th:object** `${certificateForm}`.
- **Field gắn với đối tượng form** `code`, `description`, `name`, `status`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `certificate.id`, `certificate.name`, `certificate.status`, `status.first`.
- **Tên trùng giữa model và phạm vi cục bộ** `message`.
- **Class CSS ít dùng chung hơn** `admin-catalog`, `catalog-form-grid`, `field-error`, `scoring-fields`.
- **Class CSS thường dùng lại** `actions`, `admin-theme`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `field`, `footer-wrap`, `hamburger-icon`, `messages-container`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-39"></a>

### Danh sách bộ chấm điểm · `admin/settings/schemes.html`

- **Template** [admin/settings/schemes.html](../src/main/resources/templates/admin/settings/schemes.html)
- **Đường dẫn** `/admin/scoring-schemes`.
- **Phương thức render** [AdminScoringController.showScoringSchemes](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L50).
- **Lớp liên quan** [ScoringAdminService](../src/main/java/com/aptisprep/service/scoring/ScoringAdminService.java), [ScoringViews](../src/main/java/com/aptisprep/service/scoring/ScoringViews.java), [MockExamRepository](../src/main/java/com/aptisprep/repository/MockExamRepository.java), [ListPagination](../src/main/java/com/aptisprep/controller/ListPagination.java).
- **Chức năng** Lọc và phân trang các bộ chấm theo chứng chỉ và loại đề.
- **Biến đọc trong template từ controller hoặc luồng redirect** `certificates`, `examType`, `schemeExamTypes`, `schemePage`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `baseUrl`, `certificate`, `certificateId`, `currentPage`, `end`, `index`, `pageParam`, `pageUrl`, `scheme`, `start`, `totalItems`, `totalPages`, `type`, `types`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{/admin/scoring-schemes}`.
- **Tên input, select hoặc button khai báo trực tiếp** `certificateId`, `examType`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `certificate.id`, `certificate.name`, `scheme.certificateName`, `scheme.examType`, `scheme.id`, `scheme.name`, `scheme.scopeSkill`, `scheme.status`, `scheme.statusLabel`, `scheme.version`, `schemeExamTypes.get`, `schemePage.content`, `schemePage.hasContent`, `schemePage.number`, `schemePage.totalElements`, `schemePage.totalPages`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `certificate`, `certificateId`, `scheme`, `totalPages`.
- **Class CSS ít dùng chung hơn** `active`, `admin-catalog`, `admin-responsive-table`, `admin-scoring-settings`, `catalog-table-actions`, `data-table`, `disabled`, `page-link`, `pagination`, `pagination-gap`, `pagination-pages`, `scoring-status`, `student-pagination`, `study-instructions`, `table-wrap`, `toolbar`.
- **Class CSS thường dùng lại** `admin-theme`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `empty-box`, `footer-wrap`, `hamburger-icon`, `messages-container`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-40"></a>

### Xem, tạo hoặc sửa bộ chấm điểm · `admin/settings/scheme.html`

- **Template** [admin/settings/scheme.html](../src/main/resources/templates/admin/settings/scheme.html)
- **Đường dẫn** `/admin/scoring-schemes/create`, `/admin/scoring-schemes/{id}`, `/admin/scoring-schemes/{id}/edit`.
- **Phương thức render** [AdminScoringController.createScheme](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L189), [AdminScoringController.scheme](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L201), [AdminScoringController.editScheme](../src/main/java/com/aptisprep/controller/admin/AdminScoringController.java#L210).
- **Lớp liên quan** [ScoringSchemeForm](../src/main/java/com/aptisprep/dto/form/ScoringSchemeForm.java), [ScoreBandForm](../src/main/java/com/aptisprep/dto/form/ScoreBandForm.java), [ScoringScaleRuleForm](../src/main/java/com/aptisprep/dto/form/ScoringScaleRuleForm.java), [ScoringAdminService](../src/main/java/com/aptisprep/service/scoring/ScoringAdminService.java), [ScoringViews](../src/main/java/com/aptisprep/service/scoring/ScoringViews.java).
- **Chức năng** Quản lý bộ chấm, quy đổi điểm và các khoảng CEFR theo kỹ năng.
- **Biến đọc trong template từ controller hoặc luồng redirect** `addBand`, `bandForm`, `bands`, `certificates`, `editing`, `readOnly`, `scaleRuleForm`, `scheme`, `schemeForm`, `selectedSkill`, `skills`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `band`, `bandEditorOpen`, `certificate`, `key`, `level`, `message`, `scale`, `skill`, `status`, `type`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/catalog-editor.js](../src/main/resources/static/js/catalog-editor.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${editing ? '/admin/scoring-schemes/' + scheme.id : '/admin/scoring-schemes'}`, `POST @{/admin/scoring-schemes/{id}/activate(id=${scheme.id})}`, `POST @{/admin/scoring-schemes/{id}/disable(id=${scheme.id})}`, `GET @{${'/admin/scoring-schemes/' + scheme.id + (readOnly ? '' : '/edit') + '#cefr-bands'}}`, `POST @{/admin/scoring-schemes/{schemeId}/bands/{bandId}/delete(schemeId=${scheme.id},bandId=${band.id},skill=${selectedSkill})}`, `POST @{/admin/scoring-schemes/{id}/scale-rule(id=${scheme.id})}`, `POST @{/admin/scoring-schemes/{id}/bands(id=${scheme.id})}`.
- **Đối tượng form từ th:object** `${bandForm}`, `${scaleRuleForm}`, `${schemeForm}`.
- **Field gắn với đối tượng form** `cefrLevel`, `certificateId`, `examType`, `id`, `maxScore`, `minScore`, `name`, `passingScore`, `rawMax`, `rawMin`, `scaleMax`, `scaleMin`, `scopeSkill`, `status`, `targetLevel`, `version`.
- **Tên input, select hoặc button khai báo trực tiếp** `|ranges[${key}].maxScore|`, `|ranges[${key}].minScore|`, `|skillScales[${skill}].rawMax|`, `|skillScales[${skill}].rawMin|`, `|skillScales[${skill}].scaleMax|`, `|skillScales[${skill}].scaleMin|`, `configureScores`, `skill`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `band.cefrLevel`, `band.id`, `band.maxScore`, `band.minScore`, `bandForm.id`, `bands.isEmpty`, `certificate.code`, `certificate.id`, `certificate.name`, `scale.rawMax`, `scale.rawMin`, `scale.scaleMax`, `scale.scaleMin`, `scheme.certificateId`, `scheme.examType`, `scheme.id`, `scheme.name`, `scheme.status`, `schemeForm.ranges.get`, `schemeForm.skillScales.get`, `status.first`.
- **Tên trùng giữa model và phạm vi cục bộ** `certificate`, `message`, `skill`.
- **Class CSS ít dùng chung hơn** `admin-catalog`, `admin-responsive-table`, `catalog-form-grid`, `catalog-form-section`, `data-table`, `field-error`, `scoring-band-action-heading`, `scoring-band-actions`, `scoring-band-delete`, `scoring-fields`, `table-wrap`, `toolbar`.
- **Class CSS thường dùng lại** `actions`, `admin-theme`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `field`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-41"></a>

### Danh sách học viên · `admin/student/list.html`

- **Template** [admin/student/list.html](../src/main/resources/templates/admin/student/list.html)
- **Đường dẫn** `/admin/students`.
- **Phương thức render** [AdminStudentController.list](../src/main/java/com/aptisprep/controller/admin/AdminStudentController.java#L21).
- **Lớp liên quan** [StudentManagementService](../src/main/java/com/aptisprep/service/admin/StudentManagementService.java), [AdminStudentView](../src/main/java/com/aptisprep/dto/view/AdminStudentView.java), [ListPagination](../src/main/java/com/aptisprep/controller/ListPagination.java).
- **Chức năng** Tìm học viên và lọc trạng thái tài khoản.
- **Biến đọc trong template từ controller hoặc luồng redirect** `activeStudents`, `lockedStudents`, `pageData`, `selectedStatus`, `totalStudents`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `baseUrl`, `currentPage`, `end`, `index`, `pageParam`, `pageUrl`, `q`, `start`, `student`, `totalItems`, `totalPages`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `GET @{/admin/students}`.
- **Tên input, select hoặc button khai báo trực tiếp** `q`, `status`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `pageData.items`, `pageData.items.isEmpty`, `pageData.page`, `pageData.size`, `pageData.totalItems`, `pageData.totalPages`, `student.email`, `student.fullName`, `student.id`, `student.initials`, `student.joinedDate`, `student.statusClass`, `student.statusLabel`, `student.targetLevel`.
- **Helper tích hợp của Thymeleaf** `#strings`.
- **Tên trùng giữa model và phạm vi cục bộ** `q`, `student`, `totalPages`.
- **Class CSS ít dùng chung hơn** `active`, `admin-responsive-table`, `data-table`, `disabled`, `page-link`, `pagination`, `pagination-gap`, `pagination-pages`, `student-avatar`, `student-identity`, `student-management`, `student-pagination`, `student-sr-only`, `student-status`, `student-summary`, `student-table`, `student-toolbar`, `table-wrap`, `toolbar`, `toolbar-search`.
- **Class CSS thường dùng lại** `admin-theme`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `empty-box`, `footer-wrap`, `hamburger-icon`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-42"></a>

### Chi tiết học viên · `admin/student/detail.html`

- **Template** [admin/student/detail.html](../src/main/resources/templates/admin/student/detail.html)
- **Đường dẫn** `/admin/students/{id}`.
- **Phương thức render** [AdminStudentController.detail](../src/main/java/com/aptisprep/controller/admin/AdminStudentController.java#L38).
- **Lớp liên quan** [StudentManagementService](../src/main/java/com/aptisprep/service/admin/StudentManagementService.java), [AdminStudentView](../src/main/java/com/aptisprep/dto/view/AdminStudentView.java).
- **Chức năng** Xem hồ sơ, mục tiêu, tiến độ và kết quả. Controller tiếp nhận yêu cầu khóa hoặc mở khóa.
- **Biến đọc trong template từ controller hoặc luồng redirect** `student`.
- **Biến dùng chung** `display`, `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `attempt`, `path`, `skill`, `status`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST @{/admin/students/{id}/lock(id=${student.id})}`, `POST @{/admin/students/{id}/unlock(id=${student.id})}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `attempt.date`, `attempt.score`, `attempt.status`, `attempt.title`, `path.checkpointState`, `path.completedLessons`, `path.pathSource`, `path.percentLabel`, `path.progressPercent`, `path.skill`, `path.totalLessons`, `skill.level`, `skill.name`, `skill.target`, `status.toString`, `student.attemptPage`, `student.attempts`, `student.attempts.isEmpty`, `student.attemptSize`, `student.attemptTotal`, `student.email`, `student.fullName`, `student.goal`, `student.goal.targetDate`, `student.goal.targets.values`, `student.id`, `student.initials`, `student.joinedDate`, `student.progress`, `student.progress.isEmpty`, `student.skills`, `student.status.name`, `student.statusClass`, `student.statusLabel`.
- **Tên trùng giữa model và phạm vi cục bộ** `attempt`, `skill`.
- **Class CSS ít dùng chung hơn** `admin-responsive-table`, `badge-danger`, `badge-success`, `badge-warning`, `dashboard-grid`, `data-table`, `empty-state`, `lower-grid`, `pagination`, `panel-heading`, `skill-level`, `skill-list`, `skill-name`, `skill-target`, `student-account-actions`, `student-avatar`, `student-avatar-large`, `student-identity`, `student-lock-button`, `student-management`, `student-profile`, `student-profile-facts`, `student-profile-heading`, `student-status`, `student-status-inactive`, `student-table`, `student-target`, `table-wrap`.
- **Class CSS thường dùng lại** `admin-theme`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `empty-box`, `eyebrow`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `section-title-row`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-43"></a>

### Danh sách Flashcard quản trị · `admin/flashcards/list.html`

- **Template** [admin/flashcards/list.html](../src/main/resources/templates/admin/flashcards/list.html)
- **Đường dẫn** `/admin/flashcards`.
- **Phương thức render** [AdminFlashcardController.showFlashcardList](../src/main/java/com/aptisprep/controller/admin/AdminFlashcardController.java#L24).
- **Lớp liên quan** [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java), [FlashcardViews](../src/main/java/com/aptisprep/dto/view/FlashcardViews.java), [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java).
- **Chức năng** Xem bộ thẻ của giáo trình theo kỹ năng và cấp độ, mở trang tạo hoặc chỉnh sửa bộ thẻ.
- **Biến đọc trong template từ controller hoặc luồng redirect** `lessonBranches`, `sets`, `unclassifiedSets`.
- **Biến dùng chung** `error`, `errorMessage`, `info`, `infoMessage`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`, `successMessage`, `warning`, `warningMessage`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `branch`, `set`, `tier`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `branch.count`, `branch.levels`, `branch.skill`, `set.count`, `set.description`, `set.id`, `set.lessonId`, `set.lessonSkill`, `set.lessonTitle`, `set.level`, `set.status`, `set.title`, `sets.isEmpty`, `tier.level`, `tier.sets`, `tier.sets.size`, `unclassifiedSets.isEmpty`, `unclassifiedSets.size`.
- **Tên trùng giữa model và phạm vi cục bộ** `set`.
- **Class CSS ít dùng chung hơn** `flashcard-function-group`, `flashcard-level-branch`, `flashcard-level-branches`, `flashcard-library`, `flashcard-library-header`, `flashcard-page`, `flashcard-row`, `flashcard-set-actions`, `flashcard-set-card`, `flashcard-set-content`, `flashcard-set-grid`, `flashcard-set-mark`, `flashcard-set-meta`, `flashcard-skill-branch`, `flashcard-skill-label`, `flashcard-topic-tree`, `set-badges`, `set-description`, `set-lesson`, `set-lesson-source`, `set-status`.
- **Class CSS thường dùng lại** `admin-theme`, `alert`, `alert-danger`, `alert-info`, `alert-success`, `alert-warning`, `ap-alert`, `ap-alert-danger`, `ap-alert-info`, `ap-alert-success`, `ap-alert-warning`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `badge`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `empty-box`, `footer-wrap`, `hamburger-icon`, `messages-container`, `muted`, `nav-links`, `nav-toggle`, `nav-wrap`, `navi`, `navi-antenna`, `navi-arm`, `navi-arm-left`, `navi-arm-right`, `navi-body`, `navi-empty`, `navi-eye`, `navi-mouth`, `navi-shadow`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

<a id="html-44"></a>

### Tạo hoặc sửa Flashcard quản trị · `admin/flashcards/form.html`

- **Template** [admin/flashcards/form.html](../src/main/resources/templates/admin/flashcards/form.html)
- **Đường dẫn** `/admin/flashcards/create`, `/admin/flashcards/{id}/edit`.
- **Phương thức render** [AdminFlashcardController.showCreateForm](../src/main/java/com/aptisprep/controller/admin/AdminFlashcardController.java#L42), [AdminFlashcardController.showEditForm](../src/main/java/com/aptisprep/controller/admin/AdminFlashcardController.java#L50).
- **Lớp liên quan** [FlashcardSetForm](../src/main/java/com/aptisprep/dto/form/FlashcardSetForm.java), [FlashcardSetService](../src/main/java/com/aptisprep/service/learning/FlashcardSetService.java), [LessonService](../src/main/java/com/aptisprep/service/learning/LessonService.java).
- **Chức năng** Chọn bài học liên kết và nhập thông tin bộ thẻ. Gửi form tạo hoặc sửa về controller quản trị.
- **Biến đọc trong template từ controller hoặc luồng redirect** `form`, `lessons`, `levels`, `setId`.
- **Biến dùng chung** `display`, `isAdmin`, `isStudent`, `message`, `navSection`, `signedIn`.
- **Biến cục bộ hoặc tham số fragment** `activePage`, `lesson`, `message`, `row`, `stat`, `status`, `v`.
- **JavaScript nạp trực tiếp** [js/app-dialog.js](../src/main/resources/static/js/app-dialog.js), [js/flashcard-editor.js](../src/main/resources/static/js/flashcard-editor.js), [js/navigation.js](../src/main/resources/static/js/navigation.js).
- **CSS nạp trực tiếp** nhóm CSS dùng chung ở trên.
- **Thư viện từ CDN** Bootstrap 5.3.8.

<details>
<summary>Chi tiết template và form</summary>

- **Đích form** `POST @{/logout}`, `POST ${setId==null?'/admin/flashcards':'/admin/flashcards/'+setId}`.
- **Đối tượng form từ th:object** `${form}`.
- **Field gắn với đối tượng form** `cefrLevel`, `description`, `lessonId`, `status`, `title`.
- **Tên input, select hoặc button khai báo trực tiếp** `${'rows[' + (stat.index) + '].definition'}`, `${'rows[' + (stat.index) + '].id'}`, `${'rows[' + (stat.index) + '].term'}`.
- **Đường dẫn thuộc tính đọc trong biểu thức** `lesson.cefrLevel`, `lesson.id`, `lesson.skill`, `lesson.title`, `row.definition`, `row.id`, `row.term`, `stat.count`, `stat.index`, `status.first`.
- **Tên trùng giữa model và phạm vi cục bộ** `lesson`, `message`.
- **Class CSS ít dùng chung hơn** `bulk-import`, `bulk-import-body`, `card-editor-row`, `card-number`, `catalog-form-grid`, `char-count`, `editor-actions`, `flashcard-editor`, `flashcard-editor-heading`, `flashcard-page`, `remove-card`.
- **Class CSS thường dùng lại** `actions`, `admin-theme`, `alert`, `alert-danger`, `ap-alert`, `ap-alert-danger`, `ap-container`, `ap-skip-link`, `app-main`, `app-shell`, `app-sidebar`, `back-link`, `brand`, `brand-mark`, `button`, `button-secondary`, `button-small`, `collapse`, `collapsed`, `container`, `d-lg-block`, `dashboard-panel`, `dashboard-section`, `eyebrow`, `field`, `footer-wrap`, `hamburger-icon`, `nav-links`, `nav-toggle`, `nav-wrap`, `page-header`, `sidebar-content`, `sidebar-divider`, `sidebar-label`, `sidebar-link`, `sidebar-nav`, `sidebar-toggle`, `sidebar-toggle-icon`, `site-footer`, `site-header`.

</details>

## Chi tiết các thành phần HTML

<a id="html-60"></a>

### Nhãn trạng thái · `fragments/status-badge.html`

- **Template** [fragments/status-badge.html](../src/main/resources/templates/fragments/status-badge.html)
- **Lớp liên quan** [DisplayFormatting](../src/main/java/com/aptisprep/controller/DisplayFormatting.java).
- **Chức năng** Hiển thị nhãn và nội dung trạng thái. Kiểm thử [DisplayFormattingTests](../src/test/java/com/aptisprep/controller/DisplayFormattingTests.java#L62) gọi fragment trực tiếp.
- **Fragment khai báo** `statusBadge(status)`, `statusText(status)`.
- **Biến cục bộ hoặc tham số fragment** `status`.

<details>
<summary>Chi tiết template và form</summary>

- **Đường dẫn thuộc tính đọc trong biểu thức** `status.toString`.
- **Class CSS ít dùng chung hơn** `badge-danger`, `badge-success`, `badge-warning`.
- **Class CSS thường dùng lại** `badge`.

</details>

## Ví dụ lần theo đề Urban Life

1. `GET /student/placement` gọi `StudentAccountController.placement()`. Controller lấy đề từ `PlacementService` và truyền `skill`, `placementPage`, `placementExams` vào `placement.html`. `placementExams` là danh sách `PlacementOption`. Mỗi `exam` trong vòng lặp có danh sách `sections`, tạo biến `part` và trạng thái vòng lặp `partStat`.
2. Nút **Bắt đầu** gửi `skill` và `examId` tới `POST /student/placement/start`. `PlacementService` chuyển sang `ExamAttemptService`. Lượt thi mở tại `GET /student/attempts/{id}`.
3. `attempt.html` đọc `attempt`. `exam-runner.js` dùng các URL của `StudentExamController` để lưu bài, phát media, chuyển phần và nộp. Khi thoát, form gửi `POST /student/attempts/{id}/exit`.
4. Sau khi nộp, phần khách quan chạy qua `ObjectiveGradingService`. Writing và Speaking đi qua `AttemptGradingOrchestrationService`, `AiAssessmentService` và `GeminiGradingService`. Speaking dùng file ghi âm để phiên âm và dùng bản chép lời để chấm nội dung.
5. `StudentContentController.result()` nhận kết quả từ luồng `ExamAttemptService` và `AttemptResultReader`, rồi đưa `attemptResult`, `scoreGuides` và các cờ liên quan vào `attempt-result.html`.

Nội dung đề được thêm bằng [add-placement-urban-life-01.sql](../database/add-placement-urban-life-01.sql). Audio được tạo bằng [build-placement-urban-life-audio.ps1](../scripts/build-placement-urban-life-audio.ps1).
