IF COL_LENGTH('lessons', 'image_path') IS NULL
BEGIN
    ALTER TABLE lessons ADD image_path VARCHAR(500) NULL, image_name NVARCHAR(255) NULL;
END;
GO

IF COL_LENGTH('lesson_responses', 'response_version') IS NULL
BEGIN
    ALTER TABLE lesson_responses ADD response_version INT NOT NULL CONSTRAINT df_lesson_response_version DEFAULT 1,
        assessment_key VARCHAR(255) NULL;
END;
GO
