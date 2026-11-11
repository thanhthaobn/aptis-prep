package com.aptisprep.dto;

import com.aptisprep.entity.ExamAttemptEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExamAttemptDTO {
    private Integer id;
    private Integer userId;
    private String userFullName;
    private String userEmail;
    private Integer mockExamId;
    private String examType;
    private String examTitle;
    private Integer scoringSchemeId;
    private String status = "IN_PROGRESS";
    private String state;
    private String gradingStatus;
    private Integer answerVersion = 1;
    private String policyVersion;
    private String rubricVersion;
    private String gradingRequestKey;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private BigDecimal finalScaleScore;
    private String estimatedOverallCefr;
    private String overallCefr;
    private Instant deadline;
    private Long remainingSeconds;
    private Integer nextSectionAfterMinute;

    private List<SkillResultDTO> skills = new ArrayList<>();
    private List<UserAnswerDTO> answers = new ArrayList<>();

    public ExamAttemptDTO() {}

    public ExamAttemptDTO(Integer attemptId, String state, Instant deadline) {
        this.id = attemptId;
        this.state = state;
        this.deadline = deadline;
    }

    public ExamAttemptDTO(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            String examTitle,
            Integer scoringSchemeId,
            String state,
            List<SkillResultDTO> skills,
            BigDecimal finalScaleScore,
            String overallCefr,
            Instant submittedAt) {
        this.id = attemptId;
        this.userId = userId;
        this.mockExamId = examId;
        this.examType = examType;
        this.examTitle = examTitle;
        this.scoringSchemeId = scoringSchemeId;
        this.state = state;
        this.status = state;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.finalScaleScore = finalScaleScore;
        this.overallCefr = overallCefr;
        this.estimatedOverallCefr = overallCefr;
        this.submittedAt =
                submittedAt != null
                        ? LocalDateTime.ofInstant(submittedAt, java.time.ZoneId.systemDefault())
                        : null;
    }

    public static ExamAttemptDTO convertEntityToDto(ExamAttemptEntity entity) {
        if (entity == null) return null;
        ExamAttemptDTO dto = new ExamAttemptDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setMockExamId(entity.getMockExamId());
        dto.setStatus(entity.getStatus());
        dto.setState(entity.getStatus());
        dto.setGradingStatus(entity.getGradingStatus());
        dto.setAnswerVersion(entity.getAnswerVersion());
        dto.setScoringSchemeId(entity.getScoringSchemeId());
        dto.setPolicyVersion(entity.getPolicyVersion());
        dto.setRubricVersion(entity.getRubricVersion());
        dto.setGradingRequestKey(entity.getGradingRequestKey());
        dto.setStartedAt(entity.getStartedAt());
        dto.setSubmittedAt(entity.getSubmittedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setFinalScaleScore(entity.getFinalScaleScore());
        dto.setEstimatedOverallCefr(entity.getEstimatedOverallCefr());
        dto.setOverallCefr(entity.getEstimatedOverallCefr());
        return dto;
    }

    public ExamAttemptEntity convertDtoToEntity() {
        ExamAttemptEntity entity =
                new ExamAttemptEntity(
                        this.userId,
                        this.mockExamId,
                        this.status != null ? this.status : "IN_PROGRESS",
                        this.gradingStatus != null ? this.gradingStatus : "PENDING",
                        this.answerVersion != null ? this.answerVersion : 1,
                        this.scoringSchemeId,
                        this.policyVersion,
                        this.rubricVersion,
                        this.gradingRequestKey,
                        this.startedAt != null ? this.startedAt : LocalDateTime.now(),
                        this.submittedAt,
                        this.completedAt,
                        this.finalScaleScore,
                        this.estimatedOverallCefr != null
                                ? this.estimatedOverallCefr
                                : this.overallCefr,
                        null,
                        LocalDateTime.now());
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(ExamAttemptEntity entity) {
        if (entity == null) return;
        entity.setStatus(this.status != null ? this.status : this.state);
        entity.setGradingStatus(this.gradingStatus);
        entity.setSubmittedAt(this.submittedAt);
        entity.setCompletedAt(this.completedAt);
        entity.setFinalScaleScore(this.finalScaleScore);
        entity.setEstimatedOverallCefr(
                this.estimatedOverallCefr != null ? this.estimatedOverallCefr : this.overallCefr);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Integer getMockExamId() {
        return mockExamId;
    }

    public void setMockExamId(Integer mockExamId) {
        this.mockExamId = mockExamId;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer scoringSchemeId) {
        this.scoringSchemeId = scoringSchemeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getState() {
        return state != null ? state : status;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(Integer answerVersion) {
        this.answerVersion = answerVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getRubricVersion() {
        return rubricVersion;
    }

    public void setRubricVersion(String rubricVersion) {
        this.rubricVersion = rubricVersion;
    }

    public String getGradingRequestKey() {
        return gradingRequestKey;
    }

    public void setGradingRequestKey(String gradingRequestKey) {
        this.gradingRequestKey = gradingRequestKey;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public BigDecimal getFinalScaleScore() {
        return finalScaleScore;
    }

    public void setFinalScaleScore(BigDecimal finalScaleScore) {
        this.finalScaleScore = finalScaleScore;
    }

    public String getEstimatedOverallCefr() {
        return estimatedOverallCefr;
    }

    public void setEstimatedOverallCefr(String estimatedOverallCefr) {
        this.estimatedOverallCefr = estimatedOverallCefr;
    }

    public String getOverallCefr() {
        return overallCefr != null ? overallCefr : estimatedOverallCefr;
    }

    public void setOverallCefr(String overallCefr) {
        this.overallCefr = overallCefr;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Integer getNextSectionAfterMinute() {
        return nextSectionAfterMinute;
    }

    public void setNextSectionAfterMinute(Integer nextSectionAfterMinute) {
        this.nextSectionAfterMinute = nextSectionAfterMinute;
    }

    public List<SkillResultDTO> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillResultDTO> skills) {
        this.skills = skills;
    }

    public List<UserAnswerDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<UserAnswerDTO> answers) {
        this.answers = answers;
    }

    public Integer attemptId() {
        return getId();
    }

    public Integer userId() {
        return getUserId();
    }

    public Integer examId() {
        return getMockExamId();
    }

    public String examType() {
        return getExamType();
    }

    public String examTitle() {
        return getExamTitle();
    }

    public Integer scoringSchemeId() {
        return getScoringSchemeId();
    }

    public String state() {
        return getState();
    }

    public List<SkillResultDTO> skills() {
        return getSkills();
    }

    public BigDecimal finalScaleScore() {
        return getFinalScaleScore();
    }

    public String overallCefr() {
        return getOverallCefr();
    }

    public Instant submittedAt() {
        return submittedAt != null
                ? submittedAt.atZone(java.time.ZoneId.systemDefault()).toInstant()
                : null;
    }

    public Instant deadline() {
        return getDeadline();
    }

    // Nested classes
    public static class SectionPage {
        private int index;
        private String skill;
        private int durationMinutes;
        private int questionCount;
        private String state;
        private boolean open;

        public SectionPage() {}

        public SectionPage(
                int index,
                String skill,
                int durationMinutes,
                int questionCount,
                String state,
                boolean open) {
            this.index = index;
            this.skill = skill;
            this.durationMinutes = durationMinutes;
            this.questionCount = questionCount;
            this.state = state;
            this.open = open;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public int getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(int questionCount) {
            this.questionCount = questionCount;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public boolean isOpen() {
            return open;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        public int index() {
            return index;
        }

        public String skill() {
            return skill;
        }

        public int durationMinutes() {
            return durationMinutes;
        }

        public int questionCount() {
            return questionCount;
        }

        public String state() {
            return state;
        }

        public boolean open() {
            return open;
        }
    }

    public static class QuestionPage {
        private Integer questionId;
        private String skill;
        private String questionType;
        private String content;
        private String instruction;
        private String readingPassage;
        private int position;
        private Integer preparationSeconds;
        private List<ItemPage> items = new ArrayList<>();
        private List<MediaPage> media = new ArrayList<>();

        public QuestionPage() {}

        public QuestionPage(
                Integer questionId,
                String skill,
                String questionType,
                String content,
                String instruction,
                String readingPassage,
                int position,
                Integer preparationSeconds,
                List<ItemPage> items,
                List<MediaPage> media) {
            this.questionId = questionId;
            this.skill = skill;
            this.questionType = questionType;
            this.content = content;
            this.instruction = instruction;
            this.readingPassage = readingPassage;
            this.position = position;
            this.preparationSeconds = preparationSeconds;
            this.items = items != null ? items : new ArrayList<>();
            this.media = media != null ? media : new ArrayList<>();
        }

        public Integer getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Integer questionId) {
            this.questionId = questionId;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public String getReadingPassage() {
            return readingPassage;
        }

        public void setReadingPassage(String readingPassage) {
            this.readingPassage = readingPassage;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public Integer getPreparationSeconds() {
            return preparationSeconds;
        }

        public void setPreparationSeconds(Integer preparationSeconds) {
            this.preparationSeconds = preparationSeconds;
        }

        public List<ItemPage> getItems() {
            return items;
        }

        public void setItems(List<ItemPage> items) {
            this.items = items;
        }

        public List<MediaPage> getMedia() {
            return media;
        }

        public void setMedia(List<MediaPage> media) {
            this.media = media;
        }

        public Integer questionId() {
            return questionId;
        }

        public String skill() {
            return skill;
        }

        public String questionType() {
            return questionType;
        }

        public String content() {
            return content;
        }

        public String instruction() {
            return instruction;
        }

        public String readingPassage() {
            return readingPassage;
        }

        public int position() {
            return position;
        }

        public Integer preparationSeconds() {
            return preparationSeconds;
        }

        public List<ItemPage> items() {
            return items;
        }

        public List<MediaPage> media() {
            return media;
        }
    }

    public static class ItemPage {
        private Integer itemId;
        private int position;
        private String content;
        private Integer responseSeconds;
        private Integer minWordCount;
        private Integer maxWordCount;
        private List<OptionPage> options = new ArrayList<>();
        private Integer selectedOptionId;
        private Integer userPosition;
        private String textAnswer;
        private String audioUrl;

        public ItemPage() {}

        public ItemPage(
                Integer itemId,
                int position,
                String content,
                Integer responseSeconds,
                Integer minWordCount,
                Integer maxWordCount,
                List<OptionPage> options,
                Integer selectedOptionId,
                Integer userPosition,
                String textAnswer,
                String audioUrl) {
            this.itemId = itemId;
            this.position = position;
            this.content = content;
            this.responseSeconds = responseSeconds;
            this.minWordCount = minWordCount;
            this.maxWordCount = maxWordCount;
            this.options = options != null ? options : new ArrayList<>();
            this.selectedOptionId = selectedOptionId;
            this.userPosition = userPosition;
            this.textAnswer = textAnswer;
            this.audioUrl = audioUrl;
        }

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getResponseSeconds() {
            return responseSeconds;
        }

        public void setResponseSeconds(Integer responseSeconds) {
            this.responseSeconds = responseSeconds;
        }

        public Integer getMinWordCount() {
            return minWordCount;
        }

        public void setMinWordCount(Integer minWordCount) {
            this.minWordCount = minWordCount;
        }

        public Integer getMaxWordCount() {
            return maxWordCount;
        }

        public void setMaxWordCount(Integer maxWordCount) {
            this.maxWordCount = maxWordCount;
        }

        public List<OptionPage> getOptions() {
            return options;
        }

        public void setOptions(List<OptionPage> options) {
            this.options = options;
        }

        public Integer getSelectedOptionId() {
            return selectedOptionId;
        }

        public void setSelectedOptionId(Integer selectedOptionId) {
            this.selectedOptionId = selectedOptionId;
        }

        public Integer getUserPosition() {
            return userPosition;
        }

        public void setUserPosition(Integer userPosition) {
            this.userPosition = userPosition;
        }

        public String getTextAnswer() {
            return textAnswer;
        }

        public void setTextAnswer(String textAnswer) {
            this.textAnswer = textAnswer;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }

        public Integer itemId() {
            return itemId;
        }

        public int position() {
            return position;
        }

        public String content() {
            return content;
        }

        public Integer responseSeconds() {
            return responseSeconds;
        }

        public Integer minWordCount() {
            return minWordCount;
        }

        public Integer maxWordCount() {
            return maxWordCount;
        }

        public List<OptionPage> options() {
            return options;
        }

        public Integer selectedOptionId() {
            return selectedOptionId;
        }

        public Integer userPosition() {
            return userPosition;
        }

        public String textAnswer() {
            return textAnswer;
        }

        public String audioUrl() {
            return audioUrl;
        }
    }

    public static class OptionPage {
        private Integer optionId;
        private String optionCode;
        private String content;
        private int position;

        public OptionPage() {}

        public OptionPage(Integer optionId, String optionCode, String content, int position) {
            this.optionId = optionId;
            this.optionCode = optionCode;
            this.content = content;
            this.position = position;
        }

        public Integer getOptionId() {
            return optionId;
        }

        public void setOptionId(Integer optionId) {
            this.optionId = optionId;
        }

        public String getOptionCode() {
            return optionCode;
        }

        public void setOptionCode(String optionCode) {
            this.optionCode = optionCode;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public Integer optionId() {
            return optionId;
        }

        public String optionCode() {
            return optionCode;
        }

        public String content() {
            return content;
        }

        public int position() {
            return position;
        }
    }

    public static class MediaPage {
        private Integer mediaId;
        private String mediaType;
        private String mediaUrl;
        private Integer maxPlayCount;
        private Integer playCount;
        private String tokenHash;

        public MediaPage() {}

        public MediaPage(
                Integer mediaId,
                String mediaType,
                String mediaUrl,
                Integer maxPlayCount,
                Integer playCount,
                String tokenHash) {
            this.mediaId = mediaId;
            this.mediaType = mediaType;
            this.mediaUrl = mediaUrl;
            this.maxPlayCount = maxPlayCount;
            this.playCount = playCount;
            this.tokenHash = tokenHash;
        }

        public Integer getMediaId() {
            return mediaId;
        }

        public void setMediaId(Integer mediaId) {
            this.mediaId = mediaId;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public String getMediaUrl() {
            return mediaUrl;
        }

        public void setMediaUrl(String mediaUrl) {
            this.mediaUrl = mediaUrl;
        }

        public Integer getMaxPlayCount() {
            return maxPlayCount;
        }

        public void setMaxPlayCount(Integer maxPlayCount) {
            this.maxPlayCount = maxPlayCount;
        }

        public Integer getPlayCount() {
            return playCount;
        }

        public void setPlayCount(Integer playCount) {
            this.playCount = playCount;
        }

        public String getTokenHash() {
            return tokenHash;
        }

        public void setTokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
        }

        public Integer mediaId() {
            return mediaId;
        }

        public String mediaType() {
            return mediaType;
        }

        public String mediaUrl() {
            return mediaUrl;
        }

        public Integer maxPlayCount() {
            return maxPlayCount;
        }

        public Integer playCount() {
            return playCount;
        }

        public String tokenHash() {
            return tokenHash;
        }
    }

    public static class AttemptPage {
        private Integer attemptId;
        private String title;
        private String examType;
        private Long remainingSeconds;
        private List<QuestionPage> questions = new ArrayList<>();
        private Instant deadline;
        private Integer nextSectionAfterMinute;
        private List<SectionPage> sections = new ArrayList<>();

        public AttemptPage() {}

        public AttemptPage(
                Integer attemptId,
                String title,
                String examType,
                Long remainingSeconds,
                List<QuestionPage> questions,
                Instant deadline,
                Integer nextSectionAfterMinute,
                List<SectionPage> sections) {
            this.attemptId = attemptId;
            this.title = title;
            this.examType = examType;
            this.remainingSeconds = remainingSeconds;
            this.questions = questions != null ? questions : new ArrayList<>();
            this.deadline = deadline;
            this.nextSectionAfterMinute = nextSectionAfterMinute;
            this.sections = sections != null ? sections : new ArrayList<>();
        }

        public Integer getAttemptId() {
            return attemptId;
        }

        public void setAttemptId(Integer attemptId) {
            this.attemptId = attemptId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public Long getRemainingSeconds() {
            return remainingSeconds;
        }

        public void setRemainingSeconds(Long remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }

        public List<QuestionPage> getQuestions() {
            return questions;
        }

        public void setQuestions(List<QuestionPage> questions) {
            this.questions = questions;
        }

        public Instant getDeadline() {
            return deadline;
        }

        public void setDeadline(Instant deadline) {
            this.deadline = deadline;
        }

        public Integer getNextSectionAfterMinute() {
            return nextSectionAfterMinute;
        }

        public void setNextSectionAfterMinute(Integer nextSectionAfterMinute) {
            this.nextSectionAfterMinute = nextSectionAfterMinute;
        }

        public List<SectionPage> getSections() {
            return sections;
        }

        public void setSections(List<SectionPage> sections) {
            this.sections = sections;
        }

        public Integer attemptId() {
            return attemptId;
        }

        public String title() {
            return title;
        }

        public String examType() {
            return examType;
        }

        public Long remainingSeconds() {
            return remainingSeconds;
        }

        public List<QuestionPage> questions() {
            return questions;
        }

        public Instant deadline() {
            return deadline;
        }

        public Integer nextSectionAfterMinute() {
            return nextSectionAfterMinute;
        }

        public List<SectionPage> sections() {
            return sections;
        }
    }

    public static class AttemptReview {
        private ExamAttemptDTO attempt;
        private List<SkillResultDTO> skillResults = new ArrayList<>();
        private List<ReviewQuestion> questions = new ArrayList<>();

        public AttemptReview() {}

        public AttemptReview(
                ExamAttemptDTO attempt,
                List<SkillResultDTO> skillResults,
                List<ReviewQuestion> questions) {
            this.attempt = attempt;
            this.skillResults = skillResults != null ? skillResults : new ArrayList<>();
            this.questions = questions != null ? questions : new ArrayList<>();
        }

        public ExamAttemptDTO getAttempt() {
            return attempt;
        }

        public void setAttempt(ExamAttemptDTO attempt) {
            this.attempt = attempt;
        }

        public List<SkillResultDTO> getSkillResults() {
            return skillResults;
        }

        public void setSkillResults(List<SkillResultDTO> skillResults) {
            this.skillResults = skillResults;
        }

        public List<ReviewQuestion> getQuestions() {
            return questions;
        }

        public void setQuestions(List<ReviewQuestion> questions) {
            this.questions = questions;
        }

        public ExamAttemptDTO attempt() {
            return attempt;
        }

        public List<SkillResultDTO> skillResults() {
            return skillResults;
        }

        public List<ReviewQuestion> questions() {
            return questions;
        }
    }

    public static class ReviewQuestion {
        private Integer questionId;
        private String skill;
        private String questionType;
        private String content;
        private String instruction;
        private String readingPassage;
        private int position;
        private List<ReviewItem> items = new ArrayList<>();
        private List<MediaPage> media = new ArrayList<>();

        public ReviewQuestion() {}

        public ReviewQuestion(
                Integer questionId,
                String skill,
                String questionType,
                String content,
                String instruction,
                String readingPassage,
                int position,
                List<ReviewItem> items,
                List<MediaPage> media) {
            this.questionId = questionId;
            this.skill = skill;
            this.questionType = questionType;
            this.content = content;
            this.instruction = instruction;
            this.readingPassage = readingPassage;
            this.position = position;
            this.items = items != null ? items : new ArrayList<>();
            this.media = media != null ? media : new ArrayList<>();
        }

        public Integer getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Integer questionId) {
            this.questionId = questionId;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public String getReadingPassage() {
            return readingPassage;
        }

        public void setReadingPassage(String readingPassage) {
            this.readingPassage = readingPassage;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public List<ReviewItem> getItems() {
            return items;
        }

        public void setItems(List<ReviewItem> items) {
            this.items = items;
        }

        public List<MediaPage> getMedia() {
            return media;
        }

        public void setMedia(List<MediaPage> media) {
            this.media = media;
        }

        public Integer questionId() {
            return questionId;
        }

        public String skill() {
            return skill;
        }

        public String questionType() {
            return questionType;
        }

        public String content() {
            return content;
        }

        public String instruction() {
            return instruction;
        }

        public String readingPassage() {
            return readingPassage;
        }

        public int position() {
            return position;
        }

        public List<ReviewItem> items() {
            return items;
        }

        public List<MediaPage> media() {
            return media;
        }
    }

    public static class ReviewItem {
        private Integer itemId;
        private int position;
        private String content;
        private List<OptionPage> options = new ArrayList<>();
        private Integer selectedOptionId;
        private Integer correctOptionId;
        private Integer userPosition;
        private Integer correctPosition;
        private String textAnswer;
        private String audioUrl;
        private Boolean correct;
        private BigDecimal score;

        public ReviewItem() {}

        public ReviewItem(
                Integer itemId,
                int position,
                String content,
                List<OptionPage> options,
                Integer selectedOptionId,
                Integer correctOptionId,
                Integer userPosition,
                Integer correctPosition,
                String textAnswer,
                String audioUrl,
                Boolean correct,
                BigDecimal score) {
            this.itemId = itemId;
            this.position = position;
            this.content = content;
            this.options = options != null ? options : new ArrayList<>();
            this.selectedOptionId = selectedOptionId;
            this.correctOptionId = correctOptionId;
            this.userPosition = userPosition;
            this.correctPosition = correctPosition;
            this.textAnswer = textAnswer;
            this.audioUrl = audioUrl;
            this.correct = correct;
            this.score = score;
        }

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<OptionPage> getOptions() {
            return options;
        }

        public void setOptions(List<OptionPage> options) {
            this.options = options;
        }

        public Integer getSelectedOptionId() {
            return selectedOptionId;
        }

        public void setSelectedOptionId(Integer selectedOptionId) {
            this.selectedOptionId = selectedOptionId;
        }

        public Integer getCorrectOptionId() {
            return correctOptionId;
        }

        public void setCorrectOptionId(Integer correctOptionId) {
            this.correctOptionId = correctOptionId;
        }

        public Integer getUserPosition() {
            return userPosition;
        }

        public void setUserPosition(Integer userPosition) {
            this.userPosition = userPosition;
        }

        public Integer getCorrectPosition() {
            return correctPosition;
        }

        public void setCorrectPosition(Integer correctPosition) {
            this.correctPosition = correctPosition;
        }

        public String getTextAnswer() {
            return textAnswer;
        }

        public void setTextAnswer(String textAnswer) {
            this.textAnswer = textAnswer;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }

        public Boolean getCorrect() {
            return correct;
        }

        public void setCorrect(Boolean correct) {
            this.correct = correct;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }

        public Integer itemId() {
            return itemId;
        }

        public int position() {
            return position;
        }

        public String content() {
            return content;
        }

        public List<OptionPage> options() {
            return options;
        }

        public Integer selectedOptionId() {
            return selectedOptionId;
        }

        public Integer correctOptionId() {
            return correctOptionId;
        }

        public Integer userPosition() {
            return userPosition;
        }

        public Integer correctPosition() {
            return correctPosition;
        }

        public String textAnswer() {
            return textAnswer;
        }

        public String audioUrl() {
            return audioUrl;
        }

        public Boolean correct() {
            return correct;
        }

        public BigDecimal score() {
            return score;
        }
    }

    public static class MediaResource {
        private String mediaType;
        private org.springframework.core.io.Resource resource;

        public MediaResource() {}

        public MediaResource(String mediaType, org.springframework.core.io.Resource resource) {
            this.mediaType = mediaType;
            this.resource = resource;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public org.springframework.core.io.Resource getResource() {
            return resource;
        }

        public void setResource(org.springframework.core.io.Resource resource) {
            this.resource = resource;
        }

        public String mediaType() {
            return mediaType;
        }

        public org.springframework.core.io.Resource resource() {
            return resource;
        }
    }

    public static class SnapshotSchemeInfo {
        private Integer scoringSchemeId;
        private String skill;

        public SnapshotSchemeInfo() {}

        public SnapshotSchemeInfo(Integer scoringSchemeId, String skill) {
            this.scoringSchemeId = scoringSchemeId;
            this.skill = skill;
        }

        public Integer getScoringSchemeId() {
            return scoringSchemeId;
        }

        public void setScoringSchemeId(Integer scoringSchemeId) {
            this.scoringSchemeId = scoringSchemeId;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public Integer scoringSchemeId() {
            return scoringSchemeId;
        }

        public String skill() {
            return skill;
        }
    }
}
