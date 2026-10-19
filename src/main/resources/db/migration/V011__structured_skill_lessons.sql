ALTER TABLE lessons ADD
    topic NVARCHAR(160) NULL,
    task_type NVARCHAR(160) NULL,
    objectives NVARCHAR(MAX) NULL,
    strategy NVARCHAR(MAX) NULL,
    example_text NVARCHAR(MAX) NULL,
    analysis NVARCHAR(MAX) NULL,
    common_mistakes NVARCHAR(MAX) NULL,
    practice_prompt NVARCHAR(MAX) NULL,
    sample_response NVARCHAR(MAX) NULL,
    checklist NVARCHAR(MAX) NULL,
    video_url NVARCHAR(500) NULL,
    source_url NVARCHAR(500) NULL,
    source_name NVARCHAR(255) NULL;
GO
CREATE TABLE lesson_checks (
 id INT IDENTITY(1,1) PRIMARY KEY,
 lesson_id INT NOT NULL REFERENCES lessons(id),
 position INT NOT NULL,
 prompt NVARCHAR(2000) NOT NULL,
 option_a NVARCHAR(1000) NOT NULL,
 option_b NVARCHAR(1000) NOT NULL,
 option_c NVARCHAR(1000) NOT NULL,
 correct_option INT NOT NULL CHECK(correct_option BETWEEN 0 AND 2),
 explanation NVARCHAR(4000) NOT NULL,
 CONSTRAINT uq_lesson_check_position UNIQUE(lesson_id,position)
);
