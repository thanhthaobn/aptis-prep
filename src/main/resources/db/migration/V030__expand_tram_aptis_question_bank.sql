/*
 * Expand Trâm's catalogue with original Aptis-style practice content.
 * The wording is authored for AptisPrep and is not copied from a live exam.
 * The structure follows the public Aptis General format: core grammar/vocabulary,
 * reading cohesion/opinion/headings, listening information/inference, four
 * writing parts and four speaking parts.
 */
DECLARE @certificate_id INT = (SELECT id FROM certificates WHERE code = 'APTIS_GENERAL');
DECLARE @scheme_id INT = (SELECT TOP 1 id FROM scoring_schemes WHERE certificate_id = @certificate_id AND status = 'ACTIVE' ORDER BY version DESC);
IF @certificate_id IS NULL OR @scheme_id IS NULL
    THROW 50027, 'Expanded Aptis catalogue requires the Aptis certificate and scoring scheme.', 1;

DECLARE @item INT;
DECLARE @gv1 INT, @gv2 INT, @gv3 INT, @gv4 INT, @gv5 INT;
DECLARE @r1 INT, @r2 INT, @r3 INT, @r4 INT, @r5 INT;
DECLARE @l1 INT, @l2 INT, @l3 INT, @l4 INT, @l5 INT;
DECLARE @w1 INT, @w2 INT, @w3 INT, @w4 INT, @w5 INT;
DECLARE @s1 INT, @s2 INT, @s3 INT, @s4 INT, @s5 INT;

