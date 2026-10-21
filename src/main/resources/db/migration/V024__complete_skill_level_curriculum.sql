-- Original AptisPrep teaching material. No learner scores, progress or attempts are seeded.
-- A1-C1 are teaching bands, not certified CEFR results. Videos introduce the skill format.
-- Existing content and user history are preserved. Each new lesson has its own vocabulary set.

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Kể một chuyến đi bằng thì quá khứ' AND skill=N'Grammar & Vocabulary' AND cefr_level=N'A2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Grammar & Vocabulary' AND cefr_level=N'A2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Kể một chuyến đi bằng thì quá khứ',N'Kể một sự kiện đã kết thúc; dùng động từ quá khứ và nối đúng trình tự.',N'Grammar & Vocabulary',N'A2',@order,N'PUBLISHED',N'Travel memories',N'Quá khứ đơn và từ nối thời gian',N'Kể một sự kiện đã kết thúc; dùng động từ quá khứ và nối đúng trình tự.',N'1. Xác định mốc thời gian đã kết thúc: yesterday, last weekend.
2. Câu khẳng định dùng động từ quá khứ; học riêng các dạng went, took, saw.
3. Với did/didn''t, đưa động từ về nguyên mẫu.
4. Dùng first, then, finally để người đọc theo được câu chuyện.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Last Saturday, we took a train to Hue. First, we visited the old city. Then we had lunch by the river. We didn''t stay overnight because we had work on Sunday.',N'Took, visited và had kể việc đã kết thúc. Sau didn''t, stay giữ nguyên. Because nêu lý do trở về; then cho biết thứ tự.',N'Viết didn''t went; đổi thì giữa câu chuyện mà không đổi mốc thời gian; nhầm then với than.',N'Kể chuyến đi gần đây bằng 5 câu. Nêu thời gian, phương tiện, hai hoạt động và một điều bạn không làm.',N'Last month, I went to Da Nang with my sister. We took a bus. First, we walked along the beach. Then we tried local food. We didn''t swim because it was windy.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=Y_DELbsHzZI',N'https://www.britishcouncil.org/exam/aptis/practice-materials/grammar-vocabulary',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'We didn''t ___ overnight.',N'stay',N'stayed',N'staying',0,N'Sau didn''t dùng động từ nguyên mẫu.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which word introduces the next event?',N'Because',N'Then',N'Although',1,N'Then nối sự việc tiếp theo theo thứ tự thời gian.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Travel memories — A2',N'Từ và cụm từ dùng trong bài: Kể một chuyến đi bằng thì quá khứ',N'A2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'take a train',N'đi tàu',N'A2',1,'ACTIVE'),
 (@set,N'overnight',N'qua đêm',N'A2',2,'ACTIVE'),
 (@set,N'last weekend',N'cuối tuần trước',N'A2',3,'ACTIVE'),
 (@set,N'finally',N'cuối cùng',N'A2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Phân biệt trải nghiệm và thời điểm đã kết thúc' AND skill=N'Grammar & Vocabulary' AND cefr_level=N'B1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Grammar & Vocabulary' AND cefr_level=N'B1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Phân biệt trải nghiệm và thời điểm đã kết thúc',N'Phân biệt trải nghiệm đến hiện tại với sự việc ở thời điểm xác định; dùng for và since.',N'Grammar & Vocabulary',N'B1',@order,N'PUBLISHED',N'Learning experiences',N'Hiện tại hoàn thành và quá khứ đơn',N'Phân biệt trải nghiệm đến hiện tại với sự việc ở thời điểm xác định; dùng for và since.',N'1. Hỏi: đang nói trải nghiệm hay một sự kiện đã có thời gian kết thúc?
2. Trải nghiệm: have/has + V3, có thể dùng ever/never.
3. Thời điểm xác định: dùng quá khứ đơn với last year, in 2022.
4. For chỉ khoảng thời gian; since chỉ điểm bắt đầu của trạng thái còn tiếp diễn.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'I have studied English for three years. I joined a speaking club last September. I have met people from several countries, but I haven''t spoken at a conference yet.',N'For three years kéo dài đến hiện tại. Last September là thời điểm quá khứ xác định nên dùng joined. Yet nói việc chưa xảy ra tính đến hiện tại.',N'Dùng have joined với last September; dùng since three years; coi mọi câu chứa kinh nghiệm đều là quá khứ đơn.',N'Viết 6 câu về quá trình học: hai trải nghiệm, hai sự kiện có mốc thời gian và hai trạng thái với for/since.',N'I have learned English since 2023. I started a new course last month. I have never given a speech in English. I have practised with a partner for six weeks.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=Y_DELbsHzZI',N'https://www.britishcouncil.org/exam/aptis/practice-materials/grammar-vocabulary',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'I ___ the club last September.',N'have joined',N'joined',N'have join',1,N'Last September là mốc đã kết thúc nên dùng joined.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'She has lived here ___ 2021.',N'for',N'during',N'since',2,N'Since đi với điểm bắt đầu 2021.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Learning experiences — B1',N'Từ và cụm từ dùng trong bài: Phân biệt trải nghiệm và thời điểm đã kết thúc',N'B1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'so far',N'cho đến nay',N'B1',1,'ACTIVE'),
 (@set,N'yet',N'chưa; tính đến hiện tại',N'B1',2,'ACTIVE'),
 (@set,N'join a club',N'tham gia câu lạc bộ',N'B1',3,'ACTIVE'),
 (@set,N'give a speech',N'phát biểu',N'B1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Diễn đạt khả năng và hệ quả bằng câu điều kiện' AND skill=N'Grammar & Vocabulary' AND cefr_level=N'B2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Grammar & Vocabulary' AND cefr_level=N'B2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Diễn đạt khả năng và hệ quả bằng câu điều kiện',N'Phân biệt giả định hiện tại với giả định trái quá khứ; chọn mệnh đề điều kiện và kết quả nhất quán.',N'Grammar & Vocabulary',N'B2',@order,N'PUBLISHED',N'Work and choices',N'Điều kiện loại hai và loại ba',N'Phân biệt giả định hiện tại với giả định trái quá khứ; chọn mệnh đề điều kiện và kết quả nhất quán.',N'1. Xác định giả định thuộc hiện tại/tương lai hay quá khứ.
2. Giả định ít thực ở hiện tại: if + quá khứ, would + động từ.
3. Giả định trái quá khứ: if + had + V3, would have + V3.
4. Đọc lại toàn câu để kiểm tra quan hệ điều kiện và hệ quả.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'If our office were closer, I would cycle to work. If I had checked the weather yesterday, I would have taken a raincoat.',N'Were closer giả định tình trạng hiện tại. Had checked và would have taken nói một lựa chọn đã không xảy ra hôm qua.',N'Dùng would ngay sau if trong cấu trúc cơ bản; nhầm would take với would have taken khi nói hôm qua.',N'Viết hai giả định về nơi làm việc hiện tại và hai điều bạn sẽ làm khác trong một chuyến đi đã qua. Giải thích sự khác nhau về thời gian.',N'If I had more flexible hours, I would attend an evening class. If I had booked earlier, I would have paid less for the ticket.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=Y_DELbsHzZI',N'https://www.britishcouncil.org/exam/aptis/practice-materials/grammar-vocabulary',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'If I had booked earlier, I ___ less.',N'would pay',N'will pay',N'would have paid',2,N'Giả định trái quá khứ cần would have + V3.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which sentence imagines a present situation?',N'If I were free, I would join you.',N'If I had been free, I would have joined you.',N'I joined you yesterday.',0,N'Were và would join diễn tả giả định hiện tại.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Work and choices — B2',N'Từ và cụm từ dùng trong bài: Diễn đạt khả năng và hệ quả bằng câu điều kiện',N'B2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'flexible hours',N'giờ làm linh hoạt',N'B2',1,'ACTIVE'),
 (@set,N'book in advance',N'đặt trước',N'B2',2,'ACTIVE'),
 (@set,N'consequence',N'hệ quả',N'B2',3,'ACTIVE'),
 (@set,N'hypothetical',N'mang tính giả định',N'B2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Dùng cấu trúc nhấn mạnh và kết hợp từ tự nhiên' AND skill=N'Grammar & Vocabulary' AND cefr_level=N'C1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Grammar & Vocabulary' AND cefr_level=N'C1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Dùng cấu trúc nhấn mạnh và kết hợp từ tự nhiên',N'Nhấn mạnh có mục đích; dùng collocation phù hợp văn phong báo cáo; giữ sắc thái thận trọng.',N'Grammar & Vocabulary',N'C1',@order,N'PUBLISHED',N'Research and decisions',N'Câu chẻ, đảo ngữ và collocation',N'Nhấn mạnh có mục đích; dùng collocation phù hợp văn phong báo cáo; giữ sắc thái thận trọng.',N'1. Chọn thông tin thật sự cần nhấn mạnh trước khi đổi cấu trúc.
2. Dùng What ... is ... để tập trung vào một nhu cầu.
3. Với Not only đứng đầu câu, đảo trợ động từ và chủ ngữ.
4. Chọn cụm quen dùng như draw a conclusion, address a concern; tránh dịch từng từ.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'What the survey reveals is a need for clearer communication. Not only did participation increase, but respondents also offered more detailed feedback. These findings raise concerns about the previous approach.',N'Câu chẻ nhấn nhu cầu giao tiếp rõ hơn. Not only did participation increase đảo did trước chủ ngữ; increase giữ nguyên. Raise concerns là collocation, không phải make concerns.',N'Viết Not only participation increased; nhấn mọi câu khiến văn nặng nề; kết luận chắc chắn hơn bằng chứng.',N'Viết lại một báo cáo 4 câu về khóa học: nhấn nhu cầu chính bằng câu chẻ, thêm một câu Not only và một kết luận thận trọng.',N'What learners need is timely feedback. Not only does regular feedback clarify mistakes, but it also encourages revision. The survey suggests that shorter response times may improve engagement.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=Y_DELbsHzZI',N'https://www.britishcouncil.org/exam/aptis/practice-materials/grammar-vocabulary',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Not only ___ participation increase, but feedback also improved.',N'did',N'had',N'was',0,N'Đảo did trước chủ ngữ; increase giữ nguyên.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which collocation is natural?',N'make a concern',N'raise a concern',N'do a concern',1,N'Raise a concern có nghĩa nêu lên mối lo ngại.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Research and decisions — C1',N'Từ và cụm từ dùng trong bài: Dùng cấu trúc nhấn mạnh và kết hợp từ tự nhiên',N'C1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'draw a conclusion',N'rút ra kết luận',N'C1',1,'ACTIVE'),
 (@set,N'raise a concern',N'nêu mối lo ngại',N'C1',2,'ACTIVE'),
 (@set,N'tentative finding',N'phát hiện mang tính sơ bộ',N'C1',3,'ACTIVE'),
 (@set,N'address an issue',N'giải quyết một vấn đề',N'C1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Đọc tin nhắn và nhận biết thông tin cơ bản' AND skill=N'Reading' AND cefr_level=N'A1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Reading' AND cefr_level=N'A1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Đọc tin nhắn và nhận biết thông tin cơ bản',N'Tìm đúng người, thời gian và nơi hẹn; phân biệt thông tin thay đổi với kế hoạch ban đầu.',N'Reading',N'A1',@order,N'PUBLISHED',N'Meeting a friend',N'Hiểu câu ngắn',N'Tìm đúng người, thời gian và nơi hẹn; phân biệt thông tin thay đổi với kế hoạch ban đầu.',N'1. Đọc câu hỏi và khoanh thông tin cần tìm: who, when, where.
2. Đọc từng câu ngắn, tìm tên riêng và từ chỉ giờ.
3. Chú ý not, but và new để nhận ra thay đổi.
4. Gạch dưới câu trả lời trực tiếp thay vì đoán từ một từ riêng lẻ.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Hi Lan, the art class is at ten on Saturday. Meet me at the school gate at 9:45, not at the café. Bring a pencil. See you, Anna.',N'Ten là giờ học; 9:45 mới là giờ hẹn. Địa điểm hẹn là school gate. Café bị phủ định bởi not.',N'Nhầm giờ bắt đầu lớp với giờ hẹn; chọn địa điểm sau not; trả lời theo kinh nghiệm riêng.',N'Đọc tin nhắn: Hi Minh, our bus leaves at eight. Please meet me at the library at 7:30. Bring water, but don''t bring food. — Kim. Trả lời: gặp lúc mấy giờ, ở đâu và mang gì?',N'Meet at 7:30 at the library. Bring water. The bus leaves later, at eight.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=yiwhUC4S5_o',N'https://www.britishcouncil.org/exam/aptis/practice-material/reading-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Where should Lan meet Anna?',N'At the café',N'At the school gate',N'At the bus stop',1,N'Tin nhắn nói school gate; café bị phủ định.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'What time should Minh meet Kim?',N'7:30',N'8:00',N'9:45',0,N'Please meet ... at 7:30 chỉ giờ gặp.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Meeting a friend — A1',N'Từ và cụm từ dùng trong bài: Đọc tin nhắn và nhận biết thông tin cơ bản',N'A1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'school gate',N'cổng trường',N'A1',1,'ACTIVE'),
 (@set,N'bring',N'mang theo',N'A1',2,'ACTIVE'),
 (@set,N'leave',N'khởi hành',N'A1',3,'ACTIVE'),
 (@set,N'meet',N'gặp',N'A1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Sắp xếp câu bằng dấu hiệu liên kết' AND skill=N'Reading' AND cefr_level=N'B1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Reading' AND cefr_level=N'B1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Sắp xếp câu bằng dấu hiệu liên kết',N'Theo dõi từ chỉ trình tự và đại từ tham chiếu; ghép câu theo quan hệ nguyên nhân–kết quả.',N'Reading',N'B1',@order,N'PUBLISHED',N'Starting a community garden',N'Liên kết văn bản',N'Theo dõi từ chỉ trình tự và đại từ tham chiếu; ghép câu theo quan hệ nguyên nhân–kết quả.',N'1. Xác định câu mở giới thiệu người hoặc sự vật chưa được nhắc trước.
2. Tìm đại từ they, this, it và danh từ mà chúng thay thế.
3. Nối hành động theo thời gian và quan hệ because/so.
4. Đọc lại đoạn đã sắp xếp để kiểm tra mọi tham chiếu đều rõ.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'A. A group of neighbours wanted to grow vegetables.
B. First, they asked the council for an unused piece of land.
C. Once permission arrived, they cleared the site.
D. This preparation allowed them to plant their first seeds.',N'A giới thiệu nhóm. They ở B tham chiếu neighbours. C cần permission được hỏi ở B. This preparation ở D chỉ việc dọn đất trong C.',N'Đặt this trước khi có ý tham chiếu; chỉ dựa vào một từ First mà bỏ qua logic toàn đoạn.',N'Sắp xếp ba câu sau sau câu mở ''Mai decided to organise a book exchange.'': X. By the end of the afternoon, everyone had chosen a new book. Y. She first invited her neighbours to bring books. Z. On the day, they placed their books on a large table.',N'Y → Z → X. She chỉ Mai; they chỉ neighbours; By the end đánh dấu giai đoạn kết thúc.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=yiwhUC4S5_o',N'https://www.britishcouncil.org/exam/aptis/practice-material/reading-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'In the example, what does ''they'' in B refer to?',N'The vegetables',N'The council',N'The neighbours',2,N'They chỉ nhóm hàng xóm đã được giới thiệu.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which order completes Mai''s story?',N'X–Y–Z',N'Y–Z–X',N'Z–X–Y',1,N'Mời trước, đặt sách lên bàn rồi kết thúc buổi trao đổi.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Starting a community garden — B1',N'Từ và cụm từ dùng trong bài: Sắp xếp câu bằng dấu hiệu liên kết',N'B1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'permission',N'sự cho phép',N'B1',1,'ACTIVE'),
 (@set,N'refer to',N'tham chiếu đến',N'B1',2,'ACTIVE'),
 (@set,N'by the end',N'vào cuối',N'B1',3,'ACTIVE'),
 (@set,N'once',N'một khi; sau khi',N'B1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Ghép quan điểm qua cách diễn đạt tương đương' AND skill=N'Reading' AND cefr_level=N'B2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Reading' AND cefr_level=N'B2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Ghép quan điểm qua cách diễn đạt tương đương',N'Nhận ra paraphrase; phân biệt quan điểm với điều kiện đi kèm; đối chiếu bằng chứng giữa người viết.',N'Reading',N'B2',@order,N'PUBLISHED',N'Remote working',N'Đối chiếu ý kiến người viết',N'Nhận ra paraphrase; phân biệt quan điểm với điều kiện đi kèm; đối chiếu bằng chứng giữa người viết.',N'1. Tóm tắt quan điểm mỗi người trong một câu.
2. Đánh dấu từ thể hiện thái độ: value, worry, prefer.
3. Tìm ý tương đương thay vì từ trùng; flexibility có thể là choosing my hours.
4. Kiểm tra các điều kiện như provided that trước khi ghép.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Lena: Working from home saves my commute, but I miss spontaneous discussions.
Omar: I value choosing my hours, provided the team agrees on a daily overlap.
Jade: Being in the office helps me switch off once I leave.',N'Lena thiếu trao đổi không chuẩn bị trước. Omar ủng hộ linh hoạt có điều kiện thời gian chung. Jade muốn ranh giới giữa công việc và đời sống.',N'Gán mọi câu nói về home cho cùng một quan điểm; bỏ qua điều kiện; biến but thành đồng ý hoàn toàn.',N'Ghép: 1. Wants clearer boundaries. 2. Accepts flexibility with a shared routine. 3. Misses informal interaction. Ghi tên và bằng chứng cho từng ý.',N'1 Jade — switch off once I leave. 2 Omar — provided ... daily overlap. 3 Lena — miss spontaneous discussions.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=yiwhUC4S5_o',N'https://www.britishcouncil.org/exam/aptis/practice-material/reading-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Who supports flexible hours with a condition?',N'Lena',N'Omar',N'Jade',1,N'Omar yêu cầu nhóm có thời gian làm việc trùng nhau.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'''Spontaneous discussions'' most nearly means…',N'planned presentations',N'informal conversations that arise naturally',N'written weekly reports',1,N'Spontaneous nghĩa là nảy sinh tự nhiên, không lên kế hoạch trước.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Remote working — B2',N'Từ và cụm từ dùng trong bài: Ghép quan điểm qua cách diễn đạt tương đương',N'B2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'commute',N'việc đi lại giữa nhà và nơi làm',N'B2',1,'ACTIVE'),
 (@set,N'provided that',N'với điều kiện là',N'B2',2,'ACTIVE'),
 (@set,N'switch off',N'ngừng nghĩ về công việc; thư giãn',N'B2',3,'ACTIVE'),
 (@set,N'spontaneous',N'tự phát; tự nhiên',N'B2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Suy luận lập trường và giới hạn của lập luận' AND skill=N'Reading' AND cefr_level=N'C1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Reading' AND cefr_level=N'C1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Suy luận lập trường và giới hạn của lập luận',N'Phân biệt khẳng định với nhượng bộ; suy luận thái độ; nhận biết giới hạn dữ liệu.',N'Reading',N'C1',@order,N'PUBLISHED',N'Urban green spaces',N'Đọc hàm ý và đánh giá bằng chứng',N'Phân biệt khẳng định với nhượng bộ; suy luận thái độ; nhận biết giới hạn dữ liệu.',N'1. Gạch các từ mức độ: may, necessarily, merely.
2. Phân biệt quan điểm tác giả với ý được dẫn lại để phản biện.
3. Xác định điều tác giả chấp nhận và điều còn dè dặt.
4. Chọn suy luận được hỗ trợ; không nâng một khả năng thành sự chắc chắn.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'The new park is often presented as proof that the district is becoming more inclusive. It certainly offers welcome shade. Yet access on paper is not access in practice: residents working late shifts may never benefit from its restricted opening hours. A visitor count alone cannot settle the question.',N'Tác giả công nhận lợi ích bóng mát nhưng phản bác kết luận bao trùm chỉ dựa vào số lượt khách. May giữ mức độ khả năng; không nói mọi công nhân đều bị loại trừ.',N'Hiểu Yet là phủ định toàn bộ lợi ích; coi visitor count là bằng chứng đủ; suy luận mạnh hơn văn bản.',N'Viết hai câu: lập trường của tác giả và một loại bằng chứng bổ sung cần thu thập. Trích cụm thể hiện sự dè dặt.',N'The author welcomes the park but questions whether all residents can use it. Evidence about shift workers'' access and opening times is needed. ''Cannot settle the question'' limits the conclusion.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=yiwhUC4S5_o',N'https://www.britishcouncil.org/exam/aptis/practice-material/reading-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'What is the author''s main reservation?',N'Parks never help residents.',N'Visitor numbers do not establish fair access.',N'Shade is harmful.',1,N'Tác giả yêu cầu xem khả năng tiếp cận thực tế, không chỉ số lượt khách.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'What does ''may never benefit'' imply?',N'All workers are definitely excluded.',N'There is a possibility some workers cannot benefit.',N'The park has closed permanently.',1,N'May diễn tả khả năng, không phải kết luận tuyệt đối.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Urban green spaces — C1',N'Từ và cụm từ dùng trong bài: Suy luận lập trường và giới hạn của lập luận',N'C1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'inclusive',N'có tính bao trùm',N'C1',1,'ACTIVE'),
 (@set,N'restricted access',N'quyền tiếp cận bị hạn chế',N'C1',2,'ACTIVE'),
 (@set,N'reservation',N'sự dè dặt; điều còn lo ngại',N'C1',3,'ACTIVE'),
 (@set,N'settle the question',N'giải quyết dứt điểm vấn đề',N'C1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Nghe thông báo thay đổi lịch trình' AND skill=N'Listening' AND cefr_level=N'A2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Listening' AND cefr_level=N'A2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Nghe thông báo thay đổi lịch trình',N'Ghi đúng kế hoạch cuối cùng sau khi người nói sửa giờ hoặc địa điểm.',N'Listening',N'A2',@order,N'PUBLISHED',N'Travel arrangements',N'Nghe thời gian, địa điểm và thông tin thay thế',N'Ghi đúng kế hoạch cuối cùng sau khi người nói sửa giờ hoặc địa điểm.',N'1. Trước khi nghe, tạo ba ô: time, place, action.
2. Lượt đầu ghi kế hoạch chung.
3. Lượt sau chú ý not ... but, now và instead; gạch bỏ thông tin cũ.
4. Kiểm tra bạn đã trả lời kế hoạch cuối chứ không phải lời đề nghị ban đầu.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Ví dụ cách ghi: ''We planned to meet at two, but let''s make it three.'' → old: 2:00; final: 3:00.',N'But chuyển từ kế hoạch cũ sang quyết định mới. Không chọn two chỉ vì xuất hiện trước.',N'Chọn giờ đầu tiên; bỏ qua nơi tập trung; đọc transcript trước khi tự nghe.',N'Nghe thông báo về chuyến đi hai lượt. Ghi giờ xe rời đi, nơi tập trung và hai thứ cần mang. Sau khi trả lời câu hỏi, mở transcript để kiểm tra.',N'The coach leaves at 9:15. Meet at the north gate by 9:00. Bring a raincoat and water. Lunch is provided.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=cbGwglNyEaQ',N'https://www.britishcouncil.org/exam/aptis/practice-materials/listening-aptis-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',N'Hello everyone. Here is an update about our museum trip tomorrow. The coach was due to leave at nine, but the driver has asked us to leave at a quarter past nine instead. Please meet at the north gate by nine o''clock, not at the main entrance. The weather may be wet, so bring a raincoat and some water. You do not need to bring food because the museum café will provide lunch. We should be back by five.');
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'What time will the coach leave?',N'9:00',N'9:15',N'9:45',1,N'A quarter past nine là 9:15; đây là giờ sau khi thay đổi.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Where should everyone meet?',N'At the main entrance',N'At the café',N'At the north gate',2,N'Thông báo chọn north gate và phủ định main entrance.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Travel arrangements — A2',N'Từ và cụm từ dùng trong bài: Nghe thông báo thay đổi lịch trình',N'A2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'a quarter past',N'... giờ mười lăm',N'A2',1,'ACTIVE'),
 (@set,N'be due to',N'dự kiến sẽ',N'A2',2,'ACTIVE'),
 (@set,N'north gate',N'cổng phía bắc',N'A2',3,'ACTIVE'),
 (@set,N'provide lunch',N'cung cấp bữa trưa',N'A2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Ghép người nói với lý do lựa chọn' AND skill=N'Listening' AND cefr_level=N'B1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Listening' AND cefr_level=N'B1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Ghép người nói với lý do lựa chọn',N'Phân biệt lý do chính với chi tiết phụ; ghép người nói qua ý tương đương.',N'Listening',N'B1',@order,N'PUBLISHED',N'Choosing evening courses',N'Nghe ý chính và động cơ',N'Phân biệt lý do chính với chi tiết phụ; ghép người nói qua ý tương đương.',N'1. Đọc các lý do và ghi một từ khóa cho mỗi ý.
2. Mỗi lượt nói, ghi lựa chọn và lý do sau because/so.
3. Lượt hai kiểm tra ý nào chỉ là chi tiết phụ.
4. Ghép theo thông điệp chứ không theo tên môn học.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'''The course is nearby, but the real attraction is the chance to meet new people.'' → lý do chính: giao lưu; vị trí chỉ là chi tiết phụ.',N'The real attraction nhấn lý do quyết định. But giúp tách lý do chính khỏi yếu tố thuận tiện.',N'Chọn lý do đầu tiên được nhắc; ghép theo từ course; bỏ qua mục tiêu dài hạn.',N'Nghe ba người chọn khóa học. Ghép Anna, Ben, Cara với lý do: phát triển nghề nghiệp; sở thích sáng tạo; gặp gỡ mọi người. Nêu bằng chứng.',N'Anna: career development — apply for a management role. Ben: a creative hobby — enjoy making things. Cara: social contact — meet people locally.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=cbGwglNyEaQ',N'https://www.britishcouncil.org/exam/aptis/practice-materials/listening-aptis-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',N'Anna speaking. I have signed up for a public speaking course. My friends think I want to perform on stage, but that is not my plan. I need to lead meetings confidently so that I can apply for a management role. Ben here. I spend all day working with numbers. In the evening I want to do something with my hands, so I chose pottery. I have no intention of selling what I make. Cara speaking. I recently moved to this town and hardly know anyone. The cooking course sounds interesting, but learning recipes is not my main reason for joining. I hope to meet people who live nearby.');
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Why did Anna choose public speaking?',N'To perform on stage',N'To develop her career',N'To teach pottery',1,N'Anna muốn dẫn họp tự tin và ứng tuyển vị trí quản lý.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Who mainly wants social contact?',N'Anna',N'Ben',N'Cara',2,N'Cara nói mục tiêu chính là gặp người sống gần mình.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Choosing evening courses — B1',N'Từ và cụm từ dùng trong bài: Ghép người nói với lý do lựa chọn',N'B1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'sign up for',N'đăng ký',N'B1',1,'ACTIVE'),
 (@set,N'management role',N'vị trí quản lý',N'B1',2,'ACTIVE'),
 (@set,N'have no intention of',N'không có ý định',N'B1',3,'ACTIVE'),
 (@set,N'main reason',N'lý do chính',N'B1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Theo dõi sự đồng ý có điều kiện trong thảo luận' AND skill=N'Listening' AND cefr_level=N'B2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Listening' AND cefr_level=N'B2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Theo dõi sự đồng ý có điều kiện trong thảo luận',N'Nhận ra đồng ý một phần; phân biệt đề xuất thử nghiệm với quyết định lâu dài.',N'Listening',N'B2',@order,N'PUBLISHED',N'Hybrid working',N'Nghe quan điểm và điều kiện',N'Nhận ra đồng ý một phần; phân biệt đề xuất thử nghiệm với quyết định lâu dài.',N'1. Dự đoán hai phía của vấn đề trước khi nghe.
2. Ghi người nói và lập trường bằng dấu cộng, trừ hoặc có điều kiện.
3. Chú ý I agree up to a point, as long as, rather than.
4. Kiểm tra kết luận cuối; đừng đồng nhất sự nhượng bộ với đồng ý hoàn toàn.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'''I can support the plan as long as we review it after a month.'' → đồng ý có điều kiện đánh giá lại, chưa phải ủng hộ vô điều kiện.',N'As long as đặt điều kiện bắt buộc. Review là đánh giá lại nên kế hoạch chưa cố định.',N'Ghi agreed mà bỏ điều kiện; lẫn ý ban đầu với quyết định cuối; chọn theo một từ giống đáp án.',N'Nghe cuộc thảo luận. Ghi một lợi ích, một rủi ro và điều kiện để kế hoạch được tiếp tục. Phân biệt pilot và permanent policy.',N'Benefit: fewer unnecessary journeys. Risk: new staff may miss support. Condition: review evidence after six weeks before deciding on a permanent policy.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=cbGwglNyEaQ',N'https://www.britishcouncil.org/exam/aptis/practice-materials/listening-aptis-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',N'Maya began the discussion by suggesting that the team should work from home three days a week. She argued that fewer journeys would save time and reduce stress. Daniel said he could see the benefits, but was concerned that new colleagues might struggle to get informal help. Maya replied that they could set common office days and keep a short online drop in session every morning. Daniel agreed that this addressed part of his concern. However, he did not want the arrangement to become permanent immediately. They eventually agreed on a six week pilot. During that period, they would collect feedback from new staff as well as compare project deadlines. If communication suffered, they would adjust the number of home working days. Maya concluded that a trial based on evidence was better than either rejecting the idea outright or treating it as a guaranteed success.');
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'What do they finally agree to?',N'A permanent three-day home policy',N'A six-week pilot followed by review',N'Ending all home working',1,N'Kết luận là thử sáu tuần và đánh giá dữ liệu.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'What is Daniel''s main concern?',N'Travel costs are too high.',N'New colleagues may lack informal support.',N'Project deadlines are unnecessary.',1,N'Daniel lo người mới khó nhận trợ giúp không chính thức.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Hybrid working — B2',N'Từ và cụm từ dùng trong bài: Theo dõi sự đồng ý có điều kiện trong thảo luận',N'B2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'up to a point',N'đến một mức độ nhất định',N'B2',1,'ACTIVE'),
 (@set,N'pilot scheme',N'chương trình thử nghiệm',N'B2',2,'ACTIVE'),
 (@set,N'informal support',N'hỗ trợ không chính thức',N'B2',3,'ACTIVE'),
 (@set,N'reject outright',N'bác bỏ hoàn toàn',N'B2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Nghe sự dè dặt và hàm ý trong một bài bình luận' AND skill=N'Listening' AND cefr_level=N'C1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Listening' AND cefr_level=N'C1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Nghe sự dè dặt và hàm ý trong một bài bình luận',N'Phân biệt lợi ích quan sát được với kết luận nhân quả; nhận ra thái độ ủng hộ có dè dặt.',N'Listening',N'C1',@order,N'PUBLISHED',N'Evaluating a city policy',N'Suy luận thái độ và giới hạn bằng chứng',N'Phân biệt lợi ích quan sát được với kết luận nhân quả; nhận ra thái độ ủng hộ có dè dặt.',N'1. Tách claim, evidence và qualification trong ghi chú.
2. Nghe các từ dè dặt: tempting, premature, not necessarily.
3. Xác định tác giả đang phản đối mục tiêu hay chất lượng bằng chứng.
4. Suy luận thái độ từ toàn bài; dùng lượt nghe hai kiểm tra cụm làm thay đổi mức độ chắc chắn.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'''Encouraging as the figures are, they do not establish causation.'' → số liệu tích cực, nhưng chưa chứng minh nguyên nhân.',N'Cấu trúc nhượng bộ công nhận kết quả rồi giới hạn cách diễn giải. Tác giả không phủ nhận số liệu, chỉ phản đối kết luận quá sớm.',N'Hiểu dè dặt thành phủ nhận mọi lợi ích; đồng nhất correlation và causation; bỏ phần sau however.',N'Nghe bài bình luận. Tóm tắt lập trường trong hai câu, chỉ ra hai yếu tố có thể ảnh hưởng kết quả và đề xuất bước đánh giá tiếp theo.',N'The speaker welcomes the scheme but regards a causal claim as premature. Seasonal weather and a publicity campaign could explain part of the increase. A longer study with comparable districts is needed.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=cbGwglNyEaQ',N'https://www.britishcouncil.org/exam/aptis/practice-materials/listening-aptis-test',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',N'The council has announced that its cycle hire scheme has transformed local travel, pointing to a sharp rise in registered journeys. Encouraging as those figures are, the claim deserves a closer look. The scheme began at the start of an unusually dry spring, while a major publicity campaign offered free rides during the first month. Neither factor makes the increase meaningless. They do, however, make it difficult to attribute the whole change to the scheme itself. There is a second difficulty. Registration data tell us how often bicycles were borrowed, not which journeys they replaced. A short leisure ride that would not otherwise have happened is not equivalent to a car journey avoided. It would therefore be premature to declare a substantial reduction in traffic. What is needed is a longer study, including user interviews and comparison with districts where no scheme was introduced. My reservation concerns the strength of the evidence, not the ambition of making transport cleaner. If the council is willing to evaluate the scheme carefully and adjust it in response to what it finds, the initial figures could be a useful starting point rather than a verdict.');
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'What is the speaker''s attitude?',N'Supportive of the aim but cautious about the evidence',N'Certain the scheme has failed',N'Certain all rides replaced car trips',0,N'Người nói ủng hộ mục tiêu nhưng dè dặt với kết luận nhân quả.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'What does borrowing data alone fail to show?',N'Whether the bicycles were borrowed',N'Which previous journeys were replaced',N'Whether any people registered',1,N'Dữ liệu lượt mượn chưa nói chuyến đi đó thay thế cách di chuyển nào.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Evaluating a city policy — C1',N'Từ và cụm từ dùng trong bài: Nghe sự dè dặt và hàm ý trong một bài bình luận',N'C1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'attribute to',N'quy cho; cho là do',N'C1',1,'ACTIVE'),
 (@set,N'causation',N'quan hệ nhân quả',N'C1',2,'ACTIVE'),
 (@set,N'premature',N'quá sớm; vội vàng',N'C1',3,'ACTIVE'),
 (@set,N'a verdict',N'kết luận mang tính quyết định',N'C1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Viết thông tin cá nhân và câu trả lời ngắn' AND skill=N'Writing' AND cefr_level=N'A1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Writing' AND cefr_level=N'A1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Viết thông tin cá nhân và câu trả lời ngắn',N'Trả lời đúng câu hỏi về bản thân bằng từ hoặc câu ngắn; dùng chữ hoa và dấu chấm.',N'Writing',N'A1',@order,N'PUBLISHED',N'Joining a club',N'Điền thông tin và viết câu đơn',N'Trả lời đúng câu hỏi về bản thân bằng từ hoặc câu ngắn; dùng chữ hoa và dấu chấm.',N'1. Tìm từ hỏi: name, live, like.
2. Chọn đúng thông tin thay vì kể tất cả về bản thân.
3. Với câu hoàn chỉnh, viết chủ ngữ + động từ + thông tin.
4. Viết hoa I, tên người và địa danh; kiểm tra dấu kết thúc.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Question: Where do you live?
Answer: I live in Hanoi.
Question: What do you like doing?
Answer: I like reading.',N'Where hỏi nơi chốn nên câu đầu có Hanoi. Câu về sở thích dùng like + V-ing. Mỗi câu chỉ cần trả lời đúng một yêu cầu.',N'Trả lời tuổi cho câu hỏi nơi ở; bỏ động từ; viết i thường; dùng like read khi diễn tả thích hoạt động.',N'Bạn đăng ký câu lạc bộ đọc sách. Viết câu trả lời cho: What is your name? Where do you live? What do you like doing after school or work?',N'My name is Linh. I live in Hue. I like reading short stories after work.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=wumg5p4xeJY',N'https://www.britishcouncil.org/exam/aptis/take-test/writing-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Which answer fits ''Where do you live?''?',N'I am twenty.',N'I live in Hue.',N'I like music.',1,N'Where hỏi địa điểm.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which sentence is correctly written?',N'i live in hue',N'I live Hue.',N'I live in Hue.',2,N'Cần I và Hue viết hoa, giới từ in và dấu chấm.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Joining a club — A1',N'Từ và cụm từ dùng trong bài: Viết thông tin cá nhân và câu trả lời ngắn',N'A1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'first name',N'tên gọi',N'A1',1,'ACTIVE'),
 (@set,N'live in',N'sống ở',N'A1',2,'ACTIVE'),
 (@set,N'after work',N'sau giờ làm',N'A1',3,'ACTIVE'),
 (@set,N'short story',N'truyện ngắn',N'A1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Viết đoạn ngắn về một hoạt động cuối tuần' AND skill=N'Writing' AND cefr_level=N'A2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Writing' AND cefr_level=N'A2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Viết đoạn ngắn về một hoạt động cuối tuần',N'Viết đoạn 40–60 từ có thời gian, hoạt động, người đi cùng và cảm nhận; dùng từ nối cơ bản.',N'Writing',N'A2',@order,N'PUBLISHED',N'Weekend activities',N'Đoạn mô tả có trình tự',N'Viết đoạn 40–60 từ có thời gian, hoạt động, người đi cùng và cảm nhận; dùng từ nối cơ bản.',N'1. Chọn một hoạt động cụ thể.
2. Ghi bốn ý: when, where, who, feeling.
3. Viết câu chủ đề rồi thêm hai hoặc ba chi tiết theo trình tự.
4. Nối bằng and, then, because; dùng quá khứ nhất quán nếu kể cuối tuần trước.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Last Sunday, I visited a market with my cousin. We bought vegetables and tried a local snack. Then we walked home. I enjoyed the morning because the market was lively.',N'Câu đầu giới thiệu thời gian, nơi và người. Then nối trình tự; because giải thích cảm nhận. Các động từ đều kể quá khứ.',N'Liệt kê từ rời; đổi sang hiện tại giữa đoạn; viết cảm nhận nhưng không có lý do.',N'Viết 40–60 từ về một hoạt động cuối tuần đã qua. Nêu thời gian, nơi, người đi cùng, hai việc đã làm và cảm nhận.',N'Last Saturday, I went to a small park with my sister. We rode our bikes and took some photos near the lake. Then we had lunch at home. I enjoyed the trip because the park was quiet and the weather was cool.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=wumg5p4xeJY',N'https://www.britishcouncil.org/exam/aptis/take-test/writing-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Which word gives a reason?',N'Then',N'Because',N'And',1,N'Because nối cảm nhận với lý do.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which opening gives a clear past time?',N'Sometimes I like parks.',N'Next week I will go.',N'Last Saturday, I visited a park.',2,N'Last Saturday cho mốc quá khứ rõ ràng.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Weekend activities — A2',N'Từ và cụm từ dùng trong bài: Viết đoạn ngắn về một hoạt động cuối tuần',N'A2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'lively',N'nhộn nhịp',N'A2',1,'ACTIVE'),
 (@set,N'take photos',N'chụp ảnh',N'A2',2,'ACTIVE'),
 (@set,N'spend time with',N'dành thời gian cùng',N'A2',3,'ACTIVE'),
 (@set,N'local snack',N'món ăn vặt địa phương',N'A2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Điều chỉnh giọng văn trong hai email cùng chủ đề' AND skill=N'Writing' AND cefr_level=N'B2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Writing' AND cefr_level=N'B2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Điều chỉnh giọng văn trong hai email cùng chủ đề',N'Chuyển giọng theo người nhận; nêu sự việc, ảnh hưởng và yêu cầu cụ thể; dùng cách diễn đạt lịch sự.',N'Writing',N'B2',@order,N'PUBLISHED',N'A cancelled course',N'Email thân mật và trang trọng',N'Chuyển giọng theo người nhận; nêu sự việc, ảnh hưởng và yêu cầu cụ thể; dùng cách diễn đạt lịch sự.',N'1. Giữ ba nội dung: điều xảy ra, ảnh hưởng, điều bạn muốn.
2. Với bạn bè, dùng câu tự nhiên và lời chào thân mật.
3. Với ban tổ chức, dùng opening và request lịch sự, tránh cảm xúc quy chụp.
4. Kiểm tra xem yêu cầu cuối có rõ và khả thi không.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'To a friend: Hi Sam, I''m disappointed our workshop was cancelled. Shall we try the Saturday class instead?
To the organiser: Dear Course Coordinator, I am writing about the cancellation of the workshop. Could you confirm whether an alternative session or a refund is available?',N'Hai email có cùng sự việc. Email cho bạn dùng contractions và đề nghị cùng làm; email trang trọng xác định mục đích rồi yêu cầu thông tin rõ ràng.',N'Dùng Hey với ban tổ chức; dùng đe dọa thay cho yêu cầu; viết dài nhưng không nói muốn chuyển lớp hay hoàn tiền.',N'Viết tin 40–50 từ cho Sam và email 120–150 từ cho ban tổ chức về khóa học bị đổi lịch. Nêu ảnh hưởng tới bạn và đề nghị giải pháp. Đây là độ dài luyện tập của bài.',N'Dear Course Coordinator,
I am writing about the revised timetable for the photography course. I enrolled because the original sessions were on Saturday mornings. The new weekday schedule conflicts with my working hours, so I would be unable to attend most sessions.
I appreciate that timetables sometimes need to change. However, I would be grateful if you could offer a place on a weekend course or transfer my registration to the next intake. If neither option is possible, please explain the refund procedure. I am still interested in the course and would prefer to attend at a suitable time.
Could you confirm the available options by Friday? Thank you for your assistance.
Kind regards,
Minh',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=wumg5p4xeJY',N'https://www.britishcouncil.org/exam/aptis/take-test/writing-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Which request suits a course organiser?',N'Give me my money now!',N'Could you explain the available alternatives?',N'Hey, what''s going on?',1,N'Could you ... lịch sự và yêu cầu thông tin cụ thể.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'What should remain consistent across both emails?',N'The level of formality',N'The exact greeting',N'The facts about the schedule change',2,N'Sự việc phải nhất quán, giọng văn thay đổi theo người nhận.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'A cancelled course — B2',N'Từ và cụm từ dùng trong bài: Điều chỉnh giọng văn trong hai email cùng chủ đề',N'B2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'revised timetable',N'lịch trình điều chỉnh',N'B2',1,'ACTIVE'),
 (@set,N'alternative session',N'buổi học thay thế',N'B2',2,'ACTIVE'),
 (@set,N'refund procedure',N'thủ tục hoàn tiền',N'B2',3,'ACTIVE'),
 (@set,N'I would be grateful if…',N'Tôi rất cảm kích nếu…',N'B2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Viết đề xuất có đánh giá phương án và phản biện' AND skill=N'Writing' AND cefr_level=N'C1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Writing' AND cefr_level=N'C1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Viết đề xuất có đánh giá phương án và phản biện',N'So sánh phương án theo tiêu chí; công nhận hạn chế; đề xuất giải pháp gắn với bằng chứng.',N'Writing',N'C1',@order,N'PUBLISHED',N'Community learning space',N'Đề xuất có lập luận và khuyến nghị',N'So sánh phương án theo tiêu chí; công nhận hạn chế; đề xuất giải pháp gắn với bằng chứng.',N'1. Xác định người nhận và quyết định họ cần đưa ra.
2. Chọn hai tiêu chí nhất quán để so sánh các phương án.
3. Mỗi đoạn có luận điểm, bằng chứng và hệ quả.
4. Đưa phản biện mạnh nhất và cách giảm rủi ro.
5. Kết luận có hành động cụ thể và tiêu chí đánh giá, tránh khẳng định tuyệt đối.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Extending the library''s hours would improve access for shift workers. However, a full seven-day extension would increase staffing costs. A two-evening pilot, evaluated through attendance and user feedback, would test demand before a permanent commitment.',N'Đề xuất không né chi phí. Pilot giới hạn rủi ro và gắn quyết định dài hạn với bằng chứng. Would và could giữ giọng thận trọng.',N'Chỉ liệt kê ưu điểm; bỏ rủi ro; kết luận tuyệt đối dù dữ liệu ít; dùng từ nối hình thức nhưng không có logic.',N'Viết đề xuất 180–220 từ cho trung tâm cộng đồng: so sánh mở thêm phòng học với kéo dài giờ mở cửa. Nêu tiêu chí, hạn chế, khuyến nghị và cách đánh giá sau ba tháng.',N'I recommend testing extended opening hours before investing in an additional room. The main criteria should be access and cost.
A new room could reduce crowding at peak times, but it would do little for residents whose work prevents them from attending during the day. Extending two evenings a week would address that barrier directly and require less initial investment.
The main concern is the recurring cost of staffing. A limited pilot would make this manageable. Attendance should be recorded by time slot, while short user surveys could establish whether evening access reaches people who currently cannot use the centre.
After three months, the committee could compare the cost per additional visitor with the projected cost of expansion. If evenings attract little new demand, the budget could instead support a smaller room conversion. This approach would preserve flexibility and allow the eventual decision to rest on local evidence.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=wumg5p4xeJY',N'https://www.britishcouncil.org/exam/aptis/take-test/writing-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'What makes the pilot proposal well supported?',N'It guarantees success.',N'It avoids all costs.',N'It tests demand using attendance and feedback.',2,N'Thử nghiệm có dữ liệu đánh giá trước khi cam kết lâu dài.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which sentence acknowledges a limitation?',N'The main concern is the recurring cost of staffing.',N'The plan is perfect.',N'Everyone will benefit equally.',0,N'Câu nêu rõ hạn chế chi phí vận hành.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Community learning space — C1',N'Từ và cụm từ dùng trong bài: Viết đề xuất có đánh giá phương án và phản biện',N'C1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'recurring cost',N'chi phí định kỳ',N'C1',1,'ACTIVE'),
 (@set,N'evaluate demand',N'đánh giá nhu cầu',N'C1',2,'ACTIVE'),
 (@set,N'initial investment',N'đầu tư ban đầu',N'C1',3,'ACTIVE'),
 (@set,N'preserve flexibility',N'giữ tính linh hoạt',N'C1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Giới thiệu bản thân bằng câu ngắn rõ ý' AND skill=N'Speaking' AND cefr_level=N'A1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Speaking' AND cefr_level=N'A1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Giới thiệu bản thân bằng câu ngắn rõ ý',N'Nói tên, nơi ở và sở thích bằng câu đầy đủ; nói rõ và ngắt giữa các ý.',N'Speaking',N'A1',@order,N'PUBLISHED',N'Personal information',N'Trả lời câu hỏi cá nhân cơ bản',N'Nói tên, nơi ở và sở thích bằng câu đầy đủ; nói rõ và ngắt giữa các ý.',N'1. Chuẩn bị ba từ khóa về tên, thành phố và sở thích.
2. Trả lời trực tiếp bằng I am, I live, I like.
3. Nói từng nhóm từ ngắn; ngắt tự nhiên giữa câu.
4. Ghi âm và kiểm tra người nghe có hiểu ba thông tin không.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'My name is Hanh. I live in Hanoi. I like cooking. I often cook with my mother.',N'Mỗi câu làm một nhiệm vụ. Câu cuối mở rộng sở thích bằng một chi tiết quen thuộc; không cần dùng từ khó.',N'Chỉ nói tên thành phố mà không thành câu; nói quá nhanh; học thuộc thông tin không đúng bản thân.',N'Ghi âm 20–30 giây trả lời: What is your name? Where do you live? What do you like doing? Sau đó nghe lại và viết một điều cần cải thiện.',N'My name is Linh. I live in a small town near Hue. I like reading. I read short stories after work.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=1c5TCWR6Fyg',N'https://www.britishcouncil.org/exam/aptis/take-test/speaking-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Which sentence answers ''Where do you live?''?',N'I live in Hue.',N'I am nineteen.',N'I like football.',0,N'Câu đầu cung cấp nơi ở.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'What is useful when practising?',N'Speaking as fast as possible',N'Listening to your recording',N'Using only difficult words',1,N'Nghe lại giúp nhận ra thông tin chưa rõ.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Personal information — A1',N'Từ và cụm từ dùng trong bài: Giới thiệu bản thân bằng câu ngắn rõ ý',N'A1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'my name is',N'tên tôi là',N'A1',1,'ACTIVE'),
 (@set,N'I live in',N'tôi sống ở',N'A1',2,'ACTIVE'),
 (@set,N'I like cooking',N'tôi thích nấu ăn',N'A1',3,'ACTIVE'),
 (@set,N'near',N'gần',N'A1',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Mô tả một cảnh sinh hoạt theo thứ tự' AND skill=N'Speaking' AND cefr_level=N'A2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Speaking' AND cefr_level=N'A2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Mô tả một cảnh sinh hoạt theo thứ tự',N'Mô tả những gì quan sát được bằng hiện tại tiếp diễn; phân biệt quan sát và suy đoán.',N'Speaking',N'A2',@order,N'PUBLISHED',N'A weekend in the park',N'Mô tả người, hành động và địa điểm',N'Mô tả những gì quan sát được bằng hiện tại tiếp diễn; phân biệt quan sát và suy đoán.',N'1. Mở bằng địa điểm và cảnh chung.
2. Chọn hai hoặc ba người/hành động rõ nhất.
3. Dùng on the left, in the background để sắp xếp mô tả.
4. Nếu chưa chắc, dùng perhaps thay cho khẳng định.
5. Kết bằng cảm nhận ngắn có lý do.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'In this scene, several people are in a park. On the left, a woman is reading on a bench. Two children are playing near a tree. It looks peaceful.',N'Is reading và are playing mô tả hành động đang diễn ra. On the left định vị; looks cho biết cảm nhận chứ không phải dữ kiện chắc chắn.',N'Liệt kê mọi đồ vật; quên be trong hiện tại tiếp diễn; đoán nghề nghiệp như thông tin chắc chắn.',N'Dùng ảnh sinh hoạt do bạn chọn hoặc cảnh bằng lời sau: a father preparing food; two children playing; a dog near a bench. Nói 30–40 giây theo thứ tự cảnh chung → chi tiết → cảm nhận.',N'There is a family in a park. The father is preparing some food, and the children are playing together. A dog is sitting near a bench. Perhaps they are having a picnic. The scene looks relaxed.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=1c5TCWR6Fyg',N'https://www.britishcouncil.org/exam/aptis/take-test/speaking-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Which sentence describes an action in progress?',N'She reading.',N'She is reading.',N'She reads yesterday.',1,N'Hiện tại tiếp diễn: be + V-ing.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which word marks an uncertain inference?',N'Definitely',N'Always',N'Perhaps',2,N'Perhaps cho thấy thông tin là suy đoán.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'A weekend in the park — A2',N'Từ và cụm từ dùng trong bài: Mô tả một cảnh sinh hoạt theo thứ tự',N'A2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'in the background',N'ở phía sau',N'A2',1,'ACTIVE'),
 (@set,N'on the left',N'ở bên trái',N'A2',2,'ACTIVE'),
 (@set,N'it looks peaceful',N'cảnh trông yên bình',N'A2',3,'ACTIVE'),
 (@set,N'perhaps',N'có lẽ',N'A2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'So sánh hai lựa chọn và bảo vệ ý kiến' AND skill=N'Speaking' AND cefr_level=N'B2';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Speaking' AND cefr_level=N'B2';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'So sánh hai lựa chọn và bảo vệ ý kiến',N'Nêu điểm chung và khác; giải thích sở thích theo tiêu chí; công nhận một lợi thế của phương án còn lại.',N'Speaking',N'B2',@order,N'PUBLISHED',N'Ways of learning',N'So sánh, nêu ưu nhược điểm và sở thích',N'Nêu điểm chung và khác; giải thích sở thích theo tiêu chí; công nhận một lợi thế của phương án còn lại.',N'1. Nêu chủ đề chung của hai lựa chọn.
2. So sánh theo cùng tiêu chí như interaction và flexibility.
3. Chọn sở thích và giải thích bằng ví dụ.
4. Công nhận một lợi thế phía kia trước khi kết luận.
5. Dùng whereas, while, although đúng quan hệ ý.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'Both online and classroom courses can provide structured learning. Online classes offer flexibility, whereas face-to-face lessons make spontaneous discussion easier. I prefer classroom learning because I stay more focused, although online courses suit people with irregular schedules.',N'Both nêu điểm chung; whereas so sánh khác biệt theo cách học. Sở thích có lý do và nhượng bộ, nên không biến thành khẳng định một phương án luôn tốt hơn.',N'Mô tả từng lựa chọn mà không so sánh; nói I prefer nhưng thiếu lý do; phủ nhận hoàn toàn phương án khác.',N'Nói 60–90 giây so sánh học một mình với học theo nhóm. Nêu một điểm chung, hai khác biệt, sở thích cá nhân và một trường hợp phương án kia phù hợp.',N'Both approaches can help learners revise. Studying alone gives me control over the pace, whereas group study exposes me to different explanations. I prefer a small group for difficult topics because discussing mistakes helps me notice gaps. However, I work alone when I need to memorise vocabulary quietly.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=1c5TCWR6Fyg',N'https://www.britishcouncil.org/exam/aptis/take-test/speaking-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Which phrase introduces a contrast?',N'For example',N'Whereas',N'As a result',1,N'Whereas đối chiếu hai ý khác nhau.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'Which answer supports a preference?',N'Groups are best. That''s all.',N'I like groups because explaining mistakes helps me learn.',N'Everybody agrees with me.',1,N'Câu có lựa chọn và lý do cụ thể.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Ways of learning — B2',N'Từ và cụm từ dùng trong bài: So sánh hai lựa chọn và bảo vệ ý kiến',N'B2','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'at my own pace',N'theo nhịp độ riêng',N'B2',1,'ACTIVE'),
 (@set,N'face-to-face',N'trực tiếp',N'B2',2,'ACTIVE'),
 (@set,N'whereas',N'trong khi; trái lại',N'B2',3,'ACTIVE'),
 (@set,N'notice gaps',N'nhận ra lỗ hổng kiến thức',N'B2',4,'ACTIVE');
END;
GO

DECLARE @lesson INT, @set INT, @order INT;
SELECT @lesson=id FROM lessons WHERE title=N'Trình bày quan điểm có sắc thái và xử lý phản biện' AND skill=N'Speaking' AND cefr_level=N'C1';
IF @lesson IS NULL
BEGIN
 SELECT @order=COALESCE(MAX(display_order),0)+1 FROM lessons WHERE skill=N'Speaking' AND cefr_level=N'C1';
 INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,transcript)
 VALUES(N'Trình bày quan điểm có sắc thái và xử lý phản biện',N'Phát triển lập luận có điều kiện; nhượng bộ hợp lý; trả lời phản biện mà vẫn giữ mạch nói.',N'Speaking',N'C1',@order,N'PUBLISHED',N'Technology in education',N'Lập luận mở rộng và phản hồi ý kiến đối lập',N'Phát triển lập luận có điều kiện; nhượng bộ hợp lý; trả lời phản biện mà vẫn giữ mạch nói.',N'1. Nêu lập trường có phạm vi: under certain conditions, to some extent.
2. Giải thích cơ chế thay vì chỉ đưa khẩu hiệu.
3. Thêm ví dụ và một hạn chế của lập luận.
4. Khi phản biện, tóm tắt công bằng ý đối lập rồi trả lời phần cốt lõi.
5. Kết bằng điều kiện thực hiện cụ thể.
Video đi kèm giới thiệu cách làm và cấu trúc kỹ năng; ví dụ và bài thực hành của bài được biên soạn riêng.',N'I support using automated feedback for first drafts, provided teachers remain involved. It can flag recurring errors quickly, leaving more time for discussion. Admittedly, a system may misread an unusual but effective expression. That is precisely why learners should be able to question the feedback.',N'Provided đặt điều kiện. Admittedly công nhận hạn chế thật; câu sau phản hồi bằng cơ chế xem xét lại, thay vì né vấn đề.',N'Khẳng định công nghệ luôn đúng hoặc luôn sai; dùng từ nhượng bộ mà không có nhượng bộ thật; trả lời lệch phản biện.',N'Nói khoảng hai phút về việc dùng công cụ phản hồi tự động khi học ngôn ngữ. Nêu lập trường, ví dụ, rủi ro và điều kiện sử dụng. Sau đó trả lời: ''Could students become too dependent on these tools?''',N'There is a risk of dependency if feedback replaces reflection. I would therefore ask learners to explain which suggestions they accepted and why. Used that way, the tool becomes a starting point for discussion. It should not make the final judgement about a learner''s meaning.',N'Tôi đã thực hiện các bước trong phần Cách làm chưa?
Tôi có thể giải thích bằng chứng hoặc lý do cho câu trả lời không?
Tôi đã đối chiếu bài mẫu và ghi lại một lỗi cần sửa chưa?',N'https://www.youtube.com/watch?v=1c5TCWR6Fyg',N'https://www.britishcouncil.org/exam/aptis/take-test/speaking-video',N'AptisPrep — bài tự biên soạn; tham khảo cấu trúc British Council',NULL);
 SET @lesson=SCOPE_IDENTITY();
END;
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=1)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,1,N'Which phrase qualifies support?',N'Under all circumstances',N'Provided teachers remain involved',N'Without any limits',1,N'Provided nêu điều kiện cho sự ủng hộ.');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@lesson AND position=2)
 INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation)
 VALUES(@lesson,2,N'How does the sample address dependency?',N'By denying the risk',N'By asking learners to justify accepted suggestions',N'By removing all feedback',1,N'Yêu cầu giải thích giúp duy trì suy nghĩ độc lập.');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,visibility,lesson_id)
 VALUES(N'Technology in education — C1',N'Từ và cụm từ dùng trong bài: Trình bày quan điểm có sắc thái và xử lý phản biện',N'C1','PUBLISHED','PUBLIC',@lesson);
 SET @set=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set,N'to some extent',N'đến một mức độ nào đó',N'C1',1,'ACTIVE'),
 (@set,N'admittedly',N'phải thừa nhận rằng',N'C1',2,'ACTIVE'),
 (@set,N'qualify a claim',N'giới hạn phạm vi một nhận định',N'C1',3,'ACTIVE'),
 (@set,N'justify a decision',N'giải thích cơ sở của quyết định',N'C1',4,'ACTIVE');
END;
GO

