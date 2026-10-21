IF COL_LENGTH('exam_attempts', 'request_key') IS NULL
BEGIN
    ALTER TABLE exam_attempts ADD request_key VARCHAR(255) NULL;
END;
GO

IF COL_LENGTH('user_answers', 'answer_version') IS NULL
BEGIN
    ALTER TABLE user_answers ADD answer_version INT NOT NULL CONSTRAINT df_user_answers_answer_version DEFAULT 1;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_exam_attempts_user_request_key')
BEGIN
    CREATE UNIQUE INDEX uq_exam_attempts_user_request_key
        ON exam_attempts(user_id, request_key)
        WHERE request_key IS NOT NULL;
END;
GO

IF OBJECT_ID('exam_attempt_snapshots', 'U') IS NULL
BEGIN
    CREATE TABLE exam_attempt_snapshots (
        id INT IDENTITY(1,1) PRIMARY KEY,
        exam_attempt_id INT NOT NULL,
        mock_exam_id INT NOT NULL,
        certificate_id INT NOT NULL,
        scoring_scheme_id INT NULL,
        exam_type VARCHAR(30) NOT NULL,
        skill VARCHAR(30) NULL,
        target_level VARCHAR(10) NULL,
        title NVARCHAR(255) NOT NULL,
        duration_minutes INT NULL,
        answer_version INT NOT NULL,
        policy_version VARCHAR(100) NOT NULL,
        grading_context NVARCHAR(MAX) NOT NULL,
        locked_question_snapshot NVARCHAR(MAX) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT uq_exam_attempt_snapshot_attempt UNIQUE (exam_attempt_id),
        CONSTRAINT fk_exam_attempt_snapshot_attempt FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id)
    );
END;
GO

IF OBJECT_ID('attempt_outbox', 'U') IS NULL
BEGIN
    CREATE TABLE attempt_outbox (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        event_key VARCHAR(255) NOT NULL,
        event_type VARCHAR(80) NOT NULL,
        aggregate_id INT NOT NULL,
        payload NVARCHAR(MAX) NOT NULL,
        status VARCHAR(30) NOT NULL,
        attempts INT NOT NULL DEFAULT 0,
        next_attempt_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        last_error NVARCHAR(1000) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        processed_at DATETIME2 NULL,
        CONSTRAINT uq_attempt_outbox_event_key UNIQUE (event_key)
    );
END;
GO

IF OBJECT_ID('user_media_activities', 'U') IS NULL
BEGIN
    CREATE TABLE user_media_activities (
        id INT IDENTITY(1,1) PRIMARY KEY,
        exam_attempt_id INT NOT NULL,
        question_media_id INT NOT NULL,
        play_count INT NOT NULL DEFAULT 0,
        last_played_at DATETIME2 NULL,
        CONSTRAINT uq_media_activities_attempt_media UNIQUE (exam_attempt_id, question_media_id),
        CONSTRAINT fk_media_activities_attempt FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id),
        CONSTRAINT fk_media_activities_media FOREIGN KEY (question_media_id) REFERENCES question_media(id),
        CONSTRAINT ck_media_activities_play_count CHECK (play_count >= 0 AND play_count <= 2)
    );
END;
GO

IF COL_LENGTH('user_media_activities', 'play_token_hash') IS NULL
BEGIN
    ALTER TABLE user_media_activities ADD play_token_hash VARCHAR(64) NULL;
END;
GO

IF COL_LENGTH('user_media_activities', 'play_token_expires_at') IS NULL
BEGIN
    ALTER TABLE user_media_activities ADD play_token_expires_at DATETIME2 NULL;
END;
GO
