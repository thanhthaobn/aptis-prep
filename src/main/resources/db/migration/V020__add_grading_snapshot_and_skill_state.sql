IF COL_LENGTH('exam_attempts', 'answer_version') IS NULL
BEGIN
    ALTER TABLE exam_attempts ADD answer_version INT NOT NULL
        CONSTRAINT df_exam_attempts_answer_version DEFAULT 1;
END;
GO

IF COL_LENGTH('exam_attempts', 'scoring_scheme_id') IS NULL
    ALTER TABLE exam_attempts ADD scoring_scheme_id INT NULL;
IF COL_LENGTH('exam_attempts', 'policy_version') IS NULL
    ALTER TABLE exam_attempts ADD policy_version VARCHAR(100) NULL;
IF COL_LENGTH('exam_attempts', 'rubric_version') IS NULL
    ALTER TABLE exam_attempts ADD rubric_version VARCHAR(100) NULL;
IF COL_LENGTH('exam_attempts', 'grading_request_key') IS NULL
    ALTER TABLE exam_attempts ADD grading_request_key VARCHAR(255) NULL;
GO

UPDATE exam_attempts
SET rubric_version = 'aptis-rubric-v1'
WHERE rubric_version IS NULL;

IF COL_LENGTH('exam_skill_results', 'grading_status') IS NULL
    ALTER TABLE exam_skill_results ADD grading_status VARCHAR(30) NULL;
IF COL_LENGTH('exam_skill_results', 'objective_raw_score') IS NULL
    ALTER TABLE exam_skill_results ADD objective_raw_score DECIMAL(6,2) NULL;
IF COL_LENGTH('exam_skill_results', 'subjective_raw_score') IS NULL
    ALTER TABLE exam_skill_results ADD subjective_raw_score DECIMAL(6,2) NULL;
GO

UPDATE exam_skill_results
SET grading_status = CASE WHEN scale_score IS NULL THEN 'WAITING' ELSE 'COMPLETED' END
WHERE grading_status IS NULL;
