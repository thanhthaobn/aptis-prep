/*
   Original Aptis-style practice bank for the five Aptis General components.

   This file contains newly written practice content. It is not a copy of an
   official British Council test. It is safe to run more than once and only
   creates missing questions and exams.

   Before running:
   1. Apply the current schema and migrations.
   2. Upload licensed audio and image files separately.
   3. Set @mediaReady = 1 only after the media paths below exist under uploads.

   The default creates a virtual speech media row for Listening. The browser
   reads the original transcript aloud until a real WAV file is uploaded.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @mediaReady bit = 0;
DECLARE @certificateId int = (SELECT TOP (1) id FROM certificates WHERE status = 'ACTIVE' ORDER BY id);
DECLARE @scoringSchemeId int =
    (SELECT TOP (1) id FROM scoring_schemes WHERE certificate_id = @certificateId AND status = 'ACTIVE' ORDER BY id);

IF @certificateId IS NULL
    THROW 51000, 'No active certificate is available.', 1;
IF @scoringSchemeId IS NULL
    THROW 51000, 'No active scoring scheme is available for the active certificate.', 1;

DECLARE @questionDefinitions TABLE
(
    question_key varchar(80) NOT NULL PRIMARY KEY,
    content nvarchar(max) NOT NULL,
    instruction nvarchar(max) NOT NULL,
    reading_passage nvarchar(max) NULL,
    question_type varchar(30) NOT NULL,
    skill varchar(30) NOT NULL,
    part_no int NOT NULL,
    cefr_level varchar(10) NOT NULL,
    difficulty varchar(30) NOT NULL,
    preparation_seconds int NULL
);

INSERT @questionDefinitions
    (question_key, content, instruction, reading_passage, question_type, skill, part_no, cefr_level, difficulty, preparation_seconds)
VALUES
('CORE_GRAMMAR',
 N'Complete each sentence with the correct grammar option.',
 N'Choose one answer for each sentence.', NULL, 'SINGLE_CHOICE', 'GRAMMAR_VOCABULARY', 1, 'B1', 'MEDIUM', NULL),
('CORE_VOCAB',
 N'Use the correct word or word combination in each context.',
 N'Choose one answer for each question.', NULL, 'SINGLE_CHOICE', 'GRAMMAR_VOCABULARY', 2, 'B1', 'MEDIUM', NULL),
('READING_PART_1',
 N'Complete the short email about joining a community garden.',
 N'Read the email and choose the best word for each gap.',
 N'Hi Alex,\nI joined the community garden last month. The first meeting was friendly and I learned how to plant herbs. We meet every Saturday morning, so I usually arrive early to help prepare the tools. If you have time, you should come and see the garden.\nBest,\nMina',
 'SINGLE_CHOICE', 'READING', 1, 'B1', 'MEDIUM', NULL),
('READING_PART_2',
 N'Put the sentences about a repair workshop in the correct order.',
 N'The first sentence is already in the correct place. Order the remaining sentences.', NULL, 'ORDERING', 'READING', 2, 'B1', 'MEDIUM', NULL),
('READING_PART_3',
 N'Match each opinion about public transport with the correct speaker.',
 N'Read the four opinions and match each statement to one person.', NULL, 'MATCHING', 'READING', 3, 'B1', 'HARD', NULL),
('READING_PART_4',
 N'Match headings to paragraphs in an article about quieter city centres.',
 N'Read the article and choose the heading that best summarises each paragraph.',
 N'Paragraph A: Several towns have reduced traffic in their central streets by creating timed delivery zones. Shops can still receive supplies, but large vehicles arrive outside the busiest hours.\n\nParagraph B: The change was not welcomed by everyone. Some shop owners feared that customers would stop visiting if they could not drive directly to the centre.\n\nParagraph C: After six months, local councils compared air-quality readings with measurements from the previous year. The results suggested a modest improvement near the main shopping streets.\n\nParagraph D: Residents also reported that the streets felt safer for children and older people. Cafes began placing more tables outside, which changed the atmosphere of the area.\n\nParagraph E: The policy still needs careful planning. Deliveries to hospitals and homes cannot always follow the same timetable as commercial deliveries.\n\nParagraph F: Other towns are watching the results before making a decision. They want to understand the costs as well as the benefits.\n\nParagraph G: The project shows that a transport policy can affect more than traffic. It can also change how people use shared public spaces.',
 'MATCHING', 'READING', 4, 'B2', 'HARD', NULL),
('LISTENING_PART_1',
 N'Listen to a short community-centre message and identify the exact information.',
 N'Choose the correct answer for each question. The recording should be played no more than twice.', NULL, 'SINGLE_CHOICE', 'LISTENING', 1, 'B1', 'MEDIUM', NULL),
('LISTENING_PART_2',
 N'Match four people with the reason they chose a weekend course.',
 N'Listen to the four short speakers and match each speaker to one reason.', NULL, 'MATCHING', 'LISTENING', 2, 'B1', 'MEDIUM', NULL),
('LISTENING_PART_3',
 N'Listen to a discussion about flexible working and identify each opinion.',
 N'Choose who expresses each opinion: the man, the woman, or both.', NULL, 'SINGLE_CHOICE', 'LISTENING', 3, 'B2', 'HARD', NULL),
('LISTENING_PART_4',
 N'Listen to a commentary about a city cycling scheme and identify the speaker''s viewpoint.',
 N'Choose the best description of the speaker''s attitude.', NULL, 'SINGLE_CHOICE', 'LISTENING', 4, 'C1', 'HARD', NULL),
('WRITING_PART_1',
 N'Respond to five short messages from members of a photography club.',
 N'Answer each message with one word or a short phrase.', NULL, 'WRITING_TEXT', 'WRITING', 1, 'B1', 'MEDIUM', NULL),
('WRITING_PART_2',
 N'Write a short message about a photography club meeting.',
 N'Write 20 to 30 words. Include the day, place and one thing members should bring.', NULL, 'WRITING_TEXT', 'WRITING', 2, 'B1', 'MEDIUM', NULL),
('WRITING_PART_3',
 N'Respond to three members discussing a local photography project.',
 N'Write about 40 words for each response. Answer the question and give a reason.', NULL, 'WRITING_TEXT', 'WRITING', 3, 'B1', 'MEDIUM', NULL),
('WRITING_PART_4',
 N'Write an informal email and a formal email about a changed photography course.',
 N'Write an informal email of 40 to 50 words and a formal email of 120 to 150 words.', NULL, 'WRITING_TEXT', 'WRITING', 4, 'B2', 'HARD', NULL),
('SPEAKING_PART_1',
 N'Answer questions about yourself and your interests.',
 N'Speak for 30 seconds for each question.', NULL, 'SPEAKING_AUDIO', 'SPEAKING', 1, 'B1', 'MEDIUM', 0),
('SPEAKING_PART_2',
 N'Describe a photograph of people working together in a community garden.',
 N'Describe the photograph, then answer the two follow-up questions. Speak for 45 seconds for each response.', NULL, 'SPEAKING_AUDIO', 'SPEAKING', 2, 'B1', 'MEDIUM', 60),
('SPEAKING_PART_3',
 N'Compare two photographs showing different ways of studying.',
 N'Compare the photographs, then answer the two follow-up questions. Speak for 45 seconds for each response.', NULL, 'SPEAKING_AUDIO', 'SPEAKING', 3, 'B2', 'HARD', 60),
('SPEAKING_PART_4',
 N'Discuss whether towns should provide more free activities for young adults.',
 N'You have one minute to prepare. Speak for two minutes and answer all three questions.', NULL, 'SPEAKING_AUDIO', 'SPEAKING', 4, 'C1', 'HARD', 60);

DECLARE @questionMap TABLE
(
    question_key varchar(80) NOT NULL PRIMARY KEY,
    question_id int NOT NULL,
    skill varchar(30) NOT NULL,
    part_no int NOT NULL
);

DECLARE @questionKey varchar(80), @content nvarchar(max), @instruction nvarchar(max), @passage nvarchar(max),
        @questionType varchar(30), @skill varchar(30), @partNo int, @level varchar(10),
        @difficulty varchar(30), @preparationSeconds int, @questionId int;

DECLARE question_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT question_key, content, instruction, reading_passage, question_type, skill, part_no,
           cefr_level, difficulty, preparation_seconds
    FROM @questionDefinitions
    ORDER BY skill, part_no, question_key;

OPEN question_cursor;
FETCH NEXT FROM question_cursor INTO @questionKey, @content, @instruction, @passage, @questionType,
    @skill, @partNo, @level, @difficulty, @preparationSeconds;
WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @questionId = NULL;
    SELECT TOP (1) @questionId = id
    FROM questions
    WHERE content = @content
      AND skill = @skill
      AND part_no = @partNo
      AND question_type = @questionType
      AND cefr_level = @level
    ORDER BY id;

    IF @questionId IS NULL
    BEGIN
        INSERT questions
            (content, instruction, reading_passage, question_type, skill, part_no, cefr_level,
             difficulty, allow_option_reuse, preparation_seconds, status, created_at, updated_at)
        VALUES
            (@content, @instruction, @passage, @questionType, @skill, @partNo, @level,
             @difficulty, CASE WHEN @questionType = 'ORDERING' THEN 0 WHEN @questionKey = 'READING_PART_3' THEN 1 ELSE 0 END,
             @preparationSeconds, 'PUBLISHED', SYSDATETIME(), SYSDATETIME());
        SET @questionId = CONVERT(int, SCOPE_IDENTITY());
    END;

    INSERT @questionMap(question_key, question_id, skill, part_no)
    VALUES (@questionKey, @questionId, @skill, @partNo);

    FETCH NEXT FROM question_cursor INTO @questionKey, @content, @instruction, @passage, @questionType,
        @skill, @partNo, @level, @difficulty, @preparationSeconds;
END;
CLOSE question_cursor;
DEALLOCATE question_cursor;

DECLARE @itemDefinitions TABLE
(
    question_key varchar(80) NOT NULL,
    position int NOT NULL,
    content nvarchar(max) NOT NULL,
    correct_position int NULL,
    min_word_count int NULL,
    max_word_count int NULL,
    response_seconds int NULL,
    PRIMARY KEY (question_key, position)
);

INSERT @itemDefinitions
    (question_key, position, content, correct_position, min_word_count, max_word_count, response_seconds)
VALUES
('CORE_GRAMMAR', 1, N'If the weather ___ better tomorrow, we will hold the meeting outside.', 3, NULL, NULL, NULL),
('CORE_GRAMMAR', 2, N'Maria has worked at the centre ___ three years.', 2, NULL, NULL, NULL),
('CORE_GRAMMAR', 3, N'By the time we arrived, the film ___.', 1, NULL, NULL, NULL),
('CORE_GRAMMAR', 4, N'You ___ bring a ticket because registration is free.', 2, NULL, NULL, NULL),
('CORE_GRAMMAR', 5, N'He asked me where I ___ the previous weekend.', 3, NULL, NULL, NULL),
('CORE_VOCAB', 1, N'The new timetable is more ___ because buses run every ten minutes.', 1, NULL, NULL, NULL),
('CORE_VOCAB', 2, N'The word closest in meaning to reliable is ___.', 2, NULL, NULL, NULL),
('CORE_VOCAB', 3, N'Please ___ attention to the safety instructions.', 3, NULL, NULL, NULL),
('CORE_VOCAB', 4, N'The project was delayed because of a shortage of ___.', 1, NULL, NULL, NULL),
('CORE_VOCAB', 5, N'We need to ___ a decision before Friday.', 2, NULL, NULL, NULL),
('READING_PART_1', 1, N'The garden meeting takes place every ___.', 2, NULL, NULL, NULL),
('READING_PART_1', 2, N'Mina usually arrives early to help prepare the ___.', 1, NULL, NULL, NULL),
('READING_PART_1', 3, N'Mina suggests that Alex should ___ the garden.', 3, NULL, NULL, NULL),
('READING_PART_1', 4, N'The email is mainly about Mina''s new ___.', 2, NULL, NULL, NULL),
('READING_PART_1', 5, N'The tone of the email is ___.', 1, NULL, NULL, NULL),
('READING_PART_2', 1, N'The workshop opened with a short explanation of the repair process.', 1, NULL, NULL, NULL),
('READING_PART_2', 2, N'First, each participant chose a broken household object.', 2, NULL, NULL, NULL),
('READING_PART_2', 3, N'Then the instructor showed everyone how to identify the safest tool.', 3, NULL, NULL, NULL),
('READING_PART_2', 4, N'After that, the group worked in pairs for nearly an hour.', 4, NULL, NULL, NULL),
('READING_PART_2', 5, N'At the end, everyone tested the repaired objects.', 5, NULL, NULL, NULL),
('READING_PART_2', 6, N'Finally, the instructor explained how to maintain each repair safely.', 6, NULL, NULL, NULL),
('READING_PART_3', 1, N'Public transport should be cheaper for students.', 2, NULL, NULL, NULL),
('READING_PART_3', 2, N'Cycling is the best way to avoid traffic.', 4, NULL, NULL, NULL),
('READING_PART_3', 3, N'Late evening services are important.', 1, NULL, NULL, NULL),
('READING_PART_3', 4, N'People should be encouraged to leave their cars at home.', 3, NULL, NULL, NULL),
('READING_PART_3', 5, N'Bus routes should connect residential areas with hospitals.', 2, NULL, NULL, NULL),
('READING_PART_3', 6, N'Protected cycle lanes would make commuting easier.', 4, NULL, NULL, NULL),
('READING_PART_3', 7, N'Transport information should be available in several languages.', 1, NULL, NULL, NULL),
('READING_PART_4', 1, N'Paragraph A', 1, NULL, NULL, NULL),
('READING_PART_4', 2, N'Paragraph B', 6, NULL, NULL, NULL),
('READING_PART_4', 3, N'Paragraph C', 2, NULL, NULL, NULL),
('READING_PART_4', 4, N'Paragraph D', 3, NULL, NULL, NULL),
('READING_PART_4', 5, N'Paragraph E', 5, NULL, NULL, NULL),
('READING_PART_4', 6, N'Paragraph F', 7, NULL, NULL, NULL),
('READING_PART_4', 7, N'Paragraph G', 8, NULL, NULL, NULL),
('LISTENING_PART_1', 1, N'What time will the evening class start?', 2, NULL, NULL, NULL),
('LISTENING_PART_1', 2, N'Where should late arrivals go?', 3, NULL, NULL, NULL),
('LISTENING_PART_1', 3, N'What should participants bring?', 1, NULL, NULL, NULL),
('LISTENING_PART_2', 1, N'Speaker A', 3, NULL, NULL, NULL),
('LISTENING_PART_2', 2, N'Speaker B', 1, NULL, NULL, NULL),
('LISTENING_PART_2', 3, N'Speaker C', 4, NULL, NULL, NULL),
('LISTENING_PART_2', 4, N'Speaker D', 2, NULL, NULL, NULL),
('LISTENING_PART_3', 1, N'The new arrangement will reduce commuting stress.', 3, NULL, NULL, NULL),
('LISTENING_PART_3', 2, N'New employees may need more face-to-face support.', 1, NULL, NULL, NULL),
('LISTENING_PART_3', 3, N'The speakers agree to test the plan before making it permanent.', 2, NULL, NULL, NULL),
('LISTENING_PART_3', 4, N'The speakers think evidence should guide the final decision.', 3, NULL, NULL, NULL),
('LISTENING_PART_4', 1, N'What is the speaker''s overall attitude to the cycling scheme?', 2, NULL, NULL, NULL),
('LISTENING_PART_4', 2, N'What evidence does the speaker say is still needed?', 3, NULL, NULL, NULL),
('WRITING_PART_1', 1, N'What is your first name?', NULL, 1, 5, NULL),
('WRITING_PART_1', 2, N'Which type of photograph do you enjoy?', NULL, 1, 5, NULL),
('WRITING_PART_1', 3, N'Which day can you attend?', NULL, 1, 5, NULL),
('WRITING_PART_1', 4, N'What camera do you use?', NULL, 1, 5, NULL),
('WRITING_PART_1', 5, N'Would you like to join the outdoor project?', NULL, 1, 5, NULL),
('WRITING_PART_2', 1, N'Write a message to a new club member. Mention the Saturday meeting, the community centre and a camera or phone.', NULL, 20, 30, NULL),
('WRITING_PART_3', 1, N'Which place is best for taking photographs in your town? Explain why.', NULL, 30, 40, NULL),
('WRITING_PART_3', 2, N'What makes a photography club useful for beginners?', NULL, 30, 40, NULL),
('WRITING_PART_3', 3, N'Would you prefer a weekend workshop or an evening workshop? Give a reason.', NULL, 30, 40, NULL),
('WRITING_PART_4', 1, N'Write an informal email to a friend about the photography course timetable changing.', NULL, 40, 50, NULL),
('WRITING_PART_4', 2, N'Write a formal email to the course organiser. Explain the problem and suggest a practical alternative.', NULL, 120, 150, NULL),
('SPEAKING_PART_1', 1, N'Where do you live and what do you like about the place?', NULL, NULL, NULL, 30),
('SPEAKING_PART_1', 2, N'What do you usually do at weekends?', NULL, NULL, NULL, 30),
('SPEAKING_PART_1', 3, N'What kind of English practice is most useful for you?', NULL, NULL, NULL, 30),
('SPEAKING_PART_2', 1, N'Describe the people, place and activity in the photograph.', NULL, NULL, NULL, 45),
('SPEAKING_PART_2', 2, N'Tell me about a time when you worked with other people.', NULL, NULL, NULL, 45),
('SPEAKING_PART_2', 3, N'Why are community activities important?', NULL, NULL, NULL, 45),
('SPEAKING_PART_3', 1, N'Describe the main differences between the two study photographs.', NULL, NULL, NULL, 45),
('SPEAKING_PART_3', 2, N'Which way of studying would be more effective for you and why?', NULL, NULL, NULL, 45),
('SPEAKING_PART_3', 3, N'How might technology change the way people study in the future?', NULL, NULL, NULL, 45),
('SPEAKING_PART_4', 1, N'What free activities should towns provide for young adults?', NULL, NULL, NULL, 120),
('SPEAKING_PART_4', 2, N'What benefits can these activities bring to a community?', NULL, NULL, NULL, 120),
('SPEAKING_PART_4', 3, N'What problems might a town face when organising them?', NULL, NULL, NULL, 120);

DECLARE @optionDefinitions TABLE
(
    question_key varchar(80) NOT NULL,
    item_position int NOT NULL,
    option_position int NOT NULL,
    option_code varchar(5) NOT NULL,
    content nvarchar(1000) NOT NULL,
    is_correct bit NOT NULL,
    PRIMARY KEY (question_key, item_position, option_position)
);

INSERT @optionDefinitions
    (question_key, item_position, option_position, option_code, content, is_correct)
VALUES
('CORE_GRAMMAR', 1, 1, 'A', N'is', 0), ('CORE_GRAMMAR', 1, 2, 'B', N'was', 0), ('CORE_GRAMMAR', 1, 3, 'C', N'will be', 1),
('CORE_GRAMMAR', 2, 1, 'A', N'since', 0), ('CORE_GRAMMAR', 2, 2, 'B', N'for', 1), ('CORE_GRAMMAR', 2, 3, 'C', N'during', 0),
('CORE_GRAMMAR', 3, 1, 'A', N'had started', 1), ('CORE_GRAMMAR', 3, 2, 'B', N'starts', 0), ('CORE_GRAMMAR', 3, 3, 'C', N'has started', 0),
('CORE_GRAMMAR', 4, 1, 'A', N'must', 0), ('CORE_GRAMMAR', 4, 2, 'B', N'do not have to', 1), ('CORE_GRAMMAR', 4, 3, 'C', N'could not', 0),
('CORE_GRAMMAR', 5, 1, 'A', N'goes', 0), ('CORE_GRAMMAR', 5, 2, 'B', N'has gone', 0), ('CORE_GRAMMAR', 5, 3, 'C', N'had gone', 1),
('CORE_VOCAB', 1, 1, 'A', N'convenient', 1), ('CORE_VOCAB', 1, 2, 'B', N'fragile', 0), ('CORE_VOCAB', 1, 3, 'C', N'rare', 0),
('CORE_VOCAB', 2, 1, 'A', N'uncertain', 0), ('CORE_VOCAB', 2, 2, 'B', N'dependable', 1), ('CORE_VOCAB', 2, 3, 'C', N'ordinary', 0),
('CORE_VOCAB', 3, 1, 'A', N'make', 0), ('CORE_VOCAB', 3, 2, 'B', N'pay', 1), ('CORE_VOCAB', 3, 3, 'C', N'keep', 0),
('CORE_VOCAB', 4, 1, 'A', N'staff', 1), ('CORE_VOCAB', 4, 2, 'B', N'permission', 0), ('CORE_VOCAB', 4, 3, 'C', N'silence', 0),
('CORE_VOCAB', 5, 1, 'A', N'do', 0), ('CORE_VOCAB', 5, 2, 'B', N'make', 1), ('CORE_VOCAB', 5, 3, 'C', N'put', 0),
('READING_PART_1', 1, 1, 'A', N'Monday', 0), ('READING_PART_1', 1, 2, 'B', N'Saturday', 1), ('READING_PART_1', 1, 3, 'C', N'Sunday', 0),
('READING_PART_1', 2, 1, 'A', N'tools', 1), ('READING_PART_1', 2, 2, 'B', N'flowers', 0), ('READING_PART_1', 2, 3, 'C', N'books', 0),
('READING_PART_1', 3, 1, 'A', N'clean', 0), ('READING_PART_1', 3, 2, 'B', N'close', 0), ('READING_PART_1', 3, 3, 'C', N'visit', 1),
('READING_PART_1', 4, 1, 'A', N'new job', 0), ('READING_PART_1', 4, 2, 'B', N'new hobby', 1), ('READING_PART_1', 4, 3, 'C', N'new house', 0),
('READING_PART_1', 5, 1, 'A', N'friendly', 1), ('READING_PART_1', 5, 2, 'B', N'angry', 0), ('READING_PART_1', 5, 3, 'C', N'formal', 0),
('LISTENING_PART_1', 1, 1, 'A', N'6:00', 0), ('LISTENING_PART_1', 1, 2, 'B', N'6:30', 1), ('LISTENING_PART_1', 1, 3, 'C', N'7:00', 0),
('LISTENING_PART_1', 2, 1, 'A', N'the main hall', 0), ('LISTENING_PART_1', 2, 2, 'B', N'the café', 0), ('LISTENING_PART_1', 2, 3, 'C', N'the side entrance', 1),
('LISTENING_PART_1', 3, 1, 'A', N'a notebook', 1), ('LISTENING_PART_1', 3, 2, 'B', N'a passport', 0), ('LISTENING_PART_1', 3, 3, 'C', N'a bicycle', 0),
('LISTENING_PART_3', 1, 1, 'A', N'the man', 0), ('LISTENING_PART_3', 1, 2, 'B', N'the woman', 0), ('LISTENING_PART_3', 1, 3, 'C', N'both speakers', 1),
('LISTENING_PART_3', 2, 1, 'A', N'the man', 1), ('LISTENING_PART_3', 2, 2, 'B', N'the woman', 0), ('LISTENING_PART_3', 2, 3, 'C', N'both speakers', 0),
('LISTENING_PART_3', 3, 1, 'A', N'the man', 0), ('LISTENING_PART_3', 3, 2, 'B', N'the woman', 1), ('LISTENING_PART_3', 3, 3, 'C', N'both speakers', 0),
('LISTENING_PART_3', 4, 1, 'A', N'the man', 0), ('LISTENING_PART_3', 4, 2, 'B', N'the woman', 0), ('LISTENING_PART_3', 4, 3, 'C', N'both speakers', 1),
('LISTENING_PART_4', 1, 1, 'A', N'completely negative', 0), ('LISTENING_PART_4', 1, 2, 'B', N'cautiously supportive', 1), ('LISTENING_PART_4', 1, 3, 'C', N'uninterested', 0),
('LISTENING_PART_4', 2, 1, 'A', N'a longer comparison with other districts', 0), ('LISTENING_PART_4', 2, 2, 'B', N'a survey about ticket prices only', 0), ('LISTENING_PART_4', 2, 3, 'C', N'interviews and wider traffic data', 1);

INSERT @optionDefinitions(question_key, item_position, option_position, option_code, content, is_correct)
SELECT 'READING_PART_3', item.position, options.option_position, options.option_code, options.content,
       CONVERT(bit, CASE WHEN item.correct_position = options.option_position THEN 1 ELSE 0 END)
FROM @itemDefinitions item
CROSS JOIN (VALUES
    (1, 'A', N'Leah'), (2, 'B', N'Omar'), (3, 'C', N'Priya'), (4, 'D', N'Jon')) options(option_position, option_code, content)
WHERE item.question_key = 'READING_PART_3';

INSERT @optionDefinitions(question_key, item_position, option_position, option_code, content, is_correct)
SELECT 'READING_PART_4', item.position, options.option_position, options.option_code, options.content,
       CONVERT(bit, CASE WHEN item.correct_position = options.option_position THEN 1 ELSE 0 END)
FROM @itemDefinitions item
CROSS JOIN (VALUES
    (1, 'A', N'How delivery times were changed'),
    (2, 'B', N'Evidence from air-quality checks'),
    (3, 'C', N'A safer and busier centre'),
    (4, 'D', N'A cheaper way to build roads'),
    (5, 'E', N'Limits of applying one timetable'),
    (6, 'F', N'Concerns before the trial'),
    (7, 'G', N'Why other towns are waiting'),
    (8, 'H', N'A wider lesson from the policy')) options(option_position, option_code, content)
WHERE item.question_key = 'READING_PART_4';

INSERT @optionDefinitions(question_key, item_position, option_position, option_code, content, is_correct)
SELECT 'LISTENING_PART_2', item.position, options.option_position, options.option_code, options.content,
       CONVERT(bit, CASE WHEN item.correct_position = options.option_position THEN 1 ELSE 0 END)
FROM @itemDefinitions item
CROSS JOIN (VALUES
    (1, 'A', N'To meet people in a new town'),
    (2, 'B', N'To fit around family responsibilities'),
    (3, 'C', N'To gain skills for a new job'),
    (4, 'D', N'To try something different from work'),
    (5, 'E', N'To prepare for a university course'),
    (6, 'F', N'To improve a competitive hobby')) options(option_position, option_code, content)
WHERE item.question_key = 'LISTENING_PART_2';

INSERT question_items(question_id, content, correct_position, position, min_word_count, max_word_count, response_seconds)
SELECT qm.question_id, d.content, d.correct_position, d.position, d.min_word_count, d.max_word_count, d.response_seconds
FROM @itemDefinitions d
JOIN @questionMap qm ON qm.question_key = d.question_key
WHERE NOT EXISTS
(
    SELECT 1 FROM question_items existing
    WHERE existing.question_id = qm.question_id AND existing.position = d.position
);

INSERT question_options(question_item_id, content, option_code, is_correct, position)
SELECT item.id, o.content, o.option_code, o.is_correct, o.option_position
FROM @optionDefinitions o
JOIN @questionMap qm ON qm.question_key = o.question_key
JOIN question_items item ON item.question_id = qm.question_id AND item.position = o.item_position
WHERE NOT EXISTS
(
    SELECT 1 FROM question_options existing
    WHERE existing.question_item_id = item.id AND existing.position = o.option_position
);

/* The transcripts are original and are kept here for the media upload checklist. */
DECLARE @mediaDefinitions TABLE
(
    question_key varchar(80) NOT NULL PRIMARY KEY,
    media_url nvarchar(500) NOT NULL,
    filename nvarchar(255) NOT NULL,
    alt_text nvarchar(500) NOT NULL,
    transcript nvarchar(max) NOT NULL
);

