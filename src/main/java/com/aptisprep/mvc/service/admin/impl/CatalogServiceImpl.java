package com.aptisprep.mvc.service.admin.impl;

import com.aptisprep.mvc.dto.CertificateDTO;
import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.dto.QuestionDTO;
import com.aptisprep.mvc.dto.QuestionItemDTO;
import com.aptisprep.mvc.dto.QuestionMediaDTO;
import com.aptisprep.mvc.dto.QuestionOptionDTO;
import com.aptisprep.mvc.dto.ScoringSchemeDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.enums.ExamType;
import com.aptisprep.mvc.enums.QuestionType;
import com.aptisprep.mvc.enums.RecordStatus;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.repository.UserAnswerRepository;
import com.aptisprep.mvc.service.admin.AdminExamService;
import com.aptisprep.mvc.service.admin.CatalogService;
import com.aptisprep.mvc.service.admin.QuestionMediaService;
import com.aptisprep.mvc.service.storage.StorageService;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogServiceImpl implements CatalogService {
    private List<String> SKILLS = Skill.codes();
    private List<String> LEVELS = CefrLevel.curriculumLevels();
    private List<String> TYPES =
            Arrays.stream(QuestionType.values()).map(streamValue -> streamValue.name()).toList();
    private List<String> EXAM_TYPES = ExamType.codes();
    private QuestionRepository questionRepository;
    private QuestionItemRepository questionItemRepository;
    private QuestionOptionRepository questionOptionRepository;
    private QuestionMediaRepository questionMediaRepository;
    private MockExamRepository mockExamRepository;
    private CertificateRepository certificateRepository;
    private ScoringSchemeRepository scoringSchemeRepository;
    private UserAnswerRepository userAnswerRepository;
    private StorageService storage;
    private QuestionMediaService questionMediaService;
    private AdminExamService examCatalog;

    public CatalogServiceImpl(
            QuestionRepository questionRepository,
            QuestionItemRepository questionItemRepository,
            QuestionOptionRepository questionOptionRepository,
            QuestionMediaRepository questionMediaRepository,
            MockExamRepository mockExamRepository,
            CertificateRepository certificateRepository,
            ScoringSchemeRepository scoringSchemeRepository,
            UserAnswerRepository userAnswerRepository,
            StorageService storage,
            QuestionMediaService questionMediaService,
            AdminExamService examCatalog) {
        this.questionRepository = questionRepository;
        this.questionItemRepository = questionItemRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.mockExamRepository = mockExamRepository;
        this.certificateRepository = certificateRepository;
        this.scoringSchemeRepository = scoringSchemeRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.storage = storage;
        this.questionMediaService = questionMediaService;
        this.examCatalog = examCatalog;
    }

    public Map<String, Object> module(String key) {
        if ("questions".equals(key))
            return values(
                    "key",
                    key,
                    "title",
                    "Ngân hàng câu hỏi",
                    "description",
                    "Quản lý nội dung, phương án trả lời và tài nguyên đính kèm.",
                    "singular",
                    "câu hỏi",
                    "createLabel",
                    "Thêm câu hỏi",
                    "typeLabel",
                    "Dạng câu hỏi",
                    "kinds",
                    List.of(
                            "SINGLE_CHOICE",
                            "MATCHING",
                            "ORDERING",
                            "WRITING_TEXT",
                            "SPEAKING_AUDIO"),
                    "editable",
                    true,
                    "baseUrl",
                    "/admin/questions");
        if ("exams".equals(key))
            return values(
                    "key",
                    key,
                    "title",
                    "Đề thi",
                    "description",
                    "Sắp xếp câu hỏi, thời gian và phạm vi kỹ năng của từng đề.",
                    "singular",
                    "đề thi",
                    "createLabel",
                    "Tạo đề thi",
                    "typeLabel",
                    "Loại đề",
                    "kinds",
                    EXAM_TYPES,
                    "editable",
                    true,
                    "baseUrl",
                    "/admin/exams");
        throw new ResourceNotFoundException("Không tìm thấy nội dung");
    }

    private Map<String, Object> values(Object... pairs) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2)
            result.put(String.valueOf(pairs[index]), pairs[index + 1]);
        return result;
    }

    public Page<QuestionDTO> questionPage(
            String q, String skill, String status, String kind, int page, int size) {
        String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<QuestionDTO> values =
                questionRepository.findAll().stream()
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
                        .sorted(
                                Comparator.comparing(
                                                (Question streamValue) ->
                                                        streamValue.getUpdatedAt())
                                        .reversed())
                        .map(streamValue -> QuestionDTO.convertEntityToDto(streamValue))
                        .toList();
        return paginate(values, page, size);
    }

    @Override
    public Page<MockExamDTO> examPage(
            String q, String skill, String status, String kind, int page, int size) {
        String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<MockExamDTO> values =
                mockExamRepository.findAll().stream()
                        .filter(x -> matches(term, x.getTitle(), x.getDescription()))
                        .filter(x -> skill.isBlank() || Objects.equals(x.getSkill(), skill))
                        .filter(x -> status.isBlank() || x.getStatus().equals(status))
                        .filter(x -> kind.isBlank() || x.getExamType().equals(kind))
                        .sorted(
                                Comparator.comparing(
                                                (MockExam streamValue) ->
                                                        streamValue.getUpdatedAt())
                                        .reversed())
                        .map(streamValue -> examCatalog.entry(streamValue))
                        .toList();
        return paginate(values, page, size);
    }

    private <T> Page<T> paginate(List<T> values, int page, int size) {
        int safeSize = Math.min(50, Math.max(1, size));
        int safePage = Math.max(0, page);
        int from = Math.min(safePage * safeSize, values.size());
        int to = Math.min(from + safeSize, values.size());
        return new PageImpl<>(
                values.subList(from, to), PageRequest.of(safePage, safeSize), values.size());
    }

    @Override
    public QuestionDTO question(Integer id) {
        questionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        return questionForm(id);
    }

    @Override
    public MockExamDTO exam(Integer id) {
        return examCatalog.entry(
                mockExamRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi")));
    }

    public QuestionDTO questionForm(Integer id) {
        QuestionDTO form = new QuestionDTO();
        if (id == null) {
            form.setItems(List.of(new QuestionItemDTO()));
            return form;
        }
        Question q =
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        form = QuestionDTO.convertEntityToDto(q);
        List<QuestionItemDTO> values = new ArrayList<>();
        for (QuestionItem item : questionItemRepository.findByQuestionIdOrderByPosition(id)) {
            QuestionItemDTO itemForm = QuestionItemDTO.convertEntityToDto(item);
            List<QuestionOptionDTO> os =
                    questionOptionRepository
                            .findByQuestionItemIdOrderByPosition(item.getId())
                            .stream()
                            .map(streamValue -> QuestionOptionDTO.convertEntityToDto(streamValue))
                            .collect(
                                    Collectors.toCollection(
                                            () -> new ArrayList<QuestionOptionDTO>()));
            if (!os.isEmpty()) itemForm.setOptions(os);
            values.add(itemForm);
        }
        if (!values.isEmpty()) form.setItems(values);
        return form;
    }

    @Transactional
    public Integer saveQuestion(Integer id, QuestionDTO form, MultipartFile upload) {
        normalizeItems(form);
        normalizeQuestionFields(form);
        validateQuestionDefinition(form);
        Question question =
                id == null
                        ? new Question()
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
        question = QuestionDTO.convertDtoToEntity(form);
        question.setContent(form.getContent().trim());
        question.setInstruction(blankToNull(form.getInstruction()));
        question.setReadingPassage(
                "READING".equals(form.getSkill()) ? blankToNull(form.getReadingPassage()) : null);
        question = questionRepository.save(question);
        replaceQuestionItems(question.getId(), form, id != null);
        if (upload != null && !upload.isEmpty()) saveMedia(question, upload);
        return question.getId();
    }

    @Transactional
    public void publishQuestion(Integer id) {
        Question q =
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionDTO form = questionForm(id);
        validateQuestionDefinition(form);
        q.setStatus("PUBLISHED");
        questionRepository.save(q);
    }

    public MockExamDTO examForm(Integer id) {
        return examCatalog.examForm(id);
    }

    @Transactional
    public Integer saveExam(Integer id, MockExamDTO form) {
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
        MockExamDTO form = examCatalog.examForm(id);
        examCatalog.normalizeSections(form);
        return form.getSections().stream()
                .map(
                        section ->
                                new ExamPreviewSection(
                                        section.getSkill(),
                                        section.getDurationMinutes(),
                                        parseQuestionIds(section.getQuestionIds()).stream()
                                                .map(
                                                        streamValue ->
                                                                questionRepository.findById(
                                                                        streamValue))
                                                .flatMap(streamValue -> streamValue.stream())
                                                .map(
                                                        streamValue ->
                                                                examPreviewQuestion(streamValue))
                                                .toList()))
                .toList();
    }

    private List<Integer> parseQuestionIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(streamValue -> streamValue.trim())
                .filter(token -> !token.isBlank())
                .map(streamValue -> Integer.valueOf(streamValue))
                .toList();
    }

    private ExamPreviewQuestion examPreviewQuestion(Question question) {
        List<ExamPreviewItem> items =
                questionItemRepository.findByQuestionIdOrderByPosition(question.getId()).stream()
                        .map(
                                item ->
                                        new ExamPreviewItem(
                                                item.getPosition(),
                                                item.getContent(),
                                                item.getPoints(),
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
                                                item.getTranscript(),
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
        Question question =
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
        QuestionMedia value =
                questionMediaRepository
                        .findById(mediaId)
                        .filter(item -> Objects.equals(item.getQuestionId(), questionId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy media của câu hỏi"));
        // Do not remove the physical object here: active attempts keep a
        // snapshot of this media and must remain playable after an admin edits
        // the catalogue. A later retention job can delete unreferenced files.
        questionMediaRepository.delete(value);
        List<QuestionMedia> remaining =
                questionMediaRepository.findByQuestionIdOrderByPosition(questionId);
        int position = 1;
        for (QuestionMedia item : remaining) {
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
        QuestionMedia value =
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
        List<QuestionMedia> values =
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
        QuestionMedia value =
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
        Question q =
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        q.setStatus("ARCHIVED");
        questionRepository.save(q);
    }

    @Transactional
    public void restoreQuestion(Integer id) {
        Question q =
                questionRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (!"ARCHIVED".equals(q.getStatus()))
            throw new BusinessRuleException("Câu hỏi chưa được lưu trữ");
        q.setStatus("DRAFT");
        questionRepository.save(q);
    }

    public List<Certificate> activeCertificates() {
        return certificateRepository.findByStatusOrderByName(RecordStatus.ACTIVE).stream()
                .filter(Certificate::isAptisGeneral)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateDTO> activeCertificateViews() {
        return activeCertificates().stream().map(CertificateDTO::convertEntityToDto).toList();
    }

    public List<ScoringScheme> activeSchemes() {
        return scoringSchemeRepository.findByStatusOrderByName(RecordStatus.ACTIVE).stream()
                .filter(
                        scheme ->
                                scheme.getCertificate() != null
                                        && scheme.getCertificate().isAptisGeneral())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoringSchemeDTO> activeSchemeViews() {
        return activeSchemes().stream().map(ScoringSchemeDTO::convertEntityToDto).toList();
    }

    public List<Question> publishedQuestions() {
        return questionRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED");
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> publishedQuestionViews() {
        return publishedQuestions().stream().map(QuestionDTO::convertEntityToDto).toList();
    }

    public List<QuestionMedia> mediaItems(Integer questionId) {
        return questionMediaRepository.findByQuestionIdOrderByPosition(questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionMediaDTO> mediaItemViews(Integer questionId) {
        return mediaItems(questionId).stream().map(QuestionMediaDTO::convertEntityToDto).toList();
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

    private void saveMedia(Question q, MultipartFile file) {
        questionMediaService.save(q.getId(), file);
    }

    private void validateQuestionDefinition(QuestionDTO form) {
        if (!Set.of("EASY", "MEDIUM", "HARD").contains(form.getDifficulty()))
            throw new BusinessRuleException("Độ khó không hợp lệ");
        if (!Set.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(form.getStatus()))
            throw new BusinessRuleException("Trạng thái câu hỏi không hợp lệ");
        QuestionType questionType;
        try {
            questionType = QuestionType.valueOf(form.getQuestionType());
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("Dạng câu hỏi không hợp lệ");
        }
        if (!questionType.supportsSkill(form.getSkill()))
            throw new BusinessRuleException("Dạng câu hỏi không phù hợp với kỹ năng");
        if (form.getItems() == null || form.getItems().isEmpty())
            throw new BusinessRuleException("Câu hỏi phải có ít nhất một ý hỏi");
        if ((questionType == QuestionType.MATCHING || questionType == QuestionType.ORDERING)
                && form.getItems().size() < 2)
            throw new BusinessRuleException("Dạng ghép nối hoặc sắp xếp phải có ít nhất 2 ý hỏi");
        validateQuestionItems(form, questionType);
    }

    private void validateQuestionItems(QuestionDTO form, QuestionType questionType) {
        if (questionType == QuestionType.ORDERING) {
            Set<Integer> positions = new HashSet<>();
            for (QuestionItemDTO item : form.getItems()) {
                Integer position = item.getCorrectPosition();
                if (position == null
                        || position < 1
                        || position > form.getItems().size()
                        || !positions.add(position))
                    throw new BusinessRuleException(
                            "Vị trí đúng phải đủ và duy nhất từ 1 đến số ý hỏi");
            }
        }
        for (QuestionItemDTO item : form.getItems()) {
            if (item.getContent() == null || item.getContent().isBlank())
                throw new BusinessRuleException("Nội dung ý hỏi là bắt buộc");
            if (item.getPoints() == null || item.getPoints().compareTo(BigDecimal.ZERO) <= 0)
                throw new BusinessRuleException("Điểm của mỗi ý hỏi phải lớn hơn 0");
            if (item.getMinWordCount() != null
                    && item.getMaxWordCount() != null
                    && item.getMinWordCount() > item.getMaxWordCount())
                throw new BusinessRuleException("Số từ tối thiểu không được lớn hơn tối đa");
            if (!questionType.usesOptions()) continue;
            List<QuestionOptionDTO> options =
                    item.getOptions() == null ? List.of() : item.getOptions();
            int lastFilled = 0;
            boolean gap = false;
            for (int index = 0; index < options.size(); index++) {
                boolean filled =
                        options.get(index).getContent() != null
                                && !options.get(index).getContent().isBlank();
                if (filled) {
                    if (gap)
                        throw new BusinessRuleException(
                                "Phương án phải được nhập liên tục từ A trở đi");
                    lastFilled = index + 1;
                } else {
                    gap = true;
                }
            }
            Integer correctPosition = item.getCorrectPosition();
            boolean correct =
                    correctPosition != null
                            && correctPosition >= 1
                            && correctPosition <= lastFilled
                            && correctPosition <= options.size()
                            && options.get(correctPosition - 1).getContent() != null
                            && !options.get(correctPosition - 1).getContent().isBlank();
            if (lastFilled < 2 || !correct)
                throw new BusinessRuleException(
                        "Mỗi ý hỏi phải có ít nhất 2 phương án và một đáp án đúng");
        }
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
                        .filter(streamValue -> Objects.nonNull(streamValue))
                        .map(x -> x.toLowerCase(Locale.ROOT))
                        .anyMatch(x -> x.contains(term));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void normalizeItems(QuestionDTO form) {
        if (form.getItems() == null || form.getItems().isEmpty())
            form.setItems(List.of(new QuestionItemDTO()));
    }

    private void normalizeQuestionFields(QuestionDTO form) {
        String skill = form.getSkill();
        if (form.getQuestionType() == null || form.getQuestionType().isBlank()) {
            if ("WRITING".equals(skill)) form.setQuestionType("WRITING_TEXT");
            else if ("SPEAKING".equals(skill)) form.setQuestionType("SPEAKING_AUDIO");
            else form.setQuestionType("SINGLE_CHOICE");
        } else if (!TYPES.contains(form.getQuestionType())) {
            form.setQuestionType("SINGLE_CHOICE");
        }
        String type = form.getQuestionType();
        if (type == null || !TYPES.contains(type)) return;
        if (!"READING".equals(skill)) form.setReadingPassage(null);
        for (QuestionItemDTO item : form.getItems()) {
            if (item.getPoints() == null) item.setPoints(BigDecimal.ONE);
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

    private void replaceQuestionItems(Integer questionId, QuestionDTO form, boolean editing) {
        if (editing) {
            for (QuestionItem item :
                    questionItemRepository.findByQuestionIdOrderByPosition(questionId)) {
                questionOptionRepository.deleteByQuestionItemId(item.getId());
            }
            questionItemRepository.deleteByQuestionId(questionId);
        }
        int position = 1;
        for (QuestionItemDTO itemForm : form.getItems()) {
            QuestionItem item = QuestionItemDTO.convertDtoToEntity(itemForm);
            item.setQuestionId(questionId);
            item.setContent(itemForm.getContent().trim());
            item.setPosition(position++);
            item = questionItemRepository.save(item);
            if (usesOptions(form.getQuestionType())) saveItemOptions(item.getId(), itemForm);
        }
    }

    private void saveItemOptions(Integer itemId, QuestionItemDTO itemForm) {
        for (int index = 0; index < itemForm.getOptions().size(); index++) {
            QuestionOptionDTO optionForm = itemForm.getOptions().get(index);
            if (optionForm.getContent() == null || optionForm.getContent().isBlank()) continue;
            QuestionOption option = QuestionOptionDTO.convertDtoToEntity(optionForm);
            option.setQuestionItemId(itemId);
            option.setContent(optionForm.getContent().trim());
            option.setCorrect(Objects.equals(index + 1, itemForm.getCorrectPosition()));
            option.setPosition(index + 1);
            questionOptionRepository.save(option);
        }
    }
}
