SET NOCOUNT ON;
INSERT dbo.lesson_check_attempt_answers(attempt_id,question_id,selected_option)
SELECT a.id, TRY_CONVERT(int,REPLACE(v.[key],'q','')), TRY_CONVERT(int,v.value)
FROM dbo.lesson_check_attempts a
CROSS APPLY STRING_SPLIT(COALESCE(a.answers_json,N''),'&') p
CROSS APPLY (SELECT LEFT(p.value,CHARINDEX('=',p.value+'=')-1) [key], SUBSTRING(p.value,CHARINDEX('=',p.value+'=')+1,20) value) v
WHERE a.answers_json IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.lesson_check_attempt_answers x WHERE x.attempt_id=a.id)
  AND TRY_CONVERT(int,REPLACE(v.[key],'q','')) IS NOT NULL;

INSERT dbo.lesson_check_attempt_feedback(attempt_id,prompt,selected,correct,right_answer,explanation,options_text)
SELECT a.id, f.prompt, f.selected, f.correct, COALESCE(f.rightAnswer,0), f.explanation,
       (SELECT STRING_AGG(o.value,CHAR(31)) WITHIN GROUP (ORDER BY CONVERT(int,o.[key])) FROM OPENJSON(f.options) o)
FROM dbo.lesson_check_attempts a
CROSS APPLY OPENJSON(COALESCE(a.review_json,N'[]')) WITH (
    prompt nvarchar(max) '$.prompt', selected nvarchar(max) '$.selected', correct nvarchar(max) '$.correct',
    rightAnswer bit '$.right', explanation nvarchar(max) '$.explanation', options nvarchar(max) '$.options' AS JSON
) f
WHERE a.review_json IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.lesson_check_attempt_feedback x WHERE x.attempt_id=a.id);
GO
