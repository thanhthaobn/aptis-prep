package com.aptisprep.dto;

import com.aptisprep.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminStudentViewDTO {
    private AdminStudentViewDTO() {}

    public static String labels(UserStatus status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case ACTIVE -> "Đang hoạt động";
            case LOCKED -> "Đang bị khóa";
            case INACTIVE -> "Chưa kích hoạt";
        };
    }

    public static class ListItem {
        private Integer id;
        private String fullName;
        private String email;
        private String initials;
        private LocalDateTime joinedAt;
        private UserStatus status;
        private String targetLevel;

        public ListItem() {}

        public ListItem(
                Integer id,
                String fullName,
                String email,
                String initials,
                LocalDateTime joinedAt,
                UserStatus status,
                String targetLevel) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.initials = initials;
            this.joinedAt = joinedAt;
            this.status = status;
            this.targetLevel = targetLevel;
        }

        public String statusLabel() {
            return labels(status);
        }

        public String statusClass() {
            if (status == null) return "";
            return switch (status) {
                case ACTIVE -> "student-status-active";
                case LOCKED -> "student-status-locked";
                case INACTIVE -> "student-status-inactive";
            };
        }

        public String joinedDate() {
            return joinedAt == null ? "" : joinedAt.toLocalDate().toString();
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public Integer id() { return id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String fullName() { return fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String email() { return email; }

        public String getInitials() { return initials; }
        public void setInitials(String initials) { this.initials = initials; }
        public String initials() { return initials; }

        public LocalDateTime getJoinedAt() { return joinedAt; }
        public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
        public LocalDateTime joinedAt() { return joinedAt; }

        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }
        public UserStatus status() { return status; }

        public String getTargetLevel() { return targetLevel; }
        public void setTargetLevel(String targetLevel) { this.targetLevel = targetLevel; }
        public String targetLevel() { return targetLevel; }
    }

    public static class Progress {
        private Integer pathId;
        private String skill;
        private String pathSource;
        private String pathStatus;
        private int completedLessons;
        private int totalLessons;
        private Integer progressPercent;
        private int completedRequiredLessons;
        private int totalRequiredLessons;
        private String nextLessonTitle;
        private String checkpointState;
        private String targetLevel;

        public Progress() {}

        public Progress(
                Integer pathId,
                String skill,
                String pathSource,
                String pathStatus,
                int completedLessons,
                int totalLessons,
                Integer progressPercent,
                int completedRequiredLessons,
                int totalRequiredLessons,
                String nextLessonTitle,
                String checkpointState,
                String targetLevel) {
            this.pathId = pathId;
            this.skill = skill;
            this.pathSource = pathSource;
            this.pathStatus = pathStatus;
            this.completedLessons = completedLessons;
            this.totalLessons = totalLessons;
            this.progressPercent = progressPercent;
            this.completedRequiredLessons = completedRequiredLessons;
            this.totalRequiredLessons = totalRequiredLessons;
            this.nextLessonTitle = nextLessonTitle;
            this.checkpointState = checkpointState;
            this.targetLevel = targetLevel;
        }

        public String percentLabel() {
            return progressPercent == null ? "Chưa có dữ liệu" : progressPercent + "%";
        }

        public Integer getPathId() { return pathId; }
        public void setPathId(Integer pathId) { this.pathId = pathId; }
        public Integer pathId() { return pathId; }

        public String getSkill() { return skill; }
        public void setSkill(String skill) { this.skill = skill; }
        public String skill() { return skill; }

        public String getPathSource() { return pathSource; }
        public void setPathSource(String pathSource) { this.pathSource = pathSource; }
        public String pathSource() { return pathSource; }

        public String getPathStatus() { return pathStatus; }
        public void setPathStatus(String pathStatus) { this.pathStatus = pathStatus; }
        public String pathStatus() { return pathStatus; }

        public int getCompletedLessons() { return completedLessons; }
        public void setCompletedLessons(int completedLessons) { this.completedLessons = completedLessons; }
        public int completedLessons() { return completedLessons; }

        public int getTotalLessons() { return totalLessons; }
        public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
        public int totalLessons() { return totalLessons; }

        public Integer getProgressPercent() { return progressPercent; }
        public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
        public Integer progressPercent() { return progressPercent; }

        public int getCompletedRequiredLessons() { return completedRequiredLessons; }
        public void setCompletedRequiredLessons(int completedRequiredLessons) { this.completedRequiredLessons = completedRequiredLessons; }
        public int completedRequiredLessons() { return completedRequiredLessons; }

        public int getTotalRequiredLessons() { return totalRequiredLessons; }
        public void setTotalRequiredLessons(int totalRequiredLessons) { this.totalRequiredLessons = totalRequiredLessons; }
        public int totalRequiredLessons() { return totalRequiredLessons; }

        public String getNextLessonTitle() { return nextLessonTitle; }
        public void setNextLessonTitle(String nextLessonTitle) { this.nextLessonTitle = nextLessonTitle; }
        public String nextLessonTitle() { return nextLessonTitle; }

        public String getCheckpointState() { return checkpointState; }
        public void setCheckpointState(String checkpointState) { this.checkpointState = checkpointState; }
        public String checkpointState() { return checkpointState; }

        public String getTargetLevel() { return targetLevel; }
        public void setTargetLevel(String targetLevel) { this.targetLevel = targetLevel; }
        public String targetLevel() { return targetLevel; }
    }

    public static class Skill {
        private String name;
        private String level;
        private String target;

        public Skill() {}
        public Skill(String name, String level, String target) {
            this.name = name;
            this.level = level;
            this.target = target;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String name() { return name; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String level() { return level; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String target() { return target; }
    }

    public static class Attempt {
        private String title;
        private String date;
        private String status;
        private String score;

        public Attempt() {}
        public Attempt(String title, String date, String status, String score) {
            this.title = title;
            this.date = date;
            this.status = status;
            this.score = score;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String title() { return title; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String date() { return date; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String status() { return status; }

        public String getScore() { return score; }
        public void setScore(String score) { this.score = score; }
        public String score() { return score; }
    }

    public static class Detail {
        private Integer id;
        private String fullName;
        private String email;
        private String initials;
        private LocalDateTime joinedAt;
        private UserStatus status;
        private AccountSnapshotDTO.GoalViewDTO goal;
        private List<Skill> skills;
        private List<Progress> progress;
        private List<Attempt> attempts;
        private int attemptPage;
        private int attemptSize;
        private long attemptTotal;

        public Detail() {}

        public Detail(
                Integer id,
                String fullName,
                String email,
                String initials,
                LocalDateTime joinedAt,
                UserStatus status,
                AccountSnapshotDTO.GoalViewDTO goal,
                List<Skill> skills,
                List<Progress> progress,
                List<Attempt> attempts,
                int attemptPage,
                int attemptSize,
                long attemptTotal) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.initials = initials;
            this.joinedAt = joinedAt;
            this.status = status;
            this.goal = goal;
            this.skills = skills;
            this.progress = progress;
            this.attempts = attempts;
            this.attemptPage = attemptPage;
            this.attemptSize = attemptSize;
            this.attemptTotal = attemptTotal;
        }

        public String statusLabel() { return labels(status); }
        public String statusClass() {
            if (status == null) return "";
            return switch (status) {
                case ACTIVE -> "student-status-active";
                case LOCKED -> "student-status-locked";
                case INACTIVE -> "student-status-inactive";
            };
        }
        public String joinedDate() { return joinedAt == null ? "" : joinedAt.toLocalDate().toString(); }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public Integer id() { return id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String fullName() { return fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String email() { return email; }

        public String getInitials() { return initials; }
        public void setInitials(String initials) { this.initials = initials; }
        public String initials() { return initials; }

        public LocalDateTime getJoinedAt() { return joinedAt; }
        public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
        public LocalDateTime joinedAt() { return joinedAt; }

        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }
        public UserStatus status() { return status; }

        public AccountSnapshotDTO.GoalViewDTO getGoal() { return goal; }
        public void setGoal(AccountSnapshotDTO.GoalViewDTO goal) { this.goal = goal; }
        public AccountSnapshotDTO.GoalViewDTO goal() { return goal; }

        public List<Skill> getSkills() { return skills; }
        public void setSkills(List<Skill> skills) { this.skills = skills; }
        public List<Skill> skills() { return skills; }

        public List<Progress> getProgress() { return progress; }
        public void setProgress(List<Progress> progress) { this.progress = progress; }
        public List<Progress> progress() { return progress; }

        public List<Attempt> getAttempts() { return attempts; }
        public void setAttempts(List<Attempt> attempts) { this.attempts = attempts; }
        public List<Attempt> attempts() { return attempts; }

        public int getAttemptPage() { return attemptPage; }
        public void setAttemptPage(int attemptPage) { this.attemptPage = attemptPage; }
        public int attemptPage() { return attemptPage; }

        public int getAttemptSize() { return attemptSize; }
        public void setAttemptSize(int attemptSize) { this.attemptSize = attemptSize; }
        public int attemptSize() { return attemptSize; }

        public long getAttemptTotal() { return attemptTotal; }
        public void setAttemptTotal(long attemptTotal) { this.attemptTotal = attemptTotal; }
        public long attemptTotal() { return attemptTotal; }
    }

    public static class Page {
        private List<ListItem> items;
        private int page;
        private int size;
        private long totalItems;

        public Page() {}

        public Page(List<ListItem> items, int page, int size, long totalItems) {
            this.items = items;
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
        }

        public int totalPages() {
            return totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
        }

        public boolean hasPrevious() { return page > 0; }
        public boolean hasNext() { return page + 1 < totalPages(); }

        public List<ListItem> getItems() { return items; }
        public void setItems(List<ListItem> items) { this.items = items; }
        public List<ListItem> items() { return items; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int page() { return page; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int size() { return size; }

        public long getTotalItems() { return totalItems; }
        public void setTotalItems(long totalItems) { this.totalItems = totalItems; }
        public long totalItems() { return totalItems; }
    }
}
