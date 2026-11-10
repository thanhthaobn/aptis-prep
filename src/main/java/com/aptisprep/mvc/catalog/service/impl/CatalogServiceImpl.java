package com.aptisprep.mvc.catalog.service.impl;

import com.aptisprep.mvc.catalog.dto.AdminCatalogViewDTO.CatalogPage;
import com.aptisprep.mvc.catalog.dto.AdminCatalogViewDTO.Entry;
import com.aptisprep.mvc.catalog.dto.AdminCatalogViewDTO.Field;
import com.aptisprep.mvc.catalog.dto.AdminCatalogViewDTO.Module;
import com.aptisprep.mvc.catalog.dto.AdminCatalogViewDTO.Row;
import com.aptisprep.mvc.catalog.dto.ExamCatalogDTO.CertificateOptionDTO;
import com.aptisprep.mvc.catalog.dto.ExamCatalogDTO.PublishedQuestionOptionDTO;
import com.aptisprep.mvc.catalog.dto.ExamCatalogDTO.QuestionMediaDTO;
import com.aptisprep.mvc.catalog.dto.ExamCatalogDTO.SchemeOptionDTO;
import com.aptisprep.mvc.catalog.dto.ExamFormDTO;
import com.aptisprep.mvc.catalog.dto.QuestionEntityDTO;
import com.aptisprep.mvc.catalog.dto.QuestionFormDTO;
import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.repository.*;
import com.aptisprep.mvc.catalog.service.AdminExamService;
import com.aptisprep.mvc.catalog.service.CatalogService;
import com.aptisprep.mvc.common.enums.CefrLevel;
import com.aptisprep.mvc.common.enums.QuestionType;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.scoring.service.CertificateSchemeService;
import com.aptisprep.mvc.storage.service.StorageService;
import com.aptisprep.repository.UserAnswerRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogServiceImpl implements CatalogService {
    private static final List<String> SKILLS = Skill.codes();
    private static final List<String> LEVELS = CefrLevel.curriculumLevels();
    private static final List<String> TYPES =
            Arrays.stream(QuestionType.values()).map(Enum::name).toList();
    private static final List<String> EXAM_TYPES = ExamDefinitionValidator.TYPES;
    private final QuestionRepository questionRepository;
    private final QuestionItemRepository questionItemRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final MockExamRepository mockExamRepository;
    private final CertificateSchemeService certificateSchemes;
    private final UserAnswerRepository userAnswerRepository;
    private final StorageService storage;
    private final AdminExamService examCatalog;

    public CatalogServiceImpl(
            QuestionRepository questionRepository,
            QuestionItemRepository questionItemRepository,
            QuestionOptionRepository questionOptionRepository,
            QuestionMediaRepository questionMediaRepository,
            MockExamRepository mockExamRepository,
            CertificateSchemeService certificateSchemes,
            UserAnswerRepository userAnswerRepository,
            StorageService storage,
            AdminExamService examCatalog) {
        this.questionRepository = questionRepository;
        this.questionItemRepository = questionItemRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.mockExamRepository = mockExamRepository;
        this.certificateSchemes = certificateSchemes;
        this.userAnswerRepository = userAnswerRepository;
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
            return questionRepository.findAll().stream()
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
            return mockExamRepository.findAll().stream()
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
                    questionRepository
                            .findById(id)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy câu hỏi")));
        if ("exams".equals(key))
            return examCatalog.entry(
                    mockExamRepository
                            .findById(id)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy đề thi")));
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    public QuestionFormDTO questionForm(Integer id) {
        QuestionFormDTO form = new QuestionFormDTO();
        if (id == null) return form;
        QuestionEntity q =
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        form = QuestionEntityDTO.convertEntityToDto(q).toForm();
        List<QuestionFormDTO.ItemForm> values = new ArrayList<>();
        for (QuestionItemEntity item : questionItemRepository.findByQuestionIdOrderByPosition(id)) {
            QuestionFormDTO.ItemForm itemForm = new QuestionFormDTO.ItemForm();
            itemForm.setContent(item.getContent());
            itemForm.setCorrectPosition(item.getCorrectPosition());
            itemForm.setMinWordCount(item.getMinWordCount());
            itemForm.setMaxWordCount(item.getMaxWordCount());
            itemForm.setResponseSeconds(item.getResponseSeconds());
            List<QuestionFormDTO.OptionForm> os =
                    questionOptionRepository
                            .findByQuestionItemIdOrderByPosition(item.getId())
                            .stream()
                            .map(
                                    o -> {
                                        QuestionFormDTO.OptionForm f =
                                                new QuestionFormDTO.OptionForm(o.getOptionCode());
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
    public Integer saveQuestion(Integer id, QuestionFormDTO form, MultipartFile upload) {
        normalizeItems(form);
        normalizeQuestionFields(form);
        QuestionDefinitionValidator.validate(form);
        QuestionEntity question =
                id == null
                        ? new QuestionEntity()
                        : questionRepository
                                .findById(id)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Không tìm thấy câu hỏi"));
        if (id != null && userAnswerRepository.existsByQuestionId(id)) {
            throw new BusinessRuleException(
                    "Câu hỏi đã có trong lịch sử làm bài. Hãy lưu trữ và tạo phiên bản câu hỏi mới");
        }
        if ("PUBLISHED".equals(question.getStatus()) && !"PUBLISHED".equals(form.getStatus())) {
            throw new BusinessRuleException("Câu hỏi đã công bố không thể quay về bản nháp");
        }
        applyQuestionFields(question, form);
        question = questionRepository.save(question);
        replaceQuestionItems(question.getId(), form, id != null);
        if (upload != null && !upload.isEmpty()) saveMedia(question, upload);
        return question.getId();
    }

    @Transactional
    public void publishQuestion(Integer id) {
        QuestionEntity q =
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionFormDTO form = questionForm(id);
        QuestionDefinitionValidator.validate(form);
        q.setStatus("PUBLISHED");
        questionRepository.save(q);
    }

    public ExamFormDTO examForm(Integer id) {
        return examCatalog.examForm(id);
    }

    @Transactional
    public Integer saveExam(Integer id, ExamFormDTO form) {
        return examCatalog.saveExam(id, form);
    }

    @Transactional
    public void publishExam(Integer id) {
        examCatalog.publishExam(id);
    }

    public void unpublishExam(Integer id) {
        examCatalog.unpublishExam(id);
    }

    public List<ExamPreviewSection> examPreview(Integer id) {
        ExamFormDTO form = examCatalog.examForm(id);
        examCatalog.normalizeSections(form);
        return form.getSections().stream()
                .map(
                        section ->
                                new ExamPreviewSection(
                                        section.getSkill(),
                                        section.getDurationMinutes(),
                                        parseQuestionIds(section.getQuestionIds()).stream()
                                                .map(questionRepository::findById)
                                                .flatMap(Optional::stream)
                                                .map(this::examPreviewQuestion)
                                                .toList()))
                .toList();
    }

    private List<Integer> parseQuestionIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(Integer::valueOf)
                .toList();
    }

    private ExamPreviewQuestion examPreviewQuestion(QuestionEntity question) {
        List<ExamPreviewItem> items =
                questionItemRepository.findByQuestionIdOrderByPosition(question.getId()).stream()
                        .map(
                                item ->
                                        new ExamPreviewItem(
                                                item.getPosition(),
                                                item.getContent(),
                                                item.getResponseSeconds(),
                                                item.getMinWordCount(),
                                                item.getMaxWordCount(),
                                                questionOptionRepository
                                                        .findByQuestionItemIdOrderByPosition(
                                                                item.getId())
                                                        .stream()
                                                        .map(
                                                                option ->
                                                                        new ExamPreviewOption(
                                                                                option
                                                                                        .getOptionCode(),
                                                                                option
                                                                                        .getContent()))
                                                        .toList()))
                        .toList();
        List<ExamPreviewMedia> media =
                questionMediaRepository.findByQuestionIdOrderByPosition(question.getId()).stream()
                        .map(
                                item ->
                                        new ExamPreviewMedia(
                                                item.getId(),
                                                item.getMediaType(),
                                                item.getOriginalFilename(),
                                                item.getContentType(),
                                                item.getAltText(),
                                                item.getMediaUrl() != null
                                                        && item.getMediaUrl()
                                                                .startsWith("speech://")))
                        .toList();
        return new ExamPreviewQuestion(
                question.getId(),
                question.getSkill(),
                question.getQuestionType(),
                questionTypeLabel(question.getQuestionType()),
                question.getContent(),
                question.getInstruction(),
                question.getReadingPassage(),
                items,
                media);
    }

    private String questionTypeLabel(String type) {
        return switch (type) {
            case "SINGLE_CHOICE" -> "Chọn một đáp án";
            case "MATCHING" -> "Nối đáp án";
            case "ORDERING" -> "Sắp xếp thứ tự";
            case "WRITING_TEXT" -> "Trả lời bằng văn bản";
            case "SPEAKING_AUDIO" -> "Trả lời bằng ghi âm";
            default -> type;
        };
    }

    @Transactional
    public void addMedia(Integer questionId, MultipartFile[] uploads) {
        QuestionEntity question =
                questionRepository
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
                questionMediaRepository
                        .findById(mediaId)
                        .filter(item -> Objects.equals(item.getQuestionId(), questionId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy media của câu hỏi"));
        questionMediaRepository.delete(value);
        List<QuestionMediaEntity> remaining =
                questionMediaRepository.findByQuestionIdOrderByPosition(questionId);
        int position = 1;
        for (QuestionMediaEntity item : remaining) {
            item.setPosition(position++);
            questionMediaRepository.save(item);
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
                questionMediaRepository
                        .findById(mediaId)
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
        questionMediaRepository.save(value);
    }

    @Transactional
    public void moveMedia(Integer questionId, Integer mediaId, int direction) {
        assertQuestionNotAnswered(questionId);
        if (direction != -1 && direction != 1)
            throw new BusinessRuleException("Hướng sắp xếp không hợp lệ");
        List<QuestionMediaEntity> values =
                new ArrayList<>(
                        questionMediaRepository.findByQuestionIdOrderByPosition(questionId));
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
            questionMediaRepository.save(values.get(i));
        }
    }

    @Transactional(readOnly = true)
    public MediaContent mediaContent(Integer questionId, Integer mediaId) {
        QuestionMediaEntity value =
                questionMediaRepository
                        .findById(mediaId)
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
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        q.setStatus("ARCHIVED");
        questionRepository.save(q);
    }

    @Transactional
    public void restoreQuestion(Integer id) {
        QuestionEntity q =
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (!"ARCHIVED".equals(q.getStatus()))
            throw new BusinessRuleException("Câu hỏi chưa được lưu trữ");
        q.setStatus("DRAFT");
        questionRepository.save(q);
    }

    public List<CertificateOptionDTO> activeCertificates() {
        return certificateSchemes.findAllActiveCertificates().stream()
                .map(c -> new CertificateOptionDTO(c.getId(), c.getName()))
                .toList();
    }

    public List<SchemeOptionDTO> activeSchemes() {
        return certificateSchemes.findAllActiveSchemes().stream()
                .map(
                        s ->
                                new SchemeOptionDTO(
                                        s.getId(),
                                        s.getName(),
                                        s.getVersion(),
                                        s.getExamType(),
                                        s.getScopeSkill(),
                                        s.getCertificate() == null
                                                ? null
                                                : new CertificateOptionDTO(
                                                        s.getCertificate().getId(),
                                                        s.getCertificate().getName())))
                .toList();
    }

    public List<PublishedQuestionOptionDTO> publishedQuestions() {
        return questionRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .map(q -> new PublishedQuestionOptionDTO(q.getId(), q.getSkill(), q.getContent()))
                .toList();
    }

    public List<QuestionMediaDTO> mediaItems(Integer questionId) {
        return questionMediaRepository.findByQuestionIdOrderByPosition(questionId).stream()
                .map(
                        m ->
                                new QuestionMediaDTO(
                                        m.getId(),
                                        m.getMediaType(),
                                        m.getOriginalFilename(),
                                        m.getContentType(),
                                        m.getFileSize(),
                                        m.getPosition(),
                                        m.getAltText(),
                                        m.getMediaUrl(),
                                        m.getTranscript(),
                                        m.getMaxPlayCount()))
                .toList();
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
        new QuestionMediaWriter(questionMediaRepository, storage).save(q.getId(), file);
    }

    private void assertQuestionNotAnswered(Integer questionId) {
        if (userAnswerRepository.existsByQuestionId(questionId)) {
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
        for (QuestionItemEntity item :
                questionItemRepository.findByQuestionIdOrderByPosition(q.getId())) {
            rows.add(
                    new Row(
                            String.valueOf(item.getPosition()),
                            item.getContent(),
                            item.getCorrectPosition() == null
                                    ? ""
                                    : "Đúng vị trí " + item.getCorrectPosition()));
            questionOptionRepository
                    .findByQuestionItemIdOrderByPosition(item.getId())
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
        questionMediaRepository
                .findByQuestionIdOrderByPosition(q.getId())
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

    private void normalizeItems(QuestionFormDTO form) {
        if (form.getItems() == null || form.getItems().isEmpty())
            form.setItems(List.of(new QuestionFormDTO.ItemForm()));
        QuestionFormDTO.ItemForm first = form.getItems().get(0);
        if ((first.getContent() == null || first.getContent().isBlank())
                && form.getItemContent() != null) first.setContent(form.getItemContent());
        if (first.getCorrectPosition() == null) first.setCorrectPosition(form.getCorrectPosition());
        if (first.getMinWordCount() == null) first.setMinWordCount(form.getMinWordCount());
        if (first.getMaxWordCount() == null) first.setMaxWordCount(form.getMaxWordCount());
        if (first.getResponseSeconds() == null) first.setResponseSeconds(form.getResponseSeconds());
        if ((first.getOptions() == null || first.getOptions().isEmpty())
                && form.getOptions() != null) first.setOptions(form.getOptions());
    }

    private void normalizeQuestionFields(QuestionFormDTO form) {
        String type = form.getQuestionType();
        if (type == null || !TYPES.contains(type)) return;
        if (!"READING".equals(form.getSkill())) form.setReadingPassage(null);
        for (QuestionFormDTO.ItemForm item : form.getItems()) {
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

    private void applyQuestionFields(QuestionEntity question, QuestionFormDTO form) {
        QuestionEntity mapped = QuestionEntityDTO.convertDtoToEntity(form);
        question.setContent(mapped.getContent());
        question.setInstruction(mapped.getInstruction());
        question.setReadingPassage(mapped.getReadingPassage());
        question.setQuestionType(mapped.getQuestionType());
        question.setSkill(mapped.getSkill());
        question.setPartNo(mapped.getPartNo());
        question.setCefrLevel(mapped.getCefrLevel());
        question.setDifficulty(mapped.getDifficulty());
        question.setAllowOptionReuse(mapped.isAllowOptionReuse());
        question.setPreparationSeconds(mapped.getPreparationSeconds());
        question.setStatus(mapped.getStatus());
    }

    private void replaceQuestionItems(Integer questionId, QuestionFormDTO form, boolean editing) {
        if (editing) {
            for (QuestionItemEntity item :
                    questionItemRepository.findByQuestionIdOrderByPosition(questionId)) {
                questionOptionRepository.deleteByQuestionItemId(item.getId());
            }
            questionItemRepository.deleteByQuestionId(questionId);
        }
        int position = 1;
        for (QuestionFormDTO.ItemForm itemForm : form.getItems()) {
            QuestionItemEntity item = new QuestionItemEntity();
            item.setQuestionId(questionId);
            item.setContent(itemForm.getContent().trim());
            item.setPosition(position++);
            item.setCorrectPosition(itemForm.getCorrectPosition());
            item.setMinWordCount(itemForm.getMinWordCount());
            item.setMaxWordCount(itemForm.getMaxWordCount());
            item.setResponseSeconds(itemForm.getResponseSeconds());
            item = questionItemRepository.save(item);
            if (usesOptions(form.getQuestionType())) saveItemOptions(item.getId(), itemForm);
        }
    }

    private void saveItemOptions(Integer itemId, QuestionFormDTO.ItemForm itemForm) {
        for (int index = 0; index < itemForm.getOptions().size(); index++) {
            QuestionFormDTO.OptionForm optionForm = itemForm.getOptions().get(index);
            if (optionForm.getContent() == null || optionForm.getContent().isBlank()) continue;
            QuestionOptionEntity option = new QuestionOptionEntity();
            option.setQuestionItemId(itemId);
            option.setContent(optionForm.getContent().trim());
            option.setOptionCode(optionForm.getCode());
            option.setCorrect(Objects.equals(index + 1, itemForm.getCorrectPosition()));
            option.setPosition(index + 1);
            questionOptionRepository.save(option);
        }
    }
}
