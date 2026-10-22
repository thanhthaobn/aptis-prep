-- Run explicitly against the local SQL Server database after V028.
-- Inserts curriculum only. Does not create assessments, progress, goals or user profiles.
-- Existing matching published records are reused, never overwritten.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @lockResult int;
EXEC @lockResult = sys.sp_getapplock @Resource=N'library-curriculum-a1', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=10000;
IF @lockResult < 0 THROW 51000, 'Cannot lock curriculum import.', 1;
DECLARE @image nvarchar(500)=(SELECT image_path FROM lessons WHERE id=40 AND title=N'Viết lời mời tham gia câu lạc bộ đọc sách');
DECLARE @audio nvarchar(500)=(SELECT audio_path FROM lessons WHERE id=37 AND skill='Listening');
IF @image IS NULL OR @audio IS NULL THROW 51000, 'Required library media is missing; import image and audio first.', 1;
DECLARE @certificate int=(SELECT TOP(1) id FROM certificates WHERE status='ACTIVE' ORDER BY id);
IF @certificate IS NULL THROW 51000, 'No active certificate.', 1;
-- This syllabus assesses A1 only, including grammar. It is not a placement scale.
-- The agreed checkpoint policy separately requires >=30/50 to pass.
DECLARE @scheme int=(SELECT id FROM scoring_schemes WHERE certificate_id=@certificate AND name=N'Chuẩn hoàn thành chặng A1' AND version='0.1-A1');
IF @scheme IS NULL BEGIN
    INSERT scoring_schemes(certificate_id,name,version,status) VALUES(@certificate,N'Chuẩn hoàn thành chặng A1','0.1-A1','ACTIVE');
    SET @scheme=CONVERT(int,SCOPE_IDENTITY());
    INSERT score_bands(scoring_scheme_id,skill,cefr_level,min_score,max_score)
    SELECT @scheme,skill,'A1',0,50 FROM (VALUES('GRAMMAR_VOCABULARY'),('READING'),('LISTENING'),('WRITING'),('SPEAKING'))s(skill);
END;
IF (SELECT COUNT(DISTINCT skill) FROM score_bands WHERE scoring_scheme_id=@scheme AND skill IN ('GRAMMAR_VOCABULARY','READING','LISTENING','WRITING','SPEAKING'))<>5
    THROW 51000, 'Scoring scheme must cover all five skills.', 1;

DECLARE @source TABLE(skill nvarchar(30) PRIMARY KEY,code varchar(30),title nvarchar(255),task nvarchar(160),example nvarchar(max),prompt nvarchar(max),sample nvarchar(max),set_title nvarchar(255),exam_title nvarchar(255),question_type varchar(30));
INSERT @source VALUES
(N'Reading','READING',N'Đọc thông báo thư viện cộng đồng',N'Đọc tìm thông tin',
 N'The community library opens at 9:00 AM and closes at 5:00 PM. The reading club meets on Saturday at 10:00 AM. Please bring your library card. Membership is free.',
 N'Read the library notice. Find the opening time, club day and things to bring.',
 N'Read the question first. Underline 9:00 AM, Saturday and library card in the notice.',
 N'Từ vựng đọc thông báo thư viện',N'Kiểm tra cuối chặng A1 — Reading','SINGLE_CHOICE'),
(N'Grammar & Vocabulary','GRAMMAR_VOCABULARY',N'Hiện tại đơn trong sinh hoạt hằng ngày',N'Ngữ pháp trong ngữ cảnh',
 N'Use the present simple for routines. I read every day. She reads every Saturday. The library opens at nine. They do not work on Sundays.',
 N'Choose the correct verb form. Pay attention to the subject and the negative form.',
 N'Use -s with he, she and it. After do not or does not, use the base form.',
 N'Từ vựng về thói quen đọc sách',N'Kiểm tra cuối chặng A1 — Grammar & Vocabulary','SINGLE_CHOICE'),
