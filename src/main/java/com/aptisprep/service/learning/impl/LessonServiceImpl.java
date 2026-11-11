package com.aptisprep.service.learning.impl;

import com.aptisprep.dto.form.LessonFormDTO;
import com.aptisprep.dto.view.LessonActivityDTO;
import com.aptisprep.dto.view.LessonDTO;
import com.aptisprep.dto.view.LessonProgressDTO;
import com.aptisprep.dto.view.LessonTeachingViewsDTO.CheckViewDTO;
import com.aptisprep.dto.view.LessonTeachingViewsDTO.CompletionStateDTO;
import com.aptisprep.dto.view.LessonTeachingViewsDTO.LessonStatsDTO;
import com.aptisprep.dto.view.LessonTreeViewsDTO.LevelBranchDTO;
import com.aptisprep.dto.view.LessonTreeViewsDTO.NeighborsDTO;
import com.aptisprep.dto.view.LessonTreeViewsDTO.SkillBranchDTO;
import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonCheckAttempt;
import com.aptisprep.entity.LessonProgress;
import com.aptisprep.entity.User;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.LessonCheckAttemptRepository;
import com.aptisprep.repository.LessonProgressRepository;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.learning.LearningPathService;
import com.aptisprep.service.learning.LessonService;
import com.aptisprep.service.learning.LessonTeachingService;
import com.aptisprep.service.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonCheckAttemptRepository lessonCheckAttemptRepository;
    private final UserService userRepository;
    private final LearningPathService learningPaths;
    private final LessonTeachingService teaching;
    private final StorageService storage;

    public LessonServiceImpl(
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonCheckAttemptRepository lessonCheckAttemptRepository,
            UserService userRepository,
            LearningPathService lp,
            LessonTeachingService t,
            StorageService s) {
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonCheckAttemptRepository = lessonCheckAttemptRepository;
        this.userRepository = userRepository;
        learningPaths = lp;
        teaching = t;
        storage = s;
    }

    @Override
    public List<Lesson> adminLessons() {
        return lessonRepository.findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc();
    }

    @Override
    public List<LessonDTO> adminLessonDTOs() {
        return adminLessons().stream().map(LessonDTO::convertEntityToDto).toList();
    }

    @Override
    public List<SkillBranchDTO> adminTree() {
        return adminTree(adminLessons());
    }

    @Override
    public List<SkillBranchDTO> adminTree(List<Lesson> all) {
        return SKILLS.stream()
                .map(
                        skill ->
                                new SkillBranchDTO(
                                        skill,
                                        LEVELS.stream()
                                                .map(
                                                        level ->
                                                                new LevelBranchDTO(
                                                                        level,
                                                                        all.stream()
                                                                                .filter(
                                                                                        l ->
                                                                                                skill
                                                                                                                .equals(
                                                                                                                        l
                                                                                                                                .getSkill())
                                                                                                        && level
                                                                                                                .equals(
                                                                                                                        l
                                                                                                                                .getCefrLevel()))
                                                                                .map(
                                                                                        LessonDTO
                                                                                                ::convertEntityToDto)
                                                                                .toList()))
                                                .toList(),
                                        (int)
                                                all.stream()
                                                        .filter(l -> skill.equals(l.getSkill()))
                                                        .count()))
                .toList();
    }

    @Override
    public LessonFormDTO newForm(String skill, String level) {
        if (!SKILLS.contains(skill) || !LEVELS.contains(level))
            throw new BusinessRuleException("Kỹ năng hoặc cấp độ không hợp lệ");
        int max =
                adminLessons().stream()
                        .filter(l -> skill.equals(l.getSkill()) && level.equals(l.getCefrLevel()))
                        .mapToInt(Lesson::getDisplayOrder)
                        .max()
                        .orElse(0);
        if (max == Integer.MAX_VALUE)
            throw new BusinessRuleException("Không thể gợi ý thêm thứ tự cho nhánh này");
        LessonFormDTO form = new LessonFormDTO();
        form.setSkill(skill);
        form.setCefrLevel(level);
        form.setDisplayOrder(max + 1);
        form.setStatus("DRAFT");
        form.setTranscriptVisibility("AFTER_LISTEN");
        form.setSampleVisibility("AFTER_SUBMIT");
        return form;
    }

    @Override
    public List<Lesson> publishedLessons() {
        return lessonRepository.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED");
    }

    @Override
    public List<LessonDTO> publishedLessonDTOs() {
        return publishedLessons().stream().map(LessonDTO::convertEntityToDto).toList();
    }

    @Override
    public Lesson require(Integer id) {
        return lessonRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    }

    @Override
    public LessonDTO requireDto(Integer id) {
        return LessonDTO.convertEntityToDto(require(id));
    }

    @Override
    public Lesson requirePublished(Integer id) {
        Lesson l = require(id);
        if (!"PUBLISHED".equals(l.getStatus()))
            throw new ResourceNotFoundException("Bài học chưa được công bố");
        return l;
    }

    @Override
    public LessonDTO requirePublishedDto(Integer id) {
        return LessonDTO.convertEntityToDto(requirePublished(id));
    }

    @Override
    public List<CheckViewDTO> questions(Integer lessonId) {
        return teaching.questions(lessonId);
    }

    @Override
    public LessonStatsDTO stats(Integer lessonId) {
        return teaching.stats(lessonId);
    }

    @Override
    public LessonFormDTO form(Integer id) {
        Lesson l = require(id);
        LessonFormDTO f = LessonDTO.convertEntityToDto(l).toForm();
        f.setChecks(new ArrayList<>(teaching.editorQuestions(id)));
        if (f.getTranscriptVisibility() == null) f.setTranscriptVisibility("AFTER_LISTEN");
        if (f.getSampleVisibility() == null) f.setSampleVisibility("AFTER_SUBMIT");
        return f;
    }

    @Override
    @Transactional
    public Lesson save(Integer id, LessonFormDTO f) {
        return save(id, f, null, false);
    }

    @Override
    @Transactional
    public Lesson save(Integer id, LessonFormDTO f, MultipartFile audio, boolean removeAudio) {
        return save(id, f, audio, removeAudio, null, false);
    }

    @Override
    @Transactional
    public Lesson save(
            Integer id,
            LessonFormDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage) {
        return save(id, f, audio, removeAudio, image, removeImage, null, false);
    }

    @Override
    @Transactional
    public Lesson save(
            Integer id,
            LessonFormDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo) {
        if (video != null && !video.isEmpty()) validateVideo(video);
        validateListeningRelease(id, f, audio, removeAudio);
        validate(f, id);
        teaching.validate(f);
        Lesson lesson = id == null ? new Lesson() : require(id);
        applyLessonFields(lesson, f);
        lesson = lessonRepository.save(lesson);
        teaching.saveChecks(lesson.getId(), f.getChecks());
        updateAudio(lesson, f.getSkill(), audio, removeAudio);
        updateImage(lesson, image, removeImage);
        updateVideo(lesson, video, removeVideo);
        return lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public LessonDTO saveDto(
            Integer id,
            LessonFormDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo) {
        return LessonDTO.convertEntityToDto(
                save(id, f, audio, removeAudio, image, removeImage, video, removeVideo));
    }

    @Override
    @Transactional
    public void archive(Integer id) {
        Lesson l = require(id);
        l.setStatus("ARCHIVED");
        lessonRepository.save(l);
    }

    @Override
    @Transactional
    public Lesson duplicate(Integer id) {
        Lesson source = require(id);
        LessonFormDTO f = form(id);
        f.setTitle(source.getTitle() + " — Bản sao");
        f.setStatus("DRAFT");
        f.setDisplayOrder(newForm(source.getSkill(), source.getCefrLevel()).getDisplayOrder());
        return save(null, f);
    }

    @Override
    @Transactional
    public LessonDTO duplicateDto(Integer id) {
        return LessonDTO.convertEntityToDto(duplicate(id));
    }

    @Override
    @Transactional
    public void move(Integer id, int direction) {
        if (direction != -1 && direction != 1)
            throw new BusinessRuleException("Hướng di chuyển không hợp lệ");
        Lesson current = require(id);
        List<Lesson> branch =
                adminLessons().stream()
                        .filter(
                                x ->
                                        x.getSkill().equals(current.getSkill())
                                                && x.getCefrLevel().equals(current.getCefrLevel()))
                        .toList();
        int index =
                IntStream.range(0, branch.size())
                        .filter(i -> branch.get(i).getId().equals(id))
                        .findFirst()
                        .orElseThrow();
        int target = index + direction;
        if (target < 0 || target >= branch.size()) return;
        Lesson other = branch.get(target);
        int a = current.getDisplayOrder(),
                b = other.getDisplayOrder(),
                temp = branch.stream().mapToInt(Lesson::getDisplayOrder).max().orElse(0) + 1000;
        current.setDisplayOrder(temp);
        lessonRepository.saveAndFlush(current);
        other.setDisplayOrder(a);
        lessonRepository.saveAndFlush(other);
        current.setDisplayOrder(b);
        lessonRepository.save(current);
    }

    @Override
    @Transactional
    public LessonProgressDTO recordOpen(String email, Integer lessonId) {
        User u = active(email);
        Lesson l = requirePublished(lessonId);
        LessonProgress p =
                lessonProgressRepository
                        .findByUserIdAndLessonId(u.getId(), l.getId())
                        .orElseGet(
                                () -> {
                                    LessonProgress x = new LessonProgress();
                                    x.setUser(u);
                                    x.setLesson(l);
                                    x.setStatus("IN_PROGRESS");
                                    return x;
                                });
        p.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(p);
        return new LessonProgressDTO(p.getId(), p.getStatus());
    }

    @Override
    @Transactional
    public LessonProgressDTO complete(String email, Integer lessonId) {
        User u = active(email);
        Lesson lesson = requirePublished(lessonId);
        CompletionStateDTO state = teaching.completion(lessonId, email);
        if (!state.eligible()) throw new BusinessRuleException(state.message(lesson.getSkill()));
        learningPaths.completeLesson(u.getId(), lessonId);
        LessonProgress p =
                lessonProgressRepository.findByUserIdAndLessonId(u.getId(), lessonId).orElseThrow();
        return new LessonProgressDTO(p.getId(), p.getStatus());
    }

    @Override
    public LessonProgressDTO progress(String email, Integer lessonId) {
        User u = active(email);
        return lessonProgressRepository
                .findByUserIdAndLessonId(u.getId(), lessonId)
                .map(p -> new LessonProgressDTO(p.getId(), p.getStatus()))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, String> lessonStatuses(String email, Collection<Integer> lessonIds) {
        User user = active(email);
        if (lessonIds.isEmpty()) return Map.of();
        HashMap<Integer, String> statuses = new HashMap<>();
        for (LessonProgress value :
                lessonProgressRepository.findByUserIdAndLessonIdIn(user.getId(), lessonIds)) {
            boolean completed =
                    "COMPLETED".equals(value.getStatus()) && value.getCompletedAt() != null;
            statuses.put(value.getLesson().getId(), completed ? "COMPLETED" : "IN_PROGRESS");
        }
        return Map.copyOf(statuses);
    }

    private User active(String email) {
        User u =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (u.getStatus() != UserStatus.ACTIVE)
            throw new BusinessRuleException("Tài khoản không hoạt động");
        return u;
    }

    @Override
    public NeighborsDTO neighbors(Integer lessonId) {
        return neighbors(require(lessonId));
    }

    @Override
    public NeighborsDTO neighbors(Lesson lesson) {
        List<Lesson> all =
                publishedLessons().stream()
                        .filter(
                                x ->
                                        x.getSkill().equals(lesson.getSkill())
                                                && x.getCefrLevel().equals(lesson.getCefrLevel()))
                        .toList();
        int i =
                IntStream.range(0, all.size())
                        .filter(x -> all.get(x).getId().equals(lesson.getId()))
                        .findFirst()
                        .orElse(-1);
        Lesson prev = i > 0 ? all.get(i - 1) : null;
        Lesson next = i >= 0 && i + 1 < all.size() ? all.get(i + 1) : null;
        return new NeighborsDTO(
                LessonDTO.convertEntityToDto(prev), LessonDTO.convertEntityToDto(next));
    }

    @Override
    public String getSkill(Integer lessonId) {
        return require(lessonId).getSkill();
    }

    private void validate(LessonFormDTO f, Integer id) {
        if (!SKILLS.contains(f.getSkill())
                || !LEVELS.contains(f.getCefrLevel())
                || !STATUSES.contains(f.getStatus()))
            throw new BusinessRuleException("Kỹ năng, CEFR hoặc trạng thái không hợp lệ");
        if (f.getMinWordCount() != null
                && f.getMaxWordCount() != null
                && f.getMinWordCount() > f.getMaxWordCount())
            throw new BusinessRuleException("Số từ tối thiểu không được lớn hơn số từ tối đa");
        if (lessonRepository.existsBySkillAndCefrLevelAndDisplayOrderAndIdNot(
                f.getSkill(), f.getCefrLevel(), f.getDisplayOrder(), id == null ? -1 : id))
            throw new BusinessRuleException(
                    "Thứ tự bài học đã tồn tại trong kỹ năng và cấp độ này");
    }

    private static void validateAudio(String skill, MultipartFile file) {
        String name = cleanName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        String type =
                file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!"Listening".equals(skill))
            throw new BusinessRuleException("Tệp âm thanh chỉ dùng cho bài Listening");
        if (file.getSize() > 20L * 1024 * 1024)
            throw new BusinessRuleException("Tệp âm thanh tối đa 20 MB");
        if (!(name.endsWith(".mp3")
                        || name.endsWith(".wav")
                        || name.endsWith(".m4a")
                        || name.endsWith(".ogg")
                        || name.endsWith(".webm"))
                || !type.startsWith("audio/"))
            throw new BusinessRuleException(
                    "Chỉ chấp nhận tệp âm thanh MP3, WAV, M4A, OGG hoặc WEBM");
    }

    private static void validateImage(MultipartFile file) {
        String name = cleanName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        String type =
                file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (file.getSize() > 8L * 1024 * 1024)
            throw new BusinessRuleException("Hình ảnh tối đa 8 MB");
        if (!(name.endsWith(".jpg")
                        || name.endsWith(".jpeg")
                        || name.endsWith(".png")
                        || name.endsWith(".webp")
                        || name.endsWith(".gif"))
                || !type.startsWith("image/"))
            throw new BusinessRuleException("Chỉ chấp nhận hình JPG, PNG, WEBP hoặc GIF");
    }

    private static void validateVideo(MultipartFile file) {
        String name = cleanName(file.getOriginalFilename()).toLowerCase(Locale.ROOT),
                type = Objects.toString(file.getContentType(), "");
        if (file.getSize() > 50L * 1024 * 1024
                || !(name.endsWith(".mp4") && type.equals("video/mp4")
                        || name.endsWith(".webm") && type.equals("video/webm")))
            throw new BusinessRuleException("Video chỉ nhận MP4 hoặc WEBM, tối đa 50 MB");
        try (InputStream input = file.getInputStream()) {
            byte[] head = input.readNBytes(12);
            boolean mp4 =
                    head.length >= 12
                            && head[4] == 'f'
                            && head[5] == 't'
                            && head[6] == 'y'
                            && head[7] == 'p';
            boolean webm =
                    head.length >= 4
                            && (head[0] & 255) == 0x1a
                            && (head[1] & 255) == 0x45
                            && (head[2] & 255) == 0xdf
                            && (head[3] & 255) == 0xa3;
            if (name.endsWith(".mp4") ? !mp4 : !webm)
                throw new BusinessRuleException("Nội dung tệp video không đúng định dạng");
        } catch (IOException ex) {
            throw new BusinessRuleException("Không đọc được video tải lên");
        }
    }

    private static String cleanName(String value) {
        if (value == null || value.isBlank()) return "audio";
        String normalized = value.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private void validateListeningRelease(
            Integer id, LessonFormDTO form, MultipartFile audio, boolean removeAudio) {
        if (!"PUBLISHED".equals(form.getStatus()) || !"Listening".equals(form.getSkill())) return;
        boolean available =
                audio != null && !audio.isEmpty()
                        || id != null && !removeAudio && has(require(id).getAudioPath());
        boolean transcriptAfterListen =
                has(form.getTranscript())
                        && (!has(form.getTranscriptVisibility())
                                || "AFTER_LISTEN".equals(form.getTranscriptVisibility()));
        boolean sampleAfterListen =
                has(form.getSampleResponse()) && "AFTER_LISTEN".equals(form.getSampleVisibility());
        if (!available && (transcriptAfterListen || sampleAfterListen)) {
            throw new BusinessRuleException(
                    "Cần tải audio để mở transcript / bài mẫu sau khi nghe, hoặc chọn thời điểm mở khác");
        }
    }

    private void applyLessonFields(Lesson lesson, LessonFormDTO form) {
        Lesson mapped = LessonDTO.convertDtoToEntity(form);
        lesson.setTitle(mapped.getTitle());
        lesson.setDescription(mapped.getDescription());
        lesson.setSkill(mapped.getSkill());
        lesson.setCefrLevel(mapped.getCefrLevel());
        lesson.setDisplayOrder(mapped.getDisplayOrder());
        lesson.setStatus(mapped.getStatus());
        lesson.setMaterial(mapped.getMaterial());
        lesson.setTranscript(mapped.getTranscript());
        lesson.setMinWordCount(mapped.getMinWordCount());
        lesson.setMaxWordCount(mapped.getMaxWordCount());
    }

    private void updateAudio(
            Lesson lesson, String skill, MultipartFile audio, boolean removeAudio) {
        if (removeAudio && lesson.getAudioPath() != null) {
            teaching.resetAudioEngagement(lesson.getId());
            lesson.setAudioPath(null);
            lesson.setAudioName(null);
        }
        if (audio != null && !audio.isEmpty()) {
            validateAudio(skill, audio);
            teaching.resetAudioEngagement(lesson.getId());
            lesson.setAudioPath(storage.store(audio, "lesson-audio/" + lesson.getId()));
            lesson.setAudioName(cleanName(audio.getOriginalFilename()));
        }
    }

    private void updateImage(Lesson lesson, MultipartFile image, boolean removeImage) {
        if (removeImage && lesson.getImagePath() != null) {
            lesson.setImagePath(null);
            lesson.setImageName(null);
        }
        if (image != null && !image.isEmpty()) {
            validateImage(image);
            lesson.setImagePath(storage.store(image, "lesson-images/" + lesson.getId()));
            lesson.setImageName(cleanName(image.getOriginalFilename()));
        }
    }

    private void updateVideo(Lesson lesson, MultipartFile video, boolean removeVideo) {
        if (removeVideo) lesson.setVideoPath(null);
        if (video != null && !video.isEmpty()) {
            lesson.setVideoPath(storage.store(video, "lesson-video/" + lesson.getId()));
        }
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonActivityDTO> findLessonActivitiesByUserId(Integer userId) {
        return lessonProgressRepository.findByUserId(userId).stream()
                .map(p -> new LessonActivityDTO(p.getLastAccessedAt(), p.getCompletedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> findPracticeDatesByUserId(Integer userId) {
        return lessonCheckAttemptRepository.findByUserId(userId).stream()
                .map(LessonCheckAttempt::getSubmittedAt)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countLessons() {
        return lessonRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findRecentLessons(int limit) {
        return lessonRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt", "id")))
                .getContent();
    }
}
