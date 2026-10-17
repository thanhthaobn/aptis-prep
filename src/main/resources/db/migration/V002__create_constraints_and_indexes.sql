ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'ADMIN'));
ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

ALTER TABLE certificates ADD CONSTRAINT uq_certificates_code UNIQUE (code);
ALTER TABLE certificates ADD CONSTRAINT fk_certificates_created_by FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE scoring_schemes ADD CONSTRAINT fk_scoring_schemes_certificate FOREIGN KEY (certificate_id) REFERENCES certificates(id);
ALTER TABLE scoring_schemes ADD CONSTRAINT uq_scoring_schemes_version UNIQUE (certificate_id, version);

ALTER TABLE score_bands ADD CONSTRAINT fk_score_bands_scheme FOREIGN KEY (scoring_scheme_id) REFERENCES scoring_schemes(id);
ALTER TABLE score_bands ADD CONSTRAINT uq_score_bands_level UNIQUE (scoring_scheme_id, skill, cefr_level);
ALTER TABLE score_bands ADD CONSTRAINT ck_score_bands_range CHECK (min_score <= max_score AND min_score >= 0 AND max_score <= 50);

ALTER TABLE questions ADD CONSTRAINT ck_questions_part CHECK (part_no IS NULL OR part_no BETWEEN 1 AND 4);
ALTER TABLE questions ADD CONSTRAINT ck_questions_preparation CHECK (preparation_seconds IS NULL OR preparation_seconds >= 0);

ALTER TABLE question_items ADD CONSTRAINT fk_question_items_question FOREIGN KEY (question_id) REFERENCES questions(id);
ALTER TABLE question_items ADD CONSTRAINT uq_question_items_order UNIQUE (question_id, display_order);
ALTER TABLE question_items ADD CONSTRAINT ck_question_items_order CHECK (display_order > 0);
ALTER TABLE question_items ADD CONSTRAINT ck_question_items_position CHECK (correct_position IS NULL OR correct_position > 0);
ALTER TABLE question_items ADD CONSTRAINT ck_question_items_words CHECK (min_word_count IS NULL OR max_word_count IS NULL OR min_word_count <= max_word_count);

ALTER TABLE question_options ADD CONSTRAINT fk_question_options_item FOREIGN KEY (question_item_id) REFERENCES question_items(id);
ALTER TABLE question_options ADD CONSTRAINT uq_question_options_order UNIQUE (question_item_id, display_order);
ALTER TABLE question_options ADD CONSTRAINT uq_question_options_id_item UNIQUE (id, question_item_id);
ALTER TABLE question_options ADD CONSTRAINT ck_question_options_order CHECK (display_order > 0);

ALTER TABLE question_media ADD CONSTRAINT fk_question_media_question FOREIGN KEY (question_id) REFERENCES questions(id);
ALTER TABLE question_media ADD CONSTRAINT uq_question_media_order UNIQUE (question_id, display_order);
ALTER TABLE question_media ADD CONSTRAINT ck_question_media_type CHECK (media_type IN ('IMAGE', 'AUDIO', 'VIDEO'));
ALTER TABLE question_media ADD CONSTRAINT ck_question_media_size CHECK (file_size IS NULL OR file_size >= 0);
ALTER TABLE question_media ADD CONSTRAINT ck_question_media_max_play CHECK (max_play_count IS NULL OR max_play_count > 0);

ALTER TABLE mock_exams ADD CONSTRAINT fk_mock_exams_certificate FOREIGN KEY (certificate_id) REFERENCES certificates(id);
ALTER TABLE mock_exams ADD CONSTRAINT fk_mock_exams_scheme FOREIGN KEY (scoring_scheme_id) REFERENCES scoring_schemes(id);
ALTER TABLE mock_exams ADD CONSTRAINT ck_mock_exams_type CHECK (exam_type IN ('PLACEMENT', 'PRACTICE', 'CHECKPOINT', 'FULL_MOCK'));

ALTER TABLE mock_exam_sections ADD CONSTRAINT fk_exam_sections_exam FOREIGN KEY (mock_exam_id) REFERENCES mock_exams(id);
ALTER TABLE mock_exam_sections ADD CONSTRAINT uq_exam_sections_skill UNIQUE (mock_exam_id, skill);
ALTER TABLE mock_exam_sections ADD CONSTRAINT uq_exam_sections_order UNIQUE (mock_exam_id, display_order);
ALTER TABLE mock_exam_sections ADD CONSTRAINT ck_exam_sections_duration CHECK (duration_minutes > 0);

