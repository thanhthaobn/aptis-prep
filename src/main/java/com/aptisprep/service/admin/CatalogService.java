package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.form.QuestionForm;
import com.aptisprep.dto.view.AdminCatalogPreview;
import com.aptisprep.dto.view.AdminCatalogPreview.Entry;
import com.aptisprep.dto.view.AdminCatalogPreview.Field;
import com.aptisprep.dto.view.AdminCatalogPreview.Module;
import com.aptisprep.dto.view.AdminCatalogPreview.Row;
import com.aptisprep.entity.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.storage.StorageService;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogService {
    private static final List<String> SKILLS = List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    private static final List<String> LEVELS = List.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final List<String> TYPES = List.of("SINGLE_CHOICE", "MATCHING", "ORDERING", "WRITING_TEXT", "SPEAKING_AUDIO");
    private static final List<String> EXAM_TYPES = List.of("PLACEMENT", "PRACTICE", "CHECKPOINT", "FULL_MOCK");
    private final QuestionRepository questions;
    private final QuestionItemRepository items;
    private final QuestionOptionRepository options;
    private final QuestionMediaRepository media;
    private final MockExamRepository exams;
    private final MockExamSectionRepository sections;
    private final MockExamQuestionRepository examQuestions;
    private final CertificateRepository certificates;
    private final ScoringSchemeRepository schemes;
    private final StorageService storage;

    public CatalogService(QuestionRepository questions, QuestionItemRepository items, QuestionOptionRepository options,
                          QuestionMediaRepository media, MockExamRepository exams, MockExamSectionRepository sections,
                          MockExamQuestionRepository examQuestions, CertificateRepository certificates,
                          ScoringSchemeRepository schemes, StorageService storage) {
        this.questions = questions; this.items = items; this.options = options; this.media = media;
        this.exams = exams; this.sections = sections; this.examQuestions = examQuestions;
        this.certificates = certificates; this.schemes = schemes; this.storage = storage;
    }

    public Module module(String key) {
        if ("questions".equals(key)) return new Module(key, "Ngân hàng câu hỏi", "Quản lý nội dung, phương án trả lời và tài nguyên đính kèm.", "câu hỏi", "Thêm câu hỏi", "Dạng câu hỏi", List.of("SINGLE_CHOICE", "MATCHING", "ORDERING", "WRITING_TEXT", "SPEAKING_AUDIO"), List.of());
        if ("exams".equals(key)) return new Module(key, "Đề thi", "Sắp xếp câu hỏi, thời gian và phạm vi kỹ năng của từng đề.", "đề thi", "Tạo đề thi", "Loại đề", EXAM_TYPES, List.of());
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public List<Entry> find(String key, String q, String skill, String status, String kind) {
        String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if ("questions".equals(key)) return questions.findAll().stream().filter(x -> matches(term, x.getContent(), x.getInstruction()))
                .filter(x -> skill.isBlank() || x.getSkill().equals(skill)).filter(x -> status.isBlank() || x.getStatus().equals(status))
                .filter(x -> kind.isBlank() || x.getQuestionType().equals(kind)).sorted(Comparator.comparing(QuestionEntity::getUpdatedAt).reversed()).map(this::questionEntry).toList();
        if ("exams".equals(key)) return exams.findAll().stream().filter(x -> matches(term, x.getTitle(), x.getDescription()))
                .filter(x -> skill.isBlank() || Objects.equals(x.getSkill(), skill)).filter(x -> status.isBlank() || x.getStatus().equals(status))
                .filter(x -> kind.isBlank() || x.getExamType().equals(kind)).sorted(Comparator.comparing(MockExamEntity::getUpdatedAt).reversed()).map(this::examEntry).toList();
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public Entry entry(String key, int id) {
        if ("questions".equals(key)) return questionEntry(questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi")));
        if ("exams".equals(key)) return examEntry(exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi")));
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public QuestionForm questionForm(Integer id) {
        QuestionForm form = new QuestionForm();
        if (id == null) return form;
        QuestionEntity q = questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        form.setContent(q.getContent()); form.setInstruction(q.getInstruction()); form.setQuestionType(q.getQuestionType()); form.setSkill(q.getSkill());
        form.setPartNo(q.getPartNo()); form.setCefrLevel(q.getCefrLevel()); form.setDifficulty(q.getDifficulty()); form.setAllowOptionReuse(q.isAllowOptionReuse());
        form.setPreparationSeconds(q.getPreparationSeconds()); form.setStatus(q.getStatus());
        QuestionItemEntity item = items.findByQuestionIdOrderByPosition(id).stream().findFirst().orElse(null);
        if (item != null) {
            form.setItemContent(item.getContent()); form.setCorrectPosition(item.getCorrectPosition()); form.setMinWordCount(item.getMinWordCount());
            form.setMaxWordCount(item.getMaxWordCount()); form.setResponseSeconds(item.getResponseSeconds());
            List<QuestionForm.OptionForm> os = options.findByQuestionItemIdOrderByPosition(item.getId()).stream().map(o -> {
                QuestionForm.OptionForm f = new QuestionForm.OptionForm(o.getOptionCode()); f.setContent(o.getContent()); f.setCorrect(Boolean.TRUE.equals(o.getCorrect())); return f;
            }).collect(Collectors.toCollection(ArrayList::new));
            if (!os.isEmpty()) form.setOptions(os);
        }
        return form;
    }

    @Transactional
    public Integer saveQuestion(Integer id, QuestionForm form, MultipartFile upload) {
        validateQuestion(form);
        QuestionEntity q = id == null ? new QuestionEntity() : questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if ("PUBLISHED".equals(q.getStatus()) && !"PUBLISHED".equals(form.getStatus())) throw new BusinessRuleException("Câu hỏi đã công bố không thể quay về bản nháp");
        q.setContent(form.getContent().trim()); q.setInstruction(blankToNull(form.getInstruction())); q.setQuestionType(form.getQuestionType()); q.setSkill(form.getSkill());
        q.setPartNo(form.getPartNo()); q.setCefrLevel(form.getCefrLevel()); q.setDifficulty(form.getDifficulty()); q.setAllowOptionReuse(form.isAllowOptionReuse());
        q.setPreparationSeconds(form.getPreparationSeconds()); q.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : form.getStatus());
        q = questions.save(q);
        if (id != null) {
            for (QuestionItemEntity old : items.findByQuestionIdOrderByPosition(id)) { options.deleteByQuestionItemId(old.getId()); }
            items.deleteByQuestionId(id);
        }
        QuestionItemEntity item = new QuestionItemEntity(); item.setQuestionId(q.getId()); item.setContent(form.getItemContent().trim()); item.setPosition(1);
        item.setCorrectPosition(form.getCorrectPosition()); item.setMinWordCount(form.getMinWordCount()); item.setMaxWordCount(form.getMaxWordCount()); item.setResponseSeconds(form.getResponseSeconds());
        item = items.save(item);
        int position = 1;
        if (usesOptions(form.getQuestionType())) for (QuestionForm.OptionForm option : form.getOptions()) if (option.getContent() != null && !option.getContent().isBlank()) {
            QuestionOptionEntity entity = new QuestionOptionEntity(); entity.setQuestionItemId(item.getId()); entity.setContent(option.getContent().trim()); entity.setOptionCode(option.getCode()); entity.setCorrect(Objects.equals(position, form.getCorrectPosition())); entity.setPosition(position++); options.save(entity);
        }
        if (upload != null && !upload.isEmpty()) saveMedia(q, upload);
        return q.getId();
    }

    @Transactional
    public void publishQuestion(Integer id) {
        QuestionEntity q = questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionForm form = questionForm(id); validateQuestion(form); q.setStatus("PUBLISHED"); questions.save(q);
    }

    public ExamForm examForm(Integer id) {
        ExamForm form = new ExamForm();
        certificates.findByStatusOrderByName("ACTIVE").stream().findFirst().ifPresent(x -> form.setCertificateId(x.getId()));
        schemes.findByStatusOrderByName("ACTIVE").stream().findFirst().ifPresent(x -> form.setScoringSchemeId(x.getId()));
        if (id == null) return form;
        MockExamEntity exam = exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        form.setTitle(exam.getTitle()); form.setDescription(exam.getDescription()); form.setExamType(exam.getExamType()); form.setTargetLevel(exam.getTargetLevel());
        form.setSkill(exam.getSkill()); form.setSectionSkill(exam.getSkill()); form.setDurationMinutes(exam.getDurationMinutes()); form.setCertificateId(exam.getCertificateId());
        form.setScoringSchemeId(exam.getScoringSchemeId()); form.setStatus(exam.getStatus());
        form.setQuestionIds(examQuestions.findByMockExamIdOrderByPosition(id).stream().map(x -> "" + getQuestionId(x)).collect(Collectors.joining(",")));
        sections.findByMockExamIdOrderById(id).stream().findFirst().ifPresent(x -> { form.setSectionSkill(x.getSkill()); });
        return form;
    }

    @Transactional
    public Integer saveExam(Integer id, ExamForm form) {
        validateExamBasics(form);
        MockExamEntity exam = id == null ? new MockExamEntity() : exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        if ("PUBLISHED".equals(exam.getStatus()) && !"PUBLISHED".equals(form.getStatus())) throw new BusinessRuleException("Đề đã công bố không thể quay về bản nháp");
        exam.setTitle(form.getTitle().trim()); exam.setDescription(blankToNull(form.getDescription())); exam.setExamType(form.getExamType()); exam.setTargetLevel(form.getTargetLevel());
        exam.setSkill(form.getSectionSkill()); exam.setDurationMinutes(form.getDurationMinutes()); exam.setCertificateId(form.getCertificateId()); exam.setScoringSchemeId(form.getScoringSchemeId()); exam.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : form.getStatus());
        exam = exams.save(exam);
        if (id != null) { examQuestions.deleteByMockExamId(id); sections.deleteByMockExamId(id); }
        List<Integer> ids = parseIds(form.getQuestionIds());
        validateQuestionSelection(ids, form.getSectionSkill());
        MockExamSectionEntity section = null;
        if (!ids.isEmpty()) { section = new MockExamSectionEntity(); section.setMockExamId(exam.getId()); section.setSkill(form.getSectionSkill()); section.setDisplayOrder(1); section.setDurationMinutes(form.getSectionDurationMinutes()); section.setStatus("ACTIVE"); section = sections.save(section); }
        int pos = 1; for (Integer questionId : ids) { MockExamQuestionEntity link = new MockExamQuestionEntity(); link.setMockExamId(exam.getId()); link.setMockExamSectionId(section == null ? null : section.getId()); link.setQuestionId(questionId); link.setPosition(pos++); examQuestions.save(link); }
        return exam.getId();
    }

    @Transactional
    public void publishExam(Integer id) {
        MockExamEntity exam = exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        ExamForm form = examForm(id); validateExamBasics(form); List<Integer> ids = parseIds(form.getQuestionIds());
        if (ids.isEmpty()) throw new BusinessRuleException("Không thể công bố đề trống");
        validateQuestionSelection(ids, form.getSectionSkill()); exam.setStatus("PUBLISHED"); exams.save(exam);
    }

    public List<CertificateEntity> activeCertificates() { return certificates.findByStatusOrderByName("ACTIVE"); }
    public List<ScoringSchemeEntity> activeSchemes() { return schemes.findByStatusOrderByName("ACTIVE"); }
    public List<QuestionEntity> publishedQuestions() { return questions.findByStatusOrderByUpdatedAtDesc("PUBLISHED"); }
    public List<String> skills() { return SKILLS; }
    public List<String> levels() { return LEVELS; }
    public List<String> questionTypes() { return TYPES; }
    public List<String> examTypes() { return EXAM_TYPES; }
    public boolean hasData(String key) { return "questions".equals(key) ? questions.count() > 0 : "exams".equals(key) && exams.count() > 0; }

    private void validateQuestion(QuestionForm f) {
        if (f.getContent() == null || f.getContent().isBlank() || f.getContent().length() > 10000) throw new BusinessRuleException("Nội dung câu hỏi là bắt buộc");
        if (!SKILLS.contains(f.getSkill()) || !LEVELS.contains(f.getCefrLevel()) || !TYPES.contains(f.getQuestionType())) throw new BusinessRuleException("Kỹ năng, cấp độ hoặc dạng câu hỏi không hợp lệ");
        if (f.getItemContent() == null || f.getItemContent().isBlank()) throw new BusinessRuleException("Câu hỏi phải có ít nhất một ý hỏi");
        if (usesOptions(f.getQuestionType())) {
            long count = f.getOptions() == null ? 0 : f.getOptions().stream().filter(x -> x.getContent() != null && !x.getContent().isBlank()).count();
            long correct = f.getCorrectPosition() != null && f.getCorrectPosition() >= 1 && f.getCorrectPosition() <= count ? 1 : 0;
            if (count < 2 || correct != 1) throw new BusinessRuleException("Dạng chọn đáp án phải có ít nhất 2 phương án và đúng 1 đáp án đúng");
        }
        if (f.getMinWordCount() != null && f.getMaxWordCount() != null && f.getMinWordCount() > f.getMaxWordCount()) throw new BusinessRuleException("Số từ tối thiểu không được lớn hơn tối đa");
    }
    private void validateExamBasics(ExamForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank() || f.getTitle().length() > 255) throw new BusinessRuleException("Tên đề thi là bắt buộc");
        if (!EXAM_TYPES.contains(f.getExamType()) || !SKILLS.contains(f.getSectionSkill()) || !LEVELS.contains(f.getTargetLevel())) throw new BusinessRuleException("Loại đề, kỹ năng hoặc cấp độ không hợp lệ");
        if (f.getDurationMinutes() == null || f.getDurationMinutes() <= 0 || f.getSectionDurationMinutes() == null || f.getSectionDurationMinutes() <= 0) throw new BusinessRuleException("Thời lượng phải lớn hơn 0");
        if (f.getCertificateId() == null || !certificates.existsById(f.getCertificateId())) throw new BusinessRuleException("Chứng chỉ không hợp lệ");
        if (f.getScoringSchemeId() == null || !schemes.existsById(f.getScoringSchemeId())) throw new BusinessRuleException("Bộ chấm điểm không hợp lệ");
    }
    private void validateQuestionSelection(List<Integer> ids, String skill) {
        Set<Integer> unique = new HashSet<>(ids); if (unique.size() != ids.size()) throw new BusinessRuleException("Một câu hỏi không được chọn lặp trong cùng đề");
        for (Integer id : ids) { QuestionEntity q = questions.findById(id).orElseThrow(() -> new BusinessRuleException("Câu hỏi không tồn tại: " + id)); if (!"PUBLISHED".equals(q.getStatus())) throw new BusinessRuleException("Chỉ được đưa câu hỏi đã công bố vào đề"); if (!q.getSkill().equals(skill)) throw new BusinessRuleException("Kỹ năng section không khớp với câu hỏi"); }
    }
    private void saveMedia(QuestionEntity q, MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!(contentType.startsWith("image/") || contentType.startsWith("audio/"))) throw new BusinessRuleException("Media câu hỏi chỉ nhận ảnh hoặc âm thanh");
        QuestionMediaEntity m = new QuestionMediaEntity(); m.setQuestionId(q.getId()); m.setMediaType(contentType.startsWith("audio/") ? "AUDIO" : "IMAGE"); m.setMediaUrl(storage.store(file, "questions/"));
        m.setOriginalFilename(file.getOriginalFilename()); m.setContentType(contentType); m.setFileSize(file.getSize()); m.setPosition(media.findByQuestionIdOrderByPosition(q.getId()).size() + 1); media.save(m);
    }
    private boolean usesOptions(String type) { return "SINGLE_CHOICE".equals(type) || "MATCHING".equals(type) || "ORDERING".equals(type); }
    private boolean matches(String term, String... values) { return term.isBlank() || Arrays.stream(values).filter(Objects::nonNull).map(x -> x.toLowerCase(Locale.ROOT)).anyMatch(x -> x.contains(term)); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<Integer> parseIds(String raw) { if (raw == null || raw.isBlank()) return List.of(); try { return Arrays.stream(raw.split(",")).map(String::trim).filter(x -> !x.isBlank()).map(Integer::valueOf).toList(); } catch (NumberFormatException e) { throw new BusinessRuleException("Danh sách câu hỏi không hợp lệ"); } }
    private Integer getQuestionId(MockExamQuestionEntity e) { return e.getQuestionId(); }
    private Entry questionEntry(QuestionEntity q) {
        List<Row> rows = items.findByQuestionIdOrderByPosition(q.getId()).stream().flatMap(i -> options.findByQuestionItemIdOrderByPosition(i.getId()).stream().map(o -> new Row(o.getOptionCode(), o.getContent(), Boolean.TRUE.equals(o.getCorrect()) ? "Đáp án đúng" : ""))).toList();
        return new Entry(q.getId(), q.getContent(), q.getContent(), q.getSkill(), q.getCefrLevel(), q.getQuestionType(), q.getStatus(), statusLabel(q.getStatus()), q.getUpdatedAt().toLocalDate().toString(), List.of(new Field("Phần thi", String.valueOf(q.getPartNo())), new Field("Độ khó", String.valueOf(q.getDifficulty()))), rows);
    }
    private Entry examEntry(MockExamEntity e) { List<Row> rows = sections.findByMockExamIdOrderById(e.getId()).stream().map(s -> new Row(s.getSkill(), "Các câu hỏi đã chọn", "Section")).toList(); return new Entry(e.getId(), e.getTitle(), e.getDescription(), e.getSkill() == null ? "" : e.getSkill(), e.getTargetLevel(), e.getExamType(), e.getStatus(), statusLabel(e.getStatus()), e.getUpdatedAt().toLocalDate().toString(), List.of(new Field("Thời lượng", e.getDurationMinutes() + " phút"), new Field("Bộ chấm điểm", String.valueOf(e.getScoringSchemeId()))), rows); }
    private String statusLabel(String status) { return switch (status) { case "PUBLISHED" -> "Đã công bố"; case "ARCHIVED" -> "Đã lưu trữ"; default -> "Bản nháp"; }; }
}
