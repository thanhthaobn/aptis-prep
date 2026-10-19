-- Original teaching materials. External videos are linked, not copied.
-- Preserve existing lessons and completion; append new lessons to each level.
DECLARE @lesson_0 INT;
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
SELECT N'Đọc tìm thông tin: xác định từ khóa và bằng chứng',N'Tìm được thông tin về thời gian, địa điểm và điều kiện trong thông báo.',N'Reading',N'A2',COALESCE(MAX(display_order),0)+1,'PUBLISHED',N'Thông báo và lịch sinh hoạt',N'Tìm thông tin cụ thể trong văn bản ngắn',N'Tìm được thông tin về thời gian, địa điểm và điều kiện trong thông báo.
Phân biệt từ trùng với thông tin thực sự trả lời câu hỏi.
Nêu được câu làm bằng chứng cho đáp án.',N'Bước 1 — Đọc câu hỏi trước: xác định cần tìm người, nơi chốn, thời gian hay lý do.
Bước 2 — Gạch từ khóa và nghĩ cách diễn đạt tương đương, ví dụ closes = stops accepting visitors.
Bước 3 — Đọc quét để tìm vùng chứa thông tin, sau đó đọc kỹ cả câu và câu lân cận.
Bước 4 — Chú ý ngoại lệ như only, except, before, after. Không chọn chỉ vì thấy một con số giống đáp án.
Bước 5 — Đối chiếu từng lựa chọn với văn bản. Ghi lại bằng chứng rồi mới kết luận.
Video giới thiệu dạng bài Reading của Aptis. Bài này tập trung vào kỹ năng nền A2, không phải một đề Aptis đầy đủ.',N'RIVERSIDE LIBRARY
The library opens at 8 a.m. from Monday to Friday. On Saturday, it opens at 10 a.m. and closes at 4 p.m. The study room closes thirty minutes before the library. Visitors can return books using the box outside the entrance when the building is closed.

Question: When does the study room close on Saturday?
A. 10 a.m.
B. 3:30 p.m.
C. 4 p.m.',N'Cần tìm giờ đóng cửa của phòng học, không phải giờ đóng cửa thư viện.
Câu thứ hai cho biết thứ Bảy thư viện đóng lúc 4 p.m. Câu tiếp theo nói phòng học đóng sớm hơn 30 phút. Suy ra 3:30 p.m., chọn B.
A là giờ mở cửa. C là giờ đóng cửa tòa nhà. Hai số có thật trong văn bản nhưng không trả lời đúng đối tượng được hỏi.',N'Chọn số đầu tiên nhìn thấy.
Bỏ qua đối tượng study room và hiểu thành library.
Dịch từng từ làm mất thời gian thay vì đọc để tìm thông tin.
Dùng hiểu biết ngoài đời thay cho điều văn bản nói.',N'Đọc thông báo mới:
The swimming pool is open from 7 a.m. to 8 p.m. on weekdays. On Sundays, it closes at 6 p.m. The ticket desk stops selling tickets one hour before closing. Children under twelve must come with an adult.

