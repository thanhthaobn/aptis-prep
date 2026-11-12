package com.aptisprep.mvc.service.admin.impl;

import com.aptisprep.mvc.repository.LessonRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.QuestionRepository;
import com.aptisprep.mvc.service.admin.AdminDashboardService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private LessonRepository lessons;
    private QuestionRepository questions;
    private MockExamRepository exams;

    public AdminDashboardServiceImpl(
            LessonRepository lessons, QuestionRepository questions, MockExamRepository exams) {
        this.lessons = lessons;
        this.questions = questions;
        this.exams = exams;
    }

    @Override
    @Transactional(readOnly = true)
    public ContentSummary contentSummary() {
        long lessonCount = lessons.count();
        long questionCount = questions.count();
        long publishedExamCount = exams.countByStatus("PUBLISHED");
        PageRequest recent = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        List<ContentUpdate> content = new ArrayList<>();
        for (var lesson : lessons.findAll(recent)) {
            content.add(
                    new ContentUpdate(
                            lesson.getTitle(),
                            "Bài học",
                            "/admin/lessons/" + lesson.getId(),
                            lesson.getUpdatedAt()));
        }
        for (var question : questions.findAll(recent)) {
            content.add(
                    new ContentUpdate(
                            question.getContent(),
                            "Câu hỏi",
                            "/admin/questions/" + question.getId(),
                            question.getUpdatedAt()));
        }
        for (var exam : exams.findAll(recent)) {
            content.add(
                    new ContentUpdate(
                            exam.getTitle(),
                            "Đề thi",
                            "/admin/exams/" + exam.getId(),
                            exam.getUpdatedAt()));
        }
        List<ContentUpdate> recentContent =
                content.stream()
                        .sorted(
                                Comparator.comparing(
                                        streamValue -> streamValue.updatedAt(),
                                        Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(3)
                        .toList();
        return new ContentSummary(lessonCount, questionCount, publishedExamCount, recentContent);
    }
}







