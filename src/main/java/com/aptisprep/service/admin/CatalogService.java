package com.aptisprep.service.admin;

import com.aptisprep.dto.AdminCatalogViewDTO.CatalogPage;
import com.aptisprep.dto.AdminCatalogViewDTO.Entry;
import com.aptisprep.dto.AdminCatalogViewDTO.Module;
import com.aptisprep.dto.ExamCatalogDTO.CertificateOptionDTO;
import com.aptisprep.dto.ExamCatalogDTO.PublishedQuestionOptionDTO;
import com.aptisprep.dto.ExamCatalogDTO.SchemeOptionDTO;
import com.aptisprep.dto.ExamFormDTO;
import com.aptisprep.dto.QuestionFormDTO;
import com.aptisprep.dto.QuestionMediaDTO;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface CatalogService {
    public Module module(String key);

    public List<Entry> find(String key, String q, String skill, String status, String kind);

    public CatalogPage findPage(
            String key, String q, String skill, String status, String kind, int page, int size);

    public Entry getCatalogEntryById(String key, int id);

    public Entry entry(String key, int id);

    public QuestionFormDTO getQuestionById(Integer id);

    public QuestionFormDTO questionForm(Integer id);

    public Integer saveQuestion(Integer id, QuestionFormDTO form, MultipartFile upload);

    public void publishQuestion(Integer id);

    public ExamFormDTO getExamFormById(Integer id);

    public ExamFormDTO examForm(Integer id);

    public Integer saveExam(Integer id, ExamFormDTO form);

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

    public List<CertificateOptionDTO> activeCertificates();

    public List<SchemeOptionDTO> activeSchemes();

    public List<PublishedQuestionOptionDTO> publishedQuestions();

    public List<QuestionMediaDTO> mediaItems(Integer questionId);

    public List<String> getAvailableSkills();

    public List<String> skills();

    public List<String> getAvailableLevels();

    public List<String> levels();

    public List<String> getAvailableQuestionTypes();

    public List<String> questionTypes();

    public List<String> getAvailableExamTypes();

    public List<String> examTypes();

    public static class ExamPreviewSection {
        private String skill;
        private Integer minutes;
        private List<ExamPreviewQuestion> questions;

        public ExamPreviewSection() {}

        public ExamPreviewSection(
                String skill, Integer minutes, List<ExamPreviewQuestion> questions) {
            this.skill = skill;
            this.minutes = minutes;
            this.questions = questions;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public Integer getMinutes() {
            return minutes;
        }

        public void setMinutes(Integer minutes) {
            this.minutes = minutes;
        }

        public Integer minutes() {
            return minutes;
        }

        public List<ExamPreviewQuestion> getQuestions() {
            return questions;
        }

        public void setQuestions(List<ExamPreviewQuestion> questions) {
            this.questions = questions;
        }

        public List<ExamPreviewQuestion> questions() {
            return questions;
        }
    }

    public static class ExamPreviewQuestion {
        private Integer id;
        private String skill;
        private String type;
        private String typeLabel;
        private String content;
        private String instruction;
        private String readingPassage;
        private List<ExamPreviewItem> items;
        private List<ExamPreviewMedia> media;

        public ExamPreviewQuestion() {}

        public ExamPreviewQuestion(
                Integer id,
                String skill,
                String type,
                String typeLabel,
                String content,
                String instruction,
                String readingPassage,
                List<ExamPreviewItem> items,
                List<ExamPreviewMedia> media) {
            this.id = id;
            this.skill = skill;
            this.type = type;
            this.typeLabel = typeLabel;
            this.content = content;
            this.instruction = instruction;
            this.readingPassage = readingPassage;
            this.items = items;
            this.media = media;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

        public String typeLabel() {
            return typeLabel;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String content() {
            return content;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public String instruction() {
            return instruction;
        }

        public String getReadingPassage() {
            return readingPassage;
        }

        public void setReadingPassage(String readingPassage) {
            this.readingPassage = readingPassage;
        }

        public String readingPassage() {
            return readingPassage;
        }

        public List<ExamPreviewItem> getItems() {
            return items;
        }

        public void setItems(List<ExamPreviewItem> items) {
            this.items = items;
        }

        public List<ExamPreviewItem> items() {
            return items;
        }

        public List<ExamPreviewMedia> getMedia() {
            return media;
        }

        public void setMedia(List<ExamPreviewMedia> media) {
            this.media = media;
        }

        public List<ExamPreviewMedia> media() {
            return media;
        }
    }

    public static class ExamPreviewItem {
        private int position;
        private String content;
        private Integer responseSeconds;
        private Integer minWordCount;
        private Integer maxWordCount;
        private List<ExamPreviewOption> options;

        public ExamPreviewItem() {}

        public ExamPreviewItem(
                int position,
                String content,
                Integer responseSeconds,
                Integer minWordCount,
                Integer maxWordCount,
                List<ExamPreviewOption> options) {
            this.position = position;
            this.content = content;
            this.responseSeconds = responseSeconds;
            this.minWordCount = minWordCount;
            this.maxWordCount = maxWordCount;
            this.options = options;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int position() {
            return position;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String content() {
            return content;
        }

        public Integer getResponseSeconds() {
            return responseSeconds;
        }

        public void setResponseSeconds(Integer responseSeconds) {
            this.responseSeconds = responseSeconds;
        }

        public Integer responseSeconds() {
            return responseSeconds;
        }

        public Integer getMinWordCount() {
            return minWordCount;
        }

        public void setMinWordCount(Integer minWordCount) {
            this.minWordCount = minWordCount;
        }

        public Integer minWordCount() {
            return minWordCount;
        }

        public Integer getMaxWordCount() {
            return maxWordCount;
        }

        public void setMaxWordCount(Integer maxWordCount) {
            this.maxWordCount = maxWordCount;
        }

        public Integer maxWordCount() {
            return maxWordCount;
        }

        public List<ExamPreviewOption> getOptions() {
            return options;
        }

        public void setOptions(List<ExamPreviewOption> options) {
            this.options = options;
        }

        public List<ExamPreviewOption> options() {
            return options;
        }
    }

    public static class ExamPreviewOption {
        private String code;
        private String content;

        public ExamPreviewOption() {}

        public ExamPreviewOption(String code, String content) {
            this.code = code;
            this.content = content;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String content() {
            return content;
        }
    }

    public static class ExamPreviewMedia {
        private Integer id;
        private String type;
        private String filename;
        private String altText;
        private String contentType;
        private boolean browserSpeech;

        public ExamPreviewMedia() {}

        public ExamPreviewMedia(
                Integer id,
                String type,
                String filename,
                String altText,
                String contentType,
                boolean browserSpeech) {
            this.id = id;
            this.type = type;
            this.filename = filename;
            this.altText = altText;
            this.contentType = contentType;
            this.browserSpeech = browserSpeech;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String filename() {
            return filename;
        }

        public String getAltText() {
            return altText;
        }

        public void setAltText(String altText) {
            this.altText = altText;
        }

        public String altText() {
            return altText;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String contentType() {
            return contentType;
        }

        public boolean isBrowserSpeech() {
            return browserSpeech;
        }

        public void setBrowserSpeech(boolean browserSpeech) {
            this.browserSpeech = browserSpeech;
        }

        public boolean browserSpeech() {
            return browserSpeech;
        }
    }

    public static class MediaContent {
        private Resource resource;
        private String contentType;

        public MediaContent() {}

        public MediaContent(Resource resource, String contentType) {
            this.resource = resource;
            this.contentType = contentType;
        }

        public Resource getResource() {
            return resource;
        }

        public void setResource(Resource resource) {
            this.resource = resource;
        }

        public Resource resource() {
            return resource;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String contentType() {
            return contentType;
        }
    }

    public long countExams();

    public long countPublishedExams();

    public long countQuestions();

    public List<com.aptisprep.entity.MockExamEntity> findRecentExams(int limit);

    public List<com.aptisprep.entity.QuestionEntity> findRecentQuestions(int limit);
}
