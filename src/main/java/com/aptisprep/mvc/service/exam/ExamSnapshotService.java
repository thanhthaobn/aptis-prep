package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.repository.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Quản lý bản sao bất biến của đề được dùng trong một lượt thi.
 *
 * <p>Các bảng đề/câu hỏi hiện tại có thể bị chỉnh sửa sau khi học viên bắt đầu. Luồng resume và
 * chấm điểm vẫn phải thấy đúng nội dung ban đầu, nên service này copy câu hỏi, item, option,
 * media, metadata chấm và các mốc thời gian section vào bảng snapshot lúc bắt đầu.
 */
public interface ExamSnapshotService {
    public record SectionWindow(int start, int end) {
        public boolean contains(LocalDateTime started, LocalDateTime now) {
            return !now.isBefore(started.plusMinutes(start))
                    && now.isBefore(started.plusMinutes(end));
        }
    }

    public Optional<MockExam> frozenExam(Integer attemptId);

    public void preserveLegacyAttempt(
            ExamAttempt attempt, MockExam exam, List<MockExamQuestion> links);

    public ExamAttemptSnapshot require(Integer attemptId);

    public Integer durationMinutes(Integer attemptId, Integer fallback);

    public void updateAnswerVersion(Integer attemptId, int version);

    public void createSnapshot(ExamAttempt attempt, MockExam exam, List<MockExamQuestion> links);

    public LocalDateTime sectionClockStart(ExamAttempt attempt);

    public void advanceSectionClock(Integer attemptId, Duration remaining);

    public List<ExamQuestionSnapshot> lockedQuestionEntities(Integer attemptId);

    public ExamQuestionSnapshot lockedQuestionEntity(Integer attemptId, Integer questionId);

    public ExamMediaSnapshot lockedMediaEntity(Integer attemptId, Integer mediaId);

    public void requireOpenSection(ExamAttempt attempt, ExamQuestionSnapshot question);
}
