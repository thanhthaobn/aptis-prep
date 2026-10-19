ALTER TABLE flashcard_sets ADD owner_user_id int NULL;
ALTER TABLE flashcard_sets ADD visibility varchar(20) NOT NULL CONSTRAINT df_flashcard_visibility DEFAULT 'PUBLIC';
GO
ALTER TABLE flashcard_sets ADD CONSTRAINT fk_flashcard_set_owner FOREIGN KEY(owner_user_id) REFERENCES users(id);
ALTER TABLE flashcard_sets ADD CONSTRAINT ck_flashcard_visibility CHECK(visibility IN ('PUBLIC','PRIVATE'));
GO
CREATE INDEX ix_flashcard_sets_owner ON flashcard_sets(owner_user_id, status);
