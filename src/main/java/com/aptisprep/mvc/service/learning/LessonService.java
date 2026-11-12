package com.aptisprep.mvc.service.learning;


import com.aptisprep.mvc.dto.LessonDTO;
import com.aptisprep.mvc.dto.LessonProgressDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;


public interface LessonService {
    public List<String> getSkills();
    public List<String> getLevels();
    public List<String> getStatuses();
    public List<Lesson> adminLessons();
    public List<LessonDTO> adminLessonDtos();
    public Page<LessonDTO> searchAdminLessonDtos(
            String query, String skill, String level, int page, int size);
    public LessonDTO newForm(String skill, String level);
    public List<Lesson> publishedLessons();
    public List<Lesson> filterPublishedLessons(String query, String skill, String level);
    public List<LessonDTO> filterPublishedLessonDtos(String query, String skill, String level);
    public Lesson require(Integer id);
    public LessonDTO requireView(Integer id);
    public Lesson requirePublished(Integer id);
    public LessonDTO requirePublishedView(Integer id);
    public Lesson requireForMedia(Integer id, String email, String notFoundMessage);
    public LessonDTO requireForMediaView(Integer id, String email, String notFoundMessage);
    public List<LessonTeachingService.CheckView> questions(Integer lessonId);
    public LessonTeachingService.LessonStats stats(Integer lessonId);
    public LessonDTO form(Integer id);
    public Lesson save(Integer id, LessonDTO f);
    public LessonDTO saveView(Integer id, LessonDTO f);
    public Lesson save(Integer id, LessonDTO f, MultipartFile audio, boolean removeAudio);
    public Lesson save( Integer id, LessonDTO f, MultipartFile audio, boolean removeAudio, MultipartFile image, boolean removeImage);
    public Lesson save( Integer id, LessonDTO f, MultipartFile audio, boolean removeAudio, MultipartFile image, boolean removeImage, MultipartFile video, boolean removeVideo);
    public LessonDTO saveView(Integer id, LessonDTO f, MultipartFile audio, boolean removeAudio, MultipartFile image, boolean removeImage, MultipartFile video, boolean removeVideo);
    public void archive(Integer id);
    public Lesson duplicate(Integer id);
    public LessonDTO duplicateView(Integer id);
    public void move(Integer id, int direction);
    public LessonProgress recordOpen(String email, Integer lessonId);
    public LessonProgress complete(String email, Integer lessonId);
    public LessonProgress progress(String email, Integer lessonId);
    public LessonProgressDTO progressView(String email, Integer lessonId);
    public Map<Integer, String> lessonStatuses(String email, Collection<Integer> lessonIds);
    public Lesson previousLesson(Lesson lesson);
    public LessonDTO previousLessonView(Integer lessonId);
    public Lesson nextLesson(Lesson lesson);
    public LessonDTO nextLessonView(Integer lessonId);
}

