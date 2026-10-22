-- Remove the answer-revealing notice from the current Listening lesson only.
-- Do not delete the shared image file or rewrite historical submissions.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
UPDATE lessons
SET image_path=NULL,image_name=NULL,updated_at=SYSDATETIME()
WHERE id=42 AND skill='Listening' AND title=N'Nghe thông báo giờ mở cửa thư viện'
  AND (image_path IS NOT NULL OR image_name IS NOT NULL);
COMMIT;
SELECT id,title,image_path,audio_path FROM lessons WHERE id=42;
