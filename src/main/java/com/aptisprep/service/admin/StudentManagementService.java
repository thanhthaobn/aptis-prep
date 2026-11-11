package com.aptisprep.service.admin;

import com.aptisprep.dto.AccountUnlockRequestDTO;
import com.aptisprep.dto.AdminStudentViewDTO;
import com.aptisprep.enums.UserStatus;
import java.util.List;

public interface StudentManagementService {
    public AdminStudentViewDTO.Page list(String query, UserStatus status, int page, int size);

    public AdminStudentViewDTO.Detail detail(Integer id, int attemptPage, int attemptSize);

    public void lock(String adminEmail, Integer id, String reason);

    public void unlock(String adminEmail, Integer id);

    public void remove(String adminEmail, Integer id, String reason);

    public void restore(String adminEmail, Integer id);

    public List<AccountUnlockRequestDTO> listUnlockRequests(String statusFilter);

    public void approveUnlockRequest(String adminEmail, Integer requestId, String note);

    public void rejectUnlockRequest(String adminEmail, Integer requestId, String note);

    public long pendingUnlockRequests();

    public long totalStudents();

    public long activeStudents();

    public long lockedStudents();
}
