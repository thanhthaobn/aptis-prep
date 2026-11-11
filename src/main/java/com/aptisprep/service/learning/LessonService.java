package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonActivityDTO;
import com.aptisprep.dto.LessonDTO;
import com.aptisprep.dto.LessonEditorForm;
import com.aptisprep.dto.LessonProgressDTO;
import com.aptisprep.dto.LessonTeachingViewsDTO.CheckViewDTO;
import com.aptisprep.dto.LessonTeachingViewsDTO.LessonStatsDTO;
import com.aptisprep.dto.LessonTreeViewsDTO.NeighborsDTO;
import com.aptisprep.dto.LessonTreeViewsDTO.SkillBranchDTO;
import com.aptisprep.entity.Lesson;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface LessonService {

    public List<Lesson> getAllAdminLessons();

    public List<Lesson> adminLessons();

    public List<LessonDTO> getAllAdminLessonDTOs();

    public List<LessonDTO> adminLessonDTOs();

    public List<SkillBranchDTO> getAdminTree();

    public List<SkillBranchDTO> adminTree();

    public List<SkillBranchDTO> getAdminTree(List<Lesson> all);

    public List<SkillBranchDTO> adminTree(List<Lesson> all);

    public LessonEditorForm createNewForm(String skill, String level);

    public LessonEditorForm newForm(String skill, String level);

    public List<Lesson> getPublishedLessons();

    public List<Lesson> publishedLessons();

    public List<LessonDTO> getPublishedLessonDTOs();

    public List<LessonDTO> publishedLessonDTOs();

    public Lesson getLessonById(Integer id);

    public Lesson require(Integer id);

    public LessonDTO getLessonDtoById(Integer id);

    public LessonDTO requireDto(Integer id);

    public Lesson getPublishedLessonById(Integer id);

    public Lesson requirePublished(Integer id);

    public LessonDTO getPublishedLessonDtoById(Integer id);

    public LessonDTO requirePublishedDto(Integer id);

    public List<CheckViewDTO> getQuestionsByLessonId(Integer lessonId);

    public List<CheckViewDTO> questions(Integer lessonId);

    public LessonStatsDTO getLessonStatistics(Integer lessonId);

    public LessonStatsDTO stats(Integer lessonId);

    public LessonEditorForm getLessonFormById(Integer id);

    public LessonEditorForm form(Integer id);

    public Lesson save(Integer id, LessonEditorForm lessonForm);

    public Lesson save(
            Integer id, LessonEditorForm lessonForm, MultipartFile audio, boolean removeAudio);

    public Lesson save(
            Integer id,
            LessonEditorForm lessonForm,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage);

    public Lesson save(
            Integer id,
            LessonEditorForm lessonForm,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo);

    public LessonDTO saveDto(
            Integer id,
            LessonEditorForm lessonForm,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo);

    public void archiveLesson(Integer id);

    public void archive(Integer id);

    public Lesson duplicateLesson(Integer id);

    public Lesson duplicate(Integer id);

    public LessonDTO duplicateLessonDto(Integer id);

    public LessonDTO duplicateDto(Integer id);

    public void moveLesson(Integer id, int direction);

    public void move(Integer id, int direction);

    public LessonProgressDTO recordOpen(String email, Integer lessonId);

    public LessonProgressDTO complete(String email, Integer lessonId);

    public LessonProgressDTO progress(String email, Integer lessonId);

    public Map<Integer, String> lessonStatuses(String email, Collection<Integer> lessonIds);

    public NeighborsDTO getNeighbors(Integer lessonId);

    public NeighborsDTO neighbors(Integer lessonId);

    public NeighborsDTO getNeighbors(Lesson lesson);

    public NeighborsDTO neighbors(Lesson lesson);

    public String getSkill(Integer lessonId);

    public List<LessonActivityDTO> findLessonActivitiesByUserId(Integer userId);

    public List<java.time.LocalDateTime> findPracticeDatesByUserId(Integer userId);

    public long countLessons();

    public List<Lesson> findRecentLessons(int limit);
}
