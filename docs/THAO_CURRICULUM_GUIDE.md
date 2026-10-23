# Nội dung học tập và cách kiểm tra luồng Thảo

Chốt phạm vi 23/10/2026: hành trình học A1–C1. Profile C1/C2 không sinh chặng mới C2; mục tiêu/profile C2 cũ vẫn giữ để tra cứu, không tự hạ mức. Lesson/Flashcard vẫn chỉ tạo A1–C1. Giới hạn Goal mới thuộc phần tài khoản của Thông, không sửa trong đợt xử lý luồng Thảo.

Kiểm chứng V039: `mvn -o clean verify` với `APTIS_SQLSERVER_ISOLATED_TESTS=true` đạt (197 test: 195 đạt, không có failure/error; 2 skip là Gemini live và migration trực tiếp DB ứng dụng); JavaScript 9/9 đạt. Test gồm 89%/90%, tua/nghe lặp/replay, token khác user/hết hạn, thay audio, hoàn thành qua MVC/JPA, C1/legacy C2; SQL Server tạm kiểm migration, Hibernate validate, bảo toàn dữ liệu, đáp án D/E/F, chấm đúng/sai, validation và đối chiếu schema ERD. Cờ `-o` dùng dependency đã cache. Không chạy migration trực tiếp lên DB ứng dụng, không push/merge trong đợt sửa này.

### Checklist SQL Server cho L01 và ERD V039

`V2SqlServerPreservationTests` dùng database UUID riêng, chạy migration đến V037, thêm fixture A/B/C và dấu nghe cũ, chụp toàn bộ cột cũ rồi nâng lên V039. Test tự dọn đúng database đã tạo trong `finally`; không chạy `clean` hoặc migration trên database ứng dụng.

| Ca kiểm tra | Cách kiểm chứng |
|---|---|
| 4 lựa chọn, đáp án D; 6 lựa chọn, đáp án F | Gọi validation và saveChecks thật, flush/clear JPA rồi đọc lại từ SQL Server |
| Nộp đúng/sai, lưu điểm và lịch sử | Gọi grade thật; kiểm score, total, feedback và đọc lại các attempt đã lưu |
| Chỉ số âm hoặc vượt số lựa chọn, thiếu câu trả lời | Service từ chối; DB cũng từ chối correct_option = -1 và 6 |
| Lựa chọn bị khuyết ở giữa | Validation từ chối; câu hợp lệ và số attempt không bị thay đổi |
| Bảo toàn dữ liệu A/B/C và dữ liệu cũ khác | So sánh snapshot trước/sau migration trên tất cả bảng và các cột cũ |
| ERD có thuộc tính | So sánh toàn bộ tên bảng/cột trong Mermaid với metadata SQL Server sau V039: 39 bảng, 414 cột |

Hai ERD trong [BUSINESS_SPEC_CURRENT.md](BUSINESS_SPEC_CURRENT.md) và ảnh SVG được cập nhật theo V039. Sơ đồ không thuộc tính không đổi quan hệ (62 FK); sơ đồ có thuộc tính thêm 10 cột phiên nghe của lesson_engagement. Các cột/bảng legacy vẫn được giữ, không tự drop.

Format bằng google-java-format theo indent 4 spaces cho các service học tập, controller Lesson, entity phiên nghe, service đọc thời lượng và các test phiên nghe/SQL liên quan; không tách thêm service hoặc đổi quy tắc nghiệp vụ trong bước format.

Chương trình hiện hỗ trợ A1, A2, B1, B2 và C1. Sau V024/V025, mỗi kỹ năng có ít nhất một bài giảng và một bộ thẻ liên kết ở mỗi mức. Các mức dùng để tổ chức nội dung học, không phải kết quả đánh giá CEFR của học viên.

## Các bài học chính

