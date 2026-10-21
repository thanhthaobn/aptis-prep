-- Remove only known legacy placeholders from the learner catalog. They are
-- archived instead of deleted so existing progress and review history remain valid.
UPDATE lessons SET status = 'ARCHIVED', updated_at = SYSDATETIME()
WHERE title IN (
    N'Reading A1: Giới thiệu bản thân',
    N'Reading A1: Một ngày của Lucy',
    N'Reading A1: Đọc lời mời qua email'
);

UPDATE flashcard_sets SET status = 'ARCHIVED'
WHERE title IN (
    N'Reading A1: Giới thiệu bản thân',
    N'Reading A1: Một ngày của Lucy',
    N'Reading A1: Đọc lời mời qua email'
);
GO

-- V013 depended on pre-existing rows. These inserts make a clean database
-- independently usable and remain idempotent for an existing database.
IF NOT EXISTS (SELECT 1 FROM lessons WHERE title=N'Hiện tại đơn: nói về thói quen trong ngữ cảnh')
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
VALUES(N'Hiện tại đơn: nói về thói quen trong ngữ cảnh',N'Dùng hiện tại đơn và cụm từ sinh hoạt trong câu có nghĩa.',N'Grammar & Vocabulary','A1',101,'PUBLISHED',N'Daily routines',N'Ngữ pháp và từ vựng trong ngữ cảnh',
N'Nhận biết chủ ngữ; chọn đúng dạng động từ; dùng cụm từ sinh hoạt trong câu.',
N'Xác định câu diễn tả thói quen. Với he/she/it, thêm -s/-es ở câu khẳng định. Với phủ định và câu hỏi, dùng does/doesn''t rồi giữ động từ nguyên mẫu. Học từ theo cụm như get up, have breakfast, go to work.',
N'Mai gets up at six. She has breakfast at home. She doesn''t drive to work. Does she take the bus? Yes, she does.',
N'Gets và has đi với she. Sau doesn''t và does, drive và take giữ dạng nguyên mẫu.',
N'Dùng “she don''t”; viết “doesn''t goes”; học từ rời mà không đặt câu.',
N'Hoàn thành: My brother ___ (work) in a bookshop. We ___ (not study) on Sundays. ___ she ___ (have) breakfast at home? Sau đó viết bốn câu về ngày thường của bạn.',
N'works; don''t study; Does she have. Ví dụ: I get up at six. I have breakfast with my family. I don''t work on Sundays.',
N'Đã tìm chủ ngữ chưa? Sau do/does động từ có giữ nguyên không? Câu có đúng nghĩa thói quen không?',
N'https://www.youtube.com/watch?v=Y_DELbsHzZI',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council; nội dung bài do AptisPrep biên soạn');
GO

IF NOT EXISTS (SELECT 1 FROM lessons WHERE title=N'Đọc tìm thông tin: xác định từ khóa và bằng chứng')
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
VALUES(N'Đọc tìm thông tin: xác định từ khóa và bằng chứng',N'Tìm thời gian, địa điểm và điều kiện trong thông báo ngắn.',N'Reading','A2',201,'PUBLISHED',N'Thông báo và lịch sinh hoạt',N'Tìm thông tin cụ thể',
N'Xác định từ khóa; nhận ra cách diễn đạt tương đương; chỉ ra câu làm bằng chứng.',
N'Đọc câu hỏi trước để biết cần tìm người, nơi, thời gian hay lý do. Đọc quét để tìm vùng thông tin, rồi đọc kỹ câu lân cận. Chú ý only, except, before và after. Chỉ chọn khi có bằng chứng.',
N'The library closes at 4 p.m. on Saturday. The study room closes thirty minutes before the library. When does the study room close?',
N'Thư viện đóng lúc 4 giờ; phòng học đóng sớm hơn 30 phút nên đáp án là 3:30 p.m.',
N'Chọn số đầu tiên nhìn thấy; nhầm đối tượng; dùng hiểu biết bên ngoài thay cho văn bản.',
N'The pool closes at 6 p.m. on Sundays. The ticket desk stops selling tickets one hour before closing. What time does the ticket desk stop selling tickets?',
N'At 5 p.m. Bằng chứng: quầy vé dừng bán trước giờ đóng cửa một giờ.',
N'Đúng đối tượng chưa? Đã xét từ chỉ thời gian và điều kiện chưa? Có trích được bằng chứng không?',
N'https://www.youtube.com/watch?v=yiwhUC4S5_o',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council; nội dung bài do AptisPrep biên soạn');
GO

IF NOT EXISTS (SELECT 1 FROM lessons WHERE title=N'Nghe có mục tiêu: dự đoán thông tin và xác nhận đáp án')
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
VALUES(N'Nghe có mục tiêu: dự đoán thông tin và xác nhận đáp án',N'Chuẩn bị trước khi nghe và nhận ra thông tin được sửa lại.',N'Listening','A1',301,'PUBLISHED',N'Giao tiếp hằng ngày',N'Nghe thời gian và địa điểm',
N'Dự đoán loại thông tin cần nghe; ghi từ khóa; xác nhận thông tin cuối cùng.',
N'Trước khi nghe, đọc nhiệm vụ và dự đoán cần số, giờ hay địa điểm. Lượt đầu nghe ý chính. Lượt sau tập trung chỗ còn thiếu và các tín hiệu actually, sorry, instead.',
N'“Meet at nine. Sorry, can we meet at half past nine instead? I will wait near the café, not at the main entrance.”',
N'Sorry và instead báo hiệu giờ đổi thành 9:30. Near the café là nơi cuối cùng; main entrance bị phủ định.',
N'Chốt đáp án ở thông tin đầu; bỏ qua từ phủ định; bật phụ đề ngay từ đầu.',
N'Nghe video Cooking do giáo viên cung cấp. Lần đầu ghi chủ đề và ba từ khóa. Lần sau ghi hai thông tin cụ thể kèm mốc thời gian.',
N'Ghi theo mẫu: thông tin nghe được — mốc thời gian — bằng chứng — điều cần nghe lại.',
N'Đã nghe trước khi bật phụ đề chưa? Thông tin có mốc thời gian không? Có phân biệt nghe được và suy đoán không?',
N'https://www.youtube.com/watch?v=cbGwglNyEaQ',N'https://www.youtube.com/watch?v=uVGV8LG3HHM',N'British Council và nguồn video người học cung cấp; bài tập do AptisPrep biên soạn');
GO

IF NOT EXISTS (SELECT 1 FROM lessons WHERE title=N'Viết email thân mật: phân tích đề và triển khai từng ý')
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name,min_word_count,max_word_count)
VALUES(N'Viết email thân mật: phân tích đề và triển khai từng ý',N'Xác định người nhận, lập dàn ý và viết đủ yêu cầu.',N'Writing','B1',401,'PUBLISHED',N'Câu lạc bộ và hoạt động cộng đồng',N'Email thân mật',
N'Xác định người nhận; trả lời đủ ý; dùng lý do và chi tiết hỗ trợ.',
N'Khoanh người nhận và các ý bắt buộc. Viết câu mở nêu mục đích. Mỗi đề xuất đi cùng lý do. Kết thúc bằng câu hỏi hoặc lời mời phản hồi rồi kiểm tra đủ ý, giọng văn và động từ.',
N'Your friend wants to join your club. Suggest a day, say what to bring, and explain why it is suitable for beginners.',
N'Ba yêu cầu cần ba ý rõ ràng. Giọng thân mật phù hợp với bạn bè; because giúp giải thích đề xuất.',
N'Viết bài thuộc lòng; thiếu một yêu cầu; dùng giọng quá trang trọng; chỉ liệt kê mà không giải thích.',
N'Viết email 70–90 từ cho Alex: cho biết thời gian, vật dụng cần mang và vì sao người mới có thể tham gia.',
N'Hi Alex, I am glad you want to join. Come on Saturday at ten because we start with a short introduction. Please bring an apron and a notebook. We work in small groups, so beginners can follow each step. Can you come? Best, Mai',
N'Đủ ba ý chưa? Giọng văn phù hợp chưa? Mỗi đề xuất có lý do chưa? Đã kiểm tra động từ và chính tả chưa?',
N'https://www.youtube.com/watch?v=wumg5p4xeJY',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council; nội dung bài do AptisPrep biên soạn',70,90);
GO

IF NOT EXISTS (SELECT 1 FROM lessons WHERE title=N'Nói có phát triển ý: trả lời, lý do và ví dụ')
INSERT INTO lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,objectives,strategy,example_text,analysis,common_mistakes,practice_prompt,sample_response,checklist,video_url,source_url,source_name)
VALUES(N'Nói có phát triển ý: trả lời, lý do và ví dụ',N'Phát triển câu trả lời từ ý chính, lý do và ví dụ.',N'Speaking','B1',501,'PUBLISHED',N'Sở thích và trải nghiệm cá nhân',N'Trả lời câu hỏi cá nhân',
N'Trả lời trực tiếp; thêm lý do; đưa ví dụ cụ thể; tự nghe và nhận xét.',
N'Trả lời câu hỏi bằng một câu đơn giản, thêm lý do với because, rồi thêm ví dụ có when, where, who. Chỉ chuẩn bị ba đến năm từ khóa; không học thuộc cả đoạn.',
N'What do you enjoy doing at weekends? — I enjoy cycling because it helps me relax. Last Sunday, I rode to a park with my sister.',
N'Câu đầu trả lời trực tiếp, because nêu lý do, Last Sunday mở ví dụ cụ thể.',
N'Chỉ nói một câu; học thuộc câu trả lời không phù hợp; nói quá nhanh; dừng lâu để tìm từ khó.',
N'Chuẩn bị bốn từ khóa rồi ghi âm 30–45 giây: What activity helps you relax, and why?',
N'I enjoy reading because it helps me slow down after work. Last weekend, I read a short story with my friend and we discussed the ending.',
N'Đã trả lời ngay đầu chưa? Có lý do và ví dụ chưa? Khi nghe lại, đoạn nào chưa rõ hoặc ngập ngừng?',
N'https://www.youtube.com/watch?v=1c5TCWR6Fyg',N'https://www.britishcouncil.org/exam/english/aptis/preparation-videos',N'British Council; nội dung bài do AptisPrep biên soạn');
GO

-- Add a short knowledge check only when the curated lesson has none.
DECLARE @gv INT=(SELECT id FROM lessons WHERE title=N'Hiện tại đơn: nói về thói quen trong ngữ cảnh');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@gv)
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES
(@gv,1,N'My sister ___ breakfast at home.',N'have',N'has',N'having',1,N'My sister là ngôi thứ ba số ít nên dùng has.'),
(@gv,2,N'Chọn câu đúng.',N'She doesn''t work on Sunday.',N'She doesn''t works on Sunday.',N'She don''t work on Sunday.',0,N'Doesn''t đi với động từ nguyên mẫu work.');

