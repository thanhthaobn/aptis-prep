/* ARCHIVE ONLY. Do not execute: this legacy script changes an existing scoring policy.
   Use database/complete-grammar-scoring.sql to create a separate version while retaining history.
   This file is excluded from application resources and is not a migration. */
DECLARE @scheme_id INT = (
    SELECT id
    FROM scoring_schemes
    WHERE certificate_id = (SELECT id FROM certificates WHERE code = 'APTIS_GENERAL')
      AND version = '1.0'
);

IF @scheme_id IS NOT NULL
BEGIN
    INSERT INTO score_bands (scoring_scheme_id, skill, cefr_level, min_score, max_score)
    SELECT @scheme_id, value.skill, value.cefr_level, value.min_score, value.max_score
    FROM (VALUES
        ('GRAMMAR_VOCABULARY', 'A1', CAST(0.00 AS DECIMAL(5,2)), CAST(9.99 AS DECIMAL(5,2))),
        ('GRAMMAR_VOCABULARY', 'A2', CAST(10.00 AS DECIMAL(5,2)), CAST(19.99 AS DECIMAL(5,2))),
        ('GRAMMAR_VOCABULARY', 'B1', CAST(20.00 AS DECIMAL(5,2)), CAST(29.99 AS DECIMAL(5,2))),
        ('GRAMMAR_VOCABULARY', 'B2', CAST(30.00 AS DECIMAL(5,2)), CAST(39.99 AS DECIMAL(5,2))),
        ('GRAMMAR_VOCABULARY', 'C1', CAST(40.00 AS DECIMAL(5,2)), CAST(45.99 AS DECIMAL(5,2))),
        ('GRAMMAR_VOCABULARY', 'C2', CAST(46.00 AS DECIMAL(5,2)), CAST(50.00 AS DECIMAL(5,2)))
    ) value(skill, cefr_level, min_score, max_score)
    WHERE NOT EXISTS (
        SELECT 1
        FROM score_bands existing
        WHERE existing.scoring_scheme_id = @scheme_id
          AND existing.skill = value.skill
          AND existing.cefr_level = value.cefr_level
    );
END;