| Kỹ năng | A1 | A2 | B1 | B2 | C1 |
|---|---|---|---|---|---|
| Grammar & Vocabulary | Hiện tại đơn: nói về thói quen trong ngữ cảnh | Kể một chuyến đi bằng thì quá khứ | Phân biệt trải nghiệm và thời điểm đã kết thúc | Diễn đạt khả năng và hệ quả bằng câu điều kiện | Dùng cấu trúc nhấn mạnh và kết hợp từ tự nhiên |
| Reading | Đọc tin nhắn và nhận biết thông tin cơ bản | Đọc tìm thông tin: xác định từ khóa và bằng chứng | Sắp xếp câu bằng dấu hiệu liên kết | Ghép quan điểm qua cách diễn đạt tương đương | Suy luận lập trường và giới hạn của lập luận |
| Listening | Nghe có mục tiêu: dự đoán thông tin và xác nhận đáp án | Nghe thông báo thay đổi lịch trình | Ghép người nói với lý do lựa chọn | Theo dõi sự đồng ý có điều kiện trong thảo luận | Nghe sự dè dặt và hàm ý trong một bài bình luận |
| Writing | Viết thông tin cá nhân và câu trả lời ngắn | Viết đoạn ngắn về một hoạt động cuối tuần | Viết email thân mật: phân tích đề và triển khai từng ý | Điều chỉnh giọng văn trong hai email cùng chủ đề | Viết đề xuất có đánh giá phương án và phản biện |
| Speaking | Giới thiệu bản thân bằng câu ngắn rõ ý | Mô tả một cảnh sinh hoạt theo thứ tự | Nói có phát triển ý: trả lời, lý do và ví dụ | So sánh hai lựa chọn và bảo vệ ý kiến | Trình bày quan điểm có sắc thái và xử lý phản biện |

## Nội dung và nguồn

20 bài mới có mục tiêu, cách làm, ví dụ, phân tích, lỗi thường gặp, thực hành, bài mẫu, hai câu kiểm tra có lời giải và bốn thẻ từ phù hợp nội dung. Năm bài chính trước đó và dữ liệu học viên được giữ lại.

Ví dụ, câu hỏi và bài mẫu được AptisPrep tự biên soạn. Video giới thiệu cấu trúc kỹ năng được liên kết từ trang British Council; không xem video tổng quan đó là video giảng riêng của mỗi chủ đề hay bằng chứng cho mức CEFR.

