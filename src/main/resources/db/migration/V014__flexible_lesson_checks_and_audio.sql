ALTER TABLE lesson_checks ADD option_d nvarchar(1000) NULL;
ALTER TABLE lesson_checks ADD option_e nvarchar(1000) NULL;
ALTER TABLE lesson_checks ADD option_f nvarchar(1000) NULL;
ALTER TABLE lesson_checks ALTER COLUMN option_c nvarchar(1000) NULL;

ALTER TABLE lessons ADD audio_path nvarchar(500) NULL;
ALTER TABLE lessons ADD audio_name nvarchar(255) NULL;
