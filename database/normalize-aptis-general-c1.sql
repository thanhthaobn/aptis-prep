-- Keep the existing C1 threshold and absorb the adjacent legacy C2 range.
-- Stored attempt results and snapshots are outside this repair.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

SELECT high.id AS c2_id, topBand.id AS c1_id, high.max_score AS upper_score,
       high.min_score AS c2_min, topBand.max_score AS c1_max
INTO #aptis_general_c2
FROM dbo.score_bands high WITH (UPDLOCK, HOLDLOCK)
JOIN dbo.scoring_schemes scheme ON scheme.id = high.scoring_scheme_id
JOIN dbo.certificates certificate ON certificate.id = scheme.certificate_id
LEFT JOIN dbo.score_bands topBand WITH (UPDLOCK, HOLDLOCK)
  ON topBand.scoring_scheme_id = high.scoring_scheme_id
 AND topBand.skill = high.skill
 AND topBand.cefr_level = 'C1'
WHERE certificate.code = 'APTIS_GENERAL' AND high.cefr_level = 'C2';

IF EXISTS (
    SELECT 1 FROM #aptis_general_c2
    WHERE c1_id IS NOT NULL AND c1_max + 0.01 <> c2_min
)
    THROW 51001, 'Review non-adjacent C1 and C2 ranges before applying this repair.', 1;

UPDATE band
SET max_score = legacy.upper_score
FROM dbo.score_bands band JOIN #aptis_general_c2 legacy ON band.id = legacy.c1_id;

DELETE band
FROM dbo.score_bands band JOIN #aptis_general_c2 legacy ON band.id = legacy.c2_id
WHERE legacy.c1_id IS NOT NULL;

UPDATE band
SET cefr_level = 'C1'
FROM dbo.score_bands band JOIN #aptis_general_c2 legacy ON band.id = legacy.c2_id
WHERE legacy.c1_id IS NULL;

SELECT COUNT(*) AS normalized_c2_bands FROM #aptis_general_c2;
DROP TABLE #aptis_general_c2;

-- Defer any exam whose old target has not yet been frozen for every attempt.
UPDATE exam
SET target_level = 'C1'
FROM dbo.mock_exams exam
JOIN dbo.certificates certificate ON certificate.id = exam.certificate_id
WHERE certificate.code = 'APTIS_GENERAL' AND exam.target_level = 'C2'
  AND NOT EXISTS (
      SELECT 1 FROM dbo.exam_attempts attempt
      WHERE attempt.mock_exam_id = exam.id
        AND NOT EXISTS (
            SELECT 1 FROM dbo.exam_attempt_snapshots snapshot
            WHERE snapshot.exam_attempt_id = attempt.id
        )
  );
SELECT @@ROWCOUNT AS normalized_exam_targets;
COMMIT TRANSACTION;
