# Quy tắc làm lại kiểm tra đầu vào — đã chốt

Đây là quyết định nghiệp vụ cho luồng Placement tổng hợp. Học viên chỉ thấy một đề Placement đã công bố; đề chứa các phần/kỹ năng cần thiết và được chấm theo snapshot của chính lượt thi.

## 1. Số lượt và trạng thái

- Học viên được làm lại Placement sau khi lượt trước đã kết thúc; không giới hạn số lượt và không đặt cooldown.
- Tại một thời điểm, mỗi học viên chỉ có một lượt Placement tổng hợp đang làm hoặc chờ chấm.
- Nếu lượt đang làm hoặc đang chờ chấm, yêu cầu bắt đầu lại sẽ tiếp tục đúng lượt đó, không reset deadline.
- Lượt đã hoàn tất, hết hạn hoặc hủy vẫn được giữ trong lịch sử; có thể tạo lượt mới bằng request key mới.
- Đã nộp và đang chấm/chờ AI vẫn được xem là lượt đang hoạt động; không tạo lượt mới cho đến khi có trạng thái kết thúc.
- Lỗi chấm: giữ bài đã lưu, cho retry phần thất bại trên cùng attempt; không tạo lại bài hoặc bắt học viên trả lời lại. Không gán lỗi chấm thành điểm 0. Khi chưa có kết quả hợp lệ, không áp dụng vào Profile.
- Bấm bắt đầu hai lần/hai tab/hai request đồng thời phải trả cùng lượt hợp lệ hoặc từ chối rõ ràng. Kiểm tra rồi insert ngoài transaction là chưa đủ; cần transaction/khóa/constraint phù hợp SQL Server và requestKey lưu bền.
- Không lọc đề theo từng kỹ năng ở giao diện học viên; server vẫn giữ các overload cũ để tương thích dữ liệu/API cũ.

## 2. Kết quả mới nhất và bảo vệ checkpoint

Tách ba khái niệm theo từng user/skill:

1. **Đánh giá đầu vào gần nhất:** CEFR của lượt Placement hợp lệ mới nhất, không chọn điểm cao nhất qua mọi lượt.
2. **Mức đã xác nhận qua checkpoint:** mức cao nhất đã đạt từ checkpoint hợp lệ; Placement không được xóa/hạ mức này.
3. **Trình độ hiện tại dùng điều hướng:** lấy đánh giá Placement gần nhất nhưng không thấp hơn mức đã xác nhận qua checkpoint. Chưa có Placement mới hợp lệ thì giữ hồ sơ hợp lệ hiện có, không reset về mặc định.

“Mới nhất” sắp theo thời điểm nộp đã lưu ở server, rồi attempt_id để phân xử khi bằng nhau, **không** theo thời điểm AI trả kết quả. Chỉ dùng attempt đúng người/skill/type và kết quả đã chấm hợp lệ theo scheme. Retry kết quả cũ không được ghi đè kết quả Placement mới hơn. Khi có chấm lại hợp lệ của cùng attempt, cập nhật đánh giá của attempt đó idempotent, không tạo thêm lịch sử thi.

So sánh CEFR bằng thứ tự tường minh A1 < A2 < B1 < B2 < C1 < C2, không so chuỗi hoặc quy đổi tùy tiện từ phần trăm bài học. Chưa có điểm/skill không đủ điều kiện chấm thì giữ trạng thái chưa đánh giá, không coi là A1.

| Tình huống | Đầu vào gần nhất | Mức checkpoint | Trình độ dùng điều hướng |
|---|---|---|---|
| Chưa đạt checkpoint, Placement cũ B2 → lượt mới hợp lệ B1 | B1 | Chưa có | B1, không giữ điểm Placement cao nhất |
| Đã đạt checkpoint B2, làm lại Placement được B1 | B1 | B2 | B2, đề xuất ôn tập, không tự hạ |
| Đã đạt checkpoint B1, Placement mới hợp lệ B2 | B2 | B1 | B2 theo quyền cập nhật Placement của V2; không tuyên bố đã đạt checkpoint B2 |
| Đã đạt checkpoint B2, lượt mới chờ/lỗi chấm | Chưa có kết quả mới | B2 | Giữ trình độ hợp lệ hiện có, không áp dụng kết quả lỗi |

Không làm mất căn cứ checkpoint khi source_attempt_id của Profile đổi sang Placement. Schema hiện chỉ có current_level/source_attempt_id nên Người 1/2/3 phải thống nhất cách lưu/truy xuất mức checkpoint từ lịch sử hợp lệ hoặc migration mới có dấu vết nguồn. Không chỉ nhìn attempt đang được source_attempt_id trỏ tới. Không sửa V001–V005.

