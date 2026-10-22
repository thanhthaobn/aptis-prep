package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.form.QuestionForm;
import com.aptisprep.dto.view.AdminCatalogPreview;
import com.aptisprep.dto.view.AdminCatalogPreview.Entry;
import com.aptisprep.dto.view.AdminCatalogPreview.Field;
import com.aptisprep.dto.view.AdminCatalogPreview.Module;
import com.aptisprep.dto.view.AdminCatalogPreview.Row;
import com.aptisprep.entity.*;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.storage.StorageService;
import com.aptisprep.service.contract.CertificateSchemeContract;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.IOException;
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
    private final com.aptisprep.repository.ExamAttemptRepository attempts;
    private final com.aptisprep.repository.UserAnswerRepository answers;
    private final StorageService storage;
    private final CertificateSchemeContract certificateSchemes;

    public CatalogService(QuestionRepository questions, QuestionItemRepository items, QuestionOptionRepository options,
                          QuestionMediaRepository media, MockExamRepository exams, MockExamSectionRepository sections,
                          MockExamQuestionRepository examQuestions, CertificateRepository certificates,
                          ScoringSchemeRepository schemes, com.aptisprep.repository.ExamAttemptRepository attempts,
                          com.aptisprep.repository.UserAnswerRepository answers, StorageService storage,
                          CertificateSchemeContract certificateSchemes) {
        this.questions = questions; this.items = items; this.options = options; this.media = media;
        this.exams = exams; this.sections = sections; this.examQuestions = examQuestions;
        this.certificates = certificates; this.schemes = schemes; this.attempts = attempts; this.answers = answers;
        this.storage = storage; this.certificateSchemes = certificateSchemes;
    }

    public Module module(String key) {
        if ("questions".equals(key)) return new Module(key, "Ngân hàng câu hỏi", "Quản lý nội dung, phương án trả lời và tài nguyên đính kèm.", "câu hỏi", "Thêm câu hỏi", "Dạng câu hỏi", List.of("SINGLE_CHOICE", "MATCHING", "ORDERING", "WRITING_TEXT", "SPEAKING_AUDIO"), List.of());
        if ("exams".equals(key)) return new Module(key, "Đề thi", "Sắp xếp câu hỏi, thời gian và phạm vi kỹ năng của từng đề.", "đề thi", "Tạo đề thi", "Loại đề", EXAM_TYPES, List.of());
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public List<Entry> find(String key, String q, String skill, String status, String kind) {
        String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if ("questions".equals(key)) return questions.findAll().stream().filter(x -> matches(term, x.getContent(), x.getInstruction(), x.getReadingPassage()))
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
        form.setContent(q.getContent()); form.setInstruction(q.getInstruction()); form.setReadingPassage(q.getReadingPassage()); form.setQuestionType(q.getQuestionType()); form.setSkill(q.getSkill());
        form.setPartNo(q.getPartNo()); form.setCefrLevel(q.getCefrLevel()); form.setDifficulty(q.getDifficulty()); form.setAllowOptionReuse(q.isAllowOptionReuse());
        form.setPreparationSeconds(q.getPreparationSeconds()); form.setStatus(q.getStatus());
        List<QuestionForm.ItemForm> values = new ArrayList<>();
        for (QuestionItemEntity item : items.findByQuestionIdOrderByPosition(id)) {
            QuestionForm.ItemForm itemForm = new QuestionForm.ItemForm();
            itemForm.setContent(item.getContent()); itemForm.setCorrectPosition(item.getCorrectPosition()); itemForm.setMinWordCount(item.getMinWordCount());
            itemForm.setMaxWordCount(item.getMaxWordCount()); itemForm.setResponseSeconds(item.getResponseSeconds());
            List<QuestionForm.OptionForm> os = options.findByQuestionItemIdOrderByPosition(item.getId()).stream().map(o -> {
                QuestionForm.OptionForm f = new QuestionForm.OptionForm(o.getOptionCode()); f.setContent(o.getContent()); f.setCorrect(Boolean.TRUE.equals(o.getCorrect())); return f;
            }).collect(Collectors.toCollection(ArrayList::new));
            if (!os.isEmpty()) itemForm.setOptions(os);
            values.add(itemForm);
        }
        if (!values.isEmpty()) form.setItems(values);
        return form;
    }

    @Transactional
    public Integer saveQuestion(Integer id, QuestionForm form, MultipartFile upload) {
        normalizeItems(form);
        validateQuestion(form);
        QuestionEntity q = id == null ? new QuestionEntity() : questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (id != null && answers.existsByQuestionId(id)) {
            throw new BusinessRuleException("Câu hỏi đã có trong lịch sử làm bài; hãy lưu trữ và tạo phiên bản câu hỏi mới");
        }
        if ("PUBLISHED".equals(q.getStatus()) && !"PUBLISHED".equals(form.getStatus())) throw new BusinessRuleException("Câu hỏi đã công bố không thể quay về bản nháp");
        q.setContent(form.getContent().trim()); q.setInstruction(blankToNull(form.getInstruction())); q.setReadingPassage("READING".equals(form.getSkill()) ? blankToNull(form.getReadingPassage()) : null); q.setQuestionType(form.getQuestionType()); q.setSkill(form.getSkill());
        q.setPartNo(form.getPartNo()); q.setCefrLevel(form.getCefrLevel()); q.setDifficulty(form.getDifficulty()); q.setAllowOptionReuse(form.isAllowOptionReuse());
        q.setPreparationSeconds(form.getPreparationSeconds()); q.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : form.getStatus());
        q = questions.save(q);
        if (id != null) {
            for (QuestionItemEntity old : items.findByQuestionIdOrderByPosition(id)) { options.deleteByQuestionItemId(old.getId()); }
            items.deleteByQuestionId(id);
        }
        int itemPosition = 1;
        for (QuestionForm.ItemForm value : form.getItems()) {
            QuestionItemEntity item = new QuestionItemEntity(); item.setQuestionId(q.getId()); item.setContent(value.getContent().trim()); item.setPosition(itemPosition++);
            item.setCorrectPosition(value.getCorrectPosition()); item.setMinWordCount(value.getMinWordCount()); item.setMaxWordCount(value.getMaxWordCount()); item.setResponseSeconds(value.getResponseSeconds());
            item = items.save(item);
            int optionPosition = 1;
            if (usesOptions(form.getQuestionType())) for (QuestionForm.OptionForm option : value.getOptions()) if (option.getContent() != null && !option.getContent().isBlank()) {
                QuestionOptionEntity entity = new QuestionOptionEntity(); entity.setQuestionItemId(item.getId()); entity.setContent(option.getContent().trim()); entity.setOptionCode(option.getCode()); entity.setCorrect(Objects.equals(optionPosition, value.getCorrectPosition())); entity.setPosition(optionPosition++); options.save(entity);
            }
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
        certificates.findByStatusOrderByName(RecordStatus.ACTIVE).stream().findFirst().ifPresent(x -> form.setCertificateId(x.getId()));
        if (form.getCertificateId() != null) schemes.findByCertificateIdAndStatusOrderByVersionDesc(form.getCertificateId(), RecordStatus.ACTIVE).stream().findFirst().ifPresent(x -> form.setScoringSchemeId(x.getId()));
        if (id == null) {
            ExamForm.SectionForm section = new ExamForm.SectionForm(); section.setSkill(form.getSectionSkill());
            section.setDurationMinutes(form.getSectionDurationMinutes()); section.setQuestionIds("");
            form.setSections(new ArrayList<>(List.of(section))); return form;
        }
        MockExamEntity exam = exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        form.setTitle(exam.getTitle()); form.setDescription(exam.getDescription()); form.setExamType(exam.getExamType()); form.setTargetLevel(exam.getTargetLevel());
        form.setSkill(exam.getSkill()); form.setSectionSkill(exam.getSkill()); form.setDurationMinutes(exam.getDurationMinutes()); form.setCertificateId(exam.getCertificateId());
        form.setScoringSchemeId(exam.getScoringSchemeId()); form.setStatus(exam.getStatus());
        form.setQuestionIds(examQuestions.findByMockExamIdOrderByPosition(id).stream().map(x -> "" + getQuestionId(x)).collect(Collectors.joining(",")));
        List<ExamForm.SectionForm> sectionForms = new ArrayList<>();
        for (MockExamSectionEntity section : sections.findByMockExamIdOrderById(id)) {
            ExamForm.SectionForm sectionForm = new ExamForm.SectionForm();
            sectionForm.setSkill(section.getSkill()); sectionForm.setDurationMinutes(section.getDurationMinutes());
            sectionForm.setQuestionIds(examQuestions.findByMockExamIdOrderByPosition(id).stream()
                    .filter(link -> Objects.equals(link.getMockExamSectionId(), section.getId()))
                    .map(link -> String.valueOf(link.getQuestionId())).collect(Collectors.joining(",")));
            sectionForms.add(sectionForm);
        }
        if (!sectionForms.isEmpty()) {
            form.setSections(sectionForms);
            form.setSectionSkill(sectionForms.get(0).getSkill());
            form.setSectionDurationMinutes(sectionForms.get(0).getDurationMinutes());
        }
        return form;
    }

    @Transactional
    public Integer saveExam(Integer id, ExamForm form) {
        normalizeSections(form);
        validateExamBasics(form);
        MockExamEntity exam = id == null ? new MockExamEntity() : exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        if (id != null && attempts.existsByMockExamId(id)) {
            throw new BusinessRuleException("Đề thi đã có lịch sử làm bài; hãy tạo phiên bản đề mới");
        }
        if ("PUBLISHED".equals(exam.getStatus()) && !"PUBLISHED".equals(form.getStatus())) throw new BusinessRuleException("Đề đã công bố không thể quay về bản nháp");
        exam.setTitle(form.getTitle().trim()); exam.setDescription(blankToNull(form.getDescription())); exam.setExamType(form.getExamType()); exam.setTargetLevel(form.getTargetLevel());
        exam.setSkill("FULL_MOCK".equals(form.getExamType()) ? "FULL_MOCK" : form.getSections().get(0).getSkill());
        exam.setDurationMinutes(form.getDurationMinutes()); exam.setCertificateId(form.getCertificateId()); exam.setScoringSchemeId(form.getScoringSchemeId()); exam.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : form.getStatus());
        exam = exams.save(exam);
        if (id != null) { examQuestions.deleteByMockExamId(id); sections.deleteByMockExamId(id); }
        int pos = 1; int sectionOrder = 1;
        Set<Integer> allQuestionIds = new HashSet<>();
        for (ExamForm.SectionForm sectionForm : form.getSections()) {
            List<Integer> ids = parseIds(sectionForm.getQuestionIds());
            validateQuestionSelection(ids, sectionForm.getSkill());
            if (!allQuestionIds.addAll(ids)) throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
            MockExamSectionEntity section = new MockExamSectionEntity(); section.setMockExamId(exam.getId()); section.setSkill(sectionForm.getSkill());
            section.setDisplayOrder(sectionOrder++); section.setDurationMinutes(sectionForm.getDurationMinutes()); section.setStatus("ACTIVE"); section = sections.save(section);
            for (Integer questionId : ids) { MockExamQuestionEntity link = new MockExamQuestionEntity(); link.setMockExamId(exam.getId()); link.setMockExamSectionId(section.getId()); link.setQuestionId(questionId); link.setPosition(pos++); examQuestions.save(link); }
        }
        return exam.getId();
    }

    @Transactional
    public void publishExam(Integer id) {
        MockExamEntity exam = exams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        ExamForm form = examForm(id); normalizeSections(form); validateExamBasics(form);
        Set<Integer> all = new HashSet<>();
        for (ExamForm.SectionForm section : form.getSections()) {
            List<Integer> ids = parseIds(section.getQuestionIds());
            if (ids.isEmpty()) throw new BusinessRuleException("Không thể công bố section trống");
            validateQuestionSelection(ids, section.getSkill());
            if (!all.addAll(ids)) throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
        }
        if (all.isEmpty()) throw new BusinessRuleException("Không thể công bố đề trống");
        exam.setStatus("PUBLISHED"); exams.save(exam);
    }

    @Transactional
    public void addMedia(Integer questionId, MultipartFile[] uploads) {
        QuestionEntity question = questions.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        assertQuestionNotAnswered(questionId);
        if (uploads == null || uploads.length == 0) throw new BusinessRuleException("Chưa chọn media");
        for (MultipartFile upload : uploads) if (upload != null && !upload.isEmpty()) saveMedia(question, upload);
    }

    @Transactional
    public void deleteMedia(Integer questionId, Integer mediaId) {
        assertQuestionNotAnswered(questionId);
        QuestionMediaEntity value = media.findById(mediaId).filter(item -> Objects.equals(item.getQuestionId(), questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media của câu hỏi"));
        // Do not remove the physical object here: active attempts keep a
        // snapshot of this media and must remain playable after an admin edits
        // the catalogue. A later retention job can delete unreferenced files.
        media.delete(value);
        List<QuestionMediaEntity> remaining = media.findByQuestionIdOrderByPosition(questionId);
        int position = 1; for (QuestionMediaEntity item : remaining) { item.setPosition(position++); media.save(item); }
    }

    @Transactional
    public void updateMedia(Integer questionId, Integer mediaId, String altText, String transcript, Integer maxPlayCount) {
        assertQuestionNotAnswered(questionId);
        QuestionMediaEntity value = media.findById(mediaId).filter(item -> Objects.equals(item.getQuestionId(), questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media của câu hỏi"));
        String cleanAlt = blankToNull(altText);
        if (cleanAlt != null && cleanAlt.length() > 500) throw new BusinessRuleException("Alt text tối đa 500 ký tự");
        String cleanTranscript = blankToNull(transcript);
        if (cleanTranscript != null && cleanTranscript.length() > 20000) throw new BusinessRuleException("Transcript tối đa 20.000 ký tự");
        if ("AUDIO".equals(value.getMediaType())) {
            if (maxPlayCount == null || maxPlayCount < 1 || maxPlayCount > 2) throw new BusinessRuleException("Audio chỉ được phép từ 1 đến 2 lượt nghe");
            value.setMaxPlayCount(maxPlayCount);
        } else {
            value.setMaxPlayCount(null);
        }
        value.setAltText(cleanAlt); value.setTranscript(cleanTranscript); media.save(value);
    }

    @Transactional
    public void moveMedia(Integer questionId, Integer mediaId, int direction) {
        assertQuestionNotAnswered(questionId);
        if (direction != -1 && direction != 1) throw new BusinessRuleException("Hướng sắp xếp không hợp lệ");
        List<QuestionMediaEntity> values = new ArrayList<>(media.findByQuestionIdOrderByPosition(questionId));
        int index = -1;
        for (int i = 0; i < values.size(); i++) if (Objects.equals(values.get(i).getId(), mediaId)) { index = i; break; }
        if (index < 0) throw new ResourceNotFoundException("Không tìm thấy media của câu hỏi");
        int target = index + direction;
        if (target < 0 || target >= values.size()) return;
        Collections.swap(values, index, target);
        for (int i = 0; i < values.size(); i++) { values.get(i).setPosition(i + 1); media.save(values.get(i)); }
    }

    @Transactional(readOnly = true)
    public MediaContent mediaContent(Integer questionId, Integer mediaId) {
        QuestionMediaEntity value = media.findById(mediaId).filter(item -> Objects.equals(item.getQuestionId(), questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media của câu hỏi"));
        return new MediaContent(storage.load(value.getMediaUrl()), value.getContentType());
    }

    @Transactional
    public void archiveQuestion(Integer id) {
        QuestionEntity q = questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        q.setStatus("ARCHIVED"); questions.save(q);
    }

    @Transactional
    public void restoreQuestion(Integer id) {
        QuestionEntity q = questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (!"ARCHIVED".equals(q.getStatus())) throw new BusinessRuleException("Câu hỏi chưa được lưu trữ");
        q.setStatus("DRAFT"); questions.save(q);
    }

    public record MediaContent(org.springframework.core.io.Resource resource, String contentType) {}

    public List<Certificate> activeCertificates() { return certificates.findByStatusOrderByName(RecordStatus.ACTIVE); }
    public List<ScoringScheme> activeSchemes() { return schemes.findByStatusOrderByName(RecordStatus.ACTIVE); }
    public List<QuestionEntity> publishedQuestions() { return questions.findByStatusOrderByUpdatedAtDesc("PUBLISHED"); }
    public List<QuestionMediaEntity> mediaItems(Integer questionId) { return media.findByQuestionIdOrderByPosition(questionId); }
    public List<String> skills() { return SKILLS; }
    public List<String> levels() { return LEVELS; }
    public List<String> questionTypes() { return TYPES; }
    public List<String> examTypes() { return EXAM_TYPES; }
    public boolean hasData(String key) { return "questions".equals(key) ? questions.count() > 0 : "exams".equals(key) && exams.count() > 0; }

    private void validateQuestion(QuestionForm f) {
        if (f.getContent() == null || f.getContent().isBlank() || f.getContent().length() > 10000) throw new BusinessRuleException("Nội dung câu hỏi là bắt buộc");
        if (f.getReadingPassage() != null && f.getReadingPassage().length() > 50000) throw new BusinessRuleException("Đoạn văn Reading tối đa 50.000 ký tự");
        if (!SKILLS.contains(f.getSkill()) || !LEVELS.contains(f.getCefrLevel()) || !TYPES.contains(f.getQuestionType())) throw new BusinessRuleException("Kỹ năng, cấp độ hoặc dạng câu hỏi không hợp lệ");
        if (f.getItems() == null || f.getItems().isEmpty() || f.getItems().stream().anyMatch(x -> x.getContent() == null || x.getContent().isBlank())) throw new BusinessRuleException("Câu hỏi phải có ít nhất một ý hỏi");
        if (("MATCHING".equals(f.getQuestionType()) || "ORDERING".equals(f.getQuestionType())) && f.getItems().size() < 2) throw new BusinessRuleException("Dạng ghép nối hoặc sắp xếp phải có ít nhất 2 ý hỏi");
        if (usesOptions(f.getQuestionType())) {
            for (QuestionForm.ItemForm item : f.getItems()) {
                long count = item.getOptions() == null ? 0 : item.getOptions().stream().filter(x -> x.getContent() != null && !x.getContent().isBlank()).count();
                boolean correct = item.getCorrectPosition() != null && item.getCorrectPosition() >= 1 && item.getCorrectPosition() <= count;
                if (count < 2 || !correct) throw new BusinessRuleException("Mỗi ý hỏi phải có ít nhất 2 phương án và đúng 1 đáp án đúng");
            }
        }
        if (f.getItems().stream().anyMatch(x -> x.getMinWordCount() != null && x.getMaxWordCount() != null && x.getMinWordCount() > x.getMaxWordCount())) throw new BusinessRuleException("Số từ tối thiểu không được lớn hơn tối đa");
    }
    private void validateExamBasics(ExamForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank() || f.getTitle().length() > 255) throw new BusinessRuleException("Tên đề thi là bắt buộc");
        if (!EXAM_TYPES.contains(f.getExamType()) || !LEVELS.contains(f.getTargetLevel())) throw new BusinessRuleException("Loại đề hoặc cấp độ không hợp lệ");
        if (f.getSections() == null || f.getSections().isEmpty()) throw new BusinessRuleException("Đề phải có ít nhất một section");
        if (f.getSections().stream().anyMatch(section -> !SKILLS.contains(section.getSkill()))) throw new BusinessRuleException("Kỹ năng section không hợp lệ");
        if ("FULL_MOCK".equals(f.getExamType()) && f.getSections().stream().map(ExamForm.SectionForm::getSkill).distinct().count() < 2)
            throw new BusinessRuleException("Full Mock phải có ít nhất hai kỹ năng");
        if (f.getDurationMinutes() == null || f.getDurationMinutes() <= 0 || f.getSectionDurationMinutes() == null || f.getSectionDurationMinutes() <= 0) throw new BusinessRuleException("Thời lượng phải lớn hơn 0");
        if (f.getSections().stream().anyMatch(section -> section.getDurationMinutes() == null || section.getDurationMinutes() <= 0)) throw new BusinessRuleException("Thời lượng section phải lớn hơn 0");
        if (f.getCertificateId() == null || !certificates.existsById(f.getCertificateId())) throw new BusinessRuleException("Chứng chỉ không hợp lệ");
        if (f.getScoringSchemeId() == null || !schemes.existsById(f.getScoringSchemeId())) throw new BusinessRuleException("Bộ chấm điểm không hợp lệ");
        for (ExamForm.SectionForm section : f.getSections()) certificateSchemes.requireAvailableScheme(f.getCertificateId(), f.getScoringSchemeId(), section.getSkill());
    }
    private void validateQuestionSelection(List<Integer> ids, String skill) {
        Set<Integer> unique = new HashSet<>(ids); if (unique.size() != ids.size()) throw new BusinessRuleException("Một câu hỏi không được chọn lặp trong cùng đề");
        for (Integer id : ids) { QuestionEntity q = questions.findById(id).orElseThrow(() -> new BusinessRuleException("Câu hỏi không tồn tại: " + id)); if (!"PUBLISHED".equals(q.getStatus())) throw new BusinessRuleException("Chỉ được đưa câu hỏi đã công bố vào đề"); if (!Objects.equals(q.getSkill(), skill)) throw new BusinessRuleException("Kỹ năng section không khớp với câu hỏi"); }
    }
    private void saveMedia(QuestionEntity q, MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!(contentType.startsWith("image/") || contentType.startsWith("audio/"))) throw new BusinessRuleException("Media câu hỏi chỉ nhận ảnh hoặc âm thanh");
        boolean audio = contentType.startsWith("audio/");
        long maxBytes = audio ? 25L * 1024 * 1024 : 8L * 1024 * 1024;
        if (file.getSize() <= 0 || file.getSize() > maxBytes) throw new BusinessRuleException("Media vượt quá dung lượng cho phép");
        try {
            byte[] header = file.getInputStream().readNBytes(16);
            if (audio ? !looksLikeAudio(header) : !looksLikeImage(header)) throw new BusinessRuleException("Nội dung file không khớp với định dạng media");
        } catch (IOException exception) {
            throw new BusinessRuleException("Không thể đọc file media");
        }
        QuestionMediaEntity m = new QuestionMediaEntity(); m.setQuestionId(q.getId()); m.setMediaType(audio ? "AUDIO" : "IMAGE"); m.setMediaUrl(storage.store(file, "questions/"));
        m.setOriginalFilename(cleanFilename(file.getOriginalFilename())); m.setContentType(contentType); m.setFileSize(file.getSize());
        m.setMaxPlayCount(audio ? 2 : null); m.setAltText(cleanFilename(file.getOriginalFilename()));
        m.setPosition(media.findByQuestionIdOrderByPosition(q.getId()).size() + 1); media.save(m);
    }

    private boolean looksLikeAudio(byte[] bytes) {
        return bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WAVE")
                || bytes.length >= 4 && ascii(bytes, 0, "OggS")
                || bytes.length >= 3 && ascii(bytes, 0, "ID3")
                || bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0
                || bytes.length >= 8 && ascii(bytes, 4, "ftyp");
    }

    private boolean looksLikeImage(byte[] bytes) {
        return bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                || bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF
                || bytes.length >= 6 && (ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a"))
                || bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++) if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        return true;
    }

    private String cleanFilename(String value) {
        if (value == null || value.isBlank()) return "media";
        String name = value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private void assertQuestionNotAnswered(Integer questionId) {
        if (answers.existsByQuestionId(questionId)) {
            throw new BusinessRuleException("Câu hỏi đã có trong lịch sử làm bài; hãy lưu trữ và tạo phiên bản câu hỏi mới");
        }
    }

    private void normalizeSections(ExamForm form) {
        if (form.getSections() != null && !form.getSections().isEmpty()) return;
        ExamForm.SectionForm section = new ExamForm.SectionForm(); section.setSkill(form.getSectionSkill());
        section.setDurationMinutes(form.getSectionDurationMinutes()); section.setQuestionIds(form.getQuestionIds());
        form.setSections(new ArrayList<>(List.of(section)));
    }
    private boolean usesOptions(String type) { return "SINGLE_CHOICE".equals(type) || "MATCHING".equals(type) || "ORDERING".equals(type); }
    private boolean matches(String term, String... values) { return term.isBlank() || Arrays.stream(values).filter(Objects::nonNull).map(x -> x.toLowerCase(Locale.ROOT)).anyMatch(x -> x.contains(term)); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<Integer> parseIds(String raw) { if (raw == null || raw.isBlank()) return List.of(); try { return Arrays.stream(raw.split(",")).map(String::trim).filter(x -> !x.isBlank()).map(Integer::valueOf).toList(); } catch (NumberFormatException e) { throw new BusinessRuleException("Danh sách câu hỏi không hợp lệ"); } }
    private Integer getQuestionId(MockExamQuestionEntity e) { return e.getQuestionId(); }
    private Entry questionEntry(QuestionEntity q) {
        List<Row> rows = new ArrayList<>();
        for (QuestionItemEntity item : items.findByQuestionIdOrderByPosition(q.getId())) {
            rows.add(new Row("Ý " + item.getPosition(), item.getContent(), item.getCorrectPosition() == null ? "" : "Đúng vị trí " + item.getCorrectPosition()));
            options.findByQuestionItemIdOrderByPosition(item.getId()).forEach(o -> rows.add(new Row(o.getOptionCode(), o.getContent(), Boolean.TRUE.equals(o.getCorrect()) ? "Đáp án đúng" : "")));
        }
        media.findByQuestionIdOrderByPosition(q.getId()).forEach(m -> rows.add(new Row("MEDIA #" + m.getId(), m.getOriginalFilename(), m.getMediaType() + " · " + m.getFileSize() + " bytes")));
        return new Entry(q.getId(), q.getContent(), q.getContent(), q.getSkill(), q.getCefrLevel(), q.getQuestionType(), q.getStatus(), statusLabel(q.getStatus()), q.getUpdatedAt() == null ? "" : q.getUpdatedAt().toLocalDate().toString(), List.of(new Field("Phần thi", String.valueOf(q.getPartNo())), new Field("Độ khó", String.valueOf(q.getDifficulty()))), rows);
    }

    private void normalizeItems(QuestionForm form) {
        if (form.getItems() == null || form.getItems().isEmpty()) form.setItems(List.of(new QuestionForm.ItemForm()));
        QuestionForm.ItemForm first = form.getItems().get(0);
        if ((first.getContent() == null || first.getContent().isBlank()) && form.getItemContent() != null) first.setContent(form.getItemContent());
        if (first.getCorrectPosition() == null) first.setCorrectPosition(form.getCorrectPosition());
        if (first.getMinWordCount() == null) first.setMinWordCount(form.getMinWordCount());
        if (first.getMaxWordCount() == null) first.setMaxWordCount(form.getMaxWordCount());
        if (first.getResponseSeconds() == null) first.setResponseSeconds(form.getResponseSeconds());
        if ((first.getOptions() == null || first.getOptions().isEmpty()) && form.getOptions() != null) first.setOptions(form.getOptions());
    }
    private Entry examEntry(MockExamEntity e) { List<Row> rows = sections.findByMockExamIdOrderById(e.getId()).stream().map(s -> new Row(s.getSkill(), "Các câu hỏi đã chọn", "Section")).toList(); return new Entry(e.getId(), e.getTitle(), e.getDescription(), e.getSkill() == null ? "" : e.getSkill(), e.getTargetLevel(), e.getExamType(), e.getStatus(), statusLabel(e.getStatus()), e.getUpdatedAt().toLocalDate().toString(), List.of(new Field("Thời lượng", e.getDurationMinutes() + " phút"), new Field("Bộ chấm điểm", String.valueOf(e.getScoringSchemeId()))), rows); }
    private String statusLabel(String status) { return switch (status) { case "PUBLISHED" -> "Đã công bố"; case "ARCHIVED" -> "Đã lưu trữ"; default -> "Bản nháp"; }; }
}
