package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_attempt_snapshot_options")
public class ExamOptionSnapshotEntity {
    public ExamOptionSnapshotEntity() {}

    public ExamOptionSnapshotEntity(
            ExamItemSnapshotEntity item,
            Integer sourceOptionId,
            String optionCode,
            Integer position,
            String content,
            Boolean correct) {
        this.item = item;
        this.sourceOptionId = sourceOptionId;
        this.optionCode = optionCode;
        this.position = position;
        this.content = content;
        this.correct = correct;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_snapshot_id", nullable = false)
    private ExamItemSnapshotEntity item;

    @Column(name = "source_option_id", nullable = false)
    private Integer sourceOptionId;

    @Column(name = "option_code", length = 30)
    private String optionCode;

    @Column(nullable = false)
    private Integer position;

    @Column(columnDefinition = "nvarchar(max)")
    private String content;

    @Column(nullable = false)
    private Boolean correct;

    public Integer getId() {
        return id;
    }

    public ExamItemSnapshotEntity getItem() {
        return item;
    }

    public void setItem(ExamItemSnapshotEntity value) {
        item = value;
    }

    public Integer getSourceOptionId() {
        return sourceOptionId;
    }

    public void setSourceOptionId(Integer value) {
        sourceOptionId = value;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String value) {
        optionCode = value;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer value) {
        position = value;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        content = value;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean value) {
        correct = value;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
