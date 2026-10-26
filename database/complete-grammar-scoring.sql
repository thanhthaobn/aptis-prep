/*
  Explicit repair for the missing Grammar & Vocabulary bands in APTIS_GENERAL 1.0.
  Back up and rehearse on a restored database before running on an existing DB.
  Not a startup migration. Does not change schema, old policy, attempts or results.
  Internal A1-C1 estimate, not an official Aptis conversion table.
  Used exams remain unchanged; a new exam version is created for future attempts.
  Legacy Checkpoint C2 is retained and is outside the A1-C1 curriculum.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lock_result INT;
    EXEC @lock_result = sys.sp_getapplock
        @Resource = N'aptisprep-complete-grammar-scoring-1.1-gv-c1',
        @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 15000;
    IF @lock_result < 0
        THROW 51000, 'Cannot acquire the Grammar scoring repair lock.', 1;

    DECLARE @source_id INT, @certificate_id INT, @replacement_id INT;
    DECLARE @version VARCHAR(30) = '1.1-gv-c1';
    SELECT @source_id = s.id, @certificate_id = s.certificate_id
    FROM dbo.scoring_schemes s WITH (UPDLOCK, HOLDLOCK)
    JOIN dbo.certificates c ON c.id = s.certificate_id
    WHERE c.code = 'APTIS_GENERAL' AND c.status = 'ACTIVE'
      AND s.version = '1.0' AND s.status = 'ACTIVE';
    IF @source_id IS NULL
        THROW 51001, 'The active APTIS_GENERAL 1.0 source policy is required.', 1;
    IF EXISTS (SELECT 1 FROM dbo.score_bands WHERE scoring_scheme_id = @source_id AND skill = 'GRAMMAR_VOCABULARY')
        THROW 51002, 'Source Grammar bands already exist; review instead of overwriting policy.', 1;
    IF (SELECT COUNT(*) FROM dbo.scoring_scale_rules WHERE scoring_scheme_id = @source_id) <> 5
       OR NOT EXISTS (
           SELECT 1 FROM dbo.scoring_scale_rules
           WHERE scoring_scheme_id = @source_id AND skill = 'GRAMMAR_VOCABULARY'
             AND raw_min = 0 AND raw_max = 50 AND scale_min = 0 AND scale_max = 50
       )
        THROW 51003, 'Expected five skill rules and the internal Grammar 0-50 rule.', 1;

    DECLARE @grammar TABLE (cefr_level VARCHAR(10), min_score DECIMAL(5,2), max_score DECIMAL(5,2));
    INSERT INTO @grammar VALUES
        ('A1', 0.00, 9.99), ('A2', 10.00, 19.99), ('B1', 20.00, 29.99),
        ('B2', 30.00, 39.99), ('C1', 40.00, 50.00);

    SELECT @replacement_id = id FROM dbo.scoring_schemes WITH (UPDLOCK, HOLDLOCK)
    WHERE certificate_id = @certificate_id AND version = @version;
    IF @replacement_id IS NULL
    BEGIN
        INSERT INTO dbo.scoring_schemes (certificate_id, name, version, status, effective_from, created_at)
        SELECT certificate_id, N'Estimated Aptis Scoring - Grammar A1-C1', @version, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME()
        FROM dbo.scoring_schemes WHERE id = @source_id;
        SET @replacement_id = SCOPE_IDENTITY();
        INSERT INTO dbo.scoring_scale_rules (scoring_scheme_id, skill, raw_min, raw_max, scale_min, scale_max)
        SELECT @replacement_id, skill, raw_min, raw_max, scale_min, scale_max
        FROM dbo.scoring_scale_rules WHERE scoring_scheme_id = @source_id;
        INSERT INTO dbo.score_bands (scoring_scheme_id, skill, cefr_level, min_score, max_score)
        SELECT @replacement_id, skill, cefr_level, min_score, max_score
        FROM dbo.score_bands WHERE scoring_scheme_id = @source_id;
        INSERT INTO dbo.score_bands (scoring_scheme_id, skill, cefr_level, min_score, max_score)
        SELECT @replacement_id, 'GRAMMAR_VOCABULARY', cefr_level, min_score, max_score FROM @grammar;
    END;

    -- A rerun verifies the existing replacement; it never edits a used version.
    IF NOT EXISTS (SELECT 1 FROM dbo.scoring_schemes WHERE id = @replacement_id AND status = 'ACTIVE')
        THROW 51004, 'The replacement policy is not active.', 1;
    IF EXISTS (
        SELECT skill, raw_min, raw_max, scale_min, scale_max FROM dbo.scoring_scale_rules WHERE scoring_scheme_id = @source_id
        EXCEPT
        SELECT skill, raw_min, raw_max, scale_min, scale_max FROM dbo.scoring_scale_rules WHERE scoring_scheme_id = @replacement_id
    ) OR EXISTS (
        SELECT skill, raw_min, raw_max, scale_min, scale_max FROM dbo.scoring_scale_rules WHERE scoring_scheme_id = @replacement_id
        EXCEPT
        SELECT skill, raw_min, raw_max, scale_min, scale_max FROM dbo.scoring_scale_rules WHERE scoring_scheme_id = @source_id
    )
        THROW 51005, 'Replacement raw-scale rules differ from the source.', 1;
    IF EXISTS (
        SELECT skill, cefr_level, min_score, max_score FROM dbo.score_bands WHERE scoring_scheme_id = @source_id
        UNION ALL SELECT 'GRAMMAR_VOCABULARY', cefr_level, min_score, max_score FROM @grammar
        EXCEPT SELECT skill, cefr_level, min_score, max_score FROM dbo.score_bands WHERE scoring_scheme_id = @replacement_id
    ) OR EXISTS (
        SELECT skill, cefr_level, min_score, max_score FROM dbo.score_bands WHERE scoring_scheme_id = @replacement_id
        EXCEPT (
            SELECT skill, cefr_level, min_score, max_score FROM dbo.score_bands WHERE scoring_scheme_id = @source_id
            UNION ALL SELECT 'GRAMMAR_VOCABULARY', cefr_level, min_score, max_score FROM @grammar
        )
    )
        THROW 51006, 'Replacement CEFR bands differ from the approved internal policy.', 1;
    IF EXISTS (
        SELECT 1 FROM dbo.scoring_scale_rules r
        OUTER APPLY (
            SELECT MIN(min_score) minimum, MAX(max_score) maximum, COUNT(*) band_count
            FROM dbo.score_bands b WHERE b.scoring_scheme_id = r.scoring_scheme_id AND b.skill = r.skill
        ) b
        WHERE r.scoring_scheme_id = @replacement_id
          AND (r.raw_min >= r.raw_max OR r.scale_min >= r.scale_max OR b.band_count = 0
               OR b.minimum <> r.scale_min OR b.maximum <> r.scale_max)
    ) OR EXISTS (
        SELECT 1 FROM (
            SELECT b.*, LAG(max_score) OVER (PARTITION BY skill ORDER BY min_score) previous_end
            FROM dbo.score_bands b WHERE scoring_scheme_id = @replacement_id
        ) b WHERE min_score > max_score OR (previous_end IS NOT NULL AND (min_score <= previous_end OR min_score > previous_end + 0.01))
    )
        THROW 51007, 'Every replacement skill must have complete, nonoverlapping score coverage.', 1;

    DECLARE @candidates TABLE (row_no INT IDENTITY, exam_id INT);
    INSERT INTO @candidates (exam_id)
    SELECT e.id FROM dbo.mock_exams e WITH (UPDLOCK, HOLDLOCK)
    WHERE e.scoring_scheme_id = @source_id AND e.status = 'PUBLISHED' AND e.demo_only = 0
      AND NOT (e.exam_type = 'CHECKPOINT' AND e.target_level = 'C2')
      AND EXISTS (
          SELECT 1 FROM dbo.mock_exam_questions eq JOIN dbo.questions q ON q.id = eq.question_id
          WHERE eq.mock_exam_id = e.id AND q.skill = 'GRAMMAR_VOCABULARY'
      );
    DECLARE @changes TABLE (source_exam_id INT, future_exam_id INT, action VARCHAR(20));
    DECLARE @row INT = 1, @exam_id INT, @future_id INT, @title NVARCHAR(255);
    DECLARE @section_map TABLE (old_id INT PRIMARY KEY, new_id INT);
    WHILE @row <= (SELECT COUNT(*) FROM @candidates)
    BEGIN
        SELECT @exam_id = exam_id FROM @candidates WHERE row_no = @row;
        IF NOT EXISTS (SELECT 1 FROM dbo.exam_attempts WITH (UPDLOCK, HOLDLOCK) WHERE mock_exam_id = @exam_id)
        BEGIN
            UPDATE dbo.mock_exams SET scoring_scheme_id = @replacement_id WHERE id = @exam_id;
            INSERT INTO @changes VALUES (@exam_id, @exam_id, 'REASSIGNED');
        END
        ELSE
        BEGIN
            SELECT @title = LEFT(title, 220) + N' [GV1.1 #' + CONVERT(NVARCHAR(12), id) + N']'
            FROM dbo.mock_exams WHERE id = @exam_id;
            SET @future_id = NULL;
            SELECT @future_id = id FROM dbo.mock_exams
            WHERE scoring_scheme_id = @replacement_id AND title = @title;
            IF @future_id IS NULL
            BEGIN
                INSERT INTO dbo.mock_exams (certificate_id, scoring_scheme_id, title, description, exam_type, target_level, status, created_at, updated_at, skill, duration_minutes, demo_only)
                SELECT certificate_id, @replacement_id, @title, description, exam_type, target_level, status, SYSUTCDATETIME(), SYSUTCDATETIME(), skill, duration_minutes, demo_only
                FROM dbo.mock_exams WHERE id = @exam_id;
                SET @future_id = SCOPE_IDENTITY();
                DELETE FROM @section_map;
                DECLARE @old_section INT, @new_section INT;
                DECLARE sections_to_copy CURSOR LOCAL FAST_FORWARD FOR
                    SELECT id FROM dbo.mock_exam_sections WHERE mock_exam_id = @exam_id ORDER BY display_order;
                OPEN sections_to_copy;
                FETCH NEXT FROM sections_to_copy INTO @old_section;
                WHILE @@FETCH_STATUS = 0
                BEGIN
                    INSERT INTO dbo.mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status)
                    SELECT @future_id, skill, display_order, duration_minutes, status FROM dbo.mock_exam_sections WHERE id = @old_section;
                    SET @new_section = SCOPE_IDENTITY();
                    INSERT INTO @section_map VALUES (@old_section, @new_section);
                    FETCH NEXT FROM sections_to_copy INTO @old_section;
                END;
                CLOSE sections_to_copy;
                DEALLOCATE sections_to_copy;
                INSERT INTO dbo.mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position)
                SELECT @future_id, m.new_id, q.question_id, q.position
                FROM dbo.mock_exam_questions q LEFT JOIN @section_map m ON m.old_id = q.mock_exam_section_id
                WHERE q.mock_exam_id = @exam_id;
                INSERT INTO @changes VALUES (@exam_id, @future_id, 'COPIED');
            END
            ELSE
            BEGIN
                IF EXISTS (
                    SELECT q.question_id, q.position, s.skill, s.display_order, s.duration_minutes, s.status
                    FROM dbo.mock_exam_questions q LEFT JOIN dbo.mock_exam_sections s ON s.id = q.mock_exam_section_id WHERE q.mock_exam_id = @exam_id
                    EXCEPT
                    SELECT q.question_id, q.position, s.skill, s.display_order, s.duration_minutes, s.status
                    FROM dbo.mock_exam_questions q LEFT JOIN dbo.mock_exam_sections s ON s.id = q.mock_exam_section_id WHERE q.mock_exam_id = @future_id
                ) OR EXISTS (
                    SELECT q.question_id, q.position, s.skill, s.display_order, s.duration_minutes, s.status
                    FROM dbo.mock_exam_questions q LEFT JOIN dbo.mock_exam_sections s ON s.id = q.mock_exam_section_id WHERE q.mock_exam_id = @future_id
                    EXCEPT
                    SELECT q.question_id, q.position, s.skill, s.display_order, s.duration_minutes, s.status
                    FROM dbo.mock_exam_questions q LEFT JOIN dbo.mock_exam_sections s ON s.id = q.mock_exam_section_id WHERE q.mock_exam_id = @exam_id
                )
                    THROW 51008, 'An existing replacement exam was edited; review instead of copying again.', 1;
                INSERT INTO @changes VALUES (@exam_id, @future_id, 'REUSED');
            END;
        END;
        SET @row += 1;
    END;
    COMMIT TRANSACTION;
    SELECT @source_id source_scheme_id, @replacement_id replacement_scheme_id, @version replacement_version;
    SELECT source_exam_id, future_exam_id, action FROM @changes ORDER BY source_exam_id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
