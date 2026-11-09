SET NOCOUNT ON;
INSERT dbo.lesson_playback_ranges(engagement_id,start_seconds,end_seconds)
SELECT e.id,
       TRY_CONVERT(float, JSON_VALUE(r.value,'$[0]')),
       TRY_CONVERT(float, JSON_VALUE(r.value,'$[1]'))
FROM dbo.lesson_engagement e
CROSS APPLY OPENJSON(e.playback_ranges_json) r
WHERE e.playback_ranges_json IS NOT NULL
  AND ISJSON(e.playback_ranges_json)=1
  AND NOT EXISTS (SELECT 1 FROM dbo.lesson_playback_ranges x WHERE x.engagement_id=e.id)
  AND TRY_CONVERT(float, JSON_VALUE(r.value,'$[0]')) IS NOT NULL
  AND TRY_CONVERT(float, JSON_VALUE(r.value,'$[1]')) IS NOT NULL;
GO
