# Chứng chỉ & Chấm điểm — Người 1 tiếp nhận

Phân công mới nhất: chuyển trọn UC-A03–A05 từ Người 3 sang Người 1, gồm giao diện, Entity/Repository, validation, service đọc và ScoringPolicyContract. Người 3 chọn/tham chiếu chứng chỉ-scheme, sở hữu engine và gửi context đã khóa; Người 1 chấm khách quan/AI, quy đổi và lưu kết quả/Profile. Người 2 nhận DTO để dựng history/result UI. Xem TEAM_ASSIGNMENT_V2.md và SERVICE_CONTRACTS.md; chưa triển khai nghiệp vụ trong lần sửa tài liệu này.

## Hiện có: chỉ giao diện

Thay mục Cấu hình bằng Chứng chỉ & Chấm điểm, theme teal/font chung/tiêu đề không khung. `/admin/settings` redirect sang `/admin/certificates`; không còn trạng thái Gemini hay chỉnh cấu hình kỹ thuật ở trang này.

AdminSettingsPreviewController → ScoringPreviewService (fixture 9101/9201) → admin/settings/index, certificate, scheme. Không đọc/ghi DB, không có POST, không gọi AI. Nút lưu/ngừng sử dụng/xóa disabled. Các số hiển thị chỉ phục vụ bố cục, không tự nối vào engine và không phải ngưỡng Aptis chính thức.

| GET | Nội dung |
|---|---|
| `/admin/certificates`, `/admin/scoring-schemes` | Danh sách chứng chỉ và bộ chấm điểm |
| `/admin/certificates/create` | Tạo chứng chỉ |
| `/admin/certificates/9101`, `/admin/certificates/9101/edit` | Xem/sửa chứng chỉ |
| `/admin/scoring-schemes/create` | Tạo bộ chấm điểm |
| `/admin/scoring-schemes/9201?skill=Reading` | Xem scheme/band theo kỹ năng |
| `/admin/scoring-schemes/9201/edit?skill=Reading` | Khung sửa scheme/band |

## Cách hiểu

- Certificate là loại/chương trình thi (Aptis ESOL General), không phải giấy chứng nhận cấp học viên.
- Scoring Scheme là bộ quy tắc có phiên bản thuộc Certificate. Đề gắn scheme để xác định cách chấm.
- Score Band là khoảng **scale score** theo skill trong scheme → CEFR. Không lấy phần trăm bài học hoặc raw score của mọi skill để tra chung.
- Gemini hỗ trợ chấm Writing/Speaking, không phải Certificate/Scheme/Band. Người 1 nối provider/đánh giá AI, quy đổi và tổng hợp; Người 3 gửi bài đã khóa; Speaking qua transcript không đánh giá phát âm/ngữ điệu.

## Người 1 triển khai UC-A03–A05

1. Map certificates/scoring_schemes/score_bands theo V2, thay fixture và tiếp nhận route; không tạo mapping trùng.
2. Certificate: name/code/description/status; code duy nhất, created_by từ admin principal. Xem/sửa/disable; đang được tham chiếu thì không hard delete.
3. Scheme: certificate hợp lệ, name/version/status/effective_from; version duy nhất trong certificate. Scheme đã dùng phải giữ căn cứ lịch sử, tạo version mới thay vì sửa điểm cũ. Map trạng thái thực tế sang nhãn tiếng Việt.
4. Band: thêm/sửa/xóa khi hợp lệ, đúng scheme/skill/CEFR; min ≤ max, decimal theo schema; không chồng lấn kể cả biên. Với khoảng đóng hai đầu, hai khoảng không chung endpoint. Xử lý thiếu band rõ ràng, không bịa điểm.
5. Grammar & Vocabulary chưa có band trong fixture, không mặc định có CEFR như Reading. Chốt policy riêng với Người 3 theo đặc tả nhóm trước khi bật quy đổi.
6. Schema hiện không có bảng raw→scale chuyên biệt. Người 1/3 chốt công thức/mapping theo skill/scheme/version và test; nếu cần thì migration mới. UI band chỉ là scale→CEFR, không coi raw→scale đã hoàn tất.
7. Người 1 triển khai ScoringPolicyContract cho context của Người 3 và service đọc/kiểm Certificate-Scheme cho Người 3. ExamCatalogContract thuộc Người 3, không chuyển sang Người 1. Đề phải tham chiếu scheme đúng certificate/version, không lộ marking key vào HTML.
8. POST MVC + CSRF, validation trên form, redirect sau lưu; không frontend REST. Quyền ADMIN cả đọc/ghi. Không sửa V001–V005.

## Người 3 phối hợp

Nhận scheme/version khi tạo attempt và giữ căn cứ chấm lịch sử; gửi `LockedAttempt`/grading context sau khi khóa bài. Không tự dùng version mới nhất để tính lại bài cũ và không tự ghi điểm. AI score chưa mặc định là raw/scale/CEFR cuối: Người 1 ánh xạ theo policy đã chốt, chưa chấm/lỗi giữ NULL. Không đưa CEFR AI trực tiếp vào Profile bỏ qua checkpoint policy.

## Người 3 phối hợp và thứ tự bàn giao

Người 1 bàn giao ngay đầu đợt một Certificate/scheme cố định + DTO khả dụng/validator + test quy đổi cho 3, chưa cần hoàn thiện các form. Người 3 dựng chọn scheme ở đề, kiểm lại khi ghi/publish; không truy cập repository hoặc sửa band. Người 3 dùng mock ScoringPolicy trong test trước khi có implementation, sau đó nối policy thật. Mạch tích hợp đầu: policy 1 → đề/engine 3 → grading/kết quả 1 → history/result UI 2. Không đưa bean policy giả vào cấu hình chạy thật. Bàn giao version đã dùng/disable/history cùng PR, không chờ đến cuối.

## Nghiệm thu nghiệp vụ

Test duplicate code/version, min/max, band trùng/biên/thiếu, rounding, disable/reference và bảo toàn lịch sử theo version. Test phân quyền/CSRF, form lỗi và dữ liệu rỗng. Những điều kiện này chưa thực thi bởi UI preview. Không thêm form API key hoặc trang chấm bài riêng cho admin.
