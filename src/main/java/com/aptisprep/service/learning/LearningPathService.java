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
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final UserGoalRepository userGoalRepository;
    private final ObjectProvider<ExamAttemptContract> attempts;
    private final ExamCatalogContract catalog;
    private final AccountAccessContract accounts;
    private final CheckpointStatusReader checkpointStatus;

    public LearningPathService(
            LearningPathRepository learningPathRepository,
            LearningPathItemRepository learningPathItemRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            UserRepository userRepository,
            UserGoalRepository userGoalRepository,
            ObjectProvider<ExamAttemptContract> attempts,
            ExamCatalogContract catalog,
            AccountAccessContract accounts,
            CheckpointStatusReader checkpointStatus) {
        this.learningPathRepository = learningPathRepository;
        this.learningPathItemRepository = learningPathItemRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.userRepository = userRepository;
        this.userGoalRepository = userGoalRepository;
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
        userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        List<LearningPath> current =
                new ArrayList<>(
                        learningPathRepository.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
                                userId, "ACTIVE"));
        Set<String> shown = new HashSet<>();
        current.forEach(path -> shown.add(path.getSkill()));
        // Keep the final completed stage visible after reaching the goal or exhausting available
        // content.
        for (LearningPath path :
                learningPathRepository.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
                        userId, "COMPLETED")) if (shown.add(path.getSkill())) current.add(path);
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
            String currentLevel,
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
        LearnerContext learner = accounts.context(user.getId());
        List<SkillLevel> assessedLevels =
                learner == null || learner.levels() == null ? List.of() : learner.levels();
        Map<Integer, String> targets = new HashMap<>();
        userGoalRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), GoalStatus.ACTIVE)
                .ifPresent(
                        goal ->
                                goal.getSkills()
                                        .forEach(
                                                target ->
                                                        targets.put(
                                                                target.getId(),
                                                                target.getTargetLevel())));
        List<Stage> result =
                new ArrayList<>(
                        summaries(user.getId()).stream()
                                .filter(view -> skill == null || view.skill().equals(skill))
                                .map(
                                        view -> {
                                            LearningPath path = owned(user.getId(), view.pathId());
                                            List<StageLesson> assigned =
                                                    learningPathItemRepository
                                                            .findByLearningPathIdOrderByPositionAsc(
                                                                    path.getId())
                                                            .stream()
                                                            .map(
                                                                    item ->
                                                                            new StageLesson(
                                                                                    item.getLesson()
                                                                                            .getId(),
                                                                                    item.getLesson()
                                                                                            .getTitle(),
                                                                                    item.getLesson()
                                                                                            .getCefrLevel(),
                                                                                    item
                                                                                            .getStatus(),
                                                                                    "PUBLISHED"
                                                                                            .equals(
                                                                                                    item.getLesson()
                                                                                                            .getStatus()),
                                                                                    item.getLesson()
                                                                                                            .getMaterial()
                                                                                                    == null
                                                                                            ? null
                                                                                            : item.getLesson()
                                                                                                    .getMaterial()
                                                                                                    .getTopic(),
                                                                                    item
                                                                                            .isRequired(),
                                                                                    completed(
                                                                                            item)))
                                                            .toList();
                                            return new Stage(
                                                    path.getId(),
                                                    path.getSkill(),
                                                    path.getFromLevel(),
                                                    path.getTargetLevel(),
                                                    targets.get(path.getGoalSkillId()),
                                                    assessedLevel(assessedLevels, path.getSkill())
                                                                    == null
                                                            ? path.getFromLevel()
                                                            : assessedLevel(
                                                                            assessedLevels,
                                                                            path.getSkill())
                                                                    .estimatedLevel(),
                                                    path.getStatus(),
                                                    assigned);
                                        })
                                .toList());
        Map<String, UserGoalSkill> personalGoals = goalTargets(user.getId());
        for (String code : LessonService.SKILLS) {
            if ((skill != null && !skill.equals(code))
                    || result.stream().anyMatch(stage -> stage.skill().equals(code))) continue;
            SkillLevel assessed = assessedLevel(assessedLevels, code);
            String current = assessed == null ? null : assessed.estimatedLevel();
            UserGoalSkill personal = personalGoals.get(codeSkill(code));
            String goal = personal == null ? null : personal.getTargetLevel();
            boolean reached = goal != null && current != null && rank(current) >= rank(goal);
            String target = reached ? null : nextLevel(current);
            result.add(
                    new Stage(
                            null,
                            code,
                            current,
                            target,
                            goal,
                            current,
                            reached ? "GOAL_REACHED" : target == null ? "COMPLETED" : "NO_CONTENT",
                            List.of()));
        }
        return result;
    }

    @Override
    @Transactional
    public void completeLesson(Integer userId, Integer lessonId) {
        User user = lock(active(userId));
        Lesson lesson =
                lessonRepository
                        .findById(lessonId)
                        .filter(value -> "PUBLISHED".equals(value.getStatus()))
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        LessonProgress value =
                lessonProgressRepository
                        .findByUserIdAndLessonId(userId, lessonId)
                        .orElseGet(
                                () -> {
                                    LessonProgress created = new LessonProgress();
                                    created.setUser(user);
                                    created.setLesson(lesson);
                                    return created;
                                });
        if (!"COMPLETED".equals(value.getStatus())) {
            value.setStatus("COMPLETED");
            value.setCompletedAt(LocalDateTime.now());
        }
        value.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(value);
        for (LearningPathItem item :
                learningPathItemRepository.findByLearningPathUserIdAndLearningPathStatusAndLessonId(
                        userId, "ACTIVE", lessonId)) {
            item.setStatus("COMPLETED");
            item.setCompletedAt(value.getCompletedAt());
            learningPathItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public Integer startCheckpoint(Integer userId, Integer pathId, String requestKey) {
        lock(active(userId));
        LearningPath path = owned(userId, pathId);
        if (!"ACTIVE".equals(path.getStatus()))
            throw new BusinessRuleException("Chặng học không còn hoạt động");
        List<LearningPathItem> required = requiredItems(path);
        if (required.isEmpty() || required.stream().anyMatch(item -> !completed(item)))
            throw new BusinessRuleException(
                    "Hãy hoàn thành các bài bắt buộc trước khi làm checkpoint");
        if (path.getTargetLevel() == null)
            throw new BusinessRuleException(
                    "Hãy đặt mục tiêu hoặc làm Placement để có chặng kiểm tra phù hợp");
        if (!LessonService.LEVELS.contains(path.getTargetLevel()))
            throw new BusinessRuleException(
                    "Hành trình học chỉ hỗ trợ từ A1 đến C1; chặng cũ được giữ để tra cứu");
        ExamAttemptContract engine = engine();
        if (path.getCheckpointExamId() != null) {
            Optional<AttemptResult> running =
                    checkpointStatus.history(path, engine).stream()
                            .filter(checkpointStatus::unfinished)
                            .findFirst();
            if (running.isPresent()) return running.get().attemptId();
        }
        ExamSummary checkpoint =
                catalog.checkpoint(codeSkill(path.getSkill()), path.getTargetLevel());
        path.setCheckpointExamId(checkpoint.examId());
        learningPathRepository.save(path);
        List<AttemptResult> previous = checkpointStatus.history(path, engine);
        Optional<AttemptResult> unfinished =
                previous.stream().filter(checkpointStatus::unfinished).findFirst();
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
        AttemptResult result = engine().result(active(email).getId(), attemptId);
        return "/student/attempts/"
                + attemptId
                + ("IN_PROGRESS".equals(result.state()) ? "" : "/result");
    }

    /** AFTER_COMMIT callbacks need a real new transaction before locks/Profile delegation. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCheckpoint(Integer userId, Integer attemptId) {
        User user = lock(active(userId));
        if (learningPathRepository.existsByUserIdAndSourceAttemptIdAndStatus(
                userId, attemptId, "COMPLETED")) return;
        AttemptResult result = engine().result(userId, attemptId);
        if (result == null
                || !Objects.equals(result.userId(), userId)
                || !Objects.equals(result.attemptId(), attemptId)
                || !"CHECKPOINT".equals(result.examType())
                || !checkpointStatus.terminal(result))
            throw new BusinessRuleException("Checkpoint chưa có kết quả hoàn tất hợp lệ");
        LearningPath path =
                learningPathRepository
                        .findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE")
                        .stream()
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
        SkillResult skill =
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
        List<LearningPathItem> required = requiredItems(path);
        if (required.isEmpty() || required.stream().anyMatch(item -> !completed(item)))
            throw new BusinessRuleException("Chặng học chưa hoàn thành các bài bắt buộc");
        accounts.applyAssessment(userId, attemptId);
        if (!skill.passed() || rank(skill.estimatedLevel()) < rank(path.getTargetLevel())) return;
        path.setStatus("COMPLETED");
        path.setCompletedAt(LocalDateTime.now());
        path.setSourceAttemptId(attemptId);
        learningPathRepository.saveAndFlush(path);
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
        List<Lesson> published =
                lessonRepository.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc(
                        "PUBLISHED");
        Map<String, UserGoalSkill> targets = goalTargets(user.getId());
        LearnerContext context = accounts.context(user.getId());
        List<SkillLevel> levels =
                context == null || context.levels() == null ? List.of() : context.levels();
        int changed = 0;
        for (String skill : LessonService.SKILLS) {
            UserGoalSkill goal = targets.get(codeSkill(skill));
            SkillLevel assessed = assessedLevel(levels, skill);
            String from = assessed == null ? null : validLevel(assessed.estimatedLevel());
            String target = nextLevel(from);
            LearningPath current =
                    learningPathRepository
                            .findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
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
            List<Lesson> selected =
                    published.stream()
                            .filter(lesson -> skill.equals(lesson.getSkill()))
                            .filter(lesson -> Objects.equals(contentLevel, lesson.getCefrLevel()))
                            .toList();
            if (selected.isEmpty() && current == null) continue;
            Desired desired =
                    new Desired(
                            skill,
                            goal != null ? "GOAL" : from == null ? "DEFAULT" : "PLACEMENT",
                            goal == null ? null : goal.getId(),
                            from,
                            target,
                            assessed == null ? null : assessed.sourceAttemptId(),
                            selected);
            boolean same = sameStage(current, desired);
            List<LearningPathItem> assignedItems =
                    current == null
                            ? List.<LearningPathItem>of()
                            : learningPathItemRepository.findByLearningPathIdOrderByPositionAsc(
                                    current.getId());
            List<Integer> assigned =
                    assignedItems.stream().map(item -> item.getLesson().getId()).toList();
            boolean invalidAssignment = hasInvalidLesson(assignedItems, skill, contentLevel);
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
        learningPathRepository.saveAndFlush(path);
    }

    private void createPath(User user, Desired desired) {
        LearningPath path = new LearningPath();
        path.setUser(user);
        path.setSkill(desired.skill());
        path.setPathSource(desired.source());
        path.setGoalSkillId(desired.goalSkillId());
        path.setFromLevel(desired.fromLevel());
        path.setTargetLevel(desired.targetLevel());
        path.setSourceAttemptId(desired.sourceAttemptId());
        path.setStatus("ACTIVE");
        path = learningPathRepository.save(path);
        int position = 1;
        Set<Integer> seen = new HashSet<>();
        for (Lesson lesson : desired.lessons()) {
            if (!seen.add(lesson.getId())) continue;
            LessonProgress done =
                    lessonProgressRepository
                            .findByUserIdAndLessonId(user.getId(), lesson.getId())
                            .filter(
                                    value ->
                                            "COMPLETED".equals(value.getStatus())
                                                    && value.getCompletedAt() != null)
                            .orElse(null);
            LearningPathItem item = new LearningPathItem();
            item.setLearningPath(path);
            item.setLesson(lesson);
            item.setPosition(position++);
            item.setRequired(true);
            item.setStatus(done == null ? "NOT_STARTED" : "COMPLETED");
            item.setCompletedAt(done == null ? null : done.getCompletedAt());
            learningPathItemRepository.save(item);
        }
    }

    private PathProgress summaryOf(LearningPath path) {
        List<LearningPathItem> assigned =
                learningPathItemRepository.findByLearningPathIdOrderByPositionAsc(path.getId());
        int total = assigned.size(), done = (int) assigned.stream().filter(this::completed).count();
        int required = (int) assigned.stream().filter(LearningPathItem::isRequired).count();
        int completedRequired =
                (int)
                        assigned.stream()
                                .filter(LearningPathItem::isRequired)
                                .filter(this::completed)
                                .count();
        LearningPathItem next =
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
        return learningPathItemRepository
                .findByLearningPathIdOrderByPositionAsc(path.getId())
                .stream()
                .filter(LearningPathItem::isRequired)
                .toList();
    }

    private LearningPath owned(Integer userId, Integer pathId) {
        return learningPathRepository
                .findByIdAndUserId(pathId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lộ trình"));
    }

    private ExamAttemptContract engine() {
        ExamAttemptContract engine = attempts.getIfAvailable();
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
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        checkActive(user);
        return user;
    }

    private User active(String email) {
        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
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
        List<String> levels = LessonService.LEVELS;
        return next < levels.size() ? levels.get(next) : null;
    }

    private Map<String, UserGoalSkill> goalTargets(Integer userId) {
        Map<String, UserGoalSkill> targets = new HashMap<>();
        UserGoal goal =
                userGoalRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                        .orElse(null);
        if (goal != null) {
            for (UserGoalSkill target : goal.getSkills()) {
                targets.put(codeSkill(target.getSkill()), target);
            }
        }
        return targets;
    }

    private SkillLevel assessedLevel(List<SkillLevel> levels, String skill) {
        for (SkillLevel level : levels) {
            if (codeSkill(skill).equals(codeSkill(level.skill()))) return level;
        }
        return null;
    }

    private boolean sameStage(LearningPath current, Desired desired) {
        return current != null
                && Objects.equals(current.getGoalSkillId(), desired.goalSkillId())
                && Objects.equals(current.getFromLevel(), desired.fromLevel())
                && Objects.equals(current.getTargetLevel(), desired.targetLevel());
    }

    private boolean hasInvalidLesson(List<LearningPathItem> items, String skill, String level) {
        for (LearningPathItem item : items) {
            Lesson lesson = item.getLesson();
            if (!"PUBLISHED".equals(lesson.getStatus())
                    || !skill.equals(lesson.getSkill())
                    || !Objects.equals(level, lesson.getCefrLevel())) return true;
        }
        return false;
    }
}