INSERT @mediaDefinitions(question_key, media_url, filename, alt_text, transcript)
VALUES
('LISTENING_PART_1', N'curriculum/audio/aptis-original-listening-part-1.wav', N'aptis-original-listening-part-1.wav', N'Community centre evening class announcement',
 N'Hello everyone. The evening photography class will begin at half past six, not six o''clock. If you arrive late, please use the side entrance because the main hall will be closed. Bring a notebook for the first activity.'),
('LISTENING_PART_2', N'curriculum/audio/aptis-original-listening-part-2.wav', N'aptis-original-listening-part-2.wav', N'Four speakers explain why they chose a weekend course',
 N'Speaker A chose the course to meet people after moving to a new town. Speaker B wants practical skills for a new job. Speaker C is interested in trying an activity that is different from work. Speaker D chose the course because the timetable fits around family responsibilities.'),
('LISTENING_PART_3', N'curriculum/audio/aptis-original-listening-part-3.wav', N'aptis-original-listening-part-3.wav', N'Discussion about a flexible working pilot',
 N'Man: Working from home could reduce commuting stress. Woman: I agree, although new employees may need more face-to-face support. Man: We can test the arrangement for six weeks. Woman: That seems sensible. We should use the evidence before making it permanent. Man: Yes, the final decision should be based on the feedback.'),
