package com.aptisprep.mvc.service.learning.impl;

import com.aptisprep.mvc.dto.LessonDTO;
import com.aptisprep.mvc.dto.LessonProgressDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.learning.*;
import com.aptisprep.mvc.service.learning.LessonService;
import com.aptisprep.mvc.service.learning.LessonTeachingService.CompletionState;
import com.aptisprep.mvc.service.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.stream.IntStream;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LessonServiceImpl implements LessonService {
    private LessonRepository lessonRepository;
    private LessonProgressRepository lessonProgressRepository;
    private UserRepository userRepository;
    private LearningPathService learningPaths;
    private LessonTeachingService teaching;
    private StorageService storage;

    public LessonServiceImpl(
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

    @Override
    public List<LessonDTO> adminLessonDtos() {
        return adminLessons().stream()
                .map(streamValue -> LessonDTO.convertEntityToDto(streamValue))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonDTO> searchAdminLessonDtos(
            String query, String skill, String level, int page, int size) {
        String term = query == null ? "" : query.trim();
        String selectedSkill = skill == null ? "" : skill.trim();
        String selectedLevel = level == null ? "" : level.trim();
        if (term.length() > 255)
            throw new BusinessRuleException("Từ khóa tìm kiếm tối đa 255 ký tự");
        if (!selectedSkill.isBlank() && !getSkills().contains(selectedSkill))
            throw new BusinessRuleException("Kỹ năng tìm kiếm không hợp lệ");
        if (!selectedLevel.isBlank() && !getLevels().contains(selectedLevel))
            throw new BusinessRuleException("Cấp độ tìm kiếm không hợp lệ");
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String pattern =
                term.isBlank() ? "%" : "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
        return lessonRepository
                .searchAdminLessons(
                        pattern, selectedSkill, selectedLevel, PageRequest.of(safePage, safeSize))
                .map(LessonDTO::convertEntityToDto);
    }

    @Override
    public List<String> getSkills() {
        return Skill.labels();
    }

    @Override
    public List<String> getLevels() {
        return CefrLevel.curriculumLevels();
    }

    @Override
    public List<String> getStatuses() {
        return List.of("DRAFT", "PUBLISHED", "ARCHIVED");
    }

    public LessonDTO newForm(String skill, String level) {
        if (!getSkills().contains(skill) || !getLevels().contains(level))
            throw new BusinessRuleException("Kỹ năng hoặc cấp độ không hợp lệ");
        int max =
                adminLessons().stream()
                        .filter(l -> skill.equals(l.getSkill()) && level.equals(l.getCefrLevel()))
                        .mapToInt(streamValue -> streamValue.getDisplayOrder())
                        .max()
                        .orElse(0);
        if (max == Integer.MAX_VALUE)
            throw new BusinessRuleException("Không thể gợi ý thêm thứ tự cho nhánh này");
        LessonDTO form = new LessonDTO();
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

    @Override
    public List<Lesson> filterPublishedLessons(String query, String skill, String level) {
        String normalizedQuery =
                Normalizer.normalize(query == null ? "" : query, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT);
        String selectedSkill = skill == null ? "" : skill;
        String selectedLevel = level == null ? "" : level;
        return publishedLessons().stream()
                .filter(
                        lesson -> {
                            String topic =
                                    lesson.getTopic() == null ? "" : lesson.getTopic().trim();
                            String searchable =
                                    Normalizer.normalize(
                                                    lesson.getTitle()
                                                            + " "
                                                            + lesson.getDescription()
                                                            + " "
                                                            + topic,
                                                    Normalizer.Form.NFD)
                                            .replaceAll("\\p{M}", "")
                                            .toLowerCase(Locale.ROOT);
                            return normalizedQuery.isBlank()
                                    || searchable.contains(normalizedQuery);
                        })
                .filter(
                        lesson ->
                                selectedSkill.isBlank() || selectedSkill.equals(lesson.getSkill()))
                .filter(
                        lesson ->
                                selectedLevel.isBlank()
                                        || selectedLevel.equals(lesson.getCefrLevel()))
                .toList();
    }

    @Override
    public List<LessonDTO> filterPublishedLessonDtos(String query, String skill, String level) {
        return filterPublishedLessons(query, skill, level).stream()
                .map(streamValue -> LessonDTO.convertEntityToDto(streamValue))
                .toList();
    }

    public Lesson require(Integer id) {
        return lessonRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDTO requireView(Integer id) {
        return LessonDTO.convertEntityToDto(require(id));
    }

    public Lesson requirePublished(Integer id) {
        Lesson l = require(id);
        if (!"PUBLISHED".equals(l.getStatus()))
            throw new ResourceNotFoundException("Bài học chưa được công bố");
        return l;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDTO requirePublishedView(Integer id) {
        return LessonDTO.convertEntityToDto(requirePublished(id));
    }

    @Override
    public Lesson requireForMedia(Integer id, String email, String notFoundMessage) {
        Lesson lesson =
                lessonRepository
                        .findById(id)
                        .filter(value -> "PUBLISHED".equals(value.getStatus()))
                        .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
        active(email);
        return lesson;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDTO requireForMediaView(Integer id, String email, String notFoundMessage) {
        return LessonDTO.convertEntityToDto(requireForMedia(id, email, notFoundMessage));
    }

    public List<LessonTeachingService.CheckView> questions(Integer lessonId) {
        return teaching.questions(lessonId);
    }

    public LessonTeachingService.LessonStats stats(Integer lessonId) {
        return teaching.stats(lessonId);
    }

    public LessonDTO form(Integer id) {
        Lesson l = require(id);
        LessonDTO f = LessonDTO.convertEntityToDto(l);
        f.setChecks(new ArrayList<>(teaching.editorQuestions(id)));
        if (f.getTranscriptVisibility() == null) f.setTranscriptVisibility("AFTER_LISTEN");
        if (f.getSampleVisibility() == null) f.setSampleVisibility("AFTER_SUBMIT");
        return f;
    }

    @Transactional
    public Lesson save(Integer id, LessonDTO f) {
        return save(id, f, null, false);
    }

    @Override
    @Transactional
    public LessonDTO saveView(Integer id, LessonDTO f) {
        return LessonDTO.convertEntityToDto(save(id, f));
    }

    @Transactional
    public Lesson save(Integer id, LessonDTO f, MultipartFile audio, boolean removeAudio) {
        return save(id, f, audio, removeAudio, null, false);
    }

    @Transactional
    public Lesson save(
            Integer id,
            LessonDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage) {
        return save(id, f, audio, removeAudio, image, removeImage, null, false);
    }

    @Transactional
    public Lesson save(
            Integer id,
            LessonDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo) {
        Lesson lesson = id == null ? null : require(id);
        assignDisplayOrder(f, lesson);
        if (video != null && !video.isEmpty()) validateVideo(video);
        validateListeningRelease(id, f, audio, removeAudio);
        validate(f, id);
        teaching.validate(f);
        if (lesson == null) lesson = new Lesson();
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
    public LessonDTO saveView(
            Integer id,
            LessonDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo) {
        return LessonDTO.convertEntityToDto(
                save(id, f, audio, removeAudio, image, removeImage, video, removeVideo));
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
        LessonDTO f = form(id);
        f.setTitle(source.getTitle() + " — Bản sao");
        f.setStatus("DRAFT");
        f.setDisplayOrder(newForm(source.getSkill(), source.getCefrLevel()).getDisplayOrder());
        return save(null, f);
    }

    @Override
    @Transactional
    public LessonDTO duplicateView(Integer id) {
        return LessonDTO.convertEntityToDto(duplicate(id));
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
                temp =
                        branch.stream()
                                        .mapToInt(streamValue -> streamValue.getDisplayOrder())
                                        .max()
                                        .orElse(0)
                                + 1000;
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

    @Override
    @Transactional(readOnly = true)
    public LessonProgressDTO progressView(String email, Integer lessonId) {
        return LessonProgressDTO.convertEntityToDto(progress(email, lessonId));
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

    @Override
    public Lesson previousLesson(Lesson lesson) {
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
        return i > 0 ? all.get(i - 1) : null;
    }

    @Override
    public Lesson nextLesson(Lesson lesson) {
        List<Lesson> all =
                publishedLessons().stream()
                        .filter(
                                item ->
                                        item.getSkill().equals(lesson.getSkill())
                                                && item.getCefrLevel()
                                                        .equals(lesson.getCefrLevel()))
                        .toList();
        int index =
                IntStream.range(0, all.size())
                        .filter(value -> all.get(value).getId().equals(lesson.getId()))
                        .findFirst()
                        .orElse(-1);
        return index >= 0 && index + 1 < all.size() ? all.get(index + 1) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDTO previousLessonView(Integer lessonId) {
        return LessonDTO.convertEntityToDto(previousLesson(requirePublished(lessonId)));
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDTO nextLessonView(Integer lessonId) {
        return LessonDTO.convertEntityToDto(nextLesson(requirePublished(lessonId)));
    }

    private void validate(LessonDTO f, Integer id) {
        if (!getSkills().contains(f.getSkill())
                || !getLevels().contains(f.getCefrLevel())
                || !getStatuses().contains(f.getStatus()))
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

    private void assignDisplayOrder(LessonDTO form, Lesson existing) {
        if (form.getDisplayOrder() != null && form.getDisplayOrder() > 0) return;
        if (form.getSkill() == null || form.getCefrLevel() == null) return;
        if (existing != null
                && Objects.equals(existing.getSkill(), form.getSkill())
                && Objects.equals(existing.getCefrLevel(), form.getCefrLevel())) {
            form.setDisplayOrder(existing.getDisplayOrder());
            return;
        }
        int max =
                adminLessons().stream()
                        .filter(
                                lesson ->
                                        form.getSkill().equals(lesson.getSkill())
                                                && form.getCefrLevel()
                                                        .equals(lesson.getCefrLevel()))
                        .mapToInt(Lesson::getDisplayOrder)
                        .max()
                        .orElse(0);
        if (max == Integer.MAX_VALUE)
            throw new BusinessRuleException("Không thể tự tạo thứ tự cho bài học");
        form.setDisplayOrder(max + 1);
    }

    private void validateAudio(String skill, MultipartFile file) {
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

    private void validateImage(MultipartFile file) {
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

    private void validateVideo(MultipartFile file) {
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

    private String cleanName(String value) {
        if (value == null || value.isBlank()) return "audio";
        String normalized = value.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void validateListeningRelease(
            Integer id, LessonDTO form, MultipartFile audio, boolean removeAudio) {
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

    private void applyLessonFields(Lesson lesson, LessonDTO form) {
        Lesson converted = LessonDTO.convertDtoToEntity(form);
        BeanUtils.copyProperties(
                converted,
                lesson,
                "id",
                "videoPath",
                "audioPath",
                "audioName",
                "imagePath",
                "imageName",
                "createdAt",
                "updatedAt");
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
