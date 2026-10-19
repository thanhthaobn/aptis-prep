package com.aptisprep.entity;
import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="lesson_check_attempts")
public class LessonCheckAttempt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @Column(name="user_id",nullable=false) private Integer userId; @Column(name="lesson_id",nullable=false) private Integer lessonId;
 @Column(nullable=false) private int score; @Column(nullable=false) private int total;
 @Column(name="answers_json",nullable=false,columnDefinition="nvarchar(max)") private String answersJson;
 @Column(name="submitted_at",nullable=false) private LocalDateTime submittedAt;
 @PrePersist void create(){if(submittedAt==null)submittedAt=LocalDateTime.now();}
 public Integer getId(){return id;} public Integer getUserId(){return userId;} public void setUserId(Integer v){userId=v;} public Integer getLessonId(){return lessonId;} public void setLessonId(Integer v){lessonId=v;} public int getScore(){return score;} public void setScore(int v){score=v;} public int getTotal(){return total;} public void setTotal(int v){total=v;} public String getAnswersJson(){return answersJson;} public void setAnswersJson(String v){answersJson=v;} public LocalDateTime getSubmittedAt(){return submittedAt;}
}