1. What time does the ticket desk stop selling tickets on Sundays?
2. Can an eleven-year-old child visit alone?
3. Ghi câu hoặc cụm từ làm bằng chứng cho từng câu trả lời.',N'1. At 5 p.m. — Hồ bơi đóng lúc 6 p.m. Chủ nhật; quầy vé ngừng bán trước một giờ.
2. No. — Children under twelve must come with an adult.
Bằng chứng cần kết hợp đúng ngày, đúng đối tượng và điều kiện tuổi.',N'Tôi đã tìm đúng đối tượng câu hỏi chưa?
Tôi đã xét ngày và từ chỉ điều kiện chưa?
Tôi có trích được bằng chứng không?
Nếu sai, tôi nhầm đối tượng, thời gian hay điều kiện?',N'https://www.youtube.com/watch?v=yiwhUC4S5_o',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council — video và cấu trúc Aptis; nội dung giảng và bài tập do AptisPrep biên soạn'
FROM lessons WHERE skill=N'Reading' AND cefr_level=N'A2';
SET @lesson_0=SCOPE_IDENTITY();
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_0,1,N'Quầy vé trong bài thực hành ngừng bán lúc mấy giờ vào Chủ nhật?',N'5 p.m.',N'6 p.m.',N'8 p.m.',0,N'Đóng cửa 6 p.m., ngừng bán vé trước một giờ nên là 5 p.m.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_0,2,N'An eleven-year-old child can visit the pool…',N'alone.',N'with an adult.',N'only on weekdays.',1,N'Under twelve bao gồm trẻ 11 tuổi. Văn bản yêu cầu đi cùng người lớn.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_0,3,N'Cách chọn đáp án đáng tin cậy nhất là gì?',N'Chọn số giống trong câu hỏi.',N'Dùng trải nghiệm ngoài đời.',N'Đối chiếu thông tin và chỉ ra bằng chứng.',2,N'Bằng chứng phải trả lời đúng đối tượng và điều kiện trong câu hỏi.');
GO
DECLARE @lesson_1 INT;
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
SELECT N'Nghe có mục tiêu: dự đoán thông tin và xác nhận đáp án',N'Biết chuẩn bị trước khi nghe thay vì cố dịch tất cả.',N'Listening',N'A1',COALESCE(MAX(display_order),0)+1,'PUBLISHED',N'Giao tiếp hằng ngày',N'Nghe thời gian, địa điểm và thông tin sửa lại',N'Biết chuẩn bị trước khi nghe thay vì cố dịch tất cả.
Nhận ra thông tin người nói sửa lại.
Dùng lần nghe tiếp theo để xác nhận điều chưa chắc.',N'Trước khi nghe — Đọc nhiệm vụ. Dự đoán dạng thông tin cần nghe: số, giờ, địa điểm hoặc hoạt động.
Lượt đầu — Nghe tình huống và ý chính; ghi vài từ khóa, không chép toàn bộ lời nói.
Lượt tiếp — Tập trung thông tin còn thiếu. Chú ý actually, sorry, instead: câu sau có thể sửa thông tin trước.
Sau khi nghe — Giải thích vì sao chọn câu trả lời bằng thông tin nghe được.
Video British Council hướng dẫn phương pháp làm bài Listening; ví dụ lời thoại bên dưới do AptisPrep biên soạn, không phải transcript video.',N'Lời thoại minh họa để phân tích:
“Let''s meet at the station at nine. Sorry, my bus is late. Can we meet at half past nine instead? I''ll wait near the café, not at the main entrance.”

