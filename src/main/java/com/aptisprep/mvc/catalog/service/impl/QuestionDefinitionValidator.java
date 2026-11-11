package com.aptisprep.mvc.catalog.service.impl;

import com.aptisprep.mvc.catalog.dto.QuestionFormDTO;
import com.aptisprep.mvc.common.enums.CefrLevel;
import com.aptisprep.mvc.common.enums.QuestionType;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import java.util.*;

/** Validates a question definition without database writes or Spring state. */
final class QuestionDefinitionValidator {
    private static final List<String> SKILLS = Skill.codes();
    private static final List<String> LEVELS = CefrLevel.curriculumLevels();
    private static final List<String> TYPES =
            Arrays.stream(QuestionType.values()).map(Enum::name).toList();
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private QuestionDefinitionValidator() {}

    public static void validate(QuestionFormDTO f) {
        if (f.getContent() == null || f.getContent().isBlank() || f.getContent().length() > 10000)
            throw new BusinessRuleException("Nội dung câu hỏi là bắt buộc");
        if (f.getReadingPassage() != null && f.getReadingPassage().length() > 50000)
            throw new BusinessRuleException("Đoạn văn Reading tối đa 50.000 ký tự");
        if (f.getSkill() == null
                || f.getCefrLevel() == null
                || f.getQuestionType() == null
                || !SKILLS.contains(f.getSkill())
                || !LEVELS.contains(f.getCefrLevel())
                || !TYPES.contains(f.getQuestionType())
                || !DIFFICULTIES.contains(f.getDifficulty()))
            throw new BusinessRuleException(
                    "Kỹ năng, cấp độ, độ khó hoặc dạng câu hỏi không hợp lệ");
        if (f.getStatus() == null
                || !List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(f.getStatus()))
            throw new BusinessRuleException("Trạng thái lưu không hợp lệ");
        QuestionType type = QuestionType.valueOf(f.getQuestionType());
        if (!type.supportsSkill(f.getSkill()))
            throw new BusinessRuleException("Dạng câu hỏi không phù hợp với kỹ năng");
        if (f.getItems() == null
                || f.getItems().isEmpty()
                || f.getItems().stream()
                        .anyMatch(x -> x.getContent() == null || x.getContent().isBlank()))
            throw new BusinessRuleException("Câu hỏi phải có ít nhất một ý hỏi");
        if (("MATCHING".equals(f.getQuestionType()) || "ORDERING".equals(f.getQuestionType()))
                && f.getItems().size() < 2)
            throw new BusinessRuleException("Dạng ghép nối hoặc sắp xếp phải có ít nhất 2 ý hỏi");
        if (type == QuestionType.ORDERING) {
            Set<Integer> positions = new HashSet<>();
            for (QuestionFormDTO.ItemForm item : f.getItems()) {
                Integer position = item.getCorrectPosition();
                if (position == null
                        || position < 1
                        || position > f.getItems().size()
                        || !positions.add(position))
                    throw new BusinessRuleException(
                            "Vị trí đúng phải đủ và duy nhất từ 1 đến số ý hỏi");
            }
        }
        if (type.usesOptions()) {
            for (QuestionFormDTO.ItemForm item : f.getItems()) {
                List<QuestionFormDTO.OptionForm> itemOptions =
                        item.getOptions() == null ? List.of() : item.getOptions();
                int lastFilled = 0;
                boolean gap = false;
                for (int index = 0; index < itemOptions.size(); index++) {
                    boolean filled =
                            itemOptions.get(index).getContent() != null
                                    && !itemOptions.get(index).getContent().isBlank();
                    if (filled) {
                        if (gap)
                            throw new BusinessRuleException(
                                    "Phương án phải được nhập liên tục từ A trở đi");
                        lastFilled = index + 1;
                    } else {
                        gap = true;
                    }
                }
                boolean correct =
                        f.getItems() != null
                                && item.getCorrectPosition() != null
                                && item.getCorrectPosition() >= 1
                                && item.getCorrectPosition() <= lastFilled
                                && itemOptions.get(item.getCorrectPosition() - 1).getContent()
                                        != null
                                && !itemOptions
                                        .get(item.getCorrectPosition() - 1)
                                        .getContent()
                                        .isBlank();
                if (lastFilled < 2 || !correct)
                    throw new BusinessRuleException(
                            "Mỗi ý hỏi phải có ít nhất 2 phương án và một đáp án đúng");
            }
        }
        if (f.getItems().stream()
                .anyMatch(
                        x ->
                                x.getMinWordCount() != null && x.getMinWordCount() < 0
                                        || x.getMaxWordCount() != null && x.getMaxWordCount() < 0
                                        || x.getResponseSeconds() != null
                                                && x.getResponseSeconds() <= 0))
            throw new BusinessRuleException(
                    "Giới hạn số từ không được âm và thời gian trả lời phải lớn hơn 0");
        if (f.getItems().stream()
                .anyMatch(
                        x ->
                                x.getMinWordCount() != null
                                        && x.getMaxWordCount() != null
                                        && x.getMinWordCount() > x.getMaxWordCount()))
            throw new BusinessRuleException("Số từ tối thiểu không được lớn hơn tối đa");
        if (f.getPreparationSeconds() != null && f.getPreparationSeconds() < 0)
            throw new BusinessRuleException("Thời gian chuẩn bị không được âm");
    }
}
