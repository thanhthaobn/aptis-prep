IF COL_LENGTH('user_flashcard_progress', 'next_review_at') IS NULL
    ALTER TABLE user_flashcard_progress ADD next_review_at DATETIME2 NULL;
IF COL_LENGTH('user_flashcard_progress', 'interval_days') IS NULL
    ALTER TABLE user_flashcard_progress ADD interval_days INT NOT NULL CONSTRAINT df_flashcard_progress_interval DEFAULT 0;
IF COL_LENGTH('user_flashcard_progress', 'ease_factor') IS NULL
    ALTER TABLE user_flashcard_progress ADD ease_factor DECIMAL(4,2) NOT NULL CONSTRAINT df_flashcard_progress_ease DEFAULT 2.50;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_flashcard_progress_interval')
    ALTER TABLE user_flashcard_progress ADD CONSTRAINT ck_flashcard_progress_interval CHECK (interval_days >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_flashcard_progress_ease')
    ALTER TABLE user_flashcard_progress ADD CONSTRAINT ck_flashcard_progress_ease CHECK (ease_factor >= 1.30 AND ease_factor <= 3.00);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_flashcard_progress_due')
    CREATE INDEX ix_flashcard_progress_due ON user_flashcard_progress(user_id, next_review_at);
