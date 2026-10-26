package com.aptisprep.service.learning;

import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Owns path/progress only; Profile and attempt state are delegated to the team contracts. */
@Service
public class LearningPathService implements LearningPathContract {
    @PersistenceContext private EntityManager entityManager;
    private final LearningPathRepository paths;
    private final LearningPathItemRepository items;
    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final UserRepository users;
    private final UserGoalRepository goals;
    private final ObjectProvider<ExamAttemptContract> attempts;
    private final ExamCatalogContract catalog;
    private final AccountAccessContract accounts;
    private final CheckpointStatusReader checkpointStatus;

    public LearningPathService(
            LearningPathRepository paths,
            LearningPathItemRepository items,
            LessonRepository lessons,
            LessonProgressRepository progress,
            UserRepository users,
            UserGoalRepository goals,
            ObjectProvider<ExamAttemptContract> attempts,
            ExamCatalogContract catalog,
            AccountAccessContract accounts,
            CheckpointStatusReader checkpointStatus) {
        this.paths = paths;
        this.items = items;
        this.lessons = lessons;
        this.progress = progress;
        this.users = users;
        this.goals = goals;
        this.attempts = attempts;
        this.catalog = catalog;
        this.accounts = accounts;
        this.checkpointStatus = checkpointStatus;
    }

    @Override
    @Transactional
    public void ensurePaths(Integer userId) {
        syncPaths(lock(active(userId)), true);
    }

    @Transactional
    public List<PathProgress> ensureAndSummaries(String email) {
        User user = lock(active(email));
        syncPaths(user, true);
        return summaries(user.getId());
    }

    @Transactional
    public int refreshPaths(String email) {
        return syncPaths(lock(active(email)), true);
    }

    @Transactional(readOnly = true)
    public Integer userId(String email) {
        return active(email).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathProgress> summaries(Integer userId) {
        users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        List<LearningPath> current =
                new ArrayList<>(
                        paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE"));
        Set<String> shown = new HashSet<>();
        current.forEach(path -> shown.add(path.getSkill()));
        // Keep the final completed stage visible after reaching the goal or exhausting available
        // content.
        for (var path :
                paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "COMPLETED"))
            if (shown.add(path.getSkill())) current.add(path);
        return current.stream()
                .sorted(Comparator.comparing(LearningPath::getSkill))
                .map(this::summaryOf)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PathProgress summary(Integer userId, Integer pathId) {
        return summaryOf(owned(userId, pathId));
    }

    public record Stage(
            Integer pathId,
            String skill,
            String fromLevel,
            String targetLevel,
            String goalLevel,
            String status,
            List<StageLesson> lessons) {
        public List<StageLesson> missingRequiredLessons() {
            return lessons.stream()
                    .filter(lesson -> lesson.required() && !lesson.completed())
                    .toList();
        }
    }

    public record StageLesson(
            Integer id,
            String title,
            String level,
            String status,
            boolean available,
            String topic,
            boolean required,
            boolean completed) {}

    @Transactional(readOnly = true)
    public List<Stage> stages(String email, String skill) {
        User user = active(email);
        Map<Integer, String> targets = new HashMap<>();
        goals.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), GoalStatus.ACTIVE)
                .ifPresent(
                        goal ->
                                goal.getSkills()
                                        .forEach(
                                                target ->
                                                        targets.put(
                                                                target.getId(),
                                                                target.getTargetLevel())));
        return summaries(user.getId()).stream()
                .filter(view -> skill == null || view.skill().equals(skill))
                .map(
                        view -> {
                            var path = owned(user.getId(), view.pathId());
                            var assigned =
                                    items
                                            .findByLearningPathIdOrderByPositionAsc(path.getId())
                                            .stream()
                                            .map(
                                                    item ->
                                                            new StageLesson(
                                                                    item.getLesson().getId(),
                                                                    item.getLesson().getTitle(),
                                                                    item.getLesson().getCefrLevel(),
                                                                    item.getStatus(),
                                                                    "PUBLISHED"
                                                                            .equals(
                                                                                    item.getLesson()
                                                                                            .getStatus()),
                                                                    item.getLesson().getMaterial()
                                                                                    == null
                                                                            ? null
                                                                            : item.getLesson()
                                                                                    .getMaterial()
                                                                                    .getTopic(),
                                                                    item.isRequired(),
                                                                    completed(item)))
                                            .toList();
                            return new Stage(
                                    path.getId(),
                                    path.getSkill(),
                                    path.getFromLevel(),
                                    path.getTargetLevel(),
                                    targets.get(path.getGoalSkillId()),
                                    path.getStatus(),
                                    assigned);
                        })
                .toList();
    }

