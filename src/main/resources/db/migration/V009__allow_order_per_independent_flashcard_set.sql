-- SQL Server treats NULL as a value in a UNIQUE constraint. Independent sets
-- must be able to use the same display_order with a NULL lesson_id.
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uq_flashcards_lesson_order' AND parent_object_id = OBJECT_ID('dbo.flashcards'))
    ALTER TABLE dbo.flashcards DROP CONSTRAINT uq_flashcards_lesson_order;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_flashcards_legacy_lesson_order' AND object_id = OBJECT_ID('dbo.flashcards'))
    CREATE UNIQUE INDEX uq_flashcards_legacy_lesson_order
    ON dbo.flashcards(lesson_id, display_order) WHERE lesson_id IS NOT NULL;
