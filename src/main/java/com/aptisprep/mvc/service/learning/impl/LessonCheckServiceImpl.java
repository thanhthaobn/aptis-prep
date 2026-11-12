package com.aptisprep.mvc.service.learning.impl;

import com.aptisprep.mvc.dto.LessonCheckDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.learning.*;
import com.aptisprep.mvc.service.learning.LessonCheckService;
import com.aptisprep.mvc.service.learning.LessonTeachingService.AnswerFeedback;
import com.aptisprep.mvc.service.learning.LessonTeachingService.CheckView;
import com.aptisprep.mvc.service.learning.LessonTeachingService.Feedback;
import java.util.*;
import java.util.stream.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Knowledge-check authoring, validation and scoring; no media/AI submission responsibility. */
@Service
@Transactional
public class LessonCheckServiceImpl implements LessonCheckService {
    private LessonCheckRepository lessonCheckRepository;
    private LessonRepository lessonRepository;
    private UserRepository userRepository;
    private LessonCheckAttemptRepository lessonCheckAttemptRepository;

    public LessonCheckServiceImpl(
            LessonCheckRepository lessonCheckRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            LessonCheckAttemptRepository lessonCheckAttemptRepository) {
        this.lessonCheckRepository = lessonCheckRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.lessonCheckAttemptRepository = lessonCheckAttemptRepository;
    }

    public boolean hasChecks(Integer lessonId) {
        return lessonCheckRepository.countByLessonId(lessonId) > 0;
    }

    public List<CheckView> questions(Integer id) {
        return lessonCheckRepository.findByLessonIdOrderByPositionAsc(id).stream()
                .map(q -> new CheckView(q.getId(), q.getPrompt(), options(q)))
                .toList();
    }

    public List<LessonCheckDTO> editorQuestions(Integer id) {
        return lessonCheckRepository.findByLessonIdOrderByPositionAsc(id).stream()
                .map(
                        q -> {
                            return LessonCheckDTO.convertEntityToDto(q);
                        })
                .toList();
    }

    public void validateChecks(List<LessonCheckDTO> forms) {
        if (forms == null || forms.size() > 20)
            throw new BusinessRuleException("Mỗi bài tối đa 20 câu kiểm tra kiến thức");
        for (LessonCheckDTO q : forms) {
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
        for (LessonCheckDTO q : forms) {
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

    public void saveChecks(Integer id, List<LessonCheckDTO> forms) {
        lessonCheckRepository.deleteByLessonId(id);
        lessonCheckRepository.flush();
        int position = 1;
        for (LessonCheckDTO form : forms) {
            if (empty(form)) continue;
            form.setLessonId(id);
            form.setPosition(position++);
            // The existing checks were deleted above. Generated IDs must not be
            // reused when the submitted DTO still contains an old row ID.
            form.setId(null);
            LessonCheck q = LessonCheckDTO.convertDtoToEntity(form);
            q.setOptionC(blankToNull(q.getOptionC()));
            q.setOptionD(blankToNull(q.getOptionD()));
            q.setOptionE(blankToNull(q.getOptionE()));
            q.setOptionF(blankToNull(q.getOptionF()));
            lessonCheckRepository.save(q);
        }
    }

    public Feedback grade(Integer id, Map<String, String> answers, String email) {
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
        ArrayList<AnswerFeedback> feedback = new ArrayList<AnswerFeedback>();
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
                    new AnswerFeedback(
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
            LessonCheckAnswer answer = new LessonCheckAnswer();
            answer.setQuestionId(q.getId());
            answer.setSelectedOption(Integer.valueOf(selected));
            attempt.addAnswer(answer);
        }
        for (AnswerFeedback value : feedback) {
            LessonCheckFeedback review = new LessonCheckFeedback();
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
        boolean passed = score * 100 >= all.size() * 60;
        // Không trả đáp án đúng/giải thích khi chưa đạt. Nếu không, người học có thể
        // sao chép toàn bộ đáp án sang lần làm tiếp theo để vượt qua điều kiện hoàn thành.
        return new Feedback(score, all.size(), passed ? List.copyOf(feedback) : List.of());
    }

    private List<String> options(LessonCheck q) {
        return Stream.of(
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD(),
                        q.getOptionE(),
                        q.getOptionF())
                .filter(streamValue -> has(streamValue))
                .toList();
    }

    private List<String> options(LessonCheckDTO q) {
        return Stream.of(
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD(),
                        q.getOptionE(),
                        q.getOptionF())
                .filter(streamValue -> has(streamValue))
                .toList();
    }

    private String blankToNull(String value) {
        return has(value) ? value.trim() : null;
    }

    private boolean empty(LessonCheckDTO q) {
        return !has(q.getPrompt()) && options(q).isEmpty() && !has(q.getExplanation());
    }

    private boolean has(String value) {
        return value != null && !value.isBlank();
    }
}
