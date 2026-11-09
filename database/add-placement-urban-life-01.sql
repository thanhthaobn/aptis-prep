/*
  Optional content import for AptisPrep. Run on the application database after
  generating the audio with scripts/build-placement-urban-life-audio.ps1.
  Requires an active, complete Aptis General scoring policy for all five skills.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lock_result INT;
    EXEC @lock_result = sys.sp_getapplock
        @Resource = N'aptisprep-placement-urban-life-01',
        @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 15000;
    IF @lock_result < 0
        THROW 51000, 'Cannot lock the placement content import.', 1;

    DECLARE @title NVARCHAR(255) = N'Aptis Placement 01 – Urban Life';
    IF EXISTS (SELECT 1 FROM dbo.mock_exams WHERE exam_type = 'PLACEMENT' AND title = @title)
    BEGIN
        SELECT id, title, status FROM dbo.mock_exams WHERE exam_type = 'PLACEMENT' AND title = @title;
        COMMIT TRANSACTION;
        RETURN;
    END;

    DECLARE @sections TABLE (
        position INT PRIMARY KEY, skill VARCHAR(30) UNIQUE, minutes INT
    );
    INSERT INTO @sections VALUES
        (1, 'GRAMMAR_VOCABULARY', 8), (2, 'READING', 10), (3, 'LISTENING', 8),
        (4, 'WRITING', 16), (5, 'SPEAKING', 6);

    DECLARE @certificate_id INT, @scheme_id INT;
    SELECT TOP (1) @certificate_id = c.id, @scheme_id = s.id
    FROM dbo.certificates c
    JOIN dbo.scoring_schemes s ON s.certificate_id = c.id
    WHERE c.code = 'APTIS_GENERAL' AND c.status = 'ACTIVE' AND s.status = 'ACTIVE'
      AND s.scope_skill IS NULL AND (s.exam_type IS NULL OR s.exam_type = 'PLACEMENT')
      AND NOT EXISTS (
          SELECT 1 FROM @sections section
          WHERE NOT EXISTS (
              SELECT 1 FROM dbo.scoring_scale_rules scale_rule
              CROSS APPLY (
                  SELECT MIN(b.min_score) AS first_score, MAX(b.max_score) AS last_score,
                         SUM(CASE WHEN b.cefr_order = 0
                                       OR b.min_score <> b.previous_max + 0.01
                                       OR b.cefr_order <= b.previous_cefr
                                  THEN 1 ELSE 0 END) AS invalid_bands
                  FROM (
                      SELECT min_score, max_score, level_rank.cefr_order,
                             LAG(max_score) OVER (ORDER BY min_score, id) AS previous_max,
                             LAG(level_rank.cefr_order) OVER (ORDER BY min_score, id) AS previous_cefr
                      FROM dbo.score_bands
                      CROSS APPLY (VALUES (CHARINDEX(cefr_level, 'A1A2B1B2C1C2'))) level_rank(cefr_order)
                      WHERE scoring_scheme_id = s.id AND skill = section.skill
                  ) b
              ) bands
              WHERE scale_rule.scoring_scheme_id = s.id AND scale_rule.skill = section.skill
                AND bands.first_score = scale_rule.scale_min AND bands.last_score = scale_rule.scale_max
                AND bands.invalid_bands = 0
                AND (section.skill NOT IN ('WRITING', 'SPEAKING')
                     OR (scale_rule.raw_min = 0 AND scale_rule.raw_max = 50))
          )
      )
    ORDER BY CASE WHEN s.exam_type = 'PLACEMENT' THEN 0 ELSE 1 END, s.id DESC;
    IF @scheme_id IS NULL
        THROW 51001, 'Configure an active Aptis General scoring scheme with rules and complete bands for all five skills.', 1;

    DECLARE @questions TABLE (
        position INT PRIMARY KEY, section INT, part INT, question_type VARCHAR(30),
        title NVARCHAR(200), instruction NVARCHAR(400), passage NVARCHAR(MAX),
        preparation_seconds INT, audio VARCHAR(100)
    );
    INSERT INTO @questions VALUES
        (1, 1, 1, 'SINGLE_CHOICE', N'Grammar in context', N'Choose the best option to complete each sentence.', NULL, NULL, NULL),
        (2, 1, 2, 'SINGLE_CHOICE', N'Vocabulary in context', N'Choose the word or phrase that best fits each sentence.', NULL, NULL, NULL),
        (3, 2, 1, 'SINGLE_CHOICE', N'A notice from Greenbank Library', N'Read the notice and answer the questions.',
         N'From 6 May, the library will open at 8 a.m. on weekdays for students preparing for exams. The study room is free, but you must reserve a desk online by 6 p.m. the previous day. On Saturdays, the library will continue to open at 10 a.m. Anyone returning books before the building opens can use the box beside the main entrance.',
         NULL, NULL),
        (4, 2, 2, 'SINGLE_CHOICE', N'The quiet street experiment', N'Read the article and choose the best answer.',
         N'Last spring, the town of Bellford closed Market Street to through traffic on Sunday mornings. The council expected cafés to benefit and shopkeepers to complain about deliveries. Instead, some cafés reported no change in sales, while several shops supported the trial after seeing families spend longer in the area. Residents on nearby streets, however, noticed more cars passing their homes.

The council has agreed to continue the trial for three more months. During that time, it will count vehicles on surrounding streets as well as visitors on Market Street. The town planner argued that a lively centre alone would be a poor measure of success. In her view, the scheme would be worth keeping only if the benefits could be achieved without transferring the problem elsewhere.',
         NULL, NULL),
        (5, 3, 1, 'SINGLE_CHOICE', N'A message about the market', N'Listen to the voicemail and answer both questions.', NULL, NULL, 'market-message.wav'),
        (6, 3, 2, 'SINGLE_CHOICE', N'An announcement at the station', N'Listen to the announcement and answer both questions.', NULL, NULL, 'station-announcement.wav'),
        (7, 3, 3, 'SINGLE_CHOICE', N'A conversation about a repair café', N'Listen to a volunteer describe the project and answer both questions.', NULL, NULL, 'repair-cafe.wav'),
        (8, 4, 1, 'WRITING_TEXT', N'Join a neighbourhood group',
         N'You have joined an online group for people who want to improve public spaces in their neighbourhood. Answer the two messages from the group.',
         NULL, NULL, NULL),
        (9, 4, 2, 'WRITING_TEXT', N'Write to the council',
         N'Write an email in an appropriate formal style.', NULL, NULL, NULL),
        (10, 5, 1, 'SPEAKING_AUDIO', N'Your neighbourhood',
         N'Record your response in English.', NULL, 0, NULL),
        (11, 5, 2, 'SPEAKING_AUDIO', N'A public space you remember',
         N'Use the preparation time to organise your ideas, then record your response.', NULL, 20, NULL),
        (12, 5, 3, 'SPEAKING_AUDIO', N'A choice for the city',
         N'Give reasons and support your opinion with an example.', NULL, 45, NULL);

    DECLARE @items TABLE (
        question INT, position INT, prompt NVARCHAR(500),
        a NVARCHAR(300), b NVARCHAR(300), c NVARCHAR(300), correct INT,
        min_words INT, max_words INT, response_seconds INT,
        PRIMARY KEY (question, position)
    );
    INSERT INTO @items VALUES
        (1, 1, N'My brother ___ to work by bus every day.', N'go', N'goes', N'going', 2, NULL, NULL, NULL),
        (1, 2, N'We ___ in this flat since January.', N'have lived', N'lived', N'are living', 1, NULL, NULL, NULL),
        (1, 3, N'If the last train leaves before we get there, we ___ a taxi.', N'took', N'would take', N'will take', 3, NULL, NULL, NULL),
        (1, 4, N'The cycle path, ___ was completed last year, links the park to the station.', N'which', N'where', N'what', 1, NULL, NULL, NULL),
        (1, 5, N'Had the organisers checked the forecast, they ___ the event indoors.', N'will move', N'would have moved', N'had moved', 2, NULL, NULL, NULL),
        (2, 1, N'The bus was so ___ that we could not find a seat.', N'crowded', N'empty', N'private', 1, NULL, NULL, NULL),
        (2, 2, N'The roadworks caused a long ___ on the way to the airport.', N'delivery', N'departure', N'delay', 3, NULL, NULL, NULL),
        (2, 3, N'The council will ___ a survey to find out what residents need.', N'carry out', N'turn down', N'put off', 1, NULL, NULL, NULL),
        (2, 4, N'The new footbridge is expected to ___ pressure on the crossing near the school.', N'impose', N'ease', N'preserve', 2, NULL, NULL, NULL),
        (2, 5, N'The proposal is attractive, but it may not be ___ within the current budget.', N'mutual', N'inevitable', N'feasible', 3, NULL, NULL, NULL),
        (3, 1, N'What time does the library open on Saturdays?', N'8 a.m.', N'10 a.m.', N'6 p.m.', 2, NULL, NULL, NULL),
        (3, 2, N'When is the latest you can reserve a desk for Thursday morning?', N'By 6 p.m. on Wednesday', N'By 8 a.m. on Thursday', N'By 10 a.m. on Saturday', 1, NULL, NULL, NULL),
        (3, 3, N'What can visitors do before the library opens?', N'Reserve a desk at reception', N'Enter the study room', N'Return books using the box', 3, NULL, NULL, NULL),
        (4, 1, N'Which outcome differed from what the council expected?', N'Families left the area earlier.', N'Several shops supported the trial.', N'All cafés reported higher sales.', 2, NULL, NULL, NULL),
        (4, 2, N'What problem did residents report?', N'More traffic on nearby streets', N'Fewer deliveries to local shops', N'More noise from cafés at night', 1, NULL, NULL, NULL),
        (4, 3, N'Why will the council count traffic on surrounding streets?', N'To choose new Sunday delivery hours', N'To calculate how many visitors use cafés', N'To check whether the trial moves traffic elsewhere', 3, NULL, NULL, NULL),
        (4, 4, N'What is the town planner''s view of the trial?', N'Support should depend on evidence from inside and outside Market Street.', N'Strong sales alone are enough to justify keeping it.', N'The trial should end before any further evidence is collected.', 1, NULL, NULL, NULL),
        (5, 1, N'What time does the speaker want to meet?', N'9:15', N'9:45', N'10:30', 2, NULL, NULL, NULL),
        (5, 2, N'Where should they meet?', N'By the fountain', N'Outside the bakery', N'By the bus stop', 3, NULL, NULL, NULL),
        (6, 1, N'Which platform will the train leave from?', N'Platform 2', N'Platform 4', N'Platform 6', 1, NULL, NULL, NULL),
        (6, 2, N'What should passengers do if they need North Park?', N'Stay on until the airport.', N'Change at Riverside and take a bus.', N'Wait for the next train on platform 4.', 2, NULL, NULL, NULL),
        (7, 1, N'Why did the repair café start asking people to book?', N'It wanted to charge an entry fee.', N'It had fewer volunteers than usual.', N'Some visitors had been waiting too long.', 3, NULL, NULL, NULL),
        (7, 2, N'What does the speaker see as the café''s main achievement?', N'Visitors gain confidence to try repairs themselves.', N'The café can repair more objects each week.', N'The project earns money for new tools.', 1, NULL, NULL, NULL),
        (8, 1, N'Introduce yourself and describe one public place you often visit.', NULL, NULL, NULL, NULL, 20, 35, NULL),
        (8, 2, N'A member asks what the group should improve first. Suggest one change and explain why it would help.', NULL, NULL, NULL, NULL, 35, 55, NULL),
        (9, 1, N'The council plans to shorten the opening hours of your local community centre. Explain how you and others use it, describe one likely effect of the change, and propose a practical alternative.', NULL, NULL, NULL, NULL, 100, 140, NULL),
        (10, 1, N'Describe where you live. What do you like about the area, and why?', NULL, NULL, NULL, NULL, NULL, NULL, 30),
        (11, 1, N'Talk about a public place you visited recently. Describe what you did there and explain what made the place memorable.', NULL, NULL, NULL, NULL, NULL, NULL, 45),
        (12, 1, N'A city has enough money either to improve its public transport or to build more parks. Compare the benefits of both options. Which should the city choose, and why?', NULL, NULL, NULL, NULL, NULL, NULL, 75);

    DECLARE @now DATETIME2 = SYSUTCDATETIME();
    DECLARE @duration INT = (SELECT SUM(minutes) FROM @sections);
    INSERT INTO dbo.mock_exams
        (certificate_id, scoring_scheme_id, title, description, exam_type, status,
         created_at, updated_at, skill, duration_minutes, demo_only)
    VALUES
        (@certificate_id, @scheme_id, @title,
         N'Bộ đề đánh giá đầu vào chủ đề đời sống đô thị. Gồm 23 câu khách quan, 3 nhiệm vụ Writing và 3 nhiệm vụ Speaking.',
         'PLACEMENT', 'PUBLISHED', @now, @now, 'FULL_MOCK', @duration, 0);
    DECLARE @exam_id INT = SCOPE_IDENTITY();

    INSERT INTO dbo.mock_exam_sections (mock_exam_id, skill, display_order, duration_minutes, status)
    SELECT @exam_id, skill, position, minutes, 'ACTIVE' FROM @sections;

    DECLARE @position INT = 1, @question_id INT;
    WHILE EXISTS (SELECT 1 FROM @questions WHERE position = @position)
    BEGIN
        INSERT INTO dbo.questions
            (content, instruction, question_type, skill, part_no, allow_option_reuse,
             preparation_seconds, status, created_at, updated_at, reading_passage)
        SELECT q.title, q.instruction, q.question_type, s.skill, q.part, 0,
               q.preparation_seconds, 'PUBLISHED', @now, @now, q.passage
        FROM @questions q JOIN @sections s ON s.position = q.section
        WHERE q.position = @position;
        SET @question_id = SCOPE_IDENTITY();

        INSERT INTO dbo.mock_exam_questions (mock_exam_id, mock_exam_section_id, question_id, position)
        SELECT @exam_id, s.id, @question_id, @position
        FROM @questions q JOIN dbo.mock_exam_sections s
          ON s.mock_exam_id = @exam_id AND s.display_order = q.section
        WHERE q.position = @position;

        INSERT INTO dbo.question_items
            (question_id, content, position, min_word_count, max_word_count, response_seconds)
        SELECT @question_id, prompt, position, min_words, max_words, response_seconds
        FROM @items WHERE question = @position;

        INSERT INTO dbo.question_options (question_item_id, content, option_code, is_correct, position)
        SELECT i.id, option_data.content, option_data.code,
               CASE WHEN option_data.position = input.correct THEN 1 ELSE 0 END, option_data.position
        FROM @items input JOIN dbo.question_items i
          ON i.question_id = @question_id AND i.position = input.position
        CROSS APPLY (VALUES (1, 'A', input.a), (2, 'B', input.b), (3, 'C', input.c))
            option_data(position, code, content)
        WHERE input.question = @position AND input.correct IS NOT NULL;

        INSERT INTO dbo.question_media
            (question_id, media_type, media_url, original_filename, content_type, max_play_count, position, created_at)
        SELECT @question_id, 'AUDIO', CONCAT('questions/placement-urban-life-01/', audio),
               audio, 'audio/wav', 2, 1, @now
        FROM @questions WHERE position = @position AND audio IS NOT NULL;

        SET @position += 1;
    END;

    COMMIT TRANSACTION;
    SELECT @exam_id AS exam_id, @title AS title, @scheme_id AS scoring_scheme_id;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
