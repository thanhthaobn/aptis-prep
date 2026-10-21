package com.aptisprep.service.learning;

import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonProgress;
import com.aptisprep.entity.LearningPath;
import com.aptisprep.entity.LearningPathItem;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.LearningPathItemRepository;
import com.aptisprep.repository.LearningPathRepository;
import com.aptisprep.repository.LessonProgressRepository;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.UserGoalRepository;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.ExamCatalogContract;
import com.aptisprep.service.contract.IntegrationModels.LearnerContext;
import com.aptisprep.service.contract.IntegrationModels.PathProgress;
import com.aptisprep.service.contract.IntegrationModels.SkillLevel;
import com.aptisprep.service.contract.LearningPathContract;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningPathService implements LearningPathContract {
    private static final List<String> SKILLS = LessonService.SKILLS;
    private final AccountAccessService accounts;
    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final LearningPathRepository paths;
    private final LearningPathItemRepository items;
    private final ExamCatalogContract exams;
    private final ObjectProvider<ExamAttemptContract> attempts;

    public LearningPathService(AccountAccessService accounts, UserGoalRepository ignoredGoals, LessonRepository lessons,
            LessonProgressRepository progress, LearningPathRepository paths, LearningPathItemRepository items,
            ExamCatalogContract exams, ObjectProvider<ExamAttemptContract> attempts) {
        this.accounts = accounts; this.lessons = lessons; this.progress = progress; this.paths = paths;
        this.items = items; this.exams = exams; this.attempts = attempts;
    }

    @Override @Transactional
    public void ensurePaths(Integer userId) { ensurePathsInternal(userId); }

    @Transactional
    public int refreshPaths(String email) { return ensurePathsInternal(userId(email)); }

    @Transactional
    public List<PathProgress> ensureAndSummaries(String email) {
        Integer id = userId(email); ensurePaths(id); return summaries(id);
    }

    @Transactional(readOnly = true)
    public Integer userId(String email) { return accounts.requireActiveStudent(email); }

    @Override @Transactional(readOnly = true)
    public List<PathProgress> summaries(Integer userId) {
        return paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE").stream().map(this::summaryView).toList();
    }

    @Override @Transactional(readOnly = true)
    public PathProgress summary(Integer userId, Integer pathId) {
        return summaryView(paths.findByIdAndUserId(pathId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lộ trình")));
    }

    @Override @Transactional
    public void completeLesson(Integer userId, Integer lessonId) {
        accounts.requireActiveStudentById(userId);
        Lesson lesson = lessons.findById(lessonId).filter(x -> "PUBLISHED".equals(x.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Bài học chưa được công bố"));
        LessonProgress value = progress.findByUserIdAndLessonId(userId, lessonId).orElseGet(() -> {
            LessonProgress created = new LessonProgress(); created.setUser(accounts.requireActiveStudentById(userId));
            created.setLesson(lesson); created.setStatus("IN_PROGRESS"); return created;
        });
        value.setStatus("COMPLETED"); if (value.getCompletedAt() == null) value.setCompletedAt(LocalDateTime.now());
        value.setLastAccessedAt(LocalDateTime.now()); progress.save(value);
        paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE").forEach(path ->
                items.findByPathIdAndLessonId(path.getId(), lessonId).ifPresent(item -> {
                    item.setStatus("COMPLETED"); item.setCompletedAt(value.getCompletedAt()); items.save(item);
                }));
    }

    @Override @Transactional
    public Integer startCheckpoint(Integer userId, Integer pathId, String requestKey) {
        accounts.requireActiveStudentById(userId);
        LearningPath path = paths.findByIdAndUserId(pathId, userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lộ trình"));
        List<LearningPathItem> pathItems = items.findByPathIdOrderByPositionAsc(pathId);
        if (pathItems.stream().filter(LearningPathItem::isRequired).anyMatch(x -> !"COMPLETED".equals(x.getStatus())))
            throw new BusinessRuleException("Hãy hoàn thành các bài bắt buộc trước khi làm checkpoint");
        if (path.getTargetLevel() == null) throw new BusinessRuleException("Lộ trình định hướng chưa có checkpoint mục tiêu");
        var checkpoint = exams.checkpoint(codeSkill(path.getSkill()), path.getTargetLevel());
        ExamAttemptContract attempt = attempts.getIfAvailable(); if (attempt == null) throw new BusinessRuleException("Exam Engine chưa sẵn sàng");
        return attempt.start(userId, checkpoint.examId(), requestKey == null ? "checkpoint:" + userId + ":" + pathId : requestKey).attemptId();
    }

    @Override @Transactional
    public void applyCheckpoint(Integer userId, Integer attemptId) { accounts.applyAssessment(userId, attemptId); ensurePaths(userId); }

    private int ensurePathsInternal(Integer userId) {
        LearnerContext context = accounts.context(userId); Map<String, String> targets = new LinkedHashMap<>();
        context.targets().forEach(value -> targets.put(displaySkill(value.skill()), value.targetLevel()));
        if (targets.isEmpty()) context.levels().forEach(value -> targets.put(displaySkill(value.skill()), value.estimatedLevel()));
        if (targets.isEmpty()) SKILLS.forEach(skill -> targets.put(skill, null));
        int changed = 0;
        for (Map.Entry<String, String> entry : targets.entrySet()) {
            String current = context.levels().stream().filter(x -> entry.getKey().equals(displaySkill(x.skill())))
                    .map(SkillLevel::estimatedLevel).findFirst().orElse(null);
            Integer sourceAttempt = context.levels().stream().filter(x -> entry.getKey().equals(displaySkill(x.skill())))
                    .map(SkillLevel::sourceAttemptId).findFirst().orElse(null);
            if (ensurePath(userId, entry.getKey(), entry.getValue(), current, sourceAttempt)) changed++;
        }
        return changed;
    }

    private boolean ensurePath(Integer userId, String skill, String target, String current, Integer sourceAttempt) {
        LearningPath existing = paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(userId, skill, "ACTIVE").orElse(null);
        List<Lesson> available = lessons.findBySkillAndStatusOrderByDisplayOrderAsc(skill, "PUBLISHED");
        List<Integer> oldIds = existing == null ? List.of() : items.findByPathIdOrderByPositionAsc(existing.getId()).stream().map(x -> x.getLesson().getId()).toList();
        List<Integer> newIds = available.stream().map(Lesson::getId).toList();
        if (existing != null && Objects.equals(existing.getFromLevel(), current) && Objects.equals(existing.getTargetLevel(), target) && oldIds.equals(newIds)) return false;
        if (existing != null) { existing.setStatus("ARCHIVED"); paths.save(existing); }
        LearningPath path = new LearningPath(); path.setUser(accounts.requireActiveStudentById(userId)); path.setSkill(skill);
        path.setFromLevel(current); path.setTargetLevel(target); path.setSourceAttemptId(sourceAttempt);
        path.setPathSource(target == null ? "ORIENTATION" : (sourceAttempt == null ? "GOAL" : "PLACEMENT")); path.setStatus("ACTIVE"); path = paths.save(path);
        int position = 1;
        for (Lesson lesson : available) {
            LearningPathItem item = new LearningPathItem(); item.setLearningPath(path); item.setLesson(lesson); item.setPosition(position++); item.setRequired(true);
            LessonProgress done = progress.findByUserIdAndLessonId(userId, lesson.getId()).orElse(null);
            item.setStatus(done != null && "COMPLETED".equals(done.getStatus()) ? "COMPLETED" : "NOT_STARTED"); item.setCompletedAt(done == null ? null : done.getCompletedAt()); items.save(item);
        }
        return true;
    }

    private PathProgress summaryView(LearningPath path) {
        List<LearningPathItem> values = items.findByPathIdOrderByPositionAsc(path.getId()); int total = values.size();
        int completed = (int) values.stream().filter(x -> "COMPLETED".equals(x.getStatus())).count();
        int required = (int) values.stream().filter(LearningPathItem::isRequired).count();
        int completedRequired = (int) values.stream().filter(x -> x.isRequired() && "COMPLETED".equals(x.getStatus())).count();
        Integer percent = total == 0 ? null : (100 * completed) / total;
        LearningPathItem next = values.stream().filter(x -> !"COMPLETED".equals(x.getStatus())).findFirst().orElse(null);
        String title = next == null ? null : next.getLesson().getTitle();
        return new PathProgress(path.getId(), path.getSkill(), path.getPathSource(), path.getStatus(), completed, total, percent, completedRequired, required,
                next == null ? null : next.getLesson().getId(), title, path.getTargetLevel() == null ? "not_configured" : (completedRequired == required ? "available" : "locked"));
    }

    private static String displaySkill(String value) {
        if (value == null) return null; return switch (value.trim().toUpperCase()) {
            case "GRAMMAR_VOCABULARY", "GRAMMAR & VOCABULARY" -> "Grammar & Vocabulary"; case "READING" -> "Reading"; case "LISTENING" -> "Listening"; case "WRITING" -> "Writing"; case "SPEAKING" -> "Speaking"; default -> value;
        };
    }
    private static String codeSkill(String value) { return value == null ? null : value.trim().toUpperCase().replace(" & ", "_").replace(" ", "_"); }
}
