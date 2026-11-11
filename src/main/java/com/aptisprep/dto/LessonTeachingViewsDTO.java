package com.aptisprep.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class LessonTeachingViewsDTO {
    private LessonTeachingViewsDTO() {}

    public static class ReleaseStateDTO {
        private boolean transcript;
        private boolean sample;
        private String transcriptHint;
        private String sampleHint;

        public ReleaseStateDTO() {}

        public ReleaseStateDTO(
                boolean transcript, boolean sample, String transcriptHint, String sampleHint) {
            this.transcript = transcript;
            this.sample = sample;
            this.transcriptHint = transcriptHint;
            this.sampleHint = sampleHint;
        }

        public boolean isTranscript() {
            return transcript;
        }

        public void setTranscript(boolean transcript) {
            this.transcript = transcript;
        }

        public boolean transcript() {
            return transcript;
        }

        public boolean isSample() {
            return sample;
        }

        public void setSample(boolean sample) {
            this.sample = sample;
        }

        public boolean sample() {
            return sample;
        }

        public String getTranscriptHint() {
            return transcriptHint;
        }

        public void setTranscriptHint(String transcriptHint) {
            this.transcriptHint = transcriptHint;
        }

        public String transcriptHint() {
            return transcriptHint;
        }

        public String getSampleHint() {
            return sampleHint;
        }

        public void setSampleHint(String sampleHint) {
            this.sampleHint = sampleHint;
        }

        public String sampleHint() {
            return sampleHint;
        }
    }

    public static class HistoricalMediaDTO {
        private String path;
        private String kind;

        public HistoricalMediaDTO() {}

        public HistoricalMediaDTO(String path, String kind) {
            this.path = path;
            this.kind = kind;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String kind() {
            return kind;
        }
    }

    public static class CheckViewDTO {
        private Integer id;
        private String prompt;
        private List<String> options;
        private List<Integer> optionValues;

        public CheckViewDTO() {}

        public CheckViewDTO(
                Integer id, String prompt, List<String> options, List<Integer> optionValues) {
            this.id = id;
            this.prompt = prompt;
            this.options = options;
            this.optionValues = optionValues;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String prompt() {
            return prompt;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public List<String> options() {
            return options;
        }

        public List<Integer> getOptionValues() {
            return optionValues;
        }

        public void setOptionValues(List<Integer> optionValues) {
            this.optionValues = optionValues;
        }

        public List<Integer> optionValues() {
            return optionValues;
        }
    }

    public static class AnswerFeedbackDTO {
        private String prompt;
        private String selected;
        private String correct;
        private boolean right;
        private String explanation;
        private List<String> options;

        public AnswerFeedbackDTO() {}

        public AnswerFeedbackDTO(
                String prompt,
                String selected,
                String correct,
                boolean right,
                String explanation,
                List<String> options) {
            this.prompt = prompt;
            this.selected = selected;
            this.correct = correct;
            this.right = right;
            this.explanation = explanation;
            this.options = options;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String prompt() {
            return prompt;
        }

        public String getSelected() {
            return selected;
        }

        public void setSelected(String selected) {
            this.selected = selected;
        }

        public String selected() {
            return selected;
        }

        public String getCorrect() {
            return correct;
        }

        public void setCorrect(String correct) {
            this.correct = correct;
        }

        public String correct() {
            return correct;
        }

        public boolean isRight() {
            return right;
        }

        public void setRight(boolean right) {
            this.right = right;
        }

        public boolean right() {
            return right;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String explanation() {
            return explanation;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public List<String> options() {
            return options;
        }
    }

    public static class FeedbackDTO {
        private int score;
        private int total;
        private List<AnswerFeedbackDTO> answers;

        public FeedbackDTO() {}

        public FeedbackDTO(int score, int total, List<AnswerFeedbackDTO> answers) {
            this.score = score;
            this.total = total;
            this.answers = answers;
        }

        public boolean passed() {
            return total > 0 && score * 100 >= total * 80;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int score() {
            return score;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int total() {
            return total;
        }

        public List<AnswerFeedbackDTO> getAnswers() {
            return answers;
        }

        public void setAnswers(List<AnswerFeedbackDTO> answers) {
            this.answers = answers;
        }

        public List<AnswerFeedbackDTO> answers() {
            return answers;
        }
    }

    public static class AttemptViewDTO {
        private Integer id;
        private int score;
        private int total;
        private LocalDateTime submittedAt;
        private List<AnswerFeedbackDTO> answers;
        private LessonContextSnapshot context;

        public AttemptViewDTO() {}

        public AttemptViewDTO(
                Integer id,
                int score,
                int total,
                LocalDateTime submittedAt,
                List<AnswerFeedbackDTO> answers,
                LessonContextSnapshot context) {
            this.id = id;
            this.score = score;
            this.total = total;
            this.submittedAt = submittedAt;
            this.answers = answers;
            this.context = context;
        }

        public boolean passed() {
            return total > 0 && score * 100 >= total * 80;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int score() {
            return score;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int total() {
            return total;
        }

        public LocalDateTime getSubmittedAt() {
            return submittedAt;
        }

        public void setSubmittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
        }

        public LocalDateTime submittedAt() {
            return submittedAt;
        }

        public List<AnswerFeedbackDTO> getAnswers() {
            return answers;
        }

        public void setAnswers(List<AnswerFeedbackDTO> answers) {
            this.answers = answers;
        }

        public List<AnswerFeedbackDTO> answers() {
            return answers;
        }

        public LessonContextSnapshot getContext() {
            return context;
        }

        public void setContext(LessonContextSnapshot context) {
            this.context = context;
        }

        public LessonContextSnapshot context() {
            return context;
        }
    }

    public static class LessonStatsDTO {
        private long started;
        private long completed;
        private long audioListeners;
        private long attempts;
        private double averagePercent;

        public LessonStatsDTO() {}

        public LessonStatsDTO(
                long started,
                long completed,
                long audioListeners,
                long attempts,
                double averagePercent) {
            this.started = started;
            this.completed = completed;
            this.audioListeners = audioListeners;
            this.attempts = attempts;
            this.averagePercent = averagePercent;
        }

        public long getStarted() {
            return started;
        }

        public void setStarted(long started) {
            this.started = started;
        }

        public long started() {
            return started;
        }

        public long getCompleted() {
            return completed;
        }

        public void setCompleted(long completed) {
            this.completed = completed;
        }

        public long completed() {
            return completed;
        }

        public long getAudioListeners() {
            return audioListeners;
        }

        public void setAudioListeners(long audioListeners) {
            this.audioListeners = audioListeners;
        }

        public long audioListeners() {
            return audioListeners;
        }

        public long getAttempts() {
            return attempts;
        }

        public void setAttempts(long attempts) {
            this.attempts = attempts;
        }

        public long attempts() {
            return attempts;
        }

        public double getAveragePercent() {
            return averagePercent;
        }

        public void setAveragePercent(double averagePercent) {
            this.averagePercent = averagePercent;
        }

        public double averagePercent() {
            return averagePercent;
        }
    }

    public static class CompletionStateDTO {
        private boolean hasQuestions;
        private boolean checkSubmitted;
        private boolean needsResponse;
        private boolean responseSaved;
        private boolean needsAudio;
        private boolean audioPlayed;
        private boolean eligible;

        public CompletionStateDTO() {}

        public CompletionStateDTO(
                boolean hasQuestions,
                boolean checkSubmitted,
                boolean needsResponse,
                boolean responseSaved,
                boolean needsAudio,
                boolean audioPlayed,
                boolean eligible) {
            this.hasQuestions = hasQuestions;
            this.checkSubmitted = checkSubmitted;
            this.needsResponse = needsResponse;
            this.responseSaved = responseSaved;
            this.needsAudio = needsAudio;
            this.audioPlayed = audioPlayed;
            this.eligible = eligible;
        }

        public String message(String skill) {
            if (!checkSubmitted && hasQuestions)
                return "Hãy đạt ít nhất 80% phần kiểm tra kiến thức";
            if (needsResponse && !responseSaved)
                return "Speaking".equals(skill)
                        ? "Hãy ghi âm và lưu bản ghi để hoàn thành bài nói"
                        : "Hãy nhập và nộp bài viết để hoàn thành bài học";
            if (needsAudio && !audioPlayed) return "Hãy nghe ít nhất 90% bài nghe";
            return "Đã đủ điều kiện hoàn thành";
        }

        public boolean isHasQuestions() {
            return hasQuestions;
        }

        public void setHasQuestions(boolean hasQuestions) {
            this.hasQuestions = hasQuestions;
        }

        public boolean hasQuestions() {
            return hasQuestions;
        }

        public boolean isCheckSubmitted() {
            return checkSubmitted;
        }

        public void setCheckSubmitted(boolean checkSubmitted) {
            this.checkSubmitted = checkSubmitted;
        }

        public boolean checkSubmitted() {
            return checkSubmitted;
        }

        public boolean isNeedsResponse() {
            return needsResponse;
        }

        public void setNeedsResponse(boolean needsResponse) {
            this.needsResponse = needsResponse;
        }

        public boolean needsResponse() {
            return needsResponse;
        }

        public boolean isResponseSaved() {
            return responseSaved;
        }

        public void setResponseSaved(boolean responseSaved) {
            this.responseSaved = responseSaved;
        }

        public boolean responseSaved() {
            return responseSaved;
        }

        public boolean isNeedsAudio() {
            return needsAudio;
        }

        public void setNeedsAudio(boolean needsAudio) {
            this.needsAudio = needsAudio;
        }

        public boolean needsAudio() {
            return needsAudio;
        }

        public boolean isAudioPlayed() {
            return audioPlayed;
        }

        public void setAudioPlayed(boolean audioPlayed) {
            this.audioPlayed = audioPlayed;
        }

        public boolean audioPlayed() {
            return audioPlayed;
        }

        public boolean isEligible() {
            return eligible;
        }

        public void setEligible(boolean eligible) {
            this.eligible = eligible;
        }

        public boolean eligible() {
            return eligible;
        }
    }
}
