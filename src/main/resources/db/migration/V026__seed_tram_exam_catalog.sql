/*
 * Seed the exam catalogue owned by Tram.
 *
 * The rows below are intentionally small but cover every catalogue/attempt
 * contract: question items/options/media, all exam types, all five skills,
 * multi-item free responses and a multi-section Full Mock.
 */
DECLARE @certificate_id INT = (SELECT id FROM certificates WHERE code = 'APTIS_GENERAL');
DECLARE @scheme_id INT = (
    SELECT TOP 1 id FROM scoring_schemes
    WHERE certificate_id = @certificate_id AND status = 'ACTIVE'
    ORDER BY version DESC
);

IF @certificate_id IS NULL OR @scheme_id IS NULL
    THROW 50026, 'Tram catalogue seed requires the Aptis certificate and scoring scheme.', 1;

DECLARE @q_gv INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Choose the correct form: My brother ___ in a bookshop.', N'Choose one answer.', 'SINGLE_CHOICE', 'GRAMMAR_VOCABULARY', 1, 'A1', 'EASY', 'PUBLISHED');
SET @q_gv = CONVERT(INT, SCOPE_IDENTITY());
DECLARE @item_gv INT;
INSERT INTO question_items (question_id, content, correct_position, position)
VALUES (@q_gv, N'My brother ___ in a bookshop.', 2, 1);
SET @item_gv = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_options (question_item_id, content, option_code, is_correct, position)
VALUES (@item_gv, N'work', 'A', 0, 1),
       (@item_gv, N'works', 'B', 1, 2),
       (@item_gv, N'working', 'C', 0, 3);

DECLARE @q_read_matching INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Match each description with the correct place.', N'Read the descriptions and choose the matching place.', 'MATCHING', 'READING', 1, 'A2', 'MEDIUM', 'PUBLISHED');
SET @q_read_matching = CONVERT(INT, SCOPE_IDENTITY());
DECLARE @item_rm1 INT, @item_rm2 INT, @item_rm3 INT;
INSERT INTO question_items (question_id, content, correct_position, position)
VALUES (@q_read_matching, N'You can borrow books here.', 1, 1),
       (@q_read_matching, N'Swimming lessons start here at six o''clock.', 2, 2),
       (@q_read_matching, N'You can meet friends for coffee here.', 3, 3);
SELECT @item_rm1 = id FROM question_items WHERE question_id = @q_read_matching AND position = 1;
SELECT @item_rm2 = id FROM question_items WHERE question_id = @q_read_matching AND position = 2;
SELECT @item_rm3 = id FROM question_items WHERE question_id = @q_read_matching AND position = 3;
INSERT INTO question_options (question_item_id, content, option_code, is_correct, position)
VALUES (@item_rm1, N'Library', 'A', 1, 1), (@item_rm1, N'Swimming pool', 'B', 0, 2), (@item_rm1, N'Café', 'C', 0, 3),
       (@item_rm2, N'Library', 'A', 0, 1), (@item_rm2, N'Swimming pool', 'B', 1, 2), (@item_rm2, N'Café', 'C', 0, 3),
       (@item_rm3, N'Library', 'A', 0, 1), (@item_rm3, N'Swimming pool', 'B', 0, 2), (@item_rm3, N'Café', 'C', 1, 3);

DECLARE @q_read_ordering INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Put the reading strategy steps in the correct order.', N'Choose the position for each step.', 'ORDERING', 'READING', 2, 'B1', 'MEDIUM', 'PUBLISHED');
SET @q_read_ordering = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_items (question_id, content, correct_position, position)
VALUES (@q_read_ordering, N'Read the question carefully.', 1, 1),
       (@q_read_ordering, N'Scan the text for the key information.', 2, 2),
       (@q_read_ordering, N'Check the answer against the evidence.', 3, 3);

