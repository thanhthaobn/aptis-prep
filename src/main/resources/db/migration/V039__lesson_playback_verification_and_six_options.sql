-- Preserve progress/history. New listening claims require server-verified playback.
ALTER TABLE lesson_engagement ADD
    playback_token_hash VARCHAR(64) NULL,
    playback_audio_path NVARCHAR(500) NULL,
    playback_duration_seconds FLOAT NULL,
    playback_position_seconds FLOAT NULL,
    playback_last_ping_at DATETIME2 NULL,
    playback_expires_at DATETIME2 NULL,
    playback_ranges_json NVARCHAR(MAX) NULL,
    playback_sequence INT NOT NULL CONSTRAINT df_lesson_playback_sequence DEFAULT 0,
    playback_running BIT NOT NULL CONSTRAINT df_lesson_playback_running DEFAULT 0,
    verified_audio_path NVARCHAR(500) NULL;
GO
-- V011 used an unnamed CHECK, so discover its generated name instead of guessing it.
DECLARE @drop_checks NVARCHAR(MAX) = N'';
SELECT @drop_checks = @drop_checks + N'ALTER TABLE dbo.lesson_checks DROP CONSTRAINT '
    + QUOTENAME(name) + N';'
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.lesson_checks')
  AND definition LIKE N'%correct_option%';
IF LEN(@drop_checks) > 0 EXEC sp_executesql @drop_checks;
ALTER TABLE lesson_checks ADD CONSTRAINT ck_lesson_checks_correct_option
    CHECK (correct_option BETWEEN 0 AND 5);
GO
