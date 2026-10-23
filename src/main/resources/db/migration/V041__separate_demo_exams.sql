-- Keep every exam, answer and attempt. Only mark the known V037 demo catalog entries.
ALTER TABLE mock_exams ADD demo_only BIT NOT NULL
    CONSTRAINT df_mock_exams_demo_only DEFAULT 0;
GO
UPDATE mock_exams SET demo_only = 1
WHERE title IN (N'AptisPrep Demo · Writing · 2 câu AI',
                N'AptisPrep Demo · Speaking · transcript',
                N'AptisPrep Demo · Reading · policy');
GO