/* Grammar and Vocabulary — 25 tasks represented by five multi-item questions. */
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Choose the correct word to complete each sentence.',N'Part 1 Grammar. Read the whole sentence before choosing an answer.','SINGLE_CHOICE','GRAMMAR_VOCABULARY',1,'B1','MEDIUM','PUBLISHED');
SET @gv1=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@gv1,N'If the weather ___ better tomorrow, we will hold the meeting outside.',3,1),(@gv1,N'She ___ this course since September.',2,2),(@gv1,N'I was tired, ___ I finished the report before leaving.',1,3),(@gv1,N'The new library ___ by the council last year.',2,4),(@gv1,N'Could you tell me where ___ the nearest bus stop is?',1,5);
SELECT @item=id FROM question_items WHERE question_id=@gv1 AND position=1;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'is',N'A',1,1),(@item,N'was',N'B',0,2),(@item,N'will be',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv1 AND position=2;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'attends',N'A',0,1),(@item,N'has attended',N'B',1,2),(@item,N'attended',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv1 AND position=3;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'but',N'A',1,1),(@item,N'because',N'B',0,2),(@item,N'unless',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv1 AND position=4;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'opens',N'A',0,1),(@item,N'was opened',N'B',1,2),(@item,N'has opened',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv1 AND position=5;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'where',N'A',1,1),(@item,N'what',N'B',0,2),(@item,N'which',N'C',0,3);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Choose the best grammar option for each sentence.',N'Part 1 Grammar. There is one correct answer for each item.','SINGLE_CHOICE','GRAMMAR_VOCABULARY',1,'B2','HARD','PUBLISHED');
SET @gv2=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@gv2,N'By the time the guests arrived, we ___ the room.',2,1),(@gv2,N'It is important that every applicant ___ the form carefully.',3,2),(@gv2,N'We would have caught the train if we ___ earlier.',1,3),(@gv2,N'The manager asked whether I ___ available on Friday.',2,4),(@gv2,N'The project is expected ___ by the end of June.',3,5);
SELECT @item=id FROM question_items WHERE question_id=@gv2 AND position=1;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'prepare',N'A',0,1),(@item,N'had prepared',N'B',1,2),(@item,N'are preparing',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv2 AND position=2;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'completes',N'A',0,1),(@item,N'completed',N'B',0,2),(@item,N'complete',N'C',1,3);
SELECT @item=id FROM question_items WHERE question_id=@gv2 AND position=3;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'had left',N'A',1,1),(@item,N'left',N'B',0,2),(@item,N'would leave',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv2 AND position=4;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'am',N'A',0,1),(@item,N'would be',N'B',1,2),(@item,N'have been',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv2 AND position=5;
INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'finish',N'A',0,1),(@item,N'finishing',N'B',0,2),(@item,N'to be finished',N'C',1,3);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Match each word with the meaning that fits it best.',N'Part 2 Vocabulary. Choose one definition for each word.','MATCHING','GRAMMAR_VOCABULARY',2,'B1','MEDIUM','PUBLISHED');
SET @gv3=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@gv3,N'reliable',1,1),(@gv3,N'postpone',2,2),(@gv3,N'affordable',3,3),(@gv3,N'brief',2,4),(@gv3,N'achievement',1,5);
/* Explicit rows keep SQL Server's identity values and answer keys readable. */
SELECT @item=id FROM question_items WHERE question_id=@gv3 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'can be trusted',N'A',1,1),(@item,N'very expensive',N'B',0,2),(@item,N'quickly changing',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv3 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'to explain',N'A',0,1),(@item,N'to delay until later',N'B',1,2),(@item,N'to remove',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv3 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'easy to carry',N'A',0,1),(@item,N'available online',N'B',0,2),(@item,N'not too expensive',N'C',1,3);
SELECT @item=id FROM question_items WHERE question_id=@gv3 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'lasting a short time',N'A',1,1),(@item,N'full of detail',N'B',0,2),(@item,N'not yet finished',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv3 AND position=5; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'something successfully completed',N'A',1,1),(@item,N'a difficult question',N'B',0,2),(@item,N'a public complaint',N'C',0,3);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Choose the word that completes each sentence naturally.',N'Part 2 Vocabulary. Check meaning and collocation.','SINGLE_CHOICE','GRAMMAR_VOCABULARY',2,'B2','HARD','PUBLISHED');
SET @gv4=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@gv4,N'The course provides a useful ___ of practical skills.',2,1),(@gv4,N'We need to ___ the problem before it becomes serious.',1,2),(@gv4,N'The museum has ___ a new exhibition for local artists.',3,3),(@gv4,N'Her explanation was clear and highly ___.',2,4),(@gv4,N'The committee reached a ___ after a long discussion.',1,5);
SELECT @item=id FROM question_items WHERE question_id=@gv4 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'collection',N'A',0,1),(@item,N'range',N'B',1,2),(@item,N'row',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv4 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'address',N'A',1,1),(@item,N'attach',N'B',0,2),(@item,N'allow',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv4 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'opened',N'A',0,1),(@item,N'closed',N'B',0,2),(@item,N'launched',N'C',1,3);
SELECT @item=id FROM question_items WHERE question_id=@gv4 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'considerate',N'A',0,1),(@item,N'informative',N'B',1,2),(@item,N'industrial',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv4 AND position=5; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'decision',N'A',1,1),(@item,N'permission',N'B',0,2),(@item,N'knowledge',N'C',0,3);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Match each word with the word or phrase it commonly combines with.',N'Part 2 Vocabulary. Focus on common word combinations.','MATCHING','GRAMMAR_VOCABULARY',2,'B2','MEDIUM','PUBLISHED');
SET @gv5=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@gv5,N'make',1,1),(@gv5,N'heavy',2,2),(@gv5,N'public',3,3),(@gv5,N'conduct',1,4),(@gv5,N'long-term',2,5);
SELECT @item=id FROM question_items WHERE question_id=@gv5 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'a decision',N'A',1,1),(@item,N'a decisioning',N'B',0,2),(@item,N'a deciding',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv5 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'weather',N'A',0,1),(@item,N'traffic',N'B',1,2),(@item,N'journey',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv5 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'library',N'A',0,1),(@item,N'place',N'B',0,2),(@item,N'transport',N'C',1,3);
SELECT @item=id FROM question_items WHERE question_id=@gv5 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'research',N'A',1,1),(@item,N'knowledge',N'B',0,2),(@item,N'progress',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@gv5 AND position=5; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'career',N'A',0,1),(@item,N'planning',N'B',1,2),(@item,N'weather',N'C',0,3);

