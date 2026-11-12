package com.aptisprep.mvc.service.exam;
import com.aptisprep.mvc.support.contract.*;


import static com.aptisprep.mvc.support.contract.GradingContextFormatter.DEFAULT_RUBRIC_VERSION;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.support.contract.GradingContextFormatter;
import com.aptisprep.mvc.support.contract.IntegrationModels.*;
import com.aptisprep.mvc.service.scoring.ScoringPolicyService;
import com.aptisprep.mvc.service.grading.AttemptFinalizationService;
import com.aptisprep.mvc.service.grading.ObjectiveGradingService;
import com.aptisprep.mvc.service.storage.StorageService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Application service điều phối toàn bộ vòng đời lượt làm bài.
 *
 * <p>Một attempt không chỉ là một row trong {@code exam_attempts}: nó còn sở hữu snapshot đề đã
 * khóa, đáp án, thời gian section, quyền truy cập media và trạng thái chấm. Vì vậy các method
 * public kiểm tra quyền/trạng thái trước khi ủy quyền cho các service chuyên trách.
 */

public interface ExamAttemptService extends ExamAttemptCommandService {
        public record AttemptReview(String title, List<QuestionPage> questions) {}
    
        public record SectionPage(
                int index,
                String skill,
                int durationMinutes,
                int questionCount,
                String state,
                boolean open) {}
    
        public record AttemptPage(
                Integer attemptId,
                String title,
                String examType,
                Long remainingSeconds,
                List<QuestionPage> questions,
                Instant deadline,
                Integer nextSectionAfterMinute,
                String sectionSkill,
                List<SectionPage> sections) {
            public boolean nextSection() {
                return nextSectionAfterMinute != null;
            }
    
            public AttemptPage(
                    Integer attemptId,
                    String title,
                    String examType,
                    Long remainingSeconds,
                    List<QuestionPage> questions,
                    Instant deadline) {
                this(
                        attemptId,
                        title,
                        examType,
                        remainingSeconds,
                        questions,
                        deadline,
                        null,
                        null,
                        List.of());
            }
    
            public AttemptPage(
                    Integer attemptId,
                    String title,
                    Long remainingSeconds,
                    List<QuestionPage> questions,
                    Instant deadline) {
                this(attemptId, title, null, remainingSeconds, questions, deadline);
            }
    
            public AttemptPage(
                    Integer attemptId,
                    String title,
                    Long remainingSeconds,
                    List<QuestionPage> questions) {
                this(attemptId, title, null, remainingSeconds, questions, null);
            }
    
            public int totalItems() {
                return questions == null
                        ? 0
                        : questions.stream()
                                .mapToInt(
                                        question ->
                                                question.items() == null ? 0 : question.items().size())
                                .sum();
            }
    
            public int answeredItems() {
                return questions == null
                        ? 0
                        : questions.stream()
                                .flatMap(
                                        question ->
                                                question.items() == null
                                                        ? Stream.empty()
                                                        : question.items().stream())
                                .filter(streamValue -> streamValue.answered())
                                .mapToInt(item -> 1)
                                .sum();
            }
        }
    
        public record QuestionPage(
                Integer questionId,
                String content,
                String instruction,
                String readingPassage,
                String skill,
                String type,
                int position,
                List<ItemPage> items,
                List<MediaPage> media) {}
    
        public record ItemPage(
                Integer itemId,
                int position,
                String content,
                Integer responseSeconds,
                List<OptionPage> options,
                Integer selectedOptionId,
                Integer userPosition,
                @JsonIgnore Integer correctPosition,
                String textAnswer,
                boolean audioSaved,
                Integer minWordCount,
                Integer maxWordCount) {
            public boolean answered() {
                return selectedOptionId != null
                        || userPosition != null
                        || (textAnswer != null && !textAnswer.isBlank())
                        || audioSaved;
            }
        }
    
        public record OptionPage(
                Integer id, String code, String content, @JsonIgnore boolean correct) {}
    
        public record MediaPage(Integer id, String type, String filename, String contentType) {}
    
        public record MediaPlayResult(
                Integer mediaId, int playCount, int maxPlayCount, String contentToken) {}
    
        public record MediaResource(Resource resource, String contentType) {}

    public AttemptHandle start(Integer userId, Integer examId, String requestKey);
    public Integer latestResumableAttemptId(Integer userId, Integer examId);
    public AttemptHandle startPlacement(Integer userId, Integer examId, String requestKey);
    public boolean hasInProgressPlacement(Integer userId, String skill);
    public boolean hasInProgressPlacement(Integer userId);
    public boolean hasPlacementAttempt(Integer userId);
    public AttemptHandle startCheckpoint( Integer userId, Integer examId, Integer pathId, String requestKey);
    public AttemptHandle startCheckpointRetake( Integer userId, Integer examId, Integer pathId, String requestKey);
    public Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId);
    public void saveSpeakingAudio( Integer userId, Integer attemptId, Integer questionId, Integer itemId, MultipartFile audioFile);
    public MediaPlayResult playMedia(Integer userId, Integer attemptId, Integer mediaId);
    public MediaResource mediaContent( Integer userId, Integer attemptId, Integer mediaId, String contentToken);
    public MediaResource speakingAudio(Integer userId, Integer attemptId, Integer answerId);
    public void saveAnswer( Integer userId, Integer attemptId, Integer questionId, Integer itemId, Integer selectedOptionId, Integer userPosition, String textAnswer);
    public void submit(Integer userId, Integer attemptId, String requestKey);
    public void discard(Integer userId, Integer attemptId);
    public AttemptResult result(Integer userId, Integer attemptId);
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter);
    public default HistoryPage history(Integer userId, int page, int size) {
        return history(userId, page, size, HistoryFilter.empty());
    }
    public AdminHistoryPage adminHistory( String adminEmail, int page, int size, AdminHistoryFilter filter);
    public AttemptResult adminResult(String adminEmail, Integer attemptId);
    public void retryFailedGrading(Integer userId, Integer attemptId, String requestKey);
    public boolean canRegradeEmpty(Integer userId, Integer attemptId);
    public void regradeEmpty(Integer userId, Integer attemptId);
    public void advanceSection(Integer userId, Integer attemptId, int sectionEndMinute);
    public AttemptReview review(Integer userId, Integer attemptId);
    public AttemptReview adminReview(String adminEmail, Integer attemptId);
    public void adminDiscard(String adminEmail, Integer attemptId);
    public MediaResource reviewMedia(Integer userId, Integer attemptId, Integer mediaId);
    public MediaResource reviewRecording( Integer userId, Integer attemptId, Integer questionId, Integer itemId);
    public AttemptPage page(Integer userId, Integer attemptId);
}

