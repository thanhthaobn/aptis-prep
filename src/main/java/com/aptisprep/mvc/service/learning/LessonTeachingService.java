package com.aptisprep.mvc.service.learning;

import com.aptisprep.mvc.dto.LessonContextSnapshotDTO;
import com.aptisprep.mvc.dto.LessonWorkSubmissionDTO;
import com.aptisprep.mvc.dto.LessonResponseDTO;


import com.aptisprep.mvc.config.AiProperties;
import com.aptisprep.mvc.dto.LessonDTO;
import com.aptisprep.mvc.dto.LessonCheckDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.grading.AiAssessmentService;
import com.aptisprep.mvc.dto.AiAssessmentJobDTO;
import com.aptisprep.mvc.service.storage.StorageService;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Objects;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional

public interface LessonTeachingService {
        public record ReleaseState(
                boolean transcript, boolean sample, String transcriptHint, String sampleHint) {}
    
        public record HistoricalMedia(String path, String kind) {}
    
        public record AttemptView(
                Integer id,
                int score,
                int total,
                LocalDateTime submittedAt,
                List<AnswerFeedback> answers,
                LessonContextSnapshotDTO context) {}
    
        public record CompletionState(
                boolean hasQuestions,
                boolean checkSubmitted,
                boolean needsResponse,
                boolean responseSaved,
                boolean needsAudio,
                boolean audioPlayed,
                boolean eligible) {
            public String message(String skill) {
                if (!checkSubmitted && hasQuestions)
                    return "Hãy đạt ít nhất 60% phần kiểm tra kiến thức";
                if (needsResponse && !responseSaved)
                    return "Speaking".equals(skill)
                            ? "Hãy ghi âm và lưu bản ghi để hoàn thành bài nói"
                            : "Hãy nhập và nộp bài viết để hoàn thành bài học";
                if (needsAudio && !audioPlayed) return "Hãy nghe ít nhất 90% bài nghe";
                return "Đã đủ điều kiện hoàn thành";
            }
        }
    
        public record LessonStats(
                long started,
                long completed,
                long audioListeners,
                long attempts,
                double averagePercent) {}
    
        public record CheckView(Integer id, String prompt, List<String> options) {}
    
        public record AnswerFeedback(
                String prompt,
                String selected,
                String correct,
                boolean right,
                String explanation,
                List<String> options) {}
    
        public record Feedback(int score, int total, List<AnswerFeedback> answers) {}

    public ReleaseState releaseState(Integer id, String email);
    public boolean canAssess(Integer id, String email);
    public boolean aiAvailable();
    public HistoricalMedia historicalMedia( Integer lessonId, Integer historyId, String kind, String email, boolean quiz);
    public void resetAudioEngagement(Integer id);
    public LessonResponseDTO savedWork(Integer lessonId, String email);
    public void saveRecording(Integer lessonId, MultipartFile file, String email);
    public String recordingAudio(Integer lessonId, String email);
    public List<AttemptView> history(Integer lessonId, String email);
    public List<LessonWorkSubmissionDTO> submissionHistory(Integer lessonId, String email);
    public List<LessonWorkSubmissionDTO> assessableSubmissions(Integer lessonId, String email);
    public String submissionAudio(Integer lessonId, Integer submissionId, String email);
    public void submitResponse(Integer lessonId, String text, String email);
    public CompletionState completion(Integer lessonId, String email);
    public void section(Integer lessonId, String section, String email);
    public String lastSection(Integer lessonId, String email);
    public LessonStats stats(Integer lessonId);
    public String response(Integer lessonId, String email);
    public void saveResponse(Integer lessonId, String text, String email);
    public String assess(Integer lessonId, Integer submissionId, String email);
    public List<CheckView> questions(Integer id);
    public List<LessonCheckDTO> editorQuestions(Integer id);
    public void validate(LessonDTO f);
    public void saveChecks(Integer id, List<LessonCheckDTO> forms);
    public String videoId(String url);
    public Feedback grade(Integer id, Map<String, String> answers, String email);
}