/* Reading — sentence comprehension, cohesion, opinion matching and headings. */
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Read the notice about the community garden and choose the best word for each sentence.',N'Part 1 Sentence comprehension.','SINGLE_CHOICE','READING',1,'A2','MEDIUM','PUBLISHED');
SET @r1=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@r1,N'The garden is open to local ___ at weekends.',2,1),(@r1,N'Volunteers should bring their own ___ if possible.',3,2),(@r1,N'Produce from the garden is shared ___ the members.',1,3),(@r1,N'The next planning meeting is held on ___ evening.',2,4),(@r1,N'New members can ask the coordinator for more ___.',1,5);
SELECT @item=id FROM question_items WHERE question_id=@r1 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'visitors',N'A',0,1),(@item,N'residents',N'B',1,2),(@item,N'passengers',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@r1 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'clothes',N'A',0,1),(@item,N'paper',N'B',0,2),(@item,N'gloves',N'C',1,3);
SELECT @item=id FROM question_items WHERE question_id=@r1 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'among',N'A',1,1),(@item,N'during',N'B',0,2),(@item,N'against',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@r1 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Tuesday',N'A',0,1),(@item,N'Thursday',N'B',1,2),(@item,N'Saturday',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@r1 AND position=5; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'information',N'A',1,1),(@item,N'invention',N'B',0,2),(@item,N'invitation',N'C',0,3);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Put the sentences into the correct order to make a coherent text about a repair café.',N'Part 2 Text cohesion. The opening sentence is already in place.','ORDERING','READING',2,'B1','MEDIUM','PUBLISHED');
SET @r2=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@r2,N'First, visitors describe the broken item to a volunteer.',1,1),(@r2,N'The volunteer then checks whether the repair is safe to attempt.',2,2),(@r2,N'Next, the pair chooses the right tools from the shared shelves.',3,3),(@r2,N'They work together while discussing what caused the problem.',4,4),(@r2,N'Finally, the visitor tests the item and records the result.',5,5),(@r2,N'This simple process keeps useful objects in use for longer.',6,6);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Read four opinions about evening classes and match each statement with the correct person.',N'Part 3 Opinion matching. Each statement is used once.','MATCHING','READING',3,'B1','HARD','PUBLISHED');
SET @r3=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@r3,N'Learns best when the teacher gives immediate feedback.',1,1),(@r3,N'Chooses a course mainly because it creates social opportunities.',2,2),(@r3,N'Prefers a flexible schedule because of work commitments.',3,3),(@r3,N'Worries that online study can feel too isolated.',4,4),(@r3,N'Would rather study a practical subject than a theoretical one.',1,5),(@r3,N'Believes short sessions are easier to maintain than long ones.',3,6),(@r3,N'Uses recorded material to review difficult points.',4,7);
SELECT @item=id FROM question_items WHERE question_id=@r3 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Ana',N'A',1,1),(@item,N'Ben',N'B',0,2),(@item,N'Chi',N'C',0,3),(@item,N'Diego',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@r3 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Ana',N'A',0,1),(@item,N'Ben',N'B',1,2),(@item,N'Chi',N'C',0,3),(@item,N'Diego',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@r3 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Ana',N'A',0,1),(@item,N'Ben',N'B',0,2),(@item,N'Chi',N'C',1,3),(@item,N'Diego',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@r3 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Ana',N'A',0,1),(@item,N'Ben',N'B',0,2),(@item,N'Chi',N'C',0,3),(@item,N'Diego',N'D',1,4);
SELECT @item=id FROM question_items WHERE question_id=@r3 AND position=5; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Ana',N'A',1,1),(@item,N'Ben',N'B',0,2),(@item,N'Chi',N'C',0,3),(@item,N'Diego',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@r3 AND position=6; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Ana',N'A',0,1),(@item,N'Ben',N'B',0,2),(@item,N'Chi',N'C',1,3),(@item,N'Diego',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@r3 AND position=7; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Ana',N'A',0,1),(@item,N'Ben',N'B',0,2),(@item,N'Chi',N'C',0,3),(@item,N'Diego',N'D',1,4);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Read a short article about libraries changing their services and match each paragraph with the best heading.',N'Part 4 Long text comprehension. One heading is extra.','MATCHING','READING',4,'B2','HARD','PUBLISHED');
SET @r4=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@r4,N'Paragraph 1: A library begins by asking residents what support they need most.',1,1),(@r4,N'Paragraph 2: Evening opening hours make the building useful to shift workers.',2,2),(@r4,N'Paragraph 3: Staff train visitors to use reliable online information.',3,3),(@r4,N'Paragraph 4: Shared work tables help freelancers who cannot work at home.',4,4),(@r4,N'Paragraph 5: The programme is reviewed using attendance and feedback.',5,5);
SELECT @item=id FROM question_items WHERE question_id=@r4 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Listening before changing services',N'A',1,1),(@item,N'A digital skill for everyone',N'B',0,2),(@item,N'A quiet place to work',N'C',0,3),(@item,N'Checking whether the plan works',N'D',0,4),(@item,N'Opening a second building',N'E',0,5),(@item,N'Learning from other cities',N'F',0,6);
SELECT @item=id FROM question_items WHERE question_id=@r4 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Listening before changing services',N'A',0,1),(@item,N'A digital skill for everyone',N'B',1,2),(@item,N'A quiet place to work',N'C',0,3),(@item,N'Checking whether the plan works',N'D',0,4),(@item,N'Opening a second building',N'E',0,5),(@item,N'Learning from other cities',N'F',0,6);
SELECT @item=id FROM question_items WHERE question_id=@r4 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Listening before changing services',N'A',0,1),(@item,N'A digital skill for everyone',N'B',0,2),(@item,N'A quiet place to work',N'C',1,3),(@item,N'Checking whether the plan works',N'D',0,4),(@item,N'Opening a second building',N'E',0,5),(@item,N'Learning from other cities',N'F',0,6);
SELECT @item=id FROM question_items WHERE question_id=@r4 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Listening before changing services',N'A',0,1),(@item,N'A digital skill for everyone',N'B',0,2),(@item,N'A quiet place to work',N'C',0,3),(@item,N'Checking whether the plan works',N'D',1,4),(@item,N'Opening a second building',N'E',0,5),(@item,N'Learning from other cities',N'F',0,6);
SELECT @item=id FROM question_items WHERE question_id=@r4 AND position=5; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Listening before changing services',N'A',0,1),(@item,N'A digital skill for everyone',N'B',0,2),(@item,N'A quiet place to work',N'C',0,3),(@item,N'Checking whether the plan works',N'D',0,4),(@item,N'Opening a second building',N'E',1,5),(@item,N'Learning from other cities',N'F',0,6);

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Read the email about a cancelled workshop and choose the best answer for each question.',N'Part 1/4 Reading comprehension. Use information from the whole message.','SINGLE_CHOICE','READING',1,'B2','HARD','PUBLISHED');
SET @r5=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@r5,N'Why was the workshop moved?',2,1),(@r5,N'What does the organiser offer first?',1,2),(@r5,N'What should participants do by Friday?',3,3),(@r5,N'Which tone does the email use?',2,4),(@r5,N'What is the writer still hoping to do?',1,5);
SELECT @item=id FROM question_items WHERE question_id=@r5 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'The teacher was ill',N'A',0,1),(@item,N'The venue was unavailable',N'B',1,2),(@item,N'Too few people registered',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@r5 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'An alternative date',N'A',1,1),(@item,N'A different tutor',N'B',0,2),(@item,N'A discount on books',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@r5 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Pay a second fee',N'A',0,1),(@item,N'Bring a printed ticket',N'B',0,2),(@item,N'Confirm their preferred option',N'C',1,3);
SELECT @item=id FROM question_items WHERE question_id=@r5 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Angry and threatening',N'A',0,1),(@item,N'Polite but concerned',N'B',1,2),(@item,N'Completely informal',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@r5 AND position=5; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Attend the course',N'A',1,1),(@item,N'Cancel all future courses',N'B',0,2),(@item,N'Become a course organiser',N'C',0,3);

