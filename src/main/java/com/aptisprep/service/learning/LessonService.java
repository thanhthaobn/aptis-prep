package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonDTO;
import com.aptisprep.entity.Lesson;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.Skill;
import com.aptisprep.presentation.LessonActivityDTO;
import com.aptisprep.presentation.LessonProgressDTO;
import com.aptisprep.presentation.LessonTeachingViewsDTO.CheckViewDTO;
import com.aptisprep.presentation.LessonTeachingViewsDTO.LessonStatsDTO;
import com.aptisprep.presentation.LessonTreeViewsDTO.NeighborsDTO;
import com.aptisprep.presentation.LessonTreeViewsDTO.SkillBranchDTO;
import com.aptisprep.presentation.form.LessonEditorForm;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface LessonService {
    List<String> SKILLS = Skill.labels();
    List<String> LEVELS = CefrLevel.curriculumLevels();
    List<String> STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");

    List<Lesson> adminLessons();

    List<LessonDTO> adminLessonDTOs();

    List<SkillBranchDTO> adminTree();

    List<SkillBranchDTO> adminTree(List<Lesson> all);

    LessonEditorForm newForm(String skill, String level);

    List<Lesson> publishedLessons();

    List<LessonDTO> publishedLessonDTOs();

    Lesson require(Integer id);

    LessonDTO requireDto(Integer id);

    Lesson requirePublished(Integer id);

    LessonDTO requirePublishedDto(Integer id);

    List<CheckViewDTO> questions(Integer lessonId);

    LessonStatsDTO stats(Integer lessonId);

    LessonEditorForm form(Integer id);

    Lesson save(Integer id, LessonEditorForm f);

    Lesson save(Integer id, LessonEditorForm f, MultipartFile audio, boolean removeAudio);

    Lesson save(
            Integer id,
            LessonEditorForm f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage);

    Lesson save(
            Integer id,
            LessonEditorForm f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo);

    LessonDTO saveDto(
            Integer id,
            LessonEditorForm f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo);

    void archive(Integer id);

    Lesson duplicate(Integer id);

    LessonDTO duplicateDto(Integer id);

    void move(Integer id, int direction);

    LessonProgressDTO recordOpen(String email, Integer lessonId);

    LessonProgressDTO complete(String email, Integer lessonId);

    LessonProgressDTO progress(String email, Integer lessonId);

    Map<Integer, String> lessonStatuses(String email, Collection<Integer> lessonIds);

    NeighborsDTO neighbors(Integer lessonId);

    NeighborsDTO neighbors(Lesson lesson);

    String getSkill(Integer lessonId);

    List<LessonActivityDTO> findLessonActivitiesByUserId(Integer userId);

    List<java.time.LocalDateTime> findPracticeDatesByUserId(Integer userId);

    long countLessons();

    List<Lesson> findRecentLessons(int limit);
}
