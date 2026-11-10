/*
   Repair migration for databases where lesson_engagement exists but the
   normalized playback range table was not created.
   Additive and idempotent: it does not delete or rewrite playback data.
*/

IF OBJECT_ID(N'dbo.lesson_playback_ranges', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.lesson_playback_ranges (
        id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
        engagement_id int NOT NULL,
        start_seconds float NOT NULL,
        end_seconds float NOT NULL,
        CONSTRAINT fk_lesson_playback_range_engagement
            FOREIGN KEY (engagement_id) REFERENCES dbo.lesson_engagement(id)
    );
END;

IF OBJECT_ID(N'dbo.lesson_playback_ranges', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = N'ix_lesson_playback_range_engagement'
         AND object_id = OBJECT_ID(N'dbo.lesson_playback_ranges')
   )
    CREATE INDEX ix_lesson_playback_range_engagement
        ON dbo.lesson_playback_ranges(engagement_id, start_seconds);

IF OBJECT_ID(N'dbo.lesson_playback_ranges', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = N'fk_lesson_playback_range_engagement'
         AND parent_object_id = OBJECT_ID(N'dbo.lesson_playback_ranges')
   )
    ALTER TABLE dbo.lesson_playback_ranges
        ADD CONSTRAINT fk_lesson_playback_range_engagement
        FOREIGN KEY (engagement_id)
        REFERENCES dbo.lesson_engagement(id);
GO
