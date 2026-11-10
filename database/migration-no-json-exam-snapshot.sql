/* Run after backing up the database. The old JSON columns stay nullable during the transition. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.exam_attempt_snapshots', 'section_advance_millis') IS NULL
BEGIN
    ALTER TABLE dbo.exam_attempt_snapshots
        ADD section_advance_millis bigint NOT NULL
            CONSTRAINT df_exam_snapshot_section_advance DEFAULT 0;
END;

IF OBJECT_ID('dbo.exam_attempt_snapshot_questions', 'U') IS NULL
BEGIN
CREATE TABLE dbo.exam_attempt_snapshot_questions (
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    snapshot_id int NOT NULL,
    source_question_id int NOT NULL,
    position int NOT NULL,
    skill varchar(30) NULL,
    question_type varchar(50) NULL,
    content nvarchar(max) NULL,
    instruction nvarchar(max) NULL,
    reading_passage nvarchar(max) NULL,
    section_start_minute int NULL,
    section_end_minute int NULL,
    CONSTRAINT fk_exam_snapshot_question_snapshot FOREIGN KEY (snapshot_id) REFERENCES dbo.exam_attempt_snapshots(id)
);
CREATE INDEX ix_exam_snapshot_question_snapshot ON dbo.exam_attempt_snapshot_questions(snapshot_id, position);
END;

IF OBJECT_ID('dbo.exam_attempt_snapshot_items', 'U') IS NULL
BEGIN
CREATE TABLE dbo.exam_attempt_snapshot_items (
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    question_snapshot_id int NOT NULL,
    source_item_id int NOT NULL,
    position int NOT NULL,
    content nvarchar(max) NULL,
    correct_position int NULL,
    min_word_count int NULL,
    max_word_count int NULL,
    response_seconds int NULL,
    CONSTRAINT fk_exam_snapshot_item_question FOREIGN KEY (question_snapshot_id) REFERENCES dbo.exam_attempt_snapshot_questions(id)
);
CREATE INDEX ix_exam_snapshot_item_question ON dbo.exam_attempt_snapshot_items(question_snapshot_id, position);
END;

IF OBJECT_ID('dbo.exam_attempt_snapshot_options', 'U') IS NULL
BEGIN
CREATE TABLE dbo.exam_attempt_snapshot_options (
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    item_snapshot_id int NOT NULL,
    source_option_id int NOT NULL,
    option_code varchar(30) NULL,
    position int NOT NULL,
    content nvarchar(max) NULL,
    correct bit NOT NULL,
    CONSTRAINT fk_exam_snapshot_option_item FOREIGN KEY (item_snapshot_id) REFERENCES dbo.exam_attempt_snapshot_items(id)
);
CREATE INDEX ix_exam_snapshot_option_item ON dbo.exam_attempt_snapshot_options(item_snapshot_id, position);
END;

IF OBJECT_ID('dbo.exam_attempt_snapshot_media', 'U') IS NULL
BEGIN
CREATE TABLE dbo.exam_attempt_snapshot_media (
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    question_snapshot_id int NOT NULL,
    source_media_id int NOT NULL,
    position int NOT NULL,
    media_type varchar(30) NULL,
    media_url nvarchar(max) NULL,
    original_filename nvarchar(255) NULL,
    content_type varchar(100) NULL,
    transcript nvarchar(max) NULL,
    max_play_count int NULL,
    CONSTRAINT fk_exam_snapshot_media_question FOREIGN KEY (question_snapshot_id) REFERENCES dbo.exam_attempt_snapshot_questions(id)
);
CREATE INDEX ix_exam_snapshot_media_question ON dbo.exam_attempt_snapshot_media(question_snapshot_id, position);
END;

COMMIT TRANSACTION;