/* Listening — all five tasks have authenticated audio and a two-play limit. */
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Listen to a short announcement and identify the exact information.',N'Part 1 Information recognition. You can listen twice.','SINGLE_CHOICE','LISTENING',1,'A2','MEDIUM','PUBLISHED'); SET @l1=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@l1,N'What time does the meeting begin?',2,1),(@l1,N'Where should visitors wait?',1,2),(@l1,N'What should people bring?',3,3);
SELECT @item=id FROM question_items WHERE question_id=@l1 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'9:00',N'A',0,1),(@item,N'9:30',N'B',1,2),(@item,N'10:00',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@l1 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'At the main desk',N'A',1,1),(@item,N'Outside the café',N'B',0,2),(@item,N'In the car park',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@l1 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'A passport',N'A',0,1),(@item,N'A laptop',N'B',0,2),(@item,N'The meeting notes',N'C',1,3);
INSERT INTO question_media(question_id,media_type,media_url,original_filename,content_type,file_size,max_play_count,position,alt_text,transcript) VALUES(@l1,'AUDIO',N'curriculum/audio/listening-a2.wav',N'aptis-listening-part-1.wav','audio/wav',1099246,2,1,N'Announcement about a meeting time and place',N'The meeting begins at half past nine. Please wait at the main desk and bring the meeting notes.');

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Listen to four speakers and match each person with the reason for joining a course.',N'Part 2 Information matching. You can listen twice.','MATCHING','LISTENING',2,'B1','MEDIUM','PUBLISHED'); SET @l2=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@l2,N'Anna chose the course because she wants to...',1,1),(@l2,N'Ben chose the course because he wants to...',2,2),(@l2,N'Cara chose the course because she wants to...',3,3),(@l2,N'Dan chose the course because he wants to...',4,4);
SELECT @item=id FROM question_items WHERE question_id=@l2 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'lead meetings confidently',N'A',1,1),(@item,N'work with clay',N'B',0,2),(@item,N'meet people nearby',N'C',0,3),(@item,N'learn to repair bikes',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@l2 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'lead meetings confidently',N'A',0,1),(@item,N'work with clay',N'B',1,2),(@item,N'meet people nearby',N'C',0,3),(@item,N'learn to repair bikes',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@l2 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'lead meetings confidently',N'A',0,1),(@item,N'work with clay',N'B',0,2),(@item,N'meet people nearby',N'C',1,3),(@item,N'learn to repair bikes',N'D',0,4);
SELECT @item=id FROM question_items WHERE question_id=@l2 AND position=4; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'lead meetings confidently',N'A',0,1),(@item,N'work with clay',N'B',0,2),(@item,N'meet people nearby',N'C',0,3),(@item,N'learn to repair bikes',N'D',1,4);
INSERT INTO question_media(question_id,media_type,media_url,original_filename,content_type,file_size,max_play_count,position,alt_text,transcript) VALUES(@l2,'AUDIO',N'curriculum/audio/listening-b1.wav',N'aptis-listening-part-2.wav','audio/wav',1458446,2,1,N'Four people explain why they joined a course',N'Anna chose public speaking to lead meetings confidently. Ben chose pottery because he wanted to work with his hands. Cara joined a cooking course mainly to meet people nearby. Dan chose a repair workshop to learn practical skills.');

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Listen to a discussion and identify the speakers'' attitude and intention.',N'Part 3 Inference. You can listen twice.','SINGLE_CHOICE','LISTENING',3,'B2','HARD','PUBLISHED'); SET @l3=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@l3,N'What does the first speaker mainly support?',2,1),(@l3,N'What is the main concern about the proposal?',3,2),(@l3,N'What solution do they finally suggest?',1,3);
SELECT @item=id FROM question_items WHERE question_id=@l3 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Closing the office permanently',N'A',0,1),(@item,N'Testing flexible working carefully',N'B',1,2),(@item,N'Increasing daily travel',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@l3 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'People may lose informal support',N'A',0,1),(@item,N'The building is too large',N'B',0,2),(@item,N'The pilot is too short to plan',N'C',1,3);
SELECT @item=id FROM question_items WHERE question_id=@l3 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Run a limited pilot and collect evidence',N'A',1,1),(@item,N'Cancel all online meetings',N'B',0,2),(@item,N'Adopt the plan without review',N'C',0,3);
INSERT INTO question_media(question_id,media_type,media_url,original_filename,content_type,file_size,max_play_count,position,alt_text,transcript) VALUES(@l3,'AUDIO',N'curriculum/audio/listening-b2.wav',N'aptis-listening-part-3.wav','audio/wav',2248340,2,1,N'Discussion about a flexible working pilot',N'Maya supports working from home but wants common office days. Daniel is concerned that new colleagues may struggle to get informal help. They agree to run a six-week pilot and collect feedback before making it permanent.');

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Listen to a commentary and infer the speaker''s viewpoint.',N'Part 4 Inference. You can listen twice.','SINGLE_CHOICE','LISTENING',4,'C1','HARD','PUBLISHED'); SET @l4=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@l4,N'How does the speaker view the new city policy?',1,1),(@l4,N'What evidence does the speaker say is still needed?',2,2),(@l4,N'What does the speaker recommend next?',3,3);
SELECT @item=id FROM question_items WHERE question_id=@l4 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Supportive but cautious',N'A',1,1),(@item,N'Completely dismissive',N'B',0,2),(@item,N'Certain it has failed',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@l4 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'More advertising',N'A',0,1),(@item,N'Whether journeys replaced car trips',N'B',1,2),(@item,N'A larger office',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@l4 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Wait for a longer comparison',N'A',0,1),(@item,N'Remove the policy immediately',N'B',0,2),(@item,N'Compare evidence before deciding',N'C',1,3);
INSERT INTO question_media(question_id,media_type,media_url,original_filename,content_type,file_size,max_play_count,position,alt_text,transcript) VALUES(@l4,'AUDIO',N'curriculum/audio/listening-c1.wav',N'aptis-listening-part-4.wav','audio/wav',2951200,2,1,N'Commentary about evidence for a city policy',N'The speaker supports the aim but is cautious about claims based only on borrowing data. The speaker recommends comparing which journeys were replaced before reaching a verdict.');

INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Listen to a revised arrangement and identify the information that changed.',N'Part 3 Information and inference. You can listen twice.','SINGLE_CHOICE','LISTENING',3,'B1','MEDIUM','PUBLISHED'); SET @l5=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,correct_position,position) VALUES(@l5,N'Where will the speakers meet?',2,1),(@l5,N'Why did they change the plan?',1,2),(@l5,N'What will they do after meeting?',3,3);
SELECT @item=id FROM question_items WHERE question_id=@l5 AND position=1; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'At the station',N'A',0,1),(@item,N'Near the café',N'B',1,2),(@item,N'At the library',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@l5 AND position=2; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'The first place was closed',N'A',1,1),(@item,N'The train was cancelled',N'B',0,2),(@item,N'They forgot the address',N'C',0,3);
SELECT @item=id FROM question_items WHERE question_id=@l5 AND position=3; INSERT INTO question_options(question_item_id,content,option_code,is_correct,position) VALUES(@item,N'Go straight home',N'A',0,1),(@item,N'Call the organiser',N'B',0,2),(@item,N'Walk to the library',N'C',1,3);
INSERT INTO question_media(question_id,media_type,media_url,original_filename,content_type,file_size,max_play_count,position,alt_text,transcript) VALUES(@l5,'AUDIO',N'curriculum/audio/listening-b1.wav',N'aptis-listening-part-3-revised.wav','audio/wav',1458446,2,1,N'Dialogue about a changed meeting place',N'They decide to meet near the café because the station entrance is closed. After meeting, they plan to walk to the library.');

