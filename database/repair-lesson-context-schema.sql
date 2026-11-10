/*
   Repair migration for databases where lesson_context_snapshots already exists
   but the owner tables were not updated by migration-no-json-lesson-context.sql.
   Additive and idempotent: it does not delete or rewrite application data.
*/

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.lesson_context_snapshots', N'U') IS NULL
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
END;

IF OBJECT_ID(N'dbo.lesson_check_attempts', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.lesson_check_attempts', N'context_snapshot_id') IS NULL
    ALTER TABLE dbo.lesson_check_attempts ADD context_snapshot_id int NULL;

IF OBJECT_ID(N'dbo.lesson_work_submissions', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.lesson_work_submissions', N'context_snapshot_id') IS NULL
    ALTER TABLE dbo.lesson_work_submissions ADD context_snapshot_id int NULL;

IF OBJECT_ID(N'dbo.lesson_check_attempts', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = N'fk_lesson_check_context'
         AND parent_object_id = OBJECT_ID(N'dbo.lesson_check_attempts')
   )
    ALTER TABLE dbo.lesson_check_attempts
        ADD CONSTRAINT fk_lesson_check_context
        FOREIGN KEY (context_snapshot_id)
        REFERENCES dbo.lesson_context_snapshots(id);

IF OBJECT_ID(N'dbo.lesson_work_submissions', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = N'fk_lesson_work_context'
         AND parent_object_id = OBJECT_ID(N'dbo.lesson_work_submissions')
   )
    ALTER TABLE dbo.lesson_work_submissions
        ADD CONSTRAINT fk_lesson_work_context
        FOREIGN KEY (context_snapshot_id)
        REFERENCES dbo.lesson_context_snapshots(id);

COMMIT TRANSACTION;
GO
