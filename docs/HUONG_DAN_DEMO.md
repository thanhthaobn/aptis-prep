# Hướng dẫn demo AptisPrep — phần bài học, flashcard và lộ trình

> Cập nhật: bộ nội dung tên tự nhiên đã được nhập sẵn. Xem
> [Bộ nội dung thư viện](BO_NOI_DUNG_THU_VIEN.md) để dùng ID và đáp án thực tế.
> Không cần tạo thêm các bài `[DEMO]` theo ví dụ dưới đây. Các bài QA 35–39 đã
> lưu trữ; mục 9 bên dưới chỉ là ghi chép lần kiểm thử trước.

Tài liệu ngày 22/10/2026. Đây là hướng dẫn thao tác và dữ liệu mẫu, không tự
tạo thêm tài khoản/bài học hoặc thay đổi dữ liệu của bạn.

## 1. Phần đã triển khai và giới hạn cần nói đúng

| Phần | Nội dung để giới thiệu |
| --- | --- |
| Nhập bài quản trị | Một form đơn giản, nhãn và trường phù hợp từng kỹ năng; không dùng trình chia khối phức tạp. Có câu hỏi, đáp án đúng, giải thích. |
| Media | Tải trực tiếp một ảnh, một video; Listening có thêm audio. Học viên xem/phát trong bài. |
| Hành động học viên | Grammar/Reading làm câu hỏi; Listening nghe và trả lời; Writing viết/nộp; Speaking ghi âm/lưu. Không bắt nhập ghi chú chung cho mọi kỹ năng. |
| Lịch sử | Giữ nội dung đã nộp và đề lúc nộp. Sửa bài hiện tại không thay ngược đề/đáp án của lượt cũ. |
| Mở đáp án tham khảo | Quản trị chọn thời điểm mở transcript/bài mẫu; nội dung đang khóa không gửi xuống HTML. |
| Cây bài học | Kỹ năng → cấp độ → chủ đề hoặc bài độc lập; có liên kết flashcard gắn bài. Thư viện cho tự học nội dung đã công bố, không chỉ các bài đang được giao. |
| Flashcard | Bộ gắn bài và bộ cá nhân; bộ gắn bài phân theo kỹ năng/cấp độ, có đóng/mở nhóm; nhập từng thẻ hoặc nhập nhanh. |
| Lộ trình | Đồng bộ khi mở trang lộ trình. Chưa đánh giá hiển thị xuất phát A1; chưa đặt mục tiêu dùng đích C2. Chặng hiện tại và mục tiêu cuối là hai khái niệm khác nhau. |
| Tiến độ | Hoàn thành bài khác với đạt CEFR. Muốn qua chặng phải có kết quả checkpoint hợp lệ. |

Đã thử trực tiếp tạo đủ 5 kỹ năng; Grammar/Reading/Listening/Writing đã có
bài nộp/lượt làm thật và đối chiếu SQL Server. Speaking đã kiểm tra form,
ảnh/video; ghi micro thật còn cần thử trên trình duyệt của bạn. Test tự động
đã kiểm tra lưu bản ghi/lịch sử nhưng không thay thế việc thử thiết bị micro.

Không giới thiệu AI là tự chấm phát âm: hiện phản hồi Speaking dựa trên
transcript đã lưu, không tự phiên âm hoặc chấm trực tiếp audio. Nút AI chỉ
hiện khi cấu hình dịch vụ đủ điều kiện. Không cần demo AI trong kịch bản cơ bản.

## 2. Chuẩn bị trước buổi demo

1. Chạy SQL Server, Run ứng dụng một lần trong IntelliJ, mở http://localhost:8080.
   Nếu báo port 8080 in use: dừng bản ứng dụng đang chạy trùng, không sửa DB.
2. Mở hai phiên đăng nhập riêng: cửa sổ thường cho quản trị, cửa sổ ẩn danh
   cho học viên. Hai tab cùng cửa sổ chia sẻ cookie, đăng nhập bên này có thể
   làm bên kia đổi vai trò.
3. Tài khoản demo được cấu hình trong dự án:
   - Admin: `admin@aptis.local` / `Password@123`.
   - Student: `student@aptis.local` / `Password@123`.
   Nếu bạn đã đổi mật khẩu, dùng mật khẩu hiện tại. Không đưa tài khoản demo
   này lên môi trường công khai.
4. Dùng học viên mới nếu muốn trình bày trạng thái chưa có Goal/đánh giá hoặc
   transcript/bài mẫu đang khóa. Học viên demo cũ đã có lượt làm nên nội dung
   có thể đã mở; không xóa DB hay lịch sử để làm lại buổi demo.
