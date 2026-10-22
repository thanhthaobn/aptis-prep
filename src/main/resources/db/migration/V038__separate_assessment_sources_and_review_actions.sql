-- Additive compatibility migration. Existing jobs/results/submissions are retained.
ALTER TABLE ai_assessment_jobs ADD source_type VARCHAR(30) NOT NULL
    CONSTRAINT df_ai_job_source DEFAULT 'EXAM_ANSWER', source_id INT NULL;
GO
UPDATE ai_assessment_jobs SET source_id = answer_id;
UPDATE ai_assessment_jobs SET source_type = 'LESSON_SUBMISSION',
    source_id = COALESCE(TRY_CONVERT(INT, SUBSTRING(assessment_key,
        CHARINDEX('-submission-', assessment_key) + 12, 40)), answer_version)
WHERE assessment_key LIKE 'lesson-%-submission-%';
GO
ALTER TABLE ai_assessment_jobs ALTER COLUMN attempt_id INT NULL;
ALTER TABLE ai_assessment_jobs ALTER COLUMN answer_id INT NULL;
ALTER TABLE ai_assessments ALTER COLUMN answer_id INT NULL;
-- Legacy lesson IDs are kept intact for audit; source_type decides their domain.
CREATE TABLE flashcard_reviews (
    id VARCHAR(160) NOT NULL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id),
    flashcard_id INT NOT NULL REFERENCES flashcards(id),
    state VARCHAR(30) NOT NULL,
    reviewed_at DATETIME2 NOT NULL
);
ALTER TABLE user_answers ADD audio_manifest NVARCHAR(MAX) NULL;
GO
-- Copy legacy item answers to the V2 answer-level representation; never replace existing text.
UPDATE a SET text_answer = legacy.content
FROM user_answers a
CROSS APPLY (SELECT STRING_AGG(CAST(d.answer_text AS NVARCHAR(MAX)), NCHAR(10) + N'--- ITEM ---' + NCHAR(10))
    WITHIN GROUP (ORDER BY d.question_item_id) AS content
    FROM user_answer_details d WHERE d.user_answer_id = a.id AND NULLIF(d.answer_text, N'') IS NOT NULL) legacy
WHERE NULLIF(a.text_answer, N'') IS NULL AND legacy.content IS NOT NULL;
UPDATE a SET audio_manifest = (SELECT d.audio_path AS [path] FROM user_answer_details d
    WHERE d.user_answer_id = a.id AND NULLIF(d.audio_path, N'') IS NOT NULL
    ORDER BY d.question_item_id FOR JSON PATH)
FROM user_answers a WHERE a.audio_manifest IS NULL AND EXISTS
    (SELECT 1 FROM user_answer_details d WHERE d.user_answer_id = a.id AND NULLIF(d.audio_path, N'') IS NOT NULL);
GO
