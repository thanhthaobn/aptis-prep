package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.entity.MockExam;

/** Kiểm tra quyền và điều kiện hoàn thành lộ trình trước khi bắt đầu checkpoint. */
public interface CheckpointStartService {
    public void validate(Integer userId, MockExam exam, Integer pathId);

    /** Cho phép làm lại checkpoint đã đạt từ đúng chặng đã hoàn thành. */
    public void validateForRetake(Integer userId, MockExam exam, Integer pathId);
}