/* Writing — four official task parts plus an extra extended practice task. */
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status)
VALUES(N'Answer five short messages from other members of a community club.',N'Part 1 Word-level writing. Use one word or a short phrase for each response.','WRITING_TEXT','WRITING',1,'A2','MEDIUM','PUBLISHED'); SET @w1=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,min_word_count,max_word_count) VALUES(@w1,N'What is your first name?',1,1,5),(@w1,N'Which day can you attend?',2,1,5),(@w1,N'What activity do you prefer?',3,1,8),(@w1,N'Who will come with you?',4,1,8),(@w1,N'What time is convenient?',5,1,8);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status) VALUES(N'Write a short message to the community club about joining a weekend event.',N'Part 2 Short text writing. Write 20–30 words and answer every point.','WRITING_TEXT','WRITING',2,'A2','MEDIUM','PUBLISHED'); SET @w2=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,min_word_count,max_word_count) VALUES(@w2,N'Say whether you can attend, explain one reason and ask one practical question.',1,20,30);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status) VALUES(N'Respond to three members discussing a local photography club.',N'Part 3 Social interaction. Write about 40 words for each response.','WRITING_TEXT','WRITING',3,'B1','MEDIUM','PUBLISHED'); SET @w3=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,min_word_count,max_word_count) VALUES(@w3,N'Reply to a member who asks what subjects you like photographing.',1,35,50),(@w3,N'Reply to a member who suggests a Saturday walk.',2,35,50),(@w3,N'Reply to a member who asks how beginners can improve.',3,35,50);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status) VALUES(N'Write two emails about a community centre changing its photography course timetable.',N'Part 4 Formal and informal writing. Write an informal email of 40–50 words and a formal email of 120–150 words.','WRITING_TEXT','WRITING',4,'B2','HARD','PUBLISHED'); SET @w4=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,min_word_count,max_word_count) VALUES(@w4,N'Write to a friend explaining how the change affects you and suggesting a solution.',1,40,50),(@w4,N'Write to the course coordinator explaining the problem and requesting a suitable alternative.',2,120,150);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,status) VALUES(N'Write a formal proposal to improve access to a community learning centre.',N'Extended Aptis-style practice. Write 120–150 words, compare two options and recommend one with reasons.','WRITING_TEXT','WRITING',4,'C1','HARD','PUBLISHED'); SET @w5=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,min_word_count,max_word_count) VALUES(@w5,N'Compare extending evening hours with creating an additional study room, then recommend a plan that could be evaluated after three months.',1,120,150);

