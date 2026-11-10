package com.aptisprep.mvc.student.service;

import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.student.dto.AdminStudentViewDTO;

public interface StudentManagementService {
    AdminStudentViewDTO.Page list(String query, UserStatus status, int page, int size);

    AdminStudentViewDTO.Detail detail(Integer id, int attemptPage, int attemptSize);

    void lock(String adminEmail, Integer id, String reason);

    void unlock(String adminEmail, Integer id);

    void remove(String adminEmail, Integer id, String reason);

    long totalStudents();

    long activeStudents();

    long lockedStudents();
}
