CREATE TABLE flashcard_sets (
 id INT IDENTITY(1,1) PRIMARY KEY,
 title NVARCHAR(255) NOT NULL,
 description NVARCHAR(2000) NULL,
 cefr_level VARCHAR(10) NOT NULL,
 status VARCHAR(30) NOT NULL,
 legacy_lesson_id INT NULL
);
ALTER TABLE flashcards ALTER COLUMN lesson_id INT NULL;
ALTER TABLE flashcards ADD set_id INT NULL, status VARCHAR(30) NOT NULL CONSTRAINT df_flashcards_status DEFAULT 'ACTIVE';
GO
-- Preserve card IDs and learner history; collect legacy cards into their lesson's set.
INSERT INTO flashcard_sets(title,description,cefr_level,status,legacy_lesson_id)
SELECT title,CONCAT(N'Bộ từ vựng: ',title),cefr_level,status,id FROM lessons WHERE id IN (SELECT lesson_id FROM flashcards);
UPDATE c SET set_id=s.id FROM flashcards c JOIN flashcard_sets s ON c.lesson_id=s.legacy_lesson_id;
ALTER TABLE flashcard_sets DROP COLUMN legacy_lesson_id;
ALTER TABLE flashcards ADD CONSTRAINT fk_flashcard_set FOREIGN KEY(set_id) REFERENCES flashcard_sets(id);
CREATE INDEX ix_flashcards_set ON flashcards(set_id,status,display_order);
