package com.aptisprep.service.learning;

import com.aptisprep.repository.LessonProgressRepository;
import com.aptisprep.service.contract.AccountAccessContract;
import com.aptisprep.service.contract.IntegrationModels.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Read-only dashboard selection; never creates progress or changes the learner's goal. */
@Service
public class LessonRecommendationService {
    private final LessonProgressRepository progress;
    private final AccountAccessContract accounts;

    public LessonRecommendationService(
            LessonProgressRepository progress, AccountAccessContract accounts) {
        this.progress = progress;
        this.accounts = accounts;
    }

    public record Recommendation(
            Integer lessonId, String title, String skill, PathProgress path, String reason) {}

    @Transactional(readOnly = true)
    public Recommendation recommend(Integer userId, List<PathProgress> paths) {
        var history = progress.findByUserId(userId);
        var recent =
                history.stream()
                        .filter(
                                p ->
                                        "IN_PROGRESS".equals(p.getStatus())
                                                && p.getCompletedAt() == null)
                        .filter(p -> "PUBLISHED".equals(p.getLesson().getStatus()))
                        .max(
                                Comparator.comparing(
                                        p ->
                                                p.getLastAccessedAt() == null
                                                        ? LocalDateTime.MIN
                                                        : p.getLastAccessedAt()));
        if (recent.isPresent()) {
            var lesson = recent.get().getLesson();
            var path =
                    paths.stream()
                            .filter(
                                    p ->
                                            "ACTIVE".equals(p.pathStatus())
                                                    && Objects.equals(
                                                            p.nextLessonId(), lesson.getId()))
                            .findFirst()
                            .orElse(null);
            return new Recommendation(
                    lesson.getId(),
                    lesson.getTitle(),
                    lesson.getSkill(),
                    path,
                    "Tiếp tục bài đang học dở gần nhất");
        }
        var context = accounts.context(userId);
        List<SkillTarget> targets =
                context == null || context.targets() == null ? List.of() : context.targets();
        List<SkillLevel> levels =
                context == null || context.levels() == null ? List.of() : context.levels();
        var selected =
                paths.stream()
                        .filter(p -> "ACTIVE".equals(p.pathStatus()) && p.nextLessonId() != null)
                        .min(
                                Comparator.<PathProgress>comparingInt(
                                                p -> hasTarget(p, targets) ? 0 : 1)
                                        .thenComparing(
                                                Comparator.comparingInt(
                                                                (PathProgress p) ->
                                                                        gap(p, targets, levels))
                                                        .reversed())
                                        .thenComparingDouble(
                                                p ->
                                                        p.totalLessons() == 0
                                                                ? 1
                                                                : (double) p.completedLessons()
                                                                        / p.totalLessons())
                                        // Equal priorities rotate toward the skill least recently
                                        // visited, never skill-name ordering.
                                        .thenComparing(p -> lastVisit(history, p.skill()))
                                        .thenComparing(PathProgress::pathId))
                        .orElse(null);
        if (selected == null) return null;
        String reason =
                hasTarget(selected, targets)
                        ? "Theo mục tiêu: ưu tiên khoảng cách trình độ lớn hơn, rồi tiến độ chặng"
                                + " thấp hơn"
                        : "Theo tiến độ: ưu tiên chặng ít hoàn thành hơn, rồi kỹ năng lâu chưa học";
        return new Recommendation(
                selected.nextLessonId(),
                selected.nextLessonTitle(),
                selected.skill(),
                selected,
                reason);
    }

    private LocalDateTime lastVisit(
            List<com.aptisprep.entity.LessonProgress> history, String skill) {
        return history.stream()
                .filter(p -> skill.equals(p.getLesson().getSkill()))
                .map(p -> p.getLastAccessedAt())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
    }

    private boolean hasTarget(PathProgress path, List<SkillTarget> targets) {
        return targets.stream().anyMatch(t -> key(t.skill()).equals(key(path.skill())));
    }

    private int gap(PathProgress path, List<SkillTarget> targets, List<SkillLevel> levels) {
        int target =
                targets.stream()
                        .filter(t -> key(t.skill()).equals(key(path.skill())))
                        .mapToInt(t -> rank(t.targetLevel()))
                        .findFirst()
                        .orElse(-1);
        int current =
                levels.stream()
                        .filter(l -> key(l.skill()).equals(key(path.skill())))
                        .mapToInt(l -> rank(l.estimatedLevel()))
                        .findFirst()
                        .orElse(-1);
        // Unknown proficiency is not invented or treated as a measured weakness.
        return target < 0 || current < 0 ? 0 : Math.max(0, target - current);
    }

    private static int rank(String value) {
        return List.of("A1", "A2", "B1", "B2", "C1", "C2")
                .indexOf(value == null ? "" : value.toUpperCase(Locale.ROOT));
    }

    private static String key(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
