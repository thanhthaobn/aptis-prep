# Checklist triển khai — Thông, Thảo, Trâm, Dương

Theo [phân công chính thức](TEAM_ASSIGNMENT_V2.md) và [ranh giới service](SERVICE_CONTRACTS.md). Thông = 1, Thảo = 2, Trâm = 3, Dương = 4. Checklist là việc cần làm, chưa đánh dấu chức năng hoàn tất chỉ vì đã có schema/UI/service nền.

Làm theo thứ tự trong từng người; sau bước bàn giao sớm thì tiếp tục việc độc lập, không đợi người khác xong cả luồng. Mỗi bước triển khai xuyên lớp, có kiểm thử và PR nhỏ. Mock/fixture trong test không được thay implementation thật ở cấu hình chạy ứng dụng.

## 0. Cả nhóm làm trước khi sửa nghiệp vụ

- [ ] Mỗi người chạy được project Java 21/SQL Server theo [SETUP_LOCAL](SETUP_LOCAL.md), đăng nhập demo và chạy test; cấu hình mật khẩu/key riêng, không commit.
- [ ] Đọc V2, UI_GUIDE, ROUTES, SERVICE_CONTRACTS và Git workflow; xác nhận bảng/file/route của mình. Không tạo Entity/Repository thứ hai cho cùng bảng.
- [ ] Tách các mapping/template/service preview dùng chung trước khi nhiều người cùng sửa: hồ sơ thuộc Thông, Placement thuộc Trâm; đề/làm bài/kết quả thuộc Dương; bài học thuộc Thảo.
- [ ] Thông + Trâm + Dương chốt DTO chứng chỉ/scheme, raw/scale/rubric/version, validator publish/start và test quy đổi kỳ vọng.
- [ ] Thông + Dương chốt AI request/result/job state, answerVersion, quyền truy cập, schema/writer và retry. Chốt một phương án lưu AI trong PR schema, không hai đường ghi.
- [ ] Cả nhóm chốt ACTIVE/context, Profile/path/summary, Placement/checkpoint result và lỗi/NULL/requestKey. Interface tối thiểu hiện có cần mở rộng có review, không mặc định đã đủ.
- [ ] Thống nhất seed/test ID chung; migration mới lấy số theo thứ tự merge, không sửa V001–V005. Người giữ nền review cấu hình/bảo mật/CSS/fragment/contract/migration.

## 1. Thông — bắt đầu bằng policy tối thiểu, không bắt đầu bằng AI

### Bước 1 — Mở đường cho cả nhóm

- [ ] Kiểm tra auth hiện có; triển khai/test ACTIVE/context tối thiểu, user mới chưa Goal/Profile phải trả trạng thái thiếu thật, không mặc định B2.
- [ ] Map Certificate/ScoringScheme/ScoreBand theo schema; một bộ cố định hợp lệ từ seed/migration mới được review, không hardcode điểm giả vào service thật.
- [ ] Làm service đọc chứng chỉ/scheme khả dụng và kiểm scheme đúng certificate/skill/status.
- [ ] Triển khai ScoringPolicyContract cho mạch khách quan nhỏ đã chốt, có raw→scale→CEFR theo version, test rounding/biên/thiếu band. Không coi ngưỡng fixture là điểm Aptis chính thức.
- [ ] **Bàn giao Trâm:** DTO lựa chọn/validator và một scheme khả dụng. **Bàn giao Dương:** policy version, đơn vị điểm, input/output kiểm thử. **Bàn giao Thảo:** ACTIVE/Goal/Profile context.

### Bước 2 — Hoàn thiện tài khoản và Chứng chỉ & Chấm điểm

- [ ] Hồ sơ/Goal: validate, quyền sở hữu, bỏ qua; đổi Goal gọi path service Thảo. Chốt session nếu đổi email principal.
- [ ] Admin học viên: danh sách/tìm/lọc/phân trang, chi tiết; chưa có summary/history thật thì test bằng mock, không tự làm query của Thảo/Dương.
- [ ] Khóa/mở khóa và chặn request tiếp theo của session cũ; kiểm ACTIVE trước ghi, không unlock INACTIVE sai luật.
- [ ] Nối CRUD chứng chỉ/scheme/band vào giao diện có sẵn; POST/CSRF/redirect, code/version duy nhất, band không chồng lấn, ngừng dùng/bảo toàn tham chiếu.
- [ ] Hoàn thiện policy các skill đã chốt; giữ version đã sử dụng, không đổi bài cũ theo ngưỡng mới nhất.
- [ ] Service ghi Profile: kiểm kết quả thật/newest/floor checkpoint và chống áp dụng lặp; test với mock kết quả Dương trước khi engine hoàn chỉnh.