5. Chuẩn bị ảnh, video và audio. Có sẵn trong thư mục
   `D:\HSF302_FPT\PROJECT\target\qa-lesson-smoke`:
   - `library-card.png`: ảnh thông báo thư viện.
   - `library-guide.webm`: video hướng dẫn 6 giây.
   - `library-announcement.wav`: lời thông báo khoảng 10 giây.
   Sao chép các tệp cần dùng ra thư mục riêng trước khi `mvn clean`, vì target
   là thư mục build tạm. Không cần chạy file Python để demo ứng dụng.
6. Dùng Chrome/Edge ở `localhost`, chuẩn bị micro và cho phép khi trình duyệt hỏi.

Giới hạn mỗi bài: ảnh 8 MB; audio Listening 20 MB; video MP4/WEBM 50 MB;
tổng tệp một lần lưu 80 MB. Mỗi loại một tệp. So sánh nhiều ảnh thì dùng ảnh
ghép. DB lưu đường dẫn, còn tệp media nằm trong storage — cần giữ cả hai khi
sao lưu/chuyển máy.

## 3. Thứ tự demo gợi ý (20–30 phút)

1. Quản trị nhập Reading, thêm media, câu hỏi và công bố.
2. Học viên tìm bài trong cây, làm sai rồi làm đúng, xem giải thích/lịch sử.
3. Quản trị sửa đề; học viên đối chiếu đề mới với đề cũ trong lịch sử.
4. Listening: khóa transcript → nghe → trả lời → mở transcript.
5. Writing: bản nháp → nộp chính thức → lịch sử → bài mẫu.
6. Speaking: ghi → nghe lại → lưu → tải lại → nghe trong lịch sử.
7. Flashcard: gắn bài, nhập nhanh, học viên mở và tạo bộ riêng.
8. Lộ trình: mục tiêu, tiến độ bài và checkpoint. Chỉ demo chuyển chặng nếu
   đã chuẩn bị đầy đủ nội dung/đề/kết quả chấm.

Grammar dùng cùng luồng chấm trắc nghiệm với Reading nhưng dữ liệu khác.
Có thể nhập sẵn bốn bài còn lại để tiết kiệm thời gian trình bày.

## 4. Dữ liệu quản trị cần nhập — mỗi kỹ năng một bài

Mở Quản trị → Bài học → Tạo bài học. Các trường chung:

| Trường | Giá trị |
| --- | --- |
| Tên | Theo từng bài bên dưới, tiền tố `[DEMO]` |
| CEFR | A1 |
| Giới thiệu | Luyện kỹ năng qua chủ đề thư viện và câu lạc bộ đọc sách. |
| Chủ đề | Thư viện và câu lạc bộ |
| Thứ tự | Giữ số form tự đề xuất để tránh trùng thứ tự cũ |
| Trạng thái | Bản nháp khi nhập, sau đó Đã công bố |
| Ảnh | library-card.png |
| Video | library-guide.webm; không cần thêm YouTube |

Các trường hướng dẫn, phân tích, checklist bổ sung có thể để trống. Khi muốn
demo bài độc lập, tạo một bài khác và để trống Chủ đề, không đổi kỹ năng/cấp độ.

### 4.1. Grammar & Vocabulary

- Tên: `[DEMO] Grammar A1 — Present simple`.
- Kỹ năng: Grammar & Vocabulary. Dạng bài: Ngữ pháp trong ngữ cảnh.
- Cấu trúc / từ vựng và ví dụ:

```text
Use the present simple for routines.
I read every day. She reads every Saturday.
The library opens at nine.
```

- Yêu cầu bài tập: `Choose the correct verb to complete each sentence.`
- Thêm hai câu hỏi:

| Câu hỏi | A | B | Đúng | Giải thích |
| --- | --- | --- | --- | --- |
| She ___ every Saturday. | reads | read | A | She là chủ ngữ số ít nên động từ thêm -s. |
| They ___ to the library on Mondays. | goes | go | B | They đi với dạng nguyên thể go. |

- Gợi ý vận dụng: `Use -s with he, she and it in affirmative sentences.`
- Thời điểm xem: Sau khi nộp bài.

### 4.2. Reading

- Tên: `[DEMO] Reading A1 — Library notice`.
- Kỹ năng: Reading. Dạng bài: Đọc tìm thông tin.
- Bài đọc chính:

