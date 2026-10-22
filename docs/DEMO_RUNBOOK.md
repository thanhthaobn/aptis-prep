# Runbook demo AptisPrep

Tài liệu này dùng cho buổi demo chạy trực tiếp, theo thứ tự ít rủi ro nhất.
Không xóa DB, sửa điểm bằng SQL hoặc dùng dữ liệu người thật để làm lại demo.

## 1. Chọn phạm vi demo

### Không dùng OpenAI — đủ cho demo nghiệp vụ chính

Không cần API key để trình bày đăng nhập, quản trị bài học, media, flashcard,
lưu lịch sử Writing/Speaking, nghe Listening, cây bài học, hoàn thành bài,
checkpoint khách quan và mở A2. Đây là phương án nên dùng làm kịch bản chính.

### Có dùng OpenAI — phần bổ sung

Cần API key riêng khi muốn bấm **Yêu cầu AI nhận xét** cho bài Writing/Speaking
hoặc chấm câu tự luận trong đề thi. API có chi phí và billing riêng với ChatGPT.
Không đưa key lên màn hình trình chiếu, Git, JavaScript hoặc tài liệu.

## 2. Cấu hình OpenAI an toàn

1. Tạo key tại `https://platform.openai.com/api-keys` và bảo đảm tài khoản API
   có billing/quota. Không gửi key qua chat.
2. Trong IntelliJ: **Run → Edit Configurations → AptisPrepApplication**.
3. Thêm Environment variables:

   ```text
   OPENAI_API_KEY=sk-...;
   ```

4. Vì profile dev mặc định tắt AI, mở file bị gitignore
   `src/main/resources/application-dev.properties` và thêm:

   ```properties
   app.ai.enabled=true
   app.ai.api-key=${OPENAI_API_KEY:}
   app.ai.grading-model=gpt-4.1-mini
   app.ai.transcription-model=gpt-4o-mini-transcribe
   ```

5. Stop process Java rồi Run lại. Chỉ refresh trình duyệt là chưa đủ.
6. Không chụp màn hình Environment variables. Trước khi share project, kiểm tra
   `git status` để chắc `application-dev.properties` không được track.

Nếu không demo AI, dùng `app.ai.enabled=false`. Ứng dụng vẫn chạy và không tạo
điểm giả. Hiện máy này chưa có API key được cấu hình.

## 3. Kiểm tra trước buổi demo

1. Chạy SQL Server và ứng dụng; đợi log có `Started AptisPrepApplication`.
2. Mở `http://localhost:8080` và `/login`; cả hai phải tải được.
3. Mở cửa sổ thường cho Admin và cửa sổ ẩn danh cho Student để không trùng cookie.
4. Tài khoản local hiện dùng:

   - Admin: `admin@aptis.local` / `Password@123`
   - Student: `student@aptis.local` / `Password@123`

5. Kiểm tra micro trên Chrome/Edge tại localhost trước buổi trình bày.
6. Không chạy `mvn clean` ngay trước demo nếu media mẫu đang nằm trong `target`.
7. Backup cả DB và thư mục `uploads`; DB chỉ lưu đường dẫn media.

## 4. Dữ liệu đã có sẵn

| Kỹ năng | Bài | Flashcard | Checkpoint A1 |
| --- | ---: | ---: | ---: |
| Grammar & Vocabulary | 41 | 31 | 15 |
| Reading | 43 | 33 | 17 |
| Listening | 42 | 32 | 16 |
| Writing | 40 | 35 | 19 |
| Speaking | 44 | 34 | 18 |

Mở trực tiếp bài bằng `/student/lessons/{id}` hoặc qua **Bài học → kỹ năng →
A1 → Thư viện và câu lạc bộ**. Không tạo thêm bài mang tên Demo/QA nếu chỉ cần
trình bày luồng có sẵn.

Đáp án nhanh:

- Grammar 41: A, B, B.
- Reading 43: A, B, A.
- Listening 42: A, A, A.
- Checkpoint Reading 17: A, B, A, A, A.

