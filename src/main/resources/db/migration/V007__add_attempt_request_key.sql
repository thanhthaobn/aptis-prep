ALTER TABLE exam_attempts ADD request_key VARCHAR(255) NULL;
GO

CREATE UNIQUE INDEX uq_exam_attempts_user_request_key
    ON exam_attempts(user_id, request_key)
    WHERE request_key IS NOT NULL;
