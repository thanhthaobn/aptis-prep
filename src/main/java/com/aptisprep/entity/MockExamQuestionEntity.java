package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "mock_exam_questions")
public class MockExamQuestionEntity {
    public MockExamQuestionEntity(
            Integer mockExamId, Integer mockExamSectionId, Integer questionId, int position) {
        this.mockExamId = mockExamId;
        this.mockExamSectionId = mockExamSectionId;
        this.questionId = questionId;
        this.position = position;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mock_exam_id", nullable = false)
    private Integer mockExamId;

    @Column(name = "mock_exam_section_id")
    private Integer mockExamSectionId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(nullable = false)
    private int position;

    public MockExamQuestionEntity() {}

    public Integer getId() {
        return id;
    }

    public Integer getMockExamId() {
        return mockExamId;
    }

    public Integer getMockExamSectionId() {
        return mockExamSectionId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public int getPosition() {
        return position;
    }

    public void setMockExamId(Integer v) {
        mockExamId = v;
    }

    public void setMockExamSectionId(Integer v) {
        mockExamSectionId = v;
    }

    public void setQuestionId(Integer v) {
        questionId = v;
    }

    public void setPosition(int v) {
        position = v;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
