-- SQL Server treats NULL as a value in unique constraints. The legacy section
-- uniques would prevent the same question or position in two sectionless exams.
-- V004 already enforces the stronger exam-wide uniqueness required by V2.
ALTER TABLE mock_exam_questions DROP CONSTRAINT uq_exam_questions_question;
ALTER TABLE mock_exam_questions DROP CONSTRAINT uq_exam_questions_order;
