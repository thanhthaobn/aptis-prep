package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonEntityDto;
import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.enums.CefrLevel;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.common.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.learning.LessonTeachingService.CompletionState;
import com.aptisprep.service.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LessonService {
    public static final List<String> SKILLS = Skill.labels();
    public static final List<String> LEVELS = CefrLevel.curriculumLevels();
    public static final List<String> STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final LearningPathService learningPaths;
    private final LessonTeachingService teaching;
    private final StorageService storage;

    public LessonService(
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            UserRepository userRepository,
            LearningPathService lp,
            LessonTeachingService t,
            StorageService s) {
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.userRepository = userRepository;
        learningPaths = lp;
        teaching = t;
        storage = s;
    }

    public List<Lesson> adminLessons() {
        return lessonRepository.findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc();
    }

    public record LevelBranch(String level, List<LessonEntityDto> lessons) {
        public Map<String, List<LessonEntityDto>> topics() {
            LinkedHashMap<String, List<LessonEntityDto>> result =
                    new LinkedHashMap<String, List<LessonEntityDto>>();
            for (LessonEntityDto lesson : lessons) {
                String topic =
                        lesson.getMaterial() == null ? null : lesson.getMaterial().getTopic();
                if (topic == null || topic.isBlank()) topic = "Bài học độc lập";
                result.computeIfAbsent(topic, key -> new ArrayList<>()).add(lesson);
            }
            return result;
        }
    }

    public record SkillBranch(String skill, List<LevelBranch> levels, int count) {}

    public List<SkillBranch> adminTree(List<Lesson> all) {
        return SKILLS.stream()
                .map(
                        skill ->
                                new SkillBranch(
                                        skill,
                                        LEVELS.stream()
                                                .map(
                                                        level ->
                                                                new LevelBranch(
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
                                                                                        LessonEntityDto
                                                                                                ::convertEntityToDto)
                                                                                .toList()))
                                                .toList(),
                                        (int)
                                                all.stream()
                                                        .filter(l -> skill.equals(l.getSkill()))
                                                        .count()))
                .toList();
    }

    public LessonForm newForm(String skill, String level) {
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
        LessonForm form = new LessonForm();
        form.setSkill(skill);
        form.setCefrLevel(level);
        form.setDisplayOrder(max + 1);
        form.setStatus("DRAFT");
        form.setTranscriptVisibility("AFTER_LISTEN");
        form.setSampleVisibility("AFTER_SUBMIT");
        return form;
    }

    public List<Lesson> publishedLessons() {
        return lessonRepository.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED");
    }

    public Lesson require(Integer id) {
        return lessonRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    }

    public Lesson requirePublished(Integer id) {
        Lesson l = require(id);
        if (!"PUBLISHED".equals(l.getStatus()))
            throw new ResourceNotFoundException("Bài học chưa được công bố");
        return l;
    }

    public List<LessonTeachingService.CheckView> questions(Integer lessonId) {
        return teaching.questions(lessonId);
    }

    public LessonTeachingService.LessonStats stats(Integer lessonId) {
        return teaching.stats(lessonId);
    }

    public LessonForm form(Integer id) {
        Lesson l = require(id);
        LessonForm f = LessonEntityDto.convertEntityToDto(l).toForm();
        f.setChecks(new ArrayList<>(teaching.editorQuestions(id)));
        if (f.getTranscriptVisibility() == null) f.setTranscriptVisibility("AFTER_LISTEN");
        if (f.getSampleVisibility() == null) f.setSampleVisibility("AFTER_SUBMIT");
        return f;
    }

    @Transactional
    public Lesson save(Integer id, LessonForm f) {
        return save(id, f, null, false);
    }

    @Transactional
    public Lesson save(Integer id, LessonForm f, MultipartFile audio, boolean removeAudio) {
        return save(id, f, audio, removeAudio, null, false);
    }

    @Transactional
    public Lesson save(
            Integer id,
            LessonForm f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage) {
        return save(id, f, audio, removeAudio, image, removeImage, null, false);
    }

    @Transactional
    public Lesson save(
            Integer id,
            LessonForm f,
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

    @Transactional
    public void archive(Integer id) {
        Lesson l = require(id);
        l.setStatus("ARCHIVED");
        lessonRepository.save(l);
    }

    @Transactional
    public Lesson duplicate(Integer id) {
        Lesson source = require(id);
        LessonForm f = form(id);
        f.setTitle(source.getTitle() + " — Bản sao");
        f.setStatus("DRAFT");
        f.setDisplayOrder(newForm(source.getSkill(), source.getCefrLevel()).getDisplayOrder());
        return save(null, f);
    }

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

    @Transactional
    public LessonProgress recordOpen(String email, Integer lessonId) {
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
        return lessonProgressRepository.save(p);
    }

    @Transactional
    public LessonProgress complete(String email, Integer lessonId) {
        User u = active(email);
        Lesson lesson = requirePublished(lessonId);
        CompletionState state = teaching.completion(lessonId, email);
        if (!state.eligible()) throw new BusinessRuleException(state.message(lesson.getSkill()));
        learningPaths.completeLesson(u.getId(), lessonId);
        return lessonProgressRepository.findByUserIdAndLessonId(u.getId(), lessonId).orElseThrow();
    }

    public LessonProgress progress(String email, Integer lessonId) {
        User u = active(email);
        return lessonProgressRepository.findByUserIdAndLessonId(u.getId(), lessonId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Integer, String> lessonStatuses(String email, Collection<Integer> lessonIds) {
        User user = active(email);
        if (lessonIds.isEmpty()) return Map.of();
        HashMap<Integer, String> statuses = new HashMap<Integer, String>();
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

    public record Neighbors(Lesson previous, Lesson next) {}

    public Neighbors neighbors(Lesson lesson) {
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
        return new Neighbors(
                i > 0 ? all.get(i - 1) : null,
                i >= 0 && i + 1 < all.size() ? all.get(i + 1) : null);
    }

    private void validate(LessonForm f, Integer id) {
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

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private void validateListeningRelease(
            Integer id, LessonForm form, MultipartFile audio, boolean removeAudio) {
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

    private void applyLessonFields(Lesson lesson, LessonForm form) {
        Lesson mapped = LessonEntityDto.convertDtoToEntity(form);
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
}
