package com.aptisprep.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class FlashcardViewsDTO {
    private FlashcardViewsDTO() {}

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
}
