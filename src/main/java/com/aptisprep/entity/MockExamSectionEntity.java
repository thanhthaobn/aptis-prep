package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "mock_exam_sections")
public class MockExamSectionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "mock_exam_id", nullable = false) private Integer mockExamId;
    @Column(nullable = false, length = 30) private String skill;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(nullable = false, length = 30) private String status = "ACTIVE";
    public MockExamSectionEntity() {}
    public Integer getId() { return id; }
    public void setMockExamId(Integer v) { mockExamId = v; }
    public String getSkill() { return skill; }
    public void setSkill(String v) { skill = v; }
    public void setDisplayOrder(int v) { displayOrder = v; }
    public void setDurationMinutes(int v) { durationMinutes = v; }
    public void setStatus(String v) { status = v; }
}
