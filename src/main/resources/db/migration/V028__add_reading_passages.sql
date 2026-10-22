/* Store the source text separately from the task prompt so Reading attempts
   can show the passage while preserving the question and item structure. */
IF COL_LENGTH('questions', 'reading_passage') IS NULL
    ALTER TABLE questions ADD reading_passage NVARCHAR(MAX) NULL;

UPDATE questions
SET reading_passage = N'Town Centre Guide' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'The public library is on King Street and lends books to residents. The swimming pool is next to the sports centre; its first lesson begins at six o''clock. The Riverside Café is opposite the bus station and is a popular place to meet friends for coffee.'
WHERE skill = 'READING'
  AND content = N'Match each description with the correct place.'
  AND (reading_passage IS NULL OR LTRIM(RTRIM(reading_passage)) = '');

UPDATE questions
SET reading_passage = N'Reading a short text' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'When you answer a reading question, first identify exactly what the question is asking. Then scan the text for key information rather than reading every sentence at the same speed. Finally, check your answer against the evidence in the text before moving on.'
WHERE skill = 'READING'
  AND content = N'Put the reading strategy steps in the correct order.'
  AND (reading_passage IS NULL OR LTRIM(RTRIM(reading_passage)) = '');

UPDATE questions
SET reading_passage = N'The Riverside Community Garden is open to local residents every Saturday and Sunday. Members grow vegetables, share basic gardening tools and take turns looking after the beds. New volunteers can join at any time by speaking to the coordinator. The next planning meeting will be held on Thursday evening at 6:30 in the small meeting room. Anyone who needs more information can ask at the main gate.'
WHERE skill = 'READING'
  AND content = N'Read the notice about the community garden and choose the best word for each sentence.'
  AND (reading_passage IS NULL OR LTRIM(RTRIM(reading_passage)) = '');

UPDATE questions
SET reading_passage = N'Repair Café: how a visit works' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'At the monthly repair café, visitors bring a broken household item and explain the problem to a volunteer. The volunteer checks whether the repair is safe, and then both people choose tools from the shared shelves. They work together and discuss what caused the problem. Before leaving, the visitor tests the item and records the result. This simple process keeps useful objects in use for longer.'
WHERE skill = 'READING'
  AND content = N'Put the sentences into the correct order to make a coherent text about a repair café.'
  AND (reading_passage IS NULL OR LTRIM(RTRIM(reading_passage)) = '');

UPDATE questions
SET reading_passage = N'Evening classes: four learners' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'Ana: I learn best when the teacher gives me immediate feedback, especially when I am practising pronunciation.' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'Ben: I chose my evening class mainly because it gives me a chance to meet people from the area.' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'Chi: My work timetable changes every week, so I need a course with flexible sessions.' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'Diego: Online lessons are useful, but I sometimes worry that studying alone can feel too isolated. I record difficult explanations and review them later.'
WHERE skill = 'READING'
  AND content = N'Read four opinions about evening classes and match each statement with the correct person.'
  AND (reading_passage IS NULL OR LTRIM(RTRIM(reading_passage)) = '');

UPDATE questions
SET reading_passage = N'Libraries for changing communities' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'Many libraries now design services with local residents instead of assuming that every neighbourhood needs the same support. Some branches open later so shift workers can use them. Others offer short sessions that help visitors judge whether information on the internet is reliable. Shared work tables provide a quiet place for freelancers who cannot work at home. Managers review these programmes by comparing attendance figures with comments from users.'
WHERE skill = 'READING'
  AND content = N'Read a short article about libraries changing their services and match each paragraph with the best heading.'
  AND (reading_passage IS NULL OR LTRIM(RTRIM(reading_passage)) = '');

UPDATE questions
SET reading_passage = N'Subject: Photography workshop moved to 18 May' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'Dear participants,' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'I am sorry to tell you that the photography workshop cannot take place in the community centre this Saturday because the venue is unavailable. We can offer the same workshop on Saturday 18 May, or we can refund your booking. Please reply by Friday to confirm which option you prefer. We are still hoping to run the outdoor practice session if the weather remains suitable.' + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10) + N'Best wishes,' + NCHAR(13) + NCHAR(10) + N'Mina' + NCHAR(13) + NCHAR(10) + N'Workshop organiser'
WHERE skill = 'READING'
  AND content = N'Read the email about a cancelled workshop and choose the best answer for each question.'
  AND (reading_passage IS NULL OR LTRIM(RTRIM(reading_passage)) = '');