('LISTENING_PART_4', N'curriculum/audio/aptis-original-listening-part-4.wav', N'aptis-original-listening-part-4.wav', N'Commentary about evaluating a city cycling scheme',
 N'The city cycling scheme has encouraged more registered journeys, which is promising. However, the figures alone do not prove that car journeys have been replaced. A longer comparison with other districts, together with user interviews and traffic data, is still needed. The scheme may be worthwhile, but the evidence should be evaluated carefully.');

IF @mediaReady = 1
BEGIN
    UPDATE existing
    SET media_url = md.media_url,
        original_filename = md.filename,
        content_type = 'audio/wav',
        alt_text = md.alt_text,
        transcript = md.transcript,
        file_size = NULL
    FROM question_media existing
    JOIN @mediaDefinitions md
      ON existing.media_url = N'speech://' + md.question_key
    JOIN @questionMap qm ON qm.question_key = md.question_key AND existing.question_id = qm.question_id;

    INSERT question_media(question_id, media_type, media_url, original_filename, content_type, position, alt_text, transcript, max_play_count, created_at)
    SELECT qm.question_id, 'AUDIO', md.media_url, md.filename, 'audio/wav', 1, md.alt_text, md.transcript, 2, SYSDATETIME()
    FROM @mediaDefinitions md
    JOIN @questionMap qm ON qm.question_key = md.question_key
    WHERE NOT EXISTS
    (
        SELECT 1 FROM question_media existing
        WHERE existing.question_id = qm.question_id AND existing.position = 1
    );
