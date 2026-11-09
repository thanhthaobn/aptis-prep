package com.aptisprep.service.student;

import com.aptisprep.dto.view.StudentCatalogView.Entry;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentExamCatalogService {
    List<String> EXAM_TYPES = List.of("PRACTICE", "FULL_MOCK", "PLACEMENT", "CHECKPOINT");
    List<String> SORT_OPTIONS = List.of("level", "newest", "duration");

    record Category(String code, String label, long count) {}
    record Catalog(Page<Entry> page, List<Category> categories) {}

    List<Entry> find(String module, String query, String skill, String level, String state);
    Catalog browse(String query, String skill, String level, String state, String examType, String sort, Pageable pageable);
    Entry entry(String module, int id);

    static String categoryLabel(String type) {
        return switch (type) {
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            default -> "Luyện từng kỹ năng";
        };
    }
}
