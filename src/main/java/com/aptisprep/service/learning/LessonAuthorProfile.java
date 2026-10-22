package com.aptisprep.service.learning;

import java.util.*;

/** Labels for the existing simple form, not a new content/block model. */
public record LessonAuthorProfile(String exampleLabel,String promptLabel,String strategyLabel,String analysisLabel,
                                  String checklistLabel,String sampleLabel,String mediaHint,List<String> tasks) {
 public static LessonAuthorProfile forSkill(String skill){return switch(skill){
  case "Reading" -> new LessonAuthorProfile("Bài đọc chính","Yêu cầu đọc hiểu","Chiến lược đọc (không bắt buộc)","Gợi ý phân tích bài đọc","Tiêu chí tự kiểm tra","Gợi ý đọc hiểu",
   "Ảnh đặt cạnh bài đọc. Video hướng dẫn ở trước phần đọc; câu hỏi ở ngay sau bài đọc.",List.of("Đọc hiểu","Đọc tìm thông tin","Nhận diện ý chính"));
  case "Listening" -> new LessonAuthorProfile("Trước khi nghe — bối cảnh (không nhập transcript ở đây)","Nhiệm vụ nghe","Chiến lược nghe (không bắt buộc)","Phân tích tình huống","Tiêu chí nghe hiểu","Gợi ý làm bài nghe",
   "Audio đặt ngay trước câu hỏi nghe. Ảnh chỉ minh họa bối cảnh, không chứa đáp án. Video hướng dẫn không thay thế audio thực hành.",List.of("Hội thoại","Thông báo","Phỏng vấn"));
  case "Writing" -> new LessonAuthorProfile("Dàn ý / ví dụ hướng dẫn (không bắt buộc)","Đề viết học viên cần nộp","Cách triển khai bài viết (không bắt buộc)","Phân tích dàn ý","Tiêu chí đánh giá bài viết","Bài viết tham khảo",
   "Ảnh đề bài đặt trên ô viết. Video hướng dẫn đặt trước phần viết. Bài mẫu mở theo thời điểm đã chọn.",List.of("Viết ngắn","Email","Bài luận"));
  case "Speaking" -> new LessonAuthorProfile("Tình huống / khung triển khai (không bắt buộc)","Câu hỏi / đề nói học viên cần ghi âm","Cách triển khai câu trả lời (không bắt buộc)","Phân tích cách diễn đạt","Tiêu chí đánh giá bài nói","Gợi ý bài nói",
   "Ảnh mô tả đặt ngay trên bộ ghi âm. Video hướng dẫn đặt trước phần nói. Học viên phải lưu bản ghi âm.",List.of("Trả lời câu hỏi","Mô tả ảnh","So sánh trong một ảnh ghép"));
  default -> new LessonAuthorProfile("Cấu trúc / từ vựng và ví dụ","Yêu cầu bài tập vận dụng","Cách dùng (không bắt buộc)","Giải thích ví dụ","Tiêu chí tự kiểm tra","Gợi ý vận dụng",
   "Ảnh minh họa đặt cạnh ví dụ. Video hướng dẫn ở trước ví dụ; trắc nghiệm ở phần luyện tập.",List.of("Ngữ pháp trong ngữ cảnh","Từ vựng theo chủ đề"));
 };}
}
