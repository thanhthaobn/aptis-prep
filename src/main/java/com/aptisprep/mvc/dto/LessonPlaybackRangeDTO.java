package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonPlaybackRangeDTO {
    private Integer id;
    private LessonEngagement engagement;
    private double startSeconds;
    private double endSeconds;

    public LessonPlaybackRangeDTO() {}

    public LessonPlaybackRangeDTO(
            Integer id,
            LessonEngagement engagement,
            double startSeconds,
            double endSeconds) {
        this.id = id;
        this.engagement = engagement;
        this.startSeconds = startSeconds;
        this.endSeconds = endSeconds;
    }

    public static LessonPlaybackRangeDTO convertEntityToDto(LessonPlaybackRange entity) {
        if (entity == null) return null;
        return new LessonPlaybackRangeDTO(
                entity.getId(),
                entity.getEngagement(),
                entity.getStartSeconds(),
                entity.getEndSeconds());
    }

    public static LessonPlaybackRange convertDtoToEntity(LessonPlaybackRangeDTO dto) {
        if (dto == null) return null;
        return new LessonPlaybackRange(
                dto.getId(),
                dto.getEngagement(),
                dto.getStartSeconds(),
                dto.getEndSeconds());
    }

    public double getEndSeconds() {
        return endSeconds;
    }

    public void setEndSeconds(double endSeconds) {
        this.endSeconds = endSeconds;
    }

    public double getStartSeconds() {
        return startSeconds;
    }

    public void setStartSeconds(double startSeconds) {
        this.startSeconds = startSeconds;
    }

    public LessonEngagement getEngagement() {
        return engagement;
    }

    public void setEngagement(LessonEngagement engagement) {
        this.engagement = engagement;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}











