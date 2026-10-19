CREATE TABLE lesson_check_attempts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    lesson_id INT NOT NULL,
    score INT NOT NULL,
    total INT NOT NULL,
    answers_json NVARCHAR(MAX) NOT NULL,
    submitted_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_lca_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_lca_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    CONSTRAINT ck_lca_score CHECK (score >= 0 AND total > 0 AND score <= total)
);
CREATE INDEX ix_lca_user_lesson_time ON lesson_check_attempts(user_id, lesson_id, submitted_at DESC);

CREATE TABLE lesson_engagement (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    lesson_id INT NOT NULL,
    audio_played_at DATETIME2 NULL,
    last_section VARCHAR(40) NULL,
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_le_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_le_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    CONSTRAINT uq_le_user_lesson UNIQUE(user_id, lesson_id)
);

ALTER TABLE lesson_responses ADD audio_path nvarchar(500) NULL;
ALTER TABLE lesson_responses ADD audio_name nvarchar(255) NULL;
ALTER TABLE lessons ADD transcript nvarchar(max) NULL;
ALTER TABLE lessons ADD min_word_count int NULL;
ALTER TABLE lessons ADD max_word_count int NULL;