    @Override
    @Transactional
    public void completeLesson(Integer userId, Integer lessonId) {
        User user = lock(active(userId));
        Lesson lesson =
                lessons.findById(lessonId)
                        .filter(value -> "PUBLISHED".equals(value.getStatus()))
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        var value =
                progress.findByUserIdAndLessonId(userId, lessonId)
                        .orElseGet(
                                () -> {
                                    var created = new LessonProgress();
                                    created.setUser(user);
                                    created.setLesson(lesson);
                                    return created;
                                });
        if (!"COMPLETED".equals(value.getStatus())) {
            value.setStatus("COMPLETED");
            value.setCompletedAt(LocalDateTime.now());
        }
        value.setLastAccessedAt(LocalDateTime.now());
        progress.save(value);
        for (var item :
                items.findByLearningPathUserIdAndLearningPathStatusAndLessonId(
                        userId, "ACTIVE", lessonId)) {
            item.setStatus("COMPLETED");
            item.setCompletedAt(value.getCompletedAt());
            items.save(item);
        }
    }

    @Override
    @Transactional
    public Integer startCheckpoint(Integer userId, Integer pathId, String requestKey) {
        lock(active(userId));
        var path = owned(userId, pathId);
        if (!"ACTIVE".equals(path.getStatus()))
            throw new BusinessRuleException("Chặng học không còn hoạt động");
        var required = requiredItems(path);
        if (required.isEmpty() || required.stream().anyMatch(item -> !completed(item)))
            throw new BusinessRuleException(
                    "Hãy hoàn thành các bài bắt buộc trước khi làm checkpoint");
        if (path.getTargetLevel() == null)
            throw new BusinessRuleException(
                    "Hãy đặt mục tiêu hoặc làm Placement để có chặng kiểm tra phù hợp");
        if (!LessonService.LEVELS.contains(path.getTargetLevel()))
            throw new BusinessRuleException(
                    "Hành trình học chỉ hỗ trợ từ A1 đến C1; chặng cũ được giữ để tra cứu");
        var engine = engine();
        if (path.getCheckpointExamId() != null) {
            var running =
                    checkpointStatus.history(path, engine).stream()
                            .filter(checkpointStatus::unfinished)
                            .findFirst();
            if (running.isPresent()) return running.get().attemptId();
        }
        var checkpoint = catalog.checkpoint(codeSkill(path.getSkill()), path.getTargetLevel());
        path.setCheckpointExamId(checkpoint.examId());
        paths.save(path);
        var previous = checkpointStatus.history(path, engine);
        var unfinished = previous.stream().filter(checkpointStatus::unfinished).findFirst();
        if (unfinished.isPresent()) return unfinished.get().attemptId();
        int last = previous.stream().mapToInt(AttemptResult::attemptId).max().orElse(0);
        // One stable start operation, a fresh key after each terminal result.
        return engine.startCheckpoint(
                        userId,
                        checkpoint.examId(),
                        pathId,
                        "checkpoint:" + userId + ":" + pathId + ":after:" + last)
                .attemptId();
    }

    @Transactional(readOnly = true)
    public String checkpointDestination(String email, Integer attemptId) {
        var result = engine().result(active(email).getId(), attemptId);
        return "/student/attempts/"
                + attemptId
                + ("IN_PROGRESS".equals(result.state()) ? "" : "/result");
    }

