BEGIN TRANSACTION;
ALTER TABLE dbo.exam_attempt_snapshots DROP COLUMN locked_question_snapshot, grading_context;
ALTER TABLE dbo.lesson_engagement DROP COLUMN playback_ranges_json;
ALTER TABLE dbo.lesson_check_attempts DROP COLUMN answers_json, review_json, content_snapshot;
ALTER TABLE dbo.lesson_work_submissions DROP COLUMN content_snapshot;
COMMIT;
GO