/* Speaking — four official parts, split into five reusable question records. */
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,preparation_seconds,status) VALUES(N'Answer questions about yourself and your interests.',N'Part 1 Sentence comprehension. Record about 30 seconds for each item.','SPEAKING_AUDIO','SPEAKING',1,'A2','MEDIUM',15,'PUBLISHED'); SET @s1=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,response_seconds) VALUES(@s1,N'Where do you live and what do you like about the place?',1,30),(@s1,N'What do you usually do at weekends?',2,30),(@s1,N'What skill would you like to improve?',3,30);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,preparation_seconds,status) VALUES(N'Describe a photograph of people working together in a community garden.',N'Part 2 Describe, express an opinion and give reasons. Record about 45 seconds for each item.','SPEAKING_AUDIO','SPEAKING',2,'A2','MEDIUM',30,'PUBLISHED'); SET @s2=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,response_seconds) VALUES(@s2,N'Describe what the people are doing and where they are.',1,45),(@s2,N'Would you enjoy joining this activity? Why or why not?',2,45),(@s2,N'Why are community activities useful?',3,45);
INSERT INTO question_media(question_id,media_type,media_url,original_filename,content_type,file_size,max_play_count,position,alt_text,transcript) VALUES(@s2,'IMAGE',N'curriculum/images/speaking-community.svg',N'speaking-community.svg','image/svg+xml',1800,NULL,1,N'People working together in a community garden',NULL);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,preparation_seconds,status) VALUES(N'Compare two photographs showing different ways of studying.',N'Part 3 Describe, compare and give reasons. Record about 45 seconds for each item.','SPEAKING_AUDIO','SPEAKING',3,'B1','MEDIUM',30,'PUBLISHED'); SET @s3=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,response_seconds) VALUES(@s3,N'Describe the main differences between the two scenes.',1,45),(@s3,N'Which way of studying would suit you better?',2,45),(@s3,N'How might the two learners feel in these situations?',3,45);
INSERT INTO question_media(question_id,media_type,media_url,original_filename,content_type,file_size,max_play_count,position,alt_text,transcript) VALUES(@s3,'IMAGE',N'curriculum/images/speaking-library.svg',N'speaking-library.svg','image/svg+xml',1800,NULL,1,N'Student studying in a library',NULL),(@s3,'IMAGE',N'curriculum/images/speaking-park.svg',N'speaking-park.svg','image/svg+xml',1800,NULL,2,N'Student studying outdoors in a park',NULL);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,preparation_seconds,status) VALUES(N'Discuss whether towns should provide more free activities for young adults.',N'Part 4 Discuss an abstract topic. Prepare for one minute and speak for up to two minutes.','SPEAKING_AUDIO','SPEAKING',4,'B2','HARD',60,'PUBLISHED'); SET @s4=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,response_seconds) VALUES(@s4,N'What free activities would be most useful?',1,120),(@s4,N'How could such activities benefit a town?',2,120),(@s4,N'What problems might organisers need to solve?',3,120);
INSERT INTO questions(content,instruction,question_type,skill,part_no,cefr_level,difficulty,preparation_seconds,status) VALUES(N'Discuss the advantages and risks of using technology in community learning.',N'Part 4 Extended discussion. Prepare for one minute and speak for up to two minutes.','SPEAKING_AUDIO','SPEAKING',4,'C1','HARD',60,'PUBLISHED'); SET @s5=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO question_items(question_id,content,position,response_seconds) VALUES(@s5,N'How can technology make learning more accessible?',1,120),(@s5,N'When can technology reduce the quality of learning?',2,120),(@s5,N'What balance should a learning centre aim for?',3,120);

/* Five questions per skill are attached to every existing checkpoint. */
DECLARE @question_group TABLE(skill VARCHAR(30),question_id INT,position INT);
INSERT INTO @question_group VALUES
('GRAMMAR_VOCABULARY',@gv1,1),('GRAMMAR_VOCABULARY',@gv2,2),('GRAMMAR_VOCABULARY',@gv3,3),('GRAMMAR_VOCABULARY',@gv4,4),('GRAMMAR_VOCABULARY',@gv5,5),
('READING',@r1,1),('READING',@r2,2),('READING',@r3,3),('READING',@r4,4),('READING',@r5,5),
('LISTENING',@l1,1),('LISTENING',@l2,2),('LISTENING',@l3,3),('LISTENING',@l4,4),('LISTENING',@l5,5),
('WRITING',@w1,1),('WRITING',@w2,2),('WRITING',@w3,3),('WRITING',@w4,4),('WRITING',@w5,5),
('SPEAKING',@s1,1),('SPEAKING',@s2,2),('SPEAKING',@s3,3),('SPEAKING',@s4,4),('SPEAKING',@s5,5);
INSERT INTO mock_exam_questions(mock_exam_id,mock_exam_section_id,question_id,position)
SELECT e.id, s.id, g.question_id, g.position + 1
FROM mock_exams e
JOIN mock_exam_sections s ON s.mock_exam_id=e.id AND s.skill=e.skill
JOIN @question_group g ON g.skill=e.skill
WHERE e.exam_type='CHECKPOINT' AND e.status='PUBLISHED'
  AND NOT EXISTS(SELECT 1 FROM mock_exam_questions x WHERE x.mock_exam_id=e.id AND x.question_id=g.question_id);

