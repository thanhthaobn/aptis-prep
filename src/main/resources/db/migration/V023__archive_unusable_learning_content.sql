-- This old reading row has no teaching method, example or practice content.
-- Archive instead of delete to preserve any learner history that references it.
UPDATE lessons
SET status='ARCHIVED', updated_at=SYSDATETIME()
WHERE title=N'Reading A2: Một cuối tuần ở Đà Lạt'
  AND NULLIF(LTRIM(RTRIM(ISNULL(strategy,N''))),N'') IS NULL
  AND NULLIF(LTRIM(RTRIM(ISNULL(practice_prompt,N''))),N'') IS NULL;

-- A public library set must belong to a lesson. Personal sets intentionally
-- have no lesson and are excluded because they have an owner.
UPDATE flashcard_sets
SET status='ARCHIVED'
WHERE owner_user_id IS NULL
  AND lesson_id IS NULL
  AND status<>'ARCHIVED';
