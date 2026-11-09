/* Backfill the relational snapshot tables from the legacy JSON column. */
SET NOCOUNT ON;
DECLARE @snapshotId int, @snapshotJson nvarchar(max), @questionJson nvarchar(max);
DECLARE snapshot_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT id, locked_question_snapshot
    FROM dbo.exam_attempt_snapshots
    WHERE locked_question_snapshot IS NOT NULL
      AND ISJSON(locked_question_snapshot) = 1
      AND NOT EXISTS (
          SELECT 1 FROM dbo.exam_attempt_snapshot_questions q WHERE q.snapshot_id = exam_attempt_snapshots.id
      );
OPEN snapshot_cursor;
FETCH NEXT FROM snapshot_cursor INTO @snapshotId, @snapshotJson;
WHILE @@FETCH_STATUS = 0
BEGIN
    DECLARE @questionId int, @position int, @skill varchar(30), @questionType varchar(50),
            @content nvarchar(max), @instruction nvarchar(max), @readingPassage nvarchar(max),
            @sectionStart int, @sectionEnd int, @items nvarchar(max), @media nvarchar(max);
    DECLARE question_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT id, position, skill, questionType, content, instruction, readingPassage,
               sectionStartMinute, sectionEndMinute, items, media
        FROM OPENJSON(@snapshotJson)
        WITH (
            id int '$.id',
            position int '$.position',
            skill varchar(30) '$.skill',
            questionType varchar(50) '$.type',
            content nvarchar(max) '$.content',
            instruction nvarchar(max) '$.instruction',
            readingPassage nvarchar(max) '$.readingPassage',
            sectionStartMinute int '$.sectionStartMinute',
            sectionEndMinute int '$.sectionEndMinute',
            items nvarchar(max) '$.items' AS JSON,
            media nvarchar(max) '$.media' AS JSON
        );
    OPEN question_cursor;
    FETCH NEXT FROM question_cursor INTO @questionId,@position,@skill,@questionType,@content,@instruction,@readingPassage,@sectionStart,@sectionEnd,@items,@media;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        INSERT dbo.exam_attempt_snapshot_questions(snapshot_id,source_question_id,position,skill,question_type,content,instruction,reading_passage,section_start_minute,section_end_minute)
        VALUES(@snapshotId,@questionId,@position,@skill,@questionType,@content,@instruction,@readingPassage,@sectionStart,@sectionEnd);
        DECLARE @questionSnapshotId int = CONVERT(int,SCOPE_IDENTITY());

        INSERT dbo.exam_attempt_snapshot_items(question_snapshot_id,source_item_id,position,content,correct_position,min_word_count,max_word_count,response_seconds)
        SELECT @questionSnapshotId,id,position,content,correctPosition,minWordCount,maxWordCount,responseSeconds
        FROM OPENJSON(COALESCE(@items,N'[]')) WITH (
            id int '$.id', position int '$.position', content nvarchar(max) '$.content',
            correctPosition int '$.correctPosition', minWordCount int '$.minWordCount',
            maxWordCount int '$.maxWordCount', responseSeconds int '$.responseSeconds',
            options nvarchar(max) '$.options' AS JSON
        );

        INSERT dbo.exam_attempt_snapshot_options(item_snapshot_id,source_option_id,option_code,position,content,correct)
        SELECT i.id,o.id,o.code,o.position,o.content,COALESCE(o.correct,0)
        FROM dbo.exam_attempt_snapshot_items i
        JOIN OPENJSON(COALESCE(@items,N'[]')) WITH (
            id int '$.id', options nvarchar(max) '$.options' AS JSON
        ) src ON src.id=i.source_item_id AND i.question_snapshot_id=@questionSnapshotId
        CROSS APPLY OPENJSON(COALESCE(src.options,N'[]')) WITH (
            id int '$.id', code varchar(30) '$.code', position int '$.position',
            content nvarchar(max) '$.content', correct bit '$.correct'
        ) o;

        INSERT dbo.exam_attempt_snapshot_media(question_snapshot_id,source_media_id,position,media_type,media_url,original_filename,content_type,transcript,max_play_count)
        SELECT @questionSnapshotId,id,0,type,mediaUrl,filename,contentType,transcript,COALESCE(maxPlayCount,2)
        FROM OPENJSON(COALESCE(@media,N'[]')) WITH (
            id int '$.id', type varchar(30) '$.type', mediaUrl nvarchar(max) '$.mediaUrl',
            filename nvarchar(255) '$.filename', contentType varchar(100) '$.contentType',
            transcript nvarchar(max) '$.transcript', maxPlayCount int '$.maxPlayCount'
        );
        FETCH NEXT FROM question_cursor INTO @questionId,@position,@skill,@questionType,@content,@instruction,@readingPassage,@sectionStart,@sectionEnd,@items,@media;
    END
    CLOSE question_cursor; DEALLOCATE question_cursor;
    FETCH NEXT FROM snapshot_cursor INTO @snapshotId, @snapshotJson;
END
CLOSE snapshot_cursor; DEALLOCATE snapshot_cursor;
GO
