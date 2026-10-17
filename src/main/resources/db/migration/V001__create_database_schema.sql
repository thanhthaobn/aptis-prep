CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    full_name NVARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE certificates (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description NVARCHAR(MAX) NULL,
    status VARCHAR(30) NOT NULL,
    created_by INT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE scoring_schemes (
    id INT IDENTITY(1,1) PRIMARY KEY,
    certificate_id INT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    version VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    effective_from DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE score_bands (
    id INT IDENTITY(1,1) PRIMARY KEY,
    scoring_scheme_id INT NOT NULL,
    skill VARCHAR(30) NOT NULL,
    cefr_level VARCHAR(10) NOT NULL,
    min_score DECIMAL(5,2) NOT NULL,
    max_score DECIMAL(5,2) NOT NULL
);

CREATE TABLE questions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    content NVARCHAR(MAX) NOT NULL,
    instruction NVARCHAR(MAX) NULL,
    question_type VARCHAR(30) NOT NULL,
    skill VARCHAR(30) NOT NULL,
    part_no INT NULL,
    cefr_level VARCHAR(10) NULL,
    difficulty VARCHAR(30) NULL,
    allow_option_reuse BIT NOT NULL DEFAULT 1,
    preparation_seconds INT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE question_items (
    id INT IDENTITY(1,1) PRIMARY KEY,
    question_id INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    correct_position INT NULL,
    display_order INT NOT NULL,
    min_word_count INT NULL,
    max_word_count INT NULL,
    response_seconds INT NULL
);

CREATE TABLE question_options (
    id INT IDENTITY(1,1) PRIMARY KEY,
    question_item_id INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    option_code VARCHAR(50) NULL,
    is_correct BIT NULL,
    display_order INT NOT NULL
);

CREATE TABLE question_media (
    id INT IDENTITY(1,1) PRIMARY KEY,
    question_id INT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    file_path NVARCHAR(500) NOT NULL,
    original_filename NVARCHAR(255) NULL,
    content_type VARCHAR(100) NULL,
    file_size BIGINT NULL,
    max_play_count INT NULL,
    display_order INT NOT NULL,
    alt_text NVARCHAR(500) NULL,
    transcript NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE mock_exams (
    id INT IDENTITY(1,1) PRIMARY KEY,
    certificate_id INT NOT NULL,
    scoring_scheme_id INT NULL,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    exam_type VARCHAR(30) NOT NULL,
    target_level VARCHAR(10) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE mock_exam_sections (
    id INT IDENTITY(1,1) PRIMARY KEY,
    mock_exam_id INT NOT NULL,
    skill VARCHAR(30) NOT NULL,
    display_order INT NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE mock_exam_questions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    mock_exam_section_id INT NOT NULL,
    question_id INT NOT NULL,
    display_order INT NOT NULL
);

CREATE TABLE exam_attempts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    mock_exam_id INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    grading_status VARCHAR(30) NULL,
    started_at DATETIME2 NOT NULL,
    submitted_at DATETIME2 NULL,
    completed_at DATETIME2 NULL,
    final_scale_score DECIMAL(6,2) NULL,
    estimated_overall_cefr VARCHAR(10) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE user_answers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    exam_attempt_id INT NOT NULL,
    question_id INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at DATETIME2 NULL,
    completed_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE user_answer_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_answer_id INT NOT NULL,
    question_item_id INT NOT NULL,
    selected_option_id INT NULL,
    user_position INT NULL,
    answer_text NVARCHAR(MAX) NULL,
    audio_path NVARCHAR(500) NULL,
    transcript NVARCHAR(MAX) NULL,
    recording_duration_seconds INT NULL,
    auto_score DECIMAL(5,2) NULL,
    feedback NVARCHAR(MAX) NULL,
    grading_status VARCHAR(30) NULL,
    graded_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE user_media_activities (
    id INT IDENTITY(1,1) PRIMARY KEY,
    exam_attempt_id INT NOT NULL,
    question_media_id INT NOT NULL,
    play_count INT NOT NULL DEFAULT 0,
    last_played_at DATETIME2 NULL
);

CREATE TABLE exam_skill_results (
    id INT IDENTITY(1,1) PRIMARY KEY,
    exam_attempt_id INT NOT NULL,
    skill VARCHAR(30) NOT NULL,
    correct_count INT NULL,
    total_items INT NULL,
    raw_score DECIMAL(6,2) NULL,
    scale_score DECIMAL(5,2) NULL,
    cefr_level VARCHAR(10) NULL,
    grading_method VARCHAR(30) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE user_skill_profiles (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    skill VARCHAR(30) NOT NULL,
    current_level VARCHAR(10) NOT NULL,
    source_attempt_id INT NULL,
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE user_goals (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    certificate_id INT NULL,
    target_date DATE NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE user_goal_skills (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_goal_id INT NOT NULL,
    skill VARCHAR(30) NOT NULL,
    target_level VARCHAR(10) NOT NULL,
    target_score DECIMAL(5,2) NULL
);

CREATE TABLE lessons (
    id INT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    skill VARCHAR(30) NOT NULL,
    cefr_level VARCHAR(10) NOT NULL,
    video_path NVARCHAR(500) NULL,
    display_order INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE lesson_progress (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    lesson_id INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    last_accessed_at DATETIME2 NULL,
    completed_at DATETIME2 NULL
);

CREATE TABLE flashcards (
    id INT IDENTITY(1,1) PRIMARY KEY,
    lesson_id INT NOT NULL,
    front_content NVARCHAR(MAX) NOT NULL,
    back_content NVARCHAR(MAX) NOT NULL,
    cefr_level VARCHAR(10) NULL,
    display_order INT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE user_flashcard_progress (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    flashcard_id INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    mastery_level INT NULL,
    review_count INT NOT NULL DEFAULT 0,
    last_reviewed_at DATETIME2 NULL
);

CREATE TABLE learning_paths (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    goal_skill_id INT NULL,
    source_attempt_id INT NULL,
    skill VARCHAR(30) NOT NULL,
    from_level VARCHAR(10) NULL,
    target_level VARCHAR(10) NOT NULL,
    path_source VARCHAR(30) NOT NULL,
    checkpoint_exam_id INT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    completed_at DATETIME2 NULL
);

CREATE TABLE learning_path_items (
    id INT IDENTITY(1,1) PRIMARY KEY,
    learning_path_id INT NOT NULL,
    lesson_id INT NOT NULL,
    display_order INT NOT NULL,
    is_required BIT NOT NULL DEFAULT 1
);
