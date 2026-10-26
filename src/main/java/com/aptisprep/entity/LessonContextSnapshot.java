package com.aptisprep.entity;

import tools.jackson.databind.json.JsonMapper;

/** Immutable stimulus snapshot. Deliberately excludes transcripts, models and answer keys. */
public record LessonContextSnapshot(
        String title,
        String skill,
        String description,
        String objectives,
        String strategy,
        String exampleText,
        String analysis,
        String practicePrompt,
        String imagePath,
        String audioPath,
        String videoPath,
        String videoUrl,
        Integer minWords,
        Integer maxWords) {
    // Pure entity snapshot codec: strings/integers only, independent of web JSON configuration.
    // Reuse one immutable mapper rather than allocating one for every entity getter.
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static LessonContextSnapshot capture(Lesson l) {
        LessonMaterial m = l.getMaterial();
        return new LessonContextSnapshot(
                l.getTitle(),
                l.getSkill(),
                l.getDescription(),
                m == null ? null : m.getObjectives(),
                m == null ? null : m.getStrategy(),
                m == null ? null : m.getExampleText(),
                m == null ? null : m.getAnalysis(),
                m == null ? null : m.getPracticePrompt(),
                l.getImagePath(),
                l.getAudioPath(),
                l.getVideoPath(),
                m == null ? null : m.getVideoUrl(),
                l.getMinWordCount(),
                l.getMaxWordCount());
    }

    public static String serialize(Lesson l) {
        return JSON.writeValueAsString(capture(l));
    }

    public static LessonContextSnapshot read(String json) {
        // V027 compatibility: an old block array is not a snapshot of this simple form.
        return json == null || json.isBlank() || !json.stripLeading().startsWith("{")
                ? null
                : JSON.readValue(json, LessonContextSnapshot.class);
    }
}
