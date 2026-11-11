package com.aptisprep.service.learning.impl;

import com.aptisprep.dto.AttemptResultDTO;
import com.aptisprep.dto.ExamSummaryDTO;
import com.aptisprep.dto.GoalSkillDTO;
import com.aptisprep.dto.LearnerContextDTO;
import com.aptisprep.dto.LearningPathViewsDTO.StageDTO;
import com.aptisprep.dto.LearningPathViewsDTO.StageLessonDTO;
import com.aptisprep.dto.PathProgressDTO;
import com.aptisprep.dto.SkillLevelDTO;
import com.aptisprep.dto.SkillResultDTO;
import com.aptisprep.entity.*;
import com.aptisprep.entity.User;
import com.aptisprep.enums.*;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.admin.*;
import com.aptisprep.service.admin.ExamCatalogService;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.learning.LearningPathService;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class LearningPathServiceImpl implements LearningPathService {
    @PersistenceContext private EntityManager entityManager;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserService userRepository;
    private final ObjectProvider<ExamAttemptService> attempts;
    private final ExamCatalogService catalog;
    private final AccountAccessService accounts;
    private final CheckpointStatusReader checkpointStatus;

    public LearningPathServiceImpl(
            LearningPathRepository learningPathRepository,
            LearningPathItemRepository learningPathItemRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            UserService userRepository,
            ObjectProvider<ExamAttemptService> attempts,
            ExamCatalogService catalog,
            AccountAccessService accounts,
            CheckpointStatusReader checkpointStatus) {
        this.learningPathRepository = learningPathRepository;
        this.learningPathItemRepository = learningPathItemRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.userRepository = userRepository;
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

    @Override
    @Transactional
    public List<PathProgressDTO> ensureAndSummaries(String email) {
        User user = lock(active(email));
        syncPaths(user, true);
        return summaries(user.getId());
    }

    @Override
    @Transactional
    public int refreshPaths(String email) {
        return syncPaths(lock(active(email)), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer userId(String email) {
        return active(email).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathProgressDTO> summaries(Integer userId) {
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
                .sorted(Comparator.comparing(LearningPath::getSkill))
                .map(this::summaryOf)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PathProgressDTO summary(Integer userId, Integer pathId) {
        return summaryOf(owned(userId, pathId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageDTO> stages(String email, String skill) {
        User user = active(email);
        LearnerContextDTO learner = accounts.context(user.getId());
        List<SkillLevelDTO> assessedLevels =
                learner == null || learner.levels() == null ? List.of() : learner.levels();
        Map<Integer, String> targets = accounts.goalSkillTargetsById(user.getId());
        List<LearningPath> paths =
                learningPathRepository.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
                        user.getId(), "ACTIVE");
        ArrayList<StageDTO> result = new ArrayList<>();
        for (String item : Skill.labels()) {
            if (skill != null && !skill.equalsIgnoreCase(item)) continue;
            LearningPath path =
                    paths.stream().filter(p -> p.getSkill().equals(item)).findFirst().orElse(null);
            SkillLevelDTO assessed = assessedLevel(assessedLevels, item);
            String current = assessed == null ? null : validLevel(assessed.estimatedLevel());
            if (path != null) {
                String target = path.getTargetLevel();
                String goal =
                        path.getGoalSkillId() == null ? null : targets.get(path.getGoalSkillId());
                List<LearningPathItem> assigned =
                        learningPathItemRepository.findByLearningPathIdOrderByPositionAsc(
                                path.getId());
                ArrayList<StageLessonDTO> lessons = new ArrayList<>();
                for (LearningPathItem lessonItem : assigned) {
                    Lesson lesson = lessonItem.getLesson();
                    boolean done = completed(lessonItem);
                    lessons.add(
                            new StageLessonDTO(
                                    lesson.getId(),
                                    lesson.getTitle(),
                                    lesson.getCefrLevel(),
                                    done ? "COMPLETED" : "NOT_STARTED",
                                    "PUBLISHED".equals(lesson.getStatus()),
                                    lesson.getTopic(),
                                    lessonItem.isRequired(),
                                    done));
                }
                result.add(
                        new StageDTO(
                                path.getId(),
                                item,
                                path.getFromLevel(),
                                target,
                                goal,
                                current,
                                path.getStatus(),
                                lessons));
                continue;
            }
            result.add(
                    new StageDTO(null, item, null, null, null, current, "NO_CONTENT", List.of()));
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
        ExamAttemptService engine = engine();
        if (path.getCheckpointExamId() != null) {
            Optional<AttemptResultDTO> running =
                    checkpointStatus.history(path, engine).stream()
                            .filter(checkpointStatus::unfinished)
                            .findFirst();
            if (running.isPresent()) return running.get().attemptId();
        }
        ExamSummaryDTO checkpoint =
                catalog.checkpoint(codeSkill(path.getSkill()), path.getTargetLevel());
        path.setCheckpointExamId(checkpoint.examId());
        learningPathRepository.save(path);
        List<AttemptResultDTO> previous = checkpointStatus.history(path, engine);
        Optional<AttemptResultDTO> unfinished =
                previous.stream().filter(checkpointStatus::unfinished).findFirst();
        if (unfinished.isPresent()) return unfinished.get().attemptId();
        int last = previous.stream().mapToInt(AttemptResultDTO::attemptId).max().orElse(0);
        return engine.startCheckpoint(
                        userId,
                        checkpoint.examId(),
                        pathId,
                        "checkpoint:" + userId + ":" + pathId + ":after:" + last)
                .attemptId();
    }

    @Override
    @Transactional(readOnly = true)
    public String checkpointDestination(String email, Integer attemptId) {
        AttemptResultDTO result = engine().result(active(email).getId(), attemptId);
        return "/student/attempts/"
                + attemptId
                + ("IN_PROGRESS".equals(result.state()) ? "" : "/result");
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCheckpointStart(
            Integer userId, Integer examId, String targetLevel, String skill, Integer pathId) {
        if (pathId == null) {
            throw new BusinessRuleException("Checkpoint cần ngữ cảnh lộ trình học");
        }
        LearningPath path =
                learningPathRepository
                        .findByIdAndUserId(pathId, userId)
                        .orElseThrow(
                                () -> new BusinessRuleException("Lộ trình không thuộc học viên"));
        String normSkill = skill == null ? "" : skill.trim().toUpperCase(Locale.ROOT);
        String pathNormSkill =
                path.getSkill() == null ? "" : path.getSkill().trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(path.getStatus())
                || !Objects.equals(path.getCheckpointExamId(), examId)
                || !Objects.equals(path.getTargetLevel(), targetLevel)
                || !Set.of("A1", "A2", "B1", "B2", "C1")
                        .contains(Objects.toString(path.getTargetLevel(), ""))
                || !pathNormSkill.equals(normSkill)) {
            throw new BusinessRuleException("Checkpoint không khớp chặng học đang hoạt động");
        }
        List<LearningPathItem> required =
                learningPathItemRepository.findByLearningPathIdOrderByPositionAsc(pathId).stream()
                        .filter(LearningPathItem::isRequired)
                        .toList();
        if (required.isEmpty()
                || required.stream()
                        .anyMatch(
                                item ->
                                        !"COMPLETED".equals(item.getStatus())
                                                || item.getCompletedAt() == null)) {
            throw new BusinessRuleException(
                    "Cần hoàn thành các bài học bắt buộc trước khi thi Checkpoint");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCheckpoint(Integer userId, Integer attemptId) {
        User user = lock(active(userId));
        if (learningPathRepository.existsByUserIdAndSourceAttemptIdAndStatus(
                userId, attemptId, "COMPLETED")) return;
        ExamAttemptService engine = engine();
        AttemptResultDTO result = engine.result(userId, attemptId);
        if (result == null
                || !Objects.equals(result.userId(), userId)
                || !Objects.equals(result.attemptId(), attemptId)
                || !"CHECKPOINT".equals(result.examType())
                || !checkpointStatus.terminal(result))
            throw new BusinessRuleException("Checkpoint chưa có kết quả hoàn tất hợp lệ");
        var originPathId = engine.checkpointOriginPathId(userId, attemptId);
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
        SkillResultDTO skill =
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
        Map<String, GoalSkillDTO> targets = accounts.activeGoalSkillsBySkill(user.getId());
        LearnerContextDTO context = accounts.context(user.getId());
        List<SkillLevelDTO> levels =
                context == null || context.levels() == null ? List.of() : context.levels();
        int changed = 0;
        for (String skill : Skill.labels()) {
            GoalSkillDTO goal = targets.get(codeSkill(skill));
            String finalGoal = goal == null ? null : goal.targetLevel();
            SkillLevelDTO assessed = assessedLevel(levels, skill);
            String from = assessed == null ? null : validLevel(assessed.estimatedLevel());
            String target = nextLevel(from);
            LearningPath current =
                    learningPathRepository
                            .findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                    user.getId(), skill, "ACTIVE")
                            .orElse(null);
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
                            goal == null ? null : goal.id(),
                            from,
                            target,
                            assessed == null ? null : assessed.sourceAttemptId(),
                            selected);
            boolean same = sameStage(current, desired);
            List<LearningPathItem> assignedItems =
                    current == null
                            ? List.of()
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

    private PathProgressDTO summaryOf(LearningPath path) {
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
        return new PathProgressDTO(
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

    private ExamAttemptService engine() {
        ExamAttemptService engine = attempts.getIfAvailable();
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
        if (CefrLevel.BELOW_A1.equalsIgnoreCase(value)) return CefrLevel.BELOW_A1;
        return rank(value) < 0 ? null : value.toUpperCase(Locale.ROOT);
    }

    private static String nextLevel(String value) {
        int next = rank(value) + 1;
        List<String> levels = CefrLevel.curriculumLevels();
        return next < levels.size() ? levels.get(next) : null;
    }

    private SkillLevelDTO assessedLevel(List<SkillLevelDTO> levels, String skill) {
        for (SkillLevelDTO level : levels) {
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
