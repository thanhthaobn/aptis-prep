package com.aptisprep.mvc.service.admin;

import com.aptisprep.mvc.dto.CertificateDTO;
import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.dto.QuestionDTO;
import com.aptisprep.mvc.dto.QuestionMediaDTO;
import com.aptisprep.mvc.dto.ScoringSchemeDTO;
import com.aptisprep.mvc.entity.Certificate;
import com.aptisprep.mvc.entity.Question;
import com.aptisprep.mvc.entity.QuestionMedia;
import com.aptisprep.mvc.entity.ScoringScheme;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface CatalogService {
    public Map<String, Object> module(String key);

    public Page<QuestionDTO> questionPage(
            String q, String skill, String status, String kind, int page, int size);

    public Page<MockExamDTO> examPage(
            String q, String skill, String status, String kind, int page, int size);

    public QuestionDTO question(Integer id);

    public MockExamDTO exam(Integer id);

    public QuestionDTO questionForm(Integer id);

    public Integer saveQuestion(Integer id, QuestionDTO form, MultipartFile upload);

    public void publishQuestion(Integer id);

    public MockExamDTO examForm(Integer id);

    public Integer saveExam(Integer id, MockExamDTO form);

    public void publishExam(Integer id);

    public void unpublishExam(Integer id);

    public List<ExamPreviewSection> examPreview(Integer id);

    public void addMedia(Integer questionId, MultipartFile[] uploads);

    public void deleteMedia(Integer questionId, Integer mediaId);

    public void updateMedia(
            Integer questionId,
            Integer mediaId,
            String altText,
            String transcript,
            Integer maxPlayCount);

    public void moveMedia(Integer questionId, Integer mediaId, int direction);

    public MediaContent mediaContent(Integer questionId, Integer mediaId);

    public void archiveQuestion(Integer id);

    public void restoreQuestion(Integer id);

    public List<Certificate> activeCertificates();

    public List<CertificateDTO> activeCertificateViews();

    public List<ScoringScheme> activeSchemes();

    public List<ScoringSchemeDTO> activeSchemeViews();

    public List<Question> publishedQuestions();

    public List<QuestionDTO> publishedQuestionViews();

    public List<QuestionMedia> mediaItems(Integer questionId);

    public List<QuestionMediaDTO> mediaItemViews(Integer questionId);

    public List<String> skills();

    public List<String> levels();

    public List<String> questionTypes();

    public List<String> examTypes();

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
            String contentType,
            String altText,
            String transcript,
            boolean browserSpeech) {}

    record MediaContent(Resource resource, String contentType) {}
}
