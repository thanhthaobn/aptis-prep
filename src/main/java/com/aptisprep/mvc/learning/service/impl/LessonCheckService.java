package com.aptisprep.mvc.learning.service.impl;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.service.UserService;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.learning.dto.LessonFormDTO;
import com.aptisprep.mvc.learning.dto.LessonFormDTO.CheckForm;
import com.aptisprep.mvc.learning.dto.LessonTeachingViewsDTO.AnswerFeedbackDTO;
import com.aptisprep.mvc.learning.dto.LessonTeachingViewsDTO.CheckViewDTO;
import com.aptisprep.mvc.learning.dto.LessonTeachingViewsDTO.FeedbackDTO;
import com.aptisprep.mvc.learning.entity.LessonCheck;
import com.aptisprep.mvc.learning.entity.LessonCheckAnswerEntity;
import com.aptisprep.mvc.learning.entity.LessonCheckAttempt;
import com.aptisprep.mvc.learning.entity.LessonCheckFeedbackEntity;
import com.aptisprep.mvc.learning.repository.LessonCheckAttemptRepository;
import com.aptisprep.mvc.learning.repository.LessonCheckRepository;
import com.aptisprep.mvc.learning.repository.LessonRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Knowledge-check authoring, validation and scoring; helper in learning feature. */
@Component
@Transactional
public class LessonCheckService {
    private final LessonCheckRepository lessonCheckRepository;
    private final LessonRepository lessonRepository;
    private final UserService userRepository;
    private final LessonCheckAttemptRepository lessonCheckAttemptRepository;

    public LessonCheckService(
            LessonCheckRepository lessonCheckRepository,
            LessonRepository lessonRepository,
            UserService userRepository,
            LessonCheckAttemptRepository lessonCheckAttemptRepository) {
        this.lessonCheckRepository = lessonCheckRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.lessonCheckAttemptRepository = lessonCheckAttemptRepository;
    }

    public boolean hasChecks(Integer lessonId) {
        return lessonCheckRepository.countByLessonId(lessonId) > 0;
    }

    public List<CheckViewDTO> questions(Integer id) {
        return lessonCheckRepository.findByLessonIdOrderByPositionAsc(id).stream()
                .map(q -> new CheckViewDTO(q.getId(), q.getPrompt(), options(q)))
                .toList();
    }

    public List<LessonFormDTO.CheckForm> editorQuestions(Integer id) {
        return lessonCheckRepository.findByLessonIdOrderByPositionAsc(id).stream()
                .map(
                        q -> {
                            CheckForm f = new LessonFormDTO.CheckForm();
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

    public void validateChecks(List<LessonFormDTO.CheckForm> forms) {
        if (forms == null || forms.size() > 20)
            throw new BusinessRuleException("Mỗi bài tối đa 20 câu kiểm tra kiến thức");
        for (CheckForm q : forms) {
            if (empty(q)) continue;
            List<String> opts = options(q);
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
        for (CheckForm q : forms) {
            boolean gap = false;
            for (String option :
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

    public void saveChecks(Integer id, List<LessonFormDTO.CheckForm> forms) {
        lessonCheckRepository.deleteByLessonId(id);
        lessonCheckRepository.flush();
        int position = 1;
        for (CheckForm f : forms) {
            if (empty(f)) continue;
            LessonCheck q = new LessonCheck();
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
            lessonCheckRepository.save(q);
        }
    }

    public FeedbackDTO grade(Integer id, Map<String, String> answers, String email) {
        User u =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (u.getStatus() != UserStatus.ACTIVE || u.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
        lessonRepository
                .findById(id)
                .filter(l -> "PUBLISHED".equals(l.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        List<LessonCheck> all = lessonCheckRepository.findByLessonIdOrderByPositionAsc(id);
        if (all.isEmpty()) throw new BusinessRuleException("Bài chưa có câu kiểm tra kiến thức");
        ArrayList<AnswerFeedbackDTO> feedback = new ArrayList<>();
        int score = 0;
        for (LessonCheck q : all) {
            String selected = answers.get("q" + q.getId());
            List<String> options = options(q);
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
                    new AnswerFeedbackDTO(
                            q.getPrompt(),
                            options.get(value),
                            options.get(q.getCorrectOption()),
                            right,
                            q.getExplanation(),
                            options));
        }
        LessonCheckAttempt attempt = new LessonCheckAttempt();
        attempt.setUserId(u.getId());
        attempt.setLessonId(id);
        attempt.setScore(score);
        attempt.setTotal(all.size());
        for (LessonCheck q : all) {
            String selected = answers.get("q" + q.getId());
            LessonCheckAnswerEntity answer = new LessonCheckAnswerEntity();
            answer.setQuestionId(q.getId());
            answer.setSelectedOption(Integer.valueOf(selected));
            attempt.addAnswer(answer);
        }
        for (AnswerFeedbackDTO value : feedback) {
            LessonCheckFeedbackEntity review = new LessonCheckFeedbackEntity();
            review.setPrompt(value.prompt());
            review.setSelected(value.selected());
            review.setCorrect(value.correct());
            review.setRightAnswer(value.right());
            review.setExplanation(value.explanation());
            review.setOptionsText(String.join("\u001f", value.options()));
            attempt.addFeedback(review);
        }
        attempt.snapshot(lessonRepository.findById(id).orElseThrow());
        lessonCheckAttemptRepository.save(attempt);
        return new FeedbackDTO(score, all.size(), feedback);
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

    private static List<String> options(LessonFormDTO.CheckForm q) {
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

    private static boolean empty(LessonFormDTO.CheckForm q) {
        return !has(q.getPrompt()) && options(q).isEmpty() && !has(q.getExplanation());
    }

    private static boolean has(String value) {
        return value != null && !value.isBlank();
    }
}
