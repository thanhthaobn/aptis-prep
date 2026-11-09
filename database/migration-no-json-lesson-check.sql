IF OBJECT_ID('dbo.lesson_check_attempt_answers','U') IS NULL
BEGIN
    CREATE TABLE dbo.lesson_check_attempt_answers (
        id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
        attempt_id int NOT NULL,
        question_id int NOT NULL,
        selected_option int NOT NULL,
        CONSTRAINT fk_lesson_check_answer_attempt FOREIGN KEY(attempt_id) REFERENCES dbo.lesson_check_attempts(id)
    );
    CREATE INDEX ix_lesson_check_answer_attempt ON dbo.lesson_check_attempt_answers(attempt_id, id);
END
IF OBJECT_ID('dbo.lesson_check_attempt_feedback','U') IS NULL
BEGIN
    CREATE TABLE dbo.lesson_check_attempt_feedback (
        id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
        attempt_id int NOT NULL,
        prompt nvarchar(max) NOT NULL,
        selected nvarchar(max) NULL,
        correct nvarchar(max) NULL,
        right_answer bit NOT NULL,
        explanation nvarchar(max) NULL,
        options_text nvarchar(max) NULL,
        CONSTRAINT fk_lesson_check_feedback_attempt FOREIGN KEY(attempt_id) REFERENCES dbo.lesson_check_attempts(id)
    );
    CREATE INDEX ix_lesson_check_feedback_attempt ON dbo.lesson_check_attempt_feedback(attempt_id, id);
END
GO
