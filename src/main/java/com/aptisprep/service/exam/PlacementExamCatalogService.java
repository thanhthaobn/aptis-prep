package com.aptisprep.service.exam;

import com.aptisprep.dto.PlacementDTO.PlacementExam;
import java.util.List;

public interface PlacementExamCatalogService {
    public List<PlacementExam> findPublished();

    public List<PlacementExam> findPublished(String skill);
}
