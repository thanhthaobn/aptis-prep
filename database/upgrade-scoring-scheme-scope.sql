SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.scoring_schemes', 'exam_type') IS NULL
    ALTER TABLE dbo.scoring_schemes ADD exam_type varchar(30) NULL;
IF COL_LENGTH('dbo.scoring_schemes', 'scope_skill') IS NULL
    ALTER TABLE dbo.scoring_schemes ADD scope_skill varchar(30) NULL;
COMMIT TRANSACTION;
