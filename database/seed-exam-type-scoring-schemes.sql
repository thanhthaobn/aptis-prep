/*
 * Seed one active Aptis scoring scheme per exam type.
 * The script is safe to run repeatedly and keeps the existing untyped scheme
 * as a legacy record while moving exams to type-specific schemes.
 */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @certificate_id INT = (
    SELECT TOP (1) id
    FROM certificates
    WHERE code = 'APTIS_GENERAL'
    ORDER BY id
);

IF @certificate_id IS NULL
    THROW 51000, 'APTIS_GENERAL certificate was not found.', 1;

DECLARE @schemes TABLE (
    exam_type VARCHAR(30) NOT NULL,
    scheme_name NVARCHAR(255) NOT NULL,
    scheme_version VARCHAR(30) NOT NULL
);

INSERT INTO @schemes (exam_type, scheme_name, scheme_version)
VALUES
    ('PRACTICE', N'Estimated Aptis Scoring - Practice', '1.0-PRACTICE'),
    ('FULL_MOCK', N'Estimated Aptis Scoring - Full mock', '1.0-FULL-MOCK'),
    ('PLACEMENT', N'Estimated Aptis Scoring - Placement', '1.0-PLACEMENT'),
    ('CHECKPOINT', N'Estimated Aptis Scoring - Checkpoint', '1.0-CHECKPOINT');

DECLARE @exam_type VARCHAR(30);
DECLARE @scheme_name NVARCHAR(255);
DECLARE @scheme_version VARCHAR(30);
DECLARE @scheme_id INT;

DECLARE scheme_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT exam_type, scheme_name, scheme_version FROM @schemes;

OPEN scheme_cursor;
FETCH NEXT FROM scheme_cursor INTO @exam_type, @scheme_name, @scheme_version;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @scheme_id = NULL;
    SELECT @scheme_id = id
    FROM scoring_schemes
    WHERE certificate_id = @certificate_id
      AND version = @scheme_version;

    IF @scheme_id IS NULL
    BEGIN
        INSERT INTO scoring_schemes (
            certificate_id, name, version, status, effective_from, created_at, exam_type, scope_skill
        )
        VALUES (
            @certificate_id, @scheme_name, @scheme_version, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), @exam_type, NULL
        );
        SET @scheme_id = CONVERT(INT, SCOPE_IDENTITY());
    END
    ELSE
    BEGIN
        UPDATE scoring_schemes
        SET name = @scheme_name,
            status = 'ACTIVE',
            exam_type = @exam_type,
            scope_skill = NULL
        WHERE id = @scheme_id;
    END;

    INSERT INTO scoring_scale_rules (scoring_scheme_id, skill, raw_min, raw_max, scale_min, scale_max)
    SELECT @scheme_id, values_table.skill, 0, 50, 0, 50
    FROM (VALUES
        ('LISTENING'), ('READING'), ('SPEAKING'), ('WRITING'), ('GRAMMAR_VOCABULARY')
    ) AS values_table(skill)
    WHERE NOT EXISTS (
        SELECT 1
        FROM scoring_scale_rules existing
        WHERE existing.scoring_scheme_id = @scheme_id
          AND existing.skill = values_table.skill
    );

    INSERT INTO score_bands (scoring_scheme_id, skill, cefr_level, min_score, max_score)
    SELECT @scheme_id, values_table.skill, values_table.cefr_level, values_table.min_score, values_table.max_score
    FROM (VALUES
        ('LISTENING', 'A1', CAST(0 AS DECIMAL(10, 2)), CAST(9.99 AS DECIMAL(10, 2))),
        ('LISTENING', 'A2', 10, 19.99), ('LISTENING', 'B1', 20, 29.99),
        ('LISTENING', 'B2', 30, 39.99), ('LISTENING', 'C1', 40, 45.99), ('LISTENING', 'C2', 46, 50),
        ('READING', 'A1', 0, 9.99), ('READING', 'A2', 10, 19.99), ('READING', 'B1', 20, 29.99),
        ('READING', 'B2', 30, 39.99), ('READING', 'C1', 40, 45.99), ('READING', 'C2', 46, 50),
        ('SPEAKING', 'A1', 0, 9.99), ('SPEAKING', 'A2', 10, 19.99), ('SPEAKING', 'B1', 20, 29.99),
        ('SPEAKING', 'B2', 30, 39.99), ('SPEAKING', 'C1', 40, 45.99), ('SPEAKING', 'C2', 46, 50),
        ('WRITING', 'A1', 0, 9.99), ('WRITING', 'A2', 10, 19.99), ('WRITING', 'B1', 20, 29.99),
        ('WRITING', 'B2', 30, 39.99), ('WRITING', 'C1', 40, 45.99), ('WRITING', 'C2', 46, 50),
        ('GRAMMAR_VOCABULARY', 'A1', 0, 9.99), ('GRAMMAR_VOCABULARY', 'A2', 10, 19.99),
        ('GRAMMAR_VOCABULARY', 'B1', 20, 29.99), ('GRAMMAR_VOCABULARY', 'B2', 30, 39.99),
        ('GRAMMAR_VOCABULARY', 'C1', 40, 45.99), ('GRAMMAR_VOCABULARY', 'C2', 46, 50)
    ) AS values_table(skill, cefr_level, min_score, max_score)
    WHERE NOT EXISTS (
        SELECT 1
        FROM score_bands existing
        WHERE existing.scoring_scheme_id = @scheme_id
          AND existing.skill = values_table.skill
          AND existing.cefr_level = values_table.cefr_level
    );

    UPDATE mock_exams
    SET scoring_scheme_id = @scheme_id,
        updated_at = SYSUTCDATETIME()
    WHERE certificate_id = @certificate_id
      AND exam_type = @exam_type;

    FETCH NEXT FROM scheme_cursor INTO @exam_type, @scheme_name, @scheme_version;
END;

CLOSE scheme_cursor;
DEALLOCATE scheme_cursor;
COMMIT TRANSACTION;