Cần tìm: giờ hẹn cuối cùng và nơi chờ.',N'Nine là giờ đề nghị ban đầu; Sorry báo hiệu sửa lại. Half past nine instead là lựa chọn cuối: 9:30.
Near the café là nơi chờ, còn main entrance bị phủ định bởi not.
Không cần biết nghĩa mọi từ vẫn có thể trả lời nếu nhận ra giờ, nơi chốn và tín hiệu sửa thông tin.',N'Chốt đáp án ngay khi nghe thấy thông tin đầu tiên.
Không nghe từ phủ định hoặc tín hiệu sửa lại.
Bật phụ đề ngay từ đầu rồi chỉ đọc thay vì nghe.
Chỉ nghe lại liên tục mà không xác định thông tin cần kiểm tra.',N'Xem video hướng dẫn, viết 3 điều bạn sẽ làm trước và trong khi nghe.
Sau đó mở video nguồn Cooking: https://www.youtube.com/watch?v=uVGV8LG3HHM
Lần đầu: ghi chủ đề và 3 từ bạn nghe được, chưa bật phụ đề.
Lần tiếp theo: ghi 2 thông tin cụ thể về thói quen hoặc sở thích của người nói. Mỗi thông tin ghi thêm mốc thời gian bạn nghe thấy.
Cuối cùng đối chiếu bằng cách nghe lại và dùng phụ đề nếu có. Không đoán đáp án từ tên video.',N'Mẫu ghi chép phương pháp, không phải đáp án nội dung Cooking:
Trước khi nghe: tôi xác định cần tìm sở thích, hoạt động hoặc món ăn.
Khi nghe: tôi ghi từ khóa thay vì cả câu.
Sau khi nghe: tôi nghe lại đúng đoạn và kiểm tra thông tin.
Thông tin nghe được: … / mốc thời gian: … / điều cần kiểm tra: …',N'Tôi có nghe trước khi nhìn phụ đề không?
Tôi có ghi được thông tin cụ thể kèm vị trí trong video không?
Tôi có phân biệt điều thực sự nghe được với điều mình đoán không?
Tôi biết lỗi của mình là từ vựng, âm thanh hay mất tập trung chưa?',N'https://www.youtube.com/watch?v=cbGwglNyEaQ',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council — video và cấu trúc Aptis; nội dung giảng và bài tập do AptisPrep biên soạn'
FROM lessons WHERE skill=N'Listening' AND cefr_level=N'A1';
SET @lesson_1=SCOPE_IDENTITY();
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_1,1,N'Giờ hẹn cuối cùng trong lời thoại minh họa là gì?',N'9:00',N'9:30',N'10:30',1,N'Sorry và instead báo hiệu giờ đã đổi sang half past nine.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_1,2,N'Người nói chờ ở đâu trong ví dụ?',N'Gần quán cà phê.',N'Tại cửa chính.',N'Trên xe buýt.',0,N'Near the café là nơi được chọn. Main entrance bị phủ định.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_1,3,N'Khi nghe lại, bạn nên làm gì?',N'Luôn chép mọi từ.',N'Chọn theo thông tin đầu tiên.',N'Xác nhận phần còn thiếu và thông tin sửa lại.',2,N'Nghe lại có mục tiêu giúp xử lý điều chưa chắc thay vì chỉ lặp thụ động.');
GO
DECLARE @lesson_2 INT;
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
SELECT N'Viết email thân mật: phân tích đề và triển khai từng ý',N'Xác định người nhận và giọng văn trước khi viết.',N'Writing',N'B1',COALESCE(MAX(display_order),0)+1,'PUBLISHED',N'Câu lạc bộ và hoạt động cộng đồng',N'Email thân mật — mục đích, đề xuất và lý do',N'Xác định người nhận và giọng văn trước khi viết.
Lập dàn ý gồm mục đích, đề xuất, lý do và lời kết.
Viết câu rõ nghĩa, liên kết hợp lý và tự sửa lỗi.',N'1. Khoanh người nhận: bạn bè hay người quản lý? Bài này viết cho bạn nên dùng giọng thân mật, lịch sự.
2. Liệt kê các ý bắt buộc trong đề; biến mỗi ý thành một phần của dàn ý.
3. Viết câu mở nêu lý do gửi thư. Không dành quá nhiều chữ cho lời chào.
4. Mỗi đề xuất đi kèm một lý do hoặc chi tiết cụ thể. Dùng because để giải thích, so để nói kết quả.
5. Kết thúc bằng câu hỏi hoặc lời mời phản hồi và lời chào phù hợp.
6. Đọc lại theo thứ tự: đủ ý → đúng giọng → động từ → chính tả.
Video là hướng dẫn tổng quan Aptis Writing; nhiệm vụ ở đây là bài giảng nhỏ, không phải bài thi chính thức.',N'Đề: Your friend wants to join your weekend reading club. Write an informal email. Suggest a meeting day, recommend a book, and explain why the club is suitable for beginners.