END;
ELSE
BEGIN
    /* Browser speech keeps the seed usable until licensed WAV files are uploaded. */
    INSERT question_media(question_id, media_type, media_url, original_filename, content_type, position, alt_text, transcript, max_play_count, created_at)
    SELECT qm.question_id, 'AUDIO', N'speech://' + md.question_key, md.filename + N' (giọng đọc trình duyệt)', 'audio/x-aptis-speech', 1, md.alt_text, md.transcript, 2, SYSDATETIME()
    FROM @mediaDefinitions md
    JOIN @questionMap qm ON qm.question_key = md.question_key
    WHERE NOT EXISTS
    (
        SELECT 1 FROM question_media existing
        WHERE existing.question_id = qm.question_id AND existing.position = 1
    );
END;

DECLARE @examMap TABLE
(
    exam_key varchar(80) NOT NULL PRIMARY KEY,
    exam_id int NOT NULL,
    skill varchar(30) NULL
);

DECLARE @examDefinitions TABLE
(
    exam_key varchar(80) NOT NULL PRIMARY KEY,
    title nvarchar(255) NOT NULL,
    description nvarchar(max) NOT NULL,
    exam_type varchar(30) NOT NULL,
    target_level varchar(10) NOT NULL,
    skill varchar(30) NULL,
    duration_minutes int NOT NULL
);

