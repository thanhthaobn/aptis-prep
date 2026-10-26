package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.form.QuestionForm;
import com.aptisprep.dto.view.AdminCatalogView.CatalogPage;
import com.aptisprep.dto.view.AdminCatalogView.Entry;
import com.aptisprep.dto.view.AdminCatalogView.Field;
import com.aptisprep.dto.view.AdminCatalogView.Module;
import com.aptisprep.dto.view.AdminCatalogView.Row;
import com.aptisprep.entity.*;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.QuestionType;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.storage.StorageService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogService {
    private static final List<String> SKILLS = Skill.codes();
    private static final List<String> LEVELS = CefrLevel.codes();
    private static final List<String> TYPES =
            Arrays.stream(QuestionType.values()).map(Enum::name).toList();
    private static final List<String> EXAM_TYPES = ExamDefinitionValidator.TYPES;
    private final QuestionRepository questions;
    private final QuestionItemRepository items;
    private final QuestionOptionRepository options;
    private final QuestionMediaRepository media;
    private final MockExamRepository exams;
    private final CertificateRepository certificates;
    private final ScoringSchemeRepository schemes;
    private final UserAnswerRepository answers;
    private final StorageService storage;
    private final AdminExamService examCatalog;

    public CatalogService(
            QuestionRepository questions,
            QuestionItemRepository items,
            QuestionOptionRepository options,
            QuestionMediaRepository media,
            MockExamRepository exams,
            CertificateRepository certificates,
            ScoringSchemeRepository schemes,
            UserAnswerRepository answers,
            StorageService storage,
            AdminExamService examCatalog) {
        this.questions = questions;
        this.items = items;
        this.options = options;
        this.media = media;
        this.exams = exams;
        this.certificates = certificates;
        this.schemes = schemes;
        this.answers = answers;
        this.storage = storage;
        this.examCatalog = examCatalog;
    }

    public Module module(String key) {
        if ("questions".equals(key))
            return new Module(
                    key,
                    "Ngân hàng câu hỏi",
                    "Quản lý nội dung, phương án trả lời và tài nguyên đính kèm.",
                    "câu hỏi",
                    "Thêm câu hỏi",
                    "Dạng câu hỏi",
                    List.of(
                            "SINGLE_CHOICE",
                            "MATCHING",
                            "ORDERING",
                            "WRITING_TEXT",
                            "SPEAKING_AUDIO"),
                    List.of());
        if ("exams".equals(key))
            return new Module(
                    key,
                    "Đề thi",
                    "Sắp xếp câu hỏi, thời gian và phạm vi kỹ năng của từng đề.",
                    "đề thi",
                    "Tạo đề thi",
                    "Loại đề",
                    EXAM_TYPES,
                    List.of());
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public List<Entry> find(String key, String q, String skill, String status, String kind) {
        String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if ("questions".equals(key))
            return questions.findAll().stream()
                    .filter(
                            x ->
                                    matches(
                                            term,
                                            x.getContent(),
                                            x.getInstruction(),
                                            x.getReadingPassage()))
                    .filter(x -> skill.isBlank() || x.getSkill().equals(skill))
                    .filter(x -> status.isBlank() || x.getStatus().equals(status))
                    .filter(x -> kind.isBlank() || x.getQuestionType().equals(kind))
                    .sorted(Comparator.comparing(QuestionEntity::getUpdatedAt).reversed())
                    .map(this::questionEntry)
                    .toList();
        if ("exams".equals(key))
            return exams.findAll().stream()
                    .filter(x -> matches(term, x.getTitle(), x.getDescription()))
                    .filter(x -> skill.isBlank() || Objects.equals(x.getSkill(), skill))
                    .filter(x -> status.isBlank() || x.getStatus().equals(status))
                    .filter(x -> kind.isBlank() || x.getExamType().equals(kind))
                    .sorted(Comparator.comparing(MockExamEntity::getUpdatedAt).reversed())
                    .map(examCatalog::entry)
                    .toList();
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public CatalogPage findPage(
            String key, String q, String skill, String status, String kind, int page, int size) {
        return paginate(find(key, q, skill, status, kind), page, size);
    }

    private CatalogPage paginate(List<Entry> all, int page, int size) {
        int safeSize = Math.min(50, Math.max(1, size));
        int totalItems = all.size();
        int totalPages = totalItems == 0 ? 0 : (totalItems + safeSize - 1) / safeSize;
        int safePage = totalPages == 0 ? 0 : Math.min(Math.max(0, page), totalPages - 1);
        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, totalItems);
        return new CatalogPage(all.subList(from, to), safePage, safeSize, totalItems, totalPages);
    }

    public Entry entry(String key, int id) {
        if ("questions".equals(key))
            return questionEntry(
                    questions
                            .findById(id)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy câu hỏi")));
        if ("exams".equals(key))
            return examCatalog.entry(
                    exams.findById(id)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy đề thi")));
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public QuestionForm questionForm(Integer id) {
        QuestionForm form = new QuestionForm();
        if (id == null) return form;
        QuestionEntity q =
                questions
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        form.setContent(q.getContent());
        form.setInstruction(q.getInstruction());
        form.setReadingPassage(q.getReadingPassage());
        form.setQuestionType(q.getQuestionType());
        form.setSkill(q.getSkill());
        form.setPartNo(q.getPartNo());
        form.setCefrLevel(q.getCefrLevel());
        form.setDifficulty(q.getDifficulty());
        form.setAllowOptionReuse(q.isAllowOptionReuse());
        form.setPreparationSeconds(q.getPreparationSeconds());
        form.setStatus(q.getStatus());
        List<QuestionForm.ItemForm> values = new ArrayList<>();
        for (QuestionItemEntity item : items.findByQuestionIdOrderByPosition(id)) {
            QuestionForm.ItemForm itemForm = new QuestionForm.ItemForm();
            itemForm.setContent(item.getContent());
            itemForm.setCorrectPosition(item.getCorrectPosition());
            itemForm.setMinWordCount(item.getMinWordCount());
            itemForm.setMaxWordCount(item.getMaxWordCount());
            itemForm.setResponseSeconds(item.getResponseSeconds());
            List<QuestionForm.OptionForm> os =
                    options.findByQuestionItemIdOrderByPosition(item.getId()).stream()
                            .map(
                                    o -> {
                                        QuestionForm.OptionForm f =
                                                new QuestionForm.OptionForm(o.getOptionCode());
                                        f.setContent(o.getContent());
                                        f.setCorrect(Boolean.TRUE.equals(o.getCorrect()));
                                        return f;
                                    })
                            .collect(Collectors.toCollection(ArrayList::new));
            if (!os.isEmpty()) itemForm.setOptions(os);
            values.add(itemForm);
        }
        if (!values.isEmpty()) form.setItems(values);
        return form;
    }

    @Transactional
    public Integer saveQuestion(Integer id, QuestionForm form, MultipartFile upload) {
        normalizeItems(form);
        normalizeQuestionFields(form);
        QuestionDefinitionValidator.validate(form);
        QuestionEntity q =
                id == null
                        ? new QuestionEntity()
                        : questions
                                .findById(id)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Không tìm thấy câu hỏi"));
        if (id != null && answers.existsByQuestionId(id)) {
            throw new BusinessRuleException(
                    "Câu hỏi đã có trong lịch sử làm bài. Hãy lưu trữ và tạo phiên bản câu hỏi"
                            + " mới");
        }
        if ("PUBLISHED".equals(q.getStatus()) && !"PUBLISHED".equals(form.getStatus()))
            throw new BusinessRuleException("Câu hỏi đã công bố không thể quay về bản nháp");
        q.setContent(form.getContent().trim());
        q.setInstruction(blankToNull(form.getInstruction()));
        q.setReadingPassage(
                "READING".equals(form.getSkill()) ? blankToNull(form.getReadingPassage()) : null);
        q.setQuestionType(form.getQuestionType());
        q.setSkill(form.getSkill());
        q.setPartNo(form.getPartNo());
        q.setCefrLevel(form.getCefrLevel());
        q.setDifficulty(form.getDifficulty());
        q.setAllowOptionReuse(form.isAllowOptionReuse());
        q.setPreparationSeconds(form.getPreparationSeconds());
        q.setStatus(form.getStatus());
        q = questions.save(q);
        if (id != null) {
            for (QuestionItemEntity old : items.findByQuestionIdOrderByPosition(id)) {
                options.deleteByQuestionItemId(old.getId());
            }
            items.deleteByQuestionId(id);
        }
        int itemPosition = 1;
        for (QuestionForm.ItemForm value : form.getItems()) {
            QuestionItemEntity item = new QuestionItemEntity();
            item.setQuestionId(q.getId());
            item.setContent(value.getContent().trim());
            item.setPosition(itemPosition++);
            item.setCorrectPosition(value.getCorrectPosition());
            item.setMinWordCount(value.getMinWordCount());
            item.setMaxWordCount(value.getMaxWordCount());
            item.setResponseSeconds(value.getResponseSeconds());
            item = items.save(item);
            if (usesOptions(form.getQuestionType()))
                for (int optionIndex = 0; optionIndex < value.getOptions().size(); optionIndex++) {
                    QuestionForm.OptionForm option = value.getOptions().get(optionIndex);
                    if (option.getContent() != null && !option.getContent().isBlank()) {
                        QuestionOptionEntity entity = new QuestionOptionEntity();
                        entity.setQuestionItemId(item.getId());
                        entity.setContent(option.getContent().trim());
                        entity.setOptionCode(option.getCode());
                        entity.setCorrect(
                                Objects.equals(optionIndex + 1, value.getCorrectPosition()));
                        entity.setPosition(optionIndex + 1);
                        options.save(entity);
                    }
                }
        }
        if (upload != null && !upload.isEmpty()) saveMedia(q, upload);
        return q.getId();
    }

    @Transactional
    public void publishQuestion(Integer id) {
        QuestionEntity q =
                questions
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionForm form = questionForm(id);
        QuestionDefinitionValidator.validate(form);
        q.setStatus("PUBLISHED");
        questions.save(q);
    }

    public ExamForm examForm(Integer id) {
        return examCatalog.examForm(id);
    }

    @Transactional
    public Integer saveExam(Integer id, ExamForm form) {
        return examCatalog.saveExam(id, form);
    }

    @Transactional
    public void publishExam(Integer id) {
        examCatalog.publishExam(id);
    }

    @Transactional
    public void addMedia(Integer questionId, MultipartFile[] uploads) {
        QuestionEntity question =
                questions
                        .findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        assertQuestionNotAnswered(questionId);
        if (uploads == null || uploads.length == 0)
            throw new BusinessRuleException("Chưa chọn media");
        for (MultipartFile upload : uploads)
            if (upload != null && !upload.isEmpty()) saveMedia(question, upload);
    }

    @Transactional
    public void deleteMedia(Integer questionId, Integer mediaId) {
        assertQuestionNotAnswered(questionId);
        QuestionMediaEntity value =
                media.findById(mediaId)
                        .filter(item -> Objects.equals(item.getQuestionId(), questionId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy media của câu hỏi"));
        // Do not remove the physical object here: active attempts keep a
        // snapshot of this media and must remain playable after an admin edits
        // the catalogue. A later retention job can delete unreferenced files.
        media.delete(value);
        List<QuestionMediaEntity> remaining = media.findByQuestionIdOrderByPosition(questionId);
        int position = 1;
        for (QuestionMediaEntity item : remaining) {
            item.setPosition(position++);
            media.save(item);
        }
    }

    @Transactional
    public void updateMedia(
            Integer questionId,
            Integer mediaId,
            String altText,
            String transcript,
            Integer maxPlayCount) {
        assertQuestionNotAnswered(questionId);
        QuestionMediaEntity value =
                media.findById(mediaId)
                        .filter(item -> Objects.equals(item.getQuestionId(), questionId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy media của câu hỏi"));
        String cleanAlt = blankToNull(altText);
        if (cleanAlt != null && cleanAlt.length() > 500)
            throw new BusinessRuleException("Alt text tối đa 500 ký tự");
        String cleanTranscript = blankToNull(transcript);
        if (cleanTranscript != null && cleanTranscript.length() > 20000)
            throw new BusinessRuleException("Transcript tối đa 20.000 ký tự");
        if ("AUDIO".equals(value.getMediaType())) {
            if (maxPlayCount == null || maxPlayCount < 1 || maxPlayCount > 2)
                throw new BusinessRuleException("Audio chỉ được phép từ 1 đến 2 lượt nghe");
            value.setMaxPlayCount(maxPlayCount);
        } else {
            value.setMaxPlayCount(null);
        }
        value.setAltText(cleanAlt);
        value.setTranscript(cleanTranscript);
        media.save(value);
    }

    @Transactional
    public void moveMedia(Integer questionId, Integer mediaId, int direction) {
        assertQuestionNotAnswered(questionId);
        if (direction != -1 && direction != 1)
            throw new BusinessRuleException("Hướng sắp xếp không hợp lệ");
        List<QuestionMediaEntity> values =
                new ArrayList<>(media.findByQuestionIdOrderByPosition(questionId));
        int index = -1;
        for (int i = 0; i < values.size(); i++)
            if (Objects.equals(values.get(i).getId(), mediaId)) {
                index = i;
                break;
            }
        if (index < 0) throw new ResourceNotFoundException("Không tìm thấy media của câu hỏi");
        int target = index + direction;
        if (target < 0 || target >= values.size()) return;
        Collections.swap(values, index, target);
        for (int i = 0; i < values.size(); i++) {
            values.get(i).setPosition(i + 1);
            media.save(values.get(i));
        }
    }

    @Transactional(readOnly = true)
    public MediaContent mediaContent(Integer questionId, Integer mediaId) {
        QuestionMediaEntity value =
                media.findById(mediaId)
                        .filter(item -> Objects.equals(item.getQuestionId(), questionId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy media của câu hỏi"));
        return new MediaContent(storage.load(value.getMediaUrl()), value.getContentType());
    }

    @Transactional
    public void archiveQuestion(Integer id) {
        QuestionEntity q =
                questions
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        q.setStatus("ARCHIVED");
        questions.save(q);
    }

    @Transactional
    public void restoreQuestion(Integer id) {
        QuestionEntity q =
                questions
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (!"ARCHIVED".equals(q.getStatus()))
            throw new BusinessRuleException("Câu hỏi chưa được lưu trữ");
        q.setStatus("DRAFT");
        questions.save(q);
    }

    public record MediaContent(Resource resource, String contentType) {}

    public List<Certificate> activeCertificates() {
        return certificates.findByStatusOrderByName(RecordStatus.ACTIVE);
    }

    public List<ScoringScheme> activeSchemes() {
        return schemes.findByStatusOrderByName(RecordStatus.ACTIVE);
    }

    public List<QuestionEntity> publishedQuestions() {
        return questions.findByStatusOrderByUpdatedAtDesc("PUBLISHED");
    }

    public List<QuestionMediaEntity> mediaItems(Integer questionId) {
        return media.findByQuestionIdOrderByPosition(questionId);
    }

    public List<String> skills() {
        return SKILLS;
    }

    public List<String> levels() {
        return LEVELS;
    }

    public List<String> questionTypes() {
        return TYPES;
    }

    public List<String> examTypes() {
        return EXAM_TYPES;
    }

    private void saveMedia(QuestionEntity q, MultipartFile file) {
        new QuestionMediaWriter(media, storage).save(q.getId(), file);
    }

    private void assertQuestionNotAnswered(Integer questionId) {
        if (answers.existsByQuestionId(questionId)) {
            throw new BusinessRuleException(
                    "Câu hỏi đã có trong lịch sử làm bài. Hãy lưu trữ và tạo phiên bản câu hỏi"
                            + " mới");
        }
    }

    private boolean usesOptions(String type) {
        return QuestionType.valueOf(type).usesOptions();
    }

    private boolean matches(String term, String... values) {
        return term.isBlank()
                || Arrays.stream(values)
                        .filter(Objects::nonNull)
                        .map(x -> x.toLowerCase(Locale.ROOT))
                        .anyMatch(x -> x.contains(term));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Entry questionEntry(QuestionEntity q) {
        List<Row> rows = new ArrayList<>();
        if ("READING".equals(q.getSkill())) {
            String passage = q.getReadingPassage();
            rows.add(
                    new Row(
                            "ĐOẠN VĂN",
                            passage == null || passage.isBlank()
                                    ? "Chưa có đoạn văn nguồn."
                                    : passage,
                            "Văn bản nguồn"));
        }
        for (QuestionItemEntity item : items.findByQuestionIdOrderByPosition(q.getId())) {
            rows.add(
                    new Row(
                            String.valueOf(item.getPosition()),
                            item.getContent(),
                            item.getCorrectPosition() == null
                                    ? ""
                                    : "Đúng vị trí " + item.getCorrectPosition()));
            options.findByQuestionItemIdOrderByPosition(item.getId())
                    .forEach(
                            o ->
                                    rows.add(
                                            new Row(
                                                    o.getOptionCode(),
                                                    o.getContent(),
                                                    Boolean.TRUE.equals(o.getCorrect())
                                                            ? "Đáp án đúng"
                                                            : "")));
        }
        media.findByQuestionIdOrderByPosition(q.getId())
                .forEach(
                        m ->
                                rows.add(
                                        new Row(
                                                "MEDIA #" + m.getId(),
                                                m.getOriginalFilename(),
                                                m.getMediaType()
                                                        + " · "
                                                        + m.getFileSize()
                                                        + " bytes")));
        return new Entry(
                q.getId(),
                q.getContent(),
                q.getContent(),
                q.getSkill(),
                q.getCefrLevel(),
                q.getQuestionType(),
                q.getStatus(),
                statusLabel(q.getStatus()),
                q.getUpdatedAt() == null ? "" : q.getUpdatedAt().toLocalDate().toString(),
                List.of(
                        new Field("Phần thi", String.valueOf(q.getPartNo())),
                        new Field("Độ khó", String.valueOf(q.getDifficulty()))),
                rows);
    }

    private void normalizeItems(QuestionForm form) {
        if (form.getItems() == null || form.getItems().isEmpty())
            form.setItems(List.of(new QuestionForm.ItemForm()));
        QuestionForm.ItemForm first = form.getItems().get(0);
        if ((first.getContent() == null || first.getContent().isBlank())
                && form.getItemContent() != null) first.setContent(form.getItemContent());
        if (first.getCorrectPosition() == null) first.setCorrectPosition(form.getCorrectPosition());
        if (first.getMinWordCount() == null) first.setMinWordCount(form.getMinWordCount());
        if (first.getMaxWordCount() == null) first.setMaxWordCount(form.getMaxWordCount());
        if (first.getResponseSeconds() == null) first.setResponseSeconds(form.getResponseSeconds());
        if ((first.getOptions() == null || first.getOptions().isEmpty())
                && form.getOptions() != null) first.setOptions(form.getOptions());
    }

    private void normalizeQuestionFields(QuestionForm form) {
        String type = form.getQuestionType();
        if (type == null || !TYPES.contains(type)) return;
        if (!"READING".equals(form.getSkill())) form.setReadingPassage(null);
        for (QuestionForm.ItemForm item : form.getItems()) {
            if (!usesOptions(type)) item.setOptions(new ArrayList<>());
            if (!"ORDERING".equals(type) && !usesOptions(type)) item.setCorrectPosition(null);
            if (!"WRITING_TEXT".equals(type)) {
                item.setMinWordCount(null);
                item.setMaxWordCount(null);
            }
            if (!Set.of("WRITING_TEXT", "SPEAKING_AUDIO").contains(type))
                item.setResponseSeconds(null);
        }
        if (!"SPEAKING_AUDIO".equals(type)) form.setPreparationSeconds(null);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PUBLISHED" -> "Đã công bố";
            case "ARCHIVED" -> "Đã lưu trữ";
            default -> "Bản nháp";
        };
    }
}
