package com.aptisprep.mvc.service.student;

import com.aptisprep.mvc.dto.MockExamDTO;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentExamCatalogService {
    public List<MockExamDTO> find(
            String module, String query, String skill, String level, String state);

    public Page<MockExamDTO> browse(
            String query,
            String skill,
            String level,
            String state,
            String examType,
            String sort,
            Pageable pageable);

    public Map<String, Long> categoryCounts(String query, String skill, String level, String state);

    public MockExamDTO entry(String module, int id);

    public String categoryLabel(String type);

    public List<String> examTypes();

    public List<String> sortOptions();
}
