-- Owner 1 persistence. V001-V005 are immutable; this migration is additive.
IF COL_LENGTH('user_skill_profiles', 'checkpoint_floor_level') IS NULL
BEGIN
    ALTER TABLE user_skill_profiles ADD checkpoint_floor_level VARCHAR(10) NULL, checkpoint_source_attempt_id INT NULL;
    ALTER TABLE user_skill_profiles ADD CONSTRAINT fk_skill_profiles_checkpoint_attempt
        FOREIGN KEY (checkpoint_source_attempt_id) REFERENCES exam_attempts(id);
END;
GO

IF COL_LENGTH('user_skill_profiles', 'checkpoint_last_attempt_id') IS NULL
BEGIN
    ALTER TABLE user_skill_profiles ADD checkpoint_last_attempt_id INT NULL;
    ALTER TABLE user_skill_profiles ADD CONSTRAINT fk_skill_profiles_checkpoint_last_attempt
        FOREIGN KEY (checkpoint_last_attempt_id) REFERENCES exam_attempts(id);
END;
GO

IF COL_LENGTH('user_skill_profiles', 'checkpoint_last_submitted_at') IS NULL
BEGIN
    ALTER TABLE user_skill_profiles ADD checkpoint_last_submitted_at DATETIME2 NULL;
END;
GO

IF COL_LENGTH('user_skill_profiles', 'placement_level') IS NULL
BEGIN
    ALTER TABLE user_skill_profiles ADD placement_level VARCHAR(10) NULL;
END;
GO

IF COL_LENGTH('user_skill_profiles', 'placement_submitted_at') IS NULL
BEGIN
    ALTER TABLE user_skill_profiles ADD placement_submitted_at DATETIME2 NULL;
END;
GO

IF OBJECT_ID('user_profile_applications', 'U') IS NULL
BEGIN
    CREATE TABLE user_profile_applications (
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        attempt_id INT NOT NULL,
        skill VARCHAR(30) NOT NULL,
        applied_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT uq_profile_application_attempt_skill UNIQUE (user_id, attempt_id, skill),
        CONSTRAINT fk_profile_application_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_profile_application_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id)
    );
END;
GO

IF OBJECT_ID('ai_assessment_jobs', 'U') IS NULL
BEGIN
    CREATE TABLE ai_assessment_jobs (
        id INT IDENTITY(1,1) PRIMARY KEY,
        assessment_key VARCHAR(255) NOT NULL,
        user_id INT NOT NULL,
        attempt_id INT NOT NULL,
        answer_id INT NOT NULL,
        answer_version INT NOT NULL,
        skill VARCHAR(30) NOT NULL,
        task_type VARCHAR(30) NOT NULL,
        task_content NVARCHAR(MAX) NOT NULL,
        locked_content NVARCHAR(MAX) NOT NULL,
        rubric_version VARCHAR(100) NOT NULL,
        policy_version VARCHAR(100) NOT NULL,
        max_score DECIMAL(5,2) NOT NULL,
        transcript NVARCHAR(MAX) NULL,
        state VARCHAR(30) NOT NULL,
        retry_count INT NOT NULL DEFAULT 0,
        last_error NVARCHAR(1000) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        next_retry_at DATETIME2 NULL,
        CONSTRAINT uq_ai_job_assessment_key UNIQUE (assessment_key),
        CONSTRAINT fk_ai_job_user FOREIGN KEY (user_id) REFERENCES users(id)
    );
END;
GO

IF OBJECT_ID('ai_assessments', 'U') IS NULL
BEGIN
    CREATE TABLE ai_assessments (
        id INT IDENTITY(1,1) PRIMARY KEY,
        job_id INT NOT NULL,
        assessment_key VARCHAR(255) NOT NULL,
        answer_id INT NOT NULL,
        answer_version INT NOT NULL,
        state VARCHAR(30) NOT NULL,
        score DECIMAL(5,2) NULL,
        estimated_cefr VARCHAR(10) NULL,
        feedback NVARCHAR(MAX) NULL,
        transcript NVARCHAR(MAX) NULL,
        rubric_version VARCHAR(100) NOT NULL,
        policy_version VARCHAR(100) NOT NULL,
        provider VARCHAR(50) NULL,
        model VARCHAR(100) NULL,
        error_message NVARCHAR(1000) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        completed_at DATETIME2 NULL,
        CONSTRAINT uq_ai_assessment_job_version UNIQUE (job_id, answer_version),
        CONSTRAINT fk_ai_assessment_job FOREIGN KEY (job_id) REFERENCES ai_assessment_jobs(id),
        CONSTRAINT ck_ai_assessment_score CHECK (score IS NULL OR (score >= 0 AND score <= 50))
    );
END;
