package com.aptisprep.dto;

import com.aptisprep.entity.FlashcardSet;
import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlashcardSetDTO {
    private Integer id;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotBlank(message = "Trình độ CEFR không được để trống")
    private String cefrLevel = "A1";

    @NotBlank(message = "Trạng thái không được để trống")
    private String status = "DRAFT";

    private Lesson lesson;
    private User owner;
    private String visibility;
    private Integer lessonId;
    private List<Row> rows = new ArrayList<>();
    private int count;

    public FlashcardSetDTO() {}

    public FlashcardSetDTO(
            Integer id,
            String title,
            String description,
            String cefrLevel,
            String status,
            Lesson lesson,
            User owner,
            String visibility,
            Integer lessonId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.cefrLevel = cefrLevel;
        this.status = status;
        this.lesson = lesson;
        this.owner = owner;
        this.visibility = visibility;
        this.lessonId = lessonId;
    }

    public static class Row {
        private Integer id;

        @Size(max = 200, message = "Thuật ngữ tối đa 200 ký tự")
        private String term;

        @Size(max = 1000, message = "Định nghĩa tối đa 1000 ký tự")
        private String definition;

        public Row() {}

        public Row(Integer id, String term, String definition) {
            this.id = id;
            this.term = term;
            this.definition = definition;
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

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String term() {
            return term;
        }

        public String getDefinition() {
            return definition;
        }

        public void setDefinition(String definition) {
            this.definition = definition;
        }

        public String definition() {
            return definition;
        }
    }

    public static class SetView {
        private Integer id;
        private String title;
        private String description;
        private String level;
        private String status;
        private int count;
        private boolean personal;
        private Integer lessonId;
        private String lessonTitle;
        private String lessonSkill;

        public SetView() {}

        public SetView(
                Integer id,
                String title,
                String description,
                String level,
                String status,
                int count,
                boolean personal,
                Integer lessonId,
                String lessonTitle,
                String lessonSkill) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.level = level;
            this.status = status;
            this.count = count;
            this.personal = personal;
            this.lessonId = lessonId;
            this.lessonTitle = lessonTitle;
            this.lessonSkill = lessonSkill;
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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String level() {
            return level;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String status() {
            return status;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }

        public boolean isPersonal() {
            return personal;
        }

        public void setPersonal(boolean personal) {
            this.personal = personal;
        }

        public boolean personal() {
            return personal;
        }

        public Integer getLessonId() {
            return lessonId;
        }

        public void setLessonId(Integer lessonId) {
            this.lessonId = lessonId;
        }

        public Integer lessonId() {
            return lessonId;
        }

        public String getLessonTitle() {
            return lessonTitle;
        }

        public void setLessonTitle(String lessonTitle) {
            this.lessonTitle = lessonTitle;
        }

        public String lessonTitle() {
            return lessonTitle;
        }

        public String getLessonSkill() {
            return lessonSkill;
        }

        public void setLessonSkill(String lessonSkill) {
            this.lessonSkill = lessonSkill;
        }

        public String lessonSkill() {
            return lessonSkill;
        }
    }

    public static class LevelSets {
        private String level;
        private List<SetView> sets;

        public LevelSets() {}

        public LevelSets(String level, List<SetView> sets) {
            this.level = level;
            this.sets = sets;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String level() {
            return level;
        }

        public List<SetView> getSets() {
            return sets;
        }

        public void setSets(List<SetView> sets) {
            this.sets = sets;
        }

        public List<SetView> sets() {
            return sets;
        }
    }

    public static class SkillSets {
        private String skill;
        private int count;
        private List<LevelSets> levels;

        public SkillSets() {}

        public SkillSets(String skill, int count, List<LevelSets> levels) {
            this.skill = skill;
            this.count = count;
            this.levels = levels;
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

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }

        public List<LevelSets> getLevels() {
            return levels;
        }

        public void setLevels(List<LevelSets> levels) {
            this.levels = levels;
        }

        public List<LevelSets> levels() {
            return levels;
        }
    }

    public static class CardView {
        private Integer id;
        private String term;
        private String definition;
        private String state;
        private LocalDateTime nextReviewAt;

        public CardView() {}

        public CardView(
                Integer id,
                String term,
                String definition,
                String state,
                LocalDateTime nextReviewAt) {
            this.id = id;
            this.term = term;
            this.definition = definition;
            this.state = state;
            this.nextReviewAt = nextReviewAt;
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

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String term() {
            return term;
        }

        public String getDefinition() {
            return definition;
        }

        public void setDefinition(String definition) {
            this.definition = definition;
        }

        public String definition() {
            return definition;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String state() {
            return state;
        }

        public LocalDateTime getNextReviewAt() {
            return nextReviewAt;
        }

        public void setNextReviewAt(LocalDateTime nextReviewAt) {
            this.nextReviewAt = nextReviewAt;
        }

        public LocalDateTime nextReviewAt() {
            return nextReviewAt;
        }
    }

    public static class LessonLink {
        private Integer id;
        private String title;
        private String skill;

        public LessonLink() {}

        public LessonLink(Integer id, String title, String skill) {
            this.id = id;
            this.title = title;
            this.skill = skill;
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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
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
    }

    public static class StudySetView {
        private Integer id;
        private String title;
        private String description;
        private String cefrLevel;
        private LessonLink lesson;

        public StudySetView() {}

        public StudySetView(
                Integer id, String title, String description, String cefrLevel, LessonLink lesson) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.cefrLevel = cefrLevel;
            this.lesson = lesson;
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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        public String getCefrLevel() {
            return cefrLevel;
        }

        public void setCefrLevel(String cefrLevel) {
            this.cefrLevel = cefrLevel;
        }

        public String cefrLevel() {
            return cefrLevel;
        }

        public LessonLink getLesson() {
            return lesson;
        }

        public void setLesson(LessonLink lesson) {
            this.lesson = lesson;
        }

        public LessonLink lesson() {
            return lesson;
        }
    }

    public static FlashcardSetDTO convertEntityToDto(FlashcardSet entity) {
        if (entity == null) return null;
        FlashcardSetDTO dto = new FlashcardSetDTO();
        dto.id = entity.getId();
        dto.title = entity.getTitle();
        dto.description = entity.getDescription();
        dto.cefrLevel = entity.getCefrLevel();
        dto.status = entity.getStatus();
        dto.lesson = entity.getLesson();
        dto.owner = entity.getOwner();
        dto.visibility = entity.getVisibility();
        dto.lessonId = entity.getLesson() == null ? null : entity.getLesson().getId();
        return dto;
    }

    public FlashcardSet convertDtoToEntity() {
        FlashcardSet entity = new FlashcardSet();
        updateEntityFromDto(entity);
        if (this.id != null) {
            entity.setId(this.id);
        }
        return entity;
    }

    public static FlashcardSet convertDtoToEntity(FlashcardSetDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public static FlashcardSet convertDtoToEntity(FlashcardSetDTO form, Lesson lesson, User owner) {
        if (form == null) return null;
        if (lesson != null) form.setLesson(lesson);
        if (owner != null) {
            form.setOwner(owner);
            form.setVisibility("PRIVATE");
        } else {
            form.setVisibility("PUBLIC");
        }
        return form.convertDtoToEntity();
    }

    public void updateEntityFromDto(FlashcardSet entity) {
        if (entity == null) return;
        entity.setTitle(trim(this.title));
        entity.setDescription(blankToNull(this.description));
        entity.setCefrLevel(this.cefrLevel);
        entity.setStatus(this.status);
        if (this.lesson != null) entity.setLesson(this.lesson);
        if (this.owner != null) entity.setOwner(this.owner);
        if (this.visibility != null) entity.setVisibility(this.visibility);
    }

    public FlashcardSetFormDTO toForm() {
        FlashcardSetFormDTO form = new FlashcardSetFormDTO();
        form.setId(this.id);
        form.setTitle(this.title);
        form.setDescription(this.description);
        form.setCefrLevel(this.cefrLevel);
        form.setStatus(this.status);
        form.setLesson(this.lesson);
        form.setOwner(this.owner);
        form.setVisibility(this.visibility);
        form.setLessonId(this.lessonId);
        form.setRows(this.rows);
        form.setCount(this.count);
        return form;
    }

    public static FlashcardSetDTO fromForm(FlashcardSetDTO form, Lesson lesson, User owner) {
        if (form == null) return null;
        form.setLesson(lesson);
        form.setOwner(owner);
        form.setVisibility(owner == null ? "PUBLIC" : "PRIVATE");
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public String cefrLevel() {
        return cefrLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String status() {
        return status;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public Lesson lesson() {
        return lesson;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public User owner() {
        return owner;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String visibility() {
        return visibility;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public Integer lessonId() {
        return lessonId;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }

    public List<Row> rows() {
        return rows;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int count() {
        return count;
    }
}
