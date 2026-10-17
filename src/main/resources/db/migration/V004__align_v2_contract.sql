-- V2 is the canonical contract. Existing extension columns are retained to avoid data loss.
ALTER TABLE question_items DROP CONSTRAINT ck_question_items_order;
ALTER TABLE question_options DROP CONSTRAINT ck_question_options_order;
ALTER TABLE learning_path_items DROP CONSTRAINT ck_path_items_order;
GO
EXEC sp_rename 'question_items.display_order', 'position', 'COLUMN';
EXEC sp_rename 'question_options.display_order', 'position', 'COLUMN';
EXEC sp_rename 'question_media.display_order', 'position', 'COLUMN';
EXEC sp_rename 'question_media.file_path', 'media_url', 'COLUMN';
EXEC sp_rename 'mock_exam_questions.display_order', 'position', 'COLUMN';
EXEC sp_rename 'learning_path_items.display_order', 'position', 'COLUMN';
GO

ALTER TABLE question_items ADD CONSTRAINT ck_question_items_order CHECK (position > 0);
ALTER TABLE question_options ADD CONSTRAINT ck_question_options_order CHECK (position > 0);
ALTER TABLE learning_path_items ADD CONSTRAINT ck_path_items_order CHECK (position > 0);

ALTER TABLE mock_exams ADD skill VARCHAR(30) NULL, duration_minutes INT NULL;
GO
ALTER TABLE mock_exams ADD CONSTRAINT ck_mock_exams_duration
    CHECK (duration_minutes IS NULL OR duration_minutes > 0);

ALTER TABLE mock_exam_questions ADD mock_exam_id INT NULL;
GO
UPDATE mq SET mock_exam_id = s.mock_exam_id
FROM mock_exam_questions mq JOIN mock_exam_sections s ON s.id = mq.mock_exam_section_id;

-- V2 requires each question and position to be unique across the whole exam.
IF EXISTS (SELECT mock_exam_id, question_id FROM mock_exam_questions GROUP BY mock_exam_id, question_id HAVING COUNT(*) > 1)
    THROW 50001, 'V2 alignment: duplicate questions across sections must be resolved first.', 1;
IF EXISTS (SELECT mock_exam_id, position FROM mock_exam_questions GROUP BY mock_exam_id, position HAVING COUNT(*) > 1)
    THROW 50002, 'V2 alignment: exam question positions must be globally unique before migration.', 1;

ALTER TABLE mock_exam_questions ALTER COLUMN mock_exam_id INT NOT NULL;
ALTER TABLE mock_exam_questions ALTER COLUMN mock_exam_section_id INT NULL;
ALTER TABLE mock_exam_questions ADD CONSTRAINT fk_v2_exam_questions_exam
    FOREIGN KEY (mock_exam_id) REFERENCES mock_exams(id);
ALTER TABLE mock_exam_questions ADD CONSTRAINT uq_v2_exam_question UNIQUE (mock_exam_id, question_id);
ALTER TABLE mock_exam_questions ADD CONSTRAINT uq_v2_exam_position UNIQUE (mock_exam_id, position);
ALTER TABLE mock_exam_sections ADD CONSTRAINT uq_v2_section_id_exam UNIQUE (id, mock_exam_id);
ALTER TABLE mock_exam_questions ADD CONSTRAINT fk_v2_question_section_exam
    FOREIGN KEY (mock_exam_section_id, mock_exam_id) REFERENCES mock_exam_sections(id, mock_exam_id);

ALTER TABLE user_answers ADD
    text_answer NVARCHAR(MAX) NULL,
    audio_url NVARCHAR(500) NULL,
    ai_score DECIMAL(5,2) NULL,
    ai_feedback NVARCHAR(MAX) NULL,
    graded_at DATETIME2 NULL,
    transcript NVARCHAR(MAX) NULL,
    ai_provider VARCHAR(30) NULL,
    ai_model VARCHAR(100) NULL;
GO
-- Never collapse multiple free-response items silently into one V2 answer.
IF EXISTS (
    SELECT user_answer_id FROM user_answer_details
    WHERE answer_text IS NOT NULL OR audio_path IS NOT NULL
    GROUP BY user_answer_id HAVING COUNT(*) > 1
)
    THROW 50003, 'V2 alignment: multiple free-response items require manual data mapping.', 1;

UPDATE a SET text_answer = d.answer_text, audio_url = d.audio_path,
    ai_score = d.auto_score, ai_feedback = d.feedback, graded_at = d.graded_at,
    transcript = d.transcript
FROM user_answers a JOIN user_answer_details d ON d.user_answer_id = a.id
WHERE d.answer_text IS NOT NULL OR d.audio_path IS NOT NULL;

ALTER TABLE user_answers ADD CONSTRAINT ck_v2_answer_ai_score
    CHECK (ai_score IS NULL OR ai_score BETWEEN 0 AND 50);

UPDATE questions SET question_type = 'WRITING_TEXT' WHERE question_type = 'TEXT_RESPONSE';
UPDATE questions SET question_type = 'SPEAKING_AUDIO' WHERE question_type = 'AUDIO_RESPONSE';
ALTER TABLE questions ADD CONSTRAINT ck_v2_question_type
    CHECK (question_type IN ('SINGLE_CHOICE','MATCHING','ORDERING','WRITING_TEXT','SPEAKING_AUDIO'));
ALTER TABLE questions ADD CONSTRAINT ck_v2_question_skill
    CHECK (skill IN ('GRAMMAR_VOCABULARY','READING','LISTENING','WRITING','SPEAKING'));

ALTER TABLE learning_paths ALTER COLUMN target_level VARCHAR(10) NULL;
ALTER TABLE learning_path_items ADD status VARCHAR(30) NOT NULL
    CONSTRAINT df_v2_path_item_status DEFAULT 'NOT_STARTED', completed_at DATETIME2 NULL;
GO
UPDATE pi SET status = lp.status, completed_at = lp.completed_at
FROM learning_path_items pi
JOIN learning_paths p ON p.id = pi.learning_path_id
JOIN lesson_progress lp ON lp.user_id = p.user_id AND lp.lesson_id = pi.lesson_id;
