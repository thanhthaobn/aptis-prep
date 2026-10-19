CREATE TABLE lesson_responses (
 id INT IDENTITY(1,1) PRIMARY KEY,
 user_id INT NOT NULL REFERENCES users(id),
 lesson_id INT NOT NULL REFERENCES lessons(id),
 response_text NVARCHAR(MAX) NULL,
 updated_at DATETIME2 NOT NULL,
 CONSTRAINT uq_lesson_response_user_lesson UNIQUE(user_id,lesson_id)
);
