INSERT INTO certificates (name, code, description, status)
VALUES (N'Aptis ESOL General', 'APTIS_GENERAL', N'Certificate cố định của hệ thống AptisPrep', 'ACTIVE');

DECLARE @certificate_id INT = (SELECT id FROM certificates WHERE code = 'APTIS_GENERAL');

INSERT INTO scoring_schemes (certificate_id, name, version, status, effective_from)
VALUES (@certificate_id, N'Estimated Aptis Scoring', '1.0', 'ACTIVE', SYSDATETIME());

-- Các khoảng điểm bên dưới chỉ phục vụ demo Estimated CEFR.
-- Không được mô tả hoặc hiển thị là ngưỡng Aptis chính thức.

DECLARE @scheme_id INT = (
    SELECT id FROM scoring_schemes
    WHERE certificate_id = @certificate_id AND version = '1.0'
);

INSERT INTO score_bands (scoring_scheme_id, skill, cefr_level, min_score, max_score)
VALUES
(@scheme_id, 'READING', 'A1', 0, 9.99),
(@scheme_id, 'READING', 'A2', 10, 19.99),
(@scheme_id, 'READING', 'B1', 20, 29.99),
(@scheme_id, 'READING', 'B2', 30, 39.99),
(@scheme_id, 'READING', 'C1', 40, 45.99),
(@scheme_id, 'READING', 'C2', 46, 50),
(@scheme_id, 'LISTENING', 'A1', 0, 9.99),
(@scheme_id, 'LISTENING', 'A2', 10, 19.99),
(@scheme_id, 'LISTENING', 'B1', 20, 29.99),
(@scheme_id, 'LISTENING', 'B2', 30, 39.99),
(@scheme_id, 'LISTENING', 'C1', 40, 45.99),
(@scheme_id, 'LISTENING', 'C2', 46, 50),
(@scheme_id, 'WRITING', 'A1', 0, 9.99),
(@scheme_id, 'WRITING', 'A2', 10, 19.99),
(@scheme_id, 'WRITING', 'B1', 20, 29.99),
(@scheme_id, 'WRITING', 'B2', 30, 39.99),
(@scheme_id, 'WRITING', 'C1', 40, 45.99),
(@scheme_id, 'WRITING', 'C2', 46, 50),
(@scheme_id, 'SPEAKING', 'A1', 0, 9.99),
(@scheme_id, 'SPEAKING', 'A2', 10, 19.99),
(@scheme_id, 'SPEAKING', 'B1', 20, 29.99),
(@scheme_id, 'SPEAKING', 'B2', 30, 39.99),
(@scheme_id, 'SPEAKING', 'C1', 40, 45.99),
(@scheme_id, 'SPEAKING', 'C2', 46, 50);