/* New skill practice sets use the same authored question bank. */
DECLARE @exam_seed TABLE(title NVARCHAR(255), description NVARCHAR(MAX), exam_type VARCHAR(30), skill VARCHAR(30), target_level VARCHAR(10), duration_minutes INT);
INSERT INTO @exam_seed VALUES
(N'Aptis Grammar and Vocabulary Practice B1',N'50-task core practice: grammar completion and vocabulary matching/usage.',N'PRACTICE',N'GRAMMAR_VOCABULARY',N'B1',25),
(N'Aptis Reading Practice B1',N'Practice sentence comprehension, text cohesion, opinion matching and headings.',N'PRACTICE',N'READING',N'B1',35),
(N'Aptis Listening Practice B1',N'Practice information recognition, matching and inference with two-play audio.',N'PRACTICE',N'LISTENING',N'B1',40),
(N'Aptis Writing Practice B1',N'Four-part writing practice: short answers, interaction and two emails.',N'PRACTICE',N'WRITING',N'B1',50),
(N'Aptis Speaking Practice B1',N'Four-part speaking practice with picture prompts and extended discussion.',N'PRACTICE',N'SPEAKING',N'B1',12),
(N'Aptis Placement Grammar and Vocabulary B1',N'Placement version of the Aptis core task set.',N'PLACEMENT',N'GRAMMAR_VOCABULARY',N'B1',25),
(N'Aptis Placement Reading B1',N'Placement version of the four Reading task parts.',N'PLACEMENT',N'READING',N'B1',35),
(N'Aptis Placement Listening B1',N'Placement version with authenticated two-play listening tasks.',N'PLACEMENT',N'LISTENING',N'B1',40),
(N'Aptis Placement Writing B1',N'Placement version of the four Writing task parts.',N'PLACEMENT',N'WRITING',N'B1',50),
(N'Aptis Placement Speaking B1',N'Placement version of the four Speaking task parts.',N'PLACEMENT',N'SPEAKING',N'B1',12);
INSERT INTO mock_exams(certificate_id,scoring_scheme_id,title,description,exam_type,target_level,skill,duration_minutes,status)
SELECT @certificate_id,@scheme_id,title,description,exam_type,target_level,skill,duration_minutes,N'PUBLISHED' FROM @exam_seed;
INSERT INTO mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status)
SELECT e.id,e.skill,1,e.duration_minutes,N'ACTIVE' FROM mock_exams e JOIN @exam_seed s ON s.title=e.title;
INSERT INTO mock_exam_questions(mock_exam_id,mock_exam_section_id,question_id,position)
SELECT e.id,sec.id,g.question_id,g.position
FROM mock_exams e JOIN @exam_seed seed ON seed.title=e.title
JOIN mock_exam_sections sec ON sec.mock_exam_id=e.id
JOIN @question_group g ON g.skill=seed.skill;

/* A full Aptis-style mock with all five components and official maximum timings. */
DECLARE @full_mock_id INT, @fs_gv INT, @fs_r INT, @fs_l INT, @fs_w INT, @fs_s INT;
INSERT INTO mock_exams(certificate_id,scoring_scheme_id,title,description,exam_type,target_level,skill,duration_minutes,status)
VALUES(@certificate_id,@scheme_id,N'Aptis General Full Mock — Format Practice B1',N'Original Aptis-style full mock: core 25 minutes, Reading 35, Listening 40, Writing 50 and Speaking 12.',N'FULL_MOCK',N'B1',N'FULL_MOCK',162,N'PUBLISHED');
SET @full_mock_id=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status) VALUES(@full_mock_id,N'GRAMMAR_VOCABULARY',1,25,N'ACTIVE'); SET @fs_gv=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status) VALUES(@full_mock_id,N'READING',2,35,N'ACTIVE'); SET @fs_r=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status) VALUES(@full_mock_id,N'LISTENING',3,40,N'ACTIVE'); SET @fs_l=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status) VALUES(@full_mock_id,N'WRITING',4,50,N'ACTIVE'); SET @fs_w=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status) VALUES(@full_mock_id,N'SPEAKING',5,12,N'ACTIVE'); SET @fs_s=CONVERT(INT,SCOPE_IDENTITY());
INSERT INTO mock_exam_questions(mock_exam_id,mock_exam_section_id,question_id,position)
SELECT @full_mock_id,@fs_gv,question_id,position FROM @question_group WHERE skill=N'GRAMMAR_VOCABULARY'
UNION ALL SELECT @full_mock_id,@fs_r,question_id,position+5 FROM @question_group WHERE skill=N'READING'
UNION ALL SELECT @full_mock_id,@fs_l,question_id,position+10 FROM @question_group WHERE skill=N'LISTENING'
UNION ALL SELECT @full_mock_id,@fs_w,question_id,position+15 FROM @question_group WHERE skill=N'WRITING'
UNION ALL SELECT @full_mock_id,@fs_s,question_id,position+20 FROM @question_group WHERE skill=N'SPEAKING';
