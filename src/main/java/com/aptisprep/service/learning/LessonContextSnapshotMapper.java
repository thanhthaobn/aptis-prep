package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonContextSnapshot;
import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonContextSnapshotEntity;

public final class LessonContextSnapshotMapper {
    private LessonContextSnapshotMapper() {}

    public static LessonContextSnapshotEntity capture(Lesson lesson) {
        return new LessonContextSnapshotEntity(
                lesson.getTitle(),
                lesson.getSkill(),
                lesson.getDescription(),
                lesson.getObjectives(),
                lesson.getStrategy(),
                lesson.getExampleText(),
                lesson.getAnalysis(),
                lesson.getPracticePrompt(),
                lesson.getImagePath(),
                lesson.getAudioPath(),
                lesson.getVideoPath(),
                lesson.getVideoUrl(),
                lesson.getMinWordCount(),
                lesson.getMaxWordCount());
    }

    public static LessonContextSnapshot toPresentation(LessonContextSnapshotEntity entity) {
        if (entity == null) return null;
        return new LessonContextSnapshot(
                entity.getTitle(),
                entity.getSkill(),
                entity.getDescription(),
                entity.getObjectives(),
                entity.getStrategy(),
                entity.getExampleText(),
                entity.getAnalysis(),
                entity.getPracticePrompt(),
                entity.getImagePath(),
                entity.getAudioPath(),
                entity.getVideoPath(),
                entity.getVideoUrl(),
                entity.getMinWords(),
                entity.getMaxWords());
    }
}
