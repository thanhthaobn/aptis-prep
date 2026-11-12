package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.entity.ExamAttempt;
import org.springframework.web.multipart.MultipartFile;

/** Ghi câu trả lời trong một attempt đang được service nghiệp vụ khóa quyền truy cập. */
public interface ExamAnswerService {
    public void saveSpeakingAudio(
            ExamAttempt attempt, Integer questionId, Integer itemId, MultipartFile audioFile);

    public void saveAnswer(
            ExamAttempt attempt,
            Integer questionId,
            Integer itemId,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer);
}
