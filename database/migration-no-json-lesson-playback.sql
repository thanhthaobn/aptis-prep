IF COL_LENGTH('dbo.lesson_engagement','playback_ranges_json') IS NOT NULL
BEGIN
    IF OBJECT_ID('dbo.lesson_playback_ranges','U') IS NULL
    BEGIN
        CREATE TABLE dbo.lesson_playback_ranges (
            id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
            engagement_id int NOT NULL,
            start_seconds float NOT NULL,
            end_seconds float NOT NULL,
            CONSTRAINT fk_lesson_playback_range_engagement
                FOREIGN KEY (engagement_id) REFERENCES dbo.lesson_engagement(id)
        );
        CREATE INDEX ix_lesson_playback_range_engagement
            ON dbo.lesson_playback_ranges(engagement_id, start_seconds);
    END
END
GO