- [Grammar & Vocabulary](https://www.britishcouncil.org/exam/aptis/practice-materials/grammar-vocabulary)
- [Reading](https://www.britishcouncil.org/exam/aptis/practice-material/reading-test)
- [Listening](https://www.britishcouncil.org/exam/aptis/practice-materials/listening-aptis-test)
- [Writing](https://www.britishcouncil.org/exam/aptis/take-test/writing-video)
- [Speaking](https://www.britishcouncil.org/exam/aptis/take-test/speaking-video)

Bốn bài Listening A2–C1 mới có tệp WAV đóng gói cùng ứng dụng. Lời thoại là nội dung tự biên soạn, đọc bằng Microsoft David Desktop (giọng tổng hợp Windows). Không tải hoặc sao chép âm thanh của bên thứ ba. Transcript khớp tệp nghe và được ẩn để người học tự nghe trước. Dữ liệu biên soạn nằm ở `docs/content/curriculum-a1-c1.json`; có thể tái tạo âm thanh bằng `scripts/build-curriculum-audio.ps1` trên Windows có giọng tiếng Anh.

## Thao tác kiểm tra

Sau tích hợp develop ngày 21/10, phần chặng cá nhân chỉ giao mức tiếp theo từ Profile đến Goal. Thư viện A1–C1 vẫn cho tự ôn và được tách rõ khỏi các bài đang tính tiến độ. Xem thay đổi và phần còn phụ thuộc Thông/Trâm tại [THAO_INTEGRATION_AND_HANDOFF.md](THAO_INTEGRATION_AND_HANDOFF.md).

1. Khởi động bản mới, đăng nhập học viên và mở `/student/lessons`. Lọc lần lượt năm kỹ năng, năm mức: mỗi tổ hợp phải có bài. Ở desktop rộng, thẻ bài học có ba cột; màn hẹp tự giảm cột, menu thu gọn.
2. Trên thẻ một bài mới, nhấn tên bộ từ vựng ở cuối thẻ. Trang ôn thẻ hiển thị đúng tên bài học nguồn, kỹ năng và mức. Nhấn tên bài nguồn để quay về đúng bài.
3. Mở `/student/flashcards`: mỗi bộ thư viện có khung Bài học nguồn, nút Mở bài học và nút Ôn bộ thẻ. Bộ cá nhân vẫn có nhãn Riêng tư và không bị ép gắn bài.
4. Lọc Writing rồi Speaking ở danh sách bài học: Writing tím; Speaking cam đất. Màu theo kỹ năng được dùng lại ở lộ trình, Flashcard, chi tiết bài và khu vực kết quả có kỹ năng.
5. Mở Lộ trình của tôi → Cập nhật lộ trình. Đi vào từng kỹ năng: đủ A1–C1, chủ đề và các bài trong chủ đề. Nhánh Grammar & Vocabulary phải mở bài ngữ pháp; không chuyển thẳng sang danh sách Flashcard.
6. Nhấn liên kết số thẻ dưới một bài trong cây lộ trình: mở đúng bộ thẻ của bài đó.
7. Với bài Listening mới: phát âm thanh, nghe xong trước khi mở transcript, trả lời câu hỏi và nộp. Phản hồi chỉ ra đáp án và giải thích. Phát âm thanh là một điều kiện hoàn thành bài Listening có tệp nghe.
8. Với Writing: lưu bài viết của bạn; tải lại vẫn còn nội dung. Với Speaking: ghi âm bằng micro của trình duyệt, nghe lại, lưu bản ghi hoặc ghi chú theo yêu cầu. Không tự gán điểm nếu dịch vụ chấm chưa chạy.
9. Hoàn thành một bài rồi cập nhật lộ trình: trạng thái hoàn thành và lịch sử phải được giữ.
10. Admin → Bài học → mở bài mới → Chỉnh sửa bộ thẻ: đúng bài học và CEFR đã liên kết. Admin → Flashcard → Mở bài học cũng phải về đúng bài nguồn.

## Đáp án kiểm tra của bài mới

Các đáp án đánh số A/B/C theo thứ tự hiển thị. Chỉ dùng bảng này để đối chiếu sau khi tự làm.

| Kỹ năng | CEFR | Bài | Câu 1 | Câu 2 |
|---|---|---|---|---|
| Grammar & Vocabulary | A2 | Kể một chuyến đi bằng thì quá khứ | A | B |
| Grammar & Vocabulary | B1 | Phân biệt trải nghiệm và thời điểm đã kết thúc | B | C |
| Grammar & Vocabulary | B2 | Diễn đạt khả năng và hệ quả bằng câu điều kiện | C | A |
| Grammar & Vocabulary | C1 | Dùng cấu trúc nhấn mạnh và kết hợp từ tự nhiên | A | B |
| Reading | A1 | Đọc tin nhắn và nhận biết thông tin cơ bản | B | A |
| Reading | B1 | Sắp xếp câu bằng dấu hiệu liên kết | C | B |
| Reading | B2 | Ghép quan điểm qua cách diễn đạt tương đương | B | B |
| Reading | C1 | Suy luận lập trường và giới hạn của lập luận | B | B |
| Listening | A2 | Nghe thông báo thay đổi lịch trình | B | C |
| Listening | B1 | Ghép người nói với lý do lựa chọn | B | C |
| Listening | B2 | Theo dõi sự đồng ý có điều kiện trong thảo luận | B | B |
| Listening | C1 | Nghe sự dè dặt và hàm ý trong một bài bình luận | A | B |
| Writing | A1 | Viết thông tin cá nhân và câu trả lời ngắn | B | C |
| Writing | A2 | Viết đoạn ngắn về một hoạt động cuối tuần | B | C |
| Writing | B2 | Điều chỉnh giọng văn trong hai email cùng chủ đề | B | C |
| Writing | C1 | Viết đề xuất có đánh giá phương án và phản biện | C | A |
| Speaking | A1 | Giới thiệu bản thân bằng câu ngắn rõ ý | A | B |
| Speaking | A2 | Mô tả một cảnh sinh hoạt theo thứ tự | B | C |
| Speaking | B2 | So sánh hai lựa chọn và bảo vệ ý kiến | B | B |
| Speaking | C1 | Trình bày quan điểm có sắc thái và xử lý phản biện | B | B |