DECLARE @q_listening_a2 INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Listen to the announcement and choose the correct time.', N'You can listen to the recording up to two times.', 'SINGLE_CHOICE', 'LISTENING', 1, 'A2', 'MEDIUM', 'PUBLISHED');
SET @q_listening_a2 = CONVERT(INT, SCOPE_IDENTITY());
DECLARE @item_la2 INT;
INSERT INTO question_items (question_id, content, correct_position, position)
VALUES (@q_listening_a2, N'What time does the meeting begin?', 2, 1);
SET @item_la2 = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_options (question_item_id, content, option_code, is_correct, position)
VALUES (@item_la2, N'9:00', 'A', 0, 1), (@item_la2, N'9:30', 'B', 1, 2), (@item_la2, N'10:00', 'C', 0, 3);
INSERT INTO question_media (question_id, media_type, media_url, original_filename, content_type, file_size, max_play_count, position, alt_text, transcript)
VALUES (@q_listening_a2, 'AUDIO', N'curriculum/audio/listening-a2.wav', N'listening-a2.wav', 'audio/wav', 1099246, 2, 1, N'Listening announcement for the A2 practice question', N'The meeting begins at half past nine.');

DECLARE @q_listening_b1 INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Listen to the conversation and identify the revised arrangement.', N'You can listen to the recording up to two times.', 'SINGLE_CHOICE', 'LISTENING', 2, 'B1', 'MEDIUM', 'PUBLISHED');
SET @q_listening_b1 = CONVERT(INT, SCOPE_IDENTITY());
DECLARE @item_lb1 INT;
INSERT INTO question_items (question_id, content, correct_position, position)
VALUES (@q_listening_b1, N'Where will the speakers meet?', 2, 1);
SET @item_lb1 = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_options (question_item_id, content, option_code, is_correct, position)
VALUES (@item_lb1, N'At the station', 'A', 0, 1), (@item_lb1, N'Near the café', 'B', 1, 2), (@item_lb1, N'At the library', 'C', 0, 3);
INSERT INTO question_media (question_id, media_type, media_url, original_filename, content_type, file_size, max_play_count, position, alt_text, transcript)
VALUES (@q_listening_b1, 'AUDIO', N'curriculum/audio/listening-b1.wav', N'listening-b1.wav', 'audio/wav', 1458446, 2, 1, N'Listening conversation for the B1 practice question', N'They decide to meet near the café.');

DECLARE @q_writing_b1 INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Write an email to a new member of your community club.', N'Answer every item. Use complete sentences and an appropriate tone.', 'WRITING_TEXT', 'WRITING', 1, 'B1', 'MEDIUM', 'PUBLISHED');
SET @q_writing_b1 = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_items (question_id, content, position, min_word_count, max_word_count, response_seconds)
VALUES (@q_writing_b1, N'Introduce the club and explain when the next meeting is.', 1, 35, 80, NULL),
       (@q_writing_b1, N'Give one reason why the member should attend.', 2, 20, 60, NULL);

DECLARE @q_writing_b2 INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Write a formal response to a proposal about improving a local activity centre.', N'Explain your opinion, support it with reasons and make one practical suggestion.', 'WRITING_TEXT', 'WRITING', 2, 'B2', 'HARD', 'PUBLISHED');
SET @q_writing_b2 = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_items (question_id, content, position, min_word_count, max_word_count, response_seconds)
VALUES (@q_writing_b2, N'State whether you support the proposal and explain why.', 1, 60, 130, NULL),
       (@q_writing_b2, N'Add one realistic suggestion for the centre.', 2, 30, 80, NULL);

DECLARE @q_speaking_a2 INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, preparation_seconds, status)
VALUES (N'Talk about a familiar daily activity.', N'Record one response for each item. Check your microphone before you begin.', 'SPEAKING_AUDIO', 'SPEAKING', 1, 'A2', 'MEDIUM', 30, 'PUBLISHED');
SET @q_speaking_a2 = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_items (question_id, content, position, response_seconds)
VALUES (@q_speaking_a2, N'Describe what you usually do at the weekend.', 1, 60),
       (@q_speaking_a2, N'Give one reason why you enjoy this activity.', 2, 45);

