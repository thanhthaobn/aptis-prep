package com.aptisprep.mvc.service.learning.impl;

import com.aptisprep.mvc.entity.LearningPath;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.service.catalog.PublishedExamCatalogService;
import com.aptisprep.mvc.service.exam.ExamAttemptService;
import com.aptisprep.mvc.service.learning.CheckpointStatusService;
import com.aptisprep.mvc.support.contract.IntegrationModels.*;
import java.time.ZoneId;
import java.util.*;

/** Checkpoint read/status rules shared by stage summaries and checkpoint commands. */
import org.springframework.stereotype.Service;

@Service
public class CheckpointStatusServiceImpl implements CheckpointStatusService {
    private ExamAttemptService attempts;
    private PublishedExamCatalogService catalog;

    public CheckpointStatusServiceImpl(
            ExamAttemptService attempts, PublishedExamCatalogService catalog) {
        this.attempts = attempts;
        this.catalog = catalog;
    }

    @Override
    public String state(LearningPath path) {
        if (path.getCheckpointExamId() != null) {
            List<AttemptResult> history = history(path);
            if (!history.isEmpty()) {
                AttemptResult latest = history.getFirst();
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

    @Override
    public List<AttemptResult> history(LearningPath path) {
        HistoryPage history =
                attempts.history(
                        path.getUser().getId(),
                        0,
                        100,
                        new HistoryFilter("", codeSkill(path.getSkill()), "CHECKPOINT", ""));
        if (history == null || history.items() == null) return List.of();
        return history.items().stream()
                .filter(result -> Objects.equals(result.examId(), path.getCheckpointExamId()))
                .filter(result -> result.submittedAt() == null || notBeforePath(result, path))
                .sorted(Comparator.comparing((AttemptResult streamValue) -> streamValue.attemptId()).reversed())
                .toList();
    }

    @Override
    public boolean unfinished(AttemptResult result) {
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

    @Override
    public boolean terminal(AttemptResult result) {
        return "COMPLETED".equals(result.state()) || "EXPIRED".equals(result.state());
    }

    @Override
    public boolean notBeforePath(AttemptResult result, LearningPath path) {
        return result.submittedAt() != null
                && (path.getCreatedAt() == null
                        || !result.submittedAt()
                                .isBefore(
                                        path.getCreatedAt()
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()));
    }

    private String codeSkill(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }
}







