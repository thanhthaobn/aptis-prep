-- Remove only the legacy Reading sets created automatically by V007 from view.
-- Keep their card IDs and review history recoverable.
UPDATE flashcard_sets SET status='ARCHIVED'
WHERE title IN (N'Reading A1: Giới thiệu bản thân', N'Reading A1: Một ngày của Lucy', N'Reading A1: Đọc lời mời qua email')
AND description=CONCAT(N'Bộ từ vựng: ',title)
AND EXISTS (SELECT 1 FROM flashcards c WHERE c.set_id=flashcard_sets.id AND c.lesson_id IS NOT NULL);