    /** AFTER_COMMIT callbacks need a real new transaction before locks/Profile delegation. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCheckpoint(Integer userId, Integer attemptId) {
        User user = lock(active(userId));
        if (paths.existsByUserIdAndSourceAttemptIdAndStatus(userId, attemptId, "COMPLETED")) return;
        var result = engine().result(userId, attemptId);
        if (result == null
                || !Objects.equals(result.userId(), userId)
                || !Objects.equals(result.attemptId(), attemptId)
                || !"CHECKPOINT".equals(result.examType())
                || !checkpointStatus.terminal(result))
            throw new BusinessRuleException("Checkpoint chưa có kết quả hoàn tất hợp lệ");
        var path =
                paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE").stream()
                        .filter(
                                value ->
                                        Objects.equals(
                                                value.getCheckpointExamId(), result.examId()))
                        .filter(value -> checkpointStatus.notBeforePath(result, value))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Kết quả không thuộc chặng học hiện tại"));
        var skill =
                result.skills() == null
                        ? null
                        : result.skills().stream()
                                .filter(
                                        value ->
                                                codeSkill(path.getSkill())
                                                        .equals(codeSkill(value.skill())))
                                .findFirst()
                                .orElse(null);
        if (skill == null
                || !"COMPLETED".equals(skill.gradingState())
                || skill.scaleScore() == null
                || rank(skill.estimatedLevel()) < 0
                || path.getTargetLevel() == null)
            throw new BusinessRuleException("Kỹ năng checkpoint chưa chấm xong");
        var required = requiredItems(path);
        if (required.isEmpty() || required.stream().anyMatch(item -> !completed(item)))
            throw new BusinessRuleException("Chặng học chưa hoàn thành các bài bắt buộc");
        accounts.applyAssessment(userId, attemptId);
        if (!skill.passed() || rank(skill.estimatedLevel()) < rank(path.getTargetLevel())) return;
        path.setStatus("COMPLETED");
        path.setCompletedAt(LocalDateTime.now());
        path.setSourceAttemptId(attemptId);
        paths.saveAndFlush(path);
        syncPaths(user, false);
    }

    private record Desired(
            String skill,
            String source,
            Integer goalSkillId,
            String fromLevel,
            String targetLevel,
            Integer sourceAttemptId,
            List<Lesson> lessons) {}

    private int syncPaths(User user, boolean refreshContent) {
        var published = lessons.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED");
        Map<String, UserGoalSkill> targets = new HashMap<>();
        goals.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), GoalStatus.ACTIVE)
                .ifPresent(
                        goal ->
                                goal.getSkills()
                                        .forEach(
                                                target ->
                                                        targets.put(
                                                                codeSkill(target.getSkill()),
                                                                target)));
        var context = accounts.context(user.getId());
        List<SkillLevel> levels =
                context == null || context.levels() == null ? List.of() : context.levels();
        int changed = 0;
        for (String skill : LessonService.SKILLS) {
            var goal = targets.get(codeSkill(skill));
            var assessed =
                    levels.stream()
                            .filter(value -> codeSkill(skill).equals(codeSkill(value.skill())))
                            .findFirst()
                            .orElse(null);
            String from = assessed == null ? null : validLevel(assessed.estimatedLevel());
            String target = nextLevel(from);
            var current =
                    paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                    user.getId(), skill, "ACTIVE")
                            .orElse(null);
            String finalGoal = goal == null ? null : goal.getTargetLevel();
            boolean goalReached =
                    finalGoal != null && from != null && rank(from) >= rank(finalGoal);
            if (goalReached || target == null) {
                if (current != null) {
                    archive(current);
                    changed++;
                }
                continue;
            }
            String contentLevel = target;
            var selected =
                    published.stream()
                            .filter(lesson -> skill.equals(lesson.getSkill()))
                            .filter(lesson -> Objects.equals(contentLevel, lesson.getCefrLevel()))
                            .toList();
            if (selected.isEmpty() && current == null) continue;
            var desired =
                    new Desired(
                            skill,
                            goal != null ? "GOAL" : from == null ? "DEFAULT" : "PLACEMENT",
                            goal == null ? null : goal.getId(),
                            from,
                            target,
                            assessed == null ? null : assessed.sourceAttemptId(),
                            selected);
            boolean same =
                    current != null
                            && Objects.equals(current.getGoalSkillId(), desired.goalSkillId())
                            && Objects.equals(current.getFromLevel(), desired.fromLevel())
                            && Objects.equals(current.getTargetLevel(), desired.targetLevel());
            var assignedItems =
                    current == null
                            ? List.<LearningPathItem>of()
                            : items.findByLearningPathIdOrderByPositionAsc(current.getId());
            var assigned = assignedItems.stream().map(item -> item.getLesson().getId()).toList();
            boolean invalidAssignment =
                    assignedItems.stream()
                            .anyMatch(
                                    item ->
                                            !"PUBLISHED".equals(item.getLesson().getStatus())
                                                    || !skill.equals(item.getLesson().getSkill())
                                                    || !Objects.equals(
                                                            contentLevel,
                                                            item.getLesson().getCefrLevel()));
            if (same
                    && !invalidAssignment
                    && (!refreshContent
                            || assigned.equals(selected.stream().map(Lesson::getId).toList())))
                continue;
            if (current != null) {
                archive(current);
                changed++;
            }
            if (!selected.isEmpty()) {
                createPath(user, desired);
                if (current == null) changed++;
            }
        }
        return changed;
    }

    private void archive(LearningPath path) {
        path.setStatus("ARCHIVED");
        paths.saveAndFlush(path);
    }

    private void createPath(User user, Desired desired) {
        var path = new LearningPath();
        path.setUser(user);
        path.setSkill(desired.skill());
        path.setPathSource(desired.source());
        path.setGoalSkillId(desired.goalSkillId());
        path.setFromLevel(desired.fromLevel());
        path.setTargetLevel(desired.targetLevel());
        path.setSourceAttemptId(desired.sourceAttemptId());
        path.setStatus("ACTIVE");
        path = paths.save(path);
        int position = 1;
        Set<Integer> seen = new HashSet<>();
        for (var lesson : desired.lessons()) {
            if (!seen.add(lesson.getId())) continue;
            var done =
                    progress.findByUserIdAndLessonId(user.getId(), lesson.getId())
                            .filter(
                                    value ->
                                            "COMPLETED".equals(value.getStatus())
                                                    && value.getCompletedAt() != null)
                            .orElse(null);
            var item = new LearningPathItem();
            item.setLearningPath(path);
            item.setLesson(lesson);
            item.setPosition(position++);
            item.setRequired(true);
            item.setStatus(done == null ? "NOT_STARTED" : "COMPLETED");
            item.setCompletedAt(done == null ? null : done.getCompletedAt());
            items.save(item);
        }
    }

    private PathProgress summaryOf(LearningPath path) {
        var assigned = items.findByLearningPathIdOrderByPositionAsc(path.getId());
        int total = assigned.size(), done = (int) assigned.stream().filter(this::completed).count();
        int required = (int) assigned.stream().filter(LearningPathItem::isRequired).count();
        int completedRequired =
                (int)
                        assigned.stream()
                                .filter(LearningPathItem::isRequired)
                                .filter(this::completed)
                                .count();
        var next =
                assigned.stream()
                        .filter(item -> !completed(item))
                        .filter(item -> "PUBLISHED".equals(item.getLesson().getStatus()))
                        .findFirst()
                        .orElse(null);
        String checkpoint =
                "COMPLETED".equals(path.getStatus())
                        ? "passed"
                        : path.getTargetLevel() == null || required == 0
                                ? "not_configured"
                                : completedRequired < required
                                        ? "locked"
                                        : checkpointStatus.state(path);
        return new PathProgress(
                path.getId(),
                path.getSkill(),
                path.getPathSource(),
                path.getStatus(),
                done,
                total,
                LearningProgressCalculator.percent(done, total),
                completedRequired,
                required,
                next == null ? null : next.getLesson().getId(),
                next == null ? null : next.getLesson().getTitle(),
                checkpoint);
    }

    private boolean completed(LearningPathItem item) {
        return "COMPLETED".equals(item.getStatus()) && item.getCompletedAt() != null;
    }

    private List<LearningPathItem> requiredItems(LearningPath path) {
        return items.findByLearningPathIdOrderByPositionAsc(path.getId()).stream()
                .filter(LearningPathItem::isRequired)
                .toList();
    }

    private LearningPath owned(Integer userId, Integer pathId) {
        return paths.findByIdAndUserId(pathId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lộ trình"));
    }

    private ExamAttemptContract engine() {
        var engine = attempts.getIfAvailable();
        if (engine == null) throw new BusinessRuleException("Luồng làm bài chưa sẵn sàng");
        return engine;
    }

    private User lock(User user) {
        entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(user);
        checkActive(user);
        return user;
    }

    private User active(Integer id) {
        var user =
                users.findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        checkActive(user);
        return user;
    }

    private User active(String email) {
        var user =
                users.findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        checkActive(user);
        return user;
    }

    private void checkActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
    }

    private static String codeSkill(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    private static int rank(String value) {
        return value == null ? -1 : CefrLevel.codes().indexOf(value.toUpperCase(Locale.ROOT));
    }

    private static String validLevel(String value) {
        return rank(value) < 0 ? null : value.toUpperCase(Locale.ROOT);
    }

    private static String nextLevel(String value) {
        int next = rank(value) + 1;
        var levels = LessonService.LEVELS;
        return next < levels.size() ? levels.get(next) : null;
    }
}