## 5. Kịch bản demo trơn tru 25 phút

### Luồng 1 — Admin và nội dung công bố (3 phút)

1. Admin mở **Quản trị → Bài học**, tìm bài Reading 43.
2. Chỉ ra kỹ năng, A1, chủ đề, bài đọc, câu hỏi, đáp án và giải thích.
3. Mở bộ thẻ 33 để chứng minh flashcard được gắn đúng bài/kỹ năng/cấp độ.
4. Không sửa bài đang dùng giữa buổi demo. Nếu cần trình diễn Draft/Archive,
   dùng một bản sao riêng để không ảnh hưởng lộ trình.

Kỳ vọng: Student chỉ thấy bài PUBLISHED; bộ thẻ theo bài nằm đúng Reading/A1.

### Luồng 2 — Reading, lịch sử và hoàn thành (4 phút)

1. Student mở bài 43.
2. Lần đầu chọn A, A, A để có một câu sai; nộp và xem giải thích.
3. Lần hai chọn A, B, A; nộp đạt 100%.
4. Mở lịch sử để thấy hai lượt độc lập.
5. Bấm **Hoàn thành bài học**, tải lại trang.

Kỳ vọng: trạng thái hoàn thành và cả hai lượt vẫn còn sau tải lại.

### Luồng 3 — Listening không lộ đáp án (3 phút)

1. Mở bài 42; phần đầu là **Trước khi nghe**, không có ảnh chứa đáp án.
2. Phát audio, chọn A/A/A và để audio chạy đủ ít nhất 90%.
3. Nộp câu hỏi; mở transcript khi đủ điều kiện.
4. Bấm hoàn thành.

Kỳ vọng: lựa chọn không mất khi audio kết thúc; transcript không xuất hiện sớm.

### Luồng 4 — Writing và phản hồi AI tùy chọn (4 phút)

Nhập:

```text
Hi Sam, would you like to join the library reading club this Saturday at ten
in the morning? Please bring your library card and your favourite book.
I hope to see you there!
```

1. Chờ trạng thái tự lưu rồi tải lại để chứng minh bản nháp còn.
2. Bấm **Lưu bài viết vào lịch sử**.
3. Mở lịch sử; đối chiếu đề tại thời điểm nộp và nguyên văn bài viết.
4. Nếu AI đã bật, bấm **Yêu cầu AI nhận xét**, chờ trang đánh giá hiển thị
   COMPLETED, điểm 0–50, CEFR và góp ý tiếng Việt.
5. Nếu AI chưa bật, bỏ bước 4; việc lưu bài và hoàn thành vẫn hoạt động.

Kỳ vọng: lỗi/quota AI không được ghi thành điểm 0; có thể dùng nút retry ở trang
đánh giá sau khi sửa cấu hình.

### Luồng 5 — Speaking, micro và lịch sử (4 phút)

Đọc:

```text
The library opens at nine in the morning. The reading club meets on Saturday
at ten. I should bring my library card. Membership is free.
```

1. Bấm ghi âm, cho phép micro, nói 5–10 giây, dừng và nghe lại.
2. Bấm **Lưu bản ghi**, tải lại, phát bản ghi từ lịch sử.
3. Nếu demo AI, nhập đúng lời vừa nói vào ô transcript và lưu transcript vào
   lịch sử; sau đó bấm **Yêu cầu AI nhận xét**.
4. Nói rõ AI ở luồng bài học đánh giá transcript, không khẳng định chấm phát âm.

Kỳ vọng: không có audio thì không hoàn thành Speaking; transcript không thay thế audio.

### Luồng 6 — Flashcard (2 phút)

1. Mở **Flashcard → Theo bài học**.
2. Mở kỹ năng Reading → A1 → bộ 33, lật thẻ và đánh dấu trạng thái ôn.
3. Chuyển **Của tôi**, tạo bộ cá nhân để chứng minh hai nguồn dữ liệu tách nhau.