DECLARE @reading INT=(SELECT id FROM lessons WHERE title=N'Đọc tìm thông tin: xác định từ khóa và bằng chứng');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@reading)
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES
(@reading,1,N'Quầy vé dừng bán lúc mấy giờ?',N'5 p.m.',N'6 p.m.',N'7 p.m.',0,N'Đóng cửa lúc 6 giờ và dừng bán trước một giờ.'),
(@reading,2,N'Cách chọn đáp án đáng tin cậy nhất?',N'Chọn từ giống câu hỏi.',N'Chỉ ra bằng chứng trong bài.',N'Dùng kiến thức ngoài bài.',1,N'Đáp án cần được chứng minh bởi văn bản.');

DECLARE @listening INT=(SELECT id FROM lessons WHERE title=N'Nghe có mục tiêu: dự đoán thông tin và xác nhận đáp án');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@listening)
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES
(@listening,1,N'Giờ hẹn cuối cùng là gì?',N'9:00',N'9:30',N'10:30',1,N'Instead báo hiệu giờ đổi thành half past nine.'),
(@listening,2,N'Người nói sẽ chờ ở đâu?',N'Gần quán cà phê.',N'Cửa chính.',N'Bến xe.',0,N'Near the café là nơi cuối cùng.');

DECLARE @writing INT=(SELECT id FROM lessons WHERE title=N'Viết email thân mật: phân tích đề và triển khai từng ý');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@writing)
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES
(@writing,1,N'Trước khi viết cần xác định gì?',N'Người nhận và yêu cầu.',N'Một thành ngữ khó.',N'Số câu tối đa.',0,N'Người nhận quyết định giọng văn; yêu cầu quyết định nội dung.'),
(@writing,2,N'Mỗi đề xuất nên đi cùng điều gì?',N'Một lý do hoặc chi tiết.',N'Một từ thật dài.',N'Một lời chào mới.',0,N'Lý do và chi tiết giúp ý rõ ràng.');

