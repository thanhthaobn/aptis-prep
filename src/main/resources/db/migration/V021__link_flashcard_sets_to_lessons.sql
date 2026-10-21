-- A library flashcard set belongs to a lesson. Personal sets belong to their owner.
-- Move the legacy direct Flashcard -> Lesson link to FlashcardSet -> Lesson while
-- preserving card IDs and USER_FLASHCARD_PROGRESS history.
ALTER TABLE flashcard_sets ADD lesson_id INT NULL;
GO

UPDATE s
SET lesson_id = x.lesson_id
FROM flashcard_sets s
JOIN (
    SELECT set_id, MIN(lesson_id) lesson_id
    FROM flashcards
    WHERE set_id IS NOT NULL AND lesson_id IS NOT NULL
    GROUP BY set_id
    HAVING COUNT(DISTINCT lesson_id) = 1
) x ON x.set_id = s.id
WHERE s.owner_user_id IS NULL;

INSERT INTO flashcard_sets(title, description, cefr_level, status, owner_user_id, visibility, lesson_id)
SELECT l.title, CONCAT(N'Bộ từ vựng của bài: ', l.title), l.cefr_level, l.status, NULL, 'PUBLIC', l.id
FROM lessons l
WHERE EXISTS (SELECT 1 FROM flashcards c WHERE c.lesson_id = l.id AND c.set_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM flashcard_sets s WHERE s.lesson_id = l.id);

UPDATE c
SET set_id = s.id
FROM flashcards c
JOIN flashcard_sets s ON s.lesson_id = c.lesson_id
WHERE c.set_id IS NULL AND c.lesson_id IS NOT NULL;

-- Dữ liệu cũ có thể có nhiều bộ cùng trỏ tới một bài. Giữ bộ mới nhất làm
-- bộ chính của bài; các bộ còn lại vẫn được bảo toàn để Admin liên kết lại.
;WITH ranked_sets AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY lesson_id ORDER BY id DESC) AS row_number
    FROM flashcard_sets
    WHERE lesson_id IS NOT NULL
)
UPDATE s
SET lesson_id = NULL
FROM flashcard_sets s
JOIN ranked_sets r ON r.id = s.id
WHERE r.row_number > 1;

IF EXISTS (SELECT 1 FROM flashcards WHERE set_id IS NULL)
    THROW 50021, 'V021: Flashcard without a lesson or set must be assigned before migration.', 1;

ALTER TABLE flashcard_sets ADD CONSTRAINT fk_flashcard_set_lesson
    FOREIGN KEY (lesson_id) REFERENCES lessons(id);

CREATE UNIQUE INDEX uq_flashcard_sets_lesson
    ON flashcard_sets(lesson_id) WHERE lesson_id IS NOT NULL;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_flashcards_legacy_lesson_order' AND object_id = OBJECT_ID('dbo.flashcards'))
    DROP INDEX uq_flashcards_legacy_lesson_order ON dbo.flashcards;

IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_flashcards_lesson' AND parent_object_id = OBJECT_ID('dbo.flashcards'))
    ALTER TABLE flashcards DROP CONSTRAINT fk_flashcards_lesson;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_flashcards_set' AND object_id = OBJECT_ID('dbo.flashcards'))
    DROP INDEX ix_flashcards_set ON dbo.flashcards;

ALTER TABLE flashcards ALTER COLUMN set_id INT NOT NULL;
ALTER TABLE flashcards DROP COLUMN lesson_id;

CREATE INDEX ix_flashcards_set ON flashcards(set_id,status,display_order);
