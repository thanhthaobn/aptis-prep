package com.aptisprep.controller;

import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.Skill;
import com.aptisprep.service.contract.IntegrationModels.SkillResult;
import com.aptisprep.service.learning.LessonService;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContext;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Presentation formatting shared by HTML pages; does not query or modify business data. */
@Component
public class DisplayFormatting {
    public List<String> examTypes() {
        return List.of("PLACEMENT", "PRACTICE", "CHECKPOINT", "FULL_MOCK");
    }

    public List<String> resultStates() {
        return List.of("IN_PROGRESS", "WAITING_GRADING", "COMPLETED", "ERROR", "EXPIRED");
    }

    public boolean hasPendingGrades(List<SkillResult> skills) {
        if (skills == null) return false;
        for (SkillResult skill : skills) {
            if ("PENDING".equals(skill.gradingState()) || "WAITING".equals(skill.gradingState()))
                return true;
        }
        return false;
    }

    public boolean hasGradingErrors(List<SkillResult> skills) {
        if (skills == null) return false;
        for (SkillResult skill : skills) {
            if ("ERROR".equals(skill.gradingState())) return true;
        }
        return false;
    }

    public boolean hasRetryableGrades(List<SkillResult> skills) {
        if (skills == null) return false;
        for (SkillResult skill : skills) {
            if (skill.retryable()) return true;
        }
        return false;
    }

    public String historyExamType(String type) {
        return switch (type == null ? "" : type) {
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            default -> "Bài luyện tập";
        };
    }

    public String historyStateClass(String state) {
        return switch (state == null ? "" : state) {
            case "COMPLETED" -> " is-complete";
            case "ERROR" -> " is-error";
            default -> " is-pending";
        };
    }

    public String historyState(String state) {
        return switch (state == null ? "" : state) {
            case "COMPLETED" -> "Đã hoàn thành";
            case "ERROR" -> "Chấm điểm gặp lỗi";
            case "EXPIRED" -> "Đã hết thời gian";
            case "IN_PROGRESS" -> "Đang làm bài";
            default -> "Đang chấm điểm";
        };
    }

    public String checkpointAction(String state) {
        return switch (state == null ? "" : state) {
            case "in_progress" -> "Tiếp tục bài đang làm";
            case "grading" -> "Xem trạng thái chấm";
            case "failed" -> "Làm lại kiểm tra cuối chặng";
            default -> "Bắt đầu bài kiểm tra cuối chặng";
        };
    }

    public List<String> learningLevels() {
        return LessonService.LEVELS;
    }

    public List<String> scoringLevels() {
        return CefrLevel.codes();
    }

    public List<String> skillCodes() {
        return Skill.codes();
    }

    public List<String> flashcardGroups(boolean admin) {
        return admin ? List.of("admin") : List.of("mine", "lesson");
    }

    public boolean fieldInvalid(String formName, String field) {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) return false;
        Errors errors = new RequestContext(attributes.getRequest()).getErrors(formName, false);
        return errors != null && errors.hasFieldErrors(field);
    }

    public List<String> globalErrors(String formName) {
        return formErrors(formName, true);
    }

    public List<String> allErrors(String formName) {
        return formErrors(formName, false);
    }

    private List<String> formErrors(String formName, boolean globalOnly) {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return List.of();
        }
        RequestContext context = new RequestContext(attributes.getRequest());
        Errors errors = context.getErrors(formName, false);
        if (errors == null) return List.of();
        List<String> messages = new ArrayList<>();
        List<ObjectError> selected = globalOnly ? errors.getGlobalErrors() : errors.getAllErrors();
        for (ObjectError error : selected) {
            messages.add(context.getMessage(error, false));
        }
        return messages;
    }

    public String date(TemporalAccessor value) {
        return format(value, "dd/MM/yyyy");
    }

    public String dateTime(TemporalAccessor value) {
        return format(value, "dd/MM/yyyy HH:mm");
    }

    public String dateTimeWithSeconds(TemporalAccessor value) {
        return format(value, "dd/MM/yyyy HH:mm:ss");
    }

    public String timeAndDate(TemporalAccessor value) {
        return format(value, "HH:mm dd/MM/yyyy");
    }

    private String format(TemporalAccessor value, String pattern) {
        if (value == null) return null;
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                        .withZone(ZoneId.systemDefault());
        return formatter.format(value);
    }

    public List<Integer> pageNumbers(int start, int end) {
        List<Integer> result = new ArrayList<>();
        int step = start <= end ? 1 : -1;
        for (int number = start; ; number += step) {
            result.add(number);
            if (number == end) return result;
        }
    }

    public String join(Iterable<String> values, String separator) {
        return values == null ? null : String.join(separator, values);
    }
}
