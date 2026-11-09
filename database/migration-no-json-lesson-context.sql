IF OBJECT_ID('dbo.lesson_context_snapshots','U') IS NULL
BEGIN
    CREATE TABLE dbo.lesson_context_snapshots (
        id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
        title nvarchar(255) NOT NULL,
        skill varchar(30) NULL,
        description nvarchar(max) NULL,
        objectives nvarchar(max) NULL,
        strategy nvarchar(max) NULL,
        example_text nvarchar(max) NULL,
        analysis nvarchar(max) NULL,
        practice_prompt nvarchar(max) NULL,
        image_path nvarchar(500) NULL,
        audio_path nvarchar(500) NULL,
        video_path nvarchar(500) NULL,
        video_url nvarchar(1000) NULL,
        min_words int NULL,
        max_words int NULL
    );
    ALTER TABLE dbo.lesson_check_attempts ADD context_snapshot_id int NULL;
    ALTER TABLE dbo.lesson_work_submissions ADD context_snapshot_id int NULL;
    ALTER TABLE dbo.lesson_check_attempts ADD CONSTRAINT fk_lesson_check_context FOREIGN KEY(context_snapshot_id) REFERENCES dbo.lesson_context_snapshots(id);
    ALTER TABLE dbo.lesson_work_submissions ADD CONSTRAINT fk_lesson_work_context FOREIGN KEY(context_snapshot_id) REFERENCES dbo.lesson_context_snapshots(id);
END
GO
