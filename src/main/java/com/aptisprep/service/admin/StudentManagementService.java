package com.aptisprep.service.admin;

import com.aptisprep.dto.view.AdminStudentView;
import com.aptisprep.enums.UserStatus;

public interface StudentManagementService {
    AdminStudentView.Page list(String query, UserStatus status, int page, int size);

    AdminStudentView.Detail detail(Integer id, int attemptPage, int attemptSize);

    void lock(String adminEmail, Integer id);

    void unlock(String adminEmail, Integer id);

    long totalStudents();

    long activeStudents();

    long lockedStudents();
}