Dàn ý:
Mở: vui vì bạn muốn tham gia.
Ý 1: Chủ nhật, vì cả hai rảnh.
Ý 2: sách truyện ngắn, vì dễ hoàn thành.
Ý 3: thành viên giúp nhau, phù hợp người mới.
Kết: hỏi bạn có tham gia được không.',N'Người nhận là bạn nên Hi … và lời kết thân mật phù hợp.
Ba yêu cầu phải có đủ; một email trôi chảy nhưng thiếu đề xuất sách vẫn chưa hoàn thành nhiệm vụ.
Cặp đề xuất + lý do giúp nội dung cụ thể hơn: Let''s meet on Sunday because …
Mỗi đoạn làm một nhiệm vụ. Không áp khuôn mở bài–thân bài–kết luận của bài luận vào mọi email.',N'Viết bài thuộc lòng không trả lời yêu cầu.
Dùng giọng quá trang trọng cho bạn hoặc quá suồng sã với người quản lý.
Chỉ liệt kê đề xuất mà thiếu lý do.
Sửa từ khó trước khi kiểm tra đủ ý.',N'Bạn nhận tin từ Alex: “I want to join your cooking club, but I am a beginner. When should I come, what should I bring, and will I be able to follow the activities?”
Viết email thân mật khoảng 70–90 từ để luyện triển khai ý. Đây là giới hạn luyện tập của bài, không phải khẳng định giới hạn đề Aptis chính thức.
Trả lời đủ: thời gian, vật dụng và lý do người mới có thể tham gia.',N'Hi Alex,
I''m glad you want to join our cooking club. Come on Saturday at ten because we start with a short introduction. Please bring an apron and a small notebook. You don''t need to buy ingredients because the club provides them.
Don''t worry about being a beginner. We work in small groups, and an experienced member explains each step. We are making a simple vegetable soup this week, so it is a good activity to start with.
Can you come?
Best,
Mai

Đối chiếu: đoạn đầu trả lời thời gian và vật dụng; đoạn sau giải thích vì sao phù hợp người mới; câu cuối mời phản hồi.',N'Đủ ba ý của đề chưa?
Mỗi đề xuất có chi tiết hoặc lý do chưa?
Giọng văn có phù hợp người nhận không?
Động từ sau chủ ngữ có đúng không?
Bài viết thực hành được lưu, chưa được chấm điểm AI; hãy đối chiếu tiêu chí và bài mẫu.',N'https://www.youtube.com/watch?v=wumg5p4xeJY',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council — video và cấu trúc Aptis; nội dung giảng và bài tập do AptisPrep biên soạn'
FROM lessons WHERE skill=N'Writing' AND cefr_level=N'B1';
SET @lesson_2=SCOPE_IDENTITY();
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_2,1,N'Trước khi viết email, cần xác định điều gì trước?',N'Người nhận và mục đích.',N'Một thành ngữ khó.',N'Số câu càng nhiều càng tốt.',0,N'Người nhận quyết định giọng văn; mục đích và yêu cầu quyết định nội dung.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_2,2,N'Đề thực hành yêu cầu trả lời những gì?',N'Thời tiết và giá vé.',N'Thời gian, vật dụng và khả năng tham gia của người mới.',N'Lịch sử câu lạc bộ.',1,N'Cần trả lời trực tiếp ba câu hỏi của Alex.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_2,3,N'Câu nào vừa đề xuất vừa giải thích?',N'Saturday.',N'You should come.',N'Come on Saturday because we start with an introduction.',2,N'Câu có đề xuất cụ thể và lý do hỗ trợ.');
GO
DECLARE @lesson_3 INT;
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
SELECT N'Nói có phát triển ý: trả lời, lý do và ví dụ',N'Tạo câu trả lời có ý chính, lý do và ví dụ.',N'Speaking',N'B1',COALESCE(MAX(display_order),0)+1,'PUBLISHED',N'Sở thích và trải nghiệm cá nhân',N'Trả lời câu hỏi cá nhân có giải thích',N'Tạo câu trả lời có ý chính, lý do và ví dụ.
Chuẩn bị từ khóa thay vì học thuộc cả đoạn.
Tự nhận xét độ rõ ý và sự liên kết khi luyện nói.',N'Bước 1: trả lời trực tiếp câu hỏi bằng một câu đơn giản.
Bước 2: thêm một lý do bằng because hoặc one reason is …
Bước 3: thêm ví dụ thật: when, where, who, what happened.
Bước 4: kết bằng cảm nhận hoặc liên hệ ngắn nếu còn thời gian.
Trước khi nói, chỉ ghi 3–5 từ khóa. Khi thiếu từ, diễn đạt bằng từ dễ hơn thay vì dừng quá lâu.
Video hướng dẫn Aptis Speaking tổng quan. Bài này tập trung triển khai câu trả lời cá nhân; không thay thế các dạng mô tả/so sánh ảnh.',N'Question: What do you enjoy doing at weekends?
Từ khóa: cycling — fresh air — sister — Sunday — park.