(N'Listening','LISTENING',N'Nghe thông báo giờ mở cửa thư viện',N'Thông báo',
 N'You are at a community library. Listen for the opening time and the day of a reading club.',
 N'Listen to the announcement, then answer the questions. You may listen again before submitting.',
 N'Listen for the time, day and object after the phrase please bring.',
 N'Từ vựng nghe thông báo thư viện',N'Kiểm tra cuối chặng A1 — Listening','SINGLE_CHOICE'),
(N'Writing','WRITING',N'Viết lời mời tham gia câu lạc bộ đọc sách',N'Email',
 N'Start with a greeting. Invite your friend, give the time, say what to bring and finish with a friendly closing.',
 N'Write 20–60 words inviting your friend Sam to the library reading club on Saturday at 10:00 AM. Ask Sam to bring a library card and a favourite book.',
 N'Hi Sam, would you like to join the library reading club this Saturday at ten in the morning? Please bring your library card and your favourite book. I hope to see you there!',
 N'Cụm từ viết lời mời thân mật',N'Kiểm tra cuối chặng A1 — Writing','WRITING_TEXT'),
(N'Speaking','SPEAKING',N'Nói về thư viện và câu lạc bộ đọc sách',N'Mô tả ảnh',
 N'Use short, clear sentences. Describe the opening time, the club meeting and what visitors should bring.',
 N'Look at the library notice. Say when the library opens, when the reading club meets, and what you should bring. Record your answer.',
 N'The library opens at nine in the morning. The reading club meets on Saturday at ten. I should bring my library card. Membership is free.',
 N'Cụm từ giới thiệu thư viện',N'Kiểm tra cuối chặng A1 — Speaking','SPEAKING_AUDIO');

DECLARE @mapped TABLE(skill nvarchar(30) PRIMARY KEY,lesson_id int);
DECLARE @skill nvarchar(30),@code varchar(30),@title nvarchar(255),@task nvarchar(160),@example nvarchar(max),@prompt nvarchar(max),@sample nvarchar(max),@setTitle nvarchar(255),@examTitle nvarchar(255),@type varchar(30);
DECLARE lessons_cursor CURSOR LOCAL FAST_FORWARD FOR SELECT * FROM @source;
OPEN lessons_cursor;
FETCH NEXT FROM lessons_cursor INTO @skill,@code,@title,@task,@example,@prompt,@sample,@setTitle,@examTitle,@type;
WHILE @@FETCH_STATUS=0 BEGIN
    DECLARE @lesson int=(SELECT TOP(1) id FROM lessons WHERE title=@title AND skill=@skill AND cefr_level='A1' ORDER BY id);
    IF @lesson IS NULL BEGIN
        INSERT lessons(title,description,skill,cefr_level,display_order,status,topic,task_type,example_text,practice_prompt,sample_response,sample_visibility,transcript_visibility,image_path,image_name,audio_path,audio_name,transcript,min_word_count,max_word_count)
        SELECT @title,N'Học cách trao đổi thông tin cơ bản về thư viện và hoạt động đọc sách.',@skill,'A1',COALESCE(MAX(display_order),0)+1,'PUBLISHED',N'Thư viện và câu lạc bộ',@task,@example,@prompt,@sample,'AFTER_SUBMIT','AFTER_LISTEN',CASE WHEN @code='LISTENING' THEN NULL ELSE @image END,CASE WHEN @code='LISTENING' THEN NULL ELSE N'Thông báo thư viện.png' END,
               CASE WHEN @code='LISTENING' THEN @audio END,CASE WHEN @code='LISTENING' THEN N'Thông báo thư viện.wav' END,
               CASE WHEN @code='LISTENING' THEN N'Welcome to the community library. We open at nine in the morning. The reading club meets on Saturday. Please bring your library card.' END,
               CASE WHEN @code='WRITING' THEN 20 END,CASE WHEN @code='WRITING' THEN 60 END
        FROM lessons WHERE skill=@skill AND cefr_level='A1';
        SET @lesson=CONVERT(int,SCOPE_IDENTITY());
    END;
    IF NOT EXISTS(SELECT 1 FROM lessons WHERE id=@lesson AND status='PUBLISHED') THROW 51000, 'Matching lesson is not published; review it manually.', 1;
    INSERT @mapped VALUES(@skill,@lesson);
    FETCH NEXT FROM lessons_cursor INTO @skill,@code,@title,@task,@example,@prompt,@sample,@setTitle,@examTitle,@type;
