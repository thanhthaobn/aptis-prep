package com.aptisprep.mvc.learning.service;

import com.aptisprep.mvc.common.enums.CefrLevel;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.learning.dto.LessonEntityDTO;
import com.aptisprep.mvc.learning.dto.LessonFormDTO;
import com.aptisprep.mvc.learning.dto.LessonProgressDTO;
import com.aptisprep.mvc.learning.dto.LessonTeachingViewsDTO.CheckViewDTO;
import com.aptisprep.mvc.learning.dto.LessonTeachingViewsDTO.LessonStatsDTO;
import com.aptisprep.mvc.learning.dto.LessonTreeViewsDTO.NeighborsDTO;
import com.aptisprep.mvc.learning.dto.LessonTreeViewsDTO.SkillBranchDTO;
import com.aptisprep.mvc.learning.entity.Lesson;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface LessonService {
    List<String> SKILLS = Skill.labels();
    List<String> LEVELS = CefrLevel.curriculumLevels();
    List<String> STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");

    List<Lesson> adminLessons();

    List<LessonEntityDTO> adminLessonDTOs();

    List<SkillBranchDTO> adminTree();

    List<SkillBranchDTO> adminTree(List<Lesson> all);

    LessonFormDTO newForm(String skill, String level);

    List<Lesson> publishedLessons();

    List<LessonEntityDTO> publishedLessonDTOs();

    Lesson require(Integer id);

    LessonEntityDTO requireDto(Integer id);

    Lesson requirePublished(Integer id);

    LessonEntityDTO requirePublishedDto(Integer id);

    List<CheckViewDTO> questions(Integer lessonId);

    LessonStatsDTO stats(Integer lessonId);

    LessonFormDTO form(Integer id);

    Lesson save(Integer id, LessonFormDTO f);

    Lesson save(Integer id, LessonFormDTO f, MultipartFile audio, boolean removeAudio);

    Lesson save(
            Integer id,
            LessonFormDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage);

    Lesson save(
            Integer id,
            LessonFormDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo);

    LessonEntityDTO saveDto(
            Integer id,
            LessonFormDTO f,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo);

    void archive(Integer id);

    Lesson duplicate(Integer id);

    LessonEntityDTO duplicateDto(Integer id);

    void move(Integer id, int direction);

    LessonProgressDTO recordOpen(String email, Integer lessonId);

    LessonProgressDTO complete(String email, Integer lessonId);

    LessonProgressDTO progress(String email, Integer lessonId);

    Map<Integer, String> lessonStatuses(String email, Collection<Integer> lessonIds);

    NeighborsDTO neighbors(Integer lessonId);

    NeighborsDTO neighbors(Lesson lesson);

    String getSkill(Integer lessonId);
}
