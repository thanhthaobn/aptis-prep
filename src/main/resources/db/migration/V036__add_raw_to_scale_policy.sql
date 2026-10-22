IF OBJECT_ID('scoring_scale_rules', 'U') IS NULL
BEGIN
    CREATE TABLE scoring_scale_rules (
        id INT IDENTITY(1,1) PRIMARY KEY,
        scoring_scheme_id INT NOT NULL,
        skill VARCHAR(30) NOT NULL,
        raw_min DECIMAL(6,2) NOT NULL,
        raw_max DECIMAL(6,2) NOT NULL,
        scale_min DECIMAL(5,2) NOT NULL,
        scale_max DECIMAL(5,2) NOT NULL,
        CONSTRAINT fk_scoring_scale_rules_scheme FOREIGN KEY (scoring_scheme_id) REFERENCES scoring_schemes(id),
        CONSTRAINT uq_scoring_scale_rules_scheme_skill UNIQUE (scoring_scheme_id, skill),
        CONSTRAINT ck_scoring_scale_rules_range CHECK (raw_min < raw_max AND scale_min <= scale_max)
    );
END;
GO

DECLARE @scheme_id INT = (SELECT id FROM scoring_schemes WHERE version = '1.0' AND certificate_id = (SELECT id FROM certificates WHERE code = 'APTIS_GENERAL'));
IF @scheme_id IS NOT NULL
BEGIN
    -- Internal demo calibration only; these values are not official Aptis thresholds.
    INSERT INTO scoring_scale_rules(scoring_scheme_id, skill, raw_min, raw_max, scale_min, scale_max)
    SELECT @scheme_id, value.skill, value.raw_min, value.raw_max, value.scale_min, value.scale_max
    FROM (VALUES
        ('GRAMMAR_VOCABULARY', 0.00, 50.00, 0.00, 50.00),
        ('READING',             0.00, 40.00, 0.00, 50.00),
        ('LISTENING',           0.00, 40.00, 0.00, 50.00),
        ('WRITING',             0.00, 50.00, 0.00, 50.00),
        ('SPEAKING',            0.00, 50.00, 0.00, 50.00)
    ) value(skill, raw_min, raw_max, scale_min, scale_max)
    WHERE NOT EXISTS (SELECT 1 FROM scoring_scale_rules r WHERE r.scoring_scheme_id = @scheme_id AND r.skill = value.skill);
END;