END;
CLOSE lessons_cursor; DEALLOCATE lessons_cursor;

-- Lesson checks use zero-based correct_option; formal exam items use one-based positions.
DECLARE @checks TABLE(skill nvarchar(30),position int,prompt nvarchar(2000),a nvarchar(1000),b nvarchar(1000),answer int,explanation nvarchar(4000));
INSERT @checks VALUES
(N'Reading',1,N'What time does the library open?',N'9:00 AM',N'10:00 AM',0,N'The notice says the library opens at 9:00 AM.'),
(N'Reading',2,N'When does the reading club meet?',N'Sunday',N'Saturday',1,N'The reading club meets on Saturday.'),
(N'Reading',3,N'What should visitors bring?',N'A library card',N'A bus ticket',0,N'Visitors should bring their library card.'),
(N'Grammar & Vocabulary',1,N'She ___ every Saturday.',N'reads',N'read',0,N'She is singular, so the affirmative verb takes -s.'),
(N'Grammar & Vocabulary',2,N'They ___ to the library on Mondays.',N'goes',N'go',1,N'Use go with they.'),
(N'Grammar & Vocabulary',3,N'He does not ___ on Sunday.',N'works',N'work',1,N'After does not, use the base form work.'),
(N'Listening',1,N'When does the library open?',N'Nine in the morning',N'Ten in the morning',0,N'The speaker says: We open at nine in the morning.'),
(N'Listening',2,N'When does the reading club meet?',N'Saturday',N'Sunday',0,N'The speaker says the reading club meets on Saturday.'),
(N'Listening',3,N'What should you bring?',N'A library card',N'A passport',0,N'The announcement asks visitors to bring a library card.');
INSERT lesson_checks(lesson_id,position,prompt,option_a,option_b,correct_option,explanation)
SELECT m.lesson_id,c.position,c.prompt,c.a,c.b,c.answer,c.explanation FROM @checks c JOIN @mapped m ON m.skill=c.skill
WHERE NOT EXISTS(SELECT 1 FROM lesson_checks x WHERE x.lesson_id=m.lesson_id AND x.position=c.position);

