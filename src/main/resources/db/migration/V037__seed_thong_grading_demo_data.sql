DECLARE @certificate_id INT = (SELECT id FROM certificates WHERE code = 'APTIS_GENERAL');
DECLARE @scheme_id INT = (SELECT id FROM scoring_schemes WHERE certificate_id = @certificate_id AND version = '1.0');
DECLARE @exam_id INT, @section_id INT, @question_id INT, @item_id INT;

IF @certificate_id IS NOT NULL AND @scheme_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM mock_exams WHERE title = N'AptisPrep Demo · Writing · 2 câu AI')
BEGIN
    INSERT INTO mock_exams(certificate_id, scoring_scheme_id, title, description, exam_type, skill, target_level, duration_minutes, status)
    VALUES (@certificate_id, @scheme_id, N'AptisPrep Demo · Writing · 2 câu AI', N'Dữ liệu demo để kiểm tra nhiều câu cùng kỹ năng và retry AI.', 'PLACEMENT', 'WRITING', 'B1', 20, 'PUBLISHED');
    SET @exam_id = SCOPE_IDENTITY();
    INSERT INTO mock_exam_sections(mock_exam_id, skill, display_order, duration_minutes, status)
    VALUES (@exam_id, 'WRITING', 1, 20, 'ACTIVE');
    SET @section_id = SCOPE_IDENTITY();

    INSERT INTO questions(content, instruction, question_type, skill, part_no, cefr_level, status)
    VALUES (N'Write a short message to a classmate about a study plan.', N'Write 60–80 words.', 'WRITING_TEXT', 'WRITING', 1, 'B1', 'PUBLISHED');
    SET @question_id = SCOPE_IDENTITY();
    INSERT INTO question_items(question_id, content, position, min_word_count, max_word_count)
    VALUES (@question_id, N'Your message', 1, 60, 80);
    INSERT INTO mock_exam_questions(mock_exam_id, mock_exam_section_id, question_id, position)
    VALUES (@exam_id, @section_id, @question_id, 1);

    INSERT INTO questions(content, instruction, question_type, skill, part_no, cefr_level, status)
    VALUES (N'Describe one change that would improve your English study routine.', N'Write 80–100 words.', 'WRITING_TEXT', 'WRITING', 2, 'B1', 'PUBLISHED');
    SET @question_id = SCOPE_IDENTITY();
    INSERT INTO question_items(question_id, content, position, min_word_count, max_word_count)
    VALUES (@question_id, N'Your response', 1, 80, 100);
    INSERT INTO mock_exam_questions(mock_exam_id, mock_exam_section_id, question_id, position)
    VALUES (@exam_id, @section_id, @question_id, 2);
END;

IF @certificate_id IS NOT NULL AND @scheme_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM mock_exams WHERE title = N'AptisPrep Demo · Speaking · transcript')
BEGIN
    INSERT INTO mock_exams(certificate_id, scoring_scheme_id, title, description, exam_type, skill, target_level, duration_minutes, status)
    VALUES (@certificate_id, @scheme_id, N'AptisPrep Demo · Speaking · transcript', N'Dữ liệu demo Speaking qua transcript; không đánh giá phát âm/ngữ điệu.', 'PLACEMENT', 'SPEAKING', 'B1', 10, 'PUBLISHED');
    SET @exam_id = SCOPE_IDENTITY();
    INSERT INTO mock_exam_sections(mock_exam_id, skill, display_order, duration_minutes, status)
    VALUES (@exam_id, 'SPEAKING', 1, 10, 'ACTIVE');
    SET @section_id = SCOPE_IDENTITY();
    INSERT INTO questions(content, instruction, question_type, skill, part_no, cefr_level, status)
    VALUES (N'Talk about a useful way to practise English every day.', N'Speak for 45–60 seconds.', 'SPEAKING_AUDIO', 'SPEAKING', 1, 'B1', 'PUBLISHED');
    SET @question_id = SCOPE_IDENTITY();
    INSERT INTO question_items(question_id, content, position, response_seconds)
    VALUES (@question_id, N'Your recording', 1, 60);
    INSERT INTO mock_exam_questions(mock_exam_id, mock_exam_section_id, question_id, position)
    VALUES (@exam_id, @section_id, @question_id, 1);
END;

IF @certificate_id IS NOT NULL AND @scheme_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM mock_exams WHERE title = N'AptisPrep Demo · Reading · policy')
BEGIN
    INSERT INTO mock_exams(certificate_id, scoring_scheme_id, title, description, exam_type, skill, target_level, duration_minutes, status)
    VALUES (@certificate_id, @scheme_id, N'AptisPrep Demo · Reading · policy', N'Dữ liệu demo objective grading theo raw-to-scale của Reading.', 'PLACEMENT', 'READING', 'B1', 10, 'PUBLISHED');
    SET @exam_id = SCOPE_IDENTITY();
    INSERT INTO mock_exam_sections(mock_exam_id, skill, display_order, duration_minutes, status)
    VALUES (@exam_id, 'READING', 1, 10, 'ACTIVE');
    SET @section_id = SCOPE_IDENTITY();
    INSERT INTO questions(content, instruction, question_type, skill, part_no, cefr_level, status)
    VALUES (N'Choose the best answer for each item.', N'Read and select one option.', 'SINGLE_CHOICE', 'READING', 1, 'B1', 'PUBLISHED');
    SET @question_id = SCOPE_IDENTITY();
    INSERT INTO question_items(question_id, content, position) VALUES (@question_id, N'The course starts next Monday.', 1);
    SET @item_id = SCOPE_IDENTITY();
    INSERT INTO question_options(question_item_id, content, option_code, is_correct, position) VALUES
        (@item_id, N'True', 'A', 1, 1), (@item_id, N'False', 'B', 0, 2);
    INSERT INTO question_items(question_id, content, position) VALUES (@question_id, N'Learners should bring a notebook.', 2);
    SET @item_id = SCOPE_IDENTITY();
    INSERT INTO question_options(question_item_id, content, option_code, is_correct, position) VALUES
        (@item_id, N'True', 'A', 1, 1), (@item_id, N'False', 'B', 0, 2);
    INSERT INTO question_items(question_id, content, position) VALUES (@question_id, N'The final activity is optional.', 3);
    SET @item_id = SCOPE_IDENTITY();
    INSERT INTO question_options(question_item_id, content, option_code, is_correct, position) VALUES
        (@item_id, N'True', 'A', 0, 1), (@item_id, N'False', 'B', 1, 2);
    INSERT INTO question_items(question_id, content, position) VALUES (@question_id, N'The teacher sends feedback by email.', 4);
    SET @item_id = SCOPE_IDENTITY();
    INSERT INTO question_options(question_item_id, content, option_code, is_correct, position) VALUES
        (@item_id, N'True', 'A', 1, 1), (@item_id, N'False', 'B', 0, 2);
    INSERT INTO mock_exam_questions(mock_exam_id, mock_exam_section_id, question_id, position)
    VALUES (@exam_id, @section_id, @question_id, 1);
END;
