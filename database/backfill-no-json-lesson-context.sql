SET NOCOUNT ON;
DECLARE @id int, @json nvarchar(max), @newId int;
DECLARE c CURSOR LOCAL FAST_FORWARD FOR
    SELECT id, content_snapshot FROM dbo.lesson_check_attempts
    WHERE content_snapshot IS NOT NULL AND ISJSON(content_snapshot)=1 AND context_snapshot_id IS NULL;
OPEN c; FETCH NEXT FROM c INTO @id,@json;
WHILE @@FETCH_STATUS=0
BEGIN
    INSERT dbo.lesson_context_snapshots(title,skill,description,objectives,strategy,example_text,analysis,practice_prompt,image_path,audio_path,video_path,video_url,min_words,max_words)
    SELECT title,skill,description,objectives,strategy,exampleText,analysis,practicePrompt,imagePath,audioPath,videoPath,videoUrl,minWords,maxWords
    FROM OPENJSON(@json) WITH (title nvarchar(255) '$.title', skill varchar(30) '$.skill', description nvarchar(max) '$.description', objectives nvarchar(max) '$.objectives', strategy nvarchar(max) '$.strategy', exampleText nvarchar(max) '$.exampleText', analysis nvarchar(max) '$.analysis', practicePrompt nvarchar(max) '$.practicePrompt', imagePath nvarchar(500) '$.imagePath', audioPath nvarchar(500) '$.audioPath', videoPath nvarchar(500) '$.videoPath', videoUrl nvarchar(1000) '$.videoUrl', minWords int '$.minWords', maxWords int '$.maxWords');
    SET @newId=CONVERT(int,SCOPE_IDENTITY()); UPDATE dbo.lesson_check_attempts SET context_snapshot_id=@newId WHERE id=@id;
    FETCH NEXT FROM c INTO @id,@json;
END
CLOSE c; DEALLOCATE c;

DECLARE w CURSOR LOCAL FAST_FORWARD FOR
    SELECT id, content_snapshot FROM dbo.lesson_work_submissions
    WHERE content_snapshot IS NOT NULL AND ISJSON(content_snapshot)=1 AND context_snapshot_id IS NULL;
OPEN w; FETCH NEXT FROM w INTO @id,@json;
WHILE @@FETCH_STATUS=0
BEGIN
    INSERT dbo.lesson_context_snapshots(title,skill,description,objectives,strategy,example_text,analysis,practice_prompt,image_path,audio_path,video_path,video_url,min_words,max_words)
    SELECT title,skill,description,objectives,strategy,exampleText,analysis,practicePrompt,imagePath,audioPath,videoPath,videoUrl,minWords,maxWords
    FROM OPENJSON(@json) WITH (title nvarchar(255) '$.title', skill varchar(30) '$.skill', description nvarchar(max) '$.description', objectives nvarchar(max) '$.objectives', strategy nvarchar(max) '$.strategy', exampleText nvarchar(max) '$.exampleText', analysis nvarchar(max) '$.analysis', practicePrompt nvarchar(max) '$.practicePrompt', imagePath nvarchar(500) '$.imagePath', audioPath nvarchar(500) '$.audioPath', videoPath nvarchar(500) '$.videoPath', videoUrl nvarchar(1000) '$.videoUrl', minWords int '$.minWords', maxWords int '$.maxWords');
    SET @newId=CONVERT(int,SCOPE_IDENTITY()); UPDATE dbo.lesson_work_submissions SET context_snapshot_id=@newId WHERE id=@id;
    FETCH NEXT FROM w INTO @id,@json;
END
CLOSE w; DEALLOCATE w;
GO
