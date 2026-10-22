package com.aptisprep.service.admin;

import com.aptisprep.dto.view.AdminCatalogPreview.Entry;
import com.aptisprep.dto.view.AdminCatalogPreview.Field;
import com.aptisprep.dto.view.AdminCatalogPreview.Module;
import com.aptisprep.dto.view.AdminCatalogPreview.Row;
import com.aptisprep.dto.view.AdminCatalogPreview.CatalogPage;
import com.aptisprep.exception.ResourceNotFoundException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Read-only examples for layout review; never reads or writes application data.
 */
@Service
public class AdminCatalogPreviewService {
    private final Map<String, Module> modules = Map.of(
            "lessons", new Module("lessons", "Bài học", "Tổ chức bài học và Flashcard theo kỹ năng, cấp độ.", "bài học", "Tạo bài học", "Nội dung", List.of("Bài học", "Flashcard"), List.of(
                    new Entry(2001, "Liên kết ý trong bài đọc", "Nhận diện từ nối và trình tự ý trong đoạn văn.", "Reading", "B1", "Bài học", "PUBLISHED", "Đã công bố", "16/10/2026",
                            List.of(new Field("Thời lượng gợi ý", "20 phút"), new Field("Flashcard", "8 thẻ"), new Field("Chủ đề", "Liên kết ý")),
                            List.of(new Row("1. Nhận biết từ nối", "Tìm các dấu hiệu bổ sung, đối lập và kết quả.", "5 phút"), new Row("2. Sắp xếp ý", "Xác định câu chủ đề và các ý bổ trợ.", "10 phút"), new Row("3. Tự kiểm tra", "Ôn lại kiến thức và các lỗi thường gặp.", "5 phút"))),
                    new Entry(2002, "Từ vựng về học tập", "Bộ Flashcard theo chủ đề trường học và khóa học.", "Grammar & Vocabulary", "A2", "Flashcard", "DRAFT", "Bản nháp", "15/10/2026",
                            List.of(new Field("Số thẻ", "12 thẻ"), new Field("Chủ đề", "Học tập")), List.of(new Row("enrol", "đăng ký tham gia khóa học", "Động từ"), new Row("assignment", "bài tập được giao", "Danh từ"))),
                    new Entry(2003, "Nắm ý chính khi nghe", "Luyện nghe theo chủ đề giao tiếp hằng ngày.", "Listening", "B1", "Bài học", "ARCHIVED", "Đã lưu trữ", "10/10/2026", List.of(new Field("Thời lượng gợi ý", "15 phút")), List.of()))),
            "questions", new Module("questions", "Ngân hàng câu hỏi", "Quản lý nội dung, phương án trả lời và tài nguyên đính kèm.", "câu hỏi", "Thêm câu hỏi", "Dạng câu hỏi", List.of("Chọn một đáp án", "Ghép nối", "Sắp xếp", "Trả lời bằng văn bản", "Trả lời bằng âm thanh"), List.of(
                    new Entry(3001, "Chọn từ phù hợp trong câu", "The course provides a very _____ introduction to academic writing.", "Grammar & Vocabulary", "B1", "Chọn một đáp án", "PUBLISHED", "Đã công bố", "16/10/2026",
                            List.of(new Field("Số ý hỏi", "1"), new Field("Phần thi", "1"), new Field("Độ khó", "Trung bình")), List.of(new Row("A. benefit", "Phương án trả lời", ""), new Row("B. useful", "Phương án trả lời", "Đáp án đúng"), new Row("C. success", "Phương án trả lời", ""))),
                    new Entry(3002, "Viết thư cho câu lạc bộ", "Write an email to your club organiser suggesting a new activity.", "Writing", "B2", "Trả lời bằng văn bản", "DRAFT", "Bản nháp", "15/10/2026", List.of(new Field("Số từ", "120–150 từ"), new Field("Phần thi", "4")), List.of(new Row("Yêu cầu", "Trình bày đề xuất, lý do và lời kết phù hợp.", "Tự luận"))),
                    new Entry(3003, "Giới thiệu sở thích", "Tell us about an activity you enjoy in your free time.", "Speaking", "A2", "Trả lời bằng âm thanh", "PUBLISHED", "Đã công bố", "14/10/2026", List.of(new Field("Thời gian trả lời", "30 giây"), new Field("Phần thi", "1")), List.of(new Row("Yêu cầu", "Nêu hoạt động, tần suất và lý do yêu thích.", "Ghi âm"))))),
            "exams", new Module("exams", "Đề thi", "Sắp xếp câu hỏi, thời gian và phạm vi kỹ năng của từng đề.", "đề thi", "Tạo đề thi", "Loại đề", List.of("Kiểm tra đầu vào", "Luyện tập", "Kiểm tra cuối chặng", "Thi thử tổng hợp"), List.of(
                    new Entry(4001, "Thi thử tổng hợp số 01", "Bài thi mô phỏng đủ các kỹ năng Aptis.", "Tổng hợp", "B2", "Thi thử tổng hợp", "PUBLISHED", "Đã công bố", "16/10/2026", List.of(new Field("Chứng chỉ", "Aptis"), new Field("Bộ chấm điểm", "Ước lượng — phiên bản 1"), new Field("Tổng thời gian", "165 phút")), List.of(new Row("Grammar & Vocabulary", "50 câu", "25 phút"), new Row("Reading", "4 phần", "35 phút"), new Row("Listening", "4 phần", "40 phút"), new Row("Writing", "4 phần", "50 phút"), new Row("Speaking", "4 phần", "15 phút"))),
                    new Entry(4002, "Kiểm tra cuối chặng Reading B1", "Đánh giá mục tiêu của chặng Reading B1.", "Reading", "B1", "Kiểm tra cuối chặng", "DRAFT", "Bản nháp", "15/10/2026", List.of(new Field("Thời gian", "35 phút"), new Field("Mục tiêu", "B1")), List.of(new Row("Đọc hiểu đoạn ngắn", "5 câu hỏi", "Phần 1"), new Row("Sắp xếp và liên kết ý", "5 câu hỏi", "Phần 2"))),
                    new Entry(4003, "Đánh giá đầu vào Listening", "Xác định trình độ Listening ban đầu.", "Listening", "A2", "Kiểm tra đầu vào", "PUBLISHED", "Đã công bố", "14/10/2026", List.of(new Field("Thời gian", "20 phút")), List.of(new Row("Hội thoại ngắn", "10 câu hỏi", "2 lượt nghe"))))),
            "results", new Module("results", "Kết quả", "Theo dõi lượt thi, điểm từng kỹ năng và trạng thái chấm.", "kết quả", "", "Loại đề", List.of("Kiểm tra đầu vào", "Luyện tập", "Kiểm tra cuối chặng", "Thi thử tổng hợp"), List.of(
                    new Entry(5001, "Nguyễn Minh Anh", "Thi thử tổng hợp số 01 · minhanh@example.com", "Tổng hợp", "B1", "Thi thử tổng hợp", "COMPLETED", "Đã hoàn thành", "16/10/2026", List.of(new Field("Điểm ước lượng", "132 / 200"), new Field("Thời gian làm", "148 phút"), new Field("Ngày nộp", "16/10/2026")), List.of(new Row("Grammar & Vocabulary", "Tham chiếu ngữ pháp và từ vựng", "36 / 50"), new Row("Reading", "B1", "34 / 50"), new Row("Listening", "B1", "32 / 50"), new Row("Writing", "B1 · Ước lượng bằng AI", "33 / 50"), new Row("Speaking", "B1 · Ước lượng nội dung bản nhận diện", "33 / 50"))),
                    new Entry(5002, "Trần Hoàng Nam", "Kiểm tra đầu vào Writing · hoangnam@example.com", "Writing", "Chưa đánh giá", "Kiểm tra đầu vào", "GRADING", "Chờ chấm", "15/10/2026", List.of(new Field("Điểm", "Chưa có điểm"), new Field("Ngày nộp", "15/10/2026")), List.of(new Row("Writing", "Đang chờ kết quả", "Chưa có điểm"))),
                    new Entry(5003, "Lê Thu Hà", "Luyện tập Reading · thuha@example.com", "Reading", "Chưa đánh giá", "Luyện tập", "GRADING_FAILED", "Chấm chưa thành công", "14/10/2026", List.of(new Field("Điểm", "Chưa có điểm"), new Field("Bài làm", "Đã lưu")), List.of(new Row("Reading", "Bài làm được giữ nguyên", "Chưa có điểm")))))
    );

