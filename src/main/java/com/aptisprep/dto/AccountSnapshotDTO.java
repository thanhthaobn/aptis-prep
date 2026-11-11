package com.aptisprep.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AccountSnapshotDTO {
    private String fullName;
    private String email;
    private GoalViewDTO goal;
    private List<SkillViewDTO> profiles;

    public AccountSnapshotDTO() {}

    public AccountSnapshotDTO(
            String fullName, String email, GoalViewDTO goal, List<SkillViewDTO> profiles) {
        this.fullName = fullName;
        this.email = email;
        this.goal = goal;
        this.profiles = profiles;
    }

    public static class GoalViewDTO {
        private Integer certificateId;
        private String certificateName;
        private LocalDate targetDate;
        private Map<String, String> targets;
        private Map<String, BigDecimal> targetScores;

        public GoalViewDTO() {}

        public GoalViewDTO(
                Integer certificateId,
                String certificateName,
                LocalDate targetDate,
                Map<String, String> targets,
                Map<String, BigDecimal> targetScores) {
            this.certificateId = certificateId;
            this.certificateName = certificateName;
            this.targetDate = targetDate;
            this.targets = targets;
            this.targetScores = targetScores;
        }

        public GoalViewDTO(
                Integer certificateId,
                String certificateName,
                LocalDate targetDate,
                Map<String, String> targets) {
            this(certificateId, certificateName, targetDate, targets, Map.of());
        }

        public Integer getCertificateId() {
            return certificateId;
        }

        public void setCertificateId(Integer certificateId) {
            this.certificateId = certificateId;
        }

        public Integer certificateId() {
            return certificateId;
        }

        public String getCertificateName() {
            return certificateName;
        }

        public void setCertificateName(String certificateName) {
            this.certificateName = certificateName;
        }

        public String certificateName() {
            return certificateName;
        }

        public LocalDate getTargetDate() {
            return targetDate;
        }

        public void setTargetDate(LocalDate targetDate) {
            this.targetDate = targetDate;
        }

        public LocalDate targetDate() {
            return targetDate;
        }

        public Map<String, String> getTargets() {
            return targets;
        }

        public void setTargets(Map<String, String> targets) {
            this.targets = targets;
        }

        public Map<String, String> targets() {
            return targets;
        }

        public Map<String, BigDecimal> getTargetScores() {
            return targetScores;
        }

        public void setTargetScores(Map<String, BigDecimal> targetScores) {
            this.targetScores = targetScores;
        }

        public Map<String, BigDecimal> targetScores() {
            return targetScores;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GoalViewDTO that = (GoalViewDTO) o;
            return java.util.Objects.equals(certificateId, that.certificateId)
                    && java.util.Objects.equals(certificateName, that.certificateName)
                    && java.util.Objects.equals(targetDate, that.targetDate)
                    && java.util.Objects.equals(targets, that.targets)
                    && java.util.Objects.equals(targetScores, that.targetScores);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(
                    certificateId, certificateName, targetDate, targets, targetScores);
        }
    }

    public static class SkillViewDTO {
        private String skill;
        private String currentLevel;
        private Integer sourceAttemptId;

        public SkillViewDTO() {}

        public SkillViewDTO(String skill, String currentLevel, Integer sourceAttemptId) {
            this.skill = skill;
            this.currentLevel = currentLevel;
            this.sourceAttemptId = sourceAttemptId;
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

        public String getCurrentLevel() {
            return currentLevel;
        }

        public void setCurrentLevel(String currentLevel) {
            this.currentLevel = currentLevel;
        }

        public String currentLevel() {
            return currentLevel;
        }

        public Integer getSourceAttemptId() {
            return sourceAttemptId;
        }

        public void setSourceAttemptId(Integer sourceAttemptId) {
            this.sourceAttemptId = sourceAttemptId;
        }

        public Integer sourceAttemptId() {
            return sourceAttemptId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SkillViewDTO that = (SkillViewDTO) o;
            return java.util.Objects.equals(skill, that.skill)
                    && java.util.Objects.equals(currentLevel, that.currentLevel)
                    && java.util.Objects.equals(sourceAttemptId, that.sourceAttemptId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(skill, currentLevel, sourceAttemptId);
        }
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String fullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String email() {
        return email;
    }

    public GoalViewDTO getGoal() {
        return goal;
    }

    public void setGoal(GoalViewDTO goal) {
        this.goal = goal;
    }

    public GoalViewDTO goal() {
        return goal;
    }

    public List<SkillViewDTO> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<SkillViewDTO> profiles) {
        this.profiles = profiles;
    }

    public List<SkillViewDTO> profiles() {
        return profiles;
    }
}
