-- AptisPrep: current SQL Server schema. No sample rows, credentials or migration history.
-- Select a NEW EMPTY database in SSMS before running this file.
-- Never execute this file against an existing application database.
IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)
    THROW 50001, 'Schema installation requires an empty database. Existing data was not changed.', 1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[attempt_outbox](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_key] [varchar](255) NOT NULL,
	[event_type] [varchar](80) NOT NULL,
	[aggregate_id] [int] NOT NULL,
	[payload] [nvarchar](max) NOT NULL,
	[status] [varchar](30) NOT NULL,
	[attempts] [int] NOT NULL,
	[next_attempt_at] [datetime2](7) NOT NULL,
	[last_error] [nvarchar](1000) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[processed_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_attempt_outbox_event_key] UNIQUE NONCLUSTERED
(
	[event_key] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lessons](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[title] [nvarchar](255) NOT NULL,
	[description] [nvarchar](max) NULL,
	[skill] [varchar](30) NOT NULL,
	[cefr_level] [varchar](10) NOT NULL,
	[video_path] [nvarchar](500) NULL,
	[display_order] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[topic] [nvarchar](160) NULL,
	[task_type] [nvarchar](160) NULL,
	[objectives] [nvarchar](max) NULL,
	[strategy] [nvarchar](max) NULL,
	[example_text] [nvarchar](max) NULL,
	[analysis] [nvarchar](max) NULL,
	[common_mistakes] [nvarchar](max) NULL,
	[practice_prompt] [nvarchar](max) NULL,
	[sample_response] [nvarchar](max) NULL,
	[checklist] [nvarchar](max) NULL,
	[video_url] [nvarchar](500) NULL,
	[source_url] [nvarchar](500) NULL,
	[source_name] [nvarchar](255) NULL,
	[audio_path] [nvarchar](500) NULL,
	[audio_name] [nvarchar](255) NULL,
	[transcript] [nvarchar](max) NULL,
	[min_word_count] [int] NULL,
	[max_word_count] [int] NULL,
	[image_path] [varchar](500) NULL,
	[image_name] [nvarchar](255) NULL,
	[content_blocks_json] [nvarchar](max) NULL,
	[transcript_visibility] [varchar](30) NULL,
	[sample_visibility] [varchar](30) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_lessons_skill_order] UNIQUE NONCLUSTERED
