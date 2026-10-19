package com.aptisprep.entity;
import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="lesson_engagement",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","lesson_id"}))
public class LessonEngagement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @Column(name="user_id",nullable=false) private Integer userId; @Column(name="lesson_id",nullable=false) private Integer lessonId;
 @Column(name="audio_played_at") private LocalDateTime audioPlayedAt; @Column(name="last_section",length=40) private String lastSection; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 @PrePersist @PreUpdate void touch(){updatedAt=LocalDateTime.now();}
 public Integer getId(){return id;} public Integer getUserId(){return userId;} public void setUserId(Integer v){userId=v;} public Integer getLessonId(){return lessonId;} public void setLessonId(Integer v){lessonId=v;} public LocalDateTime getAudioPlayedAt(){return audioPlayedAt;} public void setAudioPlayedAt(LocalDateTime v){audioPlayedAt=v;} public String getLastSection(){return lastSection;} public void setLastSection(String v){lastSection=v;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
