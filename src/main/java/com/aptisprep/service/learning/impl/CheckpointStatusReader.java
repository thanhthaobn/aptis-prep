package com.aptisprep.service.learning.impl;

import com.aptisprep.dto.form.HistoryFilterDTO;
import com.aptisprep.dto.view.AttemptResultDTO;
import com.aptisprep.dto.view.HistoryPageDTO;
import com.aptisprep.entity.LearningPath;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.service.admin.ExamCatalogService;
import com.aptisprep.service.exam.ExamAttemptService;
import java.time.ZoneId;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Checkpoint read/status rules shared by stage summaries and checkpoint commands. */
@Component
final class CheckpointStatusReader {
    private final ObjectProvider<ExamAttemptService> attempts;
    private final ExamCatalogService catalog;

    CheckpointStatusReader(
            ObjectProvider<ExamAttemptService> attempts, ExamCatalogService catalog) {
        this.attempts = attempts;
        this.catalog = catalog;
    }

    String state(LearningPath path) {
        ExamAttemptService engine = attempts.getIfAvailable();
        if (engine != null && path.getCheckpointExamId() != null) {
            List<AttemptResultDTO> history = history(path, engine);
            if (!history.isEmpty()) {
                AttemptResultDTO latest = history.getFirst();
                if (unfinished(latest))
                    return "IN_PROGRESS".equals(latest.state()) ? "in_progress" : "grading";
                if (latest.skills() != null
                        && latest.skills().stream()
                                .anyMatch(skill -> "ERROR".equals(skill.gradingState())))
                    return "error";
                if (terminal(latest)) {
                    boolean passed =
                            latest.skills() != null
                                    && latest.skills().stream()
                                            .anyMatch(
                                                    skill ->
                                                            codeSkill(path.getSkill())
                                                                            .equals(
                                                                                    codeSkill(
                                                                                            skill
                                                                                                    .skill()))
                                                                    && skill.passed());
                    return passed ? "grading" : "failed";
                }
            }
        }
        try {
            catalog.checkpoint(codeSkill(path.getSkill()), path.getTargetLevel());
            return "available";
        } catch (ResourceNotFoundException | BusinessRuleException missing) {
            return "not_configured";
        }
    }

    List<AttemptResultDTO> history(LearningPath path, ExamAttemptService engine) {
        HistoryPageDTO history =
                engine.history(
                        path.getUser().getId(),
                        0,
                        100,
                        new HistoryFilterDTO("", codeSkill(path.getSkill()), "CHECKPOINT", ""));
        if (history == null || history.items() == null) return List.of();
        return history.items().stream()
                .filter(result -> Objects.equals(result.examId(), path.getCheckpointExamId()))
                .filter(result -> result.submittedAt() == null || notBeforePath(result, path))
                .sorted(Comparator.comparing(AttemptResultDTO::attemptId).reversed())
                .toList();
    }

    boolean unfinished(AttemptResultDTO result) {
        return "IN_PROGRESS".equals(result.state())
                || "SUBMITTED".equals(result.state())
                || "WAITING_GRADING".equals(result.state())
                || ("EXPIRED".equals(result.state())
                        && (result.skills() == null
                                || result.skills().stream()
                                        .anyMatch(
                                                skill ->
                                                        "PENDING".equals(skill.gradingState())
                                                                || "WAITING"
                                                                        .equals(
                                                                                skill
                                                                                        .gradingState())
                                                                || "GRADING"
                                                                        .equals(
                                                                                skill
                                                                                        .gradingState()))));
    }

    boolean terminal(AttemptResultDTO result) {
        return "COMPLETED".equals(result.state()) || "EXPIRED".equals(result.state());
    }

    boolean notBeforePath(AttemptResultDTO result, LearningPath path) {
        return result.submittedAt() != null
                && (path.getCreatedAt() == null
                        || !result.submittedAt()
                                .isBefore(
                                        path.getCreatedAt()
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()));
    }

    private static String codeSkill(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }
}