INSERT @examDefinitions
    (exam_key, title, description, exam_type, target_level, skill, duration_minutes)
VALUES
('FULL_MOCK', N'Aptis General Original Full Mock B1', N'Bộ đề tự biên soạn theo cấu trúc Aptis General gồm Grammar & Vocabulary, Reading, Listening, Writing và Speaking.', 'FULL_MOCK', 'B1', NULL, 162),
('PRACTICE_CORE', N'Aptis Grammar and Vocabulary Original Practice B1', N'Bài luyện Core tự biên soạn gồm Grammar và Vocabulary.', 'PRACTICE', 'B1', 'GRAMMAR_VOCABULARY', 25),
('PRACTICE_READING', N'Aptis Reading Original Practice B1', N'Bài luyện Reading tự biên soạn theo bốn dạng bài Aptis.', 'PRACTICE', 'B1', 'READING', 35),
('PRACTICE_LISTENING', N'Aptis Listening Original Practice B1', N'Bài luyện Listening tự biên soạn theo bốn dạng bài Aptis. Có thể dùng WAV hoặc giọng đọc tiếng Anh của trình duyệt.', 'PRACTICE', 'B1', 'LISTENING', 40),
('PRACTICE_WRITING', N'Aptis Writing Original Practice B1', N'Bài luyện Writing tự biên soạn gồm bốn nhiệm vụ liên kết theo một chủ đề.', 'PRACTICE', 'B1', 'WRITING', 50),
('PRACTICE_SPEAKING', N'Aptis Speaking Original Practice B1', N'Bài luyện Speaking tự biên soạn gồm bốn phần. Cần upload ảnh cho phần mô tả và so sánh nếu muốn dùng media.', 'PRACTICE', 'B1', 'SPEAKING', 12);

