package com.aptisprep.service.learning;

import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.learning.LessonTeachingService.AnswerFeedback;
import com.aptisprep.service.learning.LessonTeachingService.CheckView;
import com.aptisprep.service.learning.LessonTeachingService.Feedback;
import java.util.*;
import java.util.stream.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Knowledge-check authoring, validation and scoring; no media/AI submission responsibility. */
@Service
@Transactional
public class LessonCheckService {
    private final LessonCheckRepository checks;
    private final LessonRepository lessons;
    private final UserRepository users;
    private final LessonCheckAttemptRepository attempts;
    private final JsonMapper json;

    public LessonCheckService(
            LessonCheckRepository checks,
            LessonRepository lessons,
            UserRepository users,
            LessonCheckAttemptRepository attempts,
            JsonMapper json) {
        this.checks = checks;
        this.lessons = lessons;
        this.users = users;
        this.attempts = attempts;
        this.json = json;
    }

    public boolean hasChecks(Integer lessonId) {
        return checks.countByLessonId(lessonId) > 0;
    }

    public List<CheckView> questions(Integer id) {
        return checks.findByLessonIdOrderByPositionAsc(id).stream()
                .map(q -> new CheckView(q.getId(), q.getPrompt(), options(q)))
                .toList();
    }

    public List<LessonForm.CheckForm> editorQuestions(Integer id) {
        return checks.findByLessonIdOrderByPositionAsc(id).stream()
                .map(
                        q -> {
                            var f = new LessonForm.CheckForm();
                            f.setPrompt(q.getPrompt());
                            f.setOptionA(q.getOptionA());
                            f.setOptionB(q.getOptionB());
                            f.setOptionC(q.getOptionC());
                            f.setOptionD(q.getOptionD());
                            f.setOptionE(q.getOptionE());
                            f.setOptionF(q.getOptionF());
                            f.setCorrectOption(q.getCorrectOption());
                            f.setExplanation(q.getExplanation());
                            return f;
                        })
                .toList();
    }

    public void validateChecks(List<LessonForm.CheckForm> forms) {
        if (forms == null || forms.size() > 20)
            throw new BusinessRuleException("Mỗi bài tối đa 20 câu kiểm tra kiến thức");
        for (var q : forms) {
            if (empty(q)) continue;
            var opts = options(q);
            if (!has(q.getPrompt())
                    || opts.size() < 2
                    || !has(q.getExplanation())
                    || q.getCorrectOption() == null
                    || q.getCorrectOption() < 0
                    || q.getCorrectOption() >= opts.size())
                throw new BusinessRuleException(
                        "Mỗi câu kiểm tra cần câu hỏi, từ 2 đến 6 lựa chọn liên tiếp, đáp án đúng"
                                + " và giải thích");
        }
        for (var q : forms) {
            boolean gap = false;
            for (var option :
                    Arrays.asList(
                            q.getOptionA(),
                            q.getOptionB(),
                            q.getOptionC(),
                            q.getOptionD(),
                            q.getOptionE(),
                            q.getOptionF())) {
                if (!has(option)) gap = true;
                else if (gap)
                    throw new BusinessRuleException(
                            "Điền lựa chọn liên tiếp từ A, B. Không bỏ trống ở giữa");
            }
        }
    }

    public void saveChecks(Integer id, List<LessonForm.CheckForm> forms) {
        checks.deleteByLessonId(id);
        checks.flush();
        int position = 1;
        for (var f : forms) {
            if (empty(f)) continue;
            var q = new LessonCheck();
            q.setLessonId(id);
            q.setPosition(position++);
            q.setPrompt(f.getPrompt());
            q.setOptionA(f.getOptionA());
            q.setOptionB(f.getOptionB());
            q.setOptionC(blankToNull(f.getOptionC()));
            q.setOptionD(blankToNull(f.getOptionD()));
            q.setOptionE(blankToNull(f.getOptionE()));
            q.setOptionF(blankToNull(f.getOptionF()));
            q.setCorrectOption(f.getCorrectOption());
            q.setExplanation(f.getExplanation());
            checks.save(q);
        }
    }

    public Feedback grade(Integer id, Map<String, String> answers, String email) {
        var u =
                users.findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (u.getStatus() != UserStatus.ACTIVE || u.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
        lessons.findById(id)
                .filter(l -> "PUBLISHED".equals(l.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        var all = checks.findByLessonIdOrderByPositionAsc(id);
        if (all.isEmpty()) throw new BusinessRuleException("Bài chưa có câu kiểm tra kiến thức");
        var feedback = new ArrayList<AnswerFeedback>();
        int score = 0;
        for (var q : all) {
            var selected = answers.get("q" + q.getId());
            var options = options(q);
            int value;
            try {
                value = Integer.parseInt(selected);
            } catch (Exception ex) {
                throw new BusinessRuleException("Vui lòng trả lời đầy đủ các câu hỏi");
            }
            if (value < 0 || value >= options.size())
                throw new BusinessRuleException("Lựa chọn không hợp lệ");
            boolean right = value == q.getCorrectOption();
            if (right) score++;
            feedback.add(
                    new AnswerFeedback(
                            q.getPrompt(),
                            options.get(value),
                            options.get(q.getCorrectOption()),
                            right,
                            q.getExplanation(),
                            options));
        }
        var attempt = new LessonCheckAttempt();
        attempt.setUserId(u.getId());
        attempt.setLessonId(id);
        attempt.setScore(score);
        attempt.setTotal(all.size());
        attempt.setAnswersJson(
                answers.entrySet().stream()
                        .filter(e -> e.getKey().matches("q\\d+") && e.getValue().matches("[0-5]"))
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&")));
        attempt.setReviewJson(json.writeValueAsString(feedback));
        attempt.snapshot(lessons.findById(id).orElseThrow());
        attempts.save(attempt);
        return new Feedback(score, all.size(), feedback);
    }

    private static List<String> options(LessonCheck q) {
        return Stream.of(
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD(),
                        q.getOptionE(),
                        q.getOptionF())
                .filter(LessonCheckService::has)
                .toList();
    }

    private static List<String> options(LessonForm.CheckForm q) {
        return Stream.of(
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD(),
                        q.getOptionE(),
                        q.getOptionF())
                .filter(LessonCheckService::has)
                .toList();
    }

    private static String blankToNull(String value) {
        return has(value) ? value.trim() : null;
    }

    private static boolean empty(LessonForm.CheckForm q) {
        return !has(q.getPrompt()) && options(q).isEmpty() && !has(q.getExplanation());
    }

    private static boolean has(String value) {
        return value != null && !value.isBlank();
    }
}
