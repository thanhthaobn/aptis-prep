package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamFormDTO;
import com.aptisprep.dto.form.QuestionFormDTO;
import com.aptisprep.dto.view.AdminCatalogViewDTO.CatalogPage;
import com.aptisprep.dto.view.AdminCatalogViewDTO.Entry;
import com.aptisprep.dto.view.AdminCatalogViewDTO.Module;
import com.aptisprep.dto.view.ExamCatalogDTO.CertificateOptionDTO;
import com.aptisprep.dto.view.ExamCatalogDTO.PublishedQuestionOptionDTO;
import com.aptisprep.dto.view.ExamCatalogDTO.QuestionMediaDTO;
import com.aptisprep.dto.view.ExamCatalogDTO.SchemeOptionDTO;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface CatalogService {
    Module module(String key);

    List<Entry> find(String key, String q, String skill, String status, String kind);

    CatalogPage findPage(
            String key, String q, String skill, String status, String kind, int page, int size);

    Entry entry(String key, int id);

    QuestionFormDTO questionForm(Integer id);

    Integer saveQuestion(Integer id, QuestionFormDTO form, MultipartFile upload);

    void publishQuestion(Integer id);

    ExamFormDTO examForm(Integer id);

    Integer saveExam(Integer id, ExamFormDTO form);

    void publishExam(Integer id);

    void unpublishExam(Integer id);

    List<ExamPreviewSection> examPreview(Integer id);

    void addMedia(Integer questionId, MultipartFile[] uploads);

    void deleteMedia(Integer questionId, Integer mediaId);

    void updateMedia(
            Integer questionId,
            Integer mediaId,
            String altText,
            String transcript,
            Integer maxPlayCount);

    void moveMedia(Integer questionId, Integer mediaId, int direction);

    MediaContent mediaContent(Integer questionId, Integer mediaId);

    void archiveQuestion(Integer id);

    void restoreQuestion(Integer id);

    List<CertificateOptionDTO> activeCertificates();

    List<SchemeOptionDTO> activeSchemes();

    List<PublishedQuestionOptionDTO> publishedQuestions();

    List<QuestionMediaDTO> mediaItems(Integer questionId);

    List<String> skills();

    List<String> levels();

    List<String> questionTypes();

    List<String> examTypes();

    record ExamPreviewSection(String skill, Integer minutes, List<ExamPreviewQuestion> questions) {}

    record ExamPreviewQuestion(
            Integer id,
            String skill,
            String type,
            String typeLabel,
            String content,
            String instruction,
            String readingPassage,
            List<ExamPreviewItem> items,
            List<ExamPreviewMedia> media) {}

    record ExamPreviewItem(
            int position,
            String content,
            Integer responseSeconds,
            Integer minWordCount,
            Integer maxWordCount,
            List<ExamPreviewOption> options) {}

    record ExamPreviewOption(String code, String content) {}

    record ExamPreviewMedia(
            Integer id,
            String type,
            String filename,
            String altText,
            String contentType,
            boolean browserSpeech) {}

    record MediaContent(Resource resource, String contentType) {}

    long countExams();

    long countPublishedExams();

    long countQuestions();

    List<com.aptisprep.entity.MockExamEntity> findRecentExams(int limit);

    List<com.aptisprep.entity.QuestionEntity> findRecentQuestions(int limit);
}
