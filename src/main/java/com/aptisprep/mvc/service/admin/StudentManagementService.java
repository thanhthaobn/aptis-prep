package com.aptisprep.mvc.service.admin;

import com.aptisprep.mvc.dto.UserDTO;
import com.aptisprep.mvc.dto.UserGoalDTO;
import com.aptisprep.mvc.dto.UserSkillProfileDTO;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryPage;
import com.aptisprep.mvc.support.contract.IntegrationModels.PathProgress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public interface StudentManagementService {
    public Page<UserDTO> list(String query, UserStatus status, int page, int size);

    public UserDTO detail(Integer id);

    public UserGoalDTO goal(Integer userId);

    public List<UserSkillProfileDTO> skills(Integer userId);

    public List<PathProgress> progress(Integer userId);

    public HistoryPage attempts(Integer userId, int page, int size);

    public Map<Integer, String> targetLabels(Collection<Integer> userIds);

    public void lock(String adminEmail, Integer id, String reason);

    public void unlock(String adminEmail, Integer id);

    public void remove(String adminEmail, Integer id, String reason);

    public void restore(String adminEmail, Integer id);

    public long totalStudents();

    public long activeStudents();

    public long lockedStudents();
}