Kỳ vọng: bộ theo bài được nhóm kỹ năng/cấp độ và có thể thu gọn; bộ cá nhân chỉ
thuộc tài khoản đang đăng nhập. Flashcard không bắt buộc để hoàn thành bài.

### Luồng 7 — Checkpoint Reading A1 lên A2 (5 phút)

1. Hoàn thành tất cả bài Reading A1 bắt buộc được liệt kê trong lộ trình.
2. Mở **Lộ trình → Reading**, kiểm tra cuối chặng hiển thị đủ điều kiện.
3. Bắt đầu checkpoint từ lộ trình, không gọi URL start trực tiếp.
4. Lần đạt chọn A/B/A/A/A và nộp.
5. Trở lại lộ trình: Profile xác nhận A1, chặng A1 hoàn tất, A2 được mở nếu mục
   tiêu cao hơn A1. Chưa đặt mục tiêu thì đích mặc định là C2.

Muốn minh họa trượt: lần đầu chỉ đúng 2/5 (40%), xác nhận vẫn ở A1; lần sau đúng
ít nhất 3/5 (60%). Checkpoint A1 chỉ xác nhận tối đa A1, không nhảy thẳng C2.

## 6. Cách nhập một bài mới nếu giảng viên yêu cầu

Trong Admin chọn **Bài học → Tạo bài học**:

1. Chọn đúng skill và CEFR trước; nhãn form sẽ đổi theo skill.
2. Nhập tên, chủ đề, thứ tự không trùng, mục tiêu/nội dung và trạng thái Draft.
3. Grammar/Reading/Listening: mỗi câu phải có 2–6 lựa chọn liên tiếp, đáp án đúng
   và giải thích. Listening phải có audio; ảnh không được chứa đáp án.
4. Writing: nhập đề thực hành và giới hạn từ. Speaking: nhập đề nói; học viên sẽ
   tạo audio ở phía Student.
5. Media mỗi bài: tối đa một ảnh 8 MB, một audio Listening 20 MB, một video
   MP4/WEBM 50 MB; tổng request 80 MB.
6. Lưu Draft, dùng **Xem trước như học viên**, thử media, rồi mới PUBLISHED.
7. Tạo bộ flashcard, chọn skill/level và gắn Lesson; nhập nhanh mỗi dòng theo
   định dạng mà form đang hướng dẫn, sau đó kiểm tra lại thuật ngữ/nghĩa.

## 7. Checklist đạt/rớt cho từng luồng

| Luồng | Đạt khi | Nếu không đạt |
| --- | --- | --- |
| Startup | Có log Started, `/` và `/login` trả 200 | Kiểm tra SQL Server, port 8080, Flyway |
| Reading/Grammar | Lượt làm và giải thích còn sau reload | Kiểm tra POST, CSRF, DB history |
| Listening | Audio phát, lựa chọn còn, transcript mở đúng lúc | Kiểm tra file uploads và mốc 90% |
| Writing | Draft và submission history tách nhau | Kiểm tra giới hạn từ và nút lưu lịch sử |
| Speaking | Audio lưu/phát lại được trong history | Kiểm tra quyền micro, định dạng, bước Lưu |
| AI | Assessment COMPLETED hoặc ERROR rõ ràng, không điểm giả | Kiểm tra enabled, key, billing/quota, retry |
| Checkpoint | Fail giữ chặng; pass 60% mở cấp kế | Kiểm tra bài bắt buộc và đề đúng skill/level |

## 8. Sau buổi demo

1. Nếu key đã xuất hiện trong ảnh, log hoặc chat, thu hồi key và tạo key mới.
2. Tắt `app.ai.enabled` nếu không tiếp tục dùng để tránh phát sinh chi phí.
3. Không xóa lịch sử demo bằng SQL. Dùng tài khoản mới cho buổi sau hoặc giữ
   lịch sử để chứng minh tính lưu bền.
4. Sao lưu DB và `uploads` cùng nhau.