DECLARE @cards TABLE(skill nvarchar(30),position int,term nvarchar(200),definition nvarchar(1000));
INSERT @cards VALUES
(N'Reading',1,N'notice',N'thông báo'),(N'Reading',2,N'opening time',N'giờ mở cửa'),(N'Reading',3,N'membership',N'tư cách thành viên'),(N'Reading',4,N'library card',N'thẻ thư viện'),(N'Reading',5,N'free',N'miễn phí'),(N'Reading',6,N'visitor',N'khách đến thăm'),
(N'Grammar & Vocabulary',1,N'every day',N'mỗi ngày'),(N'Grammar & Vocabulary',2,N'read',N'đọc'),(N'Grammar & Vocabulary',3,N'routine',N'thói quen thường ngày'),(N'Grammar & Vocabulary',4,N'on Mondays',N'vào các ngày thứ Hai'),(N'Grammar & Vocabulary',5,N'go to',N'đi đến'),(N'Grammar & Vocabulary',6,N'does not',N'không — dùng với he/she/it'),
(N'Listening',1,N'announcement',N'lời thông báo'),(N'Listening',2,N'community',N'cộng đồng'),(N'Listening',3,N'in the morning',N'vào buổi sáng'),(N'Listening',4,N'meet',N'gặp gỡ, họp mặt'),(N'Listening',5,N'please bring',N'vui lòng mang theo'),(N'Listening',6,N'Saturday',N'thứ Bảy'),
(N'Writing',1,N'Hi Sam',N'Chào Sam'),(N'Writing',2,N'would you like to',N'bạn có muốn'),(N'Writing',3,N'join',N'tham gia'),(N'Writing',4,N'favourite book',N'cuốn sách yêu thích'),(N'Writing',5,N'see you there',N'hẹn gặp bạn ở đó'),(N'Writing',6,N'hope to',N'hy vọng sẽ'),
(N'Speaking',1,N'it opens at',N'nơi này mở cửa lúc'),(N'Speaking',2,N'I should bring',N'tôi nên mang theo'),(N'Speaking',3,N'reading club',N'câu lạc bộ đọc sách'),(N'Speaking',4,N'at ten',N'lúc mười giờ'),(N'Speaking',5,N'library',N'thư viện'),(N'Speaking',6,N'clear sentence',N'câu nói rõ ràng');
INSERT flashcard_sets(title,description,cefr_level,status,lesson_id,owner_user_id,visibility)
SELECT s.set_title,N'Từ và cụm từ sử dụng trong bài học về thư viện.','A1','PUBLISHED',m.lesson_id,NULL,'PUBLIC'
FROM @source s JOIN @mapped m ON s.skill=m.skill
WHERE NOT EXISTS(SELECT 1 FROM flashcard_sets f WHERE f.lesson_id=m.lesson_id AND f.title=s.set_title);
INSERT flashcards(set_id,front_content,back_content,cefr_level,display_order,status)
SELECT f.id,c.term,c.definition,'A1',c.position,'ACTIVE' FROM @cards c JOIN @mapped m ON c.skill=m.skill
JOIN @source s ON c.skill=s.skill JOIN flashcard_sets f ON f.lesson_id=m.lesson_id AND f.title=s.set_title
WHERE NOT EXISTS(SELECT 1 FROM flashcards x WHERE x.set_id=f.id AND x.display_order=c.position);

-- Five formal questions per objective skill support clear 3/5 pass and 2/5 fail scenarios.
DECLARE @examItems TABLE(skill nvarchar(30),position int,prompt nvarchar(2000),a nvarchar(1000),b nvarchar(1000),answer int);
INSERT @examItems SELECT skill,position,prompt,a,b,answer+1 FROM @checks;
INSERT @examItems VALUES
(N'Reading',4,N'What time does the library close?',N'5:00 PM',N'9:00 PM',1),
(N'Reading',5,N'How much does membership cost?',N'It is free.',N'Ten pounds.',1),
(N'Grammar & Vocabulary',4,N'The library ___ at nine.',N'open',N'opens',2),
(N'Grammar & Vocabulary',5,N'___ you read every day?',N'Do',N'Does',1),
(N'Listening',4,N'Where is the announcement made?',N'At a library',N'At an airport',1),
(N'Listening',5,N'Does the library open in the morning or at night?',N'At night',N'In the morning',2);

