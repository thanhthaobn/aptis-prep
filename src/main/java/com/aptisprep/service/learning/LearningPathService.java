package com.aptisprep.service.learning;

import com.aptisprep.entity.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.ExamCatalogContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.contract.LearningPathContract;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningPathService implements LearningPathContract {
    private static final List<String> SKILLS = List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    private final AccountAccessService accounts;
    private final UserGoalRepository goals;
    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final LearningPathRepository paths;
    private final LearningPathItemRepository items;
    private final ExamCatalogContract exams;
    private final ObjectProvider<ExamAttemptContract> attempts;

    public LearningPathService(AccountAccessService accounts, UserGoalRepository goals, LessonRepository lessons,
            LessonProgressRepository progress, LearningPathRepository paths, LearningPathItemRepository items,
            ExamCatalogContract exams, ObjectProvider<ExamAttemptContract> attempts) {
        this.accounts = accounts; this.goals = goals; this.lessons = lessons; this.progress = progress; this.paths = paths;
        this.items = items; this.exams = exams; this.attempts = attempts;
    }

    @Override
    @Transactional
    public void ensurePaths(Integer userId) {
        LearnerContext context = accounts.context(userId);
        Map<String, String> targets = new LinkedHashMap<>();
        context.targets().forEach(value -> targets.put(value.skill(), value.targetLevel()));
        if (targets.isEmpty()) context.levels().forEach(value -> targets.put(value.skill(), value.estimatedLevel()));
        if (targets.isEmpty()) SKILLS.forEach(skill -> targets.put(skill, null));
        for (String skill : targets.keySet()) ensurePath(userId, skill, targets.get(skill), context);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathProgress> summaries(Integer userId) {
        return paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE").stream().map(this::summaryView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PathProgress summary(Integer userId, Integer pathId) {
        return summaryView(paths.findByIdAndUserId(pathId, userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lộ trình")));
    }

    @Override
    @Transactional
    public void completeLesson(Integer userId, Integer lessonId) {
        accounts.requireActiveStudentById(userId);
        LessonEntity lesson = lessons.findById(lessonId).filter(x -> "PUBLISHED".equals(x.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Bài học chưa được công bố"));
        LessonProgressEntity value = progress.findByUserIdAndLessonId(userId, lessonId).orElseGet(() -> {
            LessonProgressEntity created = new LessonProgressEntity(); created.setUserId(userId); created.setLessonId(lessonId); return created;
        });
        value.setStatus("COMPLETED"); if (value.getCompletedAt() == null) value.setCompletedAt(LocalDateTime.now()); value.setLastAccessedAt(LocalDateTime.now()); progress.save(value);
        paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE").forEach(path -> items.findByPathIdAndLessonId(path.getId(), lessonId).ifPresent(item -> { item.setStatus("COMPLETED"); item.setCompletedAt(value.getCompletedAt()); items.save(item); }));
    }

    @Override
    @Transactional
    public Integer startCheckpoint(Integer userId, Integer pathId, String requestKey) {
        accounts.requireActiveStudentById(userId);
        LearningPathEntity path = paths.findByIdAndUserId(pathId, userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lộ trình"));
        List<LearningPathItemEntity> pathItems = items.findByPathIdOrderByPositionAsc(pathId);
        if (pathItems.stream().filter(LearningPathItemEntity::isRequired).anyMatch(x -> !"COMPLETED".equals(x.getStatus()))) throw new BusinessRuleException("Hãy hoàn thành các bài bắt buộc trước khi làm checkpoint");
        if (path.getTargetLevel() == null) throw new BusinessRuleException("Lộ trình định hướng chưa có checkpoint mục tiêu");
        var checkpoint = exams.checkpoint(path.getSkill(), path.getTargetLevel());
        ExamAttemptContract attempt = attempts.getIfAvailable(); if (attempt == null) throw new BusinessRuleException("Exam Engine chưa sẵn sàng");
        return attempt.start(userId, checkpoint.examId(), requestKey == null ? "checkpoint:" + userId + ":" + pathId : requestKey).attemptId();
    }

    @Override
    @Transactional
    public void applyCheckpoint(Integer userId, Integer attemptId) {
        accounts.applyAssessment(userId, attemptId); ensurePaths(userId);
    }

    private void ensurePath(Integer userId, String skill, String target, LearnerContext context) {
        String current = context.levels().stream().filter(x -> skill.equals(x.skill())).map(SkillLevel::estimatedLevel).findFirst().orElse(null);
        Integer sourceAttempt = context.levels().stream().filter(x -> skill.equals(x.skill())).map(SkillLevel::sourceAttemptId).findFirst().orElse(null);
        LearningPathEntity existing = paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(userId, skill, "ACTIVE").orElse(null);
        if (existing != null && Objects.equals(existing.getFromLevel(), current) && Objects.equals(existing.getTargetLevel(), target)) return;
        if (existing != null) { existing.setStatus("ARCHIVED"); paths.save(existing); }
        List<LessonEntity> available = lessons.findBySkillAndStatusOrderByDisplayOrderAsc(skill, "PUBLISHED");
        if (available.isEmpty()) return;
        LearningPathEntity path = new LearningPathEntity(); path.setUserId(userId); path.setSkill(skill); path.setFromLevel(current); path.setTargetLevel(target);
        path.setSourceAttemptId(sourceAttempt); path.setPathSource(target == null ? "ORIENTATION" : (sourceAttempt == null ? "GOAL" : "PLACEMENT")); path.setStatus("ACTIVE"); path.setCreatedAt(LocalDateTime.now()); path = paths.save(path);
        int position = 1; for (LessonEntity lesson : available) {
            LearningPathItemEntity item = new LearningPathItemEntity(); item.setPathId(path.getId()); item.setLessonId(lesson.getId()); item.setPosition(position++); item.setRequired(true);
            LessonProgressEntity done = progress.findByUserIdAndLessonId(userId, lesson.getId()).orElse(null);
            item.setStatus(done != null && "COMPLETED".equals(done.getStatus()) ? "COMPLETED" : "NOT_STARTED"); item.setCompletedAt(done == null ? null : done.getCompletedAt()); items.save(item);
        }
    }

    private PathProgress summaryView(LearningPathEntity path) {
        List<LearningPathItemEntity> values = items.findByPathIdOrderByPositionAsc(path.getId());
        int total = values.size(); int completed = (int) values.stream().filter(x -> "COMPLETED".equals(x.getStatus())).count();
        int required = (int) values.stream().filter(LearningPathItemEntity::isRequired).count(); int completedRequired = (int) values.stream().filter(x -> x.isRequired() && "COMPLETED".equals(x.getStatus())).count();
        Integer percent = total == 0 ? null : (100 * completed) / total;
        LearningPathItemEntity next = values.stream().filter(x -> !"COMPLETED".equals(x.getStatus())).findFirst().orElse(null);
        String title = next == null ? null : lessons.findById(next.getLessonId()).map(LessonEntity::getTitle).orElse(null);
        return new PathProgress(path.getId(), path.getSkill(), path.getPathSource(), path.getStatus(), completed, total, percent, completedRequired, required, next == null ? null : next.getLessonId(), title, checkpointState(path, completedRequired, required));
    }

    private String checkpointState(LearningPathEntity path, int completedRequired, int required) {
        if (path.getTargetLevel() == null) return "not_configured";
        return completedRequired == required ? "available" : "locked";
    }
}