Câu trả lời ngắn: I like cycling.
Câu phát triển: I enjoy cycling at weekends because it helps me relax. Last Sunday, I rode to a nearby park with my sister. We talked and had a picnic, so it was a lovely morning.',N'I enjoy cycling trả lời trực tiếp.
Because it helps me relax giải thích sở thích.
Last Sunday … đưa ra ví dụ có thời gian, người đi cùng và hoạt động.
So it was … kết nối sự việc với cảm nhận.
Không cần dùng nhiều từ khó; ưu tiên câu có nghĩa và phát triển đúng câu hỏi.',N'Chỉ trả lời một từ hoặc một câu quá ngắn.
Học thuộc câu trả lời rồi dùng cho câu hỏi không phù hợp.
Nói thật nhanh để nhồi ý.
Dừng lâu vì cố tìm một từ khó trong khi có thể nói đơn giản hơn.',N'Chuẩn bị 4 từ khóa rồi nói khoảng 30–45 giây cho từng câu:
1. What activity helps you relax?
2. Tell me about a weekend you enjoyed.
3. Do you prefer spending free time alone or with friends? Why?
Có thể ghi âm bằng thiết bị của bạn rồi nghe lại. Nhập dàn ý và nhận xét vào ô bên dưới; trang này hiện lưu ghi chú, chưa nhận/chấm bản ghi âm.',N'Dàn ý câu 3: with friends — share ideas — cooking — last weekend.
Example: I prefer spending free time with friends because we can share ideas and try new activities. Last weekend, two friends came to my home and we cooked dinner together. We made a few mistakes, but we laughed a lot. I also enjoy quiet time alone, especially when I want to read.

Đây là ví dụ phát triển ý, không phải kịch bản bắt buộc học thuộc.',N'Tôi trả lời đúng câu hỏi ngay đầu chưa?
Có lý do và ví dụ cụ thể chưa?
Tôi có liên kết ý hay chỉ liệt kê?
Khi tự nghe bản ghi, có đoạn dừng nào cần luyện lại?
Website chưa chấm phát âm hoặc đưa điểm Speaking cho bài này.',N'https://www.youtube.com/watch?v=1c5TCWR6Fyg',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council — video và cấu trúc Aptis; nội dung giảng và bài tập do AptisPrep biên soạn'
FROM lessons WHERE skill=N'Speaking' AND cefr_level=N'B1';
SET @lesson_3=SCOPE_IDENTITY();
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_3,1,N'Cấu trúc nào giúp phát triển câu trả lời?',N'Trả lời → lý do → ví dụ.',N'Chào hỏi → im lặng.',N'Lặp lại nguyên câu hỏi.',0,N'Lý do và ví dụ giúp người nghe hiểu rõ ý chính.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_3,2,N'Nên chuẩn bị gì trước khi nói?',N'Một bài dài để học thuộc.',N'Vài từ khóa gợi ý.',N'Chỉ một từ thật khó.',1,N'Từ khóa hỗ trợ diễn đạt tự nhiên và giữ đúng hướng.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_3,3,N'Trong ví dụ, Last Sunday có vai trò gì?',N'Đưa ra một trải nghiệm cụ thể.',N'Thay cho lý do.',N'Là lời chào kết.',0,N'Dấu mốc thời gian mở phần ví dụ để minh họa ý chính.');
GO
DECLARE @lesson_4 INT;
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
SELECT N'Hiện tại đơn: nói về thói quen trong ngữ cảnh',N'Chọn dạng động từ hiện tại đơn phù hợp chủ ngữ.',N'Grammar & Vocabulary',N'A1',COALESCE(MAX(display_order),0)+1,'PUBLISHED',N'Daily routines',N'Hiện tại đơn và cụm từ chỉ sinh hoạt',N'Chọn dạng động từ hiện tại đơn phù hợp chủ ngữ.
Dùng don''t/doesn''t và động từ nguyên mẫu.
Kết hợp cụm từ sinh hoạt trong câu có nghĩa.',N'1. Xác định ý nghĩa: thói quen, sự việc lặp lại hoặc thông tin chung.
2. Tìm chủ ngữ. I/you/we/they thường dùng động từ nguyên mẫu; he/she/it thêm -s/-es ở câu khẳng định.
3. Câu phủ định: don''t hoặc doesn''t + động từ nguyên mẫu. Không thêm -s sau doesn''t.
4. Câu hỏi: Do/Does + chủ ngữ + động từ nguyên mẫu?
5. Học theo cụm: have breakfast, go to work, take a break. Đặt câu về bản thân để gắn nghĩa với ngữ cảnh.
Video British Council giới thiệu phần Grammar & Vocabulary của Aptis, không phải video giảng riêng thì hiện tại đơn. Phần giải thích thì nằm trong bài giảng này.',N'Mai gets up at six every day. She has breakfast at home. She doesn''t drive to work. Does she take the bus? Yes, she does.