DECLARE @q_speaking_b1 INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, preparation_seconds, status)
VALUES (N'Discuss a change that could improve your local community.', N'Record one response for each item. Give reasons and an example.', 'SPEAKING_AUDIO', 'SPEAKING', 2, 'B1', 'MEDIUM', 45, 'PUBLISHED');
SET @q_speaking_b1 = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_items (question_id, content, position, response_seconds)
VALUES (@q_speaking_b1, N'Suggest one useful change for your local community.', 1, 75),
       (@q_speaking_b1, N'Explain how the change would help residents.', 2, 60);

/* A draft record keeps the Admin create -> validate -> publish path testable. */
DECLARE @q_draft INT;
INSERT INTO questions (content, instruction, question_type, skill, part_no, cefr_level, difficulty, status)
VALUES (N'Draft question for catalogue authoring review.', N'Complete and validate this item before publishing.', 'SINGLE_CHOICE', 'GRAMMAR_VOCABULARY', 2, 'A2', 'MEDIUM', 'DRAFT');
SET @q_draft = CONVERT(INT, SCOPE_IDENTITY());
DECLARE @item_draft INT;
INSERT INTO question_items (question_id, content, correct_position, position)
VALUES (@q_draft, N'This draft item is ready for an editor to review.', 1, 1);
SET @item_draft = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO question_options (question_item_id, content, option_code, is_correct, position)
VALUES (@item_draft, N'Option one', 'A', 1, 1), (@item_draft, N'Option two', 'B', 0, 2);

DECLARE @exam_id INT;
DECLARE @section_id INT;

/* One Placement and one Practice exam per skill. */
INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Placement Grammar & Vocabulary', N'Bài kiểm tra đầu vào kỹ năng Grammar & Vocabulary.', 'PLACEMENT', 'B1', 'GRAMMAR_VOCABULARY', 20, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'GRAMMAR_VOCABULARY', 1, 20, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_gv, 1);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Practice Grammar & Vocabulary A1', N'Luyện tập ngữ pháp và từ vựng cơ bản.', 'PRACTICE', 'A1', 'GRAMMAR_VOCABULARY', 15, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'GRAMMAR_VOCABULARY', 1, 15, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_gv, 1);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Placement Reading', N'Bài kiểm tra đầu vào kỹ năng Reading.', 'PLACEMENT', 'B1', 'READING', 25, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'READING', 1, 25, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_read_matching, 1), (@exam_id, @section_id, @q_read_ordering, 2);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Practice Reading A2-B1', N'Luyện tập ghép nối và sắp xếp thông tin trong bài đọc.', 'PRACTICE', 'B1', 'READING', 20, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'READING', 1, 20, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_read_matching, 1), (@exam_id, @section_id, @q_read_ordering, 2);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Placement Listening', N'Bài kiểm tra đầu vào kỹ năng Listening có giới hạn hai lượt nghe.', 'PLACEMENT', 'B1', 'LISTENING', 20, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'LISTENING', 1, 20, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_listening_a2, 1), (@exam_id, @section_id, @q_listening_b1, 2);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Practice Listening A2-B1', N'Luyện nghe với audio trong curriculum và bộ đếm lượt phát.', 'PRACTICE', 'B1', 'LISTENING', 20, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'LISTENING', 1, 20, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_listening_a2, 1), (@exam_id, @section_id, @q_listening_b1, 2);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Placement Writing', N'Bài kiểm tra đầu vào kỹ năng Writing gồm nhiều item.', 'PLACEMENT', 'B1', 'WRITING', 35, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'WRITING', 1, 35, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_writing_b1, 1);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Practice Writing B1-B2', N'Luyện viết nhiều item, bảo toàn từng câu trả lời riêng.', 'PRACTICE', 'B2', 'WRITING', 40, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'WRITING', 1, 40, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_writing_b1, 1), (@exam_id, @section_id, @q_writing_b2, 2);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Placement Speaking', N'Bài kiểm tra đầu vào kỹ năng Speaking bằng upload multipart.', 'PLACEMENT', 'B1', 'SPEAKING', 25, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'SPEAKING', 1, 25, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_speaking_a2, 1);

INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Practice Speaking A2-B1', N'Luyện nói nhiều item và lưu riêng từng file ghi âm.', 'PRACTICE', 'B1', 'SPEAKING', 25, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'SPEAKING', 1, 25, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_speaking_a2, 1), (@exam_id, @section_id, @q_speaking_b1, 2);

/* Checkpoints exist for every skill and every curriculum level. */
DECLARE @checkpoint_exams TABLE (exam_id INT, skill VARCHAR(30), question_id INT);
INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
OUTPUT inserted.id, seed.skill, seed.question_id INTO @checkpoint_exams (exam_id, skill, question_id)
SELECT @certificate_id, @scheme_id,
       N'Checkpoint ' + seed.skill + N' ' + levels.target_level,
       N'Bài kiểm tra cuối chặng cho kỹ năng ' + seed.skill + N' ở mức ' + levels.target_level + N'.',
       'CHECKPOINT', levels.target_level, seed.skill, 20, 'PUBLISHED'
FROM (VALUES
    ('GRAMMAR_VOCABULARY', @q_gv), ('READING', @q_read_matching),
    ('LISTENING', @q_listening_a2), ('WRITING', @q_writing_b1),
    ('SPEAKING', @q_speaking_a2)
) seed(skill, question_id)
CROSS JOIN (VALUES ('A1'), ('A2'), ('B1'), ('B2'), ('C1'), ('C2')) levels(target_level);

DECLARE @checkpoint_sections TABLE (exam_id INT, section_id INT);
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status)
OUTPUT inserted.mock_exam_id, inserted.id INTO @checkpoint_sections (exam_id, section_id)
SELECT exam_id, skill, 1, 20, 'ACTIVE' FROM @checkpoint_exams;
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position)
SELECT exams.exam_id, sections.section_id, exams.question_id, 1
FROM @checkpoint_exams exams
JOIN @checkpoint_sections sections ON sections.exam_id = exams.exam_id;

/* A Full Mock proves that the same attempt engine can handle multiple skills. */
INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Full Mock Aptis General B1', N'Đề mô phỏng đầy đủ năm kỹ năng, mỗi kỹ năng là một section riêng.', 'FULL_MOCK', 'B1', 'FULL_MOCK', 120, 'PUBLISHED');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
DECLARE @section_gv INT, @section_read INT, @section_listen INT, @section_write INT, @section_speak INT;
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'GRAMMAR_VOCABULARY', 1, 20, 'ACTIVE');
SET @section_gv = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'READING', 2, 25, 'ACTIVE');
SET @section_read = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'LISTENING', 3, 20, 'ACTIVE');
SET @section_listen = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'WRITING', 4, 30, 'ACTIVE');
SET @section_write = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'SPEAKING', 5, 25, 'ACTIVE');
SET @section_speak = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position)
VALUES (@exam_id, @section_gv, @q_gv, 1),
       (@exam_id, @section_read, @q_read_matching, 2),
       (@exam_id, @section_read, @q_read_ordering, 3),
       (@exam_id, @section_listen, @q_listening_a2, 4),
       (@exam_id, @section_write, @q_writing_b1, 5),
       (@exam_id, @section_speak, @q_speaking_a2, 6);

/* A draft exam is available for Admin authoring/publish checks. */
INSERT INTO mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, skill, duration_minutes, status)
VALUES (@certificate_id, @scheme_id, N'Draft Placement Reading Review', N'Đề nháp để Admin tiếp tục chọn câu, kiểm tra và publish.', 'PLACEMENT', 'A2', 'READING', 20, 'DRAFT');
SET @exam_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status) VALUES (@exam_id, 'READING', 1, 20, 'ACTIVE');
SET @section_id = CONVERT(INT, SCOPE_IDENTITY());
INSERT INTO mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position) VALUES (@exam_id, @section_id, @q_read_matching, 1);