### Bước 3 — Làm luồng AI độc lập rồi nối Dương

- [ ] Tiếp nhận grading service sẵn có, dùng provider HTTP mock và submitted-answer DTO kiểm thử để phát triển không cần trang thi/API key thật.
- [ ] Tác vụ đánh giá lưu bền, claim/idempotency, validate response, timeout/lỗi/tắt/retry có kiểm soát; lưu NULL thay vì 0, không gọi mạng trong transaction DB.
- [ ] Ghi assessment theo schema/writer đã chốt; bản cũ trả chậm không đè version mới; thông báo sau commit.
- [ ] Phản hồi tự luận chi tiết/fragment và form retry có CSRF/owner/admin; ghi rõ Speaking qua transcript chưa đánh giá phát âm, không trang chấm mẫu Admin.
- [ ] **Nối Dương:** bài đã khóa → assessment → Dương quy đổi/tổng hợp. Lỗi áp kết quả chỉ retry áp, không gọi provider lại.

### Bước 4 — Tích hợp và nghiệm thu

- [ ] Nối progress Thảo/history Dương vào admin học viên; cùng Trâm test Placement → Profile, cùng Thảo test checkpoint → Profile.
- [ ] Test provider lỗi/response sai/retry đồng thời/crash, khóa session, ownership, band/version lịch sử; cập nhật route/tài liệu và PR nghiệm thu.

**Không nhận thêm:** engine/recorder, Placement orchestration, trang kết quả toàn bài hoặc tự ghi trạng thái attempt/EXAM_SKILL_RESULT.

## 2. Thảo — bắt đầu bằng một bài học có lưu tiến độ

### Bước 1 — Mạch học nhỏ độc lập

- [ ] Map Lesson/Flashcard và progress, dùng User hiện có, không tạo User mới.
- [ ] Admin tạo/xem/sửa/vô hiệu hóa một Lesson và thẻ; validate skill/CEFR/thứ tự, bảo toàn nội dung đã học.
- [ ] Học viên mở bài, hoàn thành/ôn thẻ có lưu bền; kiểm ACTIVE/ownership, gửi lặp/refresh không đếm trùng.
- [ ] Test quyền và dữ liệu trống; không bật upload thật khi helper/quyền media chưa đủ, có thể làm nội dung văn bản trước.

### Bước 2 — Lộ trình và summary

- [ ] Map path/items; test rule bằng context mock có/không Goal/Placement, chưa cần bài thi thật hoặc AI.
- [ ] Tạo path định hướng/cá nhân, vị trí/bài bắt buộc/tùy chọn, trạng thái thiếu nội dung.
- [ ] Đồng bộ completion với item; C/N/làm tròn xuống theo REAL_PROGRESS_CONTRACT; N=0 trả NULL, bài tiếp theo có thể NULL.
- [ ] Đổi Goal archive path cũ/tạo mới, giữ completion hợp lệ và lịch sử.
- [ ] **Bàn giao Thông:** progress summary/bài tiếp theo và ca rỗng/N=0. **Bàn giao Trâm:** ensurePaths theo context và quy tắc cập nhật sau Placement.

### Bước 3 — Nối dashboard và checkpoint

- [ ] Nối context thật từ Thông, hoàn thiện tổng quan/lộ trình/Bài học; CTA theo trạng thái Goal/path, không bắt buộc đặt Goal/đầu vào.
- [ ] Dùng ExamCatalog của Trâm chọn checkpoint đúng skill/target, kiểm bài bắt buộc trước khi gọi engine Dương.
- [ ] Test checkpoint đạt/trượt/chưa chấm với mock; sau đó nối kết quả thật, gọi Profile Thông và đóng/mở chặng idempotent.
- [ ] Nối helper media Trâm và endpoint đúng quyền cho tài nguyên bài học nếu cần.

### Bước 4 — Nghiệm thu

