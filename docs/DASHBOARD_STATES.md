# Tổng quan theo mục tiêu và trạng thái học tập

## Hiện có

Dashboard vẫn là UI cố định. Trang `/student/orientation` đã nối nút Reading/Listening/Writing/Speaking/Grammar & Vocabulary sang `/student/lessons?skill=...`; dùng bộ lọc GET sẵn có. Writing/Speaking có lựa chọn riêng, không đưa sang đề thi hoặc tự tạo attempt. Grammar & Vocabulary gồm bài học và Flashcard theo skill, chưa có lọc riêng loại nội dung. Nếu chưa có bài thì hiển thị trạng thái không tìm thấy, không mượn bài kỹ năng khác để lấp chỗ trống. Các mốc trên dashboard vẫn dẫn tới khu vực tương ứng của trang khám phá, rồi mở danh sách bài đã lọc.

Các liên kết này không sinh path, không ghi progress và chưa tự thay nội dung sau khi đặt mục tiêu.

## Quy tắc hiển thị cần triển khai

Đọc trạng thái từ DB qua service, không dựa trên chữ nút, URL, session flag hay biến JavaScript. Goal/Placement có thể bỏ qua. Ưu tiên trạng thái thực tế theo bảng; có path hợp lệ thì hiển thị hành trình đó kể cả chưa đặt mục tiêu.

| Trạng thái | Khối chính | Hành động |
|---|---|---|
| Chưa Goal, chưa path | Khám phá trước, chọn mục tiêu sau | Khám phá lộ trình; đặt mục tiêu là tùy chọn |
| Có Goal, chưa path | Mục tiêu của bạn: lấy từ Goal | Mở trang lộ trình; nếu chưa có nội dung, báo chưa có bài phù hợp |
| Có path, chưa học | Bắt đầu hành trình | Bài đầu tiên được phép học trong path |
| Có path, đã học, còn bài | Tiếp tục học | Bài tiếp theo từ summary của đúng path |
| Hoàn thành bài bắt buộc, checkpoint khả dụng | Sẵn sàng kiểm tra cuối chặng | Mở giới thiệu checkpoint, không tạo lượt thi bằng GET |
| Đang chờ chấm checkpoint | Đang chờ kết quả | Xem trạng thái; vẫn có thể ôn bài |
| Đã hết bài, checkpoint chưa đạt | Ôn tập để thử lại | Xem kết quả/gợi ý, không tự nâng CEFR |
| Đã hoàn tất chặng/Goal | Chặng đã hoàn thành / Đã đạt mục tiêu | Xem kết quả/chặng tiếp theo theo nghiệp vụ |

Ưu tiên checkpoint/chặng đã hoàn tất hơn CTA tiếp tục bài khi các trạng thái cùng xảy ra. Phần trăm và điều kiện mở checkpoint là hai chỉ số khác nhau (REAL_PROGRESS_CONTRACT.md). Đạt Goal phải do service nghiệp vụ xác nhận, không suy ra từ 100% bài học.

## Mục tiêu và đầu vào

Làm lại đầu vào theo [PLACEMENT_RETAKE_POLICY.md](PLACEMENT_RETAKE_POLICY.md): hiển thị đánh giá Placement gần nhất riêng với mức đã đạt checkpoint. Không thay trình độ dùng điều hướng bằng mức thấp hơn checkpoint, không xóa tiến độ hoặc đưa học viên về trạng thái mới chỉ vì làm lại.

- Khi có Goal, bỏ câu “Thử học trước, đặt mục tiêu sau”; hiện mục tiêu thật, không hardcode B2.
- Không có Placement vẫn được học theo path định hướng hoặc path theo Goal. Kiểm tra đầu vào là CTA phụ tùy chọn, không khóa trang Bài học.
- Chưa đánh giá thì “Chưa đánh giá”; đã có Skill Profile thì hiện CEFR theo từng skill và nguồn đánh giá. Không coi target CEFR là trình độ hiện tại.
- Người chưa Goal nhưng đã học theo path định hướng vẫn thấy “Tiếp tục học”, không bị đưa về lời chào người mới.
- Đổi Goal tạo/cập nhật path theo contract; dashboard đọc lại sau redirect, không giữ phần trăm cũ từ session. Nếu thiếu nội dung, hiển thị đúng trạng thái thay vì dựng bài tiếp theo giả.
- Không tải được dữ liệu thì báo lỗi tải; không coi lỗi DB là chưa Goal/chưa path. GET tổng quan không được tự tạo path/attempt. Tạo path qua bước nghiệp vụ có ghi dữ liệu, chống lặp.

## Phân công tích hợp

1. Người 1 cung cấp AccountAccessContract.context: user, Goal và Skill Profile; thông báo thay đổi Goal sau commit. Tiếp nhận form Goal/Profile; Placement chuyển sang Người 3 theo phân công mới.
2. Người 2 sở hữu view-model tổng quan và controller/template dashboard: kết hợp context của 1 với LearningPathContract.summary/summaries, chọn đúng path đang xem và CTA theo bảng. Nối bài tiếp theo vào lesson cụ thể, không trỏ danh sách chung khi đã có nextLessonId.
3. Người 3 cung cấp đề công bố đúng skill/target, không để draft xuất hiện/lộ đáp án; điều phối Placement → service Profile 1 → path 2. Nội dung Lesson do Người 2 cung cấp.
4. Người 3 cung cấp attempt metadata/result state đúng user; Người 3 xử lý Placement, Người 2 xử lý checkpoint và history/result UI. Người 1 chấm objective/AI, quy đổi/tổng hợp và ghi Profile; 3/2 gọi service 1 theo nguồn hợp lệ, không tự ghi Goal/Profile/Path ngoài phần của mình.

Interface tối thiểu hiện có chưa có view-model dashboard/implementation. Người 2 bổ sung DTO dashboard trong PR, thống nhất nguồn từng field với Người 1/3; không trộn fixture với dữ liệu thật. Bên đọc không gọi repository của bên khác.

## Kiểm thử nghiệm thu

- Tài khoản mới chưa Goal/Placement mở được khám phá và bài học đúng skill.
- Có Goal chưa Placement: không còn CTA đặt mục tiêu sau; không ép làm Placement.
- Có path định hướng đã học nhưng chưa Goal: vẫn hiện bài tiếp theo và C/N thật.
- Năm liên kết skill lọc đúng; chưa có nội dung hiển thị rỗng, không đổi sang skill khác.
- Đổi Goal, reload/đăng nhập lại: nội dung theo DB mới, không giữ trạng thái cũ.
- Học xong/chờ chấm/trượt/đạt checkpoint hiển thị đúng; không tự tăng CEFR.
- Hai user không thấy mục tiêu/nextLesson/result của nhau. Tài khoản khóa không thực hiện ghi dữ liệu.