```text
The community library opens at 9:00 AM and closes at 5:00 PM.
The reading club meets on Saturday.
Please bring your library card.
```

- Yêu cầu đọc hiểu: `Read the notice and answer the questions.`

| Câu hỏi | A | B | Đúng | Giải thích |
| --- | --- | --- | --- | --- |
| What time does the library open? | 9:00 AM | 10:00 AM | A | The notice says the library opens at 9:00 AM. |
| When does the reading club meet? | Sunday | Saturday | B | The reading club meets on Saturday. |

- Gợi ý đọc hiểu: `Find the time and day in the notice before choosing.`
- Thời điểm xem: Sau khi nộp bài.

### 4.3. Listening

- Tên: `[DEMO] Listening A1 — Library announcement`.
- Kỹ năng: Listening. Dạng bài: Thông báo.
- Bối cảnh trước khi nghe: `You are listening to an announcement at a library.`
- Nhiệm vụ nghe: `Listen for the opening time and the day of the reading club.`
- Tải audio: `library-announcement.wav`.
- Transcript (nhập vào ô transcript, không đưa vào phần bối cảnh):

```text
Welcome to the community library. We open at nine in the morning.
The reading club meets on Saturday. Please bring your library card.
```

- Thời điểm mở transcript: Sau khi nghe ít nhất 90%.
- Dùng hai câu hỏi/đáp án của bài Reading ở trên.
- Audio là bài nghe chính; video chỉ là hướng dẫn, không thay audio.

### 4.4. Writing

- Tên: `[DEMO] Writing A1 — Invite a friend`.
- Kỹ năng: Writing. Dạng bài: Email.
- Đề viết học viên cần nộp:

```text
Write 20–60 words inviting a friend to the library reading club on Saturday.
Mention the time and what your friend should bring.
```

- Dàn ý: `Greeting → invitation → time → things to bring → closing.`
- Số từ tối thiểu: 20. Số từ tối đa: 60.
- Bài viết tham khảo:

```text
Hi Sam, please join me at the library reading club this Saturday at nine
in the morning. Bring your library card and your favourite book.
I look forward to seeing you there.
```

- Thời điểm mở bài mẫu: Sau khi nộp bài.
- Không cần thêm trắc nghiệm cho kịch bản này; bài viết mới là bài nộp chính.

### 4.5. Speaking

- Tên: `[DEMO] Speaking A1 — Describe the library`.
- Kỹ năng: Speaking. Dạng bài: Mô tả ảnh.
- Câu hỏi / đề nói học viên cần ghi âm:

```text
Look at the library notice. Say when the library opens, when the reading
club meets, and what you should bring.
```

- Khung triển khai: `Opening time → reading club day → things to bring.`
- Gợi ý bài nói:

```text
The library opens at nine in the morning. The reading club meets on
Saturday. I should bring my library card.
```

- Thời điểm mở gợi ý: Sau khi nộp bài.
- Không cần trắc nghiệm. Không nhập audio đề như Listening; học viên dùng micro.

## 5. Demo thao tác học viên và kết quả cần thấy

### A. Thư viện, trạng thái bài và cây phân loại

1. Mở Học tập → Bài học, tìm `[DEMO]`.
2. Mở kỹ năng → A1 → Thư viện và câu lạc bộ → chọn bài.
3. Chỉ bài Đã công bố được học viên xem. Để chứng minh, giữ một bài thử ở Bản
   nháp rồi công bố, tải lại danh sách; không cần lưu trữ bài đang dùng để thi demo.
4. Muốn demo lưu trữ: dùng một bài riêng, lưu trữ trong admin rồi tải lại danh
   sách học viên; bài không còn trong thư viện công bố. Không khẳng định lịch sử
   hoặc mẫu số của các chặng cũ sẽ bị xóa theo.

### B. Grammar và Reading: sai → đúng → lịch sử

1. Trước khi làm, chỉ ra chưa đủ điều kiện Hoàn thành và gợi ý chưa mở.
2. Lần 1 chọn A/A: đúng câu đầu, sai câu hai, kết quả 1/2 = 50%.
3. Bấm Kiểm tra đáp án: thấy đáp án đã chọn, đáp án đúng, giải thích; chưa đủ 60%.
4. Lần 2 chọn A/B: 2/2 = 100%; mở lịch sử thấy hai lượt riêng.
5. Bấm Hoàn thành bài học. Nộp đúng không đồng nghĩa đã tự bấm hoàn thành.
6. Tải lại trang: lượt làm và trạng thái hoàn thành phải còn.