DECLARE @speaking INT=(SELECT id FROM lessons WHERE title=N'Nói có phát triển ý: trả lời, lý do và ví dụ');
IF NOT EXISTS(SELECT 1 FROM lesson_checks WHERE lesson_id=@speaking)
INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,correct_option,explanation) VALUES
(@speaking,1,N'Cấu trúc phát triển câu trả lời là gì?',N'Trả lời – lý do – ví dụ.',N'Lặp câu hỏi – kết thúc.',N'Chào hỏi – im lặng.',0,N'Lý do và ví dụ làm rõ câu trả lời.'),
(@speaking,2,N'Nên chuẩn bị gì trước khi nói?',N'Cả đoạn để học thuộc.',N'Ba đến năm từ khóa.',N'Một từ khó.',1,N'Từ khóa hỗ trợ nói tự nhiên và đúng hướng.');
GO

-- Every curated lesson receives one public library set. Existing sets and card
-- progress are preserved; cards are inserted only for a newly created set.
DECLARE @lesson_id INT, @set_id INT;

SET @lesson_id=(SELECT id FROM lessons WHERE title=N'Hiện tại đơn: nói về thói quen trong ngữ cảnh');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson_id)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,owner_user_id,visibility,lesson_id) VALUES(N'Daily routines — Thói quen hằng ngày',N'Cụm từ dùng với hiện tại đơn trong đời sống hằng ngày.','A1','PUBLISHED',NULL,'PUBLIC',@lesson_id);
 SET @set_id=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set_id,N'get up',N'thức dậy','A1',1,'ACTIVE'),(@set_id,N'have breakfast',N'ăn sáng','A1',2,'ACTIVE'),(@set_id,N'go to work',N'đi làm','A1',3,'ACTIVE'),(@set_id,N'take a break',N'nghỉ giải lao','A1',4,'ACTIVE'),(@set_id,N'go to bed',N'đi ngủ','A1',5,'ACTIVE');