ALTER TABLE mock_exam_questions ADD CONSTRAINT fk_exam_questions_section FOREIGN KEY (mock_exam_section_id) REFERENCES mock_exam_sections(id);
ALTER TABLE mock_exam_questions ADD CONSTRAINT fk_exam_questions_question FOREIGN KEY (question_id) REFERENCES questions(id);
ALTER TABLE mock_exam_questions ADD CONSTRAINT uq_exam_questions_question UNIQUE (mock_exam_section_id, question_id);
ALTER TABLE mock_exam_questions ADD CONSTRAINT uq_exam_questions_order UNIQUE (mock_exam_section_id, display_order);

ALTER TABLE exam_attempts ADD CONSTRAINT fk_exam_attempts_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE exam_attempts ADD CONSTRAINT fk_exam_attempts_exam FOREIGN KEY (mock_exam_id) REFERENCES mock_exams(id);
ALTER TABLE exam_attempts ADD CONSTRAINT ck_exam_attempts_score CHECK (final_scale_score IS NULL OR final_scale_score BETWEEN 0 AND 200);

ALTER TABLE user_answers ADD CONSTRAINT fk_user_answers_attempt FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id);
ALTER TABLE user_answers ADD CONSTRAINT fk_user_answers_question FOREIGN KEY (question_id) REFERENCES questions(id);
ALTER TABLE user_answers ADD CONSTRAINT uq_user_answers_attempt_question UNIQUE (exam_attempt_id, question_id);

ALTER TABLE user_answer_details ADD CONSTRAINT fk_answer_details_answer FOREIGN KEY (user_answer_id) REFERENCES user_answers(id);
ALTER TABLE user_answer_details ADD CONSTRAINT fk_answer_details_item FOREIGN KEY (question_item_id) REFERENCES question_items(id);
ALTER TABLE user_answer_details ADD CONSTRAINT fk_answer_details_selected_option FOREIGN KEY (selected_option_id, question_item_id) REFERENCES question_options(id, question_item_id);
ALTER TABLE user_answer_details ADD CONSTRAINT uq_answer_details_answer_item UNIQUE (user_answer_id, question_item_id);
ALTER TABLE user_answer_details ADD CONSTRAINT ck_answer_details_score CHECK (auto_score IS NULL OR auto_score BETWEEN 0 AND 50);

ALTER TABLE user_media_activities ADD CONSTRAINT fk_media_activities_attempt FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id);
ALTER TABLE user_media_activities ADD CONSTRAINT fk_media_activities_media FOREIGN KEY (question_media_id) REFERENCES question_media(id);
ALTER TABLE user_media_activities ADD CONSTRAINT uq_media_activities_attempt_media UNIQUE (exam_attempt_id, question_media_id);
ALTER TABLE user_media_activities ADD CONSTRAINT ck_media_activities_count CHECK (play_count BETWEEN 0 AND 2);

ALTER TABLE exam_skill_results ADD CONSTRAINT fk_skill_results_attempt FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempts(id);
ALTER TABLE exam_skill_results ADD CONSTRAINT uq_skill_results_attempt_skill UNIQUE (exam_attempt_id, skill);
ALTER TABLE exam_skill_results ADD CONSTRAINT ck_skill_results_counts CHECK ((correct_count IS NULL OR correct_count >= 0) AND (total_items IS NULL OR total_items >= 0));
ALTER TABLE exam_skill_results ADD CONSTRAINT ck_skill_results_scale CHECK (scale_score IS NULL OR scale_score BETWEEN 0 AND 50);

ALTER TABLE user_skill_profiles ADD CONSTRAINT fk_skill_profiles_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_skill_profiles ADD CONSTRAINT fk_skill_profiles_attempt FOREIGN KEY (source_attempt_id) REFERENCES exam_attempts(id);
ALTER TABLE user_skill_profiles ADD CONSTRAINT uq_skill_profiles_user_skill UNIQUE (user_id, skill);