DECLARE @examKey varchar(80), @examTitle nvarchar(255), @examDescription nvarchar(max), @examType varchar(30),
        @targetLevel varchar(10), @examSkill varchar(30), @duration int, @examId int;
DECLARE exam_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT exam_key, title, description, exam_type, target_level, skill, duration_minutes
    FROM @examDefinitions ORDER BY exam_key;

OPEN exam_cursor;
FETCH NEXT FROM exam_cursor INTO @examKey, @examTitle, @examDescription, @examType, @targetLevel, @examSkill, @duration;
WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @examId = NULL;
    SELECT TOP (1) @examId = id
    FROM mock_exams
    WHERE title = @examTitle AND exam_type = @examType AND target_level = @targetLevel;

    IF @examId IS NULL
    BEGIN
        INSERT mock_exams(certificate_id, scoring_scheme_id, title, description, exam_type, target_level, status, skill, duration_minutes, demo_only, created_at, updated_at)
        VALUES(@certificateId, @scoringSchemeId, @examTitle, @examDescription, @examType, @targetLevel, 'PUBLISHED', @examSkill, @duration, 0, SYSDATETIME(), SYSDATETIME());
        SET @examId = CONVERT(int, SCOPE_IDENTITY());
    END;
    INSERT @examMap(exam_key, exam_id, skill) VALUES(@examKey, @examId, @examSkill);
    FETCH NEXT FROM exam_cursor INTO @examKey, @examTitle, @examDescription, @examType, @targetLevel, @examSkill, @duration;