DECLARE @result TABLE(skill nvarchar(30),lesson_id int,set_id int,exam_id int,question_id int);
DECLARE exams_cursor CURSOR LOCAL FAST_FORWARD FOR SELECT * FROM @source;
OPEN exams_cursor;
FETCH NEXT FROM exams_cursor INTO @skill,@code,@title,@task,@example,@prompt,@sample,@setTitle,@examTitle,@type;
WHILE @@FETCH_STATUS=0 BEGIN
    DECLARE @questionText nvarchar(max)=CASE WHEN @code='READING' THEN @example WHEN @code='GRAMMAR_VOCABULARY' THEN N'Complete the sentences using the present simple.' WHEN @code='LISTENING' THEN N'Listen to the community library announcement and answer the questions.' ELSE @prompt END;
    DECLARE @question int=(SELECT TOP(1) id FROM questions WHERE content=@questionText AND skill=@code AND cefr_level='A1' AND question_type=@type ORDER BY id);
    IF @question IS NULL BEGIN
        INSERT questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,allow_option_reuse,preparation_seconds,status)
        VALUES(@questionText,CASE WHEN @type='SINGLE_CHOICE' THEN N'Choose one answer for each question.' WHEN @code='WRITING' THEN N'Write 20–60 words. Include all the requested information.' ELSE N'Prepare your answer, then record yourself speaking clearly.' END,@type,@code,1,'A1','EASY',0,CASE WHEN @code='SPEAKING' THEN 15 END,'PUBLISHED');
        SET @question=CONVERT(int,SCOPE_IDENTITY());
        IF @type='SINGLE_CHOICE' BEGIN
            INSERT question_items(question_id,content,correct_position,position)
            SELECT @question,prompt,answer,position FROM @examItems WHERE skill=@skill;
            INSERT question_options(question_item_id,content,option_code,is_correct,position)
            SELECT i.id,CASE WHEN p.pos=1 THEN e.a ELSE e.b END,CASE WHEN p.pos=1 THEN 'A' ELSE 'B' END,CASE WHEN e.answer=p.pos THEN 1 ELSE 0 END,p.pos
            FROM @examItems e JOIN question_items i ON i.question_id=@question AND i.position=e.position CROSS JOIN (VALUES(1),(2))p(pos) WHERE e.skill=@skill;
        END ELSE BEGIN
            INSERT question_items(question_id,content,position,min_word_count,max_word_count,response_seconds)
            VALUES(@question,@prompt,1,CASE WHEN @code='WRITING' THEN 20 END,CASE WHEN @code='WRITING' THEN 60 END,CASE WHEN @code='SPEAKING' THEN 45 END);
        END;
        IF @code='LISTENING'
            INSERT question_media(question_id,media_type,media_url,original_filename,content_type,max_play_count,position,alt_text)
            VALUES(@question,'AUDIO',@audio,N'Thông báo thư viện.wav','audio/wav',2,1,N'Thông báo giờ mở cửa và câu lạc bộ đọc sách');
        IF @code='SPEAKING'
            INSERT question_media(question_id,media_type,media_url,original_filename,content_type,position,alt_text)
            VALUES(@question,'IMAGE',@image,N'Thông báo thư viện.png','image/png',1,N'Thư viện mở cửa 9 giờ; câu lạc bộ đọc sách vào thứ Bảy lúc 10 giờ');
    END;
    DECLARE @exam int=(SELECT TOP(1) id FROM mock_exams WHERE title=@examTitle AND exam_type='CHECKPOINT' AND skill=@code AND target_level='A1' ORDER BY id);
    IF @exam IS NULL BEGIN
        INSERT mock_exams(certificate_id,scoring_scheme_id,title,description,exam_type,target_level,status,skill,duration_minutes)
        VALUES(@certificate,@scheme,@examTitle,N'Đánh giá nội dung đã học ở chặng A1 trước khi chuyển sang chặng A2.','CHECKPOINT','A1','PUBLISHED',@code,CASE WHEN @code='WRITING' THEN 10 ELSE 5 END);
        SET @exam=CONVERT(int,SCOPE_IDENTITY());
        INSERT mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status)
        VALUES(@exam,@code,1,CASE WHEN @code='WRITING' THEN 10 ELSE 5 END,'ACTIVE');
        DECLARE @section int=CONVERT(int,SCOPE_IDENTITY());
        INSERT mock_exam_questions(mock_exam_id,mock_exam_section_id,question_id,position) VALUES(@exam,@section,@question,1);
    END;
    INSERT @result SELECT @skill,m.lesson_id,f.id,@exam,@question FROM @mapped m JOIN flashcard_sets f ON f.lesson_id=m.lesson_id AND f.title=@setTitle WHERE m.skill=@skill;
    FETCH NEXT FROM exams_cursor INTO @skill,@code,@title,@task,@example,@prompt,@sample,@setTitle,@examTitle,@type;
END;
CLOSE exams_cursor; DEALLOCATE exams_cursor;
COMMIT;
SELECT * FROM @result ORDER BY skill;
