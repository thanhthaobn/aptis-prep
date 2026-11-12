/* Practice mode item-point scoring.
   Run after the current schema and migration-no-json-exam-snapshot.sql.
   Existing questions/snapshots receive one point per item for backward compatibility. */

IF COL_LENGTH(N'dbo.question_items', N'points') IS NULL
BEGIN
    ALTER TABLE dbo.question_items
        ADD points decimal(6,2) NOT NULL
            CONSTRAINT df_question_items_points DEFAULT (1);
END;

IF OBJECT_ID(N'dbo.exam_attempt_snapshot_items', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.exam_attempt_snapshot_items', N'points') IS NULL
BEGIN
    ALTER TABLE dbo.exam_attempt_snapshot_items
        ADD points decimal(6,2) NOT NULL
            CONSTRAINT df_exam_snapshot_item_points_upgrade DEFAULT (1);
END;

IF COL_LENGTH(N'dbo.exam_skill_results', N'raw_max') IS NULL
BEGIN
    ALTER TABLE dbo.exam_skill_results ADD raw_max decimal(6,2) NULL;
END;