END;
CLOSE exam_cursor;
DEALLOCATE exam_cursor;

DECLARE @sectionMap TABLE(exam_key varchar(80) NOT NULL, skill varchar(30) NOT NULL, section_id int NOT NULL, PRIMARY KEY(exam_key, skill));

INSERT mock_exam_sections(mock_exam_id, skill, display_order, duration_minutes, status)
SELECT em.exam_id, s.skill, s.display_order, s.duration_minutes, 'ACTIVE'
FROM @examMap em
JOIN
(
    SELECT 'FULL_MOCK' exam_key, 'GRAMMAR_VOCABULARY' skill, 1 display_order, 25 duration_minutes UNION ALL
    SELECT 'FULL_MOCK', 'READING', 2, 35 UNION ALL
    SELECT 'FULL_MOCK', 'LISTENING', 3, 40 UNION ALL
    SELECT 'FULL_MOCK', 'WRITING', 4, 50 UNION ALL
    SELECT 'FULL_MOCK', 'SPEAKING', 5, 12 UNION ALL
    SELECT 'PRACTICE_CORE', 'GRAMMAR_VOCABULARY', 1, 25 UNION ALL
    SELECT 'PRACTICE_READING', 'READING', 1, 35 UNION ALL
    SELECT 'PRACTICE_LISTENING', 'LISTENING', 1, 40 UNION ALL
    SELECT 'PRACTICE_WRITING', 'WRITING', 1, 50 UNION ALL
    SELECT 'PRACTICE_SPEAKING', 'SPEAKING', 1, 12
) s ON s.exam_key = em.exam_key
WHERE NOT EXISTS
(
    SELECT 1 FROM mock_exam_sections existing
    WHERE existing.mock_exam_id = em.exam_id AND existing.skill = s.skill
);