So sánh:
They work near the station.
He works near the station.
He doesn''t work on Sundays.',N'Mai và she là ngôi thứ ba số ít nên câu khẳng định dùng gets, has.
Doesn''t đã mang dấu hiệu ngôi và thì, nên drive giữ nguyên.
Trong câu hỏi Does she take …, take không thêm -s.
They là số nhiều nên work; he là số ít nên works. On Sundays gợi hoạt động lặp lại.',N'She go thay cho She goes.
She doesn''t goes: đánh dấu ngôi hai lần.
Nhầm have breakfast thành take breakfast trong cụm thông dụng của bài.
Chỉ thuộc công thức nhưng không hiểu câu đang diễn tả thói quen nào.',N'Hoàn thành câu và giải thích lựa chọn:
1. My brother ___ (work) in a bookshop.
2. We ___ (not study) on Sundays.
3. ___ she ___ (have) breakfast at home?
Sau đó viết 4 câu về ngày thường của bạn: hai câu khẳng định, một câu phủ định và một câu hỏi.',N'1. works — My brother là số ít.
2. don''t study — We dùng don''t + động từ nguyên mẫu.
3. Does she have — Sau does dùng have, không dùng has.

Ví dụ cá nhân:
I get up at six. I have breakfast with my family. I don''t work on Sundays. Do you take the bus to work?',N'Tìm chủ ngữ trước khi chọn dạng động từ.
Kiểm tra động từ sau do/does/don''t/doesn''t giữ nguyên.
Đọc lại câu để chắc nghĩa phù hợp thói quen.
Thử đổi chủ ngữ từ I sang she để kiểm tra mình hiểu cách dùng.',N'https://www.youtube.com/watch?v=Y_DELbsHzZI',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council — video và cấu trúc Aptis; nội dung giảng và bài tập do AptisPrep biên soạn'
FROM lessons WHERE skill=N'Grammar & Vocabulary' AND cefr_level=N'A1';
SET @lesson_4=SCOPE_IDENTITY();
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_4,1,N'My brother ___ in a bookshop.',N'work',N'works',N'working',1,N'My brother là chủ ngữ ngôi thứ ba số ít, câu khẳng định dùng works.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_4,2,N'Chọn câu đúng.',N'She doesn''t goes to work.',N'She don''t go to work.',N'She doesn''t go to work.',2,N'Doesn''t + động từ nguyên mẫu go.');
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES(@lesson_4,3,N'___ she have breakfast at home?',N'Does',N'Do',N'Is',0,N'Câu hỏi hiện tại đơn với she dùng Does + chủ ngữ + động từ nguyên mẫu.');
GO
