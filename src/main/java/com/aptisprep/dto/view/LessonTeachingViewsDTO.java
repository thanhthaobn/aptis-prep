package com.aptisprep.dto.view;

import java.time.LocalDateTime;
import java.util.List;

public final class LessonTeachingViewsDTO {
    private LessonTeachingViewsDTO() {}

    public record ReleaseStateDTO(
            boolean transcript, boolean sample, String transcriptHint, String sampleHint) {}

    public record HistoricalMediaDTO(String path, String kind) {}

    public record CheckViewDTO(
            Integer id, String prompt, List<String> options, List<Integer> optionValues) {}

    public record AnswerFeedbackDTO(
            String prompt,
            String selected,
            String correct,
            boolean right,
            String explanation,
            List<String> options) {}

    public record FeedbackDTO(int score, int total, List<AnswerFeedbackDTO> answers) {
        public boolean passed() {
            return LessonTeachingViewsDTO.passed(score, total);
        }
    }

    public record AttemptViewDTO(
            Integer id,
            int score,
            int total,
            LocalDateTime submittedAt,
            List<AnswerFeedbackDTO> answers,
            LessonContextSnapshotEntityDTO context) {
        public boolean passed() {
            return LessonTeachingViewsDTO.passed(score, total);
        }
    }

    public record LessonStatsDTO(
            long started,
            long completed,
            long audioListeners,
            long attempts,
            double averagePercent) {}

    public record CompletionStateDTO(
            boolean hasQuestions,
            boolean checkSubmitted,
            boolean needsResponse,
            boolean responseSaved,
            boolean needsAudio,
            boolean audioPlayed,
            boolean eligible) {
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
    }

    private static boolean passed(int score, int total) {
        return total > 0 && score * 100 >= total * 80;
    }
}
