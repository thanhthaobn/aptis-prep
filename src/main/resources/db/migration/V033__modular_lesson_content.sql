ALTER TABLE lessons ADD content_blocks_json NVARCHAR(MAX) NULL;
ALTER TABLE lesson_checks ADD block_key VARCHAR(80) NULL;
ALTER TABLE lesson_check_attempts ADD content_snapshot NVARCHAR(MAX) NULL;
ALTER TABLE lesson_work_submissions ADD content_snapshot NVARCHAR(MAX) NULL;
CREATE TABLE lesson_block_listens (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id),
    lesson_id INT NOT NULL REFERENCES lessons(id),
    block_key VARCHAR(80) NOT NULL,
    media_path VARCHAR(500) NOT NULL,
    played_at DATETIME2 NOT NULL,
    CONSTRAINT uq_lesson_block_listens UNIQUE(user_id,lesson_id,block_key)
);
