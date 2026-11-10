/*
   Safe compatibility repair for an existing SQL Server database.

   The current application stores exam snapshots in the relational
   exam_attempt_snapshot_* tables.  Older databases can still contain the
   legacy JSON columns on the parent tables.  Hibernate no longer maps those
   columns, so they must be nullable while the legacy data is retained.

   Run the normalized-table migrations first when those tables are missing:
     migration-no-json-exam-snapshot.sql
     migration-no-json-lesson-check.sql
     migration-no-json-lesson-context.sql
     migration-no-json-lesson-playback.sql

   This script does not delete or rewrite any data.  The legacy columns can
   be dropped later, after the corresponding backfill scripts are verified.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

IF COL_LENGTH(N'dbo.exam_attempt_snapshots', N'section_advance_millis') IS NULL
    ALTER TABLE dbo.exam_attempt_snapshots
        ADD section_advance_millis BIGINT NOT NULL
            CONSTRAINT df_exam_snapshot_section_advance_repair DEFAULT 0 WITH VALUES;

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.exam_attempt_snapshots')
      AND name = N'grading_context'
      AND is_nullable = 0
)
    EXEC(N'ALTER TABLE dbo.exam_attempt_snapshots ALTER COLUMN grading_context NVARCHAR(MAX) NULL');

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.exam_attempt_snapshots')
      AND name = N'locked_question_snapshot'
      AND is_nullable = 0
)
    EXEC(N'ALTER TABLE dbo.exam_attempt_snapshots ALTER COLUMN locked_question_snapshot NVARCHAR(MAX) NULL');

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.lesson_check_attempts')
      AND name = N'answers_json'
      AND is_nullable = 0
)
    EXEC(N'ALTER TABLE dbo.lesson_check_attempts ALTER COLUMN answers_json NVARCHAR(MAX) NULL');

COMMIT TRANSACTION;