ALTER TABLE user_goals ADD CONSTRAINT fk_user_goals_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_goals ADD CONSTRAINT fk_user_goals_certificate FOREIGN KEY (certificate_id) REFERENCES certificates(id);

ALTER TABLE user_goal_skills ADD CONSTRAINT fk_goal_skills_goal FOREIGN KEY (user_goal_id) REFERENCES user_goals(id);
ALTER TABLE user_goal_skills ADD CONSTRAINT uq_goal_skills_goal_skill UNIQUE (user_goal_id, skill);
ALTER TABLE user_goal_skills ADD CONSTRAINT ck_goal_skills_score CHECK (target_score IS NULL OR target_score BETWEEN 0 AND 50);

ALTER TABLE lessons ADD CONSTRAINT uq_lessons_skill_order UNIQUE (skill, cefr_level, display_order);
ALTER TABLE lessons ADD CONSTRAINT ck_lessons_order CHECK (display_order > 0);

ALTER TABLE lesson_progress ADD CONSTRAINT fk_lesson_progress_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE lesson_progress ADD CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id);
ALTER TABLE lesson_progress ADD CONSTRAINT uq_lesson_progress_user_lesson UNIQUE (user_id, lesson_id);

ALTER TABLE flashcards ADD CONSTRAINT fk_flashcards_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id);
ALTER TABLE flashcards ADD CONSTRAINT uq_flashcards_lesson_order UNIQUE (lesson_id, display_order);
ALTER TABLE flashcards ADD CONSTRAINT ck_flashcards_order CHECK (display_order > 0);

ALTER TABLE user_flashcard_progress ADD CONSTRAINT fk_flashcard_progress_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_flashcard_progress ADD CONSTRAINT fk_flashcard_progress_card FOREIGN KEY (flashcard_id) REFERENCES flashcards(id);
ALTER TABLE user_flashcard_progress ADD CONSTRAINT uq_flashcard_progress_user_card UNIQUE (user_id, flashcard_id);
ALTER TABLE user_flashcard_progress ADD CONSTRAINT ck_flashcard_progress_mastery CHECK (mastery_level IS NULL OR mastery_level BETWEEN 0 AND 5);
ALTER TABLE user_flashcard_progress ADD CONSTRAINT ck_flashcard_progress_reviews CHECK (review_count >= 0);

ALTER TABLE learning_paths ADD CONSTRAINT fk_learning_paths_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE learning_paths ADD CONSTRAINT fk_learning_paths_goal_skill FOREIGN KEY (goal_skill_id) REFERENCES user_goal_skills(id);
ALTER TABLE learning_paths ADD CONSTRAINT fk_learning_paths_attempt FOREIGN KEY (source_attempt_id) REFERENCES exam_attempts(id);
ALTER TABLE learning_paths ADD CONSTRAINT fk_learning_paths_checkpoint FOREIGN KEY (checkpoint_exam_id) REFERENCES mock_exams(id);

ALTER TABLE learning_path_items ADD CONSTRAINT fk_path_items_path FOREIGN KEY (learning_path_id) REFERENCES learning_paths(id);
ALTER TABLE learning_path_items ADD CONSTRAINT fk_path_items_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id);
ALTER TABLE learning_path_items ADD CONSTRAINT uq_path_items_order UNIQUE (learning_path_id, display_order);
ALTER TABLE learning_path_items ADD CONSTRAINT uq_path_items_lesson UNIQUE (learning_path_id, lesson_id);
ALTER TABLE learning_path_items ADD CONSTRAINT ck_path_items_order CHECK (display_order > 0);

CREATE INDEX ix_certificates_created_by ON certificates(created_by);
CREATE INDEX ix_question_items_question ON question_items(question_id);
CREATE INDEX ix_question_options_item ON question_options(question_item_id);
CREATE INDEX ix_exam_attempts_user ON exam_attempts(user_id);
CREATE INDEX ix_exam_attempts_exam ON exam_attempts(mock_exam_id);
CREATE INDEX ix_user_answers_attempt ON user_answers(exam_attempt_id);
CREATE INDEX ix_media_activities_attempt ON user_media_activities(exam_attempt_id);
CREATE INDEX ix_lesson_progress_user ON lesson_progress(user_id);
CREATE INDEX ix_learning_paths_user ON learning_paths(user_id);