INSERT @sectionMap(exam_key, skill, section_id)
SELECT em.exam_key, s.skill, section.id
FROM @examMap em
JOIN mock_exam_sections section ON section.mock_exam_id = em.exam_id
JOIN
(
    SELECT 'FULL_MOCK' exam_key, 'GRAMMAR_VOCABULARY' skill UNION ALL
    SELECT 'FULL_MOCK', 'READING' UNION ALL
    SELECT 'FULL_MOCK', 'LISTENING' UNION ALL
    SELECT 'FULL_MOCK', 'WRITING' UNION ALL
    SELECT 'FULL_MOCK', 'SPEAKING' UNION ALL
    SELECT 'PRACTICE_CORE', 'GRAMMAR_VOCABULARY' UNION ALL
    SELECT 'PRACTICE_READING', 'READING' UNION ALL
    SELECT 'PRACTICE_LISTENING', 'LISTENING' UNION ALL
    SELECT 'PRACTICE_WRITING', 'WRITING' UNION ALL
    SELECT 'PRACTICE_SPEAKING', 'SPEAKING'
) s ON s.exam_key = em.exam_key AND s.skill = section.skill
WHERE NOT EXISTS
(
    SELECT 1 FROM @sectionMap existing
    WHERE existing.exam_key = em.exam_key AND existing.skill = s.skill
);

DECLARE @fullAssignments TABLE(question_key varchar(80) NOT NULL PRIMARY KEY, position int NOT NULL);
INSERT @fullAssignments(question_key, position)
VALUES
('CORE_GRAMMAR', 1), ('CORE_VOCAB', 2),
('READING_PART_1', 3), ('READING_PART_2', 4), ('READING_PART_3', 5), ('READING_PART_4', 6),
('LISTENING_PART_1', 7), ('LISTENING_PART_2', 8), ('LISTENING_PART_3', 9), ('LISTENING_PART_4', 10),
('WRITING_PART_1', 11), ('WRITING_PART_2', 12), ('WRITING_PART_3', 13), ('WRITING_PART_4', 14),
('SPEAKING_PART_1', 15), ('SPEAKING_PART_2', 16), ('SPEAKING_PART_3', 17), ('SPEAKING_PART_4', 18);

INSERT mock_exam_questions(mock_exam_id, mock_exam_section_id, question_id, position)
SELECT exam.exam_id, section.section_id, qm.question_id, fullOrder.position
FROM @examMap exam
JOIN @fullAssignments fullOrder ON exam.exam_key = 'FULL_MOCK'
JOIN @questionMap qm ON qm.question_key = fullOrder.question_key
JOIN @sectionMap section ON section.exam_key = 'FULL_MOCK' AND section.skill = qm.skill
WHERE NOT EXISTS
(
    SELECT 1 FROM mock_exam_questions existing
    WHERE existing.mock_exam_id = exam.exam_id AND existing.question_id = qm.question_id
);

INSERT mock_exam_questions(mock_exam_id, mock_exam_section_id, question_id, position)
SELECT exam.exam_id, section.section_id, qm.question_id,
       ROW_NUMBER() OVER (PARTITION BY exam.exam_id ORDER BY qm.part_no, qm.question_key)
FROM @examMap exam
JOIN @sectionMap section ON section.exam_key = exam.exam_key AND section.skill = exam.skill
JOIN @questionMap qm ON qm.skill = exam.skill
WHERE exam.exam_key <> 'FULL_MOCK'
  AND NOT EXISTS
(
    SELECT 1 FROM mock_exam_questions existing
    WHERE existing.mock_exam_id = exam.exam_id AND existing.question_id = qm.question_id
);

/* Repair the existing Reading item shown in the admin preview when its passage was omitted. */
UPDATE q
SET reading_passage = N'The community photography course starts next Monday at the Riverside Learning Centre. Classes take place every Monday evening from 6:30 to 8:00. Learners should bring a phone or camera. The first session introduces basic composition and is suitable for beginners. The course fee includes printed notes, but learners need to pay for their own transport.'
FROM questions q
WHERE q.skill = 'READING'
  AND (q.reading_passage IS NULL OR LTRIM(RTRIM(q.reading_passage)) = N'')
  AND q.content = N'Choose the best answer for each item.'
  AND EXISTS
  (
      SELECT 1 FROM question_items item
      WHERE item.question_id = q.id
        AND item.content = N'The course starts next Monday.'
  );

COMMIT TRANSACTION;

SELECT exam_key, exam_id, skill FROM @examMap ORDER BY exam_key;
SELECT question_key, question_id, skill, part_no FROM @questionMap ORDER BY skill, part_no, question_key;