## 3. Lộ trình và giao diện

- Giữ toàn bộ lịch sử bài làm, kết quả, lesson_progress và lượt ôn Flashcard; không xóa/reset khi làm lại Placement.
- Khi kết quả thấp hơn checkpoint: thể hiện riêng đánh giá đầu vào gần nhất và mức checkpoint đã đạt; có thể đề xuất bài ôn, không tự hạ trình độ hoặc bắt học lại toàn bộ chặng đã hoàn tất.
- Điều chỉnh lộ trình theo REAL_PROGRESS_CONTRACT: tạo phiên bản/path mới khi cần, lưu trữ path cũ, kế thừa bài đã hoàn thành hợp lệ. Không tự sửa mẫu số hoặc xóa completion trên path cũ.
- UI Placement có các hành động theo trạng thái: bắt đầu, tiếp tục bài đang làm, xem trạng thái chấm, chấm lại phần lỗi, làm lại sau lượt kết thúc. Nút disabled không thay kiểm tra server.
- Hiện nguồn đánh giá khi có dữ liệu thật: “Đầu vào gần nhất”, “Đã đạt qua kiểm tra cuối chặng”; không ghi trình độ hiện tại thấp hơn mức checkpoint chỉ vì lượt mới thấp điểm.
- Practice/Full Mock vẫn không tự nâng/hạ Skill Profile. Quy tắc trên chỉ dành cho Placement và bảo vệ checkpoint.

## 4. Phân công

| Thành viên | Phần phải thực hiện | Kiểm thử bàn giao |
|---|---|---|
| Người 1 — đầu mối ghi Profile và AI | Xác minh kết quả đầu vào/newest/floor khi ghi Skill Profile; giữ dấu vết nguồn; tác vụ chấm tự luận/retry AI, không điều phối trang Placement | Không hạ checkpoint/áp lặp; dữ liệu thiếu/chờ/lỗi; ownership; retry AI và bản trả về cũ |
| Người 2 | Cung cấp căn cứ checkpoint đã đạt theo skill; điều chỉnh path/đề xuất ôn tập, không xóa tiến độ cũ; dashboard tách mục tiêu/trình độ/đầu vào | Checkpoint B2 + Placement B1 vẫn B2; path cũ còn lịch sử; kế thừa completion; không tăng/giảm lặp |
| Người 3 — đầu mối Placement/engine | Đề/scheme có version; trang chọn/skip/resume; chọn kết quả mới nhất hợp lệ; điều phối engine 3 → grading/result 1 → Profile 1 → path 2, dấu xử lý bền | Đề đúng phạm vi; không lộ đáp án; pending/retake/thiếu đề; kết quả trả chậm, crash giữa Profile và path |

Người 1/2/3 chốt trạng thái terminal/non-terminal trước khi nối UI; cả ba review contract/migration áp kết quả. Người 3 điều phối Placement và duy nhất ghi lifecycle attempt; Người 1 duy nhất ghi result/skill result/Profile; Người 2 ghi path/UI. Không tự định nghĩa khác nhau cách bảo vệ checkpoint. Interface hiện có tối thiểu: mở rộng DTO Placement/mức checkpoint trong PR chung; không truyền CEFR tùy ý từ FE vào applyAssessment. Phân công mới theo TEAM_ASSIGNMENT_V2.md thay phân công cũ.

## 5. Checklist nghiệm thu

- [ ] Làm lại nhiều lần, chỉ có một lượt đang làm/chờ chấm của cùng skill.
- [ ] Hai request start đồng thời không tạo hai lượt, reload không tăng giờ.
- [ ] Resume, hết hạn và chờ AI được phân biệt đúng; lỗi chấm retry trên cùng bài đã lưu.
- [ ] Đầu vào gần nhất không chọn điểm cao nhất, retry cũ không ghi đè lượt mới.
- [ ] Checkpoint B2 không bị hạ bởi Placement B1; đánh giá đầu vào B1 vẫn được lưu/hiển thị riêng.
- [ ] Không checkpoint: Placement mới hợp lệ B1 có thể thay đánh giá Placement cũ B2.
- [ ] Mỗi kết quả chỉ áp dụng một lần, có dấu vết bền và bảo toàn lịch sử/progress.
- [ ] User/skill khác không bị ảnh hưởng, tài khoản khóa không bắt đầu/nộp/chấm lại.
