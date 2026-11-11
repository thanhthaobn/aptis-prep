package com.aptisprep.service.admin;

import com.aptisprep.dto.view.AccountUnlockRequestDTO;
import com.aptisprep.dto.view.AdminStudentViewDTO;
import com.aptisprep.enums.UserStatus;
import java.util.List;

public interface StudentManagementService {
    AdminStudentViewDTO.Page list(String query, UserStatus status, int page, int size);

    AdminStudentViewDTO.Detail detail(Integer id, int attemptPage, int attemptSize);

    void lock(String adminEmail, Integer id, String reason);

    void unlock(String adminEmail, Integer id);

    void remove(String adminEmail, Integer id, String reason);

    void restore(String adminEmail, Integer id);

    List<AccountUnlockRequestDTO> listUnlockRequests(String statusFilter);

    void approveUnlockRequest(String adminEmail, Integer requestId, String note);

    void rejectUnlockRequest(String adminEmail, Integer requestId, String note);

    long pendingUnlockRequests();

    long totalStudents();

    long activeStudents();

    long lockedStudents();
}
