-- Run against an existing AptisPrep database before enabling custom exam scales.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

ALTER TABLE dbo.score_bands DROP CONSTRAINT ck_score_bands_range;
ALTER TABLE dbo.score_bands WITH CHECK ADD CONSTRAINT ck_score_bands_range
    CHECK (min_score <= max_score AND min_score >= 0 AND max_score <= 999.99);

ALTER TABLE dbo.exam_skill_results DROP CONSTRAINT ck_skill_results_scale;
ALTER TABLE dbo.exam_skill_results WITH CHECK ADD CONSTRAINT ck_skill_results_scale
    CHECK (scale_score IS NULL OR (scale_score >= 0 AND scale_score <= 999.99));

ALTER TABLE dbo.exam_attempts DROP CONSTRAINT ck_exam_attempts_score;
ALTER TABLE dbo.exam_attempts WITH CHECK ADD CONSTRAINT ck_exam_attempts_score
    CHECK (final_scale_score IS NULL OR (final_scale_score >= 0 AND final_scale_score <= 999.99));

COMMIT TRANSACTION;