### C. Listening: chứng minh quy tắc transcript và lựa chọn không mất

1. Dùng học viên chưa nghe bài này để thấy transcript đang khóa.
2. Phát audio thật, chọn đáp án trong lúc nghe, để audio chạy hết.
3. Lựa chọn vẫn còn. Nộp đáp án rồi mở Xem transcript.
4. Nếu không chọn đáp án trong lúc nghe, trang có thể tải lại khi audio kết
   thúc để hiển thị nội dung vừa mở khóa. Không coi đó là lỗi mất đáp án.
5. Nghe đủ ít nhất 90%, đạt tối thiểu 60% trắc nghiệm rồi bấm Hoàn thành.
6. Không kéo thanh tua đến cuối để trình diễn như đã nghe đủ.

### D. Writing: bản nháp khác với lịch sử

1. Nhập `Hi Sam, come on Saturday.` rồi dừng vài giây: xem trạng thái tự lưu.
2. Tải lại sau khi có xác nhận tự lưu: bản nháp còn, nhưng chưa có lần nộp mới
   trong Bài làm đã lưu. Bài mẫu chưa mở nếu chưa từng nộp trước đó.
3. Bấm Lưu bài viết vào lịch sử với đoạn ngắn này: phải bị từ chối vì dưới 20 từ.
4. Thay bằng đoạn 32 từ ở mục 4.4, bấm Lưu bài viết vào lịch sử.
5. Thấy thông báo đã lưu, một mục lịch sử có thời gian và nguyên văn bài viết;
   bài mẫu mở theo thiết lập. Bấm Hoàn thành bài học.

### E. Speaking: bản ghi là bắt buộc

1. Khi chưa có audio, nút Hoàn thành bị khóa. Nhập transcript không thay audio.
2. Bấm Bắt đầu ghi → cho phép micro → đọc câu ở mục 4.5 khoảng 5–10 giây.
   Chỉ đọc câu mẫu, tránh thu thông tin riêng tư trong buổi demo.
3. Bấm Dừng ghi, nghe thử, rồi Lưu bản ghi. Không bỏ qua bước Lưu.
4. Tải lại trang, phát lại bản ghi, mở Bài làm đã lưu: phải có lượt và audio nghe được.
5. Nếu cần demo transcript, nhập đúng lời vừa nói rồi Lưu transcript vào lịch sử.
   Đây là thao tác riêng, không phải chức năng nhận dạng giọng nói tự động.
6. Bấm Hoàn thành. Khi ghi bản mới, transcript hiện hành có thể bị xóa để tránh
   gắn lời cũ với audio mới; lịch sử của lần nộp cũ vẫn được giữ.
7. Nếu bị kẹt chờ quyền, chưa coi demo thành công: kiểm tra quyền site/micro,
   thiết bị đầu vào và dùng Chrome/Edge tại localhost. Thử trước buổi trình bày.

### F. Sửa đề mà không sửa lịch sử — phần nên ưu tiên trình bày

1. Sau khi học viên đã nộp Reading, sang cửa sổ admin sửa bài đó:
   - Bài đọc: đổi giờ mở cửa từ 9:00 thành 10:00.
   - Câu hỏi giờ mở cửa: đổi đáp án đúng từ A sang B, cập nhật giải thích.
   - Bỏ hoặc thay video nếu muốn chứng minh giữ media lịch sử.
2. Lưu và tải lại bài ở cửa sổ học viên: phần bài hiện tại có đề mới.
3. Mở lượt làm cũ → Đề bài tại thời điểm nộp: vẫn 9:00, đáp án đúng cũ là A,
   lựa chọn và điểm của lần nộp cũ không đổi; video cũ vẫn xem được trong lịch sử.
4. Với Writing, đổi yêu cầu từ Saturday sang Sunday sau khi có bài nộp. Lịch sử
   vẫn giữ yêu cầu Saturday cùng nội dung học viên đã nộp, không biến thành bài Sunday.

## 6. Flashcard: dữ liệu nhập và hai luồng riêng

### Admin tạo bộ gắn bài

1. Mở bài Reading trong admin → Tạo bộ thẻ cho bài, hoặc vào Flashcard → Tạo bộ.
2. Tên: `[DEMO] Library vocabulary`; chọn đúng bài Reading vừa tạo, cấp A1,
   trạng thái Đã công bố.
3. Mở Nhập nhanh nhiều thuật ngữ, dán:

```text
library | thư viện
reading club | câu lạc bộ đọc sách
library card | thẻ thư viện
opening time | giờ mở cửa
bring | mang theo
```