- [ ] Test đổi Goal, hoàn thành lặp, thiếu bài/đề, checkpoint trả chậm, crash giữa nâng Profile/chuyển chặng; không tăng CEFR theo phần trăm.
- [ ] Cùng Thông kiểm admin/student dùng cùng summary; dữ liệu còn sau đăng nhập lại, có test SQL Server cần thiết và cập nhật tài liệu.

**Không nhận thêm:** engine checkpoint riêng, quy đổi điểm hoặc tự ghi Profile.

## 3. Trâm — bắt đầu bằng đề khách quan nhỏ, không chờ CRUD hoàn chỉnh

### Bước 1 — Đề nhỏ cho Dương

- [ ] Chốt DTO câu/item/option/media an toàn với Dương; đáp án/grading context chỉ backend.
- [ ] Map Question→Item→Option và Exam/Question/Section, tạo bộ câu/đề nhỏ theo seed hoặc test fixture đúng quy trình; trước mắt có thể bắt đầu SINGLE_CHOICE.
- [ ] Chọn Certificate/scheme từ service Thông; khi chưa có implementation thì mock contract trong test, không tạo bảng/entity hoặc policy thay thế.
- [ ] Publish đề nhỏ hợp lệ, có thời gian, skill/type và policy version; kiểm scheme đúng certificate/status/phạm vi khi ghi/publish.
- [ ] **Bàn giao Dương:** đề, item/option DTO, grading context backend, version và đáp án dự kiến. Không cần đợi màn quản trị đẹp/đủ mọi dạng câu.

### Bước 2 — Hoàn thiện ngân hàng câu hỏi và đề

- [ ] CRUD/validation đủ SINGLE_CHOICE, MATCHING, ORDERING, WRITING_TEXT, SPEAKING_AUDIO; option đúng item, ordering hợp lệ, tự luận không ép option.
- [ ] Media đề/helper MIME thực/dung lượng/path an toàn; bàn giao helper cho Thảo/Dương, không tự làm hai lượt nghe của engine.
- [ ] Form câu hỏi/đề/section, position, tái sử dụng câu; từ chối đề rỗng/sai cấu trúc và scheme không phù hợp.
- [ ] Cùng Dương bảo toàn version/snapshot câu/đề đã dùng; không sửa đáp án làm đổi lịch sử.
- [ ] Đủ bốn loại đề theo V2; dashboard admin lấy số liệu thật từ service các đầu mối.

### Bước 3 — Placement

- [ ] Tiếp nhận/tách trang Placement khỏi preview hồ sơ Thông; giới thiệu/chọn/bỏ qua, CTA theo trạng thái.
- [ ] Test orchestration bằng mock engine/Profile/path; chọn kết quả hợp lệ mới nhất theo submittedAt/id, không chọn max score hoặc thời gian AI trả về.
- [ ] Gọi start/resume Dương; chờ chấm không tạo lượt mới cùng skill; việc chặn concurrent do engine thực thi.
- [ ] Kết quả hợp lệ → gọi Profile Thông → ensurePaths Thảo; có dấu xử lý bền để retry/crash không làm mất bước cập nhật path.

### Bước 4 — Nghiệm thu

- [ ] Nối engine/context/policy thật; test skip/resume/retake/pending/thiếu đề/scheme ngừng dùng.
- [ ] Test Placement thấp hơn checkpoint không hạ Profile, kết quả cũ trả chậm không đè kết quả mới, không reset progress/lịch sử.
- [ ] Test publish/quyền/media/version, hoàn thiện seed và tài liệu.

**Không nhận thêm:** CRUD Certificate/Scheme/Band, ScoringPolicyContract, AI provider hoặc tự ghi Profile/path/điểm thi.

## 4. Dương — bắt đầu bằng start/save/submit khách quan, chưa làm AI ngay

### Bước 1 — Engine nhỏ độc lập

- [ ] Chốt attempt state, answer DTO, deadline/requestKey và version với nhóm; map attempt/answer/detail/media activity/skill result, không tạo lại entity nội dung đề.
- [ ] Dùng catalog/scoring mock trong test để làm start → hiển thị câu đơn → lưu/khôi phục đáp án; kiểm owner/ACTIVE, không lộ đáp án đúng.
- [ ] Deadline backend, reload không reset; submit lưu/khóa bài, không sửa sau nộp/hết giờ; chống start/submit concurrent và gửi lặp.
- [ ] Chấm khách quan mạch nhỏ; gọi policy Thông, lưu kết quả và dựng trang kết quả tối thiểu.
- [ ] **Bàn giao Thông:** submitted-answer context, writer AI nếu dùng schema cũ, version và quyền đọc. **Bàn giao Thảo/Trâm:** result DTO/state và thông báo kết quả sau commit.

