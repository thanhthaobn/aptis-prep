package com.aptisprep.service.admin;

import com.aptisprep.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

/** Layout fixtures only: not a scoring engine or official Aptis thresholds. */
@Service
public class ScoringPreviewService {
    public record Certificate(int id, String name, String code, String description, String status) {}
    public record Scheme(int id, int certificateId, String name, String version, String status, String effectiveFrom) {}
    public record Band(String level, String min, String max) {}
    public List<Certificate> certificates() {
        return List.of(new Certificate(9101, "Aptis ESOL General", "APTIS", "Chương trình học tập và luyện thi Aptis.", "Đang sử dụng"));
    }
    public List<Scheme> schemes() {
        return List.of(new Scheme(9201, 9101, "Đánh giá CEFR ước lượng", "1.0", "Đang sử dụng", "2026-10-17T08:00"));
    }
    public Certificate certificate(int id) {
        return certificates().stream().filter(c -> c.id() == id).findFirst().orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
    }
    public Scheme scheme(int id) {
        return schemes().stream().filter(s -> s.id() == id).findFirst().orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
    }
    public List<Band> bands() {
        return List.of(new Band("A1", "0", "9.99"),new Band("A2", "10", "19.99"),new Band("B1", "20", "29.99"),new Band("B2", "30", "39.99"),new Band("C1", "40", "45.99"),new Band("C2", "46", "50"));
    }
}
