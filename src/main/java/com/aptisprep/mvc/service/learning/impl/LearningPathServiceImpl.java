package com.aptisprep.mvc.service.learning.impl;

import com.aptisprep.mvc.service.learning.*;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.service.learning.LearningPathService;
import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.catalog.PublishedExamCatalogService;
import com.aptisprep.mvc.service.exam.ExamAttemptCommandService;
import com.aptisprep.mvc.service.exam.ExamAttemptQueryService;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.*;
import com.aptisprep.mvc.support.learning.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class LearningPathServiceImpl implements LearningPathService {
    private record Desired(
            String skill,
            String source,
            Integer goalSkillId,
            String fromLevel,
            String targetLevel,
            Integer sourceAttemptId,
            List<Lesson> lessons) {}

    @PersistenceContext private EntityManager entityManager;
    private LearningPathRepository learningPathRepository;
    private LearningPathItemRepository learningPathItemRepository;
    private LessonRepository lessonRepository;
    private LessonProgressRepository lessonProgressRepository;
    private UserRepository userRepository;
    private UserGoalRepository userGoalRepository;
    private ExamAttemptCommandService attempts;
    private ExamAttemptQueryService attemptQueries;
    private PublishedExamCatalogService catalog;
    private AccountAccessService accounts;
    private CheckpointStatusService checkpointStatus;

    public LearningPathServiceImpl(
            LearningPathRepository learningPathRepository,
            LearningPathItemRepository learningPathItemRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            UserRepository userRepository,
            UserGoalRepository userGoalRepository,
            ExamAttemptCommandService attempts,
            ExamAttemptQueryService attemptQueries,
            PublishedExamCatalogService catalog,
            AccountAccessService accounts,
            CheckpointStatusService checkpointStatus) {
        this.learningPathRepository = learningPathRepository;
        this.learningPathItemRepository = learningPathItemRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.userRepository = userRepository;
        this.userGoalRepository = userGoalRepository;
        this.attempts = attempts;
        this.attemptQueries = attemptQueries;
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
        for (LearningPath path :
                learningPathRepository.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
                        userId, "COMPLETED")) if (shown.add(path.getSkill())) current.add(path);
        return current.stream()
                .sorted(Comparator.comparing(streamValue -> streamValue.getSkill()))
                .map(streamValue -> summaryOf(streamValue))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PathProgress summary(Integer userId, Integer pathId) {
        return summaryOf(owned(userId, pathId));
    }







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
                                                                                    item.getLesson().getTopic(),
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
        for (String code : Skill.labels()) {
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
        if (!CefrLevel.curriculumLevels().contains(path.getTargetLevel()))
            throw new BusinessRuleException(
                    "Hành trình học chỉ hỗ trợ từ A1 đến C1; chặng cũ được giữ để tra cứu");
        ExamAttemptCommandService engine = engine();
        if (path.getCheckpointExamId() != null) {
            Optional<AttemptResult> running =
                    checkpointStatus.history(path).stream()
                            .filter(streamValue -> checkpointStatus.unfinished(streamValue))
                            .findFirst();
            if (running.isPresent()) return running.get().attemptId();
        }
        MockExamDTO checkpoint =
                catalog.checkpoint(codeSkill(path.getSkill()), path.getTargetLevel());
        path.setCheckpointExamId(checkpoint.getId());
        learningPathRepository.save(path);
        List<AttemptResult> previous = checkpointStatus.history(path);
        Optional<AttemptResult> unfinished =
                previous.stream().filter(streamValue -> checkpointStatus.unfinished(streamValue)).findFirst();
        if (unfinished.isPresent()) return unfinished.get().attemptId();
        int last = previous.stream().mapToInt(streamValue -> streamValue.attemptId()).max().orElse(0);
        return engine.startCheckpoint(
                        userId,
                        checkpoint.getId(),
                        pathId,
                        "checkpoint:" + userId + ":" + pathId + ":after:" + last)
                .attemptId();
    }

    @Override
    @Transactional
    public Integer retryCheckpoint(Integer userId, Integer attemptId) {
        lock(active(userId));
        AttemptResult previousResult = attemptQueries.result(userId, attemptId);
        if (previousResult == null
                || !Objects.equals(previousResult.userId(), userId)
                || !Objects.equals(previousResult.attemptId(), attemptId)
                || !"CHECKPOINT".equals(previousResult.examType())
                || !checkpointStatus.terminal(previousResult)
                || previousResult.submittedAt() == null)
            throw new BusinessRuleException("Chỉ có checkpoint đã nộp mới được làm lại");

        Integer pathId =
                attemptQueries
                        .checkpointOriginPathId(userId, attemptId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Không xác định được chặng học của checkpoint"));
        LearningPath path = owned(userId, pathId);
        if (!Objects.equals(path.getCheckpointExamId(), previousResult.examId()))
            throw new BusinessRuleException(
                    "Checkpoint không khớp với chặng học của kết quả");

        // Chặng đang hoạt động có thể làm lại sau khi trượt. Chặng đã hoàn thành
        // chỉ được làm lại như một lần luyện thêm, không mở lại trình độ cũ.
        if ("ACTIVE".equals(path.getStatus())) return startCheckpoint(userId, pathId, null);
        if (!"COMPLETED".equals(path.getStatus()))
            throw new BusinessRuleException(
                    "Chỉ được làm lại checkpoint của chặng đang học hoặc đã hoàn thành");

        MockExamDTO checkpoint =
                catalog.checkpoint(codeSkill(path.getSkill()), path.getTargetLevel());
        if (!Objects.equals(checkpoint.getId(), previousResult.examId()))
            throw new BusinessRuleException("Checkpoint cũ không còn phù hợp với chặng học");

        List<AttemptResult> previousAttempts = checkpointStatus.history(path);
        Optional<AttemptResult> unfinished =
                previousAttempts.stream()
                        .filter(streamValue -> checkpointStatus.unfinished(streamValue))
                        .findFirst();
        if (unfinished.isPresent()) return unfinished.get().attemptId();

        int lastAttemptId =
                Math.max(
                        attemptId,
                        previousAttempts.stream()
                                .mapToInt(AttemptResult::attemptId)
                                .max()
                                .orElse(0));
        return attempts
                .startCheckpointRetake(
                        userId,
                        checkpoint.getId(),
                        pathId,
                        "checkpoint:" + userId + ":" + pathId + ":after:" + lastAttemptId)
                .attemptId();
    }

    @Transactional(readOnly = true)
    public AttemptResult checkpointResult(String email, Integer attemptId) {
        return attemptQueries.result(active(email).getId(), attemptId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCheckpoint(Integer userId, Integer attemptId) {
        User user = lock(active(userId));
        if (learningPathRepository.existsByUserIdAndSourceAttemptIdAndStatus(
                userId, attemptId, "COMPLETED")) return;
        AttemptResult result = attemptQueries.result(userId, attemptId);
        if (result == null
                || !Objects.equals(result.userId(), userId)
                || !Objects.equals(result.attemptId(), attemptId)
                || !"CHECKPOINT".equals(result.examType())
                || !checkpointStatus.terminal(result))
            throw new BusinessRuleException("Checkpoint chưa có kết quả hoàn tất hợp lệ");
        var originPathId = attemptQueries.checkpointOriginPathId(userId, attemptId);
        var candidates =
                originPathId.isPresent()
                        ? learningPathRepository
                                .findByIdAndUserId(originPathId.get(), userId)
                                .stream()
                        : learningPathRepository
                                .findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId, "ACTIVE")
                                .stream();
        LearningPath path =
                candidates
                        .filter(
                                value ->
                                        Objects.equals(
                                                value.getCheckpointExamId(), result.examId()))
                        .filter(value -> checkpointStatus.notBeforePath(result, value))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Kết quả không thuộc chặng học"));
        if ("COMPLETED".equals(path.getStatus())) return;
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
                || path.getTargetLevel() == null)
            throw new BusinessRuleException("Kỹ năng checkpoint chưa chấm xong");
        List<LearningPathItem> required = requiredItems(path);
        if (required.isEmpty() || required.stream().anyMatch(item -> !completed(item)))
            throw new BusinessRuleException("Chặng học chưa hoàn thành các bài bắt buộc");
        if (skill.estimatedLevel() != null) accounts.applyAssessment(userId, attemptId);
        if (!skill.passed() || rank(skill.estimatedLevel()) < rank(path.getTargetLevel())) return;
        path.setStatus("COMPLETED");
        path.setCompletedAt(LocalDateTime.now());
        path.setSourceAttemptId(attemptId);
        learningPathRepository.saveAndFlush(path);
        syncPaths(user, false);
    }



    private int syncPaths(User user, boolean refreshContent) {
        List<Lesson> published =
                lessonRepository.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc(
                        "PUBLISHED");
        Map<String, UserGoalSkill> targets = goalTargets(user.getId());
        LearnerContext context = accounts.context(user.getId());
        List<SkillLevel> levels =
                context == null || context.levels() == null ? List.of() : context.levels();
        int changed = 0;
        for (String skill : Skill.labels()) {
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
                            || assigned.equals(selected.stream().map(streamValue -> streamValue.getId()).toList())))
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
        int total = assigned.size(), done = (int) assigned.stream().filter(streamValue -> completed(streamValue)).count();
        int required = (int) assigned.stream().filter(streamValue -> streamValue.isRequired()).count();
        int completedRequired =
                (int)
                        assigned.stream()
                                .filter(streamValue -> streamValue.isRequired())
                                .filter(streamValue -> completed(streamValue))
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
                .filter(streamValue -> streamValue.isRequired())
                .toList();
    }

    private LearningPath owned(Integer userId, Integer pathId) {
        return learningPathRepository
                .findByIdAndUserId(pathId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lộ trình"));
    }

    private ExamAttemptCommandService engine() {
        return attempts;
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

    private String codeSkill(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    private int rank(String value) {
        return value == null ? -1 : CefrLevel.codes().indexOf(value.toUpperCase(Locale.ROOT));
    }

    private String validLevel(String value) {
        if (CefrLevel.BELOW_A1.equalsIgnoreCase(value)) return CefrLevel.BELOW_A1;
        return rank(value) < 0 ? null : value.toUpperCase(Locale.ROOT);
    }

    private String nextLevel(String value) {
        int next = rank(value) + 1;
        List<String> levels = CefrLevel.curriculumLevels();
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