(
	[skill] ASC,
	[cefr_level] ASC,
	[display_order] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[users](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[full_name] [nvarchar](255) NOT NULL,
	[email] [varchar](255) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[role] [varchar](30) NOT NULL,
	[status] [varchar](30) NOT NULL,
	[lock_reason] [nvarchar](1000) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_users_email] UNIQUE NONCLUSTERED
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[questions](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[content] [nvarchar](max) NOT NULL,
	[instruction] [nvarchar](max) NULL,
	[question_type] [varchar](30) NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[part_no] [int] NULL,
	[cefr_level] [varchar](10) NULL,
	[difficulty] [varchar](30) NULL,
	[allow_option_reuse] [bit] NOT NULL,
	[preparation_seconds] [int] NULL,
	[status] [varchar](30) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[reading_passage] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[question_media](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[question_id] [int] NOT NULL,
	[media_type] [varchar](20) NOT NULL,
	[media_url] [nvarchar](500) NOT NULL,
	[original_filename] [nvarchar](255) NULL,
	[content_type] [varchar](100) NULL,
	[file_size] [bigint] NULL,
	[max_play_count] [int] NULL,
	[position] [int] NOT NULL,
	[alt_text] [nvarchar](500) NULL,
	[transcript] [nvarchar](max) NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_question_media_order] UNIQUE NONCLUSTERED
(
	[question_id] ASC,
	[position] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[question_items](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[question_id] [int] NOT NULL,
	[content] [nvarchar](max) NOT NULL,
	[correct_position] [int] NULL,
	[position] [int] NOT NULL,
	[points] [decimal](6, 2) NOT NULL CONSTRAINT [df_question_items_points] DEFAULT ((1)),
	[min_word_count] [int] NULL,
	[max_word_count] [int] NULL,
	[response_seconds] [int] NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_question_items_order] UNIQUE NONCLUSTERED
(
	[question_id] ASC,
	[position] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[flashcard_sets](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[title] [nvarchar](255) NOT NULL,
	[description] [nvarchar](2000) NULL,
	[cefr_level] [varchar](10) NOT NULL,
	[status] [varchar](30) NOT NULL,
	[owner_user_id] [int] NULL,
	[visibility] [varchar](20) NOT NULL,
	[lesson_id] [int] NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lesson_work_submissions](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[lesson_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[prompt_snapshot] [nvarchar](max) NULL,
	[response_text] [nvarchar](max) NULL,
	[audio_path] [varchar](500) NULL,
	[submitted_at] [datetime2](7) NOT NULL,
	[content_snapshot] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lesson_responses](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[lesson_id] [int] NOT NULL,
	[response_text] [nvarchar](max) NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[audio_path] [nvarchar](500) NULL,
	[audio_name] [nvarchar](255) NULL,
	[response_version] [int] NOT NULL,
	[assessment_key] [varchar](255) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_lesson_response_user_lesson] UNIQUE NONCLUSTERED
(
	[user_id] ASC,
	[lesson_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lesson_progress](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[lesson_id] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
	[last_accessed_at] [datetime2](7) NULL,
	[completed_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_lesson_progress_user_lesson] UNIQUE NONCLUSTERED
(
	[user_id] ASC,
	[lesson_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lesson_engagement](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[lesson_id] [int] NOT NULL,
	[audio_played_at] [datetime2](7) NULL,
	[last_section] [varchar](40) NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[playback_token_hash] [varchar](64) NULL,
	[playback_audio_path] [nvarchar](500) NULL,
	[playback_duration_seconds] [float] NULL,
	[playback_position_seconds] [float] NULL,
	[playback_last_ping_at] [datetime2](7) NULL,
	[playback_expires_at] [datetime2](7) NULL,
	[playback_ranges_json] [nvarchar](max) NULL,
	[playback_sequence] [int] NOT NULL,
	[playback_running] [bit] NOT NULL,
	[verified_audio_path] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_le_user_lesson] UNIQUE NONCLUSTERED
(
	[user_id] ASC,
	[lesson_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lesson_checks](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[lesson_id] [int] NOT NULL,
	[position] [int] NOT NULL,
	[prompt] [nvarchar](2000) NOT NULL,
	[option_a] [nvarchar](1000) NOT NULL,
	[option_b] [nvarchar](1000) NOT NULL,
	[option_c] [nvarchar](1000) NULL,
	[correct_option] [int] NOT NULL,
	[explanation] [nvarchar](4000) NOT NULL,
	[option_d] [nvarchar](1000) NULL,
	[option_e] [nvarchar](1000) NULL,
	[option_f] [nvarchar](1000) NULL,
	[block_key] [varchar](80) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_lesson_check_position] UNIQUE NONCLUSTERED
(
	[lesson_id] ASC,
	[position] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lesson_check_attempts](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[lesson_id] [int] NOT NULL,
	[score] [int] NOT NULL,
	[total] [int] NOT NULL,
	[answers_json] [nvarchar](max) NOT NULL,
	[submitted_at] [datetime2](7) NOT NULL,
	[review_json] [nvarchar](max) NULL,
	[content_snapshot] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[lesson_block_listens](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[lesson_id] [int] NOT NULL,
	[block_key] [varchar](80) NOT NULL,
	[media_path] [varchar](500) NOT NULL,
	[played_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_lesson_block_listens] UNIQUE NONCLUSTERED
(
	[user_id] ASC,
	[lesson_id] ASC,
	[block_key] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[certificates](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](255) NOT NULL,
	[code] [varchar](50) NOT NULL,
	[description] [nvarchar](max) NULL,
	[status] [varchar](30) NOT NULL,
	[created_by] [int] NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_certificates_code] UNIQUE NONCLUSTERED
(
	[code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[ai_assessment_jobs](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[assessment_key] [varchar](255) NOT NULL,
	[user_id] [int] NOT NULL,
	[attempt_id] [int] NULL,
	[answer_id] [int] NULL,
	[answer_version] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[task_type] [varchar](30) NOT NULL,
	[task_content] [nvarchar](max) NOT NULL,
	[locked_content] [nvarchar](max) NOT NULL,
	[rubric_version] [varchar](100) NOT NULL,
	[policy_version] [varchar](100) NOT NULL,
	[max_score] [decimal](5, 2) NOT NULL,
	[transcript] [nvarchar](max) NULL,
	[state] [varchar](30) NOT NULL,
	[retry_count] [int] NOT NULL,
	[last_error] [nvarchar](1000) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[next_retry_at] [datetime2](7) NULL,
	[source_type] [varchar](30) NOT NULL,
	[source_id] [int] NULL,
	[processing_token] [varchar](64) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_ai_job_assessment_key] UNIQUE NONCLUSTERED
(
	[assessment_key] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[ai_assessments](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[job_id] [int] NOT NULL,
	[assessment_key] [varchar](255) NOT NULL,
	[answer_id] [int] NULL,
	[answer_version] [int] NOT NULL,
	[state] [varchar](30) NOT NULL,
	[score] [decimal](5, 2) NULL,
	[estimated_cefr] [varchar](10) NULL,
	[feedback] [nvarchar](max) NULL,
	[transcript] [nvarchar](max) NULL,
	[rubric_version] [varchar](100) NOT NULL,
	[policy_version] [varchar](100) NOT NULL,
	[provider] [varchar](50) NULL,
	[model] [varchar](100) NULL,
	[error_message] [nvarchar](1000) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[completed_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_ai_assessment_job_version] UNIQUE NONCLUSTERED
(
	[job_id] ASC,
	[answer_version] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[question_options](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[question_item_id] [int] NOT NULL,
	[content] [nvarchar](max) NOT NULL,
	[option_code] [varchar](50) NULL,
	[is_correct] [bit] NULL,
	[position] [int] NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_question_options_id_item] UNIQUE NONCLUSTERED
(
	[id] ASC,
	[question_item_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_question_options_order] UNIQUE NONCLUSTERED
(
	[question_item_id] ASC,
	[position] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_goals](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[certificate_id] [int] NULL,
	[target_date] [date] NULL,
	[status] [varchar](30) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[flashcards](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[front_content] [nvarchar](max) NOT NULL,
	[back_content] [nvarchar](max) NOT NULL,
	[cefr_level] [varchar](10) NULL,
	[display_order] [int] NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[set_id] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[scoring_schemes](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[certificate_id] [int] NOT NULL,
	[name] [nvarchar](255) NOT NULL,
	[version] [varchar](30) NOT NULL,
	[exam_type] [varchar](30) NULL,
	[scope_skill] [varchar](30) NULL,
	[status] [varchar](30) NOT NULL,
	[effective_from] [datetime2](7) NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_scoring_schemes_version] UNIQUE NONCLUSTERED
(
	[certificate_id] ASC,
	[version] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[scoring_scale_rules](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[scoring_scheme_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[raw_min] [decimal](6, 2) NOT NULL,
	[raw_max] [decimal](6, 2) NOT NULL,
	[scale_min] [decimal](5, 2) NOT NULL,
	[scale_max] [decimal](5, 2) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_scoring_scale_rules_scheme_skill] UNIQUE NONCLUSTERED
(
	[scoring_scheme_id] ASC,
	[skill] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[score_bands](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[scoring_scheme_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[cefr_level] [varchar](10) NOT NULL,
	[min_score] [decimal](5, 2) NOT NULL,
	[max_score] [decimal](5, 2) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_score_bands_level] UNIQUE NONCLUSTERED
(
	[scoring_scheme_id] ASC,
	[skill] ASC,
	[cefr_level] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_goal_skills](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_goal_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[target_level] [varchar](10) NOT NULL,
	[target_score] [decimal](5, 2) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_goal_skills_goal_skill] UNIQUE NONCLUSTERED
(
	[user_goal_id] ASC,
	[skill] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_flashcard_progress](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[flashcard_id] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
	[mastery_level] [int] NULL,
	[review_count] [int] NOT NULL,
	[last_reviewed_at] [datetime2](7) NULL,
	[next_review_at] [datetime2](7) NULL,
	[interval_days] [int] NOT NULL,
	[ease_factor] [decimal](4, 2) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_flashcard_progress_user_card] UNIQUE NONCLUSTERED
(
	[user_id] ASC,
	[flashcard_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[flashcard_reviews](
	[id] [varchar](160) NOT NULL,
	[user_id] [int] NOT NULL,
	[flashcard_id] [int] NOT NULL,
	[state] [varchar](30) NOT NULL,
	[reviewed_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[mock_exams](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[certificate_id] [int] NOT NULL,
	[scoring_scheme_id] [int] NULL,
	[title] [nvarchar](255) NOT NULL,
	[description] [nvarchar](max) NULL,
	[exam_type] [varchar](30) NOT NULL,
	[target_level] [varchar](10) NULL,
	[status] [varchar](30) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[skill] [varchar](30) NULL,
	[duration_minutes] [int] NULL,
	[demo_only] [bit] NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[exam_attempts](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[mock_exam_id] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
	[grading_status] [varchar](30) NULL,
	[started_at] [datetime2](7) NOT NULL,
	[submitted_at] [datetime2](7) NULL,
	[completed_at] [datetime2](7) NULL,
	[final_scale_score] [decimal](6, 2) NULL,
	[estimated_overall_cefr] [varchar](10) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[request_key] [varchar](255) NULL,
	[answer_version] [int] NOT NULL,
	[scoring_scheme_id] [int] NULL,
	[policy_version] [varchar](100) NULL,
	[rubric_version] [varchar](100) NULL,
	[grading_request_key] [varchar](255) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[mock_exam_sections](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[mock_exam_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[display_order] [int] NOT NULL,
	[duration_minutes] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_exam_sections_order] UNIQUE NONCLUSTERED
(
	[mock_exam_id] ASC,
	[display_order] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_exam_sections_skill] UNIQUE NONCLUSTERED
(
	[mock_exam_id] ASC,
	[skill] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_v2_section_id_exam] UNIQUE NONCLUSTERED
(
	[id] ASC,
	[mock_exam_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_skill_profiles](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[current_level] [varchar](10) NOT NULL,
	[source_attempt_id] [int] NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[checkpoint_floor_level] [varchar](10) NULL,
	[checkpoint_source_attempt_id] [int] NULL,
	[checkpoint_last_attempt_id] [int] NULL,
	[checkpoint_last_submitted_at] [datetime2](7) NULL,
	[placement_level] [varchar](10) NULL,
	[placement_submitted_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_skill_profiles_user_skill] UNIQUE NONCLUSTERED
(
	[user_id] ASC,
	[skill] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_profile_applications](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[attempt_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[applied_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_profile_application_attempt_skill] UNIQUE NONCLUSTERED
(
	[user_id] ASC,
	[attempt_id] ASC,
	[skill] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_media_activities](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[exam_attempt_id] [int] NOT NULL,
	[question_media_id] [int] NOT NULL,
	[play_count] [int] NOT NULL,
	[last_played_at] [datetime2](7) NULL,
	[play_token_hash] [varchar](64) NULL,
	[play_token_expires_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_media_activities_attempt_media] UNIQUE NONCLUSTERED
(
	[exam_attempt_id] ASC,
	[question_media_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[exam_skill_results](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[exam_attempt_id] [int] NOT NULL,
	[skill] [varchar](30) NOT NULL,
	[correct_count] [int] NULL,
	[total_items] [int] NULL,
	[raw_score] [decimal](6, 2) NULL,
	[scale_score] [decimal](5, 2) NULL,
	[cefr_level] [varchar](10) NULL,
	[grading_method] [varchar](30) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[grading_status] [varchar](30) NULL,
	[objective_raw_score] [decimal](6, 2) NULL,
	[subjective_raw_score] [decimal](6, 2) NULL,
	[raw_max] [decimal](6, 2) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_skill_results_attempt_skill] UNIQUE NONCLUSTERED
(
	[exam_attempt_id] ASC,
	[skill] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[mock_exam_questions](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[mock_exam_section_id] [int] NULL,
	[question_id] [int] NOT NULL,
	[position] [int] NOT NULL,
	[mock_exam_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_v2_exam_position] UNIQUE NONCLUSTERED
(
	[mock_exam_id] ASC,
	[position] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_v2_exam_question] UNIQUE NONCLUSTERED
(
	[mock_exam_id] ASC,
	[question_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[exam_attempt_snapshots](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[exam_attempt_id] [int] NOT NULL,
	[mock_exam_id] [int] NOT NULL,
	[certificate_id] [int] NOT NULL,
	[scoring_scheme_id] [int] NULL,
	[exam_type] [varchar](30) NOT NULL,
	[skill] [varchar](30) NULL,
	[target_level] [varchar](10) NULL,
	[title] [nvarchar](255) NOT NULL,
	[duration_minutes] [int] NULL,
	[answer_version] [int] NOT NULL,
	[policy_version] [varchar](100) NOT NULL,
	[grading_context] [nvarchar](max) NOT NULL,
	[locked_question_snapshot] [nvarchar](max) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_exam_attempt_snapshot_attempt] UNIQUE NONCLUSTERED
(
	[exam_attempt_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[learning_paths](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[goal_skill_id] [int] NULL,
	[source_attempt_id] [int] NULL,
	[skill] [varchar](30) NOT NULL,
	[from_level] [varchar](10) NULL,
	[target_level] [varchar](10) NULL,
	[path_source] [varchar](30) NOT NULL,
	[checkpoint_exam_id] [int] NULL,
	[status] [varchar](30) NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[completed_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_answers](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[exam_attempt_id] [int] NOT NULL,
	[question_id] [int] NOT NULL,
	[status] [varchar](30) NOT NULL,
	[started_at] [datetime2](7) NULL,
	[completed_at] [datetime2](7) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
	[text_answer] [nvarchar](max) NULL,
	[audio_url] [nvarchar](500) NULL,
	[ai_score] [decimal](5, 2) NULL,
	[ai_feedback] [nvarchar](max) NULL,
	[graded_at] [datetime2](7) NULL,
	[transcript] [nvarchar](max) NULL,
	[ai_provider] [varchar](30) NULL,
	[ai_model] [varchar](100) NULL,
	[answer_version] [int] NOT NULL,
	[audio_manifest] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_user_answers_attempt_question] UNIQUE NONCLUSTERED
(
	[exam_attempt_id] ASC,
	[question_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[user_answer_details](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_answer_id] [int] NOT NULL,
	[question_item_id] [int] NOT NULL,
	[selected_option_id] [int] NULL,
	[user_position] [int] NULL,
	[answer_text] [nvarchar](max) NULL,
	[audio_path] [nvarchar](500) NULL,
	[transcript] [nvarchar](max) NULL,
	[recording_duration_seconds] [int] NULL,
	[auto_score] [decimal](5, 2) NULL,
	[feedback] [nvarchar](max) NULL,
	[grading_status] [varchar](30) NULL,
	[graded_at] [datetime2](7) NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_answer_details_answer_item] UNIQUE NONCLUSTERED
(
	[user_answer_id] ASC,
	[question_item_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[learning_path_items](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[learning_path_id] [int] NOT NULL,
	[lesson_id] [int] NOT NULL,
	[position] [int] NOT NULL,
	[is_required] [bit] NOT NULL,
	[status] [varchar](30) NOT NULL,
	[completed_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_path_items_lesson] UNIQUE NONCLUSTERED
(
	[learning_path_id] ASC,
	[lesson_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [uq_path_items_order] UNIQUE NONCLUSTERED
(
	[learning_path_id] ASC,
	[position] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

CREATE NONCLUSTERED INDEX [ix_question_items_question] ON [dbo].[question_items]
(
	[question_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
SET ANSI_PADDING ON

CREATE NONCLUSTERED INDEX [ix_flashcard_sets_owner] ON [dbo].[flashcard_sets]
(
	[owner_user_id] ASC,
	[status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE UNIQUE NONCLUSTERED INDEX [uq_flashcard_sets_lesson] ON [dbo].[flashcard_sets]
(
	[lesson_id] ASC
)
WHERE ([lesson_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_lesson_submission_owner] ON [dbo].[lesson_work_submissions]
(
	[user_id] ASC,
	[lesson_id] ASC,
	[submitted_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_lesson_progress_user] ON [dbo].[lesson_progress]
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_lca_user_lesson_time] ON [dbo].[lesson_check_attempts]
(
	[user_id] ASC,
	[lesson_id] ASC,
	[submitted_at] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_certificates_created_by] ON [dbo].[certificates]
(
	[created_by] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_question_options_item] ON [dbo].[question_options]
(
	[question_item_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
SET ANSI_PADDING ON

CREATE NONCLUSTERED INDEX [ix_flashcards_set] ON [dbo].[flashcards]
(
	[set_id] ASC,
	[status] ASC,
	[display_order] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_flashcard_progress_due] ON [dbo].[user_flashcard_progress]
(
	[user_id] ASC,
	[next_review_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_exam_attempts_exam] ON [dbo].[exam_attempts]
(
	[mock_exam_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_exam_attempts_user] ON [dbo].[exam_attempts]
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
SET ANSI_PADDING ON

CREATE UNIQUE NONCLUSTERED INDEX [uq_exam_attempts_user_request_key] ON [dbo].[exam_attempts]
(
	[user_id] ASC,
	[request_key] ASC
)
WHERE ([request_key] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_media_activities_attempt] ON [dbo].[user_media_activities]
(
	[exam_attempt_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_learning_paths_user] ON [dbo].[learning_paths]
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
SET ANSI_PADDING ON

CREATE UNIQUE NONCLUSTERED INDEX [uq_learning_paths_active_user_skill] ON [dbo].[learning_paths]
(
	[user_id] ASC,
	[skill] ASC
)
WHERE ([status]='ACTIVE')
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [ix_user_answers_attempt] ON [dbo].[user_answers]
(
	[exam_attempt_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
ALTER TABLE [dbo].[attempt_outbox] ADD  DEFAULT ((0)) FOR [attempts]
ALTER TABLE [dbo].[attempt_outbox] ADD  DEFAULT (sysdatetime()) FOR [next_attempt_at]
ALTER TABLE [dbo].[attempt_outbox] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[lessons] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[lessons] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[users] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[users] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[questions] ADD  DEFAULT ((1)) FOR [allow_option_reuse]
ALTER TABLE [dbo].[questions] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[questions] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[question_media] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[flashcard_sets] ADD  CONSTRAINT [df_flashcard_visibility]  DEFAULT ('PUBLIC') FOR [visibility]
ALTER TABLE [dbo].[lesson_responses] ADD  CONSTRAINT [df_lesson_response_version]  DEFAULT ((1)) FOR [response_version]
ALTER TABLE [dbo].[lesson_engagement] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[lesson_engagement] ADD  CONSTRAINT [df_lesson_playback_sequence]  DEFAULT ((0)) FOR [playback_sequence]
ALTER TABLE [dbo].[lesson_engagement] ADD  CONSTRAINT [df_lesson_playback_running]  DEFAULT ((0)) FOR [playback_running]
ALTER TABLE [dbo].[lesson_check_attempts] ADD  DEFAULT (sysdatetime()) FOR [submitted_at]
ALTER TABLE [dbo].[certificates] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[certificates] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[ai_assessment_jobs] ADD  DEFAULT ((0)) FOR [retry_count]
ALTER TABLE [dbo].[ai_assessment_jobs] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[ai_assessment_jobs] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[ai_assessment_jobs] ADD  CONSTRAINT [df_ai_job_source]  DEFAULT ('EXAM_ANSWER') FOR [source_type]
ALTER TABLE [dbo].[ai_assessments] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[user_goals] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[flashcards] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[flashcards] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[flashcards] ADD  CONSTRAINT [df_flashcards_status]  DEFAULT ('ACTIVE') FOR [status]
ALTER TABLE [dbo].[scoring_schemes] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[user_flashcard_progress] ADD  DEFAULT ((0)) FOR [review_count]
ALTER TABLE [dbo].[user_flashcard_progress] ADD  CONSTRAINT [df_flashcard_progress_interval]  DEFAULT ((0)) FOR [interval_days]
ALTER TABLE [dbo].[user_flashcard_progress] ADD  CONSTRAINT [df_flashcard_progress_ease]  DEFAULT ((2.50)) FOR [ease_factor]
ALTER TABLE [dbo].[mock_exams] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[mock_exams] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[mock_exams] ADD  CONSTRAINT [df_mock_exams_demo_only]  DEFAULT ((0)) FOR [demo_only]
ALTER TABLE [dbo].[exam_attempts] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[exam_attempts] ADD  CONSTRAINT [df_exam_attempts_answer_version]  DEFAULT ((1)) FOR [answer_version]
ALTER TABLE [dbo].[user_skill_profiles] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[user_profile_applications] ADD  DEFAULT (sysdatetime()) FOR [applied_at]
ALTER TABLE [dbo].[user_media_activities] ADD  DEFAULT ((0)) FOR [play_count]
ALTER TABLE [dbo].[exam_skill_results] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[exam_attempt_snapshots] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[learning_paths] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[user_answers] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[user_answers] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[user_answers] ADD  CONSTRAINT [df_user_answers_answer_version]  DEFAULT ((1)) FOR [answer_version]
ALTER TABLE [dbo].[user_answer_details] ADD  DEFAULT (sysdatetime()) FOR [created_at]
ALTER TABLE [dbo].[user_answer_details] ADD  DEFAULT (sysdatetime()) FOR [updated_at]
ALTER TABLE [dbo].[learning_path_items] ADD  DEFAULT ((1)) FOR [is_required]
ALTER TABLE [dbo].[learning_path_items] ADD  CONSTRAINT [df_v2_path_item_status]  DEFAULT ('NOT_STARTED') FOR [status]
ALTER TABLE [dbo].[question_media]  WITH CHECK ADD  CONSTRAINT [fk_question_media_question] FOREIGN KEY([question_id])
REFERENCES [dbo].[questions] ([id])
ALTER TABLE [dbo].[question_media] CHECK CONSTRAINT [fk_question_media_question]
ALTER TABLE [dbo].[question_items]  WITH CHECK ADD  CONSTRAINT [fk_question_items_question] FOREIGN KEY([question_id])
REFERENCES [dbo].[questions] ([id])
ALTER TABLE [dbo].[question_items] CHECK CONSTRAINT [fk_question_items_question]
ALTER TABLE [dbo].[flashcard_sets]  WITH CHECK ADD  CONSTRAINT [fk_flashcard_set_lesson] FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[flashcard_sets] CHECK CONSTRAINT [fk_flashcard_set_lesson]
ALTER TABLE [dbo].[flashcard_sets]  WITH CHECK ADD  CONSTRAINT [fk_flashcard_set_owner] FOREIGN KEY([owner_user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[flashcard_sets] CHECK CONSTRAINT [fk_flashcard_set_owner]
ALTER TABLE [dbo].[lesson_work_submissions]  WITH CHECK ADD  CONSTRAINT [fk_lesson_submission_lesson] FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[lesson_work_submissions] CHECK CONSTRAINT [fk_lesson_submission_lesson]
ALTER TABLE [dbo].[lesson_work_submissions]  WITH CHECK ADD  CONSTRAINT [fk_lesson_submission_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[lesson_work_submissions] CHECK CONSTRAINT [fk_lesson_submission_user]
ALTER TABLE [dbo].[lesson_responses]  WITH CHECK ADD FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[lesson_responses]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[lesson_progress]  WITH CHECK ADD  CONSTRAINT [fk_lesson_progress_lesson] FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[lesson_progress] CHECK CONSTRAINT [fk_lesson_progress_lesson]
ALTER TABLE [dbo].[lesson_progress]  WITH CHECK ADD  CONSTRAINT [fk_lesson_progress_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[lesson_progress] CHECK CONSTRAINT [fk_lesson_progress_user]
ALTER TABLE [dbo].[lesson_engagement]  WITH CHECK ADD  CONSTRAINT [fk_le_lesson] FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[lesson_engagement] CHECK CONSTRAINT [fk_le_lesson]
ALTER TABLE [dbo].[lesson_engagement]  WITH CHECK ADD  CONSTRAINT [fk_le_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[lesson_engagement] CHECK CONSTRAINT [fk_le_user]
ALTER TABLE [dbo].[lesson_checks]  WITH CHECK ADD FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[lesson_check_attempts]  WITH CHECK ADD  CONSTRAINT [fk_lca_lesson] FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[lesson_check_attempts] CHECK CONSTRAINT [fk_lca_lesson]
ALTER TABLE [dbo].[lesson_check_attempts]  WITH CHECK ADD  CONSTRAINT [fk_lca_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[lesson_check_attempts] CHECK CONSTRAINT [fk_lca_user]
ALTER TABLE [dbo].[lesson_block_listens]  WITH CHECK ADD FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[lesson_block_listens]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[certificates]  WITH CHECK ADD  CONSTRAINT [fk_certificates_created_by] FOREIGN KEY([created_by])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[certificates] CHECK CONSTRAINT [fk_certificates_created_by]
ALTER TABLE [dbo].[ai_assessment_jobs]  WITH CHECK ADD  CONSTRAINT [fk_ai_job_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[ai_assessment_jobs] CHECK CONSTRAINT [fk_ai_job_user]
ALTER TABLE [dbo].[ai_assessments]  WITH CHECK ADD  CONSTRAINT [fk_ai_assessment_job] FOREIGN KEY([job_id])
REFERENCES [dbo].[ai_assessment_jobs] ([id])
ALTER TABLE [dbo].[ai_assessments] CHECK CONSTRAINT [fk_ai_assessment_job]
ALTER TABLE [dbo].[question_options]  WITH CHECK ADD  CONSTRAINT [fk_question_options_item] FOREIGN KEY([question_item_id])
REFERENCES [dbo].[question_items] ([id])
ALTER TABLE [dbo].[question_options] CHECK CONSTRAINT [fk_question_options_item]
ALTER TABLE [dbo].[user_goals]  WITH CHECK ADD  CONSTRAINT [fk_user_goals_certificate] FOREIGN KEY([certificate_id])
REFERENCES [dbo].[certificates] ([id])
ALTER TABLE [dbo].[user_goals] CHECK CONSTRAINT [fk_user_goals_certificate]
ALTER TABLE [dbo].[user_goals]  WITH CHECK ADD  CONSTRAINT [fk_user_goals_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[user_goals] CHECK CONSTRAINT [fk_user_goals_user]
ALTER TABLE [dbo].[flashcards]  WITH CHECK ADD  CONSTRAINT [fk_flashcard_set] FOREIGN KEY([set_id])
REFERENCES [dbo].[flashcard_sets] ([id])
ALTER TABLE [dbo].[flashcards] CHECK CONSTRAINT [fk_flashcard_set]
ALTER TABLE [dbo].[scoring_schemes]  WITH CHECK ADD  CONSTRAINT [fk_scoring_schemes_certificate] FOREIGN KEY([certificate_id])
REFERENCES [dbo].[certificates] ([id])
ALTER TABLE [dbo].[scoring_schemes] CHECK CONSTRAINT [fk_scoring_schemes_certificate]
ALTER TABLE [dbo].[scoring_scale_rules]  WITH CHECK ADD  CONSTRAINT [fk_scoring_scale_rules_scheme] FOREIGN KEY([scoring_scheme_id])
REFERENCES [dbo].[scoring_schemes] ([id])
ALTER TABLE [dbo].[scoring_scale_rules] CHECK CONSTRAINT [fk_scoring_scale_rules_scheme]
ALTER TABLE [dbo].[score_bands]  WITH CHECK ADD  CONSTRAINT [fk_score_bands_scheme] FOREIGN KEY([scoring_scheme_id])
REFERENCES [dbo].[scoring_schemes] ([id])
ALTER TABLE [dbo].[score_bands] CHECK CONSTRAINT [fk_score_bands_scheme]
ALTER TABLE [dbo].[user_goal_skills]  WITH CHECK ADD  CONSTRAINT [fk_goal_skills_goal] FOREIGN KEY([user_goal_id])
REFERENCES [dbo].[user_goals] ([id])
ALTER TABLE [dbo].[user_goal_skills] CHECK CONSTRAINT [fk_goal_skills_goal]
ALTER TABLE [dbo].[user_flashcard_progress]  WITH CHECK ADD  CONSTRAINT [fk_flashcard_progress_card] FOREIGN KEY([flashcard_id])
REFERENCES [dbo].[flashcards] ([id])
ALTER TABLE [dbo].[user_flashcard_progress] CHECK CONSTRAINT [fk_flashcard_progress_card]
ALTER TABLE [dbo].[user_flashcard_progress]  WITH CHECK ADD  CONSTRAINT [fk_flashcard_progress_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[user_flashcard_progress] CHECK CONSTRAINT [fk_flashcard_progress_user]
ALTER TABLE [dbo].[flashcard_reviews]  WITH CHECK ADD FOREIGN KEY([flashcard_id])
REFERENCES [dbo].[flashcards] ([id])
ALTER TABLE [dbo].[flashcard_reviews]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[mock_exams]  WITH CHECK ADD  CONSTRAINT [fk_mock_exams_certificate] FOREIGN KEY([certificate_id])
REFERENCES [dbo].[certificates] ([id])
ALTER TABLE [dbo].[mock_exams] CHECK CONSTRAINT [fk_mock_exams_certificate]
ALTER TABLE [dbo].[mock_exams]  WITH CHECK ADD  CONSTRAINT [fk_mock_exams_scheme] FOREIGN KEY([scoring_scheme_id])
REFERENCES [dbo].[scoring_schemes] ([id])
ALTER TABLE [dbo].[mock_exams] CHECK CONSTRAINT [fk_mock_exams_scheme]
ALTER TABLE [dbo].[exam_attempts]  WITH CHECK ADD  CONSTRAINT [fk_exam_attempts_exam] FOREIGN KEY([mock_exam_id])
REFERENCES [dbo].[mock_exams] ([id])
ALTER TABLE [dbo].[exam_attempts] CHECK CONSTRAINT [fk_exam_attempts_exam]
ALTER TABLE [dbo].[exam_attempts]  WITH CHECK ADD  CONSTRAINT [fk_exam_attempts_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[exam_attempts] CHECK CONSTRAINT [fk_exam_attempts_user]
ALTER TABLE [dbo].[mock_exam_sections]  WITH CHECK ADD  CONSTRAINT [fk_exam_sections_exam] FOREIGN KEY([mock_exam_id])
REFERENCES [dbo].[mock_exams] ([id])
ALTER TABLE [dbo].[mock_exam_sections] CHECK CONSTRAINT [fk_exam_sections_exam]
ALTER TABLE [dbo].[user_skill_profiles]  WITH CHECK ADD  CONSTRAINT [fk_skill_profiles_attempt] FOREIGN KEY([source_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[user_skill_profiles] CHECK CONSTRAINT [fk_skill_profiles_attempt]
ALTER TABLE [dbo].[user_skill_profiles]  WITH CHECK ADD  CONSTRAINT [fk_skill_profiles_checkpoint_attempt] FOREIGN KEY([checkpoint_source_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[user_skill_profiles] CHECK CONSTRAINT [fk_skill_profiles_checkpoint_attempt]
ALTER TABLE [dbo].[user_skill_profiles]  WITH CHECK ADD  CONSTRAINT [fk_skill_profiles_checkpoint_last_attempt] FOREIGN KEY([checkpoint_last_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[user_skill_profiles] CHECK CONSTRAINT [fk_skill_profiles_checkpoint_last_attempt]
ALTER TABLE [dbo].[user_skill_profiles]  WITH CHECK ADD  CONSTRAINT [fk_skill_profiles_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[user_skill_profiles] CHECK CONSTRAINT [fk_skill_profiles_user]
ALTER TABLE [dbo].[user_profile_applications]  WITH CHECK ADD  CONSTRAINT [fk_profile_application_attempt] FOREIGN KEY([attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[user_profile_applications] CHECK CONSTRAINT [fk_profile_application_attempt]
ALTER TABLE [dbo].[user_profile_applications]  WITH CHECK ADD  CONSTRAINT [fk_profile_application_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[user_profile_applications] CHECK CONSTRAINT [fk_profile_application_user]
ALTER TABLE [dbo].[user_media_activities]  WITH CHECK ADD  CONSTRAINT [fk_media_activities_attempt] FOREIGN KEY([exam_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[user_media_activities] CHECK CONSTRAINT [fk_media_activities_attempt]
ALTER TABLE [dbo].[user_media_activities]  WITH CHECK ADD  CONSTRAINT [fk_media_activities_media] FOREIGN KEY([question_media_id])
REFERENCES [dbo].[question_media] ([id])
ALTER TABLE [dbo].[user_media_activities] CHECK CONSTRAINT [fk_media_activities_media]
ALTER TABLE [dbo].[exam_skill_results]  WITH CHECK ADD  CONSTRAINT [fk_skill_results_attempt] FOREIGN KEY([exam_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[exam_skill_results] CHECK CONSTRAINT [fk_skill_results_attempt]
ALTER TABLE [dbo].[mock_exam_questions]  WITH CHECK ADD  CONSTRAINT [fk_exam_questions_question] FOREIGN KEY([question_id])
REFERENCES [dbo].[questions] ([id])
ALTER TABLE [dbo].[mock_exam_questions] CHECK CONSTRAINT [fk_exam_questions_question]
ALTER TABLE [dbo].[mock_exam_questions]  WITH CHECK ADD  CONSTRAINT [fk_exam_questions_section] FOREIGN KEY([mock_exam_section_id])
REFERENCES [dbo].[mock_exam_sections] ([id])
ALTER TABLE [dbo].[mock_exam_questions] CHECK CONSTRAINT [fk_exam_questions_section]
ALTER TABLE [dbo].[mock_exam_questions]  WITH CHECK ADD  CONSTRAINT [fk_v2_exam_questions_exam] FOREIGN KEY([mock_exam_id])
REFERENCES [dbo].[mock_exams] ([id])
ALTER TABLE [dbo].[mock_exam_questions] CHECK CONSTRAINT [fk_v2_exam_questions_exam]
ALTER TABLE [dbo].[mock_exam_questions]  WITH CHECK ADD  CONSTRAINT [fk_v2_question_section_exam] FOREIGN KEY([mock_exam_section_id], [mock_exam_id])
REFERENCES [dbo].[mock_exam_sections] ([id], [mock_exam_id])
ALTER TABLE [dbo].[mock_exam_questions] CHECK CONSTRAINT [fk_v2_question_section_exam]
ALTER TABLE [dbo].[exam_attempt_snapshots]  WITH CHECK ADD  CONSTRAINT [fk_exam_attempt_snapshot_attempt] FOREIGN KEY([exam_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[exam_attempt_snapshots] CHECK CONSTRAINT [fk_exam_attempt_snapshot_attempt]
ALTER TABLE [dbo].[learning_paths]  WITH CHECK ADD  CONSTRAINT [fk_learning_paths_attempt] FOREIGN KEY([source_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[learning_paths] CHECK CONSTRAINT [fk_learning_paths_attempt]
ALTER TABLE [dbo].[learning_paths]  WITH CHECK ADD  CONSTRAINT [fk_learning_paths_checkpoint] FOREIGN KEY([checkpoint_exam_id])
REFERENCES [dbo].[mock_exams] ([id])
ALTER TABLE [dbo].[learning_paths] CHECK CONSTRAINT [fk_learning_paths_checkpoint]
ALTER TABLE [dbo].[learning_paths]  WITH CHECK ADD  CONSTRAINT [fk_learning_paths_goal_skill] FOREIGN KEY([goal_skill_id])
REFERENCES [dbo].[user_goal_skills] ([id])
ALTER TABLE [dbo].[learning_paths] CHECK CONSTRAINT [fk_learning_paths_goal_skill]
ALTER TABLE [dbo].[learning_paths]  WITH CHECK ADD  CONSTRAINT [fk_learning_paths_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([id])
ALTER TABLE [dbo].[learning_paths] CHECK CONSTRAINT [fk_learning_paths_user]
ALTER TABLE [dbo].[user_answers]  WITH CHECK ADD  CONSTRAINT [fk_user_answers_attempt] FOREIGN KEY([exam_attempt_id])
REFERENCES [dbo].[exam_attempts] ([id])
ALTER TABLE [dbo].[user_answers] CHECK CONSTRAINT [fk_user_answers_attempt]
ALTER TABLE [dbo].[user_answers]  WITH CHECK ADD  CONSTRAINT [fk_user_answers_question] FOREIGN KEY([question_id])
REFERENCES [dbo].[questions] ([id])
ALTER TABLE [dbo].[user_answers] CHECK CONSTRAINT [fk_user_answers_question]
ALTER TABLE [dbo].[user_answer_details]  WITH CHECK ADD  CONSTRAINT [fk_answer_details_answer] FOREIGN KEY([user_answer_id])
REFERENCES [dbo].[user_answers] ([id])
ALTER TABLE [dbo].[user_answer_details] CHECK CONSTRAINT [fk_answer_details_answer]
ALTER TABLE [dbo].[user_answer_details]  WITH CHECK ADD  CONSTRAINT [fk_answer_details_item] FOREIGN KEY([question_item_id])
REFERENCES [dbo].[question_items] ([id])
ALTER TABLE [dbo].[user_answer_details] CHECK CONSTRAINT [fk_answer_details_item]
ALTER TABLE [dbo].[user_answer_details]  WITH CHECK ADD  CONSTRAINT [fk_answer_details_selected_option] FOREIGN KEY([selected_option_id], [question_item_id])
REFERENCES [dbo].[question_options] ([id], [question_item_id])
ALTER TABLE [dbo].[user_answer_details] CHECK CONSTRAINT [fk_answer_details_selected_option]
ALTER TABLE [dbo].[learning_path_items]  WITH CHECK ADD  CONSTRAINT [fk_path_items_lesson] FOREIGN KEY([lesson_id])
REFERENCES [dbo].[lessons] ([id])
ALTER TABLE [dbo].[learning_path_items] CHECK CONSTRAINT [fk_path_items_lesson]
ALTER TABLE [dbo].[learning_path_items]  WITH CHECK ADD  CONSTRAINT [fk_path_items_path] FOREIGN KEY([learning_path_id])
REFERENCES [dbo].[learning_paths] ([id])
ALTER TABLE [dbo].[learning_path_items] CHECK CONSTRAINT [fk_path_items_path]
ALTER TABLE [dbo].[lessons]  WITH CHECK ADD  CONSTRAINT [ck_lessons_order] CHECK  (([display_order]>(0)))
ALTER TABLE [dbo].[lessons] CHECK CONSTRAINT [ck_lessons_order]
ALTER TABLE [dbo].[users]  WITH CHECK ADD  CONSTRAINT [ck_users_role] CHECK  (([role]='ADMIN' OR [role]='STUDENT'))
ALTER TABLE [dbo].[users] CHECK CONSTRAINT [ck_users_role]
ALTER TABLE [dbo].[users]  WITH CHECK ADD  CONSTRAINT [ck_users_status] CHECK  (([status]='LOCKED' OR [status]='INACTIVE' OR [status]='ACTIVE'))
ALTER TABLE [dbo].[users] CHECK CONSTRAINT [ck_users_status]
ALTER TABLE [dbo].[questions]  WITH CHECK ADD  CONSTRAINT [ck_questions_part] CHECK  (([part_no] IS NULL OR [part_no]>=(1) AND [part_no]<=(4)))
ALTER TABLE [dbo].[questions] CHECK CONSTRAINT [ck_questions_part]
ALTER TABLE [dbo].[questions]  WITH CHECK ADD  CONSTRAINT [ck_questions_preparation] CHECK  (([preparation_seconds] IS NULL OR [preparation_seconds]>=(0)))
ALTER TABLE [dbo].[questions] CHECK CONSTRAINT [ck_questions_preparation]
ALTER TABLE [dbo].[questions]  WITH CHECK ADD  CONSTRAINT [ck_v2_question_skill] CHECK  (([skill]='SPEAKING' OR [skill]='WRITING' OR [skill]='LISTENING' OR [skill]='READING' OR [skill]='GRAMMAR_VOCABULARY'))
ALTER TABLE [dbo].[questions] CHECK CONSTRAINT [ck_v2_question_skill]
ALTER TABLE [dbo].[questions]  WITH CHECK ADD  CONSTRAINT [ck_v2_question_type] CHECK  (([question_type]='SPEAKING_AUDIO' OR [question_type]='WRITING_TEXT' OR [question_type]='ORDERING' OR [question_type]='MATCHING' OR [question_type]='SINGLE_CHOICE'))
ALTER TABLE [dbo].[questions] CHECK CONSTRAINT [ck_v2_question_type]
ALTER TABLE [dbo].[question_media]  WITH CHECK ADD  CONSTRAINT [ck_question_media_max_play] CHECK  (([max_play_count] IS NULL OR [max_play_count]>(0)))
ALTER TABLE [dbo].[question_media] CHECK CONSTRAINT [ck_question_media_max_play]
ALTER TABLE [dbo].[question_media]  WITH CHECK ADD  CONSTRAINT [ck_question_media_size] CHECK  (([file_size] IS NULL OR [file_size]>=(0)))
ALTER TABLE [dbo].[question_media] CHECK CONSTRAINT [ck_question_media_size]
ALTER TABLE [dbo].[question_media]  WITH CHECK ADD  CONSTRAINT [ck_question_media_type] CHECK  (([media_type]='VIDEO' OR [media_type]='AUDIO' OR [media_type]='IMAGE'))
ALTER TABLE [dbo].[question_media] CHECK CONSTRAINT [ck_question_media_type]
ALTER TABLE [dbo].[question_items]  WITH CHECK ADD  CONSTRAINT [ck_question_items_order] CHECK  (([position]>(0)))
ALTER TABLE [dbo].[question_items] CHECK CONSTRAINT [ck_question_items_order]
ALTER TABLE [dbo].[question_items]  WITH CHECK ADD  CONSTRAINT [ck_question_items_position] CHECK  (([correct_position] IS NULL OR [correct_position]>(0)))
ALTER TABLE [dbo].[question_items] CHECK CONSTRAINT [ck_question_items_position]
ALTER TABLE [dbo].[question_items]  WITH CHECK ADD  CONSTRAINT [ck_question_items_words] CHECK  (([min_word_count] IS NULL OR [max_word_count] IS NULL OR [min_word_count]<=[max_word_count]))
ALTER TABLE [dbo].[question_items] CHECK CONSTRAINT [ck_question_items_words]
ALTER TABLE [dbo].[flashcard_sets]  WITH CHECK ADD  CONSTRAINT [ck_flashcard_visibility] CHECK  (([visibility]='PRIVATE' OR [visibility]='PUBLIC'))
ALTER TABLE [dbo].[flashcard_sets] CHECK CONSTRAINT [ck_flashcard_visibility]
ALTER TABLE [dbo].[lesson_checks]  WITH CHECK ADD  CONSTRAINT [ck_lesson_checks_correct_option] CHECK  (([correct_option]>=(0) AND [correct_option]<=(5)))
ALTER TABLE [dbo].[lesson_checks] CHECK CONSTRAINT [ck_lesson_checks_correct_option]
ALTER TABLE [dbo].[lesson_check_attempts]  WITH CHECK ADD  CONSTRAINT [ck_lca_score] CHECK  (([score]>=(0) AND [total]>(0) AND [score]<=[total]))
ALTER TABLE [dbo].[lesson_check_attempts] CHECK CONSTRAINT [ck_lca_score]
ALTER TABLE [dbo].[ai_assessments]  WITH CHECK ADD  CONSTRAINT [ck_ai_assessment_score] CHECK  (([score] IS NULL OR [score]>=(0) AND [score]<=(50)))
ALTER TABLE [dbo].[ai_assessments] CHECK CONSTRAINT [ck_ai_assessment_score]
ALTER TABLE [dbo].[question_options]  WITH CHECK ADD  CONSTRAINT [ck_question_options_order] CHECK  (([position]>(0)))
ALTER TABLE [dbo].[question_options] CHECK CONSTRAINT [ck_question_options_order]
ALTER TABLE [dbo].[flashcards]  WITH CHECK ADD  CONSTRAINT [ck_flashcards_order] CHECK  (([display_order]>(0)))
ALTER TABLE [dbo].[flashcards] CHECK CONSTRAINT [ck_flashcards_order]
ALTER TABLE [dbo].[scoring_scale_rules]  WITH CHECK ADD  CONSTRAINT [ck_scoring_scale_rules_range] CHECK  (([raw_min]<[raw_max] AND [scale_min]<=[scale_max]))
ALTER TABLE [dbo].[scoring_scale_rules] CHECK CONSTRAINT [ck_scoring_scale_rules_range]
ALTER TABLE [dbo].[score_bands]  WITH CHECK ADD  CONSTRAINT [ck_score_bands_range] CHECK  (([min_score]<=[max_score] AND [min_score]>=(0) AND [max_score]<=(999.99)))
ALTER TABLE [dbo].[score_bands] CHECK CONSTRAINT [ck_score_bands_range]
ALTER TABLE [dbo].[user_goal_skills]  WITH CHECK ADD  CONSTRAINT [ck_goal_skills_score] CHECK  (([target_score] IS NULL OR [target_score]>=(0) AND [target_score]<=(50)))
ALTER TABLE [dbo].[user_goal_skills] CHECK CONSTRAINT [ck_goal_skills_score]
ALTER TABLE [dbo].[user_flashcard_progress]  WITH CHECK ADD  CONSTRAINT [ck_flashcard_progress_ease] CHECK  (([ease_factor]>=(1.30) AND [ease_factor]<=(3.00)))
ALTER TABLE [dbo].[user_flashcard_progress] CHECK CONSTRAINT [ck_flashcard_progress_ease]
ALTER TABLE [dbo].[user_flashcard_progress]  WITH CHECK ADD  CONSTRAINT [ck_flashcard_progress_interval] CHECK  (([interval_days]>=(0)))
ALTER TABLE [dbo].[user_flashcard_progress] CHECK CONSTRAINT [ck_flashcard_progress_interval]
ALTER TABLE [dbo].[user_flashcard_progress]  WITH CHECK ADD  CONSTRAINT [ck_flashcard_progress_mastery] CHECK  (([mastery_level] IS NULL OR [mastery_level]>=(0) AND [mastery_level]<=(5)))
ALTER TABLE [dbo].[user_flashcard_progress] CHECK CONSTRAINT [ck_flashcard_progress_mastery]
ALTER TABLE [dbo].[user_flashcard_progress]  WITH CHECK ADD  CONSTRAINT [ck_flashcard_progress_reviews] CHECK  (([review_count]>=(0)))
ALTER TABLE [dbo].[user_flashcard_progress] CHECK CONSTRAINT [ck_flashcard_progress_reviews]
ALTER TABLE [dbo].[mock_exams]  WITH CHECK ADD  CONSTRAINT [ck_mock_exams_duration] CHECK  (([duration_minutes] IS NULL OR [duration_minutes]>(0)))
ALTER TABLE [dbo].[mock_exams] CHECK CONSTRAINT [ck_mock_exams_duration]
ALTER TABLE [dbo].[mock_exams]  WITH CHECK ADD  CONSTRAINT [ck_mock_exams_type] CHECK  (([exam_type]='FULL_MOCK' OR [exam_type]='CHECKPOINT' OR [exam_type]='PRACTICE' OR [exam_type]='PLACEMENT'))
ALTER TABLE [dbo].[mock_exams] CHECK CONSTRAINT [ck_mock_exams_type]
ALTER TABLE [dbo].[exam_attempts]  WITH CHECK ADD  CONSTRAINT [ck_exam_attempts_score] CHECK  (([final_scale_score] IS NULL OR [final_scale_score]>=(0) AND [final_scale_score]<=(999.99)))
ALTER TABLE [dbo].[exam_attempts] CHECK CONSTRAINT [ck_exam_attempts_score]
ALTER TABLE [dbo].[mock_exam_sections]  WITH CHECK ADD  CONSTRAINT [ck_exam_sections_duration] CHECK  (([duration_minutes]>(0)))
ALTER TABLE [dbo].[mock_exam_sections] CHECK CONSTRAINT [ck_exam_sections_duration]
ALTER TABLE [dbo].[user_media_activities]  WITH CHECK ADD  CONSTRAINT [ck_media_activities_count] CHECK  (([play_count]>=(0) AND [play_count]<=(2)))
ALTER TABLE [dbo].[user_media_activities] CHECK CONSTRAINT [ck_media_activities_count]
ALTER TABLE [dbo].[exam_skill_results]  WITH CHECK ADD  CONSTRAINT [ck_skill_results_counts] CHECK  ((([correct_count] IS NULL OR [correct_count]>=(0)) AND ([total_items] IS NULL OR [total_items]>=(0))))
ALTER TABLE [dbo].[exam_skill_results] CHECK CONSTRAINT [ck_skill_results_counts]
ALTER TABLE [dbo].[exam_skill_results]  WITH CHECK ADD  CONSTRAINT [ck_skill_results_scale] CHECK  (([scale_score] IS NULL OR [scale_score]>=(0) AND [scale_score]<=(999.99)))
ALTER TABLE [dbo].[exam_skill_results] CHECK CONSTRAINT [ck_skill_results_scale]
ALTER TABLE [dbo].[user_answers]  WITH CHECK ADD  CONSTRAINT [ck_v2_answer_ai_score] CHECK  (([ai_score] IS NULL OR [ai_score]>=(0) AND [ai_score]<=(50)))
ALTER TABLE [dbo].[user_answers] CHECK CONSTRAINT [ck_v2_answer_ai_score]
ALTER TABLE [dbo].[user_answer_details]  WITH CHECK ADD  CONSTRAINT [ck_answer_details_score] CHECK  (([auto_score] IS NULL OR [auto_score]>=(0) AND [auto_score]<=(50)))
ALTER TABLE [dbo].[user_answer_details] CHECK CONSTRAINT [ck_answer_details_score]
ALTER TABLE [dbo].[learning_path_items]  WITH CHECK ADD  CONSTRAINT [ck_path_items_order] CHECK  (([position]>(0)))
ALTER TABLE [dbo].[learning_path_items] CHECK CONSTRAINT [ck_path_items_order]

CREATE TABLE [dbo].[account_unlock_requests](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[message] [nvarchar](1000) NOT NULL,
	[status] [varchar](20) NOT NULL CONSTRAINT [df_account_unlock_requests_status] DEFAULT ('PENDING'),
	[admin_note] [nvarchar](1000) NULL,
	[handled_by] [int] NULL,
	[created_at] [datetime2](7) NOT NULL CONSTRAINT [df_account_unlock_requests_created_at] DEFAULT (sysdatetime()),
	[handled_at] [datetime2](7) NULL,
	CONSTRAINT [pk_account_unlock_requests] PRIMARY KEY CLUSTERED ([id] ASC),
	CONSTRAINT [fk_account_unlock_requests_user] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users]([id]),
	CONSTRAINT [fk_account_unlock_requests_handled_by] FOREIGN KEY ([handled_by]) REFERENCES [dbo].[users]([id]),
	CONSTRAINT [ck_account_unlock_requests_status] CHECK ([status] IN ('PENDING', 'APPROVED', 'REJECTED'))
)
CREATE UNIQUE NONCLUSTERED INDEX [uq_account_unlock_requests_pending_user]
	ON [dbo].[account_unlock_requests]([user_id])
	WHERE [status] = 'PENDING';

COMMIT TRANSACTION;
