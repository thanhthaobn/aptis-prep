IF COL_LENGTH('lesson_check_attempts', 'review_json') IS NULL
    ALTER TABLE lesson_check_attempts ADD review_json NVARCHAR(MAX) NULL;
GO
IF OBJECT_ID('lesson_work_submissions', 'U') IS NULL
BEGIN
    CREATE TABLE lesson_work_submissions (
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        lesson_id INT NOT NULL,
        skill VARCHAR(30) NOT NULL,
        prompt_snapshot NVARCHAR(MAX) NULL,
        response_text NVARCHAR(MAX) NULL,
        audio_path VARCHAR(500) NULL,
        submitted_at DATETIME2 NOT NULL,
        CONSTRAINT fk_lesson_submission_user FOREIGN KEY(user_id) REFERENCES users(id),
        CONSTRAINT fk_lesson_submission_lesson FOREIGN KEY(lesson_id) REFERENCES lessons(id)
    );
    CREATE INDEX ix_lesson_submission_owner ON lesson_work_submissions(user_id,lesson_id,submitted_at);
    INSERT INTO lesson_work_submissions(user_id,lesson_id,skill,prompt_snapshot,response_text,audio_path,submitted_at)
    SELECT r.user_id,r.lesson_id,l.skill,l.practice_prompt,r.response_text,r.audio_path,r.updated_at
    FROM lesson_responses r JOIN lessons l ON l.id=r.lesson_id
    WHERE (l.skill='Writing' AND LEN(LTRIM(RTRIM(r.response_text)))>0)
       OR (l.skill='Speaking' AND r.audio_path IS NOT NULL);
END;
GO