    public Module module(String key) {
        Module module = modules.get(key);
        if (module == null) throw new ResourceNotFoundException("Không tìm thấy nội dung");
        return module;
    }

    public Entry entry(String key, int id) {
        return module(key).entries().stream().filter(e -> e.id() == id).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nội dung"));
    }

    public List<Entry> find(String key, String q, String skill, String status, String kind) {
        String term = normalize(q);
        return module(key).entries().stream()
                .filter(e -> normalize(e.title() + " " + e.subtitle()).contains(term))
                .filter(e -> skill.isBlank() || e.skill().equals(skill))
                .filter(e -> status.isBlank() || e.status().equals(status))
                .filter(e -> kind.isBlank() || e.kind().equals(kind)).toList();
    }

    public CatalogPage findPage(String key, String q, String skill, String status, String kind, int page, int size) {
        List<Entry> all = find(key, q, skill, status, kind);
        int safeSize = Math.min(50, Math.max(1, size));
        int totalItems = all.size();
        int totalPages = totalItems == 0 ? 0 : (totalItems + safeSize - 1) / safeSize;
        int safePage = totalPages == 0 ? 0 : Math.min(Math.max(0, page), totalPages - 1);
        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, totalItems);
        return new CatalogPage(all.subList(from, to), safePage, safeSize, totalItems, totalPages);
    }
    private String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd');
    }
}