### Bước 2 — Nối đề/policy thật và mở rộng engine

- [ ] Nhận đề nhỏ Trâm + policy Thông, chạy mạch thật đề → làm/nộp → quy đổi → kết quả trước khi cần AI.
- [ ] Hoàn thiện năm dạng câu và bốn loại đề qua cùng engine; chống option sai item/câu ngoài đề.
- [ ] Listening endpoint theo attempt/media, tối đa hai lượt qua reload, chốt Range/retry/giờ/quyền; không URL tĩnh để vượt giới hạn.
- [ ] Speaking recorder/upload/phát lại, gắn owner/attempt/answer, dùng helper Trâm; không nhận path tùy ý.
- [ ] Hoàn thiện danh sách/chi tiết đề, lịch sử/kết quả Student/Admin, filter/page/pending/error; service đọc kiểm owner/admin cho các bên dùng.

### Bước 3 — Nối AI và các luồng đánh giá

- [ ] Bài tự luận đã khóa → tạo yêu cầu bền → gọi AI Thông sau commit; không gọi OpenAI trực tiếp.
- [ ] Nhận assessment/version hợp lệ, quy đổi qua policy Thông và áp một lần; điểm chưa chấm NULL. Duy nhất engine đổi attempt state/EXAM_SKILL_RESULT.
- [ ] Gắn phản hồi AI Thông vào kết quả, retry qua service có quyền; lỗi áp assessment không gọi provider lại.
- [ ] Placement Trâm/checkpoint Thảo dùng chung start/resume/submit/result; kiểm một Placement chưa kết thúc/user/skill trên mọi đề.

### Bước 4 — Nghiệm thu

- [ ] Test hai tab/start đồng thời, hết giờ/submit lặp, reload/hai lượt nghe, upload/phát sai quyền.
- [ ] Test AI pending/lỗi/tắt/response cũ/sự kiện trùng/crash, bảo toàn policy lúc bắt đầu; không tự nâng Profile từ Practice/Full Mock.
- [ ] Kết quả/lịch sử lưu bền, owner/admin đúng; đủ dữ liệu cho Placement/checkpoint và cập nhật tài liệu.

**Không nhận thêm:** CRUD nội dung/scoring, provider/retry mạng AI, tự ghi Profile/path.

## 5. Mốc phối hợp chung — dùng để kiểm tiến độ nhóm

| Mốc | Thành viên và đầu ra cần chạy thật | Chưa cần chờ |
|---|---|---|
| M1: Thi khách quan nhỏ | Thông policy → Trâm đề → Dương start/save/submit/kết quả | AI, Placement hoàn chỉnh, toàn bộ CRUD |
| M2: Học và progress | Thảo học/path/summary ↔ Thông context/admin học viên | AI và Full Mock |
| M3: Tự luận | Dương khóa bài → Thông assessment → Dương tổng hợp/phản hồi | Key thật để test: dùng provider mock, sau đó smoke test riêng nếu bật |
| M4: Placement | Trâm điều phối engine Dương → Profile Thông → path Thảo | Có thể nối kỹ năng khách quan trước, vẫn phải test pending AI sau |
| M5: Checkpoint | Thảo điều kiện → đề Trâm/engine Dương → Profile Thông → chặng tiếp Thảo | Không dựng engine thứ hai |
| M6: Nghiệm thu chung | Khóa session, ownership, version, concurrency, crash/retry, SQL Server, UI và clone mới | Không được thay bằng fixture để báo hoàn tất |

- [ ] Merge PR nhỏ sau từng mốc, các nhánh cập nhật theo Git workflow; không đợi cuối dự án mới ghép.
- [ ] Contract đổi phải báo bên dùng và sửa test cùng PR. Khi chờ implementation, tiếp tục test/mock hoặc nhiệm vụ độc lập trong checklist của mình.
- [ ] Chỉ đánh dấu xong sau khi có code/test và đầu ra kiểm tra được; tài liệu này chưa xác nhận mốc nào đã hoàn thành.
