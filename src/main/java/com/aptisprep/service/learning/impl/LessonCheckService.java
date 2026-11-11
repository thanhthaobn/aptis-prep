package com.aptisprep.service.learning.impl;

import com.aptisprep.dto.form.LessonFormDTO;
import com.aptisprep.dto.form.LessonFormDTO.CheckForm;
import com.aptisprep.dto.view.LessonTeachingViewsDTO.AnswerFeedbackDTO;
import com.aptisprep.dto.view.LessonTeachingViewsDTO.CheckViewDTO;
import com.aptisprep.dto.view.LessonTeachingViewsDTO.FeedbackDTO;
import com.aptisprep.entity.LessonCheck;
import com.aptisprep.entity.LessonCheckAnswerEntity;
import com.aptisprep.entity.LessonCheckAttempt;
import com.aptisprep.entity.LessonCheckFeedbackEntity;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.LessonCheckAttemptRepository;
import com.aptisprep.repository.LessonCheckRepository;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.service.account.UserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Knowledge-check authoring, validation and scoring; helper in learning feature. */
@Component
@Transactional
public class LessonCheckService {
    public static final int PASS_PERCENT = 80;
    private static final int QUESTIONS_PER_ATTEMPT = 5;

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
                .map(q -> view(q, null))
                .toList();
    }

    public List<CheckViewDTO> questions(Integer id, String email) {
        User user = requireLearner(email);
        List<LessonCheck> questions = questionsForAttempt(id, user);
        long attemptNumber = attemptNumber(user.getId(), id);
        return questions.stream()
                .map(q -> view(q, optionSeed(user.getId(), id, attemptNumber, q.getId())))
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
        User u = requireLearner(email);
        lessonRepository
                .findById(id)
                .filter(l -> "PUBLISHED".equals(l.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        List<LessonCheck> all = questionsForAttempt(id, u);
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

    public static boolean passed(int score, int total) {
        return total > 0 && score * 100 >= total * PASS_PERCENT;
    }

    private User requireLearner(String email) {
        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
        return user;
    }

    private List<LessonCheck> questionsForAttempt(Integer lessonId, User user) {
        List<LessonCheck> all =
                new ArrayList<>(lessonCheckRepository.findByLessonIdOrderByPositionAsc(lessonId));
        if (all.size() <= QUESTIONS_PER_ATTEMPT) return all;
        Collections.shuffle(
                all,
                new Random(
                        questionSeed(
                                user.getId(), lessonId, attemptNumber(user.getId(), lessonId))));
        return List.copyOf(all.subList(0, QUESTIONS_PER_ATTEMPT));
    }

    private long attemptNumber(Integer userId, Integer lessonId) {
        return lessonCheckAttemptRepository
                .findByUserIdAndLessonIdOrderBySubmittedAtDesc(userId, lessonId)
                .size();
    }

    private static long questionSeed(Integer userId, Integer lessonId, long attemptNumber) {
        return Objects.hash(userId, lessonId, attemptNumber, "questions");
    }

    private static long optionSeed(
            Integer userId, Integer lessonId, long attemptNumber, Integer questionId) {
        return Objects.hash(userId, lessonId, attemptNumber, questionId, "options");
    }

    private static CheckViewDTO view(LessonCheck question, Long seed) {
        List<String> source = options(question);
        List<Integer> order =
                IntStream.range(0, source.size()).boxed().collect(Collectors.toList());
        if (seed != null) Collections.shuffle(order, new Random(seed));
        return new CheckViewDTO(
                question.getId(),
                question.getPrompt(),
                order.stream().map(source::get).toList(),
                List.copyOf(order));
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
