package com.aptisprep.dto;

import java.util.List;

public class LessonAuthorProfileDTO {
    private String exampleLabel;
    private String promptLabel;
    private String strategyLabel;
    private String analysisLabel;
    private String checklistLabel;
    private String sampleLabel;
    private String mediaHint;
    private List<String> tasks;

    public LessonAuthorProfileDTO() {}

    public LessonAuthorProfileDTO(
            String exampleLabel,
            String promptLabel,
            String strategyLabel,
            String analysisLabel,
            String checklistLabel,
            String sampleLabel,
            String mediaHint,
            List<String> tasks) {
        this.exampleLabel = exampleLabel;
        this.promptLabel = promptLabel;
        this.strategyLabel = strategyLabel;
        this.analysisLabel = analysisLabel;
        this.checklistLabel = checklistLabel;
        this.sampleLabel = sampleLabel;
        this.mediaHint = mediaHint;
        this.tasks = tasks;
    }

    public static LessonAuthorProfileDTO forSkill(String skill) {
        return switch (skill == null ? "" : skill) {
            case "Reading" ->
                    new LessonAuthorProfileDTO(
                            "Bài đọc chính",
                            "Yêu cầu đọc hiểu",
                            "Chiến lược đọc (không bắt buộc)",
                            "Gợi ý phân tích bài đọc",
                            "Tiêu chí tự kiểm tra",
                            "Gợi ý đọc hiểu",
                            "Ảnh đặt cạnh bài đọc. Video hướng dẫn ở trước phần đọc. Câu hỏi ở ngay sau bài đọc.",
                            List.of("Đọc hiểu", "Đọc tìm thông tin", "Nhận diện ý chính"));
            case "Listening" ->
                    new LessonAuthorProfileDTO(
                            "Trước khi nghe — bối cảnh (không nhập transcript ở đây)",
                            "Nhiệm vụ nghe",
                            "Chiến lược nghe (không bắt buộc)",
                            "Phân tích tình huống",
                            "Tiêu chí nghe hiểu",
                            "Gợi ý làm bài nghe",
                            "Audio đặt ngay trước câu hỏi nghe. Ảnh chỉ minh họa bối cảnh, không chứa đáp án. Video hướng dẫn không thay thế audio thực hành.",
                            List.of("Hội thoại", "Thông báo", "Phỏng vấn"));
            case "Writing" ->
                    new LessonAuthorProfileDTO(
                            "Dàn ý / ví dụ hướng dẫn (không bắt buộc)",
                            "Đề viết học viên cần nộp",
                            "Cách triển khai bài viết (không bắt buộc)",
                            "Phân tích dàn ý",
                            "Tiêu chí đánh giá bài viết",
                            "Bài viết tham khảo",
                            "Ảnh đề bài đặt trên ô viết. Video hướng dẫn đặt trước phần viết. Bài mẫu mở theo thời điểm đã chọn.",
                            List.of("Viết ngắn", "Email", "Bài luận"));
            case "Speaking" ->
                    new LessonAuthorProfileDTO(
                            "Tình huống / khung triển khai (không bắt buộc)",
                            "Câu hỏi / đề nói học viên cần ghi âm",
                            "Cách triển khai câu trả lời (không bắt buộc)",
                            "Phân tích cách diễn đạt",
                            "Tiêu chí đánh giá bài nói",
                            "Gợi ý bài nói",
                            "Ảnh mô tả đặt ngay trên bộ ghi âm. Video hướng dẫn đặt trước phần nói. Học viên phải lưu bản ghi âm.",
                            List.of("Trả lời câu hỏi", "Mô tả ảnh", "So sánh trong một ảnh ghép"));
            default ->
                    new LessonAuthorProfileDTO(
                            "Cấu trúc / từ vựng và ví dụ",
                            "Yêu cầu bài tập vận dụng",
                            "Cách dùng (không bắt buộc)",
                            "Giải thích ví dụ",
                            "Tiêu chí tự kiểm tra",
                            "Gợi ý vận dụng",
                            "Ảnh minh họa đặt cạnh ví dụ. Video hướng dẫn ở trước ví dụ. Trắc nghiệm ở phần luyện tập.",
                            List.of("Ngữ pháp trong ngữ cảnh", "Từ vựng theo chủ đề"));
        };
    }

    public String getExampleLabel() {
        return exampleLabel;
    }

    public void setExampleLabel(String exampleLabel) {
        this.exampleLabel = exampleLabel;
    }

    public String exampleLabel() {
        return exampleLabel;
    }

    public String getPromptLabel() {
        return promptLabel;
    }

    public void setPromptLabel(String promptLabel) {
        this.promptLabel = promptLabel;
    }

    public String promptLabel() {
        return promptLabel;
    }

    public String getStrategyLabel() {
        return strategyLabel;
    }

    public void setStrategyLabel(String strategyLabel) {
        this.strategyLabel = strategyLabel;
    }

    public String strategyLabel() {
        return strategyLabel;
    }

    public String getAnalysisLabel() {
        return analysisLabel;
    }

    public void setAnalysisLabel(String analysisLabel) {
        this.analysisLabel = analysisLabel;
    }

    public String analysisLabel() {
        return analysisLabel;
    }

    public String getChecklistLabel() {
        return checklistLabel;
    }

    public void setChecklistLabel(String checklistLabel) {
        this.checklistLabel = checklistLabel;
    }

    public String checklistLabel() {
        return checklistLabel;
    }

    public String getSampleLabel() {
        return sampleLabel;
    }

    public void setSampleLabel(String sampleLabel) {
        this.sampleLabel = sampleLabel;
    }

    public String sampleLabel() {
        return sampleLabel;
    }

    public String getMediaHint() {
        return mediaHint;
    }

    public void setMediaHint(String mediaHint) {
        this.mediaHint = mediaHint;
    }

    public String mediaHint() {
        return mediaHint;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public void setTasks(List<String> tasks) {
        this.tasks = tasks;
    }

    public List<String> tasks() {
        return tasks;
    }
}