4. Bấm Thêm vào danh sách, kiểm tra hai cột thuật ngữ/định nghĩa.
5. Bấm + Thêm thẻ, nhập thêm `notice` / `thông báo` để trình diễn cả nhập lẻ.
6. Bấm Lưu bộ thẻ. Chỉ bấm Thêm vào danh sách chưa lưu DB.
7. Bên học viên: mở bài Reading thấy bộ đi kèm; vào Flashcard, mở nhóm kỹ năng
   Reading → A1 → bộ vừa tạo. Mở bộ và dùng các thao tác ôn có sẵn.

Giới hạn hiện tại: tối đa 200 thẻ/bộ; thuật ngữ 200 ký tự; định nghĩa 1.000 ký tự.
Không hứa màu bộ thẻ biểu thị CEFR/độ thành thạo nếu UI không có chú giải đó.

### Học viên tạo bộ riêng

1. Học viên → Flashcard → Tạo bộ thẻ riêng.
2. Tên `[DEMO] Từ hay quên của tôi`, cấp A1, dùng 3 cặp từ bất kỳ bên trên.
3. Lưu rồi tìm trong khu vực bộ cá nhân, không tìm trong cây bộ gắn bài.
4. Không cần chọn bài học; bộ cá nhân có chủ sở hữu, không phải bộ chung do admin công bố.

## 7. Lộ trình, mục tiêu và bài kiểm tra cuối chặng

### Trạng thái ban đầu và tự đồng bộ

1. Dùng học viên mới chưa có kết quả đánh giá/Goal, mở Lộ trình của tôi.
2. Xuất phát hiển thị A1, mục tiêu mặc định C2: `A1 → C2`. Đây là giá trị điều
   hướng giao diện, không phải chứng nhận học viên đã thi đạt A1 hay tạo Goal giả.
3. Đặt mục tiêu Reading B2 trong Hồ sơ/mục tiêu, quay lại lộ trình: đích cuối
   chuyển B2, không yêu cầu học viên tự bấm nút cập nhật.
4. Công bố một bài A1 mới đúng kỹ năng; tải lại trang lộ trình để thấy nội dung
   đồng bộ. Đây là cập nhật khi vào/tải trang, không phải thông báo realtime không cần tải.
5. Thư viện Bài học vẫn cho mở các mức đã công bố để tự ôn. Học ngoài chặng không
   đồng nghĩa tự vượt qua chặng hoặc tăng CEFR.

### Chuẩn bị checkpoint trước khi demo chuyển chặng

- Có ít nhất một bài bắt buộc ở chặng và học viên đã hoàn thành toàn bộ bài bắt buộc.
- Có đề `CHECKPOINT` trạng thái công bố, đúng kỹ năng và đúng cấp chặng đang học.
  Chặng A1 cần checkpoint A1, không lấy C2 chỉ vì mục tiêu cuối mặc định là C2.
- Đề có câu hỏi phù hợp và cấu hình chấm/scale đã công bố; muốn đi tiếp A2 phải
  có nội dung A2 phù hợp. Thi đạt xong không tự sinh nội dung chưa tồn tại.
- Các bài `[QA]`/`[DEMO]` công bố cũng có thể được giao vào chặng: kiểm tra số
  bài thực tế, đừng giả định chỉ một bài vừa tạo là đủ mở checkpoint.
- Chưa có bài C2 trong form hiện tại (cấp nhập A1–C1); C2 là đích mặc định,
  không có nghĩa hệ thống đã có đầy đủ giáo trình và đề C2.

Chuẩn bị đề qua khu vực quản trị đề thi/ngân hàng câu hỏi của nhóm phụ trách
engine, không sửa trực tiếp điểm/Profile trong DB để giả lập đã đỗ.

| Metadata đề demo | Giá trị ví dụ cho Reading A1 |
| --- | --- |
| Tên đề | `[DEMO] Reading checkpoint A1` |
| Loại đề | CHECKPOINT |
| Kỹ năng đề và section | READING |
| Cấp mục tiêu của đề | A1 |
| Thời gian | 5 phút |
| Câu hỏi | ID các câu Reading hợp lệ đã công bố; chọn trên hệ thống, không điền ID giả |
| Scoring scheme / chứng chỉ | Cấu hình thực tế đã công bố và được nhóm chấm điểm xác nhận |
| Trạng thái | Đã công bố |