END;

SET @lesson_id=(SELECT id FROM lessons WHERE title=N'Đọc tìm thông tin: xác định từ khóa và bằng chứng');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson_id)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,owner_user_id,visibility,lesson_id) VALUES(N'Reading clues — Từ khóa đọc hiểu',N'Từ chỉ thời gian, điều kiện và bằng chứng trong thông báo.','A2','PUBLISHED',NULL,'PUBLIC',@lesson_id);
 SET @set_id=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set_id,N'opening hours',N'giờ mở cửa','A2',1,'ACTIVE'),(@set_id,N'before closing',N'trước giờ đóng cửa','A2',2,'ACTIVE'),(@set_id,N'entrance',N'lối vào','A2',3,'ACTIVE'),(@set_id,N'evidence',N'bằng chứng','A2',4,'ACTIVE'),(@set_id,N'except',N'ngoại trừ','A2',5,'ACTIVE');
END;

SET @lesson_id=(SELECT id FROM lessons WHERE title=N'Nghe có mục tiêu: dự đoán thông tin và xác nhận đáp án');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson_id)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,owner_user_id,visibility,lesson_id) VALUES(N'Listening signals — Tín hiệu khi nghe',N'Từ báo hiệu sửa thông tin và xác nhận đáp án.','A1','PUBLISHED',NULL,'PUBLIC',@lesson_id);
 SET @set_id=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set_id,N'actually',N'thực ra; dùng khi sửa hoặc làm rõ thông tin','A1',1,'ACTIVE'),(@set_id,N'instead',N'thay vào đó','A1',2,'ACTIVE'),(@set_id,N'half past nine',N'chín giờ ba mươi','A1',3,'ACTIVE'),(@set_id,N'main entrance',N'lối vào chính','A1',4,'ACTIVE'),(@set_id,N'near the café',N'gần quán cà phê','A1',5,'ACTIVE');
END;

SET @lesson_id=(SELECT id FROM lessons WHERE title=N'Viết email thân mật: phân tích đề và triển khai từng ý');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson_id)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,owner_user_id,visibility,lesson_id) VALUES(N'Informal email — Email thân mật',N'Cụm từ mở ý, giải thích và kết thúc email.','B1','PUBLISHED',NULL,'PUBLIC',@lesson_id);
 SET @set_id=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set_id,N'I am glad to hear that…',N'Mình rất vui khi biết rằng…','B1',1,'ACTIVE'),(@set_id,N'I suggest…',N'Mình đề xuất…','B1',2,'ACTIVE'),(@set_id,N'because',N'bởi vì; dùng để nêu lý do','B1',3,'ACTIVE'),(@set_id,N'Don''t worry about…',N'Đừng lo về…','B1',4,'ACTIVE'),(@set_id,N'Can you come?',N'Bạn có thể đến không?','B1',5,'ACTIVE');
END;

SET @lesson_id=(SELECT id FROM lessons WHERE title=N'Nói có phát triển ý: trả lời, lý do và ví dụ');
IF NOT EXISTS(SELECT 1 FROM flashcard_sets WHERE lesson_id=@lesson_id)
BEGIN
 INSERT INTO flashcard_sets(title,description,cefr_level,status,owner_user_id,visibility,lesson_id) VALUES(N'Speaking builders — Cụm phát triển ý',N'Cụm từ giúp nêu ý, lý do và ví dụ khi nói.','B1','PUBLISHED',NULL,'PUBLIC',@lesson_id);
 SET @set_id=SCOPE_IDENTITY();
 INSERT INTO flashcards(set_id,front_content,back_content,cefr_level,display_order,status) VALUES
 (@set_id,N'In my opinion…',N'Theo ý kiến của tôi…','B1',1,'ACTIVE'),(@set_id,N'One reason is…',N'Một lý do là…','B1',2,'ACTIVE'),(@set_id,N'For example…',N'Ví dụ…','B1',3,'ACTIVE'),(@set_id,N'Last weekend…',N'Cuối tuần trước…','B1',4,'ACTIVE'),(@set_id,N'It helps me…',N'Nó giúp tôi…','B1',5,'ACTIVE');
END;
GO
