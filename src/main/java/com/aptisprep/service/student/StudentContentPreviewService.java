package com.aptisprep.service.student;

import com.aptisprep.dto.view.StudentContentPreview.*;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;
import com.aptisprep.exception.ResourceNotFoundException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Temporary exam catalog only. Lessons, flashcards and results use persisted data. */
@Service
public class StudentContentPreviewService {
    private final MockExamRepository exams;
    private final MockExamSectionRepository sections;
    private final MockExamQuestionRepository examQuestions;

    public StudentContentPreviewService(MockExamRepository exams, MockExamSectionRepository sections,
            MockExamQuestionRepository examQuestions) {
        this.exams = exams;
        this.sections = sections;
        this.examQuestions = examQuestions;
    }

    private final Map<String, List<Entry>> entries = Map.of(
        "lessons", List.of(
            new Entry(6001, "Liên kết ý trong bài đọc", "Reading", "B1", "Bài học", "AVAILABLE", "Khám phá", "Nhận biết từ nối và trình tự ý.",
                List.of(new Fact("Thời lượng", "20 phút"), new Fact("Chủ đề", "Liên kết ý")),
                List.of(new Block("Nhận biết từ nối", "However diễn tả sự đối lập; therefore diễn tả kết quả.", "1"), new Block("Đọc và nhận xét", "I enjoy learning English. However, I sometimes find listening difficult.", "2"), new Block("Tự ôn tập", "Hai câu trên bổ sung hay đối lập ý? Tìm từ nối để giải thích.", "3"))),
            new Entry(6002, "Từ vựng về học tập", "Grammar & Vocabulary", "A2", "Flashcard", "AVAILABLE", "Khám phá", "Chạm vào thẻ để xem nghĩa.",
                List.of(new Fact("Số thẻ", "3 thẻ")), List.of(new Block("assignment", "Bài tập được giao · I finished my assignment.", "Danh từ"), new Block("improve", "Cải thiện · I want to improve my English.", "Động từ"), new Block("enrol", "Đăng ký khóa học · I plan to enrol in an English course.", "Động từ"))),
            new Entry(6003, "Nắm ý chính khi nghe", "Listening", "B1", "Bài học", "AVAILABLE", "Khám phá", "Xác định thông tin cần nghe.",
                List.of(new Fact("Thời lượng", "15 phút")), List.of(new Block("Trước khi nghe", "Đọc câu hỏi và xác định ai, ở đâu, khi nào.", "1"), new Block("Hai lượt nghe", "Lượt đầu tìm ý chính, lượt sau kiểm tra chi tiết. Chưa có âm thanh cho nội dung này.", "2"))),
            new Entry(6004, "Viết về một hoạt động yêu thích", "Writing", "B1", "Bài học", "AVAILABLE", "Khám phá", "Từ một ý tưởng đến đoạn văn rõ ràng.",
                List.of(new Fact("Thời lượng", "20 phút"), new Fact("Chủ đề", "Sở thích")),
                List.of(new Block("Lên ý tưởng", "Chọn một hoạt động bạn thích. Ghi ba ý: hoạt động đó là gì, bạn làm khi nào và vì sao bạn yêu thích.", "1"), new Block("Kết nối các câu", "I enjoy reading in my free time. I usually read before bed because it helps me relax.", "2"), new Block("Tự kiểm tra", "Đã trả lời đủ ba ý chưa? Kiểm tra thì hiện tại đơn, từ nối because và dấu kết thúc câu.", "3"))),
            new Entry(6005, "Giới thiệu sở thích bằng tiếng Anh", "Speaking", "A2", "Bài học", "AVAILABLE", "Khám phá", "Nói rõ ý, bắt đầu từ điều quen thuộc.",
                List.of(new Fact("Thời lượng", "15 phút"), new Fact("Chủ đề", "Sở thích")),
                List.of(new Block("Chuẩn bị từ khóa", "Chọn một sở thích và ba từ khóa. Không cần viết toàn bộ bài nói trước khi luyện tập.", "1"), new Block("Mở rộng câu trả lời", "I like cooking. I often cook with my family at weekends. It is fun, and I can try new dishes.", "2"), new Block("Tự luyện nói", "Thử nói trong khoảng 30 giây. Nêu sở thích, tần suất và lý do; nói theo nhịp tự nhiên, không cần học thuộc từng từ.", "3")))),
        "exams", List.of(
            new Entry(7001, "Thi thử tổng hợp số 01", "Tổng hợp", "B2", "Thi thử", "AVAILABLE", "Xem cấu trúc", "Làm quen các phần thi Aptis.",
                List.of(new Fact("Tổng thời gian", "165 phút"), new Fact("Lượt nghe tối đa", "2 lượt mỗi đoạn")), List.of(new Block("Grammar & Vocabulary", "50 câu", "25 phút"), new Block("Reading", "4 phần", "35 phút"), new Block("Listening", "4 phần", "40 phút"), new Block("Writing", "4 phần", "50 phút"), new Block("Speaking", "4 phần", "15 phút"))),
            new Entry(7002, "Luyện tập Reading", "Reading", "B1", "Luyện tập", "AVAILABLE", "Xem cấu trúc", "Luyện đọc hiểu và sắp xếp ý.",
                List.of(new Fact("Thời gian", "35 phút")), List.of(new Block("Đọc hiểu", "Chọn thông tin phù hợp", "Phần 1"), new Block("Liên kết ý", "Sắp xếp đoạn văn", "Phần 2"))),
            new Entry(7003, "Luyện tập Writing", "Writing", "B2", "Luyện tập", "AVAILABLE", "Xem cấu trúc", "Luyện diễn đạt theo yêu cầu.",
                List.of(new Fact("Thời gian", "50 phút")), List.of(new Block("Trả lời ngắn", "Viết về chủ đề quen thuộc", "Phần 1–3"), new Block("Viết thư", "Điều chỉnh cách diễn đạt theo người nhận", "Phần 4")))),
        "results", List.of(
            new Entry(8001, "Luyện tập Reading", "Reading", "B1", "Luyện tập", "COMPLETED", "Đã có kết quả", "Đánh giá Reading và gợi ý ôn tập.",
                List.of(new Fact("Điểm", "34 / 50"), new Fact("CEFR ước lượng", "B1"), new Fact("Ngày làm", "16/10/2026")), List.of(new Block("Đọc hiểu", "Nắm được thông tin chính; cần chú ý chi tiết.", "34 / 50"), new Block("Gợi ý ôn tập", "Ôn cách nhận biết từ nối và liên kết ý.", "Reading"))),
            new Entry(8002, "Luyện tập Writing", "Writing", "Chưa đánh giá", "Luyện tập", "GRADING", "Chờ chấm", "Chưa có điểm hoặc nhận xét.",
                List.of(new Fact("Điểm", "Chưa có"), new Fact("Ngày làm", "16/10/2026")), List.of()),
            new Entry(8003, "Luyện tập Speaking", "Speaking", "Chưa đánh giá", "Luyện tập", "GRADING_FAILED", "Chấm chưa thành công", "Không quy đổi lỗi chấm thành điểm 0.",
                List.of(new Fact("Điểm", "Chưa có"), new Fact("Ngày làm", "15/10/2026")), List.of()))
    );