Không dùng trắc nghiệm nhúng trong bài học thay cho đề checkpoint: đó là hai
loại bài kiểm tra và hai luồng lưu khác nhau.

### Các bước trình bày chuyển chặng

1. Hoàn thành đủ bài bắt buộc, quay về lộ trình → phần cuối chặng mở nút làm bài.
2. Bắt đầu checkpoint từ chính chặng; nếu có lượt đang làm phải tiếp tục lượt đó.
3. Trả lời và nộp; chờ kết quả thực sự hoàn tất chấm.
4. Không đạt: giữ chặng hiện tại, giữ tiến độ bài đã học, có thể làm lại khi được phép.
5. Đạt A1: đóng chặng A1, ghi nhận cấp theo kết quả và mở chặng A2 nếu đủ điều
   kiện/nội dung và chưa đạt đích cuối. Thanh mức đã đi qua được nối màu.
6. Nếu chờ chấm/lỗi: hiển thị trạng thái tương ứng, không nói đã lên A2.

Đây là kịch bản cần nghiệm thu cùng engine/scoring; đợt kiểm thử UI bài học
vừa rồi chưa chứng minh toàn bộ checkpoint/AI chạy end-to-end trên SQL Server.
Phân công trong repo: Trâm phụ trách đề/engine attempt, Thông phụ trách
chấm điểm/AI/Skill Profile, Thảo phụ trách bài học/flashcard/lộ trình và nối checkpoint.

## 8. Bằng chứng lưu dữ liệu và kết quả

Trong demo thường chỉ cần nộp → tải lại → mở lịch sử, không bắt buộc mở SQL.
Nếu được hỏi lưu ở đâu:

| Dữ liệu | Nơi lưu |
| --- | --- |
| Lượt làm trắc nghiệm trong bài | lesson_check_attempts: điểm, lựa chọn, review_json, content_snapshot |
| Bản nháp/bài hiện hành | lesson_responses |
| Bài viết/bản ghi/transcript đã nộp | lesson_work_submissions |
| Mốc nghe/vị trí học | lesson_engagement |
| Hoàn thành bài/chặng | lesson_progress, learning_path_items, learning_paths |
| Bộ thẻ/từ vựng | flashcard_sets, flashcards; tiến độ thẻ theo user |
| Media | Tệp trong storage, DB giữ đường dẫn; lịch sử giữ tham chiếu tệp cũ |

Nếu trình diễn SQL, chỉ SELECT theo đúng lesson_id/user_id của bài demo,
không UPDATE điểm, DELETE lịch sử hoặc lộ mật khẩu cấu hình kết nối.

Kết quả thi checkpoint nằm ở luồng đề thi/attempt và trang Kết quả. Lượt
trắc nghiệm/bài viết trong một bài học xem ở chính bài học; không hứa chúng
đều hiện như một kỳ thi trong trang Kết quả.

## 9. Dùng dữ liệu QA có sẵn thay vì nhập lại

Các link học viên đã được tạo trong đợt kiểm thử trước:

- Reading: http://localhost:8080/student/lessons/35
- Grammar: http://localhost:8080/student/lessons/36
- Listening: http://localhost:8080/student/lessons/37
- Writing: http://localhost:8080/student/lessons/38
- Speaking: http://localhost:8080/student/lessons/39

Lưu ý: 35–38 hiện đã sửa sang V2 để kiểm lịch sử. Reading 35 hiện đúng 10:00,
lượt cũ đúng 9:00 và video cũ chỉ còn trong lịch sử. Writing 38 đề hiện tại
Sunday, đề đã nộp cũ Saturday. Dùng các bài này để trình bày **lịch sử không
đổi sau sửa đề**, không đọc dữ liệu mới như thể chưa từng thay đổi.

Muốn demo từ đầu đúng dữ liệu ở mục 4, tạo bài `[DEMO]` mới và học viên chưa
từng làm. Không reset DB hoặc gán ngược các lượt cũ.

## 10. Câu nói ngắn khi trình bày

“Quản trị vẫn dùng mẫu có cấu trúc để hệ thống chấm và lưu được lịch sử,
nhưng mỗi kỹ năng có phần nhập phù hợp. Học viên chỉ thấy nội dung công bố;
Writing lưu bài viết, Speaking lưu audio, còn transcript/bài mẫu mở theo quy
tắc. Khi quản trị sửa đề, lịch sử vẫn giữ nguyên đề và bài nộp lúc đó.
Tiến độ bài học không phải điểm CEFR: việc lên chặng được quyết định qua
checkpoint đúng kỹ năng và cấp độ.”
