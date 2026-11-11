/*
   Additive upgrade for an existing AptisPrep database created through V031.
   Back up the database and rehearse this script on a restored copy first.
   Do not run database/schema-current.sql on a database that already has data.
   This script is intentionally not loaded by Spring Boot.
*/

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH(N'dbo.users', N'lock_reason') IS NULL
    ALTER TABLE dbo.users ADD lock_reason NVARCHAR(1000) NULL;

IF COL_LENGTH(N'dbo.user_flashcard_progress', N'next_review_at') IS NULL
    ALTER TABLE dbo.user_flashcard_progress ADD next_review_at DATETIME2 NULL;
IF COL_LENGTH(N'dbo.user_flashcard_progress', N'interval_days') IS NULL
    ALTER TABLE dbo.user_flashcard_progress ADD interval_days INT NOT NULL
        CONSTRAINT df_flashcard_progress_interval_upgrade DEFAULT 0 WITH VALUES;
IF COL_LENGTH(N'dbo.user_flashcard_progress', N'ease_factor') IS NULL
    ALTER TABLE dbo.user_flashcard_progress ADD ease_factor DECIMAL(4, 2) NOT NULL
        CONSTRAINT df_flashcard_progress_ease_upgrade DEFAULT 2.50 WITH VALUES;

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'ck_flashcard_progress_interval'
)
    ALTER TABLE dbo.user_flashcard_progress ADD CONSTRAINT ck_flashcard_progress_interval
        CHECK (interval_days >= 0);
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'ck_flashcard_progress_ease'
)
    ALTER TABLE dbo.user_flashcard_progress ADD CONSTRAINT ck_flashcard_progress_ease
        CHECK (ease_factor >= 1.30 AND ease_factor <= 3.00);
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'ix_flashcard_progress_due'
      AND object_id = OBJECT_ID(N'dbo.user_flashcard_progress')
)
    CREATE INDEX ix_flashcard_progress_due
        ON dbo.user_flashcard_progress(user_id, next_review_at);

IF OBJECT_ID(N'dbo.scoring_scale_rules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.scoring_scale_rules (
        id INT IDENTITY(1, 1) NOT NULL PRIMARY KEY,
        scoring_scheme_id INT NOT NULL,
        skill VARCHAR(30) NOT NULL,
        raw_min DECIMAL(6, 2) NOT NULL,
        raw_max DECIMAL(6, 2) NOT NULL,
        scale_min DECIMAL(5, 2) NOT NULL,
        scale_max DECIMAL(5, 2) NOT NULL,
        CONSTRAINT fk_scoring_scale_rules_scheme_upgrade
            FOREIGN KEY (scoring_scheme_id) REFERENCES dbo.scoring_schemes(id),
        CONSTRAINT uq_scoring_scale_rules_scheme_skill_upgrade
            UNIQUE (scoring_scheme_id, skill),
        CONSTRAINT ck_scoring_scale_rules_range_upgrade
            CHECK (raw_min < raw_max AND scale_min <= scale_max)
    );
END;

IF COL_LENGTH(N'dbo.ai_assessment_jobs', N'source_type') IS NULL
    ALTER TABLE dbo.ai_assessment_jobs ADD source_type VARCHAR(30) NOT NULL
        CONSTRAINT df_ai_job_source_upgrade DEFAULT 'EXAM_ANSWER' WITH VALUES;
IF COL_LENGTH(N'dbo.ai_assessment_jobs', N'source_id') IS NULL
    ALTER TABLE dbo.ai_assessment_jobs ADD source_id INT NULL;
IF COL_LENGTH(N'dbo.ai_assessment_jobs', N'processing_token') IS NULL
    ALTER TABLE dbo.ai_assessment_jobs ADD processing_token VARCHAR(64) NULL;
IF COL_LENGTH(N'dbo.user_answers', N'audio_manifest') IS NULL
    ALTER TABLE dbo.user_answers ADD audio_manifest NVARCHAR(MAX) NULL;

UPDATE jobs
SET source_id = jobs.answer_id
FROM dbo.ai_assessment_jobs jobs
WHERE jobs.source_id IS NULL;

ALTER TABLE dbo.ai_assessment_jobs ALTER COLUMN attempt_id INT NULL;
ALTER TABLE dbo.ai_assessment_jobs ALTER COLUMN answer_id INT NULL;
ALTER TABLE dbo.ai_assessments ALTER COLUMN answer_id INT NULL;

IF OBJECT_ID(N'dbo.flashcard_reviews', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.flashcard_reviews (
        id VARCHAR(160) NOT NULL PRIMARY KEY,
        user_id INT NOT NULL,
        flashcard_id INT NOT NULL,
        state VARCHAR(30) NOT NULL,
        reviewed_at DATETIME2 NOT NULL,
        CONSTRAINT fk_flashcard_reviews_user_upgrade
            FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT fk_flashcard_reviews_flashcard_upgrade
            FOREIGN KEY (flashcard_id) REFERENCES dbo.flashcards(id)
    );
END;

IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_token_hash') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_token_hash VARCHAR(64) NULL;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_audio_path') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_audio_path NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_duration_seconds') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_duration_seconds FLOAT NULL;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_position_seconds') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_position_seconds FLOAT NULL;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_last_ping_at') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_last_ping_at DATETIME2 NULL;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_expires_at') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_expires_at DATETIME2 NULL;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_ranges_json') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_ranges_json NVARCHAR(MAX) NULL;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_sequence') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_sequence INT NOT NULL
        CONSTRAINT df_lesson_playback_sequence_upgrade DEFAULT 0 WITH VALUES;
IF COL_LENGTH(N'dbo.lesson_engagement', N'playback_running') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD playback_running BIT NOT NULL
        CONSTRAINT df_lesson_playback_running_upgrade DEFAULT 0 WITH VALUES;
IF COL_LENGTH(N'dbo.lesson_engagement', N'verified_audio_path') IS NULL
    ALTER TABLE dbo.lesson_engagement ADD verified_audio_path NVARCHAR(500) NULL;

IF COL_LENGTH(N'dbo.mock_exams', N'demo_only') IS NULL
    ALTER TABLE dbo.mock_exams ADD demo_only BIT NOT NULL
        CONSTRAINT df_mock_exams_demo_only_upgrade DEFAULT 0 WITH VALUES;

COMMIT TRANSACTION;