    public List<Entry> find(String module, String q, String skill, String level, String state) {
        if ("exams".equals(module) && exams.count() > 0) return realExams(q, skill, level);
        String term = normalize(q);
        return exams.stream()
            .filter(e -> normalize(e.title() + " " + e.summary()).contains(term))
            .filter(e -> skill.isBlank() || e.skill().equals(skill))
            .filter(e -> level.isBlank() || e.level().equals(level))
            .filter(e -> state.isBlank() || e.state().equals(state)).toList();
    }

    public Entry entry(String module, int id) {
        if ("exams".equals(module)) {
            if (exams.count() > 0) {
                var real = exams.findById(id).filter(exam -> "PUBLISHED".equals(exam.getStatus()));
                if (real.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy đề thi đã công bố");
                return realExamEntry(real.get());
            }
        }
        return entries.getOrDefault(module, List.of()).stream().filter(e -> e.id() == id).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nội dung"));
    }

    private List<Entry> realExams(String q, String skill, String level) {
        String term = normalize(q);
        String requestedSkill = normalizeSkill(skill);
        return exams.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .filter(exam -> normalize(exam.getTitle() + " " + exam.getDescription()).contains(term))
                .filter(exam -> requestedSkill.isBlank() || requestedSkill.equals(normalizeSkill(exam.getSkill())))
                .filter(exam -> level == null || level.isBlank() || level.equalsIgnoreCase(exam.getTargetLevel()))
                .map(this::realExamEntry)
                .toList();
    }

    private Entry realExamEntry(MockExamEntity exam) {
        List<Block> blocks = new ArrayList<>();
        List<MockExamSectionEntity> realSections = sections.findByMockExamIdOrderById(exam.getId());
        if (!realSections.isEmpty()) {
            realSections.stream().sorted(Comparator.comparingInt(MockExamSectionEntity::getDisplayOrder))
                    .forEach(section -> blocks.add(new Block(displaySkill(section.getSkill()),
                            examQuestions.findByMockExamIdOrderByPosition(exam.getId()).stream()
                                    .filter(link -> section.getId().equals(link.getMockExamSectionId())).count() + " câu hỏi",
                            section.getDurationMinutes() + " phút")));
        } else {
            blocks.add(new Block(displaySkill(exam.getSkill()),
                    examQuestions.findByMockExamIdOrderByPosition(exam.getId()).size() + " câu hỏi",
                    exam.getDurationMinutes() + " phút"));
        }
        return new Entry(exam.getId(), exam.getTitle(), displaySkill(exam.getSkill()), exam.getTargetLevel(),
                examTypeLabel(exam.getExamType()), "PUBLISHED", "Đã công bố",
                exam.getDescription() == null ? "Đề thi đã công bố" : exam.getDescription(),
                List.of(new Fact("Tổng thời gian", String.valueOf(exam.getDurationMinutes()) + " phút"),
                        new Fact("Số section", String.valueOf(Math.max(1, realSections.size())))), blocks);
    }

    private String normalizeSkill(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT).replace(" & ", "_").replace(' ', '_');
    }
    private String displaySkill(String value) {
        return switch (normalizeSkill(value)) {
            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
            case "READING" -> "Reading";
            case "LISTENING" -> "Listening";
            case "WRITING" -> "Writing";
            case "SPEAKING" -> "Speaking";
            default -> value == null ? "" : value;
        };
    }
    private String examTypeLabel(String value) {
        return switch (value == null ? "" : value) {
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            default -> "Luyện tập";
        };
    }
    private String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replace('đ', 'd');
    }
}
