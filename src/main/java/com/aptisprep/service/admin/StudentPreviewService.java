package com.aptisprep.service.admin;

import com.aptisprep.dto.view.StudentPreview;
import com.aptisprep.dto.view.StudentPreview.Skill;
import com.aptisprep.dto.view.StudentPreview.Attempt;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.ResourceNotFoundException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Read-only UI data. No repository, database mutation or account status changes. */
@Service
public class StudentPreviewService {
    private final List<StudentPreview> students = List.of(
        new StudentPreview(1001, "Nguyễn Minh Anh", "minhanh@example.com", "MA", "01/10/2026", UserStatus.ACTIVE,
            "B2", "30/12/2026", 8, 12, 42,
            List.of(new Skill("Grammar & Vocabulary", "B1", "B2"), new Skill("Reading", "B1", "B2"),
                new Skill("Listening", "B1", "B2"), new Skill("Writing", "A2", "B2"), new Skill("Speaking", "A2", "B2")),
            List.of(new Attempt("Thi thử tổng hợp", "15/10/2026", "Đã hoàn thành", "132 / 200"),
                new Attempt("Luyện tập Reading", "12/10/2026", "Đã hoàn thành", "34 / 50"))),
        new StudentPreview(1002, "Trần Hoàng Nam", "hoangnam@example.com", "HN", "04/10/2026", UserStatus.LOCKED,
            "B1", "20/12/2026", 3, 10, 18,
            List.of(new Skill("Grammar & Vocabulary", "A2", "B1"), new Skill("Reading", "A2", "B1"),
                new Skill("Listening", "A2", "B1"), new Skill("Writing", "Chưa đánh giá", "B1"), new Skill("Speaking", "Chưa đánh giá", "B1")),
            List.of(new Attempt("Kiểm tra đầu vào", "05/10/2026", "Chờ chấm", "Chưa có điểm"))),
        new StudentPreview(1003, "Lê Thu Hà", "thuha@example.com", "TH", "16/10/2026", UserStatus.ACTIVE,
            "Chưa đặt", "Chưa đặt", 0, 0, 0,
            List.of(new Skill("Grammar & Vocabulary", "Chưa đánh giá", "Chưa đặt"), new Skill("Reading", "Chưa đánh giá", "Chưa đặt"),
                new Skill("Listening", "Chưa đánh giá", "Chưa đặt"), new Skill("Writing", "Chưa đánh giá", "Chưa đặt"), new Skill("Speaking", "Chưa đánh giá", "Chưa đặt")), List.of()),
        new StudentPreview(1004, "Phạm Gia Huy", "giahuy@example.com", "GH", "02/10/2026", UserStatus.INACTIVE,
            "B2", "Chưa đặt", 0, 0, 0, List.of(), List.of())
    );

    public List<StudentPreview> find(String query, UserStatus status) {
        String term = normalize(query);
        return students.stream().filter(student -> status == null || student.status() == status)
                .filter(student -> normalize(student.fullName() + " " + student.email()).contains(term)).toList();
    }
    public StudentPreview get(int id) {
        return students.stream().filter(student -> student.id() == id).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
    }
    public int total() { return students.size(); }
    public long activeCount() { return students.stream().filter(s -> s.status() == UserStatus.ACTIVE).count(); }
    public long lockedCount() { return students.stream().filter(s -> s.status() == UserStatus.LOCKED).count(); }
    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
    }
}
