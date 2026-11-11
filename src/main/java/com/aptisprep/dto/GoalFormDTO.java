package com.aptisprep.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class GoalFormDTO extends UserGoalDTO {
    public GoalFormDTO() {
        super();
    }

    public GoalFormDTO(
            Map<String, BigDecimal> targetScores,
            Integer certificateId,
            LocalDate targetDate,
            String grammarVocabulary,
            String reading,
            String listening,
            String writing,
            String speaking) {
        super();
        setTargetScores(targetScores);
        setCertificateId(certificateId);
        setTargetDate(targetDate);
        setGrammarVocabulary(grammarVocabulary);
        setReading(reading);
        setListening(listening);
        setWriting(writing);
        setSpeaking(speaking);
    }
}
